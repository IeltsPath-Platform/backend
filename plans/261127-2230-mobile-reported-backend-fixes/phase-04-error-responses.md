# Phase 4: mã lỗi và câu lỗi (P1, P2)

Theo AGENTS.md §3.3: mọi câu lỗi tiếng Việt, mọi handler đi qua `VietnameseErrorMessages.userFacing`.
Patch của bên mobile dùng làm tham khảo được cho content, library, game, nhưng **thiếu access, user** và không có test.

## Sai method trả 405 (hiện trả 500)

Thêm handler `HttpRequestMethodNotSupportedException` vào `GlobalExceptionHandler` của **access, content, game, library,
user**. Câu: "Phương thức này không được hỗ trợ."

## Thiếu hoặc sai kiểu tham số trả 400 (hiện trả 500)

Thêm `MissingServletRequestParameterException` và `MethodArgumentTypeMismatchException`:

- access, content, game: trong `GlobalExceptionHandler`.
- learning: thêm `MissingServletRequestParameterException` vào `GlobalExceptionHandler`. Hiện chỉ
  `StudyDataExceptionHandler` bắt loại này và chỉ áp cho 5 controller, nên `GET /api/learning/practice-progress` thiếu
  `skill` trả 500.
- Rà library, user, assessment, community, notification: thiếu loại nào thì thêm.

Câu: "Thiếu thông tin cần thiết để tải dữ liệu." và "Dữ liệu gửi lên không hợp lệ".

## Câu lỗi tiếng Anh

- game: `GameMatchNotFoundException` → "Không tìm thấy trận đấu", `GameSessionNotFoundException` → "Không tìm thấy phiên
  chơi" (không kèm id; id để trong log). Bọc mọi câu trả về bằng `userFacing`; ghi log lỗi 500.
- assessment: handler của `AssessmentNotFoundException` đang trả nguyên `e.getMessage()`; bọc `userFacing`.
- learning, dịch tại nguồn, giữ nguyên `code`:

| File | Câu hiện tại |
|---|---|
| `HeartsService` | Hearts are already full |
| `LessonAccess` | Lesson was not found |
| `PracticeAccess` | Complete the lesson before practice |
| `SpeakingTestQuestions` | Nothing to record (2 chỗ), No Speaking test found, A part is not in this test |
| `CompleteSpeakingSubmissionUseCase` | Speaking submission not found |
| `CreatePlacementAssignment` | Placement limit reached, No published placement test is available |
| `GetLearningProgressStatisticsUseCase` | Complete curriculum metadata is unavailable |
| `StartPracticeAttemptUseCase` | Practice set was not found |
| `SubmitTheoryCheckUseCase` | Quick check contains unsupported questions |

Danh sách này lấy từ `LearningRequestException` và `ResponseStatusException`; rà thêm các exception khác khi sửa. Kiểm tra
web và mobile không so khớp chuỗi tiếng Anh nào (chúng phải dựa vào `code`).

## Test

Mỗi service đã sửa: một test tầng web cho sai method (405), thiếu tham số (400), sai kiểu (400), kiểm tra `message` tiếng
Việt. Sửa các test đang mong câu tiếng Anh cũ.
