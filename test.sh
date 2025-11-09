#!/bin/bash

# Script to run tests with coverage report

set -e

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                   Link Shortener Test Script                        ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.8+ first."
    exit 1
fi

echo "🧪 Running tests..."
mvn clean test

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All tests passed!"
    echo ""
    echo "📊 Generating coverage report..."
    mvn jacoco:report
    
    if [ $? -eq 0 ]; then
        echo "✅ Coverage report generated!"
        echo ""
        echo "📈 Coverage report location: target/site/jacoco/index.html"
        echo ""
        
        # Try to open the report in browser (macOS)
        if [[ "$OSTYPE" == "darwin"* ]]; then
            echo "🌐 Opening coverage report in browser..."
            open target/site/jacoco/index.html
        fi
    else
        echo "❌ Failed to generate coverage report!"
        exit 1
    fi
else
    echo "❌ Tests failed!"
    exit 1
fi

