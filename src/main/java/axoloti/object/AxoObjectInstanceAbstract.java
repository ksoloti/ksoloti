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
package axoloti.object;

import axoloti.MainFrame;
import axoloti.Net;
import axoloti.Patch;
import axoloti.PatchGUI;
import axoloti.attribute.AttributeInstance;
import axoloti.displays.DisplayInstance;
import axoloti.inlets.InletInstance;
import axoloti.listener.ObjectModifiedListener;
import axoloti.outlets.OutletInstance;
import axoloti.parameters.ParameterInstance;
import axoloti.sd.SDFileReference;
import axoloti.ui.Theme;

import static axoloti.utils.CharEscape.charEscape;
import static axoloti.utils.FileUtils.toUnixPath;

import axoloti.utils.Constants;
import components.LabelComponent;
import components.TextFieldComponent;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "obj_abstr")
public abstract class AxoObjectInstanceAbstract extends JPanel implements Comparable<AxoObjectInstanceAbstract>, ObjectModifiedListener, MouseListener, MouseMotionListener {

    private static final Logger LOGGER = Logger.getLogger(AxoObjectInstanceAbstract.class.getName());

    @Attribute(name = "type")
    public String typeName;
    @Deprecated
    @Attribute(name = "sha", required = false)
    public String typeSHA;
    @Attribute(name = "uuid", required = false)
    public String typeUUID;
    @Attribute(name = "name", required = false)
    String InstanceName;
    @Attribute
    int x;
    @Attribute
    int y;
    public Patch patch;
    AxoObjectAbstract type;
    private Point dragLocation = null;
    private Point dragAnchor = null;
    protected boolean Selected = false;
    private boolean Locked = false;
    private boolean typeWasAmbiguous = false;
    JPanel Titlebar = new JPanel();
    TextFieldComponent InstanceNameTF;
    LabelComponent InstanceLabel;

    public AxoObjectInstanceAbstract() { }

    public AxoObjectInstanceAbstract(AxoObjectAbstract type, Patch patch1, String InstanceName1, Point location) {
        super();
        this.type = type;
        if (type != null && type.id != null) {
            typeName = type.id;
        } else {
            typeName = "unnamed";
        }
        if (type.createdFromRelativePath && (patch1 != null)) {
            String pPath = patch1.getFileNamePath();
            String oPath = type.sObjFilePath;

            if (oPath.endsWith(".axp") || oPath.endsWith(".axo") || oPath.endsWith(".axs")) {
                oPath = oPath.substring(0, oPath.length() - 4);
            }
            pPath = toUnixPath(pPath);
            oPath = toUnixPath(oPath);
            String[] pPathA = pPath.split("/");
            String[] oPathA = oPath.split("/");
            int i = 0;
            while ((i < pPathA.length) && (i < oPathA.length) && (oPathA[i].equals(pPathA[i]))) {
                i++;
            }
            String rPath = "";
            for (int j = i; j < pPathA.length - 1; j++) {
                rPath += "../";
            }
            if (rPath.isEmpty()) {
                rPath = ".";
            } else {
                rPath = rPath.substring(0, rPath.length() - 1);
            }
            for (int j = i; j < oPathA.length; j++) {
                rPath += "/" + oPathA[j];
            }

            System.out.println(rPath);
            typeName = rPath;
        }

        typeUUID = type.getUUID();
        this.InstanceName = InstanceName1;
        this.x = location.x;
        this.y = location.y;
        this.patch = patch1;
    }

    public Patch getPatch() {
        return patch;
    }

    public PatchGUI getPatchGUI() {
        return (PatchGUI) patch;
    }

    public String getInstanceName() {
        return InstanceName;
    }

    public void setType(AxoObjectAbstract type) {
        this.type = type;
        typeUUID = type.getUUID();
    }

    public void setInstanceName(String InstanceName) {
        if (this.InstanceName.equals(InstanceName)) {
            return;
        }
        if (patch != null) {
            AxoObjectInstanceAbstract o1 = patch.GetObjectInstance(InstanceName);
            if ((o1 != null) && (o1 != this)) {
                LOGGER.log(Level.SEVERE, "Object name already exists: \"{0}\"", InstanceName);
                repaint();
                return;
            }
        }
        this.InstanceName = InstanceName;
        if (InstanceLabel != null) {
            InstanceLabel.setText(InstanceName);
            patch.SetDirty();
        }
    }

    public AxoObjectAbstract getType() {
        return type;
    }

    public AxoObjectAbstract resolveType() {
        if (type != null) {
            return type;
        }

        if (typeUUID != null) {
            type = MainFrame.axoObjects.GetAxoObjectFromUUID(typeUUID);
            if (type != null) {
                // System.out.println("Restored from UUID:" + type.id);
                typeName = type.id;
            }
        }

        if (type == null) {

            if (typeName == null || patch == null) {
                LOGGER.log(Level.SEVERE, "Cannot resolve object. typeName or patch is null.");
                return null;
            }

            ArrayList<AxoObjectAbstract> types = MainFrame.axoObjects.GetAxoObjectFromName(typeName, patch.GetCurrentWorkingDirectory());
            if (types == null) {
                LOGGER.log(Level.SEVERE, "Object not found: \"" + typeName + "\", labeled \"" + InstanceName + "\"");
            }
            else {
                // pick first
                if (types.size() > 1) {
                    typeWasAmbiguous = true;
                }

                type = types.get(0);

                if (type instanceof AxoObjectUnloaded) {
                    AxoObjectUnloaded aou = (AxoObjectUnloaded) type;
                    type = aou.Load();
                    return (AxoObject) type;
                }
            }
        }

        if (type == null) {
            LOGGER.log(Level.SEVERE, "Failed to resolve type for " + InstanceName);
        }
        return type;
    }

    private final Dimension TitleBarMinimumSize = new Dimension(26, 13);
    private final Dimension TitleBarMaximumSize = new Dimension(32768, 13);

    public void PostConstructor() {
        removeAll();
        setMinimumSize(new Dimension(44, 40));
        // setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        // setFocusable(true);
        Titlebar.removeAll();
        Titlebar.setLayout(new BoxLayout(Titlebar, BoxLayout.LINE_AXIS));
        Titlebar.setBackground(Theme.Object_TitleBar_Background);
        Titlebar.setMinimumSize(TitleBarMinimumSize);
        Titlebar.setMaximumSize(TitleBarMaximumSize);

        setBorder(borderUnselected);
        resolveType();

        setBackground(Theme.Object_Default_Background);

        setVisible(true);

        Titlebar.addMouseListener(this);
        addMouseListener(this);

        Titlebar.addMouseMotionListener(this);
        addMouseMotionListener(this);
    }

    JPopupMenu CreatePopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        return popup;
    }

    @Override
    public void mouseClicked(MouseEvent me) {
    }

    @Override
    public void mousePressed(MouseEvent me) {
        handleMousePressed(me);
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        handleMouseReleased(me);
    }

    @Override
    public void mouseEntered(MouseEvent me) {
    }

    @Override
    public void mouseExited(MouseEvent me) {
    }

    @Override
    public void mouseDragged(MouseEvent me) {

        if ((patch != null) && (draggingObjects != null)) {

            Point locOnScreen = me.getLocationOnScreen();
            int dx = locOnScreen.x - dragAnchor.x;
            int dy = locOnScreen.y - dragAnchor.y;

            for (AxoObjectInstanceAbstract o : draggingObjects) {
                
                int nx = o.dragLocation.x + dx;
                int ny = o.dragLocation.y + dy;
                
                if (!me.isShiftDown()) {
                    nx = ((nx + (Constants.X_GRID / 2)) / Constants.X_GRID) * Constants.X_GRID;
                    ny = ((ny + (Constants.Y_GRID / 2)) / Constants.Y_GRID) * Constants.Y_GRID;
                }
                
                if (o.isSelected()) {
                    if (o.x != nx || o.y != ny) {
                        o.setLocation(nx, ny);
                    }
                }
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent me) {
    }

    private void moveToDraggedLayer(AxoObjectInstanceAbstract o) {
        if (getPatchGUI().objectLayerPanel.isAncestorOf(o)) {
            getPatchGUI().objectLayerPanel.remove(o);
            getPatchGUI().draggedObjectLayerPanel.add(o);
        }
    }

    ArrayList<AxoObjectInstanceAbstract> draggingObjects = null;

    protected void handleMousePressed(MouseEvent me) {
        if (patch != null) {
            if (me.isPopupTrigger()) {
                JPopupMenu p = CreatePopupMenu();
                p.show(Titlebar, 0, Titlebar.getHeight());
                me.consume();
            } else if (!IsLocked()) {
                if (me.getButton() == MouseEvent.BUTTON1) {
                    // if (me.getClickCount() == 1) {
                        if (me.isShiftDown()) {
                            if (!patch.getPatchframe().getIgnoreShiftKey()) {
                                SetSelected(!isSelected());
                            }
                            else {
                                /* Skip shift + left click to select/unselect, clear ignoreShiftKey flag */
                                patch.getPatchframe().setIgnoreShiftKey(false);
                            }
                            me.consume();
                        }
                        else if (Selected == false) {
                            ((PatchGUI) patch).SelectNone();
                            SetSelected(true);
                            me.consume();
                        }
                    // }
                    // if (me.getClickCount() == 2) {
                        // ((PatchGUI) patch).ShowClassSelector(AxoObjectInstanceAbstract.this.getLocation(), AxoObjectInstanceAbstract.this, null, true);
                        // me.consume();
                    // }
                }
                draggingObjects = new ArrayList<AxoObjectInstanceAbstract>();
                dragAnchor = me.getLocationOnScreen();
                moveToDraggedLayer(this);
                draggingObjects.add(this);
                dragLocation = getLocation();
                if (isSelected()) {
                    for (AxoObjectInstanceAbstract o : patch.objectInstances) {
                        if (o.isSelected()) {
                            moveToDraggedLayer(o);
                            draggingObjects.add(o);
                            o.dragLocation = o.getLocation();
                        }
                    }
                }
                me.consume();
            }
        }
    }

    private void moveToObjectLayer(AxoObjectInstanceAbstract o, int z) {
        if (getPatchGUI().draggedObjectLayerPanel.isAncestorOf(o)) {
            getPatchGUI().draggedObjectLayerPanel.remove(o);
            getPatchGUI().objectLayerPanel.add(o);
            getPatchGUI().objectLayerPanel.setComponentZOrder(o, z);
        }
    }

    protected void handleMouseReleased(MouseEvent me) {
        if (me.isPopupTrigger()) {
            JPopupMenu p = CreatePopupMenu();
            p.show(Titlebar, 0, Titlebar.getHeight());
            me.consume();
            return;
        }
        int maxZIndex = 0;
        if (draggingObjects != null) {
            if (patch != null) {
                boolean dirtyOnRelease = false;
                for (AxoObjectInstanceAbstract o : draggingObjects) {
                    moveToObjectLayer(o, 0);
                    if (getPatchGUI().objectLayerPanel.getComponentZOrder(o) > maxZIndex) {
                        maxZIndex = getPatchGUI().objectLayerPanel.getComponentZOrder(o);
                    }
                    if (o.x != dragLocation.x || o.y != dragLocation.y) {
                        dirtyOnRelease = true;
                    }
                    o.repaint();
                }
                draggingObjects = null;
                if (dirtyOnRelease) {
                    patch.SetDirty();
                }
                patch.AdjustSize();
            }
            me.consume();
        }
    }

    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        this.x = x;
        this.y = y;
        if (patch != null) {
            repaint();
            for (InletInstance i : GetInletInstances()) {
                Net n = getPatch().GetNet(i);
                if (n != null) {
                    n.updateBounds();
                    n.repaint();
                }
            }
            for (OutletInstance i : GetOutletInstances()) {
                Net n = getPatch().GetNet(i);
                if (n != null) {
                    n.updateBounds();
                    n.repaint();
                }
            }
        }
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    public void addInstanceNameEditor() {
        InstanceNameTF = new TextFieldComponent(InstanceName);
        InstanceNameTF.selectAll();
        InstanceNameTF.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String s = InstanceNameTF.getText();
                setInstanceName(s);
                getParent().remove(InstanceNameTF);
            }
        });
        InstanceNameTF.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                String s = InstanceNameTF.getText();
                setInstanceName(s);
                getParent().remove(InstanceNameTF);
                repaint();
            }

            @Override
            public void focusGained(FocusEvent e) {
            }
        });
        InstanceNameTF.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent ke) {
            }

            @Override
            public void keyReleased(KeyEvent ke) {
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    String s = InstanceNameTF.getText();
                    setInstanceName(s);
                    getParent().remove(InstanceNameTF);
                    repaint();
                }
            }
        });

        getParent().add(InstanceNameTF, 0);
        InstanceNameTF.setLocation(getLocation().x, getLocation().y + InstanceLabel.getLocation().y);
        InstanceNameTF.setSize(getWidth(), 15);
        InstanceNameTF.setMargin(new Insets(-3, 1, 0, 0));
        InstanceNameTF.setVisible(true);
        InstanceNameTF.requestFocus();
    }

    /*
     public class AxoObjectInstanceNameVerifier extends InputVerifier {

     @Override
     public boolean verify(JComponent input) {
     String text = ((TextFieldComponent) input).getText();
     Pattern p = Pattern.compile("[^a-z0-9_]", Pattern.CASE_INSENSITIVE);
     Matcher m = p.matcher(text);
     boolean b = m.find();
     if (b) {
     System.out.println("reject instancename : special character found");
     return false;
     }
     if (patch != null) {
     for (AxoObjectInstanceAbstract o : patch.objectInstances) {
     if (o.InstanceName.equalsIgnoreCase(text) && (AxoObjectInstanceAbstract.this != o)) {
     System.out.println("reject instancename : exists");
     return false;
     }
     }
     }
     return true;
     }
     }
     */
    public String GenerateInstanceDataDeclaration2() {
        return null;
    }

    public String GenerateCodeMidiHandler(String vprefix) {
        return "";
    }

    public String GenerateCallMidiHandler() {
        return "";
    }

    public ArrayList<InletInstance> GetInletInstances() {
        return new ArrayList<InletInstance>();
    }

    public ArrayList<OutletInstance> GetOutletInstances() {
        return new ArrayList<OutletInstance>();
    }

    public ArrayList<ParameterInstance> getParameterInstances() {
        return new ArrayList<ParameterInstance>();
    }

    public ArrayList<AttributeInstance> getAttributeInstances() {
        return new ArrayList<AttributeInstance>();
    }

    public ArrayList<DisplayInstance> GetDisplayInstances() {
        return new ArrayList<DisplayInstance>();
    }

    public InletInstance GetInletInstance(String n) {
        return null;
    }

    public OutletInstance GetOutletInstance(String n) {
        return null;
    }

    public void refreshIndex() {
    }

    static Border borderSelected = BorderFactory.createLineBorder(Theme.Object_Border_Selected);
    static Border borderUnselected = BorderFactory.createLineBorder(Theme.Object_Border_Unselected);
    // static Border borderUnselectedLocked = BorderFactory.createLineBorder(Theme.Object_Border_Unselected_Locked);

    public void SetSelected(boolean Selected) {
        boolean changed = (this.Selected != Selected);
        this.Selected = Selected; /* Update the state immediately */

        if (changed) {
            if (Selected) {
                setBorder(borderSelected);
                Titlebar.setBackground(Theme.Object_Border_Selected);
            } else {
                setBorder(borderUnselected);
                if (this instanceof AxoObjectInstancePatcher) {
                    Titlebar.setBackground(Theme.Object_TitleBar_Subpatch_Background);
                }
                else if (this instanceof AxoObjectInstancePatcherObject) {
                    Titlebar.setBackground(Theme.Object_TitleBar_Embedded_Background);
                }
                else {
                    Titlebar.setBackground(Theme.Object_TitleBar_Background);
                }
            }
            repaint();
        }
    }

    public boolean isSelected() {
        return Selected;
    }

    public void Lock() {
        SetSelected(false);
        Locked = true;
    }

    public void Unlock() {
        if (!this.Selected) setBorder(borderUnselected);
        Locked = false;
    }

    public boolean IsLocked() {
        return Locked;
    }

    public void SetLocation(int x1, int y1) {
        super.setLocation(x1, y1);
        x = x1;
        y = y1;

        if (patch != null) {
            for (Net n : patch.nets) {
                n.updateBounds();
            }
        }
    }

    public void moveToFront() {
        getPatchGUI().objectLayerPanel.setComponentZOrder(this, 0);
    }

    public boolean providesModulationSource() {
        return false;
    }

    @Override
    public int compareTo(AxoObjectInstanceAbstract o) {
        if (o.y == this.y) {
            if (o.x < x) {
                return 1;
            } else if (o.x == x) {
                return 0;
            } else {
                return -1;
            }
        }
        if (o.y < y) {
            return 1;
        } else {
            return -1;
        }
    }

    public String getLegalName() {
        return charEscape(InstanceName);
    }

    public String getCInstanceName() {
        String s = "objectinstance_" + getLegalName();
        return s;
    }

    public void PromoteToOverloadedObj() {
    }

    /*
     public String GenerateStructName() {
     return "";
     }

     public String GenerateDoFunctionName(){
     return "";
     }

     public String GenerateInitFunctionName(){
     return "";
     }
     */
    public String GenerateInitCodePlusPlus(String vprefix) {
        return "";
    }

    public String GenerateDisposeCodePlusPlus(String vprefix) {
        return "";
    }

    public String GenerateClass(String ClassName) {
        return "";
    }

    public boolean hasStruct() {
        return false;
    }

    public boolean hasInit() {
        return false;
    }

    public void resizeToGrid() {
        revalidate();
        Dimension d = getPreferredSize();
        d.width = ((d.width + Constants.X_GRID - 1) / Constants.X_GRID) * Constants.X_GRID;
        d.height = ((d.height + Constants.Y_GRID - 3) / Constants.Y_GRID) * Constants.Y_GRID;
        setSize(d);
    }

    @Override
    public void ObjectModified(Object src) {
    }

    public ArrayList<SDFileReference> GetDependentSDFiles() {
        return null;
    }

    public boolean isTypeWasAmbiguous() {
        return typeWasAmbiguous;
    }

    public void Close() {
        AxoObjectAbstract t = getType();
        if (t != null) {
            t.removeObjectModifiedListener(this);
        }
    }
}
