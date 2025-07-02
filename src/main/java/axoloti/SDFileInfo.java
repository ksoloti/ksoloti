/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
 * Edited 2023 - 2024 by Ksoloti
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
package axoloti;

import java.util.Calendar;

/**
 *
 * @author jtaelman
 */
public class SDFileInfo {

    String filename;
    Calendar timestamp;
    int size;
    private boolean isDirectory;

    public SDFileInfo(String filename, Calendar timestamp, int size, boolean isDirectory) {
        this.filename = filename;
        this.timestamp = timestamp;
        this.size = size;
        this.isDirectory = isDirectory;
    }

    public SDFileInfo(String filename, boolean isDirectory) {
        /* Dummy constructor for "/" root */
        this(filename, null, 0, isDirectory);
    }

    public String getFilename() {
        return filename;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public int getSize() {
        return size;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getFilenameNoExtension() {
        int i = filename.lastIndexOf('.');
        if (i > 0 && !isDirectory) {
            return filename.substring(0, i);
        } else {
            return filename;
        }
    }

    public String getExtension() {
        int i = filename.lastIndexOf('.');
        if (i > 0 && !isDirectory) {
            return filename.substring(i + 1);
        } else {
            return "";
        }
    }

    public String getPatchFileName() {
        if (!isDirectory) {
            int i = filename.lastIndexOf('/');
            return filename.substring(i + 1);
        }
        return "";
    }

    public String getPureName() {

        String path = filename;
        if (path.startsWith("/")) {
            /* Remove leading slash for name extraction */
            path = path.substring(1);
        }

        if (isDirectory && path.endsWith("/") && path.length() > 0) {
            /* Remove trailing slash for dir name */
            path = path.substring(0, path.length() - 1);
        }

        int lastSlash = path.lastIndexOf('/');

        return (lastSlash == -1) ? path : path.substring(lastSlash + 1);
    }
}
