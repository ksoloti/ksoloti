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

// import axoloti.CheckForUpdates;
// import axoloti.MainFrame;
import axoloti.dialogs.AboutFrame;
import axoloti.dialogs.ShortcutsFrame;
import java.awt.Desktop;
// import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 *
 * @author jtaelman
 */
public class HelpMenu extends JMenu {

    private static final Logger LOGGER = Logger.getLogger(HelpMenu.class.getName());

    private javax.swing.JMenuItem jMenuHelpContents;
    private javax.swing.JMenuItem jMenuAbout;
    private javax.swing.JMenuItem jMenuShortcuts;
    // private javax.swing.JMenuItem jMenuUpdates;
    private javax.swing.JMenuItem jMenuCommunity;
    private javax.swing.JMenuItem jMenuAxolotiCommunityBackup;
    private axoloti.menus.HelpLibraryMenu helpLibraryMenu1;

    public HelpMenu() {
        setDelay(300);
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                Populate();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                removeAll();
            }

            @Override
            public void menuCanceled(MenuEvent e) {
                removeAll();
            }
        });        
    }
    
    private void Populate() {
        jMenuHelpContents = new javax.swing.JMenuItem();
        jMenuAbout = new javax.swing.JMenuItem();
        jMenuShortcuts = new javax.swing.JMenuItem();
        // jMenuUpdates = new javax.swing.JMenuItem();
        jMenuCommunity = new javax.swing.JMenuItem();
        jMenuAxolotiCommunityBackup = new javax.swing.JMenuItem();
        helpLibraryMenu1 = new axoloti.menus.HelpLibraryMenu();

        jMenuHelpContents.setMnemonic('H');
        jMenuHelpContents.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        jMenuHelpContents.setText("Help Contents");
        jMenuHelpContents.setToolTipText("Open Axoloti online user guide in your browser. (Has not been updated in a while)");
        jMenuHelpContents.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuHelpContentsActionPerformed(evt);
            }
        });
        add(jMenuHelpContents);

        jMenuAbout.setMnemonic('A');
        jMenuAbout.setText("About");
        jMenuAbout.setToolTipText("Popup window with some background info and version tags");
        jMenuAbout.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAboutActionPerformed(evt);
            }
        });
        add(jMenuAbout);

        jMenuShortcuts.setMnemonic('S');
        jMenuShortcuts.setText("Keyboard Shortcuts");
        jMenuShortcuts.setToolTipText("List of keyboard and mouse shortcuts");
        jMenuShortcuts.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuShortcutsActionPerformed(evt);
            }
        });
        add(jMenuShortcuts);

        jMenuCommunity.setMnemonic('C');
        jMenuCommunity.setText("Community Website");
        jMenuCommunity.setToolTipText("Open Ksoloti Forum in your browser.");
        jMenuCommunity.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCommunityActionPerformed(evt);
            }
        });
        add(jMenuCommunity);

        jMenuAxolotiCommunityBackup.setMnemonic('B');
        jMenuAxolotiCommunityBackup.setText("Axoloti Community Forum Backup");
        jMenuAxolotiCommunityBackup.setToolTipText("Open the read-only backup of the original Axoloti Community Forum in your browser.");
        jMenuAxolotiCommunityBackup.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAxolotiCommunityBackupActionPerformed(evt);
            }
        });
        add(jMenuAxolotiCommunityBackup);

        // jMenuUpdates.setMnemonic('U');
        // jMenuUpdates.setText("Check for Updates...");
        // jMenuUpdates.addActionListener(new java.awt.event.ActionListener() {
        //     @Override
        //     public void actionPerformed(java.awt.event.ActionEvent evt) {
        //         jMenuUpdatesActionPerformed(evt);
        //     }
        // });
        // add(jMenuUpdates);

        helpLibraryMenu1.setText("Help Patches");
        helpLibraryMenu1.setToolTipText("Open a help patch from a List of found help patches in all loaded libraries.");
        helpLibraryMenu1.setMnemonic('P');
        helpLibraryMenu1.setDisplayedMnemonicIndex(5);
        add(helpLibraryMenu1);

    }

    private void jMenuAboutActionPerformed(java.awt.event.ActionEvent evt) {
        AboutFrame.aboutFrame.setVisible(true);
    }

    private void jMenuShortcutsActionPerformed(java.awt.event.ActionEvent evt) {
        ShortcutsFrame.shortcutsFrame.setVisible(true);
    }

    // private void jMenuUpdatesActionPerformed(java.awt.event.ActionEvent evt) {
    //     CheckForUpdates.checkForUpdates();
    // }

    private void jMenuHelpContentsActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Desktop.getDesktop().browse(new URI("https://sebiik.github.io/community.axoloti.com.backup/t/axoloti-user-guide/50.html"));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void jMenuCommunityActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Desktop.getDesktop().browse(new URI("https://ksoloti.discourse.group/"));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void jMenuAxolotiCommunityBackupActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Desktop.getDesktop().browse(new URI("https://sebiik.github.io/community.axoloti.com.backup/"));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

}
