/*
 * Copyright (C) 2013, 2014 Johannes Taelman
 * Edited 2023 - 2024 by Ksoloti
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * @file    templates/chconf.h
 * @brief   Configuration file template.
 * @details A copy of this file must be placed in each project directory, it
 *          contains the application specific kernel settings.
 *
 * @addtogroup config
 * @details Kernel related settings and hooks.
 * @{
 */
#ifndef _CHCONF_H_
#define _CHCONF_H_

#define _CHIBIOS_RT_CONF_
#define _CHIBIOS_RT_CONF_VER_6_1_
#define _CHIBIOS_HAL_CONF_
#define _CHIBIOS_HAL_CONF_VER_7_1_

#define SHELL_CMD_MEM_ENABLED               FALSE
#define SHELL_CMD_TEST_ENABLED              FALSE
/*===========================================================================*/
/**
 * @name Kernel parameters and options
 * @{
 */
/*===========================================================================*/

#define CH_CFG_ST_RESOLUTION                32
#define CH_CFG_ST_FREQUENCY                 10000
#define CH_CFG_INTERVALS_SIZE               32
#define CH_CFG_ST_TIMEDELTA                 0
#define CH_CFG_TIME_TYPES_SIZE              32
#define CH_CFG_USE_TM                       TRUE
#define CH_DBG_STATISTICS                   FALSE
#define CH_DBG_TRACE_MASK                   CH_DBG_TRACE_MASK_DISABLED
#define CH_CFG_USE_MUTEXES_RECURSIVE        FALSE
#define CH_DBG_TRACE_BUFFER_SIZE            128
#define CH_CFG_SYSTEM_EXTRA_FIELDS                                          \
  /* Add threads custom fields here.*/
#define CH_CFG_SYSTEM_INIT_HOOK() {                                         \
  /* Add threads initialization code here.*/                                \
}
#define CH_CFG_THREAD_EXTRA_FIELDS                                          \
  /* Add threads custom fields here.*/
#define CH_CFG_THREAD_INIT_HOOK(tp) {                                       \
  /* Add threads initialization code here.*/                                \
}
#define CH_CFG_THREAD_EXIT_HOOK(tp) {                                       \
  /* Add threads finalization code here.*/                                  \
}
#define CH_CFG_IRQ_PROLOGUE_HOOK() {                                        \
  /* IRQ prologue code here.*/                                              \
}
#define CH_CFG_IRQ_EPILOGUE_HOOK() {                                        \
  /* IRQ epilogue code here.*/                                              \
}
#define CH_CFG_IDLE_ENTER_HOOK() {                                          \
  /* Idle-enter code here.*/                                                \
}
#define CH_CFG_IDLE_LEAVE_HOOK() {                                          \
  /* Idle-leave code here.*/                                                \
}
#define CH_CFG_SYSTEM_TICK_HOOK() {                                         \
  /* System tick event code here.*/                                         \
}
#define CH_CFG_SYSTEM_HALT_HOOK(reason) {                                   \
  /* System halt code here.*/                                               \
}
#define CH_CFG_TRACE_HOOK(tep) {                                            \
  /* Trace code here.*/                                                     \
}


 
#if !defined(CH_CFG_USE_OBJ_FIFOS)
#define CH_CFG_USE_OBJ_FIFOS                FALSE
#endif

/**
 * @brief   Pipes APIs.
 * @details If enabled then the pipes APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_PIPES)
#define CH_CFG_USE_PIPES                    FALSE
#endif

/**
 * @brief   Objects Caches APIs.
 * @details If enabled then the objects caches APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_OBJ_CACHES)
#define CH_CFG_USE_OBJ_CACHES               FALSE
#endif

/**
 * @brief   Delegate threads APIs.
 * @details If enabled then the delegate threads APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_DELEGATES)
#define CH_CFG_USE_DELEGATES                FALSE
#endif

/**
 * @brief   Jobs Queues APIs.
 * @details If enabled then the jobs queues APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_JOBS)
#define CH_CFG_USE_JOBS                     FALSE
#endif

/** @} */

/*===========================================================================*/
/**
 * @name Objects factory options
 * @{
 */
/*===========================================================================*/

/**
 * @brief   Objects Factory APIs.
 * @details If enabled then the objects factory APIs are included in the
 *          kernel.
 *
 * @note    The default is @p FALSE.
 */
#if !defined(CH_CFG_USE_FACTORY)
#define CH_CFG_USE_FACTORY                  FALSE
#endif

/**
 * @brief   Maximum length for object names.
 * @details If the specified length is zero then the name is stored by
 *          pointer but this could have unintended side effects.
 */
#if !defined(CH_CFG_FACTORY_MAX_NAMES_LENGTH)
#define CH_CFG_FACTORY_MAX_NAMES_LENGTH     8
#endif

/**
 * @brief   Enables the registry of generic objects.
 */
#if !defined(CH_CFG_FACTORY_OBJECTS_REGISTRY)
#define CH_CFG_FACTORY_OBJECTS_REGISTRY     FALSE
#endif

/**
 * @brief   Enables factory for generic buffers.
 */
#if !defined(CH_CFG_FACTORY_GENERIC_BUFFERS)
#define CH_CFG_FACTORY_GENERIC_BUFFERS      FALSE
#endif

/**
 * @brief   Enables factory for semaphores.
 */
#if !defined(CH_CFG_FACTORY_SEMAPHORES)
#define CH_CFG_FACTORY_SEMAPHORES           FALSE
#endif

/**
 * @brief   Enables factory for mailboxes.
 */
#if !defined(CH_CFG_FACTORY_MAILBOXES)
#define CH_CFG_FACTORY_MAILBOXES            FALSE
#endif

/**
 * @brief   Enables factory for objects FIFOs.
 */
#if !defined(CH_CFG_FACTORY_OBJ_FIFOS)
#define CH_CFG_FACTORY_OBJ_FIFOS            FALSE
#endif

/**
 * @brief   Memory Pools Allocator APIs.
 * @details If enabled then the memory pools allocator APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_MEMPOOLS)
#define CH_CFG_USE_MEMPOOLS                 FALSE
#endif

/**
 * @brief   Mailboxes APIs.
 * @details If enabled then the asynchronous messages (mailboxes) APIs are
 *          included in the kernel.
 *
 * @note    The default is @p TRUE.
 * @note    Requires @p CH_CFG_USE_SEMAPHORES.
 */
#if !defined(CH_CFG_USE_MAILBOXES)
#define CH_CFG_USE_MAILBOXES                TRUE
#endif

/**
 * @brief   System tick frequency.
 * @details Frequency of the system timer that drives the system ticks. This
 *          setting also defines the system tick time unit.
 */
#if !defined(CH_CFG_FREQUENCY) || defined(__DOXYGEN__)
#define CH_CFG_FREQUENCY                    1000
#endif

/**
 * @brief   Round robin interval.
 * @details This constant is the number of system ticks allowed for the
 *          threads before preemption occurs. Setting this value to zero
 *          disables the preemption for threads with equal priority and the
 *          round robin becomes cooperative. Note that higher priority
 *          threads can still preempt, the kernel is always preemptive.
 *
 * @note    Disabling the round robin preemption makes the kernel more compact
 *          and generally faster.
 */
#if !defined(CH_CFG_TIME_QUANTUM) || defined(__DOXYGEN__)
#define CH_CFG_TIME_QUANTUM                 20
#endif

/**
 * @brief   Managed RAM size.
 * @details Size of the RAM area to be managed by the OS. If set to zero
 *          then the whole available RAM is used. The core memory is made
 *          available to the heap allocator and/or can be used directly through
 *          the simplified core memory allocator.
 *
 * @note    In order to let the OS manage the whole RAM the linker script must
 *          provide the @p __heap_base__ and @p __heap_end__ symbols.
 * @note    Requires @p CH_CFG_USE_MEMCORE.
 */
#if !defined(CH_CFG_MEMCORE_SIZE) || defined(__DOXYGEN__)
#define CH_CFG_MEMCORE_SIZE                 0
#endif

/**
 * @brief   Idle thread automatic spawn suppression.
 * @details When this option is activated the function @p chSysInit()
 *          does not spawn the idle thread automatically. The application has
 *          then the responsibility to do one of the following:
 *          - Spawn a custom idle thread at priority @p IDLEPRIO.
 *          - Change the main() thread priority to @p IDLEPRIO then enter
 *            an endless loop. In this scenario the @p main() thread acts as
 *            the idle thread.
 *          .
 * @note    Unless an idle thread is spawned the @p main() thread must not
 *          enter a sleep state.
 */
#if !defined(CH_CFG_NO_IDLE_THREAD) || defined(__DOXYGEN__)
#define CH_CFG_NO_IDLE_THREAD               FALSE
#endif

/** @} */

/*===========================================================================*/
/**
 * @name Performance options
 * @{
 */
/*===========================================================================*/

/**
 * @brief   OS optimization.
 * @details If enabled then time efficient rather than space efficient code
 *          is used when two possible implementations exist.
 *
 * @note    This is not related to the compiler optimization options.
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_OPTIMIZE_SPEED) || defined(__DOXYGEN__)
#define CH_CFG_OPTIMIZE_SPEED               TRUE
#endif

/** @} */

/*===========================================================================*/
/**
 * @name Subsystem options
 * @{
 */
/*===========================================================================*/

/**
 * @brief   Threads registry APIs.
 * @details If enabled then the registry APIs are included in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_REGISTRY) || defined(__DOXYGEN__)
#define CH_CFG_USE_REGISTRY                 TRUE
#endif

/**
 * @brief   Threads synchronization APIs.
 * @details If enabled then the @p chThdWait() function is included in
 *          the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_WAITEXIT) || defined(__DOXYGEN__)
#define CH_CFG_USE_WAITEXIT                 TRUE
#endif

/**
 * @brief   Semaphores APIs.
 * @details If enabled then the Semaphores APIs are included in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_SEMAPHORES) || defined(__DOXYGEN__)
#define CH_CFG_USE_SEMAPHORES               TRUE
#endif

/**
 * @brief   Semaphores queuing mode.
 * @details If enabled then the threads are enqueued on semaphores by
 *          priority rather than in FIFO order.
 *
 * @note    The default is @p FALSE. Enable this if you have special requirements.
 * @note    Requires @p CH_CFG_USE_SEMAPHORES.
 */
#if !defined(CH_CFG_USE_SEMAPHORES_PRIORITY) || defined(__DOXYGEN__)
#define CH_CFG_USE_SEMAPHORES_PRIORITY      FALSE
#endif

/**
 * @brief   Atomic semaphore API.
 * @details If enabled then the semaphores the @p chSemSignalWait() API
 *          is included in the kernel.
 *
 * @note    The default is @p TRUE.
 * @note    Requires @p CH_CFG_USE_SEMAPHORES.
 */
#if !defined(CH_CFG_USE_SEMSW) || defined(__DOXYGEN__)
#define CH_CFG_USE_SEMSW                    TRUE
#endif

/**
 * @brief   Mutexes APIs.
 * @details If enabled then the mutexes APIs are included in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_MUTEXES) || defined(__DOXYGEN__)
#define CH_CFG_USE_MUTEXES                  TRUE
#endif

/**
 * @brief   Conditional Variables APIs.
 * @details If enabled then the conditional variables APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 * @note    Requires @p CH_CFG_USE_MUTEXES.
 */
#if !defined(CH_CFG_USE_CONDVARS) || defined(__DOXYGEN__)
#define CH_CFG_USE_CONDVARS                 FALSE
#endif

/**
 * @brief   Conditional Variables APIs with timeout.
 * @details If enabled then the conditional variables APIs with timeout
 *          specification are included in the kernel.
 *
 * @note    The default is @p TRUE.
 * @note    Requires @p CH_CFG_USE_CONDVARS.
 */
#if !defined(CH_CFG_USE_CONDVARS_TIMEOUT) || defined(__DOXYGEN__)
#define CH_CFG_USE_CONDVARS_TIMEOUT         FALSE
#endif

/**
 * @brief   Events Flags APIs.
 * @details If enabled then the event flags APIs are included in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_EVENTS) || defined(__DOXYGEN__)
#define CH_CFG_USE_EVENTS                   TRUE
#endif

/**
 * @brief   Events Flags APIs with timeout.
 * @details If enabled then the events APIs with timeout specification
 *          are included in the kernel.
 *
 * @note    The default is @p TRUE.
 * @note    Requires @p CH_CFG_USE_EVENTS.
 */
#if !defined(CH_CFG_USE_EVENTS_TIMEOUT) || defined(__DOXYGEN__)
#define CH_CFG_USE_EVENTS_TIMEOUT           TRUE
#endif

/**
 * @brief   Synchronous Messages APIs.
 * @details If enabled then the synchronous messages APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_MESSAGES) || defined(__DOXYGEN__)
#define CH_CFG_USE_MESSAGES                 TRUE
#endif

/**
 * @brief   Synchronous Messages queuing mode.
 * @details If enabled then messages are served by priority rather than in
 *          FIFO order.
 *
 * @note    The default is @p FALSE. Enable this if you have special requirements.
 * @note    Requires @p CH_CFG_USE_MESSAGES.
 */
#if !defined(CH_CFG_USE_MESSAGES_PRIORITY) || defined(__DOXYGEN__)
#define CH_CFG_USE_MESSAGES_PRIORITY        FALSE
#endif

/**
 * @brief   Mailboxes APIs.
 * @details If enabled then the asynchronous messages (mailboxes) APIs are
 *          included in the kernel.
 *
 * @note    The default is @p TRUE.
 * @note    Requires @p CH_CFG_USE_SEMAPHORES.
 */
#if !defined(CH_CFG_USE_MAILBOXES) || defined(__DOXYGEN__)
#define CH_CFG_USE_MAILBOXES                FALSE
#endif

/**
 * @brief   I/O Queues APIs.
 * @details If enabled then the I/O queues APIs are included in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_QUEUES) || defined(__DOXYGEN__)
#define CH_CFG_USE_QUEUES                   TRUE
#endif

/**
 * @brief   Core Memory Manager APIs.
 * @details If enabled then the core memory manager APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_MEMCORE) || defined(__DOXYGEN__)
#define CH_CFG_USE_MEMCORE                  TRUE
#endif

/**
 * @brief   Heap Allocator APIs.
 * @details If enabled then the memory heap allocator APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 * @note    Requires @p CH_CFG_USE_MEMCORE and either @p CH_CFG_USE_MUTEXES or
 *          @p CH_CFG_USE_SEMAPHORES.
 * @note    Mutexes are recommended.
 */
#if !defined(CH_CFG_USE_HEAP) || defined(__DOXYGEN__)
#define CH_CFG_USE_HEAP                     FALSE
#endif

/**
 * @brief   C-runtime allocator.
 * @details If enabled the the heap allocator APIs just wrap the C-runtime
 *          @p malloc() and @p free() functions.
 *
 * @note    The default is @p FALSE.
 * @note    Requires @p CH_CFG_USE_HEAP.
 * @note    The C-runtime may or may not require @p CH_CFG_USE_MEMCORE, see the
 *          appropriate documentation.
 */
#if !defined(CH_CFG_USE_MALLOC_HEAP) || defined(__DOXYGEN__)
#define CH_CFG_USE_MALLOC_HEAP              FALSE
#endif

/**
 * @brief   Memory Pools Allocator APIs.
 * @details If enabled then the memory pools allocator APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 */
#if !defined(CH_CFG_USE_MEMPOOLS) || defined(__DOXYGEN__)
#define CH_CFG_USE_MEMPOOLS                 FALSE
#endif

/**
 * @brief   Dynamic Threads APIs.
 * @details If enabled then the dynamic threads creation APIs are included
 *          in the kernel.
 *
 * @note    The default is @p TRUE.
 * @note    Requires @p CH_CFG_USE_WAITEXIT.
 * @note    Requires @p CH_CFG_USE_HEAP and/or @p CH_CFG_USE_MEMPOOLS.
 */
#if !defined(CH_CFG_USE_DYNAMIC) || defined(__DOXYGEN__)
#define CH_CFG_USE_DYNAMIC                  FALSE
#endif

/** @} */

/*===========================================================================*/
/**
 * @name Debug options
 * @{
 */
/*===========================================================================*/

/**
 * @brief   Debug option, system state check.
 * @details If enabled the correct call protocol for system APIs is checked
 *          at runtime.
 *
 * @note    The default is @p FALSE.
 */
#if !defined(CH_DBG_SYSTEM_STATE_CHECK) || defined(__DOXYGEN__)
#define CH_DBG_SYSTEM_STATE_CHECK       FALSE
#endif

/**
 * @brief   Debug option, parameters checks.
 * @details If enabled then the checks on the API functions input
 *          parameters are activated.
 *
 * @note    The default is @p FALSE.
 */
#if !defined(CH_DBG_ENABLE_CHECKS) || defined(__DOXYGEN__)
#define CH_DBG_ENABLE_CHECKS            FALSE
#endif

/**
 * @brief   Debug option, consistency checks.
 * @details If enabled then all the assertions in the kernel code are
 *          activated. This includes consistency checks inside the kernel,
 *          runtime anomalies and port-defined checks.
 *
 * @note    The default is @p FALSE.
 */
#if !defined(CH_DBG_ENABLE_ASSERTS) || defined(__DOXYGEN__)
#define CH_DBG_ENABLE_ASSERTS           FALSE
#endif

/**
 * @brief   Debug option, trace buffer.
 * @details If enabled then the context switch circular trace buffer is
 *          activated.
 *
 * @note    The default is @p FALSE.
 */
#if !defined(CH_DBG_ENABLE_TRACE) || defined(__DOXYGEN__)
#define CH_DBG_ENABLE_TRACE             FALSE
#endif

/**
 * @brief   Debug option, stack checks.
 * @details If enabled then a runtime stack check is performed.
 *
 * @note    The default is @p FALSE.
 * @note    The stack check is performed in a architecture/port dependent way.
 *          It may not be implemented or some ports.
 * @note    The default failure mode is to halt the system with the global
 *          @p panic_msg variable set to @p NULL.
 */
#if !defined(CH_DBG_ENABLE_STACK_CHECK) || defined(__DOXYGEN__)
#define CH_DBG_ENABLE_STACK_CHECK       FALSE
#endif

/**
 * @brief   Debug option, stacks initialization.
 * @details If enabled then the threads working area is filled with a byte
 *          value when a thread is created. This can be useful for the
 *          runtime measurement of the used stack.
 *
 * @note    The default is @p FALSE.
 */
#if !defined(CH_DBG_FILL_THREADS) || defined(__DOXYGEN__)
#define CH_DBG_FILL_THREADS             TRUE
#endif

/**
 * @brief   Debug option, threads profiling.
 * @details If enabled then a field is added to the @p Thread structure that
 *          counts the system ticks occurred while executing the thread.
 *
 * @note    The default is @p TRUE.
 * @note    This debug option is defaulted to TRUE because it is required by
 *          some test cases into the test suite.
 */
#if !defined(CH_DBG_THREADS_PROFILING) || defined(__DOXYGEN__)
#define CH_DBG_THREADS_PROFILING        FALSE
#endif

/** @} */

/*===========================================================================*/
/**
 * @name Kernel hooks
 * @{
 */
/*===========================================================================*/

/**
 * @brief   Threads descriptor structure extension.
 * @details User fields added to the end of the @p Thread structure.
 */
#if !defined(THREAD_EXT_FIELDS) || defined(__DOXYGEN__)
#define THREAD_EXT_FIELDS                                                   \
  /* Add threads custom fields here.*/
#endif

/**
 * @brief   Threads initialization hook.
 * @details User initialization code added to the @p chThdInit() API.
 *
 * @note    It is invoked from within @p chThdInit() and implicitly from all
 *          the threads creation APIs.
 */
#if !defined(THREAD_EXT_INIT_HOOK) || defined(__DOXYGEN__)
#define THREAD_EXT_INIT_HOOK(tp) {                                          \
  /* Add threads initialization code here.*/                                \
}
#endif

/**
 * @brief   Threads finalization hook.
 * @details User finalization code added to the @p chThdExit() API.
 *
 * @note    It is inserted into lock zone.
 * @note    It is also invoked when the threads simply return in order to
 *          terminate.
 */
#if !defined(THREAD_EXT_EXIT_HOOK) || defined(__DOXYGEN__)
#define THREAD_EXT_EXIT_HOOK(tp) {                                          \
  /* Add threads finalization code here.*/                                  \
}
#endif

/**
 * @brief   Context switch hook.
 * @details This hook is invoked just before switching between threads.
 */
#if !defined(THREAD_CONTEXT_SWITCH_CFG_HOOK) || defined(__DOXYGEN__)
#define THREAD_CONTEXT_SWITCH_CFG_HOOK(ntp, otp) {                              \
  /* System halt code here.*/                                               \
}
#endif

/**
 * @brief   Idle Loop hook.
 * @details This hook is continuously invoked by the idle thread loop.
 */
#if !defined(IDLE_LOOP_HOOK) || defined(__DOXYGEN__)
#define IDLE_LOOP_HOOK() {                                                  \
  /* Idle loop code here.*/                                                 \
}
#endif

/**
 * @brief   System tick event hook.
 * @details This hook is invoked in the system tick handler immediately
 *          after processing the virtual timers queue.
 */
#if !defined(SYSTEM_TICK_EVENT_HOOK) || defined(__DOXYGEN__)
#define SYSTEM_TICK_EVENT_HOOK() {                                          \
  /* System tick event code here.*/                                         \
}
#endif

/**
 * @brief   System halt hook.
 * @details This hook is invoked in case to a system halting error before
 *          the system is halted.
 */
#if !defined(SYSTEM_HALT_HOOK) || defined(__DOXYGEN__)
#define SYSTEM_HALT_HOOK() {                                                \
  /* System halt code here.*/                                               \
}
#endif


/**
 * @brief   Context switch hook.
 * @details This hook is invoked just before switching between threads.
 */
#define CH_CFG_CONTEXT_SWITCH_HOOK(ntp, otp) {                              \
  /* Context switch code here.*/                                            \
}

/**
 * @brief   Idle Loop hook.
 * @details This hook is continuously invoked by the idle thread loop.
 */
#define CH_CFG_IDLE_LOOP_HOOK() {                                           \
  /* Idle loop code here.*/                                                 \
}

/** @} */

/*===========================================================================*/
/* Port-specific settings (override port settings defaulted in chcore.h).    */
/*===========================================================================*/

/*===========================================================================*/
/* Other                                                                     */
/*===========================================================================*/

#define CHPRINTF_USE_FLOAT  FALSE

#endif  /* _CHCONF_H_ */

/** @} */
