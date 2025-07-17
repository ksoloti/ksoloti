#!/usr/bin/env bash

set -e

cd $(dirname $0)

# install udev rules
if [ ! -f /etc/udev/rules.d/49-axoloti.rules ];
then
   echo "##### Copying 49-axoloti.rules to /etc/udev/rules.d/ #####"

else
   echo "##### Overwriting 49-axoloti.rules at /etc/udev/rules.d/ #####"
fi

sudo cp 49-axoloti.rules /etc/udev/rules.d/

# reload udev rules
echo "##### Reloading udev rules #####"
sudo udevadm control --reload-rules
echo "#####         DONE         #####"
