/**
 * Copyright (C) 2015 Johannes Taelman
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

/**
 * System health monitor and LED blinker thread
 */

#include "ch.h"
#include "hal.h"
#include "axoloti_board.h"
#include "sysmon.h"
#include "pconnection.h"
#include "patch.h"
#include "sdcard.h"

bool repeat = FALSE;
bool isEnabled = TRUE;
uint32_t pattern = BLINK_BOOT;
uint16_t voltage_50;
uint16_t v50_min;
uint16_t v50_max;
bool sdcsw_prev = FALSE;

volatile uint8_t pattern_index;

static WORKING_AREA(waThreadSysmon, 256);
/* Separating ADC3 sampling process from Sysmon thread in order to "abuse" the 5V voltage supervisor thread to get 4 additional (sampled at ca. 50 hz) ADC inputs at PF6, PF7, PF8, and PF9. */
static WORKING_AREA(waThreadAdc3, 16);

__attribute__((noreturn))
static msg_t ThreadSysmon(void *arg) {
  (void)arg;
#if CH_USE_REGISTRY
  chRegSetThreadName("sysmon");
#endif
  pattern_index = 0;
  while (1) {
    uint8_t pi = pattern_index;

#ifdef OCFLAG_PORT
    if (!palReadPad(OCFLAG_PORT, OCFLAG_PIN)) {
      setErrorFlag(ERROR_USBH_OVERCURRENT);
      pattern = BLINK_OVERLOAD;
      repeat = FALSE;
    }
#endif
    if (isEnabled) {
#ifdef LED1_PORT
      palWritePad(LED1_PORT, LED1_PIN, (pattern >> pi) & 1);
#endif
      pi++;
#ifdef LED2_PORT
      palWritePad(LED2_PORT, LED2_PIN, (pattern >> pi) & 1);
#endif
      pi++;
      if (pi > 31) {
        if (!repeat) {
          pattern = BLINK_OK;
        }
        pattern_index = 0;
      }
      else
        pattern_index = pi;
    }

    if (adcvalues[18] > v50_max) // last element of adcvalues contains PF10 = 5V supervisor data
      v50_max = adcvalues[18];
    if (adcvalues[18] < v50_min)
      v50_min = adcvalues[18];
    voltage_50 = adcvalues[18];

// sdcard switch monitor
#ifdef SDCSW_PIN
    bool sdcsw = palReadPad(SDCSW_PORT, SDCSW_PIN);
    if (sdcsw && !sdcsw_prev) {
//      LogTextMessage("sdcard ejected");
      StopPatch();
      sdcard_unmount();
    }
    else if (!sdcsw && sdcsw_prev) {
//      LogTextMessage("sdcard inserted");
      sdcard_attemptMountIfUnmounted();
      if (!fs_ready) {
        pattern_index = 0;
        pattern = BLINK_OVERLOAD;
      }
      LoadPatchStartSD();
    }
    sdcsw_prev = sdcsw;
#endif

    chThdSleepMilliseconds(100);
  }
}

static msg_t ThreadAdc3(void *arg) {
    (void)arg;
    #if CH_USE_REGISTRY
    chRegSetThreadName("adc3");
    #endif
    uint8_t adc_ch = 0;
    while (1) {
        chThdSleepMilliseconds(4);
        // alternating v50 monitor and four adc conversions, appending results to adcvalues array
        adcvalues[adc_ch+ADC_GRP1_NUM_CHANNELS] = (ADC3->DR);
        if (++adc_ch > 4) adc_ch = 0;
        ADC3->SQR3 = adc_ch+4; // 4 to 8
        ADC3->CR2 |= ADC_CR2_SWSTART; // start next conversion
    }
}

void sysmon_init(void) {
#ifdef LED1_PORT
  palSetPadMode(LED1_PORT, LED1_PIN, PAL_MODE_OUTPUT_PUSHPULL);
#endif
#ifdef LED2_PORT
  palSetPadMode(LED2_PORT, LED2_PIN, PAL_MODE_OUTPUT_PUSHPULL);
#endif
#ifdef SDCSW_PIN
  palSetPadMode(SDCSW_PORT, SDCSW_PIN, PAL_MODE_INPUT_PULLUP);
#endif
  // ADC3 for 5V supply monitoring
  rccEnableADC3(FALSE);
  // ADC3->CR1 |= 0x100; // set scan bit
  ADC3->CR2 = ADC_CR2_ADON;
  ADC3->SMPR1 = 0x07FFFFFF; // 0b 0000 0111 1111 1111 1111 1111 1111 1111
  ADC3->SMPR2 = 0x3F7FFFFF; // 0b 0011 1111 0111 1111 1111 1111 1111 1111
  // ADC3->SMPR2 = 0x1B6DB6DB; // 0b 0001 1011 0110 1101 1011 0110 1101 1011 sample time 56 cycles for channels 0 to 9
  ADC3->SQR1 = 0;
  // ADC3->SQR1 = 0x00400000; // 5 conversions in sequence
  ADC3->SQR2 = 0;
  // ADC3->SQR3 = 8; // 0b 1000
  ADC3->SQR3 = 4;
  // ADC3->SQR3 = 0x004298E8; // 0b 0000 0000 0100 0010 1001 1000 1110 1000 5 conversions in sequence: ch 8 then 7, 6, 5, 4
  ADC3->CR2 |= ADC_CR2_SWSTART;
  v50_max = 0;
  v50_min = 0xFFFF;

  isEnabled = true;

  chThdCreateStatic(waThreadAdc3, sizeof(waThreadAdc3), NORMALPRIO,
  ThreadAdc3, NULL);
  chThdCreateStatic(waThreadSysmon, sizeof(waThreadSysmon), NORMALPRIO,
                    ThreadSysmon, NULL);

}

void sysmon_disable_blinker(void) {
  isEnabled = false;
}

void sysmon_enable_blinker(void) {
  isEnabled = true;
}

void sysmon_blink_pattern(uint32_t pat) {
  pattern = pat;
  pattern_index = 0;
}

uint32_t errorflags = 0;

void setErrorFlag(error_flag_t error) {
  errorflags |= 1 << error;
  repeat = TRUE;
  sysmon_blink_pattern(BLINK_ERROR);
}

bool getErrorFlag(error_flag_t error) {
  return (errorflags & (1 << error)) > 0;
}

void errorFlagClearAll(void) {
  errorflags = 0;
}

uint16_t sysmon_getVoltage50(void){
  return voltage_50;
}

uint16_t sysmon_getVoltage10(void){
  return adcvalues[13];
}
