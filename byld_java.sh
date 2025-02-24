#!/bin/bash
set -e

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
            ant -q clean
            ant -q
            ./Ksoloti.sh
        ;;
        linux)
            ant -q clean
            #./platform_linux/compile_java.sh #outdated java8 define?
            ant -q
            ./Ksoloti.sh
        ;;
        windows)
            ant -q clean
            ant -q
            ./Ksoloti.sh
        ;;
esac