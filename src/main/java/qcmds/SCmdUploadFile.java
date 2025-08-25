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
import axoloti.sd.SDCardInfo;
import axoloti.sd.SDFileInfo;

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
public class SCmdUploadFile extends AbstractSCmd {

    private static final Logger LOGGER = Logger.getLogger(SCmdUploadFile.class.getName());

    private CountDownLatch createFileLatch;
    private CountDownLatch appendFileLatch;
    private CountDownLatch closeFileLatch;

    private volatile int createFileStatus = 0xFF;
    private volatile int appendFileStatus = 0xFF;
    private volatile int closeFileStatus = 0xFF;

    InputStream inputStream;
    final String filename;
    final Calendar cal;
    File file;
    long size;

    public SCmdUploadFile(InputStream inputStream, String filename) {
        this.inputStream = inputStream;
        this.filename = filename;
        this.cal = null;
    }

    public SCmdUploadFile(File file, String filename) {
        this.file = file;
        this.filename = filename;
        this.inputStream = null;
        this.cal = null;
    }

    public SCmdUploadFile(File file, String filename, Calendar cal) {
        this.file = file;
        this.filename = filename;
        this.inputStream = null;
        this.cal = cal;
    }

    @Override
    public String GetStartMessage() {
        return null;
    }

    @Override
    public String GetDoneMessage() {
        return null;
    }

    /* These methods are called by USBBulkConnection.processByte() */
    public void setCreateFileCompletedWithStatus(int statusCode) {
        this.createFileStatus = statusCode;
        if (createFileLatch != null) {
            createFileLatch.countDown();
        }
    }

    public void setAppendFileCompletedWithStatus(int statusCode) {
        this.appendFileStatus = statusCode;
        if (appendFileLatch != null) {
            appendFileLatch.countDown();
        }
    }

    public void setCloseFileCompletedWithStatus(int statusCode) {
        this.closeFileStatus = statusCode;
        if (closeFileLatch != null) {
            closeFileLatch.countDown();
        }
    }

    @Override
    public SCmd Do(Connection connection) {
        connection.setCurrentExecutingCommand(this);

        this.createFileStatus = 0xFF;
        this.appendFileStatus = 0xFF;
        this.closeFileStatus = 0xFF;

        try {
            if (inputStream == null) {
                if (!file.exists()) {
                    LOGGER.log(Level.WARNING, "File does not exist: " + filename + "\n");
                    new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                    setCompletedWithStatus(1);
                    return this;
                }
                if (file.isDirectory()) {
                    LOGGER.log(Level.WARNING, "Cannot upload directories: " + filename + "\n");
                    new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                    setCompletedWithStatus(1);
                    return this;
                }
                if (!file.canRead()) {
                    LOGGER.log(Level.WARNING, "Cannot read file: " + filename + "\n");
                    new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                    setCompletedWithStatus(1);
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
            LOGGER.log(Level.INFO, "Uploading file to SD card... " + filename + ", size: " + SDFileInfo.getHumanReadableSize(tlength));
            size = tlength;

            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": USB connection lost.");
                new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                setCompletedWithStatus(1);
                return this;
            }

            createFileLatch = new CountDownLatch(1);
            connection.TransmitCreateFile(filename, tlength, ts);

            if (!createFileLatch.await(3, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": Core did not acknowledge file creation within timeout.");
                new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                setCompletedWithStatus(1);
                return this;
            }
            if (createFileStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": Core reported error (" + SDCardInfo.getFatFsErrorString(createFileStatus) + ") during file creation.");
                new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                setCompletedWithStatus(1);
                return this;
            }

            int MaxBlockSize = 32768;
            long totalBytesSent = 0;
            int remLength = tlength;
            long pct = 0;
            int chunkNum = 0;

            do {
                chunkNum++;
                int bytesToRead = Math.min(remLength, MaxBlockSize);
                if (bytesToRead <= 0) {
                    break;
                }

                byte[] buffer = new byte[bytesToRead];
                int nRead = inputStream.read(buffer, 0, bytesToRead);

                if (nRead == -1) {
                    LOGGER.log(Level.SEVERE, "Unexpected end of file or read error for " + filename + ". Read " + nRead + " bytes. Chunk number " + chunkNum);
                    new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                    setCompletedWithStatus(1);
                    return this;
                }
                if (nRead != bytesToRead) {
                    LOGGER.log(Level.WARNING, "Partial read for " + filename + ": Expected " + bytesToRead + " bytes, read " + nRead);
                    byte[] actualBuffer = new byte[nRead];
                    System.arraycopy(buffer, 0, actualBuffer, 0, nRead);
                    buffer = actualBuffer;
                }

                if (!connection.isConnected()) {
                    LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": USB connection lost.");
                    new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                    setCompletedWithStatus(1);
                    return this;
                }

                appendFileLatch = new CountDownLatch(1);
                connection.TransmitAppendFile(buffer);

                if (!appendFileLatch.await(3, TimeUnit.SECONDS)) {
                    LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": Core did not acknowledge chunk receipt within timeout. Chunk number " + chunkNum);
                    new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                    setCompletedWithStatus(1);
                    return this;
                }
                if (appendFileStatus != 0x00) {
                    LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": Core reported error (" + SDCardInfo.getFatFsErrorString(appendFileStatus) + ") during chunk append. Chunk number " + chunkNum);
                    new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                    setCompletedWithStatus(1);
                    return this;
                }

                totalBytesSent += nRead;
                remLength -= nRead;

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
                            content = content.replaceAll("\r\n", "\n");
                            int docLength = content.length();
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
                            LOGGER.log(Level.SEVERE, "Error during progress update: " + ex.getMessage());
                        }
                    });
                }
                pct = newpct;
            } while (remLength > 0);

            inputStream.close();

            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": USB connection lost.");
                new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                setCompletedWithStatus(1);
                return this;
            }

            closeFileLatch = new CountDownLatch(1);
            connection.TransmitCloseFile(filename, ts); 

            if (!closeFileLatch.await(3, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": Core did not acknowledge file close within timeout.");
                new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                setCompletedWithStatus(1);
                return this;
            }
            if (closeFileStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Failed to upload file " + filename + ": Core reported error (" + SDCardInfo.getFatFsErrorString(closeFileStatus) + ") during file close.");
                new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
                setCompletedWithStatus(1);
                return this;
            }

            /* Overall command success */
            SDCardInfo.getInstance().AddFile(filename, (int) size, ts);
            LOGGER.log(Level.INFO, "Done uploading file.\n");
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File I/O error during upload for " + filename + ": " + ex.getMessage());
            new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
            setCompletedWithStatus(1);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Upload interrupted for " + filename + ": " + ex.getMessage());
            new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
            setCompletedWithStatus(1);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during upload for " + filename + ": " + ex.getMessage());
            new SCmdDeleteFile(filename).Do(connection, true); /* Silently delete file stub */
            setCompletedWithStatus(1);
        }
        finally {
            connection.setCurrentExecutingCommand(null);
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error closing input stream for " + filename + ": " + e.getMessage());
                }
            }
        }
        return this;
    }
}
