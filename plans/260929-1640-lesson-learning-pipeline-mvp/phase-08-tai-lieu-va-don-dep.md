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

Đưa tài liệu khớp code sau phase 2–7 và kiểm toàn repo. **Không** viết lại `docs/system-architecture.md`: plan `260928-2019-architecture-doc-service-split` giữ việc đó (phase 2 của plan ấy, sau khi tách library-service). Ở đây chỉ sửa những mục mô tả sai hành vi hiện tại.

## Requirements

- Tài liệu ghi đúng hành vi mới, theo quy tắc "code là nguồn đúng" (`CLAUDE.md`).
- Không ghi giá trị secret.

## Related Code Files

- Modify:
  - `docs/system-architecture.md`: chỉ §6 (luồng tạo path, kết quả thi → mastery, luồng bài học) và §11 (bỏ mục đã giải; thêm: "cổng bài học là nguồn quyết định luồng học, `next_objective` chỉ là gợi ý cho tutor"). Dòng bảng HTTP §3 cho `/internal/learning-content` và bỏ endpoint KP mapping.
  - `.sdd/database/DATABASE_V5.md`: content (bảng bài học, `content_packages.topic_id`, `knowledge_points` bỏ band), ai-learning bỏ `mastery_path_knowledge_point_bands`, assessment (`answer_snapshot` chứa đáp án và lời giải), ai-learning (bảng V10–V11).
  - `.sdd/specs/FEATURE_TREE_V2.md` `:11`, `:269`, `:330`, `:472`, `:591`: bài học là nơi học chính.
  - `AGENTS.md` §3.8: bỏ câu "Sắp thứ tự path bằng LLM không bị giới hạn (một lần mỗi goal)"; thêm "path theo user, không LLM; luật bài học trong `app/lessons`". Cập nhật dòng "Cập nhật lần cuối … commit".
  - `CLAUDE.md` §1, §5 nếu có dữ kiện đổi; `services/ai-learning-service/CLAUDE.md`.
  - `README.md` §6; README của content, assessment (`README.md:18` bỏ gọi user-service), ai-learning (endpoint, migration, setting `AI_LEARNING_REVIEW_MASTERY_THRESHOLD`).

## Implementation Steps

1. Grep kiểm các dữ kiện sắp ghi: route, tên bảng, setting, migration content V7–V8 và ai-learning V10–V11; assessment không migration mới.
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

- **Chồng chéo với plan kiến trúc:** chỉ sửa §3 (một dòng), §6, §11; ghi vào plan kiến trúc rằng đặc tả §12 đã cập nhật ở phase 1.
- **`.sdd/global`** là baseline LOCKED: không sửa constitution.
