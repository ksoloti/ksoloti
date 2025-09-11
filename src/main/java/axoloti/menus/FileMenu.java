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
package axoloti.menus;

import axoloti.Axoloti;

import static axoloti.MainFrame.axoObjects;
import static axoloti.MainFrame.mainframe;

import axoloti.PatchFrame;
import axoloti.PatchGUI;
import axoloti.USBBulkConnection;
import axoloti.dialogs.PatchBank;
import axoloti.dialogs.PreferencesFrame;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.FileUtils;
import axoloti.utils.KeyUtils;
import axoloti.utils.Preferences;
import generatedobjects.GeneratedObjects;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 *
 * @author jtaelman
 */
public class FileMenu extends JMenu {

    private static final Logger LOGGER = Logger.getLogger(FileMenu.class.getName());

    String currentTestPath; /* Retains the previously entered batch test path */

    public FileMenu(String s) {
        super(s);
        setDelay(300);
    }

    public FileMenu() {
        super();
        setDelay(300);
    }

    public void initComponents() {

        int pos = 0;
        currentTestPath = System.getProperty(Axoloti.LIBRARIES_DIR); /* Init to "all stock libraries" path */

        jMenuNewBank = new JMenuItem();
        jMenuNewPatch = new JMenuItem();
        jMenuOpen = new JMenuItem();
        jMenuOpenURL = new JMenuItem();
        jMenuQuit = new JMenuItem();
        jMenuRegenerateObjects = new JMenuItem();
        jMenuReloadObjects = new JMenuItem();
        jMenuSync = new JMenuItem();
        jSeparator1 = new JPopupMenu.Separator();
        jSeparator2 = new JPopupMenu.Separator();
        jSeparator3 = new JPopupMenu.Separator();
        libraryMenu1 = new LibraryMenu();
        recentFileMenu1 = new RecentFileMenu();
        favouriteMenu1 = new FavouriteMenu();
        jMenuItemPreferences = new JMenuItem();
        jMenuAutoTestDir = new JMenuItem();

        jMenuNewPatch.setMnemonic('N');
        jMenuNewPatch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuNewPatch.setText("New Patch");
        jMenuNewPatch.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuNewPatchActionPerformed(evt);
            }
        });
        insert(jMenuNewPatch, pos++);

        jMenuNewBank.setMnemonic('B');
        jMenuNewBank.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                KeyUtils.CONTROL_OR_CMD_MASK | KeyEvent.SHIFT_DOWN_MASK));
        jMenuNewBank.setText("New Patchbank");
        jMenuNewBank.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuNewBankActionPerformed(evt);
            }
        });
        insert(jMenuNewBank, pos++);

        jMenuOpen.setMnemonic('O');
        jMenuOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuOpen.setText("Open...");
        jMenuOpen.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuOpenActionPerformed(evt);
            }
        });
        insert(jMenuOpen, pos++);

        jMenuOpenURL.setMnemonic('U');
        jMenuOpenURL.setText("Open from URL...");
        jMenuOpenURL.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuOpenURLActionPerformed(evt);
            }
        });
        insert(jMenuOpenURL, pos++);

        recentFileMenu1.setMnemonic('R');
        recentFileMenu1.setText("Open Recent");
        insert(recentFileMenu1, pos++);

        libraryMenu1.setMnemonic('L');
        libraryMenu1.setText("Library");
        insert(libraryMenu1, pos++);

        favouriteMenu1.setMnemonic('V');
        favouriteMenu1.setText("Favourites");
        insert(favouriteMenu1, pos++);

        add(jSeparator1);
        pos++;

        jMenuSync.setMnemonic('Y');
        jMenuSync.setText("Sync Libraries");
        jMenuSync.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSyncActionPerformed(evt);
            }
        });
        add(jMenuSync);

        jMenuReloadObjects.setMnemonic('J');
        jMenuReloadObjects.setText("Reload Objects");
        jMenuReloadObjects.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuReloadObjectsActionPerformed(evt);
            }
        });
        add(jMenuReloadObjects);

        jMenuRegenerateObjects.setText("Regenerate Objects");
        jMenuRegenerateObjects.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuRegenerateObjectsActionPerformed(evt);
            }
        });
        add(jMenuRegenerateObjects);

        jMenuAutoTestDir.setText("Test Compilation: All / Enter Directory");
        jMenuAutoTestDir.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAutoTestDirActionPerformed(evt);
            }
        });
        add(jMenuAutoTestDir);
        add(jSeparator2);

        jMenuItemPreferences.setMnemonic('P');
        jMenuItemPreferences.setText("Preferences...");
        jMenuItemPreferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
                KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuItemPreferences.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemPreferencesActionPerformed(evt);
            }
        });
        add(jMenuItemPreferences);
        add(jSeparator3);

        jMenuQuit.setMnemonic('Q');
        jMenuQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                KeyUtils.CONTROL_OR_CMD_MASK));
        jMenuQuit.setText("Quit");
        jMenuQuit.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuQuitActionPerformed(evt);
            }
        });
        add(jMenuQuit);

        jMenuRegenerateObjects.setVisible(false);
        if (!Preferences.getInstance().getExpertMode()) {
            jMenuAutoTestDir.setVisible(false);
        }
    }

    private javax.swing.JMenuItem jMenuNewBank;
    private javax.swing.JMenuItem jMenuNewPatch;
    private javax.swing.JMenuItem jMenuOpen;
    private javax.swing.JMenuItem jMenuOpenURL;
    private javax.swing.JMenuItem jMenuQuit;
    @Deprecated
    private javax.swing.JMenuItem jMenuRegenerateObjects;
    private javax.swing.JMenuItem jMenuReloadObjects;
    private javax.swing.JMenuItem jMenuSync;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private axoloti.menus.LibraryMenu libraryMenu1;
    private axoloti.menus.RecentFileMenu recentFileMenu1;
    private axoloti.menus.FavouriteMenu favouriteMenu1;
    private javax.swing.JMenuItem jMenuItemPreferences;
    private javax.swing.JMenuItem jMenuAutoTestDir;

    private void jMenuItemPreferencesActionPerformed(java.awt.event.ActionEvent evt) {
        PreferencesFrame.getInstance().setState(java.awt.Frame.NORMAL);
        PreferencesFrame.getInstance().setVisible(true);
    }

    private void jMenuAutoTestDirActionPerformed(java.awt.event.ActionEvent evt) {
        String inputPath = JOptionPane.showInputDialog(
            mainframe,
            "Enter directory to test.\n" + 
            "(Default: Test all stock libraries)\n" + 
            "Wildcards:\n" + 
            "  '*' matches zero or more characters\n" + 
            "  '?' matches exactly one character\n\n" + 
            "WARNING: Running these tests may take a long time and/or freeze the UI.",
            currentTestPath
        );

        if (inputPath != null && !inputPath.isEmpty()) {
            currentTestPath = inputPath;

            final List<File> directoriesToTest;
            boolean hasWildcard = inputPath.contains("*") || inputPath.contains("?");

            if (hasWildcard) {
                try {
                    int firstWildcardIndex = -1;
                    for (int i = 0; i < inputPath.length(); i++) {
                        char c = inputPath.charAt(i);
                        if (c == '*' || c == '?' || c == '[' || c == ']') {
                            firstWildcardIndex = i;
                            break;
                        }
                    }
            
                    String rootPathString;
                    String globPattern;
            
                    if (firstWildcardIndex > 0) {
                        int lastSeparatorIndex = inputPath.lastIndexOf(File.separatorChar, firstWildcardIndex - 1);
            
                        if (lastSeparatorIndex != -1) {
                            rootPathString = inputPath.substring(0, lastSeparatorIndex);
                        } else {
                            rootPathString = ".";
                        }
                    } else {
                        rootPathString = inputPath;
                    }
            
                    globPattern = "glob:" + inputPath.replace('\\', '/');
                    
                    Path rootPath = Paths.get(rootPathString).toAbsolutePath();
                    PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);
            
                    try (Stream<Path> paths = Files.walk(rootPath)) {
                        directoriesToTest = paths
                            .filter(Files::isDirectory)
                            .filter(matcher::matches)
                            .map(Path::toFile)
                            .collect(Collectors.toList());
                    }
            
                    if (directoriesToTest.isEmpty()) {
                        LOGGER.log(Level.WARNING, "No directories found matching pattern: " + inputPath);
                        return;
                    }
                }
                catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error during wildcard search: " + ex.getMessage());
                    ex.printStackTrace(System.out);
                    return;
                }
            } else {
                File f = new File(currentTestPath);
                if (!f.exists()) {
                    LOGGER.log(Level.WARNING, "Path not found: " + currentTestPath);
                    return;
                }
                if (!f.canRead()) {
                    LOGGER.log(Level.WARNING, "Failed to read path: " + currentTestPath);
                    return;
                }

                directoriesToTest = List.of(f);
            }

            class Thd extends Thread {
                public void run() {
                    try {
                        String logpath = System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + "build" + File.separator + "batch_test.log";
                        /* If previous log exists, delete it */
                        File logf = new File(logpath);
                        if (logf.exists()) {
                            logf.delete();
                        }
                        /* If previous lock exists, delete it (happens if test was interrupted) */
                        File lockf = new File(logpath + ".lck");
                        if (lockf.exists()) {
                            lockf.delete();
                        }

                        FileHandler fh = new FileHandler(logpath, true);
                        SimpleFormatter formatter = new SimpleFormatter();
                        fh.setFormatter(formatter);
                        Logger.getLogger("").addHandler(fh);

                        LOGGER.log(Level.WARNING, "Running tests, please wait...");
                        LOGGER.log(Level.INFO, "Creating log file at " + logpath);

                        /* From now on, Leave out timecode from logging format (easier diff) */
                        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
                        formatter = new SimpleFormatter();
                        fh.setFormatter(formatter);

                        if (USBBulkConnection.getInstance().isConnected()) {
                            LOGGER.log(Level.INFO, "Core is connected - Attempting test upload of patches and measuring DSP load.");
                        }

                        LOGGER.log(Level.INFO, "Testing directories:");
                        for (File dir : directoriesToTest) {
                            LOGGER.log(Level.INFO, dir.getAbsolutePath());
                        }
                        LOGGER.log(Level.INFO, "");
                        for (File dir : directoriesToTest) {
                            mainframe.runTestDir(dir);
                        }

                        LOGGER.log(Level.WARNING, "Done running tests.\n");

                        Logger.getLogger("").removeHandler(fh);
                        fh.close();

                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Error during batch test process: " + ex.getMessage());
                        ex.printStackTrace(System.out);
                    } finally {
                        /* Revert logging format */
                        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tFT%1$tT.%1$tN  %4$s: %5$s%n");
                    }
                }
            }

            Thd thread = new Thd();
            thread.setName("jMenuAutoTestDirActionPerformedThread");
            thread.start();
        }
    }

    private void jMenuRegenerateObjectsActionPerformed(java.awt.event.ActionEvent evt) {
        GeneratedObjects.WriteAxoObjects();
        jMenuReloadObjectsActionPerformed(evt);
    }

    private void jMenuReloadObjectsActionPerformed(java.awt.event.ActionEvent evt) {
        axoObjects.LoadAxoObjects();
    }

    private void jMenuOpenURLActionPerformed(java.awt.event.ActionEvent evt) {
        OpenURL();
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
        } catch (MalformedURLException | URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, "Invalid URL: " + uri + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to open URL: " + uri + ", " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }

    private void jMenuOpenActionPerformed(java.awt.event.ActionEvent evt) {
        FileUtils.Open((JFrame) SwingUtilities.getWindowAncestor(this));
    }

    private void jMenuNewPatchActionPerformed(java.awt.event.ActionEvent evt) {
        NewPatch();
    }

    private void jMenuSyncActionPerformed(java.awt.event.ActionEvent evt) {
        class Thd extends Thread {
            public void run() {
                LOGGER.log(Level.INFO, "Syncing Libraries...");
                for (AxolotiLibrary lib : Preferences.getInstance().getLibraries()) {
                    lib.sync();
                }
                LOGGER.log(Level.INFO, "Done syncing Libraries.\n");
                axoObjects.LoadAxoObjects();
            }
        }
        Thd thread = new Thd();
        thread.setName("jMenuSyncActionPerformedThread");
        thread.start();
    }

    private void jMenuNewBankActionPerformed(java.awt.event.ActionEvent evt) {
        NewBank();
    }

    public void NewPatch() {
        PatchGUI patch1 = new PatchGUI();
        PatchFrame pf = new PatchFrame(patch1);
        patch1.PostContructor();
        pf.setLocation((int)pf.getLocation().getX() + 240 , (int)pf.getLocation().getY() + 160);
        patch1.setFileNamePath("untitled");
        pf.setVisible(true);
    }

    public void NewBank() {
        PatchBank b = new PatchBank();
        b.setVisible(true);
    }

    private void jMenuQuitActionPerformed(java.awt.event.ActionEvent evt) {
        mainframe.Quit();
    }

}
