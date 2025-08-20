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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.usb4java.*;

import axoloti.utils.Preferences;

/**
 *
 * @author Johannes Taelman
 */
public class Usb {

    private static final Logger LOGGER = Logger.getLogger(Usb.class.getName());

    static final public short VID_STM = (short) 0x0483;
    static final public short PID_STM_DFU = (short) 0xDF11;
    static final public short PID_STM_CDC = (short) 0x5740;
    static final public short PID_STM_STLINK = (short) 0x3748;
    static final public short VID_AXOLOTI = (short) 0x16C0;
    static final public short PID_AXOLOTI = (short) 0x0442;
    static final public short PID_AXOLOTI_SDCARD = (short) 0x0443;
    static final public short PID_KSOLOTI = (short) 0x0444;
    static final public short PID_KSOLOTI_SDCARD = (short) 0x0445;
    static final public short PID_KSOLOTI_USBAUDIO = (short) 0x0446;
    static final public short PID_AXOLOTI_USBAUDIO = (short) 0x0447;

    static Context context; /* One context for all libusb operations */

    public Usb() {
    }

    public static Context getContext() {
        return context;
    }

    public static void initialize() {
        if (context == null) {
            context = new Context();
            int result = LibUsb.init(context);
            if (result != LibUsb.SUCCESS) {
                throw new LibUsbException("Unable to initialize libusb.", result);
            }
        }
    }

    public static void shutdown() {
        if (context != null) {
            LibUsb.exit(context);
        }
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

    public static void listDevices() {
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(context, list);
        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }
        try {
            LOGGER.log(Level.INFO, "Relevant USB Devices currently attached:");
            boolean hasOne = false;
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result == LibUsb.SUCCESS) {
                    if (descriptor.idVendor() == VID_STM) {
                        if (descriptor.idProduct() == PID_STM_CDC) {
                            hasOne = true;
                            LOGGER.log(Level.INFO, "* USB Serial port device");
                        }
                        else if (descriptor.idProduct() == PID_STM_DFU) {
                            hasOne = true;
                            LOGGER.log(Level.INFO, "* DFU device");
                            DeviceHandle handle = new DeviceHandle();
                            result = LibUsb.open(device, handle);
                            if (result < 0) {
                                LOGGER.log(Level.INFO, " but cannot access: {0}", LibUsb.strError(result));
                            }
                            else {
                                LOGGER.log(Level.INFO, " driver OK");
                                LibUsb.close(handle);
                                handle = null; /* Null immediately to prevent race conditions */
                            }
                        }
                        else if (descriptor.idProduct() == PID_STM_STLINK) {
                            LOGGER.log(Level.INFO, "* STM STLink");
                            hasOne = true;
                        }
                        else {
                            LOGGER.log(Level.INFO, "* other STM device:\n{0}", descriptor.dump());
                            hasOne = true;
                        }

                    }
                    else if (Preferences.getInstance().getFirmwareMode().contains("Ksoloti Core") && descriptor.idVendor() == VID_AXOLOTI && descriptor.idProduct() == PID_KSOLOTI) {
                        hasOne = true;
                        DeviceHandle handle = new DeviceHandle();
                        result = LibUsb.open(device, handle);
                        if (result < 0) {
                            LOGGER.log(Level.INFO, "* Ksoloti USB device, but cannot access: {0}", LibUsb.strError(result));
                        }
                        else {
                            LOGGER.log(Level.INFO, "* Ksoloti USB device, serial #{0}", LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber()));
                            LibUsb.close(handle);
                            handle = null; /* Null immediately to prevent race conditions */
                        }
                        LOGGER.log(Level.INFO, "  location: {0}", DeviceToPath(device));

                    }
                    else if (Preferences.getInstance().getFirmwareMode().contains("Axoloti Core") && descriptor.idVendor() == VID_AXOLOTI && descriptor.idProduct() == PID_AXOLOTI) {
                        hasOne = true;
                        DeviceHandle handle = new DeviceHandle();
                        result = LibUsb.open(device, handle);
                        if (result < 0) {
                            LOGGER.log(Level.INFO, "* Axoloti USB device, but cannot access: {0}", LibUsb.strError(result));
                        } else {
                            LOGGER.log(Level.INFO, "* Axoloti USB device, serial #{0}", LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber()));
                            LibUsb.close(handle);
                            handle = null; /* Null immediately to prevent race conditions */
                        }
                        LOGGER.log(Level.INFO, "  location: {0}", DeviceToPath(device));
                    }

                }
                else {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }
            }
            if (!hasOne) {
                LOGGER.log(Level.INFO, "None found...");
            }
        } finally {
            LibUsb.freeDeviceList(list, true);
        }
    }

    public static boolean isDFUDeviceAvailable() {
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(context, list);
        if (result < 0) {
            LOGGER.log(Level.SEVERE, "Unable to get device list");
            return false;
        }

        try {
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS) {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }
                if (descriptor.idVendor() == VID_STM && descriptor.idProduct() == PID_STM_DFU) {
                    DeviceHandle handle = new DeviceHandle();
                    result = LibUsb.open(device, handle);
                    if (result < 0) {
                        LOGGER.log(Level.SEVERE, "DFU device found but cannot access: {0}", LibUsb.strError(result));
                        switch (axoloti.utils.OSDetect.getOS()) {
                            case WIN:
                                LOGGER.log(Level.SEVERE, "Please install the WinUSB driver for the \"STM32 Bootloader\":");
                                LOGGER.log(Level.SEVERE, "Launch Zadig (http://zadig.akeo.ie/) , " +
									"select \"Options->List all devices\", select \"STM32 BOOTLOADER\", and \"replace\" the STTub30 driver with the WinUSB driver");                                break;
                            case LINUX:
                                LOGGER.log(Level.SEVERE, "Probably need to add a udev rule.");
                                break;
                            default:
                        }
                        return false;
                    }
                    else {
                        LibUsb.close(handle);
                        handle = null; /* Null immediately to prevent race conditions */
                        return true;
                    }
                }
            }
        }
        finally {
            LibUsb.freeDeviceList(list, true);
        }
        return false;
    }

    public static boolean isSerialDeviceAvailable() {
        Device d = findDevice(VID_STM, PID_STM_CDC);
        return d != null;
    }

    public static Device findDevice(short vendorId, short productId) {
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(context, list);
        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }

        try {
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS) {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }
                if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
                    return device;
                }
            }
        }
        finally {
            LibUsb.freeDeviceList(list, true);
        }

        return null;
    }

}
