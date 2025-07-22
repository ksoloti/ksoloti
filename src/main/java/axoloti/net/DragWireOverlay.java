package axoloti.net;

import axoloti.PatchGUI;
import axoloti.Theme;
import axoloti.datatypes.DataType;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.QuadCurve2D;

import javax.swing.JComponent;

// This component draws the single wire being dragged
public class DragWireOverlay extends JComponent {

    private Point startPoint;
    private Point endPoint;
    private DataType wireDataType;
    private boolean wireVisible;

    public DragWireOverlay() {
        setOpaque(false);
        this.wireVisible = false;
        // CRITICAL: Initially hide and disable the component so it doesn't block events
        setVisible(false);
        setEnabled(false); // Make it non-interactive
    }

    /**
     * Sets the points for the temporary wire and its data type.
     * @param start The starting point of the wire (e.g., OutletInstance jack location).
     * @param end The current end point of the wire (mouse position).
     * @param dataType The DataType of the wire for coloring.
     */
    public void setWirePoints(Point start, Point end, DataType dataType) {
        this.startPoint = start;
        this.endPoint = end;
        this.wireDataType = dataType;
    }

    public void showWire() {
        this.wireVisible = true;
        setVisible(true);   // CRITICAL: Make it visible
        setEnabled(true);   // CRITICAL: Make it interactive (if needed, but usually not for pure drawing overlay)
                            // For getDeepestComponentAt to ignore it, setEnabled(false) is better.
                            // Let's stick with setVisible(true/false) as the primary control for blocking events.
        repaint();
    }

    public void hideWire() {
        this.wireVisible = false;
        setVisible(false);  // CRITICAL: Hide it
        setEnabled(false);  // CRITICAL: Make it non-interactive
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        System.out.println("DragWireOverlay.paintComponent() called. Current Bounds: " + getBounds());
        if (!wireVisible || startPoint == null || endPoint == null || wireDataType == null) {
            System.out.println("DragWireOverlay: NOT DRAWING. wireVisible=" + wireVisible + ", startPoint=" + startPoint + ", endPoint=" + endPoint + ", wireDataType=" + wireDataType);
            return;
        }
        System.out.println("DragWireOverlay: Drawing wire from " + startPoint + " to " + endPoint + " with type " + wireDataType);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int shadowOffset = 2;

        g2.setColor(Theme.Cable_Shadow);
        // Ensure NetDrawingPanel.drawWireHelper is correctly imported/static or part of this class
        NetDrawingPanel.drawWireHelper(g2, startPoint.x + shadowOffset, startPoint.y + shadowOffset, endPoint.x + shadowOffset, endPoint.y + shadowOffset);

        Color wireColor = wireDataType.GetColor();
        g2.setColor(wireColor);
        NetDrawingPanel.drawWireHelper(g2, startPoint.x, startPoint.y, endPoint.x, endPoint.y);

        g2.dispose();
    }
}