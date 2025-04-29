
#!/usr/bin/env bash
set -e

printf "\nWarning! This script is only for testing upload of Ksoloti Core normal firmware!\n"
printf "Do not use this script for Axoloti Core or any otherwise customized firmware mode.\n"

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