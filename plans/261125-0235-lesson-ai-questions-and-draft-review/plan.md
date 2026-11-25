# Sinh câu hỏi bằng AI trong bài học, trang bản nháp chỉ xem lại và duyệt

- Status: code và kiểm thử hoàn tất; còn xác minh thủ công LLM/trình duyệt và đếm dữ liệu đã duyệt `skill = null`
- Cập nhật: 2026-11-25
- Repo: `backend/` (content-service, config-repo của api-gateway) và `frontend/`
- Dựa trên phần code đã hoàn tất của [`261124-2200-section-ai-question-generation`](../261124-2200-section-ai-question-generation/plan.md); nghiệm thu thủ công của plan đầu không chặn plan này.
  Plan này dùng lại `itemId`, `revision`, khóa, hết hạn (phase 03) và `DraftReview.tsx` (phase 04) theo code thật.

## Mục tiêu

- Block bài tập của bài học nháp có nút "Sinh câu hỏi bằng AI"; nguồn là một điểm kiến thức của bài, câu duyệt xong vào thẳng block.
- Trang "Sinh nội dung bằng AI" thành "Duyệt câu hỏi AI": chỉ xem lại và duyệt, gồm cả câu cho phần của đề và câu cho bài học.
- Sửa lỗi câu AI duyệt theo điểm kiến thức bị `skill = null`.

## Quyết định

| # | Quyết định | Nguồn |
| --- | --- | --- |
| Q1 | Sinh ở nơi cần dùng; trang bản nháp không tạo mới | Chủ dự án, 2026-11-25 |
| Q2 | Một thành phần duyệt, dùng ở modal tại chỗ và ở trang bản nháp | Theo Q1 |
| Q3 | Ẩn độ khó, mặc định `MEDIUM` | Chủ dự án, 2026-11-25 |
| Q4 | Đổi tên menu thành "Duyệt câu hỏi AI" | Chủ dự án, 2026-11-25 |
| Q5 | Bỏ link "Sinh câu cho KP này" ở hàng việc | Chủ dự án, 2026-11-25 |

## Phase

| # | Phase | Trạng thái |
| --- | --- | --- |
| 01 | [Sinh trong bài học và trang bản nháp](phase-01-lesson-exercise-and-review-page.md) (gồm bảng ngoại lệ L1..L11) | Code/test xong; thủ công và kiểm kê dữ liệu còn chờ |

## Kết quả đối chiếu trước triển khai (Bước 0)

- Plan sinh câu hỏi cho phần đã có `itemId`/`revision`, khóa khi đọc/sửa/duyệt và `DraftReview.tsx`; phần QUESTIONS hiện tại chưa dùng các cơ chế đó. Đây là phần cần triển khai trong plan này, không phải lệch thiết kế.
- QUESTIONS hiện dùng `index`/`itemIndexes` trong API sửa/bỏ/duyệt; mở rộng API hiện có sang `itemId`/`revision`, vẫn giữ luồng bản nháp cũ không có `lessonId` để đưa câu vào ngân hàng.
- `DraftReview.tsx` hiện chuyên cho SECTION_QUESTIONS và API/phần tử của câu hỏi phần; mở rộng adapter/props để dùng cùng màn duyệt cho QUESTIONS của bài học và bản nháp QUESTIONS cũ.
- `QuestionDraftRequest` hiện yêu cầu `difficulty`, còn use case duyệt đặt `skill = null`; bỏ độ khó khỏi yêu cầu mới (server đặt MEDIUM), vẫn đọc yêu cầu đã lưu, và thêm suy luận kỹ năng theo quy tắc của phase.
- Migration đã áp dụng cao nhất là V150 (bao gồm ràng buộc lượt mở cho bản nháp phần); chỉ thêm migration kế tiếp, không sửa V150.
- Trạng thái `EXPIRED` và TTL 14 ngày đã có cho SECTION_QUESTIONS; áp dụng cho QUESTIONS có `lessonId`, giữ nguyên bản nháp QUESTIONS cũ.
- Chưa thấy lệch thiết kế cần quyết định lại. Kiểm tra gate plan trước: `SectionQuestionDraftReviewUseCase` và `DraftReview.tsx` tồn tại; các báo cáo Maven gần nhất không có lỗi/thất bại.

## Kết quả hoàn tất code

- Đối chiếu với plan đầu không phát hiện lệch thiết kế. Dùng cấu trúc `itemId`/`revision`, khóa bản nháp, `EXPIRED`/14 ngày và `DraftReview.tsx` theo code thực tế; thêm hành vi `lessonId` cho `QUESTIONS`.
- Thêm migration V151; giữ nguyên V149/V150 và các migration đã áp dụng.
- ArchitectureTest phát hiện use case phụ thuộc trực tiếp vào `ContentAiProperties`; chuyển sang cổng `QuestionDraftLimits` ở application, đồng thời bỏ các dòng vi phạm tương ứng khỏi frozen architecture baseline.
- Test hết hạn phát hiện toán tử JSONB `?` xung đột placeholder của `NamedParameterJdbcTemplate`; dùng `jsonb_exists`.
- Đồng bộ fixture controller với `knowledgePointIds`, `itemId`/`revision`, và test slice với các dependency của `LessonAuthoringUseCase`.
- Không chạy LLM thật, không thử giao diện trên trình duyệt thật. Không có kết nối tới cơ sở dữ liệu ứng dụng để đếm số câu đã duyệt đang có `skill = null`; không thay đổi dữ liệu.

## Tiêu chí hoàn thành

- Mọi dòng L1..L11 có test; tên test ghi hành vi, không ghi mã L.
- Schema mới dùng migration mới, không sửa migration đã áp dụng.
- Contract cập nhật cùng code: `docs/contracts/ai-drafts-v1.md` (`origin`, `lessonId`, `blockId`), contract soạn bài học
  (`publish-drafts`, `expectedQuestionVersionIds`), `GET /admin/questions` (`knowledgePointIds`); `docs/ai-policy.md` (prompt có câu đã có của KP).
- `mvn -q -pl services/content-service -am test`, test Gateway, vitest, eslint, `tsc -b` khu admin đạt.
- Thử tay với LLM thật một điểm ngữ pháp; báo số câu đã duyệt trước đây đang `skill = null`.
