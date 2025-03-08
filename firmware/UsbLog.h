#pragma once

#if USE_USB_LOG
#include <stdint.h>

typedef enum _UsbLogType
{
  ltState,
  ltPut,
  ltGet
} UsbLogType;

typedef struct _UsbLog
{
  UsbLogType type;
  uint8_t    value;
} UsbLog;

#define USB_LOG_SIZE 2000

extern UsbLog usbLog[USB_LOG_SIZE];
extern void AddUsbLog(UsbLogType type, uint8_t value);
#else
  #define AddUsbLog(a,b)
#endif 