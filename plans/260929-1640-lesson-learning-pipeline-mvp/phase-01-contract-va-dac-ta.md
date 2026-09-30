---
phase: 1
title: "Contract và đặc tả"
status: pending
priority: P1
dependencies: []
effort: "1 ngày"
---

# Phase 1: Contract và đặc tả

## Overview

Viết contract cho mọi thay đổi HTTP/event trước khi code (AGENTS §3.3, §5). Viết bộ test vector `answer_spec` dùng chung cho Java và Python. Cập nhật đặc tả trong plan kiến trúc. Cuối phase **dừng để người dùng duyệt contract**.

## Requirements

- Functional: đủ contract để phase 2–7 làm mà không phải đoán tên trường.
- Non-functional: không ghi giá trị secret; contract nêu rõ trường mới, trường đổi nghĩa, thứ tự triển khai.

## Architecture

| File | Nội dung |
| --- | --- |
| `answer-spec-v1.md` (mới) | Hai dạng: `CHOICE` `{"type":"CHOICE","correct":"A"}` (dạng cũ `{"correct":"A"}` hiểu là CHOICE; dùng cho MC, TFNG, matching từng đoạn) và `FILL` `{"type":"FILL","accepted":["critics"]}`. Chuẩn hóa FILL: trim, gộp khoảng trắng, không phân biệt hoa thường. Câu trả lời là chuỗi. **Cách lấy câu trả lời từ `attempt_responses.payload` của assessment** (JSON `{"answer": "…"}`) ghi rõ tại đây. Đúng = `max_score`, sai hoặc bỏ trống = 0. Spec `{}` là không chấm được. `question_knowledge_points.weight` **không dùng** khi chấm và khi ghi mastery |
| `answer-spec-v1-vectors.json` (mới) | Mảng `{ "name", "spec", "response", "correct" }`. Gồm ca biên: thiếu trả lời, `null`, khoảng trắng, hoa thường, dạng cũ không có `type`, spec `{}`, payload assessment dạng JSON |
| `lesson-learning-v1.md` (mới) | API học viên (mục dưới), body lỗi, luật giấu đáp án, danh sách trường cho phép của câu hỏi, thay đổi của assessment |
| `learning-content-internal-v1.md` (mới) | Endpoint nội bộ content (`/internal/learning-content/*`); có `answerSpec` nên chỉ gọi nội bộ, Gateway chặn tường minh |
| `assessment-completed-v2.md` (sửa) | Thêm `package_version_id` (producer luôn gửi; consumer thiếu thì bỏ bước topic, không DLQ). `learning_goal_id` nullable, producer gửi `null` (assessment không còn tra goal). Bỏ đoạn "không phát event khi không có goal" (`:25-26`). Pending đổi sang theo `user_id` (`:89-91`). Thứ tự triển khai: consumer nhận null (phase 5) trước producer (phase 4). Runbook replay DLQ |

**API học viên** (`/api/ai-learning`, `lesson-learning-v1.md`):

| Endpoint | Ghi chú |
| --- | --- |
| `GET /topics` | Thứ tự học (`sequenceOrder`), `status`, `completedLessonCount` |
| `GET /topics/{id}/lessons` | Bài và trạng thái; trạng thái đề cuối |
| `GET /lessons/{id}` | Cổng; câu hỏi chỉ gồm `questionVersionId`, `sortOrder`, `stem`, `options` (+ passage); `solutions` chỉ cho khối đã đạt |
| `POST /lessons/{id}/exercises/{blockId}/submissions` | `{requestId, answers}`; cổng như GET |
| `POST /lessons/{id}/complete` | Chỉ bài không có khối EXERCISE; cổng như GET |
| `GET /reviews/{reviewId}` | `reviewId` UUID, chỉ chủ sở hữu (khác: 404); trả set đang mở (giao nếu chưa có) |
| `POST /reviews/{reviewId}/submissions` | `{reviewSetId, requestId, answers}`; chỉ vào set đang mở, sai set thì 409 |
| `POST /topics/{id}/test-assignments` | Idempotent: còn lần giao chưa dùng thì trả lại. Trả `{assignmentId, packageId, packageVersionId}` |

- Body lỗi: `{ "detail", "code", "reviews"? }`. Code: `REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`, `TEST_LOCKED`, `TEST_UNAVAILABLE`, `REQUEST_CONFLICT`, `REVIEW_SET_CLOSED`, `NOT_FOUND`.
- Response nộp khối và bài ôn:
  - chưa đạt: `results[{questionVersionId, correct}]`;
  - đạt: mỗi phần tử thêm `correctAnswer` và `explanation`.

**Thay đổi assessment** (ghi trong `lesson-learning-v1.md`):
- Tạo attempt: `{ packageVersionId, mode, channel }`. `attemptType` **suy từ loại gói**:
  - `TOPIC_TEST` → `TOPIC_GATE`;
  - `MOCK_TEST` → `MOCK`;
  - `PLACEMENT_TEST` → `PLACEMENT`;
  - `QUIZ` → `QUIZ`;
  - `PRACTICE_SET` và `LESSON` → 422.
- `expiresAt` lấy từ `rules` của package version, không nhận từ client. Trường `sections` và `attemptType` cũ bị bỏ qua (Jackson mặc định bỏ qua trường lạ).
- Structure bỏ `answerSnapshot`.
- Result cho học viên (DTO riêng, không đổi DTO của người chấm): `score`, `maxScore`, `percent`, `items[{attemptItemId, questionVersionId, correct}]`; `solutions[]` khi `percent ≥ 70`. Trả version COMPLETED mới nhất.
- Bỏ `POST /api/assessments/attempts/{id}/result` của học viên.

**Thay đổi API content công khai** (ghi trong `lesson-learning-v1.md`, mục riêng):
- `GET /api/content/knowledge-points` bỏ 4 trường `bandMin`, `bandMax`, `effectiveBandMin`, `effectiveBandMax`.
- `POST /api/content/knowledge-points` không còn nhận `bandMin`, `bandMax`.
- Band của topic giữ nguyên.

**Content nội bộ** (`learning-content-internal-v1.md`):
- `GET /topic-sequence`: topic có ≥ 1 bài PUBLISHED **và** ≥ 1 gói `TOPIC_TEST` PUBLISHED, theo `sort_order`.
- `GET /topics/{id}/lessons`
- `GET /lessons/{id}`
- `GET /topics/{id}/test-packages`
- `POST /practice-sets/search` `{knowledgePointId, excludePackageIds[], minQuestions=3, limit=1}`
- `GET /package-versions/{id}`

## Related Code Files

- Create: `docs/contracts/answer-spec-v1.md`, `docs/contracts/answer-spec-v1-vectors.json`, `docs/contracts/lesson-learning-v1.md`, `docs/contracts/learning-content-internal-v1.md`
- Modify: `docs/contracts/assessment-completed-v2.md`
- Modify: `plans/260928-2019-architecture-doc-service-split/plan.md` (§ đặc tả bảng), `main-learning-pipeline.md`: ghi chú "thay bởi" cho các điểm đổi, gồm:
  - bài ôn = gói mới;
  - giấu đáp án;
  - mã đề dùng một lần;
  - thứ tự học lưu ở `topic_progress.sequence_order`;
  - bỏ `topics.test_package_id`;
  - assessment không thêm cột;
  - premium và xếp lại topic hoãn.

## Implementation Steps

1. Đọc `assessment-completed-v2.md`, `practice-v1.md` và controller hiện tại để giữ tên trường có sẵn: `StartAssessmentAttemptRequest.java`, `AttemptStructureResponse.java`, `AssessmentResultResponse.java`, `SaveAttemptResponseRequest.java`.
2. Viết `answer-spec-v1.md` và file vectors (≥ 15 ca, gồm các câu seed V4 dạng cũ).
3. Viết `lesson-learning-v1.md` theo bảng trên, có ví dụ JSON cho từng endpoint và từng lỗi.
4. Viết `learning-content-internal-v1.md`: schema các khối `TEXT`, `ASSET`, `VOCABULARY`, `EXERCISE`; payload package version (kèm `packageId`, `packageType`, `topicId`, `rules`).
5. Sửa `assessment-completed-v2.md`, thêm mục runbook replay DLQ.
6. Cập nhật đặc tả trong plan kiến trúc và `main-learning-pipeline.md`.
7. **Dừng, trình người dùng duyệt** các thay đổi contract: event, request/response assessment, bỏ endpoint học viên mở result, API học viên mới.

## Success Criteria

- [ ] 4 file mới và 1 file sửa có đủ ví dụ JSON; tên trường khớp code ở phần giữ nguyên.
- [ ] File vectors là JSON hợp lệ, ≥ 15 ca.
- [ ] Người dùng đã duyệt contract (ghi ngày duyệt đầu `lesson-learning-v1.md`).

## Risk Assessment

- **Contract lệch khi code:** phase sau sửa contract cùng commit nếu đổi trường.
- **`learning_goal_id` nullable phá vỡ consumer cũ:** consumer duy nhất là ai-learning; phase 5 làm consumer nhận null trước phase 4.
