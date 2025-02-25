#!/bin/bash
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
        printf "\nUnknown OS: $unamestr - aborting...\n"
        exit
    ;;
esac

case "$platform" in
        mac)
            ant -q clean
            ant -q
            ./jdks/SwingExplorer-1.8.0-SNAPSHOT/bin/swexpl -cp ./dist/Ksoloti.jar axoloti.Axoloti
        ;;
        linux)
            ant -q clean
            ant -q
            ./jdks/SwingExplorer-1.8.0-SNAPSHOT/bin/swexpl -cp ./dist/Ksoloti.jar axoloti.Axoloti
        ;;
        windows)
            ant -q clean
            ant -q
            ./jdks/SwingExplorer-1.8.0-SNAPSHOT/bin/swexpl.bat -cp ./dist/Ksoloti.jar axoloti.Axoloti
        ;;
esac

