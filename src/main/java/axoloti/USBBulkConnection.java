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
import java.nio.charset.StandardCharsets;
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
import qcmds.QCmdChangeWorkingDirectory;
import qcmds.QCmdCreateDirectory;
import qcmds.QCmdDeleteFile;
import qcmds.QCmdGetFileInfo;
import qcmds.QCmdGetFileList;
import qcmds.QCmdMemRead;
import qcmds.QCmdMemRead1Word;
import qcmds.QCmdPing;
import qcmds.QCmdProcessor;
import qcmds.QCmdSerialTask;
import qcmds.QCmdTransmitGetFWVersion;
import qcmds.QCmdUploadFile;

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
    static private USBBulkConnection conn = null;

    ByteBuffer dispData;

    final Sync sync;
    final Sync readsync;

    private Boolean isSDCardPresent = null;
    private int connectionFlags = 0;

    int CpuId0 = 0;
    int CpuId1 = 0;
    int CpuId2 = 0;
    int fwcrc = -1;

    private final byte[] startPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('s')};
    private final byte[] stopPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('S')};
    private final byte[] pingPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('p')};
    private final byte[] getFileListPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('l')};
    private final byte[] copyToFlashPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('F')};
    private final byte[] axoFileOpPckt = new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('C')};

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

    class Sync {
        boolean Acked = false;
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

                        if (result != LibUsb.SUCCESS) {
                            // System.err.println(Instant.now() + " [DEBUG] Receiver: LibUsb.bulkTransfer returned error: " + result + " (" + LibUsb.strError(result) + ")");
                        }
                    }

                    if (result == LibUsb.SUCCESS && sz > 0) {
                        recvbuffer.position(0);
                        recvbuffer.limit(sz);
                        for (int i = 0; i < sz; i++) {
                            byte b = recvbuffer.get(i);
                            processByte(b);
                        }
                    }
                }
                catch (LibUsbException e) {
                    // System.err.println(Instant.now() + " [DEBUG] Receiver: LibUsbException: " + e.getMessage());
                    e.printStackTrace(System.err);
                    // SwingUtilities.invokeLater(() -> {
                    //     if (MainFrame.mainframe != null) {
                    //         MainFrame.mainframe.abortAllOperations("USB connection lost unexpectedly. Please check device and reconnect.");
                    //     }
                    // });
                    disconnectRequested = true;
                }
                catch (Exception e) {
                    // System.err.println(Instant.now() + " [DEBUG] Receiver: Unexpected exception: " + e.getMessage());
                    e.printStackTrace(System.err);
                    // SwingUtilities.invokeLater(() -> {
                    //     if (MainFrame.mainframe != null) {
                    //         MainFrame.mainframe.abortAllOperations("USB connection lost unexpectedly. Please check device and reconnect.");
                    //     }
                    // });
                    disconnectRequested = true;
                }
            }
            // System.out.println(Instant.now() + " [DEBUG] Receiver thread exiting.");
        }
    }

    class Transmitter implements Runnable {
        @Override
        public void run() {
            while (!disconnectRequested) {
                try {
                    QCmdSerialTask cmd = queueSerialTask.take();

                    if (disconnectRequested) {
                        // System.out.println(Instant.now() + " [DEBUG] Transmitter: Disconnect requested while waiting for task.");
                        break;
                    }

                    QCmd response = cmd.Do(USBBulkConnection.this);
                    if (response != null) {
                        QCmdProcessor.getQCmdProcessor().getQueueResponse().add(response);
                    }
                }
                catch (InterruptedException ex) {
                    // System.out.println(Instant.now() + " [DEBUG] Transmitter: thread interrupted. Exiting loop.");
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Transmitter: Unexpected exception during command execution: " + e.getMessage(), e);
                }
            }
            // System.out.println(Instant.now() + " [DEBUG] Transmitter thread exiting.");
        }
    }

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
    public void setCurrentExecutingCommand(QCmdSerialTask command) {
        this.currentExecutingCommand = command;
    }

    @Override
    public QCmdSerialTask getCurrentExecutingCommand() {
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
            if (!(cmd instanceof QCmdPing)) {
                // System.out.println(Instant.now() + " [DEBUG] AppendToQueue: attempting to append " + cmd.getClass().getSimpleName());
            }
            boolean added = queueSerialTask.offer(cmd, 100, TimeUnit.MILLISECONDS);
            if (!added) {
                LOGGER.log(Level.WARNING, "USB command queue full. Command not sent: " + cmd.getClass().getSimpleName());
            }
            return added;
        }
        catch (InterruptedException ex) {
            /* Restore the interrupted status, as per best practice */
            Thread.currentThread().interrupt();
            // System.err.println(Instant.now() + " [DEBUG] USBBulkConnection AppendToQueue interrupted while offering command: " + cmd.getClass().getSimpleName() + " - " + ex.getMessage());
            return false; /* Command was not added due to interruption */
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
                    // System.err.println(Instant.now() + " [DEBUG] Receiver join interrupted: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            if (transmitterThread != null && transmitterThread.isAlive()) {
                transmitterThread.interrupt();
                try {
                    transmitterThread.join(3000);
                }
                catch (InterruptedException ex) {
                    // System.err.println(Instant.now() + " [DEBUG] Transmitter join interrupted: " + ex.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            // if (receiverThread != null && receiverThread.isAlive()) {
            //     System.err.println(Instant.now() + " [DEBUG] Receiver thread did not terminate gracefully after timeout.");
            // }
            // if (transmitterThread != null && transmitterThread.isAlive()) {
            //     System.err.println(Instant.now() + " [DEBUG] Transmitter thread did not terminate gracefully after timeout.");
            // }
            
            if (handle != null) {
                try {
                    int result = LibUsb.releaseInterface(handle, useBulkInterfaceNumber);
                    if (result != LibUsb.SUCCESS) {
                        // System.err.println(Instant.now() + " [DEBUG] LibUsb: Unable to release interface: " + LibUsb.errorName(result) + " (Error Code: " + result + ")");
                    }
                }
                catch (LibUsbException ex) {
                    // System.err.println(Instant.now() + " [DEBUG] LibUsb: Exception during interface release: " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }

                try {
                    LibUsb.close(handle);
                }
                catch (LibUsbException ex) {
                    // System.err.println(Instant.now() + " [DEBUG] LibUsb: Exception during device close: " + ex.getMessage());
                    ex.printStackTrace(System.err);
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
                            // System.err.println(Instant.now() + " [DEBUG] LibUsb: Failed to open device handle: " + LibUsb.errorName(result) + " (Error Code: " + result + ")");
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
            // System.err.println(Instant.now() + " [DEBUG] USB device not found or inaccessible.");
            return false;
        }

        try {
            int result = LibUsb.claimInterface(handle, useBulkInterfaceNumber);
            if (result != LibUsb.SUCCESS) {
                LOGGER.log(Level.WARNING, "USB interface already in use or inaccessible.");
                // System.err.println(Instant.now() + " [DEBUG] LibUsb: Unable to claim USB interface: " + LibUsb.errorName(result) + " (Error Code: " + result + ")");
                try {
                    LibUsb.close(handle);
                }
                catch (LibUsbException closeEx) {
                    // System.err.println(Instant.now() + " [DEBUG] Error closing handle after failed claim: " + closeEx.getMessage());
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
                // System.err.println(Instant.now() + " [DEBUG] Initial ping timeout. Connection failed.");
                ShowDisconnect();
                try {
                    disconnect();
                }
                catch (Exception e) {
                    // System.err.println(Instant.now() + " [DEBUG] Error during cleanup after failed ping response: " + e.getMessage());
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
            ShowDisconnect();
            if (handle != null) {
                try {
                    LibUsb.close(handle);
                }
                catch (LibUsbException ce) {
                    // System.err.println(Instant.now() + " [DEBUG] Error closing handle after connection exception: " + ce.getMessage());
                }
                handle = null;
            }
            return false;
        }
        catch (Exception ex) {
            ShowDisconnect();
            if (handle != null) {
                try {
                    LibUsb.close(handle);
                }
                catch (LibUsbException ce) {
                    // System.err.println(Instant.now() + " [DEBUG] Error closing handle after connection exception: " + ce.getMessage());
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

            if (handle == null) {
                LOGGER.log(Level.SEVERE, "USB bulk write failed: Connection is not active.");
                // System.err.println(Instant.now() + " [DEBUG] USB bulk write failed: handle is null. Disconnected?");
                return LibUsb.ERROR_NO_DEVICE;
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.rewind();

            IntBuffer transfered = IntBuffer.allocate(1);
    
            int result = LibUsb.bulkTransfer(handle, (byte) OUT_ENDPOINT, buffer, transfered, 1000); /* 1s timeout */
            if (result != LibUsb.SUCCESS) {

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
                LOGGER.log(Level.SEVERE, "USB bulk write failed: " + errstr);
                QCmdProcessor.getQCmdProcessor().Abort();
            }
            return result;
        }
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

    public static Connection GetConnection() {
        if (conn == null) {
            conn = new USBBulkConnection();
        }
        return conn;
    }

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

    @Override
    public void TransmitStart() {
        writeBytes(startPckt);
    }

    @Override
    public void TransmitStop() {
        writeBytes(stopPckt);
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
    public void TransmitBringToDFU() {
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
    public void TransmitMidi(int m0, int m1, int m2) {
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
    public void TransmitUpdatedPreset(byte[] b) {
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
    public int TransmitGetFileList() {
        // ClearSync();
        int writeResult = writeBytes(getFileListPckt);
        return writeResult;
    }

    @Override
    public int TransmitGetFileInfo(String filename) {

        /* Total size:
           "AxoC"           (4) +
           pFileSize        (4) +
           FileName[0]      (1) +
           FileName[1]      (1) +
           (skip fdate)
           (skip ftime)
           filename bytes   (variable length) +
           null terminator  (1)
        */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(10 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(axoFileOpPckt);  // "AxoC" header
        buffer.putInt(0);           // pFileSize placeholder (4 bytes)
        buffer.put((byte)0x00);     // FileName[0] (always 0 for new protocol)
        buffer.put((byte)'I');      // FileName[1] (sub-command 'I')
        // buffer.putShort((short)0);  // fdate placeholder (2 bytes)
        // buffer.putShort((short)0);  // ftime placeholder (2 bytes)
        buffer.put(filenameBytes);  // FileName[6]+ (variable length)
        buffer.put((byte)0x00);     // Null terminator

        // ClearSync();
        int writeResult = writeBytes(buffer.array());
        return writeResult;
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
    public int TransmitUploadFragment(byte[] buffer, int offset) {
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
    public int TransmitCreateFile(String filename, int size, Calendar date) {

        /* Total size:
           "AxoC"           (4) +
           pFileSize        (4) +
           FileName[0]      (1) +
           FileName[1]      (1) +
           fdate            (2) +
           ftime            (2) +
           filename bytes   (variable length) +
           null terminator  (1)
         */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(14 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(axoFileOpPckt);  // "AxoC" header
        buffer.putInt(size);        // pFileSize (4 bytes)
        buffer.put((byte)0x00);     // FileName[0] (always 0 for new protocol)
        buffer.put((byte)'f');      // FileName[1] (sub-command 'f')

        /* Calculate FatFs date/time */
        int dy = date.get(Calendar.YEAR);
        int dm = date.get(Calendar.MONTH) + 1;
        int dd = date.get(Calendar.DAY_OF_MONTH);
        int th = date.get(Calendar.HOUR_OF_DAY);
        int tm = date.get(Calendar.MINUTE);
        int ts = date.get(Calendar.SECOND);
        short fatFsDate = (short)(((dy - 1980) << 9) | (dm << 5) | dd);
        short fatFsTime = (short)((th << 11) | (tm << 5) | (ts / 2));

        buffer.putShort(fatFsDate); // FileName[2/3]
        buffer.putShort(fatFsTime); // FileName[4/5]
        buffer.put(filenameBytes);  // FileName[6]+ (variable length)
        buffer.put((byte)0x00);     // Null terminator

        // ClearSync();
        int writeResult = writeBytes(buffer.array());
        return writeResult;
    }

    @Override
    public int TransmitDeleteFile(String filename) {

        /* Total size:
           "AxoC"           (4) +
           pFileSize        (4) +
           FileName[0]      (1) +
           FileName[1]      (1) +
           (skip fdate)
           (skip ftime)
           filename bytes   (variable length) +
           null terminator  (1)
         */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(10 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(axoFileOpPckt);  // "AxoC" header
        buffer.putInt(0);           // pFileSize placeholder (4 bytes)
        buffer.put((byte)0x00);     // FileName[0] (always 0)
        buffer.put((byte)'D');      // FileName[1] (sub-command 'D')
        // buffer.putShort((short)0);  // fdate placeholder (2 bytes)
        // buffer.putShort((short)0);  // ftime placeholder (2 bytes)
        buffer.put(filenameBytes);  // FileName[6]+ (variable length)
        buffer.put((byte)0x00);     // Null terminator

        int writeResult = writeBytes(buffer.array());
        return writeResult;
    }

    @Override
    public int TransmitChangeWorkingDirectory(String path) {

        /* Total size:
           "AxoC"           (4) +
           pFileSize        (4) +
           FileName[0]      (1) +
           FileName[1]      (1) +
           (skip fdate)
           (skip ftime)
           filename bytes   (variable length) +
           null terminator  (1)
         */
        byte[] pathBytes = path.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(10 + pathBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(axoFileOpPckt);  // "AxoC" header
        buffer.putInt(0);           // pFileSize placeholder (4 bytes)
        buffer.put((byte)0x00);     // FileName[0] (always 0)
        buffer.put((byte)'C');      // FileName[1] (sub-command 'C')
        // buffer.putShort((short)0);  // fdate placeholder (2 bytes)
        // buffer.putShort((short)0);  // ftime placeholder (2 bytes)
        buffer.put(pathBytes);      // FileName[6]+ (variable length)
        buffer.put((byte)0x00);     // Null terminator

        // ClearSync();
        int writeResult = writeBytes(buffer.array());
        return writeResult;
    }

    @Override
    public int TransmitCreateDirectory(String filename, Calendar date) {

        /* Total size:
           "AxoC"           (4) +
           pFileSize        (4) +
           FileName[0]      (1) +
           FileName[1]      (1) +
           fdate            (2) +
           ftime            (2) +
           filename bytes   (variable length) +
           null terminator  (1)
         */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(14 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(axoFileOpPckt);  // "AxoC" header
        buffer.putInt(0);           // pFileSize placeholder (4 bytes)
        buffer.put((byte)0x00);     // FileName[0] (always 0)
        buffer.put((byte)'k');      // FileName[1] (sub-command 'k')

        /* Calculate FatFs date/time */
        int dy = date.get(Calendar.YEAR);
        int dm = date.get(Calendar.MONTH) + 1;
        int dd = date.get(Calendar.DAY_OF_MONTH);
        int th = date.get(Calendar.HOUR_OF_DAY);
        int tm = date.get(Calendar.MINUTE);
        int ts = date.get(Calendar.SECOND);
        short fatFsDate = (short)(((dy - 1980) << 9) | (dm << 5) | dd);
        short fatFsTime = (short)((th << 11) | (tm << 5) | (ts / 2));

        buffer.putShort(fatFsDate);  // FileName[2/3]
        buffer.putShort(fatFsTime);  // FileName[4/5]
        buffer.put(filenameBytes);   // FileName[6]+ (variable length)
        buffer.put((byte)0x00);      // Null terminator

        // ClearSync();
        int writeResult = writeBytes(buffer.array());
        return writeResult;
    }

    @Override
    public int TransmitAppendFile(byte[] buffer) {

        /* Total size:
           "Axoa"           (4) +
           Length           (4)
           (data is streamed in successive writeBytes(buffer))
         */
        int size = buffer.length;
        ByteBuffer headerBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN); // "Axoa" + length

        headerBuffer.put((byte)'A').put((byte)'x').put((byte)'o').put((byte)'a');
        headerBuffer.putInt(size);  // Length of the data chunk

        // ClearSync();
        int writeResult = writeBytes(headerBuffer.array());
        if (writeResult != LibUsb.SUCCESS) {
            return writeResult;
        }
        writeResult = writeBytes(buffer); /* Send the actual data payload */
        return writeResult;
    }

    @Override
    public int TransmitCloseFile(String filename, Calendar date) {

        /* Total size:
           "AxoC"           (4) +
           pFileSize        (4) +
           FileName[0]      (1) +
           FileName[1]      (1) +
           fdate            (2) +
           ftime            (2)
           filename bytes   (variable length) +
           null terminator  (1)
         */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(14 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(axoFileOpPckt);  // "AxoC" header
        buffer.putInt(0);           // pFileSize placeholder (4 bytes)
        buffer.put((byte)0x00);     // FileName[0] (always 0)
        buffer.put((byte)'c');      // FileName[1] (sub-command 'c')

        /* Calculate FatFs date/time */
        int dy = date.get(Calendar.YEAR);
        int dm = date.get(Calendar.MONTH) + 1;
        int dd = date.get(Calendar.DAY_OF_MONTH);
        int th = date.get(Calendar.HOUR_OF_DAY);
        int tm = date.get(Calendar.MINUTE);
        int ts = date.get(Calendar.SECOND);
        short fatFsDate = (short)(((dy - 1980) << 9) | (dm << 5) | dd);
        short fatFsTime = (short)((th << 11) | (tm << 5) | (ts / 2));

        buffer.putShort(fatFsDate);  // FileName[2/3]
        buffer.putShort(fatFsTime);  // FileName[4/5]
        buffer.put(filenameBytes);   // FileName[6]+ (variable length)
        buffer.put((byte)0x00);      // Null terminator

        ClearSync();
        int writeResult = writeBytes(buffer.array());
        return writeResult;
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
                // System.out.println(Instant.now() + " [DEBUG] rcv ppc objname:" + pi.GetObjectInstance().getInstanceName() + " pname:"+ pi.getName());
            }
        });

    }

    enum ReceiverState {

        header,
        ackPckt,                /* general acknowledge */
        paramchangePckt,        /* parameter changed */
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
        // System.out.println(Instant.now() + " [DEBUG] s=" + dataIndex + "  v=" + Integer.toHexString(packetData[dataIndex>>2]) + " c=" + Integer.toHexString(c));
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

    void processByte(byte cc) {

        int c = cc & 0xff;

        // String charDisplay;
        // if (c >= 0x20 && c <= 0x7E) {
        //     charDisplay = "'" + String.valueOf((char) c) + "'";
        // }
        // else {
        //     charDisplay = String.format("%02x", c) + "h"; // Show hex for non-printable characters
        // }
        // System.out.println(Instant.now() + " [DEBUG] processByte c=" + charDisplay + " s=" + state.name());

        switch (state) {

            case header:
                switch (headerstate) {
                    case 0: /* This should always be 'A' or command will be ignored */
                        if (c == 'A') {
                            headerstate = 1;
                        }
                        break;

                    case 1: /* This should always be 'x' or command will be ignored */
                        if (c == 'x') {
                            headerstate = 2;
                        }
                        else {
                            GoIdleState();
                        }
                        break;

                    case 2: /* This should always be 'o' or command will be ignored */
                        if (c == 'o') {
                            headerstate = 3;
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
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'Q'");
                                break;
                            case 'A':
                                state = ReceiverState.ackPckt;
                                dataIndex = 0;
                                dataLength = 24;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'A'");
                                break;
                            case 'D':
                                state = ReceiverState.displayPcktHdr;
                                dataIndex = 0;
                                dataLength = 8;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'D'");
                                break;
                            case 'R': /* New case for 'R' header (AxoR packet from MCU) */
                                state = ReceiverState.commandResultPckt;
                                dataIndex = 0;
                                dataLength = 2; /* Expecting command_byte (1 byte) + status_byte (1 byte) */
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'R'");
                                break;
                            case 'T':
                                state = ReceiverState.textPckt;
                                textRcvBuffer.clear();
                                dataIndex = 0;
                                dataLength = 255;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'T'");
                                break;
                            case 'l':
                                state = ReceiverState.sdinfo;
                                sdinfoRcvBuffer.rewind();
                                dataIndex = 0;
                                dataLength = 12;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'l'");
                                break;
                            case 'f':
                                state = ReceiverState.fileinfo_fixed_fields;
                                fileinfoRcvBuffer.clear();
                                dataIndex = 0;
                                dataLength = 8;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'f'");
                                break;
                            case 'r':
                                state = ReceiverState.memread;
                                memReadBuffer.clear();
                                dataIndex = 0;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'r'");
                                break;
                            case 'y':
                                state = ReceiverState.memread1word;
                                dataIndex = 0;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'y'");
                                break;
                            case 'V':
                                state = ReceiverState.fwversion;
                                dataIndex = 0;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'V'");
                                break;
                            default:
                                GoIdleState();
                                // System.err.println(Instant.now() + " [DEBUG] Error trying to complete headerstate after valid 'Axo'");
                                break;
                        }
                        break;

                    default:
                        // System.err.println(Instant.now() + " [DEBUG] processByte: invalid header");
                        GoIdleState();
                        break;
                }
                break;

            case paramchangePckt:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
                // System.out.println(Instant.now() + " [DEBUG] pch packet i=" +dataIndex + " v=" + c + " c="+ (char)(cc));
                if (dataIndex == dataLength) {
                    // System.out.println(Instant.now() + " [DEBUG] param packet complete 0x" + Integer.toHexString(packetData[1]) + "    0x" + Integer.toHexString(packetData[0]));
                    RPacketParamChange(packetData[2], packetData[1], packetData[0]);
                    GoIdleState();
                }
                break;

            case ackPckt:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
                if (dataIndex == dataLength) {
                    Acknowledge(packetData[0], packetData[1], packetData[2], packetData[3], packetData[4], packetData[5]);
                    GoIdleState();
                }
                break;

            case displayPcktHdr:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
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

            case commandResultPckt:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
                if (dataIndex == dataLength) { // Once both bytes are received
                    int commandByte = packetData[0] & 0xFF;
                    int statusCode = (packetData[0] >> 8) & 0xFF;

                    // System.out.println(Instant.now() + " [DEBUG] AxoR received for '" + (char)commandByte + "': Status = " + SDCardInfo.getFatFsErrorString(statusCode));

                    if (currentExecutingCommand != null) {
                        // Special handling for QCmdUploadFile's sub-commands
                        if (currentExecutingCommand instanceof QCmdUploadFile) {
                            QCmdUploadFile uploadCmd = (QCmdUploadFile) currentExecutingCommand;
                            if (commandByte == 'f') {
                                uploadCmd.setCreateFileCompleted((byte)statusCode);
                            }
                            else if (commandByte == 'a') {
                                uploadCmd.setAppendFileCompleted((byte)statusCode);
                            }
                            else if (commandByte == 'c') {
                                uploadCmd.setCloseFileCompleted((byte)statusCode);
                            }
                            else {
                                // System.err.println(Instant.now() + " [DEBUG] Warning: QCmdUploadFile received unexpected AxoR for command: " + (char)commandByte);
                            }
                        }
                        // Handling for other commands that expect an AxoR for their completion
                        else if (currentExecutingCommand instanceof QCmdGetFileList ||
                                 currentExecutingCommand instanceof QCmdCreateDirectory ||
                                 currentExecutingCommand instanceof QCmdChangeWorkingDirectory ||
                                 currentExecutingCommand instanceof QCmdDeleteFile ||
                                 currentExecutingCommand instanceof QCmdGetFileInfo) {

                            if (currentExecutingCommand.getExpectedAckCommandByte() == commandByte) { // for example, ('l' == 'l') -> TRUE
                                currentExecutingCommand.setMcuStatusCode((byte)statusCode);
                                currentExecutingCommand.setCommandCompleted(statusCode == 0x00);
                            }
                            else {
                                // System.err.println(Instant.now() + " [DEBUG] Warning: currentExecutingCommand (" + currentExecutingCommand.getClass().getSimpleName() + ") received unexpected AxoR for command: " + (char)commandByte + ". Expected: " + currentExecutingCommand.getExpectedAckCommandByte() + ". Ignoring.");
                            }
                        }
                        // Generic handling for all other single-step QCmdSerialTasks
                        else {
                            // System.err.println(Instant.now() + " [DEBUG] Warning: currentExecutingCommand (" + currentExecutingCommand.getClass().getSimpleName() + ") received an AxoR for command: " + (char)commandByte + ", but this command does not expect an AxoR for completion. Ignoring.");
                        }
                    }
                    else {
                        // System.err.println(Instant.now() + " [DEBUG] Warning: AxoR received but no currentExecutingCommand is set.");
                    }
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
                        /* Filter out error if SD card is connected but no start.bin is found */
                        LOGGER.log(Level.INFO, "SD card connected, no startup patch found.");
                    }
                    else {
                        LOGGER.log(Level.WARNING, "{0}", textRcvBuffer.toString());
                    }
                    // System.out.println(Instant.now() + " [DEBUG] FINAL MCU Text Message (AxoT): " + textRcvBuffer.toString());
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

                    int clusters = sdinfoRcvBuffer.getInt();
                    int clustersize = sdinfoRcvBuffer.getInt();
                    int blocksize = sdinfoRcvBuffer.getInt();
                    // System.out.println(Instant.now() +  " [DEBUG] processByte sdinfo: clusters:" + clusters + " clsize:" + clustersize + " blsize:" + blocksize);

                    SDCardInfo.getInstance().SetInfo(clusters, clustersize, blocksize);
                    GoIdleState();
                }
                break;

                case fileinfo_fixed_fields: /* State to collect the 8-byte size and timestamp */
                fileinfoRcvBuffer.put(cc);
                dataIndex++;
        
                if (dataIndex == dataLength) { /* exactly 8 bytes (size + timestamp) */ 
                    // System.out.println(Instant.now() + " [DEBUG] processByte: Received fixed fields for Axof. Processing.");
        
                    fileinfoRcvBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    fileinfoRcvBuffer.limit(fileinfoRcvBuffer.position());
                    fileinfoRcvBuffer.rewind();
        
                    /* Read the 4-byte size and 4-byte timestamp */
                    currentFileSize = fileinfoRcvBuffer.getInt();
                    currentFileTimestamp = fileinfoRcvBuffer.getInt();
                    // System.out.println(Instant.now() + " [DEBUG] processByte: Parsed preliminary size: " + currentFileSize + ", timestamp: " + currentFileTimestamp);
        
                    /* Prepare to collect the variable-length filename */
                    fileinfoRcvBuffer.clear();
                    dataIndex = 0;
                    state = ReceiverState.fileinfo_filename; /* Transition to the next sub-state */
                }
                break;
        
            case fileinfo_filename: /* State to collect filename bytes until null terminator */
                if (cc == 0x00) {
                    // System.out.println(Instant.now() + " [DEBUG] processByte: Null terminator found for Axof filename. Processing.");
        
                    fileinfoRcvBuffer.limit(fileinfoRcvBuffer.position());
                    fileinfoRcvBuffer.rewind();
        
                    /* Get the collected filename bytes as an array */
                    byte[] filenameBytes = new byte[fileinfoRcvBuffer.remaining()];
                    fileinfoRcvBuffer.get(filenameBytes);
                    String fname = new String(filenameBytes, Charset.forName("ISO-8859-1"));

                    SDCardInfo.getInstance().AddFile(fname, currentFileSize, currentFileTimestamp);
                    // System.out.println(Instant.now() + " [DEBUG] processByte: Parsed file: \'" + fname + "\', size: " + currentFileSize + ", timestamp: " + currentFileTimestamp);
        
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
                            // System.out.println(Instant.now() + " [DEBUG] memread offset 0x" + Integer.toHexString(memReadAddr));
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
                        // System.out.println(Instant.now() + " [DEBUG] " + String.format("addr %08X value %08X", memReadAddr, memReadValue));
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
                        System.out.println(Instant.now() + String.format(" Firmware version: %d.%d.%d.%d, CRC: 0x%s, entry point: 0x%08X", fwversion[0], fwversion[1], fwversion[2], fwversion[3], sFwcrc, patchentrypoint));
                        LOGGER.log(Level.INFO, String.format("Firmware version %d.%d.%d.%d | CRC %s\n", fwversion[0], fwversion[1], fwversion[2], fwversion[3], sFwcrc));
                        MainFrame.mainframe.setFirmwareID(sFwcrc);
                        GoIdleState();
                        break;
                }
                dataIndex++;
                break;

            default:
                GoIdleState();
                // System.out.println(Instant.now() + " [DEBUG] Unhandled byte c=" + String.format("%02x", c) + "(char=" + (char)c + ") in state=" + state);
                break;
        }
    }

    @Override
    public ksoloti_core getTargetProfile() {
        return targetProfile;
    }

}
