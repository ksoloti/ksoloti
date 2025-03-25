#pragma once

#include <inttypes.h>
#include <stdbool.h>
#include "hal.h"

#if ANALYSE_USB_AUDIO
#define ANALYSER_CHANNELS 1
typedef enum
{
    acUsbAudioError = 0
} AnalyserChannel;

#endif

#if ANALYSER_CHANNELS > 0

bool AddAnalyserChannel(AnalyserChannel channel, stm32_gpio_t *port, uint32_t pad);
void AnalyserSetChannel(AnalyserChannel channel, bool bState);
void AnalyserSetup(void);

typedef struct 
{
    stm32_gpio_t *port;
    uint32_t pad;
} AnalyserChannelData;

#else

#define AddAnalyserChannel(channel, port, pad)
#define AnalyserSetChannel(channel, bState)
#define AnalyserSetup()
#endif