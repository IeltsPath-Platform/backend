# Phase 7 - access control: Examiner reach, Learner-only features, API documentation

Rules: SRS DR-49, DR-50, DR-51. Size M. Status: proposed. Three small parts, one PR each if the diff is large.

## What was checked
- Ownership is enforced inside the use cases (`findByIdAndUserId`, an explicit owner check in `GetGradingJobUseCase`), so
  one learner cannot read another learner's data. This phase does not change that.
- Roles in the token: `CUSTOMER` (the Learner), `CONTENT_AUTHOR`, `EXAMINER`, `ADMIN`, `STAFF_MANAGER` (being renamed),
  `SALES_STAFF`. The gateway requires a valid token for everything except the public list and `denyAll` on `/internal/**`.

## 7a - an Examiner reads any learner's data (learning-service)
Today `hasRole('EXAMINER')` is the only check on three endpoints, and the learner id comes from the URL:
- `GET /api/learning/admin/learners/{userId}/overview` (`StaffAdminController`)
- `GET /api/learning/admin/learners/{userId}/report` (`ReportController`)
- `GET /api/learning/admin/learners/{userId}/courses/{courseId}/score` (`CourseScoreController`)
Nothing in the web app, the mobile app or another service calls them (grep of `admin/learners` finds only a result record).

Recommended fix: remove the three endpoints and the use case paths that only they use (YAGNI). If the owner wants an
Examiner view later, add it with a check that the learner has a submission assigned to that Examiner.
Fallback if removal is refused: change the role to `ADMIN` and add the learner id to the audit log.
Tests: the three paths return 404 (removed) or 403 for an Examiner; the learner's own report, score and overview still work.

## 7b - Learner-only features need the Learner role (many services)
Controllers with no `@PreAuthorize`, so any signed-in user may call them. The list from the scan:
- learning: `LessonLearning`, `Practice`, `Review`, `Writing`, `FreePractice`, `FreeWriting`, `Streak`, `Hearts`, `Gamification`,
  `LearningGoal`, `LearnerTree`, `StudySession`, `LearningActivity`, `LessonResource`
- assessment: `AssessmentAttempt`, `AssessmentResult`, `PlacementResult`, `ProvisionalFeedback`, `BandStatistics`,
  `LearnerSubmission`, `VideoPractice`, and the two `GradingJob` methods without a role (the other three are `EXAMINER`)
- library: `Flashcard`, `FlashcardDeck`, `Note`, `SavedVideoSegment`, `VideoDictation`, `VideoProgress`, `VocabularyNotebook`,
  `VocabularyProgress`, `VocabularyStudy`, `VocabularyStudyMe`
- game: every controller; the WebSocket handshake too (`/ws/games/**` is public at the gateway, so check the token in the handshake)
- community: posting, commenting, reacting and reporting (the same controller also has the Administrator moderation methods)
- access: `LearnerAccess` (points, subscription, key activation) and `AccessCatalog` (plans, key products)
Keep as they are: `notification/*`, `user/LearnerProfile`, `user/Session`, `user/OAuthIdentity` (every role has its own
profile, password, sessions and notifications), the public vocabulary catalogue, and the admin and staff controllers
that already have a role.

Steps:
1. Owner pre-check, run on a copy of production data: which accounts hold the Learner role and also a staff role, so
   that nobody who needs the learner screens loses them:
   `SELECT u.email, array_agg(r.name ORDER BY r.name) FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id GROUP BY u.id, u.email HAVING bool_or(r.name = 'CUSTOMER') AND count(*) > 1;`
2. Add one meta-annotation in `common-security`, for example `@LearnerOnly` that stands for
   `@PreAuthorize("hasRole('CUSTOMER')")`, and put it on the classes above. Community: allow `CUSTOMER` and `ADMIN` on the
   shared controller because moderation needs to read posts; keep the existing admin check in the use cases.
3. Games: reject the handshake when the token has no `CUSTOMER` role.
4. Run the web app and the mobile app once as Learner, Content Author, Examiner, Administrator and Sales Staff; the staff
   roles must keep every staff screen and only lose the learner screens.
Tests: per service, a security test that a token with `CONTENT_AUTHOR` (and one with `SALES_STAFF`) gets 403 on one endpoint
of each annotated controller, and a `CUSTOMER` token still passes. Existing controller tests keep passing.

## 7c - API documentation closed in production (gateway, config, compose)
Today `api-gateway.yaml` lists `/swagger-ui/**`, `/v3/api-docs/**`, `/api-docs/**` and `/webjars/**` as public, and
`API_DOCS_ENABLED` defaults to `true` in every service config and in `docker-compose.yml`. `docker-compose.prod.yml` does
not set it, so production serves the documentation without sign-in unless the server's `.env` sets it to `false`.
Fix:
1. `docker-compose.prod.yml`: set `API_DOCS_ENABLED: "false"` for the gateway and every business service.
2. Config repo: default `API_DOCS_ENABLED` to `false`; keep `true` in `docker-compose.yml` (local development) explicitly.
3. Gateway: when the flag is `false`, do not list the documentation paths as public (they then need a token, or answer 404).
4. Document the variable in `docs/deployment.md` and `.env.example`.
Tests: with the flag `false`, an unauthenticated `GET /swagger-ui.html` and `GET /v3/api-docs` return 401 or 404; with `true`
they work. Ask the owner to confirm the real `.env` of production before and after the change.

## Acceptance and SRS
- Each part ships code, tests and a rollback note. No public contract changes except the removal in 7a.
- SRS: DR-49 (7a), DR-50 (7b) and DR-51 (7c) change from Planned to enforced when they merge; the Permission Matrix row for the
  Examiner and the footnote on Learner-only rows are updated at the same time.

## Risks and rollback
- 7b can lock out a staff account that also uses the learner screens; the pre-check lists them and the owner decides who gets the Learner role.
- 7b touches many files with one-line changes; keep them in one commit per service for an easy revert.
- 7c can hide the documentation that a developer uses on the server; keep it on in local development.
- Rollback: remove the annotations, restore the three endpoints from git, set `API_DOCS_ENABLED` back to `true`.
