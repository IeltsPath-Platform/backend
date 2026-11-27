# Phase 3: web (repo frontend)

Gồm ba việc: bỏ cấp gói, bỏ trang từ vựng của Admin, thêm đặt lại mật khẩu.

Gốc: `src/features/admin`. Thư mục `plans/` của repo frontend bị gitignore nên việc của web ghi ở đây.

## Bỏ "Cấp gói Premium"

- `users/components/UserRowMenu.tsx`: bỏ mục `premium` và prop `onGrantPremium`.
- `users/pages/UsersPage.tsx`: bỏ nhánh `kind: 'premium'`.
- `users/components/AccessActionForms.tsx`: xoá `GrantPremiumForm`, giữ `AdjustPointsForm`.
- `users/api/accessAdminApi.ts`: bỏ `grantSubscription`; bỏ `listPlans` nếu không còn nơi dùng.
- `users/pages/UsersPage.menu.test.tsx`: bỏ ca cấp gói.

## Bỏ trang từ vựng của Admin

- `adminMenu.ts` (`rolesFor`): `/admin/vocabulary` và `/content-author/vocabulary` chỉ còn `CONTENT_AUTHOR`.
- `AdminRoutes.tsx`: bỏ `LegacyVocabulary`; đường `/admin/vocabulary` chuyển hướng như các đường nội dung cũ khác.
- Sửa test: `adminMenu.test.ts`, `AdminRoutes.test.tsx`, `AdminVocabularyPage.test.tsx`.

## Thêm "Đặt lại mật khẩu" (sau khi có API của [phase 4](phase-04-admin-password-reset.md))

- `users/components/UserRowMenu.tsx`: thêm mục "Đặt lại mật khẩu" cho dòng của người khác. Dòng của chính Admin thì mở luồng "Đổi mật khẩu" đang có.
- Ẩn hoặc khoá mục này với tài khoản chỉ đăng nhập Google nếu danh sách người dùng đã cho biết; nếu chưa thì để server trả lỗi.
- Form mở bằng drawer bên phải như các form tạo, sửa khác; một ô mật khẩu mới, lỗi đỏ dưới ô, lỗi từ server mới dùng toast; không thêm dòng chú thích.
- Dùng chung luật mật khẩu `@/features/auth/lib/passwordRules`.
- `users/api`: thêm hàm gọi `PUT /api/users/{id}/password`.
- `users/pages/UsersPage.menu.test.tsx`: cập nhật danh sách mục menu và thêm ca đặt lại mật khẩu.

## Không đổi

- "Điều chỉnh điểm" và hàng đợi kiểm duyệt cộng đồng.
- Mobile.

## Kiểm tra

- Chạy lint, typecheck, test của repo.
- Đăng nhập Admin: menu người dùng không còn "Cấp gói Premium", không vào được trang từ vựng, đặt lại được mật khẩu cho một tài khoản và đăng nhập được bằng mật khẩu mới.
- Đăng nhập Content Author: trang từ vựng chạy như trước.
