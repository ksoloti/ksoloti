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

#ifndef _SYSMON_H
#define _SYSMON_H

void sysmon_init(void);
void sysmon_blink_pattern(uint32_t pattern);

/* Edit: Newer compilers may display "warning: binary constants are a GCC extension" 
   So using hex here */
// just green
#define BLINK_OK 0x55555555 /* 0b0101 0101 0101 0101 0101 0101 0101 0101 */
// green/red/green/red alternating : boot
#define BLINK_BOOT 0x99999999 /* 0b1001 1001 1001 1001 1001 1001 1001 1001 */
// green+red
#define BLINK_OVERLOAD 0xFFFFFFFF /* 0b1111 1111 1111 1111 1111 1111 1111 1111 */
// green + red slow blink
#define BLINK_ERROR 0xF5F5F5F5 /* 0b 1111 0101 1111 0101 1111 0101 1111 0101 */
// green/red overlapping alternate slow blink
#define SYNCED_ERROR 0xB4B4B4B4 /* 0b1011 0100 1011 0100 1011 0100 1011 0100 */

typedef enum
{
    ERROR_USBH_OVERCURRENT = 0,
    ERROR_OVERVOLT_50,
    ERROR_OVERVOLT_33,
    ERROR_UNDERVOLT_50,
    ERROR_UNDERVOLT_33,
    ERROR_SDRAM,
    ERROR_SDCARD,
    ERROR_CODEC_I2C
} error_flag_t ;

void setErrorFlag(error_flag_t error);
bool getErrorFlag(error_flag_t error);
void errorFlagClearAll(void);
void sysmon_disable_blinker(void);
void sysmon_enable_blinker(void);
uint16_t sysmon_getVoltage50(void);
uint16_t sysmon_getVoltage10(void);

#endif
