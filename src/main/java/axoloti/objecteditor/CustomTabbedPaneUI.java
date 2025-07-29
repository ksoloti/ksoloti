/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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

package axoloti.objecteditor;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;

/**
 * 
 * @author Ksoloti
 */
public class CustomTabbedPaneUI extends FlatTabbedPaneUI {

    /* Adds a colored stripe to a tab */

    private Map<Integer, Color> tabColors = new HashMap<>();
    private int stripeWidth = 3; /* Width of the colored stripe. Currently 3 to match FlatLaf's selected tab stripe on the right */ 

    public CustomTabbedPaneUI() {
        /* Default colors for ObjectEditor tabs: */
        tabColors.put(0, new Color(239, 83, 80));    // Overview     - Material Red 400
        tabColors.put(1, new Color(255, 167, 38));   // Attributes   - Material Orange 400
        tabColors.put(2, new Color(255, 238, 88));   // Local Data   - Material Yellow 400
        tabColors.put(3, new Color(76, 175, 80));    // Displays     - Material Green 500
        tabColors.put(4, new Color(0, 188, 212));    // Init Code    - Material Cyan 500
        tabColors.put(5, new Color(33, 150, 243));   // Inlets       - Material Blue 500
        tabColors.put(6, new Color(92, 107, 192));   // Outlets      - Material Indigo 400
        tabColors.put(7, new Color(171, 71, 188));   // Parameters   - Material Purple 400
        tabColors.put(8, new Color(236, 64, 122));   // K-rate Code  - Material Pink 400
        tabColors.put(9, new Color(255, 112, 67));   // S-rate Code  - Material Deep Orange 400
        tabColors.put(10, new Color(156, 204, 101)); // MIDI Code    - Material Light Green 400
        tabColors.put(11, new Color(0, 150, 136));   // Dispose Code - Material Teal 500
        tabColors.put(12, new Color(158, 158, 158)); // XML Preview  - Material Grey 500
    }

    /* Optional: Set colors dynamically */
    public void setTabColor(int tabIndex, Color color) {
        tabColors.put(tabIndex, color);
        if (tabPane != null) {
            tabPane.repaint();
        }
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        /* Call super method to draw the default background */
        super.paintTabBackground(g, tabPlacement, tabIndex, x, y, w, h, isSelected);

        Color stripeColor = tabColors.getOrDefault(tabIndex, Color.LIGHT_GRAY); // Default if no color set
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(stripeColor);

        /* The stripe will be drawn at x, extending 'stripeWidth' to the right */
        /* The height will be the full height of the tab (h) */
        /* Padding (left, top, bottom) is 2 * stripeWidth */
        int padding = 2 * stripeWidth;
        g2d.fillRect(x + padding, y + padding, stripeWidth, h - (2 * padding));
        g2d.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        Rectangle tabBounds = getTabBounds(tabIndex, new Rectangle());
        int desiredTextX = tabBounds.x + getTabInsets(tabPlacement, tabIndex).left;

        Rectangle leftAlignedTextRect = new Rectangle(
            desiredTextX,
            textRect.y,
            tabBounds.width - (desiredTextX - tabBounds.x) - getTabInsets(tabPlacement, tabIndex).right,
            textRect.height
        );
        super.paintText(g, tabPlacement, font, metrics, tabIndex, title, leftAlignedTextRect, isSelected);
    }

    /* Optional: Override getTabInsets to create space for the stripe */
    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        Insets defaultInsets = super.getTabInsets(tabPlacement, tabIndex);
        return new Insets(defaultInsets.top, defaultInsets.left + stripeWidth, defaultInsets.bottom, defaultInsets.right);
    }

    /* Optional: Override getTabLabelShiftX to fine-tune horizontal alignment */
    @Override
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        return super.getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
    }

    /* Optional: Override getTabLabelShiftY to fine-tune vertical alignment */
    @Override
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        return super.getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
    }
}
