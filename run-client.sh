#!/bin/bash

# Secure Chat Application - Client Startup Script
echo "=== Starting Secure Chat Client ==="

# Check if compiled classes exist
if [ ! -d "build" ]; then
    echo "❌ Build directory not found!"
    echo "Please run ./compile.sh first to compile the application."
    exit 1
fi

# Check if client classes exist
if [ ! -f "build/client/ChatClient.class" ]; then
    echo "❌ Client classes not found!"
    echo "Please run ./compile.sh to compile the application."
    exit 1
fi

echo "🚀 Starting Secure Chat Client..."
echo "💬 GUI interface will open shortly"
echo "🔗 Make sure the server is running first"
echo ""
echo "Client Features:"
echo "• Secure messaging with AES encryption"
echo "• File transfer capabilities"
echo "• Chat session management"
echo "• Timer-based chat destruction"
echo "========================================"

# Start the client
cd build
java client.ChatClient
