/*
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2024 - 2025 by Ksoloti
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

#include <stdint.h>
#include "axoloti_defines.h"
#include "ch.h"
#include "hal.h"
#include "sysmon.h"
#include "stm32f4xx.h"
#include "stm32f427xx.h"

#ifdef FW_I2SCODEC

/* SPI3 in I2S3 mode - TX: DMA1, Stream 7, Channel 0 */
#define STM32_SPI_I2S3_TX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 7)
#define STM32_I2S3_TX_DMA_CHN 0

/* I2S3_EXT          - RX: DMA1, Stream 0, Channel 3 */
#define STM32_SPI_I2S3_RX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 0)
#define STM32_I2S3EXT_RX_DMA_CHN 3

/* Overriding generic SPI3 priorities here */
#define STM32_SPI_I2S3_DMA_PRIORITY         1
#define STM32_SPI_I2S3_IRQ_PRIORITY         2

#define STM32_I2S3_TX_DMA_CHANNEL           (STM32_DMA_GETCHANNEL(STM32_SPI_I2S3_TX_DMA_STREAM, STM32_I2S3_TX_DMA_CHN))
#define STM32_I2S3_RX_DMA_CHANNEL           (STM32_DMA_GETCHANNEL(STM32_SPI_I2S3_RX_DMA_STREAM, STM32_I2S3EXT_RX_DMA_CHN))

/* Required by wait_sai_dma_tc_flag(): access to SAI DMA's transfer complete flag */
#define STM32_SAI_A_DMA_STREAM              STM32_DMA_STREAM_ID(2, 1)

#define I2S3_WS_PORT       GPIOA
#define I2S3_WS_PIN        15
#define I2S3_BCLK_PORT     GPIOB
#define I2S3_BCLK_PIN      3
#define I2S3_SDIN_PORT     GPIOB
#define I2S3_SDIN_PIN      4
// #define I2S3_MCLK_PORT     GPIOC
// #define I2S3_MCLK_PIN      7
#define I2S3_SDOUT_PORT    GPIOD
#define I2S3_SDOUT_PIN     6

extern void i2s_computebufI(int32_t* i2s_inp, int32_t* i2s_outp);

const stm32_dma_stream_t* i2s_tx_dma;
const stm32_dma_stream_t* i2s_rx_dma;

uint32_t i2s_interrupt_timestamp;

extern int32_t i2s_buf[DOUBLE_BUFSIZE];
extern int32_t i2s_buf2[DOUBLE_BUFSIZE];
extern int32_t i2s_rbuf[DOUBLE_BUFSIZE];
extern int32_t i2s_rbuf2[DOUBLE_BUFSIZE];


void wait_sai_dma_tc_flag(void) {
    volatile uint32_t i = 10000000;
    /* j may have to be changed for any other MCU than STM32F42x! */
    volatile float j = 35.2f * (STM32_SYSCLK / 1000000.f); /* Magic number - see below */
    volatile uint32_t k = (uint32_t) j;

    /* Wait for SAI DMA Transfer Complete flag
     * which marks the beginning of the next 16*2-sample buffer transfer
     */
    while (--i) {
        if ((STM32_DMA_STREAM(STM32_SAI_A_DMA_STREAM))->stream->CR & STM32_DMA_CR_CT) {
            break;
        }
    }

    /* Once the SAI DMA TC flag has been detected, we know a definite timing point.
     * From here we just waste the "exact" amount of time it takes to sync the
     * I2S DMA interrupts to the SAI ones.
     */
    while(k) {
        --k;
    }
}


static void dma_i2s_tx_interrupt(void* dat, uint32_t flags) {

    (void) dat;
    (void) flags;
    i2s_interrupt_timestamp = hal_lld_get_counter_value();

    if ((i2s_tx_dma)->stream->CR & STM32_DMA_CR_CT) {
#ifdef I2S_DEBUG
        palSetPad(GPIOA, 1);
#endif
        i2s_computebufI(i2s_rbuf2, i2s_buf);
    }
    else {
        i2s_computebufI(i2s_rbuf, i2s_buf2);
    }
    dmaStreamClearInterrupt(i2s_tx_dma);
#ifdef I2S_DEBUG
    palClearPad(GPIOA, 1);
#endif
}

static void dma_i2s_rx_interrupt(void* dat, uint32_t flags) {
    (void) dat;
    (void) flags;
    dmaStreamClearInterrupt(i2s_rx_dma);
}

void i2s_peripheral_init(void)  {

#ifdef I2S_DEBUG
    palSetPadMode(GPIOA, 0, PAL_MODE_OUTPUT_PUSHPULL);
    palSetPadMode(GPIOA, 1, PAL_MODE_OUTPUT_PUSHPULL);
#endif

    /* release I2S3 */
    palSetPadMode(I2S3_WS_PORT, I2S3_WS_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_BCLK_PORT, I2S3_BCLK_PIN, PAL_MODE_INPUT);
    //palSetPadMode(I2S3_MCLK_PORT, I2S3_MCLK_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_SDIN_PORT, I2S3_SDIN_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_SDOUT_PORT, I2S3_SDOUT_PIN, PAL_MODE_INPUT);

    rccEnableSPI3(false);
    /* configure I2S peripheral */
    SPI3->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_1 | SPI_I2SCFGR_DATLEN_1; /* I2S master transmit, Philips standard, 32-bit data length, 32-bit channel length */
    SPI3->I2SPR = 0x000C | SPI_I2SPR_ODD;

    I2S3ext->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_0 | SPI_I2SCFGR_DATLEN_1; /* I2S slave receive, Philips standard, 32-bit data length, 32-bit channel length */
    I2S3ext->I2SPR = 0x0002; // 0x000C | SPI_I2SPR_ODD; /* not used in slave mode */

    /* reassign I2S3 */
    palSetPadMode(I2S3_WS_PORT, I2S3_WS_PIN, PAL_MODE_ALTERNATE(6));
    palSetPadMode(I2S3_BCLK_PORT, I2S3_BCLK_PIN, PAL_MODE_ALTERNATE(6));
    // palSetPadMode(I2S3_MCLK_PORT, I2S3_MCLK_PIN, PAL_MODE_ALTERNATE(6));
    palSetPadMode(I2S3_SDIN_PORT, I2S3_SDIN_PIN, PAL_MODE_ALTERNATE(7));
    palSetPadMode(I2S3_SDOUT_PORT, I2S3_SDOUT_PIN, PAL_MODE_ALTERNATE(5));

}


void i2s_dma_init(void) {

    /* initialize DMA */
    i2s_tx_dma = STM32_DMA_STREAM(STM32_SPI_I2S3_TX_DMA_STREAM);

    uint32_t i2s_tx_dma_mode =
        STM32_DMA_CR_CHSEL(STM32_I2S3_TX_DMA_CHANNEL) |
        STM32_DMA_CR_PL(STM32_SPI_I2S3_DMA_PRIORITY) |
        STM32_DMA_CR_DBM /* double buffer mode */ |
        STM32_DMA_CR_DIR_M2P |
        STM32_DMA_CR_MINC |
        STM32_DMA_CR_MSIZE_WORD |
        STM32_DMA_CR_PSIZE_HWORD |
        STM32_DMA_CR_TEIE |
        STM32_DMA_CR_TCIE;

    bool_t b = dmaStreamAllocate(i2s_tx_dma, STM32_SPI_I2S3_IRQ_PRIORITY, (stm32_dmaisr_t) dma_i2s_tx_interrupt, (void*) 0);

    dmaStreamSetPeripheral(i2s_tx_dma, &(SPI3->DR));
    dmaStreamSetMemory0(i2s_tx_dma, i2s_buf);
    dmaStreamSetMemory1(i2s_tx_dma, i2s_buf2);
    dmaStreamSetTransactionSize(i2s_tx_dma, 64);
    dmaStreamSetMode(i2s_tx_dma, i2s_tx_dma_mode);

    i2s_rx_dma = STM32_DMA_STREAM(STM32_SPI_I2S3_RX_DMA_STREAM);

    uint32_t i2s_rx_dma_mode =
        STM32_DMA_CR_CHSEL(STM32_I2S3_RX_DMA_CHANNEL) |
        STM32_DMA_CR_PL(STM32_SPI_I2S3_DMA_PRIORITY) |
        STM32_DMA_CR_DBM /* double buffer mode */ |
        STM32_DMA_CR_DIR_P2M |
        STM32_DMA_CR_MINC |
        STM32_DMA_CR_MSIZE_WORD |
        STM32_DMA_CR_PSIZE_HWORD |
        STM32_DMA_CR_TEIE |
        STM32_DMA_CR_TCIE;

    b |= dmaStreamAllocate(i2s_rx_dma, STM32_SPI_I2S3_IRQ_PRIORITY, (stm32_dmaisr_t) 0, (void*) 0);

    if (b) {
        setErrorFlag(ERROR_CODEC_I2C);
        while (1);
    }

    dmaStreamSetPeripheral(i2s_rx_dma, &(I2S3ext->DR));
    dmaStreamSetMemory0(i2s_rx_dma, i2s_rbuf);
    dmaStreamSetMemory1(i2s_rx_dma, i2s_rbuf2);
    dmaStreamSetTransactionSize(i2s_rx_dma, 64);
    dmaStreamSetMode(i2s_rx_dma, i2s_rx_dma_mode);

}


void i2s_init(void) {

    i2s_peripheral_init();

    i2s_dma_init();

    /* Sync I2S DMA pointer to SAI... */
    chSysLock();
    wait_sai_dma_tc_flag();
    dmaStreamClearInterrupt(i2s_tx_dma);
    dmaStreamEnable(i2s_tx_dma);
    dmaStreamClearInterrupt(i2s_rx_dma);
    dmaStreamEnable(i2s_rx_dma);
    SPI3->CR2 = SPI_CR2_TXDMAEN;
    I2S3ext->CR2 = SPI_CR2_RXDMAEN;
    SPI3->I2SCFGR |= SPI_I2SCFGR_I2SE;
    I2S3ext->I2SCFGR |= SPI_I2SCFGR_I2SE;
    chSysUnlock();
}


void i2s_dma_stop(void) {
}


void i2s_stop(void) {
    SPI3->I2SCFGR = 0;
    I2S3ext->I2SCFGR = 0;
    SPI3->CR2 = 0;
    I2S3ext->CR2 = 0;
}

#endif /* FW_I2SCODEC */