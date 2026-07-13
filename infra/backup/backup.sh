#!/usr/bin/env bash
set -Eeuo pipefail

log() {
  printf '[%s] %s\n' "$(date --iso-8601=seconds)" "$*"
}

fail() {
  log "ERRO: $*" >&2
  exit 1
}

require_env() {
  local name="$1"
  [[ -n "${!name:-}" ]] || fail "variável obrigatória ausente: ${name}"
}

for name in PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD RESTIC_REPOSITORY RESTIC_PASSWORD; do
  require_env "$name"
done

ATTACHMENT_ROOT="${ATTACHMENT_ROOT:-/source/storage}"
CONFIG_ROOT="${CONFIG_ROOT:-/source/config}"
BACKUP_WORK_ROOT="${BACKUP_WORK_ROOT:-/work}"
RESTIC_HOST="${RESTIC_HOST:-renda-com-carro}"
RUN_CHECK=false
[[ "${1:-}" == "--check" ]] && RUN_CHECK=true
[[ "$(date +%u)" == "7" ]] && RUN_CHECK=true

mkdir -p "$BACKUP_WORK_ROOT"
LOCK_DIR="$BACKUP_WORK_ROOT/.backup.lock"
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  fail "já existe outro backup em execução"
fi

STAGE_DIR="$(mktemp -d "$BACKUP_WORK_ROOT/stage.XXXXXX")"
cleanup() {
  rm -rf "$STAGE_DIR" "$LOCK_DIR"
}
trap cleanup EXIT INT TERM

if ! restic cat config >/dev/null 2>&1; then
  if [[ "${RESTIC_AUTO_INIT:-false}" == "true" ]]; then
    log "Inicializando repositório Restic"
    restic init
  else
    fail "repositório Restic indisponível ou não inicializado; valide a conexão ou defina RESTIC_AUTO_INIT=true somente na primeira execução"
  fi
fi

log "Gerando dump PostgreSQL"
pg_dump \
  --format=custom \
  --compress=6 \
  --no-owner \
  --no-privileges \
  --file="$STAGE_DIR/database.dump"

mkdir -p "$STAGE_DIR/storage" "$STAGE_DIR/config"
if [[ -d "$ATTACHMENT_ROOT" ]]; then
  cp -a "$ATTACHMENT_ROOT"/. "$STAGE_DIR/storage"/
fi
if [[ -d "$CONFIG_ROOT" ]]; then
  cp -a "$CONFIG_ROOT"/. "$STAGE_DIR/config"/
fi

cat > "$STAGE_DIR/metadata.txt" <<META
created_at=$(date --iso-8601=seconds)
database=${PGDATABASE}
host=${RESTIC_HOST}
format=postgres-custom-plus-storage
META

log "Enviando snapshot criptografado"
(
  cd "$STAGE_DIR"
  restic backup database.dump storage config metadata.txt \
    --host "$RESTIC_HOST" \
    --tag renda-com-carro \
    --tag automatic
)

log "Aplicando retenção 7 diários, 4 semanais e 12 mensais"
restic forget \
  --host "$RESTIC_HOST" \
  --tag renda-com-carro \
  --keep-daily 7 \
  --keep-weekly 4 \
  --keep-monthly 12 \
  --prune

if [[ "$RUN_CHECK" == "true" ]]; then
  log "Executando verificação semanal do repositório"
  restic check --read-data-subset=5%
fi

log "Backup concluído com sucesso"
