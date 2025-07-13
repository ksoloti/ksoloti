/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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

package axoloti;

import axoloti.targetprofile.ksoloti_core;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.SwingUtilities;

import qcmds.QCmdSerialTask;

/**
 *
 * @author jtaelman
 */
public abstract class Connection {
    
    private ArrayList<ConnectionStatusListener> csls = new ArrayList<ConnectionStatusListener>();
    private ArrayList<SDCardMountStatusListener> sdcmls = new ArrayList<SDCardMountStatusListener>();
    private ArrayList<ConnectionFlagsListener> cfcmls = new ArrayList<ConnectionFlagsListener>();

    abstract public boolean isConnected();
    abstract public void disconnect();
    abstract public boolean connect();
    abstract public void SelectPort();
    
    abstract public void TransmitStop();
    abstract public void TransmitStart();
    abstract public void TransmitPing();
    abstract public void TransmitRecallPreset(int presetNo);
    abstract public int  TransmitUploadFragment(byte[] buffer, int offset);
    abstract public int  TransmitGetFileList();
    abstract public int  TransmitGetFileInfo(String filename);
    abstract public int  TransmitCreateFile(String filename, int size, Calendar date);
    abstract public int  TransmitCreateDirectory(String filename, Calendar date);
    abstract public int  TransmitDeleteFile(String filename);
    abstract public int  TransmitChangeWorkingDirectory(String path);
    abstract public int  TransmitAppendFile(byte[] buffer);
    abstract public int  TransmitCloseFile(String filename, Calendar date);
    abstract public void TransmitMemoryRead(int addr, int length);
    abstract public void TransmitMemoryRead1Word(int addr);    
    abstract public void TransmitCosts();
    abstract public void TransmitUpdatedPreset(byte[] b);
    abstract public void TransmitMidi(int m0, int m1, int m2);
    abstract public void TransmitGetFWVersion();
    abstract public void TransmitGetSpilinkSynced();
    abstract public void TransmitCopyToFlash();
    abstract public void TransmitBringToDFU();
    
    abstract public boolean AppendToQueue(QCmdSerialTask cmd);
    abstract public void ClearSync();
    abstract public boolean WaitSync(int msec);
    abstract public boolean WaitSync();
    abstract public void ClearReadSync();
    abstract public boolean WaitReadSync();
    abstract public void setPatch(Patch patch);
    abstract public ksoloti_core getTargetProfile();
    abstract public ByteBuffer getMemReadBuffer();
    abstract public int getMemRead1Word();
    abstract public boolean GetSDCardPresent();
    abstract public int GetConnectionFlags();
    abstract public void setCurrentExecutingCommand(qcmds.QCmdSerialTask command);
    abstract public QCmdSerialTask getCurrentExecutingCommand();
    abstract public String getTargetCpuId();
    abstract public String getDetectedCpuId();

    public void addConnectionStatusListener(ConnectionStatusListener csl) {
        SwingUtilities.invokeLater(() -> {
            if (isConnected()) {
                csl.ShowConnect();
            }
            else {
                csl.ShowDisconnect();
            }
        });
        csls.add(csl);
    }

    public void removeConnectionStatusListener(ConnectionStatusListener csl) {
        csls.remove(csl);
    }

    public void ShowDisconnect() {
        for (ConnectionStatusListener csl : csls) {
            SwingUtilities.invokeLater(() -> {
                csl.ShowDisconnect();
            });
        }
    }

    public void ShowConnect() {
        for (ConnectionStatusListener csl : csls) {
            SwingUtilities.invokeLater(() -> {
                csl.ShowConnect();
            });
        }
    }

    public void addSDCardMountStatusListener(SDCardMountStatusListener sdcml) {
        SwingUtilities.invokeLater(() -> {
            if (GetSDCardPresent()) {
                sdcml.ShowSDCardMounted();
            } else {
                sdcml.ShowSDCardUnmounted();
            }
        });
        sdcmls.add(sdcml);
    }

    public void removeSDCardMountStatusListener(SDCardMountStatusListener sdcml) {
        sdcmls.remove(sdcml);
    }

    public void ShowSDCardMounted() {
        for (SDCardMountStatusListener sdcml : sdcmls) {
            SwingUtilities.invokeLater(() -> {
                sdcml.ShowSDCardMounted();
            });
        }
    }

    public void ShowSDCardUnmounted() {
        for (SDCardMountStatusListener sdcml : sdcmls) {
            SwingUtilities.invokeLater(() -> {
                sdcml.ShowSDCardUnmounted();
            });
        }
    }

    public void addConnectionFlagsListener(ConnectionFlagsListener cfcml) {
        SwingUtilities.invokeLater(() -> {
            cfcml.ShowConnectionFlags(GetConnectionFlags());
        });
        cfcmls.add(cfcml);
    }

    public void removeConnectionFlagsListener(ConnectionFlagsListener cfcml) {
        cfcmls.remove(cfcml);
    }

    public void ShowConnectionFlags(int connectionFlags) {
        for (ConnectionFlagsListener cfcml : cfcmls) {
            SwingUtilities.invokeLater(() -> {
                cfcml.ShowConnectionFlags(connectionFlags);
            });
        }
    }

    @Deprecated
    abstract public int writeBytes(byte[] data);

}
