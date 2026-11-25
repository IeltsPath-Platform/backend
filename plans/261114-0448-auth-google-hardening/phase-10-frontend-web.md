---
phase: 10
title: "Frontend web cho xác thực và Google sign-in"
status: pending
priority: P1
dependencies: [1, 2, 3, 4, 5, 7, 8]
---

# Phase 10: Frontend web

Repo `frontend/` (React + Vite + zod + vitest), **nhánh riêng tạo từ `origin/main` của repo frontend** (không dùng nhánh
`feat/author-content-improvements`, nhánh đó có PR #125 đang mở). Tên gợi ý `feat/auth-hardening-web`. Ứng dụng mobile ngoài
phạm vi. Làm theo 4 đợt, mỗi đợt một PR và chỉ bắt đầu khi phần backend tương ứng đã **merge** (hoặc ít nhất contract đã
chốt trong `docs/contracts/` của backend):

| Đợt | Phụ thuộc backend | Nội dung |
| --- | --- | --- |
| 1 | Phase 4 | Mật khẩu: tối thiểu 8, hiển thị `WEAK_PASSWORD` |
| 2 | Phase 2, 3 | Màn quản lý phiên đăng nhập; kiểm tra luồng refresh khi 401 |
| 3 | Phase 5 | Google sign-in gửi nonce của server |
| 4 | Phase 7, 8 | Xác thực lại, gỡ liên kết Google, đặt mật khẩu lần đầu, lịch sử bảo mật |

## Quy ước giao diện (bắt buộc, đã chốt trước đây)

- Trường bắt buộc có dấu `*` đỏ; lỗi kiểm tra ở frontend là **chữ đỏ ngay dưới ô nhập** (zod), **không dùng toast**; toast
  chỉ cho thông báo trả về từ backend (thành công hoặc lỗi không gắn với ô nào).
- Không thêm đoạn chữ giải thích (hint/caption) dưới ô hoặc tiêu đề mới; giải thích nằm trong tài liệu/chat. Các chuỗi
  `PASSWORD_LENGTH_HINT`, `PASSWORD_LENGTH_PLACEHOLDER` đã có chỉ được **cập nhật** cho đúng số 8, không thêm chuỗi hint mới.
- Hiển thị thông báo lỗi của backend (tiếng Việt, có dấu) nguyên văn; phân nhánh bằng `details.code`, không so chuỗi.
- Mọi chữ hiển thị là tiếng Việt có dấu. Dùng token màu và lớp CSS hiện có của `profile.css` và auth; không thêm thư viện mới.
- Màn học viên/profile dùng component có sẵn (`SecuritySection` và các subcard hiện có). Quy ước drawer/modal của khu admin
  (`AdminLayer`) không áp dụng cho profile; hộp thoại xác nhận dùng cùng kiểu với hộp "Hủy liên kết" đang có.

## Đợt 1: mật khẩu (sau backend phase 4)

Đổi tối thiểu 6 → 8 và hiển thị lý do yếu.

**File (đã khảo sát):**
- `src/features/auth/lib/passwordRules.ts`: `PASSWORD_MIN_LENGTH = 8`, `PASSWORD_LENGTH_HINT = 'Từ 8 đến 72 ký tự.'`,
  `PASSWORD_LENGTH_PLACEHOLDER = 'Từ 8 đến 72 ký tự'`, `PASSWORD_LENGTH_MESSAGE = 'Mật khẩu phải có từ 8 đến 72 ký tự.'`;
  sửa các regex trong `mapAuthPasswordHttpError` đang gắn cứng "6" ("từ 6", "6 đến 72") sang 8. Thêm hàm
  `weakPasswordReasons(error)`: nếu `details.code === 'WEAK_PASSWORD'` thì tách `details.reasons` (kiểm kiểu thật mà backend
  trả: chuỗi phân cách dấu phẩy hoặc mảng) và ánh xạ sang câu tiếng Việt của bảng "Lý do của `WEAK_PASSWORD`" trong
  `plan.md`; lý do lạ thì dùng thông báo `message` của backend.
- `src/features/auth/components/AuthForm.tsx` (dùng `PASSWORD_MIN_LENGTH` quanh dòng 219, 242) và
  `src/features/auth/pages/ResetPasswordPage.tsx` (quanh dòng 209, 224): kiểm bằng hằng mới; khi submit nhận
  `WEAK_PASSWORD` thì hiện lý do **dưới ô mật khẩu** (không toast).
- `src/features/profile/components/SecuritySection.tsx`: `calculatePasswordStrength` đang coi `pass.length >= 6` là hợp lệ,
  đổi dùng `PASSWORD_MIN_LENGTH`; form đổi mật khẩu hiện lý do `WEAK_PASSWORD` dưới ô mật khẩu mới.
- `src/features/admin/users/pages/UsersPage.tsx` dòng 34 (`.min(6, 'Mật khẩu có từ 6 đến 72 ký tự.')`): đổi sang 8 và dùng
  hằng chung, thông báo "Mật khẩu có từ 8 đến 72 ký tự.".
- Test: cập nhật `AuthPage.test.tsx`, `ResetPasswordPage.test.tsx`, `ProfileTab.test.tsx` đang giả định 6 ký tự; thêm test
  `passwordRules` (mật khẩu 7 ký tự bị chặn, 8 qua; `WEAK_PASSWORD` với từng lý do ra đúng câu; lý do lạ dùng `message`).

## Đợt 2: phiên đăng nhập (sau backend phase 2, 3)

- `profileApi.ts` (hoặc file `sessionsApi.ts` cạnh nó nếu `profileApi.ts` đã quá lớn): `listSessions()` (`GET /api/users/me/sessions`),
  `revokeSession(sessionId)` (`DELETE .../{sessionId}`), `revokeAllSessions({ exceptCurrent })` (`DELETE .../sessions?exceptCurrent=true`).
  Zod schema: `sessionId`, `startedAt`, `lastUsedAt`, `ipAddress` (đã che), `userAgent`, `current`.
- Hook `useSessions` trong `features/profile/hooks/`; subcard "Thiết bị đăng nhập" trong `SecuritySection` (hoặc component mới
  `SessionsSection.tsx` được gắn vào `ProfileTab`): mỗi dòng hiện thiết bị (rút gọn `userAgent`), IP đã che, hoạt động gần
  nhất, nhãn "Thiết bị này" khi `current`; nút "Đăng xuất thiết bị" (không có trên dòng hiện tại), nút "Đăng xuất các thiết bị
  khác" (`exceptCurrent=true`) có hộp xác nhận. Thu hồi xong tải lại danh sách; lỗi backend hiện bằng toast (đúng quy ước).
- Kiểm chứng luồng 401 → refresh: `src/lib/httpClient.ts` đã có `refreshAccessToken` (single-flight `refreshInFlight`). Thêm
  test vitest (`httpClient.refresh.test.ts`): request trả 401 `SESSION_REVOKED`/hết hạn → gọi `/auth/refresh` đúng một lần dù
  nhiều request đồng thời → lặp lại request cũ; nếu refresh cũng 401 → xóa phiên và chuyển về đăng nhập. **Không** đổi hành
  vi nếu test cho thấy đã đúng; nếu thiếu thì sửa tối thiểu. Đây là điều kiện để chủ dự án hạ `ACCESS_TOKEN_MAX_AGE_SECONDS`
  xuống 900 (xác nhận trên trình duyệt thật là việc của chủ dự án).
- Test: `SessionsSection.test.tsx` (hiển thị, nhãn thiết bị hiện tại, thu hồi gọi API đúng, xác nhận trước khi thu hồi hàng loạt).

## Đợt 3: Google nonce (sau backend phase 5)

- `authApi.ts`: `googleNonce()` → `POST /auth/google/nonce` trả `{ nonce, expiresInSeconds }` (zod).
- `src/features/auth/lib/googleIdentity.ts`: thêm `nonce?: string` vào kiểu tùy chọn `initialize`.
- `GoogleSignInButton.tsx`: trước `initialize`, lấy nonce mới từ server và truyền `nonce` cho GIS; nonce chỉ dùng một lần
  và hết hạn 300 giây nên phải lấy lại khi: nonce hết hạn, sau mỗi lần credential được dùng (thành công hoặc 401), và khi
  component mount lại. Lỗi lấy nonce (mạng/503): vẫn cho đăng nhập như hiện nay (server mới bắt buộc nonce khi
  `require-nonce=true`; lúc đó hiện thông báo của backend) và không chặn trang.
- Không log nonce hay credential ra console.
- Test: `GoogleSignInButton.test.tsx` và `googleIdentity.test.ts` cập nhật: `initialize` nhận `nonce`; lấy lại nonce sau khi dùng.

## Đợt 4: xác thực lại, đặt mật khẩu lần đầu, lịch sử bảo mật (sau backend phase 7, 8)

- `profileApi.ts`: `reauth({ currentPassword } | { googleIdToken })` → `POST /api/users/me/reauth` trả `{ reauthToken,
  expiresInSeconds, method }`; `unlinkOAuthIdentity(provider, reauthToken?)` gửi header `X-Reauth-Token` qua tùy chọn
  `headers` của `apiRequest` (đã hỗ trợ); `setInitialPassword(newPassword, reauthToken)` → `POST /api/users/me/password/initial`
  với header `X-Reauth-Token`; `listSecurityEvents()` → `GET /api/users/me/security-events`.
- **Gỡ liên kết Google** (`SecuritySection.tsx`, quanh `handleConfirmUnlink`, dòng 149–160): trong hộp xác nhận hiện
  thêm bước xác thực lại. Nếu `hasPassword`: ô "Mật khẩu hiện tại" (bắt buộc, dấu `*` đỏ) rồi gọi `reauth`; nếu tài khoản
  chỉ có Google: dùng `GoogleSignInButton`/GIS (kèm nonce từ đợt 3) để lấy ID token rồi gọi `reauth` với `googleIdToken`.
  Có `reauthToken` thì gọi `unlinkOAuthIdentity(provider, token)`. Mã `REAUTH_REQUIRED` từ backend → mở lại bước xác thực lại;
  `REAUTH_ACCOUNT_MISMATCH` và lỗi sai mật khẩu hiện bằng thông báo của backend (mật khẩu sai hiện dưới ô).
  Frontend luôn gửi token nếu có, nên chạy tốt cả khi cờ `reauth-required-for-unlink` còn `false`.
- **Đặt mật khẩu lần đầu** cho tài khoản chỉ có Google (`hasPassword === false`): form "Tạo mật khẩu" trong
  `SecuritySection` (thay cho thông báo "chưa có mật khẩu" hiện tại): xác thực lại bằng Google (method `GOOGLE`) → ô mật
  khẩu mới + xác nhận (zod: min 8, khớp nhau, lỗi dưới ô; `WEAK_PASSWORD` dưới ô) → `setInitialPassword`. Thành công
  (backend đã thu hồi mọi phiên) → đăng xuất cục bộ và chuyển về trang đăng nhập kèm thông báo của backend, giống đổi mật
  khẩu hiện nay (xem `useProfile.ts` quanh dòng 257–280 để theo cùng cách xử lý sau đổi mật khẩu).
  `PASSWORD_ALREADY_SET` (409) → tải lại hồ sơ để form tự chuyển sang "Đổi mật khẩu".
- **Lịch sử bảo mật**: subcard "Hoạt động bảo mật" liệt kê `security-events` (50 mục: thời gian, loại, IP đã che, thiết bị).
  Nhãn loại sự kiện tiếng Việt: `PASSWORD_CHANGED` "Đổi mật khẩu", `PASSWORD_RESET` "Đặt lại mật khẩu", `PASSWORD_SET` "Tạo
  mật khẩu", `GOOGLE_LINKED` "Liên kết Google", `GOOGLE_UNLINKED` "Hủy liên kết Google", `GOOGLE_PASSWORD_CLEARED` "Xóa mật
  khẩu khi liên kết Google", `SESSION_REVOKED` "Đăng xuất một thiết bị", `SESSIONS_REVOKED_ALL` "Đăng xuất tất cả thiết bị",
  `REFRESH_REUSE_DETECTED` "Phát hiện phiên đăng nhập bị dùng lại", `REAUTH_SUCCEEDED` "Xác thực lại thành công",
  `REAUTH_FAILED` "Xác thực lại thất bại", `ACCOUNT_STATUS_CHANGED` "Thay đổi trạng thái tài khoản", `ROLE_CHANGED`
  "Thay đổi vai trò"; loại lạ hiện nguyên mã. Chỉ đọc, không có nút thao tác.
- Test: `profileApi.test.ts` cho các hàm mới (đúng đường dẫn, header `X-Reauth-Token`, zod); component test cho luồng gỡ
  liên kết cần xác thực lại (đúng/sai mật khẩu, `REAUTH_REQUIRED`), form tạo mật khẩu (yếu, không khớp, thành công), danh
  sách sự kiện (nhãn tiếng Việt).

## Kiểm chứng (mỗi đợt)

`npm run lint`, `npx tsc -b`, `npx vitest run` cho các file chạm tới rồi toàn bộ suite. Lỗi có sẵn trên `main` đã biết
(`AdminRoutes` 2 test, `PackageAuthoring.integration` 1 test) không thuộc phase này: báo riêng, không sửa lẫn, không làm
yếu test. Kiểm trình duyệt thật (đăng nhập Google, thu hồi phiên, gỡ liên kết) là việc của chủ dự án.

## Success Criteria
- [ ] Mọi chỗ trên web nhập mật khẩu mới yêu cầu tối thiểu 8 và hiện lý do `WEAK_PASSWORD` dưới ô, không toast.
- [ ] Người dùng xem và thu hồi được thiết bị đăng nhập; test chứng minh 401 → refresh một lần và lặp lại request.
- [ ] Google sign-in gửi nonce của server và lấy lại nonce khi cần.
- [ ] Gỡ liên kết Google đi qua xác thực lại; tài khoản chỉ có Google tạo được mật khẩu rồi mới gỡ được liên kết.
- [ ] Người dùng thấy lịch sử bảo mật của chính mình với nhãn tiếng Việt.
- [ ] `lint`, `tsc -b` sạch; vitest xanh ngoài các lỗi có sẵn đã nêu.
