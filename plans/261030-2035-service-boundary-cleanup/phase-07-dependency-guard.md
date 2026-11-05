---
phase: 7
title: "Chặn vòng phụ thuộc quay lại"
status: pending
priority: P2
effort: "0.5 ngày"
dependencies: [2, 6]
---

# Phase 07: Chặn vòng phụ thuộc quay lại

## Overview

Sau các phase trước không còn vòng gọi đồng bộ. Phase này ghi quy tắc và thêm một test tự động để không ai vô tình thêm vòng mới. Không thêm thư viện kiểm thử mới (không dùng ArchUnit).

## Requirements

- Quy tắc trong `AGENTS.md` §3.5: gọi đồng bộ giữa service phải một chiều; client HTTP mới phải được ghi vào bảng phụ thuộc trong `docs/system-architecture.md` và không được tạo vòng; thông báo bằng event không tính là gọi đồng bộ.
- Mục kiểm tra trong checklist review PR (nếu repo có mẫu PR ở `.github`, thêm vào đó; nếu không, ghi vào `AGENTS.md` §6): "client HTTP mới trỏ tới service đã gọi ngược lại thì phải giải thích".
- Test tự động `ServiceDependencyGraphTest`: đọc mọi `infra/config-server/config-repo/*-service.yaml`, với mỗi tệp rút các biến dạng `${<TÊN>_SERVICE_URL...}` (ví dụ `CONTENT_SERVICE_URL`, `USER_SERVICE_URL`, `LEARNING_SERVICE_URL`) thành cạnh từ service sở hữu tệp tới service `<tên>`, dựng đồ thị và kiểm tra không có vòng. Kiểm tra cả khóa có cùng biến nhưng tên khóa khác (như `app.study-time.learning-url`, `library.content.base-url`).
- Vị trí test: cùng chỗ các test đọc config-repo hiện có (`infra/api-gateway/src/test`), hoặc nơi hợp lý hơn theo cấu trúc repo; ghi lý do chọn.

## Tests first (red)

- Test đơn vị của hàm tìm vòng với đồ thị mẫu có vòng (phải phát hiện) và không vòng (phải qua).
- Test trên config-repo thật: phải **đỏ trên `main` hiện tại** (config-repo khai báo hai vòng: user ↔ learning và user ↔ assessment; vòng content ↔ library chỉ hiện trong đồ thị sau phase 01, khi content-service có URL library) và xanh sau khi phase 02 và 06 xong. Nếu chạy phase này trước 06, đánh dấu các vòng đã biết là ngoại lệ tạm có chú thích, và bỏ ngoại lệ ở phase 06.
- Test báo thông điệp lỗi nêu đường vòng (ví dụ `user -> learning -> user`).

## Related Code Files

- Tạo: `ServiceDependencyGraphTest` (và lớp tiện ích nhỏ nếu cần).
- Sửa: `AGENTS.md` §3.5 và §6, `docs/system-architecture.md` (bảng phụ thuộc, đã cập nhật ở các phase trước), mẫu PR nếu có.

## Implementation Steps

1. Viết test đỏ trên config-repo thật.
2. Cài hàm dựng đồ thị và tìm vòng.
3. Ghi quy tắc và checklist.
4. Chạy `mvn -B -ntp verify`.

## Success Criteria

- [ ] Test xanh sau phase 02 và 06; đỏ nếu thêm một cạnh tạo vòng (thử tạm và hoàn tác để chứng minh).
- [ ] Quy tắc có trong `AGENTS.md` và bảng phụ thuộc khớp đồ thị test dựng.

## Risk Assessment

- Cạnh chỉ có trong `docker-compose.yml` mà không nằm trong config-repo sẽ không bị test bắt; mục này chỉ bảo vệ các cạnh khai báo trong config-repo. Ghi giới hạn đó trong tài liệu.
- Rollback: xóa test (không ảnh hưởng chạy thật).
