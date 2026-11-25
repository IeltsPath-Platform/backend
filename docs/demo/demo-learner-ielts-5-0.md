# Demo learner: IELTS 5.0, mid-course

Demo data for one learner, written by the Flyway callbacks `db/callback/afterMigrate__demo_data.sql` of user, access, learning, library and notification services. It runs after every migrate, only when `DEMO_DATA_ENABLED=true`, and never overwrites progress made since (every row has a fixed id or natural key). Never enable it in production: the password is public.

## Run it

1. In the root `.env` set `DEMO_DATA_ENABLED=true` (Compose sets it by default).
2. Restart Config Server, then user, access, learning, library and notification services. Each applies its callback at startup.
3. Log in as `h2@gmail.com` with `Demo@123`.

To give the data to an account that already exists, set `DEMO_LEARNER_ID` to its user id before step 2 (the callbacks of learning, library, access and notification use it instead of h2):

```sql
-- in user_db
SELECT id FROM users WHERE email = 'lehieuhn153@gmail.com';
```

Restarting with another id writes a second, separate copy for that account; the first one stays.

## What the learner has

Course IELTS 5.0 started 15 days ago (ids from Content V105, V111, V136). Dates are relative to the first run.

| Area | Data |
|---|---|
| Course | 11 of 26 lessons done (42%), lesson 12 open but locked until the blocking review is done; placement band 4.5, goal band 6.0 |
| Topic tests | First six topics tested: four passed (85, 72, 90, 78%), two finished under 70% (64, 55%) |
| Practice | Passed on the first submission for most lessons; lesson 4 needed a review, lesson 9 attempted every set; two failed attempts kept |
| Reviews | 9 cases: blocking practice (lesson 11, set assigned), blocking theory (lesson 10, one of two checks done), optional basket from consolidation, maintenance, free practice, an error pattern and a Writing criterion, one done (with its passed set), one skipped |
| Mastery | 16 knowledge points in all three tiers (2 priority, 6 consolidate, 8 maintain), some due for review now; 80 answers with errors |
| Gamification | 870 XP (level 2), 11-day streak, 3 of 7 hearts, 3 daily quests (2 done), badges `FIRST_TOPIC_TEST` and `STREAK_7`, silver league, fourth of ten (rivals are `rival1..9@ielts.demo`), 11 days of study time, two weekly snapshots (band 4.5 then 5.0) |
| Vocabulary | 36 saved words: 8 mastered, 8 learning, 10 familiar, 6 new, 4 fallen back; 14 learned words due for review; review events for a week, weekly goal 120, streak 6, XP |
| Writing (AI graded) | Three essays on Content's demo prompts: a Task 2 essay (band 5.0, seven corrections, below the pass band 6), its revision (band 6.0, passed) and a Task 1 essay (band 5.5); the Writing criterion review points at the first one |
| Examiner review | The practice essay of lesson 11 (band 5.5 by AI, failed attempt) graded by `examiner@ielts.demo` (Demo@123): band 6.0, four criterion comments, three marked errors, feedback; one human grading credit used |
| Free practice | Three knowledge-point practice sets (two passed, one failed) |
| Plan | Premium for six months with three human gradings (one used); wallet 975 points with five ledger entries |
| Notifications | Words due for review and examiner finished the essay (unread), placement result ready and Premium activated (read) |

## Not included

- **Assessment records behind the topic tests and the placement test** (attempts, items, results). Their rows show in the submission history from Learning's own tables, but opening the full result of one finds nothing.
- **Final course test and course outcome.** The learner is at 42%, and the final test only opens after every topic has been tested, so it would contradict the story.
- **Speaking.** The IELTS 5.0 course has no Speaking lessons; no free Speaking submission is seeded either.
- Community posts, reactions and follows.
