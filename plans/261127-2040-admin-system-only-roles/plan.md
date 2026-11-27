---
title: "Admin chỉ quản lý hệ thống và người dùng, thêm đặt lại mật khẩu"
description: "Bỏ các thao tác nghiệp vụ khỏi vai trò ADMIN, chuyển gói và loại mã sang Sales Manager, giữ thông báo Premium khi học viên kích hoạt mã, thêm chức năng Admin đặt lại mật khẩu người dùng."
status: pending
priority: P1
branch: "docs/plan-admin-system-only-roles"
tags: [user-service, access-service, library-service, learning-service, api-gateway, frontend, rbac]
created: "2026-11-27T20:40:00+07:00"
updated: "2026-11-27T20:45:00+07:00"
---

# Plan: Admin chỉ quản lý hệ thống và người dùng

Nhóm chốt ngày 27/11/2026: Admin không tham gia nghiệp vụ IELTS và bán mã. Báo cáo 3 và 4 đã mô tả theo quyết định này, code cần làm theo.

Cùng đợt này làm luôn chức năng Admin đặt lại mật khẩu cho người dùng (UC-96 của Báo cáo 3, chưa có trong code).

## Quyền sau khi sửa

| Thao tác | Hiện tại | Sau khi sửa |
|---|---|---|
| Tài khoản, vai trò, lịch sử đăng nhập, sự kiện bảo mật, giám sát, thống kê, dead-letter, giới hạn bài của giám khảo | ADMIN | ADMIN (giữ) |
| Điều chỉnh điểm, hoàn điểm | ADMIN | ADMIN (giữ) |
| Kiểm duyệt cộng đồng | ADMIN | ADMIN (giữ) |
| Cấp gói Premium trực tiếp | ADMIN | Bỏ |
| Sinh mã rời, thu hồi mã theo id | ADMIN | Bỏ (Sales Manager đã quản lý mã theo lô) |
| Tạo gói, tạo loại mã | ADMIN | STAFF_MANAGER |
| Sửa kho từ vựng | CONTENT_AUTHOR, ADMIN | CONTENT_AUTHOR |
| Xem, đổi mục tiêu học của học viên | ADMIN | Bỏ |
| Trừ lượt giám khảo chấm của học viên (`consume-human-grading`) | ADMIN | Bỏ |
| Đặt lại mật khẩu người dùng | Chưa có | ADMIN (làm mới) |

## Điểm phải để ý

Sự kiện `access.subscription-granted.v1` (thông báo và e-mail "đã có Premium") **hiện chỉ được phát từ chức năng Admin cấp gói**. Kích hoạt mã không phát. Nếu chỉ xoá chức năng cấp thì thông báo này mất hẳn. Phase 1 chuyển việc phát sự kiện sang kích hoạt mã Premium.

## Phases

| # | Nội dung | Repo | Phụ thuộc |
|---|---|---|---|
| 1 | [access-service: bỏ cấp gói và sinh mã, chuyển gói/loại mã, phát sự kiện khi kích hoạt](phase-01-access-service.md) | backend | |
| 2 | [library, learning, gateway](phase-02-library-learning-gateway.md) | backend | |
| 3 | [Web: bỏ nút và route của Admin](phase-03-frontend.md) | frontend | |
| 4 | [user-service: Admin đặt lại mật khẩu](phase-04-admin-password-reset.md) | backend | |
| 5 | [Tài liệu code và test case Report 5](phase-05-docs-and-test-cases.md) | backend, report | 1, 2, 3, 4 |

Phần bỏ quyền: merge web trước hoặc cùng lúc với backend, để Admin không còn nút gọi vào API đã bỏ. Phần đặt lại mật khẩu thì ngược lại: merge backend trước rồi mới tới nút trên web. Mobile không phải sửa.

## Tiêu chí nghiệm thu

- [ ] Token chỉ có ADMIN không cấp được gói, không sinh hay thu hồi mã, không tạo gói và loại mã, không sửa kho từ vựng, không đọc hay đổi mục tiêu học của người khác.
- [ ] Sales Manager tạo được gói và loại mã mới.
- [ ] Kích hoạt mã Premium tạo đúng một thông báo và một e-mail; gửi lại cùng request không tạo thêm.
- [ ] Hai request đồng thời vẫn không tạo được hai subscription Active cho một người.
- [ ] Content Author vẫn quản lý từ vựng như trước.
- [ ] Web không còn mục "Cấp gói Premium" và trang từ vựng cho Admin.
- [ ] Admin đặt lại mật khẩu được cho người dùng khác: mật khẩu mới đăng nhập được, phiên cũ bị đăng xuất, người dùng nhận e-mail báo.
- [ ] Không còn đường `consume-human-grading`.
- [ ] Test của các module đã sửa và test web đều qua.

## Không làm trong plan này

- Không đổi dữ liệu cũ: subscription có `source_type = 'ADMIN'` và bảng `admin_grant_requests` giữ nguyên, không có migration.
- Không đổi quyền điểm và kiểm duyệt.
- Không bắt người dùng đổi mật khẩu ở lần đăng nhập sau (Báo cáo 3 không yêu cầu).

## Đã chốt (27/11/2026)

- Bỏ `consume-human-grading`.
- Giữ bảng `admin_grant_requests`, nên Report 4 vẫn là 242 bảng.
- Làm thêm Admin đặt lại mật khẩu, trong cùng plan.
