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

#include "axoloti_defines.h"
#include "ui.h"
#include "ch.h"
#include "hal.h"
#include "midi.h"
#include "axoloti_math.h"
#include "patch.h"
#include "sdcard.h"
#include "pconnection.h"
#include "axoloti_board.h"
#include "ff.h"
#include <string.h>

static WORKING_AREA(waThreadUI, 1172);
    static msg_t ThreadUI(void *arg) {
    (void)(arg);
#if CH_CFG_USE_REGISTRY
    chRegSetThreadName("ui");
#endif
    while (1) {
        PExTransmit();
        chThdSleepMilliseconds(2);
        PExReceive();
        chThdSleepMilliseconds(2);
    }
    return (msg_t)0;
}


void ui_init(void) {
    chThdCreateStatic(waThreadUI, sizeof(waThreadUI), UI_USB_PRIO, (void*) ThreadUI, NULL);
}


// #define LCD_COL_INDENT 5
// #define LCD_COL_EQ 91
// #define LCD_COL_VAL 97
// #define LCD_COL_ENTER 97
// #define STATUSROW 7
