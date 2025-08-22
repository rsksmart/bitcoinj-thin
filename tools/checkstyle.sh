#!/bin/bash
#set -euo pipefail

CHECKSTYLE_JAR="checkstyle-11.0.0-all.jar"
CHECKSTYLE_CONFIG="checkstyle.xml"

# Collect staged Java files (Added, Copied, Modified, Renamed)
REPO_ROOT=$(git rev-parse --show-toplevel)

STAGED_JAVA_FILES=$(git diff --cached --name-only --diff-filter=ACMR | grep java | sed "s|^|$REPO_ROOT/|")

if [ -n "$STAGED_JAVA_FILES" ]; then
  echo "Running Checkstyle on staged Java files..."
  java -jar "$REPO_ROOT/tools/$CHECKSTYLE_JAR" -c "$REPO_ROOT/tools/$CHECKSTYLE_CONFIG" $STAGED_JAVA_FILES
  status=$?
  if [ $status -ne 0 ]; then
    echo "❌ Checkstyle violations found. Commit aborted."
    exit $status
  fi
  echo "✅ Checkstyle passed."
fi

exit 0
