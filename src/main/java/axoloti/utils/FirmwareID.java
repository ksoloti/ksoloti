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
package axoloti.utils;

import axoloti.Axoloti;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 *
 * @author Johannes Taelman
 */
public class FirmwareID {

    private static final Logger LOGGER = Logger.getLogger(FirmwareID.class.getName());

    static public String getFirmwareID() {
        try {
            String boarddef = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "build";

            if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
                boarddef += File.separator + "ksoloti";
            }
            else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
                boarddef += File.separator + "axoloti";
            }
            if (Preferences.getInstance().getFirmwareMode().contains("SPILink")) {
                boarddef += "_spilink";
            }
            if (Preferences.getInstance().getFirmwareMode().contains("USBAudio")) {
                boarddef += "_usbaudio";
            }
            if (Preferences.getInstance().getFirmwareMode().contains("I2SCodec")) {
                boarddef += "_i2scodec";
            }
            boarddef += ".bin";
            File f = new File(boarddef);

            if (f == null || !f.canRead()) {
                LOGGER.log(Level.WARNING, "Could not find " + boarddef + ".\nPlease compile the firmware first.");
                return "(" + boarddef + " not found)";
            }
            int tlength = (int) f.length();
            FileInputStream inputStream = new FileInputStream(f);
            byte[] bb = new byte[tlength];
            int nRead = inputStream.read(bb, 0, tlength);
            inputStream.close();
            if (nRead != tlength) {
                LOGGER.log(Level.SEVERE, "File size wrong?" + nRead);
            }
            CRC32 zcrc = new CRC32();
            zcrc.update(bb);
            int zcrcv = (int) zcrc.getValue();
            return String.format("%08X", zcrcv);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error trying to get firmware ID: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
        return "";
    }
}
