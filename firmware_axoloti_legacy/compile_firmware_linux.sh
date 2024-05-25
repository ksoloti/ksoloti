#!/bin/sh
export PATH=${axoloti_runtime}/platform_linux/bin:$PATH

cd "${axoloti_legacy_firmware}"
make -f Makefile.patch clean

echo "Compiling Axoloti Legacy firmware... ${axoloti_legacy_firmware}"
mkdir -p build/obj
mkdir -p build/lst
if ! make $1 ; then
    exit 1
fi

echo "Compiling Axoloti Legacy firmware flasher..."
cd flasher
mkdir -p flasher_build/lst
mkdir -p flasher_build/obj
make $1
cd ..

echo "Compiling Axoloti Legacy firmware mounter..."
cd mounter
mkdir -p mounter_build/lst
mkdir -p mounter_build/obj
make $1
cd ..
