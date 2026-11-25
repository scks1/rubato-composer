package org.rubato.base.test;

import junit.framework.TestCase;

import org.rubato.base.AbstractRubette;
import org.rubato.base.Rubette;
import org.rubato.composer.RunInfo;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;

/**
 * Checks AbstractRubette.addError(Exception), which rubettes such as MacroRubette call from
 * their catch blocks, for exceptions with a null, empty or blank message.
 */
public class AddErrorExceptionTest extends TestCase {

	private Plain rubette;

	protected void setUp() {
		this.rubette = new Plain();
	}

	public void testANullMessageFallsBackToTheClassName() {
		this.rubette.addError(new NullPointerException());
		assertEquals("NullPointerException.", this.only());
	}

	public void testAnEmptyMessageFallsBackToTheClassName() {
		this.rubette.addError(new IllegalStateException(""));
		assertEquals("IllegalStateException.", this.only());
	}

	public void testABlankMessageFallsBackToTheClassName() {
		this.rubette.addError(new org.rubato.base.RubatoException("   "));
		assertEquals("RubatoException.", this.only());
	}

	public void testAnAnonymousExceptionClassFallsBackToItsFullName() {
		Exception e = new Exception() { };
		this.rubette.addError(e);
		assertEquals(e.getClass().getName() + ".", this.only());
	}

	public void testAMessageGetsAPeriodAsBefore() {
		this.rubette.addError(new org.rubato.base.RubatoException(" Input is wrong "));
		assertEquals("Input is wrong.", this.only());
	}

	public void testAMessageWithAPeriodIsKept() {
		this.rubette.addError(new org.rubato.base.RubatoException("Input is wrong."));
		assertEquals("Input is wrong.", this.only());
	}

	private String only() {
		assertEquals(this.rubette.getErrors().toString(), 1, this.rubette.getErrors().size());
		return this.rubette.getErrors().get(0);
	}

	private static final class Plain extends AbstractRubette {
		public void run(RunInfo runInfo) { }
		public String getName() { return "Plain"; }
		public Rubette fromXML(XMLReader reader, Element element) { return this; }
		public void toXML(XMLWriter writer) { }
		public Rubette duplicate() { return new Plain(); }
	}
}
