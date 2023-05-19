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

/*
* Adapted from pdm_CS43L22.c
* Created on: Jun 7, 2012
* Author: Kumar Abhishek
*/

#include "pdm.h"
#include "ch.h"
#include "hal.h"

#include "codec.h"
#include "stm32f4xx.h"
#include "axoloti_board.h"
#include "sysmon.h"

extern void computebuf_PDM_I(int32_t *inp);


const stm32_dma_stream_t* pdm_i2s_dma_tx;
const stm32_dma_stream_t* pdm_i2s_dma_rx;
static uint32_t i2s_dma_tx_mode = 0;
static uint32_t i2s_dma_rx_mode = 0;


static void pdm_dma_i2s_tx_interrupt(void* dat, uint32_t flags)
{
    (void)dat;
    (void)flags;
    dmaStreamClearInterrupt(pdm_i2s_dma_tx);
}

static void pdm_dma_i2s_rx_interrupt(void* dat, uint32_t flags)
{
    (void)dat;
    (void)flags;
    if ((pdm_i2s_dma_tx)->stream->CR & STM32_DMA_CR_CT)
    {
        computebuf_PDM_I(pdm_rbuf);
    }
    else
    {
        computebuf_PDM_I(pdm_rbuf2);
    }
    dmaStreamClearInterrupt(pdm_i2s_dma_rx);
}

static void pdm_dma_init(void)
{
    // TX
    // pdm_i2s_dma_tx = STM32_DMA_STREAM(STM32_SPI_SPI3_TX_DMA_STREAM);

    // i2s_dma_tx_mode = STM32_DMA_CR_CHSEL(I2S3_TX_DMA_CHANNEL)
    // | STM32_DMA_CR_PL(STM32_SPI_SPI3_DMA_PRIORITY) | STM32_DMA_CR_DIR_M2P
    // | STM32_DMA_CR_TEIE | STM32_DMA_CR_TCIE | STM32_DMA_CR_DBM | // double buffer mode
    // STM32_DMA_CR_PSIZE_WORD | STM32_DMA_CR_MSIZE_WORD;

    // bool_t b = dmaStreamAllocate(pdm_i2s_dma_tx, STM32_SPI_SPI3_IRQ_PRIORITY,
    //                              (stm32_dmaisr_t)pdm_dma_i2s_tx_interrupt,
    //                              (void *)&SPID3);

    // dmaStreamSetPeripheral(pdm_i2s_dma_tx, &(PDM_I2S->DR));
    // dmaStreamSetMemory0(pdm_i2s_dma_tx, buf);
    // dmaStreamSetMemory1(pdm_i2s_dma_tx, buf2);
    // dmaStreamSetTransactionSize(pdm_i2s_dma_tx, 64);
    // dmaStreamSetMode(pdm_i2s_dma_tx, i2s_dma_tx_mode | STM32_DMA_CR_MINC);
    // dmaStreamClearInterrupt(pdm_i2s_dma_tx);
    // dmaStreamEnable(pdm_i2s_dma_tx)

    // RX
    pdm_i2s_dma_rx = STM32_DMA_STREAM(STM32_SPI_SPI3_RX_DMA_STREAM);
    i2s_dma_rx_mode = STM32_DMA_CR_CHSEL(I2S3ext_RX_DMA_CHANNEL)
    | STM32_DMA_CR_PL(STM32_SPI_SPI3_DMA_PRIORITY) | STM32_DMA_CR_DIR_P2M
    | STM32_DMA_CR_TEIE | STM32_DMA_CR_TCIE | STM32_DMA_CR_DBM | // double buffer mode
    STM32_DMA_CR_PSIZE_WORD | STM32_DMA_CR_MSIZE_WORD;

    bool_t b |= dmaStreamAllocate(pdm_i2s_dma_rx, STM32_SPI_SPI3_IRQ_PRIORITY,
                                 (stm32_dmaisr_t)pdm_dma_i2s_rx_interrupt,
                                 (void *)&SPID3);

    // Show error if DMA allocation fails
    if (b) { setErrorFlag(ERROR_CODEC_I2C); /* using arbitrary error flag here */ }

    dmaStreamSetPeripheral(pdm_i2s_dma_rx, &(PDM_I2Sext->DR));
    dmaStreamSetMemory0(pdm_i2s_dma_rx, pdm_rbuf);
    dmaStreamSetMemory1(pdm_i2s_dma_rx, pdm_rbuf2);
    dmaStreamSetTransactionSize(pdm_i2s_dma_rx, 64);
    dmaStreamSetMode(pdm_i2s_dma_rx, i2s_dma_rx_mode | STM32_DMA_CR_MINC);
    dmaStreamClearInterrupt(pdm_i2s_dma_rx);
    dmaStreamEnable(pdm_i2s_dma_rx);
}

void pdm_i2s_init_48k(void)
{
    // palSetPadMode(GPIOA, 15, PAL_MODE_OUTPUT_PUSHPULL); //i2s3_ws
    // palSetPadMode(GPIOA, 15, PAL_MODE_ALTERNATE(6)); //i2s3_ws
    palSetPadMode(GPIOB, 3, PAL_MODE_ALTERNATE(6)); //i2s3_ck
    palSetPadMode(GPIOB, 4, PAL_MODE_ALTERNATE(7)); //i2s3ext_sd
    // palSetPadMode(GPIOB, 5, PAL_MODE_ALTERNATE(6)); //i2s3_sd

    // SPI3 in I2S Mode, Master
    PDM_I2S_ENABLE;
    PDM_I2S->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_1
    | SPI_I2SCFGR_DATLEN_1;
    PDM_I2S->I2SPR = /*SPI_I2SPR_MCKOE | SPI_I2SPR_ODD |*/ 14; // divider should be ODD | 3 if  MCKOE is set, 14 otherwise


    PDM_I2Sext ->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_0 | SPI_I2SCFGR_DATLEN_1; /* SLAVE RECEIVE*/
    PDM_I2Sext ->I2SPR = 0x0002;

    pdm_dma_init();

    /* Paste the code block below in the "Init Code" of the object you want the PDM mic to be available in:

    // Start DMA for I2S3 and I2S3ext
    PDM_I2S->CR2 = SPI_CR2_TXDMAEN; 
    PDM_I2Sext->CR2 = SPI_CR2_RXDMAEN;
    chThdSleepMilliseconds(1);
    // Start I2S3 and I2S3ext
    PDM_I2S->I2SCFGR |= SPI_I2SCFGR_I2SE;
    PDM_I2Sext->I2SCFGR |= SPI_I2SCFGR_I2SE;

    */


    /* Paste the code block below in the "Dispose" Code of your  object:

    // Stop TX DMA and I2S3
    PDM_I2Sext->CR2 = 0;
    PDM_I2Sext->I2SCFGR &= ~(SPI_I2SCFGR_I2SE);
    // Stop RX DMA and I2S3ext
    PDM_I2S->CR2 = 0;
    PDM_I2S->I2SCFGR &= ~(SPI_I2SCFGR_I2SE);

    */

}
