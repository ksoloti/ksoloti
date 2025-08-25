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

import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.Connection;

/**
 *
 * @author Johannes Taelman
 */
public class SCmdGetFileList extends AbstractSCmd {
    private static final Logger LOGGER = Logger.getLogger(SCmdGetFileList.class.getName());

    public SCmdGetFileList() {
        this.expectedAckCommandByte = 'l'; // Expecting AxoRl
    }

    @Override
    public String GetStartMessage() {
        return null;
    }

    @Override
    public String GetDoneMessage() {
        return null;
        // return "Receiving SD card file list " + (isSuccessful() ? "successful" : "failed");
    }

    @Override
    public SCmd Do(Connection connection) {
        connection.setCurrentExecutingCommand(this);

        /* This method sends the Axol packet to the MCU. */
        int writeResult = connection.TransmitGetFileList();
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Get file list failed: USB write error.");
            setCompletedWithStatus(1);
            return this;
        }

        try {
            /* Wait for the final AxoRl response from the MCU */
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "Get file list command timed out.");
                setCompletedWithStatus(1);
                return this;
            }
            else if (!isSuccessful()) {
                LOGGER.log(Level.SEVERE, "Failed to get file list.");
                setCompletedWithStatus(1);
                return this;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Get file list command interrupted: " + e.getMessage());
            setCompletedWithStatus(1);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during get file list command: " + e.getMessage());
            setCompletedWithStatus(1);
        }
        return this;
    }
}
