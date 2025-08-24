/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
 * @author jtaelman
 */
public class QCmdCopyPatchToFlash extends AbstractQCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdCopyPatchToFlash.class.getName());

    public QCmdCopyPatchToFlash() {
        this.expectedAckCommandByte = 'F';
    }

    @Override
    public String GetStartMessage() {
        return "Writing patch to flash...";
    }

    @Override
    public String GetDoneMessage() {
        return "Done writing patch to flash.\n";
    }

    @Override
    public QCmd Do(Connection connection) {
        LOGGER.info(GetStartMessage());
        connection.setCurrentExecutingCommand(this);

        int writeResult = connection.TransmitCopyToFlash();
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Failed to send TransmitCopyToFlash: USB write error.");
            setCompletedWithStatus(1);
            return this;
        }

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "Copy patch to Flash command timed out.");
                setCompletedWithStatus(1);
                return this;
            } else if (!isSuccessful()) {
                LOGGER.log(Level.SEVERE, "Failed to copy patch to Flash.");
                setCompletedWithStatus(1);
                return this;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Copy patch to Flash command interrupted: " + e.getMessage());
            setCompletedWithStatus(1);
            return this;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during copy patch to Flash command: " + e.getMessage());
            setCompletedWithStatus(1);
            return this;
        }
        LOGGER.info(GetDoneMessage());
        return this;
    }
}
