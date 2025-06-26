/*
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
#include "flash.h"
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

void BootLoaderInit(void);


static uint32_t fwid;

static uint8_t AckPending = 0;

static uint8_t connected = 0;

static char FileName[256];

static FIL pFile;
static int pFileSize;

static WORKING_AREA(waThreadUSBDMidi, 256);

// If you want more concurrent messages you can set this larger.
#define LOG_BUFFER_SIZE (256)

MUTEX_DECL(LogMutex);
char             LogBuffer[LOG_BUFFER_SIZE];
uint8_t          LogBufferUsed = 0;


connectionflags_t connectionFlags;

__attribute__((noreturn)) static msg_t ThreadUSBDMidi(void *arg) {
    (void)arg;

#if CH_CFG_USE_REGISTRY == TRUE
    chRegSetThreadName("usbdmidi");
#endif

    uint8_t r[4];

    while (1) {
        chnReadTimeout(&MDU1, &r[0], 4, TIME_INFINITE);
        MidiInMsgHandler(MIDI_DEVICE_USB_DEVICE, ((r[0] & 0xF0) >> 4) + 1, r[1],
                         r[2], r[3]);
    }
}


void InitPConnection(void) {

    extern int32_t _flash_end;
    fwid = CalcCRC32((uint8_t *)(FLASH_BASE_ADDR), (uint32_t)(&_flash_end) & 0x07FFFFF);

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
    if (length > 2048) {
        return; // FIXME
    }

    chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )&patchMeta.pDisplayVector[0], length);
}


void LogTextMessage(const char* format, ...) {
    if ((usbGetDriverStateI(BDU1.config->usbp) == USB_ACTIVE) && (connected)) {
        MemoryStream ms;
        uint8_t      tmp[256-5]; // nead AXOT and null

        msObjectInit(&ms, (uint8_t *)tmp, 256-5, 0); 

        va_list ap;
        va_start(ap, format);
        chvprintf((BaseSequentialStream *)&ms, format, ap);
        va_end(ap);
        chSequentialStreamPut(&ms, 0);

        if(chMtxTryLock(&LogMutex))
        {
          size_t length = strlen((char *)tmp);
          if((length) && (LogBufferUsed + 4 + length + 1) < LOG_BUFFER_SIZE)
          {
            LogBuffer[LogBufferUsed++] = 'A';
            LogBuffer[LogBufferUsed++] = 'x';
            LogBuffer[LogBufferUsed++] = 'o';
            LogBuffer[LogBufferUsed++] = 'T';

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
        // LogTextMessage("%lu: PExTx: leaving !chOQIsEmptyI", hal_lld_get_counter_value());
    }
    else {
        if(chMtxTryLock(&LogMutex))
        {
          if(LogBufferUsed)
          {
            chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )LogBuffer, LogBufferUsed);
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
            chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )&ack[0], 7 * 4);


            // LogTextMessage("%lu: Finished sending AxoA, AckPending:%lu", hal_lld_get_counter_value(), AckPending);
            // clear overload flag
            connectionFlags.dspOverload = false;

// #ifdef DEBUG_SERIAL
//            chprintf((BaseSequentialStream * )&SD2, "ack!\r\n");
// #endif

            if (!patchStatus) {
                TransmitDisplayPckt();
            }

            connected = 1;
            exception_checkandreport();
            AckPending = 0;
        }

        if (!patchStatus) {
            uint16_t i;
            for (i = 0; i < patchMeta.numPEx; i++) {
                if (patchMeta.pPExch[i].signals & 0x01) {
                    int v = (patchMeta.pPExch)[i].value;
                    patchMeta.pPExch[i].signals &= ~0x01;
                    PExMessage msg;
                    msg.header = 0x516F7841; /*"AxoQ" */
                    msg.patchID = patchMeta.patchID;
                    msg.index = i;
                    msg.value = v;
                    chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )&msg, sizeof(msg));
                }
            }
        }
    }
}

/* Do not warn about 'strcpy' accessing between 1 and 2147483646 bytes at offsets 76 and 1 may overlap up to 2147483571 bytes at offset [2147483646, 76] [-Wrestrict] */
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wrestrict"

static FRESULT scan_files(char *path) {
  /* Recursive scan of all items in a directory */

  // LogTextMessage("%lu: Entered scan_files:%s", hal_lld_get_counter_value(), (char *)&fbuff[0]);
  FRESULT res;
  FILINFO fno;
  DIR dir;

  uint32_t current_path_len;
  char *fname;
  char *msg = &((char*)fbuff)[64];

  fno.lfname = &FileName[0];
  fno.lfsize = sizeof(FileName);

  res = f_opendir(&dir, path);
  if (res == FR_OK) {

    for (;;) {
      // LogTextMessage("%lu: scan_files: Entered 'for (;;)', path: %s", hal_lld_get_counter_value(), path);

      res = f_readdir(&dir, &fno);
      if (res != FR_OK || fno.fname[0] == 0) {
        // LogTextMessage("%lu: scan_files: BREAKING LOOP. res: %lu, fno.fname[0]: %02x, current_dir: %s", hal_lld_get_counter_value(), res, fno.fname[0], path);
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

        // LogTextMessage("%lu: scan_files: AM_DIR, path: %s, current_path_len:%lu", hal_lld_get_counter_value(), path, current_path_len);
        current_path_len = strlen(path);
        path[current_path_len] = '/';
        strcpy(&path[current_path_len+1], fname);

        msg[0] = 'A';
        msg[1] = 'x';
        msg[2] = 'o';
        msg[3] = 'f';
        *(int32_t *)(&msg[4]) = fno.fsize;
        *(int32_t *)(&msg[8]) = fno.fdate + (fno.ftime<<16);
        strcpy(&msg[12], &path[1]);
        int l = strlen(&msg[12]);
        msg[12+l] = '/';
        msg[13+l] = 0;
        // LogTextMessage("%lu: scan_files: sending Axof msg", hal_lld_get_counter_value());
        chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )msg, l+14);

        // LogTextMessage("%lu: scan_files: entering scan_files recursion", hal_lld_get_counter_value());
        res = scan_files(path);
        if (res != FR_OK) {
          // LogTextMessage("%lu: scan_files recursion break, res:%lu", hal_lld_get_counter_value(), res);
          break;
        }
        // else {
        //   LogTextMessage("%lu: scan_files recursion done, res:%lu", hal_lld_get_counter_value(), res);
        // }

        path[current_path_len] = 0;

      }
      else { /* Is file */
        msg[0] = 'A';
        msg[1] = 'x';
        msg[2] = 'o';
        msg[3] = 'f';
        *(int32_t *)(&msg[4]) = fno.fsize;
        *(int32_t *)(&msg[8]) = fno.fdate + (fno.ftime<<16);
        
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
        // LogTextMessage("%lu: scan_files: !AM_DIR, path: %s, current_subdir_path_len:%lu, append_offset:%lu", hal_lld_get_counter_value(), path, current_subdir_path_len, append_offset);

        int l = strlen(&msg[12]); /* Calculate total length of the constructed path (starting from msg[12]) */
        chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )msg, 12 + l + 1);
      }
    }
    f_closedir(&dir);
  }
  else {
    // LogTextMessage("ERROR: scan_files f_opendir, err:%lu, path:%s", res, path);
	  report_fatfs_error(res, path);
  }

  // LogTextMessage("%lu: scan_files: Exiting path: %s, final res: %lu", hal_lld_get_counter_value(), path, res);
  return res;
}

#pragma GCC diagnostic pop


void ReadDirectoryListing(void) {
  // LogTextMessage("%lu: Entered RDL", hal_lld_get_counter_value());
  FATFS *fsp;
  uint32_t clusters;
  FRESULT err;

  err = f_getfree("/", &clusters, &fsp);
  if (err != FR_OK) {
    // LogTextMessage("%lu: ERROR: RDL f_getfree, err:%lu", hal_lld_get_counter_value(), err);
    report_fatfs_error(err, 0);
    /* Even on error, we should signal the end of the operation to the host */
    ((char*)fbuff)[0] = 'A';
    ((char*)fbuff)[1] = 'x';
    ((char*)fbuff)[2] = 'o';
    ((char*)fbuff)[3] = 'R';
    ((char*)fbuff)[4] = 'd';
    ((char*)fbuff)[5] = (char)err;
    chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&fbuff[0]), 6);
    return;
  }

  ((char*)fbuff)[0] = 'A';
  ((char*)fbuff)[1] = 'x';
  ((char*)fbuff)[2] = 'o';
  ((char*)fbuff)[3] = 'd';
  
  fbuff[1] = clusters;
  fbuff[2] = fsp->csize;
  fbuff[3] = MMCSD_BLOCK_SIZE;
  chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&fbuff[0]), 16);
  // LogTextMessage("%lu: RDL finished sending Axod", hal_lld_get_counter_value());
  chThdSleepMilliseconds(10); /* Give some time for the USB buffer to clear */

  fbuff[0] = '/';
  fbuff[1] = 0;
  // LogTextMessage("%lu: RDL entering scan_files", hal_lld_get_counter_value());
  err = scan_files((char *)&fbuff[0]);
  if (err != FR_OK) {
    // LogTextMessage("%lu: ERROR: RDL scan_files, err:%lu", hal_lld_get_counter_value(), err);
    report_fatfs_error(err, 0);
    /* Even on error, we should signal the end of the operation to the host */
    ((char*)fbuff)[0] = 'A';
    ((char*)fbuff)[1] = 'x';
    ((char*)fbuff)[2] = 'o';
    ((char*)fbuff)[3] = 'R';
    ((char*)fbuff)[4] = 'd';
    ((char*)fbuff)[5] = (char)err;
    chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&fbuff[0]), 6);
    return;
  }
  // else {
  //   LogTextMessage("%lu: RDL scan_files done, err:%lu", hal_lld_get_counter_value(), err);
  // }

  /* Send the final "Axof" for the root directory to indicate parent context */
  ((char*)fbuff)[0] = 'A';
  ((char*)fbuff)[1] = 'x';
  ((char*)fbuff)[2] = 'o';
  ((char*)fbuff)[3] = 'f';
  fbuff[1] = 0;
  fbuff[2] = 0;
  ((char*)fbuff)[12] = '/';
  ((char*)fbuff)[13] = 0;
  chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&fbuff[0]), 14);
  // LogTextMessage("%lu: RDL finished sending Axof", hal_lld_get_counter_value());

  /* Send the "End of Operation" packet */
  ((char*)fbuff)[0] = 'A';
  ((char*)fbuff)[1] = 'x';
  ((char*)fbuff)[2] = 'o';
  ((char*)fbuff)[3] = 'R';
  ((char*)fbuff)[4] = 'd';
  ((char*)fbuff)[5] = (char)err;
  chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&fbuff[0]), 6);
  // LogTextMessage("%lu: RDL finished sending AxoE, leaving", hal_lld_get_counter_value());
  return;
}


/* input data decoder state machine
 *
 * "AxoP" (int value, int16 index) -> parameter set
 * "AxoR" (int length, data) -> preset data set
 * "AxoW" (int length, int addr, char[length] data) -> generic memory write
 * "Axow" (int length, int offset, char[12] filename, char[length] data) -> data write to SD card
 * "Axor" (int offset, int length) -> generic memory read
 * "Axoy" (int offset) -> generic memory read, single 32bit aligned
 * "AxoY" returns true if Core SPILink jumper is set, i.e. Core is set up to be synced
 * "AxoS" -> start patch
 * "Axos" -> stop patch
 * "AxoT" (char number) -> apply preset
 * "AxoM" (char char char) -> 3 byte midi message
 * "AxoD" go to DFU mode
 * "AxoV" reply FW version number (4 bytes)
 * "AxoF" copy patch code to flash (assumes patch is stopped)
 * "Axod" read directory listing
 * "AxoC (int length) (char[] filename)" create and open file on SD card
 * "Axoc" close file on SD card
 * "AxoA (int length) (byte[] data)" append data on open file on SD card
 * "AxoB (int or) (int and)" buttons for virtual Axoloti Control
 */


static void ManipulateFile(void) {
  // LogTextMessage("%lu: Entered MNPFL", hal_lld_get_counter_value());
  sdcard_attemptMountIfUnmounted();
  FRESULT err = FR_OK; // Initialize err to success (0)
  if (FileName[0]) {
    // LogTextMessage("Executing backwards compatibility block.");
    /* backwards compatibility */
    FRESULT op_err; // Temporary variable for each operation's result
    op_err = f_open(&pFile, &FileName[0], FA_WRITE | FA_CREATE_ALWAYS);
    if (op_err != FR_OK) {
      // LogTextMessage("ERROR: MNPFL f_open (backwards), err:%lu, path:%s", op_err, &FileName[0]);
      report_fatfs_error(op_err, &FileName[0]);
      err = op_err; // Propagate this error to the main 'err'
    }
    if (err == FR_OK) { // Only proceed if no error yet
        op_err = f_lseek(&pFile, pFileSize);
        if (op_err != FR_OK) {
            // LogTextMessage("ERROR: MNPFL f_lseek1 (backwards), err:%lu, path:%s", op_err, &FileName[0]);
            report_fatfs_error(op_err, &FileName[0]);
            err = op_err; // Propagate this error
        }
    }
    if (err == FR_OK) { // Only proceed if no error yet
        op_err = f_lseek(&pFile, 0);
        if (op_err != FR_OK) {
            // LogTextMessage("ERROR: MNPFL f_lseek2 (backwards), err:%lu, path:%s", op_err, &FileName[0]);
            report_fatfs_error(op_err, &FileName[0]);
            err = op_err; // Propagate this error
        }
    }
  }
  else {
    /* filename[0] == 0 */
    if (FileName[1]=='d') {
      // LogTextMessage("Executing 'd' (create directory) command.");
      /* create directory */
      FRESULT op_err;
      op_err = f_mkdir(&FileName[6]);
      if ((op_err != FR_OK) && (op_err != FR_EXIST)) { // FR_EXIST is not an error for mkdir
        // LogTextMessage("ERROR: MNPFL f_mkdir, err:%lu, path:%s", op_err, &FileName[6]);
        report_fatfs_error(op_err, &FileName[6]);
        err = op_err; // Propagate this error
      }
      // If mkdir was FR_OK or FR_EXIST, the overall operation so far is considered successful.
      // Now, try to set timestamp.
      if (err == FR_OK || err == FR_EXIST) { // If no previous error OR directory already existed
          /* and set timestamp */
          FILINFO fno;
          fno.fdate = FileName[2] + (FileName[3]<<8);
          fno.ftime = FileName[4] + (FileName[5]<<8);
          op_err = f_utime(&FileName[6], &fno);
          if (op_err != FR_OK) {
            // LogTextMessage("ERROR: MNPFL f_utime, err:%lu, path:%s", op_err, &FileName[6]);
            report_fatfs_error(op_err, &FileName[6]);
            err = op_err; // Propagate this specific utime error as the overall error
          }
      }
      // If f_mkdir itself failed with a true error, we don't attempt f_utime,
      // and 'err' already holds the mkdir error.
    }
    else if (FileName[1]=='f') {
      // LogTextMessage("Executing 'f' (create file) command.");
      /* create file */
      FRESULT op_err;
      op_err = f_open(&pFile, &FileName[6], FA_WRITE | FA_CREATE_ALWAYS);
      if (op_err != FR_OK) {
        // LogTextMessage("ERROR: MNPFL f_open, err:%lu, path:%s", op_err, &FileName[6]);
        report_fatfs_error(op_err, &FileName[6]);
        err = op_err; // Propagate this error
      }
      if (err == FR_OK) { // Only proceed if no error yet
          op_err = f_lseek(&pFile, pFileSize);
          if (op_err != FR_OK) {
            // LogTextMessage("ERROR: MNPFL f_lseek3, err:%lu, path:%s", op_err, &FileName[6]);
            report_fatfs_error(op_err, &FileName[6]);
            err = op_err; // Propagate this error
          }
      }
      if (err == FR_OK) { // Only proceed if no error yet
          op_err = f_lseek(&pFile, 0);
          if (op_err != FR_OK) {
            // LogTextMessage("ERROR: MNPFL f_lseek4, err:%lu, path:%s", op_err, &FileName[6]);
            report_fatfs_error(op_err, &FileName[6]);
            err = op_err; // Propagate this error
          }
      }
    }
    else if (FileName[1]=='D') {
      // LogTextMessage("Executing 'D' (delete) command.");
      /* delete */

      err = f_unlink(&FileName[6]);
      if (err != FR_OK) {
        // LogTextMessage("ERROR: MNPFL f_unlink, err:%lu, path:%s", err, &FileName[6]);
        report_fatfs_error(err, &FileName[6]);
      }
      // else {
      //   LogTextMessage("SUCCESS: MNPFL f_unlink, path:%s", &FileName[6]);
      // }
      /* New Response Packet: ['A', 'x', 'o', 'R', command_byte, status_byte] */
      char resp_msg[6];
      resp_msg[0] = 'A';
      resp_msg[1] = 'x';
      resp_msg[2] = 'o';
      resp_msg[3] = 'R';        // Header: 'R' for Result (MCU to Java)
      resp_msg[4] = 'D';        // Command byte: 'D' for the Delete command
      resp_msg[5] = (char)err;  // Status byte: 0 (FR_OK) for success, or the FRESULT error code
      chSequentialStreamWrite((BaseSequentialStream *)&BDU1, (const unsigned char*) resp_msg, 6);
      // LogTextMessage("%lu: Sent AxoR (Delete Result), command='%c', status=%lu", hal_lld_get_counter_value(), resp_msg[4], err);
    }
    else if (FileName[1]=='C') {
      // LogTextMessage("Executing 'C' (change directory) command.");
      /* change working directory */
      // Single operation, direct assignment to 'err' is fine
      err = f_chdir(&FileName[6]);
      if (err != FR_OK) {
        // LogTextMessage("ERROR: MNPFL f_chdir, err:%lu, path:%s", err, &FileName[6]);
        report_fatfs_error(err, &FileName[6]);
      }
    }
    else if (FileName[1]=='I') {
      // LogTextMessage("Executing 'I' (get file info) command.");
      /* get file info */
      FILINFO fno;
      fno.lfname = &((char*)fbuff)[0];
      fno.lfsize = 256;
      FRESULT stat_err = f_stat(&FileName[6], &fno); // Use a local variable here as it sends its own msg
      if (stat_err == FR_OK) {
        char *msg = &((char*)fbuff)[0];
        msg[0] = 'A';
        msg[1] = 'x';
        msg[2] = 'o';
        msg[3] = 'f';
        *(int32_t *)(&msg[4]) = fno.fsize;
        *(int32_t *)(&msg[8]) = fno.fdate + (fno.ftime<<16);
        strcpy(&msg[12], &FileName[6]);
        int l = strlen(&msg[12]);
        chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )msg, l+13);
      }
      else {
        // LogTextMessage("ERROR: MNPFL f_stat, err:%lu, path:%s", stat_err, &FileName[6]);
        report_fatfs_error(stat_err, &FileName[6]); // Report this specific error
      }
    }
  }
  AckPending = (err == FR_OK);
  // LogTextMessage("%lu: Leaving MNPFL, AckPending=%lu", hal_lld_get_counter_value(), AckPending);
}


static void CloseFile(void) {
  FRESULT err;
  err = f_close(&pFile);
  if (err != FR_OK) {
    // LogTextMessage("ERROR: CloseFile f_close, err:%lu, path:%s", err, &FileName[0]);
    report_fatfs_error(err, &FileName[0]);
  }
  if (!FileName[0]) {
    /* and set timestamp */
    FILINFO fno;
    fno.fdate = FileName[2] + (FileName[3]<<8);
    fno.ftime = FileName[4] + (FileName[5]<<8);
    err = f_utime(&FileName[6], &fno);
    if (err != FR_OK) {
      // LogTextMessage("ERROR: CloseFile f_utime, err:%lu, path:%s", err, &FileName[6]);
      report_fatfs_error(err, &FileName[6]);
    }
  }
}


static void CopyPatchToFlash(void) {
    flash_unlock();
    flash_Erase_sector(11);

    int src_addr = PATCHMAINLOC;
    int flash_addr = PATCHFLASHLOC;

    int c;
    for (c = 0; c < PATCHFLASHSIZE;) {
        flash_ProgramWord(flash_addr, *(int32_t *)src_addr);
        src_addr += 4;
        flash_addr += 4;
        c += 4;
    }

    /* Verify */
    src_addr = PATCHMAINLOC;
    flash_addr = PATCHFLASHLOC;

    int err = 0;
    for (c = 0; c < PATCHFLASHSIZE;) {
        if (*(int32_t *)flash_addr != *(int32_t *)src_addr) {
            err++;
        }

        src_addr += 4;
        flash_addr += 4;
        c += 4;
    }

    if (err) {
        while (1); /* Flash verify failed */
    }

    AckPending = 1;
}


void ReplyFWVersion(void) {
  uint8_t reply[16];
  reply[0] = 'A';
  reply[1] = 'x';
  reply[2] = 'o';
  reply[3] = 'V';
  reply[4] = FWVERSION1; /* major */
  reply[5] = FWVERSION2; /* minor */
  reply[6] = FWVERSION3;
  reply[7] = FWVERSION4;
  uint32_t fwid = GetFirmwareID();
  reply[8] = (uint8_t)(fwid>>24);
  reply[9] = (uint8_t)(fwid>>16);
  reply[10] = (uint8_t)(fwid>>8);
  reply[11] = (uint8_t)(fwid);
  reply[12] = (uint8_t)(PATCHMAINLOC>>24);
  reply[13] = (uint8_t)(PATCHMAINLOC>>16);
  reply[14] = (uint8_t)(PATCHMAINLOC>>8);
  reply[15] = (uint8_t)(PATCHMAINLOC);
  chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&reply[0]), 16);
}


void ReplySpilinkSynced(void) {
  uint8_t reply[5];
  reply[0] = 'A';
  reply[1] = 'x';
  reply[2] = 'o';
  reply[3] = 'Y';
  /* SPILINK pin high means Core is master (default), else synced */
  reply[4] = !palReadPad(SPILINK_JUMPER_PORT, SPILINK_JUMPER_PIN);
  chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&reply[0]), 5);
}

typedef struct _PCDebug
{
  uint8_t c;
  int state;
} PCDebug;

#define PC_DBG_COUNT (0)
#if PC_DBG_COUNT
PCDebug dbg_received[PC_DBG_COUNT]  __attribute__ ((section (".sram3")));;
uint16_t uCount = 0;

void AddPCDebug(uint8_t c, int state)
{
  dbg_received[uCount].c = c;
  dbg_received[uCount].state = state;
  uCount++;
  if(uCount==PC_DBG_COUNT)
    uCount = 0;
}
#else
  #define AddPCDebug(a,b)
#endif



void PExReceiveByte(unsigned char c) {
  static char header = 0;
  static int state = 0;
  static unsigned int index;
  static int value;
  static int position;
  static int offset;
  static int length;
  static int a;
  static int b;
  static uint32_t patchid;

  AddPCDebug(c, state);

  if (!header) {
    switch (state) {
    case 0:
      if (c == 'A')
        state++;
      break;
    case 1:
      if (c == 'x')
        state++;
      else
        state = 0;
      break;
    case 2:
      if (c == 'o')
        state++;
      else
        state = 0;
      break;
    case 3:
      header = c;
      if (c == 'P') { /* param change */
        state = 4;
      }
      else if (c == 'R') { /* preset change */
        state = 4;
      }
      else if (c == 'W') { /* write */
        state = 4;
      }
      else if (c == 'w') { /* write file to SD */
        state = 4;
      }
      else if (c == 'T') { /* apply preset */
        state = 4;
      }
      else if (c == 'M') { /* midi command */
        state = 4;
      }
      else if (c == 'B') { /* virtual Axoloti Control buttons */
        state = 4;
      }
      else if (c == 'C') { /* create sdcard file */
        state = 4;
      }
      else if (c == 'a') { /* append data to sdcard file */
        /* Note: changed from 'A' to lower-case to avoid confusion with "AxoA" ack message (MCU->Patcher) */
        state = 4;
      }
      else if (c == 'r') { /* generic read */
        state = 4;
      }
      else if (c == 'y') { /* generic read */
        state = 4;
      }
      else if (c == 'U') { /* Set cpU safety*/
        state = 4;
      }
      else if (c == 'S') { /* stop patch */
        // LogTextMessage("%lu: AxoS received", hal_lld_get_counter_value());
        state = 0;
        header = 0;
        StopPatch();
        AckPending = 1;
      }
      else if (c == 'D') { /* go to DFU mode */
        state = 0;
        header = 0;
        StopPatch();
        exception_initiate_dfu();
      }
      else if (c == 'F') { /* copy to flash */
        state = 0;
        header = 0;
        StopPatch();
        CopyPatchToFlash();
      }
      else if (c == 'd') { /* read directory listing */
        // LogTextMessage("%lu: Axod received", hal_lld_get_counter_value());
        AckPending = 1; /* Immediately acknowledge the command receipt. */
        state = 0;
        header = 0;
        // StopPatch(); /* not strictly necessary but patch will glitch */
        /* IMPORTANT: Calling  ReadDirectoryListing() *after* AckPending is set and state is reset.
         * PExTransmit will send the AxoA shortly.
         * ReadDirectoryListing() will then stream data and send a final AxoE "End of Operation" packet. */
        ReadDirectoryListing();
      }
      else if (c == 's') { /* start patch */
        state = 0;
        header = 0;
        loadPatchIndex = LIVE;
        StartPatch();
        AckPending = 1;
      }
      else if (c == 'V') { /* FW version number */
        state = 0;
        header = 0;
        ReplyFWVersion();
        AckPending = 1;
      }
      else if (c == 'Y') { /* is this Core SPILINK synced */
        state = 0;
        header = 0;
        ReplySpilinkSynced();
        AckPending = 1;
      }
      else if (c == 'p') { /* ping */
        // LogTextMessage("%lu: Axop (ping) received", hal_lld_get_counter_value());
        state = 0;
        header = 0;
// #ifdef DEBUG_SERIAL
//        chprintf((BaseSequentialStream * )&SD2, "ping\r\n");
// #endif
        AckPending = 1;
      }
      else if (c == 'c') { /* close sdcard file */
        // LogTextMessage("%lu: Axoc (f_close) received", hal_lld_get_counter_value());
        state = 0;
        header = 0;
        CloseFile();
        AckPending = 1;
      }
      else
        state = 0;
      break;
    }
  }
  else if (header == 'P') { /* param change */
    switch (state) {
    case 4:
      patchid = c;
      state++;
      break;
    case 5:
      patchid += c << 8;
      state++;
      break;
    case 6:
      patchid += c << 16;
      state++;
      break;
    case 7:
      patchid += c << 24;
      state++;
      break;
    case 8:
      value = c;
      state++;
      break;
    case 9:
      value += c << 8;
      state++;
      break;
    case 10:
      value += c << 16;
      state++;
      break;
    case 11:
      value += c << 24;
      state++;
      break;
    case 12:
      index = c;
      state++;
      break;
    case 13:
      index += c << 8;
      state = 0;
      header = 0;
      if ((patchid == patchMeta.patchID) &&
          (index < patchMeta.numPEx)) {
        PExParameterChange(&(patchMeta.pPExch)[index], value, 0xFFFFFFEE);
      }
      break;
    default:
      state = 0;
      header = 0;
    }
  }
  else if (header == 'U') {
    // LogTextMessage("%lu: AxoU received", hal_lld_get_counter_value());
    static uint16_t uUIMidiCost = 0;
    static uint8_t  uDspLimit200 = 0;
    
    switch (state) {
    case 4:
      uUIMidiCost = c;
      state++;
      break;
    case 5:
      uUIMidiCost += c << 8;
      state++;
      break;
    case 6:
      uDspLimit200 = c;

      SetPatchSafety(uUIMidiCost, uDspLimit200);

      // we have our values now so ack
      header = 0;
      state = 0;
      AckPending = 1;
      break;
    default:
      header = 0;
      state = 0;
      AckPending = 1;
      break;
    }
  }
  else if (header == 'W') {
    // LogTextMessage("%lu: AxoW received", hal_lld_get_counter_value());
    switch (state) {
    case 4:
      offset = c;
      state++;
      break;
    case 5:
      offset += c << 8;
      state++;
      break;
    case 6:
      offset += c << 16;
      state++;
      break;
    case 7:
      offset += c << 24;
      state++;
      break;
    case 8:
      value = c;
      state++;
      break;
    case 9:
      value += c << 8;
      state++;
      break;
    case 10:
      value += c << 16;
      state++;
      break;
    case 11:
      value += c << 24;
      state++;
      break;
    default:
      if (value > 0) {
        value--;
        *((unsigned char *)offset) = c;
        offset++;
        if (value == 0) {
          header = 0;
          state = 0;
          AckPending = 1;
        }
      }
      else {
        header = 0;
        state = 0;
        AckPending = 1;
      }
    }
  }
  else if (header == 'w') {
    // LogTextMessage("%lu: Axow received", hal_lld_get_counter_value());
    switch (state) {
    case 4:
      offset = c;
      state++;
      break;
    case 5:
      offset += c << 8;
      state++;
      break;
    case 6:
      offset += c << 16;
      state++;
      break;
    case 7:
      offset += c << 24;
      state++;
      break;
    case 8:
      value = c;
      state++;
      break;
    case 9:
      value += c << 8;
      state++;
      break;
    case 10:
      value += c << 16;
      state++;
      break;
    case 11:
      value += c << 24;
      length = value;
      position = offset;
      state++;
      break;
    case 12:
    case 13:
    case 14:
    case 15:
    case 16:
    case 17:
    case 18:
    case 19:
    case 20:
    case 21:
    case 22:
    case 23:
      FileName[state - 12] = c;
      state++;
      break;
    default:
      if (value > 0) {
        value--;
        *((unsigned char *)position) = c;
        position++;
        if (value == 0) {
          FRESULT err;
          header = 0;
          state = 0;
          sdcard_attemptMountIfUnmounted();
          err = f_open(&pFile, &FileName[0], FA_WRITE | FA_CREATE_ALWAYS);
          if (err != FR_OK) {
            LogTextMessage("File open failed");
          }
          int bytes_written;
          err = f_write(&pFile, (char *)offset, length, (void *)&bytes_written);
          if (err != FR_OK) {
            LogTextMessage("File write failed");
          }
          err = f_close(&pFile);
          if (err != FR_OK) {
            LogTextMessage("File close failed");
          }
          AckPending = 1;
        }
      }
      else {
        header = 0;
        state = 0;
      }
    }
  }
  else if (header == 'T') { /* Apply Preset */
    ApplyPreset(c);
    AckPending = 1;
    header = 0;
    state = 0;
  }
  else if (header == 'M') { /* Midi message */
    static uint8_t midi_r[3];
    switch (state) {
    case 4:
      midi_r[0] = c;
      state++;
      break;
    case 5:
      midi_r[1] = c;
      state++;
      break;
    case 6:
      midi_r[2] = c;
      MidiInMsgHandler(MIDI_DEVICE_INTERNAL, 1, midi_r[0], midi_r[1],
                       midi_r[2]);
      header = 0;
      state = 0;
      break;
    default:
      header = 0;
      state = 0;
    }
  }
  else if (header == 'C') {
    // LogTextMessage("%lu: AxoC received", hal_lld_get_counter_value());
    switch (state) {
    case 4:
      pFileSize = c;
      state++;
      break;
    case 5:
      pFileSize += c << 8;
      state++;
      break;
    case 6:
      pFileSize += c << 16;
      state++;
      break;
    case 7:
      pFileSize += c << 24;
      state++;
      break;
    case 8:
      FileName[state - 8] = c;
      /* Filename starting with null means there are attributes present */
      state++;
      break;
    default:
      if (c || ((!FileName[0])&&(state<14))) {
        FileName[state - 8] = c;
        state++;
      }
      else {
        FileName[state - 8] = 0;
        StopPatch();
        ManipulateFile();
        header = 0;
        state = 0;
        /* AckPending set from within ManipulateFile()! */
      }
    }
  }
  else if (header == 'a') {
    // LogTextMessage("%lu: Axoa received", hal_lld_get_counter_value());
    switch (state) {
    case 4:
      value = c;
      state++;
      break;
    case 5:
      value += c << 8;
      state++;
      break;
    case 6:
      value += c << 16;
      state++;
      break;
    case 7:
      value += c << 24;
      length = value;
      position = PATCHMAINLOC;
      state++;
      break;
    default:
      if (value > 0) {
        value--;
        *((unsigned char *)position) = c;
        position++;
        if (value == 0) {
          FRESULT err;
          header = 0;
          state = 0;
          int bytes_written;
          err = f_write(&pFile, (char *)PATCHMAINLOC, length,
                        (void *)&bytes_written);
          if (err != FR_OK) {
            // LogTextMessage("ERROR: 'header == 'a'->case default' f_write, err:%lu, path:%s", err, pFile);
            report_fatfs_error(err, 0);
          }
          AckPending = 1;
        }
      }
      else {
        header = 0;
        state = 0;
      }
    }
  }
  else if (header == 'B') {
    switch (state) {
    case 4:
      a = c;
      state++;
      break;
    case 5:
      a += c << 8;
      state++;
      break;
    case 6:
      a += c << 16;
      state++;
      break;
    case 7:
      a += c << 24;
      state++;
      break;
    case 8:
      b = c;
      state++;
      break;
    case 9:
      b += c << 8;
      state++;
      break;
    case 10:
      b += c << 16;
      state++;
      break;
    case 11:
      b += c << 24;
      state++;
      break;
    case 12:
      // EncBuffer[0] += c;
      state++;
      break;
    case 13:
      // EncBuffer[1] += c;
      state++;
      break;
    case 14:
      // EncBuffer[2] += c;
      state++;
      break;
    case 15:
      // EncBuffer[3] += c;
      header = 0;
      state = 0;
      // Btn_Nav_Or.word = Btn_Nav_Or.word | a;
      // Btn_Nav_And.word = Btn_Nav_And.word & b;
      break;
    }
  }
  else if (header == 'R') {
    // LogTextMessage("%lu: AxoR received", hal_lld_get_counter_value());
    switch (state) {
    case 4:
      length = c;
      state++;
      break;
    case 5:
      length += c << 8;
      state++;
      break;
    case 6:
      length += c << 16;
      state++;
      break;
    case 7:
      length += c << 24;
      state++;
      offset = (int)patchMeta.pPresets;
      break;
    default:
      if (length > 0) {
        length--;
        if (offset) {
          *((unsigned char *)offset) = c;
          offset++;
        }
        if (length == 0) {
          header = 0;
          state = 0;
          AckPending = 1;
        }
      }
      else {
        header = 0;
        state = 0;
        AckPending = 1;
      }
    }
  }
  else if (header == 'r') { /* generic read */
    // LogTextMessage("%lu: Axor received", hal_lld_get_counter_value());
    switch (state) {
    case 4:
      offset = c;
      state++;
      break;
    case 5:
      offset += c << 8;
      state++;
      break;
    case 6:
      offset += c << 16;
      state++;
      break;
    case 7:
      offset += c << 24;
      state++;
      break;
    case 8:
      value = c;
      state++;
      break;
    case 9:
      value += c << 8;
      state++;
      break;
    case 10:
      value += c << 16;
      state++;
      break;
    case 11:
      value += c << 24;

      uint32_t read_repy_header[3];
      ((char*)read_repy_header)[0] = 'A';
      ((char*)read_repy_header)[1] = 'x';
      ((char*)read_repy_header)[2] = 'o';
      ((char*)read_repy_header)[3] = 'r';
      read_repy_header[1] = offset;
      read_repy_header[2] = value;
      chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&read_repy_header[0]), 3 * 4);
      chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(offset), value);

      AckPending = 1;
      header = 0;
      state = 0;
      break;
    }
  }
  else if (header == 'y') { /* generic read, 32bit */
    // LogTextMessage("%lu: Axoy received", hal_lld_get_counter_value());
    switch (state) {
    case 4:
      offset = c;
      state++;
      break;
    case 5:
      offset += c << 8;
      state++;
      break;
    case 6:
      offset += c << 16;
      state++;
      break;
    case 7:
      offset += c << 24;

      uint32_t read_repy_header[3];
      ((char*)read_repy_header)[0] = 'A';
      ((char*)read_repy_header)[1] = 'x';
      ((char*)read_repy_header)[2] = 'o';
      ((char*)read_repy_header)[3] = 'y';
      read_repy_header[1] = offset;
      read_repy_header[2] = *((uint32_t*)offset);
      chSequentialStreamWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&read_repy_header[0]), 3 * 4);

      AckPending = 1;
      header = 0;
      state = 0;
      break;
    }
  }  else {
    header = 0;
    state = 0;
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


/*
void USBDMidiPoll(void) {
  uint8_t r[4];
  while (chnReadTimeout(&MDU1, &r, 4, TIME_IMMEDIATE)) {
    MidiInMsgHandler(MIDI_DEVICE_USB_DEVICE, (( r[0] & 0xF0) >> 4)+ 1, r[1], r[2], r[3]);
  }
}
*/
