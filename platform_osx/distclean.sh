#!/bin/bash

# this removes unnecessary code and archives
# created by running build.sh,
# but leaves all the binary dependencies
# so the whole axoloti folder can be moved to a 
# different machine that does not have xcode etc installed.

set -e

PLATFORM_ROOT="$(cd $(dirname $0); pwd -P)"

cd "$PLATFORM_ROOT"

rm src/ChibiOS_*.zip
rm src/dfu-util-0.*.tar.gz
rm src/libusb-1.0.*.tar.bz2
rm src/make-*.tar.gz

rm -rv src/dfu-util-0.*
rm -rv src/libusb-1.0.*
rm -rv src/make-*

rm gcc-arm-none-eabi-9-2020-q2-update-mac.tar.bz2

rm -rv lib/
rm -rv share/
rm -rv bin/
rm -rv include/
