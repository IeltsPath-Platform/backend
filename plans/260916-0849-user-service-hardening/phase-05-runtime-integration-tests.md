---
phase: 5
title: "Cấu hình, container và kiểm thử tích hợp"
status: pending
priority: P1
dependencies: [1, 2, 3, 4]
---

# Phase 5: Cấu hình, container và kiểm thử tích hợp

## Overview

Làm local/CI boot được user-service không dựa vào `localhost` sai trong
container, đưa secrets ra environment và thêm verification end-to-end.

## Related Code Files

- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\main\resources\application.yml`
- Create: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\infra\config-server\config-repo\user-service.yaml`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\docker-compose.yml`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\pom.xml`
- Create/Modify integration tests under: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\src\test\java\com\group01\user\`
- Modify documentation only if run commands change: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\README.md`

## Implementation Steps

1. Sửa Config Server URI mặc định từ port 8889 sang 8888. Local profile có thể
   override, container dùng `http://config-server:8888`.
2. Thêm `user-service.yaml` với datasource/Flyway/JPA/Eureka/profile provider
   URLs và Actuator health. JWT secret, service credential và DB password dùng
   environment placeholders, không commit giá trị default bí mật.
3. Thêm Compose `user-service`: build context root, port, required environment,
   dependencies/health checks cho Postgres, Config Server, Eureka. Profile
   provider URLs phải là service DNS khi provider được compose; nếu không, nhận
   URL external explicit—không dùng `localhost` ngầm định.
4. Thêm test dependencies quản lý bởi Boot BOM: Testcontainers PostgreSQL/JUnit
   và HTTP stub tool đã chọn. `spring-security-test` đã được thêm ở Phase 1.
   Giữ mọi dependency mới ở scope test.
5. Viết integration suite: Flyway+JPA, MockMvc security matrix, token refresh
   concurrency, `RestClient` timeout/error mapping, outbox worker retry, Config
   Server property binding và Compose smoke test.
6. Thiết lập CI gates theo thứ tự: compile → unit → Testcontainers integration
   → Docker build → Compose health smoke. Không đánh dấu hoàn tất khi Docker
   unavailable; ghi rõ test nào bị skipped.

## Todo List

- [ ] Fresh checkout có thể build user-service từ root.
- [ ] Config Server trả config user-service không chứa secret plaintext.
- [ ] Container không gọi peer qua `localhost`.
- [ ] CI chặn regression quyền, migration và token replay.

## Success Criteria

- [ ] `mvn -pl services/user-service -am test` chạy unit + integration tests.
- [ ] `docker compose up --build` đưa Postgres/Config/Eureka/user-service health
  về UP với environment bắt buộc.
- [ ] Smoke test chứng minh public/protected endpoints theo Phase 2 matrix.
- [ ] README/runbook mô tả biến môi trường required và rollback migration.

## Risk Assessment

- Testcontainers cần Docker runner trong CI; provision runner trước khi bắt buộc
  gate. Không thay bằng H2 cho Postgres locking/migration behavior.
- Compose không thể chứng minh doctor/patient thật nếu service chưa ở repo; dùng
  contract stub trong CI và cấu hình external endpoint ở environment thực.
