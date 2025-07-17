/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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

package axoloti.utils;

/**
 *
 * @author Johannes Taelman
 */
public final class OSDetect {

    public enum OS {
        WIN, MAC, LINUX, UNKNOWN
    };

    public enum ARCH {
        X86_64, AARCH64, UNKNOWN
    };

    private static OS os = OS.UNKNOWN;
    private static ARCH arch = ARCH.UNKNOWN;

    public static OS getOS() {
        if (os == OS.UNKNOWN) {
            String osname = System.getProperty("os.name").toLowerCase();
            if (osname.contains("win")) {
                os = OS.WIN;
            }
            else if (osname.contains("mac")) {
                os = OS.MAC;
            }
            else if (osname.contains("nux")) {
                os = OS.LINUX;
            }
        }
        return os;
    }

    public static ARCH getArch() {
        if (arch == ARCH.UNKNOWN) {
            String archname = System.getProperty("os.arch").toLowerCase();
            if (archname.contains("aarch64")) {
                arch = ARCH.AARCH64;
            }
            else if (archname.contains("x86_64")) { 
                arch = ARCH.X86_64;
            }
        }
        return arch;
    }
}