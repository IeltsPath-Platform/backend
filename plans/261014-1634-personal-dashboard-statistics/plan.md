---
title: "Dashboard du lieu chi tiet ca nhan - BE va FE"
description: "Kế hoạch BE/FE cho thời gian học, tỷ lệ hoàn thành bài học, lịch sử band, ôn từ vựng và khóa đang học; bỏ Mentor."
status: in_progress
priority: P2
branch: "docs/personal-dashboard-statistics-plan"
tags: [dashboard, statistics, backend, frontend]
blockedBy: []
blocks: []
created: "2026-10-14T09:35:29.915Z"
createdBy: "ck:plan"
source: skill
---

# Dashboard dữ liệu chi tiết cá nhân — kế hoạch BE và FE

## Overview

Ngày nghiên cứu: **14/10/2026**, múi giờ Asia/Ho_Chi_Minh. Trạng thái: **đang triển khai; xem báo cáo kiểm chứng**.

**Đọc bản phân tích chính:** [Thiết kế dữ liệu, công thức và luồng thống kê](./dashboard-statistics-design.md).

Hai quyết định đã được người dùng xác nhận:

- Tiến độ 4 kỹ năng = **tỷ lệ bài học đã hoàn thành**, không phải mastery hoặc tỷ lệ đạt band mục tiêu.
- **Bỏ toàn bộ Mentor**: không có thẻ, API, bảng dữ liệu hay công việc tích hợp Mentor trong kế hoạch này.

Dashboard còn 5 nhóm: thời gian học; tiến độ kỹ năng; lịch sử band; từ vựng cần ôn; khóa đang học. BE tính số liệu tại service sở hữu dữ liệu, FE tải các nhóm song song và chỉ trình bày. Dùng PostgreSQL và các thành phần hiện có; chưa cần analytics-service, Redis, warehouse hay gọi LLM khi mở dashboard.

Hiện trạng lúc lập plan: FE chi tiết còn hardcode; BE chưa có bộ đo thời gian hoạt động, lịch sử ôn từ vựng hoặc API tổng hợp đủ cho màn hình. Các phần này đã được triển khai và đang kiểm chứng sau sửa lỗi độc lập. Band fallback/chưa đo được phân biệt với kết quả đủ điều kiện; chưa nghiệm thu browser/E2E.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Study time collection](./phase-01-study-time-collection.md) | in_progress — DB Testcontainers pass; P1–P2 sửa xong; browser lifecycle (nhiều tab, sleep/resume, offline, logout, audio) chưa chạy thật |
| 2 | [Domain statistics APIs](./phase-02-domain-statistics-apis.md) | in_progress — mọi lỗi P1 (tên tham số, precision, migration) đã sửa; P3 (timezone, stale-while-reloading, 503 message) đã hoàn tất; AnswerSpecGraderTest thiếu fixture B1 còn tồn tại |
| 3 | [Frontend integration and validation](./phase-03-frontend-integration-and-validation.md) | in_progress — lint/typecheck/build pass; unit test pass; browser/E2E chưa chạy thật |

## Dependencies

- Các plan course/multi-skill đã hoàn thành là nền tảng, không phải công việc phải triển khai lại.
- `../261009-1417-practice-gated-lesson-unlock/plan.md` đang pending: có giao điểm ở luồng học nhưng không chặn số liệu `lesson_progress.completed_at`. Không dùng dashboard để thay đổi gate; nghiệm thu lại nhãn Practice sau khi plan đó triển khai. Không tạo quan hệ blocks giả hoặc sửa trạng thái plan cũ.
- `../261007-2315-placement-test-gate/plan.md` chứa mô tả cũ về placement bắt buộc; code hiện tại và AGENTS là nguồn đối chiếu, không khôi phục gate cũ trong task này.
- Khi lập plan, `backend/docs/` chưa có trong checkout; các tài liệu kiến trúc/contract được khôi phục nguyên bản trước lượt kiểm chứng cuối ngày 15/10/2026. Bằng chứng nghiên cứu ban đầu vẫn được giữ tại [source evidence](./reports/source-evidence.md).
- Phase 1 và phần đọc dữ liệu của phase 2 có thể làm song song theo service; phase 3 phụ thuộc contract của cả hai. Ôn từ vựng cần cả luồng ghi/làm bài ôn, không thể chỉ thêm biểu đồ.

## Phạm vi và nghiệm thu

- [ ] Mỗi số có định nghĩa, mẫu số, nguồn, thời điểm cập nhật, trạng thái thiếu dữ liệu.
- [ ] Nộp lại/refresh/2 tab/chấm lại không đếm đôi; 0 khác chưa có dữ liệu.
- [x] Thời gian học có giới hạn đo lường rõ ràng; không suy ra từ thời gian mở trang hoặc độ dài audio.
- [x] Lesson nhiều skill được tính đúng; hoàn thành lesson khác Practice PASSED, topic PASSED, course test PASSED.
- [x] Band fallback/chưa chấm không được trình bày như năng lực đã đo.
- [x] Thống kê từ vựng lấy từ lịch sử ôn thật; không coi lưu flashcard là đã thuộc.
- [x] Mentor bị bỏ hoàn toàn khỏi màn hình chi tiết; không thay bằng AI Mentor.
- [ ] Query theo tập, có giới hạn, không join database khác service và không HTTP/repository trong vòng lặp.
- [ ] Kiểm tra phân quyền, concurrency và lifecycle browser theo checklist phase 3.

## Mặc định triển khai đã chọn

Các mục đã đánh dấu dựa trên [kiểm thử độc lập](./reports/independent-test-report.md) và [lượt sửa lỗi trước](./reports/fix-after-independent-test.md); chúng không thay thế nghiệm thu browser/E2E. Lượt kiểm chứng cuối đang chạy, nên cả ba phase vẫn `in_progress`. Assessment study context vẫn có `courseId=null`: thời gian làm bài thi chỉ vào tổng ALL, chưa vào từng course.

Theo lựa chọn khuyến nghị trong [bản thiết kế](./dashboard-statistics-design.md): khóa đã bắt đầu, từ đếm theo nghĩa canonical, lịch 1/3/7/14 ngày, idle 120 giây, ẩn tab không tính, chỉ ghi nhận band đủ provenance. Mentor bị bỏ hẳn. Thời hạn lưu telemetry chưa bật cơ chế xóa; cần chính sách riêng trước khi vận hành lâu dài.

## Kiểm chứng của lần lập plan

Đã đọc code, migration, FE và nguồn wireframe; đối chiếu tài liệu chính thức MDN, PostgreSQL, IELTS và RabbitMQ. Chỉ tạo tài liệu trong thư mục plan này, không đọc `.env`, không đổi code, không chạy build/test ứng dụng. Các lượt nghiên cứu phụ đã cung cấp kết quả trung gian nhưng dừng do giới hạn phiên; phần tổng hợp và rà soát cuối được thực hiện trực tiếp, không tuyên bố đã có review độc lập hoàn chỉnh.

Đã kiểm tra 6 file Markdown khi tạo plan: liên kết file nội bộ, đường dẫn bằng chứng chính, code fence và placeholder hợp lệ; khi đó cả 3 phase pending. Tiến độ và kết quả triển khai hiện tại ở [báo cáo status](./reports/implementation-status.md). Agent độc lập có thể dùng [prompt kiểm thử](./independent-test-prompt.md).
