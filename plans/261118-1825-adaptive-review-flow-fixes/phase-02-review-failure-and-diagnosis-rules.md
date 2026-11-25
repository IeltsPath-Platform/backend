---
phase: 2
title: "Luật trượt bài ôn và chẩn đoán"
status: pending
items: [4, 4a, 4b, 4c, 5, 6, 7]
---

# Phase 2: Luật trượt bài ôn và chẩn đoán

Mục 4 và 4a đã chốt ngày 2026-11-18: phương án A với N = 4, và (a). Bỏ qua chẩn đoán chỉ cho phép sau khi đã làm ít nhất
một probe (red team, người dùng chốt).

<!-- Updated: Red Team Session 1 - mốc đếm set trượt thay cho reset, PracticeClearance, danh sách chờ, skip cần ≥1 probe, outcome phiên, rollback -->

## Ngữ cảnh

- `domain/aggregate/ReviewItem.java`: `recordSetResult` (134–156); lý thuyết chỉ ở lần trượt đầu (149); bài ôn tùy chọn
  trigger `PRACTICE` trượt thì thành bắt buộc (145).
- **Số set trượt không được lưu:** `JdbcReviewItemRepository.java:38` đếm
  `count(*) FROM review_sets f WHERE f.review_item_id = r.id AND f.passed = FALSE` mỗi lần đọc.
- Bốn chỗ nâng bài ôn lên bắt buộc: `promoteToBlocking` (`JdbcReviewItemRepository.java:220`, gọi từ
  `ReviewReevaluation.java:75` và `DiagnosticFlow.java:155`), `promoteSuggestionsToBlocking` (230, gọi từ
  `SubmitPracticeAttemptUseCase.java:276`), nâng bài ôn tiêu chí Writing (252).
- `PracticeClearance.java:72,89`: `nonePending` tính **mọi** bài ôn `PENDING`, kể cả không chặn.
- `JdbcDiagnosticRepository.java:136`: `pendingWaitingOn`, gọi từ `GetReviewUseCase.java:95` và `GetDiagnosticUseCase.java:43`.
- `GetDiagnosticUseCase.java:39-56`: tự suy `rootIsGap`, `switch` đầy đủ trên trạng thái probe.
- CHECK constraint: `V18__diagnostics.sql:8` (session `PROBING`/`CONCLUDED`), `:26` (probe status), `V5__review_ladder.sql:4`
  (`theory_reason`), `V48__speaking_criterion_reviews.sql:4-6` (`trigger_kind`). Migration mới nhất: V54.

## Migration

- `V55__diagnostic_skip_and_outcome.sql`: CHECK probe status thêm `SKIPPED`; cột `diagnostic_sessions.conclusion_kind`
  (`GAPS_FOUND`, `ROOT_IS_GAP`, `INCONCLUSIVE`, `NO_PREREQUISITES`), null cho phiên cũ (đọc như cũ); `UPDATE` điền
  `NO_PREREQUISITES` cho phiên `CONCLUDED` không có probe nào (do phase 1 tạo); cột `diagnostic_probes.response jsonb` lưu
  phản hồi nộp probe để replay.
- `V56__review_failure_baseline.sql`: cột `review_items.failures_counted_from timestamptz` (null = đếm tất cả);
  `review_items.unblocked_at timestamptz`; CHECK `theory_reason` thêm `REPEATED_FAIL`; CHECK `trigger_kind` thêm `PRIORITY`,
  `DECAYED` (dùng ở phase 3).
- Không xóa hay sửa dòng `review_sets` nào: các dòng đó là nguồn của `assignedPackageIds` (`:362-368`) và
  `revealedPackageIds`; xóa sẽ làm gói đã lộ đáp án bị giao lại.

## Mục 4: trượt bài ôn bắt buộc mãi

**Trước:**

| Lần trượt | Kết quả |
|-----------|---------|
| 1 | Lý thuyết + quick check, rồi set mới |
| 2 | Mở chẩn đoán dò ngược, rồi set mới |
| 3, 4, 5… | Set mới, không giới hạn, tới khi đạt hoặc hết gói (`SKIPPED`) |

**Sau (phương án A, N = 4):**

| Lần trượt | Kết quả |
|-----------|---------|
| 1 | Như cũ |
| 2 | Như cũ (mở dò ngược ở GET kế tiếp, phase 1) |
| 3 | **Lý thuyết + quick check lần nữa**, theory reason `REPEATED_FAIL` |
| 4 (= N) | **`blocking = false`**, vẫn `PENDING`, ghi `unblocked_at`. Bài học, Practice, thi topic thôi bị chặn. Bài ôn hiện trong giỏ ôn (lý do `UNBLOCKED`) và theo luật bài ôn tùy chọn: một set, đạt thì `DONE`, trượt thì `SKIPPED` |

Thay đổi:

- `findOwned`/`findPending` đếm set trượt có `submitted_at > failures_counted_from` (null thì đếm tất cả). Báo cáo
  (`findBlockingByPoint`, `sets_answered` ở `:74`) giữ đếm tổng.
- `ReviewItem.recordSetResult` nhận N (truyền từ use case, không đọc config trong domain):
  - nhánh bắt buộc: `failedSets == 1` hoặc `== 3` → `THEORY` (lần 3 dùng `REPEATED_FAIL`);
  - `failedSets >= N` → `blocking = false`, status `PENDING`, stage `PRACTICE`, đánh dấu vừa hạ cấp (repository ghi
    `unblocked_at`).
- Cấu hình `learning.review.unblock-after-failed-sets: 4` ở `infra/config-server/config-repo/learning-service.yaml` + lớp
  `*Properties`; kiểm `>= 4` khi khởi động (lần trượt thứ 3 đã dành cho bước lý thuyết nên không thể thả bài ôn sớm hơn
  lần thứ 4).
- **Practice không còn bị chặn bởi bài ôn đã hạ cấp:** `PracticeClearance.ReviewFact` thêm cờ `unblocked`; bài ôn có
  `unblocked_at` tính như đã xong ở `nonePending` (dòng 72, 89). Bài ôn tùy chọn khác giữ luật cũ. **Không** thêm lời gọi
  `refreshPassForLesson` (không có tác dụng: `ATTEMPT_NOT_BLOCKED` không được lưu, `PracticeProgress.java:78`).
- `ReviewSubmissionResult` thêm `blocking` (bài ôn sau khi chấm còn chặn không). Phản hồi cũ lưu cho replay thiếu trường
  này → đọc là `null`.
- `ReviewResult.maxFailedSets` = N cho bài ôn đang chặn, `null` cho bài ôn tùy chọn.
- Cập nhật javadoc `ReviewItem` (dòng 15–26).

### Mục 4a: nâng bài ôn đã hạ cấp lên bắt buộc lại

**Trước:** các câu nâng cấp chỉ `SET blocking = TRUE`; số set trượt đếm lại toàn bộ → trượt 1 set nữa là 5 ≥ 4, hạ cấp ngay.

**Sau (a):** cả bốn chỗ nâng cấp (3 câu SQL + `DiagnosticFlow` dùng `promoteToBlocking`) ghi
`failures_counted_from = clock_timestamp()`, `stage = 'PRACTICE'`, `theory_reason = NULL`, `unblocked_at = NULL`. Số set
trượt tính lại từ 0 mà không mất lịch sử. Chẩn đoán vẫn không chạy lại (mỗi bài ôn gốc một phiên), chấp nhận.

### Mục 4b: bài ôn gốc chờ KP hổng

**Trước:** `pendingWaitingOn` lọc `w.status = 'PENDING'`. KP hổng bị hạ cấp vẫn `PENDING` → gốc chờ mãi.

**Sau:** khi bài ôn KP hổng bị hạ cấp, trong **cùng transaction** xóa nó khỏi `waiting_on_review_ids` của mọi bài ôn gốc
(`array_remove`). Nâng cấp lại sau đó không thêm lại, nên gốc không bị đổi sang chờ khi đang có set mở. Cả hai nơi gọi
`pendingWaitingOn` hưởng chung.

### Mục 4c: bài ôn tùy chọn trigger `PRACTICE` tự chặn lại

**Trước:** dòng 145: mọi bài ôn tùy chọn trigger `PRACTICE` trượt đều thành bắt buộc.

**Sau:** chỉ khi chưa từng trượt và chưa hạ cấp (`failedSets == 0 && unblocked_at == null`).

## Mục 5: bỏ qua chẩn đoán

**Trước:** không có cách đóng phiên; bài ôn gốc ở `DIAGNOSING` cho tới khi làm hết probe.

**Sau:**

- `POST /api/learning/diagnostics/{sessionId}/skip`, use case mới `SkipDiagnosticUseCase` theo mẫu hai bước của phase 1
  (danh mục KP lấy trước khóa vì `conclude` cần).
- **Chỉ cho bỏ qua khi phiên đã có ít nhất một probe `ANSWERED`.** Chưa có → `409 DIAGNOSIS_PROBE_REQUIRED`.
- Probe `OPEN` → `SKIPPED`, xóa `package_id`/`package_version_id` của probe đó (học viên chưa thấy đáp án) để gói không bị
  tính là đã giao (`assignedPackageIds`, `JdbcReviewItemRepository.java:366-367`). Rồi `conclude`.
- Probe `SKIPPED` tính như chưa biết, không bao giờ là KP hổng. Ví dụ: "Past participle" đã trượt, "Be verb" bỏ qua → vẫn
  giao bài ôn "Past participle"; không probe nào trượt → bài ôn gốc nhận set tiếp.
- Idempotent theo trạng thái: phiên đã kết luận → `200` với trạng thái hiện tại. Phiên của người khác → `404`.
- Route nằm dưới `/api/learning/**`, tier rate limit `default` của Gateway.

## Mục 6: probe không có set

**Trước:** probe `NO_SET` không báo đội nội dung. Mọi probe `NO_SET` → `Conclusion(root, rootIsGap = true)`, lưu KP gốc làm
kết luận dù chưa kiểm được gì.

**Sau:**

- `DiagnosticFlow.addProbes`: không tìm được set → `diagnostics.recordContentGap(kp)`.
- `PrerequisiteDiagnosis.conclude` trả thêm loại kết luận: không probe nào `ANSWERED` → `INCONCLUSIVE`, danh sách KP rỗng,
  gốc chạy tiếp. Lưu vào `conclusion_kind`.
- `GetDiagnosticUseCase`: đọc `conclusion_kind` thay vì tự suy `rootIsGap` (phiên cũ có cột null → giữ cách suy cũ); thêm
  nhánh `SKIPPED` vào `switch` (dòng 52–56).
- Rà nơi đọc kết luận cho thống kê cạnh `EDGE_SUSPECT` (`WorkItemSync`, `WorkItemSubjectResolver`): phiên `INCONCLUSIVE`
  hoặc `NO_PREREQUISITES` không được tính vào tỷ lệ "là gốc lỗi".

## Mục 7: KP hổng chưa từng được học

**Trước:** `DiagnosticFlow.conclude` gắn bài ôn KP hổng vào `lessonLocator.teaching(kp).orElse(rootReview.lessonId())`.
`TheoryFocus` không thấy KP trong bài học gốc → `LESSON_FALLBACK`: lý thuyết và quick check của KP khác.

**Sau:** trong `GetReviewUseCase`, bài ôn trigger `DIAGNOSTIC` có scope `LESSON_FALLBACK`:

- trả `theory = []`, scope mới `NO_LESSON`;
- đang ở bước `THEORY` thì tự `completeTheory()` rồi giao set.

Set ôn không đổi: client gửi `lessonId` dưới tên `preferredLessonId`; Content chỉ dùng nó để xếp set của bài học đó lên
trước (`JdbcLearningContentReader.java:797-799`, `ORDER BY ... = :preferredLessonId DESC`), không lọc.

## Rollback

Sau khi code mới ghi `REPEATED_FAIL`, probe `SKIPPED`, trigger `PRIORITY`/`DECAYED`, bản cũ đọc bằng `valueOf` sẽ lỗi 500
(`JdbcReviewItemRepository.java:51`, `DiagnosticProbe.Status`). Phase 5 chuẩn bị script SQL đưa dữ liệu về giá trị cũ
(`REPEATED_FAIL` → `SECOND_FAIL`, `SKIPPED` → `NO_SET`, `PRIORITY`/`DECAYED` → `CONSOLIDATE`) để chạy trước khi rollback image.

## File sửa

- `domain/aggregate/ReviewItem.java`, `domain/vo/TheoryReason.java`, `domain/vo/DiagnosticProbe.java`,
  `domain/vo/DiagnosticSession.java`
- `domain/service/PrerequisiteDiagnosis.java`, `domain/service/PracticeClearance.java`
- `application/service/DiagnosticFlow.java`, `PracticeProgress.java` (truyền cờ `unblocked`)
- `application/usecase/SubmitReviewUseCase.java`, `GetReviewUseCase.java`, `GetDiagnosticUseCase.java`, mới
  `SkipDiagnosticUseCase.java`
- `application/result/ReviewResult.java`, `ReviewSubmissionResult.java`, `DiagnosticResult.java`; DTO response tương ứng ở `api/dto`
- `api/controller/DiagnosticController.java`
- `infrastructure/persistence/JdbcReviewItemRepository.java`, `JdbcDiagnosticRepository.java`
- `src/main/resources/db/migration/V55__diagnostic_skip_and_outcome.sql`, `V56__review_failure_baseline.sql`
- `infra/config-server/config-repo/learning-service.yaml`, lớp properties mới

## Test

- `ReviewItemTest`: trượt 1 → THEORY; 2 → PRACTICE; 3 → THEORY `REPEATED_FAIL`; 4 → `blocking = false`, `PENDING`; bài
  ôn đã hạ cấp trượt → `SKIPPED`; gợi ý `PRACTICE` chưa trượt → thành bắt buộc (giữ hành vi cũ); bài ôn đã hạ cấp không
  tự chặn lại.
- Testcontainers: bài ôn hạ cấp từ trigger bài học, đề thi và Practice → `LessonAccessGate` và `PracticeClearance` không còn
  chặn; bài ôn hiện trong `GET /review-basket`.
- Hạ cấp → đề thi nâng lại → số set trượt = 0, stage `PRACTICE`, `review_sets` cũ còn nguyên, gói cũ không được giao lại.
- Gốc chờ KP hổng; KP hổng hạ cấp → gốc giao set; nâng KP hổng lại → gốc vẫn không bị đổi sang chờ.
- Skip: chưa làm probe nào → `409 DIAGNOSIS_PROBE_REQUIRED`; có probe đã trượt → bài ôn `DIAGNOSTIC`; không probe trượt →
  gốc giao set; skip hai lần → `200` cùng kết quả; gói của probe bị bỏ qua được giao lại được.
- Probe `NO_SET` → có dòng `content_gap_signals`; phiên toàn `NO_SET` → `INCONCLUSIVE`.
- `GET /diagnostics/{id}` với probe `SKIPPED` và phiên cũ (`conclusion_kind` null).
- Bài ôn `DIAGNOSTIC` cho KP không có trong bài học → `theory = []`, scope `NO_LESSON`, không bước lý thuyết.
- Sửa `RemediationLadderIntegrationTest.java:162,200` (đang assert `maxFailedSets` null).
- `PrerequisiteDiagnosisTest`, `PracticeClearanceTest`: thêm trường hợp `SKIPPED`, `INCONCLUSIVE`, `unblocked`.

## Rủi ro

- Lách cổng: đã giảm bằng điều kiện "≥ 1 probe trước khi bỏ qua". Học viên vẫn có thể trượt cố ý 4 set; KP vẫn `PRIORITY`
  trong báo cáo và giỏ ôn.
- Đổi luật trong aggregate dùng chung cho mọi trigger: chạy đủ test learning-service.
- Rollback cần script dữ liệu (mục Rollback).
