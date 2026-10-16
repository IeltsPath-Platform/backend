# Rà soát plan dashboard cá nhân

> Báo cáo này viết trước khi bỏ ôn từ vựng và bỏ việc sửa luồng chấm bài. Các dòng nhắc vocab, provenance, snapshot bên dưới không còn áp dụng; chưa rà soát lại sau thay đổi.

Ngày: 14/10/2026. Loại: tự rà soát nguồn và tính nhất quán của tài liệu; không phải kiểm thử ứng dụng hay review độc lập.

## Kết quả

- Hai quyết định người dùng được giữ ở plan, thiết kế và phase: lesson completion; bỏ Mentor.
- Phân biệt rõ hiện trạng/source với API, schema và tham số đề xuất.
- Đã đọc lại plan và cả ba phase, rà công thức/owner/dependency theo bản thiết kế.
- Sáu file nội dung trước khi thêm báo cáo này được kiểm bằng Node: không link file nội bộ hỏng, không code fence mất cặp, không placeholder stub, không trailing whitespace; đường dẫn đầu mỗi dòng bằng chứng đều tồn tại.
- `ck plan status` đọc được plan, đủ 3 phase pending. Pending chỉ việc implementation, không có nghĩa tài liệu còn stub.
- Python alias Windows không chạy được; kiểm tra tài liệu được thực hiện lại thành công bằng Node có sẵn. Không cài dependency hay đổi cấu hình máy.
- Git status cho thấy chỉ thư mục plan mới được thêm trong task; các untracked assets/capstone-docs/report có từ trước được giữ nguyên.

## Các rủi ro đã xử lý trong thiết kế

| Rủi ro | Cách xử lý |
|---|---|
| Nhầm mastery/band với tiến độ | Công thức lesson completion, denominator theo skill, null khi thiếu nội dung |
| Nhân đôi thời gian giữa tab/retry | Lease theo user, sequence/receipt, interval không chồng, atomic daily totals |
| Tạo band giả do fallback | Snapshot provenance, eligibility, không Overall khi thiếu 4 skill cùng attempt |
| Vocab chỉ là biểu đồ không có nguồn | Có personal progress + review queue + submit + history và version race check |
| Sai denominator hoặc target | Dedupe sense/lesson; không copy daily goal toàn cục vào từng course |
| Chấm lại thành lần thi mới | Latest completed version theo attempt, giữ ngày nộp gốc |
| Overengineering | 4 API owner, không service/cache/broker mới, query theo tập/index |
| Số liệu quá khứ không có bằng chứng | NOT_TRACKED/UNKNOWN_LEGACY; không tự dựng historical time/band |

## Chưa xác nhận ở runtime

Không chạy Maven, frontend build, browser E2E, SQL EXPLAIN, tải hoặc LLM. Latency, retention, lịch ôn và interval policy còn là đề xuất cần duyệt/đo khi triển khai. Không có mâu thuẫn đã biết giữa các file; các quyết định sản phẩm chưa chốt được ghi rõ ở thiết kế §7.
