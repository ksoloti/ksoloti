#!/bin/sh
set -e
platformdir="$(cd $(dirname $0); pwd -P)"

export axoloti_release=${axoloti_release:="$platformdir/.."}
export axoloti_runtime=${axoloti_runtime:="$platformdir/.."}
export axoloti_firmware=${axoloti_firmware:="$axoloti_release/firmware"}
export axoloti_legacy_firmware=${axoloti_firmware:="$axoloti_release/firmware_axoloti_legacy"}
export axoloti_home=${axoloti_home:="$platformdir/.."}

cd "${axoloti_firmware}"
"${axoloti_firmware}/compile_firmware_osx.sh" $1
cd ..
"${axoloti_legacy_firmware}/compile_firmware_osx.sh" $1
