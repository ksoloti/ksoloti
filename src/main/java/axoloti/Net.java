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
package axoloti;

import axoloti.datatypes.DataType;
import axoloti.inlets.InletInstance;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.outlets.OutletInstance;
import axoloti.ui.Theme;
import axoloti.utils.GraphicsUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.simpleframework.xml.*;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "net")
public class Net extends JComponent {

    private static final Logger LOGGER = Logger.getLogger(Net.class.getName());

    @ElementList(inline = true, required = false)
    ArrayList<OutletInstance> source;
    @ElementList(inline = true, required = false)
    ArrayList<InletInstance> dest = new ArrayList<InletInstance>();
    Patch patch;
    boolean selected = false;

    final static float[] dash = {3.f, 6.f};
    final static Stroke strokeValidSelected = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    final static Stroke strokeValidDeselected = new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    final static Stroke strokeBrokenSelected = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0.f);
    final static Stroke strokeBrokenDeselected = new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0.f);
    final static Stroke strokeDragging = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0.f);
    final QuadCurve2D.Float curve = new QuadCurve2D.Float();

    public Net() {
        if (source == null) {
            source = new ArrayList<OutletInstance>();
        }
        if (dest == null) {
            dest = new ArrayList<InletInstance>();
        }

        setSize(1, 1);
        setLocation(0, 0);
        setOpaque(false);
    }

    public Net(Patch patch) {
        this();
        this.patch = patch;
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
            OutletInstance r = o.getOutletInstance(outletname);
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
                LOGGER.log(Level.SEVERE, "Could not resolve net dest obj: {0}::{1}", new Object[]{i.getObjname(), i.getInletname()});
                patch.nets.remove(this);
                return;
            }
            InletInstance r = o.getInletInstance(inletname);
            if (r == null) {
                LOGGER.log(Level.SEVERE, "Could not resolve net dest inlet: {0}::{1}", new Object[]{i.getObjname(), i.getInletname()});
                patch.nets.remove(this);
                return;
            }
            dest2.add(r);
        }
        source = source2;
        dest = dest2;
        updateBounds();
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
        repaint();
    }

    public boolean getSelected() {
        return this.selected;
    }

    public void connectInlet(InletInstance inlet) {
        if (inlet.GetObjectInstance().patch != patch) {
            return;
        }
        dest.add(inlet);
        updateBounds();
    }

    public boolean disconnectInlet(InletInstance inlet) {
        if (inlet != null) {
            return dest.remove(inlet);
        }
        return false;
    }

    public void connectOutlet(OutletInstance outlet) {
        if (outlet.GetObjectInstance().patch == patch) {
            source.add(outlet);
        }
        updateBounds();
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

    float CtrlPointY(float x1, float y1, float x2, float y2) {
        float looseness = 0.05f;
        return Math.max(y1, y2) + Math.abs(y2 - y1) * looseness + Math.abs(x2 - x1) * looseness;
    }

    void DrawWire(Graphics2D g2, float x1, float y1, float x2, float y2) {
        curve.setCurve(x1, y1, (x1 + x2) / 2, CtrlPointY(x1, y1, x2, y2), x2, y2);
        // curve.setCurve(x1, y1, x2, y2, x2, y2); /* this would make straight lines */
        g2.draw(curve);
    }

    public void updateBounds() {
        int min_y = Integer.MAX_VALUE;
        int min_x = Integer.MAX_VALUE;
        int max_y = Integer.MIN_VALUE;
        int max_x = Integer.MIN_VALUE;

        /* Create a single list to hold all points that define the net */
        ArrayList<Point> points = new ArrayList<>();
        
        /* Add all inlet points */
        for (InletInstance i : dest) {
            points.add(i.getJackLocInCanvas());
        }
        /* Add all outlet points (for a valid net, this will be just one) */
        for (OutletInstance i : source) {
            points.add(i.getJackLocInCanvas());
        }

        /* Iterate over the single list to find the bounds */
        for (Point p : points) {
            min_x = Math.min(min_x, p.x);
            min_y = Math.min(min_y, p.y);
            max_x = Math.max(max_x, p.x);
            max_y = Math.max(max_y, p.y);
        }
        
        int fudge = 8;
        this.setBounds(min_x - fudge, min_y - fudge,
                Math.max(1, max_x - min_x + (2 * fudge)),
                (int)CtrlPointY(min_x, min_y, max_x, max_y) - min_y + (2 * fudge));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        float shadowOffset = 1.0f;
        Graphics2D g2 = GraphicsUtils.configureGraphics(g);
        Point p0;
        Color c;

        if (isValidNet()) {
            if (selected) {
                g2.setStroke(strokeValidSelected);
                c = GetDataType().GetColorHighlighted();
            } else {
                g2.setStroke(strokeValidDeselected);
                c = GetDataType().GetColor();
            }

            p0 = source.get(0).getJackLocInCanvas();
        } else {
            if (selected) {
                g2.setStroke(strokeBrokenSelected);
            } else {
                g2.setStroke(strokeBrokenDeselected);
            }

            if (GetDataType() != null) {
                c = GetDataType().GetColor();
            } else {
                c = Theme.Cable_Sourceless;
            }

            if (!source.isEmpty()) {
                p0 = source.get(0).getJackLocInCanvas();
            } else if (!dest.isEmpty()) {
                p0 = dest.get(0).getJackLocInCanvas();
            } else {
                LOGGER.log(Level.SEVERE, "Empty nets should not exist");
                return;
            }
        }

        Point from = SwingUtilities.convertPoint(getPatchGUI().Layers, p0, this);
        from.x -= 1; /* Compensate for outlet graphic shift */
        Color c_shadow = c.darker().darker();
        for (InletInstance i : dest) {
            Point p1 = i.getJackLocInCanvas();

            Point to = SwingUtilities.convertPoint(getPatchGUI().Layers, p1, this);

            g2.setColor(c_shadow); /* derive wire shadow color from actual color */
            if (from.x > to.x) {
                /* Wire goes right-to-left */
                if (from.y > to.y) {
                    /* Wire goes upwards, i.e. starts lower than it ends */
                    DrawWire(g2, from.x - shadowOffset, from.y + shadowOffset, to.x - shadowOffset, to.y + shadowOffset);
                }
                else {
                    /* Wire goes downwards, i.e. starts higher than it ends */
                    DrawWire(g2, from.x + shadowOffset, from.y + shadowOffset, to.x + shadowOffset, to.y + shadowOffset);
                }
            }
            else {
                /* Wire goes left-to-right */
                if (from.y > to.y) {
                    /* Wire goes upwards, i.e. starts lower than it ends */
                    DrawWire(g2, from.x + shadowOffset, from.y + shadowOffset, to.x + shadowOffset, to.y + shadowOffset);
                }
                else {
                    /* Wire goes downwards, i.e. starts higher than it ends */
                    DrawWire(g2, from.x - shadowOffset, from.y + shadowOffset, to.x - shadowOffset, to.y + shadowOffset);
                }
            }

            g2.setColor(c);
            DrawWire(g2, from.x, from.y, to.x, to.y);
        }
    }

    public PatchGUI getPatchGUI() {
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
            for (OutletInstance i : o.getOutletInstances()) {
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
        // java.util.Collections.sort(source);
        DataType t = source.get(0).GetDataType();
        return t;
    }
    
    public ArrayList<OutletInstance> GetSource() {
        return source;
    }

    public ArrayList<InletInstance> GetDest() {
        return dest;
    }

    public String CType() {
        DataType d = GetDataType();
        if (d != null) {
            return d.CType();
        } else {
            return null;
        }
    }

    public String GetCName() {
        int i = patch.nets.indexOf(this);
        return "net" + i;
    }
}
