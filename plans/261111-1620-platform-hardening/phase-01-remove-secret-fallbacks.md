---
phase: 1
title: "Remove secret fallbacks"
status: done
priority: P1
dependencies: []
---

# Phase 1: Bỏ secret dự phòng trong config-repo

## Overview
Config-repo được track trong git, và có giá trị dự phòng cho secret. Deploy mà quên đặt biến thì service chạy bằng secret
công khai: ai đọc repo cũng ký được JWT external và internal, tức vào được mọi service. Phase này cho service **không
khởi động** khi thiếu secret, thay vì chạy âm thầm bằng giá trị trong repo.

## Requirements
- Bỏ phần `:<giá trị>` của mọi placeholder secret trong `infra/config-server/config-repo/*.yaml` (7 file:
  `access-service`, `api-gateway`, `assessment-service`, `community-service`, `content-service`, `notification-service`,
  `user-service`): `EXTERNAL_JWT_SECRET`, `GATEWAY_INTERNAL_JWT_SECRET`, `*_DB_PASSWORD`, `RABBITMQ_PASSWORD`. Giữ dạng
  `${NAME}`: Spring báo lỗi khởi động nêu tên biến khi thiếu. Phạm vi cuối theo quyết định ở `plan.md`.
- Giá trị không bí mật (URL, cổng, tên user mặc định cho local) giữ dự phòng như cũ.
- Thêm `backend/.env.example`: **chỉ tên biến, giá trị rỗng**, nhóm theo service, ghi biến nào bắt buộc. `.dockerignore`
  đã cho phép file này.
- Kiểm test: test Spring Boot không được dựa vào config-repo (thường tắt config server). Test nào hỏng vì thiếu biến thì
  đặt giá trị giả trong profile test của chính module đó, không đặt lại dự phòng ở config-repo.
- CI đã tạo `.env` giả cho `docker compose config`; bổ sung biến mới bắt buộc nếu Compose cần. Ghi chú: dòng
  `REDIS_PASSWORD` trong bước tạo `.env` của `ci.yml` có ký tự CR đơn lẻ trước `EXTERNAL_JWT_SECRET`; chuẩn hóa thành hai
  dòng LF.
- Tài liệu: `CLAUDE.md` §5 (bỏ cạm bẫy "config-repo có giá trị fallback"), `AGENTS.md` §5 và §7, `docs/local-development.md`
  (copy `.env.example` thành `.env`), `infra/README.md` nếu nhắc. Ghi rõ: trong Docker mỗi service chỉ nhận biến Compose
  truyền; khi chạy trên máy mọi service đọc `.env` gốc (giữ nguyên, là đánh đổi của môi trường dev).
- **Không** xoay khóa thật trong phase này; nhắc người dùng: giá trị cũ đã nằm trong lịch sử git nên coi là lộ, mọi môi
  trường ngoài máy dev phải dùng giá trị khác.

## Related Code Files
- Modify: 7 file config-repo ở trên, `.github/workflows/ci.yml`, tài liệu ở trên
- Create: `backend/.env.example`

## Implementation Steps
1. `grep` liệt kê mọi placeholder có dự phòng chứa `SECRET`, `PASSWORD`, `KEY`, `TOKEN` (chỉ in tên, không in giá trị).
2. Sửa config-repo; viết `.env.example`.
3. `mvn -B -ntp verify` cả reactor (như CI); sửa test hỏng theo yêu cầu ở trên.
4. `docker compose config --quiet` với `.env` giả; chạy local một service thiếu biến để xác nhận lỗi khởi động nêu tên biến.
5. Cập nhật tài liệu.

## Success Criteria
- [ ] Không còn placeholder secret nào có giá trị dự phòng trong config-repo (kiểm bằng `grep`).
- [ ] Thiếu `GATEWAY_INTERNAL_JWT_SECRET` thì service không khởi động và log nêu tên biến (không nêu giá trị).
- [ ] CI xanh.

## Risk Assessment
- Dev đang dựa vào dự phòng sẽ hỏng khi kéo code: thông báo trong mô tả PR, `.env.example` đủ tên biến.
- Giá trị cũ vẫn ở lịch sử git: không xóa lịch sử (phá hủy); coi như đã lộ.
