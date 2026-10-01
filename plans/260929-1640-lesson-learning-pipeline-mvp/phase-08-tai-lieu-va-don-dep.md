---
phase: 8
title: "Tài liệu và dọn dẹp"
status: completed
priority: P2
dependencies: [2, 3, 4, 5, 6, 7]
effort: "0.5 ngày"
---

# Phase 8: Tài liệu và dọn dẹp

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan `261001-1228`). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

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

- [x] Tài liệu runtime hiện tại ghi đúng Java; các mô tả Python/DeepTutor cũ trong baseline được đánh dấu lịch sử, không phải hành vi đang chạy.
- [x] Compile cả reactor và toàn bộ test pass; không `.pyc` hay `graphify-out/` trong diff.
- [x] Có report E2E.

## Risk Assessment

- **Phạm vi sửa `docs/system-architecture.md`:** chỉ §3 (một dòng), §6, §11; phần kiến trúc chia service do plan chia service làm.
- **`.sdd/global`** là baseline LOCKED: không sửa constitution.

## Kết quả

- Hoàn tất 2026-10-02 trên nhánh `feat/lesson-pipeline-docs`, nền `feat/main-follow` commit `896fa2c`; không push.
- Đồng bộ schema Java, feature tree, quy tắc Learning, luồng kiến trúc và README root/Content/Assessment/Learning. `CLAUDE.md` đã đúng nên giữ nguyên; không tái tạo tài liệu Python đã xóa.
- `mvn -q compile -DskipTests`, `mvn -q test` (693 test, 0 fail/error/skip), `docker compose config --quiet` và `graphify update .` đều exit 0.
- Live Gateway/Content/Assessment/Learning/PostgreSQL/RabbitMQ: 7/7 kiểm tra pass; Lan X1 75%, KP2 mastery 0.487, chèn review và mở topic kế; DLQ replay không ghi trùng. [Report và giới hạn kiểm chứng](./reports/e2e-261002-foundation-learning-pipeline.md).
- Thực hiện theo ánh xạ Java thay Python; dùng database/container tạm riêng, không áp migration lên DB dùng chung. Reading hints và E2E toàn MVP vẫn thuộc roadmap tiếp theo.
