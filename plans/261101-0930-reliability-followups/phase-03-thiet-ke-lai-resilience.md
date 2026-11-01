---
phase: 3
title: "Thiết kế lại resilience (thay phase 5 cũ)"
status: pending
priority: P2
effort: "L"
dependencies: [old-2, old-3, 2]
---

# Phase 3: Thiết kế lại resilience

Người làm: Codex, sau khi phase 3 cũ và [phase 2](./phase-02-chinh-phase-3-auth-va-xff.md) qua cổng. Luật cứng và mẫu báo cáo: mục "Khung kiểm soát Codex" trong [plan.md cũ](../261031-2145-backend-reliability-patterns/plan.md). Phase này **thay** [phase 5 cũ](../261031-2145-backend-reliability-patterns/phase-05-resilience-va-goi-service.md); chỉ giữ nguyên mục "Contract test" và danh sách 22 client của phase cũ.

## Overview

Thêm circuit breaker, retry và bulkhead cho lời gọi HTTP giữa service, đặt timeout và retry an toàn ở gateway. Sửa bốn lỗi thiết kế của bản cũ: bọc sai chỗ so với khối `catch` của client, retry nằm trong breaker, POST truy vấn bị coi là ghi, timeout gateway va chạm timeout client và LLM.

## Hiện trạng (đã kiểm)

- `RestLearningContentClient.response()` (dòng 230–246) bắt `RestClientResponseException`, `ResourceAccessException`, `RestClientException` và đổi thành `LearningRequestException` (404 → `NOT_FOUND`, 503 → `CONTENT_UNAVAILABLE`, còn lại → `CONTENT_FAILURE`). Các client khác theo cùng mẫu.
- Learning có 7 POST chỉ để truy vấn (`progressMetadata`, `practiceSetAvailability`, `searchPracticeSets`, `prerequisites`, `questionDifficulties`, `workItemSubjects`, `suggestionCandidates`).
- Timeout client: connect 2 s, read 3–5 s. LLM: learning `grading-timeout-seconds: 20` (connect 5 s), assessment đọc 45 s, content có `BUSY_RETRY_DELAYS` 5 s + 15 s.
- Gateway chưa đặt `connect-timeout` và `response-timeout`; chưa có Retry filter; `GatewayErrorAttributes` đã có.
- Gọi giữa service qua hostname Docker cố định (`CONTENT_SERVICE_URL=http://content-service:8082`), không qua Eureka.

## Thiết kế

### Vị trí bọc và thứ tự

`rawRequest` chỉ chứa lời gọi `RestClient` thô. `ResilientCall` đặt ngoài nó; **khối `catch` hiện có của client giữ nguyên** và vẫn bao ngoài `ResilientCall`.

```java
private <T> T response(CallKind kind, Supplier<T> rawRequest) {
    try {
        T body = resilient.call("content", kind, rawRequest);
        if (body == null) throw contentFailure();
        return body;
    } catch (DownstreamUnavailableException open) {   // mới: breaker mở hoặc bulkhead đầy
        throw contentUnavailable();
    } catch (RestClientResponseException exception) { /* nguyên vẹn */ }
    catch (ResourceAccessException exception) { /* nguyên vẹn */ }
    catch (RestClientException exception) { /* nguyên vẹn */ }
}
```

Thứ tự trong `ResilientCall` (ngoài vào trong): **Retry → CircuitBreaker → Bulkhead → rawRequest**.

- Mỗi lần thử được breaker ghi nhận riêng; breaker đã mở thì không retry (`CallNotPermittedException`, `BulkheadFullException` ném ngay, đổi thành `DownstreamUnavailableException(target, reason)`).
- Chỉ **ghi nhận lỗi và retry** cho `ResourceAccessException` (kết nối, timeout đọc) và `RestClientResponseException` mã 5xx. Mọi exception khác (4xx, lỗi giải mã, exception nghiệp vụ ném từ supplier như 401 khi chưa có JWT) đi qua nguyên vẹn, không đếm, không retry.
- Hết lượt retry: ném lại **exception gốc**, không bọc. Vì vậy ánh xạ lỗi cũ của client vẫn chạy, và 404 vẫn thành `NOT_FOUND`.

### Loại lời gọi

```java
public enum CallKind {
    READ,               // không tác dụng phụ ở phía nhận, kể cả khi dùng POST để mang body truy vấn
    IDEMPOTENT_WRITE,   // ghi, phía nhận chống trùng theo khóa (idempotencyKey, submissionId)
    WRITE               // còn lại, gồm mọi lời gọi LLM: không retry
}
```

- Mỗi phương thức client ghi rõ `CallKind` tại chỗ gọi, không suy ra từ HTTP verb. Bảy POST truy vấn của learning là `READ`.
- `IDEMPOTENT_WRITE` chỉ cho `RestAccessClient` (trừ điểm theo `idempotencyKey`) và `AccessHumanGradingCreditClient` (theo `submissionId`). Mỗi cái cần test phía nhận: gọi hai lần cùng khóa chỉ trừ một lần.
- Số lần thử: tối đa 2 (1 lần retry, 200 ms). Quy tắc: `số lần thử × read timeout + backoff ≤ response-timeout của route − 3 s`.

### Ánh xạ phản hồi

| Tình huống | HTTP | `code` | Header |
|---|---|---|---|
| Breaker mở | 503 | `CONTENT_UNAVAILABLE`, `ACCESS_UNAVAILABLE`… (mã hiện có theo đích) | `Retry-After: 30` |
| Bulkhead LLM đầy | 503 | `AI_BUSY` | `Retry-After: 5` |
| Gateway quá hạn | 504 | `GATEWAY_TIMEOUT` | — |

Cần trường `code` của `ErrorResponse` (phase 3 cũ). Phân biệt hai trường hợp 503 qua `DownstreamUnavailableException.reason()`.

### Timeout gateway

| Nhóm route | `response-timeout` | Căn cứ |
|---|---|---|
| Mặc định | 15 s | READ tối đa 10,2 s |
| Upload | 120 s | đã có ở phase 3 cũ |
| AI của assessment (`attempts/*/submit`, `admin/ai-*`) | 120 s | grader đọc 45 s, không retry |
| AI của learning (endpoint gọi `EssayGrader`/`LlmClient`) | 60 s | chấm 20 s + connect 5 s; **chưa có trong danh sách route AI của phase 3 cũ** |
| AI của content (`admin/ai-drafts/*`) | đo: `llm.timeout × 1 + 20 s` + 10 s | có retry bận 5 s + 15 s |

Các endpoint LLM của learning phải được thêm vào tier `ai` của rate limit cùng lúc (cùng danh sách route).

Gateway thêm `Retry` mặc định: 1 lần, chỉ `GET`, chỉ khi lỗi kết nối (`ConnectException`, `AnnotatedConnectException`), backoff 100 ms. Không retry theo mã trạng thái, không bao giờ retry POST.

## Related Code Files

| Hành động | File |
|---|---|
| Tạo | `shared/common-resilience/` (pom, `CallKind`, `DownstreamUnavailableException`, `ResilientCall`, `CommonResilienceAutoConfiguration`, `AutoConfiguration.imports`, README, test) |
| Sửa | `pom.xml` gốc, `services/{learning,assessment,community,content,game}-service/pom.xml` |
| Sửa | 22 client theo danh sách phase 5 cũ; học theo mẫu `response()` ở trên |
| Sửa | `GlobalExceptionHandler` của service có `AI_BUSY` và `Retry-After` |
| Sửa | `infra/config-server/config-repo/{application,api-gateway}.yaml` (breaker, retry, bulkhead, timeout, Retry filter, route AI của learning) |
| Sửa | `infra/api-gateway/.../error/GatewayErrorAttributes.java` (504 và `GATEWAY_TIMEOUT`) |
| Tạo | `docs/contracts/fixtures/*.json` và lớp `ContractFixtures`, 6 contract test: giữ nguyên mục của phase 5 cũ |
| Sửa | `docs/system-architecture.md` (bảng breaker, quy tắc timeout, danh sách route chậm) |

## Implementation Steps

Chia hai lượt duyệt để lỗi thiết kế lộ sớm: **3a** = module, `RestLearningContentClient`, gateway; **3b** = các client còn lại.

### Test viết trước (phải đỏ)

1. `ResilientCallTest`: `rethrowsOriginalExceptionAfterRetriesSoClientMappingStillWorks`, `fourHundredFourIsNeitherCountedNorRetried`, `serverErrorOpensBreaker`, `readTimeoutCountsAsFailure`, `businessExceptionFromSupplierPassesThrough`, `doesNotRetryWhenBreakerIsOpen`, `readIsRetriedOnce`, `postMarkedReadIsRetried`, `writeIsNeverRetried`, `idempotentWriteIsRetried`, `bulkheadFullBecomesUnavailableWithoutRetry`, `breakerClosesAfterHalfOpenSuccess`.
2. Mỗi client trong 3a/3b: `MockRestServiceServer` — `notFoundStaysNotFoundAndKeepsBreakerClosed`, `repeatedServerErrorsOpenBreakerThenNoHttpRequestIsMade`, `openBreakerMapsToTheServiceUnavailableException`.
3. Handler: `bulkheadFullReturns503AiBusyWithRetryAfter`, `openBreakerReturns503WithRetryAfter`.
4. Gateway: `slowDownstreamReturns504WithStandardBody`, `aiRoutesAllowLongerResponse`, `retriesGetOnceOnConnectionRefused`, `neverRetriesPost`.
5. Phía nhận idempotent: `debitWithSameIdempotencyKeyChargesOnce`, `consumeWithSameSubmissionChargesOnce`.
6. Chạy lại toàn bộ test client hiện có làm mốc.

### Code

7. 3a: module, mẫu `response()`, `RestLearningContentClient`, cấu hình gateway, route AI của learning.
8. Gate. 3b: bọc các client còn lại theo thứ tự rủi ro (assessment → game → community → content), giữ fallback của phase 5 cũ.
9. Xóa vòng retry tự viết ở `OpenAiCompatibleContentGenerator` chỉ khi test hành vi giữ xanh; nếu không, giữ và ghi lý do.

## Lệnh kiểm chứng

```
mvn -B -ntp -pl shared/common-resilience -am verify
mvn -B -ntp -pl infra/api-gateway,services/learning-service -am verify     # lượt 3a
mvn -B -ntp verify                                                         # lượt 3b
```

## Success Criteria

- [ ] Test 1–6 xanh.
- [ ] Dừng content-service, gọi bài học qua gateway 15 lần: các lần sau 503 trong < 100 ms với `Retry-After`; bật lại, hồi phục ≤ 30 s; trong suốt thời gian đó một bài học không tồn tại vẫn trả 404, không làm breaker mở.
- [ ] `resilience4j_circuitbreaker_state{name="content"}` có ở `/actuator/prometheus` của learning.
- [ ] Downstream chậm > 15 s qua gateway → 504 định dạng chung; route AI của learning chịu 60 s.

## Risk Assessment

- **Breaker dùng chung theo đích che lỗi cục bộ một endpoint:** chấp nhận, theo dõi metric.
- **Retry nhân tải lúc downstream yếu:** chỉ 1 lần retry, chỉ READ, breaker mở thì dừng.
- **Timeout route sai cắt request hợp lệ:** số liệu LLM của content phải đo (đọc `content.llm.timeout`), ghi vào báo cáo trước khi chốt.
- **Rollback:** tắt bằng cấu hình (`resilience4j` instance rộng ngưỡng) hoặc bỏ lời gọi `resilient.call`; ánh xạ lỗi cũ không bị đổi nên không cần dọn.

## Dừng và hỏi

- Client có `catch` bao quanh cả `RestClient` lẫn xử lý khác, không tách được `rawRequest`.
- Không xác định được endpoint nào của learning gọi LLM, hoặc timeout LLM của content.
- Version Resilience4j xung đột với Spring Boot hiện tại (`BLOCKED`, dán lỗi).
- Phía nhận không idempotent như phase này giả định: không đánh dấu `IDEMPOTENT_WRITE`, báo lại.
