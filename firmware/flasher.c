// /*
//  * Copyright (C) 2013, 2014 Johannes Taelman
//  * Edited 2023 - 2025 by Ksoloti
//  *
//  * This file is part of Axoloti.
//  *
//  * Axoloti is free software: you can redistribute it and/or modify it under the
//  * terms of the GNU General Public License as published by the Free Software
//  * Foundation, either version 3 of the License, or (at your option) any later
//  * version.
//  *
//  * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
//  * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
//  * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License along with
//  * Axoloti. If not, see <http://www.gnu.org/licenses/>.
//  */

// #include "hal.h"
// #include "ui.h"
// #include "axoloti_board.h"
// #include "exceptions.h"
// #include "sdram.h"
// #include "flash.h"

// #pragma GCC optimize ("O0")
// static void __attribute__((section(".framtext"))) KsolotiSleepMilliseconds(uint32_t uMiliseconds)
// {
//   uint32_t uTotalTicks = MS2RTT(uMiliseconds);
//   uint32_t uStartTick = DWT->CYCCNT;
//   while (DWT->CYCCNT - uStartTick < uTotalTicks)
//     ;
// }

// //#define SERIALDEBUG

// #ifdef SERIALDEBUG
// #define DBGPRINTCHAR(x) sdPut(&SD2, x)

// void dbgPrintHexDigit(uint8_t b)
// {
//   if (b > 9)
//     DBGPRINTCHAR('a' + b - 10);
//   else
//     DBGPRINTCHAR('0' + b);
// }

// #define DBGPRINTHEX(x)                \
//   DBGPRINTCHAR(' ');                  \
//   DBGPRINTCHAR('0');                  \
//   DBGPRINTCHAR('x');                  \
//   dbgPrintHexDigit((x >> 28) & 0x0f); \
//   dbgPrintHexDigit((x >> 24) & 0x0f); \
//   dbgPrintHexDigit((x >> 20) & 0x0f); \
//   dbgPrintHexDigit((x >> 16) & 0x0f); \
//   dbgPrintHexDigit((x >> 12) & 0x0f); \
//   dbgPrintHexDigit((x >> 8) & 0x0f);  \
//   dbgPrintHexDigit((x >> 4) & 0x0f);  \
//   dbgPrintHexDigit((x) & 0x0f);       \
//   DBGPRINTCHAR('\r');                 \
//   DBGPRINTCHAR('\n');
// #else
// #define DBGPRINTCHAR(x)
// #define DBGPRINTHEX(x)

// #endif /* SERIALDEBUG */


// #define FLASH_BASE_ADDR 0x08000000

// #if BOARD_KSOLOTI_CORE_H743
//   // TODOH7
//   #include "stm32h7xx_hal_flash.h"

//   static void __attribute__((section(".framtext"))) LockFlash(void)
//   {
//     HAL_FLASH_Lock();
//     SCB_DisableICache();
//   }
  
//   static void __attribute__((section(".framtext"))) UnlockFlash(void)
//   {
//     HAL_FLASH_Unlock();
//     SCB_EnableICache();
//   }
  
//   static void __attribute__((section(".framtext"))) EraseFlash(void)
//   {
//     FLASH_EraseInitTypeDef EraseInitStruct;

//     EraseInitStruct.TypeErase     = FLASH_TYPEERASE_SECTORS;
//     EraseInitStruct.VoltageRange  = FLASH_VOLTAGE_RANGE_3;
//     EraseInitStruct.Banks         = FLASH_BANK_2;
//     EraseInitStruct.NbSectors     = 1;

//     uint32_t i;
//     for (i = 0; i < 8; i++)
//     {
//       EraseInitStruct.Sector = i;

//       uint32_t uSectorError = 0;
//       HAL_FLASHEx_Erase(&EraseInitStruct, &uSectorError); // TODOH& replace all these HAL calls
  
//       palWritePad(LED2_PORT, LED2_PIN, i % 2);

//       DBGPRINTCHAR('f');
//       DBGPRINTHEX(i);
//     }
//   }

//   static void __attribute__((section(".framtext"))) FlashProgramWord(uint32_t Address, uint32_t Data)
//   {
//     HAL_FLASH_Program(FLASH_TYPEPROGRAM_FLASHWORD, Address, Data);
//   }

// #else

//   static int __attribute__((section(".framtext"))) FlashWaitForLastOperation(void)
//   {
//     while (FLASH->SR & FLASH_SR_BSY)
//     {
//       WWDG->CR = WWDG_CR_T;
//     }
//     return FLASH->SR;
//   }

//   static void __attribute__((section(".framtext"))) FlashEraseSector(int sector)
//   {
//     // assume VDD>2.7V
//     FLASH->CR &= ~FLASH_CR_PSIZE;
//     FLASH->CR |= FLASH_CR_PSIZE_1;
//     FLASH->CR &= ~FLASH_CR_SNB;
//     FLASH->CR |= FLASH_CR_SER | (sector << 3);
//     FLASH->CR |= FLASH_CR_STRT;
//     FlashWaitForLastOperation();

//     FLASH->CR &= (~FLASH_CR_SER);
//     FLASH->CR &= ~FLASH_CR_SER;
//     FlashWaitForLastOperation();
//   }


//   int __attribute__((section(".framtext"))) FlashProgramWord(uint32_t Address, uint32_t Data)
//   {
//     int status;

//     FlashWaitForLastOperation();

//     /* if the previous operation is completed, proceed to program the new data */
//     FLASH->CR &= ~FLASH_CR_PSIZE;
//     FLASH->CR |= FLASH_CR_PSIZE_1;
//     FLASH->CR |= FLASH_CR_PG;

//     *(__IO uint32_t *)Address = Data;

//     /* Wait for last operation to be completed */
//     status = FlashWaitForLastOperation();

//     /* if the program operation is completed, disable the PG Bit */
//     FLASH->CR &= (~FLASH_CR_PG);

//     /* Return the Program Status */
//     return status;
//   }


//   void __attribute__((section(".framtext"))) UnlockFlash(void)
//   {
//     FLASH->KEYR = 0x45670123;
//     FLASH->KEYR = 0xCDEF89AB;
//   }

//   void __attribute__((section(".framtext"))) LockFlash(void)
//   {
//     FLASH->CR |= FLASH_CR_LOCK;
//   }

//   void __attribute__((section(".framtext"))) EraseFlash(void)
//   {
//     uint32_t i;
//     for (i = 0; i < 12; i++)
//     {
//       FlashEraseSector(i);

//       palWritePad(LED2_PORT, LED2_PIN, i % 2);

//       DBGPRINTCHAR('f');
//       DBGPRINTHEX(i);
//     }
//   }

// #endif

// // From here code for both F4 and H7

// void __attribute__((section(".framtext"))) DisplayAbortErr(int err)
// {
//   DBGPRINTCHAR('0' + err);

//   /* blink red slowly, green off */
//   palWritePad(LED1_PORT, LED1_PIN, 0);

//   int i = 10;
//   while (i--)
//   {
//     palWritePad(LED2_PORT, LED2_PIN, 1);
//     KsolotiSleepMilliseconds(1000);
//     palWritePad(LED2_PORT, LED2_PIN, 0);
//     KsolotiSleepMilliseconds(1000);
//   }

//   FlashSystemReset();
// }




// static void __attribute__((section(".framtext"))) FlashRamFunction(void)
// {
//   halInit();

//   /* Float USB inputs, hope the host notices detach... */
//   palSetPadMode(GPIOA, 11, PAL_MODE_INPUT);
//   palSetPadMode(GPIOA, 12, PAL_MODE_INPUT);

//   /* Set up LEDs */
//   palSetPadMode(LED1_PORT, LED1_PIN, PAL_MODE_OUTPUT_PUSHPULL);
//   palSetPad(LED1_PORT, LED1_PIN);
//   palSetPadMode(LED2_PORT, LED2_PIN, PAL_MODE_OUTPUT_PUSHPULL);

// #ifdef SERIALDEBUG
//   /* SD2 for serial debug output */
//   palSetPadMode(GPIOA, 3, PAL_MODE_ALTERNATE(7) | PAL_MODE_INPUT); // RX
//   palSetPadMode(GPIOA, 2, PAL_MODE_OUTPUT_PUSHPULL);               // TX
//   palSetPadMode(GPIOA, 2, PAL_MODE_ALTERNATE(7));                  // TX

//   /* 115200 baud */
//   static const SerialConfig sd2Cfg = {115200, 0, 0, 0};

//   sdStart(&SD2, &sd2Cfg);
// #endif /* SERIALDEBUG */

//   DBGPRINTCHAR('a');
//   uint32_t pbuf[16];
//   SDRAM_ReadBuffer(&pbuf[0], 0 + 0x050000, 16);
//   DBGPRINTCHAR('x');

//   uint32_t *sdram32 = (uint32_t *)SDRAM_BANK_ADDR;
//   uint8_t *sdram8 = (uint8_t *)SDRAM_BANK_ADDR;

//   if ((sdram8[0] != 'f') || (sdram8[1] != 'l') || (sdram8[2] != 'a') || (sdram8[3] != 's') || (sdram8[4] != 'c') || (sdram8[5] != 'o') || (sdram8[6] != 'p') || (sdram8[7] != 'y'))
//   {
//     DisplayAbortErr(1);
//   }

//   DBGPRINTCHAR('b');

//   uint32_t flength = sdram32[2];
//   uint32_t fcrc = sdram32[3];

//   if (flength > 1 * 1024 * 1024)
//   {
//     DisplayAbortErr(2);
//   }

//   DBGPRINTCHAR('c');

//   DBGPRINTHEX(flength);

//   uint32_t ccrc = FlashCalcCRC32((uint8_t *)(SDRAM_BANK_ADDR + 0x010), flength);

//   DBGPRINTCHAR('d');

//   DBGPRINTHEX(ccrc);
//   DBGPRINTHEX(fcrc);

//   if (ccrc != fcrc)
//   {
//     DisplayAbortErr(3);
//   }

//   DBGPRINTCHAR('e');

//   /* Unlock sequence */
//   UnlockFlash();

//   // Erase flash
//   EraseFlash();

//   DBGPRINTCHAR('g');

//   int destptr = FLASH_BASE_ADDR;                            /* flash base adress */
//   uint32_t *srcptr = (uint32_t *)(SDRAM_BANK_ADDR + 0x010); /* sdram base adress + header offset */

//   bool ledOn = false;
//   uint32_t i;
//   for (i = 0; i < (flength + 3) / 4; i++)
//   {
//     uint32_t d = *srcptr;

//     FlashProgramWord(destptr, d);

// #if BOARD_KSOLOTI_CORE_H743
//   if ((FLASH->SR1 != 0) && (FLASH->SR1 != 1))
//   {
//     DBGPRINTCHAR('z');
//     DBGPRINTHEX(FLASH->SR1);
//   }
// #else    
//     if ((FLASH->SR != 0) && (FLASH->SR != 1))
//     {
//       DBGPRINTCHAR('z');
//       DBGPRINTHEX(FLASH->SR);
//     }
// #endif

//     if ((i & 0xFFF) == 0)
//     {
//       ledOn = !ledOn;
//       palWritePad(LED2_PORT, LED2_PIN, ledOn);

//       DBGPRINTCHAR('j');
//       DBGPRINTHEX(destptr);
//       DBGPRINTHEX(*(srcptr));
//     }

//     destptr += 4;
//     srcptr++;
//   }

//   palWritePad(LED1_PORT, LED1_PIN, 1);
//   palWritePad(LED2_PORT, LED2_PIN, 1);

//   DBGPRINTCHAR('k');

//   ccrc = FlashCalcCRC32((uint8_t *)(FLASH_BASE_ADDR), flength);

//   DBGPRINTCHAR('l');

//   DBGPRINTHEX(ccrc);
//   DBGPRINTHEX(fcrc);

//   if (ccrc != fcrc)
//   {
//     DisplayAbortErr(5);
//   }

//   DBGPRINTCHAR('\r');
//   DBGPRINTCHAR('\n');
//   DBGPRINTCHAR('S');
//   DBGPRINTCHAR('U');
//   DBGPRINTCHAR('C');
//   DBGPRINTCHAR('C');
//   DBGPRINTCHAR('E');
//   DBGPRINTCHAR('S');
//   DBGPRINTCHAR('S');
//   DBGPRINTCHAR('\r');
//   DBGPRINTCHAR('\n');

//   LockFlash();

//   KsolotiSleepMilliseconds(1000);

//   FlashSystemReset();
// }


// // extern void *_fram_text;
// // extern void *_fram_text_start_flash;
// // extern void *_fram_text_start;
// // extern void *_fram_text_end;

// void flasher(void)
// {
//   // enable SDRAM
//   configSDRAM();

//   halInit();

//   // now execute from ram
//   FlashFirmware();
// }