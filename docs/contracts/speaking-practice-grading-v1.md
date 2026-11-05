# Speaking practice grading contract v1

Learning owns Speaking submissions and grades. `POST /api/learning/speaking-practice/submissions` requires consent and
an active grading feature. `/terms` includes `enabled`; clients must not offer submission when it is false. V44 stores
the question text and active rubric snapshot with the submission before the background job starts, so retries use the
same content and rubric version.

The grading states are `QUEUED`, `ANALYZING`, `GRADING`, `PAYMENT_PENDING`, `GRADED`, `FAILED_RETRYABLE`, and
`NEEDS_EXAMINER`. The job analyzes each recording once and caches the transcript, the confidence and the score of every
word and every sound. It stores a validated grade as `PAYMENT_PENDING` and stops: the access service accepts a debit only
from the wallet's owner, and a background job holds no learner token. The learner's own request then calls
`POST /api/learning/speaking-practice/submissions/{id}/settle`, which debits with idempotency key
`speaking:{userId}:{requestId}`, changes to `GRADED` only after Access confirms the debit and updates the learning path in
the same transaction. It answers 402 `INSUFFICIENT_POINTS` when the balance is short (the grade is kept, calling again
after a top-up settles it) and 503 `ACCESS_UNAVAILABLE` when points cannot be reached. The result page calls it by itself,
so the learner presses nothing.

`GET /api/learning/speaking-practice/submissions/{id}` returns the learner-safe `display` object as `result` only once the
submission is `GRADED`; a grade that is not yet paid for is never shown. It contains estimated criterion bands and
comments, transcript quality, transcripts, rhythm, pronunciation summary, weak words, and criteria below the configured
pass band. Internal `signals` are stored beside `display` in JSONB but are not returned to the learner. Signals record
transcript quality, bands, mapped criterion KPs, evidence usability, and the prompt/rubric identifiers; LLM wording never
drives mastery. The code writes criterion evidence only after `GRADED` and skips LOW-quality transcripts.

Pronunciation detail. Azure scores at phoneme granularity with IPA symbols, and keeps for every sound the sound it heard
instead when that was not the expected one. The scores are the speech service's and are final: the model never changes
them and never gives the Pronunciation band. The prompt carries a `<pronunciation_detail>` block (the weak words with
the score of each sound, and the sounds weak in several places); the model answers with `pronunciationNotes` (a word, one of
its sounds, and a tip in Vietnamese), and the validator keeps only notes for a word, and a sound of that word, that the
block listed. `display.pronunciation` carries `weakWords[]` (`word`, `accuracy`, `errorType`, `sounds[]`, `tip`) and
`weakSounds[]` (`symbol`, `count`, `accuracy`, `heardAs`, `words`, `tip`); `display.transcripts[].words[]` carries every
word with its `accuracy`, `errorType` and `sounds[]` (`symbol`, `accuracy`, `heardAs`). An analysis cached before sounds were
kept has no sounds: its words read as empty and the block is left out of the prompt.
The starter rubric and the interim pronunciation band table are calibration placeholders, so grading is off by default:
set `LEARNING_SPEAKING_ENABLED=true` to try it. `/terms.enabled` is true only when the flag is on and Azure Speech and the
LLM are both configured; creating a submission otherwise answers 503 `SPEAKING_GRADING_DISABLED`.

Grading context. The grading job holds no learner token and cannot call Content. `POST .../complete` therefore captures,
in the learner's request, the active rubric (as before) and the Speaking error types (`SPK_*`) and knowledge points, and
stores the latter two in `speaking_grading_contexts` (V45). The job reads them from there. If Content cannot be reached at
that moment the context is stored empty: the grade is still given, without error codes or personal advice.

Recordings without speech. When the speech analysis finds no spoken word in any recording, the submission ends as
`ABANDONED` with failure code `NO_SPEECH` before any quota, LLM call or charge, and the learner may record again.

Daily limit. One unit of `speaking_grading` is taken just before the LLM call and given back when that call produces no
grade. A learner who has reached the limit has the submission wait (`FAILED_RETRYABLE`, `DAILY_LIMIT_REACHED`); that wait
does not count as a grading attempt, so it never turns into `NEEDS_EXAMINER`.

Transcript quality is `LOW` only when the mean ASR confidence is under 0.85. A high share of badly pronounced words keeps
a transcript from being `HIGH` but does not make it `LOW`, so weak speakers' results still reach the learning path.
