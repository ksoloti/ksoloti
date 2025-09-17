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

import axoloti.dialogs.AxoSplashScreen;
import axoloti.object.AxoObjects;
import axoloti.ui.Theme;
import axoloti.utils.FileUtils;
import axoloti.utils.OSDetect;
import axoloti.utils.Preferences;
import axoloti.utils.OSDetect.ARCH;
import axoloti.utils.OSDetect.OS;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


/**
 *
 * @author Johannes Taelman
 */
public class Axoloti {
    private final static Logger LOGGER = Logger.getLogger(Axoloti.class.getName());

    public final static int SINGLE_INSTANCE_PORT = 55576; /* For checking if another Patcher instance is running */
    private static Thread singleInstanceListenerThread;

    public final static String HOME_DIR       = "axoloti_home";
    public final static String LIBRARIES_DIR  = "axoloti_libraries";
    public final static String FIRMWARE_DIR   = "axoloti_firmware";
    public final static String PLATFORM_DIR   = "axoloti_platform";

    private static String cacheFWDir = null;
    private static boolean cacheDeveloper = false;

    private static class OpenFilesHandlerImpl implements OpenFilesHandler {
        @Override
        public void openFiles(OpenFilesEvent e) {
            MainFrame.openFilesFromListener(new ArrayList<>(e.getFiles()));
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {

        if (OSDetect.getOS() == OS.MAC && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().setOpenFileHandler(new OpenFilesHandlerImpl());
        }

        /* Collect all file paths from the command-line arguments */
        ArrayList<String> filePathsList = new ArrayList<>();
        for (String arg : args) {
            if (arg != null && !arg.startsWith("-")) {
                filePathsList.add(arg);
            }
        }
        final String[] filePaths = filePathsList.toArray(new String[0]);

        AxoSplashScreen splashScreen = null;
        
        try {
            initProperties();
            Preferences.LoadPreferences();
            Preferences.getInstance().applyTheme();
            Synonyms.instance(); // prime it
            
            if (OSDetect.getOS() == OSDetect.OS.MAC) {
                // System.setProperty("apple.laf.useScreenMenuBar", "true"); /* This option breaks menu functions */
                System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS"); /* deprecated? non-functional? */
                System.setProperty("apple.awt.application.name", "Ksoloti");
                System.setProperty("apple.awt.application.appearance", "system");
                System.setProperty("apple.awt.transparentTitleBar", "true");

                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                        desktop.setQuitHandler(new QuitHandler() {
                            @Override
                            public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
                                boolean canQuit = true;
                                for (Window window : Window.getWindows()) {
                                    if (window instanceof DocumentWindow) {
                                        DocumentWindow docWindow = (DocumentWindow) window;
                                        if (docWindow.AskClose()) {
                                            canQuit = false;
                                            break; 
                                        }
                                    }
                                }
                                if (canQuit) {
                                    response.performQuit();
                                } else {
                                    response.cancelQuit();
                                }
                            }
                        });
                    }
                }
            }
            
            System.setProperty("awt.useSystemAAFontSettings", "gasp");
            System.setProperty("swing.aatext", "true");
            System.setProperty("line.separator", "\n");

            /* Set tooltip delay and duration */
            javax.swing.ToolTipManager.sharedInstance().setDismissDelay(600000);
            javax.swing.ToolTipManager.sharedInstance().setInitialDelay(1250);
            javax.swing.ToolTipManager.sharedInstance().setReshowDelay(10);
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", true);
            UIManager.put("flatlaf.menuBarEmbedded", true);
            UIManager.put("ScrollBar.thumb", UIManager.getColor("ScrollBar.hoverThumbColor"));
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Component.focusWidth", 0);
            UIManager.put("ToggleButton.selectedForeground", Theme.Button_Accent_Foreground);
            UIManager.put("ToggleButton.selectedBackground", Theme.Button_Accent_Background);
            UIManager.put("Objects.Grey", UIManager.getColor("Panel.foreground"));

            ServerSocket serverSocket = new ServerSocket(SINGLE_INSTANCE_PORT);
            System.out.println(Instant.now() + " No existing Patcher instance found. Starting new instance.");
            startSingleInstanceListener(serverSocket);
            splashScreen = new AxoSplashScreen(false);
            splashScreen.showSplashScreen();
            handleCommandLine(filePaths, splashScreen);

        } catch (IOException | URISyntaxException e) {
            /* An existing instance is running */
            if (filePaths.length > 0) {
                System.out.println(Instant.now() + " Existing Patcher instance found. Handing over file(s) and exiting.");
                try (Socket socket = new Socket("localhost", SINGLE_INSTANCE_PORT);
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                    for (String path : filePaths) {
                        writer.println(path);
                    }
                    System.out.println(Instant.now() + " File(s) handed over to existing instance: " + Arrays.toString(filePaths));
                } catch (IOException ex) {
                    System.out.println(Instant.now() + " Failed to connect to existing instance: " + ex.getMessage());
                    ex.printStackTrace(System.out);
                }
                System.exit(0);
            } else {
                System.out.println(Instant.now() + " Another instance is running, but no file args were provided. Starting a new instance.");
                handleCommandLine(filePaths, splashScreen);
            }
        }
    }

    private static void startSingleInstanceListener(ServerSocket serverSocket) {
        if (singleInstanceListenerThread == null || !singleInstanceListenerThread.isAlive()) {
            singleInstanceListenerThread = new Thread(() -> {
                try (ServerSocket server = serverSocket) {
                    System.out.println(Instant.now() + " Main instance listening for new files...");
                    while (true) {
                        try (Socket clientSocket = server.accept();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                            
                            ArrayList<File> receivedFiles = new ArrayList<>();
                            String filePath;
                            while ((filePath = reader.readLine()) != null) {
                                File f = new File(filePath);
                                if (f.exists()) {
                                    System.out.println(Instant.now() + " Main instance received new file: " + filePath);
                                    receivedFiles.add(f);
                                }
                            }
                            if (!receivedFiles.isEmpty()) {
                                SwingUtilities.invokeLater(() -> MainFrame.openFilesFromListener(receivedFiles));
                            }

                        }
                    }
                } catch (IOException e) {
                    System.out.println(Instant.now() + " Single-instance listener thread failed: " + e.getMessage());
                    e.printStackTrace(System.out);
                }
            });
            singleInstanceListenerThread.setDaemon(true);
            singleInstanceListenerThread.setName("singleInstanceListenerThread");
            singleInstanceListenerThread.start();
        }
    }

    static void BuildEnv(String var, String def) {
        String ev = System.getProperty(var);
        if (ev == null) {
            ev = System.getenv(var);
            if (ev == null) {
                ev = def;
            }
        }
        File f = new File(ev);
        if (f.exists()) {
            try {
                ev = f.getCanonicalPath();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error attempting to get canonical path: " + ex.getMessage());
                ex.printStackTrace(System.out);
            }
        }
        System.setProperty(var, ev);
    }

    static boolean TestDir(String var) {
        String ev = System.getProperty(var);
        File f = new File(ev);
        if (!f.exists()) {
            System.err.println(var + " directory does not exist " + ev);
            return false;
        }
        if (!f.isDirectory()) {
            System.err.println(var + " must be a valid directory " + ev);
            return false;
        }
        return true;
    }

    public static boolean isDeveloper() {

        String fwEnv = System.getProperty(FIRMWARE_DIR);
        if (cacheFWDir != null && fwEnv.equals(cacheFWDir)) {
            return cacheDeveloper;
        }

        cacheFWDir = fwEnv;
        cacheDeveloper = false;

        String dirRelease = System.getProperty(HOME_DIR);

        String fwRelease = dirRelease + File.separator + "firmware";

        if (!fwRelease.equals(cacheFWDir)) {
            File fR = new File(fwRelease);
            File fE = new File(fwEnv);

            try {
                cacheDeveloper = !fR.getCanonicalPath().equals(fE.getCanonicalPath());
            }
            catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error attempting to compare canonical paths: " + ex.getMessage());
                ex.printStackTrace(System.out);
                cacheDeveloper = false;
            }
        }
        else {
            File f = new File(dirRelease + File.separator + ".git");
            if (f.exists()) {
                cacheDeveloper = true;
            }
        }
        return cacheDeveloper;
    }

    private static void initProperties() throws URISyntaxException, IOException {
        File jarFile = new File(Axoloti.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String defaultHome = ".";
        String defaultLibraries = "defaultLibraries";

        BuildEnv(HOME_DIR, defaultHome);
        File homedir = new File(System.getProperty(HOME_DIR));
        if (!homedir.exists()) {
            homedir.mkdir();
        }
        if (!TestDir(HOME_DIR)) {
            System.err.println("Home directory is invalid");
        }

        String osHomeDir;
        OS os = OSDetect.getOS();
        if (os != null) {
            switch (os) {
                case WIN:
                    /* Expected to stay valid for future Windows versions, good for Win 7, 10, 11! */
                    osHomeDir = System.getenv("HOMEDRIVE") + System.getenv("HOMEPATH");
                    break;
                case MAC:
                    osHomeDir = System.getenv("HOME");
                    break;
                case LINUX:
                default:
                    osHomeDir = System.getenv("HOME");
                    break;
            }
        }
        else {
            osHomeDir = System.getenv("HOME");
        }

        defaultLibraries = osHomeDir + File.separator + "ksoloti" + File.separator + Version.AXOLOTI_SHORT_VERSION;

        BuildEnv(LIBRARIES_DIR, defaultLibraries);
        File libdir = new File(System.getProperty(LIBRARIES_DIR));
        if (!libdir.exists()) {
            libdir.mkdir();
        }
        if (!TestDir(LIBRARIES_DIR)) {
            System.err.println("Libraries directory is invalid");
        }

        File builddir = new File(System.getProperty(LIBRARIES_DIR) + File.separator + "build");
        if (!builddir.exists()) {
            builddir.mkdir();
        }

        deletePrecompiledHeaderFile();

        BuildEnv(FIRMWARE_DIR, System.getProperty(HOME_DIR) + File.separator + "firmware");
        if (!TestDir(FIRMWARE_DIR)) {
            System.err.println("Firmware directory is invalid");
        }

        if (os != null) {
            switch (os) {
                case WIN:
                    BuildEnv(PLATFORM_DIR, System.getProperty(HOME_DIR) + File.separator + "platform_win_x64");
                    break;
                case MAC:
                    if (OSDetect.getArch() == ARCH.AARCH64) {
                        BuildEnv(PLATFORM_DIR, System.getProperty(HOME_DIR) + File.separator + "platform_mac_aarch64");
                    }
                    else {
                        BuildEnv(PLATFORM_DIR, System.getProperty(HOME_DIR) + File.separator + "platform_mac_x64");
                    }
                    break;
                case LINUX:
                    if (OSDetect.getArch() == ARCH.AARCH64) {
                        BuildEnv(PLATFORM_DIR, System.getProperty(HOME_DIR) + File.separator + "platform_linux_aarch64");
                    }
                    else {
                        BuildEnv(PLATFORM_DIR, System.getProperty(HOME_DIR) + File.separator + "platform_linux_x64");
                    }
                    break;
                default:
                break;
            }
        }
        if (!TestDir(PLATFORM_DIR)) {
            System.err.println("platform_* directory is invalid");
        }

        System.out.println("Directories:\n"
                + "Current = " + System.getProperty("user.dir") + "\n"
                + "Jar = " + jarFile.getParentFile().getCanonicalPath() + "\n"
                + "Home = " + System.getProperty(HOME_DIR) + "\n"
                + "Firmware = " + System.getProperty(FIRMWARE_DIR) + "\n"
                + "Libraries = " + System.getProperty(LIBRARIES_DIR) + "\n"
                + "Platform = " + System.getProperty(PLATFORM_DIR) + "\n"
        );
    }

    public static void deletePrecompiledHeaderFile() {
        /* Flush previous xpatch.h.gch build file on start */
        File builddir = new File(System.getProperty(LIBRARIES_DIR) + File.separator + "build");
        File[] bfiles = builddir.listFiles();
        if (bfiles != null) {
            for (File f : bfiles) {
                /* Flush previous temp build files... */
                if (f.isDirectory()) {
                    FileUtils.deleteDirectory(f);
                }
                else {
                    /* ...and flush precompiled header file. */
                    if (f.getName().equals("xpatch.h.gch")) f.delete();
                }
            }
        }
    }

    private static void handleCommandLine(final String args[], final AxoSplashScreen splashScreen) {
        boolean cmdLineOnly = false;
        boolean cmdRunAllTest = false;
        boolean cmdRunPatchTest = false;
        boolean cmdRunObjectTest = false;
        boolean cmdRunFileTest = false;
        boolean cmdRunUpgrade = false;
        String cmdFile = null;
        ArrayList<String> guiArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equalsIgnoreCase("-exitOnFirstFail")) {
                MainFrame.stopOnFirstFail = true;
                guiArgs.add(arg);
            }

            // exclusive options
            if (arg.equalsIgnoreCase("-runAllTests")) {
                cmdLineOnly = true;
                cmdRunAllTest = true;
            } else if (arg.equalsIgnoreCase("-runPatchTests")) {
                cmdLineOnly = true;
                cmdRunPatchTest = true;
            } else if (arg.equalsIgnoreCase("-runObjTests")) {
                cmdLineOnly = true;
                cmdRunObjectTest = true;
            } else if (arg.equalsIgnoreCase("-runTest")) {
                cmdLineOnly = true;
                cmdRunFileTest = true;
                if (i + 1 < args.length) {
                    cmdFile = args[i + 1];
                } else {
                    System.err.println("-runTest patchname/directory: missing file/dir");
                    System.exit(-1);
                }
            } else if (arg.equalsIgnoreCase("-runUpgrade")) {
                cmdLineOnly = true;
                cmdRunUpgrade = true;
                if (i + 1 < args.length) {
                    cmdFile = args[i + 1];
                } else {
                    System.err.println("-runUpgrade patchname/directory: missing file/dir");
                    System.exit(-1);
                }
            } else if (arg.equalsIgnoreCase("-help")) {
                System.out.println("Ksoloti "
                        + " [-runAllTests|-runPatchTests|-runObjTests] "
                        + " [-runTest patchfile|dir]"
                        + " [-runUpgrade patchfile|dir]"
                        + " [-exitOnFirstFail");
                System.exit(0);
            }
            else {
                guiArgs.add(arg);
            }
        }

        String[] finalGuiArgs = guiArgs.toArray(new String[0]);

        if (cmdLineOnly) {
            try {
                MainFrame frame = new MainFrame(args);
                frame.setVisible(false);
                AxoObjects objs = new AxoObjects();
                objs.LoadAxoObjects();
                System.out.println("Waiting for libraries to load...");
                Thread.sleep(10000);
                if (splashScreen != null) {
                    splashScreen.dispose();
                }

                System.out.println("Ksoloti command line initialised.");
                int exitCode = 0;
                if (cmdRunAllTest) {
                    exitCode = frame.runAllTests() ? 0 : -1;
                } else if (cmdRunPatchTest) {
                    exitCode = frame.runPatchTests() ? 0 : -1;
                } else if (cmdRunObjectTest) {
                    exitCode = frame.runObjectTests() ? 0 : -1;
                } else if (cmdRunFileTest) {
                    exitCode = frame.runFileTest(cmdFile) ? 0 : -1;
                } else if (cmdRunUpgrade) {
                    exitCode = frame.runFileUpgrade(cmdFile) ? 0 : -1;
                }
                System.out.println("Axoloti cmd line complete");
                System.exit(exitCode);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.exit(-2);
            }
        } else {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        MainFrame frame = new MainFrame(finalGuiArgs); 
                        frame.setVisible(true);
                        if (splashScreen != null) {
                            splashScreen.dispose();
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                }
            });
        }
    }
}
