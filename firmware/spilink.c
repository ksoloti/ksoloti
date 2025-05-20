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

#include "ch.h"
#include "hal.h"
#include "axoloti_board.h"
#include "axoloti.h"
#include "mcuconf.h"
#include "spilink.h"
#include "spidb.h"
#include "sysmon.h"

#ifdef FW_SPILINK

bool_t spilink_toggle;
Thread *pThreadSpilink = 0;

spilink_data_t spilink_tx[2] SPILINK_DMA_SECTION;
spilink_data_t spilink_rx[2] SPILINK_DMA_SECTION;

spilink_channels_t *spilink_rx_samples;
spilink_channels_t *spilink_tx_samples;

uint32_t frameno = 0;

extern void detect_MCO(void);

#if defined(BOARD_KSOLOTI_CORE)
#define SPILINK_NSS_PORT GPIOD
#define SPILINK_NSS_PIN 5
#elif defined(BOARD_AXOLOTI_CORE)
#define SPILINK_NSS_PORT GPIOA
#define SPILINK_NSS_PIN 15
#endif

#define SPILINKD SPID3

bool_t spilink_master_active = 0;
// int spilink_update_index;
// int lcd_update_index;

/* SPI configuration (10.5 MHz, CPHA=0, CPOL=0, 16 bit). */
static const SPIDBConfig spidbcfg_master = {
    { 
        .circular = true,
        .slave    = false,
        .data_cb  = NULL,
        .error_cb = NULL,
        .ssport   = SPILINK_NSS_PORT,
        .sspad    = SPILINK_NSS_PIN,

#if BOARD_KSOLOTI_CORE_H743
        .cfg1      = 0U, // TODO SPILINK_H7
        .cfg2      = 0U
#else
        .cr1      = SPI_CR1_DFF /* 16-bit frame */
                  | SPI_CR1_BR_0, /* 10.5 MHz */
                  // | SPI_CR1_CPOL /* CPOL=1 */
                  // | SPI_CR1_CPHA /* CPHA=1 */

        .cr2      = 0U
#endif

    },
    (void *)&spilink_rx, (void *)&spilink_tx,
    sizeof(spilink_data_t) / 2
};

static const SPIDBConfig spidbcfg_slave = {
    {
        .circular  = true,
        .slave     = true,
        .data_cb   = NULL,
        .error_cb  = NULL,
        .ssport    = SPILINK_NSS_PORT,
        .sspad     = SPILINK_NSS_PIN,

#if BOARD_KSOLOTI_CORE_H743
        .cfg1       = 0U, // TODO SPILINK_H7
        .cfg2       = 0U
#else
        .cr1       = SPI_CR1_DFF /* 16-bit frame */,
        .cr2       = 0U
#endif

    },
    (void *)&spilink_rx, (void *)&spilink_tx,
    sizeof(spilink_data_t) / 2
};

static WORKING_AREA(waThreadSpilink, 256); // SPILINK_DMA_SECTION?;


static msg_t ThreadSpilinkSlave(void *arg)
{
    (void) arg;

#if CH_CFG_USE_REGISTRY
    chRegSetThreadName("spilink");
#endif

    /* Synced */
    while (1) {
        /* Waiting for messages.*/
        msg_t m = chEvtWaitAnyTimeout(7, MS2ST(50));

        if (!m) {
            /* timeout */
            int i; for (i = 0; i < 2; i++) {
                int j; for (j = 0; j < SPILINK_CHANNELS; j++) {
                    int k; for (k = 0; k < SPILINK_BUFSIZE; k++) {
                        spilink_rx[i].audio_io.channel[j].samples[k] = i + 2;
                    }
                }
            }
            LogTextMessage("SPILINK sync lost, waiting...");
            detect_MCO();
            LogTextMessage("SPILINK sync found, rebooting...");
            chThdSleepMilliseconds(10);
            NVIC_SystemReset();
            continue;

        }
        else if (m & half_transfer_complete) {
            spilink_toggle = 0;
            if (spilink_rx[0].header != SPILINK_HEADER) {
                spidbSlaveResync(&SPILINKD);
                // LogTextMessage("spislaveresync halftransfer");
            }
        }
        else if (m & full_transfer_complete) {
            spilink_toggle = 1;
            if (spilink_rx[0].header != SPILINK_HEADER) {
                spidbSlaveResync(&SPILINKD);
                // LogTextMessage("spislaveresync fulltransfer");
            }
            /* else if (!(SAI1_Block_A->CR1 & SAI_xCR1_SAIEN)) {
                   chSysLock();
                   SAI1_Block_A->CR1 |= SAI_xCR1_SAIEN;
                   SAI1_Block_B->CR1 |= SAI_xCR1_SAIEN;
                   chSysUnlock();
            }*/
        }
        else if (m & other_transfer) {
            spidbSlaveResync(&SPILINKD);
            // LogTextMessage("spislaveresync other");
            continue;
        }
        else {
            // LogTextMessage("spislaveresync unknown state");
            //????
        }
    }
}


void spilink_clear_audio_tx(void) {
    int i; for (i = 0; i < 2; i++) {
        int j; for (j = 0; j < SPILINK_CHANNELS; j++) {
            int k; for (k = 0; k < SPILINK_BUFSIZE; k++) {
                spilink_tx[i].audio_io.channel[j].samples[k] = i + 1;
            }
        }
    }
}


void spilink_init(bool_t isMaster) {
    if (isMaster)
        palSetPadMode(SPILINK_NSS_PORT, SPILINK_NSS_PIN, PAL_MODE_OUTPUT_PUSHPULL); /* master NSS */
    else
        palSetPadMode(SPILINK_NSS_PORT, SPILINK_NSS_PIN, PAL_MODE_INPUT); /* synced NSS */

    palSetPadMode(GPIOB, 3, PAL_MODE_ALTERNATE(6)); /* SCK */
    palSetPadMode(GPIOB, 4, PAL_MODE_ALTERNATE(6)); /* MISO */
    palSetPadMode(GPIOD, 6, PAL_MODE_ALTERNATE(5)); /* MOSI */

    int i; for (i = 0; i < 2; i++) {
        spilink_tx[i].header = SPILINK_HEADER;
        spilink_tx[i].footer = SPILINK_FOOTER;

        int j; for (j = 0; j < SPILINK_CHANNELS; j++) {
            int k; for (k = 0; k < SPILINK_BUFSIZE; k++) {
                spilink_rx[i].audio_io.channel[j].samples[k] = i;
                spilink_tx[i].audio_io.channel[j].samples[k] = i + 1;
            }
        }
    }

    spilink_rx_samples = &spilink_rx[0].audio_io;
    spilink_tx_samples = &spilink_tx[0].audio_io;

    if (isMaster) {
        /* Master */
        spilink_toggle = 0;
        spidbMasterStart(&SPILINKD, &spidbcfg_master);
        spilink_master_active = 1;
    }
    else {
        /* Synced */
        Thread *_pThreadSpilink = chThdCreateStatic(waThreadSpilink,
            sizeof(waThreadSpilink), SPILINK_PRIO, (void*) ThreadSpilinkSlave, NULL);

        spidbSlaveStart(&SPILINKD, &spidbcfg_slave, _pThreadSpilink);
        pThreadSpilink = _pThreadSpilink;
    }
}


#endif /* FW_SPILINK */