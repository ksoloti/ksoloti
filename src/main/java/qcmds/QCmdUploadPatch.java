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

import axoloti.Axoloti;
import axoloti.Connection;
import axoloti.MainFrame;
import axoloti.sd.SDCardInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
public class QCmdUploadPatch extends AbstractQCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdUploadPatch.class.getName());

    private CountDownLatch startMemWriteLatch;
    private CountDownLatch appendMemWriteLatch;
    private CountDownLatch closeMemWriteLatch;

    private volatile int startMemWriteStatus = 0xFF;
    private volatile int appendMemWriteStatus = 0xFF;
    private volatile int closeMemWriteStatus = 0xFF;


    File f;
    String filename;

    public QCmdUploadPatch(File f) {
        this.f = f;
        String pname = f.getAbsolutePath();
        int i = pname.lastIndexOf(File.separatorChar);
        if (i < 0) {
            this.filename = pname;
        }
        else {
            this.filename = pname.substring(i+1, pname.length());
        }
    }

    public QCmdUploadPatch() {
        this(null);
    }

    @Override
    public String GetStartMessage() {
        return "Uploading patch... " + filename;
    }

    @Override
    public String GetDoneMessage() {
        return "Done uploading patch.\n";
    }

    public void setStartMemWriteCompletedWithStatus(int statusCode) {
        this.startMemWriteStatus = statusCode;
        if (startMemWriteLatch != null) {
            startMemWriteLatch.countDown();
        }
    }

    public void setAppendMemWriteCompletedWithStatus(int statusCode) {
        this.appendMemWriteStatus = statusCode;
        if (appendMemWriteLatch != null) {
            appendMemWriteLatch.countDown();
        }
    }

    public void setCloseMemWriteCompletedWithStatus(int statusCode) {
        this.closeMemWriteStatus = statusCode;
        if (closeMemWriteLatch != null) {
            closeMemWriteLatch.countDown();
        }
    }

    @Override
    public QCmd Do(Connection connection) {
        LOGGER.info(GetStartMessage());
        connection.setCurrentExecutingCommand(this);

        this.startMemWriteStatus = 0xFF;
        this.appendMemWriteStatus = 0xFF;
        this.closeMemWriteStatus = 0xFF;

        try (FileInputStream inputStream = new FileInputStream(f)) {
            if (this.f == null) {
                String buildDir = System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + "build";
                f = new File(buildDir + File.separator + "xpatch.bin");
            }

            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Patch upload failed for " + filename + ": USB connection lost.");
                setCompletedWithStatus(1);
                return this;
            }

            int tlength = (int) f.length();
            int offset = connection.getTargetProfile().getPatchAddr();

            startMemWriteLatch = new CountDownLatch(1);
            connection.TransmitStartMemWrite(offset, tlength);

            if (!startMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Patch upload failed for " + filename + ": Core did not acknowledge memory write within timeout.");
                setCompletedWithStatus(1);
                return this;
            }
            if (startMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": Core reported error (" + startMemWriteStatus + ") during file creation.");
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
                    setCompletedWithStatus(1);
                    return this;
                }
                if (nRead != bytesToRead) {
                    LOGGER.log(Level.WARNING, "Partial read for " + filename + ": Expected " + bytesToRead + " bytes, read " + nRead + ". Chunk Number " + chunkNum);
                    byte[] actualBuffer = new byte[nRead];
                    System.arraycopy(buffer, 0, actualBuffer, 0, nRead);
                    buffer = actualBuffer;
                }

                if (!connection.isConnected()) {
                    LOGGER.log(Level.SEVERE, "Patch upload failed for " + filename + ": USB connection lost.");
                    setCompletedWithStatus(1);
                    return this;
                }

                appendMemWriteLatch = new CountDownLatch(1);
                connection.TransmitAppendMemWrite(buffer);

                if (!appendMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                    LOGGER.log(Level.SEVERE, "Patch upload failed for " + filename + ": Core did not acknowledge chunk receipt within timeout. Chunk number " + chunkNum);
                    setCompletedWithStatus(1);
                    return this;
                }
                if (appendMemWriteStatus != 0x00) {
                    LOGGER.log(Level.SEVERE, "Patch upload failed for " + filename + ": Core reported error (" + SDCardInfo.getFatFsErrorString(appendMemWriteStatus) + ") during chunk append. Chunk Number " + chunkNum);
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
                            LOGGER.log(Level.SEVERE, "Unexpected exception in progress update: {0}", ex.getMessage());
                        }
                    });
                }
                pct = newpct;
            } while (remLength > 0);

            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + filename + ": USB connection lost.");
                setCompletedWithStatus(1);
                return this;
            }

            closeMemWriteLatch = new CountDownLatch(1);
            connection.TransmitCloseMemWrite(offset, tlength); 

            if (!closeMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Patch upload failed for " + filename + ": Core did not acknowledge write close within timeout.");
                setCompletedWithStatus(1);
                return this;
            }
            if (closeMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Patch upload failed for " + filename + ": Core reported error (" + closeMemWriteStatus + ") during write close.");
                setCompletedWithStatus(1);
                return this;
            }
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File I/O error during upload for " + filename + ": {0}", ex.getMessage());
            setCompletedWithStatus(1);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Patch upload interrupted for " + filename + ": {0}", ex.getMessage());
            setCompletedWithStatus(1);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during patch upload for " + filename + ": {0}", ex.getMessage());
            setCompletedWithStatus(1);
        }
        finally {
            connection.setCurrentExecutingCommand(null);
        }
        return this;
    }
}
