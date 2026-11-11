#!/usr/bin/env bash
# Run automated clash detection and scheduling engine tests
set -e

# Detect JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 17+)
    fi
fi

if [ ! -d "target/classes" ] || [ ! -d "target/test-classes" ]; then
    echo "Building project first..."
    ./compile.sh
fi

echo "============================================================="
echo "1. Running Standalone Clash Detection Hard-Constraint Suite:"
echo "============================================================="
$JAVA_HOME/bin/java -cp "target/classes:target/test-classes" in.frcce.ecs.service.StandaloneClashTest

echo ""
echo "============================================================="
echo "2. Running FrCRCE SE-ECS Seed Data Scheduling Engine Test:"
echo "============================================================="
$JAVA_HOME/bin/java -cp "target/classes:target/test-classes" in.frcce.ecs.service.SeedDataSchedulingTest

echo ""
echo "============================================================="
echo "3. Running Milestone 2 UI & Layout Verification:"
echo "============================================================="
$JAVA_HOME/bin/java -cp "target/classes:target/test-classes:lib/*" in.frcce.ecs.ui.UiThemeAndLayoutTest

echo ""
echo "============================================================="
echo "4. Running Part 4 Secondary Panels Style Verification:"
echo "============================================================="
$JAVA_HOME/bin/java -cp "target/classes:target/test-classes:lib/*" in.frcce.ecs.ui.SecondaryPanelsTest

