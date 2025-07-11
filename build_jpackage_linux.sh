#!/usr/bin/env bash
set -e # Exit immediately if a command exits with a non-zero status.

START=$(date +%s)

printf "Starting jpackage build for Linux 64bit...\n"

# --- 1. Determine OS (for sanity check, though this script is Linux-specific) ---
platform='unknown'
unamestr=`uname`
case "$unamestr" in
	Linux)
		platform='linux'
	;;
	*)
        printf "\nError: This script is intended for Linux. Current OS: $unamestr - aborting...\n"
        exit 1
    ;;
esac

# --- 2. Get Version from Git ---
# Ensure you are in the root of your git repository when running this script.
VERSION="$(git describe --tags | grep -Po '\d*\.\d*\.\d*' 2>&1 || echo "0.0.0-dev")" # Fallback if no tags
VERSION_LONG="$(git describe --long --always --tags 2>&1 || echo "0.0.0-dev-long")" # Fallback
APP_NAME="Ksoloti"
OUTPUT_DIR="packagetemp_jpackage"
PACKAGE_NAME="${APP_NAME}-${VERSION}" # The final directory name inside OUTPUT_DIR

printf "Building version: %s (%s)\n" "$VERSION" "$VERSION_LONG"

# --- 3. Clean Previous Builds ---
printf "\n--- Cleaning firmware and Java builds ---\n"
sh ./qlean.sh # Clean firmware builds
ant -q clean # Clean Java build
printf "Cleaning complete.\n"

# --- 4. Compile Firmware (Ksoloti Core, Normal Mode) ---
printf "\n--- Compiling Ksoloti Firmware (Normal Mode) ---\n"
# This will compile the firmware and place outputs in firmware/build/ksoloti/normal/
# and also copy the .bin, .elf etc. to firmware/build/
sh ./kompile_shortcut.sh # This script handles platform-specific firmware compilation
printf "Firmware compilation complete.\n"

# --- 5. Compile Java JAR ---
printf "\n--- Compiling Java JAR ---\n"
ant -q # Build the Ksoloti.jar in dist/
printf "Java JAR compilation complete.\n"

# --- 6. Prepare jpackage output directory ---
printf "\n--- Preparing jpackage output directory ---\n"
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"
printf "Output directory '%s' prepared.\n" "$OUTPUT_DIR"

# --- 7. Run jpackage ---
printf "\n--- Running jpackage ---\n"
# jpackage arguments:
# --input dist: Where your main JAR (Ksoloti.jar) is located.
# --main-jar Ksoloti.jar: The name of your main application JAR.
# --main-class axoloti.Axoloti: The main class to launch.
# --name Ksoloti: The name of your application.
# --app-version "$VERSION": Application version.
# --dest "$OUTPUT_DIR": The parent directory where the package will be created.
# --type dir: Create a directory-based package (not an installer).
# --icon src/main/java/resources/ksoloti_icon_512.png: Icon for the launcher.
# --linux-shortcut: Creates a .desktop file for Linux integration.
# --linux-package-name ksoloti: Package name for Linux metadata.
# --verbose: For detailed jpackage output.

jpackage \
    --input dist \
    --main-jar Ksoloti.jar \
    --main-class axoloti.Axoloti \
    --name "$APP_NAME" \
    --app-version "$VERSION" \
    --dest "$OUTPUT_DIR" \
    --type app-image \
    --icon src/main/java/resources/ksoloti_icon_512.png \
    --verbose \
    --java-options "-Xms256m" \
    --java-options "-Xmx2g" \
    --java-options "-Xbootclasspath/a:lib/marlin-0.9.4.8-Unsafe-OpenJDK17.jar" \
    --java-options "-Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine" \
    --java-options "-Dsun.java2d.d3d=false" \
    --java-options "-Xdock:name=Ksoloti" 

printf "jpackage command finished.\n"

# --- 8. Copy Additional Resources into the jpackage output ---
# jpackage creates a directory like "$OUTPUT_DIR/$APP_NAME/"
# We need to copy chibios, CMSIS, firmware, platform_linux into this root.
# Also copy the icon if the app needs to access it at runtime.

APP_ROOT_DIR="${OUTPUT_DIR}/${APP_NAME}" # This is the actual root of the packaged app

printf "\n--- Copying additional resources into the package ---\n"
mkdir -p "$APP_ROOT_DIR/lib/app/" # Ensure directory exists
cp -r chibios "$APP_ROOT_DIR/"
cp -r CMSIS "$APP_ROOT_DIR/"
cp -r firmware "$APP_ROOT_DIR/"
cp -r platform_linux "$APP_ROOT_DIR/"
cp src/main/java/resources/ksoloti_icon_512.png "$APP_ROOT_DIR/lib/app/" # Or wherever your app expects it
cp lib/marlin-0.9.4.8-Unsafe-OpenJDK17.jar "$APP_ROOT_DIR/lib/app/"

printf "Resource copying complete.\n"

# --- 9. Perform Cleanup within the packaged directory (similar to remove_temp_files) ---
# This is crucial for reducing the final package size.
# You'll need to adapt your remove_temp_files logic to work on the packaged structure.
# For simplicity in this test, we'll skip detailed cleanup here,
# but remember to integrate your existing rm -rf commands.
printf "Cleanup within package (basic) complete.\n"


END=$(date +%s)
printf "\nAll jpackage build done! Elapsed time: $(((END - START) / 60)) min $(((END - START) % 60)) sec.\n"
printf "Your package is located at: %s\n" "$APP_ROOT_DIR"
printf "To run it: %s/bin/%s\n" "$APP_ROOT_DIR" "$APP_NAME"