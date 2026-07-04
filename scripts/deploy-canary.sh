#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/yszt-canary}"
DOMAIN="${DOMAIN:-yszt.jilinpc.com}"
CANARY_PORT="${YSZT_CANARY_PORT:-18080}"
CANARY_BIND="${YSZT_CANARY_BIND:-127.0.0.1}"

cd "$APP_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is not installed" >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose plugin is not available" >&2
  exit 1
fi

if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
  echo "MYSQL_ROOT_PASSWORD is required" >&2
  exit 1
fi

if docker ps -a --format '{{.Names}}' | grep -Eq '^(yszt-canary-mysql|yszt-canary-redis|yszt-canary-app|yszt-canary-frontend)$'; then
  echo "canary containers already exist; inspect before redeploying" >&2
  docker ps -a --filter "name=yszt-canary-" --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
  exit 1
fi

if command -v ss >/dev/null 2>&1 && ss -ltn "( sport = :$CANARY_PORT )" | grep -q ":$CANARY_PORT"; then
  echo "port $CANARY_PORT is already in use; choose another YSZT_CANARY_PORT" >&2
  exit 1
fi

echo "Deploying $DOMAIN canary at ${CANARY_BIND}:${CANARY_PORT}"
docker compose -f docker-compose.canary.yml build
docker compose -f docker-compose.canary.yml up -d

echo "Waiting for frontend health..."
for i in $(seq 1 60); do
  if docker compose -f docker-compose.canary.yml ps --status running | grep -q 'yszt-canary-frontend'; then
    if docker exec yszt-canary-frontend wget -qO- http://127.0.0.1/ | grep -q '<div id="app"'; then
      break
    fi
  fi
  sleep 2
done

echo "Health checks:"
docker compose -f docker-compose.canary.yml ps
docker exec yszt-canary-frontend wget -qO- http://127.0.0.1/api/health

cat <<EOF

Canary is ready.
Local URL on server: http://${CANARY_BIND}:${CANARY_PORT}/
Remote validation:
  ssh -L ${CANARY_PORT}:127.0.0.1:${CANARY_PORT} root@<server>
  open http://127.0.0.1:${CANARY_PORT}/

No existing 80/443 reverse proxy has been modified.
EOF
