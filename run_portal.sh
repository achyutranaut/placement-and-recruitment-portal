#!/bin/bash
set -e

export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "================================================================="
echo "   VIT Placement and Training Cell — Recruitment Portal"
echo "   Academic Year 2025–2026 Campus Placement Drive Infrastructure"
echo "================================================================="

# Ensure Oracle Database and Redis containers are running if docker is installed
if command -v docker >/dev/null 2>&1; then
    if ! nc -z localhost 1521 2>/dev/null; then
        echo "[*] Starting Oracle Database container..."
        docker start oracle-free 2>/dev/null || docker start portal-oracle-db 2>/dev/null || true
        # Wait up to 15s for Oracle listener
        for i in {1..15}; do
            if nc -z localhost 1521 2>/dev/null; then
                echo "[✓] Oracle Database listener ready on port 1521."
                break
            fi
            sleep 1
        done
    else
        echo "[✓] Oracle Database detected active on port 1521."
    fi

    if ! nc -z localhost 6379 2>/dev/null; then
        docker start energy-trading-platform-redis-1 2>/dev/null || docker start portal-redis 2>/dev/null || true
    fi
fi

echo ""
echo "[1/2] Launching Spring Boot Backend (:8080)..."
(cd "$DIR/backend" && mvn spring-boot:run) &
BACKEND_PID=$!

echo "[2/2] Launching React Vite Frontend (:3000)..."
(cd "$DIR/frontend" && npm run dev) &
FRONTEND_PID=$!

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true" EXIT INT TERM

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
echo "   • Student:   student1   / password123 (STU001, Aarav Sharma)"
echo "   • Recruiter: recruiter1 / password123 (Microsoft India)"
echo "   • Admin:     admin      / admin123"
echo "================================================================="

wait
