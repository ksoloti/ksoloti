
#!/bin/bash
set -e

printf "\nWarning! This script is only for testing upload of Ksoloti Core firmware!\n"
printf "Do not use this script for Axoloti Core or any SPILink-enabled or otherwise customized firmware.\n"
printf "\nWarning! This script is only for testing upload of Ksoloti Core firmware!\n"
printf "Do not use this script for Axoloti Core or any SPILink-enabled or otherwise customized firmware.\n"

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

case "$platform" in
    mac)
        sh ./platform_osx/upload_fw_dfu.sh ksoloti.bin
    ;;
    linux)
        sh ./platform_linux/upload_fw_dfu.sh ksoloti.bin
    ;;
    windows)
        sh ./platform_win/upload_fw_dfu.sh ksoloti.bin
        cd ..
    ;;
esac