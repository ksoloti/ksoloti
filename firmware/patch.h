/*
 * Copyright (C) 2013, 2014 Johannes Taelman
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

#ifndef __PATCH_H
#define __PATCH_H
#include <stdint.h>
#include "axoloti_defines.h"
#include "ch.h"
#include "hal.h"
#include "ui.h"
#include "stm32f4xx_hal_i2c.h"
#include "axoloti_board.h"
#include "midi.h"
#include "crc32.h"
#include "exceptions.h"
#include "ff.h"
#include "../chibios/os/hal/lib/streams/chprintf.h"

typedef void (*fptr_patch_init_t) (uint32_t fwID);
typedef void (*fptr_patch_dispose_t) (void);
#if FW_USBAUDIO
typedef void (*fptr_patch_dsp_process_t) (int32_t*, int32_t*, int32_t*, int32_t*);
#elif defined(FW_I2SCODEC)
typedef void (*fptr_patch_dsp_process_t) (int32_t*, int32_t*, int32_t*, int32_t*);
#else
typedef void (*fptr_patch_dsp_process_t) (int32_t*, int32_t*);
#endif
typedef void (*fptr_patch_midi_in_handler_t) (midi_device_t dev, uint8_t port, uint8_t, uint8_t, uint8_t);
typedef void (*fptr_patch_applyPreset_t) (uint8_t);

typedef struct {
  int32_t pexIndex;
  int32_t value;
} PresetParamChange_t;

typedef struct {
  fptr_patch_init_t fptr_patch_init;
  fptr_patch_dispose_t fptr_patch_dispose;
  fptr_patch_dsp_process_t fptr_dsp_process;
  fptr_patch_midi_in_handler_t fptr_MidiInHandler;
  fptr_patch_applyPreset_t fptr_applyPreset;
  uint32_t numPEx;
  ParameterExchange_t* pPExch;
  int32_t* pDisplayVector;
  uint32_t patchID;
  uint32_t initpreset_size;
  void* pInitpreset;
  uint32_t npresets;
  uint32_t npreset_entries;
  PresetParamChange_t* pPresets; // is a npreset array of npreset_entries of PresetParamChange_t
} patchMeta_t;


extern patchMeta_t patchMeta;

extern uint32_t     dspLoad200; // DSP load: Values 0-200 correspond to 0-100%

extern bool     dspOverload;

typedef enum {
  START_SD = -1,
  START_FLASH = -2,
  BY_FILENAME = -3,
  LIVE = -4,
  UNINITIALIZED = -5
  /* and positive numbers are index in patchbank */
} loadPatchIndex_t;
extern loadPatchIndex_t loadPatchIndex;

typedef enum {
  RUNNING = 0,
  STOPPED = 1,
  STOPPING = 2,
  STARTFAILED = 3,
} patchStatus_t;

extern volatile patchStatus_t patchStatus;

extern uint8_t hid_buttons[3];
extern uint8_t hid_mouse_x;
extern uint8_t hid_mouse_y;

extern uint8_t hid_keys[6];
extern uint8_t hid_key_modifiers;

extern uint8_t hid_joy_left_axis_x;
extern uint8_t hid_joy_left_axis_y;
extern uint8_t hid_joy_right_axis_x;
extern uint8_t hid_joy_right_axis_y;
extern uint8_t hid_joy_buttons[3];

extern I2C_HandleTypeDef onboard_i2c_handle;
extern uint8_t i2crxbuf[8];
extern uint8_t i2ctxbuf[8];

void InitPatch0(void);
uint8_t StartPatch(void);
uint8_t StopPatch(void);

void start_dsp_thread(void);

#define PATCHMAINLOC 0x20011000

// patch is located in sector 11
#define PATCHFLASHLOC 0x080E0000
#define PATCHFLASHSIZE 0xB000

void StartLoadPatchTread(void);
void LoadPatch(const char* name);
void LoadPatchStartSD(void);
void LoadPatchStartFlash(void);
void LoadPatchIndexed(uint32_t index);
loadPatchIndex_t GetIndexOfCurrentPatch(void);

void codec_clearbuffer(void);

#if FW_USBAUDIO
void usb_clearbuffer(void);
extern bool usbAudioResample;
#endif

void SetPatchSafety(uint16_t uUIMidiCost, uint8_t uDspLimit200);
void ReportThreadStacks(void);

int get_USBH_LL_GetURBState(void);
int get_USBH_LL_SubmitURB(void);
#endif //__PATCH_H
