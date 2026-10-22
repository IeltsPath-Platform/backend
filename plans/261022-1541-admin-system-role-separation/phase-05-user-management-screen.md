# Phase 05: Màn quản lý người dùng (frontend)

Phụ thuộc phase 02 (luật vai trò và contract `user-management-v1.md`).

## Vị trí

`src/features/admin/users/` với `api/userAdminApi.ts`, `types.ts` (zod), `lib/`, `pages/UsersPage.tsx`,
`pages/UserDetailPage.tsx` (tùy chọn, có thể làm dạng panel), `users.css`. Route `/admin/users` (và `/admin/users/:userId`
nếu làm trang chi tiết).

## Chức năng

1. **Danh sách** (`GET /api/users`): bảng email, họ tên, vai trò (badge), trạng thái (badge), ngày tạo.
   - Lọc phía client theo vai trò, trạng thái và ô tìm kiếm của thanh trên (`useAdminSearch`, khớp email/họ tên).
   - Phân trang phía client 25 dòng/trang (API chưa phân trang).
   - Đủ trạng thái loading, lỗi (có Thử lại), rỗng.
2. **Tạo tài khoản nhân sự** (`POST /api/users`): form email, họ tên, số điện thoại (tùy chọn), mật khẩu tạm (6-72 ký tự),
   chọn vai trò (checkbox). Giao diện chặn trước tổ hợp xung đột (ADMIN với CONTENT_AUTHOR/EXAMINER) và vẫn hiển thị lỗi
   `ROLE_CONFLICT` từ server.
3. **Đổi vai trò** (`PUT /api/users/{id}/roles`): checkbox như trên, lưu một lần.
4. **Đổi trạng thái** (`PUT /api/users/{id}/status`): ACTIVE / INACTIVE / LOCKED; hỏi xác nhận trước khi khóa.
5. **Sửa thông tin** (`PUT /api/users/{id}`): họ tên, số điện thoại.
6. **Xóa** (`DELETE /api/users/{id}`): hỏi xác nhận có nhắc rằng không hoàn tác được.
7. Tài khoản đang đăng nhập: ẩn hoặc vô hiệu nút tự khóa, tự xóa, tự gỡ ADMIN (vẫn xử lý `SELF_*` từ server).
   Lấy id người đang đăng nhập từ `useAuthSession` (kiểm tra trường có sẵn; nếu chưa có thì so theo email).

## Lỗi hiển thị (tiếng Việt, theo `details.code`)

| Mã | Câu hiển thị |
|---|---|
| `ROLE_CONFLICT` | Tài khoản quản trị hệ thống không được kiêm vai trò R&D hoặc Mentor. |
| `SELF_ADMIN_REMOVAL` | Bạn không thể tự gỡ quyền quản trị của chính mình. |
| `SELF_ACCOUNT_CHANGE` | Bạn không thể tự khóa hoặc xóa tài khoản đang dùng. |
| `LAST_ADMIN` | Đây là tài khoản quản trị cuối cùng đang hoạt động, không thể gỡ, khóa hay xóa. |
| trùng email (theo mã hiện có của user-service) | Email đã được dùng. |

Nhãn vai trò: ADMIN "Quản trị hệ thống", CONTENT_AUTHOR "R&D (nội dung)", EXAMINER "Mentor", CUSTOMER "Học viên",
SALES_STAFF "Kinh doanh". Nhãn trạng thái: ACTIVE "Hoạt động", INACTIVE "Ngừng", LOCKED "Bị khóa",
PENDING_VERIFY "Chờ xác minh".

## Mẫu cần theo

Bảng, bộ lọc, form, badge: xem `questions/pages/QuestionBankPage.tsx`. Ghi rồi tải lại bằng `reload()`, không dựa
vào body trả về. Trang dùng `AdminPage` với nút "Tài khoản mới" ở thanh tiêu đề.

## Test

Mock module API: danh sách và lọc, tạo tài khoản gửi đúng body, chặn tổ hợp xung đột ở form, hiển thị từng mã lỗi,
nút tự khóa của chính mình bị vô hiệu, xác nhận trước khi xóa.

## Tiêu chí hoàn thành

typecheck, lint, test xanh. Thử tay: ADMIN tạo tài khoản CONTENT_AUTHOR, đăng nhập tài khoản đó vào `/admin` thấy các
mục nội dung.
