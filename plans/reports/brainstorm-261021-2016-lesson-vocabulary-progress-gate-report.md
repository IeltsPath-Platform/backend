# Brainstorm: Từ vựng theo lesson — data, tiến độ, gate

- Ngày: 2026-10-21 · Branch: fix/ecm-ui · Modes: markdown (không --html/--wiki)
- Phạm vi người phụ trách: data từ vựng của lesson + ghi tiến độ/mastery + gate. UI flashcard do dev khác.

## 1. Vấn đề & yêu cầu
- Mỗi lesson có bộ từ vựng riêng (từ xuất hiện trong lesson).
- User phải học xong từ vựng của lesson mới được sang lesson sau (cộng với các gate hiện có).
- Học kiểu flashcard; backend cần contract rõ cho dev FE.

## 2. Hiện trạng (scout)
- Figma node `2881:14526` = section "Test đầu vào" (placement). "Vocabulary 0/10" là phần câu hỏi của Test A, **không** phải thiết kế từ vựng lesson → chưa có design tham khảo.
- content-service đã có `lesson_blocks.block_type = 'VOCABULARY'` + `lesson_block_vocabulary(block_id, vocabulary_sense_id, sort_order)` (V8), ref logic sang library. Internal API trả `vocabularySenseIds`; learning-service chuyển tiếp trong `LessonResponse`. Chưa có seed, FE chưa render.
- library-service: `vocabulary_items` (lemma, ipa, audio) + `vocabulary_senses` (POS, EN def, VI meaning, example, image). Flashcard cá nhân Leitner 1–5. Không có API lấy sense theo danh sách id.
- learning-service: `LessonAccessGate` REVIEW_REQUIRED → TOPIC_LOCKED → LESSON_LOCKED → PRACTICE_REQUIRED. Completion = mọi block EXERCISE pass (`SubmitLessonExerciseUseCase:158`); `CompleteLessonUseCase` chỉ cho lesson không có exercise. `kp_evidence` theo KP, mastery MP-1.0 → điều khiển bài ôn và gate topic.

## 3. Phương án đã cân nhắc
| | Mô tả | Pros | Cons |
|---|---|---|---|
| **A (chọn)** | learning-service giữ tiến độ từ vựng/lesson, block VOCABULARY tính vào completion | Đúng ranh giới service, 1 transaction + lock user, gate không đổi | Sửa 2–3 chỗ tính completion |
| B | Dùng flashcard Leitner ở library, gate gọi library | Tái dùng flashcard | HTTP chéo service trong gate; Leitner = ôn dài hạn, sai ngữ nghĩa "xong bài", chặn nhiều ngày |
| C | FE tự gọi complete | Rẻ | Không có dữ liệu, dễ bypass, vướng `LESSON_HAS_EXERCISES` |

## 4. Quyết định đã chốt
- Pass rule: mỗi sense được đánh "Đã biết" ≥ 1 lần (kiểu Quizlet Learn; thẻ "Chưa biết" quay lại cuối hàng). Pass trong 1 phiên.
- **Không** ghi `kp_evidence`; self-rating không đủ tin cậy cho mastery KP. Chỉ lưu tiến độ theo từ.
- Auto-add flashcard Leitner: để vòng sau.
- Authoring: seed bằng Flyway.
- FE lấy nội dung thẻ qua API batch sense mới ở library (không cho learning gọi library).

## 5. Thiết kế
### 5.1 Data (seed)
- library: migration seed `vocabulary_items` + `vocabulary_senses` với UUID cố định.
- content: migration thêm block `VOCABULARY` cho lesson + `lesson_block_vocabulary`. Vị trí block là quyết định nội dung; gate không phụ thuộc.
- library API mới: `GET /api/content/vocabulary/senses?ids=…` → lemma, ipa, audio, POS, VI/EN meaning, example, image. Chỉ trả sense ACTIVE; giới hạn số id/lần. Gateway đã route `/api/content/vocabulary/**` → library.

### 5.2 Tiến độ (learning-service, migration mới)
```sql
lesson_vocabulary_progress(
  user_id, lesson_id, block_id, sense_id,
  attempts INT, last_result VARCHAR,   -- KNOWN | UNKNOWN
  first_known_at TIMESTAMPTZ NULL,     -- set 1 lần, không xóa
  updated_at, PRIMARY KEY (user_id, block_id, sense_id))
lesson_vocabulary_submissions(request_id UNIQUE, user_id, lesson_id, block_id, response JSONB, created_at)
```

### 5.3 API
- `GET /api/learning/lessons/{id}`: block VOCABULARY thêm `passed`, `knownSenseIds`.
- `POST /api/learning/lessons/{id}/vocabulary/{blockId}/reviews`
  - body `{requestId, results:[{senseId, known}]}`
  - lock user → `LessonAccess.authorize` → validate block là VOCABULARY và sense thuộc block → upsert → nếu mọi sense có `first_known_at` thì `passBlock` → tính completion.
  - response `{blockPassed, lessonCompleted, knownSenseIds, remainingSenseIds}`; replay `requestId` trả response đã lưu.

### 5.4 Completion & gate
- Completion = mọi block EXERCISE **và** VOCABULARY pass. Gom predicate dùng chung (vd `Block::isGradable`) cho `SubmitLessonExerciseUseCase`, `CompleteLessonUseCase` (lesson TEXT+VOCAB tự complete khi pass vocab) và use case mới.
- `LessonAccessGate` không đổi: lesson chưa complete → bài sau `LESSON_LOCKED`. Practice pass/topic completion ăn theo `progress.complete()`.

## 6. Rủi ro
- Lesson đã complete trước khi thêm vocab: completion một chiều → user cũ không bị khóa lại. Ghi vào docs.
- User đang học dở lesson được thêm vocab: bài sau bị khóa tới khi học vocab (đúng ý).
- Sense INACTIVE/lệch giữa content và library: test seed kiểm mọi sense ref tồn tại + ACTIVE.
- Self-rating dễ "spam Đã biết": chấp nhận cho MVP; quiz khách quan là bước nâng cấp.

## 7. Nghiệm thu
- Nộp đủ known → `blockPassed=true`; không còn exercise chưa pass → `lessonCompleted=true`, lesson kế mở.
- Còn sense chưa known → lesson kế `403 LESSON_LOCKED` (sửa ở validation: mã gate là 403).
- Replay `requestId` → cùng response. Sense lạ → 400. Block không phải VOCABULARY → 400/404. Lesson bị khóa → mã gate đúng.
- Không có dòng `kp_evidence` mới.
- Test: unit pass rule, integration Postgres (Testcontainers) cho use case + completion, seed migration test ở library/content.

## 8. Ngoài phạm vi
UI flashcard, auto-add Leitner, quiz khách quan + evidence KP, admin API gắn từ cho lesson.

## 9. Bước tiếp
- `/ck:plan` với report này. Chia phase: (1) seed library + content + API batch sense; (2) learning migration + use case + completion; (3) cập nhật docs/system-architecture.md + contract cho dev flashcard.
- Gửi sớm contract API (5.1, 5.3) cho dev flashcard.

## Câu hỏi còn mở
- Danh sách từ cụ thể cho từng lesson demo: ai cung cấp nội dung?
- Giới hạn số từ/lesson (đề xuất 8–15) — cần chốt với nội dung.

## Addendum 2026-10-21 — thu hẹp phạm vi
- Flashcard là module riêng của dev khác (library Leitner, `flashcard-srs-v1.md`), không phải UI cần backend riêng.
- Vòng này chỉ làm data: seed sense, gắn block VOCABULARY vào lesson, API batch sense, contract. **Không gate, không bảng
  tiến độ, không đổi completion.** Thiết kế mục 5.2–5.4 giữ làm tham khảo cho vòng nối module flashcard.
- Tài liệu kiến trúc (01/02/03): mastery VOC chỉ từ bài tập chấm điểm (mcq/fib/match → evidence MP-1.0); SRS từng từ là
  lớp riêng, không ghi evidence; VOC/GRA không vào band. Khớp quyết định không ghi `kp_evidence`.
- Plan hiện hành: `261021-2016-lesson-vocabulary/plan.md` (Validation Session 2).
