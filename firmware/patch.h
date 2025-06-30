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

#ifndef __PATCH_H
#define __PATCH_H
#include <stdint.h>
#include "axoloti_defines.h"
#include "ch.h"
#include "hal.h"
#include "ui.h"
#if BOARD_KSOLOTI_CORE_H743
  #include "stm32h7xx.h"
  #include "stm32h7xx_hal_dma.h"
  #include "stm32h7xx_hal_i2c.h"
#else
  #include "stm32f4xx_hal_i2c.h"
#endif
#include "axoloti_board.h"
#include "midi.h"
#include "crc32.h"
#include "exceptions.h"
#include "ff.h"

#if BOARD_KSOLOTI_CORE_H743
  #define DAC DAC1
  #define APB1ENR APB1LENR
#endif

typedef void (*fptr_patch_init_t) (uint32_t fwID);
typedef void (*fptr_patch_dispose_t) (void);
#if FW_USBAUDIO
typedef void (*fptr_patch_dsp_process_t) (int32_t*, int32_t*, int32_t*, int32_t*);
extern bool usbAudioResample;
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

typedef enum {
  btInvalid,
  btF4,
  btH7_64,
  btH7_256,
} binType;

typedef struct {
  binType  type;
  uint32_t fwid;  
  uint32_t headerSize;
  uint32_t codeSize;
} binHeader_type;

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

extern I2C_HandleTypeDef onboard_i2c_handle;
extern uint8_t i2crxbuf[8];
extern uint8_t i2ctxbuf[8];

void InitPatch0(void);
int StartPatch(void);
void StopPatch(void);

void start_dsp_thread(void);

// need to do this at runtime, how?
// 1. Put this data at start of bin
// 2. Load it from flash somehow.
// 3. Who knows?

// so this needs sorting for firmware, how
// firmware needs to run in both modes for H7
#define PATCHFLASHLOC_H7        0x08100000

#define PATCHMAINLOC_H7_64      0x00000000
#define PATCHFLASHSIZE_H7_64    (64 * 1024)
#define PATCHFLASHSLOTS_H7_64   8

#define PATCHMAINLOC_H7_256     0x24040000
#define PATCHFLASHSIZE_H7_256   (256 * 1024)
#define PATCHFLASHSLOTS_H7_256  4

#define PATCHMAINLOC_F4         0x2000B000
#define PATCHFLASHLOC_F4        0x080E0000
#define PATCHFLASHSIZE_F4       0x00011000
#define PATCHFLASHSLOTS_F4      1



extern uint32_t GetPatchMainLoc(void);
extern uint32_t GetPatchFlashLoc(void);
extern uint32_t GetPatchFlashCodeLoc(void);
extern uint32_t GetPatchFlashSize(void);
extern uint32_t GetPatchFlashSlots(void);
extern uint32_t GetPatchHeaderLoc(void);
extern uint32_t GetPatchHeaderByteSize(void);
extern uint32_t GetPatchBinSize(void);

extern bool     CheckPatchBinHeader(void);
extern void     SetPatchOffset(uint32_t uOffset);
extern void     SetPatchHeaderFlashByteSize(uint32_t uSize);

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


int get_USBH_LL_GetURBState(void);
int get_USBH_LL_SubmitURB(void);
#endif //__PATCH_H
