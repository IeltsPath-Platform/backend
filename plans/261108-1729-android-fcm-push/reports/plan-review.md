# Tự rà soát kế hoạch FCM — 2026-11-08

## Phạm vi và cách rà soát

Đây là rà soát tài liệu bởi agent chính, không phải code review độc lập hoặc bằng chứng test implementation. Planner subagent không chạy được do model không được tài khoản hỗ trợ; không coi lần gọi đó là review thành công. Agent chính hoàn thiện plan từ khảo sát code và yêu cầu đã đọc.

## Các quyết định đã kiểm tra nhất quán

- Hai API, 204, owner-scoped DELETE, transfer, global installation mapping và active-token uniqueness thống nhất giữa phase 1–5.
- Bảng local delivery outbox là quyết định duy nhất; không vừa đề xuất Rabbit event mới vừa bỏ qua common-events/confirm rules.
- Insert notification + snapshot jobs cùng transaction, external send ngoài transaction; ACK/redelivery vẫn dựa trên persistence.
- Không thêm FCM vào Redis subscriber; bao phủ sáu nguồn notification và giữ preference riêng của community.
- Registration version tăng khi binding thay đổi; same-token PUT không tăng. Worker và invalid-token update kiểm tra binding/version/claim.
- Worker tắt giữ jobs, enqueue tắt không backfill; không NoOp thành SENT; lease/timeout/batch được ràng buộc.
- Retry có budget, expiry, parking và fencing. Không hứa exactly-once hoặc thu hồi push đã accepted.
- Credentials, token redaction, metric cardinality và account lifecycle có tiêu chí/rủi ro cụ thể.
- Gateway route đã có; không sửa shared security/producer events hay Docker nếu không có bằng chứng cần thiết.

## Những việc chưa được chứng minh bằng thực thi

Chưa chạy migration, automated implementation tests, test Android, load test, dependency compatibility build hoặc production rollout. Tất cả phase vẫn Pending. Chưa có credentials/project ID/package ID thật trong plan.

## Điểm còn cần chốt khi implementation/deploy

Pin SDK tương thích; token protection/retention; thu hồi khi xóa tài khoản; Firebase identity/Android channel ID; TTL theo event, tốc độ tổng theo số instance và semantics logout offline. Đây là điều kiện được ghi rõ, không phải mâu thuẫn nội bộ đã được bỏ qua.
