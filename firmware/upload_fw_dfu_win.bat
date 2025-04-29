@echo off

set axoloti_firmware=%~sdp0\
set axoloti_home=%axoloti_firmware%..\
set axoloti_platform=%axoloti_home%\platform_win

set PATH=%axoloti_platform%\\bin;%PATH%

%axoloti_platform%/bin/dfu-util --transfer-size 4096 --device 0483:df11 --intf 0 --alt 0 --download "%axoloti_firmware%/build/%1" --dfuse-address=0x08000000:leave
