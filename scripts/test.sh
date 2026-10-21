#!/usr/bin/env sh
set -eu
PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
sh "$PROJECT_ROOT/scripts/build.sh"
TEST_DIR=$(mktemp -d "$PROJECT_ROOT/build/tests.XXXXXX")
trap 'rm -rf -- "$TEST_DIR"' EXIT HUP INT TERM
javac --release 17 -encoding UTF-8 -cp "$PROJECT_ROOT/dist/eclassroom.jar" \
    -d "$TEST_DIR" "$PROJECT_ROOT"/src/test/java/eclassroom/*.java
java -Djava.awt.headless=true -cp "$PROJECT_ROOT/dist/eclassroom.jar:$TEST_DIR" eclassroom.CoreTest
java -Djava.awt.headless=true -cp "$PROJECT_ROOT/dist/eclassroom.jar:$TEST_DIR" eclassroom.ServerIntegrationTest
