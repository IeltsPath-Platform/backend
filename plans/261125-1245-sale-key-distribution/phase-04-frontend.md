# Phase 04 - Giao diện: nối API thật, vào đúng màn theo role

Đọc trước `frontend/AGENTS.md` và `frontend/.sdd/constraints/admin-ui.md`: không chữ giải thích dưới ô hay dưới tiêu đề, lỗi nhập đỏ
dưới ô (zod), toast chỉ cho thông báo từ backend, tạo hoặc sửa mở ngăn bên phải, xem chi tiết mở hộp thoại giữa.
Phase này chỉ bắt đầu khi phase 02 và 03 đã có API chạy được.

## Hiện trạng

- `features/point-management/data/{store.ts,mock-data.json,materialize.ts}` giữ trạng thái giả trong bộ nhớ; mọi trang đọc từ `usePointState`.
- Route `/point-management/*` mở cho mọi người, menu không có role. `signInDestination` không biết hai role.
- Demo có các dòng chữ giải thích (`pm-note`, `pm-rules`, mô tả dưới tiêu đề hộp thoại) trái quy ước "không chữ giải thích".

## Việc làm

1. **Lớp API** `point-management/api/pointApi.ts`: một client (theo mẫu `accessApi.ts`) cho toàn bộ endpoint phase 02 và 03,
   parse bằng zod, `AbortSignal`, lỗi đọc `details.code`. Hook tải dữ liệu theo mẫu `useAdminResource`. Bỏ `mock-data.json`,
   `materialize.ts` và các hàm đổi trạng thái trong `store.ts`; giữ phần thuần tính toán (`selectors`, `format`) nếu còn dùng.
2. **Phân quyền route**: bọc `/point-management/sale-manager/*` bằng `RequireAuth` và `RequireRole(STAFF_MANAGER)` (ADMIN không vào, theo D1),
   `/point-management/sale/*` bằng `RequireRole(SALES_STAFF)`; sai role chuyển về trang mặc định của role đó. Menu có `roles`.
3. **Điểm vào sau đăng nhập**: `signInDestination` thêm `STAFF_MANAGER` -> `/point-management/sale-manager/overview`,
   `SALES_STAFF` -> `/point-management/sale/issue`. Nhiều role thì theo thứ tự ADMIN, CONTENT_AUTHOR, EXAMINER, STAFF_MANAGER, SALES_STAFF.
4. **Màn Quản lý**: tổng quan, danh sách đợt, tạo đợt, chi tiết đợt (lọc trạng thái, chọn nhiều, phân bổ, cân lại, rút, thu hồi,
   xuất CSV tải từ server), và mục "Phát key" cho Quản lý phát trực tiếp từ kho chung (cùng cách làm với màn phát của sale, D7). Tạo đợt mở ngăn bên phải (hiện là trang riêng); đóng lỗi ngoại lệ trong ngăn.
5. **Màn Sale**: phát key (một lần bấm, sinh `Idempotency-Key` mỗi lần bấm, giữ khóa đó nếu mất phản hồi rồi thử lại), key đã phát,
   chi tiết key (che mặc định, "Hiện mã" gọi `reveal`). Nút gói tắt khi hết key; `409 NO_KEY_AVAILABLE` báo trong hộp thoại.
6. **Nhãn role**: `ProfileHeader` thêm `STAFF_MANAGER` ("Quản lý bán hàng"); đồng bộ nhãn ở trang người dùng.
7. **Nhập key của học viên**: `home/accessApi.ts` đọc `details.code`; hộp thoại không đổi hành vi.
8. Xóa các dòng chữ giải thích thừa; thông tin thật cần biết thì nằm trong hộp thoại xác nhận (câu hỏi và hậu quả), không để dưới tiêu đề.

## Trạng thái và lỗi cần xử lý

- Đang tải, rỗng, lỗi có nút thử lại cho từng bảng; danh sách phân trang ở server.
- Mất phản hồi khi phát key, giao key hoặc thu hồi: tải lại dữ liệu rồi mới cho thao tác tiếp, không tự gửi lại mù (riêng phát key dùng lại `Idempotency-Key`).
- `NOT_ENOUGH_KEYS` hiện số còn lại ngay trong ngăn phân bổ; `SALE_NOT_ELIGIBLE` bỏ dòng sale đó; `USER_SERVICE_UNAVAILABLE` cho thử lại.
- Kết quả `skipped` của cân lại, rút, thu hồi hiện trong thông báo kết quả của hộp thoại ("Đã thu hồi 40, bỏ qua 8 key đã kích hoạt").
- Đổi trang hoặc rời màn khi đang gọi: bỏ kết quả cũ (AbortController).

## Test (vitest)

- `PointManagementRoutes` theo role: học viên, sale vào màn quản lý bị chuyển hướng; quản lý vào được màn quản lý; ADMIN không vào được; sale vào màn sale.
- `signInDestination` cho từng role và tổ hợp role.
- Từng thao tác gọi đúng endpoint và đúng thân; kết quả `skipped`; `NOT_ENOUGH_KEYS`; `NO_KEY_AVAILABLE`.
- Phát key: bấm đôi chỉ một yêu cầu; thử lại sau mất phản hồi dùng lại cùng `Idempotency-Key`.
- Che và hiện mã: mặc định che, bấm hiện mới gọi `reveal`; không để mã vào URL hay log.
- Test cũ của bản demo chuyển sang dựng dữ liệu bằng phản hồi API giả (mock `fetch`), bỏ `resetPointState`.
- Chạy `npx vitest run src/features/point-management src/features/home src/features/auth`, `eslint`, `npx tsc -b`.
