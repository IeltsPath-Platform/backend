# Lộ trình theo kỹ năng, Practice gắn Lesson và thang ôn tập (remediation ladder)

Status: in progress (P1, P2 xong 2026-10-03, nhánh `feat/skill-tracks-practice`)
Người implement: Codex. Người viết plan: Claude. Base: `main` @ `c00993d` (đã validate 2026-10-03).

## 1. Mục tiêu nghiệp vụ (đã chốt với chủ dự án)

| Khái niệm | Định nghĩa đích |
| --- | --- |
| **Lesson** | Thuộc đúng **một skill** (Listening / Reading / Writing / Speaking). Trọng tâm lý thuyết + ví dụ + vài bài tập ngắn trong bài. |
| **Practice** | Cũng thuộc một skill và **gắn với một lesson**. Sâu hơn, tập trung một dạng câu hỏi / task. **Chỉ mở khi lesson đó đã hoàn thành.** |
| **Mock Test** | Thi giả lập đủ 4 kỹ năng. **Ngoài phạm vi plan này** (plan riêng sau). |

Thang ôn tập khi Practice yếu (áp dụng cho từng KP sai):

| Tình huống | Hệ thống làm gì |
| --- | --- |
| Practice ≥ 70% | Đạt. Không tạo ôn tập. |
| Trượt lần 1 (Practice < 70%) | Tạo review **không xét mastery** cho mỗi KP có tỉ lệ đúng < 70% **trong chính attempt đó**; chỉ ở **lần nộp đầu** của package; KP chưa có review chờ và còn package chưa lộ (hết thì không tạo). Review giao **1 set cùng dạng**, có **hint**, sau khi nộp **luôn hiện giải thích**. Chưa bắt học lý thuyết. |
| Hoàn thành lesson | **Không tạo review nữa.** Lỗi ở first-submission bài tập lesson chỉ ghi evidence và dùng cho fast-track. |
| Assessment / topic test | Giữ luật cũ: KP sai + mastery < 0.6 + có practice set + chưa có review chờ. |
| Trượt lần 2, **hoặc** điểm < 40% | **Bắt buộc ôn lý thuyết**: chỉ các block lý thuyết dạy **đúng KP sai**, kèm 2–3 câu quick-check → rồi quay lại làm set. |
| Fast-track | Nếu KP đó **đã sai ngay ở bài tập trong lesson** (lần nộp đầu) → trượt lần 1 cũng đi thẳng vào lý thuyết. |
| Trượt lần 3 | **Không khoá vĩnh viễn**: review → `SKIPPED` (KP đánh dấu yếu), learner đi tiếp. |
| Sai nhiều KP | Mỗi KP một review, ôn lần lượt (cũ nhất trước). |
| Phạm vi khoá | Review đang chờ chỉ khoá lesson/practice/topic-test **cùng skill**. Skill khác học bình thường. |

"Lần trượt" đếm theo chuỗi: lần trượt Practice gốc (hoặc assessment gây ra review) là lần 1, set ôn đầu tiên trượt
là lần 2, set ôn sau lý thuyết trượt là lần 3 ⇒ `MAX_FAILED_REVIEW_SETS` đổi từ 3 thành **2**.

Điều kiện thi topic test: mọi lesson PUBLISHED của topic phải **"qua Practice"** (D13).

## 2. Hiện trạng trên main và khoảng trống

| Hiện trạng (main) | Khoảng trống | Phase sửa |
| --- | --- | --- |
| `topics` không có `skill`; chỉ `knowledge_points.skill`/`questions.skill`. | Không biết lesson thuộc skill nào. | P1 |
| Essay Writing (KP6/KP7) nằm cuối L3/L4 của topic `DEMO_READING`. | Lesson trộn skill. | P1 |
| `topic-sequence` chỉ trả topic có TOPIC_TEST. | Topic Writing không có test tự chấm ⇒ không thể vào lộ trình. | P1, P3 |
| `TopicStatusDeriver`: một chuỗi topic duy nhất xuyên mọi skill. | Phải học xong Reading mới tới Listening. | P3 |
| `lesson_knowledge_points` chỉ ở mức lesson. | Không biết block lý thuyết nào dạy KP nào. | P2 |
| `PRACTICE_SET` chỉ dùng làm set ôn tập, chỉ gắn KP. | Không có catalog Practice cho learner, không gắn lesson. | P2, P4 |
| Review chờ chặn **mọi** lesson và topic test (`LessonAccessGate`, `AssignTopicTestUseCase`, `GetTopicLessonsUseCase`). | Khoá xuyên skill. | P3 |
| `GetReviewUseCase` trả **mọi** TEXT block của lesson; set ôn hiện lời giải chỉ khi đạt. | Không có thang PRACTICE→THEORY; không ôn đúng phần KP. | P5 |
| `ReviewItem` SKIPPED sau 3 set trượt. | Không có stage, không fast-track, không ngưỡng 40%. | P5 |

## 3. Quyết định thiết kế (Codex không được tự đổi)

- **D1 – Skill nằm ở topic.** `topics.skill` (`LISTENING|READING|WRITING|SPEAKING`, NOT NULL sau backfill). Lesson
  kế thừa skill từ topic. Câu hỏi trong lesson phải cùng skill với topic (validation + test).
- **D2 – Topic không có TOPIC_TEST vẫn vào lộ trình.** `topic-sequence` trả `skill`, `hasTopicTest`. Topic
  `hasTopicTest=false` được PASS khi hoàn thành mọi lesson (dùng cho Writing; Speaking sau này).
- **D3 – Ánh xạ block → KP.** Bảng mới `lesson_block_knowledge_points`, **chỉ cho block TEXT** (lý thuyết ôn tập
  hiện chỉ lấy TEXT; ASSET là passage/audio của bài tập, đưa vào ôn sẽ lộ transcript). Block EXERCISE suy KP từ
  question mapping. Internal lesson DTO trả `knowledgePointIds` cho mọi block. Fallback khi lesson không có mapping
  cho KP: dùng mọi TEXT block của lesson (hành vi cũ).
- **D4 – Practice gắn lesson.** `content_packages.lesson_id` (nullable, chỉ hợp lệ khi `package_type='PRACTICE_SET'`).
  Practice của một lesson = các PRACTICE_SET PUBLISHED có `lesson_id` đó.
- **D5 – Lộ trình theo skill.** Mỗi skill có một chuỗi topic riêng; trong mỗi skill topic chưa PASS đầu tiên là
  `IN_PROGRESS`. Thứ tự trong skill vẫn theo `topics.sort_order`.
- **D6 – Review khoá theo skill.** `review_items.skill` = skill của topic chứa lesson dạy KP (lesson `review.lessonId`).
  Review chờ chỉ chặn lesson, practice, topic test **cùng skill**. Review cũ có `skill IS NULL` vẫn chặn mọi skill
  (an toàn) cho tới khi được backfill.
- **D7 – Practice attempt.** Learner tự chọn practice set của lesson đã COMPLETED. Practice **bắt buộc** để mở topic
  test (D13). Chỉ **attempt nộp đầu tiên của mỗi package** ghi evidence (`practice_set`); các lần sau vẫn
  chấm và hiện lời giải nhưng không ghi evidence. Sau khi nộp luôn hiện đáp án + giải thích (D11). Premium: Learning
  **không** kiểm gói của learner (giống lesson hiện nay, contract ghi "Learning does not check the learner's plan
  yet"); chỉ trả `accessLevel`.
- **D8 – Thang ôn tập.** `review_items.stage` (`PRACTICE|THEORY`) + `theory_reason`. Review chỉ còn hai nguồn:
  Practice (luật mục 1, không xét mastery) và assessment (luật mastery cũ). Chuyển sang THEORY khi:
  (a) set ôn trượt lần đầu, (b) set ôn < 40%, (c) review vừa tạo mà KP đã sai trong first-submission bài tập của
  lesson `review.lessonId` (fast-track), (d) review tạo từ practice attempt có tỉ lệ đúng **của KP đó** < 40%.
  Hoàn thành quick-check (đúng hay sai) ⇒ về PRACTICE; quick-check **không ghi evidence**.
  `MAX_FAILED_REVIEW_SETS = 2`. `CompleteLessonUseCase`, `SubmitLessonExerciseUseCase` **bỏ** gọi
  `ReviewReevaluation` (vẫn ghi `kp_evidence` lesson_exercise như cũ).
- **D9 – Set ôn có hint + giải thích.** `GET /reviews/{id}` trả `hint` của câu hỏi; `POST .../submissions` luôn trả
  `correctAnswer` + `explanation` (kể cả khi trượt). Chọn package xem D11.
- **D10 – Không đổi** công thức mastery, ngưỡng tạo review (`learning.review-mastery-threshold=0.6`), PassMark 70%,
  luồng Writing LLM, luồng assessment consumer (ngoài việc gắn skill cho review).
- **D11 – Ngoại lệ lộ lời giải (chủ dự án đã duyệt) + chống gian lận.** AGENTS.md §3.8 cấm lộ `explanation` trước
  khi đạt. Ngoại lệ **chỉ** cho practice attempt và review set, **chỉ sau khi nộp**. Lesson exercise, topic test,
  assessment giữ luật cũ. Bắt buộc kèm:
  - package "đã lộ" với learner = package có practice attempt **đã nộp** hoặc review set **đã nộp** (kể cả set nộp
    trước khi có ngoại lệ, cho đơn giản và an toàn);
  - set ôn **không bao giờ** dùng package đã lộ với chính learner đó; hết package chưa lộ ⇒ review `SKIPPED`
    (bỏ rotation LRU `PackageRotation.leastRecentlyUsed` cho review);
  - practice làm lại được phép nhưng chỉ lần nộp đầu mỗi package ghi evidence (D7).
  P6 sửa câu §3.8 AGENTS.md và contract cho đúng ngoại lệ này.
- **D12 – Kho package dùng chung.** Practice của lesson và set ôn lấy từ cùng kho `PRACTICE_SET` của KP. Vì D11,
  practice làm trước sẽ "tiêu" bớt set ôn. Để thang ôn chạy đủ (1 practice + 2 set ôn), KP được demo phải có
  ≥ 3 package: P2 seed thêm 2 package cho KP của PS-KP1 (Reading L1). **Listening không seed package mới** (không có
  mp3 mới): PS-NUM/SPELL/PARA/TRAP chỉ được gắn `lesson_id`; mỗi KP Listening chỉ có 1 đề nên Practice Listening trượt
  sẽ **không tạo review** (hết đề chưa lộ). Ghi vào README content mục "Còn thiếu". KP khác ít package thì review
  SKIPPED sớm hơn hoặc không được tạo – chấp nhận, ghi trong contract.
- **D13 – Topic test cần "qua Practice".** Topic test (`AssignTopicTestUseCase`, `testStatus`) chỉ mở khi mọi
  lesson PUBLISHED của topic đã COMPLETED **và** `practiceStatus = PASSED`. Lesson PASSED với `practicePassReason`:
  1. `FIRST_SUBMISSION` – một practice set của lesson đạt ≥ 70% **ở lần nộp đầu** của package đó (làm lại không tính);
  2. `REVIEW_FINISHED` – một review sinh từ practice attempt của lesson (`trigger_kind='PRACTICE'`, `lesson_id` =
     lesson) đã kết thúc `DONE` hoặc `SKIPPED`;
  3. `ALL_SETS_ATTEMPTED` (chống kẹt) – **mỗi** package PRACTICE_SET PUBLISHED đang gắn lesson có ≥ 1 attempt đã nộp
     **và** không có review PENDING nào tạo từ Practice của lesson này;
  4. `NO_PRACTICE` – lesson không có package PRACTICE_SET PUBLISHED nào (Writing, lesson tương lai chưa có đề).
  **Đơn điệu**: lần đầu thoả điều kiện thì lưu vào bảng `lesson_practice_passes` (P4) và không bao giờ quay lại chưa
  qua; package mới seed sau chỉ ảnh hưởng lesson chưa qua. Tính bằng query theo tập lesson/package, không query trong
  vòng lặp. Mọi lesson thuộc topic có TOPIC_TEST phải có ≥ 1 practice set: P2 seed thêm cho TF1, PM1, PM2, PS1, PS2
  (và lesson nào khác thiếu sau backfill). Topic không có test (Writing) không áp luật này. Lỗi khi chưa đủ: 409
  `PRACTICE_REQUIRED` (sau `REVIEW_REQUIRED`, trước `TEST_LOCKED`).
- **D14 – Chỉ có dữ liệu demo.** Không có bước chuyển tiếp cho người dùng thật: tiến độ và review cũ áp luật mới
  ngay (kể cả topic test đòi Practice với lesson đã COMPLETED trước nâng cấp).
- **D15 – Nội dung đề mới do Codex viết**, theo mẫu seed hiện có; liệt kê toàn văn từng câu (stem, đáp án, giải
  thích, hint) trong Verification của P2 để chủ dự án duyệt trong PR.
- **D16 – Mỗi câu hỏi chỉ thuộc một nơi dùng** (một lesson hoặc một package `PRACTICE_SET`/`TOPIC_TEST`/`MOCK_TEST`/
  `PLACEMENT_TEST`). Dùng chung ngân hàng câu hỏi, không tách bảng cho thi thử, không trigger DB: kiểm tra khi publish
  (`422 QUESTION_ALREADY_USED`) và bằng test seed. Thêm cột `questions.purpose` (`LEARNING`/`EXAM`, migration V17):
  lesson, Practice, thi cuối topic dùng câu `LEARNING`; thi thử, thi xếp lớp dùng câu `EXAM`
  (`422 QUESTION_PURPOSE_MISMATCH`) (P2b).

## 4. Phases

| Phase | Service | Nội dung | File |
| --- | --- | --- | --- |
| P1 ✓ | content | `topics.skill`, topic `DEMO_WRITING`, tách essay khỏi Reading, `topic-sequence` mới | [phase-01](phase-01-content-topic-skill.md) |
| P2 ✓ | content | `lesson_block_knowledge_points`, `content_packages.lesson_id`, API practice theo lesson | [phase-02](phase-02-content-block-kp-practice-link.md) |
| P2b ✓ | content | Mỗi câu hỏi một chủ; `questions.purpose` phân biệt học/thi, chặn sai loại khi publish | [phase-02b](phase-02b-content-question-exclusivity.md) |
| P3 | learning | Lộ trình theo skill, gate review theo skill, topic không có test | [phase-03](phase-03-learning-skill-tracks.md) |
| P4 | learning | Catalog Practice + practice attempts | [phase-04](phase-04-learning-practice-attempts.md) |
| P5 | learning | Thang ôn tập: stage, lý thuyết theo KP, quick-check, hint/giải thích | [phase-05](phase-05-learning-remediation-ladder.md) |
| P6 | cả hai | Contract, README, E2E runbook, graphify | [phase-06](phase-06-contracts-docs-e2e.md) |

Thứ tự bắt buộc: P1 → P2 → P2b → P3 → P4 → P5 → P6. P3 cần P1 (skill trong topic-sequence). P4 cần P2. P5 cần P2 + P4.
Mỗi phase là một commit (hoặc vài commit) build xanh độc lập; không gộp phase.

## 5. Đọc trước khi code

| File | Vì sao |
| --- | --- |
| `AGENTS.md`, `CLAUDE.md` | Quy tắc repo, kiến trúc DDD/Clean. |
| `docs/contracts/learning-content-internal-v1.md` | Contract Content↔Learning sẽ đổi ở P1/P2. |
| `docs/contracts/lesson-learning-v1.md` | Contract public learner sẽ đổi ở P3–P5. |
| `services/content-service/src/main/resources/db/migration/V8__…`, `V9__…`–`V14__…` | Schema lesson + seed hiện tại (ID cố định). |
| `services/content-service/.../infrastructure/persistence/adapter/JdbcLearningContentReader.java` | SQL topic-sequence, lesson, practice search. |
| `services/content-service/.../api/controller/InternalLearningContentController.java` | Route internal. |
| `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql`, `V2__lesson_writing.sql` | Schema learning. |
| `services/learning-service/.../domain/service/{TopicStatusDeriver,LessonAccessGate,ReviewRule,PassMark}.java` | Luật cốt lõi sẽ đổi. |
| `services/learning-service/.../domain/aggregate/{LearnerCurriculum,ReviewItem,LessonProgress}.java` | Aggregate sẽ đổi. |
| `services/learning-service/.../application/usecase/*.java`, `application/service/{LessonAccess,ReviewReevaluation}.java` | Điểm tích hợp. |
| `services/learning-service/.../application/port/LearningContentClient.java` | DTO nhận từ Content. |
| `services/learning-service/.../api/dto/response/{ReviewResponse,ReviewSubmissionResponse,SubmissionResponse}.java` | Tên field hiện có phải giữ (`reviewStatus`, `theory: string[]`, `set.reviewSetId`, `results`). |
| `services/learning-service/.../application/service/LessonEvidenceReference.java` | Mỗi nguồn evidence có namespace UUIDv5 riêng (`UNIQUE(user_id, source, source_reference_id)`). |
| `docs/contracts/lesson-writing-v1.md`, `docs/fe-main-flow-guide.md` | Luồng essay bị chuyển topic; tài liệu FE mô tả luật lộ lời giải. |

## 6. Quy tắc cho người implement

1. Không sửa migration đã có (V1–V14 content, V1–V2 learning); chỉ thêm migration mới.
2. Giữ đúng kiến trúc hiện có: luật nghiệp vụ trong `domain/service` / aggregate (thuần Java, có unit test);
   use case một `execute`; DTO response trong `api/dto/response`; JDBC repository trong `infrastructure/persistence`.
3. Mọi endpoint mới đi qua `LearnerLock` (ghi) như use case hiện có, idempotent theo `requestId` khi có nộp bài.
4. Không lộ đáp án trước khi nộp; không đổi shape JSON hiện có ngoài việc **thêm field** (trừ chỗ plan ghi rõ).
5. Không đụng `third_party/`, `.env`, secret. Không thêm dependency mới.
6. Sau mỗi phase: chạy test của service bị đổi, cập nhật mục "Verification" của phase, `graphify update .`.
7. Gặp chỗ plan mâu thuẫn với code: dừng, ghi vào `reports/questions.md`, chọn phương án an toàn nhất (không mở
   rộng quyền truy cập, không mất dữ liệu) và ghi rõ đã chọn gì.
8. Danh sách mới phải có giới hạn (`limit` mặc định 20, tối đa 100), không gọi client/repository trong vòng lặp.
9. Commit không nhắc AI/agent, số phase, mã plan; tên test/migration/comment mô tả hành vi, không mô tả phase.

## Validation 2026-10-03

Đối chiếu plan với code main `c00993d` và bối cảnh giao cho agent. Đã sửa trong plan:

| Lệch | Sửa |
| --- | --- |
| Plan cho rotation LRU dùng lại package; bối cảnh cấm dùng lại package đã lộ lời giải. | D11, P5 §1.4: không dùng lại, hết ⇒ SKIPPED. |
| Practice và set ôn chung kho; seed chỉ 1–2 package/KP ⇒ thang ôn không chạy đủ. | D12, P2 §1.3 seed thêm. |
| P4 kiểm entitlement qua `AccessClient`; port này chỉ có `balance`/`debit`, Learning chưa kiểm gói. | D7, P4 luật 3: chỉ trả `accessLevel`. |
| P5 đổi `theory` thành object, dùng `status`/`items`; DTO thật là `reviewStatus`, `theory: List<String>`, `results`. | P5 §5: chỉ thêm field, giữ tên cũ. |
| Theory gồm ASSET ⇒ có thể lộ transcript Listening. | D3: mapping và theory chỉ TEXT. |
| P1/P2 nhắc use case publish lesson / authoring block; content không có (lesson chỉ seed). | P1 §3, P2 §4: kiểm bằng test + migration; chỉ `CreateContentPackage` nhận `lessonId`. |
| Evidence practice cần reference riêng, nếu không đụng `UNIQUE(user_id, source, source_reference_id)` hoặc trùng namespace. | P4 §3: `LessonEvidenceReference.forPracticeSet`. |
| Learning V3 sửa `passed_block_ids` của essay; essay không bao giờ vào `passed_block_ids`, KP của lesson tự cập nhật qua `LessonProgress.place`. | P3 §1: chỉ cập nhật `lesson_writing_submissions.lesson_id`, `review_items.lesson_id`. |
| `GET /reviews` không có giới hạn. | P3 §4: `limit`. |
| AGENTS.md §3.8, contract dòng 9/106/124, `fe-main-flow-guide.md` nói lời giải chỉ hiện khi đạt. | P6 §1–2. |

Chủ dự án chốt thêm (2026-10-03): Practice < 70% tạo review không xét mastery (theo KP < 70% trong attempt, lần
nộp đầu, còn package chưa lộ); bỏ review khi hoàn thành lesson; hiện lời giải sau khi nộp + không dùng lại đề; topic
test cần mọi lesson qua Practice (lần nộp đầu đạt hoặc review xong); seed đề cho 5 lesson chưa có Practice. ⇒ D7, D8,
D13, mục 1, P2 §1.3, P3 test, P4 §1, P5 §1.1, P6.

Chốt lần 2 (2026-10-03): giữ điều kiện chống kẹt với định nghĩa và `practicePassReason` như D13 (đơn điệu); không seed
Listening mới (D12); Codex viết đề, chủ dự án duyệt PR (D15); chỉ dữ liệu demo (D14). Test seed hiện có phải sửa có
chủ đích: `LessonPipelineSeedTest` (dòng ~197–221 đang khẳng định KP5 không có practice set đủ 3 câu) và snapshot
`src/test/resources/seed/lesson-pipeline-demo-expected.json` (essay chuyển topic, `lesson_id` của package).
Giao code: nhánh `feat/skill-tracks-practice`, mỗi phase một commit, một PR vào `main`.

Đã kiểm, khớp: `/complete` chạy được cho lesson chỉ có TEXT + essay (`Block.isExercise` loại essay); Gateway
`internal-jwt-paths` và springdoc `paths-to-match` đã phủ `/api/learning/**`; migration kế tiếp đúng là content V15,
learning V3; block ID essay giữ nguyên không vướng `uq_lesson_blocks_sort_order` nếu đặt sort 1/2 trong W1/W2.

## 7. Ngoài phạm vi

Mock Test 4 kỹ năng (seed, learner flow, giao mã đề chưa làm, quy đổi band, chấm Writing/Speaking trong assessment;
riêng luật chặn trùng câu hỏi đã nằm ở P2b); chấm Speaking; Practice cho Writing
(essay LLM) và Speaking; ôn tập lặp lại theo thời gian (spaced repetition); `/learning/today`; frontend.

## 8. Tiêu chí hoàn thành tổng

- `GET /api/learning/topics` trả topic kèm `skill`; mỗi skill có tối đa một topic `IN_PROGRESS`.
- Learner có thể học song song Reading và Listening; review Reading đang chờ không chặn Listening.
- Lesson L3/L4 chỉ còn nội dung Reading; Writing nằm ở topic `DEMO_WRITING` (W1, W2) và topic này PASS khi xong lesson.
- `GET /api/learning/lessons/{id}/practice-sets` trả LOCKED trước khi lesson xong, AVAILABLE sau đó; làm và nộp được.
- Hoàn thành lesson không còn tạo review; topic test trả 409 `PRACTICE_REQUIRED` tới khi mọi lesson qua Practice.
- Kịch bản thang ôn tập ở P5 (fail 1 → set có hint, fail 2 / <40% / fast-track → lý thuyết đúng KP + quick-check,
  fail 3 → SKIPPED) có integration test và đi được trong E2E runbook.
- `mvn -q -pl services/content-service,services/learning-service -am test` xanh.

## Verification

(Codex điền sau khi xong từng phase.)
