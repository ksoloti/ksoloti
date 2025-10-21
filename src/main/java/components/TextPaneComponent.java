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
import java.awt.Dimension;
import javax.swing.JTextPane;

import axoloti.ui.Theme;

/**
 *
 * @author Johannes Taelman, sebiik
 */
public class TextPaneComponent extends JTextPane {


    public TextPaneComponent() {
        initComponents();
    }

    public TextPaneComponent(String text) {
        setText(text);
        initComponents();
    }

    private void initComponents() {
        // setContentType("text/plain");
        setMinimumSize(new Dimension(14,14));
        // setFont(Constants.FONT);
        setBackground(Theme.Object_Default_Background);
        setForeground(Theme.Object_Label_Text);
        setEditable(false);
        setFocusable(false);
    }
}
