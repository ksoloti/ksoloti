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

#include "codec.h"

#include "axoloti_defines.h"
#ifdef FW_SPILINK
#include "ch.h"
#include "spilink.h"
#endif
#ifdef FW_I2SCODEC
#include "i2scodec.h"
#endif
#include "codec_ADAU1961.h"

#if BOARD_KSOLOTI_CORE_H743
    int32_t buf[DOUBLE_BUFSIZE]   __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)));
    int32_t buf2[DOUBLE_BUFSIZE]  __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)));
    int32_t rbuf[DOUBLE_BUFSIZE]  __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)));
    int32_t rbuf2[DOUBLE_BUFSIZE] __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)));

    #ifdef FW_I2SCODEC
        int32_t i2s_buf[DOUBLE_BUFSIZE]   __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)));
        int32_t i2s_buf2[DOUBLE_BUFSIZE]  __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)));
        int32_t i2s_rbuf[DOUBLE_BUFSIZE]  __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)));
        int32_t i2s_rbuf2[DOUBLE_BUFSIZE] __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)));
    #endif
#else
    int32_t buf[DOUBLE_BUFSIZE]   __attribute__ ((section (".sram2")));
    int32_t buf2[DOUBLE_BUFSIZE]  __attribute__ ((section (".sram2")));
    int32_t rbuf[DOUBLE_BUFSIZE]  __attribute__ ((section (".sram2")));
    int32_t rbuf2[DOUBLE_BUFSIZE] __attribute__ ((section (".sram2")));


    #ifdef FW_I2SCODEC
        int32_t i2s_buf[DOUBLE_BUFSIZE]   __attribute__ ((section (".sram2")));
        int32_t i2s_buf2[DOUBLE_BUFSIZE]  __attribute__ ((section (".sram2")));
        int32_t i2s_rbuf[DOUBLE_BUFSIZE]  __attribute__ ((section (".sram2")));
        int32_t i2s_rbuf2[DOUBLE_BUFSIZE] __attribute__ ((section (".sram2")));
    #endif
#endif

void codec_init(bool_t isMaster) {
    // volatile extern uint32_t __ram3_clear__;
    // volatile uint32_t u = __ram3_clear__;
    codec_ADAU1961_SAI_init(SAMPLERATE, isMaster);
}


void codec_clearbuffer(void) {
    int i; for(i=0; i<DOUBLE_BUFSIZE; i++) {
        buf[i] = 0;
        buf2[i] = 0;
#ifdef FW_I2SCODEC
        i2s_buf[i] = 0;
        i2s_buf2[i] = 0;
#endif
    }


#ifdef FW_SPILINK
    spilink_clear_audio_tx();
#endif

}

#include "codec_ADAU1961_SAI.c"
