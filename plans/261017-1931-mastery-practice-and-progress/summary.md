# Tóm tắt các phase

| Phase | Service | Làm gì | Hệ thống thay đổi thế nào | Học viên có thấy khác không | Phụ thuộc |
|---|---|---|---|---|---|
| 1. Metadata KP | content | KP thêm CEFR, can-do, phạm vi; mã `G`/`V`; seed ~13 KP Grammar/Vocab có cạnh (chờ duyệt) | Cây đủ 6 miền như tài liệu mentor | Có nhãn CEFR trên cây | – |
| 2. Mastery MP-1.0 | learning | Cửa sổ 8, half-life 30 ngày, trọng số nguồn, đủ 3 bằng chứng/2 ngày, biên đệm, tier `INSUFFICIENT`, job giảm theo thời gian; giữ port DeepTutor, chọn bằng cờ | Mastery đáng tin hơn, không nhảy tier | Có trạng thái "Chưa đủ dữ liệu" | – |
| 3. Luyện tự do | learning | Cây của học viên tô màu theo tier; luyện bất kỳ KP, chừa 2 gói cho bài ôn; không chặn, không vào điểm course | Đúng US-010: chọn KP yếu để luyện | Có màn cây và nút Luyện | 2 |
| 4. Báo cáo kỹ năng | learning, user | Gom mastery theo 6 khối, top 3 KP, band ước tính, so với band mục tiêu (user-service mở endpoint nội bộ) | Báo cáo trả lời "còn cách mục tiêu bao xa" | Dashboard band hiện tại vs mục tiêu | 2 |
| 5. Bản chụp tuần/tháng | learning | Job chụp tuần/tháng, xu hướng 4–12 tuần, phút học, dự kiến ngày đạt mục tiêu | Có lịch sử tiến độ | Màn "Tuần của bạn", màn tháng | 4 |
| 6. Flashcard 5 hộp | library | Sửa test library; hộp Leitner 1/2/4/8/16 ngày, danh sách thẻ đến hạn, ôn thẻ | Từ vựng có lịch ôn | Màn "Ôn từ vựng hôm nay" | – |
| 7. Cờ vắng học, thống kê | learning | Cờ `INACTIVE` sau 3 ngày, tự đóng khi quay lại; DAU/WAU/MAU, giữ chân theo tuần | Giáo viên thấy học viên có nguy cơ | Không | – |
| 8. Xem trước, hẹn giờ | content | Xem bản nháp như học viên; hẹn giờ xuất bản | Phát hành nội dung có kế hoạch | Không | – |
| 9. Contract, nghiệm thu | docs | Contract, AGENTS.md, DATABASE_V5, demo 7 bước, `mvn verify` | Tài liệu khớp code | Không | tất cả |

## Song song được

- 1, 2, 6, 7, 8 độc lập.
- 3 và 4 sau 2; 5 sau 4.

## Không làm

Phụ huynh, lớp, thống kê lớp, giao bài, nhắn cả lớp; dạng bài mới và chấm phát âm; email/push/PDF; thanh toán; xuất
dữ liệu.
