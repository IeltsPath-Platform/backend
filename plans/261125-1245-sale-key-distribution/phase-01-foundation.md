# Phase 01 - Nền: nhật ký, mã hóa mã key, danh sách sale, phân quyền

Theo quyết định D1, D2, D3, D4 (xem `plan.md`). Phủ: S20, S21. Đọc trước `backend/AGENTS.md` §3.3, §3.5, §3.9 và `docs/system-architecture.md` (bảng HTTP đồng bộ).

## Hiện trạng

- `activation_keys.code_ciphertext` có (V12) nhưng `GenerateActivationKeysUseCase` không ghi; `batch_code`, `seq` cũng không.
- Không có bảng nhật ký. Màn demo hứa ghi nhật ký cho: xem mã đầy đủ, cân lại, thu hồi, xuất CSV.
- access-service chưa gọi service nào. user-service chỉ cho ADMIN liệt kê người dùng (`/api/users`).
- access-service `GlobalExceptionHandler` không nối `VietnameseErrorMessages`; `KeyAlreadyRedeemedException`, `KeyExpiredException`,
  `KeyRevokedException` còn tiếng Anh.

## Việc làm

1. **Migration V14 (access-service)**: bảng `key_audit_log` (`id`, `action` `REVEAL|EXPORT|ALLOCATE|REASSIGN|WITHDRAW|REVOKE|ISSUE`,
   `actor_id`, `key_id` nullable, `batch_code` nullable, `quantity`, `detail JSONB` không chứa mã, `created_at`); bảng
   `key_issue_requests` (`idempotency_key` khóa chính, `sale_id`, `product_id`, `key_id`, `created_at`) cho việc phát key; chỉ mục
   theo `(key_id, created_at)` và `(actor_id, created_at)`. Migration mới, không sửa V12.
2. **Mã hóa mã key**: port `KeyCodeCipher` ở `application/port`, triển khai AES-256-GCM ở infrastructure; khóa từ
   `ACCESS_KEY_CODE_SECRET` (Base64, 32 byte, chỉ ở `.env`, không giá trị dự phòng trong config-repo, thêm tên vào `.env.example`).
   Thiếu khóa thì tạo đợt trả `503 KEY_CODE_CIPHER_UNAVAILABLE`, service vẫn khởi động và kích hoạt key vẫn chạy (so băm).
   Mỗi bản ghi mã hóa kèm nonce ngẫu nhiên; không log mã, không log bản mã.
3. **Danh sách sale**: user-service thêm `POST /internal/users/by-role` (nhận `role`, trả `id`, `displayName`, `active`) theo mẫu
   `InternalUserController.display-names`. access-service thêm port `SaleDirectory` và client HTTP qua `ResilientCall`
   (`target = user-service`, `READ`), URL `USER_SERVICE_URL` trong `config-repo/access-service.yaml`. Cạnh access -> user là
   một chiều (user-service không gọi access); ghi vào bảng "Đồng bộ (HTTP)" và chạy `ServiceDependencyGraphTest`.
4. **Phân quyền**: `@PreAuthorize("hasRole('STAFF_MANAGER')")` cho `/api/access/manager/**` (ADMIN không vào được, theo D1);
   `@PreAuthorize("hasRole('SALES_STAFF')")` cho `/api/access/sale/**`. Kiểm `internal-jwt-paths` và tier rate limit của Gateway cho
   path mới; thêm test phân quyền (học viên, sale, ADMIN vào API quản lý đều `403`; Quản lý vào API sale `403`).
5. **Luật kích hoạt theo D2 và D4**: `ActivationKey` thêm `batchCode` và `issuedAt` (đọc từ cột V12; entity JPA và mapper cập nhật).
   `redeem()` ném `KeyNotIssuedException` khi `batchCode != null` và `issuedAt == null`. `ActivateKeyUseCase` trả cho học viên cùng
   `404` và câu "Mã kích hoạt không hợp lệ" như key không tồn tại (không lộ key có thật nhưng chưa phát), `details.code = KEY_NOT_FOUND`;
   lý do thật chỉ ghi log mức id. Key có `batchCode` rỗng (sinh từ `/keys/generate`) kích hoạt như hiện nay. Hàm `ActivationKey`
   dựng bằng constructor 9 tham số trong test cũ giữ một constructor tương thích để không phá test.
6. **Lỗi tiếng Việt**: access-service `GlobalExceptionHandler.buildResponse` gọi `VietnameseErrorMessages.userFacing`; dịch tại nguồn
   các exception key kèm `details.code` (`KEY_ALREADY_REDEEMED`, `KEY_EXPIRED`, `KEY_REVOKED`, `KEY_NOT_FOUND`). Frontend
   `home/accessApi.ts` chuyển sang đọc `details.code` thay vì so câu tiếng Anh. Hai thay đổi này đi cùng một lần phát hành.

## File

- `backend/services/access-service/src/main/resources/db/migration/V14__key_distribution_audit_and_issue_requests.sql` (mới)
- `.../application/port/KeyCodeCipher.java`, `SaleDirectory.java`; `.../infrastructure/crypto/AesGcmKeyCodeCipher.java`;
  `.../infrastructure/client/RestSaleDirectory.java`; cấu hình `access.key-code.*`
- `backend/services/user-service/.../InternalUserController.java` + use case liệt kê theo role
- `backend/infra/config-server/config-repo/access-service.yaml`, `api-gateway.yaml` (nếu cần), `.env.example`
- `frontend/src/features/home/accessApi.ts`

## Trường hợp ngoại lệ

| Mã | Trường hợp | Xử lý |
| --- | --- | --- |
| S20 | Học viên nhập key thuộc đợt nhưng chưa ai phát | `404`, câu "Mã kích hoạt không hợp lệ"; key không đổi trạng thái; không lộ rằng key có thật |
| S21 | Học viên nhập key không thuộc đợt (sinh từ `/keys/generate`) | Kích hoạt như hiện nay, kể cả key đã sinh trước migration |

## Test

- S20 và S21 (Mockito cho aggregate và use case, rồi Testcontainers với bản ghi V12 thật); replay `Idempotency-Key` của lần kích hoạt cũ vẫn trả kết quả cũ.
- Cipher: mã hóa rồi giải mã đúng; nonce khác nhau mỗi lần; bản mã bị sửa thì lỗi; khóa sai thì lỗi; không có khóa thì báo không khả dụng.
- `RestSaleDirectory`: `MockRestServiceServer` cho 200, 5xx (-> `DownstreamUnavailableException`), thời gian chờ.
- user-service: chỉ lấy đúng role, `active` theo trạng thái tài khoản, người không có role không lọt vào.
- Phân quyền từng `/manager/**` và `/sale/**` với từng role.
- Thông báo lỗi key là tiếng Việt có dấu và có `details.code`; test cũ so câu tiếng Anh đổi theo.

## Rủi ro và lùi lại

- Mất `ACCESS_KEY_CODE_SECRET`: không xem lại hay xuất được mã của key đã tạo; kích hoạt không ảnh hưởng. Ghi vào tài liệu vận hành,
  lưu khóa cùng nơi lưu `.env`.
- Đổi thông báo lỗi key: frontend cũ so câu tiếng Anh sẽ rơi về câu chung. Phát hành frontend (đọc `details.code`, có dự phòng
  câu cũ) trước backend.
