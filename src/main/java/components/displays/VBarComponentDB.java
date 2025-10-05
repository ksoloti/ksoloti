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
package components.displays;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import axoloti.ui.Theme;
import axoloti.utils.GraphicsUtils;

/**
 *
 * @author Johannes Taelman
 */
public class VBarComponentDB extends ADispComponent {

    private double value;
    private double max;
    private double min;

    int height = 128;
    int width = 8;

    public VBarComponentDB(double value, double min, double max) {
        this.max = max;
        this.min = min;
        this.value = value;
        Dimension d = new Dimension(width, height);
        setPreferredSize(d);
        setMaximumSize(d);
        setMinimumSize(d);
    }
    int px;

    final int margin = 2;

    int ValToPos(double v) {
        double dB = -1000;
        if (v != 0) {
            dB = 20 * Math.log10(Math.abs(v) / 64.0);
        }
        if (dB > max) {
            dB = max;
        }
        if (dB < min) {
            dB = min;
        }
        return (int) (margin + ((max - dB) * (height - 2 * margin)) / (max - min));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = GraphicsUtils.configureGraphics(g);
        g2.setPaint(Theme.Component_Background);
        g2.fillRect(0, 0, getWidth(), height);
        g2.setPaint(Theme.Component_Foreground);
        g2.drawRect(0, 0, getWidth(), height);
        int p = ValToPos(value);
        int p1 = ValToPos(0);
        g2.setPaint(Theme.Component_Mid);
        g2.fillRect(margin, p, width - margin * 2, p1 - p);
    }

    @Override
    public void setValue(double value) {
        if (this.value != value) {
            this.value = value;
            repaint();
        }
    }

    public void setMinimum(double min) {
        this.min = min;
    }

    public double getMinimum() {
        return min;
    }

    public void setMaximum(double max) {
        this.max = max;
    }

    public double getMaximum() {
        return max;
    }
}
