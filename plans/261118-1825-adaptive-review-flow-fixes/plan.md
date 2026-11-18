---
title: Sửa luồng bài ôn, chẩn đoán dò ngược và giỏ ôn
description: >-
  Gỡ các đường làm học viên kẹt vĩnh viễn ở bài ôn bắt buộc, làm UI chẩn đoán dò ngược, cho giỏ ôn đỡ các KP yếu
  bị rơi khỏi mọi danh sách, và thống nhất công thức mastery
status: proposed
priority: P1
branch: main
tags:
  - learning-service
  - frontend
  - review
  - diagnostics
  - mastery
blockedBy: []
blocks: []
created: '2026-11-18T11:25:00.000Z'
source: conversation
---

# Sửa luồng bài ôn, chẩn đoán dò ngược và giỏ ôn

Nguồn: rà luồng adaptive learning ngày 2026-11-18 (sơ đồ Figma `adaptive-learning-follow`, frame 00–10). Mọi mục đã đối chiếu
với code ở `main` (`fbbcef05`) và qua một vòng red team (xem cuối file); chưa chạy code hay test.

## Bối cảnh

Có hai luồng hay bị nhầm tên:

- **Bài ôn bắt buộc + chẩn đoán dò ngược** ("đẩy"): hệ thống tạo bài ôn khi KP yếu, bài ôn chặn tiến độ; trượt set thứ 2
  thì mở phiên dò ngược tìm KP nền bị hổng (`DiagnosticFlow`). Đây là "phiên truy xuất KP yếu thật sự".
- **Giỏ ôn và gợi ý** ("kéo"): danh sách tính lại mỗi lần đọc (`GET /review-basket`); học viên tự chọn, chỉ tạo bài ôn tùy
  chọn, không bao giờ chặn.

Sau plan này giỏ ôn là **lưới đỡ** của bài ôn bắt buộc: mọi đường bài ôn bắt buộc kết thúc mà KP vẫn yếu đều rơi xuống giỏ
ôn. Chiều ngược lại giữ nguyên: giỏ ôn không bao giờ chặn và không mở chẩn đoán.

## Luồng trượt bài ôn bắt buộc: trước và sau

```text
TRƯỚC                                              SAU
Trượt 1 → lý thuyết → set mới                      Trượt 1 → lý thuyết → set mới                      (giữ)
Trượt 2 → mở dò ngược (trong lúc nộp bài)          Trượt 2 → mở dò ngược ở lần mở bài ôn kế tiếp      (mục 2)
          FE không có màn hình → KẸT VĨNH VIỄN               FE có trang probe; bỏ qua sau ≥ 1 probe      (mục 5, 13)
Trượt 3, 4, 5… → set mới mãi                       Trượt 3 → lý thuyết lần nữa → set mới              (mục 4)
Chỉ thoát khi đạt hoặc hết gói (SKIPPED)           Trượt 4 → thôi chặn, bài ôn vào giỏ ôn             (mục 4)
SKIPPED/DONE mà KP vẫn yếu → không hiện ở đâu      KP vẫn yếu → hiện ở nhóm "weak" của giỏ ôn         (mục 8)
```

## Bảng thay đổi: trước và sau

| # | Chủ đề | Trước | Sau | Quyết định |
|---|--------|-------|-----|------------|
| 1 | Gói nội dung hoặc bài học bị gỡ | Content chỉ trả gói `PUBLISHED`; set/probe trỏ gói đã gỡ → 404 mãi; bài học archive → `GetReview` và `SubmitTheoryCheck` 404 → kẹt ở bước lý thuyết | 404 khi tải gói xử lý như gói bị lộ (đổi set, `SKIPPED`/`NO_SET`); bài học archive → lý thuyết rỗng, tự hoàn tất bước lý thuyết; theory check không lỗi | Tự chọn |
| 2 | Chẩn đoán làm hỏng lần nộp | Mở dò ngược gọi Content trong transaction nộp set; Content lỗi → rollback cả lần nộp | Nộp set chỉ ghi kết quả; dò ngược mở ở GET kế tiếp; Content lỗi lúc đó → GET 503, không giao set | Tự chọn |
| 3 | HTTP trong khóa học viên | Các use case bài ôn, probe, giỏ ôn gọi Content (trực tiếp và qua `describe`, `activeErrorTypes`, `lessonPracticeSets`) khi đang giữ khóa, vi phạm `AGENTS.md` §3.9 | Bước chuẩn bị ngoài khóa (kiểm quyền sở hữu và replay trước), rồi `TransactionTemplate` khóa, đọc lại, kiểm khớp; `StartBasketReview` tách bắt buộc | Tự chọn; chỉ các use case của luồng này |
| 4 | Trượt bài ôn bắt buộc mãi | Lần 3+ giao set mới vô hạn tới khi đạt hoặc hết gói | Lần 3 quay lại lý thuyết; lần N = 4 (cấu hình) bài ôn thôi chặn, vẫn `PENDING`, vào giỏ ôn; Practice và bài sau mở; phản hồi nộp có `blocking` | Đã chốt: A, N = 4 |
| 4a | Nâng bài ôn đã hạ cấp lên bắt buộc lại | Số set trượt là số đếm từ `review_sets`, nâng lại vẫn đếm 4 → trượt 1 set là hạ cấp ngay | Cột mốc `failures_counted_from`; nâng cấp ghi mốc, chỉ đếm set trượt sau mốc; không xóa `review_sets` | Đã chốt: (a), cách làm theo red team |
| 4b | Bài ôn gốc chờ KP hổng | `pendingWaitingOn` đếm mọi bài ôn `PENDING`; KP hổng hạ cấp → gốc chờ mãi | Hạ cấp KP hổng thì xóa nó khỏi danh sách chờ của gốc trong cùng transaction | Hệ quả của mục 4 |
| 4c | Bài ôn tùy chọn trigger `PRACTICE` trượt | Luôn tự thành bắt buộc | Chỉ khi chưa từng trượt và chưa hạ cấp | Hệ quả của mục 4 |
| 5 | Bỏ ngang phiên dò ngược | Không có cách thoát; bài ôn gốc ở `DIAGNOSING` mãi | `POST /diagnostics/{id}/skip`, **chỉ khi đã làm ≥ 1 probe**; probe còn mở → `SKIPPED`, trả gói; kết luận từ probe đã làm | Đã chốt: UI + nút bỏ qua, cần ≥ 1 probe |
| 6 | Probe không có set | Không báo đội nội dung; mọi probe `NO_SET` vẫn kết luận "hổng ở KP gốc" | Probe `NO_SET` ghi `content_gap_signals`; lưu loại kết luận (`INCONCLUSIVE`…); gốc chạy tiếp | Tự chọn |
| 7 | KP hổng chưa từng được học | Bài ôn gắn vào bài học của KP gốc; lý thuyết `LESSON_FALLBACK` là của KP khác | Bài ôn `DIAGNOSTIC` không có lý thuyết đúng KP: lý thuyết rỗng, scope `NO_LESSON`, bỏ bước lý thuyết | Tự chọn |
| 8 | KP `PRIORITY` không còn bài ôn | Không hiện trong giỏ ôn; start trả `409 NOT_IN_REVIEW_BASKET` | Hiện ở `weak`, lý do `PRIORITY`, chỉ khi bắt đầu được (có bài học dạy KP, set > reserve); start kiểm paywall và lọc premium; `trigger_kind = PRIORITY` | Đã chốt; lọc premium chỉ ở đường mới |
| 9 | KP bị decay | `MAINTAIN` lâu không làm → `INSUFFICIENT` → rơi khỏi "due" | Job decay ghi `decayed_from_tier` (từ lúc deploy, không backfill); KP này ở `due`, lý do `DECAYED`, tối đa 10 KP cũ nhất; start như mục 8; badge đếm cả loại này | Đã chốt |
| 10 | Hai công thức mastery | `GET /mastery` và `ReviewReevaluation` dùng DeepTutor; tier, lịch ôn dùng MP1 | Cả hai đọc điểm từ `kp_mastery_profiles` theo công thức đang cấu hình | Tự chọn |
| 11 | Luyện tự do tạo bài ôn | `FreePracticeSubmission` gọi `createReviews`, trái javadoc và `free-practice-v1.md` | Bỏ lời gọi | Tự chọn: theo contract |
| 12 | GET bài ôn có tác dụng phụ | Giao set, `SKIPPED`, mở chẩn đoán; contract không nói | Giữ GET; contract ghi rõ; test hai GET liên tiếp cho cùng kết quả | Tự chọn |
| 13 | FE chưa có UI chẩn đoán | `ReviewPage` không đọc `state`, `diagnosticSessionId`, `waitingOnReviewIds`; không gọi `/diagnostics`; hiển thị "2/" và "sau null bộ" | Trang probe, danh sách "cần ôn trước", nút bỏ qua (bật sau ≥ 1 probe); "x/4"; thông báo chuyển vào giỏ ôn; nhãn `PRIORITY`/`UNBLOCKED`/`DECAYED` | Đã chốt: gồm FE |

## Phases

| Phase | Tên | Mục | Trạng thái |
|-------|-----|-----|------------|
| 1 | [Gỡ kẹt: gói bị gỡ, chẩn đoán trễ, HTTP ngoài khóa](./phase-01-unstick-review-and-probe.md) | 1, 2, 3 | Pending |
| 2 | [Luật trượt bài ôn và chẩn đoán](./phase-02-review-failure-and-diagnosis-rules.md) | 4, 4a–c, 5, 6, 7 | Pending |
| 3 | [Giỏ ôn và nhất quán mastery](./phase-03-basket-and-mastery-consistency.md) | 8, 9, 10, 11 | Pending |
| 4 | [Frontend: UI chẩn đoán và giỏ ôn](./phase-04-frontend-diagnosis-and-basket.md) | 13, hiển thị 4, 5, 7, 8, 9 | Pending |
| 5 | [Contract, tài liệu, kiểm chứng](./phase-05-contracts-docs-verification.md) | 12, docs, rollback | Pending |

Thứ tự: 1 → 2 → 3 → 4 → 5.
- Phase 1 và 2 cùng sửa `GetReviewUseCase`, `SubmitReviewUseCase`, `DiagnosticFlow`, nên làm nối tiếp, không song song.
- Phase 3 dùng CHECK `trigger_kind` của V56 (phase 2).
- Phase 4 cần endpoint skip (phase 2) và lý do mới của giỏ ôn (phase 3).

## Tiêu chí nghiệm thu

- Không còn đường nào khiến bài ôn bắt buộc chặn mãi, mỗi đường có test:
  - gói bị gỡ;
  - bài học bị archive ở bước lý thuyết;
  - Content lỗi lúc nộp;
  - bỏ ngang chẩn đoán;
  - trượt liên tục.
- Trượt set thứ 2 → mở bài ôn thấy `DIAGNOSING` → FE hiện probe, làm được; bỏ qua được sau khi làm ≥ 1 probe.
- Trượt lần thứ 4 → bài ôn `blocking = false`, `PENDING`; phản hồi nộp có `blocking: false`; bài học sau **và Practice**
  mở; bài ôn hiện trong giỏ ôn.
- Nâng bài ôn đã hạ cấp → đếm set trượt lại từ 0; không gói nào đã lộ đáp án bị giao lại.
- Bài ôn gốc đang chờ KP hổng chạy tiếp khi bài ôn KP hổng bị hạ cấp, và không bị đổi lại sang chờ.
- KP `PRIORITY` không có bài ôn và KP decay hiện trong giỏ ôn chỉ khi bắt đầu được; bắt đầu không lỗi; học viên miễn phí
  không nhận set premium; reserve cho bài ôn bắt buộc không bị dùng hết.
- Badge `due` khớp nhóm `due` của giỏ ôn.
- `GET /mastery` trả cùng điểm với `kp_mastery_profiles`.
- Luyện tự do không tạo bài ôn nào.
- Các use case trong phạm vi không gọi Content khi đang giữ khóa học viên, kể cả lời gọi gián tiếp.
- Gửi lại cùng `requestId` (bài ôn, probe) trả phản hồi cũ, kể cả sau khi gói bị gỡ.
- Kiểm tra chạy xanh:
  - backend: `mvn -q -pl services/learning-service -am test` (có Docker);
  - FE: `npm run lint`, `typecheck`, `build`, và test Vitest của các trang đã sửa.

## Phụ thuộc

- Không plan dở nào sửa `ReviewItem`, `DiagnosticFlow`, `GetReviewBasketUseCase` (đã rà `plans/*/plan.md` theo tên lớp).
- `261117-0829-suggestions-hide-unowned-course-lessons` đã xong, chỉ chạm gợi ý bài học.
- Ba migration mới, chỉ nới CHECK và thêm cột, không sửa dữ liệu cũ:
  - **V55** (phase 2): probe `SKIPPED`, `conclusion_kind`, phản hồi probe;
  - **V56** (phase 2): `failures_counted_from`, `unblocked_at`, `REPEATED_FAIL`, `trigger_kind` `PRIORITY`/`DECAYED`;
  - **V57** (phase 3): `decayed_from_tier`.
- Rollback image cần chạy script dữ liệu (phase 5).

## Câu hỏi còn mở

1. Ngoài phạm vi, để plan riêng: các use case khác của learning-service (nộp bài học, Practice…) có thể cũng gọi Content
   trong khóa; chưa rà.
2. Ngoài phạm vi: báo việc cho `CONTENT_AUTHOR` khi nhiều học viên cùng kẹt một KP (người dùng chọn để sau).
3. Ngoài phạm vi: bài ôn bắt buộc, chẩn đoán và các lần bắt đầu `CONSOLIDATE`/`MAINTAIN` có thể giao gói premium cho học
   viên miễn phí. Lỗi có từ trước; người dùng chọn chỉ lọc ở đường mới, phần còn lại để plan riêng.
4. Nâng bài ôn đã hạ cấp không chạy lại chẩn đoán (mỗi bài ôn gốc một phiên). Chấp nhận ở plan này.

## Quyết định đã chốt (2026-11-18)

- Mục 4: phương án A. Trượt lần 3 đọc lại lý thuyết; trượt lần 4 bài ôn thôi chặn và vào giỏ ôn.
  `learning.review.unblock-after-failed-sets = 4`.
- Mục 4a: (a). Nâng bài ôn đã hạ cấp thì đếm set trượt lại từ 0, bằng cột mốc, không xóa lịch sử.
- Mục 5 và 13: làm UI chẩn đoán kèm nút bỏ qua; chỉ bỏ qua được sau khi làm ≥ 1 probe; không tắt chẩn đoán tạm thời.
- Mục 8: KP `PRIORITY` hiện ở nhóm `weak`, ôn tùy chọn.
- Mục 9: KP decay hiện ở nhóm `due`, lý do `DECAYED`.
- Lọc gói premium chỉ ở các lần bắt đầu mới của giỏ ôn (`PRIORITY`, `DECAYED`).
- Phạm vi gồm cả backend và frontend.
- Không dùng vai trò Mentor/staff để can thiệp từng học viên (hệ thống không có vai trò này).

## Red Team Review

### Session 1 (2026-11-18)

**Phát hiện:** 15 sau khi gộp từ 3 reviewer (Failure Mode, Assumption Destroyer, Security). Tất cả nhận, 0 bỏ.
**Mức độ:** 3 Critical, 8 High, 4 Medium. Tất cả có dẫn chứng `file:line`; bốn điểm nặng nhất đã kiểm lại thủ công.

| # | Phát hiện | Mức | Kết quả | Áp vào |
|---|-----------|-----|---------|--------|
| 1 | Số set trượt là số đếm từ `review_sets`, không reset được bằng UPDATE; xóa set cũ làm gói đã lộ bị giao lại | Critical | Nhận | Phase 2 (V56, cột mốc) |
| 2 | Start `PRIORITY`/`DECAYED` vi phạm CHECK `trigger_kind` → 500 | Critical | Nhận | Phase 2 (V56), 3 |
| 3 | Bài học archive → `SubmitTheoryCheckUseCase` 404 → kẹt ở lý thuyết | Critical | Nhận | Phase 1 |
| 4 | Bài ôn đã hạ cấp vẫn chặn Practice (`PracticeClearance` tính mọi `PENDING`); `refreshPassForLesson` thêm vào không tác dụng | High | Nhận | Phase 2 |
| 5 | Sót lời gọi Content trong khóa: `describe`, `activeErrorTypes`, `lessonPracticeSets`; skip cũng gọi HTTP | High | Nhận | Phase 1, 2 |
| 6 | `StartBasketReview` gọi `GetReview` lồng trong transaction và khóa | High | Nhận | Phase 1 |
| 7 | Thứ tự replay `requestId` khi tách hai bước; probe không lưu phản hồi | High | Nhận | Phase 1, 2 (V55) |
| 8 | Lấy set probe trước khóa bị trùng gói | High | Nhận | Phase 1 |
| 9 | Định nghĩa `DECAYED` sai; NPE khi sắp `dueAt` null; badge không như plan nói | High | Nhận | Phase 3 (V57) |
| 10 | Phản hồi nộp không có `blocking` → FE không biết bài ôn vừa hạ cấp | High | Nhận | Phase 2, 4 |
| 11 | Start `PRIORITY` lặp làm cạn gói; mục hiện nhưng không bắt đầu được; chưa kiểm premium | High | Nhận | Phase 3 |
| 12 | Thiếu `GetDiagnosticUseCase`; kết luận "chưa xác định" không có chỗ lưu; probe bỏ qua vẫn giữ gói | Medium | Nhận | Phase 2 (V55) |
| 13 | `maxFailedSets = 4` làm vỡ test tích hợp, `attemptNumber` FE và mock | Medium | Nhận | Phase 2, 4 |
| 14 | Rollback image sau migration → `valueOf` lỗi 500 | Medium | Nhận | Phase 2, 5 |
| 15 | Sửa nhỏ: GET 503 khi cần mở chẩn đoán; danh sách chờ đổi trạng thái; chỉ bắt 404 quanh lời gọi Content; kiểm quyền sở hữu trước; nơi gọi `ReviewReevaluation`; nhãn `UNBLOCKED` | Medium | Nhận | Phase 1, 2, 3, 4 |

Hai quyết định thêm của người dùng trong phiên này:
- Lách cổng chặn (trượt cố ý cộng bỏ qua chẩn đoán): chỉ cho bỏ qua sau khi làm ≥ 1 probe.
- Gói premium: chỉ lọc ở đường bắt đầu mới của giỏ ôn.

### Whole-Plan Consistency Sweep

- Đọc lại: `plan.md`, phase 01–05.
- Các thay đổi đã đối chiếu (8):
  - reset số set trượt → cột mốc;
  - `pendingWaitingOn AND blocking` → xóa khỏi danh sách chờ;
  - bỏ `refreshPassForLesson` → cờ `unblocked` trong `PracticeClearance`;
  - "skip không gọi HTTP" → skip có bước chuẩn bị;
  - định nghĩa `DECAYED` → cột `decayed_from_tier`;
  - 2 migration → 3 migration;
  - nơi gọi `ReviewReevaluation`;
  - skip cần ≥ 1 probe.
- Tham chiếu cũ đã sửa: 9. Gồm "chỉ nới CHECK" ở `plan.md` và phase 2; `StartBasketReview` "có thể"; `ApplyAssessment` +
  `SubmitPracticeAttempt`; `StaffInsightUseCase` cho `EDGE_SUSPECT`; "Kiểm trước khi làm" về `confidence` và tên KP; số
  migration ở phase 5; phase 1 và 3 "song song".
- Mâu thuẫn chưa giải: 0.

## Validation Log

### Session 1 (2026-11-18)

**Kết quả kiểm chứng** (bước kiểm của red team, cộng một khẳng định được xử lý ở bước này):
- Khẳng định đã kiểm: 29 (tier Full).
- Đúng: 28 | Sai: 3 (đã sửa trong red team) | Chưa kiểm được: 0.
- Khẳng định cuối được xác nhận: Content chỉ dùng `preferredLessonId` để ưu tiên, không lọc
  (`JdbcLearningContentReader.java:797-799`).
- 3 khẳng định sai đã sửa:
  - "reset số set trượt" (`JdbcReviewItemRepository.java:38`);
  - thêm `SKIPPED` mà thiếu `GetDiagnosticUseCase.java:52-56`;
  - `EDGE_SUSPECT` nằm ở `WorkItemSync`, không ở `StaffInsightUseCase`.

**Câu hỏi đã hỏi: 3**
1. Số lần trượt trước khi thôi chặn đặt ở đâu → file yaml `learning.review.unblock-after-failed-sets` (không đưa lên UI admin).
2. KP đã decay trước ngày deploy → chỉ tính từ lúc deploy, không backfill.
3. Giới hạn mục `DECAYED` trong giỏ ôn → 10 KP có bằng chứng cuối cũ nhất; badge đếm theo cùng giới hạn.

**Đã lan sang:** phase 3 (giới hạn 10, không backfill, test), phase 2 (xác nhận mục 7), bảng thay đổi ở `plan.md` (mục 9).

### Whole-Plan Consistency Sweep
- Đọc lại: `plan.md`, phase 01–05.
- Thay đổi đã đối chiếu: 3 (nơi đặt N, không backfill, giới hạn 10).
- Tham chiếu cũ đã sửa: 3 (rủi ro "chạy lại job decay" và "ví dụ 10" ở phase 3; ghi chú chưa kiểm ở mục 7 phase 2).
- Mâu thuẫn chưa giải: 0.
