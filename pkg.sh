#!/bin/bash

set -e # exit immediately if anything "goes wrong"

VERSION="$(git describe --tags | grep -Po '\d*\.\d*\.\d*' 2>&1)"
VERSION_LONG="$(git describe --long --always --tags 2>&1)"
echo $VERSION
echo $VERSION_LONG

# Compile firmware and jar
./platform_linux/compile_firmware.sh
./platform_linux/compile_java.sh


# Init
rm -rf packagetemp
mkdir -p packagetemp


# Linux
java -jar ./jdks/packr-all-4.0.0.jar --verbose ./jdks/packr-linux-x64.json

rm -rf packagetemp/linux/ksoloti-${VERSION}/chibios/demos
rm -rf packagetemp/linux/ksoloti-${VERSION}/chibios/test
rm -rf packagetemp/linux/ksoloti-${VERSION}/chibios/testhal

rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/.dep
rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/.settings

rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/build/lst
rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/build/obj

rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/flasher/.dep
rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/flasher/flasher_build/lst
rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/flasher/flasher_build/obj

rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/mounter/.dep
rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/mounter/mounter_build/lst
rm -rf packagetemp/linux/ksoloti-${VERSION}/firmware/mounter/mounter_build/obj

rm -rf packagetemp/linux/ksoloti-${VERSION}/platform_linux/share
rm -rf packagetemp/linux/ksoloti-${VERSION}/platform_linux/src
rm -rf packagetemp/linux/ksoloti-${VERSION}/platform_linux/arm-none-eabi/lib/armv6-m
rm -rf packagetemp/linux/ksoloti-${VERSION}/platform_linux/arm-none-eabi/lib/armv7-ar
rm -rf packagetemp/linux/ksoloti-${VERSION}/platform_linux/arm-none-eabi/lib/armv7-m
rm -rf packagetemp/linux/ksoloti-${VERSION}/platform_linux/lib/gcc/arm-none-eabi/4.9.3/armv6-m
rm -rf packagetemp/linux/ksoloti-${VERSION}/platform_linux/lib/gcc/arm-none-eabi/4.9.3/armv7-ar
rm -rf packagetemp/linux/ksoloti-${VERSION}/platform_linux/lib/gcc/arm-none-eabi/4.9.3/armv7-m

rm -rf packagetemp/linux/ksoloti-${VERSION}/jre/jmods
rm packagetemp/linux/ksoloti-${VERSION}/jre/lib/src.zip

cd packagetemp/linux && tar -czf ../ksoloti-linux-${VERSION_LONG}.tar.gz *
cd ../..
rm -rf packagetemp/linux


# MacOS
java -jar ./jdks/packr-all-4.0.0.jar --verbose ./jdks/packr-mac-x64.json

rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/chibios/demos
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/chibios/test
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/chibios/testhal

rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/.dep
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/.settings

rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/build/lst
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/build/obj

rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/flasher/.dep
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/flasher/flasher_build/lst
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/flasher/flasher_build/obj

rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/mounter/.dep
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/mounter/mounter_build/lst
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/firmware/mounter/mounter_build/obj

rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/jre/jmods
rm packagetemp/mac/Ksoloti.app/Contents/Resources/jre/lib/src.zip

chmod +x packagetemp/mac/Ksoloti.app/Contents/Resources/jre/lib/jspawnhelper

cd packagetemp/mac && zip -q -r ../ksoloti-mac-${VERSION_LONG}.zip *
cd ../..
rm -rf packagetemp/mac


# Windows
java -jar ./jdks/packr-all-4.0.0.jar --verbose ./jdks/packr-win-x64.json

rm -rf packagetemp/win/ksoloti-${VERSION}/chibios/demos
rm -rf packagetemp/win/ksoloti-${VERSION}/chibios/test
rm -rf packagetemp/win/ksoloti-${VERSION}/chibios/testhal

rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/.dep
rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/.settings

rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/build/lst
rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/build/obj

rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/flasher/.dep
rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/flasher/flasher_build/lst
rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/flasher/flasher_build/obj

rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/mounter/.dep
rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/mounter/mounter_build/lst
rm -rf packagetemp/win/ksoloti-${VERSION}/firmware/mounter/mounter_build/obj

rm -rf packagetemp/win/ksoloti-${VERSION}/platform_win/share
rm -rf packagetemp/win/ksoloti-${VERSION}/platform_win/src
rm -rf packagetemp/win/ksoloti-${VERSION}/platform_win/arm-none-eabi/lib/armv6-m
rm -rf packagetemp/win/ksoloti-${VERSION}/platform_win/arm-none-eabi/lib/armv7-ar
rm -rf packagetemp/win/ksoloti-${VERSION}/platform_win/arm-none-eabi/lib/armv7-m
rm -rf packagetemp/win/ksoloti-${VERSION}/platform_win/lib/gcc/arm-none-eabi/4.9.3/armv6-m
rm -rf packagetemp/win/ksoloti-${VERSION}/platform_win/lib/gcc/arm-none-eabi/4.9.3/armv7-ar
rm -rf packagetemp/win/ksoloti-${VERSION}/platform_win/lib/gcc/arm-none-eabi/4.9.3/armv7-mo

rm -rf packagetemp/win/ksoloti-${VERSION}/jre/jmods
rm packagetemp/win/ksoloti-${VERSION}/jre/lib/src.zip

cd packagetemp/win && zip -q -r ../ksoloti-windows-${VERSION_LONG}.zip *
cd ../..
rm -rf packagetemp/win


# Cleanup
cd firmware && make clean
cd .. && ant clean
