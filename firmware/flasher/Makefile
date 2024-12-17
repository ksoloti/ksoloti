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

#
# Build global options
##############################################################################

##############################################################################
# Architecture or project specific options
#

# Enables the use of FPU on Cortex-M4.
# Enable this if you really want to use the STM FWLib.
ifeq ($(USE_FPU),)
  USE_FPU = hard
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

BUILDDIR = flasher_build

# Imported source files and paths
CHIBIOS = ${axoloti_release}/chibios

include board.mk
# Startup files.
include $(CHIBIOS)/os/common/ports/ARMCMx/compilers/GCC/mk/startup_stm32f4xx.mk
# HAL-OSAL files (optional).
include $(CHIBIOS)/os/hal/hal.mk
include $(CHIBIOS)/os/hal/ports/STM32/STM32F4xx/platform.mk
include $(CHIBIOS)/os/hal/osal/rt/osal.mk
# RTOS files (optional).
include $(CHIBIOS)/os/rt/rt.mk
include $(CHIBIOS)/os/rt/ports/ARMCMx/compilers/GCC/mk/port_v7m.mk
# FAT stuff
include $(CHIBIOS)/os/various/fatfs_bindings/fatfs.mk
	
	
# Define linker script file here
LDSCRIPT= STM32F427xG_flasher.ld

# C sources that can be compiled in ARM or THUMB mode depending on the global
# setting.
CSRC = $(STARTUPSRC) \
      $(PORTSRC) \
      $(KERNSRC) \
      $(HALSRC) \
      $(PLATFORMSRC) \
      $(BOARDSRC) \
      $(CHIBIOS)/os/various/shell.c \
      $(CHIBIOS)/os/various/memstreams.c \
      $(CHIBIOS)/os/hal/lib/streams/chprintf.c \
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
ASMSRC = $(STARTUPASM) \
        $(PORTASM)

INCDIR = ../ \
		 $(PORTINC) $(KERNINC) $(TESTINC) \
         $(HALINC) $(PLATFORMINC) $(BOARDINC) \
         $(FATFSINC) \
         $(CHIBIOS)/os/various \
         ../STM32F4xx_HAL_Driver/Inc 
INCDIR = ../ \
  $(CMSIS)/Core/Include \
  $(CMSIS)/DSP/Include \
        $(PORTINC) $(KERNINC) $(TESTINC) \
        $(HALINC) $(PLATFORMINC) $(BOARDINC) \
        $(FATFSINC) \
  $(FATFSINC) \
        $(OSALINC) \
  $(CHIBIOS) \
        $(CHIBIOS)/os/various \
  $(CHIBIOS)/os/common/ports/ARMCMx/compilers/GCC \
        $(CHIBIOS)/os/common/ports/ARMCMx/devices/STM32F4xx \
        $(CHIBIOS)/os/ext/CMSIS/include \
        $(CHIBIOS)/os/ext/CMSIS/ST/STM32F4xx \
        $(CHIBIOS)/os/hal/lib/streams \

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
    -Wstrict-prototypes

# Define C++ warning options here
CPPWARN = \
    -Wall \
    -Wextra

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
    -D__FPU_PRESENT \
    -DCRT1_AREAS_NUMBER=0 
# Define ASM defines here
UADEFS =

# List all user directories here
UINCDIR = 

# List the user directory to look for the libraries here
ULIBDIR = .

# List all user libraries here
ULIBS =


VPATH = .
#
# End of user defines
##############################################################################

RULESPATH = $(CHIBIOS)/os/common/ports/ARMCMx/compilers/GCC
include $(RULESPATH)/rules.mk
