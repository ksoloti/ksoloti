#!/bin/sh
export PATH=${axoloti_runtime}/platform_win/bin:$PATH
# echo "Compiling patch via ${axoloti_firmware}"
cd "${axoloti_firmware}"
make -j8 BOARDDEF=$1 SUBBOARDDEF=$2  LINKERFILE=$3 FWOPTIONDEF=$4 -f Makefile.patch.mk
