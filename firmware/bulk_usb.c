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
 * @file    bulk_usb.c
 * @brief   Bulk USB Driver code.
 *
 * @addtogroup BULK_USB
 * @{
 */

#include "ch.h"
#include "hal.h"
#include "bulk_usb.h"
#include "usbcfg.h"

#if 1 // HAL_USE_BULK_USB || defined(__DOXYGEN__)

#define BDU_LOG_SIZE 0
#if BDU_LOG_SIZE
typedef enum _BLType {blWrite, blStartTransmit, blStartReceive, blEndTransmit, blEndReceive, blFromNotify, blFromUSBInt, blConfigure, blNotifyCalled, blBlockedTX, blBlockedRX} BLType;

typedef struct _DBGLOG
{
  int       ms;
  BLType    type;
  uint16_t  uSize;
} DBGLOG;

DBGLOG bduLog[BDU_LOG_SIZE] __attribute__ ((section (".sram3")));
uint16_t uBduLogCount = 0;

void bduAddLog(BLType type, uint16_t uSize)
{
  if(uBduLogCount == 0)
    memset(bduLog, 0, sizeof(bduLog));

  systime_t time = chVTGetSystemTime();
  bduLog[uBduLogCount].ms = time / 10;

  bduLog[uBduLogCount].type = type;
  bduLog[uBduLogCount].uSize = uSize;
  uBduLogCount++;
  if(uBduLogCount == BDU_LOG_SIZE)
    uBduLogCount = 0;
}
#else
  #define bduAddLog(a,b)
#endif

/* Driver local definitions.                                                 */
/*===========================================================================*/

uint8_t bduReceiveBuffer[BULK_USB_BUFFERS_SIZE];
uint8_t bduTransmitBuffer[BULK_USB_BUFFERS_SIZE];

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
#if USE_BLOCKED_BULK_TX  
  BulkUSBDriver *bdup = (BulkUSBDriver *)ip;
  USBDriver *usbp = bdup->config->usbp;

  if ((usbGetDriverStateI(bdup->config->usbp) != USB_ACTIVE) ||
      (bdup->state != BDU_READY))
    return 0;

  bduAddLog(blBlockedTX, n);

  msg_t r = usbTransmit(usbp, bdup->config->bulk_in, bp, n);

  return r;
#else
  return oqWriteTimeout(&((BulkUSBDriver *)ip)->oqueue, bp,
                           n, TIME_INFINITE);
#endif
}

static size_t read(void *ip, uint8_t *bp, size_t n) {

  return iqReadTimeout(&((BulkUSBDriver *)ip)->iqueue, bp,
                         n, TIME_INFINITE);
}

static msg_t put(void *ip, uint8_t b) {

  return oqPutTimeout(&((BulkUSBDriver *)ip)->oqueue, b, TIME_INFINITE);
}

static msg_t get(void *ip) {

  return iqGetTimeout(&((BulkUSBDriver *)ip)->iqueue, TIME_INFINITE);
}

static msg_t putt(void *ip, uint8_t b, systime_t timeout) {

  return oqPutTimeout(&((BulkUSBDriver *)ip)->oqueue, b, timeout);
}

static msg_t gett(void *ip, systime_t timeout) {

  return iqGetTimeout(&((BulkUSBDriver *)ip)->iqueue, timeout);
}

static size_t writet(void *ip, const uint8_t *bp, size_t n, systime_t time) {

  return oqWriteTimeout(&((BulkUSBDriver *)ip)->oqueue, bp, n, time);
}

static size_t readt(void *ip, uint8_t *bp, size_t n, systime_t time) {
  return iqReadTimeout(&((BulkUSBDriver *)ip)->iqueue, bp, n, time);
}

static const struct BulkUSBDriverVMT vmt = {
  0,
  write, read, put, get,
  putt, gett, writet, readt,
  0
};


void bduInitiateReceiveI(BulkUSBDriver *bdup, size_t uCount)
{
  USBDriver *usbp = bdup->config->usbp;

  size_t uRequestCount = MIN(uCount, BULK_USB_BUFFERS_SIZE);
  usbStartReceiveI(usbp, bdup->config->bulk_out, bduReceiveBuffer, uRequestCount);

  bduAddLog(blStartReceive, uRequestCount);
}

void bduInitiateTransmitI(BulkUSBDriver *bdup, size_t uCount)
{

  USBDriver *usbp = bdup->config->usbp;

  // we need to copy from queue to buffer
  volatile size_t uQueueCount = oqGetFullI(&bdup->oqueue);
  volatile size_t uTransmitCount = MIN(uCount, MIN(uQueueCount, BULK_USB_BUFFERS_SIZE));

  size_t u;
  for(u = 0; u < uTransmitCount; u++)
  {
    bduTransmitBuffer[u] = oqGetI(&bdup->oqueue);
  }

  size_t uRequestCount = MIN(uTransmitCount, BULK_USB_BUFFERS_SIZE);
  usbStartTransmitI(usbp, bdup->config->bulk_in, bduTransmitBuffer, uRequestCount);
  bduAddLog(blStartTransmit, uRequestCount);
}

/**
 * @brief   Notification of data removed from the input queue.
 */
static void inotify(GenericQueue *qp) {
  size_t n, maxsize;
  BulkUSBDriver *bdup = chQGetLink(qp);

  /* If the USB driver is not in the appropriate state then transactions
     must not be started.*/
  if ((usbGetDriverStateI(bdup->config->usbp) != USB_ACTIVE) ||
      (bdup->state != BDU_READY))
    return;

  /* If there is in the queue enough space to hold at least one packet and
     a transaction is not yet started then a new transaction is started for
     the available space.*/
  maxsize = bdup->config->usbp->epc[bdup->config->bulk_out]->out_maxsize;
  if (!usbGetReceiveStatusI(bdup->config->usbp, bdup->config->bulk_out) &&
      ((n = iqGetEmptyI(&bdup->iqueue)) >= maxsize)) {
    chSysUnlock();

    n = (n / maxsize) * maxsize;
    //CH16 usbPrepareQueuedReceive(bdup->config->usbp,
    //                        bdup->config->bulk_out,
    //                        &bdup->iqueue, n);
    //CH16 usbStartReceiveI(bdup->config->usbp,
    //                        bdup->config->bulk_out,
    //                        &bdup->iqueue, n);
    bduInitiateReceiveI(bdup, n);
    chSysLock();
    //CH16 usbStartReceiveI(bdup->config->usbp, bdup->config->bulk_out);
  }
}

/**
 * @brief   Notification of data inserted into the output queue.
 */
static void onotify(GenericQueue *qp) {
  volatile size_t n;
  BulkUSBDriver *bdup = chQGetLink(qp);

  bduAddLog(blNotifyCalled, oqGetFullI(&bdup->oqueue));

  /* If the USB driver is not in the appropriate state then transactions
     must not be started.*/
  if ((usbGetDriverStateI(bdup->config->usbp) != USB_ACTIVE) ||
      (bdup->state != BDU_READY))
    return;

  /* If there is not an ongoing transaction and the output queue contains
     data then a new transaction is started.*/
  if (!usbGetTransmitStatusI(bdup->config->usbp, bdup->config->bulk_in) &&
      ((n = oqGetFullI(&bdup->oqueue)) > 0)) {
    //chSysUnlock();

    //CH16 usbPrepareQueuedTransmit(bdup->config->usbp,
    //                         bdup->config->bulk_in,
    //                         &bdup->oqueue, n);
    //chSysLock();
    bduAddLog(blFromNotify, n);
    bduInitiateTransmitI(bdup, n);
    //CH16 usbStartTransmitI(bdup->config->usbp, bdup->config->bulk_in);
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
void bduInit(void) {
}

/**
 * @brief   Initializes a generic full duplex driver object.
 * @details The HW dependent part of the initialization has to be performed
 *          outside, usually in the hardware initialization code.
 *
 * @param[out] bdup     pointer to a @p BulkUSBDriver structure
 *
 * @init
 */
void bduObjectInit(BulkUSBDriver *bdup) {

  bdup->vmt = &vmt;
  chEvtInit(&bdup->event);
  bdup->state = BDU_STOP;
  chIQInit(&bdup->iqueue, bdup->ib, BULK_USB_BUFFERS_SIZE, inotify, bdup);
  chOQInit(&bdup->oqueue, bdup->ob, BULK_USB_BUFFERS_SIZE, onotify, bdup);
}

/**
 * @brief   Configures and starts the driver.
 *
 * @param[in] bdup      pointer to a @p BulkUSBDriver object
 * @param[in] config    the Bulk USB driver configuration
 *
 * @api
 */
void bduStart(BulkUSBDriver *bdup, const BulkUSBConfig *config) {
  USBDriver *usbp = config->usbp;

  chDbgCheck(bdup != NULL);

  chSysLock();
  chDbgAssert((bdup->state == BDU_STOP) || (bdup->state == BDU_READY),
              "bduStart(), #1 invalid state");
  usbp->in_params[config->bulk_in - 1]   = bdup;
  usbp->out_params[config->bulk_out - 1] = bdup;
  bdup->config = config;
  bdup->state = BDU_READY;
  chSysUnlock();
}

/**
 * @brief   Stops the driver.
 * @details Any thread waiting on the driver's queues will be awakened with
 *          the message @p Q_RESET.
 *
 * @param[in] bdup      pointer to a @p BulkUSBDriver object
 *
 * @api
 */
void bduStop(BulkUSBDriver *bdup) {
  USBDriver *usbp = bdup->config->usbp;

  chDbgCheck(bdup != NULL);

  chSysLock();

  chDbgAssert((bdup->state == BDU_STOP) || (bdup->state == BDU_READY),
              "bduStop(), #1 invalid state");

  /* Driver in stopped state.*/
  usbp->in_params[bdup->config->bulk_in - 1]   = NULL;
  usbp->out_params[bdup->config->bulk_out - 1] = NULL;
  bdup->state = BDU_STOP;

  /* Queues reset in order to signal the driver stop to the application.*/
  chnAddFlagsI(bdup, CHN_DISCONNECTED);
  iqResetI(&bdup->iqueue);
  oqResetI(&bdup->oqueue);
  chSchRescheduleS();

  chSysUnlock();
}

/**
 * @brief   USB device configured handler.
 *
 * @param[in] bdup      pointer to a @p BulkUSBDriver object
 *
 * @iclass
 */
void bduConfigureHookI(BulkUSBDriver *bdup) {
  USBDriver *usbp = bdup->config->usbp;

  iqResetI(&bdup->iqueue);
  oqResetI(&bdup->oqueue);
  chnAddFlagsI(bdup, CHN_CONNECTED);

  /* Starts the first OUT transaction immediately.*/
  bduAddLog(blConfigure, usbp->epc[bdup->config->bulk_out]->out_maxsize);
  bduInitiateReceiveI(bdup, usbp->epc[bdup->config->bulk_out]->out_maxsize);
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
bool_t bduRequestsHook(USBDriver *usbp) {

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
void bduDataTransmitted(USBDriver *usbp, usbep_t ep) {
#if USE_BLOCKED_BULK_TX    
  return;
#else
  size_t n;
  BulkUSBDriver *bdup = usbp->in_params[ep - 1];

  if (bdup == NULL)
    return;

  chSysLockFromIsr();
  chnAddFlagsI(bdup, CHN_OUTPUT_EMPTY);

  USBInEndpointState *pEpState = usbp->epc[ep]->in_state;
  __attribute__((unused)) uint32_t uTransmittedCount = pEpState->txcnt;

  bduAddLog(blEndTransmit, uTransmittedCount);

  if ((n = oqGetFullI(&bdup->oqueue)) > 0) {
    /* The endpoint cannot be busy, we are in the context of the callback,
       so it is safe to transmit without a check.*/
    //chSysUnlockFromIsr();

    //CH16 usbPrepareQueuedTransmit(usbp, ep, &bdup->oqueue, n);
    if(n)
    {
      bduAddLog(blFromUSBInt, n);
      bduInitiateTransmitI(bdup, n);
    }
    //chSysLockFromIsr();
    //CH16 usbStartTransmitI(usbp, ep);
  }
  else if ((usbp->epc[ep]->in_state->txsize > 0) &&
           !(usbp->epc[ep]->in_state->txsize &
             (usbp->epc[ep]->in_maxsize - 1))) {
    /* Transmit zero sized packet in case the last one has maximum allowed
       size. Otherwise the recipient may expect more data coming soon and
       not return buffered data to app. See section 5.8.3 Bulk Transfer
       Packet Size Constraints of the USB Specification document.*/
    //chSysUnlockFromIsr();

    //CH16 usbPrepareQueuedTransmit(usbp, ep, &bdup->oqueue, 0);
    bduAddLog(blFromUSBInt, 0);
    bduInitiateTransmitI(bdup, 0);

    //chSysLockFromIsr();
    //CH16 usbStartTransmitI(usbp, ep);
  }

  chSysUnlockFromIsr();
#endif  
}

/**
 * @brief   Default data received callback.
 * @details The application must use this function as callback for the OUT
 *          data endpoint.
 *
 * @param[in] usbp      pointer to the @p USBDriver object
 * @param[in] ep        endpoint number
 */
void bduDataReceived(USBDriver *usbp, usbep_t ep) {

  size_t uQueueRemainingSize, maxsize;
  BulkUSBDriver *bdup = usbp->out_params[ep - 1];

  if (bdup == NULL)
    return;

  USBOutEndpointState *pEpState = usbp->epc[ep]->out_state;
  volatile uint32_t uReceivedCount = pEpState->rxcnt;

  chSysLockFromIsr()
  ;

  chnAddFlagsI(bdup, CHN_INPUT_AVAILABLE);

  bduAddLog(blEndReceive, uReceivedCount);

  maxsize = usbp->epc[ep]->out_maxsize;
  uQueueRemainingSize = iqGetEmptyI(&bdup->iqueue);

  // if(uReceivedCount > uQueueRemainingSize)
    // chprintf((BaseSequentialStream * )&SD2,"Very bad received = %u, space = %u\r\n", uReceivedCount, uQueueRemainingSize);

  size_t uSizeToCopy = MIN(uQueueRemainingSize, uReceivedCount);
  size_t u;
  for(u = 0; u < uSizeToCopy; u++)
  {
    iqPutI(&bdup->iqueue, bduReceiveBuffer[u]);
  }  

  uQueueRemainingSize-= uSizeToCopy;
  __attribute__((unused)) size_t temp = uQueueRemainingSize;
  uQueueRemainingSize = (uQueueRemainingSize / maxsize) * maxsize;  // Make sure we get less packets
  
  if(uQueueRemainingSize!=0)
    bduInitiateReceiveI(bdup, uQueueRemainingSize);

  chSysUnlockFromIsr()
  ;

}

#endif /* HAL_USE_BULK_USB */

/** @} */
