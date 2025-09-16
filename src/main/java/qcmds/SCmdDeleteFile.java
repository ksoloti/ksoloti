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

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.usb4java.LibUsb;

/**
 *
 * @author Johannes Taelman
 */
public class SCmdDeleteFile extends AbstractSCmd {
    private static final Logger LOGGER = Logger.getLogger(SCmdDeleteFile.class.getName());

    private String filename;

    public SCmdDeleteFile(String filename) {
        this.filename = filename;
        this.expectedAckCommandByte = 'D'; // Expecting AxoRD
    }

    @Override
    public String GetStartMessage() {
        return "Deleting file from SD card... " + filename;
    }

    @Override
    public String GetDoneMessage() {
        return null;
    }

    @Override
    public SCmd Do(Connection connection) {
        LOGGER.info(GetStartMessage());
        connection.setCurrentExecutingCommand(this);

        if (!connection.isConnected()) {
            LOGGER.log(Level.SEVERE, "Failed to send delete file command for " + filename + ": USB connection lost.");
            return this;
        }

        int writeResult = connection.TransmitDeleteFile(filename);
        if (writeResult != LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "Failed to send delete file command for " + filename + ": USB write error.");
            setCompletedWithStatus(1);
            return this;
        }

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "Delete file command for " + filename + " timed out.");
                setCompletedWithStatus(1);
                return this;
            }
            else if (!isSuccessful()) {
                LOGGER.log(Level.SEVERE, "Failed to delete file " + filename + ".");
                return this;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Delete file command interrupted for " + filename + ": " + e.getMessage());
            e.printStackTrace(System.out);
            setCompletedWithStatus(1);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during delete file command for " + filename + ": " + e.getMessage());
            e.printStackTrace(System.out);
            setCompletedWithStatus(1);
        }
        return this;
    }

    public SCmd Do(Connection connection, boolean silent) {
        /* 'silent' argument is a dummy for the purpose of overloading this method.
           Calling this will direct all log messages to the CLI only. */
        System.out.println(Instant.now() + " " + GetStartMessage());
        connection.setCurrentExecutingCommand(this);

        if (!connection.isConnected()) {
            System.out.println(Instant.now() + " Failed to send delete file command for " + filename + ": USB connection lost.");
            setCompletedWithStatus(1);
            return this;
        }

        int writeResult = connection.TransmitDeleteFile(filename);
        if (writeResult != LibUsb.SUCCESS) {
            System.out.println(Instant.now() + " Failed to send delete file command for " + filename + ": USB write error.");
            setCompletedWithStatus(1);
            return this;
        }

        try {
            if (!waitForCompletion()) {
                System.out.println(Instant.now() + " Delete file command for " + filename + " timed out.");
                setCompletedWithStatus(1);
                return this;
            }
            else if (!isSuccessful()) {
                System.out.println(Instant.now() + " Failed to delete file " + filename + ".");
                return this;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Instant.now() + " Delete file command interrupted for " + filename + ": " + e.getMessage());
            e.printStackTrace(System.out);
            setCompletedWithStatus(1);
        } catch (Exception e) {
            System.out.println(Instant.now() + " Error during delete file command for " + filename + ": " + e.getMessage());
            e.printStackTrace(System.out);
            setCompletedWithStatus(1);
        }
        return this;
    }
}
