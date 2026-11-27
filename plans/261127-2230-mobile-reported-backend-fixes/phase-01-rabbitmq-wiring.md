# Phase 1: RabbitMQ cho content, game, library (P0)

## Nguyên nhân

- PR #235 thêm `UserDeletedListener` và `spring-boot-starter-amqp` cho content-service và game-service nhưng không thêm
  cấu hình. Spring dùng mặc định `localhost:5672`, health check Rabbit `DOWN`, container không bao giờ `healthy`.
- `scripts/production/render-env.py`: `BROKER` thiếu `content`, `game` và `library` (library thiếu từ trước PR #235).
  `docker-compose.prod.yml` thay toàn bộ `environment` bằng env file do script sinh, nên production không có `RABBITMQ_*`.
- CI không bắt được vì Testcontainers tự cấu hình broker; `test-production.py` chỉ chạy sau khi merge (workflow Images).

## File sửa

- `infra/config-server/config-repo/content-service.yaml`, `game-service.yaml`: thêm dưới `spring:`
  ```yaml
    rabbitmq:
      host: ${RABBITMQ_HOST:localhost}
      port: ${RABBITMQ_PORT:5672}
      username: ${RABBITMQ_USERNAME:guest}
      password: ${RABBITMQ_PASSWORD}
  ```
- `docker-compose.yml`, `content-service` và `game-service`: thêm `RABBITMQ_HOST: rabbitmq`, `RABBITMQ_PORT: 5672`,
  `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`; thêm `rabbitmq: { condition: service_healthy }` vào `depends_on`
  (content đang dùng `depends_on: *after-infra`, phải đổi thành map có `<<: *after-infra`).
- `scripts/production/render-env.py`: thêm `content`, `game`, `library` vào `BROKER`.

Không sửa tay `/opt/ieltspath/env/*.env` trên server: mỗi lần deploy các file này được sinh lại từ secret.

## Chặn tái diễn

- Test mới `infra/api-gateway/src/test/java/com/ieltspath/apigateway/RabbitMqWiringTest.java` (cùng kiểu đọc file như
  `ServiceDependencyGraphTest`). Với mọi `services/*/pom.xml` có `spring-boot-starter-amqp`, kiểm tra:
  - config-repo của service có `spring.rabbitmq.host` và `password`;
  - `docker-compose.yml` truyền `RABBITMQ_HOST`, `RABBITMQ_PASSWORD` và `depends_on` có `rabbitmq`;
  - `BROKER` trong `render-env.py` có tên service.
- `scripts/production/test-production.py`: khẳng định env của content, game, library có `RABBITMQ_PASSWORD`.
- `.github/workflows/ci.yml`, job `compose`: thêm bước `python3 scripts/production/test-production.py` để chạy trước khi merge.

## Kiểm tra

- Viết test trước, chạy để thấy fail đúng ba service, sửa, chạy lại qua.
- `docker compose config --quiet`; `python3 scripts/production/test-production.py`.
- `docker compose up -d --build`: mọi service `healthy`; log content-service và game-service không còn `Connection refused`.
- Xoá một tài khoản test, kiểm tra content-service và game-service nhận `user.deleted.v1`.
- Báo bên mobile xoá `docker-compose.override.yml` tạm của họ.
