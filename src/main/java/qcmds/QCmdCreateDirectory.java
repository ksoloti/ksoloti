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
        return "Done creating directory.";
    }

    @Override
    public boolean isSuccessful() {
        return commandSuccess && (mcuStatusCode == 0x00 || mcuStatusCode == 0x08);
    }

    @Override
    public QCmd Do(Connection connection) {
        LOGGER.info(GetStartMessage());
        connection.setCurrentExecutingCommand(this);

        int writeResult = connection.TransmitCreateDirectory(dirname, date);
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Create directory failed for " + dirname + ": USB write error.");
            setCompletedWithStatus(1);
            return this;
        }

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "Create directory command for " + dirname + " timed out.");
                setCompletedWithStatus(1);
                return this;
            }
            else if (!isSuccessful()) {
                LOGGER.log(Level.SEVERE, "Failed to create directory " + dirname + ".");
                setCompletedWithStatus(1);
                return this;
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Create directory command for " + dirname + " interrupted: " + e.getMessage());
            setCompletedWithStatus(1);
            return this;
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during create directory command for " + dirname + ": " + e.getMessage());
            setCompletedWithStatus(1);
            return this;
        }
        LOGGER.info(GetDoneMessage());
        return this;
    }
}
