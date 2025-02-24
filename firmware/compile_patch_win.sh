#!/bin/sh

firmwaredir="$(dirname $(readlink -f $0))"

export axoloti_home=${axoloti_home:="$firmwaredir/.."}
export axoloti_firmware=${axoloti_firmware:="$firmwaredir"}

export PATH="${axoloti_home}/platform_win/bin:$PATH"

# echo "Compiling patch via ${axoloti_firmware}"
cd "${axoloti_firmware}"
make -j8 BOARDDEF=$1 FWOPTIONDEF=$2 -f Makefile.patch.mk
