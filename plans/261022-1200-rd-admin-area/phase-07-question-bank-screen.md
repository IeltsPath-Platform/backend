# Phase 07: Ngân hàng câu hỏi (frontend)

Repo: `frontend`. Phụ thuộc phase 02 (xuất bản câu hỏi) và phase 05 (shell).

## Mục tiêu

R&D xem, tạo, duyệt và xuất bản câu hỏi trong khu quản trị.

## Việc cần làm

1. Danh sách có lọc theo kỹ năng, purpose, trạng thái, KP; đọc ô tìm kiếm trên thanh trên (`useAdminSearch`).
2. Xem phiên bản của một câu hỏi với trạng thái từng phiên bản (DRAFT, PUBLISHED, ARCHIVED).
3. Tạo câu hỏi, thêm phiên bản mới. Đọc `QuestionController` (`/api/content/questions`, `/api/content/admin/questions`) và `backend/docs/contracts/answer-spec-v1.md` để dựng form theo loại câu hỏi đang có; không tự nghĩ ra loại mới (nối cặp, sắp xếp câu, chép chính tả đang ngoài phạm vi sản phẩm).
4. Xuất bản và lưu trữ phiên bản bằng API ở phase 02; hiện lỗi `DIFFICULTY_REQUIRED`, `QUESTION_VERSION_IN_USE` và lỗi kiểm tra `answer_spec` bằng thông điệp dễ hiểu.
5. Không bao giờ hiển thị `answerSpec` dưới dạng JSON thô cho người dùng cuối nếu không cần; R&D xem được đáp án vì đây là khu quản trị, nhưng giữ trong màn soạn.
6. Đủ trạng thái loading, lỗi, rỗng.

## Test

Danh sách và lọc, luồng xuất bản thành công và thất bại, hiển thị lỗi theo `details.code` (mock `apiRequest`).

## Tiêu chí hoàn thành

Tạo câu hỏi DRAFT, xuất bản được từ UI, câu PUBLISHED lọc thấy được; lưu trữ câu đang dùng báo đúng lỗi. `typecheck`, `lint`, `test` xanh.
