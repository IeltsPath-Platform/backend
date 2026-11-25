# Phase 05. Tài liệu, PR, deploy, lùi lại

[Plan](plan.md).

## Tài liệu

| File | Nội dung |
| --- | --- |
| `AGENTS.md` §2 | Dòng Redis: thêm cache cây topic của Learning |
| `AGENTS.md` §3.9 | Thêm dòng "Cache dữ liệu dùng chung": nơi gọi chọn rõ, có TTL, khóa có phiên bản, Redis lỗi thì đọc nguồn; không cache quyền, tiền, quota, tiến độ, URL ký |
| `AGENTS.md` đầu file | Ngày cập nhật |
| `CLAUDE.md` §3 | Chạy IDE: `docker compose up -d postgres rabbitmq redis` |
| `docs/system-architecture.md` §9 | Khóa, TTL, nơi dùng, hành vi khi Redis lỗi |
| `docs/local-development.md` (dòng 79 và phần chạy IDE) | `REDIS_PASSWORD` nay cần cho Learning |
| `docs/deployment.md` (dòng 51) | Thêm Learning vào nơi dùng mật khẩu Redis; xoay mật khẩu Redis phải deploy lại Learning |
| `README.md` (bảng pattern) | Một dòng cho cache |
| `services/learning-service/README.md` | `learning.topic-cache.*`, `LEARNING_TOPIC_CACHE_ENABLED` |

Contract HTTP và event không đổi.

## PR

Một PR vào `main`. Mô tả nêu: số đo trước và sau, bảng nơi cache và nơi đọc mới, pattern áp dụng (§3.5, §3.9), test nào chạy
thật, biến môi trường mới của Learning, cách tắt. Conventional commit; không nhắc mã plan hay số phase trong code và commit.
Chờ CI chạy xong và xanh rồi mới merge.

## Thứ tự deploy

1. Render lại env production để `learning-service.env` có `REDIS_HOST`, `REDIS_PASSWORD`.
2. Deploy Config Server.
3. Deploy Learning.
4. Xem 15 phút: tỷ lệ `hit`, số `error`, RAM của Redis, kích thước AOF, số request tới `topic-sequence`.

## Lùi lại

| Tình huống | Cách làm |
| --- | --- |
| Dữ liệu hiển thị sai hoặc Redis quá tải | Đặt `LEARNING_TOPIC_CACHE_ENABLED=false`, tạo lại container Learning |
| Lỗi code | Revert PR, deploy lại Config Server và Learning |
| Learning không khởi động vì thiếu `REDIS_PASSWORD` | Render lại env rồi tạo lại container |

Không có migration. Khóa Redis tự hết hạn sau 60 giây.
