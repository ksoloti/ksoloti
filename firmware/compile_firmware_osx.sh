#!/bin/sh
set -e

export PATH=${axoloti_runtime}/platform_osx/bin:$PATH

BUILD_NORMAL=$2
BUILD_USBAUDIO=$3 
BUILD_SPILINK=$4 
BUILD_FLASHER=$5 
BUILD_MOUNTER=$6

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
    echo "\nCompiling $1 - $FLASHER_PROJECT"
    cd flasher
    mkdir -p .dep
    mkdir -p flasher_build/$FLASHER_PROJECT/lst
    mkdir -p flasher_build/$FLASHER_PROJECT/obj
    if !make -j8 BOARDDEF=$1; then
        exit 1
    fi
    cp flasher_build/$FLASHER_PROJECT/$FLASHER_PROJECT.* flasher_build/
    cd ..
fi

if [ $BUILD_MOUNTER -eq 1 ]; then
    echo "\nCompiling $1 - $MOUNTER_PROJECT"
    cd mounter
    mkdir -p .dep
    mkdir -p mounter_build/$MOUNTER_PROJECT/lst
    mkdir -p mounter_build/$MOUNTER_PROJECT/obj
    if ! make -j8 BOARDDEF=$1; then
        exit 1
    fi
    cp mounter_build/$MOUNTER_PROJECT/$MOUNTER_PROJECT.* mounter_build/
    cd ..
fi

if [ $BUILD_NORMAL -eq 1 ]; then
    echo "\nCompiling $1"
    export BUILDDIR=build/$NAME/normal
    mkdir -p .dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1; then
        exit 1
    fi
    cp $BUILDDIR/$NAME.* build
fi

if [ $BUILD_SPILINK -eq 1 ]; then
    echo "\nCompiling $1 FW_SPILINK"
    export BUILDDIR=build/$NAME/spilink
    mkdir -p .dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1 FWOPTIONDEF=FW_SPILINK; then
        exit 1
    fi
    cp $BUILDDIR/"$NAME"_spilink.* build
fi

if [ $BUILD_USBAUDIO -eq 1 ]; then
    echo "\nCompiling $1 FW_USBAUDIO"
    export BUILDDIR=build/$NAME/usbaudio
    mkdir -p .dep
    mkdir -p $BUILDDIR/lst
    mkdir -p $BUILDDIR/obj
    if ! make -j8 BOARDDEF=$1 FWOPTIONDEF=FW_USBAUDIO; then
        exit 1
    fi
    cp $BUILDDIR/"$NAME"_usbaudio.* build
fi