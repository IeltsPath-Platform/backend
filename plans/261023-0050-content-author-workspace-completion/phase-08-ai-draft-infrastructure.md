# Phase 08: Hạ tầng bản nháp, job nền, hạn mức và LLM

Người làm: **Claude Code**. Nhánh: `feat/content-ai-drafts` (backend), tạo từ `origin/main` sau khi phase 07 đã merge.
Trạng thái: Pending

Repo: `backend`, service `content-service`. Phụ thuộc: phase 01 đã merge (cùng sửa `content-service.yaml`, `docker-compose.yml`, `pom.xml`).

## Mục tiêu

Nền chung cho mọi tính năng AI và bản nháp course: bảng bản nháp, hàng đợi, hạn mức theo ngày, port gọi LLM, job nền. Chưa sinh nội dung nào ở phase này; phase 09 và 11 cắm vào, phase 10 dùng cùng bảng cho bản nháp course soạn tay.

## Đọc trước

- Mẫu job nền có claim, quota và đánh dấu lỗi: `GradeGateEssayJobUseCase` của assessment-service.
- Client OpenAI-compatible đang có trong repo (learning/assessment) để dùng cùng kiểu `RestClient`, không thêm thư viện.
- `docs/ai-policy.md`.

## Yêu cầu

- **Migration `ai_drafts`** (số kế tiếp trên `origin/main`):

  ```sql
  CREATE TABLE ai_drafts (
      id UUID PRIMARY KEY,
      kind VARCHAR(20) NOT NULL CHECK (kind IN ('QUESTIONS','COURSE')),
      status VARCHAR(12) NOT NULL CHECK (status IN ('QUEUED','GENERATING','READY','FAILED','APPROVED','REJECTED')),
      requested_by UUID NOT NULL,
      request JSONB NOT NULL,
      result JSONB,
      error_code VARCHAR(40),
      model VARCHAR(80), prompt_version VARCHAR(40),
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      started_at TIMESTAMPTZ, finished_at TIMESTAMPTZ,
      reviewed_by UUID, reviewed_at TIMESTAMPTZ, review_note TEXT
  );
  CREATE INDEX idx_ai_drafts_status_created ON ai_drafts (status, created_at);
  CREATE INDEX idx_ai_drafts_requester ON ai_drafts (requested_by, created_at DESC);

  CREATE TABLE ai_usage_daily (
      user_id UUID NOT NULL, usage_date DATE NOT NULL,
      kind VARCHAR(10) NOT NULL CHECK (kind IN ('AI','IMPORT')),
      count INTEGER NOT NULL CHECK (count >= 0),
      PRIMARY KEY (user_id, usage_date, kind)
  );
  ```

  `kind` của `ai_usage_daily` có sẵn `IMPORT` để phase 12 đếm riêng lượt import, không phải đổi bảng lần nữa. Cột JSONB map từ `String` phải có `@JdbcTypeCode(SqlTypes.JSON)` nếu dùng JPA (cạm bẫy đã gặp ở access).
- **Domain (aggregate `AiDraft`):** `QUEUED → GENERATING → READY | FAILED`, rồi `READY → APPROVED | REJECTED`. Duyệt hoặc từ chối chỉ từ `READY`. `FAILED` không chạy lại tại chỗ; muốn chạy lại thì tạo yêu cầu mới. Bản nháp course soạn tay (phase 10) tạo thẳng ở `READY`, `model` null.
- **Port (application/port):**
  - `ContentGenerationPort`: `generate(GenerationPrompt) → GenerationResult` (văn bản JSON); ném `ContentGenerationException` mã `NOT_CONFIGURED`, `FAILED`, `INVALID_REPLY`.
  - `AiDraftStore`: lưu, claim theo lô (`FOR UPDATE SKIP LOCKED`), đánh dấu xong/lỗi, kéo lại việc kẹt.
  - `AiUsageQuota`: `tryConsume(userId, date, kind)` nguyên tử; `refund(...)`.
- **Adapter:** client OpenAI-compatible bằng `RestClient`, cấu hình `CONTENT_LLM_BASE_URL`, `CONTENT_LLM_API_KEY`, `CONTENT_LLM_MODEL`; timeout mặc định 60 giây. Thiếu cấu hình → việc vẫn nhận nhưng chuyển `FAILED` với `NOT_CONFIGURED`, API khác vẫn chạy.
- **Scheduler:** mỗi chu kỳ lấy tối đa `batch-size` việc `QUEUED` (mặc định 2), gọi LLM **ngoài transaction**, ghi kết quả trong transaction ngắn. Việc `GENERATING` quá `stuck-after` (mặc định 10 phút) về `QUEUED`. Nhiều instance chạy cùng lúc không xử lý trùng việc.
- **Hạn mức:** nhận yêu cầu thì trừ một lượt `AI`; hết → `429 AI_QUOTA_EXCEEDED`, không tạo việc. Việc `FAILED` do hệ thống hoàn lại một lượt. Mặc định `daily-limit` 20 việc/người/ngày, `max-items-per-request` 20.
- **API chung** (chỉ `CONTENT_AUTHOR`, mỗi người chỉ thấy bản nháp của mình):
  - `GET /api/content/admin/ai-drafts?kind=&status=&page=&size=` (phân trang, sắp `created_at` giảm dần);
  - `GET /api/content/admin/ai-drafts/{id}` (bản nháp của người khác → `404`);
  - `POST /api/content/admin/ai-drafts/{id}/reject` kèm `note`.
- **Cấu hình** (cần thiết cho tính năng, ghi rõ trong PR vì `AGENTS.md` hạn chế sửa cấu hình như tác dụng phụ):
  - `config-repo/content-service.yaml`: nhóm `content.ai.*` (`batch-size`, `stuck-after`, `daily-limit`, `max-items-per-request`, `timeout`), `content.llm.base-url: ${CONTENT_LLM_BASE_URL:}`, `api-key: ${CONTENT_LLM_API_KEY:}`, `model: ${CONTENT_LLM_MODEL:}`.
  - `docker-compose.yml` service `content-service`: ba biến `CONTENT_LLM_*` mặc định rỗng.
  - `services/content-service/README.md`: biến môi trường mới.
- Contract mới `docs/contracts/ai-drafts-v1.md`: bản nháp, trạng thái, phân trang, `429`, mã lỗi, viết trước khi code.
- Không log prompt, nội dung trả về hay khóa; log chỉ id, loại việc, mã lỗi.

## Thiết kế phía code

(`C` = `services/content-service/src/main/java/com/ieltspath/content`)

- `C/domain/aggregate/AiDraft.java`, `C/domain/vo/{DraftKind, DraftStatus}.java`
- `C/application/port/{ContentGenerationPort, AiDraftStore, AiUsageQuota}.java`
- `C/application/usecase/{ListAiDraftsUseCase, GetAiDraftUseCase, RejectAiDraftUseCase, RunAiDraftJobUseCase}.java`
- `C/infrastructure/client/OpenAiCompatibleContentGenerator.java`, store và quota JDBC ở `C/infrastructure/persistence/adapter/`, `C/infrastructure/scheduler/AiDraftScheduler.java`, `C/api/controller/AiDraftController.java`
- `RunAiDraftJobUseCase` gọi bộ sinh theo `kind` qua một interface do phase 09/11 cung cấp; phase này chỉ có bộ sinh giả trong test.

## Test

- `AiDraftTest`: chuyển trạng thái hợp lệ và không hợp lệ.
- `RunAiDraftJobUseCaseTest` (port giả): thành công, LLM lỗi, thiếu cấu hình, việc kẹt quay lại, hoàn hạn mức khi lỗi hệ thống.
- `OpenAiCompatibleContentGeneratorTest` (`MockRestServiceServer`): thiếu cấu hình, lỗi HTTP, JSON hỏng, timeout.
- `AiDraftIntegrationTest` (Testcontainers): hai lần claim song song không trùng việc.
- `AiDraftControllerTest`: `CUSTOMER`/`EXAMINER`/`ADMIN` → 403; hết hạn mức → 429; xem bản nháp của người khác → 404.
- Context load với `CONTENT_LLM_*` rỗng: service khởi động.

## Hoàn thành khi

`mvn -q -pl services/content-service -am test` xanh; contract `ai-drafts-v1.md` có trên nhánh; không test nào gọi LLM thật.

## Kết quả

Đã làm (2026-10-23, Claude Code, commit `587d8d32`):
- Migration V77 (`ai_drafts`, `ai_usage_daily` có cột `kind`); aggregate `AiDraft` với state machine; port LLM, hạn mức, repository, generator theo loại.
- Claim hàng đợi bằng `FOR UPDATE SKIP LOCKED`; hạn mức tăng nguyên tử; việc kẹt quá `stale-after` về `QUEUED`.
- Job chạy ngoài transaction; mọi lỗi ghi mã lỗi và hoàn một lượt hạn mức; prompt và phản hồi không vào log hay thông báo lỗi.
- API: danh sách, xem, từ chối (chỉ bản của chính người gọi; người khác nhận 404).
- Test: 26 test AI, gồm claim song song và hạn mức trên Postgres thật; toàn bộ content-service xanh.
- Chưa làm: route tạo yêu cầu và duyệt (thuộc phase 09 và 11), vì cần generator theo loại.
- Contract: `docs/contracts/ai-drafts-v1.md`.
