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
# Supports two environments:
#   --docker   force Docker Compose mode (default if docker-compose db is running)
#   --k8s      force Kubernetes mode     (default if kubectl + postgres pod found)
#
# Configuration (env, with sane defaults):
#   ADMIN_EMAIL     admin email           (default: admin@devgate.local)
#   ADMIN_NAME      full name             (default: Administrator)
#   ADMIN_PASSWORD  raw password          (prompted interactively if unset)
#
# DB credentials:
#   Docker mode — read from ../../.env (POSTGRES_USERNAME / POSTGRES_PASSWORD / POSTGRES_DB)
#   K8s mode    — read from k8s secret devgate-secrets or env overrides
#                 (POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, POSTGRES_USERNAME, POSTGRES_PASSWORD)
#
# Security note: when `htpasswd -b` is used, the password is passed as an argv
# element and is briefly visible in `ps` during hash generation. Acceptable for a
# manual seed step. The password and the resulting hash are never printed.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/compose.yaml"
ENV_FILE="$SCRIPT_DIR/../.env"

BCRYPT_COST=12
MIN_PASSWORD_LENGTH=8

# ─── helpers ──────────────────────────────────────────────────────────────────

err() {
	printf 'Error: %s\n' "$1" >&2
	exit 1
}

usage() {
	cat <<'EOF'
Usage: ./seed-admin.sh [--docker|--k8s]

Creates an ADMIN user in the prod database.

Options:
  --docker   Use Docker Compose (default if compose db is running)
  --k8s      Use Kubernetes kubectl (default if postgres pod found in devgate ns)

Environment variables:
  ADMIN_EMAIL      admin email      (default: admin@devgate.local)
  ADMIN_NAME       full name        (default: Administrator)
  ADMIN_PASSWORD   raw password     (prompted if unset, min 8 chars)

Docker mode DB creds: read from ../../.env (POSTGRES_USERNAME / POSTGRES_PASSWORD / POSTGRES_DB)
K8s mode DB creds:    POSTGRES_HOST (default: postgres), POSTGRES_PORT (5432),
                       POSTGRES_DB (default: users), POSTGRES_USERNAME (default: postgres),
                       POSTGRES_PASSWORD (from k8s secret devgate-secrets or env)
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
	printf '%s' "${out#x:}"
}

# ─── argument parsing ─────────────────────────────────────────────────────────

MODE=""
for arg in "$@"; do
	case "$arg" in
		--docker) MODE="docker" ;;
		--k8s)    MODE="k8s" ;;
		-h|--help) usage; exit 0 ;;
		*) err "Unknown option: $arg (see --help)" ;;
	esac
done

# ─── detect environment ───────────────────────────────────────────────────────

K8S_NAMESPACE="devgate"
K8S_PG_POD=""

detect_k8s_postgres() {
	command -v kubectl >/dev/null 2>&1 || return 1
	# Check if the postgres pod exists and is running in the devgate namespace.
	K8S_PG_POD="$(kubectl -n "$K8S_NAMESPACE" get pods \
		-l app=postgres --no-headers 2>/dev/null \
		| awk '/Running/{print $1; exit}')"
	[ -n "$K8S_PG_POD" ]
}

docker_compose_db_running() {
	docker compose -f "$COMPOSE_FILE" ps --status running --services 2>/dev/null \
		| grep -qx "db"
}

# Resolve mode if not explicitly set.
if [ -z "$MODE" ]; then
	if detect_k8s_postgres; then
		MODE="k8s"
	elif docker_compose_db_running; then
		MODE="docker"
	else
		err "Cannot detect environment. Start Docker Compose DB or k8s postgres pod, " \
		    "or specify --docker / --k8s explicitly."
	fi
fi

printf 'Mode: %s\n' "$MODE"

# ─── password ─────────────────────────────────────────────────────────────────

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

ADMIN_HASH="$(gen_bcrypt "$ADMIN_PASSWORD")"
[ -n "$ADMIN_HASH" ] || err "Failed to generate password hash"

# The SQL that inserts the admin row.
SEED_SQL="INSERT INTO users (id, full_name, role, email, hashed_password)
VALUES (gen_random_uuid(), :'name', 'ADMIN', :'email', :'hash')
ON CONFLICT (email) DO NOTHING
RETURNING id;"

# ─── execute ──────────────────────────────────────────────────────────────────

created_id=""

case "$MODE" in
	docker)
		command -v docker >/dev/null 2>&1 || err "docker is required for --docker mode"

		load_env "$ENV_FILE"

		: "${POSTGRES_USERNAME:?POSTGRES_USERNAME not set (define in ../../.env or env)}"
		: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD not set (define in ../../.env or env)}"
		: "${POSTGRES_DB:?POSTGRES_DB not set (define in ../../.env or env)}"

		docker_compose_db_running || \
			err "DB service 'db' is not running. Start it: make -C \"$SCRIPT_DIR\" up"

		created_id="$(docker compose -f "$COMPOSE_FILE" exec -T \
			-e PGPASSWORD="$POSTGRES_PASSWORD" \
			db psql -v ON_ERROR_STOP=1 -tA \
			-U "$POSTGRES_USERNAME" -d "$POSTGRES_DB" \
			-v email="$ADMIN_EMAIL" -v name="$ADMIN_NAME" -v hash="$ADMIN_HASH" \
			<<< "$SEED_SQL")"
		;;

	k8s)
		detect_k8s_postgres || \
			err "Postgres pod not found in namespace '$K8S_NAMESPACE'. Deploy infra first."

		# Defaults matching k8s/10-secrets.yaml and k8s/infra/postgres.yaml.
		POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
		POSTGRES_PORT="${POSTGRES_PORT:-5432}"
		POSTGRES_DB="${POSTGRES_DB:-users}"
		POSTGRES_USERNAME="${POSTGRES_USERNAME:-postgres}"

		# Read password from k8s secret if not provided via env.
		if [ -z "${POSTGRES_PASSWORD:-}" ]; then
			POSTGRES_PASSWORD="$(kubectl -n "$K8S_NAMESPACE" get secret devgate-secrets \
				-o jsonpath='{.data.POSTGRES_PASSWORD}' 2>/dev/null \
				| base64 -d)" \
				|| err "Cannot read POSTGRES_PASSWORD from secret devgate-secrets"
		fi

		printf 'Pod: %s  DB: %s@%s:%s/%s\n' \
			"$K8S_PG_POD" "$POSTGRES_USERNAME" "$POSTGRES_HOST" "$POSTGRES_PORT" "$POSTGRES_DB"

		created_id="$(echo "$SEED_SQL" | kubectl -n "$K8S_NAMESPACE" exec -i \
			"$K8S_PG_POD" -- \
			env PGPASSWORD="$POSTGRES_PASSWORD" \
			psql -v ON_ERROR_STOP=1 -tA \
			-U "$POSTGRES_USERNAME" -d "$POSTGRES_DB" \
			-v email="$ADMIN_EMAIL" -v name="$ADMIN_NAME" -v hash="$ADMIN_HASH")"
		;;
esac

# ─── result ───────────────────────────────────────────────────────────────────

if [ -n "$created_id" ]; then
	printf '✓ Administrator created: %s (id=%s)\n' "$ADMIN_EMAIL" "$created_id"
else
	printf '• Administrator already exists, skipped: %s\n' "$ADMIN_EMAIL"
fi
