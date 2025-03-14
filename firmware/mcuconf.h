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

// TODO - All clock stuff for H7 needs testing, when all working maybe look at doing the if differently

#if BOARD_KSOLOTI_CORE_H743

#define STM32H7xx_MCUCONF
#define STM32H743_MCUCONF

// USB_AUDIO_DATA_SECTION - need to find best location, is DMA used?
// LCD_DATA_SECTION - is this used
// KVP_DATA_SECTION - KVP need sorting out properly
// FAST_DATA_SECTION - not really needed as .bss is already in ram5

#define DEBUG_DATA_SECTION      __attribute__ ((section (".ram4"))) __attribute__ ((aligned (32)))
#define CODEC_DMA_SECTION       __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)))
#define USB_AUDIO_DATA_SECTION  __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)))
#define ADC_DMA_DATA_SECTION1   __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)))
#define ADC_DMA_DATA_SECTION3   __attribute__ ((section (".ram4"))) __attribute__ ((aligned (32)))
#define LCD_DATA_SECTION        __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)))
#define KVP_DATA_SECTION        __attribute__ ((section (".ram4"))) __attribute__ ((aligned (32)))
#define SPILINK_DMA_SECTION     __attribute__ ((section (".ram3"))) __attribute__ ((aligned (32)))
#define FAST_DATA_SECTION       __attribute__ ((section (".ram5"))) __attribute__ ((aligned (32)))
#define SDCARD_DATA_SECTION     __attribute__ ((section (".ram0nc"))) __attribute__ ((aligned (32)))
#define FRAMTEXT_CODE_SECTION   __attribute__ ((section (".framtext"))) 
/*
 * HAL driver system settings.
 */

/*
 * General settings.
 */
#define STM32_NO_INIT                       FALSE
#define STM32_TARGET_CORE                   1


/*
 * Memory attributes settings.
 */

#define STM32_NOCACHE_MPU_REGION            MPU_REGION_6
#define STM32_NOCACHE_RBAR                  0x30040000U
#define STM32_NOCACHE_ENABLE                TRUE
#define STM32_NOCACHE_RASR                  MPU_RASR_SIZE_32K

/*
 * PWR system settings.
 * Reading STM32 Reference Manual is required, settings in PWR_CR3 are
 * very critical.
 * Register constants are taken from the ST header.
 */

#define STM32_VOS                           STM32_VOS_SCALE1
#define STM32_PWR_CR1                       (PWR_CR1_SVOS_1 | PWR_CR1_SVOS_0)
#define STM32_PWR_CR2                       (PWR_CR2_BREN)
#define STM32_PWR_CR3                       (PWR_CR3_LDOEN | PWR_CR3_USB33DEN)
#define STM32_PWR_CPUCR                     0

/*
 * Clock tree static settings.
 * Reading STM32 Reference Manual is required.
 */
#define STM32_NO_INIT                       FALSE

#define STM32_HSI_ENABLED                   TRUE
#define STM32_LSI_ENABLED                   TRUE
#define STM32_CSI_ENABLED                   TRUE
#define STM32_HSI48_ENABLED                 TRUE
#define STM32_HSE_ENABLED                   TRUE
#define STM32_LSE_ENABLED                   FALSE
#define STM32_HSIDIV                        STM32_HSIDIV_DIV1


/*
 * PLLs static settings.
 * Reading STM32 Reference Manual is required.
 */
#define STM32_PLLSRC                        STM32_PLLSRC_HSE_CK
#define STM32_PLLCFGR_MASK                  ~0

#define STM32_PLL1_ENABLED                  TRUE
#define STM32_PLL1_P_ENABLED                TRUE
#define STM32_PLL1_Q_ENABLED                TRUE
#define STM32_PLL1_R_ENABLED                TRUE
#define STM32_PLL1_DIVM_VALUE               1
#define STM32_PLL1_DIVN_VALUE               120
#define STM32_PLL1_FRACN_VALUE              0
#define STM32_PLL1_DIVP_VALUE               2
#define STM32_PLL1_DIVQ_VALUE               20
#define STM32_PLL1_DIVR_VALUE               2

#define STM32_PLL2_ENABLED                  TRUE
#define STM32_PLL2_P_ENABLED                TRUE
#define STM32_PLL2_Q_ENABLED                TRUE
#define STM32_PLL2_R_ENABLED                TRUE
#define STM32_PLL2_DIVM_VALUE               1
#define STM32_PLL2_DIVN_VALUE               18
#define STM32_PLL2_FRACN_VALUE              6144
#define STM32_PLL2_DIVP_VALUE               2
#define STM32_PLL2_DIVQ_VALUE               2
#define STM32_PLL2_DIVR_VALUE               6

#define STM32_PLL3_ENABLED                  TRUE
#define STM32_PLL3_P_ENABLED                TRUE
#define STM32_PLL3_Q_ENABLED                TRUE
#define STM32_PLL3_R_ENABLED                TRUE
#define STM32_PLL3_DIVM_VALUE               32
#define STM32_PLL3_DIVN_VALUE               129
#define STM32_PLL3_FRACN_VALUE              0
#define STM32_PLL3_DIVP_VALUE               2
#define STM32_PLL3_DIVQ_VALUE               2
#define STM32_PLL3_DIVR_VALUE               2

/*
 * Core clocks dynamic settings (can be changed at runtime).
 * Reading STM32 Reference Manual is required.
 */
#define STM32_SW                            STM32_SW_PLL1_P_CK
#define STM32_RTCSEL                        STM32_RTCSEL_LSI_CK
#define STM32_D1CPRE                        STM32_D1CPRE_DIV1
#define STM32_D1HPRE                        STM32_D1HPRE_DIV2
#define STM32_D1PPRE3                       STM32_D1PPRE3_DIV2
#define STM32_D2PPRE1                       STM32_D2PPRE1_DIV2
#define STM32_D2PPRE2                       STM32_D2PPRE2_DIV2
#define STM32_D3PPRE4                       STM32_D3PPRE4_DIV2

/*
 * Peripherals clocks static settings.
 * Reading STM32 Reference Manual is required.
 */
#define STM32_MCO1SEL                       STM32_MCO1SEL_HSE_CK
#define STM32_MCO1PRE_VALUE                 1
#define STM32_MCO2SEL                       STM32_MCO2SEL_SYS_CK
#define STM32_MCO2PRE_VALUE                 5
#define STM32_TIMPRE_ENABLE                 TRUE
#define STM32_HRTIMSEL                      0
#define STM32_STOPKERWUCK                   0
#define STM32_STOPWUCK                      0
#define STM32_RTCPRE_VALUE                  8
#define STM32_CKPERSEL                      STM32_CKPERSEL_HSE_CK
#define STM32_SDMMCSEL                      STM32_SDMMCSEL_PLL2_R_CK
#define STM32_QSPISEL                       STM32_QSPISEL_HCLK
#define STM32_FMCSEL                        STM32_FMCSEL_HCLK
#define STM32_SWPSEL                        STM32_SWPSEL_PCLK1
#define STM32_FDCANSEL                      STM32_FDCANSEL_HSE_CK
#define STM32_DFSDM1SEL                     STM32_DFSDM1SEL_PCLK2
#define STM32_SPDIFSEL                      STM32_SPDIFSEL_PLL1_Q_CK
#define STM32_SPI45SEL                      STM32_SPI45SEL_PCLK2
#define STM32_SPI123SEL                     STM32_SPI123SEL_PLL1_Q_CK
#define STM32_SAI23SEL                      STM32_SAI23SEL_PLL1_Q_CK
#define STM32_SAI1SEL                       STM32_SAI1SEL_PLL1_Q_CK
#define STM32_LPTIM1SEL                     STM32_LPTIM1SEL_PCLK1
#define STM32_CECSEL                        STM32_CECSEL_LSE_CK
#define STM32_USBSEL                        STM32_USBSEL_PLL1_Q_CK
#define STM32_I2C123SEL                     STM32_I2C123SEL_PCLK1
#define STM32_RNGSEL                        STM32_RNGSEL_HSI48_CK
#define STM32_USART16SEL                    STM32_USART16SEL_PCLK2
#define STM32_USART234578SEL                STM32_USART234578SEL_PCLK1
#define STM32_SPI6SEL                       STM32_SPI6SEL_PCLK4
#define STM32_SAI4BSEL                      STM32_SAI4BSEL_PLL1_Q_CK
#define STM32_SAI4ASEL                      STM32_SAI4ASEL_PLL1_Q_CK
#define STM32_ADCSEL                        STM32_ADCSEL_PLL3_R_CK
#define STM32_LPTIM345SEL                   STM32_LPTIM345SEL_PCLK4
#define STM32_LPTIM2SEL                     STM32_LPTIM2SEL_PCLK4
#define STM32_I2C4SEL                       STM32_I2C4SEL_PCLK4
#define STM32_LPUART1SEL                    STM32_LPUART1SEL_PCLK4

/*
 * IRQ system settings.
 */
#define STM32_IRQ_EXTI0_PRIORITY            6
#define STM32_IRQ_EXTI1_PRIORITY            6
#define STM32_IRQ_EXTI2_PRIORITY            6
#define STM32_IRQ_EXTI3_PRIORITY            6
#define STM32_IRQ_EXTI4_PRIORITY            6
#define STM32_IRQ_EXTI5_9_PRIORITY          6
#define STM32_IRQ_EXTI10_15_PRIORITY        6
#define STM32_IRQ_EXTI16_PRIORITY           6
#define STM32_IRQ_EXTI17_PRIORITY           6
#define STM32_IRQ_EXTI18_PRIORITY           6
#define STM32_IRQ_EXTI19_PRIORITY           6
#define STM32_IRQ_EXTI20_21_PRIORITY        6

#define STM32_IRQ_FDCAN1_PRIORITY           10
#define STM32_IRQ_FDCAN2_PRIORITY           10
#define STM32_IRQ_MDMA_PRIORITY             9

#define STM32_IRQ_QUADSPI1_PRIORITY         10

#define STM32_IRQ_SDMMC1_PRIORITY           9
#define STM32_IRQ_SDMMC2_PRIORITY           9

#define STM32_IRQ_TIM1_UP_PRIORITY          7
#define STM32_IRQ_TIM1_CC_PRIORITY          7
#define STM32_IRQ_TIM2_PRIORITY             7
#define STM32_IRQ_TIM3_PRIORITY             7
#define STM32_IRQ_TIM4_PRIORITY             7
#define STM32_IRQ_TIM5_PRIORITY             7
#define STM32_IRQ_TIM6_PRIORITY             7
#define STM32_IRQ_TIM7_PRIORITY             7
#define STM32_IRQ_TIM8_BRK_TIM12_PRIORITY   7
#define STM32_IRQ_TIM8_UP_TIM13_PRIORITY    7
#define STM32_IRQ_TIM8_TRGCO_TIM14_PRIORITY 7
#define STM32_IRQ_TIM8_CC_PRIORITY          7
#define STM32_IRQ_TIM15_PRIORITY            7
#define STM32_IRQ_TIM16_PRIORITY            7
#define STM32_IRQ_TIM17_PRIORITY            7

#define STM32_IRQ_USART1_PRIORITY           12
#define STM32_IRQ_USART2_PRIORITY           12
#define STM32_IRQ_USART3_PRIORITY           12
#define STM32_IRQ_UART4_PRIORITY            12
#define STM32_IRQ_UART5_PRIORITY            12
#define STM32_IRQ_USART6_PRIORITY           12
#define STM32_IRQ_UART7_PRIORITY            12
#define STM32_IRQ_UART8_PRIORITY            12
#define STM32_IRQ_LPUART1_PRIORITY          12


#define STM32_ADC_DUAL_MODE                 FALSE
#define STM32_ADC_COMPACT_SAMPLES           FALSE
#define STM32_ADC_USE_ADC12                 TRUE
#define STM32_ADC_USE_ADC3                  TRUE
#define STM32_ADC_ADC12_DMA_STREAM          STM32_DMA_STREAM_ID_ANY
#define STM32_ADC_ADC3_BDMA_STREAM          STM32_BDMA_STREAM_ID_ANY
#define STM32_ADC_ADC12_DMA_PRIORITY        2
#define STM32_ADC_ADC3_DMA_PRIORITY         2
#define STM32_ADC_ADC12_IRQ_PRIORITY        5
#define STM32_ADC_ADC3_IRQ_PRIORITY         5
#define STM32_ADC_ADC12_CLOCK_MODE          ADC_CCR_CKMODE_AHB_DIV4
#define STM32_ADC_ADC3_CLOCK_MODE           ADC_CCR_CKMODE_AHB_DIV4

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
#define STM32_GPT_USE_TIM4                  TRUE
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
#define STM32_PWM_USE_TIM4                  FALSE
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
 * SDC settings
 */
#define STM32_SDC_SDIO_DMA_STREAM           STM32_DMA_STREAM_ID(2, 6)
#define STM32_SDC_USE_SDMMC1                TRUE
#define STM32_SDC_USE_SDMMC2                FALSE
#define STM32_SDC_SDMMC_UNALIGNED_SUPPORT   TRUE
#define STM32_SDC_SDMMC_WRITE_TIMEOUT       1000000
#define STM32_SDC_SDMMC_READ_TIMEOUT        1000000
#define STM32_SDC_SDMMC_CLOCK_DELAY         10
#define STM32_SDC_SDMMC_PWRSAV              TRUE

#else // BOARD_KSOLOTI_CORE_H743
/* MCU configuration is the same for Ksoloti and Axoloti boards
 * and for all firmware flavours (so far) 
 */

#define STM32F4xx_MCUCONF
#define STM32F427_MCUCONF
#define STM32F429_MCUCONF
#define STM32F437_MCUCONF
#define STM32F439_MCUCONF


#define DEBUG_DATA_SECTION      __attribute__ ((section (".sram3")))
#define CODEC_DMA_SECTION       __attribute__ ((section (".sram2")))
#define USB_AUDIO_DATA_SECTION  __attribute__ ((section (".sram2")))
#define ADC_DMA_DATA_SECTION1   __attribute__ ((section (".sram2")))
#define ADC_DMA_DATA_SECTION4   __attribute__ ((section (".sram2")))
#define LCD_DATA_SECTION        __attribute__ ((section (".sram2")))
#define KVP_DATA_SECTION        __attribute__ ((section (".sram2")))
#define SPILINK_DATA_SECTION    __attribute__ ((section (".sram2")))
#define FAST_DATA_SECTION       __attribute__ ((section (".ccmramend")))
#define SDCARD_DATA_SECTION     __attribute__ ((section (".sram3")))
#define FRAMTEXT_CODE_SECTION   __attribute__ ((section (".framtext"))) 
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
 * IRQ system settings.
 */
#define STM32_IRQ_EXTI0_PRIORITY            6
#define STM32_IRQ_EXTI1_PRIORITY            6
#define STM32_IRQ_EXTI2_PRIORITY            6
#define STM32_IRQ_EXTI3_PRIORITY            6
#define STM32_IRQ_EXTI4_PRIORITY            6
#define STM32_IRQ_EXTI5_9_PRIORITY          6
#define STM32_IRQ_EXTI10_15_PRIORITY        6
#define STM32_IRQ_EXTI16_PRIORITY           6
#define STM32_IRQ_EXTI17_PRIORITY           15
#define STM32_IRQ_EXTI18_PRIORITY           6
#define STM32_IRQ_EXTI19_PRIORITY           6
#define STM32_IRQ_EXTI20_PRIORITY           6
#define STM32_IRQ_EXTI21_PRIORITY           15
#define STM32_IRQ_EXTI22_PRIORITY           15

#define STM32_IRQ_TIM1_BRK_TIM9_PRIORITY    7
#define STM32_IRQ_TIM1_UP_TIM10_PRIORITY    7
#define STM32_IRQ_TIM1_TRGCO_TIM11_PRIORITY 7
#define STM32_IRQ_TIM1_CC_PRIORITY          7
#define STM32_IRQ_TIM2_PRIORITY             7
#define STM32_IRQ_TIM3_PRIORITY             7
#define STM32_IRQ_TIM4_PRIORITY             7
#define STM32_IRQ_TIM5_PRIORITY             7
#define STM32_IRQ_TIM6_PRIORITY             7
#define STM32_IRQ_TIM7_PRIORITY             7
#define STM32_IRQ_TIM8_BRK_TIM12_PRIORITY   7
#define STM32_IRQ_TIM8_UP_TIM13_PRIORITY    7
#define STM32_IRQ_TIM8_TRGCO_TIM14_PRIORITY 7
#define STM32_IRQ_TIM8_CC_PRIORITY          7

#define STM32_IRQ_USART1_PRIORITY           12
#define STM32_IRQ_USART2_PRIORITY           12
#define STM32_IRQ_USART3_PRIORITY           12
#define STM32_IRQ_UART4_PRIORITY            12
#define STM32_IRQ_UART5_PRIORITY            12
#define STM32_IRQ_USART6_PRIORITY           12
#define STM32_IRQ_UART7_PRIORITY            12
#define STM32_IRQ_UART8_PRIORITY            12

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
#define STM32_SPI_SPI2_IRQ_PRIORITY         3

#ifdef FW_I2SCODEC
#define STM32_SPI_USE_SPI3                  FALSE
#else
#define STM32_SPI_USE_SPI3                  TRUE
#endif

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
#endif // BOARD_KSOLOTI_CORE_H743
#endif /* _MCUCONF_H */