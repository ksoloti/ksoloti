/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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
import axoloti.sd.SDCardInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Johannes Taelman
 */
public class SCmdUploadPatch extends AbstractSCmd {

    private static final Logger LOGGER = Logger.getLogger(SCmdUploadPatch.class.getName());
    private static final int BLOCK_SIZE = 32768; 

    private CountDownLatch startMemWriteLatch;
    private CountDownLatch appendMemWriteLatch;
    private CountDownLatch closeMemWriteLatch;

    private volatile int startMemWriteStatus = 0xFF;
    private volatile int appendMemWriteStatus = 0xFF;
    private volatile int closeMemWriteStatus = 0xFF;

    File fileToUpload;
    int targetAddress;
    String name;

    public SCmdUploadPatch(File file, int targetAddress, String name) {
        this.fileToUpload = file;
        this.targetAddress = targetAddress;
        this.name = name;
    }

    public SCmdUploadPatch() {
        this(null);
    }

    public SCmdUploadPatch(File f) {
        this.fileToUpload = f;
        this.targetAddress = -1;
        this.name = "patch file";
    }

    private void resetLatches() {
        this.startMemWriteStatus = 0xFF;
        this.appendMemWriteStatus = 0xFF;
        this.closeMemWriteStatus = 0xFF;
        this.startMemWriteLatch = null;
        this.appendMemWriteLatch = null;
        this.closeMemWriteLatch = null;
    }


    @Override
    public String GetStartMessage() {
        return "Starting upload: " + this.name + "...";
    }

    @Override
    public String GetDoneMessage() {
        return "Done uploading " + this.name + ".\n";
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

    private boolean doUpload(Connection connection) throws InterruptedException, IOException {

        if (this.fileToUpload == null) {
            LOGGER.log(Level.SEVERE, "Upload failed: No file specified.");
            return false;
        }

        if (!this.fileToUpload.exists()) {
            LOGGER.log(Level.WARNING, "Patch file not found: '" + this.fileToUpload.getName() + "'. Skipping upload.");
            return true; 
        }

        LOGGER.info("Uploading " + this.name + " (" + this.fileToUpload.length() + " bytes)...");

        try (FileInputStream inputStream = new FileInputStream(this.fileToUpload)) {
            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": USB connection lost.");
                setCompletedWithStatus(1);
                return false;
            }

            int tlength = (int) this.fileToUpload.length();

            resetLatches();

            connection.setCurrentExecutingCommand(this);
            startMemWriteLatch = new CountDownLatch(1);
            int writeResult = connection.TransmitStartMemWrite(this.targetAddress, tlength);
            if (writeResult != org.usb4java.LibUsb.SUCCESS) {
                LOGGER.log(Level.SEVERE, "Failed to send patch upload command (start): USB write error.");
                setCompletedWithStatus(1);
                return false;
            }
            
            if (!startMemWriteLatch.await(3, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": Core did not acknowledge memory write (Start) within timeout.");
                setCompletedWithStatus(1);
                return false;
            }
            if (startMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": Core reported error (" + startMemWriteStatus + ") during file creation.");
                setCompletedWithStatus(1);
                return false;
            }

            int chunkNum = 0;
            int remLength = tlength;

            do {
                chunkNum++;
                int bytesToRead = Math.min(remLength, BLOCK_SIZE);
                if (bytesToRead <= 0) {
                    break;
                }

                byte[] buffer = new byte[bytesToRead];
                int nRead = inputStream.read(buffer, 0, bytesToRead);

                if (nRead == -1) {
                    LOGGER.log(Level.SEVERE, "Unexpected end of file or read error for " + this.name + ". Read " + nRead + " bytes. Chunk number " + chunkNum);
                    return false;
                }
                if (nRead != bytesToRead) {
                    LOGGER.log(Level.WARNING, "Partial read for " + this.name + ": Expected " + bytesToRead + " bytes, read " + nRead + ". Chunk Number " + chunkNum);
                    byte[] actualBuffer = new byte[nRead];
                    System.arraycopy(buffer, 0, actualBuffer, 0, nRead);
                    buffer = actualBuffer;
                }

                if (!connection.isConnected()) {
                    LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": USB connection lost.");
                    setCompletedWithStatus(1);
                    return false;
                }

                appendMemWriteLatch = new CountDownLatch(1);
                writeResult = connection.TransmitAppendMemWrite(buffer);
                if (writeResult != org.usb4java.LibUsb.SUCCESS) {
                    LOGGER.log(Level.SEVERE, "Failed to send patch upload command (append): USB write error.");
                    setCompletedWithStatus(1);
                    return false;
                }

                if (!appendMemWriteLatch.await(3, TimeUnit.SECONDS)) {
                    LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": Core did not acknowledge chunk receipt within timeout. Chunk number " + chunkNum);
                    setCompletedWithStatus(1);
                    return false;
                }
                if (appendMemWriteStatus != 0x00) {
                    LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": Core reported error (" + SDCardInfo.getFatFsErrorString(appendMemWriteStatus) + ") during chunk append. Chunk Number " + chunkNum);
                    setCompletedWithStatus(1);
                    return false;
                }

                remLength -= nRead;

            } while (remLength > 0);

            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": USB connection lost.");
                setCompletedWithStatus(1);
                return false;
            }

            closeMemWriteLatch = new CountDownLatch(1);
            writeResult = connection.TransmitCloseMemWrite(this.targetAddress, tlength); 
            if (writeResult != org.usb4java.LibUsb.SUCCESS) {
                LOGGER.log(Level.SEVERE, "Failed to send "+ this.name + " upload command (close): USB write error.");
                setCompletedWithStatus(1);
                return false;
            }

            if (!closeMemWriteLatch.await(3, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": Core did not acknowledge write close within timeout.");
                setCompletedWithStatus(1);
                return false;
            }
            if (closeMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Upload failed for " + this.name + ": Core reported error (" + closeMemWriteStatus + ") during write close (Close).");
                setCompletedWithStatus(1);
                return false;
            }
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File I/O error during upload for " + this.name + ": " + ex.getMessage());
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    @Override
    public SCmd Do(Connection connection) {
        if (targetAddress == -1) {
            String buildDir = System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + "build";
            if (this.fileToUpload == null) {
                this.fileToUpload = new File(buildDir + File.separator + "xpatch.bin");
            }
            this.targetAddress = connection.getTargetProfile().getPatchAddr();
            this.name = "patch"; 
        }

        try {
            if (!doUpload(connection)) {
                setCompletedWithStatus(1);
            }
            else {
                LOGGER.info(GetDoneMessage());
            }
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Patch upload interrupted: " + ex.getMessage());
            ex.printStackTrace(System.out);
            setCompletedWithStatus(1);
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during upload: " + ex.getMessage());
            ex.printStackTrace(System.out);
            setCompletedWithStatus(1);
        }
        finally {
            connection.clearIfCurrentExecutingCommand(this);
        }
        return this;
    }
}
