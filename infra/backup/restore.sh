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

for name in PGHOST PGPORT PGUSER PGPASSWORD RESTIC_REPOSITORY RESTIC_PASSWORD; do
  require_env "$name"
done

SNAPSHOT="${1:-latest}"
RESTORE_DATABASE="${RESTORE_DATABASE:-${PGDATABASE:-renda_com_carro_restore}}"
RESTORE_ROOT="${RESTORE_ROOT:-/restore}"
RESTORE_STORAGE_DIR="${RESTORE_STORAGE_DIR:-$RESTORE_ROOT/storage}"
RESTORE_CONFIG_DIR="${RESTORE_CONFIG_DIR:-$RESTORE_ROOT/config-review}"
BACKUP_WORK_ROOT="${BACKUP_WORK_ROOT:-/work}"

[[ "$RESTORE_DATABASE" != "postgres" ]] || fail "o banco postgres não pode ser usado como destino"
mkdir -p "$RESTORE_STORAGE_DIR" "$RESTORE_CONFIG_DIR" "$BACKUP_WORK_ROOT"

if find "$RESTORE_STORAGE_DIR" -mindepth 1 -print -quit | grep -q .; then
  fail "o diretório de anexos de destino deve estar vazio: $RESTORE_STORAGE_DIR"
fi
if find "$RESTORE_CONFIG_DIR" -mindepth 1 -print -quit | grep -q .; then
  fail "o diretório de configuração restaurada deve estar vazio: $RESTORE_CONFIG_DIR"
fi

confirmation="${RESTORE_CONFIRMATION:-}"
if [[ "$confirmation" != "RESTORE" && -t 0 ]]; then
  printf 'Digite RESTORE para recriar o banco "%s" e restaurar os arquivos: ' "$RESTORE_DATABASE"
  read -r confirmation
fi
[[ "$confirmation" == "RESTORE" ]] || fail "confirmação textual RESTORE não fornecida"

RESTORE_TMP="$(mktemp -d "$BACKUP_WORK_ROOT/restore.XXXXXX")"
cleanup() {
  rm -rf "$RESTORE_TMP"
}
trap cleanup EXIT INT TERM

log "Restaurando snapshot Restic: $SNAPSHOT"
restic restore "$SNAPSHOT" --target "$RESTORE_TMP"
[[ -f "$RESTORE_TMP/database.dump" ]] || fail "snapshot não contém database.dump"

log "Recriando banco de destino: $RESTORE_DATABASE"
dropdb \
  --host="$PGHOST" \
  --port="$PGPORT" \
  --username="$PGUSER" \
  --if-exists \
  --force \
  "$RESTORE_DATABASE"
createdb \
  --host="$PGHOST" \
  --port="$PGPORT" \
  --username="$PGUSER" \
  "$RESTORE_DATABASE"
pg_restore \
  --host="$PGHOST" \
  --port="$PGPORT" \
  --username="$PGUSER" \
  --dbname="$RESTORE_DATABASE" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  "$RESTORE_TMP/database.dump"

if [[ -d "$RESTORE_TMP/storage" ]]; then
  cp -a "$RESTORE_TMP/storage"/. "$RESTORE_STORAGE_DIR"/
fi
if [[ -d "$RESTORE_TMP/config" ]]; then
  cp -a "$RESTORE_TMP/config"/. "$RESTORE_CONFIG_DIR"/
fi

log "Restauração concluída"
log "Banco: $RESTORE_DATABASE"
log "Anexos: $RESTORE_STORAGE_DIR"
log "Configuração para revisão manual: $RESTORE_CONFIG_DIR"
