#!/bin/sh
set -e
platformdir="$(cd $(dirname $0); pwd -P)"
export axoloti_home=${axoloti_home:="$platformdir/.."}
export axoloti_firmware=${axoloti_firmware:="$axoloti_home/firmware"}

cd "${axoloti_firmware}"
"${axoloti_firmware}/compile_firmware_osx.sh" "$@"