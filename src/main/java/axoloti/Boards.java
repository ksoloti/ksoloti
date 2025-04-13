package axoloti;

import java.io.File;
import java.util.HashMap;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import axoloti.utils.OSDetect;
import qcmds.QCmdShellTask;

import static axoloti.MainFrame.mainframe;
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
import static axoloti.usb.Usb.isDFUDeviceAvailable;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import static axoloti.utils.OSDetect.getOS;

public class Boards {

    private static final Logger LOGGER = Logger.getLogger(QCmdShellTask.class.getName());

    public enum BoardType {
        Ksoloti("Ksoloti Core"),
        KsolotiGeko("Ksoloti Core Geko"),
        Axoloti("Axoloti Core"),
        Unknown("Unknown");

        private final String name;

        private BoardType(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }

        static public BoardType fromString(String name) {
            for (BoardType b : values()) {
                if (b.name.equals(name)) {
                    return b;
                }
            }
            return null;
        }
    }

    public enum FirmwareType {
        Normal("Normal"),
        SPILink("SPI Link"),
        USBAudio("USB Audio"),
        i2SCodec("i2S Codec");

        private final String name;

        private FirmwareType(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }

        static public FirmwareType fromString(String name) {
            for (FirmwareType f : values()) {
                if (f.name.equals(name)) {
                    return f;
                }
            }
            return null;
        }
    }

    public enum BoardMode {
        NormalFirmware("Normal Firmware"),
        USBAudioFirmware("USB Audio Firmware"),
        SDCard("SDCard"),
        DFU("DFU"),
        Unknown("Unknown");

        private final String name;

        private BoardMode(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }

        static public BoardMode fromString(String name) {
            for (BoardMode b : values()) {
                if (b.name.equals(name)) {
                    return b;
                }
            }
            return null;
        }
    }

    public enum SampleRateType {
        Rate48K("48K"),
        Rate96K("96K");

        private final String name;

        private SampleRateType(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }

        static public SampleRateType fromString(String name) {
            for (SampleRateType s : values()) {
                if (s.name.equals(name)) {
                    return s;
                }
            }
            return null;
        }
    }

    public enum MemoryLayoutType {
        Code64Data64("64 KB Code and 64 KB Data"),
        Code256Data64("256 KB Code and 64 KB Data."),
        Code256Shared("256 KB shared Code and Data."),
        NA("N/A");

        private final String name;

        private MemoryLayoutType(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }

        static public MemoryLayoutType fromString(String name) {
            for (MemoryLayoutType s : values()) {
                if (s.name.equals(name)) {
                    return s;
                }
            }
            return null;
        }
    }

    public static class BoardDetail {
        @Element(required = true)
        public String serialNumber;

        @Element(required = true)
        public FirmwareType firmwareType;

        @Element(required = true)
        public Integer dspSafetyLimit;

        @Element(required = false)
        public String name;

        @Element(required = true)
        public BoardType boardType;

        @Element(required = true)
        public Boolean needsUpdate;

        @Element(required = true)
        public BoardMode boardMode;

        @Element(required = false)
        public MemoryLayoutType memoryLayout;

        @Element(required = false)
        public boolean isConnected;

        @Element(required = false)
        public String path;


        BoardDetail() {
        }

        BoardDetail(BoardType type, BoardMode mode, String cpuId) {
            boardMode = mode;
            serialNumber = cpuId;
            firmwareType = FirmwareType.Normal;
            dspSafetyLimit = 3;
            name = "";
            needsUpdate = false;
            path = "";
            isConnected = false;

            if (cpuId.length() == 25) {
                // Valid serial for 1.1 and later
                char designator = cpuId.charAt(24);
                switch (designator) {
                    case 'A':
                        boardType = BoardType.Axoloti;
                        break;
                    case 'G':
                        boardType = BoardType.KsolotiGeko;
                        break;
                    case 'K':
                        boardType = BoardType.Ksoloti;
                        break;
                    default:
                        boardType = BoardType.Unknown;
                        needsUpdate = true;
                        break;
                }
            } else {
                // This does not have a valid serial for 1.1 and later
                // Gekos will always have 1.1 or later installed.
                boardType = BoardType.Unknown;
                needsUpdate = true;
            }

            if (BoardIs(BoardType.KsolotiGeko)) {
                memoryLayout = MemoryLayoutType.Code64Data64;
            }

            if(boardType == BoardType.Unknown) {
                // use vid pid
                boardType = type;
            }
        }

        boolean BoardIs(BoardType board) {
            return boardType == board;
        }

        void setMode(BoardMode mode) {
            boardMode = mode;
        }

        public String toString() { 
            if(name != null) {
                return name + " (" + boardType.toString() + ", " + firmwareType.toString() + ")"; 
            } else {
                return serialNumber + " (" + boardType.toString() + ", " + firmwareType.toString() + ")"; 
            }
        }
    }


    @ElementMap(required = false, entry = "BoardDetails", key = "cpuId", attribute = true, inline = true)
    HashMap<String, BoardDetail> BoardDetails;

    @Element(required = false)
    String selectedBoardSerial;

    public Boards() {
        BoardDetails = new HashMap<String, BoardDetail>();        
    }

    
    public BoardDetail getBoardDetail(String cpuId) {
        BoardDetail boardDetail = null;

        if(cpuId != null){
            boardDetail = BoardDetails.get(cpuId);
        }

        return boardDetail;
    }

    public HashMap<String, BoardDetail> getBoardDetails() {
        return BoardDetails;
    }

    public String getSelectedBoardSerialNumber() {
        return selectedBoardSerial;
    }

    public BoardDetail getSelectedBoardDetail() {
        return getBoardDetail(selectedBoardSerial);
    }

    public void setSelectedBoard(BoardDetail board) {
        selectedBoardSerial = board.serialNumber;
    }

    public void deleteBoardDetail(String cpuId) {
        BoardDetails.remove(cpuId);
    }

    public boolean addBoardDetail(String cpuId, BoardDetail boardDetail) {
        boolean result = false;
        
        if(cpuId != null && boardDetail!= null && !BoardDetails.containsKey(cpuId)){
            BoardDetails.put(cpuId, boardDetail);
            result = true;
        }
        return result;
    }

    public boolean addBoardOrUpdateBoardMode(BoardType type, BoardMode mode, String cpuId, String path) {
        boolean result = false;

        if(cpuId != null) {
            if(mode == BoardMode.DFU) {

            } else if(mode == BoardMode.SDCard) {

            }

            if (cpuId.length() == 25) {
                // This is a new style id, check to see if we need to replace the entry with the old style key
                // after it has been DFU updated.
                String oldStyleCpuId = cpuId.substring(0, 24);
                if(BoardDetails.containsKey(oldStyleCpuId)) {
                    // Ok lets delete this one
                    deleteBoardDetail(oldStyleCpuId);

                    // if it was selected choose the new one
                    if (selectedBoardSerial.equals(oldStyleCpuId)) {
                        selectedBoardSerial = cpuId;
                    }
                }
            }

            if(BoardDetails.containsKey(cpuId)){
                // update mode
                result = true; // Already exists so return true

                BoardDetail boardDetail = BoardDetails.get(cpuId);
                boardDetail.setMode(mode);
                boardDetail.path = path;
                boardDetail.isConnected = true;
            } else {
                // add new
                BoardDetail boardDetail = new BoardDetail(type, mode, cpuId);
                boardDetail.path = path;
                boardDetail.isConnected = true;
                result = addBoardDetail(cpuId, boardDetail);
            }
        }

        return result;
    }


    BoardMode getBoardModeFromDescriptor(DeviceDescriptor descriptor) {
        BoardMode mode = BoardMode.Unknown;

        if ((descriptor.idVendor() == VID_STM) && (descriptor.idProduct() == PID_STM_DFU)) {
            // DFU Mode
            mode = BoardMode.DFU;
        } if (descriptor.idVendor() == VID_AXOLOTI) {
            // Valid Vid
            if ((descriptor.idProduct() == PID_KSOLOTI) || (descriptor.idProduct() == PID_AXOLOTI)) {
                // Ksoloti/Axoloti running normal firmware
                mode = BoardMode.NormalFirmware;
            }else if ((descriptor.idProduct() == PID_KSOLOTI_USBAUDIO) || (descriptor.idProduct() == PID_AXOLOTI_USBAUDIO)) {
                // Ksoloti/Axoloti running USB Audio firmware
                mode = BoardMode.USBAudioFirmware;
            } else if ((descriptor.idProduct() == PID_AXOLOTI_SDCARD) || (descriptor.idProduct() == PID_KSOLOTI_SDCARD)) {
                // Axoloti Derivative running mounter
                mode = BoardMode.SDCard;
            };
        }

        return mode;
    }

    BoardType getBoardTypeFromDescriptor(DeviceDescriptor descriptor) {
        BoardType type = BoardType.Unknown;

        if (descriptor.idVendor() == VID_AXOLOTI) {
            if ((descriptor.idProduct() == PID_KSOLOTI) || (descriptor.idProduct() == PID_KSOLOTI_USBAUDIO) || (descriptor.idProduct() == PID_KSOLOTI_SDCARD)) {
                type = BoardType.Ksoloti;
            } else if ((descriptor.idProduct() == PID_AXOLOTI) || (descriptor.idProduct() == PID_AXOLOTI_USBAUDIO) || (descriptor.idProduct() == PID_AXOLOTI_SDCARD)) {
                type = BoardType.Axoloti;
            }
        }

        return type;
    }

    String getUsbSerial(Device device, DeviceDescriptor descriptor) {
        String serial = null;

        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        if (result < 0) {
            LOGGER.log(Level.SEVERE, "Failed to open USB device {0} - {1} : {2}\n", new Object[]{descriptor.idVendor(), descriptor.idProduct(), ErrorString(result)});
        } else {
            serial = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());
            LibUsb.close(handle);
        }

        return serial;
    }

    public DeviceHandle getDeviceHandleForBoard(BoardDetail boardDetail) {
        DeviceHandle deviceHandle = null;

        if(boardDetail.isConnected) {
            // There must be a better way!
            int result = LibUsb.init(null); // TODOH7 loook at all these inits

            DeviceList list = new DeviceList();
            LibUsb.getDeviceList(null, list);

            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result == LibUsb.SUCCESS) {
    
                    BoardMode boardMode = getBoardModeFromDescriptor(descriptor);
    
                    if(boardMode != BoardMode.Unknown) {
                        String serial = getUsbSerial(device, descriptor);
    
                        if(serial != null) {
                            if(serial.equals(boardDetail.serialNumber)) {
                                deviceHandle = new DeviceHandle();
                                if (LibUsb.open(device, deviceHandle) < 0) {
                                    LOGGER.log(Level.SEVERE, "Failed to open USB device {0} - {1} : {2}\n", new Object[]{descriptor.idVendor(), descriptor.idProduct(), ErrorString(result)});
                                    deviceHandle = null;
                                }
                            }
                        }
                    } 
                } else {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }            
            }
        } 

        return deviceHandle;
    }

    public void scanBoards() {
        // first set all boards to disconnected
        for(BoardDetail board : BoardDetails.values()) {
            board.isConnected = false;
        }

        // Remove any DFU boards
        BoardDetails.entrySet().removeIf(e -> e.getValue().boardMode == BoardMode.DFU);

        DeviceList list = new DeviceList();

        int result = LibUsb.init(null); // TODOH7 loook at all these inits

        if (result < 0) {
            throw new LibUsbException("Unable to initialize LibUsb context", result);
        }

        result = LibUsb.getDeviceList(null, list);

        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }

        for (Device device : list) {
            DeviceDescriptor descriptor = new DeviceDescriptor();
            
            result = LibUsb.getDeviceDescriptor(device, descriptor);
            if (result == LibUsb.SUCCESS) {

                BoardMode boardMode = getBoardModeFromDescriptor(descriptor);
                BoardType boardType = getBoardTypeFromDescriptor(descriptor);
                if(boardMode != BoardMode.Unknown) {
                    String serial = getUsbSerial(device, descriptor);

                    if(serial != null) {
                        String path = DeviceToPath(device);
                        addBoardOrUpdateBoardMode(boardType, boardMode, serial, path);
                    }
                }
            } else {
                throw new LibUsbException("Unable to read device descriptor", result);
            }            
        }

        // If we have never had a selected board then just choose the first one
        // in a convoluted java way!
        if(selectedBoardSerial == null && BoardDetails.size() > 0) {
            HashMap.Entry<String,BoardDetail> board = BoardDetails.entrySet().iterator().next();
            selectedBoardSerial = board.getKey();
        }

        // Save to disk
        MainFrame.prefs.SavePrefs(false);
    }

    private static String ErrorString(int result) {
        if (result < 0) {

            if (getOS() == OSDetect.OS.WIN) {
                if (result == LibUsb.ERROR_NOT_FOUND) {
                    return "Inaccessible: driver not installed, You may need to install a compatible driver using Zadig. More info at https://ksoloti.github.io/3-4-rescue_mode.html#zadig_bootloader";
                }
                else if (result == LibUsb.ERROR_ACCESS) {
                    return "Inaccessible: busy?";
                }
            }
            else if (getOS() == OSDetect.OS.LINUX) {

                if (result == LibUsb.ERROR_ACCESS) {
                    return "Insufficient permissions, You may need to add permissions by running platform_linux/add_udev_rules.sh. More info at https://ksoloti.github.io/3-install.html#linux_permissions";
                }
            }
            return "Inaccessible: " + result; /* Mac OS default, fallthrough for Windows and Linux */
        }
        else {
            return null;
        }
    }


    public BoardType getBoardType() {
        BoardDetail boardDetail = getSelectedBoardDetail();
        if(boardDetail != null) {
            return boardDetail.boardType;
        } else {
            return BoardType.Unknown;
        }
    }

    public boolean isKsolotiDerivative() {
        BoardType boardType = getBoardType();

        return (boardType == BoardType.Ksoloti) || (boardType == BoardType.KsolotiGeko);
    }

    public boolean isAxolotiDerivative() {
        BoardType boardType = getBoardType();

        return (boardType == BoardType.Axoloti);
    }

    public FirmwareType getFirmware() {
        BoardDetail boardDetail = getSelectedBoardDetail();
        if(boardDetail != null) {
            return boardDetail.firmwareType;
        } else
        {
            return FirmwareType.Normal;
        }
    }

    public SampleRateType getSampleRate() {
        return SampleRateType.Rate48K; // Only 48K supported at the moment
    }

    public MemoryLayoutType getMemoryLayout() {
        BoardDetail boardDetail = getSelectedBoardDetail();
        if(boardDetail != null) {
            return boardDetail.memoryLayout;
        } else {
            return MemoryLayoutType.NA;
        }
    }

    public String getFirmwareBinFilename(boolean bFullPath) {
        String name;
        if(bFullPath) 
            name = System.getProperty(Axoloti.FIRMWARE_DIR) + File.separator + "build" + File.separator;
        else
            name = "";

        switch(getBoardType())
        {
            case BoardType.Ksoloti:     name += "ksoloti"; break;
            case BoardType.KsolotiGeko: name += "ksoloti_h743"; break;
            case BoardType.Axoloti:     name += "axoloti"; break;
            case BoardType.Unknown:     name += "unknown"; break;
        } 

        switch(getFirmware())
        {
            case Normal:   break;
            case SPILink:  name += "_spilink"; break;
            case USBAudio: name += "_usbaudio"; break;
            case i2SCodec: name += "_i2scodec"; break;
        }

        name += ".bin";

        return name;
    }

    // default gets full path
    public String getFirmwareBinFilename() {
        return getFirmwareBinFilename(true);
    }
        
    public String getPatchCompilerOptions() {
        String options = "";
        switch(getBoardType())
        {
            case BoardType.Ksoloti:     options += "BOARD_KSOLOTI_CORE BOARD_KSOLOTI_CORE_F427 "; break;
            case BoardType.KsolotiGeko: options += "BOARD_KSOLOTI_CORE BOARD_KSOLOTI_CORE_H743 "; break;
            case BoardType.Axoloti:     options += "BOARD_AXOLOTI_CORE BOARD_AXOLOTI_CORE "; break;
            case BoardType.Unknown:     options += " "; break;
        } 

        if(isAxolotiDerivative()) {
            options += " ramlink_axoloti.ld";
        } else if(getBoardType() == BoardType.Ksoloti) {
            options += " ramlink_ksoloti.ld";
        } else if(getBoardType() == BoardType.KsolotiGeko) {
            switch(getMemoryLayout()) {
                case Code256Data64:
                    options += " ramlink_ksoloti_h743_sram_dtcm.ld";
                    break;
                case Code256Shared:
                    options += " ramlink_ksoloti_h743_sram_sram.ld";
                    break;
                case Code64Data64:
                    options += " ramlink_ksoloti_h743_itcm_dtcm.ld";
                    break;
                default:
                    break;
                
            }
        }

        switch(getFirmware())
        {
            case Normal:   break;
            case SPILink:  options += " FW_SPILINK"; break;
            case USBAudio: options += " FW_USBAUDIO"; break;
            case i2SCodec: options += " FW_I2SCODEC"; break;
        }

        return options;
    }

    public int getDfuCount() {
        scanBoards();

        int dfuCount = 0;

        for(BoardDetail board : BoardDetails.values()) {
            if(board.boardMode == BoardMode.DFU) {
                dfuCount++;
            }
            board.isConnected = false;
        }

        return dfuCount;
    }

    public BoardDetail getDfuBoard() {
        BoardDetail result = null;

        for(BoardDetail board : BoardDetails.values()) {
            if(board.boardMode == BoardMode.DFU) {
                result = board;
                break;
            }
        }
        
        return result;
    }

    public void selectBoardAfterDfu() {
        BoardDetail boardDetails = getSelectedBoardDetail();

        if(boardDetails.boardMode == BoardMode.DFU) {
            String path = boardDetails.path;

            scanBoards();
            selectedBoardSerial = "";
            for(BoardDetail board : BoardDetails.values()) {
                if(board.path.equals(path)) {
                    selectedBoardSerial = board.serialNumber;
                    break;
                }
            }

            mainframe.prefs.SavePrefs(false);
        }
    }

    public void setBoardName(String cpuId, String name) {
        BoardDetail boardDetail = getBoardDetail(cpuId);
        if(boardDetail != null) {
            boardDetail.name = name;
        }
    }

    public void setFirmwareType(String cpuId, FirmwareType firmwareType) {
        BoardDetail boardDetail = getBoardDetail(cpuId);
        if(boardDetail != null) {
            boardDetail.firmwareType = firmwareType;
        }
    }

    public void setMemoryLayout(String cpuId, MemoryLayoutType memoryLayout) {
        BoardDetail boardDetail = getBoardDetail(cpuId);
        if(boardDetail != null) {
            boardDetail.memoryLayout = memoryLayout;
        }
    }

    public void setDspSafetyLimit(String cpuId, int dspSafetyLimit) {
        BoardDetail boardDetail = getBoardDetail(cpuId);
        if(boardDetail != null) {
            boardDetail.dspSafetyLimit = dspSafetyLimit;
        }
    }

    public int getBulkInterfaceNumber() {
        BoardDetail boardDetail = getSelectedBoardDetail();
        if(boardDetail != null) {
            return (boardDetail.boardMode == BoardMode.NormalFirmware) ? 2 : 4;
        } else {
            return 2; // Normal firmware default, should never happen
        }
    }
}
