/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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
package axoloti.utils;


import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import axoloti.ui.SvgIconLoader;

// import java.io.InputStream;

/**
 *
 * @author Johannes Taelman
 */
public class Constants {

    private static final Logger LOGGER = Logger.getLogger(Constants.class.getName());

    static Constants constants = new Constants();

    // public static final Font FONT = new Font("SansSerif", Font.PLAIN, 10);
    // public static final Font FONT = UIManager.getFont("mini.font").deriveFont(10.0f);
    // public static final Font FONT_MONO = UIManager.getFont("monospaced.font");
    public static Font FONT_MONO; 
    public static Font FONT;
    public static Font FONT_BOLD;
    public static Font FONT_MENU;

    public static final int X_GRID = 14;
    public static final int Y_GRID = 14;

    // public static final Color TRANSPARENT = new Color(255, 255, 255, 0);

    public static final int PATCH_SIZE = 5000;

    public static final String OBJECT_LAYER_PANEL = "OBJECT_LAYER_PANEL";
    public static final String DRAGGED_OBJECT_LAYER_PANEL = "DRAGGED_OBJECT_LAYER_PANEL";

    public static final int ANCESTOR_CACHE_SIZE = 1024;

    public static ImageIcon APP_ICON;

    public static final String I = "\t"; /* Convenient for (I)ndentation of auto-generated code */

    Constants() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            createFontMenu();
            createFontMono();
            createFontPatchGUI();
            createAppIcon();
            if (FONT != null) {
                FONT_BOLD = FONT.deriveFont(Font.BOLD);
            }
            
            if (ge != null) {
                if (FONT_MONO != null) ge.registerFont(FONT_MONO);
                if (FONT != null) ge.registerFont(FONT);
                if (FONT_BOLD != null) ge.registerFont(FONT_BOLD);
                if (FONT_MENU != null) ge.registerFont(FONT_MENU);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing graphics environment. Running in headless mode or missing font files: " + e.getMessage());
            e.printStackTrace(System.out);
            FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
            FONT = new Font("SansSerif", Font.PLAIN, 12);
            FONT_BOLD = new Font("SansSerif", Font.BOLD, 12);
            FONT_MENU = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    public static void createFontPatchGUI() {
        try {
            String fstr = "/resources/fonts/NotoSans_SemiCondensed-Medium.ttf";
            FONT = Font.createFont(Font.TRUETYPE_FONT, Constants.class.getResourceAsStream(fstr)).deriveFont(11f);
        }
        catch (FontFormatException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error trying to create patch GUI font: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    public static void createFontMono() {
        try {
            String fstr = "/resources/fonts/NotoSansMono-Regular.ttf";
            FONT_MONO = Font.createFont(Font.TRUETYPE_FONT, Constants.class.getResourceAsStream(fstr)).deriveFont((float)Preferences.getInstance().getCodeFontSize());
        }
        catch (FontFormatException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error trying to create monospace GUI font: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    public static void createFontMenu() {
        // try {
            // String fstr = "/resources/fonts/NotoSans-Regular.ttf";
            // FONT_MENU = Font.createFont(Font.TRUETYPE_FONT, Constants.class.getResourceAsStream(fstr)).deriveFont(14f);
            FONT_MENU = UIManager.getFont("defaultFont");
        // } catch (FontFormatException | IOException e) {
        //     LOGGER.log(Level.SEVERE, "Error trying to create menu GUI font: " + e.getMessage());
        //     e.printStackTrace(System.out);
        // }
    }

    public static void createAppIcon() {
        Icon svgIcon;

        if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
            svgIcon = SvgIconLoader.load("/resources/appicons/ksoloti_icon.svg", 64);
            if (svgIcon != null) {
            APP_ICON = new ImageIcon(SvgIconLoader.toBufferedImage(svgIcon));
        } else {
            System.err.println("Failed to load SVG icon. Falling back to PNG.");
            APP_ICON = new ImageIcon(Constants.class.getResource("/resources/appicons/ksoloti_icon.png"));
        }
        }
        else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
            svgIcon = SvgIconLoader.load("/resources/appicons/axoloti_icon.svg", 64);
            if (svgIcon != null) {
            APP_ICON = new ImageIcon(SvgIconLoader.toBufferedImage(svgIcon));
        } else {
            System.err.println("Failed to load SVG icon. Falling back to PNG.");
            APP_ICON = new ImageIcon(Constants.class.getResource("/resources/appicons/axoloti_icon.png"));
        }
        }
    }
}
