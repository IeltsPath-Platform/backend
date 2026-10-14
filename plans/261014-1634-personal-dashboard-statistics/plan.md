---
title: "Dashboard du lieu chi tiet ca nhan - BE va FE"
description: "Kế hoạch BE/FE cho thời gian học, tỷ lệ hoàn thành bài học, lịch sử band, ôn từ vựng và khóa đang học; bỏ Mentor."
status: pending
priority: P2
branch: "main"
tags: [dashboard, statistics, backend, frontend]
blockedBy: []
blocks: []
created: "2026-10-14T09:35:29.915Z"
createdBy: "ck:plan"
source: skill
---

# Dashboard dữ liệu chi tiết cá nhân — kế hoạch BE và FE

## Overview

Ngày nghiên cứu: **14/10/2026**, múi giờ Asia/Ho_Chi_Minh. Trạng thái: **đề xuất để đọc và duyệt, chưa triển khai**.

**Đọc bản phân tích chính:** [Thiết kế dữ liệu, công thức và luồng thống kê](./dashboard-statistics-design.md).

Hai quyết định đã được người dùng xác nhận:

- Tiến độ 4 kỹ năng = **tỷ lệ bài học đã hoàn thành**, không phải mastery hoặc tỷ lệ đạt band mục tiêu.
- **Bỏ toàn bộ Mentor**: không có thẻ, API, bảng dữ liệu hay công việc tích hợp Mentor trong kế hoạch này.

Dashboard còn 5 nhóm: thời gian học; tiến độ kỹ năng; lịch sử band; từ vựng cần ôn; khóa đang học. BE tính số liệu tại service sở hữu dữ liệu, FE tải các nhóm song song và chỉ trình bày. Dùng PostgreSQL và các thành phần hiện có; chưa cần analytics-service, Redis, warehouse hay gọi LLM khi mở dashboard.

Hiện trạng quan trọng: FE chi tiết đang hardcode; BE có tiến độ và kết quả nhưng chưa có bộ đo thời gian hoạt động, lịch sử ôn từ vựng hoặc API tổng hợp đủ cho màn hình. Một số band placement là fallback hoặc chưa đo; phải phân biệt trước khi vẽ biểu đồ.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Study time collection](./phase-01-study-time-collection.md) | Pending |
| 2 | [Domain statistics APIs](./phase-02-domain-statistics-apis.md) | Pending |
| 3 | [Frontend integration and validation](./phase-03-frontend-integration-and-validation.md) | Pending |

## Dependencies

- Các plan course/multi-skill đã hoàn thành là nền tảng, không phải công việc phải triển khai lại.
- `../261009-1417-practice-gated-lesson-unlock/plan.md` đang pending: có giao điểm ở luồng học nhưng không chặn số liệu `lesson_progress.completed_at`. Không dùng dashboard để thay đổi gate; nghiệm thu lại nhãn Practice sau khi plan đó triển khai. Không tạo quan hệ blocks giả hoặc sửa trạng thái plan cũ.
- `../261007-2315-placement-test-gate/plan.md` chứa mô tả cũ về placement bắt buộc; code hiện tại và AGENTS là nguồn đối chiếu, không khôi phục gate cũ trong task này.
- README tham chiếu `backend/docs/`, nhưng thư mục đó không có trong checkout được kiểm tra. Các fact trong tài liệu này được kiểm từ source/migration và wireframe ở repo docs; xem [bằng chứng](./reports/source-evidence.md).
- Phase 1 và phần đọc dữ liệu của phase 2 có thể làm song song theo service; phase 3 phụ thuộc contract của cả hai. Ôn từ vựng cần cả luồng ghi/làm bài ôn, không thể chỉ thêm biểu đồ.

## Phạm vi và nghiệm thu

- [ ] Mỗi số có định nghĩa, mẫu số, nguồn, thời điểm cập nhật, trạng thái thiếu dữ liệu.
- [ ] Nộp lại/refresh/2 tab/chấm lại không đếm đôi; 0 khác chưa có dữ liệu.
- [ ] Thời gian học có giới hạn đo lường rõ ràng; không suy ra từ thời gian mở trang hoặc độ dài audio.
- [ ] Lesson nhiều skill được tính đúng; hoàn thành lesson khác Practice PASSED, topic PASSED, course test PASSED.
- [ ] Band fallback/chưa chấm không được trình bày như năng lực đã đo.
- [ ] Thống kê từ vựng lấy từ lịch sử ôn thật; không coi lưu flashcard là đã thuộc.
- [ ] Mentor bị bỏ hoàn toàn khỏi màn hình chi tiết; không thay bằng AI Mentor.
- [ ] Query theo tập, có giới hạn, không join database khác service và không HTTP/repository trong vòng lặp.
- [ ] Kiểm tra phân quyền, concurrency và lifecycle browser theo checklist phase 3.

## Các đề xuất còn cần duyệt khi triển khai

Chi tiết và phương án khuyến nghị ở cuối [bản thiết kế](./dashboard-statistics-design.md): phạm vi course cho radar, đơn vị đếm từ vựng, quy tắc ôn, thời gian idle, thời hạn lưu telemetry và tiêu chuẩn bài thi được đưa vào biểu đồ band. Đây là đề xuất, không phải các quyết định sản phẩm đã được chấp thuận.

## Kiểm chứng của lần lập plan

Đã đọc code, migration, FE và nguồn wireframe; đối chiếu tài liệu chính thức MDN, PostgreSQL, IELTS và RabbitMQ. Chỉ tạo tài liệu trong thư mục plan này, không đọc `.env`, không đổi code, không chạy build/test ứng dụng. Các lượt nghiên cứu phụ đã cung cấp kết quả trung gian nhưng dừng do giới hạn phiên; phần tổng hợp và rà soát cuối được thực hiện trực tiếp, không tuyên bố đã có review độc lập hoàn chỉnh.

Đã kiểm tra 6 file Markdown: liên kết file nội bộ, đường dẫn bằng chứng chính, code fence và placeholder đều hợp lệ; `ck plan status` nhận đủ 3 phase pending. [Kết quả rà soát](./reports/plan-validation.md).
