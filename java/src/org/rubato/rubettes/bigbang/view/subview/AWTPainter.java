package org.rubato.rubettes.bigbang.view.subview;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;

import org.rubato.rubettes.util.Point2D;
import org.rubato.rubettes.util.Polygon2D;

public class AWTPainter extends AbstractPainter {
	
	//pixels drawn outside the rectangle are clipped away, so painters may skip such shapes
	private static final int CULLING_MARGIN = 2;
	
	private Graphics g;
	//the clip when the painter was made, or null if there was none
	private java.awt.Rectangle clip;
	
	public AWTPainter(Graphics g) {
		this.g = g;
		this.clip = g.getClipBounds();
		this.g.setFont(org.rubato.composer.Theme.font(Font.PLAIN, 10f/13f, new Font("Lucida Grande", Font.PLAIN, 10)));
	}
	
	@Override
	public void setColor(float[] rgba) {
		this.g.setColor(new Color(rgba[0], rgba[1], rgba[2], rgba[3]));
	}
	
	@Override
	public void drawLine(double x1, double y1, double x2, double y2) {
		this.g.drawLine(this.round(x1), this.round(y1), this.round(x2), this.round(y2));
	}
	
	@Override
	public void drawRect(double x, double y, double width, double height) {
		this.g.drawRect(this.round(x), this.round(y), this.round(width), this.round(height));
	}

	@Override
	public void fillRect(double x, double y, double width, double height) {
		this.g.fillRect(this.round(x), this.round(y), this.round(width), this.round(height));
	}
	
	@Override
	public void drawPolygon(Polygon2D p) {
		this.g.drawPolygon(this.toAWTPolygon(p));
	}

	@Override
	public void fillPolygon(Polygon2D p) {
		this.g.fillPolygon(this.toAWTPolygon(p));
	}
	
	@Override
	public void drawOval(double x, double y, double width, double height) {
		this.g.drawOval(this.round(x), this.round(y), this.round(width), this.round(height));
	}
	
	@Override
	public void fillOval(double x, double y, double width, double height) {
		this.g.fillOval(this.round(x), this.round(y), this.round(width), this.round(height));
	}
	
	@Override
	public void fillArc(double x, double y, double width, double height, double startAngle, double arcAngle) {
		this.g.fillArc(this.round(x), this.round(y), this.round(width), this.round(height), this.round(startAngle), this.round(arcAngle));
	}
	
	@Override
	public void fillObject(double x, double y, double width, double height) {
		this.fillRect(this.round(x), this.round(y), this.round(width), this.round(height));
	}
	
	@Override
	public void drawString(String str, double x, double y) {
		this.g.drawString(str, this.round(x), this.round(y));
	}
	
	@Override
	public int getStringWidth(String s) {
		Graphics2D g2D = (Graphics2D)this.g;
		return (int)g2D.getFont().getStringBounds(s, g2D.getFontRenderContext()).getWidth();
	}
	
	@Override
	public int getStringHeight(String s) {
		Graphics2D g2D = (Graphics2D)this.g;
		return (int)g2D.getFont().getStringBounds(s, g2D.getFontRenderContext()).getHeight();
	}
	
	/**
	 * Returns false if the rectangle, rounded as fillRect rounds it and widened by a margin that
	 * covers shapes rounded vertex by vertex, lies wholly outside the clip.
	 */
	@Override
	public boolean mayReach(double x, double y, double width, double height) {
		if (this.clip == null) {
			return true;
		}
		int x1 = this.round(x), y1 = this.round(y);
		int x2 = x1 + this.round(width), y2 = y1 + this.round(height);
		int minX = Math.min(x1, x2) - CULLING_MARGIN, maxX = Math.max(x1, x2) + CULLING_MARGIN;
		int minY = Math.min(y1, y2) - CULLING_MARGIN, maxY = Math.max(y1, y2) + CULLING_MARGIN;
		return maxX >= this.clip.x && minX <= this.clip.x + this.clip.width
				&& maxY >= this.clip.y && minY <= this.clip.y + this.clip.height;
	}
	
	private int round(double d) {
		return (int)Math.round(d);
	}
	
	private Polygon toAWTPolygon(Polygon2D polygon) {
		Polygon p = new Polygon();
		for (Point2D currentPoint : polygon.getVertices()) {
			p.addPoint(this.round(currentPoint.getX()), this.round(currentPoint.getY()));
		}
		return p;
	}

}
