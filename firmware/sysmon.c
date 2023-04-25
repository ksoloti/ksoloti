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
/* Separating ADC3 sampling process from Sysmon thread in order to "abuse" ADC3 to get 4 additional ADC inputs at PF6, PF7, PF8, and PF9 but only if the gpio/in/analog2 object is used in the running patch. */
static WORKING_AREA(waThreadSysmonAdc3, 16);

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

    if (adcvalues[18] > v50_max) // adcvalues[18] contains PF10 = 5V supervisor data
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

__attribute__((noreturn))
static msg_t ThreadSysmonAdc3(void *arg) {
    // 5V Voltage supervisor and PF6 to 9 ADC sampling get their own little thread.
    (void)arg;
    uint8_t adc_ch = 8; // we can first pick up the first conversion of channel 8 started during initialization

    #if CH_USE_REGISTRY
    chRegSetThreadName("sysmonadc3");
    #endif

    while(1)
    {
        chThdSleepMilliseconds(2); // each ADC3 input is sampled at around 1000ms / 2ms / 5 = 100 Hz
        adcvalues[10 + adc_ch] = (ADC3->DR); // store results in indexes 14 to 18 of adcvalues[] then increment channel
        if (++adc_ch > 8) adc_ch = 4; // wrap channel around 4 to 8
        ADC3->SQR3 = adc_ch; // prepare next channel for conversion
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

    // initialize ADC3
    rccEnableADC3(FALSE);
    ADC3->CR2 = ADC_CR2_ADON;
    ADC3->SMPR1 = 0x07FFFFFF; // 0b 0000 0111 1111 1111 1111 1111 1111 1111
    ADC3->SMPR2 = 0x24924924; // 0b 0010 0100 1001 0010 0100 1001 0010 0100 sampling time 84 cycles for channels 0 to 9
    ADC3->SQR1 = 0;
    ADC3->SQR2 = 0;
    ADC3->SQR3 = 8; // start with ADC3_IN_8 (the 5V supervisor).
    ADC3->CR2 |= ADC_CR2_SWSTART;

  v50_max = 0;
  v50_min = 0xFFFF;

  isEnabled = true;

  chThdCreateStatic(waThreadSysmonAdc3, sizeof(waThreadSysmonAdc3), NORMALPRIO,
                    ThreadSysmonAdc3, NULL);
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

uint16_t sysmon_getVoltage50(void) {
  return voltage_50;
}

uint16_t sysmon_getVoltage10(void) {
  return adcvalues[13];
}
