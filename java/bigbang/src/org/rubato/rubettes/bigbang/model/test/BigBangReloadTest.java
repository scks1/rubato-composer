package org.rubato.rubettes.bigbang.model.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.Scores;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.xml.XMLReader;
import org.rubato.xml.XMLWriter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Saves a BigBang model whose graph holds transformations, opens the saved XML again and
 * checks that replaying the loaded graph gives the composition the model had before saving.
 */
public class BigBangReloadTest extends TestCase {
	
	private static final double TOLERANCE = 1e-9;
	private static final double[][] NOTES = {
		{0, 60, 120, 1, 0}, {1, 62, 100, 2, 1}, {2, 64, 90, 1, 0}, {3, 65, 80, 0.5, 1}};
	
	private static BigBang transformed(Denotator input) {
		BigBang bigBang = new BigBang(input);
		bigBang.translate(1, 3)
				.rotate(1, 62, Math.PI/6)
				.scale(1, 62, 1.5, 0.75)
				.reflect(1, 62, 1, 1)
				.shear(1, 62, 0.25, 0)
				.copying(true).affine(0, 60, new double[]{3, 2}, new double[][]{{1, 0.25}, {-0.5, 1}})
				.copying(false)
				.addWallpaperDimension(0, 2)
				.translate(4, 0)
				.endWallpaper();
		return bigBang;
	}
	
	private static String save(BigBangModel model) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		XMLWriter writer = new XMLWriter(new PrintStream(bytes, true, StandardCharsets.UTF_8));
		writer.open();
		model.toXML(writer);
		writer.close();
		return bytes.toString(StandardCharsets.UTF_8);
	}
	
	private static class Captured {
		BigBangModel model;
		String stderr;
		List<LogRecord> warnings = new ArrayList<LogRecord>();
	}
	
	private static Captured load(String xml) throws Exception {
		Captured captured = new Captured();
		Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml))).getDocumentElement();
		XMLReader reader = new XMLReader(new StringReader(xml));
		reader.parse();
		Logger root = Logger.getLogger("");
		Handler handler = new Handler() {
			public void publish(LogRecord record) {
				if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
					captured.warnings.add(record);
				}
			}
			public void flush() { }
			public void close() { }
		};
		root.addHandler(handler);
		PrintStream err = System.err;
		ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
		System.setErr(new PrintStream(errBytes, true, StandardCharsets.UTF_8));
		try {
			captured.model = BigBangModel.fromXML(reader, element);
		} finally {
			System.setErr(err);
			root.removeHandler(handler);
		}
		captured.stderr = errBytes.toString(StandardCharsets.UTF_8);
		return captured;
	}
	
	private static void assertSameNotes(String label, Denotator expected, Denotator actual) {
		double[][] want = Scores.values(expected);
		double[][] got = Scores.values(actual);
		assertEquals(label + ": note count in " + Arrays.deepToString(got), want.length, got.length);
		for (int i = 0; i < want.length; i++) {
			assertEquals(label + ": note " + i, want[i].length, got[i].length);
			for (int j = 0; j < want[i].length; j++) {
				assertEquals(label + ": note " + i + ", value " + j, want[i][j], got[i][j], TOLERANCE);
			}
		}
	}
	
	private void checkReload(Denotator input) throws Exception {
		String label = input.getForm().getNameString();
		BigBang bigBang = transformed(input);
		Denotator before = bigBang.getComposition();
		assertEquals(label, 10, bigBang.getOperationCount());
		assertEquals(label + ": the copying affine map and the wallpaper add notes", 24, Scores.values(before).length);
		
		Captured loaded = load(save(bigBang.getModel()));
		assertEquals(label + ": stderr while loading", "", loaded.stderr);
		assertTrue(label + ": warnings while loading: " + loaded.warnings, loaded.warnings.isEmpty());
		assertEquals(label, 10, loaded.model.getTransformationGraph().getEdgeCount());
		assertSameNotes(label, before, loaded.model.getComposition());
		assertEquals(label, before, loaded.model.getComposition());
	}
	
	public void testScoreWithTransformationsReloadsToTheSameComposition() throws Exception {
		this.checkReload(Scores.score(NOTES));
	}
	
	public void testSoundScoreWithTransformationsReloadsToTheSameComposition() throws Exception {
		this.checkReload(Scores.soundScore(NOTES));
	}
	
	public void testAffineParametersRoundTrip() throws Exception {
		BigBang bigBang = new BigBang(Scores.score(NOTES));
		bigBang.affine(0, 60, new double[]{3, 2}, new double[][]{{1, 0.25}, {-0.5, 1}});
		String xml = save(bigBang.getModel());
		assertTrue(xml, xml.contains("<Affine shift=\"[3.0, 2.0]\" rows=\"2\" matrix=\"[1.0, 0.25, -0.5, 1.0]\"/>"));
		Captured loaded = load(xml);
		assertEquals("", loaded.stderr);
		assertSameNotes("affine", bigBang.getComposition(), loaded.model.getComposition());
		String resaved = save(loaded.model);
		assertEquals(affineElement(xml), affineElement(resaved));
	}
	
	private static String affineElement(String xml) {
		int start = xml.indexOf("<Affine ");
		assertTrue(xml, start >= 0);
		return xml.substring(start, xml.indexOf("/>", start) + 2);
	}
	
	/**
	 * Files written before the Affine element existed hold no parameters for the operation. They
	 * load without exceptions, with the affine map as the identity, and a warning names the loss.
	 */
	public void testAffineSavedWithoutParametersLoadsAsIdentityWithAWarning() throws Exception {
		BigBang bigBang = new BigBang(Scores.score(NOTES));
		bigBang.translate(1, 3);
		Denotator translated = bigBang.getComposition();
		bigBang.affine(0, 60, new double[]{3, 2}, new double[][]{{1, 0.25}, {-0.5, 1}});
		String xml = save(bigBang.getModel()).replaceAll("(?m)^\\s*<Affine [^>]*/>\\R", "");
		assertFalse(xml, xml.contains("<Affine"));
		Captured loaded = load(xml);
		assertEquals("", loaded.stderr);
		assertEquals(loaded.warnings.toString(), 1, loaded.warnings.size());
		assertTrue(loaded.warnings.get(0).getMessage(), loaded.warnings.get(0).getMessage().contains("identity"));
		assertSameNotes("identity affine", translated, loaded.model.getComposition());
	}
	
}
