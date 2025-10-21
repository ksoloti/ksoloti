/*
 * Copyright (C) 2016 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
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
 * spilink = SPI bus link to interconnect Axoloti Cores and Axoloti Control
 *
 */

#ifndef SPILINK_H_
#define SPILINK_H_

#include "ch.h"
#include "stdint.h"
#include "ui.h"

#ifdef FW_SPILINK

#define SPILINK_BUFSIZE 16
#define SPILINK_CHANNELS 4

#define SPILINK_HEADER (('A' << 8) | ('x') | ('o' << 24) | ('<' << 16))
#define SPILINK_FOOTER (('A' << 8) | ('x') | ('o' << 24) | ('>' << 16))


typedef struct {
    int32_t samples[SPILINK_BUFSIZE];
} spilink_samples_t;

typedef struct {
    spilink_samples_t channel[SPILINK_CHANNELS];
} spilink_channels_t;

typedef struct {
    uint32_t header;
    uint32_t frameno;
    spilink_channels_t audio_io;
    uint32_t footer;
} spilink_data_t;

void spilink_init(bool_t isMaster);
void spilink_clear_audio_tx(void);

extern Thread *pThreadSpilink;

extern spilink_channels_t *spilink_rx_samples;
extern spilink_channels_t *spilink_tx_samples;

extern spilink_data_t spilink_rx[2];
extern spilink_data_t spilink_tx[2];

extern uint32_t frameno;

extern bool_t spilink_master_active;
extern bool_t spilink_toggle;

// extern int spilink_update_index;
// extern int lcd_update_index;

#endif /* FW_SPILINK */
#endif /* SPILINK_H_ */
