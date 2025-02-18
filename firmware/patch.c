/*
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
#include "ch.h"
#include "hal.h"
#include "patch.h"
#include "sdcard.h"
#include "string.h"
#include "axoloti_board.h"
#include "midi.h"
#include "watchdog.h"
#include "pconnection.h"
#include "sysmon.h"
#include "codec.h"
#include "axoloti_memory.h"
#ifdef FW_SPILINK
#include "spilink.h"
#endif
#include "audio_usb.h"


#if FW_USBAUDIO     
extern void aduDataExchange (int32_t *in, int32_t *out);
#endif

#if USE_EXTERNAL_USB_FIFO_PUMP
extern void usb_lld_external_pump(void);
#endif 

#define STACKSPACE_MARGIN 32
// #define DEBUG_PATCH_INT_ON_GPIO 1

patchMeta_t patchMeta;

volatile patchStatus_t patchStatus;

uint32_t     dspLoad200; // DSP load: Values 0-200 correspond to 0-100%

uint32_t DspTime;

char loadFName[64] = "";
loadPatchIndex_t loadPatchIndex = UNINITIALIZED;
static const char* index_fn = "/index.axb";

static int32_t inbuf[32];
static int32_t* outbuf;

#if FW_USBAUDIO
    #if USB_AUDIO_CHANNELS == 2
        static int32_t inbufUsb[32];
        static int32_t outbufUsb[32];
        void usb_clearbuffer(void)
        {
            uint_fast8_t i; for(i=0; i<32; i++) {
            	inbufUsb[i] = 0;
                outbufUsb[i] = 0;
            }
        }
    #elif USB_AUDIO_CHANNELS == 4
        static int32_t inbufUsb[64];
        static int32_t outbufUsb[64];
        void usb_clearbuffer(void)
        {
            uint_fast8_t i; for(i=0; i<64; i++) {
            	inbufUsb[i] = 0;
                outbufUsb[i] = 0;
            }
        }
    #endif
#endif


static int16_t nThreadsBeforePatch;
static WORKING_AREA(waThreadDSP, 7200) __attribute__ ((section (".ccmramend")));
static Thread* pThreadDSP = 0;

// Default valued for safety preset `Normal`
uint16_t uPatchUIMidiCost  = DSP_UI_MIDI_COST;
uint8_t  uPatchUsbLimit200 = DSP_LIMIT200;

void SetPatchSafety(uint16_t uUIMidiCost, uint8_t uDspLimit200)
{
    uPatchUIMidiCost = uUIMidiCost;
    uPatchUsbLimit200 = uDspLimit200;
#if USB_AUDIO_CHANNELS == 4
    // we have some extra overhead
    uPatchUIMidiCost += 60;
#endif
}

static void SetPatchStatus(patchStatus_t status)
{
    if(patchStatus != status)
    {
        if(status == RUNNING)
        {
            // DSP priority
            chThdSetPriority(PATCH_DSP_PRIO);
#if USE_EXTERNAL_USB_FIFO_PUMP
            // Switch to external fifo pump
            usb_lld_use_external_pump(true);
#endif
        }
        else
        {
            // Normal priority
            chThdSetPriority(PATCH_NORMAL_PRIO);

#if USE_EXTERNAL_USB_FIFO_PUMP
            if(patchStatus == RUNNING)
            {
                // switch to fifo pump thread.
                usb_lld_use_external_pump(false);
            }
#endif
        }
    }
    patchStatus = status;    
}

void InitPatch0(void) {
    SetPatchStatus(STOPPED);
    patchMeta.fptr_patch_init = 0;
    patchMeta.fptr_patch_dispose = 0;
    patchMeta.fptr_dsp_process = 0;
    patchMeta.fptr_MidiInHandler = 0;
    patchMeta.fptr_applyPreset = 0;
    patchMeta.pPExch = NULL;
    patchMeta.numPEx = 0;
    patchMeta.pDisplayVector = 0;
    patchMeta.initpreset_size = 0;
    patchMeta.npresets = 0;
    patchMeta.npreset_entries = 0;
    patchMeta.pPresets = 0;
    patchMeta.patchID = 0;
}

#define USE_MOVING_AVERAGE 0

#if USE_MOVING_AVERAGE
#include "moving_average.h"
float madata[100];
static moving_average_data ma;
#endif

#if USE_PATCH_DSPTIME_SMOOTHING_MS
#include "moving_average.h"
float dsptimeSmoothingData[3 * USE_PATCH_DSPTIME_SMOOTHING_MS];
static moving_average_data dsptimeSmoothing;
#endif

static int16_t GetNumberOfThreads(void) {
#ifdef CH_CFG_USE_REGISTRY
    int16_t i = 1;
    Thread* thd1 = chRegFirstThread();

    while (thd1) {
        i++;
        thd1 = chRegNextThread(thd1);
    }
    return i;
#else
    return -1;
#endif
}


void CheckStackOverflow(void) {
#ifdef CH_CFG_USE_REGISTRY
#ifdef CH_DBG_FILL_THREADS
    Thread* thd = chRegFirstThread();

    /* skip 1st thread, main thread */
    thd = chRegNextThread (thd);
    int critical = 0;
    int nfree = 0;

    while(thd) {
        char* stk = (char*) (thd + 1);
        nfree = 0;

        while (*stk == CH_DBG_STACK_FILL_VALUE) {
            nfree++;
            stk++;
            if (nfree >= STACKSPACE_MARGIN) {
                break;
            }
        }

        if (nfree < STACKSPACE_MARGIN) {
            critical = 1;
            break;
        }

        thd = chRegNextThread(thd);
    }

    if (critical) {
        const char* name = chRegGetThreadName(thd);

        if (name != 0) {
            if (nfree) {
                LogTextMessage("Thread %s: stack critical %d", name, nfree);
            }
            else {
                LogTextMessage("Thread %s: stack overflow", name);
            }
        }
        else {
            if (nfree) {
                LogTextMessage("Thread ??: stack critical %d", nfree);
            }
            else {
                LogTextMessage("Thread ??: stack overflow");
            }
        }
    }
#endif
#endif
}


static void StopPatch1(void) {
    if (patchMeta.fptr_patch_dispose != 0) {
        CheckStackOverflow();
        (patchMeta.fptr_patch_dispose)();

        /* Check if the number of threads after patch disposal is the same as before */
        uint8_t j = 20;
        int16_t i = GetNumberOfThreads();

        /* Try sleeping up to 1 second so threads can terminate */
        while ((j--) && (i != nThreadsBeforePatch)) {
            chThdSleepMilliseconds(50);
            i = GetNumberOfThreads();
        }

        if (i != nThreadsBeforePatch) {
            LogTextMessage("Error: patch did not terminate its thread(s)");
        }
    }

    // UIGoSafe();
    InitPatch0();
    sysmon_enable_blinker();
}


static int StartPatch1(void) {
    KVP_ClearObjects();

    sdcard_attemptMountIfUnmounted();

    /* Reinit pin configuration for ADC */
    adc_configpads();

    uint32_t* ccm; /* Clear CCMRAM area declared in ramlink_*.ld */
    for (ccm = (uint32_t*) 0x10000000; ccm < (uint32_t*) 0x1000C000; ccm++) {
        *ccm = 0;
    }

    patchMeta.fptr_dsp_process = 0;
    nThreadsBeforePatch = GetNumberOfThreads();
    patchMeta.fptr_patch_init = (fptr_patch_init_t)(PATCHMAINLOC + 1);
    (patchMeta.fptr_patch_init)(GetFirmwareID());

    if (patchMeta.fptr_dsp_process == 0) {
        report_patchLoadFail((const char*) &loadFName[0]);
        SetPatchStatus(STARTFAILED);
        return -1;
    }

    int32_t sdrem = sdram_get_free();
    if (sdrem < 0) {
        StopPatch1();
        SetPatchStatus(STARTFAILED);
        patchMeta.patchID = 0;
        report_patchLoadSDRamOverflow((const char*) &loadFName[0], -sdrem);
        return -1;
    }

    SetPatchStatus(RUNNING);
    return 0;
}


 __attribute__((__noreturn__)) static msg_t ThreadDSP(void* arg) {
    (void)(arg);

#if CH_CFG_USE_REGISTRY
    chRegSetThreadName("dsp");
#endif
    codec_clearbuffer();

#if FW_USBAUDIO
    usb_clearbuffer();
#endif

    while (1) {

#ifdef DEBUG_PATCH_INT_ON_GPIO
        palSetPad(GPIOA, 2);
#endif

        /* Codec DSP cycle */
        eventmask_t evt = chEvtWaitOne((eventmask_t)7);
        if (evt == 1) {
#if FW_USBAUDIO             
            uint16_t uDspTimeslice = DSP_CODEC_TIMESLICE - uPatchUIMidiCost - DSP_USB_AUDIO_FIRMWARE_COST;;
            if(aduIsUsbInUse())
                uDspTimeslice -= DSP_USB_AUDIO_STREAMING_COST;
#else
            uint16_t uDspTimeslice = DSP_CODEC_TIMESLICE - uPatchUIMidiCost;
#endif
            static uint32_t tStart;
            tStart = hal_lld_get_counter_value();
            watchdog_feed();

            if (patchStatus == RUNNING) {
                /* Patch running */
#if FW_USBAUDIO             
                (patchMeta.fptr_dsp_process)(inbuf, outbuf, inbufUsb, outbufUsb);
#else
                (patchMeta.fptr_dsp_process)(inbuf, outbuf);
#endif
            }
            else if (patchStatus == STOPPING) {
                codec_clearbuffer();
                #if FW_USBAUDIO
                  usb_clearbuffer();
                #endif

                StopPatch1();
                SetPatchStatus(STOPPED);
                codec_clearbuffer();
            }
            else if (patchStatus == STOPPED) {
                codec_clearbuffer();
                #if FW_USBAUDIO
                  usb_clearbuffer();
                #endif
            }

            adc_convert();

            DspTime = RTT2US(hal_lld_get_counter_value() - tStart);

#if USE_MOVING_AVERAGE
            ma_add(&ma, DspTime);
#endif

#if USE_PATCH_DSPTIME_SMOOTHING_MS
            ma_add(&dsptimeSmoothing, DspTime);
            dspLoad200 = (2000 * ma_average(&dsptimeSmoothing)) / uDspTimeslice;
#else
            dspLoad200 = (2000 * DspTime) / uDspTimeslice;
#endif


            if (dspLoad200 > uPatchUsbLimit200) {
                /* Overload: clear output buffers and give other processes a chance */
                codec_clearbuffer();

#if FW_USBAUDIO
                // reset USB audio
                aduReset();
#endif
                // LogTextMessage("DSP overrun");
                connectionFlags.dspOverload = true;

                /* DSP overrun penalty, keeping cooperative with lower priority threads */
                chThdSleepMilliseconds(1);
            }
#if USE_EXTERNAL_USB_FIFO_PUMP            
            usb_lld_external_pump();
#endif
        }
        else if (evt == 2) {
            /* load patch event */
            codec_clearbuffer();
            #if FW_USBAUDIO
                usb_clearbuffer();
            #endif

            StopPatch1();
            SetPatchStatus(STOPPED);

            if (loadFName[0]) {
                int res = sdcard_loadPatch1(loadFName);
                if (!res) StartPatch1();
            }
            else if (loadPatchIndex == START_FLASH) {
                /* Patch in flash sector 11 */
                memcpy((uint8_t*) PATCHMAINLOC, (uint8_t*) PATCHFLASHLOC, PATCHFLASHSIZE);
                if ((*(uint32_t*) PATCHMAINLOC != 0xFFFFFFFF) && (*(uint32_t*) PATCHMAINLOC != 0)) {
                    StartPatch1();
                }
            }
            else if (loadPatchIndex == START_SD) {
                strcpy(&loadFName[0], "/start.bin");
                int res = sdcard_loadPatch1(loadFName);
                if (!res) StartPatch1();
            }
            else {
                FRESULT err;
                FIL f;
                uint32_t bytes_read;

                err = f_open(&f, index_fn, FA_READ | FA_OPEN_EXISTING);
                if (err) {
                    report_fatfs_error(err, index_fn);
                }

                err = f_read(&f, (uint8_t*) PATCHMAINLOC, 0xE000, (void*) &bytes_read);
                if (err != FR_OK) {
                    report_fatfs_error(err, index_fn);
                    continue;
                }

                err = f_close(&f);
                if (err != FR_OK) {
                    report_fatfs_error(err, index_fn);
                    continue;
                }

                char* t;
                t = (char*) PATCHMAINLOC;
                int32_t cindex = 0;

                // LogTextMessage("load %d %d %x", index, bytes_read, t);
                while (bytes_read) {
                    // LogTextMessage("scan %d", *t);
                    if (cindex == loadPatchIndex) {
                        // LogTextMessage("match %d", index);
                        char *p, *e;
                        p = t;
                        e = t;

                        while ((*e != '\n') && bytes_read) {
                            e++;
                            bytes_read--;
                        }

                        if (bytes_read) {
                             e = e - 4;
                            *e++ = '/';
                            *e++ = 'p';
                            *e++ = 'a';
                            *e++ = 't';
                            *e++ = 'c';
                            *e++ = 'h';
                            *e++ = '.';
                            *e++ = 'b';
                            *e++ = 'i';
                            *e++ = 'n';
                            *e = 0;

                            loadFName[0] = '/';
                            strcpy(&loadFName[1], p);

                            int res = sdcard_loadPatch1(loadFName);
                            if (!res) {
                                StartPatch1();
                            }

                            if (patchStatus != RUNNING) {
                                loadPatchIndex = START_SD;
                                strcpy(&loadFName[0], "/start.bin");
                                res = sdcard_loadPatch1(loadFName);
                                if (!res) StartPatch1();
                            }
                        }
                        goto cont;
                    }

                    if (*t == '\n') {
                        cindex++;
                    }

                    t++;
                    bytes_read--;
                }

                if (!bytes_read) {
                    LogTextMessage("Patch load out of range: %d", loadPatchIndex);
                    loadPatchIndex = START_SD;
                    strcpy(&loadFName[0], "/start.bin");
                    int res = sdcard_loadPatch1(loadFName);
                    if (!res) StartPatch1();
                }

                cont: ;
            }
        }
        else if (evt == 4) {
            /* Start patch */
            codec_clearbuffer();
            #if FW_USBAUDIO
                usb_clearbuffer();
            #endif
            StartPatch1();
        }

#ifdef DEBUG_PATCH_INT_ON_GPIO
        palClearPad(GPIOA, 2);
#endif

    }
    // return (msg_t)0;
}


void StopPatch(void) {
    if (!patchStatus) {
        SetPatchStatus(STOPPING);

        while (1) {
          chThdSleepMilliseconds(1);
            if (patchStatus == STOPPED) {
                break;
            }  
        }

        StopPatch1();
        SetPatchStatus(STOPPED);
    }
}


int StartPatch(void) {
    chEvtSignal(pThreadDSP, (eventmask_t)4);

    while ((patchStatus != RUNNING) && (patchStatus != STARTFAILED)) {
        chThdSleepMilliseconds(1);
    }

    if (patchStatus == STARTFAILED) {
        SetPatchStatus(STOPPED);
        LogTextMessage("Patch start failed", patchStatus);
    }

    return 0;
}


void start_dsp_thread(void) {
#if USE_MOVING_AVERAGE
    ma_init(&ma, madata, sizeof(madata) / sizeof(float), false);
#endif

#if USE_PATCH_DSPTIME_SMOOTHING_MS
    ma_init(&dsptimeSmoothing, dsptimeSmoothingData, sizeof(dsptimeSmoothingData) / sizeof(float), false);
#endif

    if (!pThreadDSP)
        pThreadDSP = chThdCreateStatic(waThreadDSP, sizeof(waThreadDSP), PATCH_DSP_PRIO, (void*) ThreadDSP, NULL);
}


void computebufI(int32_t* inp, int32_t* outp) {
    uint_fast8_t i; for (i = 0; i < 32; i++) {
        inbuf[i] = inp[i];
    }

    outbuf = outp;

#if FW_USBAUDIO     
    aduDataExchange(inbufUsb, outbufUsb);
#endif    

    chSysLockFromIsr();
    chEvtSignalI(pThreadDSP, (eventmask_t)1);
    chSysUnlockFromIsr();
}


void MidiInMsgHandler(midi_device_t dev, uint8_t port, uint8_t status, uint8_t data1, uint8_t data2) {
    if (patchStatus == RUNNING) {
        (patchMeta.fptr_MidiInHandler)(dev, port, status, data1, data2);
    }
}


void LoadPatch(const char* name) {
    strcpy(loadFName, name);
    loadPatchIndex = BY_FILENAME;
    chEvtSignal(pThreadDSP, (eventmask_t)2);
}


void LoadPatchStartSD(void) {
    strcpy(loadFName, "/start.bin");
    loadPatchIndex = START_SD;
    chEvtSignal(pThreadDSP, (eventmask_t)2);
    chThdSleepMilliseconds(50);
}


void LoadPatchStartFlash(void) {
    loadPatchIndex = START_FLASH;
    chEvtSignal(pThreadDSP, (eventmask_t)2);
}


void LoadPatchIndexed(uint32_t index) {
    loadPatchIndex = index;
    loadFName[0] = 0;
    chEvtSignal(pThreadDSP, (eventmask_t)2);
}


loadPatchIndex_t GetIndexOfCurrentPatch(void) {
    return loadPatchIndex;
}
