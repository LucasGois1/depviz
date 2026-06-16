#!/bin/sh
set -eu

VERSION="${DEPVIZ_VERSION:-0.1.0-SNAPSHOT}"
BASE_URL="${DEPVIZ_BASE_URL:-https://github.com/lucasgois1/depviz/releases/download/v${VERSION}}"
INSTALL_DIR="${DEPVIZ_HOME:-$HOME/.depviz}"
BIN_DIR="$INSTALL_DIR/bin"
LIB_DIR="$INSTALL_DIR/lib"

mkdir -p "$BIN_DIR" "$LIB_DIR"

if [ -n "${DEPVIZ_LOCAL_JAR:-}" ]; then
  cp "$DEPVIZ_LOCAL_JAR" "$LIB_DIR/depviz-cli.jar"
else
  curl -fsSL "$BASE_URL/depviz-cli.jar" -o "$LIB_DIR/depviz-cli.jar"
fi

cat > "$BIN_DIR/depviz" <<LAUNCHER
#!/bin/sh
exec java -jar "$LIB_DIR/depviz-cli.jar" "\$@"
LAUNCHER

chmod +x "$BIN_DIR/depviz"

echo "Depviz installed at $BIN_DIR/depviz"
case ":$PATH:" in
  *":$BIN_DIR:"*) ;;
  *) echo "Add this to your PATH: export PATH=\"$BIN_DIR:\$PATH\"" ;;
esac
