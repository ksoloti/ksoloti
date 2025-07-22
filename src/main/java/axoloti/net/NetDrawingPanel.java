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

package axoloti.net;

import javax.swing.JPanel;

import axoloti.Patch;
import axoloti.PatchGUI;
import axoloti.Theme;
import axoloti.datatypes.DataType;
import axoloti.inlets.InletInstance;
import axoloti.outlets.OutletInstance;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Stroke;

/**
 *
 * @author Ksoloti
 */
public class NetDrawingPanel extends JPanel {
    private Patch patch;
    // private DragWireOverlay dragWireOverlay; // REMOVE THIS FIELD

    public NetDrawingPanel(PatchGUI patchGUI) { // Constructor still takes PatchGUI as it's the context
        this.patch = patchGUI;
        setOpaque(false);
        this.setLayout(null); // Changed to null layout as components will be positioned manually or by parent

        // REMOVE THESE LINES: NetDrawingPanel no longer manages DragWireOverlay
        // this.dragWireOverlay = new DragWireOverlay();
        // this.add(dragWireOverlay, BorderLayout.CENTER);

        // REMOVE THIS BLOCK: DragWireOverlay's bounds are managed by PatchGUI's JLayeredPane
        // addComponentListener(new ComponentAdapter() {
        //     @Override
        //     public void componentResized(ComponentEvent e) {
        //         dragWireOverlay.setBounds(0, 0, getWidth(), getHeight());
        //     }
        // });
    }

    // REMOVE THIS METHOD: DragWireOverlay is no longer managed by NetDrawingPanel
    // public DragWireOverlay getDragWireOverlay() {
    //     return dragWireOverlay;
    // }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (patch != null && patch.GetNets() != null) {
            for (Net net : patch.GetNets()) {
                if (net.isValidNet()) {
                    DataType netDataType = net.GetDataType();

                    if (netDataType != null && !net.getPatchGui().isCableTypeEnabled(netDataType)) {
                        continue;
                    }

                    Color wireColor = (netDataType != null) ? netDataType.GetColor() : Theme.Cable_Default;
                    Stroke wireStroke = net.isSelected() ? Theme.Cable_Stroke_Valid_Selected : Theme.Cable_Stroke_Valid_Deselected;

                    OutletInstance sourceIolet = net.GetSource().isEmpty() ? null : net.GetSource().get(0);

                    if (sourceIolet != null) {
                        Point p0 = sourceIolet.getJackLocInCanvas();
                        if (p0 == null) {
                            continue;
                        }

                        for (InletInstance destIolet : net.GetDests()) {
                            if (destIolet != null) {
                                Point p1 = destIolet.getJackLocInCanvas();

                                if (p1 != null) {
                                    int shadowOffset = 2;
                                    g2.setColor(Theme.Cable_Shadow);
                                    drawWireHelper(g2, p0.x + shadowOffset, p0.y + shadowOffset, p1.x + shadowOffset, p1.y + shadowOffset);

                                    g2.setColor(wireColor);
                                    g2.setStroke(wireStroke);
                                    drawWireHelper(g2, p0.x, p0.y, p1.x, p1.y);
                                }
                            }
                        }
                    }
                }
            }
        }
        g2.dispose();
    }

    public static void drawWireHelper(Graphics2D g2, int x0, int y0, int x1, int y1) {
        QuadCurve2D.Float q = new QuadCurve2D.Float();
        float mid_x = (x0 + x1) / 2.0f;
        float ctrlPointY = (float) Net.GetCtrlPointY(x0, y0, x1, y1);
        q.setCurve(x0, y0, mid_x, ctrlPointY, x1, y1);
        g2.draw(q);
    }
}