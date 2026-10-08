# Phase 4: Docs, kiểm chứng liên thông, bàn giao FE

## Docs phải sửa (hiện ghi "every course stays open")

- `AGENTS.md` mục 3.8; `CLAUDE.md` nếu nhắc
- `README.md` §6 và `services/learning-service/README.md` (bảng endpoint, `403 PLACEMENT_REQUIRED`, `placement-test`)
- `docs/system-architecture.md` (luồng placement, cổng)
- `docs/fe-main-flow-guide.md` (bước placement trước course; sơ đồ; mã lỗi mới)
- `docs/contracts/lesson-learning-v1.md` (endpoint mới, lỗi mới), `assessment-completed-v2.md` (cách Learning dùng placement)
- Seed demo: nếu chọn cấp placement cho demo learner, ghi vào README và callback demo.

## Kiểm chứng

1. Test hẹp từng service (content, assessment, learning), sau đó `mvn -q compile -DskipTests` cả reactor.
2. Chạy stack Compose, gọi qua gateway bằng tài khoản mới: register → login → `GET /courses` = 403 → `GET /placement-test` → attempt → lưu câu → nộp → submission essay/audio → chờ chấm → `GET /courses` = 200 có `recommended` → `GET /placement-test` = 409.
3. Kiểm câu `PLACEMENT` không lọt vào practice/topic/mock.

## Bàn giao FE (sau khi duyệt, làm riêng)

- Guard: bắt `403 PLACEMENT_REQUIRED` → màn làm placement (tái dùng luồng attempt của thi topic: structure, PUT response, submit, result, thêm nộp essay/audio).
- Sau khi nộp: poll `GET /courses` mỗi 2 giây tới khi 200.
- Xử lý `409 PLACEMENT_ALREADY_DONE`.
- Sửa lỗi form đăng nhập: lọc ký tự zero-width ở mật khẩu/email.

## Báo cáo

Lưu kết quả kiểm chứng ở `plans/261007-2315-placement-test-gate/reports/`, nêu rõ test nào chạy, test nào bị skip (thiếu Docker).
