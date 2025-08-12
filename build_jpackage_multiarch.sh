#!/usr/bin/env bash
set -e # Exit immediately if a command exits with a non-zero status.

# --- Configuration Variables (can be overridden by environment or args) ---
DEFAULT_TARGET_OS="linux"        # Default target if not specified (e.g., if no arguments provided)
DEFAULT_TARGET_ARCH="x64"        # Default target architecture if not specified
JDK_CACHE_DIR="./jdks"           # Directory to store downloaded JDKs
JDK_BASE_DIR="./jdks"            # Directory where custom Zulu JDKs are downloaded/unzipped
ZULU_JDK_BUILD_SUFFIX="21.42.19" # e.g., "21.42.19" from "zulu21.42.19-ca"
ZULU_JDK_VERSION_SUFFIX="21.0.7" # e.g., "21.0.7" from "jdk21.0.7"

# --- Helper function to download and extract JDK ---
download_jdk() {
    local os="$1"
    local arch="$2"
    local java_version="$3" # e.g., "21" for Java 21
    local jdk_url=""
    local jdk_file_name=""  # The name of the downloaded archive file
    local unzipped_dir_prefix="zulu${ZULU_JDK_BUILD_SUFFIX}-ca-jdk${ZULU_JDK_VERSION_SUFFIX}" # e.g., zulu21.42.19-ca-jdk21.0.7
    local jdk_path="${JDK_BASE_DIR}/${unzipped_dir_prefix}-${os}_${arch}"

    if [ -d "$jdk_path" ] && ([ -f "$jdk_path/bin/java" ] || [ -f "$jdk_path/bin/java.exe" ]); then
        printf "JDK %s for %s/%s already exists at %s. Skipping download.\n" "$java_version" "$os" "$arch" "$jdk_path" >&2
        echo "$jdk_path" # Return the path to stdout
        return 0
    fi

    printf "Downloading Zulu JDK %s for %s/%s...\n" "$java_version" "$os" "$arch" >&2

    # Define URLs and filenames based on OS and Arch from Azul.com/downloads/
    case "${os}_${arch}" in
        "linux_x64")     jdk_file_name="zulu${ZULU_JDK_BUILD_SUFFIX}-ca-jdk${ZULU_JDK_VERSION_SUFFIX}-linux_x64.tar.gz" ;;
        "linux_aarch64") jdk_file_name="zulu${ZULU_JDK_BUILD_SUFFIX}-ca-jdk${ZULU_JDK_VERSION_SUFFIX}-linux_aarch64.tar.gz" ;;
        "win_x64")   jdk_file_name="zulu${ZULU_JDK_BUILD_SUFFIX}-ca-jdk${ZULU_JDK_VERSION_SUFFIX}-win_x64.zip" ;;
        "mac_x64")       jdk_file_name="zulu${ZULU_JDK_BUILD_SUFFIX}-ca-jdk${ZULU_JDK_VERSION_SUFFIX}-macosx_x64.tar.gz" ;;
        "mac_aarch64")   jdk_file_name="zulu${ZULU_JDK_BUILD_SUFFIX}-ca-jdk${ZULU_JDK_VERSION_SUFFIX}-macosx_aarch64.tar.gz" ;;
        *)               printf "Error: Unsupported OS/architecture combination for Zulu download: %s/%s\n" "$os" "$arch" >&2; exit 1 ;;
    esac

    jdk_url="https://cdn.azul.com/zulu/bin/${jdk_file_name}"

    mkdir -p "${JDK_BASE_DIR}" # Use JDK_BASE_DIR for custom JDKs
    local downloaded_file="${JDK_BASE_DIR}/${jdk_file_name}"

    if [ ! -f "$downloaded_file" ]; then
        curl -L "$jdk_url" -o "$downloaded_file"
        printf "Downloaded %s\n" "$jdk_file_name" >&2
    else
        printf "JDK archive %s already downloaded. Skipping curl.\n" "$jdk_file_name" >&2
    fi

    printf "Extracting JDK to %s...\n" "$jdk_path" >&2
    local temp_extract_dir="${JDK_BASE_DIR}/temp_extract_$(date +%s%N)"
    mkdir -p "$temp_extract_dir"

    if [[ "$jdk_file_name" == *.zip ]]; then
        unzip -q "$downloaded_file" -d "$temp_extract_dir"
    else
        tar -xzf "$downloaded_file" -C "$temp_extract_dir"
    fi

    local extracted_jdk_root=""
    for d in "${temp_extract_dir}"/*/; do
        if [ -f "${d}/bin/java" ] || [ -f "${d}/bin/java.exe" ]; then
            extracted_jdk_root="${d}"
            break
        fi
    done

    if [ -n "$extracted_jdk_root" ]; then
        # If the target directory already exists (e.g., from a failed previous run), remove it.
        rm -rf "$jdk_path"
        mv "$extracted_jdk_root" "$jdk_path"
        printf "JDK extracted and moved to %s.\n" "$jdk_path" >&2
    else
        printf "Error: Could not find JDK root within extracted archive at %s. Please check structure of %s.\n" "${temp_extract_dir}" "${jdk_file_name}" >&2
        rm -rf "$temp_extract_dir" # Clean up temp directory
        exit 1
    fi
    rm -rf "$temp_extract_dir" # Clean up temp directory

    echo "$jdk_path" # Return the path to stdout
}

remove_temp_files() {
    rm -rf "${APP_ROOT_DIR}"/bin/chibios/demos
    rm -rf "${APP_ROOT_DIR}"/bin/chibios/test
    rm -rf "${APP_ROOT_DIR}"/bin/chibios/testal
            
    rm -rf "${APP_ROOT_DIR}"/bin/firmware/.settings
    rm -rf "${APP_ROOT_DIR}"/bin/firmware/build/*oloti
    rm -rf "${APP_ROOT_DIR}"/bin/firmware/flasher/flasher_build/*oloti_flasher
    rm -rf "${APP_ROOT_DIR}"/bin/firmware/mounter/mounter_build/*oloti_mounter
            
    rm     "${APP_ROOT_DIR}"/bin/firmware/build/*.dmp
    rm     "${APP_ROOT_DIR}"/bin/firmware/build/*.hex
    rm     "${APP_ROOT_DIR}"/bin/firmware/build/*.list
    rm     "${APP_ROOT_DIR}"/bin/firmware/build/*.map
            
    rm     "${APP_ROOT_DIR}"/bin/firmware/*/*_build/*.dmp
    rm     "${APP_ROOT_DIR}"/bin/firmware/*/*_build/*.hex
    rm     "${APP_ROOT_DIR}"/bin/firmware/*/*_build/*.list
    rm     "${APP_ROOT_DIR}"/bin/firmware/*/*_build/*.map
            
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/share
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/src
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/arm-none-eabi/lib/armv6-m
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/arm-none-eabi/lib/armv7-ar
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/arm-none-eabi/lib/armv7-m
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/arm-none-eabi/lib/cortex-m7
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/lib/gcc/arm-none-eabi/*/arm/v6-m
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/lib/gcc/arm-none-eabi/*/arm/v7-ar
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/lib/gcc/arm-none-eabi/*/arm/v7-m
    rm -rf "${APP_ROOT_DIR}"/bin/platform_*/lib/gcc/arm-none-eabi/*/cortex-m7

    rm -rf "${APP_ROOT_DIR}"/lib/runtime/jmods
    rm -rf "${APP_ROOT_DIR}"/lib/runtime/demo
    rm -rf "${APP_ROOT_DoR}"/lib/runtime/man
    # rm     "${APP_ROOT_DIR}"/lib/runtime/lib/src.zip

}

START=$(date +%s)

printf "Starting jpackage build...\n"

# --- 1. Determine Host OS and Architecture ---
HOST_OS='unknown'
HOST_ARCH='unknown'
unamestr=`uname`
case "$unamestr" in
    Linux)
        HOST_OS='linux'
        HOST_ARCH=$(uname -m)
        if [ "$HOST_ARCH" == "x86_64" ]; then HOST_ARCH="x64"; fi
        if [ "$HOST_ARCH" == "aarch64" ]; then HOST_ARCH="aarch64"; fi
    ;;
    Darwin)
        HOST_OS='mac'
        HOST_ARCH=$(uname -m)
        if [ "$HOST_ARCH" == "x86_64" ]; then HOST_ARCH="x64"; fi # Intel Mac
        if [ "$HOST_ARCH" == "arm64" ]; then HOST_ARCH="aarch64"; fi # Apple Silicon Mac
    ;;
    CYGWIN*|MINGW32*|MSYS*|MINGW*) # Basic detection for running on Windows with Git Bash/Cygwin
        HOST_OS='win'
        HOST_ARCH='x64' # Assuming 64-bit Windows for packaging
    ;;
    *)
        printf "\nError: Unsupported host OS: $unamestr - aborting...\n"
        exit 1
    ;;
esac
printf "Running on Host: $HOST_OS ($HOST_ARCH)\n"

# --- 2. Parse Target OS and Architecture from arguments ---
# Usage: ./jpackage_build.sh <TARGET_OS> <TARGET_ARCH>
# Example: ./jpackage_build.sh win x64
# Example: ./jpackage_build.sh mac aarch64
TARGET_OS="${1:-$DEFAULT_TARGET_OS}"
TARGET_ARCH="${2:-$DEFAULT_TARGET_ARCH}"

# Validate target OS/Arch
case "${TARGET_OS}" in
    "linux"|"win"|"mac") ;;
    *) printf "Error: Invalid TARGET_OS '$TARGET_OS'. Use 'linux', 'win', or 'mac'.\n"; exit 1 ;;
esac

case "${TARGET_ARCH}" in
    "x64"|"aarch64") ;;
    *) printf "Error: Invalid TARGET_ARCH '$TARGET_ARCH'. Use 'x64' or 'aarch64'.\n"; exit 1 ;;
esac

# Special warning for cross-building Mac aarch64 on Intel Mac (requires specific tools/Rosetta 2 setup)
if [ "$TARGET_OS" == "mac" ] && [ "$TARGET_ARCH" == "aarch64" ] && [ "$HOST_ARCH" == "x64" ]; then
    printf "Warning: Building macOS Apple Silicon package on Intel Mac might require specific setup (e.g., Rosetta 2, Xcode cmd-line tools for arm64).\n"
fi

printf "Building for Target: $TARGET_OS ($TARGET_ARCH)\n"

# --- 3. Get Version from Git ---
VERSION="$(git describe --tags | grep -Po '\d*\.\d*\.\d*' 2>&1 || echo "0.0.0-dev")" # Fallback if no tags
VERSION_LONG="$(git describe --long --always --tags 2>&1 || echo "0.0.0-dev-long")" # Fallback
APP_NAME="Ksoloti"
OUTPUT_DIR="packagetemp_jpackage"
# Create a unique output directory for each target package
PACKAGE_NAME="${APP_NAME}-${VERSION}-${TARGET_OS}_${TARGET_ARCH}"

printf "Building version: $VERSION ($VERSION_LONG)\n"

# --- 4. Clean Previous Builds ---
printf "\n--- Cleaning firmware and Java builds ---\n"
sh ./qlean.sh # Clean firmware builds
ant -q clean # Clean Java build
printf "Cleaning complete.\n"

# --- 5. Compile Firmware ---
printf "\n--- Compiling Firmware ---\n"
sh ./kompile_shortcut.sh
printf "Firmware compilation complete.\n"

# --- 6. Compile Java JAR ---
printf "\n--- Compiling Java JAR ---\n"
ant -q # Build the Ksoloti.jar in dist/
printf "Java JAR compilation complete.\n"

# --- 7. Prepare jpackage output directory ---
printf "\n--- Preparing jpackage output directory ---\n"
rm -rf "${OUTPUT_DIR}/${PACKAGE_NAME}" # Remove specific target's output directory
mkdir -p "${OUTPUT_DIR}/${PACKAGE_NAME}"
printf "Output directory '$OUTPUT_DIR/$PACKAGE_NAME' prepared.\n"

# --- 8. Download/Prepare Target JDK ---
# Java version to use for the jpackage runtime.
JAVA_VERSION="21"
TARGET_JDK_PATH=$(download_jdk "${TARGET_OS}" "${TARGET_ARCH}" "$JAVA_VERSION")
if [ ! -d "${TARGET_JDK_PATH}" ]; then
    printf "Error: Target JDK not found at $TARGET_JDK_PATH. Aborting.\n"
    exit 1
fi
printf "Using target JDK from: $TARGET_JDK_PATH.\n"

# --- 9. Run jpackage ---
printf "\n--- Running jpackage for $TARGET_OS ($TARGET_ARCH) ---\n"

# Common jpackage arguments for all platforms
JPACKAGE_ARGS=(
    --input dist
    --main-jar Ksoloti.jar
    --main-class axoloti.Axoloti
    --name "$APP_NAME"
    --app-version "$VERSION"
    --dest "$OUTPUT_DIR/${PACKAGE_NAME}" # Output into the unique target directory
    --verbose
    --java-options "-Xms256m"
    --java-options "-Xmx2g"
    --java-options "-Xbootclasspath/a:lib/marlin-0.9.4.8-Unsafe-OpenJDK17.jar"
    --java-options "-Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine"
    --java-options "-Dsun.java2d.d3d=false"
    # --jlink-options "--strip-native-commands --strip-debug --no-man-pages --no-header-files --compress=2"
    --runtime-image "$TARGET_JDK_PATH" # Specifies the JDK for the target platform
)

# Platform-specific jpackage arguments
case "${TARGET_OS}" in
    "linux")
        JPACKAGE_ARGS+=(
            --type app-image # 'deb' or 'rpm' for installable packages
            # --linux-shortcut
            # --linux-package-name "$(echo "$APP_NAME" | tr '[:upper:]' '[:lower:]')" # Standard Linux package name is lowercase
            # --linux-deb-maintainer "Your Name <your.email@example.com>" # Recommended for .deb
            # --linux-menu-group "Audio;Midi;Music;Development;Education" # Category for desktop menu
            --icon src/main/java/resources/appicons/ksoloti_icon_512.png # PNG is typical for Linux .desktop files
        )
        ;;
    "win")
        JPACKAGE_ARGS+=(
            --type app-image # 'exe' or 'msi' for installers
            --icon src/main/java/resources/appicons/ksoloti_icon_512.png
            # --icon src/main/java/resources/appicons/ksoloti_icon.ico # (only .png allowed!)
            # --win-dir-chooser # Allow user to choose installation directory
            # --win-menu # Create Start Menu entry
            # --win-shortcut # Create Desktop shortcut
            # --win-upgrade-uuid <YOUR_UNIQUE_UUID> # For installers, to enable upgrades (generate with 'uuidgen')
            # --resource-dir src/windows/resources # For custom installer assets like background images
        )
        ;;
    "mac")
        JPACKAGE_ARGS+=(
            --type app-image # 'dmg' for installable disk images
            --icon src/main/java/resources/appicons/ksoloti_icon.icns # Use the .icns file for macOS
            # --mac-sign # Requires macOS codesigning setup (certificate, keychain)
            # --mac-entitlements /path/to/entitlements.plist # For sandboxing etc.
            # --mac-app-store-runtime # For App Store submissions
            # --mac-package-identifier "com.yourcompany.Ksoloti" # Required for signing/App Store
        )
        ;;
esac

# Execute jpackage with all collected arguments
jpackage "${JPACKAGE_ARGS[@]}"

printf "jpackage command finished.\n"

# --- 10. Copy Additional Resources into the jpackage output ---
APP_ROOT_DIR="${OUTPUT_DIR}/${PACKAGE_NAME}/${APP_NAME}" # This is the actual root of the packaged app

printf "\n--- Copying additional resources into the package ---\n"
mkdir -p "$APP_ROOT_DIR/lib/app/"

# Copy core resources that are independent of the target OS/arch
cp -r chibios "$APP_ROOT_DIR/bin/"
cp -r CMSIS "$APP_ROOT_DIR/bin/"
cp -r firmware "$APP_ROOT_DIR/bin/" # The Axoloti device firmware binary

# Copy platform-specific binaries/tools
case "${TARGET_OS}_${TARGET_ARCH}" in
    "linux_x64")
        cp -r platform_linux_x64 "$APP_ROOT_DIR/bin/"
        ;;
    "linux_aarch64")
        # Assume you have 'platform_linux_aarch64' containing tools compiled for ARM64 Linux
        cp -r platform_linux_aarch64 "$APP_ROOT_DIR/bin/"
        ;;
    "win_x64")
        # Assume you have 'platform_win_x64' containing tools compiled for Windows x64
        cp -r platform_win_x64 "$APP_ROOT_DIR/bin/"
        ;;
    "mac_x64")
        # Assume you have 'platform_mac_x64' containing tools compiled for Intel macOS
        cp -r platform_mac_x64 "$APP_ROOT_DIR/bin/"
        ;;
    "mac_aarch64")
        # Assume you have 'platform_mac_aarch64' containing tools compiled for Apple Silicon macOS
        cp -r platform_mac_aarch64 "$APP_ROOT_DIR/bin/"
        ;;
esac

# Copy other application-specific resources
cp src/main/java/resources/appicons/ksoloti_icon_512.png "$APP_ROOT_DIR/lib/app/" # Or wherever your app expects it at runtime
cp lib/marlin-0.9.4.8-Unsafe-OpenJDK17.jar "$APP_ROOT_DIR/lib/app/"

printf "Resource copying complete.\n"

# --- 11. Perform Cleanup within the packaged directory (similar to remove_temp_files) ---
printf "\n--- Performing detailed cleanup within the packaged app directory ---\n"
remove_temp_files
printf "Detailed cleanup within package complete.\n"


END=$(date +%s)
printf "\nAll jpackage builds done! Elapsed time: $(((END - START) / 60)) min $(((END - START) % 60)) sec.\n"
printf "Your package is located at: $APP_ROOT_DIR\n"
printf "To run it: "
if [ "$TARGET_OS" == "win" ]; then
    printf "$APP_ROOT_DIR\\bin\\$APP_NAME.exe\n"
elif [ "$TARGET_OS" == "mac" ]; then
    printf "$OUTPUT_DIR/$PACKAGE_NAME/$APP_NAME.app/Contents/MacOS/$APP_NAME\n"
else # Linux (app-image)
    printf "$APP_ROOT_DIR/bin/$APP_NAME\n"
fi

printf "\nTo build for other platforms/architectures, run this script with arguments:\n"
printf "  ./jpackage_build.sh <TARGET_OS> <TARGET_ARCH>\n"
printf "Examples:\n"
printf "  ./jpackage_build.sh linux x64\n"
printf "  ./jpackage_build.sh linux aarch64   # Linux ARM 64bit (e.g., Raspberry Pi)\n"
printf "  ./jpackage_build.sh win x64\n"
printf "  ./jpackage_build.sh mac x64         # macOS Intel\n"
printf "  ./jpackage_build.sh mac aarch64     # macOS Apple Silicon\n"