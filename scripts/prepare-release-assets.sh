#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: scripts/prepare-release-assets.sh <version>" >&2
  exit 2
fi

VERSION="$1"
DIST_DIR="${DIST_DIR:-dist}"
CLI_JAR="depviz-cli/target/depviz-cli-${VERSION}.jar"

if [ ! -f "$CLI_JAR" ]; then
  echo "CLI jar not found: $CLI_JAR" >&2
  echo "Run: mvn -B -DskipTests package" >&2
  exit 1
fi

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

cp "$CLI_JAR" "$DIST_DIR/depviz-cli.jar"
cp scripts/install.sh "$DIST_DIR/install.sh"
chmod +x "$DIST_DIR/install.sh"

(
  cd "$DIST_DIR"
  shasum -a 256 depviz-cli.jar install.sh > checksums.txt
)

echo "Release assets written to $DIST_DIR"
