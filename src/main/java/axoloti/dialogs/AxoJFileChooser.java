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

package axoloti.dialogs;

import java.awt.Dimension;
import javax.swing.JFileChooser;

import axoloti.ui.SvgFileView;

/**
 *
 * @author Ksoloti
 */
public class AxoJFileChooser extends JFileChooser {

    private Dimension currentSize;
    private static final Dimension DEFAULT_SIZE = new Dimension(720, 480);

    public AxoJFileChooser(String path) {
        super(path);
        currentSize = DEFAULT_SIZE;
        setPreferredSize(currentSize);
        this.setFileView(new SvgFileView());
        AxoBookmarksPanel panel = new AxoBookmarksPanel();
        panel.setOwner(this);
        this.setAccessory(panel);
    }

    public void restoreCurrentSize() {
        setPreferredSize(currentSize);
    }

    public void updateCurrentSize() {
        if (getSize() != null) {
            currentSize = getSize();
        }
    }
}