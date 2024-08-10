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
            sh ./platform_osx/compile_firmware.sh BOARD_AXOLOTI_CORE && sh ./platform_osx/compile_firmware.sh BOARD_KSOLOTI_CORE 2>&1 | tee firmware.log
            ./platform_osx/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/flasher/flasher_build/axoloti_flasher.elf > ./firmware/flasher/flasher_build/axoloti_flasher.lst && ./platform_osx/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/flasher/flasher_build/ksoloti_flasher.elf > ./firmware/flasher/flasher_build/ksoloti_flasher.lst
            ./platform_osx/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/mounter/mounter_build/axoloti_mounter.elf > ./firmware/mounter/mounter_build/axoloti_mounter.lst && ./platform_osx/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/mounter/mounter_build/ksoloti_mounter.elf > ./firmware/mounter/mounter_build/ksoloti_mounter.lst
            ./platform_osx/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/build/axoloti.elf > ./firmware/build/axoloti.lst && ./platform_osx/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/build/ksoloti.elf > ./firmware/build/ksoloti.lst
        ;;
        linux)
            sh ./platform_linux/compile_firmware.sh BOARD_AXOLOTI_CORE && sh ./platform_linux/compile_firmware.sh BOARD_KSOLOTI_CORE 2>&1 | tee firmware.log
            ./platform_linux/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/mounter/mounter_build/axoloti_mounter.elf > ./firmware/mounter/mounter_build/axoloti_mounter.lst && ./platform_linux/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/mounter/mounter_build/ksoloti_mounter.elf > ./firmware/mounter/mounter_build/ksoloti_mounter.lst
            ./platform_linux/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/build/axoloti.elf > ./firmware/build/axoloti.lst && ./platform_linux/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/build/ksoloti.elf > ./firmware/build/ksoloti.lst
            ./platform_linux/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/build/axoloti.elf > ./firmware/build/axoloti.lst && ./platform_linux/bin/arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ./firmware/build/ksoloti.elf > ./firmware/build/ksoloti.lst
        ;;
        windows)
            cd platform_win
            cmd "//C path.bat && compile_firmware.bat BOARD_AXOLOTI_CORE 2>&1 | tee ..\firmware.log && compile_firmware.bat BOARD_KSOLOTI_CORE 2>&1 | tee -a ..\firmware.log"
            cmd "//C path.bat && arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ..\firmware\flasher\flasher_build\axoloti_flasher.elf > ..\firmware\flasher\flasher_build\axoloti_flasher.lst && arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ..\firmware\flasher\flasher_build\ksoloti_flasher.elf > ..\firmware\flasher\flasher_build\ksoloti_flasher.lst"
            cmd "//C path.bat && arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ..\firmware\mounter\mounter_build\axoloti_mounter.elf > ..\firmware\mounter\mounter_build\axoloti_mounter.lst && arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ..\firmware\mounter\mounter_build\ksoloti_mounter.elf > ..\firmware\mounter\mounter_build\ksoloti_mounter.lst"
            cmd "//C path.bat && arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ..\firmware\build\axoloti.elf > ..\firmware\build\axoloti.lst && arm-none-eabi-objdump -belf32-littlearm -marm --demangle --disassemble ..\firmware\build\ksoloti.elf > ..\firmware\build\ksoloti.lst"
            cd ..
        ;;
esac
