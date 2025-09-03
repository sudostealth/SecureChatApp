#!/bin/bash

# Secure Chat Application - Compilation Script
echo "=== Compiling Secure Chat Application ==="

# Create output directory if it doesn't exist
mkdir -p build

# Compile all Java files
echo "Compiling Java source files..."

# Compile utils first (dependencies)
javac -d build src/utils/*.java

# Compile server classes
javac -d build -cp build src/server/*.java

# Compile client classes  
javac -d build -cp build src/client/*.java

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
    echo "📁 Compiled classes are in the 'build' directory"
    echo ""
    echo "Next steps:"
    echo "1. Run server: ./run-server.sh"
    echo "2. Run client: ./run-client.sh"
else
    echo "❌ Compilation failed!"
    echo "Please check the error messages above and fix any issues."
    exit 1
fi
