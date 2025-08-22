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
import axoloti.sd.SDCardInfo;

import java.time.Instant;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdCreateDirectory extends AbstractQCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdCreateDirectory.class.getName());

    private String dirname;
    private Calendar date;

    public QCmdCreateDirectory(String dirname, Calendar date) {
        this.dirname = dirname;
        this.date = date;
        this.expectedAckCommandByte = 'k'; // Expecting AxoRk
    }

    @Override
    public String GetStartMessage() {
        return "Creating directory on SD card... " + dirname;
    }

    @Override
    public String GetDoneMessage() {
        return "Create directory " + (isSuccessful() ? "successful" : "failed") + " for " + dirname;
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.setCurrentExecutingCommand(this);

        int writeResult = connection.TransmitCreateDirectory(dirname, date);
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Create directory failed for " + dirname + ": USB write error.");
            setMcuStatusCode((byte)0x01); // FR_DISK_ERR or custom error for USB comms
            setCompletedWithStatus(false);
            return this;
        }

        try {
            if (!waitForCompletion()) { // 5-second timeout for MCU ACK
                LOGGER.log(Level.SEVERE, "Create directory failed for " + dirname + ": Core did not acknowledge within timeout.");
                setMcuStatusCode((byte)0x0F); // FR_TIMEOUT
                setCompletedWithStatus(false);
            }
            else {
                // Status code and completion flag are already set by processByte()
                System.out.println(Instant.now() + " Create directory " + dirname + " completed with status: " + SDCardInfo.getFatFsErrorString(getMcuStatusCode()));
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Create directory for " + dirname + " interrupted: {0}", e.getMessage());
            setMcuStatusCode((byte)0x02); // FR_INT_ERR
            setCompletedWithStatus(false);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during directory creation for " + dirname + ": {0}", e.getMessage());
            setMcuStatusCode((byte)0xFF); // Generic error
            setCompletedWithStatus(false);
        }
        return this;
    }

    @Override
    public boolean isSuccessful() {
        // For QCmdCreateDirectory, success means both commandSuccess (no Java-side comms error)
        // AND mcuStatusCode is FR_OK (0x00) OR FR_EXIST (0x08).
        return commandSuccess && (mcuStatusCode == 0x00 || mcuStatusCode == 0x08);
    }
}
