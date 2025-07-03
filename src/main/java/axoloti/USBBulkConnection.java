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

import static axoloti.MainFrame.prefs;
import static axoloti.dialogs.USBPortSelectionDlg.ErrorString;
import axoloti.dialogs.USBPortSelectionDlg;
import axoloti.displays.DisplayInstance;
import axoloti.parameters.ParameterInstance;
import axoloti.targetprofile.ksoloti_core;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import org.usb4java.*;
import qcmds.QCmd;
import qcmds.QCmdDeleteFile;
import qcmds.QCmdGetFileList;
import qcmds.QCmdMemRead;
import qcmds.QCmdMemRead1Word;
import qcmds.QCmdProcessor;
import qcmds.QCmdSerialTask;
import qcmds.QCmdTransmitGetFWVersion;

/**
 *
 * @author Johannes Taelman
 */
public class USBBulkConnection extends Connection {

    private static final Logger LOGGER = Logger.getLogger(USBBulkConnection.class.getName());

    private Patch patch;
    private volatile boolean disconnectRequested;
    private volatile boolean connected;
    private Thread transmitterThread;
    private Thread receiverThread;
    private final BlockingQueue<QCmdSerialTask> queueSerialTask;
    private String cpuid;
    private ksoloti_core targetProfile;
    private final Context context;
    private DeviceHandle handle;

    private final short bulkVID = (short) 0x16C0;
    private final short bulkPIDAxoloti = (short) 0x0442;
    private final short bulkPIDAxolotiUsbAudio = (short) 0x0447;
    private final short bulkPIDKsoloti = (short) 0x0444;
    private final short bulkPIDKsolotiUsbAudio = (short) 0x0446;
    private int useBulkInterfaceNumber = 2;

    static final byte OUT_ENDPOINT = (byte) 0x02;
    static final byte IN_ENDPOINT = (byte) 0x82;

    private int currentFileSize;
    private int currentFileTimestamp;

    private final Object usbInLock = new Object();  /* For IN endpoint operations (reading) */
    private final Object usbOutLock = new Object(); /* For OUT endpoint operations (writing) */

    protected volatile QCmdSerialTask currentExecutingCommand = null;

	protected USBBulkConnection() {
        this.sync = new Sync();
        this.readsync = new Sync();
        this.patch = null;

        disconnectRequested = false;
        connected = false;
        queueSerialTask = new ArrayBlockingQueue<QCmdSerialTask>(20);
        context = new Context();

        int result = LibUsb.init(context);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to initialize libusb.", result);
        }
    }

    @Override
    public void SetCurrentExecutingCommand(qcmds.QCmdSerialTask command) {
        this.currentExecutingCommand = command;
    }

    @Override
    public QCmdSerialTask GetCurrentExecutingCommand() {
        return currentExecutingCommand;
    }

    @Override
    public void setPatch(Patch patch) {
        this.patch = patch;
    }

    public void Panic() {
        queueSerialTask.clear();
        disconnect();
    }

    @Override
    public boolean isConnected() {
        return connected && (!disconnectRequested);
    }

    @Override
    public boolean AppendToQueue(QCmdSerialTask cmd) {
        try {
            // System.out.println(Instant.now() + " AppendToQueue: attempting to append " + cmd.getClass().getSimpleName());
            boolean added = queueSerialTask.offer(cmd, 100, TimeUnit.MILLISECONDS); // Example timeout
            if (!added) {
                System.out.println(Instant.now() + " AppendToQueue: USBBulkConnection serial task queue full, command not appended: " + cmd.getClass().getSimpleName());
            }
            return added;
        }
        catch (InterruptedException ex) {
            /* Restore the interrupted status, as per best practice */
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "USBBulkConnection AppendToQueue interrupted while offering command: " + cmd.getClass().getSimpleName(), ex);
            return false; // Command was not added due to interruption
        }
    }

    @Override
    public void disconnect() {
        if (connected) {
            disconnectRequested = true;
            connected = false;
            isSDCardPresent = null;
            ShowDisconnect();
            queueSerialTask.clear();

            LOGGER.log(Level.WARNING, "Disconnected\n");

            synchronized (readsync) {
                readsync.Acked = false;
                readsync.notifyAll();
            }
            synchronized (sync) {
                sync.Acked = false;
                sync.notifyAll();
            }

            if (receiverThread != null && receiverThread.isAlive()) {
                receiverThread.interrupt();
                try {
                    receiverThread.join(3000);
                }
                catch (InterruptedException ex) {
                    // System.err.println(Instant.now() + " Receiver join interrupted: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            if (transmitterThread != null && transmitterThread.isAlive()) {
                transmitterThread.interrupt();
                try {
                    transmitterThread.join(3000);
                }
                catch (InterruptedException ex) {
                    // System.err.println(Instant.now() + " Transmitter join interrupted: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            if (receiverThread != null && receiverThread.isAlive()) {
                System.err.println(Instant.now() + " Receiver thread did not terminate gracefully after timeout.");
            }
            if (transmitterThread != null && transmitterThread.isAlive()) {
                System.err.println(Instant.now() + " Transmitter thread did not terminate gracefully after timeout.");
            }
            
            if (handle != null) {
                try {
                    int result = LibUsb.releaseInterface(handle, useBulkInterfaceNumber);
                    if (result != LibUsb.SUCCESS) {
                        LOGGER.log(Level.WARNING, "Connection Warning: Device may have been forcefully disconnected.");
                        // System.err.println(Instant.now() + " LibUsb: Unable to release interface: " + LibUsb.errorName(result) + " (Error Code: " + result + ")");
                    }
                }
                catch (LibUsbException ex) {
                    LOGGER.log(Level.WARNING, "Connection Warning: USB interface release failed unexpectedly.");
                    // System.err.println(Instant.now() + " LibUsb: Exception during interface release: " + ex.getMessage());
                    // ex.printStackTrace(System.err);
                }

                try {
                    LibUsb.close(handle);
                }
                catch (LibUsbException ex) {
                    LOGGER.log(Level.WARNING, "Connection Warning: USB device close failed unexpectedly.");
                    // System.err.println(Instant.now() + " LibUsb: Exception during device close: " + ex.getMessage());
                    // ex.printStackTrace(System.err);
                }
            }

            handle = null;
            CpuId0 = 0;
            CpuId1 = 0;
            CpuId2 = 0;
        }
    }

    public DeviceHandle OpenDeviceHandle() {

        /* Read the USB device list */
        DeviceList list = new DeviceList();

        int result = LibUsb.getDeviceList(context, list);
        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }

        try {
            /* Iterate over all devices and scan for the right one */
            for (Device d : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();

                result = LibUsb.getDeviceDescriptor(d, descriptor);
                if (result != LibUsb.SUCCESS) {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }

                if (prefs.getFirmwareMode().contains("Ksoloti Core")) {

                    if (descriptor.idVendor() == bulkVID && ((descriptor.idProduct() == bulkPIDKsoloti) || (descriptor.idProduct() == bulkPIDKsolotiUsbAudio))) {

                        if (descriptor.idProduct() == bulkPIDKsolotiUsbAudio) {
                            useBulkInterfaceNumber = 4;
                            LOGGER.log(Level.INFO, "Ksoloti Core USBAudio found.");
                        }
                        else {
                            useBulkInterfaceNumber = 2;
                            LOGGER.log(Level.INFO, "Ksoloti Core found.");
                        }

                        DeviceHandle h = new DeviceHandle();

                        result = LibUsb.open(d, h);
                        if (result < 0) {
                            LOGGER.log(Level.INFO, ErrorString(result));
                        }
                        else {
                            String serial = LibUsb.getStringDescriptor(h, descriptor.iSerialNumber());

                            if (cpuid != null) {

                                if (serial.equals(cpuid)) {
                                    return h;
                                }
                            }
                            else {
                                return h;
                            }

                            LibUsb.close(h);
                        }
                    }
                }
                else if (prefs.getFirmwareMode().contains("Axoloti Core")) {

                    if (descriptor.idVendor() == bulkVID && ((descriptor.idProduct() == bulkPIDAxoloti) || (descriptor.idProduct() == bulkPIDAxolotiUsbAudio))) {

                        if (descriptor.idProduct() == bulkPIDAxolotiUsbAudio) {
                            useBulkInterfaceNumber = 4;
                            LOGGER.log(Level.INFO, "Axoloti Core USBAudio found.");
                        }
                        else {
                            useBulkInterfaceNumber = 2;
                            LOGGER.log(Level.INFO, "Axoloti Core found.");
                        }

                        DeviceHandle h = new DeviceHandle();

                        result = LibUsb.open(d, h);
                        if (result < 0) {
                            LOGGER.log(Level.INFO, ErrorString(result));
                        }
                        else {
                            String serial = LibUsb.getStringDescriptor(h, descriptor.iSerialNumber());

                            if (cpuid != null) {
                                if (serial.equals(cpuid)) {
                                    return h;
                                }
                            }
                            else {
                                return h;
                            }

                            LibUsb.close(h);
                        }
                    }
                }
            }

            /* Or else pick the first one */
            for (Device d : list) {

                DeviceDescriptor descriptor = new DeviceDescriptor();

                result = LibUsb.getDeviceDescriptor(d, descriptor);
                if (result != LibUsb.SUCCESS) {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }

                if (prefs.getFirmwareMode().contains("Ksoloti Core")) {

                    if (descriptor.idVendor() == bulkVID && descriptor.idProduct() == bulkPIDKsoloti) {

                        LOGGER.log(Level.INFO, "Ksoloti Core found.");
                        DeviceHandle h = new DeviceHandle();

                        result = LibUsb.open(d, h);
                        if (result < 0) {
                            LOGGER.log(Level.INFO, ErrorString(result));
                            System.err.println(Instant.now() + " LibUsb: Failed to open device handle: " + LibUsb.errorName(result) + " (Error Code: " + result + ")");
                        }
                        else {
                            return h;
                        }
                    }
                }
                else if (prefs.getFirmwareMode().contains("Axoloti Core")) {

                    if (descriptor.idVendor() == bulkVID && descriptor.idProduct() == bulkPIDAxoloti) {

                        LOGGER.log(Level.INFO, "Axoloti Core found.");
                        DeviceHandle h = new DeviceHandle();

                        result = LibUsb.open(d, h);
                        if (result < 0) {
                            LOGGER.log(Level.INFO, ErrorString(result));
                        }
                        else {
                            return h;
                        }
                    }
                }
            }
        }
        finally {
            /* Ensure the allocated device list is freed */
            LibUsb.freeDeviceList(list, true);
        }

        LOGGER.log(Level.SEVERE, "No matching USB devices found.");

        /* Device not found */
        return null;
    }

    @Override
    public boolean connect() {

        disconnect();
        disconnectRequested = false;

        GoIdleState();

        if (cpuid == null) {
            cpuid = prefs.getComPortName();
        }

        targetProfile = new ksoloti_core();

        handle = OpenDeviceHandle();
        if (handle == null) {
            System.err.println(Instant.now() + " USB device not found or inaccessible.");
            return false;
        }

        try {
            int result = LibUsb.claimInterface(handle, useBulkInterfaceNumber);
            if (result != LibUsb.SUCCESS) {
                LOGGER.log(Level.SEVERE, "USB interface already in use or inaccessible.");
                // System.err.println(Instant.now() + " LibUsb: Unable to claim USB interface: " + LibUsb.errorName(result) + " (Error Code: " + result + ")");
                try {
                    LibUsb.close(handle);
                }
                catch (LibUsbException closeEx) {
                    System.err.println(Instant.now() + " Error closing handle after failed claim: " + closeEx.getMessage());
                }
                handle = null;
                return false;
            }

            GoIdleState();

            receiverThread = new Thread(new Receiver());
            receiverThread.setName("Receiver");
            receiverThread.start();

            transmitterThread = new Thread(new Transmitter());
            transmitterThread.setName("Transmitter");
            transmitterThread.start();

            connected = true;
            ClearSync();
            TransmitPing();

            if (!WaitSync()) {
                // System.err.println(Instant.now() + " Initial ping timeout. Connection failed.");
                ShowDisconnect();
                try {
                    disconnect();
                }
                catch (Exception e) {
                    System.err.println(Instant.now() + " Error during cleanup after failed ping response: " + e.getMessage());
                }
                return false;
            }

            LOGGER.log(Level.WARNING, "Connected\n");

            QCmdProcessor qcmdp = MainFrame.mainframe.getQcmdprocessor();
            qcmdp.AppendToQueue(new QCmdTransmitGetFWVersion());
            qcmdp.WaitQueueFinished();

            QCmdMemRead1Word q1 = new QCmdMemRead1Word(targetProfile.getCPUIDCodeAddr());
            qcmdp.AppendToQueue(q1);
            targetProfile.setCPUIDCode(q1.getResult());

            QCmdMemRead q;

            q = new QCmdMemRead(targetProfile.getCPUSerialAddr(), targetProfile.getCPUSerialLength());
            qcmdp.AppendToQueue(q);
            targetProfile.setCPUSerial(q.getResult());
            ShowConnect();

            return true;
        }
        catch (LibUsbException e) {
            LOGGER.log(Level.SEVERE, "Connection Error: A USB communication problem occurred: " + e.getMessage());
            e.printStackTrace(System.err); // Print stack trace to CLI
            ShowDisconnect();
            if (handle != null) {
                try {
                    LibUsb.close(handle);
                }
                catch (LibUsbException ce) {
                    System.err.println(Instant.now() + " Error closing handle after connection exception: " + ce.getMessage());
                }
                handle = null;
            }
            return false;
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Connection Error: An unexpected issue prevented connection: " + ex.getMessage());
            ex.printStackTrace(System.err); // Print stack trace to CLI
            ShowDisconnect();
            if (handle != null) {
                try {
                    LibUsb.close(handle);
                }
                catch (LibUsbException ce) {
                    System.err.println(Instant.now() + " Error closing handle after connection exception: " + ce.getMessage());
                }
                handle = null;
            }
            return false;
        }
    }

    @Override
    public int writeBytes(byte[] bytes) {
        /* Acquire the OUT lock for writing */
        synchronized (usbOutLock) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.rewind();

            IntBuffer transfered = IntBuffer.allocate(1);
    
            int result = LibUsb.bulkTransfer(handle, (byte) OUT_ENDPOINT, buffer, transfered, prefs.getPollInterval() * 30); /*  Timeout is thirty times the poll interval, or ten times the read timeout */
            if (result != LibUsb.SUCCESS) { /* handle error -99 below */

                if (result == -99) {
                    /*
                    * Filter out error -99 ... seems to pop up every now and then but does not lead to connection loss  
                    * this "bug" will likely  be resolved after libusb update
                    */
                    // LOGGER.log(Level.INFO, "USB connection not happy: " + result);
                    return result;
                }

                String errstr;
                switch (result) {
                    case -1:  errstr = "Input/output error"; break;
                    case -2:  errstr = "Invalid parameter"; break;
                    case -3:  errstr = "Access denied (insufficient permissions?)"; break;
                    case -4:  errstr = "Device may have been disconnected"; break;
                    case -5:  errstr = "Device not found"; break;
                    case -6:  errstr = "Device busy"; break;
                    case -7:  errstr = "Operation timed out"; break;
                    case -8:  errstr = "Overflow"; break;
                    case -9:  errstr = "Pipe error"; break;
                    case -10: errstr = "System call interrupted"; break;
                    case -11: errstr = "Insufficient memory"; break;
                    case -12: errstr = "Operation not supported or unimplemented"; break;
                    default:  errstr = Integer.toString(result); break;
                }
                LOGGER.log(Level.WARNING, "USB bulk write failed: " + errstr);
                // QCmdProcessor.getQCmdProcessor().Abort();
            }
            return result;
        }
    }

    @Override
    public void TransmitRecallPreset(int presetNo) {
        byte[] data = new byte[5];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'T';
        data[4] = (byte) presetNo;
        writeBytes(data);
    }

    @Override
    public void BringToDFU() {
        byte[] data = new byte[4];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'D';
        writeBytes(data);
    }

    @Override
    public void TransmitGetFWVersion() {
        byte[] data = new byte[4];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'V';
        writeBytes(data);
    }

    @Override
    public void TransmitGetSpilinkSynced() {
        byte[] data = new byte[4];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'Y';
        writeBytes(data);
    }

    @Override
    public void SendMidi(int m0, int m1, int m2) {
        if (isConnected()) {
            byte[] data = new byte[7];
            data[0] = 'A';
            data[1] = 'x';
            data[2] = 'o';
            data[3] = 'M';
            data[4] = (byte) m0;
            data[5] = (byte) m1;
            data[6] = (byte) m2;
            writeBytes(data);
        }
    }

    @Override
    public void SendUpdatedPreset(byte[] b) {
        byte[] data = new byte[8];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'R';
        int len = b.length;
        data[4] = (byte) len;
        data[5] = (byte) (len >> 8);
        data[6] = (byte) (len >> 16);
        data[7] = (byte) (len >> 24);
        writeBytes(data);
        writeBytes(b);
    }

    @Override
    public void SelectPort() {
        USBPortSelectionDlg spsDlg = new USBPortSelectionDlg(null, true, cpuid);
        spsDlg.setVisible(true);
        cpuid = spsDlg.getCPUID();
        String name = prefs.getBoardName(cpuid);
        if (cpuid == null) return;
        if (name == null) {
            LOGGER.log(Level.INFO, "Selecting CPUID: {0} for connection.", cpuid);
        }
        else {
            LOGGER.log(Level.INFO, "Selecting \"{0}\" for connection.", new Object[]{name});
        }
    }

    static private USBBulkConnection conn = null;

    public static Connection GetConnection() {
        if (conn == null) {
            conn = new USBBulkConnection();
        }
        return conn;
    }
    
    @Override
    public void TransmitGetFileInfo(String filename) {
        byte[] data = new byte[15 + filename.length()];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'C';
        data[4] = 0;
        data[5] = 0;
        data[6] = 0;
        data[7] = 0;
        data[8] = 0;
        data[9] = 'I';
        data[10] = 0;
        data[11] = 0;
        data[12] = 0;
        data[13] = 0;
        int i = 14;
        for (int j = 0; j < filename.length(); j++) {
            data[i++] = (byte) filename.charAt(j);
        }
        data[i] = 0;
        ClearSync();
        writeBytes(data);
        /* No WaitSync() call necessary - will wait for AxoRd response */
    }

    class Sync {
        boolean Acked = false;
    }

    final Sync sync;
    final Sync readsync;

    @Override
    public void ClearSync() {
        synchronized (sync) {
            sync.Acked = false;
        }
    }

    @Override
    public boolean WaitSync(int msec) {
        synchronized (sync) {
                if (sync.Acked) {
                    return sync.Acked;
                }
                try {
                    sync.wait(msec);
                }
                catch (InterruptedException ex) {
                    // LOGGER.log(Level.SEVERE, "Sync wait interrupted");
                }
            return sync.Acked;
        }
    }

    @Override
    public boolean WaitSync() {
        return WaitSync(1000);
    }

    @Override
    public void ClearReadSync() {
        synchronized (readsync) {
            readsync.Acked = false;
        }
    }

    @Override
    public boolean WaitReadSync() {
        synchronized (readsync) {
            if (readsync.Acked) {
                return readsync.Acked;
            }
            try {
                readsync.wait(1000);
            }
            catch (InterruptedException ex) {
                // LOGGER.log(Level.SEVERE, "Sync wait interrupted");
            }
            return readsync.Acked;
        }
    }

    private final byte[] startPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('s')};
    private final byte[] stopPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('S')};
    private final byte[] pingPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('p')};
    private final byte[] getFileListPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('d')};
    private final byte[] copyToFlashPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('F')};

    @Override
    public void TransmitStart() {
        writeBytes(startPckt);
    }

    @Override
    public void TransmitStop() {
        writeBytes(stopPckt);
    }

    @Override
    public void TransmitGetFileList(QCmdSerialTask senderCommand) {
        this.currentExecutingCommand = senderCommand; // Storing the command object
        ClearSync();
        writeBytes(getFileListPckt);
    }

    @Override
    public void TransmitPing() {
        writeBytes(pingPckt);
    }

    @Override
    public void TransmitCopyToFlash() {
        writeBytes(copyToFlashPckt);
    }

    @Override
    public int UploadFragment(byte[] buffer, int offset) {
        byte[] data = new byte[12];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'W';
        int tvalue = offset;
        int nRead = buffer.length;
        data[4] = (byte) tvalue;
        data[5] = (byte) (tvalue >> 8);
        data[6] = (byte) (tvalue >> 16);
        data[7] = (byte) (tvalue >> 24);
        data[8] = (byte) (nRead);
        data[9] = (byte) (nRead >> 8);
        data[10] = (byte) (nRead >> 16);
        data[11] = (byte) (nRead >> 24);
        ClearSync();
        int result = writeBytes(data);
        result |= writeBytes(buffer);
        if (!WaitSync()) {
            result |= 1;
        };
        LOGGER.log(Level.INFO, "Block uploaded @ 0x{0} length {1}",
                   new Object[]{Integer.toHexString(offset).toUpperCase(),
                   Integer.toString(buffer.length)});
        return result;
    }

    @Override
    public void TransmitVirtualButton(int b_or, int b_and, int enc1, int enc2, int enc3, int enc4) {
        byte[] data = new byte[16];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'B';
        data[4] = (byte) b_or;
        data[5] = (byte) (b_or >> 8);
        data[6] = (byte) (b_or >> 16);
        data[7] = (byte) (b_or >> 24);
        data[8] = (byte) b_and;
        data[9] = (byte) (b_and >> 8);
        data[10] = (byte) (b_and >> 16);
        data[11] = (byte) (b_and >> 24);
        data[12] = (byte) (enc1);
        data[13] = (byte) (enc2);
        data[14] = (byte) (enc3);
        data[15] = (byte) (enc4);
        writeBytes(data);
    }

    @Override
    public int TransmitCreateFile(String filename, int size) {
        byte[] data = new byte[9 + filename.length()];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'C';
        data[4] = (byte) size;
        data[5] = (byte) (size >> 8);
        data[6] = (byte) (size >> 16);
        data[7] = (byte) (size >> 24);
        int i = 8;
        for (int j = 0; j < filename.length(); j++) {
            data[i++] = (byte) filename.charAt(j);
        }
        data[i] = 0;
        ClearSync();
        int writeResult = writeBytes(data);
        if (writeResult != LibUsb.SUCCESS) {
            return writeResult;
        }
        boolean syncSuccess = WaitSync(10000);
        return syncSuccess ? LibUsb.SUCCESS : LibUsb.ERROR_TIMEOUT;
    }

    @Override
    public int TransmitCreateFile(String filename, int size, Calendar date) {
        byte[] data = new byte[15 + filename.length()];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'C';
        data[4] = (byte) size;
        data[5] = (byte) (size >> 8);
        data[6] = (byte) (size >> 16);
        data[7] = (byte) (size >> 24);
        data[8] = 0;
        data[9] = 'f';
        int dy = date.get(Calendar.YEAR);
        int dm = date.get(Calendar.MONTH) + 1;
        int dd = date.get(Calendar.DAY_OF_MONTH);
        int th = date.get(Calendar.HOUR_OF_DAY);
        int tm = date.get(Calendar.MINUTE);
        int ts = date.get(Calendar.SECOND);
        int t = ((dy - 1980) * 512) | (dm * 32) | dd;
        int d = (th * 2048) | (tm * 32) | (ts / 2);
        data[10] = (byte) (t & 0xff);
        data[11] = (byte) (t >> 8);
        data[12] = (byte) (d & 0xff);
        data[13] = (byte) (d >> 8);
        int i = 14;
        for (int j = 0; j < filename.length(); j++) {
            data[i++] = (byte) filename.charAt(j);
        }
        data[i] = 0;
        ClearSync();
        int writeResult = writeBytes(data);
        if (writeResult != LibUsb.SUCCESS) {
            return writeResult;
        }
        boolean syncSuccess = WaitSync(10000);
        return syncSuccess ? LibUsb.SUCCESS : LibUsb.ERROR_TIMEOUT;
    }

    @Override
    public int TransmitDeleteFile(String filename, QCmdSerialTask senderCommand) { // CHANGED RETURN TYPE TO int
        this.currentExecutingCommand = senderCommand; // Storing the command object
        byte[] data = new byte[15 + filename.length()];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'C';
        data[4] = 0;
        data[5] = 0;
        data[6] = 0;
        data[7] = 0; // These might be used for cmd ID later
        data[8] = 0;
        data[9] = 'D';
        data[10] = 0;
        data[11] = 0;
        data[12] = 0;
        data[13] = 0;
        int i = 14;
        for (int j = 0; j < filename.length(); j++) {
            data[i++] = (byte) filename.charAt(j);
        }
        data[i] = 0;
        ClearSync();
        int writeResult = writeBytes(data);
        return writeResult;
    }

    @Override
    public int TransmitChangeWorkingDirectory(String path) {
        byte[] data = new byte[15 + path.length()];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'C';
        data[4] = 0;
        data[5] = 0;
        data[6] = 0;
        data[7] = 0;
        data[8] = 0;
        data[9] = 'C';
        data[10] = 0;
        data[11] = 0;
        data[12] = 0;
        data[13] = 0;
        int i = 14;
        for (int j = 0; j < path.length(); j++) {
            data[i++] = (byte) path.charAt(j);
        }
        data[i] = 0;
        ClearSync();
        int writeResult = writeBytes(data);
        if (writeResult != LibUsb.SUCCESS) {
            return writeResult;
        }
        boolean syncSuccess = WaitSync(10000);
        return syncSuccess ? LibUsb.SUCCESS : LibUsb.ERROR_TIMEOUT;
    }

    @Override
    public int TransmitCreateDirectory(String filename, Calendar date) {
        byte[] data = new byte[15 + filename.length()];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'C';
        data[4] = 0;
        data[5] = 0;
        data[6] = 0;
        data[7] = 0;
        data[8] = 0;
        data[9] = 'd';
        data[10] = 0;
        data[11] = 0;
        data[12] = 0;
        data[13] = 0;

        int i = 14;
        for (int j = 0; j < filename.length(); j++) {
            data[i++] = (byte) filename.charAt(j);
        }
        data[i] = 0;
        ClearSync();
        int writeResult = writeBytes(data);
        if (writeResult != LibUsb.SUCCESS) {
            return writeResult;
        }
        boolean syncSuccess = WaitSync(10000);
        return syncSuccess ? LibUsb.SUCCESS : LibUsb.ERROR_TIMEOUT;
    }

    @Override
    public int TransmitAppendFile(byte[] buffer) {
        byte[] data = new byte[8];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'a'; /* changed from 'A' to lower-case to avoid confusion with "AxoA" ack message (MCU->Patcher) */
        int size = buffer.length;
        // LOGGER.log(Level.INFO, "Append size: " + buffer.length);
        data[4] = (byte) size;
        data[5] = (byte) (size >> 8);
        data[6] = (byte) (size >> 16);
        data[7] = (byte) (size >> 24);
        ClearSync();
        int result1 = writeBytes(data);
        if (result1 != LibUsb.SUCCESS) {
            return result1;
        }
        int result2 = writeBytes(buffer);
        if (result2 != LibUsb.SUCCESS) {
            return result2;
        }
        boolean syncSuccess = WaitSync(10000);
        return syncSuccess ? LibUsb.SUCCESS : LibUsb.ERROR_TIMEOUT;
    }

    @Override
    public int TransmitCloseFile() {
        byte[] data = new byte[4];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'c';
        ClearSync();
        int writeResult = writeBytes(data);
        if (writeResult != LibUsb.SUCCESS) {
            return writeResult;
        }
        boolean syncSuccess = WaitSync(10000);
        return syncSuccess ? LibUsb.SUCCESS : LibUsb.ERROR_TIMEOUT;
    }

    @Override
    public void TransmitMemoryRead(int addr, int length) {
        byte[] data = new byte[12];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'r';
        data[4] = (byte) addr;
        data[5] = (byte) (addr >> 8);
        data[6] = (byte) (addr >> 16);
        data[7] = (byte) (addr >> 24);
        data[8] = (byte) length;
        data[9] = (byte) (length >> 8);
        data[10] = (byte) (length >> 16);
        data[11] = (byte) (length >> 24);
        ClearSync();
        writeBytes(data);
        WaitSync();
    }

    @Override
    public void TransmitMemoryRead1Word(int addr) {
        byte[] data = new byte[8];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'y';
        data[4] = (byte) addr;
        data[5] = (byte) (addr >> 8);
        data[6] = (byte) (addr >> 16);
        data[7] = (byte) (addr >> 24);
        ClearSync();
        writeBytes(data);
        WaitSync();
    }

    @Override
    public void TransmitCosts() {
        short uUIMidiCost = prefs.getUiMidiThreadCost();
        byte  uDspLimit200 = (byte)(prefs.getDspLimitPercent()*2);

        byte[] data = new byte[7];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'U';
        data[4] = (byte) uUIMidiCost;
        data[5] = (byte) (uUIMidiCost >> 8);
        data[6] = uDspLimit200;
        ClearSync();
        writeBytes(data);
        WaitSync();
    }


    class Receiver implements Runnable {
        @Override
        public void run() {
            ByteBuffer recvbuffer = ByteBuffer.allocateDirect(4096);
            IntBuffer transfered = IntBuffer.allocate(1);

            while (!disconnectRequested) {
                int result = LibUsb.SUCCESS;
                int sz = 0;

                try {
                    synchronized (usbInLock) {
                        recvbuffer.clear();
                        transfered.clear();
                        result = LibUsb.bulkTransfer(handle, (byte) IN_ENDPOINT, recvbuffer, transfered, prefs.getPollInterval()*3);
                        sz = transfered.get(0);
                    }

                    if (result == LibUsb.SUCCESS && sz > 0) {
                        recvbuffer.position(0);
                        recvbuffer.limit(sz);
                        for (int i = 0; i < sz; i++) {
                            byte b = recvbuffer.get(i);
                            processByte(b);
                        }
                    }
                } catch (LibUsbException e) {
                    LOGGER.log(Level.SEVERE, "Application Error: An unexpected issue occurred in USB Receiver. Connection Lost.");
                    // System.err.println(Instant.now() + " Receiver: Unexpected exception: " + e.getMessage());
                    // e.printStackTrace(System.err);
                    disconnectRequested = true;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Receiver: Unexpected exception: " + e.getMessage(), e);
                    disconnectRequested = true;
                }
            }
            // System.out.println(Instant.now() + " Receiver thread exiting.");
        }
    }

    class Transmitter implements Runnable {
        @Override
        public void run() {
            while (!disconnectRequested) {
                try {
                    QCmdSerialTask cmd = queueSerialTask.take();

                    if (disconnectRequested) {
                        // System.out.println(Instant.now() + " Transmitter: Disconnect requested while waiting for task.");
                        break;
                    }

                    QCmd response = cmd.Do(USBBulkConnection.this);
                    if (response != null) {
                        QCmdProcessor.getQCmdProcessor().getQueueResponse().add(response);
                    }
                }
                catch (InterruptedException ex) {
                    // System.out.println(Instant.now() + " Transmitter: thread interrupted. Exiting loop.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Transmitter: Unexpected exception during command execution: " + e.getMessage(), e);
                }
            }
            // System.out.println(Instant.now() + " Transmitter thread exiting.");
        }
    }

    private Boolean isSDCardPresent = null;
    
    public void SetSDCardPresent(boolean i) {
        if ((isSDCardPresent != null) && (i == isSDCardPresent)) return;
        isSDCardPresent = i;
        if (isSDCardPresent) {
            ShowSDCardMounted();
        }
        else {
            ShowSDCardUnmounted();            
        }
    }

    @Override
    public boolean GetSDCardPresent() {
        if (isSDCardPresent == null) return false;
        return isSDCardPresent;
    }

    private int connectionFlags = 0;

    public void SetConnectionFlags(int newConnectionFlags) {
        if(newConnectionFlags != connectionFlags) {
            connectionFlags = newConnectionFlags;
            ShowConnectionFlags(connectionFlags);
        }
    }

    @Override
    public int GetConnectionFlags() {
        return connectionFlags;
    }

    int CpuId0 = 0;
    int CpuId1 = 0;
    int CpuId2 = 0;
    int fwcrc = -1;

    void Acknowledge(final int ConnectionFlags, final int DSPLoad, final int PatchID, final int Voltages, final int patchIndex, final int sdcardPresent) {

        synchronized (sync) {
            sync.Acked = true;
            sync.notifyAll();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (patch != null) {
                    if ((patch.GetIID() != PatchID) && patch.IsLocked()) {
                        patch.Unlock();
                    }
                    else {
                        boolean dspOverload = 0 != (ConnectionFlags & 1);
                        patch.UpdateDSPLoad(DSPLoad, dspOverload);
                    }
                }
                MainFrame.mainframe.showPatchIndex(patchIndex);
                targetProfile.setVoltages(Voltages);
                SetSDCardPresent(sdcardPresent!=0);
                SetConnectionFlags(ConnectionFlags);
            }
        });
    }

    void RPacketParamChange(final int index, final int value, final int patchID) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public
                    void run() {
                if (patch == null) {
                    // LOGGER.log(Level.INFO, "Rx paramchange patch null {0} {1}", new Object[]{index, value});
                    return;
                }
                if (!patch.IsLocked()) {
                    return;

                }
                if (patch.GetIID() != patchID) {
                    patch.Unlock();
                    return;
                }
                if (index >= patch.ParameterInstances.size()) {
                    LOGGER.log(Level.INFO, "Rx paramchange index out of range{0} {1}",
                               new Object[]{index, value});

                    return;
                }
                ParameterInstance pi = patch.ParameterInstances.get(index);

                if (pi == null) {
                    LOGGER.log(Level.INFO, "Rx paramchange parameterInstance null{0} {1}",
                               new Object[]{index, value});
                    return;
                }

                if (!pi.GetNeedsTransmit()) {
                    pi.SetValueRaw(value);
                }
                // System.out.println(Instant.now() + " rcv ppc objname:" + pi.axoObj.getInstanceName() + " pname:"+ pi.name);
            }
        });

    }

    enum ReceiverState {

        header,
        ackPckt,                /* general acknowledge */
        paramchangePckt,        /* parameter changed */
        // lcdPckt,                /* lcd screen bitmap readback */
        displayPcktHdr,         /* object display readbac */
        displayPckt,            /* object display readback */
        textPckt,               /* text message to display in log */
        sdinfo,                 /* sdcard info */
        fileinfo_fixed_fields,  /* file listing entry, size and timestamp (8 bytes of Axof packet) */
        fileinfo_filename,      /* file listing entry, variable length filename */
        memread,                /* one-time programmable bytes */
        memread1word,           /* one-time programmable bytes */
        fwversion,
        commandResultPckt       /* New Response Packet: ['A', 'x', 'o', 'R', command_byte, status_byte] */
    };

    /*
     * Protocol documentation:
     * "AxoP" + bb + vvvv -> parameter change index bb (16bit), value vvvv (32bit)
     */
    private ReceiverState state = ReceiverState.header;
    private int headerstate;
    private int[] packetData = new int[64];
    private int dataIndex = 0;  /* in bytes */
    private int dataLength = 0; /* in bytes */
    private CharBuffer textRcvBuffer = CharBuffer.allocate(256);
    // private ByteBuffer lcdRcvBuffer = ByteBuffer.allocate(256);
    private ByteBuffer sdinfoRcvBuffer = ByteBuffer.allocate(12);
    private ByteBuffer fileinfoRcvBuffer = ByteBuffer.allocate(256);
    private ByteBuffer memReadBuffer = ByteBuffer.allocate(16 * 4);
    // private int memReadAddr;
    private int memReadLength;
    private int memReadValue;
    private byte[] fwversion = new byte[4];
    private int patchentrypoint;

    @Override
    public ByteBuffer getMemReadBuffer() {
        return memReadBuffer;
    }

    @Override
    public int getMemRead1Word() {
        return memReadValue;
    }

    void storeDataByte(int c) {
        switch (dataIndex & 0x3) {
            case 0:
                packetData[dataIndex >> 2] = c;
                break;
            case 1:
                packetData[dataIndex >> 2] += (c << 8);
                break;
            case 2:
                packetData[dataIndex >> 2] += (c << 16);
                break;
            case 3:
                packetData[dataIndex >> 2] += (c << 24);
                break;
        }
        // System.out.println(Instant.now() + " s " + dataIndex + "  v=" + Integer.toHexString(packetData[dataIndex>>2]) + " c=");
        dataIndex++;

    }

    void DisplayPackHeader(int i1, int i2) {
        if (i2 > 1024) {
            LOGGER.log(Level.FINE, "Lots of data coming! {0} / {1}",
                       new Object[]{Integer.toHexString(i1),
                       Integer.toHexString(i2)});
        }
        else {
            // LOGGER.log(Level.INFO, "OK! " + Integer.toHexString(i1) + " / " + Integer.toHexString(i2));
        }

        if (i2 > 0) {
            dataLength = i2 * 4;
            dataIndex = 0;
            dispData = ByteBuffer.allocate(dataLength);
            dispData.order(ByteOrder.LITTLE_ENDIAN);
            state = ReceiverState.displayPckt;
        }
        else {
            GoIdleState();
        }
    }

    void DistributeToDisplays(final ByteBuffer dispData) {
        // LOGGER.log(Level.INFO, "Distr1");
        try {
            if (patch == null) {
                return;
            }
            if (!patch.IsLocked()) {
                return;
            }
            if (patch.DisplayInstances == null) {
                return;
            }
            // LOGGER.log(Level.INFO, "Distr2");
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    dispData.rewind();
                    for (DisplayInstance d : patch.DisplayInstances) {
                        d.ProcessByteBuffer(dispData);
                    }
                }
            });
        }
        catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        catch (InvocationTargetException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    void GoIdleState() {
        headerstate = 0;
        state = ReceiverState.header;
    }
    ByteBuffer dispData;

    // int LCDPacketRow = 0;

    void processByte(byte cc) {

        int c = cc & 0xff;
        // String charDisplay;
        // if (c >= 0x20 && c <= 0x7E) {
        //     charDisplay = "'" + String.valueOf((char) c) + "'";
        // } else {
        //     charDisplay = String.format("%02x", c) + "h"; // Show hex for non-printable characters
        // }
        // System.out.println(Instant.now() + " processByte c=" + charDisplay + " s=" + state.name());

        switch (state) {

            case header:
                switch (headerstate) {
                    case 0: /* This should always be 'A' or command will be ignored */
                        if (c == 'A') {
                            headerstate = 1;
                            // System.out.println(Instant.now() + " Transitioning to headerstate " + headerstate + " after 'A'");
                        }
                        break;

                    case 1: /* This should always be 'x' or command will be ignored */
                        if (c == 'x') {
                            headerstate = 2;
                            // System.out.println(Instant.now() + " Transitioning to headerstate " + headerstate + " after 'x'");
                        }
                        else {
                            GoIdleState();
                        }
                        break;

                    case 2: /* This should always be 'o' or command will be ignored */
                        if (c == 'o') {
                            headerstate = 3;
                            // System.out.println(Instant.now() + " Transitioning to headerstate " + headerstate + " after 'o'");
                        }
                        else {
                            GoIdleState();
                        }
                        break;

                    case 3:
                        switch (c) {
                            case 'Q':
                                state = ReceiverState.paramchangePckt;
                                dataIndex = 0;
                                dataLength = 12;
                                // System.out.println(Instant.now() + " Completed headerstate after 'Q'");
                                break;
                            case 'A':
                                state = ReceiverState.ackPckt;
                                dataIndex = 0;
                                dataLength = 24;
                                // System.out.println(Instant.now() + " Completed headerstate after 'A'");
                                break;
                            case 'D':
                                state = ReceiverState.displayPcktHdr;
                                dataIndex = 0;
                                dataLength = 8;
                                // System.out.println(Instant.now() + " Completed headerstate after 'D'");
                                break;
                            case 'R': /* New case for 'R' header (AxoR packet from MCU) */
                                state = ReceiverState.commandResultPckt;
                                dataIndex = 0;
                                dataLength = 2; /* Expecting command_byte (1 byte) + status_byte (1 byte) */
                                // System.out.println(Instant.now() + " Completed headerstate after 'R'");
                                break;
                            case 'T':
                                state = ReceiverState.textPckt;
                                textRcvBuffer.clear();
                                dataIndex = 0;
                                dataLength = 255;
                                // System.out.println(Instant.now() + " Completed headerstate after 'T'");
                                break;
                            // case '0': /* non-existing LCD stuff */
                            // case '1':
                            // case '2':
                            // case '3':
                            // case '4':
                            // case '5':
                            // case '6':
                            // case '7':
                            // case '8':
                            //     LCDPacketRow = c - '0';
                            //     state = ReceiverState.lcdPckt;
                            //     lcdRcvBuffer.rewind();
                            //     dataIndex = 0;
                            //     dataLength = 128;
                            //     System.out.println(Instant.now() + " Completed headerstate after digit");
                            //     break;
                            case 'd':
                                state = ReceiverState.sdinfo;
                                sdinfoRcvBuffer.rewind();
                                dataIndex = 0;
                                dataLength = 12;
                                // System.out.println(Instant.now() + " Completed headerstate after 'd'");
                                break;
                            case 'f':
                                state = ReceiverState.fileinfo_fixed_fields;
                                fileinfoRcvBuffer.clear();
                                dataIndex = 0;
                                // dataLength = 8;
                                // System.out.println(Instant.now() + " Completed headerstate after 'f'");
                                break;
                            case 'r':
                                state = ReceiverState.memread;
                                memReadBuffer.clear();
                                dataIndex = 0;
                                // System.out.println(Instant.now() + " Completed headerstate after 'r'");
                                break;
                            case 'y':
                                state = ReceiverState.memread1word;
                                dataIndex = 0;
                                // System.out.println(Instant.now() + " Completed headerstate after 'y'");
                                break;
                            case 'V':
                                state = ReceiverState.fwversion;
                                dataIndex = 0;
                                // System.out.println(Instant.now() + " Completed headerstate after 'V'");
                                break;
                            default:
                                GoIdleState();
                                System.err.println(Instant.now() + " Error trying to complete headerstate after valid 'Axo'");
                                break;
                        }
                        break;

                    default:
                        System.err.println(Instant.now() + " Receiver: invalid header");
                        GoIdleState();

                        break;
                }
                break;

            case paramchangePckt:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
                // System.out.println(Instant.now() + " pch packet i=" +dataIndex + " v=" + c + " c="+ (char)(cc));
                if (dataIndex == dataLength) {
                    // System.out.println(Instant.now() + " param packet complete 0x" + Integer.toHexString(packetData[1]) + "    0x" + Integer.toHexString(packetData[0]));
                    RPacketParamChange(packetData[2], packetData[1], packetData[0]);
                    GoIdleState();
                }
                break;

            case ackPckt:
                if (dataIndex < dataLength) {
                    // System.out.println(Instant.now() + " ack packet i=" +dataIndex + " v=" + c + " c="+ (char)(cc));
                    storeDataByte(c);
                }
                if (dataIndex == dataLength) {
                    // System.out.println(Instant.now() + " ack packet complete");
                    Acknowledge(packetData[0], packetData[1], packetData[2], packetData[3], packetData[4], packetData[5]);
                    GoIdleState();
                }
                break;

            // case lcdPckt:
            //     if (dataIndex < dataLength) {
            //         // System.out.println(Instant.now() + " lcd packet i=" +dataIndex + " v=" + c + " c="+ (char)(cc));
            //         lcdRcvBuffer.put(cc);
            //         dataIndex++;
            //     }
            //     if (dataIndex == dataLength) {
            //         lcdRcvBuffer.rewind();
            //         // MainFrame.mainframe.remote.updateRow(LCDPacketRow, lcdRcvBuffer);
            //         GoIdleState();
            //     }
            //     break;

            case displayPcktHdr:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
                // System.out.println(Instant.now() + " pch packet i=" +dataIndex + " v=" + c + " c="+ (char)(cc));
                if (dataIndex == dataLength) {
                    DisplayPackHeader(packetData[0], packetData[1]);
                }
                break;

            case displayPckt:
                if (dataIndex < dataLength) {
                    dispData.put(cc);
                    dataIndex++;
                }
                if (dataIndex == dataLength) {
                    DistributeToDisplays(dispData);
                    GoIdleState();
                }
                break;

            case commandResultPckt: // This state is now reached after receiving an AxoR packet
                if (dataIndex < dataLength) {
                    storeDataByte(c); // Store the incoming bytes (command_byte then status_byte)
                }
                if (dataIndex == dataLength) { // Once both bytes are received
                    int commandByte = packetData[0] & 0xFF;
                    int statusCode = (packetData[0] >> 8) & 0xFF;

                    if (commandByte == 'D') { /* Check if this result is for the 'D' (delete) command */
                        if (statusCode == 0) { /* FR_OK (0) indicates success */
                            System.out.println(Instant.now() + " Delete command 'D' confirmed successful by MCU.");
                            if (currentExecutingCommand instanceof QCmdDeleteFile) {
                                ((QCmdDeleteFile) currentExecutingCommand).setCommandCompleted(true);
                            }
                        }
                        else {
                            System.out.println(Instant.now() + " Delete command 'D' confirmed failed by MCU with status code: " + statusCode);
                            if (currentExecutingCommand instanceof QCmdDeleteFile) {
                                ((QCmdDeleteFile) currentExecutingCommand).setCommandCompleted(false);
                            }
                        }
                        /* After setting completion, clear the currentExecutingCommand */
                        currentExecutingCommand = null;
                    }
                    else if (commandByte == 'd') {
                        if (statusCode == 0) {
                            System.out.println(Instant.now() + " Filelist command 'd' confirmed successful by MCU.");
                            if (currentExecutingCommand instanceof QCmdGetFileList) {
                                ((QCmdGetFileList) currentExecutingCommand).setCommandCompleted(true);
                            }
                            
                        }
                        else {
                            System.out.println(Instant.now() + " Filelist command 'd' confirmed failed by MCU with status code: " + statusCode);
                            if (currentExecutingCommand instanceof QCmdGetFileList) {
                                ((QCmdGetFileList) currentExecutingCommand).setCommandCompleted(false);
                            }
                        }
                        currentExecutingCommand = null;
                    }

                    // Add 'else if (commandByte == '...')' blocks here to handle results for other commands...

                    GoIdleState();
                }
                break;

            case textPckt:
                if (c != 0) {
                    textRcvBuffer.append((char) cc);
                }
                else {
                    // textRcvBuffer.append((char) cc);
                    textRcvBuffer.limit(textRcvBuffer.position());
                    textRcvBuffer.rewind();
                    if (Pattern.compile("File error:.*filename:\"/start.bin\"").matcher(textRcvBuffer.toString()).find()) {
                    // if (textRcvBuffer.toString().toLowerCase().contains("file error: fr_no_file, filename:\"/start.bin\"")) {
                        /* Filter out error if SD card is connected but no start.bin is found */
                        LOGGER.log(Level.INFO, "SD card connected, no startup patch found.");
                    }
                    else {
                        LOGGER.log(Level.WARNING, "{0}", textRcvBuffer.toString());
                    }
                    // System.out.println(Instant.now() + " FINAL MCU Text Message (AxoT): " + textRcvBuffer.toString());
                    GoIdleState();
                }
                break;

            case sdinfo:
                if (dataIndex < dataLength) {
                    sdinfoRcvBuffer.put(cc);
                    dataIndex++;
                }
                if (dataIndex == dataLength) {
                    sdinfoRcvBuffer.rewind();
                    sdinfoRcvBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    // LOGGER.log(Level.INFO, "sdinfo: "
                    // + sdinfoRcvBuffer.asIntBuffer().get(0) + " "
                    // + sdinfoRcvBuffer.asIntBuffer().get(1) + " "
                    // + sdinfoRcvBuffer.asIntBuffer().get(2));
                    SDCardInfo.getInstance().SetInfo(sdinfoRcvBuffer.asIntBuffer().get(0), sdinfoRcvBuffer.asIntBuffer().get(1), sdinfoRcvBuffer.asIntBuffer().get(2));
                    GoIdleState();
                }
                break;

                case fileinfo_fixed_fields: /* State to collect the 8-byte size and timestamp */
                fileinfoRcvBuffer.put(cc);
                dataIndex++;
        
                if (dataIndex == 8) { /* exactly 8 bytes (size + timestamp) */ 
                    // System.out.println(Instant.now() + " processByte: Received fixed fields for Axof. Processing.");
        
                    fileinfoRcvBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    fileinfoRcvBuffer.limit(fileinfoRcvBuffer.position());
                    fileinfoRcvBuffer.rewind();
        
                    /* Read the 4-byte size and 4-byte timestamp */
                    currentFileSize = fileinfoRcvBuffer.getInt();
                    currentFileTimestamp = fileinfoRcvBuffer.getInt();
                    // System.out.println(Instant.now() + " processByte: Parsed preliminary size: " + currentFileSize + ", timestamp: " + currentFileTimestamp);
        
                    /* Prepare to collect the variable-length filename */
                    fileinfoRcvBuffer.clear();
                    dataIndex = 0;
                    state = ReceiverState.fileinfo_filename; /* Transition to the next sub-state */
                }
                break;
        
            case fileinfo_filename: /* State to collect filename bytes until null terminator */
                if (cc == 0x00) {
                    // System.out.println(Instant.now() + " processByte: Null terminator found for Axof filename. Processing.");
        
                    fileinfoRcvBuffer.limit(fileinfoRcvBuffer.position());
                    fileinfoRcvBuffer.rewind();
        
                    /* Get the collected filename bytes as an array */
                    byte[] filenameBytes = new byte[fileinfoRcvBuffer.remaining()];
                    fileinfoRcvBuffer.get(filenameBytes);
                    String fname = new String(filenameBytes, Charset.forName("ISO-8859-1"));

                    SDCardInfo.getInstance().AddFile(fname, currentFileSize, currentFileTimestamp);
                    // System.out.println(Instant.now() + " processByte: Parsed file: \"" + fname + "\", size: " + currentFileSize + ", timestamp: " + currentFileTimestamp);
        
                    GoIdleState(); /* Packet complete, return to idle */
                }
                else {
                    /* Collect the current byte as part of the filename */
                    fileinfoRcvBuffer.put(cc);
                    dataIndex++;
        
                    /* Protection against malformed Axof */
                    if (dataIndex >= fileinfoRcvBuffer.capacity()) {
                        LOGGER.log(Level.SEVERE, "processByte: Filename exceeds maximum expected length. Aborting packet.");
                        GoIdleState();
                    }
                }
                break;
        
            case memread:
                switch (dataIndex) {
                    // case 0:
                    //     memReadAddr = (cc & 0xFF);
                    //     break;
                    // case 1:
                    //     memReadAddr += (cc & 0xFF) << 8;
                    //     break;
                    // case 2:
                    //     memReadAddr += (cc & 0xFF) << 16;
                    //     break;
                    // case 3:
                    //     memReadAddr += (cc & 0xFF) << 24;
                    //     break;
                    case 4:
                        memReadLength = (cc & 0xFF);
                        break;
                    case 5:
                        memReadLength += (cc & 0xFF) << 8;
                        break;
                    case 6:
                        memReadLength += (cc & 0xFF) << 16;
                        break;
                    case 7:
                        memReadLength += (cc & 0xFF) << 24;
                        break;
                    case 8:
                        memReadBuffer = ByteBuffer.allocate(memReadLength);
                        memReadBuffer.rewind();
                    default:
                        memReadBuffer.put(cc);
                        if (dataIndex == memReadLength + 7) {
                            memReadBuffer.rewind();
                            memReadBuffer.order(ByteOrder.LITTLE_ENDIAN);
                            // System.out.println(Instant.now() + " memread offset 0x" + Integer.toHexString(memReadAddr));
                            // int i = 0;
                            // while (memReadBuffer.hasRemaining()) {
                            //     System.out.print(" " + String.format("%02X", memReadBuffer.get()));
                            //     i++;
                            //     // if ((i % 4) == 0) {
                            //         // System.out.print(" ");
                            //     // }
                            //     if ((i % 32) == 0) {
                            //         System.out.println();
                            //     }
                            // }
                            // System.out.println();
                            synchronized (readsync) {
                                readsync.Acked = true;
                                readsync.notifyAll();
                            }
                            GoIdleState();
                        }
                }
                dataIndex++;
                break;

            case memread1word:
                switch (dataIndex) {
                    // case 0:
                    //     memReadAddr = (cc & 0xFF);
                    //     break;
                    // case 1:
                    //     memReadAddr += (cc & 0xFF) << 8;
                    //     break;
                    // case 2:
                    //     memReadAddr += (cc & 0xFF) << 16;
                    //     break;
                    // case 3:
                    //     memReadAddr += (cc & 0xFF) << 24;
                    //     break;
                    case 4:
                        memReadValue = (cc & 0xFF);
                        break;
                    case 5:
                        memReadValue += (cc & 0xFF) << 8;
                        break;
                    case 6:
                        memReadValue += (cc & 0xFF) << 16;
                        break;
                    case 7:
                        memReadValue += (cc & 0xFF) << 24;
                        // System.out.println(Instant.now() + " " + String.format("addr %08X value %08X", memReadAddr, memReadValue));
                        synchronized (readsync) {
                            readsync.Acked = true;
                            readsync.notifyAll();
                        }
                        GoIdleState();
                }
                dataIndex++;
                break;

            case fwversion:
                switch (dataIndex) {
                    case 0:
                        fwversion[0] = cc;
                        break;
                    case 1:
                        fwversion[1] = cc;
                        break;
                    case 2:
                        fwversion[2] = cc;
                        break;
                    case 3:
                        fwversion[3] = cc;
                        break;
                    case 4:
                        fwcrc = (cc & 0xFF) << 24;
                        break;
                    case 5:
                        fwcrc += (cc & 0xFF) << 16;
                        break;
                    case 6:
                        fwcrc += (cc & 0xFF) << 8;
                        break;
                    case 7:
                        fwcrc += (cc & 0xFF);
                        break;
                    case 8:
                        patchentrypoint = (cc & 0xFF) << 24;
                        break;
                    case 9:
                        patchentrypoint += (cc & 0xFF) << 16;
                        break;
                    case 10:
                        patchentrypoint += (cc & 0xFF) << 8;
                        break;
                    case 11:
                        patchentrypoint += (cc & 0xFF);
                        String sFwcrc = String.format("%08X", fwcrc);
                        System.out.println(Instant.now() + " " + String.format("Firmware version: %d.%d.%d.%d, CRC: 0x%s, entry point: 0x%08X", fwversion[0], fwversion[1], fwversion[2], fwversion[3], sFwcrc, patchentrypoint));
                        LOGGER.log(Level.INFO, String.format("Firmware version %d.%d.%d.%d | CRC %s\n", fwversion[0], fwversion[1], fwversion[2], fwversion[3], sFwcrc));
                        MainFrame.mainframe.setFirmwareID(sFwcrc);
                        GoIdleState();
                        break;
                }
                dataIndex++;
                break;

            default:
                GoIdleState();
                System.out.println(Instant.now() + " Unhandled byte c=" + String.format("%02x", c) + "(char=" + (char)c + ") in state=" + state);
                break;
        }
    }

    @Override
    public ksoloti_core getTargetProfile() {
        return targetProfile;
    }

}
