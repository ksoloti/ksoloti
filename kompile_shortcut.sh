#!/bin/bash
set -e

# supported boards
BUILD_AXOLOTI=0
BUILD_KSOLOTI_F427=1
BUILD_KSOLOTI_H743=0

# supported firmware modes
BUILD_NORMAL=1
BUILD_USBAUDIO=1
BUILD_SPILINK=0
BUILD_I2SCODEC=0
BUILD_FLASHER=0
BUILD_MOUNTER=0

ODFLAGS="--source-comment --demangle --disassemble"

platform='unknown'
unamestr=`uname`
case "$unamestr" in
    Linux)
        platform='linux'
        builddir="./platform_linux"
        rootdir="$(dirname $(readlink -f $0))"
    ;;
    Darwin)
        platform='mac'
        builddir='./platform_osx'
        rootdir="$(cd $(dirname $0); pwd -P)"
    ;;
    MINGW*)
        platform='windows'
        builddir="./platform_win"
        rootdir="$(cd $(dirname $0); pwd -P)"
    ;;
    *)
        printf "\nUnknown OS: $unamestr - aborting...\n"
        exit
    ;;
esac

rm -f ./firmware/build/*.*
rm -f ./firmware.log

if [[ $BUILD_AXOLOTI -eq 1 ]]; then
    printf "\n\n"
    printf "********************\n"
    printf "* Building Axoloti *\n"
    printf "********************\n"

    # compile board mode and firmware options
    sh $builddir/compile_firmware.sh BOARD_AXOLOTI_CORE BOARD_AXOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log
fi

if [[ $BUILD_KSOLOTI_F427 -eq 1 ]]; then
    printf "\n\n"
    printf "********************\n"
    printf "* Building Ksoloti *\n"
    printf "********************\n"

    # compile board mode and firmware options
    sh $builddir/compile_firmware.sh BOARD_KSOLOTI_CORE BOARD_KSOLOTI_CORE_F427 $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log
fi

if [[ $BUILD_KSOLOTI_H743 -eq 1 ]]; then
    printf "\n\n"
    printf "***********************\n"
    printf "* Building Ksoloti H7 *\n"
    printf "***********************\n"

    # compile board mode and firmware options
    sh $builddir/compile_firmware.sh BOARD_KSOLOTI_CORE BOARD_KSOLOTI_CORE_H743 $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log
fi
