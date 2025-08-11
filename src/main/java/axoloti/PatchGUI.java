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
import axoloti.iolet.IoletAbstract;
import axoloti.object.AxoObjectAbstract;
import axoloti.object.AxoObjectFromPatch;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.object.AxoObjectInstanceZombie;
import axoloti.object.AxoObjectZombie;
import axoloti.outlets.OutletInstance;
import axoloti.ui.Theme;
import axoloti.utils.Constants;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;
import axoloti.utils.StringRef;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
// import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.xml.stream.XMLStreamException;

import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.stream.Format;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "patch-1.0")
public class PatchGUI extends Patch {

    private static final Logger LOGGER = Logger.getLogger(PatchGUI.class.getName());

    /* KeyCode integers */
    // static final int keyCodeList[] = {
    //     KeyEvent.VK_A,
    //     KeyEvent.VK_B,
    //     KeyEvent.VK_C,
    //     KeyEvent.VK_D,
    //     KeyEvent.VK_E,
    //     KeyEvent.VK_F,
    //     KeyEvent.VK_G,
    //     KeyEvent.VK_H,
    //     KeyEvent.VK_I,
    //     KeyEvent.VK_J,
    //     KeyEvent.VK_K,
    //     KeyEvent.VK_L,
    //     KeyEvent.VK_M,
    //     KeyEvent.VK_N,
    //     KeyEvent.VK_O,
    //     KeyEvent.VK_P,
    //     KeyEvent.VK_Q,
    //     KeyEvent.VK_R,
    //     KeyEvent.VK_S,
    //     KeyEvent.VK_T,
    //     KeyEvent.VK_U,
    //     KeyEvent.VK_V,
    //     KeyEvent.VK_W,
    //     KeyEvent.VK_X,
    //     KeyEvent.VK_Y,
    //     KeyEvent.VK_Z
    // };


    private final static int capitalLetterOffset = 26;

    private int MousePressedBtn = 0;
    private float dspLoadPercent = 0.0f;

    public JLayeredPane Layers = new JLayeredPane();

    public JPanel objectLayerPanel = new JPanel();
    public JPanel draggedObjectLayerPanel = new JPanel();
    public JPanel netLayerPanel = new JPanel();
    public JPanel selectionRectLayerPanel = new JPanel();

    JLayer<JComponent> objectLayer = new JLayer<JComponent>(objectLayerPanel);
    JLayer<JComponent> draggedObjectLayer = new JLayer<JComponent>(draggedObjectLayerPanel);
    JLayer<JComponent> netLayer = new JLayer<JComponent>(netLayerPanel);
    JLayer<JComponent> selectionRectLayer = new JLayer<JComponent>(selectionRectLayerPanel);

    SelectionRectangle selectionrectangle = new SelectionRectangle();
    Point selectionRectStart;
    Point panOrigin;
    public AxoObjectFromPatch ObjEditor;

    public PatchGUI() {
        super();

        Layers.setLayout(null);
        Layers.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
        Layers.setLocation(0, 0);
        Layers.setFont(Constants.FONT);

        JComponent[] layerComponents = {
            objectLayer, objectLayerPanel, draggedObjectLayerPanel, netLayerPanel,
            selectionRectLayerPanel, draggedObjectLayer, netLayer, selectionRectLayer};
        for (JComponent c : layerComponents) {
            c.setLayout(null);
            c.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
            c.setLocation(0, 0);
            c.setOpaque(false);
            c.setFont(Constants.FONT);
            c.validate();
        }

        Layers.add(objectLayer, Integer.valueOf(1));
        Layers.add(netLayer, Integer.valueOf(2));
        Layers.add(draggedObjectLayer, Integer.valueOf(3));
        Layers.add(selectionRectLayer, Integer.valueOf(4));

        objectLayer.setName("objectLayer");
        draggedObjectLayer.setName("draggedObjectLayer");
        netLayer.setName("netLayer");
        netLayerPanel.setName("netLayerPanel");
        selectionRectLayerPanel.setName("selectionRectLayerPanel");
        selectionRectLayer.setName("selectionRectLayer");

        objectLayerPanel.setName(Constants.OBJECT_LAYER_PANEL);
        draggedObjectLayerPanel.setName(Constants.DRAGGED_OBJECT_LAYER_PANEL);

        selectionRectLayerPanel.add(selectionrectangle);
        selectionrectangle.setLocation(100, 100);
        selectionrectangle.setSize(100, 100);
        selectionrectangle.setOpaque(true);
        selectionrectangle.setVisible(false);

        Layers.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
        Layers.setVisible(true);
        Layers.setBackground(Theme.Patch_Unlocked_Background);
        Layers.setOpaque(true);
        Layers.revalidate();

        TransferHandler TH = new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return COPY_OR_MOVE;
            }

            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
                Patch p = GetSelectedObjects();
                if (p.objectInstances.isEmpty()) {
                    clip.setContents(new StringSelection(""), null);
                    return;
                }
                p.PreSerialize();
                Serializer serializer = new Persister(new Format(2));
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    serializer.write(p, baos);
                    StringSelection s = new StringSelection(baos.toString());
                    clip.setContents(s, (ClipboardOwner) null);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
                if (action == MOVE) {
                    deleteSelectedAxoObjInstances();
                }
            }

            @Override
            public boolean importData(TransferHandler.TransferSupport support) {
                return super.importData(support);
            }

            @Override
            public boolean importData(JComponent comp, Transferable t) {
                try {
                    if (!locked) {
                        if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {

                            paste((String) t.getTransferData(DataFlavor.stringFlavor), comp.getMousePosition(), false);
                        }
                    }
                } catch (UnsupportedFlavorException ex) {
                    LOGGER.log(Level.SEVERE, "Paste", ex);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Paste", ex);
                }
                return true;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                return new StringSelection("copy");
            }

            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                boolean r = super.canImport(support);
                return r;
            }

        };

        Layers.setTransferHandler(TH);

        InputMap inputMap = Layers.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                KeyUtils.CONTROL_OR_CMD_MASK), "cut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyUtils.CONTROL_OR_CMD_MASK), "copy");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                KeyUtils.CONTROL_OR_CMD_MASK), "paste");

        ActionMap map = Layers.getActionMap();
        map.put(TransferHandler.getCutAction().getValue(Action.NAME),
                TransferHandler.getCutAction());
        map.put(TransferHandler.getCopyAction().getValue(Action.NAME),
                TransferHandler.getCopyAction());
        map.put(TransferHandler.getPasteAction().getValue(Action.NAME),
                TransferHandler.getPasteAction());

        Layers.setEnabled(true);
        Layers.setFocusable(true);
        Layers.setFocusCycleRoot(true);
        Layers.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent ke) {
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                int xsteps = Constants.X_GRID;
                int ysteps = Constants.Y_GRID;

                if (ke.isShiftDown()) {
                    xsteps = 1;
                    ysteps = 1;
                }

                if (ke.isAltDown()) {
                    //TODO: Alt+arrows should scroll the patch canvas
                    /* do not ke.consume() so the menu will be triggered */
                }
                else {

                    if ((ke.getKeyCode() == KeyEvent.VK_SPACE) && !KeyUtils.isControlOrCommandDown(ke)) {
                        if (!ke.isShiftDown()) {
                            Point p = Layers.getMousePosition();
                            ke.consume();
                            if (p != null) {
                                ShowClassSelector(p, null, null, true);
                            }
                        }
                        else {
                            // shift + space ...
                        }
                    }
                    else if ((ke.getKeyCode() == KeyEvent.VK_DELETE) || (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
                        deleteSelectedAxoObjInstances();
                        ke.consume();
                    }
                    else if (ke.getKeyCode() == KeyEvent.VK_UP) {
                            MoveSelectedAxoObjInstances(Direction.UP, xsteps, ysteps);
                        ke.consume();
                    }
                    else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
                        MoveSelectedAxoObjInstances(Direction.DOWN, xsteps, ysteps);
                        ke.consume();
                    }
                    else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
                        MoveSelectedAxoObjInstances(Direction.RIGHT, xsteps, ysteps);
                        ke.consume();
                    }
                    else if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
                        MoveSelectedAxoObjInstances(Direction.LEFT, xsteps, ysteps);
                        ke.consume();
                    }
                    else if ((ke.getKeyCode() == KeyEvent.VK_C) && ke.isShiftDown() && !KeyUtils.isControlOrCommandDown(ke)) {
                        AxoObjectInstanceAbstract ao = AddObjectInstance(MainFrame.axoObjects.GetAxoObjectFromName("patch/comment", null).get(0), Layers.getMousePosition());
                        if (ao != null) {
                            ao.addInstanceNameEditor();
                        }
                        ke.consume();
                    }
                    else if ((ke.getKeyCode() == KeyEvent.VK_M) && KeyUtils.isControlOrCommandDown(ke) && ke.isShiftDown()) {
                        MainFrame.mainframe.setVisible(true);
                        ke.consume();
                    }
                    else if ((ke.getKeyCode() == KeyEvent.VK_Y) && KeyUtils.isControlOrCommandDown(ke) && ke.isShiftDown()) {
                        MainFrame.mainframe.getKeyboard().setVisible(true);
                        ke.consume();
                    }
                    else if ((ke.getKeyCode() == KeyEvent.VK_F) && KeyUtils.isControlOrCommandDown(ke) && ke.isShiftDown()) {
                        MainFrame.mainframe.getFilemanager().setVisible(true);
                        ke.consume();
                    }
                    else if (ke.isShiftDown() && ke.getKeyCode() > KeyEvent.VK_0 && ke.getKeyCode() < KeyEvent.VK_5 && !KeyUtils.isControlOrCommandDown(ke)) {

                        /* SHIFT+1...4: user shortcuts 1-4 */
                        Point p = Layers.getMousePosition();
                        String userstr = Preferences.getInstance().getUserShortcut(ke.getKeyCode() - KeyEvent.VK_1);
                        ke.consume();

                        if (p != null && userstr != null && !userstr.equals("")) {
                            ShowClassSelector(p, null, userstr, false);
                        }
                    }
                    else if (!KeyUtils.isControlOrCommandDown(ke) && ke.getKeyCode() >= KeyEvent.VK_A && ke.getKeyCode() <= KeyEvent.VK_Z) {

                        /* Trigger stock shortcut list */
                        int i = ke.getKeyCode() - KeyEvent.VK_A;
                        Point p = Layers.getMousePosition();
                        if (ke.isShiftDown()) {
                            i += capitalLetterOffset;
                        }
                        ke.consume();

                        if (p != null && ObjectSearchFrame.shortcutList[i] != null) {
                            ShowClassSelector(p, null, ObjectSearchFrame.shortcutList[i], false);
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {
            }
        });

        Layers.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getButton() == MouseEvent.BUTTON1) {
                    if (!me.isShiftDown() && !KeyUtils.isControlOrCommandDown(me)) {
                        if (me.getClickCount() == 2) {
                            ShowClassSelector(me.getPoint(), null, null, true);
                        }
                        else {
                            if ((osf != null) && osf.isVisible()) {
                                osf.Accept();
                            }
                        }
                        
                        Layers.requestFocusInWindow();
                    }
                    me.consume();
                }
                else {
                    if ((osf != null) && osf.isVisible()) {
                        osf.Cancel();
                    }
                    Layers.requestFocusInWindow();
                    me.consume();
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {
                MousePressedBtn = me.getButton();
                if (MousePressedBtn == MouseEvent.BUTTON1) {
                    if (!me.isShiftDown()) {
                        for (AxoObjectInstanceAbstract o : objectInstances) {
                            o.SetSelected(false);
                        }
                    }
                    selectionRectStart = me.getPoint();
                    selectionrectangle.setBounds(me.getX(), me.getY(), 1, 1);
                    //selectionrectangle.setVisible(true);

                    Layers.requestFocusInWindow();
                    me.consume();
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                    selectionrectangle.setVisible(false);
                    me.consume();
            }

            @Override
            public void mouseEntered(MouseEvent me) {
            }

            @Override
            public void mouseExited(MouseEvent me) {
            }
        });

        Layers.setVisible(true);

        DropTarget dt;
        dt = new DropTarget() {

            @Override
            public synchronized void dragOver(DropTargetDragEvent dtde) {
            }

            @Override
            public synchronized void drop(DropTargetDropEvent dtde) {
                Transferable t = dtde.getTransferable();
                if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        List<File> flist = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        for (File f : flist) {
                            if (f.exists() && f.canRead()) {
                                AxoObjectAbstract o = new AxoObjectFromPatch(f);
                                String fn = f.getCanonicalPath();
                                if (GetCurrentWorkingDirectory() != null && fn.startsWith(GetCurrentWorkingDirectory())) {
                                    o.createdFromRelativePath = true;
                                }
                                AddObjectInstance(o, dtde.getLocation());
                            }
                        }
                        dtde.dropComplete(true);
                    } catch (UnsupportedFlavorException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                    return;
                }
                super.drop(dtde);
            }
        ;
        };

        Layers.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                if (MousePressedBtn == MouseEvent.BUTTON1) {
                    int x1 = selectionRectStart.x;
                    int y1 = selectionRectStart.y;
                    int x2 = ev.getX();
                    int y2 = ev.getY();
                    int xmin = x1 < x2 ? x1 : x2;
                    int xmax = x1 > x2 ? x1 : x2;
                    int ymin = y1 < y2 ? y1 : y2;
                    int ymax = y1 > y2 ? y1 : y2;
                    int width = xmax - xmin;
                    int height = ymax - ymin;
                    selectionrectangle.setBounds(xmin, ymin, width, height);
                    selectionrectangle.setVisible(true);

                    Rectangle r = selectionrectangle.getBounds();

                    for (AxoObjectInstanceAbstract o : objectInstances) {
                        if (!o.IsLocked()) {
                            if (ev.isShiftDown()) {
                                /* Add unlocked objects within rectangle to current selection  */
                                if (o.getBounds().intersects(r) && !o.isSelected()) {
                                    o.SetSelected(true);
                                }
                            }
                            else {
                                /* Clear selection then add unlocked objects within rectangle to selection */
                                o.SetSelected(o.getBounds().intersects(r));
                            }
                        }
                    }
                }
                ev.consume();
            }
        });

        Layers.setDropTarget(dt);
        Layers.setVisible(true);
    }

    void paste(String v, Point pos, boolean applyWiresFromExternalOutlets) {
        SelectNone();
        if (v.isEmpty()) {
            LOGGER.log(Level.INFO, "Clipboard is empty, nothing to paste.");
            return;
        }

        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, new Format(2));

        try {
            PatchGUI p = serializer.read(PatchGUI.class, v);
            HashMap<String, String> dict = new HashMap<String, String>();
            ArrayList<AxoObjectInstanceAbstract> obj2 = (ArrayList<AxoObjectInstanceAbstract>) p.objectInstances.clone();

            // System.out.println(Instant.now() + " Starting first pass: Pre-processing objects.");
            for (AxoObjectInstanceAbstract o : obj2) {
                o.patch = this;
                AxoObjectAbstract obj = o.resolveType();
                if (obj != null) {
                    // System.out.println(Instant.now() + " Found object type for: " + o.getInstanceName());
                    Modulator[] m = obj.getModulators();
                    if (m != null) {
                        if (Modulators == null) {
                            Modulators = new ArrayList<Modulator>();
                        }
                        for (Modulator mm : m) {
                            mm.setObjInst(o);
                            Modulators.add(mm);
                        }
                    }
                }
                else {
                    LOGGER.log(Level.WARNING, "Object type not found for: " + o.getInstanceName() + ". Creating zombie object.");
                    p.objectInstances.remove(o);
                    AxoObjectInstanceZombie zombie = new AxoObjectInstanceZombie(new AxoObjectZombie(), this, o.getInstanceName(), new Point(o.getX(), o.getY()));
                    zombie.patch = this;
                    zombie.typeName = o.typeName;
                    zombie.PostConstructor();
                    p.objectInstances.add(zombie);
                }
            }

            /* Find offset */
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            for (AxoObjectInstanceAbstract o : p.objectInstances) {
                if (o.getX() < minX) {
                    minX = o.getX();
                }
                if (o.getY() < minY) {
                    minY = o.getY();
                }
            }

            int xOffset = 0, yOffset = 0;
            if (pos != null) {
                xOffset = ((pos.x - minX + (Constants.X_GRID / 2)) / Constants.X_GRID) * Constants.X_GRID;
                yOffset = ((pos.y - minY + (Constants.Y_GRID / 2)) / Constants.Y_GRID) * Constants.Y_GRID;
            }

            /* Check for object collisions */
            int xCollisionShift = 0;
            int yCollisionShift = 0;
            boolean hasCollision;
            do {
                hasCollision = false;
                for (AxoObjectInstanceAbstract o : p.objectInstances) {
                    int newposx = o.getX() + xOffset + xCollisionShift;
                    int newposy = o.getY() + yOffset + yCollisionShift;

                    if (getObjectAtLocation(newposx, newposy) != null) {
                        hasCollision = true;
                        xCollisionShift += Constants.X_GRID;
                        yCollisionShift += Constants.Y_GRID;
                        break;
                    }
                }
            } while (hasCollision);
            
            // System.out.println(Instant.now() + " Starting second pass: Naming and positioning objects.");
            for (AxoObjectInstanceAbstract o : p.objectInstances) {
                String original_name = o.getInstanceName();
                if (original_name != null) {
                    String new_name = original_name;
                    String ss[] = new_name.split("_");
                    boolean hasNumeralSuffix = false;
                    try {
                        if ((ss.length > 1) && (Integer.toString(Integer.parseInt(ss[ss.length - 1]))).equals(ss[ss.length - 1])) {
                            hasNumeralSuffix = true;
                        }
                    }
                    catch (NumberFormatException e) {
                        /* Ignore if the suffix is not a number */
                    }
                    
                    if (hasNumeralSuffix) {
                        int n = Integer.parseInt(ss[ss.length - 1]) + 1;
                        String bs = original_name.substring(0, original_name.length() - ss[ss.length - 1].length());
                        while (GetObjectInstance(new_name) != null) {
                            new_name = bs + n++;
                        }
                        while (dict.containsKey(new_name)) {
                            new_name = bs + n++;
                        }
                    }
                    else {
                        while (GetObjectInstance(new_name) != null) {
                            new_name = new_name + "_";
                        }
                        while (dict.containsKey(new_name)) {
                            new_name = new_name + "_";
                        }
                    }

                    if (!new_name.equals(original_name)) {
                        o.setInstanceName(new_name);
                        // System.out.println(Instant.now() + " Renamed object from: " + original_name + " to: " + new_name);
                    }
                    dict.put(original_name, new_name);
                    // System.out.println(Instant.now() + " Dictionary mapping: " + original_name + " -> " + new_name);
                }
                
                objectInstances.add(o);
                objectLayerPanel.add(o, 0);
                o.PostConstructor();
                
                int newposx = o.getX() + xOffset + xCollisionShift;
                int newposy = o.getY() + yOffset + yCollisionShift;

                o.setLocation(newposx, newposy);
                o.SetSelected(true);
                // System.out.println(Instant.now() + " Pasted object: " + o.getInstanceName() + " at new location: (" + newposx + ", " + newposy + ")");
            }

            objectLayerPanel.validate();

            // System.out.println(Instant.now() + " Starting third pass: Recreating wires (nets).");
            Map<OutletInstance, List<InletInstance>> externalConnectionsToMake = null;
            if (applyWiresFromExternalOutlets) {
                externalConnectionsToMake = new HashMap<>();
            }

            for (Net n : p.nets) {
                /* Assign patch before any other operations on the net */
                n.patch = this;

                OutletInstance connectedOutlet = null;

                /* Handles the source (outlets) of the net */
                if (n.source != null && n.source.size() > 0) {
                    ArrayList<OutletInstance> source2 = new ArrayList<OutletInstance>();
                    for (OutletInstance o : n.source) {
                        String objname = o.getObjname();
                        String outletname = o.getOutletname();
                        if ((objname != null) && (outletname != null)) {
                            String on2 = dict.get(objname);
                            if (on2 != null) {
                                /* It's an internal connection. Get the new, valid instance */
                                AxoObjectInstanceAbstract newObj = GetObjectInstance(on2);
                                if (newObj != null) {
                                    OutletInstance newOutletInstance = newObj.GetOutletInstance(outletname);
                                    if (newOutletInstance != null) {
                                        source2.add(newOutletInstance);
                                        // System.out.println(Instant.now() + " Found internal outlet and retrieved new instance: " + objname + ":" + outletname + " -> " + on2 + ":" + outletname);
                                    }
                                    // else {
                                    //     System.out.println(Instant.now() + " Could not find outlet instance for new object: " + on2 + ":" + outletname);
                                    // }
                                }
                            }
                            else if (applyWiresFromExternalOutlets) {
                                /* It's an external connection */
                                AxoObjectInstanceAbstract obj = GetObjectInstance(objname);
                                if ((obj != null) && (connectedOutlet == null)) {
                                    OutletInstance oi = obj.GetOutletInstance(outletname);
                                    if (oi != null) {
                                        connectedOutlet = oi;
                                        // System.out.println(Instant.now() + " Found external outlet: " + objname + ":" + outletname);
                                    }
                                }
                            }
                        }
                    }
                    n.source = source2;
                }

                /* Handles the destination (inlets) of the net. */
                ArrayList<InletInstance> dest2 = new ArrayList<InletInstance>();
                if (n.dest != null && n.dest.size() > 0) {
                    // System.out.println(Instant.now() + " Net: " + n.GetCName() + " has " + n.dest.size() + " destinations in clipboard.");
                    for (InletInstance o : n.dest) {
                        String objname = o.getObjname();
                        String inletname = o.getInletname();
                        if ((objname != null) && (inletname != null)) {
                            String on2 = dict.get(objname);
                            if (on2 != null) {
                                /* It's an internal connection. Get the new, valid instance */
                                AxoObjectInstanceAbstract newObj = GetObjectInstance(on2);
                                if (newObj != null) {
                                    InletInstance newInletInstance = newObj.GetInletInstance(inletname);
                                    if (newInletInstance != null) {
                                        dest2.add(newInletInstance);
                                        // System.out.println(Instant.now() + " Found internal inlet and retrieved new instance: " + objname + ":" + inletname + " -> " + on2 + ":" + inletname);
                                    }
                                    // else {
                                    //     System.out.println(Instant.now() + " Could not find inlet instance for new object: " + on2 + ":" + inletname);
                                    // }
                                }
                            }
                        }
                    }
                }
                n.dest = dest2;

                /* After processing sources and destinations, decide how to create the new net */
                if (n.source.size() > 0 && n.dest.size() > 0) {
                    /* It's a new internal net to be created */
                    n.PostConstructor();
                    nets.add(n);
                    netLayerPanel.add(n);
                    // System.out.println(Instant.now() + " Creating internal net: " + n.GetCName());
                }
                else if (connectedOutlet != null && n.dest.size() > 0 && applyWiresFromExternalOutlets) {
                    /* Net has an external source and needs to be connected to the new inlets */
                    if (!externalConnectionsToMake.containsKey(connectedOutlet)) {
                        externalConnectionsToMake.put(connectedOutlet, new ArrayList<>());
                    }
                    externalConnectionsToMake.get(connectedOutlet).addAll(n.dest);
                    // System.out.println(Instant.now() + " Queuing external connection from: " + connectedOutlet.GetCName() + " to " + n.dest.size() + " new inlets.");
                }
                else if (n.source.size() == 0 && n.dest.size() > 1) {
                    /* Floating net: the net connects two or more inlets in the group but the connection to the external outlet is severed. */
                    n.PostConstructor();
                    nets.add(n);
                    netLayerPanel.add(n);
                    // System.out.println(Instant.now() + " Pasting a 'floating' net with two or more destinations but no source.");
                }
            }
            
            // Fourth Pass: Create the new external connections
            if (applyWiresFromExternalOutlets && externalConnectionsToMake != null) {
                for (Map.Entry<OutletInstance, List<InletInstance>> entry : externalConnectionsToMake.entrySet()) {
                    OutletInstance sourceOutlet = entry.getKey();
                    List<InletInstance> destinationInlets = entry.getValue();

                    for (InletInstance destinationInlet : destinationInlets) {
                        AddConnection(destinationInlet, sourceOutlet);
                    }
                }
            }
            AdjustSize();
            SetDirty();
        }
        catch (XMLStreamException ex) {
            LOGGER.log(Level.INFO, "Paste: Clipboard does not contain valid content.");
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during the paste operation.", ex);
        }
    }

    AxoObjectInstanceAbstract getObjectAtLocation(int x, int y) {
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if ((o.getX() == x) && (o.getY() == y)) {
                return o;
            }
        }
        return null;
    }
    public ObjectSearchFrame osf;

    public void ShowClassSelector(Point p, AxoObjectInstanceAbstract o, String searchString, boolean selectText) {
        if (IsLocked()) {
            return;
        }
        if (osf == null) {
            osf = new ObjectSearchFrame(this);
        }
        osf.Launch(p, o, searchString, selectText);
    }

    void SelectAll() {
        for (AxoObjectInstanceAbstract o : objectInstances) {
            o.SetSelected(true);
        }
    }

    public void SelectNone() {
        for (AxoObjectInstanceAbstract o : objectInstances) {
            o.SetSelected(false);
        }
    }
    TextEditor NotesTextEditor;

    void ShowNotesTextEditor() {
        if (NotesTextEditor == null) {
            NotesTextEditor = new TextEditor(new StringRef(), getPatchframe());
            NotesTextEditor.setLocationRelativeTo(patchframe);
            NotesTextEditor.disableSyntaxHighlighting();
            NotesTextEditor.setTitle("Patch Notes");
            NotesTextEditor.SetText(notes);
            NotesTextEditor.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                }

                @Override
                public void focusLost(FocusEvent e) {
                    notes = NotesTextEditor.GetText();
                }
            });
        }
        NotesTextEditor.setVisible(true);
        NotesTextEditor.toFront();
    }

    enum Direction {

        UP, LEFT, DOWN, RIGHT
    }

    Patch GetSelectedObjects() {
        Patch p = new Patch();
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if (o.isSelected()) {
                p.objectInstances.add(o);
            }
        }
        p.nets = new ArrayList<Net>();
        for (Net n : nets) {
            int sel = 0;
            for (InletInstance i : n.dest) {
                if (i.GetObjectInstance().isSelected()) {
                    sel++;
                }
            }
            for (OutletInstance i : n.source) {
                if (i.GetObjectInstance().isSelected()) {
                    sel++;
                }
            }
            if (sel > 0) {
                p.nets.add(n);
            }
        }
        p.PreSerialize();
        return p;
    }

    void MoveSelectedAxoObjInstances(Direction dir, int xsteps, int ysteps) {
        if (!locked) {
            int xgrid = 1;
            int ygrid = 1;
            int xstep = 0;
            int ystep = 0;
            switch (dir) {
                case DOWN:
                    ystep = ysteps;
                    ygrid = ysteps;
                    break;
                case UP:
                    ystep = -ysteps;
                    ygrid = ysteps;
                    break;
                case LEFT:
                    xstep = -xsteps;
                    xgrid = xsteps;
                    break;
                case RIGHT:
                    xstep = xsteps;
                    xgrid = xsteps;
                    break;
            }
            boolean isUpdate = false;
            Rectangle boundsToRepaint = null;
            for (AxoObjectInstanceAbstract o : objectInstances) {
                if (o.isSelected()) {
                    isUpdate = true;
                    Point p = o.getLocation();
                    p.x = p.x + xstep;
                    p.y = p.y + ystep;
                    p.x = xgrid * (p.x / xgrid);
                    p.y = ygrid * (p.y / ygrid);
                    o.SetLocation(p.x, p.y);
                    // o.repaint();
                    if (boundsToRepaint == null) {
                        boundsToRepaint = o.getBounds();
                    } else {
                        boundsToRepaint.add(o.getBounds());
                    }
                }
            }
            if (isUpdate) {
                AdjustSize();
                SetDirty();
                if (boundsToRepaint != null) {
                    objectLayerPanel.repaint(boundsToRepaint); /* Repaint only the combined area */
                }
            }
        } else {
            LOGGER.log(Level.INFO, "Cannot move: locked");
        }
    }

    @Override
    public void PostContructor() {
        super.PostContructor();
        objectLayerPanel.removeAll();
        netLayerPanel.removeAll();

        /* Duplicate objectInstances then reverse it - achieving "top to bottom" layering so partly overlapped objects have visible titlebars */
        ArrayList<AxoObjectInstanceAbstract> obj2 = (ArrayList<AxoObjectInstanceAbstract>) objectInstances.clone();
        Collections.reverse(obj2);

        for (AxoObjectInstanceAbstract o : obj2) {
            objectLayerPanel.add(o);
        }
        for (Net n : nets) {
            netLayerPanel.add(n);
        }
        objectLayerPanel.validate();
        netLayerPanel.validate();

        Layers.setPreferredSize(new Dimension(5000, 5000));
        AdjustSize();
        Layers.validate();

        for (Net n : nets) {
            n.updateBounds();
        }
    }

    @Override
    public void setFileNamePath(String FileNamePath) {

        super.setFileNamePath(FileNamePath);

        /* Get last occurrence of slash or backslash (separating path and filename) */
        int brk = FileNamePath.lastIndexOf(File.separator) + 1;
        if (brk != 0) {
            /* Display filename first, then path in brackets */
            String str = FileNamePath.substring(brk) + "  [" + FileNamePath.substring(0, brk-1) + "]";
            patchframe.setTitle(str);
        }
        else if (patchframe.getPatchGUI().getSettings() != null && patchframe.getPatchGUI().getSettings().subpatchmode != SubPatchMode.no) {
            /* Subpatch */
            patchframe.setTitle(FileNamePath + "  (subpatch)");
            patchframe.setSaveMenuEnabled(false); /* parent has to be saved to preserve changes */
        }
        else {
            /* New patch */
            patchframe.setTitle(FileNamePath);
        }
    }

    @Override
    public Net AddConnection(InletInstance il, OutletInstance ol) {
        Net n = super.AddConnection(il, ol);
        if (n != null) {
            netLayerPanel.add(n);
            n.updateBounds();
        }
        return n;
    }

    @Override
    public Net AddConnection(InletInstance il, InletInstance ol) {
        Net n = super.AddConnection(il, ol);
        if (n != null) {
            netLayerPanel.add(n);
            n.updateBounds();
        }
        return n;
    }

    @Override
    public Net disconnect(IoletAbstract io) {
        Net n = super.disconnect(io);
        if (n != null) {
            n.updateBounds();
            n.repaint();
        }
        return n;
    }

    @Override
    public Net delete(Net n) {
        if (n != null) {
            netLayerPanel.remove(n);
            netLayer.repaint(n.getBounds());
        }
        Net nn = super.delete(n);
        return nn;
    }

    @Override
    public void delete(AxoObjectInstanceAbstract o) {
        super.delete(o);
        objectLayerPanel.remove(o);
        objectLayerPanel.repaint(o.getBounds());
        objectLayerPanel.validate();
        AdjustSize();
    }

    @Override
    public AxoObjectInstanceAbstract AddObjectInstance(AxoObjectAbstract obj, Point loc) {
        AxoObjectInstanceAbstract oi = super.AddObjectInstance(obj, loc);
        if (oi != null) {
            SelectNone();
            objectLayerPanel.add(oi);
            oi.SetSelected(true);
            oi.moveToFront();
            oi.revalidate();
            AdjustSize();
        }
        return oi;
    }

    public void SetCordsInBackground(boolean b) {
        if (b) {
            Layers.removeAll();
            Layers.add(netLayer, Integer.valueOf(1));
            Layers.add(objectLayer, Integer.valueOf(2));
            Layers.add(draggedObjectLayer, Integer.valueOf(3));
            Layers.add(selectionRectLayer, Integer.valueOf(4));
        } else {
            Layers.removeAll();
            Layers.add(objectLayer, Integer.valueOf(1));
            Layers.add(netLayer, Integer.valueOf(2));
            Layers.add(draggedObjectLayer, Integer.valueOf(3));
            Layers.add(selectionRectLayer, Integer.valueOf(4));
        }
    }

    @Override
    public void Lock() {
        super.Lock();
        Layers.setBackground(Theme.Patch_Locked_Background);
    }

    @Override
    public void Unlock() {
        super.Unlock();
        Layers.setBackground(Theme.Patch_Unlocked_Background);
    }

    @Override
    public void repaint() {
        if (Layers != null) {
            Layers.repaint();
        }
    }

    @Override
    void UpdateDSPLoad(int val200, boolean overload) {
        dspLoadPercent = val200 / 2.0f;
        patchframe.ShowDSPLoad(val200, overload);
    }

    float getDSPLoadPercent() {
        return dspLoadPercent;
    }

    Dimension GetInitialSize() {
        int mx = 480; // min size
        int my = 320;
        for (AxoObjectInstanceAbstract i : objectInstances) {

            Dimension s = i.getPreferredSize();

            int ox = i.getX() + (int) s.getWidth();
            int oy = i.getY() + (int) s.getHeight();

            if (ox > mx) {
                mx = ox;
            }
            if (oy > my) {
                my = oy;
            }
        }
        // adding more, as getPreferredSize is not returning true dimension of
        // object
        return new Dimension(mx + 300, my + 300);
    }

    public void clampLayerSize(Dimension s) {
        if (s.width < Layers.getParent().getWidth()) {
            s.width = Layers.getParent().getWidth();
        }
        if (s.height < Layers.getParent().getHeight()) {
            s.height = Layers.getParent().getHeight();
        }
    }

    @Override
    public void AdjustSize() {
        Dimension s = GetSize();
        clampLayerSize(s);
        if (!Layers.getSize().equals(s)) {
            Layers.setSize(s);
        }
        if (!Layers.getPreferredSize().equals(s)) {
            Layers.setPreferredSize(s);
        }
    }

    @Override
    void PreSerialize() {
        super.PreSerialize();
        if (NotesTextEditor != null) {
            this.notes = NotesTextEditor.GetText();
        }
        windowPos = patchframe.getBounds();
    }

    @Override
    boolean save(File f) {
        boolean b = super.save(f);
        if (ObjEditor != null) {
            ObjEditor.UpdateObject();
        }
        return b;
    }

    public static void OpenPatch(String name, InputStream stream) {
        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, new Format(2));
        try {
            PatchGUI patch1 = serializer.read(PatchGUI.class, stream);
            PatchFrame pf = new PatchFrame(patch1);
            patch1.setFileNamePath(name);
            patch1.PostContructor();
            patch1.setFileNamePath(name);
            pf.setVisible(true);
            pf.repositionIfOutsideScreen();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static PatchFrame OpenPatchInvisible(File f) {
        for (DocumentWindow dw : DocumentWindowList.GetList()) {
            if (f.equals(dw.getFile())) {
                JFrame frame1 = dw.GetFrame();
                if (frame1 instanceof PatchFrame) {
                    return (PatchFrame) frame1;
                } else {
                    return null;
                }
            }
        }

        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, new Format(2));

        try {
            PatchGUI patch1 = serializer.read(PatchGUI.class, f);
            PatchFrame pf = new PatchFrame(patch1);
            patch1.setFileNamePath(f.getAbsolutePath());
            patch1.PostContructor();
            patch1.setFileNamePath(f.getPath());
            return pf;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            if (ite.getTargetException() instanceof Patch.PatchVersionException) {
                Patch.PatchVersionException pve = (Patch.PatchVersionException) ite.getTargetException();
                LOGGER.log(Level.SEVERE, "Patch \"{0}\" was saved with a newer version of Ksoloti: {1}",
                        new Object[]{f.getAbsoluteFile(), pve.getMessage()});
            } else {
                LOGGER.log(Level.SEVERE, null, ite);
            }
            return null;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static PatchFrame OpenPatch(File f) {
        PatchFrame pf = OpenPatchInvisible(f);
        pf.setVisible(true);
        pf.setState(java.awt.Frame.NORMAL);
        pf.repositionIfOutsideScreen();
        pf.toFront();
        Preferences.getInstance().addRecentFile(f.getAbsolutePath());
        return pf;
    }

    private Map<DataType, Boolean> cableTypeEnabled = new HashMap<DataType, Boolean>();

    public void setCableTypeEnabled(DataType type, boolean enabled) {
        cableTypeEnabled.put(type, enabled);
    }

    public Boolean isCableTypeEnabled(DataType type) {
        if (cableTypeEnabled.containsKey(type)) {
            return cableTypeEnabled.get(type);
        } else {
            return true;
        }
    }

    public void updateNetVisibility() {
        for (Net n : this.nets) {
            DataType d = n.GetDataType();
            if (d != null) {
                n.setVisible(isCableTypeEnabled(d));
            }
        }
        Layers.repaint();
    }

    @Override
    public void Close() {
        super.Close();
        if (NotesTextEditor != null) {
            NotesTextEditor.dispose();
        }
        if ((settings != null) && (settings.editor != null)) {
            settings.editor.dispose();
        }
    }
}
