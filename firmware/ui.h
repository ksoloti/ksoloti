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
#ifndef __UI_H
#define __UI_H

#include "parameters.h"

void ui_init(void);

typedef struct {
    int *value;
    int minvalue;
    int maxvalue;
} iValuePair;

typedef struct {
    float value;
    float minvalue;
    float maxvalue;
} fValuePair;

typedef struct {
    void *array; // pointer to KeyValuePair array
    int length;
    int current;
} arrayValuePair;

typedef struct {
    int32_t *value;
} intDbgValuePairBar;

typedef struct {
    int16_t *value;
} sValuePair;

typedef struct {
    int *array; // pointer to array of KeyValuePair pointers
    int length;
    int current;
} arrayPtrValuePair;

typedef void (*DisplayFunction)(int);
typedef void (*ButtonFunction)(int, int);

typedef void (*VoidFunction)(void);

typedef struct {
    DisplayFunction displayFunction; // function pointer
    ButtonFunction buttonFunction;
    void * userdata;
} customUIFunctions;

typedef struct {
    int *value;
} intDisplayValue;

typedef struct {
    int *value;
} pitchDisplayValue;

typedef struct {
    int *value;
} freqDisplayValue;

typedef struct {
    int *value;
} fractDisplayValue;

typedef struct {
    ParameterExchange_t *PEx;
    int minvalue;
    int maxvalue;
} ipValuePair;

typedef struct {
    uint8_t *value;
    int minvalue;
    int maxvalue;
} u7ValuePair;

typedef struct {
    VoidFunction fnctn;
} fnctnValuePair;

typedef enum {
    KVP_TYPE_IVP,
    KVP_TYPE_FVP,
    KVP_TYPE_SVP,
    KVP_TYPE_AVP,
    KVP_TYPE_IDVP,
    KVP_TYPE_APVP,
    KVP_TYPE_CUSTOM,
    KVP_TYPE_INTDISPLAY,
    KVP_TYPE_PITCHDISPLAY,
    KVP_TYPE_FREQDISPLAY,
    KVP_TYPE_FRACTDISPLAY,
    KVP_TYPE_IPVP,
    KVP_TYPE_U7VP,
    KVP_TYPE_FNCTN,
} KVP_type;

void k_scope_DisplayFunction(void * userdata);
void k_scope_DisplayFunction2(void * userdata);
void k_scope_DisplayFunction3(void * userdata);
void k_scope_DisplayFunction4(void * userdata);

extern void (*pControlUpdate)(void);

#endif
