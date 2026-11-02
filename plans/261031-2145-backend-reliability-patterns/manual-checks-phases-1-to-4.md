# Kiểm tay trên stack thật, phase 1–4

**Trạng thái:** chưa chạy. Chỉ bắt đầu khi đủ điều kiện dưới đây.

## Điều kiện trước

- Gói C, D, E đã được Claude duyệt; `mvn -B -ntp verify` toàn reactor xanh.
- Máy trống đủ RAM để build và chạy 9 service cùng hạ tầng (kiểm bằng `Get-CimInstance Win32_OperatingSystem`). Lần trước còn khoảng 1,8 GB, không đủ.
- Không dùng VS Code mở `backend` khi chạy stack hoặc build Maven, vì language server ghi đè `target/`.
- Không ghi mật khẩu, token, JWT hay nội dung `.env` vào báo cáo.

## Chuẩn bị

1. Dừng stack cũ nếu còn: `docker compose -p ieltspath-verify stop`. Không dùng `down -v` nếu cần giữ dữ liệu.
2. Build và chạy với nhiều instance:
   `docker compose up -d --build --scale notification-service=2 --scale game-service=2 --scale learning-service=2`
3. Chờ healthy: `docker compose ps`.

Cổng trên host (đều chỉ `127.0.0.1`): gateway `8080`, Prometheus `9090`, RabbitMQ UI `15673`, Eureka `8761`.

## Phase 1 — nhiều instance

| # | Bước | Kết quả mong đợi |
|---|---|---|
| 1.1 | Hai trình duyệt, hai tài khoản; cùng vào một phòng Wordly | Nước đi của bên này hiện ở bên kia, dù hai instance game khác nhau nhận request. |
| 1.2 | Hai trình duyệt, một phòng Rocket; để hết thời gian vòng | Vòng đóng đúng một lần; không ai bị trừ mạng hai lần. |
| 1.3 | Đối chiếu test `concurrentSchedulesCloseAnExpiredRoundOnlyOnce` đã xanh | Khớp với kết quả thật ở 1.2. |

## Phase 2 — quan sát

| # | Bước | Kết quả mong đợi |
|---|---|---|
| 2.1 | Gọi qua gateway một endpoint learning có gọi content, kèm `X-Correlation-Id: manual-001` | `docker compose logs api-gateway learning-service content-service \| grep manual-001` thấy ở cả ba service. |
| 2.2 | `curl -i http://127.0.0.1:8080/actuator/prometheus` | `404`. Actuator không đi qua gateway. |
| 2.3 | Prometheus targets: `http://127.0.0.1:9090/targets` (chạy profile `observability`) | Hai target learning ở trạng thái `UP`. |
| 2.4 | `/actuator/prometheus` của assessment, community, notification | Có `outbox_pending`, `outbox_oldest_age_seconds`, `dead_letter_messages`. |
| 2.5 | Profile `json-logs` (`SPRING_PROFILES_ACTIVE` có `json-logs`) | Log mỗi dòng là một JSON, có trường `correlationId`. |
| 2.6 | Event từ assessment vào learning listener | Log consumer ở learning có `correlationId` của request gốc. |

## Phase 3 — API

| # | Bước | Kết quả mong đợi |
|---|---|---|
| 3.1 | Đăng nhập sai liên tiếp qua gateway `POST /auth/login` đến khi bị chặn | `429` kèm header `Retry-After`. |
| 3.2 | Gọi `POST /auth/reset-password` vượt ngưỡng | `429`. |
| 3.3 | Lỗi từ các service có cùng tập field: `timestamp, status, error, message, path` | Khớp contract test. |

## Phase 4 — outbox và thông báo

| # | Bước | Kết quả mong đợi |
|---|---|---|
| 4.1 | Admin cấp gói qua `subscriptions/grant` kèm header `Idempotency-Key` | Người dùng nhận thông báo trên trình duyệt. |
| 4.2 | Gửi lại đúng request đó với cùng `Idempotency-Key` | Không cấp thêm, không có thông báo thứ hai. |
| 4.3 | `docker compose stop rabbitmq` trong một phút; cấp gói trong lúc đó; `docker compose start rabbitmq` | Thông báo đến sau khi RabbitMQ sống lại. |
| 4.4 | Sau 4.3, kiểm bảng outbox của access | Không còn hàng `PENDING` cho loại event không relay. |
| 4.5 | Replay DLQ bằng token ADMIN: `POST /api/notifications/admin/dead-letters/{queue}/replay` | Message được đưa về queue và xử lý; DLQ giảm. |

## Đo draining (phase 4 đính chính)

- `docker compose stop game-service` và đo thời gian đến khi container thoát.
- Mong đợi: không vượt `stop_grace_period: 40s`. Ghi số đo thật.

## Ghi kết quả

Mỗi dòng ghi: `#`, đạt hay không đạt, lệnh đã chạy, đoạn output rút gọn (không chứa secret), ghi chú. Dòng không đạt thì ghi nguyên nhân quan sát được, không kết luận khi chưa có bằng chứng.

## Chưa xác định

- Endpoint learning cụ thể ở bước 2.1: chọn endpoint đã biết có gọi content khi chạy.
- Cách lấy token ADMIN cho bước 4.5 và 4.1: cần người có tài khoản admin trên stack local.
