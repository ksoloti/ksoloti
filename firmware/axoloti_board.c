/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
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
#include "axoloti_defines.h"
#include "axoloti_board.h"

//#define ENABLE_SERIAL_DEBUG 1

Mutex Mutex_DMAStream_1_7; // shared: SPI3 (axoloti control) and I2C2 (codec)

uint8_t adc_ch = 8; // we can first pick up the first conversion of channel 8 started during initialization

void axoloti_board_init(void) {

#ifdef BOARD_AXOLOTI_V05
  // initialize DMA2D engine
  RCC->AHB1ENR |= RCC_AHB1ENR_DMA2DEN;
  RCC->AHB1RSTR |= RCC_AHB1RSTR_DMA2DRST;
  RCC->AHB1RSTR &= ~RCC_AHB1RSTR_DMA2DRST;
#endif

  chMtxInit(&Mutex_DMAStream_1_7);
}

void adc_init(void) {

  adc_configpads();
  adcStart(&ADCD1, NULL);

  // initialize ADC3
  rccEnableADC3(FALSE);
  ADC3->CR2 = ADC_CR2_ADON;
  ADC3->SMPR1 = 0x07FFFFFF; // 0b 0000 0111 1111 1111 1111 1111 1111 1111
  ADC3->SMPR2 = 0x24924924; // 0b 0010 0100 1001 0010 0100 1001 0010 0100 sampling time 84 cycles for channels 0 to 9
  ADC3->SQR1 = 0;
  ADC3->SQR2 = 0;
  ADC3->SQR3 = 8; // start with ADC3_IN_8 (the 5V supervisor).
  ADC3->CR2 |= ADC_CR2_SWSTART;

  adcSTM32EnableTSVREFE();
}

void adc_configpads(void) {
#if (BOARD_AXOLOTI_V05)
  palSetPadMode(GPIOA, 0, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOA, 1, PAL_MODE_INPUT_ANALOG);
#ifndef ENABLE_SERIAL_DEBUG
  palSetPadMode(GPIOA, 2, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOA, 3, PAL_MODE_INPUT_ANALOG);
#endif
  palSetPadMode(GPIOA, 4, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOA, 5, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOA, 6, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOA, 7, PAL_MODE_INPUT_ANALOG);

  palSetPadMode(GPIOB, 0, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOB, 1, PAL_MODE_INPUT_ANALOG);

  /* palSetPadMode(GPIOC, 0, PAL_MODE_INPUT_ANALOG); // pin remapped to FMC */
  palSetPadMode(GPIOC, 1, PAL_MODE_INPUT_ANALOG);
  /* palSetPadMode(GPIOC, 2, PAL_MODE_INPUT_ANALOG); // pin remapped to FMC */
  /* palSetPadMode(GPIOC, 3, PAL_MODE_INPUT_ANALOG); // pin remapped to FMC */
  palSetPadMode(GPIOC, 4, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOC, 5, PAL_MODE_INPUT_ANALOG);

  /* Four additional ADC inputs sampled at low speed via ADC3 */
  palSetPadMode(GPIOF, 6, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOF, 7, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOF, 8, PAL_MODE_INPUT_ANALOG);
  palSetPadMode(GPIOF, 9, PAL_MODE_INPUT_ANALOG);

#else
#error "ADC: No board defined?"
#endif
}

/*
 * ADC samples buffer. Increased size to hold data of 5V supervisor and PF6..9 inputs.
 */
unsigned short adcvalues[ADC_GRP1_NUM_CHANNELS * ADC_GRP1_BUF_DEPTH + ADC_GRP2_NUM_CHANNELS * ADC_GRP2_BUF_DEPTH] __attribute__ ((section (".sram2")));

/*
 * ADC conversion group.
 * Mode:        Linear buffer, 8 samples of 1 channel, SW triggered.
 * Channels:    IN11.
 */
static const ADCConversionGroup adcgrpcfg1 = {
    FALSE,      //circular buffer mode
    ADC_GRP1_NUM_CHANNELS,        //Number of the analog channels
    NULL,                         //Callback function (not needed here)
    0,             //Error callback
    0, /* CR1 */
    ADC_CR2_SWSTART, /* CR2 */
    ADC_SMPR1_SMP_AN10(ADC_SAMPLE_84) | ADC_SMPR1_SMP_AN11(ADC_SAMPLE_84)
        | ADC_SMPR1_SMP_AN12(ADC_SAMPLE_84) | ADC_SMPR1_SMP_AN13(ADC_SAMPLE_84)
        | ADC_SMPR1_SMP_AN14(ADC_SAMPLE_84) | ADC_SMPR1_SMP_AN15(ADC_SAMPLE_84)
        | ADC_SMPR1_SMP_SENSOR(ADC_SAMPLE_144) | ADC_SMPR1_SMP_VREF(ADC_SAMPLE_144), //sample times ch10-18
    ADC_SMPR2_SMP_AN0(ADC_SAMPLE_84) | ADC_SMPR2_SMP_AN1(ADC_SAMPLE_84)
        | ADC_SMPR2_SMP_AN2(ADC_SAMPLE_84) | ADC_SMPR2_SMP_AN3(ADC_SAMPLE_84)
        | ADC_SMPR2_SMP_AN4(ADC_SAMPLE_84) | ADC_SMPR2_SMP_AN5(ADC_SAMPLE_84)
        | ADC_SMPR2_SMP_AN6(ADC_SAMPLE_84) | ADC_SMPR2_SMP_AN7(ADC_SAMPLE_84)
        | ADC_SMPR2_SMP_AN8(ADC_SAMPLE_84) | ADC_SMPR2_SMP_AN9(ADC_SAMPLE_84), //sample times ch0-9
    ADC_SQR1_SQ13_N(ADC_CHANNEL_IN15) | ADC_SQR1_SQ14_N(ADC_CHANNEL_VREFINT)
        | ADC_SQR1_NUM_CH(ADC_GRP1_NUM_CHANNELS), //SQR1: Conversion group sequence 13...16 + sequence length
    ADC_SQR2_SQ7_N(ADC_CHANNEL_IN6) | ADC_SQR2_SQ8_N(ADC_CHANNEL_IN7)
        | ADC_SQR2_SQ9_N(ADC_CHANNEL_IN8) | ADC_SQR2_SQ10_N(ADC_CHANNEL_IN9)
        | ADC_SQR2_SQ11_N(ADC_CHANNEL_IN11) | ADC_SQR2_SQ12_N(ADC_CHANNEL_IN14), //SQR2: Conversion group sequence 7...12, skip IN10 (PC0), IN12 (PC2), IN13 (PC3)
    ADC_SQR3_SQ1_N(ADC_CHANNEL_IN0) | ADC_SQR3_SQ2_N(ADC_CHANNEL_IN1)
        | ADC_SQR3_SQ3_N(ADC_CHANNEL_IN2) | ADC_SQR3_SQ4_N(ADC_CHANNEL_IN3)
        | ADC_SQR3_SQ5_N(ADC_CHANNEL_IN4) | ADC_SQR3_SQ6_N(ADC_CHANNEL_IN5) //SQR3: Conversion group sequence 1...6
};

void adc_convert(void) {
  adcStopConversion(&ADCD1);
  adcStartConversion(&ADCD1, &adcgrpcfg1, adcvalues, ADC_GRP1_BUF_DEPTH);

  // Sample ADC3 (slower than ADC1 but still adequate)
  adcvalues[10 + adc_ch] = (ADC3->DR); // store results in indexes 14 to 18 of adcvalues[]
  if (++adc_ch > 8) adc_ch = 4; // wrap channel from 4 to 8
  ADC3->SQR3 = adc_ch; // prepare next channel for conversion
  ADC3->CR2 |= ADC_CR2_SWSTART; // start next conversion
}
