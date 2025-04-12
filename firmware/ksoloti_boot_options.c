#include "ksoloti_boot_options.h"

#include <stdint.h>
#include "ch.h"
#include "hal.h"
#include "flash.h"
#include "sdram.h"
#include "axoloti_board.h"

#if INBUILT_MOUNTER_FLASHER
  #define MOUNTER_MAGIC 0x2a4d4f554e544552 /* *MOUNTER */
  #define FLASHER_MAGIC 0x2a464c4153484552 /* *FLASHER */
  #define RESETER_MAGIC 0x524553455445520A /* *RESETER */

  volatile uint64_t g_startup_flags __attribute__((section(".noinit")));
  volatile uint64_t g_reset_flags __attribute__((section(".noinit")));

  extern int mounter(void);
  extern void flasher(void);

  static void SetStartupFlags(uint64_t uValue)
  {
    g_startup_flags = uValue;
  #if BOARD_KSOLOTI_CORE_H743
    SCB_CleanInvalidateDCache();
  #endif
    NVIC_SystemReset();
  }

  static void ResetStartupFlags(void)
  {
    g_startup_flags = 0;
  #if BOARD_KSOLOTI_CORE_H743
    SCB_CleanInvalidateDCache();
  #endif
  }

  static uint64_t GetStartupFlags(void)
  {
    return g_startup_flags;
  }

  void StartFlasher(void)
  {
    SetStartupFlags(FLASHER_MAGIC);
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
#if BOARD_KSOLOTI_CORE_H743
      SCB_CleanInvalidateDCache();
#endif      
    }
    else
    {
      g_reset_flags = RESETER_MAGIC;
#if BOARD_KSOLOTI_CORE_H743
      SCB_CleanInvalidateDCache();
#endif      
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

  void CheckForFlasherBoot(void)
  {
    // shall we run the flasher?
    // the flasher needs to run from ram and does not use chibios
    if (GetStartupFlags() == FLASHER_MAGIC)
    {
      ResetStartupFlags();

      // enable SDRAM
      configSDRAM();

      // Init hal
      halInit();

      /* Float USB inputs, hope the host notices detach... */
      palSetPadMode(GPIOA, 11, PAL_MODE_INPUT);
      palSetPadMode(GPIOA, 12, PAL_MODE_INPUT);

      // Set up LEDs
      palSetPadMode(LED1_PORT, LED1_PIN, PAL_MODE_OUTPUT_PUSHPULL);
      palSetPad(LED1_PORT, LED1_PIN);
      palSetPadMode(LED2_PORT, LED2_PIN, PAL_MODE_OUTPUT_PUSHPULL);

      // now execute from ram
      FlashFirmware();
    }
  }
#else
  void StartFlasher(void) {}
  void StartMounter(void) {}
  void CheckForMounterBoot(void) {}
  void CheckForFlasherBoot(void) {}
#endif
