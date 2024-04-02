/**
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

#include "codec.h"

#include "axoloti_defines.h"
#ifdef FW_SPILINK
#include "ch.h"
#include "spilink.h"
#endif
#include "codec_ADAU1961.h"


int32_t buf[BUFSIZE*2]   __attribute__ ((section (".sram2")));
int32_t buf2[BUFSIZE*2]  __attribute__ ((section (".sram2")));
int32_t rbuf[BUFSIZE*2]  __attribute__ ((section (".sram2")));
int32_t rbuf2[BUFSIZE*2] __attribute__ ((section (".sram2")));

void codec_init(bool_t isMaster) {
    codec_ADAU1961_SAI_init(SAMPLERATE, isMaster);
}


void codec_clearbuffer(void) {
    int i; for(i=0; i<BUFSIZE*2; i++) {
        buf[i] = 0;
        buf2[i] = 0;
    }

#ifdef FW_SPILINK
    spilink_clear_audio_tx();
#endif

}

#include "codec_ADAU1961_SAI.c"
