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

#ifndef __CODEC_H
#define __CODEC_H
#include <stdint.h>
#include "axoloti_defines.h"
#include "ch.h"
#include "migration_v16.h"

// double buffers for DMA, interleaved stereo
extern int32_t buf[BUFSIZE * 2]; // *2 for stereo
extern int32_t buf2[BUFSIZE * 2];
extern int32_t rbuf[BUFSIZE * 2];
extern int32_t rbuf2[BUFSIZE * 2];

void codec_init(bool_t isMaster);

void codec_clearbuffer(void);

extern void computebufI(int32_t *inp, int32_t *outp);

#endif /* __CODEC_H */
