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

import axoloti.inlets.InletInstance;
import axoloti.utils.GraphicsUtils;

import java.awt.BasicStroke;
import java.awt.Color;
// import static java.awt.Component.CENTER_ALIGNMENT;
// import static java.awt.Component.RIGHT_ALIGNMENT;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.swing.JComponent;

/**
 *
 * @author Johannes Taelman
 */
public class JackInputComponent extends JComponent {

    private static final int sz = 10;
    private static final int margin = 2;
    private static final int doubleMargin = margin * 2;
    private static final Dimension dim = new Dimension(sz, sz);
    final InletInstance inlet;

    public JackInputComponent(InletInstance inlet) {
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        setSize(dim);
        setAlignmentY(CENTER_ALIGNMENT);
        setAlignmentX(RIGHT_ALIGNMENT);
        this.inlet = inlet;
    }
    private final Stroke stroke = new BasicStroke(1.5f);

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = GraphicsUtils.configureGraphics(g);
        g2.setStroke(stroke);
        g2.setPaint(Color.BLACK);
        g2.drawOval(margin + 1, margin + 1, sz - doubleMargin, sz - doubleMargin);
        g2.setPaint(getForeground());
        if (inlet.isConnected()) {
            g2.fillOval(margin, margin, sz - doubleMargin +1 , sz - doubleMargin + 1);
        }
        g2.drawOval(margin, margin, sz - doubleMargin, sz - doubleMargin);
    }
    
    public InletInstance getInlet() {
        return inlet;
    }
}
