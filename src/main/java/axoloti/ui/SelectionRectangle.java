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

package axoloti.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 *
 * @author Johannes Taelman
 */
public class SelectionRectangle extends JPanel {

    public SelectionRectangle() {
        setLayout(null);
        setLocation(10, 10);
        setSize(10, 10);
        setOpaque(true);
        setBackground(Theme.Selection_Rectangle_Fill);
        setBorder(BorderFactory.createLineBorder(Theme.Button_Accent_Background));
        setVisible(false);
    }

}
