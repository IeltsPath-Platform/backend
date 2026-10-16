# Tóm tắt các phase

| Đợt | Phase | Service | Làm gì | Hệ thống thay đổi thế nào | Học viên có thấy khác không | Phụ thuộc |
|---|---|---|---|---|---|---|
| 1. Dữ liệu nền | 1. Cây tri thức | content | Bảng cạnh tiên quyết KP → KP, chặn vòng, API sửa/ngưng KP, API xem cây, API nội bộ cho learning | Hệ thống biết KP nào là nền của KP nào | Không | – |
| 1 | 2. Seed cây 5.5 → 7 | content | Seed ~20 KP mới và cạnh cho R/L/W, course 6.0 và 7.0 ẩn, difficulty cho mọi câu, ≥ 3 bộ câu mỗi KP | Có cây thật để dùng; câu mới bắt buộc có difficulty | Không | 1 |
| 1 | 3. Mã lỗi | content, learning, assessment | Danh mục ~25 mã lỗi IELTS, gắn mã cho phương án sai, luật phân loại câu điền, giám khảo chọn mã từ danh mục | Biết học viên sai kiểu gì, không chỉ biết sai | Không | – |
| 1 | 4. Event log | learning, assessment | Mỗi câu nộp ghi 1 dòng (câu, KP, đúng/sai, thời gian, mã lỗi, nguồn); nhận `durationMs`; event v2 thêm `duration_ms` | Có dữ liệu chi tiết từng câu và thời gian làm | Không (FE gửi thêm thời gian) | 3 |
| 2. Hiểu học viên | 5. Mastery 3 mức | learning | Hồ sơ mastery theo học viên × KP: mức <60 / 60–79 / ≥80, hạn ôn, lỗi hay gặp; API báo cáo Score / Error / Evidence / Action | Có báo cáo theo Knowledge ID ("Key" của mentor) | Có màn báo cáo mới | 4 |
| 2 | 6. Giỏ ôn nhiều mức | learning | Bài ôn <60% vẫn chặn; 60–79 và đến hạn ôn vào giỏ tùy chọn; recheck | Bài ôn chia bắt buộc / tự chọn; practice test tự do không khóa bài | Có giỏ ôn | 5, plan 261009 |
| 2 | 7. Chẩn đoán dò ngược | learning | Trượt 2 set ôn hoặc hết bộ câu → thăm dò KP nền, dò sâu tối đa 3 tầng, chốt điểm tắc, giao bài ôn KP gốc trước | Cây tri thức quyết định hành vi học | Có màn chẩn đoán | 1, 2, 6 |
| 3. Luồng đánh giá | 8. Practice test theo mốc | content, assessment, learning | Cho làm practice test, gắn đề vào mốc trong course, lưu điểm lần đầu | Practice test dùng được, tính vào Process | Có đề luyện theo mốc | – |
| 3 | 9. Điểm 20/30/50 | learning | Thi xong topic test là mở topic sau; thi hết topic test mở course test; điểm tổng có trọng số Admin cấu hình; dưới 70% → bổ trợ → thi lại | Đổi luật học và thi (thay đổi lớn nhất) | Có, đổi luật mở topic và course | 4, 8 |
| 3 | 10. Hàng chờ giám khảo | assessment | API hàng chờ, nhận việc; essay course test luôn người chấm mù; LLM chấm song song để so sánh; tài liệu chính sách AI | AI không còn quyết định đạt/trượt | Course test chờ giám khảo | 3 |
| 4. Chất lượng và vận hành | 11. Rubric và test AI | content, assessment, learning | Rubric thành dữ liệu; hai bộ chấm dùng chung; gold set; đo accuracy, QWK, false-positive; prompt phải qua ngưỡng mới bật | AI được kiểm định trước khi dùng | Không | 3, 10 |
| 4 | 12. Khu quản trị | learning, assessment, access | Hồ sơ 360°, cờ bất thường, hàng việc EXAMINER / R&D (Action / Result / Note), R&D chấm lại để đánh giá Mentor, tín dụng chấm bởi người | Có phân việc System / Mentor / R&D | Có ghi chú từ Mentor | 5, 7, 9, 10, 11 |
| 4 | 13. Contract và nghiệm thu | docs | Cập nhật contract, AGENTS.md, DATABASE_V5, kịch bản demo, chạy `mvn verify` | Tài liệu khớp code | Không | tất cả |

## Song song được

- Nhánh A: 1 → 2
- Nhánh B: 3 → 4 → 5
- Nhánh C: 10 → 11
- 6, 7 chờ plan 261009 merge; 8 → 9 sau đó.

## Cần người làm (không phải code)

- Người hiểu IELTS duyệt cây, mã lỗi, rubric.
- Nhóm nội dung soạn bộ câu (`seed-template.md`).
- Vài chục essay chấm tay làm gold set.
- FE làm các màn ghi ở mục "Bàn giao FE" của từng phase.
