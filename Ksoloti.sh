#!/usr/bin/env bash

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

export axoloti_home=${axoloti_home:="$rootdir"}
export axoloti_firmware=${axoloti_firmware:="$axoloti_home/firmware"}

which java >/dev/null || printf "\nJava not found in path\n"

heap_jvmargs='-Xms256m -Xmx2g'
marlin_jvmargs='-Xbootclasspath/a:lib/marlin-0.9.4.8-Unsafe-OpenJDK17.jar -Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine -Dsun.java2d.opengl=true -Dsun.java2d.dpiaware=true'

if [ -f $rootdir/dist/Ksoloti.jar ]
then
    case "$platform" in
        mac)
            java -Xdock:name=Ksoloti $heap_jvmargs $marlin_jvmargs -jar $rootdir/dist/Ksoloti.jar $* 2>&1 | tee "$axoloti_home/ksoloti.log"
        ;;
        linux)
            java $heap_jvmargs $marlin_jvmargs -jar $rootdir/dist/Ksoloti.jar $* 2>&1 | tee "$axoloti_home/ksoloti.log"
        ;;
        windows)
            java $heap_jvmargs $marlin_jvmargs -jar $rootdir/dist/Ksoloti.jar $* 2>&1 | tee "$axoloti_home/ksoloti.log"
        ;;
    esac
else
    printf "\nKsoloti.jar does not exist.\n"
fi
