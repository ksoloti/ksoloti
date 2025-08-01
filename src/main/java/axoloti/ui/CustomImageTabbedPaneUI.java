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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;

/**
 * A custom tabbed pane UI that displays a custom image icon instead of a colored stripe.
 * It uses the same color mapping and layout as the parent CustomTabbedPaneUI.
 * 
 * @author Ksoloti
 */
public class CustomImageTabbedPaneUI extends FlatTabbedPaneUI {

    private Map<Integer, Icon> tabIcons = new HashMap<>();
    private Map<Integer, Color> tabColors = new HashMap<>();

    public CustomImageTabbedPaneUI(Map<Integer, Icon> icons) {
        super();
        this.tabIcons = icons;
    }

    public void setTabColor(int tabIndex, Color color) {
        tabColors.put(tabIndex, color);
    }

    // Define the fixed position of the icons and text labels here.
    private static final int ICON_X_CENTER_OFFSET = 18;
    private static final int TEXT_X_OFFSET = 38;

    @Override
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {
        // Let the parent class (FlatTabbedPaneUI) handle the default painting of the entire tab.
        // This includes the background, selection marker, and hover effects.
        super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);

        Graphics2D g2d = (Graphics2D) g.create();
        Rectangle tabRect = rects[tabIndex];

        // Get the custom icon for this tab.
        Icon icon = tabIcons.get(tabIndex);
        if (icon == null) {
            // If no icon is provided, we'll let the default text be drawn by the parent.
            g2d.dispose();
            return;
        }

        // Calculate icon position for centering.
        int iconX = tabRect.x + ICON_X_CENTER_OFFSET - (icon.getIconWidth() / 2);
        int iconY = tabRect.y + (tabRect.height - icon.getIconHeight()) / 2;
        
        // Paint the icon.
        icon.paintIcon(tabPane, g2d, iconX, iconY);

        g2d.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        // Only paint the text if there is NO custom icon.
        // This prevents the double-drawing issue.
        if (tabIcons.get(tabIndex) == null) {
            super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
        } else {
            // If a custom icon is present, we draw our own text.
            Graphics2D g2d = (Graphics2D) g.create();
            Rectangle tabRect = getTabBounds(tabIndex, new Rectangle());
            
            // Calculate text position for left alignment.
            int textX = tabRect.x + TEXT_X_OFFSET;
            int textY = tabRect.y + (tabRect.height - metrics.getHeight()) / 2 + metrics.getAscent();

            // Get the custom color for the text, or use the default foreground.
            Color tabColor = tabColors.getOrDefault(tabIndex, tabPane.getForeground());
            g2d.setColor(tabColor);
            g2d.setFont(g.getFont());
            g2d.drawString(title, textX, textY);
            
            g2d.dispose();
        }
    }

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        Insets defaultInsets = super.getTabInsets(tabPlacement, tabIndex);
        // We need to increase the left inset to make room for our custom icon.
        return new Insets(defaultInsets.top, defaultInsets.left + 28, defaultInsets.bottom, defaultInsets.right);
    }
}