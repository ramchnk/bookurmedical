#!/bin/bash

# Configuration
SOURCE_DIR="." 
DIST_DIR="./dist"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "========================================="
echo "   Packaging StockHub for Client"
echo "========================================="

# 1. Create dist directory
mkdir -p "$DIST_DIR"

# 2. Create a temporary build folder
BUILD_NAME="StockHub_Client_App"
TEMP_BUILD="$DIST_DIR/$BUILD_NAME"

# Clean previous build if any
rm -rf "$TEMP_BUILD"
mkdir -p "$TEMP_BUILD"

# 3. Copy files
# We copy 'webapp' and the start scripts
echo "Copying application files..."
cp -r webapp "$TEMP_BUILD/"
cp start_app.command "$TEMP_BUILD/"
cp start_app.bat "$TEMP_BUILD/"

# 4. REMOVE ADMIN TOOLS
echo "Removing Admin License Generator..."
rm -f "$TEMP_BUILD/webapp/dashboard/admin-license.html"

# 5. Create ZIP file
ZIP_NAME="StockHub_Client_${TIMESTAMP}.zip"
echo "Creating Zip: $ZIP_NAME"

cd "$DIST_DIR"
# Zip the folder recursively
zip -r -q "$ZIP_NAME" "$BUILD_NAME"
cd ..

# 6. Cleanup
rm -rf "$TEMP_BUILD"

echo ""
echo "✅ Build Successful!"
echo "📂 Package Location: $DIST_DIR/$ZIP_NAME"
echo "========================================="
