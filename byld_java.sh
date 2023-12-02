#!/bin/bash
set -e
ant clean
./platform_linux/compile_java.sh
ant clean
./Ksoloti.sh