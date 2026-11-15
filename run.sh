#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

fail() {
    printf '\nНе удалось запустить AI Compass: %s\n' "$1" >&2
    exit 1
}

command -v python3 >/dev/null 2>&1 || fail 'нужен Python 3.12 или новее.'
python3 - <<'PY' || fail 'нужен Python 3.12 или новее.'
import sys
if sys.version_info < (3, 12):
    sys.exit(1)
PY
command -v docker >/dev/null 2>&1 || fail 'установите Docker Engine с Docker Compose: https://docs.docker.com/engine/install/'
docker compose version >/dev/null 2>&1 || fail 'не найден Docker Compose (docker compose).'
docker info >/dev/null 2>&1 || fail 'Docker не запущен или текущий пользователь не имеет доступа. Проверьте: docker info'
command -v sha256sum >/dev/null 2>&1 || fail 'не найдена команда sha256sum (GNU coreutils).'

mkdir -p .runtime
LOG="$PWD/.runtime/jury-run.log"
touch "$LOG"
trap 'printf "\nОшибка запуска. Подробности: %s\n" "$LOG" >&2' ERR

if [[ ! -f .env || ! -x .tools/jdk-21.0.13+9/bin/java || ! -x .tools/node-v24.22.3-linux-x64/bin/node ]]; then
    echo 'Подготавливаем инструменты проекта…'
    bash scripts/setup.sh >> "$LOG" 2>&1
fi

if bash scripts/smoke.sh >> "$LOG" 2>&1; then
    echo 'Сайт уже работает.'
else
    echo 'Запускаем AI Compass…'
    if [[ -f backend/target/ai-compass-0.1.0.jar && -f frontend/node_modules/vite/bin/vite.js ]]; then
        bash scripts/dev-up.sh --no-build >> "$LOG" 2>&1
    else
        bash scripts/dev-up.sh >> "$LOG" 2>&1
    fi
fi

# Use the existing project-scoped Docker Compose and database settings.
source scripts/common.sh
MANIFEST='data/catalog/catalog-2026-11-14-ce1.json'
EXPECTED_SHA256='7d455d0f5c9b924a14111689195e8f4cc213d653df8df36e5fbb6f98e2b3c627'
CE_VERSION='ce000000-0000-4000-8000-000000000001'

state=$(compose exec -T postgres psql -v ON_ERROR_STOP=1 -At -F '|' \
    -U "$DB_USER" -d "$DB_NAME" -c \
    "SELECT (SELECT count(*) FROM compass.catalog_version), COALESCE(version_id::text, 'NONE'), revision FROM compass.catalog_active WHERE singleton;" 2>> "$LOG")
IFS='|' read -r version_count active_version active_revision <<< "$state"
[[ "$version_count" =~ ^[0-9]+$ && "$active_revision" =~ ^[0-9]+$ ]] || fail 'не удалось прочитать состояние каталога.'

if [[ "$active_version" == 'NONE' && "$active_revision" == '0' ]]; then
    # Initialize only a fresh catalog, or resume an interrupted import of this exact version.
    if (( version_count > 0 )); then
        [[ "$version_count" == '1' ]] || fail 'в базе уже есть другие версии каталога; автоматическая публикация остановлена.'
        existing=$(compose exec -T postgres psql -v ON_ERROR_STOP=1 -At \
            -U "$DB_USER" -d "$DB_NAME" -c 'SELECT id::text FROM compass.catalog_version LIMIT 1;' 2>> "$LOG")
        [[ "$existing" == "$CE_VERSION" ]] || fail 'в базе есть другая неопубликованная версия; требуется ручная проверка.'
    fi
    [[ -f "$MANIFEST" ]] || fail "не найден файл каталога: $MANIFEST"
    printf '%s  %s\n' "$EXPECTED_SHA256" "$MANIFEST" | sha256sum --check --status \
        || fail 'проверка SHA-256 каталога не прошла. Файл не будет опубликован.'

    echo 'Загружаем каталог из 23 AI-продуктов…'
    bash scripts/catalog.sh validate --file="$MANIFEST" >> "$LOG" 2>&1
    imported=$(bash scripts/catalog.sh import --file="$MANIFEST" 2>> "$LOG")
    printf '%s\n' "$imported" >> "$LOG"
    if [[ "$imported" =~ IMPORTED[[:space:]]version=([0-9a-f-]+)[[:space:]]hash=([0-9a-f]+) ]]; then
        imported_version="${BASH_REMATCH[1]}"
        imported_hash="${BASH_REMATCH[2]}"
    else
        fail 'не удалось получить идентификатор и хеш проверенного каталога.'
    fi
    [[ "$imported_version" == "$CE_VERSION" ]] || fail 'идентификатор импортированного каталога не совпадает.'
    bash scripts/catalog.sh review --version="$CE_VERSION" --hash="$imported_hash" \
        --actor='local-jury-bootstrap' >> "$LOG" 2>&1
    bash scripts/catalog.sh publish --version="$CE_VERSION" --expected-revision=0 \
        --actor='local-jury-bootstrap' --reason='Initial local demo dataset from repository' >> "$LOG" 2>&1
elif [[ "$active_version" == 'NONE' ]]; then
    fail 'в базе нет активной версии каталога; требуется ручная проверка.'
else
    echo 'Используем уже опубликованный каталог. Данные сохранены.'
fi

python3 - <<'PY'
import json, os, urllib.request
url = f"http://127.0.0.1:{os.environ['FRONTEND_PORT']}/api/v1/catalog/meta"
with urllib.request.urlopen(url, timeout=10) as response:
    meta = json.load(response)
if meta.get('status') != 'AVAILABLE':
    raise SystemExit('Каталог не опубликован. Смотрите .runtime/jury-run.log')
print(f"\nAI Compass запущен! Каталог: {meta['productCount']} AI-продуктов.")
print(f"Сайт: http://127.0.0.1:{os.environ['FRONTEND_PORT']}/")
print('Остановка: bash scripts/dev-down.sh')
PY
