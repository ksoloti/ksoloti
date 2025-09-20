package axoloti.iolet;

import axoloti.MainFrame;
import axoloti.Net;
import axoloti.NetDragging;
import axoloti.PatchGUI;
import axoloti.inlets.InletInstance;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.outlets.OutletInstance;
import java.awt.Component;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
// import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
// import java.util.logging.Level;
// import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.simpleframework.xml.Attribute;

public abstract class IoletAbstract extends JPanel implements MouseListener, MouseMotionListener {

    // private static final Logger LOGGER = Logger.getLogger(IoletAbstract.class.getName());

    NetDragging dragnet = null;
    IoletAbstract dragtarget = null;
    Net originalNet = null;
    private boolean isRepatching = false;
    private boolean isDuplicating = false;
    private Point pressPoint;

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
        return (name != null) ? name : "unnamed-io";
    }

    public String getObjname() {
        if (objname != null) {
            return objname;
        } else {
            int sepIndex = name.lastIndexOf(' ');
            return name.substring(0, sepIndex);
        }
    }

    public abstract String getDisplayName();

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
            PatchGUI p = getPatchGUI();
            if (p != null) {
                return SwingUtilities.convertPoint(jack, 5, 5, getPatchGUI().Layers);
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

    public PatchGUI getPatchGUI() {
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
            e.consume();
        } else {
            setHighlighted(true);
            if (!axoObj.IsLocked()) {
                pressPoint = e.getPoint();
                isRepatching = false;
                /* If shift is held during mouse button press, the wire will be duplicated/forked */
                isDuplicating = e.isShiftDown();

                if (dragnet == null) {
                    dragnet = new NetDragging(getPatchGUI());
                    if (this instanceof OutletInstance) {
                        dragnet.connectOutlet((OutletInstance) this);
                    } else if (this instanceof InletInstance && isConnected()) {
                        originalNet = axoObj.patch.GetNet(this);
                        if (originalNet != null && !originalNet.GetSource().isEmpty()) {
                            dragnet.connectOutlet(originalNet.GetSource().get(0));
                        }
                    } else {
                        dragnet.connectInlet((InletInstance) this);
                    }
                }
            }
            e.consume();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            getPopup().show(this, 0, getHeight() - 1);
            e.consume();
        } else if (dragnet != null) {
            final PatchGUI pg = getPatchGUI();
            if (pg == null) return;

            dragnet.repaint();
            pg.draggedObjectLayerPanel.remove(dragnet);
            
            Net n = null;
            
            // String sourceName = this.getName();
            // String targetName = (dragtarget != null) ? dragtarget.getName() : "null-target";
            // LOGGER.info("mouseReleased on IoletAbstract. Dragnet is null: " + (dragnet == null) + ", dragtarget is null: " + (dragtarget == null));

            if (this instanceof OutletInstance) {
                if (dragtarget instanceof InletInstance) {
                    // LOGGER.info("New connection from outlet to inlet: " + sourceName + " -> " + targetName);
                    n = pg.AddConnection((InletInstance) dragtarget, (OutletInstance) this);
                }
            } else if (this instanceof InletInstance) {
                if (dragtarget instanceof InletInstance) {
                    // LOGGER.info("Repatch attempt from inlet to inlet. Dragging from: " + sourceName + " to: " + targetName);

                    if (originalNet != null && !originalNet.GetSource().isEmpty()) {
                        OutletInstance sourceOutlet = originalNet.GetSource().get(0);
                        
                        if (dragtarget == this) {
                            // LOGGER.info("Re-patch to same inlet detected. Restoring original connection.");
                            /* Allows to reconnect the net to its original inlet */
                            originalNet.connectInlet((InletInstance) dragtarget);
                            originalNet.updateBounds();
                            n = originalNet;
                        } else {
                            // LOGGER.info("Connecting to a new inlet: " + targetName);
                            n = pg.AddConnection((InletInstance) dragtarget, sourceOutlet);
                        }
                        
                        if (n != null && n != originalNet && originalNet != null) {
                            pg.delete(originalNet);
                            // LOGGER.info("Deleted original net after successful repatch.");
                        }
                    } else {
                        // LOGGER.warning("Cannot repatch: Original net is invalid.");
                    }

                } else {
                    // LOGGER.info("Drag from " + sourceName + " released in empty space. Checking for net deletion.");
                    if (originalNet != null && originalNet.GetDest().isEmpty()) {
                        pg.delete(originalNet);
                        // LOGGER.info("Single-destination net deleted due to release in empty space.");
                    }
                }
            }
            
            if (n != null) {
                pg.SetDirty();
                // LOGGER.info("Change made. Setting patch dirty.");
            }
            
            /* Final cleanup */
            dragnet = null;
            dragtarget = null;
            originalNet = null;
            isRepatching = false;
            pg.draggedObjectLayerPanel.repaint();
            e.consume();
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
        if (!axoObj.IsLocked()) {
            if (!isRepatching && pressPoint.distance(e.getPoint()) > 5) {
                isRepatching = true;

                /* Only disconnect if not duplicating (shift + mouse press) */
                if (!isDuplicating && this instanceof InletInstance && isConnected() && originalNet != null) {
                    originalNet.disconnectInlet((InletInstance) this);
                    originalNet.updateBounds();
                }
                dragnet.setVisible(true);
                getPatchGUI().draggedObjectLayerPanel.add(dragnet);
            }

            /* Handle the visual movement of the wire */
            if (dragnet != null) {
                final PatchGUI patchGUI = getPatchGUI();
                if (patchGUI == null) return;
                
                Point p = SwingUtilities.convertPoint(this, e.getPoint(), patchGUI.objectLayerPanel);
                Component c = patchGUI.objectLayerPanel.findComponentAt(p);
                while ((c != null) && !(c instanceof IoletAbstract)) {
                    c = c.getParent();
                }

                if ((c != null) && !((this instanceof OutletInstance) && (c instanceof OutletInstance))) {
                    if (c != dragtarget) {
                        dragtarget = (IoletAbstract) c;
                        Point jackLocation = dragtarget.getJackLocInCanvas();
                        dragnet.SetDragPoint(jackLocation);
                    }
                } else {
                    if (dragnet != null) {
                        dragnet.SetDragPoint(p);
                        dragtarget = null;
                    }
                }
            }
        }
        e.consume();
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
                        axoObj.getPatchGUI().SetCordsInBackground(!highlighted);
                    }
                    else {
                        axoObj.getPatchGUI().SetCordsInBackground(true);
                    }
                }
            }
        }
    }

    public void disconnect() {
        // only called from GUI action
        if (axoObj.patch != null) {
            Net n = axoObj.patch.disconnect(this);
            if (n != null) {
                axoObj.patch.SetDirty();
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
            }
        }
    }
}
