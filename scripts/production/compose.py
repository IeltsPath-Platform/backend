#!/usr/bin/env python3
"""Production-only Compose entry point; never reads a repository .env file."""
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
SERVICES = ('user', 'content', 'library', 'assessment', 'access', 'learning', 'game', 'notification', 'community')


def command(tag=None, owner=None, env_root=None, observability=False):
    tag = tag or os.environ.get('IMAGE_TAG', '')
    owner = owner or os.environ.get('IMAGE_OWNER', '')
    if not re.fullmatch(r'[a-f0-9]{40}', tag):
        raise ValueError('IMAGE_TAG must be a full lowercase commit SHA')
    if not re.fullmatch(r'[a-z0-9][a-z0-9-]*', owner):
        raise ValueError('IMAGE_OWNER must be a lowercase GHCR owner')
    # Base interpolation happens BEFORE merge. These values are discarded by !reset;
    # no real credential is available to base interpolation, even from the caller.
    env = {k: os.environ[k] for k in (
        'PATH', 'HOME', 'USERPROFILE', 'SYSTEMROOT', 'PROGRAMFILES', 'PROGRAMDATA',
        'APPDATA', 'LOCALAPPDATA', 'DOCKER_CONFIG', 'DOCKER_HOST',
        'DOCKER_CONTEXT', 'DOCKER_TLS_VERIFY', 'DOCKER_CERT_PATH', 'XDG_RUNTIME_DIR'
    ) if k in os.environ}
    env.update({key: 'discarded-production-interpolation' for key in (
        'POSTGRES_PASSWORD', 'RABBITMQ_USERNAME', 'RABBITMQ_PASSWORD', 'REDIS_PASSWORD',
        'EXTERNAL_JWT_SECRET', 'GATEWAY_INTERNAL_JWT_SECRET'
    )})
    env.update(IMAGE_TAG=tag, IMAGE_OWNER=owner,
               PRODUCTION_ENV_ROOT=str(env_root or os.environ.get('PRODUCTION_ENV_ROOT', '/opt/ieltspath/env')))
    args = ['docker', 'compose', '--project-name', 'ieltspath', '--env-file', os.devnull,
            '-f', str(ROOT / 'docker-compose.yml')]
    if observability:
        env['GRAFANA_ADMIN_PASSWORD'] = 'discarded-production-interpolation'
        args.extend(['-f', str(ROOT / 'docker-compose.observability.yml')])
    args.extend(['-f', str(ROOT / 'docker-compose.prod.yml')])
    if observability:
        args.extend(['-f', str(ROOT / 'docker-compose.prod-observability.yml')])
    return args, env


def run(*args, tag=None, owner=None, env_root=None, observability=False, **kwargs):
    prefix, env = command(tag, owner, env_root, observability)
    return subprocess.run(prefix + list(args), cwd=ROOT, env=env, check=True, **kwargs)


if __name__ == '__main__':
    try:
        # Keep optional development observability profiles out of production.
        if any(a in ('--profile', '--build', 'build') for a in sys.argv[1:]):
            raise ValueError('Production launcher forbids builds and development profiles')
        observability = '--observability' in sys.argv
        args = [a for a in sys.argv[1:] if a != '--observability']
        run(*args, observability=observability)
    except (ValueError, subprocess.CalledProcessError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
