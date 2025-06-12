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
 * A Comparator for SDFileInfo objects that sorts them in a hierarchical,
 * explorer-like manner.
 * 
 * 1. Folders are sorted (case-insensitive) by their paths.
 *    (e.g., "/data/", "/media/", "/MyFolder")
 * 2. Within the same folder:
 *    Files are sorted alphabetically (case-insensitive) by their pure name (last component).
 * 3. Each subfolder is shown as an own entry after its parent folder.
 *    In other words, the "indent" is only to show files inside a folder.
 *    (e.g., "/data/logs/" will come as an own entry after "/data/" (i.e. not shown "inside" /data/).
 *     Files inside /data/ are listed under /data/,
 *     files inside /data/logs/ are listed under /data/logs/, etc.)
 * 4. Files in the root directory come last, without indent, but with leading slash
 *    (e.g., "/myPatchData", "/start.bin", /untitled.txt")
 */

/**
 *
 * @author Ksoloti
 */
public class AxoSDFileComparator implements Comparator<SDFileInfo> {

    @Override
    public int compare(SDFileInfo o1, SDFileInfo o2) {
        /* Get the paths for comparison, removing the initial leading slash.
           This simplifies extraction of parent paths and pure names for non-root items. */
        String path1 = o1.getFilename().substring(1); // e.g., "data/logs/" or "README.md"
        String path2 = o2.getFilename().substring(1);

        /* 1. Determine if items share a common parent directory.
              Extract the parent path (e.g., "data/logs" for "data/logs/file.txt") */
        int lastSlash1 = path1.lastIndexOf('/');
        int lastSlash2 = path2.lastIndexOf('/');

        if (lastSlash1 != -1 && lastSlash2 == -1) {
            return -1; /* o1 is a directory, o2 is a file in root folder => o1 comes first */
        }
        if (lastSlash1 == -1 && lastSlash2 != -1) {
            return 1; /* o1 is a file in root folder, o2 is a directory => o2 comes first */
        }

        String parentPath1 = (lastSlash1 == -1) ? "" : path1.substring(0, lastSlash1);
        String parentPath2 = (lastSlash2 == -1) ? "" : path2.substring(0, lastSlash2);

        /* Compare parent paths first (case-insensitive and alphabetically) */
        int parentCmp = parentPath1.toLowerCase().compareTo(parentPath2.toLowerCase());
        if (parentCmp != 0) {
            return parentCmp; /* If parents are different, sort by parent path */
        }

        /* At this point, o1 and o2 are in the same parent directory (or both are in the root).
           Now apply the rules for items within the same directory: */

        /* 2. Directories before Files. */
        if (o1.isDirectory() && !o2.isDirectory()) {
            return -1; /* o1 is a directory, o2 is a file => o1 comes first */
        }
        if (!o1.isDirectory() && o2.isDirectory()) {
            return 1;  /* o1 is a file, o2 is a directory => o2 comes first */
        }

        /* 3. If both are directories OR both are files (and in the same parent),
              sort alphabetically by their pure name (the last component).
              The SDFileInfo.getPureName() method is crucial here. */
        return o1.getPureName().toLowerCase().compareTo(o2.getPureName().toLowerCase());
    }
}