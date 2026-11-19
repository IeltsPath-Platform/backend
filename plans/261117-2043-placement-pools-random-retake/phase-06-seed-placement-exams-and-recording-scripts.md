---
phase: 6
title: "Seed placement exams and recording scripts"
status: pending
priority: P1
dependencies: [1]
effort: "XL"
---

# Phase 6: Seed placement exams and recording scripts

## Overview
Five new placement exams are authored and seeded by migration so every pool opens with two exams: two shortened exams for `BAND_0_3`, two shortened exams for `BAND_3_5`, and one full-length exam for `BAND_5_9` (the existing `PLACEMENT-4SKILLS` is the other one in that pool). The repo also gets markdown documents so the Listening audio can be recorded, uploaded and checked before the release: one recording script and review document per exam plus a README with the difficulty rubric and the upload steps.

## Decisions (user)
- 2 exams per pool at launch (5 new). With two, a third retake within 24 hours repeats the exam assigned longest ago; a third exam per pool is recommended later.
- Pools `BAND_0_3` and `BAND_3_5` use shortened exams; `BAND_5_9` keeps the full-length shape. The Listening and Reading conversion scales any length to 40 questions, so a shortened exam still converts to a band.
- **The migrations publish the exams on deploy** (no post-deploy review). The review therefore moves before merge, see "Gates".
- Audio is recorded by the user from scripts this phase writes; migrations only store the file keys.

## Requirements
- Exam shapes:
  - Shortened exam (4 exams): Listening 2 parts of 10 questions (one audio each), Reading 2 passages of 10 questions, Writing 1 task, Speaking 2 prompts (a short-answer part and a cue card).
  - Full-length exam (1 exam, pool 5-9): same structure as the existing `V27` exam: Listening Parts 1-4 (40 questions), Reading Passages 1-3 (40), Writing Task 1 and Task 2 (250-word minimum), Speaking Parts 1-3.
- Difficulty rubric per pool, written in `docs/placement-exams/README.md` and used by the reviewer: `BAND_0_3` about CEFR A1-A2 (short sentences, everyday topics, slow clear speech); `BAND_3_5` about A2-B1; `BAND_5_9` B1 to C2 with natural pace. The rubric is an authoring guide, not a calibration: no learner data exists yet.
- Package codes: `PLACEMENT-B03-A`, `PLACEMENT-B03-B`, `PLACEMENT-B35-A`, `PLACEMENT-B35-B`, `PLACEMENT-B59-B`. Each is `PUBLISHED` with version 1 `PUBLISHED`, `current_published_version_id` set and `placement_pool` set (columns from phase 1, migration `V137`).
- Every question: purpose `PLACEMENT`, status `PUBLISHED`, an answer spec valid under `answer-spec-v1`, and used by exactly one package (publish rule `questionsUsedElsewhere`; migrations bypass the publish use case, so the generator and the tests enforce these invariants).
- Audio: 12 files (shortened 4 exams x 2 parts = 8; full-length exam x 4 parts = 4), keys `listening/placement/<package-code-lowercase>/part<N>.mp3`, with the declared duration in the seed. The asset row holds the transcript and the key, like `V27`.
- Writing and Speaking prompts follow the existing `V27` patterns (essay answer spec with the pool-appropriate minimum words; Task 1 of the full-length exam carries its data table and chart facts, so it needs the same `chartFacts` handling as the existing exam). [verify against `V27` while authoring]

## Architecture
- Source of truth: one JSON file per exam in `scripts/placement-exams/exams/` (package, sections, passages, questions with answer specs, Listening transcripts and durations, Writing and Speaking prompts).
- Dev-only generator `scripts/placement-exams/generate-placement-seed.mjs` (Node, no npm dependency) reads the JSON and writes: the migrations (`V138__seed_placement_exams_band_0_3.sql`, `V139__seed_placement_exams_band_3_5.sql`, `V140__seed_placement_exams_band_5_9.sql`) and the markdown documents. UUIDs are deterministic (UUID v5 from package code and path) so a rerun produces identical files. `--check` mode fails when the committed migrations or documents differ from what the sources generate. A second mode checks the audio (see Gates).
- Migrations are plain SQL inserted into `content_assets`, `questions`, `question_versions`, `content_packages`, `content_package_versions`, `content_sections`, `section_questions` (and `question_knowledge_points` if the existing placement seed maps KPs [verify in `V25`/`V27`]), one transaction per exam, no edit of any applied migration.
- Documents in `docs/placement-exams/`: `README.md` (rubric, recording format, upload steps, checklist) and one file per exam containing the recording script (speaker roles and voices, pace, pauses, announcement text, target duration, exact file key) and the review copy (passages, questions with answer keys, writing and speaking prompts) for the examiner. Recording format: mp3, at least 128 kbps, clean room, no background noise, trailing silence under 1 second. [adjust to what the existing `V27` audio uses]

## Related Code Files
- Create: `scripts/placement-exams/generate-placement-seed.mjs`, `scripts/placement-exams/exams/*.json` (5), `services/content-service/src/main/resources/db/migration/V138__seed_placement_exams_band_0_3.sql`, `V139__seed_placement_exams_band_3_5.sql`, `V140__seed_placement_exams_band_5_9.sql`, `docs/placement-exams/README.md` and `docs/placement-exams/<package-code>.md` (5), seed tests.
- Modify: `PlacementTestSeedTest.java` (counts and pool distribution now include the new exams); `docs/contracts/content-package-authoring-v1.md` only if the seeding conventions are worth recording.

## Gates before merge and before deploy (publish on deploy has no later review)
1. **Content review:** an IELTS-qualified reviewer signs off every exam document: difficulty against the pool rubric, answer keys, ambiguity, Writing and Speaking prompts. The sign-off is recorded in the PR; the migrations are not merged without it.
2. **Audio ready:** all 12 files are recorded, uploaded to the media bucket under the exact keys, and checked with the generator's audio mode (HEAD request to each key under `CONTENT_MEDIA_BASE_URL`, status 200, content type audio, duration within 15% of the declared one). A missing file makes Listening fail with `INVALID_MEDIA_REFERENCE` for every learner who gets that exam.
3. **Deploy order:** upload audio, then deploy the Content migrations (this phase after phase 1's `V137`), then Assessment, then Learning with the frontend.
4. **Rollback:** a published exam is withdrawn with a new migration that archives the package (or the admin screen); applied migrations are never edited.

## Implementation Steps (tests first)
1. **Red (Testcontainers, fresh database):** after all migrations there are six published placement packages (the existing one plus five); pool distribution is 2 / 2 / 2; question counts match each exam's shape (20+20 for the four shortened exams, 40+40 for the full-length one); every question has purpose `PLACEMENT`, status `PUBLISHED` and an answer spec the same validator used at publish accepts; no question belongs to two packages; every Listening section has an audio asset with a key matching the pattern and a positive duration; sections are ordered; each package's `placement_pool` matches its code.
2. **Red:** generator `--check` fails when a source JSON changes without regenerating; deterministic output across two runs.
3. Draft the five exam sources (JSON) to the rubric: passages, questions and answer keys, Listening transcripts with durations, Writing and Speaking prompts.
4. Implement the generator; generate migrations and documents; run the tests (green).
5. Reviewer sign-off round (gate 1); fix sources, regenerate, rerun tests.
6. The user records and uploads the 12 files from the scripts; run the audio check (gate 2).
7. Merge and release in the order of gate 3.
8. Run the phase 5 end-to-end scenario against the real seeded exams.

## Success Criteria
- [ ] Seed tests failed before and pass after; the generator `--check` passes.
- [ ] Each pool has two published exams on a fresh database.
- [ ] All 12 audio files are reachable and playable before the release; sign-off recorded.
- [ ] A learner can take each new exam end to end and receive a band within the pool's ceiling.

## Risk Assessment
- **Unreviewed or mis-calibrated content goes live to real learners on deploy.** Their band picks the recommended course, so a bad exam gives bad recommendations. Mitigation: gate 1 is mandatory; the exams are AI-drafted and uncalibrated, so treat the first weeks as a trial and watch the band distribution per pool.
- **Missing or wrong audio at deploy** breaks Listening for every learner who is assigned that exam. In pool 5-9 the second exam makes this half of all attempts. Mitigation: gate 2 and the deploy order; archive the exam by migration if it still happens.
- With two exams per pool the third retake in 24 hours repeats an exam and answers stay visible on the result, so a repeated exam can be memorised. Accepted; add a third exam per pool when operations can.
- Size: about 240 objective questions plus essay and speaking prompts, 12 transcripts. Authoring is the cost, not the migration. Split the review by pool if needed (V138, V139 and V140 are independent).
- Question ids and package codes must never collide with existing seeds; the deterministic ids and the tests guard this.
