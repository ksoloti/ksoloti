#pragma once

#include <inttypes.h>
#include <stdbool.h>
#include "hal.h"

#if ANALYSE_USB_AUDIO

typedef enum
{
    acUsbAudioError = 0,
    acUsbAudioDataExchange,
    acUsbAudioInitiateTransmit,
    acUsbAudioInitiateReceive,
    acUsbAudioTransmitComplete,
    acUsbAudioReceiveComplete,
    acUsbDSP,
    acUsbAudioSof,
    acUsbFifo,
    acUsbAudioAdjust
} AnalyserChannel;

#define ANALYSER_CHANNELS 10

#endif

#if ANALYSER_CHANNELS > 0

bool AddAnalyserChannel(AnalyserChannel channel, stm32_gpio_t *port, uint32_t pad);
void AnalyserSetChannel(AnalyserChannel channel, bool bState);
void AnalyserTriggerChannel(AnalyserChannel channel);
void AnalyserSetup(void);

typedef struct 
{
    stm32_gpio_t *port;
    uint32_t pad;
} AnalyserChannelData;

#else

#define AddAnalyserChannel(channel, port, pad)
#define AnalyserSetChannel(channel, bState)
#define AnalyserTriggerChannel(channel)
#define AnalyserSetup()
#endif