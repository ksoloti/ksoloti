#!/usr/bin/env bash

unamestr=`uname`
case "$unamestr" in
    Linux)
        firmwaredir="$(dirname $(readlink -f $0))"
        export platformdir="${firmwaredir}/../platform_linux"
    ;;
    Darwin)
        firmwaredir="$(cd $(dirname $0); pwd -P)"
        export platformdir="${firmwaredir}/../platform_osx"
    ;;
    MINGW*)
        firmwaredir="$(cd $(dirname $0); pwd -P)"
        export platformdir="${firmwaredir}/../platform_win"
    ;;
    *)
        printf "\nUnknown OS: $unamestr - aborting...\n"
        exit
    ;;
esac
${platformdir}/bin/dfu-util --transfer-size $2 --device 0483:df11 --intf 0 --alt 0 --download "${firmwaredir}/build/$1" --dfuse-address=0x08000000:leave
