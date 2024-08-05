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
            sh ./platform_osx/compile_firmware.sh BOARD_AXOLOTI_CORE && ./platform_osx/compile_firmware.sh BOARD_KSOLOTI_CORE 2>&1 | tee firmware.log
        ;;
        linux)
            sh ./platform_linux/compile_firmware.sh BOARD_AXOLOTI_CORE && ./platform_linux/compile_firmware.sh BOARD_KSOLOTI_CORE 2>&1 | tee firmware.log
        ;;
        windows)
            cd platform_win
            cmd "//C path.bat && compile_firmware.bat BOARD_AXOLOTI_CORE 2>&1 | tee ..\firmware.log && compile_firmware.bat BOARD_KSOLOTI_CORE 2>&1 | tee -a ..\firmware.log"
            cd ..
        ;;
esac

