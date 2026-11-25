# Phase 03 - API Sale: kho của tôi, phát key, key đã phát

Phủ: S1..S3, S12, S18, S23 và phần sale của S9, S11, S13. Sale chỉ thấy key có `sale_id` là chính mình; không tạo, thu hồi hay gán lại.

## API (`/api/access/sale`, role SALES_STAFF)

| Phương thức, đường dẫn | Việc |
| --- | --- |
| `GET /stock` | Số key `READY` còn lại theo từng gói của sale, và số theo đợt (cho biểu đồ) |
| `POST /issue` | Header `Idempotency-Key`, body `{productId}`: lấy key kế tiếp, đặt `issued_at`, trả mã đầy đủ |
| `GET /issued?productId=&q=&page=&size=` | Key đã phát của tôi, mới nhất trước, mã **che**, kèm trạng thái (đã kích hoạt hay chưa) |
| `GET /keys/{id}` | Chi tiết một key của tôi, mã che |
| `POST /keys/{id}/reveal` | Mã đầy đủ của key **tôi đã phát** (`issued_at` có); ghi nhật ký `REVEAL` |

## Phát key

- Chọn key kế tiếp: `sale_id = tôi`, `product_id`, `READY`, chưa hết hạn, sắp theo hạn gần nhất trước (không hạn thì cuối), rồi đợt cũ nhất, rồi
  `seq`, bằng `FOR UPDATE SKIP LOCKED` (khớp chỉ mục `idx_activation_keys_next_to_issue`). Hai lần phát cùng lúc lấy hai key khác nhau.
- Idempotent theo `Idempotency-Key`: bảng `key_issue_requests`. Cùng khóa, cùng sale, cùng gói trả lại **đúng key cũ** (không lấy key mới);
  cùng khóa khác sale hoặc khác gói trả `409 IDEMPOTENCY_KEY_REUSED`.
- Ghi `issued_at`, dòng `key_issue_requests` và nhật ký `ISSUE` trong cùng transaction; mã giải mã sau khi lấy được key và trả trong phản hồi.
- Không có mã mã hóa (key cũ) hoặc cipher không khả dụng: không phát key đó; chọn key kế tiếp có mã, hoặc `409 NO_KEY_AVAILABLE`.

## Trường hợp ngoại lệ

| Mã | Trường hợp | Xử lý |
| --- | --- | --- |
| S1 | Bấm đôi nút phát, hoặc mất phản hồi rồi thử lại | Cùng `Idempotency-Key` trả lại đúng key cũ; giao diện sinh khóa mới chỉ khi người dùng bấm lại có chủ ý |
| S2 | Hai tab hoặc hai lượt cùng phát | `SKIP LOCKED`: hai key khác nhau, không key nào phát hai lần |
| S3 | Sale hết key của gói | `409 NO_KEY_AVAILABLE`; giao diện báo nhờ Quản lý, nút gói tắt khi `GET /stock` bằng 0 |
| S9 | Sale mất role hoặc bị khóa | Mọi `/sale/**` trả `403`; key giữ nguyên cho Quản lý xử lý |
| S11 | Key không có mã lưu | Không phát key đó; xem lại trả `409 CODE_NOT_STORED` |
| S12 | Xem key của sale khác, hoặc key không tồn tại | `404` như nhau, không lộ sự tồn tại |
| S13 | Key hết hạn trong kho của sale | Không phát, không tính vào kho còn dùng được; hiện "Hết hạn" ở danh sách |
| S23 | Sale xem mã của key còn "Sẵn sàng" (chưa phát) | `409 KEY_NOT_ISSUED`; mã chỉ hiện ở bước phát, để mọi key ra khỏi hệ thống đều có dòng nhật ký `ISSUE`, và theo D2 key chưa phát chưa kích hoạt được |
| S18 | Mã lộ qua log, metric, bộ nhớ đệm | Không log mã hoặc bản mã; phản hồi `Cache-Control: no-store`; nhật ký chỉ ghi id key |

## File

- `.../application/usecase/IssueKeyUseCase` (một use case cho cả sale và Quản lý: tham số người giữ là `saleId` hoặc rỗng cho kho chung theo D7), `GetSaleStockUseCase`, `ListIssuedKeysUseCase`, `GetSaleKeyUseCase`, `RevealSaleKeyCodeUseCase`
- `.../api/controller/KeySaleController.java`, DTO, `JdbcKeyIssueRepository` (truy vấn `SKIP LOCKED`)
- `docs/contracts/key-distribution-v1.md`

## Test

- Testcontainers: S1 (hai lần cùng khóa trả cùng key, chỉ một key đổi `issued_at`), S2 (nhiều luồng phát, không trùng, tổng đúng),
  S3, S9, S11, S12, S13, S18 (bắt log, không có mã).
- Thứ tự ưu tiên: hạn gần nhất trước, không hạn sau cùng, rồi đợt cũ, rồi `seq`.
- Key phát ra kích hoạt được bằng `POST /me/keys/activate` và đổi trạng thái thành đã kích hoạt trong `GET /issued`.
- Sale A không thấy, không phát, không xem được key của sale B.
