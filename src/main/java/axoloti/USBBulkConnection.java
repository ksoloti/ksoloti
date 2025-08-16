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

import static axoloti.dialogs.USBPortSelectionDlg.ErrorString;

import axoloti.dialogs.USBPortSelectionDlg;
import axoloti.displays.DisplayInstance;
import axoloti.parameters.ParameterInstance;
import axoloti.sd.SDCardInfo;
import axoloti.targetprofile.ksoloti_core;
import axoloti.usb.Usb;
import axoloti.utils.Preferences;

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
import qcmds.QCmdCopyPatchToFlash;
import qcmds.QCmdCreateDirectory;
import qcmds.QCmdDeleteFile;
import qcmds.QCmdGetFileInfo;
import qcmds.QCmdGetFileList;
import qcmds.QCmdMemRead;
import qcmds.QCmdMemRead1Word;
import qcmds.QCmdProcessor;
import qcmds.QCmdSerialTask;
import qcmds.QCmdStart;
import qcmds.QCmdStop;
import qcmds.QCmdTransmitGetFWVersion;
import qcmds.QCmdUploadFWSDRam;
import qcmds.QCmdUploadFile;
import qcmds.QCmdUploadPatch;

/**
 *
 * @author Johannes Taelman
 */
public class USBBulkConnection extends Connection {

    private static final Logger LOGGER = Logger.getLogger(USBBulkConnection.class.getName());

    private Patch patch;
    private volatile boolean disconnectRequested;
    private volatile boolean isConnecting;
    private volatile boolean connected;
    private volatile Thread transmitterThread;
    private volatile Thread receiverThread;
    private final BlockingQueue<QCmdSerialTask> queueSerialTask;
    private String targetCpuId;
    private String detectedCpuId;
    private ksoloti_core targetProfile;
    private final Context context;
    private volatile DeviceHandle handle;
    static private volatile USBBulkConnection conn = null;

    ByteBuffer dispData;

    final Sync sync;
    final Sync readsync;

    private Boolean isSDCardPresent = null;
    private int connectionFlags = 0;

    int CpuId0 = 0;
    int CpuId1 = 0;
    int CpuId2 = 0;
    int fwcrc = -1;
    int temp_fwcrc = -1;

    private final byte[] startPckt =        new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('s')};
    private final byte[] stopPckt =         new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('S')};
    private final byte[] pingPckt =         new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('p')};
    private final byte[] getFileListPckt =  new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('l')};
    private final byte[] copyToFlashPckt =  new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('F')};
    private final byte[] axoFileOpPckt =    new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('C')};
    private final byte[] axoMemWritePckt =  new byte[]{(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('W')};

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

    private final static Pattern sdFoundNoStartupPattern = Pattern.compile("File error:.*filename:\"/start.bin\"");

    enum ReceiverState {
        HEADER,
        ACK_PCKT,               /* general acknowledge */
        PARAMCHANGE_PCKT,       /* parameter changed */
        DISPLAY_PCKT_HDR,       /* object display readbac */
        DISPLAY_PCKT,           /* object display readback */
        TEXT_PCKT,              /* text message to display in log */
        SDINFO,                 /* sdcard info */
        FILEINFO_FIXED_FIELDS,  /* file listing entry, size and timestamp (8 bytes of Axof packet) */
        FILEINFO_FILENAME,      /* file listing entry, variable length filename */
        MEMREAD,                /* one-time programmable bytes */
        MEMREAD_1WORD,          /* one-time programmable bytes */
        FWVERSION,              /* responds with own firmware version, 1.1.0.0 (though not used for anything?) */
        COMMANDRESULT_PCKT      /* New Response Packet: ['A', 'x', 'o', 'R', command_byte, status_byte] */
    };

    /*
     * Protocol documentation:
     * "AxoP" + bb + vvvv -> parameter change index bb (16bit), value vvvv (32bit)
     */
    private ReceiverState state = ReceiverState.HEADER;
    private int headerstate;
    private int[] packetData = new int[64];
    private int dataIndex = 0;  /* in bytes */
    private int dataLength = 0; /* in bytes */
    private CharBuffer textRcvBuffer = CharBuffer.allocate(256);
    private ByteBuffer sdinfoRcvBuffer = ByteBuffer.allocate(12);
    private ByteBuffer fileinfoRcvBuffer = ByteBuffer.allocate(256);
    private ByteBuffer memReadBuffer = ByteBuffer.allocate(16 * 4);
    private int memReadAddr;
    private int memReadLength;
    private int memReadValue;
    private byte[] fwversion = new byte[4];
    private int patchentrypoint;

    class Sync {
        boolean Acked = false;
    }

    class Receiver implements Runnable {
        @Override
        public void run() {
            ByteBuffer recvbuffer = ByteBuffer.allocateDirect(4096);
            IntBuffer transfered = IntBuffer.allocate(1);

            // System.out.println(Instant.now() + " [DEBUG] Receiver thread started.");
            while (!Thread.currentThread().isInterrupted() && !disconnectRequested) {
                int result = LibUsb.SUCCESS;
                int sz = 0;
                recvbuffer.clear();
                transfered.clear();

                try {
                    synchronized (usbInLock) {
                        if (handle == null) {
                            // System.err.println(Instant.now() + " [DEBUG] Receiver: USB handle became null while waiting for lock. Initiating central disconnect.");
                            disconnect();
                            break; /* Exit the loop */
                        }

                        result = LibUsb.bulkTransfer(handle, (byte) IN_ENDPOINT, recvbuffer, transfered, 100);
                        sz = transfered.get(0);

                        /* Check interrupted status immediately after a blocking call returns */
                        if (Thread.currentThread().isInterrupted()) {
                            // System.out.println(Instant.now() + " [DEBUG] Receiver: Thread interrupted after bulkTransfer. Exiting loop.");
                            break;
                        }
                        if (disconnectRequested) {
                            // System.out.println(Instant.now() + " [DEBUG] Receiver: Disconnect requested after bulkTransfer. Exiting loop.");
                            break;
                        }

                        if (result != LibUsb.SUCCESS) {
                            // System.err.println(Instant.now() + " [DEBUG] Receiver: LibUsb.bulkTransfer returned error: " + result + " (" + LibUsb.strError(result) + ")");
                            if (result == LibUsb.ERROR_NO_DEVICE || result == LibUsb.ERROR_PIPE || result == LibUsb.ERROR_IO || result == LibUsb.ERROR_INTERRUPTED) {
                                // System.err.println(Instant.now() + " [DEBUG] Receiver: Critical LibUsb error detected. Initiating USB disconnect.");
                                disconnect();
                                break; /* Exit the loop */
                            }
                        }
                    } /* end synchronized (usbInLock) */

                    if (result == LibUsb.SUCCESS && sz > 0) {
                        recvbuffer.position(0);
                        recvbuffer.limit(sz);
                        for (int i = 0; i < sz; i++) {
                            if (Thread.currentThread().isInterrupted()) {
                                // System.err.println(Instant.now() + " [DEBUG] Receiver: Thread interrupted during byte processing. Aborting chunk.");
                                break;
                            }
                            if (disconnectRequested) {
                                // System.err.println(Instant.now() + " [DEBUG] Receiver: Disconnect requested during byte processing. Aborting chunk.");
                                break;
                            }
                            processByte(recvbuffer.get(i));
                        }
                    }
                }
                catch (LibUsbException e) {
                    // System.err.println(Instant.now() + " [DEBUG] Receiver: LibUsbException: " + e.getMessage());
                    disconnect();
                    break;
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error during Receiver thread: " + e.getMessage());
                    e.printStackTrace(System.err);
                    disconnect();
                    break;
                }
            }
            // System.out.println(Instant.now() + " [DEBUG] Receiver thread exiting gracefully.");
        }
    }

    class Transmitter implements Runnable {
        @Override
        public void run() {
            // System.out.println(Instant.now() + " [DEBUG] Transmitter thread started.");
            while (!Thread.currentThread().isInterrupted() && !disconnectRequested) {
                QCmdSerialTask cmd = null;
                try {
                    cmd = queueSerialTask.poll(5, TimeUnit.SECONDS);

                    if (Thread.currentThread().isInterrupted()) {
                        // System.out.println(Instant.now() + " [DEBUG] Transmitter: Thread interrupted after taking task. Exiting loop.");
                        break;
                    }
                    if (disconnectRequested) {
                        // System.out.println(Instant.now() + " [DEBUG] Transmitter: Disconnect requested after taking task.");
                        break;
                    }

                    if (cmd != null) {
                        QCmd response = cmd.Do(USBBulkConnection.this);
                        if (response != null) {
                            QCmdProcessor.getQCmdProcessor().getQueueResponse().put(response);
                        }
                    }
                }
                catch (InterruptedException ex) {
                    // System.out.println(Instant.now() + " [DEBUG] Transmitter: InterruptedException caught from queue.poll(). Exiting loop.");
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erropr during Transmitter thread: " + e.getMessage());
                    e.printStackTrace(System.err);
                    disconnect();
                    break;
                }
            }
            // System.out.println(Instant.now() + " [DEBUG] Transmitter thread exiting gracefully.");
        }
    }

	protected USBBulkConnection() {
        this.sync = new Sync();
        this.readsync = new Sync();
        this.patch = null;

        disconnectRequested = false;
        isConnecting = false;
        connected = false;
        queueSerialTask = new ArrayBlockingQueue<QCmdSerialTask>(99);
        this.context = Usb.getContext();
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
    public void setDisconnectRequested(boolean requested) {
        this.disconnectRequested = requested;
    }

    @Override
    public boolean isDisconnectRequested() {
        return disconnectRequested;
    }

    @Override
    public boolean isConnected() {
        return connected && (!disconnectRequested);
    }

    @Override
    public boolean AppendToQueue(QCmdSerialTask cmd) {
        try {
            // if (!(cmd instanceof QCmdPing)) {
            //     System.out.println(Instant.now() + " [DEBUG] AppendToQueue: attempting to append " + cmd.getClass().getSimpleName());
            // }
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
        // System.out.println(Instant.now() + " [DEBUG] Disconnect called. Initiating cleanup.");

        /* Guard against redundant calls */
        if (this.disconnectRequested && !connected) {
            // System.out.println(Instant.now() + " [DEBUG] Disconnect already in progress/requested and not connected. Aborting redundant call.");
            return;
        }

        /* 1. Set flag to signal threads to stop */
        this.disconnectRequested = true;

        /* 2. Clear the queue of tasks for the Transmitter thread */
        if (queueSerialTask != null) {
            queueSerialTask.clear();
            // System.out.println(Instant.now() + " [DEBUG] Disconnect: Cleared queueSerialTask.");
        }

        try {
            /* 3. Interrupt the Receiver thread */
            if (receiverThread != null && receiverThread.isAlive()) {
                // System.out.println(Instant.now() + " [DEBUG] Disconnect: Interrupting Receiver thread.");
                receiverThread.interrupt();
            }

            /* 4. Interrupt the Transmitter thread */
            if (transmitterThread != null && transmitterThread.isAlive()) {
                // System.out.println(Instant.now() + " [DEBUG] Disconnect: Interrupting Transmitter thread.");
                transmitterThread.interrupt();
            }

            /* 5. Wait for threads to terminate */
            long threadJoinTimeoutMs = 5000;

            if (receiverThread != null && receiverThread.isAlive()) {
                try {
                    // System.out.println(Instant.now() + " [DEBUG] Disconnect: Waiting for Receiver thread to join (timeout: " + threadJoinTimeoutMs + "ms).");
                    receiverThread.join(threadJoinTimeoutMs);
                    if (receiverThread.isAlive()) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: Receiver thread did not terminate within timeout.");
                    }
                    // else {
                    //     System.out.println(Instant.now() + " [DEBUG] Disconnect: Receiver thread joined successfully.");
                    // }
                }
                catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Error trying to close Receiver thread: " + e.getMessage());
                    e.printStackTrace(System.err);
                    Thread.currentThread().interrupt();
                }
                finally {
                    receiverThread = null;
                }
            }

            if (transmitterThread != null && transmitterThread.isAlive()) {
                try {
                    // System.out.println(Instant.now() + " [DEBUG] Disconnect: Waiting for Transmitter thread to join (timeout: " + threadJoinTimeoutMs + "ms).");
                    transmitterThread.join(threadJoinTimeoutMs);
                    if (transmitterThread.isAlive()) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: Transmitter thread did not terminate within timeout.");
                    }
                    // else {
                    //     System.out.println(Instant.now() + " [DEBUG] Disconnect: Transmitter thread joined successfully.");
                    // }
                }
                catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Error trying to close Transmitter thread: " + e.getMessage());
                    e.printStackTrace(System.err);
                    Thread.currentThread().interrupt();
                }
                finally {
                    transmitterThread = null;
                }
            }

            /* 6. Perform USB resource cleanup (only AFTER threads are confirmed stopped or timed out) */
            if (handle != null) {
                try {
                    // System.out.println(Instant.now() + " [DEBUG] Attempting to reset USB device using active handle.");

                    /* Calling resetDevice is a bit "risky" but so far has been improving stability a lot
                       especially during repeated disconnects and re-connects. */
                    int resetResult = LibUsb.resetDevice(handle);
                    if (resetResult != LibUsb.SUCCESS) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: Error resetting device: " + LibUsb.strError(resetResult) + " (Code: " + resetResult + ")");
                    }
                    // else {
                    //     System.out.println(Instant.now() + " [DEBUG] USB device reset successfully. Device may re-enumerate.");
                    // }

                }
                catch (LibUsbException resetEx) {
                    System.err.println(Instant.now() + " [ERROR] Disconnect: LibUsbException during device reset: " + resetEx.getMessage());
                }

                try {
                    // System.out.println(Instant.now() + " [DEBUG] Attempting to release USB interface " + useBulkInterfaceNumber);
                    LibUsb.releaseInterface(handle, useBulkInterfaceNumber);
                    // System.out.println(Instant.now() + " [DEBUG] USB interface released successfully.");
                }
                catch (LibUsbException releaseEx) {
                    System.err.println(Instant.now() + " [ERROR] Disconnect: Error releasing interface (may be normal after reset): " + releaseEx.getMessage());
                }

                try {
                    // System.out.println(Instant.now() + " [DEBUG] Attempting to close USB device handle.");
                    LibUsb.close(handle);
                    handle = null; /* Null immediately to prevent race conditions */
                    // System.out.println(Instant.now() + " [DEBUG] USB device handle closed successfully.");
                }
                catch (LibUsbException closeEx) {
                    System.err.println(Instant.now() + " [ERROR] Disconnect: Error closing handle (may be normal after reset): " + closeEx.getMessage());
                }
                finally {
                    handle = null; /* Should already be null but just to be sure */
                }
            }
            else {
                // System.out.println(Instant.now() + " [DEBUG] No USB device handle to close (it was null).");
            }
        }
        catch (Exception mainEx) {
            LOGGER.log(Level.SEVERE, "Unexpected error during Disconnect cleanup: " + mainEx.getMessage());
            mainEx.printStackTrace(System.err);
        }
        finally {

            /* 7. Reset state variables - always reset these, regardless of cleanup success */
            connected = false;
            detectedCpuId = null;
            isSDCardPresent = null;
            CpuId0 = 0;
            CpuId1 = 0;
            CpuId2 = 0;
            isConnecting = false;

            /* 8. Notify UI - always notify UI after cleanup attempt */
            ShowDisconnect();
            LOGGER.log(Level.WARNING, "Disconnected\n");

            /* 9. Clear `disconnectRequested` flag LAST, after all cleanup and UI updates */
            this.disconnectRequested = false;
            // System.out.println(Instant.now() + " [DEBUG] Disconnect process completed. Disconnect request flag cleared.");
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
            /* First pass: look for a specific device by serial number if targetCpuId is set */
            if (targetCpuId != null) {
                for (Device d : list) {
                    DeviceHandle h = tryOpenAndMatchDevice(d, true);
                    if (h != null) {
                        return h;
                    }
                }
            }

            /* Second pass: if no specific device, or targetCpuId is null, pick the first matching device */
            for (Device d : list) {
                DeviceHandle h = tryOpenAndMatchDevice(d, false);
                if (h != null) {
                    return h;
                }
            }
        }
        finally {
            LibUsb.freeDeviceList(list, true);
        }

        LOGGER.log(Level.WARNING, "No matching USB devices found.");
        return null;
    }

    private DeviceHandle tryOpenAndMatchDevice(Device d, boolean checkSerialNumber) {
        DeviceDescriptor descriptor = new DeviceDescriptor();
        int result = LibUsb.getDeviceDescriptor(d, descriptor);
        if (result != LibUsb.SUCCESS) {
            // LOGGER.log(Level.WARNING, "Unable to read device descriptor: " + LibUsb.strError(result));
            return null;
        }

        boolean isKsolotiMode = Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core");
        boolean isAxolotiMode = Preferences.getInstance().getFirmwareMode().contains("Axoloti Core");

        /* Check Vendor ID first */
        if (descriptor.idVendor() != bulkVID) {
            return null;
        }

        short expectedNormalPID;
        short expectedUsbAudioPID;
        String deviceType;

        if (isKsolotiMode) {
            expectedNormalPID = bulkPIDKsoloti;
            expectedUsbAudioPID = bulkPIDKsolotiUsbAudio;
            deviceType = "Ksoloti Core";
        }
        else if (isAxolotiMode) {
            expectedNormalPID = bulkPIDAxoloti;
            expectedUsbAudioPID = bulkPIDAxolotiUsbAudio;
            deviceType = "Axoloti Core";
        }
        else {
            return null; /* Neither mode matches */
        }

        if (descriptor.idProduct() == expectedNormalPID || descriptor.idProduct() == expectedUsbAudioPID) {
            if (descriptor.idProduct() == expectedUsbAudioPID) {
                useBulkInterfaceNumber = 4;
                LOGGER.log(Level.INFO, "{0} USBAudio found.", deviceType);
            }
            else {
                useBulkInterfaceNumber = 2;
                LOGGER.log(Level.INFO, "{0} found.", deviceType);
            }

            DeviceHandle h = new DeviceHandle();
            result = LibUsb.open(d, h);
            if (result < 0) {
                LOGGER.log(Level.INFO, ErrorString(result));
                return null;
            }
            else {
                if (checkSerialNumber) {
                    String serial = LibUsb.getStringDescriptor(h, descriptor.iSerialNumber());
                    if (targetCpuId != null && serial != null && serial.equals(targetCpuId)) {
                        return h; /* Found the specific device */
                    }
                    LibUsb.close(h);
                    h = null; /* Null immediately to prevent race conditions */
                    return null;
                }
                else {
                    return h;
                }
            }
        }
        return null;
    }

    @Override
    public boolean connect() {
        /* 1. Initial State Check: Prevent overlapping operations */
        if (disconnectRequested) {
            // System.out.println(Instant.now() + " [DEBUG] Connection attempt aborted: A disconnection is still in progress.");
            return false;
        }

        // disconnect(); // Trigger a full disconnect for cleanup first?
        setIdleState();

        /* Update UI/internal state immediately */
        this.connected = false;

        /* Internal 'isConnecting' flag to prevent multiple concurrent SwingWorkers */
        if (isConnecting) {
            return false;
        }
        isConnecting = true;

        /* Try to get targetCpuId if not already set */
        if (targetCpuId == null) {
            targetCpuId = Preferences.getInstance().getComPortName();
        }
        targetProfile = new ksoloti_core();

        try {
            /* 2. Open Device Handle */
            handle = OpenDeviceHandle();
            if (handle == null) {
                System.err.println(Instant.now() + " [ERROR] Connect: Failed to open USB device handle.");
                disconnect();
                return false;
            }
            // System.out.println(Instant.now() + " [DEBUG] Connect: USB device handle opened successfully.");

            /* 3. Claim Interface */
            // System.out.println(Instant.now() + " [DEBUG] Connect: Attempting to claim interface " + useBulkInterfaceNumber);
            int result = LibUsb.claimInterface(handle, useBulkInterfaceNumber);
            if (result != LibUsb.SUCCESS) {
                System.err.println(Instant.now() + " [ERROR] Connect: Failed to claim interface " + useBulkInterfaceNumber + ": " + LibUsb.errorName(result) + " (Code: " + result + ")");
                disconnect();
                return false;
            }
            // System.out.println(Instant.now() + " [DEBUG] Connect: USB interface " + useBulkInterfaceNumber + " claimed successfully.");

            setIdleState();

            /* 4. Start Receiver and Transmitter Threads */
            receiverThread = new Thread(new Receiver());
            receiverThread.setName("Receiver");
            receiverThread.start();

            transmitterThread = new Thread(new Transmitter());
            transmitterThread.setName("Transmitter");
            transmitterThread.start();

            /* 5. Initial Communication and Synchronization */
            ClearSync();
            TransmitPing();

            if (!WaitSync()) {
                LOGGER.log(Level.SEVERE, "Core not responding - firmware may be stuck running a problematic patch?\nRestart the Core and try again.");
                disconnect();
                return false;
            }

            /* If we reach here, initial handshake is successful */
            this.connected = true;
            LOGGER.log(Level.WARNING, "Connected\n");

            /* 6. Post-Connection Commands (CPU ID, Firmware Version) */
            try {
                QCmdProcessor.getQCmdProcessor().AppendToQueue(new QCmdTransmitGetFWVersion());
                QCmdProcessor.getQCmdProcessor().WaitQueueFinished();

                QCmdMemRead1Word q1 = new QCmdMemRead1Word(targetProfile.getCPUIDCodeAddr());
                QCmdProcessor.getQCmdProcessor().AppendToQueue(q1);
                QCmdProcessor.getQCmdProcessor().WaitQueueFinished();
                targetProfile.setCPUIDCode(q1.getResult());

                QCmdMemRead q = new QCmdMemRead(targetProfile.getCPUSerialAddr(), targetProfile.getCPUSerialLength());
                QCmdProcessor.getQCmdProcessor().AppendToQueue(q);
                QCmdProcessor.getQCmdProcessor().WaitQueueFinished();
                targetProfile.setCPUSerial(q.getResult());
                this.detectedCpuId = CpuIdToHexString(targetProfile.getCPUSerial());
                // System.out.println(Instant.now() + " [DEBUG] USBBulkConnection: detectedCpuId set to: " + this.detectedCpuId);
            }
            catch (Exception cmdEx) {
                LOGGER.log(Level.SEVERE, "Error during post-connection QCmd processing. Connection might be unstable: " + cmdEx.getMessage());
                cmdEx.printStackTrace(System.err);
                return false;
            }

            ShowConnect();
            isConnecting = false;
            return true;
        }
        catch (LibUsbException e) {
            System.err.println(Instant.now() + " [ERROR] LibUsb exception during connection: " + e.getMessage());
            disconnect();
            return false;
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during connection process: " + ex.getMessage());
            ex.printStackTrace(System.err);
            disconnect();
            return false;
        }
        finally {
            isConnecting = false;
        }
    }

    @Override
    public int writeBytes(byte[] bytes) {

        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.rewind();
        IntBuffer transfered = IntBuffer.allocate(1);

        /* Acquire the OUT lock for writing */
        synchronized (usbOutLock) {
            if (handle == null) {
                // System.err.println(Instant.now() + " [DEBUG] USB bulk write failed: handle is null. Disconnected?");
                return LibUsb.ERROR_NO_DEVICE;
            }

            int result = LibUsb.bulkTransfer(handle, (byte) OUT_ENDPOINT, buffer, transfered, 3000);
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
                // QCmdProcessor.getQCmdProcessor().Abort();
            }
            return result;
        } /* end synchronize (usbOutLock) */
    }

    @Override
    public void SelectPort() {
        USBPortSelectionDlg spsDlg = new USBPortSelectionDlg(null, true, targetCpuId);
        spsDlg.setVisible(true);
        targetCpuId = spsDlg.getCPUID();
        String name = Preferences.getInstance().getBoardName(targetCpuId);
        if (targetCpuId == null) return;
        if (name == null) {
            LOGGER.log(Level.INFO, "Selecting CPU ID {0} for connection.", targetCpuId);
        }
        else {
            LOGGER.log(Level.INFO, "Selecting \"{0}\" for connection.", new Object[]{name});
        }
    }

    public static Connection GetConnection() {
        if (conn == null) {
            synchronized (USBBulkConnection.class) {
                if (conn == null) {
                    conn = new USBBulkConnection();
                }
            }
        }
        return conn;
    }

    @Override
    public String getTargetCpuId() {
        return this.targetCpuId;
    }

    @Override
    public String getDetectedCpuId() {
        return this.detectedCpuId;
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
                sync.Acked = false;
                return true;
            }
            try {
                if (disconnectRequested) {
                    // System.out.println(Instant.now() + " [DEBUG] WaitSync: Disconnect requested, not waiting.");
                    return false;
                }
                sync.wait(msec);
            }
            catch (InterruptedException ex) {
                // System.out.println(Instant.now() + " [DEBUG] Sync wait interrupted due to disconnect request.," + ex.getMessage());
                Thread.currentThread().interrupt();
                return false;
            }
            return sync.Acked;
        }
    }

    @Override
    public boolean WaitSync() {
        return WaitSync(3000);
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
                if (disconnectRequested) {
                    // System.out.println(Instant.now() + " [DEBUG] WaitReadSync: Disconnect requested, not waiting.");
                    return false;
                }
                readsync.wait(3000);
            }
            catch (InterruptedException ex) {
                // System.out.println(Instant.now() + " [DEBUG] ReadSync wait interrupted due to disconnect request.," + ex.getMessage());
                Thread.currentThread().interrupt();
                return false;
            }
            return readsync.Acked;
        }
    }

    @Override
    public int TransmitStart() {

        /* Total size (bytes):
           "Axos"           (4) +
           uUIMidiCost      (2) +
           uDspLimit200     (1)
         */
        ByteBuffer buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);
        short uUIMidiCost = Preferences.getInstance().getUiMidiThreadCost();
        byte  uDspLimit200 = (byte)(Preferences.getInstance().getDspLimitPercent()*2);

        buffer.put(startPckt);          // "Axos" header
        buffer.putShort(uUIMidiCost);   // MIDI cost 
        buffer.put(uDspLimit200);       // DSP limit

        int writeResult = writeBytes(buffer.array());
        return writeResult;
    }

    @Override
    public int TransmitStop() {
        int writeResult = writeBytes(stopPckt);
        return writeResult;
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
        synchronized (usbOutLock) {
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
    }

    @Override
    public int TransmitGetFileList() {
        // ClearSync();
        int writeResult = writeBytes(getFileListPckt);
        return writeResult;
    }

    @Override
    public int TransmitGetFileInfo(String filename) {

        /* Total size (bytes):
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
    public int TransmitCopyToFlash() {
        int writeResult = writeBytes(copyToFlashPckt);
        return writeResult;
    }

    @Override
    public int TransmitUploadFragment(byte[] buffer, int offset) {
        synchronized (usbOutLock) {
            byte[] data = new byte[12];
            data[0] = 'A';
            data[1] = 'x';
            data[2] = 'o';
            data[3] = '%'; // TODO: set to something unused until UploadFW command is reworked to the new AxoR response system
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

            int result = writeBytes(data);
            result |= writeBytes(buffer);
            LOGGER.log(Level.INFO, "Block uploaded @ 0x{0} length {1}",
                    new Object[]{Integer.toHexString(offset).toUpperCase(),
                    Integer.toString(buffer.length)});
            return result;
        }
    }

    @Override
    public int TransmitStartMemWrite(int startAddr, int totalLen) {

        /* Total size (bytes):
           "AxoW"           (4) +
           start address    (4) +
           Total length     (4) +
           sub-command      (1)     // 'W'
         */
        ByteBuffer buffer = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(axoMemWritePckt);  // "AxoW" header
        buffer.putInt(startAddr);     // memory start address   (4 bytes)
        buffer.putInt(totalLen);      // total write size (not chunk) (4 bytes)
        buffer.put((byte)'W');        // sub-command 'W'

        int writeResult = writeBytes(buffer.array());
        return writeResult;
    }

    @Override
    public int TransmitAppendMemWrite(byte[] buffer) {
        /* Total size (bytes):
           "Axow"           (4) +
           Length           (4)
           (data is streamed in successive writeBytes(buffer))
         */
        synchronized (usbOutLock) {
            int size = buffer.length;
            ByteBuffer headerBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN); // "Axow" + length

            headerBuffer.put((byte)'A').put((byte)'x').put((byte)'o').put((byte)'w');
            headerBuffer.putInt(size);  // Length of the data chunk

            int writeResult = writeBytes(headerBuffer.array());
            if (writeResult != LibUsb.SUCCESS) {
                return writeResult;
            }
            writeResult = writeBytes(buffer); /* Send the actual data payload */
            return writeResult;
        }
    }

    @Override
    public int TransmitCloseMemWrite(int startAddr, int totalLen) {
        /* Total size (bytes):
           "AxoW"           (4) +
           start address    (4) +
           Total length     (4) +
           sub-command      (1)     // 'c'
         */
        ByteBuffer buffer = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(axoMemWritePckt);  // "AxoW" header
        buffer.putInt(startAddr);     // memory start address   (4 bytes)
        buffer.putInt(totalLen);      // total write size (not chunk) (4 bytes)
        buffer.put((byte)'c');        // sub-command 'c'

        int writeResult = writeBytes(buffer.array());
        return writeResult;
    }

    @Override
    public int TransmitCreateFile(String filename, int size, Calendar date) {

        /* Total size (bytes):
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

        /* Total size (bytes):
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

        /* Total size (bytes):
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

        /* Total size (bytes):
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

        /* Total size (bytes):
           "Axoa"           (4) +
           Length           (4)
           (data is streamed in successive writeBytes(buffer))
         */
        synchronized (usbOutLock) {
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
    }

    @Override
    public int TransmitCloseFile(String filename, Calendar date) {

        /* Total size (bytes):
           "AxoC"           (4) +
           pFileSize        (4) +
           FileName[0]      (1) +
           FileName[1]      (1) +
           fdate            (2) +
           ftime            (2)
           filename bytes   (variable length) +
           null terminator  (1)
         */
        synchronized (usbOutLock) {
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

            // ClearSync();
            int writeResult = writeBytes(buffer.array());
            return writeResult;
        }
    }

    @Override
    public void TransmitMemoryRead(int addr, int length) {
        synchronized (usbOutLock) {
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
    }

    @Override
    public void TransmitMemoryRead1Word(int addr) {
        synchronized (usbOutLock) {
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
                    boolean dspOverload = 0 != (ConnectionFlags & 1);
                    patch.UpdateDSPLoad(DSPLoad, dspOverload);
                }
                MainFrame.mainframe.showPatchIndex(patchIndex);
                targetProfile.setVoltages(Voltages);
                SetSDCardPresent(sdcardPresent != 0);
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

    public static String CpuIdToHexString(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        ByteBuffer duplicateBuffer = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        StringBuilder sb = new StringBuilder();
        duplicateBuffer.rewind();
        while (duplicateBuffer.hasRemaining()) {
            sb.append(String.format("%08X", duplicateBuffer.getInt()));
        }
        return sb.toString();
    }

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
        dataIndex++;
    }

    void DisplayPackHeader(int i1, int i2) {
        if (i2 > 1024) {
            LOGGER.log(Level.FINE, "Lots of data coming! {0} / {1}",
                       new Object[]{Integer.toHexString(i1),
                       Integer.toHexString(i2)});
        }
        // else {
        //     LOGGER.log(Level.INFO, "OK! " + Integer.toHexString(i1) + " / " + Integer.toHexString(i2));
        // }

        if (i2 > 0) {
            dataLength = i2 * 4;
            dispData = ByteBuffer.allocate(dataLength);
            dispData.order(ByteOrder.LITTLE_ENDIAN);
            setNextState(ReceiverState.DISPLAY_PCKT);
        }
        else {
            setIdleState();
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
            // System.out.println(Instant.now() + " [DEBUG] ReadSync wait interrupted due to disconnect request.," + ex.getMessage());
            Thread.currentThread().interrupt();
        }
        catch (InvocationTargetException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void setIdleState() {
        this.headerstate = 0;
        this.state = ReceiverState.HEADER;
    }

    private void setNextState(ReceiverState nextState) {
        this.state = nextState;
        this.dataIndex = 0;
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

            case HEADER:
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
                            setIdleState();
                        }
                        break;

                    case 2: /* This should always be 'o' or command will be ignored */
                        if (c == 'o') {
                            headerstate = 3;
                        }
                        else {
                            setIdleState();
                        }
                        break;

                    case 3:
                        switch (c) {
                            case 'Q':
                                setNextState(ReceiverState.PARAMCHANGE_PCKT);
                                dataLength = 12;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'Q'");
                                break;
                            case 'A':
                                setNextState(ReceiverState.ACK_PCKT);
                                dataLength = 24;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'A'");
                                break;
                            case 'D':
                                setNextState(ReceiverState.DISPLAY_PCKT_HDR);
                                dataLength = 8;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'D'");
                                break;
                            case 'R': /* New case for 'R' header (AxoR packet from MCU) */
                                setNextState(ReceiverState.COMMANDRESULT_PCKT);
                                dataLength = 2; /* Expecting command_byte (1 byte) + status_byte (1 byte) */
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'R'");
                                break;
                            case 'T':
                                setNextState(ReceiverState.TEXT_PCKT);
                                textRcvBuffer.clear();
                                dataLength = 255;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'T'");
                                break;
                            case 'l':
                                setNextState(ReceiverState.SDINFO);
                                sdinfoRcvBuffer.rewind();
                                dataLength = 12;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'l'");
                                break;
                            case 'f':
                                setNextState(ReceiverState.FILEINFO_FIXED_FIELDS);
                                fileinfoRcvBuffer.clear();
                                dataLength = 8;
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'f'");
                                break;
                            case 'r':
                                setNextState(ReceiverState.MEMREAD);
                                memReadBuffer.clear();
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'r'");
                                break;
                            case 'y':
                                setNextState(ReceiverState.MEMREAD_1WORD);
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'y'");
                                break;
                            case 'V':
                                setNextState(ReceiverState.FWVERSION);
                                // System.out.println(Instant.now() + " [DEBUG] Completed headerstate after 'V'");
                                break;
                            default:
                                setIdleState();
                                // System.err.println(Instant.now() + " [DEBUG] Error trying to complete headerstate after valid 'Axo'");
                                break;
                        }
                        break;

                    default:
                        // System.err.println(Instant.now() + " [DEBUG] processByte: invalid header");
                        setIdleState();
                        break;
                }
                break;

            case PARAMCHANGE_PCKT:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
                if (dataIndex == dataLength) {
                    // System.out.println(Instant.now() + " [DEBUG] param packet complete 0x" + Integer.toHexString(packetData[1]) + "    0x" + Integer.toHexString(packetData[0]));
                    RPacketParamChange(packetData[2], packetData[1], packetData[0]);
                    setIdleState();
                }
                break;

            case ACK_PCKT:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
                if (dataIndex == dataLength) {
                    Acknowledge(packetData[0], packetData[1], packetData[2], packetData[3], packetData[4], packetData[5]);
                    setIdleState();
                }
                break;

            case DISPLAY_PCKT_HDR:
                if (dataIndex < dataLength) {
                    storeDataByte(c);
                }
                if (dataIndex == dataLength) {
                    DisplayPackHeader(packetData[0], packetData[1]);
                }
                break;

            case DISPLAY_PCKT:
                if (dataIndex < dataLength) {
                    dispData.put(cc);
                    dataIndex++;
                }
                if (dataIndex == dataLength) {
                    DistributeToDisplays(dispData);
                    setIdleState();
                }
                break;

            case COMMANDRESULT_PCKT:
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
                        // Special handling for QCmdUploadPatch's sub-command
                        else if (currentExecutingCommand instanceof QCmdUploadPatch) {
                            QCmdUploadPatch uploadCmd = (QCmdUploadPatch) currentExecutingCommand;
                            if (commandByte == 'W') {
                                uploadCmd.setStartMemWriteCompleted((byte)statusCode);
                            }
                            else if (commandByte == 'w') {
                                uploadCmd.setAppendMemWriteCompleted((byte)statusCode);
                            }
                            else if (commandByte == 'c') {
                                uploadCmd.setCloseMemWriteCompleted((byte)statusCode);
                            }
                        }
                        else if (currentExecutingCommand instanceof QCmdUploadFWSDRam) {
                            QCmdUploadFWSDRam uploadFwCmd = (QCmdUploadFWSDRam) currentExecutingCommand;
                            if (commandByte == 'W') { // Acknowledgment for TransmitStartMemWrite (AxoWs)
                                uploadFwCmd.setStartMemWriteCompleted((byte)statusCode);
                            }
                            else if (commandByte == 'w') { // Acknowledgment for TransmitAppendMemWrite (Axow)
                                uploadFwCmd.setAppendMemWriteCompleted((byte)statusCode);
                            }
                            else if (commandByte == 'c') { // Acknowledgment for TransmitCloseMemWrite (AxoWc)
                                uploadFwCmd.setCloseMemWriteCompleted((byte)statusCode);
                            }
                        }
                        // Handling for other commands that expect an AxoR for their completion
                        else if (currentExecutingCommand instanceof QCmdStart ||
                                 currentExecutingCommand instanceof QCmdStop ||
                                 currentExecutingCommand instanceof QCmdCopyPatchToFlash ||
                                 currentExecutingCommand instanceof QCmdGetFileList ||
                                 currentExecutingCommand instanceof QCmdCreateDirectory ||
                                 currentExecutingCommand instanceof QCmdChangeWorkingDirectory ||
                                 currentExecutingCommand instanceof QCmdDeleteFile ||
                                 currentExecutingCommand instanceof QCmdGetFileInfo) {

                            if (currentExecutingCommand.getExpectedAckCommandByte() == commandByte) { // for example, ('l' == 'l') -> TRUE
                                currentExecutingCommand.setMcuStatusCode((byte)statusCode);
                                if (currentExecutingCommand instanceof QCmdCreateDirectory) {
                                    /* CreateDirectory returns status FR_OK when the directory was created, and
                                        FR_EXIST, 0x08, when the directory already exists. Treat both as success. */
                                    currentExecutingCommand.setCompletedWithStatus(statusCode == 0x00 || statusCode == 0x08);
                                }
                                else {
                                    currentExecutingCommand.setCompletedWithStatus(statusCode == 0x00);
                                }

                                try {
                                    boolean offeredSuccessfully = QCmdProcessor.getQCmdProcessor().getQueueResponse().offer(currentExecutingCommand, 10, TimeUnit.MILLISECONDS);
                                    if (!offeredSuccessfully) {
                                        LOGGER.log(Level.WARNING, "Failed to offer completed QCmd (" + currentExecutingCommand.getClass().getSimpleName() + ") to QCmdProcessor queue within timeout. Queue might be full.");
                                    }
                                    synchronized (QCmdProcessor.getQCmdProcessor().getQueueLock()) {
                                        QCmdProcessor.getQCmdProcessor().getQueueLock().notifyAll();
                                    }
                                }
                                catch (InterruptedException e) {
                                    LOGGER.log(Level.SEVERE, "Interrupted while offering response to QCmdProcessor queue: " + e.getMessage());
                                    e.printStackTrace(System.err);
                                    Thread.currentThread().interrupt();
                                }
                            }
                            // else {
                            //     System.err.println(Instant.now() + " [DEBUG] Warning: currentExecutingCommand (" + currentExecutingCommand.getClass().getSimpleName() + ") received unexpected AxoR for command: " + (char)commandByte + ". Expected: " + currentExecutingCommand.getExpectedAckCommandByte() + ". Ignoring.");
                            // }
                        }
                    }
                    setIdleState();
                }
                break;

            case TEXT_PCKT:
                if (c != 0) {
                    textRcvBuffer.append((char) cc);
                }
                else {
                    // textRcvBuffer.append((char) cc);
                    textRcvBuffer.limit(textRcvBuffer.position());
                    textRcvBuffer.rewind();
                    if (sdFoundNoStartupPattern.matcher(textRcvBuffer.toString()).find()) {
                        /* Filter out error if SD card is connected but no start.bin is found */
                        LOGGER.log(Level.INFO, "SD card connected, no startup patch found.");
                    }
                    else {
                        LOGGER.log(Level.WARNING, "{0}", textRcvBuffer.toString());
                    }
                    // System.out.println(Instant.now() + " [DEBUG] FINAL MCU Text Message (AxoT): " + textRcvBuffer.toString());
                    setIdleState();
                }
                break;

            case SDINFO:
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
                    setIdleState();
                }
                break;

            case FILEINFO_FIXED_FIELDS: /* State to collect the 8-byte size and timestamp */
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
                    setNextState(ReceiverState.FILEINFO_FILENAME); /* Transition to the next sub-state */
                }
                break;

            case FILEINFO_FILENAME: /* State to collect filename bytes until null terminator */
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

                    setIdleState(); /* Packet complete, return to idle */
                }
                else {
                    /* Collect the current byte as part of the filename */
                    fileinfoRcvBuffer.put(cc);
                    dataIndex++;

                    /* Protection against malformed Axof */
                    if (dataIndex >= fileinfoRcvBuffer.capacity()) {
                        LOGGER.log(Level.SEVERE, "processByte: Filename exceeds maximum expected length. Aborting packet.");
                        setIdleState();
                    }
                }
                break;

            case MEMREAD:
                switch (dataIndex) {
                    case 0:
                        memReadAddr = (cc & 0xFF);
                        break;
                    case 1:
                        memReadAddr += (cc & 0xFF) << 8;
                        break;
                    case 2:
                        memReadAddr += (cc & 0xFF) << 16;
                        break;
                    case 3:
                        memReadAddr += (cc & 0xFF) << 24;
                        break;
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
                            System.out.print(Instant.now() + " memread offset 0x" + Integer.toHexString(memReadAddr) + ": ");
                            int i = 0;
                            while (memReadBuffer.hasRemaining()) {
                                System.out.print(String.format("%02X", memReadBuffer.get()));
                                i++;
                                if ((i % 4) == 0) {
                                    System.out.print(" ");
                                }
                                if ((i % 32) == 0) {
                                    System.out.println();
                                }
                            }
                            System.out.println();
                            synchronized (readsync) {
                                readsync.Acked = true;
                                readsync.notifyAll();
                            }
                            memReadBuffer.clear();
                            setIdleState();
                        }
                }
                dataIndex++;
                break;

            case MEMREAD_1WORD:
                switch (dataIndex) {
                    case 0:
                        memReadAddr = (cc & 0xFF);
                        break;
                    case 1:
                        memReadAddr += (cc & 0xFF) << 8;
                        break;
                    case 2:
                        memReadAddr += (cc & 0xFF) << 16;
                        break;
                    case 3:
                        memReadAddr += (cc & 0xFF) << 24;
                        break;
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
                        setIdleState();
                }
                dataIndex++;
                break;

            case FWVERSION:
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
                        temp_fwcrc = (cc & 0xFF) << 24;
                        break;
                    case 5:
                        temp_fwcrc += (cc & 0xFF) << 16;
                        break;
                    case 6:
                        temp_fwcrc += (cc & 0xFF) << 8;
                        break;
                    case 7:
                        temp_fwcrc += (cc & 0xFF);
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
                        if (fwcrc != temp_fwcrc) {
                            /* Set & report once, then only if CRC changes during this session (for firmware dev) */
                            fwcrc = temp_fwcrc;
                            String sFwcrc = String.format("%08X", fwcrc);
                            System.out.println(Instant.now() + String.format(" Core Firmware version: %d.%d.%d.%d, CRC: 0x%s, entry point: 0x%08X", fwversion[0], fwversion[1], fwversion[2], fwversion[3], sFwcrc, patchentrypoint));
                            LOGGER.log(Level.INFO, String.format("Core running Firmware version %d.%d.%d.%d | CRC %s\n", fwversion[0], fwversion[1], fwversion[2], fwversion[3], sFwcrc));
                            MainFrame.mainframe.setFirmwareID(sFwcrc);
                        }
                        setIdleState();
                        break;
                }
                dataIndex++;
                break;

            default:
                setIdleState();
                // System.out.println(Instant.now() + " [DEBUG] Unhandled byte c=" + String.format("%02x", c) + "(char=" + (char)c + ") in state=" + state);
                break;
        }
    }

    @Override
    public ksoloti_core getTargetProfile() {
        return targetProfile;
    }

    @Override
    public byte[] getFwVersion() {
        return fwversion;
    }

    @Override
    public String getFwVersionString() {
        return String.format("%d.%d.%d.%d", fwversion[0], fwversion[1], fwversion[2], fwversion[3]);
    }
}
