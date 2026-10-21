---
phase: 2
title: "Learning: một request chấm band và KP con, luật kiểm tra, evidence, response, tài liệu"
status: complete
priority: P1
effort: "2d"
dependencies: [1]
---

# Phase 2: Learning — đánh giá KP con trong cùng request chấm band

Repo: `backend`. Module: `services/learning-service` (`L` = `services/learning-service/src/main/java/com/ieltspath/learning`, `T` = `services/learning-service/src/test/java/com/ieltspath/learning`). Đọc [plan.md](./plan.md) trước. Phase 1 phải xong.

Tài liệu này mô tả **thiết kế cuối**: một request LLM cho mỗi lần chấm. Thiết kế ban đầu dùng lần gọi thứ hai (`kpj1`,
`completeJsonQuick`, timeout 12 s, bỏ qua sau 25 s) đã bị thay ngày 2026-10-22 theo yêu cầu "gọi một lần thôi"; không còn dấu vết
của nó trong code.

## Overview
Hướng dẫn đánh giá KP con được nối vào system prompt band `v1`; LLM trả một JSON gồm `criteria`, `corrections`, `summary`,
`focus` và `judgments`. Luật thuần kiểm tra judgments và đối chiếu với correction; grade lưu `knowledgePointJudgments` và
study point `KNOWLEDGE_POINT`; `WritingEvidenceRule` ghi evidence cho KP con; response trả hợp đồng JSON trong plan.md.

Use case lesson và Practice **không cần sửa**: chúng đã ghi evidence cho mọi KP trong `outcome.correctByKnowledgePoint()` và gửi
`outcome.criterionKnowledgePoints()` vào `masteryProfiles.recordEvidence` (không XP mốc).

## Requirements
- Đúng **một** request LLM mỗi lần chấm, kể cả khi lỗi: không retry sửa JSON, không retry 429/503. JSON hoặc band sai là
  `INVALID_GRADE`, lỗi provider là `LLM_UNAVAILABLE`; cả hai không thu points. Timeout chung `learning.writing.grading-timeout-seconds` (20 s).
- Template band `v1`, `TEMPLATE_VERSION = "v1"`, `promptVersion` (`+tplv1`), `ai_prompt_activations`, `EssaySubmissionFlow`, quota ngày không đổi.
- Hướng dẫn KP chỉ được nối khi rubric có KP con; không có KP con thì prompt, kết quả và `judgmentVersion` (null) y như trước.
- `judgmentVersion = kpj2` được lưu trong JSON grade **bất cứ khi nào rubric có KP con** (prompt đã nối), kể cả khi mô hình không trả `judgments`. Không phát qua API.
- Judgments thiếu hoặc sai kiểu không làm mất band hợp lệ: KP thành `NOT_ASSESSED`, vẫn áp đối chiếu correction.
- Không log essay, prompt, output LLM.
- Bài cũ trong DB đọc lại không lỗi (`knowledgePointJudgments = []`, `judgmentVersion = null`).
- Không gọi Content thêm: dùng rubric (đã có `subKnowledgePoints`) và `getTopicSequence()`, gọi **tối đa một lần** mỗi lần chấm và chỉ khi cần.

## Architecture

```text
EssayGrader.grade(prompt, essay)
  system = band template v1 + EssayKnowledgeJudge.prompt(task, rubric.criteria())   // phần nối chỉ khi có KP con
  root   = readObject(llm.completeJson(system, user))                                // đúng một request
  grade  = parse(root)                       // band, corrections, focus; sai → INVALID_GRADE
  g1     = withCriterionStudyPoints(grade, rubric, passBand, catalog)                // CRITERION + AI như cũ
  g2     = EssayKnowledgeJudge.judge(task, essay, g1, rubric.criteria(), allowedCodes, root.path("judgments"), catalog)
  return GradedEssay(g2, promptVersion)
```
`catalog` là `Supplier` lười, nhớ kết quả: gọi `getTopicSequence()` tối đa một lần, chỉ khi có tiêu chí yếu cần chi tiết KP hoặc có
judgment `FAIL` cần biết KP con có bộ luyện.

### A. LLM client
- `L/application/port/LlmClient.java`: `String completeJson(String systemPrompt, String userPrompt)` (không còn tham số `lowReasoning`).
- `L/infrastructure/client/OpenAiCompatibleLlmClient.java`: một lần gọi duy nhất; `RestClientResponseException` → `LlmUnavailableException(status)`, `RestClientException` → `LlmUnavailableException(null)`; log chỉ có status hoặc tên lớp exception. Không có hằng/hàm retry. `reasoning_effort` và `temperature` theo cấu hình như cũ.
- `WritingSettings` không có cấu hình `judgment-*`.

### B. Content client (`L/application/port/LearningContentClient.java`)
- `record SubKnowledgePoint(UUID id, String code, String name, String description)`.
- `RubricCriterion` thêm thành phần cuối `List<SubKnowledgePoint> subKnowledgePoints` (null → `List.of()`); giữ constructor 5 và 6 tham số.
- `ErrorType` thêm `UUID knowledgePointId` cuối; giữ constructor 5 tham số.

### C. Mô hình grade (`L/domain/vo/WritingGrade.java`)
- `enum Judgment { PASS, FAIL, NOT_ASSESSED }`; `record KnowledgeJudgment(UUID knowledgePointId, String code, String name, Judgment judgment, String excerpt, String reason)`.
- `Criterion` thêm `List<KnowledgeJudgment> knowledgePointJudgments` (null → `[]`) và `withKnowledgePointJudgments(...)`.
- `StudyPoint` thêm `String excerpt, UUID practiceKnowledgePointId` và hằng `KNOWLEDGE_POINT`.
- `WritingGrade` thêm `String judgmentVersion` cuối; giữ constructor 4 và 5 tham số.

### D. Luật thuần `L/domain/service/KnowledgeJudgmentRule.java`
`judge(candidates, raws, essay, corrections)` trả đúng một judgment cho mỗi KP con theo thứ tự `candidates`:
1. Tìm raw đầu tiên có đúng `criterion` và `code`; không có → `NOT_ASSESSED`.
2. `judgment` trim + upper-case; chỉ `PASS`/`FAIL`, giá trị khác → `NOT_ASSESSED`.
3. `PASS`/`FAIL` chỉ giữ nếu `excerpt` (trim) không rỗng **và** `essay.contains(excerpt)`; ngược lại → `NOT_ASSESSED`. Excerpt, reason cắt 300 ký tự.
4. Có correction hợp lệ mà error type trỏ tới KP đó: kết quả `PASS` → `NOT_ASSESSED` (xóa excerpt/reason); `NOT_ASSESSED` → `FAIL` với excerpt của correction và reason = `fixHint`; `FAIL` giữ nguyên.
`studyPoints(criteria, existing, practiceTarget)`: mỗi judgment `FAIL` thành study point `KNOWLEDGE_POINT`; thứ tự: mỗi `CRITERION` kèm các `KNOWLEDGE_POINT` cùng tiêu chí ngay sau, rồi `KNOWLEDGE_POINT` của tiêu chí không có `CRITERION` point, cuối cùng các point còn lại (AI).

### E. Prompt và `EssayKnowledgeJudge`
- `services/learning-service/src/main/resources/essay-kp-judgment-v2.txt`: phần hướng dẫn nối vào system prompt, yêu cầu thêm mảng `judgments` cùng JSON, mô tả `PASS`/`FAIL`/`NOT_ASSESSED`, bắt buộc excerpt nguyên văn, không tính band từ judgments, `{{knowledge_points}}` liệt kê `code — name: description` theo tiêu chí (không UUID).
- `L/application/service/EssayKnowledgeJudge.java` (không phụ thuộc LLM, clock, timeout): `static String prompt(task, criteria)` dựng phần nối ("" khi không có KP con); `judge(...)` dựng `candidates`, đọc mảng `judgments` (mỗi phần tử object, trường sai kiểu → null), gọi rule, gắn judgments vào từng tiêu chí, dựng study point với `practiceKnowledgePointId` = KP con nếu catalog báo có bộ luyện, ngược lại KP tiêu chí cha. Trả `WritingGrade` với `judgmentVersion = "kpj2"` khi có KP con.

### F. Lưu và response
- `JdbcWritingSubmissionRepository`: `StoredGrade` thêm `judgmentVersion`; grade cũ thiếu trường đọc thành null/`[]`.
- `WritingSubmissionResponse`: `criteria[].knowledgePointJudgments`, `studyPoints[].excerpt`, `studyPoints[].practiceKnowledgePointId`; không lộ `judgmentVersion`.

### G. Evidence (`L/domain/service/WritingEvidenceRule.java`)
Giữ khối hiện tại (KP đề = `passed`; KP tiêu chí theo band). Thêm khối độc lập passBand: mỗi judgment `PASS` → đúng, `FAIL` → sai, `NOT_ASSESSED` bỏ; KP tiêu chí thắng nếu trùng id; KP con thắng KP đề; hai judgment cùng KP phải cùng đúng. KP con nằm trong `criterionKnowledgePoints` (không XP mốc). Thứ tự key: KP đề, KP tiêu chí, KP con.

## Related Code Files
Create: `L/domain/service/KnowledgeJudgmentRule.java`, `L/application/service/EssayKnowledgeJudge.java`, `services/learning-service/src/main/resources/essay-kp-judgment-v2.txt`, `T/domain/service/KnowledgeJudgmentRuleTest.java`, `T/application/service/EssayKnowledgeJudgeTest.java`.
Modify: `LlmClient`, `LearningContentClient`, `WritingSettings` (Javadoc), `EssayGrader`, `OpenAiCompatibleLlmClient`, `WritingGrade`, `WritingEvidenceRule`, `JdbcWritingSubmissionRepository`, `WritingSubmissionResponse`, các test tương ứng và `LessonWritingIntegrationTest`, `PracticeEssayIntegrationTest`, `FreeWritingIntegrationTest`.
Docs: `backend/docs/contracts/lesson-writing-v1.md`, `backend/AGENTS.md` mục 3.8, `backend/docs/ai-policy.md`.
Không sửa: `SubmitLessonEssayUseCase`, `SubmitPracticeAttemptUseCase`, `EssaySubmissionFlow`, Assessment.

## Tests (đã có)
- `KnowledgeJudgmentRuleTest`: giữ/hạ PASS và FAIL theo excerpt, giá trị lạ, raw không khớp, cắt 300 ký tự, đối chiếu correction cả ba hướng, thứ tự study point.
- `EssayKnowledgeJudgeTest`: không KP con giữ nguyên grade; FAIL hợp lệ thành judgment và study point; KP không được nhắc là `NOT_ASSESSED`; correction đối chiếu; đích luyện tập dự phòng; catalog chỉ hỏi khi có `FAIL`; judgments thiếu/sai vẫn giữ band và vẫn ghi `kpj2`; phần tử hỏng không làm mất judgment hợp lệ; prompt có tên KP và không có UUID.
- `EssayGraderTest`: chấm có KP con gửi **đúng một** request (prompt chứa mã/tên/mô tả KP, không UUID, `getTopicSequence()` một lần); JSON sai hoặc lỗi provider không gọi lại; chấm không KP con y như trước.
- `OpenAiCompatibleLlmClientTest`: 429/503, timeout, lỗi kết nối đều dừng sau đúng một request, không rò nội dung.
- `WritingEvidenceRuleTest`, `WritingGradeJsonTest`, `WritingWebMvcTest`, `LessonWritingIntegrationTest`, `PracticeEssayIntegrationTest`: evidence KP con, đọc lại JSON cũ, hợp đồng response, đường lesson và Practice.

## Success Criteria
- [x] Chấm có hoặc không có KP con chỉ gửi đúng một request LLM.
- [x] `promptVersion` vẫn `<rubricCode>@<version>+tplv1`; không đổi activation hay Assessment.
- [x] Judgments thiếu/sai không làm mất band; JSON/band sai không retry và không thu points.
- [x] `judgmentVersion = kpj2` khi rubric có KP con, kể cả khi không có mảng judgments.
- [x] Grade cũ đọc lại không lỗi; không log nội dung essay/prompt/output.
- [ ] Chưa đo: độ chính xác của `kpj2` trên gold set, độ trễ provider thực, kiểm thủ công với LLM thật.

## Risk Assessment
- **Mất lớp đỡ lỗi**: không retry nên provider bận hoặc JSON sai là học viên phải nộp lại (không mất points). Nếu tỷ lệ `INVALID_GRADE`/`LLM_UNAVAILABLE` cao, cân nhắc khôi phục retry JSON band (vẫn chỉ gọi thêm khi bản đầu hỏng).
- **Prompt band bị mở rộng**: khi rubric có KP con, system prompt dài hơn template đã đánh giá; chất lượng band chưa đo. `judgmentVersion = kpj2` là dấu vết duy nhất; `promptVersion` vẫn `+tplv1`.
- **`MAX_TOKENS = 4096`** dùng chung cho phản hồi lớn hơn; mô hình có reasoning có thể cắt cụt → `INVALID_GRADE`.
- Kiểm tra chéo với correction chỉ chạy thật khi error type được duyệt (hiện đều `DRAFT`).
- Hoàn tác: bỏ mapping KP con (phase 1) thì không còn phần nối prompt và không còn judgments; không cần migration dữ liệu, bài cũ vẫn đọc được.
