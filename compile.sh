#!/usr/bin/env bash
# Compile script for ECS Faculty Timetable Generator
set -e

# Detect JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 17+)
    fi
fi

echo "Using Java: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"

# Create output directory
mkdir -p target/classes target/test-classes

# Copy resources to target/classes
cp -r src/main/resources/* target/classes/ 2>/dev/null || true

# Find all java sources
find src/main/java -name "*.java" > target/sources.txt

# Classpath with jars in lib/ (if present)
CP="target/classes"
for jar in lib/*.jar; do
    if [ -f "$jar" ]; then
        CP="$CP:$jar"
    fi
done

echo "Compiling main sources..."
$JAVA_HOME/bin/javac -d target/classes -cp "$CP" @target/sources.txt

echo "Compiling test sources..."
$JAVA_HOME/bin/javac -d target/test-classes -cp "target/classes:$CP" \
    src/test/java/in/frcce/ecs/service/StandaloneClashTest.java \
    src/test/java/in/frcce/ecs/service/SeedDataSchedulingTest.java

echo "Compilation successful!"

