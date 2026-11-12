#!/bin/sh
set -eu
umask 077
: "${REDIS_PASSWORD:?Redis password is required}"
# Renderer restricts Redis passwords to URL-safe characters; no config injection.
printf 'appendonly yes\nsave ""\nrequirepass %s\n' "$REDIS_PASSWORD" > /tmp/redis-production.conf
chown redis:redis /tmp/redis-production.conf
exec /usr/local/bin/docker-entrypoint.sh redis-server /tmp/redis-production.conf
