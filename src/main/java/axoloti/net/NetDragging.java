/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
package axoloti.net;

import axoloti.PatchGUI;
import axoloti.datatypes.DataType;
import axoloti.inlets.InletInstance;
import axoloti.outlets.OutletInstance;
import java.awt.Point;

/**
 *
 * @author Johannes Taelman
 */
public class NetDragging extends Net {

    private DragWireOverlay dragWireOverlay; // The new overlay for drawing the wire

    private OutletInstance draggingSourceOutlet; // The outlet being dragged from
    private InletInstance draggingDestInlet; // The inlet being dragged to (if connected)
    private Point currentDragPoint; // The current mouse position during drag

    public NetDragging(DragWireOverlay dragWireOverlay) {
        this.dragWireOverlay = dragWireOverlay;
    }

    /**
     * Initializes the dragging operation.
     * @param startOutlet The outlet from which the drag started.
     */
    public void StartDraggingFromOutlet(OutletInstance startOutlet) {
        this.draggingSourceOutlet = startOutlet;
        this.draggingDestInlet = null; // No destination yet
        this.currentDragPoint = startOutlet.getJackLocInCanvas(); // Start at the outlet
        
        // Show the dragging wire
        dragWireOverlay.setWirePoints(
            this.draggingSourceOutlet.getJackLocInCanvas(),
            this.currentDragPoint,
            this.draggingSourceOutlet.GetDataType() // Get data type from source
        );
        dragWireOverlay.showWire();
    }

    /**
     * Initializes the dragging operation (from an inlet if re-routing).
     * This scenario might be different, consider how you start dragging from an existing net.
     * @param startInlet The inlet from which the drag started.
     */
    public void StartDraggingFromInlet(InletInstance startInlet) {
        this.draggingDestInlet = startInlet;
        this.draggingSourceOutlet = null; // No source yet
        this.currentDragPoint = startInlet.getJackLocInCanvas(); // Start at the inlet

        // For dragging from an inlet, you might need to determine the initial wire color
        // if it's an existing connection. For a new wire, it might be default.
        DataType dataType = null; // Determine appropriate data type here
        dragWireOverlay.setWirePoints(
            this.currentDragPoint, // Start point is the inlet
            this.currentDragPoint, // End point is initially same as start
            dataType
        );
        dragWireOverlay.showWire();
    }

    /**
     * Updates the end point of the dragging wire.
     * Call this from mouseDragged event handlers.
     * @param p The current mouse position in canvas coordinates.
     */
    public void UpdateDragPoint(Point p) {
        this.currentDragPoint = p;

        Point wireStart;
        DataType wireType;

        if (draggingSourceOutlet != null) {
            wireStart = draggingSourceOutlet.getJackLocInCanvas();
            wireType = draggingSourceOutlet.GetDataType();
        } else if (draggingDestInlet != null) {
            wireStart = draggingDestInlet.getJackLocInCanvas();
            wireType = null;
        } else {
            System.out.println("NetDragging.UpdateDragPoint: No active source/destination for wire.");
            return;
        }

        System.out.println("NetDragging.UpdateDragPoint: Setting wire from " + wireStart + " to " + this.currentDragPoint);
        if (dragWireOverlay == null) {
            System.err.println("NetDragging.UpdateDragPoint: dragWireOverlay is NULL!");
            return;
        }
        dragWireOverlay.setWirePoints(wireStart, this.currentDragPoint, wireType);
        dragWireOverlay.repaint();
    }

    /**
     * Ends the dragging operation.
     * Call this from mouseReleased event handlers.
     */
    public void EndDragging() {
        dragWireOverlay.hideWire(); // Hide the temporary wire
        this.draggingSourceOutlet = null;
        this.draggingDestInlet = null;
        this.currentDragPoint = null;
    }

    // You will need methods to get the dragging source/destination if you connect
    // in PatchGUI based on NetDragging's state.
    public OutletInstance getDraggingSourceOutlet() {
        return draggingSourceOutlet;
    }

    public InletInstance getDraggingDestInlet() {
        return draggingDestInlet;
    }

    public Point getCurrentDragPoint() {
        return currentDragPoint;
    }
}
