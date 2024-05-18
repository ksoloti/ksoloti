#!/bin/bash

set -e

if [ ! -d "../chibios" ]; 
then
    CH_VERSION=2.6.9
    ARDIR=ChibiOS-ver${CH_VERSION}
    ARCHIVE=${ARDIR}.zip
    if [ ! -f ${ARCHIVE} ]; 
    then
        echo "downloading ${ARCHIVE}"
		curl -L https://github.com/ChibiOS/ChibiOS/archive/ver${CH_VERSION}.zip > ${ARCHIVE}
    else
        echo "${ARCHIVE} already downloaded"
    fi

    unzip -q -o ${ARCHIVE}
    rm ${ARCHIVE}
    mv ${ARDIR} chibios
    cd chibios/ext
    unzip -q -o ./fatfs-0.9-patched.zip
    cd ../../
    mv chibios ..
fi

if [ ! -f "bin/arm-none-eabi-gcc.exe" ];
then
    ARCHIVE=gcc-arm-none-eabi-4_9-2015q2-20150609-win32.zip
    if [ ! -f ${ARCHIVE} ]; 
    then
        echo "downloading ${ARCHIVE}"
        curl -L https://launchpad.net/gcc-arm-embedded/4.9/4.9-2015-q2-update/+download/${ARCHIVE} > ${ARCHIVE}
    else
        echo "${ARCHIVE} already downloaded"
    fi    
    unzip -q -o ${ARCHIVE}
    rm ${ARCHIVE}
fi

if [ ! -f "bin/make.exe" ];
then
    ARCHIVE=make-bin-win64.zip
    echo "downloading make"
    curl -L https://github.com/maweil/MakeForWindows/releases/download/v4.4.1/make-bin-win64.zip > make-4.4.1-bin.zip
    # curl -L https://downloads.sourceforge.net/project/ezwinports/make-4.4.1-without-guile-w32-bin.zip > make-4.4.1-bin.zip
    unzip -q -o make-4.4.1-bin.zip
    cp make-4.4.1/dist/* bin/
    rm make-4.4.1-bin.zip
    rm -rf make-4.4.1/
fi


if [ ! -f "bin/libiconv2.dll" ];
then
    echo "downloading make-dep"
    curl -L http://gnuwin32.sourceforge.net/downlinks/make-dep-zip.php > make-3.81-dep.zip
    unzip -q -o make-3.81-dep.zip
    rm make-3.81-dep.zip
fi

if [ ! -f "bin/rm.exe" ];
then
    echo "downloading rm"
    curl -L http://gnuwin32.sourceforge.net/downlinks/coreutils-bin-zip.php > coreutils-5.3.0-bin.zip
    unzip -q -o coreutils-5.3.0-bin.zip
    rm coreutils-5.3.0-bin.zip
fi

if [ ! -d "apache-ant-1.10.14" ];
then
    ARCHIVE=apache-ant-1.10.14-bin.zip
    if [ ! -f ${ARCHIVE} ]; 
    then
        echo "downloading ${ARCHIVE}"
        curl -L http://archive.apache.org/dist/ant/binaries/${ARCHIVE} > ${ARCHIVE}
    else
        echo "${ARCHIVE} already downloaded"
    fi    

    unzip -q ${ARCHIVE}
    rm ${ARCHIVE}
fi

if [ ! -f "zadig-2.8.exe" ];
then
    ARCHIVE=zadig-2.8.exe
    if [ ! -f ${ARCHIVE} ]; 
    then
        echo "downloading ${ARCHIVE}"
        curl -L https://github.com/pbatard/libwdi/releases/download/v1.5.0/${ARCHIVE} > ${ARCHIVE}
    else
        echo "${ARCHIVE} already downloaded"
    fi        
fi

if [ ! -f "bin/dfu-util-static.exe" ];
then
    ARCHIVE=dfu-util-0.9-win64.zip
    if [ ! -f ${ARCHIVE} ];
    then
        echo "downloading ${ARCHIVE}"
        curl -L http://dfu-util.sourceforge.net/releases/${ARCHIVE} > ${ARCHIVE}
    else
        echo "${ARCHIVE} already downloaded"
    fi
	unzip -q -j -d bin ${ARCHIVE}
	rm ${ARCHIVE}
fi

echo "DONE!"
