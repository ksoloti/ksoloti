/*
 ChibiOS/RT - Copyright (C) 2006,2007,2008,2009,2010,
 2011,2012,2013 Giovanni Di Sirio.

 This file is part of ChibiOS/RT.

 ChibiOS/RT is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
 (at your option) any later version.

 ChibiOS/RT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @file    midi_usb.c
 * @brief   Midi USB Driver code.
 *
 * @addtogroup MIDI_USB
 * @{
 */

#include "ch.h"
#include "hal.h"
#include "midi_usb.h"
#include "usbcfg.h"

#if 1 // HAL_USE_MIDI_USB || defined(__DOXYGEN__)

#define MDU_LOG_COUNT 0

#if MDU_LOG_COUNT 
typedef enum _BLType {blStartTransmit, blStartReceive, blEndTransmit, blEndReceive} BLType;

typedef struct _DBGLOG
{
  BLType    type;
  uint16_t  uSize;
} DBGLOG;

DBGLOG mduLog[MDU_LOG_COUNT] __attribute__ ((section (".sram3")));
uint16_t umduLogCount = 0;

void mduAddLog(BLType type, uint16_t uSize)
{
  if(umduLogCount == 0)
    memset(mduLog, 0, sizeof(mduLog));

  mduLog[umduLogCount].type = type;
  mduLog[umduLogCount].uSize = uSize;
  umduLogCount++;
  if(umduLogCount == MDU_LOG_COUNT)
    umduLogCount = 0;
}
#else
  #define mduAddLog(a,b)
#endif

/*===========================================================================*/
/* Driver local definitions.                                                 */
/*===========================================================================*/

uint8_t mduReceiveBuffer[MIDI_USB_BUFFERS_SIZE];
uint8_t mduTransmitBuffer[MIDI_USB_BUFFERS_SIZE];

/*===========================================================================*/
/* Driver exported variables.                                                */
/*===========================================================================*/

/*===========================================================================*/
/* Driver local variables and types.                                         */
/*===========================================================================*/

/*===========================================================================*/
/* Driver local functions.                                                   */
/*===========================================================================*/

/*
 * Interface implementation.
 */

static size_t write(void *ip, const uint8_t *bp, size_t n) {

  return oqWriteTimeout(&((MidiUSBDriver *)ip)->oqueue, bp, n, TIME_INFINITE);
}

static size_t read(void *ip, uint8_t *bp, size_t n) {

  return iqReadTimeout(&((MidiUSBDriver *)ip)->iqueue, bp, n, TIME_INFINITE);
}

static msg_t put(void *ip, uint8_t b) {

  return oqPutTimeout(&((MidiUSBDriver *)ip)->oqueue, b, TIME_INFINITE);
}

static msg_t get(void *ip) {

  return iqGetTimeout(&((MidiUSBDriver *)ip)->iqueue, TIME_INFINITE);
}

static msg_t putt(void *ip, uint8_t b, systime_t timeout) {

  return oqPutTimeout(&((MidiUSBDriver *)ip)->oqueue, b, timeout);
}

static msg_t gett(void *ip, systime_t timeout) {

  return iqGetTimeout(&((MidiUSBDriver *)ip)->iqueue, timeout);
}

static size_t writet(void *ip, const uint8_t *bp, size_t n, systime_t time) {

  return oqWriteTimeout(&((MidiUSBDriver *)ip)->oqueue, bp, n, time);
}

static size_t readt(void *ip, uint8_t *bp, size_t n, systime_t time) {

  return iqReadTimeout(&((MidiUSBDriver *)ip)->iqueue, bp, n, time);
}

static const struct MidiUSBDriverVMT vmt = {0, write, read, put, get, putt, gett,
                                            writet, readt, 0};


void mduInitiateReceiveI(MidiUSBDriver *mdup, size_t uCount)
{
  USBDriver *usbp = mdup->config->usbp;

  size_t uRequestCount = MIN(uCount, MIDI_USB_BUFFERS_SIZE);
  usbStartReceiveI(usbp, mdup->config->bulk_out, mduReceiveBuffer, uRequestCount);
  mduAddLog(blStartReceive, uRequestCount);
}

void mduInitiateTransmitI(MidiUSBDriver *mdup, size_t uCount)
{
  USBDriver *usbp = mdup->config->usbp;

  // we need to copy from queue to buffer
  size_t uQueueCount = oqGetFullI(&mdup->oqueue);
  size_t uTransmitCount = MIN(uCount, MIN(uQueueCount, MIDI_USB_BUFFERS_SIZE));

  size_t u;
  for(u = 0; u < uTransmitCount; u++)
  {
    mduTransmitBuffer[u] = oqGetI(&mdup->oqueue);
  }

  size_t uRequestCount = MIN(uTransmitCount, MIDI_USB_BUFFERS_SIZE);
  usbStartTransmitI(usbp, mdup->config->bulk_in, mduTransmitBuffer, uRequestCount);
  mduAddLog(blStartTransmit, uRequestCount);
}

/**
 * @brief   Notification of data removed from the input queue.
 */
static void inotify(GenericQueue *qp) {
  size_t n, maxsize;
  MidiUSBDriver *mdup = chQGetLink(qp);

  /* If the USB driver is not in the appropriate state then transactions
   must not be started.*/
  if ((usbGetDriverStateI(mdup->config->usbp) != USB_ACTIVE)
      || (mdup->state != MDU_READY))
    return;

  /* If there is in the queue enough space to hold at least one packet and
   a transaction is not yet started then a new transaction is started for
   the available space.*/
  maxsize = mdup->config->usbp->epc[mdup->config->bulk_out]->out_maxsize;
  if (!usbGetReceiveStatusI(mdup->config->usbp, mdup->config->bulk_out) && ((n =
      iqGetEmptyI(&mdup->iqueue)) >= maxsize)) 
  {
    n = (n / maxsize) * maxsize;
    mduInitiateReceiveI(mdup, n);
  }
}

/**
 * @brief   Notification of data inserted into the output queue.
 */
static void onotify(GenericQueue *qp) {
  size_t n;
  MidiUSBDriver *mdup = chQGetLink(qp);

  /* If the USB driver is not in the appropriate state then transactions
   must not be started.*/
  if ((usbGetDriverStateI(mdup->config->usbp) != USB_ACTIVE)
      || (mdup->state != MDU_READY))
    return;

  /* If there is not an ongoing transaction and the output queue contains
   data then a new transaction is started.*/
  if (!usbGetTransmitStatusI(mdup->config->usbp, mdup->config->bulk_in)) {
    n = oqGetFullI(&mdup->oqueue);
    if ((n > 0) && !(n & 3)) {
      mduInitiateTransmitI(mdup, n);
    }
  }
}

/*===========================================================================*/
/* Driver exported functions.                                                */
/*===========================================================================*/

/**
 * @brief   Bulk USB Driver initialization.
 * @note    This function is implicitly invoked by @p halInit(), there is
 *          no need to explicitly initialize the driver.
 *
 * @init
 */
void mduInit(void) {
}

/**
 * @brief   Initializes a generic full duplex driver object.
 * @details The HW dependent part of the initialization has to be performed
 *          outside, usually in the hardware initialization code.
 *
 * @param[out] mdup     pointer to a @p MidiUSBDriver structure
 *
 * @init
 */
void mduObjectInit(MidiUSBDriver *mdup) {

  mdup->vmt = &vmt;
  chEvtInit(&mdup->event);
  mdup->state = MDU_STOP;
  chIQInit(&mdup->iqueue, mdup->ib, MIDI_USB_BUFFERS_SIZE, inotify, mdup);
  chOQInit(&mdup->oqueue, mdup->ob, MIDI_USB_BUFFERS_SIZE, onotify, mdup);
}

/**
 * @brief   Configures and starts the driver.
 *
 * @param[in] mdup      pointer to a @p MidiUSBDriver object
 * @param[in] config    the Bulk USB driver configuration
 *
 * @api
 */
void mduStart(MidiUSBDriver *mdup, const MidiUSBConfig *config) {
  USBDriver *usbp = config->usbp;

  chDbgCheck(mdup != NULL);

  chSysLock()
  ;
  chDbgAssert((mdup->state == MDU_STOP) || (mdup->state == MDU_READY),
              "mduStart(), #1 invalid state");
  usbp->in_params[config->bulk_in - 1] = mdup;
  usbp->out_params[config->bulk_out - 1] = mdup;
  mdup->config = config;
  mdup->state = MDU_READY;
  chSysUnlock()
  ;
}

/**
 * @brief   Stops the driver.
 * @details Any thread waiting on the driver's queues will be awakened with
 *          the message @p Q_RESET.
 *
 * @param[in] mdup      pointer to a @p MidiUSBDriver object
 *
 * @api
 */
void mduStop(MidiUSBDriver *mdup) {
  USBDriver *usbp = mdup->config->usbp;

  chDbgCheck(mdup != NULL);

  chSysLock()
  ;

  chDbgAssert((mdup->state == MDU_STOP) || (mdup->state == MDU_READY),
              "mduStop(), #1 invalid state");

  /* Driver in stopped state.*/
  usbp->in_params[mdup->config->bulk_in - 1] = NULL;
  usbp->out_params[mdup->config->bulk_out - 1] = NULL;
  mdup->state = MDU_STOP;

  /* Queues reset in order to signal the driver stop to the application.*/
  chnAddFlagsI(mdup, CHN_DISCONNECTED);
  iqResetI(&mdup->iqueue);
  oqResetI(&mdup->oqueue);
  chSchRescheduleS();

  chSysUnlock()
  ;
}


/**
 * @brief   USB device configured handler.
 *
 * @param[in] mdup      pointer to a @p MidiUSBDriver object
 *
 * @iclass
 */
void mduConfigureHookI(MidiUSBDriver *mdup) {
  USBDriver *usbp = mdup->config->usbp;

  iqResetI(&mdup->iqueue);
  oqResetI(&mdup->oqueue);
  chnAddFlagsI(mdup, CHN_CONNECTED);

  /* Starts the first OUT transaction immediately.*/
  //CH16 usbPrepareQueuedReceive(usbp, mdup->config->bulk_out, &mdup->iqueue,
  //                        usbp->epc[mdup->config->bulk_out]->out_maxsize);
  //CH16 usbStartReceiveI(usbp, mdup->config->bulk_out);
  mduInitiateReceiveI(mdup, usbp->epc[mdup->config->bulk_out]->out_maxsize);
}

/**
 * @brief   Default requests hook.
 * @details Applications wanting to use the Bulk USB driver can use
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
bool_t mduRequestsHook(USBDriver *usbp) {

  (void)usbp;
  return FALSE;
}

/**
 * @brief   Default data transmitted callback.
 * @details The application must use this function as callback for the IN
 *          data endpoint.
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 * @param[in] ep        endpoint number
 */
void mduDataTransmitted(USBDriver *usbp, usbep_t ep) {
  size_t n;
  MidiUSBDriver *mdup = usbp->in_params[ep - 1];

  if (mdup == NULL)
    return;

  chSysLockFromIsr();
  chnAddFlagsI(mdup, CHN_OUTPUT_EMPTY);

  USBInEndpointState *pEpState = usbp->epc[ep]->in_state;
  __attribute__((unused)) uint32_t uTransmittedCount = pEpState->txcnt;

  mduAddLog(blEndTransmit, uTransmittedCount);
  if ((n = oqGetFullI(&mdup->oqueue)) > 0) {
    /* The endpoint cannot be busy, we are in the context of the callback,
     so it is safe to transmit without a check.*/
    //chSysUnlockFromIsr()
    //;

    //CH16 usbPrepareQueuedTransmit(usbp, ep, &mdup->oqueue, n);
	if(n) // do we need blocks of 4
	    mduInitiateTransmitI(mdup, n);

    //chSysLockFromIsr()
    //;
    //CH16 usbStartTransmitI(usbp, ep);
  }
  else if ((usbp->epc[ep]->in_state->txsize > 0)
      && !(usbp->epc[ep]->in_state->txsize & (usbp->epc[ep]->in_maxsize - 1))) {
    /* Transmit zero sized packet in case the last one has maximum allowed
     size. Otherwise the recipient may expect more data coming soon and
     not return buffered data to app. See section 5.8.3 Bulk Transfer
     Packet Size Constraints of the USB Specification document.*/
    //chSysUnlockFromIsr()
    //;

    //CH16 usbPrepareQueuedTransmit(usbp, ep, &mdup->oqueue, 0);
    mduInitiateTransmitI(mdup, 0);

    //chSysLockFromIsr()
    //;
    //CH16 usbStartTransmitI(usbp, ep);
  }

  chSysUnlockFromIsr()
  ;
}

/**
 * @brief   Default data received callback.
 * @details The application must use this function as callback for the OUT
 *          data endpoint.
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 * @param[in] ep        endpoint number
 */
void mduDataReceived(USBDriver *usbp, usbep_t ep) {
  volatile size_t uQueueRemainingSize, maxsize;
  MidiUSBDriver *mdup = usbp->out_params[ep - 1];

  if (mdup == NULL)
    return;

  // CH16 we need to transfer data from our buffer to the queue
  // this all needs a rewrite to get rid of the queues and
  // use buffers instead.
  USBOutEndpointState *pEpState = usbp->epc[ep]->out_state;
  volatile uint32_t uReceivedCount = pEpState->rxcnt;
  
  chSysLockFromIsr()
  ;

  chnAddFlagsI(mdup, CHN_INPUT_AVAILABLE);

  mduAddLog(blEndReceive, uReceivedCount);

  maxsize = usbp->epc[ep]->out_maxsize;
  uQueueRemainingSize = iqGetEmptyI(&mdup->iqueue);

  size_t uSizeToCopy = MIN(uQueueRemainingSize, uReceivedCount);
  size_t u;
  for(u = 0; u < uSizeToCopy; u++)
  {
    iqPutI(&mdup->iqueue, mduReceiveBuffer[u]);
  }  

  uQueueRemainingSize-= uSizeToCopy;
  
  // volatile size_t temp = uQueueRemainingSize;
  uQueueRemainingSize = (uQueueRemainingSize / maxsize) * maxsize;  // Make sure we get less packets
  
  if(uQueueRemainingSize!=0)
    mduInitiateReceiveI(mdup, uQueueRemainingSize);

  chSysUnlockFromIsr()
  ;
}

// the Send etc, work for everything except Sysex
uint8_t calcDS1(uint8_t b0) {
// this works for everything bar SysEx,
// for sysex you need to use 0x4-0x7 to pack messages
  return (b0 & 0xF0) >> 4;

}

uint8_t calcCIN1(uint8_t port, uint8_t b0) {
  uint8_t ds = calcDS1(b0);
  uint8_t cin = (((port - 1) & 0x0F) << 4) | ds;
  return cin;
}

void midi_usb_MidiSend1(uint8_t port, uint8_t b0) {
  uint8_t tx[4];
  tx[0] = calcCIN1(port, b0);
  tx[1] = b0;
  tx[2] = 0;
  tx[3] = 0;
  write(&MDU1, &tx[0], 4);
}

void midi_usb_MidiSend2(uint8_t port, uint8_t b0, uint8_t b1) {
  uint8_t tx[4];
  tx[0] = calcCIN1(port, b0);
  tx[1] = b0;
  tx[2] = b1;
  tx[3] = 0;
  write(&MDU1, &tx[0], 4);
}

void midi_usb_MidiSend3(uint8_t port, uint8_t b0, uint8_t b1, uint8_t b2) {
  uint8_t tx[4];
  tx[0] = calcCIN1(port, b0);
  tx[1] = b0;
  tx[2] = b1;
  tx[3] = b2;
  write(&MDU1, &tx[0], 4);
}

#define CIN_SYSEX_START 0x04
#define CIN_SYSEX_END_1 0x05
#define CIN_SYSEX_END_2 0x06
#define CIN_SYSEX_END_3 0x07

void midi_usb_MidiSendSysEx(uint8_t port, uint8_t bytes[], uint8_t len) {

  if (len < 3) return;

  const uint8_t cn = (( port - 1) & 0x0F) << 4;

  uint8_t tx[4];

  uint8_t i = 0;
  for (i = 0; i < (len - 3); i += 3) {
    tx[0] = CIN_SYSEX_START | cn;
    tx[1] = bytes[i];
    tx[2] = bytes[i + 1];
    tx[3] = bytes[i + 2];
    write(&MDU1, &tx[0], 4);
  }

  uint8_t remain = len - i;

  if (remain == 1) {
    tx[0] = CIN_SYSEX_END_1 | cn;
    tx[1] = bytes[i];
    tx[2] = 0;
    tx[3] = 0;
    write(&MDU1, &tx[0], 4);
  }
  else if (remain == 2) {
    tx[0] = CIN_SYSEX_END_2 | cn;
    tx[1] = bytes[i];
    tx[2] = bytes[i + 1];
    tx[3] = 0;
    write(&MDU1, &tx[0], 4);
  }
  else if (remain == 3) {
    tx[0] = CIN_SYSEX_END_3 | cn;
    tx[1] = bytes[i];
    tx[2] = bytes[i + 1];
    tx[3] = bytes[i + 2];
    write(&MDU1, &tx[0], 4);
  }
}

#endif /* HAL_USE_BULK_USB */

/** @} */
