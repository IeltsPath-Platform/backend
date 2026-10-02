# P6 – Contract, tài liệu, E2E

Phụ thuộc: P1–P5.

## 1. Contract

| File | Cập nhật |
| --- | --- |
| `docs/contracts/learning-content-internal-v1.md` | `topic-sequence`: điều kiện mới (có skill + lesson PUBLISHED, không bắt buộc TOPIC_TEST), field `skill`, `hasTopicTest`, thứ tự. Lesson: `skill`, block `knowledgePointIds`. Route mới `lessons/{id}/practice-sets`. `practice-sets/search`: `preferredLessonId`. Ghi rõ tất cả là thay đổi **thêm**, v1 vẫn tương thích. |
| `docs/contracts/lesson-learning-v1.md` | Lộ trình theo skill; review khoá theo skill; topic không có test (`hasTopicTest`, `testStatus=NONE`, lỗi `NO_TOPIC_TEST`); Practice catalog + attempt (route, trạng thái, lỗi `PRACTICE_LOCKED`); thang ôn tập (stage, `theoryReason`, `theory-check`, lỗi `THEORY_REQUIRED`/`THEORY_NOT_REQUIRED`, `maxFailedSets=2`, luôn hiện lời giải); `GET /reviews`. Thêm sơ đồ trạng thái review. |

Sơ đồ đưa vào contract:

```text
                 (trigger)
                     |
        +------------+-------------+
        | PRACTICE                 | THEORY  (<40% hoặc sai trong lesson)
        v                          v
   GET: giao set có hint      GET: block lý thuyết của KP + quick-check
   nộp set ─ đạt ─> DONE      POST theory-check ─> PRACTICE
        └ trượt ─ failedSets>=2 ─> SKIPPED
                └ còn lại ─> THEORY
```

## 2. README

- `services/learning-service/README.md`: thay mục "review rules" bằng luật mới; thêm Practice; ghi chú di chuyển dữ
  liệu Writing (P3 mục 1).
- `services/content-service/README.md`: `topics.skill`, `DEMO_WRITING`, `lesson_block_knowledge_points`,
  `content_packages.lesson_id`.
- `CLAUDE.md`/`AGENTS.md`: **không** sửa trừ khi có mục mô tả sai sau thay đổi (ví dụ chuỗi topic duy nhất); nếu sửa
  chỉ sửa đúng câu sai.

## 3. E2E runbook

Thêm `plans/261002-1600-skill-tracks-practice-remediation/reports/e2e.md` (Codex tạo khi chạy), chạy stack Docker
Compose như các plan trước, learner mới, qua Gateway:

1. `GET /api/learning/topics` ⇒ có READING, LISTENING, WRITING, mỗi skill một IN_PROGRESS.
2. Học L1 (Reading) cố tình sai bài tập ⇒ hoàn thành ⇒ (nếu mastery < 0.6) review Reading PENDING.
3. Mở LS1 (Listening) ⇒ 200 (không bị chặn bởi review Reading).
4. `GET /lessons/{L1}/practice-sets` trước/sau khi xong ⇒ LOCKED/AVAILABLE.
5. Chạy kịch bản thang ôn tập: set trượt ⇒ THEORY với block đúng KP ⇒ theory-check ⇒ set trượt ⇒ SKIPPED.
6. W1, W2 ⇒ `/complete` ⇒ topic `DEMO_WRITING` PASSED.

Báo cáo che token, mật khẩu, email.

## 4. Kết thúc

- `mvn -q -pl services/content-service,services/learning-service -am test` xanh.
- `graphify update .`.
- `plan.md`: Status → completed, điền Verification từng phase.
