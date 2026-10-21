#!/usr/bin/env sh
set -eu
PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
sh "$PROJECT_ROOT/scripts/build.sh"
PREVIEW_DIR=$(mktemp -d "$PROJECT_ROOT/build/preview.XXXXXX")
trap 'rm -rf -- "$PREVIEW_DIR"' EXIT HUP INT TERM
javac --release 17 -encoding UTF-8 -cp "$PROJECT_ROOT/dist/eclassroom.jar" \
    -d "$PREVIEW_DIR" "$PROJECT_ROOT/src/test/java/eclassroom/PreviewCapture.java"
java -cp "$PROJECT_ROOT/dist/eclassroom.jar:$PREVIEW_DIR" eclassroom.PreviewCapture "$PROJECT_ROOT/docs/assets"
