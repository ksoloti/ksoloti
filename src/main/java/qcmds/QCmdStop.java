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
public class QCmdStop extends AbstractQCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdStop.class.getName());

    public QCmdStop() {
        this.expectedAckCommandByte = 'S';
    }
    
    @Override
    public String GetStartMessage() {
        return null;//Start stopping patch";
    }

    @Override
    public String GetDoneMessage() {
        return null;//Done stopping patch";
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.setCurrentExecutingCommand(this);

        int writeResult = connection.TransmitStop();
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "QCmdStop: Failed to send TransmitStop: USB write error.");
            setCompletedWithStatus(false);
            return this;
        }

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "QCmdStop: Core did not acknowledge within timeout.");
                setCompletedWithStatus(false);
            }
            else {
                System.out.println(Instant.now() + " QCmdStop completed with status: " + SDCardInfo.getFatFsErrorString(getMcuStatusCode()));
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "QCmdStop interrupted: {0}", e.getMessage());
            setCompletedWithStatus(false);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during QCmdStop: {0}", e.getMessage());
            setCompletedWithStatus(false);
        }
        return this;
    }
}
