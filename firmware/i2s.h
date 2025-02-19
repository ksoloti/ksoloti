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

#ifndef _I2S_H_
#define _I2S_H_

#include "axoloti_defines.h"

#ifdef FW_I2SCODEC

extern int32_t i2s_buf[BUFSIZE * 2]; // *2 for stereo
extern int32_t i2s_buf2[BUFSIZE * 2];
extern int32_t i2s_rbuf[BUFSIZE * 2];
extern int32_t i2s_rbuf2[BUFSIZE * 2];

extern void i2s_init(void);
extern void i2s_stop(void);

#endif /* FW_I2SCODEC */
#endif /* _I2S_H_ */