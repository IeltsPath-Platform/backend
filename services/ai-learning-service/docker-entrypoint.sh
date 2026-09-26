#!/bin/sh
set -eu

# Bind mounts may be root-owned on Linux. Both API and consumer drop privileges
# before running the supplied command; only the API receives a catalog mount.
chown -R appuser:appuser /app/data 2>/dev/null || true
exec setpriv --reuid=appuser --regid=appuser --init-groups "$@"
