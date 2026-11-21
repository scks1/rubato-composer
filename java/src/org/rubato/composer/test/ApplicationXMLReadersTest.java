package org.rubato.composer.test;

import java.io.StringReader;
import java.util.List;

import junit.framework.TestCase;

import org.rubato.base.Rubette;
import org.rubato.composer.network.NetworkModel;
import org.rubato.composer.network.NetworkXMLReader;
import org.rubato.rubettes.builtin.MacroRubetteXMLReader;
import org.rubato.xml.XMLReader;

/**
 * Checks that the core's XMLReader finds the application's readers for
 * networks and macro rubettes through META-INF/services.
 */
public class ApplicationXMLReadersTest extends TestCase {
	
	public void testTheServiceFileRegistersBothReaders() {
		assertTrue(XMLReader.getElementReader("Network") instanceof NetworkXMLReader);
		assertTrue(XMLReader.getElementReader("Rubette") instanceof MacroRubetteXMLReader);
	}
	
	public void testNetworksAreReadInDocumentOrder() {
		XMLReader reader = new XMLReader(new StringReader(
				"<Rubato><Network name=\"one\"></Network><Network name=\"two\"></Network></Rubato>"));
		reader.parse();
		assertFalse(reader.getErrors().toString(), reader.hasError());
		List<NetworkModel> networks = NetworkXMLReader.getNetworks(reader);
		assertEquals(2, networks.size());
		assertEquals("one", networks.get(0).getName());
		assertEquals("two", networks.get(1).getName());
		assertTrue(MacroRubetteXMLReader.getRubettes(reader).isEmpty());
	}
	
	public void testAMacroRubetteWithoutANameKeepsItsError() {
		XMLReader reader = new XMLReader(new StringReader("<Rubato><Rubette name=\"\"/></Rubato>"));
		reader.parse();
		assertTrue(reader.hasError());
		assertEquals("Rubette must have a name", reader.getErrors().get(0));
		List<Rubette> rubettes = MacroRubetteXMLReader.getRubettes(reader);
		assertTrue(rubettes.isEmpty());
	}
}
