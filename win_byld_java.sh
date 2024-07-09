#!/bin/bash
ant clean && ant && java -Xms256m -Xmx2g -Xbootclasspath/a:lib/marlin-0.9.4.8-Unsafe-OpenJDK11.jar -Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine -jar ./dist/Ksoloti.jar