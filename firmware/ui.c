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

// Btn_Nav_States_struct Btn_Nav_CurStates;
// Btn_Nav_States_struct Btn_Nav_PrevStates;
// Btn_Nav_States_struct Btn_Nav_Or;
// Btn_Nav_States_struct Btn_Nav_And;

// int8_t EncBuffer[4];

// struct KeyValuePair KvpsHead;
// struct KeyValuePair *KvpsDisplay;
struct KeyValuePair *ObjectKvpRoot;
#define MAXOBJECTS 256
struct KeyValuePair *ObjectKvps[MAXOBJECTS];
// #define MAXTMPMENUITEMS 15
// KeyValuePair_s TmpMenuKvps[MAXTMPMENUITEMS];
// KeyValuePair_s ADCkvps[3];

//const char stat = 2;

void SetKVP_APVP(KeyValuePair_s *kvp, KeyValuePair_s *parent,
                 const char *keyName, int length, KeyValuePair_s *array) {
    kvp->kvptype = KVP_TYPE_APVP;
    kvp->parent = (void *)parent;
    kvp->keyname = keyName;
    kvp->apvp.length = length;
    kvp->apvp.current = 0;
    kvp->apvp.array = (void *)array;
}

void SetKVP_AVP(KeyValuePair_s *kvp, KeyValuePair_s *parent,
                const char *keyName, int length, KeyValuePair_s *array) {
    kvp->kvptype = KVP_TYPE_AVP;
    kvp->parent = (void *)parent;
    kvp->keyname = keyName;
    kvp->avp.length = length;
    kvp->avp.current = 0;
    kvp->avp.array = array;
}

void SetKVP_IVP(KeyValuePair_s *kvp, KeyValuePair_s *parent,
                const char *keyName, int *value, int min, int max) {
    kvp->kvptype = KVP_TYPE_IVP;
    kvp->parent = (void *)parent;
    kvp->keyname = keyName;
    kvp->ivp.value = value;
    kvp->ivp.minvalue = min;
    kvp->ivp.maxvalue = max;
}

void SetKVP_IPVP(KeyValuePair_s *kvp, KeyValuePair_s *parent,
                 const char *keyName, ParameterExchange_t *PEx, int min,
                 int max) {
    PEx->signals = 0x0F;
    kvp->kvptype = KVP_TYPE_IPVP;
    kvp->parent = (void *)parent;
    kvp->keyname = keyName;
    kvp->ipvp.PEx = PEx;
    kvp->ipvp.minvalue = min;
    kvp->ipvp.maxvalue = max;
}

void SetKVP_FNCTN(KeyValuePair_s *kvp, KeyValuePair_s *parent,
                  const char *keyName, VoidFunction fnctn) {
    kvp->kvptype = KVP_TYPE_FNCTN;
    kvp->parent = (void *)parent;
    kvp->keyname = keyName;
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

#if 0
#define POLLENC(NAME, INCREMENT_FUNCTION, DECREMENT_FUNCTION)  \
if (!expander_PrevStates.NAME##A) {                 \
  if (!expander_PrevStates.NAME##B) {             \
      if (expander_CurStates.NAME##B) {           \
          expander_PrevStates.NAME##B = 1;        \
          DECREMENT_FUNCTION                      \
      } else if (expander_CurStates.NAME##A) {    \
          expander_PrevStates.NAME##A = 1;        \
          INCREMENT_FUNCTION                      \
      }                                           \
  } else {                                        \
      if (expander_CurStates.NAME##A) {           \
          expander_PrevStates.NAME##A = 1;        \
      } else if (!expander_CurStates.NAME##B) {   \
          expander_PrevStates.NAME##B = 0;        \
      }                                           \
  }                                               \
} else {                                            \
  if (expander_PrevStates.NAME##B) {              \
      if (!expander_CurStates.NAME##B) {          \
          expander_PrevStates.NAME##B = 0;        \
      } else if (!expander_CurStates.NAME##A) {   \
          expander_PrevStates.NAME##A = 0;        \
      }                                           \
  } else {                                        \
      if (!expander_CurStates.NAME##A) {          \
          expander_PrevStates.NAME##A = 0;        \
      } else if (expander_CurStates.NAME##B) {    \
          expander_PrevStates.NAME##B = 1;        \
      }                                           \
  }                                               \
}
#endif

#if 0
/*
* Create menu tree from file tree
*/

uint8_t *memp;
KeyValuePair_s LoadMenu;

void EnterMenuLoadFile(void) {
    KeyValuePair_s *F = &((KeyValuePair_s *)(LoadMenu.avp.array))[LoadMenu.avp.current];

    char str[20] = "0:";
    strcat(str, F->keyname);

    LoadPatch(str);
}

void EnterMenuLoad(void) {
    memp = (uint8_t *)&fbuff[0];
    FRESULT res;
    FILINFO fno;
    DIR dir;
    int index = 0;
    char *fn;
#if _USE_LFN
    fno.lfname = 0;
    fno.lfsize = 0;
#endif
    res = f_opendir(&dir, "");
    if (res == FR_OK) {
        for (;;) {
            res = f_readdir(&dir, &fno);
            if (res != FR_OK || fno.fname[0] == 0) break;
            if (fno.fname[0] == '.') continue;
            fn = fno.fname;
            if (fno.fattrib & AM_DIR) {
                // ignore subdirectories for now
            }
            else {
                int l = strlen(fn);
                if ((fn[l - 4] == '.') && (fn[l - 3] == 'B') && (fn[l - 2] == 'I') && (fn[l - 1] == 'N')) {
                    char *s;
                    s = (char *)memp;
                    strcpy(s, fn);
                    memp += l + 1;
                    // SetKVP_FNCTN(&TmpMenuKvps[index], NULL, s, &EnterMenuLoadFile);
                    index++;
                }
            }
        }
        // SetKVP_AVP(&LoadMenu, &KvpsHead, "Load SD", index, &TmpMenuKvps[0]);
        // KvpsDisplay = &LoadMenu;
    }
    // TBC: error messaging
}

void EnterMenuFormat(void) {
    FRESULT err;
    err = f_mkfs(0, 0, 0);
    if (err != FR_OK) {
        // SetKVP_AVP(&TmpMenuKvps[0], &KvpsHead, "Format failed", 0, 0);
        // KvpsDisplay = &TmpMenuKvps[0];
    }
    else {
        // SetKVP_AVP(&TmpMenuKvps[0], &KvpsHead, "Format OK", 0, 0);
        // KvpsDisplay = &TmpMenuKvps[0];
    }
}
#endif


static WORKING_AREA(waThreadUI, 1172);
    static msg_t ThreadUI(void *arg) {
    (void)(arg);
#if CH_USE_REGISTRY
    chRegSetThreadName("ui");
#endif
    while (1) {
        PExTransmit();
        PExReceive();
        chThdSleepMilliseconds(2);
    }
    return (msg_t)0;
}


// void UIGoSafe(void) {
    // KvpsDisplay = &KvpsHead;
// }

void ui_init(void) {
#if 0
    Btn_Nav_Or.word = 0;
    Btn_Nav_And.word = ~0;

    KeyValuePair_s *p1 = chCoreAlloc(sizeof(KeyValuePair_s) * 6);
    KeyValuePair_s *q1 = p1;
    SetKVP_FNCTN(q1++, &KvpsHead, "Info", 0);
    SetKVP_FNCTN(q1++, &KvpsHead, "Format", &EnterMenuFormat);

    KeyValuePair_s *p = chCoreAlloc(sizeof(KeyValuePair_s) * 6);
    // KeyValuePair *q = p;
    int entries = 0;

    SetKVP_APVP(&p[entries++], &KvpsHead, "Patch", 0, &ObjectKvps[0]);
    SetKVP_IVP(&p[entries++], &KvpsHead, "Running", &patchStatus, 0, 15);
    if (fs_ready) {
        SetKVP_FNCTN(&p[entries++], &KvpsHead, "Load SD", &EnterMenuLoad);
    }
    else {
        SetKVP_FNCTN(&p[entries++], &KvpsHead, "No SDCard", NULL);
    }
    SetKVP_AVP(&p[entries++], &KvpsHead, "SDCard Tools", 2, &p1[0]);
    SetKVP_AVP(&p[entries++], &KvpsHead, "ADCs", 3, &ADCkvps[0]);
    SetKVP_IVP(&p[entries++], &KvpsHead, "dsp%", &dspLoad200, 0, 100);

    SetKVP_AVP(&KvpsHead, NULL, "--- AXOLOTI ---", entries, &p[0]);

    KvpsDisplay = &KvpsHead;

    int i; for (i = 0; i < 3; i++) {
        ADCkvps[i].kvptype = KVP_TYPE_SVP;
        ADCkvps[i].svp.value = (int16_t *)&adcvalues[i];
        char *str = chCoreAlloc(6);
        str[0] = 'A';
        str[1] = 'D';
        str[2] = 'C';
        str[3] = '0' + i;
        str[4] = 0;
        // sprintf(str,"CC%i",i);
        ADCkvps[i].keyname = str;    //(char *)i;
    }

    ObjectKvpRoot = &p[0];
#endif

    chThdCreateStatic(waThreadUI, sizeof(waThreadUI), UI_USB_PRIO, ThreadUI, NULL);
}

void KVP_ClearObjects(void) {
    ObjectKvpRoot->apvp.length = 0;
    // KvpsDisplay = &KvpsHead;
}

void KVP_RegisterObject(KeyValuePair_s *kvp) {
    ObjectKvps[ObjectKvpRoot->apvp.length] = kvp;
    // kvp->parent = ObjectKvpRoot;
    ObjectKvpRoot->apvp.length++;
}

// #define LCD_COL_INDENT 5
// #define LCD_COL_EQ 91
// #define LCD_COL_VAL 97
// #define LCD_COL_ENTER 97
// #define STATUSROW 7
