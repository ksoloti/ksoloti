/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
package qcmds;

import axoloti.Connection;
import axoloti.SDCardInfo;

import static axoloti.MainFrame.mainframe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdUploadFile implements QCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdUploadFile.class.getName());

    InputStream inputStream;
    final String filename;
    final Calendar cal;
    File file;
    long size;
    long tsEpoch;
    boolean success = false;

    public QCmdUploadFile(InputStream inputStream, String filename) {
        this.inputStream = inputStream;
        this.filename = filename;
        this.cal = null;
    }

    public QCmdUploadFile(File file, String filename) {
        this.file = file;
        this.filename = filename;
        inputStream = null;
        this.cal = null;
    }

    public QCmdUploadFile(File file, String filename, Calendar cal) {
        this.file = file;
        this.filename = filename;
        inputStream = null;
        this.cal = cal;
    }

    @Override
    public String GetStartMessage() {
        return null; // "Uploading file to SD card... " + filename;
    }

    @Override
    public String GetDoneMessage() {
        return null;
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.ClearSync();
        try {
            if (inputStream == null) {
                if (!file.exists()) {
                    LOGGER.log(Level.WARNING, "File does not exist: " + filename + "\n");
                    success = false;
                    return this;
                }
                if (file.isDirectory()) {
                    LOGGER.log(Level.WARNING, "Cannot upload directories: " + filename + "\n");
                    success = false;
                    return this;
                }
                if (!file.canRead()) {
                    LOGGER.log(Level.WARNING, "Cannot read file: " + filename + "\n");
                    success = false;
                    return this;
                }
                inputStream = new FileInputStream(file);
            }
            LOGGER.log(Level.INFO, "Uploading file to SD card: " + filename);
            Calendar ts;
            if (cal != null) {
                ts = cal;
            } else if (file != null) {
                ts = Calendar.getInstance();
                ts.setTimeInMillis(file.lastModified());
            } else {
                ts = Calendar.getInstance();
            }
            int tlength = inputStream.available();
            LOGGER.log(Level.INFO, "Size: " + tlength + " bytes\n");
            int remLength = inputStream.available();
            size = tlength;
            connection.TransmitCreateFile(filename, tlength, ts);
            int MaxBlockSize = 32768;
            long pct = 0;
            do {
                int l;
                if (remLength > MaxBlockSize) {
                    l = MaxBlockSize;
                    remLength -= MaxBlockSize;
                } else {
                    l = remLength;
                    remLength = 0;
                }
                byte[] buffer = new byte[l];
                long nRead = inputStream.read(buffer, 0, l);
                if (nRead != l) {
                    LOGGER.log(Level.SEVERE, "Wrong file size? " + nRead);
                }
                connection.TransmitAppendFile(buffer);
                long newpct = (100 * ((long) tlength - (long) remLength)) / (long) tlength;
                if (newpct != pct) {
                    StringBuilder progressbar = new StringBuilder("                         "); /* 25-chars long progress bar */
                    for (int i = 0; i < (int) newpct/4; i++) {
                        progressbar.setCharAt(i, '='); /* fill the progress bar depending on the percentage */
                    }
                    /* Avoid printing multple lines of progress bars */
                    mainframe.consoleRemoveLastLine();
                    LOGGER.log(Level.INFO, "Uploading\t[" + progressbar + "] " + String.format("%3d", newpct) + "%");
                }
                pct = newpct;
                remLength = inputStream.available();
            } while (remLength > 0);

            inputStream.close();
            connection.TransmitCloseFile();

            SDCardInfo.getInstance().AddFile(filename, (int) size, ts);
            LOGGER.log(Level.INFO, "Done uploading file.\n");
            success = true;
            return this;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }
        LOGGER.log(Level.WARNING, "File upload failed: " + filename + "\n");
        success = false;
        return this;
    }
}
