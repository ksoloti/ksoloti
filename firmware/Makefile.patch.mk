BOARDDEF =
FWOPTIONDEF =
BUILDFILENAME =


# Some new options are important to keep SRAM usage and DSP load low with newer GCC versions.
# "--param max-completely-peeled-insns=100" makes a big difference to get SRAM down. Newer GCC versions use 200 here, original axoloti (GCC 4.9) used 100.
# Added a few flags that speed up floating-point calculation at the expense of precision. Graciously shared by https://github.com/malyzajko/daisy/blob/master/doc/documentation.md#running-generated-code
CCFLAGS = \
  --param max-completely-peeled-insns=100 \
  -O3 \
  -Wno-implicit-fallthrough \
  -Wno-return-type \
  -Wno-unused-parameter \
  -fcode-hoisting \
  -fdata-sections \
  -ffast-math \
  -ffp-contract=off \
  -ffunction-sections \
  -fno-common \
  -fno-math-errno \
  -fno-reorder-blocks \
  -fno-rtti \
  -fno-signed-zeros \
  -fno-threadsafe-statics \
  -fno-unsafe-math-optimizations \
  -fno-use-cxa-atexit \
  -fpermissive \
  -ggdb3 \
  -mcpu=cortex-m4 \
  -mfloat-abi=hard \
  -mfpu=fpv4-sp-d16 \
  -mno-thumb-interwork \
  -mthumb \
  -mtune=cortex-m4 \
  -mword-relocations \
  -nostartfiles \
  -nostdlib \
  -std=c++11

DEFS = \
  -D$(BOARDDEF) \
  -DARM_MATH_CM4 \
  -DCORTEX_USE_FPU=TRUE \
  -DTHUMB \
  -DTHUMB_NO_INTERWORKING \
  -DTHUMB_PRESENT \
  -D__FPU_PRESENT


ifneq ($(FWOPTIONDEF),FW_NORMAL)
  DEFS := $(DEFS) -D$(FWOPTIONDEF)
endif

ifeq ($(BUILDFILENAME),)
  BUILDFILENAME=xpatch
endif

ELFNAME=
ifeq ($(BOARDDEF),BOARD_KSOLOTI_CORE)
  RAMLINKOPT = -Tramlink_ksoloti.ld
  ELFNAME = ksoloti
else ifeq ($(BOARDDEF),BOARD_AXOLOTI_CORE)
  RAMLINKOPT = -Tramlink_axoloti.ld
  ELFNAME = axoloti
endif

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

ODFLAGS = \
  --demangle \
  --disassemble \
  --source-comment \
  --syms

TRGT = arm-none-eabi-
CC   = $(TRGT)gcc
CPP  = $(TRGT)g++
LD   = $(TRGT)gcc
CP   = $(TRGT)objcopy
OD   = $(TRGT)objdump

EMPTY := 
SPACE := $(EMPTY) $(EMPTY)

BUILDDIR = $(subst $(SPACE),\ ,${axoloti_libraries}/build)
FIRMWARE = $(subst $(SPACE),\ ,${axoloti_firmware})
CHIBIOS  = $(subst $(SPACE),\ ,${axoloti_home}/chibios)
CMSIS    = $(subst $(SPACE),\ ,${axoloti_home}/CMSIS)


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


INCDIR = $(CMSIS)/Core/Include \
  $(CMSIS)/DSP/Include \
  $(PORTINC) $(KERNINC) $(TESTINC) \
  $(HALINC) $(PLATFORMINC) $(BOARDINC) \
  $(FATFSINC) \
  $(OSALINC) \
  ${FIRMWARE} \
  $(CHIBIOS) \
  $(CHIBIOS)/os/various \
  ${FIRMWARE}/STM32F4xx_HAL_Driver/Inc \
  ${FIRMWARE}/mutable_instruments \
  $(CHIBIOS)/os/common/ports/ARMCMx/compilers/GCC \
  $(CHIBIOS)/os/common/ports/ARMCMx/devices/STM32F4xx \
  $(CHIBIOS)/os/ext/CMSIS/include \
  $(CHIBIOS)/os/ext/CMSIS/ST/STM32F4xx

# Paths
IINCDIR = $(patsubst %,-I%,$(INCDIR) $(DINCDIR) $(UINCDIR))
LLIBDIR = $(patsubst %,-L%,$(DLIBDIR) $(ULIBDIR))

all: ${BUILDDIR}/$(BUILDFILENAME).bin

${BUILDDIR}/xpatch.h.gch: ${FIRMWARE}/xpatch.h ${FIRMWARE}/patch.h ${FIRMWARE}/axoloti.h ${FIRMWARE}/parameter_functions.h ${FIRMWARE}/axoloti_math.h ${FIRMWARE}/axoloti_filters.h

#	@echo Building precompiled header
	@$(CPP) $(CCFLAGS) $(DEFS) $(IINCDIR) -Winvalid-pch -MD -MP -c ${FIRMWARE}/xpatch.h -o ${BUILDDIR}/xpatch.h.gch

${BUILDDIR}/$(BUILDFILENAME).bin: ${BUILDDIR}/$(BUILDFILENAME).cpp ${BUILDDIR}/xpatch.h.gch

#	@echo Removing previous build files
	@rm -f ${BUILDDIR}/$(BUILDFILENAME).o ${BUILDDIR}/$(BUILDFILENAME).elf ${BUILDDIR}/$(BUILDFILENAME).bin ${BUILDDIR}/$(BUILDFILENAME).d ${BUILDDIR}/$(BUILDFILENAME).map ${BUILDDIR}/$(BUILDFILENAME).lst

#	@echo Compiling patch dependencies
	@$(CPP) $(CCFLAGS) $(DEFS) -H $(IINCDIR) -Winvalid-pch -MD -MP --include ${BUILDDIR}/xpatch.h -c ${BUILDDIR}/$(BUILDFILENAME).cpp -o ${BUILDDIR}/$(BUILDFILENAME).o

#	@echo Linking patch dependencies
	@$(LD) $(LDFLAGS) ${BUILDDIR}/$(BUILDFILENAME).o -Wl,-Map=${BUILDDIR}/$(BUILDFILENAME).map,--cref,--just-symbols=${FIRMWARE}/build/$(ELFNAME).elf -o ${BUILDDIR}/$(BUILDFILENAME).elf

#	@echo Creating binary
	@$(CP) -O binary ${BUILDDIR}/$(BUILDFILENAME).elf ${BUILDDIR}/$(BUILDFILENAME).bin

#	@echo Creating LST file for debugging
	@$(OD) $(ODFLAGS) "${BUILDDIR}/$(BUILDFILENAME).elf" > "${BUILDDIR}/$(BUILDFILENAME).lst" 

#	@echo Removing intermediate build artifacts
	@rm -f ${BUILDDIR}/$(BUILDFILENAME).o ${BUILDDIR}/$(BUILDFILENAME).elf ${BUILDDIR}/$(BUILDFILENAME).d

#	@echo test ... currently not required
#	@cp ${BUILDDIR}/$(BUILDFILENAME).bin ${BUILDDIR}/xpatch.bin

clean:
	@rm -f ${BUILDDIR}/$(BUILDFILENAME).o ${BUILDDIR}/$(BUILDFILENAME).elf ${BUILDDIR}/$(BUILDFILENAME).bin ${BUILDDIR}/$(BUILDFILENAME).d ${BUILDDIR}/$(BUILDFILENAME).map ${BUILDDIR}/$(BUILDFILENAME).lst ${BUILDDIR}/xpatch.h.gch

.PHONY: all clean