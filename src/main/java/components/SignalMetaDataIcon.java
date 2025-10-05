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

import axoloti.datatypes.SignalMetaData;
import axoloti.ui.Theme;
import axoloti.utils.GraphicsUtils;

// import static axoloti.datatypes.SignalMetaData.bipolar;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.swing.JComponent;

/**
 *
 * @author Johannes Taelman
 */
public class SignalMetaDataIcon extends JComponent {

    private final SignalMetaData smd;

    public SignalMetaDataIcon(SignalMetaData smd) {
        this.smd = smd;
        Dimension d = new Dimension(11, 14);
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
        setBackground(Theme.Object_Default_Background);
    }
    private final int x1 = 2;
    private final int x2 = 4;
    private final int x2_5 = 6;
    private final int x3 = 8;
    private final int x4 = 11;
    private final int y1 = 11;
    private final int y2 = 2;
    private static final Stroke stroke = new BasicStroke(1.0f);
//    private static final Stroke strokeThick = new BasicStroke(2.5f);

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = GraphicsUtils.configureGraphics(g);
        g2.setStroke(stroke);
        switch (smd) {
            case rising:
                g2.setColor(getForeground());
                g2.drawLine(x1, y1, x2_5, y1); // _
                g2.drawLine(x2_5, y1, x2_5, y2); // /
                g2.drawLine(x2_5, y2, x4, y2); // -
                break;
            case falling:
                g2.setColor(getForeground());
                g2.drawLine(x1, y2, x2_5, y2); // _
                g2.drawLine(x2_5, y1, x2_5, y2); // /
                g2.drawLine(x2_5, y1, x4, y1); // -
                break;
            case risingfalling:
                g2.setColor(getForeground());
                g2.drawLine(x1, y1, x2, y1); // _
                g2.drawLine(x2, y2, x3, y2); // -
                g2.drawLine(x3, y1, x4, y1); // _
                g2.drawLine(x2, y1, x2, y2); // /
                g2.drawLine(x3, y2, x3, y1); // \
                break;
            case pulse:
                g2.setColor(getForeground());
                g2.drawLine(x1, y1, x4, y1); // __
                g2.drawLine(x2_5, y1, x2_5, y2); // |
                break;
            case bipolar:
                g2.setColor(getForeground());
                g2.drawLine(6, 3, 6, 9); // verti
                g2.drawLine(3, 6, 9, 6); // hori

                g2.drawLine(3, 11, 9, 11); // hori
/*
                 g2.drawLine(6, 3, 6, 7); // verti
                 g2.drawLine(4, 5, 8, 5); // hori
                 g2.drawLine(4, 9, 8, 9); // hori
                 */
                break;
            case positive:
                g2.setColor(getForeground());
                g2.drawLine(6, 4, 6, 10); // verti
                g2.drawLine(3, 7, 9, 7); // hori
                break;
            default:
                break;
        }
    }
}
