package axoloti.objecteditor;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * A custom tabbed pane UI that displays a colored icon instead of a colored stripe.
 * It uses the same color mapping as the parent CustomTabbedPaneUI.
 */
public class IconTabbedPaneUI extends CustomTabbedPaneUI {

    private static final Ikon[] TAB_IKONS = {
        FontAwesomeSolid.INFO_CIRCLE,       // Overview
        FontAwesomeSolid.TAG,               // Attributes
        FontAwesomeSolid.DATABASE,          // Local Data
        FontAwesomeSolid.WAVE_SQUARE,       // Displays
        FontAwesomeSolid.BOLT,              // Init Code
        FontAwesomeSolid.ARROW_RIGHT,       // Inlets
        FontAwesomeSolid.ARROW_LEFT,        // Outlets
        FontAwesomeSolid.SLIDERS_H,         // Parameters
        FontAwesomeSolid.CLOCK,             // K-rate Code
        FontAwesomeSolid.WAVE_SQUARE,       // S-rate Code
        FontAwesomeSolid.MUSIC,             // MIDI Code
        FontAwesomeSolid.TRASH,             // Dispose Code
        FontAwesomeSolid.CODE               // XML Preview
    };
    
    // We override the entire paintTab method to take full control.
    @Override
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {
        Graphics2D g2d = (Graphics2D) g.create();
        Rectangle tabRect = rects[tabIndex];

        // 1. Get the color from the parent's map
        Color tabColor = tabColors.getOrDefault(tabIndex, Color.LIGHT_GRAY);
        
        // 2. We skip calling paintTabBackground() from the parent here to prevent the stripes from being drawn.
        // Instead, we just paint the default background provided by FlatLaf.
        // This effectively removes the colored stripe.

        // 3. Get the icon for this tab and color it with the tab's color
        Ikon ikon = TAB_IKONS[tabIndex];
        
        FontIcon icon = FontIcon.of(ikon, 16, tabColor);

        // 4. Calculate new positions for the icon and text to add padding
        int padding = 8; // Horizontal space
        int iconX = tabRect.x + padding;
        int iconY = tabRect.y + (tabRect.height - icon.getIconHeight()) / 2;
        int textX = iconX + icon.getIconWidth() + padding;
        FontMetrics metrics = g.getFontMetrics();
        int textY = tabRect.y + (tabRect.height - metrics.getHeight()) / 2 + metrics.getAscent();

        // 5. Paint the icon and the text
        icon.paintIcon(tabPane, g2d, iconX, iconY);
        
        g2d.setColor(Color.WHITE); // Use white text for better contrast on the dark background
        g2d.setFont(g.getFont());
        g2d.drawString(tabPane.getTitleAt(tabIndex), textX, textY);
        
        g2d.dispose();
    }
    
    // We need to override the insets to provide space for the icon, not a stripe
    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        Insets defaultInsets = super.getTabInsets(tabPlacement, tabIndex);
        // Add padding for the icon and some space
        return new Insets(defaultInsets.top, defaultInsets.left + 24, defaultInsets.bottom, defaultInsets.right);
    }
}