@echo off

set axoloti_firmware=%~sdp0\
set axoloti_home=%axoloti_firmware%..\
set axoloti_platform=%axoloti_home%\platform_win

set PATH=%axoloti_platform%\\bin;%PATH%

set argCount = 0
for %%x in (%*) do if not "%%~x" == "" set /a argCount += 1

if %argCount% leq 2 (
	echo. & echo Building all firmware modes for the current board.
	set BUILD_NORMAL=1
	set BUILD_USBAUDIO=1 
	set BUILD_SPILINK=1 
	set BUILD_FLASHER=1 
	set BUILD_MOUNTER=1
	set BUILD_I2SCODEC=1
) else (
	set BUILD_NORMAL=%2
	set BUILD_USBAUDIO=%3 
	set BUILD_SPILINK=%4 
	set BUILD_FLASHER=%5 
	set BUILD_MOUNTER=%6
	set BUILD_I2SCODEC=%7
)

cd %axoloti_firmware%
make BOARDDEF=%1 -f Makefile.patch.mk clean

if %1==BOARD_KSOLOTI_CORE (
  set NAME=ksoloti
) else if %1==BOARD_AXOLOTI_CORE (
  set NAME=axoloti
)

set FLASHER_PROJECT=%NAME%_flasher
set MOUNTER_PROJECT=%NAME%_mounter

if %BUILD_FLASHER%==1 (
    cd flasher
    echo. & echo Compiling %FLASHER_PROJECT%
    set BUILDDIR=flasher_build\%FLASHER_PROJECT%
    if not exist flasher_build\%FLASHER_PROJECT%\.dep mkdir flasher_build\%FLASHER_PROJECT%\.dep
    if not exist flasher_build\%FLASHER_PROJECT%\lst  mkdir flasher_build\%FLASHER_PROJECT%\lst
    if not exist flasher_build\%FLASHER_PROJECT%\obj  mkdir flasher_build\%FLASHER_PROJECT%\obj
	make -j8 BOARDDEF=%1
	if %ERRORLEVEL% neq 0 (
		exit /b 1
	)
    copy /y flasher_build\%FLASHER_PROJECT%\%FLASHER_PROJECT%.* flasher_build 1>NUL
    cd ..
)

if %BUILD_MOUNTER%==1 (
    cd mounter    
    echo. & echo Compiling %MOUNTER_PROJECT%
    set BUILDDIR=mounter_build\%MOUNTER_PROJECT%
    if not exist mounter_build\%MOUNTER_PROJECT%\.dep mkdir mounter_build\%MOUNTER_PROJECT%\.dep
    if not exist mounter_build\%MOUNTER_PROJECT%\lst  mkdir mounter_build\%MOUNTER_PROJECT%\lst
    if not exist mounter_build\%MOUNTER_PROJECT%\obj  mkdir mounter_build\%MOUNTER_PROJECT%\obj
	make -j8 BOARDDEF=%1
	if %ERRORLEVEL% neq 0 (
		exit /b 1
	)
    copy /y mounter_build\%MOUNTER_PROJECT%\%MOUNTER_PROJECT%.* mounter_build 1>NUL
    cd ..
)

if %BUILD_NORMAL%==1 (
    echo. & echo Compiling %1
    set BUILDDIR=build\%NAME%\normal
    if not exist build\%NAME%\normal\.dep mkdir build\%NAME%\normal\.dep
    if not exist build\%NAME%\normal\lst  mkdir build\%NAME%\normal\lst
    if not exist build\%NAME%\normal\obj  mkdir build\%NAME%\normal\obj
	make -j8 BOARDDEF=%1
	if %ERRORLEVEL% neq 0 (
		exit /b 1
	)
    copy /y build\%NAME%\normal\%NAME%.* build 1>NUL
)

if %BUILD_SPILINK%==1 (
    echo. & echo Compiling %1 FW_SPILINK
    set BUILDDIR=build\%NAME%\spilink
    if not exist build\%NAME%\spilink\.dep mkdir build\%NAME%\spilink\.dep
    if not exist build\%NAME%\spilink\lst  mkdir build\%NAME%\spilink\lst
    if not exist build\%NAME%\spilink\obj  mkdir build\%NAME%\spilink\obj
	make -j8 BOARDDEF=%1 FWOPTIONDEF=FW_SPILINK
	if %ERRORLEVEL% neq 0 (
		exit /b 1
	)
    copy /y build\%NAME%\spilink\%NAME%_spilink.* build 1>NUL
)

if %BUILD_USBAUDIO%==1 (
    echo. & echo Compiling %1 FW_USBAUDIO
    set BUILDDIR=build\%NAME%\usbaudio
    if not exist build\%NAME%\usbaudio\.dep mkdir build\%NAME%\usbaudio\.dep
    if not exist build\%NAME%\usbaudio\lst  mkdir build\%NAME%\usbaudio\lst
    if not exist build\%NAME%\usbaudio\obj  mkdir build\%NAME%\usbaudio\obj
	make -j8 BOARDDEF=%1 FWOPTIONDEF=FW_USBAUDIO
	if %ERRORLEVEL% neq 0 (
		exit /b 1
	)
    copy /y build\%NAME%\usbaudio\%NAME%_usbaudio.* build 1>NUL
)

if %BUILD_I2SCODEC%==1 (
    echo. & echo Compiling %1 FW_I2SCODEC
    set BUILDDIR=build\%NAME%\i2scodec
    if not exist build\%NAME%\i2scodec\.dep mkdir build\%NAME%\i2scodec\.dep
    if not exist build\%NAME%\i2scodec\lst  mkdir build\%NAME%\i2scodec\lst
    if not exist build\%NAME%\i2scodec\obj  mkdir build\%NAME%\i2scodec\obj
	make -j8 BOARDDEF=%1 FWOPTIONDEF=FW_I2SCODEC
	if %ERRORLEVEL% neq 0 (
		exit /b 1
	)
    copy /y build\%NAME%\i2scodec\%NAME%_i2scodec.* build 1>NUL
)

echo.