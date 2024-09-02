#!/bin/bash
platformdir="$(dirname $(readlink -f $0))"
cd $platformdir/../

ant clean
ant
