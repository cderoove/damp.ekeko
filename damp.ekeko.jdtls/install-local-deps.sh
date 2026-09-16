#!/usr/bin/env bash
# damp.ekeko.jdtls needs two jars that aren't published to any public Maven
# repository, so the pom.xml can't just declare a normal <dependency>:
#
#   - org.eclipse.jdt.ls.core: eclipse.jdt.ls is built and distributed as a
#     Tycho/p2 product, not published to Maven Central. We need it purely
#     for the IDelegateCommandHandler interface, compiled against whatever
#     version the installed redhat.java (Language Support for Java)
#     extension actually bundles, since that's the runtime we install into.
#   - qwal: damp.ekeko's own small regular-path-expression dependency
#     (damp.ekeko/damp.libs/libs/qwal-1.0.3-SNAPSHOT.jar), never published
#     anywhere public either.
#
# This script installs both into the local Maven repository (~/.m2) under
# coordinates this project's pom.xml depends on. Run it once before
# building (mvn -f pom.xml verify will fail with "could not find artifact"
# until this has been run at least once for the current ~/.m2).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MVN="$SCRIPT_DIR/../../eclipse.jdt.ls/mvnw"
if [ ! -x "$MVN" ]; then
  echo "Expected the eclipse.jdt.ls Maven wrapper at $MVN (for a consistent Maven version) but it's not there; falling back to 'mvn' on PATH." >&2
  MVN="mvn"
fi

SEARCH_DIRS=()
for d in "$HOME/.vscode-oss/extensions" "$HOME/.vscode/extensions"; do
  [ -d "$d" ] && SEARCH_DIRS+=("$d")
done
EXT_DIR=""
if [ "${#SEARCH_DIRS[@]}" -gt 0 ]; then
  EXT_DIR="$(find "${SEARCH_DIRS[@]}" -maxdepth 1 -iname 'redhat.java-*' 2>/dev/null | sort -V | tail -1)"
fi
if [ -z "$EXT_DIR" ]; then
  echo "Could not find an installed redhat.java extension under ~/.vscode-oss or ~/.vscode." >&2
  echo "Install it first, e.g.: codium --install-extension redhat.java" >&2
  exit 1
fi

JDTLS_CORE_JAR="$(find "$EXT_DIR/server/plugins" -maxdepth 1 -iname 'org.eclipse.jdt.ls.core_*.jar' | sort -V | tail -1)"
if [ -z "$JDTLS_CORE_JAR" ]; then
  echo "Could not find org.eclipse.jdt.ls.core_*.jar under $EXT_DIR/server/plugins" >&2
  exit 1
fi
JDTLS_CORE_VERSION="$(basename "$JDTLS_CORE_JAR" .jar | sed 's/^org\.eclipse\.jdt\.ls\.core_//')"
echo "Installing org.eclipse.jdt.ls.core:$JDTLS_CORE_VERSION from $JDTLS_CORE_JAR"
"$MVN" -q org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file \
  -Dfile="$JDTLS_CORE_JAR" \
  -DgroupId=org.eclipse.jdt.ls \
  -DartifactId=org.eclipse.jdt.ls.core \
  -Dversion="$JDTLS_CORE_VERSION" \
  -Dpackaging=jar

QWAL_JAR="$SCRIPT_DIR/../damp.libs/libs/qwal-1.0.3-SNAPSHOT.jar"
if [ ! -f "$QWAL_JAR" ]; then
  echo "Could not find qwal jar at $QWAL_JAR" >&2
  exit 1
fi
echo "Installing damp:qwal:1.0.3-SNAPSHOT from $QWAL_JAR"
"$MVN" -q org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file \
  -Dfile="$QWAL_JAR" \
  -DgroupId=damp \
  -DartifactId=qwal \
  -Dversion=1.0.3-SNAPSHOT \
  -Dpackaging=jar

echo "Done. Detected org.eclipse.jdt.ls.core version: $JDTLS_CORE_VERSION"
echo "If pom.xml's <jdtls-core.version> doesn't match, update it to that value."
