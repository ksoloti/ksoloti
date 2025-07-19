#!/usr/bin/env bash
set -e

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

if [ "$#" -eq 1 ]; then
  printf "Building all firmware modes for the current board.\n"
  BUILD_NORMAL=1
  BUILD_USBAUDIO=1 
  BUILD_SPILINK=1 
  BUILD_FLASHER=1 
  BUILD_MOUNTER=1
  BUILD_I2SCODEC=1
else
  BUILD_NORMAL=$2
  BUILD_USBAUDIO=$3 
  BUILD_SPILINK=$4 
  BUILD_FLASHER=$5 
  BUILD_MOUNTER=$6
  BUILD_I2SCODEC=$7
fi

cd "${axoloti_firmware}"
make BOARDDEF=$1 -f Makefile.patch.mk clean

if [ $1 = "BOARD_KSOLOTI_CORE" ]; then
  NAME=ksoloti
else if [ $1 = "BOARD_AXOLOTI_CORE" ]; then
  NAME=axoloti
fi
fi

FLASHER_PROJECT="$NAME"_flasher
MOUNTER_PROJECT="$NAME"_mounter

if [ $BUILD_FLASHER -eq 1 ]; then
    printf "\nCompiling $FLASHER_PROJECT\n"
    cd flasher
    export BUILDDIR=flasher_build/$FLASHER_PROJECT
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1; then
        exit 1
    fi
    cp $BUILDDIR/$FLASHER_PROJECT.* flasher_build/
    cd ..
fi

if [ $BUILD_MOUNTER -eq 1 ]; then
    printf "\nCompiling $MOUNTER_PROJECT\n"
    cd mounter
    export BUILDDIR=mounter_build/$MOUNTER_PROJECT
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1; then
        exit 1
    fi
    cp $BUILDDIR/$MOUNTER_PROJECT.* mounter_build/
    cd ..
fi

if [ $BUILD_NORMAL -eq 1 ]; then
    printf "\nCompiling $1\n"
    export BUILDDIR=build/$NAME/normal
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1; then
        exit 1
    fi
    cp $BUILDDIR/$NAME.* build
fi

if [ $BUILD_SPILINK -eq 1 ]; then
    printf "\nCompiling $1 FW_SPILINK\n"
    export BUILDDIR=build/$NAME/spilink
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1 FWOPTIONDEF=FW_SPILINK; then
        exit 1
    fi
    cp $BUILDDIR/"$NAME"_spilink.* build
fi

if [ $BUILD_USBAUDIO -eq 1 ]; then
    printf "\nCompiling $1 FW_USBAUDIO\n"
    export BUILDDIR=build/$NAME/usbaudio
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1 FWOPTIONDEF=FW_USBAUDIO; then
        exit 1
    fi
    cp $BUILDDIR/"$NAME"_usbaudio.* build
fi

if [ $BUILD_I2SCODEC -eq 1 ]; then
    printf "\nCompiling $1 FW_I2SCODEC\n"
    export BUILDDIR=build/$NAME/i2scodec
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1 FWOPTIONDEF=FW_I2SCODEC; then
        exit 1
    fi
    cp $BUILDDIR/"$NAME"_i2scodec.* build
fi