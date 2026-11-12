# Phase 1: số nền prompt v1 (8 bài examiner)

Chỉ có số liệu, không có nội dung bài. Chạy 2026-11-11 trên stack local, qua API admin của Assessment.

## Điều kiện chạy
- Model `gemini-3.5-flash`, `reasoning_effort = low` (giống Learning). Lúc chạy, `docker-compose.yml` chưa truyền `ASSESSMENT_LLM_REASONING_EFFORT` nên dùng file override ngoài repo; sau đó đã thêm biến này vào compose (người dùng đồng ý).
- Prompt `IELTS_WRITING_TASK_1@2+tplv1`, `IELTS_WRITING_TASK_2@2+tplv1` (v1, chưa đổi).
- Bộ bài: 8 bài (Task 1: 5, Task 2: 3), 64 lỗi, mã lỗi do người dùng duyệt (15 lỗi dùng mã dự phòng `GRAMMAR_OTHER`).
- Band và danh sách lỗi lấy từ file kết quả của examiner. Đầu file ghi "AI chấm độc lập, chưa phải xác nhận của giám khảo người"; người dùng chọn tin nguồn này. Nên coi số dưới đây là so với nhãn examiner đã nhận, chưa phải kiểm chứng độc lập.

## Số nền

| Chỉ số | Task 1 (n=5) | Task 2 (n=3) |
|---|---|---|
| within-half-band | 1.00 | 1.00 |
| MAE | 0.10 | 0.33 |
| QWK | 0.857 | 0.667 |
| error_recall (LANGUAGE) | 0.944 | 1.00 |
| data_error_recall (DATA) | 0.75 | không áp dụng |
| error_code_false_positive | 0.375 | 0.00 |
| corrections / bài | 9.6 | 2.67 |
| p95 latency | 8.2 s | 5.5 s |
| invalid_grade_rate | 0 | 0 |
| passed (ngưỡng hiện tại) | có | có |

Số lỗi examiner trung bình mỗi bài Task 1 là 11.2 (bài 24 lỗi kéo trung bình lên); AI trả 9.6 correction mỗi bài.

## Ghi chú
- Task 2 lượt đầu có một bài lỗi `429 RESOURCE_EXHAUSTED` của Google (giới hạn tốc độ gói miễn phí khi gọi dồn). Run đó `invalid_grade_rate = 0.33`, `passed = false`. Chạy lại sau khi hết cửa sổ giới hạn cho kết quả ở bảng trên. Chạy eval nên giãn lượt hoặc dùng gói có hạn mức cao hơn.
- Một run trước đó thất bại toàn bộ (`invalid_grade_rate = 1`) vì key LLM không hợp lệ; run vẫn `DONE`, đúng thiết kế "một bài lỗi không làm hỏng cả run".
- p95 8.2 s nằm dưới `grading-timeout-seconds` mặc định 20 s.
- `error_recall` chưa vào điều kiện `passed`; ngưỡng đặt ở phase 4, sau khi có số nền trên bộ này.
- n nhỏ (5 và 3 bài): QWK và các tỷ lệ dao động mạnh theo từng bài, chưa đủ để đặt ngưỡng cuối.

## Chưa giải quyết
- Chuyên gia xét 15 lỗi đang dùng mã dự phòng (bị động, dạng động từ sau modal, giới từ, so sánh); số `error_code_false_positive` có thể đổi khi mã mới được duyệt.
- Band và lỗi trong bộ 8 bài do examiner gửi, đầu file ghi chưa phải chấm của giám khảo người; người dùng đã chọn tin nguồn này.
