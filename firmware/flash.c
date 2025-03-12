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
#include "watchdog.h"
#include "flash.h"
#include "patch.h"
#include "sdram.h"

#define GREEN_LED(x)   palWritePad(LED1_PORT, LED1_PIN, x);
#define RED_LED(x)     palWritePad(LED2_PORT, LED2_PIN, x);
#define RED_LED_TOGGLE palTogglePad(LED2_PORT, LED2_PIN);

static uint32_t FRAMTEXT_CODE_SECTION revbit(uint32_t data)
{
  uint32_t result;
  __ASM
  volatile("rbit %0, %1" : "=r"(result) : "r"(data));
  return result;
}

uint32_t FRAMTEXT_CODE_SECTION FlashCalcCRC32(uint8_t *buffer, uint32_t size)
{
  uint32_t i, j;
  uint32_t ui32x;

#if BOARD_KSOLOTI_CORE_H743
  RCC->AHB4ENR |= RCC_AHB4ENR_CRCEN;
#else
  RCC->AHB1ENR |= RCC_AHB1ENR_CRCEN;
#endif

  CRC->CR = 1;
  asm("NOP");
  asm("NOP");
  asm("NOP");
  // delay for hardware ready

  i = size >> 2;

  while (i--)
  {
    ui32x = *((uint32_t *)buffer);
    buffer += 4;
    ui32x = revbit(ui32x); // reverse the bit order of input data
    CRC->DR = ui32x;  
  }
  ui32x = CRC->DR;

  ui32x = revbit(ui32x); // reverse the bit order of output data
  i = size & 3;
  while (i--)
  {
    ui32x ^= (uint32_t)*buffer++;

    for (j = 0; j < 8; j++)
      if (ui32x & 1)
        ui32x = (ui32x >> 1) ^ 0xEDB88320;
      else
        ui32x >>= 1;
  }
  ui32x ^= 0xffffffff; // xor with 0xffffffff
  return ui32x;        // now the output is compatible with windows/winzip/winrar
}

void FRAMTEXT_CODE_SECTION KsolotiSleepMilliseconds(uint32_t uMiliseconds)
{
  uint32_t uTotalTicks = MS2RTT(uMiliseconds);
  uint32_t uStartTick = DWT->CYCCNT;
  while (DWT->CYCCNT - uStartTick < uTotalTicks)
    ;
}


void FRAMTEXT_CODE_SECTION FlashSystemReset(void)
{
  __DSB(); /* Ensure all outstanding memory accesses included
             buffered write are completed before reset */
  SCB->AIRCR = (uint32_t)((0x5FAUL << SCB_AIRCR_VECTKEY_Pos) |
                          (SCB->AIRCR & SCB_AIRCR_PRIGROUP_Msk) |
                          SCB_AIRCR_SYSRESETREQ_Msk); /* Keep priority group unchanged */
  __DSB();                                            /* Ensure completion of memory access */

  for (;;) /* wait until reset */
  {
    __NOP();
  }
}

bool FRAMTEXT_CODE_SECTION FlashProgram(FlashBank bank, uint32_t uFlashAddress, uint32_t uDataAddress, uint32_t uBytes)
{
  /* Set up LEDs */
  RED_LED(1);

  FlashUnlock(bank);

  bool bResult = true;

  uint32_t uFlashLoc = uFlashAddress;
  uint32_t uSourceLoc = uDataAddress;
  uint32_t uBlockByteSize = FlashGetBlockBytesize();
  uint32_t uBlocks = uBytes / uBlockByteSize;
  if(uBytes % uBlockByteSize)
    uBlocks++;
  
  int32_t nLedWordCount = 1024*16;

  for (uint32_t uBlock = 0; bResult && (uBlock < uBlocks); uBlock++)
  {
    bResult = FlashProgramBlock(bank, uFlashLoc, uSourceLoc);
    uFlashLoc += uBlockByteSize;
    uSourceLoc += uBlockByteSize;
    nLedWordCount -= uBlockByteSize;

    if( nLedWordCount < 0)
    {
      nLedWordCount = 1024*16;
      RED_LED_TOGGLE;
    }
  }

  if (bResult)
  {
    // Validate flash
    uint32_t uFlashLoc = uFlashAddress;
    uint32_t uSourceLoc = uDataAddress;

    uint32_t *pFlash = (uint32_t *)uFlashLoc;
    uint32_t *pSource = (uint32_t *)uSourceLoc;

    uint32_t uWords = (uBlocks * uBlockByteSize) / 4;

    for (uint32_t i = 0; bResult && (i < uWords); i++)
    {
      bResult = *pFlash == *pSource;
      pFlash++;
      pSource++;
    }
  }

  FlashLock(bank);

  RED_LED(0);

  return bResult;
}

void __attribute__((section(".framtext"))) DisplayAbortErr(int err)
{
  /* blink red slowly, green off */
  palWritePad(LED1_PORT, LED1_PIN, 0);

  int i = 20;
  while (i--)
  {
    RED_LED_TOGGLE;
    palWritePad(LED2_PORT, LED2_PIN, 1);
  }

  FlashSystemReset();
}

void FRAMTEXT_CODE_SECTION FlashFirmware(void)
{
  halInit();

#if BOARD_KSOLOTI_CORE_H743
  FLASH->SR1 = 0xffffffff;
#else  
  FLASH->SR = 0xffffffff;
#endif

  // Set LEDs on
  RED_LED(1);


  uint32_t *sdram32 = (uint32_t *)SDRAM_BANK_ADDR;
  uint8_t *sdram8 = (uint8_t *)SDRAM_BANK_ADDR;

  if ((sdram8[0] != 'f') || (sdram8[1] != 'l') || (sdram8[2] != 'a') || (sdram8[3] != 's') || (sdram8[4] != 'c') || (sdram8[5] != 'o') || (sdram8[6] != 'p') || (sdram8[7] != 'y'))
  {
    DisplayAbortErr(1);
  }

  uint32_t flength = sdram32[2];
  uint32_t fcrc = sdram32[3];

  if (flength > 1 * 1024 * 1024)
  {
    DisplayAbortErr(2);
  }

  uint32_t ccrc = FlashCalcCRC32((uint8_t *)(SDRAM_BANK_ADDR + 0x010), flength);

  if (ccrc != fcrc)
  {
    DisplayAbortErr(3);
  }

  
 
  // Erase flash
#if BOARD_KSOLOTI_CORE_H743
  uint32_t uFlashSectors = 8;
#else
  uint32_t uFlashSectors = 12;
#endif 


  FlashUnlock(fbFirmware);
  uint32_t uSector;
  bool bResult = true;
  for (uSector = 0; bResult && (uSector < uFlashSectors); uSector++)
  {
    bResult = FlashEraseSector(fbFirmware, uSector);
    RED_LED_TOGGLE;
  }
  FlashLock(fbFirmware);

  if (!bResult)
    DisplayAbortErr(4);

  uint32_t uDest = FLASH_BASE_ADDR;
  uint32_t uSrc  = SDRAM_BANK_ADDR + 0x010;

  if (!bResult)
    DisplayAbortErr(4);

  bResult = FlashProgram(fbFirmware, uDest, uSrc, flength);

  FlashLock(fbFirmware);

  if (!bResult)
    DisplayAbortErr(5);

  RED_LED(1);


  ccrc = FlashCalcCRC32((uint8_t *)(FLASH_BASE_ADDR), flength);

  if (ccrc != fcrc)
  {
    DisplayAbortErr(6);
  }

 
  KsolotiSleepMilliseconds(1000);

  FlashSystemReset();
}
