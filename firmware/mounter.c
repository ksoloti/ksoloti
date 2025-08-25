/*
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
 * Edited 2023 - 2024 by AndyCap, Ksoloti
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

#include "mass_storage/usb_msd.h"
#include "ch.h"
#include "hal.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "ff.h"
#include "ffconf.h"
// #include "axoloti_board.h"

#if defined(BOARD_AXOLOTI_CORE) || defined(BOARD_KSOLOTI_CORE)
#define LED1_PORT GPIOG
#define LED1_PIN 6
#define LED2_PORT GPIOC
#define LED2_PIN 6
// SW1 is also BOOT0
#define SW1_PORT GPIOB
#define SW1_PIN 5
#define SW2_PORT GPIOA
#define SW2_PIN 10
#define OCFLAG_PORT GPIOG
#define OCFLAG_PIN 13
#define SDCSW_PORT GPIOD
#define SDCSW_PIN 13
#endif

/* endpoint index */
#define USB_MS_DATA_EP 1

extern void inttohex(uint32_t v, unsigned char *p);

/* USB device descriptor */
static const uint8_t deviceDescriptorData[] = {
    USB_DESC_DEVICE(
        0x0200, /* supported USB version (2.0)                     */
        0x00,   /* device class (none, specified in interface)     */
        0x00,   /* device sub-class (none, specified in interface) */
        0x00,   /* device protocol (none, specified in interface)  */
        64,     /* max packet size of control end-point            */
        0x16C0, /* vendor ID (Voti)                                */

#if defined(BOARD_KSOLOTI_CORE)
        /* Ksoloti Core */
        0x0445, /* product ID (lab use only!)                      */
#elif defined(BOARD_AXOLOTI_CORE)
        /* Axoloti Core */
        0x0443, /* product ID (lab use only!)                      */
#endif

        0x0100, /* device release number                           */
        1,      /* index of manufacturer string descriptor         */
        2,      /* index of product string descriptor              */
        3,      /* index of serial number string descriptor        */
        1       /* number of possible configurations               */
    )
};

static const USBDescriptor deviceDescriptor = {
    sizeof(deviceDescriptorData),
    deviceDescriptorData
};

/* configuration descriptor */
static const uint8_t configurationDescriptorData[] = {
    /* configuration descriptor */
    USB_DESC_CONFIGURATION(
        32,   /* total length                                             */
        1,    /* number of interfaces                                     */
        1,    /* value that selects this configuration                    */
        0,    /* index of string descriptor describing this configuration */
        0xC0, /* attributes (self-powered)                                */
        150   /* max power (300 mA)                                       */
    ),

    /* interface descriptor */
    USB_DESC_INTERFACE(
        0,    /* interface number                                     */
        0,    /* value used to select alternative setting             */
        2,    /* number of end-points used by this interface          */
        0x08, /* interface class (Mass Storage)                       */
        0x06, /* interface sub-class (SCSI Transparent Storage)       */
        0x50, /* interface protocol (Bulk Only)                       */
        0     /* index of string descriptor describing this interface */
    ),

    /* end-point descriptor */
    USB_DESC_ENDPOINT(
        USB_MS_DATA_EP | 0x00, /* address (end point index | IN direction)       */
        USB_EP_MODE_TYPE_BULK, /* attributes (bulk)                              */
        64,                    /* max packet size                                */
        0x05                   /* polling interval (ignored for bulk end-points) */
    ),

    /* end-point descriptor */
    USB_DESC_ENDPOINT(
        USB_MS_DATA_EP | 0x80, /* address (end point index | OUT direction)      */
        USB_EP_MODE_TYPE_BULK, /* attributes (bulk)                              */
        64,                    /* max packet size                                */
        0x05                   /* polling interval (ignored for bulk end-points) */
    )
};

static const USBDescriptor configurationDescriptor = {
    sizeof(configurationDescriptorData),
    configurationDescriptorData
};

/* Language descriptor */
static const uint8_t languageDescriptorData[] = {
    USB_DESC_BYTE(4),
    USB_DESC_BYTE(USB_DESCRIPTOR_STRING),
    USB_DESC_WORD(0x0409) /* U.S. English */
};

static const USBDescriptor languageDescriptor = {
    sizeof(languageDescriptorData),
    languageDescriptorData
};

/* Vendor descriptor */
static const uint8_t vendorDescriptorData[] = {
    USB_DESC_BYTE(16),
    USB_DESC_BYTE(USB_DESCRIPTOR_STRING),

#if defined(BOARD_KSOLOTI_CORE)
    'K', 0, 's', 0, 'o', 0, 'l', 0, 'o', 0, 't', 0, 'i', 0
#elif defined(BOARD_AXOLOTI_CORE)
    'A', 0, 'x', 0, 'o', 0, 'l', 0, 'o', 0, 't', 0, 'i', 0
#endif

};

static const USBDescriptor vendorDescriptor = {
    sizeof(vendorDescriptorData),
    vendorDescriptorData
};

/* Product descriptor */
static const uint8_t productDescriptorData[] = {
    USB_DESC_BYTE(40),
    USB_DESC_BYTE(USB_DESCRIPTOR_STRING),
#if defined(BOARD_KSOLOTI_CORE)
    'K', 0, 's', 0, 'o', 0, 'l', 0, 'o', 0, 't', 0, 'i', 0, ' ', 0,
#elif defined(BOARD_AXOLOTI_CORE)
    'A', 0, 'x', 0, 'o', 0, 'l', 0, 'o', 0, 't', 0, 'i', 0, ' ', 0,
#endif
    'C', 0, 'a', 0, 'r', 0, 'd', 0, 'r', 0, 'e', 0, 'a', 0, 'd', 0, 'e', 0, 'r', 0
};

static const USBDescriptor productDescriptor = {
    sizeof(productDescriptorData),
    productDescriptorData
};

/* Serial number descriptor */
// static const uint8_t serialNumberDescriptorData[] = {
//     USB_DESC_BYTE(26),
//     USB_DESC_BYTE(USB_DESCRIPTOR_STRING),
//     '0', 0, '0', 0, '0', 0, '0', 0, '0', 0, '0', 0, '0', 0, '0', 0, '0', 0, '0', 0, '0', 0, '1', 0
// };

static uint8_t serialNumberDescriptorData[] = {
    USB_DESC_BYTE(52),                    /* bLength.                         */
    USB_DESC_BYTE(USB_DESCRIPTOR_STRING), /* bDescriptorType.                 */
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
  #if defined(BOARD_KSOLOTI_CORE)
    'K', 0, '0', 0, '0', 0, '0', 0,
  #else
    'A', 0, '0', 0, '0', 0, '0', 0,
  #endif
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
    '0', 0, '0', 0, '0', 0, '0', 0,
  };
  
  

static const USBDescriptor serialNumberDescriptor = {
    sizeof(serialNumberDescriptorData),
    serialNumberDescriptorData
};


/* Handles GET_DESCRIPTOR requests from the USB host */
static const USBDescriptor *getDescriptor(USBDriver *usbp, uint8_t type, uint8_t index, uint16_t lang) {
    (void)usbp;
    (void)lang;

    switch (type) {
        case USB_DESCRIPTOR_DEVICE:
            return &deviceDescriptor;
        case USB_DESCRIPTOR_CONFIGURATION:
            return &configurationDescriptor;
        case USB_DESCRIPTOR_STRING:
            switch (index) {
                case 0:
                    return &languageDescriptor;
                case 1:
                    return &vendorDescriptor;
                case 2:
                    return &productDescriptor;
                case 3: {
                    inttohex(*((uint32_t*)0x1FFF7A10),&serialNumberDescriptorData[2]);
                    inttohex(*((uint32_t*)0x1FFF7A14),&serialNumberDescriptorData[2+16]);
                    inttohex(*((uint32_t*)0x1FFF7A18),&serialNumberDescriptorData[2+32]);
                    return &serialNumberDescriptor;
                }
            }
    }
    return 0;
}

/* USB mass storage driver */
USBMassStorageDriver UMSD1;

/* Handles global events of the USB driver */
static void usbEvent(USBDriver *usbp, usbevent_t event) {
    switch (event) {
        case USB_EVENT_CONFIGURED:
            chSysLockFromIsr();
            // usbInitEndpointI(usbp, USB_MS_DATA_EP, &ep_data_config);
            msdConfigureHookI(&UMSD1);
            chSysUnlockFromIsr();
            break;

        case USB_EVENT_RESET:
        case USB_EVENT_ADDRESS:
        case USB_EVENT_SUSPEND:
        case USB_EVENT_WAKEUP:
        case USB_EVENT_STALLED:
        default:
            break;
    }
}

/* Configuration of the USB driver */
static const USBConfig usbConfig = {
    usbEvent,
    getDescriptor,
    msdRequestsHook,
    0
};

/* Turns on a LED when there is I/O activity on the USB port */
static void usbActivity(bool_t active) {
    if (active) {
        palSetPad(LED1_PORT, LED1_PIN);
    }
    else {
        palClearPad(LED1_PORT, LED1_PIN);
    }
}

/* USB mass storage configuration */
static const USBMassStorageConfig msdConfig = {
    &USBD1,
    (BaseBlockDevice*) &SDCD1,
    USB_MS_DATA_EP,
    &usbActivity,

#if defined(BOARD_KSOLOTI_CORE)
    "Ksoloti",
#elif defined(BOARD_AXOLOTI_CORE)
    "Axoloti",
#endif

    "Cardreader",
    "0.1"
};

int mounter(void) {
    /* float usb inputs, hope the host notices detach... */
    palSetPadMode(GPIOA, 11, PAL_MODE_INPUT);
    palSetPadMode(GPIOA, 12, PAL_MODE_INPUT);

    /* setup LEDs, red+green on */
    palSetPadMode(LED1_PORT, LED1_PIN, PAL_MODE_OUTPUT_PUSHPULL);
    palSetPadMode(LED2_PORT, LED2_PIN, PAL_MODE_OUTPUT_PUSHPULL);
    palClearPad(LED1_PORT, LED1_PIN);
    palClearPad(LED2_PORT, LED2_PIN);

    chSysInit();

    palSetPadMode(GPIOA, 11, PAL_MODE_ALTERNATE(10));
    palSetPadMode(GPIOA, 12, PAL_MODE_ALTERNATE(10));

    palSetPadMode(GPIOC, 8,  PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_HIGHEST);
    palSetPadMode(GPIOC, 9,  PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_HIGHEST);
    palSetPadMode(GPIOC, 10, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_HIGHEST);
    palSetPadMode(GPIOC, 11, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_HIGHEST);
    palSetPadMode(GPIOC, 12, PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_HIGHEST);
    palSetPadMode(GPIOD, 2,  PAL_MODE_ALTERNATE(12) | PAL_STM32_OSPEED_HIGHEST);
    chThdSleepMilliseconds(1);

    /* initialize the SD card */
    sdcStart(&SDCD1, NULL);
    sdcConnect(&SDCD1);

    FATFS SDC_FS;
    FRESULT err;
    /* mount the FS in FatFS... */
    err = f_mount(&SDC_FS, "", 0);
    chThdSleepMilliseconds(10);
    /* Then unmount it again. (0 or NULL as first argument)
       This should clear the dirty bit or whatever else it is the Mounter dislikes. */
    err |= f_mount(NULL, "", 0);
    if (err != FR_OK) {
        /* If card couldn't be mounted/unmounted even by FatFS,
           either there is no card connected, or the card is faulty.
           Do a slow alternate blink pattern and reboot. */
        palClearPad(LED1_PORT, LED1_PIN);
        palSetPad(LED2_PORT, LED2_PIN);
        chThdSleepMilliseconds(200);
        palSetPad(LED1_PORT, LED1_PIN);
        palClearPad(LED2_PORT, LED2_PIN);
        chThdSleepMilliseconds(200);
        palClearPad(LED1_PORT, LED1_PIN);
        palSetPad(LED2_PORT, LED2_PIN);
        chThdSleepMilliseconds(200);
        palSetPad(LED1_PORT, LED1_PIN);
        palClearPad(LED2_PORT, LED2_PIN);
        chThdSleepMilliseconds(200);
        palClearPad(LED1_PORT, LED1_PIN);
        palSetPad(LED2_PORT, LED2_PIN);
        chThdSleepMilliseconds(200);
        palSetPad(LED1_PORT, LED1_PIN);
        palClearPad(LED2_PORT, LED2_PIN);
        chThdSleepMilliseconds(200);
        palClearPad(LED1_PORT, LED1_PIN);
        palSetPad(LED2_PORT, LED2_PIN);
        chThdSleepMilliseconds(200);
        palSetPad(LED1_PORT, LED1_PIN);
        palClearPad(LED2_PORT, LED2_PIN);
        chThdSleepMilliseconds(200);
        NVIC_SystemReset();
    }

    /* initialize the USB mass storage driver */
    msdInit(&UMSD1);

    /* turn off green LED, turn on red LED */
    palClearPad(LED1_PORT, LED1_PIN);
    palSetPad(LED2_PORT, LED2_PIN);

    /* start the USB mass storage service */
    int ret = msdStart(&UMSD1, &msdConfig);
    if (ret != 0) {
        /* no media found : bye bye! */
        usbDisconnectBus(&USBD1);
        chThdSleepMilliseconds(1000);
        NVIC_SystemReset();
    }

    /* watch the mass storage events */
    EventListener connected;
    EventListener ejected;
    chEvtRegisterMask(&UMSD1.evt_connected, &connected, EVENT_MASK(1));
    chEvtRegisterMask(&UMSD1.evt_ejected, &ejected, EVENT_MASK(2));

    /* start the USB driver */
    usbDisconnectBus(&USBD1);
    chThdSleepMilliseconds(1000);
    usbStart(&USBD1, &usbConfig);
    usbConnectBus(&USBD1);

    while (1) {
        if (chEvtWaitOne(EVENT_MASK(1))) {
            /* media is now connected */

            /* wait until the media is ejected */
            chEvtWaitOne(EVENT_MASK(2));
            /* media is now ejected : bye bye! */
            usbDisconnectBus(&USBD1);
            chThdSleepMilliseconds(1000);
            NVIC_SystemReset();
        }
        chThdSleepMilliseconds(1000);
    }

    return 0;
}