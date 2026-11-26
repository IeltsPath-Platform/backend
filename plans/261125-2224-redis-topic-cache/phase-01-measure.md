# Phase 01. Đo trước khi sửa

[Plan](plan.md). Chưa sửa code chính. Mục đích: biết cache có đáng làm không.

## Đã có (2026-11-25)

Đo bằng seed và migration thật trên PostgreSQL Testcontainers, tại `b23ce054` cộng đổi số tạm cho V148:

| Chỉ số | `base` | `error-traps` |
| --- | --- | --- |
| Topic | 48 | chưa đo |
| KP | 109 | chưa đo |
| JSON | 106.354 byte | chưa đo |
| Thời gian | chưa có số đáng tin, máy quá tải khi đo | chưa đo |

## Việc cần làm

1. Dừng các container không dùng; ghi lại CPU lúc đo (`docker stats --no-stream`).
2. Viết test tạm trong content-service, **không commit**, dùng:
   - `PostgreSQLContainer("postgres:15-alpine")`, chạy mọi migration, `ANALYZE`;
   - `HikariDataSource`, không dùng `PGSimpleDataSource` (mở kết nối mới mỗi truy vấn sẽ làm sai số);
   - `JdbcLearningContentReader.topicSequence(3, includeErrorTraps)` rồi `TopicSequenceResponse.from` và Jackson.
3. Với mỗi biến thể, chạy 100 lần làm nóng rồi 200 lần đo; ghi p50, p95, max của:
   - A: đọc DB và tạo JSON (phần Content phải làm);
   - B: `GET` từ `redis:7-alpine` và parse JSON thành object (phần Learning làm khi trúng).
4. Đo đường thật khi stack chạy: thời gian Learning chờ `topic-sequence`, lấy từ metric HTTP client hoặc trace.
   Không log token hay nội dung.
5. Đếm số lần gọi `topic-sequence` cho mỗi request: báo cáo, giỏ ôn, gợi ý, nhiệm vụ, lịch sử nộp bài.
6. Tính dung lượng ghi mỗi ngày: `(byte base + byte error-traps) x 1440`.
7. Ghi kết quả vào `reports/measure-261125-2224-topic-sequence-baseline-report.md`, kèm cấu hình máy và commit. Xóa test tạm.

## Điểm dừng

Báo người duyệt và **không tự sang phase 02** nếu có một trong các điều sau:

- một response lớn hơn 1 MB;
- p95 của A dưới 20 ms;
- B không nhanh hơn đường thật ở bước 4 ít nhất 2 lần.

Dù đạt hết cũng gửi báo cáo và chờ duyệt.
