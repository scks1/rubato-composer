package org.rubato.composer.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.rubato.base.AbstractRubette;
import org.rubato.base.Rubette;
import org.rubato.composer.Problem;
import org.rubato.composer.RunInfo;
import org.rubato.composer.Runner;
import org.rubato.composer.network.NetworkModel;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

/**
 * Checks the Problems lines that the Runner writes when a rubette's run() throws: an
 * Exception names its class, its message and where it was thrown, and an Error is reported
 * and ends the run.
 */
public class RunnerErrorReportingTest extends TestCase {

	private Host host;
	private Runner runner;

	protected void setUp() {
		this.host = new Host();
		this.runner = new Runner(this.host);
	}

	public void testAnExceptionNamesItsClassMessageAndPlace() throws Exception {
		Thrower t = new Thrower("ClassCastException", "Onset is not a Power");
		this.runAndWait(t);
		List<String> lines = this.lines();
		assertEquals(lines.toString(), 1, lines.size());
		String line = lines.get(0);
		assertTrue(line, line.contains("ClassCastException: Onset is not a Power"));
		assertTrue("names the throwing frame: " + line, line.contains("RunnerErrorReportingTest.java:"));
		assertTrue(line, line.contains("Thrower.make("));
		assertFalse("does not name the Runner as the place: " + line, line.contains("Runner.java"));
	}

	public void testAnExceptionWithoutAMessageNamesItsClass() throws Exception {
		this.runAndWait(new Thrower("NullPointerException", null));
		String line = this.lines().get(0);
		assertTrue(line, line.contains("NullPointerException"));
		assertFalse("no 'null' text for a missing message: " + line, line.contains("NullPointerException: null"));
		assertTrue(line, line.contains("RunnerErrorReportingTest.java:"));
	}

	public void testOnlyTheFirstLineOfAMessageIsShownAndItIsTakenLiterally() throws Exception {
		this.runAndWait(new Thrower("IllegalStateException", "cost $1 and %2 in a\\b\nsecond line"));
		String line = this.lines().get(0);
		assertTrue(line, line.contains("IllegalStateException: cost $1 and %2 in a\\b"));
		assertFalse(line, line.contains("second line"));
	}

	public void testTheRunGoesOnAfterAnException() throws Exception {
		Thrower t = new Thrower("RuntimeException", "first");
		Thrower next = new Thrower(null, null);
		this.runAndWait(t, next);
		assertEquals("a rubette after an Exception still runs", 1, next.runs.get());
		assertEquals(1, this.lines().size());
		assertEquals(1, this.host.finishCount.get());
	}

	public void testAnErrorIsReportedAndEndsTheRun() throws Exception {
		Thrower t = new Thrower("StackOverflowError", "deep");
		Thrower next = new Thrower(null, null);
		this.runAndWait(t, next);
		List<String> lines = this.lines();
		assertEquals(lines.toString(), 1, lines.size());
		String line = lines.get(0);
		assertTrue(line, line.contains("StackOverflowError: deep"));
		assertTrue(line, line.contains("RunnerErrorReportingTest.java:"));
		assertTrue("says the rest was not run: " + line, line.contains("not run"));
		assertEquals("rubettes after an Error do not run", 0, next.runs.get());
		assertEquals(1, this.host.finishCount.get());
		assertEquals(0, this.host.dialogs.size());
	}

	public void testAnErrorAfterStopAddsNoProblem() throws Exception {
		Thrower t = new Thrower("StackOverflowError", "after stop");
		t.waitForStop = true;
		this.start(t);
		assertTrue(t.started.await(5, TimeUnit.SECONDS));
		this.runner.stop();
		assertTrue(this.host.finished.await(5, TimeUnit.SECONDS));
		assertEquals(1, this.host.finishCount.get());
		assertTrue("a stopped run reports no Error problem: " + this.lines(), this.runner.getProblems().isEmpty());
	}

	public void testAnExceptionAfterStopAddsNoProblem() throws Exception {
		Thrower t = new Thrower("IllegalStateException", "after stop");
		t.waitForStop = true;
		this.start(t);
		assertTrue(t.started.await(5, TimeUnit.SECONDS));
		this.runner.stop();
		assertTrue(this.host.finished.await(5, TimeUnit.SECONDS));
		assertTrue(this.runner.getProblems().isEmpty());
	}

	public void testRubetteErrorsAreStillReportedAsTheyAre() throws Exception {
		Thrower t = new Thrower(null, null);
		t.ownError = "Input denotator is null.";
		this.runAndWait(t);
		assertEquals(java.util.Collections.singletonList("Input denotator is null."), this.lines());
	}

	public void testEveryLanguageHasBothLinesWithTheirPlaceholders() {
		for (java.util.Locale locale : new java.util.Locale[] { java.util.Locale.ENGLISH, java.util.Locale.GERMAN }) {
			java.util.ResourceBundle b = java.util.ResourceBundle.getBundle("org.rubato.composer.messages", locale);
			for (String key : new String[] { "Runner.exceptionproblem", "Runner.errorproblem" }) {
				String text = b.getString(key);
				assertTrue(locale + " " + key + ": " + text, text.contains("%1") && text.contains("%2"));
				assertFalse(locale + " " + key + " no longer only points to the console: " + text,
						text.equals("Exception occurred, see console output."));
			}
		}
	}

	private List<String> lines() {
		List<String> out = new ArrayList<String>();
		for (Problem p : this.runner.getProblems()) {
			String s = p.toString();
			//Problem.toString is network:rubette: message
			out.add(s.substring(s.indexOf(": ") + 2));
		}
		return out;
	}

	private void runAndWait(Thrower... rubettes) throws Exception {
		this.start(rubettes);
		assertTrue("the run finished", this.host.finished.await(5, TimeUnit.SECONDS));
		Thread.sleep(50);
	}

	private void start(Thrower... rubettes) {
		NetworkModel network = new NetworkModel("error test");
		ArrayList<RubetteModel> list = new ArrayList<RubetteModel>();
		for (Thrower rubette : rubettes) {
			list.add(new RubetteModel(rubette, rubette.getName()));
		}
		this.runner.setList(network, list);
		this.runner.start();
	}

	private static final class Host implements Runner.Host {
		final CountDownLatch finished = new CountDownLatch(1);
		final AtomicInteger finishCount = new AtomicInteger();
		final List<String> dialogs = new java.util.concurrent.CopyOnWriteArrayList<String>();
		public void resetProgressWindow(int max) { }
		public void addProgressMessage(String msg) { }
		public void makeProgress(int value) { }
		public void finishRun() {
			this.finishCount.incrementAndGet();
			this.finished.countDown();
		}
		public void showErrorDialog(String msg) { this.dialogs.add(msg); }
	}

	private static final class Thrower extends AbstractRubette {
		final String kind;
		final String message;
		final AtomicInteger runs = new AtomicInteger();
		final CountDownLatch started = new CountDownLatch(1);
		volatile boolean waitForStop;
		String ownError;

		Thrower(String kind, String message) {
			this.kind = kind;
			this.message = message;
		}

		public void run(RunInfo runInfo) {
			this.runs.incrementAndGet();
			this.started.countDown();
			if (this.waitForStop) {
				while (!runInfo.stopped()) {
					Thread.onSpinWait();
				}
			}
			if (this.ownError != null) {
				this.addError(this.ownError);
			}
			//made here, so that its stack trace starts in run()
			Throwable t = this.make();
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			if (t instanceof Error) {
				throw (Error) t;
			}
		}

		private Throwable make() {
			if (this.kind == null) {
				return null;
			}
			switch (this.kind) {
				case "ClassCastException": return new ClassCastException(this.message);
				case "NullPointerException": return new NullPointerException(this.message);
				case "IllegalStateException": return new IllegalStateException(this.message);
				case "RuntimeException": return new RuntimeException(this.message);
				case "StackOverflowError": return new StackOverflowError(this.message);
				default: throw new AssertionError(this.kind);
			}
		}

		public String getName() { return "Thrower"; }
		public Rubette fromXML(XMLReader reader, Element element) { return this; }
		public void toXML(XMLWriter writer) { }
		public Rubette duplicate() { return new Thrower(this.kind, this.message); }
	}
}
