#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: scripts/set-version.sh <version>" >&2
  exit 2
fi

VERSION="$1"

case "$VERSION" in
  v*)
    echo "Version must not include the leading v tag prefix: $VERSION" >&2
    exit 2
    ;;
  "")
    echo "Version must not be empty" >&2
    exit 2
    ;;
esac

mvn -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false
perl -0pi -e "s/version = \"[^\"]+\"/version = \"$VERSION\"/" depviz-gradle-plugin/build.gradle.kts
