#pragma once

#include <stdint.h>

namespace OLED_I2C
{
  void              Start(void);
  void              Stop(void);

  extern volatile uint8_t txbuf[132];
  extern volatile uint8_t rxbuf[8];
}