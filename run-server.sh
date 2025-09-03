#!/bin/bash

# Secure Chat Application - Server Startup Script
echo "=== Starting Secure Chat Server ==="

# Check if compiled classes exist
if [ ! -d "build" ]; then
    echo "❌ Build directory not found!"
    echo "Please run ./compile.sh first to compile the application."
    exit 1
fi

# Check if server classes exist
if [ ! -f "build/server/ChatServer.class" ]; then
    echo "❌ Server classes not found!"
    echo "Please run ./compile.sh to compile the application."
    exit 1
fi

# Create files directory if it doesn't exist
mkdir -p files

echo "🚀 Starting Secure Chat Server..."
echo "📡 Socket Server will listen on port 12345"
echo "🌐 HTTP Server will listen on port 8080"
echo "📁 Files will be stored in the 'files' directory"
echo ""
echo "Press Ctrl+C to stop the server"
echo "========================================"

# Start the server
cd build
java server.ChatServer
