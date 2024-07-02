/**
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
#ifndef __AXOBOARD_H
#define __AXOBOARD_H

#include "axoloti_defines.h"

#if defined(BOARD_KSOLOTI_CORE)
#define ADC_GRP1_NUM_CHANNELS   14
#define ADC_GRP2_NUM_CHANNELS   5
#else
#define ADC_GRP1_NUM_CHANNELS   16
#define ADC_GRP2_NUM_CHANNELS   0
#endif

#define ADC_GRP1_BUF_DEPTH      1

extern unsigned short adcvalues[ADC_GRP1_NUM_CHANNELS + ADC_GRP2_NUM_CHANNELS];

void axoloti_board_init(void);
void adc_init(void);
void adc_configpads(void);
void adc_convert(void);


#if defined(BOARD_AXOLOTI_CORE) || defined(BOARD_KSOLOTI_CORE)
#define LED1_PORT GPIOG
#define LED1_PIN 6
#define LED2_PORT GPIOC
#define LED2_PIN 6
// SW1 is also BOOT0
#define SW1_PORT GPIOB
#define SW1_PIN 5
#define SW2_PORT GPIOA
#define SW2_PIN 10
#define OCFLAG_PORT GPIOG
#define OCFLAG_PIN 13
#define SDCSW_PORT GPIOD
#define SDCSW_PIN 13

#elif defined(BOARD_STM32F4_DISCOVERY)
/* LED1: green */
#define LED1_PORT GPIOD
#define LED1_PIN 12
/* LED2: red */
#define LED2_PORT GPIOD
#define LED2_PIN 14
#define SW2_PORT GPIOA
#define SW2_PIN 0

#endif

#if defined(BOARD_KSOLOTI_CORE)
#define SPILINK_JUMPER_PORT GPIOD
#define SPILINK_JUMPER_PIN 12
#elif defined(BOARD_AXOLOTI_CORE)
#define SPILINK_JUMPER_PORT GPIOB
#define SPILINK_JUMPER_PIN 10
#endif


#if defined(BOARD_KSOLOTI_CORE) || defined(BOARD_AXOLOTI_CORE)
#define SDMIDI SD6
#elif defined(BOARD_STM32F4_DISCOVERY)
#define SDMIDI SD1
#endif

#endif