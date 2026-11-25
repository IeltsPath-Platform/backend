# Phase 04 - Giao diện

Phủ: phần giao diện của E1, E7..E10, E12, E15, E23, E28, E33, E36, E38.
Đọc trước `frontend/AGENTS.md` và `.sdd/constraints/admin-ui.md`: không chữ giải thích dưới ô, lỗi nhập đỏ dưới ô, toast chỉ cho thông báo từ backend.

## File

- `src/features/admin/ai-drafts/types.ts`: thêm `SECTION_QUESTIONS`, `EXPIRED`, lý do loại mới; **không** để `z.enum` nghiêm làm hỏng cả danh sách khi gặp kind lạ.
- `src/features/admin/ai-drafts/lib/aiDraftLabels.ts`: nhãn tiếng Việt cho mọi mã mới (lý do loại, mã lỗi, mã 409/422); có test quét đủ mã.
- `src/features/admin/packages/api/sectionDraftApi.ts` (mới).
- `src/features/admin/packages/components/SectionAiButton.tsx`, `SectionAiRequestModal.tsx`, `SectionAiReviewModal.tsx` (mới).
- `SectionQuestionsEditor.tsx`: đặt nút cạnh "Thêm câu hỏi"; nút "Xuất bản N câu nháp" khi phần có câu nháp.
- `PackageEditorPage.tsx`: một lần gọi `open?versionId=` cho cả đề, truyền xuống theo `sectionId`.
- `src/features/admin/ai-drafts/components/DraftReview.tsx` (mới): phần duyệt, nhận bản nháp và nguồn (nếu có); `SectionAiReviewModal` chỉ bọc nó. Để màn khác dùng lại sau này.
- Dùng lại `QuestionDraftItemEditor` và `useDraftPolling` nếu khớp `itemId`; không khớp thì tách phần dùng chung, không chép.

## Nút theo trạng thái

| Trạng thái | Nút |
| --- | --- |
| Writing, Speaking, chưa có kỹ năng, loại đề không hỗ trợ, bản đề không phải nháp | không hiện |
| Chưa có transcript/đoạn văn | tắt, `title` nêu lý do |
| Không có lượt nào | "Sinh câu hỏi bằng AI" |
| `QUEUED`/`GENERATING` | "Đang sinh…", bấm mở trạng thái |
| `READY` | "Duyệt câu AI (M)" |
| `FAILED` | nút sinh lại; lý do hiện trong modal |

## Modal yêu cầu

Dạng câu (theo kỹ năng), số câu 1..10, ghi chú. Số câu gợi ý = mục tiêu thường dùng (10 cho Listening, 13 cho Reading) trừ số câu đang có,
kẹp vào 1..10. Hiện "Còn X/20 lượt hôm nay"; hết lượt thì nút gửi tắt. `409 SECTION_DRAFT_OPEN` -> chuyển thẳng sang lượt `details.draftId`.

## Modal duyệt

- Hai cột: trái là nguồn (bản chụp) cuộn độc lập; phải là các câu. Bấm một câu thì tô sáng đáp án trong nguồn (câu gõ) để đối chiếu.
- Đầu modal: "Yêu cầu N, dùng được M", danh sách câu bị loại kèm lý do và số câu trùng.
- Mỗi câu: ô chọn (mặc định **không** chọn, buộc người duyệt chủ động), đáp án, giải thích, sửa, bỏ. Sửa mở ngay trong modal.
- `sourceChanged`: khung cảnh báo và ô xác nhận trước khi bật nút duyệt. `sectionState` khác `OK`: chỉ cho từ chối.
- `409 DRAFT_CHANGED`: tải lại bản nháp, **giữ chữ đang gõ** trong ô sửa và báo câu đã đổi ở nơi khác.
- `409 DRAFT_ITEMS_DUPLICATED`: đánh dấu đỏ từng câu kèm "Trùng câu số k".
- Nút: "Thêm M câu vào phần", "Từ chối lượt này" (hỏi xác nhận). Không còn câu nào thì nút thêm tắt.
- Đóng modal khi đang sửa dở: hỏi trước khi bỏ. Duyệt xong: đóng, tải lại đề, toast số câu đã thêm.

## Trạng thái mạng và trình duyệt

- Polling 5 giây chỉ khi `QUEUED/GENERATING`; dừng khi đóng trang; một lần poll lỗi không xóa dữ liệu đang hiện, ba lần liên tiếp mới báo.
- Mất phản hồi khi gửi yêu cầu hoặc duyệt: tải lại `open`/chi tiết rồi quyết định, không tự gửi lại mù.
- Đổi bản đề hoặc rời trang khi đang gọi: bỏ kết quả cũ (AbortController), không cập nhật nhầm phần.
- Lỗi hiện **trong** modal (modal che trang phía sau).

## Test (vitest)

Mỗi dòng bảng nút; modal yêu cầu (kiểm nhập, hết lượt, 409 mở lại lượt); modal duyệt (chọn, sửa, bỏ, xung đột, nguồn đổi, trùng,
bỏ hết câu, từ chối); F5 giữa chừng vẫn thấy lượt; poll lỗi tạm thời; xuất bản loạt và lỗi `QUESTION_NOT_PUBLISHED` khi xuất bản đề.
Chạy: `npx.cmd vitest run src/features/admin/packages src/features/admin/ai-drafts src/features/admin/questions`, eslint, `npx.cmd tsc -b`.
