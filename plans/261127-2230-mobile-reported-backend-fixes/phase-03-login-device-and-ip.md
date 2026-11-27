# Phase 3: lưu thiết bị và IP khi đăng nhập (P1)

## Nguyên nhân

`AuthTokenIssuer.issue(user)` (đăng nhập mật khẩu và Google) truyền `LoginContext.UNKNOWN`.

## File sửa

- `services/user-service/.../application/usecase/AuthTokenIssuer.java`: lấy ngữ cảnh từ port
  `AuthAuditContextProvider` (đã có), `null` thì dùng `UNKNOWN`.
- `services/user-service/.../infrastructure/adapter/ServletAuthAuditContextProvider.java`: ưu tiên header
  `X-Gateway-Client-IP`, rồi mới tới chặng cuối của `X-Forwarded-For`, giống `LoginContextResolver`.

Bản patch của bên mobile (`mobile/docs/backend-fixes-2026-11-26.patch`) chỉ sửa file đầu. Nếu áp nguyên, sau proxy sẽ ghi
IP của proxy vì provider bỏ qua `X-Gateway-Client-IP`. Sửa provider cũng làm đúng luôn IP trong sự kiện audit.

## Test

- Đăng nhập kèm `User-Agent` và `X-Gateway-Client-IP`: refresh token lưu đúng cả hai.
- Provider: có header gateway thì dùng nó; không có thì dùng chặng cuối `X-Forwarded-For`; ngoài request trả `UNKNOWN`.
- Làm mới token vẫn giữ hành vi cũ.

Phiên tạo trước khi sửa vẫn rỗng.
