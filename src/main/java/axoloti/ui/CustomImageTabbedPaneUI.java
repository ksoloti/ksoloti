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

package axoloti.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;

/**
 * A custom tabbed pane UI that displays a custom image icon instead of a colored stripe.
 * It uses the same color mapping and layout as the parent CustomTabbedPaneUI.
 * 
 * @author Ksoloti
 */
public class CustomImageTabbedPaneUI extends CustomTabbedPaneUI {

    private Map<Integer, Icon> tabIcons = new HashMap<>();
    
    // We'll use a constructor to accept a map of custom images.
    public CustomImageTabbedPaneUI(Map<Integer, Icon> icons) {
        super(); // Call the parent constructor to initialize tabColors
        this.tabIcons = icons;
    }
    
    // We can define the fixed position of the icons and text labels here.
    private static final int ICON_X_CENTER_OFFSET = 18; // Adjust to move icons left/right
    private static final int TEXT_X_OFFSET = 38;        // Adjust to move text left/right

    @Override
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {
        Graphics2D g2d = (Graphics2D) g.create();
        Rectangle tabRect = rects[tabIndex];

        // 1. Get the custom icon for this tab
        Icon icon = tabIcons.get(tabIndex);
        if (icon == null) {
            // Fallback: If no icon is provided, paint the tab normally.
            super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);
            return;
        }

        // 2. Get the color from the parent's map for the text
        Color tabColor = tabColors.getOrDefault(tabIndex, Color.LIGHT_GRAY);
        
        // We do not call the parent's paintTabBackground() here to prevent stripes.

        // 3. Calculate icon position for centering, based on the icon's width
        int iconX = tabRect.x + ICON_X_CENTER_OFFSET - (icon.getIconWidth() / 2);
        int iconY = tabRect.y + (tabRect.height - icon.getIconHeight()) / 2;

        // 4. Calculate text position for left alignment
        int textX = tabRect.x + TEXT_X_OFFSET;
        FontMetrics metrics = g.getFontMetrics();
        int textY = tabRect.y + (tabRect.height - metrics.getHeight()) / 2 + metrics.getAscent();

        // 5. Paint the icon and the text
        icon.paintIcon(tabPane, g2d, iconX, iconY);
        
        g2d.setColor(Color.WHITE); // Use white text for better contrast on the dark background
        g2d.setFont(g.getFont());
        g2d.drawString(tabPane.getTitleAt(tabIndex), textX, textY);
        
        g2d.dispose();
    }
    
    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        Insets defaultInsets = super.getTabInsets(tabPlacement, tabIndex);
        // The total padding is now controlled by the new X offsets.
        // We set a large enough value here to ensure no clipping occurs.
        return new Insets(defaultInsets.top, defaultInsets.left + 50, defaultInsets.bottom, defaultInsets.right);
    }
}