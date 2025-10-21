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
import axoloti.utils.OSDetect;
import axoloti.utils.Preferences;

import static axoloti.utils.FileUtils.toUnixPath;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdFlashDFU extends QCmdShellTask {

    private static final Logger LOGGER = Logger.getLogger(QCmdFlashDFU.class.getName());

    @Override
    public String GetStartMessage() {
        return "Flashing firmware with DFU...";
    }

    @Override
    public String GetDoneMessage() {
        if (success) {
            return "Done flashing firmware with DFU.\n";
        } else {
            return "Error: Flashing firmware failed.\n";
        }
    }
    
    @Override
    public File GetWorkingDir() {
        String fwdir = System.getProperty(axoloti.Axoloti.FIRMWARE_DIR);
        return new File(fwdir);
    }
    
    
    @Override
    String[] GetExec() {
        String bname = "";
        if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
            bname = "ksoloti";
        }
        else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
            bname = "axoloti";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("SPILink")) {
            bname += "_spilink";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
            bname += "_usbaudio";
        }
        if (Preferences.getInstance().getFirmwareMode().contains("I2SCodec")) {
            bname += "_i2scodec";
        }
        bname += ".bin";
        LOGGER.log(Level.INFO, "File path: " + System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "build" + File.separator + bname);

        if (OSDetect.getOS() == OSDetect.OS.WIN) {
            String str = toUnixPath(FirmwareDir() + File.separator + "upload_fw_dfu_win.bat " + bname);
            return str.split("\\s+");
        }
        else if (OSDetect.getOS() == OSDetect.OS.MAC || OSDetect.getOS() == OSDetect.OS.LINUX) {
            String str = toUnixPath(FirmwareDir() + File.separator + "upload_fw_dfu.sh " + bname);
            return str.split("\\s+");
        }
        else {
            LOGGER.log(Level.SEVERE, "UPLOAD: OS UNKNOWN!");
            return null;
        }
    }

    @Override
    QCmd err() {
        return null;
    }
}
