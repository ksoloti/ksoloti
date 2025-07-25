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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.usb4java.LibUsb;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdUploadPatch extends AbstractQCmdSerialTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdUploadPatch.class.getName());

    File f;

    public QCmdUploadPatch() {
        this.f = null;
    }

    public QCmdUploadPatch(File f) {
        this.f = f;
    }

    @Override
    public String GetStartMessage() {
        return "Uploading patch...";
    }

    @Override
    public String GetDoneMessage() {
        return "Done uploading patch.\n";
    }

    @Override
    public QCmd Do(Connection connection) {
        connection.ClearSync();
        try {
            if (this.f == null) {
                String buildDir=System.getProperty(Axoloti.LIBRARIES_DIR) + File.separator + "build";
                f = new File(buildDir + File.separator + "xpatch.bin");
            }
            LOGGER.log(Level.INFO, "{0}", f.getAbsolutePath());
            int tlength = (int) f.length();
            FileInputStream inputStream = new FileInputStream(f);
            int offset = 0;
            int MaxBlockSize = 32768;
            do {
                int l;
                if (tlength > MaxBlockSize) {
                    l = MaxBlockSize;
                    tlength -= MaxBlockSize;
                } else {
                    l = tlength;
                    tlength = 0;
                }
                byte[] buffer = new byte[l];
                int nRead = inputStream.read(buffer, 0, l);
                if (nRead != l) {
                    LOGGER.log(Level.SEVERE, "File size wrong? {0}", nRead);
                }
                int result = connection.TransmitUploadFragment(buffer, connection.getTargetProfile().getPatchAddr() + offset);
                if (result != LibUsb.SUCCESS) {
                    break;
                }
                offset += nRead;
            } while (tlength > 0);
            inputStream.close();
            return this;
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "FileNotFoundException", ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "IOException", ex);
        }
        return new QCmdDisconnect();
    }

}
