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

import axoloti.Theme;
import axoloti.utils.Constants;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;
import javax.swing.event.MouseInputAdapter;

/**
 *
 * @author Johannes Taelman
 */
public abstract class PianoComponent extends JComponent {

    final int height = 50;
    final int blackKeyHeight = height / 2;
    final int width = 900;
    final int quarterKeyWidth = 3;
    final int KeyWidth = quarterKeyWidth * 2;

    final int keyx[] = {0, 3, 4, 7, 8, 12, 15, 16, 19, 20, 23, 24};
    final int keyy[] = {0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0};

    private boolean selection[] = new boolean[128];

    private int mouseDownNote;

    private int baseTranspose = 48; /* C3 */

    public PianoComponent() {
        super();

        MouseInputAdapter ma = new MouseInputAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                getParent().requestFocus();
                if (e.getButton() == 1 || e.getButton() == 3) {
                    int i = HitTest(e.getX(), e.getY());
                    if (i >= 0) {
                        mouseDownNote = i;
                        if (!selection[i]) {
                            selection[i] = true;
                            KeyDown(i);
                        }
                        repaint();
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int i = HitTest(e.getX(), e.getY());
                if (i != mouseDownNote) {
                    selection[mouseDownNote] = false;
                    KeyUp(mouseDownNote);
                    if (i >= 0) {
                        selection[i] = true;
                        mouseDownNote = i;
                        KeyDown(i);
                    }
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isShiftDown() || e.getButton() == 3) {
                    /* Sustain mode: keys stay pressed until left-clicked again*/
                    e.consume();
                    return;
                }
                else {
                    selection[mouseDownNote] = false;
                    KeyUp(mouseDownNote);
                    repaint();
                }
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public abstract void KeyUp(int key);

    public abstract void KeyDown(int key);

    public boolean isKeyDown(int key) {
        return selection[key];
    }

    public void setSelection(int key) {
        if (key < 0 || key > 127) return;
        selection[key] = true;
    }

    public void clearSelection(int key) {
        if (key < 0 || key > 127) return;
        selection[key] = false;
    }

    public void setTranspose(int trans) {
        if (trans < 0 || trans > 96) return;
        baseTranspose = trans;
        repaint();

    }    

    public void clear() {
        for (int i=0;i<selection.length;i++) {
            selection[i] = false;
        }
        repaint();
    }

    private int keyToX(int i) {
        return (28 * (i / 12) + keyx[i % 12]) * quarterKeyWidth;
    }

    private int keyToY(int i) {
        return keyy[i % 12];
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // white keys first
        for (int i = 0; i < 128; i++) {
            int y = keyToY(i);
            int x = keyToX(i);
            if (y == 0) {
                if (selection[i]) {
                    // selected
                    g2.setColor(Theme.getCurrentTheme().Keyboard_Mid);
                    g2.fillRect(x, 0, 2 * KeyWidth, height);
                    if (isEnabled()) {
                        g2.setColor(Theme.getCurrentTheme().Keyboard_Dark);
                    } else {
                        g2.setColor(Theme.getCurrentTheme().Keyboard_Mid);
                    }
                    g2.drawRect(x, 0, 2 * KeyWidth, height);
                } else {
                    // not selected
                    g2.setColor(Theme.getCurrentTheme().Keyboard_Light);
                    g2.fillRect(x, 0, 2 * KeyWidth, height);
                    if (isEnabled()) {
                        g2.setColor(Theme.getCurrentTheme().Keyboard_Dark);
                    } else {
                        g2.setColor(Theme.getCurrentTheme().Keyboard_Mid);
                    }
                    g2.drawRect(x, 0, 2 * KeyWidth, height);
                }
            }
        }
        // black keys
        for (int i = 0; i < 128; i++) {
            int y = keyToY(i);
            int x = keyToX(i);
            if (y == 1) {
                if (selection[i]) {
                    g2.setColor(Theme.getCurrentTheme().Keyboard_Mid);
                    g2.fillRect(x - 1, 0, KeyWidth + 2, blackKeyHeight);
                    if (isEnabled()) {
                        g2.setColor(Theme.getCurrentTheme().Keyboard_Dark);
                    } else {
                        g2.setColor(Theme.getCurrentTheme().Keyboard_Mid);
                    }
                    g2.drawRect(x - 1, 0, KeyWidth + 2, blackKeyHeight);
                } else {
                    if (isEnabled()) {
                        g2.setColor(Theme.getCurrentTheme().Keyboard_Dark);
                    } else {
                        g2.setColor(Theme.getCurrentTheme().Keyboard_Mid);
                    }
                    g2.fillRect(x - 1, 0, KeyWidth + 2, blackKeyHeight);
                    g2.drawRect(x - 1, 0, KeyWidth + 2, blackKeyHeight);
                }
            }
        }
        for (int i = 0; i < 128; i++) {
            if (i % 12 == 0) {
                int x = keyToX(i);
                g2.setFont(Constants.FONT);
                g2.drawString("" + ((i / 12) - 1), x + 3, height - 2);
            }
        }
        for (int i = 0; i < 128; i++) {
            int y = keyToY(i);
            int x = keyToX(i);
            if (y == 0) {
                g2.setFont(Constants.FONT);
                /* Mark keyboard-playable area with accent color */
                if (i >= baseTranspose && i < baseTranspose + 25) {
                    g2.setPaint(Theme.getCurrentTheme().Button_Accent_Background);
                }
                else {
                    g2.setPaint(Theme.getCurrentTheme().Object_Default_Foreground);
                }
                AffineTransform t = g2.getTransform();
                g2.rotate(-3.14159 / 2);
                g2.drawString(String.format("%d", i - 64), -71, x + 11);
                //            g2.drawString(String.format("%.1f", 440*Math.pow(2.0,(i-69.0)/12)), 33 ,-x-1);
                g2.setTransform(t);
            }
        }
        int x = keyToX(64);
        g2.setColor(Theme.getCurrentTheme().Keyboard_Mid);
        g2.fillOval(x + 3, height - (KeyWidth + 2), KeyWidth, KeyWidth);
        g2.dispose();
    }

    int HitTest(int x, int y) {
        if (!isEnabled()) {
            return -1;
        }
        int o = 12 * (x / (28 * quarterKeyWidth));
        int oe = o + 12;
        if (oe > 128) {
            oe = 128;
        }
        if (y < blackKeyHeight) {
            // test black keys first
            for (int i = o; i < oe; i++) {
                int iy = keyToY(i);
                if (iy != 0) {
                    int ix = keyToX(i);
                    if ((x >= ix - 1) && (x <= ix + KeyWidth + 1)) {
                        return i;
                    }
                }
            }
        }
        for (int i = o; i < oe; i++) {
            int iy = keyToY(i);
            // white key test
            if (iy == 0) {
                int ix = keyToX(i);
                if ((x >= ix) && (x <= ix + KeyWidth * 2)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
