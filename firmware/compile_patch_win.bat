@echo off

set axoloti_firmware=%~sdp0\
set axoloti_home=%axoloti_firmware%..\
set axoloti_platform=%axoloti_home%\platform_win_x64

set PATH=%axoloti_platform%\\bin;%PATH%

cd %axoloti_firmware%
make -j8 BOARDDEF=%1 FWOPTIONDEF=%2 BUILDFILENAME=%3 -f Makefile.patch.mk
IF %ERRORLEVEL% NEQ 0 (
	exit /b 1
)