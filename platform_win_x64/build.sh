#!/usr/bin/env bash

set -e

## Obsolete... Chibios is included in the repo now
# if [ ! -d "../chibios" ]; 
# then
#     CH_VERSION=2.6.9
#     ARDIR=ChibiOS-ver${CH_VERSION}
#     ARCHIVE=${ARDIR}.zip
#     if [ ! -f ${ARCHIVE} ]; 
#     then
#         printf "\ndownloading ${ARCHIVE}\n"
# 		curl -L https://github.com/ChibiOS/ChibiOS/archive/ver${CH_VERSION}.zip > ${ARCHIVE}
#     else
#         printf "\n${ARCHIVE} already downloaded\n"
#     fi

#     unzip -q -o ${ARCHIVE}
#     rm ${ARCHIVE}
#     mv ${ARDIR} chibios
#     cd chibios/ext
#     unzip -q -o ./fatfs-0.*-patched.zip
#     cd ../../
#     mv chibios ..
# fi

if [ ! -f "bin/arm-none-eabi-gcc.exe" ];
then
    ARCHIVE=gcc-arm-none-eabi-9-2020-q2-update-win32.zip
    if [ ! -f ${ARCHIVE} ]; 
    then
        printf "\ndownloading ${ARCHIVE}\n"
        curl -L https://developer.arm.com/-/media/Files/downloads/gnu-rm/9-2020q2/$ARCHIVE > $ARCHIVE
    else
        printf "\n${ARCHIVE} already downloaded\n"
    fi    
    unzip -q -o ${ARCHIVE}
    rm ${ARCHIVE}
fi

if [ ! -f "bin/make.exe" ];
then
    printf "\ndownloading make\n"
    curl -L https://github.com/mbuilov/gnumake-windows/raw/master/gnumake-4.3-x64.exe > bin/make.exe
    # unzip -q -o make-4.3-bin.zip 
    # rm make-4.3-bin.zip
fi


if [ ! -f "bin/libiconv2.dll" ];
then
    printf "\ndownloading make-dep\n"
    curl -L http://gnuwin32.sourceforge.net/downlinks/make-dep-zip.php > make-dep.zip
    unzip -q -o make-dep.zip
    rm make-dep.zip
fi

if [ ! -f "bin/rm.exe" ];
then
    printf "\ndownloading rm\n"
    curl -L http://gnuwin32.sourceforge.net/downlinks/coreutils-bin-zip.php > coreutils-5.3.0-bin.zip
    unzip -q -o coreutils-5.3.0-bin.zip
    rm coreutils-5.3.0-bin.zip
fi

if [ ! -d "..\jdks\apache-ant-1.10.14" ];
then
    ARCHIVE=..\jdks\apache-ant-1.10.14-bin.zip
    if [ ! -f ${ARCHIVE} ]; 
    then
        printf "\ndownloading ${ARCHIVE}\n"
        curl -L http://archive.apache.org/dist/ant/binaries/${ARCHIVE} > ${ARCHIVE}
    else
        printf "\n${ARCHIVE} already downloaded\n"
    fi    

    unzip -q ${ARCHIVE}
    rm ${ARCHIVE}
fi

if [ ! -f "zadig-2.8.exe" ];
then
    ARCHIVE=zadig-2.8.exe
    if [ ! -f ${ARCHIVE} ]; 
    then
        printf "\ndownloading ${ARCHIVE}\n"
        curl -L https://github.com/pbatard/libwdi/releases/download/v1.5.0/${ARCHIVE} > ${ARCHIVE}
    else
        printf "\n${ARCHIVE} already downloaded\n"
    fi        
fi

if [ ! -f "bin/dfu-util.exe" ];
then
    ARCHIVE=dfu-util-0.11-win64.zip
    if [ ! -f ${ARCHIVE} ];
    then
        printf "\ndownloading ${ARCHIVE}\n"
        curl -L http://dfu-util.sourceforge.net/releases/${ARCHIVE} > ${ARCHIVE}
    else
        printf "\n${ARCHIVE} already downloaded\n"
    fi
	unzip -q -j -d bin ${ARCHIVE}
	rm ${ARCHIVE}
fi

cd "../jdks"

JDK_ARCHIVE_LINUX="zulu21.42.19-ca-jdk21.0.7-linux_x64.tar.gz"
if [ ! -f "${JDK_ARCHIVE_LINUX}" ];
then
    echo "##### downloading ${JDK_ARCHIVE_LINUX} #####"
    curl -L https://cdn.azul.com/zulu/bin/$JDK_ARCHIVE_LINUX > $JDK_ARCHIVE_LINUX
else
    echo "##### ${JDK_ARCHIVE_LINUX} already downloaded #####"
fi

JDK_ARCHIVE_MAC="zulu21.42.19-ca-jdk21.0.7-macosx_x64.tar.gz"
if [ ! -f "${JDK_ARCHIVE_MAC}" ];
then
    echo "##### downloading ${JDK_ARCHIVE_MAC} #####"
    curl -L https://cdn.azul.com/zulu/bin/$JDK_ARCHIVE_MAC > $JDK_ARCHIVE_MAC
else
    echo "##### ${JDK_ARCHIVE_MAC} already downloaded #####"
fi

JDK_ARCHIVE_WINDOWS="zulu21.42.19-ca-jdk21.0.7-win_x64.zip"
if [ ! -f "${JDK_ARCHIVE_WINDOWS}" ];
then
    echo "##### downloading ${JDK_ARCHIVE_WINDOWS} #####"
    curl -L https://cdn.azul.com/zulu/bin/$JDK_ARCHIVE_WINDOWS > $JDK_ARCHIVE_WINDOWS
else
    echo "##### ${JDK_ARCHIVE_WINDOWS} already downloaded #####"
fi

printf "\nDONE!\n"
