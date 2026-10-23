# Phase 13: Màn AI sinh nội dung, dựng khóa học, import Excel

Người làm: **Codex**. Nhánh: `feat/content-ai-screens` (frontend, worktree `../frontend-claude`, tạo nhánh từ `origin/main` mới nhất).
Trạng thái: Blocked — cả phần A/B/C chưa có backend và contract tương ứng trên origin/main.

Repo: `frontend`. Phụ thuộc: từng phần dưới đây bắt đầu khi contract và API tương ứng đã trên `main`: phần A cần phase 08 + 09, phần B cần phase 10 (+ 11 cho nút sinh bằng AI), phần C cần phase 12. Có thể mở nhiều PR nhỏ theo từng phần.

## Mục tiêu

Tác giả dùng được các công cụ của phase 08–12 trên giao diện: yêu cầu AI sinh câu hỏi, xem/sửa/duyệt nháp; dựng course từ cây (soạn tay hoặc nhờ AI), xem phân tích, duyệt; import Excel kèm tải template. Chỉ `CONTENT_AUTHOR` thấy.

## Đọc trước

- Contract: `ai-drafts-v1.md`, `course-drafts-v1.md`, `course-import-v1.md`, phần `importRef` trong `question-publication-v1.md`.
- Quy tắc frontend ở `plan.md` mục "Quy tắc cả hai agent phải theo" (không thêm dependency, `apiRequest`, `useAdminResource`, `AdminPage`, `AdminState`, `adminErrorCode`, `adm-*`, `lucide-react`, Zod, form controlled).
- Mẫu: `src/features/admin/questions/` (danh sách, chi tiết, `VersionCard`), `questions/components/KnowledgePointPicker.tsx` (chỉ import), phần upload file của phase 02 (`uploadAudio` dùng `FormData`).

## Menu và route

Thêm vào `ADMIN_MENU` với `roles: ['CONTENT_AUTHOR']` và trong nhánh `isAuthorArea` của `AdminRoutes.tsx` (chỉ thêm dòng, không sửa dòng của người khác). Tên đề xuất, đổi được khi review:

| Menu | Path | Icon gợi ý |
|---|---|---|
| "Sinh nội dung bằng AI" | `/content-author/ai-drafts` (`:draftId` chi tiết) | `Sparkles` |
| "Dựng khóa học" | `/content-author/course-drafts` (`:draftId` trình soạn) | `Network` |
| "Import Excel" | `/content-author/course-import` | `FileSpreadsheet` |

## Thư mục

```text
src/features/admin/ai-drafts/       types.ts, api/aiDraftApi.ts, lib/aiDraftLabels.ts, pages/, components/
src/features/admin/course-drafts/   types.ts, api/courseDraftApi.ts, pages/, components/
src/features/admin/course-import/   types.ts, api/courseImportApi.ts, pages/CourseImportPage.tsx
```

## A. Sinh câu hỏi bằng AI (sau phase 08, 09)

- **Danh sách bản nháp** `AiDraftsPage`: bảng loại (câu hỏi/course), trạng thái (badge `QUEUED` "Đang chờ", `GENERATING` "Đang sinh", `READY` "Chờ duyệt", `FAILED` "Lỗi", `APPROVED` "Đã duyệt", `REJECTED` "Đã từ chối"), thời gian; phân trang theo API; lọc loại và trạng thái. Có bản nháp `QUEUED`/`GENERATING` thì tự tải lại mỗi 5 giây (`setInterval` gọi `reload`, dọn khi rời trang hoặc không còn việc chạy).
- **Form "Sinh câu hỏi"**: chọn KP (`KnowledgePointPicker`), loại câu (5 loại tự chấm được; không có Essay/Speaking), số câu (1..giới hạn từ contract), độ khó, hướng dẫn thêm (≤500 ký tự, có bộ đếm). Gửi xong chuyển sang chi tiết. `429 AI_QUOTA_EXCEEDED` → "Bạn đã dùng hết lượt sinh hôm nay". Trang đọc query `?new=questions&knowledgePointId=…` để mở sẵn form và điền KP (dùng từ nút "Sinh thêm câu" ở phần B và từ màn "Việc cần làm" của phase 15).
- **Chi tiết bản nháp câu hỏi**: mỗi câu một thẻ hiện đề, phương án, **đáp án đúng nổi bật**, giải thích, mã lỗi từng phương án, độ khó; câu bị loại khi sinh hiện riêng kèm lý do. Sửa từng câu (form inline, gửi `PATCH`, lỗi kiểm tra hiện dưới câu), bỏ câu (`DELETE`, hỏi xác nhận). Ô chọn nhận/bỏ từng câu, nút "Duyệt các câu đã chọn" và "Từ chối bản nháp" (kèm ghi chú). Duyệt xong: thông báo số câu đã tạo ở `DRAFT` kèm link sang Ngân hàng câu hỏi. Ghi chú cố định: "Câu do AI sinh có thể sai về nghĩa. Kiểm tra đáp án trước khi duyệt; câu vẫn phải xuất bản riêng." `FAILED` hiện mã lỗi bằng tiếng Việt (`NOT_CONFIGURED` "Chưa cấu hình mô hình AI", `INVALID_REPLY` "AI trả kết quả không hợp lệ", `FAILED` "Gọi AI thất bại").

## B. Dựng khóa học (sau phase 10; nút AI sau phase 11)

- **Danh sách** dùng chung API bản nháp với `kind=COURSE`; nút "Bản nháp trống" và "Sinh bằng AI".
- **Form "Sinh bằng AI"**: band, kỹ năng, chọn KP (đường chính) hoặc nhập chủ đề dạng chữ, số lesson, hướng dẫn. Gửi trả `202`, theo dõi trạng thái như phần A.
- **Trình soạn** `CourseDraftEditorPage` (desktop 2 cột, mobile 1 cột):
  - thông tin course; danh sách topic có lên/xuống; trong mỗi topic danh sách lesson; mỗi lesson: tiêu đề, tóm tắt, KP (picker), block `TEXT` (textarea) và `EXERCISE` (chọn câu bằng `QuestionChooser` hoặc nhập `importRef`), gói practice set (chọn từ gói `PUBLISHED` chưa gắn);
  - mỗi topic có ô mã và tiêu đề đề cuối; course cũng vậy; ghi chú: "Duyệt sẽ tạo đề cuối rỗng ở bản nháp; soạn tiếp ở màn Đề & gói câu hỏi";
  - mốc practice test (chọn gói, sau topic thứ mấy);
  - nút "Lưu" (`PUT`) và "Phân tích" (`GET …/analysis`).
- **Phân tích**: ba nhóm hiện riêng: lỗi chặn (`PREREQUISITE_ORDER` kèm cặp KP, `PACKAGE_ALREADY_LINKED`, mã trùng), cảnh báo (`KP_NOT_COVERED`), thiếu kho (`gaps`). Bản nháp AI thêm `unmatchedTopics` và `HALLUCINATED_REFERENCE`. Mỗi mục `gaps` có nút "Sinh thêm câu cho KP này" mở form của phần A đã điền sẵn KP.
- **Duyệt**: nút chỉ bật khi không còn lỗi chặn; hỏi xác nhận. Xong thì hiện "Đã tạo khóa học (chưa kích hoạt)" kèm link sang "Khóa học & chủ đề" và danh sách đề cuối vừa tạo, mỗi cái link sang editor gói. `409 CODE_CONFLICT` hiện danh sách mã trùng.
- Sau mỗi thao tác ghi: tải lại từ server, không vá state cục bộ.

## C. Import Excel (sau phase 12)

- Nút "Tải template" ngay cạnh ô chọn file. **`apiRequest` không tải được file nhị phân** (`parse` trong `src/lib/httpClient.ts` đọc body bằng `text()` rồi `JSON.parse`), nên thêm vào `httpClient.ts` một hàm `apiDownload(path, { signal })` trả `Blob`: dùng lại `send` (bearer, timeout), cùng luồng làm mới token khi `401` như `apiRequest`, lỗi thì ném `HttpError` như cũ. Lưu bằng `URL.createObjectURL` + thẻ `<a download>` rồi `revokeObjectURL`. Không thêm thư viện. Thêm test cho `apiDownload` (thành công, 401 làm mới token rồi thử lại, lỗi server).
- Chọn file (`accept=".xlsx"`, kiểm ≤5 MB phía client) → "Kiểm tra" gọi `validate`. Hiện thống kê sẽ tạo, bảng lỗi và bảng cảnh báo theo sheet/dòng/cột/mã/thông điệp (bảng trong `adm-table-wrap` cuộn ngang trên mobile).
- Nút "Ghi" chỉ bật khi `valid = true` và file không đổi từ lần kiểm; gửi lại cùng file. `422` hiện lại bảng lỗi. Xong: "Đã tạo khóa học (chưa kích hoạt)" kèm link như phần B.
- Lỗi file: `FORMULA_NOT_ALLOWED` "File có công thức, hãy dán giá trị", `TEMPLATE_VERSION_MISMATCH` "Template đã cũ, tải template mới", quá lớn "File lớn hơn 5 MB".

## Test (Vitest + Testing Library, mock lớp `api`)

- Menu: `CONTENT_AUTHOR` thấy ba mục mới, `ADMIN` không thấy.
- A: form không có Essay/Speaking; 429 hiện nhãn tiếng Việt; danh sách tự tải lại khi có việc đang chạy và dừng khi hết; duyệt gửi đúng `itemIndexes`; câu bị loại hiện lý do.
- B: nút Duyệt khóa khi phân tích có lỗi chặn; nút "Sinh thêm câu" mở form đã điền KP; 409 hiện danh sách mã trùng.
- C: file >5 MB không gọi API; "Ghi" khóa khi `valid = false` và khi đổi file sau kiểm; bảng lỗi hiện đúng sheet/dòng.

## Kiểm thủ công

Đăng nhập `content@gmail.com` với backend thật (LLM giả hoặc thật, ghi rõ dùng cái nào): sinh 5 câu cho một KP, sửa một câu, duyệt 3 câu, thấy 3 câu `DRAFT` ở Ngân hàng câu hỏi. Sinh course bằng AI, xem `gaps`, bấm "Sinh thêm câu", duyệt course, mở đề cuối vừa tạo ở màn gói. Tải template, điền một dòng sai, kiểm tra thấy lỗi đúng dòng, sửa, ghi thành công. Đo 390 px không cuộn ngang trang.

## Hoàn thành khi

`npm run typecheck`, `npm run lint`, `npm test`, `npm run build` xanh; checklist thủ công đã chạy hoặc ghi rõ bước nào không chạy được.

## Kết quả

- 2026-10-23: đã fetch backend origin/main (`c600b3f3`); các phase 08, 09, 10, 11, 12 đều Pending. Các contract `ai-drafts-v1.md`, `course-drafts-v1.md`, `course-import-v1.md` chưa tồn tại trên origin/main.
- A chờ 08–09; B chờ 10 (nút sinh tự động chờ thêm 11); C chờ 12. Chưa tạo nhánh `feat/content-ai-screens`, chưa đoán endpoint/schema, chưa triển khai hoặc chạy checklist.
