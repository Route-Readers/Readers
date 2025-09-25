#!/bin/sh

# Simple gradlew wrapper that avoids cache issues
GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.caching=false -Dorg.gradle.parallel=false"
INIT_SCRIPT="--init-script init.gradle"

if [ -f "gradlew.bat" ]; then
    cmd.exe /c "gradlew.bat $INIT_SCRIPT $@"
else
    echo "Using direct gradle execution to avoid lock issues"
    gradle $GRADLE_OPTS $INIT_SCRIPT "$@"
fi
