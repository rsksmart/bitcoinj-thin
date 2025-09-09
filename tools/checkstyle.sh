#!/usr/bin/env bash
# checkstyle.sh
# Runs Checkstyle on either the files pre-commit passes in ($@),
# or on staged .java files, or (in CI) on all tracked .java files.

set -euo pipefail

# --- repo-relative locations (adjust if yours differ) ---
CHECKSTYLE_JAR="tools/checkstyle-8.45.1-all.jar"
CHECKSTYLE_CONFIG="tools/checkstyle.xml"

# --- move to repo root so relative paths resolve correctly ---
REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

# --- sanity checks ---
[ -f "$CHECKSTYLE_JAR" ]    || { echo "Missing $CHECKSTYLE_JAR"; exit 1; }
[ -f "$CHECKSTYLE_CONFIG" ] || { echo "Missing $CHECKSTYLE_CONFIG"; exit 1; }

FILES=()

if [ "$#" -gt 0 ]; then
  # pre-commit will pass the staged Java files here when pass_filenames:true
  # Preserve each filename exactly as an array element.
  for f in "$@"; do
    FILES+=("$f")
  done
else
  # Collect staged .java files (Added, Copied, Modified, Renamed)
  # Use NUL delimiters (-z) and read -d '' to be safe with spaces/newlines.
  while IFS= read -r -d '' f; do
    FILES+=("$f")
  done < <(git diff --cached --name-only --diff-filter=ACMR -z -- '*.java')

  # If none staged (e.g., in CI), check all tracked .java files
  if [ "${#FILES[@]}" -eq 0 ]; then
    while IFS= read -r -d '' f; do
      FILES+=("$f")
    done < <(git ls-files -z -- '*.java')
  fi
fi

# Nothing to check → exit cleanly
if [ "${#FILES[@]}" -eq 0 ]; then
  echo "No Java files to check."
  exit 0
fi

echo "Running Checkstyle on:"
for f in "${FILES[@]}"; do
  echo " - $f"
done

# One JVM start, all files at once (faster)
if ! java -jar "$CHECKSTYLE_JAR" -c "$CHECKSTYLE_CONFIG" "${FILES[@]}"; then
  echo "❌ Checkstyle violations found. Commit/CI failed."
  exit 1
fi

echo "✅ Checkstyle passed."
