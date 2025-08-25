/*
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2024 by Andrew Capon, Ksoloti
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

#include "boot_options.h"
#include <stdint.h>
#include "ch.h"
#include "hal.h"
#include "flash.h"
#include "sdram.h"
#include "axoloti_board.h"


#define MOUNTER_MAGIC 0x2a4d4f554e544552 /* *MOUNTER */
#define RESETER_MAGIC 0x524553455445520A /* *RESETER */

volatile uint64_t g_startup_flags __attribute__((section(".noinit")));
volatile uint64_t g_reset_flags __attribute__((section(".noinit")));

extern int mounter(void);

static void SetStartupFlags(uint64_t uValue) {
    g_startup_flags = uValue;
    NVIC_SystemReset();
}

static void ResetStartupFlags(void) {
    g_startup_flags = 0;
}

static uint64_t GetStartupFlags(void) {
    return g_startup_flags;
}

void StartMounter(void) {
    SetStartupFlags(MOUNTER_MAGIC);
}

void CheckForReset(void) {
    if (g_reset_flags == RESETER_MAGIC) {
        g_reset_flags = 0;
    }
    else {
        g_reset_flags = RESETER_MAGIC;
        NVIC_SystemReset();
    }
}

void CheckForMounterBoot(void) {
    /* Shall we run the mounter? The mounter uses the chibios we have here running from flash */
    if (GetStartupFlags() == MOUNTER_MAGIC) {
        ResetStartupFlags();
        mounter();
    }
}