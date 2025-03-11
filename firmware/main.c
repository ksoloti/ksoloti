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
 
#include "axoloti_defines.h"

#if BOARD_KSOLOTI_CORE_H743
    #include "sdram.h"
    #include "stm32h7xx_ll_fmc.h"
#else
    #include "sdram.h"
    #include "stm32f4xx_fmc.h"
#endif

#include "ch.h"
#include "hal.h"
#include "chprintf.h"
#include "shell.h"
#include "string.h"
#include <stdio.h>

#include "codec.h"
#include "ui.h"
#include "midi.h"
#include "sdcard.h"
#include "patch.h"
#include "pconnection.h"
#include "axoloti_math.h"
#include "axoloti_board.h"
#include "exceptions.h"
#include "watchdog.h"

#include "usbcfg.h"
#include "sysmon.h"

#include "debug.h"

#ifdef FW_SPILINK
#include "spilink.h"
#endif

#ifdef FW_I2SCODEC
#include "i2scodec.h"
#endif

#if BOARD_KSOLOTI_CORE_H743
    // done in the makefile like it should be!
#else
    #include "sdram.c"
    #include "stm32f4xx_fmc.c"
#endif

#include "board.h"
#include "ksoloti_boot_options.h"

/*===========================================================================*/
/* Initialization and main thread.                                           */
/*===========================================================================*/


extern void MY_USBH_Init(void);
#ifdef FW_I2SCODEC
extern void i2s_init(void);
#endif

// YODOH7 maybe think of keepinf only the ram functions for both this and the slash stuff?
void KsolotiSleepMilliseconds(uint32_t uMiliseconds)
{
  uint32_t uTotalTicks = MS2RTT(uMiliseconds);
  uint32_t uStartTick = DWT->CYCCNT;
  while (DWT->CYCCNT - uStartTick < uTotalTicks)
    ;
}

int main(void) {

#if BOARD_KSOLOTI_CORE_H743
    // TODOH7 - More investication needed
    // Akso doesn't do this and I am not sure of the performance benefits
    // on the H7, the ITCM fast code memory at the moment by default
    // is all being used by the patcher.
#else        
    /* copy vector table to SRAM1! */
    #pragma GCC diagnostic push
    #pragma GCC diagnostic ignored "-Wnonnull"
        memcpy((char *)0x20000000, (const char)0x00000000, 0x200);
    #pragma GCC diagnostic pop

    /* remap SRAM1 to 0x00000000 */
    SYSCFG->MEMRMP |= 0x03;
#endif

    // Check to see if we need to start the flasher.
    CheckForFlasherBoot();

    // Initialise HAL and Chibios
    halInit();
    chSysInit();

#ifdef FW_SPILINK
    pThreadSpilink = 0;
#endif

#if BOARD_KSOLOTI_CORE_H743
    // Use the MPU to turn cache off for the memory
    // we use to store the sdcard io buffers in.
    extern uint32_t *__ram0nc_base__;
    mpuConfigureRegion(MPU_REGION_7,
        &__ram0nc_base__,
        MPU_RASR_ATTR_AP_RW_RW |
        MPU_RASR_ATTR_SHARED_DEVICE |
        MPU_RASR_ATTR_NON_CACHEABLE |
        MPU_RASR_SIZE_8K |
        MPU_RASR_ENABLE);
#endif

    // Check to see if we need to start the Mounter.
    CheckForMounterBoot();

    // Start the SDCard driver
    sdcard_init();

    // Start the system monitor
    sysmon_init();

    // If we have serial debug turned on, init the GPIO pins and start the driver
#if ENABLE_SERIAL_DEBUG
    /* SD2 for serial debug output */
    palSetPadMode(GPIOA, 3, PAL_MODE_ALTERNATE(7) | PAL_MODE_INPUT); /* RX */
    palSetPadMode(GPIOA, 2, PAL_MODE_OUTPUT_PUSHPULL); /* TX */
    palSetPadMode(GPIOA, 2, PAL_MODE_ALTERNATE(7)); /* TX */

    /* 115200 baud */
    static const SerialConfig sd2Cfg = {115200, 0, 0, 0};
    sdStart(&SD2, &sd2Cfg);

    LogUartMessage("Debug logging enabled on A2/A3\n");

#endif

    
    exception_init();

    InitPatch0();

#if FW_USBAUDIO
    InitUsbAudio();
#endif

    InitPConnection();


    chThdSleepMilliseconds(10);

    /* Pull up SPILINK detector (HIGH means MASTER i.e. regular operation) */
    palSetPadMode(SPILINK_JUMPER_PORT, SPILINK_JUMPER_PIN, PAL_MODE_INPUT_PULLUP);

    axoloti_board_init();
    adc_init();
    adc_convert();
    axoloti_math_init();
    midi_init();
    start_dsp_thread();
    ui_init();

    palClearPad(LED1_PORT, LED1_PIN);
    palSetPad(LED2_PORT, LED2_PIN);

    configSDRAM();


    // memTest();
    
    palSetPad(LED1_PORT, LED1_PIN);
    palClearPad(LED2_PORT, LED2_PIN);

    palSetPad(LED1_PORT, LED1_PIN);
    palClearPad(LED2_PORT, LED2_PIN);

    bool_t is_master = palReadPad(SPILINK_JUMPER_PORT, SPILINK_JUMPER_PIN);

    codec_init(is_master);
#ifdef FW_SPILINK
    spilink_init(is_master);
#endif
#ifdef FW_I2SCODEC
    i2s_init();
#endif

    if (!palReadPad(SW2_PORT, SW2_PIN)) {
        /* button S2 not pressed */
        // watchdog_init();
        chThdSleepMilliseconds(1);
    }

    MY_USBH_Init();

    if (!exception_check()) {
        /* Only try mounting SD and booting a patch when no exception is reported */

        sdcard_attemptMountIfUnmounted();

        /* Patch start can be skipped by holding S2 during boot */
        if (!palReadPad(SW2_PORT, SW2_PIN)) {

            if (fs_ready) {
                LoadPatchStartSD();
                chThdSleepMilliseconds(100);
            }

            /* If no patch booting or running yet try loading from flash */
            // if (patchStatus == STOPPED) {
            if (patchStatus != RUNNING) {
                LoadPatchStartFlash();
            }
        }
    }

#if FW_USBAUDIO
    EventListener audioEventListener;
    chEvtRegisterMask(&ADU1.event, &audioEventListener, AUDIO_EVENT);

    while (1) 
    {
        chEvtWaitOne(AUDIO_EVENT);
        uint32_t  evt = chEvtGetAndClearFlags(&audioEventListener);

        if(evt & AUDIO_EVENT_USB_CONFIGURED)
            LogTextMessage("Audio USB Configured.");
        else if(evt & AUDIO_EVENT_USB_SUSPEND)
            LogTextMessage("Audio USB Suspend");
        else if(evt & AUDIO_EVENT_USB_WAKEUP)
            LogTextMessage("Audio USB Wakeup.");
        else if(evt & AUDIO_EVENT_USB_STALLED)
            LogTextMessage("Audio USB Stalled.");
        else if(evt & AUDIO_EVENT_USB_RESET)
            LogTextMessage("Audio USB Reset.");
        else if(evt & AUDIO_EVENT_USB_ENABLE)
            LogTextMessage("Audio USB Enable.");
        else if(evt & AUDIO_EVENT_MUTE)
            LogTextMessage("Audio mute changed.");
        else if(evt & AUDIO_EVENT_VOLUME)
            LogTextMessage("Audio volume changed.");
        else if(evt & AUDIO_EVENT_INPUT)
            LogTextMessage("Audio input state changed = %u", aduState.isInputActive);
        else if(evt & AUDIO_EVENT_OUTPUT)
            LogTextMessage("Audio output state changed = %u", aduState.isOutputActive);
        else if(evt & AUDIO_EVENT_FORMAT)
            LogTextMessage("Audio Format type changed = %u", aduState.currentSampleRate);

        connectionFlags.usbActive = aduIsUsbInUse();
        LogTextMessage("connectionFlags.usbActive = %u", connectionFlags.usbActive );
    }
#else
    while (1) {
        chThdSleepMilliseconds(1000);
    }
#endif
}


void HAL_Delay(unsigned int n) {
    chThdSleepMilliseconds(n);
}


void _sbrk(void) {
    while (1);
}
