/*
    ChibiOS/RT - Copyright (C) 2006-2013 Giovanni Di Sirio

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

#ifndef _USBCFG_H_
#define _USBCFG_H_

#define MIN(X,Y) ((X) < (Y) ? (X) : (Y))
#define MAX(X,Y) ((X) > (Y) ? (X) : (Y))

#include "bulk_usb.h"
#include "midi_usb.h"

#if FW_USBAUDIO
#include "audio_usb.h"
#endif

/*
 * Endpoints to be used for USBD1.
 */
#if FW_USBAUDIO
#define AUDIO_ENDPPOINT_OUT 0x03
#define AUDIO_ENDPPOINT_IN  0x83
#endif

#define USBD1_DATA_REQUEST_EP           1
#define USBD1_DATA_AVAILABLE_EP         1

#define USBD2_DATA_REQUEST_EP           2
#define USBD2_DATA_AVAILABLE_EP         2



extern MidiUSBDriver  MDU1;
extern BulkUSBDriver  BDU1;
#if FW_USBAUDIO
extern AudioUSBDriver ADU1;
#endif

extern const USBConfig usbcfg;
extern const MidiUSBConfig midiusbcfg;
extern const BulkUSBConfig bulkusbcfg;
#if FW_USBAUDIO
extern const AudioUSBConfig audiousbcfg;
extern void InitUsbAudio(void);
#endif

#endif  /* _USBCFG_H_ */

/** @} */
