# Phase 3 (Deploy trước Producer): Consumers `learning-service` & `assessment-service`

Hai service này có lượng dữ liệu học/làm bài lớn theo `userId` và cùng sở hữu file ghi âm trong bucket `speaking-recordings`.

## Thiết kế Consumer chuẩn (áp dụng chung cho cả 2 service)
- **Topology:** Mỗi service tự khai báo trên `UserMessaging.EXCHANGE` (`user.events`) với routing key `user.deleted.v1`:
  - `learning-service`: queue `learning.user-deleted.v1`, retry `learning.user-deleted.v1.retry` (TTL 30s), DLQ `learning.user-deleted.v1.dlq`.
  - `assessment-service`: queue `assessment.user-deleted.v1`, retry `assessment.user-deleted.v1.retry` (TTL 30s), DLQ `assessment.user-deleted.v1.dlq`.
- **Listener (`ackMode = "MANUAL"`):** Theo mẫu `AssessmentCompletedListener`:
  - Parse `UserDeletedV1`; lỗi contract → `park(...)` sang DLQ.
  - Gọi use case xử lý idempotent theo `eventId` → `channel.basicAck(tag, false)` **sau khi transaction commit**.
  - Lỗi tạm → `basicNack(tag, false, false)` sang retry queue; quá `maxDeliveryAttempts` → `park(...)` sang DLQ. Không gọi HTTP trong listener.
- **Tombstone (`deleted_users`):**
  - Thêm bảng `deleted_users (user_id UUID PRIMARY KEY, deleted_at TIMESTAMPTZ NOT NULL)` trong migration mới.
  - Consumer `user.deleted.v1` ghi `INSERT INTO deleted_users ... ON CONFLICT DO NOTHING` **trong cùng transaction** với ghi `user_deletion_jobs` (hoặc dọn dữ liệu).
  - Các consumer khác của service (`AssessmentCompletedListener`, `HumanReviewCompletedListener`, `PlacementSpeakingRequestedListener`, `PointsGrantSettledListener` ở `learning-service`; `PlacementSpeakingCompletedListener` ở `assessment-service`) kiểm tra `deleted_users` theo `userId` và ack-bỏ-qua event đến muộn của user đã xóa.
- **Xử lý theo lô (tránh transaction khổng lồ):**
  - Listener chỉ ghi một dòng vào bảng `user_deletion_jobs (event_id UUID PRIMARY KEY, user_id UUID NOT NULL, status VARCHAR(20), created_at TIMESTAMPTZ, completed_at TIMESTAMPTZ)` (idempotent theo `event_id`) rồi ACK nhanh.
  - `@Scheduled` worker (`UserDeletionJobWorker`) claim từng job bằng `FOR UPDATE SKIP LOCKED` (an toàn nhiều instance; ghi vào bảng "Scheduler và nhiều instance" của `docs/system-architecture.md`).
  - Worker xóa từng lô nhỏ (ví dụ 500 bản ghi/lô trong transaction ngắn) cho các bảng lớn; với `speaking-recordings`, đọc danh sách `object_key` theo lô rồi gọi `ObjectStorage.delete(...)` (`common-storage`) **ngoài transaction và ngoài khóa**.

## Bảng dữ liệu cần xóa

### 1. `learning-service`
| Bảng / Storage | Hành động | Ghi chú |
| --- | --- | --- |
| `topic_progress`, `lesson_progress`, `lesson_exercise_submissions` | Xóa theo `user_id` | Xóa theo lô |
| `review_items`, `review_sets`, `review_theory_checks` | Xóa theo `user_id` | |
| `topic_test_assignments`, `practice_test_assignments`, `course_progress`, `course_test_assignments`, `learner_course_unlocks`, `course_study_activity` | Xóa theo `user_id` | |
| `kp_evidence`, `kp_mastery_profiles`, `question_attempt_events`, `assessment_result_versions` | Xóa theo `user_id` | Xóa theo lô (bảng lớn) |
| `lesson_writing_submissions`, `speaking_submissions`, `speaking_recordings`, `speaking_analyses`, `speaking_grading_contexts`, `human_review_outcomes`, `placement_speaking_jobs`, `placement_speaking_outbox` | Xóa theo `user_id` | |
| `diagnostic_sessions`, `diagnostic_probes`, `remediation_flags`, `learner_placements`, `placement_assignments` | Xóa theo `user_id` | |
| `learning_goals`, `learning_activities`, `study_sessions`, `study_heartbeat_receipts`, `study_activity_intervals`, `study_daily_totals`, `study_goal_history` | Xóa theo `user_id` | |
| `xp_ledger`, `reward_grants`, `learner_weekly_snapshots`, `learner_monthly_snapshots`, `league_members`, `learner_league`, `daily_quests`, `learner_badges`, `practice_sessions`, `practice_attempts`, `lesson_practice_passes` | Xóa theo `user_id` | |
| Bucket `speaking-recordings` (tiền tố `practice/{userId}/...`) | Xóa file qua `ObjectStorage` | Ngoài transaction |
| Lưu ý: `pg_advisory_xact_lock` | Theo `AGENTS.md §3.8` | Mở khóa theo user khi ghi/xóa dữ liệu học nếu cần |

### 2. `assessment-service`
| Bảng / Storage | Hành động | Ghi chú |
| --- | --- | --- |
| `assessment_attempts`, `attempt_sections`, `attempt_items`, `attempt_responses`, `attempt_item_knowledge_points` | Xóa theo `user_id` | Xóa theo lô / cascade |
| `assessment_results`, `skill_scores`, `item_results`, `item_result_knowledge_judgments`, `error_analysis_items` | Xóa theo attempt của `user_id` | Xóa trước hoặc cascade |
| `learner_submissions`, `grading_jobs`, `human_reviews`, `essay_grade_comparisons`, `human_review_submissions`, `video_practice_attempts`, `placement_speaking_grades`, `llm_usage_daily` | Xóa theo `user_id` / `learner_id` | Nếu user là examiner (`assigned_examiner_id` / `requested_examiner_id` / `grader_user_id`), đặt `NULL` hoặc ẩn danh tham chiếu |
| `examiner_qa_reviews` | Đặt `examiner_id`/`reviewer_id` hoặc giữ audit ẩn danh | Kiểm tra constraint `examiner_id <> reviewer_id` |
| Bucket `speaking-recordings` (tiền tố `speaking/{userId}/...`) | Xóa file qua `ObjectStorage` | Ngoài transaction |

## Kiểm chứng
- `mvn -q -pl services/learning-service -am -Dtest=UserDeleted*,ArchitectureTest test`
- `mvn -q -pl services/assessment-service -am test` (kèm `ArchitectureTest`)
