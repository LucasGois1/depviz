#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: scripts/prepare-release-assets.sh <version>" >&2
  exit 2
fi

VERSION="$1"
DIST_DIR="${DIST_DIR:-dist}"
CLI_JAR="depviz-cli/target/depviz-cli-${VERSION}.jar"
GROUP_PATH="io/github/lucasgois1/depviz"
LOCAL_REPO_DIR="$DIST_DIR/maven-repository"

if [ ! -f "$CLI_JAR" ]; then
  echo "CLI jar not found: $CLI_JAR" >&2
  echo "Run: mvn -B -DskipTests package" >&2
  exit 1
fi

if [ ! -f "depviz-gradle-plugin/build/libs/depviz-gradle-plugin-${VERSION}.jar" ] ||
   [ ! -f "depviz-gradle-plugin/build/publications/pluginMaven/pom-default.xml" ]; then
  ./gradlew -q :depviz-gradle-plugin:jar :depviz-gradle-plugin:generatePomFileForPluginMavenPublication --no-daemon
fi

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

cp "$CLI_JAR" "$DIST_DIR/depviz-cli.jar"
cp scripts/install.sh "$DIST_DIR/install.sh"
chmod +x "$DIST_DIR/install.sh"

copy_maven_artifact() {
  ARTIFACT_ID="$1"
  PACKAGING="$2"
  ARTIFACT_FILE="$3"
  POM_FILE="$4"
  TARGET_DIR="$LOCAL_REPO_DIR/$GROUP_PATH/$ARTIFACT_ID/$VERSION"

  if [ ! -f "$POM_FILE" ]; then
    echo "POM not found: $POM_FILE" >&2
    exit 1
  fi

  mkdir -p "$TARGET_DIR"
  cp "$POM_FILE" "$TARGET_DIR/$ARTIFACT_ID-${VERSION}.pom"

  if [ "$PACKAGING" != "pom" ]; then
    if [ ! -f "$ARTIFACT_FILE" ]; then
      echo "Artifact not found: $ARTIFACT_FILE" >&2
      exit 1
    fi
    cp "$ARTIFACT_FILE" "$TARGET_DIR/$ARTIFACT_ID-${VERSION}.$PACKAGING"
  fi
}

copy_maven_artifact "depviz-parent" "pom" "" "pom.xml"
copy_maven_artifact "depviz-core" "jar" "depviz-core/target/depviz-core-${VERSION}.jar" "depviz-core/pom.xml"
copy_maven_artifact "depviz-maven-plugin" "jar" "depviz-maven-plugin/target/depviz-maven-plugin-${VERSION}.jar" "depviz-maven-plugin/pom.xml"
copy_maven_artifact "depviz-gradle-plugin" "jar" "depviz-gradle-plugin/build/libs/depviz-gradle-plugin-${VERSION}.jar" "depviz-gradle-plugin/build/publications/pluginMaven/pom-default.xml"

tar -C "$LOCAL_REPO_DIR" -czf "$DIST_DIR/depviz-maven-repository.tar.gz" .

(
  cd "$DIST_DIR"
  shasum -a 256 depviz-cli.jar depviz-maven-repository.tar.gz install.sh > checksums.txt
)

echo "Release assets written to $DIST_DIR"
