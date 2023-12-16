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
import java.io.IOException;

// import java.io.InputStream;

/**
 *
 * @author Johannes Taelman
 */
public class Constants {

    // public static final Font FONT = new Font("SansSerif", Font.PLAIN, 10);
    // public static final Font FONT = UIManager.getFont("mini.font").deriveFont(10.0f);
    // public static final Font FONT_MONO = UIManager.getFont("monospaced.font");

    public static Font FONT = createFontUI();
    public static final Font FONT_BOLD = FONT.deriveFont(Font.BOLD); /* it actually saves UI space if we derive the bold font... */
    // public static Font FONT_BOLD = createFontUIBold(); /* ...instead of loading it from a ttf */
    public static Font FONT_MONO = createFontUIMono();

    public static final int X_GRID = 14;
    public static final int Y_GRID = 14;

    // public static final Color TRANSPARENT = new Color(255, 255, 255, 0);

    public static final int PATCH_SIZE = 5000;

    public static final String OBJECT_LAYER_PANEL = "OBJECT_LAYER_PANEL";
    public static final String DRAGGED_OBJECT_LAYER_PANEL = "DRAGGED_OBJECT_LAYER_PANEL";

    public static final int ANCESTOR_CACHE_SIZE = 1024;

    public static Font createFontUI() {
        try {
            FONT= Font.createFont(Font.TRUETYPE_FONT, Constants.class.getResourceAsStream(
                "/resources/fonts/NotoSans_SemiCondensed-Medium.ttf")).deriveFont(11f);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (FontFormatException e) {
            e.printStackTrace();
        }
        return FONT;
    }

    public static Font createFontUIMono() {
        try {
            FONT_MONO = Font.createFont(Font.TRUETYPE_FONT, Constants.class.getResourceAsStream(
                "/resources/fonts/NotoSansMono-Regular.ttf")).deriveFont(15f);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (FontFormatException e) {
            e.printStackTrace();
        }
        return FONT_MONO;
    }

    // public static Font createFontUIBold() {
    //     try {
    //         FONT_BOLD = Font.createFont(Font.TRUETYPE_FONT, Constants.class.getResourceAsStream(
    //             "/resources/fonts/NotoSans_SemiCondensed-Bold.ttf")).deriveFont(11f);
    //     }
    //     catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //     catch (FontFormatException e) {
    //         e.printStackTrace();
    //     }
    //     return FONT_BOLD;
    // }
}
