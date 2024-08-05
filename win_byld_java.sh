#!/bin/bash
./platform_win/apache-ant-1.10.14/bin/ant.bat clean && \
./platform_win/apache-ant-1.10.14/bin/ant.bat
#java -Xms256m -Xmx2g -Xbootclasspath/a:lib/marlin-0.9.4.8-Unsafe-OpenJDK11.jar -Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine -Dsun.java2d.dpiaware=true -Dsun.java2d.d3d=false -jar ./dist/Ksoloti.jar 