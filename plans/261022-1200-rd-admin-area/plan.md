---
title: "Khu quản trị cho R&D: API soạn nội dung và màn hình quản trị"
description: "Plan tổng BE + FE: xuất bản câu hỏi, API soạn lesson, API tổng quan, shell quản trị, đồ thị cây tri thức, ngân hàng câu hỏi, soạn bài học"
status: pending
priority: P1
branch: "feat/content-author"
tags: [content-service, frontend, admin, lesson-authoring, knowledge-graph]
blockedBy: []
blocks: []
created: "2026-10-22T12:00:00.000Z"
createdBy: "ck:plan"
source: "hội thoại với người dùng 2026-10-22 + khảo sát code content-service, learning-service, frontend"
---

# Khu quản trị cho R&D (CONTENT_AUTHOR)

Repo: `backend` và `frontend`, nhánh `feat/content-author` (đã tạo ở cả hai). Người thực hiện: Codex, làm cả hai phía. Mỗi phase ghi rõ repo.

## Context

R&D (role `CONTENT_AUTHOR`) quản lý cây tri thức và duyệt bộ câu hỏi. Backend đã có phần lớn API đọc/ghi (course, topic, KP, cây, gói đề, câu hỏi, rubric, mã lỗi, hàng việc, QA mẫu), nhưng:

- Lesson không có API ghi nào (chỉ seed SQL; `services/content-service/README.md:82` ghi rõ).
- Không có đường nào xuất bản phiên bản câu hỏi. `QuestionVersion.publish()` không có nơi gọi trong production, trong khi lesson chỉ đọc câu hỏi `PUBLISHED` (câu `DRAFT` bị bỏ im lặng ở `JdbcLearningContentReader.java:302`).
- Frontend chưa có khu quản trị, chưa có guard theo role, navbar chưa lọc theo role.

## Quyết định đã chốt với người dùng

- Khu quản trị **tách khỏi web học nhưng cùng một frontend**: route `/admin/*`, layout riêng, tông tối, sidebar icon bên trái, thanh trên có ô tìm kiếm và tài khoản (như ảnh giao diện Zamora người dùng đã gửi). Dùng chung cho `ADMIN`, `CONTENT_AUTHOR`, `EXAMINER`, menu lọc theo role.
- Lượt FE đầu gồm 4 mục: Tổng quan, Cây tri thức, Bài học, Ngân hàng câu hỏi.
- API soạn lesson làm trước. Sửa lesson theo từng block, giữ nguyên ID block.
- Thêm API xuất bản và lưu trữ phiên bản câu hỏi.
- Dùng `@xyflow/react` cho đồ thị (đã duyệt thêm dependency).
- Không thêm service mới; route giữ quy ước `/api/content/admin/**` trong content-service.
- Mô hình đồ thị tri thức theo tài liệu mentor: CEFR là trục của đồ thị, course theo band giữ vai trò lộ trình.

Ngoài phạm vi (backlog): AI sinh nháp câu hỏi/course, import Excel, liên kết từ vựng với KP ở library-service, menu Mentor/Admin ngoài những gì API sẵn có.

## Quy tắc Codex phải theo

- Đọc `backend/AGENTS.md`, `backend/CLAUDE.md`, `frontend/AGENTS.md` trước khi sửa. Giữ layer `api → application → domain`, `infrastructure → domain`; domain không import Spring/JPA.
- Không sửa migration đã áp dụng. Migration tiếp theo của content-service là **V74** (V73 đã dùng).
- Không ghi mã phase, mã plan, nhãn audit vào comment code, tên test, tên migration, commit. Commit theo conventional commit, không nhắc AI.
- Không log nội dung câu hỏi, prompt, khóa. Test không gọi LLM thật.
- Frontend: không thêm dependency ngoài `@xyflow/react`; dùng token màu có sẵn, lucide-react, shadcn primitive trong `src/components/ui/`, mobile-first, đủ trạng thái loading/lỗi/rỗng, bọc `ErrorBoundary`.
- Mỗi thay đổi API cập nhật contract trong `backend/docs/contracts/` cùng lúc. Viết contract trước khi code để FE bám theo.
- Không dùng `mvn clean` khi service đang chạy từ IDE (xem `backend/CLAUDE.md` §4).

## Phases

| Phase | Repo | Nội dung | Phụ thuộc | Trạng thái |
|---|---|---|---|---|
| [01](./phase-01-close-knowledge-graph.md) | backend | Đóng việc đồ thị tri thức: test đã xanh, còn commit | không | Test xanh, chờ commit |
| [02](./phase-02-question-publication-api.md) | backend | Xuất bản và lưu trữ phiên bản câu hỏi | không | Pending |
| [03](./phase-03-lesson-authoring-api.md) | backend | API soạn lesson, block, xuất bản | 02 | Pending |
| [04](./phase-04-admin-overview-api.md) | backend | API tổng quan cho trang chủ quản trị | 01 | Pending |
| [05](./phase-05-admin-shell.md) | frontend | Shell quản trị, guard theo role, menu | không | Pending |
| [06](./phase-06-knowledge-graph-screen.md) | frontend | Đồ thị cây tri thức (API sẵn có) | 05 | Pending |
| [07](./phase-07-question-bank-screen.md) | frontend | Ngân hàng câu hỏi | 02, 05 | Pending |
| [08](./phase-08-lesson-editor-screen.md) | frontend | Soạn bài học | 02, 03, 05 | Pending |
| [09](./phase-09-admin-overview-screen.md) | frontend | Trang tổng quan | 04, 05 | Pending |

```text
BE: 01 -> 02 -> 03 -> 04
FE: 05 -> 06 -> 07 (cần 02) -> 08 (cần 02, 03) -> 09 (cần 04)
```

## Thứ tự giao việc đề xuất

1. Phase 01 (commit) và phase 05 song song.
2. Phase 02 (contract trước) và phase 06 song song.
3. Phase 03 (contract trước) và phase 07.
4. Phase 04 và phase 08.
5. Phase 09, rồi rà soát tài liệu: `docs/system-architecture.md`, `AGENTS.md` backend (ghi khi lệch thực tế), `frontend/README.md`.

## Kiểm chứng tổng

- Backend: `mvn -q -pl services/content-service -am test` (cần Docker cho Testcontainers; content-service ~560 test, khoảng 10 phút) và `mvn -q compile -DskipTests` cho cả reactor khi đụng library hay shared. Dựng Postgres tạm áp V1 đến V74 (mỗi file một transaction, `psql -1`) để đếm lại số liệu sau migration.
- Frontend: `npm run typecheck`, `npm run lint`, `npm test`.
- Thủ công với backend thật (`docker compose up -d --build`): đăng nhập CONTENT_AUTHOR, mở `/admin`; tạo lesson DRAFT, thêm block, xuất bản bị từ chối khi thiếu KP hoặc có câu DRAFT, xuất bản câu hỏi rồi xuất bản lesson; sửa block TEXT của lesson đã xuất bản và xác nhận `blockId` không đổi; tài khoản CUSTOMER bị chặn ở `/admin`. Báo rõ phần nào không chạy được.

## Rủi ro và lưu ý

- Tạo KP qua API giờ bắt buộc `canDo`, và PUT KP active không được xóa CEFR hay can-do (thay đổi hành vi đã làm trong phase 01). V73 chuyển KP active thiếu CEFR/can-do sang INACTIVE. Nêu rõ khi báo cáo.
- DB local của người dùng còn lệch checksum V71 (lỗi Flyway `Migration checksum mismatch for migration version 71`: DB đã áp một bản V71 khác bản trên `main`). V73 trở đi chỉ chạy được sau khi xử lý lỗi đó (tạo lại `content_db`, hoặc `repair` sau khi so schema). Không sửa V71.
- Can-do, CEFR, KP Speaking và KP nền trong V73 là bản nháp AI soạn, cần chuyên gia IELTS duyệt trước khi chuyển ACTIVE.
- Bản sửa lọc bài ôn ở frontend (`httpLearningApi.ts`, chỉ giữ bài ôn `blocking`) đang nằm trong `stash@{0}` (`review-badge-required-only`) của repo `frontend`, không thuộc plan này; lấy lại bằng `git stash pop` khi cần.
- Chưa quyết: có gỡ liên kết từ vựng khi KP bị deactivate hay giữ (thuộc backlog liên kết từ vựng-KP).

## Câu hỏi chưa chốt

- Có commit phase 01 ngay không, và tách commit thế nào (đề xuất: một commit riêng cho đồ thị tri thức).
