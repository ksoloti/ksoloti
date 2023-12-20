#!/bin/bash
set -e
ant clean
./platform_linux/compile_java.sh
ant clean
# ... open using Swing Explorer
./jdks/SwingExplorer-1.8.0-SNAPSHOT/bin/swexpl -cp ./dist/Ksoloti.jar axoloti.Axoloti
