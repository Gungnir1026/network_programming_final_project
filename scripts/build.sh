#!/usr/bin/env sh
set -eu
PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
for tool in javac jar; do
    command -v "$tool" >/dev/null 2>&1 || {
        echo "Missing $tool. Install JDK 17 or newer and add its bin directory to PATH." >&2
        exit 1
    }
done
mkdir -p "$PROJECT_ROOT/build" "$PROJECT_ROOT/dist"
CLASS_DIR=$(mktemp -d "$PROJECT_ROOT/build/classes.XXXXXX")
trap 'rm -rf -- "$CLASS_DIR"' EXIT HUP INT TERM
javac --release 17 -encoding UTF-8 -d "$CLASS_DIR" "$PROJECT_ROOT"/src/main/java/eclassroom/*.java
cp -R "$PROJECT_ROOT/src/main/resources/." "$CLASS_DIR/"
jar --create --file "$PROJECT_ROOT/dist/eclassroom.jar" --main-class eclassroom.Main -C "$CLASS_DIR" .
echo "Built: $PROJECT_ROOT/dist/eclassroom.jar"
