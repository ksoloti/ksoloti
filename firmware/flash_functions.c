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
#include "flash.h"
#include "patch.h"

#if defined(BOARD_KSOLOTI_GEKO)
#include "stm32h7xx_hal.h"
#endif


#if defined(BOARD_KSOLOTI_CORE) || defined(BOARD_AXOLOTI_CORE)

volatile int SR;

bool FRAMTEXT_CODE_SECTION FlashWaitForLastOperation(__attribute__((unused)) FlashBank bank) {
    while (FLASH->SR & FLASH_SR_BSY) {
        WWDG->CR = WWDG_CR_T;
    }
    SR = FLASH->SR;
    return !(FLASH->SR);
}

void FRAMTEXT_CODE_SECTION FlashLock(__attribute__((unused)) FlashBank bank) {
    SET_BIT(FLASH->CR, FLASH_CR_LOCK);
}

void FRAMTEXT_CODE_SECTION FlashUnlock(__attribute__((unused)) FlashBank bank) {
    /* Unlock sequence as per reference manual*/
    FLASH->KEYR = 0x45670123;
    FLASH->KEYR = 0xCDEF89AB;
}

bool FRAMTEXT_CODE_SECTION FlashEraseSector(FlashBank bank, uint32_t uSector) {
    /* assume VDD>2.7V */
    FLASH->CR &= ~FLASH_CR_PSIZE;
    FLASH->CR |= FLASH_CR_PSIZE_1;
    FLASH->CR &= ~FLASH_CR_SNB;
    FLASH->CR |= FLASH_CR_SER | (uSector << 3);
    FLASH->CR |= FLASH_CR_STRT;
    FlashWaitForLastOperation(bank);

    FLASH->CR &= (~FLASH_CR_SER);
    FLASH->CR &= ~FLASH_CR_SER;
    bool bResult = FlashWaitForLastOperation(bank);

    return bResult;
}

bool FRAMTEXT_CODE_SECTION FlashProgramBlock(FlashBank bank, uint32_t uFlashAddress, uint32_t uDataAddress) {
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

    /* Return the Program Status */
    return bResult;
}

bool FRAMTEXT_CODE_SECTION FlashErasePatch(__attribute__((unused)) uint8_t uPatch) {
    FlashUnlock(fbPatch);
    bool bResult = FlashEraseSector(fbPatch, 11);
    FlashLock(fbPatch);
    return bResult;
}

bool FlashPatch(uint8_t uPatch) {
    bool bResult = false;

    if (FlashErasePatch(uPatch)) {
        uint32_t uFlashLoc = GetPatchFlashLoc();
        uint32_t uSourceLoc = GetPatchMainLoc();

        bResult = FlashProgram(fbPatch, uFlashLoc, uSourceLoc, GetPatchFlashSize());
    }

    return bResult;
}

uint32_t FRAMTEXT_CODE_SECTION FlashGetBlockBytesize(void) {
    return 4;
}


#elif defined(BOARD_KSOLOTI_GEKO)

#define FLASH_KEY1 0x45670123U
#define FLASH_KEY2 0xCDEF89ABU

#define FLASH_GET_FLAG_BANK1(__FLAG__) (READ_BIT(FLASH->SR1, (__FLAG__)) == (__FLAG__))
#define FLASH_GET_FLAG_BANK2(__FLAG__) (READ_BIT(FLASH->SR2, ((__FLAG__) & 0x7FFFFFFFU)) == (((__FLAG__) & 0x7FFFFFFFU)))

#define FLASH_CLEAR_FLAG_BANK1(__FLAG__) WRITE_REG(FLASH->CCR1, (__FLAG__))
#define FLASH_CLEAR_FLAG_BANK2(__FLAG__) WRITE_REG(FLASH->CCR2, ((__FLAG__) & 0x7FFFFFFFU))

void FRAMTEXT_CODE_SECTION FlashLock(FlashBank bank) {
    if (bank == fbFirmware) {
        SET_BIT(FLASH->CR1, FLASH_CR_LOCK);
    }
    else {
        SET_BIT(FLASH->CR2, FLASH_CR_LOCK);
    }
    SCB_DisableICache();
}

void FRAMTEXT_CODE_SECTION FlashUnlock(FlashBank bank) {
    if (bank == fbFirmware) {
        WRITE_REG(FLASH->KEYR1, FLASH_KEY1);
        WRITE_REG(FLASH->KEYR1, FLASH_KEY2);
    }
    else {
        WRITE_REG(FLASH->KEYR2, FLASH_KEY1);
        WRITE_REG(FLASH->KEYR2, FLASH_KEY2);
    }

    SCB_EnableICache();
}

bool FRAMTEXT_CODE_SECTION FlashWaitForLastOperation(FlashBank bank) {
    if (bank == fbFirmware) {
        while (FLASH_GET_FLAG_BANK1(FLASH_FLAG_QW_BANK1))
            ;

        if (FLASH_GET_FLAG_BANK1(FLASH_FLAG_EOP_BANK1)) {
            /* Clear FLASH End of Operation pending bit */
            __HAL_FLASH_CLEAR_FLAG_BANK1(FLASH_FLAG_EOP_BANK1);
        }

        return !(FLASH->SR1);
    }
    else {
        while (FLASH_GET_FLAG_BANK2(FLASH_FLAG_QW_BANK2))
            ;

        if (FLASH_GET_FLAG_BANK2(FLASH_FLAG_EOP_BANK2)) {
            /* Clear FLASH End of Operation pending bit */
            __HAL_FLASH_CLEAR_FLAG_BANK2(FLASH_FLAG_EOP_BANK2);
        }

        return !(FLASH->SR2);
    }
}

bool FRAMTEXT_CODE_SECTION FlashEraseSector(FlashBank bank, uint32_t uSector) {
    FlashWaitForLastOperation(bank);

    if (bank == fbFirmware) {
        FLASH->CR1 &= ~(FLASH_CR_PSIZE | FLASH_CR_SNB);
        FLASH->CR1 |= (FLASH_CR_SER | FLASH_VOLTAGE_RANGE_3 | (uSector << FLASH_CR_SNB_Pos) | FLASH_CR_START);
    }
    else {
        FLASH->CR2 &= ~(FLASH_CR_PSIZE | FLASH_CR_SNB);
        FLASH->CR2 |= (FLASH_CR_SER | FLASH_VOLTAGE_RANGE_3 | (uSector << FLASH_CR_SNB_Pos) | FLASH_CR_START);
    }

    bool bResult = FlashWaitForLastOperation(bank);

    return bResult;
}

bool FRAMTEXT_CODE_SECTION FlashErasePatch(uint8_t uPatch) {
    bool bResult = false;

    if (uPatch < GetPatchFlashSlots()) {
        bResult = true;

        FlashUnlock(fbPatch);

        uint32_t uFlashBlockSize = (128 * 1024);
        uint32_t uSectors = GetPatchFlashSize() < uFlashBlockSize ? 1 : GetPatchFlashSize() /uFlashBlockSize;

        for (uint32_t i = 0; bResult && (i < uSectors); i++)
            bResult = FlashEraseSector(fbPatch, uPatch + i);

        FlashLock(fbPatch);
    }

    return bResult;
}

bool FRAMTEXT_CODE_SECTION FlashProgramBlock(FlashBank bank, uint32_t uFlashAddress, uint32_t uDataAddress) {
    __IO uint32_t *pDest = (__IO uint32_t *)uFlashAddress;
    __IO uint32_t *pSrc = (__IO uint32_t *)uDataAddress;

    uint8_t row_index = FLASH_NB_32BITWORD_IN_FLASHWORD;

    if (bank == fbFirmware)
        SET_BIT(FLASH->CR1, FLASH_CR_PG);
    else
        SET_BIT(FLASH->CR2, FLASH_CR_PG);

    __ISB();
    __DSB();

    do {
        *pDest = *pSrc;
        pDest++;
        pSrc++;
        row_index--;
    } while (row_index != 0U);

    __ISB();
    __DSB();

    bool bResult = FlashWaitForLastOperation(bank);

    if (bank == fbFirmware)
        CLEAR_BIT(FLASH->CR1, FLASH_CR_PG);
    else
        CLEAR_BIT(FLASH->CR2, FLASH_CR_PG);

    return bResult;
}

uint32_t FRAMTEXT_CODE_SECTION FlashGetBlockBytesize(void) {
    return 32;
}

bool FlashPatch(uint8_t uPatch) {
    bool bResult = FALSE;

    if (uPatch < GetPatchFlashSlots()) {
        if (FlashErasePatch(uPatch)) {
            uint32_t uFlashNeeded = (128 * 1024) / GetPatchFlashSlots();
            uint32_t uFlashLoc = GetPatchFlashLoc() + (uPatch * uFlashNeeded);
            uint32_t uSourceLoc = GetPatchMainLoc();

            bResult = FlashProgram(fbPatch, uFlashLoc, uSourceLoc, uFlashNeeded);
        }
    }

    return bResult;
}

#endif