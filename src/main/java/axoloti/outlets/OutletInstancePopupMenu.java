/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
package axoloti.outlets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * @author Johannes Taelman
 */
public class OutletInstancePopupMenu extends JPopupMenu {

    OutletInstance outletinstance;

    public OutletInstancePopupMenu(OutletInstance outletinstance1) {
        super();
        this.outletinstance = outletinstance1;
        JMenuItem itemDisconnect = new JMenuItem("Disconnect outlet");
        JMenuItem itemDelete = new JMenuItem("Delete net");
        itemDisconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                outletinstance.disconnect();
            }
        });
        itemDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                outletinstance.deleteNet();
            }
        });
        this.add(itemDisconnect);
        this.add(itemDelete);
    }
}
