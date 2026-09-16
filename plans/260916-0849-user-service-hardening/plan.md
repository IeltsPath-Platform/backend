---
title: "Kế hoạch hardening user-service"
description: "Khôi phục khả năng build, chặn truy cập trái phép, bảo đảm token và provisioning nhất quán, rồi hoàn thiện vận hành/kiểm thử."
status: pending
priority: P1
branch: main
tags: [backend, api, auth, database, infra, critical]
blockedBy: [260916-0938-user-role-learner-admin]
blocks: []
created: 2026-09-16
---

# Kế hoạch hardening user-service

## Bối cảnh

Assessment xác nhận các lỗi P0: module chưa thuộc Maven reactor, `permitAll`,
JWT fallback secret, public registration nhận `ADMIN`, token refresh có race,
provisioning profile không nhất quán và môi trường local thiếu cấu hình.

Nguồn: [assessment](../reports/260916-0842-user-service-assessment.md).

## Scope challenge

- **Tái sử dụng:** Spring Security, OAuth2 Resource Server, JPA/Flyway,
  repository adapter và `RestClient` hiện có.
- **Tối thiểu:** sửa build contract, chặn quyền sai, bỏ secret mặc định, làm
  token/profile đúng đắn, rồi bổ sung config và test tích hợp.
- **Không làm:** migration Keycloak, thay gateway toàn diện, message broker,
  circuit breaker platform hay thay đổi API ngoài các endpoint/role không an toàn.
- **Chế độ:** HOLD scope. Năm phase là cần thiết vì build → security → data →
  distributed workflow → runtime/test có dependency tuyến tính.

## Quyết định thiết kế

1. Public registration chỉ tạo `PATIENT`; `DOCTOR` và `ADMIN` phải đi qua use
   case/endpoint có quyền `ADMIN`. Xác nhận với product owner trước Phase 2.
2. `user-service` tự xác thực access JWT cho endpoint protected; header
   `X-User-*` không được là nguồn định danh duy nhất.
3. Dùng JWT signing secret từ environment/config secret manager, bắt buộc khi
   startup; không có fallback trong repository.
4. Dùng transactional outbox trong PostgreSQL và worker nội bộ cho profile
   provisioning. Không thêm broker cho nhu cầu hiện tại.

## Phases

| Phase | Mục tiêu | Ưu tiên | Phụ thuộc |
|---|---|---:|---|
| 1 | [Khôi phục build contract](./phase-01-build-contract.md) | P1 | — |
| 2 | [Khóa truy cập và JWT](./phase-02-access-control-jwt.md) | P1 | 1 |
| 3 | [Bảo vệ token và dữ liệu](./phase-03-token-data-integrity.md) | P1 | 1, 2 |
| 4 | [Provisioning profile đáng tin cậy](./phase-04-profile-provisioning-outbox.md) | P1 | 2, 3 |
| 5 | [Cấu hình, container và kiểm thử tích hợp](./phase-05-runtime-integration-tests.md) | P1 | 1–4 |

## Tiêu chí hoàn thành toàn plan

- Maven reactor build/test được `user-service`; Docker image build không copy
  module không tồn tại.
- Chỉ endpoint đăng nhập/refresh/đăng ký public mới anonymous; không thể tự
  tạo `ADMIN`, đọc/sửa user khác hay giả identity bằng header.
- JWT thiếu secret làm service fail-fast; token refresh chỉ được consume một lần.
- Profile provisioning có retry/idempotency/audit, không trả `201` khi workflow
  bị mất mà không thể quan sát.
- Local Compose boot được service với Postgres, Config Server, Eureka; secret
  chỉ inject từ environment. Security, migration, concurrency và HTTP failure
  có automated coverage.

## Rollout và rollback

- Rollout theo phase; deploy Phase 2 trước các phase thay đổi data.
- Backup database trước Flyway V7/V8; kiểm tra tài khoản có `password_hash`
  null trước khi enforce `NOT NULL`.
- Canary các endpoint auth/refresh; theo dõi 401/403, token replay, outbox retry
  và provisioning dead-letter.
- Rollback application bằng image trước đó; không rollback migration đã chạy.
  Migration phải forward-compatible và có runbook bù dữ liệu.

## Red Team Review

Reviewers theo ba lens không kịp phản hồi trong giới hạn ba phút; controller đã
áp dụng finding có evidence từ source. Báo cáo: [red-team review](./reports/from-planner-to-user-service-hardening-red-team-plan-review-report.md).

| Finding | Severity | Disposition | Thay đổi |
|---|---:|---|---|
| Compose hiện chưa có `user-service`, nên Phase 1 không thể dùng `docker compose build user-service` làm gate. | High | Accept | Phase 1 dùng `docker build`; Phase 5 mới thêm và verify Compose service. |

### Whole-Plan Consistency Sweep

- Files reread: `plan.md` và 5 phase files.
- Decision deltas checked: 1.
- Reconciled stale references: 1.
- Unresolved contradictions: 0.
- Unresolved product decisions: 3 câu hỏi ở mục sau; phải chốt trước Phase 2/3/4.

## Câu hỏi cần chốt trước khi implement

1. Public registration có cho phép tạo `DOCTOR` không? Plan mặc định: không.
2. Tài khoản chờ profile có được login không? Khuyến nghị: `PENDING_PROFILE`
   không được login tới khi provisioning thành công.
3. Các user có `password_hash = NULL` là tài khoản legacy hợp lệ hay dữ liệu lỗi?
