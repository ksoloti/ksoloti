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

import static axoloti.Axoloti.FIRMWARE_DIR;
import static axoloti.Axoloti.HOME_DIR;
import axoloti.dialogs.AxoJFileChooser;
import axoloti.dialogs.FileManagerFrame;
import axoloti.dialogs.KeyboardFrame;
import axoloti.dialogs.PatchBank;
import axoloti.dialogs.PreferencesFrame;
import axoloti.object.AxoObjects;
import axoloti.usb.Usb;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Constants;
import axoloti.utils.FirmwareID;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;
import axoloti.Boards.BoardDetail;
import axoloti.Boards.BoardMode;
import axoloti.Boards.BoardType;
import axoloti.Boards.FirmwareType;
import axoloti.Boards.MemoryLayoutType;
import axoloti.Boards.SampleRateType;

import components.ScrollPaneComponent;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
// import java.net.BindException;
import java.net.MalformedURLException;
// import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Box.Filler;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;

import com.formdev.flatlaf.FlatClientProperties;

import qcmds.QCmdBringToDFUMode;
import qcmds.QCmdCompilePatch;
import qcmds.QCmdDisconnect;
import qcmds.QCmdFlashDFU;
import qcmds.QCmdPing;
import qcmds.QCmdProcessor;
import qcmds.QCmdShowConnect;
import qcmds.QCmdShowDisconnect;
import qcmds.QCmdStartFlasher;
import qcmds.QCmdStartMounter;
import qcmds.QCmdStop;
import qcmds.QCmdUploadFWSDRam;
import qcmds.QCmdUploadPatch;

import axoloti.AxolotiLibraryWatcher;

/**
 *
 * @author Johannes Taelman
 */
public final class MainFrame extends javax.swing.JFrame implements ActionListener, ConnectionStatusListener, SDCardMountStatusListener, ConnectionFlagsListener, AxolotiLibraryWatcherListener{

    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());

    static public Preferences prefs = Preferences.LoadPreferences();
    static public AxoObjects axoObjects;
    public static MainFrame mainframe;
    public static AxoJFileChooser fc;

    Thread axolotiLibraryWatcherThread;
    boolean currentConnectedState = false;

    boolean even = false;
    String LinkFirmwareID;
    String TargetFirmwareID;
    KeyboardFrame keyboard;
    FileManagerFrame filemanager;
    // AxolotiRemoteControl remote;
    QCmdProcessor qcmdprocessor;
    Thread qcmdprocessorThread;
    DeviceConnector deviceConnector;
    Thread deviceConnectorThread;
    
    static public Cursor transparentCursor;
    private final String[] args;
    JMenu favouriteMenu;
    boolean bGrabFocusOnSevereErrors = true;

    private boolean doAutoScroll = true;

    // ServerSocket instanceSocketHack;

    /**
     * Creates new form MainFrame
     *
     * @param args command line arguments
     */
    public MainFrame(String args[]) {
        this.args = args;

        initComponents();
        fileMenu.initComponents();
        setIconImage(Constants.APP_ICON.getImage());

        transparentCursor = getToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), null);

        mainframe = this;
        setVisible(true);

        fc = new AxoJFileChooser(prefs.getCurrentFileDirectory());

        final Style styleParent = jTextPaneLog.addStyle(null, null);
        jTextPaneLog.setFont(Constants.FONT_MONO);

        final Style styleSevere = jTextPaneLog.addStyle("severe", styleParent);
        final Style styleWarning = jTextPaneLog.addStyle("warning", styleParent);
        final Style styleInfo = jTextPaneLog.addStyle("info", styleParent);
        jTextPaneLog.setBackground(Theme.Console_Background);
        StyleConstants.setForeground(styleSevere, Theme.Error_Text);
        StyleConstants.setForeground(styleWarning, Theme.Console_Warning_Text);
        StyleConstants.setForeground(styleInfo, Theme.Console_Normal_Text);

        DefaultCaret caret = (DefaultCaret) jTextPaneLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        jTextPaneLog.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    System.out.println("drag & drop:");
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    /* Cap max opened files to 64 */
                    int openedCount = 0, maxCount = 64;
                    if (droppedFiles.size() > maxCount) {
                        /* Display "whoa" message first */
                        LOGGER.log(Level.WARNING, "Whoa, slow down. Only the first " + maxCount + " files were opened.");
                    }

                    for (File f : droppedFiles) {
                        /* Leave loop if already successfully opened 20 files */ 
                        if (openedCount > maxCount) {
                            break;
                        }

                        String fn = f.getName();
                        System.out.println(fn);
                        if (f != null && f.exists()) {
                            if (f.canRead()) {
                                if (fn.endsWith(".axp") || fn.endsWith(".axs") || fn.endsWith(".axh")) {
                                    PatchGUI.OpenPatch(f);
                                    openedCount++;
                                }
                                else if (fn.endsWith(".axb")) {
                                    PatchBank.OpenBank(f);
                                    openedCount++;
                                }
                                else if (fn.endsWith(".axo")) {
                                    System.out.println("opening .axo files not implemented yet");
                                    // TODO
                                }
                            }
                            else {
                                LOGGER.log(Level.SEVERE, "Error: Cannot read file \"" + fn + "\".)");
                            }
                        }
                        else {
                            LOGGER.log(Level.WARNING, "Warning: File \"" + fn + "\" not found.");
                        }
                    }
                } catch (UnsupportedFlavorException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        });

        jScrollPaneLog.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            BoundedRangeModel brm = jScrollPaneLog.getVerticalScrollBar().getModel();

            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                // Invoked when user select and move the cursor of scroll by mouse explicitly.
                if (!brm.getValueIsAdjusting()) {
                    if (doAutoScroll) {
                        brm.setValue(brm.getMaximum());
                    }
                } else {
                    // doAutoScroll will be set to true when user reaches at the bottom of document.
                    doAutoScroll = ((brm.getValue() + brm.getExtent()) == brm.getMaximum());
                }
            }
        });

        jScrollPaneLog.addMouseWheelListener(new MouseWheelListener() {
            BoundedRangeModel brm = jScrollPaneLog.getVerticalScrollBar().getModel();

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Invoked when user use mouse wheel to scroll
                if (e.getWheelRotation() < 0) {
                    // If user trying to scroll up, doAutoScroll should be false.
                    doAutoScroll = false;
                } else {
                    // doAutoScroll will be set to true when user reaches at the bottom of document.
                    doAutoScroll = ((brm.getValue() + brm.getExtent()) == brm.getMaximum());
                }
            }
        });

        Handler logHandler = new Handler() {
            @Override
            public void publish(LogRecord lr) {
                if (!SwingUtilities.isEventDispatchThread()) {
                    // System.out.println("logging outside GUI thread:"+lr.getMessage());
                    final LogRecord lrf = lr;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            publish(lrf);
                        }
                    });
                } else {
                    try {
                        String txt;
                        String excTxt = "";
                        Throwable exc = lr.getThrown();
                        if (exc != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            PrintStream ps = new PrintStream(baos);
                            exc.printStackTrace(ps);
                            excTxt = exc.toString();
                            excTxt = excTxt + "\n" + baos.toString("utf-8");
                        }
                        if (lr.getMessage() == null) {
                            txt = excTxt;
                        } else {
                            txt = java.text.MessageFormat.format(lr.getMessage(), lr.getParameters());
                            if (excTxt.length() > 0) {
                                txt = txt + "," + excTxt;
                            }
                        }
                        if (lr.getLevel() == Level.SEVERE) {
                            doAutoScroll = true;
                            jTextPaneLog.getDocument().insertString(jTextPaneLog.getDocument().getLength(),
                                    txt + "\n", styleSevere);
                            if (bGrabFocusOnSevereErrors) {
                                MainFrame.this.toFront();
                            }
                        } else if (lr.getLevel() == Level.WARNING) {
                            jTextPaneLog.getDocument().insertString(jTextPaneLog.getDocument().getLength(),
                                    txt + "\n", styleWarning);
                        } else {
                            jTextPaneLog.getDocument().insertString(jTextPaneLog.getDocument().getLength(),
                                    txt + "\n", styleInfo);
                        }
                    } catch (BadLocationException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    } catch (UnsupportedEncodingException ex) {
                    }
                }
            }

            @Override
            public void flush() {
                jScrollPaneLog.removeAll();
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        logHandler.setLevel(Level.INFO);

        Logger.getLogger("").addHandler(logHandler);
        Logger.getLogger("").setLevel(Level.INFO);
        doLayout();

        keyboard = new KeyboardFrame();
        keyboard.setTitle("Keyboard");
        keyboard.setVisible(false);

        filemanager = new FileManagerFrame();
        filemanager.setLocation((int)getLocation().getX() + 60, (int)getLocation().getY() + 60);
        filemanager.setTitle("File Manager");
        filemanager.setVisible(false);

        // remote = new AxolotiRemoteControl();
        // remote.setTitle("Remote");
        // remote.setVisible(false);

        if (!prefs.getExpertMode()) {
            jMenuItemRefreshFWID.setVisible(false);
        }

        /* When in Developer mode, make default Flash option invisible to avoid confusion */
        jMenuItemFlashDefault.setVisible(!(Axoloti.isDeveloper()));

        jMenuItemFlashUser.setVisible(Axoloti.isDeveloper());
        jMenuItemFCompile.setVisible(Axoloti.isDeveloper() || prefs.getExpertMode());

        if (!TestDir(HOME_DIR, true)) {
            LOGGER.log(Level.SEVERE, "Invalid home directory: {0} - Does it exist? Can it be written to?", System.getProperty(Axoloti.HOME_DIR));
        }
        if (!TestDir(FIRMWARE_DIR, false)) {
            LOGGER.log(Level.SEVERE, "Invalid firmware directory: {0} - Does it exist?", System.getProperty(Axoloti.FIRMWARE_DIR));
        }

        /* Do NOT do any serious initialisation in constructor
         * as a stalling error could prevent event loop running and our logging
         * console opening
         */
        Runnable initr;
        initr = new Runnable() {
            @Override
            public void run() {
                try {
                    AxolotiLibraryWatcher axolotiLibraryWatcher = AxolotiLibraryWatcher.getAxolotiLibraryWatcher();
                    axolotiLibraryWatcher.addListener(mainframe);

                    prefs.getBoards().scanBoards();
                    
                    
                    populateMainframeTitle();

                    // instanceSocketHack = new ServerSocket(1);


                    LOGGER.log(Level.WARNING, "Patcher version {0} | Build time {1}\n", new Object[]{Version.AXOLOTI_VERSION, Version.AXOLOTI_BUILD_TIME});

                    if (prefs.getExpertMode()) {
                        LOGGER.log(Level.WARNING,
                            "Expert Mode is enabled in ksoloti.prefs. The following options are now available:\n" + 
                            "- Compile firmware, Refresh firmware ID (Board -> Firmware)\n" + 
                            "- Generate and/or compile patch code, simulate lock/unlock, also while no Core is connected (patch windows -> Patch)\n" + 
                            "- Remove read-only restrictions: Edit and save to read-only libraries (axoloti-factory, *oloti-community, ksoloti-objects)\n" + 
                            "- Test-compile all patches in all libraries, or all patches (recursively) in specified folder (File -> Test Compilation)\n"
                        );
                    }

                    if (prefs.boards.getBoardType() == BoardType.Axoloti) {
                        LOGGER.log(Level.WARNING, ">>> Axoloti Legacy Mode <<<\n");
                    }

                    switch (prefs.boards.getFirmware())
                    {
                        case FirmwareType.SPILink :
                            LOGGER.log(Level.WARNING, ">>> SPILink-enabled firmware <<<\nPins PB3, PB4, PD5, PD6 are occupied by SPILink communication in this firmware mode!\n");
                            break;
                        case USBAudio:
                            LOGGER.log(Level.WARNING, ">>> USBAudio-enabled firmware <<<\n");
                            break;
                        case i2SCodec:
                            LOGGER.log(Level.WARNING, ">>> I2SCodec-enabled firmware <<<\nPins PA15, PB3, PB4, PD6 are occupied by I2S communication in this firmware mode!\n");
                            break;
                        default:
                            break;
                    }

                    updateLinkFirmwareID();

                    qcmdprocessor = QCmdProcessor.getQCmdProcessor();
                    qcmdprocessorThread = new Thread(qcmdprocessor);
                    qcmdprocessorThread.setName("QCmdProcessor");
                    qcmdprocessorThread.start();

                    deviceConnector = DeviceConnector.getDeviceConnector();
                    deviceConnectorThread = new Thread(deviceConnector);
                    deviceConnectorThread.setName("DeviceConnector");
                    deviceConnectorThread.start();

                    USBBulkConnection.GetConnection().addConnectionStatusListener(MainFrame.this);
                    USBBulkConnection.GetConnection().addSDCardMountStatusListener(MainFrame.this);
                    USBBulkConnection.GetConnection().addConnectionFlagsListener(MainFrame.this);

                    doConnect();

                    // Axoloti user library, ask user if they wish to upgrade, or do manual
                    // this allows them the opportunity to manually backup their files!
                    AxolotiLibrary ulib = prefs.getLibrary(AxolotiLibrary.USER_LIBRARY_ID);
                    if (ulib != null) {
                        String cb = ulib.getCurrentBranch();
                        if (!cb.equalsIgnoreCase(ulib.getBranch())) {
                            LOGGER.log(Level.INFO, "Current axoloti-community library does not match specified version: {0} <-> {1}", new Object[]{cb, ulib.getBranch()});
                            int s = JOptionPane.showConfirmDialog(MainFrame.this,
                                    "Axoloti community library version mismatch detected. Upgrade now?\n"
                                    + "This will stash any local changes and reapply them to the new version.\n"
                                    + "If you choose no, you will need to manually backup your changes and then sync libraries.",
                                    "Axoloti Community Library Mismatch",
                                    JOptionPane.YES_NO_OPTION);
                            if (s == JOptionPane.YES_OPTION) {
                                ulib.upgrade();
                            }
                        }
                    }

                    // Ksoloti user library, ask user if they wish to upgrade, or do manual
                    // this allows them the opportunity to manually backup their files!
                    AxolotiLibrary kso_ulib = prefs.getLibrary(AxolotiLibrary.KSOLOTI_CONTRIB_LIBRARY_ID);
                    if (kso_ulib != null) {
                        String cb = kso_ulib.getCurrentBranch();
                        if (!cb.equalsIgnoreCase(kso_ulib.getBranch())) {
                            LOGGER.log(Level.INFO, "Current ksoloti-community library does not match specified version: {0} <-> {1}", new Object[]{cb, kso_ulib.getBranch()});
                            int s = JOptionPane.showConfirmDialog(MainFrame.this,
                                    "Ksoloti community library version mismatch detected. Upgrade now?\n"
                                    + "This will stash any local changes and reapply them to the new version.\n"
                                    + "If you choose no, you will need to manually backup your changes and then sync libraries.",
                                    "Ksoloti Community Library Mismatch",
                                    JOptionPane.YES_NO_OPTION);
                            if (s == JOptionPane.YES_OPTION) {
                                kso_ulib.upgrade();
                            }
                        }
                    }

                    // factory library force and upgrade
                    // Im stashing changes here, just in case, but in reality users should not be altering factory 
                    ulib = prefs.getLibrary(AxolotiLibrary.FACTORY_ID);
                    if (ulib != null) {
                        String cb = ulib.getCurrentBranch();
                        if (!cb.equalsIgnoreCase(ulib.getBranch())) {
                            LOGGER.log(Level.INFO, "Current axoloti-factory library does not match specified version, upgrading... ({0} -> {1})", new Object[]{cb, ulib.getBranch()});
                            ulib.upgrade();
                        }
                    }

                    // ksoloti-objects library force and upgrade
                    // Im stashing changes here, just in case, but in reality users should not be altering factory 
                    ulib = prefs.getLibrary(AxolotiLibrary.KSOLOTI_LIBRARY_ID);
                    if (ulib != null) {
                        String cb = ulib.getCurrentBranch();
                        if (!cb.equalsIgnoreCase(ulib.getBranch())) {
                            LOGGER.log(Level.INFO, "Current ksoloti-objects library does not match specified version, upgrading... ({0} -> {1})", new Object[]{cb, ulib.getBranch()});
                            ulib.upgrade();
                        }
                    }

                    boolean autoSyncMessageDone = false;

                    for (AxolotiLibrary lib : prefs.getLibraries()) {
                        if (lib.isAutoSync() && lib.getEnabled()) {
                            if (!autoSyncMessageDone) {
                                LOGGER.log(Level.INFO, "Auto-syncing libraries...");
                                autoSyncMessageDone = true;
                            }
                            LOGGER.log(Level.INFO, lib.getId() + "...");
                            lib.sync();
                        }
                        if (autoSyncMessageDone) {
                            LOGGER.log(Level.INFO, "Done auto-syncing libraries.\n");
                        }
                    }

                    LOGGER.log(Level.INFO, "Checking library status...");
                    for (AxolotiLibrary lib : prefs.getLibraries()) {
                        lib.reportStatus();
                    }
                    LOGGER.log(Level.INFO, "Done checking library status.\n");


                    LOGGER.log(Level.INFO, "initialising File Watcher...");
                    String spath[] = MainFrame.prefs.getObjectSearchPath();
                    if (spath != null) {
                        for (String path : spath) {
                            LOGGER.log(Level.INFO, "Adding {0} to file watcher.", path);
                            axolotiLibraryWatcher.AddFolder(path, true);
                        }
                    }
                    else {
                        LOGGER.log(Level.SEVERE, "Object path empty!\n");
                    }
                    LOGGER.log(Level.INFO, "Done initialising File Watcher...");

                    axoObjects = new AxoObjects();
                    axoObjects.LoadAxoObjects();
                    axolotiLibraryWatcher.addListener(axoObjects);
                    
                    axolotiLibraryWatcherThread =  new Thread(axolotiLibraryWatcher);
                    axolotiLibraryWatcherThread.start();

                    // Add current file directory to filewater
                    AxolotiLibraryWatcher.getAxolotiLibraryWatcher().AddPatchFolder(prefs.getCurrentDirectory());

                }
                catch (Exception e) {
                    e.printStackTrace();
                }

           }
        };

        EventQueue.invokeLater(initr);


        String argMessage = "";
        for (String arg : this.args) { //TODO check why always opening new instance

            if (argMessage.isEmpty()) {
                argMessage += "CLI arguments:";
            }

            argMessage += " " + arg;

            if (!arg.startsWith("-")) {
                if (arg.endsWith(".axp") || arg.endsWith(".axs") || arg.endsWith(".axh")) {
                    final File f = new File(arg);
                    if (f.exists() && f.canRead()) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // wait for objects be loaded
                                    if (axoObjects.LoaderThread.isAlive()) {
                                        EventQueue.invokeLater(this);
                                    } else {
                                        PatchGUI.OpenPatch(f);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        EventQueue.invokeLater(r);
                    }
                } else if (arg.endsWith(".axo")) {
                    System.out.println("opening .axo files not implemented yet");
                    // NOP for AXO at the moment - new patch and paste object as embedded inside?
                    // NewPatchWithObjectEmbedded---or something();
                }
            }
        }

        if (!argMessage.isEmpty()) {
            LOGGER.log(Level.WARNING, argMessage);
        }

    }


    public void updateConsoleFont() {
        jTextPaneLog.setFont(Constants.FONT_MONO);
    }


    public void populateMainframeTitle() {

        String tstring = "";
        String tsuffix = "";

        tstring = prefs.boards.getBoardType().toString();

        if (Axoloti.isDeveloper()) {
            tsuffix += "Developer";
        }

        if (prefs.getExpertMode()) {
            if (tsuffix.length() > 0) {
                tsuffix += ", ";
            }
            tsuffix += "Expert Mode";
        }

        if (prefs.getFirmwareWarnDisable()) {
            if (tsuffix.length() > 0) {
                tsuffix += ", ";
            }
            tsuffix += "CRC Disabled";
        }

        String firmwareString = prefs.boards.getFirmware().toString();
        tsuffix += ((tsuffix.length()> 0) ? ", " : "") + firmwareString;

        if (tsuffix.length() > 0) {
            tstring += " (" + tsuffix + ")";
        }
        
        MainFrame.this.setTitle(tstring);
    }


    static boolean TestDir(String var, boolean write) {
        String ev = System.getProperty(var);
        File f = new File(ev);
        if (!f.exists()) {
            return false;
        }
        if (!f.isDirectory()) {
            return false;
        }
        if (write && !f.canWrite()) {
            return false;
        }

        return true;
    }


    void flashUsingSDRam(String pname) {

        updateLinkFirmwareID();

        File p = new File(pname);

        if (p.canRead()) {
            qcmdprocessor.AppendToQueue(new QCmdStop());
            qcmdprocessor.AppendToQueue(new QCmdUploadFWSDRam(p));
            qcmdprocessor.AppendToQueue(new QCmdStartFlasher(true));
            qcmdprocessor.AppendToQueue(new QCmdDisconnect());
            ShowDisconnect();
        }
        else {
            LOGGER.log(Level.SEVERE, "Cannot read firmware, please compile firmware! (File: {0})", pname);
        }
    }



    private void initComponents() {

        // jLabel1 = new javax.swing.JLabel();
        jPanelHeader = new javax.swing.JPanel();
        jPanelIconColumn = new javax.swing.JPanel();
        jPanelButtonsColumn = new javax.swing.JPanel();
        jPanelInfoColumn = new javax.swing.JPanel();
        jLabelIcon = new javax.swing.JLabel();
        jButtonClear = new javax.swing.JButton();
        jToggleButtonConnect = new javax.swing.JToggleButton();
        jLabelCPUID = new javax.swing.JLabel();
        jLabelFlags = new javax.swing.JLabel();
        // jLabelFirmwareID = new javax.swing.JLabel();
        jLabelVoltages = new javax.swing.JLabel();
        jLabelPatch = new javax.swing.JLabel();
        jLabelSDCardPresent = new javax.swing.JLabel();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        jScrollPaneLog = new ScrollPaneComponent();
        jTextPaneLog = new javax.swing.JTextPane();
        jPanelProgress = new javax.swing.JPanel();
        jProgressBar1 = new javax.swing.JProgressBar();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));
        jLabelProgress = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new axoloti.menus.FileMenu();
        jMenuEdit = new javax.swing.JMenu();
        jMenuItemCopy = new javax.swing.JMenuItem();
        jMenuBoard = new javax.swing.JMenu();
        jMenuItemSelectCom = new javax.swing.JMenuItem();
        jMenuItemFConnect = new javax.swing.JMenuItem();
        jMenuItemFDisconnect = new javax.swing.JMenuItem();
        jMenuItemPing = new javax.swing.JMenuItem();
        jMenuItemPanic = new javax.swing.JMenuItem();
        jMenuItemMount = new javax.swing.JMenuItem();
        jMenuFirmware = new javax.swing.JMenu();
        jMenuItemFlashDefault = new javax.swing.JMenuItem();
        jMenuItemFlashDFU = new javax.swing.JMenuItem();
        jMenuItemRefreshFWID = new javax.swing.JMenuItem();
        jDevSeparator = new javax.swing.JPopupMenu.Separator();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jMenuItemFCompile = new javax.swing.JMenuItem();
        jMenuItemFlashUser = new javax.swing.JMenuItem();
        windowMenu1 = new axoloti.menus.WindowMenu();
        helpMenu1 = new axoloti.menus.HelpMenu();

        // jLabel1.setText(" ");

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Ksoloti");
        setMinimumSize(new java.awt.Dimension(320, 180));
        setPreferredSize(new java.awt.Dimension(600, 400));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

        jPanelHeader.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5));
        jPanelHeader.setLayout(new javax.swing.BoxLayout(jPanelHeader, javax.swing.BoxLayout.LINE_AXIS));

        jPanelIconColumn.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5));
        jPanelIconColumn.setLayout(new javax.swing.BoxLayout(jPanelIconColumn, javax.swing.BoxLayout.PAGE_AXIS));

        jLabelIcon.setIcon(Constants.APP_ICON);

        jPanelIconColumn.add(jLabelIcon);

        jPanelHeader.add(jPanelIconColumn);

        jPanelButtonsColumn.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5));
        jPanelButtonsColumn.setLayout(new javax.swing.BoxLayout(jPanelButtonsColumn, javax.swing.BoxLayout.PAGE_AXIS));

        Dimension btndim = new Dimension(110,30);
        jToggleButtonConnect.setFocusable(false);
        jToggleButtonConnect.setText("Connect");
        jToggleButtonConnect.setMinimumSize(btndim);
        jToggleButtonConnect.setMaximumSize(btndim);
        jToggleButtonConnect.setPreferredSize(btndim);
        jToggleButtonConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonConnectActionPerformed(evt);
            }
        });
        jPanelButtonsColumn.add(jToggleButtonConnect);
        Dimension fll = new Dimension(0,4);
        jPanelButtonsColumn.add(new Filler(fll, fll, fll));

        jButtonClear.setFocusable(false);
        jButtonClear.setText("Clear Log");
        jButtonClear.setMinimumSize(btndim);
        jButtonClear.setMaximumSize(btndim);
        jButtonClear.setPreferredSize(btndim);
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });
        jPanelButtonsColumn.add(jButtonClear);
        jPanelHeader.add(jPanelButtonsColumn);

        jPanelInfoColumn.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5));
        jPanelInfoColumn.setLayout(new javax.swing.BoxLayout(jPanelInfoColumn, javax.swing.BoxLayout.PAGE_AXIS));

        jLabelCPUID.setText(" ");
        jLabelVoltages.setText(" ");
        jLabelVoltages.putClientProperty(FlatClientProperties.STYLE, "disabledForeground:#FF0000"); /* "disabledForeground" color here means voltage warning, red */
        jLabelSDCardPresent.setText(" ");
        jLabelFlags.setText(" ");
        jLabelPatch.setText(" ");
        
        populateInfoColumn();

        jPanelHeader.add(jPanelInfoColumn);
        jPanelHeader.add(filler3);

        getContentPane().add(jPanelHeader);

        jTextPaneLog.setCaretColor(new Color(0,0,0,0));
        jTextPaneLog.setEditable(false);
        jScrollPaneLog.setViewportView(jTextPaneLog);

        getContentPane().add(jScrollPaneLog);

        jPanelProgress.setMaximumSize(new java.awt.Dimension(8000, 16));
        jPanelProgress.setLayout(new javax.swing.BoxLayout(jPanelProgress, javax.swing.BoxLayout.LINE_AXIS));

        jProgressBar1.setAlignmentX(LEFT_ALIGNMENT);
        jProgressBar1.setMaximumSize(new java.awt.Dimension(100, 16));
        jProgressBar1.setMinimumSize(new java.awt.Dimension(100, 16));
        jProgressBar1.setPreferredSize(new java.awt.Dimension(100, 16));
        jPanelProgress.add(jProgressBar1);
        jPanelProgress.add(filler1);

        jLabelProgress.setFocusable(false);
        jLabelProgress.setMaximumSize(new java.awt.Dimension(500, 14));
        jLabelProgress.setPreferredSize(new java.awt.Dimension(150, 14));
        jPanelProgress.add(jLabelProgress);

        getContentPane().add(jPanelProgress);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");
        fileMenu.setDelay(300);
        jMenuBar1.add(fileMenu);

        jMenuEdit.setMnemonic('E');
        jMenuEdit.setDelay(300);
        jMenuEdit.setText("Edit");

        jMenuItemCopy.setMnemonic('C');
        jMenuItemCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuItemCopy.setText("Copy");
        jMenuEdit.add(jMenuItemCopy);

        jMenuBar1.add(jMenuEdit);

        jMenuBoard.setMnemonic('B');
        jMenuBoard.setDelay(300);
        jMenuBoard.setText("Board");

        jMenuItemSelectCom.setMnemonic('S');
        jMenuItemSelectCom.setText("Select/Configure Device...");
        jMenuItemSelectCom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSelectComActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemSelectCom);

        jMenuItemFConnect.setMnemonic('C');
        jMenuItemFConnect.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K,
                                         KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
        jMenuItemFConnect.setText("Connect");
        jMenuItemFConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFConnectActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemFConnect);

        jMenuItemFDisconnect.setMnemonic('C');
        jMenuItemFDisconnect.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K,
                                            KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
        jMenuItemFDisconnect.setText("Disconnect");
        jMenuItemFDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFDisconnectActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemFDisconnect);
        jMenuBoard.add(jSeparator1);

        
        jMenuItemMount.setText("Enter Card Reader Mode (Disconnects Patcher)");
        jMenuItemMount.setMnemonic('R');
        jMenuItemMount.setDisplayedMnemonicIndex(11);
        jMenuItemMount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMountActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemMount);
        jMenuBoard.add(jSeparator2);

        jMenuItemPing.setText("Ping");
        jMenuItemPing.setEnabled(false);
        jMenuItemPing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPingActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemPing);

        jMenuItemPanic.setText("Panic");
        jMenuItemPanic.setEnabled(false);
        jMenuItemPanic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPanicActionPerformed(evt);
            }
        });
        jMenuBoard.add(jMenuItemPanic);
        jMenuBoard.add(jSeparator3);

        jMenuFirmware.setMnemonic('F');
        jMenuFirmware.setDelay(300);
        jMenuFirmware.setText("Firmware");

        jMenuItemFlashDefault.setMnemonic('F');
        jMenuItemFlashDefault.setText("Flash");
        jMenuItemFlashDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFlashDefaultActionPerformed(evt);
            }
        });
        jMenuFirmware.add(jMenuItemFlashDefault);

        jMenuItemFlashUser.setMnemonic('F');
        jMenuItemFlashUser.setText("Flash");
        jMenuItemFlashUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFlashUserActionPerformed(evt);
            }
        });
        jMenuFirmware.add(jMenuItemFlashUser);

        jMenuItemFlashDFU.setMnemonic('R');
        jMenuItemFlashDFU.setText("Flash (Rescue)");
        jMenuItemFlashDFU.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFlashDFUActionPerformed(evt);
            }
        });
        jMenuFirmware.add(jMenuItemFlashDFU);

        jMenuItemRefreshFWID.setMnemonic('I');
        jMenuItemRefreshFWID.setText("Refresh Firmware ID");
        jMenuItemRefreshFWID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRefreshFWIDActionPerformed(evt);
            }
        });
        jMenuFirmware.add(jMenuItemRefreshFWID);
        jMenuFirmware.add(jDevSeparator);

        jMenuItemFCompile.setMnemonic('C');
        jMenuItemFCompile.setText("Compile");
        jMenuItemFCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFCompileActionPerformed(evt);
            }
        });
        jMenuFirmware.add(jMenuItemFCompile);

        

        jMenuBoard.add(jMenuFirmware);

        jMenuBar1.add(jMenuBoard);
        jMenuBar1.add(windowMenu1);

        helpMenu1.setMnemonic('H');
        helpMenu1.setText("Help");
        jMenuBar1.add(helpMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }


    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {
        jTextPaneLog.setText("");
    }


    private void jMenuItemPanicActionPerformed(java.awt.event.ActionEvent evt) {
        qcmdprocessor.Panic();
    }


    private void jMenuItemPingActionPerformed(java.awt.event.ActionEvent evt) {
        qcmdprocessor.AppendToQueue(new QCmdPing());
    }


    private void jMenuItemFDisconnectActionPerformed(java.awt.event.ActionEvent evt) {
        USBBulkConnection.GetConnection().disconnect();
    }


    private void jMenuItemFConnectActionPerformed(java.awt.event.ActionEvent evt) {
        doConnect();
    }

    public void addFlashDfuOrWarn () {
        if(prefs.boards.getDfuCount() > 1) {
            LOGGER.log(Level.SEVERE, "Multiple boards in rescue mode are attached, please only have one connected.");
        } else {
            qcmdprocessor.AppendToQueue(new qcmds.QCmdFlashDFU());
        }
    }

    public void doConnect() {
        populateMainframeTitle();
            
        ShowDisconnect();

        jToggleButtonConnect.setEnabled(false);
        //jToggleButtonConnect.setText("Connecting");

        BoardDetail boardDetail = prefs.boards.getSelectedBoardDetail();
        if (boardDetail != null) {
            if (boardDetail.boardMode == BoardMode.DFU) {
                    addFlashDfuOrWarn();
            }
            else {
                    new Thread("doConnect") {
                        public void run() {
                            boolean result = USBBulkConnection.GetConnection().connect();
                        if (result) {
                            MainFrame.mainframe.qcmdprocessor.AppendToQueue(new QCmdShowConnect());
                        }
                        jToggleButtonConnect.setEnabled(true);
                    }
                }.start();
            }
        }
    }

    private void jMenuItemSelectComActionPerformed(java.awt.event.ActionEvent evt) {
        if(USBBulkConnection.GetConnection().SelectPort()) {
            doConnect();
        }
    }


    private void jToggleButtonConnectActionPerformed(java.awt.event.ActionEvent evt) {
        if (!jToggleButtonConnect.isSelected()) {
            USBBulkConnection.GetConnection().disconnect();
        } else {
            qcmdprocessor.Panic();
            prefs.getBoards().scanBoards();
            doConnect();
        }
    }


    PreferencesFrame pp;
    /* usually we run all tests, as many may fail for same reason and you want
       a list of all affected files, but if you want to stop on first failure,
       flip this flag */
    public static boolean stopOnFirstFail = false;


    public boolean runAllTests() {
        boolean r1 = runPatchTests();
        if (!r1 && stopOnFirstFail) {
            return r1;
        }
        boolean r2 = runObjectTests();
        if (!r2 && stopOnFirstFail) {
            return r2;
        }
        return r1 && r2;
    }


    public boolean runPatchTests() {
        SetGrabFocusOnSevereErrors(false);
        boolean result;

        AxolotiLibrary fLib = prefs.getLibrary(AxolotiLibrary.FACTORY_ID);
        if (fLib == null) {
            SetGrabFocusOnSevereErrors(true);
            return false;
        }
        result = runTestDir(new File(fLib.getLocalLocation() + "patches"));

        fLib = prefs.getLibrary(AxolotiLibrary.USER_LIBRARY_ID);
        if (fLib == null) {
            SetGrabFocusOnSevereErrors(true);
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "patches"));

        fLib = prefs.getLibrary(AxolotiLibrary.KSOLOTI_LIBRARY_ID);
        if (fLib == null) {
            SetGrabFocusOnSevereErrors(true);
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "patches"));

        fLib = prefs.getLibrary(AxolotiLibrary.KSOLOTI_CONTRIB_LIBRARY_ID);
        if (fLib == null) {
            SetGrabFocusOnSevereErrors(true);
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "patches"));

        SetGrabFocusOnSevereErrors(true);
        return result;
    }


    public boolean runObjectTests() {
        boolean result;

        AxolotiLibrary fLib = prefs.getLibrary(AxolotiLibrary.FACTORY_ID);
        if (fLib == null) {
            return false;
        }
        result = runTestDir(new File(fLib.getLocalLocation() + "objects"));

        fLib = prefs.getLibrary(AxolotiLibrary.USER_LIBRARY_ID);
        if (fLib == null) {
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "objects"));

        fLib = prefs.getLibrary(AxolotiLibrary.KSOLOTI_LIBRARY_ID);
        if (fLib == null) {
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "objects"));

        fLib = prefs.getLibrary(AxolotiLibrary.KSOLOTI_CONTRIB_LIBRARY_ID);
        if (fLib == null) {
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "objects"));

        return result;
    }


    public boolean runFileTest(String patchName) {
        return runTestDir(new File(patchName));
    }


    public boolean runTestDir(File f) {
        if (!f.exists()) {
            return true;
        }
        if (f.isDirectory()) {
            File[] files = f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File f, String name) {
                    File t = new File(f + File.separator + name);
                    if (t.isDirectory()) {
                        return true;
                    }

                    if (name.length() < 4) {
                        return false;
                    }
                    String extension = name.substring(name.length() - 4);
                    boolean b = (extension.equals(".axh") || extension.equals(".axp"));
                    return b;
                }
            });
            for (File s : files) {
                if (!runTestDir(s) && stopOnFirstFail) {
                    return false;
                }
            }
            return true;
        }

        return runTestCompile(f);
    }


    private boolean runTestCompile(File f) {
        SetGrabFocusOnSevereErrors(false);
        LOGGER.log(Level.INFO, "----- Testing {0} -----", f.getPath());

        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy);
        try {
            boolean status;
            PatchGUI patch1 = serializer.read(PatchGUI.class, f);
            PatchFrame pf = new PatchFrame(patch1, qcmdprocessor);
            pf.createBufferStrategy(1);
            patch1.setFileNamePath(f.getPath());
            patch1.PostContructor();
            patch1.WriteCode();
            qcmdprocessor.WaitQueueFinished();
            Thread.sleep(500);
            QCmdCompilePatch cp = new QCmdCompilePatch(patch1);
            patch1.GetQCmdProcessor().AppendToQueue(cp);
            qcmdprocessor.WaitQueueFinished();
            pf.Close();
            Thread.sleep(1000);
            status = cp.success();
            if (status == false) {
                LOGGER.log(Level.SEVERE, "COMPILATION FAILED: {0}\n", f.getPath());
            }
            SetGrabFocusOnSevereErrors(bGrabFocusOnSevereErrors);
            return status;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "COMPILATION FAILED: " + f.getPath() + "\n", ex);
            SetGrabFocusOnSevereErrors(bGrabFocusOnSevereErrors);
            return false;
        }
    }


    public boolean runFileUpgrade(String patchName) {
        return runUpgradeDir(new File(patchName));
    }


    private boolean runUpgradeDir(File f) {
        if (!f.exists()) {
            return true;
        }
        if (f.isDirectory()) {
            File[] files = f.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File f, String name) {
                    File t = new File(f + File.separator + name);
                    if (t.isDirectory()) {
                        return true;
                    }

                    if (name.length() < 4) {
                        return false;
                    }
                    String extension = name.substring(name.length() - 4);
                    boolean b = (extension.equals(".axh") || extension.equals(".axp") || extension.equals(".axs"));
                    return b;
                }
            });
            for (File s : files) {
                if (!runUpgradeDir(s) && stopOnFirstFail) {
                    return false;
                }
            }
            return true;
        }

        return runUpgradeFile(f);
    }


    private boolean runUpgradeFile(File f) {
        LOGGER.log(Level.INFO, "Upgrading {0}", f.getPath());

        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy);
        try {
            boolean status;
            PatchGUI patch1 = serializer.read(PatchGUI.class, f);
            new PatchFrame(patch1, qcmdprocessor);
            patch1.setFileNamePath(f.getPath());
            patch1.PostContructor();
            status = patch1.save(f);
            if (status == false) {
                LOGGER.log(Level.SEVERE, "UPGRADE FAILED: {0}", f.getPath());
            }
            return status;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "UPGRADE FAILED: " + f.getPath(), ex);
            return false;
        }
    }


    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        Quit();
    }


    private void jMenuItemRefreshFWIDActionPerformed(java.awt.event.ActionEvent evt) {
        updateLinkFirmwareID();
    }

    public void userSetsBoardType() {
        BoardDetail boardDetail = prefs.boards.getSelectedBoardDetail();

        if(boardDetail.boardType == BoardType.Unknown) {
            //Custom button text
            Object[] options = {"Ksoloti Geko", "Ksoloti", "Axoloti" };
            int n = JOptionPane.showOptionDialog(this,
                                                "What sort of board is this?",
                                                "Rescue Mode Board Found",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[2]);
            switch(n)
            {
                case 2 : boardDetail.boardType = BoardType.Axoloti; break;
                case 1 : boardDetail.boardType = BoardType.Ksoloti; break;
                case 0 : boardDetail.boardType = BoardType.KsolotiGeko; break;
            }

            prefs.boards.setSelectedBoard(boardDetail);
            prefs.SavePrefs(false);

            axoloti.Axoloti.deletePrecompiledHeaderFile();
            MainFrame.mainframe.updateLinkFirmwareID();
        }
    }

    private void jMenuItemFlashDFUActionPerformed(java.awt.event.ActionEvent evt) {

        Boards boards = MainFrame.prefs.boards;
        if(boards.getDfuCount() == 0) {
            if(prefs.boards.getSelectedBoardDetail().isConnected) {
                updateLinkFirmwareID();
                qcmdprocessor.AppendToQueue(new QCmdBringToDFUMode());
                // qcmdprocessor.AppendToQueue(new qcmds.QCmdStop());
                // qcmdprocessor.AppendToQueue(new qcmds.QCmdDisconnect());
                qcmdprocessor.AppendToQueue(new qcmds.QCmdFlashDFU());
            } else {
                LOGGER.log(Level.SEVERE, "There are no devices in Rescue Mode and you are not connected to any devices that can be put into DFU mode.");
            }
        } else if(boards.getDfuCount() == 1) {
            // ok here we do not know what board we are using as the user
            // already has a board started as DFU
            BoardDetail boardDetail = boards.getDfuBoard();
            prefs.boards.setSelectedBoard(boardDetail);
            //userSetsBoardType();

            axoloti.Axoloti.deletePrecompiledHeaderFile();
            // qcmdprocessor.AppendToQueue(new qcmds.QCmdStop());
            // qcmdprocessor.AppendToQueue(new qcmds.QCmdDisconnect());
            qcmdprocessor.AppendToQueue(new qcmds.QCmdFlashDFU());
        } else {
            LOGGER.log(Level.SEVERE, "Too many devices in Rescue Mode:\nPlease make sure only one board in rescue mode is connected.");
            //LOGGER.log(Level.SEVERE, "No devices in Rescue Mode detected. To bring Ksoloti Core into Rescue Mode:\n1. Remove power.\n2. Hold down button S1 then connect the USB prog port to your computer.\nThe LEDs will stay off when in Rescue Mode.");
        }
    }


    private void jMenuItemFlashUserActionPerformed(java.awt.event.ActionEvent evt) {
        String firmwareBinName = prefs.boards.getFirmwareBinFilename();
        flashUsingSDRam(firmwareBinName); 
    }


    private void jMenuItemFCompileActionPerformed(java.awt.event.ActionEvent evt) {
        qcmdprocessor.AppendToQueue(new qcmds.QCmdCompileFirmware());
    }




    private void jMenuItemFlashDefaultActionPerformed(java.awt.event.ActionEvent evt) {
        String firmwareBinName = prefs.boards.getFirmwareBinFilename();
        flashUsingSDRam(firmwareBinName); 
    }


    private void jMenuItemMountActionPerformed(java.awt.event.ActionEvent evt) {
        qcmdprocessor.AppendToQueue(new QCmdStop());
        qcmdprocessor.AppendToQueue(new QCmdStartMounter(true));
        qcmdprocessor.AppendToQueue(new QCmdDisconnect());
    }


    public void OpenURL() {
        String uri = JOptionPane.showInputDialog(this, "Enter URL:");
        if (uri == null) {
            return;
        }
        try {
            InputStream input = new URI(uri).toURL().openStream();
            String name = uri.substring(uri.lastIndexOf("/") + 1, uri.length());
            PatchGUI.OpenPatch(name, input);
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, "Invalid URL {0}\n{1}", new Object[]{uri, ex});
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, "Invalid URL {0}\n{1}", new Object[]{uri, ex});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to open URL {0}\n{1}", new Object[]{uri, ex});
        }
    }


    public void NewPatch() {
        PatchGUI patch1 = new PatchGUI();
        PatchFrame pf = new PatchFrame(patch1, qcmdprocessor);
        patch1.PostContructor();
        patch1.setFileNamePath("untitled");
        pf.setVisible(true);
    }

    public void NewBank() {
        PatchBank b = new PatchBank();
        b.setVisible(true);
    }


    private axoloti.menus.FileMenu fileMenu;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler3;
    private axoloti.menus.HelpMenu helpMenu1;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JToggleButton jToggleButtonConnect;
    private javax.swing.JPopupMenu.Separator jDevSeparator;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    // private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelCPUID;
    private javax.swing.JLabel jLabelFlags;
    // private javax.swing.JLabel jLabelFirmwareID;
    private javax.swing.JLabel jLabelIcon;
    private javax.swing.JLabel jLabelPatch;
    private javax.swing.JLabel jLabelProgress;
    private javax.swing.JLabel jLabelSDCardPresent;
    private javax.swing.JLabel jLabelVoltages;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuBoard;
    private javax.swing.JMenu jMenuEdit;
    private javax.swing.JMenu jMenuFirmware;
    private javax.swing.JMenuItem jMenuItemCopy;
    private javax.swing.JMenuItem jMenuItemFCompile;
    private javax.swing.JMenuItem jMenuItemFConnect;
    private javax.swing.JMenuItem jMenuItemFDisconnect;
    private javax.swing.JMenuItem jMenuItemFlashDFU;
    private javax.swing.JMenuItem jMenuItemFlashDefault;
    private javax.swing.JMenuItem jMenuItemFlashUser;
    private javax.swing.JMenuItem jMenuItemMount;
    private javax.swing.JMenuItem jMenuItemPanic;
    private javax.swing.JMenuItem jMenuItemPing;
    private javax.swing.JMenuItem jMenuItemRefreshFWID;
    private javax.swing.JMenuItem jMenuItemSelectCom;
    private javax.swing.JPanel jPanelHeader;
    private javax.swing.JPanel jPanelIconColumn;
    private javax.swing.JPanel jPanelButtonsColumn;
    private javax.swing.JPanel jPanelInfoColumn;
    private javax.swing.JPanel jPanelProgress;
    private javax.swing.JProgressBar jProgressBar1;
    private ScrollPaneComponent jScrollPaneLog;
    private javax.swing.JTextPane jTextPaneLog;
    private axoloti.menus.WindowMenu windowMenu1;

    public void SetProgressValue(int i) {
        jProgressBar1.setValue(i);
    }


    public void SetProgressMessage(String s) {
        jLabelProgress.setText(s);
    }


    @Override
    public void ShowDisconnect() {
        ShowConnectDisconnect(false);
    }


    @Override
    public void ShowConnect() {
        ShowConnectDisconnect(true);
        DeviceConnector.getDeviceConnector().cancel();
    }

    public void LibraryEntryChanged(AxolotiLibraryWatcher.AxolotiLibraryChangeType changeType, Path path) {
        LOGGER.log(Level.INFO, "Library entry {0} : {1}", new Object[]{changeType, path});
    }

    private void ShowConnectDisconnect(boolean connect) {
        jToggleButtonConnect.setSelected(connect);

        if(currentConnectedState != connect ) {
            currentConnectedState = connect;
            if (connect) {
                jToggleButtonConnect.setText("Connected");
                ShowConnectionFlags(USBBulkConnection.GetConnection().GetConnectionFlags());
            }
            else {
                jToggleButtonConnect.setText("Connect");
                setCpuID();
                jLabelVoltages.setText(" ");
                v5000c = 0;
                vdd00c = 0;
                patchIndex = -4;
                jLabelSDCardPresent.setText(" ");
                jLabelFlags.setText(" ");
                jLabelPatch.setText(" ");
            }

            jToggleButtonConnect.setSelected(connect);
            jMenuItemFDisconnect.setEnabled(connect);

            jMenuItemFConnect.setEnabled(!connect);
            // jMenuItemSelectCom.setEnabled(!connect);

            jMenuItemMount.setEnabled(connect);
            jMenuItemFlashDefault.setEnabled(connect);
            jMenuItemFlashUser.setEnabled(connect);

            if (prefs.getRestartRequired()) {
                disableConnectUntilRestart();
            }
        }
    }


    public void Quit() {
        while (!DocumentWindowList.GetList().isEmpty()) {
            if (DocumentWindowList.GetList().get(0).AskClose()) {
                return;
            }
        }
        prefs.SavePrefs(true);
        if (DocumentWindowList.GetList().isEmpty()) {
            System.exit(0);
        }
    }


    public void setCpuID() {
        BoardDetail boardDetail = MainFrame.prefs.getBoards().getSelectedBoardDetail();

        if(boardDetail.name != null) {
            jLabelCPUID.setText("Board Name:    " + boardDetail.name);
            jLabelCPUID.setToolTipText("Showing the name defined in Board > Select Device... > Name.\n" +
                                    "This setting is saved in the local ksoloti.prefs file.");
        } else {
            jLabelCPUID.setText("Board ID:    " + boardDetail.serialNumber);
            jLabelCPUID.setToolTipText("Showing board ID of the currently connected Core.\n" +
                                        "You can name your Core by disconnecting it from\n" +
                                        "the Patcher, then going to Board > Select Device... > Name.\n" + 
                                        "Press Enter in the Name textfield to confirm the entry.");
        }

        /* Update listeners */
        ShowUnitName(jLabelCPUID.getText());
    }


    public void updateLinkFirmwareID() {
        LinkFirmwareID = FirmwareID.getFirmwareID();
        // TargetFirmwareID = LinkFirmwareID;
        // jLabelFirmwareID.setText("Firmware ID: " + LinkFirmwareID);
        LOGGER.log(Level.INFO, "Patcher linked to firmware {0}", LinkFirmwareID);
    }



    void setFirmwareID(String firmwareId) {
        TargetFirmwareID = firmwareId;
        /* If LinkfirmwareID is a valid 8-digit hex number but not equal to the new firmwareId */
        if (this.LinkFirmwareID.length() != 8) {
            LOGGER.log( Level.WARNING, 
                "The Patcher currently does not contain a valid binary to check the firmware against.\n"
                + "If you are currently modifying the firmware:\n"
                + "- Activate \'Expert Mode\' (see below).\n"
                + "- Run \'Board > Firmware > Compile\' and make sure there are no compilation errors.\n"
                + "\nTo activate Expert Mode:\n"
                + "- Close the Patcher.\n"
                + "- Go inside the Patcher folder (or on Mac: Ksoloti.app/Contents/Resources/)\n"
                + "  and open \'ksoloti.prefs\' in a text editor.\n"
                + "- Replace the line <ExpertMode>false</ExpertMode> with <ExpertMode>true</ExpertMode>.\n"
                + "- Restart the Patcher.\n"
            );
            qcmdprocessor.AppendToQueue(new QCmdDisconnect());
            ShowDisconnect();
        }
        else if (!firmwareId.equals(this.LinkFirmwareID)) {
            if (!prefs.getFirmwareWarnDisable()) {
                LOGGER.log(Level.WARNING, "Firmware version mismatch! Please update the firmware.");
                LOGGER.log(Level.WARNING, "Core running {0} <-> Patcher linked to {1}", new Object[]{firmwareId, this.LinkFirmwareID});
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        interactiveFirmwareUpdate();
                    }
                });
            } else {
                LOGGER.log(Level.SEVERE, "Firmware mismatch ignored due to preferences");
            }
        }
    }


    private int patchIndex = -3;

    public void showPatchIndex(int index) {
        if (patchIndex != index) {
            patchIndex = index;
            String s;
            switch (patchIndex) {
                case -1:
                    s = "Running SD card startup patch";
                    break;
                case -2:
                    s = "Running internal startup patch";
                    break;
                case -3:
                    s = "Running SD card patch";
                    break;
                case -4:
                    s = "Running live patch";
                    break;
                case -5:
                    s = " ";
                    break;
                default:
                    s = "Running patch #" + patchIndex;
            }
            jLabelPatch.setText(s);
        }
    }


    private int v5000c = 0;
    private int vdd00c = 0;

    public void setVoltages(float v50, float vdd, boolean warning) {
        int v5000 = (int) (v50 * 100.0f);
        int vdd00 = (int) (vdd * 100.0f);
        boolean upd = false;
        if (((v5000c - v5000) > 1) || ((v5000 - v5000c) > 1)) {
            v5000c = v5000;
            upd = true;
        }
        if (((vdd00c - vdd00) > 1) || ((vdd00 - vdd00c) > 1)) {
            vdd00c = vdd00;
            upd = true;
        }
        if (upd) {
            jLabelVoltages.setText(String.format(
                "Voltage Monitor:   %.2fV   %.2fV", v5000c / 100.0f, vdd00c / 100.0f));
        }

        if (warning) {
            jLabelVoltages.setEnabled(false);
        } else {
            jLabelVoltages.setEnabled(true);
        }
    }


    public void interactiveFirmwareUpdate() {
        BoardDetail boardDetail = prefs.boards.getSelectedBoardDetail();

        if(boardDetail.needsUpdate) {
            Object[] options = {"Update", "Cancel"};
            int s = JOptionPane.showOptionDialog(this,
                    "Firmware mismatch detected!\n"
                    + "Update the firmware now?\n"
                    + "This process will put the board into DFU mode.\n"
                    + "Do not interrupt until the LEDs stop blinking and the patcher reconnects.\n",
                    "Firmware Update",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);

            if (s == 0) {
                qcmdprocessor.AppendToQueue(new QCmdBringToDFUMode());
                qcmdprocessor.AppendToQueue(new QCmdFlashDFU());
            }else {
                USBBulkConnection.GetConnection().disconnect();
            }
        }
        else {
            Object[] options = {"Update", "Cancel"};
            int s = JOptionPane.showOptionDialog(this,
                    "Firmware mismatch detected!\n"
                    + "Update the firmware now?\n"
                    + "This process will cause a disconnect and the LEDs will blink for a while.\n"
                    + "Do not interrupt until the LEDs stop blinking and the patcher reconnects.\n",
                    "Firmware Update",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[1]);

            if (s == 0) {
                String firmwareBinName = prefs.boards.getFirmwareBinFilename();
                flashUsingSDRam(firmwareBinName);
            } else {
                DeviceConnector.getDeviceConnector().cancel();
                USBBulkConnection.GetConnection().disconnect();
            }
        }
    }


    public void disableConnectUntilRestart() {
        jToggleButtonConnect.setText("RESTART");
        jToggleButtonConnect.setSelected(false);
        jToggleButtonConnect.setEnabled(false);
        jMenuItemFConnect.setEnabled(false);
        jMenuItemFDisconnect.setEnabled(false);
    }
    
    
    public void refreshAppIcon() {
        Constants.createAppIcon();
        setIconImage(Constants.APP_ICON.getImage());
        jLabelIcon.setIcon(Constants.APP_ICON);
    }


    public void populateInfoColumn() {

        jPanelInfoColumn.removeAll();

        jPanelInfoColumn.add(jLabelCPUID);
        jPanelInfoColumn.add(jLabelVoltages);

        jPanelInfoColumn.add(jLabelSDCardPresent);

        if(prefs.boards.getFirmware() == FirmwareType.USBAudio) {
            jPanelInfoColumn.add(jLabelFlags);
        }

        jPanelInfoColumn.add(jLabelPatch);
    }


    public QCmdProcessor getQcmdprocessor() {
        return qcmdprocessor;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.startsWith("open:")) {
            String fn = cmd.substring(5);
            File f = new File(fn);
            if (f.exists()) {
                if (fn.endsWith(".axp") || fn.endsWith(".axs") || fn.endsWith(".axh")) {
                    PatchGUI.OpenPatch(f);
                }
                else if (fn.endsWith(".axb")) {
                    PatchBank.OpenBank(f);
                }
                else if (fn.endsWith(".axo")) {
                    System.out.println("opening .axo files not implemented yet");
                    // TODO
                }
            }
            else {
                LOGGER.log(Level.WARNING, "File not found.");
            }
        }
    }


    public FileManagerFrame getFilemanager() {
        return filemanager;
    }


    // public AxolotiRemoteControl getRemote() {
    //     return remote;
    // }


    public KeyboardFrame getKeyboard() {
        return keyboard;
    }


    public void SetGrabFocusOnSevereErrors(boolean b) {
        bGrabFocusOnSevereErrors = b;
    }


    @Override
    public void ShowSDCardMounted() {
        jLabelSDCardPresent.setText("SD Card Connected");
        jMenuItemMount.setEnabled(true);
    }


    @Override
    public void ShowSDCardUnmounted() {
        jLabelSDCardPresent.setText("No SD Card");
        jMenuItemMount.setEnabled(false);
    }


    @Override
    public void ShowConnectionFlags(int connectionFlags) {
        //boolean dspOverload = 0 != (connectionFlags & 1);
        boolean usbBuild    = 0 != (connectionFlags & 2);
        boolean usbActive   = 0 != (connectionFlags & 4);
        boolean usbUnder    = 0 != (connectionFlags & 8);
        boolean usbOver     = 0 != (connectionFlags & 16);
        boolean usbError    = 0 != (connectionFlags & 32);

        StringBuilder flags = new StringBuilder();

        if  (usbBuild) {
            flags.append("USB Audio");
            if (usbActive) {
                flags.append(" Active");
                if (usbError) {
                    flags.append(" (Error)");
                }
                else {
                    if (usbUnder) {
                        flags.append(", Underruns detected");
                    }
                    if (usbOver) {
                        flags.append(", Overruns detected");
                    }
                }
            }
            else {
                flags.append(" Inactive");
            }
        } 

        jLabelFlags.setText(flags.toString());
    }


    private ArrayList<UnitNameListener> uncmls = new ArrayList<UnitNameListener>();

    public void addUnitNameListener(UnitNameListener uncml) {
        uncmls.add(uncml);
        uncml.ShowUnitName(jLabelCPUID.getText());
    }


    public void removeUnitNameListener(UnitNameListener uncml) {
        uncmls.remove(uncml);
    }


    public void ShowUnitName(String unitName) {
        for (UnitNameListener uncml : uncmls) {
            uncml.ShowUnitName(unitName);
        }
    }
}
