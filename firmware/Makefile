#############################################################################
# Build global options
# NOTE: Can be overridden externally.
#
BOARDDEF=
SUBBOARDDEF=
FWOPTIONDEF=
USE_VERBOSE_COMPILE=no
INBUILT_MOUNTER_FLASHER=yes

$(info Compiling for $(SUBBOARDDEF))

# Compiler options here.


# O3 would turn on the six additional flags below:
# These six flags, used alone or in combination, may break the firmware!

#-funswitch-loops
#-finline-functions
#-fipa-cp-clone
#-fpredictive-commoning
#-fgcse-after-reload
#-ftree-vectorize


ifeq ($(USE_OPT),)
  USE_OPT = \
    --specs=nano.specs \
    --specs=nosys.specs \
    -O2 \
    -Wno-implicit-fallthrough \
    -Wno-return-type \
    -Wno-unused-function \
    -Wno-unused-parameter \
    -falign-functions=16 \
    -fcode-hoisting \
    -ffast-math \
    -ffp-contract=off \
    -fno-common \
    -fno-math-errno \
    -fno-reorder-blocks \
    -fno-signed-zeros \
    -fno-unsafe-math-optimizations \
    -fomit-frame-pointer \
    -ggdb3 \
    -mfloat-abi=hard \
    -mno-thumb-interwork \
    -mthumb \
    -mword-relocations \
    -fno-delete-null-pointer-checks
endif

ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h747
  USE_OPT +=  -mcpu=cortex-m7 \
              -mfpu=fpv5-sp-d16 \
              -mtune=cortex-m7 
  MCU  = cortex-m7
else
  # Axoloti and Ksoloti f427
  USE_OPT +=  -mcpu=cortex-m4 \
              -mfpu=fpv4-sp-d16 \
              -mtune=cortex-m4 

  MCU  = cortex-m4

endif

ifneq ($(FWOPTIONDEF),)
  USE_OPT := $(USE_OPT) -D$(FWOPTIONDEF)
endif

# C specific options here (added to USE_OPT).
ifeq ($(USE_COPT),)
  USE_COPT = 
endif

# C++ specific options here (added to USE_OPT).
ifeq ($(USE_CPPOPT),)
  USE_CPPOPT = \
    -fno-rtti \
    -fno-threadsafe-statics \
    -fno-use-cxa-atexit \
    -std=c++11
endif

# Enable this if you want the linker to remove unused code and data
ifeq ($(USE_LINK_GC),)
  USE_LINK_GC = no
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

# Enables the use of FPU on Cortex-M4 (no, softfp, hard).
ifeq ($(USE_FPU),)
  USE_FPU = hard
endif

#Axoloti optional modules, here we define defaults if none supplied
ifeq ($(AXO_OPT_MODULES),)
	AXO_OPT_MODULES = BRAIDS RINGS CLOUDS STREAMS ELEMENTS WARPS
endif


#
# Architecture or project specific options
##############################################################################

##############################################################################
# Project, sources and paths
#

# Define project name here
ifeq ($(BOARDDEF),BOARD_KSOLOTI_CORE)
  ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_F427)
    PROJECT = ksoloti
  else ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
    PROJECT = ksoloti_h743
  endif
else ifeq ($(BOARDDEF),BOARD_AXOLOTI_CORE)
  PROJECT = axoloti
endif

ifeq ($(FWOPTIONDEF),FW_SPILINK)
  PROJECT := $(PROJECT)_spilink
endif

ifeq ($(FWOPTIONDEF),FW_USBAUDIO)
  PROJECT := $(PROJECT)_usbaudio
endif

ifeq ($(FWOPTIONDEF),FW_I2SCODEC)
  PROJECT := $(PROJECT)_i2scodec
endif

# Imported source files and paths
axoloti_home ?= ..
axoloti_firmware ?= ../firmware

EMPTY := 
SPACE := $(EMPTY) $(EMPTY)

HOME     = $(subst $(SPACE),\ ,${axoloti_home})
FIRMWARE = $(subst $(SPACE),\ ,${axoloti_firmware})

CHIBIOS             = ${HOME}/chibios
CMSIS               = ${HOME}/CMSIS
MUTABLE_INSTRUMENTS = ${FIRMWARE}/mutable_instruments



ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h747

  include board_h7.mk

  # Licensing files.
  include $(CHIBIOS)/os/license/license.mk
  # Startup files.
  include $(CHIBIOS)/os/common/startup/ARMCMx/compilers/GCC/mk/startup_stm32h7xx.mk
  # HAL-OSAL files (optional).
  include $(CHIBIOS)/os/hal/hal.mk
  include $(CHIBIOS)/os/hal/ports/STM32/STM32H7xx/platform.mk
  include $(CHIBIOS)/os/hal/osal/rt-nil/osal.mk
  # RTOS files (optional).
  include $(CHIBIOS)/os/rt/rt.mk
  include $(CHIBIOS)/os/common/ports/ARMv7-M/compilers/GCC/mk/port.mk
  # FAT stuff
  include $(CHIBIOS)/os/various/fatfs_bindings/fatfs.mk
  include $(CHIBIOS)/os/various/shell/shell.mk
else
  # Axoloti and Ksoloti f427

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
  # FAT stuff
  include $(CHIBIOS)/os/various/fatfs_bindings/fatfs.mk
  include $(CHIBIOS)/os/various/shell/shell.mk
endif

# get the same binary as openOCD flashes from elf
override BIN = $(CP) -O binary --gap-fill 0xff
override HEX = $(CP) -O ihex --gap-fill 0xff



# Define linker script file here
ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_F427)
LDSCRIPT = STM32F427xG_ksoloti.ld
else ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
LDSCRIPT = STM32H743xI.ld
else ifeq ($(SUBBOARDDEF),BOARD_AXOLOTI_CORE)
LDSCRIPT = STM32F427xG_axoloti.ld
endif

$(info Linking with $(LDSCRIPT))

# C sources that can be compiled in ARM or THUMB mode depending on the global
# setting.
CSRC = $(ALLCSRC) \
  $(CHIBIOS)/os/hal/lib/streams/chprintf.c \
  $(CHIBIOS)/os/hal/lib/streams/memstreams.c \
  ${FIRMWARE}/codec.c \
  ${FIRMWARE}/i2scodec.c \
  ${FIRMWARE}/axoloti.c \
  ${FIRMWARE}/axoloti_filters.c \
  ${FIRMWARE}/axoloti_math.c \
  ${FIRMWARE}/axoloti_oscs.c \
  ${FIRMWARE}/axoloti_board.c \
  ${FIRMWARE}/midi.c \
  ${FIRMWARE}/ui.c \
  ${FIRMWARE}/usbcfg.c \
  ${FIRMWARE}/sdcard.c \
  ${FIRMWARE}/pconnection.c \
  ${FIRMWARE}/parameters.c \
  ${FIRMWARE}/main.c \
  ${FIRMWARE}/patch.c \
  ${FIRMWARE}/flash.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_bitreversal.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_bitreversal2.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_q15.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_q31.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_radix2_init_q15.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_radix2_init_q31.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_radix2_q15.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_radix2_q31.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_radix4_init_q15.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_radix4_init_q31.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_radix4_q15.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_cfft_radix4_q31.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_rfft_init_q15.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_rfft_init_q31.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_rfft_q15.c \
  ${CMSIS}/DSP/Source/TransformFunctions/arm_rfft_q31.c \
  ${CMSIS}/DSP/Source/BasicMathFunctions/arm_mult_q31.c \
  ${CMSIS}/DSP/Source/BasicMathFunctions/arm_mult_q15.c \
  ${CMSIS}/DSP/Source/BasicMathFunctions/arm_mult_f32.c \
  ${CMSIS}/DSP/Source/BasicMathFunctions/arm_shift_q31.c \
  ${CMSIS}/DSP/Source/BasicMathFunctions/arm_shift_q15.c \
  ${CMSIS}/DSP/Source/BasicMathFunctions/arm_scale_f32.c \
  ${CMSIS}/DSP/Source/ComplexMathFunctions/arm_cmplx_mult_cmplx_q31.c \
  ${CMSIS}/DSP/Source/ComplexMathFunctions/arm_cmplx_mult_cmplx_f32.c \
  ${CMSIS}/DSP/Source/ComplexMathFunctions/arm_cmplx_mag_q31.c \
  ${CMSIS}/DSP/Source/ComplexMathFunctions/arm_cmplx_mult_cmplx_q15.c \
  ${CMSIS}/DSP/Source/ComplexMathFunctions/arm_cmplx_mag_q15.c \
  ${CMSIS}/DSP/Source/FastMathFunctions/arm_cos_q31.c \
  ${CMSIS}/DSP/Source/FastMathFunctions/arm_sin_q31.c \
  ${CMSIS}/DSP/Source/FastMathFunctions/arm_sqrt_q31.c \
  ${CMSIS}/DSP/Source/FastMathFunctions/arm_sqrt_q15.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_init_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_fast_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_biquad_cascade_df1_32x64_init_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_biquad_cascade_df1_32x64_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_biquad_cascade_df1_fast_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_biquad_cascade_df1_init_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_biquad_cascade_df1_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_interpolate_init_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_interpolate_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_decimate_init_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_decimate_q31.c \
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_decimate_fast_q31.c\
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_lattice_init_q31.c\
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_fir_lattice_q31.c\
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_iir_lattice_init_q31.c\
  ${CMSIS}/DSP/Source/FilteringFunctions/arm_iir_lattice_q31.c\
  ${CMSIS}/DSP/Source/CommonTables/arm_common_tables.c \
  ${CMSIS}/DSP/Source/CommonTables/arm_const_structs.c \
  ${FIRMWARE}/serial_midi.c \
  ${FIRMWARE}/bulk_usb.c \
  ${FIRMWARE}/midi_usb.c \
  ${FIRMWARE}/audio_usb.c \
  ${FIRMWARE}/spilink.c \
  ${FIRMWARE}/spidb.c \
  ${FIRMWARE}/crc32.c \
  ${FIRMWARE}/watchdog.c \
  ${FIRMWARE}/sysmon.c \
  ${FIRMWARE}/axoloti_memory.c \
  ${FIRMWARE}/exceptions.c \
  ${FIRMWARE}/debug.c \
  ${FIRMWARE}/UsbLog.c \
  ${FIRMWARE}/ksoloti_boot_options.c \
  ${FIRMWARE}/analyser.c

ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h747
  CSRC += ${FIRMWARE}/STM32H7xx_HAL_Driver/Src/stm32h7xx_hal_i2c.c \
          ${FIRMWARE}/STM32H7xx_HAL_Driver/Src/stm32h7xx_hal_dma.c \
          ${FIRMWARE}/STM32H7xx_HAL_Driver/Src/stm32h7xx_ll_fmc.c \
          ${FIRMWARE}/STM32H7xx_HAL_Driver/Src/stm32h7xx_ll_usb.c \
          ${FIRMWARE}/STM32H7xx_HAL_Driver/Src/stm32h7xx_hal_hcd.c \
          ${FIRMWARE}/STM32H7xx_HAL_Driver/Src/stm32h7xx_hal_gpio.c \
          ${FIRMWARE}/STM32H7xx_HAL_Driver/Src/stm32h7xx_hal_cortex.c \
          ${FIRMWARE}/sdram.c \
          ${FIRMWARE}/board.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Core/Src/usbh_core.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Core/Src/usbh_ctlreq.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Core/Src/usbh_ioreq.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Core/Src/usbh_pipes.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Class/HID/Src/usbh_hid.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Class/HID/Src/usbh_hid_keybd.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Class/HID/Src/usbh_hid_mouse.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Class/HID/Src/usbh_hid_parser.c \
          ${FIRMWARE}/STM32_USB_Host_Library_H7/Class/AUDIO/Src/usbh_audio.c \
          ${FIRMWARE}/usbh_midi_core.c \
          ${FIRMWARE}/usbh_vendor.c \
          ${FIRMWARE}/usbh_conf_h7.c \
          ${FIRMWARE}/flash_h7_functions.c 
else
  # Axoloti and Ksoloti f427
  CSRC += ${FIRMWARE}/STM32F4xx_HAL_Driver/Src/stm32f4xx_ll_usb.c \
          ${FIRMWARE}/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_gpio.c \
          ${FIRMWARE}/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_cortex.c \
          ${FIRMWARE}/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_hcd.c \
          ${FIRMWARE}/STM32F4xx_HAL_Driver/Src/stm32f4xx_hal_i2c.c \
          ${FIRMWARE}/board.c \
          ${FIRMWARE}/STM32_USB_Host_Library/Core/Src/usbh_core.c \
          ${FIRMWARE}/STM32_USB_Host_Library/Core/Src/usbh_ctlreq.c \
          ${FIRMWARE}/STM32_USB_Host_Library/Core/Src/usbh_ioreq.c \
          ${FIRMWARE}/STM32_USB_Host_Library/Core/Src/usbh_pipes.c \
          ${FIRMWARE}/STM32_USB_Host_Library/Class/HID/Src/usbh_hid.c \
          ${FIRMWARE}/STM32_USB_Host_Library/Class/HID/Src/usbh_hid_keybd.c \
          ${FIRMWARE}/STM32_USB_Host_Library/Class/HID/Src/usbh_hid_mouse.c \
          ${FIRMWARE}/STM32_USB_Host_Library/Class/HID/Src/usbh_hid_parser.c \
          ${FIRMWARE}/usbh_midi_core.c \
          ${FIRMWARE}/usbh_vendor.c \
          ${FIRMWARE}/usbh_conf_f4.c \
          ${FIRMWARE}/flash_f4_functions.c 
endif


ifeq ($(INBUILT_MOUNTER_FLASHER),yes)  
  CSRC += ${FIRMWARE}/mounter.c \
          ${FIRMWARE}/mass_storage/usb_msd.c 
  USE_OPT += -DINBUILT_MOUNTER_FLASHER=1
endif

#optional modules
ifneq (,$(findstring BRAIDS,$(AXO_OPT_MODULES)))
#$(info including BRAIDS)
BRAIDS_SRC = \
  ${MUTABLE_INSTRUMENTS}/braids/braids_resources.cpp \
  ${MUTABLE_INSTRUMENTS}/braids/analog_oscillator.cpp \
  ${MUTABLE_INSTRUMENTS}/braids/digital_oscillator.cpp \
  ${MUTABLE_INSTRUMENTS}/braids/macro_oscillator.cpp 
endif

ifneq (,$(findstring WARPS,$(AXO_OPT_MODULES)))
#$(info including WARPS)
WARPS_SRC = \
  ${MUTABLE_INSTRUMENTS}/warps/warps_resources.cpp \
  ${MUTABLE_INSTRUMENTS}/warps/dsp/filter_bank.cpp \
  ${MUTABLE_INSTRUMENTS}/warps/dsp/modulator.cpp \
  ${MUTABLE_INSTRUMENTS}/warps/dsp/oscillator.cpp \
  ${MUTABLE_INSTRUMENTS}/warps/dsp/vocoder.cpp 
endif

ifneq (,$(findstring ELEMENTS,$(AXO_OPT_MODULES)))
#$(info including ELEMENTS)
ELEMENTS_SRC = \
  ${FIRMWARE}/axoloti_mi.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/elements_resources.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/dsp/multistage_envelope.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/dsp/ominous_voice.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/dsp/resonator.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/dsp/string.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/dsp/tube.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/dsp/exciter.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/dsp/part.cpp \
  ${MUTABLE_INSTRUMENTS}/elements/dsp/voice.cpp
endif

ifneq (,$(findstring CLOUDS,$(AXO_OPT_MODULES)))
#$(info including CLOUDS)
CLOUDS_SRC = \
  ${MUTABLE_INSTRUMENTS}/clouds/clouds_resources.cpp \
  ${MUTABLE_INSTRUMENTS}/clouds/dsp/correlator.cpp \
  ${MUTABLE_INSTRUMENTS}/clouds/dsp/mu_law.cpp \
  ${MUTABLE_INSTRUMENTS}/clouds/dsp/granular_processor.cpp \
  ${MUTABLE_INSTRUMENTS}/clouds/dsp/pvoc/frame_transformation.cpp \
  ${MUTABLE_INSTRUMENTS}/clouds/dsp/pvoc/phase_vocoder.cpp \
  ${MUTABLE_INSTRUMENTS}/clouds/dsp/pvoc/stft.cpp 
endif

ifneq (,$(findstring RINGS,$(AXO_OPT_MODULES)))
#$(info  including RINGS)
RINGS_SRC = \
  ${MUTABLE_INSTRUMENTS}/rings/rings_resources.cpp 
endif

ifneq (,$(findstring STREAMS,$(AXO_OPT_MODULES)))
#$(info including STREAMS)
STREAMS_SRC = \
  ${MUTABLE_INSTRUMENTS}/streams/streams_resources.cpp \
  ${MUTABLE_INSTRUMENTS}/streams/compressor.cpp \
  ${MUTABLE_INSTRUMENTS}/streams/envelope.cpp \
  ${MUTABLE_INSTRUMENTS}/streams/follower.cpp \
  ${MUTABLE_INSTRUMENTS}/streams/lorenz_generator.cpp \
  ${MUTABLE_INSTRUMENTS}/streams/processor.cpp \
  ${MUTABLE_INSTRUMENTS}/streams/svf.cpp \
  ${MUTABLE_INSTRUMENTS}/streams/vactrol.cpp 
endif

#$(info including base MI)

# C++ sources that can be compiled in ARM or THUMB mode depending on the global
# setting.
CPPSRC = \
  $(ALLCPPSRC) \
  ${MUTABLE_INSTRUMENTS}/stmlib/dsp/units.cpp \
  ${MUTABLE_INSTRUMENTS}/stmlib/dsp/atan.cpp \
  ${MUTABLE_INSTRUMENTS}/stmlib/utils/random.cpp \
  ${MUTABLE_INSTRUMENTS}/mutable_resources.cpp \
  ${BRAIDS_SRC} \
  ${WARPS_SRC} \
  ${ELEMENTS_SRC} \
  ${CLOUDS_SRC} \
  ${RINGS_SRC} \
  ${STREAMS_SRC} \
  ${FIRMWARE}/oled_i2c.cpp



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
ASMSRC = $(ALLASMSRC)

# List ASM with preprocessor source files here.
ASMXSRC = $(ALLXASMSRC)

INCDIR = \
  ${CMSIS}/Core/Include \
  ${CMSIS}/DSP/Include \
  $(STARTUPINC) \
  $(PORTINC) $(KERNINC) $(TESTINC) \
  $(HALINC) $(PLATFORMINC) $(BOARDINC) \
  $(FATFSINC) \
  $(SHELLINC) \
  $(OSALINC) \
  $(CHIBIOS)/os/various \
  ${MUTABLE_INSTRUMENTS} \
  ${MUTABLE_INSTRUMENTS}/warps \
  ${MUTABLE_INSTRUMENTS}/braids \
  ${MUTABLE_INSTRUMENTS}/rings \
  ${MUTABLE_INSTRUMENTS}/elements \
  ${MUTABLE_INSTRUMENTS}/stmlib \
  $(CONFDIR) $(ALLINC) \
	$(CHIBIOS)/os/hal/lib/streams

ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h747
  INCDIR += ${FIRMWARE}/STM32H7xx_HAL_Driver/Inc \
            ${FIRMWARE}/STM32_USB_Host_Library_H7/Core/Inc \
            ${FIRMWARE}/STM32_USB_Host_Library_H7/Class/HID/Inc \
            ${FIRMWARE}/STM32_USB_Host_Library_H7/Class/AUDIO/Inc 
else
  # Axoloti and Ksoloti f427
  INCDIR += ${FIRMWARE}/STM32F4xx_HAL_Driver/Inc \
            ${FIRMWARE}/STM32_USB_Host_Library/Core/Inc \
            ${FIRMWARE}/STM32_USB_Host_Library/Class/HID/Inc 
endif
#
# Project, sources and paths
##############################################################################

##############################################################################
# Compiler settings
#


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
  -D$(BOARDDEF)=1 \
  -D$(SUBBOARDDEF)=1 \
  -DCORTEX_USE_FPU=TRUE \
  -DCRT1_AREAS_NUMBER=0 \
  -DCRT0_AREAS_NUMBER=0 \
  -DTHUMB \
  -DTHUMB_NO_INTERWORKING \
  -DTHUMB_PRESENT \
  -DENABLE_EXCPT_DUMP \
  -DANALYSE_USB_AUDIO=0
  
ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h743
  UDEFS += -DPATCH_ITCM=1 \
           -DSTM32H743xx \
           -DARM_MATH_CM7 \
           -DCORE_CM7 
else
  # Axoloti and Ksoloti f427
  UDEFS += -DSTM32F427xx \
           -DARM_MATH_CM4
endif


# Define ASM defines here
UADEFS =

# List all user directories here
UINCDIR = 

# List the user directory to look for the libraries here
ULIBDIR = .

# List all user libraries here
ULIBS = -lm 

#
# End of user defines
##############################################################################

# Set DEPDIR to location for dependencies inside build folder
DEPDIR := $(BUILDDIR)/.dep

RULESPATH = $(CHIBIOS)/os/common/startup/ARMCMx/compilers/GCC/mk
include $(RULESPATH)/arm-none-eabi.mk
include ./rules.mk


