@echo off
set platformdir=%~sdp0

if not defined axoloti_home (
   set axoloti_home=%platformdir%..
)

if not defined axoloti_firmware (
   call :setfirmware "%axoloti_home%\firmware"
)
call :setrelease "%axoloti_home%"

set PATH=%platformdir%bin

goto :eof

:setfirmware
set axoloti_firmware=%~s1
goto :eof
