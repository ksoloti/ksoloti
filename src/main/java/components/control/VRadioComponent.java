/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
package components.control;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import axoloti.ui.Theme;
import axoloti.utils.GraphicsUtils;

/**
 *
 * @author Johannes Taelman
 */
public class VRadioComponent extends HRadioComponent {

    public VRadioComponent(int value, int n) {
        super(value, n);
        bsize = 12;
    }

    @Override
    int mousePosToVal(int x, int y) {
        int i = y / bsize;
        if (i < 0) {
            return 0;
        }
        if (i > n - 1) {
            return n - 1;
        }
        return i;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = GraphicsUtils.configureGraphics(g);
        if (isEnabled()) {
            g2.setColor(getBackground());
        } else {
            g2.setColor(Theme.Object_Default_Background);
        }
        for (int i = 0; i < n; i++) {
            g2.fillOval(0, i * bsize, bsize, bsize);
        }
        if (isEnabled()) {
            g2.setPaint(getForeground());
        } else {
            g2.setPaint(Theme.Component_Mid);
        }
        if (isFocusOwner()) {
            g2.setStroke(strokeThick);
        }

        for (int i = 0; i < n; i++) {
            g2.drawOval(0, i * bsize, bsize, bsize);
        }

        if (isEnabled()) {
            g2.fillOval(3, (int) value * bsize + 3, bsize - 5, bsize - 5);
        } else {
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(bsize + 2, bsize * n + 2);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(bsize + 2, bsize * n + 2);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(bsize + 2, bsize * n + 2);
    }

    public int getN() {
        return n;
    }
}
