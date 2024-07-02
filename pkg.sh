#!/bin/bash

set -e # exit immediately if anything "goes wrong"

START=$(date +%s)

VERSION="$(git describe --tags | grep -Po '\d*\.\d*\.\d*' 2>&1)"
VERSION_LONG="$(git describe --long --always --tags 2>&1)"
echo $VERSION
echo $VERSION_LONG

CUSTOMNAME=
if [ $# -eq 1 ]
  then
    CUSTOMNAME=$1
fi
echo $CUSTOMNAME

# ----- Compile firmware and jar
./qlean.sh
./kompile_shortcut.sh
ant clean
./platform_linux/compile_java.sh
ant clean


# ----- Init
mkdir -p packagetemp
rm -rf packagetemp/*


# ----- Linux
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
rm -rf packagetemp/linux/ksoloti-${VERSION}/jre/demo
rm -rf packagetemp/linux/ksoloti-${VERSION}/jre/man
rm packagetemp/linux/ksoloti-${VERSION}/jre/lib/src.zip

cd packagetemp/linux && tar -czf ../ksoloti_patcher-linux-${VERSION_LONG}${CUSTOMNAME}.tar.gz *
cd ../..
rm -rf packagetemp/linux


# ----- MacOS x64
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

rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/platform_osx/share
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/platform_osx/src
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/platform_osx/arm-none-eabi/lib/armv6-m
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/platform_osx/arm-none-eabi/lib/armv7-ar
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/platform_osx/arm-none-eabi/lib/armv7-m
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/platform_osx/lib/gcc/arm-none-eabi/4.9.3/armv6-m
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/platform_osx/lib/gcc/arm-none-eabi/4.9.3/armv7-ar
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/platform_osx/lib/gcc/arm-none-eabi/4.9.3/armv7-mo

rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/jre/jmods
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/jre/demo
rm -rf packagetemp/mac/Ksoloti.app/Contents/Resources/jre/man
rm packagetemp/mac/Ksoloti.app/Contents/Resources/jre/lib/src.zip

chmod +x packagetemp/mac/Ksoloti.app/Contents/Resources/jre/lib/jspawnhelper

cd packagetemp/mac && tar -czf ../ksoloti_patcher-mac-${VERSION_LONG}${CUSTOMNAME}.tar.gz *
cd ../..
rm -rf packagetemp/mac


# ----- Windows
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
rm -rf packagetemp/win/ksoloti-${VERSION}/jre/demo
rm -rf packagetemp/win/ksoloti-${VERSION}/jre/man
rm packagetemp/win/ksoloti-${VERSION}/jre/lib/src.zip

cd packagetemp/win && zip -q -r ../ksoloti_patcher-windows-${VERSION_LONG}${CUSTOMNAME}.zip *
cd ../..
rm -rf packagetemp/win


# ----- Cleanup
cd firmware && make clean
cd .. && ant clean

END=$(date +%s)
echo "All done! Elapsed time: $(($END-$START)) seconds."