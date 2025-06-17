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

import javax.swing.SwingUtilities;

import axoloti.Connection;
import axoloti.MainFrame;
import axoloti.SDCardInfo;
import axoloti.USBBulkConnection;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdGetFileList implements QCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdGetFileList.class.getName());

    boolean done = true;

    @Override
    public String GetStartMessage() {
        return "Receiving SD card file list...";
    }

    @Override
    public String GetDoneMessage() {
        if (done) {
            return "Done receiving SD card file list.\n";
        } else {
            return "Incomplete SD card file list.\n";
        }
    }

    @Override
    public QCmd Do(Connection connection) {

        LOGGER.log(Level.INFO, "QCmdGetFileList.Do(): Starting command execution.");

        connection.ClearSync();
        connection.ClearReadSync();

        /* Cast to the concrete type to access the specific methods */
        USBBulkConnection usbBulkConnection = (USBBulkConnection) connection;

        // 1. Reset the synchronization state
        usbBulkConnection.clearFileListSync(); // Call your existing method
        LOGGER.log(Level.INFO, "QCmdGetFileList.Do(): fileListSync state cleared (fileListDone reset to false).");

        // 2. Clear the SDCardInfo list before populating with new data
        SDCardInfo.getInstance().clear();
        LOGGER.log(Level.INFO, "QCmdGetFileList.Do(): SDCardInfo cleared.");

        // 3. Send the command to the firmware
        usbBulkConnection.TransmitGetFileList();
        LOGGER.log(Level.INFO, "QCmdGetFileList.Do(): TransmitGetFileList command sent (Axod).");

        LOGGER.log(Level.INFO, "QCmdGetFileList.Do(): Waiting for AxoE signal for 10000ms...");

        // 4. Wait for the firmware to finish sending the list
        boolean success = usbBulkConnection.waitFileListSync(10000);
        this.done = success; // Update the 'done' flag

        if (success) {
            LOGGER.log(Level.INFO, "QCmdGetFileList: AxoE signal received. File list transfer complete.");
        } else {
            LOGGER.log(Level.WARNING, "QCmdGetFileList: Timeout waiting for AxoE signal or transfer failed.");
            // Add robust error handling here: maybe retry, show an error message in UI, etc.
        }

        /* Ensure this call is always scheduled on the Event Dispatch Thread (EDT).
         * This 'Do()' method runs on a background thread (Transmitter). */
        SwingUtilities.invokeLater(() -> {
            /* Check if MainFrame and filemanager are not null before trying to use them */
            if ((MainFrame.mainframe != null) && (MainFrame.mainframe.getFilemanager() != null)) {
                MainFrame.mainframe.getFilemanager().refresh();
                if (done) {
                    LOGGER.log(Level.INFO, "UI refreshed: File list loaded successfully.");
                } else {
                    LOGGER.log(Level.WARNING, "UI refreshed: File list load incomplete (timeout).");
                }
            }
        });

        return this;
    }
}
