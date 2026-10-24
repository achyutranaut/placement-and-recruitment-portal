#!/usr/bin/env bash
# ==============================================================================
# Master Repository Launcher
# Student Placement & Recruitment Portal
# ==============================================================================
# Usage:
#   ./run_portal.sh              # Launches Project 2 (Official DA3 Polyglot Graph Portal)
#   ./run_portal.sh --status     # Inspects services status
#   ./run_portal.sh --stop       # Stops running services
#   ./run_portal.sh --project 1  # Launches Project 1 (Baseline DA2 Relational Portal)
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check if user explicitly requested Project 1
TARGET_PROJECT="Project-2-DA3"
PASSED_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project|-p)
      if [[ "${2:-}" == "1" || "${2:-}" == "existing" || "${2:-}" == "Project-1-Existing" ]]; then
        TARGET_PROJECT="Project-1-Existing"
      else
        TARGET_PROJECT="Project-2-DA3"
      fi
      shift 2
      ;;
    *)
      PASSED_ARGS+=("$1")
      shift
      ;;
  esac
done

TARGET_SCRIPT="$SCRIPT_DIR/$TARGET_PROJECT/run_portal.sh"

if [[ ! -f "$TARGET_SCRIPT" ]]; then
  echo "[!] ERROR: Launcher script not found at $TARGET_SCRIPT"
  exit 1
fi

chmod +x "$TARGET_SCRIPT" 2>/dev/null || true

exec "$TARGET_SCRIPT" ${PASSED_ARGS[@]+"${PASSED_ARGS[@]}"}
