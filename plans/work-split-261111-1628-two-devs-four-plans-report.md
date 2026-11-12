# Chia việc cho 2 dev: bốn plan làm song song

- Ngày: 2026-11-11
- Áp dụng cho repo `backend/` và `frontend/`
- Nguồn đúng về phạm vi và cách làm là các plan bên dưới; file này chỉ nói **ai làm gì, theo thứ tự nào, tránh đụng nhau ra sao**.

## 1. Bốn plan

| Mã | Plan | Nội dung | Số phase |
| --- | --- | --- | --- |
| **1102** | [Nâng cấp chấm Writing](../writing-grading-upgrade-plan/261111-1102-writing-grading-upgrade/plan.md) | Bắt đủ lỗi, kiểm số liệu Task 1, kiểm yêu cầu đề Task 2, Top 3 vấn đề, thư viện LLM dùng chung | 0, 0b, 1–8 |
| **1557** | [Theo dõi tiến bộ Writing](../261111-1557-writing-progress-tracking/plan.md) | API và màn "Tiến bộ Writing" qua nhiều bài | 1–2 |
| **1620** | [Gia cố nền tảng](../261111-1620-platform-hardening/plan.md) | Bỏ secret dự phòng, dọn outbox chết, ArchUnit ở CI, chỗ lệch nhỏ | 1–4 |
| **1640** | [Deploy production](../261111-1640-production-deployment/plan.md) | Image lên GHCR, compose production, secret theo service, deploy, sao lưu, frontend lên CDN | 1–5 |

## 2. Phân công

| | **Dev A – Writing/AI** | **Dev B – Nền tảng** |
| --- | --- | --- |
| Plan | 1102 (**trừ phase 0b**), 1557 | **1102 phase 0b**, 1620, 1640 |
| Phạm vi chính | learning, assessment (eval), content (danh mục lỗi), frontend học viên và admin AI evaluation | `shared/common-llm`, root/service `pom.xml`, config-repo (secret), game, `.github/workflows`, Docker, compose, server |

Phase 0b của 1102 giao cho Dev B vì nó tạo module shared, sửa pom ba service, chuyển template vào `docs/contracts` và ảnh
hưởng Docker build: đều là việc nền tảng.

## 3. Thứ tự theo đợt

| Đợt | Dev A | Dev B | Điều kiện |
| --- | --- | --- | --- |
| **1** | 1102-0 (spike đo thời gian, cần khóa LLM thật) → 1102-2 (sửa nhỏ Learning) → 1557-1 (API tiến bộ) | 1102-0b (common-llm, template một nguồn) → 1620-1 (bỏ secret dự phòng) | 1102-2 làm trước 0b được: **không** tự tạo timer LLM, lấy metric từ thư viện sau |
| **2** | 1102-1 (eval độ bao phủ lỗi, form bài chuẩn) → 1102-3 (danh mục lỗi Writing) | 1620-2 (dọn outbox) → 1620-3 (ArchUnit) | 1102-1 và 1102-3 **chờ 0b merge** (cùng sửa `OpenAiCompatibleEssayGrader`). 1620-3 chạy sau 0b merge |
| **3** | 1102-4 (prompt v2) → 1102-5 và 1102-8 (kiểm số liệu Task 1, kiểm yêu cầu đề Task 2) → 1102-6 (luật trần band) → 1102-7 (response và giao diện kết quả) | 1640-1 (image) → 1640-2 (compose production, cần máy thử) → 1640-3 (secret theo service) | 1102-4 cần template v1 đã nằm ở `docs/contracts` (0b) |
| **4** | 1557-2 (màn tiến bộ) | 1620-4 (chỗ lệch nhỏ, tài liệu) → 1640-4 (deploy, sao lưu, quay lui) → 1640-5 (frontend lên CDN) | 1640-4/5 cần VPS và tên miền; 1620-1 phải merge trước lần deploy đầu |

**Khi bị chặn:**
- Dev A chờ examiner hoặc chuyên gia (bộ bài chuẩn, duyệt template, duyệt mã lỗi, kiểm tay): chuyển sang 1557.
- Dev B chờ VPS hoặc tên miền: làm 1640-1 đến 3 trước.

## 4. Chỗ dễ đụng nhau

| File / chỗ | Ai | Quy tắc |
| --- | --- | --- |
| **Migration Content** | A (1102-3), B (1620-2) | **Giữ số:** B dùng **V124** (đã có trên nhánh `chore/drop-dead-outbox`), A dùng **V125 trở đi**. Không dùng V123: nếu V124 đã được áp mà V123 xuất hiện sau, Flyway báo lỗi thứ tự. Kiểm lại số trước khi merge |
| Migration Learning | A (1557-1) | Chỉ A |
| Migration Assessment V23 | A (1102-1) | Chỉ A |
| Migration Game | B (1620-2) | Chỉ B |
| Root `pom.xml`, `pom.xml` các service | B (0b, ArchUnit) | A không thêm dependency; cần thì nhờ B |
| `OpenAiCompatibleEssayGrader` (Assessment) | B (0b), A (1102-1, 1102-3) | A bắt đầu sau khi 0b merge |
| `OpenAiCompatibleLlmClient` (Learning) | B (0b) | Sửa client cho model reasoning đã commit trên nhánh `fix/grade-writting-llm`; 0b dựa trên đó |
| Config-repo `learning/assessment/content-service.yaml` | B (secret, khóa LLM), A (`learning.writing.*`) | Sửa khác dòng; rebase trước khi merge |
| `docs/contracts/` | B (template v1 vào đây), A (template v2, contract Writing) | 0b trước 1102-4 |
| `AGENTS.md`, `CLAUDE.md`, `docs/system-architecture.md` | Cả hai | Mỗi PR chỉ sửa đúng mục của mình; rebase trước khi merge; ai merge sau thì gộp dòng ngày đầu file |
| Luật ArchUnit | B viết, code của A phải qua | Luật tính `common-llm` là lớp được phép gọi HTTP. A chạy `mvn -B -ntp verify` trước khi mở PR |
| Frontend | A: `features/admin/ai-evaluation/**`, `features/practice/writing/**`, trang báo cáo. B: `usePracticeEssayGradeChoice.tsx`, `features/practice/speaking/**` (bỏ giá cứng) | Không trùng file; nếu cần sửa file của người kia thì báo trước |
| `.github/workflows/` | B | Chỉ B |

## 5. Nhánh và merge

- Mỗi phase một nhánh tách từ `main`, một PR nhỏ. Ví dụ:
  - A: `feat/writing-eval-recall`, `feat/writing-error-catalog`, `feat/writing-prompt-v2`, `feat/writing-progress`;
  - B: `feat/common-llm`, `chore/remove-secret-fallbacks`, `chore/drop-dead-outbox`, `test/architecture-rules`,
    `chore/production-deployment`.
- Không làm chung một nhánh lớn. Nhánh `fix/grade-writting-llm` (đã push, 2026-11-11) chứa: sửa client LLM cho model
  reasoning, timeout Gateway của route chấm bài, migration Content **V122** (rubric Writing band dưới 4.0), các plan.
  Merge nhánh này vào `main` trước, rồi mọi nhánh phase mới tách từ `main`.
- **Thứ tự merge bắt buộc:**
  1. 0b trước 1102-1, 1102-3, 1102-4 và 1620-3;
  2. 1620-1 trước 1640-4 (deploy đầu tiên).
- Commit theo Conventional Commits; không ghi số phase, mã plan, nhắc AI trong commit, comment, tên test, tên migration
  (`AGENTS.md` §6).
- Xong phase: cập nhật `status` trong phase file và bảng Phases của plan; viết báo cáo ngắn vào thư mục `reports/` của plan đó.

## 6. Việc cần người ngoài dev làm sớm

| Việc | Ai | Cần trước |
| --- | --- | --- |
| Khóa LLM thật cho spike | Người điều phối | Đợt 1 (1102-0) |
| Bộ 5–10 bài chuẩn có đánh dấu lỗi (Task 1 và Task 2) | Examiner | Đợt 2 (1102-1, nhập số nền) |
| Duyệt mã lỗi mới, kể cả mã Task 2 | Chuyên gia | Đợt 2 (1102-3, seed mã) |
| Duyệt câu chữ template v2 | Người dùng + examiner | Đợt 3 (1102-4/5/8) |
| Duyệt ngưỡng luật trần band | Examiner | Đợt 3 (1102-6, trước khi bật) |
| Kiểm tay 10 bài Task 1 và 10 bài Task 2 | Examiner | Cuối đợt 3 |
| Kích hoạt prompt v2 | Admin | Sau khi eval đạt |
| Mua VPS (≥ 8 GB RAM, 4 vCPU, SSD ≥ 80 GB) và tên miền riêng (`app.` + `api.`) | Người điều phối | Đợt 3 (1640-2 máy thử), đợt 4 (deploy) |
| Chọn nơi lưu bản sao lưu DB ngoài VPS | Người điều phối | Đợt 4 (1640-4) |

## 7. Còn chờ chốt

- Tên miền production (bắt buộc dùng tên miền riêng, không dùng `*.vercel.app` / `*.pages.dev`).
- Reverse proxy: đề xuất Caddy.
- Nơi lưu bản sao lưu DB: đề xuất bucket riêng trên Supabase.
