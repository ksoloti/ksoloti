/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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

import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class SCmdCreateDirectory extends AbstractSCmd {

    private static final Logger LOGGER = Logger.getLogger(SCmdCreateDirectory.class.getName());

    private String dirname;
    private Calendar date;

    public SCmdCreateDirectory(String dirname, Calendar date) {
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
        return "Done creating directory.\n";
    }

    @Override
    public boolean isSuccessful() {
        return mcuStatusCode == 0x00 || mcuStatusCode == 0x08;
    }

    @Override
    public SCmd Do(Connection connection) {
        LOGGER.info(GetStartMessage());

        int writeResult = connection.TransmitCreateDirectory(dirname, date);
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Failed to send create directory command for " + dirname + ": USB write error.");
            setCompletedWithStatus(1);
            return this;
        }
        connection.setCurrentExecutingCommand(this);

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "Create directory command for " + dirname + " timed out.");
                setCompletedWithStatus(1);
                return this;
            }
            else if (!isSuccessful()) {
                LOGGER.log(Level.SEVERE, "Failed to create directory " + dirname + ".");
                return this;
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Create directory command for " + dirname + " interrupted: " + e.getMessage());
            e.printStackTrace(System.out);
            setCompletedWithStatus(1);
            return this;
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during create directory command for " + dirname + ": " + e.getMessage());
            e.printStackTrace(System.out);
            setCompletedWithStatus(1);
            return this;
        }
        finally {
            connection.clearIfCurrentExecutingCommand(this);
        }
        LOGGER.info(GetDoneMessage());
        return this;
    }
}
