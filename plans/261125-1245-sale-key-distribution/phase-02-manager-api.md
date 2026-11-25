# Phase 02 - API Quản lý: đợt, phân bổ, thu hồi, tổng quan, xuất

Phủ: S4..S11, S13..S17, S19, S22. Trạng thái key của màn hình suy ra từ cột sẵn có (V12):
`READY` = `ACTIVE` và `issued_at` rỗng; `ISSUED` = `ACTIVE` và `issued_at` có; `ACTIVATED` = `REDEEMED`; `REVOKED`;
`EXPIRED` = `EXPIRED` hoặc `ACTIVE` quá `expires_at`. Quản lý thấy thêm nhóm "Chưa phân bổ" (`READY`, `sale_id` rỗng).

## API (`/api/access/manager`, chỉ role STAFF_MANAGER theo D1)

| Phương thức, đường dẫn | Việc |
| --- | --- |
| `POST /batches` | Tạo đợt: `productId`, `quantity` 1..1000, `expiresOn` (ngày, hoặc rỗng), `label` tùy chọn |
| `GET /batches?productId=&month=&page=&size=` | Danh sách đợt, kèm đếm theo trạng thái (một truy vấn gộp) |
| `GET /batches/{code}` | Chi tiết đợt và số key theo trạng thái, số chưa phân bổ |
| `GET /batches/{code}/keys?status=&saleId=&q=&page=&size=` | Danh sách key của đợt, mã **che** (`IP-****-****-ABCD`), phân trang ở database |
| `POST /batches/{code}/allocations` | `{allocations:[{saleId, quantity}]}`: giao từ kho chung, lấy `seq` thấp trước |
| `POST /keys/reassign` | `{keyIds, toSaleId}`: chuyển key `READY` giữa sale |
| `POST /keys/withdraw` | `{keyIds}`: rút key `READY` của sale về kho chung |
| `POST /keys/revoke` | `{keyIds, note}` và `POST /batches/{code}/revoke-unused {note}` |
| `GET /overview?month=` | Số key theo đợt, theo gói, theo sale và theo trạng thái |
| `GET /sales` | Danh sách sale từ user-service, kèm số key đang giữ |
| `POST /issue` | Header `Idempotency-Key`, body `{productId}`: phát trực tiếp key kế tiếp từ **kho chung** (`issued_at` có, `sale_id` rỗng); cùng luật chọn key và idempotent như phát của sale (D7) |
| `POST /keys/{id}/reveal` | Trả mã đầy đủ; ghi nhật ký (dùng POST vì có tác dụng phụ, không cache) |
| `GET /batches/{code}/export` | CSV UTF-8 có BOM, tối đa 1000 dòng, ghi nhật ký `EXPORT` |

## Quy tắc nghiệp vụ

- Tạo đợt trong một transaction: sinh mã đợt `B-YYMM-NNN` (đánh số theo tháng, khóa bằng `pg_advisory_xact_lock` theo `YYMM`), sinh
  `quantity` key (`IP-XXXX-XXXX-XXXX`, ký tự không nhầm lẫn như `GenerateActivationKeysUseCase`), lưu `code_hash`, `code_hint`,
  `code_ciphertext`, `batch_code`, `seq` từ 1. `expires_at` mỗi key là cuối ngày `expiresOn` theo `Asia/Ho_Chi_Minh`. Tất cả hoặc không key nào.
  Trùng băm (cực hiếm): sinh lại mã đó, không bỏ cả đợt.
- Giao key: kiểm `saleId` thuộc danh sách sale đang hoạt động **trước** transaction (không gọi HTTP trong transaction), rồi khóa dòng đợt,
  chọn `READY` và `sale_id` rỗng theo `seq` bằng `FOR UPDATE`, cập nhật `sale_id`, ghi `key_allocations` (`ALLOCATE`) và nhật ký trong cùng transaction.
- Cân lại và rút: chỉ key `READY`; key đã `ISSUED`, `REDEEMED`, `REVOKED` bị bỏ qua và đếm. Mỗi lần ghi `key_allocations` (`REASSIGN`, `WITHDRAW`).
- Thu hồi: `UPDATE ... WHERE status = 'ACTIVE'` có điều kiện, nên key vừa được học viên kích hoạt không bị thu hồi. Key đã `REDEEMED` không đổi.
  Trả `revoked` và `skipped`. Ghi `note`, `revoked_at`, nhật ký.
- Hành động trên danh sách `keyIds`: tối đa 1000 id mỗi lần; id không thuộc một đợt hoặc không tồn tại bị bỏ qua và đếm, không báo lỗi cả lượt.

## Trường hợp ngoại lệ

| Mã | Trường hợp | Xử lý |
| --- | --- | --- |
| S4 | Giao nhiều hơn số còn ở kho chung, hoặc hai Quản lý giao cùng lúc | Khóa dòng đợt; `409 NOT_ENOUGH_KEYS` kèm `available`; không giao gì |
| S5 | Cân lại hoặc rút key đã phát cho học viên | Bỏ qua key không `READY`; trả `moved` và `skipped` |
| S6 | Thu hồi key đã kích hoạt | Bỏ qua, đếm trong `skipped`; không đổi dữ liệu |
| S7 | Học viên kích hoạt cùng lúc Quản lý thu hồi | Cập nhật có điều kiện, một bên thắng; kiểm `ActivateKeyUseCase` đang khóa dòng key, nếu chưa thì thêm `findByCodeHashForUpdate` |
| S8 | Giao cho người không phải sale hoặc đã khóa | `422 SALE_NOT_ELIGIBLE`; không giao gì |
| S9 | Sale mất role hoặc bị khóa khi đang giữ key | Key ở lại; `GET /sales` đánh dấu `active=false`; Quản lý cân lại hoặc rút theo D5 |
| S10 | user-service lỗi hoặc quá thời gian | `503 USER_SERVICE_UNAVAILABLE`; không đổi gì |
| S11 | Key cũ không có `code_ciphertext` (sinh từ `/keys/generate`), hoặc cipher không khả dụng | Xem mã `409 CODE_NOT_STORED`; xuất CSV ghi ô mã trống cho dòng đó; kích hoạt vẫn chạy |
| S13 | Key quá hạn | Trạng thái `EXPIRED` khi đọc; không giao và không phát; nhập vào bị từ chối `KEY_EXPIRED` |
| S14 | `quantity` ngoài 1..1000, `expiresOn` trong quá khứ, `productId` không có, hai đợt cùng tháng tạo cùng lúc | `400` hoặc `404`; khóa advisory theo tháng nên mã đợt không trùng |
| S15 | Xuất CSV một đợt đủ 1000 key | Xuất một lượt, không phân trang; ghi `EXPORT`; chỉ Quản lý hoặc ADMIN |
| S16 | Học viên chưa xác minh email nhập key | Giữ nguyên `403` hiện tại |
| S17 | Phát hoặc giao khi đợt đã thu hồi hết | `409 NOT_ENOUGH_KEYS` |
| S22 | Quản lý phát trực tiếp khi kho chung hết key của gói, hoặc bấm đôi | `409 NO_KEY_AVAILABLE`; cùng `Idempotency-Key` trả lại đúng key cũ; chỉ lấy key `sale_id` rỗng, không lấy key đã giao cho sale |
| S19 | Danh sách lớn | Phân trang và lọc ở database; trang tối đa 100; không `findAll`; tổng quan bằng truy vấn gộp, không N+1 |

## File

- `.../domain/aggregate/KeyBatch.java`, `domain/repository/KeyBatchRepository.java`, `KeyAuditRepository.java`; value object `KeyState`
- `.../application/usecase/CreateKeyBatchUseCase`, `AllocateKeysUseCase`, `ReassignKeysUseCase`, `WithdrawKeysUseCase`,
  `RevokeKeysUseCase`, `GetKeyOverviewUseCase`, `ListBatchKeysUseCase`, `RevealKeyCodeUseCase`, `ExportBatchUseCase`
- `.../api/controller/KeyManagerController.java` và DTO; mở rộng `ActivationKeyJpaEntity` bằng các cột V12
- `docs/contracts/key-distribution-v1.md` (mới)

## Test

- Testcontainers cho từng dòng S4..S11, S13..S17, S19: song song bằng nhiều luồng cho S4 và S7; tổng số key đã giao không vượt số trong đợt.
- Tạo đợt: đúng số lượng, `seq` liên tục từ 1, mã không trùng, giải mã bản mã ra đúng mã khớp `code_hash`; lỗi giữa chừng thì không còn key nào.
- Mã đợt tăng đúng theo tháng khi hai yêu cầu cùng lúc.
- Danh sách và tổng quan: số truy vấn không tăng theo số đợt hoặc số sale.
- Nhật ký: mỗi `REVEAL`, `EXPORT`, `REASSIGN`, `WITHDRAW`, `REVOKE`, `ALLOCATE` có một dòng, không chứa mã.
