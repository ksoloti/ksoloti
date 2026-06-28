#############################################################################
# Build global options
# NOTE: Can be overridden externally.
#
BOARDDEF=

# Compiler options here.
ifeq ($(USE_OPT),)
  USE_OPT = \
    -D$(BOARDDEF) \
    --specs=nosys.specs \
    --specs=nano.specs \
    -Wno-implicit-fallthrough \
    -Wno-return-type \
    -Wno-unused-function \
    -Wno-unused-parameter \
    -O2 \
    -falign-functions=16 \
    -fno-math-errno \
    -fomit-frame-pointer \
    -ggdb3 \
    -mcpu=cortex-m4 \
    -mfloat-abi=hard \
    -mfpu=fpv4-sp-d16 \
    -mthumb \
    -mtune=cortex-m4 \
    -mword-relocations \
    -pipe 
endif 

# C specific options here (added to USE_OPT).
ifeq ($(USE_COPT),)
  USE_COPT = 
endif

# C++ specific options here (added to USE_OPT).
ifeq ($(USE_CPPOPT),)
  USE_CPPOPT = \
    -fno-rtti \
    -fno-threadsafe-statics
endif

# Enable this if you want the linker to remove unused code and data
ifeq ($(USE_LINK_GC),)
  USE_LINK_GC = yes
endif

# Linker extra options here.
ifeq ($(USE_LDOPT),)
  USE_LDOPT = 
endif

ifeq ($(USE_LTO),)
  USE_LTO = no
endif

# If enabled, this option allows to compile the application in THUMB mode.
ifeq ($(USE_THUMB),)
  USE_THUMB = yes
endif

# Enable this if you want to see the full log while compiling.
ifeq ($(USE_VERBOSE_COMPILE),)
  USE_VERBOSE_COMPILE = no
endif

# If enabled, this option makes the build process faster by not compiling
# modules not used in the current configuration.
ifeq ($(USE_SMART_BUILD),)
  USE_SMART_BUILD = yes
endif

#
# Build global options
##############################################################################

##############################################################################
# Architecture or project specific options
#

# Stack size to be allocated to the Cortex-M process stack. This stack is
# the stack used by the main() thread.
ifeq ($(USE_PROCESS_STACKSIZE),)
  USE_PROCESS_STACKSIZE = 0x400
endif

# Stack size to the allocated to the Cortex-M main/exceptions stack. This
# stack is used for processing interrupts and exceptions.
ifeq ($(USE_EXCEPTIONS_STACKSIZE),)
  USE_EXCEPTIONS_STACKSIZE = 0x400
endif

# Enables the use of FPU on Cortex-M4.
# Enable this if you really want to use the STM FWLib.
ifeq ($(USE_FPU),)
  USE_FPU = hard
endif

# FPU-related options.
ifeq ($(USE_FPU_OPT),)
  USE_FPU_OPT = -mfloat-abi=$(USE_FPU) -mfpu=fpv4-sp-d16
endif

# Enable this if you really want to use the STM FWLib.
ifeq ($(USE_FWLIB),)
  USE_FWLIB = no
endif

#
# Architecture or project specific options
##############################################################################

##############################################################################
# Project, sources and paths
#

# Define project name here
ifeq ($(BOARDDEF),BOARD_KSOLOTI_CORE)
  PROJECT = ksoloti_flasher
else ifeq ($(BOARDDEF),BOARD_AXOLOTI_CORE)
  PROJECT = axoloti_flasher
endif

# BUILDDIR = flasher_build/$(PROJECT)

# Imported source files and paths
CHIBIOS = ${axoloti_home}/chibios
CONFDIR  := .

include board.mk

# Licensing files.
include $(CHIBIOS)/os/license/license.mk
# Startup files.
include $(CHIBIOS)/os/common/startup/ARMCMx/compilers/GCC/mk/startup_stm32f4xx.mk
# HAL-OSAL files (optional).
include $(CHIBIOS)/os/hal/hal.mk
include $(CHIBIOS)/os/hal/ports/STM32/STM32F4xx/platform.mk
include $(CHIBIOS)/os/hal/osal/rt-nil/osal.mk
# RTOS files (optional).
include $(CHIBIOS)/os/rt/rt.mk
include $(CHIBIOS)/os/common/ports/ARMv7-M/compilers/GCC/mk/port.mk
# Other files (optional).
include $(CHIBIOS)/os/hal/lib/streams/streams.mk
include $(CHIBIOS)/os/various/shell/shell.mk
# FAT stuff
include $(CHIBIOS)/os/various/fatfs_bindings/fatfs.mk

# get the same binary as openOCD flashes from elf
override BIN = $(CP) -O binary --gap-fill 0xff
override HEX = $(CP) -O ihex --gap-fill 0xff
	
# Define linker script file here
LDSCRIPT= STM32F427xG_flasher.ld

# C sources that can be compiled in ARM or THUMB mode depending on the global
# setting.
CSRC = $(PORTSRC) \
      $(KERNSRC) \
      $(HALSRC) \
      $(PLATFORMSRC) \
      $(BOARDSRC) \
      $(CHIBIOS)/os/various/shell/shell.c \
      $(CHIBIOS)/os/hal/lib/streams/chprintf.c \
      $(CHIBIOS)/os/hal/lib/streams/memstreams.c \
      ../axoloti_board.c \
      ../sdram.c \
      ../crc32.c \
      ../flash.c \
      ../watchdog.c \
      ../stm32f4xx_fmc.c \
      main.c  

       
# C++ sources that can be compiled in ARM or THUMB mode depending on the global
# setting.
CPPSRC =

# C sources to be compiled in ARM mode regardless of the global setting.
# NOTE: Mixing ARM and THUMB mode enables the -mthumb-interwork compiler
#       option that results in lower performance and larger code size.
ACSRC =

# C++ sources to be compiled in ARM mode regardless of the global setting.
# NOTE: Mixing ARM and THUMB mode enables the -mthumb-interwork compiler
#       option that results in lower performance and larger code size.
ACPPSRC =

# C sources to be compiled in THUMB mode regardless of the global setting.
# NOTE: Mixing ARM and THUMB mode enables the -mthumb-interwork compiler
#       option that results in lower performance and larger code size.
TCSRC =

# C sources to be compiled in THUMB mode regardless of the global setting.
# NOTE: Mixing ARM and THUMB mode enables the -mthumb-interwork compiler
#       option that results in lower performance and larger code size.
TCPPSRC =

# List ASM source files here
ASMSRC =

# List ASM with Preprocessor source files here (.S files)
ASMXSRC = \
  $(STARTUPASM) \
  $(PORTASM)

INCDIR = ../ \
  $(CMSIS)/Core/Include \
  $(CMSIS)/DSP/Include \
  $(PORTINC) $(KERNINC) $(CONFDIR) $(ALLINC) $(TESTINC) \
  $(HALINC) $(PLATFORMINC) $(BOARDINC) \
  $(FATFSINC) $(OSALINC) \
  $(CHIBIOS) \
  $(CHIBIOS)/os/various \
  $(CHIBIOS)/os/common/ports/ARMv7-M/compilers/GCC \
  $(CHIBIOS)/os/common/startup/ARMCMx/devices/STM32F4xx \
  $(CHIBIOS)/os/ext/CMSIS/include \
  $(CHIBIOS)/os/ext/CMSIS/ST/STM32F4xx \
  $(CHIBIOS)/os/hal/lib/streams \
  ../STM32F4xx_HAL_Driver/Inc

#
# Project, sources and paths
##############################################################################

##############################################################################
# Compiler settings
#

MCU  = cortex-m4

TRGT = arm-none-eabi-
CC   = $(TRGT)gcc
CPPC = $(TRGT)g++
# Enable loading with g++ only if you need C++ runtime support.
# NOTE: You can use C++ even without C++ support if you are careful. C++
#       runtime support makes code size explode.
LD   = $(TRGT)gcc 
#LD   = $(TRGT)g++ 
CP   = $(TRGT)objcopy
AS   = $(TRGT)gcc -x assembler-with-cpp
OD   = $(TRGT)objdump
SZ   = $(TRGT)size
HEX  = $(CP) -O ihex
BIN  = $(CP) -O binary

# ARM-specific options here
AOPT =

# THUMB-specific options here
TOPT = \
    -mthumb \
    -DTHUMB

# Define C warning options here
CWARN = \
    -Wall \
    -Wextra \
    -Wstrict-prototypes \
    -Wundef

# Define C++ warning options here
CPPWARN = \
    -Wall \
    -Wextra \
    -Wundef

#
# Compiler settings
##############################################################################

##############################################################################
# Start of default section
#

# List all default C defines here, like -D_DEBUG=1
DDEFS =

# List all default ASM defines here, like -D_DEBUG=1
DADEFS =

# List all default directories to look for include files here
DINCDIR =

# List the default directory to look for the libraries here
DLIBDIR =

# List all default libraries here
DLIBS = 

#
# End of default section
##############################################################################

##############################################################################
# Start of user section
#

# List all user C define here, like -D_DEBUG=1
UDEFS = \
    -DARM_MATH_CM4 \
    -DCRT0_AREAS_NUMBER=0 \
    -DCRT1_AREAS_NUMBER=0 

# Define ASM defines here
UADEFS =

# List all user directories here
UINCDIR = 

# List the user directory to look for the libraries here
ULIBDIR = .

# List all user libraries here
ULIBS =


#
# End of user defines
##############################################################################

# RULESPATH = $(CHIBIOS)/os/common/ports/ARMCMx/compilers/GCC
# include $(RULESPATH)/rules.mk

include ../rules.mk

# Provide dummy references for the crt1.c hooks that crt0_v7m.S calls
override LDFLAGS += -Wl,--defsym=__cpu_init=0
override LDFLAGS += -Wl,--defsym=__init_ram_areas=0
override LDFLAGS += -Wl,--defsym=__late_init=0
override LDFLAGS += -Wl,--defsym=__default_exit=0

#
# Common rules
##############################################################################