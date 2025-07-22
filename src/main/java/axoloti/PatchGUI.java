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
import axoloti.net.DragWireOverlay;
import axoloti.net.Net;
import axoloti.net.NetDragging;
import axoloti.net.NetDrawingPanel;
import axoloti.object.AxoObjectAbstract;
import axoloti.object.AxoObjectFromPatch;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.object.AxoObjectInstanceZombie;
import axoloti.object.AxoObjectZombie;
import axoloti.outlets.OutletInstance;
import axoloti.utils.Constants;
import axoloti.utils.KeyUtils;

import static axoloti.MainFrame.prefs;

import java.awt.Dimension;
import java.awt.Graphics;
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
import java.awt.event.MouseMotionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.stream.Format;

import qcmds.QCmdProcessor;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "patch-1.0")
public class PatchGUI extends Patch {

    private static final Logger LOGGER = Logger.getLogger(PatchGUI.class.getName());

    /* KeyCode integers */
    static final int keyCodeList[] = {
        KeyEvent.VK_A,
        KeyEvent.VK_B,
        KeyEvent.VK_C,
        KeyEvent.VK_D,
        KeyEvent.VK_E,
        KeyEvent.VK_F,
        KeyEvent.VK_G,
        KeyEvent.VK_H,
        KeyEvent.VK_I,
        KeyEvent.VK_J,
        KeyEvent.VK_K,
        KeyEvent.VK_L,
        KeyEvent.VK_M,
        KeyEvent.VK_N,
        KeyEvent.VK_O,
        KeyEvent.VK_P,
        KeyEvent.VK_Q,
        KeyEvent.VK_R,
        KeyEvent.VK_S,
        KeyEvent.VK_T,
        KeyEvent.VK_U,
        KeyEvent.VK_V,
        KeyEvent.VK_W,
        KeyEvent.VK_X,
        KeyEvent.VK_Y,
        KeyEvent.VK_Z
    };

    /* Shortcut strings */
    static final String shortcutList[] = {

        /* Lower case shortcuts should ideally point to low-level and basic objects */
        /* a */ null,
        /* b */ null,
        /* c */ "ctrl/",
        /* d */ "disp/",
        /* e */ "patch/object",
        /* f */ null,
        /* g */ "gain/",
        /* h */ "harmony/",
        /* i */ "audio/in",
        /* j */ null,
        /* k */ null,
        /* l */ "logic/",
        /* m */ "math/",
        /* n */ "noise/",
        /* o */ "audio/out",
        /* p */ "patch/",
        /* q */ null,
        /* r */ "(read|write)",
        /* s */ "string/",
        /* t */ "table/",
        /* u */ "util/",
        /* v */ null,
        /* w */ "wave/",
        /* x */ "mix/",
        /* y */ null,
        /* z */ null,

        /* Upper case shortcuts should ideally point to more high-level and abstracted objects */
        /* A */ null,
        /* B */ "big genes/",
        /* C */ "patch/comment", /* special case - will never get here */
        /* D */ "delay/",
        /* E */ "env/",
        /* F */ "filter/",
        /* G */ "gills/",
        /* H */ null,
        /* I */ "patch/inlet",
        /* J */ null,
        /* K */ "ksoloti/",
        /* L */ "lfo/",
        /* M */ "midi/",
        /* N */ null,
        /* O */ "patch/outlet",
        /* P */ "patch/patcher",
        /* Q */ "(seq|sequencer)",
        /* R */ "reverb",
        /* S */ "script",
        /* T */ null,
        /* U */ null,
        /* V */ null,
        /* W */ "osc/",
        /* X */ "fx/",
        /* Y */ null,
        /* Z */ null,
    };

    private final static int capitalLetterOffset = 26;

    private int MousePressedBtn = 0;
    private float dspLoadPercent = 0.0f;

    public NetDrawingPanel netLayerPanel; // Keep this
    public JPanel objectLayerPanel = new JPanel(); // Keep this
    public JLayeredPane Layers = new JLayeredPane(); // Keep this

    public DragWireOverlay dragWireOverlay; // This is the ONLY DragWireOverlay instance
    public JPanel draggedObjectLayerPanel = new JPanel();
    public JPanel selectionRectLayerPanel = new JPanel();

    JLayer<JComponent> objectLayer;
    JLayer<JComponent> draggedObjectLayer = new JLayer<JComponent>(draggedObjectLayerPanel);
    JLayer<JComponent> netLayer;
    JLayer<JComponent> selectionRectLayer;

    public NetDragging netDragging;
    private boolean isDraggingNet = false;

    SelectionRectangle selectionrectangle = new SelectionRectangle();
    Point selectionRectStart;
    Point panOrigin;
    public AxoObjectFromPatch ObjEditor;
    TextEditor NotesTextEditor;
    public ObjectSearchFrame osf;
    private Map<DataType, Boolean> cableTypeEnabled = new HashMap<DataType, Boolean>();

    public PatchGUI() {
        super();

        Layers.setLayout(null);
        Layers.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
        Layers.setLocation(0, 0);
        Layers.setFont(Constants.FONT);
        Layers.setBackground(Theme.Patch_Unlocked_Background);
        Layers.setVisible(true);
        Layers.setOpaque(true);

        netLayerPanel = new NetDrawingPanel(this); // Pass 'this' to NetDrawingPanel's constructor
        netLayer = new JLayer<>(netLayerPanel);
        objectLayer = new JLayer<>(objectLayerPanel);
        selectionRectLayer = new JLayer<>(selectionRectLayerPanel);

        dragWireOverlay = new DragWireOverlay(); // ONLY create DragWireOverlay here
        netDragging = new NetDragging(dragWireOverlay); // Pass THIS instance to NetDragging

        JComponent[] layerComponents = {
            objectLayer, objectLayerPanel, draggedObjectLayerPanel, netLayerPanel,
            selectionRectLayerPanel, draggedObjectLayer, netLayer, selectionRectLayer, dragWireOverlay};
        for (JComponent c : layerComponents) {
            c.setLayout(null);
            c.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
            c.setLocation(0, 0);
            c.setOpaque(false);
            c.setFont(Constants.FONT);
            c.validate();
        }

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

        // Add components to Layers with appropriate Z-order
        Layers.add(objectLayer, Integer.valueOf(1));        // Objects and their Iolets (on top of nets)
        Layers.add(netLayer, Integer.valueOf(2));           // Permanent nets (as background)
        Layers.add(draggedObjectLayer, Integer.valueOf(3)); // Dragged objects
        Layers.add(dragWireOverlay, Integer.valueOf(4));    // Dragged wire (should be above objects/nets)
        Layers.add(selectionRectLayer, Integer.valueOf(5)); // Selection rectangle (highest)
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
                        String userstr = prefs.getUserShortcut(ke.getKeyCode() - KeyEvent.VK_1);
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

                        if (p != null && shortcutList[i] != null) {
                            ShowClassSelector(p, null, shortcutList[i], false);
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
                    // --- 1. Check for Net Dragging Start ---
                    // You'll need methods to find if an OutletInstance or InletInstance
                    // exists at the mouse click point. Implement these helper methods
                    // (e.g., findOutletAt(Point p), findInletAt(Point p)).
                    OutletInstance clickedOutlet = findOutletAt(me.getPoint());
                    InletInstance clickedInlet = findInletAt(me.getPoint());

                    if (clickedOutlet != null) {
                        netDragging.StartDraggingFromOutlet(clickedOutlet);
                        isDraggingNet = true;
                        me.consume(); // Consume to prevent further processing by other handlers
                    } else if (clickedInlet != null) {
                        // This case is for re-routing an existing net or starting a new one from an inlet
                        netDragging.StartDraggingFromInlet(clickedInlet);
                        isDraggingNet = true;
                        me.consume(); // Consume to prevent further processing
                    }
                    // --- 2. If not a Net Drag, proceed with existing logic ---
                    else {
                        if (!me.isShiftDown()) {
                            // Deselect all objects only if not starting a shift-selection
                            for (AxoObjectInstanceAbstract o : objectInstances) {
                                o.SetSelected(false);
                            }
                        }
                        selectionRectStart = me.getPoint();
                        selectionrectangle.setBounds(me.getX(), me.getY(), 1, 1);
                        // selectionrectangle.setVisible(true); // You might set this to true in mouseDragged
                                                            // if the mouse moves beyond a certain threshold.

                        Layers.requestFocusInWindow();
                        me.consume();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                if (MousePressedBtn == MouseEvent.BUTTON1) {
                    if (isDraggingNet) {
                        // --- Handle Net Dragging End ---
                        netDragging.EndDragging();
                        isDraggingNet = false;

                        // After net drag, attempt to make a connection
                        // You'll need logic here to:
                        // 1. Get the dragging source/destination from netDragging (e.g., netDragging.getDraggingSourceOutlet())
                        // 2. Find an InletInstance/OutletInstance at the current mouse position (me.getPoint())
                        // 3. If a valid connection can be made, create a new Net object.
                        //    patch.addNet(new Net(source, dest)); // Example
                        // 4. Then, always call repaintPatch() to update the display.
                        repaintPatch(); // Defined in PatchGUI

                    } else {
                        // --- Handle Selection Rectangle End ---
                        selectionrectangle.setVisible(false);
                        // Implement logic here to select objects within the final selectionrectangle bounds
                        // If selectionRectStart != null and the rectangle has a meaningful size
                        // For each AxoObjectInstance o:
                        //   if (selectionrectangle.getBounds().intersects(o.getBounds())) {
                        //      o.SetSelected(true);
                        //   }
                        // Then repaint objects/patch to show selection
                        repaintPatch(); // Defined in PatchGUI
                    }
                }
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

        Layers.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                if (MousePressedBtn == MouseEvent.BUTTON1) { // Only for left-click drags
                    if (isDraggingNet) {
                        // --- Handle Net Dragging Update (if a net drag is active) ---
                        netDragging.UpdateDragPoint(ev.getPoint());
                        ev.consume(); // Consume to prevent further processing
                    } else if (selectionRectStart != null) {
                        // --- Handle Selection Rectangle / Object Dragging Update ---
                        // (This is your provided logic, integrated here)

                        int x1 = selectionRectStart.x;
                        int y1 = selectionRectStart.y;
                        int x2 = ev.getX();
                        int y2 = ev.getY();

                        int xmin = Math.min(x1, x2);
                        int xmax = Math.max(x1, x2);
                        int ymin = Math.min(y1, y2);
                        int ymax = Math.max(y1, y2);

                        int width = xmax - xmin;
                        int height = ymax - ymin;

                        selectionrectangle.setBounds(xmin, ymin, width, height);
                        selectionrectangle.setVisible(true); // Ensure it's visible while dragging

                        Rectangle r = selectionrectangle.getBounds();

                        for (AxoObjectInstanceAbstract o : objectInstances) {
                            if (!o.IsLocked()) {
                                if (ev.isShiftDown()) {
                                    /* Add unlocked objects within rectangle to current selection  */
                                    if (o.getBounds().intersects(r) && !o.isSelected()) {
                                        o.SetSelected(true);
                                    }
                                } else {
                                    /* Clear selection then add unlocked objects within rectangle to selection */
                                    o.SetSelected(o.getBounds().intersects(r));
                                }
                            }
                        }
                        // After updating selection, request a repaint of the patch.
                        // This ensures the selection rectangle and updated object selections are drawn.
                        repaintPatch();
                        ev.consume(); // Consume to prevent further processing
                    }
                    // Else, if neither net dragging nor selection dragging is active,
                    // it might be an object drag (moving existing selected objects).
                    // That logic would go here if you have it elsewhere.
                }
                // mouseMoved is still empty, handles non-dragged movement.
            }

            @Override
            public void mouseMoved(MouseEvent me) {
                // Keep this empty or add logic for hover effects if needed
            }
        });

        Layers.setDropTarget(dt);
        Layers.setVisible(true);
    }

    void paste(String v, Point pos, boolean restoreConnectionsToExternalOutlets) {
        SelectNone();
        if (v.isEmpty()) {
            return;
        }
        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, new Format(2));
        try {
            PatchGUI p = serializer.read(PatchGUI.class, v);
            HashMap<String, String> dict = new HashMap<String, String>();
            ArrayList<AxoObjectInstanceAbstract> obj2 = (ArrayList<AxoObjectInstanceAbstract>) p.objectInstances.clone();
            for (AxoObjectInstanceAbstract o : obj2) {
                o.patch = this;
                AxoObjectAbstract obj = o.resolveType();
                if (obj != null) {
                    Modulator[] m = obj.getModulators();
                    if (m != null) {
                        if (Modulators == null) {
                            Modulators = new ArrayList<Modulator>();
                        }
                        for (Modulator mm : m) {
                            mm.objInst = o;
                            Modulators.add(mm);
                        }
                    }
                } else {
                    //o.patch = this;
                    p.objectInstances.remove(o);
                    AxoObjectInstanceZombie zombie = new AxoObjectInstanceZombie(new AxoObjectZombie(), this, o.getInstanceName(), new Point(o.getX(), o.getY()));
                    zombie.patch = this;
                    zombie.typeName = o.typeName;
                    zombie.PostConstructor();
                    p.objectInstances.add(zombie);
                }
            }
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
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
                    } catch (NumberFormatException e) {
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
                    } else {
                        while (GetObjectInstance(new_name) != null) {
                            new_name = new_name + "_";
                        }
                        while (dict.containsKey(new_name)) {
                            new_name = new_name + "_";
                        }
                    }
                    if (!new_name.equals(original_name)) {
                        o.setInstanceName(new_name);
                    }
                    dict.put(original_name, new_name);
                }
                if (o.getX() < minX) {
                    minX = o.getX();
                }
                if (o.getY() < minY) {
                    minY = o.getY();
                }
                o.patch = this;
                objectInstances.add(o);
                objectLayerPanel.add(o, 0);
                o.PostConstructor();
                int newposx = o.getX();
                int newposy = o.getY();

                if (pos != null) {
                    // paste at cursor position, with delta snapped to grid
                    newposx += Constants.X_GRID * ((pos.x - minX + Constants.X_GRID / 2) / Constants.X_GRID);
                    newposy += Constants.Y_GRID * ((pos.y - minY + Constants.Y_GRID / 2) / Constants.Y_GRID);
                }
                while (getObjectAtLocation(newposx, newposy) != null) {
                    newposx += Constants.X_GRID;
                    newposy += Constants.Y_GRID;
                }
                o.setLocation(newposx, newposy);
                o.SetSelected(true);
            }
            objectLayerPanel.validate();
            for (Net n : p.nets) {
                InletInstance connectedInlet = null;
                OutletInstance connectedOutlet = null;
                if (n.GetSource() != null) {
                    ArrayList<OutletInstance> source2 = new ArrayList<OutletInstance>();
                    for (OutletInstance o : n.GetSource()) {
                        String objname = o.getObjname();
                        String outletname = o.getOutletname();
                        if ((objname != null) && (outletname != null)) {
                            String on2 = dict.get(objname);
                            if (on2 != null) {
//                                o.name = on2 + " " + r[1];
                                OutletInstance i = new OutletInstance();
                                i.outletname = outletname;
                                i.objname = on2;
                                source2.add(i);
                            } else if (restoreConnectionsToExternalOutlets) {
                                AxoObjectInstanceAbstract obj = GetObjectInstance(objname);
                                if ((obj != null) && (connectedOutlet == null)) {
                                    OutletInstance oi = obj.GetOutletInstance(outletname);
                                    if (oi != null) {
                                        connectedOutlet = oi;
                                    }
                                }
                            }
                        }
                    }
                    n.SetSources(source2);
                }
                if (n.GetDests() != null) {
                    ArrayList<InletInstance> dest2 = new ArrayList<InletInstance>();
                    for (InletInstance o : n.GetDests()) {
                        String objname = o.getObjname();
                        String inletname = o.getInletname();
                        if ((objname != null) && (inletname != null)) {
                            String on2 = dict.get(objname);
                            if (on2 != null) {
                                InletInstance i = new InletInstance();
                                i.inletname = inletname;
                                i.objname = on2;
                                dest2.add(i);
                            }
                        }
                    }
                    n.SetDests(dest2);
                }
                if (n.GetSource().size() + n.GetDests().size() > 1) {
                    if ((connectedInlet == null) && (connectedOutlet == null)) {
                        n.SetPatch(this);
                        // n.PostConstructor();
                        nets.add(n);
                    } else if (connectedInlet != null) {
                        for (InletInstance o : n.GetDests()) {
                            InletInstance o2 = getInletByReference(o.getObjname(), o.getInletname());
                            if ((o2 != null) && (o2 != connectedInlet)) {
                                AddConnection(connectedInlet, o2);
                            }
                        }
                        for (OutletInstance o : n.GetSource()) {
                            OutletInstance o2 = getOutletByReference(o.getObjname(), o.getOutletname());
                            if (o2 != null) {
                                AddConnection(connectedInlet, o2);
                            }
                        }
                    } else if (connectedOutlet != null) {
                        for (InletInstance o : n.GetDests()) {
                            InletInstance o2 = getInletByReference(o.getObjname(), o.getInletname());
                            if (o2 != null) {
                                AddConnection(o2, connectedOutlet);
                            }
                        }
                    }
                }
            }
            AdjustSize();
            SetDirty();
        } catch (javax.xml.stream.XMLStreamException ex) {
            // silence
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private AxoObjectInstanceAbstract getObjectAtLocation(int x, int y) {
        for (AxoObjectInstanceAbstract o : objectInstances) {
            if ((o.getX() == x) && (o.getY() == y)) {
                return o;
            }
        }
        return null;
    }

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

    public NetDrawingPanel getNetDrawingPanel() {
        return this.netLayerPanel;
    }

    public DragWireOverlay getDragWireOverlay() {
        return this.dragWireOverlay;
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
            for (InletInstance i : n.GetDests()) {
                if (i.GetObjectInstance().isSelected()) {
                    sel++;
                }
            }
            for (OutletInstance i : n.GetSource()) {
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
        objectLayerPanel.validate();
        netLayerPanel.validate();

        Layers.setPreferredSize(new Dimension(5000, 5000));
        AdjustSize();
        Layers.validate();

        repaintPatch();
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
        else if (patchframe.getPatchGui().getSettings() != null && patchframe.getPatchGui().getSettings().subpatchmode != SubPatchMode.no) {
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
        Net n = super.AddConnection(il, ol); // This adds the Net data object to Patch's list
        if (n != null) {
            // REMOVE: netLayerPanel.add(n); // Net is not a JComponent, so it can't be added
            repaintPatch(); // Request a repaint to draw the new wire
        }
        return n;
    }

    @Override
    public Net AddConnection(InletInstance il, InletInstance ol) {
        Net n = super.AddConnection(il, ol);
        repaintPatch();
        return n;
    }

    @Override
    public Net RemoveConnection(IoletAbstract io) {
        Net n = super.RemoveConnection(io);
        if (n != null) {
            // This method likely modifies an existing Net (e.g., clears its connections)
            // or implicitly leads to its removal from patch.getNets() if it becomes invalid.
            // No direct add/remove call needed for the panel.
            repaintPatch(); // Request a repaint to reflect changes (e.g., wire disappears or changes)
        }
        return n;
    }

    public NetDragging getActiveNetDragging() {
        return this.netDragging;
    }

    /**
     * Sets the currently active NetDragging instance.
     * This method is called when a drag starts or ends.
     * @param activeNetDragging The NetDragging instance to set, or null to clear.
     */
    public void setActiveNetDragging(NetDragging activeNetDragging) {
        this.netDragging = activeNetDragging; // Update the NetDragging instance

        // CORRECTED: Access dragWireOverlay directly from 'this' (PatchGUI)
        if (this.dragWireOverlay != null) { // Check if the overlay itself is initialized
            if (activeNetDragging != null) {
                this.dragWireOverlay.showWire(); // Make the temporary wire visible
            } else {
                this.dragWireOverlay.hideWire(); // Hide the temporary wire
            }
        }
    }

    @Override
    public Net delete(Net n) {
        if (n != null) {
            // REMOVE: netLayerPanel.remove(n); // Net is not a JComponent, so it can't be removed from a panel
            // The `super.delete(n)` call should remove the Net data object from Patch's list.
        }
        Net nn = super.delete(n); // This removes the Net data object from Patch's list
        // Always repaint after deletion to remove the wire visually
        repaintPatch();
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
            Layers.add(netLayer, Integer.valueOf(1));           // Permanent nets (as background)
            Layers.add(objectLayer, Integer.valueOf(2));        // Objects and their Iolets (on top of nets)
            Layers.add(draggedObjectLayer, Integer.valueOf(3)); // Dragged objects
            Layers.add(dragWireOverlay, Integer.valueOf(4));    // Dragged wire (should be above objects/nets)
            Layers.add(selectionRectLayer, Integer.valueOf(5)); // Selection rectangle (highest)
            Layers.validate();
        } else {
            Layers.removeAll();
            Layers.add(objectLayer, Integer.valueOf(1));        // Objects and their Iolets (on top of nets)
            Layers.add(netLayer, Integer.valueOf(2));           // Permanent nets (as background)
            Layers.add(draggedObjectLayer, Integer.valueOf(3)); // Dragged objects
            Layers.add(dragWireOverlay, Integer.valueOf(4));    // Dragged wire (should be above objects/nets)
            Layers.add(selectionRectLayer, Integer.valueOf(5)); // Selection rectangle (highest)
            Layers.validate();
        }
    }

    @Override
    void GoLive() {
        Patch p = GetQCmdProcessor().getPatch();
        if (p != null) {
            p.Unlock();
        }
        super.GoLive();
    }

    @Override
    public void Lock() {
        super.Lock();
        patchframe.SetLive(true);
        Layers.setBackground(Theme.Patch_Locked_Background);
    }

    @Override
    public void Unlock() {
        super.Unlock();
        patchframe.SetLive(false);
        Layers.setBackground(Theme.Patch_Unlocked_Background);
    }

    @Override
    public void repaint() {
        if (Layers != null) {
            Layers.repaint();
        }
    }

    public void repaintPatch() {
        if (netLayerPanel != null) {
            netLayerPanel.repaint(); // Repaint all committed wires
        }
        if (objectLayerPanel != null) {
            objectLayerPanel.repaint(); // Repaint all objects
        }
        // If you have other layers for objects or other visual elements, repaint them too.
        // Or simply call Layers.repaint() for a full repaint of the JLayeredPane.
        Layers.repaint();
    }

    @Override
    void UpdateDSPLoad(int val200, boolean overload) {
        dspLoadPercent = val200 / 2.0f;
        patchframe.ShowDSPLoad(val200, overload);
    }

    float getDSPLoadPercent() {
        return dspLoadPercent;
    }

    public Dimension GetInitialSize() {
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
            PatchFrame pf = new PatchFrame(patch1, QCmdProcessor.getQCmdProcessor());
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
            PatchFrame pf = new PatchFrame(patch1, QCmdProcessor.getQCmdProcessor());
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
        MainFrame.prefs.addRecentFile(f.getAbsolutePath());
        return pf;
    }

    private OutletInstance findOutletAt(Point p) {
        // Iterate through all objects in the patch
        for (AxoObjectInstanceAbstract obj : objectInstances) {
            // Iterate through all outlets of each object
            for (OutletInstance outlet : obj.GetOutletInstances()) {
                // Get the center location of the outlet's "jack" in canvas coordinates
                Point jackLoc = outlet.getJackLocInCanvas();
                // Define a small clickable area around the jack (e.g., 10x10 pixels)
                // Adjust the size (5, 5, 10, 10) as needed for your UI's hit detection
                if (new Rectangle(jackLoc.x - 5, jackLoc.y - 5, 10, 10).contains(p)) {
                    return outlet; // Found an outlet at the clicked point
                }
            }
        }
        return null; // No outlet found at this point
    }

    private InletInstance findInletAt(Point p) {
        // Iterate through all objects in the patch
        for (AxoObjectInstanceAbstract obj : objectInstances) {
            // Iterate through all inlets of each object
            for (InletInstance inlet : obj.GetInletInstances()) {
                // Get the center location of the inlet's "jack" in canvas coordinates
                Point jackLoc = inlet.getJackLocInCanvas();
                // Define a small clickable area around the jack
                if (new Rectangle(jackLoc.x - 5, jackLoc.y - 5, 10, 10).contains(p)) {
                    return inlet; // Found an inlet at the clicked point
                }
            }
        }
        return null; // No inlet found at this point
    }

    public void setCableTypeEnabled(DataType type, boolean enabled) {
        cableTypeEnabled.put(type, enabled);
        updateNetVisibility(); // Call update to immediately refresh the view
    }

    public Boolean isCableTypeEnabled(DataType type) {
        if (cableTypeEnabled.containsKey(type)) {
            return cableTypeEnabled.get(type);
        } else {
            return true; // Default to visible if no specific setting exists
        }
    }

    public void updateNetVisibility() {
        repaintPatch();
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
