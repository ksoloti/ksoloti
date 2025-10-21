/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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

import javax.swing.*;
import javax.swing.filechooser.FileView;
import java.io.File;

public class SvgFileView extends FileView {

    @Override
    public Icon getIcon(File f) {
        if (f.isDirectory()) {
            return SvgIconLoader.load("/resources/icons/folder-svgrepo-com.svg", 18, Theme.Button_Accent_Background);
        } else {
            return SvgIconLoader.load("/resources/icons/file-code-svgrepo-com.svg", 18, Theme.Button_Accent_Background);
        }
    }
}