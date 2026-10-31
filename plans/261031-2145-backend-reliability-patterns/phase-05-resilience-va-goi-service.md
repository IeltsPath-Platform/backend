---
phase: 5
title: "Resilience va goi service"
status: pending
priority: P2
effort: "L"
dependencies: [2]
---

# Phase 5: Resilience và gọi service

## Overview

Thêm circuit breaker, retry có giới hạn và bulkhead (Resilience4j) cho các lời gọi HTTP quan trọng, thống nhất cách ánh xạ lỗi downstream, đặt timeout ở gateway, và thêm contract test cho 3 API nội bộ. Mục tiêu: downstream chết thì caller trả lỗi nhanh, không treo luồng, tự hồi phục.

## Hiện trạng (đã kiểm)

22 client RestClient, có read timeout (3–5 giây hoặc cấu hình), không breaker, không bulkhead; retry chỉ ở `OpenAiCompatibleContentGenerator`. Danh sách:

| Service | Client |
|---|---|
| assessment | `AccessHumanGradingCreditClient`, `ContentErrorTypeClient`, `ContentPackageClient`, `ContentRubricClient`, `OpenAiCompatibleEssayGrader`, `RestUserDirectory`, `UserExaminerClient` |
| community | `LearningStreakClient`, `UserPublicProfileClient` |
| content | `LibraryVocabularySenseClient`, `OpenAiCompatibleContentGenerator` |
| game | `ContentSnapshotClient`, `CurrentPlayerProfileClient`, `WordlyWordClient` |
| learning | `OpenAiCompatibleLlmClient`, `RestAccessClient`, `RestAssessmentStudyContextClient`, `RestDisplayNameClient`, `RestEssayPromptActivationClient`, `RestHumanReviewClient`, `RestLearningContentClient`, `UserServiceStudyTimezones` |

## Requirements

- Mỗi client có một instance breaker theo **service đích** (`content`, `access`, `user`, `assessment`, `library`, `learning`, `llm`), không theo từng client.
- Timeout: giữ read timeout hiện có; thêm connect timeout 2 giây nếu thiếu. Không dùng `TimeLimiter` (RestClient đồng bộ).
- Retry: chỉ GET và lỗi kết nối/5xx; tối đa 2 lần, backoff 200 ms → 400 ms; **không** retry POST (trừ khi có idempotency key: `RestAccessClient` debit đã có `idempotencyKey`, được phép retry).
- Bulkhead (semaphore): `llm` tối đa 4 lời gọi đồng thời mỗi instance; vượt → lỗi ngay, map thành 503 `AI_BUSY`.
- Breaker mở hoặc bulkhead đầy → ném exception "Unavailable" hiện có của service (đã có ở nhiều client) → handler trả 503 với `code` theo service đích.
- Fallback chỉ ở chỗ có giá trị hợp lý sẵn: `RestDisplayNameClient`, `UserPublicProfileClient` (tên hiển thị rỗng), `LearningStreakClient` (streak 0). Chỗ khác fail fast.
- Metric Resilience4j xuất qua Micrometer (phase 2).
- <!-- Updated: bổ sung sau validate - timeout gateway --> **Timeout ở gateway:** hiện `api-gateway.yaml` không đặt `connect-timeout` hay `response-timeout`, nên downstream treo thì gateway giữ kết nối mãi. Đặt mặc định connect 2 giây, response 15 giây; route AI và upload 120 giây. Quá hạn → 504 với định dạng lỗi chung.
- <!-- Updated: bổ sung sau validate - contract test --> **Contract test cho 3 API nội bộ:** một file JSON mẫu dùng chung cho cả bên cung cấp và bên gọi:
  1. content `GET /internal/learning-content/lessons/{id}` ↔ learning `RestLearningContentClient.getLesson`.
  2. access `POST /internal/access/points/debit` ↔ learning `RestAccessClient` (trừ điểm).
  3. access `POST /internal/access/human-grading/consume` ↔ assessment `AccessHumanGradingCreditClient`.
  Bên cung cấp: test controller trả JSON có đủ mọi field của mẫu, cùng kiểu (cho phép thêm field). Bên gọi: `MockRestServiceServer` trả đúng mẫu, client đọc được và ánh xạ đúng field chính.

## Architecture

- Module riêng `shared/common-resilience` (không gộp vào `common-observability`, vì service có thể cần log mà không cần breaker) gồm `ResilientCall` (bọc `Supplier` với breaker + retry + bulkhead theo tên đích) và cấu hình mặc định trong `config-repo/application.yaml` (`resilience4j.circuitbreaker.instances.*`). Dùng API lập trình (không annotation) để giữ RestClient đơn giản và test được.
- Breaker mặc định: cửa sổ đếm 20 lời gọi, ngưỡng lỗi 50%, chậm > read timeout tính là lỗi, mở 30 giây, half-open 3 lời gọi. Lỗi 4xx **không** tính là lỗi breaker.
- Correlation header đã do phase 2 thêm.

## Related Code Files

- Create: `shared/common-resilience/` (pom, `ResilientCall`, auto-config, README)
- Modify: 22 client trên (bọc lời gọi), `services/*/pom.xml`
- Modify: `infra/config-server/config-repo/application.yaml` và yaml từng service nếu cần ghi đè
- Modify: `GlobalExceptionHandler` các service để map exception "Unavailable" thống nhất (nhiều service đã có)

## Implementation Steps

### Test viết trước

1. Unit test `ResilientCall`: breaker mở sau ngưỡng; lời gọi khi mở không chạm downstream; half-open đóng lại khi thành công; 4xx không làm mở; retry chỉ cho thao tác đánh dấu an toàn; bulkhead đầy ném ngay.
2. Với mỗi service, một test `MockRestServiceServer` cho client quan trọng nhất (learning `RestLearningContentClient`, assessment `ContentPackageClient`, game `ContentSnapshotClient`, community `UserPublicProfileClient`, content `LibraryVocabularySenseClient`): downstream 5xx liên tiếp → sau ngưỡng caller nhận 503 mà server giả không còn nhận request.
3. Test fallback tên hiển thị và streak.
4. Test bulkhead LLM: 5 lời gọi đồng thời, lời thứ 5 nhận `AI_BUSY` ngay.
5. Chạy lại toàn bộ test client hiện có (khóa hành vi khi downstream khỏe).

### Code

6. Tạo module, cấu hình mặc định.
7. Bọc client theo thứ tự rủi ro: learning → assessment → game → community → content.
8. Bỏ retry tự viết trong `OpenAiCompatibleContentGenerator` nếu trùng chức năng (giữ hành vi qua test).
9. Tài liệu: bảng tên breaker, ngưỡng, fallback trong `docs/system-architecture.md`.

## Success Criteria

- [ ] Test 1–5 xanh.
- [ ] Thử tay: dừng content-service, gọi bài học qua gateway nhiều lần → các lần sau trả 503 trong < 100 ms; bật lại content → hồi phục sau ≤ 30 giây.
- [ ] Metric `resilience4j_circuitbreaker_state` có ở `/actuator/prometheus`.
- [ ] Downstream chậm quá 15 giây qua gateway → 504 định dạng lỗi chung; route AI chịu được 120 giây.
- [ ] 6 contract test (3 cặp provider/consumer) xanh; đổi tên một field trong response provider làm test provider đỏ.

## Risk Assessment

- **Retry POST gây trùng:** chỉ retry GET hoặc POST có idempotency key (đánh dấu tường minh trong code).
- **Breaker dùng chung theo đích che lỗi cục bộ của một endpoint:** chấp nhận; theo dõi metric, tách instance nếu cần.
- **Ngưỡng sai gây mở breaker khi tải cao:** ngưỡng cấu hình được, bắt đầu rộng.
- **Timeout gateway quá ngắn cắt request hợp lệ (AI, upload):** route riêng 120 giây; theo dõi metric 504.

## Khung code cho Codex

Điều kiện bắt đầu: phase 2 đã có (`common-observability`, Micrometer). Spring Cloud 2025.0.0 (Spring Boot 3.5) → dùng `io.github.resilience4j:resilience4j-spring-boot3` và `resilience4j-micrometer`, version quản lý ở `pom.xml` gốc. Nếu xung đột version → `BLOCKED`, dán lỗi.

### Module `shared/common-resilience`

```java
package com.ieltspath.commonresilience;

/** Cách gọi quyết định retry. Không có giá trị nào khác. */
public enum CallKind {
    READ,               // GET: retry được
    IDEMPOTENT_WRITE,   // POST có idempotencyKey/referenceId/submissionId: retry được
    WRITE               // còn lại, gồm mọi lời gọi LLM: không retry
}

/** Breaker đang mở, bulkhead đầy, hoặc hết lượt retry vì lỗi kết nối/5xx. Client đổi nó sang exception "Unavailable" của service. */
public class DownstreamUnavailableException extends RuntimeException {
    public DownstreamUnavailableException(String target, String reason, Throwable cause) { }
    public String target() { }
    public String reason() { }   // "circuit-open" | "bulkhead-full" | "failed"
}

public class ResilientCall {
    public ResilientCall(CircuitBreakerRegistry breakers, RetryRegistry retries, BulkheadRegistry bulkheads) { }
    /**
     * target: content | access | user | assessment | library | learning | llm.
     * Thứ tự: bulkhead (chỉ khi registry có instance target) → circuit breaker → retry (chỉ READ, IDEMPOTENT_WRITE).
     * Tính lỗi cho breaker và retry: ResourceAccessException, RestClientResponseException 5xx.
     * Không tính lỗi: 4xx (ném nguyên exception gốc, không bọc).
     */
    public <T> T call(String target, CallKind kind, Supplier<T> request) { }
    public void run(String target, CallKind kind, Runnable request) { }
}

@AutoConfiguration public class CommonResilienceAutoConfiguration { /* bean ResilientCall */ }
```

Đăng ký qua `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (theo `common-storage`).

### Cấu hình chung `infra/config-server/config-repo/application.yaml`

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50
        slow-call-duration-threshold: 5s
        slow-call-rate-threshold: 80
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
    instances:
      content:    { base-config: default }
      access:     { base-config: default }
      user:       { base-config: default }
      assessment: { base-config: default }
      library:    { base-config: default }
      learning:   { base-config: default }
      llm:        { base-config: default, slow-call-duration-threshold: 60s }
  retry:
    configs:
      default: { max-attempts: 3, wait-duration: 200ms, enable-exponential-backoff: true, exponential-backoff-multiplier: 2 }
    instances:
      content: { base-config: default }
      access:  { base-config: default }
      user:    { base-config: default }
      assessment: { base-config: default }
      library: { base-config: default }
      learning: { base-config: default }
  bulkhead:
    instances:
      llm: { max-concurrent-calls: 4, max-wait-duration: 0 }
```

### Áp vào client (đúng bảng, không thêm client khác)

Mỗi lời gọi HTTP trong client bọc `resilientCall.call(target, kind, () -> ...)`; bắt `DownstreamUnavailableException` ném exception "Unavailable" sẵn có. Giữ nguyên timeout hiện có; thiếu connect timeout thì thêm 2 giây.

| Client | target | kind | Exception đổi sang |
|---|---|---|---|
| learning `RestLearningContentClient` | content | READ (GET), WRITE (`progressMetadata` POST) | `ContentUnavailableException` hiện dùng trong client |
| learning `RestAccessClient` | access | READ cho GET; IDEMPOTENT_WRITE cho trừ điểm (có `idempotencyKey`) | `AccessUnavailableException` |
| learning `RestAssessmentStudyContextClient`, `RestEssayPromptActivationClient`, `RestHumanReviewClient` | assessment | GET → READ, POST → WRITE | exception hiện có của client |
| learning `RestDisplayNameClient`, `UserServiceStudyTimezones` | user | READ | **fallback:** tên rỗng / timezone mặc định hiện có |
| learning `OpenAiCompatibleLlmClient` | llm | WRITE | `LlmUnavailableException`; bulkhead đầy → 503 code `AI_BUSY` |
| assessment `ContentErrorTypeClient`, `ContentPackageClient`, `ContentRubricClient` | content | READ | `ContentUnavailableException` |
| assessment `AccessHumanGradingCreditClient` | access | IDEMPOTENT_WRITE (khóa `submissionId`) | exception hiện có |
| assessment `RestUserDirectory`, `UserExaminerClient` | user | READ | `UserServiceUnavailableException` / `ExaminerUnavailableException` |
| assessment `OpenAiCompatibleEssayGrader` | llm | WRITE | exception hiện có của grader |
| community `LearningStreakClient` | learning | READ | **fallback:** streak 0 |
| community `UserPublicProfileClient` | user | READ | **fallback:** hồ sơ rỗng như khi user không tồn tại |
| content `LibraryVocabularySenseClient` | library | READ | `VocabularyServiceUnavailableException` |
| content `OpenAiCompatibleContentGenerator` | llm | WRITE | exception hiện có; **xóa** vòng retry tự viết, giữ test hành vi |
| game `ContentSnapshotClient` | content | READ | exception hiện có |
| game `CurrentPlayerProfileClient` | user | READ | exception hiện có |
| game `WordlyWordClient` | library | READ | exception hiện có |

Client không có exception "Unavailable" riêng → dùng exception mà client đang ném khi `ResourceAccessException`; không tạo exception mới trừ khi client hiện ném `RuntimeException` trần (ghi trong báo cáo).

### Timeout gateway (`api-gateway.yaml`)

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          httpclient:
            connect-timeout: 2000
            response-timeout: 15s
```

Route AI và upload (đã liệt kê ở phase 3): thêm `metadata: { response-timeout: 120000, connect-timeout: 2000 }`. Kiểm `GatewayErrorAttributes` trả 504 theo định dạng lỗi chung khi timeout; nếu không → sửa ở `infra/api-gateway/.../error/GatewayErrorAttributes.java`.

### Contract test

Thư mục mẫu: `docs/contracts/fixtures/`.

| File | Nội dung |
|---|---|
| `learning-content-lesson-v1.response.json` | lấy từ response thật của content cho một bài seed (chạy test provider, ghi ra, rút gọn mảng còn 1 phần tử) |
| `access-points-debit-v1.request.json`, `access-points-debit-v1.response.json` | theo `DebitPointsRequest` (`userId, amount, referenceType, referenceId, idempotencyKey, description`) và response hiện có |
| `access-human-grading-consume-v1.request.json`, `access-human-grading-consume-v1.response.json` | theo `HumanGradingCreditRequest` (`submissionId`) và `HumanGradingCreditResponse` |

Helper đọc mẫu: mỗi service một lớp test `ContractFixtures` (đọc `Path.of("../../docs/contracts/fixtures", name)`; không đưa vào `shared/`). So khớp: mọi field của mẫu phải có trong JSON thật, cùng kiểu JSON (string/number/boolean/object/array/null), đệ quy; field thừa được phép.

### File được sửa

| Hành động | File |
|---|---|
| Tạo | `shared/common-resilience/` (pom, `CallKind`, `DownstreamUnavailableException`, `ResilientCall`, `CommonResilienceAutoConfiguration`, `AutoConfiguration.imports`, README, test) |
| Sửa | `pom.xml` gốc (module, version resilience4j), `services/{learning,assessment,community,content,game}-service/pom.xml` |
| Sửa | 22 client trong bảng; `GlobalExceptionHandler` của service có `AI_BUSY` |
| Sửa | `infra/config-server/config-repo/{application,api-gateway}.yaml` |
| Sửa (nếu cần) | `infra/api-gateway/src/main/java/com/ieltspath/apigateway/error/GatewayErrorAttributes.java` |
| Tạo | `docs/contracts/fixtures/*.json` (5 file), lớp test `ContractFixtures` trong learning, content, access, assessment |
| Sửa | `docs/system-architecture.md` (bảng breaker, timeout gateway, contract fixtures) |

### Tên test bắt buộc

- `ResilientCallTest`: `opensAfterFailureThreshold`, `doesNotCallDownstreamWhileOpen`, `closesAfterSuccessfulHalfOpenCalls`, `clientErrorsDoNotOpenBreaker`, `retriesReadOnServerError`, `doesNotRetryWrite`, `retriesIdempotentWrite`, `rejectsImmediatelyWhenBulkheadFull`.
- Mỗi service một test client: learning `RestLearningContentClientResilienceTest`, assessment `ContentPackageClientResilienceTest`, game `ContentSnapshotClientResilienceTest`, community `UserPublicProfileClientFallbackTest`, content `LibraryVocabularySenseClientResilienceTest` — ca chung: `returnsUnavailableQuicklyOnceBreakerOpens` (server giả không nhận thêm request khi breaker mở).
- `RestDisplayNameClientFallbackTest`, `LearningStreakClientFallbackTest`.
- `OpenAiCompatibleLlmClientBulkheadTest`: `fifthConcurrentCallIsRejectedAsBusy`.
- Gateway `GatewayTimeoutTest`: `slowDownstreamReturnsGatewayTimeout`, `aiRouteAllowsLongerResponse`.
- Contract: content `LessonContentContractProviderTest`, learning `LessonContentContractConsumerTest`, access `PointsDebitContractProviderTest`, learning `PointsDebitContractConsumerTest`, access `HumanGradingConsumeContractProviderTest`, assessment `HumanGradingConsumeContractConsumerTest`.

### Lệnh kiểm chứng

```
mvn -B -ntp -pl shared/common-resilience -am verify
mvn -B -ntp verify
```

### Kiểm tay (Claude)

Dừng content-service; gọi bài học qua gateway 15 lần → các lần sau trả 503 < 100 ms; bật lại → hồi phục ≤ 30 giây; `/actuator/prometheus` của learning có `resilience4j_circuitbreaker_state{name="content"}`.

### Dừng và hỏi

- Client trong bảng không tồn tại hoặc đã đổi tên.
- Không lấy được response thật của content cho mẫu bài học (seed không có bài phù hợp).
- Version resilience4j xung đột với Spring Boot hiện tại.
