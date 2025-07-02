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

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.Connection;
import axoloti.SDCardInfo;
import axoloti.USBBulkConnection;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdGetFileList extends AbstractQCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdGetFileList.class.getName());

    boolean success = true;

    @Override
    public String GetStartMessage() {
        return "Receiving SD card file list...";
    }

    @Override
    public String GetDoneMessage() {
        if (this.success) {
            return "Done receiving SD card file list.\n";
        }
        else {
            return "Incomplete SD card file list.\n";
        }
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.ClearSync();
        connection.ClearReadSync();
        SDCardInfo.getInstance().setBusy();

        /* Cast to the concrete type to access the specific methods */
        USBBulkConnection usbConnection = (USBBulkConnection) connection;
        usbConnection.TransmitGetFileList(this);

        try {
            /* Check if the command finishes (latch counted down) or times out */
            boolean commandCompleted = this.waitForCompletion(5000);

            if (commandCompleted) {
                if (isSuccessful()) {
                    System.out.println(Instant.now() + " TransmitGetFileList.Do() operation confirmed successful by MCU.");
                    this.success = true;
                }
                else {
                    System.out.println(Instant.now() + " TransmitGetFileList.Do() operation confirmed failed by MCU.");
                    this.success = false;
                }
            }
            else {
                System.out.println(Instant.now() + " TransmitGetFileList.Do() operation timed out waiting for MCU response (no AxoR received).");
                this.success = false;
            }
        }
        catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "TransmitGetFileList.Do() interrupted while waiting for completion", ex);
            Thread.currentThread().interrupt();
            this.success = false;
        }
        finally {
            SDCardInfo.getInstance().clearBusy();
        }
        return this;
    }
}
