# Phase 05 - Tài liệu, test suốt luồng, triển khai

## Tài liệu

- `docs/contracts/key-distribution-v1.md` (mới): mọi endpoint của phase 02 và 03, mã lỗi `details.code`, trạng thái key suy ra, quy tắc
  idempotent, nhật ký.
- `docs/system-architecture.md`: thêm cạnh HTTP access -> user-service (`/internal/users/by-role`), bảng scheduler không đổi, bảng nhật ký.
- `docs/contracts/user-management-v1.md`: ghi endpoint nội bộ mới.
- `services/access-service/README.md`: bảng API, biến `ACCESS_KEY_CODE_SECRET`, `USER_SERVICE_URL`.
- `backend/CLAUDE.md` và `AGENTS.md`: thêm biến môi trường mới (chỉ tên) vào danh sách biến bắt buộc của access-service.
- `docs/local-development.md`: cách tạo khóa Base64 32 byte và đặt vào `.env`.

## Test suốt luồng (Testcontainers, một lớp tích hợp)

1. Tạo tài khoản: Quản lý, hai sale, một học viên đã xác minh email (qua dữ liệu thử của access-service).
2. Quản lý tạo đợt 20 key gói AI Points, giao 10 cho sale A và 5 cho sale B.
3. Sale A phát 3 key; sale B phát 1; mỗi sale chỉ thấy key của mình.
4. Học viên nhập một key **chưa phát** (còn ở kho chung hoặc trong kho của sale): bị từ chối như mã không hợp lệ, key không đổi trạng thái.
   Rồi nhập một key đã phát: ví tăng đúng số điểm; key hiện "đã kích hoạt" ở danh sách của sale và của Quản lý.
5. Quản lý phát trực tiếp một key từ kho chung, học viên khác kích hoạt được. Quản lý thu hồi toàn bộ key chưa dùng của đợt: key đã
   kích hoạt bị bỏ qua; nhập key đã thu hồi báo `KEY_REVOKED`. Một key sinh bằng `POST /admin/keys/generate` (không đợt) vẫn kích hoạt được.
6. Lặp lại với gói Premium: ngày Premium và lượt chấm giáo viên cộng đúng.
7. Mọi bước ghi nhật ký; không có dòng nhật ký, log hay phản hồi danh sách nào chứa mã đầy đủ ngoài `reveal` và `export`.

## Kiểm tra cuối

```text
mvn -q -pl services/access-service -am test
mvn -q -pl services/user-service -am test
mvn -q -pl infra/api-gateway test
mvn -q -pl services/access-service -am -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
docker compose config --quiet
frontend: npx vitest run src/features/point-management src/features/home src/features/auth src/features/admin
          npx eslint src/features/point-management src/features/auth src/features/home ; npx tsc -b
```

Thử tay trên trình duyệt (chưa có test tự động): đăng nhập bằng từng role, tạo đợt 1000 key, phân bổ, phát key bằng hai tab,
nhập key ở trang giá bằng tài khoản học viên, xuất CSV mở bằng Excel còn đúng tiếng Việt.

## Triển khai

1. Đặt `ACCESS_KEY_CODE_SECRET` vào `.env` của môi trường trước khi chạy migration V14 (thiếu thì tạo đợt báo không khả dụng, các phần khác vẫn chạy).
2. Thứ tự: frontend trước (đọc `details.code` có dự phòng câu cũ), rồi user-service (endpoint nội bộ mới), rồi access-service.
3. Route `/point-management/*` của bản demo dừng mở công khai ngay khi frontend mới lên; báo trước cho người đang dùng bản demo.
4. Luật "chặn key chưa phát" (D2) chỉ áp cho key có `batch_code`; chưa có đợt nào trước khi phát hành nên không ảnh hưởng key đang lưu hành.
5. Lùi lại: tắt route khu Sale ở frontend; backend mới chỉ thêm endpoint, bảng và một điều kiện cho key thuộc đợt, nên không phá luồng nhập key hiện có.

## Việc chưa nằm trong plan

- Thông báo cho sale khi được giao thêm key, báo cáo doanh số, tích hợp thanh toán, hoàn tiền: chưa có yêu cầu.
- Màn ADMIN riêng để sinh key không đợt (`/keys/generate`): giữ API, chưa làm giao diện (D4). ADMIN không dùng được API Quản lý (D1);
  nếu sau này cần thì thêm role cho ADMIN hoặc mở lại quyết định này, không sửa lặng lẽ.
