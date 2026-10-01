---
phase: 1
title: "Contract và bảo vệ access"
status: completed
priority: P1
dependencies: []
effort: "1.5 ngày"
---

# Phase 1: Contract và bảo vệ access

## Context Links

- [Brainstorm](../reports/brainstorm-260930-0737-writing-speaking-practice-db-readiness-report.md) §6
- Contract của plan 1640: `docs/contracts/answer-spec-v1.md`, `lesson-learning-v1.md`, `learning-content-internal-v1.md`
- `services/access-service/src/main/java/com/group01/access/api/controller/InternalAccessController.java`
- `services/access-service/src/main/java/com/group01/access/application/usecase/DebitPointsUseCase.java`

## Overview

Viết contract cho Writing trong bài học, rồi đóng lỗ của access trước khi có bên gọi thật. Hiện 4 endpoint "internal" của
access nằm ở `/api/access/**`, không `@PreAuthorize`, và Gateway route tới được: học viên tự `refund` cho mình, đọc
entitlement hay trừ credit chấm tay của người khác theo `userId` trên path. Grep cho thấy chưa service nào gọi 4 endpoint
này (chỉ có test). Chỉ **debit** là ai-learning cần: chuyển sang `/internal/access`. Ba endpoint còn lại chỉ cho `ADMIN`
(Validation Session 2).

## Requirements

**Contract:**
- `docs/contracts/lesson-writing-v1.md` (mới). Ghi ngày người dùng duyệt ở đầu file.
  - `POST /api/ai-learning/lessons/{lessonId}/essays/{blockId}/submissions` với `{requestId, essayText}`.
  - `GET /api/ai-learning/writing-submissions/{submissionId}`.
  - Response thành công, body lỗi, bảng mã lỗi (mục Architecture).
  - Trạng thái `GRADING`, `GRADED`, `FAILED`, `PAYMENT_PENDING` và luật idempotency theo `requestId`.
  - Schema `result`: 4 tiêu chí `TR`, `CC`, `LR`, `GRA`, mỗi tiêu chí có `band`, `strengths[]`, `improvements[]`;
    `corrections[]` gồm `{excerpt, suggestion, category}`, tối đa 10; `overallBand`; `summary`.
  - Nói rõ band là **ước lượng**, không phải điểm thi chính thức.
  - Luật: khối essay không tính vào điều kiện hoàn thành bài; bài mẫu chỉ trả khi đạt; point chỉ trừ khi chấm thành công.
- `answer-spec-v1.md`: thêm dạng `ESSAY` `{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":6.0}`.
  - Bộ chấm tự động coi `ESSAY` là **không chấm được**, giống spec `{}`.
  - `passBand` nằm trong 4.0–9.0, bước 0.5.
  - `answer-spec-v1-vectors.json`: thêm ca ESSAY → không chấm được.
- `learning-content-internal-v1.md`: khối `EXERCISE` có đúng 1 câu `question_type = ESSAY` là **khối essay**, thêm trường
  `blockKind: "EXERCISE" | "ESSAY"` vào payload khối. Câu essay trả `stem`, `answerSpec`, `explanation` (endpoint nội bộ).
- `lesson-learning-v1.md`:
  - `GET /lessons/{id}` trả khối essay với `blockKind = "ESSAY"`, `task`, `minWords`, `passBand` và
    `latestSubmission {id, status, overallBand, passed}`, không có `explanation` khi chưa đạt.
  - Bài xong khi mọi khối `EXERCISE` **không phải essay** đã đạt. Bài chỉ có khối essay thì dùng `POST /complete`.
  - `POST /lessons/{id}/exercises/{blockId}/submissions` vào khối essay → 409 `ESSAY_BLOCK`.

**Access:**
- **Debit:** chuyển sang `POST /internal/access/points/debit` (controller mới `InternalPointsController` với
  `@RequestMapping("/internal/access")`, hoặc đổi mapping riêng của method; không để hai path cùng sống).
  - Chỉ hành động cho **chính subject của token**: `userId` trong body phải bằng subject của `CurrentUserProvider`, nếu
    không thì 403. Kiểm ở use case: controller truyền `actorUserId` vào `DebitPointsCommand`, không đặt luật trong controller.
  - Replay theo idempotency key: ledger cũ khác `userId`, `amount`, `referenceType` hoặc `referenceId` → 409
    `DuplicateIdempotencyException`; cùng nội dung → trả ledger cũ (như hiện tại).
  - Hai request song song cùng key: request sau vi phạm UNIQUE `idempotency_key` (`V1:124`) và transaction rollback. Bắt
    `DataIntegrityViolationException` ở **lớp ngoài transaction** (tách phần ghi sang bean `@Transactional` riêng, giống
    `AttemptCreator` của assessment), đọc lại ledger theo key và áp luật replay ở trên. Không trả 500.
  - Lỗi optimistic lock của ví (`@Version`) khi debit song song khác key: giữ nguyên (5xx); ai-learning gửi lại được.
- **Refund, consume-human-grading, entitlement:** giữ path cũ (`/api/access/points/refund`,
  `/api/access/users/{userId}/consume-human-grading`, `/api/access/users/{userId}/entitlement`), thêm
  `@PreAuthorize("hasRole('ADMIN')")`. Không thêm `actorUserId`, không đổi use case (`GetUserEntitlementUseCase.execute(UUID)`
  giữ nguyên, không có command).
- Không đổi `LearnerAccessController` (`GET /api/access/me/points` giữ nguyên, ai-learning dùng để kiểm số dư, đọc
  trường `balance`). <!-- Updated: Validation Session 1 - sửa đường số dư theo LearnerAccessController.java:23,42 -->
- Tạo `services/access-service/README.md` (chưa có): endpoint học viên, admin, nội bộ; luật subject của `/internal/access/**`;
  ghi rằng `/api/access/points/debit` đã chuyển sang `/internal/access/points/debit`; refund, consume, entitlement chỉ ADMIN.
  <!-- Updated: Validation Session 1 - README access chưa tồn tại, người dùng chọn tạo mới -->

## Architecture

Mã lỗi của `lesson-writing-v1` (body `{detail, code}` theo handler của 1640):

| HTTP | `code` | Khi |
| --- | --- | --- |
| 403 | `LESSON_LOCKED`, `TOPIC_LOCKED`, `REVIEW_REQUIRED` | Cổng bài học |
| 404 | `NOT_FOUND` | Bài, khối, hoặc submission không thuộc user |
| 409 | `NOT_ESSAY_BLOCK` | Nộp essay vào khối không phải khối essay |
| 409 | `ESSAY_BLOCK` | Nộp bài tập tự chấm (`/exercises/{blockId}/submissions`) vào khối essay |
| 409 | `REQUEST_CONFLICT` | `requestId` đã dùng cho user, bài hoặc khối khác |
| 409 | `GRADING_IN_PROGRESS` | Khối đang có bài `GRADING` chưa quá hạn |
| 422 | `ESSAY_EMPTY`, `ESSAY_TOO_SHORT`, `ESSAY_TOO_LONG` | Rỗng; dưới 50 từ; trên 1.000 từ hoặc 10.000 ký tự. Kiểm trước khi kiểm số dư hay gọi LLM |
| 402 | `INSUFFICIENT_POINTS` | Kiểm số dư thiếu (chưa gọi LLM), hoặc debit bị 402 (`PAYMENT_PENDING`) |
| 503 | `GRADING_UNAVAILABLE` | LLM chưa cấu hình, lỗi, timeout, hoặc output sai schema; không trừ point |
| 503 | `PAYMENT_UNAVAILABLE` | Access lỗi hoặc 401 lúc debit; dòng thành `PAYMENT_PENDING`, gửi lại cùng `requestId` |

## Related Code Files

- Create: `docs/contracts/lesson-writing-v1.md`
- Modify: `docs/contracts/answer-spec-v1.md`, `docs/contracts/answer-spec-v1-vectors.json`,
  `docs/contracts/learning-content-internal-v1.md`, `docs/contracts/lesson-learning-v1.md`
- Modify (access): `api/controller/InternalAccessController.java` (bỏ debit, thêm `@PreAuthorize` ADMIN cho 3 method còn
  lại), `application/command/DebitPointsCommand.java` (thêm `actorUserId`), `application/usecase/DebitPointsUseCase.java`
  (kiểm subject, luật replay), `api/exception/GlobalExceptionHandler.java` (403, 409)
- Create (access): controller debit dưới `/internal/access` (nếu tách), bean ghi `@Transactional` tách khỏi phần bắt UNIQUE
- Create (access): `services/access-service/README.md`
- Tests (access): `api/controller/InternalAccessControllerTest.java`, `application/usecase/DebitPointsUseCaseTest.java`
  (3 chỗ tạo `DebitPointsCommand`). Bên gọi `DebitPointsCommand` ngoài test: chỉ `InternalAccessController.java:38`.
  `RefundPointsUseCaseTest` không đổi.

## Implementation Steps

**Tests Before:**
1. Chạy `mvn -q -pl services/access-service -am test` và lưu baseline. Bổ sung test cho hành vi giữ nguyên: debit đủ
   point trừ đúng; debit thiếu → 402; replay cùng key → cùng ledger, ví trừ một lần.

**Tests After** (viết trước code):
2. `InternalAccessControllerTest` (security thật):
   - `/internal/access/points/debit` hoạt động; `/api/access/points/debit` → 404;
   - CUSTOMER gọi refund, consume-human-grading, entitlement → 403; ADMIN → 2xx như cũ.
3. Use case:
   - debit với `actorUserId` ≠ `userId` → 403, ví không đổi;
   - replay key với `userId`, `amount` hoặc `referenceId` khác → 409; cùng nội dung → ledger cũ, ví trừ một lần;
   - hai debit song song cùng key (Testcontainers) → cả hai nhận cùng ledger, không 500, ví trừ một lần;
   - `LearnerAccessController` `GET /api/access/me/points` không đổi.

**Implement:**
4. Chuyển debit sang `/internal/access`, thêm `actorUserId`, kiểm subject và luật replay ở use case, bắt UNIQUE ngoài
   transaction, `@PreAuthorize` ADMIN cho 3 endpoint còn lại, map lỗi 403/409 qua `GlobalExceptionHandler`.
5. Viết 4 file contract. Người dùng duyệt contract trước phase 2–4.

**Regression Gate:**
```powershell
mvn -q -pl services/access-service -am test
```

## Success Criteria

- [ ] Debit chỉ còn ở `/internal/access/points/debit`, từ chối `userId` khác subject.
- [ ] Refund, consume-human-grading, entitlement chỉ ADMIN.
- [ ] Replay idempotency key không trả ledger của người khác; debit song song cùng key không 500.
- [ ] Contract `lesson-writing-v1` đã duyệt; `answer-spec-v1` có ESSAY và vector tương ứng.

## Risk Assessment

- **Có client ngoài repo gọi đường cũ:** repo chưa có frontend, grep không thấy bên gọi; ghi trong README access.
- **`/internal/**` vẫn nhận mọi internal JWT:** Gateway chặn (phase 2 plan 1640). Kiểm subject là lớp phòng thủ thêm.
  Credential riêng cho service vẫn nằm ngoài phạm vi.

## Security Considerations

- Đổi public route là thay đổi contract (AGENTS §5). Người dùng đã duyệt trong brainstorm 2026-09-30; ghi vào README access.
- Không log số dư hay idempotency key đầy đủ; chỉ log id.

## Next Steps

- Phase 2 và 3 chạy song song sau khi contract được duyệt.
