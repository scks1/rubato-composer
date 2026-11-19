/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.rubato.composer;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Checks at startup that the libraries shipped as separate jars next to rubato.jar are on the
 * class path. Rubato cannot run without the required ones: when one is missing, main reports which
 * jar is missing and where it belongs, then exits with status 1.
 * <p>
 * The other separately shipped jars are optional. Without FlatLaf, Theme falls back to the platform
 * look and feel. Without Commons Math, only GeometryTools.lagrangePredictValues fails, and nothing
 * calls it. Without Xerces, JAXP uses the JDK's parser.
 */
public final class RequiredLibraries {

    /** Exit status when a required library is missing. */
    public static final int EXIT_MISSING_LIBRARY = 1;

    /**
     * A library that Rubato needs, the jar it comes in, and a class from it.
     */
    public static final class Library {
        public final String name;
        public final String jar;
        public final String probeClass;
        public final String usedBy;

        public Library(String name, String jar, String probeClass, String usedBy) {
            this.name = name;
            this.jar = jar;
            this.probeClass = probeClass;
            this.usedBy = usedBy;
        }
    }

    /** The separately shipped libraries that Rubato cannot start without. */
    public static final Library[] REQUIRED = {
        new Library("collections-generic 4.01", "collections-generic-4.01.jar", //$NON-NLS-1$ //$NON-NLS-2$
                    "org.apache.commons.collections15.Transformer", "BigBang"), //$NON-NLS-1$ //$NON-NLS-2$
    };

    private RequiredLibraries() { /* static class */ }


    /**
     * @return the libraries in the given list whose probe class the class loader cannot find
     */
    public static List<Library> findMissing(Library[] libraries, ClassLoader classLoader) {
        List<Library> missing = new ArrayList<Library>();
        for (Library library : libraries) {
            try {
                Class.forName(library.probeClass, false, classLoader);
            }
            catch (ClassNotFoundException | LinkageError e) {
                missing.add(library);
            }
        }
        return missing;
    }


    /**
     * @return the jar this class was loaded from, or null when Rubato runs from a class directory
     */
    public static File getApplicationJar() {
        try {
            URL location = RequiredLibraries.class.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(location.toURI());
            if (file.isFile()) {
                return file;
            }
        }
        catch (SecurityException | NullPointerException | URISyntaxException | IllegalArgumentException e) {
            // fall through to the class path wording
        }
        return null;
    }


    /**
     * @return the message for the given missing libraries
     */
    public static String getMessage(List<Library> missing, File applicationJar) {
        StringBuilder text = new StringBuilder();
        text.append(Messages.getString("RequiredLibraries.cannotstart")); //$NON-NLS-1$
        for (Library library : missing) {
            String line = Messages.getString("RequiredLibraries.missing") //$NON-NLS-1$
                .replace("%1", library.name).replace("%2", library.jar).replace("%3", library.usedBy); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            text.append("\n").append(line); //$NON-NLS-1$
        }
        text.append("\n\n"); //$NON-NLS-1$
        if (applicationJar != null) {
            File libraryDirectory = new File(applicationJar.getAbsoluteFile().getParentFile(), "lib"); //$NON-NLS-1$
            text.append(Messages.getString("RequiredLibraries.putinlib") //$NON-NLS-1$
                .replace("%1", libraryDirectory.getPath()).replace("%2", applicationJar.getName())); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else {
            text.append(Messages.getString("RequiredLibraries.putonclasspath")); //$NON-NLS-1$
        }
        return text.toString();
    }


    /**
     * Checks the required libraries. When one is missing, prints the message to standard error,
     * logs it, shows it in a dialog unless the environment is headless, and exits.
     */
    public static void checkOrExit() {
        List<Library> missing = findMissing(REQUIRED, RequiredLibraries.class.getClassLoader());
        if (missing.isEmpty()) {
            return;
        }
        final String message = getMessage(missing, getApplicationJar());
        System.err.println(message);
        Logger.getLogger("org.rubato.composer.composer").severe(message.replace('\n', ' ')); //$NON-NLS-1$
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(null, message,
                            Messages.getString("RequiredLibraries.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
                    }
                });
            }
            catch (Exception | Error e) {
                // the message is already on standard error and in the log
            }
        }
        System.exit(EXIT_MISSING_LIBRARY);
    }
}
