package org.rubato.rubettes.bigbang.test;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.PowerDenotator;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.view.controller.ViewController;
import org.rubato.rubettes.bigbang.view.model.DisplayObject;
import org.rubato.rubettes.bigbang.view.model.DragPreview;
import org.rubato.rubettes.bigbang.view.model.ViewParameters;
import org.rubato.rubettes.bigbang.view.subview.AWTPainter;
import org.rubato.rubettes.bigbang.view.subview.DisplayContents;
import org.rubato.rubettes.bigbang.view.subview.DisplayObjects;
import org.rubato.rubettes.util.Point;
import org.rubato.rubettes.util.Point2D;
import org.rubato.rubettes.util.SoundNoteGenerator;

/**
 * Checks that the display draws the same pixels when it skips objects outside the clip, and that
 * display objects show a drag preview's current values.
 */
public class DisplayPaintTest extends TestCase {
	
	private static final int WIDTH = 400, HEIGHT = 300;
	
	private TestObjects objects;
	//how many rectangles the culling painters rejected
	private int culled;
	
	protected void setUp() {
		this.objects = new TestObjects();
	}
	
	public void testCulledPaintMatchesFullPaintOnFlatScore() {
		this.comparePaints(this.objects.flatSoundScore);
	}
	
	public void testCulledPaintMatchesFullPaintOnScoreWithSatellites() {
		this.comparePaints(this.objects.multiLevelSoundScore);
	}
	
	public void testCulledPaintMatchesFullPaintOnLargerScore() {
		double[][] notes = new double[300][];
		for (int i = 0; i < notes.length; i++) {
			notes[i] = new double[]{i * 0.25, 36 + (i * 7) % 48, 60 + i % 60, 0.5, i % 4};
		}
		this.comparePaints(new SoundNoteGenerator().createFlatSoundScore(notes));
	}
	
	//paints at several zooms and positions, so objects lie inside, across and outside the clip,
	//with and without connectors, a selection and a drag preview. a selected anchor is left out:
	//DisplayObject.paintAnchorSelection passes a colour without opacity to setColor, which throws
	//an ArrayIndexOutOfBoundsException with or without culling
	private void comparePaints(Denotator score) {
		BigBangModel model = new BigBangModel();
		model.setOrAddComposition(score.copy());
		DisplayObjects displayObjects = new DisplayObjects(model.getObjects());
		displayObjects.addObjects(model.getObjects().getObjectsAt(null));
		DisplayContents contents = this.display(model, displayObjects);
		int compared = 0;
		double[][] zooms = {{5, 5}, {20, 8}, {60, 30}};
		int[][] positions = {{20, 660}, {-40, 300}, {150, 900}, {-300, 1400}};
		for (double[] zoom : zooms) {
			for (int[] position : positions) {
				contents.setZoomFactors(zoom);
				contents.setPosition(new Point(position[0], position[1]));
				for (int variant = 0; variant < 4; variant++) {
					contents.setSatellitesConnected(variant % 2 == 1);
					displayObjects.deselectAllObjects();
					displayObjects.setDragPreview(null);
					if (variant >= 2) {
						this.selectEveryThird(model, displayObjects);
						displayObjects.setDragPreview(this.preview(model, 3 * zoom[0], -2));
					}
					contents.setObjects(displayObjects);
					assertSamePixels(this.paint(contents, true), this.paint(contents, false), "zoom " + zoom[0] + " position " + position[0] + "," + position[1] + " variant " + variant);
					compared++;
				}
			}
		}
		assertEquals(48, compared);
		assertTrue("some objects lay outside the clip", this.culled > 0);
	}
	
	private DisplayContents display(BigBangModel model, DisplayObjects displayObjects) {
		ViewParameters parameters = new ViewParameters(new ViewController(), true);
		parameters.initSelections(model.getObjects().getCoordinateSystemValueNames().size());
		parameters.setDenotatorMinAndMaxValues(model.getObjects().getMinValues(), model.getObjects().getMaxValues());
		DisplayContents contents = new DisplayContents();
		contents.setViewParameters(parameters);
		contents.setPosition(new Point(20, 660));
		contents.setZoomFactors(new double[]{5, 5});
		contents.setObjects(displayObjects);
		contents.setCurrentSize(WIDTH, HEIGHT);
		return contents;
	}
	
	private void selectEveryThird(BigBangModel model, DisplayObjects displayObjects) {
		TreeSet<BigBangObject> selected = new TreeSet<BigBangObject>();
		int i = 0;
		for (BigBangObject object : model.getObjects().getObjectsAt(null)) {
			if (i++ % 3 == 0) {
				selected.add(object);
			}
		}
		displayObjects.selectObjects(selected);
	}
	
	private DragPreview preview(BigBangModel model, double dx, double dy) {
		List<String> names = model.getObjects().getCoordinateSystemValueNames();
		DragPreview preview = DragPreview.create(model.getObjects(), new TreeSet<BigBangObject>(), false,
				new String[]{names.get(0), names.get(1)}, new int[]{0, 0});
		assertNotNull(preview);
		preview.update(new double[]{1, 0, 0, 1, dx, dy});
		return preview;
	}
	
	private BufferedImage paint(DisplayContents contents, boolean culling) {
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setClip(0, 0, WIDTH, HEIGHT);
		contents.paint(culling ? new CountingPainter(g) : new UnculledPainter(g), WIDTH, HEIGHT);
		g.dispose();
		return image;
	}
	
	private static void assertSamePixels(BufferedImage expected, BufferedImage actual, String message) {
		int painted = 0;
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				assertEquals(message + " at " + x + "," + y, expected.getRGB(x, y), actual.getRGB(x, y));
				if (expected.getRGB(x, y) != 0) {
					painted++;
				}
			}
		}
		//every view in these checks shows at least the axes
		assertTrue(message + ": nothing painted", painted > 0);
	}
	
	public void testMayReachOnlyRejectsRectanglesOutsideTheClip() {
		BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setClip(10, 10, 80, 30);
		AWTPainter painter = new AWTPainter(g);
		assertTrue(painter.mayReach(20, 20, 5, 5));
		assertTrue("across the left edge", painter.mayReach(5, 20, 6, 5));
		assertTrue("within the margin", painter.mayReach(4, 20, 5, 5));
		assertFalse("left of the clip", painter.mayReach(0, 20, 5, 5));
		assertFalse("below the clip", painter.mayReach(20, 45, 5, 3));
		assertTrue("negative width reaching in", painter.mayReach(15, 20, -10, 5));
		assertFalse("negative width outside", painter.mayReach(5, 20, -3, 5));
		g.dispose();
		Graphics2D unclipped = image.createGraphics();
		assertTrue("no clip", new AWTPainter(unclipped).mayReach(-500, -500, 1, 1));
		unclipped.dispose();
	}
	
	public void testDisplayObjectsShowTheCurrentPreviewValues() {
		PowerDenotator score = this.objects.flatSoundScore;
		BigBangModel model = new BigBangModel();
		model.setOrAddComposition(score.copy());
		DisplayObjects displayObjects = new DisplayObjects(model.getObjects());
		displayObjects.addObjects(model.getObjects().getObjectsAt(null));
		DisplayContents contents = this.display(model, displayObjects);
		contents.centerView();
		String onset = model.getObjects().getCoordinateSystemValueNames().get(0);
		DisplayObject first = displayObjects.getObjectAt(this.firstCenter(displayObjects, model));
		assertNotNull(first);
		double base = first.getBigBangObject().getNthValue(onset, 0);
		
		DragPreview preview = this.preview(model, 2, 0);
		displayObjects.setDragPreview(preview);
		assertEquals(base + 2, first.getNthValue(onset, 0), 1e-9);
		preview.update(new double[]{1, 0, 0, 1, 7, 0});
		assertEquals("the same preview, updated", base + 7, first.getNthValue(onset, 0), 1e-9);
		DragPreview other = this.preview(model, -1, 0);
		displayObjects.setDragPreview(other);
		assertEquals("another preview", base - 1, first.getNthValue(onset, 0), 1e-9);
		displayObjects.setDragPreview(null);
		assertEquals("no preview", base, first.getNthValue(onset, 0), 1e-9);
		contents.setObjects(displayObjects);
	}
	
	private Point2D firstCenter(DisplayObjects displayObjects, BigBangModel model) {
		for (int x = 0; x < WIDTH; x++) {
			for (int y = 0; y < HEIGHT; y++) {
				if (displayObjects.getObjectAt(new Point2D(x, y)) != null) {
					return new Point2D(x, y);
				}
			}
		}
		fail("no object on screen");
		return null;
	}
	
	private final class CountingPainter extends AWTPainter {
		CountingPainter(Graphics g) {
			super(g);
		}
		@Override
		public boolean mayReach(double x, double y, double width, double height) {
			boolean reaches = super.mayReach(x, y, width, height);
			if (!reaches) {
				DisplayPaintTest.this.culled++;
			}
			return reaches;
		}
	}
	
	//paints everything, as the display did before it skipped objects outside the clip
	private static final class UnculledPainter extends AWTPainter {
		UnculledPainter(Graphics g) {
			super(g);
		}
		@Override
		public boolean mayReach(double x, double y, double width, double height) {
			return true;
		}
	}

}
