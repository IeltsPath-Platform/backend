#!/usr/bin/env bash
# Copies goals, learning activity and study time from user_db into learning_db, then checks that nothing is missing.
# Both databases live on the Compose PostgreSQL. Nothing is written to user_db and nothing is deleted anywhere.
#
#   POSTGRES_PASSWORD=... scripts/copy-study-data-to-learning.sh [--check-only]
#
# --check-only    only compare the two databases (reconcile-study-data.sql); copy nothing
#
# Environment (defaults in brackets): POSTGRES_PASSWORD (required), POSTGRES_USER [postgres],
# POSTGRES_SERVICE [postgres] (the Compose service), USER_DB [user_db], LEARNING_DB [learning_db].
# Set POSTGRES_CONTAINER to a container name to use `docker exec` on it instead of the Compose service.
# The extension dblink is created in learning_db, which needs a superuser such as the default postgres user.
# Safe to run again; see copy-study-data.sql for how a row in learning_db is protected from an older copy.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
sql_dir="$here/study-data-copy"
: "${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD (the Compose PostgreSQL password)}"
user="${POSTGRES_USER:-postgres}"
service="${POSTGRES_SERVICE:-postgres}"
user_db="${USER_DB:-user_db}"
learning_db="${LEARNING_DB:-learning_db}"

if [ -n "${POSTGRES_CONTAINER:-}" ]; then
  exec_in_postgres=(docker exec -i "$POSTGRES_CONTAINER")
else
  exec_in_postgres=(docker compose -f "$here/../docker-compose.yml" exec -T "$service")
fi

mode="copy"
case "${1:-}" in
  "") ;;
  --check-only) mode="check" ;;
  *) echo "Unknown option: $1" >&2; exit 2 ;;
esac

# Run a script file in learning_db with :src set. The connection string goes through stdin, not the command line.
run() {
  local file="$1"
  { printf "\\\\set src 'host=localhost dbname=%s user=%s password=%s'\n" "$user_db" "$user" "$POSTGRES_PASSWORD"
    cat "$file"
  } | "${exec_in_postgres[@]}" psql -X -q -v ON_ERROR_STOP=1 -U "$user" -d "$learning_db"
}

if [ "$mode" = "copy" ]; then
  echo "Copying study data from $user_db to $learning_db ..."
  run "$sql_dir/copy-study-data.sql"
fi
echo "Comparing $user_db with $learning_db ..."
run "$sql_dir/reconcile-study-data.sql"
echo "Done: learning_db holds every row of the copied tables that user_db holds."
