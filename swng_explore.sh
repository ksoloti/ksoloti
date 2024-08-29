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
            ant clean
            ant
            ant clean
            ./jdks/SwingExplorer-1.8.0-SNAPSHOT/bin/swexpl -cp ./dist/Ksoloti.jar axoloti.Axoloti
        ;;
        linux)
            ant clean
            ant
            ant clean
            ./jdks/SwingExplorer-1.8.0-SNAPSHOT/bin/swexpl -cp ./dist/Ksoloti.jar axoloti.Axoloti
        ;;
        windows)
            ./platform_win/apache-ant-1.10.14/bin/ant.bat clean
            ./platform_win/apache-ant-1.10.14/bin/ant.bat 
            ./platform_win/apache-ant-1.10.14/bin/ant.bat clean
            ./jdks/SwingExplorer-1.8.0-SNAPSHOT/bin/swexpl.bat -cp ./dist/Ksoloti.jar axoloti.Axoloti
        ;;
esac

