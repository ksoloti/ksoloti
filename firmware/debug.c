
#include <string.h>
#include <stdarg.h>
#include <stdint.h>
#include "ch.h"
#include "hal.h"
#include "chprintf.h"


#if ENABLE_SERIAL_DEBUG
void FRAMTEXT_CODE_SECTION LogUartMessage(const char *format, ...)
{
  va_list ap;
  va_start(ap, format);
  chvprintf((BaseSequentialStream *)&SD2, format, ap);
  va_end(ap);
}
void FRAMTEXT_CODE_SECTION LogUartMessageEol(const char *format, ...)
{
  va_list ap;
  va_start(ap, format);
  chvprintf((BaseSequentialStream *)&SD2, format, ap);
  va_end(ap);
  chprintf((BaseSequentialStream *)&SD2, "\n");
}
#else
void FRAMTEXT_CODE_SECTION LogUartMessage(const char *format, ...)
{
}
void FRAMTEXT_CODE_SECTION LogUartMessageEol(const char *format, ...)
{
}
#endif


// Exception code from https://forum.chibios.org/memberlist.php?mode=viewprofile&u=783 at https://forum.chibios.org/viewtopic.php?t=3819#p28370

#if ENABLE_EXCPT_DUMP

/**
 * Executes the BKPT instruction that causes the debugger to stop.
 * If no debugger is attached, this will be ignored
 */
#define bkpt() __asm volatile("BKPT #0\n")

void FRAMTEXT_CODE_SECTION NMI_Handler(void)
{
  // TODO
  while (1)
    ;
}

// See http://infocenter.arm.com/help/topic/com.arm.doc.dui0552a/BABBGBEC.html
typedef enum
{
  Reset = 1,
  NMI = 2,
  HardFault = 3,
  MemManage = 4,
  BusFault = 5,
  UsageFault = 6,
} FaultType;

char exceptionstr[50];

#if BOARD_KSOLOTI_CORE_H743
  void FRAMTEXT_CODE_SECTION sendchar(char c)
  {
    while (!(USART2->ISR & USART_ISR_TXE_TXFNF))
      ;
    USART2->TDR = (c);
  }
#else
  void FRAMTEXT_CODE_SECTION sendchar(char c)
  {
      while (!(USART2->SR & USART_SR_TXE));
        USART2->DR = ( c );
  }
#endif

void FRAMTEXT_CODE_SECTION exception_dump(char *s)
{
  do
  {
    sendchar(*s);
  } while (*s++);
}

void FRAMTEXT_CODE_SECTION faulttype(FaultType type)
{

  switch (type)
  {
  case NMI:
    exception_dump("NMI");
    break;

  case Reset:
    exception_dump("Reset");
    break;

  case BusFault:
    exception_dump("Bus Fault");
    break;

  case HardFault:
    exception_dump("Hard Fault");
    break;

  case UsageFault:
    exception_dump("Usage Fault");
    break;

  case MemManage:
    exception_dump("Memory Manager");
    break;
  }
}

void FRAMTEXT_CODE_SECTION hex2string(uint32_t hex)
{
  uint8_t i;
  char ascii = 0x0;
  uint32_t divider = 0x10000000;

  exception_dump("0x");

  for (i = 0; i < 8; i++)
  {
    ascii = hex / divider;
    hex -= (ascii * divider);
    divider /= 0x10;

    switch (ascii)
    {
    case 0xf:
      sendchar('F');
      break;

    case 0xe:
      sendchar('E');
      break;

    case 0xd:
      sendchar('D');
      break;

    case 0xc:
      sendchar('C');
      break;

    case 0xb:
      sendchar('B');
      break;

    case 0xa:
      sendchar('A');
      break;

    default:
      ascii += 0x30;
      sendchar(ascii);
      break;
    }
  }
}

void __attribute__((optimize("-O0"))) FRAMTEXT_CODE_SECTION HardFault_Handler(void)
{
  // Copy to local variables (not pointers) to allow GDB "i loc" to directly show the info
  // Get thread context. Contains main registers including PC and LR
  struct port_extctx ctx;
  memcpy(&ctx, (void *)__get_PSP(), sizeof(struct port_extctx));
  (void)ctx;
  // Interrupt status register: Which interrupt have we encountered, e.g. HardFault?
  FaultType faultType = (FaultType)__get_IPSR();
  (void)faultType;
  // For HardFault/BusFault this is the address that was accessed causing the error
  uint32_t faultAddress = SCB->BFAR;
  (void)faultAddress;
  // Flags about hardfault / busfault
  // See http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.dui0552a/Cihdjcfc.html for reference
  bool isFaultPrecise = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 1) ? true : false);
  bool isFaultImprecise = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 2) ? true : false);
  bool isFaultOnUnstacking = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 3) ? true : false);
  bool isFaultOnStacking = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 4) ? true : false);
  bool isFaultAddressValid = ((SCB->CFSR >> SCB_CFSR_BUSFAULTSR_Pos) & (1 << 7) ? true : false);
  (void)isFaultPrecise;
  (void)isFaultImprecise;
  (void)isFaultOnUnstacking;
  (void)isFaultOnStacking;
  (void)isFaultAddressValid;
  // Output some debug info about the expection
  exception_dump("********** Exception Dump **********\r\n");

  if (isFaultPrecise)
  {
    exception_dump("Fault Precise     : TRUE\r\n");
  }
  else
  {
    exception_dump("Fault Precise     : FALSE\r\n");
  }

  if (isFaultImprecise)
  {
    exception_dump("Fault Imprecise   : TRUE\r\n");
  }
  else
  {
    exception_dump("Fault Imprecise   : FALSE\r\n");
  }

  if (isFaultOnStacking)
  {
    exception_dump("Fault Onstacking  : TRUE\r\n");
  }
  else
  {
    exception_dump("Fault Onstacking  : FALSE\r\n");
  }

  if (isFaultOnUnstacking)
  {
    exception_dump("Fault Unstacking  : TRUE\r\n");
  }
  else
  {
    exception_dump("Fault Unstacking  : FALSE\r\n");
  }

  if (isFaultAddressValid)
  {
    exception_dump("Fault Valid Addr  : TRUE\r\n");
  }
  else
  {
    exception_dump("Fault Valid Addr  : FALSE\r\n");
  }

  exception_dump("Fault Addr        : ");
  hex2string(faultAddress);
  exception_dump("\r\n");

  exception_dump("Fault Type        : ");
  faulttype(faultType);
  exception_dump("\r\n");

  exception_dump("Context PC        : ");
  hex2string((uint32_t)ctx.pc);
  exception_dump("\r\n");

  exception_dump("Context Thread    : ");
  hex2string((uint32_t)ctx.lr_thd);
  exception_dump("\r\n");

  // Cause debugger to stop. Ignored if no debugger is attached
  bkpt();
  NVIC_SystemReset();
}

void FRAMTEXT_CODE_SECTION BusFault_Handler(void) __attribute__((alias("HardFault_Handler")));

void __attribute__((optimize("-O0"))) FRAMTEXT_CODE_SECTION UsageFault_Handler(void)
{
  // Copy to local variables (not pointers) to allow GDB "i loc" to directly show the info
  // Get thread context. Contains main registers including PC and LR
  struct port_extctx ctx;
  memcpy(&ctx, (void *)__get_PSP(), sizeof(struct port_extctx));
  (void)ctx;
  // Interrupt status register: Which interrupt have we encountered, e.g. HardFault?
  FaultType faultType = (FaultType)__get_IPSR();
  (void)faultType;
  // Flags about hardfault / busfault
  // See http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.dui0552a/Cihdjcfc.html for reference
  bool isUndefinedInstructionFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 0) ? true : false);
  bool isEPSRUsageFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 1) ? true : false);
  bool isInvalidPCFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 2) ? true : false);
  bool isNoCoprocessorFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 3) ? true : false);
  bool isUnalignedAccessFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 8) ? true : false);
  bool isDivideByZeroFault = ((SCB->CFSR >> SCB_CFSR_USGFAULTSR_Pos) & (1 << 9) ? true : false);
  (void)isUndefinedInstructionFault;
  (void)isEPSRUsageFault;
  (void)isInvalidPCFault;
  (void)isNoCoprocessorFault;
  (void)isUnalignedAccessFault;
  (void)isDivideByZeroFault;
  bkpt();
  NVIC_SystemReset();
}

void __attribute__((optimize("-O0"))) FRAMTEXT_CODE_SECTION MemManage_Handler(void)
{
  // Copy to local variables (not pointers) to allow GDB "i loc" to directly show the info
  // Get thread context. Contains main registers including PC and LR
  struct port_extctx ctx;
  memcpy(&ctx, (void *)__get_PSP(), sizeof(struct port_extctx));
  (void)ctx;
  // Interrupt status register: Which interrupt have we encountered, e.g. HardFault?
  FaultType faultType = (FaultType)__get_IPSR();
  (void)faultType;
  // For HardFault/BusFault this is the address that was accessed causing the error
  uint32_t faultAddress = SCB->MMFAR;
  (void)faultAddress;
  // Flags about hardfault / busfault
  // See http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.dui0552a/Cihdjcfc.html for reference
  bool isInstructionAccessViolation = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 0) ? true : false);
  bool isDataAccessViolation = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 1) ? true : false);
  bool isExceptionUnstackingFault = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 3) ? true : false);
  bool isExceptionStackingFault = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 4) ? true : false);
  bool isFaultAddressValid = ((SCB->CFSR >> SCB_CFSR_MEMFAULTSR_Pos) & (1 << 7) ? true : false);
  (void)isInstructionAccessViolation;
  (void)isDataAccessViolation;
  (void)isExceptionUnstackingFault;
  (void)isExceptionStackingFault;
  (void)isFaultAddressValid;
  bkpt();
  NVIC_SystemReset();
}
#endif
