/*
 * Copyright (C) 2005 Gérard Milmeister
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package org.rubato.composer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.rubato.base.Rubette;
import org.rubato.composer.network.NetworkModel;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.util.TextUtils;


/**
 * A Runner runs the network previously set using the method setNetwork().
 * 
 * Stopping is cooperative. The first click on stop sets the flag that
 * rubettes read through {@link RunInfo#stopped()} and that the runner checks
 * before each rubette. The second click also interrupts the run thread, which
 * ends blocking calls such as sleeps and waits. The third click interrupts
 * again and waits up to {@link #GRACE_PERIOD_MS} for the thread to end. A
 * thread that is still running after that is abandoned: the runner forgets it,
 * finishes the run in the user interface and reports that the network was
 * left running in the background. The run threads are daemon threads, so an
 * abandoned one does not keep the program alive.
 * 
 * @author Gérard Milmeister
 */
public class Runner implements Runnable {

    /**
     * How long the third click on stop waits for the network to end before
     * abandoning it, in milliseconds.
     */
    public static final long GRACE_PERIOD_MS = 2000;
    
    
    /**
     * How many characters of an exception message a Problems line shows.
     */
    private static final int MAX_MESSAGE_LENGTH = 300;
    
    
    /**
     * The parts of the user interface that a Runner reports to.
     * JComposer provides them in the program; tests provide their own.
     */
    public interface Host {
        public void resetProgressWindow(int max);
        public void addProgressMessage(String msg);
        public void makeProgress(int value);
        public void finishRun();
        public void showErrorDialog(String msg);
    }
    
    
    /**
     * Creates a new runner for the given JComposer.
     */
    public Runner(final JComposer composer) {
        this(new Host() {
            public void resetProgressWindow(int max) { composer.resetProgressWindow(max); }
            public void addProgressMessage(String msg) { composer.addProgressMessage(msg); }
            public void makeProgress(int value) { composer.makeProgress(value); }
            public void finishRun() { composer.finishRun(); }
            public void showErrorDialog(String msg) { composer.showErrorDialog(msg); }
        });
    }
    
    
    /**
     * Creates a new runner that reports to the given host.
     */
    public Runner(Host host) {
        this.host = host;
        this.runInfo = new RunnerRunInfo(host);
    }
    
    
    /**
     * Sets the network to run.
     */
    public void setNetwork(NetworkModel network) {
        this.network = network;
        dependents = network.getDependents();
    }
    
    
    /**
     * Sets a list of rubettes to run in the given network.
     */
    public void setList(NetworkModel network, ArrayList<RubetteModel> list) {
        this.network = network;
        dependents = list;
    }
    
    
    /**
     * Runs the current network.
     */
    public void run() {
        //each run keeps its own state, so an abandoned thread that ends later
        //cannot report into a newer run
        RunnerRunInfo info = runInfo;
        List<RubetteModel> list = dependents;
        LinkedList<Problem> runProblems = problems;
        try {
            host.resetProgressWindow(list.size());
            for (int i = 0; i < list.size(); i++) {
                if (info.stopped() || Thread.currentThread().isInterrupted()) { break; }
                RubetteModel model = list.get(i);
                Rubette rubette = model.getRubette();
                rubette.clearErrors();
                try {
                    logger.info(TextUtils.replaceStrings("Running rubette %%1", model.getName())); //$NON-NLS-1$
                    info.addProgressMessage(TextUtils.replaceStrings("Running rubette %%1", model.getName()));
                    if (model.isPassThrough()) {
                        rubette.setOutput(0, rubette.getInput(0));
                    }
                    else {
                        rubette.run(info);
                        if (info.abandoned) {
                            return;
                        }
                        if (rubette.hasErrors()) {
                            addProblems(runProblems, rubette.getErrors(), model);
                        }
                        else if (!info.stopped()) {
                            rubette.updateView();
                        }
                    }
                }
                catch (Exception e) {
                    if (info.abandoned) {
                        return;
                    }
                    if (!info.stopped()) {
                        e.printStackTrace();
                        addProblem(runProblems, describe("Runner.exceptionproblem", e), model); //$NON-NLS-1$
                    }
                }
                catch (Error e) {
                    //an Error such as StackOverflowError or OutOfMemoryError ends the run,
                    //since the program may not be in a state to run further rubettes
                    if (info.abandoned) {
                        return;
                    }
                    if (!info.stopped()) {
                        e.printStackTrace();
                        addProblem(runProblems, describe("Runner.errorproblem", e), model); //$NON-NLS-1$
                    }
                    break;
                }
                if (!info.abandoned) {
                    host.makeProgress(i+1);
                }
            }
        }
        finally {
            if (info.claimFinish()) {
                host.finishRun();
            }
        }
    }
    

    /**
     * Returns the Problems line for a Throwable from a rubette's run method:
     * the message with the given key, with %1 replaced by the class name and
     * the first line of the message, and %2 by the place it was thrown.
     */
    static String describe(String key, Throwable t) {
        String what = t.getClass().getSimpleName();
        if (what.isEmpty()) {
            what = t.getClass().getName();
        }
        String msg = t.getMessage();
        if (msg != null) {
            int nl = msg.indexOf('\n');
            msg = (nl >= 0 ? msg.substring(0, nl) : msg).trim();
            if (msg.length() > MAX_MESSAGE_LENGTH) {
                msg = msg.substring(0, MAX_MESSAGE_LENGTH) + "\u2026"; //$NON-NLS-1$
            }
            if (!msg.isEmpty()) {
                what += ": " + msg; //$NON-NLS-1$
            }
        }
        return fill(Messages.getString(key), what, place(t));
    }


    /**
     * Returns the first stack frame outside the Java runtime and this class,
     * as Class.method(File.java:line), or "?" when there is none.
     */
    private static String place(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            if (c.startsWith("java.") || c.startsWith("javax.") || c.startsWith("jdk.") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                || c.startsWith("sun.") || c.startsWith("com.sun.") //$NON-NLS-1$ //$NON-NLS-2$
                || c.equals(Runner.class.getName())) {
                continue;
            }
            String simple = c.substring(c.lastIndexOf('.') + 1);
            String file = e.getFileName() == null ? "?" : e.getFileName(); //$NON-NLS-1$
            return simple + "." + e.getMethodName() + "(" + file + ":" + e.getLineNumber() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        return "?"; //$NON-NLS-1$
    }


    /**
     * Replaces %1 and %2 in the template in one pass, taking the values
     * literally, so a value that contains $, \ or %2 stays as it is.
     */
    private static String fill(String template, String v1, String v2) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '%' && i + 1 < template.length() && (template.charAt(i + 1) == '1' || template.charAt(i + 1) == '2')) {
                b.append(template.charAt(i + 1) == '1' ? v1 : v2);
                i++;
            }
            else {
                b.append(c);
            }
        }
        return b.toString();
    }


    /**
     * Adds a new problem to the list of problems.
     * @param msg the string describing the problem
     * @param model the RubetteModel where the problem occurred
     */
    public void addProblem(String msg, RubetteModel model) {
        addProblem(problems, msg, model);
    }
    
    
    private void addProblem(LinkedList<Problem> list, String msg, RubetteModel model) {
        list.add(new Problem(msg, network.getJNetwork(), model.getJRubette()));
    }

    
    /**
     * Adds a list of new problems to the list of problems.
     * @param msgs a list of the strings describing the problems
     * @param model the RubetteModel where the problems occurred
     */
    public void addProblems(List<String> msgs, RubetteModel model) {
        addProblems(problems, msgs, model);
    }
    
    
    private void addProblems(LinkedList<Problem> list, List<String> msgs, RubetteModel model) {
        for (String msg : msgs) {
            addProblem(list, msg, model);
        }
    }
    
    
    /**
     * Returns the list of problems.
     */
    public List<Problem> getProblems() {
        return problems;
    }


    /**
     * Begins running of the network.
     */
    public void start() {
        runInfo = new RunnerRunInfo(host);
        problems = new LinkedList<Problem>();
        nrClicked = 0;
        thread = new Thread(this, "rubato-network-runner"); //$NON-NLS-1$
        thread.setDaemon(true);
        thread.start();
    }
    
    
    /**
     * Returns true if the thread of the latest run has not ended and has
     * not been abandoned.
     */
    public boolean isRunning() {
        return thread != null && thread.isAlive() && !runInfo.abandoned;
    }
    

    /**
     * Stops running of the network.
     */
    public void stop() {
        if (thread == null || runInfo.abandoned) {
            return;
        }
        final Thread current = thread;
        final RunnerRunInfo info = runInfo;
        info.stop();
        nrClicked++;
        if (nrClicked >= 2) {
            current.interrupt();
        }
        if (nrClicked == 3) {
            // at the third click on stop, wait briefly for the thread to end,
            // without blocking the caller, and abandon it if it does not
            Thread watchdog = new Thread(new Runnable() {
                public void run() {
                    try {
                        current.join(GRACE_PERIOD_MS);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    if (current.isAlive() && info.claimFinish()) {
                        info.abandoned = true;
                        logger.warning("Abandoned a network that did not stop"); //$NON-NLS-1$
                        host.finishRun();
                        host.showErrorDialog(Messages.getString("Runner.abandoned")); //$NON-NLS-1$
                    }
                }
            }, "rubato-network-stop"); //$NON-NLS-1$
            watchdog.setDaemon(true);
            watchdog.start();
        }
    }
    
    
    private Host          host;
    private Thread        thread;
    private NetworkModel  network;
    private ArrayList<RubetteModel> dependents;
    private LinkedList<Problem> problems = new LinkedList<Problem>();    
    private volatile RunnerRunInfo runInfo;
    private int           nrClicked = 0;     
    
    private static final Logger logger = Logger.getLogger("org.rubato.composer.runner"); //$NON-NLS-1$
    
    protected static class RunnerRunInfo implements RunInfo {
        
        public RunnerRunInfo(Host h) {
            host = h;
        }
        
        public boolean stopped() {
            return stop;
        }
        
        public void stop() {
            stop = true;
        }

        public void reset() {
            stop = false;
        }
        
        public void addMessage(RubetteModel rubette, String msg) {
            addProgressMessage(rubette.getName()+": "+msg);
        }
        
        void addProgressMessage(String msg) {
            if (!abandoned) {
                host.addProgressMessage(msg);
            }
        }
        
        //whichever ends the run first, the run thread or the stop watchdog,
        //reports it to the host
        boolean claimFinish() {
            return finished.compareAndSet(false, true);
        }
        
        private volatile boolean stop;
        volatile boolean abandoned;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private Host host;
    }
}
