#!/usr/bin/env bash
# Initialize the MySQL database schema and seed data directly via JDBC
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
    ./compile.sh
fi

CP="target/classes"
for jar in lib/*.jar; do
    if [ -f "$jar" ]; then
        CP="$CP:$jar"
    fi
done

$JAVA_HOME/bin/java -cp "$CP" in.frcce.ecs.util.DBInitializer

