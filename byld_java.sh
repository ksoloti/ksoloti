#!/usr/bin/env bash
set -e

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
        echo "Unknown OS: $unamestr - aborting..."
        exit
    ;;
esac

case "$platform" in
        mac)
            ant -q clean && ant -q
            ./Ksoloti.sh
        ;;
        linux)
            ant -q clean && ant -q
            ./Ksoloti.sh
        ;;
        windows)
            ant -q clean && ant -q
            ./Ksoloti.sh
        ;;
esac