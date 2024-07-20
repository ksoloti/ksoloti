#!/bin/sh
set -e

export PATH=${axoloti_runtime}/platform_linux/bin:$PATH

cd "${axoloti_firmware}"
make -f Makefile.patch.mk clean

echo "Compiling firmware... $1"
rm -rf .dep
rm -rf build/lst
rm -rf build/obj
mkdir -p .dep
mkdir -p build/lst
mkdir -p build/obj
if ! make BOARDDEF=-D$1; then
    exit 1
fi

echo "Compiling firmware flasher... $1"
cd flasher
rm -rf .dep
rm -rf flasher_build/lst
rm -rf flasher_build/obj
mkdir -p .dep
mkdir -p flasher_build/lst
mkdir -p flasher_build/obj
make BOARDDEF=-D$1
cd ..

echo "Compiling firmware mounter... $1"
cd mounter
rm -rf .dep
rm -rf mounter_build/lst
rm -rf mounter_build/obj
mkdir -p .dep
mkdir -p mounter_build/lst
mkdir -p mounter_build/obj
make BOARDDEF=-D$1
cd ..