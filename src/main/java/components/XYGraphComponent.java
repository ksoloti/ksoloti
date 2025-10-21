/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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
package components;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.*;

import axoloti.ui.Theme;
import axoloti.utils.GraphicsUtils;

public class XYGraphComponent extends JComponent {

    private final int length; 

    private final int width;
    private final int height;

    private final double xMaxData;
    private final double xMinData;
    private final double yMaxData;
    private final double yMinData;

    private final int xImax;
    private final int xImin;
    private final int yImax;
    private final int yImin;

    private int[] xpoints;
    private int[] ypoints;

    private int xCenter;
    private int yCenter;

    public XYGraphComponent(int length, int width, int height, double xMin, double xMax, double yMin, double yMax) {
        this.length = length;
        this.width = width;
        this.height = height;

        this.xMaxData = xMax;
        this.xMinData = xMin;
        this.yMaxData = yMax;
        this.yMinData = yMin;

        this.xImax = (int) xMax;
        this.xImin = (int) xMin;
        this.yImax = (int) yMax;
        this.yImin = (int) yMin;

        this.xpoints = new int[length];
        this.ypoints = new int[length];

        xCenter = xValToPos(0, width, xMin, xMax, xImin, xImax);
        yCenter = yValToPos(0, height, yMin, yMax, yImin, yImax);

        Dimension d = new Dimension(width + 2, height + 2);
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
    }

    private static final Stroke strokeThin = new BasicStroke(0.75f);
    private static final Stroke strokeThick = new BasicStroke(1.0f);

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = GraphicsUtils.configureGraphics(g);

        g2.setStroke(strokeThick);
        g2.setColor(Theme.Component_Background);
        g2.fillRect(0, 0, width + 1, height + 1);

        g2.setPaint(Theme.Component_Mid_Light);
        g2.drawLine(0, yCenter, width, yCenter);
        g2.drawLine(xCenter, 0, xCenter, height);

        g2.setPaint(Theme.Component_Foreground);
        g2.drawRect(0, 0, width + 1, height + 1);

        g2.setStroke(strokeThin);
        g2.setColor(Theme.Component_Foreground);

        if (xpoints.length > 0) {
            g2.drawPolyline(xpoints, ypoints, length);
        }
    }

    private int xValToPos(int x, int targetWidth, double min, double max, int imin, int imax) {
        if (x < imin) {
            x = imin;
        }
        if (x > imax) {
            x = imax;
        }
        return (int) Math.round((double) ((x - min) * targetWidth) / (max - min));
    }

    private int yValToPos(int y, int targetHeight, double min, double max, int imin, int imax) {
        if (y < imin) {
            y = imin;
        }
        if (y > imax) {
            y = imax;
        }
        return (int) Math.round((double) ((max - y) * targetHeight) / (max - min));
    }

    public void setValue(int xData[], int yData[]) {
        if (xData.length != length || yData.length != length) {
            return;
        }

        for (int i = 0; i < length; i++) {
            this.xpoints[i] = xValToPos(xData[i], width, xMinData, xMaxData, xImin, xImax);
        }

        for (int i = 0; i < length; i++) {
            this.ypoints[i] = yValToPos(yData[i], height, yMinData, yMaxData, yImin, yImax);
        }
        repaint();
    }
}
