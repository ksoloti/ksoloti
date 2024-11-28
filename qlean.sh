#!/bin/bash
rm -f ./firmware/.dep/*.*
rm -f ./firmware/build/lst/*.*
rm -f ./firmware/build/obj/*.*

rm -f ./firmware/*/.dep/*.*
rm -f ./firmware/*/build/lst/*.*
rm -f ./firmware/*/build/obj/*.*

rm -f ./firmware/*/*/.dep/*.*
rm -f ./firmware/*/*/build/lst/*.*
rm -f ./firmware/*/*/build/obj/*.*