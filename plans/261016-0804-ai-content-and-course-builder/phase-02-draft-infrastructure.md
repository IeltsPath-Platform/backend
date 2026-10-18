---
phase: 2
title: "Hạ tầng bản nháp, job nền và LLM"
status: pending
priority: P1
dependencies: []
---

# Phase 2: Hạ tầng bản nháp, job nền và LLM

## Overview

Phần dùng chung cho sinh câu hỏi và sinh course: một bảng bản nháp, một hàng đợi việc nền, bộ đếm hạn mức theo ngày, và
một port gọi LLM. Chưa sinh nội dung nào ở phase này; phase 3 và 5 cắm vào.

## Requirements

- **Migration `V35__ai_drafts.sql`:**

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
      user_id UUID NOT NULL, usage_date DATE NOT NULL, count INTEGER NOT NULL CHECK (count >= 0),
      PRIMARY KEY (user_id, usage_date)
  );
  ```

- **Domain (aggregate `AiDraft`):** trạng thái chuyển `QUEUED → GENERATING → READY | FAILED`, rồi `READY → APPROVED |
  REJECTED`. Duyệt hoặc từ chối chỉ từ `READY`. `FAILED` có thể yêu cầu chạy lại (tạo bản nháp mới).
- **Port (application):**
  - `ContentGenerationPort`: `generate(GenerationPrompt) → GenerationResult` (văn bản JSON) và ném
    `ContentGenerationException` với mã `NOT_CONFIGURED`, `FAILED`, `INVALID_REPLY`.
  - `AiDraftStore`: lưu, claim việc theo lô (`FOR UPDATE SKIP LOCKED`), đánh dấu xong hoặc lỗi, kéo lại việc kẹt.
  - `AiUsageQuota`: `tryConsume(userId, date)` nguyên tử.
- **Adapter:** client OpenAI-compatible (`RestClient`/`RestTemplate` đang dùng trong repo, không thêm thư viện), cấu hình qua
  `CONTENT_LLM_BASE_URL`, `CONTENT_LLM_API_KEY`, `CONTENT_LLM_MODEL` (tên theo Open question 2). Thiếu cấu hình → việc
  nhận nhưng chuyển `FAILED` với `NOT_CONFIGURED`; các API khác vẫn chạy.
- **Scheduler:** mỗi chu kỳ lấy tối đa N việc `QUEUED` (mặc định 2 để không ăn hết tài nguyên của API), gọi LLM **ngoài
  transaction**, ghi kết quả trong transaction ngắn. Việc `GENERATING` quá `stuck-after` (mặc định 10 phút) được đưa về
  `QUEUED`. Không log prompt hay nội dung trả về.
- **Hạn mức:** nhận yêu cầu thì trừ hạn mức; hết → `429 AI_QUOTA_EXCEEDED`, không tạo việc. Việc lỗi do hệ thống
  (`FAILED`) hoàn lại một lượt.
- **API chung** (`CONTENT_AUTHOR`, `ADMIN`):
  - `GET /api/content/admin/ai-drafts?kind=&status=&page=&size=`;
  - `GET /api/content/admin/ai-drafts/{id}`;
  - `POST /api/content/admin/ai-drafts/{id}/reject` kèm ghi chú.
  `CONTENT_AUTHOR` chỉ thấy bản nháp của mình; `ADMIN` thấy tất cả.
- **Cấu hình:** `infra/config-server/config-repo/content-service.yaml`, nhóm `content.ai.*`
  (`batch-size`, `stuck-after`, `daily-limit`, `max-items-per-request`). Giá trị bí mật chỉ qua `.env`.

## Related Code Files

(`C` = content package, theo `domain / application / api / infrastructure`)

- Tạo: `C/domain/aggregate/AiDraft.java`, `C/domain/vo/{DraftKind, DraftStatus}.java`,
  `C/application/port/{ContentGenerationPort, AiDraftStore, AiUsageQuota}.java`,
  `C/application/usecase/{ListAiDraftsUseCase, GetAiDraftUseCase, RejectAiDraftUseCase, RunAiDraftJobUseCase}.java`,
  `C/infrastructure/client/OpenAiCompatibleContentGenerator.java`, `C/infrastructure/persistence/Jdbc*` cho store và quota,
  `C/infrastructure/scheduler/AiDraftScheduler.java`, `C/api/controller/AiDraftController.java`
- Sửa: `content-service.yaml`, `docker-compose.yml` và `.env.example` chỉ khi thiếu biến (xin phép nếu phải sửa Docker,
  AGENTS.md §5 cấm sửa như tác dụng phụ).
- Test: `AiDraftTest` (chuyển trạng thái), `RunAiDraftJobUseCaseTest` (cổng giả: thành công, lỗi, hết hạn mức, kẹt),
  `OpenAiCompatibleContentGeneratorTest` (`MockRestServiceServer`: thiếu cấu hình, lỗi HTTP, JSON hỏng),
  `AiDraftIntegrationTest` (claim song song không trùng việc), `AiDraftControllerTest` (quyền, 429).

## Implementation Steps

1. **Đỏ trước:** `AiDraftTest` và `RunAiDraftJobUseCaseTest`.
2. Migration, aggregate, store, quota.
3. Port, adapter, scheduler.
4. API và phân quyền.
5. Cấu hình, báo người dùng nếu cần thêm biến môi trường.
6. `mvn -q -pl services/content-service -am test`.

## Success Criteria

- [ ] Hai bản của scheduler chạy cùng lúc không xử lý trùng một việc.
- [ ] Hết hạn mức → 429; thiếu cấu hình LLM → việc `FAILED` với `NOT_CONFIGURED`, API khác không lỗi.
- [ ] Không test nào gọi LLM thật; log không chứa nội dung sinh ra.

## Risk Assessment

- Job nền chạy chung tiến trình với API của content. Lô nhỏ (2) và thời gian gọi có timeout (mặc định 60 giây) để không
  chiếm luồng; khi cần scale thì tách ai-service (ngoài phạm vi).
- LLM trả JSON sai định dạng. Adapter chỉ trả văn bản; kiểm tra schema nằm ở use case của từng loại (phase 3, 5).

## Bàn giao FE

Màn danh sách bản nháp AI (trạng thái, người yêu cầu, thời gian) dùng chung cho hai loại.
