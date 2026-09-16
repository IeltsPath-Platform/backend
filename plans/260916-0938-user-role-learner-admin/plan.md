---
title: "Kế hoạch reset user-service về LEARNER và ADMIN"
description: "Reset schema và code user-service cho codebase sạch, chỉ giữ migration V1 và hai role LEARNER, ADMIN."
status: in-progress
priority: P1
branch: main
tags: [refactor, backend, database, api, auth]
blockedBy: []
blocks: [260916-0849-user-service-hardening]
created: 2026-09-16
---

# Kế hoạch reset user-service về LEARNER và ADMIN

## Scope đã chốt

Đây là codebase sạch. Không migration, backup, compatibility, hay giữ account
`DOCTOR`/`PATIENT` cũ. Reset schema là hành động có chủ đích: database local/dev
cũ phải được drop và tạo lại trước khi chạy app.

Chỉ giữ một migration `V1__create_user_tables.sql`. V1 phải tạo đủ schema thực
tế mà app đang dùng: `roles`, `users`, `user_roles`, `refresh_tokens`, foreign
keys/indexes, `password_hash NOT NULL`, status hợp lệ và seed `ADMIN`, `LEARNER`.
Không giữ Keycloak mapping, doctor seed, patient seed hay profile outbox.

## Trạng thái triển khai — 2026-09-16

- Đã hoàn thành phần source cho Phase 1 và 2: reset migration về V1, role
  `LEARNER`/`ADMIN`, loại bỏ runtime profile và siết JWT/gateway headers.
- Đã biên dịch thành công các module liên quan bằng
  `mvn -pl services/user-service,infra/api-gateway -am compile -DskipTests`.
- Theo yêu cầu hiện tại, không sửa/chạy test và không sửa/chạy Docker Compose.
  Việc khởi tạo database sạch qua Flyway và dọn Docker environment variables
  được hoãn lại; kế hoạch vẫn ở trạng thái `in-progress` cho đến khi các bước
  này được thực hiện.

## Quyết định thiết kế

1. `RoleName` chỉ còn `ADMIN`, `LEARNER`; default registration/create user là
   `LEARNER`. `RoleName.from()` tiếp tục reject mọi giá trị khác.
2. Public registration chỉ chấp nhận role rỗng hoặc `LEARNER`; không thể tạo
   `ADMIN`. Các field chỉ phục vụ patient/doctor profile bị bỏ khỏi request và
   command; không cần compatibility shim.
3. Xóa toàn bộ runtime profile: REST client, ports, outbox entity/repository/
   worker, exception, `patientId` trên response, config và Docker variables.
4. Gateway và user-service chỉ chuyển claim canonical thành authority; token
   `PATIENT`/`DOCTOR` bị từ chối thay vì được forward.

## Phases

| Phase | Mục tiêu | Ưu tiên | Phụ thuộc |
|---|---|---:|---|
| 1 | [Reset schema V1](./phase-01-reset-schema-v1.md) | P1 | — |
| 2 | [Rút gọn runtime và API](./phase-02-runtime-api-roles.md) | P1 | 1 |
| 3 | [Kiểm thử và khởi tạo môi trường sạch](./phase-03-test-clean-start.md) | P1 | 1, 2 |

## Quan hệ plan

Plan hardening cũ giả định `PATIENT`/`DOCTOR` và profile provisioning, nên bị
plan này thay thế ở toàn bộ phần role/profile.

| Quan hệ | Plan | Lý do |
|---|---|---|
| Blocks | [Hardening user-service](../260916-0849-user-service-hardening/plan.md) | Các quyết định role/profile cũ không còn hiệu lực. |

## Tiêu chí hoàn thành

- Chỉ còn `V1__create_user_tables.sql`; V1 tạo schema đầy đủ cho JPA và refresh token.
- `rg` không còn `PATIENT`, `DOCTOR`, `PENDING_PROFILE`, profile provisioning
  trong user-service và config (trừ lịch sử/plan không thuộc runtime). Docker
  Compose được hoãn theo yêu cầu hiện tại.
- API/JWT mới chỉ trả và nhận `LEARNER`/`ADMIN`; learner self-service, admin quản trị.
- Database mới hoàn toàn boot qua Flyway, app start và test pass.

## Không làm

- Không chuyển dữ liệu database đã tồn tại.
- Không giữ `patientId`, doctor/patient service URL, profile PII/outbox audit.
- Không version API hay hỗ trợ client payload legacy.

## Hướng dẫn reset môi trường

Trước khi chạy migration mới, xóa database/volume local cũ và khởi tạo lại.
Không chạy plan này lên môi trường cần bảo toàn dữ liệu.
