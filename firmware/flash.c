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
#include "watchdog.h"

#if BOARD_KSOLOTI_CORE_H743
#include "stm32h7xx_hal.h"
#include "patch.h"

// 8 patches can be store in flashbank 2

void LockFlash(void)
{
  HAL_FLASH_Lock();
  SCB_DisableICache();
}

void UnlockFlash(void)
{
  HAL_FLASH_Unlock();
  SCB_EnableICache();
}

bool FlashErasePatch(uint8_t uPatch)
{
  bool bResult = false;

  if(uPatch < PATCHFLASHSLOTS)
  {
    UnlockFlash();

    FLASH_EraseInitTypeDef EraseInitStruct;
    uint32_t uFlashBlockSize = (128 * 1024);

    uint32_t uSectors = PATCHFLASHSIZE < uFlashBlockSize ? 1 : uFlashBlockSize / PATCHFLASHSIZE ;

    /* Fill EraseInit structure*/
    EraseInitStruct.TypeErase     = FLASH_TYPEERASE_SECTORS;
    EraseInitStruct.VoltageRange  = FLASH_VOLTAGE_RANGE_3;
    EraseInitStruct.Banks         = FLASH_BANK_2;
    EraseInitStruct.Sector        = uPatch;
    EraseInitStruct.NbSectors     = uSectors;

    uint32_t uSectorError = 0;

    if (HAL_FLASHEx_Erase(&EraseInitStruct, &uSectorError) == HAL_OK)
      bResult = uSectorError == 0xffffffff;

    LockFlash();
  }

  return bResult;
}

bool FlashPatch(uint8_t uPatch)
{
  bool bResult = false;

  if(uPatch < PATCHFLASHSLOTS)
  {
    if(FlashErasePatch(uPatch))
    {
      UnlockFlash();

      bResult = true;
      uint32_t uFlashNeeded = (128 * 1024) / PATCHFLASHSLOTS;
      uint32_t uFlashLoc = PATCHFLASHLOC + (uPatch * uFlashNeeded);
      uint32_t uSourceLoc = PATCHMAINLOC;

      uint32_t uBlocks = uFlashNeeded / 32;

      for(uint32_t i = 0; bResult && (i < uBlocks); i++)
      {
        bResult = HAL_OK == HAL_FLASH_Program(FLASH_TYPEPROGRAM_FLASHWORD, uFlashLoc, uSourceLoc);
        uFlashLoc += 32;
        uSourceLoc += 32;
      }

      if(bResult)
      {
        // Validate flash
        uFlashLoc = PATCHFLASHLOC + (uPatch * uFlashNeeded);
        uSourceLoc = PATCHMAINLOC;

        uint32_t *pFlash  = (uint32_t *)uFlashLoc;
        uint32_t *pSource = (uint32_t *)uSourceLoc;

        uint32_t uWords = uFlashNeeded / 4;

        for(uint32_t i = 0; bResult && (i < uWords); i++)
        {
          bResult = *pFlash == *pSource;
          pFlash++;
          pSource++;
        }
      }

      LockFlash();
    }
  }

  return bResult;
}
#else // BOARD_KSOLOTI_CORE_H743
static __attribute__ ((section (".ramtext"))) int flash_WaitForLastOperation(void) {
    while (FLASH->SR & FLASH_SR_BSY) {
        WWDG->CR = WWDG_CR_T;
    }
    return FLASH->SR;
}

static __attribute__ ((section (".ramtext"))) void flash_Erase_sector1(int sector) {
    // assume VDD>2.7V
    FLASH->CR &= ~FLASH_CR_PSIZE;
    FLASH->CR |= FLASH_CR_PSIZE_1;
    FLASH->CR &= ~FLASH_CR_SNB;
    FLASH->CR |= FLASH_CR_SER | (sector << 3);
    FLASH->CR |= FLASH_CR_STRT;
    flash_WaitForLastOperation();

    FLASH->CR &= (~FLASH_CR_SER);
    FLASH->CR &= ~FLASH_CR_SER;
    flash_WaitForLastOperation();
}

int flash_Erase_sector(int sector) {
    /* interrupts would cause flash execution, stall */
    /* and cause watchdog trigger */
    chSysLock();
    flash_Erase_sector1(sector);
    chSysUnlock();
    return 0;
}

int flash_ProgramWord(uint32_t Address, uint32_t Data) {
    int status;

    flash_WaitForLastOperation();

    /* if the previous operation is completed, proceed to program the new data */
    FLASH->CR &= ~FLASH_CR_PSIZE;
    FLASH->CR |= FLASH_CR_PSIZE_1;
    FLASH->CR |= FLASH_CR_PG;

    *(__IO uint32_t*)Address = Data;

    /* Wait for last operation to be completed */
    status = flash_WaitForLastOperation();

    /* if the program operation is completed, disable the PG Bit */
    FLASH->CR &= (~FLASH_CR_PG);

    watchdog_feed();

    /* Return the Program Status */
    return status;
}

void flash_unlock(void) {
    /* Unlock sequence as per reference manual*/
    FLASH->KEYR = 0x45670123;
    FLASH->KEYR = 0xCDEF89AB;
}
#endif // BOARD_KSOLOTI_CORE_H743