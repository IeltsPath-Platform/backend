---
phase: 0b
title: "Shared LLM client and single-source essay template"
status: done
priority: P1
dependencies: [0]
---

# Phase 0b: Thư viện LLM dùng chung và template một nguồn (shared, learning, assessment, content)

## Overview
Ba service tự gọi `/chat/completions` bằng ba client khác nhau, và chỉ client của Learning xử lý được model loại
reasoning. Không client nào đọc `usage`, nên không đo được chi phí. Template chấm essay có hai bản chạy thật và một bản
tài liệu. Phase này gom phần **kỹ thuật** vào `shared/common-llm` (quyết định 23), gom template về **một file** (quyết
định 24), và bỏ vòng retry riêng của Content (quyết định 26). Làm sau spike phase 0 và trước phase 1 (quyết định 25), để
phase 1 không phải chép luật dựng request sang Assessment.

## Hiện trạng
| Service | Lớp | Khác biệt |
| --- | --- | --- |
| Learning | `infrastructure/client/OpenAiCompatibleLlmClient` | Luật theo họ model (port từ DeepTutor v1.6.9): `max_completion_tokens`, `reasoning_effort`, bỏ `temperature`; log trường bị provider từ chối (thay đổi chưa commit trên nhánh); timeout 20 s |
| Assessment | `infrastructure/client/OpenAiCompatibleEssayGrader` | Trộn HTTP với dựng prompt và parse; không gửi `max_tokens`; read timeout 45 s; không có `reasoning_effort` |
| Content | `infrastructure/client/OpenAiCompatibleContentGenerator` | Không JSON mode; timeout cấu hình; **tự retry** (5 s, 15 s) chồng lên `ResilientCall`, trái `AGENTS.md` §5 |

## Requirements

### A. Module `shared/common-llm` (chỉ technical concern)
- Theo mẫu `common-resilience`: đăng ký bằng `META-INF/spring/...AutoConfiguration.imports`, thêm vào reactor `pom.xml`.
- API tối thiểu:
  - `LlmEndpoint(baseUrl, apiKey, model, reasoningEffort, timeout, maxTokens)`: mỗi service tự dựng từ prefix cấu hình
    **của mình** (`learning.llm.*`, `assessment.llm-grading.*`, `content.llm.*`); thư viện không có prefix riêng, biến
    môi trường giữ nguyên (kể cả việc `CONTENT_LLM_*` rơi về `LEARNING_LLM_*` khi không đặt).
  - `LlmChatClient.chat(ChatRequest)`: `ChatRequest(purpose, systemPrompt, userPrompt, jsonMode, maxTokensOverride)`.
    Trả `ChatReply(content, inputTokens, outputTokens, model)`.
  - `LlmClientFactory` (bean tự cấu hình) tạo client từ `LlmEndpoint` và tên service.
- Chuyển nguyên luật theo họ model từ client của Learning, gồm cả phần log trường bị từ chối đang chưa commit. **Giữ
  comment ghi nguồn DeepTutor** (`AGENTS.md` §5).
- Luôn gọi qua `ResilientCall.call("llm", CallKind.WRITE, ...)`, không thêm retry. `DownstreamUnavailableException`, lỗi
  HTTP, timeout, body không đọc được → `LlmCallException` có mã (`UNAVAILABLE`, `TIMEOUT`, `HTTP_<status>`,
  `BAD_RESPONSE`); service đổi mã này về contract lỗi của mình.
- Metric Micrometer, nhãn `service`, `model`, `purpose`, `outcome` (không nhãn user hay nội dung):
  - timer `llm.call.duration`;
  - counter `llm.tokens` nhãn thêm `direction` = `input`/`output`, lấy từ `usage.prompt_tokens`/`completion_tokens`
    (provider không trả `usage` thì không tăng, không lỗi).
- Log chỉ có service, model, purpose, mã lỗi, status. Không bao giờ có prompt, essay hay output.
- Không chứa prompt nghiệp vụ, rubric, parser JSON nghiệp vụ.

### B. Chuyển ba service sang thư viện
- **Learning:** `OpenAiCompatibleLlmClient` thành adapter mỏng của port `LlmClient` hiện có, ủy quyền cho thư viện.
  `purpose`: `writing-grading`, `speaking-grading`… theo nơi gọi. Hành vi request giữ nguyên.
- **Assessment:** `OpenAiCompatibleEssayGrader` giữ phần dựng prompt và parse, phần HTTP đi qua thư viện. Thêm
  `assessment.llm-grading.reasoning-effort` (`ASSESSMENT_LLM_REASONING_EFFORT`) và `max-tokens`. **Hành vi đổi có chủ
  đích:** request giờ gửi `max_tokens` hoặc `max_completion_tokens` và `reasoning_effort` giống Learning (phục vụ quyết
  định 8). Read timeout chấm gate giữ 45 s.
- **Content:** `OpenAiCompatibleContentGenerator` qua thư viện, `jsonMode = false`, timeout giữ như cấu hình hiện tại.
  **Bỏ vòng retry riêng** (`DEFAULT_RETRY_DELAYS`): lỗi thì bản nháp `FAILED` với mã tương ứng, author chạy lại. Giữ các
  mã `NOT_CONFIGURED`, `TIMEOUT`, `HTTP_*`, `FAILED`.

### C. Template chấm essay một nguồn
- File gốc duy nhất: `docs/contracts/essay-prompt-template-v*.txt`. Xóa bản trong `src/main/resources` của Learning và
  Assessment.
- `pom.xml` của Learning và Assessment thêm `<resource>` lấy `essay-prompt-template-*.txt` từ
  `${maven.multiModuleProjectDirectory}/docs/contracts` vào classpath gốc. Tên file trên classpath không đổi nên code đọc
  template không đổi.
- Docker: `Dockerfile.spring-service` `COPY . .` từ root và `.dockerignore` không loại `docs/` (đã kiểm 2026-11-11). Kiểm
  lại bằng một lần build image Learning.
- `essay-kp-judgment-v2.txt` chỉ Learning dùng: để nguyên chỗ cũ.
- `promptVersion` (`<rubric>@<version>+tpl<v>`) là nghiệp vụ nên không đưa vào shared. Ghi định dạng trong
  `docs/contracts/lesson-writing-v1.md`; mỗi service giữ một hằng số và có test khẳng định cùng chuỗi cho cùng rubric mẫu.
- Test so template giữa Assessment và contract (`RubricPromptBuilderTest`) đổi thành test khẳng định classpath có đúng
  file của `docs/contracts`.

### D. Tài liệu
- `AGENTS.md` (§1 bảng module, §2 stack: 18 module, 6 shared; §3.9 dòng "Gọi HTTP sang service khác hoặc LLM" thêm
  `common-llm`), `CLAUDE.md` §2, `README.md` bảng pattern, `docs/system-architecture.md`, `docs/ai-policy.md` (metric
  token, mọi lời gọi LLM qua `common-llm`). Cập nhật dòng ngày ở đầu `AGENTS.md`.

## Related Code Files
- Create: `shared/common-llm/` (pom, `LlmEndpoint`, `ChatRequest`, `ChatReply`, `LlmChatClient`, `LlmClientFactory`,
  `LlmCallException`, auto-configuration, test)
- Modify: `pom.xml` (reactor); pom và client của ba service; `services/assessment-service/.../RubricPromptBuilder.java`;
  `infra/config-server/config-repo/{learning,assessment,content}-service.yaml`; tài liệu ở mục D
- Delete: `services/{learning,assessment}-service/src/main/resources/essay-prompt-template-v1.txt`

## Implementation Steps
1. Baseline: `mvn -q -pl services/learning-service,services/assessment-service,services/content-service -am test`.
2. Test đỏ thư viện (`MockRestServiceServer`): từng họ model (gpt-5/o-series, gemini-2.5/3, model thường); JSON mode bật
   và tắt; `usage` có và không; từng loại lỗi ra đúng mã; log không chứa prompt (bắt log trong test).
3. Code thư viện → chạy `mvn -q -pl shared/common-llm test`.
4. Chuyển Learning → test Learning xanh, request giống hệt trước (so body trong test).
5. Chuyển Assessment → test chấm gate và eval xanh; test mới cho body có `max_tokens`/`reasoning_effort`.
6. Chuyển Content, bỏ retry → test bản nháp xanh; test mới: lỗi LLM cho `FAILED` ngay, không chờ.
7. Gom template, chạy test hai service, build image Learning một lần.
8. Cập nhật tài liệu; `mvn -q compile -DskipTests` cả reactor; `docker compose config --quiet`.

## Success Criteria
- [ ] Không còn code dựng request `/chat/completions` nào ngoài `shared/common-llm`.
- [ ] Request của Learning không đổi một trường; Assessment gửi đúng `max_tokens`/`reasoning_effort`; Content không retry riêng.
- [ ] Metric `llm.call.duration` và `llm.tokens` thấy ở `/actuator/prometheus` của cả ba service khi chạy local.
- [ ] Chỉ còn một file template mỗi phiên bản, ở `docs/contracts/`; image Docker chứa đúng file.

## Risk Assessment
- **Chấm gate đổi hành vi:** `max_tokens` quá thấp có thể cắt JSON → gate essay chuyển EXAMINER. Đặt mặc định theo token ra
  đo ở phase 0, chạy một run eval v1 trước và sau (đây cũng là số nền của phase 1).
- **Content bỏ retry:** nhiều bản nháp `FAILED` hơn khi provider chập chờn. Chấp nhận theo quyết định 26; theo dõi tỷ lệ
  `FAILED` một tuần.
- **Thay đổi chạm 3 service và shared:** làm thành commit riêng cho từng bước B, C để quay lui từng phần được.
