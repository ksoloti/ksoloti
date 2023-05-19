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
 * Adapted from pdm.h
 * Created on: Jun 7, 2012
 * Author: Kumar Abhishek
 */

#ifndef __PDM_H
#define __PDM_H

#include "hal.h"

#define PDM_I2S_ENABLE rccEnableSPI3(FALSE)
#define PDM_I2S_DISABLE rccDisableSPI3(FALSE)
#define PDM_I2S SPI3
#define PDM_I2Sext I2S3ext

#define I2S3_TX_DMA_CHANNEL STM32_DMA_GETCHANNEL(STM32_SPI_SPI3_TX_DMA_STREAM, STM32_SPI3_TX_DMA_CHN)

// #define I2S3ext_RX_DMA_CHANNEL STM32_DMA_GETCHANNEL(STM32_SPI_SPI3_RX_DMA_STREAM, STM32_SPI3_RX_DMA_CHN

#define I2S3ext_RX_DMA_CHANNEL STM32_DMA_GETCHANNEL(STM32_DMA_STREAM_ID(1, 0), 3)

extern void pdm_i2s_init_48k(void);

#endif /* PDM_H_ */
