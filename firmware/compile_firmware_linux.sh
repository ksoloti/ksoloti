#!/bin/sh
set -e

export PATH=${axoloti_runtime}/platform_linux/bin:$PATH

cd "${axoloti_firmware}"
make -f Makefile.patch clean

echo "Compiling Ksoloti firmware... ${axoloti_firmware}"
mkdir -p build/obj
mkdir -p build/lst
if ! make $1 ; then
    exit 1
fi
rm -rf .dep
rm -rf build/obj
rm -rf build/lst

echo "Compiling Ksoloti firmware flasher..."
cd flasher
mkdir -p flasher_build/lst
mkdir -p flasher_build/obj
make $1
rm -rf .dep
rm -rf flasher_build/obj
rm -rf flasher_build/lst
cd ..

echo "Compiling Ksoloti firmware mounter..."
cd mounter
mkdir -p mounter_build/lst
mkdir -p mounter_build/obj
make $1
rm -rf .dep
rm -rf mounter_build/obj
rm -rf mounter_build/lst
cd ..
