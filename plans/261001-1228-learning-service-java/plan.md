---
title: "Thay ai-learning (Python) bằng learning-service (Java), chỉ giữ tính năng MVP"
description: "Xóa service Python, dựng learning-service Spring Boot (port 8086, route /api/learning/**) làm path theo user, bài học, bài ôn, giao mã đề, consumer kết quả đề và API xem mastery; tutor, practice, learner memory, sắp path bằng LLM bị bỏ."
status: in-progress
priority: P1
branch: "feat/learning-service-skeleton"
tags: [learning-service, java, mvp, ai-learning-removal, lesson, mastery]
blockedBy: []
blocks: [260929-1640-lesson-learning-pipeline-mvp, 260930-0737-lesson-writing-task2-essay, 260930-0812-lesson-writing-task1-academic, 260930-0851-listening-topic-audio-lessons, 260930-1006-reading-question-hints]
created: "2026-10-01T05:28:51.000Z"
createdBy: "ck:plan"
source: skill
---

# Thay ai-learning (Python) bằng learning-service (Java)

## Overview

Người dùng chốt 2026-10-01: bỏ service Python, viết service học bằng Java, chỉ làm tính năng MVP đã lên ở các plan con.
Plan này **thay** phase 5–7 của 1640 (path theo user, API bài học/bài ôn/mã đề, consumer) và phần ai-learning của các plan
con. Phase 1–4 của 1640 (contract, chặn lộ đáp án, content, assessment tự chấm) vẫn giữ nguyên.

Luật nghiệp vụ (cổng, bài ôn, bằng chứng lần nộp đầu, mã đề một lần dùng, TOPIC_GATE) **không đổi**: nguồn là
`260929-1640/plan.md` (bảng "Quyết định áp dụng"), `phase-06`, `phase-07` và contract đã duyệt. Plan này chỉ đổi nơi chạy,
mô hình dữ liệu và những gì bị bỏ.

## Quyết định (người dùng chốt 2026-10-01)

| Chủ đề | Quyết định |
| --- | --- |
| Service | Module Maven mới `services/learning-service`, package `com.group01.learning`, cổng 8086, Eureka `lb://learning-service`. Route Gateway `/api/learning/**` (thay `/api/ai-learning/**`), nằm trong `internal-jwt-paths` |
| Code Python | **Xóa ngay ở PR đầu**: `services/ai-learning-service/` (app, tests, migrations, Dockerfile), compose `ai-learning-*` và `llm-stub`, route Gateway cũ. Luật cần port đọc từ git history (`git show 90fd390:services/ai-learning-service/...`) |
| Dữ liệu | DB mới `learning_db` (compose `learning-db`, host 5436), Flyway V1 của Java. Không chuyển dữ liệu `ai_learning_db` (chỉ có dữ liệu dev) |
| Mastery | Port nguyên `compute_mastery` của DeepTutor (5 lần gần nhất, trọng số 0.5→1.0, trần 0.5/0.8 khi 1/2 lần) sang Java, giữ comment nguồn Apache-2.0. Số trong kịch bản Lan của `seed-content.md` giữ nguyên |
| Giữ thêm | (1) API xem mastery theo KP `GET /api/learning/mastery`; (2) hạn mức LLM theo ngày, làm cùng chấm Writing (plan 0737 viết lại) |
| Bỏ hẳn | Tutor SSE, practice notebook, learner memory, sắp path bằng LLM, `/status`, `/progress`, `/paths`, placement test-out, learning goal, parking kết quả chờ path |

## Mô hình dữ liệu (thay `mastery_paths` + `state_json`)

Không còn aggregate path của DeepTutor. Thứ tự học suy từ content; bằng chứng lưu theo user.

- `knowledge_point_catalog(kp_id PK, topic_id, has_practice_set, refreshed_at)`: bảng **dùng chung mọi user**, ghi lại từ
  `topic-sequence` mỗi lần `GET /topics`; consumer đọc `has_practice_set` không gọi HTTP.
- `topic_progress`, `lesson_progress`, `lesson_exercise_submissions`, `review_items`, `review_sets`, `topic_test_assignments`:
  như V11 trong `260929-1640/phase-06` (bỏ chữ `path_`).
- `kp_evidence(id, user_id, kp_id, correct, source, source_reference_id, attempt_id NULL, result_version NULL, created_at)`;
  `source ∈ lesson_exercise | review_set | assessment`; UQ(`user_id`, `source`, `source_reference_id`). Mastery của KP =
  `compute_mastery(correct theo created_at, id)`.
- `assessment_result_versions(user_id, attempt_id, result_version, processed_at)` PK(`user_id`, `attempt_id`): idempotency và chấm lại.

Vì bằng chứng gắn user, consumer **không cần path**: không có bảng pending, không advisory lock tạo path.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Xóa Python, dựng khung learning-service](./phase-01-xoa-python-dung-khung.md) | Done |
| 2 | [Thứ tự học, bài học, nộp bài, cổng, mastery](./phase-02-bai-hoc-va-mastery.md) | Pending |
| 3 | [Bài ôn và giao mã đề](./phase-03-bai-on-va-ma-de.md) | Pending |
| 4 | [Consumer AssessmentCompleted.v2](./phase-04-consumer-ket-qua-de.md) | Pending |
| 5 | [Tài liệu repo](./phase-05-tai-lieu-va-plan-con.md) (plan con đã đồng bộ 2026-10-01) | Pending |

Tiến độ ngày 2026-10-01: **1/5 phase hoàn tất (20%)**. Phase 1 đã hoàn tất phạm vi code trên nhánh
`feat/learning-service-skeleton`; Maven test và compile pass. `docker compose config --quiet` còn cần người dùng thêm
`LEARNING_DB_PASSWORD` vào `.env`; chưa được ghi nhận pass. Chi tiết kiểm chứng và tài liệu còn chờ phase 5:
[`learning-service-skeleton-verification.md`](./reports/learning-service-skeleton-verification.md).

Thứ tự: 1 → 2 → 3 → 4 → 5. Sau phase 4 mới làm phase 4 của 1640 (assessment tự chấm, phát event không goal, có
`package_version_id`; người dùng chốt làm tuần tự), rồi E2E.

**Giao cho agent code:** [`codex-handoff.md`](./codex-handoff.md). Plan con viết cho Python đọc theo
[`python-to-java-mapping.md`](./python-to-java-mapping.md) (đã gắn vào plan con ngày 2026-10-01).

## Kiểm thử (mức nhẹ, người dùng chốt)

- Không TDD đầy đủ. Mỗi phase: test Mockito cho luật thuần (mastery, cổng, luật bài ôn, chấm answer-spec bằng
  `docs/contracts/answer-spec-v1-vectors.json`), một test Testcontainers cho luồng ghi chính, `@WebMvcTest` cho bảo mật route.
- Regression: `mvn -q -pl services/learning-service -am test` + `mvn -q -pl infra/api-gateway test`.

## Ngoài phạm vi

- Mọi tính năng ở dòng "Bỏ hẳn". Không giữ code hay test của chúng.
- Chấm Writing, client access, hạn mức LLM: thuộc 0737/0812 (đọc theo `python-to-java-mapping.md`).
- Premium, xếp lại topic, API soạn nội dung (như 1640).

## Tiêu chí nghiệm thu

- Repo không còn Python của ai-learning; `mvn -q compile -DskipTests` cả reactor pass; `docker compose config --quiet` pass.
- Các tiêu chí nghiệm thu luồng học của `260929-1640/plan.md` đạt với prefix `/api/learning`, trừ các dòng về tutor,
  goal, parking.
- `GET /api/learning/mastery` trả mastery theo KP của chính user; số khớp kịch bản Lan.
- Consumer nhận event không goal, idempotent theo `(attempt_id, result_version)`, chấm lại thì thay bằng chứng của version cũ.

## Rủi ro

- **Xóa Python trước khi Java đủ:** giữa phase 1 và 4 không có consumer; event `assessment.completed.v2` không có queue nhận
  sẽ mất (chỉ dev). Chấp nhận (người dùng chọn xóa ngay).
- **Mất luật đã code ở Python** (chuẩn hóa event, supersede): đọc từ commit `90fd390`, ghi rõ file trong phase 4.
- **FE mock** đang dùng `/api/ai-learning/*`: phase 1 đổi contract; báo FE đổi prefix.

## Câu hỏi mở

- Biến `.env` mới `LEARNING_DB_PASSWORD` (thay `AI_LEARNING_DB_PASSWORD`): người dùng tự thêm vào `.env`.
