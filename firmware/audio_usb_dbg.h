#pragma once

#if ADU_TRANSFER_LOG_SIZE
typedef enum _BLType {blStartTransmit, blStartReceive, blEndTransmit, blEndReceive} BLType;
 
typedef struct _DBGLOG
{
  BLType    type;
  uint16_t  uSize;
} DBGLOG;

DBGLOG aduTransferLog[ADU_TRANSFER_LOG_SIZE] __attribute__ ((section (".sram3")));
uint16_t aduLogCount = 0;

void aduAddTransferLog(BLType type, uint16_t uSize)
{
  if(aduLogCount == 0)
    memset(aduTransferLog, 0, sizeof(aduTransferLog));

  aduTransferLog[aduLogCount].type = type;
  aduTransferLog[aduLogCount].uSize = uSize;
  aduLogCount++;
  if(aduLogCount == ADU_TRANSFER_LOG_SIZE)
    aduLogCount = 0;
}
#else
  #define aduAddTransferLog(a, b)
#endif 


#if ADU_OVERRUN_LOG_SIZE
typedef enum _LogType
{
  ltCodecCopyEnd___,
  ltFrameEndedEnd__,
  ltSampleAdjusted_,
  ltWaitingForSync_,
  ltAfterTXAdjust__,
  ltAfterRXAdjust__,
  ltAfterDataRX____,
  ltTxRxSynced_____,
  ltResetForSync___,
  ltErrorBefore____,
  ltErrorAfter_____,
  ltStartReceive___,
  ltStartTransmit__,
  ltUSBReset_______
} LogType; 

typedef struct _OverrunDebug
{
  uint16_t I;
  LogType  type;
  uint16_t txUsedSize;
  uint16_t txWriteOffset;
  uint16_t txReadOffset;
  uint16_t rxUsedSize;
  uint16_t C;
  uint16_t rxWriteOffset;
  uint16_t rxReadOffset;
  int16_t  sampleOffset;
  uint16_t codecFrameSampleCount;
  int16_t  codecMetricsSampleOffset;
  ADUState state;

//  uint16_t txCurrentRingBufferSize;
} __attribute__((packed)) OverrunDebug;

OverrunDebug overrunDebug[ADU_OVERRUN_LOG_SIZE]   __attribute__ ((section (".sram3")));
uint16_t uLogIndex = 0;

void AddOverunLog(LogType type)
{
  // if((type == ltCodecCopyEnd___) || (type == ltAfterDataRX____))
  {
    overrunDebug[uLogIndex].type = type;
    overrunDebug[uLogIndex].I = aduState.currentFrame;
    overrunDebug[uLogIndex].txUsedSize = aduState.txRingBufferUsedSize;
    overrunDebug[uLogIndex].txWriteOffset = aduState.txRingBufferWriteOffset;
    overrunDebug[uLogIndex].txReadOffset = aduState.txRingBufferReadOffset;
    overrunDebug[uLogIndex].rxUsedSize = aduState.rxRingBufferUsedSize;
    overrunDebug[uLogIndex].rxWriteOffset = aduState.rxRingBufferWriteOffset;
    overrunDebug[uLogIndex].rxReadOffset = aduState.rxRingBufferReadOffset;
    overrunDebug[uLogIndex].sampleOffset = aduState.sampleOffset;
    overrunDebug[uLogIndex].codecFrameSampleCount = aduState.codecFrameSampleCount;
    overrunDebug[uLogIndex].codecMetricsSampleOffset = aduState.codecMetricsSampleOffset;
    overrunDebug[uLogIndex].state = aduState.state;

    if((aduState.rxRingBufferWriteOffset < aduState.rxRingBufferReadOffset))
    {
      overrunDebug[uLogIndex].C = (aduState.rxRingBufferWriteOffset + TX_RING_BUFFER_FULL_SIZE) - aduState.rxRingBufferReadOffset;
    }
    else
      overrunDebug[uLogIndex].C = aduState.rxRingBufferWriteOffset - aduState.rxRingBufferReadOffset;

    uLogIndex++;
    if(uLogIndex == ADU_OVERRUN_LOG_SIZE)
      uLogIndex = 0;
  }
}
#else
  #define AddOverunLog(a)
#endif 

