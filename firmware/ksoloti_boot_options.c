#include "ksoloti_boot_options.h"

#include <stdint.h>
#include "ch.h"
#include "hal.h"
#include "flash.h"
#include "sdram.h"
#include "axoloti_board.h"

#if INBUILT_MOUNTER
  #define MOUNTER_MAGIC 0x2a4d4f554e544552 /* *MOUNTER */
  #define RESETER_MAGIC 0x524553455445520A /* *RESETER */

  volatile uint64_t g_startup_flags __attribute__((section(".noinit")));
  volatile uint64_t g_reset_flags __attribute__((section(".noinit")));

  extern int mounter(void);

  static void SetStartupFlags(uint64_t uValue)
  {
    g_startup_flags = uValue;
    NVIC_SystemReset();
  }

  static void ResetStartupFlags(void)
  {
    g_startup_flags = 0;
  }

  static uint64_t GetStartupFlags(void)
  {
    return g_startup_flags;
  }

  void StartMounter(void)
  {
    SetStartupFlags(MOUNTER_MAGIC);
  }
  
  void CheckForReset(void)
  {
    if (g_reset_flags == RESETER_MAGIC)
    {
      g_reset_flags = 0;
    }
    else
    {
      g_reset_flags = RESETER_MAGIC;
      NVIC_SystemReset();
    }
  }

  void CheckForMounterBoot(void)
  {
    // shall we run the mounter?
    // the mounter uses the chibios we have here running from flash
    if (GetStartupFlags() == MOUNTER_MAGIC)
    {
      ResetStartupFlags();
      mounter();
    }
  }
  #else
  void StartMounter(void) {}
  void CheckForMounterBoot(void) {}
  #endif
