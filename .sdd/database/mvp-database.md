# Database cho AI Learning MVP (tạo path, cập nhật path, điều phối làm bài, chấm Writing)

**Trạng thái:** thiết kế mục tiêu, chưa phải schema đang chạy. Tại thời điểm viết, migration của service mới đến `V9` và chưa có các bảng học theo bài ở dưới. Schema hiện tại được mô tả trong [tài liệu 17 bảng](../../docs/ai-learning-database.md); quyết định luồng học nằm trong [plan MVP](../../plans/260929-1640-lesson-learning-pipeline-mvp/plan.md) và [lộ trình MVP](../../plans/260930-2057-mvp-reading-writing-listening-roadmap/plan.md).

Danh sách cột chính thức ở `DATABASE_V5.md` §7.21–§7.26; tài liệu này là hướng dẫn luồng API ↔ bảng cho MVP.

## Phạm vi MVP

AI Learning chỉ giữ **một PostgreSQL database `ai_learning_db`** để theo dõi path, tính mastery, tiến độ topic/bài, bài ôn và kết quả đề. Content Service sở hữu bài học/câu hỏi/gói nội dung; Assessment Service sở hữu attempt và kết quả chấm. AI Learning lưu ID tham chiếu tới hai service đó, không sao chép toàn bộ nội dung và không tạo FK xuyên database.

AI Learning trong MVP chỉ làm bốn việc: tạo path (một path mỗi học viên, theo `sort_order`, không LLM), cập nhật path (bằng chứng từ bài tập, bài ôn, đề cuối, Writing; chèn bài ôn khi KP yếu), điều phối làm bài (topic → bài → bài ôn → đề cuối, cổng mở bài, giấu đáp án tới khi đạt) và chấm Writing bằng LLM (trừ 3 point qua Access). Kỹ năng: Reading, Writing, Listening. MVP **chia lại service** theo `.sdd/specs/SERVICE_ARCHITECTURE_V3.md` (tách `library-service`, giải thể learning-support); việc này không đổi bảng hay API của AI Learning.

**Tutor không thuộc MVP nhưng không bị gỡ:** code tutor chat, learner memory, practice notebook giữ nguyên và test vẫn pass; app MVP không gọi `/tutor/**`, `/practice/**`, `/status`, `/progress`. **Bài học chính của MVP nằm trong Content Service**; AI Learning chỉ lưu việc học viên đã học và đạt đến đâu. `compute_mastery` vẫn được dùng; lịch ôn của engine (`repetition_states`, `review_queue`) không được cập nhật trong MVP (chốt 2026-10-01).

## Luồng chính: client gửi gì, AI Learning trả gì

Client gọi qua API Gateway với `Authorization: Bearer <token>`. AI Learning lấy `user_id` từ internal JWT đã xác thực; client **không gửi `userId` hay `pathId` trong body**. Khi cần nội dung, AI Learning gọi Content Service và chỉ lưu ID/reference cùng tiến độ. Các JSON dưới đây là ví dụ dễ đọc cho API mục tiêu; contract chi tiết trong phase 1 của plan vẫn pending, nên cần chốt tên trường trước khi code.

```mermaid
sequenceDiagram
    actor H as Học viên / UI
    participant AI as AI Learning
    participant C as Content Service
    participant A as Assessment Service
    H->>AI: GET /topics (Bearer)
    AI->>C: GET topic-sequence (topic + KP, một lần)
    AI-->>H: Topic theo thứ tự + trạng thái
    H->>AI: GET /topics/{topicId}/lessons
    AI->>C: Lấy danh sách bài đã publish
    AI-->>H: Danh sách bài + trạng thái đề cuối
    H->>AI: GET /lessons/{lessonId}
    AI->>C: Lấy bài và câu hỏi (nội bộ)
    AI-->>H: Bài + câu hỏi đã lọc trường nhạy cảm
    H->>AI: POST .../submissions (requestId, answers)
    AI->>AI: Chấm, cập nhật tiến độ/mastery, có thể tạo bài ôn
    AI-->>H: Đúng/sai; lời giải chỉ khi đạt khối
    H->>AI: POST /topics/{topicId}/test-assignments
    AI-->>H: assignmentId + packageVersionId
    H->>A: Tạo attempt với packageVersionId
    A-->>H: Attempt để làm đề cuối
    A-->>AI: AssessmentCompleted.v2 sau khi chấm xong
    AI->>AI: Ghi mastery, đánh dấu mã đề, ghi passed_at nếu đạt
    H->>AI: GET /topics
    AI-->>H: Tiến độ topic đã cập nhật
```

### 1. Mở lộ trình

**Client gửi:**

```http
GET /api/ai-learning/topics
Authorization: Bearer <access-token>
```

AI Learning xác thực học viên, tìm hoặc tạo path, đồng bộ topic sequence từ Content, rồi trả thứ tự và tiến độ. Ví dụ response rút gọn:

Trong flow MVP, `GET /topics` gọi Content **đúng một lần**, forward bearer của request học viên:

```http
GET {CONTENT_SERVICE_BASE_URL}/internal/learning-content/topic-sequence
Authorization: Bearer <internal-jwt>
```

Content chỉ trả topic `ACTIVE` có ít nhất một bài đã publish **và** một mã đề `TOPIC_TEST`, theo `sort_order`; mỗi topic kèm KP `ACTIVE` của nó:

```json
[{"topicId":"T1","code":"DEMO_READING","name":"Demo IELTS Reading","skill":"READING","sortOrder":900,
  "knowledgePoints":[{"id":"K1","code":"DEMO_READING_MAIN_IDEA","name":"Ý chính cả bài",
    "learningType":"PROCEDURE","skill":"READING","description":"…","hasPracticeSet":true}]}]
```

Một response dùng cho ba việc: (1) tạo hoặc gộp path mastery (`CurriculumAdapter`: mỗi topic một module, mỗi KP một knowledge point, lưu trong `mastery_paths.state_json`); (2) ghi snapshot KP vào `mastery_path_knowledge_point_details` (`skill`, `description`, `has_practice_set`); (3) ghi thứ tự học vào `topic_progress.sequence_order`. Không gọi `/api/content/topics`, `/api/content/knowledge-points`, User Service hay LLM; không lọc band. Path chỉ chứa topic học được, nên KP ngoài lộ trình không nhận bằng chứng. Không insert câu hỏi hay nội dung bài học ở bước tạo path.

```json
[
  {"topicId":"T1","sequenceOrder":1,"status":"IN_PROGRESS","completedLessonCount":0},
  {"topicId":"T2","sequenceOrder":2,"status":"LOCKED","completedLessonCount":0}
]
```

`status` là `LOCKED`, `IN_PROGRESS` hoặc `PASSED`, **suy ra khi đọc, không lưu**: `PASSED` nếu `passed_at` có giá trị; topic đầu tiên theo `sequence_order` chưa PASSED là `IN_PROGRESS`; còn lại `LOCKED`. Client dùng nó để khóa/mở topic; không tự quyết định dựa trên mastery.

App MVP chỉ hiển thị trạng thái topic và bài. `GET /progress` (bản đồ mastery, ngưỡng "đã nắm" 0.9) và `/status` (`next_objective`) là của tutor, app MVP không gọi, để học viên không thấy ba ngưỡng khác nhau (70%, 0.6, 0.9).

### 2. Mở topic và bài học

**Client gửi:**

```http
GET /api/ai-learning/topics/T1/lessons
GET /api/ai-learning/lessons/L1
```

Topic đang khóa trả `403 TOPIC_LOCKED`. Bài học bị khóa bởi bài trước trả `403 LESSON_LOCKED`. Nếu có bài ôn `PENDING`, API trả `403 REVIEW_REQUIRED` kèm danh sách review cần hoàn thành.

AI Learning lấy nội dung từ Content. Ví dụ response bài học chỉ chứa trường hiển thị:

```json
{
  "lessonId":"L1",
  "title":"Past tense",
  "blocks":[
    {"blockId":"B1","type":"TEXT","content":"..."},
    {"blockId":"B2","type":"EXERCISE","questions":[
      {"questionVersionId":"Q1","sortOrder":1,"stem":"She ___ home yesterday.","options":["go","went"]}
    ]}
  ],
  "passedBlockIds":[]
}
```

`answerSpec`, đáp án và lời giải không được gửi trước khi khối đạt. Với khối đã đạt, response có thể kèm `solutions` chứa `correctAnswer` và `explanation`. AI Learning đồng thời ghi snapshot KP/thứ tự bài vào `lesson_progress`.

Response của danh sách bài trong topic có thể hiển thị trạng thái bài và đề cuối, ví dụ:

```json
{
  "topicId":"T1",
  "lessons":[
    {"lessonId":"L1","sortOrder":1,"status":"COMPLETED"},
    {"lessonId":"L2","sortOrder":2,"status":"AVAILABLE"}
  ],
  "finalTestStatus":"LOCKED"
}
```

Nếu bài không có khối `EXERCISE`, client gửi `POST /api/ai-learning/lessons/L3/complete` với body rỗng `{}`. AI Learning áp cùng các cổng truy cập rồi đánh dấu bài hoàn tất; response mục tiêu có thể là `{"lessonId":"L3","completed":true}`.

### 3. Nộp bài tập trong bài học

**Client gửi:**

```http
POST /api/ai-learning/lessons/L1/exercises/B2/submissions
Content-Type: application/json
```

```json
{
  "requestId":"99999999-9999-4999-8999-999999999999",
  "answers":[
    {"questionVersionId":"Q1","answer":"went"},
    {"questionVersionId":"Q2","answer":"go"}
  ]
}
```

AI Learning kiểm cổng bài học, chấm bằng `answerSpec` nội bộ, ghi submission và bằng chứng mastery trong cùng transaction. Nếu cùng `requestId` được gửi lại, trả response đã lưu; nếu request ID bị dùng cho nội dung khác, trả `409 REQUEST_CONFLICT`.

**Khối chưa đạt — không lộ đáp án:**

```json
{
  "blockPassed":false,
  "results":[
    {"questionVersionId":"Q1","correct":true},
    {"questionVersionId":"Q2","correct":false}
  ]
}
```

**Khối đạt — mở lời giải:**

```json
{
  "blockPassed":true,
  "results":[
    {"questionVersionId":"Q1","correct":true,"correctAnswer":"went","explanation":"Dùng quá khứ đơn."}
  ]
}
```

Bằng chứng mastery chỉ ghi ở **lần nộp đầu** của mỗi khối; làm lại chỉ để qua khối. Khi tất cả khối tự chấm đạt (khối essay Writing không tính), AI Learning đặt `lesson_progress.completed_at`. Nếu learner sai câu thuộc KP vừa học ở lần nộp đầu, mastery dưới ngưỡng (mặc định `0.6`) và Content có gói luyện cho KP (`has_practice_set`), AI Learning tạo `path_review_items` `PENDING`; bài học/topic tiếp theo bị chặn cho tới khi ôn đạt, hoặc tới khi trượt 3 set (`SKIPPED`).

### 4. Làm bài ôn khi có review bắt buộc

**Client lấy bài ôn:**

```http
GET /api/ai-learning/reviews/R1
```

AI Learning trả lý thuyết từ bài đã học và một gói câu hỏi mới từ Content. Nếu learner chưa đạt gói này, lần GET sau giao gói khác; một gói đang mở sẽ được trả lại như cũ.

Response rút gọn:

```json
{
  "reviewId":"R1",
  "lessonId":"L2",
  "theory":{"blocks":[{"type":"TEXT","content":"Ôn lại thì quá khứ đơn."}]},
  "reviewSet":{"reviewSetId":"RS1","packageVersionId":"RPV1","questions":[
    {"questionVersionId":"RQ1","stem":"They ___ yesterday.","options":["go","went"]}
  ]}
}
```

**Client nộp bài ôn:**

```json
{
  "reviewSetId":"RS1",
  "requestId":"aaaaaaaa-0000-4000-8000-000000000001",
  "answers":[{"questionVersionId":"RQ1","answer":"went"}]
}
```

Gửi tới `POST /api/ai-learning/reviews/R1/submissions`. AI Learning ghi mastery với source `review_set`. Đạt ít nhất 70% thì review chuyển `DONE`; thấp hơn thì giữ `PENDING` để lần sau giao gói khác; trượt set thứ 3 thì `SKIPPED` và học viên học tiếp. Response có `reviewStatus` (`PENDING`, `DONE`, `SKIPPED`). Sai `reviewSetId` hoặc set đã đóng trả `409 REVIEW_SET_CLOSED`.

### 5. Làm đề cuối topic

Sau khi mọi bài của topic hoàn thành và không còn review `PENDING`, client gọi:

```http
POST /api/ai-learning/topics/T1/test-assignments
```

Response:

```json
{"assignmentId":"TA1","packageId":"PK1","packageVersionId":"PV1"}
```

Client dùng `packageVersionId` để yêu cầu Assessment tạo attempt (theo contract plan: `{ "packageVersionId":"PV1", "mode":"STANDARD", "channel":"WEB" }`). `attemptType` được suy ra từ loại gói `TOPIC_TEST`; client không tự gửi loại attempt. Assignment chưa dùng được trả lại nếu client gọi lại; khi đã dùng, lần tiếp theo chọn mã đề khác nếu còn.

Assessment là nơi nhận câu trả lời và chấm đề. Khi kết quả hoàn tất, Assessment phát `AssessmentCompleted.v2`; AI Learning không chấm lại câu trả lời đề ở request client.

### 6. Nhận kết quả đề bất đồng bộ

Consumer nhận event có `user_id`, `attempt_id`, `result_id`, `result_version`, loại assessment, điểm từng câu và các KP mapping. Ví dụ phần dữ liệu rút gọn theo MVP:

```json
{
  "event_type":"AssessmentCompleted.v2",
  "data":{
    "user_id":"U",
    "learning_goal_id":null,
    "attempt_id":"A1",
    "result_id":"RES1",
    "result_version":1,
    "package_version_id":"PV1",
    "assessment_type":"TOPIC_GATE",
    "status":"COMPLETED",
    "completed_at":"2026-09-30T10:00:00Z",
    "item_results":[
      {"item_result_id":"IR1","question_version_id":"Q1","score":1,"max_score":1,"is_correct":true,"knowledge_point_mappings":[{"knowledge_point_id":"K1","weight":1}]},
      {"item_result_id":"IR2","question_version_id":"Q2","score":0,"max_score":1,"is_correct":false,"knowledge_point_mappings":[{"knowledge_point_id":"K2","weight":1}]}
    ]
  }
}
```

Trong một transaction, AI Learning kiểm version để bỏ event trùng/cũ, cập nhật evidence và mastery, đánh dấu assignment đã dùng, rồi tính phần trăm. `TOPIC_GATE` đạt ít nhất 70% thì ghi `topic_progress.passed_at` (một chiều); topic kế tự thành `IN_PROGRESS` vì trạng thái suy ra khi đọc. Nếu không đạt thì topic tiếp tục mở để làm mã đề khác. Consumer ACK event sau khi transaction commit. Client thấy kết quả cập nhật bằng lần `GET /topics` hoặc endpoint tiến độ kế tiếp.

Các lỗi chính trả `{ "detail":"...", "code":"..." }`: `REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED`, `TEST_LOCKED`, `TEST_UNAVAILABLE`, `REQUEST_CONFLICT`, `REVIEW_SET_CLOSED`, `NOT_FOUND`. Lỗi `REVIEW_REQUIRED` có thêm danh sách `reviews` để UI điều hướng.

**Lưu ý về contract:** các ví dụ JSON minh họa request/response mục tiêu, không khẳng định toàn bộ API đã có hoặc body schema đã được duyệt. Nguồn hành vi là [phase API](../../plans/260929-1640-lesson-learning-pipeline-mvp/phase-06-ai-learning-api-bai-hoc-va-bai-on.md), [phase contract](../../plans/260929-1640-lesson-learning-pipeline-mvp/phase-01-contract-va-dac-ta.md) và [phase consumer](../../plans/260929-1640-lesson-learning-pipeline-mvp/phase-07-ai-learning-consumer-ket-qua-de.md).

## Chi tiết dữ liệu theo từng bước

### Ranh giới nộp bài giữa AI Learning và Assessment

MVP có hai luồng nộp bài:

- **Bài tập nằm trong bài học:** `Client → AI Learning`. AI Learning nhận `lessonId`, `blockId`, `requestId` và câu trả lời; lấy `answerSpec`/mapping KP từ Content để chấm; lưu submission, tiến độ và bằng chứng mastery. Luồng này không tạo `assessment_attempt`.
- **Đề kiểm tra cuối topic:** `Client → Assessment`. Assessment nhận câu trả lời, chấm và lưu kết quả chính thức; sau đó phát `AssessmentCompleted.v2` qua RabbitMQ. AI Learning chỉ nhận event kết quả để cập nhật mastery và tiến độ topic.

Vì vậy, cụm “nộp bài” cần hiểu theo đúng loại: bài tập luyện trong lesson đi vào endpoint AI Learning; bài thi có attempt/kết quả chính thức đi vào Assessment. Cách chia này giữ dữ liệu attempt và chấm thi ở Assessment, còn AI Learning điều phối học và gom evidence mastery. Đây là luồng mục tiêu của tài liệu MVP.

### Sequence diagram: học bài và làm đề topic

```mermaid
sequenceDiagram
    autonumber
    actor Learner as Học viên / Client
    participant AI as AI Learning API
    database DB as ai_learning_db
    participant Content as Content Service
    participant Assessment as Assessment Service
    participant MQ as RabbitMQ

    Learner->>AI: GET /topics + JWT
    AI->>Content: GET topic-sequence (một lần)
    Content-->>AI: Topic học được kèm KP
    AI->>DB: Tạo/cập nhật mastery_paths và topic_progress
    DB-->>AI: Trạng thái mastery/topic
    AI-->>Learner: Danh sách topic, trạng thái khóa/mở

    Learner->>AI: GET /topics/T1/lessons
    AI->>DB: Đọc topic_progress và lesson_progress
    AI->>Content: Lấy bài đã publish và thông tin gói đề
    Content-->>AI: Danh sách bài và gói đề
    AI-->>Learner: Danh sách bài, tiến độ, trạng thái đề

    Learner->>AI: GET /lessons/L2
    AI->>DB: Kiểm tra topic, bài trước và review đang chờ
    AI->>Content: Lấy nội dung lesson
    Content-->>AI: Nội dung, blocks, question versions, KP
    AI->>DB: Upsert lesson_progress (thứ tự và KP của bài)
    AI-->>Learner: Lesson đã lọc nội dung theo quyền xem đáp án

    opt Bài tập nằm trong lesson
        Learner->>AI: POST exercise submission (lessonId, blockId, requestId, answers)
        AI->>DB: Mở transaction, khóa mastery_paths, kiểm cổng và requestId
        AI->>Content: Lấy answerSpec và KP mapping của câu hỏi
        Content-->>AI: Answer spec và mapping
        AI->>AI: Chấm các câu trả lời
        AI->>DB: INSERT lesson_exercise_submissions
        AI->>DB: UPDATE lesson_progress (passed blocks, completed_at)
        opt Lần nộp đầu của khối
            AI->>DB: Ghi evidence vào mastery_paths.state_json
            opt Aggregate mastery thay đổi
                AI->>DB: Tăng revision; rebuild mastery_learning_evidence; ghi mastery_events
            end
        end
        opt Bài xong, KP yếu có câu sai và có gói luyện
            AI->>DB: INSERT path_review_items (PENDING)
        end
        AI->>DB: COMMIT transaction
        AI-->>Learner: Kết quả chấm và đáp án/giải thích nếu được xem
    end

    opt Khi đủ điều kiện, làm đề kiểm tra cuối topic
        Learner->>AI: POST /topics/T1/test-assignments
        AI->>DB: Kiểm tra topic, lessons và review; tìm assignment chưa dùng
        AI->>Content: Lấy packageVersion của TOPIC_TEST
        Content-->>AI: packageId và packageVersionId
        AI->>DB: INSERT topic_test_assignments (nếu chưa có assignment mở)
        AI-->>Learner: assignmentId và packageVersionId

        Learner->>Assessment: Tạo attempt và nộp câu trả lời theo packageVersionId
        Assessment->>Assessment: Chấm bài và lưu attempt/result trong Assessment DB
        Assessment-->>MQ: Publish AssessmentCompleted.v2
        MQ-->>AI: Consumer nhận kết quả và KP mapping
        AI->>DB: Mở transaction; kiểm result_version và assignment
        alt mastery_paths của học viên đã tồn tại
            AI->>DB: Cập nhật mastery_paths.state_json và formal evidence
            AI->>DB: Ghi mastery_learning_evidence, mastery_events, formal_assessment_result_versions
            AI->>DB: Đánh dấu assignment đã dùng; ghi topic_progress.passed_at nếu đạt
            opt Có KP yếu cần ôn
                AI->>DB: INSERT path_review_items (PENDING)
            end
        else Chưa có mastery_paths
            AI->>DB: INSERT pending_formal_assessment_results
        end
        AI->>DB: COMMIT transaction
        AI-->>MQ: ACK event
        Learner->>AI: GET /topics
        AI-->>Learner: Trạng thái topic mới nhất
    end
```

Trong bảng dưới, “đọc/ghi” nói rõ database AI Learning và service ngoài. Tất cả route học viên đều xác định `U` từ JWT; không tin `userId` do client tự gửi. Những request ghi trạng thái dùng path transaction để trạng thái bài, mastery và kết quả request không bị commit dở dang.

| Bước | Input AI Learning nhận | Đọc/gọi | Ghi vào `ai_learning_db` | Output / kết quả |
| --- | --- | --- | --- | --- |
| **1. `GET /topics`** | JWT; không body. Danh tính học viên `U`. | Mỗi lần gọi, gọi Content một lần `topic-sequence` (topic + KP); đọc pending Assessment results đang chờ nếu có. | Tạo hoặc refresh `mastery_paths` (JSON mastery + revision); ghi `mastery_events` khi aggregate đổi; upsert `topic_progress` theo sequence. Nếu path được tạo và có kết quả thi đang chờ, áp chúng rồi xóa pending trong cùng transaction. Ghi snapshot `mastery_path_knowledge_point_details` (`skill`, `description`, `has_practice_set`); luật chèn bài ôn đọc `has_practice_set`. | Topic theo thứ tự, mỗi topic có `status` và số bài hoàn thành. Client dùng output này để dựng khóa/mở màn học. |
| **2. `GET /topics/{topicId}/lessons`** | JWT + `topicId`; không body. | Đọc trạng thái topic và `lesson_progress`; gọi Content lấy bài đã publish và gói đề cuối của topic. | Thông thường không ghi. Các trạng thái lần giao đề đã dùng được đọc từ `topic_test_assignments`. | Danh sách bài kèm trạng thái học và trạng thái đề cuối `LOCKED`, `AVAILABLE` hoặc `PASSED`. Topic chưa mở trả `TOPIC_LOCKED`. |
| **3. `GET /lessons/{lessonId}`** | JWT + `lessonId`; không body. | Đọc `topic_progress`, bài trước trong `lesson_progress`, review `PENDING` trong `path_review_items`; gọi Content lấy nội dung/khối/câu hỏi. | Upsert `lesson_progress` với topic, thứ tự bài và `knowledge_point_ids` của lesson để bài có thể được ghi nhận kể cả khi UI chưa gọi GET trước đó. Không lưu bản nội dung bài vào AI DB. | Nội dung hiển thị đã lọc; đáp án/giải thích chỉ có ở khối đã đạt. Nếu review đang chờ trả `REVIEW_REQUIRED`; topic/bài bị khóa trả lỗi tương ứng. |
| **4. `POST /lessons/{lessonId}/exercises/{blockId}/submissions`** | JWT + `lessonId`, `blockId`; JSON gồm `requestId` và câu trả lời theo `questionVersionId`. | Mở transaction khóa `mastery_paths` của `U`; kiểm cổng học; kiểm `requestId`; gọi Content lấy `answerSpec` và mapping KP cho câu. Chấm theo `answer-spec-v1`. | Ghi một dòng `lesson_exercise_submissions` (`answers`, `block_passed`, response gồm kết quả). Cập nhật `lesson_progress.passed_block_ids` khi khối đạt. Chỉ ở lần nộp đầu của khối, ghi evidence vào `mastery_paths.state_json`; nếu aggregate đổi, transaction cập nhật `revision`, dựng lại `mastery_learning_evidence` và ghi `mastery_events`. Nếu mọi khối tự chấm đã đạt: đặt `completed_at`, rồi tạo `path_review_items` cho KP dưới ngưỡng, có câu sai ở lần nộp đầu và có gói luyện. Các thay đổi cùng transaction. | Trả đúng/sai từng câu. Nếu khối chưa đạt thì không trả đáp án/lời giải; đạt thì có thêm đáp án/lời giải. Gửi lại cùng `requestId` nhận lại response cũ, không cộng mastery lần nữa. |
| **5. `POST /lessons/{lessonId}/complete`** | JWT + `lessonId`; body rỗng. Chỉ hợp lệ với lesson không có khối EXERCISE. | Kiểm cùng cổng topic, bài trước và review đang chờ. | Cập nhật `lesson_progress.completed_at`. Bài không có câu trả lời nên không tạo evidence từ exercise. | Trả trạng thái bài hoàn tất. Nếu vi phạm cổng, trả lỗi khóa tương ứng. |
| **6. `GET /reviews/{reviewId}`** | JWT + UUID `reviewId`; không body. | Xác minh `path_review_items` thuộc `U`; tìm `path_review_sets` đang mở. Nếu chưa có, gọi Content tìm gói mới, loại các gói đã giao; đọc phần TEXT của lesson liên quan. | Nếu cần gói mới thì tạo `path_review_sets` với `package_id` và `package_version_id`. Không tạo trùng set đang mở. `path_review_items` vẫn `PENDING`. | Trả lý thuyết và câu hỏi của set đang mở. Gói được chấm theo allowlist câu hỏi; đáp án giữ lại cho đến khi đạt set. |
| **7. `POST /reviews/{reviewId}/submissions`** | JWT + `reviewId`; JSON gồm `reviewSetId`, `requestId`, câu trả lời. | Khóa path; xác minh review thuộc `U` và set là set đang mở; lấy answer spec của package để chấm. | Cập nhật `path_review_sets` (request, thời điểm nộp, passed, response); ghi evidence source `review_set` vào `mastery_paths.state_json`. Khi evidence làm aggregate đổi, cập nhật `revision`, projection `mastery_learning_evidence` và `mastery_events`. Nếu ≥70%, đổi `path_review_items.status` thành `DONE`; nếu thấp hơn vẫn `PENDING` để GET sau giao package khác; set thứ 3 trượt thì `SKIPPED`. Không gọi lại luật tạo review. | Trả kết quả và, khi đạt, đáp án/lời giải. Set cũ không nộp lại được; sai set trả `REVIEW_SET_CLOSED`. |
| **8. `POST /topics/{topicId}/test-assignments`** | JWT + `topicId`; không body. | Kiểm topic `IN_PROGRESS`, tất cả lesson hoàn thành và không có review `PENDING`; đọc assignment cũ; gọi Content lấy gói `TOPIC_TEST` và version đang publish. | Tạo `topic_test_assignments` nếu chưa có assignment chưa dùng. Đây là reservation/idempotency của mã đề; chưa đổi mastery hay trạng thái topic. | Trả `assignmentId`, `packageId`, `packageVersionId`. Nếu topic chưa có đề trả `TEST_UNAVAILABLE`; nếu còn assignment chưa dùng thì trả lại assignment đó. |
| **9. Event `AssessmentCompleted.v2`** | RabbitMQ gửi event gồm `user_id`, `attempt_id`, `result_id`, `result_version`, `package_version_id`, loại đề, điểm từng câu và KP mapping. Không có token learner. | Nếu path tồn tại: khóa path và so version với `formal_assessment_result_versions`. Với `TOPIC_GATE`, ghép `package_version_id` với assignment chưa dùng. Nếu path chưa tồn tại: không tự gọi Content; park event. | Khi chưa có path: insert `pending_formal_assessment_results` rồi ACK. Khi có path: ghi/rebuild formal evidence trong `mastery_paths.state_json` và `mastery_learning_evidence`; ghi `mastery_events`; upsert `formal_assessment_result_versions`; đánh dấu `topic_test_assignments` đã dùng. Với đề đạt ≥70%, ghi `topic_progress.passed_at` của topic (một chiều; topic kế mở khi đọc); có thể tạo `path_review_items` theo evidence/các câu sai. Một transaction commit xong consumer mới ACK event đã áp. | Không có HTTP response đồng bộ cho lần nộp đề. UI gọi lại `GET /topics` để thấy trạng thái topic mới. Event trùng hoặc phiên bản cũ không cộng lại; event lỗi tạm được retry. |

### Ví dụ xuyên suốt một lần nộp bài

Giả sử `U` đang học `L2`, khối `B2` hỏi hai câu gắn với `K1`. UI gửi `requestId=R1`, câu 1 đúng, câu 2 sai. AI Learning làm trong một transaction:

1. Tìm `mastery_paths` của `U`, khóa row đó và xác nhận `L2/B2` đang được phép làm.
2. Nếu `R1` đã được lưu cho cùng bài/khối, trả response đã lưu. Nếu chưa, chấm hai câu bằng `answerSpec` lấy từ Content.
3. Ghi câu trả lời và response vào `lesson_exercise_submissions`; thêm `B2` vào `lesson_progress.passed_block_ids` chỉ khi khối đạt.
4. Ghi bằng chứng vào aggregate mastery cho KP liên quan nếu đây là lần nộp đầu của khối. Nếu aggregate thay đổi khi commit, `mastery_paths.revision` tăng; `mastery_learning_evidence` phản ánh cùng evidence đó.
5. Nếu đây là khối cuối và bài hoàn tất, đặt `lesson_progress.completed_at`. Nếu `K1` đã được học, mastery dưới ngưỡng, có câu sai ở lần nộp đầu và Content có gói luyện cho `K1`, thêm `path_review_items` `PENDING`.
6. Commit tất cả hoặc rollback tất cả. Client nhận kết quả chấm; nếu có review, lần mở bài tiếp theo sẽ được trả `REVIEW_REQUIRED` kèm review cần làm.

Nội dung chuẩn của bài, `answerSpec` và package vẫn thuộc Content. Bản ghi trong AI DB là lịch sử câu trả lời/progress cần cho idempotency, mastery và luật mở bước tiếp theo.

## 12 bảng nghiệp vụ cần dùng

Ký hiệu trong ví dụ: `U` là một học viên, `P` là path của `U`, `T1` là topic, `L2` là bài học, `K1` là knowledge point, `A1` là attempt thi. Trong database, các ID này là UUID. Ví dụ mô phỏng kịch bản học viên Lan trong [plan MVP](../../plans/260929-1640-lesson-learning-pipeline-mvp/plan.md); chúng **không phải dữ liệu cá nhân lấy từ database thật**.

### A. Nền tảng mastery và nhận kết quả thi: 5 bảng đã có

| Bảng | Giữ để làm gì | Khóa và dữ liệu chính | Ví dụ |
| --- | --- | --- | --- |
| `mastery_paths` | Một aggregate mastery cho mỗi học viên. `state_json` là `LearningProgress` đã serialize; `revision` tăng khi trạng thái đổi. | PK `path_id`; `user_id`; `state_json`; `revision`. MVP dự kiến unique theo `user_id`, không cần active learning goal. | `P` của `U` có mastery `K1 = 0.51`, revision `7`. |
| `mastery_events` | Lịch sử thay đổi của path, ghi cùng transaction với aggregate. Đây là event nội bộ, không phải RabbitMQ outbox. | PK `id`; FK `path_id → mastery_paths`; `revision`, `event_type`, `payload_json`. | Sau khi nhận kết quả đề `A1`, có event `assessment.result_applied`. |
| `mastery_learning_evidence` | Bản chiếu các bằng chứng học từ `state_json` để tra cứu; không phải nguồn mastery độc lập. | PK `(path_id, ordinal)`; FK tới path; `knowledge_point_id`, `source`, `source_reference_id`, `result`, `quality`, `evidence_json`. | `K1` có bằng chứng `source='lesson_exercise'` khi Lan nộp bài `L2`. |
| `formal_assessment_result_versions` | Nhớ phiên bản kết quả mới nhất đã áp cho từng attempt; chống xử lý trùng và hỗ trợ chấm lại. | PK `(path_id, attempt_id)`; FK tới path; `result_id`, `result_version`, `event_id`. | `(P, A1)` đã áp `result_version=2`; event version 1 đến muộn bị bỏ qua. |
| `pending_formal_assessment_results` | Giữ event Assessment tới trước khi path của học viên được tạo, rồi áp và xóa khi có path. | PK `event_id`; `user_id`, `attempt_id`, `result_version`, `payload`. Không có FK tới path vì path chưa tồn tại. | Event kết quả `A1` tới trước lần đầu `U` mở `/topics` được đỗ ở đây. |

**Thay đổi so với schema hiện tại:** [`mastery_paths`](../../services/ai-learning-service/migrations/V0_1__create_v5_mastery_tables.sql) đang unique theo `(user_id, learning_goal_id)` qua V1; [phase path theo user](../../plans/260929-1640-lesson-learning-pipeline-mvp/phase-05-ai-learning-path-theo-user.md) dự kiến đổi thành một path mỗi user. `pending_formal_assessment_results.learning_goal_id` hiện bắt buộc nhưng MVP dự kiến cho phép `NULL`, vì event mới không cần goal. Đây là thay đổi **chưa triển khai**.

### B. Luồng topic → bài học → bài ôn → đề cuối: 6 bảng cần thêm (V11)

Các tên và trường dưới đây lấy từ [phase API bài học/bài ôn](../../plans/260929-1640-lesson-learning-pipeline-mvp/phase-06-ai-learning-api-bai-hoc-va-bai-on.md). Đây là **bảng dự kiến**, hiện chưa có migration.

| Bảng | Cần cho MVP vì | Khóa và dữ liệu chính | Ví dụ |
| --- | --- | --- | --- |
| `topic_progress` | Lưu thứ tự topic và topic nào được mở/đã đạt. | PK `(user_id, topic_id)`; `sequence_order`, `passed_at`. **Không có cột `status`**: API suy ra `LOCKED`/`IN_PROGRESS`/`PASSED` khi đọc. | `(U, T1)` chưa có `passed_at` và đứng đầu → `IN_PROGRESS`; topic kế → `LOCKED`. |
| `lesson_progress` | Nhớ bài nào đã mở, các khối bài tập đã đạt và thời điểm hoàn thành. | PK `(user_id, lesson_id)`; `topic_id`, `lesson_sort_order`, `knowledge_point_ids`, `passed_block_ids`, `completed_at`. | `(U, L2)` đã đạt các khối `B1`, `B2` và có `completed_at`. |
| `lesson_exercise_submissions` | Lưu mỗi lần nộp bài để trả lại kết quả khi request lặp và tránh cộng bằng chứng hai lần. | PK `id`; unique `request_id`; `user_id`, `lesson_id`, `block_id`, `answers`, `block_passed`, `response`, `submitted_at`. | Lan nộp khối `B2` của `L2` với `requestId=R1`; gửi lại `R1` nhận đúng kết quả cũ. |
| `path_review_items` | Một nhiệm vụ ôn đang chờ cho KP yếu đã được học; chặn mở bài tiếp theo tới khi ôn xong. | PK `id`; `user_id`, `knowledge_point_id`, `lesson_id`, `status` (`PENDING`, `DONE`, `SKIPPED`), `done_at`; tối đa một dòng `PENDING` cho `(user_id, knowledge_point_id)`. | Sau `L2`, `K1` có mastery `0.51` và từng sai câu hỏi nên tạo review `PENDING`. |
| `path_review_sets` | Ghi gói `PRACTICE_SET` câu mới được giao cho một nhiệm vụ ôn và kết quả nộp. | PK `id`; FK `review_item_id → path_review_items`; `package_id`, `package_version_id`, `submitted_at`, `passed`, `request_id`. | Review của `K1` được giao gói `PS1`; trượt thì lần sau giao gói khác. |
| `topic_test_assignments` | Ghi mã đề cuối topic đã giao, bảo đảm mỗi lần giao dùng một lần và không mở topic bằng mã cũ. | PK `id`; `user_id`, `topic_id`, `package_id`, `package_version_id`, `consumed_attempt_id`, `consumed_at`, `percent`. | `U` nhận mã đề `TT1` cho `T1`; attempt `A1` đạt `75%`, lần giao được đánh dấu đã dùng. |

### C. Writing: 1 bảng cần thêm (V12)

| Bảng | Cần cho MVP vì | Khóa và dữ liệu chính | Ví dụ |
| --- | --- | --- | --- |
| `lesson_writing_submissions` | Lưu mỗi lần nộp essay (Task 1, Task 2), kết quả chấm LLM và trạng thái trừ point; idempotent theo `requestId`. | PK `id`; unique `request_id`; `user_id`, `lesson_id`, `block_id`, `question_version_id`, `knowledge_point_ids`, `essay_text`, `word_count`, `prompt_snapshot`, `status` (`GRADING`, `GRADED`, `FAILED`, `PAYMENT_PENDING`), `point_cost`, `debit_ledger_entry_id`, `result`, `overall_band`, `passed`. Partial unique: một dòng `GRADING` mỗi `(user_id, block_id)`. | Lan nộp essay L4 → `GRADED`, band 6.5, ví −3 point. |

Luồng nộp essay: kiểm độ dài (50–1.000 từ) → kiểm số dư (`GET /api/access/me/points`) → path transaction ngắn tạo dòng `GRADING` → LLM chấm ngoài transaction → lưu kết quả (`PAYMENT_PENDING`) → debit 3 point (`/internal/access/points/debit`, idempotent) → path transaction ngắn ghi `GRADED` và bằng chứng `lesson_writing`. LLM lỗi thì không trừ point. Bằng chứng Writing ghi **mọi lần nộp tới khi khối đạt** (mỗi lần là bài viết mới). Khối essay không chặn hoàn thành bài.

### D. Listening và gợi ý Reading: không thêm bảng

- **Listening:** audio là asset `AUDIO` của Content (`media_reference` là key, Content ghép `CONTENT_MEDIA_BASE_URL`); học viên nhận `mediaUrl`. Transcript chỉ trả khi bài xong (bài học) hoặc ≥ 70% (gói ôn, đề cuối). AI Learning dùng lại các bảng ở mục B.
- **Gợi ý Reading:** cột `question_versions.hint` ở Content; AI Learning trả `hint` cho câu từng sai trong khối chưa đạt, đọc lịch sử từ `lesson_exercise_submissions`. Không ghi `hints_used`.

### Quan hệ và nguồn dữ liệu

| Quan hệ | Loại | Ý nghĩa |
| --- | --- | --- |
| `mastery_events`, `mastery_learning_evidence`, `formal_assessment_result_versions` → `mastery_paths` | FK thật qua `path_id` | Xóa path thì các dòng này bị xóa theo. |
| `path_review_sets` → `path_review_items` | FK thật qua `review_item_id` theo plan | Một nhiệm vụ ôn có thể giao nhiều gói qua các lần trượt, nhưng chỉ một gói đang mở. |
| `topic_progress`, `lesson_progress`, submissions, review items, test assignments ↔ `mastery_paths` | Cùng `user_id`; **không có FK** trong thiết kế plan | Code cập nhật chúng trong **cùng path transaction**, bảo đảm tiến độ và mastery commit cùng nhau. |
| `topic_id`, `lesson_id`, `knowledge_point_id`, `package_id`, `package_version_id` | ID logic sang Content Service | AI Learning không sở hữu nội dung chuẩn. |
| `attempt_id`, `result_id`, `event_id` | ID logic sang Assessment Service/RabbitMQ | Consumer ghi evidence và version đã áp; không truy cập `assessment_db` trực tiếp. |

## Các bảng của phiên tutor không thuộc MVP

| Nhóm bỏ khỏi luồng nghiệp vụ | Bảng hiện có |
| --- | --- |
| Phiên chat và đoạn Reading gắn phiên | `sessions`, `turns`, `messages`, `session_materials` |
| Câu luyện do tutor tạo và lịch ôn notebook riêng | `notebook_entries`, `practice_review_state`, `practice_review_events` |
| Trí nhớ học viên và hạn mức chat/tóm tắt | `learner_memory`, `llm_daily_usage` |
| Band từng KP | `mastery_path_knowledge_point_bands`: V10 xóa bảng này ([plan path theo user](../../plans/260929-1640-lesson-learning-pipeline-mvp/phase-05-ai-learning-path-theo-user.md)). |

`mastery_path_knowledge_point_details` **vẫn thuộc MVP**: snapshot KP ghi từ `topic-sequence`, có thêm cột `has_practice_set` (V10) cho luật chèn bài ôn.

`mastery_interactions` là bảng câu hỏi tương tác kiểu DeepTutor. MVP không tạo interaction mới, nhưng `LearningTransaction` và một số đường refresh hiện vẫn đọc nó. **Để ra MVP nhanh, giữ bảng vật lý tạm thời**, không mở API tutor; chỉ bỏ bảng sau khi đã tách các lời gọi trong mastery engine và kiểm thử lại. Tương tự, các trường tutor rỗng trong `state_json` có thể giữ để tránh một migration JSON không cần thiết cho MVP.

“Bỏ khỏi MVP” ở đây nghĩa là **không còn route/luồng ghi sử dụng**. Không sửa hay xóa các migration V0.1–V9 đã áp dụng và không `DROP TABLE` trên database đang có dữ liệu như một bước dọn dẹp nhanh. Nếu cần thu gọn schema vật lý, dùng migration mới sau khi kiểm dữ liệu và quan hệ FK.

## Thứ tự làm để ra MVP nhanh

Theo thứ tự merge của [lộ trình MVP](../../plans/260930-2057-mvp-reading-writing-listening-roadmap/plan.md); tutor giữ nguyên, không gỡ router, startup recovery hay quota.

1. Path theo user, không LLM, lấy từ `topic-sequence`; giữ `mastery_paths`, evidence, version ledger và pending result. Nếu database đã có nhiều path cho cùng user, phải kiểm và sao lưu dữ liệu trước migration gộp path vì [V10](../../plans/260929-1640-lesson-learning-pipeline-mvp/phase-05-ai-learning-path-theo-user.md) xóa các path dư.
2. Thêm sáu bảng tiến độ bài học (V11) và API topic/bài/bài ôn/đề cuối. Nộp bài, cập nhật mastery và tạo review chạy trong cùng path transaction.
3. Cho Assessment consumer ghi `passed_at`, đánh dấu mã đề và chèn bài ôn trong cùng transaction; thử event trùng, event tới sớm và chấm lại.
4. Writing Task 2 rồi Task 1: bảng `lesson_writing_submissions` (V12), chấm LLM, trừ point.
5. Listening, rồi gợi ý Reading: không thêm bảng AI Learning. Cập nhật tài liệu schema hiện hành khi migration thực sự được áp dụng.

**Luồng đích:** Content cung cấp bài và gói câu hỏi → AI Learning ghi tiến độ, chấm bài tập bài học và tính mastery → AI Learning giao bài ôn hoặc đề cuối → Assessment chấm đề và phát event → AI Learning cập nhật mastery, ghi topic đạt (topic kế mở khi đọc).
