@ECHO OFF

call :setfirmware "%axoloti_firmware%"
call :sethome "%axoloti_home%"
call :setrelease "%axoloti_release%"

set PATH=%axoloti_runtime%\platform_win\bin

cd %axoloti_firmware%
make BOARDDEF=%1 FWOPTIONDEF=%2 -f Makefile.patch.mk clean

echo.&&echo Compiling firmware flasher... %1 %2
cd flasher
if exist ".dep\" rmdir /s /q .dep
if exist "flasher_build\lst" rmdir /s /q flasher_build\lst
if exist "flasher_build\obj" rmdir /s /q flasher_build\obj
mkdir .dep
mkdir flasher_build\lst
mkdir flasher_build\obj
rem FWOPTIONDEF currently not used in flasher
make -j4 BOARDDEF=%1 FWOPTIONDEF=%2
IF %ERRORLEVEL% NEQ 0 (
	exit /b 1
)
cd ..

echo.&&echo Compiling firmware mounter... %1 %2
cd mounter
if exist ".dep\" rmdir /s /q .dep
if exist "mounter_build\lst" rmdir /s /q mounter_build\lst
if exist "mounter_build\obj" rmdir /s /q mounter_build\obj
mkdir .dep
mkdir mounter_build\lst
mkdir mounter_build\obj
rem FWOPTIONDEF currently not used in mounter
make -j4 BOARDDEF=%1 FWOPTIONDEF=%2
IF %ERRORLEVEL% NEQ 0 (
	exit /b 1
)
cd ..

echo.&&echo Compiling firmware... %1 %2
if exist ".dep\" rmdir /s /q .dep
if exist "build\lst" rmdir /s /q build\lst
if exist "build\obj" rmdir /s /q build\obj
mkdir .dep
mkdir build\lst
mkdir build\obj
make -j4 BOARDDEF=%1 FWOPTIONDEF=%2
IF %ERRORLEVEL% NEQ 0 (
	exit /b 1
)

goto :eof

rem --- path shortening

:setfirmware
set axoloti_firmware=%~s1
goto :eof

:sethome
set axoloti_home=%~s1
goto :eof

:setrelease
set axoloti_release=%~s1
goto :eof
