#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# Ensure JDK 21+ from Homebrew or environment is used
if [ -d "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home" ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
    export PATH="/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"
fi

# Choose maven executable: system mvn preferred, fallback to ./mvnw
if command -v mvn >/dev/null 2>&1; then
    MVN_CMD="mvn"
elif [ -x "./mvnw" ]; then
    MVN_CMD="./mvnw"
else
    echo "Error: Neither system mvn nor ./mvnw found."
    exit 1
fi

PROFILE="$1"

if [ -z "$PROFILE" ]; then
    # Auto-detect profile
    if nc -z localhost 1521 2>/dev/null; then
        PROFILE="oracle"
        echo "[✓] Oracle Database detected on port 1521. Using 'oracle' profile."
    else
        # Try starting docker container if docker is available
        if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
            echo "[*] Attempting to start Oracle Database container (oracle-free)..."
            docker start oracle-free 2>/dev/null || true
            for i in {1..5}; do
                if nc -z localhost 1521 2>/dev/null; then
                    PROFILE="oracle"
                    echo "[✓] Oracle Database ready on port 1521. Using 'oracle' profile."
                    break
                fi
                sleep 1
            done
        fi
        
        if [ -z "$PROFILE" ]; then
            PROFILE="h2"
            echo "[!] Oracle Database not detected on port 1521."
            echo "[*] Falling back to offline H2 profile (zero-config in-memory database)."
        fi
    fi
fi

# Ensure port 8080 is free
if lsof -ti :8080 >/dev/null 2>&1; then
    echo "[*] Port 8080 is in use. Freeing port 8080..."
    lsof -ti :8080 | xargs kill -9 2>/dev/null || true
    sleep 1
fi

echo "[*] Launching Spring Boot backend with profile: $PROFILE..."
exec $MVN_CMD spring-boot:run -Dspring-boot.run.profiles=$PROFILE
