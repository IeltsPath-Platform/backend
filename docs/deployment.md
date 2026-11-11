# Production deployment

Prepared on 2026-11-11. This repository contains deployment automation; it has **not** been deployed or validated on a VPS. DNS, GitHub environments/secrets, GHCR publishing, server provisioning, TLS, CDN setup, browser checks, and an actual database restore remain operator work. No production credentials belong in Git.

The backend runs on one Linux VPS with Docker Engine, Compose **2.30.0 or later**, Python 3.10+, OpenSSH client/server, tar, and Bash. Application images are published to GHCR. Caddy terminates HTTPS at `api.<domain>`; the SPA uses `app.<domain>` on Vercel or Cloudflare Pages. A separate SSH backup machine is required at `<backup-target>` (`backup-user@backup-host:/dedicated/absolute/directory`), with Python 3 and an encrypted private filesystem. Only Caddy publishes host ports 80 and 443. Permit SSH only from approved administration/runner addresses where possible.

## Before the first deployment

1. Merge secret-fallback removal before any production publish. Generate fresh, independent JWT keys, database passwords, Redis and RabbitMQ passwords. Values previously used locally or present in Git history must never become production credentials.
2. Choose the VPS, domain, and backup target. Point DNS for `api.<domain>` at the VPS and configure the CDN custom domain `app.<domain>`. Caddy requires inbound 80/443 and outbound certificate-authority access. No DNS changes are performed by these workflows.
3. Provision Docker, Compose, Python, OpenSSH, and a dedicated deploy account. The workflow writes root-owned files below `/opt/ieltspath` using noninteractive sudo for `install`, `tar`, and `python3`. **This is root-equivalent access**, as is Docker daemon membership. Use a dedicated server and a dedicated SSH key; control access through required deployment review. Do not describe a sudo rule that can execute Python or extract arbitrary files as a restricted account. An operator may replace this transport with a reviewed, root-owned deployment receiver if stricter command restrictions are required.
4. As root on the VPS, configure a read-only GHCR token using `docker login ghcr.io --password-stdin`. The deploy workflow checks image existence on the runner; pulls on the VPS use its separately provisioned registry credentials. Keep `/root/.docker` private. Credentials never appear in image build arguments.
5. Provision a **separate** backup SSH key on the VPS and pin the backup host key in root's `known_hosts` using a trusted out-of-band fingerprint. Create the target as an empty, dedicated private directory. The first backup creates `.ieltspath-backup-root`; a nonempty unmarked directory is rejected. Ensure the backup user cannot read other users' files. Test connectivity without copying production data.
6. Create the GitHub environment `production` with required reviewers and deployment branch restrictions. These settings are configured in GitHub; YAML alone cannot enforce required reviewers. Add `DEPLOY_HOST` and `DEPLOY_USER` environment variables. Add secrets `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS` (the independently verified VPS host key, **not** `ssh-keyscan` output), and `PRODUCTION_CONFIG_JSON`.
7. Start from [`config.example.json`](../scripts/production/config.example.json), populate its blank fields privately, and store the whole JSON as `PRODUCTION_CONFIG_JSON`. Never commit the populated file. PostgreSQL, Redis, RabbitMQ, and nine database passwords require at least 32 URL-safe characters; JWT secrets must be different Base64 strings encoding at least 32 random bytes. Optional LLM/Speech/storage fields may remain blank; unsupported features then follow their existing unavailable/fallback behavior. This deployment workflow never calls an LLM.

The JSON secret must fit GitHub's secret size limit. It is a flat map of string values. No embedded newline, CR, or NUL is accepted in service env values. All supplied values are masked before rendering. Only the renderer's explicit allowlist is emitted; adding an arbitrary key to the JSON does not inject it into containers.

## Image publishing and provenance

[`images.yml`](../.github/workflows/images.yml) is triggered only by a completed `CI` workflow for a `push` to this repository's `main` branch. It requires success and checks out **the CI run's `head_sha`**, including for production-config validation. It builds three infrastructure images and nine business-service images. Pull requests and forks do not receive package-write execution through this workflow.

Images use `ghcr.io/<owner>/ieltspath-<module>:<full-commit-sha>`. An existing SHA image is reused rather than rebuilt. Treat these tags as immutable: restrict package writers and never manually overwrite them. GHCR tags are not a registry-enforced immutability policy. The `main` alias moves only when the built commit still matches current `main`; deployment always requires a full SHA. Each matrix image can publish independently, so `main` aliases can temporarily be mixed while a build is running. Deploy checks that all twelve SHA images exist.

Both Java runtime Dockerfiles use UID/GID 10001 and default to `-XX:MaxRAMPercentage=75`. Config Server embeds `config-repo` and uses `file:/app/config-repo`; there is no production configuration bind mount. Changes to config-repo require a new image release. Maven's cache mount and GitHub layer cache reduce repeated downloads. The local `SKIP_GO_OFFLINE` argument remains accepted for existing callers.

## Production Compose and environment isolation

Use the launcher for **every production Compose command**:

```bash
export IMAGE_OWNER=<ghcr-owner>
export IMAGE_TAG=<full-40-character-sha>
python3 scripts/production/compose.py config --quiet
python3 scripts/production/compose.py ps
```

It explicitly selects the two Compose files and `--env-file /dev/null` (Windows `nul` for validation), strips inherited configuration variables and Compose profiles, and supplies fixed nonsecret sentinels for the base file's six required interpolation variables. Compose interpolates each file **before merging**, so `!reset` alone cannot remove a base file's required-variable checks. The production override resets inherited environment, build, ports, and application bind mounts, and completely replaces inherited env-file lists. Every container receives only `/opt/ieltspath/env/<service>.env` with `format: raw`, so dollar signs and quotes in secret values are literal. `PRODUCTION_ENV_ROOT` changes that directory for isolated tests. Never run a raw base-plus-override command that silently loads the root `.env`.

Local `docker-compose.yml` behavior is unchanged. Production uses the same named data volumes; **never reuse a local volume** as a new production cluster. The PostgreSQL production init script runs only on an empty volume and creates nine `*_app` roles, each owning and able to connect only to its database. It does not retrofit existing superuser-owned data or rotate existing roles. Use a reviewed ownership/role migration for an existing cluster; never remove a production volume to make init rerun.

All env files and `operations.json` are written with `umask 077`, directory mode 700 and file mode 600; remote installation runs as root. `operations.json` is not loaded into any container. It holds the backup target and login-smoke credentials. Runtime environment variables remain visible to root/Docker administrators by design. Do not print `compose config`, `docker inspect`, environment-file contents, login response bodies, or database role definitions into shared logs. `config --quiet` and the tests avoid printing credentials.

| Credential | Receivers |
| --- | --- |
| External JWT | Gateway, User |
| Internal JWT | Gateway and all nine business services |
| Each service DB password | That service and PostgreSQL init only |
| PostgreSQL administrator password | PostgreSQL only; backup uses the local container socket |
| RabbitMQ credentials | Broker, Access, Assessment, Learning, Notification, Community |
| Redis password | Redis, Gateway, User, Game, Notification, Community |
| Supabase secret | User, Content, Library, Assessment, Learning, Community |
| Each LLM key | Its own service only; Content receives its own explicit configuration |
| Azure Speech key | Learning only |
| Grafana admin password | Grafana only, when enabled |
| Smoke account credentials and backup target | Root-only `operations.json`; no containers |

User cookies are secure, HTTP-only by the existing default, `SameSite=Lax`, host-only on `api.<domain>`, with the existing `/auth` refresh path. Gateway, game WebSocket, and notification origins use `https://app.<domain>`. Preview/third-party origins are rejected by configuration; do not put wildcard origins in production. Demo callbacks are disabled and API documentation is disabled.

## First bootstrap and manual deployment

With demo data disabled, an empty database has **no smoke account**. Provision a dedicated normal CUSTOMER through the existing public `/api/users/register` endpoint after the first stack start, using a private request-body file and the same strong password stored in `SMOKE_LOGIN_PASSWORD`. Do not create an admin or enable demo callbacks for the smoke test. Initial administrator provisioning is a separate operator decision using existing user-management procedures.

For a new empty VPS, trigger the protected workflow and let it obtain an acknowledged empty/bootstrap backup **before** starting containers. Once gateway routing is healthy, provision that CUSTOMER smoke account through HTTPS with a private request-body file. If the login gate has already timed out, rerun the same SHA after provisioning. The first attempt stays unaccepted until real login succeeds; there is no bypass for that check and no automatic account creation. Pass secrets through stdin or private files, never command-line literals. If rendering env files manually for recovery, use `umask 077` and `render-env.py` with private JSON on stdin.

Trigger **Deploy production** manually with an already published SHA; a blank input selects current main. It verifies successful push/main CI for that exact commit and all twelve images, transfers a private reviewed release, then obtains the VPS lock. Before installing new environment files it backs up all existing databases **and old environment files** off-host. A backup failure stops deployment. On a truly empty first run, an explicit empty/bootstrap backup manifest is uploaded and acknowledged, still testing the off-host destination. Existing stopped containers or an existing PostgreSQL volume cannot be treated as an empty bootstrap; bring the existing database up safely and obtain its real backup first.

Next the workflow installs the service files, validates Compose, pulls images, and waits up to ten minutes for health. It retries gateway HTTPS health and a real login while Eureka routes settle. It checks the access token and secure refresh cookie without logging either. Only success atomically advances `/opt/ieltspath/RELEASE` and `/opt/ieltspath/current`, and writes the SHA into the workflow summary.

`ATTEMPT` identifies a failed attempted stack replacement. A failure can leave containers or env files on the new release while `RELEASE` remains the previous successful SHA. Inspect container health and Flyway state privately before recovery. The failed incoming directory may retain private env files for diagnosis; root must remove those after recovery. Old environments are preserved privately as `env-before-<sha>-<timestamp>` for operator recovery; remove them after the retention period required by your operations policy. Never copy them into Git.

The provisional container limits total approximately 8 GiB before the OS and filesystem cache. They are **not measured production sizing**. A 12 GiB VPS is a safer initial allocation; an 8 GiB machine needs empirical adjustment before acceptance. Observability adds approximately 1.8 GiB. Record `docker stats --no-stream`, OOM events, JVM heap pressure, upload behavior, and normal concurrency after the first real test, then adjust limits from evidence.

## Backups and restoration

Every pre-deploy backup runs `pg_dump -Fc` for all nine databases, retaining role-independent object ownership. It also stores root-only env files needed to recreate roles/integrations. The entire snapshot is streamed over SSH to the separate host and the sender waits for success before continuing. Both locations keep the newest completed snapshot from each of **seven UTC calendar days and four Sundays**. Repeated deploys on one day replace that day's retained copy after acknowledgement, rather than evicting older calendar days. Failed/incomplete directories are not counted or silently deleted; inspect and clean these separately. Backup copies on the VPS do not provide disaster recovery without the successful off-host acknowledgement. The destination filesystem must provide encryption at rest because backups contain both personal data and credentials.

Install a root cron job at a quiet UTC time, using the successful release's scripts, a configured owner, and the `RELEASE` SHA:

```cron
15 2 * * * cd /opt/ieltspath/current && IMAGE_OWNER=<ghcr-owner> IMAGE_TAG=$(cat /opt/ieltspath/RELEASE) /usr/bin/python3 scripts/production/backup.py >> /var/log/ieltspath-backup.log 2>&1
```

Monitor nonzero exits and off-host arrival; rotate the operational log using the host's existing logging tools. PostgreSQL dumps are individually consistent, **not a cross-database distributed transaction**. For a release that requires an exact cross-service recovery point, put the app in maintenance and stop writers before taking the dump. RabbitMQ message volumes are not included in these database dumps; outbox replays and consumer idempotency need verification after disaster recovery. Redis tickets, sessions/rate-limit state, and notification pub/sub are transient.

For a restore, first take and verify a fresh off-host backup of current state, isolate traffic, and stop **all writers relevant to the incident**, including cross-service workflows. Download the selected `.dump` privately and verify its manifest/release. The restore command additionally stops the owning service, uses `--single-transaction`, and leaves the service stopped:

```bash
export IMAGE_OWNER=<ghcr-owner>
export IMAGE_TAG=$(cat /opt/ieltspath/RELEASE)
python3 scripts/production/restore.py user_db /secure/user_db.dump --confirm-restore user_db
```

This overwrites the selected database's objects. Review schema/data and Flyway history, then start the compatible service with the launcher and verify application behavior before reopening traffic. To rebuild a lost VPS, initialize roles on an empty cluster with the recovered env files, restore every database under the matching `*_app` role, and test queued/outbox reconciliation. Run an actual restore drill on an isolated machine and record its result before accepting production readiness; unit tests do not prove restoration.

## Rollback and key rotation

To roll back compatible code, rerun the protected workflow with the previous full SHA. It backs up the current databases before applying that image. **Flyway does not roll back schemas.** Old code must support the current schema. Destructive migrations, including removal of unused outbox tables, are an explicit compatibility boundary: recover from a compatible backup or roll forward after operator review. There is no automatic rollback that pretends to undo database changes.

For internal JWT rotation, update the one JSON key and redeploy Gateway plus all downstream services together; transient requests can fail during this single-VPS replacement. External JWT rotation signs users out and requires login again. For Supabase and LLM keys, create the replacement, deploy it to the receiving services, verify behavior, then revoke the old key; refreshing Spring config does not rebuild existing storage HTTP clients. For database credentials, changing the env JSON alone does **not** alter an existing role: coordinate `ALTER ROLE` privately with deployment or introduce temporary separately reviewed credentials. RabbitMQ users and Redis authentication also need a coordinated maintenance/change procedure; changing an init env var does not reset persisted RabbitMQ users. Keep prior private env files only as long as the recovery policy permits.

## Optional observability

Set `OBSERVABILITY_ENABLED` to string `true` and provide `GRAFANA_ADMIN_PASSWORD` in the private JSON. Deployment then loads the existing observability file plus [`docker-compose.prod-observability.yml`](../docker-compose.prod-observability.yml). For manual commands, use `compose.py --observability`. Every observability container gets its own env file; no monitoring service publishes a host port, including Grafana. Caddy remains the only published ingress. Access Grafana with an SSH tunnel to its Docker bridge IP on port 3000. Restrict that tunnel to administrators; do not expose the container IP publicly. Alloy's read-only Docker socket mount is inherited from the current observability design and still gives broad Docker metadata visibility; host administration is trusted.

## Frontend CDN

Use the frontend repository as the CDN project root. Build with `npm ci && npm run build`, output `dist`, and configure public build variables `VITE_API_BASE_URL=https://api.<domain>`, `VITE_USE_MOCK_LEARNING=false`, and `VITE_USE_MOCK_ROCKET=false`. These variables are bundled and must never contain credentials. Use only the custom `app.<domain>` origin for production; provider preview domains have different cookie-site behavior.

For Vercel, [`vercel.json`](../../frontend/vercel.json) supplies the [documented Vite SPA rewrite](https://vercel.com/docs/frameworks/frontend/vite#using-vite-to-make-spas). For Cloudflare Pages, place the provided `_redirects` content (`/* /index.html 200`) in `frontend/public/_redirects` before the build, following its [relative proxy rule](https://developers.cloudflare.com/pages/configuration/redirects/#proxying); the staged example is named `cloudflare-pages-redirects.example` to avoid selecting a provider implicitly. Do not install both provider-specific alternatives without reviewing the chosen CDN's behavior.

Before acceptance, test Chrome and Safari login, access-token expiration/refresh, logout cookie removal, rejection of an unrelated Origin, and `/ws/games/**` through HTTPS. Inspect the built bundle for accidental credentials and verify direct navigation to a client route. Set the production API base URL in the mobile build separately; native clients do not use browser CORS.

## Local verification and remaining acceptance

Run `python3 scripts/production/test-production.py`. It exercises actual Compose merging with ephemeral credentials and a deliberately failing root `.env`, checks port/build/mount/env isolation (including optional observability), credential scope, raw env quoting, input rejection, backup retention, and the no-database backup guard. It starts no containers and makes no network/LLM requests.

Still required: build all images with nonproduction credentials; inspect runtime UID and image contents; start the complete production stack locally; verify Flyway under service roles, login, submitted work, and WebSocket upgrade; measure RAM; publish twelve GHCR images only after green CI; then separately validate VPS TLS/ports, off-host backup/restore, key rotation, rollback compatibility, and CDN/browser cookie behavior. None of those live checks are claimed by the configuration tests.

Compose merge and raw env behavior follow [Docker's merge reference](https://docs.docker.com/reference/compose-file/merge/) and [env_file reference](https://docs.docker.com/reference/compose-file/services/#env_file). Exact triggering/checkout behavior follows [GitHub workflow_run documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_run).
