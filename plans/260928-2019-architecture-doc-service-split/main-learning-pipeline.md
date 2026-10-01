# Pipeline học chính

- Trạng thái: **thiết kế đích, chưa triển khai**. Code hiện tại (commit `2079964`) vẫn chạy theo hướng tutor DeepTutor là nơi học chính (`.sdd/specs/FEATURE_TREE_V2.md`).
- Chốt ngày 2026-09-28. Lý do và phương án đã loại: [brainstorm bài học](../reports/brainstorm-260928-2215-lesson-learning-flow-report.md). Bảng dữ liệu: [plan.md](./plan.md) mục "Bảng theo service".
- Ký hiệu: ✅ đã có code · 🆕 phải làm mới · ❓ chưa chốt.
- Quan hệ topic, KP, lesson, question: [topic-kp-lesson-question-explained.md](./topic-kp-lesson-question-explained.md).

## 1. Nguyên tắc

- Học như web luyện thi: **chủ đề → bài học**, học tuần tự, bài sau khóa đến khi xong bài trước.
- Mỗi học viên **một path**, không gắn learning goal. Path tạo khi học viên bắt đầu học.
- Ban đầu path của mọi học viên **giống nhau** (thứ tự `sort_order` do content author đặt). Sau mỗi kết quả, path của từng người **khác dần**: xếp lại chủ đề chưa học và chèn bài ôn bắt buộc cho KP yếu.
- Không dùng band mục tiêu, ngày thi, số phút học, placement để tạo hay xếp path. Không lọc chủ đề theo band.

## 2. Sơ đồ tổng

```mermaid
flowchart TD
    LOGIN(["Đăng ký / đăng nhập"])
    GOAL["Learning goal<br/>chỉ hiển thị, nhắc học"]
    PLACE["Placement<br/>chỉ xem trình độ"]
    START["Bấm 'Bắt đầu học'<br/>ai-learning tạo path<br/>thứ tự sort_order, giống mọi người"]

    LOGIN -.->|tùy chọn| GOAL
    LOGIN -.->|tùy chọn| PLACE
    LOGIN --> START
    START --> TOPIC

    subgraph LOOP["Vòng học một chủ đề"]
        direction TB
        TOPIC["Mở chủ đề kế<br/>theo thứ tự hiện tại của học viên<br/>chủ đề trả phí: kiểm gói"]
        NEXT{"Bước kế tiếp?"}
        REVIEW["Bài ôn bắt buộc<br/>làm lại bài tập ≥ 70%"]
        LESSON["Học bài tiếp theo<br/>lý thuyết · ảnh/audio · từ vựng"]
        EXERCISE["Bài tập nhúng<br/>ai-learning chấm ngay"]
        LESSON_OK{"≥ 70%?"}
        TEST["Đề cuối chủ đề<br/>assessment tự chấm"]
        TEST_OK{"≥ 70%?"}
        PASSED(["Chủ đề PASSED"])

        TOPIC --> NEXT
        NEXT -->|còn bài ôn chờ| REVIEW --> NEXT
        NEXT -->|còn bài chưa xong| LESSON --> EXERCISE --> LESSON_OK
        LESSON_OK -->|chưa đạt, làm lại| EXERCISE
        LESSON_OK -->|đạt, mở bài kế| NEXT
        NEXT -->|hết bài| TEST --> TEST_OK
        TEST_OK -->|trượt, bài ôn được chèn| NEXT
        TEST_OK -->|đạt| PASSED
    end

    MORE{"Còn chủ đề?"}
    DONE(["Hoàn thành path"])
    PASSED --> MORE
    MORE -->|còn| TOPIC
    MORE -->|hết| DONE

    FLASH["Thêm từ vào flashcard"]
    LESSON -.-> FLASH

    MOCK["Mock test · luyện đề"]
    MASTERY[["Cập nhật mastery KP"]]
    ADAPT["Path đổi<br/>① xếp lại chủ đề chưa học, trong cùng chủ đề cha<br/>② KP dưới ngưỡng → chèn bài ôn bắt buộc"]

    EXERCISE -.->|kết quả| MASTERY
    REVIEW -.->|kết quả| MASTERY
    TEST -.->|kết quả| MASTERY
    MOCK -.->|kết quả| MASTERY
    MASTERY -.-> ADAPT
    ADAPT -.->|bài ôn| NEXT
    ADAPT -.->|thứ tự chủ đề| TOPIC

    classDef user fill:#eceff1,stroke:#607d8b,color:#1a1a1a
    classDef ai fill:#e3f2fd,stroke:#1e88e5,color:#1a1a1a
    classDef assess fill:#fff3e0,stroke:#fb8c00,color:#1a1a1a
    classDef library fill:#e8f5e9,stroke:#43a047,color:#1a1a1a
    classDef optional stroke-dasharray:5 4
    classDef milestone fill:#ede7f6,stroke:#5e35b1,color:#1a1a1a

    class LOGIN,GOAL user
    class START,TOPIC,REVIEW,LESSON,EXERCISE,MASTERY,ADAPT ai
    class PLACE,TEST,MOCK assess
    class FLASH library
    class GOAL,PLACE optional
    class PASSED,DONE milestone
```

| Ký hiệu | Nghĩa |
| --- | --- |
| Xanh dương | ai-learning (path, mở bài, chấm bài tập, mastery) |
| Cam | assessment (placement, đề cuối, mock, luyện đề) |
| Xám | user-service |
| Xanh lá | library |
| Viền đứt | Bước tùy chọn, không ảnh hưởng path |
| Đường chấm | Kết quả chảy vào mastery rồi làm path đổi |

## 3. Các bước

### 3.1 Vào hệ thống

1. Đăng ký, đăng nhập (user-service ✅). Gateway xác thực và ký internal JWT ✅.
2. Learning goal (user-service ✅): **không bắt buộc**, không ảnh hưởng path. Dùng để hiển thị hồ sơ, nhắc học ❓.
3. Placement (assessment ✅ làm bài, 🆕 tự chấm Reading/Listening): **không bắt buộc**, không tạo path, không mở hay bỏ qua chủ đề. ❓ Có ghi vào mastery không.

### 3.2 Bắt đầu học → tạo path

4. App gọi `GET /api/ai-learning/topics` 🆕. Chưa có path thì ai-learning tạo 🆕: một path cho học viên, gồm mọi chủ đề đã publish, thứ tự theo `sort_order`. Không gọi LLM, không lọc band.
5. Trả danh sách chủ đề theo thứ tự hiện tại của học viên: chủ đề đầu `IN_PROGRESS`, các chủ đề sau `LOCKED`; chủ đề trả phí có nhãn premium (ai-learning hỏi access 🆕).

### 3.3 Học một bài

6. Mở bài: `GET /api/ai-learning/lessons/{id}` 🆕. ai-learning kiểm:
   - còn bài ôn bắt buộc chưa làm → 403 `REVIEW_REQUIRED` (trả kèm danh sách bài ôn);
   - bài trước chưa xong → 403 `LESSON_LOCKED`;
   - chủ đề trả phí mà chưa có gói → 403 `PREMIUM_REQUIRED`;
   - hợp lệ → lấy bài từ content (endpoint nội bộ 🆕), **bỏ `answer_spec`**, trả app.
7. Học lần lượt các block:
   - `TEXT`, `ASSET` (ảnh, audio, đoạn văn) từ content 🆕.
   - `VOCABULARY`: bấm "thêm vào flashcard" → library ✅ (flashcard nguồn `VOCABULARY_SENSE`).
   - `EXERCISE`: nộp `POST /api/ai-learning/lessons/{id}/exercises/{blockId}/submissions` 🆕 → ai-learning chấm theo `answer_spec` (bộ chấm mới 🆕) → trả đúng/sai + giải thích → cập nhật mastery các KP của câu hỏi ✅ → **path đổi** (mục 3.5).
8. Bài hoàn thành khi mỗi block bài tập có một lần nộp ≥ 70%; bài không có bài tập thì bấm "Hoàn thành" 🆕. Hoàn thành → mở bài kế.

### 3.4 Đề cuối chủ đề

9. Xong mọi bài `PUBLISHED` của chủ đề → mở đề cuối (package `TOPIC_TEST` do content author soạn 🆕).
10. Làm đề ở assessment: tạo attempt ✅ → nộp → **tự chấm** 🆕 → result `COMPLETED` → outbox → RabbitMQ `AssessmentCompleted.v2` (`TOPIC_GATE`, thêm `package_version_id` 🆕) ✅.
11. ai-learning nhận event ✅ → tra đề thuộc chủ đề nào 🆕 → cập nhật mastery ✅:
    - ≥ 70% → chủ đề `PASSED` → mở chủ đề kế **theo thứ tự hiện tại** của học viên;
    - < 70% → các KP làm sai xuống dưới ngưỡng → chèn bài ôn bắt buộc → ôn xong mới làm lại đề.
    Điểm hiện ngay khi nộp (assessment trả về); mở chủ đề kế trễ vài giây (qua event).

### 3.5 Path đổi sau mỗi kết quả

12. Mọi kết quả (bài tập nhúng, đề cuối, mock, luyện đề) cập nhật mastery rồi tính lại path 🆕:
    - **Xếp lại chủ đề chưa bắt đầu**, chỉ giữa các chủ đề **cùng chủ đề cha**: có bằng chứng yếu lên trước → chưa có dữ liệu giữ `sort_order` → đã vững xuống cuối (vẫn phải học). Chủ đề đang học và đã `PASSED` không đổi chỗ.
    - **Chèn bài ôn bắt buộc**: KP có mastery dưới ngưỡng (kể cả KP thuộc chủ đề đã `PASSED`) → chèn bài dạy KP đó (qua `lesson_knowledge_points`) vào trước bước kế. Không chèn chính bài đang học. Mỗi KP tối đa một bài ôn đang chờ.
    - Bài ôn xong khi làm lại bài tập của bài đó ≥ 70% (mastery được cập nhật lại); bài không có bài tập thì bấm "Hoàn thành". KP chỉ bị chèn lại khi có kết quả mới làm nó rơi xuống dưới ngưỡng.
    - Bài tập nhúng chủ yếu chạm KP của chủ đề đang học, nên thứ tự chủ đề chưa học thay đổi chủ yếu nhờ mock, luyện đề, đề cuối.

### 3.6 Luồng phụ (không nằm trong vòng chính)

- Mock test, luyện đề chính thức: assessment ✅ (tự chấm 🆕). Cập nhật mastery → path đổi; không mở/khóa chủ đề.
- Writing/Speaking: chấm AI (trừ điểm ở access) hoặc examiner (`examiner_profiles` 🆕); grading job hiện dừng ở `QUEUED`.
- Video YouTube, flashcard, note: library. Game, community: không đổi. Activity, streak: user-service.
- Thông báo: notification 🆕 (đề đã chấm, bài ôn mới được chèn, nhắc học).
- Tutor AI: ❓ vai trò chưa chốt, không nằm trong pipeline.

## 4. Luật

| Luật | Nội dung |
| --- | --- |
| Tạo path | Khi học viên bắt đầu học; một path mỗi học viên; không cần goal |
| Thứ tự ban đầu | `sort_order` của content, giống mọi học viên; không LLM, không lọc band |
| Xếp lại | Sau mọi kết quả; chỉ chủ đề chưa bắt đầu, chỉ giữa chủ đề cùng cha; yếu → chưa có dữ liệu → vững |
| Mở chủ đề | Chủ đề đầu tiên theo thứ tự hiện tại; chủ đề kế mở khi chủ đề trước `PASSED` |
| Mở bài | Bài 1 khi chủ đề mở; bài n khi bài n−1 xong; không còn bài ôn bắt buộc đang chờ |
| Hoàn thành bài | Mỗi block bài tập có lần nộp ≥ 70%; không có bài tập thì bấm hoàn thành; xong rồi thì giữ |
| Đề cuối | Mở khi xong mọi bài; đạt khi tổng `score / max_score` ≥ 70% |
| Bài ôn bắt buộc | KP dưới ngưỡng → chèn bài dạy KP; chặn bước kế đến khi xong |
| Làm lại | Không giới hạn; giữ điểm cao nhất; `requestId` trùng trả kết quả cũ |
| Premium | Theo chủ đề; danh sách vẫn hiện chủ đề trả phí, nội dung bị chặn |
| Đáp án | API học viên không bao giờ trả `answer_spec` |

## 5. Nguồn cập nhật mastery

| Nguồn | Cập nhật mastery | Làm path đổi | Mở/khóa |
| --- | --- | --- | --- |
| Bài tập nhúng (câu hỏi soạn sẵn, đã publish) | Có | Có | ≥ 70% → xong bài |
| Đề cuối chủ đề | Có | Có | ≥ 70% → chủ đề `PASSED` |
| Mock, luyện đề | Có | Có | Không |
| Placement | ❓ | ❓ (nếu ghi mastery thì có) | Không |
| Câu hỏi AI sinh trong tutor | Không | Không | Không |

## 6. Service tham gia

| Service | Vai trò trong pipeline |
| --- | --- |
| user | Đăng nhập; goal tùy chọn |
| content | Soạn chủ đề (`sort_order`, cha, premium, đề cuối), bài học, block, câu hỏi; endpoint nội bộ trả bài kèm `answer_spec` |
| ai-learning | Tạo và tính lại path, cổng mở bài, chấm bài tập nhúng, tiến độ, bài ôn bắt buộc, cập nhật mastery, nhận event |
| assessment | Làm và tự chấm đề cuối, mock, luyện đề, placement; phát event |
| access | Kiểm gói trả phí theo chủ đề |
| library | Từ vựng, flashcard |

## 7. Thay đổi bắt buộc so với code hiện tại

1. **Path gắn học viên thay vì goal**: `mastery_paths` và `pending_formal_assessment_results` đang theo (`user_id`, `learning_goal_id`) → migration mới trong ai-learning.
2. **Event khi không có goal**: `FinalizeAssessmentResultUseCase.java:90` không phát event nếu attempt không có goal; contract `AssessmentCompleted.v2` ghi `learning_goal_id` là **Required**. Goal tùy chọn thì học viên không goal làm đề cuối mà ai-learning không bao giờ nhận kết quả → phải nới contract (`learning_goal_id` tùy chọn) và bỏ luật "không goal thì không phát". Đổi contract cần duyệt (AGENTS §5).
3. **Tắt trong luồng tạo path**: LLM xếp thứ tự (`PathOrderer`, prompt đang có band mục tiêu, ngày thi, số phút, placement), lọc band (`CurriculumScope.select`), placement test-out.
4. **Thêm mới**: bảng bài học ở content; API học bài, bộ chấm `answer_spec`, bảng tiến độ và bài ôn ở ai-learning; tự chấm ở assessment; contract `answer_spec` + test vector dùng chung cho game, assessment, ai-learning.
5. **Sửa trước khi dùng**: endpoint entitlement của access đang không có guard.
6. **Cập nhật spec**: `FEATURE_TREE_V2.md` còn mô tả tutor là nơi học chính.

## 8. Câu hỏi mở

- Ngưỡng mastery để chèn bài ôn: dùng ngưỡng gate có sẵn theo loại KP (`gate_threshold`, mặc định 0.9) hay một số chung thấp hơn? 0.9 dễ làm chèn bài ôn quá dày.
- Placement có ghi vào mastery không? Nếu có, nó sẽ ảnh hưởng thứ tự chủ đề.
- Ôn theo lịch (spaced repetition, KP đến hạn ôn) có bắt buộc như bài ôn KP yếu không?
- Một KP có nhiều bài dạy: chèn bài nào (bài có `sort_order` nhỏ nhất, hay bài học viên đã học)?
- Thêm bài mới vào chủ đề học viên đã `PASSED`: đề xuất không khóa lại, bài mới hiện `AVAILABLE`.
- Goal dùng vào đâu: chỉ hiển thị, nhắc học theo số phút, ước tính kịp ngày thi?
- Dạng câu hỏi trong `answer_spec` và luật chấm điền từ.
