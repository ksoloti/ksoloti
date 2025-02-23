#!/bin/bash

printf "Cleaning firmware build...\n"

rm -f ./firmware/.dep/*.*
rm -f ./firmware/build/lst/*.*
rm -f ./firmware/build/obj/*.*

rm -f ./firmware/*/.dep/*.*
rm -f ./firmware/*/lst/*.*
rm -f ./firmware/*/obj/*.*

rm -f ./firmware/*/*/.dep/*.*
rm -f ./firmware/*/*/lst/*.*
rm -f ./firmware/*/*/obj/*.*

rm -f ./firmware/*/*/*/.dep/*.*
rm -f ./firmware/*/*/*/lst/*.*
rm -f ./firmware/*/*/*/obj/*.*