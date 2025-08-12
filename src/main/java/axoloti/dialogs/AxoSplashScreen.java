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

import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;

import axoloti.ui.SvgIconLoader;

/**
 *
 * @author Ksoloti
 */
public class AxoSplashScreen extends JWindow {

    private final Timer fadeTimer;
    private float opacity = 0.0f;
    private boolean fadeIn = false;

    public AxoSplashScreen(boolean fadeIn) {
        
        this.fadeIn = fadeIn;
        if (this.fadeIn) {
            setOpacity(opacity);
        }
        else {
            setOpacity(1.0f);
        }

        setBackground(new Color(0, 0, 0, 0)); /* Make window transparent */
        setAlwaysOnTop(true);

        try {
            JLabel label = null;
            Icon splashSvg = SvgIconLoader.load("/resources/appicons/ksoloti_splash.svg", 512);
            if (splashSvg != null && splashSvg instanceof ImageIcon) {
                label = new JLabel((ImageIcon) splashSvg);
            } else {
                System.err.println("Failed to load SVG icon. Falling back to PNG.");
                setIconImage(new ImageIcon(getClass().getResource("/resources/ksoloti_splash.png")).getImage());
            }

            label.setOpaque(false);
            this.getContentPane().add(label);
            this.pack();

            /* Center the window on the primary screen to prevent splitting on dual screens */
            GraphicsDevice defaultScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            Rectangle screenBounds = defaultScreen.getDefaultConfiguration().getBounds();
            int x = screenBounds.x + (screenBounds.width - this.getWidth()) / 2;
            int y = screenBounds.y + (screenBounds.height - this.getHeight()) / 2;
            this.setLocation(x, y);

        } catch (Exception e) {
            System.err.println("Error loading splash screen image: " + e.getMessage());
            this.dispose();
        }

        fadeTimer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                opacity += 0.05f; /* Increase opacity in small steps */
                if (opacity >= 1.0f) {
                    setOpacity(1.0f); /* Ensure it reaches full opacity */
                    fadeTimer.stop();
                } else {
                    setOpacity(opacity);
                }
            }
        });
    }

    public void showSplashScreen() {
        this.setVisible(true);
        if (this.fadeIn) {
            fadeTimer.start();
        }
    }
}
