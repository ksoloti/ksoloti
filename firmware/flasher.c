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

#include "hal.h"
#include "ui.h"
#include "axoloti_board.h"
#include "exceptions.h"


void  __attribute__ ((section (".ram3text"))) KsolotiSleepMilliseconds(uint32_t uMiliseconds)
{ 
    uint32_t uTotalTicks = MS2RTT(uMiliseconds); 
    uint32_t uStartTick = DWT->CYCCNT; 
    while (DWT->CYCCNT - uStartTick < uTotalTicks) 
        ; 
}

// #define SERIALDEBUG

#ifdef SERIALDEBUG
#define DBGPRINTCHAR(x) sdPut(&SD2, x); // chThdSleepMilliseconds(1);

void dbgPrintHexDigit(uint8_t b)
{
    if (b > 9)
        DBGPRINTCHAR('a' + b - 10);
    else
        DBGPRINTCHAR('0' + b);
}

#define DBGPRINTHEX(x)                  \
    DBGPRINTCHAR(' ');                  \
    DBGPRINTCHAR('0');                  \
    DBGPRINTCHAR('x');                  \
    dbgPrintHexDigit((x >> 28) & 0x0f); \
    dbgPrintHexDigit((x >> 24) & 0x0f); \
    dbgPrintHexDigit((x >> 20) & 0x0f); \
    dbgPrintHexDigit((x >> 16) & 0x0f); \
    dbgPrintHexDigit((x >> 12) & 0x0f); \
    dbgPrintHexDigit((x >> 8) & 0x0f);  \
    dbgPrintHexDigit((x >> 4) & 0x0f);  \
    dbgPrintHexDigit((x) & 0x0f);       \
    DBGPRINTCHAR('\r');                 \
    DBGPRINTCHAR('\n');
#else
#define DBGPRINTCHAR(x)
#define DBGPRINTHEX(x)

#endif /* SERIALDEBUG */

#if BOARD_KSOLOTI_CORE_H743
    // TODOH7
    void flasher(void)
    {

    }
#else
    #include "sdram.h"
    
    #define FLASH_BASE_ADDR 0x08000000

    void __attribute__ ((section (".ram3text"))) FlashSystemReset(void)
    {
    __DSB();                                                          /* Ensure all outstanding memory accesses included
                                                                        buffered write are completed before reset */
    SCB->AIRCR  = (uint32_t)((0x5FAUL << SCB_AIRCR_VECTKEY_Pos)    |
                            (SCB->AIRCR & SCB_AIRCR_PRIGROUP_Msk) |
                                SCB_AIRCR_SYSRESETREQ_Msk    );         /* Keep priority group unchanged */
    __DSB();                                                          /* Ensure completion of memory access */

    for(;;)                                                           /* wait until reset */
    {
        __NOP();
    }
    }


    void __attribute__ ((section (".ram3text"))) DisplayAbortErr(int err)
    {
        DBGPRINTCHAR('0' + err);

        /* blink red slowly, green off */
        palWritePad(LED1_PORT, LED1_PIN, 0);

        int i = 10;
        while (i--)
        {
            palWritePad(LED2_PORT, LED2_PIN, 1);
            KsolotiSleepMilliseconds(1000);
            palWritePad(LED2_PORT, LED2_PIN, 0);
            KsolotiSleepMilliseconds(1000);
        }

        FlashSystemReset();
    }

    static uint32_t  __attribute__ ((section (".ram3text"))) revbit(uint32_t data) 
    {
        uint32_t result;
        __ASM
        volatile ("rbit %0, %1" : "=r" (result) : "r" (data));
        return result;
    }
    
    uint32_t  __attribute__ ((section (".ram3text"))) FlashCalcCRC32(uint8_t *buffer, uint32_t size) 
    {
        uint32_t i, j;
        uint32_t ui32x;

        RCC->AHB1ENR |= RCC_AHB1ENR_CRCEN;
        CRC->CR = 1;
        asm("NOP");
        asm("NOP");
        asm("NOP");
        //delay for hardware ready

        i = size >> 2;

        while (i--) {
            ui32x = *((uint32_t *)buffer);
            buffer += 4;
            ui32x = revbit(ui32x); //reverse the bit order of input data
            CRC->DR = ui32x;
            if ((i && 0xFFF) == 0)
            watchdog_feed();
        }
        ui32x = CRC->DR;

        ui32x = revbit(ui32x); //reverse the bit order of output data
        i = size & 3;
        while (i--) {
            ui32x ^= (uint32_t) * buffer++;

            for (j = 0; j < 8; j++)
            if (ui32x & 1)
                ui32x = (ui32x >> 1) ^ 0xEDB88320;
            else
                ui32x >>= 1;
        }
        ui32x ^= 0xffffffff; //xor with 0xffffffff
        return ui32x; //now the output is compatible with windows/winzip/winrar
    }
    
    static int __attribute__ ((section (".ram3text"))) FlashWaitForLastOperation(void) 
    {
        while (FLASH->SR & FLASH_SR_BSY) {
            WWDG->CR = WWDG_CR_T;
        }
        return FLASH->SR;
    }

    static void  __attribute__ ((section (".ram3text"))) FlashEraseSector(int sector) 
    {
        // assume VDD>2.7V
        FLASH->CR &= ~FLASH_CR_PSIZE;
        FLASH->CR |= FLASH_CR_PSIZE_1;
        FLASH->CR &= ~FLASH_CR_SNB;
        FLASH->CR |= FLASH_CR_SER | (sector << 3);
        FLASH->CR |= FLASH_CR_STRT;
        FlashWaitForLastOperation();

        FLASH->CR &= (~FLASH_CR_SER);
        FLASH->CR &= ~FLASH_CR_SER;
        FlashWaitForLastOperation();
    }

    int  __attribute__ ((section (".ram3text"))) FlashProgramWord(uint32_t Address, uint32_t Data) 
    {
        int status;

        FlashWaitForLastOperation();

        /* if the previous operation is completed, proceed to program the new data */
        FLASH->CR &= ~FLASH_CR_PSIZE;
        FLASH->CR |= FLASH_CR_PSIZE_1;
        FLASH->CR |= FLASH_CR_PG;

        *(__IO uint32_t*)Address = Data;

        /* Wait for last operation to be completed */
        status = FlashWaitForLastOperation();

        /* if the program operation is completed, disable the PG Bit */
        FLASH->CR &= (~FLASH_CR_PG);

        /* Return the Program Status */
        return status;
    }

    static void  __attribute__ ((section (".ram3text"))) FlashRamFunction(void)
    {
        halInit();

        /* Float USB inputs, hope the host notices detach... */
        palSetPadMode(GPIOA, 11, PAL_MODE_INPUT);
        palSetPadMode(GPIOA, 12, PAL_MODE_INPUT);

        /* Set up LEDs */
        palSetPadMode(LED1_PORT, LED1_PIN, PAL_MODE_OUTPUT_PUSHPULL);
        palSetPad(LED1_PORT, LED1_PIN);
        palSetPadMode(LED2_PORT, LED2_PIN, PAL_MODE_OUTPUT_PUSHPULL);

    #ifdef SERIALDEBUG
        /* SD2 for serial debug output */
        palSetPadMode(GPIOA, 3, PAL_MODE_ALTERNATE(7) | PAL_MODE_INPUT); // RX
        palSetPadMode(GPIOA, 2, PAL_MODE_OUTPUT_PUSHPULL);               // TX
        palSetPadMode(GPIOA, 2, PAL_MODE_ALTERNATE(7));                  // TX

        /* 115200 baud */
        static const SerialConfig sd2Cfg = {115200, 0, 0, 0};

        sdStart(&SD2, &sd2Cfg);
    #endif /* SERIALDEBUG */

        DBGPRINTCHAR('a');
        uint32_t pbuf[16];
        SDRAM_ReadBuffer(&pbuf[0], 0 + 0x050000, 16);
        DBGPRINTCHAR('x');


        uint32_t *sdram32 = (uint32_t *)SDRAM_BANK_ADDR;
        uint8_t *sdram8 = (uint8_t *)SDRAM_BANK_ADDR;

        if ((sdram8[0] != 'f') || (sdram8[1] != 'l') || (sdram8[2] != 'a') || (sdram8[3] != 's') || (sdram8[4] != 'c') || (sdram8[5] != 'o') || (sdram8[6] != 'p') || (sdram8[7] != 'y'))
        {
            DisplayAbortErr(1);
        }

        DBGPRINTCHAR('b');

        uint32_t flength = sdram32[2];
        uint32_t fcrc = sdram32[3];

        if (flength > 1 * 1024 * 1024)
        {
            DisplayAbortErr(2);
        }

        DBGPRINTCHAR('c');

        DBGPRINTHEX(flength);

        uint32_t ccrc = FlashCalcCRC32((uint8_t *)(SDRAM_BANK_ADDR + 0x010), flength);

        DBGPRINTCHAR('d');

        DBGPRINTHEX(ccrc);
        DBGPRINTHEX(fcrc);

        if (ccrc != fcrc)
        {
            DisplayAbortErr(3);
        }

        DBGPRINTCHAR('e');

        /* Unlock sequence */
        FLASH->KEYR = 0x45670123;
        FLASH->KEYR = 0xCDEF89AB;

        uint32_t i;
        for (i = 0; i < 12; i++)
        {
            FlashEraseSector(i);

            palWritePad(LED2_PORT, LED2_PIN, i%2);

            DBGPRINTCHAR('f');
            DBGPRINTHEX(i);
        }

        DBGPRINTCHAR('g');

        int destptr = FLASH_BASE_ADDR;                            /* flash base adress */
        uint32_t *srcptr = (uint32_t *)(SDRAM_BANK_ADDR + 0x010); /* sdram base adress + header offset */

        bool ledOn = false;
        for (i = 0; i < (flength + 3) / 4; i++)
        {
            uint32_t d = *srcptr;

            FlashProgramWord(destptr, d);

            if ((FLASH->SR != 0) && (FLASH->SR != 1))
            {
                DBGPRINTCHAR('z');
                DBGPRINTHEX(FLASH->SR);
            }

            if ((i & 0xFFF) == 0)
            {
                ledOn = !ledOn;
                palWritePad(LED2_PORT, LED2_PIN, ledOn);

                DBGPRINTCHAR('j');
                DBGPRINTHEX(destptr);
                DBGPRINTHEX(*(srcptr));
            }

            destptr += 4;
            srcptr++;
        }

        palWritePad(LED1_PORT, LED1_PIN, 1);
        palWritePad(LED2_PORT, LED2_PIN, 1);

        DBGPRINTCHAR('k');

        ccrc = FlashCalcCRC32((uint8_t *)(FLASH_BASE_ADDR), flength);

        DBGPRINTCHAR('l');

        DBGPRINTHEX(ccrc);
        DBGPRINTHEX(fcrc);

        if (ccrc != fcrc)
        {
            DisplayAbortErr(5);
        }

        DBGPRINTCHAR('\r');
        DBGPRINTCHAR('\n');
        DBGPRINTCHAR('S');
        DBGPRINTCHAR('U');
        DBGPRINTCHAR('C');
        DBGPRINTCHAR('C');
        DBGPRINTCHAR('E');
        DBGPRINTCHAR('S');
        DBGPRINTCHAR('S');
        DBGPRINTCHAR('\r');
        DBGPRINTCHAR('\n');

        KsolotiSleepMilliseconds(1000);

        FlashSystemReset();
    }

    extern void *_sram3_text;
    extern void *_sram3_start;
    extern void *_sram3_text_start;
    extern void *_sram3_text_end;

    void flasher(void)
    {
    // enable SDRAM
    configSDRAM();

    // copy code to ram
    volatile uint32_t *pSrc = (uint32_t *)&_sram3_start;
    volatile uint32_t *pDst = (uint32_t *)&_sram3_text_start;
    volatile uint32_t *pEnd = (uint32_t *)&_sram3_text_end;
    
    while(pDst < pEnd)
    {
        *pDst = *pSrc;
        pDst++;
        pSrc++;
    }

    // now execute from ram
    FlashRamFunction();
    }
#endif