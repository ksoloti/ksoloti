/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
package axoloti.outlets;

import axoloti.PatchGUI;
import axoloti.Theme;
import axoloti.atom.AtomInstance;
import axoloti.datatypes.DataType;
import axoloti.inlets.InletInstance;
import axoloti.iolet.IoletAbstract;
import axoloti.net.DragWireOverlay;
import axoloti.net.Net;
import axoloti.net.NetDragging;
import axoloti.net.NetDrawingPanel;
import axoloti.object.AxoObjectInstance;
import components.LabelComponent;
import components.SignalMetaDataIcon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.simpleframework.xml.*;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "source")
public class OutletInstance<T extends Outlet> extends IoletAbstract implements Comparable<OutletInstance>, AtomInstance<T> {

    @Attribute(name = "outlet", required = false)
    public String outletname;

    private final T outlet;

    public String getOutletname() {
        if (outletname != null) {
            return outletname;
        } else {
            int sepIndex = name.lastIndexOf(' ');
            return name.substring(sepIndex + 1);
        }
    }

    @Override
    public T GetDefinition() {
        return outlet;
    }

    public OutletInstance() {
        this.outlet = null;
        this.axoObj = null;
    }

    public OutletInstance(T outlet, AxoObjectInstance axoObj) {
        this.outlet = outlet;
        this.axoObj = axoObj;
        RefreshName();
        PostConstructor();
    }

    public final void RefreshName() {
        name = axoObj.getInstanceName() + " " + outlet.name;
        objname = axoObj.getInstanceName();
        outletname = outlet.name;
        name = null;
    }

    public DataType GetDataType() {
        return outlet.getDatatype();
    }

    public String GetLabel() {
        return outlet.name;
    }

    public String GetCName() {
        return outlet.GetCName();
    }

    public final void PostConstructor() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setMaximumSize(new Dimension(32767, 14));
        setBackground(Theme.Object_Default_Background);
        add(Box.createHorizontalGlue());
        if (axoObj.getType().GetOutlets().size() > 1) {
            LabelComponent olbl = new LabelComponent(outlet.name);
            olbl.setHorizontalAlignment(SwingConstants.RIGHT);
            add(olbl);
        }
        add(new SignalMetaDataIcon(outlet.GetSignalMetaData()));
        jack = new components.JackOutputComponent(this);
        jack.setForeground(outlet.getDatatype().GetColor());
        add(jack);
        setToolTipText(outlet.description);

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public JPopupMenu getPopup() {
        return new OutletInstancePopupMenu(this);
    }

    @Override
    public int compareTo(OutletInstance t) {
        return axoObj.compareTo(t.axoObj);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            getPopup().show(this, 0, getHeight() - 1);
            e.consume();
            return;
        }

        if (!axoObj.IsLocked() && e.getButton() == MouseEvent.BUTTON1) {
            PatchGUI patchGui = getPatchGui();
            if (patchGui != null) {
                DragWireOverlay dragWireOverlay = patchGui.getDragWireOverlay();

                NetDragging currentDragging = new NetDragging(dragWireOverlay);
                currentDragging.StartDraggingFromOutlet(this);

                patchGui.setActiveNetDragging(currentDragging);

                Point mouseLocInDrawingPanel = SwingUtilities.convertPoint(this, e.getPoint(), patchGui.getNetDrawingPanel());
                currentDragging.UpdateDragPoint(mouseLocInDrawingPanel);

                System.out.println("--- Drag Started ---");
                System.out.println("OutletInstance.mousePressed: Initial drag point in NetDrawingPanel: " + mouseLocInDrawingPanel);
                System.out.println("OutletInstance.mousePressed: DragWireOverlay instance: " + dragWireOverlay);
                System.out.println("OutletInstance.mousePressed: DragWireOverlay isVisible(): " + dragWireOverlay.isVisible());
                System.out.println("OutletInstance.mousePressed: DragWireOverlay getBounds(): " + dragWireOverlay.getBounds());
                System.out.println("OutletInstance.mousePressed: NetDrawingPanel getBounds(): " + patchGui.getNetDrawingPanel().getBounds());
                System.out.println("OutletInstance.mousePressed: Layers JLayeredPane getBounds(): " + patchGui.Layers.getBounds());

                setHighlighted(true);
                e.consume();
                return;
            }
        }
        super.mousePressed(e);
    }

    // In OutletInstance.java - mouseDragged
    @Override
    public void mouseDragged(MouseEvent e) {
        PatchGUI patchGui = getPatchGui();
        if (patchGui != null) {
            NetDragging currentDraggedNet = patchGui.getActiveNetDragging();
            if (currentDraggedNet != null && currentDraggedNet.getDraggingSourceOutlet() == this) {
                Point mouseLocInDrawingPanel = SwingUtilities.convertPoint(this, e.getPoint(), patchGui.getNetDrawingPanel());
                currentDraggedNet.UpdateDragPoint(mouseLocInDrawingPanel);

                System.out.println("OutletInstance.mouseDragged: Mouse loc in NetDrawingPanel: " + mouseLocInDrawingPanel);

                e.consume();
                return;
            }
        }
        super.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            getPopup().show(this, 0, getHeight() - 1);
            e.consume();
            return;
        }

        PatchGUI patchGui = getPatchGui();
        if (patchGui != null) {
            NetDragging currentDragging = patchGui.getActiveNetDragging();
            if (currentDragging != null) {
                currentDragging.EndDragging(); // This should hide the wire

                // --- DEBUGGING START ---
                System.out.println("--- OutletInstance Release ---");
                System.out.println("Release mouse event point (relative to OutletInstance): " + e.getPoint());

                // Convert mouse point to NetDrawingPanel coordinates
                Point mouseLocInDrawingPanel = SwingUtilities.convertPoint(this, e.getPoint(), patchGui.getNetDrawingPanel());
                System.out.println("Converted release location in NetDrawingPanel: " + mouseLocInDrawingPanel);

                // IMPORTANT: Ensure DragWireOverlay is hidden/disabled before this call
                // You might need a small delay here if hideWire() has a repaint cycle,
                // but setVisible(false) and setEnabled(false) should be immediate.

                Component deepestComponent = SwingUtilities.getDeepestComponentAt(
                    patchGui.getNetDrawingPanel(),
                    mouseLocInDrawingPanel.x,
                    mouseLocInDrawingPanel.y
                );
                System.out.println("Component found by getDeepestComponentAt(): " + deepestComponent);
                if (deepestComponent != null) {
                    System.out.println("Class of component found: " + deepestComponent.getClass().getName());
                }

                // --- DEBUGGING END ---

                if (deepestComponent instanceof InletInstance) {
                    InletInstance inlet = (InletInstance) deepestComponent;
                    System.out.println("SUCCESS: Released on an InletInstance: " + inlet);
                    // ... (rest of your logic for connecting wires)
                } else {
                    System.out.println("FAILURE: Wire released on non-inlet component or empty space. Released on: " + deepestComponent);
                    // ... (logic for cancelling drag)
                }
            }
        }
    setHighlighted(false);
    e.consume();
    super.mouseReleased(e);
    }
}
