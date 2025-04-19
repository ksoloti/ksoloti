#include "analyser.h"

#pragma GCC push_options
#pragma GCC optimize ("O0")

#if ANALYSE_USB_AUDIO

void AnalyserSendChar(char c)
{
    while (!(USART2->SR & USART_SR_TXE));
      USART2->DR = ( c );
}

void AnalyserSendU16(uint16_t c)
{
    char *p = (char *)&c;
    AnalyserSendChar(*(p+1));
    AnalyserSendChar(*p);
}


void AnalyserSetup(void)
{
    // Setup Output pins    
    AddAnalyserChannel(acUsbAudioDataExchange,      GPIOB, 9);
    AddAnalyserChannel(acUsbAudioInitiateTransmit,  GPIOB, 8);

    AddAnalyserChannel(acUsbAudioInitiateReceive,   GPIOB, 7);
    AddAnalyserChannel(acUsbAudioTransmitComplete,  GPIOB, 6);
    AddAnalyserChannel(acUsbAudioReceiveComplete,   GPIOB, 4);
    AddAnalyserChannel(acUsbAudioError,             GPIOB, 3);
    
    AddAnalyserChannel(acUsbDSP,                    GPIOC, 5);
    AddAnalyserChannel(acUsbAudioSof,               GPIOC, 4);
    AddAnalyserChannel(acUsbFifoRx,                 GPIOB, 1);
    AddAnalyserChannel(acUsbAudioAdjust,            GPIOB, 0);

    AddAnalyserChannel(acDspOverload,               GPIOC, 1);
    AddAnalyserChannel(acPconnection,               GPIOA, 2);
    AddAnalyserChannel(acUsbFifoTx,                 GPIOA, 1);
    AddAnalyserChannel(acUsbFifoRxBuffer,           GPIOA, 0);

    // Setup rx/tx uart
    // palSetPadMode(GPIOA, 3, PAL_MODE_ALTERNATE(7) | PAL_MODE_INPUT); /* RX */
    // palSetPadMode(GPIOA, 2, PAL_MODE_OUTPUT_PUSHPULL); /* TX */
    // palSetPadMode(GPIOA, 2, PAL_MODE_ALTERNATE(7)); /* TX */
    // static const SerialConfig sd2Cfg = {42000000/16, 0, 0, 0};
    // sdStart(&SD2, &sd2Cfg);
}
#endif


#if ANALYSER_CHANNELS > 0

AnalyserChannelData analyserChannels[ANALYSER_CHANNELS];

bool AddAnalyserChannel(AnalyserChannel channel, stm32_gpio_t *port, uint32_t pad)
{
    bool bResult =  false;
    if(channel < ANALYSER_CHANNELS)
    {
        analyserChannels[channel].port = port;
        analyserChannels[channel].pad = pad;

        palSetPadMode(port, pad, PAL_MODE_OUTPUT_PUSHPULL);
        AnalyserSetChannel(channel, false);
    
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

#pragma GCC pop_options

#endif

