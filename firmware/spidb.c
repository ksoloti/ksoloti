/*
 * Copyright (C) 2016 Johannes Taelman
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

#include "axoloti_board.h"
#include "ch.h"
#include "halconf.h"
#include "mcuconf.h"
#include "hal.h"
#include "stm32_dma.h"

#include "spidb.h"
#include <stdint.h>

#ifdef FW_SPILINK


uint32_t spidb_interrupt_timestamp;

// #define DEBUG_SPIDB_INT_ON_GPIO 1


static void dma_spidb_slave_interrupt(void* dat, uint32_t flags) {
    SPIDriver *spip = dat;
    spidb_interrupt_timestamp = hal_lld_get_counter_value();

    if (flags & STM32_DMA_ISR_TCIF) {
        chSysLockFromIsr();

#ifdef DEBUG_SPIDB_INT_ON_GPIO
        palSetPadMode(GPIOA, 1, PAL_MODE_OUTPUT_PUSHPULL);
        palSetPad(GPIOA, 1);
#endif

        chEvtSignalI(spip->sync_transfer, full_transfer_complete);

#ifdef DEBUG_SPIDB_INT_ON_GPIO
        palClearPad(GPIOA, 1);
#endif

        chSysUnlockFromIsr();
    }
    else if (flags & STM32_DMA_ISR_HTIF) {
        chSysLockFromIsr();

#ifdef DEBUG_SPIDB_INT_ON_GPIO
        palSetPadMode(GPIOA, 2, PAL_MODE_OUTPUT_PUSHPULL);
        palSetPad(GPIOA, 2);
#endif

        chEvtSignalI(spip->sync_transfer, half_transfer_complete);

#ifdef DEBUG_SPIDB_INT_ON_GPIO
        palClearPad(GPIOA, 2);
#endif

        chSysUnlockFromIsr();
    }
    else if (flags & STM32_DMA_ISR_TEIF) {
//        chSysHalt("spidb:TEIF");
    }
}


void dmastream_slave_start(SPIDriver *spip) {

#if BOARD_KSOLOTI_CORE_H743
    dmaStreamSetMemory0(spip->rx.dma, ((SPIDBConfig *) (spip->config))->rxbuf);
    dmaStreamSetTransactionSize(spip->rx.dma, ((SPIDBConfig *) (spip->config))->size * 2);
    dmaStreamSetMode(spip->rx.dma, spip->rxdmamode | STM32_DMA_CR_MINC | STM32_DMA_CR_CIRC | STM32_DMA_CR_HTIE);

    dmaStreamSetMemory0(spip->tx.dma, ((SPIDBConfig *) (spip->config))->txbuf);
    dmaStreamSetTransactionSize(spip->tx.dma, ((SPIDBConfig *) (spip->config))->size * 2);
    dmaStreamSetMode(spip->tx.dma, spip->txdmamode | STM32_DMA_CR_MINC | STM32_DMA_CR_CIRC);
#else
    dmaStreamSetMemory0(spip->dmarx, ((SPIDBConfig *) (spip->config))->rxbuf);
    dmaStreamSetTransactionSize(spip->dmarx, ((SPIDBConfig *) (spip->config))->size * 2);
    dmaStreamSetMode(spip->dmarx, spip->rxdmamode | STM32_DMA_CR_MINC | STM32_DMA_CR_CIRC | STM32_DMA_CR_HTIE);

    dmaStreamSetMemory0(spip->dmatx, ((SPIDBConfig *) (spip->config))->txbuf);
    dmaStreamSetTransactionSize(spip->dmatx, ((SPIDBConfig *) (spip->config))->size * 2);
    dmaStreamSetMode(spip->dmatx, spip->txdmamode | STM32_DMA_CR_MINC | STM32_DMA_CR_CIRC);
#endif

    chSysLock();
    /* Wait till not selected */
    while(!palReadPad(spip->config->ssport,spip->config->sspad));

    spip->spi->CR1 |= SPI_CR1_SPE;

#if BOARD_KSOLOTI_CORE_H743
    // TODO SPILINK_H7

    dmaStreamEnable(spip->rx.dma);
    dmaStreamEnable(spip->tx.dma);
#else

    /* Wait till not busy */
    while (spip->spi->SR & SPI_SR_BSY);

    dmaStreamEnable(spip->dmarx);
    dmaStreamEnable(spip->dmatx);
#endif
    chSysUnlock();
}


void spidbSlaveResync(SPIDriver *spip) {
    palSetPad(LED2_PORT, LED2_PIN);

#if BOARD_KSOLOTI_CORE_H743
    dmaStreamDisable(spip->tx.dma);

    // TODO SPILINK_H7
    spip->spi->CR1 &= ~SPI_CR1_SPE;

    dmaStreamDisable(spip->rx.dma);
#else
    dmaStreamDisable(spip->dmatx);

    /* Wait till buffer is empty */
    while (!(spip->spi->SR & SPI_SR_TXE));

    /* Wait till transfer is done */
    while (spip->spi->SR & SPI_SR_BSY);

    spip->spi->CR1 &= ~SPI_CR1_SPE;

    dmaStreamDisable(spip->dmarx);
#endif
    dmastream_slave_start(spip);
    palClearPad(LED2_PORT, LED2_PIN);
}


/**
 * @brief   Configures and activates the SPI peripheral
 * for slave mode, dual buffer-swapping
 *
 * @param[in] spip      pointer to the @p SPIDriver object
 * @param[in] config    pointer to the @p SPIDBConfig configuration
 *
 */
void spidbSlaveStart(SPIDriver *spip, const SPIDBConfig *config, Thread * thread) {
    spiStart(spip, &config->spiconfig);

    spip->sync_transfer = thread;

#if BOARD_KSOLOTI_CORE_H743
    spip->spi->CR1 &= ~SPI_CR1_SPE;
    // TODO SPILINK_H7

    dmaStreamFreeI(spip->rx.dma);
    dmaStreamFreeI(spip->tx.dma);
#else
    spip->spi->CR1 &= ~SPI_CR1_SPE;
    spip->spi->CR1 &= ~SPI_CR1_MSTR;
    spip->spi->CR1 &= ~SPI_CR1_SSM;
    spip->spi->CR1 &= ~SPI_CR1_SSI;

    dmaStreamFree(spip->dmarx);
    dmaStreamFree(spip->dmatx);
#endif

    int irq_priority = -1;

#if STM32_SPI_USE_SPI1
    if (spip == &SPID1)
        irq_priority = STM32_SPI_SPI1_IRQ_PRIORITY;
#endif

#if STM32_SPI_USE_SPI2
    if (spip == &SPID2)
        irq_priority = STM32_SPI_SPI2_IRQ_PRIORITY;
#endif

#if STM32_SPI_USE_SPI3
    if (spip == &SPID3)
        irq_priority = STM32_SPI_SPI3_IRQ_PRIORITY;
#endif

    if (irq_priority == -1)
        chSysHalt("spidbSlaveStart wrong irq_priority");

#if BOARD_KSOLOTI_CORE_H743
    spip->rx.dma = dmaStreamAllocI( STM32_SPI_SPI3_RX_DMA_STREAM,
                                    irq_priority,
                                    (stm32_dmaisr_t)dma_spidb_slave_interrupt,
                                    (void *)spip);

    spip->tx.dma = dmaStreamAllocI( STM32_SPI_SPI3_TX_DMA_STREAM,
                                    irq_priority,
                                    (stm32_dmaisr_t)0,
                                    (void *)spip);
#else
    spip->dmarx = dmaStreamAlloc( STM32_SPI_SPI3_RX_DMA_STREAM,
                                  irq_priority,
                                  (stm32_dmaisr_t)dma_spidb_slave_interrupt,
                                  (void *)spip);

    spip->dmatx = dmaStreamAlloc( STM32_SPI_SPI3_TX_DMA_STREAM,
                                  irq_priority,
                                  (stm32_dmaisr_t)0,
                                  (void *)spip);
#endif
  
    // bool_t b = dmaStreamAllocate(spip->dmarx, irq_priority, (stm32_dmaisr_t) dma_spidb_slave_interrupt, (void *) spip);
    // chDbgAssert(!b, "spi_lld_start(), #1 stream already allocated");

    // b = dmaStreamAllocate(spip->dmatx, irq_priority, (stm32_dmaisr_t) 0, (void *) spip);
    // chDbgAssert(!b, "spi_lld_start(), #2 stream already allocated");

    spiSelect(spip);

    dmastream_slave_start(spip);
}


static void dma_spidb_master_interrupt(void* dat, uint32_t flags) {
    (void) flags;

#ifdef DEBUG_SPIDB_INT_ON_GPIO
    palSetPadMode(GPIOA, 1, PAL_MODE_OUTPUT_PUSHPULL);
    palSetPad(GPIOA, 1);
#endif
    /* assume it is a transfer ready interrupt */
    SPIDriver *spip = dat;
    spidb_interrupt_timestamp = hal_lld_get_counter_value();

#if BOARD_KSOLOTI_CORE_H743
    dmaStreamDisable(spip->rx.dma);
    dmaStreamDisable(spip->tx.dma);
#else
    dmaStreamDisable(spip->dmarx);
    dmaStreamDisable(spip->dmatx);
#endif

    palSetPad(spip->config->ssport, spip->config->sspad);

#ifdef DEBUG_SPIDB_INT_ON_GPIO
    palClearPad(GPIOA, 1);
#endif
}


void spidbMasterStart(SPIDriver *spip, const SPIDBConfig *config) {
    int i; for (i = 0; i < config->size * 2; i++)
        config->rxbuf[i] = 0;

    spiStart(spip, &config->spiconfig);


#if BOARD_KSOLOTI_CORE_H743
    spip->spi->CR1 &= ~SPI_CR1_SPE;
    // TODO SPILINK_H7
    dmaStreamFreeI(spip->rx.dma);
    dmaStreamFreeI(spip->tx.dma);
#else
    spip->spi->CR1 &= ~SPI_CR1_SPE;
    spip->spi->CR1 &= ~SPI_CR1_SSM;
    spip->spi->CR1 &= ~SPI_CR1_SSI;
    spip->spi->CR1 |= SPI_CR1_MSTR;
    spip->spi->CR1 |= SPI_CR1_SPE;

    dmaStreamFree(spip->dmarx);
    dmaStreamFree(spip->dmatx);
#endif

    int irq_priority = -1;

#if STM32_SPI_USE_SPI1
    if (spip == &SPID1)
        irq_priority = STM32_SPI_SPI1_IRQ_PRIORITY;
#endif

#if STM32_SPI_USE_SPI2
    if (spip == &SPID2)
        irq_priority = STM32_SPI_SPI2_IRQ_PRIORITY;
#endif

#if STM32_SPI_USE_SPI3
    if (spip == &SPID3)
        irq_priority = STM32_SPI_SPI3_IRQ_PRIORITY;
#endif

    if (irq_priority == -1)
        chSysHalt("spidbMasterStart wrong irq_priority");

    spip->rxdmamode |= STM32_DMA_CR_MINC;
    spip->txdmamode |= STM32_DMA_CR_MINC;

#if BOARD_KSOLOTI_CORE_H743
    spip->rx.dma = dmaStreamAllocI( STM32_SPI_SPI3_RX_DMA_STREAM,
                                    irq_priority,
                                    (stm32_dmaisr_t)dma_spidb_master_interrupt,
                                    (void *)spip);

    spip->tx.dma = dmaStreamAllocI( STM32_SPI_SPI3_TX_DMA_STREAM,
                                    irq_priority,
                                    (stm32_dmaisr_t)0,
                                    (void *)spip);

    dmaStreamSetMemory0(spip->rx.dma, ((SPIDBConfig *) (spip->config))->rxbuf);
    dmaStreamSetTransactionSize(spip->rx.dma, ((SPIDBConfig *) (spip->config))->size);
    dmaStreamSetMode(spip->rx.dma, spip->rxdmamode );

    dmaStreamSetMemory0(spip->tx.dma, ((SPIDBConfig *) (spip->config))->txbuf);
    dmaStreamSetTransactionSize(spip->tx.dma, ((SPIDBConfig *) (spip->config))->size);
    dmaStreamSetMode(spip->tx.dma, spip->txdmamode );
#else
    spip->dmarx = dmaStreamAlloc( STM32_SPI_SPI3_RX_DMA_STREAM,
                                  irq_priority,
                                  (stm32_dmaisr_t)dma_spidb_master_interrupt,
                                  (void *)spip);

    spip->dmatx = dmaStreamAlloc( STM32_SPI_SPI3_TX_DMA_STREAM,
                                  irq_priority,
                                  (stm32_dmaisr_t)0,
                                  (void *)spip);

    dmaStreamSetMemory0(spip->dmarx, ((SPIDBConfig *) (spip->config))->rxbuf);
    dmaStreamSetTransactionSize(spip->dmarx, ((SPIDBConfig *) (spip->config))->size);
    dmaStreamSetMode(spip->dmarx, spip->rxdmamode );

    dmaStreamSetMemory0(spip->dmatx, ((SPIDBConfig *) (spip->config))->txbuf);
    dmaStreamSetTransactionSize(spip->dmatx, ((SPIDBConfig *) (spip->config))->size);
    dmaStreamSetMode(spip->dmatx, spip->txdmamode );
#endif
}


void spidbStop(SPIDriver *spip) {

    // WIP! currently buggy and apparently useless

    /* Only necessary if Core is synced */
    if (!palReadPad(SPILINK_JUMPER_PORT, SPILINK_JUMPER_PIN)) {
        chSysLock();

#if BOARD_KSOLOTI_CORE_H743
        dmaStreamDisable(spip->tx.dma);
        dmaStreamFreeI(spip->tx.dma);
        // TODO SPILINK_H7

        spip->spi->CR1 &= ~SPI_CR1_SPE;

        dmaStreamDisable(spip->rx.dma);
        dmaStreamFreeI(spip->rx.dma);
#else
        dmaStreamDisable(spip->dmatx);
        dmaStreamFree(spip->dmatx);

        /* Wait till buffer is empty */
        while (!(spip->spi->SR & SPI_SR_TXE));

        /* Wait till transfer is done */
        while (spip->spi->SR & SPI_SR_BSY);

        spip->spi->CR1 &= ~SPI_CR1_SPE;

        dmaStreamDisable(spip->dmarx);
        dmaStreamFree(spip->dmarx);
#endif

        spiStop(spip);
        chSysUnlock();
    }
}

#endif