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

import static axoloti.menus.PopulatePatchMenu.populatePatchMenu;
import axoloti.utils.AxolotiLibrary;
import axoloti.utils.Preferences;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 *
 * @author jtaelman
 */
public class LibraryMenu extends JMenu {

    public LibraryMenu() {
        setDelay(300);

        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                for (AxolotiLibrary lib1 : Preferences.getInstance().getLibraries() ) {
                    JMenu plib = new JMenu(lib1.getId());
                    plib.setDelay(300);
                    populatePatchMenu(plib, lib1.getLocalLocation() + "/patches/", ".axp");
                    add(plib);                    
                }
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
