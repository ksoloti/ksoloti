/**
 * Copyright (C) 2015 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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
package axoloti.targetprofile;

import static axoloti.MainFrame.mainframe;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

import axoloti.USBBulkConnection;
import axoloti.utils.Preferences;

/**
 *
 * @author Johannes Taelman
 */
public class ksoloti_core {

    private static final Logger LOGGER = Logger.getLogger(ksoloti_core.class.getName());

    ByteBuffer OTP0Data;
    ByteBuffer OTP1Data;
    ByteBuffer CPUIDData;
    ByteBuffer BKPSRAMData;

    enum cputype_e {
        STM32F40xxx,
        STM32F42xxx
    };

    cputype_e cputype;

    public ByteBuffer CreateOTPInfo() {
        if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
            return CreateOTPInfo(1, 1, 0, 32); /* 32MB SDRAM */
        }
        else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
            return CreateOTPInfo(1, 1, 0, 8); /* 8MB SDRAM */
        }
        return null;
    }

    public ByteBuffer CreateOTPInfo(
            int boardtype,
            int boardmajorversion,
            int boardminorversion,
            int sdramsize
    ) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(32);

            String header = "";
            if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
                header = "Ksoloti Core";
            }
            else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
                header = "Axoloti Core";
            }

            bb.rewind();
            bb.put(header.getBytes("UTF8"));
            while (bb.position() < 16) {
                bb.put((byte)0);
            }
            bb.putInt(boardtype);
            bb.putInt(boardmajorversion);
            bb.putInt(boardminorversion);
            bb.putInt(sdramsize);

            return bb;
        }
        catch (UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, "Error: unsupported encoding: " + ex.getMessage());
            ex.printStackTrace(System.out);
            return null;
        }
    }

    public int getPatchAddr() {
        // SRAM1 - must match with ramlink_*.ld
        return 0x20011000;
    }

    public int getSDRAMAddr() {
        return 0xC0000000;
    }

    public int getSDRAMSize() {
        if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core")) {
            return  32 * 1024 * 1024;  /* 32MB */
        }
        else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core")) {
            return  8 * 1024 * 1024;  /* 8MB */
        }
        return -1;
    }

    public int getOTPAddr() {
        return 0x1FFF7800;
    }

    public int getOTPLength() {
        return 32;
    }

    public int getCPUSerialAddr() {
        return 0x1FFF7A10;
    }

    public int getCPUIDCodeAddr() {
        return 0xE0042000;
    }

    public void setCPUIDCode(int i) {
        //System.out.println(String.format("idcode = %8X", i));
        if (i==0 || (i & 0x0FFF) == 0x0419) {
            cputype = cputype_e.STM32F42xxx;
        }
        else {
            cputype = cputype_e.STM32F40xxx;
        }
    }

    cputype_e getCPUType() {
        return cputype;
    }

    public boolean hasSDRAM() {
        if (cputype == cputype_e.STM32F42xxx) {
            return true;
        }
        else {
            return false;
        }
    }

    public void setVoltages(int i) {
        //System.out.println(String.format("v%08X", i));
        int vref = i & 0xFFFF;
        int v50i = (i >> 16) & 0xFFFF;
        if (vref != 0) {
            float vdd = 1.21f * (float) (4096) / (float) (vref);
            float v50 = 2.0f * vdd * (float) (v50i + 1) / 4096.0f;
            boolean alert = false;
            if ((vdd < 3.0) || (vdd > 3.6)) {
                alert = true;
            }
            if ((v50 > 5.5) || (v50 < 4.5)) {
                alert = true;
            }
            mainframe.setVoltages(v50, vdd, alert);
        }
    }

    public int getCPUSerialLength() {
        return 12;
    }

    public int getBKPSRAMAddr() {
        return 0x40024000;
    }

    public int getBKPSRAMLength() {
        return 0x1000;
    }

    public void setOTP0Data(ByteBuffer b) {
        OTP0Data = b;
    }

    public void setOTP1Data(ByteBuffer b) {
        OTP1Data = b;
    }

    public void setCPUSerial(ByteBuffer b) {
        if (b != null) {
            CPUIDData = b.order(ByteOrder.LITTLE_ENDIAN);
            String s = "";
            b.rewind();
            while (b.remaining() > 0) {
                s = s + String.format("%08X", b.getInt());
            }
            USBBulkConnection.getInstance().ShowBoardIDName(s, null);
        }
        else {
            LOGGER.log(Level.SEVERE, "Invalid CPU serial number, invalid protocol?, update firmware",new Object());
            USBBulkConnection.getInstance().ShowBoardIDName("CFCFCFCF", null);
        }
    }

    public ByteBuffer getCPUSerial() {
        return CPUIDData.order(ByteOrder.LITTLE_ENDIAN);
    }

    public ByteBuffer getBKPSRAMData() {
        return BKPSRAMData;
    }

    public void setBKPSRAMData(ByteBuffer BKPSRAMData) {
        this.BKPSRAMData = BKPSRAMData;
    }

}
