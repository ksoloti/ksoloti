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
package components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

import axoloti.ui.Theme;
import axoloti.utils.GraphicsUtils;

/**
 *
 * @author Johannes Taelman
 */
public class RControlColorLed extends JComponent {

    Color color = Theme.Component_Background;

    public void setColor(Color color) {
        if (this.color != color) {
            this.color = color;
            repaint();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        int height = getHeight();
        int width = getWidth();

        int diameter = (height > width ? width : height) - 2;
        int hoffset = (width - diameter) / 2;
        int voffset = (height - diameter) / 2;

        Graphics2D g2 = GraphicsUtils.configureGraphics(g);
        g2.setPaint(Theme.Object_Default_Background);
        g2.fillRect(0, 0, width, height);
        g2.setPaint(Theme.Component_Background);
        g2.drawOval(hoffset, voffset, diameter, diameter);
        g2.setPaint(color);
        g2.fillOval(hoffset, voffset, diameter, diameter);
    }
}
