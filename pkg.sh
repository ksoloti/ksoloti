#!/bin/bash
VERSION="1.0.12"

# Compile firmware and jar
./platform_linux/compile_firmware.sh
./platform_linux/compile_java.sh


# Init
rm -rf package
mkdir -p package


# Linux
java -jar ./jdks/packr-all-4.0.0.jar --verbose ./jdks/packr-linux-x64.json

rm -rf package/linux/ksoloti-${VERSION}/chibios/demos
rm -rf package/linux/ksoloti-${VERSION}/chibios/test
rm -rf package/linux/ksoloti-${VERSION}/chibios/testhal

rm -rf package/linux/ksoloti-${VERSION}/firmware/.dep
rm -rf package/linux/ksoloti-${VERSION}/firmware/.settings

rm -rf package/linux/ksoloti-${VERSION}/firmware/build/lst
rm -rf package/linux/ksoloti-${VERSION}/firmware/build/obj

rm -rf package/linux/ksoloti-${VERSION}/firmware/flasher/.dep
rm -rf package/linux/ksoloti-${VERSION}/firmware/flasher/flasher_build/lst
rm -rf package/linux/ksoloti-${VERSION}/firmware/flasher/flasher_build/obj

rm -rf package/linux/ksoloti-${VERSION}/firmware/mounter/.dep
rm -rf package/linux/ksoloti-${VERSION}/firmware/mounter/mounter_build/lst
rm -rf package/linux/ksoloti-${VERSION}/firmware/mounter/mounter_build/obj

rm -rf package/linux/ksoloti-${VERSION}/platform_linux/share
rm -rf package/linux/ksoloti-${VERSION}/platform_linux/src
rm -rf package/linux/ksoloti-${VERSION}/platform_linux/arm-none-eabi/lib/armv6-m
rm -rf package/linux/ksoloti-${VERSION}/platform_linux/arm-none-eabi/lib/armv7-ar
rm -rf package/linux/ksoloti-${VERSION}/platform_linux/arm-none-eabi/lib/armv7-m
rm -rf package/linux/ksoloti-${VERSION}/platform_linux/lib/gcc/arm-none-eabi/4.9.3/armv6-m
rm -rf package/linux/ksoloti-${VERSION}/platform_linux/lib/gcc/arm-none-eabi/4.9.3/armv7-ar
rm -rf package/linux/ksoloti-${VERSION}/platform_linux/lib/gcc/arm-none-eabi/4.9.3/armv7-m

cd package/linux && tar -czvf ../ksoloti-linux-${VERSION}.tar.gz *
cd ../..
rm -rf package/linux


# MacOS
java -jar ./jdks/packr-all-4.0.0.jar --verbose ./jdks/packr-mac-x64.json

rm -rf package/mac/Ksoloti.app/Contents/Resources/chibios/demos
rm -rf package/mac/Ksoloti.app/Contents/Resources/chibios/test
rm -rf package/mac/Ksoloti.app/Contents/Resources/chibios/testhal

rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/.dep
rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/.settings

rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/build/lst
rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/build/obj

rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/flasher/.dep
rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/flasher/flasher_build/lst
rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/flasher/flasher_build/obj

rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/mounter/.dep
rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/mounter/mounter_build/lst
rm -rf package/mac/Ksoloti.app/Contents/Resources/firmware/mounter/mounter_build/obj

chmod +x package/mac/Ksoloti.app/Contents/Resources/jre/lib/jspawnhelper

cd package/mac && tar -czvf ../ksoloti-mac-${VERSION}.tar.gz *
cd ../..
rm -rf package/mac


# Windows
java -jar ./jdks/packr-all-4.0.0.jar --verbose ./jdks/packr-win-x64.json

rm -rf package/win/ksoloti-${VERSION}/chibios/demos
rm -rf package/win/ksoloti-${VERSION}/chibios/test
rm -rf package/win/ksoloti-${VERSION}/chibios/testhal

rm -rf package/win/ksoloti-${VERSION}/firmware/.dep
rm -rf package/win/ksoloti-${VERSION}/firmware/.settings

rm -rf package/win/ksoloti-${VERSION}/firmware/build/lst
rm -rf package/win/ksoloti-${VERSION}/firmware/build/obj

rm -rf package/win/ksoloti-${VERSION}/firmware/flasher/.dep
rm -rf package/win/ksoloti-${VERSION}/firmware/flasher/flasher_build/lst
rm -rf package/win/ksoloti-${VERSION}/firmware/flasher/flasher_build/obj

rm -rf package/win/ksoloti-${VERSION}/firmware/mounter/.dep
rm -rf package/win/ksoloti-${VERSION}/firmware/mounter/mounter_build/lst
rm -rf package/win/ksoloti-${VERSION}/firmware/mounter/mounter_build/obj

rm -rf package/win/ksoloti-${VERSION}/platform_win/share
rm -rf package/win/ksoloti-${VERSION}/platform_win/src
rm -rf package/win/ksoloti-${VERSION}/platform_win/arm-none-eabi/lib/armv6-m
rm -rf package/win/ksoloti-${VERSION}/platform_win/arm-none-eabi/lib/armv7-ar
rm -rf package/win/ksoloti-${VERSION}/platform_win/arm-none-eabi/lib/armv7-m
rm -rf package/win/ksoloti-${VERSION}/platform_win/lib/gcc/arm-none-eabi/4.9.3/armv6-m
rm -rf package/win/ksoloti-${VERSION}/platform_win/lib/gcc/arm-none-eabi/4.9.3/armv7-ar
rm -rf package/win/ksoloti-${VERSION}/platform_win/lib/gcc/arm-none-eabi/4.9.3/armv7-mo

cd package/win && zip -r ../ksoloti-windows-${VERSION}.zip *
cd ../..
rm -rf package/win


# Cleanup
cd firmware && make clean
cd .. && ant clean
