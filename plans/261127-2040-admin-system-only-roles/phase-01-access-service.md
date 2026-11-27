# Phase 1: access-service

Gốc: `services/access-service/src/main/java/com/ieltspath/access`

## Bối cảnh

- `api/controller/AdminAccessController.java` (`/api/access/admin`, ADMIN): `keys/generate`, `keys/{id}/revoke`, `plans`, `key-products`, `points/adjust`, `subscriptions/grant`.
- `api/controller/KeyManagerController.java` (`/api/access/manager`, STAFF_MANAGER): đã có tạo lô, chia mã, thu hồi.
- `application/usecase/GrantSubscriptionUseCase.java` là nơi duy nhất ghi outbox `SubscriptionGranted`.
- `application/usecase/ActivateKeyUseCase.java`: nhánh Premium chỉ ghi `SubscriptionChanged` (không được relay).

## Việc làm

### 1. Phát sự kiện khi kích hoạt mã Premium (làm trước)

- Trong nhánh Premium của `ActivateKeyUseCase`, ghi thêm outbox loại `SubscriptionGranted` với `SubscriptionGrantedV1` (eventId, userId, subscriptionId, planCode, endsAt), cùng transaction.
- Cần `PlanRepository` để lấy `planCode`.
- Giữ độ chính xác micro giây của `endsAt` như `databasePrecision` hiện có.
- Nhánh replay theo idempotency key đã trả về sớm nên không phát lại.
- Mã nạp điểm không phát sự kiện này.
- Hợp đồng sự kiện và notification-service không đổi.

### 2. Bỏ cấp gói trực tiếp, sinh mã và thu hồi mã của Admin

- `AdminAccessController`: chỉ còn `points/adjust`.
- Xoá `GrantSubscriptionUseCase`, command, request và phần persistence của `AdminGrantRequest` (bảng giữ nguyên).
- Xoá `GenerateActivationKeysUseCase`, `RevokeActivationKeyUseCase` và command, DTO đi kèm. Chỉ xoá DTO khi đã kiểm tra không còn nơi dùng.
- Giữ `SubscriptionSourceType.ADMIN` để đọc được dữ liệu cũ.

### 3. Chuyển tạo gói và loại mã sang Sales Manager

- Đưa `POST plans` và `POST key-products` sang `/api/access/manager/plans` và `/api/access/manager/key-products` (STAFF_MANAGER). Bỏ đường cũ.
- Use case và validation giữ nguyên.
- Gateway đã chuyển tiếp `/api/access/**`, không cần thêm route.

### 4. `InternalAccessController`

- Giữ `entitlement` (chỉ đọc) và `points/refund` (điểm vẫn thuộc Admin).
- Bỏ `POST /api/access/users/{userId}/consume-human-grading` (chưa service nào gọi). Giữ `ConsumeHumanGradingCreditUseCase` vì luồng chấm bài còn dùng qua `InternalHumanGradingController`.
- Sửa `InternalAccessControllerTest`.

## Test

- Thêm: kích hoạt mã Premium ghi đúng một outbox `SubscriptionGranted`; replay không ghi thêm; mã điểm không ghi.
- Thêm: Sales Manager tạo gói, loại mã (201); ADMIN bị 403.
- **Chuyển, không xoá:** `ConcurrentSubscriptionGrantIntegrationTest` (một subscription Active mỗi người) và `AccessOutboxRelayIntegrationTest` đang dựng dữ liệu bằng cấp gói trực tiếp, đổi sang kích hoạt mã.
- Xoá: `GrantSubscriptionUseCaseTest`, hai `GrantSubscriptionIdempotencyTest`, `GenerateActivationKeysUseCaseTest`, `RevokeActivationKeyUseCaseTest`.
- Sửa: `AdminAccessControllerTest`.

## Rủi ro

- Mất thông báo Premium nếu bước 2 merge mà thiếu bước 1.
- Script hoặc người dùng đang gọi thẳng các API cũ sẽ nhận lỗi. Trong repo chỉ thấy test và tài liệu gọi chúng.
