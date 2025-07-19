#!/usr/bin/env bash

unamestr=`uname`
unamearch=`uname -m`
case "$unamestr" in
    Linux)
        currentdir="$(dirname $(readlink -f $0))"
        export axoloti_home=${axoloti_home:="$currentdir/.."}
        export axoloti_firmware=${axoloti_firmware:="$currentdir"}
        case "$unamearch" in
            aarch64|arm64)
                export PATH="${axoloti_home}/platform_linux_aarch64/bin:$PATH"
            ;;
            x86_64)
                export PATH="${axoloti_home}/platform_linux_x64/bin:$PATH"
            ;;
            *)
                printf "\nUnknown CPU architecture: $unamearch - aborting...\n"
                exit
            ;;
        esac
    ;;
    Darwin)
        currentdir="$(cd $(dirname $0); pwd -P)"
        export axoloti_home=${axoloti_home:="$currentdir/.."}
        export axoloti_firmware=${axoloti_firmware:="$currentdir"}
        case "$unamearch" in
            aarch64|arm64)
                export PATH="${axoloti_home}/platform_mac_aarch64/bin:$PATH"
            ;;
            x86_64)
                export PATH="${axoloti_home}/platform_mac_x64/bin:$PATH"
            ;;
            *)
                printf "\nUnknown CPU architecture: $unamearch - aborting...\n"
                exit
            ;;
        esac
    ;;
    *)
        printf "\nUnknown OS: $unamestr - aborting...\n"
        exit
    ;;
esac

# echo "Compiling patch via ${axoloti_firmware}"
cd "${axoloti_firmware}"
make -j8 BOARDDEF=$1 FWOPTIONDEF=$2 BUILDFILENAME=$3 -f Makefile.patch.mk