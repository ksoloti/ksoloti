/**
 * Copyright (C) 2015 Johannes Taelman
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
import axoloti.utils.Preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;
import javax.swing.text.Document;

/**
 *
 * @author Johannes Taelman
 */
public class SCmdUploadFWSDRam extends AbstractSCmd {

    private static final Logger LOGGER = Logger.getLogger(SCmdUploadFWSDRam.class.getName());

    private CountDownLatch startMemWriteLatch;
    private CountDownLatch appendMemWriteLatch;
    private CountDownLatch closeMemWriteLatch;

    private volatile int startMemWriteStatus = 0xFF;
    private volatile int appendMemWriteStatus = 0xFF;
    private volatile int closeMemWriteStatus = 0xFF;

    File f;
    long originalFirmwareSize;
    long totalBytesToTransfer;

    public SCmdUploadFWSDRam(File f) {
        this.f = f;
    }

    public SCmdUploadFWSDRam() {
        this(null);
    }

    @Override
    public String GetStartMessage() {
        return "Sending firmware packets...";
    }

    @Override
    public String GetDoneMessage() {
        return "Done sending firmware packets.\n";
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
    public SCmd Do(Connection connection) {
        LOGGER.info(GetStartMessage());
        connection.setCurrentExecutingCommand(this);

        this.startMemWriteStatus = 0xFF;
        this.appendMemWriteStatus = 0xFF;
        this.closeMemWriteStatus = 0xFF;

        FileInputStream inputStream = null;

        try {
            if (this.f == null) {
                inputStream = new FileInputStream(f);
                String buildDir = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "build";
                if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
                    buildDir += File.separator + "ksoloti";
                }
                else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
                    buildDir += File.separator + "axoloti";
                }

                if (Preferences.getInstance().getFirmwareMode().contains("SPILink")) {
                    buildDir += "_spilink";
                }
                if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
                    buildDir += "_usbaudio";
                }
                if (Preferences.getInstance().getFirmwareMode().contains("I2SCodec")) {
                    buildDir += "_i2scodec";
                }
                buildDir += ".bin";
                f = new File(buildDir);
            }

            if (!f.exists()) {
                LOGGER.log(Level.SEVERE, "Firmware file does not exist: " + f.getAbsolutePath());
                setCompletedWithStatus(1);
                return this;
            }
            if (!f.canRead()) {
                LOGGER.log(Level.SEVERE, "Cannot read firmware file: " + f.getAbsolutePath());
                setCompletedWithStatus(1);
                return this;
            }

            LOGGER.log(Level.INFO, "Firmware file path: " + f.getAbsolutePath());
            originalFirmwareSize = f.length(); /* Store original file size */
            inputStream = new FileInputStream(f);

            /* Read entire file into memory for CRC calculation and initial header combination */
            byte[] fullFirmwareData = new byte[(int) originalFirmwareSize];
            int nReadFull = inputStream.read(fullFirmwareData, 0, (int) originalFirmwareSize);
            if (nReadFull != originalFirmwareSize) {
                LOGGER.log(Level.SEVERE, "Failed to read firmware file for CRC calculation. Expected {0} bytes, read {1}.", new Object[]{originalFirmwareSize, nReadFull});
                setCompletedWithStatus(1);
                return this;
            }
            inputStream.close(); /* Close after full read */

            /* Calculate CRC32 */
            CRC32 zcrc = new CRC32();
            zcrc.update(fullFirmwareData);
            int zcrcv = (int) zcrc.getValue();
            LOGGER.log(Level.INFO, "Firmware CRC: " + Integer.toHexString(zcrcv).toUpperCase());

            /* --- Create the 'flascopy' header with length and CRC --- */
            byte[] flascopyHeader = new byte[16];
            flascopyHeader[0] = 'f';
            flascopyHeader[1] = 'l';
            flascopyHeader[2] = 'a';
            flascopyHeader[3] = 's';
            flascopyHeader[4] = 'c';
            flascopyHeader[5] = 'o';
            flascopyHeader[6] = 'p';
            flascopyHeader[7] = 'y';
            flascopyHeader[8] = (byte) (originalFirmwareSize);
            flascopyHeader[9] = (byte) (originalFirmwareSize >> 8);
            flascopyHeader[10] = (byte) (originalFirmwareSize >> 16);
            flascopyHeader[11] = (byte) (originalFirmwareSize >> 24);
            flascopyHeader[12] = (byte) (zcrcv);
            flascopyHeader[13] = (byte) (zcrcv >> 8);
            flascopyHeader[14] = (byte) (zcrcv >> 16);
            flascopyHeader[15] = (byte) (zcrcv >> 24);

            totalBytesToTransfer = originalFirmwareSize + flascopyHeader.length;
            LOGGER.log(Level.INFO, "Total bytes to transfer (including header): " + totalBytesToTransfer);

            /* --- Step 1: Start Memory Write (AxoWW) --- */
            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: USB connection lost.");
                setCompletedWithStatus(1);
                return this;
            }

            int sdramAddr = connection.getTargetProfile().getSDRAMAddr();
            startMemWriteLatch = new CountDownLatch(1);
            connection.TransmitStartMemWrite(sdramAddr, (int) totalBytesToTransfer);

            if (!startMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: Core did not acknowledge start memory write within timeout.");
                setCompletedWithStatus(1);
                return this;
            }
            if (startMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: Core reported error (" + SDCardInfo.getFatFsErrorString(startMemWriteStatus) + ") during start memory write.");
                setCompletedWithStatus(1);
                return this;
            }

            /* --- Step 2: Append data in chunks (Axow) --- */
            int MaxBlockSize = 32768;
            long currentBytesSent = 0;
            int remLength = (int) totalBytesToTransfer;
            long pct = 0;
            int chunkNum = 1;

            /* Use a new FileInputStream to re-read the file for chunking */
            inputStream = new FileInputStream(f);

            /* Handle the first chunk which includes the 'flascopy' header */
            int bytesForFirstChunk = Math.min(remLength, MaxBlockSize);
            byte[] firstChunkBuffer = new byte[bytesForFirstChunk];

            /* Copy flascopy header into the beginning of the first chunk */
            System.arraycopy(flascopyHeader, 0, firstChunkBuffer, 0, flascopyHeader.length);

            /* Copy actual firmware data into the rest of the first chunk */
            int firmwareBytesInFirstChunk = bytesForFirstChunk - flascopyHeader.length;
            if (firmwareBytesInFirstChunk > 0) {
                int nReadFirmware = inputStream.read(firstChunkBuffer, flascopyHeader.length, firmwareBytesInFirstChunk);
                if (nReadFirmware != firmwareBytesInFirstChunk) {
                    LOGGER.log(Level.SEVERE, "Partial read for first firmware chunk: Expected " + firmwareBytesInFirstChunk + " bytes, read " + nReadFirmware + " bytes");
                    /* Adjust the buffer size if less was read than expected */
                    byte[] actualFirstChunk = new byte[flascopyHeader.length + nReadFirmware];
                    System.arraycopy(firstChunkBuffer, 0, actualFirstChunk, 0, flascopyHeader.length + nReadFirmware);
                    firstChunkBuffer = actualFirstChunk;
                }
            }

            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: USB connection lost.");
                setCompletedWithStatus(1);
                return this;
            }

            appendMemWriteLatch = new CountDownLatch(1);
            connection.TransmitAppendMemWrite(firstChunkBuffer);

            if (!appendMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: Core did not acknowledge first chunk receipt within timeout.");
                setCompletedWithStatus(1);
                return this;
            }
            if (appendMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: Core reported error Core reported error (" + SDCardInfo.getFatFsErrorString(startMemWriteStatus) + ") during first chunk append.");
                setCompletedWithStatus(1);
                return this;
            }

            currentBytesSent += firstChunkBuffer.length;
            remLength -= firstChunkBuffer.length;

            /* Continue with remaining chunks (only firmware data) */
            while (remLength > 0) {
                chunkNum++;
                int bytesToRead = Math.min(remLength, MaxBlockSize);
                if (bytesToRead <= 0) { /* No more bytes to read */
                    break;
                }

                byte[] buffer = new byte[bytesToRead];
                int nReadChunk = inputStream.read(buffer, 0, bytesToRead);

                if (nReadChunk == -1) { /* Unexpected end of stream */
                    LOGGER.log(Level.SEVERE, "Unexpected end of firmware file or read error during chunking. Read "+ nReadChunk +" bytes. Chunk number " + chunkNum);
                    setCompletedWithStatus(1);
                    return this;
                }
                if (nReadChunk != bytesToRead) {
                    LOGGER.log(Level.WARNING, "Partial read for firmware chunk: Expected " + bytesToRead + " bytes, read " + nReadChunk + ". Chunk number " + chunkNum);
                    byte[] actualBuffer = new byte[nReadChunk];
                    System.arraycopy(buffer, 0, actualBuffer, 0, nReadChunk);
                    buffer = actualBuffer;
                }

                if (!connection.isConnected()) {
                    LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: USB connection lost.");
                    setCompletedWithStatus(1);
                    return this;
                }

                appendMemWriteLatch = new CountDownLatch(1);
                connection.TransmitAppendMemWrite(buffer);

                if (!appendMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                    LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: Core did not acknowledge chunk receipt within timeout. Chunk number " + chunkNum);
                    setCompletedWithStatus(1);
                    return this;
                }
                if (appendMemWriteStatus != 0x00) {
                    LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: Core reported error (" + SDCardInfo.getFatFsErrorString(appendMemWriteStatus) + ") during chunk append. Chunk number " + chunkNum);
                    setCompletedWithStatus(1);
                    return this;
                }

                currentBytesSent += nReadChunk;
                remLength -= nReadChunk;

                /* Progress bar update */
                long newpct = (100 * currentBytesSent) / totalBytesToTransfer;
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
                            content = content.replaceAll("\r\n", "\n"); /* Normalize line endings */

                            int docLength = content.length(); /* Use content.length() after normalization */

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
                            }
                            else {
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
            }

            /* --- Step 3: Close Memory Write (AxoWc) --- */
            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: USB connection lost.");
                setCompletedWithStatus(1);
                return this;
            }

            closeMemWriteLatch = new CountDownLatch(1);
            connection.TransmitCloseMemWrite(sdramAddr, (int) totalBytesToTransfer);

            if (!closeMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: Core did not acknowledge memory write close within timeout.");
                setCompletedWithStatus(1);
                return this;
            }
            if (closeMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM failed: Core reported error (" + SDCardInfo.getFatFsErrorString(closeMemWriteStatus) +") during memory write close.");
                setCompletedWithStatus(1);
                return this;
            }
        }
        catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Firmware file not found: " + ex.getMessage());
            setCompletedWithStatus(1);
            return this;
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File I/O error during firmware upload to SDRAM: " + ex.getMessage());
            setCompletedWithStatus(1);
            return this;
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Firmware upload to SDRAM interrupted: " + ex.getMessage());
            setCompletedWithStatus(1);
            return this;
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during firmware upload to SDRAM: " + ex.getMessage());
            setCompletedWithStatus(1);
            return this;
        }
        finally {
            connection.setCurrentExecutingCommand(null);
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing input stream for firmware upload to SDRAM: " + e.getMessage());
                }
            }
        }
        LOGGER.info(GetDoneMessage());
        return this;
    }
}
