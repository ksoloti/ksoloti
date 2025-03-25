#include "analyser.h"

#pragma GCC push_options
#pragma GCC optimize ("O0")

#if ANALYSE_USB_AUDIO
void AnalyserSetup(void)
{
    AddAnalyserChannel(acUsbAudioDataExchange,      GPIOB, 9);
    AddAnalyserChannel(acUsbAudioInitiateTransmit,  GPIOB, 8);
    AddAnalyserChannel(acUsbAudioInitiateReceive,   GPIOB, 7);
    AddAnalyserChannel(acUsbAudioTransmitComplete,  GPIOB, 6);
    AddAnalyserChannel(acUsbAudioReceiveComplete,   GPIOB, 4);
    AddAnalyserChannel(acUsbAudioError,             GPIOB, 3);
    
    AnalyserSetChannel(acUsbAudioError,             false);
    AnalyserSetChannel(acUsbAudioDataExchange,      false);
    AnalyserSetChannel(acUsbAudioInitiateTransmit,  false);
    AnalyserSetChannel(acUsbAudioInitiateReceive,   false);
    AnalyserSetChannel(acUsbAudioTransmitComplete,  false);
    AnalyserSetChannel(acUsbAudioReceiveComplete,   false);
    
    AnalyserSetChannel(acUsbAudioError,             true);
    AnalyserSetChannel(acUsbAudioDataExchange,      true);
    AnalyserSetChannel(acUsbAudioInitiateTransmit,  true);
    AnalyserSetChannel(acUsbAudioInitiateReceive,   true);
    AnalyserSetChannel(acUsbAudioTransmitComplete,  true);
    AnalyserSetChannel(acUsbAudioReceiveComplete,   true);
}
#endif


#if ANALYSER_CHANNELS > 0

AnalyserChannelData analyserChannels[ANALYSER_CHANNELS];

bool AddAnalyserChannel(AnalyserChannel uChannel, stm32_gpio_t *port, uint32_t pad)
{
    bool bResult =  false;
    if(uChannel < ANALYSER_CHANNELS)
    {
        analyserChannels[uChannel].port = port;
        analyserChannels[uChannel].pad = pad;

        palSetPadMode(port, pad, PAL_MODE_OUTPUT_PUSHPULL);
        AnalyserSetChannel(acUsbAudioError, false);
    
        bResult = true;
    }
    
    return bResult;
}

void AnalyserSetChannel(AnalyserChannel channel, bool bState)
{
    uint32_t pad = analyserChannels[channel].pad;
    stm32_gpio_t *port = analyserChannels[channel].port;

    palWritePad(port, pad, (uint32_t)bState);
}

void AnalyserTriggerChannel(AnalyserChannel channel)
{
    AnalyserSetChannel(channel, true);
    AnalyserSetChannel(channel, false);
}

#endif

#pragma GCC pop_options