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

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.Connection;
import axoloti.sd.SDCardInfo;

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
        connection.setCurrentExecutingCommand(this);

        int writeResult = connection.TransmitCopyToFlash();
        if (writeResult != org.usb4java.LibUsb.SUCCESS) {
            LOGGER.log(Level.SEVERE, "QCmdCopyPatchToFlash: Failed to send TransmitCopyToFlash: USB write error.");
            setMcuStatusCode((byte)0x01); // FR_DISK_ERR
            setCompletedWithStatus(false);
            return this;
        }

        try {
            if (!waitForCompletion()) {
                LOGGER.log(Level.SEVERE, "QCmdCopyPatchToFlash: Core did not acknowledge within timeout.");
                setMcuStatusCode((byte)0x0F); // FR_TIMEOUT
                setCompletedWithStatus(false);
            } else {
                System.out.println(Instant.now() + " QCmdCopyPatchToFlash completed with status: " + SDCardInfo.getFatFsErrorString(getMcuStatusCode()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "QCmdCopyPatchToFlash interrupted: {0}", e.getMessage());
            setMcuStatusCode((byte)0x02); // FR_INT_ERR
            setCompletedWithStatus(false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during QCmdCopyPatchToFlash: {0}", e.getMessage());
            setMcuStatusCode((byte)0xFF); // Generic error
            setCompletedWithStatus(false);
        }
        return this;
    }
}
