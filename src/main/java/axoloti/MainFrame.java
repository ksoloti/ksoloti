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
import axoloti.dialogs.KeyboardNavigableOptionPane;
import axoloti.dialogs.PatchBank;
import axoloti.listener.BoardIDNameListener;
import axoloti.listener.ConnectionFlagsListener;
import axoloti.listener.ConnectionStatusListener;
import axoloti.listener.SDCardMountStatusListener;
import axoloti.object.AxoObjects;
import axoloti.ui.Theme;
import axoloti.usb.Usb;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Constants;
import axoloti.utils.FirmwareID;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.BoundedRangeModel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Box.Filler;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.stream.Format;

import com.formdev.flatlaf.FlatClientProperties;

import qcmds.CommandManager;
import qcmds.QCmdBringToDFUMode;
import qcmds.QCmdCompilePatch;
import qcmds.QCmdDisconnect;
import qcmds.QCmdGuiShowLog;
import qcmds.QCmdPing;
import qcmds.QCmdProcessor;
import qcmds.QCmdStart;
import qcmds.QCmdStartFlasher;
import qcmds.QCmdStartMounter;
import qcmds.QCmdStop;
import qcmds.QCmdTransmitGetFWVersion;
import qcmds.QCmdUploadFWSDRam;
import qcmds.QCmdUploadPatch;

/**
 *
 * @author Johannes Taelman
 */
public final class MainFrame extends javax.swing.JFrame implements ActionListener, ConnectionStatusListener, SDCardMountStatusListener, ConnectionFlagsListener, BoardIDNameListener {

    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());

    static public AxoObjects axoObjects;
    public static MainFrame mainframe;
    public static AxoJFileChooser fc;
    private PatchGUI currentLivePatch = null; /* Tracks which patch is currently live */

    boolean even = false;
    String LinkFirmwareID;
    KeyboardFrame keyboard;
    FileManagerFrame filemanager;
    Thread qcmdprocessorThread;
    static public Cursor transparentCursor;
    private final String[] args;
    JMenu favouriteMenu;
    boolean bGrabFocusOnSevereErrors = true;
    public boolean WarnedAboutFWCRCMismatch = false;
    private int patchIndex = -3;
    private int v5000c = 0;
    private int vdd00c = 0;

    public static Style styleSevere;
    public static Style styleWarning;
    public static Style styleInfo;

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
    private javax.swing.JLabel jLabelCPUID;
    private javax.swing.JLabel jLabelFlags;
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
    private javax.swing.JMenuItem jMenuItemEnterDFU;
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
    public static javax.swing.JTextPane jTextPaneLog;
    private axoloti.menus.WindowMenu windowMenu1;

    private boolean doAutoScroll = true;

    /* Usually we run all tests, as many may fail for same reason and you want
       a list of all affected files, but if you want to stop on first failure,
       flip this flag */
    public static boolean stopOnFirstFail = false;

    /**
     * Creates new form MainFrame
     *
     * @param args command line arguments
     */
    public MainFrame(String args[]) {
        this.args = args;

        Usb.initialize();

        initComponents();
        fileMenu.initComponents();
        setIconImage(Constants.APP_ICON.getImage());

        transparentCursor = getToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), null);

        mainframe = this;
        setVisible(true);

        fc = new AxoJFileChooser(Preferences.getInstance().getCurrentFileDirectory());

        final Style styleParent = jTextPaneLog.addStyle(null, null);
        jTextPaneLog.setFont(Constants.FONT_MONO);

        styleSevere = jTextPaneLog.addStyle("severe", styleParent);
        styleWarning = jTextPaneLog.addStyle("warning", styleParent);
        styleInfo = jTextPaneLog.addStyle("info", styleParent);

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
                    System.out.println(Instant.now() + " drag & drop:");
                    // @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    /* Cap max opened files to 32 */
                    int openedCount = 0, maxCount = 32;
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
                                    System.out.println(Instant.now() + " Opening .axo files not implemented yet");
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
                }
                catch (UnsupportedFlavorException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
                catch (IOException ex) {
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
                }
                else {
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
                }
                else {
                    // doAutoScroll will be set to true when user reaches at the bottom of document.
                    doAutoScroll = ((brm.getValue() + brm.getExtent()) == brm.getMaximum());
                }
            }
        });

        Handler logHandler = new Handler() {
            @Override
            public void publish(final LogRecord lr) {
                /* This entire block (the "else" part) needs to be executed on the EDT.
                   If we're not on the EDT, schedule *this entire logic* to run on the EDT. */
                if (!SwingUtilities.isEventDispatchThread()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            /* Now, call the publish method again. This time, it *will* be on the EDT. */
                            publish(lr);
                        }
                    });
                    return; /* Important: exit the current call as it's been re-dispatched. */
                }

                /* If we reach here, we are guaranteed to be on the Event Dispatch Thread (EDT). */
                try {
                    String txt;
                    String excTxt = "";
                    Throwable exc = lr.getThrown();

                    if (exc != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        exc.printStackTrace(ps);
                        excTxt = exc.toString();
                        excTxt = excTxt + "\n" + baos.toString("utf-8"); /* Ensure encoding for platform safety */
                    }

                    if (lr.getMessage() == null) {
                        txt = excTxt;
                    }
                    else {
                        /* Handle MessageFormat carefully, especially if parameters are null */
                        if (lr.getParameters() != null) {
                            txt = java.text.MessageFormat.format(lr.getMessage(), lr.getParameters());
                        }
                        else {
                            txt = lr.getMessage();
                        }
            
                        if (excTxt.length() > 0) {
                            txt = txt + "," + excTxt;
                        }
                    }
            
                    String formattedMessage = txt + "\n";
            
                    /* Get the document length just before insertion to ensure it's current. */
                    int currentLength = jTextPaneLog.getDocument().getLength();
            
                    if (lr.getLevel() == Level.SEVERE) {
                        jTextPaneLog.getDocument().insertString(currentLength, formattedMessage, styleSevere);
                        if (bGrabFocusOnSevereErrors) {
                            doAutoScroll = true;
                            MainFrame.this.toFront();
                        }
                    }
                    else if (lr.getLevel() == Level.WARNING) {
                        jTextPaneLog.getDocument().insertString(currentLength, formattedMessage, styleWarning);
                    }
                    else {
                        jTextPaneLog.getDocument().insertString(currentLength, formattedMessage, styleInfo);
                    }

                    if (doAutoScroll) {
                        jTextPaneLog.setCaretPosition(jTextPaneLog.getDocument().getLength());
                    }
                }
                catch (BadLocationException ex) {
                    System.err.println(Instant.now() + " BadLocationException when logging to GUI: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
                catch (UnsupportedEncodingException ex) {
                    System.err.println(Instant.now() + " UnsupportedEncodingException in log handler: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
                catch (Exception ex) {
                    System.err.println(Instant.now() + " Unexpected exception in log handler: " + ex.getMessage());
                    ex.printStackTrace(System.err);
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

        SimpleFormatter formatter = new SimpleFormatter();
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tFT%1$tT.%1$tN  %4$s: %5$s%n");
        logHandler.setFormatter(formatter);
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

        if (!Preferences.getInstance().getExpertMode()) {
            jMenuItemRefreshFWID.setVisible(false);
        }

        // jMenuItemEnterDFU.setVisible(Axoloti.isDeveloper());
        /* Enter Rescue Mode option always available */
        jMenuItemEnterDFU.setVisible(true);

        /* When in Developer mode, make default Flash option invisible to avoid confusion */
        jMenuItemFlashDefault.setVisible(!(Axoloti.isDeveloper()));

        jMenuItemFlashUser.setVisible(Axoloti.isDeveloper());
        jMenuItemFCompile.setVisible(Axoloti.isDeveloper() || Preferences.getInstance().getExpertMode());

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
        Runnable initr = new Runnable() {
            @Override
            public void run() {
                try {
                    populateMainframeTitle();
                    LOGGER.log(Level.WARNING, "Patcher version {0} | Build time {1}\n", new Object[]{Version.AXOLOTI_VERSION, Version.AXOLOTI_BUILD_TIME});

                    if (Preferences.getInstance().getExpertMode()) {
                        LOGGER.log(Level.WARNING,
                            "Expert Mode is enabled in ksoloti.prefs. The following options are now available:\n" + 
                            "- Compile firmware, Refresh firmware ID (Board -> Firmware)\n" + 
                            "- Generate and/or compile patch code, simulate lock/unlock, also while no Core is connected (patch windows -> Patch)\n" + 
                            "- Remove read-only restrictions: Edit and save to read-only libraries (axoloti-factory, *oloti-contrib, ksoloti-objects)\n" + 
                            "- Test-compile all patches in all libraries, or all patches (recursively) in specified folder (File -> Test Compilation)\n"
                        );
                    }

                    if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
                        LOGGER.log(Level.WARNING, ">>> Axoloti Legacy Mode <<<\n");
                    }

                    if (Preferences.getInstance().getFirmwareMode().contains("SPILink")) {
                        LOGGER.log(Level.WARNING, ">>> SPILink-enabled firmware <<<\nPins PB3, PB4, PD5, PD6 are occupied by SPILink communication in this firmware mode!\n");
                    }

                    if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
                        LOGGER.log(Level.WARNING, ">>> USBAudio-enabled firmware <<<\n");
                    }

                    if (Preferences.getInstance().getFirmwareMode().contains("I2SCodec")) {
                        LOGGER.log(Level.WARNING, ">>> I2SCodec-enabled firmware <<<\nPins PA15, PB3, PB4, PD6 are occupied by I2S communication in this firmware mode!\n");
                    }

                    updateLinkFirmwareID();

                    qcmdprocessorThread = new Thread(QCmdProcessor.getQCmdProcessor());
                    qcmdprocessorThread.setName("QCmdProcessor");
                    qcmdprocessorThread.start();
                    USBBulkConnection.GetConnection().addConnectionStatusListener(MainFrame.this);
                    USBBulkConnection.GetConnection().addSDCardMountStatusListener(MainFrame.this);
                    USBBulkConnection.GetConnection().addConnectionFlagsListener(MainFrame.this);
                    USBBulkConnection.GetConnection().addBoardIDNameListener(MainFrame.this);

                    ShowDisconnect();
                        new SwingWorker<Boolean, String>() {
                            @Override
                            protected Boolean doInBackground() throws Exception {
                                return USBBulkConnection.GetConnection().connect();
                            }

                            @Override
                            protected void done() {
                                try {
                                    boolean success = get();
                                    if (success) {
                                        ShowConnect();
                                    }
                                    else {
                                        ShowDisconnect();
                                    }
                                }
                                catch (Exception e) {
                                    LOGGER.log(Level.SEVERE, "Initial connection worker crashed:", e);
                                    ShowDisconnect();
                                }
                            }
                        }.execute();

                    // Axoloti user library, ask user if they wish to upgrade, or do manual
                    // this allows them the opportunity to manually backup their files!
                    AxolotiLibrary ulib = Preferences.getInstance().getLibrary(AxolotiLibrary.AXOLOTI_CONTRIB_ID);
                    if (ulib != null) {
                        String cb = ulib.getCurrentBranch();
                        if (!cb.equalsIgnoreCase(ulib.getBranch())) {
                            LOGGER.log(Level.INFO, "Current axoloti-contrib library does not match specified version: {0} <-> {1}", new Object[]{cb, ulib.getBranch()});
                            int s = KeyboardNavigableOptionPane.showConfirmDialog(MainFrame.this,
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
                    AxolotiLibrary kso_ulib = Preferences.getInstance().getLibrary(AxolotiLibrary.KSOLOTI_CONTRIB_ID);
                    if (kso_ulib != null) {
                        String cb = kso_ulib.getCurrentBranch();
                        if (!cb.equalsIgnoreCase(kso_ulib.getBranch())) {
                            LOGGER.log(Level.INFO, "Current ksoloti-contrib library does not match specified version: {0} <-> {1}", new Object[]{cb, kso_ulib.getBranch()});
                            int s = KeyboardNavigableOptionPane.showConfirmDialog(MainFrame.this,
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
                    ulib = Preferences.getInstance().getLibrary(AxolotiLibrary.AXOLOTI_FACTORY_ID);
                    if (ulib != null) {
                        String cb = ulib.getCurrentBranch();
                        if (!cb.equalsIgnoreCase(ulib.getBranch())) {
                            LOGGER.log(Level.INFO, "Current axoloti-factory library does not match specified version, upgrading... ({0} -> {1})", new Object[]{cb, ulib.getBranch()});
                            ulib.upgrade();
                        }
                    }

                    // ksoloti-objects library force and upgrade
                    // Im stashing changes here, just in case, but in reality users should not be altering factory 
                    ulib = Preferences.getInstance().getLibrary(AxolotiLibrary.KSOLOTI_OBJECTS_ID);
                    if (ulib != null) {
                        String cb = ulib.getCurrentBranch();
                        if (!cb.equalsIgnoreCase(ulib.getBranch())) {
                            LOGGER.log(Level.INFO, "Current ksoloti-objects library does not match specified version, upgrading... ({0} -> {1})", new Object[]{cb, ulib.getBranch()});
                            ulib.upgrade();
                        }
                    }

                    boolean autoSyncMessageDone = false;

                    for (AxolotiLibrary lib : Preferences.getInstance().getLibraries()) {
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
                    for (AxolotiLibrary lib : Preferences.getInstance().getLibraries()) {
                        lib.reportStatus();
                    }
                    LOGGER.log(Level.INFO, "Done checking library status.\n");

                    axoObjects = new AxoObjects();
                    axoObjects.LoadAxoObjects();

                // }
                // catch (BindException e) {
                    // e.printStackTrace();
                    // System.exit(1);
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
                                    }
                                    else {
                                        PatchGUI.OpenPatch(f);
                                    }
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        EventQueue.invokeLater(r);
                    }
                }
                else if (arg.endsWith(".axo")) {
                    System.out.println(Instant.now() + " Opening .axo files not implemented yet");
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

        if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
            tstring = "Axoloti";
        }
        else {
            tstring = "Ksoloti";
        }

        if (Axoloti.isDeveloper()) {
            tsuffix += "Developer";
        }

        if (Preferences.getInstance().getExpertMode()) {
            if (tsuffix.length() > 0) {
                tsuffix += ", ";
            }
            tsuffix += "Expert Mode";
        }

        if (Preferences.getInstance().getFirmwareMode().contains("SPILink")) {
            if (tsuffix.length() > 0) {
                tsuffix += ", ";
            }
            tsuffix += "SPILink";
        }

        if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
            if (tsuffix.length() > 0) {
                tsuffix += ", ";
            }
            tsuffix += "USBAudio";
        }

        if (Preferences.getInstance().getFirmwareMode().contains("I2SCodec")) {
            if (tsuffix.length() > 0) {
                tsuffix += ", ";
            }
            tsuffix += "I2SCodec";
        }

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

    void flashUsingSDRam(String fname_flasher, String pname) {
        updateLinkFirmwareID();

        File f = new File(fname_flasher);
        File p = new File(pname);

        if (!f.canRead()) {
            LOGGER.log(Level.SEVERE, "Cannot read flasher, please compile firmware! (File: {0})", fname_flasher);
            return;
        }
        if (!p.canRead()) {
            LOGGER.log(Level.SEVERE, "Cannot read firmware, please compile firmware! (File: {0})", pname);
            return;
        }

        setCurrentLivePatch(null);
        
        CommandManager.getInstance().startLongOperation();
        new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    QCmdUploadFWSDRam uploadFwCmd = new QCmdUploadFWSDRam(p);
                    uploadFwCmd.Do(USBBulkConnection.GetConnection());
                    boolean completed = uploadFwCmd.waitForCompletion();
                    if (!completed) {
                        LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM timed out");
                        return false;
                    }
                    if (!uploadFwCmd.isSuccessful()) {
                        LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed");
                        return false;
                    }

                    QCmdUploadPatch uploadPatchCmd = new QCmdUploadPatch(f);
                    uploadPatchCmd.Do(USBBulkConnection.GetConnection());
                    completed = uploadPatchCmd.waitForCompletion();
                    if (!completed) {
                        LOGGER.log(Level.SEVERE, "Flasher Patch upload timed out");
                        return false;
                    }
                    if (!uploadPatchCmd.isSuccessful()) {
                        LOGGER.log(Level.SEVERE, "Flasher Patch upload failed");
                        return false;
                    }
                    return true;

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Exception during firmware/flasher upload worker:", e);
                    return false;
                }
            }

            @Override
            protected void done() {
                CommandManager.getInstance().endLongOperation();
                try {
                    boolean success = get();
                    if (success) {
                        /* If code reaches here, the background process was successful */
                        QCmdStartFlasher startFlasherCmd = new QCmdStartFlasher();
                        startFlasherCmd.Do(USBBulkConnection.GetConnection());
                        // startFlasherCmd.waitForCompletion();
                        // if (startFlasherCmd.isSuccessful()) {
                        //     LOGGER.log(Level.INFO, "Flasher Patch started.");
                        // } else {
                        //     LOGGER.log(Level.SEVERE, "Flasher Patch start failed.");
                        //     return;
                        // }
                        // LOGGER.log(Level.INFO, "Firmware and Flasher upload successful, disconnecting for flash write...");
                        ShowDisconnect();
                        // QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdDisconnect());
                        // QCmdProcessor.getQCmdProcessor().WaitQueueFinished();
                    } else {
                        /* Above error messages should have handled all failures */
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Firmware update process was interrupted.", e);
                    Thread.currentThread().interrupt(); // Restore interrupt status
                } catch (java.util.concurrent.ExecutionException e) {
                    LOGGER.log(Level.SEVERE, "An unexpected error occurred in background task: " + e.getCause().getMessage(), e.getCause());
                }
            }
        }.execute();
    }

    private void initComponents() {

        jPanelHeader = new javax.swing.JPanel();
        jPanelIconColumn = new javax.swing.JPanel();
        jPanelButtonsColumn = new javax.swing.JPanel();
        jPanelInfoColumn = new javax.swing.JPanel();
        jLabelIcon = new javax.swing.JLabel();
        jButtonClear = new javax.swing.JButton();
        jToggleButtonConnect = new javax.swing.JToggleButton();
        jLabelCPUID = new javax.swing.JLabel();
        jLabelFlags = new javax.swing.JLabel();
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
        jMenuItemEnterDFU = new javax.swing.JMenuItem();
        jMenuItemFlashUser = new javax.swing.JMenuItem();
        windowMenu1 = new axoloti.menus.WindowMenu();
        helpMenu1 = new axoloti.menus.HelpMenu();

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
        jMenuItemSelectCom.setText("Select Device...");
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

        jMenuItemEnterDFU.setMnemonic('E');
        jMenuItemEnterDFU.setText("Enter Rescue Mode");
        jMenuItemEnterDFU.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemEnterDFUActionPerformed(evt);
            }
        });
        jMenuFirmware.add(jMenuItemEnterDFU);


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
        QCmdProcessor.getQCmdProcessor().Panic();
    }

    private void jMenuItemPingActionPerformed(java.awt.event.ActionEvent evt) {
        QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdPing());
        // QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdPing(true)); // no-disconnect ping for debug
    }

    private void jMenuItemFDisconnectActionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemFDisconnect.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                USBBulkConnection.GetConnection().disconnect();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    ShowDisconnect();
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Disconnect worker failed unexpectedly:", e);
                    ShowDisconnect();
                }
            }
        }.execute();
    }

    private void jMenuItemFConnectActionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItemFConnect.setEnabled(false);
        MainFrame.mainframe.SetProgressMessage("Connecting via menu...");
        QCmdProcessor.getQCmdProcessor().Panic();

        new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return USBBulkConnection.GetConnection().connect();
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        ShowConnect();
                    }
                    else {
                        ShowDisconnect();
                    }
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Connection worker failed unexpectedly:", e);
                    ShowDisconnect(); // Ensure UI reflects disconnected state
                }
            }
        }.execute();
    }

    private void jMenuItemSelectComActionPerformed(java.awt.event.ActionEvent evt) {
        USBBulkConnection.GetConnection().SelectPort();
    }

    private void jToggleButtonConnectActionPerformed(java.awt.event.ActionEvent evt) {
        jToggleButtonConnect.setEnabled(false);

        if (jToggleButtonConnect.isSelected()) { /* Attempting to connect */
            /* Guard against rapid "Connect" clicks if a disconnect was previously initiated */
            if (USBBulkConnection.GetConnection().isDisconnectRequested()) {
                // System.out.println(Instant.now() + " [DEBUG] Connection attempt ignored: A previous disconnection is still pending.");
                /* Revert the button's selected state as connection did not succeed */
                jToggleButtonConnect.setSelected(false);
                jToggleButtonConnect.setEnabled(true);
                return;
            }

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return USBBulkConnection.GetConnection().connect();
                }

                @Override
                protected void done() {
                    try {
                        get();
                    }
                    catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error during connection SwingWorker:", e);
                        USBBulkConnection.GetConnection().ShowDisconnect();
                    }
                    finally {
                        jToggleButtonConnect.setEnabled(true);
                    }
                }
            }.execute();

        }
        else { /* Attempting to disconnect */
            /* Set guard against rapid "Connect" clicks now that a disconnect was initiated */
            USBBulkConnection.GetConnection().setDisconnectRequested(true);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    USBBulkConnection.GetConnection().disconnect();
                    Thread.sleep(500); /* 500ms for disconnect to clear */
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        // System.out.println(Instant.now() + " [DEBUG] UI updated: Disconnected (successfully).");
                    }
                    catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error during disconnection SwingWorker:", e);
                        if (USBBulkConnection.GetConnection().isConnected()) {
                            USBBulkConnection.GetConnection().ShowConnect();
                            // System.out.println(Instant.now() + " [DEBUG] UI updated: Connected (disconnect failed, board still connected).");
                        }
                        else {
                            USBBulkConnection.GetConnection().ShowDisconnect();
                            // System.out.println(Instant.now() + " [DEBUG] UI updated: Disconnected (disconnect failed, but board is off).");
                        }
                    }
                    finally {
                        USBBulkConnection.GetConnection().setDisconnectRequested(false);
                        jToggleButtonConnect.setEnabled(true);
                        // System.out.println(Instant.now() + " [DEBUG] Disconnect request flag cleared and connect button re-enabled.");
                    }
                }
            }.execute();
        }
    }

    public boolean runAllTests() {

        boolean r1 = false;
        boolean r2 = false;
        
        try {
            r1 = runObjectTests();
            if (!r1 && stopOnFirstFail) {
                return r1;
            }
            r2 = runPatchTests();
            if (!r2 && stopOnFirstFail) {
                return r2;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return r1 && r2;
        }
        return r1 && r2;
    }

    public boolean runPatchTests() {
        SetGrabFocusOnSevereErrors(false);
        boolean result;

        AxolotiLibrary fLib = Preferences.getInstance().getLibrary(AxolotiLibrary.AXOLOTI_FACTORY_ID);
        if (fLib == null) {
            SetGrabFocusOnSevereErrors(true);
            return false;
        }
        result = runTestDir(new File(fLib.getLocalLocation() + "patches"));

        fLib = Preferences.getInstance().getLibrary(AxolotiLibrary.AXOLOTI_CONTRIB_ID);
        if (fLib == null) {
            SetGrabFocusOnSevereErrors(true);
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "patches"));

        fLib = Preferences.getInstance().getLibrary(AxolotiLibrary.KSOLOTI_OBJECTS_ID);
        if (fLib == null) {
            SetGrabFocusOnSevereErrors(true);
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "patches"));

        fLib = Preferences.getInstance().getLibrary(AxolotiLibrary.KSOLOTI_CONTRIB_ID);
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

        AxolotiLibrary fLib = Preferences.getInstance().getLibrary(AxolotiLibrary.AXOLOTI_FACTORY_ID);
        if (fLib == null) {
            return false;
        }
        result = runTestDir(new File(fLib.getLocalLocation() + "objects"));

        fLib = Preferences.getInstance().getLibrary(AxolotiLibrary.AXOLOTI_CONTRIB_ID);
        if (fLib == null) {
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "objects"));

        fLib = Preferences.getInstance().getLibrary(AxolotiLibrary.KSOLOTI_OBJECTS_ID);
        if (fLib == null) {
            return false;
        }
        result &= runTestDir(new File(fLib.getLocalLocation() + "objects"));

        fLib = Preferences.getInstance().getLibrary(AxolotiLibrary.KSOLOTI_CONTRIB_ID);
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
            /* Simple sorting method for easier diff-ability of logs */
            Arrays.sort(files, (f1, f2) -> f1.getAbsolutePath().compareToIgnoreCase(f2.getAbsolutePath()));

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
        
        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, new Format(2));
        boolean status = false;
        
        try {
            LOGGER.log(Level.INFO, "---------- Testing {0} ----------", f.getPath());

            PatchGUI patch1 = serializer.read(PatchGUI.class, f);
            PatchFrame pf = new PatchFrame(patch1);
            pf.createBufferStrategy(1);
            patch1.setFileNamePath(f.getPath());
            patch1.PostContructor();
            patch1.WriteCode(true); // generate code as own path/filename .cpp
            Thread.sleep(200); 
            LOGGER.log(Level.INFO, "Done generating code.");

            setCurrentLivePatch(null);

            File binFile = patch1.getBinFile();
            if (binFile.exists()) {
                /* Delete previous .bin to ensure waitForBinFile() below won't trigger false positive */
                binFile.delete();
            }

            CommandManager.getInstance().startLongOperation();
            QCmdCompilePatch cp = new QCmdCompilePatch(patch1); // compile as own path/filename .bin
            QCmdProcessor.getQCmdProcessor().AppendToQueue(cp);
            QCmdProcessor.getQCmdProcessor().WaitQueueFinished();
            CommandManager.getInstance().endLongOperation();
            if (patch1.waitForBinFile()) {
                // LOGGER.log(Level.INFO, "Done compiling patch.\n");
                /* If a Core is connected and test patch .bin could be created:
                stop patch, upload test patch .bin to RAM, start patch, report status */
                if (USBBulkConnection.GetConnection().isConnected()) {
                    QCmdUploadPatch uploadCmd = new QCmdUploadPatch(patch1.getBinFile());
                    uploadCmd.Do(USBBulkConnection.GetConnection());
                    boolean completed = uploadCmd.waitForCompletion();
                    if (!completed) {
                        LOGGER.log(Level.SEVERE, "Test patch upload timed out");
                        status = false;
                    }
                    if (!uploadCmd.isSuccessful()) {
                        LOGGER.log(Level.SEVERE, "Test patch upload failed");
                        status = false;
                    }
                    setCurrentLivePatch(patch1);
                    Thread.sleep(1000);

                    QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdPing());
                    float pct = patch1.getDSPLoadPercent();
                    if (pct < 1.0f) {
                        LOGGER.log(Level.SEVERE, "No DSP load detected\n");
                    }
                    else if (pct > 95.0f) {
                        LOGGER.log(Level.SEVERE, "High DSP load detected: {0}%\n", String.format("%.1f", pct));
                    }
                    else {
                        LOGGER.log(Level.INFO, "DSP load: {0}%\n", String.format("%.1f", pct));
                    }
                    Thread.sleep(1000);

                    QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdGuiShowLog());
                    QCmdProcessor.getQCmdProcessor().WaitQueueFinished();
                    Thread.sleep(100);
                    status = true;
                }
            }
            else {
                if (!USBBulkConnection.GetConnection().WaitSync()) {
                    USBBulkConnection.GetConnection().disconnect();
                }
                LOGGER.log(Level.INFO, "FAILED compiling patch binary.\n");
                status = false;
            }
            patch1.Close();
            pf.Close();
            Thread.sleep(200);

            SetGrabFocusOnSevereErrors(bGrabFocusOnSevereErrors);
            return status;
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "ERROR DURING PATCH TEST: " + f.getPath() + "\n", ex);
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
        Serializer serializer = new Persister(strategy, new Format(2));
        try {
            boolean status;
            PatchGUI patch1 = serializer.read(PatchGUI.class, f);
            new PatchFrame(patch1);
            patch1.setFileNamePath(f.getPath());
            patch1.PostContructor();
            status = patch1.save(f);
            if (status == false) {
                LOGGER.log(Level.SEVERE, "UPGRADE FAILED: {0}", f.getPath());
            }
            return status;
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "UPGRADE FAILED: " + f.getPath(), ex);
            return false;
        }
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        Quit();
    }

    private void jMenuItemRefreshFWIDActionPerformed(java.awt.event.ActionEvent evt) {
        updateLinkFirmwareID();
        if (USBBulkConnection.GetConnection().isConnected()) {
            QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdTransmitGetFWVersion());
            QCmdProcessor.getQCmdProcessor().WaitQueueFinished();
        }
    }

    private void jMenuItemFlashDFUActionPerformed(java.awt.event.ActionEvent evt) {
        if (Usb.isDFUDeviceAvailable()) {
            updateLinkFirmwareID();
            setCurrentLivePatch(null);
            QCmdProcessor.getQCmdProcessor().AppendToQueue(new qcmds.QCmdDisconnect());
            QCmdProcessor.getQCmdProcessor().AppendToQueue(new qcmds.QCmdFlashDFU());
        }
        else {
            LOGGER.log(Level.SEVERE, "No devices in Rescue Mode detected. To bring Ksoloti Core into Rescue Mode:\n1. Remove power.\n2. Hold down button S1 then connect the USB prog port to your computer.\nThe LEDs will stay off when in Rescue Mode.");
        }
    }

    private void jMenuItemFlashUserActionPerformed(java.awt.event.ActionEvent evt) {
        String fname = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "flasher" + File.separator + "flasher_build";
        String pname = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "build";
        if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
            fname += File.separator + "ksoloti_flasher.bin";
            pname += File.separator + "ksoloti";
        }
        else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
            fname += File.separator + "axoloti_flasher.bin";
            pname += File.separator + "axoloti";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("SPILink")) {
            pname += "_spilink";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
            pname += "_usbaudio";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("I2SCodec")) {
            pname += "_i2scodec";
        }
        pname += ".bin";
        flashUsingSDRam(fname, pname);
    }

    private void jMenuItemFCompileActionPerformed(java.awt.event.ActionEvent evt) {
        QCmdProcessor.getQCmdProcessor().AppendToQueue(new qcmds.QCmdCompileFirmware());
    }

    private void jMenuItemEnterDFUActionPerformed(java.awt.event.ActionEvent evt) {
        QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdBringToDFUMode());
        QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdDisconnect());
    }

    private void jMenuItemFlashDefaultActionPerformed(java.awt.event.ActionEvent evt) {

        String fname = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "flasher" + File.separator + "flasher_build";
        String pname = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "build";
        if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
            fname += File.separator + "ksoloti_flasher.bin";
            pname += File.separator + "ksoloti";
        }
        else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
            fname += File.separator + "axoloti_flasher.bin";
            pname += File.separator + "axoloti";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("SPILink")) {
            pname += "_spilink";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
            pname += "_usbaudio";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("I2SCodec")) {
            pname += "_i2scodec";
        }
        pname += ".bin";
        flashUsingSDRam(fname, pname);
    }

    private void jMenuItemMountActionPerformed(java.awt.event.ActionEvent evt) {
        String fname = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "mounter" + File.separator + "mounter_build";
        if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
            fname += File.separator + "ksoloti_mounter.bin";
        }
        else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
            fname += File.separator + "axoloti_mounter.bin";
        }
        File f = new File(fname);
        if (f.canRead()) {
            setCurrentLivePatch(null);
            try {
                QCmdUploadPatch uploadMounterCmd = new QCmdUploadPatch(f);
                uploadMounterCmd.Do(USBBulkConnection.GetConnection());
                boolean completed = uploadMounterCmd.waitForCompletion();
                if (!completed) {
                    LOGGER.log(Level.SEVERE, "Mounter upload timed out.");
                    return;
                }
                if (!uploadMounterCmd.isSuccessful()) {
                    LOGGER.log(Level.SEVERE, "Mounter upload failed.");
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "An error occurred while uploading Mounter.");
            }

            try {
                QCmdStartMounter startMounterCmd = new QCmdStartMounter();
                startMounterCmd.Do(USBBulkConnection.GetConnection());
                // boolean completed = startMounterCmd.waitForCompletion();
                // if (!completed) {
                //     LOGGER.log(Level.SEVERE, "Mounter start timed out.");
                //     return;
                // }
                // if (!startMounterCmd.isSuccessful()) {
                //     LOGGER.log(Level.SEVERE, "Mounter start failed.");
                //     return;
                // }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "An error occurred while starting Mounter.");
                return;
            } finally {
                // QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdDisconnect());
                ShowDisconnect();
            }
        }
        else {
            LOGGER.log(Level.SEVERE, "Cannot read Mounter firmware. Please compile firmware first!\n(File: {0})", fname);
        }
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
        }
        catch (MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, "Invalid URL {0}\n{1}", new Object[]{uri, ex});
        }
        catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, "Invalid URL {0}\n{1}", new Object[]{uri, ex});
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to open URL {0}\n{1}", new Object[]{uri, ex});
        }
    }

    public PatchGUI getCurrentLivePatch() {
        return currentLivePatch;
    }

    public void setCurrentLivePatch(PatchGUI newLivePatch) {
        // Ensure this method is called on the EDT, as it manipulates GUI components
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setCurrentLivePatch(newLivePatch));
            return;
        }

        // Only proceed if the live patch is actually changing
        if (this.currentLivePatch == newLivePatch) {
            // If the same patch is being set live again, or if it's already null, just update states.
            updatePatchLiveStates();
            return;
        }

        // --- Step 1: Unlock the previously live patch (if any) ---
        if (this.currentLivePatch != null) {
            this.currentLivePatch.Unlock(); // GUI-side unlock (cascades down to disable editing)
            LOGGER.log(Level.INFO, "Unlocked previous live patch: " + this.currentLivePatch.getFileNamePath());
            try {
                QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdStop());
                QCmdProcessor.getQCmdProcessor().WaitQueueFinished(); // Wait for MCU to process stop command
                LOGGER.log(Level.INFO, "Sent QCmdStop to Core for previous patch.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to send QCmdStop to Core for previous patch: " + this.currentLivePatch.getFileNamePath(), e);
            }
        }

        // --- Step 2: Update the reference to the new live patch ---
        this.currentLivePatch = newLivePatch;

        // --- Step 3: Lock the newly live patch (if any) and send MCU commands ---
        if (this.currentLivePatch != null) {
            // Send QCmdStart and QCmdLock to MCU *before* GUI-side Lock()
            // These must be handled by the QCmdProcessor.
            try {
                // Ensure the patch is actually started on the MCU
                QCmdStart startCmd = new QCmdStart(this.currentLivePatch);
                startCmd.Do(USBBulkConnection.GetConnection());
                if (startCmd.isSuccessful()) {
                    LOGGER.log(Level.INFO, "Patch started: " + this.currentLivePatch.getFileNamePath());
                } else {
                    LOGGER.log(Level.SEVERE, "Patch start failed for " + this.currentLivePatch.getFileNamePath());
                    return;
                }

                this.currentLivePatch.Lock(); // GUI-side lock (cascades down to disable editing)
                LOGGER.log(Level.INFO, "Locked new live patch: " + this.currentLivePatch.getFileNamePath());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to send QCmdStart/QCmdLock to Core for patch: " + this.currentLivePatch.getFileNamePath(), e);
                // Critical error: The patch is not truly live on the MCU.
                // Revert the GUI state to reflect this.
                if (this.currentLivePatch != null) { // Defensive check
                    this.currentLivePatch.Unlock(); // Unlock GUI
                }
                this.currentLivePatch = null; // Clear live patch reference
                LOGGER.log(Level.SEVERE, "Patch could not be set live on Core. Reverting GUI state.");
            }
        } else {
            LOGGER.log(Level.INFO, "No patch is currently live.");
        }

        // --- Step 4: Update all open patch windows about the new live state ---
        updatePatchLiveStates();
    }

    public void updatePatchLiveStates() { /* Signals all open patch windows the current live patch state */
        for (DocumentWindow docWindow : DocumentWindowList.GetList()) {
            if (docWindow != null && docWindow instanceof PatchFrame) {
                PatchFrame frame = (PatchFrame) docWindow;
                boolean isLive = (frame.getPatch() == currentLivePatch);
                frame.GUIShowLiveState(isLive);
            }
        }
    }

    public void setAllPatchFramesActionButtonsEnabled(boolean enabled) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setAllPatchFramesActionButtonsEnabled(enabled));
            return;
        }
        for (DocumentWindow docWindow : DocumentWindowList.GetList()) {
            if (docWindow instanceof PatchFrame) {
                PatchFrame frame = (PatchFrame) docWindow;
                frame.setActionButtonsEnabled(enabled);
            }
        }
    }

    public void NewPatch() {
        PatchGUI patch1 = new PatchGUI();
        PatchFrame pf = new PatchFrame(patch1);
        patch1.PostContructor();
        patch1.setFileNamePath("untitled");
        pf.setVisible(true);
    }

    public void NewBank() {
        PatchBank b = new PatchBank();
        b.setVisible(true);
    }

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
    }

    private void ShowConnectDisconnect(boolean connect) {

        if (connect) {
            jToggleButtonConnect.setText("Connected");
            ShowConnectionFlags(USBBulkConnection.GetConnection().GetConnectionFlags());
        }
        else {
            jToggleButtonConnect.setText("Connect");
            jLabelCPUID.setText(" ");
            jLabelCPUID.setToolTipText(" ");
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

        jMenuItemEnterDFU.setEnabled(connect);
        jMenuItemMount.setEnabled(connect);
        jMenuItemFlashDefault.setEnabled(connect && USBBulkConnection.GetConnection().getTargetProfile().hasSDRAM());
        jMenuItemFlashUser.setEnabled(connect && USBBulkConnection.GetConnection().getTargetProfile().hasSDRAM());

        if (Preferences.getInstance().getRestartRequired()) {
            disableConnectUntilRestart();
        }
    }

    public void Quit() {
        while (!DocumentWindowList.GetList().isEmpty()) {
            if (DocumentWindowList.GetList().get(0).AskClose()) {
                return;
            }
        }

        // Usb.shutdown(); /* java: io.c:2116: handle_events: Assertion `ctx->pollfds_cnt >= internal_nfds' failed. */

        Preferences.getInstance().SavePrefs();
        if (DocumentWindowList.GetList().isEmpty()) {
            System.exit(0);
        }
    }

    public void updateLinkFirmwareID() {
        String oldID = LinkFirmwareID;
        LinkFirmwareID = FirmwareID.getFirmwareID();
        if (!LinkFirmwareID.equals(oldID)) { /* Report Firmware ID change */
            LOGGER.log(Level.INFO, "Patcher linked to firmware {0}", LinkFirmwareID);
        }
        WarnedAboutFWCRCMismatch = false;
    }

    void setFirmwareID(String firmwareId) {
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
            WarnedAboutFWCRCMismatch = true;
            QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdDisconnect());
            ShowDisconnect();
        }
        else if (!firmwareId.equals(this.LinkFirmwareID)) {
            if (!WarnedAboutFWCRCMismatch) {
                LOGGER.log(Level.WARNING, "Firmware version mismatch! Please update the firmware.");
                LOGGER.log(Level.WARNING, "Core running {0} <-> Patcher linked to {1}", new Object[]{firmwareId, this.LinkFirmwareID});
                WarnedAboutFWCRCMismatch = true;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        interactiveFirmwareUpdate();
                    }
                });
            }
        }
    }

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
        }
        else {
            jLabelVoltages.setEnabled(true);
        }
    }

    public void interactiveFirmwareUpdate() {
        byte[] fwversion = USBBulkConnection.GetConnection().getFwVersion();
        if (fwversion[0] == 1 && fwversion[1] < 1) {
            /* Core is currently running 1.0.x.x old firmware. v1.1 Auto */
            LOGGER.log(Level.SEVERE, "The Core trying to connect is running v1.0.x firmware.\nTo use it with this Patcher, you must update the firmware via Rescue Mode.\nPress and hold Button S1 during power-up and select 'Board > Firmware > Flash (Rescue) to update the firmware.");
            QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdDisconnect());
            ShowDisconnect();
            return;
        }

        Object[] options = {"Update", "Cancel"};
        int s = KeyboardNavigableOptionPane.showOptionDialog(this,
                "Firmware mismatch detected!\n"
                + "Update the firmware now?\n"
                + "This process will cause a disconnect and the LEDs will blink for a while.\n"
                + "Do not interrupt until the LEDs stop blinking.\n"
                + "When only the green LED is steady lit, you can connect again.\n",
                "Firmware Update",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);

        if (s == 0) {
            String fname = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "flasher" + File.separator + "flasher_build";
            String pname = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "build";
            if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
                fname += File.separator + "ksoloti_flasher.bin";
                pname += File.separator + "ksoloti";
            }
            else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
                fname += File.separator + "axoloti_flasher.bin";
                pname += File.separator + "axoloti";
            }
            if (Preferences.getInstance().getFirmwareMode().contains("SPILink")) {
                pname += "_spilink";
            }
            if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
                pname += "_usbaudio";
            }
            if (Preferences.getInstance().getFirmwareMode().contains("I2SCodec")) {
                pname += "_i2scodec";
            }
            pname += ".bin";
            flashUsingSDRam(fname, pname);
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

        if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
            jPanelInfoColumn.add(jLabelFlags);
        }

        jPanelInfoColumn.add(jLabelPatch);
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
                    System.out.println(Instant.now() + " Opening .axo files not implemented yet");
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
        if (USBBulkConnection.GetConnection().isConnected()) {
            jLabelSDCardPresent.setText("No SD Card");
        }
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

    @Override
    public void ShowBoardIDName(String unitId, String friendlyName) {
        if (!USBBulkConnection.GetConnection().isConnected() || unitId == null || unitId.trim().isEmpty()) {
            jLabelCPUID.setText(" ");
            jLabelCPUID.setToolTipText(" ");
        }
        else {
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

                jLabelCPUID.setText(formattedCpuId.toString());
                jLabelCPUID.setToolTipText("Showing board ID of the currently connected Core.\n" +
                                        "You can name your Core by disconnecting it from\n" +
                                        "the Patcher, then going to Board > Select Device... > Name.\n" +
                                        "Press Enter in the Name textfield to confirm the entry.");
            }
            else {
                jLabelCPUID.setText("Board Name:   " + nameToDisplay);
                jLabelCPUID.setToolTipText("Showing the name defined in Board > Select Device... > Name.\n" +
                                        "This setting is saved in the local ksoloti.prefs file.");
            }
        }
    }
}
