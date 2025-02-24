#!/bin/sh
set -e
platformdir="$(dirname $(readlink -f $0))"
export axoloti_home=${axoloti_home:="$platformdir/.."}
export axoloti_firmware=${axoloti_firmware:="$axoloti_home/firmware"}

cd "${axoloti_firmware}"
"${axoloti_firmware}/compile_patch_win.sh" "$@"
