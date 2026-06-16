#!/bin/sh
set -eu

VERSION="${DEPVIZ_VERSION:-0.1.0}"
BASE_URL="${DEPVIZ_BASE_URL:-https://github.com/lucasgois1/depviz/releases/download/v${VERSION}}"
INSTALL_DIR="${DEPVIZ_HOME:-$HOME/.depviz}"
BIN_DIR="$INSTALL_DIR/bin"
LIB_DIR="$INSTALL_DIR/lib"
M2_REPO="${MAVEN_LOCAL_REPOSITORY:-$HOME/.m2/repository}"

mkdir -p "$BIN_DIR" "$LIB_DIR" "$M2_REPO"

TMP_JAR="$LIB_DIR/depviz-cli.jar.tmp.$$"
TMP_MAVEN_REPO="$LIB_DIR/depviz-maven-repository.tar.gz.tmp.$$"
trap 'rm -f "$TMP_JAR" "$TMP_MAVEN_REPO"' EXIT HUP INT TERM

if [ -n "${DEPVIZ_LOCAL_JAR:-}" ]; then
  cp "$DEPVIZ_LOCAL_JAR" "$TMP_JAR"
else
  curl -fsSL "$BASE_URL/depviz-cli.jar" -o "$TMP_JAR"
fi
mv "$TMP_JAR" "$LIB_DIR/depviz-cli.jar"

if [ -n "${DEPVIZ_LOCAL_MAVEN_REPOSITORY:-}" ]; then
  tar -C "$M2_REPO" -xzf "$DEPVIZ_LOCAL_MAVEN_REPOSITORY"
elif curl -fsSL "$BASE_URL/depviz-maven-repository.tar.gz" -o "$TMP_MAVEN_REPO"; then
  tar -C "$M2_REPO" -xzf "$TMP_MAVEN_REPO"
else
  echo "Depviz Maven repository asset was not found for $VERSION; Maven/Gradle may need to resolve Depviz plugins from configured repositories." >&2
fi

cat > "$BIN_DIR/depviz" <<'LAUNCHER'
#!/bin/sh

case "$0" in
  */*) SCRIPT_PATH="$0" ;;
  *) SCRIPT_PATH="$(command -v "$0")" ;;
esac

SCRIPT_DIR="$(CDPATH= cd "$(dirname "$SCRIPT_PATH")" && pwd -P)"
JAR_PATH="$SCRIPT_DIR/../lib/depviz-cli.jar"

if [ -n "${JAVA_HOME:-}" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
  if [ ! -x "$JAVA_CMD" ]; then
    echo "Depviz requires Java 17+, but JAVA_HOME does not contain an executable bin/java: $JAVA_HOME" >&2
    exit 1
  fi
elif command -v java >/dev/null 2>&1; then
  JAVA_CMD="java"
else
  echo "Depviz requires Java 17+, but no java executable was found. Install Java 17+ or set JAVA_HOME." >&2
  exit 1
fi

JAVA_VERSION_OUTPUT="$("$JAVA_CMD" -version 2>&1)"
JAVA_VERSION="$(printf '%s\n' "$JAVA_VERSION_OUTPUT" | sed -n 's/.*version "\([^"]*\)".*/\1/p' | sed -n '1p')"
case "$JAVA_VERSION" in
  1.*) JAVA_MAJOR="$(printf '%s\n' "$JAVA_VERSION" | sed 's/^1\.\([0-9][0-9]*\).*/\1/')" ;;
  *) JAVA_MAJOR="$(printf '%s\n' "$JAVA_VERSION" | sed 's/^\([0-9][0-9]*\).*/\1/')" ;;
esac

case "$JAVA_MAJOR" in
  ''|*[!0-9]*)
    echo "Depviz requires Java 17+, but could not determine Java version from $JAVA_CMD." >&2
    exit 1
    ;;
esac

if [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "Depviz requires Java 17+; found Java $JAVA_VERSION from $JAVA_CMD. Set JAVA_HOME to a Java 17+ installation or update PATH." >&2
  exit 1
fi

exec "$JAVA_CMD" -jar "$JAR_PATH" "$@"
LAUNCHER

chmod +x "$BIN_DIR/depviz"

echo "Depviz installed at $BIN_DIR/depviz"
case ":$PATH:" in
  *":$BIN_DIR:"*) ;;
  *)
    echo "Add this directory to your PATH to run depviz from anywhere:"
    echo "  $BIN_DIR"
    ;;
esac
