# Dev B bàn giao: cách gộp với Dev A

Viết cho: Dev A và người điều phối. Ngày 2026-11-11.

## Gộp cái gì

| Repo | Nhánh gộp | Gồm |
| --- | --- | --- |
| backend | `integration/dev-b-merge` | Toàn bộ việc của Dev B, đã gộp vào `origin/main` mới nhất và đã kiểm. Một PR duy nhất. |
| backend | `fix/notification-broker-test-isolation` | Hai commit sửa test notification. **Đã nằm trong** `integration/dev-b-merge`; chỉ cần nếu muốn sửa test độc lập với phần còn lại. |
| frontend | `chore/production-deployment` | Đã chứa `fix/grade-writting-llm` và `chore/small-fixes-and-docs`. Gộp sạch vào `main`. |

Các nhánh riêng lẻ (`feat/common-llm`, `chore/*`, `test/architecture-rules`) vẫn còn trên remote, nhưng gộp từng nhánh sẽ xung đột ở `AGENTS.md`, `CLAUDE.md` và file trạng thái plan. Dùng nhánh tích hợp để tránh phải xử lý lại.

## Đã kiểm trên nhánh tích hợp

- `mvn -B -ntp verify` cả reactor: 19/19 module xanh (config-server và notification-service chạy lại sau khi sửa).
- Frontend sau khi gộp vào `main`: typecheck, 29 test liên quan và build xanh.
- Gộp tuần tự các nhánh vào `main`: xung đột chỉ ở tài liệu; đã giải quyết trong nhánh tích hợp.

## Phát hiện khi gộp vào `main` mới (đã sửa trong nhánh tích hợp)

1. **`user-service` giờ phát event qua RabbitMQ** (do `main`). Cấu hình của nó còn mật khẩu dự phòng và script tạo env production không cấp `RABBITMQ_*` cho nó. Đã bỏ dự phòng và thêm `user` vào danh sách service dùng broker, kèm test.
2. **Test notification (`SubscriptionGrantedBrokerIntegrationTest`) đỏ ở mọi lần chạy toàn module** nhưng xanh khi chạy riêng. Hai nguyên nhân: queue dùng chung giữa các test, và test gọi `replay` ngay sau khi gửi message chưa xác nhận. Đã dọn queue trước mỗi test và chờ message vào DLQ.
3. **`main` vẫn còn `response-timeout: 60s`** ở route chấm bài. Spring Cloud Gateway đọc giá trị này dưới dạng số mili giây; nhánh tích hợp đã đổi thành `60000` và cập nhật test.

## Dev A cần biết

- **Migration Content:** Dev B dùng V124. Dev A bắt đầu từ **V125**. Không dùng V123: nếu V124 đã được áp mà V123 xuất hiện sau, Flyway báo lỗi thứ tự.
- **LLM:** mọi lời gọi LLM đi qua `shared/common-llm`; không thêm client HTTP riêng.
- **Template chấm essay:** một nguồn duy nhất ở `docs/contracts/essay-prompt-template-v*.txt`, Maven chép vào Learning và Assessment lúc build.
- **ArchUnit:** baseline đang đóng băng 38 vi phạm (content 30, learning 4, assessment 4). Code mới không được thêm vi phạm; CI đỏ nếu thêm.
- **Secret:** config-repo không còn giá trị dự phòng cho JWT, mật khẩu DB, RabbitMQ, Redis. Thiếu biến thì service không khởi động. Copy `.env.example` thành `.env`.
- **Việc của Dev A trong phase còn lại** (spike latency, eval, danh mục lỗi, prompt v2, luật trần band, giao diện) đều chưa bắt đầu theo trạng thái trên remote.

## Chưa làm

- Lint workflow bằng `actionlint`: chạy image từ nguồn ngoài bị hệ thống chặn.
- Chạy stack compose production thật; deploy thật.
- Chuyên gia/examiner chưa trả lời phiếu; backend owner chưa duyệt 38 vi phạm ArchUnit.
- Hằng giá dự phòng `pointCost: 3` trong `features/practice/writing/useWritingGrading.ts` (thuộc Dev A).
- Lint frontend đỏ sẵn ở `ListeningScript.tsx` trên `main`.
