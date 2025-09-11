@echo off

set axoloti_firmware=%~sdp0
set axoloti_home=%axoloti_firmware%..
set axoloti_platform=%axoloti_home%\platform_win_x64


set "FW_DIR=%axoloti_firmware%"
set "HOME_DIR=%axoloti_home%"
set "PLATFORM_DIR=%axoloti_platform%"
set "FW_DIR=%FW_DIR:\=/%"
set "HOME_DIR=%HOME_DIR:\=/%"
set "PLATFORM_DIR=%PLATFORM_DIR:\=/%"

set PATH=%PLATFORM_DIR%/bin;%PATH%

cd "%FW_DIR%"
make -j8 BOARDDEF=%1 FWOPTIONDEF=%2 BUILDFILENAME=%3 -f Makefile.patch.mk axoloti_firmware="%FW_DIR%" axoloti_home="%HOME_DIR%"
IF %ERRORLEVEL% NEQ 0 (
exit /b 1
)