# Phase 3: web (repo frontend)

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

## Không đổi

- "Điều chỉnh điểm" và hàng đợi kiểm duyệt cộng đồng.
- Mobile.

## Kiểm tra

- Chạy lint, typecheck, test của repo.
- Đăng nhập Admin: menu người dùng không còn "Cấp gói Premium", không vào được trang từ vựng.
- Đăng nhập Content Author: trang từ vựng chạy như trước.
