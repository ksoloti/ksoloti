#pragma once

#define bool_t bool
#define Thread thread_t
#define WORKING_AREA THD_WORKING_AREA
#define VirtualTimer virtual_timer_t
#define EventSource event_source_t
#define GenericQueue io_queue_t
#define InputQueue io_queue_t
#define OutputQueue io_queue_t
#define EventListener event_listener_t
#define chSysLockFromIsr chSysLockFromISR
#define chSysUnlockFromIsr 	chSysUnlockFromISR
#define chEvtInit chEvtObjectInit
#define chQGetLink qGetLink
#define chIQInit iqObjectInit
#define chOQInit oqObjectInit

#define SCB_FPCCR                     *((uint32_t *)0xE000EF34U)

#define hal_lld_get_counter_value port_rt_get_counter_value


#if BOARD_KSOLOTI_CORE_H743
  #define RTT2US(ticks) ((ticks) / (480000000 / 1000000UL))
  #define RTT2MS(ticks) ((ticks) / (480000000 / 1000UL))
  #define US2RTT(usec) (((480000000 + 999999UL) / 1000000UL) * (usec))
  #define MS2RTT(msec) (((480000000 + 999UL) / 1000UL) * (msec))
#else
  #define RTT2US(ticks) ((ticks) / (STM32_SYSCLK / 1000000UL))
  #define RTT2MS(ticks) ((ticks) / (STM32_SYSCLK / 1000UL))
  #define US2RTT(usec) (((STM32_SYSCLK + 999999UL) / 1000000UL) * (usec))
  #define MS2RTT(msec) (((STM32_SYSCLK + 999UL) / 1000UL) * (msec))
#endif




// V16 here down
#define PAL_STM32_PUDR_PULLUP PAL_MODE_INPUT_PULLUP

#define chTimeNow 	chVTGetSystemTimeX
#define chTimeElapsedSince 	chVTTimeElapsedSinceX
#define chTimeIsWithin 	chVTIsTimeWithinX
#define chThdSelf 	chThdGetSelfX
#define chThdGetPriority 	chThdGetPriorityX
#define chThdGetTicks 	chThdGetTicksX
#define chThdTerminated 	chThdTerminatedX
#define chThdShouldTerminate 	chThdShouldTerminateX
#define chRegGetThreadName chRegGetThreadNameX

#define PACK_STRUCT_BEGIN
#define PACK_STRUCT_STRUCT __attribute__((packed))
#define PACK_STRUCT_END

#define CH_USE_REGISTRY CH_CFG_USE_REGISTRY
#define THD_STATE_NAMES CH_STATE_NAMES
#define CH_STACK_FILL_VALUE CH_DBG_STACK_FILL_VALUE

// V18 here down

#define MS2ST TIME_MS2I





// 20.3 notes.
// 1. external pump - looks like this is not needed nymore as chibios is now not using a thread for this.
