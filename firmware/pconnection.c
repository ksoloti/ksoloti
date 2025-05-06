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
#include "analyser.h"

#if INBUILT_MOUNTER_FLASHER
extern void StartFlasher(void);
extern void StartMounter(void);
#endif

//#define DEBUG_SERIAL 1

void BootLoaderInit(void);


static uint32_t fwid;

static uint8_t AckPending = 0;

static uint8_t connected = 0;

static char FileName[FF_LFN_BUF];

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

#if CH_CFG_USE_REGISTRY
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


uint32_t GetFirmwareID(void) {
    return fwid;
}


void TransmitDisplayPckt(void) {
    if (patchMeta.pDisplayVector == 0) {
        return;
    }

    unsigned int length = 12 + (patchMeta.pDisplayVector[2] * 4);
    // if (length > 2048) {
    //     return; // FIXME
    // }

    chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )&patchMeta.pDisplayVector[0], length);
}


void LogTextMessage(const char* format, ...) {
    if ((connected) && (usbGetDriverStateI(BDU1.config->usbp) == USB_ACTIVE)) {
        if(chMtxTryLock(&LogMutex) && !port_is_isr_context() )
        {
          MemoryStream ms;
          uint8_t      tmp[256-5]; // nead AXOT and null

          msObjectInit(&ms, (uint8_t *)tmp, 256-5, 0); 

          va_list ap;
          va_start(ap, format);
          chvprintf((BaseSequentialStream *)&ms, format, ap);
          va_end(ap);
          streamPut(&ms, 0);
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
    if (!oqGetEmptyI(&BDU1.oqueue)) {
        chThdSleepMilliseconds(1);
        BDU1.oqueue.q_notify(&BDU1.oqueue);
    }
    else {
        AnalyserSetChannel(acUsbAudioSof, true);
        if(chMtxTryLock(&LogMutex))
        {
          if(LogBufferUsed)
          {
            chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )LogBuffer, LogBufferUsed);
            LogBufferUsed = 0;
          }
          chMtxUnlock(&LogMutex);
        }
        AnalyserSetChannel(acUsbAudioSof, false);
        AnalyserSetChannel(acUsbAudioSof, true);

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
            chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )&ack[0], 7 * 4);


            // clear overload flag
            connectionFlags.dspOverload = false;

// #ifdef DEBUG_SERIAL
//            chprintf((BaseSequentialStream * )&SD2,"ack!\r\n");
// #endif

            if (!patchStatus) {
                TransmitDisplayPckt();
            }

            connected = 1;
            exception_checkandreport();
            AckPending = 0;
        }
        AnalyserSetChannel(acUsbAudioSof, false);
        AnalyserSetChannel(acUsbAudioSof, true);
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
                    chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )&msg, sizeof(msg));
                }
            }
        }
        AnalyserSetChannel(acUsbAudioSof, false);
    }
}


static FRESULT scan_files(char *path) {
  static FILINFO fno; // just one as we recurse and run out of stack

  FRESULT res;
  DIR dir;
  int i;
  char *fn;
  char *msg = &((char*)fbuff)[64];
  // fno.lfname = &FileName[0];
  // fno.lfsize = sizeof(FileName);
  res = f_opendir(&dir, path);
  if (res == FR_OK) {
    i = strlen(path);
    for (;;) {
      res = f_readdir(&dir, &fno);
      if (res != FR_OK || fno.fname[0] == 0)
        break;
      if (fno.fname[0] == '.')
        continue;
      fn = fno.fname;
      if (fn[0] == '.')
        continue;
      if (fno.fattrib & AM_HID)
        continue;
      if (fno.fattrib & AM_DIR) {
        path[i] = '/';
        strcpy(&path[i+1], fn);
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
        chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )msg, l+14);
        res = scan_files(path);
        path[i] = 0;
        if (res != FR_OK) break;
      } else {
        msg[0] = 'A';
        msg[1] = 'x';
        msg[2] = 'o';
        msg[3] = 'f';
        *(int32_t *)(&msg[4]) = fno.fsize;
        *(int32_t *)(&msg[8]) = fno.fdate + (fno.ftime<<16);
        strcpy(&msg[12], &path[1]);
        msg[12+i-1] = '/';
        strcpy(&msg[12+i], fn);
        int l = strlen(&msg[12]);
        chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )msg, l+13);
      }
    }
  } else {
	  report_fatfs_error(res,0);
  }
  return res;
}


void ReadDirectoryListing(void) {
  FATFS *fsp;
  uint32_t clusters;
  FRESULT err;

  err = f_getfree("/", &clusters, &fsp);
  if (err != FR_OK) {
	report_fatfs_error(err,0);
    return;
  }
  /*
   chprintf(chp,
   "FS: %lu free clusters, %lu sectors per cluster, %lu bytes free\r\n",
   clusters, (uint32_t)SDC_FS.csize,
   clusters * (uint32_t)SDC_FS.csize * (uint32_t)MMCSD_BLOCK_SIZE);
   */
  ((char*)fbuff)[0] = 'A';
  ((char*)fbuff)[1] = 'x';
  ((char*)fbuff)[2] = 'o';
  ((char*)fbuff)[3] = 'd';
  fbuff[1] = clusters;
  fbuff[2] = fsp->csize;
  fbuff[3] = MMCSD_BLOCK_SIZE;
  chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&fbuff[0]), 16);
  chThdSleepMilliseconds(10);
  fbuff[0] = '/';
  fbuff[1] = 0;
  scan_files((char *)&fbuff[0]);

  char *msg = &((char*)fbuff)[64];
  msg[0] = 'A';
  msg[1] = 'x';
  msg[2] = 'o';
  msg[3] = 'f';
  *(int32_t *)(&msg[4]) = 0;
  *(int32_t *)(&msg[8]) = 0;
  msg[12] = '/';
  msg[13] = 0;
  chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )msg, 14);
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
 * "Axom" -> start mounter
 * "Axol" -> start flasher
 */


static void ManipulateFile(void) {
  sdcard_attemptMountIfUnmounted();
  if (FileName[0]) {
    /* backwards compatibility */
    FRESULT err;
    err = f_open(&pFile, &FileName[0], FA_WRITE | FA_CREATE_ALWAYS);
    if (err != FR_OK) {
      report_fatfs_error(err,&FileName[0]);
    }
    err = f_lseek(&pFile, pFileSize);
    if (err != FR_OK) {
      report_fatfs_error(err,&FileName[0]);
    }
    err = f_lseek(&pFile, 0);
    if (err != FR_OK) {
      report_fatfs_error(err,&FileName[0]);
    }
  } else {
    /* filename[0] == 0 */
    if (FileName[1]=='d') {
      /* create directory */
      FRESULT err;
      err = f_mkdir(&FileName[6]);
      if ((err != FR_OK) && (err != FR_EXIST)) {
        report_fatfs_error(err,&FileName[6]);
      }
      /* and set timestamp */
      FILINFO fno;
      strncpy(fno.fname, &FileName[6], FF_LFN_BUF);
      fno.fdate = FileName[2] + (FileName[3]<<8);
      fno.ftime = FileName[4] + (FileName[5]<<8);
      err = f_utime(&FileName[6],&fno);
      if (err != FR_OK) {
        report_fatfs_error(err,&FileName[6]);
      }
    } else if (FileName[1]=='f') {
      /* create file */
      FRESULT err;
      err = f_open(&pFile, &FileName[6], FA_WRITE | FA_CREATE_ALWAYS);
      if (err != FR_OK) {
        report_fatfs_error(err,&FileName[6]);
      }
      err = f_lseek(&pFile, pFileSize);
      if (err != FR_OK) {
        report_fatfs_error(err,&FileName[6]);
      }
      err = f_lseek(&pFile, 0);
      if (err != FR_OK) {
        report_fatfs_error(err,&FileName[6]);
      }
    } else if (FileName[1]=='D') {
      /* delete */
      FRESULT err;
      err = f_unlink(&FileName[6]);
      if (err != FR_OK) {
        report_fatfs_error(err,&FileName[6]);
      }
    } else if (FileName[1]=='C') {
      /* change working directory */
      FRESULT err;
      err = f_chdir(&FileName[6]);
      if (err != FR_OK) {
        report_fatfs_error(err,&FileName[6]);
      }
    } else if (FileName[1]=='I') 
    {
      /* get file info */
      FRESULT err;
      FILINFO fno;
      //fno.lfname = &((char*)fbuff)[0];
      fno.fsize = 256;
      err =  f_stat(&FileName[6],&fno);
      if (err == FR_OK) {
        strncpy((char *)fbuff, fno.fname, sizeof(fbuff));
        char *msg = &((char*)fbuff)[0];
        msg[0] = 'A';
        msg[1] = 'x';
        msg[2] = 'o';
        msg[3] = 'f';
        *(int32_t *)(&msg[4]) = fno.fsize;
        *(int32_t *)(&msg[8]) = fno.fdate + (fno.ftime<<16);
        strcpy(&msg[12], &FileName[6]);
        int l = strlen(&msg[12]);
        chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )msg, l+13);
      }
    }
  }
}


static void CloseFile(void) {
  FRESULT err;
  err = f_close(&pFile);
  if (err != FR_OK) {
    report_fatfs_error(err,&FileName[0]);
  }
  if (!FileName[0]) {
    /* and set timestamp */
    FILINFO fno;
    fno.fdate = FileName[2] + (FileName[3]<<8);
    fno.ftime = FileName[4] + (FileName[5]<<8);
    err = f_utime(&FileName[6],&fno);
    if (err != FR_OK) {
      report_fatfs_error(err,&FileName[6]);
    }
  }
}


static void CopyPatchToFlash(void) {
    FlashPatch(0);
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
  uint32_t uPatchLoc = 0; // not used, need to remove
  reply[12] = (uint8_t)(uPatchLoc>>24);
  reply[13] = (uint8_t)(uPatchLoc>>16);
  reply[14] = (uint8_t)(uPatchLoc>>8);
  reply[15] = (uint8_t)(uPatchLoc);
  chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&reply[0]), 16);
}


void ReplySpilinkSynced(void) {
  uint8_t reply[5];
  reply[0] = 'A';
  reply[1] = 'x';
  reply[2] = 'o';
  reply[3] = 'Y';
  /* SPILINK pin high means Core is master (default), else synced */
  reply[4] = !palReadPad(SPILINK_JUMPER_PORT, SPILINK_JUMPER_PIN);
  chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&reply[0]), 5);
}

typedef struct _PCDebug
{
  uint8_t c;
  int state;
} PCDebug;

#define PC_DBG_COUNT (0)
#if PC_DBG_COUNT
PCDebug dbg_received[PC_DBG_COUNT] DEBUG_DATA_SECTION;
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
      else if (c == 'T') { /* change preset */
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
      else if (c == 'A') { /* append data to sdcard file */
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
      else if (c == 'b') { // bin header
        offset = GetPatchHeaderLoc();
        index = 0;
        state = 4;
      }
      else if (c == 'S') { /* stop patch */
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
        state = 0;
        header = 0;
        StopPatch();
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
        state = 0;
        header = 0;
// #ifdef DEBUG_SERIAL
//        chprintf((BaseSequentialStream * )&SD2,"ping\r\n");
// #endif
        AckPending = 1;
      }
      else if (c == 'c') { /* close sdcard file */
        state = 0;
        header = 0;
        CloseFile();
        AckPending = 1;
      }
#if INBUILT_MOUNTER_FLASHER
      else if (c == 'm') { /* start mounter*/
        state = 0;
        header = 0;
        StopPatch();
        StartMounter();
      }
      else if (c == 'l') { /* start flasher*/
        state = 0;
        header = 0;
        StopPatch();
        StartFlasher();
      }
#endif
      else
        state = 0;
      break;
    }
  }
  else if (header == 'b') { // bin header
    *((unsigned char *)offset) = c;
    offset++;
    index++;
    if(index == GetPatchHeaderByteSize()) 
    {
      header = 0;
      state = 0;
      AckPending = 1;  
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
      SetPatchOffset(offset);
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
  else if (header == 'C') { // Create and open file on SD card
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
        ManipulateFile();
        header = 0;
        state = 0;
        AckPending = 1;
      }
    }
  }
  else if (header == 'A') { // append data to sdcard file 
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
      position = GetPatchMainLoc();
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
          err = f_write(&pFile, (char *)GetPatchMainLoc(), length,
                        (void *)&bytes_written);
          if (err != FR_OK) {
            report_fatfs_error(err,0);
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
  else if (header == 'B') { // Buttons
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
  else if (header == 'R') { // preset data set
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
      chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&read_repy_header[0]), 3 * 4);
      chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(offset), value);

      AckPending = 1;
      header = 0;
      state = 0;
      break;
    }
  }
  else if (header == 'y') { /* generic read, 32bit */
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
      chnWrite((BaseSequentialStream * )&BDU1, (const unsigned char* )(&read_repy_header[0]), 3 * 4);

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
