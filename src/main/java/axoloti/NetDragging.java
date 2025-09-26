/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2024 by Ksoloti
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */
package axoloti;

import axoloti.inlets.InletInstance;
import axoloti.outlets.OutletInstance;
import axoloti.ui.Theme;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

/**
 *
 * @author Johannes Taelman
 */
public class NetDragging extends Net {

    // private PatchGUI patchGUI;

    public NetDragging(PatchGUI patchGUI) {
        super(patchGUI);
        // this.patchGUI = patchGUI;
    }

    Point p0;

    public void SetDragPoint(Point p0) {
        this.p0 = p0;
        updateBounds();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        float shadowOffset = 1.0f;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Color c;
        g2.setStroke(strokeDragging);
        if (GetDataType() != null) {
            c = GetDataType().GetColor();
        } else {
            c = Theme.Cable_Sourceless;
        }
        if (p0 != null) {
            Point from = SwingUtilities.convertPoint(getPatchGUI().Layers, p0, this);
            Color c_shadow = c.darker().darker();
            for (OutletInstance o : source) {
                Point p1 = o.getJackLocInCanvas();
                Point to = SwingUtilities.convertPoint(getPatchGUI().Layers, p1, this);

                g2.setColor(c_shadow); /* derive wire shadow color from actual color */
                if (from.x > to.x) {
                    /* Wire goes right-to-left */
                    if (from.y > to.y) {
                        /* Wire goes upwards, i.e. starts lower than it ends */
                        DrawWire(g2, from.x - shadowOffset, from.y + shadowOffset, to.x - shadowOffset, to.y + shadowOffset);
                    }
                    else {
                        /* Wire goes downwards, i.e. starts higher than it ends */
                        DrawWire(g2, from.x + shadowOffset, from.y + shadowOffset, to.x + shadowOffset, to.y + shadowOffset);
                    }
                }
                else {
                    /* Wire goes left-to-right */
                    if (from.y > to.y) {
                        /* Wire goes upwards, i.e. starts lower than it ends */
                        DrawWire(g2, from.x + shadowOffset, from.y + shadowOffset, to.x + shadowOffset, to.y + shadowOffset);
                    }
                    else {
                        /* Wire goes downwards, i.e. starts higher than it ends */
                        DrawWire(g2, from.x - shadowOffset, from.y + shadowOffset, to.x - shadowOffset, to.y + shadowOffset);
                    }
                }
                g2.setColor(c); /* paint wire color */
                DrawWire(g2, from.x, from.y, to.x, to.y);
            }
        }
    }

    @Override
    public void updateBounds() {
        int min_y = Integer.MAX_VALUE;
        int min_x = Integer.MAX_VALUE;
        int max_y = Integer.MIN_VALUE;
        int max_x = Integer.MIN_VALUE;

        /* Create a single list to hold all points that define the net */
        ArrayList<Point> points = new ArrayList<>();
        
        /* Add the drag point if it exists */
        if (p0 != null) {
            points.add(p0);
        }
        
        /* Add all inlet points */
        for (InletInstance i : dest) {
            points.add(i.getJackLocInCanvas());
        }
        /* Add all outlet points (for a valid net, this will be just one) */
        for (OutletInstance o : source) {
            points.add(o.getJackLocInCanvas());
        }
        
        /* Iterate over the single list to find the bounds */
        for (Point p : points) {
            min_x = Math.min(min_x, p.x);
            min_y = Math.min(min_y, p.y);
            max_x = Math.max(max_x, p.x);
            max_y = Math.max(max_y, p.y);
        }

        int fudge = 8;
        this.setBounds(min_x - fudge, min_y - fudge,
                Math.max(1, max_x - min_x + (2 * fudge)),
                (int)CtrlPointY(min_x, min_y, max_x, max_y) - min_y + (2 * fudge));
    }

}
