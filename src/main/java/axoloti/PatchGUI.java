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

import axoloti.attribute.AttributeInstance;
import axoloti.datatypes.DataType;
import axoloti.dialogs.FindTextDialog;
import axoloti.inlets.InletInstance;
import axoloti.iolet.IoletAbstract;
import axoloti.object.AxoObject;
import axoloti.object.AxoObjectAbstract;
import axoloti.object.AxoObjectFromPatch;
import axoloti.object.AxoObjectInstance;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.object.AxoObjectInstanceZombie;
import axoloti.object.AxoObjectZombie;
import axoloti.outlets.OutletInstance;
import axoloti.parameters.ParameterInstance;
import axoloti.ui.MaterialColors;
import axoloti.ui.SelectionRectangle;
import axoloti.ui.Theme;
import axoloti.utils.Constants;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;
import axoloti.utils.StringRef;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
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

    private final static int capitalLetterOffset = 26;
    private int MousePressedBtn = 0;
    private float dspLoadPercent = 0.0f;

    public JLayeredPane Layers = new JLayeredPane();
    public JPanel objectLayerPanel = new JPanel();
    public JPanel draggedObjectLayerPanel = new JPanel();
    public JPanel netLayerPanel;

    JLayer<JComponent> objectLayer = new JLayer<JComponent>(objectLayerPanel);
    JLayer<JComponent> draggedObjectLayer = new JLayer<JComponent>(draggedObjectLayerPanel);
    JLayer<JComponent> netLayer;

    private ArrayList<AxoObjectInstanceAbstract> findTextResults = new ArrayList<>();
    private int currentFindTextMatchIndex = -1;
    private int currentFindCheckmask = 0;
    private String currentFindTextString = "";

    public AxoObjectFromPatch ObjEditor;
    public ObjectSearchFrame osf;
    TextEditor NotesTextEditor;

    SelectionRectangle selectionrectangle = new SelectionRectangle();
    Point selectionRectStart;
    Point panOrigin;
    private Map<DataType, Boolean> cableTypeEnabled = new HashMap<DataType, Boolean>();

    enum Direction {
        UP, LEFT, DOWN, RIGHT
    }

    public PatchGUI() {
        super();

        Layers.setLayout(null);
        Layers.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
        Layers.setLocation(0, 0);
        Layers.setFont(Constants.FONT);

        netLayerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                /* Set up find text highlight handler */
                if (findTextResults.isEmpty()) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g;

                for (int i = 0; i < findTextResults.size(); i++) {
                    AxoObjectInstanceAbstract obj = findTextResults.get(i);
                    boolean isCurrentMatch = (i == currentFindTextMatchIndex);

                    Color highlightColor = MaterialColors.PURPLE_A700;
                    int strokeWidth = isCurrentMatch ? 6 : 2;
                    BasicStroke highlightStroke = new BasicStroke(strokeWidth);
                    int halfStroke = strokeWidth / 2;

                    g2.setColor(highlightColor);
                    g2.setStroke(highlightStroke);

                    g2.drawRect(obj.getX() - halfStroke,
                                 obj.getY() - halfStroke,
                                 obj.getWidth() + strokeWidth,
                                 obj.getHeight() + strokeWidth);
                }
            }
        };
        netLayer = new JLayer<JComponent>(netLayerPanel);

        JComponent[] layerComponents = {
            objectLayer, objectLayerPanel, draggedObjectLayerPanel, netLayerPanel,
            draggedObjectLayer, netLayer};
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

        objectLayer.setName("objectLayer");
        draggedObjectLayer.setName("draggedObjectLayer");
        netLayer.setName("netLayer");
        netLayerPanel.setName("netLayerPanel");

        objectLayerPanel.setName(Constants.OBJECT_LAYER_PANEL);
        draggedObjectLayerPanel.setName(Constants.DRAGGED_OBJECT_LAYER_PANEL);

        draggedObjectLayerPanel.add(selectionrectangle);

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
                    LOGGER.log(Level.SEVERE, "Error during export to clipboard action: " + ex.getMessage());
                    ex.printStackTrace(System.out);
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
                    LOGGER.log(Level.WARNING, "Paste: Unknown file format.");
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error during paste action: " + ex.getMessage());
                    ex.printStackTrace(System.out);
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
                    JScrollPane scrollPane = getPatchframe().getScrollPane();

                    if (scrollPane != null) {
                        JScrollBar hBar = scrollPane.getHorizontalScrollBar();
                        JScrollBar vBar = scrollPane.getVerticalScrollBar();
                        int keyCode = ke.getKeyCode();
                        boolean scrolled = false;
                        int step;
                        if (!ke.isShiftDown()) {
                            step = Constants.X_GRID * 5;
                        } else {
                            step = Constants.X_GRID;
                        }

                        if (keyCode == KeyEvent.VK_UP) {
                            vBar.setValue(vBar.getValue() - step);
                            scrolled = true;
                        } else if (keyCode == KeyEvent.VK_DOWN) {
                            vBar.setValue(vBar.getValue() + step);
                            scrolled = true;
                        } else if (keyCode == KeyEvent.VK_LEFT) {
                            hBar.setValue(hBar.getValue() - step);
                            scrolled = true;
                        } else if (keyCode == KeyEvent.VK_RIGHT) {
                            hBar.setValue(hBar.getValue() + step);
                            scrolled = true;
                        }

                        if (scrolled) {
                            ke.consume();
                            return;
                        }
                    }
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
                    if (!me.isShiftDown() && !KeyUtils.isControlOrCommandDown(me)) {
                        /* Clear selection only if no modifier keys are held */
                        for (AxoObjectInstanceAbstract o : objectInstances) {
                            o.SetSelected(false);
                        }
                    }
                    selectionRectStart = me.getPoint();
                    selectionrectangle.setBounds(me.getX(), me.getY(), 1, 1);

                    Layers.requestFocusInWindow();
                    me.consume();
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                    selectionrectangle.setVisible(false);
                    draggedObjectLayerPanel.repaint();
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

                        /* Create lists to store all dropped .ax* files */
                        List<File> axoFilesToProcess = new ArrayList<>();
                        List<File> axsFilesToProcess = new ArrayList<>();
                        List<File> patchFilesToProcess = new ArrayList<>();

                        for (File f : flist) {
                            if (f.exists() && f.canRead()) {
                                String fn = f.getName();

                                if (fn.endsWith(".axp") || fn.endsWith(".axh")) {
                                    /* Collect .axp and .axh files to a list for opening them aligned next to each other later */
                                    patchFilesToProcess.add(f);
                                } else if (fn.endsWith(".axo")) {
                                    /* Collect .axo files to a list for opening them aligned next to each other later */
                                    axoFilesToProcess.add(f);
                                } else if (fn.endsWith(".axs")) {
                                    /* Collect .axs files to a list for opening them aligned next to each other later */
                                    axsFilesToProcess.add(f);
                                }
                            }
                        }

                        /* Collect names of successfully placed instances so we can set them as selected later */
                        HashSet<String> placedInstanceNames = new HashSet<>();
                        /* Open the collected files aligned next to each other */
                        Point dropLocation = dtde.getLocation();
                        int alignedX = ((dropLocation.x + (Constants.X_GRID / 2)) / Constants.X_GRID) * Constants.X_GRID;
                        int alignedY = ((dropLocation.y + (Constants.Y_GRID / 2)) / Constants.Y_GRID) * Constants.Y_GRID;
                        Point alignedDropLocation = new Point(alignedX, alignedY);
                        int xOffset = 0;

                        if (!axoFilesToProcess.isEmpty()) {
                            for (File f : axoFilesToProcess) {
                                try {
                                    AxoObject loadedObject = AxoObject.loadAxoObjectFromFile(f.toPath());
                                    AxoObjectInstanceAbstract abstractInstance = null;
                                    Point instanceLocation = new Point(alignedDropLocation.x + xOffset, alignedDropLocation.y);

                                    if (loadedObject != null) {
                                        AxoObjectAbstract libraryObject = MainFrame.axoObjects.GetAxoObjectFromUUID(loadedObject.getUUID());

                                        if (libraryObject == null && loadedObject.id != null) {
                                            ArrayList<AxoObjectAbstract> foundObjects = MainFrame.axoObjects.GetAxoObjectFromName(loadedObject.id, null);
                                            if (foundObjects != null && !foundObjects.isEmpty()) {
                                                libraryObject = foundObjects.get(0);
                                            }
                                        }

                                        if (libraryObject != null) {
                                            abstractInstance = AddObjectInstance(libraryObject, instanceLocation);
                                            if (abstractInstance != null) {
                                                placedInstanceNames.add(abstractInstance.getInstanceName().split("__temp")[0]);
                                                xOffset += abstractInstance.getWidth() + Constants.X_GRID * 2;
                                                LOGGER.info("Placed reference to library object " + libraryObject.id + ", labeled \"" + abstractInstance.getInstanceName() + "\"");
                                            }
                                        } else {
                                            abstractInstance = AddObjectInstance(loadedObject, instanceLocation);
                                            if (abstractInstance != null) {
                                                AxoObjectInstance concreteInstance = (AxoObjectInstance) abstractInstance;
                                                String uniqueName = getSimpleUniqueName(loadedObject.id);
                                                concreteInstance.setInstanceName(uniqueName);
                                                concreteInstance.ConvertToEmbeddedObj();
                                                placedInstanceNames.add(concreteInstance.getInstanceName().split("__temp")[0]);
                                                xOffset += concreteInstance.getWidth() + Constants.X_GRID * 2;
                                                LOGGER.info("Placed new embedded object labeled \"" + uniqueName + "\" from file " + f.getName());
                                            }
                                        }
                                        SetDirty();
                                    }
                                } catch (Exception ex) {
                                    LOGGER.log(Level.SEVERE, "Error: Failed to parse AxoObject from file: " + f.getName() + ", " + ex.getMessage());
                                    ex.printStackTrace(System.out);
                                }
                            }
                        }

                        if (!axsFilesToProcess.isEmpty()) {
                            for (File f : axsFilesToProcess) {
                                try {
                                    AxoObjectInstanceAbstract abstractInstance = null;
                                    String canonicalId = MainFrame.axoObjects.getCanonicalObjectIdFromPath(f);
                                    Point instanceLocation = new Point(alignedDropLocation.x + xOffset, alignedDropLocation.y);

                                    if (canonicalId != null) {
                                        /* The dropped subpatch is a library subpatch */
                                        ArrayList<AxoObjectAbstract> objs = MainFrame.axoObjects.GetAxoObjectFromName(canonicalId, GetCurrentWorkingDirectory());
                                        if (objs != null && !objs.isEmpty()) {
                                            abstractInstance = AddObjectInstance(objs.get(0), instanceLocation);
                                            if (abstractInstance != null) {
                                                placedInstanceNames.add(abstractInstance.getInstanceName().split("__temp")[0]);
                                                xOffset += abstractInstance.getWidth() + Constants.X_GRID * 2;
                                                LOGGER.info("Placed reference to library subpatch " + canonicalId + ", labeled \"" + abstractInstance.getInstanceName() + "\"");
                                            }
                                        }
                                    } else {
                                        /* The dropped subpatch is a local subpatch (not found in any library). */
                                        AxoObjectFromPatch loadedObject = new AxoObjectFromPatch(f);

                                        String absolutePath = f.getAbsolutePath();
                                        String currentDirectory = GetCurrentWorkingDirectory();

                                        if (currentDirectory != null && absolutePath.startsWith(currentDirectory)) {
                                            loadedObject.createdFromRelativePath = true;
                                            String relativePath = absolutePath.substring(currentDirectory.length() + File.separator.length());
                                            loadedObject.id = relativePath.substring(0, relativePath.lastIndexOf('.'));
                                        } else {
                                            loadedObject.id = absolutePath.substring(0, absolutePath.lastIndexOf('.'));
                                        }

                                        abstractInstance = AddObjectInstance(loadedObject, instanceLocation);
                                        if (abstractInstance != null) {
                                            /* Embed the newly created subpatch to avoid zombiism */
                                            AxoObjectInstance concreteInstance = (AxoObjectInstance) abstractInstance;
                                            concreteInstance.ConvertToPatchPatcher(); 
                                            placedInstanceNames.add(concreteInstance.getInstanceName().split("__temp")[0]);
                                            xOffset += concreteInstance.getWidth() + Constants.X_GRID * 2;
                                            LOGGER.info("Placed new embedded subpatch labeled \"" + concreteInstance.getInstanceName() + "\" from file " + f.getName());
                                        }
                                    }
                                    SetDirty();
                                } catch (Exception ex) {
                                    LOGGER.log(Level.SEVERE, "Error: Failed to parse subpatch from file: " + f.getName() + ", " + ex.getMessage());
                                    ex.printStackTrace(System.out);
                                }
                            }
                        }

                        if (!patchFilesToProcess.isEmpty()) {
                            for (File f : patchFilesToProcess) {
                                try {
                                    AxoObjectFromPatch loadedObject = new AxoObjectFromPatch(f);
                                    String absolutePath = f.getAbsolutePath();
                                    String currentDirectory = GetCurrentWorkingDirectory();

                                    if (currentDirectory != null && absolutePath.startsWith(currentDirectory)) {
                                        loadedObject.createdFromRelativePath = true;
                                        String relativePath = absolutePath.substring(currentDirectory.length() + File.separator.length());
                                        loadedObject.id = relativePath.substring(0, relativePath.lastIndexOf('.'));
                                    } else {
                                        loadedObject.id = absolutePath.substring(0, absolutePath.lastIndexOf('.'));
                                    }

                                    Point instanceLocation = new Point(alignedDropLocation.x + xOffset, alignedDropLocation.y);
                                    AxoObjectInstanceAbstract abstractInstance = AddObjectInstance(loadedObject, instanceLocation);

                                    if (abstractInstance != null) {
                                        AxoObjectInstance concreteInstance = (AxoObjectInstance) abstractInstance;
                                        concreteInstance.ConvertToPatchPatcher(); 
                                        placedInstanceNames.add(concreteInstance.getInstanceName().split("__temp")[0]);
                                        LOGGER.info("Placed new embedded subpatch labeled \"" + concreteInstance.getInstanceName() + "\" from file " + f.getName());
                                        xOffset += concreteInstance.getWidth() + Constants.X_GRID * 2;
                                    }
                                    SetDirty();
                                } catch (Exception ex) {
                                    LOGGER.log(Level.SEVERE, "Error: Failed to parse patch from file: " + f.getName() + ", " + ex.getMessage());
                                    ex.printStackTrace(System.out);
                                }
                            }
                        }

                        SelectNone();
                        for (AxoObjectInstanceAbstract instance : objectInstances) {
                            if (placedInstanceNames.contains(instance.getInstanceName())) {
                                instance.SetSelected(true);
                            }
                        }

                        dtde.dropComplete(true);

                    } catch (UnsupportedFlavorException ex) {
                        LOGGER.log(Level.WARNING, "Drag and drop: Unknown file format.");
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, "Error dropping file(s): " + ex.getMessage());
                        ex.printStackTrace(System.out);
                    }
                    return;
                }
                super.drop(dtde);
            }
        };

        Layers.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                if (MousePressedBtn == MouseEvent.BUTTON1) {
                    int x2 = ev.getX();
                    int y2 = ev.getY();
                    int xmin = selectionRectStart.x < x2 ? selectionRectStart.x : x2;
                    int xmax = selectionRectStart.x > x2 ? selectionRectStart.x : x2;
                    int ymin = selectionRectStart.y < y2 ? selectionRectStart.y : y2;
                    int ymax = selectionRectStart.y > y2 ? selectionRectStart.y : y2;
                    int width = xmax - xmin;
                    int height = ymax - ymin;
                    selectionrectangle.setBounds(xmin, ymin, width, height);
                    if (!selectionrectangle.isVisible()) {
                        selectionrectangle.setVisible(true);
                    }

                    draggedObjectLayerPanel.repaint();
                    Rectangle r = selectionrectangle.getBounds();

                    for (AxoObjectInstanceAbstract o : objectInstances) {
                        if (!o.IsLocked()) {
                            boolean intersects = o.getBounds().intersects(r);
                            if (ev.isShiftDown()) {
                                /* Shift-drag: Add objects to selection */
                                if (intersects) {
                                    o.SetSelected(true);
                                }
                            } else if (KeyUtils.isControlOrCommandDown(ev)) {
                                /* Ctrl-drag: Remove objects from selection */
                                if (intersects) {
                                    o.SetSelected(false);
                                }
                            }
                            else {
                                /* Normal drag: Only select intersecting objects, deselect all others */
                                o.SetSelected(intersects);
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

            for (AxoObjectInstanceAbstract o : p.objectInstances) {
                String original_name = o.getInstanceName();
                if (original_name != null) {
                    String new_name = getSmartUniqueName(original_name);

                    if (!new_name.equals(original_name)) {
                        o.setInstanceName(new_name);
                    }
                    dict.put(original_name, new_name);
                }

                objectInstances.add(o);
                objectLayerPanel.add(o, 0);
                o.PostConstructor();

                int newposx = o.getX() + xOffset + xCollisionShift;
                int newposy = o.getY() + yOffset + yCollisionShift;

                o.setLocation(newposx, newposy);
                o.SetSelected(true);
            }

            objectLayerPanel.validate();

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
                                    OutletInstance newOutletInstance = newObj.getOutletInstance(outletname);
                                    if (newOutletInstance != null) {
                                        source2.add(newOutletInstance);
                                    }
                                }
                            }
                            else if (applyWiresFromExternalOutlets) {
                                /* It's an external connection */
                                AxoObjectInstanceAbstract obj = GetObjectInstance(objname);
                                if ((obj != null) && (connectedOutlet == null)) {
                                    OutletInstance oi = obj.getOutletInstance(outletname);
                                    if (oi != null) {
                                        connectedOutlet = oi;
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
                    for (InletInstance o : n.dest) {
                        String objname = o.getObjname();
                        String inletname = o.getInletname();
                        if ((objname != null) && (inletname != null)) {
                            String on2 = dict.get(objname);
                            if (on2 != null) {
                                /* It's an internal connection. Get the new, valid instance */
                                AxoObjectInstanceAbstract newObj = GetObjectInstance(on2);
                                if (newObj != null) {
                                    InletInstance newInletInstance = newObj.getInletInstance(inletname);
                                    if (newInletInstance != null) {
                                        dest2.add(newInletInstance);
                                    }
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
                }
                else if (connectedOutlet != null && n.dest.size() > 0 && applyWiresFromExternalOutlets) {
                    /* Net has an external source and needs to be connected to the new inlets */
                    if (!externalConnectionsToMake.containsKey(connectedOutlet)) {
                        externalConnectionsToMake.put(connectedOutlet, new ArrayList<>());
                    }
                    externalConnectionsToMake.get(connectedOutlet).addAll(n.dest);
                }
                else if (n.source.size() == 0 && n.dest.size() > 1) {
                    /* Floating net: the net connects two or more inlets in the group but the connection to the external outlet is severed. */
                    n.PostConstructor();
                    nets.add(n);
                    netLayerPanel.add(n);
                }
            }

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
            LOGGER.log(Level.SEVERE, "Error during paste action: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    public String getSimpleUniqueName(String baseName) {
        int suffix = 1;
        String uniqueName;

        while (true) {
            uniqueName = baseName + "_" + suffix;
            if (GetObjectInstance(uniqueName) == null) {
                return uniqueName;
            }
            suffix++;
        }
    }

    public String getSmartUniqueName(String desiredName) {
        String baseName = desiredName;
        int suffix = 1;

        Pattern underscorePattern = Pattern.compile("_((\\d)+)$");
        Matcher underscoreMatcher = underscorePattern.matcher(desiredName);

        if (underscoreMatcher.find()) {
            try {
                suffix = Integer.parseInt(underscoreMatcher.group(1));
                baseName = desiredName.substring(0, underscoreMatcher.start());
            } catch (NumberFormatException e) {
                baseName = desiredName;
            }
        }

        String uniqueName;
        while (true) {
            uniqueName = baseName + "_" + suffix;

            if (GetObjectInstance(uniqueName) == null) {
                return uniqueName;
            }
            suffix++;
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
            int xgrid = 1; int xstep = 0;
            int ygrid = 1; int ystep = 0;
            switch (dir) {
                case DOWN:
                    ystep = ysteps; ygrid = ysteps;
                    break;
                case UP:
                    ystep = -ysteps; ygrid = ysteps;
                    break;
                case LEFT:
                    xstep = -xsteps; xgrid = xsteps;
                    break;
                case RIGHT:
                    xstep = xsteps; xgrid = xsteps;
                    break;
            }

            boolean isUpdate = false;
            Rectangle boundsToRepaint = null;
            Rectangle netLayerBoundsToRepaint = null;
            final int PADDING = xsteps + ysteps;

            for (AxoObjectInstanceAbstract o : objectInstances) {
                if (o.isSelected()) {
                    isUpdate = true;

                    Rectangle oldBounds = o.getBounds();
                    Point p = o.getLocation();
                    p.x = p.x + xstep;
                    p.y = p.y + ystep;
                    p.x = xgrid * (p.x / xgrid);
                    p.y = ygrid * (p.y / ygrid);
                    o.SetLocation(p.x, p.y);

                    Rectangle newBounds = o.getBounds();
                    Rectangle paddedOldBounds = new Rectangle(
                        oldBounds.x - PADDING, oldBounds.y - PADDING,
                        oldBounds.width + 2 * PADDING, oldBounds.height + 2 * PADDING
                    );
                    Rectangle paddedNewBounds = new Rectangle(
                        newBounds.x - PADDING, newBounds.y - PADDING,
                        newBounds.width + 2 * PADDING, newBounds.height + 2 * PADDING
                    );

                    if (netLayerBoundsToRepaint == null) {
                        netLayerBoundsToRepaint = paddedOldBounds;
                    } else {
                        netLayerBoundsToRepaint.add(paddedOldBounds);
                    }
                    netLayerBoundsToRepaint.add(paddedNewBounds);

                    if (boundsToRepaint == null) {
                        boundsToRepaint = newBounds;
                    } else {
                        boundsToRepaint.add(newBounds);
                    }
                }
            }

            if (isUpdate) {
                AdjustSize();
                SetDirty();

                if (boundsToRepaint != null) {
                    objectLayerPanel.repaint(boundsToRepaint); /* Repaint only the combined area */

                }

                if (netLayerBoundsToRepaint != null) {
                    netLayerPanel.repaint(netLayerBoundsToRepaint);
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
        setFileNamePath(FileNamePath, null);
    }

    @Override
    public void setFileNamePath(String FileNamePath, String parentName) {
        super.setFileNamePath(FileNamePath);
        String title;

        if (parentName != null && !parentName.isEmpty()) {
            title = FileNamePath + "  (embedded subpatch - " + parentName + ")";
            patchframe.setSaveMenuEnabled(false);
        } else if (getSettings() != null && getSettings().subpatchmode != SubPatchMode.no) {
            int brk = FileNamePath.lastIndexOf(File.separator) + 1;
            if (brk != 0) {
                title = FileNamePath.substring(brk) + "  [" + FileNamePath.substring(0, brk-1) + "]";
            } else {
                title = FileNamePath;
            }
            title += "  (local subpatch)";
            patchframe.setSaveMenuEnabled(false);
        } else {
            int brk = FileNamePath.lastIndexOf(File.separator) + 1;
            if (brk != 0) {
                title = FileNamePath.substring(brk) + "  [" + FileNamePath.substring(0, brk-1) + "]";
            } else {
                title = FileNamePath;
            }
        }
        patchframe.setTitle(title);
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

    public IoletAbstract getIoletAtLocation(Point p) {
        for (int i = objectInstances.size() - 1; i >= 0; i--) {
            AxoObjectInstanceAbstract o = objectInstances.get(i);
            Point p_obj = SwingUtilities.convertPoint(Layers, p, o);

            for (InletInstance inlet : o.getInletInstances()) {
                Point p_inlet = SwingUtilities.convertPoint(o, p_obj, inlet);
                if (inlet.contains(p_inlet)) {
                    return inlet;
                }
            }

            for (OutletInstance outlet : o.getOutletInstances()) {
                Point p_outlet = SwingUtilities.convertPoint(o, p_obj, outlet);
                if (outlet.contains(p_outlet)) {
                    return outlet;
                }
            }
        }
        return null;
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
        } else {
            Layers.removeAll();
            Layers.add(objectLayer, Integer.valueOf(1));
            Layers.add(netLayer, Integer.valueOf(2));
            Layers.add(draggedObjectLayer, Integer.valueOf(3));
        }
    }

    public void findAndHighlight(String searchText, int direction, FindTextDialog dialog, int checkmask) {
        boolean isNewSearch = false; 

        if (searchText.isEmpty()) {
            currentFindTextString = searchText;
            currentFindCheckmask = checkmask; 
            findTextResults.clear();
            currentFindTextMatchIndex = -1;
            isNewSearch = true; 
            if (netLayerPanel != null) {
                netLayerPanel.repaint();
            }
        } else if (!searchText.equals(currentFindTextString) || checkmask != currentFindCheckmask) {
            currentFindTextString = searchText;
            currentFindCheckmask = checkmask;
            findTextResults.clear();
            isNewSearch = true;
            final String lowerSearchText = searchText.toLowerCase();

            for (AxoObjectInstanceAbstract obj : objectInstances) {
                boolean isMatch = false;

                if ((checkmask & FindTextDialog.FIND_TARGET_NAME) != 0) {
                    if (obj.typeName != null && obj.typeName.toLowerCase().contains(lowerSearchText)) {
                        isMatch = true;
                    }
                }

                if ((checkmask & FindTextDialog.FIND_TARGET_LABEL) != 0) {
                    if (!isMatch && obj.getInstanceName() != null && obj.getInstanceName().toLowerCase().contains(lowerSearchText)) {
                        isMatch = true;
                    }
                }

                if ((checkmask & FindTextDialog.FIND_TARGET_ATTRIBUTES) != 0) {
                    if (!isMatch && obj.getAttributeInstances() != null) {
                        for (AttributeInstance ai : obj.getAttributeInstances()) {
                            if (ai.getName().toLowerCase().contains(lowerSearchText)) {
                                isMatch = true;
                                break;
                            }
                        }
                    }
                }

                if ((checkmask & FindTextDialog.FIND_TARGET_PARAMETERS) != 0) {
                    if (!isMatch && obj.getParameterInstances() != null) {
                        for (ParameterInstance pi : obj.getParameterInstances()) {
                            if (pi.getName().toLowerCase().contains(lowerSearchText)) {
                                isMatch = true;
                                break;
                            }
                        }
                    }
                }

                if ((checkmask & FindTextDialog.FIND_TARGET_IOLETS) != 0) {
                    if (!isMatch && obj.getInletInstances() != null) {
                        for (InletInstance ii : obj.getInletInstances()) {
                            if (ii.getInletname().toLowerCase().contains(lowerSearchText)) {
                                isMatch = true;
                                break;
                            }
                        }
                    }

                    if (!isMatch && obj.getOutletInstances() != null) {
                        for (OutletInstance ii : obj.getOutletInstances()) {
                            if (ii.getOutletname().toLowerCase().contains(lowerSearchText)) {
                                isMatch = true;
                                break;
                            }
                        }
                    }
                }

                if (isMatch) {
                    findTextResults.add(obj);
                }
            }
            if (netLayerPanel != null) {
                netLayerPanel.repaint(); 
            }

            currentFindTextMatchIndex = findTextResults.isEmpty() ? -1 : 0; 
        }

        if (!findTextResults.isEmpty() && !isNewSearch && direction != 0) { 
            currentFindTextMatchIndex += direction;
            if (currentFindTextMatchIndex >= findTextResults.size()) {
                currentFindTextMatchIndex = 0;
            } else if (currentFindTextMatchIndex < 0) {
                currentFindTextMatchIndex = findTextResults.size() - 1;
            }
        }

        if (!findTextResults.isEmpty() && currentFindTextMatchIndex != -1) {
            if (dialog != null) {
                dialog.updateResults(currentFindTextMatchIndex, findTextResults.size());
            }
            AxoObjectInstanceAbstract obj = findTextResults.get(currentFindTextMatchIndex);
            getPatchframe().scrollPatchViewTo(new Rectangle(obj.getX(),
                                            obj.getY(),
                                            obj.getWidth(),
                                            obj.getHeight()));

            if (netLayerPanel != null) {
                netLayerPanel.repaint(); 
            }
        } else {
            currentFindTextMatchIndex = -1; 
            if (dialog != null) {
                dialog.updateResults(currentFindTextMatchIndex, findTextResults.size());
            }
            if (netLayerPanel != null) {
                netLayerPanel.repaint();
            }
        }
    }

    public int getCurrentFindTextMatchIndex() {
        return currentFindTextMatchIndex;
    }

    public ArrayList<AxoObjectInstanceAbstract> getFindTextResults() {
        return findTextResults;
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
            ObjEditor.UpdateObject(this);
        }
        return b;
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
                LOGGER.log(Level.SEVERE, "Patch \'" + f.getAbsoluteFile() + "\' was saved with a newer version of Ksoloti: " + pve.getMessage());
            } else {
                LOGGER.log(Level.SEVERE, "Error during open patch action: " + ite.getMessage());
                ite.printStackTrace(System.out);
            }
            return null;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during open patch action: " + ex.getMessage());
            ex.printStackTrace(System.out);
            return null;
        }
    }

    public static void OpenPatch(String name, InputStream stream) {
        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, new Format(2));
        try {
            PatchGUI patch1 = serializer.read(PatchGUI.class, stream);
            PatchFrame pf = new PatchFrame(patch1);
            patch1.PostContructor();
            patch1.setFileNamePath(name);
            pf.setVisible(true);
            pf.setState(java.awt.Frame.NORMAL);
            pf.repositionIfOutsideScreen();
            pf.toFront();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during patch open: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    public static PatchFrame OpenPatch(File f) {
        PatchFrame pf = OpenPatchInvisible(f);
        if (pf == null) {
            LOGGER.log(Level.SEVERE, "Failed to open patch: " + f.getName());
            return null;
        }
        pf.setState(java.awt.Frame.NORMAL);
        pf.setVisible(true);
        pf.repositionIfOutsideScreen();
        pf.toFront();
        Preferences.getInstance().addRecentFile(f.getAbsolutePath());
        return pf;
    }

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
        if (osf != null) {
            osf.dispose();
        }
        if (NotesTextEditor != null) {
            NotesTextEditor.dispose();
        }
        if ((settings != null) && (settings.editor != null)) {
            settings.editor.dispose();
        }
    }
}
