# Thu hồi access token

External access token bổ sung `jti` (UUID riêng cho từng token), `iat` và `sid` (ID họ refresh token). Các claim hiện có
giữ nguyên. Gateway sao chép `sid` vào internal JWT để user-service nhận diện phiên hiện tại.

Gateway kiểm tra Redis trước khi chuyển tiếp yêu cầu đã xác thực:

| Khóa | Ý nghĩa | Thời hạn |
| --- | --- | --- |
| `auth:revoked:sid:{sid}` | Thu hồi toàn bộ access token của phiên | Thời hạn access token tối đa + 60 giây |
| `auth:revoked:jti:{jti}` | Thu hồi một access token | Thời gian còn lại của token + 60 giây |
| `auth:revoked:user:{userId}` | Mốc thu hồi theo tài khoản, tính bằng epoch giây | Thời hạn access token tối đa + 60 giây |

Token bị từ chối nếu khóa phiên hoặc token tồn tại, hoặc `iat` nhỏ hơn mốc thu hồi tài khoản. Token cũ chưa có claim mới
được chấp nhận đến khi tự hết hạn. Redis được kiểm tra bằng một lần đọc nhiều khóa với thời hạn 100 ms. Mặc định lỗi Redis
được xử lý fail-open và tăng `gateway.auth.revocation.errors`; đặt `GATEWAY_AUTH_REVOCATION_FAIL_CLOSED=true` để trả 503.

Token bị thu hồi trả 401, `code=SESSION_REVOKED`, thông báo “Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.”
Khi chế độ fail-closed gặp lỗi Redis, Gateway trả 503, `code=AUTH_UNAVAILABLE`.
