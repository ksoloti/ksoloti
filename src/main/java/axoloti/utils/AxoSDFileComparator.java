/**
 * Copyright (C) 2023 - 2024 by Ksoloti
 *
 * This file is part of Ksoloti.
 *
 * Ksoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Ksoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Ksoloti. If not, see <http://www.gnu.org/licenses/>.
 */

package axoloti.utils;

import java.util.Comparator;

import axoloti.SDFileInfo;

/**
 *
 * @author Ksoloti
 */
public class AxoSDFileComparator implements Comparator<AxoSDFileNode> { // Or Comparator<SDFileInfo> if preferred for internal node sorting

    @Override
    public int compare(AxoSDFileNode node1, AxoSDFileNode node2) {
        SDFileInfo o1 = node1.getFileInfo(); // Get the actual SDFileInfo
        SDFileInfo o2 = node2.getFileInfo();

        boolean isDir1 = o1.isDirectory();
        boolean isDir2 = o2.isDirectory();

        // 1. Directories always come before files at the same level
        if (isDir1 && !isDir2) {
            return -1; // Directory before file
        }
        if (!isDir1 && isDir2) {
            return 1; // File after directory
        }

        return o1.getFilename().compareToIgnoreCase(o2.getFilename());
    }
}