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
package axoloti.net;

import axoloti.Patch;
import axoloti.PatchGUI;
import axoloti.Theme;
import axoloti.datatypes.DataType;
import axoloti.inlets.InletInstance;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.outlets.OutletInstance;
import axoloti.utils.Constants;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.simpleframework.xml.*;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "net")
public class Net {

    private static final Logger LOGGER = Logger.getLogger(Net.class.getName());

    @ElementList(inline = true, required = false)
    ArrayList<OutletInstance> source;
    @ElementList(inline = true, required = false)
    ArrayList<InletInstance> dest;
    Patch patch;
    boolean selected = false;
    float shadowOffset = 0.5f;

    public Net() {
        if (source == null) {
            source = new ArrayList<OutletInstance>();
        }
        if (dest == null) {
            dest = new ArrayList<InletInstance>();
        }

    }

    public Net(Patch patch) {
        this();
        this.patch = patch;
    }

    public void SetPatch(Patch p) {
        if (p != null) {
            this.patch = p;
        }
    }

    public void PostConstructor() {
        // InletInstances and OutletInstances actually already exist, need to replace dummies with the real ones
        ArrayList<OutletInstance> source2 = new ArrayList<OutletInstance>();
        for (OutletInstance i : source) {
            String objname = i.getObjname();
            String outletname = i.getOutletname();
            AxoObjectInstanceAbstract o = patch.GetObjectInstance(objname);
            if (o == null) {
                LOGGER.log(Level.SEVERE, "Could not resolve net source object: {0}::{1}", new Object[]{i.getObjname(), i.getOutletname()});
                patch.nets.remove(this);
                return;
            }
            OutletInstance r = o.GetOutletInstance(outletname);
            if (r == null) {
                LOGGER.log(Level.SEVERE, "Could not resolve net source outlet: {0}::{1}", new Object[]{i.getObjname(), i.getOutletname()});
                patch.nets.remove(this);
                return;
            }
            source2.add(r);
        }
        ArrayList<InletInstance> dest2 = new ArrayList<InletInstance>();
        for (InletInstance i : dest) {
            String objname = i.getObjname();
            String inletname = i.getInletname();
            AxoObjectInstanceAbstract o = patch.GetObjectInstance(objname);
            if (o == null) {
                LOGGER.log(Level.SEVERE, "Could not resolve net dest obj :{0}::{1}", new Object[]{i.getObjname(), i.getInletname()});
                patch.nets.remove(this);
                return;
            }
            InletInstance r = o.GetInletInstance(inletname);
            if (r == null) {
                LOGGER.log(Level.SEVERE, "Could not resolve net dest inlet :{0}::{1}", new Object[]{i.getObjname(), i.getInletname()});
                patch.nets.remove(this);
                return;
            }
            dest2.add(r);
        }
        source = source2;
        dest = dest2;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (this.selected == selected) {
            return;
        }
        this.selected = selected;
        for (OutletInstance i : source) {
            i.setHighlighted(selected);
        }
        for (InletInstance i : dest) {
            i.setHighlighted(selected);
        }
    }

    public boolean getSelected() {
        return this.selected;
    }

    public void connectInlet(InletInstance inlet) {
        if (inlet.GetObjectInstance().patch != patch) {
            return;
        }
        dest.add(inlet);
    }

    public void connectOutlet(OutletInstance outlet) {
        if (outlet.GetObjectInstance().patch == patch) {
            source.add(outlet);
        }
    }

    public boolean isValidNet() {
        if (source.isEmpty()) {
            return false;
        }
        if (source.size() > 1) {
            return false;
        }
        if (dest.isEmpty()) {
            return false;
        }
        for (InletInstance s : dest) {
            if (!GetDataType().IsConvertableToType(s.GetDataType())) {
                return false;
            }
        }
        return true;
    }

    Color GetColor() {
        Color c = GetDataType().GetColor();
        if (c == null) {
            c = Theme.Cable_Default;
        }
        return c;
    }

    final QuadCurve2D.Float curve = new QuadCurve2D.Float();

    static float GetCtrlPointY(float x1, float y1, float x2, float y2) {
        float looseness = 0.05f;
        return Math.max(y1, y2) + Math.abs(y2 - y1) * looseness + Math.abs(x2 - x1) * looseness;
    }

    public void draw(Graphics g) {
        if (source.isEmpty()) {
            return; // Nothing to draw if there's no source
        }

        Color c;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (isValidNet()) {
            if (selected) {
                g2.setStroke(Theme.Cable_Stroke_Valid_Selected);
            } else {
                g2.setStroke(Theme.Cable_Stroke_Valid_Deselected);
            }

            c = GetDataType().GetColor();
        } else {
            if (selected) {
                g2.setStroke(Theme.Cable_Stroke_Broken_Selected);
            } else {
                g2.setStroke(Theme.Cable_Stroke_Broken_Deselected);
            }

            if (GetDataType() != null) {
                c = GetDataType().GetColor();
            } else {
                c = Theme.Cable_Shadow;
            }
        }
        
        Point from = source.get(0).getJackLocInCanvas(); // The single source point for this Net
        if (from == null) {
            // Log a warning: source jack location is null
            // LOGGER.log(Level.WARNING, "Source outlet jack location is null for net.");
            return;
        }

        for (InletInstance d : dest) {
            Point to = d.getJackLocInCanvas(); // Get the location of this specific destination inlet
            if (to == null) {
                // Log a warning: destination jack location is null
                // LOGGER.log(Level.WARNING, "Destination inlet jack location is null for {0}", d.GetLabel());
                continue; // Skip this destination but continue with others
            }

            // Draw shadow wire
            g2.setColor(Theme.Cable_Shadow); // Assuming Theme.Cable_Shadow is defined
            DrawWire(g2, from.x + shadowOffset, from.y + shadowOffset, to.x + shadowOffset, to.y + shadowOffset);

            // Draw main wire
            g2.setColor(c);
            DrawWire(g2, from.x, from.y, to.x, to.y);
        }
    }

    private void DrawWire(Graphics2D g2, float x1, float y1, float x2, float y2) {
        QuadCurve2D q = new QuadCurve2D.Float();
        float mid_x = (x1 + x2) / 2;
        float ctrlPointY = GetCtrlPointY(x1, y1, x2, y2);
        q.setCurve(x1, y1, mid_x, ctrlPointY, x2, y2);
        // q.setCurve(x1, y1, x2, y2, x2, y2); /* this would make straight lines */
        g2.draw(q);
    }

    public PatchGUI getPatchGui() {
        return (PatchGUI) patch;
    }

    public boolean NeedsLatch() {
        // reads before last write on net
        int lastSource = 0;
        for (OutletInstance s : source) {
            int i = patch.objectInstances.indexOf(s.GetObjectInstance());
            if (i > lastSource) {
                lastSource = i;
            }
        }
        int firstDest = java.lang.Integer.MAX_VALUE;
        for (InletInstance d : dest) {
            int i = patch.objectInstances.indexOf(d.GetObjectInstance());
            if (i < firstDest) {
                firstDest = i;
            }
        }
        return (firstDest <= lastSource);
    }

    public boolean IsFirstOutlet(OutletInstance oi) {
        if (source.size() == 1) {
            return true;
        }
        for (AxoObjectInstanceAbstract o : patch.objectInstances) {
            for (OutletInstance i : o.GetOutletInstances()) {
                if (source.contains(i)) {
                    // o is first objectinstance connected to this net
                    return oi == i;
                }
            }
        }
        LOGGER.log(Level.SEVERE, "IsFirstOutlet: shouldn't get here");
        return false;
    }

    public DataType GetDataType() {
        if (source.isEmpty()) {
            return null;
        }
        if (source.size() == 1) {
            return source.get(0).GetDataType();
        }
        java.util.Collections.sort(source);
        DataType t = source.get(0).GetDataType();
        return t;
    }

    public void SetSources(ArrayList<OutletInstance> sources) {
        if (sources != null) {
            source = sources;
        }
    }

    public void SetDests(ArrayList<InletInstance> dests) {
        if (dests != null) {
            dest = dests;
        }
    }

    public ArrayList<OutletInstance> GetSource() {
        return source;
    }

    public ArrayList<InletInstance> GetDests() {
            return dest;
    }

    public void addSource(OutletInstance o) {
        if (!source.contains(o)) {
            source.add(o);
        }
    }

    public void addDest(InletInstance d) {
        if (!dest.contains(d)) {
            dest.add(d);
        }
    }

    public void removeSource(OutletInstance o) {
        source.remove(o);
    }

    public void removeDest(InletInstance d) {
        dest.remove(d);
    }

    public String CType() {
        DataType d = GetDataType();
        if (d != null) {
            return d.CType();
        } else {
            return null;
        }
    }

    public String CName() {
        int i = patch.nets.indexOf(this);
        return "net" + i;
    }
}
