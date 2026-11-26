# Phase 4 - published content locked by the database (content-service)

Rule: SRS DR-42. Size M. Status: proposed. The audit step must finish before any trigger is written.

## Context
- Tables `question_versions` (status DRAFT, PUBLISHED, ARCHIVED) and `content_package_versions` (same statuses, `rules`).
- Today immutability is kept by the domain (`Question.publish/archive`, `PublishContentPackageUseCase`,
  `ArchiveQuestionUseCase`, `ChangePackageVisibilityUseCase`, `ScheduledPublishJob`). Repositories
  `QuestionVersionJpaRepository` and `ContentPackageVersionJpaRepository` are plain `JpaRepository`, so any code can save a changed row.
- Attempts keep their own snapshot, so old results are safe; the lock protects what learners see now.

## Requirements
1. After a version is PUBLISHED, its content columns cannot change. Moving it to ARCHIVED stays allowed.
2. Publishing a draft (DRAFT to PUBLISHED), including the scheduled publish, keeps working.
3. Import and AI draft approval keep working because they only create or change DRAFT versions.

## Files
- Migration `services/content-service/src/main/resources/db/migration/V152__lock_published_versions.sql` (content is at V151; take the next free version).
- Test `PublishedVersionLockMigrationTest` (Testcontainers).

## Steps
1. Audit: list every `save`, `saveAll` and native `UPDATE` that touches the two tables (grep the repositories above and
   the import, AI draft, seed and migration code). Write the list in the PR. Any path that edits a PUBLISHED row is a bug
   to fix first (it must create a new version).
2. Columns to protect: `question_versions` stem, options, answer_spec, schema_version, explanation, difficulty, hint, guide,
   guide_steps; `content_package_versions` rules, schema_version, version_number.
3. Trigger function `forbid_published_version_change()` BEFORE UPDATE: if `OLD.status = 'PUBLISHED'` and any protected
   column differs (`IS DISTINCT FROM`), raise an exception with a stable message; allow changes to `status`, `published_at`,
   `updated_at`. Attach to both tables. No trigger on DELETE.
4. Map the exception in `GlobalExceptionHandler` to the existing conflict response in Vietnamese.

## Tests
- Update the stem of a PUBLISHED question version: refused. Same for a package version's rules.
- DRAFT to PUBLISHED and PUBLISHED to ARCHIVED succeed.
- `ScheduledPublishJob` publishes a draft; course import and AI draft approval still pass the existing integration tests.
- Run the whole content-service suite; note that `PracticeSetMaterializerIntegrationTest` already fails on `main` for an unrelated missing bean, so compare against that baseline.

## Risks and rollback
- A hidden write path blocks an author at runtime; the audit and the full suite are the guard. Ship with the trigger
  message clear enough to find the path from the log.
- Rollback: `DROP TRIGGER` on both tables and drop the function.
