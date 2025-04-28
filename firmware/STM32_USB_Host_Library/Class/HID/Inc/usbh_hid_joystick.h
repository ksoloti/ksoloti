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
 */

#ifndef __USBH_HID_JOYSTICK_H
#define __USBH_HID_JOYSTICK_H

#ifdef __cplusplus
extern "C" {
#endif

#include "usbh_hid.h"

#define USBH_HID_JOYSTICK_PROTOLENGTH 8

typedef struct _HID_JOYSTICK_Info
{
    uint8_t              left_axis_x;
    uint8_t              left_axis_y;
    uint8_t              right_axis_x;
    uint8_t              right_axis_y;

    uint8_t              pad_arrow:4;
    uint8_t              left_hat:1;
    uint8_t              right_hat:1;
    uint8_t              select:1;
    uint8_t              start:1;

    uint8_t              pad_a:1;
    uint8_t              pad_b:1;
    uint8_t              pad_x:1;
    uint8_t              pad_y:1;
    uint8_t              reserved:4;

    uint8_t              l1:1;
    uint8_t              l2:1;
    uint8_t              r1:1;
    uint8_t              r2:1;
} HID_JOYSTICK_Info_TypeDef;

USBH_StatusTypeDef USBH_HID_JoystickInit(USBH_HandleTypeDef *phost);
HID_JOYSTICK_Info_TypeDef *USBH_HID_GetJoystickInfo(USBH_HandleTypeDef *phost);

#ifdef __cplusplus
}
#endif

#endif /* __USBH_HID_JOYSTICK_H */

/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/
