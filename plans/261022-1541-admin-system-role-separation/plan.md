---
status: ready
created: 2026-10-22
repos: backend, frontend
branch: feat/admin-system-role (tạo mới ở cả hai repo, tách từ nhánh đang chứa khu quản trị)
---

# Tách vai trò ADMIN (quản trị hệ thống) khỏi CONTENT_AUTHOR (nghiệp vụ IELTS)

## Mục tiêu

- **ADMIN chỉ quản trị hệ thống**: quản lý tài khoản và vai trò, theo dõi các microservice, xem số liệu hoạt động
  người dùng (DAU/WAU/MAU, retention), quản lý gói/khóa truy cập. ADMIN **không** được đọc hay sửa nội dung IELTS,
  cây tri thức, câu hỏi, bài học, rubric, chấm bài, cấu hình học tập.
- **CONTENT_AUTHOR (R&D) sở hữu toàn bộ nghiệp vụ IELTS và cây tri thức**: KP, tiên quyết, course/topic, bài học,
  câu hỏi, gói đề, rubric, từ vựng, video học, cấu hình mastery, chất lượng AI chấm.
- **EXAMINER (Mentor)** giữ nguyên quyền chấm bài, hàng việc EXAMINER, đọc cây tri thức.
- Khu `/admin` ở frontend dùng chung một shell nhưng menu và trang chủ tách theo vai trò.

## Quyết định đã chốt (người dùng xác nhận trước khi giao Codex)

| # | Quyết định | Ghi chú |
|---|---|---|
| D1 | Bỏ `ADMIN` khỏi mọi endpoint nghiệp vụ. Endpoint chỉ có `ADMIN` và là nghiệp vụ thì chuyển sang `CONTENT_AUTHOR` (hoặc `EXAMINER`) theo bảng phase 01. | Không thêm vai trò mới. |
| D2 | `ADMIN` loại trừ `CONTENT_AUTHOR` và `EXAMINER` trên cùng một tài khoản (`422 ROLE_CONFLICT`). `ADMIN` + `CUSTOMER` vẫn được. | Phân tách nhiệm vụ: admin không tự cấp quyền nghiệp vụ cho mình. |
| D3 | ADMIN không được tự gỡ vai trò ADMIN của mình, tự khóa, tự xóa; không được làm mất ADMIN đang hoạt động cuối cùng. | Tránh tự khóa hệ thống. |
| D4 | Theo dõi microservice làm trong `api-gateway` (đã có Eureka client và WebFlux), không thêm service mới. | Chỉ đọc trạng thái, không khởi động/dừng service. |
| D5 | Không có thư viện biểu đồ: biểu đồ hoạt động vẽ bằng CSS/SVG đơn giản. | Không thêm dependency frontend. |

Câu hỏi mở (không chặn, Codex giữ mặc định dưới đây và ghi lại trong báo cáo):
- Huy hiệu (`PATCH /api/learning/admin/badges/{code}`): mặc định chuyển sang `CONTENT_AUTHOR` (là cấu hình học tập).
- AI evaluation (`/api/assessments/admin/ai-*`) và examiner quality: mặc định chuyển sang `CONTENT_AUTHOR` (R&D sở hữu chất lượng chấm). Nếu mentor muốn examiner quality thuộc EXAMINER thì chỉ đổi annotation.
- Quản lý gói/khóa/điểm (`/api/access/admin/**`): giữ `ADMIN` (vận hành thương mại, không phải nghiệp vụ IELTS).

## Phases

| Phase | Repo | Nội dung | Phụ thuộc | Trạng thái |
|---|---|---|---|---|
| [01](phase-01-business-endpoints-without-admin.md) | backend | Bỏ ADMIN khỏi endpoint nghiệp vụ, sửa test phân quyền | - | todo |
| [02](phase-02-user-role-rules.md) | backend (user-service) | Luật vai trò: loại trừ, tự bảo vệ, admin cuối | - | todo |
| [03](phase-03-service-monitoring-api.md) | backend (api-gateway) | API trạng thái microservice | - | todo |
| [04](phase-04-admin-shell-by-role.md) | frontend | Menu, route, trang chủ theo vai trò | 01 | todo |
| [05](phase-05-user-management-screen.md) | frontend | Màn quản lý người dùng | 02 | todo |
| [06](phase-06-system-monitoring-screens.md) | frontend | Màn dịch vụ, màn hoạt động, tổng quan hệ thống | 03 | todo |

01, 02, 03 độc lập, làm song song được. 04 làm ngay sau 01 (chỉ cần biết ai được gọi gì). 05 sau 02, 06 sau 03.

## Quy tắc chung cho Codex

- Đọc `backend/AGENTS.md`, `backend/CLAUDE.md`, `frontend/AGENTS.md` trước khi sửa.
- Không sửa migration đã áp dụng; migration mới dùng số kế tiếp của từng service.
- Không ghi mã phase/plan vào comment, tên test, commit. Conventional commit, không nhắc AI. Chưa commit khi người dùng chưa đồng ý.
- Mọi thay đổi API cập nhật contract trong `backend/docs/contracts/` cùng lúc.
- Frontend: không thêm dependency; dùng token `--adm-*` và các class `adm-*` có sẵn trong `src/features/admin/admin.css`
  (giao diện sáng); mỗi màn dùng `AdminPage` (`src/features/admin/pages/AdminPage.tsx`) và `useAdminResource`.
- Test không gọi dịch vụ ngoài thật.

## Tiêu chí hoàn thành chung

- `mvn -q test` xanh cho các service bị sửa (content, learning, library, assessment, user, api-gateway). Báo số test, số lỗi.
- `npm run typecheck`, `npm run lint`, `npm test` xanh ở frontend.
- Kiểm tra tay: tài khoản ADMIN vào `/admin` chỉ thấy Tổng quan hệ thống, Người dùng, Dịch vụ, Hoạt động; gọi
  `GET /api/content/admin/overview` bằng token ADMIN nhận `403`. Tài khoản CONTENT_AUTHOR thấy 4 mục nội dung và không
  thấy mục hệ thống.

## Rủi ro

- Tài khoản dev đang có `ADMIN` (ví dụ `admin@gmail.com`) sẽ mất quyền nội dung. Cần một tài khoản CONTENT_AUTHOR riêng
  để thử nội dung (tạo qua màn Người dùng sau phase 05, hoặc SQL tay ở môi trường dev).
- Nếu có dịch vụ gọi dịch vụ khác bằng token mang vai trò ADMIN, bỏ ADMIN sẽ làm vỡ luồng đó. Phase 01 bắt buộc kiểm tra.
