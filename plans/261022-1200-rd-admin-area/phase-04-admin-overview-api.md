# Phase 04: API tổng quan cho trang chủ quản trị (backend, content-service)

Phụ thuộc: phase 01 (dùng `GetKnowledgeTreeHealthUseCase`).

## Endpoint

`GET /api/content/admin/overview` (quyền đọc: `ADMIN`, `CONTENT_AUTHOR`, `EXAMINER`) trả các số liệu thuộc content-service:

- Số KP theo trạng thái, kỹ năng, CEFR.
- Số lỗi sức khỏe đồ thị (KP cô lập, thiếu metadata, cạnh ngược cấp), lấy từ `GetKnowledgeTreeHealthUseCase`.
- Số KP active chưa có câu hỏi PUBLISHED.
- Số phiên bản câu hỏi DRAFT chờ xuất bản.
- Số lesson theo trạng thái (DRAFT, PUBLISHED, ARCHIVED).

Chỉ gồm số liệu của content-service. Frontend ghép thêm hàng việc và thống kê câu hỏi từ learning-service (`GET /api/learning/admin/work-items`, `GET /api/learning/admin/question-stats`, theo `docs/contracts/learning-staff-area-v1.md`); không tạo lớp BFF.

## Triển khai

- Use case read-only `@Transactional(readOnly = true)`, truy vấn đếm gom theo tập, không gọi repository trong vòng lặp (AGENTS.md §3.7).
- Contract mới `docs/contracts/admin-overview-v1.md`.

## Test

Mockito cho use case, `@WebMvcTest` kiểm quyền (CUSTOMER 403), Testcontainers kiểm các phép đếm trên dữ liệu seed.

## Tiêu chí hoàn thành

Một request trả đủ số liệu; các số khớp truy vấn trực tiếp trên DB tạm áp V1 đến mới nhất.

## Hình dạng response mà frontend đang đọc

Trang Tổng quan (phase 09) đọc các trường dưới đây; trường nào thiếu thì hiện dấu "—", không làm hỏng trang. Nên giữ đúng tên để khỏi sửa frontend. Sức khỏe đồ thị lấy trực tiếp từ `GET /api/content/knowledge-tree/health`, vì vậy `overview` không cần lặp lại.

```json
{
  "knowledgePoints": {
    "total": 70,
    "byStatus": { "ACTIVE": 57, "INACTIVE": 13 },
    "bySkill": { "READING": 12 },
    "byCefr": { "B1": 20 }
  },
  "activeKnowledgePointsWithoutPublishedQuestion": 12,
  "draftQuestionVersions": 4,
  "lessons": { "DRAFT": 2, "PUBLISHED": 9, "ARCHIVED": 0 }
}
```
