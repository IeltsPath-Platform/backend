---
phase: 5
title: "Rollout and mobile handoff"
status: pending
effort: ""
---

# Phase 5: Rollout and mobile handoff

## Overview

Dependencies: phase 4 đạt; Firebase project và Android build sẵn sàng cho manual E2E. Bàn giao contract, vận hành staged rollout và rollback không phá notification hiện có.

## Related Code Files / deliverables

- `docs/contracts/notification-push-v1.md`: API/status/limits/idempotency/transfer, payload và semantics delivery/expiry.
- `docs/contracts/community-events-v1.md`: liên kết mở rộng push, không thay nghĩa preference/event envelope.
- `docs/local-development.md` hoặc runbook mới `docs/notification-push-operations.md`: flags, secret injection, migration, metrics, pause/replay, rotation và cleanup.
- `docs/system-architecture.md`: local delivery outbox và scheduler multi-instance; chỉ cập nhật claims đã triển khai.
- `infra/config-server/config-repo/notification-service.yaml`: cấu hình scoped, placeholder secret, defaults off.
- Gateway đã route `/api/notifications/**`; sửa/test chỉ nếu có bằng chứng route/security/rate limit chưa đáp ứng.

## Điều kiện và quyết định trước production

| Hạng mục | Cần xác nhận |
| --- | --- |
| Firebase | Project môi trường, Android package ID, FCM HTTP v1, service identity/quyền tối thiểu, egress và credentials ADC |
| Mobile | development/preview build đúng Firebase app; native FCM token, không Expo token; không test remote push bằng Expo Go |
| Token protection | Quyền DB tối thiểu, phương thức mã hóa at rest; nếu field encryption được chọn phải có khóa/rotation và không plaintext fallback |
| Lifecycle | Logout online unregister trước xóa session; logout offline có thể còn mapping tới lần refresh/cleanup; mobile không lộ nội dung khi session không còn |
| Delete account | Tìm integration/hook xóa tài khoản thực tế; disable devices và cancel pending deliveries của user. Nếu chưa có event/hook, ghi vận hành thu hồi và coi là gate cần hoàn thiện trước production, không tự phát minh event mới trong common-events |
| Retention | Đề xuất purge terminal delivery sau 30 ngày, inactive device sau 90 ngày; xác nhận với chính sách dữ liệu hiện có, purge có batch và chạy lặp an toàn |
| Tuning | Chốt retry/TTL/device limits (nếu có), rate/concurrency; tính tổng tải theo số instance, không chỉ mỗi process |

Những điều kiện này không chặn viết code với fake provider; không tuyên bố production-ready khi chúng còn bỏ trống.

## Implementation Steps / rollout

1. Release schema/API/worker code với enqueue=false, worker=false. Flyway upgrade additive, inbox/WS regression và readiness đạt.
2. Inject project/credentials qua secret manager/ADC phù hợp môi trường. Không copy service-account JSON vào source/image/docs hoặc command history. Test missing/invalid credentials bằng fake trước.
3. Mobile tích hợp PUT sau login+permission, refresh token và DELETE trước logout; các API cần Bearer token. Android tạo channel và dùng cùng channel ID với sender khi áp dụng.
4. Staging bật enqueue=true, worker=false; phát event có kiểm soát, kiểm tra notification/job commit đúng, không gửi FCM. Không dùng backlog production để thử.
5. Staging bật worker=true, gửi bằng tài khoản/thiết bị test. Xác nhận metrics/claim/state/ownership, mọi job tới đúng recipient/device/version.
6. Production: bật theo rollout vận hành có giới hạn tốc độ; không thêm cohort framework chỉ cho phase đầu. Theo dõi queue age, accepted/failed/retry/parked và lỗi inbox/WS. Không backfill inbox lịch sử.
7. Tạo hướng dẫn pause/drain/replay sau outage; replay chỉ job còn hạn/binding đúng và giữ dedupe inbox. Không expose endpoint gửi arbitrary message.
8. Ghi rõ validation thực chạy và cấu hình cuối vào reports; mặc định không deploy hoặc gửi push thật chỉ vì plan đã được viết.

## Manual Android E2E

- Login/cấp quyền, PUT mapping đúng; 2 thiết bị một user và 2 user trên một thiết bị.
- Event community và các grading/subscription đang hỗ trợ: foreground, background và process bị hệ điều hành đóng; đúng nội dung generic, đúng thiết bị.
- Force-stop từ Android Settings có hạn chế nền tảng, không coi là bảo đảm nhận push; tách khỏi process killed thông thường trong biên bản test.
- Tap mở đúng post/comment/grading/inbox; refetch REST có auth để kiểm tra owner/nội dung và unread. Không đánh dấu read chỉ vì provider đã accepted.
- Foreground không hiện đồng thời banner WebSocket và native; mobile quyết định cơ chế hiển thị/dedupe theo notificationId.
- Tắt permission Android: app/inbox vẫn dùng được; không khẳng định server biết ngay khi OS permission đổi.
- Token refresh, logout online, đổi account, reinstall; user cũ không tạo send mới đến mapping đã thu hồi. Push đang in-flight/accepted có thể vẫn đến, mobile không mở nội dung trái quyền.
- Sai credentials, FCM outage, app offline và TTL hết: inbox không mất, không flood backlog cũ khi khôi phục.

## Rollback

Tắt worker để dừng gọi mới, tắt enqueue để ngừng sinh job. Jobs đã provider accepted không thể thu hồi; job còn lại giữ/expire theo policy. Trả release cũ khi cần nhưng giữ migration additive; không sửa/drop bảng notification hoặc migration đã chạy. Khi bật lại, chỉ gửi jobs còn hạn với binding còn khớp.

Nếu thay đổi cấu hình/secret cần restart, làm theo runbook deployment thực tế; không giả định Config refresh cập nhật an toàn Firebase singleton.

## Success Criteria

- [ ] Contract mobile, flags, rollout/rollback, credentials/rotation và retention được ghi rõ.
- [ ] Manual staging Android có bằng chứng, không tuyên bố chỉ từ automated fake tests.
- [ ] Inbox/WS không regression, token invalid được vô hiệu hóa đúng, lỗi FCM không mất notification.
- [ ] Các điều kiện production/lifecycle chưa xong được ghi là outstanding, không đóng phase giả.

## Risk Assessment

Logout offline, request in-flight và provider duplicate không có bảo đảm tuyệt đối phía BE. Payload generic + refetch REST được xác thực giảm rủi ro; nếu sản phẩm yêu cầu thu hồi ngay cả khi offline cần thiết kế session/device binding riêng và scope mới, không hứa bằng DELETE best-effort.
