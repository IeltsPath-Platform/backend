# Brainstorm: kết quả examiner chấm Writing ảnh hưởng mastery và tạo bài ôn

- Ngày: 2026-11-05
- Loại: brainstorm, người dùng đã duyệt hướng; chưa sửa code
- Phạm vi: Assessment (event), Learning (evidence, mastery, review, API), Content (mapping mã lỗi → KP), frontend (trang kết quả)
- Plan tiếp theo: `/ck:plan --tdd` từ báo cáo này

## 1. Vấn đề

Learner nhờ examiner chấm Writing (Premium, lượt chấm). Examiner cho band 4 tiêu chí, nhận xét, đánh dấu lỗi theo
**loại lỗi** (error type). Kết quả chỉ hiển thị: contract `human-review-v1.md` ghi "feedback only: no evidence, no reviews,
no change to Practice or gates". Luồng LLM chấm Writing thì có: band tiêu chí + KP con → evidence → mastery → bài ôn tự chọn.

Yêu cầu: examiner đóng vai trò như LLM — kết quả chấm cho biết KP nào yếu, cập nhật mastery, tạo bài ôn.

## 2. Hiện trạng đã kiểm trong code

| Chủ đề | Hiện trạng | Nguồn |
|---|---|---|
| Learner gửi bài | Bài lesson/practice đã `GRADED` (AI) → `POST …/human-review`; hoặc free-writing gửi thẳng examiner (`AWAITING_EXAMINER`) | `HumanReviewUseCase`, `SubmitFreeWritingToExaminerUseCase` |
| Examiner nộp | `band`, `criteria[{criterion, band, comment}]` (`taskResponse`/`coherence`/`lexical`/`grammar`), `errorCodes`, `annotations[{excerpt,start,end,errorCode,suggestion}]` | `HumanReviewService.complete`, `JdbcHumanReviewStore.complete` |
| Báo xong | Outbox `assessment.grading-completed.v1`, cùng transaction; chỉ `learnerId`, `gradingJobId`, `reviewSubmissionId`, `skill`; Notification kiểm contract chặt | `GradingCompletedAnnouncer`, `grading-completed-v1.md` |
| Learning dùng kết quả | Không: chỉ proxy đọc review; không ai trong Learning nghe event | `HumanReviewUseCase.review` |
| Mã lỗi → KP | `error_types.knowledge_point_id` (KP cha), `trap_knowledge_point_id` (KP con `TRAP_<mã>`, V79). Learning đọc qua `activeErrorTypes()` và `ErrorTrapCatalog` (catalog local) | content V45/V71/V79, learning V36 |
| Mã Writing có KP | `LINKER_MISUSE`→`DEMO_WRITING_LINKING`; `WORD_CHOICE`,`COLLOCATION`→`W70_LEXICAL_PRECISION`; `UNDERDEVELOPED_IDEA`→`W70_IDEA_DEVELOPMENT`; `T1_NO_OVERVIEW`,`T1_DATA_MISREPORTED`→`W60_T1_KEY_FEATURES` (topic/course 6.0, 7.0 ACTIVE từ V103) | V45, V71, V103 |
| Mã Writing chưa có KP | `TENSE`, `ARTICLE`, `SUBJECT_VERB_AGREEMENT`, `SENTENCE_FRAGMENT`, `PLURAL_FORM`, `NO_CLEAR_POSITION`, `OFF_TOPIC` | V45 |
| KP ngữ pháp đích | `G55_ARTICLES`, `G55_SUBJECT_VERB_AGREEMENT`, `G60_PERFECT_TENSES`, `G55_COMPLEX_SENTENCES`… ACTIVE (chuyên gia duyệt, V84); chưa bài học/câu hỏi dùng | V84, V103 mục 4 |
| Bộ luyện bẫy Writing | `LINKER_MISUSE` 6 bộ, `WORD_CHOICE` 4, `COLLOCATION` 4, `UNDERDEVELOPED_IDEA` 4 (mỗi bộ 4 câu, 2 câu chứa bẫy). Chưa có cho mã ngữ pháp | V80, V83 |
| Bài ôn theo mã lỗi | `ErrorTrapReviews`: KP con bẫy mức `PRIORITY` → review không chặn cho KP cha, bộ có ≥2 câu bẫy, trigger `ERROR_PATTERN`, giới hạn 1 | `ErrorTrapReviews` |
| Bài ôn theo tiêu chí | `WritingCriterionReviews`: tiêu chí band < passBand → review không chặn, tối đa 2, trigger `WRITING_CRITERION`, cần `lessonId` | `WritingCriterionReviews` |
| Evidence AI Writing | Lesson: `lesson_writing` (0.5), ref `LessonEvidenceReference.forWriting(submissionId, kp)`, chỉ lần nộp trước khi pass. Practice: qua `SubmitPracticeAttemptUseCase` | `SubmitLessonEssayUseCase:177-200` |
| Thay evidence | Tiền lệ `removeAssessmentEvidence` + dựng lại profile khi bài thi chấm lại | `KnowledgeEvidenceRepository` |
| Migration mới nhất | learning V42, assessment V22, content V112 | — |

## 3. Phương án đã xét

| Phương án | Kết luận |
|---|---|
| Giữ "feedback only" | Không đạt yêu cầu |
| Examiner chọn KP con PASS/FAIL (như `judgments` LLM) | **Để sau**: sửa form + contract; danh sách KP con chỉ 2 KP của CC |
| Evidence nguồn mới cho KP cha theo mã lỗi | **Bỏ**: một lỗi không đủ quyết định lộ trình (ràng buộc của brainstorm 261024-0135); cơ chế bẫy đã giải đúng bài này |
| Bài ôn sinh từ chính đoạn sai của learner (excerpt + suggestion) | **Để sau**: tính năng mới hoàn toàn |
| **Band tiêu chí (như AI) + mã lỗi qua KP con bẫy + mapping thêm mã ngữ pháp** | **Chọn** |

## 4. Thiết kế đã chốt

### 4.1 Nguyên tắc
- Examiner là "người chấm" thay LLM: Learning xử lý kết quả examiner bằng đúng đường của luồng AI và luồng bẫy, không có luật riêng.
- Tiêu chí quyết định **KP nào yếu**; mã lỗi quyết định **yếu theo kiểu nào** (KP con bẫy), như brainstorm 261024-0135.

### 4.2 Assessment: event mới
- `assessment.human-review-completed.v1`, outbox cùng transaction với `complete` (cạnh `GradingCompletedAnnouncer`), relay hiện có.
- Payload: `eventId`, `eventType`, `occurredAt`, `learnerId`, `reviewSubmissionId`, `sourceSubmissionId`, `source`, `task`,
  `criteria[{criterion, band}]`, `errorCodes[{code, count}]` (gộp `errorCodes` + `annotations[].errorCode`, mọi mã examiner ghi).
  **Không** chở essay, comment, excerpt, suggestion.
- Không sửa `grading-completed.v1` (Notification từ chối event sai contract).
- Topology mới trong `common-events` (`HumanReviewCompletedTopology`), queue Learning + retry + DLQ, theo mẫu `GradingCompletedTopology`.

### 4.3 Learning: áp kết quả
Listener mới theo mẫu `AssessmentCompletedListener` (ack sau commit, retry, DLQ) → use case áp kết quả, dưới learner lock:
1. Tìm bài viết theo `sourceSubmissionId`, kiểm thuộc `learnerId`. Idempotent: mỗi `reviewSubmissionId` áp một lần (bảng ghi đã áp).
2. **Tiêu chí:** map tên tiêu chí examiner → mã rubric (`TR`/`TA`/`CC`/`LR`/`GRA`) → KP tiêu chí (rubric Content).
   Band ≥ `passBand` của đề → đúng, < → sai. Nguồn evidence mới `human_review`, trọng số cấu hình được (đề xuất 1.0).
3. **Examiner thay AI:** với KP tiêu chí của chính bài này, xóa evidence AI tương ứng (ref xác định được) rồi ghi evidence
   examiner, dựng lại profile (như `removeAssessmentEvidence`). Evidence AI cho KP câu hỏi và KP con giữ nguyên.
4. **Mã lỗi:** mỗi mã có KP con bẫy (`ErrorTrapCatalog.trapKpByCode`) → một evidence `error_trap` sai (0.3) mỗi mã mỗi review.
   Mã không có KP con: bỏ qua.
5. **Bài ôn** (không chặn, tổng tối đa `learning.human-review.review-limit`, mặc định 2):
   - Tiêu chí trượt → `WritingCriterionReviews` (trigger `WRITING_CRITERION`, cần `lessonId` của bài).
   - Mã lỗi examiner đánh dấu → review **trực tiếp** (không chờ KP con xuống `PRIORITY`, vì đây là phán đoán của người chấm)
     cho KP cha, bộ luyện có ≥2 câu chứa bẫy đó, trigger `ERROR_PATTERN`; lesson theo `ErrorTrapReviews.teachingSubmissionLesson`
     (lesson của bài, hoặc lesson đã hoàn thành dạy KP cha) — nhờ vậy bài free-writing cũng có bài ôn.
   - Thứ tự: mã lỗi nhiều lần nhất trước, rồi tiêu chí band thấp nhất. KP đã có review PENDING: bỏ qua.
6. Lưu kết quả áp (KP yếu, review đã tạo) để đọc lại; `GET /writing-submissions/{id}/human-review` thêm `studyPoints`
   (khi `DONE` và đã áp).

Không thêm `trigger_kind` mới (tái dùng `WRITING_CRITERION`, `ERROR_PATTERN`). Thêm `human_review` vào `kp_evidence_source_check`.

### 4.4 Content: mapping mã ngữ pháp (cần chuyên gia duyệt)
Migration mới: gán `knowledge_point_id` + tạo KP con bẫy (theo V79) cho mã Writing chưa có KP. Đề xuất ban đầu, **phải được
duyệt trước khi merge**:

| Mã | KP đề xuất | Độ khớp |
|---|---|---|
| `ARTICLE` | `G55_ARTICLES` | Tốt |
| `SUBJECT_VERB_AGREEMENT` | `G55_SUBJECT_VERB_AGREEMENT` | Tốt |
| `PLURAL_FORM` | `G55_ARTICLES` ("Articles and countability") | Trung bình |
| `TENSE` | `G60_PERFECT_TENSES` | Yếu (chỉ thì hoàn thành) |
| `SENTENCE_FRAGMENT` | `G55_COMPLEX_SENTENCES` | Yếu |
| `NO_CLEAR_POSITION`, `OFF_TOPIC` | chưa có KP phù hợp | Giữ NULL |

Chưa có bộ luyện bẫy cho mã ngữ pháp → mastery đổi nhưng bài ôn bị bỏ qua cho tới khi soạn bộ luyện (việc nội dung sau).

### 4.5 Frontend
`ExaminerResult` thêm khối "Cần ôn": KP yếu (từ tiêu chí và mã lỗi), nút Ôn ngay (review đã tạo) / Luyện tập (free practice).

### 4.6 Tài liệu
`human-review-v1.md` (bỏ "feedback only"), contract mới `human-review-completed-v1.md`, `lesson-writing-v1.md` (mục evidence),
README learning-service.

## 5. Rủi ro và lưu ý
- Đảo quyết định đã ghi trong contract; cần sửa đồng bộ docs.
- Tên tiêu chí examiner ↔ mã rubric (Task 1 `TA`) phải kiểm khi lập plan; tiêu chí không map được → bỏ qua, không lỗi.
- Evidence AI của Practice essay dùng ref khác Lesson; xóa-thay cần ref xác định được — kiểm khi lập plan.
- Free-writing không có `passBand`/KP câu hỏi như lesson? → kiểm; nếu không có `passBand` thì bỏ phần tiêu chí, giữ mã lỗi.
- Trễ tới 48 h và chỉ learner Premium; tác động mastery nhỏ về số lượng.
- Bộ luyện bẫy Writing (V80, V83) là nội dung AI soạn, "examiner must review before real learners".
- Event at-least-once: idempotent theo `reviewSubmissionId` và ref evidence.

## 6. Tiêu chí thành công
1. Examiner chấm CC band < passBand → KP `DEMO_WRITING_CRITERION_CC` có evidence `human_review` sai, evidence AI của cùng bài cho KP đó bị thay; review `WRITING_CRITERION` tạo nếu còn bộ luyện.
2. Examiner đánh dấu `LINKER_MISUSE` → KP con `TRAP_LINKER_MISUSE` có 1 evidence `error_trap` sai; review `ERROR_PATTERN` cho `DEMO_WRITING_LINKING` với bộ có câu bẫy.
3. Nhận lại cùng event → không thêm evidence, không thêm review.
4. Mã không có KP con, tiêu chí không map được → bỏ qua, không lỗi.
5. Bài free-writing gửi thẳng examiner → evidence mã lỗi + review qua lesson đã dạy KP cha (nếu có).
6. Learner thấy "Cần ôn" trên trang kết quả.
7. Sau migration Content (đã duyệt), `ARTICLE` có KP con bẫy và tạo evidence.

## 7. Ngoài phạm vi
Examiner chọn KP con; bài ôn sinh từ đoạn sai của learner; bài thi course/mock (đi qua `assessment-completed-v2`); Speaking;
soạn bộ luyện bẫy cho mã ngữ pháp.

## Câu hỏi còn mở
1. Mapping mã ngữ pháp ở 4.4 cần chuyên gia IELTS duyệt (đặc biệt `TENSE`, `SENTENCE_FRAGMENT`).
2. Trọng số `human_review` 1.0 (như bài thi) — xác nhận.
