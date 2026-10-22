# Phase 04: Menu, route và trang chủ theo vai trò (frontend)

## Hiện trạng (frontend, `src/features/admin/`)

- `adminMenu.ts`: `ADMIN_MENU` gồm Tổng quan (`/admin`, ADMIN + CONTENT_AUTHOR), Cây tri thức (ADMIN, CONTENT_AUTHOR,
  EXAMINER), Bài học và Ngân hàng câu hỏi (ADMIN + CONTENT_AUTHOR). `menuFor(roles)`, `rolesFor(path)`.
- `AdminRoutes.tsx`: route index `AdminHome` chuyển hướng tới mục đầu tiên của menu nếu không phải `/admin`, ngược lại
  hiện `OverviewPage` (tổng quan nội dung). Guard theo `rolesFor(path)`.
- `features/auth/staffRoles.ts`: `STAFF_ROLES` (ADMIN, CONTENT_AUTHOR, EXAMINER), `hasAnyRole`.
- `SiteNavbar` hiện mục "Quản trị" cho STAFF_ROLES.

## Thay đổi

1. Menu mới (một cấu hình duy nhất trong `adminMenu.ts`):

| Path | Nhãn | Icon (lucide) | Vai trò |
|---|---|---|---|
| `/admin/system` | Tổng quan hệ thống | `Gauge` | ADMIN |
| `/admin/users` | Người dùng | `Users` | ADMIN |
| `/admin/services` | Dịch vụ | `Server` | ADMIN |
| `/admin/activity` | Hoạt động | `Activity` | ADMIN |
| `/admin/content` | Tổng quan nội dung | `LayoutDashboard` | CONTENT_AUTHOR |
| `/admin/knowledge-graph` | Cây tri thức | `Network` | CONTENT_AUTHOR, EXAMINER |
| `/admin/lessons` | Bài học | `BookOpen` | CONTENT_AUTHOR |
| `/admin/questions` | Ngân hàng câu hỏi | `FileQuestion` | CONTENT_AUTHOR |

2. `/admin` (index) luôn chuyển hướng tới mục đầu tiên của `menuFor(roles)`; không còn hiện trang nào ở `/admin`.
   Bỏ thuộc tính `end` không còn cần.
3. Chuyển `OverviewPage` (tổng quan nội dung) sang route `/admin/content`. Trong trang này bỏ giả định ADMIN đọc được
   `question-stats` (giờ chỉ CONTENT_AUTHOR).
4. Thêm route cho 4 trang ADMIN, mỗi trang bọc `Guard`. Trang chưa làm ở phase 05/06 thì tạm dùng một trang
   "đang làm" nhỏ, nhưng phase 05/06 phải thay hết.
5. Nhãn thanh trên: giữ "QUẢN TRỊ" cho cả hai, không cần đổi.

## Test

- `adminMenu.test.ts`: ADMIN chỉ thấy 4 mục hệ thống; CONTENT_AUTHOR chỉ thấy 4 mục nội dung; EXAMINER chỉ thấy Cây
  tri thức; một tài khoản lạ (CUSTOMER) không thấy gì.
- `AdminRoutes.test.tsx`: `/admin` của ADMIN đi tới `/admin/system`, của CONTENT_AUTHOR tới `/admin/content`, của
  EXAMINER tới `/admin/knowledge-graph`; ADMIN mở `/admin/lessons` thấy trang 403; CONTENT_AUTHOR mở `/admin/users`
  thấy trang 403.

## Tiêu chí hoàn thành

typecheck, lint, test xanh; không còn chỗ nào trong `src/features/admin` cho ADMIN quyền vào màn nội dung.
