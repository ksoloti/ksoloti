
#if FW_USBAUDIO

#include <stdlib.h>
#include <string.h>
#include <limits.h>

#include "hal.h"
#include "audio_usb.h"
#include "audio_usb_dbg.h"
#include "usb_lld.h"
#include "chevents.h"
#include "chdebug.h"
#include "usbcfg.h"

#include "analyser.h"

// do not set higher than -O2
#pragma GCC push_options
#pragma GCC optimize ("O2")
#define FORCE_INLINE __attribute__((always_inline)) inline 

#define RESAMPLE_BUFFER_BLOCKS 3


static int16_t aduTxRingBuffer[TX_RING_BUFFER_FULL_SIZE] __attribute__ ((section (".sram2")));
static int16_t aduRxRingBuffer[TX_RING_BUFFER_FULL_SIZE] __attribute__ ((section (".sram2")));
extern AudioUSBDriver ADU1;
#define N_SAMPLE_RATES  1
const uint32_t aduSampleRates[] = {48000};
static uint8_t aduControlData[8];
static uint8_t aduControlChannel;


/*===========================================================================*/
/* Driver local definitions.                                                 */
/*===========================================================================*/

/*===========================================================================*/
/* Driver exported variables.                                                */
/*===========================================================================*/

/*===========================================================================*/
/* Driver local variables and types.                                         */
/*===========================================================================*/

AduState aduState;

/*===========================================================================*/
/* Driver local functions.                                                   */
/*===========================================================================*/
// something to do with starting stopping patch in ksoloti arseing things up!
// disconnecting usb (closing live), also causing issue

void aduReset(void)
{

  AddOverunLog(ltUSBReset_______);
  aduState.state = asNeedsReset;
}

void  __attribute__((optimize("-O0"))) HandleError(void)
{
  AnalyserSetChannel(acUsbAudioError, true);
  AddOverunLog(ltErrorBefore____);

  // ok we are all out of sync, try to recover
  aduState.state = asNeedsReset;
  AnalyserSetChannel(acUsbAudioError, false);
}

// Set the sample rate
static void __attribute__((optimize("-O0"))) aduSetSampleRate(USBDriver *usbp) 
{
  audio_control_cur_4_t const *pData = (audio_control_cur_4_t const *)&aduControlData[0];
  uint32_t uSampleRate = (uint32_t) pData->bCur;

  if(uSampleRate == 44100 || uSampleRate == 48000 || uSampleRate == 96000)
    aduState.currentSampleRate =  uSampleRate;

  // notify
  chSysLockFromIsr();
  chEvtBroadcastFlagsI(&ADU1.event, AUDIO_EVENT_FORMAT);
  chSysUnlockFromIsr();

}

// Set mute
static void __attribute__((optimize("-O0"))) aduSetMute(USBDriver *usbp) 
{
  aduState.mute[aduControlChannel] = ((audio_control_cur_1_t const *)aduControlData)->bCur;
}

// Set mute
static void __attribute__((optimize("-O0"))) aduSetVolume(USBDriver *usbp) 
{
  aduState.volume[aduControlChannel] = ((audio_control_cur_2_t const *)aduControlData)->bCur;
}

bool __attribute__((optimize("-O0"))) aduHandleVolumeRequest(USBDriver *usbp, audio_control_request_t *request)
{
  // if(uLogCount < LOG_AMOUNT)
  //   memcpy(&requests[uLogCount++], request, sizeof(audio_control_request_t));
  // else
  //   uLogCount = 0;
  bool bResult = false;


  if (request->bControlSelector == AUDIO_FU_CTRL_MUTE && request->bRequest == AUDIO_CS_REQ_CUR)
  {
    // Get and Set mute
    if(request->bmRequestType_bit.direction) // Get requests
    {
      audio_control_cur_1_t mute1 = { .bCur = aduState.mute[request->bChannelNumber] };
      usbSetupTransfer(usbp, (uint8_t *)&mute1, sizeof(mute1), NULL);
      bResult = true;
    }
    else // Set Requests
    {
      aduControlChannel = request->bChannelNumber;
      usbSetupTransfer(usbp, aduControlData, request->wLength, aduSetMute);
      bResult = true;

      // notify
      chSysLockFromIsr();
      chEvtBroadcastFlagsI(&ADU1.event, AUDIO_EVENT_MUTE);
      chSysUnlockFromIsr();
    }
  }
  else if (UAC2_ENTITY_SPK_FEATURE_UNIT && request->bControlSelector == AUDIO_FU_CTRL_VOLUME)
  {
    // Get and Set volume
    if(request->bmRequestType_bit.direction) // Get requests
    {
      switch(request->bRequest)
      {
        case AUDIO_CS_REQ_RANGE:
        {
          audio_control_range_2_n_t(1) rangeVol = {
            .wNumSubRanges = 1,
            .subrange[0] = { .bMin = -VOLUME_CTRL_50_DB, VOLUME_CTRL_0_DB, 256 }
          };
          usbSetupTransfer(usbp, (uint8_t *)&rangeVol, sizeof(rangeVol), NULL);
          bResult = true;
          break;
        }

        case AUDIO_CS_REQ_CUR:
        {
          audio_control_cur_2_t curVol = { .bCur = aduState.volume[request->bChannelNumber] };
          usbSetupTransfer(usbp, (uint8_t *)&curVol, sizeof(curVol), NULL);
          bResult = true;
          break;
        }

        default: break;
      }
    }
    else // Set Requests
    {
      switch(request->bRequest)
      {
        case AUDIO_CS_REQ_CUR:
        {
          aduControlChannel = request->bChannelNumber;
          usbSetupTransfer(usbp, aduControlData, request->wLength, aduSetVolume);
          bResult = true;

          // notify
          chSysLockFromIsr();
          chEvtBroadcastFlagsI(&ADU1.event, AUDIO_EVENT_VOLUME);
          chSysUnlockFromIsr();

          break;
        }

        default: break;
      }
    }

  } 


  return bResult;
}

bool __attribute__((optimize("-O0"))) aduHandleClockRequest(USBDriver *usbp, audio_control_request_t *request)
{
  bool bResult = false;

  if (request->bControlSelector == AUDIO_CS_CTRL_SAM_FREQ)
  {
    if(request->bmRequestType_bit.direction) // Get requests
    {
      switch(request->bRequest)
      {
        case AUDIO_CS_REQ_CUR:
        {
          audio_control_cur_4_t curf = { (int32_t) aduState.currentSampleRate };
          usbSetupTransfer(usbp, (uint8_t *)&curf, sizeof(curf), NULL);
          bResult = true;
          break;
        }
      
        case AUDIO_CS_REQ_RANGE:
        {
          // Get sample rate range.
          volatile audio_control_range_4_n_t(N_SAMPLE_RATES) rangef; // optimiser is messing this code up, needs volatile
          rangef.wNumSubRanges = (uint16_t)N_SAMPLE_RATES;
          
          uint8_t i;
          for(i = 0; i < N_SAMPLE_RATES; i++)
          {
            rangef.subrange[i].bMin = (int32_t) aduSampleRates[i];
            rangef.subrange[i].bMax = (int32_t) aduSampleRates[i];
            rangef.subrange[i].bRes = 0;
          }
          usbSetupTransfer(usbp, (uint8_t *)&rangef, sizeof(rangef), NULL);
          bResult = true;
          break;
        }

        default : break;
      }
    }
    else // Set requests
    {
      switch(request->bRequest)
      {
        case AUDIO_CS_REQ_CUR:
        {
          usbSetupTransfer(usbp, aduControlData, request->wLength, aduSetSampleRate);
          bResult = true;
          break;
        }

        default: break;
      }
    }
  }
  else if (request->bControlSelector == AUDIO_CS_CTRL_CLK_VALID && request->bRequest == AUDIO_CS_REQ_CUR)
  {
    audio_control_cur_1_t cur_valid = { .bCur = 1 };
    usbSetupTransfer(usbp, (uint8_t *)&cur_valid, sizeof(cur_valid), NULL);
    bResult = true;
  }
 
  return bResult;
}

bool __attribute__((optimize("-O0"))) aduControl(USBDriver *usbp)
{
  audio_control_request_t *acrp = (audio_control_request_t *)usbp->setup;

  if (acrp->bInterface == ITF_NUM_AUDIO_STREAMING_CONTROL) 
  {
    switch(acrp->bEntityID)
    {
      case AUDIO_FUNCTION_UNIT_ID:
      {
        return aduHandleVolumeRequest(usbp, acrp);
        break;
      }

      case UAC2_ENTITY_CLOCK:
      {
        return aduHandleClockRequest(usbp, acrp);
        break;
      }
      default: break;
    }
  }
  return false;
}


//                                       4              5               1            (3 << 8) | 2     6
bool __attribute__((optimize("-O0"))) aduSwitchInterface(USBDriver *usbp, uint8_t iface, uint8_t entity, uint8_t req, uint16_t wValue, uint16_t length) 
{
  bool bResult = false;
 
  if(entity == 0)
  {
    if(iface == ITF_NUM_AUDIO_STREAMING_SPEAKER)
    {
      aduEnableOutput(usbp, wValue);
      bResult = true;
    }
    else if(iface == ITF_NUM_AUDIO_STREAMING_MICROPHONE)
    {
      aduEnableInput(usbp, wValue);
      bResult = true;
    }
  }

  if(bResult)
    usbSetupTransfer(usbp, NULL, 0, NULL);

  return bResult;
}


/*===========================================================================*/
/* Driver exported functions.                                                */
/*===========================================================================*/

/**
 * @brief   Audio USB Driver initialization.
 * @note    This function is implicitly invoked by @p halInit(), there is
 *          no need to explicitly initialize the driver.
 *
 * @init
 */

// Note this is never called by halInit(), the above comment is incorrect
void __attribute__((optimize("-O0"))) aduInit(void) 
{
}
 
/**
 * @brief   Initializes a audio driver
 * @details The HW dependent part of the initialization has to be performed
 *          outside, usually in the hardware initialization code.
 *
 * @param[out] adup     pointer to a @p AudioUSBDriver structure
 *
 * @init
 */
void __attribute__((optimize("-O0"))) aduObjectInit(AudioUSBDriver *adup)
{
  // default sample rate
  aduState.currentSampleRate = 48000;
  
  // default is disabled
  aduState.isOutputActive = false;
  aduState.isInputActive = false;
  
  // frame stuff
  aduResetBuffers();
  
  for(int iC = 0 ; iC < USB_AUDIO_CHANNELS+1; iC++)
  {
    // set not muted
    aduState.mute[iC] = 0;

    // set 0db volume
    aduState.volume[iC] = VOLUME_CTRL_0_DB;
  }

  adup->vmt = NULL; // none at the moment
  chEvtInit(&adup->event);
  adup->state = ADU_STOP;
}

/**
 * @brief   Configures and starts the driver.
 *
 * @param[in] bdup      pointer to a @p AudioUSBDriver object
 * @param[in] config    the Audio USB driver configuration
 *
 * @api
 */
void __attribute__((optimize("-O0"))) aduStart(AudioUSBDriver *adup, const AudioUSBConfig *config) 
{
  USBDriver *usbp = config->usbp;

  chDbgCheck(adup != NULL);

  chSysLock()
  ;
  chDbgAssert((adup->state == ADU_STOP) || (adup->state == ADU_READY),
              "aduStart(), #1 invalid state");
  usbp->in_params[config->iso_in - 1] = adup;
  usbp->out_params[config->iso_out - 1] = adup;
  adup->config = config;
  adup->state = ADU_READY;
  chSysUnlock()
  ;

}

/**
 * @brief   Stops the driver.
 * @details Any thread waiting on the driver's queues will be awakened with
 *          the message @p Q_RESET.
 *
 * @param[in] adup      pointer to a @p AudioUSBDriver object
 *
 * @api
 */
void __attribute__((optimize("-O0"))) aduStop(AudioUSBDriver *adup) 
{
  USBDriver *usbp = adup->config->usbp;

  chDbgCheck(adup != NULL);

  chSysLock()
  ;

  chDbgAssert((adup->state == ADU_STOP) || (adup->state == ADU_READY),
              "aduStop(), #1 invalid state");

  /* Driver in stopped state.*/
  usbp->in_params[adup->config->iso_in - 1] = NULL;
  usbp->out_params[adup->config->iso_out - 1] = NULL;
  adup->state = ADU_STOP;

  /* Queues reset in order to signal the driver stop to the application.*/
  chnAddFlagsI(adup, CHN_DISCONNECTED);
  chSchRescheduleS();

  chSysUnlock()
  ;
}
 
/**
 * @brief   USB device configured handler.
 *
 * @param[in] adup      pointer to a @p AudioUSBDriver object
 *
 * @iclass
 */
void __attribute__((optimize("-O0"))) aduConfigureHookI(AudioUSBDriver *adup) 
{
  chnAddFlagsI(adup, CHN_CONNECTED);
}

/**
 * @brief   Default requests hook.
 * @details Applications wanting to use the Audio USB driver can use
 *          this function as requests hook in the USB configuration.
 *          The following requests are emulated:
 *          - CDC_GET_LINE_CODING.
 *          - CDC_SET_LINE_CODING.
 *          - CDC_SET_CONTROL_LINE_STATE.
 *          .
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 * @return              The hook status.
 * @retval TRUE         Message handled internally.
 * @retval FALSE        Message not handled.
 */
bool_t __attribute__((optimize("-O0"))) aduRequestsHook(USBDriver *usbp) {

  (void)usbp;
  return FALSE;
}


void aduSofHookI(AudioUSBDriver *adup)
{
  AnalyserTriggerChannel(acUsbAudioSof);
}


void __attribute__((optimize("-O0"))) aduResetBuffers(void)
{
  aduState.currentFrame               = 0;
  aduState.lastOverunFrame            = 0;
  aduState.sampleAdjustEveryFrame     = 0;
  aduState.sampleAdjustFrameCounter   = 0; 
  aduState.sampleOffset               = 0;
  aduState.codecMetricsSampleOffset   = 0;
  aduState.codecMetricsBlocksOkCount  = 0;
  aduState.txRingBufferWriteOffset    = 0;
  aduState.txRingBufferReadOffset     = 0;
  aduState.txRingBufferUsedSize       = 0;
  aduState.rxRingBufferWriteOffset    = 0;
  aduState.rxRingBufferReadOffset     = 0;
  aduState.rxRingBufferUsedSize       = 0;
  aduState.codecFrameSampleCount      = 0;
  aduState.state                      = asInit;

  memset(aduTxRingBuffer, 0, sizeof(aduTxRingBuffer));

  AddOverunLog(ltResetForSync___);
}

void __attribute__((optimize("-O0"))) aduEnable(USBDriver *usbp)
{
  if(aduIsUsbInUse())
  {
    aduInitiateReceiveI(usbp);
    aduInitiateTransmitI(usbp);
  }
  else
  {
    aduResetBuffers();
  }
}

void __attribute__((optimize("-O0"))) aduEnableInput(USBDriver *usbp, bool bEnable)
{
  // this is ksoloti->host
  if(bEnable != aduState.isInputActive)
  {
    aduState.isInputActive = bEnable;

    // if(!aduState.isInputActive)
    //   memset(aduTxRingBuffer, 0, sizeof(aduTxRingBuffer));

    chSysLockFromIsr();
    chEvtBroadcastFlagsI(&ADU1.event, AUDIO_EVENT_INPUT);
    aduEnable(usbp);
    chSysUnlockFromIsr();
  }

  if (!bEnable) {
    usb_clearbuffer();
  }
}

void __attribute__((optimize("-O0"))) aduEnableOutput(USBDriver *usbp, bool bEnable)
{
  // this is host->ksoloti
  if(bEnable != aduState.isOutputActive)
  {
    aduState.isOutputActive = bEnable;
    chSysLockFromIsr();
    chEvtBroadcastFlagsI(&ADU1.event, AUDIO_EVENT_OUTPUT);
    aduEnable(usbp);
    chSysUnlockFromIsr();
  }

  if (!bEnable) {
    usb_clearbuffer();
  }
}





#if CHECK_USB_DATA
uint16_t aduSkippedTxSamplesStart = 0;
int16_t  aduSkippedTxSampleValue = 0;
uint16_t aduAddedTxSamplesStart = 0;
int16_t  aduAddedTxSampleValue = 0;

uint16_t aduSkippedRxSamplesStart = 0;
int16_t  aduSkippedRxSampleValue = 0;
uint16_t aduAddedRxSamplesStart = 0;
int16_t  aduAddedRxSampleValue = 0;
#endif

static FORCE_INLINE void aduMoveDataToTX(int32_t *pData, uint_fast16_t uLen)
{
  if(aduState.txRingBufferUsedSize+uLen > TX_RING_BUFFER_NORMAL_SIZE)
  {
    //HandleError();
    if(aduState.txRingBufferUsedSize+uLen > TX_RING_BUFFER_FULL_SIZE)
    {
      // really bad 
      HandleError();
    }
  }

  uint_fast16_t uSamplesBeforeWrap = TX_RING_BUFFER_FULL_SIZE - aduState.txRingBufferWriteOffset;
  int16_t *pSrc = ((int16_t *)pData)+1;
  int16_t *pDst = &(aduTxRingBuffer[aduState.txRingBufferWriteOffset]);

  uint_fast16_t uStage1Samples = MIN(uLen, uSamplesBeforeWrap);
  uint_fast16_t uStage2Samples = uLen - uStage1Samples;

  // stage 1
  uint_fast16_t u; 
  for(u = 0; u < uStage1Samples; u++)
  {
    *pDst++ = *pSrc;
    pSrc+=2;
  }

  if(uStage2Samples)
  {
    // stage 2
    pDst = &(aduTxRingBuffer[0]);

    for(u = 0; u < uStage2Samples; u++)
    {
      *pDst++ = *pSrc;
      pSrc+=2;
    }
  }
  
  aduState.txRingBufferWriteOffset = (aduState.txRingBufferWriteOffset + uLen) % TX_RING_BUFFER_FULL_SIZE;
  aduState.txRingBufferUsedSize+=uLen;
}

static FORCE_INLINE void aduMoveDataFromRX(int32_t *pData, uint_fast16_t uLen)
{
  // RX check
  if(aduState.rxRingBufferUsedSize < uLen)
    HandleError();

  uint_fast16_t uSamplesBeforeWrap = TX_RING_BUFFER_FULL_SIZE - aduState.rxRingBufferReadOffset;
  int32_t *pDst = pData;
  int16_t *pSrc = &(aduRxRingBuffer[aduState.rxRingBufferReadOffset]);

  uint_fast16_t uStage1Samples = MIN(uLen, uSamplesBeforeWrap);
  uint_fast16_t uStage2Samples = uLen - uStage1Samples;

  // stage 1
  uint_fast16_t u; 
  for(u = 0; u < uStage1Samples; u++)
  {
    *pDst++ = *pSrc++ << 16;
  }

  if(uStage2Samples)
  {
    // stage 2
    pSrc = &(aduRxRingBuffer[0]);

    for(u = 0; u < uStage2Samples; u++)
    {
      *pDst++ = *pSrc++ << 16;
    }
  }
  aduState.rxRingBufferReadOffset = (aduState.rxRingBufferReadOffset + uLen) % TX_RING_BUFFER_FULL_SIZE;
  aduState.rxRingBufferUsedSize -= uLen;
}



/**
 * @brief   Removes a sample for each usb RX channel and resamples 
 *          48 to 47 samples in usb rx
 */

void aduRemoveRxSample(void)
{
  int16_t *inBuffer   = &aduRxRingBuffer[aduState.rxRingBufferReadOffset];
  int16_t *endBuffer  = &aduRxRingBuffer[TX_RING_BUFFER_FULL_SIZE];

  float fD = 1.0f/(47-1);
  uint32_t ufD = 65535 * fD;
  uint32_t ufIndex = 0;

  int16_t *pSample     = inBuffer;
  int16_t *pNextSample = inBuffer + USB_AUDIO_CHANNELS;

  int16_t nTemp[4] = {inBuffer[0], inBuffer[1], inBuffer[2], inBuffer[3]};
  int16_t nTempOld[4];
  
  for(uint_fast16_t i = 0; i < 47; i++)
  {
    for(uint_fast16_t j=0; j < USB_AUDIO_CHANNELS; j++)
    {
      if(pSample == endBuffer)
        pSample = aduRxRingBuffer;
      
      if(pNextSample == endBuffer)
        pNextSample = aduRxRingBuffer;
      
      nTempOld[j] = nTemp[j];
      nTemp[j] = *pNextSample;

      *pNextSample = (((int32_t)(nTempOld[j]) * (65535 - ufIndex)) + (((int32_t)*pNextSample) * ufIndex)) >> 16;
      pSample++;
      pNextSample++;
    }

    ufIndex += ufD;
  }
  
  aduState.rxRingBufferReadOffset = (aduState.rxRingBufferReadOffset+USB_AUDIO_CHANNELS) % TX_RING_BUFFER_FULL_SIZE;
  aduState.rxRingBufferUsedSize -= USB_AUDIO_CHANNELS;
}


/**
 * @brief   Adds a sample for each usb RX channel and resamples 
 *          48 to 49 samples in usb rx
 */

void aduAddRxSample(void)
{
  int16_t *endBuffer  = &aduRxRingBuffer[TX_RING_BUFFER_FULL_SIZE];

  float fD = 1.0f/(49-1);
  uint32_t ufD = 65535 * fD;
  uint32_t ufIndex = 0;

  // We are setting samples back in time before adjusting buffer pos
  int16_t *pSetSample;
  if(aduState.rxRingBufferReadOffset== 0)
    pSetSample = &aduRxRingBuffer[TX_RING_BUFFER_FULL_SIZE-USB_AUDIO_CHANNELS];
  else
    pSetSample = &aduRxRingBuffer[aduState.rxRingBufferReadOffset-USB_AUDIO_CHANNELS];
  
  for(uint_fast16_t i = 0; i < 49; i++)
  {
    uint32_t ufUseIndex = ufIndex * (48-1);
    uint32_t uIndex = USB_AUDIO_CHANNELS * (ufUseIndex >> 16);
    ufUseIndex = ufUseIndex &0xffff;
        
    int16_t uSourceSampleIndex = aduState.rxRingBufferReadOffset+uIndex;
    if(uSourceSampleIndex >= TX_RING_BUFFER_FULL_SIZE)
      uSourceSampleIndex -= TX_RING_BUFFER_FULL_SIZE;
    
    int16_t *pSample     = &aduRxRingBuffer[uSourceSampleIndex];
    int16_t *pNextSample = pSample + USB_AUDIO_CHANNELS;

    if(pNextSample == endBuffer)
      pNextSample = aduRxRingBuffer;

    if(pSetSample == endBuffer)
      pSetSample = aduRxRingBuffer;

    for(uint_fast16_t j=0; j < USB_AUDIO_CHANNELS; j++)
    {
      *pSetSample = (((int32_t)(*pSample) * (65535 - ufUseIndex)) + (((int32_t)*pNextSample) * ufUseIndex)) >> 16;
      pSample++;
      pNextSample++;
      pSetSample++;
    }

    ufIndex += ufD;
  }

   if(aduState.rxRingBufferReadOffset == 0)
     aduState.rxRingBufferReadOffset = TX_RING_BUFFER_FULL_SIZE - USB_AUDIO_CHANNELS;
   else
     aduState.rxRingBufferReadOffset -= USB_AUDIO_CHANNELS;

   aduState.rxRingBufferUsedSize += USB_AUDIO_CHANNELS;
}


/**
 * @brief   Strectch/Resamples int32 samples in place, resulting data is offset by -USB_AUDIO_CHANNELS samples
 * @details 
 *
 * @param[in] inBuffer    pointer to the data
 */

void aduStretchInt32Buffer(int32_t *inBuffer)
{
  float fD = 1.0f/(49-1);
  uint32_t ufD = 65535 * fD;
  uint32_t ufIndex = 0;

  int32_t *pSetSample  = inBuffer-USB_AUDIO_CHANNELS; // need space at start

  for(uint_fast16_t i = 0; i < 49; i++)
  {
    uint32_t ufUseIndex = ufIndex * (48-1);
    uint32_t uIndex = USB_AUDIO_CHANNELS * (ufUseIndex >> 16);
    ufUseIndex = ufUseIndex &0xffff;
        
    int32_t *pSample     = &(inBuffer[uIndex]);
    int32_t *pNextSample = pSample + USB_AUDIO_CHANNELS;

    for(uint_fast16_t j=0; j < USB_AUDIO_CHANNELS; j++)
    {
      *pSetSample = ((*pSample >> 16) * (65535 - ufUseIndex)) + ((*pNextSample >> 16) * ufUseIndex);
      pSample++;
      pNextSample++;
      pSetSample++;
    }

    ufIndex += ufD;
  }
}

/**
 * @brief   Shrink/Resamples int32 samples in place
 * @details 
 *
 * @param[in] inBuffer    pointer to the data
 */

void aduShrinkInt32BufferNormal(int32_t *inBuffer)
{
  float fD = 1.0f/(47-1);

  uint32_t ufD = 65535 * fD;
  uint32_t ufIndex = 0;

  int32_t *pSetSample  = inBuffer;
  int32_t *pSample     = inBuffer;
  int32_t *pNextSample = inBuffer + USB_AUDIO_CHANNELS;
  
  for(uint_fast16_t i = 0; i < 47; i++)
  {
    for(uint_fast16_t j=0; j < USB_AUDIO_CHANNELS; j++)
    {
      *pSetSample = ((*pSample >> 16) * (65535 - ufIndex)) + ((*pNextSample >> 16) * ufIndex);

      pSample++;
      pNextSample++;
      pSetSample++;
    }

    ufIndex += ufD;
  }
}



/**
 * @brief   Handles transfer of data between codec and USB, resampling
 * @details 
 *
 * @param[in] in    pointer to in data  <- Codec
 * @param[in] out   pointer to out data -> USB
 */

void aduDataExchangeResample (int32_t *in, int32_t *out)
{
  if(aduIsUsbOutputEnabled())
  {
    AnalyserSetChannel(acUsbAudioDataExchange, true);
#if USB_AUDIO_CHANNELS == 2
    uint16_t uBufferSize   = 32;
    uint16_t uBufferAdjust = 2;
    static int32_t txResampleBufferData[2 + (32 * RESAMPLE_BUFFER_BLOCKS)];
    static int32_t *txResampleBuffer = &txResampleBufferData[2];
#elif  USB_AUDIO_CHANNELS == 4
    uint16_t uBufferSize   = 64;
    uint16_t uBufferAdjust = 4;
    static int32_t txResampleBufferData[4 + (64 * RESAMPLE_BUFFER_BLOCKS)];
    static int32_t *txResampleBuffer = &txResampleBufferData[4];
#endif  
    static uint16_t uTxBufferBlockCount = 0;
    static uint16_t uTxResampleBufferLen = 0;
    static uint16_t uTxResampleBufferPos = 0;

    uint16_t uLen = uBufferSize;
    uint16_t uFeedbackLen = uLen;


    uTxBufferBlockCount++;

    /////////////////////////////////
    // codec -> USB
    /////////////////////////////////
    if(aduState.state == asCodecRemove)
    {
      // remove uBufferAdjust samples
      //AnalyserTriggerChannel(acUsbAudioAdjust);
      uLen -= uBufferAdjust;
    } 
    else if(aduState.state == asCodecDuplicate)
    {
      //AnalyserTriggerChannel(acUsbAudioAdjust);
      // add uBufferAdjust samples 

      #if CHECK_USB_DATA
        aduAddedTxSamplesStart = aduState.txRingBufferWriteOffset;
        aduAddedTxSampleValue = out[0]>>16;
      #endif // CHECK_USB_DATA

      // aduMoveDataToTX(out, uBufferAdjust);
      uTxResampleBufferLen += uBufferAdjust;
    }

    // copy into resample buffer
    // we need to copy everything
    //AnalyserSetChannel(acUsbAudioAdjust, true);
    for(uint16_t u=0; u < uBufferSize; u++)
    {
      txResampleBuffer[uTxResampleBufferPos++] = out[u];
    }
    //AnalyserSetChannel(acUsbAudioAdjust, false);
    
    uTxResampleBufferLen += uLen;

    if(uTxBufferBlockCount == RESAMPLE_BUFFER_BLOCKS)
    {
      if(uTxResampleBufferLen != (RESAMPLE_BUFFER_BLOCKS * uBufferSize))
      {
        AnalyserSetChannel(acUsbAudioAdjust, true);
        // we need to resample here
        if(uTxResampleBufferLen > (RESAMPLE_BUFFER_BLOCKS * uBufferSize)) 
        {
          aduStretchInt32Buffer(txResampleBuffer);
          aduMoveDataToTX(txResampleBufferData, uTxResampleBufferLen);
          aduRemoveRxSample();
        }
        else
        {
          aduShrinkInt32BufferNormal(txResampleBuffer);
          aduMoveDataToTX(txResampleBuffer, uTxResampleBufferLen);
          aduAddRxSample();
        }
        AnalyserSetChannel(acUsbAudioAdjust, false);
      }
      else
        aduMoveDataToTX(txResampleBuffer, uTxResampleBufferLen);

      uTxBufferBlockCount = 0;
      uTxResampleBufferLen = 0;
      uTxResampleBufferPos = 0;
    }

    aduState.codecFrameSampleCount+=uFeedbackLen;

    #if CHECK_USB_DATA
      if(uLen < uBufferSize)
      {
        aduSkippedTxSamplesStart = aduState.txRingBufferWriteOffset;
        aduSkippedTxSampleValue = out[uLen+1] >> 16;
      }
    #endif // CHECK_USB_DATA


    /////////////////////////////////
    // USB -> codec
    /////////////////////////////////
    if(aduState.state > asFillingUnderflow)
    {
      AddOverunLog(ltCodecCopyStart_);

      // always copy uBufferSize samples
      aduMoveDataFromRX(in, uBufferSize);

      #if CHECK_USB_DATA
        // DEBUG test USB Data, requires USBOutputTest.axp running on Ksoloiti
        bool bOk = true;
        volatile int16_t tmpCodecData[14];

        uint_fast16_t u; for(u = 0; u < 14; u++)
        {
          int16_t nV1 = in[(u*USB_AUDIO_CHANNELS)+USB_AUDIO_CHANNELS] >> 16;
          int16_t nV2 = in[(u*USB_AUDIO_CHANNELS)] >> 16;
          
          uint32_t uDiff = abs(nV1 - nV2);
          tmpCodecData[u] = (nV1 - nV2);
          if(uDiff > 300)
          {
            bOk = false;
            //HandleError();
          }
        }
      #endif // CHECK_USB_DATA

      if((aduState.state == asCodecRemove) || (aduState.state == asCodecDuplicate))
        aduState.state = asNormal;

      AddOverunLog(ltCodecCopyEnd___);
    }
  }
  AnalyserSetChannel(acUsbAudioDataExchange, false);
}


/**
 * @brief   Handles transfer of data between codec and USB, no resampling
 * @details 
 *
 * @param[in] in    pointer to in data  <- Codec
 * @param[in] out   pointer to out data -> USB
 */

void aduDataExchangeNoResample (int32_t *in, int32_t *out)
{
  if(aduIsUsbOutputEnabled())
  {
    AnalyserSetChannel(acUsbAudioDataExchange, true);
#if USB_AUDIO_CHANNELS == 2
    uint16_t uBufferSize   = 32;
    uint16_t uBufferAdjust = 2;
#elif  USB_AUDIO_CHANNELS == 4
    uint16_t uBufferSize   = 64;
    uint16_t uBufferAdjust = 4;
#endif   
    uint16_t uLen = uBufferSize;
    uint16_t uFeedbackLen = uLen;

    /////////////////////////////////
    // codec -> USB
    /////////////////////////////////
    if(aduState.state == asCodecRemove)
    {
      // remove uBufferAdjust samples
      uLen -= uBufferAdjust;
      AnalyserTriggerChannel(acUsbAudioAdjust);
    } 
    else if(aduState.state == asCodecDuplicate)
    {
      AnalyserTriggerChannel(acUsbAudioAdjust);
      // add uBufferAdjust samples 

      #if CHECK_USB_DATA
        aduAddedTxSamplesStart = aduState.txRingBufferWriteOffset;
        aduAddedTxSampleValue = out[0]>>16;
      #endif // CHECK_USB_DATA

      aduMoveDataToTX(out, uBufferAdjust);
    }

    aduMoveDataToTX(out, uLen);

    aduState.codecFrameSampleCount+=uFeedbackLen;

    #if CHECK_USB_DATA
      if(uLen < uBufferSize)
      {
        aduSkippedTxSamplesStart = aduState.txRingBufferWriteOffset;
        aduSkippedTxSampleValue = out[uLen+1] >> 16;
      }
    #endif // CHECK_USB_DATA


    /////////////////////////////////
    // USB -> codec
    /////////////////////////////////
    if(aduState.state > asFillingUnderflow)
    {
      AddOverunLog(ltCodecCopyStart_);

      // always copy uBufferSize samples
      aduMoveDataFromRX(in, uBufferSize);

      // adjustments 
      if(aduState.state == asCodecDuplicate)
      {
        // uBufferAdjust two many in USB buffer

        #if CHECK_USB_DATA
          aduSkippedRxSamplesStart = aduState.rxRingBufferReadOffset;
          aduSkippedRxSampleValue = aduRxRingBuffer[aduSkippedRxSamplesStart];
        #endif // CHECK_USB_DATA

        aduState.rxRingBufferReadOffset = (aduState.rxRingBufferReadOffset+uBufferAdjust) % TX_RING_BUFFER_FULL_SIZE;
        aduState.rxRingBufferUsedSize -= uBufferAdjust;
      }
      else if(aduState.state == asCodecRemove)
      {
        // two  little in USB buffer
        #if CHECK_USB_DATA
          aduAddedRxSamplesStart = aduState.rxRingBufferReadOffset;
          aduAddedRxSampleValue = aduRxRingBuffer[aduSkippedRxSamplesStart];
        #endif // CHECK_USB_DATA

        if(aduState.rxRingBufferReadOffset == 0)
          aduState.rxRingBufferReadOffset = TX_RING_BUFFER_FULL_SIZE - uBufferAdjust;
        else
          aduState.rxRingBufferReadOffset -= uBufferAdjust;

        aduState.rxRingBufferUsedSize += uBufferAdjust;
      }


      #if CHECK_USB_DATA
        // DEBUG test USB Data, requires USBOutputTest.axp running on Ksoloiti
        bool bOk = true;
        volatile int16_t tmpCodecData[14];

        uint_fast16_t u; for(u = 0; u < 14; u++)
        {
          int16_t nV1 = in[(u*USB_AUDIO_CHANNELS)+USB_AUDIO_CHANNELS] >> 16;
          int16_t nV2 = in[(u*USB_AUDIO_CHANNELS)] >> 16;
          
          uint32_t uDiff = abs(nV1 - nV2);
          tmpCodecData[u] = (nV1 - nV2);
          if(uDiff > 300)
          {
            bOk = false;
            //HandleError();
          }
        }
      #endif // CHECK_USB_DATA

      if((aduState.state == asCodecRemove) || (aduState.state == asCodecDuplicate))
        aduState.state = asNormal;

      AddOverunLog(ltCodecCopyEnd___);
    }
  }
  AnalyserSetChannel(acUsbAudioDataExchange, false);
}


/**
 * @brief   End of last USB frame
 * @details Handles codec metrics
 */
FORCE_INLINE void aduCodecFrameEnded(void)
{
  // USB clock and Codec clock will be different
  // we can get underruns and overruns.
  // also the clocks slide against each other so we can get
  // jitter in the number of codec frames received
  // per USB frame, 1, 2, 3, 0r 4.
  //
  // we want to distort the USB stream as little as possible
  // so we can't adjust the sample counts based on the codec clock
  // or on the USB clock

  // we want to try to maintain TX_RING_BUFFER_NORMAL_SIZE samples available
  // at the end of this method,

  // we must never go under USE_TRANSFER_SIZE_BYTES samples available
  // at the end of this method

  // increment current usb frame
  aduState.currentFrame++;

#if USB_AUDIO_CHANNELS == 2
  int16_t nFrameSampleOffest = (int16_t)(aduState.codecFrameSampleCount)-96;
#elif USB_AUDIO_CHANNELS == 4
  int16_t nFrameSampleOffest = (int16_t)(aduState.codecFrameSampleCount)-192;
#endif

  aduState.codecMetricsSampleOffset += nFrameSampleOffest;

  if(0 == (aduState.currentFrame % CODEC_METICS_MS))
  {
    if(aduState.codecMetricsSampleOffset != 0)
    {
      // ok we are out of sync, adjust to sync
      aduState.sampleOffset += aduState.codecMetricsSampleOffset;
      uint16_t uUseBlocks   = aduState.codecMetricsBlocksOkCount+1;

      // make recovery 1 block quicker
      // if(uUseBlocks > 1)
      //   uUseBlocks -=1;

      // calculate sample adjust counter
      aduState.sampleAdjustEveryFrame = (CODEC_METICS_MS*uUseBlocks) / ((abs(aduState.sampleOffset)>>1));
#if USB_AUDIO_CHANNELS == 4
      // If using 4 channels the total adustment will take half the time, so alter.
      aduState.sampleAdjustEveryFrame *= 2;
#endif      
      // aduState.sampleAdjustEveryFrame = 100000; // ARCFATAL, not to do with overrun/overrun adjustment!!!!
      aduState.sampleAdjustFrameCounter = aduState.sampleAdjustEveryFrame;

      // reset
      aduState.codecMetricsBlocksOkCount = 0;

    }
    else
    {
      // block is ok so increment counter
      aduState.codecMetricsBlocksOkCount++;
    }

    aduState.codecMetricsSampleOffset = 0;
  }

  AddOverunLog(ltFrameEndedEnd__);

  // we need some checks here for debugging
  if(aduState.state > asFillingUnderflow)
  {
    volatile uint16_t uTXCalcSize;
    if((aduState.txRingBufferWriteOffset < aduState.txRingBufferReadOffset))
      uTXCalcSize = (aduState.txRingBufferWriteOffset + TX_RING_BUFFER_FULL_SIZE) - aduState.txRingBufferReadOffset;
    else
      uTXCalcSize = aduState.txRingBufferWriteOffset - aduState.txRingBufferReadOffset;

    if(uTXCalcSize != aduState.txRingBufferUsedSize)
      HandleError();

    volatile uint16_t uRXCalcSize;
    if((aduState.rxRingBufferWriteOffset < aduState.rxRingBufferReadOffset))
    {
      uRXCalcSize = (aduState.rxRingBufferWriteOffset + TX_RING_BUFFER_FULL_SIZE) - aduState.rxRingBufferReadOffset;
    }
    else
      uRXCalcSize = aduState.rxRingBufferWriteOffset - aduState.rxRingBufferReadOffset;

    if(uRXCalcSize != aduState.rxRingBufferUsedSize)
      HandleError();
  }

}

/**
 * @brief   Start of a new USB frame
 * @details Sets up adjustments if needed for underun/overun
 */
FORCE_INLINE void aduCodecFrameStarted(void)
{
  aduState.codecFrameSampleCount = 0;
  
  if( aduState.sampleOffset != 0)
  {
    if(aduState.sampleAdjustFrameCounter == 0)
    {
#if USB_AUDIO_CHANNELS == 2
    uint16_t uBufferAdjust = 2;
#elif  USB_AUDIO_CHANNELS == 4
    uint16_t uBufferAdjust = 4;
#endif   
      if(aduState.sampleOffset > 0)
      {
        // adjust overrun
        // chuck samples awway
        aduState.sampleOffset-=uBufferAdjust;
        aduState.state = asCodecRemove;
      }
      else
      {
        // adjust underrun
        // duplicate sample
        aduState.sampleOffset+=uBufferAdjust;
        aduState.state = asCodecDuplicate;
      }

      // check for finish or restert
      if(aduState.sampleOffset)
      {
        aduState.sampleAdjustFrameCounter = aduState.sampleAdjustEveryFrame;
      }
      else
      {
        aduState.sampleAdjustFrameCounter = aduState.sampleAdjustEveryFrame = 0;
      }

      AddOverunLog(ltSampleAdjusted_);
    }
    else
      aduState.sampleAdjustFrameCounter--;
  }

}

/**
 * @brief   Inititate a USB transmit.
 * @details Will transmit the data at the next SOF.
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 */
void aduInitiateTransmitI(USBDriver *usbp)
{
  AnalyserSetChannel(acUsbAudioInitiateTransmit, true);
  // tell codec copy that USB frame has ended
  aduCodecFrameEnded();

  int16_t *pTxLocation = NULL;
  if(aduState.state == asNeedsReset)
  {
    aduResetBuffers(); // sets to asInit
  }

  if(aduState.state == asInit || aduState.state == asFillingUnderflow)
  {
    // send silence
    pTxLocation = (aduTxRingBuffer + TX_RING_BUFFER_FULL_SIZE) - USE_TRANSFER_SIZE_SAMPLES;

    // wait for unflow buffer to be filled and synced
    uint16_t uRxSamplesRequired = USE_TRANSFER_SIZE_SAMPLES*2;
    if((aduState.txRingBufferUsedSize == TX_RING_BUFFER_NORMAL_SIZE) && (aduState.rxRingBufferUsedSize >= uRxSamplesRequired))
    {
      // adjust the size of the RX buffer so we are in sync, we want USE_TRANSFER_SIZE_SAMPLES samples
      if(aduState.rxRingBufferUsedSize > uRxSamplesRequired)
      {
        if(aduState.rxRingBufferWriteOffset >= uRxSamplesRequired)
          aduState.rxRingBufferReadOffset = aduState.rxRingBufferWriteOffset - uRxSamplesRequired;
        else
          aduState.rxRingBufferReadOffset = (aduState.rxRingBufferWriteOffset + TX_RING_BUFFER_FULL_SIZE) - uRxSamplesRequired;

        aduState.rxRingBufferUsedSize = uRxSamplesRequired;
      }

      AddOverunLog(ltTxRxSynced_____);
      aduState.state = asNormal;
    }
    else if(aduState.txRingBufferUsedSize > TX_RING_BUFFER_NORMAL_SIZE)
    {
      aduResetBuffers();
    }
    AddOverunLog(ltWaitingForSync_);
  }

  // transmit from buffer, increase read offset
  if(aduState.state > asFillingUnderflow)
  {
    if(aduState.txRingBufferUsedSize < USE_TRANSFER_SIZE_SAMPLES)
    {
      HandleError();
    }

    // set transmit location
    pTxLocation = aduTxRingBuffer + aduState.txRingBufferReadOffset;

    // increase and wrap read offset
    aduState.txRingBufferReadOffset += USE_TRANSFER_SIZE_SAMPLES;
    if(aduState.txRingBufferReadOffset == TX_RING_BUFFER_FULL_SIZE)
      aduState.txRingBufferReadOffset = 0;

    // decrease buffer used size
    aduState.txRingBufferUsedSize -= USE_TRANSFER_SIZE_SAMPLES;
  }

  AddOverunLog(ltAfterTXAdjust__);

  // tell codec copy that USB frame has started
  aduCodecFrameStarted();

#if CHECK_USB_DATA
  // DEBUG test USB Data, requires USBOutputTest.axp running on Ksoloiti
  volatile int16_t tmpData[46]; 

  bool bOk = true;
  uint16_t u; for( u = 0; u < 46; u++)
  {
    uint32_t uDiff = abs(pTxLocation[(u*USB_AUDIO_CHANNELS)+USB_AUDIO_CHANNELS] - pTxLocation[u*USB_AUDIO_CHANNELS]);
    tmpData[u] = (pTxLocation[(u*USB_AUDIO_CHANNELS)+USB_AUDIO_CHANNELS] - pTxLocation[u*USB_AUDIO_CHANNELS]);
    if(uDiff > 360)
    {
      bOk = false;
    }
  }

#endif

  // transmit USB data
  AddOverunLog(ltStartTransmit__);

  
  usbStartTransmitI(usbp, 3, (uint8_t *)pTxLocation, USE_TRANSFER_SIZE_BYTES);
  aduAddTransferLog(blStartTransmit, USE_TRANSFER_SIZE_BYTES);

  AnalyserSetChannel(acUsbAudioInitiateTransmit, false);

}


/**
 * @brief   Default data transmitted callback.
 * @details Will receive the data at the next SOF.
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 * @param[in] ep        endpoint number
 */
void aduDataTransmitted(USBDriver *usbp, usbep_t ep) 
{
  AnalyserSetChannel(acUsbAudioTransmitComplete, true);
#if ADU_TRANSFER_LOG_SIZE
  USBInEndpointState *pEpState = usbp->epc[ep]->in_state;
  volatile uint32_t uTransmittedCount = pEpState->txcnt;

  aduAddTransferLog(blEndTransmit, uTransmittedCount);
#endif

  if(aduIsUsbOutputEnabled())
  {
    chSysLockFromIsr();
    aduInitiateTransmitI(usbp);
    chSysUnlockFromIsr();
  }
  else
    aduResetBuffers();

  AnalyserSetChannel(acUsbAudioTransmitComplete, false);

}


/**
 * @brief   Inititate a USB receive.
 * @details The application must use this function as callback for the OUT
 *          data endpoint.
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 */
void aduInitiateReceiveI(USBDriver *usbp)
{
  AnalyserSetChannel(acUsbAudioInitiateReceive, true);

  AddOverunLog(ltStartReceive___);
  int16_t *pRxLocation = aduRxRingBuffer + aduState.rxRingBufferWriteOffset;

  usbStartReceiveI(usbp, 3, (uint8_t *)pRxLocation, USE_TRANSFER_SIZE_BYTES);
  aduAddTransferLog(blStartReceive, USE_TRANSFER_SIZE_BYTES);
  
  AnalyserSetChannel(acUsbAudioInitiateReceive, false);
}

/**
 * @brief   Default data received callback.
 * @details The application must use this function as callback for the OUT
 *          data endpoint.
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 * @param[in] ep        endpoint number
 */
void aduDataReceived(USBDriver *usbp, usbep_t ep) 
{
  AnalyserSetChannel(acUsbAudioReceiveComplete, true);

  AddOverunLog(ltBeforeRXAdjust_);

  //chSysLockFromIsr();

#if ADU_TRANSFER_LOG_SIZE
  USBOutEndpointState *pEpState = usbp->epc[ep]->out_state;
  volatile uint32_t uReceivedCount = pEpState->rxcnt;
  aduAddTransferLog(blEndReceive, uReceivedCount);
#endif

  // increase and wrap write offset
  aduState.rxRingBufferWriteOffset += USE_TRANSFER_SIZE_SAMPLES;
  if(aduState.rxRingBufferWriteOffset == TX_RING_BUFFER_FULL_SIZE)
    aduState.rxRingBufferWriteOffset = 0;

  // increase buffer used size
  aduState.rxRingBufferUsedSize += USE_TRANSFER_SIZE_SAMPLES;

  if(aduState.rxRingBufferUsedSize > TX_RING_BUFFER_NORMAL_SIZE)
  {
    //HandleError();
    if(aduState.rxRingBufferUsedSize > TX_RING_BUFFER_FULL_SIZE)
    {
      // really bad 
      HandleError();
    }
  }

  AddOverunLog(ltAfterRXAdjust__);

  if(aduIsUsbOutputEnabled())
  {
    chSysLockFromIsr();
    aduInitiateReceiveI(usbp);
    chSysUnlockFromIsr();
  }
  else
    aduResetBuffers();

  AnalyserSetChannel(acUsbAudioReceiveComplete, false);

}

FORCE_INLINE bool aduIsUsbInUse(void)
{
#if DISREGARD_ACTIVE_PAIRING
  return (aduState.isInputActive || aduState.isOutputActive);
#else
  return (aduState.isInputActive && aduState.isOutputActive);
#endif
}

FORCE_INLINE bool aduIsUsbOutputEnabled(void)
{
#if DISREGARD_ACTIVE_PAIRING
  return (aduState.isInputActive || aduState.isOutputActive);
#else
  return aduState.isOutputActive;
#endif
}

#pragma GCC pop_options

#endif



