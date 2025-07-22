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
package axoloti.inlets;

import axoloti.PatchGUI;
import axoloti.Theme;
import axoloti.atom.AtomInstance;
import axoloti.datatypes.DataType;
import axoloti.iolet.IoletAbstract;
import axoloti.net.Net;
import axoloti.net.NetDragging;
import axoloti.object.AxoObjectInstance;
import axoloti.outlets.OutletInstance;
import components.JackInputComponent;
import components.LabelComponent;
import components.SignalMetaDataIcon;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPopupMenu;
import org.simpleframework.xml.*;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "dest")
public class InletInstance<T extends Inlet> extends IoletAbstract implements AtomInstance<T> {

    @Attribute(name = "inlet", required = false)
    public String inletname;

    private final T inlet;

    public String getInletname() {
        if (inletname != null) {
            return inletname;
        } else {
            int sepIndex = name.lastIndexOf(' ');
            return name.substring(sepIndex + 1);
        }
    }

    @Override
    public T GetDefinition() {
        return inlet;
    }

    public InletInstance() {
        this.inlet = null;
        this.axoObj = null;
        this.setBackground(Theme.Object_Default_Background);
    }

    public InletInstance(T inlet, final AxoObjectInstance axoObj) {
        this.inlet = inlet;
        this.axoObj = axoObj;
        RefreshName();
        PostConstructor();
    }

    public final void RefreshName() {
        name = axoObj.getInstanceName() + " " + inlet.name;
        objname = axoObj.getInstanceName();
        inletname = inlet.name;
        name = null;
    }

    public DataType GetDataType() {
        return inlet.getDatatype();
    }

    public String GetCName() {
        return inlet.GetCName();
    }

    public String GetLabel() {
        return inlet.name;
    }

    public final void PostConstructor() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setBackground(Theme.Object_Default_Background);
        this.setMinimumSize(new Dimension(10, 10)); // Minimum size
        this.setPreferredSize(new Dimension(10, 10)); // Preferred size
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10)); // Allow horizontal stretch if needed, but fix height
        jack = new JackInputComponent(this);
        jack.setForeground(inlet.getDatatype().GetColor());
        jack.setBackground(Theme.Object_Default_Background);
        add(jack);
        add(new SignalMetaDataIcon(inlet.GetSignalMetaData()));
        if (axoObj.getType().GetInlets().size() > 1) {
            add(Box.createHorizontalStrut(2));
            add(new LabelComponent(inlet.name));
        }
        add(Box.createHorizontalGlue());
        setToolTipText(inlet.description);

        addMouseListener(this);
        addMouseMotionListener(this);
            System.out.println("InletInstance Created: " + getName()); // Assuming getName() exists or add a unique ID
    System.out.println("  Bounds: " + getBounds()); // Check actual component bounds
    System.out.println("  Location in Parent: " + getLocation()); // Relative to its parent
    System.out.println("  Size: " + getSize()); // Explicit size check
    System.out.println("  isVisible(): " + isVisible());
    System.out.println("  isOpaque(): " + isOpaque()); // Should ideally be false if you draw on it
    System.out.println("  getJackLocInCanvas(): " + getJackLocInCanvas()); // Its global connection point
    }

    public Inlet getInlet() {
        return inlet;
    }

    @Override
    public JPopupMenu getPopup() {
        return new InletInstancePopupMenu(this);
    }

    // --- NEW MOUSE EVENT OVERRIDES FOR INLETINSTANCE ---

    @Override
    public void mousePressed(MouseEvent e) {
        // Handle popup trigger first, as it takes precedence
        if (e.isPopupTrigger()) {
            getPopup().show(this, 0, getHeight() - 1);
            e.consume(); // Consume if it's a popup
            return;
        }

        // You could add logic here to *start* a drag from an inlet
        // For example, if you want to disconnect an existing wire by dragging it away.
        // This is more complex as it requires finding the Net connected to this inlet
        // and setting it as the source of the new drag.
        // For now, we'll let the super.mousePressed() handle basic highlighting.

        super.mousePressed(e); // Fallback to superclass logic (e.g., highlighting)
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // If an outlet has started a drag and the mouse is currently over this inlet,
        // you might want to provide visual feedback (e.g., highlighting this inlet).
        // However, the primary drag update is handled by the component that initiated the drag (OutletInstance).
        // We generally don't consume mouseDragged events here unless this InletInstance
        // itself initiated a drag operation.
        PatchGUI patchGui = getPatchGui();
        if (patchGui != null) {
            NetDragging currentDraggedNet = patchGui.netDragging; // Get the currently dragged net
            if (currentDraggedNet != null) {
                // If there's an active drag, let the source update its position.
                // We don't necessarily need to consume here.
            }
        }
        super.mouseDragged(e); // Allow superclass to handle (e.g., highlighting)
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Handle popup trigger first
        if (e.isPopupTrigger()) {
            getPopup().show(this, 0, getHeight() - 1);
            e.consume();
            return;
        }

        PatchGUI patchGui = getPatchGui();
        if (patchGui != null) {
            // Get the active dragged net directly from patchGui.netDragging
            NetDragging currentDraggedNet = patchGui.netDragging;

            // This InletInstance should only act if a drag is currently active
            // AND if the drag originated from an OutletInstance (a common scenario for connections)
            if (currentDraggedNet != null && currentDraggedNet.getDraggingSourceOutlet() != null) {
                OutletInstance sourceOutlet = currentDraggedNet.getDraggingSourceOutlet();

                // Un-highlight any previously highlighted Iolet (e.g., the source outlet)
                sourceOutlet.setHighlighted(false); // Make sure OutletInstance has setHighlighted(boolean)

                // Hide the temporary wire (this is handled by NetDragging.EndDragging)
                currentDraggedNet.EndDragging();

                // Now, attempt to make the connection
                if (sourceOutlet.GetDataType() != null && this.GetDataType() != null &&
                    sourceOutlet.GetDataType().IsConvertableToType(this.GetDataType())) {

                    boolean alreadyConnected = false;
                    for (Net existingNet : patchGui.GetNets()) {
                        if (existingNet.GetSource().contains(sourceOutlet) && existingNet.GetDests().contains(this)) {
                            alreadyConnected = true;
                            break;
                        }
                    }

                    if (!alreadyConnected) {
                        patchGui.connect(sourceOutlet, this); // Connect the source outlet to this inlet
                        patchGui.SetDirty(true); // Mark the patch as dirty
                    } else {
                        System.out.println("Connection already exists between " + sourceOutlet.getName() + " and " + this.getName());
                    }
                } else {
                    System.out.println("Invalid connection: Incompatible data types between " + sourceOutlet.GetDataType() + " and " + this.GetDataType());
                }

                // Clear the active dragged net state in PatchGUI, as the drag operation is complete
                patchGui.setActiveNetDragging(null);

                e.consume(); // Consume the event as this InletInstance handled the drag end
                return;
            }
        }

        // Fallback to superclass logic
        super.mouseReleased(e);
    }

    // --- END NEW MOUSE EVENT OVERRIDES ---
}
