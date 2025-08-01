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

import axoloti.Theme;

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
        FontAwesomeSolid.ARROW_RIGHT,       // Outlets
        FontAwesomeSolid.SLIDERS_H,         // Parameters
        FontAwesomeSolid.CLOCK,             // K-rate Code
        FontAwesomeSolid.WAVE_SQUARE,       // S-rate Code
        FontAwesomeSolid.MUSIC,             // MIDI Code
        FontAwesomeSolid.TRASH,             // Dispose Code
        FontAwesomeSolid.CODE               // XML Preview
    };
    
    private static final int ICON_X_CENTER_OFFSET = 18; /* Adjust to move icons left/right */
    private static final int TEXT_X_OFFSET = 38;        /* Adjust to move text left/right */

    @Override
    protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {
        Graphics2D g2d = (Graphics2D) g.create();
        Rectangle tabRect = rects[tabIndex];

        /* Get the color from the parent's map */
        Color tabColor = tabColors.getOrDefault(tabIndex, Color.LIGHT_GRAY);
        
        /* No stripes will be painted here as we are not calling the parent's paintTabBackground. */

        /* Get the icon for this tab and color it with the tab's color */
        Ikon ikon = TAB_IKONS[tabIndex];
        FontIcon icon = FontIcon.of(ikon, 16, tabColor);

        int iconX = tabRect.x + ICON_X_CENTER_OFFSET - (icon.getIconWidth() / 2);
        int iconY = tabRect.y + (tabRect.height - icon.getIconHeight()) / 2;

        int textX = tabRect.x + TEXT_X_OFFSET;
        FontMetrics metrics = g.getFontMetrics();
        int textY = tabRect.y + (tabRect.height - metrics.getHeight()) / 2 + metrics.getAscent();

        icon.paintIcon(tabPane, g2d, iconX, iconY);
        
        g2d.setColor(Theme.Component_Foreground);
        g2d.setFont(g.getFont());
        g2d.drawString(tabPane.getTitleAt(tabIndex), textX, textY);
        
        g2d.dispose();
    }
    
    /* Override the insets to provide enough space for the content */
    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        Insets defaultInsets = super.getTabInsets(tabPlacement, tabIndex);
        return new Insets(defaultInsets.top, defaultInsets.left + 50, defaultInsets.bottom, defaultInsets.right);
    }
}