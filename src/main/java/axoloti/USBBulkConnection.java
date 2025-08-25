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

import axoloti.dialogs.USBPortSelectionDlg;
import axoloti.displays.DisplayInstance;
import axoloti.parameters.ParameterInstance;
import axoloti.sd.SDCardInfo;
import axoloti.targetprofile.ksoloti_core;
import axoloti.usb.Usb;
import axoloti.utils.Preferences;

import static axoloti.dialogs.USBPortSelectionDlg.ErrorString;
import static axoloti.usb.Usb.DeviceToPath;
import static axoloti.usb.Usb.PID_AXOLOTI;
import static axoloti.usb.Usb.PID_AXOLOTI_SDCARD;
import static axoloti.usb.Usb.PID_AXOLOTI_USBAUDIO;
import static axoloti.usb.Usb.PID_KSOLOTI;
import static axoloti.usb.Usb.PID_KSOLOTI_SDCARD;
import static axoloti.usb.Usb.PID_KSOLOTI_USBAUDIO;

import static axoloti.usb.Usb.PID_STM_DFU;
import static axoloti.usb.Usb.VID_AXOLOTI;
import static axoloti.usb.Usb.VID_STM;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.usb4java.*;
import qcmds.SCmdChangeWorkingDirectory;
import qcmds.SCmdCopyPatchToFlash;
import qcmds.SCmdCreateDirectory;
import qcmds.SCmdDeleteFile;
import qcmds.SCmdGetFileInfo;
import qcmds.SCmdGetFileList;
import qcmds.SCmdMemRead;
import qcmds.SCmdMemRead1Word;
import qcmds.SCmdPing;
import qcmds.QCmdProcessor;
import qcmds.SCmd;
import qcmds.SCmdStart;
import qcmds.SCmdStop;
import qcmds.SCmdGetFWVersion;
import qcmds.SCmdUploadFWSDRam;
import qcmds.SCmdUploadFile;
import qcmds.SCmdUploadPatch;

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
    private volatile Thread byteProcessorThread;
    private final BlockingQueue<SCmd> queueSerialTask;
    private final BlockingQueue<Byte> receiverBlockingQueue;
    private String targetCpuId;
    private String detectedCpuId;
    private ksoloti_core targetProfile;
    private final Context context;
    private volatile DeviceHandle handle;
    static private volatile USBBulkConnection conn = null;

    ByteBuffer dispData;

    final Sync sync;

    private Boolean isSDCardPresent = null;
    private int connectionFlags = 0;

    int CpuId0 = 0;
    int CpuId1 = 0;
    int CpuId2 = 0;
    int fwcrc = -1;
    int temp_fwcrc = -1;

    private final static byte[] AxoA_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('A')};
    private final static byte[] AxoC_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('C')};
    private final static byte[] AxoF_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('F')};
    private final static byte[] AxoM_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('M')};
    private final static byte[] AxoR_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('R')};
    private final static byte[] AxoS_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('S')};
    private final static byte[] AxoT_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('T')};
    private final static byte[] AxoV_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('V')};
    private final static byte[] AxoW_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('W')};
    private final static byte[] AxoY_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('Y')};
    private final static byte[] Axoa_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('a')};
    private final static byte[] Axol_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('l')};
    private final static byte[] Axor_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('r')};
    private final static byte[] Axos_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('s')};
    private final static byte[] Axou_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('u')};
    private final static byte[] Axow_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('w')};
    private final static byte[] Axoy_pckt =  new byte[] {(byte) ('A'), (byte) ('x'), (byte) ('o'), (byte) ('y')};

    private final static short bulkVID = (short) 0x16C0;
    private final static short bulkPIDAxoloti = (short) 0x0442;
    private final static short bulkPIDAxolotiUsbAudio = (short) 0x0447;
    private final static short bulkPIDKsoloti = (short) 0x0444;
    private final static short bulkPIDKsolotiUsbAudio = (short) 0x0446;
    private static int useBulkInterfaceNumber = 2;

    private final static Pattern sdFoundNoStartupPattern = Pattern.compile("File error:.*filename:\"/start.bin\"");

    private final static byte OUT_ENDPOINT = (byte) 0x02;
    private final static byte IN_ENDPOINT = (byte) 0x82;

    private int currentFileSize;
    private int currentFileTimestamp;

    private volatile Object usbInLock = new Object();  /* For IN endpoint operations (reading) */
    private volatile Object usbOutLock = new Object(); /* For OUT endpoint operations (writing) */

    protected volatile SCmd currentExecutingCommand = null;

    enum ReceiverState {
        IDLE,               /* awaiting new response */
        ACK_PCKT,           /* general acknowledge */
        PARAMCHANGE_PCKT,   /* parameter changed */
        DISPLAY_PCKT_HDR,   /* object display readbac */
        DISPLAY_PCKT,       /* object display readback */
        TEXT_PCKT,          /* text message to display in log */
        SDINFO,             /* sdcard info */
        FILEINFO_HDR,       /* file listing entry, size and timestamp (8 bytes of Axof packet) */
        FILEINFO_DATA,      /* file listing entry, variable length filename */
        MEMREAD,            /* one-time programmable bytes */
        MEMREAD_1WORD,      /* one-time programmable bytes */
        FWVERSION,          /* responds with own firmware version, 1.1.0.0 (though not used for anything?) */
        COMMANDRESULT_PCKT  /* New Response Packet: ['A', 'x', 'o', 'R', command_byte, status_byte] */
    };

    /*
     * Protocol documentation:
     * "AxoP" + bb + vvvv -> parameter change index bb (16bit), value vvvv (32bit)
     */
    private ReceiverState state = ReceiverState.IDLE;
    private int headerstate;
    private int[] packetData = new int[64];
    private int dataIndex = 0;  /* in bytes */
    private int dataLength = 0; /* in bytes */
    private CharBuffer textRcvBuffer = CharBuffer.allocate(256);
    private ByteBuffer sdinfoRcvBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.LITTLE_ENDIAN);
    private ByteBuffer fileinfoRcvBuffer = ByteBuffer.allocateDirect(256).order(ByteOrder.LITTLE_ENDIAN);
    private ByteBuffer memReadBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.LITTLE_ENDIAN);
    private int memReadAddr;
    private int memReadLength;
    private int memRead1WordValue;
    private byte[] fwversion = new byte[4];
    private int patchentrypoint;

    class Sync {
        boolean Acked = false;
    }

    class Receiver implements Runnable {
        @Override
        public void run() {
            ByteBuffer recvbuffer = ByteBuffer.allocateDirect(4096).order(ByteOrder.LITTLE_ENDIAN);
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
                        byte[] receivedData = new byte[sz]; /* Copy chunk to faster in-memory array */
                        recvbuffer.position(0); 
                        recvbuffer.get(receivedData);
                        for (int i = 0; i < sz; i++) {
                            try {
                                receiverBlockingQueue.put(receivedData[i]); 
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
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
                    e.printStackTrace(System.out);
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
                SCmd cmd = null;
                try {
                    cmd = queueSerialTask.poll(5, TimeUnit.SECONDS);

                    if (Thread.currentThread().isInterrupted()|| disconnectRequested) {
                        break;
                    }

                    if (cmd != null) {
                        SCmd response = cmd.Do();
                        if (response != null) {
                            QCmdProcessor.getInstance().getQueueResponse().put(response);
                        }
                    }
                }
                catch (InterruptedException ex) {
                    // System.out.println(Instant.now() + " [DEBUG] Transmitter: InterruptedException caught from queue.poll(). Exiting loop.");
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error during Transmitter thread: " + e.getMessage());
                    e.printStackTrace(System.out);
                    disconnect();
                    break;
                }
            }
            // System.out.println(Instant.now() + " [DEBUG] Transmitter thread exiting gracefully.");
        }
    }

    private class ByteProcessor implements Runnable {
        @Override
        public void run() {
            try {
                // Continuously take bytes from the queue and process them
                while (!Thread.currentThread().isInterrupted() && !disconnectRequested) {
                    Byte b = receiverBlockingQueue.take();
                    processByte(b);
                }
            } catch (InterruptedException e) {
                // Thread was interrupted, exit gracefully
                Thread.currentThread().interrupt();
            }
        }
    }

	protected USBBulkConnection() {
        this.sync = new Sync();
        this.patch = null;

        disconnectRequested = false;
        isConnecting = false;
        connected = false;
        queueSerialTask = new ArrayBlockingQueue<SCmd>(99);
        receiverBlockingQueue = new LinkedBlockingQueue<>();
        this.context = Usb.getContext();
    }

    public static Connection getInstance() {
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
    public void setCurrentExecutingCommand(SCmd command) {
        this.currentExecutingCommand = command;
    }

    @Override
    public SCmd getCurrentExecutingCommand() {
        return currentExecutingCommand;
    }

    @Override
    public void setPatch(Patch patch) {
        this.patch = patch;
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
    public boolean AppendToQueue(SCmd cmd) {
        try {
            boolean added = queueSerialTask.offer(cmd, 100, TimeUnit.MILLISECONDS);
            if (!added) {
                LOGGER.log(Level.WARNING, "USB command queue full. Command not sent: " + cmd.getClass().getSimpleName());
            }
            return added;
        }
        catch (InterruptedException ex) {
            /* Restore the interrupted status, as per best practice */
            Thread.currentThread().interrupt();
            return false; /* Command was not added due to interruption */
        }
    }

    @Override
    public boolean connect() {
        /* Initial State Check: Prevent overlapping operations */
        if (disconnectRequested || isConnecting) {
            return false;
        }
        isConnecting = true;
        this.connected = false; /* Update UI/internal state immediately */

        /* Try to get Patcher's targetCpuId if not already set */
        if (targetCpuId == null) {
            targetCpuId = Preferences.getInstance().getComPortName();
        }
        targetProfile = new ksoloti_core();

        try {
            /* Open Device Handle */
            handle = OpenDeviceHandle();
            if (handle == null) {
                System.err.println(Instant.now() + " [ERROR] Connect: Failed to open USB device handle.");
                disconnect();
                return false;
            }

            /* Claim Interface */
            int result = LibUsb.claimInterface(handle, useBulkInterfaceNumber);
            if (result != LibUsb.SUCCESS) {
                System.err.println(Instant.now() + " [ERROR] Connect: Failed to claim interface " + useBulkInterfaceNumber + ": " + LibUsb.errorName(result) + " (Code: " + result + ")");
                disconnect();
                return false;
            }

            setIdleState();
            QCmdProcessor.getInstance().setConnection(this);

            /* Start Trinity of Threads */
            receiverBlockingQueue.clear();
            receiverThread = new Thread(new Receiver());
            receiverThread.setName("Receiver");
            receiverThread.start();

            transmitterThread = new Thread(new Transmitter());
            transmitterThread.setName("Transmitter");
            transmitterThread.start();

            byteProcessorThread = new Thread(new ByteProcessor());
            byteProcessorThread.setName("ByteProcessor");
            byteProcessorThread.start();

            /* Initial Communication and Synchronization */
            ClearSync();
            new SCmdPing().Do();
            if (!WaitSync()) {
                LOGGER.log(Level.SEVERE, "Core not responding - firmware may be stuck running a problematic patch?\nRestart the Core and try again.");
                disconnect();
                return false;
            }

            /* If we reach here, initial handshake is successful */
            this.connected = true;
            LOGGER.log(Level.WARNING, "Connected\n");

            /* Post-Connection Commands (Firmware version, CPU revision, board ID) */
            try {
                SCmdGetFWVersion fwVersionCmd = new SCmdGetFWVersion();
                fwVersionCmd.Do(this);
                if (!fwVersionCmd.waitForCompletion()) {
                    LOGGER.log(Level.SEVERE, "Get FW version command timed out.");
                }
                else if (!fwVersionCmd.isSuccessful()) {
                    LOGGER.log(Level.SEVERE, "Failed to get FW version.");
                }

                SCmdMemRead1Word cpuRevisionCmd = new SCmdMemRead1Word(targetProfile.getCPUIDCodeAddr());
                cpuRevisionCmd.Do(this);
                if (!cpuRevisionCmd.waitForCompletion()) {
                    LOGGER.log(Level.SEVERE, "Get CPU revision command timed out.");
                }
                else if (!cpuRevisionCmd.isSuccessful()) {
                    LOGGER.log(Level.SEVERE, "Failed to get CPU revision.");
                }
                targetProfile.setCPUIDCode(cpuRevisionCmd.getValueRead());

                SCmdMemRead boardIDCmd = new SCmdMemRead(targetProfile.getCPUSerialAddr(), targetProfile.getCPUSerialLength());
                boardIDCmd.Do(this);
                if (!boardIDCmd.waitForCompletion()) {
                    LOGGER.log(Level.SEVERE, "Get board ID command timed out.");
                }
                else if (!boardIDCmd.isSuccessful()) {
                    LOGGER.log(Level.SEVERE, "Failed to get board ID.");
                }
                targetProfile.setCPUSerial(boardIDCmd.getValuesRead());
                this.detectedCpuId = CpuIdToHexString(targetProfile.getCPUSerial());
            }
            catch (Exception cmdEx) {
                LOGGER.log(Level.SEVERE, "Error during post-connection QCmd processing. Connection might be unstable: " + cmdEx.getMessage());
                cmdEx.printStackTrace(System.out);
                return false;
            }

            ShowConnect(); /* Show connected in GUI */
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
            ex.printStackTrace(System.out);
            disconnect();
            return false;
        }
        finally {
            isConnecting = false;
        }
    }

    
    @Override
    public void disconnect() {
        /* Guard against redundant calls */
        if (this.disconnectRequested && !connected) {
            return;
        }
        this.disconnectRequested = true; /* Set flag to signal threads to stop */

        /* Clear the queue of tasks for the Transmitter thread */
        if (queueSerialTask != null) {
            queueSerialTask.clear();
        }

        try {

            /* Interrupt Trinity of Threads */
            if (receiverThread != null && receiverThread.isAlive()) {
                receiverThread.interrupt();
            }

            if (transmitterThread != null && transmitterThread.isAlive()) {
                transmitterThread.interrupt();
            }

            if (byteProcessorThread != null && byteProcessorThread.isAlive()) {
                byteProcessorThread.interrupt();
            }

            /* Clear any pending and/or queued commands */
            this.currentExecutingCommand = null;
            if (QCmdProcessor.getInstance() != null) {
                QCmdProcessor.getInstance().clearQueues();
            }

            /* Wait for threads to terminate */
            long threadJoinTimeoutMs = 5000;

            if (receiverThread != null && receiverThread.isAlive()) {
                try {
                    receiverThread.join(threadJoinTimeoutMs);
                    if (receiverThread.isAlive()) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: Receiver thread did not terminate within timeout.");
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                    Thread.currentThread().interrupt();
                }
                finally {
                    receiverThread = null;
                }
            }

            if (transmitterThread != null && transmitterThread.isAlive()) {
                try {
                    transmitterThread.join(threadJoinTimeoutMs);
                    if (transmitterThread.isAlive()) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: Transmitter thread did not terminate within timeout.");
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                    Thread.currentThread().interrupt();
                }
                finally {
                    transmitterThread = null;
                }
            }
            
            if (byteProcessorThread != null && byteProcessorThread.isAlive()) {
                try {
                    byteProcessorThread.join(threadJoinTimeoutMs);
                    if (byteProcessorThread.isAlive()) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: ByteProcessor thread did not terminate within timeout.");
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace(System.out);
                    Thread.currentThread().interrupt();
                }
                finally {
                    byteProcessorThread = null;
                }
            }

            /* Perform USB resource cleanup (only after threads are confirmed stopped or timed out) */
            synchronized (usbOutLock) {
                if (handle != null) {
                    try {

                        /* Calling resetDevice is a bit "risky" but so far has been improving stability a lot
                        especially during repeated disconnects and re-connects. */
                        int resetResult = LibUsb.resetDevice(handle);
                        if (resetResult != LibUsb.SUCCESS) {
                            System.err.println(Instant.now() + " [ERROR] Disconnect: Error resetting device: " + LibUsb.strError(resetResult) + " (Code: " + resetResult + ")");
                        }
                    }
                    catch (LibUsbException resetEx) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: LibUsbException during device reset: " + resetEx.getMessage());
                    }

                    try {
                        LibUsb.releaseInterface(handle, useBulkInterfaceNumber);
                    }
                    catch (LibUsbException releaseEx) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: Error releasing interface (may be normal after reset): " + releaseEx.getMessage());
                    }

                    try {
                        LibUsb.close(handle);
                        handle = null; /* Null immediately to prevent race conditions */
                    }
                    catch (LibUsbException closeEx) {
                        System.err.println(Instant.now() + " [ERROR] Disconnect: Error closing handle (may be normal after reset): " + closeEx.getMessage());
                    }
                    finally {
                        handle = null; /* Should already be null but just to be sure */
                    }
                }
            }
        }
        catch (Exception mainEx) {
            LOGGER.log(Level.SEVERE, "Error during Disconnect cleanup: " + mainEx.getMessage());
            mainEx.printStackTrace(System.out);
        }
        finally {
            /* Reset state variables, regardless of cleanup success */
            connected = false;
            detectedCpuId = null;
            isSDCardPresent = null;
            CpuId0 = 0;
            CpuId1 = 0;
            CpuId2 = 0;
            isConnecting = false;

            /* Notify UI */
            ShowDisconnect();
            LOGGER.log(Level.WARNING, "Disconnected\n");

            /* Clear `disconnectRequested` flag last, after all cleanup and UI updates */
            this.disconnectRequested = false;
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

    private String getDeviceSerialNumber(Device device) {
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result != LibUsb.SUCCESS) {
            return null;
        }
        try {
            DeviceDescriptor descriptor = new DeviceDescriptor();
            result = LibUsb.getDeviceDescriptor(device, descriptor);
            if (result == LibUsb.SUCCESS && handle != null) {
                String serial = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());
                return serial;
            }
            else {
                return null;
            }
        }
        finally {
            if (handle != null) {
                LibUsb.close(handle);
            }
        }
    }

    private DeviceHandle tryOpenAndMatchDevice(Device d, boolean checkSerialNumber) {
        DeviceDescriptor descriptor = new DeviceDescriptor();
        int result = LibUsb.getDeviceDescriptor(d, descriptor);
        if (result != LibUsb.SUCCESS) {
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
    public void selectPort() {
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

    public List<String[]> getDeviceList() {
        List<String[]> rows = new ArrayList<>();
        DeviceList list = new DeviceList();

        Context sharedContext = Usb.getContext();
        if (sharedContext == null) {
            return rows;
        }

        int result = LibUsb.getDeviceList(sharedContext, list);
        if (result < 0) {
            return rows;
        }

        try {
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result == LibUsb.SUCCESS) {
                    String serial = getDeviceSerialNumber(device);

                    if (descriptor.idVendor() == VID_STM) {
                        if (descriptor.idProduct() == PID_STM_DFU) {
                            rows.add(new String[]{"", USBPortSelectionDlg.sDFUBootloader, DeviceToPath(device), (serial != null) ? "Driver OK" : "Inaccessible"});
                        }
                    }
                    else if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core") &&
                             descriptor.idVendor() == VID_AXOLOTI &&
                             ((descriptor.idProduct() == PID_KSOLOTI) ||
                              (descriptor.idProduct() == PID_KSOLOTI_USBAUDIO))) {
                                  
                        String sName = (descriptor.idProduct() == PID_KSOLOTI) ? USBPortSelectionDlg.sKsolotiCore : USBPortSelectionDlg.sKsolotiCoreUsbAudio;
                        if (serial != null) {
                            String name = Preferences.getInstance().getBoardName(serial);
                            if (name == null) name = "";
                            rows.add(new String[]{name, sName, DeviceToPath(device), serial});
                        }
                        else {
                            rows.add(new String[]{"", sName, DeviceToPath(device), "Inaccessible: no serial"});
                        }
                    }
                    else if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core") &&
                             descriptor.idVendor() == VID_AXOLOTI &&
                             descriptor.idProduct() == PID_KSOLOTI_SDCARD) {

                        rows.add(new String[]{"", USBPortSelectionDlg.sKsolotiSDCard, DeviceToPath(device), "Eject/unmount disk to connect"});
                    }
                    else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core") &&
                             descriptor.idVendor() == VID_AXOLOTI &&
                             ((descriptor.idProduct() == PID_AXOLOTI) ||
                             (descriptor.idProduct() == PID_AXOLOTI_USBAUDIO))) {
                                 
                        String sName = (descriptor.idProduct() == PID_AXOLOTI) ? USBPortSelectionDlg.sAxolotiCore : USBPortSelectionDlg.sAxolotiCoreUsbAudio;
                        if (serial != null) {
                            String name = Preferences.getInstance().getBoardName(serial);
                            if (name == null) name = "";
                            rows.add(new String[]{name, sName, DeviceToPath(device), serial});
                        }
                        else {
                            rows.add(new String[]{"", sName, DeviceToPath(device), "Inaccessible: no serial"});
                        }
                    }
                    else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core") &&
                             descriptor.idVendor() == VID_AXOLOTI &&
                             descriptor.idProduct() == PID_AXOLOTI_SDCARD) {

                        rows.add(new String[]{"", USBPortSelectionDlg.sAxolotiSDCard, DeviceToPath(device), "Eject/unmount disk to connect"});
                    }
                }
            }
        }
        finally {
            LibUsb.freeDeviceList(list, true);
        }

        return rows;
    }

    @Override
    public int writeBytes(ByteBuffer data) {

        ByteBuffer buffer = data.duplicate(); /* Deep copy for safety? */
        buffer.rewind();
        IntBuffer transfered = IntBuffer.allocate(1);

        /* Acquire the OUT lock for writing */
        synchronized (usbOutLock) {
            if (handle == null) {
                return LibUsb.ERROR_NO_DEVICE;
            }

            int result = LibUsb.bulkTransfer(handle, (byte) OUT_ENDPOINT, buffer, transfered, 3000);
            if (result != LibUsb.SUCCESS) {

                if (result == -99) {
                    /*
                    * Filter out error -99 ... seems to pop up every now and then but does not lead to connection loss  
                    * this "bug" will likely  be resolved after libusb update
                    */
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
            }
            return result;
        } /* end synchronize (usbOutLock) */
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
    public int TransmitStart() {
        /* Total size (bytes):
           "Axos"           (4)
           uUIMidiCost      (2)
           uDspLimit200     (1)
         */
        short uUIMidiCost = Preferences.getInstance().getUiMidiThreadCost();
        byte  uDspLimit200 = (byte)(Preferences.getInstance().getDspLimitPercent()*2);
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(7).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Axos_pckt);
        buffer.putShort(uUIMidiCost);
        buffer.put(uDspLimit200);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitStop() {
        /* Total size (bytes):
           "AxoS"           (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoS_pckt);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitRecallPreset(int presetNo) {
        /* Total size (bytes):
           "AxoT"           (4)
           presetNo         (1)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(5).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoT_pckt);
        buffer.put((byte) presetNo);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitBringToDFU() {
        /* Total size (bytes):
           "Axou"           (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Axou_pckt);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitGetFWVersion() {
        /* Total size (bytes):
           "AxoV"           (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoV_pckt);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitGetSpilinkSynced() {
        /* Total size (bytes):
           "AxoY"           (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoY_pckt);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitMidi(int m0, int m1, int m2) {
        /* Total size (bytes):
           "AxoM"           (4)
           MIDI message     (3)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(7).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoM_pckt);
        buffer.put((byte) m0);
        buffer.put((byte) m1);
        buffer.put((byte) m2);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitUpdatedPreset(byte[] data) {
        /* Total size (bytes):
           "AxoR"           (4)
           data length      (4)
           data bytes       (variable length)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoR_pckt);
        buffer.putInt(data.length);
        buffer.put(data);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitGetFileList() {
        /* Total size (bytes):
           "Axol"           (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Axol_pckt);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitGetFileInfo(String filename) {
        /* Total size (bytes):
           "AxoC"           (4)
           pFileSize        (4)
           FileName[0]      (1)
           FileName[1]      (1) <- sub-command: 'I'
           filename bytes   (variable length)
           null terminator  (1)
        */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(10 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoC_pckt);
        buffer.putInt(0);
        buffer.put((byte)0x00);
        buffer.put((byte)'I');
        buffer.put(filenameBytes);
        buffer.put((byte)0x00);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitPing() {
        /* Total size (bytes):
           "AxoA"           (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoA_pckt);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitCopyToFlash() {
        /* Total size (bytes):
           "AxoF"           (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoF_pckt);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitStartMemWrite(int startAddr, int totalLen) {
        /* Total size (bytes):
           "AxoW"           (4)
           start address    (4)
           Total length     (4)
           sub-command      (1) <- 'W'
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(13).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoW_pckt);
        buffer.putInt(startAddr);
        buffer.putInt(totalLen);
        buffer.put((byte)'W');
        return writeBytes(buffer);
    }

    @Override
    public int TransmitAppendMemWrite(byte[] data) {
        /* Total size (bytes):
           "Axow"           (4)
           Length           (4)
           data             (variable length)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Axow_pckt);
        buffer.putInt(data.length);
        buffer.put(data);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitCloseMemWrite(int startAddr, int totalLen) {
        /* Total size (bytes):
           "AxoW"           (4)
           start address    (4)
           Total length     (4)
           sub-command      (1) <- 'e'
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(13).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoW_pckt);
        buffer.putInt(startAddr);
        buffer.putInt(totalLen);
        buffer.put((byte)'e');
        return writeBytes(buffer);
    }

    @Override
    public int TransmitCreateFile(String filename, int size, Calendar date) {
        /* Total size (bytes):
           "AxoC"           (4)
           pFileSize        (4)
           FileName[0]      (1)
           FileName[1]      (1) <- sub-command: 'f'
           fdate            (2)
           ftime            (2)
           filename bytes   (variable length)
           null terminator  (1)
         */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocateDirect(14 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoC_pckt);
        buffer.putInt(size);
        buffer.put((byte)0x00);
        buffer.put((byte)'f');

        /* Calculate FatFs date/time */
        int dy = date.get(Calendar.YEAR);
        int dm = date.get(Calendar.MONTH) + 1;
        int dd = date.get(Calendar.DAY_OF_MONTH);
        int th = date.get(Calendar.HOUR_OF_DAY);
        int tm = date.get(Calendar.MINUTE);
        int ts = date.get(Calendar.SECOND);
        short fatFsDate = (short)(((dy - 1980) << 9) | (dm << 5) | dd);
        short fatFsTime = (short)((th << 11) | (tm << 5) | (ts / 2));

        buffer.putShort(fatFsDate);
        buffer.putShort(fatFsTime);
        buffer.put(filenameBytes);
        buffer.put((byte)0x00);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitDeleteFile(String filename) {
        /* Total size (bytes):
           "AxoC"           (4)
           pFileSize        (4)
           FileName[0]      (1)
           FileName[1]      (1) <- sub-command: 'D'
           filename bytes   (variable length)
           null terminator  (1)
         */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);

        ByteBuffer buffer = ByteBuffer.allocateDirect(10 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoC_pckt);
        buffer.putInt(0);
        buffer.put((byte)0x00);
        buffer.put((byte)'D');
        buffer.put(filenameBytes);
        buffer.put((byte)0x00);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitChangeWorkingDirectory(String path) {
        /* Total size (bytes):
           "AxoC"           (4)
           pFileSize        (4)
           FileName[0]      (1)
           FileName[1]      (1) <- sub-command: 'h'
           path bytes       (variable length)
           null terminator  (1)
         */
        byte[] pathBytes = path.getBytes(StandardCharsets.US_ASCII);

        ByteBuffer buffer = ByteBuffer.allocateDirect(10 + pathBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoC_pckt);
        buffer.putInt(0);
        buffer.put((byte)0x00);
        buffer.put((byte)'h');
        buffer.put(pathBytes);
        buffer.put((byte)0x00);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitCreateDirectory(String filename, Calendar date) {
        /* Total size (bytes):
           "AxoC"           (4)
           pFileSize        (4)
           FileName[0]      (1)
           FileName[1]      (1) <- sub-command: 'k'
           fdate            (2)
           ftime            (2)
           filename bytes   (variable length)
           null terminator  (1)
         */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(14 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoC_pckt);
        buffer.putInt(0);
        buffer.put((byte)0x00);
        buffer.put((byte)'k');

        /* Calculate FatFs date/time */
        int dy = date.get(Calendar.YEAR);
        int dm = date.get(Calendar.MONTH) + 1;
        int dd = date.get(Calendar.DAY_OF_MONTH);
        int th = date.get(Calendar.HOUR_OF_DAY);
        int tm = date.get(Calendar.MINUTE);
        int ts = date.get(Calendar.SECOND);
        short fatFsDate = (short)(((dy - 1980) << 9) | (dm << 5) | dd);
        short fatFsTime = (short)((th << 11) | (tm << 5) | (ts / 2));

        buffer.putShort(fatFsDate);
        buffer.putShort(fatFsTime);
        buffer.put(filenameBytes);
        buffer.put((byte)0x00);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitAppendFile(byte[] data) {
        /* Total size (bytes):
           "Axoa"           (4)
           Length           (4)
           data             (variable length)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Axoa_pckt);
        buffer.putInt(data.length);
        buffer.put(data);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitCloseFile(String filename, Calendar date) {
        /* Total size (bytes):
           "AxoC"           (4)
           pFileSize        (4)
           FileName[0]      (1)
           FileName[1]      (1) <- sub-command: 'c'
           fdate            (2)
           ftime            (2)
           filename bytes   (variable length)
           null terminator  (1)
         */
        byte[] filenameBytes = filename.getBytes(StandardCharsets.US_ASCII);
        
        ByteBuffer buffer = ByteBuffer.allocateDirect(14 + filenameBytes.length + 1).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(AxoC_pckt);
        buffer.putInt(0);
        buffer.put((byte)0x00);
        buffer.put((byte)'c');

        /* Calculate FatFs date/time */
        int dy = date.get(Calendar.YEAR);
        int dm = date.get(Calendar.MONTH) + 1;
        int dd = date.get(Calendar.DAY_OF_MONTH);
        int th = date.get(Calendar.HOUR_OF_DAY);
        int tm = date.get(Calendar.MINUTE);
        int ts = date.get(Calendar.SECOND);
        short fatFsDate = (short)(((dy - 1980) << 9) | (dm << 5) | dd);
        short fatFsTime = (short)((th << 11) | (tm << 5) | (ts / 2));

        buffer.putShort(fatFsDate);
        buffer.putShort(fatFsTime);
        buffer.put(filenameBytes);
        buffer.put((byte)0x00);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitMemoryRead(int addr, int length) {
        /* Total size (bytes):
           "Axor"           (4)
           address          (4)
           length           (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Axor_pckt);
        buffer.putInt(addr);
        buffer.putInt(length);
        return writeBytes(buffer);
    }

    @Override
    public int TransmitMemoryRead1Word(int addr) {
        /* Total size (bytes):
           "Axoy"           (4)
           address          (4)
         */
        ByteBuffer buffer = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Axoy_pckt);
        buffer.putInt(addr);
        return writeBytes(buffer);
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
            dispData = ByteBuffer.allocateDirect(dataLength).order(ByteOrder.LITTLE_ENDIAN);
            setNextState(ReceiverState.DISPLAY_PCKT);
        }
        else {
            setIdleState();
        }
    }

    void DistributeToDisplays(final ByteBuffer dispData) {
        // LOGGER.log(Level.INFO, "Distr1");
        try {
            if (patch == null || patch.DisplayInstances == null) {
                return;
            }
            if (!patch.IsLocked()) {
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
        this.state = ReceiverState.IDLE;
        this.dataIndex = 0;
        this.dataLength = 0;
    }

    private void setNextState(ReceiverState nextState) {
        this.state = nextState;
        this.dataIndex = 0;
    }

    void processByte(byte cc) {

        int c = cc & 0xff;

        // if (!state.name().equals("ACK_PCKT")) { /* Filter out ACK payloads */
        //     String charDisplay;
        //     charDisplay = String.format("%02X", c) + "h "; // Show hex for non-printable characters
        //     if (c >= 0x20 && c <= 0x7E) {
        //         charDisplay += "'" + String.valueOf((char) c) + "'";
        //     }
        //     else {
        //         charDisplay += "   ";
        //     }
        //     System.out.println(Instant.now() + " [DEBUG] processByte c=" + charDisplay + " s=" + state.name());
        // }

        switch (state) {

            case IDLE:
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
                            case 'P':
                                dataLength = 12;
                                setNextState(ReceiverState.PARAMCHANGE_PCKT);
                                break;
                            case 'A':
                                dataLength = 24;
                                setNextState(ReceiverState.ACK_PCKT);
                                break;
                            case 'D': /* Receiving display update (scope etc.) */
                                dataLength = 8;
                                setNextState(ReceiverState.DISPLAY_PCKT_HDR);
                                break;
                            case 'R': /* ("AxoR" response packet from MCU) */
                                dataLength = 2;
                                setNextState(ReceiverState.COMMANDRESULT_PCKT);
                                break;
                            case 'L': /* Receiving LogTextMessage */
                                textRcvBuffer.clear();
                                dataLength = 255;
                                setNextState(ReceiverState.TEXT_PCKT);
                                break;
                            case 'l':
                                sdinfoRcvBuffer.rewind();
                                dataLength = 12;
                                setNextState(ReceiverState.SDINFO);
                                break;
                            case 'f':
                                fileinfoRcvBuffer.clear();
                                dataLength = 8;
                                setNextState(ReceiverState.FILEINFO_HDR);
                                break;
                            case 'r':
                                memReadBuffer.clear();
                                setNextState(ReceiverState.MEMREAD);
                                break;
                            case 'y':
                                setNextState(ReceiverState.MEMREAD_1WORD);
                                break;
                            case 'V':
                                setNextState(ReceiverState.FWVERSION);
                                break;
                            default:
                                setIdleState();
                                break;
                        } /* End switch (c) */
                        break;

                    default:
                        // System.err.println(Instant.now() + " [DEBUG] processByte: invalid header");
                        setIdleState();
                        break;
                } /* End switch (headerstate) */
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
                if (dataIndex == dataLength) {
                    int commandByte = packetData[0] & 0xFF;
                    int statusCode = (packetData[0] >> 8) & 0xFF;

                    if (currentExecutingCommand != null) {
                        /* Special handling for SCmdUploadPatch's sub-commands */
                        if (currentExecutingCommand instanceof SCmdUploadPatch) {
                            SCmdUploadPatch uploadCmd = (SCmdUploadPatch) currentExecutingCommand;
                            if (commandByte == 'W') {
                                uploadCmd.setStartMemWriteCompletedWithStatus(statusCode);
                            }
                            else if (commandByte == 'w') {
                                uploadCmd.setAppendMemWriteCompletedWithStatus(statusCode);
                            }
                            else if (commandByte == 'e') {
                                uploadCmd.setCloseMemWriteCompletedWithStatus(statusCode);
                                uploadCmd.setCompletedWithStatus(statusCode);
                            }
                        }
                        /* Special handling for SCmdUploadFile's sub-commands */
                        else if (currentExecutingCommand instanceof SCmdUploadFile) {
                            SCmdUploadFile uploadCmd = (SCmdUploadFile) currentExecutingCommand;
                            if (commandByte == 'f') {
                                uploadCmd.setCreateFileCompletedWithStatus(statusCode);
                            }
                            else if (commandByte == 'a') {
                                uploadCmd.setAppendFileCompletedWithStatus(statusCode);
                            }
                            else if (commandByte == 'c') {
                                uploadCmd.setCloseFileCompletedWithStatus(statusCode);
                                uploadCmd.setCompletedWithStatus(statusCode);
                            }
                        }
                        /* Special handling for SCmdUploadFWSDRam's sub-commands */
                        else if (currentExecutingCommand instanceof SCmdUploadFWSDRam) {
                            SCmdUploadFWSDRam uploadFwCmd = (SCmdUploadFWSDRam) currentExecutingCommand;
                            if (commandByte == 'W') {
                                uploadFwCmd.setStartMemWriteCompletedWithStatus(statusCode);
                            }
                            else if (commandByte == 'w') {
                                uploadFwCmd.setAppendMemWriteCompletedWithStatus(statusCode);
                            }
                            else if (commandByte == 'e') {
                                uploadFwCmd.setCloseMemWriteCompletedWithStatus(statusCode);
                                uploadFwCmd.setCompletedWithStatus(statusCode);
                            }
                        }
                        /* Handling for other SCmd's that expect an AxoR for their completion */
                        else if (currentExecutingCommand instanceof SCmdStart ||
                                 currentExecutingCommand instanceof SCmdStop ||
                                 currentExecutingCommand instanceof SCmdChangeWorkingDirectory ||
                                 currentExecutingCommand instanceof SCmdCreateDirectory ||
                                 currentExecutingCommand instanceof SCmdGetFileList ||
                                 currentExecutingCommand instanceof SCmdCopyPatchToFlash ||
                                 currentExecutingCommand instanceof SCmdDeleteFile ||
                                 currentExecutingCommand instanceof SCmdGetFileInfo) {

                            /* Any commands with no explicitly set command byte ('\0') will fall through */
                            if (currentExecutingCommand.getExpectedAckCommandByte() == commandByte) {
                                /* for example, ('l' == 'l') -> TRUE */
                                currentExecutingCommand.setCompletedWithStatus(statusCode);

                                try {
                                    boolean offeredSuccessfully = QCmdProcessor.getInstance().getQueueResponse().offer(currentExecutingCommand, 100, TimeUnit.MILLISECONDS);
                                    if (!offeredSuccessfully) {
                                        LOGGER.log(Level.WARNING, "Failed to offer completed QCmd (" + currentExecutingCommand.getClass().getSimpleName() + ") to QCmdProcessor queue within timeout. Queue might be full.");
                                    }
                                    synchronized (QCmdProcessor.getInstance().getQueueLock()) {
                                        QCmdProcessor.getInstance().getQueueLock().notifyAll();
                                    }
                                }
                                catch (InterruptedException e) {
                                    LOGGER.log(Level.SEVERE, "Interrupted while offering response to QCmdProcessor queue: " + e.getMessage());
                                    e.printStackTrace(System.out);
                                    Thread.currentThread().interrupt();
                                }
                            }
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

            case FILEINFO_HDR: /* State to collect the 8-byte size and timestamp */
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
                    setNextState(ReceiverState.FILEINFO_DATA); /* Transition to the next sub-state */
                }
                break;

            case FILEINFO_DATA: /* State to collect filename bytes until null terminator */
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
                        memReadBuffer = ByteBuffer.allocateDirect(memReadLength).order(ByteOrder.LITTLE_ENDIAN);
                        memReadBuffer.rewind();
                    default:
                        memReadBuffer.put(cc);
                        if (dataIndex == memReadLength + 7) {
                            memReadBuffer.rewind();
                            if (currentExecutingCommand != null && currentExecutingCommand instanceof SCmdMemRead) {
                                SCmdMemRead memReadCmd = (SCmdMemRead) currentExecutingCommand;
                                memReadCmd.setValuesRead(memReadBuffer);
                                memReadCmd.setCompletedWithStatus(0);
                            }

                            System.out.print(Instant.now() + " SCmdMemRead address 0x" + Integer.toHexString(memReadAddr).toUpperCase() + ", length " + memReadLength + ": ");
                            int i = 0;
                            memReadBuffer.rewind();
                            while (memReadBuffer.hasRemaining()) {
                                System.out.print(String.format("%02X", memReadBuffer.get()));
                                i++;
                                if ((i % 4) == 0) {
                                    System.out.print(" ");
                                }
                            }
                            System.out.println();
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
                        memRead1WordValue = (cc & 0xFF);
                        break;
                    case 5:
                        memRead1WordValue += (cc & 0xFF) << 8;
                        break;
                    case 6:
                        memRead1WordValue += (cc & 0xFF) << 16;
                        break;
                    case 7:
                        memRead1WordValue += (cc & 0xFF) << 24;
                        if (currentExecutingCommand != null && currentExecutingCommand instanceof SCmdMemRead1Word) {
                            SCmdMemRead1Word cmd = (SCmdMemRead1Word) currentExecutingCommand;
                            cmd.setValueRead(memRead1WordValue);
                            cmd.setCompletedWithStatus(0);
                            System.out.println(Instant.now() + " SCmdMemRead1Word address 0x" + Integer.toHexString(memReadAddr).toUpperCase() + ", value read: 0x" + Integer.toHexString(memRead1WordValue).toUpperCase());
                        }
                        setIdleState();
                    default:
                        setIdleState();
                        break;
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
                        temp_fwcrc = (cc & 0xFF);
                        break;
                    case 5:
                        temp_fwcrc += (cc & 0xFF) << 8;
                        break;
                    case 6:
                        temp_fwcrc += (cc & 0xFF) << 16;
                        break;
                    case 7:
                        temp_fwcrc += (cc & 0xFF) << 24;
                        break;
                    case 8:
                        patchentrypoint = (cc & 0xFF);
                        break;
                    case 9:
                        patchentrypoint += (cc & 0xFF) << 8;
                        break;
                    case 10:
                        patchentrypoint += (cc & 0xFF) << 16;
                        break;
                    case 11:
                        patchentrypoint += (cc & 0xFF) << 24;
                        if (fwcrc != temp_fwcrc) {
                            /* Set & report once, then only if CRC changes during this session (for firmware dev) */
                            fwcrc = temp_fwcrc;
                            String sFwcrc = String.format("%08X", fwcrc);
                            System.out.println(Instant.now() + String.format(" Core Firmware version: %d.%d.%d.%d, CRC: 0x%s, entry point: 0x%08X", fwversion[0], fwversion[1], fwversion[2], fwversion[3], sFwcrc, patchentrypoint));
                            LOGGER.log(Level.INFO, String.format("Core running Firmware version %d.%d.%d.%d | CRC %s\n", fwversion[0], fwversion[1], fwversion[2], fwversion[3], sFwcrc));
                            MainFrame.mainframe.setFirmwareID(sFwcrc);
                        }
                        if (currentExecutingCommand != null && currentExecutingCommand instanceof SCmdGetFWVersion) {
                            currentExecutingCommand.setCompletedWithStatus(0);
                        }
                        setIdleState();
                        break;
                    default:
                        setIdleState();
                        break;
                }
                dataIndex++;
                break;

            default:
                setIdleState();
                // System.out.println(Instant.now() + " [DEBUG] Unhandled byte c=" + String.format("%02x", c) + "(char=" + (char)c + ") in state=" + state);
                break;
        } /* End switch (state) */
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
