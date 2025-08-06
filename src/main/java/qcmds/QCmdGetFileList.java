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
import axoloti.sd.SDCardInfo;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdGetFileList extends AbstractQCmdSerialTask {
    private static final Logger LOGGER = Logger.getLogger(QCmdGetFileList.class.getName());

    public QCmdGetFileList() {
        this.expectedAckCommandByte = 'l'; // Expecting AxoRl
    }

    @Override
    public String GetStartMessage() {
        return "Receiving SD card file list...";
    }

    @Override
    public String GetDoneMessage() {
        return "Receiving SD card file list " + (isSuccessful() ? "successful" : "failed");
    }

    @Override
    public QCmd Do(Connection connection) {
        super.Do(connection);

        setMcuStatusCode((byte)0xFF);

        /* This method sends the Axol packet to the MCU. */
        int writeResult = connection.TransmitGetFileList();
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Get file list failed: USB write error.");
            setMcuStatusCode((byte)0x01); // FR_DISK_ERR
            setCompletedWithStatus(false);
            return this;
        }

        try {
            /* Wait for the final AxoRl response from the MCU */
            if (!waitForCompletion(10000)) { // 10-second timeout
                LOGGER.log(Level.SEVERE, "Get file list failed: Core did not acknowledge full listing within timeout (AxoRl).");
                setMcuStatusCode((byte)0x0F); // FR_TIMEOUT
                setCompletedWithStatus(false);
            } else {
                System.out.println(Instant.now() + " Get file list completed with status: " + SDCardInfo.getFatFsErrorString(getMcuStatusCode()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Get file list interrupted: {0}", e.getMessage());
            setMcuStatusCode((byte)0x02); // FR_INT_ERR
            setCompletedWithStatus(false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during file list retrieval: {0}", e.getMessage());
            setMcuStatusCode((byte)0xFF); // Generic error
            setCompletedWithStatus(false);
        }
        return this;
    }
}
