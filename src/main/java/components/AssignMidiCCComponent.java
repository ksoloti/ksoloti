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

import axoloti.parameters.ParameterInstanceFrac32UMap;
import axoloti.ui.Theme;
import axoloti.utils.Constants;
import axoloti.utils.GraphicsUtils;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 *
 * @author Johannes Taelman
 */
public class AssignMidiCCComponent extends JComponent {

    private static final Dimension dim = new Dimension(16, 12);

    ParameterInstanceFrac32UMap param;

    public AssignMidiCCComponent(ParameterInstanceFrac32UMap param) {
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        setSize(dim);
        this.param = param;

        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                doPopup();
                e.consume();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });
    }

    void doPopup() {
        JPopupMenu sub1 = new JPopupMenu();
        new AssignMidiCCMenuItems(param, sub1);
        sub1.show(this, 0, getHeight() - 1);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (param.getMidiCC() >= 0) {

            Graphics2D g2 = GraphicsUtils.configureGraphics(g);
            g2.setFont(Constants.FONT);
            g2.setColor(Theme.Object_Default_Background);
            g2.fillRect(0, 1, getWidth(), getHeight());

            if (param.getMidiCC() >= 0) {
                g2.setColor(Theme.Object_Default_Foreground);
                g2.fillRect(0, 1, 9, getHeight());
                g2.setColor(Theme.Object_Default_Background);
            } else {
                g2.setColor(Theme.Object_Default_Foreground);
            }

            g2.drawString("C", 1, getHeight() - 2);
            g2.setColor(Theme.Object_Default_Foreground);

            final int rmargin = 2;
            final int vmargin = 4;
            final int htick = 2;
            int[] xp = new int[] {getWidth() - rmargin - htick * 2, getWidth() - rmargin, getWidth() - rmargin - htick};
            int[] yp = new int[] {vmargin, vmargin, vmargin + htick * 2};
            g2.fillPolygon(xp, yp, 3);
        }
    }

    public void setCC(int i) { repaint(); }
}
