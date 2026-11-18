---
phase: 1
title: "Gỡ kẹt: gói bị gỡ, chẩn đoán trễ, HTTP ngoài khóa"
status: pending
items: [1, 2, 3]
---

# Phase 1: Gỡ kẹt bài ôn và probe

<!-- Updated: Red Team Session 1 - thêm SubmitTheoryCheck, kiểm kê đủ lời gọi Content, StartBasketReview bắt buộc, thứ tự replay, prefetch probe, ngữ nghĩa lỗi -->

## Ngữ cảnh

- Use case: `GetReviewUseCase`, `SubmitReviewUseCase`, `SubmitTheoryCheckUseCase`, `GetDiagnosticProbeUseCase`,
  `SubmitDiagnosticProbeUseCase`, `GetDiagnosticUseCase`, `StartBasketReviewUseCase`; service `DiagnosticFlow`.
- Content `GetPackageVersionContentUseCase` → `JdbcLearningContentReader.publishedPackageVersion` lọc
  `v.status = 'PUBLISHED'`; không có thì `404`. `RestLearningContentClient` đổi 404 thành
  `LearningRequestException(404, "NOT_FOUND")`, 503/IO thành `503 CONTENT_UNAVAILABLE`.
- Mẫu gọi bên ngoài trước rồi mới khóa: `SubmitLessonEssayUseCase` (`TransactionTemplate`).

## Mục 1: gói hoặc bài học bị gỡ khỏi Content

**Trước:** set ôn hoặc probe trỏ gói không còn `PUBLISHED` → `getPackageVersion` 404 → GET bài ôn/probe trả 404 mãi. Chỉ có
xử lý cho gói "bị lộ ở chỗ khác" (`discardOpenSet`, `replaceStaleSet`). Bài học bị archive → `getLesson` 404 ở cả
`GetReviewUseCase` lẫn `SubmitTheoryCheckUseCase:63`. FE tự gửi theory check khi stage `THEORY` (`ReviewPage.tsx:101-108`),
nên bài ôn kẹt ở bước lý thuyết.

**Sau:**

- `GetReviewUseCase`: 404 khi tải gói của set đang mở → `discardOpenSet` + `deleteOpenSet`, rồi đi tiếp nhánh tìm set mới
  (hết set thì `SKIPPED` như cũ). Chỉ thử lại một lần trong cùng request.
- `GetDiagnosticProbeUseCase`: 404 → `flow.replaceStaleSet` (có set khác thì đổi, không thì `NO_SET`).
- `SubmitReviewUseCase`: 404 → `409 REVIEW_SET_CLOSED`. `SubmitDiagnosticProbeUseCase`: 404 → `409 PROBE_CLOSED`. Cả hai
  chỉ áp dụng khi **không phải** lần gửi lại (xem mục 3, thứ tự replay).
- `getLesson` 404 trong `GetReviewUseCase` → lý thuyết rỗng, scope `LESSON_FALLBACK`. Bài ôn đang ở `THEORY` thì tự
  `completeTheory()` cho mọi trigger (không còn lý thuyết để đọc).
- `SubmitTheoryCheckUseCase`: `getLesson` 404 → coi như quick check rỗng (dòng 68–71 đã chấp nhận rỗng), vẫn
  `completeTheory()`.
- Chỉ bắt `NOT_FOUND` **bao quanh đúng lời gọi Content**. Use case tự ném `404 NOT_FOUND` khi bài ôn không thuộc học viên
  (`GetReviewUseCase.java:163-164`); lỗi đó không được nuốt. 503/502 vẫn ném ra như cũ.

## Mục 2: mở chẩn đoán ở lần GET kế tiếp

**Trước:** `SubmitReviewUseCase` (dòng 168–170) gọi `diagnosticFlow.startAfterSecondFailure(review)` sau khi ghi kết quả set;
hàm này gọi Content. Content lỗi → rollback cả lần nộp.

**Sau:**

- Bỏ lời gọi trong `SubmitReviewUseCase`. Lần nộp chỉ ghi kết quả set.
- `GetReviewUseCase`: nếu bài ôn `PENDING`, đang chặn, `failedSets >= 2` (đếm theo mốc ở phase 2, mục 4a), trigger khác
  `DIAGNOSTIC`, chưa có phiên → mở phiên. Có phiên `PROBING` thì trả `DIAGNOSING` như hiện tại.
- **Content lỗi khi cần mở phiên:** GET trả `503`, **không** giao set. Không được bắt lỗi rồi giao set 3 (sẽ mất chẩn đoán).
- Không có KP nền nào để dò: ghi phiên `CONCLUDED`, **không probe, kết luận rỗng** (trạng thái `CONCLUDED` đã có từ V18,
  không cần cột mới), trong **cùng** `TransactionTemplate` với quyết định giao set. GET bài ôn trả
  `diagnosticSessionId = null` cho phiên không có probe, để FE không hiện chẩn đoán chưa từng diễn ra. Phase 2 (V55) thêm
  cột `conclusion_kind` và điền `NO_PREREQUISITES` cho các phiên này.
  <!-- Updated: Implementation review - bỏ phụ thuộc phase 1 vào V55 -->
- Đường "hết set" (`startWhenOutOfSets`) giữ trong GET như hiện tại.

## Mục 3: gọi Content ngoài transaction và khóa

**Trước:** các use case trên có `@Transactional`, gọi `lock.lock(userId)` rồi gọi Content, cả trực tiếp lẫn qua service.

**Kiểm kê lời gọi Content theo use case** (gồm lời gọi gián tiếp):

| Use case | Lời gọi Content |
|----------|-----------------|
| `GetReviewUseCase` | `getLesson`, `getPackageVersion`, `searchPracticeSets`; `ErrorTypeTexts.byCode` → `activeErrorTypes` (dòng 186); `PracticeProgress.refreshPassForLesson` → `lessonPracticeSets` (dòng 124); `DiagnosticFlow.start*` → `prerequisites`, `searchPracticeSets`, `conclude` → `KnowledgePointDirectory.describe` → `getTopicSequence` |
| `SubmitReviewUseCase` | `getPackageVersion`; `errorTrapEvidence.errorTexts` → `activeErrorTypes` (127–128); `refreshPassForLesson` → `lessonPracticeSets` (167) |
| `SubmitTheoryCheckUseCase` | `getLesson` (63) |
| `GetDiagnosticProbeUseCase` | `getPackageVersion`; `replaceStaleSet` → `searchPracticeSets`, `conclude` → `getTopicSequence` |
| `SubmitDiagnosticProbeUseCase` | `getPackageVersion`; `errorTexts` → `activeErrorTypes` (95–96); `afterAnswer` → `prerequisites`, `searchPracticeSets`, `conclude` → `getTopicSequence` |
| `SkipDiagnosticUseCase` (mới, phase 2) | `conclude` → `getTopicSequence` |
| `StartBasketReviewUseCase` | `searchPracticeSets` (87–91); gọi lồng `getReview.execute` (73, 101, 105) |
| `GetDiagnosticUseCase` | `describe` → `getTopicSequence` (kiểm có khóa không; nếu có thì áp cùng mẫu) |

**Sau:** mỗi use case tách hai bước:

1. **Chuẩn bị (không transaction, không khóa):**
   - Bước đầu tiên luôn là đọc **có lọc theo user** (`findOwned(userId, reviewId)`, `findSession(userId, sessionId)`); không
     thuộc học viên → `404` **trước** mọi lời gọi Content.
   - Với lệnh nộp: kiểm replay trước (`submissions.find(requestId)` cho bài ôn; phản hồi probe đã lưu, phase 2) → có thì
     trả ngay, không gọi Content.
   - Gọi Content lấy đúng thứ trạng thái hiện tại cần, theo bảng trên. KP nền của probe chỉ lấy khi probe có thể trượt
     (luôn lấy khi nộp, vì chưa chấm). Danh mục KP (`describe`) và văn bản mã lỗi lấy ở đây rồi truyền vào.
   - Set ứng viên cho probe: xin tối đa `MAX_PROBES` ứng viên mỗi KP nền (thay vì 1), để bước 2 lọc trùng như `taken`
     hiện nay (`DiagnosticFlow.java:109-115`).
2. **Ghi (`TransactionTemplate`):** `lock.lock(userId)`, kiểm replay lần nữa, đọc lại trạng thái, kiểm dữ liệu đã lấy còn
   khớp (cùng set đang mở, cùng stage, gói ứng viên chưa bị giao hoặc lộ theo danh sách đọc trong khóa). Khớp → ghi.
   Không khớp → chạy lại bước 1 đúng một lần, vẫn lệch thì `409` (`REVIEW_SET_CLOSED` hoặc `PROBE_CLOSED`).

- `DiagnosticFlow`, `PracticeProgress.refreshPassForLesson`, `ErrorTrapEvidence.errorTexts` nhận dữ liệu đã lấy qua tham số
  thay vì tự gọi `content`/`directory`.
- `StartBasketReviewUseCase` **bắt buộc** tách theo cùng mẫu: chuẩn bị → `TransactionTemplate` ngắn chỉ để
  `insertOptional` → commit → gọi `GetReviewUseCase` (không `@Transactional`) **sau** khi commit. Không gọi lồng trong
  transaction: `TransactionTemplate` lồng sẽ tham gia transaction ngoài, khóa vẫn giữ, và 409 bên trong làm transaction ngoài
  rollback-only (500).
- Mỗi lần chuẩn bị gọi Content nhiều lần; rate limit `default` của Gateway (`api-gateway.yaml:373`) vẫn áp. Ghi vào mô tả PR.

## File sửa

- `application/usecase/GetReviewUseCase.java`, `SubmitReviewUseCase.java`, `SubmitTheoryCheckUseCase.java`,
  `GetDiagnosticProbeUseCase.java`, `SubmitDiagnosticProbeUseCase.java`, `StartBasketReviewUseCase.java`,
  có thể `GetDiagnosticUseCase.java`
- `application/service/DiagnosticFlow.java`, `PracticeProgress.java`, `ErrorTrapEvidence.java`
- Test tương ứng dưới `src/test/java/.../application/usecase/` và test Testcontainers của luồng bài ôn.

## Test

- Gói của set đang mở trả 404 → GET giao set khác; không còn set → `SKIPPED`, bài học sau mở.
- Gói của probe trả 404 → probe đổi set hoặc thành `NO_SET`; phiên kết luận được.
- Bài học bị archive, bài ôn ở `THEORY` → GET tự hoàn tất lý thuyết; `POST theory-check` không lỗi.
- Nộp set trượt lần 2 khi Content 503 → nộp thành công. GET khi Content vẫn 503 → `503`, không có set. GET khi Content hồi
  → `DIAGNOSING`.
- KP không có KP nền → GET hai lần chỉ gọi `prerequisites` một lần; `diagnosticSessionId = null`.
- Replay: nộp set rồi gửi lại cùng `requestId` sau khi set đã đóng và gói đã bị gỡ → cùng phản hồi, không 409. Tương tự
  cho probe.
- ID bài ôn/phiên của học viên khác → `404` và **không** có lời gọi Content nào (mock đếm số lần gọi).
- Hai KP nền cùng có gói đứng đầu → hai probe nhận hai gói khác nhau, không probe nào `NO_SET` sai.
- Dữ liệu đổi giữa bước 1 và bước 2 → chạy lại một lần, sau đó 409.
- Mock Content có độ trễ: không lời gọi Content nào xảy ra khi khóa học viên đang giữ (kiểm bằng cờ trong mock khóa).
- Basket start cho KP đã có bài ôn → không lỗi 500 khi `GetReview` trả 409.

## Rủi ro

- Tách hai bước làm use case dài hơn và có khe giữa hai bước; bắt buộc đọc lại trong khóa.
- Mất tuần tự hóa theo học viên ở bước chuẩn bị: một học viên có thể gửi nhiều GET song song, mỗi GET gọi Content nhiều
  lần. Rate limit Gateway giới hạn; nếu đo thấy tải Content tăng thì thêm bulkhead theo user ở `ResilientCall`.
