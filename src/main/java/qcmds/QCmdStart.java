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

import axoloti.Connection;
import axoloti.Patch;
import axoloti.sd.SDCardInfo;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdStart extends AbstractQCmdSerialTask {
    
    private static final Logger LOGGER = Logger.getLogger(QCmdStart.class.getName());
    Patch p;

    public QCmdStart(Patch p) {
        this.p = p;
        this.expectedAckCommandByte = 's';
    }

    @Override
    public String GetStartMessage() {
        return null;
    }

    @Override
    public String GetDoneMessage() {
        return null;
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.setCurrentExecutingCommand(this);

        connection.setPatch(p);

        int writeResult = connection.TransmitStart();
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "QCmdStart: Failed to send TransmitStart: USB write error.");
            setCompletedWithStatus(false);
            return this;
        }

        try {
            if (this instanceof QCmdStartFlasher || this instanceof QCmdStartMounter) {
                /* We won't get any "start patch" response from these commands
                   as they force an immediate reboot into Flasher/Mounter mode.
                   Hard-coded success here. So alpha. */
                setCompletedWithStatus(true);
                return this;
            }

            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "QCmdStart: Core did not acknowledge within timeout.");
                setCompletedWithStatus(false);
            }
            else {
                System.out.println(Instant.now() + " QCmdStart completed with status: " + SDCardInfo.getFatFsErrorString(getMcuStatusCode()));
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "QCmdStart interrupted: {0}", e.getMessage());
            setCompletedWithStatus(false);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during QCmdStart: {0}", e.getMessage());
            setCompletedWithStatus(false);
        }
        return this;
    }
}
