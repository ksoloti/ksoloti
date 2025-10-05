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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import axoloti.ui.Theme;
import axoloti.utils.GraphicsUtils;

/**
 *
 * @author Johannes Taelman
 */
public class LedstripComponent extends ADispComponent {

    private double value;
    private final int n;
    private static final int bsize = 11;
    final Color c_off = Theme.Led_Strip_Off;
    Color c_on = Theme.Led_Strip_On;

    public LedstripComponent(int value, int n) {
        //setInheritsPopupMenu(true);
        this.value = 0;//value;
        this.n = n;
        Dimension d = new Dimension(bsize * n + 2, bsize + 2);
        setMinimumSize(d);
        setPreferredSize(d);
        setMaximumSize(d);
        setSize(d);
    }

    public LedstripComponent(int value, int n, Color color) {
        this(value, n);
        this.c_on = color;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = GraphicsUtils.configureGraphics(g);
        g2.setColor(Theme.Object_Default_Background);
        g2.fillRect(0, 0, bsize * n + 1, bsize + 1);
//        g2.setPaint(getForeground());
//        g2.drawRect(0, 0, bsize * n + 1, bsize + 1);
//        for (int i = 1; i < n; i++) {
//            g2.drawLine(bsize * i, 0, bsize * i, bsize + 1);
//        }

        int v = (int) value;
        int inset = 3;
        for (int i = 0; i < n; i++) {
            if ((v & 1) != 0) {
                g2.setColor(c_on);
            } else {
                g2.setColor(c_off);
            }
            g2.fillRect(i * bsize + inset, inset, bsize - inset - 1, bsize - inset);
            v = v >> 1;
        }
    }

    @Override
    public void setValue(double value) {
        if (this.value != value) {
            this.value = value;
            repaint();
        }
    }
}
