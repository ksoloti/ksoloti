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
    // TODOH7
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
#ifdef FW_SPILINK
#include "spilink.h"
#endif

#ifdef FW_I2SCODEC
#include "i2scodec.h"
#endif

/*
 * except.c
 *
 *  Created on: Aug 30, 2016
 */

#define ENABLE_EXCPT_DUMP

 #if defined(ENABLE_EXCPT_DUMP)
 #include <stdint.h>
 #include <ch.h>
 #include <string.h>
 
 /**
  * Executes the BKPT instruction that causes the debugger to stop.
  * If no debugger is attached, this will be ignored
  */
 #define bkpt() __asm volatile("BKPT #0\n")
 
 void NMI_Handler(void) {
     //TODO
     while(1);
 }
 
 //See http://infocenter.arm.com/help/topic/com.arm.doc.dui0552a/BABBGBEC.html
 typedef enum  {
     Reset = 1,
     NMI = 2,
     HardFault = 3,
     MemManage = 4,
     BusFault = 5,
     UsageFault = 6,
 } FaultType;
 
 char exceptionstr[50];
 
 void sendchar(char c)
{
 
     while (!(USART2->ISR & USART_ISR_TXE_TXFNF));
     USART2->TDR = ( c );
 }
 
 void exception_dump( char *s )
 {
     do
     {
         sendchar( *s );
     }
     while( *s++ );
 }
 
 void faulttype( FaultType type )
 {
 
     switch( type )
     {
         case NMI:
           exception_dump( "NMI" );
           break;
 
         case Reset:
           exception_dump( "Reset" );
           break;
 
         case BusFault:
           exception_dump( "Bus Fault" );
           break;
 
         case HardFault:
           exception_dump( "Hard Fault" );
           break;
 
         case UsageFault:
           exception_dump( "Usage Fault" );
           break;
 
         case MemManage:
           exception_dump( "Memory Manager" );
           break;
     }
 }
 
 
 void hex2string( uint32_t hex )
 {
 uint8_t i;
 char ascii = 0x0;
 uint32_t divider = 0x10000000;
 
 
     exception_dump("0x");
 
     for ( i=0; i<8; i++ )
     {
         ascii = hex / divider;
         hex -= (ascii * divider);
         divider /= 0x10;
 
         switch (ascii)
         {
             case 0xf:
                 sendchar('F');
                 break;
 
             case 0xe:
                 sendchar('E');
                 break;
 
             case 0xd:
                 sendchar('D');
                 break;
 
             case 0xc:
                 sendchar('C');
                 break;
 
             case 0xb:
                 sendchar('B');
                 break;
 
             case 0xa:
                 sendchar('A');
                 break;
 
             default:
                 ascii += 0x30;
                 sendchar(ascii);
                 break;
         }
     }
 }
 
 void HardFault_Handler(void) {
     //Copy to local variables (not pointers) to allow GDB "i loc" to directly show the info
     //Get thread context. Contains main registers including PC and LR
     struct port_extctx ctx;
     memcpy(&ctx, (void*)__get_PSP(), sizeof(struct port_extctx));
     (void)ctx;
     //Interrupt status register: Which interrupt have we encountered, e.g. HardFault?
     FaultType faultType = (FaultType)__get_IPSR();
     (void)faultType;
     //For HardFault/BusFault this is the address that was accessed causing the error
     uint32_t faultAddress = SCB->BFAR;
     (void)faultAddress;
     //Flags about hardfault / busfault
     //See http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.dui0552a/Cihdjcfc.html for reference
     bool isFaultPrecise = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 1) ? true : false);
     bool isFaultImprecise = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 2) ? true : false);
     bool isFaultOnUnstacking = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 3) ? true : false);
     bool isFaultOnStacking = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 4) ? true : false);
     bool isFaultAddressValid = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 7) ? true : false);
     (void)isFaultPrecise;
     (void)isFaultImprecise;
     (void)isFaultOnUnstacking;
     (void)isFaultOnStacking;
     (void)isFaultAddressValid;
     //Output some debug info about the expection
     exception_dump( "********** Exception Dump **********\r\n" );
 
     if( isFaultPrecise )
     {
         exception_dump( "Fault Precise     : TRUE\r\n" );
     }
     else
     {
         exception_dump( "Fault Precise     : FALSE\r\n" );
     }
 
     if( isFaultImprecise )
     {
         exception_dump( "Fault Imprecise   : TRUE\r\n" );
     }
     else
     {
         exception_dump( "Fault Imprecise   : FALSE\r\n" );
     }
 
     if( isFaultOnStacking )
     {
         exception_dump( "Fault Onstacking  : TRUE\r\n" );
     }
     else
     {
         exception_dump( "Fault Onstacking  : FALSE\r\n" );
     }
 
     if( isFaultOnUnstacking )
     {
         exception_dump( "Fault Unstacking  : TRUE\r\n" );
     }
     else
     {
         exception_dump( "Fault Unstacking  : FALSE\r\n" );
     }
 
     if( isFaultAddressValid )
     {
         exception_dump( "Fault Valid Addr  : TRUE\r\n" );
     }
     else
     {
         exception_dump( "Fault Valid Addr  : FALSE\r\n" );
     }
 
     exception_dump( "Fault Addr        : " );
     hex2string( faultAddress );
     exception_dump( "\r\n" );
 
     exception_dump( "Fault Type        : " );
     faulttype( faultType );
     exception_dump( "\r\n" );
 
     exception_dump( "Context PC        : " );
     hex2string( ( uint32_t )ctx.pc );
     exception_dump( "\r\n" );
 
     exception_dump( "Context Thread    : " );
     hex2string( ( uint32_t )ctx.lr_thd );
     exception_dump( "\r\n" );
 
 
     //Cause debugger to stop. Ignored if no debugger is attached
     bkpt();
     NVIC_SystemReset();
 }
 
 void BusFault_Handler(void) __attribute__((alias("HardFault_Handler")));
 
 void UsageFault_Handler(void) {
     //Copy to local variables (not pointers) to allow GDB "i loc" to directly show the info
     //Get thread context. Contains main registers including PC and LR
     struct port_extctx ctx;
     memcpy(&ctx, (void*)__get_PSP(), sizeof(struct port_extctx));
     (void)ctx;
     //Interrupt status register: Which interrupt have we encountered, e.g. HardFault?
     FaultType faultType = (FaultType)__get_IPSR();
     (void)faultType;
     //Flags about hardfault / busfault
     //See http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.dui0552a/Cihdjcfc.html for reference
     bool isUndefinedInstructionFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 0) ? true : false);
     bool isEPSRUsageFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 1) ? true : false);
     bool isInvalidPCFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 2) ? true : false);
     bool isNoCoprocessorFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 3) ? true : false);
     bool isUnalignedAccessFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 8) ? true : false);
     bool isDivideByZeroFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 9) ? true : false);
     (void)isUndefinedInstructionFault;
     (void)isEPSRUsageFault;
     (void)isInvalidPCFault;
     (void)isNoCoprocessorFault;
     (void)isUnalignedAccessFault;
     (void)isDivideByZeroFault;
     bkpt();
     NVIC_SystemReset();
 }
 
 void MemManage_Handler(void) {
     //Copy to local variables (not pointers) to allow GDB "i loc" to directly show the info
     //Get thread context. Contains main registers including PC and LR
     struct port_extctx ctx;
     memcpy(&ctx, (void*)__get_PSP(), sizeof(struct port_extctx));
     (void)ctx;
     //Interrupt status register: Which interrupt have we encountered, e.g. HardFault?
     FaultType faultType = (FaultType)__get_IPSR();
     (void)faultType;
     //For HardFault/BusFault this is the address that was accessed causing the error
     uint32_t faultAddress = SCB->MMFAR;
     (void)faultAddress;
     //Flags about hardfault / busfault
     //See http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.dui0552a/Cihdjcfc.html for reference
     bool isInstructionAccessViolation = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 0) ? true : false);
     bool isDataAccessViolation = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 1) ? true : false);
     bool isExceptionUnstackingFault = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 3) ? true : false);
     bool isExceptionStackingFault = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 4) ? true : false);
     bool isFaultAddressValid = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 7) ? true : false);
     (void)isInstructionAccessViolation;
     (void)isDataAccessViolation;
     (void)isExceptionUnstackingFault;
     (void)isExceptionStackingFault;
     (void)isFaultAddressValid;
     bkpt();
     NVIC_SystemReset();
 }
#endif

// //void NMI_Handler(void) { }
// void HardFault_Handler(unsigned long *hardfault_args)
// {
//     volatile unsigned long stacked_r0 ;
//     volatile unsigned long stacked_r1 ;
//     volatile unsigned long stacked_r2 ;
//     volatile unsigned long stacked_r3 ;
//     volatile unsigned long stacked_r12 ;
//     volatile unsigned long stacked_lr ;
//     volatile unsigned long stacked_pc ;
//     volatile unsigned long stacked_psr ;
//     volatile unsigned long _CFSR ;
//     volatile unsigned long _HFSR ;
//     volatile unsigned long _DFSR ;
//     volatile unsigned long _AFSR ;
//     volatile unsigned long _BFAR ;
//     volatile unsigned long _MMAR ;

//     // stacked_r0 = ((unsigned long)hardfault_args[0]) ;
//     // stacked_r1 = ((unsigned long)hardfault_args[1]) ;
//     // stacked_r2 = ((unsigned long)hardfault_args[2]) ;
//     // stacked_r3 = ((unsigned long)hardfault_args[3]) ;
//     // stacked_r12 = ((unsigned long)hardfault_args[4]) ;
//     // stacked_lr = ((unsigned long)hardfault_args[5]) ;
//     // stacked_pc = ((unsigned long)hardfault_args[6]) ;
//     // stacked_psr = ((unsigned long)hardfault_args[7]) ;

//     // Configurable Fault Status Register
//     // Consists of MMSR, BFSR and UFSR
//     _CFSR = (*((volatile unsigned long *)(0xE000ED28))) ;

//     // Hard Fault Status Register
//     _HFSR = (*((volatile unsigned long *)(0xE000ED2C))) ;

//     // Debug Fault Status Register
//     _DFSR = (*((volatile unsigned long *)(0xE000ED30))) ;

//     // Auxiliary Fault Status Register
//     _AFSR = (*((volatile unsigned long *)(0xE000ED3C))) ;

//     // Read the Fault Address Registers. These may not contain valid values.
//     // Check BFARVALID/MMARVALID to see if they are valid values
//     // MemManage Fault Address Register
//     _MMAR = (*((volatile unsigned long *)(0xE000ED34))) ;
//     // Bus Fault Address Register
//     _BFAR = (*((volatile unsigned long *)(0xE000ED38))) ;

//     __asm("BKPT #0\n") ; // Break into the debugger
// }

// void MemManage_Handler(void) { while (1); }
// void BusFault_Handler(void) { while (1); }
// void UsageFault_Handler(void) { while (1); }
// // void SVC_Handler(void) { }
// // void DebugMon_Handler(void) { }
// // void PendSV_Handler(void) { }


#if BOARD_KSOLOTI_CORE_H743
    // TODOH7
#else
    #include "sdram.c"
    #include "stm32f4xx_fmc.c"
#endif

#include "board.h"


// #if BOARD_KSOLOTI_CORE_H743
//   // TODOH7 - will be in SAI
//   uint32_t HAL_GetTick(void) {
//     return RTT2MS(DWT->CYCCNT);
//   }
// #endif

/*===========================================================================*/
/* Initialization and main thread.                                           */
/*===========================================================================*/


extern void MY_USBH_Init(void);
#ifdef FW_I2SCODEC
extern void i2s_init(void);
#endif

#if INBUILT_MOUNTER_FLASHER
#define INBUILT_FLASHER_TEST 1
#define MOUNTER_MAGIC 0x2a4d4f554e544552 /* *MOUNTER */
#define FLASHER_MAGIC 0x2a464c4153484552 /* *FLASHER */
volatile uint64_t g_startup_flags __attribute__ ((section (".noinit")));
extern int mounter(void);
extern void flasher(void);

void StartFlasher(void)
{
    g_startup_flags = FLASHER_MAGIC;
    NVIC_SystemReset();
}

void StartMounter(void)
{
    g_startup_flags = MOUNTER_MAGIC;
    NVIC_SystemReset();
}
#endif

#if ENABLE_SERIAL_DEBUG
    void LogUartMessage(const char* format, ...)
    {
        va_list ap;
        va_start(ap, format);
        chvprintf((BaseSequentialStream *)&SD2, format, ap);
        va_end(ap);
    }
    void LogUartMessageEol(const char* format, ...)
    {
        va_list ap;
        va_start(ap, format);
        chvprintf((BaseSequentialStream *)&SD2, format, ap);
        va_end(ap);
        chprintf((BaseSequentialStream *)&SD2, "\n");
    }
#else
    void LogUartMessage(const char* format, ...)
    {
    }
    void LogUartMessageEol(const char* format, ...)
    {
    }
#endif

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

    #if INBUILT_MOUNTER_FLASHER
    // shall we run the flasher?
    // the flasher needs to run from ram and does not use chibios
    if(g_startup_flags == FLASHER_MAGIC)
    {
        g_startup_flags=0;
        flasher();
    }
#endif

    halInit();
    chSysInit();


#if INBUILT_MOUNTER_FLASHER
    // shall we run the mounter?
    // the mounter uses the chibios we have here running from flash
    if(g_startup_flags == MOUNTER_MAGIC)
    {
        g_startup_flags=0;
        mounter();
    }
#endif

#ifdef FW_SPILINK
    pThreadSpilink = 0;
#endif

    // Use the MPU ro turn cache off for the memory
    // we use to store the sdcard io buffers in.
    extern uint32_t *__ram0nc_base__;
    mpuConfigureRegion(MPU_REGION_7,
        &__ram0nc_base__,
        MPU_RASR_ATTR_AP_RW_RW |
        MPU_RASR_ATTR_SHARED_DEVICE |
        MPU_RASR_ATTR_NON_CACHEABLE |
        MPU_RASR_SIZE_8K |
        MPU_RASR_ENABLE);

    sdcard_init();
    sysmon_init();

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
#if BOARD_KSOLOTI_CORE_H743
    // TODOH7
    MY_USBH_Init();
#else
    MY_USBH_Init();
#endif
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
