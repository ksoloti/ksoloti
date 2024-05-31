#!/bin/sh
export PATH=${axoloti_runtime}/platform_osx/bin:$PATH

echo "Compiling Axoloti Legacy firmware... ${axoloti_legacy_firmware}"
cd "${axoloti_legacy_firmware}"
make -f Makefile.patch clean

mkdir -p build/obj
mkdir -p build/lst
if ! make $1; then
    exit 1
fi
# rm -rf .dep
# rm -rf build/lst
# rm -rf build/obj

echo "Compiling Axoloti Legacy firmware flasher..."
cd flasher
mkdir -p flasher_build/obj
mkdir -p flasher_build/lst
make $1
# rm -rf .dep
# rm -rf flasher_build/lst
# rm -rf flasher_build/obj
cd ..

echo "Compiling Axoloti Legacy firmware mounter..."
cd mounter
mkdir -p mounter_build/obj
mkdir -p mounter_build/lst
make $1
# rm -rf .dep
# rm -rf mounter_build/lst
# rm -rf mounter_build/obj
cd ..
