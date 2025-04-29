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

#include "axoloti_defines.h"
#include "ui.h"
#include "ch.h"
#include "hal.h"
#include "midi.h"
#include "axoloti_math.h"
#include "patch.h"
#include "sdcard.h"
#include "pconnection.h"
#include "axoloti_board.h"
#include "ff.h"
#include <string.h>
#include "analyser.h"


#if USE_KVP

// Btn_Nav_States_struct Btn_Nav_CurStates;
// Btn_Nav_States_struct Btn_Nav_PrevStates;
// Btn_Nav_States_struct Btn_Nav_Or;
// Btn_Nav_States_struct Btn_Nav_And;

// int8_t EncBuffer[4];

// struct KeyValuePair KvpsHead;
// struct KeyValuePair *KvpsDisplay;
struct KeyValuePair *ObjectKvpRoot; // TODOH7 we really need to sort this crap out!
#define MAXOBJECTS 256
struct KeyValuePair *ObjectKvps[MAXOBJECTS] KVP_DATA_SECTION;
// #define MAXTMPMENUITEMS 15
// KeyValuePair_s TmpMenuKvps[MAXTMPMENUITEMS];
// KeyValuePair_s ADCkvps[3];

//const char stat = 2;

void SetKVP_APVP(KeyValuePair_s *kvp, KeyValuePair_s *parent, int length, KeyValuePair_s *array) {
    kvp->kvptype = KVP_TYPE_APVP;
    kvp->parent = (void *)parent;
    kvp->apvp.length = length;
    kvp->apvp.current = 0;
    kvp->apvp.array = (void *)array;
}

void SetKVP_AVP(KeyValuePair_s *kvp, KeyValuePair_s *parent, int length, KeyValuePair_s *array) {
    kvp->kvptype = KVP_TYPE_AVP;
    kvp->parent = (void *)parent;
    kvp->avp.length = length;
    kvp->avp.current = 0;
    kvp->avp.array = array;
}

void SetKVP_IVP(KeyValuePair_s *kvp, KeyValuePair_s *parent, int *value, int min, int max) {
    kvp->kvptype = KVP_TYPE_IVP;
    kvp->parent = (void *)parent;
    kvp->ivp.value = value;
    kvp->ivp.minvalue = min;
    kvp->ivp.maxvalue = max;
}

void SetKVP_IPVP(KeyValuePair_s *kvp, KeyValuePair_s *parent, ParameterExchange_t *PEx, int min, int max) {
    PEx->signals = 0x0F;
    kvp->kvptype = KVP_TYPE_IPVP;
    kvp->parent = (void *)parent;
    kvp->ipvp.PEx = PEx;
    kvp->ipvp.minvalue = min;
    kvp->ipvp.maxvalue = max;
}

void SetKVP_FNCTN(KeyValuePair_s *kvp, KeyValuePair_s *parent, VoidFunction fnctn) {
    kvp->kvptype = KVP_TYPE_FNCTN;
    kvp->parent = (void *)parent;
    kvp->fnctnvp.fnctn = fnctn;
}

inline void KVP_Increment(KeyValuePair_s *kvp) {
    switch (kvp->kvptype) {
        case KVP_TYPE_IVP:
            if (*kvp->ivp.value < kvp->ivp.maxvalue) {
                (*kvp->ivp.value)++;
            }
        break;
        case KVP_TYPE_AVP:
            if (kvp->avp.current < (kvp->avp.length - 1)) {
                kvp->avp.current++;
            }
        break;
        case KVP_TYPE_APVP:
            if (kvp->apvp.current < (kvp->apvp.length - 1)) {
                kvp->apvp.current++;
            }
        break;
        case KVP_TYPE_U7VP:
            if (*kvp->u7vp.value < kvp->u7vp.maxvalue) {
                (*kvp->u7vp.value) += 1;
            }
        break;
        case KVP_TYPE_IPVP: {
            int32_t nval = kvp->ipvp.PEx->value + (1 << 20);
            if (nval < kvp->ipvp.maxvalue) {
                PExParameterChange(kvp->ipvp.PEx, nval, 0xFFFFFFE7);
            }
            else {
                PExParameterChange(kvp->ipvp.PEx, kvp->ipvp.maxvalue, 0xFFFFFFE7);
            }
        }
        break;
        default:
        break;
    }
}

inline void KVP_Decrement(KeyValuePair_s *kvp) {
    switch (kvp->kvptype) {
        case KVP_TYPE_IVP:
            if (*kvp->ivp.value > kvp->ivp.minvalue) {
                (*kvp->ivp.value)--;
            }
        break;
        case KVP_TYPE_AVP:
        if (kvp->avp.current > 0) {
            kvp->avp.current--;
        }
        break;
        case KVP_TYPE_APVP:
        if (kvp->apvp.current > 0) {
            kvp->apvp.current--;
        }
        break;
        case KVP_TYPE_U7VP:
        if (*kvp->u7vp.value > kvp->u7vp.minvalue) {
            (*kvp->u7vp.value)--;
        }
        break;
        case KVP_TYPE_IPVP: {
            int32_t nval = kvp->ipvp.PEx->value - (1 << 20);
            if (nval > kvp->ipvp.minvalue) {
                PExParameterChange(kvp->ipvp.PEx, nval, 0xFFFFFFE7);
            }
            else {
                PExParameterChange(kvp->ipvp.PEx, kvp->ipvp.minvalue, 0xFFFFFFE7);
            }
        }
        break;
        default:
        break;
    }
}

void KVP_ClearObjects(void) {
    ObjectKvpRoot->apvp.length = 0;
    // KvpsDisplay = &KvpsHead;
}

void KVP_RegisterObject(KeyValuePair_s *kvp) {
    if(ObjectKvpRoot->apvp.length < (MAXOBJECTS) )
    {
        ObjectKvps[ObjectKvpRoot->apvp.length] = kvp;
        // kvp->parent = ObjectKvpRoot;
        ObjectKvpRoot->apvp.length++;
    }
}
#endif


static WORKING_AREA(waThreadUI, 1172);
    static msg_t ThreadUI(void *arg) {
    (void)(arg);
#if CH_CFG_USE_REGISTRY
    chRegSetThreadName("ui");
#endif
    while (1) {
        AnalyserSetChannel(acPconnection, true);
        PExTransmit();
        AnalyserSetChannel(acPconnection, false);
        AnalyserSetChannel(acPconnection, true);
        PExReceive();
        AnalyserSetChannel(acPconnection, false);
        chThdSleepMilliseconds(2);
    }
    return (msg_t)0;
}


void ui_init(void) {
#if USE_KVP    
    KeyValuePair_s *p = chCoreAlloc(sizeof(KeyValuePair_s) * 6);
    ObjectKvpRoot = &p[0];
#endif

    chThdCreateStatic(waThreadUI, sizeof(waThreadUI), UI_USB_PRIO, (void*) ThreadUI, NULL);
}

// #define LCD_COL_INDENT 5
// #define LCD_COL_EQ 91
// #define LCD_COL_VAL 97
// #define LCD_COL_ENTER 97
// #define STATUSROW 7

