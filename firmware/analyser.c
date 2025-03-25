#include "analyser.h"


#if ANALYSE_USB_AUDIO
void AnalyserSetup(void)
{
    AddAnalyserChannel(acUsbAudioError, GPIOG, 11);
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

        bResult = true;
    }
    
    return bResult;
}

void AnalyserSetChannel(AnalyserChannel channel, bool bState)
{
    uint32_t pad = analyserChannels[channel].pad;
    stm32_gpio_t *port = analyserChannels[channel].port;

    palWritePort(port, (palReadLatch(port) & ~PAL_PORT_BIT(pad)) | (((bState) & 1U) << pad));
}
#endif