
#if FW_USBAUDIO

#include <stdlib.h>
#include <string.h>

#include "hal.h"
#include "audio_usb.h"
#include "audio_usb_dbg.h"
#include "usb_lld.h"
#include "chevents.h"
#include "chdebug.h"
#include "usbcfg.h"


// do not set higher than -O1
#pragma GCC optimize ("O2")
#define FORCE_INLINE __attribute__((always_inline)) inline 
//#define FORCE_INLINE
#define NEW_CODE_TX 1
#define NEW_CODE_TRY_TX 1
#define NEW_CODE_RX 1
#define NEW_CODE_TRY_RX 1

//#define FORCE_INLINE

// O0 codecCopy = 14us,  TX = 42.1us
// O1 codecCopy = 3.3us, TX = 11.5us

// Simple improvements
// O0 codecCopy = 11us,  TX = 31.0us
// O1 codecCopy = 2.9us, TX = 10.1us

// TX direct
// O0 codecCopy = 11.98us, TX = 2.6us
// O1 codecCopy = 3.14us,  TX = 2.0us

////////////////////////////////////////////////
// new timings

// O0 codecCopy = 21.00us, TX = 3.18us, RX = 1.12us
// O1 codecCopy = 7.14us,  TX = 3.18us, RX = 1.12us

// O0 codecCopy = 21.31us, TX = 3.18us, RX = 1.12us           // Inline calls, why slower?
// O1 codecCopy = 7.14us,  TX = 3.18us, RX = 1.12us

// O0 codecCopy = 16.90us, TX = 3.18us, RX = 1.12us           // TX changed
// O1 codecCopy = 5.82us,  TX = 3.18us, RX = 1.12us           // TX changed

// O1 codecCopy = 4.68us,  TX = 3.18us, RX = 1.12us           // TX & RX changed
// O2 codecCopy = 3.61us,  TX = 1.87us, RX = 0.96us           // TX & RX changed
// O3 codecCopy = 3.61us,  TX = 1.87us, RX = 0.96us           // TX & RX changed

// So we need to improve that simple codec copy, get rid of %, two tight loops needed.




static int16_t aduTxRingBuffer[TX_RING_BUFFER_FULL_SIZE] __attribute__ ((section (".sram3")));
static int16_t aduRxRingBuffer[TX_RING_BUFFER_FULL_SIZE] __attribute__ ((section (".sram3")));
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

void HandleError(void)
{

  AddOverunLog(ltErrorBefore____);

  // ok we are all out of sync, try to recover
  aduState.state = asNeedsReset;
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


/**
 * @brief   Notification of data removed from the input queue.
 */
static void inotify(GenericQueue *qp) 
{
}

/**
 * @brief   Notification of data inserted into the output queue.
 */
static void onotify(GenericQueue *qp) 
{
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
  
  // set not muted
  aduState.mute[0] = 0;
  aduState.mute[1] = 0;
  aduState.mute[2] = 0;

  // set 0db volume
  aduState.volume[0] = VOLUME_CTRL_0_DB;
  aduState.volume[1] = VOLUME_CTRL_0_DB;
  aduState.volume[2] = VOLUME_CTRL_0_DB;

  adup->vmt = NULL; // none at the moment
  chEvtInit(&adup->event);
  adup->state = ADU_STOP;
  
  chIQInit(&adup->iqueue, adup->ib, AUDIO_USB_BUFFERS_SIZE, inotify, adup);
  chOQInit(&adup->oqueue, adup->ob, AUDIO_USB_BUFFERS_SIZE, onotify, adup);
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
  chIQResetI(&adup->iqueue);
  chOQResetI(&adup->oqueue);
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
  //USBDriver *usbp = adup->config->usbp;

  chIQResetI(&adup->iqueue);
  chOQResetI(&adup->oqueue);
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


void __attribute__((optimize("-O0"))) aduSofHookI(AudioUSBDriver *adup)
{
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

#if NEW_CODE_TX
static FORCE_INLINE void aduMoveDataToTX(int32_t *pData, uint_fast16_t uLen)
{
#if NEW_CODE_TRY_TX

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

  // for (u=0; u< uLen; u++)
  // {
  //   if(aduTxRingBuffer[aduState.txRingBufferWriteOffset] != pData[u] >> 16)
  //   {
  //     // wrong
  //     HandleError();
  //   }
  //   if (++(aduState.txRingBufferWriteOffset) == TX_RING_BUFFER_FULL_SIZE) 
  //     aduState.txRingBufferWriteOffset= 0;
  // }

  aduState.txRingBufferWriteOffset = (aduState.txRingBufferWriteOffset + uLen) % TX_RING_BUFFER_FULL_SIZE;
  aduState.txRingBufferUsedSize+=uLen;
#else  
  uint_fast16_t u; for (u=0; u< uLen; u++)
  {
    aduTxRingBuffer[aduState.txRingBufferWriteOffset] = pData[u] >> 16;
    if (++(aduState.txRingBufferWriteOffset) == TX_RING_BUFFER_FULL_SIZE) 
      aduState.txRingBufferWriteOffset= 0;
  }
  aduState.txRingBufferUsedSize+=uLen;
#endif
}
#endif

#if NEW_CODE_RX
static FORCE_INLINE void aduMoveDataFromRX(int32_t *pData, uint_fast16_t uLen)
{
#if NEW_CODE_TRY_RX

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
#else  
  uint_fast16_t u; for (u=0; u < uLen; u++)
  {
    pData[u] = aduRxRingBuffer[aduState.rxRingBufferReadOffset] << 16;
    if (++(aduState.rxRingBufferReadOffset) == TX_RING_BUFFER_FULL_SIZE) 
      aduState.rxRingBufferReadOffset= 0;
  }
  aduState.rxRingBufferUsedSize -= uLen;
#endif
}
#endif

/**
 * @brief   Handles transfer of data between codec and USB
 * @details 
 *
 * @param[in] in    pointer to in data  <- Codec
 * @param[in] out   pointer to out data -> USB
 */

void aduDataExchange (int32_t *in, int32_t *out)
{
  if(aduIsUsbOutputEnabled())
  {

    uint16_t uLen = 32;
    uint16_t uFeedbackLen = uLen;
    uint_fast16_t u;

    /////////////////////////////////
    // codec -> USB
    /////////////////////////////////
    if(aduState.state == asCodecRemove)
    {
      // remove two samples
      uLen -= 2;
    } 
    else if(aduState.state == asCodecDuplicate)
    {
      // add two samples 

      #if CHECK_USB_DATA
        aduAddedTxSamplesStart = aduState.txRingBufferWriteOffset;
        aduAddedTxSampleValue = out[0]>>16;
      #endif // CHECK_USB_DATA

#if NEW_CODE_TX
      aduMoveDataToTX(out, 2);
#else
      int u; for (u=0; u< 2; u++)
      {
        aduTxRingBuffer[aduState.txRingBufferWriteOffset] = out[u] >> 16;
        if (++(aduState.txRingBufferWriteOffset) == TX_RING_BUFFER_FULL_SIZE) 
          aduState.txRingBufferWriteOffset= 0;
      }
      aduState.txRingBufferUsedSize+=2;
#endif
    }

#if NEW_CODE_TX
    aduMoveDataToTX(out, uLen);
#else
    for (u=0; u< uLen; u++)
    {
      aduTxRingBuffer[aduState.txRingBufferWriteOffset] = out[u] >> 16;
      if (++(aduState.txRingBufferWriteOffset) == TX_RING_BUFFER_FULL_SIZE) 
        aduState.txRingBufferWriteOffset= 0;
    }
    aduState.txRingBufferUsedSize+=uLen;
#endif

    aduState.codecFrameSampleCount+=uFeedbackLen;

    #if CHECK_USB_DATA
      if(uLen < 32)
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
      // always copy 32 samples
#if NEW_CODE_RX
      aduMoveDataFromRX(in, 32);
#else
      for (u=0; u < 32; u++)
      {
        in[u] = aduRxRingBuffer[aduState.rxRingBufferReadOffset] << 16;
        if (++(aduState.rxRingBufferReadOffset) == TX_RING_BUFFER_FULL_SIZE) 
          aduState.rxRingBufferReadOffset= 0;
      }
      aduState.rxRingBufferUsedSize -= 32;
#endif
      // adjustments 

      if(aduState.state == asCodecDuplicate)
      {
        // 2 two many in USB buffer

        #if CHECK_USB_DATA
          aduSkippedRxSamplesStart = aduState.rxRingBufferReadOffset;
          aduSkippedRxSampleValue = aduRxRingBuffer[aduSkippedRxSamplesStart];
        #endif // CHECK_USB_DATA

        aduState.rxRingBufferReadOffset = (aduState.rxRingBufferReadOffset+2) % TX_RING_BUFFER_FULL_SIZE;
        aduState.rxRingBufferUsedSize -= 2;
      }
      else if(aduState.state == asCodecRemove)
      {
        // two  little in USB buffer
        #if CHECK_USB_DATA
          aduAddedRxSamplesStart = aduState.rxRingBufferReadOffset;
          aduAddedRxSampleValue = aduRxRingBuffer[aduSkippedRxSamplesStart];
        #endif // CHECK_USB_DATA

        if(aduState.rxRingBufferReadOffset == 0)
          aduState.rxRingBufferReadOffset = TX_RING_BUFFER_FULL_SIZE -2;
        else
          aduState.rxRingBufferReadOffset -= 2;

        aduState.rxRingBufferUsedSize += 2;
      }


      #if CHECK_USB_DATA
        // DEBUG test USB Data, requires USBOutputTest.axp running on Ksoloiti
        volatile int16_t tmpCodecData[46];
        bool bOk = true;
        uint_fast16_t u; for(u = 0; u < 14; u++)
        {
          int16_t nV1 = in[(u*2)+2] >> 16;
          int16_t nV2 = in[(u*2)] >> 16;
          
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

  int16_t nFrameSampleOffest = (int16_t)(aduState.codecFrameSampleCount)-96;
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


  // we need some checks here for debugging
  if(aduState.state > asFillingUnderflow)
  {
    uint16_t uTXCalcSize;
    if((aduState.txRingBufferWriteOffset < aduState.txRingBufferReadOffset))
      uTXCalcSize = (aduState.txRingBufferWriteOffset + TX_RING_BUFFER_FULL_SIZE) - aduState.txRingBufferReadOffset;
    else
      uTXCalcSize = aduState.txRingBufferWriteOffset - aduState.txRingBufferReadOffset;

    if(uTXCalcSize != aduState.txRingBufferUsedSize)
      HandleError();

    uint16_t uRXCalcSize;
    if((aduState.rxRingBufferWriteOffset < aduState.rxRingBufferReadOffset))
    {
      uRXCalcSize = (aduState.rxRingBufferWriteOffset + TX_RING_BUFFER_FULL_SIZE) - aduState.rxRingBufferReadOffset;
    }
    else
      uRXCalcSize = aduState.rxRingBufferWriteOffset - aduState.rxRingBufferReadOffset;

    if(uRXCalcSize != aduState.rxRingBufferUsedSize)
      HandleError();
  }

  AddOverunLog(ltFrameEndedEnd__);
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
      if(aduState.sampleOffset > 0)
      {
        // adjust overrun
        // chuck samples awway
        aduState.sampleOffset-=2;
        aduState.state = asCodecRemove;
      }
      else
      {
        // adjust underrun
        // duplicate sample
        aduState.sampleOffset+=2;
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
    uint32_t uDiff = abs(pTxLocation[(u*2)+2] - pTxLocation[u*2]);
    tmpData[u] = (pTxLocation[(u*2)+2] - pTxLocation[u*2]);
    if(uDiff > 300)
    {
      bOk = false;
    }
  }

#endif

  // transmit USB data
  AddOverunLog(ltStartTransmit__);

  
  usbStartTransmitI(usbp, 3, (uint8_t *)pTxLocation, USE_TRANSFER_SIZE_BYTES);
  aduAddTransferLog(blStartTransmit, USE_TRANSFER_SIZE_BYTES);

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
  chSysLockFromIsr();

  AddOverunLog(ltStartReceive___);
  int16_t *pRxLocation = aduRxRingBuffer + aduState.rxRingBufferWriteOffset;

  usbStartReceiveI(usbp, 3, (uint8_t *)pRxLocation, USE_TRANSFER_SIZE_BYTES);
  aduAddTransferLog(blStartReceive, USE_TRANSFER_SIZE_BYTES);

  chSysUnlockFromIsr();
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
  chSysLockFromIsr();
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
    aduInitiateReceiveI(usbp);
  else
    aduResetBuffers();

  chSysUnlockFromIsr();
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


#endif



