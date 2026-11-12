#!/usr/bin/env python3
"""Run as root from a reviewed, private incoming release directory."""
import fcntl
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request

from backup import backup
from compose import ROOT, command, run

BASE = Path('/opt/ieltspath')
STAGE = 'input validation'


def stage(name):
    global STAGE
    STAGE = name
    print('Deployment: ' + name, flush=True)


def smoke(operations):
    origin = 'https://' + operations['API_HOST']
    with urllib.request.urlopen(origin + '/actuator/health', timeout=20) as response:
        if json.load(response).get('status') != 'UP':
            raise RuntimeError('Public gateway health failed')
    payload = json.dumps({'email': operations['SMOKE_LOGIN_EMAIL'],
                          'password': operations['SMOKE_LOGIN_PASSWORD']}).encode()
    request = urllib.request.Request(origin + '/auth/login', data=payload,
                                     headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=30) as response:
        result = json.load(response)
        # Verify a successful auth result, without printing any token or cookie.
        if not isinstance(result, dict) or not result.get('accessToken'):
            raise RuntimeError('Login returned no access token')
        cookies = response.headers.get_all('Set-Cookie', [])
        if not any('refresh' in cookie.lower() and 'secure' in cookie.lower() for cookie in cookies):
            raise RuntimeError('Login returned no secure refresh cookie')


def deploy(tag, owner):
    command(tag, owner)  # Validate before using any input in paths.
    if os.geteuid() != 0:
        raise RuntimeError('Deploy requires root to install mode-600 service files')
    BASE.mkdir(mode=0o700, parents=True, exist_ok=True)
    stage('acquiring deployment lock')
    with (BASE / 'deploy.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        candidate_env = ROOT / 'private-env'
        operations = json.loads((candidate_env / 'operations.json').read_text(encoding='utf-8'))
        monitoring = operations.get('OBSERVABILITY_ENABLED', False)
        env_root = BASE / 'env'
        current = BASE / 'RELEASE'
        previous = current.read_text().strip() if current.exists() else None
        # Back up existing credentials as well as existing data before installing new env.
        source_env = env_root if env_root.exists() else candidate_env
        stage('off-host backup')
        backup(operations, previous or tag, owner, source_env, allow_empty=previous is None)
        stage('installing private environment files')
        if env_root.exists():
            old = BASE / ('env-before-' + tag + '-' + str(time.time_ns()))
            env_root.rename(old)
        shutil.copytree(candidate_env, env_root)
        os.chmod(env_root, 0o700)
        for path in env_root.iterdir():
            os.chmod(path, 0o600)
        stage('validating production Compose')
        run('config', '--quiet', tag=tag, owner=owner, env_root=env_root, observability=monitoring)
        # Authentication was provisioned on the VPS with a read-only GHCR token.
        stage('pulling immutable images')
        run('pull', tag=tag, owner=owner, env_root=env_root, observability=monitoring)
        (BASE / 'ATTEMPT').write_text(tag + '\n', encoding='utf-8')
        stage('starting containers and waiting for health')
        run('up', '-d', '--no-build', '--remove-orphans', '--wait', '--wait-timeout', '600',
            tag=tag, owner=owner, env_root=env_root, observability=monitoring)
        # Eureka route registration can lag behind actuator health.
        stage('checking HTTPS health and login')
        for attempt in range(18):
            try:
                smoke(operations)
                break
            except (urllib.error.URLError, TimeoutError, RuntimeError, ValueError):
                if attempt == 17:
                    raise RuntimeError('HTTPS health/login checks failed; RELEASE was not advanced') from None
                time.sleep(10)
        # No automatic image rollback: Flyway may have changed the schema.
        stage('recording successful release')
        release = BASE / 'RELEASE.next'
        release.write_text(tag + '\n', encoding='utf-8')
        active = BASE / 'current.next'
        if active.is_symlink():
            active.unlink()
        active.symlink_to(ROOT, target_is_directory=True)
        active.replace(BASE / 'current')
        shutil.rmtree(candidate_env)
        (BASE / 'ATTEMPT').unlink()
        release.replace(current)
        print('Release passed HTTPS health and login checks: ' + tag)


if __name__ == '__main__':
    os.umask(0o077)
    try:
        deploy(sys.argv[1], sys.argv[2])
    except Exception as error:
        # Exceptions may contain response bodies; keep the diagnostic safe and actionable.
        print('Deployment failed during ' + STAGE + ' (' + type(error).__name__ + '). '
              'Inspect ATTEMPT, container health, '
              'and the Flyway compatibility notes before manual recovery.', file=sys.stderr)
        sys.exit(1)
