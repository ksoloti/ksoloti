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

import axoloti.Patch;
import axoloti.utils.OSDetect;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static axoloti.MainFrame.prefs;

/**
 *
 * @author Johannes Taelman
 */
public class QCmdCompilePatch extends QCmdShellTask {

    Patch p;

    public QCmdCompilePatch(Patch p) {
        this.p = p;
    }

    @Override
    public String GetStartMessage() {
        String pname = p.getFileNamePath();
        int i = pname.lastIndexOf(File.separatorChar);
        if (i < 0) {
            return "Compiling patch...";
        }
        pname = pname.substring(i+1, pname.length());
        return "Compiling patch... " + pname;
    }

    @Override
    public String GetDoneMessage() {
        if (success) {
            return "Done compiling patch.\n";
        } else {
            return "Patch compilation failed: " + p.getFileNamePath() + "\n";
        }
    }
    
    @Override
    public File GetWorkingDir() {
        return new File(FirmwareDir());
    }
    
    @Override
    String GetExec() {
            String boarddef = "";
            String fwoptiondef = "";
            if (prefs.getFirmwareMode().contains("Ksoloti Core")) {
                boarddef = "BOARD_KSOLOTI_CORE";
            }
            else if (prefs.getFirmwareMode().contains("Axoloti Core")) {
                boarddef = "BOARD_AXOLOTI_CORE";
            }
            if (prefs.getFirmwareMode().contains("SPILink")) {
                fwoptiondef += " FW_SPILINK";
            }
            if (prefs.getFirmwareMode().contains("USBAudio")) {
                fwoptiondef += " FW_USBAUDIO";
            }

            if (OSDetect.getOS() == OSDetect.OS.WIN) {
                return FirmwareDir() + "/compile_patch_win.bat " + boarddef + fwoptiondef;
            } else if (OSDetect.getOS() == OSDetect.OS.MAC) {
                return "/bin/sh ./compile_patch_osx.sh " + boarddef + fwoptiondef;
            } else if (OSDetect.getOS() == OSDetect.OS.LINUX) {
                return "/bin/sh ./compile_patch_linux.sh " + boarddef + fwoptiondef;
            } else {
                Logger.getLogger(QCmdCompilePatch.class.getName()).log(Level.SEVERE, "UPLOAD: OS UNKNOWN!");
                return null;
            }
    }

    @Override
    QCmd err() {
        return new QCmdShowCompileFail(p);
    }
}
