BOARDDEF =
FWOPTIONDEF =


# Unneccessarily long list of -fxxx optimisation options, most of which are included in -O3.
# Will leave them in for now and possibly start weeding them out occasionally.
# However some of them are important to keep SRAM usage and DSP load low in newer GCC versions
CCFLAGS = \
    -Wno-unused-parameter \
    -ggdb3 \
    -mcpu=cortex-m4 \
    -mfloat-abi=hard \
    -mfpu=fpv4-sp-d16 \
    -mlong-calls \
    -mthumb \
    -mtune=cortex-m4 \
    -mword-relocations \
    -nostartfiles \
    -nostdlib \
    -std=c++11 \
    \
    -O3 \
    -falign-functions \
    -falign-jumps \
    -falign-labels \
    -falign-loops \
    -fbranch-count-reg \
    -fcaller-saves \
    -fcode-hoisting \
    -fcombine-stack-adjustments \
    -fcompare-elim \
    -fcprop-registers \
    -fcrossjumping \
    -fcse-follow-jumps \
    -fdefer-pop \
    -fdevirtualize \
    -fdevirtualize-speculatively \
    -fexpensive-optimizations \
    -ffast-math \
    -fno-forward-propagate \
    -fgcse \
    -fgcse-after-reload \
    -fguess-branch-probability \
    -fhoist-adjacent-loads \
    -fif-conversion \
    -fif-conversion2 \
    -findirect-inlining \
    -finline-functions \
    -finline-functions-called-once \
    -finline-small-functions \
    -fipa-bit-cp \
    -fipa-cp \
    -fipa-cp-clone \
    -fipa-icf \
    -fipa-profile \
    -fipa-pure-const \
    -fipa-ra \
    -fipa-reference \
    -fipa-reference-addressable \
    -fipa-sra \
    -fipa-vrp \
    -fisolate-erroneous-paths-dereference \
    -floop-interchange \
    -floop-unroll-and-jam \
    -flra-remat \
    -fmerge-constants \
    -fno-common \
    -fno-exceptions \
    -fno-math-errno \
    -fno-rtti \
    -fno-threadsafe-statics \
    -fno-use-cxa-atexit \
    -fomit-frame-pointer \
    -foptimize-sibling-calls \
    -foptimize-strlen \
    -fno-partial-inlining \
    -fpeel-loops \
    -fpeephole2 \
    -fpermissive \
    -fpredictive-commoning \
    -fno-reorder-blocks \
    -freorder-functions \
    -frerun-cse-after-loop \
    -fschedule-fusion \
    -fno-schedule-insns \
    -fno-schedule-insns2 \
    -fshrink-wrap \
    -fsplit-loops \
    -fsplit-paths \
    -fsplit-wide-types \
    -fssa-phiopt \
    -fstore-merging \
    -fstrict-aliasing \
    -fthread-jumps \
    -ftree-bit-ccp \
    -ftree-builtin-call-dce \
    -ftree-ccp \
    -ftree-ch \
    -ftree-coalesce-vars \
    -ftree-copy-prop \
    -ftree-dce \
    -ftree-dominator-opts \
    -ftree-dse \
    -ftree-fre \
    -ftree-loop-distribute-patterns \
    -ftree-loop-distribution \
    -ftree-loop-vectorize \
    -ftree-partial-pre \
    -ftree-pre \
    -ftree-pta \
    -ftree-sink \
    -ftree-slp-vectorize \
    -ftree-slsr \
    -ftree-sra \
    -ftree-switch-conversion \
    -ftree-tail-merge \
    -ftree-ter \
    -ftree-vrp \
    -funswitch-loops \
    -fvect-cost-model=cheap \
    -fversion-loops-for-strides

DEFS = \
    -D$(BOARDDEF) \
    -DARM_MATH_CM4 \
    -DCORTEX_USE_FPU=TRUE \
    -DSTM32F427xx \
    -DTHUMB \
    -DTHUMB_NO_INTERWORKING \
    -DTHUMB_PRESENT \
    -D__FPU_PRESENT

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

LDFLAGS = \
    $(RAMLINKOPT) \
    -Bsymbolic \
    -Wl,--gc-sections \
    -fno-common \
    -mcpu=cortex-m4 \
    -mfloat-abi=hard \
    -mfpu=fpv4-sp-d16 \
    -mlong-calls \
    -mno-thumb-interwork \
    -mthumb \
    -mtune=cortex-m4 \
    -nostartfiles

TRGT = arm-none-eabi-
CC=$(TRGT)gcc
CPP=$(TRGT)g++
LD=$(TRGT)gcc
CP=$(TRGT)objcopy
DMP=$(TRGT)objdump
SIZ=$(TRGT)size

axoloti_runtime ?= ..
axoloti_release ?= ..
axoloti_home ?= ..
axoloti_libraries ?= ..
axoloti_firmware ?= ../firmware

CHIBIOS = ${axoloti_release}/chibios
CMSIS = ${axoloti_release}/CMSIS

EMPTY := 
SPACE := $(EMPTY) $(EMPTY)
BUILDDIR=$(subst $(SPACE),\ ,${axoloti_libraries}/build)
FIRMWARE=$(subst $(SPACE),\ ,${axoloti_firmware})

include $(CHIBIOS)/boards/ST_STM32F4_DISCOVERY/board.mk
include $(CHIBIOS)/os/hal/platforms/STM32F4xx/platform.mk
include $(CHIBIOS)/os/hal/hal.mk
include $(CHIBIOS)/os/ports/GCC/ARMCMx/STM32F4xx/port.mk
include $(CHIBIOS)/os/kernel/kernel.mk
include $(CHIBIOS)/os/various/fatfs_bindings/fatfs.mk

INCDIR = $(CMSIS)/Core/Include $(CMSIS)/DSP/Include \
         $(PORTINC) $(KERNINC) $(TESTINC) \
         $(HALINC) $(PLATFORMINC) $(BOARDINC) $(FATFSINC) \
         ${FIRMWARE} $(CHIBIOS) ${FIRMWARE}/mutable_instruments

# Paths
IINCDIR = $(patsubst %,-I%,$(INCDIR) $(DINCDIR) $(UINCDIR))
LLIBDIR = $(patsubst %,-L%,$(DLIBDIR) $(ULIBDIR))

all: ${BUILDDIR}/xpatch.bin

${BUILDDIR}/xpatch.h.gch: ${FIRMWARE}/xpatch.h ${FIRMWARE}/patch.h ${FIRMWARE}/axoloti.h ${FIRMWARE}/parameter_functions.h ${FIRMWARE}/axoloti_math.h ${FIRMWARE}/axoloti_filters.h
#	@echo Building precompiled header
	@$(CPP) $(CCFLAGS) $(DEFS) $(IINCDIR) -Winvalid-pch -MD -MP -c ${FIRMWARE}/xpatch.h  -o ${BUILDDIR}/xpatch.h.gch

${BUILDDIR}/xpatch.bin: ${BUILDDIR}/xpatch.cpp ${BUILDDIR}/xpatch.h.gch
#	@echo Removing previous build files
	@rm -f ${BUILDDIR}/xpatch.o ${BUILDDIR}/xpatch.elf ${BUILDDIR}/xpatch.bin ${BUILDDIR}/xpatch.d ${BUILDDIR}/xpatch.map ${BUILDDIR}/xpatch.lst
#	@echo Compiling patch dependencies
	@$(CPP) $(CCFLAGS) $(DEFS) -H $(IINCDIR) -Winvalid-pch -MD -MP --include ${BUILDDIR}/xpatch.h -c ${BUILDDIR}/xpatch.cpp -o ${BUILDDIR}/xpatch.o
#	@echo Linking patch dependencies
	@$(LD) $(LDFLAGS) ${BUILDDIR}/xpatch.o -Wl,-Map=${BUILDDIR}/xpatch.map,--cref,--just-symbols=${FIRMWARE}/build/$(ELFNAME).elf -o ${BUILDDIR}/xpatch.elf
#	@echo Creating LST file for debugging
	@$(DMP) -belf32-littlearm -marm --demangle --debugging --source --disassemble ${BUILDDIR}/xpatch.elf > ${BUILDDIR}/xpatch.lst
#   (--source-comment not supported in gcc7 yet) --line-numbers 

#	@echo Creating binary
#	$(CP) -O binary -j .text  -j .init_array -j .rodata -j .rodata\* xpatch.elf xpatch.bin
#   -j .text.startup -j .text.memcpy
	@$(CP) -O binary ${BUILDDIR}/xpatch.elf ${BUILDDIR}/xpatch.bin
#	@echo Displaying size statistic
	@$(SIZ) --format=sysv ${BUILDDIR}/xpatch.elf
	@$(SIZ) --format=berkeley ${BUILDDIR}/xpatch.elf

.PHONY: clean

clean:
	@rm -f ${BUILDDIR}/xpatch.o ${BUILDDIR}/xpatch.elf ${BUILDDIR}/xpatch.bin ${BUILDDIR}/xpatch.d ${BUILDDIR}/xpatch.map ${BUILDDIR}/xpatch.lst ${BUILDDIR}/xpatch.h.gch