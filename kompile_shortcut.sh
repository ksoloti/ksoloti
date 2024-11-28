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

        # compile board mode and firmware options
        sh ./platform_osx/compile_firmware.sh BOARD_KSOLOTI_CORE 2>&1 | tee -a firmware.log
        sh ./qlean.sh
    ;;
    linux)
        rm -f ./firmware/build/*.*
        sh ./qlean.sh

        # compile board mode and firmware options
        sh ./platform_linux/compile_firmware.sh BOARD_AXOLOTI_CORE 2>&1 | tee firmware.log

        # compile board mode and firmware options
        sh ./platform_linux/compile_firmware.sh BOARD_KSOLOTI_CORE 2>&1 | tee -a firmware.log
        sh ./qlean.sh
    ;;
    windows)
        rm -f ./firmware/build/*.*
        sh ./qlean.sh
        cd platform_win

        # compile board mode and firmware options
        cmd "//C path.bat && compile_firmware.bat BOARD_AXOLOTI_CORE 2>&1 | tee ..\firmware.log"

        # compile board mode and firmware options
        cmd "//C path.bat && compile_firmware.bat BOARD_KSOLOTI_CORE 2>&1 | tee -a ..\firmware.log"

        cd ..
        sh ./qlean.sh
    ;;
esac