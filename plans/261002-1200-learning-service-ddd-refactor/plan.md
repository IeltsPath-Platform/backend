---
title: "learning-service theo DDD và Clean Architecture"
description: "Đưa trạng thái và luật chuyển trạng thái của learning-service vào aggregate, thay các Store dạng thao tác dòng bằng repository theo aggregate; giữ JDBC adapter, schema, contract HTTP/event và hành vi."
status: completed
priority: P1
branch: "refactor/learning-ddd"
created: "2026-10-02"
source: "plans/reports/review-261002-1155-ddd-clean-architecture-audit-report.md"
---

# learning-service theo DDD và Clean Architecture

## Bối cảnh

Audit 2026-10-02: learning-service viết kiểu transaction script. Domain chỉ có hàm thuần (`domain/service`) và record
(`domain/vo`); persistence là bốn port "Store" ở `application/port` thao tác từng dòng (`passBlock`, `completeLesson`,
`finishReview(String)`, `markGraded`…); luật chuyển trạng thái nằm trong `WHERE` của SQL adapter và trong use case dài
(263, 261, 177 dòng). Các service khác để sau (xem báo cáo audit).

## Không đổi

- Schema (không migration mới), contract HTTP (`lesson-learning-v1`, `lesson-writing-v1`), event `AssessmentCompleted.v2`,
  route, cấu hình.
- Adapter dùng JDBC (`JdbcTemplate`); advisory lock theo user; `ON CONFLICT` cho idempotency.
- Chữ ký public của use case mà controller, consumer và integration test dùng (để 35 integration test là lưới an toàn).
- Luật thuần đã có: `MasteryCalculator` (port DeepTutor, giữ comment nguồn), `ReviewRule`, `AnswerSpecGrader`,
  `LessonAccessGate`, `TopicStatusDeriver`, `LessonHintPolicy`, `WritingScore`.

## Đích

```text
domain/aggregate    LessonProgress, TopicProgress, ReviewItem (+ entity ReviewSet), TopicTestAssignment, WritingSubmission
domain/vo           TopicStatus, ReviewStatus, WritingSubmissionStatus, EvidenceSource, KnowledgeEvidence, MasteryHistory,
                    EssayPrompt, WritingGrade, WritingTask, …
domain/repository   một repository mỗi aggregate + KnowledgeEvidenceRepository, KnowledgePointCatalogRepository
domain/service      luật thuần hiện có + PassMark (70%)
application/port    LearningContentClient, AccessClient, LlmClient (ngoài); LearnerLock, SubmissionReplayLog,
                    AssessmentResultLog, LlmUsageQuota (concern ứng dụng: khóa, idempotency, hạn mức)
application/service LessonAccess, ReviewReevaluation, EssayGrader, AnswerSheet, LessonEvidenceReference, LessonViewAssembler
infrastructure      Jdbc*Repository/Jdbc* adapter cho mỗi interface trên
api/dto             request/, response/
```

Luật chuyển trạng thái nằm trong aggregate (ví dụ `ReviewItem.recordSetResult` → DONE/PENDING/SKIPPED sau 3 set trượt;
`WritingSubmission` GRADING → PAYMENT_PENDING → GRADED, GRADING → FAILED, FAILED → GRADING). Repository lưu có điều kiện
theo trạng thái đã đọc (compare-and-set) ở những chỗ hiện không giữ khóa (Writing sau GRADING), để giữ đúng an toàn đồng thời.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Sắp xếp package, enum trạng thái](./phase-01-sap-xep-package.md) | Completed |
| 2 | [Tiến độ bài/topic và bằng chứng](./phase-02-tien-do-va-bang-chung.md) | Completed |
| 3 | [Bài ôn và mã đề](./phase-03-bai-on-va-ma-de.md) | Completed |
| 4 | [Bài luận Writing](./phase-04-bai-luan-writing.md) | Completed |
| 5 | [Gọn use case, test aggregate, tài liệu, E2E](./phase-05-gon-use-case-va-nghiem-thu.md) | Completed |

Mỗi phase: `mvn -q -pl services/learning-service -am test` pass (hiện 183 test, Testcontainers) trước khi sang phase kế.

## Kết quả (2026-10-02)

- Mỗi phase 1–4: 183/183 test learning pass. Phase 5: 198/198 (thêm 15 unit test aggregate và `PackageRotation`).
- Kịch bản E2E qua Gateway (stack tạm, jar build `clean`): 95/95, gồm Writing song song, hạn mức, thiếu point, LLM lỗi.
- Không còn `*Store`; bốn Store cũ thay bằng 5 aggregate, 7 repository và 5 port ứng dụng (`LearnerLock`,
  `ExerciseSubmissionLog`, `ReviewSubmissionLog`, `AssessmentResultLog`, `LlmUsageQuota`).
- Lệch so với phase 1: enum trạng thái (`ReviewStatus`, `WritingSubmissionStatus`, `EvidenceSource`) tạo ở phase dùng đến
  chúng thay vì tạo sẵn; aggregate topic là `LearnerCurriculum` (entity `TopicProgress`) thay vì `TopicProgress` riêng,
  để luật thứ tự và trạng thái topic nằm một chỗ.
- Thứ tự khóa giữ như cũ: advisory lock của học viên trước khóa dòng (`finish` của bài luận).

## Tiêu chí xong

- Không còn `application/port/*Store`; persistence của dữ liệu nghiệp vụ đi qua `domain/repository`.
- Mọi chuyển trạng thái nằm trong method của aggregate; adapter chỉ đọc/ghi (điều kiện trong SQL chỉ còn compare-and-set
  theo trạng thái/version đã đọc, không chứa luật).
- Trạng thái dùng enum, không so chuỗi trong use case.
- Toàn bộ test learning-service pass; có unit test cho từng aggregate; kịch bản E2E 95 check (report
  `plans/260930-2057-…/reports/e2e-261002-mvp-reading-writing-listening.md`) chạy lại pass.
- `AGENTS.md` §3.1 ghi learning theo template; `services/learning-service/README.md` cập nhật cấu trúc.

## Rủi ro

- Writing chạy ngoài transaction/khóa giữa các bước: repository phải lưu có điều kiện, nếu không hai request song song
  ghi đè nhau. Kiểm bằng `LessonWritingIntegrationTest` và ca song song trong E2E.
- Thứ tự ghi thay đổi có thể đổi thời điểm `clock_timestamp()` (mastery dùng `ordinal`, không dùng thời gian): kiểm bằng
  số mastery của kịch bản Lan (0.729, 0.875, 0.487).
- Phạm vi lớn: làm và kiểm từng phase, không gộp.
