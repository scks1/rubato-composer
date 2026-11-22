package org.rubato.rubettes.bigbang.test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.rubato.math.matrix.RMatrix;
import org.rubato.math.yoneda.Denotator;
import org.rubato.rubettes.bigbang.BigBangRubette;
import org.rubato.rubettes.bigbang.controller.BigBangController;
import org.rubato.rubettes.bigbang.headless.BigBang;
import org.rubato.rubettes.bigbang.headless.Scores;
import org.rubato.rubettes.bigbang.model.BigBangModel;
import org.rubato.rubettes.bigbang.model.BigBangObject;
import org.rubato.rubettes.bigbang.view.model.BigBangSwingView;
import org.rubato.rubettes.bigbang.view.model.FactsViewState;
import org.rubato.rubettes.util.Point2D;

/**
 * Applies the same operations twice, once through the BigBang rubette's Swing view as a mouse
 * gesture reaches it and once through the headless BigBang class, and checks that both give the
 * same composition.
 * <p>
 * The view path starts from a new BigBangRubette, which builds its BigBangSwingView, controller
 * and model as the application does. The test selects every object in the view's display objects
 * and calls the view's transformation method with screen points, which the view converts to
 * denotator values with its FactsViewState and sends through the controller to the model. The
 * screen points come from inverting that conversion. The drag preview stays off because no drag
 * gesture is begun. The test needs a display, as the Maven build's xvfb-run gives it.
 */
public class HeadlessParityTest extends TestCase {
	
	private static final double TOLERANCE = 1e-6;
	
	private static final double[][] NOTES = {
		{0, 60, 120, 1, 0}, {1, 64, 100, 2, 1}, {2.5, 67, 90, 0.5, 0}, {4, 59, 80, 1, 2}
	};
	
	private BigBangRubette rubette;
	private BigBangModel model;
	private BigBangSwingView view;
	private FactsViewState facts;
	
	private void startView(Denotator score) throws Exception {
		BigBang.registerForms();
		this.rubette = new BigBangRubette();
		this.model = (BigBangModel)field(BigBangRubette.class, "model").get(this.rubette);
		this.view = (BigBangSwingView)field(BigBangRubette.class, "view").get(this.rubette);
		this.facts = (FactsViewState)field(org.rubato.rubettes.bigbang.view.model.BigBangView.class, "factsViewState").get(this.view);
		this.model.setOrAddComposition(score.copy());
		int[] xy = this.facts.getViewParameters().getSelectedXYViewParameters();
		assertEquals("the view shows the first value on x", 0, xy[0]);
		assertEquals("the view shows the second value on y", 1, xy[1]);
	}
	
	private void selectAll() {
		Set<BigBangObject> all = new TreeSet<BigBangObject>(this.model.getObjects().getObjectsAt(null));
		this.view.getDisplayObjects().selectObjects(all);
		assertEquals(all.size(), this.view.getDisplayObjects().getSelectedBigBangObjects().size());
	}
	
	//the screen point the view converts to the given denotator values; the conversion is affine
	//in each coordinate
	private Point2D screen(double x, double y) {
		return new Point2D(this.invert(x, 0), this.invert(y, 1));
	}
	
	private double invert(double value, int index) {
		double at0 = this.facts.getDenotatorValue(0, index);
		double at1000 = this.facts.getDenotatorValue(1000, index);
		double display = (value - at0) * 1000 / (at1000 - at0);
		assertEquals("display value " + display + " for " + value, value, this.facts.getDenotatorValue(display, index), TOLERANCE);
		return display;
	}
	
	private static Field field(Class<?> owner, String name) throws Exception {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
	
	private void assertSameComposition(String label, Denotator expected, Denotator actual) {
		assertEquals(label + ": form", expected.getForm(), actual.getForm());
		double[][] e = Scores.values(expected);
		double[][] a = Scores.values(actual);
		assertEquals(label + ": note count", e.length, a.length);
		for (int i = 0; i < e.length; i++) {
			for (int j = 0; j < e[i].length; j++) {
				assertEquals(label + ": note " + i + ", value " + j, e[i][j], a[i][j], TOLERANCE);
			}
		}
	}
	
	private void checkAllOperations(Denotator score) throws Exception {
		//translation
		this.startView(score);
		this.selectAll();
		this.view.translateSelectedObjects(this.screen(0, 0), this.screen(1.5, 7), false, true);
		this.assertSameComposition("translation", new BigBang(score).translate(1.5, 7).getComposition(), this.model.getComposition());
		
		//rotation by a quarter turn around (1, 62)
		this.startView(score);
		this.selectAll();
		Point2D center = this.screen(1, 62);
		this.view.rotateSelectedObjects(center, center, center, Math.PI / 2, false, true);
		this.assertSameComposition("rotation", new BigBang(score).rotate(1, 62, Math.PI / 2).getComposition(), this.model.getComposition());
		
		//scaling
		this.startView(score);
		this.selectAll();
		center = this.screen(2, 60);
		this.view.scaleSelectedObjects(center, center, new double[]{2, 0.5}, false, true);
		this.assertSameComposition("scaling", new BigBang(score).scale(2, 60, 2, 0.5).getComposition(), this.model.getComposition());
		
		//reflection in the line through (0, 63) along (1, 1)
		this.startView(score);
		this.selectAll();
		center = this.screen(0, 63);
		this.view.reflectSelectedObjects(center, center, new double[]{1, 1}, false, true);
		this.assertSameComposition("reflection", new BigBang(score).reflect(0, 63, 1, 1).getComposition(), this.model.getComposition());
		
		//shearing
		this.startView(score);
		this.selectAll();
		center = this.screen(1, 60);
		this.view.shearSelectedObjects(center, center, new double[]{0.5, 1}, false, true);
		this.assertSameComposition("shearing", new BigBang(score).shear(1, 60, 0.5, 1).getComposition(), this.model.getComposition());
		
		//affine map, copying
		this.startView(score);
		this.selectAll();
		center = this.screen(0, 60);
		double[][] matrix = {{1, 0.25}, {-0.5, 1}};
		this.view.affineTransformSelectedObjects(center, center, new double[]{3, 2}, new RMatrix(matrix), true, true);
		this.assertSameComposition("affine copy", new BigBang(score).copying(true).affine(0, 60, new double[]{3, 2}, matrix).getComposition(), this.model.getComposition());
		
		//a wallpaper dimension with the view's range 0 to 5 and a translation as motif, as the
		//wallpaper buttons make it
		this.startView(score);
		this.selectAll();
		this.view.addWallpaperDimension();
		this.view.translateSelectedObjects(this.screen(0, 0), this.screen(5, 0), false, true);
		((BigBangController)field(BigBangRubette.class, "controller").get(this.rubette)).endWallpaper();
		BigBang expected = new BigBang(score).addWallpaperDimension(0, 5).translate(5, 0).endWallpaper();
		this.assertSameComposition("wallpaper", expected.getComposition(), this.model.getComposition());
		
		//a sequence, then undo and redo through the view's controller
		this.startView(score);
		this.selectAll();
		this.view.translateSelectedObjects(this.screen(0, 0), this.screen(1, 2), false, true);
		this.selectAll();
		center = this.screen(0, 60);
		this.view.scaleSelectedObjects(center, center, new double[]{1, -1}, false, true);
		expected = new BigBang(score).translate(1, 2).scale(0, 60, 1, -1);
		this.assertSameComposition("sequence", expected.getComposition(), this.model.getComposition());
		this.view.undo();
		this.assertSameComposition("undo", expected.undo().getComposition(), this.model.getComposition());
		this.view.redo();
		this.assertSameComposition("redo", expected.redo().getComposition(), this.model.getComposition());
	}
	
	public void testScoreOperationsMatchTheView() throws Exception {
		this.checkAllOperations(Scores.score(NOTES));
	}
	
	public void testSoundScoreOperationsMatchTheView() throws Exception {
		this.checkAllOperations(Scores.soundScore(NOTES));
	}
	
	public void testIncrementalAndFullReplayAgreeHeadless() {
		for (Denotator score : new Denotator[]{Scores.score(NOTES), Scores.soundScore(NOTES)}) {
			BigBang incremental = new BigBang(score, true);
			BigBang full = new BigBang(score, false);
			for (BigBang b : new BigBang[]{incremental, full}) {
				b.translate(1, 3).copying(true).rotate(1, 62, 0.75).copying(false).shear(0, 60, 0.2, 0).undo().reflect(2, 62, 0, 1);
			}
			this.assertSameComposition("incremental against full", full.getComposition(), incremental.getComposition());
			this.assertSameComposition("full replay", full.getComposition(), incremental.replay());
		}
	}

}
