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

import axoloti.utils.GraphicsUtils;

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

    private static final int ICON_X_CENTER_OFFSET = 18;
    private static final int TEXT_X_OFFSET = 38;

    @Override
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {
        super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);

        Graphics2D g2 = GraphicsUtils.configureGraphics((Graphics2D) g.create());
        Rectangle tabRect = rects[tabIndex];

        Icon icon = tabIcons.get(tabIndex);
        if (icon == null) {
            g2.dispose();
            return;
        }

        int iconX = tabRect.x + ICON_X_CENTER_OFFSET - (icon.getIconWidth() / 2);
        int iconY = tabRect.y + (tabRect.height - icon.getIconHeight()) / 2;
        
        icon.paintIcon(tabPane, g2, iconX, iconY);

        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        if (tabIcons.get(tabIndex) == null) {
            super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
        } else {
            Graphics2D g2 = GraphicsUtils.configureGraphics((Graphics2D) g.create());
            Rectangle tabRect = getTabBounds(tabIndex, new Rectangle());
            
            int textX = tabRect.x + TEXT_X_OFFSET;
            int textY = tabRect.y + (tabRect.height - metrics.getHeight()) / 2 + metrics.getAscent();

            Color tabColor = tabColors.getOrDefault(tabIndex, tabPane.getForeground());
            g2.setColor(tabColor);
            g2.setFont(g.getFont());
            g2.drawString(title, textX, textY);
            
            g2.dispose();
        }
    }

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        Insets defaultInsets = super.getTabInsets(tabPlacement, tabIndex);
        return new Insets(defaultInsets.top, defaultInsets.left + 28, defaultInsets.bottom, defaultInsets.right);
    }
}