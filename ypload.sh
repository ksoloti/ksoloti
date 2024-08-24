
#!/bin/bash
set -e

echo ""
echo "Warning! This script is only for testing upload of Ksoloti Core firmware!"
echo "Do not use this script for Axoloti Core or any SPILink-enabled or otherwise customized firmware."
echo ""

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
        echo "Unknown OS: $unamestr - aborting..."
        exit
    ;;
esac

case "$platform" in
    mac)
        sh ./platform_osx/upload_fw_dfu.sh ksoloti
    ;;
    linux)
        sh ./platform_linux/upload_fw_dfu.sh ksoloti
    ;;
    windows)
        cd platform_win
        cmd "//C path.bat && upload_fw_dfu.bat ksoloti"
        cd ..
    ;;
esac