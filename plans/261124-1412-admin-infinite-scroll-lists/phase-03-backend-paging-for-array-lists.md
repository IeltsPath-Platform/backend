# Phase 03: Phân trang phía server cho danh sách còn trả cả mảng

Plan: [plan.md](plan.md) · Trạng thái: done · Backend implementation and verification.

Lý do: cuộn vô hạn cần API trả từng trang. `AGENTS.md` §3.7 cũng yêu cầu danh sách phải phân trang, lọc và sắp xếp ở database (không `findAll()` rồi cắt trong bộ nhớ).

## Bước 0: kiểm kê (làm trước, không sửa code)

Khảo sát ban đầu chỉ kiểm sâu `GET /api/users` (trả cả mảng, frontend cắt 25 dòng) và `GET /api/learning/admin/work-items` (mảng, lọc `kind`/`status`). Chưa kiểm sâu: API từ vựng quản trị, danh sách lỗi (`error-types`), lịch sử khác, bất kỳ màn admin nào còn lại.

1. Liệt kê mọi hàm `list*`/`fetch*` trong `frontend/src/features/admin/**/api/*.ts` trả `z.array(...)` cho một **danh sách người dùng cuộn**, ghi: endpoint, service sở hữu, đã có `page`/`size` chưa, số bản ghi thực tế dự kiến.
2. Phân loại: (a) **lớn/không giới hạn** → phân trang ở bước dưới; (b) **nhỏ cố định hoặc dạng cây** (khóa học, chủ đề, rubric, cây tri thức, lỗi nếu dưới vài trăm) → để nguyên; (c) đã phân trang.
3. Ghi kết quả vào mục "Kết quả kiểm kê" ở cuối file này. Đã chốt: **người dùng** và **hàng việc** phân trang ở server; API khác chỉ thêm nếu thuộc nhóm (a) và báo lại chủ dự án trước khi làm (còn mở: từ vựng quản trị).

## Bước 1: thêm `page`/`size` cho các API nhóm (a)

Với mỗi API được duyệt (đã chốt: người dùng, hàng việc):

- **Người dùng:** các bộ lọc đang áp ở client trong `UsersPage.tsx` (vai trò, trạng thái, tìm kiếm) thành tham số server; đọc `UsersPage.tsx` trước để liệt kê chính xác, và giữ thứ tự mặc định đang thấy trên màn.

- Thêm `page` (từ 0, mặc định 0) và `size` (1 đến 100, mặc định 25); ngoài khoảng trả `400` tiếng Việt (`AGENTS.md` §3.3). Phản hồi `{ content, page, size, totalElements, totalPages }`, đúng khuôn `QuestionPageResponse`/`AuthorPackagePageResponse`.
- Giữ mọi bộ lọc hiện có; thứ tự ổn định giữa các trang (có tie-breaker bằng `id`); `totalElements` đếm cùng điều kiện lọc bằng cùng một đoạn `WHERE`.
- Lọc, sắp xếp, đếm chạy ở database; không `findAll()`; không N+1 (`AGENTS.md` §3.7).
- Cập nhật contract tương ứng trong `backend/docs/contracts/` cùng PR, ghi "thay đổi so với bản trước: phản hồi từng là mảng".
- Tìm mọi nơi gọi API này (frontend admin, `mobile/`, service khác) và cập nhật cùng lúc; nơi nào cần cả danh sách thì phải có đường riêng (ví dụ danh sách chọn nhỏ), không bắt chước phân trang.

## Files (ước lượng, chốt sau kiểm kê)

- `user-service`: controller + use case + repository danh sách người dùng quản trị; contract `docs/contracts/` của user (tên chốt khi kiểm kê).
- `learning-service`: controller + use case + repository `work-items`; contract `learning-staff-area-v1.md`.
- Mỗi service: test controller (`@WebMvcTest`), test use case (Mockito), test persistence (Testcontainers) kiểm trang không trùng và tổng đúng, ArchUnit.
- Không migration trừ khi `EXPLAIN` cho thấy cần chỉ mục: nếu cần, **dừng và hỏi** trước khi thêm.

## Test

- Mỗi API: hai trang liên tiếp không trùng dòng và nối liền thứ tự; `totalElements` đúng khi có lọc; `size` ngoài khoảng và `page` âm trả `400` tiếng Việt; thiếu `page`/`size` dùng mặc định.
- Chạy hẹp trước từng service: `mvn -q -pl services/<tên>-service -am test` (Testcontainers cần Docker; nếu bị bỏ qua phải nói rõ), `ArchitectureTest`, rồi chạy cả service một lần ở cuối.

## Rủi ro

- Đổi hình dạng phản hồi (mảng thành đối tượng) làm hỏng mọi bên gọi cũ: phải cập nhật cùng lúc; kiểm `mobile/` bằng `grep` trước.
- Gateway có thể đặt tier hoặc giới hạn riêng cho route: kiểm `api-gateway.yaml` khi thêm tham số mới (không đổi route công khai).
- Danh sách người dùng chứa dữ liệu cá nhân: giữ nguyên kiểm tra quyền `ADMIN` của route, không mở rộng cột trả về.

## Kết quả kiểm kê

Bước 0 hoàn tất ngày 2026-11-24. Chưa sửa mã nguồn.

| Endpoint / danh sách | Hiện trạng và caller | Phân loại / kết luận |
|---|---|---|
| `GET /api/users` (user-service) | Trả toàn bộ mảng qua `GetAllUsersUseCase` → `UserRepository.findAll()`. `UsersPage` lọc client theo vai trò, trạng thái và chuỗi tìm trong email/họ tên; không có sort client, nên phải giữ thứ tự API hiện tại. `SystemOverviewPage` cũng gọi `listUsers()` để tính tổng người dùng, số tài khoản khóa và số lượng theo từng vai trò trên toàn bộ dữ liệu. | Đã duyệt phân trang. Caller màn Người dùng chuyển sang lọc server. Tổng quan hệ thống cần số liệu trên toàn bộ tập; `{content,...}` một trang không đủ để tính chính xác. Chủ dự án đã duyệt giữ tổng quan bằng endpoint danh sách đầy đủ GET /api/users/admin/all, không đổi các trường user trả về và vẫn giữ quyền ADMIN. |
| `GET /api/learning/admin/work-items` (learning-service) | Trả mảng từ truy vấn `work_items`, đã lọc DB theo queue/status/kind và sắp `created_at, id`; chưa phân trang/đếm. `WorkQueuePage` lọc `ACTIVE` (không phải `DONE`) ở client, `DONE` gửi filter lên server, `ALL` lấy tất cả; `OverviewPage` dùng caller `fetchAuthorWorkItems()` để đếm việc mở, hiện tối đa 8 dòng và số còn lại. | Đã duyệt phân trang. Có thể giữ tổng quan bằng `totalElements` và `content` trang đầu; cần biểu diễn bộ lọc `ACTIVE` tương đương DB (`status <> DONE`) để tổng số chính xác. Không phát hiện caller cần toàn bộ từng dòng cho xử lý khác. |
| Admin từ vựng | Tab từ chủ đề dùng `GET /api/content/vocabulary` với `size=25`, response đã có `items/page`; tìm kiếm cũng yêu cầu tối đa 25. Topics/sets là danh mục cấu hình nhỏ; picker từ vựng trong bài học dùng suggestions đã phân trang. | Không cần thêm phân trang trong phạm vi này. |
| Mã lỗi (`error-types`) | Nạp danh mục mã lỗi cố định qua `GET /api/content/error-types?activeOnly=false`, lọc client theo nhóm/trạng thái/tên. | Danh mục tham chiếu nhỏ, không thuộc danh sách không giới hạn theo phạm vi đã chốt; giữ nguyên. |
| Danh sách admin còn lại | Các cây khóa học/điểm kiến thức, rubric và cấu hình là dữ liệu cấu trúc hoặc danh mục; API khác đã có trang hoặc giới hạn theo chức năng. | Không phát hiện API lớn cần thêm vào phase này. |

Đã rà caller frontend, backend và `mobile/`: mobile có gọi `/api/users/me` và `/{id}/public-profile`, nhưng không gọi collection `GET /api/users` hoặc work-items; các route cùng tiền tố không đổi.

**Kết quả Bước 0:** người dùng và work-items là các danh sách lớn đã chốt để phân trang. Chủ dự án đã duyệt giữ tổng quan bằng endpoint đầy đủ riêng GET /api/users/admin/all. Các danh mục nhỏ và dạng cây giữ nguyên; không cần thêm API khác.
