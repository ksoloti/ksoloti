#include "oled_i2c.h"

#include "ch.h"
#include "hal.h"
#include "cache.h"
#include "mcuconf.h"
#include "hal_pal.h"

extern I2CDriver I2CD1;

namespace OLED_I2C
{
#if BOARD_KSOLOTI_CORE_H743
  uint8_t txbuf[132] ADC_DMA_DATA_SECTION1;
  uint8_t rxbuf[8] ADC_DMA_DATA_SECTION1;
#else
  uint8_t txbuf[132] __attribute__((section(".sram2")));
  uint8_t rxbuf[8] __attribute__((section(".sram2")));
#endif

  void Start(void)
  {
    palSetPadMode(GPIOB, 8, PAL_MODE_ALTERNATE(4) | PAL_STM32_PUDR_PULLUP | PAL_STM32_OTYPE_OPENDRAIN); // SCL
    palSetPadMode(GPIOB, 9, PAL_MODE_ALTERNATE(4) | PAL_STM32_PUDR_PULLUP | PAL_STM32_OTYPE_OPENDRAIN); // SDA

#if BOARD_KSOLOTI_CORE_H743
    static const I2CConfig i2cfg =
        {
            // see RM0433 for TIMINGR configuration values
            STM32_TIMINGR_PRESC(0) |
                STM32_TIMINGR_SCLDEL(3U) | STM32_TIMINGR_SDADEL(3U) |
                STM32_TIMINGR_SCLH(3U) | STM32_TIMINGR_SCLL(9U),
            0,
            0};
#else
    static const I2CConfig i2cfg =
        {
            OPMODE_I2C,
            400000,
            FAST_DUTY_CYCLE_2,
        };
#endif

    i2cStart(&I2CD1, &i2cfg);
  }

  void Stop(void)
  {
    i2cStop(&I2CD1);
    palSetPadMode(GPIOB, 8, PAL_MODE_INPUT_ANALOG);
    palSetPadMode(GPIOB, 9, PAL_MODE_INPUT_ANALOG);
  }

  // volatile uint8_t *GetTxbuf(void)
  // {
  //   return txbuf;
  // }

  // volatile uint8_t *GetRxbuf(void)
  // {
  //   return rxbuf;
  // }

}