# Tóm tắt các phase

| Phase | Service | Làm gì | Hệ thống thay đổi thế nào | Học viên có thấy khác không | Phụ thuộc |
|---|---|---|---|---|---|
| 1. Points | access | Endpoint cộng thưởng nội bộ (idempotent, trần 20/ngày); loại giao dịch thưởng học, hồi tim, đóng băng | Points là tiền chung (gem gộp vào) | Lịch sử points có nhãn mới | – |
| 2. XP, cấp độ | learning | Sổ XP; 10/15/20 theo độ khó, trần 100/ngày + shadow; thưởng KP 60/80%; cấp mỗi 1000 XP; cài đặt múi giờ | Có thước đo nỗ lực tách khỏi mastery | XP, cấp ở header và sau mỗi bài | – |
| 3. Tim | learning, access | 5 tim (PREMIUM 7); sai câu ở lesson, Practice, luyện tự do mất tim; +1/5h; hết tim chặn (`403 HEARTS_DEPLETED`); hồi đầy 5 points; lên cấp hồi đầy | Chặn nộp bừa; có thể tạm chặn tiến độ | Có tim, màn "Hết tim" | 1, 2, plan 3 phase 3 |
| 4. Streak server | learning, access | Streak tính từ lần nộp thật; đóng băng 10–50 points; mốc 7/30 ngày thưởng XP và points; hàng chờ cộng points sang access | Thưởng không dựa vào số client tự khai | Streak đúng, mua đóng băng | 1, 2 |
| 5. Nhiệm vụ ngày | learning | 3 nhiệm vụ: luyện 5 câu, luyện KP yếu, giữ streak; tự trao thưởng | Gamification kéo học viên về KP yếu | Khối nhiệm vụ trên dashboard | 2, 4 |
| 6. Huy hiệu | learning | ~10 huy hiệu (streak, KP MAINTAIN, thi, course, cấp, nhiệm vụ) | Ghi nhận thành tích | Màn huy hiệu | 2, 4 |
| 7. League | learning, user | Nhóm 30 người cùng hạng, XP tuần, CN 00:00 UTC top 7 lên, 23–30 xuống; 7 hạng; user-service trả tên hiển thị | Thi đua theo tuần | Tab League | 2 |
| 8. Nghiệm thu | docs | Contract `gamification-v1`, AGENTS.md, DATABASE_V5, demo 6 bước, `mvn verify` | Tài liệu khớp code | Không | tất cả |

## Song song được

- 1 và 2 song song; 3, 4 sau 1 và 2; 5, 6 sau 4; 7 sau 2.

## Câu hỏi mở cần chốt trước khi code

Trần points/ngày (mặc định 20), 7 hạng league, ẩn danh trên bảng xếp hạng, mốc cấp độ (mặc định 1000 XP/cấp).
