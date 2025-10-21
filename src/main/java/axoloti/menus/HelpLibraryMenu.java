/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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

import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Preferences;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 *
 * @author jtaelman
 */
public class HelpLibraryMenu extends JMenu {

    public HelpLibraryMenu() {
        setDelay(300);
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                for (AxolotiLibrary lib : Preferences.getInstance().getLibraries()) {
                    JMenu mi = new JMenu(lib.getId());
                    mi.setDelay(300);
                    String dir = lib.getLocalLocation() + File.separator + "objects/";
                    PopulatePatchMenu.populatePatchMenu(mi, dir, ".axh");
                    HelpLibraryMenu.this.add(mi);
                }
            }

            @Override
            public void menuDeselected(MenuEvent e
            ) {
                removeAll();
            }

            @Override
            public void menuCanceled(MenuEvent e
            ) {
                removeAll();
            }
        }
        );
    }
}
