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

#include <stdint.h>
#include "axoloti_defines.h"
#include "i2s.h"
#include "ch.h"
#include "hal.h"
#include "sysmon.h"
#include "stm32f4xx.h"
#include "stm32f427xx.h"

#ifdef FW_I2S

#define I2S_DEBUG

#define STM32_I2S3_TX_DMA_CHANNEL (STM32_DMA_GETCHANNEL(STM32_SPI_SPI3_TX_DMA_STREAM, STM32_SPI3_TX_DMA_CHN))
#define STM32_I2S3_RX_DMA_CHANNEL (STM32_DMA_GETCHANNEL(STM32_SPI_SPI3_RX_DMA_STREAM, STM32_SPI3_RX_DMA_CHN))

#define STM32_I2S_TX_DMA_PRIORITY 1
#define STM32_I2S_RX_DMA_PRIORITY 1
#define STM32_I2S_TX_IRQ_PRIORITY 3
#define STM32_I2S_RX_IRQ_PRIORITY 3

/* Required by wait_sai_dma_tc_sync(): access to SAI DMA's transfer complete flag */
#define STM32_SAI_A_DMA_STREAM STM32_DMA_STREAM_ID(2, 1)

#define I2S3_WS_PORT GPIOA
#define I2S3_WS_PIN 15
#define I2S3_BCLK_PORT GPIOB
#define I2S3_BCLK_PIN 3
// #define I2S3_MCLK_PORT GPIOC
// #define I2S3_MCLK_PIN 7
#define I2S3_SDOUT_PORT GPIOD
#define I2S3_SDOUT_PIN 6
#define I2S3_SDIN_PORT GPIOB
#define I2S3_SDIN_PIN 4

#define SAI1_FS_PORT GPIOE
#define SAI1_FS_PIN 4

const stm32_dma_stream_t* i2s_tx_dma;
const stm32_dma_stream_t* i2s_rx_dma;


uint32_t i2s_interrupt_timestamp;

int32_t i2s_buf[BUFSIZE*2]   __attribute__ ((section (".sram2")));
int32_t i2s_buf2[BUFSIZE*2]  __attribute__ ((section (".sram2")));
int32_t i2s_rbuf[BUFSIZE*2]  __attribute__ ((section (".sram2")));
int32_t i2s_rbuf2[BUFSIZE*2] __attribute__ ((section (".sram2")));

typedef enum {
    falling=0,
    rising=1
} edge_t;


void wait_sai_fsync(edge_t edge) {
    /* sync on ADAU codec LRCLK = WS. */
    while (1) {
        volatile int i, j, k;
        k = 427; /* magic number found through trial and error - see below */

        i = 1000000;
        while(--i) {
            /* wait till LRCLK goes low (or already is) */
            if (edge ^ !palReadPad(SAI1_FS_PORT, SAI1_FS_PIN)) {
                break;
            }
        }

        j = 1000000;
        while(--j) {
            /* wait till LRCLK goes high */
            if (edge ^ palReadPad(SAI1_FS_PORT, SAI1_FS_PIN)) {
                break;
            }
        }

        i = 1000000;
        while(--i) {
            /* wait till LRCLK goes low again */
            if (edge ^ !palReadPad(SAI1_FS_PORT, SAI1_FS_PIN)) {
                /* When turning the I2S on after syncing it in chSysLock state, its WS
                 * and other clocks  will run ca. 1 us late compared to the SAI.
                 * This is actually not a problem but might be at high patch load?
                 * Enter the k hack:
                 * We make the MCU spin and waste time until the next WS cycle,
                 * making it run so late it's punctual again.
                 * Somewhere between 10 and 50 ns now.
                 */
                while(--k) {
                    __asm("nop");
                }
                break;
            }
        }
        break; /* time out if pulse edge not found */
    }
}


void wait_sai_dma_tc_sync(void) {
    /* wait for SAI DMA transfer complete flag */
    volatile int i = 100000000;

    while (--i) {
        if ((STM32_DMA_STREAM(STM32_SAI_A_DMA_STREAM))->stream->CR & STM32_DMA_CR_CT) {
            break;
        }
        //if (DMA2_Stream1->NDTR == 0) {
        //    break;
        //}
    }
    /* Or time out */
}


static void dma_i2s_tx_interrupt(void* dat, uint32_t flags) {

    /* This interrupt is not being used, using SAI interrupt instead */
    (void) dat;
    (void) flags;
    i2s_interrupt_timestamp = hal_lld_get_counter_value();

    if ((i2s_tx_dma)->stream->CR & STM32_DMA_CR_CT) {
#ifdef I2S_DEBUG
        palSetPad(GPIOA, 1);
        __asm("nop");
        __asm("nop");
        __asm("nop");
        __asm("nop");
        __asm("nop");
        __asm("nop");
        palClearPad(GPIOA, 1);
#endif
    }
    dmaStreamClearInterrupt(i2s_tx_dma);
}

static void dma_i2s_rx_interrupt(void* dat, uint32_t flags) {
    (void) dat;
    (void) flags;
    dmaStreamClearInterrupt(i2s_rx_dma);
}

void i2s_peripheral_init(void)  {

#ifdef I2S_DEBUG
    palSetPadMode(GPIOA, 1, PAL_MODE_OUTPUT_PUSHPULL);
#endif

    rccEnableSPI3(false);
    /* configure I2S peripheral */
    SPI3->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_1 | SPI_I2SCFGR_DATLEN_1; /* I2S master transmit, Philips standard, 32-bit data length, 32-bit channel length */
    SPI3->I2SPR = 0x000C | SPI_I2SPR_ODD;

    I2S3ext->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_0 | SPI_I2SCFGR_DATLEN_1; /* I2S slave receive, Philips standard, 32-bit data length, 32-bit channel length */
    I2S3ext->I2SPR = 0x0002; // 0x000C | SPI_I2SPR_ODD; /* not used in slave mode */

    /* release I2S3 */
    palSetPadMode(I2S3_WS_PORT, I2S3_WS_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_BCLK_PORT, I2S3_BCLK_PIN, PAL_MODE_INPUT);
    //palSetPadMode(I2S3_MCLK_PORT, I2S3_MCLK_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_SDIN_PORT, I2S3_SDIN_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_SDOUT_PORT, I2S3_SDOUT_PIN, PAL_MODE_INPUT);


    /* reassign I2S3 */
    palSetPadMode(I2S3_WS_PORT, I2S3_WS_PIN, PAL_MODE_ALTERNATE(6));
    palSetPadMode(I2S3_BCLK_PORT, I2S3_BCLK_PIN, PAL_MODE_ALTERNATE(6));
    // palSetPadMode(I2S3_MCLK_PORT, I2S3_MCLK_PIN, PAL_MODE_ALTERNATE(6));
    palSetPadMode(I2S3_SDIN_PORT, I2S3_SDIN_PIN, PAL_MODE_ALTERNATE(7));
    palSetPadMode(I2S3_SDOUT_PORT, I2S3_SDOUT_PIN, PAL_MODE_ALTERNATE(5));

}


void i2s_dma_init(void) {

    /* initialize DMA */
    i2s_tx_dma = STM32_DMA_STREAM(STM32_SPI_SPI3_TX_DMA_STREAM);

    uint32_t i2s_tx_dma_mode =
        STM32_DMA_CR_CHSEL(STM32_I2S3_TX_DMA_CHANNEL) |
        STM32_DMA_CR_PL(STM32_I2S_TX_DMA_PRIORITY) |
        STM32_DMA_CR_DBM /* double buffer mode */ |
        STM32_DMA_CR_DIR_M2P |
        STM32_DMA_CR_MINC |
        STM32_DMA_CR_MSIZE_WORD |
        STM32_DMA_CR_PSIZE_WORD |
        STM32_DMA_CR_TEIE |
        STM32_DMA_CR_TCIE;

    bool_t b = dmaStreamAllocate(i2s_tx_dma, STM32_I2S_TX_DMA_PRIORITY, (stm32_dmaisr_t) dma_i2s_tx_interrupt, (void*) 0);

    dmaStreamSetMode(i2s_tx_dma, i2s_tx_dma_mode);
    dmaStreamSetPeripheral(i2s_tx_dma, &(SPI3->DR));
    dmaStreamSetMemory0(i2s_tx_dma, i2s_buf);
    dmaStreamSetMemory1(i2s_tx_dma, i2s_buf2);
    dmaStreamSetTransactionSize(i2s_tx_dma, 64);
    dmaStreamClearInterrupt(i2s_tx_dma);

    i2s_rx_dma = STM32_DMA_STREAM(STM32_SPI_SPI3_RX_DMA_STREAM);

    uint32_t i2s_rx_dma_mode =
        STM32_DMA_CR_CHSEL(STM32_I2S3_RX_DMA_CHANNEL) |
        STM32_DMA_CR_PL(STM32_I2S_RX_DMA_PRIORITY) |
        STM32_DMA_CR_DBM /* double buffer mode */ |
        STM32_DMA_CR_DIR_P2M |
        STM32_DMA_CR_MINC |
        STM32_DMA_CR_MSIZE_WORD |
        STM32_DMA_CR_PSIZE_WORD;
        //STM32_DMA_CR_TEIE |
        //STM32_DMA_CR_TCIE;

    b |= dmaStreamAllocate(i2s_rx_dma, STM32_I2S_RX_DMA_PRIORITY, (stm32_dmaisr_t) 0, (void*) 0);

    if (b) {
        setErrorFlag(ERROR_CODEC_I2C);
        while (1);
    }

    dmaStreamSetMode(i2s_rx_dma, i2s_rx_dma_mode);
    dmaStreamSetPeripheral(i2s_rx_dma, &(I2S3ext->DR));
    dmaStreamSetMemory0(i2s_rx_dma, i2s_rbuf);
    dmaStreamSetMemory1(i2s_rx_dma, i2s_rbuf2);
    dmaStreamSetTransactionSize(i2s_rx_dma, 64);
    dmaStreamClearInterrupt(i2s_rx_dma);

}


void i2s_init(void) {

    i2s_peripheral_init();

    i2s_dma_init();

    /* sync I2S WS falling edge with SAI (codec) WS falling edge (will sync to around 10-50 ns) */
    //wait_sai_fsync(falling);
    //wait_sai_dma_tc_sync();
    chSysLock();
    dmaStreamEnable(i2s_tx_dma);
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

#endif /* FW_I2S */