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


int FRAMTEXT_CODE_SECTION FlashWaitForLastOperation(__attribute__ ((unused)) FlashBank bank)
{
  while (FLASH->SR & FLASH_SR_BSY)
  {
    WWDG->CR = WWDG_CR_T;
  }
  return FLASH->SR;
}

void FRAMTEXT_CODE_SECTION FlashLock(__attribute__ ((unused)) FlashBank bank)
{
  SET_BIT(FLASH->CR, FLASH_CR_LOCK);
}

void FRAMTEXT_CODE_SECTION FlashUnlock(__attribute__ ((unused)) FlashBank bank)
{
  /* Unlock sequence as per reference manual*/
  FLASH->KEYR = 0x45670123;
  FLASH->KEYR = 0xCDEF89AB;
}

void FRAMTEXT_CODE_SECTION FlashEraseSector(FlashBank bank, uint32_t uSector)
{
  /* interrupts would cause flash execution, stall */
  /* and cause watchdog trigger */
  chSysLock();

  // assume VDD>2.7V
  FLASH->CR &= ~FLASH_CR_PSIZE;
  FLASH->CR |= FLASH_CR_PSIZE_1;
  FLASH->CR &= ~FLASH_CR_SNB;
  FLASH->CR |= FLASH_CR_SER | (uSector << 3);
  FLASH->CR |= FLASH_CR_STRT;
  FlashWaitForLastOperation(bank);

  FLASH->CR &= (~FLASH_CR_SER);
  FLASH->CR &= ~FLASH_CR_SER;
  FlashWaitForLastOperation(bank);

  chSysUnlock();
}

bool FRAMTEXT_CODE_SECTION FlashProgramBlock(FlashBank bank, uint32_t uFlashAddress, uint32_t uDataAddress)
{
  FlashWaitForLastOperation(bank);

  /* if the previous operation is completed, proceed to program the new data */
  FLASH->CR &= ~FLASH_CR_PSIZE;
  FLASH->CR |= FLASH_CR_PSIZE_1;
  FLASH->CR |= FLASH_CR_PG;

  __IO uint32_t *pDest = (__IO uint32_t *)uFlashAddress;
  __IO uint32_t *pSrc = (__IO uint32_t *)uDataAddress;

  *pDest = *pSrc;

  /* Wait for last operation to be completed */
  bool bResult = FlashWaitForLastOperation(bank);

  /* if the program operation is completed, disable the PG Bit */
  FLASH->CR &= (~FLASH_CR_PG);

  watchdog_feed();

  /* Return the Program Status */
  return bResult;
}

// bool FRAMTEXT_CODE_SECTION FlashProgramBlocks(FlashBank bank, uint32_t uFlashAddress, uint32_t uDataAddress, uint32_t uBlocks)
// {
//   FlashUnlock(bank);

//   bool bResult = true;

//   uint32_t uFlashLoc = uFlashAddress;
//   uint32_t uSourceLoc = uDataAddress;
  
//   for (uint32_t uBlock = 0; bResult && (uBlock < uBlocks); uBlock++)
//   {
//     bResult = FlashProgramBlock(fbPatch, uFlashLoc, uSourceLoc);
//     uFlashLoc += 1;
//     uSourceLoc += 1;
//   }

//   if (bResult)
//   {
//     // Validate flash
//     uint32_t uFlashLoc = uFlashAddress;
//     uint32_t uSourceLoc = uDataAddress;

//     uint32_t *pFlash = (uint32_t *)uFlashLoc;
//     uint32_t *pSource = (uint32_t *)uSourceLoc;

//     uint32_t uWords = uBlocks * 8;

//     for (uint32_t i = 0; bResult && (i < uWords); i++)
//     {
//       bResult = *pFlash == *pSource;
//       pFlash++;
//       pSource++;
//     }
//   }

//   FlashLock(bank);

//   return bResult;
// }


bool FRAMTEXT_CODE_SECTION FlashErasePatch(__attribute__ ((unused)) uint8_t uPatch)
{
  FlashUnlock(fbPatch);
  FlashEraseSector(fbPatch, 11);
  FlashLock(fbPatch);
  return true;
}

bool FlashPatch(uint8_t uPatch)
{
  bool bResult = false;

  if (FlashErasePatch(uPatch))
  {
    uint32_t uFlashLoc = PATCHFLASHLOC;
    uint32_t uSourceLoc = PATCHMAINLOC;

    bResult = FlashProgram(fbPatch, uFlashLoc, uSourceLoc, PATCHFLASHSIZE);
  }

  return bResult;
}

uint32_t FRAMTEXT_CODE_SECTION FlashGetBlockWordsize(void)
{
  return 1;
}