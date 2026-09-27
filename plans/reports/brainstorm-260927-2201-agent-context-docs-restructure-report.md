# Brainstorm: tái cấu trúc tài liệu ngữ cảnh cho agent (CLAUDE.md / AGENTS.md)

- Ngày: 2026-09-27 · Branch: `feat/ai-learning-service` · HEAD lúc đánh giá: `79f9fd6`
- Quyết định: **hướng B (tách lớp)** + **loại `third_party/` khỏi graphify** (người dùng duyệt)
- Modes: không có `--html` / `--wiki`

## 1. Vấn đề

Câu hỏi: `CLAUDE.md` đã đủ ngữ cảnh để Claude làm việc trên codebase chưa? Kết luận: **chưa, và một phần gây hiểu sai.**

Bằng chứng (scout + trải nghiệm thực tế khi code phase 1 hạn mức tutor trong cùng phiên):

| # | Vấn đề | Bằng chứng |
| --- | --- | --- |
| 1 | Sai sự thật, lỗi thời | `CLAUDE.md` "cập nhật 2026-09-18", sửa lần cuối `115c36d` (2026-09-20), **79 commit** trước HEAD. Ghi "chỉ có User", "5 module", "không có messaging", compose chạy config/eureka/gateway/user. Thực tế: 13 module first-party, 9 service, RabbitMQ, compose chỉ một phần stack. |
| 2 | `AGENTS.md` không được nạp, và ra lệnh sai | Claude Code chỉ auto-load `CLAUDE.md`; không có `@import`. `AGENTS.md` §2 "Tech stack bắt buộc" không có Python; "không có message broker… mọi bổ sung phải là quyết định thiết kế" → agent tuân thủ có thể coi ai-learning/RabbitMQ là vi phạm. |
| 3 | Mù phần đang phát triển nhất | 0 lần nhắc `ai-learning`, Python, FastAPI, RabbitMQ, DeepTutor, `third_party`, `pytest` trong cả hai file. |
| 4 | Thiếu bất biến | Không import DeepTutor (có test), test không gọi LLM thật, không log nội dung hội thoại, một commit mỗi phase — chỉ biết được qua plan. |
| 5 | Thiếu thông tin vận hành | Cách chạy test Python (venv 3.11, `AI_LEARNING_TEST_DATABASE_URL`, `AI_LEARNING_TEST_AMQP_URL`) tốn ~6 lượt tool để khám phá; topology host vs compose, cổng, `.env` được Spring import, `CONTENT_SERVICE_URL` của game sai khi chạy host; `.pyc` từng bị track. |
| 6 | Graphify lệch | 37k/46k node là `third_party/deeptutor` (`.graphifyignore` chỉ bỏ `*.md`) → `graphify query` bị kéo về DeepTutor, trong khi hook bắt graphify-first trước mọi grep/read. |
| 7 | Hình thức | 277 dòng nạp mỗi phiên, phần lớn là mô tả kiến trúc suy ra được từ code (dễ lỗi thời); phần lệnh/quy tắc/cạm bẫy mỏng; trùng nội dung với `AGENTS.md` (298 dòng). |

Điểm tốt cần giữ: security model (Gateway ký internal JWT, không tin `X-User-*`), layer Clean/DDD, chỉ ghi tên biến secret, mục "chưa nhất quán" và "chưa xác định".

## 2. Các hướng đã cân nhắc

| Hướng | Ưu | Nhược |
| --- | --- | --- |
| A. Sửa tại chỗ | Ít thay đổi | Vẫn dài, trùng AGENTS.md, lỗi thời lại nhanh |
| **B. Tách lớp (chọn)** | Đúng, rẻ context, ít dữ kiện dễ đổi, nested CLAUDE.md chỉ nạp khi cần | Chạm 4–5 file, cần nhóm đọc lại |
| C. Sinh lại tự động (`/init`, docs-manager) | Nhanh | Chung chung, mất quyết định viết tay, vẫn phải review |

## 3. Giải pháp đã chốt

Ngôn ngữ: tiếng Việt, identifier/lệnh/biến giữ tiếng Anh (như hiện tại).

1. **Root `CLAUDE.md` (~120 dòng, ≤ 150)** — nạp mỗi phiên:
   - Dự án 1 đoạn; `@AGENTS.md` để nạp quy tắc.
   - Bảng module 1 dòng/module: ngôn ngữ, DB + cổng, cổng service, route Gateway; `third_party/deeptutor` = chỉ tham khảo.
   - Chạy local: Java trên host; compose chạy learning-support-db 5433, community-db 5434, game-db 5435, RabbitMQ 5672/15672, stack AI Learning (DB 5436, API 8000); Postgres 5432 cần user/content/assessment/access/notification DB; `.env` root được compose và Spring (`optional:file:../../.env[.properties]`) nạp; thứ tự khởi động.
   - Lệnh Java (`mvn -pl <module> test`, compile), `docker compose config --quiet`; Python → nested CLAUDE.md.
   - Giao tiếp: HTTP nội bộ (assessment→content/user, game→content, ai-learning→content/user), `AssessmentCompleted.v2` qua RabbitMQ; link `docs/contracts/`.
   - Bất biến chung: internal JWT; không lộ secret; không import DeepTutor; không log nội dung hội thoại/prompt/key; không sửa migration đã áp dụng; commit conventional, không nhắc AI/mã plan/số phase.
   - Cạm bẫy (cập nhật từ §13 cũ): `CONTENT_SERVICE_URL` game; outbox chỉ assessment relay; notification-service là skeleton; `PUT` vs `PATCH` user status; secret mặc định fallback trong `api-gateway.yaml`.
   - Tài liệu tra cứu + cách dùng graphify (sau khi loại `third_party/`).
2. **`services/ai-learning-service/CLAUDE.md` (mới, ≤ ~100 dòng)** — chỉ nạp khi làm trong service:
   - Stack Python 3.11/FastAPI/psycopg2 (connection mỗi lần gọi)/pydantic-settings prefix `AI_LEARNING_`.
   - Vai trò package trong `app/`; `mastery/` là bản port DeepTutor v1.6.9: giữ hành vi, test giữ giá trị gốc.
   - Bất biến: lượt tutor luôn đóng; DTO camelCase alias; test không gọi LLM thật; migration V* (Flyway) được test dựng lại schema.
   - Lệnh test: venv 3.11, `pip install pytest -r requirements-test.txt`, env test DB/AMQP, `PYTHONDONTWRITEBYTECODE=1`.
3. **`AGENTS.md` = nguồn quy tắc duy nhất (~200 dòng, tự đứng được cho Codex/tool khác)**:
   - §2: thêm Python/FastAPI/psycopg2, RabbitMQ (Spring AMQP, pika); bỏ câu "không có message broker".
   - §3.1: mở rộng từ User Service sang mọi service Java; §6: lệnh cho mọi module + Python; §7 làm mới.
   - Chuyển mô tả kiến trúc trùng lặp sang docs.
4. **`docs/system-architecture.md` (mới, đọc khi cần)**: nhận §4–12, §14 của CLAUDE.md cũ, cập nhật đủ 9 service (sơ đồ, flow, security, data ownership, quyết định kiến trúc, bài học).
5. **`.graphifyignore`**: thêm `third_party/`; build lại graph (~46k → ~9k node). AGENTS.md ghi dùng `graphify update . --force` khi node giảm có chủ đích.

Chi phí context: CLAUDE.md + AGENTS.md ≈ 320 dòng/phiên (hiện 277 nhưng sai). Muốn thấp hơn phải bỏ import → trùng quy tắc (vi phạm DRY), không chọn.

## 4. Tiêu chí nghiệm thu

- Mọi dữ kiện khớp HEAD: số module, cổng, route Gateway, tên biến env — đối chiếu `docker-compose.yml`, `config-repo/*.yaml`, `application.yml` từng module.
- 0 giá trị secret trong tài liệu (chỉ tên biến).
- Root CLAUDE.md ≤ 150 dòng; nested ≤ 100 dòng; AGENTS.md không còn khẳng định Java-only / không broker.
- Mỗi lệnh ghi trong tài liệu đã được chạy thử thành công (ít nhất `mvn -pl <1 module> test`, `docker compose config --quiet`, lệnh pytest).
- Sau rebuild: 0 node từ `third_party/`; `graphify explain "run_turn"` trả node first-party.

## 5. Phạm vi

- Trong: `CLAUDE.md`, `AGENTS.md`, `services/ai-learning-service/CLAUDE.md` (mới), `docs/system-architecture.md` (mới), `.graphifyignore`, rebuild `graphify-out/` (gitignored).
- Ngoài: viết lại README root/service, sửa code, CI, frontend.

## 6. Rủi ro

- **Lỗi thời lại**: giảm bằng cách tránh dữ kiện dễ đổi (đếm số lớp, liệt kê class), ưu tiên con trỏ tới file nguồn; ghi "cập nhật lần cuối + commit" ở đầu file.
- **Nhóm không đồng ý chuyển nội dung**: nội dung chỉ di chuyển sang `docs/system-architecture.md`, không xóa.
- **Tool khác chỉ đọc AGENTS.md**: giữ AGENTS.md tự đứng được (quy tắc đầy đủ, không phụ thuộc CLAUDE.md).
- **Graph mất tham chiếu DeepTutor khi port code**: đọc trực tiếp `third_party/deeptutor` khi cần; ai-learning không import nó.

## 7. Bước tiếp theo

- `/ck:plan` (mặc định, không `--tdd`: chỉ đổi tài liệu, không đổi hành vi code), đầu vào là report này.

## Câu hỏi chưa giải quyết

- Có muốn đưa root `README.md` về cùng chuẩn (bỏ phần trùng) ở đợt sau không?
- Postgres local chưa có `access_db`, `notification_db`: tài liệu nên ghi lệnh tạo DB hay để người chạy tự tạo?
