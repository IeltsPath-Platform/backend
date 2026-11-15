---
phase: 6
title: "Đủ điều kiện đăng nhập Google theo sub (G6)"
status: pending
priority: P2
dependencies: []
---

# Phase 6: Kiểm tra miền email chỉ khi tạo mới hoặc liên kết theo email

## Vấn đề
`LinkGoogleAccountUseCase.execute` gọi `google.isEmailAuthoritative()` ngay dòng đầu, trước khi tra theo `sub`. Người đã
liên kết Google từ trước nhưng email Google hiện tại chuyển sang miền không còn được coi là đáng tin (ví dụ Workspace bị
đổi miền hoặc mất claim `hd`) sẽ bị 403 `GOOGLE_EMAIL_NOT_SUPPORTED` dù `sub` đã được liên kết và là định danh ổn định.

## Thiết kế
Thứ tự mới trong `LinkGoogleAccountUseCase.execute`:
1. Khóa tạo theo `google-subject:{sub}` như hiện nay.
2. Tra `(GOOGLE, sub)`. **Nếu có** → đăng nhập tài khoản đã liên kết: bỏ qua kiểm tra miền, không dùng email trong token để
   tìm tài khoản, không cập nhật email đã lưu; vẫn áp `requireGoogleEligible` (chỉ CUSTOMER, phải ACTIVE).
3. **Nếu chưa có** (tạo mới hoặc liên kết vào tài khoản theo email) → bắt buộc `isEmailAuthoritative()`, giữ nguyên toàn bộ
   logic hiện có (xác minh email, xóa mật khẩu của tài khoản chưa xác minh, thu hồi refresh token, sự kiện).

Không đổi `GoogleIdentity`, verifier hay contract lỗi; vẫn yêu cầu claim `email` không rỗng ở verifier.

## Files
- `application/usecase/LinkGoogleAccountUseCase.java` (đổi thứ tự, tách `email` chỉ tính khi cần).
- `docs/contracts/google-sign-in-v1.md` (làm rõ: kiểm tra miền chỉ áp khi tạo mới/liên kết theo email).

## Tests (`LinkGoogleAccountUseCaseTest`)
- `sub` đã liên kết + email miền không hỗ trợ → đăng nhập được, không đổi email đã lưu.
- `sub` chưa liên kết + email miền không hỗ trợ → 403 `GOOGLE_EMAIL_NOT_SUPPORTED`, không tạo user, không liên kết.
- `sub` đã liên kết nhưng user không ACTIVE → `GoogleAccountNotActiveException`.
- `sub` đã liên kết nhưng user có role khác CUSTOMER → `GoogleSignInNotAllowedException`.
- Các case cũ (liên kết theo email, xóa mật khẩu tài khoản chưa xác minh, xung đột `sub`) không đổi; `GoogleSignInConcurrencyIT`
  vẫn xanh.

## Success Criteria
- [ ] Tài khoản đã liên kết đăng nhập được dù miền email Google đổi; người mới với miền không hỗ trợ vẫn bị 403.
- [ ] Không có thay đổi hành vi nào khác ở các nhánh tạo mới/liên kết.
- [ ] `mvn -q -pl services/user-service -am test` xanh.
