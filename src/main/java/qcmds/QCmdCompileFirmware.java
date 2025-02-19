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

import axoloti.MainFrame;
import axoloti.utils.OSDetect;

import static axoloti.MainFrame.prefs;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdCompileFirmware extends QCmdShellTask {

    @Override
    public String GetStartMessage() {
        return "Running compilation script...";
    }

    @Override
    public String GetDoneMessage() {
        MainFrame.mainframe.updateLinkFirmwareID();
        MainFrame.mainframe.WarnedAboutFWCRCMismatch = false;
        return "Done running compilation script.\n";
    }
    
    @Override
    public File GetWorkingDir() {
        return new File(FirmwareDir());
    }
    
    @Override
    String GetExec() {
        String boarddef = "";
        if (prefs.getFirmwareMode().contains("Ksoloti Core")) {
            boarddef = "BOARD_KSOLOTI_CORE";
        }
        else if (prefs.getFirmwareMode().contains("Axoloti Core")) {
            boarddef = "BOARD_AXOLOTI_CORE";
        }

        if (OSDetect.getOS() == OSDetect.OS.WIN) {
            return RuntimeDir() + "/platform_win/bin/sh.exe " + FirmwareDir() +"/compile_firmware_win.sh " + boarddef;
        }
        else if (OSDetect.getOS() == OSDetect.OS.MAC) {
            return "/bin/sh " + FirmwareDir() + "/compile_firmware_osx.sh " + boarddef;
        }
        else if (OSDetect.getOS() == OSDetect.OS.LINUX) {
            return "/bin/sh " + FirmwareDir() + "/compile_firmware_linux.sh " + boarddef;
        }
        else {
            Logger.getLogger(QCmdCompileFirmware.class.getName()).log(Level.SEVERE, "UPLOAD: OS UNKNOWN!");
            return null;
        }
    }

    @Override
    QCmd err() {
        return null;
    }
}
