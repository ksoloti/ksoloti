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
package qcmds;

import axoloti.Connection;
import axoloti.SDCardInfo;
import axoloti.USBBulkConnection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.usb4java.LibUsb;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdDeleteFile extends AbstractQCmdSerialTask {
    private static final Logger LOGGER = Logger.getLogger(QCmdDeleteFile.class.getName());

    private String filename;

    public QCmdDeleteFile(String filename) {
        this.filename = filename;
        this.expectedAckCommandByte = 'D'; // Expecting AxoRD
    }

    @Override
    public String GetStartMessage() {
        return "Deleting file from SD card... " + filename;
    }

    @Override
    public String GetDoneMessage() {
        return "Delete file " + (isSuccessful() ? "successful" : "failed") + " for " + filename;
    }

    @Override
    public QCmd Do(Connection connection) {
        super.Do(connection); // Sets 'this' as currentExecutingCommand

        setMcuStatusCode((byte)0xFF);

        int writeResult = USBBulkConnection.GetConnection().TransmitDeleteFile(filename); // Pass 'this' as senderCommand
        if (writeResult != LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Delete file failed for " + filename + ": USB write error.");
            setMcuStatusCode((byte)0x01); // FR_DISK_ERR
            setCommandCompleted(false);
            return this;
        }

        try {
            if (!waitForCompletion()) { // 3-second timeout
                LOGGER.log(Level.SEVERE, "Delete file failed for " + filename + ": Core did not acknowledge within timeout.");
                setMcuStatusCode((byte)0x0F); // FR_TIMEOUT
                setCommandCompleted(false);
            } else {
                LOGGER.log(Level.INFO, "Delete file " + filename + " completed with status: " + SDCardInfo.getFatFsErrorString(getMcuStatusCode()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Delete file for " + filename + " interrupted: {0}", e.getMessage());
            setMcuStatusCode((byte)0x02); // FR_INT_ERR
            setCommandCompleted(false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during file deletion for " + filename + ": {0}", e.getMessage());
            setMcuStatusCode((byte)0xFF); // Generic error
            setCommandCompleted(false);
        }
        return this;
    }
}
