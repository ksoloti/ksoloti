/**
  ******************************************************************************
  * @file           : Target/usbh_conf.c
  * @version        : v1.0_Cube
  * @brief          : This file implements the board support package for the USB host library
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2025 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */

#include "usbh_core.h"

#include "usbh_hid.h"
#include "usbh_hid_parser.h"
#include "usbh_midi_core.h"
#include "ch.h"

#include "midi.h"

#include "UsbLog.h"

#define HOST_POWERSW_CLK_ENABLE()          __GPIOC_CLK_ENABLE()
#define HOST_POWERSW_PORT                  GPIOD
#define HOST_POWERSW_VBUS                  GPIO_PIN_7

#if USE_USB_LOG
void osMessagePut(osMessageQId queue_id, uint32_t info, uint32_t millisec)
{
  AddUsbLog(ltPut, 1<<info);
  if(port_is_isr_context()) 
    chEvtSignalI (queue_id, 1<<info);
  else                      
    chEvtSignal (queue_id, 1<<info);    
}

void osMessagePutI(osMessageQId queue_id, uint32_t info, uint32_t millisec)
{
  AddUsbLog(ltPut, 1<<info);
  chEvtSignal (queue_id,1<<info);
}
#endif



void MIDI_CB(uint8_t a,uint8_t b,uint8_t c,uint8_t d){
  USBH_DbgLog("M %x - %x %x %x\r\n",a,b,c,d);
  //  a= pkt header 0xF0 = cable number 0x0F=CIN
  MidiInMsgHandler(MIDI_DEVICE_USB_HOST, ((a & 0xF0) >> 4)+ 1 ,b,c,d);
}

HCD_HandleTypeDef hhcd_USB_OTG_HS;
USBH_HandleTypeDef hUSBHost; /* USB Host handle */

void Error_Handler(void);

/* Private function prototypes -----------------------------------------------*/
USBH_StatusTypeDef USBH_Get_USB_Status(HAL_StatusTypeDef hal_status);

/* Private functions ---------------------------------------------------------*/

void MX_DriverVbusHS(uint8_t state)
{
  GPIO_InitTypeDef GPIO_InitStruct;

  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);
  /* Configure Power Switch Vbus Pin */
  GPIO_InitStruct.Pin = HOST_POWERSW_VBUS;
  GPIO_InitStruct.Speed = GPIO_SPEED_FAST;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(HOST_POWERSW_PORT, &GPIO_InitStruct);

  uint8_t data = state;
  /* USER CODE BEGIN PREPARE_GPIO_DATA_VBUS_HS */
  if(state == 0)
  {
    /* Drive high Charge pump */
    data = GPIO_PIN_SET;
  }
  else
  {
    /* Drive low Charge pump */
    data = GPIO_PIN_RESET;
  }
  /* USER CODE END PREPARE_GPIO_DATA_VBUS_HS */
  HAL_GPIO_WritePin(GPIOD,GPIO_PIN_7,(GPIO_PinState)data);
}

/*******************************************************************************
                       LL Driver Callbacks (HCD -> USB Host Library)
*******************************************************************************/
/* MSP Init */

void HAL_HCD_MspInit(HCD_HandleTypeDef* hcdHandle)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};

#if DEBUG_ON_GPIO
  // for debug
  GPIO_InitStruct.Pin = GPIO_PIN_0 | GPIO_PIN_1;
  GPIO_InitStruct.Speed = GPIO_SPEED_HIGH;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Alternate = 0;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);
#endif

  //RCC_PeriphCLKInitTypeDef PeriphClkInitStruct = {0};
  if(hcdHandle->Instance==USB_OTG_HS)
  {
  /* USER CODE BEGIN USB_OTG_HS_MspInit 0 */

  /* USER CODE END USB_OTG_HS_MspInit 0 */

  /** Initializes the peripherals clock
  */
    // PeriphClkInitStruct.PeriphClockSelection = RCC_PERIPHCLK_USB;
    // PeriphClkInitStruct.UsbClockSelection = RCC_USBCLKSOURCE_PLL;
    // if (HAL_RCCEx_PeriphCLKConfig(&PeriphClkInitStruct) != HAL_OK)
    // {
    //   Error_Handler();
    // }

    __HAL_RCC_PLLCLKOUT_ENABLE(RCC_PLL1_DIVQ);
    __HAL_RCC_USB_CONFIG(RCC_USBCLKSOURCE_PLL);

  /** Enable USB Voltage detector
  */
    // HAL_PWREx_EnableUSBVoltageDetector();

    /* Enable the USB voltage detector */
    SET_BIT (PWR->CR3, PWR_CR3_USB33DEN);

    __HAL_RCC_GPIOB_CLK_ENABLE();
    /**USB_OTG_HS GPIO Configuration
    PB14     ------> USB_OTG_HS_DM
    PB15     ------> USB_OTG_HS_DP
    */
    GPIO_InitStruct.Pin = GPIO_PIN_14|GPIO_PIN_15;
    GPIO_InitStruct.Mode = GPIO_MODE_AF_PP;
    GPIO_InitStruct.Pull = GPIO_NOPULL;
    GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
    GPIO_InitStruct.Alternate = GPIO_AF12_OTG2_FS;
    HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

    /* Peripheral clock enable */
    __HAL_RCC_USB_OTG_HS_CLK_ENABLE();

    /* Peripheral interrupt init */
    HAL_NVIC_SetPriority(OTG_HS_IRQn, 6, 0);
    HAL_NVIC_EnableIRQ(OTG_HS_IRQn);
  /* USER CODE BEGIN USB_OTG_HS_MspInit 1 */

  /* USER CODE END USB_OTG_HS_MspInit 1 */
  }
}

void HAL_HCD_MspDeInit(HCD_HandleTypeDef* hcdHandle)
{
  if(hcdHandle->Instance==USB_OTG_HS)
  {
  /* USER CODE BEGIN USB_OTG_HS_MspDeInit 0 */

  /* USER CODE END USB_OTG_HS_MspDeInit 0 */
    /* Peripheral clock disable */
    __HAL_RCC_USB_OTG_HS_CLK_DISABLE();

    /**USB_OTG_HS GPIO Configuration
    PB14     ------> USB_OTG_HS_DM
    PB15     ------> USB_OTG_HS_DP
    */
    HAL_GPIO_DeInit(GPIOB, GPIO_PIN_14|GPIO_PIN_15);

    /* Peripheral interrupt Deinit*/
    HAL_NVIC_DisableIRQ(OTG_HS_IRQn);

  /* USER CODE BEGIN USB_OTG_HS_MspDeInit 1 */

  /* USER CODE END USB_OTG_HS_MspDeInit 1 */
  }
}

/**
  * @brief  SOF callback.
  * @param  hhcd: HCD handle
  * @retval None
  */
void HAL_HCD_SOF_Callback(HCD_HandleTypeDef *hhcd)
{
  USBH_LL_IncTimer(hhcd->pData);
}

/**
  * @brief  SOF callback.
  * @param  hhcd: HCD handle
  * @retval None
  */
void HAL_HCD_Connect_Callback(HCD_HandleTypeDef *hhcd)
{
  USBH_LL_Connect(hhcd->pData);
}

/**
  * @brief  SOF callback.
  * @param  hhcd: HCD handle
  * @retval None
  */
void HAL_HCD_Disconnect_Callback(HCD_HandleTypeDef *hhcd)
{
  USBH_LL_Disconnect(hhcd->pData);
}

/**
  * @brief  Notify URB state change callback.
  * @param  hhcd: HCD handle
  * @param  chnum: channel number
  * @param  urb_state: state
  * @retval None
  */
void HAL_HCD_HC_NotifyURBChange_Callback(HCD_HandleTypeDef *hhcd, uint8_t chnum, HCD_URBStateTypeDef urb_state)
{
  /* To be used with OS to sync URB state with the global state machine */
#if (USBH_USE_OS == 1)
  USBH_LL_NotifyURBChange(hhcd->pData);
#endif
}
/**
* @brief  Port Port Enabled callback.
  * @param  hhcd: HCD handle
  * @retval None
  */
void HAL_HCD_PortEnabled_Callback(HCD_HandleTypeDef *hhcd)
{
  USBH_LL_PortEnabled(hhcd->pData);
}

/**
  * @brief  Port Port Disabled callback.
  * @param  hhcd: HCD handle
  * @retval None
  */
void HAL_HCD_PortDisabled_Callback(HCD_HandleTypeDef *hhcd)
{
  USBH_LL_PortDisabled(hhcd->pData);
}

/*******************************************************************************
                       LL Driver Interface (USB Host Library --> HCD)
*******************************************************************************/

/**
  * @brief  Initialize the low level portion of the host driver.
  * @param  phost: Host handle
  * @retval USBH status
  */
USBH_StatusTypeDef USBH_LL_Init(USBH_HandleTypeDef *phost)
{
  /* Init USB_IP */
  if (phost->id == HOST_HS) {
  /* Link the driver to the stack. */
  hhcd_USB_OTG_HS.pData = phost;
  phost->pData = &hhcd_USB_OTG_HS;

  hhcd_USB_OTG_HS.Instance = USB_OTG_HS;
  hhcd_USB_OTG_HS.Init.Host_channels = 16;
  hhcd_USB_OTG_HS.Init.speed = HCD_SPEED_FULL;
  hhcd_USB_OTG_HS.Init.dma_enable = DISABLE;
  hhcd_USB_OTG_HS.Init.phy_itface = USB_OTG_EMBEDDED_PHY;
  hhcd_USB_OTG_HS.Init.Sof_enable = DISABLE;
  hhcd_USB_OTG_HS.Init.low_power_enable = DISABLE;
  hhcd_USB_OTG_HS.Init.use_external_vbus = DISABLE;
  if (HAL_HCD_Init(&hhcd_USB_OTG_HS) != HAL_OK)
  {
    Error_Handler( );
  }

  USBH_LL_SetTimer(phost, HAL_HCD_GetCurrentFrame(&hhcd_USB_OTG_HS));
  }
  return USBH_OK;
}

/**
  * @brief  De-Initialize the low level portion of the host driver.
  * @param  phost: Host handle
  * @retval USBH status
  */
USBH_StatusTypeDef USBH_LL_DeInit(USBH_HandleTypeDef *phost)
{
  HAL_StatusTypeDef hal_status = HAL_OK;
  USBH_StatusTypeDef usb_status = USBH_OK;

  hal_status = HAL_HCD_DeInit(phost->pData);

  usb_status = USBH_Get_USB_Status(hal_status);

  return usb_status;
}

/**
  * @brief  Start the low level portion of the host driver.
  * @param  phost: Host handle
  * @retval USBH status
  */
USBH_StatusTypeDef USBH_LL_Start(USBH_HandleTypeDef *phost)
{
  HAL_StatusTypeDef hal_status = HAL_OK;
  USBH_StatusTypeDef usb_status = USBH_OK;

  hal_status = HAL_HCD_Start(phost->pData);

  usb_status = USBH_Get_USB_Status(hal_status);

  return usb_status;
}

/**
  * @brief  Stop the low level portion of the host driver.
  * @param  phost: Host handle
  * @retval USBH status
  */
USBH_StatusTypeDef USBH_LL_Stop(USBH_HandleTypeDef *phost)
{
  HAL_StatusTypeDef hal_status = HAL_OK;
  USBH_StatusTypeDef usb_status = USBH_OK;

  hal_status = HAL_HCD_Stop(phost->pData);

  usb_status = USBH_Get_USB_Status(hal_status);

  return usb_status;
}

/**
  * @brief  Return the USB host speed from the low level driver.
  * @param  phost: Host handle
  * @retval USBH speeds
  */
USBH_SpeedTypeDef USBH_LL_GetSpeed(USBH_HandleTypeDef *phost)
{
  USBH_SpeedTypeDef speed = USBH_SPEED_FULL;

  switch (HAL_HCD_GetCurrentSpeed(phost->pData))
  {
  case 0 :
    speed = USBH_SPEED_HIGH;
    break;

  case 1 :
    speed = USBH_SPEED_FULL;
    break;

  case 2 :
    speed = USBH_SPEED_LOW;
    break;

  default:
   speed = USBH_SPEED_FULL;
    break;
  }
  return  speed;
}

/**
  * @brief  Reset the Host port of the low level driver.
  * @param  phost: Host handle
  * @retval USBH status
  */
USBH_StatusTypeDef USBH_LL_ResetPort(USBH_HandleTypeDef *phost)
{
  HAL_StatusTypeDef hal_status = HAL_OK;
  USBH_StatusTypeDef usb_status = USBH_OK;

  hal_status = HAL_HCD_ResetPort(phost->pData);

  usb_status = USBH_Get_USB_Status(hal_status);

  return usb_status;
}

/**
  * @brief  Return the last transferred packet size.
  * @param  phost: Host handle
  * @param  pipe: Pipe index
  * @retval Packet size
  */
uint32_t USBH_LL_GetLastXferSize(USBH_HandleTypeDef *phost, uint8_t pipe)
{
  return HAL_HCD_HC_GetXferCount(phost->pData, pipe);
}

/**
  * @brief  Open a pipe of the low level driver.
  * @param  phost: Host handle
  * @param  pipe_num: Pipe index
  * @param  epnum: Endpoint number
  * @param  dev_address: Device USB address
  * @param  speed: Device Speed
  * @param  ep_type: Endpoint type
  * @param  mps: Endpoint max packet size
  * @retval USBH status
  */
USBH_StatusTypeDef USBH_LL_OpenPipe(USBH_HandleTypeDef *phost, uint8_t pipe_num, uint8_t epnum,
                                    uint8_t dev_address, uint8_t speed, uint8_t ep_type, uint16_t mps)
{
  HAL_StatusTypeDef hal_status = HAL_OK;
  USBH_StatusTypeDef usb_status = USBH_OK;

  hal_status = HAL_HCD_HC_Init(phost->pData, pipe_num, epnum,
                               dev_address, speed, ep_type, mps);

  usb_status = USBH_Get_USB_Status(hal_status);

  return usb_status;
}

/**
  * @brief  Close a pipe of the low level driver.
  * @param  phost: Host handle
  * @param  pipe: Pipe index
  * @retval USBH status
  */
USBH_StatusTypeDef USBH_LL_ClosePipe(USBH_HandleTypeDef *phost, uint8_t pipe)
{
  HAL_StatusTypeDef hal_status = HAL_OK;
  USBH_StatusTypeDef usb_status = USBH_OK;

  hal_status = HAL_HCD_HC_Halt(phost->pData, pipe);

  usb_status = USBH_Get_USB_Status(hal_status);

  return usb_status;
}

/**
  * @brief  Submit a new URB to the low level driver.
  * @param  phost: Host handle
  * @param  pipe: Pipe index
  *         This parameter can be a value from 1 to 15
  * @param  direction : Channel number
  *          This parameter can be one of the these values:
  *           0 : Output
  *           1 : Input
  * @param  ep_type : Endpoint Type
  *          This parameter can be one of the these values:
  *            @arg EP_TYPE_CTRL: Control type
  *            @arg EP_TYPE_ISOC: Isochrounous type
  *            @arg EP_TYPE_BULK: Bulk type
  *            @arg EP_TYPE_INTR: Interrupt type
  * @param  token : Endpoint Type
  *          This parameter can be one of the these values:
  *            @arg 0: PID_SETUP
  *            @arg 1: PID_DATA
  * @param  pbuff : pointer to URB data
  * @param  length : Length of URB data
  * @param  do_ping : activate do ping protocol (for high speed only)
  *          This parameter can be one of the these values:
  *           0 : do ping inactive
  *           1 : do ping active
  * @retval Status
  */
USBH_StatusTypeDef USBH_LL_SubmitURB(USBH_HandleTypeDef *phost, uint8_t pipe, uint8_t direction,
                                     uint8_t ep_type, uint8_t token, uint8_t *pbuff, uint16_t length,
                                     uint8_t do_ping)
{
  HAL_StatusTypeDef hal_status = HAL_OK;
  USBH_StatusTypeDef usb_status = USBH_OK;

  hal_status = HAL_HCD_HC_SubmitRequest(phost->pData, pipe, direction ,
                                        ep_type, token, pbuff, length,
                                        do_ping);
  usb_status =  USBH_Get_USB_Status(hal_status);

  return usb_status;
}

/**
  * @brief  Get a URB state from the low level driver.
  * @param  phost: Host handle
  * @param  pipe: Pipe index
  *         This parameter can be a value from 1 to 15
  * @retval URB state
  *          This parameter can be one of the these values:
  *            @arg URB_IDLE
  *            @arg URB_DONE
  *            @arg URB_NOTREADY
  *            @arg URB_NYET
  *            @arg URB_ERROR
  *            @arg URB_STALL
  */
USBH_URBStateTypeDef USBH_LL_GetURBState(USBH_HandleTypeDef *phost, uint8_t pipe)
{
  return (USBH_URBStateTypeDef)HAL_HCD_HC_GetURBState (phost->pData, pipe);
}

/**
  * @brief  Drive VBUS.
  * @param  phost: Host handle
  * @param  state : VBUS state
  *          This parameter can be one of the these values:
  *           0 : VBUS Inactive
  *           1 : VBUS Active
  * @retval Status
  */
USBH_StatusTypeDef USBH_LL_DriverVBUS(USBH_HandleTypeDef *phost, uint8_t state)
{
  if (phost->id == HOST_HS) {
    MX_DriverVbusHS(state);
  }

  /* USER CODE BEGIN 0 */

  /* USER CODE END 0*/

  HAL_Delay(200);
  return USBH_OK;
}

/**
  * @brief  Set toggle for a pipe.
  * @param  phost: Host handle
  * @param  pipe: Pipe index
  * @param  toggle: toggle (0/1)
  * @retval Status
  */
USBH_StatusTypeDef USBH_LL_SetToggle(USBH_HandleTypeDef *phost, uint8_t pipe, uint8_t toggle)
{
  HCD_HandleTypeDef *pHandle;
  pHandle = phost->pData;

  if(pHandle->hc[pipe].ep_is_in)
  {
    pHandle->hc[pipe].toggle_in = toggle;
  }
  else
  {
    pHandle->hc[pipe].toggle_out = toggle;
  }

  return USBH_OK;
}

/**
  * @brief  Return the current toggle of a pipe.
  * @param  phost: Host handle
  * @param  pipe: Pipe index
  * @retval toggle (0/1)
  */
uint8_t USBH_LL_GetToggle(USBH_HandleTypeDef *phost, uint8_t pipe)
{
  uint8_t toggle = 0;
  HCD_HandleTypeDef *pHandle;
  pHandle = phost->pData;

  if(pHandle->hc[pipe].ep_is_in)
  {
    toggle = pHandle->hc[pipe].toggle_in;
  }
  else
  {
    toggle = pHandle->hc[pipe].toggle_out;
  }
  return toggle;
}

/**
  * @brief  Delay routine for the USB Host Library
  * @param  Delay: Delay in ms
  * @retval None
  */
void USBH_Delay(uint32_t Delay)
{
  HAL_Delay(Delay);
}

/**
  * @brief  Returns the USB status depending on the HAL status:
  * @param  hal_status: HAL status
  * @retval USB status
  */
USBH_StatusTypeDef USBH_Get_USB_Status(HAL_StatusTypeDef hal_status)
{
  USBH_StatusTypeDef usb_status = USBH_OK;

  switch (hal_status)
  {
    case HAL_OK :
      usb_status = USBH_OK;
    break;
    case HAL_ERROR :
      usb_status = USBH_FAIL;
    break;
    case HAL_BUSY :
      usb_status = USBH_BUSY;
    break;
    case HAL_TIMEOUT :
      usb_status = USBH_FAIL;
    break;
    default :
      usb_status = USBH_FAIL;
    break;
  }
  return usb_status;
}


/**
 * @brief  User Process
 * @param  phost: Host Handle
 * @param  id: Host Library user message ID
 * @retval none
 */
static void USBH_UserProcess(USBH_HandleTypeDef *pHost, uint8_t vId) {
  switch (vId) {
  case HOST_USER_SELECT_CONFIGURATION:
    break;

  case HOST_USER_DISCONNECTION:
    break;

  case HOST_USER_CLASS_ACTIVE:
    break;

  case HOST_USER_CONNECTION:
    break;

  default:
    break;
  }
}

extern USBH_ClassTypeDef  Vendor_Class;
#define USBH_VENDOR_CLASS  &Vendor_Class



void MY_USBH_Init(void) {

  /* Init Host Library */
  USBH_Init(&hUSBHost, USBH_UserProcess, 0);

  /* Add Supported Class */
  /* highest priority first */
  if(USBH_OK != USBH_RegisterClass(&hUSBHost, USBH_VENDOR_CLASS))
    USBH_DbgLog("Failed to register USBH_VENDOR_CLASS");
  else
    USBH_DbgLog("Registered USBH_VENDOR_CLASS");

  if(USBH_OK != USBH_RegisterClass(&hUSBHost, USBH_MIDI_CLASS))
    USBH_DbgLog("Failed to register USBH_MIDI_CLASS");
  else
    USBH_DbgLog("Registered USBH_MIDI_CLASS");

  if(USBH_OK != USBH_RegisterClass(&hUSBHost, USBH_HID_CLASS))
    USBH_DbgLog("Failed to register USBH_HID_CLASS");
  else
    USBH_DbgLog("Registered USBH_HID_CLASS");

  /* Start Host Process */
  USBH_Start(&hUSBHost);

}

uint8_t hid_buttons[3];
uint8_t hid_mouse_x;
uint8_t hid_mouse_y;

uint8_t hid_keys[6];
uint8_t hid_key_modifiers;

void USBH_HID_EventCallback(USBH_HandleTypeDef *phost) {

  if (USBH_HID_GetDeviceType(&hUSBHost) == HID_MOUSE) {

    HID_MOUSE_Info_TypeDef *m_pinfo_mouse;
    m_pinfo_mouse = USBH_HID_GetMouseInfo(phost);

    if (m_pinfo_mouse) {

      USBH_DbgLog("btns:%u%u%u", m_pinfo_mouse->buttons[0],m_pinfo_mouse->buttons[1],m_pinfo_mouse->buttons[2]);

      hid_buttons[0] = m_pinfo_mouse->buttons[0];
      hid_buttons[1] = m_pinfo_mouse->buttons[1];
      hid_buttons[2] = m_pinfo_mouse->buttons[2];

      hid_mouse_x += m_pinfo_mouse->x;
      hid_mouse_y += m_pinfo_mouse->y;

    }
    else {
      hid_buttons[0] = 0;
      hid_buttons[1] = 0;
      hid_buttons[2] = 0;
    }

    USBH_DbgLog("btns:%u%u%u", hid_buttons[0],hid_buttons[1],hid_buttons[2]);

  }
  else if (USBH_HID_GetDeviceType(&hUSBHost) == HID_KEYBOARD) {

    HID_KEYBD_Info_TypeDef *m_pinfo_keyb;
    m_pinfo_keyb = USBH_HID_GetKeybdInfo(phost);

    if (m_pinfo_keyb) {

      hid_key_modifiers  =  (m_pinfo_keyb->lctrl  << 0);
      hid_key_modifiers  += (m_pinfo_keyb->lshift << 1);
      hid_key_modifiers  += (m_pinfo_keyb->lalt   << 2);
      hid_key_modifiers  += (m_pinfo_keyb->lgui   << 3);

      hid_key_modifiers  += (m_pinfo_keyb->rctrl  << 4);
      hid_key_modifiers  += (m_pinfo_keyb->rshift << 5);
      hid_key_modifiers  += (m_pinfo_keyb->ralt   << 6);
      hid_key_modifiers  += (m_pinfo_keyb->rgui   << 7);

      uint8_t k; for (k = 0; k < 6; k++) {
        hid_keys[k] = m_pinfo_keyb->keys[k];
      } 
    }
    else {

      hid_key_modifiers = 0;

      uint8_t k; for (k = 0; k < 6; k++) {
        hid_keys[k] = 0;
      }
    }

    // USBH_DbgLog( "hid_keys:%u%u%u%u%u%u",
    //   hid_keys[0], hid_keys[1], hid_keys[2], hid_keys[3], hid_keys[4], hid_keys[5]);

    // USBH_DbgLog( "->mods:%u%u%u%u%u%u%u%u",
    //   hid_key_lctrl,
    //   hid_key_lshift,
    //   hid_key_lalt,
    //   hid_key_lgui,
    //   hid_key_rctrl,
    //   hid_key_rshift,
    //   hid_key_ralt,
    //   hid_key_rgui);
  }
}


#define PORT_IRQ_HANDLER(id) void id(void)
#define CH_IRQ_HANDLER(id) PORT_IRQ_HANDLER(id)

char mem[256];
bool memused=0;

void* fakemalloc(size_t size){
  if (size > 256){
    USBH_ErrLog("fakemalloc: can't allocate...");
  }
  if (memused){
    USBH_ErrLog("fakemalloc: already taken...");
  }
  memused = 1;
  return (void*)mem;
}

void fakefree(void * p){
  (void)p;
  memused = 0;
}


//#define STM32_OTG1_HANDLER          Vector14C
//#define STM32_OTG2_HANDLER          Vector174
//#define STM32_OTG2_EP1OUT_HANDLER   Vector168
//#define STM32_OTG2_EP1IN_HANDLER    Vector16C

//STM32_OTG2_HANDLER
CH_IRQ_HANDLER(Vector174) 
{
  #if DEBUG_ON_GPIO
    HAL_GPIO_WritePin( GPIOA, GPIO_PIN_0, 1);
  #endif
  CH_IRQ_PROLOGUE();
  chSysLockFromIsr();
  HAL_HCD_IRQHandler(&hhcd_USB_OTG_HS);
  chSysUnlockFromIsr();
  CH_IRQ_EPILOGUE();
  #if DEBUG_ON_GPIO
    HAL_GPIO_WritePin( GPIOA, GPIO_PIN_0, 0);
  #endif
}

CH_IRQ_HANDLER(Vector168) {
  while (1) {
  }
}

CH_IRQ_HANDLER(Vector16C) {
  while (1) {
  }
}

void Error_Handler(void)
{
  __disable_irq();
  while (1)
  {
  }
}
