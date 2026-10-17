---
phase: 2
title: "Content catalog API"
status: pending
priority: P1
effort: "1.5d"
dependencies: [1]
---

# Phase 2: Content catalog API

## Overview
Catalog cho học viên `POST /api/content/practice-tests/search` (`PART` và `FULL`) và payload nội bộ `GET /internal/learning-content/package-versions/{id}`
mang thêm `partNo`, `timeLimitSeconds`, `groups`, `groupId` để Assessment chụp vào attempt. Gateway không đổi (route `/api/content/**` đã tới content-service).

**Thiết kế gốc (đọc trước, làm theo): [`../261014-1002-practice-test-library/phase-02-content-catalog-api.md`](../261014-1002-practice-test-library/phase-02-content-catalog-api.md).**
Request/response, quyền, thứ tự, phân trang, số query cố định đều giữ nguyên.

## Lệch / bổ sung so với phase thiết kế
- Có endpoint nguồn `GET /api/content/practice-tests/sources` đọc từ enum `TestSource` (Q8 mặc định: có). Trả `[{code, name}]` theo thứ tự enum, cùng quyền role với `search`.
- `skill` ngoài `READING`/`LISTENING` → trang rỗng (không lỗi). Test của phase này chỉ dùng dữ liệu Reading; code không cứng skill.
- Ảnh bìa: dữ liệu demo là data URI `data:image/svg+xml;base64,...` (đã được `MediaReferencePolicy` hiện có chấp nhận). Mở rộng object key cho ảnh vẫn làm như phase thiết kế,
  nhưng **test phải phủ cả data URI svg** (không được làm hỏng nhánh này).
- Test Testcontainers của reader tự `INSERT` fixture như phase thiết kế, **và thêm** một test chạy trên dữ liệu `V30` thật, khẳng định đúng các số của acceptance criteria trong `plan.md`:
  `PART` 6 thẻ · `FULL` 3 thẻ · `PT-READING-ISP-01` có `questionCount = 15`, `timeLimitSeconds = 3600`, `parts.size = 3` · `sources=[C10_C20]` 1 thẻ ·
  `query="walking"` đúng 1 thẻ · hai thẻ của `PT-READING-ACT-01` có `requiredFeatureKey = PREMIUM_CONTENT` · `formats` của part 1 `PT-READING-ISP-01` = `[TRUE_FALSE_NOT_GIVEN, GAP_FILLING]`.
  (Dữ liệu `V30` còn có gói khác sau này thì test dùng điều kiện lọc theo `code`, không đếm toàn bảng.)
- Response không chứa `answerSpec`, `explanation`, transcript; test phải khẳng định bằng cách parse JSON.

## Related Code Files
Gốc: `services/content-service/src/main/java/com/ieltspath/content/` — danh sách Create/Modify/Tests ở phase thiết kế gốc.
Thêm: `api/controller/PracticeTestController.java` (thêm `GET /sources`), `application/usecase/ListTestSourcesUseCase.java` (hoặc gộp vào use case search nếu nhỏ), test `PracticeTestSourcesTest`.

## Implementation Steps
1. Test đỏ theo phase thiết kế + các ý bổ sung.
2. `MediaReferencePolicy` (ảnh nhận object key, giữ data URI svg), reader, use case, controller, DTO, `/sources`.
3. Mở rộng payload nội bộ (một query nhóm cho cả version, không query theo từng section).
4. `springdoc.paths-to-match` của content-service nếu file đang liệt kê route.
5. `mvn -q -pl services/content-service -am test`.

## Success Criteria
- [ ] Mọi tiêu chí của phase thiết kế gốc.
- [ ] Test trên dữ liệu `V30` đạt đúng số liệu ở trên.
- [ ] Số query không đổi khi `size` từ 1 lên 50.
- [ ] Consumer cũ của payload nội bộ (Learning, Game) vẫn đọc được: chỉ thêm field.

## Risk Assessment
Như phase thiết kế. Thêm: nếu `exclude`/`only` vượt 500 phần tử trong thực tế, ghi vào Verification (chưa cần xử lý).
