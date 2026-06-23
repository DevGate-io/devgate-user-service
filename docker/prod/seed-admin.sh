#!/usr/bin/env bash
#
# seed-admin.sh — bootstrap an ADMIN user directly into the prod database.
#
# There is no public way to create the first administrator (registration yields
# MEMBER; promoting to ADMIN requires an existing ADMIN). This operational script
# inserts an admin row straight into the `users` table with a proper BCrypt hash.
#
# Idempotent: re-running with the same email is a no-op (ON CONFLICT DO NOTHING).
#
# Configuration (env, with sane defaults):
#   ADMIN_EMAIL     admin email           (default: admin@devgate.local)
#   ADMIN_NAME      full name             (default: Administrator)
#   ADMIN_PASSWORD  raw password          (prompted interactively if unset)
# DB credentials are read from ../../.env (POSTGRES_USERNAME/PASSWORD/DB) unless
# already present in the environment.
#
# Security note: when `htpasswd -b` is used, the password is passed as an argv
# element and is briefly visible in `ps` during hash generation. Acceptable for a
# manual seed step. The password and the resulting hash are never printed.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/compose.yaml"
ENV_FILE="$SCRIPT_DIR/../../.env"

DB_SERVICE="db"
BCRYPT_COST=12
MIN_PASSWORD_LENGTH=8

err() {
	printf 'Error: %s\n' "$1" >&2
	exit 1
}

usage() {
	cat <<'EOF'
Usage: ./seed-admin.sh

Creates an ADMIN user in the prod database (service "db").

Environment variables:
  ADMIN_EMAIL      admin email      (default: admin@devgate.local)
  ADMIN_NAME       full name        (default: Administrator)
  ADMIN_PASSWORD   raw password     (prompted if unset, min 8 chars)

DB credentials (POSTGRES_USERNAME / POSTGRES_PASSWORD / POSTGRES_DB) are read
from ../../.env unless already set in the environment.
EOF
}

# Load KEY=VALUE pairs from the project .env without overriding already-set vars.
load_env() {
	local file="$1" key val
	[ -f "$file" ] || return 0
	while IFS='=' read -r key val; do
		case "$key" in
			''|\#*) continue ;;
		esac
		key="${key//[[:space:]]/}"
		[ -n "$key" ] || continue
		# strip one pair of surrounding double quotes, if any
		val="${val%\"}"
		val="${val#\"}"
		if [ -z "${!key:-}" ]; then
			export "$key=$val"
		fi
	done < "$file"
}

# Produce a BCrypt hash ($2y$...) for the given password on stdout.
gen_bcrypt() {
	local pw="$1" out
	if command -v htpasswd >/dev/null 2>&1; then
		out="$(htpasswd -nbBC "$BCRYPT_COST" x "$pw")"
	elif command -v docker >/dev/null 2>&1; then
		out="$(docker run --rm httpd:2.4-alpine htpasswd -nbBC "$BCRYPT_COST" x "$pw")"
	else
		err "Cannot generate BCrypt hash: neither 'htpasswd' nor 'docker' is available"
	fi
	# htpasswd prints "x:$2y$..."; drop the "x:" prefix
	printf '%s' "${out#x:}"
}

case "${1:-}" in
	-h|--help) usage; exit 0 ;;
esac

command -v docker >/dev/null 2>&1 || err "docker is required but not found in PATH"

load_env "$ENV_FILE"

: "${POSTGRES_USERNAME:?not set (define it in ../../.env or the environment)}"
: "${POSTGRES_PASSWORD:?not set (define it in ../../.env or the environment)}"
: "${POSTGRES_DB:?not set (define it in ../../.env or the environment)}"

ADMIN_EMAIL="${ADMIN_EMAIL:-admin@devgate.local}"
ADMIN_NAME="${ADMIN_NAME:-Administrator}"

if [ -z "${ADMIN_PASSWORD:-}" ]; then
	read -rsp "Admin password: " ADMIN_PASSWORD
	echo
	read -rsp "Confirm password: " ADMIN_PASSWORD_CONFIRM
	echo
	[ "$ADMIN_PASSWORD" = "$ADMIN_PASSWORD_CONFIRM" ] || err "Passwords do not match"
fi

[ "${#ADMIN_PASSWORD}" -ge "$MIN_PASSWORD_LENGTH" ] \
	|| err "Password must be at least $MIN_PASSWORD_LENGTH characters long"

# Verify the DB service is running before attempting the insert.
if ! docker compose -f "$COMPOSE_FILE" ps --status running --services 2>/dev/null \
	| grep -qx "$DB_SERVICE"; then
	err "DB service '$DB_SERVICE' is not running. Start it first: make -C \"$SCRIPT_DIR\" up"
fi

ADMIN_HASH="$(gen_bcrypt "$ADMIN_PASSWORD")"
[ -n "$ADMIN_HASH" ] || err "Failed to generate password hash"

# Insert via psql variables (:'name' etc.) — no string concatenation into SQL.
# Role is stored as the enum constant name (@Enumerated(STRING)) -> 'ADMIN'.
created_id="$(docker compose -f "$COMPOSE_FILE" exec -T \
	-e PGPASSWORD="$POSTGRES_PASSWORD" \
	"$DB_SERVICE" psql -v ON_ERROR_STOP=1 -tA \
	-U "$POSTGRES_USERNAME" -d "$POSTGRES_DB" \
	-v email="$ADMIN_EMAIL" -v name="$ADMIN_NAME" -v hash="$ADMIN_HASH" <<'SQL'
INSERT INTO users (id, full_name, role, email, hashed_password)
VALUES (gen_random_uuid(), :'name', 'ADMIN', :'email', :'hash')
ON CONFLICT (email) DO NOTHING
RETURNING id;
SQL
)"

if [ -n "$created_id" ]; then
	printf '✓ Administrator created: %s (id=%s)\n' "$ADMIN_EMAIL" "$created_id"
else
	printf '• Administrator already exists, skipped: %s\n' "$ADMIN_EMAIL"
fi
