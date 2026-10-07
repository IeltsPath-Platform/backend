# Game Service

`game-service` runs on port `8087` and uses its own `game_db` in the root Compose PostgreSQL instance (host port `5440`). Gateway routes
`/api/games/**` and `/ws/games/**` here.

Before starting a room or session, Game requests an immutable content snapshot via
`POST /internal/game-content/snapshots`. It chooses the owner from `learningDomain`: `VOCABULARY` calls Library
Service; `GRAMMAR` calls Content Service. Both use the same
[snapshot contract](../../docs/contracts/game-content-snapshot-v1.md). Game forwards its authenticated bearer
token and `X-Correlation-Id`.

The root Compose stack includes Config Server, Content, Library and Game; container URLs use the service names. For
Java running on the host, `LIBRARY_SERVICE_URL` defaults to `http://localhost:8081` in Config Server and
`CONTENT_SERVICE_URL` must be set to `http://localhost:8082`.

Run the full stack with `docker compose up -d --build`; for host development start PostgreSQL using
`docker compose up -d postgres` and set `GAME_DB_URL=jdbc:postgresql://localhost:5440/game_db` plus the local
`GAME_DB_PASSWORD`. Verify with `mvn -q -pl services/game-service -am test`; persistence tests need Docker.
