rem --- Builds bundle and runtime

@echo off

call build.bat

setlocal
set JAVA_HOME=

:getjdklocation
rem Resolve location of Java JDK environment

set KeyName=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\21.0.7
set Cmd=reg query "%KeyName%" /s
for /f "tokens=2*" %%i in ('%Cmd% ^| find "JavaHome"') do set JAVA_HOME=%%j

if not defined JAVA_HOME (
   echo JDK not installed, please install JDK first
   echo Azul Zulu JDK 21.42.19 (21.0.7) is recommended. Direct download: https://www.azul.com/core-post-download/?endpoint=zulu&uuid=e92d2424-0b2b-4236-9d28-73278f5b0dd9
   pause
   goto :end
)

echo JAVA_HOME: %JAVA_HOME%

set ANT=%~dp0\..\jdks\apache-ant-1.10.14\bin\ant.bat

echo ANT: %ANT%

if not exist %ANT% (
   echo ANT not found, please run build.bat first
   pause
   goto :end
)

cd %~dp0\..

set PATH=%PATH%;%~dp0\bin;C:\Program Files (x86)\WiX Toolset v3.9\bin

%ANT% -Dbuild.bundle=true bundle
%ANT% -Dbuild.runtime=true runtime

:end
endlocal
