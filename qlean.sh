#!/bin/bash

printf "Cleaning firmware build...\n"

rm -rf ./firmware/build/*
rm -rf ./firmware/flasher/flasher_build/*
rm -rf ./firmware/mounter/mounter_build/*