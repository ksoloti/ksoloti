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
#include "axoloti_board.h"
#include "mcuconf.h"
#include "midi.h"
#include "serial_midi.h"
#include "patch.h"

static const int8_t StatusLengthLookup[16] = {
    0, 0, 0, 0, 0, 0, 0, 0,
    3,  /* 0x80 = note off, 3 bytes */
    3,  /* 0x90 = note on, 3 bytes */
    3,  /* 0xA0 = poly pressure, 3 bytes */
    3,  /* 0xB0 = control change, 3 bytes */
    2,  /* 0xC0 = program change, 2 bytes */
    2,  /* 0xD0 = channel pressure, 2 bytes */
    3,  /* 0xE0 = pitch bend, 3 bytes */
    -1  /* 0xF0 = other things. may vary */
};

static const int8_t SysMsgLengthLookup[16] = {
    -1, /* 0xF0 = sysex start, may vary */
    2,  /* 0xF1 = MIDI Time Code, 2 bytes */
    3,  /* 0xF2 = MIDI Song position, 3 bytes */
    2,  /* 0xF3 = MIDI Song Select, 2 bytes. */
    1,  /* 0xF4 = undefined */
    1,  /* 0xF5 = undefined */
    1,  /* 0xF6 = TUNE Request */
    -1, /* 0xF7 = sysex end */
    1,  /* 0xF8 = timing clock 1 byte */
    1,  /* 0xF9 = proposed measure end? */
    1,  /* 0xFA = start 1 byte */
    1,  /* 0xFB = continue 1 byte */
    1,  /* 0xFC = stop 1 byte */
    1,  /* 0xFD = undefined */
    1,  /* 0xFE = active sensing 1 byte */
    3   /* 0xFF = not reset, but a META-EVENT, which is always 3 bytes */
};

static const SerialConfig sdMidiCfg = {
    31250, /* baud */
    0, 0, 0
};

uint8_t MidiByte0, MidiByte1, MidiByte2;
uint8_t MidiCurData;
uint8_t MidiNumData;
uint8_t MidiInChannel;

static WORKING_AREA(waThreadMidi, 256) FAST_DATA_SECTION;


/* MIDI IN */

void serial_MidiInByteHandler(uint8_t data) {
    int8_t len;

    if (data & 0x80) {
        len = StatusLengthLookup[data >> 4];

        if (len == -1) {
            len = SysMsgLengthLookup[data & 0x0F];

            if (len == 1) {
                MidiInMsgHandler(MIDI_DEVICE_DIN, 1, data, 0, 0);
            }
            else {
                MidiByte0 = data;
                MidiNumData = len - 1;
                MidiCurData = 0;
            }
        }
        else {
            MidiByte0 = data;
            MidiNumData = len - 1;
            MidiCurData = 0;
        }
    }
    else {
        /* not a status byte */

        if (MidiCurData == 0) {
            MidiByte1 = data;

            if (MidiNumData == 1) {
                /* 2 byte message complete */
                MidiInMsgHandler(MIDI_DEVICE_DIN, 1, MidiByte0, MidiByte1, 0);
                MidiCurData = 0;
            }
            else {
                MidiCurData++;
            }
        }
        else if (MidiCurData == 1) {
            MidiByte2 = data;

            if (MidiNumData == 2) {
                MidiInMsgHandler(MIDI_DEVICE_DIN, 1, MidiByte0, MidiByte1, MidiByte2);
                MidiCurData = 0;
            }
        }
    }
}


/* MIDI OUT */

void serial_MidiSend1(uint8_t b0) {
    sdPut(&SDMIDI, b0);
}


void serial_MidiSend2(uint8_t b0, uint8_t b1) {
    uint8_t tx[2];
    tx[0] = b0;
    tx[1] = b1;
    sdWrite(&SDMIDI, tx, 2);
}


void serial_MidiSend3(uint8_t b0, uint8_t b1, uint8_t b2) {
    uint8_t tx[3];
    tx[0] = b0;
    tx[1] = b1;
    tx[2] = b2;
    sdWrite(&SDMIDI, tx, 3);
}


int32_t serial_MidiGetOutputBufferPending(void) {
  return oqGetFullI(&SDMIDI.oqueue);
}


/* MIDI UART */

__attribute__((noreturn)) static msg_t ThreadMidi(void *arg) {
    (void)arg;
#if CH_CFG_USE_REGISTRY
    chRegSetThreadName("midi");
#endif
    while (1) {
        char ch;
        ch = sdGet(&SDMIDI);
        serial_MidiInByteHandler(ch);
    }
}


void serial_midi_init(void) {
    /*
     * Activates the serial driver 6 using the driver default configuration.
     */

    palSetPadMode(GPIOG,  9, PAL_MODE_ALTERNATE(8) | PAL_MODE_INPUT_PULLUP); /* RX */
    palSetPadMode(GPIOG, 14, PAL_MODE_ALTERNATE(8) | PAL_MODE_INPUT_PULLUP | PAL_STM32_OTYPE_PUSHPULL); /* TX */ //TODO does adding PAL_MODE_OUTPUT_PUSHPULL or PAL_STM32_OTYPE_PUSHPULL work?

    sdStart(&SDMIDI, &sdMidiCfg);

    chThdCreateStatic(waThreadMidi, sizeof(waThreadMidi), SERIAL_MIDI_PRIO, (void*) ThreadMidi, NULL);
}
