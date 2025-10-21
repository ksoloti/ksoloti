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
package components;

import axoloti.ui.Theme;
import axoloti.utils.Constants;

import java.awt.Dimension;

import javax.swing.JLabel;

/**
 *
 * @author Johannes Taelman
 */
public class LabelComponent extends JLabel {

    public LabelComponent(String text) {
        super(text);
        setMaximumSize(new Dimension(32768,13));
        setMinimumSize(new Dimension(12,13));
        setFont(Constants.FONT);
        setBackground(Theme.Object_Default_Background);
        setForeground(Theme.Object_Label_Text);
    }
}
