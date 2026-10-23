# Phase 03: Slide và tài liệu trong trình soạn bài học

Người làm: **Codex** (đổi từ Claude Code ngày 2026-10-23). Nhánh: `feat/content-author-screens` (frontend, worktree `../frontend-claude`, nhánh đã có commit tiền đề tách khu `/content-author`).
Trạng thái: In progress — client/UI đổi thứ tự vẫn sau cờ false; rollout còn Blocked chờ xác minh backend/contract merge main rồi bật cờ và kiểm lại; suite lượt mới còn 2 lỗi ngoài phạm vi thay đổi form chủ đề.

Repo: `frontend`. CRUD dùng API đã có; bật đổi thứ tự sau khi xác minh backend/contract merge main, rồi chạy lại focused tests/các cổng kiểm và checklist live.

## Mục tiêu

Trong `/content-author/lessons/:lessonId`, tác giả thêm, sửa, sắp xếp, xóa link slide (Google Slides embed) và link tài liệu của bài. Tóm tắt Markdown đã sửa được qua `updateLesson` (`summaryMarkdown`), không làm lại.

## API (đã có, `LessonResourceAdminController`)

- `GET /api/content/admin/lessons/{lessonId}/resources` → `{ lessonId, summaryMarkdown, slides: [{ id, title, url, sortOrder }], documents: [...] }`
- `POST …/resources` body `{ type: "SLIDE" | "DOCUMENT", title (≤255), url (≤2000), sortOrder (≥0) }` → `201` item
- `PUT …/resources/{resourceId}` body `{ title, url, sortOrder }` (không đổi được `type`; muốn đổi thì xóa rồi tạo lại) → item
- `DELETE …/resources/{resourceId}` → `204`

### API đổi thứ tự — triển khai local, chỉ bật sau khi xác minh merge

Theo thông tin người dùng cung cấp 2026-10-23:

- `PUT /api/content/admin/lessons/{lessonId}/resources/order`
- Body `{ "type": "SLIDE" | "DOCUMENT", "resourceIds": ["<uuid>"] }`; thành công `204`.
- `resourceIds` phải đúng tập tài nguyên của loại đã chọn, mỗi ID xuất hiện đúng một lần; sai trả `400`.
- Sau khi merge: bật nút lên/xuống, gửi đúng **một PUT** chứa toàn bộ danh sách theo thứ tự mới, rồi tải lại từ server. Không dùng nhiều PUT cập nhật từng tài nguyên.
- Nguồn contract đã đọc trên nhánh backend: `docs/contracts/lesson-authoring-v1.md`, mục **Reorder slides or documents**. Được triển khai local theo contract này; xác minh code/contract merge main trước khi bật cờ, sau đó kiểm lại và chạy checklist live.

Contract đổi thứ tự đã có trong `lesson-authoring-v1.md` trên nhánh backend; không yêu cầu bổ sung lại endpoint hoặc mục contract này.

## Đọc trước

`src/features/admin/lessons/api/lessonApi.ts`, `types.ts`, `pages/LessonEditorPage.tsx` (+ test), trang học viên `src/features/learning-path/components/resources/LessonSlides.tsx` (để biết URL nào hiển thị được, ví dụ link Google Slides `…/embed`).

## Sửa / tạo

- `lessons/types.ts`: thêm `lessonResourceSchema`, `lessonResourcesSchema` và type.
- `lessons/api/lessonApi.ts`: `fetchLessonResources(lessonId, signal)`, `createLessonResource`, `updateLessonResource`, `deleteLessonResource`.
- Tạo `lessons/components/LessonResourcesEditor.tsx`: hai danh sách "Slide" và "Tài liệu". Mỗi dòng: tiêu đề, URL (link mở tab mới, `rel="noreferrer"`), nút sửa (inline), lên/xuống, xóa (xác nhận). **Chỉ dẫn mới nhất 2026-10-23:** nút lên/xuống giữ disabled, chú thích "Chờ API đổi thứ tự"; chỉ bật khi backend có endpoint đổi thứ tự cả danh sách được công bố trong `lesson-authoring-v1.md`. Không gửi nhiều PUT tuần tự để đổi thứ tự. Form thêm mới dưới mỗi danh sách: tiêu đề, URL. Kiểm URL phía client bằng `new URL()` và chỉ chấp nhận `https:`; báo lỗi ngay dưới ô. Slide: gợi ý "Dán link Google Slides dạng …/embed".
- `LessonEditorPage.tsx`: chèn `LessonResourcesEditor` thành một `section` có `aria-label="Slide và tài liệu"`, đặt sau khối "Thông tin bài học". Tải độc lập bằng `useAdminResource` riêng, để lỗi tải tài nguyên không làm hỏng phần còn lại của trang.

## Test

- `LessonResourcesEditor.test.tsx` (mock API): hiện 2 danh sách theo thứ tự `sortOrder`; thêm slide gửi `type: 'SLIDE'` và `sortOrder` = cuối; URL `http://…` hoặc không hợp lệ bị chặn, không gọi API; nút đổi thứ tự disabled, đúng chú thích và không gọi `updateLessonResource`; xóa có xác nhận; lỗi API hiện `role="alert"`.
- Test `LessonEditorPage` hiện có vẫn xanh; thêm 1 test: lỗi tải tài nguyên không chặn phần thông tin bài.

## Kiểm thủ công

Mở một bài, thêm slide Google Slides embed, mở trang học viên `/learn/lessons/:lessonId` thấy slide; đổi thứ tự 2 tài liệu, tải lại giữ đúng thứ tự.

## Hoàn thành khi

`npm run typecheck`, `npm run lint`, `npm test`, `npm run build` xanh.

## Kết quả

- 2026-10-23: xác nhận tiền đề `76608bf`, `81242c5` đã merge trên frontend main qua PR #28; worktree `frontend-claude` đã rebase `origin/main`.
- Đã triển khai schema theo `LessonResourceResponse`/`LessonResourcesResponse`, tải độc lập và form thêm/sửa/xóa HTTPS. Người dùng chốt tạm khóa riêng đổi thứ tự trong lượt làm việc 2026-10-23.
- Kiểm tra: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua; 16 test editor/resource qua. `npm.cmd test`: 90 file qua, 1 file thất bại (546 qua / 2 lỗi), hai lỗi ở `flashcards/lib/owl-reactions.test.ts` về nội dung phản ứng confused/tired, ngoài phạm vi thay đổi. Không đổi test hoặc mã flashcard để che lỗi.
- Chưa chạy checklist với backend thật hoặc xác nhận trên màn hình 390px; sẽ bổ sung kết quả sau khi kiểm tra môi trường. Không tuyên bố phase hoàn thành.
- Commit riêng `9037ce5 feat(lessons): author slide and document links`. Rà soát bổ sung: giữ cả hai danh sách khi refetch để lưu một tài liệu không làm mất bản nhập chưa lưu ở slide (và ngược lại); chỉ reset form vừa lưu thành công. Đã thêm test giữ bản nhập và khóa nút thứ tự theo quyết định người dùng. Backend thật 8080 và 18080 không kết nối được khi kiểm ngoài sandbox.
- Đã push; [draft PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29), commit bổ sung `e833022`. Rà soát code đã xác nhận sửa lỗi giữ bản nhập. Lượt kiểm cuối: typecheck/lint/build qua; toàn bộ npm test 577 qua / 1 lỗi flashcard tired ngẫu nhiên ngoài phạm vi (`owl-reactions.test.ts:109`), không tuyên bố 4 cổng đều xanh ở trạng thái cuối. Đổi thứ tự và checklist thật vẫn chờ như trên; không tự merge PR.

- Cập nhật lượt làm việc mới nhất 2026-10-23: giữ toàn bộ phần thêm/sửa/xóa và giữ bản nhập khi tải lại. Đã đổi cả title của nút và chú thích hiển thị thành "Chờ API đổi thứ tự"; không có handler đổi thứ tự hoặc chuỗi PUT thay thế.
- Kiểm chứng lượt mới nhất: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua; 78 test liên quan các màn author/menu/route qua. `npm.cmd test`: 94/95 file qua, 580 test qua / 1 lỗi flashcard confused tại `owl-reactions.test.ts:87`, ngoài phạm vi; không sửa test/mã flashcard.
- Checklist chưa chạy: thêm slide thật, mở slide ở trang học viên, đổi thứ tự tài liệu rồi tải lại, kiểm 390px. Gateway 8080 và 18080 timeout khi kiểm ngoài sandbox; riêng đổi thứ tự còn chờ endpoint và contract. Không tuyên bố đã kiểm thủ công.
- Đã push `5117a61 fix(lessons): clarify pending resource ordering` vào [draft PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29). PR giữ nháp, không merge; không sửa backend code.
- Thông báo API mới 2026-10-23: đã kiểm tra trực tiếp GitHub backend main `c600b3f399dddcbef060cdea6e7438bbf9a5f42d` và contract/controller tại đúng commit đó. Chưa có mục **Reorder slides or documents** hoặc `/resources/order`; API chưa được xác nhận merge. Giữ nguyên source frontend và chú thích chờ API, chưa gọi endpoint mới.
- Lượt kiểm tra điều kiện merge này chỉ cập nhật file phase và mô tả PR #29; không có commit/frontend source mới, không chạy lại test/build hoặc checklist backend thật, không thay kết quả kiểm chứng code ở lượt trước bằng claim mới.

### Lịch sử lượt trước 2026-10-23 — implementation sau cờ, chưa rollout

- Đã thêm `reorderLessonResources` với Zod kiểm request và response rỗng `204`, cùng test mock fetch. Khi cờ bật, UI gửi đúng một PUT toàn bộ ID của loại SLIDE hoặc DOCUMENT theo thứ tự mới rồi tải lại; không dùng chuỗi PUT từng tài nguyên.
- `RESOURCE_REORDER_ENABLED = false` tại `lessons/lib/resource-reorder.ts`; ở cấu hình hiện tại nút vẫn disabled **Chờ API đổi thứ tự** và không gọi endpoint mới. CRUD và bảo toàn bản nhập tiếp tục giữ nguyên.
- Thứ tự kích hoạt: xác minh backend code/contract đã merge main → đổi cờ true → chạy lại focused tests, typecheck/lint/test/build và checklist live. Không yêu cầu chạy thành công UI đổi thứ tự khi cờ còn false. Contract trên nhánh đã mô tả thứ tự `0..n-1`, đúng tập ID, không đổi loại tài nguyên còn lại; không cần yêu cầu làm lại API.
- Kiểm chứng cuối lượt: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua (exit 0); 2 file / 12 test mới cho API lesson và UI khi bật cờ qua. Toàn bộ `npm.cmd test` exit 1: 102/103 file qua, 670/672 test qua; 2 lỗi ngẫu nhiên đã biết ở `flashcards/lib/owl-reactions.test.ts:87` (confused) và `:109` (tired), ngoài phạm vi. Không sửa/bỏ test, không chạy lại để đổi kết quả. Build còn cảnh báo kích thước chunk sẵn có.
- Không tuyên bố tất cả cổng xanh hoặc phase hoàn thành. Commit local: `8aeeab2 feat(lessons): prepare atomic resource ordering`. Không push thêm hoặc merge; [PR #29](https://github.com/IeltsPath-Platform/frontend/pull/29) vẫn nháp, không cập nhật remote trong lượt này.
- Gateway 8080/18080 timeout trong lượt này. Thêm slide thật, xem ở trang học viên, đổi thứ tự/tải lại và kiểm 390px vẫn chưa chạy.

### Cập nhật mới nhất 2026-10-23 — xác minh lại cờ, chưa bật endpoint

- Đã đọc lại `lessons/lib/resource-reorder.ts`: `RESOURCE_REORDER_ENABLED = false`. Lượt này không thay cờ và không gọi API `/resources/order` mới từ UI; phần CRUD hiện tại tiếp tục dùng API đã có.
- Endpoint và mục **Reorder slides or documents** đã có trong contract nhánh `lesson-authoring-v1.md`; không yêu cầu backend triển khai lại. Thứ tự tiếp theo: xác minh backend code/contract merge main → bật true → chạy focused tests, đủ 4 cổng typecheck/lint/test/build và checklist live.
- Kiểm chứng lượt mới: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build` qua (exit 0), build còn cảnh báo kích thước chunk sẵn có. Focused TopicForm/CoursesPage: 2 file / 21 test qua. Toàn bộ `npm.cmd test` exit 1: 102/104 file qua, 677/679 test qua; lỗi ở `flashcards/lib/owl-reactions.test.ts:87` và timeout 5000ms tại `UsersPage.test.tsx:69`, ngoài phạm vi thay đổi form chủ đề. Một lần chạy chẩn đoán riêng UsersPage qua 1 file / 10 test; không thay kết quả thất bại của full suite hoặc kết luận nguyên nhân từ lần chạy riêng này.
- Gateway 8080/18080 tiếp tục timeout; checklist backend thật và 390px chưa chạy. Không tuyên bố mọi cổng xanh hoặc rollout/phase hoàn thành. Không sửa/bỏ test ngoài phạm vi.
- Source lesson và cờ không đổi trong lượt này; commit frontend mới chỉ chứa phạm vi khóa học/chủ đề. Không push thêm, merge hoặc commit các ghi chú backend phase.

## Việc cho bên kia

- Đổi thứ tự bằng 2 PUT như plan không thực hiện được với API từng item: migration `V56__lesson_resources.sql` đặt `UNIQUE (lesson_id, resource_type, sort_order)` (`uq_lesson_resources_order`), `LessonResourceRepositoryAdapter.save` dùng `saveAndFlush`, nên PUT đầu khi hoán đổi sẽ bị từ chối. **Người dùng đã chốt 2026-10-23: Claude Code bổ sung API đổi thứ tự nguyên tử và contract; frontend tạm chặn riêng phần này, không dùng 3 PUT.** Client/UI `/resources/order` nay đã triển khai local sau cờ false; chờ xác minh backend merge để bật, rồi kiểm lại và chạy live checklist. Không yêu cầu làm lại API đã được bổ sung trên nhánh.
- Merge API/contract đổi thứ tự đã có trên nhánh; frontend giữ cờ false đến khi xác minh merge. Cung cấp môi trường để kiểm live sau khi bật cờ; không yêu cầu bổ sung lại endpoint hoặc contract đổi thứ tự.
