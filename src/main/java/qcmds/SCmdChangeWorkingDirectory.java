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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class SCmdChangeWorkingDirectory extends AbstractSCmd {
    private static final Logger LOGGER = Logger.getLogger(SCmdChangeWorkingDirectory.class.getName());

    private String path;

    public SCmdChangeWorkingDirectory(String path) {
        this.path = path;
        this.expectedAckCommandByte = 'h';
    }

    @Override
    public String GetStartMessage() {
        return "Changing working directory to: " + path;
    }

    @Override
    public String GetDoneMessage() {
        return null;
    }

    @Override
    public SCmd Do(Connection connection) {
        LOGGER.info(GetStartMessage());
        connection.setCurrentExecutingCommand(this);

        int writeResult = connection.TransmitChangeWorkingDirectory(path);
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Failed to send change directory command for " + path + ": USB write error.");
            setCompletedWithStatus(1);
            return this;
        }

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "Change directory command for " + path + " timed out.");
                setCompletedWithStatus(1);
                return this;
            }
            else if (!isSuccessful()) {
                LOGGER.log(Level.SEVERE, "Failed to change directory to " + path + ".");
                return this;
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Change directory command for " + path + " interrupted: " + e.getMessage());
            setCompletedWithStatus(1);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during change directory command for " + path + ": " + e.getMessage());
            setCompletedWithStatus(1);
        }
        return this;
    }
}
