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

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdDeleteFile extends AbstractQCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdDeleteFile.class.getName());

    boolean success = true;

    final String filename;

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
        if (this.success) {
            SDCardInfo.getInstance().Delete(filename);
            return "Done deleting file.\n";
        }
        else {
            return "Failed to delete file (see MCU status in console/logs)\n";
        }
    }
    
    @Override
    public QCmd Do(Connection connection) {
        connection.ClearSync();
        connection.ClearReadSync();

        USBBulkConnection usbConnection = (USBBulkConnection) connection;
        usbConnection.TransmitDeleteFile(filename, this);

        try {
            /* Check if the command finishes (latch counted down) or times out */
            boolean commandCompleted = this.waitForCompletion(5000);

            if (commandCompleted) {
                if (isSuccessful()) {
                    System.out.println(Instant.now() + " DeleteFile operation confirmed successful by MCU.");
                    this.success = true;
                }
                else {
                    System.out.println(Instant.now() + " DeleteFile operation confirmed failed by MCU.");
                    this.success = false;
                }
            }
            else {
                System.out.println(Instant.now() + " DeleteFile operation timed out waiting for MCU response (no AxoR received).");
                this.success = false;
            }
        }
        catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "DeleteFile Do() interrupted while waiting for completion", ex);
            Thread.currentThread().interrupt();
            this.success = false;
        }
        return this;
    }
}
