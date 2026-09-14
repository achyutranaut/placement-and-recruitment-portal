#!/bin/bash
set -e

export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "================================================================="
echo "   VIT Placement and Training Cell — Recruitment Portal"
echo "   Academic Year 2025–2026 Campus Placement Drive Infrastructure"
echo "================================================================="

# Ensure Oracle Database and Redis containers are running if docker is installed
DB_PROFILE="oracle"
if command -v docker >/dev/null 2>&1; then
    # Check if docker daemon is running; if not, attempt to start Docker Desktop on macOS
    if ! docker info >/dev/null 2>&1; then
        echo "[*] Docker daemon is not running. Attempting to start Docker Desktop..."
        if [ -d "/Applications/Docker.app" ]; then
            open -a Docker
            echo -n "[*] Waiting for Docker Desktop to initialize"
            for i in {1..30}; do
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

    if docker info >/dev/null 2>&1; then
        if ! nc -z localhost 1521 2>/dev/null; then
            echo "[*] Starting Oracle Database container..."
            docker start oracle-free 2>/dev/null || docker start portal-oracle-db 2>/dev/null || true
            # Wait up to 25s for Oracle listener
            echo -n "[*] Waiting for Oracle Database listener on port 1521"
            for i in {1..25}; do
                if nc -z localhost 1521 2>/dev/null; then
                    echo " ready!"
                    echo "[✓] Oracle Database listener ready on port 1521."
                    break
                fi
                echo -n "."
                sleep 1
            done
            echo ""
        else
            echo "[✓] Oracle Database detected active on port 1521."
        fi

        if ! nc -z localhost 6379 2>/dev/null; then
            docker start energy-trading-platform-redis-1 2>/dev/null || docker start portal-redis 2>/dev/null || true
        fi
    fi
fi

if nc -z localhost 1521 2>/dev/null; then
    DB_PROFILE="oracle"
    echo "[✓] Connecting to authoritative Oracle Database 23ai (port 1521)."
else
    echo "[!] Oracle Database not detected on port 1521."
    echo "[*] Falling back to offline H2 profile with Oracle compatibility mode."
    DB_PROFILE="h2"
fi

# Clean up any lingering processes on ports 8080 or 3000 from previous runs
if lsof -ti :8080 >/dev/null 2>&1; then
    echo "[*] Freeing port 8080 from previous backend instance..."
    lsof -ti :8080 | xargs kill -9 2>/dev/null || true
    sleep 1
fi
if lsof -ti :3000 >/dev/null 2>&1; then
    echo "[*] Freeing port 3000 from previous frontend instance..."
    lsof -ti :3000 | xargs kill -9 2>/dev/null || true
    sleep 1
fi

cleanup() {
    echo ""
    echo "[*] Shutting down portal services..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    lsof -ti :8080 | xargs kill -9 2>/dev/null || true
    lsof -ti :3000 | xargs kill -9 2>/dev/null || true
    exit 0
}
trap cleanup EXIT INT TERM

echo ""
echo "[1/2] Launching Spring Boot Backend (:8080) [Profile: $DB_PROFILE]..."
(cd "$DIR/backend" && mvn spring-boot:run -Dspring-boot.run.profiles=$DB_PROFILE) &
BACKEND_PID=$!

echo "[2/2] Launching React Vite Frontend (:3000)..."
(cd "$DIR/frontend" && npm run dev) &
FRONTEND_PID=$!

echo ""
echo "================================================================="
echo "   PORTAL READY FOR EVALUATION & DEMONSTRATION"
echo "   - Landing Page:           http://localhost:3000"
echo "   - Student Portal:         http://localhost:3000/student"
echo "   - Resume Vault (BLOB):    http://localhost:3000/student/resume"
echo "   - Campus Drives:          http://localhost:3000/student/drives"
echo "   - Applications Tracker:   http://localhost:3000/student/applications"
echo "   - Recruiter Portal (ATS): http://localhost:3000/recruiter"
echo "   - Recruiter Registration: http://localhost:3000/recruiter/register"
echo "   - Placement Admin/DB:     http://localhost:3000/admin"
echo "   - Swagger UI API Docs:    http://localhost:8080/swagger-ui.html"
echo "================================================================="
echo "   Demo Personas:"
echo "   • Student:   25bce1799  / password123 (STU023, Achyut Ranaut)"
echo "   • Recruiter: recruiter1 / password123 (Microsoft India)"
echo "   • Admin:     admin      / admin123"
echo "================================================================="

wait
