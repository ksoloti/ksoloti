/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
#include "codec_ADAU1961.h"
#include "ch.h"
#include "hal.h"

#include "codec.h"
#include "stm32f4xx.h"
#include "axoloti_board.h"

#define CODEC_ADAU1961_I2S_ENABLE rccEnableSPI2(FALSE)
#define CODEC_ADAU1961_I2S_DISABLE rccDisableSPI2(FALSE)
#define CODEC_ADAU1961_I2S SPI2
#define CODEC_ADAU1961_I2Sext I2S2ext

#if defined(BOARD_KSOLOTI_CORE)
#define CODEC_I2CD I2CD2
#elif defined(BOARD_AXOLOTI_CORE)
#define CODEC_I2CD I2CD3
#elif defined(BOARD_STM32F4_DISCOVERY)
// TODO
#endif

extern void computebufI(int32_t *inp, int32_t *outp);

const stm32_dma_stream_t* i2sdma_ADAU1961;
const stm32_dma_stream_t* i2sdma_ADAU1961rx;

uint32_t codec_interrupt_timestamp;

#define I2S2_TX_DMA_CHANNEL \
STM32_DMA_GETCHANNEL(STM32_SPI_SPI2_TX_DMA_STREAM /* STM32_DMA_STREAM_ID(1, 4) */, \
STM32_SPI2_TX_DMA_CHN /* 0000 */)

#define I2S2ext_RX_DMA_CHANNEL \
STM32_DMA_GETCHANNEL(STM32_DMA_STREAM_ID(1, 3), \
3)

static const SPIConfig spi1c_cfg = {NULL, /* HW dependent part.*/GPIOE, 8,
                                    SPI_CR1_BR_0 | SPI_CR1_BR_1 | SPI_CR1_BR_2
                                        | SPI_CR1_CPOL | SPI_CR1_CPHA};

void codec_ADAU1961_hw_reset(void) {
}

/* Ksoloti Core: I2C interface #2, SDA : PB11, SCL : PB10 */
/* Axoloti Core: I2C interface #3, SDA : PH7,  SCL : PH8  */
static const I2CConfig i2cfg2 = {OPMODE_I2C, 400000, FAST_DUTY_CYCLE_16_9, };

static uint8_t i2crxbuf[8];
static uint8_t i2ctxbuf[8];
static systime_t tmo;

#define ADAU1961_I2C_ADDR (0x70>>1)

void CheckI2CErrors(void) {
  volatile i2cflags_t errors;
  errors = i2cGetErrors(&CODEC_I2CD);
  (void)errors;
}

void ADAU1961_I2CStart(void) {
  // No need to redefine the pin mode each time!
  // palSetPadMode(GPIOB, 10, PAL_MODE_ALTERNATE(4) | PAL_STM32_OTYPE_OPENDRAIN);
  // palSetPadMode(GPIOB, 11, PAL_MODE_ALTERNATE(4) | PAL_STM32_OTYPE_OPENDRAIN);
  // chMtxLock(&Mutex_DMAStream_1_7);
  i2cStart(&CODEC_I2CD, &i2cfg2);
}

void ADAU1961_I2CStop(void) {
  i2cStop(&CODEC_I2CD);
  // chMtxUnlock();
}

uint8_t ADAU1961_ReadRegister(uint16_t RegisterAddr) {
  msg_t status;
  i2ctxbuf[0] = RegisterAddr >> 8;
  i2ctxbuf[1] = RegisterAddr;
  ADAU1961_I2CStart();
  i2cAcquireBus(&CODEC_I2CD);
  status = i2cMasterTransmitTimeout(&CODEC_I2CD, ADAU1961_I2C_ADDR, i2ctxbuf, 2,
                                    i2crxbuf, 1, tmo);
  if (status != RDY_OK) {
    CheckI2CErrors();
  }
  i2cReleaseBus(&CODEC_I2CD);
  ADAU1961_I2CStop();
  chThdSleepMilliseconds(1);
  return i2crxbuf[0];
}

void ADAU1961_ReadRegister6(uint16_t RegisterAddr) {
  msg_t status;
  i2ctxbuf[0] = RegisterAddr >> 8;
  i2ctxbuf[1] = RegisterAddr;
  ADAU1961_I2CStart();
  i2cAcquireBus(&CODEC_I2CD);
  status = i2cMasterTransmitTimeout(&CODEC_I2CD, ADAU1961_I2C_ADDR, i2ctxbuf, 2,
                                    i2crxbuf, 0, tmo);
  if (status != RDY_OK) {
    CheckI2CErrors();
  }
  status = i2cMasterReceiveTimeout(&CODEC_I2CD, ADAU1961_I2C_ADDR, i2crxbuf, 6, tmo);
  if (status != RDY_OK) {
    CheckI2CErrors();
  }
  i2cReleaseBus(&CODEC_I2CD);
  ADAU1961_I2CStop();
  chThdSleepMilliseconds(1);
}

void ADAU1961_WriteRegister(uint16_t RegisterAddr, uint8_t RegisterValue) {
  msg_t status;
  i2ctxbuf[0] = RegisterAddr >> 8;
  i2ctxbuf[1] = RegisterAddr;
  i2ctxbuf[2] = RegisterValue;

  ADAU1961_I2CStart();
  i2cAcquireBus(&CODEC_I2CD);
  status = i2cMasterTransmitTimeout(&CODEC_I2CD, ADAU1961_I2C_ADDR, i2ctxbuf, 3,
                                    i2crxbuf, 0, tmo);
  if (status != RDY_OK) {
    CheckI2CErrors();
    status = i2cMasterTransmitTimeout(&CODEC_I2CD, ADAU1961_I2C_ADDR, i2ctxbuf, 3,
                                      i2crxbuf, 0, tmo);
    chThdSleepMilliseconds(1);
  }
  i2cReleaseBus(&CODEC_I2CD);
  ADAU1961_I2CStop();
  chThdSleepMilliseconds(1);

  uint8_t rd = ADAU1961_ReadRegister(RegisterAddr);
  if (rd != RegisterValue) {
//    while(1){}
    palSetPad(GPIOA, 8);
  }
  chThdSleepMilliseconds(1);
}

void ADAU1961_WriteRegister6(uint16_t RegisterAddr, uint8_t * RegisterValues) {
  msg_t status;
  i2ctxbuf[0] = RegisterAddr >> 8;
  i2ctxbuf[1] = RegisterAddr;
  i2ctxbuf[2] = RegisterValues[0];
  i2ctxbuf[3] = RegisterValues[1];
  i2ctxbuf[4] = RegisterValues[2];
  i2ctxbuf[5] = RegisterValues[3];
  i2ctxbuf[6] = RegisterValues[4];
  i2ctxbuf[7] = RegisterValues[5];
  ADAU1961_I2CStart();
  i2cAcquireBus(&CODEC_I2CD);
  status = i2cMasterTransmitTimeout(&CODEC_I2CD, ADAU1961_I2C_ADDR, i2ctxbuf, 8,
                                    i2crxbuf, 0, TIME_INFINITE);
  i2cReleaseBus(&CODEC_I2CD);
  ADAU1961_I2CStop();
  if (status != RDY_OK) {
    CheckI2CErrors();
  }

  chThdSleepMilliseconds(1);
}

void codec_ADAU1961_hw_init(uint16_t samplerate) {
  palSetPadMode(GPIOA, 8, PAL_MODE_OUTPUT_PUSHPULL);

  int k;
  for (k = 0; k < 3; k++) {
    palSetPad(GPIOA, 8);
    chThdSleepMilliseconds(150);
    palClearPad(GPIOA, 8);
    chThdSleepMilliseconds(150);
  }

  tmo = MS2ST(4);
  chThdSleepMilliseconds(5);
  /*
   * 1. Power down the PLL.
   * 2. Reset the PLL control register.
   * 3. Start the PLL.
   * 4. Poll the lock bit.
   * 5. Assert the core clock enable bit after the PLL lock is acquired.
   */

  while (1) {
    palSetPadMode(GPIOA, 8, PAL_MODE_OUTPUT_PUSHPULL);

    uint8_t pllreg[6];

    if (samplerate == 48000) {
      // reg setting 0x007D 0012 3101
      pllreg[0] = 0x00;
      pllreg[1] = 0x7D;
      pllreg[2] = 0x00;
      pllreg[3] = 0x12;
      pllreg[4] = 0x31;
      pllreg[5] = 0x01;
    }
    // else if (samplerate == 44100) {
    //   // reg setting 0x0271 0193 2901
    //   pllreg[0] = 0x02;
    //   pllreg[1] = 0x71;
    //   pllreg[2] = 0x01;
    //   pllreg[3] = 0x93;
    //   pllreg[4] = 0x29;
    //   pllreg[5] = 0x01;
    // }
    else
      while (1) {
      }

    ADAU1961_WriteRegister6(ADAU1961_REG_R1_PLLC, &pllreg[0]);

    while (1) {
      // wait for PLL
      ADAU1961_ReadRegister6(ADAU1961_REG_R1_PLLC);
      if (i2ctxbuf[5] & 0x02)
        break;
      chThdSleepMilliseconds(1);
      palTogglePad(GPIOA, 8);
    }
    palClearPad(GPIOA, 8);

    ADAU1961_WriteRegister(ADAU1961_REG_R0_CLKC, 0x09); // PLL = clksrc

    // i2s2_sd (dac) is a confirmed connection, i2s2_ext_sd (adc) is not however
    // bclk and lrclk are ok too
    ADAU1961_WriteRegister(ADAU1961_REG_R2_DMICJ, 0x20); // enable digital mic function on pin JACKDET/MICIN
    ADAU1961_WriteRegister(ADAU1961_REG_R3_RES, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R4_RMIXL0, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R5_RMIXL1, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R6_RMIXR0, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R7_RMIXR1, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R8_LDIVOL, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R9_RDIVOL, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R10_MICBIAS, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R11_ALC0, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R12_ALC1, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R13_ALC2, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R14_ALC3, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R15_SERP0, 0x01); // codec is master
    ADAU1961_WriteRegister(ADAU1961_REG_R16_SERP1, 0x00); // 32bit samples
//ADAU1961_WriteRegister(ADAU1961_REG_R16_SERP1,0x60); // 64bit samples, spdif clock!
    ADAU1961_WriteRegister(ADAU1961_REG_R17_CON0, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R18_CON1, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R19_ADCC, 0x30);
    ADAU1961_WriteRegister(ADAU1961_REG_R20_LDVOL, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R21_RDVOL, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R22_PMIXL0, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R23_PMIXL1, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R24_PMIXR0, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R25_PMIXR1, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R26_PLRML, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R27_PLRMR, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R28_PLRMM, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R29_PHPLVOL, 0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R30_PHPRVOL, 0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R31_PLLVOL, 0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R32_PLRVOL, 0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R33_PMONO, 0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R34_POPCLICK, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R35_PWRMGMT, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R36_DACC0, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R37_DACC1, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R38_DACC2, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R39_SERPP, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R40_CPORTP0, 0xAA);
    ADAU1961_WriteRegister(ADAU1961_REG_R41_CPORTP1, 0xAA);
    ADAU1961_WriteRegister(ADAU1961_REG_R42_JACKDETP, 0x00);

    chThdSleepMilliseconds(100);

    break;
  }

#if 0
  while(1) {
    ADAU1961_WriteRegister(ADAU1961_REG_R39_SERPP,0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R39_SERPP,0x03);
    ADAU1961_WriteRegister(ADAU1961_REG_R39_SERPP,0x0C);
    ADAU1961_WriteRegister(ADAU1961_REG_R39_SERPP,0x30);
    ADAU1961_WriteRegister(ADAU1961_REG_R39_SERPP,0xC0);
    ADAU1961_WriteRegister(ADAU1961_REG_R39_SERPP,0x00);
  }
#endif

  {
// ADC Enable
    ADAU1961_WriteRegister(ADAU1961_REG_R19_ADCC, 0x33); // ADC enable
    ADAU1961_WriteRegister(ADAU1961_REG_R36_DACC0, 0x03); // DAC enable

    ADAU1961_WriteRegister(ADAU1961_REG_R31_PLLVOL, 0xE7); // Playback Line Output Left Volume
    ADAU1961_WriteRegister(ADAU1961_REG_R32_PLRVOL, 0xE7); // Playback Right Output Left Volume

    ADAU1961_WriteRegister(ADAU1961_REG_R26_PLRML, 0x05); // unmute Mixer5, 6dB gain
    ADAU1961_WriteRegister(ADAU1961_REG_R27_PLRMR, 0x11); // unmute Mixer6, 6dB gain
    ADAU1961_WriteRegister(ADAU1961_REG_R22_PMIXL0, 0x21); // unmute DAC, no aux mix
    ADAU1961_WriteRegister(ADAU1961_REG_R24_PMIXR0, 0x41); // unmute DAC, no aux mix

    ADAU1961_WriteRegister(ADAU1961_REG_R35_PWRMGMT, 0x03); //enable L&R

    ADAU1961_WriteRegister(ADAU1961_REG_R4_RMIXL0, 0x01); // mixer1 enable, mute LINP and LINR
    ADAU1961_WriteRegister(ADAU1961_REG_R5_RMIXL1, 0x17); // unmute PGA, 6dB gain on aux, 20dB boost
    ADAU1961_WriteRegister(ADAU1961_REG_R6_RMIXR0, 0x01); // mixer2 enable, mute LINP and LINR
    ADAU1961_WriteRegister(ADAU1961_REG_R7_RMIXR1, 0x17); // unmute PGA, 6dB gain on aux, 20 dB boost

    ADAU1961_WriteRegister(ADAU1961_REG_R8_LDIVOL, 0x7F); // not 35.25 dB gain!
    ADAU1961_WriteRegister(ADAU1961_REG_R9_RDIVOL, 0x7F); // not 35.25 dB gain!

    // capless headphone config
    ADAU1961_WriteRegister(ADAU1961_REG_R33_PMONO, 0x03);   //MONOM+MOMODE
    ADAU1961_WriteRegister(ADAU1961_REG_R28_PLRMM, 0x01); // MX7EN, COMMON MODE OUT
    ADAU1961_WriteRegister(ADAU1961_REG_R29_PHPLVOL, 0xE3);
    ADAU1961_WriteRegister(ADAU1961_REG_R30_PHPRVOL, 0xE3);

  }

  chThdSleepMilliseconds(10);
}

static void dma_i2s_interrupt(void* dat, uint32_t flags) {
  (void)dat;
  (void)flags;
  codec_interrupt_timestamp = hal_lld_get_counter_value();
  if ((i2sdma_ADAU1961)->stream->CR & STM32_DMA_CR_CT) {
    computebufI(rbuf, buf2);
  }
  else {
    computebufI(rbuf2, buf);
  }
  dmaStreamClearInterrupt(i2sdma_ADAU1961);
}

static void dma_i2s_rxinterrupt(void* dat, uint32_t flags) {
  (void)dat;
  (void)flags;
  dmaStreamClearInterrupt(i2sdma_ADAU1961rx);
}

static void codec_ADAU1961_dma_init(void) {
  // TX
  i2sdma_ADAU1961 = STM32_DMA_STREAM(STM32_SPI_SPI2_TX_DMA_STREAM);

  uint32_t i2stxdmamode = STM32_DMA_CR_CHSEL(I2S2_TX_DMA_CHANNEL)
      | STM32_DMA_CR_PL(STM32_SPI_SPI2_DMA_PRIORITY) | STM32_DMA_CR_DIR_M2P
      | STM32_DMA_CR_TEIE | STM32_DMA_CR_TCIE | STM32_DMA_CR_DBM | // double buffer mode
      STM32_DMA_CR_PSIZE_HWORD | STM32_DMA_CR_MSIZE_WORD;

  bool_t b = dmaStreamAllocate(i2sdma_ADAU1961, STM32_SPI_SPI2_IRQ_PRIORITY,
                               (stm32_dmaisr_t)dma_i2s_interrupt,
                               (void *)&SPID2);

  dmaStreamSetPeripheral(i2sdma_ADAU1961, &(CODEC_ADAU1961_I2S->DR));
// my double buffer test
  dmaStreamSetMemory0(i2sdma_ADAU1961, buf);
  dmaStreamSetMemory1(i2sdma_ADAU1961, buf2);
  dmaStreamSetTransactionSize(i2sdma_ADAU1961, 64);
  dmaStreamSetMode(i2sdma_ADAU1961, i2stxdmamode | STM32_DMA_CR_MINC);

  // RX
#if 1
  i2sdma_ADAU1961rx = STM32_DMA_STREAM(STM32_SPI_SPI2_RX_DMA_STREAM);

  uint32_t i2srxdmamode = STM32_DMA_CR_CHSEL(3/*I2S2_RX_DMA_CHANNEL*/)
      | STM32_DMA_CR_PL(STM32_SPI_SPI2_DMA_PRIORITY) | STM32_DMA_CR_DIR_P2M |
//  STM32_DMA_CR_DMEIE |
      STM32_DMA_CR_TEIE | STM32_DMA_CR_TCIE | STM32_DMA_CR_DBM | // double buffer mode
      STM32_DMA_CR_PSIZE_HWORD | STM32_DMA_CR_MSIZE_WORD;

  b = dmaStreamAllocate(i2sdma_ADAU1961rx, STM32_SPI_SPI2_IRQ_PRIORITY,
                        (stm32_dmaisr_t)dma_i2s_rxinterrupt, (void *)&SPID2);

  while (b) {
    // failed
  }
//  if (!b)
//  chprintf((BaseChannel*)&SD2, "DMA Allocated Successfully to I2S2\r\n");

  dmaStreamSetPeripheral(i2sdma_ADAU1961rx, &(CODEC_ADAU1961_I2Sext->DR));
// my double buffer test
  dmaStreamSetMemory0(i2sdma_ADAU1961rx, rbuf);
  dmaStreamSetMemory1(i2sdma_ADAU1961rx, rbuf2);
  dmaStreamSetTransactionSize(i2sdma_ADAU1961rx, 64); //PLAYBACK_BUFFER_SIZE);
  dmaStreamSetMode(i2sdma_ADAU1961rx, i2srxdmamode | STM32_DMA_CR_MINC);

  dmaStreamClearInterrupt(i2sdma_ADAU1961rx);
  dmaStreamEnable(i2sdma_ADAU1961rx);
#endif
  // enable
  dmaStreamClearInterrupt(i2sdma_ADAU1961);
  dmaStreamEnable(i2sdma_ADAU1961);
}

void codec_ADAU1961_i2s_init(uint16_t sampleRate) {

#if 1
  palSetPadMode(GPIOB, 12, PAL_MODE_ALTERNATE(5)); // i2s2ws
  palSetPadMode(GPIOB, 13, PAL_MODE_ALTERNATE(5)); // i2s2ck
  palSetPadMode(GPIOB, 14, PAL_MODE_ALTERNATE(5)); // i2s2_ext_sd
  palSetPadMode(GPIOB, 15, PAL_MODE_ALTERNATE(5)); // i2s2_sd
#else // test if codec is connected
  palSetPadMode(GPIOB, 12, PAL_MODE_INPUT); // i2s2ws
  palSetPadMode(GPIOB, 13, PAL_MODE_INPUT); // i2s2ck
  palSetPadMode(GPIOB, 14, PAL_MODE_INPUT); // i2s2_ext_sd
  palSetPadMode(GPIOB, 15, PAL_MODE_INPUT); // i2s2_sd
#endif

  palSetPadMode(GPIOC, 6, PAL_MODE_ALTERNATE(5)); // i2s2_mck

// SPI2 in I2S Mode, Master
  CODEC_ADAU1961_I2S_ENABLE;

  CODEC_ADAU1961_I2S->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_DATLEN_1; /* SLAVE TRANSMIT, 32bit */

  // generate 8MHz clock on MCK pin with PWM...
  static const PWMConfig pwmcfg = {168000000, /* 400kHz PWM clock frequency.  */
                                   21, /* PWM period is 128 cycles.    */
                                   NULL, { {PWM_OUTPUT_ACTIVE_HIGH, NULL}, {
                                       PWM_OUTPUT_ACTIVE_HIGH, NULL},
                                          {PWM_OUTPUT_ACTIVE_HIGH, NULL}, {
                                              PWM_OUTPUT_ACTIVE_HIGH, NULL}},
                                   /* HW dependent part.*/
                                   0,
                                   0};
  palSetPadMode(GPIOC, 6, PAL_MODE_ALTERNATE(3));
  // i2s2_mck
  pwmStart(&PWMD8, &pwmcfg);
  pwmEnableChannel(&PWMD8, 0, 10);

  CODEC_ADAU1961_I2Sext->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_0
      | SPI_I2SCFGR_DATLEN_1; /* SLAVE RECEIVE, 32bit*/
  CODEC_ADAU1961_I2Sext->I2SPR = 0x0002;

//  CODEC_ADAU1961_I2S ->I2SPR = SPI_I2SPR_MCKOE |

//// FULL DUPLEX CONFIG

  ;

  codec_ADAU1961_dma_init();

// Enable I2S DMA Request
  CODEC_ADAU1961_I2S->CR2 = SPI_CR2_TXDMAEN;  //|SPI_CR2_RXDMAEN;
//  CODEC_ADAU1961_I2S ->CR2 = SPI_CR2_RXNEIE;
//  CODEC_ADAU1961_I2S ->CR2 = SPI_CR2_TXEIE;

  CODEC_ADAU1961_I2Sext->CR2 = SPI_CR2_RXDMAEN;
//  CODEC_ADAU1961_I2S ->CR2 = SPI_CR2_TXDMAEN;

// Now Enable I2S
  CODEC_ADAU1961_I2S->I2SCFGR |= SPI_I2SCFGR_I2SE;
  CODEC_ADAU1961_I2Sext->I2SCFGR |= SPI_I2SCFGR_I2SE;
}

void codec_ADAU1961_Stop(void) {
  CODEC_ADAU1961_I2S->I2SCFGR = 0;
  CODEC_ADAU1961_I2Sext->I2SCFGR = 0;
  CODEC_ADAU1961_I2S->CR2 = 0;
  CODEC_ADAU1961_I2Sext->CR2 = 0;

}
