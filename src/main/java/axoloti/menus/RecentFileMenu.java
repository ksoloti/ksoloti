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
package axoloti.menus;

import axoloti.MainFrame;
import axoloti.utils.Preferences;

import java.io.File;
import java.util.ArrayList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 *
 * @author Johannes Taelman
 */
public class RecentFileMenu extends JMenu {

    public RecentFileMenu() {
        setDelay(300);

        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {

                ArrayList<String> r = Preferences.getInstance().getRecentFiles();

                /* "Garbage bin": collects any strings that point to non-existent files (cannot remove those inside the loop - ConcurrentModificationException) */
                // ArrayList<String> filesNotFound = new ArrayList<String>();

                /* Loop through list and add all valid file references */
                for (String s : r) {
                    File f = new File(s);
                    // if (f.exists() && f.canRead()) {
                    /* Add as menu entry */
                    JMenuItem mi = new JMenuItem(s);
                    if (!f.exists() || !f.canRead()) {
                        /* If file can't be found, still show it but greyed out */
                        mi.setEnabled(false);
                        mi.setToolTipText("File not found! It was likely moved, renamed, or deleted.");
                    }
                    mi.setActionCommand("open:" + s);
                    mi.addActionListener(MainFrame.mainframe);
                    add(mi, 0);
                    // } else {
                    //     /* If file does not exist, add entry to the garbage bin */
                    //     filesNotFound.add(s);
                    // }
                }
                // for (String s : filesNotFound) {
                //     /* Now remove any invalid strings */
                //     Preferences.getInstance().removeRecentFile(s);
                // }
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

}
