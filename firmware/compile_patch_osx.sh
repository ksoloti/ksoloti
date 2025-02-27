#!/bin/sh
export PATH=${axoloti_runtime}/platform_osx/bin:$PATH
# echo "Compiling patch via ${axoloti_firmware}"
cd "${axoloti_firmware}"
make -j BOARDDEF=$1 SUBBOARDDEF=$2 LINKERFILE=$3 FWOPTIONDEF=$4 -f Makefile.patch.mk
