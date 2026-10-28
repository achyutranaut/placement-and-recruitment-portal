#!/usr/bin/env bash
# ==============================================================================
# STUDENT PLACEMENT & RECRUITMENT PORTAL — UNIFIED LAUNCHER
# ==============================================================================
# Usage:
#   ./run_portal.sh [OPTIONS]
#
# Options:
#   --help, -h       Display this help message and exit.
#   --status         Inspect database and environment readiness without starting.
#   --oracle-only    Explicitly disable Neo4j Aura cloud graph projection.
#   --db <engine>    Specify primary database engine (only 'oracle' is supported).
# ==============================================================================

set -o pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

# ------------------------------------------------------------------------------
# CLI Options Parsing
# ------------------------------------------------------------------------------
SHOW_STATUS_ONLY=false
FORCE_ORACLE_ONLY=false
STOP_SERVICES=false
DAEMON_MODE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h)
      echo "Student Placement & Recruitment Portal Launcher"
      echo ""
      echo "Usage: ./run_portal.sh [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --help, -h       Show this help message and exit"
      echo "  --status         Check environment, Oracle container, and ports without starting"
      echo "  --oracle-only    Force disable Neo4j Aura projection (run in Oracle-only mode)"
      echo "  --daemon, -d     Launch services in the background and detach immediately"
      echo "  --stop           Cleanly stop active portal services belonging to this project"
      echo "  --db oracle      Explicitly specify database engine (Oracle Database 23ai is primary)"
      echo ""
      echo "Environment Variables (via .env or shell):"
      echo "  JWT_SECRET             256-bit signing key (ephemeral key generated if empty)"
      echo "  ORACLE_HOST            Oracle listener host (default: localhost)"
      echo "  ORACLE_PORT            Oracle listener port (default: 1521)"
      echo "  ORACLE_SERVICE         Oracle PDB name (default: FREEPDB1)"
      echo "  ORACLE_USER            Oracle app user (default: placement_app)"
      echo "  ORACLE_PASSWORD        Oracle app password (default: OraclePassword123!)"
      echo "  PORTAL_NEO4J_ENABLED   Enable Neo4j Aura graph projection (default: false)"
      echo "  NEO4J_URI              Neo4j Aura cloud URI (e.g. neo4j+s://...databases.neo4j.io)"
      echo "  NEO4J_USERNAME         Neo4j Aura username (default: neo4j)"
      echo "  NEO4J_PASSWORD         Neo4j Aura password"
      echo "  PORTAL_QDRANT_ENABLED  Enable Qdrant vector search engine (default: true if detected)"
      echo "  QDRANT_URL             Qdrant HTTP endpoint (default: http://localhost:6333)"
      echo "  QDRANT_API_KEY         Qdrant API key (optional for local instance)"
      echo "  QDRANT_COLLECTION      Qdrant collection name (default: placement-semantic-search)"
      echo "  SERVER_PORT            Spring Boot backend HTTP port (default: 8080)"
      echo "  FRONTEND_PORT          React/Vite frontend HTTP port (default: 3001)"
      exit 0
      ;;
    --status)
      SHOW_STATUS_ONLY=true
      shift
      ;;
    --daemon|-d|--detach)
      DAEMON_MODE=true
      shift
      ;;
    --stop)
      STOP_SERVICES=true
      shift
      ;;
    --oracle-only)
      FORCE_ORACLE_ONLY=true
      shift
      ;;
    --db)
      if [[ -z "$2" || "$2" != "oracle" ]]; then
        echo "[!] Unsupported database engine: '${2:-}'."
        echo "[!] Oracle Database 23ai Free is the required authoritative transactional database."
        echo "[!] Fallback to H2 or standalone Neo4j is not permitted."
        exit 1
      fi
      shift 2
      ;;
    *)
      echo "[!] Unknown option: $1 (Run ./run_portal.sh --help for available options)"
      exit 1
      ;;
  esac
done

echo "================================================================="
echo "   Student Placement & Recruitment Portal"
echo "   Dual-Engine Architecture: Oracle 23ai (OLTP) + Neo4j Aura (Graph)"
echo "================================================================="

# ------------------------------------------------------------------------------
# Step 1: Load Environment Configuration Securely
# ------------------------------------------------------------------------------
# Precedence Order (highest to lowest):
#   1. Caller's pre-existing shell environment (explicitly exported variables before script invocation)
#   2. Root-level .env ($DIR/.env)
#   3. Backend .env ($DIR/backend/.env)
#   4. Infrastructure .env ($DIR/infrastructure/.env)
#   5. Default values defined in run_portal.sh
# ------------------------------------------------------------------------------
INITIAL_EXPLICIT_VARS=()
for var in JWT_SECRET PORTAL_NEO4J_ENABLED NEO4J_URI NEO4J_USERNAME NEO4J_PASSWORD \
           PORTAL_QDRANT_ENABLED QDRANT_URL QDRANT_API_KEY QDRANT_COLLECTION \
           ORACLE_HOST ORACLE_PORT ORACLE_SERVICE ORACLE_USER ORACLE_PASSWORD \
           SERVER_PORT FRONTEND_PORT; do
  if [[ -n "${!var+x}" ]]; then
    INITIAL_EXPLICIT_VARS+=("$var")
  fi
done

DOTENV_LOADED=false

load_env_file() {
  local env_file="$1"
  local file_label="$2"
  [[ -f "$env_file" ]] || return 0

  local loaded_count=0
  while IFS= read -r raw_line || [[ -n "$raw_line" ]]; do
    local line
    line=$(echo "$raw_line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
    [[ -z "$line" || "$line" =~ ^# ]] && continue

    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      local key="${BASH_REMATCH[1]}"
      local val="${BASH_REMATCH[2]}"
      if [[ "$val" =~ ^\"(.*)\"$ ]]; then
        val="${BASH_REMATCH[1]}"
      elif [[ "$val" =~ ^\'(.*)\'$ ]]; then
        val="${BASH_REMATCH[1]}"
      fi

      local is_caller_explicit=false
      for explicit_var in "${INITIAL_EXPLICIT_VARS[@]}"; do
        if [[ "$explicit_var" == "$key" ]]; then
          is_caller_explicit=true
          break
        fi
      done

      if [[ "$is_caller_explicit" == "false" ]]; then
        export "$key"="$val"
        ((loaded_count++))
      fi
    fi
  done < "$env_file"

  echo "[✓] Loaded configuration from $file_label ($loaded_count variables)."
  DOTENV_LOADED=true
}

# Load files in ascending precedence order:
load_env_file "$DIR/../backend/.env" "root backend/.env"
load_env_file "$DIR/../.env" "repository root .env"
load_env_file "$DIR/infrastructure/.env" "infrastructure/.env"
load_env_file "$DIR/backend/.env" "backend/.env"
load_env_file "$DIR/.env" "project .env"

if [[ "$DOTENV_LOADED" != "true" ]]; then
  echo "[i] No local .env file detected. Using active environment configuration."
  echo "    To customize local settings: cp .env.example .env"
fi

# Override if --oracle-only was passed on command line
if [[ "$FORCE_ORACLE_ONLY" == "true" ]]; then
  export PORTAL_NEO4J_ENABLED="false"
fi

# ------------------------------------------------------------------------------
# Step 2: Validate Prerequisites
# ------------------------------------------------------------------------------
echo ""
echo "[1/5] Validating runtime prerequisites..."

# Auto-detect modern JDK (Java 21+) if JAVA_HOME is unset or current java in PATH is older
CURRENT_JAVA_MAJOR=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}' || echo "0")
if [[ -z "$JAVA_HOME" || "$CURRENT_JAVA_MAJOR" -lt 21 ]]; then
  if [[ -d "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home" ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
  elif [[ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
  elif [[ -d "/opt/homebrew/opt/openjdk" ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk"
  elif command -v /usr/libexec/java_home >/dev/null 2>&1 && /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 21)
  fi
  if [[ -n "$JAVA_HOME" && -d "$JAVA_HOME/bin" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
fi

# Check Java 21+
if ! command -v java >/dev/null 2>&1; then
  echo "[!] ERROR: Java is not installed or not found in PATH."
  echo "    Please install JDK 21 (e.g. 'brew install openjdk@21') and add it to PATH."
  exit 1
fi

JAVA_VERSION_OUTPUT=$(java -version 2>&1 | head -n 1)
JAVA_MAJOR=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [[ -z "$JAVA_MAJOR" || "$JAVA_MAJOR" -lt 21 ]]; then
  echo "[!] ERROR: Java 21 or newer is required. Detected: $JAVA_VERSION_OUTPUT"
  echo "    Please configure JAVA_HOME to point to JDK 21."
  exit 1
fi
echo "[✓] Java runtime: $JAVA_VERSION_OUTPUT (JAVA_HOME: ${JAVA_HOME:-system})"

# Check Maven
if ! command -v mvn >/dev/null 2>&1; then
  echo "[!] ERROR: Apache Maven (mvn) is not installed or not in PATH."
  echo "    Please install Maven (e.g. 'brew install maven')."
  exit 1
fi
echo "[✓] Apache Maven: $(mvn -version 2>&1 | head -n 1)"

# Check Node.js and npm
if ! command -v node >/dev/null 2>&1; then
  echo "[!] ERROR: Node.js is not installed or not in PATH."
  echo "    Please install Node.js 20+ (e.g. 'brew install node')."
  exit 1
fi
if ! command -v npm >/dev/null 2>&1; then
  echo "[!] ERROR: npm is not installed or not in PATH."
  exit 1
fi
echo "[✓] Node.js: $(node -v) / npm: $(npm -v)"

# ------------------------------------------------------------------------------
# Step 3: Configure Environment Variables & Secrets
# ------------------------------------------------------------------------------
# Secure JWT Secret Management
if [[ -z "$JWT_SECRET" ]]; then
  export JWT_SECRET=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | xxd -p -c 32)
  echo "[*] Generated ephemeral 256-bit JWT_SECRET for this runtime session."
else
  echo "[✓] Using configured JWT_SECRET."
fi

# Oracle Connection Variables
export ORACLE_HOST="${ORACLE_HOST:-localhost}"
export ORACLE_PORT="${ORACLE_PORT:-1521}"
export ORACLE_SERVICE="${ORACLE_SERVICE:-FREEPDB1}"
export ORACLE_USER="${ORACLE_USER:-placement_app}"
# In Oracle Multitenant (PDB FREEPDB1), local users cannot have c## prefix (ORA-65094).
# Automatically strip c## or C## prefix if provided.
if [[ "$ORACLE_USER" =~ ^[cC]## ]]; then
  export ORACLE_USER="${ORACLE_USER:3}"
fi
export ORACLE_PASSWORD="${ORACLE_PASSWORD:-OraclePassword123!}"

# Neo4j Projection Helper & Smart Auto-Configuration
is_neo4j_listening() {
  local host="${1:-localhost}"
  local port="${2:-7687}"
  if command -v nc >/dev/null 2>&1; then
    nc -z -G 2 "$host" "$port" 2>/dev/null || nc -z -w 2 "$host" "$port" 2>/dev/null
  else
    (echo > "/dev/tcp/$host/$port") >/dev/null 2>&1
  fi
}

# Qdrant Vector DB Helper
is_qdrant_listening() {
  local host="${1:-localhost}"
  local port="${2:-6333}"
  if command -v nc >/dev/null 2>&1; then
    nc -z -G 2 "$host" "$port" 2>/dev/null || nc -z -w 2 "$host" "$port" 2>/dev/null
  else
    (echo > "/dev/tcp/$host/$port") >/dev/null 2>&1
  fi
}

# Resolve Neo4j dual-database configuration
if [[ "$FORCE_ORACLE_ONLY" == "true" || "${PORTAL_NEO4J_ENABLED:-}" == "false" ]]; then
  export PORTAL_NEO4J_ENABLED="false"
  export NEO4J_URI="${NEO4J_URI:-}"
  export NEO4J_USERNAME="${NEO4J_USERNAME:-}"
  export NEO4J_PASSWORD="${NEO4J_PASSWORD:-}"
elif [[ "${PORTAL_NEO4J_ENABLED:-}" == "true" ]]; then
  # Neo4j explicitly enabled
  export PORTAL_NEO4J_ENABLED="true"
  export NEO4J_URI="${NEO4J_URI:-}"
  export NEO4J_USERNAME="${NEO4J_USERNAME:-neo4j}"
  export NEO4J_PASSWORD="${NEO4J_PASSWORD:-}"
  if [[ -z "$NEO4J_URI" ]]; then
    echo "[!] WARNING: PORTAL_NEO4J_ENABLED=true but NEO4J_URI is not set."
  fi
  if [[ -z "$NEO4J_PASSWORD" ]]; then
    echo "[!] WARNING: PORTAL_NEO4J_ENABLED=true but NEO4J_PASSWORD is not set."
  fi
elif [[ -n "${NEO4J_URI:-}" && -n "${NEO4J_PASSWORD:-}" ]]; then
  # Explicitly configured credentials (e.g. Aura Cloud or custom bolt URI)
  export PORTAL_NEO4J_ENABLED="true"
  export NEO4J_USERNAME="${NEO4J_USERNAME:-neo4j}"
elif is_neo4j_listening "localhost" "7687" || (command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q 'portal-neo4j'); then
  # Local Neo4j container or listener detected on port 7687
  export PORTAL_NEO4J_ENABLED="true"
  export NEO4J_URI="${NEO4J_URI:-bolt://localhost:7687}"
  export NEO4J_USERNAME="${NEO4J_USERNAME:-neo4j}"
  export NEO4J_PASSWORD="${NEO4J_PASSWORD:-placement2026}"
else
  # Default to disabled if neither cloud credentials nor local container detected
  export PORTAL_NEO4J_ENABLED="false"
fi

# Resolve Qdrant Vector Database configuration
if [[ "${PORTAL_QDRANT_ENABLED:-}" == "false" ]]; then
  export PORTAL_QDRANT_ENABLED="false"
  export QDRANT_URL="${QDRANT_URL:-}"
  export QDRANT_API_KEY="${QDRANT_API_KEY:-}"
  export QDRANT_COLLECTION="${QDRANT_COLLECTION:-placement-semantic-search}"
elif [[ "${PORTAL_QDRANT_ENABLED:-}" == "true" ]]; then
  export PORTAL_QDRANT_ENABLED="true"
  export QDRANT_URL="${QDRANT_URL:-http://localhost:6333}"
  export QDRANT_API_KEY="${QDRANT_API_KEY:-}"
  export QDRANT_COLLECTION="${QDRANT_COLLECTION:-placement-semantic-search}"
elif [[ -n "${QDRANT_URL:-}" ]]; then
  export PORTAL_QDRANT_ENABLED="true"
  export QDRANT_COLLECTION="${QDRANT_COLLECTION:-placement-semantic-search}"
elif is_qdrant_listening "localhost" "6333" || (command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' 2>/dev/null | grep -q 'portal-qdrant'); then
  export PORTAL_QDRANT_ENABLED="true"
  export QDRANT_URL="${QDRANT_URL:-http://localhost:6333}"
  export QDRANT_API_KEY="${QDRANT_API_KEY:-}"
  export QDRANT_COLLECTION="${QDRANT_COLLECTION:-placement-semantic-search}"
else
  export PORTAL_QDRANT_ENABLED="false"
fi

# Service Ports
export SERVER_PORT="${SERVER_PORT:-8080}"
export FRONTEND_PORT="${FRONTEND_PORT:-3001}"
export CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:${FRONTEND_PORT},http://127.0.0.1:${FRONTEND_PORT},http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173}"

# Evaluate Neo4j Configuration
AURA_STATUS_DESC="Disabled"
if [[ "$PORTAL_NEO4J_ENABLED" == "true" ]]; then
  SAFE_AURA_URI=$(echo "$NEO4J_URI" | sed -E 's|://.*@|://***@|')
  AURA_STATUS_DESC="Active ($SAFE_AURA_URI)"
  echo "[✓] Neo4j dual-database graph projection is ENABLED: $SAFE_AURA_URI (User: $NEO4J_USERNAME)"
else
  echo "[i] Neo4j graph projection: DISABLED (Oracle Database 23ai is primary)."
  echo "    To connect, run with local Neo4j or set credentials in .env."
fi

# Evaluate Qdrant Configuration
QDRANT_STATUS_DESC="Disabled"
if [[ "$PORTAL_QDRANT_ENABLED" == "true" ]]; then
  QDRANT_STATUS_DESC="Active ($QDRANT_URL)"
  echo "[✓] Qdrant vector database is ENABLED: $QDRANT_URL (Collection: $QDRANT_COLLECTION)"
else
  echo "[i] Qdrant vector search: DISABLED (Oracle Database 23ai is primary)."
  echo "    To connect, run with local Qdrant container or set credentials in .env."
fi

# ------------------------------------------------------------------------------
# Step 4: Ensure Authoritative Oracle Database is Ready
# ------------------------------------------------------------------------------
echo ""
echo "[2/5] Inspecting Oracle Database 23ai availability..."

is_oracle_listening() {
  if command -v nc >/dev/null 2>&1; then
    nc -z -w 2 "$ORACLE_HOST" "$ORACLE_PORT" >/dev/null 2>&1
  else
    (echo > "/dev/tcp/$ORACLE_HOST/$ORACLE_PORT") >/dev/null 2>&1
  fi
}

is_oracle_healthy() {
  if command -v docker >/dev/null 2>&1; then
    local status
    status=$(docker inspect --format='{{.State.Health.Status}}' portal-oracle-db 2>/dev/null || true)
    [[ "$status" == "healthy" ]]
  else
    false
  fi
}

is_pid_matching_project() {
  local pid="$1"
  [[ -z "$pid" ]] && return 1

  # Check working directory of process
  local proc_cwd
  proc_cwd=$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | grep "^n" | cut -c2- || true)
  if [[ "$proc_cwd" == "$DIR"* ]]; then
    return 0
  fi

  # Check command arguments of process
  local proc_args
  proc_args=$(ps -wwp "$pid" -o args= 2>/dev/null || true)
  if [[ "$proc_args" == *"$DIR"* ]]; then
    return 0
  fi

  return 1
}

inspect_port_owner() {
  local port="$1"
  local svc_name="$2"
  local pid=""

  if command -v lsof >/dev/null 2>&1; then
    pid=$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null | head -n 1 || true)
  fi

  if [[ -z "$pid" ]]; then
    echo "FREE"
    return 0
  fi

  if is_pid_matching_project "$pid"; then
    echo "OWNED:$pid"
  else
    local proc_comm
    proc_comm=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
    echo "CONFLICT:$pid:$proc_comm"
  fi
}

# Handle --status inspection without starting any services
if [[ "$SHOW_STATUS_ONLY" == "true" ]]; then
  echo ""
  echo "================================================================="
  echo "   PORTAL SERVICE & ENVIRONMENT STATUS CHECK"
  echo "================================================================="
  if is_oracle_listening; then
    echo "   • Oracle Database 23ai: LISTENING ($ORACLE_HOST:$ORACLE_PORT/$ORACLE_SERVICE)"
  else
    echo "   • Oracle Database 23ai: NOT RUNNING (run ./run_portal.sh to launch container)"
  fi
  if [[ "$PORTAL_NEO4J_ENABLED" == "true" ]]; then
    if [[ "$NEO4J_URI" == *"localhost"* || "$NEO4J_URI" == *"127.0.0.1"* ]]; then
      if is_neo4j_listening localhost 7687; then
        echo "   • Neo4j Graph Database: LISTENING (Local bolt://localhost:7687)"
      else
        echo "   • Neo4j Graph Database: NOT RUNNING (Local port 7687 closed)"
      fi
    else
      echo "   • Neo4j Graph Database: CONFIGURED (Cloud Aura $SAFE_AURA_URI)"
    fi
  else
    echo "   • Neo4j Graph Database: DISABLED (Run without --oracle-only to enable)"
  fi
  if [[ "$PORTAL_QDRANT_ENABLED" == "true" ]]; then
    if is_qdrant_listening localhost 6333; then
      echo "   • Qdrant Vector DB:   LISTENING (Local $QDRANT_URL, Collection: $QDRANT_COLLECTION)"
    else
      echo "   • Qdrant Vector DB:   CONFIGURED ($QDRANT_URL, Collection: $QDRANT_COLLECTION)"
    fi
  else
    echo "   • Qdrant Vector DB:   DISABLED (Set PORTAL_QDRANT_ENABLED=true to enable)"
  fi

  echo "   • Backend Service (Port $SERVER_PORT):"
  B_STATE=$(inspect_port_owner "$SERVER_PORT" "Backend")
  if [[ "$B_STATE" == OWNED:* ]]; then
    BPID=$(echo "$B_STATE" | cut -d: -f2)
    B_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$SERVER_PORT/actuator/health" 2>/dev/null || true)
    if [[ "$B_HEALTH" == "200" ]]; then
      echo "     └─ RUNNING (PID $BPID, healthy at http://localhost:$SERVER_PORT/actuator/health)"
    else
      echo "     └─ RUNNING (PID $BPID, unready/starting)"
    fi
  elif [[ "$B_STATE" == CONFLICT:* ]]; then
    CPID=$(echo "$B_STATE" | cut -d: -f2)
    CCOMM=$(echo "$B_STATE" | cut -d: -f3)
    echo "     └─ OCCUPIED by external process (PID $CPID, $CCOMM)"
  else
    echo "     └─ STOPPED (port $SERVER_PORT is free)"
  fi

  echo "   • Frontend Service (Port $FRONTEND_PORT):"
  F_STATE=$(inspect_port_owner "$FRONTEND_PORT" "Frontend")
  if [[ "$F_STATE" == OWNED:* ]]; then
    FPID=$(echo "$F_STATE" | cut -d: -f2)
    F_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$FRONTEND_PORT" 2>/dev/null || true)
    if [[ "$F_HEALTH" == "200" || "$F_HEALTH" == "304" ]]; then
      echo "     └─ RUNNING (PID $FPID, responsive at http://localhost:$FRONTEND_PORT)"
    else
      echo "     └─ RUNNING (PID $FPID, unready)"
    fi
  elif [[ "$F_STATE" == CONFLICT:* ]]; then
    CPID=$(echo "$F_STATE" | cut -d: -f2)
    CCOMM=$(echo "$F_STATE" | cut -d: -f3)
    echo "     └─ OCCUPIED by external process (PID $CPID, $CCOMM)"
  else
    echo "     └─ STOPPED (port $FRONTEND_PORT is free)"
  fi
  echo "================================================================="
  exit 0
fi

# Handle --stop cleanly without starting any services
if [[ "$STOP_SERVICES" == "true" ]]; then
  echo ""
  echo "================================================================="
  echo "   Stopping Student Placement & Recruitment Portal Services"
  echo "================================================================="

  STOPPED_ANY=false

  # Check backend port
  BACKEND_STATE=$(inspect_port_owner "$SERVER_PORT" "Backend")
  if [[ "$BACKEND_STATE" == OWNED:* ]]; then
    BPID=$(echo "$BACKEND_STATE" | cut -d: -f2)
    echo "[*] Stopping backend process (PID $BPID)..."
    pkill -P "$BPID" 2>/dev/null || true
    kill "$BPID" 2>/dev/null || true
    STOPPED_ANY=true
  elif [[ "$BACKEND_STATE" == CONFLICT:* ]]; then
    CPID=$(echo "$BACKEND_STATE" | cut -d: -f2)
    echo "[!] Port $SERVER_PORT is occupied by an external/unrelated process (PID $CPID). Refusing to kill."
  else
    echo "[i] Backend is not running on port $SERVER_PORT."
  fi

  # Check frontend port
  FRONTEND_STATE=$(inspect_port_owner "$FRONTEND_PORT" "Frontend")
  if [[ "$FRONTEND_STATE" == OWNED:* ]]; then
    FPID=$(echo "$FRONTEND_STATE" | cut -d: -f2)
    echo "[*] Stopping frontend process (PID $FPID)..."
    pkill -P "$FPID" 2>/dev/null || true
    kill "$FPID" 2>/dev/null || true
    STOPPED_ANY=true
  elif [[ "$FRONTEND_STATE" == CONFLICT:* ]]; then
    CPID=$(echo "$FRONTEND_STATE" | cut -d: -f2)
    echo "[!] Port $FRONTEND_PORT is occupied by an external/unrelated process (PID $CPID). Refusing to kill."
  else
    echo "[i] Frontend is not running on port $FRONTEND_PORT."
  fi

  if [[ "$STOPPED_ANY" == "true" ]]; then
    echo "[✓] Portal services stopped cleanly."
  else
    echo "[i] No active portal services were running."
  fi
  echo "[i] Shared Oracle Database container remains intact."
  echo "    To stop Oracle container manually: docker compose -f infrastructure/docker-compose.yml stop oracle-db"
  exit 0
fi

if is_oracle_listening && (is_oracle_healthy || [[ "$ORACLE_HOST" != "localhost" && "$ORACLE_HOST" != "127.0.0.1" ]]); then
  echo "[✓] Oracle Database listener detected active on $ORACLE_HOST:$ORACLE_PORT (Service: $ORACLE_SERVICE)."
else
  # Oracle is not ready on port; attempt to start container using docker-compose.yml
  echo "[*] Oracle Database is not responding on $ORACLE_HOST:$ORACLE_PORT."
  
  if ! command -v docker >/dev/null 2>&1; then
    echo "[!] ERROR: Docker is required to start the local Oracle Database 23ai container."
    echo "    Please install Docker Desktop or start Oracle Database manually on port $ORACLE_PORT."
    exit 1
  fi

  # Check Docker daemon; start Docker Desktop on macOS if installed
  if ! docker info >/dev/null 2>&1; then
    echo "[*] Docker daemon is not running. Attempting to launch Docker Desktop..."
    if [[ -d "/Applications/Docker.app" ]]; then
      open -a Docker
      echo -n "[*] Waiting for Docker daemon to initialize"
      for _ in {1..35}; do
        if docker info >/dev/null 2>&1; then
          echo " ready!"
          break
        fi
        echo -n "."
        sleep 1
      done
      echo ""
    fi
  fi

  if ! docker info >/dev/null 2>&1; then
    echo "[!] ERROR: Docker daemon is not running."
    echo "    Please start Docker Desktop and rerun ./run_portal.sh."
    exit 1
  fi

  COMPOSE_SERVICES="oracle-db redis"
  if [[ "$PORTAL_NEO4J_ENABLED" == "true" && ("$NEO4J_URI" == *"localhost"* || "$NEO4J_URI" == *"127.0.0.1"*) ]]; then
    COMPOSE_SERVICES="oracle-db redis neo4j"
  fi

  echo "[*] Starting containers ($COMPOSE_SERVICES) via infrastructure/docker-compose.yml..."
  docker compose -f "$DIR/infrastructure/docker-compose.yml" up -d $COMPOSE_SERVICES

  echo -n "[*] Waiting for Oracle Database 23ai to become healthy"
  ORACLE_READY=false
  for _ in {1..90}; do
    if is_oracle_healthy || (is_oracle_listening && [[ $_ -gt 15 ]]); then
      ORACLE_READY=true
      echo " ready!"
      break
    fi
    echo -n "."
    sleep 1
  done
  echo ""

  if [[ "$ORACLE_READY" != "true" ]]; then
    echo "[!] ERROR: Oracle Database container failed to become ready within 90 seconds."
    echo "    Inspect container status and logs with:"
    echo "      docker compose -f infrastructure/docker-compose.yml ps"
    echo "      docker logs portal-oracle-db"
    echo "[!] Refusing to silently fall back to H2 or an offline state."
    exit 1
  fi
  echo "[✓] Oracle Database 23ai is fully operational on $ORACLE_HOST:$ORACLE_PORT."
fi

# Verify or start Neo4j Database
if [[ "$PORTAL_NEO4J_ENABLED" == "true" ]]; then
  if [[ "$NEO4J_URI" == *"localhost"* || "$NEO4J_URI" == *"127.0.0.1"* ]]; then
    if is_neo4j_listening localhost 7687; then
      echo "[✓] Neo4j Graph Database listener detected active on bolt://localhost:7687."
    elif command -v docker >/dev/null 2>&1; then
      echo "[*] Local Neo4j is not listening on port 7687. Starting container via docker compose..."
      docker compose -f "$DIR/infrastructure/docker-compose.yml" up -d neo4j
      echo -n "[*] Waiting for Neo4j Database listener on port 7687"
      for _ in {1..25}; do
        if is_neo4j_listening localhost 7687; then
          echo " ready!"
          break
        fi
        echo -n "."
        sleep 1
      done
      echo ""
      if is_neo4j_listening localhost 7687; then
        echo "[✓] Neo4j Graph Database is fully operational on bolt://localhost:7687."
      else
        echo "[!] WARNING: Local Neo4j container started but port 7687 not yet accepting connections."
      fi
    else
      echo "[!] WARNING: Local Neo4j is configured on port 7687 but neither port nor Docker is reachable."
    fi
  else
    echo "[i] Neo4j Aura cloud endpoint configured: $SAFE_AURA_URI."
  fi
fi

# ------------------------------------------------------------------------------
# Step 5: Process Ownership & Port Availability Inspection
# ------------------------------------------------------------------------------
echo ""
echo "[3/5] Inspecting portal service processes and ports..."

BACKEND_STATE=$(inspect_port_owner "$SERVER_PORT" "Backend")
FRONTEND_STATE=$(inspect_port_owner "$FRONTEND_PORT" "Frontend")

BACKEND_PID=""
FRONTEND_PID=""
BACKEND_ALREADY_RUNNING=false
FRONTEND_ALREADY_RUNNING=false
BACKEND_STARTED_BY_SCRIPT=false
FRONTEND_STARTED_BY_SCRIPT=false

if [[ "$BACKEND_STATE" == CONFLICT:* ]]; then
  CONFLICT_PID=$(echo "$BACKEND_STATE" | cut -d: -f2)
  CONFLICT_COMM=$(echo "$BACKEND_STATE" | cut -d: -f3)
  echo "[!] ERROR: Port $SERVER_PORT is already occupied by an external/unrelated process (PID $CONFLICT_PID: $CONFLICT_COMM)."
  echo "    The portal will not terminate external or unrelated processes."
  echo "    Please stop the conflicting process or change SERVER_PORT in .env."
  exit 1
elif [[ "$BACKEND_STATE" == OWNED:* ]]; then
  BACKEND_PID=$(echo "$BACKEND_STATE" | cut -d: -f2)
  BACKEND_ALREADY_RUNNING=true
  echo "[✓] Port $SERVER_PORT is already running this project's backend (PID $BACKEND_PID)."
else
  echo "[✓] Port $SERVER_PORT (backend) is available."
fi

if [[ "$FRONTEND_STATE" == CONFLICT:* ]]; then
  CONFLICT_PID=$(echo "$FRONTEND_STATE" | cut -d: -f2)
  CONFLICT_COMM=$(echo "$FRONTEND_STATE" | cut -d: -f3)
  echo "[!] ERROR: Port $FRONTEND_PORT is already occupied by an external/unrelated process (PID $CONFLICT_PID: $CONFLICT_COMM)."
  echo "    The portal will not terminate external or unrelated processes."
  echo "    Please stop the conflicting process or change FRONTEND_PORT in .env."
  exit 1
elif [[ "$FRONTEND_STATE" == OWNED:* ]]; then
  FRONTEND_PID=$(echo "$FRONTEND_STATE" | cut -d: -f2)
  FRONTEND_ALREADY_RUNNING=true
  echo "[✓] Port $FRONTEND_PORT is already running this project's frontend (PID $FRONTEND_PID)."
else
  echo "[✓] Port $FRONTEND_PORT (frontend) is available."
fi

# ------------------------------------------------------------------------------
# Step 6: Process Supervision & Safe Cleanup Trap
# ------------------------------------------------------------------------------
mkdir -p "$DIR/logs"
BACKEND_LOG="$DIR/logs/backend.log"
FRONTEND_LOG="$DIR/logs/frontend.log"

cleanup() {
  trap - INT TERM EXIT
  echo ""
  echo "[*] Shutting down portal services..."

  if [[ "$FRONTEND_STARTED_BY_SCRIPT" == "true" && -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    echo "    Stopping frontend process (PID $FRONTEND_PID)..."
    pkill -P "$FRONTEND_PID" 2>/dev/null || true
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi

  if [[ "$BACKEND_STARTED_BY_SCRIPT" == "true" && -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "    Stopping backend process (PID $BACKEND_PID)..."
    pkill -P "$BACKEND_PID" 2>/dev/null || true
    kill "$BACKEND_PID" 2>/dev/null || true
  fi

  echo "[✓] Supervised portal processes stopped cleanly."
  echo "[i] Shared Oracle Database container remains running."
  echo "    To stop Oracle manually: docker compose -f infrastructure/docker-compose.yml stop oracle-db"
  exit 0
}

if [[ "$DAEMON_MODE" == "true" ]]; then
  trap cleanup INT TERM
else
  trap cleanup INT TERM EXIT
fi

# ------------------------------------------------------------------------------
# Step 7: Launch or Verify Backend (Spring Boot with Oracle Profile)
# ------------------------------------------------------------------------------
echo ""
if [[ "$BACKEND_ALREADY_RUNNING" == "true" ]]; then
  echo "[4/5] Verifying running Spring Boot backend (PID $BACKEND_PID, Port: $SERVER_PORT)..."
  echo -n "[*] Checking health at http://localhost:$SERVER_PORT/actuator/health"
  BACKEND_HEALTHY=false
  for _ in {1..15}; do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$SERVER_PORT/actuator/health" 2>/dev/null || true)
    if [[ "$HTTP_STATUS" == "200" ]]; then
      BACKEND_HEALTHY=true
      echo " ready!"
      break
    fi

    SYS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$SERVER_PORT/api/v1/system/backend-info" 2>/dev/null || true)
    if [[ "$SYS_STATUS" == "200" ]]; then
      BACKEND_HEALTHY=true
      echo " ready (primary OLTP active)!"
      break
    fi

    echo -n "."
    sleep 1
  done
  echo ""

  if [[ "$BACKEND_HEALTHY" != "true" ]]; then
    echo "[!] ERROR: Existing backend (PID $BACKEND_PID) is not responding healthy."
    echo "    To restart the backend cleanly, run: ./run_portal.sh --stop && ./run_portal.sh"
    exit 1
  fi
  echo "[✓] Backend is healthy and accepting connections on port $SERVER_PORT."
else
  echo "[4/5] Launching Spring Boot backend (Profile: oracle, Port: $SERVER_PORT)..."
  echo "      Log file: $BACKEND_LOG"

  # SEC-02: Non-sensitive JVM memory and runtime options (secrets supplied via environment variables)
  NON_SENSITIVE_JVM_OPTS="${JAVA_OPTS:--Xms512m -Xmx2048m}"

  (
    cd "$DIR/backend"
    # Supply secrets and runtime configuration strictly via environment variables
    export SPRING_PROFILES_ACTIVE="oracle"
    export SPRING_DATASOURCE_URL="jdbc:oracle:thin:@$ORACLE_HOST:$ORACLE_PORT/$ORACLE_SERVICE"
    export SPRING_DATASOURCE_USERNAME="$ORACLE_USER"
    export SPRING_DATASOURCE_PASSWORD="$ORACLE_PASSWORD"
    export JWT_SECRET="$JWT_SECRET"
    export PORTAL_NEO4J_ENABLED="$PORTAL_NEO4J_ENABLED"
    export NEO4J_URI="$NEO4J_URI"
    export NEO4J_USERNAME="$NEO4J_USERNAME"
    export NEO4J_PASSWORD="$NEO4J_PASSWORD"
    export PORTAL_QDRANT_ENABLED="$PORTAL_QDRANT_ENABLED"
    export QDRANT_URL="$QDRANT_URL"
    export QDRANT_API_KEY="$QDRANT_API_KEY"
    export QDRANT_COLLECTION="$QDRANT_COLLECTION"
    export SERVER_PORT="$SERVER_PORT"
    export CORS_ALLOWED_ORIGINS="$CORS_ALLOWED_ORIGINS"
    export REDIS_HOST="${REDIS_HOST:-localhost}"
    export REDIS_PORT="${REDIS_PORT:-6379}"
    export REDIS_PASSWORD="${REDIS_PASSWORD:-}"

    exec mvn spring-boot:run \
      -Dspring-boot.run.profiles=oracle \
      -Dspring-boot.run.jvmArguments="$NON_SENSITIVE_JVM_OPTS"
  ) > "$BACKEND_LOG" 2>&1 &
  BACKEND_PID=$!
  BACKEND_STARTED_BY_SCRIPT=true

  echo -n "[*] Waiting for backend to become healthy at http://localhost:$SERVER_PORT/actuator/health"
  BACKEND_HEALTHY=false
  for _ in {1..900}; do
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
      echo " FAILED!"
      echo "[!] ERROR: Backend process exited prematurely."
      echo "    Last 25 lines of $BACKEND_LOG:"
      echo "-----------------------------------------------------------------"
      tail -n 25 "$BACKEND_LOG" 2>/dev/null || true
      echo "-----------------------------------------------------------------"
      exit 1
    fi

    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$SERVER_PORT/actuator/health" 2>/dev/null || true)
    if [[ "$HTTP_STATUS" == "200" ]]; then
      BACKEND_HEALTHY=true
      echo " ready!"
      break
    fi

    SYS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$SERVER_PORT/api/v1/system/backend-info" 2>/dev/null || true)
    if [[ "$SYS_STATUS" == "200" ]]; then
      BACKEND_HEALTHY=true
      echo " ready (primary OLTP active)!"
      break
    fi

    echo -n "."
    sleep 1
  done
  echo ""

  if [[ "$BACKEND_HEALTHY" != "true" ]]; then
    echo "[!] ERROR: Backend failed to respond healthy within 900 seconds."
    echo "    Last 25 lines of $BACKEND_LOG:"
    echo "-----------------------------------------------------------------"
    tail -n 25 "$BACKEND_LOG" 2>/dev/null || true
    echo "-----------------------------------------------------------------"
    exit 1
  fi
  echo "[✓] Backend is healthy and accepting connections on port $SERVER_PORT."
fi

# ------------------------------------------------------------------------------
# Step 8: Launch or Verify Frontend (React 19 / Vite)
# ------------------------------------------------------------------------------
echo ""
if [[ "$FRONTEND_ALREADY_RUNNING" == "true" ]]; then
  echo "[5/5] Verifying running React Vite frontend (PID $FRONTEND_PID, Port: $FRONTEND_PORT)..."
  echo -n "[*] Checking frontend at http://localhost:$FRONTEND_PORT"
  FRONTEND_READY=false
  for _ in {1..10}; do
    FRONT_HTTP=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$FRONTEND_PORT" 2>/dev/null || true)
    if [[ "$FRONT_HTTP" == "200" || "$FRONT_HTTP" == "304" ]]; then
      FRONTEND_READY=true
      echo " ready!"
      break
    fi
    echo -n "."
    sleep 1
  done
  echo ""

  if [[ "$FRONTEND_READY" != "true" ]]; then
    echo "[!] ERROR: Existing frontend (PID $FRONTEND_PID) is not responding."
    echo "    To restart the frontend cleanly, run: ./run_portal.sh --stop && ./run_portal.sh"
    exit 1
  fi
  echo "[✓] Frontend is ready on http://localhost:$FRONTEND_PORT."
else
  echo "[5/5] Launching React Vite frontend on port $FRONTEND_PORT..."
  echo "      Log file: $FRONTEND_LOG"

  (
    cd "$DIR/frontend"
    exec npm run dev
  ) > "$FRONTEND_LOG" 2>&1 &
  FRONTEND_PID=$!
  FRONTEND_STARTED_BY_SCRIPT=true

  echo -n "[*] Waiting for frontend development server at http://localhost:$FRONTEND_PORT"
  FRONTEND_READY=false
  for _ in {1..30}; do
    if ! kill -0 "$FRONTEND_PID" 2>/dev/null; then
      echo " FAILED!"
      echo "[!] ERROR: Frontend process exited prematurely."
      echo "    Last 20 lines of $FRONTEND_LOG:"
      tail -n 20 "$FRONTEND_LOG" 2>/dev/null || true
      exit 1
    fi

    FRONT_HTTP=$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$FRONTEND_PORT" 2>/dev/null || true)
    if [[ "$FRONT_HTTP" == "200" || "$FRONT_HTTP" == "304" ]]; then
      FRONTEND_READY=true
      echo " ready!"
      break
    fi
    echo -n "."
    sleep 1
  done
  echo ""

  if [[ "$FRONTEND_READY" != "true" ]]; then
    echo "[!] ERROR: Frontend failed to start within 30 seconds."
    echo "    Last 20 lines of $FRONTEND_LOG:"
    tail -n 20 "$FRONTEND_LOG" 2>/dev/null || true
    exit 1
  fi
  echo "[✓] Frontend is ready on http://localhost:$FRONTEND_PORT."
fi

# ------------------------------------------------------------------------------
# Step 9: Ready Display Banner
# ------------------------------------------------------------------------------
echo ""
echo "================================================================="
echo "   PORTAL READY FOR DEMONSTRATION & EVALUATION"
echo "================================================================="
echo "   • Frontend Application:   http://localhost:$FRONTEND_PORT"
echo "   • Student Portal:         http://localhost:$FRONTEND_PORT/student"
echo "   • Campus Placement Drives:http://localhost:$FRONTEND_PORT/student/drives"
echo "   • Recruiter Portal:       http://localhost:$FRONTEND_PORT/recruiter"
echo "   • SQL & Cypher Workspace: http://localhost:$FRONTEND_PORT/admin/sql-compiler"
echo "   • Graph Explorer (Neo4j): http://localhost:$FRONTEND_PORT/graph"
echo "   • Semantic Vector Search: http://localhost:$FRONTEND_PORT/semantic-search"
echo "   • Backend REST API:       http://localhost:$SERVER_PORT/api/v1"
echo "   • Swagger UI API Docs:    http://localhost:$SERVER_PORT/swagger-ui.html"
echo "   • Spring Actuator Health: http://localhost:$SERVER_PORT/actuator/health"
echo "================================================================="
echo "   Active Databases:"
echo "   • Primary OLTP:    Oracle Database 23ai ($ORACLE_HOST:$ORACLE_PORT/$ORACLE_SERVICE)"
echo "   • Graph Engine:    Neo4j (${AURA_STATUS_DESC})"
echo "   • Vector Engine:   Qdrant (${QDRANT_STATUS_DESC})"
echo "================================================================="
echo "   Demo Personas:"
echo "   • Student:   25bce1799  / password123 (STU001, Achyut Ranaut)"
echo "   • Recruiter: recruiter1 / password123 (Microsoft India)"
echo "   • Admin:     admin      / admin123"
echo "================================================================="
echo "   [i] Press Ctrl+C at any time to cleanly stop portal services."
echo ""

# Supervise or wait for processes
if [[ "$DAEMON_MODE" == "true" ]]; then
  echo "   [✓] Portal services are running in background (detached)."
  echo "   [✓] To inspect status:  ./run_portal.sh --status"
  echo "   [✓] To stop services:   ./run_portal.sh --stop"
  if [[ "$BACKEND_STARTED_BY_SCRIPT" == "true" && -n "$BACKEND_PID" ]]; then
    disown "$BACKEND_PID" 2>/dev/null || true
  fi
  if [[ "$FRONTEND_STARTED_BY_SCRIPT" == "true" && -n "$FRONTEND_PID" ]]; then
    disown "$FRONTEND_PID" 2>/dev/null || true
  fi
  exit 0
fi

if [[ "$BACKEND_STARTED_BY_SCRIPT" == "true" && "$FRONTEND_STARTED_BY_SCRIPT" == "true" ]]; then
  wait "$BACKEND_PID" "$FRONTEND_PID"
elif [[ "$BACKEND_STARTED_BY_SCRIPT" == "true" ]]; then
  wait "$BACKEND_PID"
elif [[ "$FRONTEND_STARTED_BY_SCRIPT" == "true" ]]; then
  wait "$FRONTEND_PID"
else
  echo "[i] Both services are actively running. Monitoring processes [Press Ctrl+C to stop]..."
  while kill -0 "$BACKEND_PID" 2>/dev/null && kill -0 "$FRONTEND_PID" 2>/dev/null; do
    sleep 2
  done
fi
