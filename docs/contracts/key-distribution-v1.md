# Key distribution API v1

Phân phối key theo đợt: quản lý tạo đợt, giao key cho sale, sale phát key cho học viên, học viên kích hoạt. Base path:
`/api/access/manager` (role `STAFF_MANAGER`, không có ADMIN) và `/api/access/sale` (role `SALES_STAFF`). Gateway chuyển
`/api/access/**` tới access-service; cả hai path đi kèm internal JWT.

## Trạng thái của key

Không lưu thành cột riêng; suy ra từ `activation_keys` (V12):

| State | Điều kiện |
| --- | --- |
| `READY` | `ACTIVE`, chưa có `issued_at`. `sale_id` rỗng là kho chung của quản lý, có giá trị là sale đang giữ |
| `ISSUED` | `ACTIVE`, đã có `issued_at` (đã phát cho học viên, chưa kích hoạt) |
| `ACTIVATED` | `REDEEMED` |
| `REVOKED` | `REVOKED` (có `revoked_at`, `note`) |
| `EXPIRED` | `EXPIRED`, hoặc `ACTIVE` quá `expires_at` |

Manager thấy thêm `GIVEN` = `READY` có sale giữ, hoặc `ISSUED`.

Key của đợt (`batch_code` khác rỗng) **chỉ kích hoạt được sau khi đã phát** (`issued_at` có). Key chưa phát bị từ chối như
mã không tồn tại (`KEY_NOT_FOUND`), không lộ rằng key có thật. Key không thuộc đợt (sinh từ `POST /api/access/admin/keys/generate`)
coi như đã phát và kích hoạt như cũ.

## Manager

| Method, path | Body / query | Trả về |
| --- | --- | --- |
| `POST /batches` | `{productId, quantity 1..1000, expiresOn?: YYYY-MM-DD, label?}` | 201 `BatchResponse` |
| `GET /batches` | `productId?`, `month?: YYYY-MM`, `page`, `size` (≤100) | `PageResponse<BatchResponse>` |
| `GET /batches/{code}` | | `BatchResponse` |
| `GET /batches/{code}/keys` | `state?`, `saleId?`, `pool?`, `held?`, `q?` (4 ký tự cuối), `page`, `size` (≤100) | `PageResponse<KeyResponse>` |
| `POST /batches/{code}/allocations` | `{allocations:[{saleId, quantity}]}` | 204 |
| `POST /batches/{code}/revoke-unused` | `{note?}` | `ActionResponse` |
| `GET /batches/{code}/export` | | `text/csv`, UTF-8 có BOM, mã đầy đủ, tối đa 1000 dòng |
| `POST /keys/reassign` | `{keyIds, toSaleId}` | `ActionResponse` |
| `POST /keys/withdraw` | `{keyIds}` | `ActionResponse` |
| `POST /keys/revoke` | `{keyIds, note?}` | `ActionResponse` |
| `POST /keys/{id}/reveal` | | `{code}` (no-store, ghi nhật ký) |
| `POST /issue` | header `Idempotency-Key`, `{productId}` | `IssuedKeyResponse` (no-store) |
| `GET /overview` | `month?` | `{byBatch: CountsResponse[], byProduct: CountsResponse[]}` |
| `GET /sales` | | `[{id, displayName, active, held, issued, activated}]` (nguồn: user-service) |

`ActionResponse` = `{changed, skipped}`. `changed` là số key đã đổi; `skipped` là số key được yêu cầu nhưng không ở trạng thái cho
phép (ví dụ cân lại key đã cấp, thu hồi key đã kích hoạt).

## Sale

| Method, path | Body / query | Trả về |
| --- | --- | --- |
| `GET /stock` | | `{byProduct: CountsResponse[], byBatch: CountsResponse[]}` chỉ key của tôi |
| `POST /issue` | header `Idempotency-Key`, `{productId}` | `IssuedKeyResponse` (no-store) |
| `GET /issued` | `productId?`, `state?`, `q?`, `page`, `size` | `PageResponse<KeyResponse>` (key tôi đã phát, mới nhất trước) |
| `GET /keys/{id}` | | `KeyResponse` (chỉ key của tôi) |
| `POST /keys/{id}/reveal` | | `{code}` chỉ key đã phát (`issued_at` có), ghi nhật ký |

## Học viên

`POST /api/access/me/keys/activate` giữ nguyên hợp đồng. Lỗi key trả `code` ổn định trong body:

| `code` | HTTP | `message` |
| --- | --- | --- |
| `KEY_NOT_FOUND` | 404 | Mã kích hoạt không hợp lệ (cũng dùng cho key chưa phát) |
| `KEY_ALREADY_REDEEMED` | 400 | Mã này đã được sử dụng. |
| `KEY_EXPIRED` | 400 | Mã kích hoạt đã hết hạn. |
| `KEY_REVOKED` | 400 | Mã kích hoạt đã bị thu hồi. |

## Mã lỗi của khu quản lý và sale

`details.code` (hoặc `code` ở gốc body) và HTTP:

| Code | HTTP | Khi nào |
| --- | --- | --- |
| `NOT_ENOUGH_KEYS` | 409 | Giao nhiều hơn số READY của đợt; `details.available` |
| `NO_KEY_AVAILABLE` | 409 | Không còn key của gói để phát (kể cả key hết hạn, không có mã lưu) |
| `KEY_NOT_ISSUED` | 409 | Xem mã một key chưa phát |
| `CODE_NOT_STORED` | 409 | Key sinh trước khi có mã mã hóa |
| `CODE_UNREADABLE` | 503 | Mã đã lưu không giải mã được (khóa đổi) |
| `KEY_CODE_CIPHER_UNAVAILABLE` | 503 | Chưa cấu hình `ACCESS_KEY_CODE_SECRET` |
| `SALE_NOT_ELIGIBLE` | 422 | Người nhận không phải sale đang hoạt động |
| `USER_SERVICE_UNAVAILABLE` | 503 | Không lấy được danh sách sale |
| `IDEMPOTENCY_KEY_REQUIRED` | 400 | Thiếu header `Idempotency-Key` |
| `IDEMPOTENCY_KEY_REUSED` | 409 | Khóa đã dùng cho yêu cầu khác (khác sale/quản lý hoặc khác gói) |
| `KEY_NOT_FOUND` | 404 | Key không tồn tại hoặc không thuộc người gọi (không lộ sự tồn tại) |
| `BATCH_NOT_FOUND` | 404 | Đợt không tồn tại |
| `INVALID_REQUEST` | 400 | Số lượng, danh sách, ghi chú ngoài giới hạn |

Mọi câu hiển thị cho người dùng là tiếng Việt có dấu.

## Idempotency

- Phát key (`/issue`) bắt buộc `Idempotency-Key` (≤100 ký tự). Cùng khóa, cùng người gọi, cùng gói trả lại đúng key đã phát;
  khác người hoặc khác gói trả `IDEMPOTENCY_KEY_REUSED`. Bảng `key_issue_requests` (V14).
- Hai lần phát cùng lúc lấy hai key khác nhau (`FOR UPDATE SKIP LOCKED`).

## Nhật ký (`key_audit_log`, V14)

Mỗi hành động tạo đợt, giao, cân lại, rút, thu hồi, phát, xem mã và xuất có một dòng: `action`, `actor_id`, `key_id`,
`batch_code`, `quantity`, `detail` (JSON đếm, không chứa mã). Không log mã key ở bất kỳ đâu.

## Ràng buộc đồng bộ

access-service gọi user-service `GET /internal/users/sales-staff` (bearer và `X-Correlation-Id` của người gọi), cấu hình
`USER_SERVICE_URL` (khóa `access.user.base-url`, mặc định `http://localhost:8085`). Lời gọi qua `ResilientCall` (target `user`,
`READ`), ngoài transaction. Danh sách không cache.

Biến môi trường mới: `ACCESS_KEY_CODE_SECRET` (Base64 của 32 byte ngẫu nhiên, chỉ ở `.env`). Thiếu thì không tạo được đợt,
kích hoạt key vẫn chạy.
