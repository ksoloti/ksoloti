#!/usr/bin/env bash

set -e

echo -e "\n\nKsoloti build script for Linux"
echo -e "This will build Ksoloti (I think)"
echo -e "Use at your own risk\n"
echo -e "Some packages will be installed with apt-get,"
echo -e "and all users will be granted permission to access some USB devices"
echo -e "For this you'll require sudo rights and need to enter your password...\n"
echo -e "Press RETURN to continue\nCTRL-C if you are unsure!\n"
read

PLATFORM_ROOT="$(cd $(dirname $0); pwd -P)"

ARCH=$(uname -m | sed 's/x86_//;s/i[3-6]86/32/')
if [ -f /etc/lsb-release ]; then
    . /etc/lsb-release
    OS=$DISTRIB_ID
elif [ -f /etc/debian_version ]; then
    OS=Debian  # XXX or Ubuntu??
    if [ -n "`grep 8.6 /etc/debian_version`" ] && [ -z "`uname -m | grep x86_64`" ]; then
      OS=DebianJessie32bit
    fi

elif [ -f /etc/arch-release ]; then
    OS=Archlinux
elif [ -f /etc/gentoo-release ]; then
    OS=Gentoo
elif [ -f /etc/fedora-release ]; then
    OS=Fedora
else
    OS=$(uname -s)
fi

case $OS in
    Ubuntu|Debian|DebianJessie32bit|MX)
        echo "apt-get install -y libtool libudev-dev automake autoconf curl lib32z1 lib32ncurses5 lib32bz2-1.0 build-essential"
      if [ $OS==DebianJessie32bit ]; then
            sudo apt-get install -y libtool libudev-dev automake autoconf \
               ant curl build-essential
      else
            sudo apt-get install -y libtool libudev-dev automake autoconf \
               ant curl lib32z1 lib32ncurses5 build-essential
      fi

        # On more recent versions of Ubuntu
        # the libbz2 and libncurses5 packages are multi-arch
        install_lib_bz2() {
            if [ $ARCH=aarch64 ]; then
              sudo apt-get install -y libncurses5-dev
              sudo apt-get install -y libbz2-dev
            else
              sudo apt-get install -y lib32bz2-1.0
              sudo apt-get install -y lib32ncurses5
            fi
        }
        set +e
        if ! install_lib_bz2; then
            set -e
            sudo dpkg --add-architecture i386
            sudo apt-get install -y libbz2-1.0:i386 libncurses5:i386
        fi
        ;;


    Archlinux|Arch)
        echo "pacman -Syy"
        sudo pacman -Syy
        echo "pacman -S --noconfirm apache-ant libtool automake autoconf curl lib32-ncurses lib32-bzip2"
        sudo pacman -S --noconfirm apache-ant libtool automake autoconf curl \
             lib32-ncurses lib32-bzip2
        ;;
    Gentoo)
	echo "detected Gentoo"
	;;
    Fedora)
        echo "detected Fedora"
        sudo dnf group install "Development Tools"
        sudo dnf -y install libusb dfu-util libtool libudev-devel automake autoconf \
        ant curl ncurses-libs bzip2
        ;;
    *)
        echo "Cannot handle dist: $OS"
        exit
        ;;
esac

cd "$PLATFORM_ROOT"

./add_udev_rules.sh

mkdir -p "${PLATFORM_ROOT}/bin"
mkdir -p "${PLATFORM_ROOT}/lib"
mkdir -p "${PLATFORM_ROOT}/src"



if [ ! -f "$PLATFORM_ROOT/bin/arm-none-eabi-gcc" ];
then
    cd "${PLATFORM_ROOT}/src"
    if [ $ARCH=aarch64 ]; then
      ARCHIVE=gcc-arm-none-eabi-9-2020-q2-update-aarch64-linux.tar.bz2
    else
      ARCHIVE=gcc-arm-none-eabi-9-2020-q2-update-x86_64-linux.tar.bz2
    fi
    if [ ! -f ${ARCHIVE} ];
    then
        echo "downloading ${ARCHIVE}"
        echo "curl -L https://developer.arm.com/-/media/Files/downloads/gnu-rm/9-2020q2/$ARCHIVE > $ARCHIVE"
        curl -L https://developer.arm.com/-/media/Files/downloads/gnu-rm/9-2020q2/$ARCHIVE > $ARCHIVE
    else
        echo "${ARCHIVE} already downloaded"
    fi
    tar xfj ${ARCHIVE}
    cp -rv gcc-arm-none-eabi-9-2020-q2-update/* ..
    rm -r gcc-arm-none-eabi-9-2020-q2-update
else
    echo "bin/arm-none-eabi-gcc already present, skipping..."
fi

if [ ! -f "$PLATFORM_ROOT/lib/libusb-1.0.a" ];
then
    cd "${PLATFORM_ROOT}/src"
    ARDIR=libusb-1.0.24
    ARCHIVE=${ARDIR}.tar.bz2
    if [ ! -f ${ARCHIVE} ];
    then
        echo "##### downloading ${ARCHIVE} #####"
        curl -L http://sourceforge.net/projects/libusb/files/libusb-1.0/$ARDIR/$ARCHIVE/download > $ARCHIVE
    else
        echo "##### ${ARCHIVE} already downloaded #####"
    fi
    tar xfj ${ARCHIVE}

    cd "${PLATFORM_ROOT}/src/libusb-1.0.24"

    ./configure --prefix="${PLATFORM_ROOT}"
    make
    make install

else
    echo "##### libusb already present, skipping... #####"
fi

if [ ! -f "${PLATFORM_ROOT}/bin/dfu-util" ];
then
    cd "${PLATFORM_ROOT}/src"
    ARDIR=dfu-util-0.11
    ARCHIVE=${ARDIR}.tar.gz
    if [ ! -f $ARCHIVE ];
    then
        echo "##### downloading ${ARCHIVE} #####"
        curl -L http://dfu-util.sourceforge.net/releases/$ARCHIVE > $ARCHIVE
    else
        echo "##### ${ARCHIVE} already downloaded #####"
    fi
    tar xfz ${ARCHIVE}

    cd "${PLATFORM_ROOT}/src/${ARDIR}"
    ./configure --prefix="${PLATFORM_ROOT}" USB_LIBS="${PLATFORM_ROOT}/lib/libusb-1.0.a -ludev -pthread" USB_CFLAGS="-I${PLATFORM_ROOT}/include/libusb-1.0/"
    make
    make install
    make clean
    ldd "${PLATFORM_ROOT}/bin/dfu-util"
else
    echo "##### dfu-util already present, skipping... #####"
fi

cd "${PLATFORM_ROOT}/../jdks"

JDK_ARCHIVE_LINUX="zulu21.42.19-ca-jdk21.0.7-linux_x64.tar.gz"
if [ ! -f "${JDK_ARCHIVE_LINUX}" ];
then
    echo "##### downloading ${JDK_ARCHIVE_LINUX} #####"
    curl -L https://cdn.azul.com/zulu/bin/$JDK_ARCHIVE_LINUX > $JDK_ARCHIVE_LINUX
else
    echo "##### ${JDK_ARCHIVE_LINUX} already downloaded #####"
fi

JDK_ARCHIVE_LINUX_ARCH64="zulu21.42.19-ca-jdk21.0.7-linux_arch64.tar.gz"
if [ ! -f "${JDK_ARCHIVE_LINUX_ARCH64}" ];
then
    echo "##### downloading ${JDK_ARCHIVE_LINUX_ARCH64} #####"
    curl -L https://cdn.azul.com/zulu/bin/$JDK_ARCHIVE_LINUX_ARCH64 > $JDK_ARCHIVE_LINUX_ARCH64
else
    echo "##### ${JDK_ARCHIVE_LINUX_ARCH64} already downloaded #####"
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

case $OS in
    Ubuntu|Debian|MX)
        echo "Installing sdkman using which we get jdk21..."
        curl -s "https://get.sdkman.io" | bash
	    . ~/.sdkman/bin/sdkman-init.sh
        echo "installing java jdk21..."
        sdk install java 21.0.7.fx-zulu
        sdk default java 21.0.7.fx-zulu
        ;;
    Archlinux)
        #!! outdated
        # echo "pacman -Syy jdk7-openjdk"
        # sudo pacman -S --noconfirm jdk7-openjdk
        ;;
    Gentoo)
        #!! outdated
        # echo "emerge --update jdk:1.7 ant"
        # sudo emerge --update jdk:1.7 ant
        ;;
esac


echo "##### compiling firmware... #####"
cd "${PLATFORM_ROOT}"/..
./firmware/compile_firmware.sh BOARD_AXOLOTI_CORE
./firmware/compile_firmware.sh BOARD_KSOLOTI_CORE

echo "##### building Patcher... #####"
./byld_java.sh

echo "DONE"
