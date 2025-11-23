/*
 * Copyright (C) 2013, 2014 Johannes Taelman
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

#include "ch.h"
#include "hal.h"
#include "chprintf.h"
#include "shell.h"
#include "patch.h"
#include "sdcard.h"
#include "exceptions.h"
#include "ff.h"
#include <string.h>

#define POLLING_INTERVAL                10
#define POLLING_DELAY                   10

static const char* sram3_suffix = "_sram3";

FATFS SDC_FS; /* FS object */
bool_t fs_ready = FALSE; /* FS mounted and ready */

uint32_t fbuff[256] __attribute__((section (".sram2"))); /* Generic large buffer */

static VirtualTimer tmr; /* Card monitor timer */
static unsigned cnt; /* Debounce counter */
static EventSource inserted_event, removed_event; /* Card event sources */

#if 1

/* Insertion monitor timer callback function */
static void tmrfunc(void* p) {
    BaseBlockDevice* bbdp = p;
    chSysLockFromIsr();

    if (cnt > 0) {
        if (blkIsInserted(bbdp)) {
            if (--cnt == 0) {
                chEvtBroadcastI(&inserted_event);
            }
        }
        else {
            cnt = POLLING_INTERVAL;
        }
    }
    else {
        if (!blkIsInserted(bbdp)) {
            cnt = POLLING_INTERVAL;
            chEvtBroadcastI(&removed_event);
        }
    }
    chVTSetI(&tmr, MS2ST(POLLING_DELAY), tmrfunc, bbdp);
    chSysUnlockFromIsr();
}


/* Polling monitor start */
static void tmr_init(void* p) {
    chEvtInit(&inserted_event);
    chEvtInit(&removed_event);
    chSysLock();
    cnt = POLLING_INTERVAL;
    chVTSetI(&tmr, MS2ST(POLLING_DELAY), tmrfunc, p);
    chSysUnlock();
}


/* Card insertion event */
static void InsertHandler(eventid_t id) {
    FRESULT err;
    (void) id;

    /* On insertion SDC initialization and FS mount */
    if (fs_ready) {
        sdcDisconnect(&SDCD1);
        fs_ready = FALSE;
    }

    if (sdcConnect(&SDCD1)) {
        return;
    }

    err = f_mount(&SDC_FS, "", 0); 
    if (err != FR_OK) {
        sdcDisconnect(&SDCD1);
        return;
    }
    fs_ready = TRUE;
}


/* Card removal event */
static void RemoveHandler(eventid_t id) {
    (void) id;
    sdcDisconnect(&SDCD1);
    fs_ready = FALSE;
}

#endif

void sdcard_init(void) {
    palSetPadMode(GPIOC, 8, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_MID2);
    palSetPadMode(GPIOC, 9, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_MID2);
    palSetPadMode(GPIOC, 10, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_MID2);
    palSetPadMode(GPIOC, 11, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_MID2);
    palSetPadMode(GPIOC, 12, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_MID2);
    palSetPadMode(GPIOD, 2, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_MID2);
    chThdSleepMicroseconds(1);
    sdcStart(&SDCD1, NULL);
    chThdSleepMilliseconds(10);
}


void sdcard_attemptMountIfUnmounted() {
    if (fs_ready) {
        return;
    }
    InsertHandler(0);
}


void sdcard_unmount(void){
    RemoveHandler(0);
}


int sdcard_loadPatch1(char* fname) {
    FIL FileObject;
    FRESULT err;
    uint32_t bytes_read;

    StopPatch();

    // LogTextMessage("load %s", fname);

    /* Change working directory */
    int i = 0;
    for (i=strlen(fname); i; i--){
        if (fname[i] == '/') {
            break;
        }
    }

    if (i > 0) {
        fname[i] = 0;
        // LogTextMessage("chdir %s", fname);
        err = f_chdir(fname);
        if (err != FR_OK) {
            report_fatfs_error(err, fname);
            return -1;
        }
        fname = &fname[i + 1];
    }
    else {
        f_chdir("/");
    }

    err = f_open(&FileObject, fname, FA_READ | FA_OPEN_EXISTING);
    if (err != FR_OK) {
        report_fatfs_error(err, fname);
        return -1;
    }

    /* Previous write errors may have created a bin file with 0 bytes size.
     * loading this corrupted bin would cause a crash or even cause problems
     * with mounting the SD card (and you need to mount it to fix the corrupted bin).
     */
    uint32_t size = f_size(&FileObject);
    if (size < 128) { /* Arbitrary size, just needs to be smaller than the smallest possible bin size */
        report_fatfs_error(FR_INVALID_OBJECT, fname);
        return -1;
    }

    err = f_read(&FileObject, (uint8_t*) PATCHMAINLOC, 0xE000, (void*) &bytes_read);
    if (err != FR_OK) {
        report_fatfs_error(err, fname);
        return -1;
    }

    err = f_close(&FileObject);
        if (err != FR_OK) {
        report_fatfs_error(err, fname);
        return -1;
    }

    /* If a .bin_sram3 file exists, load it into SRAM3 */
    char* fname_sram3 = strcat(fname, sram3_suffix);

    err = f_open(&FileObject, fname_sram3, FA_READ | FA_OPEN_EXISTING);
    if (err != FR_OK) {
        // report_fatfs_error(err, fname_sram3);
        return 0; /* Simply does not exist, no error */
    }

    size = f_size(&FileObject);
    if (size < 1) { /* Minimum size, just needs to be smaller than the smallest possible bin size */
        // report_fatfs_error(FR_INVALID_OBJECT, fname_sram3);
        return 0; /* No error */
    }

    err = f_read(&FileObject, (uint8_t*) PATCHMAINLOC_SRAM3, PATCHFLASHSIZE_SRAM3, (void*) &bytes_read);
    if (err != FR_OK) {
        report_fatfs_error(err, fname_sram3);
        return -1;
    }

    err = f_close(&FileObject);
        if (err != FR_OK) {
        report_fatfs_error(err, fname_sram3);
        return -1;
    }

    return 0;
}
