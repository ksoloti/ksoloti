/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : Target/usbh_conf.h
  * @version        : v1.0_Cube
  * @brief          : Header for usbh_conf.c file.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2025 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __USBH_CONF__H__
#define __USBH_CONF__H__
#ifdef __cplusplus
 extern "C" {
#endif
/* Includes ------------------------------------------------------------------*/

#define DEBUG_ON_GPIO 0

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "ch.h"
#include "migration_v16.h"
#include "mcuconf.h"

#include "stm32h7xx.h"
#include "stm32h7xx_hal.h"

/* USER CODE BEGIN INCLUDE */

/* USER CODE END INCLUDE */

/** @addtogroup STM32_USB_HOST_LIBRARY
  * @{
  */

/** @defgroup USBH_CONF
  * @brief usb host low level driver configuration file
  * @{
  */

/** @defgroup USBH_CONF_Exported_Variables USBH_CONF_Exported_Variables
  * @brief Public variables.
  * @{
  */

/**
  * @}
  */

/** @defgroup USBH_CONF_Exported_Defines USBH_CONF_Exported_Defines
  * @brief Defines for configuration of the Usb host.
  * @{
  */

#define USBH_MAX_NUM_ENDPOINTS                6
#define USBH_MAX_NUM_INTERFACES               6
#define USBH_MAX_NUM_CONFIGURATION            1
#define USBH_KEEP_CFG_DESCRIPTOR              1
#define USBH_MAX_NUM_SUPPORTED_CLASS          3
#define USBH_MAX_SIZE_CONFIGURATION           0x200
#define USBH_MAX_DATA_BUFFER                  0x200
#define USBH_DEBUG_LEVEL                      3
#define USBH_USE_OS                           1

/****************************************/
/* #define for FS and HS identification */
#define HOST_HS 		0
#define HOST_FS 		1

// #if (USBH_USE_OS == 1)
//   #include "cmsis_os.h"
//   #define USBH_PROCESS_PRIO          osPriorityNormal
//   #define USBH_PROCESS_STACK_SIZE    ((uint16_t)640)
// #endif /* (USBH_USE_OS == 1) */

// chbios glue


#define osThreadId Thread *
#define osThreadDef(name, fn, prio, instances, stacksz) \
  static WORKING_AREA(wa##name, 640); \
  Thread *name = chThdCreateStatic(wa##name, sizeof(wa##name), USB_HOST_CONF_PRIO, (void*) fn, phost); \
  phost->os_event = name;
#define osThreadCreate(x,y) x
#define osThread(x) x
#define osThreadTerminate(x) x

#define osMessageQId Thread *

#if !USE_USB_LOG
#define osMessagePutI(q,val,time) chEvtSignalI (q,1<<val);

#define osMessagePut(q,val,time)    \
({                                  \
  if(port_is_isr_context())         \
    chEvtSignalI (q,1<<val);        \
  else                              \
    chEvtSignal (q,1<<val);         \
})
#endif

#define osMessageGet(q,to) chEvtWaitOneTimeout(0xFF, MS2ST(to))
#define osMessageQDef(name, queue_sz, type) \
  static int buf[queue_sz]; \
  MAILBOX_DECL(name, &buf, queue_sz)
#define osMessageCreate(queue_def, thread_id)  thread_id /*&queue_def*/
#define osMessageQ(x) x
#define osWaitForever TIME_INFINITE
#define osEventMessage 1
#define osMessageDelete(x) x
#define osMessageWaiting(x) 0
typedef int osEvent;

#define osDelay(x) HAL_Delay(x)


// Memory management macros 
#define USBH_malloc         fakemalloc
#define USBH_free           fakefree
#define USBH_memset         memset
#define USBH_memcpy         memcpy

extern void* fakemalloc(size_t size);
extern void fakefree(void * p);


// DEBUG macros 
#if (USBH_DEBUG_LEVEL > 0)
  #if ENABLE_SERIAL_DEBUG
    extern void LogUartMessageEol(const char* format, ...);
    #define  USBH_UsrLog(...)   LogUartMessageEol(__VA_ARGS__)
  #else
    extern void LogTextMessage(const char* format, ...);
    #define  USBH_UsrLog(...)   LogTextMessage(__VA_ARGS__)
  #endif
#else
#define USBH_UsrLog(...)
#endif


#if (USBH_DEBUG_LEVEL > 1)
  #if ENABLE_SERIAL_DEBUG
    #define  USBH_ErrLog(...)   LogUartMessageEol(__VA_ARGS__)
  #else
    #define  USBH_ErrLog(...)   LogTextMessage(__VA_ARGS__)
  #endif
#else
#define USBH_ErrLog(...)
#endif


#if (USBH_DEBUG_LEVEL > 2)
  #if ENABLE_SERIAL_DEBUG
    #define  USBH_DbgLog(...)   LogUartMessageEol(__VA_ARGS__)
  #else
    #define  USBH_DbgLog(...)   LogTextMessage(__VA_ARGS__)
  #endif
#else
#define USBH_DbgLog(...)
#endif

#ifdef __cplusplus
}
#endif

#endif /* __USBH_CONF__H__ */

