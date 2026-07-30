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


#define FLASHER_MAGIC 0x2a464c4153484552 /* *FLASHER */
#define MOUNTER_MAGIC 0x2a4d4f554e544552 /* *MOUNTER */
#define RESETER_MAGIC 0x524553455445520A /* *RESETER */

volatile uint64_t g_startup_flags __attribute__((section(".noinit"), aligned(8)));
volatile uint64_t g_reset_flags __attribute__((section(".noinit"), aligned(8)));

extern int mounter(void);
extern void flasher(void);

static void SetStartupFlags(uint64_t uValue) {
    g_startup_flags = uValue;
    NVIC_SystemReset();
}

static void FRAMTEXT_CODE_SECTION ResetStartupFlags(void) {
    g_startup_flags = 0;
}

static uint64_t FRAMTEXT_CODE_SECTION GetStartupFlags(void) {
    return g_startup_flags;
}

void StartFlasher(void) {
    SetStartupFlags(FLASHER_MAGIC);
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

void FRAMTEXT_CODE_SECTION CheckForFlasherBoot(void) {
    /* Shall we run the flasher? */
    /* The flasher needs to run from RAM and does not use chibios */
    if (GetStartupFlags() == FLASHER_MAGIC) {
        ResetStartupFlags();

        configSDRAM();
        NVIC_DisableIRQ(FMC_IRQn);
        NVIC_ClearPendingIRQ(FMC_IRQn);

        /* Float USB inputs, hope the host notices detach... */
        palSetPadMode(GPIOA, 11, PAL_MODE_INPUT);
        palSetPadMode(GPIOA, 12, PAL_MODE_INPUT);

        /* Set up LEDs */
        palSetPadMode(LED1_PORT, LED1_PIN, PAL_MODE_OUTPUT_PUSHPULL);
        palSetPad(LED1_PORT, LED1_PIN);
        palSetPadMode(LED2_PORT, LED2_PIN, PAL_MODE_OUTPUT_PUSHPULL);

        /* now execute from RAM */
        FlashFirmware();
    }
}