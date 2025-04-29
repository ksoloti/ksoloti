#!/usr/bin/env bash
set -e

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

if [ "$#" -eq 2 ]; then
  printf "Building all firmware modes for the current board.\n"
  BUILD_NORMAL=1
  BUILD_USBAUDIO=1 
  BUILD_SPILINK=1 
  BUILD_FLASHER=1 
  BUILD_MOUNTER=1
  BUILD_I2SCODEC=1
else
  BUILD_NORMAL=$3
  BUILD_USBAUDIO=$4 
  BUILD_SPILINK=$5 
  BUILD_FLASHER=$6 
  BUILD_MOUNTER=$7
  BUILD_I2SCODEC=$8
fi

cd "${axoloti_firmware}"
make BOARDDEF=$1 SUBBOARDDEF=$2 -f Makefile.patch.mk clean

if [ $1 = "BOARD_KSOLOTI_CORE" ]; then
    if [ $2 = "BOARD_KSOLOTI_CORE_F427" ]; then
        NAME=ksoloti
    else if [ $2 = "BOARD_KSOLOTI_CORE_H743" ]; then
        NAME=ksoloti_h743
    else
        NAME=UNKNOWN
    fi
    fi
else if [ $1 = "BOARD_AXOLOTI_CORE" ]; then
  NAME=axoloti
fi
fi

FLASHER_PROJECT="$NAME"_flasher
MOUNTER_PROJECT="$NAME"_mounter

if [ $BUILD_FLASHER -eq 1 ]; then
    printf "\nCompiling $2 - $FLASHER_PROJECT\n"
    cd flasher
    export BUILDDIR=flasher_build/$FLASHER_PROJECT
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j BOARDDEF=$1 SUBBOARDDEF=$2; then
        exit 1
    fi
    cp $BUILDDIR/$FLASHER_PROJECT.* flasher_build/
    cd ..
fi

if [ $BUILD_MOUNTER -eq 1 ]; then
    printf "\nCompiling $2 - $MOUNTER_PROJECT\n"
    cd mounter
    export BUILDDIR=mounter_build/$MOUNTER_PROJECT
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j BOARDDEF=$1 SUBBOARDDEF=$2; then
        exit 1
    fi
    cp $BUILDDIR/$MOUNTER_PROJECT.* mounter_build/
    cd ..
fi

if [ $BUILD_NORMAL -eq 1 ]; then
    printf "\nCompiling $2\n"
    export BUILDDIR=build/$NAME/normal
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j BOARDDEF=$1 SUBBOARDDEF=$2; then
        exit 1
    fi
    cp $BUILDDIR/$NAME.* build
fi

if [ $BUILD_SPILINK -eq 1 ]; then
    printf "\nCompiling $2 FW_SPILINK\n"
    export BUILDDIR=build/$NAME/spilink
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j BOARDDEF=$1 SUBBOARDDEF=$2 FWOPTIONDEF=FW_SPILINK; then
        exit 1
    fi
    cp $BUILDDIR/"$NAME"_spilink.* build
fi

if [ $BUILD_USBAUDIO -eq 1 ]; then
    printf "\nCompiling $2 FW_USBAUDIO\n"
    export BUILDDIR=build/$NAME/usbaudio
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j BOARDDEF=$1  SUBBOARDDEF=$2 FWOPTIONDEF=FW_USBAUDIO; then
        exit 1
    fi
    cp $BUILDDIR/"$NAME"_usbaudio.* build
fi

if [ $BUILD_I2SCODEC -eq 1 ]; then
    printf "\nCompiling $2 FW_I2SCODEC\n"
    export BUILDDIR=build/$NAME/i2scodec
    mkdir -p $BUILDDIR/.dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j BOARDDEF=$1  SUBBOARDDEF=$2 FWOPTIONDEF=FW_I2SCODEC; then
        exit 1
    fi
    cp $BUILDDIR/"$NAME"_i2scodec.* build
fi