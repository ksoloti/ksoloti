cd %~dp0
del coreutils-*-bin.zip
del make-*-bin.zip
del make-*-dep.zip
del ChibiOS_*.zip
del gcc-arm-none-eabi-*-win32.zip
del stlink-*-win.zip
del src\libusb-1.0.*.tar.bz2

rmdir /S arm-none-eabi
rmdir /S bin
rmdir /S chibios
rmdir /S contrib
rmdir /S lib
rmdir /S man
rmdir /S manifest
rmdir /S share
rmdir /S include
rmdir /S stlink-*-win
rmdir /S src\libusb-1.0.*

