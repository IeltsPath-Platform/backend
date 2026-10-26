---
phase: 2
title: "Hồ sơ examiner và danh sách cho học viên"
status: completed
priority: P1
effort: "0.5d"
dependencies: []
---

# Phase 2: user-service

## Dữ liệu

Bảng `examiner_profiles` (khóa chính `user_id`, tham chiếu `users`):

| Cột | Ý nghĩa |
|---|---|
| `ielts_band NUMERIC(2,1)` | Band IELTS của examiner (bước 0.5, 0 đến 9), có thể trống |
| `certificates JSONB` | Mảng tối đa 10 chứng chỉ `{name, issuer, year, score}` |
| `accepting_submissions BOOLEAN` | Đang nhận bài; mặc định `TRUE` (giả định, xem câu hỏi mở) |
| `daily_limit INT` | Số bài mới nhận tối đa mỗi ngày, mặc định 10, từ 1 đến 50 |

Tên, ảnh, giới thiệu lấy từ hồ sơ công khai sẵn có (`displayName`, `avatarUrl`, `bio`).

## API

- `GET /api/users/examiners` (đã đăng nhập): examiner `ACTIVE` đang nhận bài, kèm tên, ảnh, giới thiệu, band, chứng chỉ.
- `GET /api/users/examiners/{id}`: một examiner (kể cả khi đang tắt nhận bài, để học viên xem người đã chọn).
- `PUT /api/users/me/examiner-profile` (EXAMINER): sửa band, chứng chỉ, đang nhận bài.
- `PUT /api/users/{id}/examiner-profile/capacity` (ADMIN): đổi `daily_limit`.
- Nội bộ `GET /internal/users/examiners/{id}`: `{id, acceptingSubmissions, dailyLimit}` cho assessment-service; 404 khi không phải examiner `ACTIVE`.

## Kiểm chứng

Test controller, use case và migration (PostgreSQL thật qua Testcontainers).
