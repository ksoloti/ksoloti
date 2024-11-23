#!/bin/bash
set -e

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
        echo "Unknown OS: $unamestr - aborting..."
        exit
    ;;
esac

case "$platform" in
    mac)
        rm -f ./firmware/build/*.*
        sh ./qlean.sh

        # compile board mode and firmware options
        sh ./platform_osx/compile_firmware.sh BOARD_AXOLOTI_CORE 2>&1 | tee firmware.log
        # create .list files
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/flasher/flasher_build/axoloti_flasher.elf > ./firmware/flasher/flasher_build/axoloti_flasher.list
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/mounter/mounter_build/axoloti_mounter.elf > ./firmware/mounter/mounter_build/axoloti_mounter.list 
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/axoloti.elf > ./firmware/build/axoloti.list
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/axoloti_spilink.elf > ./firmware/build/axoloti_spilink.list
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/axoloti_usbaudio.elf > ./firmware/build/axoloti_usbaudio.list

        # compile board mode and firmware options
        sh ./platform_osx/compile_firmware.sh BOARD_KSOLOTI_CORE 2>&1 | tee -a firmware.log
        # create .list files
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/flasher/flasher_build/ksoloti_flasher.elf > ./firmware/flasher/flasher_build/ksoloti_flasher.list
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/mounter/mounter_build/ksoloti_mounter.elf > ./firmware/mounter/mounter_build/ksoloti_mounter.list
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/ksoloti.elf > ./firmware/build/ksoloti.list
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/ksoloti_spilink.elf > ./firmware/build/ksoloti_spilink.list
        ./platform_osx/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/ksoloti_usbaudio.elf > ./firmware/build/ksoloti_usbaudio.list
        sh ./qlean.sh
    ;;
    linux)
        rm -f ./firmware/build/*.*
        sh ./qlean.sh

        # compile board mode and firmware options
        sh ./platform_linux/compile_firmware.sh BOARD_AXOLOTI_CORE 2>&1 | tee firmware.log
        # create .list files
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/flasher/flasher_build/axoloti_flasher.elf > ./firmware/flasher/flasher_build/axoloti_flasher.list
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/mounter/mounter_build/axoloti_mounter.elf > ./firmware/mounter/mounter_build/axoloti_mounter.list 
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/axoloti.elf > ./firmware/build/axoloti.list
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/axoloti_spilink.elf > ./firmware/build/axoloti_spilink.list
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/axoloti_usbaudio.elf > ./firmware/build/axoloti_usbaudio.list

        # compile board mode and firmware options
        sh ./platform_linux/compile_firmware.sh BOARD_KSOLOTI_CORE 2>&1 | tee -a firmware.log
        # create .list files
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/flasher/flasher_build/ksoloti_flasher.elf > ./firmware/flasher/flasher_build/ksoloti_flasher.list
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/mounter/mounter_build/ksoloti_mounter.elf > ./firmware/mounter/mounter_build/ksoloti_mounter.list
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/ksoloti.elf > ./firmware/build/ksoloti.list
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/ksoloti_spilink.elf > ./firmware/build/ksoloti_spilink.list
        ./platform_linux/bin/arm-none-eabi-objdump --source-comment --demangle --disassemble ./firmware/build/ksoloti_spilink.elf > ./firmware/build/ksoloti_usbaudio.list
        sh ./qlean.sh
    ;;
    windows)
        rm -f ./firmware/build/*.*
        sh ./qlean.sh
        cd platform_win

        # compile board mode and firmware options
        cmd "//C path.bat && compile_firmware.bat BOARD_AXOLOTI_CORE 2>&1 | tee ..\firmware.log"
        # create .list files
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\flasher\flasher_build\axoloti_flasher.elf > ..\firmware\flasher\flasher_build\axoloti_flasher.list"
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\mounter\mounter_build\axoloti_mounter.elf > ..\firmware\mounter\mounter_build\axoloti_mounter.list"
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\build\axoloti.elf > ..\firmware\build\axoloti.list"
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\build\axoloti_spilink.elf > ..\firmware\build\axoloti_spilink.list"
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\build\axoloti_spilink.elf > ..\firmware\build\axoloti_usbaudio.list"

        # compile board mode and firmware options
        cmd "//C path.bat && compile_firmware.bat BOARD_KSOLOTI_CORE 2>&1 | tee -a ..\firmware.log"
        # create .list files
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\flasher\flasher_build\ksoloti_flasher.elf > ..\firmware\flasher\flasher_build\ksoloti_flasher.list"
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\mounter\mounter_build\ksoloti_mounter.elf > ..\firmware\mounter\mounter_build\ksoloti_mounter.list"
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\build\ksoloti.elf > ..\firmware\build\ksoloti.list"
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\build\ksoloti_spilink.elf > ..\firmware\build\ksoloti_spilink.list"
        cmd "//C path.bat && arm-none-eabi-objdump --source-comment --demangle --disassemble ..\firmware\build\ksoloti_spilink.elf > ..\firmware\build\ksoloti_usbaudio.list"

        cd ..
        sh ./qlean.sh
    ;;
esac