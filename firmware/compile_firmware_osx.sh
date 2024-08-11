#!/bin/sh
set -e

export PATH=${axoloti_runtime}/platform_osx/bin:$PATH

cd "${axoloti_firmware}"
make BOARDDEF=$1 FWOPTIONDEF=$2 -f Makefile.patch.mk clean

echo "Compiling firmware flasher... $1 $2"
cd flasher
rm -rf .dep
rm -rf flasher_build/lst
rm -rf flasher_build/obj
mkdir -p .dep
mkdir -p flasher_build/lst
mkdir -p flasher_build/obj
# FWOPTIONDEF currently not used in flasher
if ! make -j4 BOARDDEF=$1 FWOPTIONDEF=$2; then
    exit 1
fi
cd ..

echo "Compiling firmware mounter... $1 $2"
cd mounter
rm -rf .dep
rm -rf mounter_build/lst
rm -rf mounter_build/obj
mkdir -p .dep
mkdir -p mounter_build/lst
mkdir -p mounter_build/obj
# FWOPTIONDEF currently not used in mounter
if ! make -j4 BOARDDEF=$1 FWOPTIONDEF=$2 ; then
    exit 1
fi
cd ..

echo "Compiling firmware... $1 $2"
rm -rf .dep
rm -rf build/lst
rm -rf build/obj
mkdir -p .dep
mkdir -p build/lst
mkdir -p build/obj
if ! make -j4 BOARDDEF=$1 FWOPTIONDEF=$2; then
    exit 1
fi
