#!/bin/sh
set -eu

# Both API and consumer drop privileges before running the supplied command.
exec setpriv --reuid=appuser --regid=appuser --init-groups "$@"
