# Vocabulary learner progress report

Date: 2026-10-26
Branch: `feat/vocabulary-notebook`

## Delivered

- Added V21 review events and per-user weekly goals. Applied reviews append an event in the same transaction as the flashcard update; replays of the latest `requestId` do not append another event. Review `sessionId` is optional.
- Added weekly goal, six-band map, topic/set progress, latest session result, and read-time achievement APIs. Summary and notebook entry responses only gain fields; entries now filter by `topicId` and `setId`.
- All learner reads/writes use `CurrentUserProvider` and user-scoped SQL aggregates. No Learning service calls or XP/point awards were added.

## Final API contract

All routes below require authentication. User identity is taken from the internal JWT through `CurrentUserProvider`. Invalid input returns the existing `ErrorResponse` with `400`; missing/invalid authentication returns `401`; missing or inactive set/card returns `404`. `GET /recent-result` returns `204` if no session event is available.

| Method and path | Query/body | Success response |
| --- | --- | --- |
| `GET /api/learning-support/flashcards/notebook/entries` | Existing filters plus optional `topicId=<uuid>` and `setId=<uuid>`; existing `q`, levels, due, sort, page, size remain | Existing `{items,page,size,total}`; each entry adds `cefrLevel` and `topicName`. SQL filters and pagination. |
| `GET /api/learning-support/flashcards/notebook/summary` | `timezone` IANA, default `Asia/Ho_Chi_Minh` | Existing fields plus `learnedCount`, `catalogTotal`, `learningCount` (FAMILIAR + LEARNING), `masteredCount`. Learned means `review_count > 0` or Leitner box ≥2. |
| `POST /api/learning-support/flashcards/{id}/reviews` | Existing `{correct,requestId}` plus optional `sessionId=<uuid>` | Existing `FlashcardResponse`; a newly applied review adds one `flashcard_review_events` row, while a replay of the latest `requestId` adds none. |
| `GET /api/learning-support/flashcards/notebook/weekly-goal` | Optional `timezone` IANA, default `Asia/Ho_Chi_Minh` | `{target,progress,remaining,days:[{date,weekday,achieved}]}`; Monday–Sunday in local timezone. `progress` is distinct senses added or reviewed this week; `achieved` marks a day with an applied review. |
| `PUT /api/learning-support/flashcards/notebook/weekly-goal` | Same optional `timezone`; body `{"targetWords":120}`; target 10–1000 | Same weekly-goal response. Default target is 120 until overridden for the user. |
| `GET /api/learning-support/flashcards/notebook/vocabulary-map` | Optional `currentCefr=A1..C2` | `{bands:[{band,cefr,total,learned,learning,needsReview,notLearned}],currentBand,suggestedBand}` with all six CEFR levels. `currentBand` uses the supplied value or greatest learning count; null if no entries are learning. Suggested is the lowest band with due entries, otherwise null. |
| `GET /api/learning-support/flashcards/notebook/topics-progress` | Optional `setId=<uuid>` | `{topics:[{topicId,name,total,learned,percent,status}],current}`. `current` is the most recently reviewed topic with at most five unlearned `nextWords`, or null. |
| `GET /api/learning-support/flashcards/notebook/sets/{setId}/progress` | UUID path | `{setId,total,learned,learning,needsReview,notLearned,percent}`; missing/inactive set returns `404`. |
| `GET /api/learning-support/flashcards/notebook/recent-result` | — | `200 {sessionId,topic,correct,total,percent,finishedAt}` for the latest session with a supplied session ID, or `204`. |
| `GET /api/learning-support/flashcards/notebook/achievements` | Optional `timezone` IANA, default `Asia/Ho_Chi_Minh` | `{items:[{code,achieved,progress,target}]}` for a seven-day review streak, one completed topic, and 300 mastered words. Calculated on read, not stored. |

Bad UUID/CEFR/timezone, malformed body, invalid weekly target, invalid page, or invalid existing notebook filters return `400`. Existing `POST /notebook/entries`, `PUT /entries/{id}/level`, `/saved`, and flashcard review response fields remain available.

## Schema and domain rules

- V21 adds `flashcard_review_events(id,user_id,flashcard_id,vocabulary_sense_id NULL,session_id NULL,correct,reviewed_at)` with user/time, user/session/time, and user/sense/time indexes.
- V21 adds `vocabulary_weekly_goals(user_id PK,target_words CHECK 10..1000,updated_at)`. `library.vocabulary.weekly-goal-default` is 120.
- Domain defines learned entries, the IANA-local Monday week, a review day, streak behavior (streak may end today or yesterday), topic status, band selection, achievement thresholds, and goal validation. CEFR current level may be passed from Learning's `/api/learning/report`; Library never calls Learning.

## Verification

- `mvn -q -pl services/library-service -am test`: exit 0; 219 tests, 0 failures, 0 errors, 0 skipped. Docker/Testcontainers ran. Flyway applied through V21. Includes multi-user SQL aggregate tests, migration validation, review event idempotency persistence, nullable session ID, endpoint authentication/validation, and domain boundary tests.
- `git diff --check`: clean.
- `graphify update .`: completed; Java graph extraction succeeded. SQL AST extraction was unavailable because `tree_sitter_sql` is not installed.

## Deviations and open questions

- No scope deviation. Compatibility is retained for existing review callers and clients that omit `sessionId`.
- The phase does not specify the topic-completion badge threshold; implementation treats one fully learned topic as achieved. Confirm whether a higher threshold is wanted before changing the user-visible badge.

Status: DONE_WITH_CONCERNS
