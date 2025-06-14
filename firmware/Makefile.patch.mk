BOARDDEF =
SUBBOARDDEF=
FWOPTIONDEF =
LINKERFILE =

$(info SUBBOARDDEF = $(SUBBOARDDEF))
$(info FWOPTIONDEF = $(FWOPTIONDEF))
$(info LINKERFILE  = $(LINKERFILE))
 
# Some new options are important to keep SRAM usage and DSP load low with newer GCC versions.
# "--param max-completely-peeled-insns=100" makes a big difference to get SRAM down. Newer GCC versions use 200 here, original axoloti (GCC 4.9) used 100.
# Added a few flags that speed up floating-point calculation at the expense of precision. Graciously shared by https://github.com/malyzajko/daisy/blob/master/doc/documentation.md#running-generated-code
CCFLAGS = \
  -Wno-implicit-fallthrough \
  -Wno-unused-parameter \
  -Wno-return-type \
  -ggdb3 \
  -mfloat-abi=hard \
  -mthumb \
  -mword-relocations \
  -nostartfiles \
  -nostdlib \
  -std=c++11 \
  -O3 \
  --param max-completely-peeled-insns=100 \
  -fcode-hoisting \
  -fno-threadsafe-statics \
  -ffunction-sections \
  -fdata-sections \
  -fno-common \
  -fno-math-errno \
  -fno-reorder-blocks \
  -fno-rtti \
  -mno-thumb-interwork \
  -fno-use-cxa-atexit \
  -fpermissive \
  -ffast-math \
  -fno-unsafe-math-optimizations \
  -fno-signed-zeros \
  -ffp-contract=off 
  
  #-Wa,-adhlns="$@.lst"

ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h747
  CCFLAGS += -mcpu=cortex-m7 \
             -mfpu=fpv5-sp-d16 \
             -mtune=cortex-m7 
else
  # Axoloti and Ksoloti f427
  CCFLAGS += -mcpu=cortex-m4 \
             -mfpu=fpv4-sp-d16 \
             -mtune=cortex-m4 
endif

DEFS = \
  -D$(BOARDDEF)=1 \
  -D$(SUBBOARDDEF)=1 \
  -DCORTEX_USE_FPU=TRUE \
  -DTHUMB \
  -DTHUMB_NO_INTERWORKING \
  -DTHUMB_PRESENT 

ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h743
  DEFS += -DPATCH_ITCM=0 \
          -DSTM32H743xx \
          -DARM_MATH_CM7 \
          -DCORE_CM7 \
          -Dbool_t=bool
else
  # Axoloti and Ksoloti f427
  DEFS += -DPATCH_ITCM=1 \
          -DSTM32F427xx \
          -DARM_MATH_CM4
endif

ifneq ($(FWOPTIONDEF),)
  DEFS := $(DEFS) -D$(FWOPTIONDEF)
endif

ELFNAME=

ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_F427)
  ELFNAME = ksoloti
else ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  ELFNAME = ksoloti_h743
else ifeq ($(SUBBOARDDEF),BOARD_AXOLOTI_CORE)
  ELFNAME = axoloti
endif

RAMLINKOPT = -T$(LINKERFILE)

$(info DEFS =  $(DEFS))
$(info CCFLAGS =  $(CCFLAGS))


ifeq ($(FWOPTIONDEF),FW_SPILINK)
  ELFNAME := $(ELFNAME)_spilink
endif

ifeq ($(FWOPTIONDEF),FW_USBAUDIO)
  ELFNAME := $(ELFNAME)_usbaudio
endif

ifeq ($(FWOPTIONDEF),FW_I2SCODEC)
  ELFNAME := $(ELFNAME)_i2scodec
endif

LDFLAGS = \
  $(RAMLINKOPT) \
  -Bsymbolic \
  -Wl,--gc-sections \
  -Wl,--print-memory-usage \
  -fno-common \
  -mcpu=cortex-m4 \
  -mfloat-abi=hard \
  -mfpu=fpv4-sp-d16 \
  -mno-thumb-interwork \
  -mthumb \
  -mtune=cortex-m4 \
  -nostartfiles 
  
DMPFLAGS = \
  --demangle \
  --disassemble \
  --source-comment \
  --syms

TRGT = arm-none-eabi-
CC   = $(TRGT)gcc
CPP  = $(TRGT)g++
LD   = $(TRGT)gcc
CP   = $(TRGT)objcopy
DMP  = $(TRGT)objdump
SIZ  = $(TRGT)size

axoloti_libraries ?= ..
axoloti_firmware  ?= ../firmware
axoloti_home      ?= ..

EMPTY := 
SPACE := $(EMPTY) $(EMPTY)

BUILDDIR = $(subst $(SPACE),\ ,${axoloti_libraries}/build)
FIRMWARE = $(subst $(SPACE),\ ,${axoloti_firmware})
CHIBIOS  = $(subst $(SPACE),\ ,${axoloti_home}/chibios)
CMSIS    = $(subst $(SPACE),\ ,${axoloti_home}/CMSIS)



ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h747

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
else
  # Axoloti and Ksoloti f427

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


INCDIR = $(CMSIS)/Core/Include \
  $(CMSIS)/DSP/Include \
  $(PORTINC) $(KERNINC) $(TESTINC) \
  $(HALINC) $(PLATFORMINC) $(BOARDINC) \
  $(FATFSINC) \
  $(OSALINC) \
  ${FIRMWARE} \
  $(CHIBIOS) \
  $(CHIBIOS)/os/various \
  $(CHIBIOS)/os/ \
  ${FIRMWARE}/mutable_instruments 
	

ifeq ($(SUBBOARDDEF),BOARD_KSOLOTI_CORE_H743)
  # Ksoloti h747
  INCDIR += ${FIRMWARE}/STM32H7xx_HAL_Driver/Inc 
else
  # Axoloti and Ksoloti f427
  INCDIR += ${FIRMWARE}/STM32F4xx_HAL_Driver/Inc \
            $(CHIBIOS)/os/common/ports/ARMCMx/compilers/GCC \
            $(CHIBIOS)/os/common/ports/ARMCMx/devices/STM32F4xx \
            $(CHIBIOS)/os/ext/CMSIS/include \
            $(CHIBIOS)/os/ext/CMSIS/ST/STM32F4xx
endif

# Paths
IINCDIR = $(patsubst %,-I%,$(INCDIR) $(DINCDIR) $(UINCDIR) $(CONFDIR) $(ALLINC))
LLIBDIR = $(patsubst %,-L%,$(DLIBDIR) $(ULIBDIR))


ifneq ($(wildcard ${BUILDDIR}/src/),)
	AXO_CSOURCES=$(shell find "${BUILDDIR}/src" -type f -iname '*.c')
	AXO_CPPSOURCES=$(shell find "${BUILDDIR}/src" -type f -iname '*.cpp')
	AXO_INCLUDES=$(shell find "${BUILDDIR}/src" -type d)
	AXO_OBJECTS=$(foreach x, $(basename $(AXO_CSOURCES)), $(x).o)
	AXO_OBJECTS+=$(foreach x, $(basename $(AXO_CPPSOURCES)), $(x).o)
else
endif


$(info AXO_CSOURCES = $(AXO_CSOURCES))
$(info AXO_CPPSOURCES = $(AXO_CPPSOURCES))
$(info AXO_OBJECTS = $(AXO_OBJECTS))
$(info AXO_INCLUDES= $(AXO_INCLUDES))
# $(info INCDIR = $(INCDIR))

INCDIR+=$(AXO_INCLUDES)

all: ${BUILDDIR}/xpatch.bin

.cpp.o:
	@$(CPP) $(CCFLAGS) $(DEFS) -H $(IINCDIR) -Winvalid-pch -MD -MP --include ${FIRMWARE}/xpatch.h -c $< -o $@

.c.o:
	@$(CPP) $(CCFLAGS) $(DEFS) -H $(IINCDIR) -I ${BUILDDIR}/src/** -Winvalid-pch -MD -MP --include ${FIRMWARE}/xpatch.h -c $< -o $@

${BUILDDIR}/xpatch.h.gch: ${FIRMWARE}/xpatch.h ${FIRMWARE}/patch.h ${FIRMWARE}/axoloti.h ${FIRMWARE}/parameter_functions.h ${FIRMWARE}/axoloti_math.h ${FIRMWARE}/axoloti_filters.h
	@echo Building precompiled header
	@$(CPP) $(CCFLAGS) $(DEFS) $(IINCDIR) -Winvalid-pch -MD -MP -c ${FIRMWARE}/xpatch.h -o ${BUILDDIR}/xpatch.h.gch

${BUILDDIR}/xpatch.bin: ${BUILDDIR}/xpatch.cpp ${BUILDDIR}/xpatch.h.gch $(AXO_OBJECTS)
	@echo Removing previous build files
	@rm -f ${BUILDDIR}/xpatch.o ${BUILDDIR}/xpatch.elf ${BUILDDIR}/xpatch.bin ${BUILDDIR}/xpatch.d ${BUILDDIR}/xpatch.map ${BUILDDIR}/xpatch.list ${BUILDDIR}/xpatch.siz 
	@echo Compiling patch dependencies
	@$(CPP) $(CCFLAGS) $(DEFS) -H $(IINCDIR) -Winvalid-pch -MD -MP --include ${BUILDDIR}/xpatch.h -c ${BUILDDIR}/xpatch.cpp -o ${BUILDDIR}/xpatch.o
	@echo Linking patch dependencies
	@$(LD) $(LDFLAGS) ${BUILDDIR}/xpatch.o $(AXO_OBJECTS) -lm -Wl,-Map=${BUILDDIR}/xpatch.map,--cref,--just-symbols=${FIRMWARE}/build/$(ELFNAME).elf -o ${BUILDDIR}/xpatch.elf

	@echo Creating binary
	@$(CP) -O binary ${BUILDDIR}/xpatch.elf ${BUILDDIR}/xpatch.bin

clean:
	@rm -f ${BUILDDIR}/xpatch.o ${BUILDDIR}/xpatch.elf ${BUILDDIR}/xpatch.bin ${BUILDDIR}/xpatch.d ${BUILDDIR}/xpatch.map ${BUILDDIR}/xpatch.lst ${BUILDDIR}/xpatch.h.gch

.PHONY: all clean