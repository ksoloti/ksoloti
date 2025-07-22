package axoloti.iolet;

import axoloti.MainFrame;
import axoloti.Patch;
import axoloti.PatchGUI;
import axoloti.inlets.InletInstance;
import axoloti.net.Net;
import axoloti.net.NetDragging;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.outlets.OutletInstance;
import java.awt.Component;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
// import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.simpleframework.xml.Attribute;

public abstract class IoletAbstract extends JPanel implements MouseListener, MouseMotionListener {

    @Deprecated
    @Attribute(required = false)
    public String name;
    @Attribute(name = "obj", required = false)
    public String objname;

    public AxoObjectInstanceAbstract axoObj;
    public JLabel lbl;
    public JComponent jack;

    @Deprecated
    public String getName() {
        return name;
    }

    public String getObjname() {
        if (objname != null) {
            return objname;
        } else {
            int sepIndex = name.lastIndexOf(' ');
            return name.substring(0, sepIndex);
        }
    }

    public AxoObjectInstanceAbstract GetObjectInstance() {
        return axoObj;
    }

    private Point getJackLocInCanvasHidden() {
        Point p1 = new Point(5, 5);
        Component p = (Component) jack;
        while (p != null) {
            p1.x = p1.x + p.getX();
            p1.y = p1.y + p.getY();
            if (p == axoObj) {
                break;
            }
            p = (Component) p.getParent();
        }
        return p1;
    }

    public Point getJackLocInCanvas() {
        try {
            PatchGUI p = getPatchGui();
            if (p != null) {
                return SwingUtilities.convertPoint(jack, 5, 5, getPatchGui().Layers);
            } else {
                return getJackLocInCanvasHidden();
            }
        } catch (IllegalComponentStateException e) {
            return getJackLocInCanvasHidden();
        } catch (NullPointerException e) {
            return getJackLocInCanvasHidden();
        }
    }

    abstract public JPopupMenu getPopup();

    public PatchGUI getPatchGui() {
        try {
            return (PatchGUI) axoObj.getPatch();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            getPopup().show(this, 0, getHeight() - 1);
        } else {
            setHighlighted(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            getPopup().show(this, 0, getHeight() - 1);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setHighlighted(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setHighlighted(false);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    public boolean isConnected() {
        if (axoObj == null) {
            return false;
        }
        if (axoObj.patch == null) {
            return false;
        }
        return (axoObj.patch.GetNet(this) != null);
    }

    public void setHighlighted(boolean highlighted) {
        if ((getRootPane() == null
                || getRootPane().getCursor() != MainFrame.transparentCursor)
                && axoObj != null
                && axoObj.patch != null) {
            Net n = axoObj.patch.GetNet(this);

            if (n != null && n.getSelected() != highlighted) {
                n.setSelected(highlighted);

                if (axoObj.patch.getPatchframe().getPatchCordsInBackground()) {
                    if (!axoObj.patch.IsLocked()) {
                        /* temporarily raise cables to top */
                        axoObj.getPatchGui().SetCordsInBackground(!highlighted);
                    }
                    else {
                        axoObj.getPatchGui().SetCordsInBackground(true);
                    }
                }
                getPatchGui().repaintPatch();
            }
        }
    }

public void disconnect() {
        // only called from GUI action
        if (axoObj.patch != null) {
            Patch currentPatch = axoObj.patch; // Get the Patch instance

            boolean disconnectedResult = false; // To track if any disconnection occurred

            // Check if this IoletAbstract instance is an OutletInstance
            if (this instanceof OutletInstance) {
                OutletInstance outlet = (OutletInstance) this;
                disconnectedResult = currentPatch.disconnect(outlet);
            }
            // Check if this IoletAbstract instance is an InletInstance
            else if (this instanceof InletInstance) {
                InletInstance inlet = (InletInstance) this;
                disconnectedResult = currentPatch.disconnect(inlet);
            }

            if (disconnectedResult) {
                currentPatch.SetDirty(true); // Mark patch as dirty if a disconnection happened
                // You might also want to trigger a repaint here if your
                // PatchGUI doesn't automatically repaint on SetDirty()
                // e.g., if (currentPatch instanceof PatchGUI) { ((PatchGUI) currentPatch).repaintPatch(); }
            }
        }
    }

    public void deleteNet() {
        // only called from GUI action
        if (axoObj.patch != null) {
            Net n = axoObj.patch.GetNet(this);
            n = axoObj.patch.delete(n);
            if (n != null) {
                axoObj.patch.SetDirty();
                getPatchGui().repaintPatch();
            }
        }
    }
}
