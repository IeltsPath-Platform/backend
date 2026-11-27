# Phase 5: tài liệu code và test case

Làm sau khi phase 1 đến 4 đã merge.

## Tài liệu trong repo backend

- `services/access-service/README.md`: bảng quyền, đường `/admin/keys/generate`, các đường chỉ ADMIN.
- `services/learning-service/README.md`: bỏ dòng `/api/users/{id}/learning-goals` (ADMIN).
- `docs/contracts/subscription-granted-v1.md`: sự kiện phát khi kích hoạt mã Premium, không còn `POST /api/access/admin/subscriptions/grant` và header `Idempotency-Key`.
- `docs/contracts/key-distribution-v1.md`: bỏ nhắc `keys/generate` (mã cũ sinh theo cách này vẫn kích hoạt được).
- `docs/local-development.md`: cách tạo Premium cho tài khoản demo là kích hoạt mã.
- README của `user-service` nếu có liệt kê API: thêm `PUT /api/users/{id}/password`.

## Test case của Report 5 (repo report)

Vòng 1 đã chạy trên chức năng cũ. Sau khi code đổi thì sửa test case và chạy lại ở vòng 2.

| File | Test case | Đổi gì |
|---|---|---|
| 5.2 | ACC-01 | Sales Manager tạo gói và loại mã, thay cho ADMIN |
| 5.2 | ACC-02 | Sinh mã theo lô của Sales Manager, thay cho `keys/generate` |
| 5.2 | ACC-17 | Bỏ (cấp gói trực tiếp) |
| 5.2 | NTF-18 | Kích hoạt mã Premium tạo thông báo và e-mail |
| 5.2 | GW-09 | Bỏ đường mục tiêu học của Admin |
| 5.3 | ADM-08 | Đang không đạt; chạy lại sau khi có chức năng đặt lại mật khẩu |
| 5.1 | sheet `UT-PointsKeys` | Bỏ ca của `GenerateActivationKeysUseCase`, thêm ca phát sự kiện khi kích hoạt |

Rà thêm 5.2 (LIB-05, CONT-23, CONT-24) và 5.3 (VOC-19, CRS-12) xem có bước nào dùng tài khoản Admin để ghi từ vựng.

Sau khi chạy lại, cập nhật số liệu trong Report 5.0.
