#!/bin/bash

# Define the template file and the final desktop file name
TEMPLATE_FILE="Ksoloti_template.desktop"
DEST_FILE="Ksoloti.desktop"

# Get the absolute path of the Patcher's home directory
APP_DIR="$( dirname "$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )" )"

# Define the full paths for Exec and Icon using the home directory
EXECUTABLE_PATH="$APP_DIR/Ksoloti %F"
ICON_PATH="$APP_DIR/platform_linux_aarch64/ksoloti_icon.svg"

# Define the destination directory for desktop files
DEST_DIR="$HOME/.local/share/applications"

# Create the destination directory if it doesn't exist
mkdir -p "$DEST_DIR"

# Create the template file in the script's directory (if it doesn't exist)
cat > "$TEMPLATE_FILE" << EOF
[Desktop Entry]
Name=Ksoloti-1.1.0
Comment=Open Axoloti Patch Files
Exec=<<CHANGE_ME>>/Ksoloti %F
Icon=<<CHANGE_ME>>/platform_linux_aarch64/ksoloti_icon.svg
Terminal=false
Type=Application
MimeType=application/x-axoloti-patch;
StartupWMClass=axoloti-Axoloti
EOF

# Use sed to replace the placeholders with the actual paths and copy to the destination
sed -e "s|<<CHANGE_ME>>/Ksoloti %F|$EXECUTABLE_PATH|" \
    -e "s|<<CHANGE_ME>>/platform_linux_aarch64/ksoloti_icon.svg|$ICON_PATH|" \
    "$TEMPLATE_FILE" > "$DEST_DIR/$DEST_FILE"

# Make the desktop file executable
chmod +x "$DEST_DIR/$DEST_FILE"
rm "$TEMPLATE_FILE"

echo "Desktop file created successfully at $DEST_DIR/$DEST_FILE"