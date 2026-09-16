---
date: 2026-09-16T08:49:00+07:00
plan: 260916-0849-user-service-hardening
status: partial
---

# Red-team review: User-service hardening plan

## Summary

Ba reviewer lens (security, assumptions, failure modes) không phản hồi trong
giới hạn ba phút, nên không có finding external để áp dụng. Controller thực hiện
một static evidence review và sửa một gate sai trong Phase 1.

## Finding accepted

1. **High — Phase 1 dùng Compose target chưa tồn tại.**
   - Evidence: `docker-compose.yml:11` khai báo services; các service hiện có là
     `app-db` (`docker-compose.yml:15`), `config-server` (`:37`),
     `eureka-server` (`:57`) và `api-gateway` (`:79`); không có `user-service`.
   - Risk: `docker compose build user-service` không thể chứng minh Dockerfile
     build được trước khi Phase 5 thêm service.
   - Applied correction: Phase 1 dùng `docker build -f
     services/user-service/Dockerfile .`; Phase 5 thêm Compose service và dùng
     Compose build/smoke test.

## Rejected findings

None. Không có finding external đủ evidence để adjudicate.

## Recommendations

- Chốt ba product/data decisions trong `plan.md` trước khi bắt đầu Phase 2.
- Chạy validate thủ công trước implementation để kiểm tra endpoint/claim contract
  với gateway và owner của doctor/patient profile services.

## Unresolved Questions

- Public registration có thể tạo doctor không?
- User đang provisioning profile có thể login không?
- `password_hash = NULL` là legacy valid hay data lỗi?
