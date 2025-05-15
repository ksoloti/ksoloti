/* Ksoloti USB Host HID Joystick Class 
 *
 * Based onSTMicroelectronics USB Host Middleware
 * and stm32f401ccu_usb_host_ps3_joystick by Dong-Higenis
 * https://github.com/dong-higenis/stm32f401ccu_usb_host_ps3_joystick
 *
 * And the following tutorial(s)
 * https://blog.csdn.net/softlove03/article/details/128616152
 * https://blog.csdn.net/softlove03/article/details/128646031
 *
 *
 ******************************************************************************
 * @attention
 *
 * <h2><center>&copy; COPYRIGHT 2014 STMicroelectronics</center></h2>
 *
 * Licensed under MCD-ST Liberty SW License Agreement V2, (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *        http://www.st.com/software_license_agreement_liberty_v2
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************
 */


// TODO: UART communication via HC-05 Bluetooth dongle?

#include "usbh_hid_joystick.h"
#include "usbh_hid_parser.h"


#define MIN_JOY_SEND_TIME_MS (1000)

HID_JOYSTICK_Info_TypeDef  joystick_info;
uint32_t                   joystick_report_data[USBH_HID_JOYSTICK_PROTOLENGTH];

static USBH_StatusTypeDef USBH_HID_JoystickDecode(USBH_HandleTypeDef *phost);

/* Structures defining how to access items in a HID joystick report */

/* Access x coordinate change. */
static const HID_Report_ItemTypedef prop_x =
{
    (uint8_t *)joystick_report_data + 1, /*data*/
    8,     /*size*/
    0,     /*shift*/
    0,     /*count (only for array items)*/
    1,     /*signed?*/
    0,     /*min value read can return*/
    0xFF,  /*max value read can return*/
    0,     /*min vale device can report*/
    0xFF,  /*max value device can report*/
    1      /*resolution*/
};

/* Access y coordinate change. */
static const HID_Report_ItemTypedef prop_y =
{
    (uint8_t *)joystick_report_data + 2, /*data*/
    8,     /*size*/
    0,     /*shift*/
    0,     /*count (only for array items)*/
    1,     /*signed?*/
    0,     /*min value read can return*/
    0xFF,  /*max value read can return*/
    0,     /*min vale device can report*/
    0xFF,  /*max value device can report*/
    1      /*resolution*/
};


/* Access y coordinate change. */
static const HID_Report_ItemTypedef prop_z =
{
    (uint8_t *)joystick_report_data + 3, /*data*/
    8,     /*size*/
    0,     /*shift*/
    0,     /*count (only for array items)*/
    1,     /*signed?*/
    0,     /*min value read can return*/
    0xFF,  /*max value read can return*/
    0,     /*min vale device can report*/
    0xFF,  /*max value device can report*/
    1      /*resolution*/
};


/* Access y coordinate change. */
static const HID_Report_ItemTypedef prop_rz =
{
    (uint8_t *)joystick_report_data + 4, /*data*/
    8,     /*size*/
    0,     /*shift*/
    0,     /*count (only for array items)*/
    1,     /*signed?*/
    0,     /*min value read can return*/
    0xFF,  /*max value read can return*/
    0,     /*min vale device can report*/
    0xFF,  /*max value device can report*/
    1      /*resolution*/
};

static const HID_Report_ItemTypedef prop_pad =
{
    (uint8_t *)joystick_report_data + 5, /*data*/
    4,     /*size*/
    0,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    0x0F,  /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button A state. */
static const HID_Report_ItemTypedef prop_btn_a =
{
    (uint8_t *)joystick_report_data + 6, /*data*/
    1,     /*size*/
    0,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button B state. */
static const HID_Report_ItemTypedef prop_btn_b =
{
    (uint8_t *)joystick_report_data + 6, /*data*/
    1,     /*size*/
    1,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};


/* Access button X state. */
static const HID_Report_ItemTypedef prop_btn_x =
{
    (uint8_t *)joystick_report_data + 6, /*data*/
    1,     /*size*/
    3,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button Y state. */
static const HID_Report_ItemTypedef prop_btn_y =
{
    (uint8_t *)joystick_report_data + 6, /*data*/
    1,     /*size*/
    4,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button L1 state. */
static const HID_Report_ItemTypedef prop_btn_l1 =
{
    (uint8_t *)joystick_report_data + 6, /*data*/
    1,     /*size*/
    6,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button R1 state. */
static const HID_Report_ItemTypedef prop_btn_r1 =
{
    (uint8_t *)joystick_report_data + 6, /*data*/
    1,     /*size*/
    7,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access hat switch right state. */
static const HID_Report_ItemTypedef prop_hat_switch_left =
{
    (uint8_t *)joystick_report_data + 7, /*data*/
    1,     /*size*/
    5,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min vale device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access hat switch right state. */
static const HID_Report_ItemTypedef prop_hat_switch_right =
{
    (uint8_t *)joystick_report_data + 7, /*data*/
    1,     /*size*/
    6,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min vale device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button L2 state. */
static const HID_Report_ItemTypedef prop_btn_l2 =
{
    (uint8_t *)joystick_report_data + 7, /*data*/
    1,     /*size*/
    0,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button R2 state. */
static const HID_Report_ItemTypedef prop_btn_r2 =
{
    (uint8_t *)joystick_report_data + 7, /*data*/
    1,     /*size*/
    1,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button Select state. */
static const HID_Report_ItemTypedef prop_btn_select =
{
    (uint8_t *)joystick_report_data + 7, /*data*/
    1,     /*size*/
    2,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/* Access button Start state. */
static const HID_Report_ItemTypedef prop_btn_start =
{
    (uint8_t *)joystick_report_data + 7, /*data*/
    1,     /*size*/
    3,     /*shift*/
    0,     /*count (only for array items)*/
    0,     /*signed?*/
    0,     /*min value read can return*/
    1,     /*max value read can return*/
    0,     /*min value device can report*/
    1,     /*max value device can report*/
    1      /*resolution*/
};

/**
  * @}
  */

/** @defgroup USBH_HID_JOYSTICK_Private_Functions
  * @{
  */

/**
  * @brief  USBH_HID_JoystickInit
  *         The function init the HID Joystick.
  * @param  phost: Host handle
  * @retval USBH Status
  */
USBH_StatusTypeDef USBH_HID_JoystickInit(USBH_HandleTypeDef *phost)
{
    uint32_t x;
    HID_HandleTypeDef *HID_Handle = (HID_HandleTypeDef *) phost->pActiveClass->pData;

    // USBH_memset(&joystick_info, 0, sizeof(HID_JOYSTICK_Info_TypeDef));

    for (x = 0U; x < (sizeof(joystick_report_data)/sizeof(uint32_t)); x++)
    {
  	    joystick_report_data[x] = 0U;
    }

    if (HID_Handle->length > (sizeof(joystick_report_data)/sizeof(uint32_t)))
    {
        HID_Handle->length = (sizeof(joystick_report_data)/sizeof(uint32_t));
    }

    HID_Handle->pData = (uint8_t *)joystick_report_data;
    fifo_init(&HID_Handle->fifo, phost->device.Data, HID_QUEUE_SIZE * sizeof(joystick_report_data)); // changed to fifo_init

    return USBH_OK;
}

/**
  * @brief  USBH_HID_GetJoystickInfo
  *         The function return joystick information.
  * @param  phost: Host handle
  * @retval joystick information
  */
HID_JOYSTICK_Info_TypeDef *USBH_HID_GetJoystickInfo(USBH_HandleTypeDef *phost)
{
    if (USBH_HID_JoystickDecode(phost) == USBH_OK)
    {
        return &joystick_info;
    }
    else
    {
        return NULL;
    }
}


#if USBH_DEBUG_LEVEL > 2
void print_pushed(uint8_t b)
{
	USBH_DbgLog("%c ", b?'O':'X');
}

void print_joy_info(HID_JOYSTICK_Info_TypeDef joystick_info)
{
    USBH_DbgLog("\n");
    USBH_DbgLog("%3d ", (char)joystick_info.left_axis_x);
    USBH_DbgLog("%3d ", (char)joystick_info.left_axis_y);
    USBH_DbgLog("%3d ", (char)joystick_info.right_axis_x);
    USBH_DbgLog("%3d ", (char)joystick_info.right_axis_y);
    USBH_DbgLog("%02X ", joystick_info.pad_arrow);
    print_pushed(joystick_info.left_hat);
    print_pushed(joystick_info.right_hat);
    print_pushed(joystick_info.select);
    print_pushed(joystick_info.start);
    print_pushed(joystick_info.pad_a);
    print_pushed(joystick_info.pad_b);
    print_pushed(joystick_info.pad_x);
    print_pushed(joystick_info.pad_y);
    print_pushed(joystick_info.l1);
    print_pushed(joystick_info.r1);
    print_pushed(joystick_info.l2);
    print_pushed(joystick_info.r2);
}
#endif


/**
  * @brief  USBH_HID_JoystickDecode
  *         The function decode joystick data.
  * @param  phost: Host handle
  * @retval USBH Status
  */
static USBH_StatusTypeDef USBH_HID_JoystickDecode(USBH_HandleTypeDef *phost)
{
    HID_HandleTypeDef *HID_Handle = (HID_HandleTypeDef *) phost->pActiveClass->pData;

    if (HID_Handle->length == 0U)
    {
        return USBH_FAIL;
    }

    /*Fill report */
    if (fifo_read(&HID_Handle->fifo, &joystick_report_data, HID_Handle->length) ==  HID_Handle->length)
    {

        // SEB: removed is_diff check and old_report_data

#if USBH_DEBUG_LEVEL > 2
        USBH_DbgLog("\n");
        uint32_t x;
        for(x = 0; x < HID_Handle->length; x++)
        {
    	    USBH_DbgLog("%02X ", HID_Handle->pData[x]);
        }
#endif

        /*Decode report */ // TOOD: need all the "&" and "x ? y : z" checks?
        joystick_info.pad_arrow    = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_pad, 0U) & 0x0F;
        joystick_info.left_hat     = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_hat_switch_left, 0U) ? 1 : 0;
  	    joystick_info.right_hat    = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_hat_switch_right, 0U) ? 1 : 0;

        joystick_info.left_axis_x  = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_x, 0U);
        joystick_info.left_axis_y  = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_y, 0U);
        joystick_info.right_axis_x = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_z, 0U);
        joystick_info.right_axis_y = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_rz, 0U);

        joystick_info.pad_a        = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_a, 0U) ? 1 : 0;
        joystick_info.pad_b        = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_b, 0U) ? 1 : 0;
        joystick_info.pad_x        = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_x, 0U) ? 1 : 0;
        joystick_info.pad_y        = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_y, 0U) ? 1 : 0;

        joystick_info.l1           = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_l1, 0U) ? 1 : 0;
        joystick_info.r1           = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_r1, 0U) ? 1 : 0;
        joystick_info.l2           = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_l2, 0U) ? 1 : 0;
        joystick_info.r2           = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_r2, 0U) ? 1 : 0;

        joystick_info.select       = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_select, 0U) ? 1 : 0;
        joystick_info.start        = (uint8_t)HID_ReadItem((HID_Report_ItemTypedef *) &prop_btn_start, 0U) ? 1 : 0;

#if USBH_DEBUG_LEVEL > 2
        print_joy_info(joystick_info);
#endif

        return USBH_OK;
    }

    return USBH_FAIL;
}

// hid event callback SEB: Ksoloti does this in usbh_conf.c
// void USBH_HID_EventCallback(USBH_HandleTypeDef *phost)
// {
// 	  USBH_HID_GetJoystickInfo(phost);
// }


/************************ END OF FILE****/
