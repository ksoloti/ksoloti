#!/usr/bin/env bash

unamestr=`uname`
case "$unamestr" in
    Linux)
        currentdir="$(dirname $(readlink -f $0))"
        export axoloti_home=${axoloti_home:="$currentdir/.."}
        export axoloti_firmware=${axoloti_firmware:="$currentdir"}
        export PATH="${axoloti_home}/platform_linux/bin:$PATH"
    ;;
    Darwin)
        currentdir="$(cd $(dirname $0); pwd -P)"
        export axoloti_home=${axoloti_home:="$currentdir/.."}
        export axoloti_firmware=${axoloti_firmware:="$currentdir"}
        export PATH="${axoloti_home}/platform_osx/bin:$PATH"
    ;;
    *)
        printf "\nUnknown OS: $unamestr - aborting...\n"
        exit
    ;;
esac

# echo "Compiling patch via ${axoloti_firmware}"
cd "${axoloti_firmware}"
make -j BOARDDEF=$1 SUBBOARDDEF=$2 LINKERFILE=$3 FWOPTIONDEF=$4 -f Makefile.patch.mk
