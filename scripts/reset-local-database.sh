#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_ARGS=()
START_ARGS=(up -d --build)
AUTO_CONFIRM=false

usage() {
  echo "사용법: ./scripts/reset-local-database.sh [--yes] [--local]"
  echo "  --yes    RESET 확인 입력을 생략합니다."
  echo "  --local  ARM64 사전 빌드 이미지용 docker-compose.local.yml을 사용합니다."
}

for argument in "$@"; do
  case "${argument}" in
    --yes)
      AUTO_CONFIRM=true
      ;;
    --local)
      COMPOSE_ARGS=(-f docker-compose.local.yml)
      START_ARGS=(up -d --no-build --pull never)
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "알 수 없는 옵션: ${argument}" >&2
      usage >&2
      exit 2
      ;;
  esac
done

cd "${PROJECT_ROOT}"

echo "주의: lecture_db의 기존 스키마와 데이터를 모두 삭제하고 현재 init-db 기준으로 다시 만듭니다."
echo "MariaDB Docker 볼륨과 Kafka 데이터는 삭제하지 않습니다."

if [[ "${AUTO_CONFIRM}" != true ]]; then
  read -r -p "계속하려면 RESET을 입력하세요: " confirmation
  if [[ "${confirmation}" != "RESET" ]]; then
    echo "데이터베이스 초기화를 취소했습니다."
    exit 1
  fi
fi

docker compose "${COMPOSE_ARGS[@]}" config --quiet
docker compose "${COMPOSE_ARGS[@]}" stop
docker compose "${COMPOSE_ARGS[@]}" up -d mariadb

echo "MariaDB 준비 상태를 확인합니다."
for attempt in {1..30}; do
  if docker compose "${COMPOSE_ARGS[@]}" exec -T mariadb healthcheck.sh --connect --innodb_initialized >/dev/null 2>&1; then
    break
  fi
  if [[ "${attempt}" -eq 30 ]]; then
    echo "MariaDB가 제한 시간 안에 준비되지 않았습니다." >&2
    exit 1
  fi
  sleep 2
done

docker compose "${COMPOSE_ARGS[@]}" exec -T mariadb mariadb -uroot -pSqlDba-1 -e \
  "DROP DATABASE IF EXISTS lecture_db; CREATE DATABASE lecture_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON lecture_db.* TO 'manager'@'%'; FLUSH PRIVILEGES;"

docker compose "${COMPOSE_ARGS[@]}" exec -T mariadb mariadb -uroot -pSqlDba-1 lecture_db \
  < "${PROJECT_ROOT}/init-db/01_init.sql"

for migration in "${PROJECT_ROOT}"/init-db/migrations/*.sql; do
  echo "적용 중: ${migration#"${PROJECT_ROOT}/"}"
  docker compose "${COMPOSE_ARGS[@]}" exec -T mariadb mariadb -uroot -pSqlDba-1 lecture_db < "${migration}"
done

docker compose "${COMPOSE_ARGS[@]}" "${START_ARGS[@]}"
docker compose "${COMPOSE_ARGS[@]}" ps -a

echo "lecture_db 초기화와 서비스 재기동이 완료되었습니다."
