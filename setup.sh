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
#  Step 1 — apt prerequisites (needed for SDKMAN)
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
#  Step 3 — Gradle (via SDKMAN) + wrapper
# ═════════════════════════════════════════════════
echo -e "${YELLOW}[3/4] Ensuring Gradle + wrapper...${NC}"

if ! command -v gradle &>/dev/null; then
    echo -e "${YELLOW}  Installing Gradle 8.14 via SDKMAN...${NC}"
    # re-source sdkman in case it was just installed
    export SDKMAN_DIR="$HOME/.sdkman"
    [ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ] && source "$SDKMAN_DIR/bin/sdkman-init.sh"

    if command -v sdk &>/dev/null; then
        sdk install gradle 8.14
    else
        echo -e "${RED}  SDKMAN unavailable. Cannot install Gradle.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}  Gradle already installed.${NC}"
fi

if [ ! -f "./gradlew" ]; then
    gradle wrapper --gradle-version 8.14
    echo -e "${GREEN}  Gradle wrapper generated.${NC}"
else
    echo -e "${GREEN}  Gradle wrapper already present.${NC}"
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

chmod +x ./gradlew
exec ./gradlew run
