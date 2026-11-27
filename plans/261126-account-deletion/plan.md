---
title: "Tự xóa tài khoản người dùng (Google Play Account Deletion)"
description: "Cho phép người dùng tự xóa tài khoản qua DELETE /api/users/me (xác thực mật khẩu hoặc Google ID token), thu hồi phiên/token, phát user.deleted.v1 qua outbox và dọn dữ liệu/file ở các service."
status: in-progress
priority: P1
branch: "feat/account-deletion"
tags: [user-service, api-gateway, common-events, rabbitmq, storage, gdpr, google-play]
created: "2026-11-26T12:05:00+07:00"
updated: "2026-11-26T12:05:00+07:00"
---

# Plan: Tự xóa tài khoản người dùng (Google Play)

## Trạng thái
- **Status:** `IN PROGRESS`. Code đã merge ở PR #230; review 2026-11-27 tìm thấy lỗi SQL và thứ tự xóa, đã sửa trên nhánh `fix/account-deletion-purge` (xem mục "Sửa sau review"). Còn thiếu kiểm thử E2E trên full Docker stack.
- **Nhánh:** `feat/account-deletion` (đã xác nhận).
- **URL web ngoài phạm vi backend:** Trang web yêu cầu xóa tài khoản thuộc repo frontend (ví dụ `https://<domain>/account/delete` hoặc `/settings/delete-account`), gọi `DELETE /api/users/me` qua API Gateway.

## Quyết định đã chốt (2026-11-26)
1. **Community:** Ẩn danh `"Người dùng đã xóa"`; xóa bookmarks, follows, reactions; ảnh trong bài giữ theo bài.
2. **Access:** Giữ sổ cái/giao dịch/key activation/credit; đóng subscription đang active; ví điểm về 0; xóa `user_email_verification_state`.
3. **Đăng ký lại cùng email/Google:** Cho phép như tài khoản mới (ẩn email thành `deleted+<userId>@deleted.local`, phone null, xóa `oauth_identities`).
4. **Ân hạn:** Không có ân hạn — xóa ngay.
5. **Event đến muộn (Tombstone):** Mỗi service consumer lưu `deleted_users(user_id UUID PRIMARY KEY, deleted_at TIMESTAMPTZ)` trong cùng transaction xử lý `user.deleted.v1`; các consumer khác của service đó kiểm tra tombstone và ack-bỏ-qua event của user đã xóa.
6. **Admin:** Giữ nguyên hành vi `DELETE /api/users/{id}` (deactivate) hiện có.

## Các phase triển khai (sau khi được duyệt)

Thứ tự an toàn: **common-events & contract → consumers (tạo queue trước) → user-service & Gateway (producer sau) → docs & E2E**.

| Phase | Nội dung | Phụ thuộc | Status |
| --- | --- | --- | --- |
| 1 | [Contract `user.deleted.v1` & `shared/common-events`](./phase-01-contracts-and-common-events.md) | Duyệt câu hỏi | Done |
| 2 | [Consumers: `learning-service` & `assessment-service` (kèm xóa lô & `speaking-recordings`)](./phase-03-consumers-core-and-learning-assessment.md) | Phase 1 | Done |
| 3 | [Consumers: `library`, `access`, `notification`, `game`, `community`, `content`](./phase-04-consumers-library-access-notification-game-community-content.md) | Phase 1 | Done |
| 4 | [Producer `user-service` (`DELETE /api/users/me`, soft delete, outbox) & `api-gateway`](./phase-02-user-service-and-gateway.md) | Phase 1, 2, 3 | Done |
| 5 | [Cập nhật `docs/system-architecture.md` & kiểm chứng E2E](./phase-05-docs-and-e2e-verification.md) | Phase 2, 3, 4 | Docs done; E2E chưa chạy |

*(Lưu ý: đánh số file phase giữ `phase-01`..`phase-05` theo thứ tự logic; thứ tự deploy/merge bắt buộc đưa consumer lên trước producer.)*

## Tiêu chí nghiệm thu tổng thể

1. `DELETE /api/users/me` xác thực bằng `currentPassword` (tài khoản có mật khẩu) hoặc `googleIdToken` (tài khoản chỉ có Google), trả lỗi tiếng Việt có dấu + `code` ổn định; idempotent (gọi lại không lỗi 500).
2. Gateway route `DELETE /api/users/me` dùng tier `auth`, cập nhật `AuthRouteConsistencyTest`.
3. `user-service` chuyển `status = DELETED`, ghi `deleted_at`, thu hồi mọi refresh token, chặn login/refresh/reset/Google sign-in, ghi `auth:revoked:user:{userId}` vào Redis để Gateway chặn access token đang sống ngay lập tức, ẩn danh PII/xóa avatar, và ghi `user.deleted.v1` vào `outbox_events` (`correlation_id`, `trace_context`) cùng transaction.
4. Mỗi service consumer sở hữu queue, retry queue, DLQ riêng; idempotent theo `eventId`; ACK sau commit; không gọi HTTP; bảng lớn xử lý theo lô qua job claim `FOR UPDATE SKIP LOCKED`; xóa file Supabase Storage qua `common-storage` ngoài transaction/khóa.
5. `mvn -q -pl shared/common-events test`, `mvn -q -pl infra/api-gateway test`, và `mvn -q -pl services/<svc> -am test` (cùng `ArchitectureTest`) xanh cho mọi module sửa (trừ 9 test integration có sẵn trên `main` liên quan `restAccessClient.premium()` ở `learning-service`).

## Câu hỏi cần người dùng quyết định

1. **Chính sách dữ liệu `community-service`:** Xóa hẳn bài viết/bình luận/ảnh cộng đồng của user, hay ẩn danh tác giả thành `"Người dùng đã xóa"` (giữ lại bài viết/bình luận để không làm gãy luồng thảo luận của người khác)? *(Khuyến nghị: ẩn danh tác giả, xóa bookmarks/follows/reactions/reports liên quan danh tính cá nhân; xem chi tiết ở Phase 4.)*
2. **Dữ liệu giữ lại ở `access-service` (thanh toán/điểm/key):** Giữ bản ghi lịch sử giao dịch (`subscriptions`, `key_activations`, `point_ledger_entries`, `human_grading_credit_consumptions`, `point_credit_requests`) để phục vụ đối soát kế toán/thuế (chỉ vô hiệu hóa `point_wallets` về 0 hoặc xóa ví, không xóa sổ cái), hay xóa toàn bộ? *(Khuyến nghị: giữ sổ cái/kích hoạt key theo luật kế toán tối thiểu, đóng/xóa số dư ví hiện tại và xóa `user_email_verification_state`.)*
3. **Đăng ký lại cùng email / tài khoản Google:** Cho phép người dùng xóa tài khoản xong được đăng ký lại bằng chính email hoặc tài khoản Google đó (ẩn danh `email` thành `deleted+<userId>@deleted.local`, xóa `phone_number` và `oauth_identities`), hay chặn vĩnh viễn không cho đăng ký lại? *(Khuyến nghị: cho phép đăng ký lại như tài khoản mới hoàn toàn.)*
4. **Thời gian ân hạn (grace period) trước khi xóa thật:** Xóa ngay lập tức khi gọi `DELETE /api/users/me` (user nhận xác nhận và bị đăng xuất ngay), hay có thời gian ân hạn (ví dụ 14–30 ngày cho phép khôi phục trước khi phát `user.deleted.v1`)? *(Khuyến nghị: xóa ngay lập tức để đáp ứng đơn giản, minh bạch yêu cầu Google Play mà không cần thêm luồng khôi phục tài khoản.)*

## Sửa sau review (2026-11-27)

Nguồn: review PR #230. Nhánh ix/account-deletion-purge.

- [x] learning: xóa ghi âm theo khóa (submission_id, question_version_id); xóa theo thứ tự khóa ngoại; thêm 9 bảng bị sót.
- [x] assessment: sửa tên cột; đánh dấu ecording_deleted_at thay vì xóa tham chiếu; xóa xaminer_qa_reviews, i_gold_essays và i_eval_items của bài học viên; giữ bản chấm của giám khảo bị xóa, trả bài đang chấm về hàng đợi.
- [x] game: gỡ 5 cột gợi ý Wordly trước khi xóa lượt đoán; phòng do người dùng làm chủ giữ chủ phòng ẩn danh.
- [x] user: tài khoản DELETED không đổi lại hay sửa được; admin DELETE bỏ qua tài khoản đã xóa; staff không tự xóa; xóa ảnh qua hàng chờ có thử lại; xóa dữ liệu xaminer_profiles.
- [x] community: ẩn danh người báo cáo thay vì xóa báo cáo.
- [x] notification: không ghi lại email, tùy chọn, digest cho người đã xóa khi sự kiện đến muộn.
- [x] access: số dư về 0 qua một dòng sổ điểm.
- [x] Mọi service: job crash ở lần thử cuối thành FAILED khi hết lease.
- [x] Test PostgreSQL thật cho learning, assessment, game (gieo dữ liệu mọi bảng, kiểm dữ liệu người khác, chặn sót bảng), access, user.
- [ ] E2E trên full Docker stack.
- [ ] Gom UserDeletedListener lặp ở 8 service vào thư viện chung.
- [ ] Chính sách quyền riêng tư liệt kê dữ liệu giữ lại.