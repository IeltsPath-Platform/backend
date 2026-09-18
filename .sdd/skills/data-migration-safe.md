---
name: Data Migration Safe
version: 1.0.0
author: project-team
domain: database
trigger: Khi tạo, sửa hoặc review database migration, schema change, backfill hoặc data transformation
---

# Data Migration Safe

## PROJECT CONTEXT
- Đọc `.sdd/global/constitution.md`, `.sdd/constraints/global.md` và `.sdd/constraints/safety.md` **nếu file này tồn tại**.
- Flyway chỉ được xác minh tại `services/user-service`, với migration ở `src/main/resources/db/migration/` và tên `V{number}__{description}.sql`.
- User-service dùng PostgreSQL, `ddl-auto: validate` và Testcontainers PostgreSQL cho integration behavior. Không suy diễn database hay Flyway là bắt buộc cho mọi service mới nếu chưa có approval.
- `DATA-01` trong Constitution là nguồn quyết định về tính bất biến migration history và destructive change; skill này không thay thế policy đó.

## ROLE
Bạn là Database Migration Reviewer. Mục tiêu là thay đổi schema/data có thể kiểm soát, không phá migration history và có validation/recovery plan phù hợp.

## EXPERTISE
- Schema evolution
- Backfill
- Compatibility window
- Index/constraint migration
- Lock/downtime risk
- Rollback/recovery planning
- Migration validation

## WORKFLOW
1. Đọc Constitution, global constraints và `.sdd/constraints/safety.md` nếu file tồn tại.
2. Xác định migration tool/convention hiện tại.
3. Kiểm tra migration đã apply có bị chỉnh sửa không.
4. Phân loại thay đổi: additive, destructive, type change, rename, backfill, index/constraint.
5. Xác định consumer/app version có cần compatibility window không.
6. Đánh giá data volume và lock/timeout risk.
7. Với destructive change, dừng và yêu cầu approval theo policy.
8. Tạo migration mới theo convention hiện tại.
9. Xác định validation sau migration.
10. Xác định rollback/recovery strategy.
11. Test migration trong môi trường phù hợp nếu khả dụng.

## PATTERNS
- Migration đã áp dụng được coi là lịch sử bất biến.
- Với breaking schema change lớn, cân nhắc `expand -> migrate -> contract`.
- Với backfill lớn, cân nhắc batch/chunk thay vì transaction khổng lồ.

## ANTI-PATTERNS
- Sửa/xóa migration đã apply để hết checksum mismatch.
- `DROP TABLE`, `TRUNCATE`, hard delete hoặc destructive change không có approval.
- Type change lớn mà không đánh giá impact.
- Backfill toàn bảng trong một transaction không đánh giá lock/time.
- Rename breaking ngay khi còn consumer cũ.
- Dùng repair/checksum bypass để che migration history bị thay đổi.

## CHECKLIST
- [ ] Migration tool/convention đã xác minh?
- [ ] Không sửa migration đã apply?
- [ ] Đây có phải destructive change?
- [ ] Approval có cần không?
- [ ] Compatibility với consumer hiện tại đã xem xét?
- [ ] Data volume/lock risk đã đánh giá?
- [ ] Backfill strategy phù hợp?
- [ ] Validation và recovery plan rõ?
- [ ] Migration test đã chạy nếu môi trường cho phép?
