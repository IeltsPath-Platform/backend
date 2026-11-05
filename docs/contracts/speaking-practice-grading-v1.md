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
The phase-2 starter rubric and the interim pronunciation band table are calibration placeholders. The scheduler remains
enabled by default in the development configuration; set `LEARNING_SPEAKING_ENABLED=false` to switch it off until the rubric is reviewed and calibrated.
