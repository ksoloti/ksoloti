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
import axoloti.MainFrame;
import axoloti.SDCardInfo;
import axoloti.USBBulkConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.text.Document;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdUploadFile extends AbstractQCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdUploadFile.class.getName());

    // Internal latches for each distinct step's Acknowledge
    private CountDownLatch createFileLatch;
    private CountDownLatch appendFileLatch;
    private CountDownLatch closeFileLatch;

    // Internal status for each step, set by processByte()
    private volatile byte createFileStatus = (byte)0xFF;
    private volatile byte appendFileStatus = (byte)0xFF;
    private volatile byte closeFileStatus = (byte)0xFF;

    InputStream inputStream;
    final String filename;
    final Calendar cal;
    File file;
    long size;

    // Constructors as before
    public QCmdUploadFile(InputStream inputStream, String filename) {
        this.inputStream = inputStream;
        this.filename = filename;
        this.cal = null;
    }

    public QCmdUploadFile(File file, String filename) {
        this.file = file;
        this.filename = filename;
        this.inputStream = null;
        this.cal = null;
    }

    public QCmdUploadFile(File file, String filename, Calendar cal) {
        this.file = file;
        this.filename = filename;
        this.inputStream = null;
        this.cal = cal;
    }

    @Override
    public String GetStartMessage() {
        return "Uploading file to SD card: " + filename;
    }

    @Override
    public String GetDoneMessage() {
        return "File upload " + (isSuccessful() ? "successful" : "failed") + " for " + filename;
    }

    /* These methods are called by USBBulkConnection.processByte() */
    public void setCreateFileCompleted(byte statusCode) {
        this.createFileStatus = statusCode;
        if (createFileLatch != null) {
            createFileLatch.countDown();
        }
    }

    public void setAppendFileCompleted(byte statusCode) {
        this.appendFileStatus = statusCode;
        if (appendFileLatch != null) {
            appendFileLatch.countDown();
        }
    }

    public void setCloseFileCompleted(byte statusCode) {
        this.closeFileStatus = statusCode;
        if (closeFileLatch != null) {
            closeFileLatch.countDown();
        }
    }

    @Override
    public QCmd Do(Connection connection) {
        super.Do(connection);

        setCommandCompleted(false);
        setMcuStatusCode((byte)0xFF);

        this.createFileStatus = (byte)0xFF;
        this.appendFileStatus = (byte)0xFF;
        this.closeFileStatus = (byte)0xFF;

        try {
            if (inputStream == null) {
                if (!file.exists()) {
                    LOGGER.log(Level.WARNING, "File does not exist: " + filename + "\n");
                    setMcuStatusCode((byte)0x04); // FR_NO_FILE
                    return this;
                }
                if (file.isDirectory()) {
                    LOGGER.log(Level.WARNING, "Cannot upload directories: " + filename + "\n");
                    setMcuStatusCode((byte)0x05); // FR_NO_PATH (or custom error for directory)
                    return this;
                }
                if (!file.canRead()) {
                    LOGGER.log(Level.WARNING, "Cannot read file: " + filename + "\n");
                    setMcuStatusCode((byte)0x07); // FR_DENIED (permission)
                    return this;
                }
                inputStream = new FileInputStream(file);
            }

            Calendar ts;
            if (cal != null) {
                ts = cal;
            }
            else if (file != null) {
                ts = Calendar.getInstance();
                ts.setTimeInMillis(file.lastModified());
            }
            else {
                ts = Calendar.getInstance();
            }

            int tlength = inputStream.available(); 
            LOGGER.log(Level.INFO, "Uploading file to SD card: " + filename + ", Size: " + tlength + " bytes");
            size = tlength; // Store total size for SDCardInfo.AddFile and accurate progress

            // --- Step 1: Create the file on SD card (AxoCf) ---
            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": USB connection lost before file creation.");
                setMcuStatusCode((byte)0x03); // FR_NOT_READY (connection lost)
                return this;
            }

            createFileLatch = new CountDownLatch(1); // New latch for this step
            USBBulkConnection.GetConnection().TransmitCreateFile(filename, tlength, ts);

            if (!createFileLatch.await(3, TimeUnit.SECONDS)) { // Wait for create file ACK
                LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": Core did not acknowledge file creation within timeout.");
                setMcuStatusCode((byte)0x0F); // FR_TIMEOUT
                return this;
            }
            if (createFileStatus != 0x00) { // Check status from MCU (0x00 is FR_OK)
                LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": Core reported error (" + SDCardInfo.getFatFsErrorString(createFileStatus) + ") during file creation.");
                setMcuStatusCode(createFileStatus);
                return this;
            }

            // --- Step 2: Append data in chunks (Axoa) ---
            int MaxBlockSize = 32768;
            long totalBytesSent = 0;
            int remLength = tlength;
            long pct = 0;

            do {
                int bytesToRead = Math.min(remLength, MaxBlockSize);
                if (bytesToRead <= 0) { // No more bytes to read
                    break;
                }

                byte[] buffer = new byte[bytesToRead];
                int nRead = inputStream.read(buffer, 0, bytesToRead);

                if (nRead == -1) { // Unexpected end of stream
                    LOGGER.log(Level.SEVERE, "Unexpected end of file or read error for " + filename + ". Read " + nRead + " bytes.");
                    setMcuStatusCode((byte)0x01); // FR_DISK_ERR or custom I/O error
                    return this;
                }
                if (nRead != bytesToRead) {
                    LOGGER.log(Level.WARNING, "Partial read for " + filename + ": Expected " + bytesToRead + " bytes, read " + nRead + ".");
                    byte[] actualBuffer = new byte[nRead];
                    System.arraycopy(buffer, 0, actualBuffer, 0, nRead);
                    buffer = actualBuffer; // Use the actually read bytes
                }

                if (!connection.isConnected()) {
                    LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": USB connection lost during file transfer.");
                    setMcuStatusCode((byte)0x03); // FR_NOT_READY
                    return this;
                }

                appendFileLatch = new CountDownLatch(1); // New latch for this chunk
                USBBulkConnection.GetConnection().TransmitAppendFile(buffer);

                if (!appendFileLatch.await(3, TimeUnit.SECONDS)) { // Wait for append chunk ACK
                    LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": Core did not acknowledge chunk receipt within timeout.");
                    setMcuStatusCode((byte)0x0F); // FR_TIMEOUT
                    return this;
                }
                if (appendFileStatus != 0x00) { // Check status from MCU
                    LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": Core reported error (" + SDCardInfo.getFatFsErrorString(appendFileStatus) + ") during chunk append.");
                    setMcuStatusCode(appendFileStatus);
                    return this;
                }
                // try {Thread.sleep(500);} catch (Exception e) {};

                totalBytesSent += nRead;
                remLength -= nRead;

                // Progress bar update
                long newpct = (100 * totalBytesSent) / tlength;
                if (newpct != pct) {
                    StringBuilder progressbar = new StringBuilder("                         "); /* 25-chars long progress bar */
                    for (int i = 0; i < (int) newpct/4; i++) {
                        progressbar.setCharAt(i, '='); /* fill the progress bar depending on the percentage */
                    }

                    final String progressMessage = "Uploading\t[" + progressbar + "] " + String.format("%3d", newpct) + "%";
                    SwingUtilities.invokeLater(() -> {

                        try {
                            Document doc = MainFrame.jTextPaneLog.getDocument();
                            String content = doc.getText(0, doc.getLength());
                            content = content.replaceAll("\r\n", "\n"); // Normalize line endings

                            int docLength = content.length(); // Use content.length() after normalization

                            int startOfLineToRemove = -1;
                            String lastLineContent = "";

                            if (docLength > 0) {
                                /* Find the index of the *last character* of the content */
                                int endOfContent = docLength;
                                while (endOfContent > 0 && content.charAt(endOfContent - 1) == '\n') {
                                    endOfContent--;
                                }

                                if (endOfContent > 0) {
                                    /* Find the newline before this actual last line of content */
                                    int lastNewlineBeforeContent = content.lastIndexOf('\n', endOfContent - 1);

                                    /* Calculate the start index of the line to remove */
                                    startOfLineToRemove = (lastNewlineBeforeContent == -1) ? 0 : (lastNewlineBeforeContent + 1);

                                    /* Extract the actual content of the last line */
                                    lastLineContent = content.substring(startOfLineToRemove, endOfContent);
                                }
                            }

                            /* Check if the last line matches the progress bar pattern */
                            if (lastLineContent.trim().startsWith("Uploading\t[")) {
                                /* If it matches, remove this last line and all trailing newlines */
                                doc.remove(startOfLineToRemove, doc.getLength() - startOfLineToRemove);
                            } else {
                                /* If the previous line was NOT a progress bar, append back a newline before inserting the new one */
                                if (docLength > 0 && content.charAt(docLength - 1) != '\n') {
                                    doc.insertString(doc.getLength(), "\n", null);
                                }
                            }

                            doc.insertString(doc.getLength(), progressMessage + "\n", MainFrame.styleInfo);
                            MainFrame.jTextPaneLog.setCaretPosition(doc.getLength());
                        }
                        catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Unexpected exception in progress update: {0}", ex.getMessage());
                        }
                    });
                }
                pct = newpct;
            } while (remLength > 0);

            inputStream.close();

            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": USB connection lost before file close.");
                setMcuStatusCode((byte)0x03); // FR_NOT_READY
                return this;
            }

            closeFileLatch = new CountDownLatch(1); // New latch for this step
            USBBulkConnection.GetConnection().TransmitCloseFile(filename, ts); 

            if (!closeFileLatch.await(3, TimeUnit.SECONDS)) { // Wait for close file ACK
                LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": Core did not acknowledge file close within timeout.");
                setMcuStatusCode((byte)0x0F); // FR_TIMEOUT
                return this;
            }
            if (closeFileStatus != 0x00) { // Check status from MCU
                LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": Core reported error (" + SDCardInfo.getFatFsErrorString(closeFileStatus) + ") during file close.");
                setMcuStatusCode(closeFileStatus);
                return this;
            }

            /* Overall command success */
            SDCardInfo.getInstance().AddFile(filename, (int) size, ts);
            LOGGER.log(Level.INFO, "Done uploading file.\n");
            setMcuStatusCode((byte)0x00); // FR_OK for overall command
            setCommandCompleted(true); // Signal overall success

        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File I/O error during upload for " + filename + ": {0}", ex.getMessage());
            setMcuStatusCode((byte)0x01); // FR_DISK_ERR or custom I/O error
            setCommandCompleted(false);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Upload interrupted for " + filename + ": {0}", ex.getMessage());
            setMcuStatusCode((byte)0x02); // FR_INT_ERR or custom interrupted error
            setCommandCompleted(false);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during upload for " + filename + ": {0}", ex.getMessage());
            setMcuStatusCode((byte)0xFF); // Generic error
            setCommandCompleted(false);
        }
        finally {
            connection.setCurrentExecutingCommand(null);
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing input stream for " + filename + ": {0}", e.getMessage());
                }
            }
        }
        return this;
    }
}
