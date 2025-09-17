#!/usr/bin/env bash

printf "Renewing script permissions...\n"

chmod 755 ./*.sh
chmod 755 ./*/*.sh
chmod 755 ./*/*/*.sh

chmod 755 ./dist/*
chmod 755 ./platform_*/bin/*
chmod 755 ./platform_*/*/bin/*

chmod 755 ./jdks/SwingExplorer-1.8.0-SNAPSHOT/bin/swexpl