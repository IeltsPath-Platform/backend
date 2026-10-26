# Phase 13: Màn AI sinh nội dung, dựng khóa học, import Excel

Người làm: **Codex**. Nhánh hiện tại: `feat/content-author-screens` (frontend, worktree `../frontend-claude`, theo chỉ dẫn mới nhất của người dùng).
Trạng thái: Done về code (2026-10-23): A, B, C đã triển khai; menu "Sinh nội dung bằng AI" vẫn sau cờ `QUESTION_DRAFTS_ENABLED = false` — chờ backend merge, bật cờ và checklist thật

Repo: `frontend`. Phần A được phép triển khai local theo contract nhánh backend trước khi merge; bật tính năng phụ thuộc phase 08 + 09 đã merge và xác minh contract. Phần B cần phase 10 (+ 11 cho nút sinh bằng AI), phần C cần phase 12; B/C chưa được mở trong lượt này. Chỉ commit frontend local, không push/merge hoặc commit backend phase.

## Phạm vi hiện tại theo chỉ dẫn mới nhất 2026-10-23

- Chỉ phần A sinh/đọc/duyệt/từ chối nháp câu hỏi; dùng nhánh frontend hiện tại, không tạo `feat/content-ai-screens` hoặc đòi main-only trước khi viết local.
- Nguồn contract duy nhất cho phần A là `git show origin/feat/content-package-authoring:docs/contracts/ai-drafts-v1.md` và `question-drafts-v1.md`, ref đã fetch `1daabfe6`. Ở ref ban đầu `48fd4911` còn thiếu contract câu hỏi; sau thông báo push của người dùng đã fetch và đọc đủ cả hai contract. Không dùng implementation backend local chưa công bố làm nguồn suy đoán.
- Có đúng 4 loại câu: `MULTIPLE_CHOICE`, `TRUE_FALSE_NOT_GIVEN`, `FILL_IN_BLANK`, `SHORT_ANSWER`. Không có API sửa/xóa từng item trong phiên bản này; yêu cầu cũ 5 loại và PATCH/DELETE bên dưới đã được thay bằng phạm vi A hiện hành.
- `QUESTION_DRAFTS_ENABLED = false` tại `ai-drafts/draftFlags.ts`. Menu A ẩn bằng `enabled: false`; truy cập trực tiếp index/detail chỉ hiện **Tính năng đang chờ triển khai**, không mount picker, tải API hoặc polling. Route chỉ ở nhánh `isAuthorArea`; không mở quyền cho ADMIN.
- Không triển khai dựng khóa, sinh khóa hoặc import Excel; nội dung thiết kế B/C bên dưới giữ làm mục tiêu sau này, không mở rộng scope lượt này. Không thêm dependency hoặc API giả; các cờ/phần khác giữ nguyên.

## Mục tiêu

Tác giả dùng được các công cụ của phase 08–12 trên giao diện: yêu cầu AI sinh câu hỏi, xem/sửa/duyệt nháp; dựng course từ cây (soạn tay hoặc nhờ AI), xem phân tích, duyệt; import Excel kèm tải template. Chỉ `CONTENT_AUTHOR` thấy.

## Đọc trước

- Contract A: `ai-drafts-v1.md` và `question-drafts-v1.md` tại ref nhánh backend nêu trên. Contract dành cho B/C sau này: `course-drafts-v1.md`, `course-import-v1.md`, phần `importRef` trong `question-publication-v1.md`.
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

- **API/schema**: dùng `apiRequest` auth và Zod request/response cho đúng 5 route: `GET /api/content/admin/ai-drafts?kind=&status=&page=&size=`, `GET /api/content/admin/ai-drafts/{id}`, `POST /api/content/admin/ai-drafts/questions`, `POST /api/content/admin/ai-drafts/{id}/approve`, `POST /api/content/admin/ai-drafts/{id}/reject`. Chỉ CONTENT_AUTHOR; tác giả chỉ đọc nháp của mình, nháp người khác trả 404. Không có PATCH/DELETE item.
- **Danh sách bản nháp** `AiDraftsPage`: chỉ `kind=QUESTIONS`, trạng thái (badge `QUEUED` "Đang chờ", `GENERATING` "Đang sinh", `READY` "Chờ duyệt", `FAILED` "Lỗi", `APPROVED` "Đã duyệt", `REJECTED` "Đã từ chối"), thời gian; lọc trạng thái và phân trang `page`/`size` theo response mảng thuần. Không tự dựng `total`/`totalPages`; trang sau dựa vào số item nhận so với page size. Khi tải thành công còn bản nháp `QUEUED`/`GENERATING`, tải lại mỗi 5 giây; dừng khi terminal, lỗi hoặc unmount.
- **Form "Sinh câu hỏi"**: chọn đúng một KP bằng `KnowledgePointPicker` có sẵn (chỉ import, không sửa picker), 4 loại câu nêu ở phạm vi hiện tại, số câu 1..20 theo giới hạn mặc định `content.ai.max-items-per-request` của contract, độ khó `EASY | MEDIUM | HARD`, hướng dẫn thêm ≤500 ký tự và bộ đếm. Gửi xong chuyển chi tiết; nếu form đã unmount thì response muộn không điều hướng ngược. `429 AI_QUOTA_EXCEEDED` hiển thị nhãn tiếng Việt và giữ bản nhập. Query `?new=questions&knowledgePointId=…` mở sẵn form/điền KP; không triển khai phần B hoặc phase 15 trong lượt này.
- **Chi tiết bản nháp câu hỏi**: mỗi câu một thẻ hiện đề, phương án, **đáp án đúng nổi bật**, giải thích, mã lỗi distractor, độ khó; FILL hiện toàn bộ đáp án chấp nhận. `result.rejected` hiện riêng với index và lý do tiếng Việt. Chỉ chọn/bỏ chọn để duyệt, không sửa/xóa item. Duyệt gửi đúng `itemIndexes` gốc của `result.items`, không dùng thứ tự hiển thị của phương án hoặc index của danh sách rejected; từ chối gửi `{note}` ≤1000 ký tự. Chỉ thao tác ở READY, khóa khi pending/tải lại, giữ lựa chọn/ghi chú khi lỗi; ghi thành công phải GET tải lại, không vá trạng thái từ response ghi. Duyệt tạo câu hỏi DRAFT theo contract và vẫn cần xuất bản riêng; có link Ngân hàng câu hỏi. Hiện nhãn tiếng Việt cho mã lỗi generation/review và ghi chú cố định: "Câu do AI sinh có thể sai về nghĩa. Kiểm tra đáp án trước khi duyệt; câu vẫn phải xuất bản riêng."

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

- Menu A: cờ false thì ẩn với mọi vai trò, direct index/detail chỉ placeholder và không API/picker/polling; khi bật cờ chỉ CONTENT_AUTHOR thấy, ADMIN không thấy. Ba mục của thiết kế đầy đủ chỉ áp dụng khi từng phần được mở sau này.
- A: mock fetch kiểm đúng 5 route/auth/query/body/Zod và mảng thuần; chỉ 4 loại câu hỗ trợ; 429 hiện tiếng Việt và giữ form; response tạo nháp đến sau unmount không điều hướng; polling 5 giây chỉ QUEUED/GENERATING, dừng terminal/error/unmount; duyệt đúng `itemIndexes` gốc; rejected/FILL/đáp án/distractor hiển thị đúng; chỉ READY được ghi, pending khóa, lỗi giữ dữ liệu, thành công GET reload; không sửa/xóa item.
- B: nút Duyệt khóa khi phân tích có lỗi chặn; nút "Sinh thêm câu" mở form đã điền KP; 409 hiện danh sách mã trùng.
- C: file >5 MB không gọi API; "Ghi" khóa khi `valid = false` và khi đổi file sau kiểm; bảng lỗi hiện đúng sheet/dòng.

## Kiểm thủ công

Phần A chỉ chạy sau xác minh backend merge và bật cờ: đăng nhập `content@gmail.com` với backend thật (ghi rõ cấu hình mô hình sử dụng), sinh 5 câu cho một KP, kiểm đáp án/giải thích và câu bị loại, chọn duyệt 3 câu, thấy 3 câu `DRAFT` ở Ngân hàng câu hỏi; kiểm lỗi quota, từ chối và màn 390px. Không sửa/xóa item hoặc dùng giả dữ liệu để thay checklist thật.

Checklist B/C dành cho sau này: Sinh course bằng AI, xem `gaps`, bấm "Sinh thêm câu", duyệt course, mở đề cuối vừa tạo ở màn gói. Tải template, điền một dòng sai, kiểm tra thấy lỗi đúng dòng, sửa, ghi thành công. Đo 390 px không cuộn ngang trang.

## Hoàn thành khi

`npm run typecheck`, `npm run lint`, `npm test`, `npm run build` xanh; checklist thủ công đã chạy hoặc ghi rõ bước nào không chạy được.

## Kết quả

- 2026-10-23: đã fetch backend origin/main (`c600b3f3`); các phase 08, 09, 10, 11, 12 đều Pending. Các contract `ai-drafts-v1.md`, `course-drafts-v1.md`, `course-import-v1.md` chưa tồn tại trên origin/main.
- A chờ 08–09; B chờ 10 (nút sinh tự động chờ thêm 11); C chờ 12. Chưa tạo nhánh `feat/content-ai-screens`, chưa đoán endpoint/schema, chưa triển khai hoặc chạy checklist.

### Cập nhật hiện tại 2026-10-23 — chỉ phần A, implementation local sau cờ false

- Người dùng cho phép phần A ngay trên `feat/content-author-screens` theo contract nhánh, thay điều kiện tạo nhánh riêng/main-only trước đó. Đã đọc đúng hai contract qua `git show origin/feat/content-package-authoring` sau fetch ref `1daabfe6`; contract câu hỏi từng thiếu ở `48fd4911` nay đã có. Không đọc hoặc dựa vào implementation backend local chưa công bố để mở thêm scope.
- Đã triển khai `ai-drafts/types.ts`, `api/aiDraftApi.ts` cho đúng 5 route cùng Zod/mock-fetch; danh sách QUESTIONS lọc trạng thái, phân trang mảng thuần, polling 5 giây dừng đúng vòng đời; form một KP/4 loại/số câu 1..20/độ khó/hướng dẫn 500 ký tự; detail đáp án/giải thích/distractor/FILL/rejected, chọn itemIndexes gốc để duyệt hoặc note tối đa 1000 ký tự để từ chối. Ghi thành công tải lại GET, chỉ READY được duyệt/từ chối; pending khóa và lỗi giữ bản nhập. Guard unmount ngăn response tạo nháp muộn điều hướng khỏi trang đang xem.
- `QUESTION_DRAFTS_ENABLED = false` đã được đọc trực tiếp. Menu A đang ẩn; direct route index/detail hiện **Tính năng đang chờ triển khai**, không API, không picker hoặc polling. Chưa bật cờ/gọi endpoint mới trong UI đang triển khai. Không thay KnowledgePointPicker, dependency, backend code, cờ hoặc tính năng khác.
- Kiểm chứng cuối lượt: 62 test mock-fetch/schema API, 11 test component và 60 test page/menu/route qua. `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua (exit 0); build 3044 module, còn cảnh báo chunk lớn sẵn có. Toàn bộ `npm.cmd test` exit 1: 108/111 file qua, 785/791 test qua. Sáu lỗi nằm ngoài phần sửa: flashcard `owl-reactions.test.ts:109` (tired, nội dung chọn ngẫu nhiên), hai timeout UsersPage (`:69`, `:90`) và ba timeout PracticeEssays (`:259` cho TASK_1/TASK_2, `:336`). Không sửa hoặc bỏ test ngoài phạm vi.
- Chẩn đoán riêng một lần UsersPage và PracticeEssays: 2 file / 34 test qua. Kết quả này không thay thế lần full suite thất bại hoặc xác nhận nguyên nhân timeout. Không chạy lại full suite để đổi kết quả.
- Phần A chưa rollout: chỉ sau backend phase 08/09 đã merge và contract được xác minh mới bật cờ true, rồi chạy focused tests, đủ 4 cổng kiểm và checklist live. Chưa chạy sinh/duyệt/từ chối với backend thật hoặc màn 390px; không tuyên bố đã gọi mô hình thật hay hoàn thành phase.
- Commit local `e60e76c feat(content): add question draft generation and review` trên `feat/content-author-screens`, gồm 23 file trong tính năng và menu/route/test liên quan. Không push thêm vào [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29), không merge hoặc commit ghi chú backend phase. B/C giữ nguyên Blocked; không viết course builder, sinh course, import/template/download hoặc tính năng review/work queue. Các cờ resource/rubric vẫn false, phase 02 giữ nguyên.

## Việc cho bên kia

- Phần A chờ backend phase 08/09 công bố/merge code cùng hai contract rồi xác minh merge để bật cờ và kiểm live. Contract đã có ở nhánh; không yêu cầu viết lại các route đã mô tả hoặc dùng code phase 09 chưa công bố làm bằng chứng đã sẵn sàng rollout.
- B/C tiếp tục chờ backend và contract theo phụ thuộc ban đầu; không thuộc phạm vi implementation hiện tại.

### Cập nhật 2026-10-23 — B, C và sửa/xóa câu (A)

- Lượt 2026-10-23 (Claude Code tiếp quản vì Codex hết token), nhánh `feat/content-author-screens`, đã push vào [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29). Phần A thêm sửa và xóa từng câu (`PATCH`/`DELETE …/items/{index}`, lý do `DUPLICATE_STEM`, `422 INVALID_DRAFT_ITEM`). Phần B: danh sách và trình soạn bản nháp course, sinh bằng AI, phân tích, duyệt. Phần C: tải template bằng `apiDownload`, kiểm tra, bảng lỗi theo sheet/dòng/cột, import.
- Sửa lỗi của lượt trước: báo cáo `422` của import nằm trong `details` của phản hồi lỗi (không ở gốc), trang cũ đọc sai chỗ nên không hiện bảng lỗi.
- Test thêm: `httpClient.download.test.ts`, `CourseImportPage.test.tsx` (8), sửa hai lỗi kiểu trong test của B.
- Các mục menu "Dựng khóa học" và "Nhập Excel" hiện cho CONTENT_AUTHOR; nếu backend chưa merge thì gọi API sẽ lỗi, đặt cờ nếu cần ẩn.
- typecheck, lint, build qua; `npm test` 1047/1049 qua, lỗi còn lại là flashcard `owl-reactions.test.ts` (nội dung chọn ngẫu nhiên, ngoài phạm vi, không sửa). Chưa chạy checklist thủ công với backend thật.
