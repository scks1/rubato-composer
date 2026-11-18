package org.rubato.composer.view2d.test;

import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import junit.framework.TestCase;

import org.rubato.composer.view2d.Polygon2D;

/**
 * Checks Polygon2D's rectangle tests against java.awt.Polygon. Polygon2D is
 * a double-precision copy of java.awt.Polygon, whose rectangle tests still
 * use the even-odd crossing count that Polygon2D used before it switched to
 * Path2D. contains(x, y, w, h) agrees with java.awt.Polygon exactly.
 * intersects(x, y, w, h) agrees except where the rectangle only touches the
 * polygon's boundary: the crossing count reports those as intersecting and
 * Path2D does not.
 */
public class Polygon2DTest extends TestCase {

	private static final int[][][] POLYGONS = {
		//convex square
		{{0, 10, 10, 0}, {0, 0, 10, 10}},
		//concave L shape
		{{0, 10, 10, 4, 4, 0}, {0, 0, 4, 4, 10, 10}},
		//self-intersecting bow tie
		{{0, 10, 0, 10}, {0, 0, 10, 10}},
		//five-pointed star drawn in one stroke (even-odd leaves the center empty)
		{{5, 8, 0, 10, 2}, {0, 10, 4, 4, 10}},
		//triangle with a horizontal and a vertical edge
		{{0, 12, 0}, {0, 0, 12}}
	};

	//rectangle origins and sizes, including half-unit offsets and rectangles whose edges lie on polygon edges
	private static final double[] ORIGINS = {-2, -0.5, 0, 0.5, 1, 2, 3.5, 4, 4.5, 5, 6, 8, 9.5, 10, 11};
	private static final double[] SIZES = {0.5, 1, 2, 3.5, 4, 6, 10, 12};

	public void testIntersectsMatchesAwtPolygonExceptBoundaryTouches() {
		int boundaryOnly = 0;
		for (int[][] p : POLYGONS) {
			Polygon awt = new Polygon(p[0], p[1], p[0].length);
			Area polygonArea = new Area(awt);
			Polygon2D poly = this.makePolygon2D(p);
			for (double x : ORIGINS) for (double y : ORIGINS) for (double w : SIZES) for (double h : SIZES) {
				boolean expected = awt.intersects(x, y, w, h);
				boolean actual = poly.intersects(x, y, w, h);
				if (expected != actual) {
					String description = this.describe("intersects", p, x, y, w, h);
					TestCase.assertTrue(description, expected);
					Area overlap = new Area(new Rectangle2D.Double(x, y, w, h));
					overlap.intersect(polygonArea);
					TestCase.assertTrue(description + " overlaps the interior", overlap.isEmpty());
					boundaryOnly++;
				}
			}
		}
		//the L shape's concave corner produces boundary-only touches in this grid
		TestCase.assertTrue(boundaryOnly > 0);
	}

	public void testIntersectsMatchesAwtPolygonOnRandomRectangles() {
		Random random = new Random(1);
		for (int[][] p : POLYGONS) {
			Polygon awt = new Polygon(p[0], p[1], p[0].length);
			Polygon2D poly = this.makePolygon2D(p);
			for (int i = 0; i < 5000; i++) {
				double x = random.nextDouble()*14-2, y = random.nextDouble()*14-2;
				double w = random.nextDouble()*8, h = random.nextDouble()*8;
				TestCase.assertEquals(this.describe("intersects", p, x, y, w, h),
						awt.intersects(x, y, w, h), poly.intersects(x, y, w, h));
				TestCase.assertEquals(this.describe("contains", p, x, y, w, h),
						awt.contains(x, y, w, h), poly.contains(x, y, w, h));
			}
		}
	}

	public void testContainsMatchesAwtPolygon() {
		for (int[][] p : POLYGONS) {
			Polygon awt = new Polygon(p[0], p[1], p[0].length);
			Polygon2D poly = this.makePolygon2D(p);
			for (double x : ORIGINS) for (double y : ORIGINS) for (double w : SIZES) for (double h : SIZES) {
				TestCase.assertEquals(this.describe("contains", p, x, y, w, h),
						awt.contains(x, y, w, h), poly.contains(x, y, w, h));
			}
		}
	}

	public void testKnownCases() {
		Polygon2D square = this.makePolygon2D(POLYGONS[0]);
		TestCase.assertTrue(square.contains(2, 2, 3, 3));
		TestCase.assertTrue(square.intersects(8, 8, 5, 5));
		TestCase.assertFalse(square.contains(8, 8, 5, 5));
		TestCase.assertFalse(square.intersects(11, 11, 2, 2));
		Polygon2D l = this.makePolygon2D(POLYGONS[1]);
		//the notch of the L is outside
		TestCase.assertFalse(l.intersects(5, 5, 2, 2));
		TestCase.assertFalse(l.contains(2, 2, 4, 4));
		TestCase.assertTrue(l.contains(1, 1, 2, 2));
	}

	private Polygon2D makePolygon2D(int[][] p) {
		double[] xs = new double[p[0].length];
		double[] ys = new double[p[1].length];
		for (int i = 0; i < xs.length; i++) {
			xs[i] = p[0][i];
			ys[i] = p[1][i];
		}
		return new Polygon2D(xs, ys, xs.length);
	}

	private String describe(String method, int[][] p, double x, double y, double w, double h) {
		return method + " polygon " + java.util.Arrays.toString(p[0]) + java.util.Arrays.toString(p[1])
				+ " rect " + x + "," + y + "," + w + "," + h;
	}

}
