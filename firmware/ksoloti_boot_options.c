#include "ksoloti_boot_options.h"

#include <stdint.h>
#include "ch.h"
#include "hal.h"
#include "flash.h"
#include "sdram.h"

#if INBUILT_MOUNTER_FLASHER
    #define MOUNTER_MAGIC 0x2a4d4f554e544552 /* *MOUNTER */
    #define FLASHER_MAGIC 0x2a464c4153484552 /* *FLASHER */

    volatile uint64_t g_startup_flags __attribute__ ((section (".noinit")));

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

    void CheckForMounterBoot(void)
    {
        // shall we run the mounter?
        // the mounter uses the chibios we have here running from flash
        if(GetStartupFlags() == MOUNTER_MAGIC)
        {
            ResetStartupFlags();
            mounter();
        }
    }

    void CheckForFlasherBoot(void)
    {
        // shall we run the flasher?
        // the flasher needs to run from ram and does not use chibios
        if(GetStartupFlags() == FLASHER_MAGIC)
        {
            ResetStartupFlags();
 
            // enable SDRAM
            configSDRAM();

            // Init hal
            halInit();

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
