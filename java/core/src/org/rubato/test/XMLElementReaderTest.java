package org.rubato.test;

import java.util.List;

import junit.framework.TestCase;

import org.rubato.xml.XMLElementReader;
import org.rubato.xml.XMLReader;
import org.w3c.dom.Element;

/**
 * Checks how XMLReader reads toplevel elements that only the composer
 * application understands, with and without a registered reader.
 */
public class XMLElementReaderTest extends TestCase {
	
	private static final String FILE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<Rubato>\n"
		+ "  <Probe name=\"first\"/>\n"
		+ "  <Scheme>(define x 1)</Scheme>\n"
		+ "  <Probe name=\"second\"/>\n"
		+ "</Rubato>\n";
	
	public void testWithoutTheApplicationNetworksAreReportedNotSkipped() {
		assertNull("the core has no network reader of its own", XMLReader.getElementReader("Network"));
		XMLReader reader = new XMLReader(new java.io.StringReader(
				"<Rubato><Network name=\"n\"></Network><Rubette name=\"m\"/></Rubato>"));
		reader.parse();
		assertTrue(reader.hasError());
		List<String> errors = reader.getErrors();
		assertEquals(errors.toString(), 2, errors.size());
		assertTrue(errors.get(0), errors.get(0).contains("<Network>"));
		assertTrue(errors.get(0), errors.get(0).contains("composer application is not loaded"));
		assertTrue(errors.get(1), errors.get(1).contains("<Rubette>"));
		assertTrue(reader.getElementObjects("Network").isEmpty());
	}
	
	public void testUnknownElementsKeepTheirOldError() {
		XMLReader reader = new XMLReader(new java.io.StringReader("<Rubato><Nonsense/></Rubato>"));
		reader.parse();
		assertEquals(1, reader.getErrors().size());
		assertTrue(reader.getErrors().get(0).contains("not recognized"));
	}
	
	public void testARegisteredReaderGetsItsElementsInDocumentOrder() {
		XMLReader.registerElementReader(new XMLElementReader() {
			public String getElementName() { return "Probe"; }
			public Object fromXML(XMLReader reader, Element element) {
				return element.getAttribute("name");
			}
		});
		XMLReader reader = new XMLReader(new java.io.StringReader(FILE));
		reader.parse();
		assertFalse(reader.getErrors().toString(), reader.hasError());
		assertEquals(java.util.Arrays.asList("first", "second"), reader.getElementObjects("Probe"));
		assertEquals("(define x 1)", reader.getSchemeCode());
	}
	
	public void testANullResultIsNotKept() {
		XMLReader.registerElementReader(new XMLElementReader() {
			public String getElementName() { return "Empty"; }
			public Object fromXML(XMLReader reader, Element element) { return null; }
		});
		XMLReader reader = new XMLReader(new java.io.StringReader("<Rubato><Empty/></Rubato>"));
		reader.parse();
		assertFalse(reader.hasError());
		assertTrue(reader.getElementObjects("Empty").isEmpty());
	}
}
