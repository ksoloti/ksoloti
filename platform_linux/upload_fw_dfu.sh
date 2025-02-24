#!/bin/bash
platformdir="$(dirname $(readlink -f $0))"
export axoloti_home=${axoloti_home:="$platformdir/.."}
export axoloti_firmware=${axoloti_firmware:="$axoloti_home/firmware"}

if [ -f "${platformdir}/bin/dfu-util" ];
then
    cd "${platformdir}/bin"
    ./dfu-util --transfer-size 4096 --device 0483:df11 -i 0 -a 0 -D "${axoloti_firmware}/build/$1" --dfuse-address=0x08000000:leave
else
    printf "\ndfu-util not found, run ./build.sh or ./build.bat in axoloti/platform_*\n"
fi
