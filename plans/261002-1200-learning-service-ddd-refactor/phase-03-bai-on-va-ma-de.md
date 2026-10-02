---
phase: 3
title: "Bài ôn và mã đề"
status: completed
---

# Phase 3: Bài ôn và mã đề

Thay `ReviewStore`, `AssessmentResultStore`.

## Việc

- Aggregate `ReviewItem` (id, userId, knowledgePointId, lessonId, status `ReviewStatus`, failedSetCount, openSet) với
  entity `ReviewSet` (id, packageId, packageVersionId): `assign(set)` (chỉ khi PENDING và không có set mở),
  `recordSetResult(passed)` → DONE khi đạt, SKIPPED ở lần trượt thứ `MAX_FAILED_REVIEW_SETS`, PENDING còn lại;
  `skip()` khi hết gói. Repository: `findOwned(userId, reviewId)` (kèm set mở và số set trượt), `findPending(userId)`,
  `insertPending(...)`, `save`, `assignedPackageIds`, `lastAssignedAt`.
- Aggregate `TopicTestAssignment` (id, userId, topicId, packageId, packageVersionId, consumed): `consume(attemptId,
  percent)` một lần; `passes()` (≥ 70%). Chọn gói (chưa dùng, rồi dùng lâu nhất) → `domain/service/TestPackageSelector`.
  Repository: `findOpen(userId, topicId)`, `findOpenForAttempt(userId, packageVersionId, completedAt)` (FOR UPDATE),
  `lastConsumedAt`, `insert`, `save`.
- Port `AssessmentResultLog`: `appliedVersion`, `recordVersion` (idempotency của event).
- Response của review set để replay: `SubmissionReplayLog`.
- Sửa `ReviewUseCase`, `AssignTopicTestUseCase`, `ApplyAssessmentResultUseCase`. Xóa `ReviewStore`, `JdbcReviewStore`,
  `AssessmentResultStore`, `JdbcAssessmentResultStore`.

## Kiểm

`ReviewAndTestAssignmentIntegrationTest`, `AssessmentResultIntegrationTest` pass.
