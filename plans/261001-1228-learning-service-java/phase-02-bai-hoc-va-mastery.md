---
phase: 2
title: "Thứ tự học, bài học, nộp bài, cổng, mastery"
status: completed
priority: P1
dependencies: [1]
effort: "3 ngày"
---

# Phase 2: Thứ tự học, bài học, nộp bài, cổng, mastery

## Overview

Làm phần "6a" của `260929-1640/phase-06` bằng Java: `GET /topics`, `GET /topics/{id}/lessons`, `GET /lessons/{id}`, nộp khối,
`complete`, cổng, luật chèn bài ôn, cộng `GET /mastery`. Luật nghiệp vụ lấy nguyên từ phase 6 đó; dưới đây chỉ ghi chỗ khác.

## Requirements

**Domain (thuần, không Spring):**
- `MasteryCalculator.compute(List<Boolean>)`: port `compute_mastery` (git `90fd390:services/ai-learning-service/app/mastery/mastery.py`),
  comment nguồn: `Derived from DeepTutor v1.6.9 (Apache-2.0), deeptutor/learning/mastery.py @ da856ad.`
- `AnswerSpecGrader`: chấm theo `docs/contracts/answer-spec-v1.md`.
- `TopicStatusDeriver`: `PASSED` khi `passed_at`; topic đầu chưa PASSED theo `sequence_order` là `IN_PROGRESS`; còn lại `LOCKED`;
  `sequence_order = NULL` bỏ qua.
- `LessonAccessGate`: thứ tự lỗi `REVIEW_REQUIRED` (kèm `reviews`) → `TOPIC_LOCKED` → `LESSON_LOCKED`; dùng cho GET bài, nộp,
  complete.
- `ReviewRule.reevaluate(consideredKps, wrongKps, mastery, lessonsByKp, kpsWithPracticeSet, pendingKps)`: đủ bốn điều kiện
  của phase 6 thì chèn `review_items` PENDING cho bài có `(sequence_order, lesson_sort_order)` nhỏ nhất.

**Application/infrastructure:**
- `application/port/LearningContentClient` + adapter `RestClient` gọi content `/internal/learning-content/*` (contract
  `learning-content-internal-v1.md`), forward bearer và `X-Correlation-Id`. Content 404 → 404 `NOT_FOUND`; lỗi khác 502/503.
- `GET /topics`: gọi `topic-sequence` **một lần**; upsert `knowledge_point_catalog` (theo lô) và `topic_progress.sequence_order`
  (topic biến mất → NULL); trả `status`, `completedLessonCount`. Không còn tạo path.
- Nộp khối: trình tự 7 bước của phase 6 trong **một** `@Transactional`; tuần tự hóa theo user bằng
  `pg_advisory_xact_lock(hashtext(user_id))` ở đầu transaction (thay khóa dòng `mastery_paths`). Bằng chứng (`kp_evidence`,
  `source = lesson_exercise`) chỉ ở lần nộp đầu của khối; `source_reference_id = UUIDv5(namespace cố định của source,
  "{requestId}:{questionVersionId}:{kpId}")`. Bài vừa xong → `ReviewRule` với mastery tính từ `kp_evidence`.
- DTO câu hỏi: record chỉ có `questionVersionId, sortOrder, stem, options` + passage; khối đã đạt kèm `solutions`.
- **`GET /mastery`** (giữ thêm, người dùng chốt): `[{knowledgePointId, topicId, mastery, evidenceCount}]` của user hiện tại,
  KP trong `knowledge_point_catalog`, một query gom `kp_evidence` theo KP. Ghi vào `lesson-learning-v1.md`.
- Không gọi repository hay content trong vòng lặp (AGENTS §3.7).

## Tests (nhẹ)

- Unit: `MasteryCalculator` (giá trị gốc của DeepTutor), `AnswerSpecGrader` (đủ vector JSON), `TopicStatusDeriver`,
  `LessonAccessGate`, `ReviewRule` với kịch bản Lan ngưỡng 0.6 (`seed-content.md` "Kịch bản kiểm thử"; số lệch thì dừng báo).
- Testcontainers: nộp khối trùng `requestId` → cùng response; nộp lại khối không thêm bằng chứng; bài xong chèn review đúng.
- `@WebMvcTest`: allowlist field câu hỏi (không `answerSpec`, `explanation` trước khi đạt).

## Success Criteria

- [x] 5 endpoint bài học + `/mastery` đúng contract; cổng áp cho mọi đường đọc và ghi.
- [x] Không N+1; test trên pass.

## Kết quả ngày 2026-10-01

Hoàn tất trên `feat/learning-lessons`, tạo từ `feat/main-follow` mới nhất tại `9d6f192` (đã fetch origin).
Năm lớp domain thuần, client Content, sáu route, khóa transaction theo user, bằng chứng lần nộp đầu, đọc mastery theo
ordinal và chèn review đều đã triển khai. Giữ nguyên số Lan và comment nguồn DeepTutor Apache-2.0.

`mvn -q -pl services/learning-service -am test`: **125 pass, 0 fail/error/skip** (Learning 117, common-security 8).
Docker chạy thật: 12 ca integration PostgreSQL và 1 ca context/Flyway. Review không còn blocker; Graphify đã cập nhật.
Phase 3–5 còn pending; không triển khai route bài ôn, giao mã đề hoặc consumer trong phase này.

Chi tiết file, lệnh và bằng chứng: [learning-lessons-verification.md](./reports/learning-lessons-verification.md).
