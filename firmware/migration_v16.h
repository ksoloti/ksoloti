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

#define RTT2US(ticks) ((ticks) / (168000000 / 1000000UL))


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

// V18 here down

// #define CH_USE_REGISTRY CH_CFG_USE_REGISTRY
// #define THD_STATE_NAMES CH_STATE_NAMES
// #define MS2ST TIME_MS2I

