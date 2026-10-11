#!/bin/sh
set -eu
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to a Java 8 JDK}"
export GRADLE_USER_HOME="$PWD/.gradle-user"
if [ "${1:-}" = setup ]; then
    "$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain setupDecompWorkspace --no-daemon
fi
"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build --no-daemon
