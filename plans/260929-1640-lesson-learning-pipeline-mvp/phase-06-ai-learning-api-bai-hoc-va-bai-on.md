---
phase: 6
title: "AI Learning: API bài học, bài ôn, giao mã đề"
status: pending
priority: P1
dependencies: [3, 5]
effort: "4 ngày (2 PR)"
---

# Phase 6: AI Learning: API bài học, bài ôn, giao mã đề

## Overview

Làm API học viên theo `lesson-learning-v1.md`:
- thứ tự học và danh sách bài;
- cổng cho **mọi** đường đọc và ghi;
- nộp khối bài tập;
- đánh giá lại path khi bài xong;
- bài ôn bằng gói `PRACTICE_SET`;
- giao mã đề một lần dùng.

**Mọi thay đổi trạng thái của học viên chạy trong một path transaction, trên cùng một connection.**

Chia 2 PR:
- **6a:** V11, ghi bằng chứng, thứ tự học, bài học, nộp bài, cổng, luật bài ôn.
- **6b:** bài ôn bằng gói, giao mã đề.

## Requirements

**Transaction và đồng thời** (cho mọi endpoint ghi):
- Mở `PostgresLearningStore.transaction(path)` (khóa dòng `mastery_paths` của user, nên các lượt ghi của cùng user chạy tuần tự).
- Mọi SQL của bài học, bài ôn, giao mã đề chạy trên **connection đang mở của transaction đó** (qua `_active_connection`), không mở connection riêng.
- Thứ tự khi nộp khối:
  1. Tra `requestId`: cùng user, bài, khối thì trả lại `response` đã lưu; khác thì 409 `REQUEST_CONFLICT`.
  2. Chấm.
  3. Cập nhật có điều kiện `UPDATE lesson_progress SET passed_block_ids = array_append(...) WHERE NOT (block = ANY(passed_block_ids)) RETURNING` để biết khối **lần đầu** đạt. Chỉ ghi bằng chứng khi khối **chưa từng đạt** trước lần nộp này.
  4. Ghi bằng chứng.
  5. Lưu submission kèm `response`.
  6. Nếu bài vừa xong: `completed_at`, rồi `reevaluate_reviews`.
  7. Commit.
- Lỗi ở bất kỳ bước nào thì rollback toàn bộ; thử lại với cùng `requestId` sẽ chạy lại từ đầu.
- `topic_progress` và `lesson_progress`: `INSERT … ON CONFLICT DO NOTHING`.

**Ghi bằng chứng:**
- Hàm mới trong `app/learning/practice_evidence.py` (không phải `app/mastery`). Không dùng `record_external_quiz_outcome`, vì hàm đó luôn gán `source = "assessment_service"` (`external_assessment.py:72`).
- Nhận `source` là `lesson_exercise` hoặc `review_set`.
- `source_reference_id = uuid5(namespace theo source, f"{request_id}:{question_version_id}:{knowledge_point_id}")`.
- Sửa `formal_provenance.py:80-93` và bước chiếu evidence để lưu reference cho source mới.
- V11 đổi partial unique index của `mastery_learning_evidence` (`V2:29-31`) sang bao cả ba source.
- Supersede kết quả thi không được xóa bằng chứng bài học hay bài ôn.
- KP của câu không có trong path: gọi refresh trước (qua `GET /topics`). Nếu vẫn không có thì bỏ qua KP đó và ghi log (id, không nội dung).

**Endpoint** (prefix `/api/ai-learning`, `require_current_user`, forward bearer khi gọi content):
- **`GET /topics`:**
  - gọi `refresh_active_path` (gộp topic và KP mới, không làm gì nếu content không đổi);
  - gọi content `GET /topic-sequence` **một lần**;
  - upsert `topic_progress` với `sequence_order`; topic đầu tiên là IN_PROGRESS.
  - Topic đầu `IN_PROGRESS`, các topic còn lại `LOCKED`; topic mới xuất hiện trong content được thêm dòng `LOCKED`.
  - Trả theo `sequence_order`, kèm `status` và `completedLessonCount`.
  - Không gọi content theo từng topic. Tool reorder của tutor không ảnh hưởng thứ tự học.
- **`GET /topics/{id}/lessons`:** topic chưa mở → 403 `TOPIC_LOCKED`. Trả bài và trạng thái đề cuối (`LOCKED|AVAILABLE|PASSED`).
- **`authorize_lesson_access(user, lesson)`**, dùng chung cho `GET /lessons/{id}`, `POST …/submissions`, `POST …/complete`. Thứ tự lỗi:
  1. bài ôn PENDING (trừ chính bài ôn đó) → `REVIEW_REQUIRED` kèm `reviews`;
  2. topic chưa IN_PROGRESS hoặc PASSED → `TOPIC_LOCKED`;
  3. bài trước chưa xong → `LESSON_LOCKED`.
- **`GET /lessons/{id}`:**
  - DTO câu hỏi dùng **danh sách trường cho phép**: `questionVersionId`, `sortOrder`, `stem`, `options`, và passage; `extra="forbid"`;
  - khối đã đạt kèm `solutions` (`correctAnswer`, `explanation`);
  - lưu hoặc làm mới `knowledge_point_ids`, `lesson_sort_order` vào `lesson_progress`.
- **`POST /lessons/{id}/exercises/{blockId}/submissions`** `{requestId, answers}`:
  - cổng như trên;
  - thiếu câu → 422;
  - làm mới `knowledge_point_ids` từ payload content (không phụ thuộc việc đã GET hay chưa);
  - response: chưa đạt chỉ có `correct`; đạt thì thêm lời giải.
- **`POST /lessons/{id}/complete`:** chỉ bài không có khối EXERCISE; cổng như trên.
- **`GET /reviews/{reviewId}`** (6b):
  - `reviewId` UUID; `WHERE id AND user_id`, không thấy → 404.
  - Trong path transaction: có set đang mở (partial unique: một set mở cho mỗi review item) thì trả lại.
  - Nếu chưa có: search content với `excludePackageIds` = các gói đã giao; hết thì lấy gói giao lâu nhất.
  - Trả lý thuyết (khối TEXT của bài dạy KP) và gói (allowlist như trên).
- **`POST /reviews/{reviewId}/submissions`** `{reviewSetId, requestId, answers}` (6b):
  - `SELECT … FOR UPDATE` set đang mở khớp `reviewSetId`; không khớp hoặc đã đóng → 409 `REVIEW_SET_CLOSED`.
  - Chấm, ghi bằng chứng (`review_set`), đóng set.
  - ≥ 70%: review DONE. < 70%: GET sau sẽ giao gói khác.
  - Không đánh giá lại path.
- **`POST /topics/{id}/test-assignments`** (6b):
  - Cổng: topic IN_PROGRESS, mọi bài xong, không bài ôn PENDING.
  - Idempotent: còn lần giao chưa dùng (`consumed_at IS NULL`) thì trả lại.
  - Không có thì chọn `package_id` chưa từng dùng (`consumed_at` khác NULL), hết thì lấy gói dùng lâu nhất. Lấy **version đang publish** của gói (content `GET /topics/{id}/test-packages`).
  - Topic không có mã đề → 409 `TEST_UNAVAILABLE`.
- **Lỗi content:** 404 → `NOT_FOUND` 404 (không dồn thành 502 như `main.py:88-90`); lỗi khác giữ 502/503.

**Luật chèn bài ôn** `reevaluate_reviews(conn, user_id, considered_kps, wrong_kps)`: hàm thuần, chạy trên connection được truyền vào. Với mỗi KP trong `considered_kps`, chèn `path_review_items` PENDING cho bài có `sequence_order`/`lesson_sort_order` nhỏ nhất khi đủ cả ba:
- `mastery(kp) < threshold`;
- `kp ∈ wrong_kps`;
- có bài trong `lesson_progress` của user với `completed_at` khác NULL và `kp ∈ knowledge_point_ids`, **kể cả bài vừa xong**.

Khi bài vừa xong là bài được chọn, đây là **luyện thêm theo mastery**:
- học viên nhận lý thuyết của chính bài đó và một gói câu mới cùng KP (cùng cơ chế bài ôn);
- bài kế bị chặn (`REVIEW_REQUIRED`) tới khi đạt.

Học viên làm tốt, không có KP dưới ngưỡng kèm câu sai, thì đi thẳng.

Đã có PENDING cho KP thì bỏ qua. `considered_kps` và `wrong_kps` khi bài xong gom qua mọi lần nộp của bài. `threshold` = `AI_LEARNING_REVIEW_MASTERY_THRESHOLD` (mặc định 0.6).

## Architecture

**Migration `V11__lesson_learning.sql`:**
- `topic_progress`: PK(`user_id`, `topic_id`), `sequence_order`, `status LOCKED|IN_PROGRESS|PASSED`, `passed_at`, `updated_at`.
- `lesson_progress`: PK(`user_id`, `lesson_id`), `topic_id`, `lesson_sort_order`, `knowledge_point_ids UUID[]` (GIN), `passed_block_ids TEXT[]`, `completed_at`, `updated_at`.
- `lesson_exercise_submissions`: `id` UUID, `user_id`, `lesson_id`, `block_id`, `request_id` UQ, `answers` JSONB, `block_passed`, `response` JSONB, `submitted_at`.
- `path_review_items`: `id` UUID, `user_id`, `knowledge_point_id`, `lesson_id`, `status PENDING|DONE`, `created_at`, `done_at`; partial UQ(`user_id`, `knowledge_point_id`) WHERE PENDING.
- `path_review_sets`: `id` UUID, `review_item_id` FK, `user_id`, `package_id`, `package_version_id`, `assigned_at`, `submitted_at`, `passed`, `request_id` UQ NULL, `response` JSONB; partial UQ(`review_item_id`) WHERE `submitted_at IS NULL`; INDEX(`user_id`, `package_id`).
- `topic_test_assignments`: `id` UUID, `user_id`, `topic_id`, `package_id`, `package_version_id`, `assigned_at`, `consumed_attempt_id` NULL, `consumed_at` NULL, `percent` NULL; partial UQ(`user_id`, `topic_id`) WHERE `consumed_at IS NULL`.
- Đổi partial unique index của `mastery_learning_evidence` để bao `lesson_exercise`, `review_set`.

**Module:**
- `app/lessons/`:
  - `grading.py`: `answer-spec-v1`; không dùng `app/mastery/grading.py`.
  - `gates.py`: hàm thuần.
  - `review_rule.py`
  - `store.py`: SQL, nhận connection.
  - `service.py`: điều phối.
- `app/learning/practice_evidence.py`
- `app/api/lessons.py`, `app/api/dto/lessons.py`: alias camelCase, `extra="forbid"`.
- `main.py`: include router; `LearningGateError(code, status, extra)` → `{detail, code, …}`; giữ `{detail}` cho lỗi cũ.
- `app/clients/content_service.py`: thêm `get_topic_sequence`, `get_topic_lessons`, `get_lesson`, `get_topic_test_packages`, `search_practice_sets`, `get_package_version`.
- `app/config.py`: `review_mastery_threshold: float = 0.6`, validate 0 < x ≤ 1.

**Không làm:** premium, xếp lại topic, flashcard (app gọi learning-support trực tiếp).

**Thẩm quyền:** luồng học do cổng bài học quyết định. `next_objective` của `/status` và tutor chỉ là gợi ý (ghi trong contract và tài liệu).

## Related Code Files

- Create: `migrations/V11__lesson_learning.sql`, `app/lessons/{__init__,grading,gates,review_rule,store,service}.py`, `app/learning/practice_evidence.py`, `app/api/lessons.py`, `app/api/dto/lessons.py`
- Modify: `main.py`, `app/clients/content_service.py`, `app/config.py`, `app/api/dependencies.py`, `app/learning/formal_provenance.py:80-93`, `app/persistence/postgres_learning_store.py` (lưu reference cho source mới; hàm lấy connection đang mở dùng được từ `app/lessons/store.py`), `README.md:187-202`
- Tests:
  - tạo mới: `tests/test_lesson_grading.py`, `tests/test_lesson_gates.py`, `tests/test_review_rule.py`, `tests/test_practice_evidence.py`, `tests/test_lesson_store_postgres.py`, `tests/test_lesson_api.py`, `tests/test_review_sets.py`, `tests/test_topic_test_assignment.py`;
  - cập nhật: `tests/test_migrations_postgres.py`.

## Implementation Steps

**Tests Before:**
1. Baseline toàn bộ test ai-learning sau phase 5. `/status`, `/progress`, tutor, practice giữ nguyên.

**Tests After** (viết trước code):
2. `test_lesson_grading.py`: mọi vector.
3. `test_practice_evidence.py`:
   - source đúng theo nguồn; reference tất định theo `request_id`;
   - ghi hai lần cùng `request_id` → không trùng;
   - supersede kết quả thi không xóa bằng chứng bài học.
4. `test_review_rule.py` (số mastery thật):
   - kịch bản Lan (0.6):
     - L1 xong (KP3 0.88) → không chèn;
     - L2 xong (KP1 0.51, sai Q5) → chèn L2 (luyện thêm KP1);
     - L3 và L4 đúng hết → không chèn;
     - đề sai Q15 → chèn L3;
   - chạy lại với threshold 0.9: như trên, và thêm L1 bị chèn (KP3 0.88 < 0.9, từng sai Q11);
   - học viên đúng hết L1–L4 lần đầu → không chèn gì;
   - PENDING trùng không chèn lại;
   - nhiều bài dạy KP → chọn thứ tự nhỏ nhất.
5. `test_lesson_gates.py`:
   - thứ tự lỗi;
   - chính bài ôn thì mở được;
   - POST submissions và complete bị chặn giống GET (topic khóa, bài ôn chờ);
   - đề cuối khóa khi topic chưa IN_PROGRESS.
6. `test_lesson_store_postgres.py`:
   - trùng `requestId` → cùng response;
   - `requestId` của user khác → 409;
   - **hai luồng** nộp cùng khối chưa đạt → bằng chứng ghi một lần;
   - hai khối cuối nộp song song → bài xong đúng một lần;
   - **lỗi giữa chừng** (lỗi cài ở bước ghi bằng chứng) → không ghi gì, thử lại cùng `requestId` → ghi đủ.
7. `test_lesson_api.py`:
   - tập key của câu hỏi đúng bằng danh sách cho phép (không `answerSpec`, không `explanation`) ở mọi response;
   - 2/3 chưa đạt không có lời giải; 3/3 có lời giải, bài kế AVAILABLE;
   - nộp lại khối đã đạt → mastery không đổi;
   - nộp bài mà chưa GET → `knowledge_point_ids` vẫn được lưu;
   - `complete` cho bài có bài tập → 409;
   - content 404 → 404.
8. `test_review_sets.py`:
   - review của user khác → 404;
   - hai lần GET cùng lúc → một set;
   - POST vào set đã đóng hoặc sai `reviewSetId` → 409;
   - 1/4 → GET sau nhận gói khác; hết gói → gói lâu nhất; 3/4 → DONE;
   - không đánh giá lại path.
9. `test_topic_test_assignment.py`:
   - POST hai lần → cùng lần giao;
   - sau khi dùng (phase 7 đánh dấu) → mã khác theo `package_id`;
   - hết mã → mã dùng lâu nhất, version đang publish;
   - không có mã → `TEST_UNAVAILABLE`;
   - cổng.

**Implement (6a):** V11 → practice_evidence → grading → gates → review_rule → store → content client → service → router và handler lỗi.
**Implement (6b):** bài ôn → giao mã đề.

**Regression Gate:**
```powershell
$env:PYTHONDONTWRITEBYTECODE = "1"
python -m pytest tests -p no:cacheprovider
```
Chạy kèm `AI_LEARNING_TEST_DATABASE_URL`. Không test nào gọi LLM thật.

## Success Criteria

- [ ] 8 endpoint đúng contract; cổng áp cho mọi đường đọc và ghi.
- [ ] Test bước 2–9 pass, gồm test đồng thời và test lỗi giữa chừng.
- [ ] Không gọi content hay repository trong vòng lặp; `app/mastery/*` không bị sửa.

## Risk Assessment

- **Commit path mỗi lần nộp:** chiếu evidence đã chuyển sang ghi theo lô ở phase 5.
- **Hết gói luyện tập:** gói cũ có thể đã hiện lời giải; chấp nhận trong MVP.
- **Phase lớn nhất:** 2 PR như trên.
