#!/bin/bash
set -e

BUILD_AXOLOTI=0
BUILD_KSOLOTI=1
BUILD_NORMAL=1
BUILD_USBAUDIO=1
BUILD_SPILINK=0
BUILD_FLASHER=1
BUILD_MOUNTER=1

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

        if [[ $BUILD_AXOLOTI -eq 1 ]]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Axoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_osx/compile_firmware.sh BOARD_AXOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER 2>&1 | tee -a firmware.log

            # create .lst files
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/flasher/flasher_build/axoloti_flasher/axoloti_flasher.elf > ./firmware/flasher/flasher_build/axoloti_flasher.lst
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/mounter/mounter_build/axoloti_mounter/axoloti_mounter.elf > ./firmware/mounter/mounter_build/axoloti_mounter.lst 
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/axoloti/normal/axoloti.elf > ./firmware/build/axoloti.lst
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/axoloti/spilink/axoloti_spilink.elf > ./firmware/build/axoloti_spilink.lst
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/axoloti/usbaudio/axoloti_usbaudio.elf > ./firmware/build/axoloti_usbaudio.lst
        fi

        if [[ $BUILD_KSOLOTI -eq 1 ]]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Ksoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_osx/compile_firmware.sh BOARD_KSOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER 2>&1 | tee -a firmware.log

            # create .lst files
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/flasher/flasher_build/ksoloti_flasher/ksoloti_flasher.elf > ./firmware/flasher/flasher_build/ksoloti_flasher.lst
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/mounter/mounter_build/ksoloti_mounter/ksoloti_mounter.elf > ./firmware/mounter/mounter_build/ksoloti_mounter.lst
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/ksoloti/normal/ksoloti.elf > ./firmware/build/ksoloti.lst
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/ksoloti/spilink/ksoloti_spilink.elf > ./firmware/build/ksoloti_spilink.lst
            # ./platform_osx/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/ksoloti/usbaudio/ksoloti_usbaudio.elf > ./firmware/build/ksoloti_usbaudio.lst
        fi

    ;;

    linux)
        rm -f ./firmware/build/*.*
        rm -f ./firmware.log

        if [[ $BUILD_AXOLOTI -eq 1 ]]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Axoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_linux/compile_firmware.sh BOARD_AXOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER 2>&1 | tee -a firmware.log

            # create .lst files
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/flasher/flasher_build/axoloti_flasher/axoloti_flasher.elf > ./firmware/flasher/flasher_build/axoloti_flasher.lst
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/mounter/mounter_build/axoloti_mounter/axoloti_mounter.elf > ./firmware/mounter/mounter_build/axoloti_mounter.lst 
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/axoloti/normal/axoloti.elf > ./firmware/build/axoloti.lst
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/axoloti/spilink/axoloti_spilink.elf > ./firmware/build/axoloti_spilink.lst
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/axoloti/usbaudio/axoloti_usbaudio.elf > ./firmware/build/axoloti_usbaudio.lst
        fi

        if [[ $BUILD_KSOLOTI -eq 1 ]]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Ksoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_linux/compile_firmware.sh BOARD_KSOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER 2>&1 | tee -a firmware.log

            # create .lst files
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/flasher/flasher_build/ksoloti_flasher/ksoloti_flasher.elf > ./firmware/flasher/flasher_build/ksoloti_flasher.lst
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/mounter/mounter_build/ksoloti_mounter/ksoloti_mounter.elf > ./firmware/mounter/mounter_build/ksoloti_mounter.lst
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/ksoloti/normal/ksoloti.elf > ./firmware/build/ksoloti.lst
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/ksoloti/spilink/ksoloti_spilink.elf > ./firmware/build/ksoloti_spilink.lst
            # ./platform_linux/bin/arm-none-eabi-objdump $ODFLAGS ./firmware/build/ksoloti/usbaudio/ksoloti_usbaudio.elf > ./firmware/build/ksoloti_usbaudio.lst
        fi

    ;;

    windows)
        rm -f ./firmware/build/*.*
        rm -f ./firmware.log

        # cd platform_win

        if [[ $BUILD_AXOLOTI -eq 1 ]]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Axoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_win/compile_firmware.sh BOARD_AXOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER 2>&1 | tee -a firmware.log

            # create .lst files
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/flasher/flasher_build/axoloti_flasher/axoloti_flasher.elf > ./firmware/flasher/flasher_build/axoloti_flasher.lst
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/mounter/mounter_build/axoloti_mounter/axoloti_mounter.elf > ./firmware/mounter/mounter_build/axoloti_mounter.lst 
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/build/axoloti/normal/axoloti.elf > ./firmware/build/axoloti.lst
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/build/axoloti/spilink/axoloti_spilink.elf > ./firmware/build/axoloti_spilink.lst
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/build/axoloti/usbaudio/axoloti_usbaudio.elf > ./firmware/build/axoloti_usbaudio.lst
        fi

        if [[ $BUILD_KSOLOTI -eq 1 ]]; then
            printf "\n\n"
            printf "********************\n"
            printf "* Building Ksoloti *\n"
            printf "********************\n"

            # compile board mode and firmware options
            sh ./platform_win/compile_firmware.sh BOARD_KSOLOTI_CORE $BUILD_NORMAL $BUILD_USBAUDIO $BUILD_SPILINK $BUILD_FLASHER $BUILD_MOUNTER 2>&1 | tee -a firmware.log

            # create .lst files
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/flasher/flasher_build/ksoloti_flasher/ksoloti_flasher.elf > ./firmware/flasher/flasher_build/ksoloti_flasher.lst
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/mounter/mounter_build/ksoloti_mounter/ksoloti_mounter.elf > ./firmware/mounter/mounter_build/ksoloti_mounter.lst
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/build/ksoloti/normal/ksoloti.elf > ./firmware/build/ksoloti.lst
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/build/ksoloti/spilink/ksoloti_spilink.elf > ./firmware/build/ksoloti_spilink.lst
            # ./platform_win/bin/arm-none-eabi-objdump.exe $ODFLAGS ./firmware/build/ksoloti/usbaudio/ksoloti_usbaudio.elf > ./firmware/build/ksoloti_usbaudio.lst
        fi

        # cd ..

    ;;
esac