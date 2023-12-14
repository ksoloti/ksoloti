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

if [ -f $rootdir/dist/Ksoloti.jar ]
then
    case "$platform" in
        mac)
                java -Xdock:name=Ksoloti \
                        # --patch-module sun.java2d.marlin=$rootdir/lib/marlin-0.9.4.7-Unsafe.jar \
                        # -Dsun.java2d.renderer=sun.java2d.marlin.DMarlinRenderingEngine \
                        -jar $rootdir/dist/Ksoloti.jar $* 2>&1 | tee "$axoloti_home/ksoloti.log"
        ;;
        linux)
                java \
                        # --patch-module sun.java2d.marlin=$rootdir/lib/marlin-0.9.4.7-Unsafe.jar \
                        # -Dsun.java2d.renderer=sun.java2d.marlin.DMarlinRenderingEngine \
                        -jar $rootdir/dist/Ksoloti.jar $* 2>&1 | tee "$axoloti_home/ksoloti.log"
        ;;
    esac
else
    echo "Ksoloti.jar does not exist."
fi
