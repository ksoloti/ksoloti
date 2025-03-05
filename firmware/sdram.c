/*
 * Copyright (C) 2015 Johannes Taelman
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

/**
 *  Adapted from:
 ******************************************************************************
 * @file    stm32f429i_discovery_sdram.c
 * @author  MCD Application Team
 * @version V1.0.1
 * @date    28-October-2013
 * @brief   This file provides a set of functions needed to drive the
 * IS42S16400J SDRAM memory mounted on STM32F429I-DISCO Kit.
 ******************************************************************************
 * @attention
 *
 * <h2><center>&copy; COPYRIGHT 2013 STMicroelectronics</center></h2>
 *
 * Licensed under MCD-ST Liberty SW License Agreement V2, (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.st.com/software_license_agreement_liberty_v2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************
 */

#include "sdram.h"

#if BOARD_KSOLOTI_CORE_H743
  #include "stm32h7xx_ll_fmc.h"
  #include "stm32h7xx_hal_cortex.h"
#else
  #include "stm32f4xx_fmc.h"
#endif

#include "ch.h"
#include "hal.h"
#include "axoloti_board.h"
#include "sysmon.h"
/**
 * @brief  Configures the FMC and GPIOs to interface with the SDRAM memory.
 *         This function must be called before any read/write operation
 *         on the SDRAM.
 * @param  None
 * @retval None
 */
#if BOARD_KSOLOTI_CORE_H743
  void HAL_MPU_ConfigRegion(const MPU_Region_InitTypeDef *MPU_Init)
  {
    /* Set the Region number */
    MPU->RNR = MPU_Init->Number;

    /* Disable the Region */
    CLEAR_BIT(MPU->RASR, MPU_RASR_ENABLE_Msk);

    /* Apply configuration */
    MPU->RBAR = MPU_Init->BaseAddress;
    MPU->RASR = ((uint32_t)MPU_Init->DisableExec             << MPU_RASR_XN_Pos)   |
                ((uint32_t)MPU_Init->AccessPermission        << MPU_RASR_AP_Pos)   |
                ((uint32_t)MPU_Init->TypeExtField            << MPU_RASR_TEX_Pos)  |
                ((uint32_t)MPU_Init->IsShareable             << MPU_RASR_S_Pos)    |
                ((uint32_t)MPU_Init->IsCacheable             << MPU_RASR_C_Pos)    |
                ((uint32_t)MPU_Init->IsBufferable            << MPU_RASR_B_Pos)    |
                ((uint32_t)MPU_Init->SubRegionDisable        << MPU_RASR_SRD_Pos)  |
                ((uint32_t)MPU_Init->Size                    << MPU_RASR_SIZE_Pos) |
                ((uint32_t)MPU_Init->Enable                  << MPU_RASR_ENABLE_Pos);
  }

  static FMC_SDRAM_TypeDef *pSdramInstance = (FMC_SDRAM_TypeDef *)FMC_SDRAM_DEVICE;

  void SDRAM_Init(void) 
  {
    MPU_Region_InitTypeDef MPU_InitStruct;

    /* Configure the MPU attributes as WB for SDRAM */
    MPU_InitStruct.Enable = MPU_REGION_ENABLE;
    MPU_InitStruct.BaseAddress = SDRAM_BANK_ADDR;
    MPU_InitStruct.Size = MPU_REGION_SIZE_32MB;
    MPU_InitStruct.AccessPermission = MPU_REGION_FULL_ACCESS;
    MPU_InitStruct.IsBufferable = MPU_ACCESS_BUFFERABLE;
    MPU_InitStruct.IsCacheable = MPU_ACCESS_CACHEABLE;
    MPU_InitStruct.IsShareable = MPU_ACCESS_NOT_SHAREABLE;
    MPU_InitStruct.Number = MPU_REGION_NUMBER1;
    MPU_InitStruct.TypeExtField = MPU_TEX_LEVEL0;
    MPU_InitStruct.SubRegionDisable = 0x00;
    MPU_InitStruct.DisableExec = MPU_INSTRUCTION_ACCESS_ENABLE;
  
    HAL_MPU_ConfigRegion(&MPU_InitStruct);

    FMC_SDRAM_InitTypeDef FMC_SDRAMInitStructure;
    FMC_SDRAM_TimingTypeDef FMC_SDRAMTimingInitStructure;

    /* Enable FMC clock */
    rccEnableAHB3(RCC_AHB3ENR_FMCEN, FALSE);

    // seb tweaked below values a bit faster for Micron MT48LC16M16A2P
    /* FMC Configuration ---------------------------------------------------------*/
    /* FMC SDRAM Bank configuration */
    /* Timing configuration for 84 Mhz of SD clock frequency (168Mhz/2) */ // TODOH7 calculate timings
    /* TMRD: 2 Clock cycles */ // seb 2 clock cycles, so seems good
    FMC_SDRAMTimingInitStructure.LoadToActiveDelay = 2;
    /* TXSR: min=70ns (6x11.90ns) */ // seb min 67 ns
    FMC_SDRAMTimingInitStructure.ExitSelfRefreshDelay = 7;
    /* TRAS: min=42ns (4x11.90ns) max=120k (ns) */ // seb same
    FMC_SDRAMTimingInitStructure.SelfRefreshTime = 4;
    /* TRC:  min=63 (6x11.90ns) */ // seb min 60 ns; set to 6 * 11.11ns = 66.66ns
    FMC_SDRAMTimingInitStructure.RowCycleDelay = 7; // seb reverted to original 7 due to Axoloti compatibility "backport"
    /* TWR:  2 Clock cycles */ // seb 1 clock cycle + 6 ns, or 12 ns. TWR has to be >= TRAS-TRCD => 4-2 = 2
    FMC_SDRAMTimingInitStructure.WriteRecoveryTime = 2;
    /* TRP:  15ns => 2x11.90ns */ // seb 18 ns
    FMC_SDRAMTimingInitStructure.RPDelay = 2;
    /* TRCD: 15ns => 2x11.90ns */ // seb 18 ns
    FMC_SDRAMTimingInitStructure.RCDDelay = 2;

    /* FMC SDRAM control configuration */
    FMC_SDRAMInitStructure.SDBank = FMC_SDRAM_BANK1;

    /* Row addressing: [8:0] */
    FMC_SDRAMInitStructure.ColumnBitsNumber = FMC_SDRAM_COLUMN_BITS_NUM_9;

    /* Column addressing: [12:0] */
    FMC_SDRAMInitStructure.RowBitsNumber = FMC_SDRAM_ROW_BITS_NUM_13;

    FMC_SDRAMInitStructure.MemoryDataWidth = FMC_SDRAM_MEM_BUS_WIDTH_16; 
    FMC_SDRAMInitStructure.InternalBankNumber = FMC_SDRAM_INTERN_BANKS_NUM_4;
    FMC_SDRAMInitStructure.CASLatency = FMC_SDRAM_CAS_LATENCY_2; 
    FMC_SDRAMInitStructure.WriteProtection = FMC_SDRAM_WRITE_PROTECTION_DISABLE;
    FMC_SDRAMInitStructure.SDClockPeriod = FMC_SDRAM_CLOCK_PERIOD_2; 
    FMC_SDRAMInitStructure.ReadBurst = FMC_SDRAM_RBURST_ENABLE; 
    FMC_SDRAMInitStructure.ReadPipeDelay = FMC_SDRAM_RPIPE_DELAY_1;

    /* FMC SDRAM bank initialization */
    volatile HAL_StatusTypeDef res;
    
    res = FMC_SDRAM_Init(pSdramInstance, &FMC_SDRAMInitStructure);
    res = FMC_SDRAM_Timing_Init(pSdramInstance, &FMC_SDRAMTimingInitStructure, FMC_SDRAM_BANK1);

    /* FMC SDRAM device initialization sequence */
    SDRAM_InitSequence();
  }

    /**
   * @brief  Executes the SDRAM memory initialization sequence.
   * @param  None.
   * @retval None.
   */
  void SDRAM_InitSequence(void) {
    FMC_SDRAM_CommandTypeDef FMC_SDRAMCommandStructure;
    uint32_t tmpr = 0;
    volatile HAL_StatusTypeDef res;

    /* Step 3 --------------------------------------------------------------------*/
    /* Configure a clock configuration enable command */
    FMC_SDRAMCommandStructure.CommandMode = FMC_SDRAM_CMD_CLK_ENABLE;
    FMC_SDRAMCommandStructure.CommandTarget = FMC_SDRAM_CMD_TARGET_BANK1;
    FMC_SDRAMCommandStructure.AutoRefreshNumber = 1;
    FMC_SDRAMCommandStructure.ModeRegisterDefinition = 0;
    /* Send the command */
    res = FMC_SDRAM_SendCommand(pSdramInstance, &FMC_SDRAMCommandStructure, 0xffff);


    //In the ST example, this is 100ms, but the 429 RM says 100us is typical, and
    //the ISSI datasheet confirms this. 1ms seems plenty, and is much shorter than
    //refresh interval, meaning we won't risk losing contents if the SDRAM is in self-refresh
    //mode
    /* Step 4 --------------------------------------------------------------------*/
    /* Insert 1 ms delay */
    //chThdSleepMilliseconds(1);
    osalSysPolledDelayX(OSAL_US2RTC(STM32_HCLK, 100));

    /* Step 5 --------------------------------------------------------------------*/
    /* Configure a PALL (precharge all) command */
    FMC_SDRAMCommandStructure.CommandMode = FMC_SDRAM_CMD_PALL;
    FMC_SDRAMCommandStructure.CommandTarget = FMC_SDRAM_CMD_TARGET_BANK1;
    FMC_SDRAMCommandStructure.AutoRefreshNumber = 1;
    FMC_SDRAMCommandStructure.ModeRegisterDefinition = 0;
    res = FMC_SDRAM_SendCommand(pSdramInstance, &FMC_SDRAMCommandStructure, 0xffff);

    /* Step 6 --------------------------------------------------------------------*/
    /* Configure a Auto-Refresh command */
    FMC_SDRAMCommandStructure.CommandMode = FMC_SDRAM_CMD_AUTOREFRESH_MODE;
    FMC_SDRAMCommandStructure.CommandTarget = FMC_SDRAM_CMD_TARGET_BANK1;
    FMC_SDRAMCommandStructure.AutoRefreshNumber = 4;
    FMC_SDRAMCommandStructure.ModeRegisterDefinition = 0;
    /* Send the  first command */
    res = FMC_SDRAM_SendCommand(pSdramInstance, &FMC_SDRAMCommandStructure, 0xffff);

    /* Step 7 --------------------------------------------------------------------*/
    /* Program the external memory mode register */
    tmpr = (uint32_t)SDRAM_MODEREG_BURST_LENGTH_2 |
    SDRAM_MODEREG_BURST_TYPE_SEQUENTIAL |
    SDRAM_MODEREG_CAS_LATENCY_2 |
    SDRAM_MODEREG_OPERATING_MODE_STANDARD |
    SDRAM_MODEREG_WRITEBURST_MODE_SINGLE;

    /* Configure a load Mode register command*/
    FMC_SDRAMCommandStructure.CommandMode = FMC_SDRAM_CMD_LOAD_MODE;
    FMC_SDRAMCommandStructure.CommandTarget = FMC_SDRAM_CMD_TARGET_BANK1;
    FMC_SDRAMCommandStructure.AutoRefreshNumber = 1;
    FMC_SDRAMCommandStructure.ModeRegisterDefinition = tmpr;
    /* Send the second command */
    res = FMC_SDRAM_SendCommand(pSdramInstance, &FMC_SDRAMCommandStructure, 0xffff);

    /* Step 8 --------------------------------------------------------------------*/

    /* Set the refresh rate counter */
    /* (7.81 us x Freq) - 20 */
    /* Set the device refresh counter */

    res = FMC_SDRAM_ProgramRefreshRate(pSdramInstance, 683); // original 683, stm code has 603!!

    res = FMC_SDRAM_WriteProtection_Disable(pSdramInstance, FMC_SDRAM_BANK1);
  }


  /**
   * @brief  Writes a Entire-word buffer to the SDRAM memory.
   * @param  pBuffer: pointer to buffer.
   * @param  uwWriteAddress: SDRAM memory internal address from which the data will be
   *         written.
   * @param  uwBufferSize: number of words to write.
   * @retval None.
   */
  void SDRAM_WriteBuffer(uint32_t* pBuffer, uint32_t uwWriteAddress,uint32_t uwBufferSize) 
  {
    __IO uint32_t
    write_pointer = (uint32_t)uwWriteAddress;

    /* Disable write protection */
    FMC_SDRAM_WriteProtection_Disable(pSdramInstance, FMC_SDRAM_BANK1);


    /* While there is data to write */
    for (; uwBufferSize != 0; uwBufferSize--) 
    {
      /* Transfer data to the memory */
      *(uint32_t *)(SDRAM_BANK_ADDR + write_pointer) = *pBuffer++;
      *(uint32_t *)(SDRAM_BANK_ADDR + write_pointer) = *pBuffer++;
      *(uint32_t *)(SDRAM_BANK_ADDR + write_pointer) = *pBuffer++;
      *(uint32_t *)(SDRAM_BANK_ADDR + write_pointer) = *pBuffer++;
      *(uint32_t *)(SDRAM_BANK_ADDR + write_pointer) = *pBuffer++;

      /* Increment the address*/
      write_pointer += 4;
    }
  }

  /**
  * @brief  Reads data buffer from the SDRAM memory.
  * @param  pBuffer: pointer to buffer.
  * @param  ReadAddress: SDRAM memory internal address from which the data will be
  *         read.
  * @param  uwBufferSize: number of words to write.
  * @retval None.
  */
  void SDRAM_ReadBuffer(uint32_t* pBuffer, uint32_t uwReadAddress, uint32_t uwBufferSize) 
  {
    __IO uint32_t
    write_pointer = (uint32_t)uwReadAddress;

    /* Read data */
    for (; uwBufferSize != 0x00; uwBufferSize--) 
    {
      *pBuffer++ = *(__IO uint32_t *)(SDRAM_BANK_ADDR + write_pointer );

      /* Increment the address*/
      write_pointer += 4;
    }
  }

#else
  void SDRAM_Init(void) {
    FMC_SDRAMInitTypeDef FMC_SDRAMInitStructure;
    FMC_SDRAMTimingInitTypeDef FMC_SDRAMTimingInitStructure;

    /* Enable FMC clock */
    rccEnableAHB3(RCC_AHB3ENR_FMCEN, FALSE);


    // seb tweaked below values a bit faster for Micron MT48LC16M16A2P
    /* FMC Configuration ---------------------------------------------------------*/
    /* FMC SDRAM Bank configuration */
    /* Timing configuration for 84 Mhz of SD clock frequency (168Mhz/2) */
    /* TMRD: 2 Clock cycles */ // seb 2 clock cycles, so seems good
    FMC_SDRAMTimingInitStructure.FMC_LoadToActiveDelay = 2;
    /* TXSR: min=70ns (6x11.90ns) */ // seb min 67 ns
    FMC_SDRAMTimingInitStructure.FMC_ExitSelfRefreshDelay = 7;
    /* TRAS: min=42ns (4x11.90ns) max=120k (ns) */ // seb same
    FMC_SDRAMTimingInitStructure.FMC_SelfRefreshTime = 4;
    /* TRC:  min=63 (6x11.90ns) */ // seb min 60 ns; set to 6 * 11.11ns = 66.66ns
    FMC_SDRAMTimingInitStructure.FMC_RowCycleDelay = 7; // seb reverted to original 7 due to Axoloti compatibility "backport"
    /* TWR:  2 Clock cycles */ // seb 1 clock cycle + 6 ns, or 12 ns. TWR has to be >= TRAS-TRCD => 4-2 = 2
    FMC_SDRAMTimingInitStructure.FMC_WriteRecoveryTime = 2;
    /* TRP:  15ns => 2x11.90ns */ // seb 18 ns
    FMC_SDRAMTimingInitStructure.FMC_RPDelay = 2;
    /* TRCD: 15ns => 2x11.90ns */ // seb 18 ns
    FMC_SDRAMTimingInitStructure.FMC_RCDDelay = 2;

    /* FMC SDRAM control configuration */
    FMC_SDRAMInitStructure.FMC_Bank = FMC_Bank1_SDRAM;
  #if defined(BOARD_KSOLOTI_CORE)
    /* Row addressing: [8:0] */
    FMC_SDRAMInitStructure.FMC_ColumnBitsNumber = FMC_ColumnBits_Number_9b;
    /* Column addressing: [12:0] */
    FMC_SDRAMInitStructure.FMC_RowBitsNumber = FMC_RowBits_Number_13b;
  #elif defined(BOARD_AXOLOTI_CORE)
    /* Row addressing: [7:0] */
    FMC_SDRAMInitStructure.FMC_ColumnBitsNumber = FMC_ColumnBits_Number_8b;
    /* Column addressing: [11:0] */
    FMC_SDRAMInitStructure.FMC_RowBitsNumber = FMC_RowBits_Number_12b;
  #endif
    FMC_SDRAMInitStructure.FMC_SDMemoryDataWidth = SDRAM_MEMORY_WIDTH; // 16bit
    FMC_SDRAMInitStructure.FMC_InternalBankNumber = FMC_InternalBank_Number_4;
    FMC_SDRAMInitStructure.FMC_CASLatency = SDRAM_CAS_LATENCY; // 2
    FMC_SDRAMInitStructure.FMC_WriteProtection = FMC_Write_Protection_Disable;
    FMC_SDRAMInitStructure.FMC_SDClockPeriod = SDCLOCK_PERIOD; // 2
    FMC_SDRAMInitStructure.FMC_ReadBurst = SDRAM_READBURST; // enabled
    FMC_SDRAMInitStructure.FMC_ReadPipeDelay = FMC_ReadPipe_Delay_1;
    FMC_SDRAMInitStructure.FMC_SDRAMTimingStruct = &FMC_SDRAMTimingInitStructure;

    /* FMC SDRAM bank initialization */
    FMC_SDRAMInit(&FMC_SDRAMInitStructure);

    /* FMC SDRAM device initialization sequence */
    SDRAM_InitSequence();

  }

/**
 * @brief  Executes the SDRAM memory initialization sequence.
 * @param  None.
 * @retval None.
 */
void SDRAM_InitSequence(void) {
  FMC_SDRAMCommandTypeDef FMC_SDRAMCommandStructure;
  uint32_t tmpr = 0;

  /* Step 3 --------------------------------------------------------------------*/
  /* Configure a clock configuration enable command */
  FMC_SDRAMCommandStructure.FMC_CommandMode = FMC_Command_Mode_CLK_Enabled;
  FMC_SDRAMCommandStructure.FMC_CommandTarget = FMC_Command_Target_bank1;
  FMC_SDRAMCommandStructure.FMC_AutoRefreshNumber = 1;
  FMC_SDRAMCommandStructure.FMC_ModeRegisterDefinition = 0;
  /* Wait until the SDRAM controller is ready */
  while (FMC_GetFlagStatus(FMC_Bank1_SDRAM, FMC_FLAG_Busy) != RESET) {
  }
  /* Send the command */
  FMC_SDRAMCmdConfig(&FMC_SDRAMCommandStructure);

  //In the ST example, this is 100ms, but the 429 RM says 100us is typical, and
  //the ISSI datasheet confirms this. 1ms seems plenty, and is much shorter than
  //refresh interval, meaning we won't risk losing contents if the SDRAM is in self-refresh
  //mode
  /* Step 4 --------------------------------------------------------------------*/
  /* Insert 1 ms delay */
  chThdSleepMilliseconds(1);

  /* Step 5 --------------------------------------------------------------------*/
  /* Configure a PALL (precharge all) command */
  FMC_SDRAMCommandStructure.FMC_CommandMode = FMC_Command_Mode_PALL;
  FMC_SDRAMCommandStructure.FMC_CommandTarget = FMC_Command_Target_bank1;
  FMC_SDRAMCommandStructure.FMC_AutoRefreshNumber = 1;
  FMC_SDRAMCommandStructure.FMC_ModeRegisterDefinition = 0;
  /* Wait until the SDRAM controller is ready */
  while (FMC_GetFlagStatus(FMC_Bank1_SDRAM, FMC_FLAG_Busy) != RESET) {
  }
  /* Send the command */
  FMC_SDRAMCmdConfig(&FMC_SDRAMCommandStructure);

  /* Step 6 --------------------------------------------------------------------*/
  /* Configure a Auto-Refresh command */
  FMC_SDRAMCommandStructure.FMC_CommandMode = FMC_Command_Mode_AutoRefresh;
  FMC_SDRAMCommandStructure.FMC_CommandTarget = FMC_Command_Target_bank1;
  FMC_SDRAMCommandStructure.FMC_AutoRefreshNumber = 4;
  FMC_SDRAMCommandStructure.FMC_ModeRegisterDefinition = 0;
  /* Wait until the SDRAM controller is ready */
  while (FMC_GetFlagStatus(FMC_Bank1_SDRAM, FMC_FLAG_Busy) != RESET) {
  }
  /* Send the  first command */
  FMC_SDRAMCmdConfig(&FMC_SDRAMCommandStructure);

  /* Wait until the SDRAM controller is ready */
  while (FMC_GetFlagStatus(FMC_Bank1_SDRAM, FMC_FLAG_Busy) != RESET) {
  }
  /* Send the second command */
  FMC_SDRAMCmdConfig(&FMC_SDRAMCommandStructure);

  /* Step 7 --------------------------------------------------------------------*/
  /* Program the external memory mode register */
  tmpr = (uint32_t)SDRAM_MODEREG_BURST_LENGTH_2 |
  SDRAM_MODEREG_BURST_TYPE_SEQUENTIAL |
  SDRAM_MODEREG_CAS_LATENCY_2 |
  SDRAM_MODEREG_OPERATING_MODE_STANDARD |
  SDRAM_MODEREG_WRITEBURST_MODE_SINGLE;

  /* Configure a load Mode register command*/
  FMC_SDRAMCommandStructure.FMC_CommandMode = FMC_Command_Mode_LoadMode;
  FMC_SDRAMCommandStructure.FMC_CommandTarget = FMC_Command_Target_bank1;
  FMC_SDRAMCommandStructure.FMC_AutoRefreshNumber = 1;
  FMC_SDRAMCommandStructure.FMC_ModeRegisterDefinition = tmpr;
  /* Wait until the SDRAM controller is ready */
  while (FMC_GetFlagStatus(FMC_Bank1_SDRAM, FMC_FLAG_Busy) != RESET) {
  }
  /* Send the command */
  FMC_SDRAMCmdConfig(&FMC_SDRAMCommandStructure);

  /* Step 8 --------------------------------------------------------------------*/

  /* Set the refresh rate counter */
  /* (7.81 us x Freq) - 20 */
  /* Set the device refresh counter */
  FMC_SetRefreshCount(683);
  /* Wait until the SDRAM controller is ready */
  while (FMC_GetFlagStatus(FMC_Bank1_SDRAM, FMC_FLAG_Busy) != RESET) {
  }

  FMC_SDRAMWriteProtectionConfig(FMC_Bank1_SDRAM, DISABLE);
}


/**
 * @brief  Writes a Entire-word buffer to the SDRAM memory.
 * @param  pBuffer: pointer to buffer.
 * @param  uwWriteAddress: SDRAM memory internal address from which the data will be
 *         written.
 * @param  uwBufferSize: number of words to write.
 * @retval None.
 */
void SDRAM_WriteBuffer(uint32_t* pBuffer, uint32_t uwWriteAddress,
  uint32_t uwBufferSize) {
__IO uint32_t
write_pointer = (uint32_t)uwWriteAddress;

/* Disable write protection */
FMC_SDRAMWriteProtectionConfig(FMC_Bank1_SDRAM, DISABLE);

/* Wait until the SDRAM controller is ready */
while (FMC_GetFlagStatus(FMC_Bank1_SDRAM, FMC_FLAG_Busy) != RESET) {
}

/* While there is data to write */
for (; uwBufferSize != 0; uwBufferSize--) {
/* Transfer data to the memory */
*(uint32_t *)(SDRAM_BANK_ADDR + write_pointer) = *pBuffer++;

/* Increment the address*/
write_pointer += 4;
}

}

  /**
  * @brief  Reads data buffer from the SDRAM memory.
  * @param  pBuffer: pointer to buffer.
  * @param  ReadAddress: SDRAM memory internal address from which the data will be
  *         read.
  * @param  uwBufferSize: number of words to write.
  * @retval None.
  */
  void SDRAM_ReadBuffer(uint32_t* pBuffer, uint32_t uwReadAddress,
  uint32_t uwBufferSize) {
    __IO uint32_t
    write_pointer = (uint32_t)uwReadAddress;

    /* Wait until the SDRAM controller is ready */
    while (FMC_GetFlagStatus(FMC_Bank1_SDRAM, FMC_FLAG_Busy) != RESET) {
    }

    /* Read data */
    for (; uwBufferSize != 0x00; uwBufferSize--) {
    *pBuffer++ = *(__IO uint32_t *)(SDRAM_BANK_ADDR + write_pointer );

    /* Increment the address*/
    write_pointer += 4;
    }
  }

#endif

void configSDRAM(void) {
  SDRAM_Init();

#if 0
  int qsource[16];
  int qdest[16];

  int i;
  for (i = 0; i < 16; i++) {
    qsource[i] = i;
  }
//  a small test...
  SDRAM_WriteBuffer(&qsource[0], 0, 16);
  for (i = 0; i < 16; i++) {
    qdest[i] = 0;
  }
  SDRAM_ReadBuffer(&qdest[0], 0, 16);
#endif
}

void memTest(void)
{
  palSetPad(LED2_PORT,LED2_PIN);
  palClearPad(LED1_PORT,LED1_PIN);

#if defined(BOARD_KSOLOTI_CORE)
  int memSize = 0x02000000; // 32MB
#elif defined(BOARD_AXOLOTI_CORE)
  int memSize = 0x00800000; // 8 MB
#endif

  void *base;
  base = (void *)0xC0000000;
  int i;
  // 4MB test
  const uint32_t a = 22695477;
  const uint32_t c = 1;
  //write
  volatile uint32_t iter = 0;
  volatile uint32_t niter = 16;
  volatile uint32_t niter2 = 16;
  // linear write with linear congruential generator values
  // 362 ms execution cycle at 8MB : 22MB/s read+write+compute
  for (iter = 0; iter < niter; iter++)
  {
    palTogglePad(LED2_PORT,LED2_PIN);
    uint32_t x = iter;
    // write
    for (i = 0; i < memSize / 4; i++)
    {
      x = (a * x) + c;
      //
      ((volatile uint32_t *)base)[i] = x;
    }
    // read/verify
    x = iter;
    for (i = 0; i < memSize / 4; i++)
    {
      x = (a * x) + c;
      if (((volatile uint32_t *)base)[i] != x)
      {
        setErrorFlag(ERROR_SDRAM);
        while (1) {
          chThdSleepMilliseconds(100);
        }
      }
    }
  }
  // scattered byte write at linear congruential generator addresses
  // 300 ms execution time for one iteration: 3.3M scattered read+write per second
  // equals 68
  for (iter = 0; iter < niter2; iter++)
  {
    palTogglePad(LED2_PORT,LED2_PIN);
    uint32_t x = iter;
    // write
    for (i = 0; i < 1024 * 1024; i++)
    {
      x = (a * x) + c;
      ((volatile uint8_t *)base)[x & (memSize - 1)] = (uint8_t)i;
    }
    // read/verify
    x = iter;
    for (i = 0; i < 1024 * 1024; i++)
    {
      x = (a * x) + c;
      if (((volatile uint8_t *)base)[x & (memSize - 1)] != (uint8_t)i)
      {
        setErrorFlag(ERROR_SDRAM);
        while (1) {
          chThdSleepMilliseconds(100);
        }
      }
    }
  }
  palClearPad(LED2_PORT,LED2_PIN);
  palSetPad(LED1_PORT,LED1_PIN);
}
