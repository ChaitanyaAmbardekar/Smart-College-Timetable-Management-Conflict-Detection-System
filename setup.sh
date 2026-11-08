#!/usr/bin/env bash
# One-click setup script for ECS Faculty Timetable Generator
set -e

mkdir -p lib target/classes target/test-classes

echo "============================================================="
echo "1. Checking MySQL Connector Driver in lib/..."
echo "============================================================="
if [ ! -f "lib/mysql-connector-j-8.3.0.jar" ]; then
    echo "Downloading mysql-connector-j-8.3.0.jar..."
    curl -L -o lib/mysql-connector-j-8.3.0.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar || {
        echo "Note: If curl was blocked or offline, please place mysql-connector-j-8.3.0.jar manually into lib/"
    }
else
    echo "mysql-connector-j-8.3.0.jar already present."
fi

if [ ! -f "lib/flatlaf-3.2.5.jar" ]; then
    echo "Downloading flatlaf-3.2.5.jar..."
    curl -L -o lib/flatlaf-3.2.5.jar https://repo1.maven.org/maven2/com/formdev/flatlaf/3.2.5/flatlaf-3.2.5.jar || true
fi

echo ""
echo "============================================================="
echo "2. Compiling project sources..."
echo "============================================================="
./compile.sh

echo ""
echo "============================================================="
echo "3. Running verification test suite..."
echo "============================================================="
./test.sh

echo ""
echo "============================================================="
echo "Setup complete! To start the app, run:"
echo "  ./run.sh"
echo "============================================================="

