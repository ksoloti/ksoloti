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

/*
 * STM32F4xx drivers configuration.
 * The following settings override the default settings present in
 * the various device driver implementation headers.
 * Note that the settings for each driver only have effect if the whole
 * driver is enabled in halconf.h.
 *
 * IRQ priorities:
 * 15...0       Lowest...Highest.
 *
 * DMA priorities:
 * 0...3        Lowest...Highest.
 */

#ifndef _MCUCONF_H
#define _MCUCONF_H

#include "axoloti_defines.h"

/* MCU configuration is the same for Ksoloti and Axoloti boards
 * and for all firmware flavours (so far) 
 */

#define STM32F4xx_MCUCONF


/*
 * HAL driver system settings.
 */
#define STM32_NO_INIT                       FALSE
#define STM32_HSI_ENABLED                   FALSE
#define STM32_LSI_ENABLED                   FALSE
#define STM32_HSE_ENABLED                   TRUE
#define STM32_LSE_ENABLED                   FALSE
#define STM32_CLOCK48_REQUIRED              TRUE
#define STM32_SW                            STM32_SW_PLL
#define STM32_PLLSRC                        STM32_PLLSRC_HSE
#define STM32_PLLM_VALUE                    8
#define STM32_PLLN_VALUE                    336
#define STM32_PLLP_VALUE                    2
#define STM32_PLLQ_VALUE                    7
#define STM32_HPRE                          STM32_HPRE_DIV1
#define STM32_PPRE1                         STM32_PPRE1_DIV4
#define STM32_PPRE2                         STM32_PPRE2_DIV2
#define STM32_RTCSEL                        STM32_RTCSEL_NOCLOCK
#define STM32_RTCPRE_VALUE                  8
#define STM32_MCO1SEL                       STM32_MCO1SEL_HSE
#define STM32_MCO1PRE                       STM32_MCO1PRE_DIV1
#define STM32_MCO2SEL                       STM32_MCO2SEL_SYSCLK
#define STM32_MCO2PRE                       STM32_MCO2PRE_DIV5
#define STM32_I2SSRC                        STM32_I2SSRC_PLLI2S
#define STM32_PLLI2SN_VALUE                 384
#define STM32_PLLI2SR_VALUE                 5
#define STM32_PVD_ENABLE                    FALSE
#define STM32_PLS                           STM32_PLS_LEV0
#define STM32_BKPRAM_ENABLE                 FALSE

/* SEB If we ever go to 216 MHz (STM32F767)
#define STM32_PLLM_VALUE                    8
#define STM32_PLLN_VALUE                    432
#define STM32_PLLP_VALUE                    2
#define STM32_PLLQ_VALUE                    9
*/

/*
 * ADC driver system settings.
 */
#define STM32_ADC_ADCPRE                    ADC_CCR_ADCPRE_DIV8
#define STM32_ADC_USE_ADC1                  TRUE
#define STM32_ADC_USE_ADC2                  FALSE
#define STM32_ADC_USE_ADC3                  FALSE

#define STM32_ADC_ADC1_DMA_STREAM           STM32_DMA_STREAM_ID(2, 0)
#define STM32_ADC_ADC2_DMA_STREAM           STM32_DMA_STREAM_ID(2, 2)
#define STM32_ADC_ADC3_DMA_STREAM           STM32_DMA_STREAM_ID(2, 1)
#define STM32_ADC_ADC1_DMA_PRIORITY         2
#define STM32_ADC_ADC2_DMA_PRIORITY         2
#define STM32_ADC_ADC3_DMA_PRIORITY         2
#define STM32_ADC_IRQ_PRIORITY              6
#define STM32_ADC_ADC1_DMA_IRQ_PRIORITY     6
#define STM32_ADC_ADC2_DMA_IRQ_PRIORITY     6
#define STM32_ADC_ADC3_DMA_IRQ_PRIORITY     6

/*
 * CAN driver system settings.
 */
#define STM32_CAN_USE_CAN1                  FALSE
#define STM32_CAN_USE_CAN2                  FALSE
#define STM32_CAN_CAN1_IRQ_PRIORITY         11
#define STM32_CAN_CAN2_IRQ_PRIORITY         11

/*
 * EXT driver system settings.
 */
#define STM32_EXT_EXTI0_IRQ_PRIORITY        6
#define STM32_EXT_EXTI1_IRQ_PRIORITY        6
#define STM32_EXT_EXTI2_IRQ_PRIORITY        6
#define STM32_EXT_EXTI3_IRQ_PRIORITY        6
#define STM32_EXT_EXTI4_IRQ_PRIORITY        6
#define STM32_EXT_EXTI5_9_IRQ_PRIORITY      6
#define STM32_EXT_EXTI10_15_IRQ_PRIORITY    6
#define STM32_EXT_EXTI16_IRQ_PRIORITY       6
#define STM32_EXT_EXTI17_IRQ_PRIORITY       15
#define STM32_EXT_EXTI18_IRQ_PRIORITY       6
#define STM32_EXT_EXTI19_IRQ_PRIORITY       6
#define STM32_EXT_EXTI20_IRQ_PRIORITY       6
#define STM32_EXT_EXTI21_IRQ_PRIORITY       15
#define STM32_EXT_EXTI22_IRQ_PRIORITY       15

/*
 * GPT driver system settings.
 */
#define STM32_GPT_USE_TIM1                  FALSE
#define STM32_GPT_USE_TIM2                  FALSE
#define STM32_GPT_USE_TIM3                  FALSE
#define STM32_GPT_USE_TIM4                  FALSE
#define STM32_GPT_USE_TIM5                  FALSE
#define STM32_GPT_USE_TIM8                  FALSE
#define STM32_GPT_TIM1_IRQ_PRIORITY         7
#define STM32_GPT_TIM2_IRQ_PRIORITY         7
#define STM32_GPT_TIM3_IRQ_PRIORITY         7
#define STM32_GPT_TIM4_IRQ_PRIORITY         7
#define STM32_GPT_TIM5_IRQ_PRIORITY         7
#define STM32_GPT_TIM8_IRQ_PRIORITY         7

/*
 * I2C driver system settings.
 */
#define STM32_I2C_USE_I2C1                  TRUE
#define STM32_I2C_USE_I2C2                  FALSE
#define STM32_I2C_USE_I2C3                  FALSE

#define STM32_I2C_I2C1_RX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 5)
#define STM32_I2C_I2C1_TX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 6)
#define STM32_I2C_I2C2_RX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 2)
#define STM32_I2C_I2C2_TX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 7)
#define STM32_I2C_I2C3_RX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 2)
#define STM32_I2C_I2C3_TX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 4)
#define STM32_I2C_I2C1_IRQ_PRIORITY         5
#define STM32_I2C_I2C2_IRQ_PRIORITY         5
#define STM32_I2C_I2C3_IRQ_PRIORITY         5
#define STM32_I2C_I2C1_DMA_PRIORITY         3
#define STM32_I2C_I2C2_DMA_PRIORITY         3
#define STM32_I2C_I2C3_DMA_PRIORITY         3
#define STM32_I2C_I2C1_DMA_ERROR_HOOK()     chSysHalt("I2C_I2C1_DMA_ERROR")
#define STM32_I2C_I2C2_DMA_ERROR_HOOK()     chSysHalt("I2C_I2C2_DMA_ERROR")
#define STM32_I2C_I2C3_DMA_ERROR_HOOK()     chSysHalt("I2C_I2C3_DMA_ERROR")

/*
 * ICU driver system settings.
 */
#define STM32_ICU_USE_TIM1                  FALSE
#define STM32_ICU_USE_TIM2                  FALSE
#define STM32_ICU_USE_TIM3                  FALSE
#define STM32_ICU_USE_TIM4                  FALSE
#define STM32_ICU_USE_TIM5                  FALSE
#define STM32_ICU_USE_TIM8                  FALSE
#define STM32_ICU_TIM1_IRQ_PRIORITY         7
#define STM32_ICU_TIM2_IRQ_PRIORITY         7
#define STM32_ICU_TIM3_IRQ_PRIORITY         7
#define STM32_ICU_TIM4_IRQ_PRIORITY         7
#define STM32_ICU_TIM5_IRQ_PRIORITY         7
#define STM32_ICU_TIM8_IRQ_PRIORITY         7

/*
 * PWM driver system settings.
 */
#define STM32_PWM_USE_ADVANCED              FALSE
#define STM32_PWM_USE_TIM1                  TRUE
#define STM32_PWM_USE_TIM2                  TRUE
#define STM32_PWM_USE_TIM3                  TRUE
#define STM32_PWM_USE_TIM4                  TRUE
#define STM32_PWM_USE_TIM5                  TRUE
#define STM32_PWM_USE_TIM8                  TRUE
#define STM32_PWM_TIM1_IRQ_PRIORITY         7
#define STM32_PWM_TIM2_IRQ_PRIORITY         7
#define STM32_PWM_TIM3_IRQ_PRIORITY         7
#define STM32_PWM_TIM4_IRQ_PRIORITY         7
#define STM32_PWM_TIM5_IRQ_PRIORITY         7
#define STM32_PWM_TIM8_IRQ_PRIORITY         7

/*
 * SERIAL driver system settings.
 */
#define STM32_SERIAL_USE_USART1             TRUE
#define STM32_SERIAL_USE_USART2             TRUE
#define STM32_SERIAL_USE_USART3             TRUE
#define STM32_SERIAL_USE_UART4              FALSE
#define STM32_SERIAL_USE_UART5              FALSE
#define STM32_SERIAL_USE_USART6             TRUE

#define STM32_SERIAL_USART1_PRIORITY        12
#define STM32_SERIAL_USART2_PRIORITY        12
#define STM32_SERIAL_USART3_PRIORITY        12
#define STM32_SERIAL_UART4_PRIORITY         12
#define STM32_SERIAL_UART5_PRIORITY         12
#define STM32_SERIAL_USART6_PRIORITY        12

/*
 * SPI driver system settings.
 */
#define STM32_SPI_USE_SPI1                  TRUE
#define STM32_SPI_SPI1_RX_DMA_STREAM        STM32_DMA_STREAM_ID(2, 2)
#define STM32_SPI_SPI1_TX_DMA_STREAM        STM32_DMA_STREAM_ID(2, 3)
#define STM32_SPI_SPI1_DMA_PRIORITY         1
#define STM32_SPI_SPI1_IRQ_PRIORITY         10

#define STM32_SPI_USE_SPI2                  FALSE
#define STM32_SPI_SPI2_RX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 3)
#define STM32_SPI_SPI2_TX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 4)
#define STM32_SPI_SPI2_DMA_PRIORITY         1
#define STM32_SPI_SPI2_IRQ_PRIORITY         2

#define STM32_SPI_USE_SPI3                  TRUE
#define STM32_SPI_SPI3_RX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 0)
#define STM32_SPI_SPI3_TX_DMA_STREAM        STM32_DMA_STREAM_ID(1, 7)
#define STM32_SPI_SPI3_DMA_PRIORITY         3
#define STM32_SPI_SPI3_IRQ_PRIORITY         3

#define STM32_SPI_DMA_ERROR_HOOK(spip)      chSysHalt("SPI_DMA_ERROR")

/*
 * UART driver system settings.
 */

#define STM32_UART_USE_USART1               TRUE
#define STM32_UART_USART1_RX_DMA_STREAM     STM32_DMA_STREAM_ID(2, 5)
#define STM32_UART_USART1_TX_DMA_STREAM     STM32_DMA_STREAM_ID(2, 7)
#define STM32_UART_USART1_IRQ_PRIORITY      12
#define STM32_UART_USART1_DMA_PRIORITY      0

#define STM32_UART_USE_USART2               FALSE
#define STM32_UART_USART2_RX_DMA_STREAM     STM32_DMA_STREAM_ID(1, 5)
#define STM32_UART_USART2_TX_DMA_STREAM     STM32_DMA_STREAM_ID(1, 6)
#define STM32_UART_USART2_IRQ_PRIORITY      12
#define STM32_UART_USART2_DMA_PRIORITY      0

#define STM32_UART_USE_USART3               TRUE
#define STM32_UART_USART3_RX_DMA_STREAM     STM32_DMA_STREAM_ID(1, 1)
#define STM32_UART_USART3_TX_DMA_STREAM     STM32_DMA_STREAM_ID(1, 3)
#define STM32_UART_USART3_IRQ_PRIORITY      12
#define STM32_UART_USART3_DMA_PRIORITY      0

#define STM32_UART_USE_USART6               FALSE
#define STM32_UART_USART6_RX_DMA_STREAM     STM32_DMA_STREAM_ID(2, 2)
#define STM32_UART_USART6_TX_DMA_STREAM     STM32_DMA_STREAM_ID(2, 7)
#define STM32_UART_USART6_IRQ_PRIORITY      12
#define STM32_UART_USART6_DMA_PRIORITY      0

#define STM32_UART_DMA_ERROR_HOOK(uartp)    chSysHalt("UART_DMA_ERROR")

/*
 * USB driver system settings.
 */
#define STM32_USB_USE_OTG1                  TRUE
#define STM32_USB_USE_OTG2                  FALSE
#define STM32_USB_OTG1_IRQ_PRIORITY         14
#define STM32_USB_OTG2_IRQ_PRIORITY         14
#define STM32_USB_OTG1_RX_FIFO_SIZE         512
#define STM32_USB_OTG2_RX_FIFO_SIZE         1024
#define STM32_USB_OTG_THREAD_STACK_SIZE     128
#define STM32_USB_OTGFIFO_FILL_BASEPRI      0
#define USE_INT_EP_MIDI                     0
#define USE_INT_EP_BULK                     0

#define DSP_CODEC_TIMESLICE                 3333
#define DSP_UI_MIDI_COST                    100
#define DSP_USB_AUDIO_FIRMWARE_COST         5
#define DSP_USB_AUDIO_STREAMING_COST        65
#define DSP_LIMIT200                        200

#define USE_EXTERNAL_USB_FIFO_PUMP          0
#define USE_BLOCKED_BULK_TX                 1
#define USB_USE_WAIT                        USE_BLOCKED_BULK_TX
#define USE_PATCH_DSPTIME_SMOOTHING_MS      0
// USB_AUDIO_CHANNELS must be 2 or 4
#define USB_AUDIO_CHANNELS                  4

/*
 * Thread priority settings. v1.0.12 settings in comments for reference.
 */
#define STM32_USB_OTG_THREAD_PRIO           HIGHPRIO     /* HIGHPRIO-2 */
#define PATCH_DSP_PRIO                      HIGHPRIO-1   /* HIGHPRIO-1 */
#define SPILINK_PRIO                        HIGHPRIO-1   /* HIGHPRIO-1 */
#define UI_USB_PRIO                         HIGHPRIO-2   /* NORMALPRIO */
#define MIDI_USB_PRIO                       HIGHPRIO-2   /* NORMALPRIO*/
#define USB_HOST_CONF_PRIO                  NORMALPRIO   /* HIGHPRIO-2 */
#define PATCH_NORMAL_PRIO                   NORMALPRIO   /* N/A */
#define SERIAL_MIDI_PRIO                    NORMALPRIO   /* NORMALPRIO */
#define SYSMON_PRIO                         NORMALPRIO   /* NORMALPRIO*/

/*
 * SDC settings.
 */
#define STM32_SDC_SDIO_DMA_STREAM           STM32_DMA_STREAM_ID(2, 6)

#endif /* _MCUCONF_H */