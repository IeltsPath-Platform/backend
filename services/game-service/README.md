# Game Service

`game-service` runs on port `8087` and uses `game_db` (Compose `game-db`, host port `5435`). Gateway routes
`/api/games/**` and `/ws/games/**` here.

Before starting a room or session, Game requests an immutable content snapshot via
`POST /internal/game-content/snapshots`. It chooses the owner from `learningDomain`: `VOCABULARY` calls Library
Service; `GRAMMAR` calls Content Service. Both use the same
[snapshot contract](../../docs/contracts/game-content-snapshot-v1.md). Game forwards its authenticated bearer
token and `X-Correlation-Id`.

For Java running on the host, `LIBRARY_SERVICE_URL` defaults to `http://localhost:8081` in Config Server.
`CONTENT_SERVICE_URL` instead defaults to `http://content-service:8082` there, so set
`CONTENT_SERVICE_URL=http://localhost:8082` for host execution. The Compose `game-service` container refers to
`http://config-server:8888`, but that container is absent from Compose; run Game on the host.

Start `game-db` with `docker compose up -d game-db` (root `.env` needs `GAME_DB_PASSWORD` and the other variables
required by Compose). Verify with `mvn -q -pl services/game-service -am test`; persistence tests need Docker.
