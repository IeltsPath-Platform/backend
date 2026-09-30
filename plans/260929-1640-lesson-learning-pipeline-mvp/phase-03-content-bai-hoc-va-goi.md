---
phase: 3
title: "Content: bài học, gói, endpoint nội bộ, seed"
status: pending
priority: P1
dependencies: [1]
effort: "2–3 ngày"
---

# Phase 3: Content: bài học, gói, endpoint nội bộ, seed

## Overview

Content có bài học (5 bảng mới), gói `TOPIC_TEST` gắn topic, 6 endpoint nội bộ `/internal/learning-content/*` cho ai-learning và assessment. Snapshot game không được lấy câu thuộc đề, gói ôn hay bài học. Seed đủ để chạy trọn luồng.

## Requirements

- Functional: đủ 6 endpoint của `learning-content-internal-v1.md`. Chỉ trả bài, gói, câu hỏi đã `PUBLISHED`.
- `GET /topic-sequence`: topic ACTIVE có ≥ 1 bài PUBLISHED và ≥ 1 gói `TOPIC_TEST` PUBLISHED, theo `sort_order`. Topic cha hoặc topic không có bài không vào thứ tự học. Mỗi topic **kèm KP ACTIVE** (Validation Session 2): `[{topicId, code, name, skill, sortOrder, knowledgePoints: [{id, code, name, learningType, skill, description, hasPracticeSet}]}]`; KP ACTIVE của topic, sắp theo `created_at` rồi `id`; không `answerSpec`, không band. `hasPracticeSet` xem mục dưới seed.
- `POST /practice-sets/search`:
  - gói `PRACTICE_SET` PUBLISHED có ≥ `minQuestions` (mặc định 3) câu, trong đó có câu đo KP;
  - loại `excludePackageIds`;
  - không trả gói có câu trùng câu bài học hoặc đề cuối của **bất kỳ** topic;
  - `limit` mặc định 1, tối đa 10.
- `GetGameContentSnapshotUseCase` từ chối câu thuộc `TOPIC_TEST`, `PRACTICE_SET` hoặc `lesson_block_questions` (400). Game không còn là nơi dò đúng/sai cho các câu này.
- Không tạo được gói `TOPIC_TEST` qua `POST /api/content/packages` (chỉ seed): trả 400, không để CHECK của DB trả 500.
- Non-functional:
  - không N+1 (AGENTS §3.7): mỗi endpoint một số query cố định, lấy theo lô bằng `IN`;
  - review SQL cùng test Testcontainers (repo chưa có gate đếm query).

## Architecture

**Migration `V8__lessons_and_topic_tests.sql`** (không sửa V1–V6):
- `lessons`: `id`, `topic_id` FK, `code` UQ, `title`, `summary`, `sort_order` (UQ theo topic), `status` `DRAFT|PUBLISHED|ARCHIVED`, `created_at`, `updated_at`.
- `lesson_blocks`: `id`, `lesson_id` FK, `sort_order` (UQ theo lesson), `block_type` `TEXT|ASSET|VOCABULARY|EXERCISE`, `text_content`, `asset_id` FK NULL, CHECK theo `block_type`.
- `lesson_block_vocabulary`: PK(`block_id`, `vocabulary_sense_id`), `sort_order`. Id logic, **không FK**: MVP chia service,
  từ vựng sang `library-service`. Seed MVP chưa có khối `VOCABULARY`; bảng và kiểu khối vẫn tạo theo thiết kế.
- `lesson_block_questions`: PK(`block_id`, `question_version_id`), `sort_order`, INDEX(`question_version_id`).
- `lesson_knowledge_points`: PK(`lesson_id`, `knowledge_point_id`), INDEX(`knowledge_point_id`).
- `content_packages.topic_id` UUID NULL FK `topics`, INDEX; CHECK `package_type <> 'TOPIC_TEST' OR topic_id IS NOT NULL`.
- **Bỏ band của KP** (người dùng chốt 2026-09-29):
  - `ALTER TABLE knowledge_points DROP CONSTRAINT chk_knowledge_points_band_min, chk_knowledge_points_band_max, chk_knowledge_points_band_order, DROP COLUMN band_min, DROP COLUMN band_max`;
  - band của `topics` giữ nguyên.
- CHECK `package_type`: **thêm** `TOPIC_TEST`, **giữ** `LESSON` (`GetReadingPassageUseCase.java:35` còn dùng). Dùng `DROP CONSTRAINT IF EXISTS content_packages_package_type_check`; tên thật được kiểm bằng test ở bước 1.

**Domain** (layout của content: `domain/repository`, adapter trong `infrastructure/persistence/adapter`):
- `domain/aggregate/Lesson` + `domain/entity/LessonBlock` + `domain/vo/BlockType`.
- `PackageType` thêm `TOPIC_TEST`. **Không** thêm `topicId` vào aggregate `ContentPackage`: endpoint nội bộ đọc cột qua query projection. Nhờ vậy không đụng factory, constructor và mapper của package.
- `CreateContentPackageUseCase` từ chối `TOPIC_TEST`.
- **Bỏ band khỏi KP:**
  - `KnowledgePoint` bỏ field `band`;
  - request/command tạo KP bỏ `bandMin`/`bandMax`;
  - `KnowledgePointResult`/`KnowledgePointResponse` bỏ `band` và `effectiveBand`;
  - `GetKnowledgePointsUseCase` bỏ `orInherit`.
  - `BandRange` VO **giữ** cho topic. Bỏ `orInherit` nếu không còn nơi gọi.

**Endpoint nội bộ** `api/controller/InternalLearningContentController` (`/internal/learning-content`), DTO trong `api/dto/internal/`. Query plan:

| Endpoint | Query plan |
| --- | --- |
| `GET /topic-sequence` | Một query topic kèm `EXISTS` bài PUBLISHED và `EXISTS` gói `TOPIC_TEST` PUBLISHED, rồi một query KP ACTIVE `WHERE topic_id IN (…)` kèm cột `hasPracticeSet` (`EXISTS` theo predicate của search); ghép bằng `Map` |
| `GET /topics/{id}/lessons` | Bài PUBLISHED theo `sort_order`, rồi KP và khối EXERCISE theo `lesson_id IN` |
| `GET /lessons/{id}` | Bài và khối (entity graph), rồi theo lô: câu của khối, KP của câu, asset, vocabulary sense. Trả `answerSpec`, `explanation`, `knowledgePointIds` cho từng câu, `knowledgePointIds` cho bài |
| `GET /topics/{id}/test-packages` | Gói `TOPIC_TEST` PUBLISHED của topic, kèm version PUBLISHED hiện hành |
| `POST /practice-sets/search` | Một query SQL theo luật ở Requirements; sắp theo số câu đo KP giảm dần, rồi `id` |
| `GET /package-versions/{id}` | Truy theo version id (không nạp cả aggregate như `findDistinctByVersions_Id`), rồi theo lô sections, section_questions, question versions, KP mappings, assets. Trả `packageId`, `packageType`, `topicId`, `rules`, sections (`sectionId`, `title`, `skill`, `instructions`, `sortOrder`, `passage` = text của asset PASSAGE gắn section), items (`questionVersionId`, `sortOrder`, `stem`, `options`, `answerSpec`, `explanation`, `maxScore`, `knowledgePointMappings`) |

Mẫu tham khảo: `GetGameContentSnapshotUseCase.java:73-97`, `InternalAssessmentContentController.java`, `GetQuestionKnowledgePointMappingsUseCase.java` (validate, giới hạn).

**Seed `V9__seed_lesson_pipeline_demo.sql`**: **nội dung đầy đủ ở [`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md)** (đoạn văn, câu, `options`, `answer_spec`, `explanation`, khối bài học, gói, mã đề, kịch bản Lan có số mastery). Chỉ seed các mục ghi "V9 (1640)" trong file đó. Tóm tắt:
- Topic `DEMO_READING` (đã có, `sort_order` 900) và topic mới `TFNG_SKILLS` (`sort_order` 910).
- KP1–KP4 cho DEMO_READING (dùng lại `DEMO_READING_MAIN_IDEA` làm KP1), KP5 cho TFNG. KP mới: `kind = STRATEGY`, `learning_type = PROCEDURE` (`kind` NOT NULL, `V1:23`; chốt 2026-10-01).
- Bài L1–L4 và TF1, kèm `lesson_knowledge_points`. Đoạn *Green roofs* là asset `PASSAGE` mới (không dùng đoạn của V6); câu Q1, Q3, Q4, Q5, Q11, Q12, Q13, QT1. L3 và L4 chưa có khối essay (V10, V11 thêm sau).
- Mã đề `TOPIC_TEST`:
  - DEMO_READING có 2 mã, mỗi mã 4 câu đo KP1–KP4 (mã A `X1` dùng *Street trees*: Q2, Q14, Q15, Q16; mã B `X2` dùng *Recycling plastic*: PL1–PL4);
  - TFNG_SKILLS có 1 mã (`X5`, đoạn *Hillside community garden*, TT1–TT3, 3 câu KP5; soạn thêm vì demo không có).
- **6 gói `PRACTICE_SET`:** KP1 × 2 và KP2 × 2 (kịch bản Lan: luyện thêm KP1 sau L2, ôn KP2 sau đề, mỗi KP đủ cho một lần trượt), KP3 × 1 và KP4 × 1. Mỗi gói 1 đoạn văn + 4 câu. **KP5 không có gói**: dùng để test luật "KP không có gói luyện thì không chèn bài ôn" (phase 6).
- Mọi câu có `answer_spec` theo v1 và có `explanation`. Không sửa V4, V6. Gói V4 (1 câu) bị search loại nhờ `minQuestions`.
<!-- Updated: Validation Session 3 - seed đầy đủ ở seed-content.md; kind STRATEGY; mã đề TFNG soạn thêm; KP5 không có gói -->

**`hasPracticeSet` trong `topic-sequence`** (Validation Session 3): mỗi KP trả thêm `hasPracticeSet` = có ít nhất một gói thỏa **cùng điều kiện** với `POST /practice-sets/search` (không có `excludePackageIds`, `minQuestions` mặc định). Dùng chung một predicate SQL với search để hai nơi không lệch. Tính trong query KP theo lô (`EXISTS` tương quan), không query theo từng KP.

## Related Code Files

- Create:
  - `db/migration/V8__lessons_and_topic_tests.sql`, `db/migration/V9__seed_lesson_pipeline_demo.sql`
  - domain: `Lesson`, `LessonBlock`, `BlockType`, `LessonRepository`, `LessonNotFoundException`
  - persistence: JPA entity, repository, mapper, adapter cho lesson; repository hoặc query projection cho topic-sequence, practice search, package version
  - `application/usecase/{GetTopicSequenceUseCase,GetTopicLessonsUseCase,GetLessonContentUseCase,GetTopicTestPackagesUseCase,SearchPracticeSetsUseCase,GetPackageVersionContentUseCase}.java` + `application/result/*`
  - `api/controller/InternalLearningContentController.java`, `api/dto/internal/*`
- Modify:
  - `domain/vo/PackageType.java` (+`TOPIC_TEST`)
  - `application/usecase/CreateContentPackageUseCase.java` (từ chối `TOPIC_TEST`)
  - `application/usecase/GetGameContentSnapshotUseCase.java` (từ chối câu của đề, gói ôn, bài học)
  - `api/exception/GlobalExceptionHandler.java` (404 `LessonNotFoundException`)
- Modify (bỏ band của KP):
  - `domain/aggregate/KnowledgePoint.java:24,30,44,57-60,87`
  - `api/dto/request/CreateKnowledgePointRequest.java:35-40`
  - `application/command/CreateKnowledgePointCommand.java:18`
  - `application/result/KnowledgePointResult.java:24-25`
  - `api/dto/response/KnowledgePointResponse.java:26-30,45-48`
  - `application/usecase/CreateKnowledgePointUseCase.java:42,58`
  - `application/usecase/GetKnowledgePointsUseCase.java:59`
  - `api/controller/KnowledgePointController.java:46`
  - JPA entity và mapper của KP
  - `domain/vo/BandRange.java` (bỏ `orInherit` nếu không còn nơi gọi)
  - Tests: `KnowledgePointControllerTest`, `GetKnowledgePointsUseCaseTest`, `BandRangeMigrationTest` (cột KP không còn; cột topic còn)
- Tests: `GetReadingPassageUseCaseTest` (vẫn chỉ đọc PRACTICE_SET), test game snapshot, `ContentPackageTest` và `PublishContentPackageUseCaseTest` (enum mới)

## Implementation Steps

**Tests Before:**
1. Testcontainers: migrate V1–V6, đọc tên thật của constraint `package_type` và các CHECK hiện có.
2. Chạy test hiện có của reading, game snapshot, publish package, ghi baseline.

**Tests After** (viết trước code):
3. Migration: V8 + V9 chạy sạch; `TOPIC_TEST` không có `topic_id` bị chặn; bài trùng `sort_order` trong một topic bị UQ chặn; dòng `LESSON` vẫn hợp lệ.
4. Seed:
   - mọi `answer_spec` parse được theo v1;
   - không câu nào vừa ở gói luyện tập vừa ở bài học hay đề;
   - search cho KP2 trả 2 gói khác nhau qua hai lần (`excludePackageIds`);
   - search KP1 không trả gói V4;
   - `topic-sequence` = [DEMO_READING, TFNG_SKILLS], mỗi topic có đúng KP ACTIVE của nó (KP1–KP4, KP5), không có KP INACTIVE;
   - `hasPracticeSet`: KP1–KP4 `true`, KP5 `false`; KP chỉ có gói V4 (1 câu) → `false`;
   - seed khớp `seed-content.md`: đáp án đúng của từng câu (so bằng bộ chấm `answer-spec-v1`) và KP của từng câu đúng như bảng.
4b. Bỏ band của KP:
   - migration: cột band của KP không còn, band của topic còn;
   - `GET /api/content/knowledge-points` không có khóa band nào;
   - `POST` KP có `bandMin` → bị bỏ qua (Jackson bỏ qua trường lạ), KP vẫn tạo được.
5. Use case (Mockito): search loại `exclude`, lọc `minQuestions`, sắp đúng, giới hạn `limit`; chi tiết bài trả KP và câu theo `sort_order`; tạo gói `TOPIC_TEST` qua API → 400.
6. Game snapshot với câu thuộc `TOPIC_TEST` hoặc lesson block → 400.
7. Controller `/internal/learning-content/*`: 404 cho id không có; body khớp contract.

**Implement:** V8 → domain và persistence → use case → controller → sửa game snapshot và create package → V9.

**Regression Gate:**
```powershell
mvn -q -pl services/content-service -am test
```

## Success Criteria

- [ ] 6 endpoint nội bộ đúng `learning-content-internal-v1.md`; không N+1 (review SQL).
- [ ] Seed đủ cho kịch bản Lan và cho một lần ôn trượt KP2.
- [ ] Reading và game vẫn chạy; game không lấy được câu của đề, gói ôn, bài học.

## Risk Assessment

- **ai-learning còn đọc `effectiveBandMin/Max` tới hết phase 5:** `curriculum_scope.py:86-87` đọc bằng `.get`, nên thiếu trường thì thành "không giới hạn" và không lỗi. Không phụ thuộc thứ tự triển khai.
- **Seed lớn** (khoảng 40 câu): chia khối SQL có comment theo bài/gói; test seed bắt trùng câu và spec sai.
- **Chặn câu ở game snapshot làm vỡ game đang dùng câu seed V4 (gói `PRACTICE_SET`):** trước khi sửa, grep test và seed của game-service xem dùng câu nào. Nếu game dùng câu V4, chỉ chặn câu thuộc `TOPIC_TEST`, lesson block, và các gói `PRACTICE_SET` đủ `minQuestions` (gói ôn).
- **`/internal/**` nhận mọi internal JWT:** Gateway chặn tường minh ở phase 2; secret fallback là vấn đề riêng (`plan.md`).
