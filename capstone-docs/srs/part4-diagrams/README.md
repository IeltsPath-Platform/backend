# SRS 4.1 source map

The ERD is derived from existing SQL migrations, read only. No SQL was executed.

`IELTSPath_ERD.drawio` contains the 14 editable diagram views embedded in the SRS. PNG exports use the same SQL-derived model.

Solid links: declared foreign keys or named association tables. Dashed links: stored identifier references without SQL foreign keys. Cardinalities reflect nullable and unique/primary-key declarations, not an invented minimum child count.

## Core entity mapping

| Entity | Owner | Source table | Migration |
|---|---|---|---|
| User | user | `users` | `services/user-service/src/main/resources/db/migration/V1__create_user_tables.sql` |
| Role | user | `roles` | `services/user-service/src/main/resources/db/migration/V1__create_user_tables.sql` |
| Learner Profile | user | `learner_profiles` | `services/user-service/src/main/resources/db/migration/V2__create_identity_tables.sql` |
| Learning Goal | user | `learning_goals` | `services/user-service/src/main/resources/db/migration/V2__create_identity_tables.sql` |
| Learning Activity | user | `learning_activities` | `services/user-service/src/main/resources/db/migration/V5__create_learning_activity_and_streak_tables.sql` |
| Streak | user | `streaks` | `services/user-service/src/main/resources/db/migration/V5__create_learning_activity_and_streak_tables.sql` |
| Plan | access | `plans` | `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` |
| Plan Feature | access | `plan_features` | `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` |
| Subscription | access | `subscriptions` | `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` |
| Key Product | access | `key_products` | `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` |
| Activation Key | access | `activation_keys` | `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` |
| Key Activation | access | `key_activations` | `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` |
| Point Wallet | access | `point_wallets` | `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` |
| Point Ledger Entry | access | `point_ledger_entries` | `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` |
| Topic | content | `topics` | `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` |
| Knowledge Point | content | `knowledge_points` | `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` |
| Lesson | content | `lessons` | `services/content-service/src/main/resources/db/migration/V8__lessons_and_topic_tests.sql` |
| Lesson Block | content | `lesson_blocks` | `services/content-service/src/main/resources/db/migration/V8__lessons_and_topic_tests.sql` |
| Content Asset | content | `content_assets` | `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` |
| Content Package | content | `content_packages` | `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` |
| Package Version | content | `content_package_versions` | `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` |
| Content Section | content | `content_sections` | `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` |
| Question | content | `questions` | `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` |
| Question Version | content | `question_versions` | `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` |
| Vocabulary Item | library | `vocabulary_items` | `services/library-service/src/main/resources/db/migration/V1__create_library_catalog.sql` |
| Vocabulary Sense | library | `vocabulary_senses` | `services/library-service/src/main/resources/db/migration/V1__create_library_catalog.sql` |
| Learning Video | library | `learning_videos` | `services/library-service/src/main/resources/db/migration/V1__create_library_catalog.sql` |
| Video Segment | library | `video_segments` | `services/library-service/src/main/resources/db/migration/V1__create_library_catalog.sql` |
| Segment Lexical Entry | library | `video_segment_lexical_entries` | `services/library-service/src/main/resources/db/migration/V1__create_library_catalog.sql` |
| Video Progress | library | `video_learning_progress` | `services/library-service/src/main/resources/db/migration/V2__create_personal_library.sql` |
| Saved Segment | library | `saved_video_segments` | `services/library-service/src/main/resources/db/migration/V2__create_personal_library.sql` |
| Note | library | `notes` | `services/library-service/src/main/resources/db/migration/V2__create_personal_library.sql` |
| Flashcard Deck | library | `flashcard_decks` | `services/library-service/src/main/resources/db/migration/V2__create_personal_library.sql` |
| Flashcard | library | `flashcards` | `services/library-service/src/main/resources/db/migration/V2__create_personal_library.sql` |
| Topic Progress | learning | `topic_progress` | `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` |
| Lesson Progress | learning | `lesson_progress` | `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` |
| Exercise Submission | learning | `lesson_exercise_submissions` | `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` |
| Writing Submission | learning | `lesson_writing_submissions` | `services/learning-service/src/main/resources/db/migration/V2__lesson_writing.sql` |
| Practice Attempt | learning | `practice_attempts` | `services/learning-service/src/main/resources/db/migration/V4__practice_attempts.sql` |
| Practice Pass | learning | `lesson_practice_passes` | `services/learning-service/src/main/resources/db/migration/V4__practice_attempts.sql` |
| Review Item | learning | `review_items` | `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` |
| Review Set | learning | `review_sets` | `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` |
| Review Theory Check | learning | `review_theory_checks` | `services/learning-service/src/main/resources/db/migration/V5__review_ladder.sql` |
| Topic Test Assignment | learning | `topic_test_assignments` | `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` |
| KP Evidence | learning | `kp_evidence` | `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` |
| Assessment Attempt | assessment | `assessment_attempts` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Attempt Section | assessment | `attempt_sections` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Attempt Item | assessment | `attempt_items` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Attempt Response | assessment | `attempt_responses` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Assessment Result | assessment | `assessment_results` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Skill Score | assessment | `skill_scores` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Item Result | assessment | `item_results` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Error Analysis Item | assessment | `error_analysis_items` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Learner Submission | assessment | `learner_submissions` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Grading Job | assessment | `grading_jobs` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Human Review | assessment | `human_reviews` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Grading Point Cost | assessment | `grading_point_costs` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Video Practice Attempt | assessment | `video_practice_attempts` | `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` |
| Game Room | game | `game_rooms` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Room Member | game | `game_room_members` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Game Match | game | `game_matches` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Match Player | game | `game_match_players` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Game Session | game | `game_sessions` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Game Answer | game | `game_answers` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Game Event | game | `game_events` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Quiz Event | game | `quiz_events` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Quiz Participation | game | `quiz_participations` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Leaderboard Period | game | `leaderboard_periods` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Leaderboard Entry | game | `leaderboard_entries` | `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` |
| Post | community | `posts` | `services/community-service/src/main/resources/db/migration/V1__create_community_tables.sql` |
| Comment | community | `comments` | `services/community-service/src/main/resources/db/migration/V1__create_community_tables.sql` |
| Post Reaction | community | `post_reactions` | `services/community-service/src/main/resources/db/migration/V1__create_community_tables.sql` |

## Association tables

| Association | Endpoints | Keys |
|---|---|---|
| `user_roles` | User / Role | `user_id, role_id` |
| `flashcard_deck_items` | Flashcard Deck / Flashcard | `deck_id, flashcard_id` |
| `lesson_knowledge_points` | Lesson / Knowledge Point | `lesson_id, knowledge_point_id` |
| `lesson_block_questions` | Lesson Block / Question Version | `block_id, question_version_id` |
| `lesson_block_knowledge_points` | Lesson Block / Knowledge Point | `block_id, knowledge_point_id` |
| `question_knowledge_points` | Question Version / Knowledge Point | `question_version_id, knowledge_point_id` |
| `section_questions` | Content Section / Question Version | `section_id, question_version_id` |
| `lesson_block_vocabulary` | Lesson Block / Vocabulary Sense | `block_id, vocabulary_sense_id` |
| `content_asset_links` | Content Section / Content Asset | `section_id, asset_id` |
| `content_asset_links` | Question Version / Content Asset | `question_version_id, asset_id` |

`content_asset_links` belongs to either a section or a question version, not both (SQL CHECK).
`attempt_item_knowledge_points` and `item_result_knowledge_judgments` preserve assessment-level KP attribution/judgment; neither introduces a new canonical Knowledge Point.
Reference fields that are polymorphic or lack a declared target (for example source_reference_id and feedback_revision_id) are not converted into guessed foreign keys.

## Reproduce

Use the installed Codex Python runtime for `build-srs-entities.py` and `update-srs-section.py`. Set `CODEX_ARTIFACT_MODULES` to the bundled Node module directory and run `render-erd.cjs` with the bundled Node runtime. The updater preserves its original input under `qa/` for scope verification.

## Migration evidence

All migrations below were inspected for CREATE/ALTER/DROP statements. Seed records do not define new entities.

- `services/access-service/src/main/resources/db/migration/V1__create_access_tables.sql` — SHA-256 `cc80abea5a1ca572c13fb87529052cab71c2389a7bd855e3bdd1db6aa057af35`
- `services/access-service/src/main/resources/db/migration/V2__add_video_learning_premium_feature.sql` — SHA-256 `7f4cbaa11f9bae6ee7f6282f4616ae4acab3149f8466029ac4dc1242b25aeadb`
- `services/assessment-service/src/main/resources/db/migration/V1__create_assessment_tables.sql` — SHA-256 `084fd6cbd2b8ec558645279e9da5c2e7aa5d6cd44b2932f105725b6aac483262`
- `services/assessment-service/src/main/resources/db/migration/V2__seed_grading_point_costs.sql` — SHA-256 `c1f223108e8be3e9f9fa2bf924eab6a06a0b04916e2429c2557f2a4b549ac7ef`
- `services/assessment-service/src/main/resources/db/migration/V3__add_attempt_response_lock_version.sql` — SHA-256 `3f4c0d94f8c00113b707e2f4efa4e4c9286a9aa637f283504a59d24a803fa067`
- `services/assessment-service/src/main/resources/db/migration/V4__add_assessment_completed_event_support.sql` — SHA-256 `9a7eab0c8df574e46aa9c64e4bde2a0a87b0362ae1fd2a4455eb5c02811e688e`
- `services/community-service/src/main/resources/db/migration/V1__create_community_tables.sql` — SHA-256 `26dd560297c9c53f6422070d9b162ca0790c462bc41f6321b6e20bda0e26db2e`
- `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` — SHA-256 `41a760d9d3a9c3f1f43e1598513aa166612833ebd4f4d0bfa1ce48e087d8cd34`
- `services/content-service/src/main/resources/db/migration/V2__align_content_entitlements.sql` — SHA-256 `7a08a7b6e85135b01bd6b615c2221c91cd391cfab09ce761c0e7aa2ed11d777a`
- `services/content-service/src/main/resources/db/migration/V3__add_knowledge_point_learning_type.sql` — SHA-256 `b940d0fb92bef2397c5cf7ea9c33cb25e0fd6e8f53d76331b534b8e481ba5f97`
- `services/content-service/src/main/resources/db/migration/V4__seed_main_flow_content.sql` — SHA-256 `74fab0e7cfade72d76ac7393d5775deb89b392e98b85bc7703c9c59051db1634`
- `services/content-service/src/main/resources/db/migration/V5__add_band_ranges.sql` — SHA-256 `773f19374bade9c03a4f0d5d2e5094c00dc4db2c3f745fbc9ce1a7c9b464d83e`
- `services/content-service/src/main/resources/db/migration/V6__seed_demo_reading_passage.sql` — SHA-256 `c37a99760e4303606b997ee571bd31e7e9350d98a09deb7ddb56514fa9c83366`
- `services/content-service/src/main/resources/db/migration/V7__drop_vocabulary_and_video_tables.sql` — SHA-256 `852b47a2f9a6d4b8131f3a7c9aee1fe86a8461102b71b5f3d4ecdddf217696ec`
- `services/content-service/src/main/resources/db/migration/V8__lessons_and_topic_tests.sql` — SHA-256 `ee839d2b6c1be6518ce9623304e6fe56fd45104ecc2f30693c82a3a079df9c43`
- `services/content-service/src/main/resources/db/migration/V9__seed_lesson_pipeline_demo.sql` — SHA-256 `9a000fb7bb55d8d75256fe5f7a5f1a3e71941f2fc7f1824ca9f298e10a0bf7cb`
- `services/content-service/src/main/resources/db/migration/V10__seed_writing_task2_demo.sql` — SHA-256 `5fa95004e4a1aa6a6f2d4134e921f00cc0acf032bfd8d3d17101c300fc21ac40`
- `services/content-service/src/main/resources/db/migration/V11__seed_writing_task1_demo.sql` — SHA-256 `b3fa2f47719c2ae3e76afbec958bbb97805ffc84a0f29e179bd78501426626d4`
- `services/content-service/src/main/resources/db/migration/V12__seed_listening_demo.sql` — SHA-256 `b826db4f6990c7cf6a10ffc64157029cf397b6124ca58692b7627519e86e5e10`
- `services/content-service/src/main/resources/db/migration/V13__question_version_hint.sql` — SHA-256 `aba2fde463d6e9614b0ff1810d0d8f40da56d951196ce8b5cd1a5889aa2f0af2`
- `services/content-service/src/main/resources/db/migration/V14__premium_topics.sql` — SHA-256 `f82f15e985eb3cc156df6cfc31cc2c41d729b7a0237493a4233ffcad8e9ae662`
- `services/content-service/src/main/resources/db/migration/V15__topic_skill_and_writing_topic.sql` — SHA-256 `a4ea21b7399835cd2032396c0fc56a5db378b97e9fd7660803e3908d6b86fc16`
- `services/content-service/src/main/resources/db/migration/V16__lesson_block_kps_and_practice_lesson.sql` — SHA-256 `e829d16c6f70b07fb2f4d3c6e1c74e8a9987dbf2d9c87649bbe3c9534d82b3b0`
- `services/content-service/src/main/resources/db/migration/V17__question_purpose.sql` — SHA-256 `73a1cf62ecbd90f19af4dcfe59af2054338d1431a21fb42854c554903c637612`
- `services/content-service/src/main/resources/db/migration/V18__more_reading_practice_and_final_tests.sql` — SHA-256 `1dbd1e1d61acbd84600e05600a2b2b9995f81178ee9d4398dff794dde997fcbb`
- `services/game-service/src/main/resources/db/migration/V1__create_game_tables.sql` — SHA-256 `fba9a9595085ed0a629b362cee946d7413f73dfbbf00e75d43276b38bfc86222`
- `services/learning-service/src/main/resources/db/migration/V1__learning_schema.sql` — SHA-256 `09fd2ea6876b59b1367968b3e442c20bd7190108cbc13762aedfe18a0efaa1fb`
- `services/learning-service/src/main/resources/db/migration/V2__lesson_writing.sql` — SHA-256 `ed2485b796438aafe3522919592a45d839d87a2e56a5d3cd8e7f8f9e8167f9d3`
- `services/learning-service/src/main/resources/db/migration/V3__skill_tracks.sql` — SHA-256 `47efbe46f420fac80ccacd66cc3ee64f16bd3d09e3ba23fcb68b1613409860e7`
- `services/learning-service/src/main/resources/db/migration/V4__practice_attempts.sql` — SHA-256 `46541cc09d2377103364e3b40c64d80de2200e616e2ed08fe8ee4b6e66fc5205`
- `services/learning-service/src/main/resources/db/migration/V5__review_ladder.sql` — SHA-256 `570f6672c2947034e04da687a12d43ab883610a95fcda66e887a79347eb3fed9`
- `services/library-service/src/main/resources/db/migration/V1__create_library_catalog.sql` — SHA-256 `a3a4dbab0c4962dd01782313614443eacd088c66b48086ad3a19e11426d3bb76`
- `services/library-service/src/main/resources/db/migration/V2__create_personal_library.sql` — SHA-256 `a2fc61a2e7518ce6ae86a8bfef46f493b92ad622b692adc83dfd0394f8d8c7f3`
- `services/user-service/src/main/resources/db/migration/V1__create_user_tables.sql` — SHA-256 `e09d8253398c2b30122f5d6ff5de1b75026aa5a3e6cbf868c4f94262c0a89c84`
- `services/user-service/src/main/resources/db/migration/V2__create_identity_tables.sql` — SHA-256 `141d7464409029e0dc86e0480fc74951df10624f64e8e04607a7e1a078ee9e9a`
- `services/user-service/src/main/resources/db/migration/V3__rename_learner_add_roles.sql` — SHA-256 `ab3292e6a58f04b6ea7170fcbe89f0375a63bc54a281360ca246e835f9d25db6`
- `services/user-service/src/main/resources/db/migration/V4__enforce_one_active_learning_goal_per_user.sql` — SHA-256 `534e2cfcfdf64e750957e8f55200febb559b7ff6fc87cd05f5f8df8b00cf544b`
- `services/user-service/src/main/resources/db/migration/V5__create_learning_activity_and_streak_tables.sql` — SHA-256 `9cac11d62b3e26ad2cefbe616041bcaad3b8801b9a23789734a7710aac424a5d`
