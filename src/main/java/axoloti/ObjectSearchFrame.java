/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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

import axoloti.object.AxoObjectAbstract;
import axoloti.object.AxoObjectIdComparator;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.object.AxoObjectTreeNode;
import axoloti.ui.Theme;
import axoloti.utils.Constants;
import axoloti.utils.OSDetect;
import axoloti.utils.ResizableUndecoratedFrame;
import components.ScrollPaneComponent;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author Johannes Taelman
 */
public class ObjectSearchFrame extends ResizableUndecoratedFrame {

    DefaultMutableTreeNode root;
    DefaultTreeModel tm;
    public AxoObjectAbstract type;
    private final PatchGUI p;
    private AxoObjectInstanceAbstract target_object;
    private AxoObjectTreeNode objectTree;

    AxoObjectAbstract previewObj;
    private int patchLocX;
    private int patchLocY;

    private boolean accepted = false;
    private boolean dragStarted = false;
    private Point dragStartPoint = null;
    private int dragStartIndex = -1;

    /* Shortcut strings */
    public static final String shortcutList[] = {

        /* Lower case shortcuts should ideally point to low-level and basic objects */
        /* a */ null,
        /* b */ null,
        /* c */ "ctrl/",
        /* d */ "disp/",
        /* e */ "patch/object", /* 'e'mbed object */
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
        /* W */ "osc/", /* 'W'aveform generator */
        /* X */ "fx/",
        /* Y */ null,
        /* Z */ null,
    };

    private ScrollPaneComponent jScrollPaneObjectInfo;
    private ScrollPaneComponent jScrollPaneObjectPreview;
    private ScrollPaneComponent jScrollPaneObjectSearch;
    private ScrollPaneComponent jScrollPaneObjectTree;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JButton jButtonAccept;
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JList jResultList;
    private javax.swing.JPanel jPanelLeft;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelObjectPreview;
    private javax.swing.JPanel jPanelSearchField;
    private javax.swing.JSplitPane jSplitPaneLeft;
    private javax.swing.JSplitPane jSplitPaneMain;
    private javax.swing.JSplitPane jSplitPaneRight;
    private javax.swing.JTextField jTextFieldObjName;
    private javax.swing.JTextPane jTextPaneObjectInfo;
    private javax.swing.JTree jObjectTree;

    class StringIcon implements Icon {

        final String str;
        final int w, h;

        public StringIcon(String str) {
            this(str, 20, 20);
        }

        public StringIcon(String str, int w, int h) {
            this.str = str;
            this.w = w;
            this.h = h;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            FontMetrics metrics = g2.getFontMetrics(g2.getFont());
            Rectangle2D bounds = metrics.getStringBounds(str, g2);
            int xc = (w / 2) + x;
            int yc = (h / 2) + y;
            g2.drawString(str, xc - (int) bounds.getCenterX(), yc - (int) bounds.getCenterY());
        }

        @Override
        public int getIconWidth() {
            return w;
        }

        @Override
        public int getIconHeight() {
            return h;
        }
    }

    /**
     * Creates new form ObjectSearchFrame
     *
     * @param p parent
     */
    public ObjectSearchFrame(PatchGUI p) {
        super();
        initComponents();

        if (OSDetect.getOS() == OSDetect.OS.MAC) {
            // buttons with a text label use huge margins on macos
            // or when forced, will substitute the label with '...',
            // while buttons with just an icon can have a tight margin (want!)
            // We're using a single unicode character as a label
            // but do not want it to be treated as a label...
            jButtonCancel.setIcon(new StringIcon(jButtonCancel.getText()));
            jButtonCancel.setText(null);
            jButtonAccept.setIcon(new StringIcon(jButtonAccept.getText()));
            jButtonAccept.setText(null);
            // Alternative approach: use real icons
            // Unfortunately macos does not provide icons. with appropriate
            // semantics..
            //
            // Toolkit toolkit = Toolkit.getDefaultToolkit();
            // Image image = toolkit.getImage("NSImage://NSStopProgressTemplate");
            // image = toolkit.getImage("NSImage://NSMenuOnStateTemplate");
            //
            // prettify buttons - macos exclusive
            jButtonCancel.putClientProperty("JButton.buttonType", "segmented");
            jButtonAccept.putClientProperty("JButton.buttonType", "segmented");
            jButtonCancel.putClientProperty("JButton.segmentPosition", "first");
            jButtonAccept.putClientProperty("JButton.segmentPosition", "last");
        }

        jButtonAccept.setEnabled(false);

        this.setTitle("    Object Finder");

        this.p = p;
        DefaultMutableTreeNode root1 = new DefaultMutableTreeNode();
        this.objectTree = MainFrame.axoObjects.getObjectTree();
        this.root = PopulateJTree(MainFrame.axoObjects.getObjectTree(), root1);
        tm = new DefaultTreeModel(this.root);
        jObjectTree.setModel(tm);
        jObjectTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) jObjectTree.getLastSelectedPathComponent();
                if (node == null) {
                    return;
                }
                if (node.getUserObject() instanceof AxoObjectTreeNode) {
                    AxoObjectTreeNode anode = (AxoObjectTreeNode) node.getUserObject();
                    jPanelObjectPreview.removeAll();
                    jPanelObjectPreview.repaint();
                    if (anode.description != null) {
                        jTextPaneObjectInfo.setText(anode.description.replace("\n", "<br/>"));
                    }
                    jTextPaneObjectInfo.setCaretPosition(0);
                    previewObj = null;
                }
                Object nodeInfo = node.getUserObject();
                if (nodeInfo instanceof AxoObjectAbstract) {
                    SetPreview((AxoObjectAbstract) nodeInfo);
                    if (!jTextFieldObjName.hasFocus()) {
                        jTextFieldObjName.setText(((AxoObjectAbstract) nodeInfo).id.replace("\n", "<br/>"));

                    }
                }
            }
        });

        jObjectTree.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) jObjectTree.getLastSelectedPathComponent();
                    if (node != null) {
                        if (node.isLeaf()) {
                            Accept();
                            e.consume();
                        } else {
                            jObjectTree.expandPath(jObjectTree.getLeadSelectionPath());
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    Cancel();
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        jObjectTree.addMouseListener(new MouseListener() {
            private AxoObjectAbstract draggedObject = null;

            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {
                    Accept();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
                dragStarted = false;

                TreePath path = jObjectTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node != null && node.getUserObject() instanceof AxoObjectAbstract) {
                        draggedObject = (AxoObjectAbstract) node.getUserObject();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStarted) {
                    if (draggedObject != null) {
                        Point screenLoc = e.getLocationOnScreen();
                        Point finalPoint = new Point(screenLoc);
                        JPanel targetPanel = p.objectLayerPanel;
                        SwingUtilities.convertPointFromScreen(finalPoint, targetPanel);

                        p.AddObjectInstance(draggedObject, snapToGrid(finalPoint));
                        getRootPane().setCursor(Cursor.getDefaultCursor());
                        jTextFieldObjName.setText("");
                        accepted = false;
                        draggedObject = null;
                    }
                }
                dragStartPoint = null;
                dragStarted = false;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        jObjectTree.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {
                    int dx = Math.abs(e.getX() - dragStartPoint.x);
                    int dy = Math.abs(e.getY() - dragStartPoint.y);
                    int dragThreshold = 10;

                    if (dx > dragThreshold || dy > dragThreshold) {
                        dragStarted = true;
                        getRootPane().setCursor(new Cursor(Cursor.HAND_CURSOR));
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }
        });

        jResultList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Object o = ObjectSearchFrame.this.jResultList.getSelectedValue();
                if (o instanceof AxoObjectAbstract) {
                    SetPreview((AxoObjectAbstract) o);
                    if (!jObjectTree.hasFocus()) {
                        ExpandJTreeToEl((AxoObjectAbstract) o);
                    }
                    if (!jTextFieldObjName.hasFocus()) {
                        jTextFieldObjName.setText(((AxoObjectAbstract) o).id);
                    }
                } else if (o == null) {
                } else {
                    System.out.println("Different obj?" + o.toString());
                }
            }
        });

        jResultList.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    Accept();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    type = null;
                    Cancel();
                    e.consume();
                }
                else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    jResultList.clearSelection();
                    if (jTextFieldObjName != null) {
                        String str = jTextFieldObjName.getText();
                        if (str.length() > 0) {
                            /* Emulate letter deletion */
                            jTextFieldObjName.setText(str.substring(0, str.length()-1));
                        }
                    }
                    jTextFieldObjName.grabFocus();
                    e.consume();
                }
                else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    jResultList.clearSelection();
                    if (jTextFieldObjName != null) {
                        String str = jTextFieldObjName.getText();
                        if (str.length() > 0) {
                            /* Emulate arrow left */
                            jTextFieldObjName.setCaretPosition(str.length()-1);
                        }
                    }
                    jTextFieldObjName.grabFocus();
                    e.consume();

                }
                else if (e.getKeyCode() == KeyEvent.VK_UP && jResultList.getSelectedIndex() == 0) {
                    jResultList.clearSelection();
                    jTextFieldObjName.grabFocus();
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) { }

            @Override
            public void keyReleased(KeyEvent e) { }
        });

        jResultList.addMouseListener(new MouseListener() {
            private AxoObjectAbstract draggedObject = null;

            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 2) && (e.getButton() == MouseEvent.BUTTON1)) {
                    Accept();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
                dragStarted = false;

                dragStartIndex = jResultList.locationToIndex(e.getPoint());
                Object selectedObject = jResultList.getSelectedValue();
                if (selectedObject instanceof AxoObjectAbstract) {
                    draggedObject = (AxoObjectAbstract) selectedObject;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStarted) {
                    if (draggedObject != null) {
                        Point screenLoc = e.getLocationOnScreen();
                        Point finalPoint = new Point(screenLoc);
                        JPanel targetPanel = p.objectLayerPanel;
                        SwingUtilities.convertPointFromScreen(finalPoint, targetPanel);
                        
                        p.AddObjectInstance(draggedObject, snapToGrid(finalPoint));
                        getRootPane().setCursor(Cursor.getDefaultCursor());
                        jTextFieldObjName.setText("");
                        accepted = false;
                        draggedObject = null;
                    }
                }
                dragStartPoint = null;
                dragStarted = false;
                dragStartIndex = -1;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        jResultList.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint != null) {
                    int dx = Math.abs(e.getX() - dragStartPoint.x);
                    int dy = Math.abs(e.getY() - dragStartPoint.y);
                    int dragThreshold = 10;

                    if (dx > dragThreshold || dy > dragThreshold) {
                        dragStarted = true;
                        getRootPane().setCursor(new Cursor(Cursor.HAND_CURSOR));
                    }
                }

                /* Prevent unwanted scrolling while dragging */
                if (dragStarted) {
                    if (dragStartIndex != -1) {
                        jResultList.setSelectedIndex(dragStartIndex);
                    }
                    e.consume();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }
        });

        jPanelObjectPreview.setVisible(true);
        jScrollPaneObjectTree.setVisible(true);
        jScrollPaneObjectInfo.setVisible(true);
        jScrollPaneObjectPreview.setVisible(true);
        jSplitPaneMain.setVisible(true);
        jSplitPaneRight.setVisible(true);
        jTextPaneObjectInfo.setVisible(true);
        jTextPaneObjectInfo.setContentType("text/html");

        jTextFieldObjName.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) { }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    Accept();
                    e.consume();
                }
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    type = null;
                    Cancel();
                    e.consume();
                }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    jResultList.grabFocus();
                    jResultList.setSelectedIndex(0);
                    jResultList.ensureIndexIsVisible(0);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                Search(jTextFieldObjName.getText());
            }
        });
    }

    private Point snapToGrid(Point p) {
        p.x = Constants.X_GRID * (p.x / Constants.X_GRID);
        p.y = Constants.Y_GRID * (p.y / Constants.Y_GRID);
        return p;
    }

    private Point clipToStayWithinScreen(Point patchLoc) {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Rectangle allScreenBounds = new Rectangle();

        for(GraphicsDevice curGs : gs) {
            GraphicsConfiguration[] gc = curGs.getConfigurations();
            for(GraphicsConfiguration curGc : gc) {
                Rectangle bounds = curGc.getBounds();
                allScreenBounds = allScreenBounds.union(bounds);
            }
        }

        Point patchFrameOnScreen = p.getPatchframe().patch.objectLayerPanel.getLocationOnScreen();

        if(patchFrameOnScreen.getX() + patchLoc.getX() + getWidth() > allScreenBounds.getWidth() + allScreenBounds.getX()) {
            patchLoc.x = (int) (allScreenBounds.getWidth() + allScreenBounds.getX() - patchFrameOnScreen.getX() - getWidth());
        }
        if(patchFrameOnScreen.getY() + patchLoc.getY() + getHeight() > allScreenBounds.getHeight() + allScreenBounds.getY()) {
            patchLoc.y = (int) (allScreenBounds.getHeight() + allScreenBounds.getY() - patchFrameOnScreen.getY() - getHeight());
        }

        return patchLoc;
    }

    void Launch(Point patchLoc, AxoObjectInstanceAbstract o, String searchString, boolean selectSearchString) {
        if (this.objectTree != MainFrame.axoObjects.getObjectTree()) {
            DefaultMutableTreeNode root1 = new DefaultMutableTreeNode();
            this.objectTree = MainFrame.axoObjects.getObjectTree();
            this.root = PopulateJTree(MainFrame.axoObjects.getObjectTree(), root1);
            tm = new DefaultTreeModel(this.root);
            jObjectTree.setModel(tm);
        }

        MainFrame.mainframe.SetGrabFocusOnSevereErrors(false);
        accepted = false;
        snapToGrid(patchLoc);
        patchLocX = patchLoc.x;
        patchLocY = patchLoc.y;
        Point ps = p.objectLayerPanel.getLocationOnScreen();
        Point patchLocClipped = clipToStayWithinScreen(patchLoc);

        setLocation(patchLocClipped.x + ps.x, patchLocClipped.y + ps.y);
        target_object = o;
        if (o != null) {
            AxoObjectAbstract oa = o.getType();
            if (oa != null) {
                Search(oa.id);
                SetPreview(oa);
                ExpandJTreeToEl(oa);
            }
            jTextFieldObjName.setText(o.typeName);
        } else if (searchString != null) {
            Search(searchString);
            jTextFieldObjName.setText(searchString);
        }
        jTextFieldObjName.grabFocus();
        if (selectSearchString) {
            jTextFieldObjName.selectAll();
        }
        else {
            jTextFieldObjName.setSelectionStart(jTextFieldObjName.getText().length());
        }
        jTextFieldObjName.setSelectionEnd(jTextFieldObjName.getText().length());
        setVisible(true);
    }

    void SetPreview(AxoObjectAbstract o) {
        if (o == null) {
            previewObj = null;
            type = null;
            jPanelObjectPreview.removeAll();
            jPanelObjectPreview.repaint();
            jButtonAccept.setEnabled(false);
            return;
        }
        else {
            accepted = true;
            jButtonAccept.setEnabled(true);            
        }
        if (o != previewObj) {
            previewObj = o;
            type = o;
            ExpandJTreeToEl(o);
            jResultList.setSelectedValue(o, true);
            if (jResultList.getSelectedValue() != o) {
            }
            AxoObjectInstanceAbstract inst = o.CreateInstance(null, "dummy", new Point(5, 5));
            jPanelObjectPreview.removeAll();
            jPanelObjectPreview.add(inst);
            jPanelObjectPreview.setPreferredSize(inst.getPreferredSize());
            jPanelObjectPreview.revalidate();
            jPanelObjectPreview.repaint();
            AxoObjectAbstract t = inst.getType();
            if (t != null) {
                String description = t.sDescription == null || t.sDescription.isEmpty() ? o.sDescription : t.sDescription;
                String path = t.sObjFilePath == null ? o.sObjFilePath : t.sObjFilePath;
                String author = t.sAuthor == null ? o.sAuthor : t.sAuthor;
                String license = t.sLicense == null ? o.sLicense : t.sLicense;

                String txt = "";
                if ((description != null) && (!description.isEmpty())) {
                    txt += description;
                }
                if ((path != null) && (!path.isEmpty())) {
                    txt += "<p>Path: " + path;
                }
                if ((author != null) && (!author.isEmpty())) {
                    txt += "<p>Author: " + author;
                }
                if ((license != null) && (!license.isEmpty())) {
                    txt += "<p>License: " + license;
                }
                jTextPaneObjectInfo.setText(txt.replace("\n", "<br/>"));
            }
            jTextPaneObjectInfo.setCaretPosition(0);
        }
    }

    static DefaultMutableTreeNode PopulateJTree(AxoObjectTreeNode anode, DefaultMutableTreeNode root) {
        for (String n : anode.SubNodes.keySet()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(anode.SubNodes.get(n));
            root.add(PopulateJTree(anode.SubNodes.get(n), node));
        }
        for (AxoObjectAbstract n : anode.Objects) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(n);
            root.add(node);
        }
        return root;
    }

    void ExpandJTreeToEl(AxoObjectAbstract aoa) {
        Enumeration e = root.depthFirstEnumeration();
        DefaultMutableTreeNode n = null;
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) o;
            if (aoa.equals(dmtn.getUserObject())) {
                n = (DefaultMutableTreeNode) o;
                break;
            }
        }
        if (n != null) {
            if ((jObjectTree.getSelectionPath() != null) && (jObjectTree.getSelectionPath().getLastPathComponent() != n)) {
                jObjectTree.scrollPathToVisible(new TreePath(n.getPath()));
                jObjectTree.setSelectionPath(new TreePath(n.getPath()));
            }
        } else {
            jObjectTree.clearSelection();
        }
    }

    boolean containsUpperCase(String str) {
        for (int i=0; i < str.length(); i++) {
            Character c = str.charAt(i);
            if (Character.getType(c) == Character.UPPERCASE_LETTER) {
                return true;
            }
        }
        return false;
    }

    public void Search(String s) {
        ArrayList<AxoObjectAbstract> listData = new ArrayList<AxoObjectAbstract>();
        // ArrayList<AxoObjectAbstract> tempList = new ArrayList<AxoObjectAbstract>();

        /* --- if search field is empty, show complete list --- */
        if ((s == null) || s.isEmpty()) {
            for (AxoObjectAbstract o : MainFrame.axoObjects.ObjectList) {
                /* add complete list */
                listData.add(o);
            }
            /* Add all to list - don't sort */
            jResultList.setListData(listData.toArray());
        }
        else {

            boolean caseSensitiveSearch = containsUpperCase(s);

            for (AxoObjectAbstract o : MainFrame.axoObjects.ObjectList) {
                boolean matches = false;
                if (caseSensitiveSearch) {
                    if (o.id.equals(s) || o.id.startsWith(s) || o.id.contains(s) || (o.sDescription != null && o.sDescription.contains(s))) {
                        matches = true;
                    }
                    String rgx = s.replace("*", ".*");
                    if (!s.startsWith("^")) rgx = ".*" + rgx;
                    if (!s.endsWith("$")) rgx = rgx + ".*";
                    try {
                        if (Pattern.matches(rgx, o.id)) {
                            matches = true;
                        }
                    }
                    catch (PatternSyntaxException e) {
                        e.printStackTrace(System.out);
                    }
                }
                else {
                    String lower_oId = o.id.toLowerCase();
                    String lower_s = s.toLowerCase();
                    if (lower_oId.equals(lower_s) || lower_oId.startsWith(lower_s) || lower_oId.contains(lower_s) || (o.sDescription != null && o.sDescription.toLowerCase().contains(lower_s))) {
                        matches = true;
                    }
                    String rgx = lower_s.replace("*", ".*");
                    if (!lower_s.startsWith("^")) rgx = ".*" + rgx;
                    if (!lower_s.endsWith("$")) rgx = rgx + ".*";
                    try {
                        if (Pattern.matches(rgx, lower_oId)) {
                            matches = true;
                        }
                    }
                    catch (PatternSyntaxException e) {
                        e.printStackTrace(System.out);
                    }
                }

                if (matches && !listData.contains(o)) {
                    listData.add(o);
                }
            }

            Collections.sort(listData, new AxoObjectIdComparator(s, caseSensitiveSearch));
            jResultList.setListData(listData.toArray());
        }

        if (!listData.isEmpty()) {
            type = listData.get(0);
            jResultList.setSelectedIndex(0);
            jResultList.ensureIndexIsVisible(0);
            ExpandJTreeToEl(listData.get(0));
            SetPreview(type);
        }
        else {
            ArrayList<AxoObjectAbstract> objs = MainFrame.axoObjects.GetAxoObjectFromName(s, p.GetCurrentWorkingDirectory());
            if ((objs != null) && (objs.size() > 0)) {
                jResultList.setListData(objs.toArray());
                SetPreview(objs.get(0));
            }
        }

        /* show number of results */
        this.setTitle(String.format("    %d found", listData.size()));
    }

    void Cancel() {
        accepted = false;
        MainFrame.mainframe.SetGrabFocusOnSevereErrors(true);
        setVisible(false);
        p.repaint();
    }

    void Accept() {
        MainFrame.mainframe.SetGrabFocusOnSevereErrors(true);
        setVisible(false);
        AxoObjectAbstract x = type;
        if (x == null) {
            ArrayList<AxoObjectAbstract> objs = MainFrame.axoObjects.GetAxoObjectFromName(jTextFieldObjName.getText(), p.GetCurrentWorkingDirectory());
            if ((objs != null) && (!objs.isEmpty())) {
                x = objs.get(0);
                jTextFieldObjName.setText("");
            }
        }
        if (x != null) {
            if (target_object == null) {
                p.AddObjectInstance(x, new Point(patchLocX, patchLocY));
            } else {
                p.ChangeObjectInstanceType(target_object, x);
                p.cleanUpIntermediateChangeStates(2);
            }
        }
        setVisible(false);
        p.repaint();
        accepted = false;
    }

    private void initComponents() {

        jSplitPaneMain = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT, true);
        jSplitPaneLeft = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, true);
        jSplitPaneRight = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, true);

        jPanelLeft = new javax.swing.JPanel();
        jPanelMain = new javax.swing.JPanel();
        jPanelSearchField = new javax.swing.JPanel();

        jTextPaneObjectInfo = new javax.swing.JTextPane();
        jTextFieldObjName = new javax.swing.JTextField();
        jButtonAccept = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        jResultList = new JList<AxoObjectAbstract>() {
            /*
             * Overrides scrollRectToVisible to do nothing. This prevents the
             * JScrollPane from automatically scrolling when the user is
             * dragging an item in the list
             */
            @Override
            public void scrollRectToVisible(Rectangle rect) {
            }
        };

        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));

        jObjectTree = new javax.swing.JTree();

        jScrollPaneObjectTree = new ScrollPaneComponent();
        jScrollPaneObjectSearch = new ScrollPaneComponent();
        jScrollPaneObjectInfo = new ScrollPaneComponent();
        jScrollPaneObjectPreview = new ScrollPaneComponent();
        jPanelObjectPreview = new javax.swing.JPanel();

        setForeground(java.awt.SystemColor.controlText);
        setIconImages(null);
        setName(""); // NOI18N
        setUndecorated(true);
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                formWindowLostFocus(evt);
            }
        });

        jPanelMain.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        jPanelMain.setLayout(new java.awt.BorderLayout());

        jSplitPaneMain.setDividerLocation(220);
        jSplitPaneMain.setMinimumSize(new java.awt.Dimension(120, 60));
        jSplitPaneMain.setPreferredSize(new java.awt.Dimension(640, 400));

        jPanelLeft.setAlignmentX(LEFT_ALIGNMENT);
        jPanelLeft.setAlignmentY(TOP_ALIGNMENT);
        jPanelLeft.setLayout(new javax.swing.BoxLayout(jPanelLeft, javax.swing.BoxLayout.PAGE_AXIS));

        jPanelSearchField.setAlignmentY(TOP_ALIGNMENT);
        jPanelSearchField.setLayout(new javax.swing.BoxLayout(jPanelSearchField, javax.swing.BoxLayout.LINE_AXIS));

        jTextFieldObjName.setAlignmentX(LEFT_ALIGNMENT);
        jTextFieldObjName.setMaximumSize(new java.awt.Dimension(2147483647, 30));
        // jTextFieldObjName.setMinimumSize(new java.awt.Dimension(40, 20));
        jTextFieldObjName.setPreferredSize(new java.awt.Dimension(600, 30));
        jTextFieldObjName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldObjNameActionPerformed(evt);
            }
        });
        jPanelSearchField.add(jTextFieldObjName);
        jPanelSearchField.add(filler1);

        jButtonAccept.setText("✔");
        jButtonAccept.setToolTipText("Accept");
        jButtonAccept.setActionCommand("");
        jButtonAccept.setDefaultCapable(false);
        jButtonAccept.setFocusable(false);
        jButtonAccept.setMargin(new java.awt.Insets(4, 0, 4, 0));
        jButtonAccept.setMinimumSize(new java.awt.Dimension(30, 30));
        jButtonAccept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAcceptActionPerformed(evt);
            }
        });
        jPanelSearchField.add(jButtonAccept);
        jPanelSearchField.add(filler2);

        jButtonCancel.setText("✖");
        jButtonCancel.setToolTipText("Cancel");
        jButtonCancel.setActionCommand("");
        jButtonCancel.setDefaultCapable(false);
        jButtonCancel.setFocusable(false);
        jButtonCancel.setMargin(new java.awt.Insets(4, 0, 4, 0));
        jButtonCancel.setMinimumSize(new java.awt.Dimension(30, 30));
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });
        jPanelSearchField.add(jButtonCancel);
        jPanelLeft.add(jPanelSearchField);

        jSplitPaneLeft.setResizeWeight(0.5);
        jSplitPaneLeft.setAlignmentX(CENTER_ALIGNMENT);
        jSplitPaneLeft.setAlignmentY(BOTTOM_ALIGNMENT);
        jSplitPaneLeft.setMinimumSize(new java.awt.Dimension(80, 120));
        jSplitPaneLeft.setPreferredSize(new java.awt.Dimension(180, 160));

        jScrollPaneObjectSearch.setMinimumSize(new java.awt.Dimension(24, 64));
        jScrollPaneObjectSearch.setPreferredSize(new java.awt.Dimension(180, 160));

        jResultList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jResultList.setAutoscrolls(false);
        jResultList.setDragEnabled(false);
        jResultList.setTransferHandler(null);
        jResultList.setAlignmentX(LEFT_ALIGNMENT);
        jResultList.setMinimumSize(new java.awt.Dimension(100, 50));
        jResultList.setVisibleRowCount(6);
        jScrollPaneObjectSearch.setViewportView(jResultList);

        jSplitPaneLeft.setTopComponent(jScrollPaneObjectSearch);

        jScrollPaneObjectTree.setPreferredSize(new java.awt.Dimension(180, 160));

        jObjectTree.setAlignmentX(LEFT_ALIGNMENT);
        jObjectTree.setAutoscrolls(false);
        jObjectTree.setDragEnabled(false);
        jObjectTree.setTransferHandler(null);
        jObjectTree.setMinimumSize(new java.awt.Dimension(100, 50));
        jObjectTree.setRootVisible(false);
        jObjectTree.setShowsRootHandles(true);
        jScrollPaneObjectTree.setViewportView(jObjectTree);

        jSplitPaneLeft.setBottomComponent(jScrollPaneObjectTree);

        jPanelLeft.add(jSplitPaneLeft);

        jSplitPaneMain.setLeftComponent(jPanelLeft);

        jSplitPaneRight.setDividerLocation(207);
        jSplitPaneRight.setResizeWeight(0.5);
        jSplitPaneRight.setPreferredSize(new java.awt.Dimension(300, 240));

        jScrollPaneObjectInfo.setMinimumSize(new java.awt.Dimension(6, 120));

        jTextPaneObjectInfo.setEditable(false);
        jTextPaneObjectInfo.setFocusCycleRoot(false);
        jTextPaneObjectInfo.setFocusable(false);
        jTextPaneObjectInfo.setRequestFocusEnabled(false);
        jScrollPaneObjectInfo.setViewportView(jTextPaneObjectInfo);

        jSplitPaneRight.setTopComponent(jScrollPaneObjectInfo);

        jPanelObjectPreview.setBackground(Theme.Patch_Unlocked_Background);
        jPanelObjectPreview.setEnabled(false);
        jPanelObjectPreview.setFocusable(false);

        jScrollPaneObjectPreview.setMinimumSize(new java.awt.Dimension(6, 120));
        jScrollPaneObjectPreview.setViewportView(jPanelObjectPreview);

        javax.swing.GroupLayout jPanelObjectPreviewLayout = new javax.swing.GroupLayout(jPanelObjectPreview);
        jPanelObjectPreview.setLayout(jPanelObjectPreviewLayout);
        jPanelObjectPreviewLayout.setHorizontalGroup(
            jPanelObjectPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanelObjectPreviewLayout.setVerticalGroup(
            jPanelObjectPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jSplitPaneRight.setRightComponent(jScrollPaneObjectPreview);
        jSplitPaneMain.setRightComponent(jSplitPaneRight);
        jPanelMain.add(jSplitPaneMain, java.awt.BorderLayout.CENTER);
        getContentPane().add(jPanelMain, java.awt.BorderLayout.CENTER);

        pack();
    }

    private void formWindowLostFocus(java.awt.event.WindowEvent evt) {
        getRootPane().setCursor(Cursor.getDefaultCursor());
        if ((evt.getOppositeWindow() == null)
                || !(evt.getOppositeWindow() instanceof axoloti.PatchFrame)) {
            Cancel();
        } else {
            if (accepted) {
                Accept();
            } else {
                Cancel();
            }
        }
    }

    private void jTextFieldObjNameActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {
        Cancel();
    }

    private void jButtonAcceptActionPerformed(java.awt.event.ActionEvent evt) {
        Accept();
    }
}
