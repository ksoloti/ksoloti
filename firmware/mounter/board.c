#include "ch.h"
#include "hal.h"
#include "stm32_rcc.h"
#include <string.h>

/**
 * @brief   PAL setup.
 * @details Digital I/O ports static configuration as defined in @p board.h.
 *          This variable is used by the HAL when initializing the PAL driver.
 */


 #define AHB1_EN_MASK    STM32_GPIO_EN_MASK
 #define AHB1_LPEN_MASK  AHB1_EN_MASK
 
 /**
  * @brief   GPIO port setup info.
  */
 typedef struct {
   /** Initial value for MODER register.*/
   uint32_t              moder;
   /** Initial value for OTYPER register.*/
   uint32_t              otyper;
   /** Initial value for OSPEEDR register.*/
   uint32_t              ospeedr;
   /** Initial value for PUPDR register.*/
   uint32_t              pupdr;
   /** Initial value for ODR register.*/
   uint32_t              odr;
   /** Initial value for AFRL register.*/
   uint32_t              afrl;
   /** Initial value for AFRH register.*/
   uint32_t              afrh;
 } stm32_gpio_setup_t;
 
 /**
  * @brief   STM32 GPIO static initializer.
  * @details An instance of this structure must be passed to @p palInit() at
  *          system startup time in order to initialize the digital I/O
  *          subsystem. This represents only the initial setup, specific pads
  *          or whole ports can be reprogrammed at later time.
  */
 typedef struct {
 #if STM32_HAS_GPIOA || defined(__DOXYGEN__)
   /** @brief Port A setup data.*/
   stm32_gpio_setup_t    PAData;
 #endif
 #if STM32_HAS_GPIOB || defined(__DOXYGEN__)
   /** @brief Port B setup data.*/
   stm32_gpio_setup_t    PBData;
 #endif
 #if STM32_HAS_GPIOC || defined(__DOXYGEN__)
   /** @brief Port C setup data.*/
   stm32_gpio_setup_t    PCData;
 #endif
 #if STM32_HAS_GPIOD || defined(__DOXYGEN__)
   /** @brief Port D setup data.*/
   stm32_gpio_setup_t    PDData;
 #endif
 #if STM32_HAS_GPIOE || defined(__DOXYGEN__)
   /** @brief Port E setup data.*/
   stm32_gpio_setup_t    PEData;
 #endif
 #if STM32_HAS_GPIOF || defined(__DOXYGEN__)
   /** @brief Port F setup data.*/
   stm32_gpio_setup_t    PFData;
 #endif
 #if STM32_HAS_GPIOG || defined(__DOXYGEN__)
   /** @brief Port G setup data.*/
   stm32_gpio_setup_t    PGData;
 #endif
 #if STM32_HAS_GPIOH || defined(__DOXYGEN__)
   /** @brief Port H setup data.*/
   stm32_gpio_setup_t    PHData;
 #endif
 #if STM32_HAS_GPIOI || defined(__DOXYGEN__)
   /** @brief Port I setup data.*/
   stm32_gpio_setup_t    PIData;
 #endif
 #if STM32_HAS_GPIOJ || defined(__DOXYGEN__)
   /** @brief Port I setup data.*/
   stm32_gpio_setup_t    PJData;
 #endif
 #if STM32_HAS_GPIOK || defined(__DOXYGEN__)
   /** @brief Port I setup data.*/
   stm32_gpio_setup_t    PKData;
 #endif
 } PALConfig;
 
 
 const PALConfig pal_default_config = 
 {
   {VAL_GPIOA_MODER, VAL_GPIOA_OTYPER, VAL_GPIOA_OSPEEDR, VAL_GPIOA_PUPDR,
     VAL_GPIOA_ODR, VAL_GPIOA_AFRL, VAL_GPIOA_AFRH},
   {VAL_GPIOB_MODER, VAL_GPIOB_OTYPER, VAL_GPIOB_OSPEEDR, VAL_GPIOB_PUPDR,
     VAL_GPIOB_ODR, VAL_GPIOB_AFRL, VAL_GPIOB_AFRH},
   {VAL_GPIOC_MODER, VAL_GPIOC_OTYPER, VAL_GPIOC_OSPEEDR, VAL_GPIOC_PUPDR,
     VAL_GPIOC_ODR, VAL_GPIOC_AFRL, VAL_GPIOC_AFRH},
   {VAL_GPIOD_MODER, VAL_GPIOD_OTYPER, VAL_GPIOD_OSPEEDR, VAL_GPIOD_PUPDR,
     VAL_GPIOD_ODR, VAL_GPIOD_AFRL, VAL_GPIOD_AFRH},
   {VAL_GPIOE_MODER, VAL_GPIOE_OTYPER, VAL_GPIOE_OSPEEDR, VAL_GPIOE_PUPDR,
     VAL_GPIOE_ODR, VAL_GPIOE_AFRL, VAL_GPIOE_AFRH},
   {VAL_GPIOF_MODER, VAL_GPIOF_OTYPER, VAL_GPIOF_OSPEEDR, VAL_GPIOF_PUPDR,
     VAL_GPIOF_ODR, VAL_GPIOF_AFRL, VAL_GPIOF_AFRH},
   {VAL_GPIOG_MODER, VAL_GPIOG_OTYPER, VAL_GPIOG_OSPEEDR, VAL_GPIOG_PUPDR,
     VAL_GPIOG_ODR, VAL_GPIOG_AFRL, VAL_GPIOG_AFRH},
   {VAL_GPIOH_MODER, VAL_GPIOH_OTYPER, VAL_GPIOH_OSPEEDR, VAL_GPIOH_PUPDR,
     VAL_GPIOH_ODR, VAL_GPIOH_AFRL, VAL_GPIOH_AFRH},
   {VAL_GPIOI_MODER, VAL_GPIOI_OTYPER, VAL_GPIOI_OSPEEDR, VAL_GPIOI_PUPDR,
     VAL_GPIOI_ODR, VAL_GPIOI_AFRL, VAL_GPIOI_AFRH}
 };
 
 
 static void initgpio(stm32_gpio_t *gpiop, const stm32_gpio_setup_t *config) 
 {
   gpiop->OTYPER  = config->otyper;
   gpiop->OSPEEDR = config->ospeedr;
   gpiop->PUPDR   = config->pupdr;
   gpiop->ODR     = config->odr;
   gpiop->AFRL    = config->afrl;
   gpiop->AFRH    = config->afrh;
   gpiop->MODER   = config->moder;
 }
 
 
 void stm32_gpio_init(void) 
 {
   const PALConfig *config = &pal_default_config;
 
   /*
    * Enables the GPIO related clocks.
    */
 #if defined(STM32L0XX)
   RCC->IOPENR |= AHB_EN_MASK;
   RCC->IOPSMENR |= AHB_LPEN_MASK;
 #elif defined(STM32L1XX)
   rccEnableAHB(AHB_EN_MASK, TRUE);
   RCC->AHBLPENR |= AHB_LPEN_MASK;
 #elif defined(STM32F0XX)
   rccEnableAHB(AHB_EN_MASK, TRUE);
 #elif defined(STM32F3XX) || defined(STM32F37X)
   rccEnableAHB(AHB_EN_MASK, TRUE);
 #elif defined(STM32F2XX) || defined(STM32F4XX) || defined(STM32F7XX)
   RCC->AHB1ENR   |= AHB1_EN_MASK;
   RCC->AHB1LPENR |= AHB1_LPEN_MASK;
 #endif
 
   /*
    * Initial GPIO setup.
    */
 #if STM32_HAS_GPIOA
   initgpio(GPIOA, &config->PAData);
 #endif
 #if STM32_HAS_GPIOB
   initgpio(GPIOB, &config->PBData);
 #endif
 #if STM32_HAS_GPIOC
   initgpio(GPIOC, &config->PCData);
 #endif
 #if STM32_HAS_GPIOD
   initgpio(GPIOD, &config->PDData);
 #endif
 #if STM32_HAS_GPIOE
   initgpio(GPIOE, &config->PEData);
 #endif
 #if STM32_HAS_GPIOF
   initgpio(GPIOF, &config->PFData);
 #endif
 #if STM32_HAS_GPIOG
   initgpio(GPIOG, &config->PGData);
 #endif
 #if STM32_HAS_GPIOH
   initgpio(GPIOH, &config->PHData);
 #endif
 #if STM32_HAS_GPIOI
   initgpio(GPIOI, &config->PIData);
 #endif
 #if STM32_HAS_GPIOJ
   initgpio(GPIOJ, &config->PJData);
 #endif
 #if STM32_HAS_GPIOK
   initgpio(GPIOK, &config->PKData);
 #endif
 }
 
/**
 * @brief   Early initialization code.
 * @details This initialization must be performed just after stack setup
 *          and before any other initialization.
 */
void __early_init(void)
{
  /* Reset of all peripherals.*/
  rccResetAHB1(~0);
  rccResetAHB2(~0);
  rccResetAPB1(~0);
  NVIC->ICER[0] = 0xFFFFFFFF;
  NVIC->ICER[1] = 0xFFFFFFFF;
  NVIC->ICER[2] = 0xFFFFFFFF;
  NVIC->ICER[3] = 0xFFFFFFFF;
  NVIC->ICER[4] = 0xFFFFFFFF;
  NVIC->ICER[5] = 0xFFFFFFFF;
  NVIC->ICER[6] = 0xFFFFFFFF;
  NVIC->ICER[7] = 0xFFFFFFFF;
  rccResetAPB2(~0x10000000); //RCC_APB1RSTR_PWRRST
  OTG_HS->GINTMSK = 0; // disable OTG_HS interrupts!
  stm32_gpio_init();
  stm32_clock_init();
}

#if HAL_USE_SDC || defined(__DOXYGEN__)
/**
 * @brief   SDC card detection.
 */
bool_t sdc_lld_is_card_inserted(SDCDriver *sdcp)
{
    (void)sdcp;
    /* TODO: Fill the implementation.*/
    return TRUE;
}

/**
 * @brief   SDC card write protection detection.
 */
bool_t sdc_lld_is_write_protected(SDCDriver *sdcp)
{
    (void)sdcp;
    /* TODO: Fill the implementation.*/
    return FALSE;
}
#endif /* HAL_USE_SDC */

/**
 * @brief   Board-specific initialization code.
 * @todo    Add your board-specific code, if any.
 */
void boardInit(void)
{
}

void STM32_OTG2_HANDLER(void)
{
    /* catch spurious interrupts... */
}
