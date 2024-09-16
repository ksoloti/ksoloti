#!/bin/sh
export PATH=${axoloti_runtime}/platform_linux/bin:$PATH
# echo "Compiling patch via ${axoloti_firmware}"
cd "${axoloti_firmware}"
make -j16 BOARDDEF=$1 FWOPTIONDEF=$2 -f Makefile.patch.mk