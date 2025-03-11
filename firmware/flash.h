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

#pragma once

#include <stdint.h>
#include "mcuconf.h"

typedef enum _FlashBank
{
  fbFirmware,
  fbPatch
} FlashBank;

#define FLASH_BASE_ADDR 0x08000000

extern void     FRAMTEXT_CODE_SECTION FlashLock(FlashBank bank);
extern void     FRAMTEXT_CODE_SECTION FlashUnlock(FlashBank bank);
extern int      FRAMTEXT_CODE_SECTION FlashWaitForLastOperation(FlashBank bank); 
extern void     FRAMTEXT_CODE_SECTION FlashEraseSector(FlashBank bank, uint32_t uSector); 
extern bool     FRAMTEXT_CODE_SECTION FlashErasePatch(uint8_t uPatch);
extern bool     FRAMTEXT_CODE_SECTION FlashProgramBlock(FlashBank bank, uint32_t uFlashAddress, uint32_t uDataAddress);
//extern bool     FRAMTEXT_CODE_SECTION FlashProgramBlocks(FlashBank bank, uint32_t uFlashAddress, uint32_t uDataAddress, uint32_t uBlocks);
extern uint32_t FRAMTEXT_CODE_SECTION FlashCalcCRC32(uint8_t *buffer, uint32_t size);
extern uint32_t FRAMTEXT_CODE_SECTION FlashGetBlockWordsize(void);
extern void     FRAMTEXT_CODE_SECTION KsolotiSleepMilliseconds(uint32_t uMiliseconds);
extern void     FRAMTEXT_CODE_SECTION FlashSystemReset(void);
extern bool     FRAMTEXT_CODE_SECTION FlashProgram(FlashBank bank, uint32_t uFlashAddress, uint32_t uDataAddress, uint32_t uBytes);
extern void     FRAMTEXT_CODE_SECTION FlashFirmware(void);


extern bool FlashPatch(uint8_t uPatch);





