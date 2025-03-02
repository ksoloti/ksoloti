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
#include "ch.h"
#include "hal.h"
#include "axoloti_control.h"
#include "axoloti_board.h"
#include "ui.h"
#include <string.h>

#define _BV(bit) (1 << (bit))

uint8_t lcd_buffer[(AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH) * AXOLOTI_CONTROL_LCDROWS] LCD_DATA_SECTION;
uint8_t led_buffer[AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH] LCD_DATA_SECTION;


void LCD_updateBoundingBox(int x, int y, int x2, int y2) {
  (void)x;
  (void)y;
  (void)x2;
  (void)y2;
}

// the most basic function, set a single pixel
void LCD_drawPixel(int x, int y, uint16_t color) {
  if ((x < 0) || (x >= AXOLOTI_CONTROL_LCDWIDTH) || (y < 0) || (y >= AXOLOTI_CONTROL_LCDHEIGHT))
    return;

  // x is which column
  if (color)
    lcd_buffer[AXOLOTI_CONTROL_LCDHEADER + x + (y / 8) * AXOLOTI_CONTROL_LCDWIDTH] |= _BV(y%8);
  else
    lcd_buffer[AXOLOTI_CONTROL_LCDHEADER + x + (y / 8) * AXOLOTI_CONTROL_LCDWIDTH] &= ~_BV(y%8);

  LCD_updateBoundingBox(x, y, x, y);
}

void LCD_setPixel(int x, int y) {
  if ((x < 0) || (x >= AXOLOTI_CONTROL_LCDWIDTH) || (y < 0) || (y >= AXOLOTI_CONTROL_LCDHEIGHT))
    return;
  lcd_buffer[AXOLOTI_CONTROL_LCDHEADER + x + (y / 8) * (AXOLOTI_CONTROL_LCDWIDTH + AXOLOTI_CONTROL_LCDHEADER)] |= _BV(y%8);
  LCD_updateBoundingBox(x, y, x, y);
}

void LCD_clearPixel(int x, int y) {
  if ((x < 0) || (x >= AXOLOTI_CONTROL_LCDWIDTH) || (y < 0) || (y >= AXOLOTI_CONTROL_LCDHEIGHT))
    return;
  lcd_buffer[AXOLOTI_CONTROL_LCDHEADER + x + (y / 8) * (AXOLOTI_CONTROL_LCDWIDTH + AXOLOTI_CONTROL_LCDHEADER)] &= ~_BV(y%8);
  LCD_updateBoundingBox(x, y, x, y);
}

uint8_t LCD_getPixel(int x, int y) {
  if ((x < 0) || (x >= AXOLOTI_CONTROL_LCDWIDTH) || (y < 0) || (y >= AXOLOTI_CONTROL_LCDHEIGHT))
    return 0;

  return (lcd_buffer[AXOLOTI_CONTROL_LCDHEADER + x + (y / 8) * (AXOLOTI_CONTROL_LCDWIDTH + AXOLOTI_CONTROL_LCDHEADER)]
      >> (y % 8)) & 0x1;
}

// clear everything
void LCD_clearDisplay(void) {
  int i; for (i = 0; i < AXOLOTI_CONTROL_LCDROWS; i++)
    memset(&lcd_buffer[AXOLOTI_CONTROL_LCDHEADER + i * AXOLOTI_CONTROL_LCDWIDTH], 0, AXOLOTI_CONTROL_LCDWIDTH);
  LCD_updateBoundingBox(0, 0, AXOLOTI_CONTROL_LCDWIDTH - 1, AXOLOTI_CONTROL_LCDHEIGHT - 1);
//  cursor_y = cursor_x = 0;
}

extern const unsigned char font[];

// draw a character
void LCD_drawChar(int x, int y, unsigned char c) {
  // pixel x, line y
  //
  if ((x < 0) || (x >= (AXOLOTI_CONTROL_LCDWIDTH - 5)) || (y < 0) || (y >= (AXOLOTI_CONTROL_LCDROWS)))
    return;
  int i = c * 5;
  int j = AXOLOTI_CONTROL_LCDHEADER + x + y * (AXOLOTI_CONTROL_LCDWIDTH + AXOLOTI_CONTROL_LCDHEADER);
  lcd_buffer[j++] = font[i++];
  lcd_buffer[j++] = font[i++];
  lcd_buffer[j++] = font[i++];
  lcd_buffer[j++] = font[i++];
  lcd_buffer[j++] = font[i++];
  lcd_buffer[j] = 0;
}

void LCD_drawString(int x, int y, const char *str) {
  if ((y < 0) || (y >= (AXOLOTI_CONTROL_LCDROWS))|| (x<0))return;
  // pixel x, line y
      unsigned char c;
      int j = AXOLOTI_CONTROL_LCDHEADER + x+ y*(AXOLOTI_CONTROL_LCDWIDTH+AXOLOTI_CONTROL_LCDHEADER);
      lcd_buffer[j++] = 0x00;
      int x2 = x;
      while((c=*str++)) {
        if (x2 >= AXOLOTI_CONTROL_LCDWIDTH)
        return;
        x2 += 6;
        int i=c*5;
        lcd_buffer[j++] = font[i++];
        lcd_buffer[j++] = font[i++];
        lcd_buffer[j++] = font[i++];
        lcd_buffer[j++] = font[i++];
        lcd_buffer[j++] = font[i++];
        lcd_buffer[j++] = 0;
      }
    }

void LCD_drawNumber3D(int x, int y, int i) {
  if (i < 0) {
    LCD_drawChar(x, y, '-');
    i = -i;
  }
  else
    LCD_drawChar(x, y, ' ');

  LCD_drawChar(x + 18, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawChar(x + 12, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawChar(x + 6, y, '0' + i);
}

void LCD_drawNumber5D(int x, int y, int i) {
  if (i < 0) {
    LCD_drawChar(x, y, '-');
    i = -i;
  }
  else
    LCD_drawChar(x, y, ' ');

  LCD_drawChar(x + 30, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawChar(x + 24, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawChar(x + 18, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawChar(x + 12, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawChar(x + 6, y, '0' + i);
}

void LCD_drawCharInv(int x, int y, unsigned char c) {
  // pixel x, line y
  //
  if ((x < 0) || (x >= (AXOLOTI_CONTROL_LCDWIDTH - 5)) || (y < 0) || (y >= (AXOLOTI_CONTROL_LCDHEIGHT / 8)))
    return;
  int i = c * 5;
  int j = AXOLOTI_CONTROL_LCDHEADER + x + y * (AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH);
  lcd_buffer[j++] = ~font[i++];
  lcd_buffer[j++] = ~font[i++];
  lcd_buffer[j++] = ~font[i++];
  lcd_buffer[j++] = ~font[i++];
  lcd_buffer[j++] = ~font[i++];
  lcd_buffer[j] = 0xFF;
}

void LCD_drawStringInv(int x, int y, const char *str) {
  if ((y < 0) || (y >= (AXOLOTI_CONTROL_LCDROWS))|| (x<0))return;
  // pixel x, line y
      unsigned char c;
      int j = AXOLOTI_CONTROL_LCDHEADER + x+ y*(AXOLOTI_CONTROL_LCDHEADER+AXOLOTI_CONTROL_LCDWIDTH);
      lcd_buffer[j++] = 0xFF;
      int x2 = x;
      while((c=*str++)) {
        if (x2 >= AXOLOTI_CONTROL_LCDWIDTH)
        return;
        x2 += 6;
        int i=c*5;
        lcd_buffer[j++] = ~font[i++];
        lcd_buffer[j++] = ~font[i++];
        lcd_buffer[j++] = ~font[i++];
        lcd_buffer[j++] = ~font[i++];
        lcd_buffer[j++] = ~font[i++];
        lcd_buffer[j++] = 0xFF;
      }
    }

void LCD_drawNumber3DInv(int x, int y, int i) {
  if (i < 0) {
    LCD_drawCharInv(x, y, '-');
    i = -i;
  }
  else
    LCD_drawCharInv(x, y, ' ');

  LCD_drawCharInv(x + 18, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawCharInv(x + 12, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawCharInv(x + 6, y, '0' + i);
}

void LCD_drawNumber5DInv(int x, int y, int i) {
  if (i < 0) {
    LCD_drawCharInv(x, y, '-');
    i = -i;
  }
  else
    LCD_drawCharInv(x, y, ' ');

  LCD_drawCharInv(x + 30, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawCharInv(x + 24, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawCharInv(x + 18, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawCharInv(x + 12, y, '0' + (i % 10));
  i = i / 10;
  LCD_drawCharInv(x + 6, y, '0' + i);
}

void LCD_drawStringN(int x, int y, const char *str, int N) {
  (void)N;
  if ((y < 0) || (y >= (AXOLOTI_CONTROL_LCDROWS))|| (x<0))return;
  unsigned char c;
  lcd_buffer[AXOLOTI_CONTROL_LCDHEADER + x + y * (AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH)] = 0x00;
  x++;
  while ((c = *str++)) {
    if (x >= (AXOLOTI_CONTROL_LCDWIDTH - 5))
      break;
    int i = c * 5;
    int j = AXOLOTI_CONTROL_LCDHEADER + x + y * (AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH);
    lcd_buffer[j++] = font[i++];
    lcd_buffer[j++] = font[i++];
    lcd_buffer[j++] = font[i++];
    lcd_buffer[j++] = font[i++];
    lcd_buffer[j++] = font[i++];
    lcd_buffer[j] = 0;
    x += 6;
  }
  while (x < AXOLOTI_CONTROL_LCDWIDTH) {
    int j = AXOLOTI_CONTROL_LCDHEADER + x + y * (AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH);
    lcd_buffer[j] = 0;
    x++;
  }
}

void LCD_drawStringInvN(int x, int y, const char *str, int N) {
  (void)N;
  if ((y < 0) || (y >= (AXOLOTI_CONTROL_LCDROWS))|| (x<0))return;
  unsigned char c;
  lcd_buffer[AXOLOTI_CONTROL_LCDHEADER + x + y * (AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH)] = 0xFF;
  x++;
  while ((c = *str++)) {
    if (x >= (AXOLOTI_CONTROL_LCDWIDTH - 5))
      break;
    int i = c * 5;
    int j = AXOLOTI_CONTROL_LCDHEADER + x + y * (AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH);
    lcd_buffer[j++] = ~font[i++];
    lcd_buffer[j++] = ~font[i++];
    lcd_buffer[j++] = ~font[i++];
    lcd_buffer[j++] = ~font[i++];
    lcd_buffer[j++] = ~font[i++];
    lcd_buffer[j] = 0xFF;
    x += 6;
  }
  while (x < AXOLOTI_CONTROL_LCDWIDTH) {
    int j = AXOLOTI_CONTROL_LCDHEADER + x + y * (AXOLOTI_CONTROL_LCDHEADER + AXOLOTI_CONTROL_LCDWIDTH);
    lcd_buffer[j] = 0xFF;
    x++;
  }
}

void LCD_drawIBAR(int x, int y, int v, int N) {
  if ((y < 0) || (y >= (AXOLOTI_CONTROL_LCDHEIGHT / 8)) || (x < 0))
    return;
  int j = AXOLOTI_CONTROL_LCDHEADER + x + (y * AXOLOTI_CONTROL_LCDWIDTH);
  int k = 1;
  int i;
  if (v > 0) {
    for (i = 0; i < 30; i++) {
      x++;
      lcd_buffer[j++] = (v > k) << 1;
      k = k << 1;
    }
  }
  else {
    v = -v;
    for (i = 0; i < 30; i++) {
      x++;
      lcd_buffer[j++] = (v > k) << 6;
      k = k << 1;
    }
  }
  while (x < N) {
    lcd_buffer[j++] = 0;
    x++;
  }
}

void LCD_drawIBARadd(int x, int y, int v) {
  if ((y < 0) || (y >= (AXOLOTI_CONTROL_LCDHEIGHT)) || (x < 0))
    return;
  int j = AXOLOTI_CONTROL_LCDHEADER + x + (y * AXOLOTI_CONTROL_LCDWIDTH);
  int b = 1 << (y & 0x07);
  if (v + x > AXOLOTI_CONTROL_LCDWIDTH) { // clip
    v = AXOLOTI_CONTROL_LCDWIDTH - x;
  }
  int i;
  if (v > 0) {
    for (i = 0; i < v; i++) {
      x++;
      lcd_buffer[j] &= ~b;
      lcd_buffer[j++] += b;
    }
  }
}

