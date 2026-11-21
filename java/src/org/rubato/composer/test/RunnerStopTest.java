package org.rubato.composer.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.rubato.base.AbstractRubette;
import org.rubato.base.Rubette;
import org.rubato.composer.RunInfo;
import org.rubato.composer.Runner;
import org.rubato.composer.network.NetworkModel;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

/**
 * Checks that the stop button halts a running network in each of its three
 * steps: the stop flag, an interrupt, and abandoning a thread that ignores both.
 */
public class RunnerStopTest extends TestCase {
	
	private RecordingHost host;
	private Runner runner;
	
	protected void setUp() {
		this.host = new RecordingHost();
		this.runner = new Runner(this.host);
	}
	
	public void testFirstStopEndsARubetteThatChecksTheFlag() throws Exception {
		TestRubette polling = new TestRubette(TestRubette.POLL);
		TestRubette next = new TestRubette(TestRubette.POLL);
		this.start(polling, next);
		assertTrue("the rubette started", polling.started.await(5, TimeUnit.SECONDS));
		this.runner.stop();
		assertTrue("the run finished", this.host.finished.await(5, TimeUnit.SECONDS));
		assertEquals("rubettes after the stopped one do not run", 0, next.runs.get());
		assertEquals(1, this.host.finishCount.get());
		assertEquals(0, this.host.dialogs.size());
		assertTrue(this.runner.getProblems().isEmpty());
	}
	
	public void testSecondStopInterruptsABlockingRubette() throws Exception {
		TestRubette sleeping = new TestRubette(TestRubette.SLEEP);
		this.start(sleeping);
		assertTrue(sleeping.started.await(5, TimeUnit.SECONDS));
		this.runner.stop();
		assertFalse("the flag alone does not wake a sleeping rubette", this.host.finished.await(300, TimeUnit.MILLISECONDS));
		this.runner.stop();
		assertTrue("the interrupt ends the sleep", this.host.finished.await(5, TimeUnit.SECONDS));
		assertEquals(1, this.host.finishCount.get());
		assertEquals(0, this.host.dialogs.size());
		assertTrue("a stopped run reports no exception problem", this.runner.getProblems().isEmpty());
	}
	
	public void testThirdStopAbandonsARubetteThatIgnoresStopAndInterrupts() throws Exception {
		TestRubette runaway = new TestRubette(TestRubette.SPIN);
		TestRubette next = new TestRubette(TestRubette.POLL);
		this.start(runaway, next);
		assertTrue(runaway.started.await(5, TimeUnit.SECONDS));
		long before = System.nanoTime();
		this.runner.stop();
		this.runner.stop();
		//on JDK 20 and later the old third click called Thread.stop(), which throws
		//UnsupportedOperationException here
		this.runner.stop();
		long stopMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
		assertTrue("stop returns without waiting for the grace period, took " + stopMillis + " ms", stopMillis < Runner.GRACE_PERIOD_MS);
		assertTrue("the run is finished after the grace period", this.host.finished.await(Runner.GRACE_PERIOD_MS + 5000, TimeUnit.MILLISECONDS));
		assertEquals(1, this.host.finishCount.get());
		//the watchdog calls finishRun before showErrorDialog, so wait for the dialog too
		assertTrue("the user is told the network was abandoned", this.host.dialogShown.await(5, TimeUnit.SECONDS));
		assertEquals(1, this.host.dialogs.size());
		assertFalse(this.runner.isRunning());
		assertTrue("the abandoned thread is a daemon", runaway.daemon);
		assertTrue("the abandoned thread is still spinning", runaway.thread.isAlive());
		int progressBefore = this.host.progressCount.get();
		
		runaway.release = true;
		runaway.thread.join(5000);
		assertFalse(runaway.thread.isAlive());
		assertEquals("the abandoned thread does not finish the run again", 1, this.host.finishCount.get());
		assertEquals("the abandoned thread reports no progress", progressBefore, this.host.progressCount.get());
		assertEquals("rubettes after the abandoned one do not run", 0, next.runs.get());
	}
	
	public void testARunAfterAnAbandonedRunWorks() throws Exception {
		TestRubette runaway = new TestRubette(TestRubette.SPIN);
		this.start(runaway);
		assertTrue(runaway.started.await(5, TimeUnit.SECONDS));
		this.runner.stop();
		this.runner.stop();
		this.runner.stop();
		assertTrue(this.host.finished.await(Runner.GRACE_PERIOD_MS + 5000, TimeUnit.MILLISECONDS));
		
		this.host.finished = new CountDownLatch(1);
		TestRubette quick = new TestRubette(TestRubette.RETURN);
		this.start(quick);
		assertTrue(this.host.finished.await(5, TimeUnit.SECONDS));
		assertEquals(1, quick.runs.get());
		assertEquals(2, this.host.finishCount.get());
		runaway.release = true;
		runaway.thread.join(5000);
		assertEquals("the old thread ending does not finish the new run", 2, this.host.finishCount.get());
	}
	
	public void testAnErrorInARubetteStillFinishesTheRun() throws Exception {
		TestRubette failing = new TestRubette(TestRubette.ERROR);
		this.start(failing);
		assertTrue("the progress window is closed after an Error", this.host.finished.await(5, TimeUnit.SECONDS));
		assertEquals(1, this.host.finishCount.get());
	}
	
	public void testStopBeforeStartDoesNothing() {
		this.runner.stop();
		this.runner.stop();
		this.runner.stop();
		assertEquals(0, this.host.finishCount.get());
	}
	
	private void start(TestRubette... rubettes) {
		NetworkModel network = new NetworkModel("stop test");
		ArrayList<RubetteModel> list = new ArrayList<RubetteModel>();
		for (TestRubette rubette : rubettes) {
			list.add(new RubetteModel(rubette, rubette.getName()));
		}
		this.runner.setList(network, list);
		this.runner.start();
	}
	
	private static final class RecordingHost implements Runner.Host {
		volatile CountDownLatch finished = new CountDownLatch(1);
		final AtomicInteger finishCount = new AtomicInteger();
		final AtomicInteger progressCount = new AtomicInteger();
		final List<String> dialogs = new java.util.concurrent.CopyOnWriteArrayList<String>();
		final CountDownLatch dialogShown = new CountDownLatch(1);
		public void resetProgressWindow(int max) { }
		public void addProgressMessage(String msg) { }
		public void makeProgress(int value) { this.progressCount.incrementAndGet(); }
		public void finishRun() {
			this.finishCount.incrementAndGet();
			this.finished.countDown();
		}
		public void showErrorDialog(String msg) {
			this.dialogs.add(msg);
			this.dialogShown.countDown();
		}
	}
	
	private static final class TestRubette extends AbstractRubette {
		static final int POLL = 0, SLEEP = 1, SPIN = 2, RETURN = 3, ERROR = 4;
		final int behaviour;
		final CountDownLatch started = new CountDownLatch(1);
		final AtomicInteger runs = new AtomicInteger();
		volatile boolean release;
		volatile Thread thread;
		volatile boolean daemon;
		
		TestRubette(int behaviour) {
			this.behaviour = behaviour;
		}
		
		public void run(RunInfo runInfo) {
			this.runs.incrementAndGet();
			this.thread = Thread.currentThread();
			this.daemon = this.thread.isDaemon();
			this.started.countDown();
			switch (this.behaviour) {
				case POLL:
					while (!runInfo.stopped()) {
						Thread.onSpinWait();
					}
					break;
				case SLEEP:
					try {
						Thread.sleep(60000);
					}
					catch (InterruptedException e) {
						throw new RuntimeException("interrupted", e);
					}
					break;
				case SPIN:
					//ignores the flag and swallows interrupts, as a runaway computation would
					while (!this.release) {
						Thread.interrupted();
						Thread.onSpinWait();
					}
					break;
				case ERROR:
					throw new StackOverflowError("test");
				default:
					break;
			}
		}
		
		public String getName() { return "Test" + this.behaviour; }
		public Rubette fromXML(XMLReader reader, Element element) { return this; }
		public void toXML(XMLWriter writer) { }
		public Rubette duplicate() { return new TestRubette(this.behaviour); }
	}
}
