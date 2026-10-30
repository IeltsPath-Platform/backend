---
title: "Course cover image + course/topic description"
status: pending
blockedBy: []
blocks: [261030-1204-band-skill-lesson-paywall]
---

# Course cover image + course/topic description

Status: pending

## Mục tiêu

Thêm 3 cột nullable, expose qua API admin + learner, hiển thị ở FE (fallback `CourseArt` khi không có ảnh).

| Bảng | Cột | Kiểu |
|---|---|---|
| `courses` | `cover_image_url` | `VARCHAR(500)` |
| `courses` | `description` | `TEXT` |
| `topics` | `description` | `TEXT` |

Quyết định đã chốt: ảnh là URL dán vào (không upload), một ảnh dùng chung cho bìa và đại diện, không backfill dữ liệu cũ.

## Phases

| # | File | Nội dung | Phụ thuộc |
|---|---|---|---|
| 1 | [phase-01-migration-va-content-service.md](phase-01-migration-va-content-service.md) | V86, domain/entity/DTO/use case của course + topic | - |
| 2 | [phase-02-learning-service-passthrough.md](phase-02-learning-service-passthrough.md) | Đẩy 3 trường qua đường learner (content → learning → FE) | 1 |
| 3 | [phase-03-fe-admin-va-learner.md](phase-03-fe-admin-va-learner.md) | Form admin + hiển thị learner | 1, 2 |

## Acceptance criteria

- Tạo/sửa course với `coverImageUrl`, `description`; tạo/sửa topic với `description`; GET trả lại đúng; để trống thì trả `null`.
- `GET /api/learning/courses` và topic path trả 3 trường mới.
- Course có ảnh thì tile + banner hiện ảnh; không có thì vẫn là `CourseArt`.
- `TopicPage` hiện mô tả topic khi có.
- Test backend + FE liên quan xanh, migration chạy được trên DB đã có dữ liệu seed.

## Không làm

- Upload file, bảng asset cho course, ảnh riêng cho topic, `thumbnail_url` tách riêng.
- Mobile (ngoài phạm vi).
- Gợi ý/caption dưới field trong form admin.
