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
#include "chprintf.h"
#include "ui.h"
#include "axoloti_board.h"
#include "crc32.h"
#include "sdram.h"
#include "exceptions.h"
#include "flash.h"

extern uint32_t _vectors[0x200]; // Trick compiler into believing us it is 0x200 bytes long.

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

#define FLASH_BASE_ADDR 0x08000000

/* Dummy ui hooks... */
// Btn_Nav_States_struct Btn_Nav_CurStates;
// Btn_Nav_States_struct Btn_Nav_PrevStates;
// Btn_Nav_States_struct Btn_Nav_Or;
// Btn_Nav_States_struct Btn_Nav_And;

// int8_t EncBuffer[4];

void DisplayAbortErr(int err)
{
    DBGPRINTCHAR('0' + err);

    /* blink red slowly, green off */
    palWritePad(LED1_PORT, LED1_PIN, 0);

    int i = 10;
    while (i--)
    {
        palWritePad(LED2_PORT, LED2_PIN, 1);
        chThdSleepMilliseconds(1000);
        palWritePad(LED2_PORT, LED2_PIN, 0);
        chThdSleepMilliseconds(1000);
    }

    NVIC_SystemReset();
}

int main(void)
{
    // small sleep required before watchdog_feed() & halInit();
    chThdSleepMilliseconds(100);

    watchdog_feed();
    halInit();

    /* Float USB inputs, hope the host notices detach... */
    palSetPadMode(GPIOA, 11, PAL_MODE_INPUT);
    palSetPadMode(GPIOA, 12, PAL_MODE_INPUT);

    /* Set up LEDs */
    palSetPadMode(LED1_PORT, LED1_PIN, PAL_MODE_OUTPUT_PUSHPULL);
    palSetPad(LED1_PORT, LED1_PIN);
    palSetPadMode(LED2_PORT, LED2_PIN, PAL_MODE_OUTPUT_PUSHPULL);

    chSysInit();
    watchdog_feed();
    configSDRAM();

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

    watchdog_feed();

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

    uint32_t ccrc = CalcCRC32((uint8_t *)(SDRAM_BANK_ADDR + 0x010), flength);

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
        flash_Erase_sector(i);

        palWritePad(LED2_PORT, LED2_PIN, 1);
        chThdSleepMilliseconds(100);
        palWritePad(LED2_PORT, LED2_PIN, 0);

        DBGPRINTCHAR('f');
        DBGPRINTHEX(i);
    }

    DBGPRINTCHAR('g');

    DBGPRINTHEX(flength);

    ccrc = CalcCRC32((uint8_t *)(SDRAM_BANK_ADDR + 0x010), flength);

    DBGPRINTCHAR('h');

    DBGPRINTHEX(ccrc);
    DBGPRINTHEX(fcrc);

    if (ccrc != fcrc)
    {
        DisplayAbortErr(4);
    }

    DBGPRINTCHAR('i');

    int destptr = FLASH_BASE_ADDR;                            /* flash base adress */
    uint32_t *srcptr = (uint32_t *)(SDRAM_BANK_ADDR + 0x010); /* sdram base adress + header offset */

    for (i = 0; i < (flength + 3) / 4; i++)
    {
        uint32_t d = *srcptr;

        flash_ProgramWord(destptr, d);

        if ((FLASH->SR != 0) && (FLASH->SR != 1))
        {
            DBGPRINTCHAR('z');
            DBGPRINTHEX(FLASH->SR);
        }

        // DBGPRINTHEX(f);

        if ((i & 0xFFF) == 0)
        {
            palWritePad(LED2_PORT, LED2_PIN, 1);
            chThdSleepMilliseconds(100);
            palWritePad(LED2_PORT, LED2_PIN, 0);

            DBGPRINTCHAR('j');
            DBGPRINTHEX(destptr);
            DBGPRINTHEX(*(srcptr));
        }

        destptr += 4;
        srcptr++;
    }

    DBGPRINTCHAR('k');

    ccrc = CalcCRC32((uint8_t *)(FLASH_BASE_ADDR), flength);

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

    chThdSleepMilliseconds(1000);
    NVIC_SystemReset();

    return 0;
}
