/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
#include "stm32f4xx_hal_i2c.h"
#include "axoloti_board.h"
#include "sysmon.h"
#include "spilink.h"
#include "spilink_lld.h"

#define ADAU1961_I2C_ADDR 0x70 /* (0x38<<1) */
#define TIMEOUT 1000000

#define STM32_SAI_A_DMA_STREAM STM32_DMA_STREAM_ID(2, 1)
#define STM32_SAI_B_DMA_STREAM STM32_DMA_STREAM_ID(2, 4)
#define SAI_A_DMA_CHANNEL 0
#define SAI_B_DMA_CHANNEL 1
#define STM32_SAI_A_DMA_PRIORITY 1
#define STM32_SAI_B_DMA_PRIORITY 1
#define STM32_SAI_A_IRQ_PRIORITY 2
#define STM32_SAI_B_IRQ_PRIORITY 2

#define SAI1_FS_PORT GPIOE
#define SAI1_FS_PIN 4
#define SAI1_SCK_PORT GPIOE
#define SAI1_SCK_PIN 5
#define SAI1_SD_A_PORT GPIOE
#define SAI1_SD_A_PIN 6
#define SAI1_SD_B_PORT GPIOE
#define SAI1_SD_B_PIN 3

#define MCO1_PORT GPIOA
#define MCO1_PIN 8

#define SPILINK_FSYNC_PORT GPIOD
#define SPILINK_FSYNC_PIN 5

// #define DEBUG_SAI_INT_ON_GPIO 1

extern void computebufI(int32_t *inp, int32_t *outp);

const stm32_dma_stream_t* sai_a_dma;
const stm32_dma_stream_t* sai_b_dma;

volatile SAI_Block_TypeDef *sai_a = SAI1_Block_A;
volatile SAI_Block_TypeDef *sai_b = SAI1_Block_B;

/* use STM32 HAL for I2C */
static I2C_HandleTypeDef ADAU1961_i2c_handle;

static uint8_t i2crxbuf[8];
static uint8_t i2ctxbuf[8];

/* approx 1Hz drift... */
// static uint8_t pll48k_pullup[6]   = {0x1F, 0x40, 0x04, 0x82, 0x31, 0x01};
static uint8_t pll48k_exact[6]    = {0x1F, 0x40, 0x04, 0x81, 0x31, 0x01};
static uint8_t pll48k_pulldown[6] = {0x1F, 0x40, 0x04, 0x80, 0x31, 0x01};

uint32_t codec_interrupt_timestamp;

typedef enum {
    falling=0,
    rising=1
} edge_t;


void blink_and_retry(void)
{
    sysmon_blink_pattern(SYNCED_ERROR);
    chThdSleepMilliseconds(2000);
}


void wait_SPI_fsync(edge_t edge)
{
    while (1)
    {
        /* sync on NSS. */
        palSetPadMode(SPILINK_FSYNC_PORT, SPILINK_FSYNC_PIN, PAL_MODE_INPUT);

        volatile int i,j;

        j = 1000000; /* wait till NSS is low (or already is) */
        while(--j)
        {
            if (edge ^ !palReadPad(SPILINK_FSYNC_PORT, SPILINK_FSYNC_PIN))
                break;
        }

        i = 1000000; /* wait till NSS is high */
        while(--i)
        {
            if (edge ^ palReadPad(SPILINK_FSYNC_PORT, SPILINK_FSYNC_PIN))
                break;
        }

        j = 1000000; /* wait till NSS is low again */
        while(--j)
        {
            if (edge ^ !palReadPad(SPILINK_FSYNC_PORT, SPILINK_FSYNC_PIN))
                break;
        }

        if ((j == 0) || (i == 0))
        {
            /* no pulse edge found. Blink and retry. */
            blink_and_retry();
        }
        else
            break; /* pulse edge found. Leave this loop and function. */
    }
}


void detect_MCO(void)
{
    while (1)
    {
        volatile int i, j;

        chSysLock();

        j = 1000; /* wait till clock is low */
        while (--j)
        {
            if (!palReadPad(MCO1_PORT, MCO1_PIN))
                break;
        }

        i = 1000; /* then wait till clock is high */
        while (--i)
        {
            if (palReadPad(MCO1_PORT, MCO1_PIN))
                break;
        }

        j = 1000; /* then wait till clock is low again */
        while (--j)
        {
            if (!palReadPad(MCO1_PORT, MCO1_PIN))
                break;
        }

        chSysUnlock();

        if ((j == 0) || (i == 0))
        {
            /* no pulse edge found. Blink and retry. */
            blink_and_retry();
        }
        else
            break; /* clock found, leave this loop... */
    }
}


void lock_SAI_to_SPI_FS(void)
{
    while(1)
    {
        /* Sync codec FS with SPI frame */
        volatile int i = 0;

        /* Wait for SPI frame */
        wait_SPI_fsync(falling);

        /* Now count time to SAI1 FS edge */
        while(palReadPad(SAI1_FS_PORT, SAI1_FS_PIN))
            ++i;

        if ((i > 1) && (i < 4))
            break; /* Lock found */
    }
}


static void ADAU_I2C_Init(void)
{
    if(HAL_I2C_GetState(&ADAU1961_i2c_handle) == HAL_I2C_STATE_RESET)
    {
        /* DISCOVERY_I2Cx peripheral configuration */
        ADAU1961_i2c_handle.Init.ClockSpeed = 400000;
        ADAU1961_i2c_handle.Init.DutyCycle = I2C_DUTYCYCLE_16_9;
        ADAU1961_i2c_handle.Init.OwnAddress1 = 0x33;
        ADAU1961_i2c_handle.Init.AddressingMode = I2C_ADDRESSINGMODE_7BIT;
        ADAU1961_i2c_handle.Instance = I2C2;

        /* SCL: PB10, SDA: PB11 */
        palSetPadMode(GPIOB, 10, PAL_MODE_ALTERNATE(4) | PAL_STM32_OTYPE_OPENDRAIN | PAL_STM32_PUDR_PULLUP);
        palSetPadMode(GPIOB, 11, PAL_MODE_ALTERNATE(4) | PAL_STM32_OTYPE_OPENDRAIN | PAL_STM32_PUDR_PULLUP);

        rccEnableI2C2(FALSE);
        // nvicEnableVector(I2C2_EV_IRQn, STM32_I2C_I2C2_IRQ_PRIORITY);
        // nvicEnableVector(I2C2_ER_IRQn, STM32_I2C_I2C2_IRQ_PRIORITY);

        HAL_I2C_Init(&ADAU1961_i2c_handle);
    }
}


uint32_t HAL_GetTick(void)
{
    return halGetCounterValue();
}


uint32_t HAL_RCC_GetPCLK1Freq(void)
{
    return STM32_PCLK1;
}


void ADAU1961_WriteRegister(uint16_t RegisterAddr, uint8_t RegisterValue)
{
    i2ctxbuf[0] = RegisterAddr >> 8;
    i2ctxbuf[1] = RegisterAddr;
    i2ctxbuf[2] = RegisterValue;

    chThdSleepMilliseconds(2);

    HAL_StatusTypeDef r = HAL_I2C_Master_Transmit(&ADAU1961_i2c_handle, ADAU1961_I2C_ADDR, i2ctxbuf, 3, TIMEOUT);

    if (r != HAL_OK)
    {
        // volatile unsigned int i = r;
        while(1);
    }

    chThdSleepMilliseconds(2);
}


void ADAU1961_WriteRegister6(uint16_t RegisterAddr, uint8_t * RegisterValues)
{

    i2ctxbuf[0] = RegisterAddr >> 8;
    i2ctxbuf[1] = RegisterAddr;
    i2ctxbuf[2] = RegisterValues[0];
    i2ctxbuf[3] = RegisterValues[1];
    i2ctxbuf[4] = RegisterValues[2];
    i2ctxbuf[5] = RegisterValues[3];
    i2ctxbuf[6] = RegisterValues[4];
    i2ctxbuf[7] = RegisterValues[5];

    chThdSleepMilliseconds(2);

    HAL_StatusTypeDef r = HAL_I2C_Master_Transmit(&ADAU1961_i2c_handle, ADAU1961_I2C_ADDR, i2ctxbuf, 8, TIMEOUT);

    if (r != HAL_OK)
    {
        setErrorFlag(ERROR_CODEC_I2C);
        while(1);
    }

    chThdSleepMilliseconds(10);
}


void ADAU1961_ReadRegister6(uint16_t RegisterAddr)
{
    i2ctxbuf[0] = RegisterAddr >> 8;
    i2ctxbuf[1] = RegisterAddr;

    chThdSleepMilliseconds(2);

    HAL_I2C_Master_Transmit(&ADAU1961_i2c_handle, ADAU1961_I2C_ADDR, i2ctxbuf, 2, TIMEOUT);

    chThdSleepMilliseconds(2);

    HAL_StatusTypeDef r = HAL_I2C_Master_Receive(&ADAU1961_i2c_handle, ADAU1961_I2C_ADDR+1, i2crxbuf, 6, TIMEOUT);

    if (r != HAL_OK)
    {
        setErrorFlag(ERROR_CODEC_I2C);
        while(1);
    }

    chThdSleepMilliseconds(10);
}


static void dma_sai_a_interrupt_spilink_master(void* dat, uint32_t flags)
{
    (void) dat;
    (void) flags;

#ifdef DEBUG_SAI_INT_ON_GPIO
    palSetPadMode(GPIOA, 0, PAL_MODE_OUTPUT_PUSHPULL);
    palSetPad(GPIOA, 0);
#endif

    chSysLockFromIsr();
    codec_interrupt_timestamp = halGetCounterValue();
    spilink_master_process();
    chSysUnlockFromIsr();

    if ((sai_a_dma)->stream->CR & STM32_DMA_CR_CT)
        computebufI(rbuf2, buf);
    else
        computebufI(rbuf, buf2);

#ifdef DEBUG_SAI_INT_ON_GPIO
    palClearPad(GPIOA, 0);
#endif
}


static void dma_sai_a_interrupt_spilink_slave(void* dat, uint32_t flags)
{
    (void) dat;
    (void) flags;

#ifdef DEBUG_SAI_INT_ON_GPIO
    palSetPadMode(GPIOA, 0, PAL_MODE_OUTPUT_PUSHPULL);
    palSetPad(GPIOA, 0);
#endif

    codec_interrupt_timestamp = halGetCounterValue();
    spilink_slave_process();

    if ((sai_a_dma)->stream->CR & STM32_DMA_CR_CT)
        computebufI(rbuf2, buf);
    else
        computebufI(rbuf, buf2);

#ifdef DEBUG_SAI_INT_ON_GPIO
    palClearPad(GPIOA, 0);
#endif
}


#ifdef DEBUG_SAI_INT_ON_GPIO
static void dma_sai_b_interrupt(void* dat, uint32_t flags)
{
    (void) dat;
    (void) flags;
    palSetPadMode(GPIOA, 3, PAL_MODE_OUTPUT_PUSHPULL);
    palSetPad(GPIOA, 3);
    asm("nop"); asm("nop"); asm("nop"); asm("nop");
    palClearPad(GPIOA, 3);
}
#endif


void codec_ADAU1961_hw_init(uint16_t samplerate, bool_t isMaster)
{
    ADAU_I2C_Init();
    chThdSleepMilliseconds(5);

    /*
    * 1. Power down the PLL.
    * 2. Reset the PLL control register.
    * 3. Start the PLL.
    * 4. Poll the lock bit.
    * 5. Assert the core clock enable bit after the PLL lock is acquired.
    */

    ADAU1961_WriteRegister(ADAU1961_REG_R0_CLKC, 0x0E); /* Disable core, PLL as clksrc, 1024*FS */


    /* Confirm samplerate (redundant) */
    if (samplerate != 48000)
    {
        /* Incompatible sample rate. Do nothing. */
        setErrorFlag(ERROR_CODEC_I2C);
        while (1);
    }

    if (isMaster)
        ADAU1961_WriteRegister6(ADAU1961_REG_R1_PLLC, &pll48k_exact[0]);
    else
        /* Temporarily set lower PLL frequency of synced odec */
        ADAU1961_WriteRegister6(ADAU1961_REG_R1_PLLC, &pll48k_pulldown[0]);
    chThdSleepMilliseconds(3);

    uint8_t i = 100;
    while(--i)
    {
        /* wait for PLL */
        ADAU1961_ReadRegister6(ADAU1961_REG_R1_PLLC);
        if (i2crxbuf[5] & 0x02)
        {
            /* Wait until PLL signals locked state */
            break;
        }
        chThdSleepMilliseconds(10);

        if (i == 0)
        {
            /* PLL never got to lock... Something wrong with the codec? */
            setErrorFlag(ERROR_CODEC_I2C);
            while (1);
        }
    }

    ADAU1961_WriteRegister(ADAU1961_REG_R0_CLKC,     0x0F); /* Enable core, PLL as clksrc, 1024*FS */
    chThdSleepMilliseconds(1);

    /*
    * i2s2_sd (dac) is a confirmed connection, i2s2_ext_sd (adc) is not however
    * bclk and lrclk are ok too
    */

    ADAU1961_WriteRegister(ADAU1961_REG_R2_DMICJ,    0x20); /* Enable digital mic function via pin JACKDET/MICIN */
    ADAU1961_WriteRegister(ADAU1961_REG_R3_RES,      0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R4_RMIXL0,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R5_RMIXL1,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R6_RMIXR0,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R7_RMIXR1,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R8_LDIVOL,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R9_RDIVOL,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R10_MICBIAS, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R11_ALC0,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R12_ALC1,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R13_ALC2,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R14_ALC3,    0x00);

#ifdef USING_ADAU1761
    ADAU1961_WriteRegister(ADAU1761_REG_R58_SERINRT,  0x01);
    ADAU1961_WriteRegister(ADAU1761_REG_R59_SEROUTRT, 0x01);
    ADAU1961_WriteRegister(ADAU1761_REG_R64_SERSR,    0x00);
    ADAU1961_WriteRegister(ADAU1761_REG_R65_CKEN0,    0x7F);
    ADAU1961_WriteRegister(ADAU1761_REG_R66_CKEN1,    0x03); /* Enable CLK0 and CLK1 (generate BCLK and LRCLK) */
#endif

    ADAU1961_WriteRegister(ADAU1961_REG_R15_SERP0,    0x01); /* Codec is master in both modes */

    if (!isMaster)
    {
        /* Pick up timing from codec's LRCLK (=FS) */

        palSetPad(LED2_PORT, LED2_PIN); /* Light red LED as long as there is no sync */

        chSysLock();
        lock_SAI_to_SPI_FS();
        chSysUnlock();

        /* Now write exact frequency back into the PLL */
        ADAU1961_WriteRegister6(ADAU1961_REG_R1_PLLC, &pll48k_exact[0]);

        palClearPad(LED2_PORT, LED2_PIN); /* Clear red LED as we are now synced */

        dmaStreamClearInterrupt(sai_b_dma);
        dmaStreamEnable(sai_b_dma);

        dmaStreamClearInterrupt(sai_a_dma);
        dmaStreamEnable(sai_a_dma);

        SAI1_Block_A->CR1 |= SAI_xCR1_SAIEN;
        SAI1_Block_B->CR1 |= SAI_xCR1_SAIEN;
    }

    ADAU1961_WriteRegister(ADAU1961_REG_R16_SERP1,    0x00); /* 32bit samples */
    // ADAU1961_WriteRegister(ADAU1961_REG_R16_SERP1,    0x60); /* 64bit samples, spdif clock! */

    ADAU1961_WriteRegister(ADAU1961_REG_R17_CON0,     0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R18_CON1,     0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R19_ADCC,     0x30); /* ADC highpass enabled */
    ADAU1961_WriteRegister(ADAU1961_REG_R20_LDVOL,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R21_RDVOL,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R22_PMIXL0,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R23_PMIXL1,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R24_PMIXR0,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R25_PMIXR1,   0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R26_PLRML,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R27_PLRMR,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R28_PLRMM,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R29_PHPLVOL,  0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R30_PHPRVOL,  0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R31_PLLVOL,   0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R32_PLRVOL,   0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R33_PMONO,    0x02);
    ADAU1961_WriteRegister(ADAU1961_REG_R34_POPCLICK, 0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R35_PWRMGMT,  0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R36_DACC0,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R37_DACC1,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R38_DACC2,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R39_SERPP,    0x00);
    ADAU1961_WriteRegister(ADAU1961_REG_R40_CPORTP0,  0xAA);
    ADAU1961_WriteRegister(ADAU1961_REG_R41_CPORTP1,  0xAA);
    ADAU1961_WriteRegister(ADAU1961_REG_R42_JACKDETP, 0x00);

    chThdSleepMilliseconds(10);

    ADAU1961_WriteRegister(ADAU1961_REG_R19_ADCC,     0x33); /* ADC enable, highpass enabled */
    ADAU1961_WriteRegister(ADAU1961_REG_R36_DACC0,    0x03); /* DAC enable */

    ADAU1961_WriteRegister(ADAU1961_REG_R31_PLLVOL,   0xE7); /* Playback Line Output Left Volume */
    ADAU1961_WriteRegister(ADAU1961_REG_R32_PLRVOL,   0xE7); /* Playback Right Output Left Volume */

    ADAU1961_WriteRegister(ADAU1961_REG_R26_PLRML,    0x05); /* unmute Mixer5, 6dB gain */
    ADAU1961_WriteRegister(ADAU1961_REG_R27_PLRMR,    0x11); /* unmute Mixer6, 6dB gain */
    ADAU1961_WriteRegister(ADAU1961_REG_R22_PMIXL0,   0x21); /* unmute DAC, no aux mix */
    ADAU1961_WriteRegister(ADAU1961_REG_R24_PMIXR0,   0x41); /* unmute DAC, no aux mix */

    ADAU1961_WriteRegister(ADAU1961_REG_R35_PWRMGMT,  0x03); /* enable L&R */

    ADAU1961_WriteRegister(ADAU1961_REG_R4_RMIXL0,    0x01); /* mixer1 enable, mute LINP and LINR */
    ADAU1961_WriteRegister(ADAU1961_REG_R5_RMIXL1,    0x08); /* unmute PGA, aux mute, 0 dB boost */
    ADAU1961_WriteRegister(ADAU1961_REG_R6_RMIXR0,    0x01); /* mixer2 enable, mute LINP and LINR */
    ADAU1961_WriteRegister(ADAU1961_REG_R7_RMIXR1,    0x08); /* unmute PGA, aux mute, 0 dB boost */

    ADAU1961_WriteRegister(ADAU1961_REG_R8_LDIVOL,    0x43); /* 0dB gain */
    ADAU1961_WriteRegister(ADAU1961_REG_R9_RDIVOL,    0x43); /* 0dB gain */

    /* capless headphone config */
    ADAU1961_WriteRegister(ADAU1961_REG_R33_PMONO,    0x03); /* MONOM + MOMODE */
    ADAU1961_WriteRegister(ADAU1961_REG_R28_PLRMM,    0x01); /* MX7EN, COMMON MODE OUT */
    ADAU1961_WriteRegister(ADAU1961_REG_R29_PHPLVOL,  0xC3);
    ADAU1961_WriteRegister(ADAU1961_REG_R30_PHPRVOL,  0xC3);

    chThdSleepMilliseconds(10);
}


void codec_ADAU1961_SAI_init(uint16_t samplerate, bool_t isMaster)
{


    /*configure MCO */
    if (isMaster) {
        /* Core in master mode */
        palSetPadMode(MCO1_PORT, MCO1_PIN, PAL_MODE_OUTPUT_PUSHPULL);
        palSetPadMode(MCO1_PORT, MCO1_PIN, PAL_MODE_ALTERNATE(0));
    }
    else {
        /* Core in synced mode */
        palSetPadMode(MCO1_PORT, MCO1_PIN, PAL_MODE_INPUT);
        /* verify clock is present */
        detect_MCO();
    }

    /* release SAI */
    palSetPadMode(SAI1_FS_PORT, SAI1_FS_PIN, PAL_MODE_INPUT);
    palSetPadMode(SAI1_SD_A_PORT, SAI1_SD_A_PIN, PAL_MODE_INPUT);
    palSetPadMode(SAI1_SD_B_PORT, SAI1_SD_B_PIN, PAL_MODE_INPUT);
    palSetPadMode(SAI1_SCK_PORT, SAI1_SCK_PIN, PAL_MODE_INPUT);

    codec_ADAU1961_hw_init(samplerate, isMaster);

    /* configure SAI */
    RCC->APB2ENR |= RCC_APB2ENR_SAI1EN;
    chThdSleepMilliseconds(1);
    SAI1_Block_A->CR2 = 0;//SAI_xCR2_FTH_1;
    SAI1_Block_B->CR2 = 0;//SAI_xCR2_FTH_0;

    SAI1_Block_A->FRCR = /*SAI_xFRCR_FSDEF |*/ SAI_xFRCR_FRL_0 | SAI_xFRCR_FRL_1
        | SAI_xFRCR_FRL_2 | SAI_xFRCR_FRL_3 | SAI_xFRCR_FRL_4 | SAI_xFRCR_FRL_5
        | SAI_xFRCR_FSALL_0 | SAI_xFRCR_FSALL_1 | SAI_xFRCR_FSALL_2
        | SAI_xFRCR_FSALL_3 | SAI_xFRCR_FSALL_4 | SAI_xFRCR_FSOFF;

    SAI1_Block_B->FRCR = /*SAI_xFRCR_FSDEF |*/ SAI_xFRCR_FRL_0 | SAI_xFRCR_FRL_1
        | SAI_xFRCR_FRL_2 | SAI_xFRCR_FRL_3 | SAI_xFRCR_FRL_4 | SAI_xFRCR_FRL_5
        | SAI_xFRCR_FSALL_0 | SAI_xFRCR_FSALL_1 | SAI_xFRCR_FSALL_2
        | SAI_xFRCR_FSALL_3 | SAI_xFRCR_FSALL_4 | SAI_xFRCR_FSOFF;

    SAI1_Block_A->SLOTR = (3 << 16) | SAI_xSLOTR_NBSLOT_0;
    SAI1_Block_B->SLOTR = (3 << 16) | SAI_xSLOTR_NBSLOT_0;

    /* SAI1_A is slave transmitter */
    /* SAI1_B is synchronous slave receiver */
    SAI1_Block_A->CR1 = SAI_xCR1_DS_0 | SAI_xCR1_DS_1 | SAI_xCR1_DS_2
        | SAI_xCR1_MODE_1 | SAI_xCR1_DMAEN | SAI_xCR1_CKSTR;

    SAI1_Block_B->CR1 = SAI_xCR1_DS_0 | SAI_xCR1_DS_1 | SAI_xCR1_DS_2
        | SAI_xCR1_SYNCEN_0 | SAI_xCR1_MODE_1 | SAI_xCR1_MODE_0
        | SAI_xCR1_DMAEN | SAI_xCR1_CKSTR;

    chThdSleepMilliseconds(1);

    /* reassign SAI */
    palSetPadMode(SAI1_FS_PORT, SAI1_FS_PIN, PAL_MODE_ALTERNATE(6));
    palSetPadMode(SAI1_SD_A_PORT, SAI1_SD_A_PIN, PAL_MODE_ALTERNATE(6));
    palSetPadMode(SAI1_SD_B_PORT, SAI1_SD_B_PIN, PAL_MODE_ALTERNATE(6));
    palSetPadMode(SAI1_SCK_PORT, SAI1_SCK_PIN, PAL_MODE_ALTERNATE(6));

    /* initialize DMA */
    sai_a_dma = STM32_DMA_STREAM(STM32_SAI_A_DMA_STREAM);
    sai_b_dma = STM32_DMA_STREAM(STM32_SAI_B_DMA_STREAM);

    uint32_t sai_a_dma_mode = STM32_DMA_CR_CHSEL(SAI_A_DMA_CHANNEL)
        | STM32_DMA_CR_PL(STM32_SAI_A_DMA_PRIORITY) | STM32_DMA_CR_DIR_M2P
        | STM32_DMA_CR_TEIE | STM32_DMA_CR_TCIE | STM32_DMA_CR_DBM /* double buffer mode */
        | STM32_DMA_CR_PSIZE_WORD | STM32_DMA_CR_MSIZE_WORD;

    uint32_t sai_b_dma_mode = STM32_DMA_CR_CHSEL(SAI_B_DMA_CHANNEL)
        | STM32_DMA_CR_PL(STM32_SAI_B_DMA_PRIORITY) | STM32_DMA_CR_DIR_P2M
        | STM32_DMA_CR_TEIE | STM32_DMA_CR_TCIE | STM32_DMA_CR_DBM /* double buffer mode */
        | STM32_DMA_CR_PSIZE_WORD | STM32_DMA_CR_MSIZE_WORD;

    bool_t b;
    if  (isMaster)
        b = dmaStreamAllocate(sai_a_dma, STM32_SAI_A_IRQ_PRIORITY,
                              (stm32_dmaisr_t)dma_sai_a_interrupt_spilink_master, (void *)0);
    else
        b = dmaStreamAllocate(sai_a_dma, STM32_SAI_A_IRQ_PRIORITY,
                              (stm32_dmaisr_t)dma_sai_a_interrupt_spilink_slave, (void *)0);
    dmaStreamSetPeripheral(sai_a_dma, &(sai_a->DR));
    dmaStreamSetMemory0(sai_a_dma, buf);
    dmaStreamSetMemory1(sai_a_dma, buf2);
    dmaStreamSetTransactionSize(sai_a_dma, 32);
    dmaStreamSetMode(sai_a_dma, sai_a_dma_mode | STM32_DMA_CR_MINC);


#ifdef DEBUG_SAI_INT_ON_GPIO
    b |= dmaStreamAllocate(sai_b_dma, STM32_SAI_B_IRQ_PRIORITY, dma_sai_b_interrupt, (void *)0);
#else
    b |= dmaStreamAllocate(sai_b_dma, STM32_SAI_B_IRQ_PRIORITY, (stm32_dmaisr_t)0, (void *)0);
#endif

    if (b)
    {
        setErrorFlag(ERROR_CODEC_I2C);
        while (1);
    }

    dmaStreamSetPeripheral(sai_b_dma, &(sai_b->DR));
    dmaStreamSetMemory0(sai_b_dma, rbuf);
    dmaStreamSetMemory1(sai_b_dma, rbuf2);
    dmaStreamSetTransactionSize(sai_b_dma, 32);
    dmaStreamSetMode(sai_b_dma, sai_b_dma_mode | STM32_DMA_CR_MINC);

    dmaStreamClearInterrupt(sai_b_dma);
    dmaStreamClearInterrupt(sai_a_dma);

    if (isMaster)
    {
        chSysLock();
        SAI1_Block_A->CR2 |= SAI_xCR2_FFLUSH;
        SAI1_Block_B->CR2 |= SAI_xCR2_FFLUSH;
        SAI1_Block_A->DR=0;
        SAI1_Block_B->DR=0;
        dmaStreamEnable(sai_b_dma);
        dmaStreamEnable(sai_a_dma);
        SAI1_Block_B->CR1 |= SAI_xCR1_SAIEN;
        SAI1_Block_A->CR1 |= SAI_xCR1_SAIEN;
        /* 2.25 us offset between dmarx and dmatx */
        chSysUnlock();
    }
    else
    {
        chSysLock();
        SAI1_Block_A->CR2 |= SAI_xCR2_FFLUSH;
        SAI1_Block_B->CR2 |= SAI_xCR2_FFLUSH;
        SAI1_Block_A->DR=0;
        SAI1_Block_B->DR=0;
        wait_SPI_fsync(rising);
        dmaStreamEnable(sai_b_dma);
        dmaStreamEnable(sai_a_dma);
        SAI1_Block_B->CR1 |= SAI_xCR1_SAIEN;
        SAI1_Block_A->CR1 |= SAI_xCR1_SAIEN;
        chSysUnlock();
    }
}
