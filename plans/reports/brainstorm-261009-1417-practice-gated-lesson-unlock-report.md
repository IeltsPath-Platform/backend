# Brainstorm: Mở lesson sau khi Practice của lesson trước đạt

- Ngày: 2026-10-09
- Phạm vi: learning-service (không migration)
- Trạng thái: thiết kế đã duyệt, chưa có plan
- Thay thế: thiết kế "ôn lại KP yếu" (refresh) đã hủy, plan và báo cáo cũ đã xóa (chưa từng commit)

## 1. Yêu cầu

- Lesson N chỉ mở khi mọi lesson trước trong topic đã COMPLETED **và** Practice của chúng đã PASSED.
- Practice trượt sinh bài ôn → bài ôn phải **đạt** (DONE) mới tính xong; không còn bỏ qua sau 2 set.
- Không đánh dấu/nhắc KP yếu.

## 2. Hiện trạng (đã kiểm với code)

| Điểm | Nguồn |
| --- | --- |
| Mở lesson chỉ xét lesson trước COMPLETED | `LessonAccess` L56-64, `LessonAccessGate` L27-41 |
| Review PENDING chặn lesson cùng skill | `LessonAccessGate` L29-34 |
| Review sai 2 set → SKIPPED | `ReviewItem.recordSetResult` L117-133, `ReviewRule.MAX_FAILED_REVIEW_SETS` |
| Hết package chưa lộ → review SKIPPED | `GetReviewUseCase` L84-92 |
| Practice PASSED: FIRST_SUBMISSION / REVIEW_FINISHED / ALL_SETS_ATTEMPTED / NO_PRACTICE; Writing tùy chọn; lưu `lesson_practice_passes`, không ghi đè | `PracticeClearance`, `PracticeProgress` |
| Lỗi: REVIEW_FINISHED khi **bất kỳ** review của skill không PENDING, dù review khác còn PENDING | `PracticeClearance` L64-67, `wholeLesson` L80-81 |
| Mở topic sau cần thi topic trước đạt; thi topic đã đòi Practice PASSED → thay đổi chỉ trong một topic | `TopicStatusDeriver` L26-32, `GetTopicLessonsUseCase` L60-64 |
| `PRACTICE_REQUIRED` + `lessonIds` đã có (409, khi xin đề topic) | `AssignTopicTestUseCase` L87, contract L205 |
| Response bài ôn có `failedSets`, `maxFailedSets` | `lesson-learning-v1` L136, L157, L175 |

Seed `content_db` (PRACTICE_SET đã publish chứa câu của KP): 9 KP có 1 bộ, 1 KP 2 bộ, 6 KP 3 bộ, 3 KP 4–5 bộ.

## 3. Phương án khi hết bộ câu (bài ôn phải đạt)

| Phương án | Ưu | Nhược | Kết luận |
| --- | --- | --- | --- |
| A. Lặp đến khi đạt; hết bộ chưa lộ → SKIPPED(NO_PACKAGE), cho qua | Không kẹt, giữ quy tắc không giao lại package | Với seed mỏng, luật nghiêm hiếm phát huy | **Chọn** |
| B. Hết bộ → giao lại bộ cũ | Luôn phải đạt | Học viên đã thấy lời giải → đạt nhờ nhớ; phá AGENTS.md §3.8; phải giấu lời giải | Loại |
| C. Hết bộ → khóa cứng chờ Content | Nghiêm nhất | 9 KP 1 bộ → kẹt vĩnh viễn, hỏng demo | Loại |

## 4. Quyết định của người dùng

- Review SKIPPED do trượt không còn; phải DONE. Hết bộ câu → SKIPPED (NO_PACKAGE) tính là xong.
- Dữ liệu cũ chỉ là demo: không xử lý chuyển tiếp; cổng áp cho mọi lesson.
- Mã lỗi: `403 PRACTICE_REQUIRED` + `lessonIds`.
- Xóa hẳn plan/báo cáo refresh (đã xóa).

## 5. Thiết kế đã duyệt

1. `ReviewItem.recordSetResult`: trượt → luôn sang THEORY (`LOW_SCORE` nếu < 40%, ngược lại `SECOND_FAIL`), `failedSets++`,
   không SKIPPED. Bỏ `ReviewRule.MAX_FAILED_REVIEW_SETS`. `maxFailedSets` trong response → `null` (không giới hạn).
   `SKIPPED` chỉ còn từ `ReviewItem.skip()` (hết package). Không cần cột `skip_reason`.
2. `PracticeClearance`: REVIEW_FINISHED (theo skill và `wholeLesson`) chỉ khi có review của skill và **không còn review
   PENDING** nào; ALL_SETS_ATTEMPTED, NO_PRACTICE, FIRST_SUBMISSION giữ nguyên.
3. `LessonAccess.authorize` + `LessonAccessGate`: thêm điều kiện Practice của mọi lesson trước PASSED, dùng
   `PracticeProgress.forTopic` (cùng luật với danh sách lesson và cổng thi topic). Thứ tự: `REVIEW_REQUIRED` →
   `TOPIC_LOCKED` → `LESSON_LOCKED` → `PRACTICE_REQUIRED` (403, `lessonIds` = lesson trước có Practice chưa PASSED).
   `LearningGateException` + `GlobalExceptionHandler` mang `lessonIds`.
4. `GetTopicLessonsUseCase`: `status` AVAILABLE chỉ khi lesson trước COMPLETED và Practice PASSED.
5. Không đổi: cổng thi topic, mở topic/course, review từ kết quả thi (cũng lặp đến khi đạt/hết bộ), Writing tùy chọn.

## 6. Rủi ro

- Mỗi lần mở lesson thêm một lời gọi Content `topicPracticeSets` (qua `PracticeProgress.forTopic`); chấp nhận, danh sách
  lesson đã làm tương tự.
- Integration test đi L1 → L2 không làm Practice sẽ đỏ → sửa fixture.
- `lesson_practice_passes` cũ (luật cũ) không bị ghi đè → demo có thể lệch; reset bằng `docker compose down -v`.
- Seed mỏng → KP 1 bộ: trượt Practice vẫn qua (ALL_SETS_ATTEMPTED). Cần nhóm nội dung soạn ≥ 3–4 bộ/KP.
- Học viên có thể lặp bài ôn nhiều lượt (mỗi lượt đọc lý thuyết) cho tới khi hết bộ; không kẹt.

## 7. Tiêu chí kiểm chứng

- L1 COMPLETED, Practice chưa làm → mở L2: `403 PRACTICE_REQUIRED`, `lessonIds=[L1]`; danh sách lesson: L2 `LOCKED`.
- Practice L1 đạt lần đầu → L2 AVAILABLE.
- Practice L1 trượt → review; set ôn trượt 2, 3 lần → vẫn PENDING (THEORY ↔ PRACTICE); đạt → DONE → L2 mở.
- Hai review của cùng skill, một DONE một PENDING → Practice REQUIRED.
- Hết package chưa lộ → review SKIPPED → Practice PASSED → L2 mở.
- Thi topic, mở topic sau không đổi hành vi.
- `mvn -q -pl services/learning-service -am test` xanh, Testcontainers không skip.

## 8. Việc tiếp theo

- `/ck:plan --tdd` (sửa luật nghiệp vụ đang có test).
- Docs: `docs/contracts/lesson-learning-v1.md`, `AGENTS.md` §3.8, `docs/system-architecture.md`,
  `services/learning-service/README.md`, `docs/fe-main-flow-guide.md`; SRS do người dùng cập nhật.

## Câu hỏi còn mở

None.
