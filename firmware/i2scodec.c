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
// #include "stm32f4xx.h"
// #include "stm32f427xx.h"

#ifdef FW_I2SCODEC

/* SPI3 in I2S3 mode - TX: DMA1, Stream 7, Channel 0 */
#define STM32_SPI_I2S3_TX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 7)
#define STM32_I2S3_TX_DMA_CHANNEL 0

/* I2S3_EXT          - RX: DMA1, Stream 0, Channel 3 */
#define STM32_SPI_I2S3_RX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 0)
#define STM32_I2S3EXT_RX_DMA_CHANNEL 3

/* Overriding generic SPI3 priorities here */
#define STM32_SPI_I2S3_DMA_PRIORITY         1
#define STM32_SPI_I2S3_IRQ_PRIORITY         3

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


const stm32_dma_stream_t* i2s_tx_dma;
const stm32_dma_stream_t* i2s_rx_dma;

uint32_t i2s_tc_interrupt_timestamp;

extern int32_t i2s_buf[DOUBLE_BUFSIZE];
extern int32_t i2s_buf2[DOUBLE_BUFSIZE];
extern int32_t i2s_rbuf[DOUBLE_BUFSIZE];
extern int32_t i2s_rbuf2[DOUBLE_BUFSIZE];


static void dma_i2s_tx_interrupt(void* dat, uint32_t flags) {

    (void) dat;
    (void) flags;

    if ((i2s_tx_dma)->stream->CR & STM32_DMA_CR_CT) {
        i2s_tc_interrupt_timestamp = hal_lld_get_counter_value();

#ifdef I2S_DEBUG
        palSetPadMode(GPIOA, 1, PAL_MODE_OUTPUT_PUSHPULL);
        palSetPad(GPIOA, 1);
#endif

    }
    else {
        /* No action */
    }
    dmaStreamClearInterrupt(i2s_tx_dma);

#ifdef I2S_DEBUG
    palClearPad(GPIOA, 1);
#endif

}


void i2s_peripheral_init(void)  {

    /* Release I2S3 */
    palSetPadMode(I2S3_WS_PORT, I2S3_WS_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_BCLK_PORT, I2S3_BCLK_PIN, PAL_MODE_INPUT);
    //palSetPadMode(I2S3_MCLK_PORT, I2S3_MCLK_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_SDIN_PORT, I2S3_SDIN_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_SDOUT_PORT, I2S3_SDOUT_PIN, PAL_MODE_INPUT);

    /* Enable SPI3 clock */
    rccEnableSPI3(false);


#if BOARD_KSOLOTI_CORE_H743
    SPI3->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_2 | SPI_I2SCFGR_DATLEN_1;
#else
    /* Configure I2S peripheral */
    /* I2S3: slave transmit, Philips standard, 32-bit data length (32-bit channel length is set implicitly by data length) */
    SPI3->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_DATLEN_1;
    /* I2S3ext: slave receive, Philips standard, 32-bit data length (32-bit channel length is set implicitly by data length) */
    I2S3ext->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_0 | SPI_I2SCFGR_DATLEN_1;
#endif

    /* Reassign I2S3 */
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
        STM32_DMA_CR_TEIE;
        //STM32_DMA_CR_TCIE;

    i2s_tx_dma = dmaStreamAlloc( STM32_SPI_I2S3_TX_DMA_STREAM, STM32_SPI_I2S3_IRQ_PRIORITY, (stm32_dmaisr_t)dma_i2s_tx_interrupt, (void *)0);

#if BOARD_KSOLOTI_CORE_H743
    dmaStreamSetPeripheral(i2s_tx_dma, &(SPI3->TXDR));
#else
    dmaStreamSetPeripheral(i2s_tx_dma, &(SPI3->DR));
#endif
    dmaStreamSetMemory0(i2s_tx_dma, i2s_buf);
    dmaStreamSetMemory1(i2s_tx_dma, i2s_buf2);
    dmaStreamSetTransactionSize(i2s_tx_dma, 64);
    dmaStreamSetMode(i2s_tx_dma, i2s_tx_dma_mode);

    i2s_rx_dma = STM32_DMA_STREAM(STM32_SPI_I2S3_RX_DMA_STREAM);

    uint32_t i2s_rx_dma_mode =
        STM32_DMA_CR_CHSEL(STM32_I2S3EXT_RX_DMA_CHANNEL) |
        STM32_DMA_CR_PL(STM32_SPI_I2S3_DMA_PRIORITY) |
        STM32_DMA_CR_DBM /* double buffer mode */ |
        STM32_DMA_CR_DIR_P2M |
        STM32_DMA_CR_MINC |
        STM32_DMA_CR_MSIZE_WORD |
        STM32_DMA_CR_PSIZE_HWORD |
        STM32_DMA_CR_TEIE;
        //STM32_DMA_CR_TCIE;

    i2s_rx_dma = dmaStreamAlloc( STM32_SPI_I2S3_RX_DMA_STREAM, STM32_SPI_I2S3_IRQ_PRIORITY, (stm32_dmaisr_t)0, (void *)0);

    if (!i2s_rx_dma || !i2s_tx_dma ) {
        setErrorFlag(ERROR_CODEC_I2C);
        while (1);
    }

#if BOARD_KSOLOTI_CORE_H743
    dmaStreamSetPeripheral(i2s_rx_dma, &(SPI3->RXDR));
#else
    dmaStreamSetPeripheral(i2s_rx_dma, &(I2S3ext->DR));
#endif

    dmaStreamSetMemory0(i2s_rx_dma, i2s_rbuf);
    dmaStreamSetMemory1(i2s_rx_dma, i2s_rbuf2);
    dmaStreamSetTransactionSize(i2s_rx_dma, 64);
    dmaStreamSetMode(i2s_rx_dma, i2s_rx_dma_mode);

}


void i2s_init(void) {

    i2s_peripheral_init();

    i2s_dma_init();

    /* Enable DMA streams */
    dmaStreamClearInterrupt(i2s_tx_dma);
    dmaStreamEnable(i2s_tx_dma);
    dmaStreamClearInterrupt(i2s_rx_dma);
    dmaStreamEnable(i2s_rx_dma);

    /* Enable DMA, I2S */
#if BOARD_KSOLOTI_CORE_H743
    SPI3->CFG1 = SPI_CFG1_TXDMAEN | SPI_CFG1_RXDMAEN;
    // SPI3->I2SCFGR |= SPI_I2SCFGR_I2SE;
#else
    SPI3->CR2 = SPI_CR2_TXDMAEN;
    I2S3ext->CR2 = SPI_CR2_RXDMAEN;
    SPI3->I2SCFGR |= SPI_I2SCFGR_I2SE;
    I2S3ext->I2SCFGR |= SPI_I2SCFGR_I2SE;
#endif

}


#endif /* FW_I2SCODEC */