#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$DIR/.env"

# shellcheck source=/dev/null
[ -f "$ENV_FILE" ] && source "$ENV_FILE"

PGHOST="localhost"
PGPORT="5433"
PGUSER="${POSTGRES_USERNAME:-postgres}"
PGPASSWORD="${POSTGRES_PASSWORD:-postgres}"
PGDATABASE="${POSTGRES_DB:-devgate-users}"
PASSWORD="${SEED_PASSWORD:-password123}"

export PGHOST PGPORT PGUSER PGPASSWORD PGDATABASE

if ! command -v psql &>/dev/null; then
	echo "ERROR: psql not found. Install postgresql-client."
	exit 1
fi

if ! command -v python3 &>/dev/null; then
	echo "ERROR: python3 not found."
	exit 1
fi

if ! python3 -c "import bcrypt" 2>/dev/null; then
	echo "ERROR: python3 bcrypt module not found. Run: pip install bcrypt"
	exit 1
fi

psql -Atc "
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    last_login DATETIME,
    role VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL
);
" > /dev/null

EXISTING=$(psql -Atc "SELECT COUNT(*) FROM users;" 2>/dev/null || echo "0")
if [ "$EXISTING" -gt 0 ]; then
	echo "Database already has $EXISTING user(s), skipping seed."
	exit 0
fi

echo "Seeding initial users..."

hash_password() {
	python3 -c "import bcrypt; print(bcrypt.hashpw(b'$1', bcrypt.gensalt(rounds=10)).decode())"
}

hash_password "$PASSWORD" > /dev/null

USERS=(
	"admin@devgate.io|ADMIN|Admin User"
	"manager@devgate.io|MANAGER|Manager User"
	"devops@devgate.io|DEVOPS|DevOps User"
	"qa@devgate.io|QA|QA User"
	"member@devgate.io|MEMBER|Member User"
)

for entry in "${USERS[@]}"; do
	IFS="|" read -r email role name <<< "$entry"
	pw_hash=$(hash_password "$PASSWORD")
	psql -Atc "INSERT INTO users (id, full_name, role, email, hashed_password) VALUES (gen_random_uuid(), '$name', '$role', '$email', '$pw_hash');"
	echo "  + $name ($role) <$email>"
done

echo "Done. ${#USERS[@]} users created."
