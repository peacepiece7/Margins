#!/usr/bin/env sh
set -eu

DEPLOY_ENV_PATH="${DEPLOY_ENV_PATH:-.env}"
RUNTIME_ENV_PATH="${RUNTIME_ENV_PATH:-.env.production}"
REMOTE_ENV_PATH="${MARGINS_REMOTE_ENV_PATH:-/opt/margins/.env}"
BACKUP_ENABLED="${BACKUP_ENABLED:-1}"

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)

load_env_file() {
  env_file="$1"
  [ -f "$env_file" ] || return 0
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
      ''|\#*) continue ;;
      *=*)
        name=${line%%=*}
        value=${line#*=}
        value=$(printf '%s' "$value" | sed "s/^['\"]//; s/['\"]$//")
        case "$name" in
          *[!A-Za-z0-9_]*|'') continue ;;
          *) export "$name=$value" ;;
        esac
        ;;
    esac
  done < "$env_file"
}

load_env_file "$REPO_ROOT/$DEPLOY_ENV_PATH"

if [ -z "${MARGINS_DEPLOY_HOST:-}" ] || [ -z "${MARGINS_DEPLOY_USER:-}" ]; then
  echo "Missing MARGINS_DEPLOY_HOST or MARGINS_DEPLOY_USER" >&2
  exit 1
fi

case "$REMOTE_ENV_PATH" in
  /*) ;;
  *) echo "REMOTE_ENV_PATH must be an absolute path" >&2; exit 1 ;;
esac

RUNTIME_ENV_FILE="$REPO_ROOT/$RUNTIME_ENV_PATH"
if [ ! -f "$RUNTIME_ENV_FILE" ]; then
  echo "Runtime env file not found: $RUNTIME_ENV_FILE" >&2
  exit 1
fi

SSH_OPTIONS="-o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new"
if [ -n "${MARGINS_DEPLOY_SSH_KEY:-}" ]; then
  if [ ! -f "$MARGINS_DEPLOY_SSH_KEY" ]; then
    echo "MARGINS_DEPLOY_SSH_KEY must point to an existing private key file" >&2
    exit 1
  fi
  SSH_OPTIONS="$SSH_OPTIONS -i $MARGINS_DEPLOY_SSH_KEY"
fi

TARGET="${MARGINS_DEPLOY_USER}@${MARGINS_DEPLOY_HOST}"
REMOTE_TMP="/tmp/margins.env.$(date +%Y%m%d%H%M%S).$$"
REMOTE_DIR=$(dirname "$REMOTE_ENV_PATH")

# shellcheck disable=SC2086
scp $SSH_OPTIONS "$RUNTIME_ENV_FILE" "$TARGET:$REMOTE_TMP"

# shellcheck disable=SC2086
ssh $SSH_OPTIONS "$TARGET" "set -e
remote_env='$REMOTE_ENV_PATH'
remote_tmp='$REMOTE_TMP'
remote_dir='$REMOTE_DIR'
backup_enabled='$BACKUP_ENABLED'
mkdir -p \"\$remote_dir\"
if [ \"\$backup_enabled\" = \"1\" ] && [ -f \"\$remote_env\" ]; then
  cp \"\$remote_env\" \"\$remote_env.backup.\$(date +%Y%m%d%H%M%S)\"
fi
mv \"\$remote_tmp\" \"\$remote_env\"
chmod 600 \"\$remote_env\"
printf 'runtime_env=updated\n'
printf 'runtime_env_path=%s\n' \"\$remote_env\"
printf 'runtime_env_keys='
sed -n 's/^\([A-Za-z_][A-Za-z0-9_]*\)=.*/\1/p' \"\$remote_env\" | sort | paste -sd ',' -
printf '\nruntime_env_perms='
stat -c '%a %U:%G' \"\$remote_env\"
printf '\n'
"
