# Brainstorm: API bài học cho học viên đi qua AI Learning hay gọi thẳng Content

- Ngày: 2026-09-30. Phạm vi: MVP (plan `260929-1640-lesson-learning-pipeline-mvp`). Chỉ thiết kế, không code.
- Câu hỏi: `GET /api/ai-learning/topics/{id}/lessons` và `GET /api/ai-learning/lessons/{id}` có nên đi qua AI Learning rồi gọi Content, hay client gọi thẳng Content?

## Dữ kiện

- Payload bài học của Content chứa `answerSpec`, `explanation`, `hint`, transcript audio, `chartFacts`: đáp án hoặc dữ liệu chỉ lộ khi đạt.
- Content không biết tiến độ học viên (bài xong, khối đạt, bài ôn chờ); tiến độ ở `ai_learning_db`.
- Phase 2 của 1640 khóa `/api/content/questions|packages|assets` với CUSTOMER; Gateway chặn `/internal/**`; Content chưa có API bài học công khai.
- Cổng `REVIEW_REQUIRED`, `TOPIC_LOCKED`, `LESSON_LOCKED` và luật giấu đáp án đều cần tiến độ.

## Phương án

| | A. Qua AI Learning | B. Thẳng Content | C. Lai |
| --- | --- | --- | --- |
| Client gọi | 1 API | 2 nơi, tự ghép | 2 nơi, tự ghép |
| Cổng mở bài | Đủ | Thủng: đọc được bài bị khóa | Đủ cho chi tiết bài |
| Giấu đáp án theo tiến độ | Một chỗ | Content phải gọi ngược AI Learning, hoặc tách lời giải ra API khác | Như A cho chi tiết |
| Content đổi | Không (dùng endpoint nội bộ) | Thêm API công khai | Thêm API danh sách công khai |
| Chi phí | +1 bước gọi nội bộ | Phụ thuộc hai chiều | Hai nguồn thứ tự bài có thể lệch |

## Quyết định

**A.** App chỉ gọi `/api/ai-learning/*` cho luồng học. AI Learning là lớp điều phối việc học (kiểu backend-for-frontend): gọi Content nội bộ, áp cổng, lọc trường, gắn trạng thái. Content là kho nội dung, chỉ service gọi nội bộ.

- `/lessons/{id}` bắt buộc qua AI Learning (cổng + giấu đáp án cần tiến độ).
- `/topics/{id}/lessons`: tên bài không nhạy cảm, nhưng trạng thái bài và đề cuối chỉ AI Learning có; qua AI Learning thì client gọi một lần.
- Không đổi plan 1640 (đã thiết kế theo A).

## Rủi ro và theo dõi

- Thêm độ trễ một bước gọi nội bộ: chấp nhận cho MVP.
- Tối ưu sau (không làm trong MVP): cache payload bài đã publish ở AI Learning, vì bài ít đổi.

## Câu hỏi mở

- Không có.
