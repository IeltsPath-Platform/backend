# Phase 2 (sửa nhỏ không đổi prompt): báo cáo

- Ngày: 2026-11-11, nhánh `feat/writing-quick-fixes` (backend)
- Trạng thái: DONE_WITH_CONCERNS

## Đã làm
- Correction trong response có `errorCode`, `errorName`, `fixHint`, `severity`, `practiceKnowledgePointId`; lưu vào JSON bài chấm, không gọi Content khi đọc. Bài cũ không có trường mới (NON_NULL).
- Hoàn quota khi lỗi hệ thống (`LLM_UNAVAILABLE`, `AI_BUSY`, `RUBRIC_UNAVAILABLE`, `PROMPT_VERSION_UNAVAILABLE`). `INVALID_GRADE` giữ lượt.
- JSON bài chấm lưu `model` (`learning.llm.model`); API không trả.
- Counter `learning.writing.grading` (nhãn `outcome`, `source`) và distribution `learning.writing.corrections`.
- Contract `docs/contracts/lesson-writing-v1.md` cập nhật.

## Kiểm chứng
- `mvn -B -ntp -pl services/learning-service -am test`: BUILD SUCCESS, 1527 test, 0 fail, 2 skip (`AzureSpeechClientLiveTest`, test live; 3 skip của `SupabaseStorageContractTest` cần `STORAGE_CONTRACT_*`).
- Test mới: `EssaySubmissionFlowTest` (4), `WritingSubmissionResponseTest` (2), `EssayGraderTest` (29).

## Concerns
- `AI_BUSY` giờ đặt submission thành `FAILED` ngay (trước đây để `GRADING` đến khi quá 120 giây stale). Học viên gửi lại được ngay. Đây là thay đổi hành vi ngoài phase; cần người duyệt.
- Metric `llm.call.duration`, `llm.tokens` chưa làm: chờ phase 0b (không tạo timer riêng).
- Chưa có test cho nhánh hết hạn mức (không gọi LLM) và cho `practiceKnowledgePointId`.

## Chưa giải quyết
- Có chấp nhận `AI_BUSY` → `FAILED` không, hay giữ hành vi cũ?
