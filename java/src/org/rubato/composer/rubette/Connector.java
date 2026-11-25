/*
 * Copyright (C) 2005 Gérard Milmeister
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of version 2 of the GNU General Public
 * License as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package org.rubato.composer.rubette;

import org.rubato.composer.Theme;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;


/**
 * Connectors are the the little boxes on rubettes where links
 * come in and go out.
 * There are two types of connectors: input and output.
 * Each connector can be in one of two states, namely
 * connected or unconnected, which is indicated by the
 * use of different colors.
 * 
 * @author Gérard Milmeister
 */
public class Connector {
    
    public final static int CONNECTED   = 0;
    public final static int UNCONNECTED = 1;
    
    public final static int INPUT  = 0;
    public final static int OUTPUT = 1;
    
    public final static String[] stateString = { "INPUT", "OUTPUT" }; //$NON-NLS-1$ //$NON-NLS-2$

    
    /**
     * Creates a new connector.
     * @param jrubette the rubette this connector is part of
     * @param pos      the position of the connector (1..8)
     * @param r        the size of the connector
     * @param state    the initial state of the connector
     * @param type the type of the connector
     */
    public Connector(JRubette jrubette, int pos, Rectangle r, int state, int type) {
        this.pos      = pos;
        this.r        = r;
        this.state    = state;
        this.type     = type;
        this.jrubette = jrubette;
    }
    
    
    public JRubette getJRubette() {
        return jrubette;
    }
    
    
    public int getPos() {
        return pos;
    }
    
    
    public int getState() {
        return state;
    }
    
    public int getType() {
        return type;
    }
    
    
    public void setState(int state) {
        this.state = state;
    }
    
    
    public int getLinkCount() {
        return links.size();
    }
    
    
    public JLink getLink(int i) {
        return links.get(i); 
    }
    
    
    /**
     * Adds a new link to the connector.
     * The start of end position of the link is set
     * according to the type of the connector.
     */
    public boolean addLink(JLink link) {
        if (type == OUTPUT) {
            link.moveSrc(getConnectPoint());
        }
        else {
            link.moveDest(getConnectPoint());
        }
        links.add(link);
        state = CONNECTED;
        return true;
    }
    
    
    public boolean setLink(JLink link) {
        if (type == OUTPUT) {
            links = new ArrayList<JLink>();
            link.moveSrc(getConnectPoint());
            links.add(link);
            return true;
        }
        else {
            return false;
        }
    }
        
    
    public void removeLink(JLink link) {
        links.remove(link);
        if (links.isEmpty()) {
            state = UNCONNECTED;
        }
    }
    
    
    public List<JLink> getLinks() {
        return links;
    }
    
    
    public Point getConnectPoint() {
        Point point = new Point();
        point.x = jrubette.getX()+r.x+r.width/2;
        if (type == INPUT) {
            point.y = jrubette.getY();
        }
        else {
            point.y = jrubette.getY()+jrubette.getHeight();
        }
        return point;
    }
    
    
    public boolean contains(int x, int y) {
        return r.contains(x, y);
    }
    
    
    public void setRectangle(Rectangle r) {
        this.r = r;
    }
    
    
    public void paint(Graphics2D g) {
        if (Theme.isFlat()) {
            paintFlat(g);
            return;
        }
        g.setColor(stateColor[state]);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(outlineColor);
        g.drawRect(r.x, r.y, r.width, r.height);
       
    }
    
    
    /**
     * Paints the connector for the FlatLaf themes as a circle on the box
     * edge: filled when a link is attached and a ring when none is, so the
     * two states differ in shape as well as in color.
     */
    private void paintFlat(Graphics2D g0) {
        Graphics2D g = (Graphics2D)g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            double d = r.width+1;
            double cx = r.x+r.width/2.0;
            double cy = (type == INPUT) ? r.y+r.height : r.y+1;
            Ellipse2D.Double c = new Ellipse2D.Double(cx-d/2, cy-d/2, d, d);
            if (state == CONNECTED) {
                g.setColor(stateColor[CONNECTED]);
                g.fill(c);
            }
            else {
                g.setColor(ringFill);
                g.fill(c);
                g.setColor(stateColor[UNCONNECTED]);
                g.setStroke(new BasicStroke(1.6f));
                g.draw(new Ellipse2D.Double(cx-d/2+0.8, cy-d/2+0.8, d-1.6, d-1.6));
            }
        }
        finally {
            g.dispose();
        }
    }


    public void refresh() {
        if (type == INPUT) {
            for (JLink jlink : links) {
                jlink.moveDest(getConnectPoint());
            }
        }
        else {
            for (JLink jlink : links) {
                jlink.moveSrc(getConnectPoint());
            }
        }
    }
    
    
    public String toString() {
        return "Connector["+jrubette+","+stateString[type]+","+pos+"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
    
    
    private JRubette  jrubette;
    private int       pos;
    private Rectangle r;
    private int       state;
    private int       type;
    
    private ArrayList<JLink> links = new ArrayList<JLink>();
    
    private final static Color stateColor[] = new Color[] {
        Theme.color("Rubato.connector.state0", Color.BLUE), //$NON-NLS-1$
        Theme.color("Rubato.connector.state1", Color.RED)   //$NON-NLS-1$
    };
    private final static Color ringFill = Theme.color("Rubato.connector.ringFill", Color.WHITE); //$NON-NLS-1$
    private final static Color outlineColor = Theme.color("Rubato.canvas.line", Color.BLACK); //$NON-NLS-1$
}
