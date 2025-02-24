#!/bin/bash
set -e # exit immediately if anything "goes wrong"

START=$(date +%s)

platform='unknown'
unamestr=`uname`
case "$unamestr" in
	Linux)
		platform='linux'
		rootdir="$(dirname $(readlink -f $0))"
	;;

	Darwin)
		platform='mac'
		rootdir="$(cd $(dirname $0); pwd -P)"
	;;

	MINGW*)
		platform='windows'
		rootdir="$(cd $(dirname $0); pwd -P)"
	;;

    *)
        printf "\nUnknown OS: $unamestr - aborting...\n"
        exit
    ;;
esac


VERSION="$(git describe --tags | grep -Po '\d*\.\d*\.\d*' 2>&1)"
VERSION_LONG="$(git describe --long --always --tags 2>&1)"
CUSTOMLABEL=

if [ $# -eq 1 ]; then
    VERSION=$1
    DASH="-"
    CUSTOMLABEL=$1${DASH}
fi

printf "$VERSION\n"
printf "$VERSION_LONG\n"

# ----- Windows: Check if 7-Zip is installed
case "$platform" in
    windows)
        #printf "\nJava build currently not supported on Windows.\n"
        DIRECTORY="C:/Program Files/7-Zip"
        if [ -d "$DIRECTORY" ]; then
            printf "\n$DIRECTORY found.\n"
        else
            printf "\n$DIRECTORY does not exist. Please install 7-Zip in the default location.\n"
            exit 1
        fi
    ;;
esac


remove_temp_files()
{
    rm -rf ./chibios/demos
    rm -rf ./chibios/test
    rm -rf ./chibios/testhal

    rm -rf ./firmware/.settings
    rm -rf ./firmware/build/*oloti
    rm -rf ./firmware/flasher/flasher_build/*oloti_flasher
    rm -rf ./firmware/mounter/mounter_build/*oloti_mounter

    rm     ./firmware/build/*.dmp
    rm     ./firmware/build/*.hex
    # rm     ./firmware/build/*.list
    rm     ./firmware/build/*.map

    rm     ./firmware/*/*_build/*.dmp
    rm     ./firmware/*/*_build/*.hex
    # rm     ./firmware/*/*_build/*.list
    rm     ./firmware/*/*_build/*.map


    rm -rf ./platform_*/share
    rm -rf ./platform_*/src
    rm -rf ./platform_*/arm-none-eabi/lib/armv6-m
    rm -rf ./platform_*/arm-none-eabi/lib/armv7-ar
    rm -rf ./platform_*/arm-none-eabi/lib/armv7-m
    rm -rf ./platform_*/arm-none-eabi/lib/cortex-m7
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/4.9.3/armv6-m
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/4.9.3/armv7-ar
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/4.9.3/armv7-m
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/4.9.3/cortex-m7

    rm -rf ./jre/jmods
    rm -rf ./jre/demo
    rm -rf ./jre/man
    rm     ./jre/lib/src.zip
}


# ----- Compile firmware

sh ./qlean.sh
sh ./rrenew_permissions.sh
sh ./kompile_shortcut.sh


# ----- Compile jar (OS differentiation in case of necessary changes on certain systems)
case "$platform" in
        mac)
            ant -q clean
            ant -q
        ;;
        linux)
            ant -q clean
            ant -q
        ;;
        windows)
            ant -q clean
            ant -q
        ;;
esac


# ----- Init, clean temp folder
mkdir -p packagetemp
rm -rf packagetemp/*


# ----- Linux
java -jar ./jdks/packr-all-4.0.0.jar --verbose --output ./packagetemp/linux/ksoloti-${VERSION} -- ./jdks/packr-linux-x64.json

cd ./packagetemp/linux/ksoloti-${VERSION} 
remove_temp_files
cd ..

tar -czf ../ksoloti_patcher-linux-${CUSTOMLABEL}${VERSION_LONG}.tar.gz *

cd ../..
rm -rf ./packagetemp/linux



# ----- MacOS x64
java -jar ./jdks/packr-all-4.0.0.jar --verbose --output ./packagetemp/mac/Ksoloti-${VERSION}.app -- ./jdks/packr-mac-x64.json

cd ./packagetemp/mac/Ksoloti-${VERSION}.app/Contents/Resources
remove_temp_files

chmod 755 ./jre/lib/jspawnhelper
chmod 755 ./firmware/*.mk
chmod -R 755 ./platform_osx/bin/*
chmod -R 755 ./platform_osx/*/bin/*
cd ../../..

tar -czf ../ksoloti_patcher-mac-${CUSTOMLABEL}${VERSION_LONG}.tar.gz *

cd ../..
rm -rf ./packagetemp/mac


# ----- Windows
java -jar ./jdks/packr-all-4.0.0.jar --verbose --output ./packagetemp/win/ksoloti-${VERSION} -- ./jdks/packr-win-x64.json

cd ./packagetemp/win/ksoloti-${VERSION} 
remove_temp_files
rm -rf ./platform_win/apache-ant-*
cd ..

# ----- Compress win package (depending on what system we're building on)
case "$platform" in
        mac)
            zip -q -r ../ksoloti_patcher-windows-${CUSTOMLABEL}${VERSION_LONG}.zip *
        ;;
        linux)
            zip -q -r ../ksoloti_patcher-windows-${CUSTOMLABEL}${VERSION_LONG}.zip *
        ;;
        windows)
            # apply icon
            ../../jdks/rcedit-x64.exe ./ksoloti-${VERSION}/Ksoloti.exe --set-icon ../../src/main/java/resources/ksoloti_icon.ico --set-product-version "${VERSION}"
            # zip using 7-zip
            "C:/Program Files/7-Zip/7z.exe" a -tzip ../ksoloti_patcher-windows-${CUSTOMLABEL}${VERSION_LONG}.zip *
        ;;
esac
cd ../..
rm -rf ./packagetemp/win


END=$(date +%s)
printf "\nAll done! Elapsed time: $(((END - START) / 60)) min $(((END - START) % 60)) sec.\n"
