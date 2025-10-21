/*
 * Copyright (C)2013, 2014 Johannes Taelman
 * Edited 2023 - 2025 by Ksoloti
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option)any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef _AXOLOTI_MATH_H
#define _AXOLOTI_MATH_H

#include "ch.h"
#include "hal.h"

#include <stdint.h>
#include "math.h"

//#pragma GCC diagnostic push
//#pragma GCC diagnostic ignored "-Wstrict-aliasing"
//#define __GNUC__ TRUE
#include "arm_math.h"
//#undef __CORE_CM4_SIMD_H
//#include "core_cm4_simd.h"
//#pragma GCC diagnostic pop

// #define SINETSIZE 1024
// extern int16_t sinet[SINETSIZE + 1];

#define SINE2TSIZE 4096
extern int32_t sine2t[SINE2TSIZE + 1];

#define WINDOWSIZE 1024
extern int16_t windowt[WINDOWSIZE + 1];

#define PITCHTSIZE 257
extern uint32_t pitcht[PITCHTSIZE];

#define EXPTSIZE 256
extern uint16_t expt[EXPTSIZE];

#define LOGTSIZE 256
#define LOGTSIZEN 8
extern uint16_t logt[LOGTSIZE];

typedef union {
    int32_t i;
    float f;
    struct {
        uint32_t mantissa :23;
        uint32_t exponent :8;
        uint32_t sign :1;
    } parts;
} Float_t;

void axoloti_math_init(void);

/* [Rotate Right]
   Performs a bitwise Rotate Right operation on op1 by op2 positions.
   Bits shifted out from the right end re-enter on the left end. */
__attribute__((always_inline)) __STATIC_INLINE int32_t ___ROR (int32_t op1, int32_t op2) {
    int32_t result;
    __ASM volatile ("ror %0, %1, %2" : "=r" (result): "r" (op1), "r" (op2));
    return result;
}

/* [Signed Most Significant Word Multiply]
   Performs a 32-bit by 32-bit signed multiplication and returns
   the most significant 32 bits of the 64-bit result.
   (op1 * op2) >> 32 */
__attribute__((always_inline)) __STATIC_INLINE int32_t ___SMMUL (int32_t op1, int32_t op2) {
    int32_t result;
    __ASM volatile ("smmul %0, %1, %2" : "=r" (result): "r" (op1), "r" (op2));
    return result;
}

/* [Signed Most Significant Word Multiply Accumulate]
   Performs a 32-bit by 32-bit signed multiplication (op1 * op2),
   takes the most significant 32 bits of the result,
   and then adds it to op3 (the accumulator).
   op3 + ((op1 * op2) >> 32) */
__attribute__((always_inline)) __STATIC_INLINE int32_t ___SMMLA (int32_t op1, int32_t op2, int32_t op3) {
    int32_t result;
    __ASM volatile ("smmla %0, %1, %2, %3" : "=r" (result): "r" (op1), "r" (op2), "r" (op3));
    return result;
}

/* [Signed Most Significant Word Multiply Subtract]
   Performs a 32-bit by 32-bit signed multiplication (op1 * op2),
   takes the most significant 32 bits of the result,
   and then subtracts it from op3 (the accumulator).
   op3 - ((op1 * op2) >> 32) */
__attribute__((always_inline)) __STATIC_INLINE int32_t ___SMMLS (int32_t op1, int32_t op2, int32_t op3) {
    int32_t result;
    __ASM volatile ("smmls %0, %1, %2, %3" : "=r" (result): "r" (op1), "r" (op2), "r" (op3));
    return result;
}

/* [Signed Divide]
   Performs a 32-bit signed integer division of op1 by op2.
   op1 / op2 */
__attribute__((always_inline)) __STATIC_INLINE int32_t ___SDIV (int32_t op1, int32_t op2) {
    int32_t result;
    __ASM volatile ("sdiv %0, %1, %2" : "=r" (result): "r" (op1), "r" (op2));
    return result;
}

/* [Unsigned Divide]
   Performs a 32-bit unsigned integer division of op1 by op2.
   op1 / op2 */
__attribute__((always_inline)) __STATIC_INLINE uint32_t ___UDIV (uint32_t op1, uint32_t op2) {
    uint32_t result;
    __ASM volatile ("udiv %0, %1, %2" : "=r" (result): "r" (op1), "r" (op2));
    return result;
}


/* ALL FLOATING-POINT OPERATIONS ARE SINGLE-PRECISION */

/* [Floating-point Multiply]
   Performs a floating-point multiplication of op1 by op2.
   op1 / op2 */
__attribute__((always_inline)) __STATIC_INLINE float ___VMULF(float op1, float op2){
    float result;
    __ASM volatile ("vmul.f32 %0, %1, %2" : "=w" (result): "w" (op1), "w" (op2));
    return result;
}

/* [Floating-point Divide]
   Performs a floating-point division of op1 by op2.
   op1 / op2 */
__attribute__((always_inline)) __STATIC_INLINE float ___VDIVF(float op1, float op2){
    float result;
    __ASM volatile ("vdiv.f32 %0, %1, %2" : "=w" (result): "w" (op1), "w" (op2));
    return result;
}

/* [Floating-point Fused Multiply Add]
   Performs a fused multiply-add operation.
   "Fused" means that multiplication and addition are performed
   with only one rounding step for the entire operation (for speed).
   (op1 * op2) + op3 */
__attribute__((always_inline)) __STATIC_INLINE float ___VMLAF(float op1, float op2, float op3)
{
    /* Note: The order of operands in ASM for VMLA is Accumulator, Multiplicand1, Multiplicand2 */
    float result = op3; // Initialize with the accumulator
    __ASM volatile("vmla.f32 %0, %1, %2" : "+w"(result) : "w"(op1), "w"(op2));
    return (result);
}

/* [Floating-point Fused Multiply Subtract]
   Performs a fused multiply-subtract operation.
   "Fused" means that multiplication and subtraction are performed
   with only one rounding step for the entire operation (for speed).
   op3 - (op1 * op2) */
__attribute__((always_inline)) __STATIC_INLINE float ___VMLSF(float op1, float op2, float op3)
{
    /* Note: The order of operands in ASM for VMLS is Accumulator, Multiplicand1, Multiplicand2 */
    float result = op3; // Initialize with the accumulator
    __ASM volatile("vmls.f32 %0, %1, %2" : "+w"(result) : "w"(op1), "w"(op2));
    return (result);
}

/* [Floating-point Square Root]
   Calculates the square root of a floating-point number.
   Note: Standard sqrtf() typically compiles to this. */
__attribute__((always_inline)) __STATIC_INLINE float ___VSQRTF(float op1){
    float result;
    __ASM volatile ("vsqrt.f32 %0, %1" : "=w" (result): "w" (op1));
    return result;
}
#define _VSQRTF ___VSQRTF

/* MIDI Note to Frequency (assumes 48 kHz sample rate, output is Q31 fixed-point) */
__attribute__((always_inline)) __STATIC_INLINE uint32_t mtof48k_q31(int32_t pitch) {
    int32_t p = __SSAT(pitch, 28);
    uint32_t pi = p >> 21;
    int32_t y1 = pitcht[128+pi];
    int32_t y2 = pitcht[128+1+pi];
    int32_t pf = (p & 0x1FFFFF) << 10;
    int32_t pfc = INT32_MAX - pf;
    uint32_t r;
    r = ___SMMUL(y1, pfc);
    r = ___SMMLA(y2, pf, r);
    return r << 1; /* is frequency */
}

/* MIDI Note to Frequency, Extended Range (assumes 48 kHz sample rate, output is Q31 fixed-point) */
__attribute__((always_inline)) __STATIC_INLINE uint32_t mtof48k_ext_q31(int32_t pitch) {
    int32_t p = __SSAT(pitch, 29);
    uint32_t pi = p >> 21;
    int32_t y1 = pitcht[128+pi];
    int32_t y2 = pitcht[128+1+pi];
    int32_t pf = (p & 0X1FFFFF) << 10;
    int32_t pfc = INT32_MAX - pf;
    uint32_t r;
    r = ___SMMUL(y1, pfc);
    r = ___SMMLA(y2, pf, r);
    return r << 1; /* is frequency */
}

/* Sine Function (input and output Q31 fixed-point) */
__attribute__((always_inline)) __STATIC_INLINE int32_t sin_q31(int32_t phase) {
    uint32_t p = (uint32_t) (phase);
    uint32_t pi = p >> 20;
    int32_t y1 = sine2t[pi];
    int32_t y2 = sine2t[1+pi];
    int32_t pf= (p & 0xfffff) << 11;
    int32_t pfc = INT32_MAX - pf;
    int32_t rr;
    rr = ___SMMUL(y1, pfc);
    rr = ___SMMLA(y2, pf, rr);
    return rr << 1;
}

/* Hann Window Function (input and output Q31 fixed-point) */
__attribute__((always_inline)) __STATIC_INLINE uint32_t hann_q31(int32_t phase) {
    uint32_t p = phase;
    uint32_t pi = p >> 22;
    int32_t y1 = windowt[pi];
    int32_t y2 = windowt[1+pi];
    int32_t pf = (p & 0x3fffff) << 9;
    int32_t pfc = INT32_MAX - pf;
    int32_t rr;
    rr = ___SMMUL(y1<<16, pfc);
    rr = ___SMMLA(y2<<16, pf, rr);
    return rr << 1;
}


/* Purposefully doing some "literal" float to integer conversion here */
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wstrict-aliasing"

/* [Q27 fixed-point to floating point conversion]
   Converts a 32-bit signed integer in Q27 fixed-point format (aka "blue wire")
   to a floating-point number. */
__attribute__((always_inline)) __STATIC_INLINE float q27_to_float(int32_t op1) {
    float fop1 = *(float*) (&op1);
    __ASM volatile ("VCVT.F32.S32 %0, %0, 27" : "+w" (fop1));
    return fop1;
}

/* [Floating point to Q27 fixed-point (aka "blue wire") conversion]
   Converts a floating-point number to a 32-bit signed integer
   in Q27 fixed-point format (aka "blue wire"). */
__attribute__((always_inline)) __STATIC_INLINE int32_t float_to_q27(float fop1) {
    __ASM volatile ("VCVT.S32.F32 %0, %0, 27" : "+w" (fop1));
    int32_t r = *(int32_t*) (&fop1);
    return r;
}

#pragma GCC diagnostic pop

__attribute__((always_inline)) __STATIC_INLINE int32_t ConvertIntToFrac(int i) {
    return i << 21;
}
#define int_to_frac ConvertIntToFrac

__attribute__((always_inline)) __STATIC_INLINE int32_t ConvertFracToInt(int i) {
    return i >> 21;
}
#define frac_to_int ConvertFracToInt

__attribute__((always_inline)) __STATIC_INLINE int32_t ConvertFloatToFrac(float f) {
    return (int32_t) (f * (1 << 21) );
}
#define float_to_frac ConvertFloatToFrac

/* Generates a random signed 32-bit integer value (full range: -2^31 to 2^31-1) */
__attribute__((always_inline)) __STATIC_INLINE int32_t rand_s32(void) {
    /* This function differs from the standard C rand()definition, standard C
     * rand() only returns positive numbers, while rand_s32() returns the full
     * signed 32 bit range.
     * The hardware random generator can't provide new data as quick as desireable
     * but rather than waiting for a new true random number,
     * we multiply/add the seed with the latest hardware-generated number.
     */
    static uint32_t randSeed = 22229; /* Static declaration inside function -> will retain its last value between calls */
    return randSeed = (randSeed * 196314163) + RNG->DR;
}

/* If RAND_MAX was perviously defined, satisfy compiler by undefining it */
#ifdef RAND_MAX
#undef RAND_MAX
#endif
#define RAND_MAX INT32_MAX

/* Generates a random unsigned 31-bit integer value (0 to 2^31-1) */
__attribute__((always_inline)) __STATIC_INLINE int32_t rand_u32(void) {
    /* like standard C rand() */
    return ((uint32_t) rand_s32()) >> 1;
}

// #define rand rand_u32 // TODO: effect?

uint32_t FastLog(uint32_t f);

// deprecated macros
#define MTOF(pitch, frequency) frequency = mtof48k_q31(pitch);

#define MTOFEXTENDED(pitch, frequency) frequency = mtof48k_ext_q31(pitch);
#define MTOF_EXTENDED MTOFEXTENDED

#define SINE2TINTERP(phase, output) output = sin_q31(phase);
#define SINE2T_INTERP SINE2TINTERP

#define HANNING2TINTERP(phase, output) output = hann_q31(phase);
#define HANNING2T_INTERP HANNING2TINTERP

/* Deprecated functions */
__attribute__((always_inline)) __STATIC_INLINE uint32_t GenerateRandomNumber(void) {
    return rand_s32();
}

#endif