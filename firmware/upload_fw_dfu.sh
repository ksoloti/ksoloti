#!/usr/bin/env bash

unamestr=`uname`
unamearch=`uname -m`
case "$unamestr" in
    Linux)
        firmwaredir="$(dirname $(readlink -f $0))"
        case "$unamearch" in
            aarch64)
                export platformdir="${firmwaredir}/../platform_linux_aarch64"
            ;;
            x86_64)
                export platformdir="${firmwaredir}/../platform_linux_x64"
            ;;
            *)
                printf "\nUnknown CPU architecture: $unamearch - aborting...\n"
                exit
            ;;
        esac
    ;;
    Darwin)
        firmwaredir="$(cd $(dirname $0); pwd -P)"
        export platformdir="${firmwaredir}/../platform_macos"
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
${platformdir}/bin/dfu-util --transfer-size 4096 --device 0483:df11 --intf 0 --alt 0 --download "${firmwaredir}/build/$1" --dfuse-address=0x08000000:leave
