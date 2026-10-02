# Lộ trình theo kỹ năng, Practice gắn Lesson và thang ôn tập (remediation ladder)

Status: pending
Người implement: Codex. Người viết plan: Claude. Base: `main` @ `d501e29`.

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
| Trượt lần 1 | Tạo review cho KP sai → giao **1 set cùng dạng**, có **hint**, sau khi nộp **luôn hiện giải thích**. Chưa bắt học lý thuyết. |
| Trượt lần 2, **hoặc** điểm < 40% | **Bắt buộc ôn lý thuyết**: chỉ các block lý thuyết dạy **đúng KP sai**, kèm 2–3 câu quick-check → rồi quay lại làm set. |
| Fast-track | Nếu KP đó **đã sai ngay ở bài tập trong lesson** (lần nộp đầu) → trượt lần 1 cũng đi thẳng vào lý thuyết. |
| Trượt lần 3 | **Không khoá vĩnh viễn**: review → `SKIPPED` (KP đánh dấu yếu), learner đi tiếp. |
| Sai nhiều KP | Mỗi KP một review, ôn lần lượt (cũ nhất trước). |
| Phạm vi khoá | Review đang chờ chỉ khoá lesson/practice/topic-test **cùng skill**. Skill khác học bình thường. |

"Lần trượt" đếm theo chuỗi: lần trượt Practice gốc (hoặc bài tập lesson / assessment gây ra review) là lần 1,
set ôn đầu tiên trượt là lần 2, set ôn sau lý thuyết trượt là lần 3 ⇒ `MAX_FAILED_REVIEW_SETS` đổi từ 3 thành **2**.

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
- **D3 – Ánh xạ block → KP.** Bảng mới `lesson_block_knowledge_points` cho block TEXT/ASSET (và tuỳ chọn
  VOCABULARY). Block EXERCISE suy KP từ question mapping. Internal lesson DTO trả `knowledgePointIds` cho mọi block.
  Fallback khi lesson không có mapping cho KP: dùng mọi TEXT/ASSET block của lesson (hành vi cũ).
- **D4 – Practice gắn lesson.** `content_packages.lesson_id` (nullable, chỉ hợp lệ khi `package_type='PRACTICE_SET'`).
  Practice của một lesson = các PRACTICE_SET PUBLISHED có `lesson_id` đó.
- **D5 – Lộ trình theo skill.** Mỗi skill có một chuỗi topic riêng; trong mỗi skill topic chưa PASS đầu tiên là
  `IN_PROGRESS`. Thứ tự trong skill vẫn theo `topics.sort_order`.
- **D6 – Review khoá theo skill.** `review_items.skill` = skill của topic chứa lesson dạy KP (lesson `review.lessonId`).
  Review chờ chỉ chặn lesson, practice, topic test **cùng skill**. Review cũ có `skill IS NULL` vẫn chặn mọi skill
  (an toàn) cho tới khi được backfill.
- **D7 – Practice attempt.** Learner tự chọn practice set của lesson đã COMPLETED. Không bắt buộc làm Practice để
  mở topic test (MVP). Chỉ **attempt nộp đầu tiên của mỗi package** ghi evidence (`practice_set`); các lần sau vẫn
  chấm và hiện lời giải nhưng không ghi evidence. Sau khi nộp luôn hiện đáp án + giải thích.
- **D8 – Thang ôn tập.** `review_items.stage` (`PRACTICE|THEORY`) + `theory_reason`. Chuyển sang THEORY khi:
  (a) set ôn trượt lần đầu, (b) set ôn < 40%, (c) khi review được tạo từ Practice/assessment mà KP đã sai trong first-submission bài
  tập của lesson dạy KP (review tạo từ chính lesson thì bắt đầu ở PRACTICE), (d) khi tạo review từ practice attempt có điểm < 40%. Hoàn thành quick-check (đúng hay sai) ⇒ về PRACTICE;
  quick-check **không ghi evidence**. `MAX_FAILED_REVIEW_SETS = 2`.
- **D9 – Set ôn có hint + giải thích.** `GET /reviews/{id}` trả `hint` của câu hỏi; `POST .../submissions` luôn trả
  `correctAnswer` + `explanation` (kể cả khi trượt). Package đã dùng (ôn hoặc practice) được rotation tránh trước.
- **D10 – Không đổi** công thức mastery, ngưỡng tạo review (`learning.review-mastery-threshold=0.6`), PassMark 70%,
  luồng Writing LLM, luồng assessment consumer (ngoài việc gắn skill cho review).

## 4. Phases

| Phase | Service | Nội dung | File |
| --- | --- | --- | --- |
| P1 | content | `topics.skill`, topic `DEMO_WRITING`, tách essay khỏi Reading, `topic-sequence` mới | [phase-01](phase-01-content-topic-skill.md) |
| P2 | content | `lesson_block_knowledge_points`, `content_packages.lesson_id`, API practice theo lesson | [phase-02](phase-02-content-block-kp-practice-link.md) |
| P3 | learning | Lộ trình theo skill, gate review theo skill, topic không có test | [phase-03](phase-03-learning-skill-tracks.md) |
| P4 | learning | Catalog Practice + practice attempts | [phase-04](phase-04-learning-practice-attempts.md) |
| P5 | learning | Thang ôn tập: stage, lý thuyết theo KP, quick-check, hint/giải thích | [phase-05](phase-05-learning-remediation-ladder.md) |
| P6 | cả hai | Contract, README, E2E runbook, graphify | [phase-06](phase-06-contracts-docs-e2e.md) |

Thứ tự bắt buộc: P1 → P2 → P3 → P4 → P5 → P6. P3 cần P1 (skill trong topic-sequence). P4 cần P2. P5 cần P2 + P4.
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

## 7. Ngoài phạm vi

Mock Test 4 kỹ năng (seed, learner flow, chấm Writing/Speaking trong assessment); chấm Speaking; Practice cho Writing
(essay LLM) và Speaking; ôn tập lặp lại theo thời gian (spaced repetition); `/learning/today`; frontend.

## 8. Tiêu chí hoàn thành tổng

- `GET /api/learning/topics` trả topic kèm `skill`; mỗi skill có tối đa một topic `IN_PROGRESS`.
- Learner có thể học song song Reading và Listening; review Reading đang chờ không chặn Listening.
- Lesson L3/L4 chỉ còn nội dung Reading; Writing nằm ở topic `DEMO_WRITING` (W1, W2) và topic này PASS khi xong lesson.
- `GET /api/learning/lessons/{id}/practice-sets` trả LOCKED trước khi lesson xong, AVAILABLE sau đó; làm và nộp được.
- Kịch bản thang ôn tập ở P5 (fail 1 → set có hint, fail 2 / <40% / fast-track → lý thuyết đúng KP + quick-check,
  fail 3 → SKIPPED) có integration test và đi được trong E2E runbook.
- `mvn -q -pl services/content-service,services/learning-service -am test` xanh.

## Verification

(Codex điền sau khi xong từng phase.)
