#!/usr/bin/env bash
set -e

# ═══════════════════════════════════════════════════════════
#  Chain Reaction — One-Command Linux Installer
#  Ubuntu 18.04 / 22.04 (Debian-based)
# ═══════════════════════════════════════════════════════════

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

banner() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║     Chain Reaction — Linux Quick Setup       ║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
    echo ""
}

# ── helpers ─────────────────────────────────────

has_java25() {
    if command -v java &>/dev/null; then
        local ver
        ver=$(java -version 2>&1 | head -1 | grep -oP '"[0-9]+')
        ver="${ver//\"/}"
        if [ "$ver" = "25" ]; then
            return 0
        fi
    fi
    return 1
}

find_sdkman_java25() {
    if [ -d "$HOME/.sdkman/candidates/java" ]; then
        find "$HOME/.sdkman/candidates/java" -maxdepth 1 -name "25*" -type d 2>/dev/null | head -1
    fi
}

ensure_dir() {
    mkdir -p "$1" 2>/dev/null || true
}

# ─────────────────────────────────────────────────

banner

# ═════════════════════════════════════════════════
#  Step 1 — apt prerequisites
# ═════════════════════════════════════════════════
echo -e "${YELLOW}[1/4] Checking prerequisites (curl, unzip, zip)...${NC}"

MISSING=""
command -v curl  &>/dev/null || MISSING="$MISSING curl"
command -v unzip &>/dev/null || MISSING="$MISSING unzip"
command -v zip   &>/dev/null || MISSING="$MISSING zip"

if [ -n "$MISSING" ]; then
    echo -e "  Missing:${MISSING}"
    if command -v apt-get &>/dev/null; then
        sudo apt-get update -qq
        sudo apt-get install -y -qq curl unzip zip
    elif command -v dnf &>/dev/null; then
        sudo dnf install -y curl unzip zip
    elif command -v yum &>/dev/null; then
        sudo yum install -y curl unzip zip
    else
        echo -e "${RED}  Unsupported package manager. Install curl, unzip, zip manually.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}  All present.${NC}"
fi

# ═════════════════════════════════════════════════
#  Step 2 — Java 25 LTS
# ═════════════════════════════════════════════════
echo -e "${YELLOW}[2/4] Ensuring Java 25 LTS...${NC}"

JAVA25_READY=false

# --- check 1: already the active java ---
if has_java25; then
    echo -e "${GREEN}  Java 25 is already the active JDK.${NC}"
    JAVA25_READY=true
fi

# --- check 2: SDKMAN already has Java 25 installed ---
if [ "$JAVA25_READY" = false ]; then
    SDKMAN_JAVA25=$(find_sdkman_java25)
    if [ -n "$SDKMAN_JAVA25" ]; then
        export JAVA_HOME="$SDKMAN_JAVA25"
        export PATH="$JAVA_HOME/bin:$PATH"
        if has_java25; then
            echo -e "${GREEN}  Java 25 found via SDKMAN.${NC}"
            JAVA25_READY=true
        fi
    fi
fi

# --- check 3: install via SDKMAN ---
if [ "$JAVA25_READY" = false ]; then
    echo -e "${YELLOW}  Java 25 not found. Installing via SDKMAN...${NC}"

    if [ ! -d "$HOME/.sdkman" ]; then
        curl -s "https://get.sdkman.io" | bash
    fi

    export SDKMAN_DIR="$HOME/.sdkman"
    if [ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
        source "$SDKMAN_DIR/bin/sdkman-init.sh"
    fi

    if command -v sdk &>/dev/null; then
        # find a Java 25 candidate (prefer temurin)
        JAVA25_ID=$(sdk list java 2>/dev/null | grep -oP '25[^\s\|]*tem[^\s\|]*' | head -1 || true)
        if [ -z "$JAVA25_ID" ]; then
            JAVA25_ID=$(sdk list java 2>/dev/null | grep -oP '25[^\s\|]*' | head -1 || true)
        fi

        if [ -n "$JAVA25_ID" ]; then
            sdk install java "$JAVA25_ID"
            SDKMAN_JAVA25=$(find_sdkman_java25)
            if [ -n "$SDKMAN_JAVA25" ]; then
                export JAVA_HOME="$SDKMAN_JAVA25"
                export PATH="$JAVA_HOME/bin:$PATH"
                JAVA25_READY=true
            fi
        fi
    fi
fi

# --- check 4: direct Adoptium download (fallback) ---
if [ "$JAVA25_READY" = false ]; then
    echo -e "${YELLOW}  SDKMAN couldn't install Java 25. Trying Adoptium direct download...${NC}"

    JAVA25_HOME="$HOME/jdk-25"
    JAVA_URL="https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse"

    ensure_dir "$JAVA25_HOME"
    curl -L --progress-bar -o /tmp/jdk25.tar.gz "$JAVA_URL"
    tar -xzf /tmp/jdk25.tar.gz -C "$JAVA25_HOME" --strip-components=1
    rm -f /tmp/jdk25.tar.gz

    export JAVA_HOME="$JAVA25_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"

    if has_java25; then
        echo -e "${GREEN}  Java 25 installed to ~/jdk-25${NC}"
        JAVA25_READY=true

        # persist for future shell sessions
        if ! grep -q "jdk-25" "$HOME/.bashrc" 2>/dev/null; then
            echo "" >> "$HOME/.bashrc"
            echo "# Chain Reaction — Java 25" >> "$HOME/.bashrc"
            echo "export JAVA_HOME=\"\$HOME/jdk-25\"" >> "$HOME/.bashrc"
            echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\"" >> "$HOME/.bashrc"
        fi
    else
        echo -e "${RED}  Failed to install Java 25.${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}  Java: $(java -version 2>&1 | head -1)${NC}"
echo -e "${GREEN}  JAVA_HOME: ${JAVA_HOME}${NC}"

# ═════════════════════════════════════════════════
#  Step 3 — Gradle wrapper (no system Gradle needed)
#  Gradle 9.x supports Java 25 (fixes "class file
#  major version 69" error that 8.14 hits).
# ═════════════════════════════════════════════════
echo -e "${YELLOW}[3/4] Bootstrapping Gradle wrapper (Gradle 9.x)...${NC}"

GRADLE_VERSION="9.2"
WRAPPER_DIR="gradle/wrapper"
ensure_dir "$WRAPPER_DIR"

# Only regenerate if the wrapper is missing or using the wrong Gradle version
NEED_WRAPPER=false

if [ ! -f "./gradlew" ] || [ ! -f "$WRAPPER_DIR/gradle-wrapper.jar" ]; then
    NEED_WRAPPER=true
elif [ -f "$WRAPPER_DIR/gradle-wrapper.properties" ]; then
    if ! grep -q "gradle-${GRADLE_VERSION}" "$WRAPPER_DIR/gradle-wrapper.properties" 2>/dev/null; then
        echo -e "${YELLOW}  Upgrading wrapper from 8.14 to ${GRADLE_VERSION}...${NC}"
        NEED_WRAPPER=true
    fi
else
    NEED_WRAPPER=true
fi

if [ "$NEED_WRAPPER" = true ]; then
    # Download the wrapper JAR from the Gradle 9.x GitHub release
    echo -e "  Downloading gradle-wrapper.jar..."
    WRAPPER_JAR_URL="https://github.com/gradle/gradle/raw/v${GRADLE_VERSION}.0/gradle/wrapper/gradle-wrapper.jar"
    curl -L --progress-bar -o "$WRAPPER_DIR/gradle-wrapper.jar" "$WRAPPER_JAR_URL"

    # Write gradle-wrapper.properties
    cat > "$WRAPPER_DIR/gradle-wrapper.properties" << PROPS_EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
PROPS_EOF

    # Write gradlew shell script
    cat > ./gradlew << 'GRADLEW_EOF'
#!/bin/sh

#
# Gradle start up script for POSIX generated by Gradle.
#

# Attempt to set APP_HOME
PRG="$0"
while [ -h "$PRG" ]; do
    ls=$(ls -ld "$PRG")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=$(dirname "$PRG")/"$link"
    fi
done
SAVED="$(pwd)"
cd "$(dirname "$PRG")/" >/dev/null
APP_HOME="$(pwd -P)"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

MAX_FD="maximum"

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
    CYGWIN* ) cygwin=true ;;
    Darwin* ) darwin=true ;;
    MSYS* | MINGW* ) msys=true ;;
    NonStop* ) nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Auto-detect Java 25 from locations setup.sh may have installed it to.
# This makes "./gradlew run" work standalone without pre-sourcing anything.
if [ -z "$JAVA_HOME" ]; then
    if [ -d "$HOME/jdk-25" ]; then
        JAVA_HOME="$HOME/jdk-25"
    elif [ -d "$HOME/.sdkman/candidates/java" ]; then
        JAVA25_DIR=$(find "$HOME/.sdkman/candidates/java" -maxdepth 1 -name "25*" -type d 2>/dev/null | head -1)
        if [ -n "$JAVA25_DIR" ]; then
            JAVA_HOME="$JAVA25_DIR"
        fi
    fi
    if [ -n "$JAVA_HOME" ]; then
        export JAVA_HOME
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" ] && [ "$darwin" = "false" ] && [ "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=$(ulimit -H -n)
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" ] || [ "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# Collect all arguments for the java command, stracks://the current JVM options in the environment.
# See https://docs.gradle.org/current/userguide/build_environment.html
if $cygwin || $msys ; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Dfile.encoding=UTF-8\""
fi

exec "$JAVACMD" \
    $DEFAULT_JVM_OPTS \
    $JAVA_OPTS \
    $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
GRADLEW_EOF

    chmod +x ./gradlew
    echo -e "${GREEN}  Gradle wrapper generated (Gradle ${GRADLE_VERSION}).${NC}"
else
    echo -e "${GREEN}  Gradle wrapper already present (Gradle ${GRADLE_VERSION}).${NC}"
fi

# ═════════════════════════════════════════════════
#  Step 4 — Build & Launch
# ═════════════════════════════════════════════════
echo ""
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo -e "${BOLD}${GREEN}  Setup complete! Launching Chain Reaction...  ${NC}"
echo -e "${CYAN}════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Next time just run:  ${BOLD}./gradlew run${NC}"
echo ""

exec ./gradlew run
