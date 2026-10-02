---
phase: 2
title: "Tiến độ bài/topic và bằng chứng"
status: completed
---

# Phase 2: Tiến độ bài/topic và bằng chứng

Thay `LearningProgressStore` (18 method) bằng aggregate và repository.

## Việc

- Aggregate `LessonProgress` (userId, lessonId, topicId, sortOrder, knowledgePointIds, passedBlockIds, completedAt):
  `place(topicId, sortOrder, kpIds)`, `passBlock(blockId)`, `complete()` (chỉ một lần), `isCompleted()`,
  `hasPassed(blockId)`. Repository: `findByUserAndTopic`, `find`, `findCompleted`, `save`, `completedCountsByTopic`.
- Aggregate `TopicProgress` (userId, topicId, sequenceOrder, passedAt): `pass()` một chiều. Repository: `findByUser`,
  `pass`, `replaceSequence(userId, orders)`.
- `KnowledgePointCatalogRepository.upsert(entries)` (catalog dùng chung, giữ thứ tự khóa theo kpId).
- `KnowledgeEvidenceRepository`: `append(userId, List<KnowledgeEvidence>)`, `removeAssessmentEvidence(userId, attemptId)`,
  `findMasteryHistories(userId)` (read model 5 lần gần nhất, giữ SQL hiện tại).
- Port ứng dụng: `LearnerLock.lock(userId)` (advisory lock), `SubmissionReplayLog` (lưu/đọc response theo `requestId`,
  câu sai theo khối cho gợi ý, response lần nộp đầu cho bài ôn).
- Sửa các use case và `LessonAccess`, `ReviewReevaluation` dùng interface mới. Xóa `LearningProgressStore`,
  `JdbcLearningProgressStore`.
- `LessonSubmissionIntegrationTest` đang autowire `LearningProgressStore`: đổi sang repository tương ứng.

## Kiểm

Toàn bộ test learning pass; mastery kịch bản Lan (0.729, 0.875) không đổi.

## Rủi ro

Thứ tự ghi trong `submit` (bằng chứng → response → hoàn thành bài → bài ôn) phải giữ nguyên để `ordinal` của bằng chứng
không đổi.
