---
phase: 4
title: "AI Learning: API nộp essay và evidence"
status: pending
priority: P1
dependencies: [2, 3]
effort: "3 ngày"
---

# Phase 4: AI Learning: API nộp essay và evidence

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan [`261001-1228`](../261001-1228-learning-service-java/plan.md)). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

## Context Links

- Plan 1640 phase 6: `plans/260929-1640-lesson-learning-pipeline-mvp/phase-06-ai-learning-api-bai-hoc-va-bai-on.md`
  (V11, `app/lessons/`, `authorize_lesson_access`, `practice_evidence.py`, path transaction)
- Phase 3 của plan này (`app/writing/grader.py`, `AccessServiceClient`)
- Contract `docs/contracts/lesson-writing-v1.md`, `lesson-learning-v1.md`

## Overview

Ghép grader và access client vào luồng nộp essay, **chấm ngay trong request**:

```text
kiểm input → tra requestId → lấy bài từ content → kiểm số dư
→ [path tx 1] cổng + tạo dòng GRADING → commit
→ LLM chấm (ngoài transaction) → lưu kết quả, PAYMENT_PENDING
→ debit (idempotent) → [path tx 2] GRADED + evidence → commit → trả kết quả
```

Không giữ khóa path trong lúc gọi LLM. Không worker, không hoàn point. Gửi lại cùng `requestId` thì chạy tiếp từ bước dang dở.

## Requirements

**Migration `V12__lesson_writing_submissions.sql`:**

```sql
CREATE TABLE lesson_writing_submissions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    block_id UUID NOT NULL,
    question_version_id UUID NOT NULL,
    knowledge_point_ids UUID[] NOT NULL,
    request_id VARCHAR(100) NOT NULL UNIQUE,
    essay_text TEXT NOT NULL,
    word_count INT NOT NULL CHECK (word_count >= 0),
    prompt_snapshot JSONB NOT NULL,          -- stem, task, minWords, passBand, sampleAnswer
    status VARCHAR(20) NOT NULL CHECK (status IN ('GRADING','GRADED','FAILED','PAYMENT_PENDING')),
    point_cost INT NOT NULL CHECK (point_cost > 0),
    debit_ledger_entry_id UUID,
    failure_code VARCHAR(50),
    result JSONB,
    overall_band NUMERIC(2,1) CHECK (overall_band BETWEEN 0 AND 9),
    passed BOOLEAN,
    grading_started_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    graded_at TIMESTAMPTZ,
    CHECK (status <> 'GRADED' OR (result IS NOT NULL AND overall_band IS NOT NULL
                                  AND passed IS NOT NULL AND debit_ledger_entry_id IS NOT NULL)),
    CHECK (status <> 'PAYMENT_PENDING' OR (result IS NOT NULL AND passed IS NOT NULL))
);
CREATE UNIQUE INDEX uq_lesson_writing_in_flight ON lesson_writing_submissions (user_id, block_id)
    WHERE status = 'GRADING';
CREATE INDEX idx_lesson_writing_user_lesson ON lesson_writing_submissions (user_id, lesson_id, submitted_at DESC);
```

- Không đổi index của `mastery_learning_evidence`: `uq_mastery_evidence_source_reference` (V2) gồm cột `source`, partial
  `WHERE source_reference_id IS NOT NULL`, nên bao được `lesson_writing` (1640 phase 6 đã kiểm).
- So với brainstorm: bỏ `QUEUED`, `lease_until`, `attempt_count`, `refund_ledger_entry_id` vì không còn worker hay hoàn
  point. Thêm `PAYMENT_PENDING`.

**`POST /api/ai-learning/lessons/{lessonId}/essays/{blockId}/submissions`** `{requestId, essayText}`,
`require_current_user`, forward bearer (lấy bằng `credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme)` như
`app/api/tutor.py:116`; `AuthenticatedUser` không giữ token):
1. Kiểm input:
   - `requestId` 1–100 ký tự;
   - essay rỗng hoặc chỉ khoảng trắng → 422 `ESSAY_EMPTY`;
   - dưới `writing_min_words` (50) → 422 `ESSAY_TOO_SHORT`; <!-- Updated: Validation Session 1 - ngưỡng dưới 50 từ -->
   - quá `writing_max_words` hoặc `writing_max_chars` → 422 `ESSAY_TOO_LONG`;
   - LLM chưa cấu hình → 503 `GRADING_UNAVAILABLE`;
   - `access_service_base_url` rỗng → 503 `PAYMENT_UNAVAILABLE`.
2. Tra `request_id`:
   - khác user, bài hoặc khối → 409 `REQUEST_CONFLICT`;
   - `GRADED` → trả lại response đã lưu;
   - `PAYMENT_PENDING` → nhảy tới bước 7;
   - `FAILED` → chạy lại từ bước 3 trên cùng dòng;
   - `GRADING` chưa quá `writing_stale_grading_seconds` → 409 `GRADING_IN_PROGRESS`;
   - `GRADING` đã quá hạn → giành lại bằng compare-and-set trên `grading_started_at`, rồi chạy từ bước 3.
3. Lấy bài qua content nội bộ (client của 1640):
   - không có bài hoặc khối → 404;
   - `blockKind ≠ ESSAY` → 409 `NOT_ESSAY_BLOCK`.
4. `get_balance` < `writing_point_cost` → 402 `INSUFFICIENT_POINTS`: **chưa tạo dòng, chưa gọi LLM**. Access lỗi → 503
   `PAYMENT_UNAVAILABLE`.
5. Path tx 1 (`PostgresLearningStore.transaction(path)`, cùng connection):
   - `authorize_lesson_access` → `REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`;
   - làm mới `lesson_progress.knowledge_point_ids` như luồng nộp bài tập;
   - insert dòng `GRADING`, hoặc chuyển dòng `FAILED`/quá hạn về `GRADING`;
   - vi phạm `uq_lesson_writing_in_flight` → 409 `GRADING_IN_PROGRESS`;
   - commit.
6. `grade_task2`, ngoài transaction:
   - `WritingGradingError`, `LlmApiError`, timeout → `UPDATE … SET status='FAILED', failure_code WHERE id AND status='GRADING'`
     → 503 `GRADING_UNAVAILABLE`, không trừ point;
   - thành công → `UPDATE … SET result, overall_band, passed, status='PAYMENT_PENDING'`, lưu trước khi debit để lần gửi
     lại không gọi LLM nữa.
7. Debit:
   - idempotency key `lesson-writing:{user_id}:{request_id}`, `reference_id = submission id`,
     `amount = point_cost` của dòng;
   - `InsufficientPoints` → giữ `PAYMENT_PENDING`, 402 `INSUFFICIENT_POINTS` kèm `submissionId`;
   - `AccessUnavailable` (gồm 401 do token hết hạn) → giữ `PAYMENT_PENDING`, 503 `PAYMENT_UNAVAILABLE` kèm `submissionId`.
8. Path tx 2:
   - `SELECT … FOR UPDATE` dòng; đã `GRADED` (một request khác chạy tiếp xong trước) thì trả lại luôn;
   - `already_passed = EXISTS` dòng `GRADED`, `passed`, cùng user và khối, khác `id`;
   - nếu chưa từng đạt (ngoại lệ có lý do so với luật "lần nộp đầu" của 1640: mỗi lần nộp là bài viết mới): ghi evidence
     qua `practice_evidence.py` với `source = lesson_writing`, mỗi KP của câu một bản ghi,
     `correct = passed`, `source_reference_id = uuid5(NS_LESSON_WRITING, f"{submission_id}:{kp_id}")`. KP không có trong path
     thì bỏ qua và log id (như luật 1640);
   - set `GRADED`, `debit_ledger_entry_id`, `graded_at`;
   - commit;
   - **không** gọi `reevaluate_reviews` và không đổi `lesson_progress.completed_at`.
9. Response 200: `submissionId`, `status`, `wordCount`, `overallBand`, `passed`, `criteria[]`, `corrections[]`, `summary`,
   `pointsCharged`, `sampleAnswer` (chỉ khi `passed` hoặc khối đã từng đạt). DTO `extra="forbid"`, alias camelCase.

**`GET /api/ai-learning/writing-submissions/{id}`:**
- `WHERE id AND user_id`; không có → 404.
- `GRADED`: trả như bước 9.
- `PAYMENT_PENDING`: chỉ `status` và `code`, **không có kết quả**.
- `GRADING`, `FAILED`: `status` và `failureCode`.

**Tích hợp bài học** (sửa code của 1640):
- Bài xong khi mọi khối `EXERCISE` có `blockKind = EXERCISE` đã nằm trong `passed_block_ids`. Khối essay không tính.
- `POST /lessons/{id}/complete` được dùng khi bài không có khối `EXERCISE` tự chấm nào (kể cả bài chỉ có khối essay).
- `POST /lessons/{id}/exercises/{blockId}/submissions` vào khối essay → 409 `ESSAY_BLOCK`.
- `GET /lessons/{id}`: khối essay trả `blockKind`, câu (allowlist `questionVersionId`, `stem`, `task`, `minWords`,
  `passBand`), `latestSubmission {id, status, overallBand, passed}`, và `sampleAnswer` chỉ khi khối đã đạt.
  `latestSubmission` lấy cho mọi khối essay của bài bằng **một** query `DISTINCT ON (block_id)`.

## Architecture

```text
app/writing/store.py     # SQL trên connection truyền vào (tx 1, tx 2) và connection riêng cho cập nhật ngoài tx
app/writing/service.py   # điều phối bước 1–9; phụ thuộc grader, AccessServiceClient, content client, store, practice_evidence
app/api/writing.py       # router
app/api/dto/writing.py   # request/response, extra="forbid"
```

- Mọi mã lỗi đi qua `LearningGateError(code, status, extra)` của 1640 → body `{detail, code, …}`.
- Cập nhật ở bước 6 và 7 dùng connection ngắn riêng, không khóa path. Chỉ bước 5 và 8 mở path transaction.
- Đồng thời:
  - hai `requestId` khác nhau cùng khối: partial unique chặn, request sau nhận 409;
  - hai request cùng `requestId`: request sau thấy `GRADING` chưa quá hạn và nhận 409;
  - chạy tiếp từ `PAYMENT_PENDING`: tx 2 có `FOR UPDATE` và key debit giống nhau, nên chỉ trừ một lần, `GRADED` một lần.

## Related Code Files

- Create: `services/ai-learning-service/migrations/V12__lesson_writing_submissions.sql`, `app/writing/store.py`,
  `app/writing/service.py`, `app/api/writing.py`, `app/api/dto/writing.py`
- Modify: `main.py` (include router), `app/api/dependencies.py` (`get_access_client`, `get_writing_service`),
  `app/learning/practice_evidence.py` (source `lesson_writing`), `app/learning/formal_provenance.py` (thêm `lesson_writing`
  vào các source được lưu reference, cạnh `lesson_exercise`/`review_set` của 1640), `app/lessons/service.py`, `app/lessons/store.py`, `app/lessons/gates.py` (nếu luật hoàn
  thành nằm ở đó), `app/api/dto/lessons.py`, `app/clients/content_service.py` (đọc `blockKind`), `README.md`
- Tests: tạo `tests/test_writing_store_postgres.py`, `tests/test_writing_service_postgres.py`, `tests/test_writing_api.py`;
  cập nhật `tests/test_migrations_postgres.py`, `tests/test_lesson_api.py`, `tests/test_lesson_store_postgres.py`,
  `tests/test_practice_evidence.py`

## Implementation Steps

**Tests Before:**
1. Baseline toàn bộ test ai-learning sau plan 1640. Thêm test khóa hành vi hiện có sẽ bị động tới:
   - bài có 3 khối tự chấm xong khi đạt đủ 3;
   - `complete` cho bài có bài tập → 409;
   - evidence `lesson_exercise` ghi một lần mỗi (câu, KP).

**Tests After** (viết trước code; content, access, LLM đều là fake; Postgres thật với schema tạm):
2. `test_writing_store_postgres.py`:
   - V12 chạy sau V11;
   - CHECK `GRADED` thiếu `result` hoặc ledger bị từ chối;
   - partial unique chặn hai dòng `GRADING` cùng khối, cho phép khi một dòng đã `GRADED`/`FAILED`;
   - index evidence nhận `lesson_writing`.
3. `test_writing_service_postgres.py`:
   - **Đường chính:** đủ point → `GRADED`, debit gọi một lần với đúng key và `amount = 3`, evidence đúng số KP, `correct`
     theo `passBand`.
   - **Idempotent:** gửi lại cùng `requestId` → cùng response, không gọi LLM hay debit lần hai.
   - **Thiếu point khi kiểm số dư** → 402, không có dòng, LLM không được gọi.
   - **LLM lỗi / sai schema / timeout** → `FAILED`, 503, debit không được gọi; gửi lại cùng `requestId` với LLM đã chạy
     được → `GRADED`.
   - **Debit 402 sau khi chấm** → `PAYMENT_PENDING`, 402. GET không lộ kết quả. Nạp point rồi gửi lại cùng `requestId` →
     LLM không được gọi, debit một lần, `GRADED`.
   - **Debit 401 hoặc 500** → `PAYMENT_PENDING`, 503; gửi lại → `GRADED`.
   - **Lỗi cài giữa debit và tx 2**, rồi gửi lại → debit replay (cùng ledger), `GRADED` một lần, evidence một lần.
   - **Song song:** hai `requestId` khác cùng khối → một 200, một 409 `GRADING_IN_PROGRESS`. Hai request cùng `requestId` →
     một 200, một 409.
   - **Dòng `GRADING` quá hạn** → được giành lại và chấm.
   - **Evidence một lần:** lần 1 band 5.5 (chưa đạt) ghi evidence sai; lần 2 band 6.5 ghi evidence đúng; lần 3 ghi thêm → không.
   - **Cổng:** bài ôn chờ → 403 `REVIEW_REQUIRED`; bài bị khóa → 403 `LESSON_LOCKED`; cả hai không có dòng, không gọi LLM.
   - **Sai loại khối:** khối tự chấm → 409 `NOT_ESSAY_BLOCK`. `requestId` của user khác → 409 `REQUEST_CONFLICT`.
   - **Không chèn bài ôn:** `path_review_items` không đổi sau khi chấm (kể cả band thấp).
4. `test_writing_api.py`:
   - tập key response đúng allowlist;
   - `sampleAnswer` chỉ có khi đạt hoặc khối đã đạt;
   - GET submission của user khác → 404;
   - 422 cho essay rỗng, dưới 50 từ, hoặc quá dài; cả ba không gọi access hay LLM.
5. Tích hợp bài học (`test_lesson_api.py`, `test_lesson_store_postgres.py`):
   - bài cuối `DEMO_READING` (có khối essay) xong khi các khối tự chấm đạt, dù chưa nộp essay;
   - học viên 0 point đi hết topic và nhận mã đề;
   - nộp bài tập vào khối essay → 409 `ESSAY_BLOCK`;
   - `GET /lessons/{id}` có `latestSubmission` bằng một query;
   - kịch bản Lan của 1640 vẫn pass.

**Implement:** V12 → store → evidence source → service → router, DTO, dependencies → sửa luật hoàn thành bài và DTO bài học.

**Regression Gate:**
```powershell
$env:PYTHONDONTWRITEBYTECODE = "1"
python -m pytest tests -p no:cacheprovider
```

## Success Criteria

- [ ] Mọi test trên pass; không test nào gọi LLM hay access thật.
- [ ] Mỗi `requestId` trừ point tối đa một lần; LLM lỗi không trừ.
- [ ] Không giữ path transaction trong lúc gọi LLM.
- [ ] Khối essay không chặn hoàn thành bài; cổng bài học vẫn chặn nộp essay.
- [ ] Không log essay, prompt hay output LLM.

## Risk Assessment

- **Request kéo dài khoảng 20–45s:** client cần trạng thái chờ; Gateway hiện không đặt response-timeout. Nếu sau này đặt
  timeout ngắn hơn 60s, phải xem lại luồng này.
- **Token hết hạn giữa chừng (TTL 60s):** debit trả 401 → `PAYMENT_PENDING` → client gửi lại cùng `requestId` với token mới.
  Timeout mỗi lượt LLM ≤ 25s giữ trường hợp này hiếm.
- **LLM đã tốn tiền nhưng chưa thu (race hết point):** kết quả bị giữ tới khi trả; kiểm số dư trước khi chấm giới hạn
  trường hợp này. Chấp nhận.
- **Dòng `PAYMENT_PENDING` bị bỏ lại:** không mất tiền của học viên; không cần job đối soát.
- **Gửi spam request:** kiểm số dư trước và partial unique giới hạn tốc độ tốn LLM.

## Security Considerations

- Chỉ chủ bài đọc được submission (`WHERE id AND user_id`).
- `sampleAnswer` theo luật giấu đáp án; `answerSpec` không bao giờ trả ra.
- Debit dùng bearer của học viên; access kiểm `userId` = subject (phase 1).

## Next Steps

- Phase 5 cập nhật tài liệu. Đợt sau: Task 1, Writing trong đề cuối (dùng lại `app/writing/grader.py`), Speaking.
