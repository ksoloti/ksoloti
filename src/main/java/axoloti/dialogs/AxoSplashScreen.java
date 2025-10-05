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

package axoloti.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

import axoloti.ui.SvgIconLoader;
import axoloti.utils.GraphicsUtils;

/**
 *
 * @author Ksoloti
 */
public class AxoSplashScreen extends JWindow {

    class RoundedPanel extends JPanel {
        private final int cornerRadius;
        private final JLabel contentLabel;

        public RoundedPanel(JLabel label, int radius) {
            this.cornerRadius = radius;
            this.contentLabel = label;
            
            // CRITICAL: Must be non-opaque so it can draw its own background,
            // allowing the window beneath to show (if it could).
            setOpaque(false); 
            
            // Set layout and add the label
            setLayout(new BorderLayout());
            add(contentLabel, BorderLayout.CENTER);
            
            // Ensure the preferred size is correct for packing
            setPreferredSize(label.getPreferredSize());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = GraphicsUtils.configureGraphics((Graphics2D) g.create());
            RoundRectangle2D shape = new RoundRectangle2D.Float(
                0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius
            );
            g2.setClip(shape);
            super.paintComponent(g2); 
            g2.dispose();
            
            Container topLevel = getTopLevelAncestor();
            if (topLevel instanceof Window) {
                ((Window) topLevel).setShape(shape);
            }
        }
    }

    public AxoSplashScreen() {
        setBackground(new Color(255, 255, 255, 0)); /* Make window transparent */
        setAlwaysOnTop(true);
        setOpacity(1.0f);

        try {
            JLabel label = null;
            Icon splashSvg = SvgIconLoader.load("/resources/appicons/ksoloti_splash.svg", 512);
            GraphicsDevice defaultScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

            if (splashSvg != null) {
                label = new JLabel(splashSvg);
            } else {
                System.err.println("Failed to load SVG icon. Falling back to PNG.");
                label = new JLabel(new ImageIcon(getClass().getResource("/resources/appicons/ksoloti_splash.png")));
            }

            if (label != null) {
                Dimension d = new Dimension(512,384);
                this.getContentPane().setMinimumSize(d);
                this.getContentPane().setMaximumSize(d);
                this.getContentPane().setPreferredSize(d);
                label.setMinimumSize(d);
                label.setMaximumSize(d);
                label.setPreferredSize(d);
                label.setOpaque(false);
                this.getContentPane().add(label);
                this.pack();
                if (defaultScreen.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
                    int width = this.getWidth();
                    int height = this.getHeight();
                    int desiredFixedRadius = 76; 
    
                    RoundRectangle2D roundedRect = new RoundRectangle2D.Float(0, 0, width, height, desiredFixedRadius, desiredFixedRadius);
                    this.setShape(roundedRect);
                }
            }

            /* Center the window on the primary screen to prevent splitting on dual screens */
            Rectangle screenBounds = defaultScreen.getDefaultConfiguration().getBounds();
            int x = screenBounds.x + (screenBounds.width - this.getWidth()) / 2;
            int y = screenBounds.y + (screenBounds.height - this.getHeight()) / 2;
            this.setLocation(x, y);

        } catch (Exception e) {
            System.err.println("Error loading splash screen image: " + e.getMessage());
            e.printStackTrace(System.out);
            this.dispose();
        }
    }

    public void showSplashScreen() {
        this.setVisible(true);
    }
}
