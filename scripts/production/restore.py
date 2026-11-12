#!/usr/bin/env python3
"""Explicit operator restoration of one database while its writer is stopped."""
import argparse
import os
from pathlib import Path
import sys

from compose import SERVICES, run


def restore(args):
    if args.database not in {s + '_db' for s in SERVICES} or args.confirm_restore != args.database:
        raise ValueError('Select a known database and repeat it with --confirm-restore')
    dump = Path(args.dump).resolve(strict=True)
    if not dump.is_file():
        raise ValueError('Expected one PostgreSQL custom-format dump file')
    name = args.database.removesuffix('_db')
    run('stop', name + '-service')
    with dump.open('rb') as stream:
        run('exec', '-T', 'postgres', 'pg_restore', '-U', 'postgres', '--exit-on-error',
            '--single-transaction', '--clean', '--if-exists', '--no-owner', '--no-privileges',
            '--role', name + '_app', '-d', args.database, stdin=stream)
    print('Database restored. The service remains stopped for operator verification.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('database')
    parser.add_argument('dump')
    parser.add_argument('--confirm-restore', required=True)
    try:
        import fcntl
        with Path('/opt/ieltspath/deploy.lock').open('a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            restore(parser.parse_args())
    except Exception:
        print('Restore failed; leave the service stopped and inspect database state.', file=sys.stderr)
        sys.exit(1)
