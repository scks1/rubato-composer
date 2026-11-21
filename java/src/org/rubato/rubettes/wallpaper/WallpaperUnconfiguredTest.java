package org.rubato.rubettes.wallpaper;

import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;

import javax.swing.JComponent;
import javax.swing.JLabel;

import junit.framework.TestCase;

import org.rubato.base.Rubette;
import org.rubato.composer.rubette.RubetteModel;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Checks a Wallpaper rubette whose input form is not set: it is read back from what it saves,
 * and its view shows a message.
 */
public class WallpaperUnconfiguredTest extends TestCase {
	
	//NetworkModel writes each rubette in a Rubette element and reads it back with fromXML
	public void testAnUnconfiguredRubetteLoadsAgain() throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		XMLWriter writer = new XMLWriter(new PrintStream(bytes, true));
		writer.open();
		writer.openBlock("Rubette", "name", "Wallpaper", "class", WallpaperRubette.class.getCanonicalName());
		new RubetteModel(new WallpaperRubette().newInstance(), "net").toXML(writer);
		writer.closeBlock();
		writer.close();
		String xml = bytes.toString("UTF-8");
		
		Element rubetteElement = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml))).getDocumentElement();
		rubetteElement = XMLReader.getChild(rubetteElement, "Rubette");
		XMLReader reader = new XMLReader(new StringReader("<Rubato></Rubato>"));
		reader.parse();
		Rubette loaded = new WallpaperRubette().fromXML(reader, rubetteElement);
		assertFalse(reader.getErrors() + " in " + xml, reader.hasError());
		assertTrue(loaded instanceof WallpaperRubette);
		assertNull(((WallpaperRubette)loaded).getTempInputForm());
	}
	
	public void testTheViewOfAnUnconfiguredRubetteShowsAMessage() {
		WallpaperRubette rubette = (WallpaperRubette)new WallpaperRubette().newInstance();
		JComponent view = rubette.getView();
		assertNotNull(view);
		assertTrue(this.labelTexts(view), this.labelTexts(view).contains("No input form is set"));
	}
	
	private String labelTexts(Container container) {
		StringBuilder texts = new StringBuilder();
		for (Component component : container.getComponents()) {
			if (component instanceof JLabel) {
				texts.append(((JLabel)component).getText()).append('\n');
			}
			if (component instanceof Container) {
				texts.append(this.labelTexts((Container)component));
			}
		}
		return texts.toString();
	}
}
