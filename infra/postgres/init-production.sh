#!/bin/bash
set -euo pipefail
set +x
for service in user content library assessment access learning game notification community; do
    variable="${service^^}_DB_PASSWORD"
    password="${!variable:?Missing service database password}"
    # Values travel on stdin, never psql argv; escape SQL literals defensively.
    password="${password//\'/\'\'}"
    printf "CREATE ROLE %s_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '%s';\nCREATE DATABASE %s_db OWNER %s_app;\nREVOKE CONNECT ON DATABASE %s_db FROM PUBLIC;\nGRANT CONNECT ON DATABASE %s_db TO %s_app;\n" \
        "$service" "$password" "$service" "$service" "$service" "$service" "$service" |
        psql --username "$POSTGRES_USER" --dbname postgres --no-psqlrc --set ON_ERROR_STOP=1 > /dev/null
done
printf 'REVOKE CONNECT ON DATABASE postgres FROM PUBLIC;\nREVOKE CONNECT ON DATABASE template1 FROM PUBLIC;\n' |
    psql --username "$POSTGRES_USER" --dbname postgres --no-psqlrc --set ON_ERROR_STOP=1 > /dev/null
