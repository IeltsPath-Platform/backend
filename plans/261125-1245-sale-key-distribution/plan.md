# Khu Sale thật: đợt key, phân bổ cho sale, phát key cho học viên

- Status: chưa code; D1–D7 đã chốt (2026-11-25)
- Cập nhật: 2026-11-25
- Repo: `backend/` (access-service, user-service, config-repo) và `frontend/` (`features/point-management`)

## Hiện trạng (đọc code 2026-11-25)

- Màn Sale và Sale manager (`/point-management/*`) là bản demo: dữ liệu mẫu `mock-data.json`, thao tác chỉ đổi biến trong
  trình duyệt (`data/store.ts`), tải lại là mất. Route không có `RequireAuth`, menu `roles: []`.
- Role `SALES_STAFF` và `STAFF_MANAGER` có trong token và trang quản lý người dùng nhưng không có endpoint nào. Đăng nhập xong
  `signInDestination` đẩy cả hai về `/overview` của học viên.
- access-service đã có bảng (V12: `key_batches`, `key_allocations`, các cột `batch_code`, `seq`, `sale_id`, `issued_at`, `note`,
  `revoked_at`, `code_ciphertext`) nhưng chưa có code dùng. Chỉ có ADMIN `POST /keys/generate` (không gắn đợt, không lưu mã
  mã hóa) và `POST /keys/{id}/revoke`.
- Học viên nhập key qua `POST /api/access/me/keys/activate` (đã chạy, có test). Key sinh từ đợt dùng cùng `code_hash` nên
  `ActivateKeyUseCase` không phải đổi.
- Lỗi của access-service còn tiếng Anh ("Activation key has already been redeemed"), frontend đang so khớp nguyên câu để dịch.
- `ProfileHeader` không có nhãn cho `STAFF_MANAGER`.

## Mục tiêu

- Quản lý (STAFF_MANAGER) tạo đợt key, giao key cho sale, cân lại, rút về kho, thu hồi, phát trực tiếp, xuất CSV, xem tổng quan.
- Sale (SALES_STAFF) chỉ thấy key được giao, bấm một lần để lấy key kế tiếp của một gói, xem lại key đã phát.
- Học viên nhập key phát từ đợt và nhận quyền lợi, đúng như key sinh từ `/keys/generate` hôm nay.
- Màn hình dùng API thật; bỏ dữ liệu mẫu.

## Quyết định

Nguồn: chủ dự án, 2026-11-25.

| # | Quyết định | Hệ quả |
| --- | --- | --- |
| D1 | Chỉ STAFF_MANAGER dùng API Quản lý; ADMIN **không** | `@PreAuthorize("hasRole('STAFF_MANAGER')")` ở `/manager/**`. ADMIN muốn thao tác phải được gán thêm role Quản lý |
| D2 | Key thuộc đợt **chỉ kích hoạt được sau khi đã phát** (`issued_at` có); key chưa phát bị từ chối | `ActivationKey.redeem()` kiểm `issuedAt`; học viên nhập key chưa phát nhận câu "mã không hợp lệ" như key không tồn tại, không lộ sự tồn tại. Đổi hành vi kích hoạt chỉ với key có `batch_code` |
| D3 | Lưu mã đầy đủ dạng mã hóa AES-256-GCM, khóa `ACCESS_KEY_CODE_SECRET` trong `.env`; xem hoặc xuất đều ghi nhật ký | Phase 01 |
| D4 | Key không thuộc đợt (sinh từ `POST /admin/keys/generate`, `batch_code` rỗng) coi như đã phát; API đó giữ nguyên | Key đang lưu hành vẫn kích hoạt được; không cần migration dữ liệu |
| D5 | Sale ngừng hoạt động đang giữ key: key ở lại, Quản lý thấy "ngừng hoạt động" và tự cân lại; không tự rút | Phase 02 (S9) |
| D6 | Xuất CSV ở server, có nhật ký, tối đa 1000 dòng | Phase 02 |
| D7 | Quản lý được phát key trực tiếp từ kho chung cho học viên (`issued_at` có, `sale_id` rỗng) | Thêm `POST /manager/issue` ở phase 02; cần thiết vì D2 chặn key chưa phát |

## Phase

| # | Phase | Phụ thuộc |
| --- | --- | --- |
| 01 | [Nền: nhật ký, mã hóa mã key, danh sách sale, phân quyền, luật kích hoạt](phase-01-foundation.md) | không |
| 02 | [API Quản lý: đợt, phân bổ, thu hồi, tổng quan, xuất](phase-02-manager-api.md) | 01 |
| 03 | [API Sale: kho của tôi, phát key, key đã phát](phase-03-sale-api.md) | 01, 02 |
| 04 | [Giao diện: nối API thật, vào đúng màn theo role](phase-04-frontend.md) | 02, 03 |
| 05 | [Tài liệu, test suốt luồng, triển khai](phase-05-docs-and-verification.md) | 04 |

## Tiêu chí hoàn thành

- Mọi dòng S1..S23 (trong phase 01, 02 và 03) có test; tên test ghi hành vi, không ghi mã S, số phase hay mã plan.
- Một test tích hợp chạy suốt trên PostgreSQL thật: Quản lý tạo đợt, giao cho sale, sale phát key, học viên kích hoạt, ví hoặc gói
  đổi đúng; thu hồi key đã kích hoạt bị bỏ qua.
- Schema mới dùng migration mới, không sửa V1..V12 của access-service.
- Lỗi trả về client bằng tiếng Việt có dấu, có `details.code` ổn định; access-service nối `VietnameseErrorMessages`.
- Contract mới `docs/contracts/key-distribution-v1.md`, cập nhật `docs/system-architecture.md` (bảng HTTP đồng bộ: access gọi user-service),
  README của access-service.
- `mvn -q -pl services/access-service -am test`, user-service, test Gateway (`ServiceDependencyGraphTest`, `AuthRouteConsistencyTest`),
  `ArchitectureTest`; frontend `vitest`, `eslint`, `tsc -b` đạt.
- Không còn `mock-data.json` hay `data/store.ts` giả lập trong luồng chạy thật.
