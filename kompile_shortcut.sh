#!/bin/bash
set -e


# supported boards
BUILD_AXOLOTI=0
BUILD_KSOLOTI=1

# supported firmware modes
BUILD_NORMAL=1
BUILD_USBAUDIO=1
BUILD_SPILINK=1
BUILD_I2SCODEC=1
BUILD_FLASHER=0
BUILD_MOUNTER=0

ODFLAGS="--source-comment --demangle --disassemble"

platform='unknown'
unamestr=`uname`
case "$unamestr" in
    Linux)
        platform='linux'
        rootdir="$(dirname $(readlink -f $0))"
    ;;
    Darwin)
        platform='mac'
        rootdir="$(cd $(dirname $0); pwd -P)"
    ;;
    MINGW*)
        platform='windows'
        rootdir="$(cd $(dirname $0); pwd -P)"
    ;;
    *)
        printf "\nUnknown OS: $unamestr - aborting...\n"
        exit
    ;;
esac

case "$platform" in
    mac)
        rm -f ./firmware/build/*.*
        rm -f ./firmware.log

        if [ $BUILD_AXOLOTI -eq 1 ]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Axoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_osx/compile_firmware.sh BOARD_AXOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log

        fi

        if [ $BUILD_KSOLOTI -eq 1 ]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Ksoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_osx/compile_firmware.sh BOARD_KSOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log

        fi

    ;;

    linux)
        rm -f ./firmware/build/*.*
        rm -f ./firmware.log

        if [ $BUILD_AXOLOTI -eq 1 ]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Axoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_linux/compile_firmware.sh BOARD_AXOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log

        fi

        if [ $BUILD_KSOLOTI -eq 1 ]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Ksoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_linux/compile_firmware.sh BOARD_KSOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log

        fi

    ;;

    windows)
        rm -f ./firmware/build/*.*
        rm -f ./firmware.log

        if [[ $BUILD_AXOLOTI -eq 1 ]]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Axoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_win/compile_firmware.sh BOARD_AXOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log

        fi

        if [[ $BUILD_KSOLOTI -eq 1 ]]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Ksoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_win/compile_firmware.sh BOARD_KSOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER $BUILD_I2SCODEC 2>&1 | tee -a firmware.log

        fi

    ;;
esac