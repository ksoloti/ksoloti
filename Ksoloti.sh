#!/bin/bash

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

export axoloti_release=${axoloti_release:="$rootdir"}
export axoloti_runtime=${axoloti_runtime:="$rootdir"}
export axoloti_firmware=${axoloti_firmware:="$axoloti_release/firmware"}
export axoloti_home=${axoloti_home:="$rootdir"}

which java >/dev/null || echo "Java not found in path" 

heap_jvmargs='-Xms256m -Xmx2g'
marlin_jvmargs='-Xbootclasspath/a:lib/marlin-0.9.4.8-Unsafe-OpenJDK11.jar -Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine -Dsun.java2d.d3d=false'

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
    echo "Ksoloti.jar does not exist."
fi
