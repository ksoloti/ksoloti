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

#ifndef SPIDB_H_
#define SPIDB_H_

#include "ch.h"
#include "hal.h"

#ifdef FW_SPILINK

/*
 * Double buffered periodic spi exchange
 */

typedef struct {
    SPIConfig spiconfig;
    uint8_t *rxbuf;
    uint8_t *txbuf;
    int size;
} SPIDBConfig;

typedef enum {
    half_transfer_complete=1,
    full_transfer_complete=2,
    other_transfer=4
} spidb_signal_t;

extern uint32_t spidb_interrupt_timestamp;

void spidbMasterStart(SPIDriver *spip, const SPIDBConfig *config);
void spidbSlaveStart(SPIDriver *spip, const SPIDBConfig *config, Thread * thread);
void spidbSlaveResync(SPIDriver *spip);
void spidbStop(SPIDriver *spip);


/* inline functions */
__STATIC_INLINE void spidbMasterExchangeI(SPIDriver *spip, bool_t toggle) {
    SPIDBConfig *config = (SPIDBConfig *)spip->config;

    uint32_t offset = toggle ? 0 : 2 * config->size; /* assumes 16 bit xfer */

    palClearPad(config->spiconfig.ssport, config->spiconfig.sspad);

#if BOARD_KSOLOTI_CORE_H743
    dmaStreamSetMemory0(spip->rx.dma, config->rxbuf + offset);
    dmaStreamSetTransactionSize(spip->rx.dma, config->size);
    dmaStreamSetMode(spip->rx.dma, spip->rxdmamode);

    dmaStreamSetMemory0(spip->tx.dma, config->txbuf + offset);
    dmaStreamSetTransactionSize(spip->tx.dma, config->size);
    dmaStreamSetMode(spip->tx.dma, spip->txdmamode);

    dmaStreamEnable(spip->rx.dma);
    dmaStreamEnable(spip->tx.dma);
#else
    dmaStreamSetMemory0(spip->dmarx, config->rxbuf + offset);
    dmaStreamSetTransactionSize(spip->dmarx, config->size);
    dmaStreamSetMode(spip->dmarx, spip->rxdmamode);

    dmaStreamSetMemory0(spip->dmatx, config->txbuf + offset);
    dmaStreamSetTransactionSize(spip->dmatx, config->size);
    dmaStreamSetMode(spip->dmatx, spip->txdmamode);

    dmaStreamEnable(spip->dmarx);
    dmaStreamEnable(spip->dmatx);
#endif
}


#endif /* FW_SPILINK */
#endif /* SPIDB_H_ */
