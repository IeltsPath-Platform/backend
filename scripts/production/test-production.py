#!/usr/bin/env python3
"""Local checks use ephemeral credentials and never start containers or call an LLM."""
import base64
from datetime import datetime, timedelta
import importlib.util
import io
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import tempfile
import tarfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

HERE = Path(__file__).resolve().parent


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


renderer = load('renderer', 'render-env.py')
compose = load('production_compose', 'compose.py')
backup_module = load('production_backup', 'backup.py')
# Deploy is Linux-only. Windows can still test its orchestration boundaries.
with patch.dict(sys.modules, {'fcntl': SimpleNamespace(LOCK_EX=1, LOCK_NB=2, flock=lambda *args: None)}):
    deployment = load('production_deployment', 'deploy.py')


def configuration():
    config = json.loads((HERE / 'config.example.json').read_text())
    config.update(API_HOST='api.example.invalid', FRONTEND_ORIGIN='https://app.example.invalid',
                  BACKUP_TARGET='backup@backup.example.invalid:/snapshots/ieltspath',
                  SMOKE_LOGIN_EMAIL='smoke@example.invalid', SMOKE_LOGIN_PASSWORD=secrets.token_urlsafe(32),
                  RABBITMQ_USERNAME='ieltspath')
    for key in config:
        if key.endswith('_PASSWORD'):
            config[key] = secrets.token_urlsafe(32)
        if key.endswith('_JWT_SECRET'):
            config[key] = base64.b64encode(secrets.token_bytes(32)).decode()
    return config


class ProductionEnvironmentTest(unittest.TestCase):
    def test_failed_predeploy_backup_prevents_container_changes_and_release_advance(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            base = root / 'installed'
            base.mkdir()
            (base / 'RELEASE').write_text('b' * 40 + '\n')
            renderer.render(configuration(), root / 'private-env')
            with patch.object(deployment, 'BASE', base), patch.object(deployment, 'ROOT', root), \
                    patch.object(deployment.os, 'geteuid', return_value=0, create=True), \
                    patch.object(deployment, 'backup', side_effect=RuntimeError('Off-host backup failed')), \
                    patch.object(deployment, 'run') as docker:
                with self.assertRaises(RuntimeError):
                    deployment.deploy('a' * 40, 'owner')
                docker.assert_not_called()
            self.assertEqual((base / 'RELEASE').read_text().strip(), 'b' * 40)
            self.assertFalse((base / 'env').exists())

    def test_remote_receiver_accepts_flat_snapshots_and_rejects_path_traversal(self):
        import sys
        for name, accepted in [('manifest.json', True), ('../escape', False)]:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as directory:
                archive_bytes = io.BytesIO()
                with tarfile.open(fileobj=archive_bytes, mode='w') as archive:
                    member = tarfile.TarInfo(name)
                    payload = b'{}'
                    member.size = len(payload)
                    archive.addfile(member, io.BytesIO(payload))
                target = Path(directory) / 'offhost'
                result = subprocess.run([sys.executable, '-c', backup_module.REMOTE,
                                         str(target), '20261111000000', 'weekly'],
                                        input=archive_bytes.getvalue(), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                self.assertEqual(result.returncode == 0, accepted)
                self.assertFalse((Path(directory) / 'escape').exists())
                if accepted:
                    self.assertTrue((target / 'daily/20261111000000/.complete').is_file())
                    self.assertTrue((target / 'weekly/20261111000000/manifest.json').is_file())

    def test_backup_retention_counts_only_complete_snapshots(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for category, total in [('daily', 10), ('weekly', 6)]:
                for number in range(total):
                    day = datetime(2026, 8, 2) + timedelta(days=number * (7 if category == 'weekly' else 1))
                    snapshot = root / category / day.strftime('%Y%m%d%H%M%S')
                    snapshot.mkdir(parents=True)
                    (snapshot / '.complete').touch()
                # Multiple deploys on one day must not evict previous calendar days.
                duplicate = root / category / (day.strftime('%Y%m%d') + '235959')
                duplicate.mkdir()
                (duplicate / '.complete').touch()
                (root / category / '9999').mkdir()  # Incomplete copies are never promoted.
            backup_module.rotate(root)
            self.assertEqual(len(list((root / 'daily').glob('*/.complete'))), 7)
            self.assertEqual(len(list((root / 'weekly').glob('*/.complete'))), 4)
            self.assertTrue((root / 'daily' / '9999').is_dir())

    def test_missing_database_blocks_backup_before_any_offhost_command(self):
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(backup_module, 'BASE', Path(directory)), \
                patch.object(backup_module, 'run', return_value=SimpleNamespace(stdout=b'')), \
                patch.object(backup_module.subprocess, 'Popen') as remote:
            with self.assertRaises(RuntimeError):
                backup_module.backup(configuration(), 'a' * 40, 'owner', Path(directory) / 'env')
            remote.assert_not_called()

    def test_credential_scope_and_secure_cookie(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / 'env'
            renderer.render(configuration(), root)
            def keys(name):
                return dict(line.split('=', 1) for line in (root / (name + '.env')).read_text().splitlines())
            gateway, user, content = keys('api-gateway'), keys('user-service'), keys('content-service')
            self.assertNotIn('USER_DB_PASSWORD', gateway)
            self.assertNotIn('SUPABASE_SECRET_KEY', gateway)
            self.assertNotIn('EXTERNAL_JWT_SECRET', content)
            self.assertNotIn('USER_DB_PASSWORD', content)
            self.assertNotIn('LEARNING_LLM_API_KEY', content)
            self.assertNotIn('GATEWAY_INTERNAL_JWT_SECRET', keys('config-server'))
            self.assertEqual(user['APP_AUTH_COOKIE_SECURE'], 'true')
            self.assertEqual(user['APP_AUTH_COOKIE_DOMAIN'], '')
            self.assertEqual(user['DEMO_DATA_ENABLED'], 'false')
            self.assertEqual(user['USER_DB_USERNAME'], 'user_app')
            self.assertIn('RABBITMQ_PASSWORD', user)
            self.assertNotIn('RABBITMQ_PASSWORD', gateway)
            if os.name != 'nt':
                self.assertEqual((root / 'user-service.env').stat().st_mode & 0o777, 0o600)

    def test_rejects_cross_site_origin_and_config_injection(self):
        for key, value in [('FRONTEND_ORIGIN', 'https://preview.example.invalid'),
                           ('REDIS_PASSWORD', 'password\nrequirepass injected'),
                           ('USER_DB_PASSWORD', '')]:
            with self.subTest(key=key), tempfile.TemporaryDirectory() as directory:
                config = configuration()
                config[key] = value
                with self.assertRaises(ValueError):
                    renderer.render(config, Path(directory) / 'env')
        with tempfile.TemporaryDirectory() as directory:
            config = configuration()
            config['CONTENT_DB_PASSWORD'] = config['USER_DB_PASSWORD']
            with self.assertRaises(ValueError):
                renderer.render(config, Path(directory) / 'env')

    def test_launcher_discards_caller_secrets_and_rejects_mutable_tags(self):
        with patch.dict(os.environ, POSTGRES_PASSWORD=secrets.token_urlsafe(32), COMPOSE_PROFILES='observability'):
            args, env = compose.command('a' * 40, 'owner')
            self.assertEqual(env['POSTGRES_PASSWORD'], 'discarded-production-interpolation')
            self.assertNotIn('COMPOSE_PROFILES', env)
            self.assertIn(os.devnull, args)
        with self.assertRaises(ValueError):
            compose.command('main', 'owner')

    def test_actual_compose_merge_ignores_root_dotenv_and_removes_local_contracts(self):
        source = Path(os.environ.get('PRODUCTION_BASE_COMPOSE', compose.ROOT / 'docker-compose.yml'))
        self.assertTrue(source.is_file(), 'Set PRODUCTION_BASE_COMPOSE to the existing local Compose file')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            shutil.copyfile(source, root / 'docker-compose.yml')
            shutil.copyfile(compose.ROOT / 'docker-compose.prod.yml', root / 'docker-compose.prod.yml')
            shutil.copyfile(source.parent / 'docker-compose.observability.yml', root / 'docker-compose.observability.yml')
            shutil.copyfile(compose.ROOT / 'docker-compose.prod-observability.yml', root / 'docker-compose.prod-observability.yml')
            (root / '.env').write_text('POSTGRES_PASSWORD=${MUST_NOT_READ:?ROOT_DOTENV_WAS_READ}\n')
            config = configuration()
            # Dollar and quote characters must survive raw env_file without interpolation.
            config['SUPABASE_SECRET_KEY'] = secrets.token_urlsafe(32) + '$literal#\"'
            renderer.render(config, root / 'env')
            with patch.object(compose, 'ROOT', root), patch.dict(os.environ, DOCKER_CONFIG=str(root / 'docker-config')):
                result = compose.run('config', '--format', 'json', tag='a' * 40, owner='owner',
                                     env_root=root / 'env', stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                services = json.loads(result.stdout)['services']
                for name, service in services.items():
                    self.assertNotIn('build', service)
                    self.assertEqual(service['restart'], 'unless-stopped')
                    self.assertNotIn('discarded-production-interpolation', json.dumps(service))
                    if name != 'caddy':
                        self.assertFalse(service.get('ports'), name)
                self.assertEqual({p['published'] for p in services['caddy']['ports']}, {'80', '443'})
                self.assertFalse(services['config-server'].get('volumes'))
                # `config` escapes dollars for round-trippable Compose output.
                self.assertEqual(services['content-service']['environment']['SUPABASE_SECRET_KEY'].replace('$$', '$'),
                                 config['SUPABASE_SECRET_KEY'])
                self.assertEqual(services['content-service']['environment']['DEMO_DATA_ENABLED'], 'false')
                result = compose.run('config', '--format', 'json', observability=True,
                                     tag='a' * 40, owner='owner', env_root=root / 'env',
                                     stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                services = json.loads(result.stdout)['services']
                for name, service in services.items():
                    if name != 'caddy':
                        self.assertFalse(service.get('ports'), name)
                    self.assertNotIn('discarded-production-interpolation', json.dumps(service))
                self.assertIn('grafana', services)


if __name__ == '__main__':
    unittest.main()
