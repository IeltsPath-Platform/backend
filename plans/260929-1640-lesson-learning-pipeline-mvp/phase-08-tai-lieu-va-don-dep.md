---
phase: 8
title: "Tài liệu và dọn dẹp"
status: pending
priority: P2
dependencies: [2, 3, 4, 5, 6, 7]
effort: "0.5 ngày"
---

# Phase 8: Tài liệu và dọn dẹp

## Overview

Đưa tài liệu khớp code sau phase 2–7 và kiểm toàn repo. **Không** viết lại `docs/system-architecture.md` (việc chia service có plan riêng và viết lại tài liệu này khi tách xong). Ở đây chỉ sửa những mục mô tả sai hành vi hiện tại.

## Requirements

- Tài liệu ghi đúng hành vi mới, theo quy tắc "code là nguồn đúng" (`CLAUDE.md`).
- Không ghi giá trị secret.

## Related Code Files

- Modify:
  - `docs/system-architecture.md`: chỉ §6 (luồng tạo path, kết quả thi → mastery, luồng bài học) và §11 (bỏ mục đã giải; thêm: "cổng bài học là nguồn quyết định luồng học, `next_objective` chỉ là gợi ý cho tutor"). Dòng bảng HTTP §3 cho `/internal/learning-content` và bỏ endpoint KP mapping.
  - `.sdd/database/DATABASE_V5.md`: content (bảng bài học, `content_packages.topic_id`, `knowledge_points` bỏ band), ai-learning bỏ `mastery_path_knowledge_point_bands`, assessment (`answer_snapshot` chứa đáp án và lời giải), ai-learning (bảng V10–V11). Theo Validation Session 1: §7.21 `topic_progress` bỏ cột `status` (suy ra khi đọc, chỉ lưu `passed_at`); §7.24 `path_review_items.status` thêm `SKIPPED`; §7.23 ghi "chỉ lần nộp đầu ghi bằng chứng"; dòng V5.3 ghi 0908 đã hoãn.
  - `docs/ai-learning-database.md` (bỏ goal và band, thêm bảng V11) và `.sdd/database/mvp-database.md` (đã đồng bộ thiết kế ở phase 1 lộ trình 2026-10-01; ở đây chỉ đánh dấu mục nào đã có migration).
  - `.sdd/specs/FEATURE_TREE_V2.md` `:11`, `:269`, `:330`, `:472`, `:591`: bài học là nơi học chính.
  - `AGENTS.md` §3.8: bỏ câu "Sắp thứ tự path bằng LLM không bị giới hạn (một lần mỗi goal)"; thêm "path theo user, không LLM; luật bài học trong `app/lessons`". Cập nhật dòng "Cập nhật lần cuối … commit".
  - `CLAUDE.md` §1, §5 nếu có dữ kiện đổi; `services/ai-learning-service/CLAUDE.md`.
  - `README.md` §6; README của content, assessment (`README.md:18` bỏ gọi user-service), ai-learning (endpoint, migration, setting `AI_LEARNING_REVIEW_MASTERY_THRESHOLD`).

## Implementation Steps

1. Grep kiểm các dữ kiện sắp ghi: route, tên bảng, setting, migration content V8–V9 và ai-learning V10–V11; assessment không migration mới.
2. Sửa từng tài liệu ở trên.
3. `graphify update .` (thêm `--force` vì số node giảm có chủ đích).
4. Kiểm toàn repo:
```powershell
mvn -q compile -DskipTests
mvn -q -pl services/content-service -am test
mvn -q -pl services/assessment-service -am test
mvn -q -pl infra/api-gateway test
docker compose config --quiet
```
   Chạy pytest ai-learning đủ biến môi trường như phase 7.
5. Chạy E2E thủ công của phase 7 một lần cuối; ghi report vào `plans/260929-1640-lesson-learning-pipeline-mvp/reports/`.

## Success Criteria

- [ ] Không tài liệu nào còn ghi path theo goal, sắp thứ tự bằng LLM, `topics.test_package_id`, bài ôn là "làm lại bài tập cũ", hay assessment gọi user-service lấy goal.
- [ ] Compile cả reactor và toàn bộ test pass; không `.pyc` hay `graphify-out/` trong diff.
- [ ] Có report E2E.

## Risk Assessment

- **Phạm vi sửa `docs/system-architecture.md`:** chỉ §3 (một dòng), §6, §11; phần kiến trúc chia service do plan chia service làm.
- **`.sdd/global`** là baseline LOCKED: không sửa constitution.
