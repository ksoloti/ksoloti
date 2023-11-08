#!/bin/bash
set -e
rm -rf ./build/classes/*
./platform_linux/compile_java.sh
./Axoloti.sh