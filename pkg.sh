#!/usr/bin/env bash
set -e # exit immediately if anything "goes wrong"

START=$(date +%s)

platform='unknown'
unamestr=`uname`
case "$unamestr" in
	Linux)
		platform='linux'
	;;

	Darwin)
		platform='mac'
	;;

	MINGW*)
		platform='windows'
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

    rm     ./firmware/build/*.dmp
    rm     ./firmware/build/*.hex
    rm     ./firmware/build/*.list
    rm     ./firmware/build/*.map

    rm     ./firmware/*/*_build/*.dmp
    rm     ./firmware/*/*_build/*.hex
    rm     ./firmware/*/*_build/*.list
    rm     ./firmware/*/*_build/*.map

    rm -rf ./platform_*/build.sh
    rm -rf ./platform_*/notes.txt
    rm -rf ./platform_win*/build*.bat
    rm -rf ./platform_win*/make_dist.bat
    rm -rf ./platform_*/share
    rm -rf ./platform_*/src

    rm -rf ./platform_*/arm-none-eabi/lib/thumb/nofp
    rm -rf ./platform_*/arm-none-eabi/lib/thumb/v6*
    rm -rf ./platform_*/arm-none-eabi/lib/thumb/v7
    rm -rf ./platform_*/arm-none-eabi/lib/thumb/v7-*
    rm -rf ./platform_*/arm-none-eabi/lib/thumb/v7+*
    rm -rf ./platform_*/arm-none-eabi/lib/thumb/v7e-m
    rm -rf ./platform_*/arm-none-eabi/lib/thumb/v7e-m+dp
    rm -rf ./platform_*/arm-none-eabi/lib/thumb/v7ve*
    rm -rf ./platform_*/arm-none-eabi/lib/thumb/v8*

    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/nofp
    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/v6*
    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/v7
    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/v7-*
    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/v7+*
    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/v7e-m
    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/v7e-m+dp
    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/v7ve*
    rm -rf ./platform_*/arm-none-eabi/include/c++/*/arm-none-eabi/thumb/v8*

    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/nofp
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/v6*
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/v7
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/v7-*
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/v7+*
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/v7e-m
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/v7e-m+dp
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/v7ve*
    rm -rf ./platform_*/lib/gcc/arm-none-eabi/*/thumb/v8*

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
            ant -q clean && ant -q
        ;;
        linux)
            ant -q clean && ant -q
        ;;
        windows)
            ant -q clean && ant -q
        ;;
esac


# ----- Init, clean temp folder
mkdir -p packagetemp
rm -rf packagetemp/*


# ----- Linux x64
java -jar ./jdks/packr-all-4.0.0.jar --verbose --output ./packagetemp/linux_x64/ksoloti-${VERSION} -- ./jdks/packr-linux_x64.json

cd ./packagetemp/linux_x64/ksoloti-${VERSION} 
remove_temp_files
cd ..

tar -czf ../ksoloti_patcher-linux_x64-${CUSTOMLABEL}${VERSION_LONG}.tar.gz *

cd ../..
rm -rf ./packagetemp/linux_x64


# ----- Linux aarch64
java -jar ./jdks/packr-all-4.0.0.jar --verbose --output ./packagetemp/linux_aarch64/ksoloti-${VERSION} -- ./jdks/packr-linux_aarch64.json

cd ./packagetemp/linux_aarch64/ksoloti-${VERSION} 
remove_temp_files
cd ..

tar -czf ../ksoloti_patcher-linux_aarch64-${CUSTOMLABEL}${VERSION_LONG}.tar.gz *

cd ../..
rm -rf ./packagetemp/linux_aarch64


# ----- MacOS x64
java -jar ./jdks/packr-all-4.0.0.jar --verbose --output ./packagetemp/mac_x64/Ksoloti-${VERSION}.app -- ./jdks/packr-mac_x64.json

cp ./jdks/template_Info.plist ./packagetemp/mac_x64/Ksoloti-${VERSION}.app/Contents/Info.plist
cd ./packagetemp/mac_x64/Ksoloti-${VERSION}.app/Contents/Resources
remove_temp_files

chmod 755 ./jre/lib/jspawnhelper
chmod 755 ./firmware/*.mk
chmod -R 755 ./platform_mac_x64/bin/*
chmod -R 755 ./platform_mac_x64/*/bin/*
cd ../../..

tar -czf ../ksoloti_patcher-mac-${CUSTOMLABEL}${VERSION_LONG}.tar.gz *

cd ../..
rm -rf ./packagetemp/mac_x64

# # ----- MacOS aarch64 (arm64)
# java -jar ./jdks/packr-all-4.0.0.jar --verbose --output ./packagetemp/mac_aarch64/Ksoloti-${VERSION}.app -- ./jdks/packr-mac_aarch64.json

# cp ./jdks/template_Info.plist ./packagetemp/mac_aarch64/Ksoloti-${VERSION}.app/Contents/Info.plist
# cd ./packagetemp/mac_aarch64/Ksoloti-${VERSION}.app/Contents/Resources
# remove_temp_files

# chmod 755 ./jre/lib/jspawnhelper
# chmod 755 ./firmware/*.mk
# chmod -R 755 ./platform_mac_aarch64/bin/*
# chmod -R 755 ./platform_mac_aarch64/*/bin/*
# cd ../../..

# tar -czf ../ksoloti_patcher-mac_aarch64-${CUSTOMLABEL}${VERSION_LONG}.tar.gz *

# cd ../..
# rm -rf ./packagetemp/mac_aarch64

# ----- Windows x64
java -jar ./jdks/packr-all-4.0.0.jar --verbose --output ./packagetemp/win_x64/ksoloti-${VERSION} -- ./jdks/packr-win_x64.json

cd ./packagetemp/win_x64/ksoloti-${VERSION} 
remove_temp_files
cd ..

# ----- Compress win package (depending on what system we're building on)
case "$platform" in
        mac)
            zip -q -r ../ksoloti_patcher-windows_x64-${CUSTOMLABEL}${VERSION_LONG}.zip *
        ;;
        linux)
            # apply icon (requires wine! Wine only runs 32bit so we need to use rcedit-x86.exe)
            wine "../../jdks/rcedit-x86.exe" "./ksoloti-${VERSION}/Ksoloti.exe" --set-icon "../../src/main/java/resources/appicons/ksoloti_icon.ico" --set-product-version "${VERSION}"
            zip -q -r ../ksoloti_patcher-windows_x64-${CUSTOMLABEL}${VERSION_LONG}.zip *
        ;;
        windows)
            # apply icon
            ../../jdks/rcedit-x64.exe ./ksoloti-${VERSION}/Ksoloti.exe --set-icon ../../src/main/java/resources/appicons/ksoloti_icon.ico --set-product-version "${VERSION}"
            # zip using 7-zip
            "C:/Program Files/7-Zip/7z.exe" a -tzip ../ksoloti_patcher-windows_x64-${CUSTOMLABEL}${VERSION_LONG}.zip *
        ;;
esac
cd ../..
rm -rf ./packagetemp/win_x64


END=$(date +%s)
printf "\nAll done! Elapsed time: $(((END - START) / 60)) min $(((END - START) % 60)) sec.\n"
