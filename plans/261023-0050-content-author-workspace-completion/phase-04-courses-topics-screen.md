# Phase 04: Màn "Khóa học & chủ đề"

Người làm: **Codex** (đổi từ Claude Code ngày 2026-10-23). Nhánh: `feat/content-author-screens` (frontend, worktree `../frontend-claude`).
Trạng thái: In progress — quyết định từ chối lưu thứ tự trùng đã chốt, lỗi form theo trường đã triển khai và focused tests qua; mũi tên còn Blocked chờ reorder nguyên tử, checklist thật chưa chạy và full suite còn 2 lỗi ngoài phạm vi thay đổi.

Repo: `frontend`. API khóa học/chủ đề và lỗi `SORT_ORDER_TAKEN` đã có; đổi vị trí bằng mũi tên cần API/contract reorder nguyên tử.

## Mục tiêu

Tác giả tạo/sửa khóa học theo band và cây chủ đề, gán chủ đề vào khóa, đổi thứ tự và trạng thái, mà không phải gọi API tay.

## API (đã có)

Khóa học:
- `GET /api/content/courses` → danh sách (xem `CourseResponse` trong backend để viết schema; đọc file `api/dto/response/CourseResponse.java`).
- `POST /api/content/admin/courses` body `{ code (≤100), name (≤255), bandLevel (0.0..9.0) }`.
- `PUT /api/content/admin/courses/{id}` body `{ name, bandLevel, status: "ACTIVE" | "INACTIVE" }`.

Chủ đề (`TopicController`, path `/api/content/admin/topics` hoặc `/api/content/topics`):
- `GET` → cây chủ đề (`TopicTreeResponse`; đọc file DTO để viết schema). `lessons/api/lessonApi.ts` đã có `fetchTopics` — **dùng lại**, mở rộng schema nếu thiếu trường.
- `POST` body `{ parentTopicId?, code (≤100), name (≤255), sortOrder, bandMin?, bandMax? (0..9), skill?: LISTENING|READING|WRITING|SPEAKING, courseId? }`.
- `PUT /{id}` body `{ parentTopicId?, name, sortOrder, status: ACTIVE|INACTIVE, bandMin?, bandMax?, skill?, courseId? }`.

Ràng buộc nghiệp vụ backend đã có (hiện lỗi đúng, không tự kiểm lại): đổi skill của topic đã có bài `PUBLISHED` → `409 TOPIC_SKILL_LOCKED`; topic `ACTIVE` có bài `PUBLISHED` phải có skill; không nhận skill `ALL`. Đọc `DATABASE_V5.md` mục `topics.skill` trong `backend/.sdd/database/` để biết đủ mã lỗi.

**Quyết định cuối 2026-10-23:** `sortOrder` phải duy nhất giữa các topic cùng `parentTopicId`; toàn bộ topic gốc chung một nhóm, kể cả khác khóa học. Tạo mới hoặc đổi parent/order sang vị trí đã có trả `409`, `details.code = SORT_ORDER_TAKEN`, message server **"Thứ tự N đã có chủ đề khác cùng cấp"**; không di chuyển topic khác. Sửa metadata mà giữ nguyên parent/order được miễn kiểm trùng để không khóa dữ liệu cũ. Nguồn: `lesson-authoring-v1.md`, mục **Topic sort order**; `TopicSortOrders` và `UpdateTopicUseCase`.

## Tạo

```text
src/features/admin/courses/
  types.ts, api/courseApi.ts
  pages/CoursesPage.tsx
  components/CourseForm.tsx, components/TopicTree.tsx, components/TopicForm.tsx
  pages/CoursesPage.test.tsx
```

- Menu: `{ path: '/content-author/courses', label: 'Khóa học & chủ đề', icon: Library, roles: ['CONTENT_AUTHOR'] }`, đặt sau "Tổng quan nội dung". Route trong nhánh `isAuthorArea`.
- Bố cục: cột trái danh sách khóa học (mã, tên, band, trạng thái) + form tạo; chọn một khóa → cột phải là cây chủ đề của khóa đó (lọc theo `courseId`; thêm mục "Chủ đề chưa gán khóa"). Mobile: 1 cột, chọn khóa bằng `select`.
- `TopicTree`: hiển thị lồng nhau theo `parentTopicId`, mỗi nút có mã, tên, kỹ năng (màu `--adm-skill-*` kèm chữ), band min–max, trạng thái, số bài nếu DTO có. Nút "Sửa" mở `TopicForm` inline; nút "Thêm chủ đề con". **Chỉ dẫn cuối 2026-10-23:** nhóm dữ liệu cũ trùng sortOrder vẫn khóa mũi tên, hướng dẫn "Sửa thứ tự trong form từng chủ đề"; không còn chờ người dùng chọn phương án. Người dùng sửa một giá trị rõ ràng trong form; không tự chuẩn hóa, reindex hoặc chọn số tạm. Luồng mũi tên 2 PUT cũ không đáp ứng contract mới: PUT đầu trỏ vào vị trí đang có sẽ trả 409, cần reorder nguyên tử trước khi coi tính năng này hoạt động với contract mới.
- `CourseForm`/`TopicForm`: kiểm client: code/name bắt buộc, band 0..9 bước 0.5, `bandMin ≤ bandMax`. Với `SORT_ORDER_TAKEN`, giữ nguyên mọi giá trị form, hiện đúng message server ngay dưới ô "Thứ tự chủ đề", đặt `aria-invalid` và liên kết `aria-describedby` tới lỗi `role="alert"`. Xóa lỗi trường khi sửa thứ tự/parent hoặc gửi lại; lỗi chung tiếp tục hiện ở đầu form. Riêng `TOPIC_SKILL_LOCKED` → "Chủ đề đã có bài xuất bản, không đổi kỹ năng được". Không autosort, chuẩn hóa hoặc retry sau lỗi.
- Liên kết: từ một chủ đề có nút "Mở bài học" dẫn tới `/content-author/lessons?topicId=…` (nếu `LessonsPage` chưa đọc `topicId` từ query, thêm việc đọc đó; chỉ sửa tối thiểu).

## Test

Hiện danh sách khóa; tạo khóa gửi đúng body (band dạng số); sửa trạng thái; cây chủ đề lồng đúng cấp; `bandMin > bandMax` bị chặn; lỗi `TOPIC_SKILL_LOCKED` hiện nhãn tiếng Việt; menu: CONTENT_AUTHOR thấy, ADMIN không. Tạo/sửa gặp 409 `SORT_ORDER_TAKEN`: message server nằm dưới ô thứ tự và liên kết bằng ARIA, giữ mọi giá trị form, không gọi ghi thêm hoặc tự chọn thứ tự khác; sửa order/parent hoặc gửi lại xóa lỗi cũ; lỗi generic không bị gắn nhầm vào ô thứ tự.

## Kiểm thủ công

Tạo khóa "Intensive 7.0" band 7.0, tạo chủ đề Writing con của nó, mở trang học viên chọn khóa thấy chủ đề (nếu chủ đề có bài xuất bản). Thử đổi skill của chủ đề đã có bài xuất bản → thấy lỗi khóa.

## Hoàn thành khi

`npm run typecheck`, `npm run lint`, `npm test`, `npm run build` xanh.

## Kết quả

- 2026-10-23, nhánh frontend `feat/content-author-screens`, worktree `frontend-claude`: thêm màn khóa học/chủ đề, form controlled, chọn khóa trên mobile, cây lồng theo parentTopicId, tạo chủ đề con, band bước 0.5, trạng thái, đổi thứ tự bằng 2 PUT giữa anh em; tái dùng fetchTopics và giữ đủ metadata. Menu/route chỉ CONTENT_AUTHOR. LessonsPage đọc và cập nhật query topicId.
- Schema theo CourseResponse/TopicTreeResponse/TopicResponse và request DTO thật. Đã đọc DATABASE_V5 mục topics.skill và các use case. Course.create tạo ACTIVE (hiển thị đúng trên form). UpdateTopicUseCase không gỡ course/skill khi gửi null, nên form không đưa lựa chọn gỡ course/skill đã gán.
- 16 test khóa học/menu/bài học qua; typecheck và build qua. Lần chạy toàn bộ mới nhất: 92 file / 556 test qua, không thay đổi mã flashcard. Hai lỗi flashcard ở lượt kiểm trước không lặp lại trong lượt này (test phụ thuộc lựa chọn ngẫu nhiên).
- Chưa chạy checklist tạo Intensive 7.0/đổi skill/chọn khóa ở web học: HTTP localhost:8080/actuator/health timeout, chưa xác nhận backend thật khả dụng hoặc phiên đăng nhập content@gmail.com. Không tuyên bố đã kiểm thủ công.
- Kết quả kiểm tra sau khi tích hợp các màn: typecheck, lint, build qua; npm test 95 file / 573 test qua. Đã kiểm tra thêm gateway 8080 và 18080 ngoài sandbox: không kết nối được. Commit riêng: `1642b27 feat(courses): manage courses and nested topics`. Chờ cập nhật link PR sau review.
- Rà soát bổ sung: giữ form chủ đề chưa lưu khi metadata khóa học tải lại; mũi tên chỉ đổi vị trí giữa các chủ đề có cùng parentTopicId thật (kể cả khi cha nằm ở khóa khác). Đã thêm test cho cả hai trường hợp.
- Backend cho phép sortOrder trùng; hoán đổi 2 giá trị bằng nhau không đổi được thứ tự. Người dùng đã chọn chuẩn hóa cả nhóm khi trùng rồi đổi vị trí, cho phép nhiều hơn 2 PUT (2026-10-23). Đã triển khai trong TopicTree, giữ luồng 2 PUT khi nhóm không trùng; khóa thao tác xung đột trong lúc ghi/tải lại.
- Đã push; [draft PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29), commit bổ sung `537dbc7`. Khóa mũi tên và các trình soạn khác khi đang sửa một chủ đề, khóa mở form khi đang ghi/tải lại; test hồi quy qua, reviewer xác nhận lỗi ghi đè đã được giải quyết. Nhãn kỹ năng dùng chữ màu text và viền token kỹ năng để giữ tương phản.
- Lượt kiểm trước khi xử lý thứ tự trùng: typecheck/lint/build qua; npm test 577 qua / 1 lỗi flashcard tired ngẫu nhiên ngoài phạm vi (`owl-reactions.test.ts:109`). Không sửa/bỏ test flashcard để che lỗi; không tự merge PR.
- Kiểm chứng sau thay đổi chuẩn hóa: 13 test CoursesPage qua, gồm nhóm 3 anh em trùng thứ tự, không ghi sang nhóm khác cha, lỗi giữa chuỗi ghi phải dừng và tải lại thứ tự đã lưu một phần. Typecheck/lint/build qua. Toàn bộ npm test: 578 qua / 2 lỗi flashcard confused/tired ngoài phạm vi (`owl-reactions.test.ts:87,109`); chưa đạt yêu cầu toàn bộ test xanh. Checklist backend thật và 390px chưa chạy; giữ PR #29 ở draft.
- Đã commit/push `750e1f7 fix(courses): normalize tied sibling order before moving topics` vào [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) bằng push thường; reviewer xác nhận xử lý chuẩn hóa đúng lựa chọn, 13 test CoursesPage qua, không phát hiện lỗi mới. Không sửa code backend hoặc tự merge.
- **Cập nhật thay thế lượt trước, 2026-10-23:** theo yêu cầu mới, đã bỏ luồng chuẩn hóa trong `750e1f7` bằng thay đổi bổ sung, không viết lại lịch sử PR. Nhóm trùng bị khóa mũi tên cả ở UI lẫn handler, hiển thị "Nhóm này có thứ tự trùng. Chờ quyết định xử lý." Form/sửa/tạo, nhóm khác cha không trùng và hoán đổi 2 PUT thông thường được giữ. Không chọn phương án thay người dùng.
- Kiểm chứng lượt mới nhất: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua; 78 test author/menu/route qua. Toàn bộ `npm.cmd test`: 580 qua / 1 lỗi flashcard confused ngoài phạm vi tại `owl-reactions.test.ts:87` (94/95 file qua); không sửa hoặc bỏ test này.
- Checklist chưa chạy: tạo Intensive 7.0, thêm chủ đề con Writing, xem ở web học, thử đổi kỹ năng chủ đề đã có bài xuất bản, kiểm 390px. Gateway 8080/18080 timeout khi kiểm ngoài sandbox. Phần trùng sortOrder chờ quyết định mới; không merge PR.
- Đã push `0e67156 fix(courses): pause tied topic ordering pending a decision` vào [draft PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) bằng push thường. Reviewer xác nhận chuẩn hóa đã được gỡ, nhóm trùng bị khóa đúng chỉ dẫn mới. Không sửa lịch sử commit hoặc backend code.

### Lịch sử lượt trước 2026-10-23 — giữ quyết định tạm dừng thứ tự trùng

- Khóa học, cây chủ đề, form tạo/sửa và luồng nhóm không trùng tiếp tục hoạt động. Mũi tên của nhóm trùng vẫn khóa ở UI và handler; không chuẩn hóa/reindex cả nhóm hoặc gửi lại tự động.
- Form gửi đúng thao tác và giá trị người dùng nhập, hiển thị lỗi server khi có xung đột. Không tự chọn sortOrder khác, không chuẩn hóa nhóm và không retry sau lỗi để vượt quyết định đang chờ. Đã thêm test hồi quy server conflict; source khóa học/chủ đề giữ nguyên trong lượt này.
- Kiểm chứng cuối lượt: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua (exit 0). Nhóm package API/index/editor/SectionEditor, AdminRoutes và CoursesPage: 6 file / 84 test qua, gồm hồi quy conflict. Toàn bộ `npm.cmd test` exit 1: 102/103 file qua, 670/672 test qua; 2 lỗi ngẫu nhiên đã biết ở `flashcards/lib/owl-reactions.test.ts:87` (confused) và `:109` (tired), ngoài phạm vi. Không sửa/bỏ test, không chạy lại để đổi kết quả. Build còn cảnh báo kích thước chunk sẵn có.
- Không tuyên bố tất cả cổng xanh hoặc phase hoàn thành. Commit local chỉ thêm test hồi quy: `5b14edc test(courses): preserve topic drafts after order conflicts`. Không push thêm hoặc merge; [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) vẫn nháp, không cập nhật remote trong lượt này.
- Gateway 8080/18080 timeout trong lượt này; tạo Intensive 7.0/chủ đề con Writing, xem ở web học, kiểm lỗi đổi skill đã xuất bản và màn 390px vẫn chưa chạy. Chỉ phần xử lý thứ tự trùng cần quyết định mới của người dùng; không yêu cầu backend triển khai lại API khóa học/chủ đề đã có.

### Cập nhật mới nhất 2026-10-23 — quyết định cuối: từ chối lưu thứ tự đã có

- Quyết định đã chốt: không autosort/chuẩn hóa, không còn chờ lựa chọn của người dùng. Backend trả 409 `SORT_ORDER_TAKEN` với message **"Thứ tự N đã có chủ đề khác cùng cấp"**; UI giữ toàn bộ giá trị để người dùng tự sửa thứ tự hoặc parent rồi gửi lại.
- `TopicForm.tsx` dùng `orderError` riêng và `useId` để liên kết lỗi `role="alert"` ngay dưới trường với `aria-invalid`/`aria-describedby`. Đổi thứ tự, đổi parent hoặc resubmit xóa lỗi cũ. Lỗi khác vẫn hiển thị ở khối lỗi chung, không gắn nhầm vào trường thứ tự.
- `TopicTree.tsx` đã bỏ câu "Chờ quyết định xử lý"; nhóm trùng cũ vẫn khóa mũi tên và hướng dẫn sửa trực tiếp trong form. Không tự đổi các topic khác hoặc chọn số tạm để lách contract.
- Giới hạn còn lại: luồng 2 PUT đổi chỗ của nhóm không trùng sẽ bị contract mới từ chối ngay PUT đầu vì vị trí đích đang có chủ đề. Chưa có API/contract đổi thứ tự topic nguyên tử được xác nhận; không tuyên bố mũi tên đã hoạt động theo contract mới, không tự dựng API hoặc dùng thứ tự tạm.
- Kiểm chứng lượt mới: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua (exit 0), build còn cảnh báo kích thước chunk sẵn có. Focused TopicForm/CoursesPage: 2 file / 21 test qua, gồm lỗi tạo/sửa theo trường, giữ dữ liệu, sửa/gửi lại và lỗi generic. Toàn bộ `npm.cmd test` exit 1: 102/104 file qua, 677/679 test qua; lỗi tại `flashcards/lib/owl-reactions.test.ts:87` và timeout 5000ms ở `UsersPage.test.tsx:69`, ngoài phạm vi thay đổi form chủ đề. Không sửa/bỏ test ngoài phạm vi.
- Chạy chẩn đoán riêng UsersPage đúng một lần: 1 file / 10 test qua. Giữ nguyên báo cáo full suite có 2 lỗi; không dùng lượt chạy riêng để tuyên bố full suite xanh hoặc kết luận lỗi do đâu.
- Gateway 8080/18080 tiếp tục timeout; chưa chạy checklist backend thật hoặc 390px. Không tuyên bố phase hoàn thành. Commit frontend local: `50d6730 fix(courses): show topic order conflicts beside the input`, chỉ 4 file thuộc khóa học/chủ đề; không push thêm, merge hoặc commit backend phase. Frontend worktree sạch sau commit; [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) không được cập nhật remote trong lượt này.

## Việc cho bên kia

- Cần API và contract đổi thứ tự topic nguyên tử cho mũi tên theo quy tắc duy nhất trong cùng parent; thao tác phải đổi toàn bộ thứ tự cần thiết trong một transaction. Luồng 2 PUT từng topic không dùng được khi vị trí đích đang có, và frontend không được tự chọn số tạm hoặc chuẩn hóa để vượt ràng buộc.
- **Đã có (backend, 2026-10-23, chưa merge):** `PUT /api/content/admin/topics/order` với `{ parentTopicId?, topicIds }` — `topicIds` là toàn bộ topic cùng parent theo thứ tự mới (parent null = nhóm gốc), một transaction, `204`; sai tập id → `400`, parent lạ → `404`. Nhóm có thứ tự trùng được đánh số lại liên tiếp nên cũng gỡ được nhóm đang bị khóa mũi tên. Contract: `lesson-authoring-v1.md` mục "Reordering topics". Frontend gửi lại cả nhóm anh em đã đổi chỗ, không tự chọn số.
- `SORT_ORDER_TAKEN` và quy tắc từ chối lưu trùng đã được backend triển khai/công bố trong `lesson-authoring-v1.md`; không yêu cầu triển khai lại lỗi này. Không còn quyết định xử lý trùng nào chờ người dùng.
