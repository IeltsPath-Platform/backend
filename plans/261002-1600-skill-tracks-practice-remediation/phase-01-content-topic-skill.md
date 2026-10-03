# P1 – Content: skill cho topic, topic Writing riêng, topic-sequence mới

Service: `content-service`. Phụ thuộc: không. Kết quả: mỗi topic có lesson đều có đúng một skill; không lesson nào trộn skill.

## 1. Migration `V15__topic_skill_and_writing_topic.sql`

### 1.1 Cột mới

```sql
ALTER TABLE topics ADD COLUMN skill VARCHAR(20)
    CHECK (skill IN ('LISTENING', 'READING', 'WRITING', 'SPEAKING'));
CREATE INDEX idx_topics_skill_sort ON topics (skill, sort_order);
```

Cột **nullable** (topic chỉ chứa KP, không có lesson, có thể chưa có skill). Ràng buộc "topic có lesson PUBLISHED phải
có skill" được kiểm ở cuối migration (1.4) và ở use case publish lesson (mục 3).

### 1.2 Backfill topic seed (ID cố định trong V4/V6/V9/V12/V14)

| Topic code | Skill |
| --- | --- |
| `DEMO_READING` (`10000000-0000-4000-8000-000000000001`) | READING |
| `TFNG_SKILLS` (`20000000-0000-4000-8000-010000000001`) | READING |
| `DEMO_LISTENING` (`23000000-0000-4000-8000-010000000001`) | LISTENING |
| `PREMIUM_MATCHING_INFO`, `PREMIUM_SENTENCE_COMPLETION` (`24000000-…-010000000001/2`) | READING |
| Topic khác | Suy từ `knowledge_points.skill` nếu mọi KP ACTIVE của topic cùng một skill (bỏ qua NULL/`ALL`); ngược lại để NULL. |

Viết theo `code` (không theo ID) để chạy được cả trên DB không có seed.

### 1.3 Tách Writing khỏi `DEMO_READING`

Hiện trạng: KP6 `DEMO_READING_W1_CHART` (`22000000-0000-4000-8000-020000000006`, V11) có TEXT block
`22000000-0000-4000-8000-040000000001` (sort 4) + EXERCISE essay `22000000-0000-4000-8000-040000000002` (sort 5) ở
**L3** (`20000000-0000-4000-8000-030000000003`). KP7 `DEMO_READING_W2_OPINION` (`21000000-0000-4000-8000-020000000007`,
V10) có TEXT `21000000-0000-4000-8000-040000000001` + essay `21000000-0000-4000-8000-040000000002` ở **L4**
(`20000000-0000-4000-8000-030000000004`). Codex phải mở V10/V11 xác nhận lại ID trước khi viết SQL.

Làm (chỉ khi các bản ghi seed tồn tại – bọc bằng `WHERE EXISTS`/`DO $$ … $$`):

1. Tạo topic `DEMO_WRITING` (`25000000-0000-4000-8000-010000000001`, name "Writing cơ bản", sort_order 950, ACTIVE,
   skill WRITING).
2. Tạo lesson PUBLISHED `W1` "Mô tả biểu đồ (Task 1)" (`25000000-0000-4000-8000-030000000001`, sort 1) và `W2`
   "Luận quan điểm (Task 2)" (`25000000-0000-4000-8000-030000000002`, sort 2) thuộc `DEMO_WRITING`. Dùng đúng các cột
   bắt buộc của `lessons` như V9 (kiểm tra `published_at`/version nếu V8 yêu cầu).
3. `UPDATE lesson_blocks SET lesson_id = W1, sort_order = 1/2` cho 2 block của KP6; tương tự W2 cho KP7.
   **Giữ nguyên block ID** (Learning lưu `block_id` trong `lesson_writing_submissions` và `passed_block_ids`).
4. `UPDATE knowledge_points SET topic_id = DEMO_WRITING` cho KP6, KP7. Đổi `code`? **Không** (Learning/Assessment có
   thể tham chiếu code trong log/test); chỉ đổi `name` nếu cần.
5. `lesson_knowledge_points`: xoá (L3, KP6), (L4, KP7); thêm (W1, KP6), (W2, KP7).
6. Không tạo TOPIC_TEST cho `DEMO_WRITING` (D2).

### 1.4 Kiểm tra cuối migration

`DO $$` raise exception nếu tồn tại topic ACTIVE có lesson PUBLISHED mà `skill IS NULL`.

## 2. Domain / API

| Thay đổi | File |
| --- | --- |
| `Topic` aggregate thêm `skill` (enum `Skill`, chỉ cho phép L/R/W/S, không `ALL`). | `domain/aggregate/Topic.java` |
| JPA entity/mapper thêm cột. | `TopicJpaEntity`, `TopicPersistenceMapper` |
| `CreateTopicRequest`/`UpdateTopicRequest` thêm `skill` (optional; validate không nhận `ALL`). Response `TopicResponse`, `TopicTreeResponse` thêm `skill`. | `api/dto/...`, `CreateTopicUseCase`, `UpdateTopicUseCase` |
| Đổi skill của topic đã có lesson PUBLISHED ⇒ 409 `TOPIC_SKILL_LOCKED`. | `UpdateTopicUseCase` |

## 3. Validation lesson theo skill

Content **không có** API tạo/publish lesson (lesson chỉ đến từ migration seed; use case lesson hiện có chỉ đọc:
`GetLessonContentUseCase`, `GetTopicLessonsUseCase`). Không tạo API authoring mới. Ràng buộc được giữ bằng:

- kiểm tra cuối V15 (mục 1.4) và một test nhất quán chạy trên DB sau mọi migration:
  - topic có lesson PUBLISHED phải có skill;
  - mọi question version trong lesson PUBLISHED có `questions.skill = topics.skill`;
  - `lesson_knowledge_points` của lesson chỉ chứa KP có `skill` = topic skill hoặc NULL;
- seed tương lai phải qua được test này (ghi vào README content).

## 4. Internal `GET /internal/learning-content/topic-sequence`

SQL ở `JdbcLearningContentReader.topicSequence` (dòng ~86):

- bỏ điều kiện bắt buộc TOPIC_TEST; thay bằng `skill IS NOT NULL` + có lesson PUBLISHED;
- trả thêm `skill` và `hasTopicTest` (EXISTS TOPIC_TEST PUBLISHED có current version);
- `ORDER BY t.skill, t.sort_order, t.id`.

Cập nhật `TopicSequenceResult`, `TopicSequenceResponse`. Field mới là **thêm**, không đổi field cũ.

## 5. Test

- Migration test (Testcontainers, cùng kiểu test hiện có của content): sau V15
  - mọi topic có lesson có skill;
  - L3/L4 không còn block essay; W1/W2 có đúng 2 block, cùng block ID cũ;
  - `lesson_knowledge_points` như mục 1.3.5.
- Test nhất quán: với mọi lesson PUBLISHED, mọi question trong block có `skill` = topic skill.
- `topicSequence` trả `DEMO_WRITING` với `hasTopicTest=false`, `DEMO_READING` với `hasTopicTest=true`, `skill` đúng.
- Unit test `Topic` từ chối `ALL`; MVC test tạo/sửa topic với `skill`; 409 khi đổi skill topic có lesson.

## Acceptance

`mvn -q -pl services/content-service -am test` xanh. Learning-service cũ vẫn chạy với response mới (chỉ thêm field).

## Verification

Status: completed 2026-10-03 (nhánh `feat/skill-tracks-practice`).

- `mvn -pl services/content-service -am test`: 150 test, 0 fail, 0 skip (Testcontainers chạy V1–V15 thật).
- `mvn -pl services/learning-service -am test`: 198 test xanh (learning chưa đổi; field mới bị bỏ qua khi đọc).
- Code review (subagent): không có lỗi chặn; đã sửa: bỏ query thừa khi skill không đổi, test `ALL` kiểm đúng command,
  hai dòng tài liệu cũ (`answer-spec-v1.md`, `fe-main-flow-guide.md`). `graphify update .` đã chạy.
- ID V15 đối chiếu V9/V10/V11: KP6 `22000000-…-020000000006`, KP7 `21000000-…-020000000007`, block TEXT/essay
  `22000000-…-040000000001/2` → W1, `21000000-…-040000000001/2` → W2; L3, L4 còn 3 block Reading mỗi bài.
- Thứ tự `topic-sequence` mới: DEMO_LISTENING, DEMO_READING, TFNG_SKILLS, PREMIUM_MATCHING_INFO,
  PREMIUM_SENTENCE_COMPLETION, DEMO_WRITING (`hasTopicTest=false`).
- `PUT /api/content/topics/{id}`: `skill` null giữ nguyên (khác `band`: null là xoá); đổi skill khi topic có lesson
  PUBLISHED ⇒ 409 `TOPIC_SKILL_LOCKED`.

Cần chủ dự án duyệt (nội dung học viên thấy): V15 viết lại text 2 block TEXT vì text cũ nhắc bài đọc Reading:
- W1 (`22000000-…-040000000001`): "Writing Task 1 mở bằng câu tổng quan nêu đặc điểm nổi bật nhất của biểu đồ, rồi mới
  đưa số liệu để chứng minh và so sánh."
- W2 (`21000000-…-040000000001`): "Bài luận nêu quan điểm: mở bài nói rõ bạn đồng ý đến mức nào; mỗi đoạn thân bài mở
  bằng một câu chủ đề rồi chứng minh bằng lý do và ví dụ."

Trạng thái tạm (không merge/deploy riêng phase 1): learning hiện tại vẫn xếp một chuỗi theo `sort_order`, nên
`DEMO_WRITING` (950) đứng cuối, sau cả topic premium, và không có topic test ⇒ bài luận chưa tới được cho tới khi P3
xong. Merge cùng P3 trong một PR.
