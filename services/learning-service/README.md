# Learning Service

Spring Boot service on port 8086, registered in Eureka as `learning-service`.
Gateway forwards `/api/learning/**` and supplies the internal JWT validated by
`common-security`. This module currently provides startup, Flyway schema and
Actuator health; learner APIs and the assessment consumer are not implemented yet.

Runtime configuration is in `infra/config-server/config-repo/learning-service.yaml`.
PostgreSQL runs as Compose `learning-db` on `127.0.0.1:5436`, database `learning_db`.
Set `LEARNING_DB_PASSWORD`, `GATEWAY_INTERNAL_JWT_SECRET` and RabbitMQ credentials
in the local environment. The database password has no fallback.

The package layers follow the other Java services:
`api → application → domain`, with adapters under `infrastructure`.
The service owns its nine tables and does not read other services' databases.

```powershell
mvn -q -pl services/learning-service -am test
mvn -pl services/learning-service spring-boot:run
```

The context test uses Testcontainers PostgreSQL and is skipped when Docker is unavailable.
Tests supply their own configuration and do not import local environment files.
