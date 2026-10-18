---
phase: 8
title: "Contract, tài liệu, nghiệm thu"
status: completed
priority: P1
dependencies: [1, 2, 3, 4, 5, 6, 7]
---

# Phase 8: Contract, tài liệu, nghiệm thu

## Requirements

- **Contract** trong `docs/contracts/`:
  - tạo `gamification-v1.md` (XP, cấp, tim, streak server, đóng băng, nhiệm vụ, huy hiệu, league, mã lỗi
    `HEARTS_DEPLETED`, `HEARTS_FULL`, `FREEZE_ALREADY_OWNED`, `FREEZE_LIMIT`);
  - sửa contract bài học và Practice: khối `gamification` trong response, `403 HEARTS_DEPLETED`;
  - access: cộng thưởng nội bộ, loại giao dịch mới, trần ngày;
  - `user-internal-v1.md`: thêm `display-names`.
- **Tài liệu:** AGENTS.md §3.8 (tim chặn bài luyện, XP lần nộp đầu, streak server là nguồn thưởng), §3.5 (learning → user,
  learning → access credit), §3.6 (`learning.gamification.*`, `access.points.daily-earn-limit`);
  `docs/system-architecture.md`; `.sdd/database/DATABASE_V5.md`.
- **Kịch bản demo:**
  1. Làm Practice sai 5 câu → hết tim → `403 HEARTS_DEPLETED`; bài ôn vẫn nộp được; hồi đầy bằng 5 points.
  2. Trả lời đúng câu HARD → +20 XP; vượt 100 XP/ngày → phần thừa là shadow.
  3. Dựng 7 ngày hoạt động → streak 7, +100 XP, +10 points vào access, huy hiệu `STREAK_7`.
  4. Mua đóng băng, bỏ 1 ngày, chuỗi giữ.
  5. Nhiệm vụ `WEAKNESS` xong khi luyện KP mục tiêu.
  6. 31 học viên demo có XP → 2 nhóm league; chạy chốt tuần → lên/xuống hạng đúng.
- **Kiểm chứng:** `mvn -B -ntp verify` cả reactor, Testcontainers không skip; `docker compose config --quiet`; ghi kết
  quả thật vào `reports/acceptance.md`.

## Success Criteria

- [x] Không contract nào lệch controller. (learning, access và user-service đều đã đối chiếu với code)
- [x] Demo 1–6 chạy trên Compose. (2026-10-18, qua Gateway; xem `reports/acceptance.md`. Ghi chú: Compose dựng lại image từ code hiện tại nhưng
  không `down -v`; học viên demo chưa có dữ liệu học trước đó. Hai chỗ chưa chạy trên Compose: nộp bài ôn khi hết tim, và mọi thứ cần
  câu HARD vì nội dung seed không có câu HARD nên phải gán tạm trong DB cục bộ)
- [x] `mvn -B -ntp verify` xanh. (cả reactor, 2026-10-18; xem `reports/acceptance.md`; sau đó learning-service đổi (catalog có `band`,
  V31) và chạy lại riêng module: 714 test, 0 lỗi)

## Tiến độ (2026-10-18)

- Xong (phần learning, Claude): `docs/contracts/gamification-v1.md` (XP, cấp, tim, streak, đóng băng, nhiệm vụ, huy hiệu, league,
  điểm thưởng, mã lỗi); `lesson-learning-v1.md` thêm khối `gamification`, `403 HEARTS_DEPLETED`; AGENTS.md §3.5, §3.6, §3.8;
  `.sdd/database/DATABASE_V5.md` (bảng kiểm kê learning 30 → 45, §7.22–§7.28). Mã lỗi tôi đối chiếu với `GlobalExceptionHandler`
  và các use case; hai mã còn thiếu handler (`INSUFFICIENT_POINTS`, `ACCESS_UNAVAILABLE`) đã được thêm cùng test.
- Xong (Codex, commit dd6362c): `access-points-v1.md` (cộng thưởng nội bộ, trần ngày, consumer C6) và `user-internal-v1.md`
  (display-names). Tôi đã đối chiếu cả hai với `InternalUserController`, client của learning và listener Access.
- Đã chạy (2026-10-18): `docker compose config --quiet` (exit 0); `mvn -B -ntp -o verify` cả reactor xanh, 0 bỏ qua; vòng
  khứ hồi điểm thưởng với RabbitMQ thật (`PointsGrantBrokerRoundTripTest`, Access được mô phỏng). Xem `reports/acceptance.md`.
- Đã chạy (2026-10-18, sau khi chuyển ổ đĩa Docker sang E:): 6 kịch bản demo trên Compose thật và grant qua consumer Access thật,
  kể cả Access tắt rồi bật lại. Chạy ra hai lỗi cấu hình mà test không bắt được (Access thiếu RabbitMQ, learning thiếu
  `USER_SERVICE_URL`), đã sửa; chi tiết ở `reports/acceptance.md`.
- Không còn việc trong phase. `docs/system-architecture.md` không tồn tại trong repo (chỉ có `.sdd/global/system-architecture.md`,
  không nói về nội bộ learning); không tạo mới vì ngoài phạm vi.
- Không ghi "đã chạy" cho các mục chưa chạy.
