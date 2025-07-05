package axoloti;

import axoloti.targetprofile.ksoloti_core;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;

import qcmds.QCmdSerialTask;

/**
 *
 * @author jtaelman
 */
public abstract class Connection {
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
    
    private ArrayList<ConnectionStatusListener> csls = new ArrayList<ConnectionStatusListener>();

    public void addConnectionStatusListener(ConnectionStatusListener csl) {
        if (isConnected()) {
            csl.ShowConnect();
        } else {
            csl.ShowDisconnect();
        }
        csls.add(csl);
    }

    public void removeConnectionStatusListener(ConnectionStatusListener csl) {
        csls.remove(csl);
    }

    public void ShowDisconnect() {
        for (ConnectionStatusListener csl : csls) {
            csl.ShowDisconnect();
        }
    }

    public void ShowConnect() {
        for (ConnectionStatusListener csl : csls) {
            csl.ShowConnect();
        }
    }

    private ArrayList<SDCardMountStatusListener> sdcmls = new ArrayList<SDCardMountStatusListener>();

    public void addSDCardMountStatusListener(SDCardMountStatusListener sdcml) {
        if (GetSDCardPresent()) {
            sdcml.ShowSDCardMounted();
        } else {
            sdcml.ShowSDCardUnmounted();
        }
        sdcmls.add(sdcml);
    }

    public void removeSDCardMountStatusListener(SDCardMountStatusListener sdcml) {
        sdcmls.remove(sdcml);
    }

    public void ShowSDCardMounted() {
        for (SDCardMountStatusListener sdcml : sdcmls) {
            sdcml.ShowSDCardMounted();
        }
    }

    public void ShowSDCardUnmounted() {
        for (SDCardMountStatusListener sdcml : sdcmls) {
            sdcml.ShowSDCardUnmounted();
        }
    }

    private ArrayList<ConnectionFlagsListener> cfcmls = new ArrayList<ConnectionFlagsListener>();

    public void addConnectionFlagsListener(ConnectionFlagsListener cfcml) {
        cfcml.ShowConnectionFlags(GetConnectionFlags());
        cfcmls.add(cfcml);
    }

    public void removeConnectionFlagsListener(ConnectionFlagsListener cfcml) {
        cfcmls.remove(cfcml);
    }

    public void ShowConnectionFlags(int connectionFlags) {
        for (ConnectionFlagsListener cfcml : cfcmls) {
            cfcml.ShowConnectionFlags(connectionFlags);
        }
    }

    @Deprecated
    abstract public int writeBytes(byte[] data);

}
