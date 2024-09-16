#!/bin/sh
set -e

export PATH=${axoloti_runtime}/platform_osx/bin:$PATH

cd "${axoloti_firmware}"
make BOARDDEF=$1 -f Makefile.patch.mk clean

echo "Compiling firmware flasher... $1"
cd flasher
mkdir -p .dep
mkdir -p flasher_build/lst
mkdir -p flasher_build/obj
rm -f .dep/*
rm -f flasher_build/lst/*
rm -f flasher_build/obj/*
# FWOPTIONDEF currently not used in flasher
if ! make -j16 BOARDDEF=$1; then
    exit 1
fi
cd ..

echo "Compiling firmware mounter... $1"
cd mounter
mkdir -p .dep
mkdir -p mounter_build/lst
mkdir -p mounter_build/obj
rm -f .dep/*
rm -f mounter_build/lst/*
rm -f mounter_build/obj/*
# FWOPTIONDEF currently not used in mounter
if ! make -j16 BOARDDEF=$1; then
    exit 1
fi
cd ..

echo "Compiling firmware... $1"
mkdir -p .dep
mkdir -p build/lst
mkdir -p build/obj
rm -f .dep/*
rm -f build/lst/*
rm -f build/obj/*
if ! make -j16 BOARDDEF=$1; then
    exit 1
fi

echo "Compiling firmware... $1 FW_SPILINK"
mkdir -p .dep
mkdir -p build/lst
mkdir -p build/obj
rm -f .dep/*
rm -f build/lst/*
rm -f build/obj/*
if ! make -j16 BOARDDEF=$1 FWOPTIONDEF=FW_SPILINK; then
    exit 1
fi
