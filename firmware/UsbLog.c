#include "UsbLog.h"

#if USE_USB_LOG
UsbLog usbLog[USB_LOG_SIZE];
uint32_t uUsbLogIndex = 0;

void AddUsbLog(UsbLogType type, uint8_t value)
{
  usbLog[uUsbLogIndex].type = type;
  usbLog[uUsbLogIndex].value = value;

  uUsbLogIndex++;
  if(uUsbLogIndex == USB_LOG_SIZE)
    uUsbLogIndex = 0;
}
#define AddUsbLog(a,b)
#endif