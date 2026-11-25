---
title: "Phân trang danh sách gói đề của khu soạn nội dung (backend) và bỏ ô Mở trực tiếp bằng ID"
description: "GET /api/content/admin/packages hiện cắt cứng 500 gói, không phân trang: gói thứ 501 trở đi không bao giờ hiện. Thêm page/size và totalElements ở backend, frontend hiện thanh phân trang"
status: done
priority: P2
branch: "feat/author-package-pagination (backend + frontend)"
tags: [content-service, frontend, admin, content-package, pagination]
blockedBy: []
blocks: []
created: "2026-11-24T00:19:00.000+07:00"
createdBy: "Claude Code"
source: "hội thoại 2026-11-24: trang Đề & gói câu hỏi dài hơn 470 dòng, chưa phân trang"
---

# Phân trang danh sách gói đề

Repo: `backend/services/content-service` (chính) và `frontend`. Không chung file với plan `261123-2220-lesson-editor-pickers` (phase 02 của plan đó sửa API câu hỏi, không sửa API gói).

## Đã làm (2026-11-24)

Bỏ ô "Mở trực tiếp bằng ID" khỏi `PackagesPage.tsx`, cùng test của nó. Không thuộc các phase dưới.

## Vấn đề

- Route thật là `GET /api/content/admin/packages` (`ContentPackageAuthoringController.list`), đọc bằng `JdbcAuthorPackageReader.list`.
- Truy vấn có `LIMIT 500` cứng (`AuthorPackageReader.LIST_LIMIT`), sắp `updated_at DESC, id`. Contract `content-package-authoring-v1.md` dòng 85-86 ghi rõ "no pagination in v1".
- Hệ quả: khi tổng gói vượt 500, gói cũ nhất **không thể thấy hay mở** từ giao diện, không có báo gì. Dữ liệu demo hiện có khoảng 474 gói, tức sắp chạm.
- Frontend vẽ toàn bộ dòng một trang dài.
- Nơi gọi `listPackages` ở frontend: `PackagesPage.tsx` và `PlacementPoolSummary.tsx` (đếm gói placement đã xuất bản theo kho). Cái sau **không được vỡ**.

## Quyết định đề xuất (chưa duyệt)

| Chủ đề | Đề xuất | Lý do |
|---|---|---|
| Phân trang | Ở server: tham số `page` (từ 0) và `size` (1 đến 100, mặc định 25) | `AGENTS.md` §3.7; khớp danh sách câu hỏi (`question-publication-v1`) |
| Hình dạng phản hồi | Đối tượng `{ content, page, size, totalElements, totalPages }` giống `QuestionPageResponse` | Một khuôn phân trang cho cả khu soạn |
| Tương thích | **Đổi hình dạng** từ mảng sang đối tượng (thay đổi hợp đồng). Frontend và test cập nhật cùng PR | Giữ mảng khi thiếu `page` sẽ có hai hình dạng cho một route, dễ sai |
| Giới hạn 500 | Bỏ | Phân trang thay thế; vẫn chặn `size` tối đa 100 |
| Đếm kho placement | `PlacementPoolSummary` gọi với `type=PLACEMENT_TEST&status=PUBLISHED` và đọc `totalElements` cho từng kho (`placementPool=...&size=1`) | Không còn dựa vào việc tải hết rồi đếm |
| Sắp xếp | Giữ `updated_at DESC, id` | Ổn định giữa các trang nhờ `id` |

## Phases

| Phase | Nội dung | Trạng thái |
|---|---|---|
| [01](phase-01-backend-paged-list.md) | Backend: `page`, `size`, `totalElements`, contract, test | done |
| [02](phase-02-frontend-pager.md) | Frontend: thanh phân trang, cập nhật `listPackages`, `PlacementPoolSummary`, test | done, phụ thuộc 01 |

## Tiêu chí hoàn thành

- Mở trang Đề & gói: chỉ 25 dòng, có "Trang n / tổng" và tổng số gói; đổi bộ lọc hoặc ô tìm thì về trang 1.
- Gói cũ nhất (kể cả khi tổng vượt 500) vẫn truy cập được bằng cách chuyển trang.
- `PlacementPoolSummary` hiện số gói đã xuất bản từng kho đúng như trước.
- Backend và frontend xanh (lệnh ở từng phase).

## Rủi ro

1. **Đổi hợp đồng công khai.** Chỉ có frontend khu soạn gọi route này (đã tìm trong `frontend/src`; chưa kiểm `mobile/`). Cần xác nhận không còn nơi gọi khác trước khi gộp.
2. **Đếm trang khi lọc.** `totalElements` phải đếm với cùng điều kiện lọc; dùng chung một đoạn `WHERE` cho truy vấn dữ liệu và truy vấn đếm để không lệch.
3. **`PlacementPoolSummary`** hiện có thể phụ thuộc việc nhận đủ danh sách; kiểm test của nó.

## Câu hỏi còn mở

- Cỡ trang mặc định 25 hay 50? Plan hiện 25.
- Có cần chọn số dòng mỗi trang trên giao diện không? Plan hiện không (YAGNI).
