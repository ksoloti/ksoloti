/*
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
#include "pconnection.h"
#include "parameters.h"
#include "patch.h"
#include "codec.h"
#include "usbcfg.h"
#include "midi.h"
#include "sdcard.h"
#include "ui.h"
#include "string.h"
#include "flash.h"
#include "exceptions.h"
#include "crc32.h"
#include "watchdog.h"
#include "usbcfg.h"
#include "bulk_usb.h"
#include "midi.h"
#include "midi_usb.h"
#include "watchdog.h"
#include "sysmon.h"
#include "ff.h"

#ifdef FW_SPILINK
#include "spilink.h"
#endif

#include "stdio.h"
#include "memstreams.h"

//#define DEBUG_SERIAL

// If you want more concurrent messages you can set this larger.
#define LOG_BUFFER_SIZE (256)

static uint32_t fwid;
static uint8_t AckPending = 0;
static uint8_t connected = 0;
static char FileName[256];

static FIL pFile;
static int32_t pFileSize;


MUTEX_DECL(LogMutex);
char    LogBuffer[LOG_BUFFER_SIZE];
uint8_t LogBufferUsed = 0;

connectionflags_t connectionFlags;

static WORKING_AREA(waThreadUSBDMidi, 256);


#define PC_DBG_COUNT (0)
#if PC_DBG_COUNT
typedef struct _PCDebug {
    uint8_t c;
    int state;
} PCDebug;

PCDebug dbg_received[PC_DBG_COUNT] __attribute__((section (".sram3")));
uint16_t uCount = 0;

void AddPCDebug(uint8_t c, int state) {
    dbg_received[uCount].c = c;
    dbg_received[uCount].state = state;
    uCount++;
    if (uCount == PC_DBG_COUNT) {
        uCount = 0;
    }
}
#else
#define AddPCDebug(a,b)
#endif


__attribute__((noreturn)) static msg_t ThreadUSBDMidi(void* arg) {
    (void)arg;

#if CH_CFG_USE_REGISTRY == TRUE
    chRegSetThreadName("usbdmidi");
#endif

    uint8_t r[4];

    while (1) {
        chnReadTimeout(&MDU1, &r[0], 4, TIME_INFINITE);
        MidiInMsgHandler(MIDI_DEVICE_USB_DEVICE, ((r[0] & 0xF0) >> 4) + 1, r[1], r[2], r[3]);
    }
}


void uint32_to_le_bytes(uint32_t value32, char* dest) {
    dest[0] = ((value32 >>  0) & 0xFF);
    dest[1] = ((value32 >>  8) & 0xFF);
    dest[2] = ((value32 >> 16) & 0xFF);
    dest[3] = ((value32 >> 24) & 0xFF);
}


uint32_t le_bytes_to_uint32(const uint8_t* src) {
    return (uint32_t) (src[0] | (src[1] << 8) | (src[2] << 16) | (src[3] << 24));
}


void InitPConnection(void) {

    extern int32_t _flash_end;
    fwid = CalcCRC32((uint8_t*) (FLASH_BASE_ADDR), (uint32_t) (&_flash_end) & 0x07FFFFF);

    /* Initializes a serial-over-USB CDC driver. */
    mduObjectInit(&MDU1);
    mduStart(&MDU1, &midiusbcfg);
    bduObjectInit(&BDU1);
    bduStart(&BDU1, &bulkusbcfg);

    /*
     * Activates the USB driver and then the USB bus pull-up on D+.
     * Note, a delay is inserted in order to not have to disconnect the cable
     * after a reset.
     */
    usbDisconnectBus(midiusbcfg.usbp);
    chThdSleepMilliseconds(1000);
    usbStart(midiusbcfg.usbp, &usbcfg);
    usbConnectBus(midiusbcfg.usbp);

    connectionFlags.value = 0;
#if FW_USBAUDIO
    connectionFlags.usbBuild = 1;
#endif

    chMtxObjectInit(&LogMutex);
    chThdCreateStatic(waThreadUSBDMidi, sizeof(waThreadUSBDMidi), MIDI_USB_PRIO, (void*) ThreadUSBDMidi, NULL);
}


int GetFirmwareID(void) {
    return fwid;
}


void TransmitDisplayPckt(void) {
    if (patchMeta.pDisplayVector == 0) {
        return;
    }

    unsigned int length = 12 + (patchMeta.pDisplayVector[2] * 4);
    if (length > 2560) {
        return; // FIXME
    }
    chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) &patchMeta.pDisplayVector[0], length);
}


void LogTextMessage(const char* format, ...) {
    if ((usbGetDriverStateI(BDU1.config->usbp) == USB_ACTIVE) && (connected)) {
        if(chMtxTryLock(&LogMutex)) {
            MemoryStream ms;
            uint8_t      tmp[LOG_BUFFER_SIZE-5]; /* nead "AxoL" and '\0' */

            msObjectInit(&ms, (uint8_t*) tmp, LOG_BUFFER_SIZE-5, 0); 

            va_list ap;
            va_start(ap, format);
            chvprintf((BaseSequentialStream*) &ms, format, ap);
            va_end(ap);
            chSequentialStreamPut(&ms, 0);

            size_t length = strlen((char*) tmp);
            if((length) && (LogBufferUsed + 4 + length + 1) < LOG_BUFFER_SIZE) {
                LogBuffer[LogBufferUsed++] = 'A';
                LogBuffer[LogBufferUsed++] = 'x';
                LogBuffer[LogBufferUsed++] = 'o';
                LogBuffer[LogBufferUsed++] = 'L';

                memcpy(&LogBuffer[LogBufferUsed], tmp, length+1);
                LogBufferUsed += length+1;
            }
            chMtxUnlock(&LogMutex);
        }
    }
}


void PExTransmit(void) {
    if (!chOQIsEmptyI(&BDU1.oqueue)) {
        chThdSleepMilliseconds(1);
        BDU1.oqueue.q_notify(&BDU1.oqueue);
    }
    else {
        if(chMtxTryLock(&LogMutex)) {
            if(LogBufferUsed) {
                chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) LogBuffer, LogBufferUsed);
                LogBufferUsed = 0;
            }
            chMtxUnlock(&LogMutex);
        }

        if (AckPending) {
            uint32_t ack[7];
            ack[0] = 0x416F7841; /* "AxoA" */
            ack[1] = connectionFlags.value; // flags for overload, USB audio etc
            ack[2] = dspLoad200;
            ack[3] = patchMeta.patchID;
            ack[4] = sysmon_getVoltage10() + (sysmon_getVoltage50()<<16);

            if (patchStatus) {
                ack[5] = UNINITIALIZED;
            }
            else {
                ack[5] = loadPatchIndex;
            }
            ack[6] = fs_ready;
            chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) &ack[0], 7 * 4);

            connectionFlags.dspOverload = false; /* clear overload flag */

            if (patchStatus == RUNNING) {
                TransmitDisplayPckt();
            }

            connected = 1;
            exception_checkandreport();
            AckPending = 0;
        }

        if (patchStatus == RUNNING) {
            uint16_t i;
            for (i = 0; i < patchMeta.numPEx; i++) {
                if (patchMeta.pPExch[i].signals & 0x01) {
                    int v = (patchMeta.pPExch)[i].value;
                    patchMeta.pPExch[i].signals &= ~0x01;
                    PExMessage pex_msg;
                    pex_msg.header = 0x506F7841; /* "AxoP" */
                    pex_msg.patchID = patchMeta.patchID;
                    pex_msg.index = i;
                    pex_msg.value = v;
                    chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) &pex_msg, sizeof(pex_msg));
                }
            }
        }
    }
}


static void send_AxoResult(char cmd_byte, uint8_t status) {
    /* Send command response: AxoR<command_byte><status_byte> 
       Required by Patcher to mark operations as completed and see if they were successful.
       Currently the following commands require an AxoR<c><s> response:
       - Create directory    AxoR<k><status>
       - Change directory    AxoR<h><status>
       - Create file         AxoR<f><status>
       - Append to file      AxoR<a><status>
       - Close file          AxoR<c><status>
       - Delete file         AxoR<D><status>
       - Get file list       AxoR<l><status>
       - Get file info       AxoR<I><status>
       - Start memory write  AxoR<W><status>
       - Append to memory    AxoR<w><status>
       - End memory write    AxoR<e><status>
       - Start patch         AxoR<s><status>
       - Stop patch          AxoR<S><status>
       - Copy patch to flash AxoR<F><status> */
    char res_msg[6];
    res_msg[0] = 'A'; res_msg[1] = 'x'; res_msg[2] = 'o'; res_msg[3] = 'R';
    res_msg[4] = cmd_byte;
    res_msg[5] = (char) status;
    chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) res_msg, 6);
}


/* Do not warn about 'strcpy' accessing between 1 and 2147483646 bytes at offsets 76 and 1 may overlap up to 2147483571 bytes at offset [2147483646, 76] [-Wrestrict] */
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wrestrict"
static FRESULT scan_files(char* path) {
    /* Recursive scan of all items in a directory */

    FRESULT op_result;
    static FILINFO fno;
    DIR dir;

    uint32_t current_path_len;
    char* fname;
    char* msg = (char*) &fbuff[128]; /* 128 ints = 512 bytes offset */

    fno.lfname = &FileName[0];
    fno.lfsize = sizeof(FileName);

    op_result = f_opendir(&dir, path);
    if (op_result == FR_OK) {
        for (;;) {
            op_result = f_readdir(&dir, &fno);
            if (op_result != FR_OK || fno.fname[0] == 0) {
                break;
            }
            if (fno.fname[0] == '.')
                continue;

#if _USE_LFN
            fname = *fno.lfname ? fno.lfname : fno.fname;
#else
            fname = fno.fname;
#endif

            if (fname[0] == '.') /* ignore hidden items */
                continue;
            if (fno.fattrib & AM_HID) /* ignore hidden items */
                continue;

            if (fno.fattrib & AM_DIR) { /* Is directory */
                current_path_len = strlen(path);
                path[current_path_len] = '/';
                strcpy(&path[current_path_len+1], fname);

                msg[0] = 'A';
                msg[1] = 'x';
                msg[2] = 'o';
                msg[3] = 'f';
                uint32_to_le_bytes(fno.fsize, &msg[4]);
                uint32_to_le_bytes(fno.fdate + (fno.ftime<<16), &msg[8]);

                strcpy(&msg[12], &path[1]);
                int l = strlen(&msg[12]);
                msg[12+l] = '/';
                msg[13+l] = 0;
                chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) msg, l+14);

                op_result = scan_files(path);
                if (op_result != FR_OK) {
                    break;
                }
                path[current_path_len] = 0;
            }
            else { /* Is file */
                msg[0] = 'A';
                msg[1] = 'x';
                msg[2] = 'o';
                msg[3] = 'f';
                uint32_to_le_bytes(fno.fsize, &msg[4]);
                uint32_to_le_bytes(fno.fdate + (fno.ftime<<16), &msg[8]);

                int current_subdir_path_len = strlen(&path[1]);
                strcpy(&msg[12], &path[1]);

                int append_offset = 12 + current_subdir_path_len;
                if (current_subdir_path_len > 0 && msg[append_offset - 1] == '/') {
                    /* Path already ends with a slash, append filename directly */
                    strcpy(&msg[append_offset], fname);
                }
                else {
                    msg[append_offset] = '/';
                    strcpy(&msg[append_offset + 1], fname);
                    append_offset++; /* Adjust offset for the added slash */
                }

                int l = strlen(&msg[12]); /* Calculate total length of the constructed path (starting from msg[12]) */
                chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) msg, 12 + l + 1);
            }
        }
        f_closedir(&dir);
    }
    else {
        report_fatfs_error(op_result, path);
    }

    return op_result;
}
#pragma GCC diagnostic pop /* diagnostic ignored "-Wrestrict" */


void ReadDirectoryListing(void) {
    FATFS* fsp;
    uint32_t clusters;
    FRESULT op_result;

    op_result = f_getfree("/", &clusters, &fsp);
    if (op_result != FR_OK) {
        goto RDL_result_and_exit;
    }

    ((char*) fbuff)[0] = 'A';
    ((char*) fbuff)[1] = 'x';
    ((char*) fbuff)[2] = 'o';
    ((char*) fbuff)[3] = 'l';
    fbuff[1] = clusters;
    fbuff[2] = fsp->csize;
    fbuff[3] = MMCSD_BLOCK_SIZE;
    chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) (&fbuff[0]), 16);
    chThdSleepMilliseconds(10); /* Give some time for the USB buffer to clear */


    ((char*) fbuff)[0] = '/';
    ((char*) fbuff)[1] = '\0';

    op_result = scan_files((char*) &fbuff[0]); /* scan_files() sends "Axof" data for each file/folder */
    if (op_result != FR_OK) {
        goto RDL_result_and_exit;
    }

    /* Send the final "Axof" for the root directory to indicate parent context */
    ((char*) fbuff)[0] = 'A';
    ((char*) fbuff)[1] = 'x';
    ((char*) fbuff)[2] = 'o';
    ((char*) fbuff)[3] = 'f';
    fbuff[1] = 0;
    fbuff[2] = 0;
    ((char*) fbuff)[12] = '/';
    ((char*) fbuff)[13] = '\0';
    chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) (&fbuff[0]), 14);
    chThdSleepMilliseconds(10); /* Give some time for the USB buffer to clear */

    RDL_result_and_exit:
    /* Send the Result packet */
    send_AxoResult('l', op_result);
}


/* Input data decoder state machine
 *
 * "AxoP" (int32 value, uint16 param_index) -> Parameter set
 * "AxoR" (uint32 length, uint8[] data) -> Preset data set
 * "AxoW" (uint32 startAddress, totalLength) -> Start or close generic memory write
 * "Axow" (uint32 chunkLength) -> Append chunk during generic memory write
 * "Axor" (uint32 offset, length) -> Generic memory read of 'length' bytes
 * "Axoy" (uint32 offset) -> Generic memory read, single 32bit aligned
 * "AxoY" -> Returns true if Core SPILink jumper is set, i.e. Core is set up to be synced
 * "AxoS" -> Start patch
 * "Axos" -> Stop patch
 * "AxoT" (uint8 number) -> Apply preset
 * "AxoM" (uint8 status, data1, data2) -> 3 byte midi message
 * "Axou" -> Go to DFU mode
 * "AxoV" -> Reply FW version number (sends 12 bytes (3 ints): fwversion, crc, patchmainloc)
 * "AxoF" -> Copy patch code to flash (stops patch automatically)
 * "Axol" -> Read directory listing (triggers "Axol" response with card info, a series of "Axof" file info packets, and a final "AxoRl<status>")
 * "AxoC" (uint32 length, char[] filename) -> Create, append to, close, delete file (directory) on SD card, change directory
 */

static void ManipulateFile(void) {
    sdcard_attemptMountIfUnmounted();

    if (FileName[0] != 0) { /* backwards compatibility, don't change! */

        FRESULT op_result = f_open(&pFile, &FileName[0], FA_WRITE | FA_CREATE_ALWAYS);
        if (op_result != FR_OK) {
            report_fatfs_error(op_result, &FileName[0]);
            return;
        }

        op_result = f_lseek(&pFile, pFileSize);
        if (op_result != FR_OK) {
            report_fatfs_error(op_result, &FileName[0]);
            return;
        }

        op_result = f_lseek(&pFile, 0);
        if (op_result != FR_OK) {
            report_fatfs_error(op_result, &FileName[0]);
            return;
        }
    }
    else { /* filename[0] == 0 */

        /* At the time ManipulateFile() is called,
         * the sub-command is now in FileName[1]
         * fdate is FileName[2] + (FileName[3]<<8)
         * ftime is FileName[4] + (FileName[5]<<8)
         * filename path starts at FileName[6]
         * pFileSize is used for file size in 'f' command
         */

        if (FileName[1] == 'k') { /* create directory (AxoCk) */
            FRESULT op_result = f_mkdir(&FileName[6]); /* Path from FileName[6]+ */
            if (op_result == FR_OK) { /* Dir was newly created, so update timestamp */
                FILINFO fno;
                fno.fdate = FileName[2] + (FileName[3]<<8); /* Date from FileName[2/3] */
                fno.ftime = FileName[4] + (FileName[5]<<8); /* Time from FileName[4/5] */

                op_result = f_utime(&FileName[6], &fno); /* Path from FileName[6]+ */
            }

            send_AxoResult(FileName[1], op_result); /* FileName[1] contains sub-command char */
            return;
        }

        else if (FileName[1] == 'f') { /* create file (AxoCf) */
            FRESULT op_result = f_open(&pFile, &FileName[6], FA_WRITE | FA_CREATE_ALWAYS); /* Path from FileName[6]+ */
            if (op_result != FR_OK) {
                goto Cf_result_and_exit;
            }

            op_result = f_lseek(&pFile, pFileSize); /* pFileSize holds the size from received AxoCf command */
            if (op_result != FR_OK) {
                goto Cf_result_and_exit;
            }

            op_result = f_lseek(&pFile, 0);

            Cf_result_and_exit:
            send_AxoResult(FileName[1], op_result); /* FileName[1] contains sub-command char */
            return;
        }

        else if (FileName[1] == 'c') { /* close currently open file (AxoCc) */
            FRESULT op_result = f_close(&pFile);
            if (op_result != FR_OK) {
                goto Cc_result_and_exit;
            }

            FILINFO fno;
            fno.fdate = FileName[2] + (FileName[3]<<8); /* Date from FileName[2/3] */
            fno.ftime = FileName[4] + (FileName[5]<<8); /* Time from FileName[4/5] */
            op_result = f_utime(&FileName[6], &fno); /* Path from FileName[6]+ */

            Cc_result_and_exit:
            send_AxoResult(FileName[1], op_result); /* FileName[1] contains sub-command char */
            return;
        }

        else if (FileName[1] == 'D') { /* delete file (AxoCD) */

            // f_chdir("/"); /* Change to root to avoid FR_DENIED for currently open directory */
            FRESULT op_result = f_unlink(&FileName[6]); /* Path from FileName[6]+ */

            send_AxoResult(FileName[1], op_result); /* FileName[1] contains sub-command char */
            return;
        }

        else if (FileName[1] == 'h') { /* change working directory (AxoCh) */
            FRESULT op_result = f_chdir(&FileName[6]); /* Path from FileName[6]+ */
            if (op_result != FR_OK) {
            }

            send_AxoResult(FileName[1], op_result); /* FileName[1] contains sub-command char */
            return;
        }

        else if (FileName[1] == 'I') { /* get file info (AxoCI) */
            FILINFO fno;
            fno.lfname = &((char*) fbuff)[0]; // fbuff is a global buffer
            fno.lfsize = 256; // Max size for long file name

            FRESULT op_result = f_stat(&FileName[6], &fno); /* Path from FileName[6]+ */
            if (op_result == FR_OK) {
                char* msg = &((char*) fbuff)[0];
                msg[0] = 'A'; msg[1] = 'x'; msg[2] = 'o'; msg[3] = 'f';
                uint32_to_le_bytes(fno.fsize, &msg[4]);
                uint32_to_le_bytes(fno.fdate + (fno.ftime<<16), &msg[8]);
                strcpy(&msg[12], &FileName[6]); // Copy from FileName[6]
                int l = strlen(&msg[12]);
                chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) msg, l+13);
                chThdSleepMilliseconds(10); /* Give some time for the USB buffer to clear */
            }

            send_AxoResult(FileName[1], op_result); /* Explicit AxoR for success or failure */
            return;
        }
    }
}


static FRESULT AppendFile(uint32_t length) {
    UINT bytes_written;
    
    FRESULT op_result = f_write(&pFile, (char*) PATCHMAINLOC, length, &bytes_written);
    if (op_result == FR_OK && bytes_written != length) {
        op_result = FR_DISK_ERR;
    }
}


static uint8_t CopyPatchToFlash(void) {
    flash_unlock();
    flash_Erase_sector(11);

    int src_addr = PATCHMAINLOC;
    int flash_addr = PATCHFLASHLOC;

    int c;
    for (c = 0; c < PATCHFLASHSIZE; c += 4) {
        flash_ProgramWord(flash_addr, *(int32_t*) src_addr);
        src_addr += 4;
        flash_addr += 4;
    }

    /* Verify */
    src_addr = PATCHMAINLOC;
    flash_addr = PATCHFLASHLOC;

    int err = 0;
    for (c = 0; c < PATCHFLASHSIZE; c += 4) {
        if (*(int32_t*) flash_addr != *(int32_t*) src_addr) {
            err++;
        }
        src_addr += 4;
        flash_addr += 4;
    }
    flash_lock();

    if (err) {
        return FR_DISK_ERR; /* Flash verify failed */
    }
    return FR_OK;
}


void ReplyFWVersion(void) {
    char reply[16];
    reply[0] = 'A';
    reply[1] = 'x';
    reply[2] = 'o';
    reply[3] = 'V';

    reply[4] = FWVERSION1; /* major */
    reply[5] = FWVERSION2; /* minor */
    reply[6] = FWVERSION3;
    reply[7] = FWVERSION4;

    uint32_t fwid = GetFirmwareID();
    /* NOTE: Changed to send these two ints in LITTLE ENDIAN */
    uint32_to_le_bytes(fwid, &reply[8]);
    uint32_to_le_bytes(PATCHMAINLOC, &reply[12]);

    chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) (&reply[0]), 16);
}


void ReplySpilinkSynced(void) {
    char reply[5];
    reply[0] = 'A';
    reply[1] = 'x';
    reply[2] = 'o';
    reply[3] = 'Y';
    /* SPILINK pin high means Core is master (default), else synced */
    reply[4] = !palReadPad(SPILINK_JUMPER_PORT, SPILINK_JUMPER_PIN);
    chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) (&reply[0]), 5);
}


void PExReceiveByte(unsigned char c) {
    static volatile char header = 0;
    static volatile uint32_t state = 0;
    static volatile uint32_t current_filename_idx; /* For parsing filename characters into FileName[6]+ */
    static volatile uint32_t param_index;
    static volatile int32_t value;
    static volatile uint32_t write_position;
    static volatile uint32_t offset;
    static volatile uint32_t length;
    static volatile uint32_t patchid;
    // static volatile uint32_t total_write_length;

    AddPCDebug(c, state);

    if (header == 0) {
        switch (state) {
            /* Confirm "Axo" sequence first */
            case 0:
                if (c == 'A') state = 1; 
                break;
            case 1: 
                if (c == 'x') state = 2;
                else state = 0;
                break;
            case 2:
                if (c == 'o') state = 3;
                else state = 0;
                break;
            case 3:
                /* "Axo" sequence confirmed. Continue decoding... */
                header = c;
                switch (c) {

                    case 'M': /* midi command */
                    case 'P': /* param change */
                    case 'R': /* preset change */
                    case 'T': /* apply preset */
                    case 'W': /* generic write start, close */
                    case 'a': /* append data to opened sdcard file (top-level Axoa) */
                    case 'r': /* generic read */
                    case 's': /* start patch (includes midi cost and dsp limit) */
                    case 'w': /* append to memory during 'W' generic write (top-level Axow) */
                    case 'y': /* generic read, 32 bit */
                        state = 4; /* All the above pass on directly to state 4. */
                        break;
                    case 'u': { /* go to DFU mode */
                        uint8_t res = StopPatch();
                        if (res == FR_OK) {
                            exception_initiate_dfu();
                        }
                        state = 0; header = 0;
                        break;
                    }
                    case 'F': { /* copy to flash */
                        uint8_t res = CopyPatchToFlash();
                        send_AxoResult('F', res);
                        state = 0; header = 0;
                        break;
                    }
                    case 'l': /* read directory listing */
                        ReadDirectoryListing(); /* Will send AxoRl when done */
                        state = 0; header = 0;
                        break;
                    case 'V': /* FW version number */
                        ReplyFWVersion();
                        state = 0; header = 0;
                        break;
                    case 'Y': /* is this Core SPILINK synced */
                        ReplySpilinkSynced();
                        state = 0; header = 0;
                        break;
                    case 'A': /* ping */
                        AckPending = 1; /* Only ping explicitly triggers AxoA */
                        state = 0; header = 0;
                        break;
                    case 'C': /* Unified File System Command (create, delete, mkdir, getinfo, close) */
                        state = 4; /* Next state will receive the 4-byte pFileSize/placeholder */
                        current_filename_idx = 0; /* Reset for filename parsing */
                        break;
                    case 'S': { /* stop patch */
                        uint8_t res = StopPatch();
                        send_AxoResult('S', res);
                        state = 0; header = 0;
                        break;
                    }
                    case 'm': { /* start internal Mounter */
                        uint8_t res = StopPatch();
                        if (res == FR_OK) {
                            StartMounter();
                        }
                        state = 0; header = 0;
                        break;
                    }
                    default:
                        state = 0; header = 0; /* Unknown Axo* header */
                } /* End switch (c) */
        } /* End switch (state) */
    }
    else if (header == 'P') { /* param change */
        switch (state) {
            case 4: patchid  = c; state++; break;
            case 5: patchid |= (uint32_t)c <<  8; state++; break;
            case 6: patchid |= (uint32_t)c << 16; state++; break;
            case 7: patchid |= (uint32_t)c << 24; state++; break;
            case 8:   value  = c; state++; break;
            case 9:   value |= (int32_t)c <<  8; state++; break;
            case 10:  value |= (int32_t)c << 16; state++; break;
            case 11:  value |= (int32_t)c << 24; state++; break;
            case 12:  param_index  = c; state++; break;
            case 13:  param_index |= (uint32_t)c << 8;
                if ((patchid == patchMeta.patchID) && (param_index < patchMeta.numPEx)) {
                    PExParameterChange(&(patchMeta.pPExch)[param_index], value, 0xFFFFFFEE);
                }
                state = 0; header = 0;
                break;
            default:
                state = 0; header = 0;
        } /* End switch (state) */
    }
    else if (header == 's') { /* start patch (includes midi cost and dsp limit) */
        static uint16_t uUIMidiCost = 0; /* Local static */
        static uint8_t  uDspLimit200 = 0; /* Local static */
        switch (state) {
            case 4:  uUIMidiCost  = c; state++; break;
            case 5:  uUIMidiCost |= (uint16_t)c << 8; state++; break;
            case 6: { 
                uDspLimit200  = c;
                SetPatchSafety(uUIMidiCost, uDspLimit200);
                loadPatchIndex = LIVE;
                uint8_t res = StartPatch();
                send_AxoResult('s', res);
                state = 0; header = 0;
                break;
            }
            default:
                state = 0; header = 0;
        } /* End switch (state) */
    }
    else if (header == 'W') { /* 'AxoW' memory write commands */
        switch (state) {
            case 4: offset = c; state++; break; /* Address byte 0 */
            case 5: offset |= (uint32_t)c <<  8; state++; break;
            case 6: offset |= (uint32_t)c << 16; state++; break;
            case 7: offset |= (uint32_t)c << 24; state++; break;
            case 8: value = c; state++; break; /* Length byte 0 */
            case 9: value |= (int32_t)c <<  8; state++; break;
            case 10: value |= (int32_t)c << 16; state++; break;
            case 11: value |= (int32_t)c << 24; state++; break;
            case 12: { /* Sub-command can be 'W' (start) or 'e' (end) */
                switch (c) {
                    case 'W': { /* start Memory Write */
                        uint8_t res = StopPatch();
                        if (res == FR_OK) {
                            write_position = offset; /* Initialize write_position here */
                            // total_write_length = (uint32_t)value;
                        }
                        send_AxoResult('W', res);
                        state = 0; header = 0;
                        break;
                    }
                    case 'e': /* end Memory Write */
                        // TODO: error checking based on total_write_length?
                        send_AxoResult('e', FR_OK);
                        state = 0; header = 0;
                        break;
                    default:
                        send_AxoResult('W', FR_INVALID_PARAMETER);
                        state = 0; header = 0;
                        break;
                } /* End switch (c) */
                break;
            }
            default:
                send_AxoResult('W', FR_DISK_ERR);
                state = 0; header = 0;
        } /* End switch (state) */
    }
    /* 'Axow' NOW USED FOR STREAMING CHUNKS BETWEEN 'AxoWW' start memory write AND 'AxoWe' close memory write */
    else if (header == 'w') { /* Handle 'Axow' streaming command */
        switch (state) {
            case 4: value = c; state++; break; /* Chunk length byte 0 */
            case 5: value |= (int32_t)c << 8; state++; break;
            case 6: value |= (int32_t)c << 16; state++; break;
            case 7: value |= (int32_t)c << 24; /* Chunk length */
                length = (uint32_t)value; // Store the length of the new chunk
                // NOTE: The 'write_position' pointer is NOT reset here.
                state = 8;
                break;
            case 8: /* Data streaming state */
                if (value > 0) { // Now consistently using 'value' as the counter
                    value--;
                    *((unsigned char*) write_position) = c;
                    write_position++;

                    /* Update: dummy loop no longer necessary - probably due to refinements of the Java-side SCmds. */

                    if (value == 0) {
                        send_AxoResult('w', FR_OK);
                        state = 0; header = 0;
                    }
                } else {
                    send_AxoResult('w', FR_INVALID_PARAMETER);
                    state = 0; header = 0;
                }
                break;
            default:
                send_AxoResult('w', FR_DISK_ERR);
                state = 0; header = 0;
        } /* End switch (state) */
    }
    else if (header == 'T') { /* apply preset */
        ApplyPreset(c); /* 'c' is the preset index */
        state = 0; header = 0;
    }
    else if (header == 'M') { /* midi message */
        static uint8_t midi_r[3]; /* Local static */
        switch (state) {
            case 4: midi_r[0] = c; state++; break;
            case 5: midi_r[1] = c; state++; break;
            case 6: midi_r[2] = c;
                MidiInMsgHandler(MIDI_DEVICE_INTERNAL, 1, midi_r[0], midi_r[1], midi_r[2]);
                state = 0; header = 0;
                break;
            default:
                state = 0; header = 0;
        } /* End switch (state) */
    }
    else if (header == 'C') { /* create/edit/close/delete file, create/change directory on SD */ 
        switch (state) {
            case 4: /* Expecting pFileSize (byte 0) for 'f', or placeholder for others */
                pFileSize = c; /* Store first byte of pFileSize/placeholder */
                state++;
                break;
            case 5: /* pFileSize (byte 1) */
                pFileSize |= (int32_t)c << 8;
                state++;
                break;
            case 6: /* pFileSize (byte 2) */
                pFileSize |= (int32_t)c << 16;
                state++;
                break;
            case 7: /* pFileSize (byte 3) */
                pFileSize |= (int32_t)c << 24;
                /* Now pFileSize is fully parsed */
                state++;
                break;
            case 8: /* Expecting FileName[0] */
                FileName[0] = c; /* Should always be 0 */
                state++;
                break;
            case 9: /* Expecting FileName[1] (sub-command: 'f', 'k', 'c', 'h', 'D', 'I') */
                FileName[1] = c; /* Store the sub-command */

                if (FileName[1] == 'c' || FileName[1] == 'f' || FileName[1] == 'k') {
                    /* These expect fdate/ftime next (FileName[2]...[5]) */
                    state = 10; /* Go to state to receive FileName[2] (fdate byte 0) */
                } else if (FileName[1] == 'h' || FileName[1] == 'D' || FileName[1] == 'I') {
                    /* These skip fdate/ftime and go straight to filename (FileName[6]+) */
                    current_filename_idx = 6; /* Start filename parsing from FileName[6] */
                    state = 14; /* Go to state to receive FileName[6] (filename byte 0) */
                } else {
                    /* Unknown sub-command for AxoC */
                    state = 0; header = 0;
                }
                break;
            /* States for parsing fdate/ftime (2 bytes each) into FileName[2] to FileName[5] --- */
            /* Used by 'f', 'k', 'c' */
            case 10: /* Expecting FileName[2] (fdate byte 0) */
                FileName[2] = c; state++; break;
            case 11: /* Expecting FileName[3] (fdate byte 1) */
                FileName[3] = c; state++; break;
            case 12: /* Expecting FileName[4] (ftime byte 0) */
                FileName[4] = c; state++; break;
            case 13: /* Expecting FileName[5] (ftime byte 1) */
                FileName[5] = c;
                current_filename_idx = 6; /* Next byte is FileName[6] (start of filename) */
                state++; /* Move on to filename parsing state (case 14) */
                break;

            /* States for parsing filename (variable length, null-terminated) into FileName[6] onwards --- */
            /* Used by 'f', 'k', 'c', 'h', 'D', 'I' */
            case 14: { /* Start/continue filename parsing (into FileName[6]+) */
                if (current_filename_idx < sizeof(FileName)) {
                    FileName[current_filename_idx++] = c;
                    if (c == 0) { // Null terminator received
                        /* Filename complete, dispatch command */
                        uint8_t res = StopPatch();
                        if (res == FR_OK) {
                            ManipulateFile(); /* ManipulateFile will now use FileName and pFileSize */
                        }
                        state = 0; header = 0;
                    }
                }
                else {
                    state = 0; header = 0; /* Error: filename too long */
                }
                break;
            }

            default: /* Unknown state: reset state machine */
                state = 0; header = 0;
        } /* End switch (state) */
    }
    else if (header == 'a') { /* append data to open file on SD */
        switch (state) {
            case 4: value  = c; state++; break;
            case 5: value |= (int32_t)c <<  8; state++; break;
            case 6: value |= (int32_t)c << 16; state++; break;
            case 7: value |= (int32_t)c << 24; /* Chunk length */
                length = (uint32_t)value; /* Store the length to be received */
                write_position = PATCHMAINLOC; /* Set base address for data buffer */
                state = 8; /* Move on to data streaming state */
                break;
            case 8: /* Data streaming state */
                if (value > 0) { /* 'value' now tracks remaining bytes to receive */
                    value--;
                    *((unsigned char*) write_position) = c;
                    write_position++;
                    
                    /* Update: dummy loop no longer necessary - probably due to refinements of the Java-side SCmds. */

                    if (value == 0) {
                        FRESULT res = AppendFile(length); /* Call AppendFile with the total length */
                        send_AxoResult('a', res);
                        state = 0; header = 0;
                    }
                }
                else { /* Should not happen, or error */
                    send_AxoResult('a', FR_INVALID_PARAMETER);
                    state = 0; header = 0;
                }
                break;
            default: /* Error or unexpected state */
                send_AxoResult('a', FR_DISK_ERR);
                state = 0; header = 0;
                break;
        } /* End switch (state) */
    }
    else if (header == 'R') { /* preset change */
        switch (state) {
            case 4: value  = c; state++; break;
            case 5: value |= (uint32_t)c <<  8; state++; break;
            case 6: value |= (uint32_t)c << 16; state++; break;
            case 7: value |= (uint32_t)c << 24; state++;
                offset = (uint32_t)patchMeta.pPresets;
                break;
            default: /* Data streaming state */
                if (value > 0) {
                    value--;
                    if (offset) { /* Check if offset is valid */
                        *((unsigned char*) offset) = c;
                        offset++;
                    }
                    if (value == 0) {
                        state = 0; header = 0;
                    }
                }
                else { /* Should not happen, or error */
                    state = 0; header = 0;
                }
        } /* End switch (state) */
    }
    else if (header == 'r') { /* generic read */
        switch (state) {
            case 4: offset  = c; state++; break;
            case 5: offset |= (uint32_t)c <<  8; state++; break;
            case 6: offset |= (uint32_t)c << 16; state++; break;
            case 7: offset |= (uint32_t)c << 24; state++; break;
            case 8:  value  = c; state++; break;
            case 9:  value |= (int32_t)c <<  8; state++; break;
            case 10: value |= (int32_t)c << 16; state++; break;
            case 11: value |= (int32_t)c << 24;
                char read_reply_header[12];
                read_reply_header[0] = 'A';
                read_reply_header[1] = 'x';
                read_reply_header[2] = 'o';
                read_reply_header[3] = 'r';
                uint32_to_le_bytes(offset, &read_reply_header[4]);
                uint32_to_le_bytes(value, &read_reply_header[8]);
                chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) (&read_reply_header[0]), 12);
                chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) (offset), (uint32_t)value);
                state = 0; header = 0;
                break;
            default:
                state = 0; header = 0;
        } /* End switch (state) */
    }
    else if (header == 'y') { /* generic read, 32-bit */
        switch (state) {
            case 4: offset  = c; state++; break;
            case 5: offset |= (uint32_t)c <<  8; state++; break;
            case 6: offset |= (uint32_t)c << 16; state++; break;
            case 7: offset |= (uint32_t)c << 24;
                value = *((uint32_t*) offset);
                char read_reply_header[12];
                read_reply_header[0] = 'A';
                read_reply_header[1] = 'x';
                read_reply_header[2] = 'o';
                read_reply_header[3] = 'y';
                uint32_to_le_bytes(offset, &read_reply_header[4]);
                uint32_to_le_bytes(value, &read_reply_header[8]);
                chSequentialStreamWrite((BaseSequentialStream*) &BDU1, (const unsigned char*) (&read_reply_header[0]), 12);
                state = 0; header = 0;
                break;
            default:
                state = 0; header = 0;
        } /* End switch (state) */
    }
    else { /* unknown command */
        state = 0; header = 0;
    }
}


void PExReceive(void) {
    if (!AckPending) {
        unsigned char received;
        while (chnReadTimeout(&BDU1, &received, 1, TIME_IMMEDIATE)) {
            PExReceiveByte(received);
        }
    }
}
