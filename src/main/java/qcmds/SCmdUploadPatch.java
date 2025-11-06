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

    private CountDownLatch startMemWriteLatch;
    private CountDownLatch appendMemWriteLatch;
    private CountDownLatch closeMemWriteLatch;

    private volatile int startMemWriteStatus = 0xFF;
    private volatile int appendMemWriteStatus = 0xFF;
    private volatile int closeMemWriteStatus = 0xFF;


    File f;
    String filename;

    public SCmdUploadPatch(File f) {
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

    public SCmdUploadPatch() {
        this(null);
    }

    @Override
    public String GetStartMessage() {
        return "Uploading patch... '" + filename + "'";
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
    public SCmd Do(Connection connection) {
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
                LOGGER.log(Level.SEVERE, "Patch upload failed for '" + filename + "': USB connection lost.");
                setCompletedWithStatus(1);
                return this;
            }

            int tlength = (int) f.length();
            int offset = connection.getTargetProfile().getPatchAddr();

            startMemWriteLatch = new CountDownLatch(1);
            connection.TransmitStartMemWrite(offset, tlength);

            if (!startMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Patch upload failed for '" + filename + "': Core did not acknowledge memory write within timeout.");
                setCompletedWithStatus(1);
                return this;
            }
            if (startMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Upload failed for '" + filename + "': Core reported error (" + startMemWriteStatus + ") during file creation.");
                setCompletedWithStatus(1);
                return this;
            }

            int MaxBlockSize = 32768;
            int chunkNum = 0;
            int remLength = tlength;

            do {
                chunkNum++;
                int bytesToRead = Math.min(remLength, MaxBlockSize);
                if (bytesToRead <= 0) {
                    break;
                }

                byte[] buffer = new byte[bytesToRead];
                int nRead = inputStream.read(buffer, 0, bytesToRead);

                if (nRead == -1) {
                    LOGGER.log(Level.SEVERE, "Unexpected end of file or read error for '" + filename + "'. Read " + nRead + " bytes. Chunk number " + chunkNum);
                    setCompletedWithStatus(1);
                    return this;
                }
                if (nRead != bytesToRead) {
                    LOGGER.log(Level.WARNING, "Partial read for '" + filename + "': Expected " + bytesToRead + " bytes, read " + nRead + ". Chunk Number " + chunkNum);
                    byte[] actualBuffer = new byte[nRead];
                    System.arraycopy(buffer, 0, actualBuffer, 0, nRead);
                    buffer = actualBuffer;
                }

                if (!connection.isConnected()) {
                    LOGGER.log(Level.SEVERE, "Patch upload failed for '" + filename + "': USB connection lost.");
                    setCompletedWithStatus(1);
                    return this;
                }

                appendMemWriteLatch = new CountDownLatch(1);
                connection.TransmitAppendMemWrite(buffer);

                if (!appendMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                    LOGGER.log(Level.SEVERE, "Patch upload failed for '" + filename + "': Core did not acknowledge chunk receipt within timeout. Chunk number " + chunkNum);
                    setCompletedWithStatus(1);
                    return this;
                }
                if (appendMemWriteStatus != 0x00) {
                    LOGGER.log(Level.SEVERE, "Patch upload failed for '" + filename + "': Core reported error (" + SDCardInfo.getFatFsErrorString(appendMemWriteStatus) + ") during chunk append. Chunk Number " + chunkNum);
                    setCompletedWithStatus(1);
                    return this;
                }

                remLength -= nRead;

            } while (remLength > 0);

            if (!connection.isConnected()) {
                LOGGER.log(Level.SEVERE, "Upload failed for '" + filename + "': USB connection lost.");
                setCompletedWithStatus(1);
                return this;
            }

            closeMemWriteLatch = new CountDownLatch(1);
            connection.TransmitCloseMemWrite(offset, tlength); 

            if (!closeMemWriteLatch.await(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.SEVERE, "Patch upload failed for '" + filename + "': Core did not acknowledge write close within timeout.");
                setCompletedWithStatus(1);
                return this;
            }
            if (closeMemWriteStatus != 0x00) {
                LOGGER.log(Level.SEVERE, "Patch upload failed for '" + filename + "': Core reported error (" + closeMemWriteStatus + ") during write close.");
                setCompletedWithStatus(1);
                return this;
            }
        }
        catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File I/O error during upload for '" + filename + "': " + ex.getMessage());
            ex.printStackTrace(System.out);
            setCompletedWithStatus(1);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Patch upload interrupted: " + ex.getMessage());
            ex.printStackTrace(System.out);
            setCompletedWithStatus(1);
            return this;
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during upload: " + ex.getMessage());
            ex.printStackTrace(System.out);
            setCompletedWithStatus(1);
            return this;
        }
        finally {
            connection.setCurrentExecutingCommand(null);
        }
        LOGGER.info(GetDoneMessage());
        return this;
    }
}
