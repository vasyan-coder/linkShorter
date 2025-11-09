#!/bin/bash

# Script to build and run the Link Shortener application

set -e

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                   Link Shortener Build Script                       ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.8+ first."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17+ first."
    exit 1
fi

# Build the project
echo "🔨 Building project..."
mvn clean package

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "🚀 Starting application..."
    echo ""
    
    # Find the JAR file
    JAR_FILE=$(find target -name "*-jar-with-dependencies.jar" | head -n 1)
    
    if [ -z "$JAR_FILE" ]; then
        echo "❌ JAR file not found!"
        exit 1
    fi
    
    # Run the application with optional UUID argument
    if [ -n "$1" ]; then
        java -jar "$JAR_FILE" "$1"
    else
        java -jar "$JAR_FILE"
    fi
else
    echo "❌ Build failed!"
    exit 1
fi

