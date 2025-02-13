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

#define STM32_I2S3_TX_DMA_STREAM STM32_DMA_STREAM_ID(1, 7)
#define STM32_I2S3_RX_DMA_STREAM STM32_DMA_STREAM_ID(1, 0)

#define I2S3_TX_DMA_CHANNEL (STM32_DMA_GETCHANNEL(STM32_I2S3_TX_DMA_STREAM, STM32_SPI3_TX_DMA_CHN))
#define I2S3_RX_DMA_CHANNEL (STM32_DMA_GETCHANNEL(STM32_I2S3_RX_DMA_STREAM, STM32_SPI3_RX_DMA_CHN))

#define STM32_I2S_TX_DMA_PRIORITY 1
#define STM32_I2S_RX_DMA_PRIORITY 1
#define STM32_I2S_TX_IRQ_PRIORITY 2
#define STM32_I2S_RX_IRQ_PRIORITY 2

#define I2S3_WS_PORT GPIOA
#define I2S3_WS_PIN 15
#define I2S3_BCLK_PORT GPIOB
#define I2S3_BCLK_PIN 3
#define I2S3_SDOUT_PORT GPIOD
#define I2S3_SDOUT_PIN 6
#define I2S3_SDIN_PORT GPIOB
#define I2S3_SDIN_PIN 4

const stm32_dma_stream_t* i2s_tx_dma;
const stm32_dma_stream_t* i2s_rx_dma;

uint32_t i2s_interrupt_timestamp;


typedef enum {
    falling=0,
    rising=1
} edge_t;

void wait_codec_fsync(edge_t edge) {
    while (1) {
        /* sync on codec LRCLK = WS. */
        volatile int i,j;

        j = 1000000; /* wait till LRCLK is low (or already is) */
        while(--j) {
            if (edge ^ !palReadPad(SAI1_FS_PORT, SAI1_FS_PIN))
                break;
        }

        i = 1000000; /* wait till LRCLK is high */
        while(--i) {
            if (edge ^ palReadPad(SAI1_FS_PORT, SAI1_FS_PIN))
                break;
        }

        j = 1000000; /* wait till LRCLK is low again */
        while(--j) {
            if (edge ^ !palReadPad(SAI1_FS_PORT, SAI1_FS_PIN))
                break;
        }

        break; /* if pulse edge found or not, leave this loop and function. */
    }
}

void i2S_computebufI(int32_t* inp, int32_t* outp) {
    uint_fast8_t i; for (i = 0; i < 32; i++) {
        inbuf[i] = inp[i];
    }

    outbuf = outp;
}


static void dma_i2s_interrupt(void* dat, uint32_t flags) {

    (void)dat;
    (void)flags;
    i2s_interrupt_timestamp = hal_lld_get_counter_value();

    if ((i2s_tx_dma)->stream->CR & STM32_DMA_CR_CT) {
        i2s_computebufI(i2s_rbuf2, i2s_buf);
    }
    else {
        i2s_computebufI(i2s_rbuf, i2s_buf2);
    }

    dmaStreamClearInterrupt(i2s_tx_dma);
}


void i2s_hw_init(void) {

    dmaStreamClearInterrupt(i2s_rx_dma);
    dmaStreamEnable(i2s_rx_dma);

    dmaStreamClearInterrupt(i2s_tx_dma);
    dmaStreamEnable(i2s_tx_dma);

    chSysLock();
    wait_codec_fsync();
    chSysUnlock();

    I2S3->I2SCFGR    = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_1; /* I2S master transmit, Philips standard, 16-bit data and channel length */
    I2S3ext->I2SCFGR = SPI_I2SCFGR_I2SMOD | SPI_I2SCFGR_I2SCFG_0; /* I2S slave receive, Philips standard, 16-bit data and channel length */

    I2S3->I2SPR = 0x0C | SPI_I2SPR_ODD;
    // I2S3ext->I2SPR = 0x0C | SPI_I2SPR_ODD; /* not used in slave mode */

    chThdSleepMilliseconds(1);
}


void i2s_periph_init() {

    /* release I2S3 */
    palSetPadMode(I2S3_WS_PORT, I2S3_WS_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_BCLK_PORT, I2S3_BCLK_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_SDIN_PORT, I2S3_SDIN_PIN, PAL_MODE_INPUT);
    palSetPadMode(I2S3_SDOUT_PORT, I2S3_SDOUT_PIN, PAL_MODE_INPUT);

    i2s_hw_init();

    /* configure I2S */

    chThdSleepMilliseconds(1);

    chThdSleepMilliseconds(1);

    /* reassign I2S3 */
    palSetPadMode(I2S3_WS_PORT, I2S3_WS_PIN, PAL_MODE_ALTERNATE());
    palSetPadMode(I2S3_BCLK_PORT, I2S3_BCLK_PIN, PAL_MODE_ALTERNATE());
    palSetPadMode(I2S3_SDIN_PORT, I2S3_SDIN_PIN, PAL_MODE_ALTERNATE());
    palSetPadMode(I2S3_SDOUT_PORT, I2S3_SDOUT_PIN, PAL_MODE_ALTERNATE());

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

#ifdef FW_SPILINK
    if  (isMaster) {
        b = dmaStreamAllocate(sai_a_dma, STM32_SAI_A_IRQ_PRIORITY,
                              (stm32_dmaisr_t)dma_sai_a_interrupt_spilink_master, (void *)0);
    }
    else {
        b = dmaStreamAllocate(sai_a_dma, STM32_SAI_A_IRQ_PRIORITY,
                              (stm32_dmaisr_t)dma_sai_a_interrupt_spilink_slave, (void *)0);
    }

#else
    b = dmaStreamAllocate(sai_a_dma, STM32_SAI_A_IRQ_PRIORITY, (stm32_dmaisr_t)dma_sai_a_interrupt, (void *)0);

#endif

    dmaStreamSetPeripheral(sai_a_dma, &(sai_a->DR));
    dmaStreamSetMemory0(sai_a_dma, buf);
    dmaStreamSetMemory1(sai_a_dma, buf2);
    dmaStreamSetTransactionSize(sai_a_dma, 32);
    dmaStreamSetMode(sai_a_dma, sai_a_dma_mode | STM32_DMA_CR_MINC);


    b |= dmaStreamAllocate(sai_b_dma, STM32_SAI_B_IRQ_PRIORITY, (stm32_dmaisr_t)0, (void *)0);

    if (b) {
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

#ifdef FW_SPILINK
    if (isMaster) {
        chSysLock();
        SAI1_Block_A->CR2 |= SAI_xCR2_FFLUSH;
        SAI1_Block_B->CR2 |= SAI_xCR2_FFLUSH;
        SAI1_Block_A->DR = 0;
        SAI1_Block_B->DR = 0;
        dmaStreamEnable(sai_b_dma);
        dmaStreamEnable(sai_a_dma);
        SAI1_Block_B->CR1 |= SAI_xCR1_SAIEN;
        SAI1_Block_A->CR1 |= SAI_xCR1_SAIEN;
        /* 2.25 us offset between dmarx and dmatx */
        chSysUnlock();
    }
    else {
        chSysLock();
        SAI1_Block_A->CR2 |= SAI_xCR2_FFLUSH;
        SAI1_Block_B->CR2 |= SAI_xCR2_FFLUSH;
        SAI1_Block_A->DR = 0;
        SAI1_Block_B->DR = 0;
        wait_SPI_fsync(rising);
        dmaStreamEnable(sai_b_dma);
        dmaStreamEnable(sai_a_dma);
        SAI1_Block_B->CR1 |= SAI_xCR1_SAIEN;
        SAI1_Block_A->CR1 |= SAI_xCR1_SAIEN;
        chSysUnlock();
    }
#else
    dmaStreamEnable(sai_b_dma);
    dmaStreamEnable(sai_a_dma);

    SAI1_Block_A->CR1 |= SAI_xCR1_SAIEN;
    SAI1_Block_B->CR1 |= SAI_xCR1_SAIEN;

#endif

}
