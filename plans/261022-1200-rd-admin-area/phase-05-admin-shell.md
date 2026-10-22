# Phase 05: Shell quản trị (frontend)

Repo: `frontend`, nhánh `feat/content-author`. Đọc `frontend/AGENTS.md` trước. Không phụ thuộc phase backend nào.

## Mục tiêu

Khu quản trị tách khỏi web học nhưng cùng một frontend, giao diện theo ảnh Zamora người dùng đã gửi: thanh trên có logo và chữ "QUẢN TRỊ", ô tìm kiếm, chuông, menu tài khoản; sidebar icon bên trái, mục đang chọn nổi bật; tông tối.

## Hiện trạng frontend (đã khảo sát)

- Router phẳng trong `src/app/App.tsx`; `RequireAuth` (`features/auth/AuthGuards.tsx`) chỉ kiểm `isLoggedIn`, không có guard theo role.
- Role nằm trong `useAuthSession()` (`features/auth/authSession.ts`): `{isLoggedIn, hydrated, roles: string[], userName, email, userId, points, tier}`. Chỗ duy nhất kiểm role hiện nay là `roles?.includes('ADMIN')` nội tuyến ở `ContentReportsAdminPage.tsx` và `CommunityPage.tsx`.
- `components/SiteNavbar.tsx` có hai mảng điều hướng cố định (`guestNavigation`, `memberNavigation`), không lọc theo role.
- HTTP: `apiRequest` ở `src/lib/httpClient.ts` (`auth: true`, ném `HttpError`). Cấu trúc feature: `api/`, `components/`, `pages/`, `lib/`; hook `useApiResource` (`features/learning-path/lib/useApiResource.ts`), `PageState` (`ApiErrorState`, `LoadingState`).
- CSS: Tailwind 4 cho shadcn; màn hình dùng CSS thường theo từng feature với token `--classroom-*`. Không có token màu cho khu quản trị.
- Alias: `@/`, `~components`, `~features`, `~types`. TypeScript strict, `verbatimModuleSyntax` (dùng `import type`). Test: vitest + Testing Library (jsdom) trong `src/**/*.test.{ts,tsx}`.

## Việc cần làm

1. Guard `RequireRole` trong `features/auth/AuthGuards.tsx`: nhận danh sách role, dùng `useAuthSession().roles`; thiếu quyền thì hiện trang 403 có nút về trang học (không im lặng điều hướng).
2. Tạo `src/features/admin/`:
   - `AdminLayout.tsx` (không dùng `SiteNavbar`), `AdminSidebar.tsx`, `AdminTopbar.tsx`, `admin.css` với token `--adm-*` (không sửa `learning-path.css`).
   - `adminMenu.ts`: mỗi mục có `path`, `label`, `icon` (lucide), `roles`. Lượt đầu: Tổng quan, Cây tri thức, Bài học, Ngân hàng câu hỏi cho `ADMIN` và `CONTENT_AUTHOR`; `EXAMINER` chỉ thấy Cây tri thức (API cho phép đọc).
   - Menu tài khoản: tên, email, "Về trang học", đăng xuất.
   - Context `useAdminSearch` cho ô tìm kiếm trên thanh trên; các trang danh sách đọc để lọc. Không có tìm kiếm toàn hệ thống.
3. Route nhóm `/admin/*` trong `App.tsx`: bọc `RequireAuth` và `RequireRole`; các mục chưa làm trỏ tới trang placeholder rõ ràng.
4. `SiteNavbar.tsx`: thêm mục "Quản trị" chỉ hiện với `ADMIN`, `CONTENT_AUTHOR`, `EXAMINER`.
5. Mobile-first: sidebar thu thành thanh dưới hoặc menu trượt ở màn nhỏ; focus bàn phím, tương phản WCAG AA.

## Test

Guard (CUSTOMER bị chặn, CONTENT_AUTHOR vào được, ẩn danh bị đưa về đăng nhập), menu lọc theo role, shell render, mục "Quản trị" trên navbar ẩn/hiện đúng role. Mock `@/features/auth/authSession` như `ContentReportsAdminPage.test.tsx`.

## Tiêu chí hoàn thành

Đăng nhập CONTENT_AUTHOR vào `/admin` thấy shell và 4 mục; CUSTOMER vào `/admin` thấy trang 403; web học không đổi giao diện. `typecheck`, `lint`, `test` xanh.
