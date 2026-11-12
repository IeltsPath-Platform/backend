#!/usr/bin/env python3
"""Dump all service databases and require an acknowledged off-host copy."""
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import tarfile

from compose import SERVICES, command, run

BASE = Path('/opt/ieltspath')
# Executed on the separate backup machine over pinned OpenSSH transport.
# Only regular archive files under the chosen timestamp are accepted.
REMOTE = r'''
import os, pathlib, shutil, sys, tarfile
os.umask(0o077)
root = pathlib.Path(sys.argv[1]).resolve()
if root == pathlib.Path(root.anchor):
    raise SystemExit('Backup target cannot be a filesystem root')
root.mkdir(parents=True, exist_ok=True, mode=0o700)
root.chmod(0o700)
marker = root / '.ieltspath-backup-root'
if not marker.exists() and any(root.iterdir()):
    raise SystemExit('Backup target must be an empty dedicated directory')
marker.touch(mode=0o600, exist_ok=True)
stamp = sys.argv[2]
daily = root / 'daily' / stamp
daily.mkdir(parents=True, exist_ok=False, mode=0o700)
with tarfile.open(fileobj=sys.stdin.buffer, mode='r|') as archive:
    for member in archive:
        if not member.isfile() or pathlib.PurePosixPath(member.name).name != member.name:
            raise SystemExit('Unsafe backup archive')
        target = daily / member.name
        with target.open('xb') as output:
            shutil.copyfileobj(archive.extractfile(member), output)
        target.chmod(0o600)
(daily / '.complete').touch(mode=0o600)
if sys.argv[3] == 'weekly':
    weekly = root / 'weekly' / stamp
    weekly.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    shutil.copytree(daily, weekly)
for category, count in [('daily', 7), ('weekly', 4)]:
    folder = root / category
    if folder.exists():
        snapshots = sorted(p for p in folder.iterdir() if p.is_dir() and p.name.isdigit() and (p / '.complete').exists())
        by_day = {p.name[:8]: p for p in snapshots}
        keep = set(sorted(by_day.values())[-count:])
        for old in snapshots:
            if old not in keep:
                shutil.rmtree(old)
print('Off-host backup acknowledged')
'''


def rotate(root):
    for category, count in [('daily', 7), ('weekly', 4)]:
        directory = root / category
        if directory.exists():
            snapshots = sorted(p for p in directory.iterdir() if p.is_dir() and p.name.isdigit() and (p / '.complete').exists())
            by_day = {p.name[:8]: p for p in snapshots}
            keep = set(sorted(by_day.values())[-count:])
            for old in snapshots:
                if old not in keep:
                    shutil.rmtree(old)


def backup(operations, tag, owner, env_root, allow_empty=False):
    target = operations['BACKUP_TARGET']
    match = re.fullmatch(r'([a-z_][a-z0-9_-]*@[A-Za-z0-9.-]+):(/[A-Za-z0-9_./-]+)', target)
    if not match or '..' in Path(match[2]).parts or len(Path(match[2]).parts) < 2:
        raise ValueError('BACKUP_TARGET must be user@backup-host:/absolute/dedicated/path')
    timestamp = datetime.now(timezone.utc)
    stamp = timestamp.strftime('%Y%m%d%H%M%S%f')
    root = BASE / 'backups'
    daily = root / 'daily' / stamp
    daily.mkdir(mode=0o700, parents=True)
    ids = run('ps', '-q', 'postgres', tag=tag, owner=owner, env_root=env_root,
              stdout=subprocess.PIPE).stdout.strip()
    if not ids and not allow_empty:
        raise RuntimeError('PostgreSQL is unavailable; deployment stopped before backup')
    if not ids:
        _, docker_env = command(tag, owner, env_root)
        volume = subprocess.run(['docker', 'volume', 'inspect', 'ieltspath_postgres-data'],
                                env=docker_env, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        if volume.returncode == 0 or b'no such volume' not in volume.stderr.lower():
            raise RuntimeError('Existing or unavailable PostgreSQL storage cannot be treated as an empty bootstrap')
    if ids:
        for name in SERVICES:
            with (daily / (name + '_db.dump')).open('xb') as output:
                run('exec', '-T', 'postgres', 'pg_dump', '-U', 'postgres', '-Fc', '--no-owner',
                    '--no-privileges', name + '_db', tag=tag, owner=owner, env_root=env_root, stdout=output)
    manifest = {'created': timestamp.isoformat(), 'release': tag,
                'databases': list(SERVICES) if ids else [], 'bootstrap_empty': not bool(ids)}
    (daily / 'manifest.json').write_text(json.dumps(manifest), encoding='utf-8')
    # Environment files are required for disaster recovery of roles and integrations.
    # This directory is private on both machines; the destination needs encryption at rest.
    env_archive = daily / 'environment.tar.gz'
    with tarfile.open(env_archive, 'w:gz') as archive:
        archive.add(env_root, arcname='env', recursive=True)
    weekly = timestamp.weekday() == 6
    remote_command = shlex.join(['python3', '-c', REMOTE, match[2], stamp, 'weekly' if weekly else 'daily'])
    process = subprocess.Popen(['ssh', '-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=yes',
                                match[1], remote_command], stdin=subprocess.PIPE)
    try:
        with tarfile.open(fileobj=process.stdin, mode='w|') as archive:
            for path in sorted(daily.iterdir()):
                archive.add(path, arcname=path.name)
        process.stdin.close()
        if process.wait() != 0:
            raise RuntimeError('Off-host backup failed; deployment stopped')
    finally:
        if process.poll() is None:
            process.kill()
            process.wait()
    (daily / '.complete').touch(mode=0o600)
    if weekly:
        (root / 'weekly').mkdir(parents=True, exist_ok=True, mode=0o700)
        shutil.copytree(daily, root / 'weekly' / stamp)
    rotate(root)
    return daily


if __name__ == '__main__':
    os.umask(0o077)
    env_root = Path(os.environ.get('PRODUCTION_ENV_ROOT', '/opt/ieltspath/env'))
    config = json.loads((env_root / 'operations.json').read_text(encoding='utf-8'))
    try:
        import fcntl
        with (BASE / 'deploy.lock').open('a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            backup(config, os.environ['IMAGE_TAG'], os.environ['IMAGE_OWNER'], env_root)
    except Exception as error:
        # Never print subprocess input or environment files.
        print('Backup failed: ' + str(error), file=sys.stderr)
        sys.exit(1)
