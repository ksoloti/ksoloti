@ECHO OFF

call :setfirmware "%axoloti_firmware%"
call :sethome "%axoloti_home%"
call :setrelease "%axoloti_release%"

set PATH=%axoloti_runtime%\platform_win\bin

cd %axoloti_firmware%
make BOARDDEF=%1 -f Makefile.patch.mk clean

echo.&&echo Compiling firmware flasher... %1
cd flasher
if not exist ".dep" mkdir .dep
if not exist "flasher_build\lst" mkdir flasher_build\lst
if not exist "flasher_build\obj" mkdir flasher_build\obj
del /q .dep\*
del /q flasher_build\lst\*
del /q flasher_build\obj\*
rem FWOPTIONDEF currently not used in flasher
make -j16 BOARDDEF=%1
IF %ERRORLEVEL% NEQ 0 (
	exit /b 1
)
cd ..

echo.&&echo Compiling firmware mounter... %1
cd mounter
if not exist ".dep" mkdir .dep
if not exist "mounter_build\lst" mkdir mounter_build\lst
if not exist "mounter_build\obj" mkdir mounter_build\obj
del /q .dep\*
del /q mounter_build\lst\*
del /q mounter_build\obj\*
rem FWOPTIONDEF currently not used in mounter
make -j16 BOARDDEF=%1
IF %ERRORLEVEL% NEQ 0 (
	exit /b 1
)
cd ..

echo.&&echo Compiling firmware... %1
if not exist ".dep" mkdir .dep
if not exist "build\lst" mkdir build\lst
if not exist "build\obj" mkdir build\obj
del /q .dep\*
del /q build\lst\*
del /q build\obj\*
make -j16 BOARDDEF=%1
IF %ERRORLEVEL% NEQ 0 (
	exit /b 1
)

echo.&&echo Compiling firmware... %1 FW_SPILINK
del /q .dep\*
del /q build\lst\*
del /q build\obj\*
make -j16 BOARDDEF=%1 FWOPTIONDEF=FW_SPILINK
IF %ERRORLEVEL% NEQ 0 (
	exit /b 1
)

echo.&&echo Compiling firmware... %1 FW_USBAUDIO
del /q .dep\*
del /q build\lst\*
del /q build\obj\*
make -j16 BOARDDEF=%1 FWOPTIONDEF=FW_USBAUDIO
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
