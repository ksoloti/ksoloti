/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
// import axoloti.utils.OSDetect.OS;

// import java.io.InputStream;

/**
 *
 * @author Johannes Taelman
 */
public class Constants {
    static Constants constants = new Constants();

    // public static final Font FONT = new Font("SansSerif", Font.PLAIN, 10);
    // public static final Font FONT = UIManager.getFont("mini.font").deriveFont(10.0f);
    // public static final Font FONT_MONO = UIManager.getFont("monospaced.font");
    public static Font FONT_MONO; 
    public static Font FONT;
    public static Font FONT_BOLD;

    public static final int X_GRID = 14;
    public static final int Y_GRID = 14;

    // public static final Color TRANSPARENT = new Color(255, 255, 255, 0);

    public static final int PATCH_SIZE = 5000;

    public static final String OBJECT_LAYER_PANEL = "OBJECT_LAYER_PANEL";
    public static final String DRAGGED_OBJECT_LAYER_PANEL = "DRAGGED_OBJECT_LAYER_PANEL";

    public static final int ANCESTOR_CACHE_SIZE = 1024;

    Constants() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        createFontUIMono();
        createFontUI();
        FONT_BOLD = FONT.deriveFont(Font.BOLD);
        ge.registerFont(FONT_MONO);
        ge.registerFont(FONT);
        ge.registerFont(FONT_BOLD);
    }

    public static void createFontUI() {
        try {
            String fstr = "/resources/fonts/NotoSans_SemiCondensed-Medium.ttf";
            // if (OSDetect.getOS() == OS.WIN) fstr.replace("/", "\\");
            FONT = Font.createFont(Font.TRUETYPE_FONT, Constants.class.getResourceAsStream(fstr)).deriveFont(11f);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (FontFormatException e) {
            e.printStackTrace();
        }
    }

    public static void createFontUIMono() {
        try {
            String fstr = "/resources/fonts/NotoSansMono-Regular.ttf";
            // if (OSDetect.getOS() == OS.WIN) fstr.replace("/", "\\");
            FONT_MONO = Font.createFont(Font.TRUETYPE_FONT, Constants.class.getResourceAsStream(fstr)).deriveFont((float)Preferences.LoadPreferences().getCodeFontSize());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (FontFormatException e) {
            e.printStackTrace();
        }
    }
}
