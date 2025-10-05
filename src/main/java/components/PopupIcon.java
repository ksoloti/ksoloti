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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;

import axoloti.ui.Theme;
import axoloti.utils.GraphicsUtils;

/**
 *
 * @author Johannes Taelman
 */
public class PopupIcon extends JComponent implements MouseListener {

    public interface PopupIconListener {
        void ShowPopup();
    }

    private PopupIconListener pl;

    private final Dimension minsize = new Dimension(9, 12);
    private final Dimension maxsize = new Dimension(9, 12);

    public PopupIcon() {
        setMinimumSize(minsize);
        setPreferredSize(maxsize);
        setMaximumSize(maxsize);
        setSize(minsize);
        addMouseListener(this);
    }

    public void setPopupIconListener(PopupIconListener pl) {
        this.pl = pl;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = GraphicsUtils.configureGraphics(g);
        g2.setColor(Theme.Object_TitleBar_Foreground);
        final int rmargin = 3;
        final int htick = 3;
        final int xw = getWidth()+1;
        int[] xp = new int[]{xw - rmargin - htick * 2, xw - rmargin, xw - rmargin - htick};
        final int vmargin = 3;
        int[] yp = new int[]{vmargin, vmargin, vmargin + htick * 2};
        if (isEnabled()) {
            g2.fillPolygon(xp, yp, 3);
        } else {
            g2.drawPolygon(xp, yp, 3);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        pl.ShowPopup();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
