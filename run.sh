#!/usr/bin/env bash
# Launch the ECS Faculty Timetable Generator GUI
set -e

# Detect JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    if [ -d "/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home"
    elif [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 17+)
    fi
fi

if [ ! -d "target/classes" ]; then
    echo "Compiling first..."
    ./compile.sh
fi

CP="target/classes"
for jar in lib/*.jar; do
    if [ -f "$jar" ]; then
        CP="$CP:$jar"
    fi
done

echo "Starting ECS Faculty Timetable Generator GUI..."
$JAVA_HOME/bin/java -cp "$CP" in.frcce.ecs.Main

