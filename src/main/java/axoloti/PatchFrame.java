/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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

import axoloti.dialogs.KeyboardNavigableOptionPane;
import axoloti.object.AxoObjectInstanceAbstract;
import axoloti.object.AxoObjectInstancePatcher;
import axoloti.utils.KeyUtils;
import axoloti.utils.OSDetect.OS;
import axoloti.utils.Preferences;
import components.PresetPanel;
import components.ScrollPaneComponent;
import components.VisibleCablePanel;

import static axoloti.MainFrame.fc;
import static axoloti.MainFrame.mainframe;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultEditorKit;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

import qcmds.QCmdLock;
import qcmds.QCmdProcessor;
import qcmds.QCmdStart;
import qcmds.QCmdStop;
import qcmds.QCmdUploadPatch;

/**
 *
 * @author Johannes Taelman
 */
public class PatchFrame extends javax.swing.JFrame implements DocumentWindow, ConnectionStatusListener, SDCardMountStatusListener,  BoardIDNameListener {

    private static final Logger LOGGER = Logger.getLogger(PatchFrame.class.getName());
    /**
     * Creates new form PatchFrame
     */
    PatchGUI patch;

    private PresetPanel presetPanel;
    private VisibleCablePanel visibleCablePanel;
    QCmdProcessor qcmdprocessor;
    ArrayList<DocumentWindow> dwl;

    private boolean previousOverload;
    private boolean firstShow = true;

    private axoloti.menus.FileMenu fileMenuP;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private axoloti.menus.HelpMenu helpMenu1;
    private javax.swing.JToggleButton jToggleButtonLive;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemCordsInBackground;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemLive;
    private javax.swing.JLabel jLabelDSPLoad;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuClose;
    private javax.swing.JMenuItem jMenuCompileCode;
    private javax.swing.JMenu jMenuEdit;
    private javax.swing.JMenuItem jMenuGenerateAndCompileCode;
    private javax.swing.JMenuItem jMenuGenerateCode;
    private javax.swing.JMenuItem jMenuItemAddObj;
    private javax.swing.JMenuItem jMenuItemAdjScroll;
    private javax.swing.JMenuItem jMenuItemClearPreset;
    private javax.swing.JMenuItem jMenuItemDuplicate;
    private javax.swing.JMenuItem jMenuItemDelete;
    private javax.swing.JMenuItem jMenuItemDifferenceToPreset;
    private javax.swing.JMenuItem jMenuItemLock;
    private javax.swing.JMenuItem jMenuItemNotes;
    private javax.swing.JMenuItem jMenuItemPresetCurrentToInit;
    private javax.swing.JMenuItem jMenuItemSelectAll;
    private javax.swing.JMenuItem jMenuItemSettings;
    private javax.swing.JMenuItem jMenuItemOpenFileLocation;
    private javax.swing.JMenuItem jMenuItemUnlock;
    private javax.swing.JMenuItem jMenuItemUploadInternalFlash;
    private javax.swing.JMenuItem jMenuItemUploadSD;
    private javax.swing.JMenuItem jMenuItemUploadSDStart;
    private javax.swing.JMenu jMenuPatch;
    private javax.swing.JMenu jMenuPreset;
    private javax.swing.JMenuItem jMenuSave;
    private javax.swing.JMenuItem jMenuSaveAs;
    private javax.swing.JMenuItem jMenuSaveClip;
    private javax.swing.JMenuItem jMenuSaveCopy;
    private javax.swing.JMenuItem jMenuUploadCode;
    private javax.swing.JMenu jMenuView;
    private javax.swing.JProgressBar jProgressBarDSPLoad;
    private javax.swing.JLabel jLabelBoardIDName;
    private ScrollPaneComponent jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPanel jToolbarPanel;
    private javax.swing.JMenuItem redoItem;
    private javax.swing.JMenuItem undoItem;
    private axoloti.menus.WindowMenu windowMenu1;

    public PatchFrame(final PatchGUI patch, QCmdProcessor qcmdprocessor) {

        try {
            MainFrame.axoObjects.LoaderThread.join(); /* Make sure all object libraries are loaded before creating/opening a patch */
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

        setMinimumSize(new Dimension(200,120));
        setIconImage(new ImageIcon(getClass().getResource("/resources/ksoloti_icon_axp.png")).getImage());
        this.qcmdprocessor = qcmdprocessor;
        this.dwl = new ArrayList<DocumentWindow>();

        initComponents();
        jScrollPane1.setVisible(false);
        fileMenuP.initComponents();
        this.patch = patch;
        this.patch.patchframe = this;

        presetPanel = new PresetPanel(patch);
        visibleCablePanel = new VisibleCablePanel(patch);

        Dimension di = new Dimension(10,0);

        jToolbarPanel.add(new Box.Filler(di, di, new Dimension(32767, 32767)));
        jToolbarPanel.add(presetPanel);
        if (patch.settings != null) {
            presetPanel.setVisible(patch.settings.GetNPresets() > 0);
        }
        else {
            presetPanel.setVisible(false);
        }

        jToolbarPanel.add(new Box.Filler(di, di, new Dimension(32767, 32767)));
        jToolbarPanel.add(visibleCablePanel);

        jScrollPane1.setViewportView(patch.Layers);

        JMenuItem menuItem = new JMenuItem(new DefaultEditorKit.CutAction());
        menuItem.setMnemonic('X');
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, 
                KeyUtils.CONTROL_OR_CMD_MASK));
        menuItem.setText("Cut");
        jMenuEdit.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Patch p = patch.GetSelectedObjects();
                if (p.objectInstances.isEmpty()) {
                    getToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
                    return;
                }
                p.PreSerialize();
                Serializer serializer = new Persister(new Format(2));
                try {
                    Clipboard clip = getToolkit().getSystemClipboard();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    serializer.write(p, baos);
                    StringSelection s = new StringSelection(baos.toString());
                    clip.setContents(s, (ClipboardOwner) null);
                    patch.deleteSelectedAxoObjInstances();
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        });

        menuItem = new JMenuItem(new DefaultEditorKit.CopyAction());
        menuItem.setMnemonic('C');
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 
                KeyUtils.CONTROL_OR_CMD_MASK));
        menuItem.setText("Copy");
        jMenuEdit.add(menuItem);

        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Patch p = patch.GetSelectedObjects();
                if (p.objectInstances.isEmpty()) {
                    getToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
                    return;
                }
                p.PreSerialize();
                Serializer serializer = new Persister(new Format(2));
                try {
                    Clipboard clip = getToolkit().getSystemClipboard();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    serializer.write(p, baos);
                    StringSelection s = new StringSelection(baos.toString());
                    clip.setContents(s, (ClipboardOwner) null);
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        });
        menuItem = new JMenuItem(new DefaultEditorKit.PasteAction());
        menuItem.setMnemonic('V');
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, 
                KeyUtils.CONTROL_OR_CMD_MASK));
        menuItem.setText("Paste");
        jMenuEdit.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard clip = getToolkit().getSystemClipboard();
                try {
                    patch.paste((String) clip.getData(DataFlavor.stringFlavor), null, false);
                }
                catch (UnsupportedFlavorException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
                catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        });

        if (patch.getWindowPos() != null) {
            setBounds(patch.getWindowPos());
        }
        else {
            Dimension d = patch.GetInitialSize();
            setSize(d);
        }

        if (!Preferences.getInstance().getExpertMode()) {
            jSeparator6.setVisible(false);
            jMenuItemLock.setVisible(false);
            jMenuGenerateAndCompileCode.setVisible(false);
            jMenuGenerateCode.setVisible(false);
            jMenuCompileCode.setVisible(false);
            jMenuUploadCode.setVisible(false);
            jMenuItemLock.setVisible(false);
            jMenuItemUnlock.setVisible(false);
        }
        jMenuPreset.setVisible(false);
        jMenuItemAdjScroll.setVisible(true);
        patch.Layers.requestFocus();
        if (USBBulkConnection.GetConnection().isConnected()) {
            ShowConnect();
        }

        this.undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 
                KeyUtils.CONTROL_OR_CMD_MASK));
        this.redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 
                KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
        this.redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, 
                KeyUtils.CONTROL_OR_CMD_MASK));

        createBufferStrategy(2);
        USBBulkConnection.GetConnection().addConnectionStatusListener(this);
        USBBulkConnection.GetConnection().addSDCardMountStatusListener(this);
        USBBulkConnection.GetConnection().addBoardIDNameListener(this);
    }

    public void repositionIfOutsideScreen() {

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

        Point patchFrameOnScreen = getLocationOnScreen();
        double safetyMargin = 32.0;

        if(patchFrameOnScreen.getX() > (allScreenBounds.getMaxX() - safetyMargin) ||
           patchFrameOnScreen.getY() > (allScreenBounds.getMaxY() - safetyMargin) ||
           patchFrameOnScreen.getY() < (allScreenBounds.getMinY() + safetyMargin) ||
           patchFrameOnScreen.getY() < (allScreenBounds.getMinY() + safetyMargin)) {
            /* Do not tolerate any coordinate to be outside all
             * screen boundaries (with safety margin). Reset position instead.
             */
            setLocationRelativeTo(mainframe);
        }
    }

    private void handleLiveAction(boolean selected) {

        if (mainframe == null || patch == null || qcmdprocessor == null) {
            System.err.println(Instant.now() + " [ERROR] Cannot perform live action: Core components are missing.");
            return;
        }

        /* Disable both components immediately to prevent further clicks */
        jToggleButtonLive.setEnabled(false);
        jCheckBoxMenuItemLive.setEnabled(false);

        if (selected) { /* Signal mainframe that this patch will be the current live one */
            mainframe.setCurrentLivePatch(patch);
        }
        else {
            mainframe.setCurrentLivePatch(null);
        }

        if (selected) { /* Go Live action */
            new SwingWorker<Boolean, String>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try {
                        PatchGUI previouslyLive = mainframe.getCurrentLivePatch();
                        if (previouslyLive != null && previouslyLive != patch) {
                            qcmdprocessor.AppendToQueue(new QCmdStop());
                            qcmdprocessor.WaitQueueFinished();
                            previouslyLive.Unlock();
                        }
                        patch.GoLive();
                        return true;
                    }
                    catch (Exception e) {
                        return false;
                    }
                }

                @Override
                protected void done() {
                    try {
                        boolean success = get();

                        if (success) {
                            mainframe.setCurrentLivePatch(patch);
                            patch.Lock();
                        }
                        else {
                            mainframe.setCurrentLivePatch(null);
                            patch.Unlock();
                        }

                    }
                    catch (Exception e) {
                        /* If an exception occurs in get(), assume failure */
                        mainframe.setCurrentLivePatch(null);
                        patch.Unlock();

                    }
                    finally {
                        jToggleButtonLive.setEnabled(true);
                        jCheckBoxMenuItemLive.setEnabled(true);
                    }
                }
            }.execute();
        }
        else { /* Stop Live action */
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    qcmdprocessor.AppendToQueue(new QCmdStop());
                    qcmdprocessor.WaitQueueFinished(); // Wait for the stop command to complete
                    return null;
                }

                @Override
                protected void done() {
                    mainframe.setCurrentLivePatch(null); /* Signal mainframe that currently no patch is live */
                    patch.Unlock();
                    jToggleButtonLive.setEnabled(true);
                    jCheckBoxMenuItemLive.setEnabled(true);
                }
            }.execute();
        }
    }

    public void SetLive(boolean b) {
        jToggleButtonLive.setSelected(b);
        jCheckBoxMenuItemLive.setSelected(b);
        presetPanel.ShowLive(b);
    }

    public void showPresetPanel(boolean show) {
        presetPanel.setVisible(show);
    }

    void ShowConnectDisconnect(boolean status) {
        jToggleButtonLive.setEnabled(status);
        jCheckBoxMenuItemLive.setEnabled(status);
        jMenuItemUploadInternalFlash.setEnabled(status);
        jMenuItemUploadSD.setEnabled(status);
        jMenuItemUploadSDStart.setEnabled(status);
    }

    @Override
    public void ShowDisconnect() {
        if (patch.IsLocked()) {
            patch.Unlock();
        }
        SetLive(false);
        ShowConnectDisconnect(false);
    }

    @Override
    public void ShowConnect() {
        patch.Unlock();
        SetLive(false);
        ShowConnectDisconnect(true);
    }

    public void ShowCompileFail() {
        jToggleButtonLive.setSelected(false);
        jToggleButtonLive.setEnabled(true);
    }

    public void Close() {
        DocumentWindowList.UnregisterWindow(this);
        USBBulkConnection.GetConnection().removeConnectionStatusListener(this);
        USBBulkConnection.GetConnection().removeSDCardMountStatusListener(this);
        USBBulkConnection.GetConnection().removeBoardIDNameListener(this);
        patch.Close();
        super.dispose();
    }

    @Override
    public boolean AskClose() {
        if (patch.isDirty() && patch.getContainer() == null) {
            Object[] options = {"Save",
                "Discard",
                "Cancel"};
            int n = KeyboardNavigableOptionPane.showOptionDialog(
                    this,
                    "Save changes to \"" + patch.getFileNamePath() + "\"?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[2]);
            switch (n) {
                case JOptionPane.YES_OPTION:
                    jMenuSaveActionPerformed(null);
                    Close();
                    return false;
                case JOptionPane.NO_OPTION:
                    Close();
                    return false;
                case JOptionPane.CANCEL_OPTION:
                    return true;
                default:
                    return false;
            }
        }
        else {
            Close();
            return false;
        }
    }

    public ScrollPaneComponent getScrollPane() {
        return this.jScrollPane1;
    }

    private void initComponents() {

        jToolbarPanel = new javax.swing.JPanel();
        jToggleButtonLive = new javax.swing.JToggleButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0));
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0));
        jLabelDSPLoad = new javax.swing.JLabel();
        jProgressBarDSPLoad = new javax.swing.JProgressBar();
        jLabelBoardIDName = new javax.swing.JLabel(" USB");
        jScrollPane1 = new ScrollPaneComponent();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenuP = new axoloti.menus.FileMenu();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        jMenuSave = new javax.swing.JMenuItem();
        jMenuSaveAs = new javax.swing.JMenuItem();
        jMenuSaveCopy = new javax.swing.JMenuItem();
        jMenuSaveClip = new javax.swing.JMenuItem();
        jMenuClose = new javax.swing.JMenuItem();
        jMenuEdit = new javax.swing.JMenu();
        undoItem = new javax.swing.JMenuItem();
        redoItem = new javax.swing.JMenuItem();
        jMenuItemDuplicate = new javax.swing.JMenuItem();
        jMenuItemDelete = new javax.swing.JMenuItem();
        jMenuItemSelectAll = new javax.swing.JMenuItem();
        jMenuItemAddObj = new javax.swing.JMenuItem();
        jMenuView = new javax.swing.JMenu();
        jMenuItemNotes = new javax.swing.JMenuItem();
        jMenuItemSettings = new javax.swing.JMenuItem();
        jMenuItemOpenFileLocation = new javax.swing.JMenuItem();
        jCheckBoxMenuItemCordsInBackground = new javax.swing.JCheckBoxMenuItem();
        jMenuItemAdjScroll = new javax.swing.JMenuItem();
        jMenuPatch = new javax.swing.JMenu();
        jCheckBoxMenuItemLive = new javax.swing.JCheckBoxMenuItem();
        jMenuItemUploadSD = new javax.swing.JMenuItem();
        jMenuItemUploadSDStart = new javax.swing.JMenuItem();
        jMenuItemUploadInternalFlash = new javax.swing.JMenuItem();
        jMenuGenerateAndCompileCode = new javax.swing.JMenuItem();
        jMenuGenerateCode = new javax.swing.JMenuItem();
        jMenuCompileCode = new javax.swing.JMenuItem();
        jMenuUploadCode = new javax.swing.JMenuItem();
        jMenuItemLock = new javax.swing.JMenuItem();
        jMenuItemUnlock = new javax.swing.JMenuItem();
        jMenuPreset = new javax.swing.JMenu();
        jMenuItemClearPreset = new javax.swing.JMenuItem();
        jMenuItemPresetCurrentToInit = new javax.swing.JMenuItem();
        jMenuItemDifferenceToPreset = new javax.swing.JMenuItem();
        windowMenu1 = new axoloti.menus.WindowMenu();
        helpMenu1 = new axoloti.menus.HelpMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                formComponentHidden(evt);
            }
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                formWindowLostFocus(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jToolbarPanel.setAlignmentX(RIGHT_ALIGNMENT);
        jToolbarPanel.setAlignmentY(TOP_ALIGNMENT);
        jToolbarPanel.setMaximumSize(new java.awt.Dimension(32767, 0));
        jToolbarPanel.setPreferredSize(new java.awt.Dimension(212, 49));
        jToolbarPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5));
        jToolbarPanel.setLayout(new javax.swing.BoxLayout(jToolbarPanel, javax.swing.BoxLayout.LINE_AXIS));

        Dimension btndim = new Dimension(110,30);
        jToggleButtonLive.setText("Live");
        jToggleButtonLive.setMinimumSize(btndim);
        jToggleButtonLive.setMaximumSize(btndim);
        jToggleButtonLive.setPreferredSize(btndim);
        jToggleButtonLive.setEnabled(false);
        jToggleButtonLive.setFocusable(false);
        jToggleButtonLive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonLiveActionPerformed(evt);
            }
        });
        jToolbarPanel.add(jToggleButtonLive);

        filler1.setAlignmentX(LEFT_ALIGNMENT);
        jToolbarPanel.add(filler1);

        jLabelDSPLoad.setText("DSP Load ");
        jToolbarPanel.add(jLabelDSPLoad);

        jProgressBarDSPLoad.setToolTipText("CPU load of currently running patch");
        jProgressBarDSPLoad.setAlignmentX(LEFT_ALIGNMENT);
        jProgressBarDSPLoad.setMaximumSize(new java.awt.Dimension(200, 18));
        jProgressBarDSPLoad.setMinimumSize(new java.awt.Dimension(60, 18));
        jProgressBarDSPLoad.setPreferredSize(new java.awt.Dimension(200, 18));
        jProgressBarDSPLoad.setName(""); // NOI18N
        jProgressBarDSPLoad.setMaximum(200);
        jProgressBarDSPLoad.setStringPainted(true);
        jToolbarPanel.add(jProgressBarDSPLoad);

        filler2.setAlignmentX(LEFT_ALIGNMENT);
        jToolbarPanel.add(filler2);

        jToolbarPanel.add(jLabelBoardIDName);

        filler3.setAlignmentX(LEFT_ALIGNMENT);
        jToolbarPanel.add(filler3);

        getContentPane().add(jToolbarPanel);

        jScrollPane1.setAutoscrolls(true);
        getContentPane().add(jScrollPane1);
        
        fileMenuP.setMnemonic('F');
        fileMenuP.setText("File");
        fileMenuP.add(jSeparator1);

        jMenuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuSave.setText("Save");
        jMenuSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveActionPerformed(evt);
            }
        });
        fileMenuP.add(jMenuSave);

        jMenuSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
        jMenuSaveAs.setText("Save As...");
        jMenuSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveAsActionPerformed(evt);
            }
        });
        fileMenuP.add(jMenuSaveAs);

        jMenuSaveCopy.setText("Save a Copy...");
        jMenuSaveCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveCopyActionPerformed(evt);
            }
        });
        fileMenuP.add(jMenuSaveCopy);

        jMenuSaveClip.setText("Copy to Clipboard");
        jMenuSaveClip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveClipActionPerformed(evt);
            }
        });
        fileMenuP.add(jMenuSaveClip);

        jMenuClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuClose.setText("Close");
        jMenuClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCloseActionPerformed(evt);
            }
        });
        fileMenuP.add(jMenuClose);

        jMenuBar1.add(fileMenuP);

        jMenuEdit.setMnemonic('E');
        jMenuEdit.setDelay(300);
        jMenuEdit.setText("Edit");

        undoItem.setMnemonic('U');
        undoItem.setText("Undo");
        undoItem.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                undoItemAncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        undoItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoItemActionPerformed(evt);
            }
        });
        jMenuEdit.add(undoItem);

        redoItem.setMnemonic('R');
        redoItem.setText("Redo");
        redoItem.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                redoItemAncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        redoItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoItemActionPerformed(evt);
            }
        });
        jMenuEdit.add(redoItem);

        jMenuItemDuplicate.setMnemonic('D');
        jMenuItemDuplicate.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuItemDuplicate.setText("Duplicate");
        jMenuEdit.add(jMenuItemDuplicate);
        jMenuItemDuplicate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDuplicateActionPerformed(evt);
            }
        });

        jMenuItemDelete.setMnemonic('E');
        jMenuItemDelete.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        jMenuItemDelete.setText("Delete");
        jMenuItemDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDeleteActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemDelete);

        jMenuItemSelectAll.setMnemonic('A');
        jMenuItemSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuItemSelectAll.setText("Select All");
        jMenuItemSelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSelectAllActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemSelectAll);

        jMenuItemAddObj.setMnemonic('N');
        jMenuItemAddObj.setText("New Object...");
        jMenuItemAddObj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAddObjActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemAddObj);
        jMenuEdit.add(jSeparator4);

        jMenuBar1.add(jMenuEdit);

        jMenuView.setMnemonic('V');
        jMenuView.setDelay(300);
        jMenuView.setText("View");

        jMenuItemNotes.setMnemonic('T');
        jMenuItemNotes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuItemNotes.setText("Patch Notes");
        jMenuItemNotes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemNotesActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemNotes);

        jMenuItemSettings.setMnemonic('I');
        jMenuItemSettings.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuItemSettings.setText("Patch Settings");
        jMenuItemSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSettingsActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemSettings);
        jMenuView.add(jSeparator2);

        jMenuItemOpenFileLocation.setMnemonic('F');
        jMenuItemOpenFileLocation.setText("Open File Location");
        jMenuItemOpenFileLocation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenFileLocationActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemOpenFileLocation);
        jMenuView.add(jSeparator3);

        jCheckBoxMenuItemCordsInBackground.setMnemonic('B');
        jCheckBoxMenuItemCordsInBackground.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyUtils.CONTROL_OR_CMD_MASK));
        jCheckBoxMenuItemCordsInBackground.setText("Patch Cords in Background");
        jCheckBoxMenuItemCordsInBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemCordsInBackgroundActionPerformed(evt);
            }
        });
        jMenuView.add(jCheckBoxMenuItemCordsInBackground);

        jMenuItemAdjScroll.setMnemonic('R');
        jMenuItemAdjScroll.setText("Refresh Scrollbars");
        jMenuItemAdjScroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAdjScrollActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemAdjScroll);

        jMenuBar1.add(jMenuView);

        jMenuPatch.setMnemonic('P');
        jMenuPatch.setDelay(300);
        jMenuPatch.setText("Patch");

        jCheckBoxMenuItemLive.setMnemonic('L');
        jCheckBoxMenuItemLive.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyUtils.CONTROL_OR_CMD_MASK));
        jCheckBoxMenuItemLive.setText("Live");
        jCheckBoxMenuItemLive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemLiveActionPerformed(evt);
            }
        });
        jMenuPatch.add(jCheckBoxMenuItemLive);

        jMenuItemUploadSD.setMnemonic('U');
        jMenuItemUploadSD.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U,
                KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
        jMenuItemUploadSD.setText("Upload to SD Card");
        jMenuItemUploadSD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemUploadSDActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuItemUploadSD);
        jMenuPatch.add(jSeparator5);

        jMenuItemUploadSDStart.setText("Upload to SD Card as Startup");
        jMenuItemUploadSDStart.setMnemonic('S');
        jMenuItemUploadSDStart.setDisplayedMnemonicIndex(21);
        jMenuItemUploadSDStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemUploadSDStartActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuItemUploadSDStart);

        jMenuItemUploadInternalFlash.setMnemonic('I');
        jMenuItemUploadInternalFlash.setText("Upload to Internal Flash as Startup");
        jMenuItemUploadInternalFlash.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemUploadInternalFlashActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuItemUploadInternalFlash);
        jMenuPatch.add(jSeparator6);

        jMenuGenerateAndCompileCode.setText("Generate & compile code");
        jMenuGenerateAndCompileCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuGenerateAndCompileCodeActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuGenerateAndCompileCode);

        jMenuGenerateCode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuGenerateCode.setText("Generate code");
        jMenuGenerateCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuGenerateCodeActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuGenerateCode);

        jMenuCompileCode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuCompileCode.setText("Compile code");
        jMenuCompileCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCompileCodeActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuCompileCode);

        jMenuUploadCode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuUploadCode.setText("Upload code");
        jMenuUploadCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuUploadCodeActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuUploadCode);

        jMenuItemLock.setText("Lock");
        jMenuItemLock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLockActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuItemLock);

        jMenuItemUnlock.setText("Unlock");
        jMenuItemUnlock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemUnlockActionPerformed(evt);
            }
        });
        jMenuPatch.add(jMenuItemUnlock);

        jMenuBar1.add(jMenuPatch);

        jMenuPreset.setText("Preset");
        jMenuPreset.setDelay(300);
        jMenuPreset.setEnabled(false);

        jMenuItemClearPreset.setText("Clear current preset");
        jMenuItemClearPreset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemClearPresetActionPerformed(evt);
            }
        });
        jMenuPreset.add(jMenuItemClearPreset);

        jMenuItemPresetCurrentToInit.setText("Copy current state to init");
        jMenuItemPresetCurrentToInit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPresetCurrentToInitActionPerformed(evt);
            }
        });
        jMenuPreset.add(jMenuItemPresetCurrentToInit);

        jMenuItemDifferenceToPreset.setText("Difference between current and init to preset");
        jMenuItemDifferenceToPreset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDifferenceToPresetActionPerformed(evt);
            }
        });
        jMenuPreset.add(jMenuItemDifferenceToPreset);

        jMenuBar1.add(jMenuPreset);
        jMenuBar1.add(windowMenu1);

        helpMenu1.setMnemonic('H');
        helpMenu1.setText("Help");
        jMenuBar1.add(helpMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }

    private void jToggleButtonLiveActionPerformed(java.awt.event.ActionEvent evt) {
        handleLiveAction(jToggleButtonLive.isSelected());
    }

    private void jMenuCopyActionPerformed(java.awt.event.ActionEvent evt) {
        Patch p = patch.GetSelectedObjects();
        if (p.objectInstances.isEmpty()) {
            getToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
            return;
        }
        p.PreSerialize();
        Serializer serializer = new Persister(new Format(2));
        try {
            Clipboard clip = getToolkit().getSystemClipboard();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.write(p, baos);
            StringSelection s = new StringSelection(baos.toString());
            clip.setContents(s, (ClipboardOwner)null);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void jMenuPasteActionPerformed(java.awt.event.ActionEvent evt) {
        Clipboard clip = getToolkit().getSystemClipboard();
        try {
            patch.paste((String)clip.getData(DataFlavor.stringFlavor), null, false);
        }
        catch (UnsupportedFlavorException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void jMenuSaveActionPerformed(java.awt.event.ActionEvent evt) { 
        String fn = patch.getFileNamePath();
        if ((fn != null) && (!fn.equals("untitled"))) {
            File f = new File(fn);
            patch.setFileNamePath(f.getPath());
            patch.save(f);
        }
        else {
            jMenuSaveAsActionPerformed(evt);
        }
    }

    File FileChooserSave(String title) {

        fc.resetChoosableFileFilters();
        fc.setCurrentDirectory(new File(Preferences.getInstance().getCurrentFileDirectory()));
        fc.restoreCurrentSize();
        fc.setDialogTitle(title);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(FileUtils.axpFileFilter);
        fc.addChoosableFileFilter(FileUtils.axsFileFilter);
        fc.addChoosableFileFilter(FileUtils.axhFileFilter);

        String fn = patch.getFileNamePath();
        if (fn == null) {
            fn = "untitled";
        }
        File f = new File(fn);
        fc.setSelectedFile(f);

        String ext = "";
        int dot = fn.lastIndexOf('.');
        if (dot > 0 && fn.length() > dot + 3) {
            ext = fn.substring(dot);
        }

        if (ext.equalsIgnoreCase(".axp")) {
            fc.setFileFilter(FileUtils.axpFileFilter);
        }
        else if (ext.equalsIgnoreCase(".axs")) {
            fc.setFileFilter(FileUtils.axsFileFilter);
        }
        else if (ext.equalsIgnoreCase(".axh")) {
            fc.setFileFilter(FileUtils.axhFileFilter);
        }
        else {
            fc.setFileFilter(FileUtils.axpFileFilter);
        }

        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String filterext = ".axp";
            if (fc.getFileFilter() == FileUtils.axpFileFilter) {
                filterext = ".axp";
            }
            else if (fc.getFileFilter() == FileUtils.axsFileFilter) {
                filterext = ".axs";
            }
            else if (fc.getFileFilter() == FileUtils.axhFileFilter) {
                filterext = ".axh";
            }

            File fileToBeSaved = fc.getSelectedFile();
            ext = "";
            String fname = fileToBeSaved.getAbsolutePath();
            dot = fname.lastIndexOf('.');
            if (dot > 0 && fname.length() > dot + 3) {
                ext = fname.substring(dot);
            }

            if (!(ext.equalsIgnoreCase(".axp")
                    || ext.equalsIgnoreCase(".axh")
                    || ext.equalsIgnoreCase(".axs"))) {

                fileToBeSaved = new File(fc.getSelectedFile() + filterext);

            }
            else if (!ext.equals(filterext)) {
                Object[] options = {"Change",
                    "No"};
                int n = KeyboardNavigableOptionPane.showOptionDialog(this,
                        "File extension does not match filter. Change extension to " + filterext + "?",
                        "File Extension",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                switch (n) {
                    case JOptionPane.YES_OPTION:
                        fileToBeSaved = new File(fname.substring(0, fname.length() - 4) + filterext);
                        break;
                    case JOptionPane.NO_OPTION:
                        return null;
                }
            }

            if (fileToBeSaved.exists()) {
                Object[] options = {"Overwrite",
                    "Cancel"};
                int n = KeyboardNavigableOptionPane.showOptionDialog(this,
                        "File exists! Overwrite?",
                        "File Exists",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
                switch (n) {
                    case JOptionPane.YES_OPTION:
                        break;
                    case JOptionPane.NO_OPTION:
                        return null;
                }
            }

            fc.updateCurrentSize();
            return fileToBeSaved;
        }
        else {
            fc.updateCurrentSize();
            return null;
        }
    }

    private void jMenuSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
        File fileToBeSaved = FileChooserSave("Save As...");
        if (fileToBeSaved != null) {
            patch.setFileNamePath(fileToBeSaved.getPath());
            patch.save(fileToBeSaved);
            Preferences.getInstance().setCurrentFileDirectory(fileToBeSaved.getPath());
        }
    }

    private void jMenuItemAdjScrollActionPerformed(java.awt.event.ActionEvent evt) {
        jScrollPane1.setAutoscrolls(true);
        patch.AdjustSize();
    }

    private void jCheckBoxMenuItemCordsInBackgroundActionPerformed(java.awt.event.ActionEvent evt) {
        patch.SetCordsInBackground(jCheckBoxMenuItemCordsInBackground.isSelected());
    }

    private void jMenuGenerateCodeActionPerformed(java.awt.event.ActionEvent evt) {
        patch.WriteCode(true);
    }

    private void jMenuCompileCodeActionPerformed(java.awt.event.ActionEvent evt) {
        patch.Compile();
    }

    private void jMenuUploadCodeActionPerformed(java.awt.event.ActionEvent evt) {
        patch.GetQCmdProcessor().SetPatch(null);
        patch.GetQCmdProcessor().AppendToQueue(new QCmdStop());
        if (patch.getBinFile().exists()) {
            patch.GetQCmdProcessor().AppendToQueue(new QCmdUploadPatch(patch.getBinFile()));
            patch.GetQCmdProcessor().AppendToQueue(new QCmdStart(patch));
            patch.GetQCmdProcessor().AppendToQueue(new QCmdLock(patch));
        }
        else {
            String path = System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + "build" + patch.generateBuildFilenameStem(true);
            LOGGER.log(Level.INFO, path.replace('\\', '/') + ".bin not found.");
        }
    }

    private void jMenuItemLockActionPerformed(java.awt.event.ActionEvent evt) {
        patch.Lock();
    }

    private void jMenuItemUnlockActionPerformed(java.awt.event.ActionEvent evt) {
        patch.Unlock();
    }

    private void jMenuItemClearPresetActionPerformed(java.awt.event.ActionEvent evt) {
        patch.ClearCurrentPreset();
    }

    private void jMenuItemPresetCurrentToInitActionPerformed(java.awt.event.ActionEvent evt) {
        patch.CopyCurrentToInit();
    }

    private void jMenuItemDifferenceToPresetActionPerformed(java.awt.event.ActionEvent evt) {
        patch.DifferenceToPreset();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        AskClose();
    }

    private void jMenuItemDuplicateActionPerformed(java.awt.event.ActionEvent evt) {
        if (patch.IsLocked()) {
            return;
        }
        Patch p = patch.GetSelectedObjects();
        if (p.objectInstances.isEmpty()) {
            return;
        }
        jMenuCopyActionPerformed(evt);
        jMenuPasteActionPerformed(evt);

        /* emulate mouse dragging */
        Robot robot;
        try {
            robot = new Robot();
            Point point = p.objectInstances.get(0).getLocationOnScreen();
            robot.mouseMove(point.x + 40,point.y + 22);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void jMenuItemDeleteActionPerformed(java.awt.event.ActionEvent evt) { 
        patch.deleteSelectedAxoObjInstances();
    }

    private void jMenuItemSelectAllActionPerformed(java.awt.event.ActionEvent evt) {
        patch.SelectAll();
    }

    private void jMenuItemNotesActionPerformed(java.awt.event.ActionEvent evt) {
        patch.ShowNotesTextEditor();
    }

    private void jMenuItemSettingsActionPerformed(java.awt.event.ActionEvent evt) {
        //TODO: create CTRL+SHIFT+I for opening object settings
        // AxoObjectInstanceAbstract selObj = null;
        // ArrayList<AxoObjectInstanceAbstract> oi = patch.objectInstances;
        // if(oi != null) {
            // for(AxoObjectInstanceAbstract i : oi) {
                // if(i.IsSelected() && i instanceof AxoObjectInstance) {
                    // selObj = i;
                // }
            // }
        // }
        // 
        // if(selObj!=null) {
            // ((AxoObjectInstance) selObj).OpenEditor();
        // }
        // else {
            if (patch.settings == null) {
                patch.settings = new PatchSettings();
            }
            patch.settings.showEditor(patch);
        // }
    }

    private void jMenuItemOpenFileLocationActionPerformed(java.awt.event.ActionEvent evt) {
        Desktop desktop = Desktop.getDesktop();
        OS os = axoloti.utils.OSDetect.getOS();
        try {
            switch (os) {
                case WIN:
                    /* desktop.open(new File(patch.getFileNamePath()).getParentFile()); opens folder but doesn't point to file.
                     * desktop.browseFileDirectory(new File(patch.getFileNamePath())); not supported on Windows.
                     * Do explorer.exe workaround instead.
                     */
                    String[] str = new String("explorer.exe /select,\"" + patch.getFileNamePath() + "\"").split("\\s+");
                    Runtime.getRuntime().exec(str);
                    break;
                case MAC:
                    // TODO: supported?
                    // desktop.browseFileDirectory(new File(patch.getFileNamePath()));
                case LINUX:
                default:
                    // desktop.browseFileDirectory(new File(patch.getFileNamePath())); /* NOT supported, tested on Debian */
                    desktop.open(new File(patch.getFileNamePath()).getParentFile()); /* Opens folder but doesn't point to file. It's the best we can get */
                    break;
            }
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void jCheckBoxMenuItemLiveActionPerformed(java.awt.event.ActionEvent evt) {
        handleLiveAction(jCheckBoxMenuItemLive.isSelected());
    }

    private void jMenuItemUploadSDActionPerformed(java.awt.event.ActionEvent evt) {
        patch.UploadToSDCard();
    }

    private void jMenuItemUploadSDStartActionPerformed(java.awt.event.ActionEvent evt) {
        patch.UploadToSDCard("/start.bin");
    }

    private void jMenuSaveClipActionPerformed(java.awt.event.ActionEvent evt) {
        Serializer serializer = new Persister(new Format(2));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        try {
            serializer.write(patch, baos);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(new StringSelection(baos.toString()), null);
    }

    private void jMenuItemUploadInternalFlashActionPerformed(java.awt.event.ActionEvent evt) {

        jMenuItemUploadInternalFlash.setEnabled(false);

        new SwingWorker<Boolean, String>() {

            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    boolean success;
                    patch.WriteCode(true);
                    qcmdprocessor.AppendToQueue(new qcmds.QCmdCompilePatch(patch));
                    qcmdprocessor.WaitQueueFinished();
                    qcmdprocessor.AppendToQueue(new qcmds.QCmdStop());
                    if (patch.getBinFile().exists()) {
                        qcmdprocessor.AppendToQueue(new qcmds.QCmdUploadPatch(patch.getBinFile()));
                        qcmdprocessor.AppendToQueue(new qcmds.QCmdCopyPatchToFlash());
                        qcmdprocessor.WaitQueueFinished();
                        success = true;
                    }
                    else {
                        String path = System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + "build" + patch.generateBuildFilenameStem(true);
                        LOGGER.log(Level.INFO, path.replace('\\', '/') + ".bin not found.");
                        success = false;
                    }
                    return success;
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Patch upload to internal Flash failed:", e);
                    publish("Go Live failed: " + e.getMessage());
                    return false;

                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                    }
                    else {
                    }
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Upload to internal Flash worker failed unexpectedly:", e);
                }
                finally {
                    jMenuItemUploadInternalFlash.setEnabled(true);
                }
            }
        }.execute();
    }

    private void jMenuItemAddObjActionPerformed(java.awt.event.ActionEvent evt) {
        patch.ShowClassSelector(new Point(20, 20), null, null, true);
    }

    private void jMenuCloseActionPerformed(java.awt.event.ActionEvent evt) {
        AskClose();
    }

    private void formComponentShown(java.awt.event.ComponentEvent evt) {
        DocumentWindowList.RegisterWindow(this);
        if (firstShow) {
            if (jScrollPane1 != null && jScrollPane1.getViewport() != null) {
                /* When patch is first opened, scroll to top-left scroll position */
                jScrollPane1.getViewport().setViewPosition(new Point(0,0));
            }
            jScrollPane1.setVisible(true);
            firstShow = false;
        }
    }

    private void formComponentHidden(java.awt.event.ComponentEvent evt) {
        DocumentWindowList.UnregisterWindow(this);
    }

    private void jMenuSaveCopyActionPerformed(java.awt.event.ActionEvent evt) {
        File fileToBeSaved = FileChooserSave("Save Copy...");
        patch.getPatchframe().requestFocus();
        if (fileToBeSaved != null) {
            patch.save(fileToBeSaved);
            Preferences.getInstance().setCurrentFileDirectory(fileToBeSaved.getPath());
        }
    }

    private void jMenuGenerateAndCompileCodeActionPerformed(java.awt.event.ActionEvent evt) {
        patch.WriteCode(true);
        patch.Compile();
    }

    private void undoItemActionPerformed(java.awt.event.ActionEvent evt) {
        patch.undo();
        this.updateUndoRedoEnabled();
    }

    private void redoItemActionPerformed(java.awt.event.ActionEvent evt) {
        patch.redo();
        this.updateUndoRedoEnabled();
    }

    private void undoItemAncestorAdded(javax.swing.event.AncestorEvent evt) {
        undoItem.setEnabled(patch.canUndo());
    }

    private void redoItemAncestorAdded(javax.swing.event.AncestorEvent evt) {
        redoItem.setEnabled(patch.canRedo());
    }

    private void formWindowLostFocus(java.awt.event.WindowEvent evt) {
        getRootPane().setCursor(Cursor.getDefaultCursor());
    }

    private boolean GoLive() {
        if (patch.getFileNamePath().endsWith(".axs") || patch.getContainer() != null) {
            Object[] options = {"Go Live",
                "Cancel"};

            int n = KeyboardNavigableOptionPane.showOptionDialog(this,
                    "This is a subpatch intended to be placed inside a main patch and possibly has no input or output.\nDo you still want to take it live?",
                    "File is Subpatch",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]);
            switch (n) {
                case JOptionPane.NO_OPTION:
                    return false;
                case JOptionPane.YES_OPTION:
                    ; // fall thru
            }
        }
        for (AxoObjectInstanceAbstract aoi : patch.objectInstances) {
            if (aoi instanceof AxoObjectInstancePatcher) {
                ((AxoObjectInstancePatcher)aoi).updateObj1();
            }
        }
        patch.GoLive();
        return true;
    }

    void ShowDSPLoad(int val200, boolean overload) {
        int pv = jProgressBarDSPLoad.getValue();
        if (val200 == pv) {
            return;
        }
        if (patch.IsLocked()) {
            jProgressBarDSPLoad.setValue((pv+val200)/2);
        }
        else if (pv != 0) {
            jProgressBarDSPLoad.setValue(0);
        }

        if(previousOverload != overload) {
            if(overload) {
                jProgressBarDSPLoad.setForeground(Theme.ProgressBar_Overload_Foreground);
            }
            else {
                jProgressBarDSPLoad.setForeground(Theme.Button_Accent_Background); 
            }
        }
        previousOverload = overload;
    }

    @Override
    public JFrame GetFrame() {
        return this;
    }

    @Override
    public File getFile() {
        if (patch.getFileNamePath() == null) {
            return null;
        }
        else {
            return new File(patch.getFileNamePath());
        }
    }

    public PatchGUI getPatch() {
        return patch;
    }

    public void setSaveMenuEnabled(boolean b) {
        jMenuSave.setEnabled(b);
    }

    public boolean getSaveMenuEnabled() {
        return jMenuSave.isEnabled();
    }

    public boolean getPatchCordsInBackground() {
        return jCheckBoxMenuItemCordsInBackground.isSelected();
    }

    public void setUnsavedAsterisk(boolean b) {

        if (getSaveMenuEnabled()) { /* hacky way to see if a patchframe is an embedded subpatch */
            if (b && !getTitle().startsWith("*")) {
                setTitle("*" + getTitle()); /* asterisk to indicate unsaved state */
            }

            else if (!b && getTitle().startsWith("*")) {
                setTitle(getTitle().substring(1)); /* else clear asterisk */
            }
        }
    }

    @Override
    public ArrayList<DocumentWindow> GetChildDocuments() {
        return dwl;
    }

    public void updateUndoRedoEnabled() {
        redoItem.setEnabled(patch.canRedo());
        undoItem.setEnabled(patch.canUndo());
    }

    @Override
    public void ShowSDCardMounted() {
        jMenuItemUploadSD.setEnabled(true);
        jMenuItemUploadSDStart.setEnabled(true);
    }

    @Override
    public void ShowSDCardUnmounted() {
        jMenuItemUploadSD.setEnabled(false);
        jMenuItemUploadSDStart.setEnabled(false);
    }

    // @Override
    // public void ShowConnectionFlags(int connectionFlags) {
    // }

    @Override
    public void ShowBoardIDName(String unitId, String friendlyName) {

        if (!USBBulkConnection.GetConnection().isConnected()) {
            jLabelBoardIDName.setText(" ");
            jLabelBoardIDName.setToolTipText("");
            return;
        }
        if (unitId == null || unitId.trim().isEmpty()) {
            return;
        }

        String nameToDisplay = friendlyName;
        if (nameToDisplay == null || nameToDisplay.trim().isEmpty()) {
            nameToDisplay = Preferences.getInstance().getBoardName(unitId);
        }
        if (nameToDisplay == null || nameToDisplay.trim().isEmpty()) {
            StringBuilder formattedCpuId = new StringBuilder("Board ID:   ");
            if (unitId.length() >= 24) {
                formattedCpuId.append(unitId.substring(0, 8)).append(" ")
                              .append(unitId.substring(8, 16)).append(" ")
                              .append(unitId.substring(16, 24));
            }
            else if (unitId.length() >= 16) {
                formattedCpuId.append(unitId.substring(0, 8)).append(" ")
                              .append(unitId.substring(8, 16));
            }
            else if (unitId.length() > 0) {
                formattedCpuId.append(unitId);
            }
            else {
                formattedCpuId.append("N/A");
            }

            jLabelBoardIDName.setText(formattedCpuId.toString());
            jLabelBoardIDName.setToolTipText("Showing board ID of the currently connected Core.\n" +
                                                "You can name your Core by disconnecting it from\n" +
                                                "the Patcher, then going to Board > Select Device... > Name.\n" +
                                                "Press Enter in the Name textfield to confirm the entry.");
        }
        else {
            jLabelBoardIDName.setText("Board Name:   " + nameToDisplay);
            jLabelBoardIDName.setToolTipText("Showing the name defined in Board > Select Device... > Name.\n" +
                                                "This setting is saved in the local ksoloti.prefs file.");
        }
    }
}
