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
#ifndef __AXOLOTI_DEFINES_H
#define __AXOLOTI_DEFINES_H

#include <stdint.h>

/* Currently supported boards:
 * Do not uncomment any of the BOARD_* defines here!
 * The defines are handled via the Patcher and passed to the Makefile and compiler
 * depending on the board setting in the Patcher preferences.
 * #define BOARD_KSOLOTI_CORE
 * #define BOARD_AXOLOTI_CORE
 * #define BOARD_STM32F4_DISCOVERY
 */

// #define FW_SPILINK 

#define PI_F 3.14159265359f
#define SAMPLERATE 48000
#define BUFSIZE 16
#define BUFSIZE_POW 4

typedef int32_t int32buffer[BUFSIZE];

#define USING_ADAU1761 1 /* Works with ADAU1961/1761/1361 so we just leave this defined */

/* Firmware version 1.0.0.4 - Ksoloti v0.4+ */
#define FWVERSION1 1
#define FWVERSION2 0
#define FWVERSION3 0
#define FWVERSION4 4

#define BOARD_OTG_NOVBUSSENS

#endif
