#!/usr/bin/env sh
set -eu
PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
MODE=${1:-client}
if [ "$#" -gt 0 ]; then shift; fi
case "$MODE" in
    server|client) ;;
    *) echo "Usage: sh scripts/run.sh [server [port]|client]" >&2; exit 2 ;;
esac
if [ "$MODE" = client ] && [ "$#" -ne 0 ]; then
    echo "The client accepts its connection settings in the login window." >&2
    exit 2
fi
if [ ! -f "$PROJECT_ROOT/dist/eclassroom.jar" ]; then
    sh "$PROJECT_ROOT/scripts/build.sh"
fi
if [ "$MODE" = server ]; then
    exec java -Dfile.encoding=UTF-8 -cp "$PROJECT_ROOT/dist/eclassroom.jar" eclassroom.Server "$@"
else
    exec java -Dfile.encoding=UTF-8 -jar "$PROJECT_ROOT/dist/eclassroom.jar"
fi
