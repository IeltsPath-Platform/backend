---
title: "Khu soạn nội dung (CONTENT_AUTHOR): soạn tay đề/gói và đề cuối, slide, khóa học, rubric, mã lỗi; AI sinh câu hỏi/course; import Excel"
description: "Lượt hai sau rd-admin-area, gộp cả plan 261016-0804: API và màn soạn gói đề (kể cả đề cuối topic/course), màn cho các API author đã có, rồi AI sinh câu hỏi/course, dựng course từ cây tri thức và import Excel"
status: in-progress
priority: P1
branch: "claude (backend): feat/content-package-authoring (01), feat/review-work-queue-api (14), mỗi phase 07–12 một nhánh feat/content-*; codex (frontend): feat/content-author-screens (03–06), feat/content-package-screens (02), feat/content-review-screens (15), feat/content-ai-screens (13)"
tags: [content-service, frontend, admin, content-package, rubric, course, ai, llm, course-builder, excel-import]
blockedBy: []
blocks: []
created: "2026-10-23T00:50:00.000+07:00"
createdBy: "Claude Code"
source: "đối chiếu endpoint @PreAuthorize CONTENT_AUTHOR trong content-service với API frontend /content-author đang gọi (main backend 55234413, frontend b1814e3)"
---

# Hoàn thiện khu soạn nội dung

Repo: `backend` và `frontend`. Hai agent làm song song trên cùng plan: **Claude Code làm toàn bộ backend** (phase 01, 07–12, 14), **Codex làm toàn bộ giao diện** (phase 02–06, 13, 15). Xem mục "Phân công và phối hợp". (Đổi phân công ngày 2026-10-23; trước đó Codex làm phase 01.)

Plan này nối tiếp [`261022-1200-rd-admin-area`](../261022-1200-rd-admin-area/plan.md) (lượt đầu: Tổng quan, Cây tri thức, Bài học, Ngân hàng câu hỏi; đã có trên `main`). Không làm lại gì ở lượt đó.

**Từ 2026-10-23 plan này gộp luôn [`261016-0804-ai-content-and-course-builder`](../261016-0804-ai-content-and-course-builder/plan.md)** (AI sinh câu hỏi/course, dựng course từ cây tri thức, import Excel; khớp các ô "CONTENT_AUTHOR · plan 2" trên board FigJam "Cây tri thức – đánh giá"). Phần đó thành phase 07–13 dưới đây; plan 0804 chỉ còn trỏ về đây. Một plan cho cả khu soạn nội dung: **soạn tay (01–06)** và **AI/Excel (07–13)** cùng ghi vào gói đề, câu hỏi, section, nên đi chung luật và thứ tự.

## Context: khảo sát ngày 2026-10-23

Frontend khu `/content-author` chỉ gọi: `/api/content/knowledge-tree*`, `/api/content/courses` (đọc), `/api/content/topics` (đọc), `/api/content/admin/lessons…`, `/api/content/admin/questions…`, `/api/content/admin/overview`, `/api/learning/admin/{question-stats,work-items}`.

Backend có API cho `CONTENT_AUTHOR` nhưng chưa có màn hình:

| Nhóm | API | Đủ để làm UI? |
|---|---|---|
| Gói đề (`PRACTICE_SET`, `TOPIC_TEST`, `MOCK_TEST`, `PLACEMENT_TEST`, `PRACTICE_TEST`, `QUIZ`) | `ContentPackageController` `/api/content/packages`: list, detail, tạo gói, thêm version, thêm section, publish; `ContentPackageAdminController` `/api/content/admin/packages`: preview, schedule, cancel, list schedules; `ContentAssetController` `/api/content/assets`: tạo asset, link asset | **Không.** Xem 4 lỗ hổng dưới |
| Slide và tài liệu của bài học | `LessonResourceAdminController` `/api/content/admin/lessons/{id}/resources` (CRUD) | Đủ |
| Khóa học | `POST/PUT /api/content/admin/courses` | Đủ |
| Chủ đề | `POST/PUT /api/content/admin/topics` (`TopicController`, path kép) | Đủ |
| Rubric chấm Writing | `GET/POST/PUT /api/content/admin/rubrics`, `POST …/{code}/{version}/activate`, `GET /api/content/rubrics?task=` | Đủ |
| Mã lỗi | `GET/POST/PUT /api/content/error-types` | Đủ |

Bốn lỗ hổng của API gói đề (đã kiểm trong code):

1. **Không gắn được câu hỏi vào section.** `section_questions` chỉ được ghi bằng SQL seed (24 migration có `INSERT INTO section_questions`); Java chỉ đọc (`SectionQuestion` không có use case ghi).
2. **Không chọn được version.** `GET /api/content/packages/{id}` trả version đã xuất bản, nếu không có thì version mới nhất, nhưng **không trả `versionId`, `status` của version, hay danh sách version**. Frontend không biết bản nháp nào để sửa, publish hay schedule.
3. **Thêm section không kiểm version còn `DRAFT`** (`AddContentSectionUseCase` không kiểm status). Gọi vào version đã xuất bản sẽ sửa đề học viên đang làm.
4. **Không thay hoặc gỡ được đoạn văn/audio của section.** Preview và học viên lấy đoạn văn = asset `PASSAGE` đầu tiên link vào section, audio = asset `AUDIO` đầu tiên (`JdbcLearningContentReader.java` ~dòng 760). Có `POST /api/content/assets` và `POST /api/content/assets/links` nhưng không có gỡ link, nên chỉ thêm được, không thay được.

## Quyết định đã chốt với người dùng (2026-10-23)

- Làm hết 6 nhóm, theo thứ tự ưu tiên: gói đề → slide bài học → khóa học/chủ đề → rubric → mã lỗi.
- API mới cho gói đề đặt dưới `/api/content/admin/packages/**` (quy ước admin hiện có). Không đổi hay xóa endpoint cũ ở `/api/content/packages`.
- **Audio: upload file từ màn soạn.** Backend (phase 01) thêm endpoint upload audio cho content-service, dùng `shared/common-storage` (mẫu: `library-service` `UploadFlashcardImageUseCase` + `FlashcardImageStorageAdapter`), lưu vào bucket `media` (bucket public mà `CONTENT_MEDIA_BASE_URL` đang trỏ tới). Vẫn cho dán key có sẵn (file seed cũ) như đường phụ. Chi tiết ở phase 01 mục 6 và phase 02.
- **Chỉ `CONTENT_AUTHOR`** thấy và dùng các màn mới. EXAMINER không thấy màn nào trong plan này.
- **Tên menu:** "Đề & gói câu hỏi", "Khóa học & chủ đề", "Rubric chấm Writing", "Mã lỗi".
- Phần tách khu `/content-author` và trang Tổng quan dạng biểu đồ là điều kiện tiên quyết của mọi phase frontend. Claude Code đã commit phần này trên nhánh `feat/content-author-screens` (worktree `../frontend-claude`, commit `76608bf`, `81242c5`). Sau khi đổi phân công, **Codex** mở PR cho nhánh đó (nếu chưa có) và người dùng merge.
- Không soạn `section_question_groups` (nhóm câu theo dạng đề) trong plan này; câu gắn vào section có `group_id = null`.
- Không làm nhập CSV/YAML (nhập Excel làm ở phase 12), không làm xóa gói. Đưa vào backlog.

### Quyết định bổ sung khi gộp (người dùng chốt 2026-10-23)

- **Cách gộp:** giữ nguyên phase 01–06 và đường dẫn plan này; phần của plan 0804 thành phase 07–13.
- **Mở tạo đề cuối `TOPIC_TEST` và `COURSE_TEST`** cho cả màn soạn tay lẫn công cụ (Excel, duyệt nháp course). Tạo ở `DRAFT`, bắt buộc gắn đúng topic hoặc course, qua route mới `POST /api/content/admin/packages` (phase 01 mục 7). Route cũ `POST /api/content/packages` giữ lệnh cấm như hiện tại.
- **`PRACTICE_TEST` dùng câu `EXAM`**, đúng như `PublishContentPackageUseCase` đang kiểm. `sourceCode` tùy chọn: không có là đề kiểm tra nội bộ của course (không hiện trong thư viện đề).
- **Phạm vi AI/Excel:** đủ bốn khối: AI sinh câu hỏi (08–09), dựng course từ cây (10), AI sinh course từ yêu cầu (11), import Excel (12).
- **Phân công (sửa lại cùng ngày):** **Claude Code làm toàn bộ backend** (01, 07–12, 14), **Codex làm toàn bộ giao diện** (02–06, 13, 15). Claude Code tiếp quản phase 01 từ chỗ Codex dừng (nhánh `feat/content-package-authoring`, contract đang viết dở, port `ContentAudioStorage` chưa commit). Codex tiếp quản worktree `../frontend-claude`.

### Quyết định bổ sung: ba màn còn thiếu so với board (người dùng chốt 2026-10-23)

Board giao cho `CONTENT_AUTHOR` "đánh giá Mentor", "đánh giá AI bằng gold set" và hàng việc theo vai trò. Backend đã có cả ba, nhưng chưa có màn hình (khu author chỉ hiện 8 việc chỉ-xem ở trang Tổng quan). Thêm:

- **Phase 14 (Claude Code, backend):** bổ sung các API đọc còn thiếu để ba màn dùng được: danh sách bài QA mình đã nhận, tên giám khảo trong bảng chất lượng, danh sách bộ bài chuẩn / lượt đánh giá / bản prompt đang kích hoạt, tên KP hoặc câu hỏi trong việc cần làm.
- **Phase 15 (Codex, frontend):** ba màn "Việc cần làm của R&D", "Đánh giá giám khảo", "Đánh giá AI chấm Writing".
- Màn đánh giá AI làm sau cùng trong phase 15: chỉ hữu ích khi đã có bộ bài chuẩn do chuyên gia IELTS chấm (chưa có; rubric hiện là bản nháp AI soạn). Hiện chưa có bản prompt nào được kích hoạt, nên AI đang chấm học viên bằng rubric `ACTIVE` chưa kiểm định; màn phải nói rõ điều này.
- Lấy tên giám khảo: assessment gọi `POST /internal/users/display-names` của user-service (cùng mẫu learning đang dùng), lỗi thì vẫn trả bảng, tên để trống.
- **Giữ các quyết định của plan 0804:** AI chỉ ghi vào bản nháp, duyệt xong mới tạo thực thể thật ở `DRAFT`/`INACTIVE` và vẫn phải xuất bản thủ công; sinh course chỉ chọn từ kho, không bịa câu (KP thiếu câu thì liệt kê `gaps`); việc AI chạy nền, API trả `202`; hạn mức 20 việc/người/ngày, tối đa 20 câu/việc (cấu hình được); biến `CONTENT_LLM_BASE_URL`, `CONTENT_LLM_API_KEY`, `CONTENT_LLM_MODEL`; Apache POI để đọc Excel (duyệt 2026-10-16); LLM sau port để sau này tách `ai-service` chỉ đổi adapter; không thêm service, không thêm role; repo public nên prompt yêu cầu tự viết ngữ liệu, không chép đề thương mại, người duyệt chịu trách nhiệm.
- **Role:** mọi route và màn mới chỉ `CONTENT_AUTHOR`. Không `ADMIN` (đã tách khỏi nghiệp vụ IELTS, plan `261022-1541-admin-system-role-separation`).

## Quy tắc cả hai agent phải theo

- Đọc `backend/AGENTS.md`, `backend/CLAUDE.md`, `frontend/AGENTS.md`, `frontend/README.md` trước khi sửa. Backend giữ layer `api → application → domain`, `infrastructure → domain`; domain không import Spring/JPA.
- **Viết contract trước, code sau.** Mọi API mới hoặc đổi hành vi ghi vào `backend/docs/contracts/` trong cùng commit với code.
- Không sửa migration đã áp dụng. V73 là cao nhất trên `main`. Các phase backend lấy số kế tiếp **theo thứ tự merge** (01 nếu cần → 07 → 08 → …); kiểm lại số trên `origin/main` ngay trước khi tạo.
- Không log prompt, nội dung AI sinh ra, khóa API hay nội dung ô Excel; log chỉ mang id, loại việc, mã lỗi. Test không gọi LLM thật (port giả hoặc `MockRestServiceServer`).
- Không ghi mã phase, mã plan, nhãn audit vào comment, tên test, tên migration, commit. Commit theo conventional commit, không nhắc AI.
- Không log nội dung câu hỏi, đoạn văn, transcript. Test không gọi LLM thật.
- Frontend: **không thêm dependency.** Dùng lại `apiRequest` (`src/lib/httpClient.ts`), `useAdminResource`, `AdminPage`, `AdminState` (`AdminLoading`/`AdminError`/`AdminEmpty`), `adminErrorMessage`/`adminErrorCode` (`src/features/admin/lib/adminErrors.ts`), class CSS `adm-*` trong `src/features/admin/admin.css`, icon `lucide-react`, schema Zod parse mọi response. Form viết dạng controlled như `QuestionBankPage.tsx`/`LessonEditorPage.tsx` (repo không dùng react-hook-form ở khu admin). Mobile-first, đủ trạng thái loading/lỗi/rỗng, label cho mọi input, nút bị khóa khi đang gửi.
- Route mới chỉ thêm trong nhánh `isAuthorArea` của `src/features/admin/AdminRoutes.tsx`; menu thêm vào `ADMIN_MENU` trong `src/features/admin/adminMenu.ts` với `roles: ['CONTENT_AUTHOR']`. Không thêm route legacy `/admin/...` cho màn mới.
- Không đụng các file đang sửa dở của người dùng ngoài phạm vi phase (khi bắt đầu, chạy `git status` trong `frontend`; nếu còn thay đổi chưa commit thì dừng và hỏi).

## Phases

Trạng thái từng phase ghi **trong file phase** (dòng `Trạng thái:` ở đầu), không sửa bảng này, để hai agent không xung đột ở `plan.md`.

| Phase | Người làm | Repo | Nội dung | Phụ thuộc |
|---|---|---|---|---|
| [01](./phase-01-package-authoring-api.md) | Claude Code | backend | API soạn gói đề: chi tiết theo version, chặn sửa bản đã xuất bản, sửa/xóa section, gắn câu hỏi, đoạn văn/audio, **tạo gói kể cả đề cuối topic/course** | không |
| [02](./phase-02-package-screens.md) | Codex | frontend | Màn "Đề & gói câu hỏi": danh sách, tạo (kể cả đề cuối), soạn version, xem trước, xuất bản, lên lịch | 01 |
| [03](./phase-03-lesson-slides-documents.md) | Codex | frontend | Slide và tài liệu trong trình soạn bài học | không |
| [04](./phase-04-courses-topics-screen.md) | Codex | frontend | Màn "Khóa học & chủ đề" | không |
| [05](./phase-05-rubrics-screen.md) | Codex | frontend | Màn "Rubric chấm Writing" | không |
| [06](./phase-06-error-types-screen.md) | Codex | frontend | Màn "Mã lỗi" | không |
| [07](./phase-07-question-import-ref.md) | Claude Code | backend | Mã tham chiếu câu hỏi `import_ref` | 01 đã merge (số migration) |
| [08](./phase-08-ai-draft-infrastructure.md) | Claude Code | backend | Hạ tầng bản nháp, job nền, hạn mức, port và adapter LLM | 01 đã merge (cấu hình chung) |
| [09](./phase-09-ai-question-drafts.md) | Claude Code | backend | AI sinh câu hỏi theo KP, sửa, duyệt thành câu `DRAFT` | 07, 08 |
| [10](./phase-10-course-builder-from-tree.md) | Claude Code | backend | Dựng course từ cây tri thức: bản nháp course, kiểm phủ KP, duyệt thành course thật | 01, 07, 08 |
| [11](./phase-11-ai-course-drafts.md) | Claude Code | backend | AI sinh course từ yêu cầu ("6.5, chủ đề A, B, C"), chỉ chọn từ kho | 08, 09, 10 |
| [12](./phase-12-excel-import.md) | Claude Code | backend | Import Excel theo template: tải template, kiểm tra trước, ghi tất cả hoặc không | 01, 07, 10 |
| [13](./phase-13-ai-and-import-screens.md) | Codex | frontend | Màn bản nháp AI, sinh câu hỏi, dựng/sinh course, import Excel | API tương ứng của 08–12 đã merge |
| [14](./phase-14-review-and-work-queue-api.md) | Claude Code | backend | API đọc bổ sung cho hàng việc R&D, đánh giá giám khảo, đánh giá AI (assessment, learning, content) | không |
| [15](./phase-15-review-and-work-queue-screens.md) | Codex | frontend | Màn "Việc cần làm của R&D", "Đánh giá giám khảo", "Đánh giá AI chấm Writing" | 14 |

```text
Claude Code (backend):  01 ──> 14 ──> 07 ──> 08 ──> 09 ──> 10 ──> 11 ──> 12
Codex (frontend):       03 ──> 04 ──> 05 ──> 06 ──> 02 ──> 15 ──> 13
                        (02 chen lên làm ngay khi 01 đã merge; 15 khi 14 đã merge; 13 làm từng màn khi API của nó đã merge)
```

Phase 14 nhỏ và không đụng content-service nhiều, nên làm ngay sau 01 để Codex có việc ở phase 15 trong lúc backend AI chạy.

Phần cây tri thức mà các phase AI cần (cây KP và tiên quyết, độ khó câu, danh mục mã lỗi) đã có trên `main`
(`knowledge-tree-v1.md`, `/api/content/error-types`); không chờ plan `261016-0131` đóng hẳn.

## Phân công và phối hợp

**Điều kiện trước khi bắt đầu phase frontend:** nhánh `feat/content-author-screens` (worktree `../frontend-claude`, đã có commit tách khu `/content-author`: `App.tsx`, `AdminRoutes.tsx`, `adminMenu.ts`, `AuthGuards.tsx`, `staffRoles.ts`…, và trang Tổng quan dạng biểu đồ trong `features/admin/overview/`) được mở PR và người dùng merge vào `main`. Các phase frontend đều dựa trên route `/content-author/*` đó. Chưa có trên `origin/main` thì Codex dừng. Backend không phụ thuộc điều kiện này.

| | Claude Code (backend) | Codex (frontend) |
|---|---|---|
| Phase | 01, 14, 07, 08, 09, 10, 11, 12 | 03, 04, 05, 06, 02, 15, 13 |
| Nhánh | `feat/content-package-authoring` (01, tiếp nhánh Codex đã tạo); 14: `feat/review-work-queue-api`; 07–12: mỗi phase một nhánh `feat/content-<chủ đề>` tạo từ `origin/main` sau khi phase trước đã merge | `feat/content-author-screens` (03–06, nhánh đã có commit tiền đề), `feat/content-package-screens` (02), `feat/content-review-screens` (15), `feat/content-ai-screens` (13); mỗi nhánh tạo từ `origin/main` mới nhất |
| Thư mục làm việc | `backend/` (clone chính) | worktree `../frontend-claude` (tiếp quản, giữ tên thư mục để không phá worktree); clone chính `frontend/` để nguyên cho người dùng |
| Thư mục tính năng | `backend/services/{content,assessment,learning}-service/**` trong phạm vi phase, `backend/docs/contracts/**` | `frontend/src/features/admin/{packages,courses,rubrics,error-types,ai-drafts,course-drafts,course-import,work-queue,examiner-review,ai-evaluation}/**`, `frontend/src/features/admin/lessons/components/LessonResourcesEditor*` |

Backend và frontend ở hai repo khác nhau nên hai agent không đụng file của nhau. Khi bắt đầu phase 01, Claude Code chạy `git status` trong `backend/`: phải thấy đúng các file Codex để lại (`docs/contracts/content-package-authoring-v1.md`, `application/port/ContentAudioStorage.java`, thư mục plan, các xóa file plan 0804, `services/.gitkeep` bị xóa sẵn); đọc chúng rồi làm tiếp, không xóa. Thay đổi của plan commit riêng (`docs(plans): …`), không gộp vào commit tính năng.

File dùng chung và quy tắc:

| File | Ai sửa | Quy tắc |
|---|---|---|
| `src/features/admin/adminMenu.ts`, `src/features/admin/AdminRoutes.tsx` | Codex, nhiều nhánh | Mỗi nhánh chỉ **thêm** dòng cho màn của mình. Trước khi mở PR: `git fetch && git rebase origin/main`; xung đột ở hai file này giải bằng cách giữ cả hai phía. |
| Test menu (`adminMenu.test.ts`) | Codex | Mỗi nhánh thêm `it(...)` riêng cho mục của mình. |
| `src/features/admin/lessons/api/lessonApi.ts`, `lessons/types.ts`, `LessonEditorPage.tsx`, `LessonsPage.tsx` | Codex (phase 03, 04) | Các nhánh khác chỉ **đọc/import** `fetchTopics`, `fetchLessons`. |
| `src/features/admin/lessons/components/QuestionChooser.tsx` | Codex (phase 02, thêm prop `purpose`) | Phase 13 dùng lại, không sửa thêm. |
| `src/features/admin/questions/components/KnowledgePointPicker.tsx` | Không ai sửa | Phase 06, 13 chỉ import. |
| `src/features/admin/overview/**` | Codex (phase 15) | Khối "Việc cần làm của R&D" trên Tổng quan thêm link "Xem tất cả" sang màn mới; không đổi phần khác. |
| Shared/config backend (`docker-compose.yml`, `config-repo/*.yaml`, `pom.xml`) | Claude Code | Chỉ sửa khi phase cần, ghi rõ trong PR (`AGENTS.md` hạn chế sửa như tác dụng phụ). |
| `src/features/admin/admin.css` | Hạn chế | Ưu tiên CSS riêng trong thư mục tính năng; nếu buộc phải thêm class chung `adm-*`, thêm ở cuối file và ghi trong PR. |
| `frontend/README.md` | Người merge **sau cùng** | Cập nhật danh sách màn `/content-author` một lần, cho cả hai phần. |
| File phase của mình | Người làm phase đó | Cập nhật dòng `Trạng thái:` và mục "Kết quả" khi xong. |

Bàn giao giữa hai bên:

- Codex chỉ dựa vào contract trong `backend/docs/contracts/` đã có trên `main`, không đoán API trước: phase 02 dùng `content-package-authoring-v1`; phase 13 dùng `ai-drafts-v1`, `course-drafts-v1`, `course-import-v1`, phần `importRef` của câu hỏi; phase 15 dùng các contract phase 14 cập nhật (`admin-area-v1`, `assessment-grading-v1`, `learning-staff-area-v1`).
- Phase 03–06 không cần API mới, làm ngay được.
- Phase 10–12 tạo gói, gắn câu vào section, tạo đề cuối **bằng các use case của phase 01**, không viết đường ghi thứ hai.
- Nếu một bên phát hiện cần sửa file thuộc bên kia: không sửa; ghi vào mục "Việc cho bên kia" ở cuối file phase của mình, báo người dùng.

PR: mỗi nhánh một PR riêng. Người dùng merge frontend tiền đề (`feat/content-author-screens` phần đã commit) trước; backend theo thứ tự 01 → 14 → 07 → … → 12; frontend mỗi nhánh sau khi API nó dùng đã merge (rebase trước khi merge).

## Việc cuối

Codex (sau nhánh frontend cuối): cập nhật `frontend/README.md` (danh sách màn khu author).

Claude Code (mỗi phase backend): `backend/services/<service>/README.md` khi thêm route hoặc biến môi trường; `backend/docs/system-architecture.md` nếu lệch.

Sau phase 12 (Claude Code):

- `backend/AGENTS.md`: bảng module thêm trách nhiệm mới của content (bản nháp AI, dựng course, import Excel); mục cấu hình thêm nhóm `content.ai.*` và biến `CONTENT_LLM_*`; ghi rõ nội dung AI luôn qua bản nháp và người duyệt.
- `backend/.sdd/database/DATABASE_V5.md`: `ai_drafts`, `ai_usage_daily`, `questions.import_ref`, mapping `content_packages.topic_id`.
- `backend/docs/ai-policy.md`: mục "AI sinh nội dung" (chỉ ra bản nháp, người duyệt, không tới học viên khi chưa duyệt, sinh course chỉ chọn từ kho).
- Kịch bản demo `backend/docs/demo/ai-content-demo.md` (bước ở mục "Kiểm chứng tổng").

## Kiểm chứng tổng

- Backend: `mvn -q -pl services/content-service -am test` (cần Docker cho Testcontainers; khoảng 10 phút); phase 14 chạy thêm `services/assessment-service` và `services/learning-service`. Không chạy `mvn clean` khi service đang chạy từ IDE.
- Frontend: `npm run typecheck`, `npm run lint`, `npm test`, `npm run build`.
- Thủ công với backend thật: đăng nhập `content@gmail.com` (role CONTENT_AUTHOR, đã có trong DB local của người dùng), làm checklist cuối mỗi phase. Tài khoản `admin@gmail.com` (ADMIN) phải **không** thấy các menu mới. Báo rõ bước nào không chạy được và vì sao.
- Kịch bản đầu-cuối sau phase 12 và 13 (trên Compose sạch `docker compose down -v` rồi `up`):
  1. Tạo gói `TOPIC_TEST` cho một topic, soạn, xuất bản; học viên thấy đề mới trong nhóm đề cuối của topic đó.
  2. Sinh 5 câu cho một KP; sửa một câu; duyệt; câu `DRAFT` xuất hiện ở Ngân hàng câu hỏi.
  3. Yêu cầu course "6.5, chủ đề Paraphrase và TFNG"; xem bản nháp, `gaps`, `unmatchedTopics`; duyệt thành course `INACTIVE` (đề cuối rỗng ở `DRAFT`); soạn đề cuối ở màn "Đề & gói câu hỏi".
  4. Tải template Excel, điền, kiểm tra (một dòng lỗi), sửa, import thành công.
  5. Kích hoạt course; học viên thấy course và học được tới đề cuối topic đầu tiên.
  Chạy bằng LLM giả nếu không có khóa thật, và ghi rõ trong báo cáo; không tuyên bố đã chạy LLM thật khi chưa chạy.

## Rủi ro và lưu ý

- Gói tạo bằng màn mới không có `section_question_groups`, nên không khớp bộ lọc dạng đề (`formats`) của thư viện đề luyện (`JdbcPracticeTestCatalogReader`). Học viên vẫn làm được đề; chỉ là không lọc theo dạng. Ghi rõ trong màn soạn.
- Publish kiểm `QUESTION_ALREADY_USED` (một câu chỉ thuộc một gói đã xuất bản), `QUESTION_PURPOSE_MISMATCH` (`MOCK_TEST` và `PRACTICE_TEST` cần câu `EXAM`, `PLACEMENT_TEST` cần `PLACEMENT`, loại khác cần `LEARNING`), `INVALID_PACKAGE_LESSON`. Màn phase 02 phải hiển thị đúng các mã này bằng tiếng Việt.
- `PRACTICE_TEST`: `sourceCode` tùy chọn; có thì phải thuộc `GET /api/content/practice-tests/sources`, không có thì là đề kiểm tra nội bộ của course. Publish còn đòi mọi câu tự chấm được (Reading/Listening dạng `CHOICE`/`FILL`) hoặc essay Writing có `passBand`; `COURSE_TEST` cũng vậy. Lỗi này đến dưới dạng `INVALID_CONTENT_STATE` kèm message, màn nên hiện message để tác giả biết sửa gì.
- Xuất bản `TOPIC_TEST`/`COURSE_TEST` vào topic/course đang có học viên đưa đề vào nhóm đề thi ngay (thi lại lấy mã đề khác). Bản `DRAFT` không lộ: Learning chỉ đọc gói `PUBLISHED` có `current_published_version_id` (đã kiểm `JdbcLearningContentReader`).
- Chất lượng chuyên môn của câu AI sinh không kiểm tự động được (đáp án có thể sai nghĩa). Người duyệt chịu trách nhiệm; màn duyệt hiện rõ đáp án và giải thích; mọi câu vẫn phải qua `PUBLISHED`.
- Job AI chạy chung tiến trình với API content: lô nhỏ (2 việc/chu kỳ), timeout gọi LLM 60 giây. Cần scale thì tách `ai-service` (ngoài phạm vi).
- File Excel là đầu vào không tin cậy: giới hạn kích thước, cấm công thức, tắt XXE, chặn zip bomb là bắt buộc (phase 12).
- DB local của người dùng có thể lệch checksum migration cũ (xem rủi ro trong plan `rd-admin-area`). Nếu content-service không khởi động được vì Flyway, dừng và báo, không sửa migration cũ.

- Upload audio cần `SUPABASE_URL` và `SUPABASE_SECRET_KEY` cho content-service. Thiếu cả hai thì `common-storage` dùng `UnavailableObjectStorage`: service vẫn chạy, upload trả `503`. Thiếu **một** trong hai thì service không khởi động (đã gặp ở library-service ngày 2026-10-22), nên khi thêm biến vào `docker-compose.yml` phải để cả hai cùng mặc định rỗng.
- Bucket `media` phải cho phép ghi bằng secret key; file upload mới nằm dưới prefix `content-audio/`, không đè file seed.

## Backlog (không làm trong plan này)

Upload ảnh (cover section, ảnh trong câu hỏi); soạn nhóm câu (`section_question_groups`, `format_code`); nhập CSV/YAML; xóa hoặc lưu trữ gói; nhân bản version cũ thành bản nháp mới; đổi topic/course của đề cuối sau khi tạo; dọn file audio tải lên mà không gắn.

Ngoài phạm vi (từ plan 0804): tách `ai-service`; AI viết bài lý thuyết dài, tạo audio Listening, sinh ảnh biểu đồ Writing Task 1; AI sinh câu `ESSAY`/`SPEAKING`; độ khó thích ứng; tự xuất bản không qua người duyệt.

## Câu hỏi chưa chốt

Không còn. Bốn câu ban đầu (audio, EXAMINER, tên menu, ai đưa phần `/content-author` lên main) và năm câu khi gộp (cách gộp, đề cuối, purpose của `PRACTICE_TEST`, phạm vi AI/Excel, phân công) đã chốt ở mục "Quyết định đã chốt". Tên ba menu mới của phase 13 và ba menu của phase 15 là đề xuất, đổi được khi review PR.
