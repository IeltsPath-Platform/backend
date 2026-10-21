---
phase: 1
title: "Content: ánh xạ tiêu chí sang KP và contract"
status: pending
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 1: Content: ánh xạ tiêu chí sang KP và contract

## Overview

Content-service là chủ của rubric và cây KP, nên ánh xạ `(task, tiêu chí) -> KP` nằm ở đây và được trả cho Learning qua endpoint rubric hiện có (thêm một trường, không đổi nghĩa trường cũ).

## Requirements

- Migration mới, không sửa migration đã áp dụng; số kế tiếp là V69 (V66-V68 đang có, một số do Codex).
- Ánh xạ không gắn với phiên bản rubric: rubric đã publish bất biến, và phiên bản cũ (retired) dùng bởi prompt đã kích hoạt vẫn phải có KP khi `getRubricVersion`.
- Thay đổi contract có tính cộng thêm và nullable; client cũ bỏ qua trường lạ.

## Architecture

```
writing_criterion_knowledge_points(task, criterion_code) PK
        |  join theo (rubric.task, criterion.code) khi đọc
        v
GET /internal/learning-content/rubrics/active?task=...   -> criteria[].knowledgePointId
GET /internal/learning-content/rubrics/{code}/{version}  -> criteria[].knowledgePointId
```

## Related Code Files

- Create: `services/content-service/src/main/resources/db/migration/V69__writing_criterion_knowledge_points.sql`
- Modify: `services/content-service/.../infrastructure/persistence/adapter/JdbcRubricRepository.java` (join bảng ánh xạ khi đọc tiêu chí), record tiêu chí của rubric trong domain/api tương ứng (thêm `knowledgePointId`, nullable), DTO phản hồi của `InternalLearningContentController`
- Modify: `docs/contracts/learning-content-internal-v1.md` (mô tả trường `criteria[].knowledgePointId`, cộng thêm, nullable)
- Không đổi: API admin soạn nháp rubric (`PUT /api/content/admin/rubrics/...`): ánh xạ do migration sở hữu, chưa cho sửa qua UI

## Implementation Steps

1. Tìm record tiêu chí mà `JdbcRubricRepository` trả ra và nơi `InternalLearningContentController` map sang response.
2. Viết V69: tạo bảng `writing_criterion_knowledge_points (task, criterion_code, knowledge_point_id REFERENCES knowledge_points(id), PRIMARY KEY (task, criterion_code))`, giới hạn `task IN ('TASK_1','TASK_2')`.
3. Seed bằng `INSERT ... SELECT` join theo `knowledge_points.code`, đủ 8 dòng: TASK_1: TA/CC/LR/GRA; TASK_2: TR/CC/LR/GRA (ánh xạ ở D1 trong `plan.md`). Nếu một mã KP không tồn tại thì migration phải thất bại rõ ràng (đếm dòng chèn, `RAISE EXCEPTION` khi < 8), không âm thầm bỏ dòng.
4. Sửa `JdbcRubricRepository`: LEFT JOIN bảng ánh xạ theo `(rubrics.task, rubric_criteria.code)` ở cả đường đọc bản active lẫn đọc theo `(code, version)`.
5. Thêm `knowledgePointId` vào record tiêu chí và DTO response; để `null` khi không có dòng ánh xạ.
6. Cập nhật contract doc.

## Success Criteria

- [ ] Sau V69, rubric active của TASK_1 và TASK_2 trả mỗi tiêu chí một `knowledgePointId` khác `null`; trong cùng một task các KP đôi một khác nhau.
- [ ] Rubric phiên bản cũ đọc theo `(code, version)` cũng có `knowledgePointId`.
- [ ] Migration thất bại nếu thiếu KP nguồn; chạy trên DB đã có V68 không lỗi.
- [ ] Client đọc rubric không có trường mới vẫn hoạt động (trường cộng thêm).

## Risk Assessment

- KP nguồn có thể đang `INACTIVE` hoặc không thuộc topic nào: ánh xạ vẫn lưu được nhưng bằng chứng sẽ bị bỏ ở phía Learning. Kiểm tra trạng thái và việc KP có trong danh mục học viên ở phase 3 (bước kiểm chứng) và ghi nhận trong test.
- Đánh số migration trùng với migration mới của Codex: kiểm tra thư mục ngay trước khi tạo file.
- Rollback: bảng mới độc lập, trường trả về nullable; bỏ trường khỏi response là đủ để Learning quay về hành vi cũ.
