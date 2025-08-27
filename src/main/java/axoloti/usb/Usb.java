/**
 * Copyright (C) 2015 Johannes Taelman
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
package axoloti.usb;


import java.nio.ByteBuffer;
import org.usb4java.*;

import axoloti.USBBulkConnection;

/**
 *
 * @author Johannes Taelman
 */
public class Usb {

    public final static short VID_STM = (short) 0x0483;
    public final static short PID_STM_DFU = (short) 0xDF11;
    public final static short PID_STM_CDC = (short) 0x5740;
    public final static short PID_STM_STLINK = (short) 0x3748;
    public final static short VID_AXOLOTI = (short) 0x16C0;
    public final static short PID_AXOLOTI = (short) 0x0442;
    public final static short PID_AXOLOTI_SDCARD = (short) 0x0443;
    public final static short PID_KSOLOTI = (short) 0x0444;
    public final static short PID_KSOLOTI_SDCARD = (short) 0x0445;
    public final static short PID_KSOLOTI_USBAUDIO = (short) 0x0446;
    public final static short PID_AXOLOTI_USBAUDIO = (short) 0x0447;
    public final static short bulkVID = (short) 0x16C0;
    public final static short bulkPIDAxoloti = (short) 0x0442;
    public final static short bulkPIDAxolotiUsbAudio = (short) 0x0447;
    public final static short bulkPIDKsoloti = (short) 0x0444;
    public final static short bulkPIDKsolotiUsbAudio = (short) 0x0446;

    public Usb() {
    }

    public static String DeviceToPath(Device device) {
        ByteBuffer path = ByteBuffer.allocateDirect(10);
        int n = LibUsb.getPortNumbers(device, path);
        String paths = "";
        for (int i = 0; i < n; i++) {
            paths += ":" + path.get(i);
        }
        return paths;
    }

    public static boolean isSerialDeviceAvailable() {
        Device d = USBBulkConnection.findDevice(VID_STM, PID_STM_CDC);
        return d != null;
    }
}
