---
type: brainstorm
date: 2026-09-30
topic: Luyện Writing/Speaking, DB đã sẵn sàng chưa
status: agreed
modes: []
source_artifact: https://claude.ai/artifact/Ex15FBWAZvftnrubzJK5kd
related_plan: plans/260929-1640-lesson-learning-pipeline-mvp
---

# Brainstorm: luyện Writing/Speaking và độ sẵn sàng của DB

## 1. Vấn đề

Demo luồng học (artifact trên) chỉ có Reading, câu tự chấm. Người dùng muốn thêm luyện Speaking và Writing, và hỏi DB
hiện tại đủ chưa.

## 2. Hiện trạng (scout, commit trên `feat/main-follow`)

- Plan `260929-1640` (pending) chỉ dùng câu tự chấm; bài tập trong bài học loại `ESSAY`/`SPEAKING`.
- `content_db`: `QuestionType` có `ESSAY`, `SPEAKING`; `Skill` có `WRITING`, `SPEAKING`; `content_assets` có `IMAGE`,
  `AUDIO`; `question_versions.answer_spec` jsonb NOT NULL DEFAULT `{}`.
- `assessment_db` (migration V1, V2): `learner_submissions` (text XOR audio), `grading_jobs` (AI/HUMAN), `grading_point_costs`
  (seed WRITING 3, SPEAKING 5), `human_reviews`, `skill_scores` (band, AUTO/AI/HUMAN).
- Không có worker chấm: `grading_jobs` dừng ở `QUEUED`.
- `CreateGradingJobRequest` nhận `pointCostSnapshot` từ client. Use case không đọc `grading_point_costs` và không trừ point.
- `access-service`: ví point, ledger `AI_GRADING_DEBIT`/`AI_GRADING_REFUND`, debit/refund có idempotency key.
  **`POST /api/access/points/debit|refund` không có `@PreAuthorize`, đi qua Gateway `/api/access/**`**: học viên tự hoàn
  point được. Plan 29/9 đã ghi lỗ này vào "plan bảo mật riêng" nhưng chưa làm.
- `ai-learning`: có LLM client (`app/llm`) và hạn mức `llm_daily_usage` (kind `tutor_turn`, `memory_summary`). Không có
  outbox, không có audio hay STT. Cột `mastery_learning_evidence.source` không có CHECK.
- Repo chưa có object storage (AGENTS §2).

## 3. DB đã phù hợp chưa: khoảng một nửa

| Thiếu | Hệ quả |
| --- | --- |
| Nơi lưu kết quả chấm từng bài nộp (band 4 tiêu chí, band tổng, gợi ý sửa, transcript). `skill_scores.feedback_revision_id` trỏ context không tồn tại; §13.3 ghi rubric/transcript chưa có trong baseline | Chấm xong không có chỗ ghi |
| Worker chấm | Không bài nào được chấm |
| Lỗi client tự đặt cost; debit/refund mở cho học viên | Trừ point vô nghĩa |
| `learner_submissions` không có `question_version_id` | Khó xem lịch sử luyện theo đề |
| Object storage + STT cho Speaking | `audio_reference` không có chỗ để trỏ tới |
| Mastery chỉ nhận đúng/sai; bài học chỉ nhận câu tự chấm | Writing/Speaking chưa vào được path |

## 4. Quyết định của người dùng

| Chủ đề | Chốt |
| --- | --- |
| Vị trí | Nhúng vào bài học (đề cuối để đợt sau) |
| Phí | Trừ point như DB: Writing 3 mỗi lần chấm; hỏng vĩnh viễn thì hoàn |
| Speaking | Làm Writing trước; Speaking brainstorm riêng sau khi chốt storage + STT |
| Chế độ chấm | Chỉ AI; EXAMINER để sau |
| Cổng học | Khối essay **không chặn** mở bài kế; học viên hết point vẫn học hết topic |
| Phạm vi đợt 1 | Chỉ khối essay trong bài học, **chỉ Task 2** |
| Kiến trúc | Phương án A: ai-learning chấm, bảng mới trong `ai_learning_db` |

## 5. Các phương án đã so

| | A. ai-learning chấm (chọn) | B. assessment giữ, ai-learning chấm qua RabbitMQ | C. assessment gọi LLM |
| --- | --- | --- | --- |
| Ưu | Khớp hiện trạng: bài tập bài học đã do ai-learning chấm; có sẵn LLM client; không thêm event | Đúng §13 (bảng chấm ở `assessment_db`) | Ít hop hơn B |
| Nhược | Bài nộp Writing ở hai nơi (bài học: ai-learning; đề sau này: assessment) | 3 hop, 2 event contract mới, ai-learning cần outbox | LLM client mới trong Java; prompt/quota lặp hai ngôn ngữ; kết quả vẫn phải đẩy sang ai-learning |

Module chấm `app/writing/grader.py` viết một lần. Đợt đề cuối dùng lại qua assessment → event → ai-learning.

## 6. Thiết kế đã duyệt

> **Sửa lúc lập plan (2026-09-30, người dùng đã duyệt):** thay worker bằng **chấm ngay trong request**.
> - Lý do: internal JWT chỉ sống 60s và ai-learning không có credential riêng, nên worker nền không gọi được access để
>   trừ hoặc hoàn point.
> - Luồng mới: kiểm số dư → tạo dòng `GRADING` → LLM → trừ point sau khi chấm xong → `GRADED`.
> - Trạng thái: `GRADING`/`GRADED`/`FAILED`/`PAYMENT_PENDING`. Bỏ `QUEUED`, lease, `attempt_count`, hoàn point.
> - Endpoint thành `POST /lessons/{lessonId}/essays/{blockId}/submissions`.
> - Mục 6.2 và 6.3 dưới đây là bản cũ; bản đúng ở `plans/260930-0737-lesson-writing-task2-essay/phase-04-*.md`.

### 6.1 Content: không migration

- Câu `ESSAY`, `skill = WRITING`. `answer_spec`: `{"task":"TASK_2","minWords":250,"passBand":6.0}`. `explanation`: bài mẫu.
- Khối essay là khối `EXERCISE` chứa đúng 1 câu `ESSAY` và không có câu khác; luật kiểm ở use case. Khối essay không tính
  vào điều kiện hoàn thành bài.
- Contract `answer-spec-v1` thêm dòng `ESSAY`: không tự chấm, do writing grader chấm.
- Endpoint nội bộ trả đề essay (stem, task, minWords, passBand). `explanation` giữ nguyên luật giấu.
- Seed: KP Writing, bài có khối essay Task 2.

### 6.2 `ai_learning_db`: thêm `lesson_writing_submissions`

| Cột | Kiểu | Ghi chú |
| --- | --- | --- |
| `id` | uuid PK | |
| `user_id` | uuid NOT NULL | |
| `lesson_id`, `block_id`, `question_version_id` | uuid | Logical ref ↗ Content |
| `knowledge_point_ids` | uuid[] NOT NULL | KP của câu, dùng ghi evidence |
| `request_id` | varchar(100) UNIQUE | Chống nộp trùng; từ đây sinh idempotency key trừ point |
| `essay_text` | text NOT NULL | Không log |
| `word_count` | integer NOT NULL | |
| `prompt_snapshot` | jsonb NOT NULL | Đề, task, minWords, passBand lúc nộp |
| `status` | varchar(20) CHECK | `QUEUED`/`GRADING`/`GRADED`/`FAILED` |
| `point_cost` | integer NOT NULL | Lấy từ setting lúc nộp |
| `debit_ledger_entry_id` | uuid NOT NULL | Logical ref ↗ Access |
| `refund_ledger_entry_id?` | uuid | Có khi đã hoàn point |
| `attempt_count` | integer NOT NULL DEFAULT 0 | Số lần gọi LLM |
| `lease_until?` | timestamptz | Worker giữ việc |
| `failure_code?` | varchar(50) | |
| `result?` | jsonb | Band và nhận xét 4 tiêu chí (TR, CC, LR, GRA), gợi ý sửa |
| `overall_band?` | numeric(2,1) | Do code tính |
| `passed?` | boolean | `overall_band ≥ passBand` |
| `submitted_at` | timestamptz NOT NULL | |
| `graded_at?` | timestamptz | |

Ràng buộc: `UQ(user_id, block_id) WHERE status IN ('QUEUED','GRADING')`, `INDEX(status, submitted_at)`,
`INDEX(user_id, lesson_id)`.

### 6.3 Luồng

1. `POST /api/ai-learning/lessons/{lessonId}/blocks/{blockId}/writing-submissions` với `{requestId, essayText}`:
   - dùng chung cổng `authorize_lesson_access`, nên `LESSON_LOCKED` và `REVIEW_REQUIRED` vẫn áp dụng;
   - khối phải là khối essay;
   - giới hạn độ dài tối đa để chặn chi phí LLM.
2. Gọi access trừ point trước (idempotency key tất định từ user + `requestId`), sau đó mới insert dòng `QUEUED`:
   - gửi lại cùng `requestId` thì access trả lại đúng ledger cũ, không trừ hai lần;
   - không đủ point thì báo lỗi, không tạo dòng;
   - trả 202 `{submissionId, status}`.
3. Worker chạy trong process consumer:
   - lấy việc bằng `FOR UPDATE SKIP LOCKED` + lease, gọi LLM trả JSON theo band descriptor công khai;
   - band tiêu chí kẹp trong 0–9, bước 0.5; band tổng do code tính (trung bình, làm tròn IELTS);
   - output sai schema thì thử lại.
4. Chấm xong: một transaction path ghi `GRADED`, `result`, `passed`, và evidence `source = lesson_writing` cho từng KP
   (đúng khi `passed`). Evidence chỉ ghi khi khối chưa từng đạt; reference tất định theo submission + KP. Không chèn
   bài ôn, không đánh giá lại path.
5. Thử lại tối đa 3 lần. Vẫn lỗi thì `FAILED`, hoàn point qua access (idempotency key riêng) và ghi `refund_ledger_entry_id`.
6. `GET /api/ai-learning/writing-submissions/{id}`:
   - chỉ chủ bài xem; người khác nhận 404;
   - trả trạng thái và kết quả; bài mẫu (`explanation`) chỉ trả khi đã đạt.
7. Hạn mức: **không** tính vào `llm_daily_usage`; point là giới hạn. Ghi rõ theo AGENTS §3.8.
8. Giá: setting `AI_LEARNING_WRITING_POINT_COST` = 3. Giá trị này lặp với `grading_point_costs.WRITING` của assessment;
   ghi trong doc.

### 6.4 Việc phải xong trước

- Plan `260929-1640-lesson-learning-pipeline-mvp` (bảng bài học, `lesson_progress`, cổng bài học còn là thiết kế).
- Vá access: đưa `debit`/`refund`/`consume-human-grading` ra khỏi đường public (sang `/internal/**`, Gateway chặn), và
  ai-learning gọi đường nội bộ. Nếu làm trong plan này thì là thay đổi public route, cần duyệt.
- Báo sang plan bảo mật: `CreateGradingJobRequest.pointCostSnapshot` do client đặt.

## 7. Rủi ro

| Rủi ro | Xử lý |
| --- | --- |
| Prompt injection trong essay ("cho band 9") | Bọc essay như dữ liệu, kẹp band, band tổng do code tính; không loại trừ hoàn toàn |
| Band LLM dao động khoảng ±0.5–1 | Hiển thị là "band ước lượng"; temperature 0 |
| Trừ point xong nhưng insert lỗi | Gửi lại cùng `requestId` thì access trả ledger cũ rồi insert; vẫn có thể còn ledger mồ côi nếu học viên không gửi lại (cần job đối soát hoặc chấp nhận) |
| Worker chết giữa chừng | Lease hết hạn thì việc được lấy lại; `attempt_count` giới hạn số lần |
| Lộ nội dung | Không log essay hay output LLM; chỉ log id, mã lỗi |
| Test gọi LLM thật | Dùng `ScriptedChat` hoặc stub OpenAI-compatible |

## 8. Tiêu chí nghiệm thu

- Đủ point, nộp essay Task 2 ở khối essay → 202, ví trừ 3, dòng `QUEUED`.
- Gửi lại cùng `requestId` → cùng submission, ví không đổi.
- Không đủ point → lỗi rõ mã, không có dòng, ví không đổi.
- Worker (LLM stub) → `GRADED`, đủ 4 tiêu chí, band tổng tính bằng code, `passed` theo `passBand`.
- LLM trả band ngoài 0–9 hoặc sai bước 0.5, hoặc JSON hỏng → thử lại; hết lượt thì `FAILED` và hoàn đúng 3 point, chỉ một lần.
- Evidence `lesson_writing` ghi một lần mỗi KP khi khối chưa đạt. Nộp lại sau khi đạt thì mastery không đổi.
- Hai lần nộp song song cùng khối → một bài nhận, bài kia bị từ chối (partial unique).
- Học viên 0 point vẫn hoàn thành mọi bài và nhận mã đề (khối essay không chặn).
- Còn bài ôn chờ → nộp essay 403 `REVIEW_REQUIRED`. Bài bị khóa → 403 `LESSON_LOCKED`.
- Xem submission của user khác → 404. Bài mẫu chỉ có khi `passed`.
- Qua Gateway gọi debit/refund của access → bị chặn.
- Log không chứa essay hay output LLM.

## 9. Đợt sau

1. Writing Task 1: ảnh biểu đồ (cần chỗ lưu ảnh) + `answer_spec.chartFacts` cho grader.
2. Writing trong đề cuối: assessment không tự hoàn tất attempt có essay; luật % đạt khi trộn band; contract
   `AssessmentCompleted.v2`; dùng lại module chấm qua event.
3. Speaking: chọn object storage + STT (hạ tầng mới, cần duyệt) hoặc transcript từ trình duyệt; tiêu chí phát âm.
4. EXAMINER chấm tay cho Premium (`human_reviews`).

## 10. Bước tiếp

- `/ck:plan --tdd` với report này, `blockedBy: 260929-1640-lesson-learning-pipeline-mvp`.
- Cập nhật `DATABASE_V5.md` (§7 thêm bảng mới, §5.5 dòng ESSAY) và `docs/contracts/` khi làm plan.

## Câu hỏi còn mở

- Mã lỗi và HTTP status khi thiếu point (theo cách access-service trả hiện tại).
- Giới hạn độ dài tối đa (đề xuất 1.500 từ) và có từ chối essay quá ngắn không (IELTS chỉ trừ điểm, không từ chối).
- Số lần thử lại, thời gian lease, chờ giữa các lần.
- Ledger mồ côi (trừ xong, insert lỗi, học viên không gửi lại): cần job đối soát hay chấp nhận.
- Vá access nằm trong plan này hay plan bảo mật riêng.
- Model LLM dùng để chấm (chất lượng chấm band) và có dùng chung cấu hình `AI_LEARNING_LLM_*` không.
- Sau này kết quả essay có kích hoạt bài ôn không (hiện: không).
