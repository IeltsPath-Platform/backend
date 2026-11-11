#!/usr/bin/env python3
"""Read flat production configuration JSON from stdin; write private raw env files."""
import json
import os
from pathlib import Path
import re
import sys

BUSINESS = {'user': 8085, 'content': 8082, 'library': 8081, 'assessment': 8083,
            'access': 8084, 'learning': 8086, 'game': 8087, 'notification': 8088, 'community': 8089}
REDIS = {'api-gateway', 'user-service', 'game-service', 'notification-service', 'community-service'}
BROKER = {'assessment', 'access', 'learning', 'notification', 'community'}
STORAGE = {'user', 'content', 'library', 'assessment', 'learning', 'community'}
LINKS = {'content': ['library'], 'library': ['access'], 'assessment': ['content', 'user'],
         'learning': ['assessment', 'content', 'access', 'user'], 'game': ['content', 'library'],
         'community': ['user', 'learning']}
OPTIONAL = {
    'user': ['USER_AVATAR_BUCKET'],
    'content': ['CONTENT_MEDIA_BASE_URL', 'CONTENT_MEDIA_BUCKET', 'CONTENT_PREMIUM_MEDIA_BUCKET'],
    'library': ['LIBRARY_CATALOG_MEDIA_BUCKET', 'LIBRARY_FLASHCARD_BUCKET'],
    'assessment': ['ASSESSMENT_SPEAKING_BUCKET', 'ASSESSMENT_SPEAKING_RETENTION_DAYS', 'ASSESSMENT_LLM_DAILY_LIMIT'],
    'learning': ['LEARNING_SPEECH_KEY', 'LEARNING_SPEECH_REGION', 'LEARNING_SPEECH_LOCALE',
                 'LEARNING_SPEAKING_ENABLED', 'LEARNING_SPEAKING_DAILY_LIMIT', 'LEARNING_SPEAKING_POINT_COST',
                 'LEARNING_SPEAKING_BUCKET', 'LEARNING_SPEAKING_RETENTION_DAYS',
                 'LEARNING_LLM_REASONING_EFFORT', 'LEARNING_WRITING_POINT_COST', 'LEARNING_WRITING_DAILY_GRADING_LIMIT'],
    'community': ['COMMUNITY_MEDIA_BUCKET', 'COMMUNITY_IMAGE_HOSTS', 'COMMUNITY_VIDEO_HOSTS'],
}


def render(config, output):
    def get(key):
        value = config.get(key, '')
        if not isinstance(value, str) or any(c in value for c in '\r\n\0'):
            raise ValueError('Invalid single-line configuration: ' + key)
        return value

    def required(key):
        value = get(key)
        if not value or '<' in value or '>' in value:
            raise ValueError('Missing production configuration: ' + key)
        return value

    api_host = required('API_HOST')
    if not re.fullmatch(r'api\.[a-z0-9.-]+', api_host):
        raise ValueError('API_HOST must be api.<domain>, without a scheme or port')
    origin = required('FRONTEND_ORIGIN')
    if origin != 'https://app.' + api_host[4:]:
        raise ValueError('FRONTEND_ORIGIN must be https://app.<same-domain>')
    password_keys = ('REDIS_PASSWORD', 'RABBITMQ_PASSWORD', 'POSTGRES_PASSWORD',
                     *(s.upper() + '_DB_PASSWORD' for s in BUSINESS))
    for key in password_keys:
        if not re.fullmatch(r'[A-Za-z0-9_-]{32,}', required(key)):
            raise ValueError(key + ' must contain at least 32 URL-safe characters')
    if len({get(key) for key in password_keys}) != len(password_keys):
        raise ValueError('Infrastructure and service database passwords must be independent')
    if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_-]+', required('RABBITMQ_USERNAME')) or get('RABBITMQ_USERNAME') == 'guest':
        raise ValueError('Use a dedicated non-guest RabbitMQ username')
    import base64
    for key in ('EXTERNAL_JWT_SECRET', 'GATEWAY_INTERNAL_JWT_SECRET'):
        if len(base64.b64decode(required(key), validate=True)) < 32:
            raise ValueError(key + ' must encode at least 32 bytes')
    if get('EXTERNAL_JWT_SECRET') == get('GATEWAY_INTERNAL_JWT_SECRET'):
        raise ValueError('External and internal JWT keys must differ')
    monitoring = get('OBSERVABILITY_ENABLED') == 'true'
    common = {'SPRING_CLOUD_CONFIG_URI': 'http://config-server:8888',
              'EUREKA_URL': 'http://eureka-server:8761/eureka/', 'API_DOCS_ENABLED': 'false',
              'GATEWAY_INTERNAL_JWT_SECRET': required('GATEWAY_INTERNAL_JWT_SECRET'),
              'JAVA_TOOL_OPTIONS': '-XX:MaxRAMPercentage=75 -Dsun.net.inetaddr.ttl=5',
              'TRACING_ENABLED': 'true' if monitoring else 'false',
              'OTLP_TRACES_ENDPOINT': 'http://alloy:4318/v1/traces'}
    files = {}
    for name, port in BUSINESS.items():
        prefix = name.upper()
        values = dict(common, SERVER_PORT=str(port), DEMO_DATA_ENABLED='false')
        values.update({prefix + '_DB_URL': f'jdbc:postgresql://postgres:5432/{name}_db',
                       prefix + '_DB_USERNAME': name + '_app',
                       prefix + '_DB_PASSWORD': required(prefix + '_DB_PASSWORD')})
        for target in LINKS.get(name, []):
            values[target.upper() + '_SERVICE_URL'] = f'http://{target}-service:{BUSINESS[target]}'
        if name in BROKER:
            values.update(RABBITMQ_HOST='rabbitmq', RABBITMQ_USERNAME=required('RABBITMQ_USERNAME'),
                          RABBITMQ_PASSWORD=required('RABBITMQ_PASSWORD'))
        if name in STORAGE:
            values.update({k: get(k) for k in ('SUPABASE_URL', 'SUPABASE_SECRET_KEY')})
        if name in ('content', 'assessment', 'learning'):
            values.update({prefix + '_LLM_' + k: get(prefix + '_LLM_' + k)
                           for k in ('BASE_URL', 'API_KEY', 'MODEL')})
        values.update({key: get(key) for key in OPTIONAL.get(name, []) if get(key)})
        if name == 'user':
            values.update(EXTERNAL_JWT_SECRET=required('EXTERNAL_JWT_SECRET'),
                          APP_AUTH_COOKIE_SECURE='true', APP_AUTH_COOKIE_SAME_SITE='LAX',
                          APP_AUTH_COOKIE_DOMAIN='')
        if name == 'game':
            values['GAME_WEBSOCKET_ALLOWED_ORIGINS'] = origin
        if name == 'notification':
            values['FRONTEND_ORIGIN'] = origin
        files[name + '-service'] = values
    files['api-gateway'] = dict(common, SERVER_PORT='8080', FRONTEND_ORIGIN=origin,
                                EXTERNAL_JWT_SECRET=required('EXTERNAL_JWT_SECRET'))
    for name in REDIS:
        files[name].update(REDIS_HOST='redis', REDIS_PASSWORD=required('REDIS_PASSWORD'))
    files['config-server'] = {'SERVER_PORT': '8888', 'SPRING_PROFILES_ACTIVE': 'native',
                              'CONFIG_NATIVE_REPO_URI': 'file:/app/config-repo'}
    files['eureka-server'] = {'SERVER_PORT': '8761', 'SPRING_CLOUD_CONFIG_URI': 'http://config-server:8888',
                              'EUREKA_URL': 'http://eureka-server:8761/eureka/'}
    files['postgres'] = {'POSTGRES_DB': 'postgres', 'POSTGRES_USER': 'postgres',
                         'POSTGRES_PASSWORD': required('POSTGRES_PASSWORD')}
    files['postgres'].update({s.upper() + '_DB_PASSWORD': required(s.upper() + '_DB_PASSWORD') for s in BUSINESS})
    files['rabbitmq'] = {'RABBITMQ_DEFAULT_USER': required('RABBITMQ_USERNAME'),
                         'RABBITMQ_DEFAULT_PASS': required('RABBITMQ_PASSWORD')}
    files['redis'] = {'REDIS_PASSWORD': required('REDIS_PASSWORD')}
    files['prometheus'] = {}
    files['loki'] = {}
    files['tempo'] = {}
    files['alloy'] = {}
    files['grafana'] = {'GF_AUTH_ANONYMOUS_ENABLED': 'false',
                         'GF_SECURITY_ADMIN_PASSWORD': required('GRAFANA_ADMIN_PASSWORD') if monitoring else ''}
    files['caddy'] = {'API_HOST': api_host}
    output = Path(output)
    output.mkdir(mode=0o700, parents=True, exist_ok=False)
    for name, values in files.items():
        path = output / (name + '.env')
        with path.open('x', encoding='utf-8', newline='\n') as stream:
            os.chmod(path, 0o600)
            stream.write(''.join(key + '=' + value + '\n' for key, value in sorted(values.items())))
    operations = {key: required(key) for key in ('BACKUP_TARGET', 'SMOKE_LOGIN_EMAIL', 'SMOKE_LOGIN_PASSWORD')}
    operations.update(API_HOST=api_host, OBSERVABILITY_ENABLED=monitoring)
    path = output / 'operations.json'
    path.write_text(json.dumps(operations), encoding='utf-8')
    os.chmod(path, 0o600)


if __name__ == '__main__':
    os.umask(0o077)
    try:
        render(json.load(sys.stdin), sys.argv[1])
    except (ValueError, OSError) as error:
        # JSON errors do not expose input; validation errors contain key names only.
        print('Environment rendering failed: ' + str(error), file=sys.stderr)
        sys.exit(1)
