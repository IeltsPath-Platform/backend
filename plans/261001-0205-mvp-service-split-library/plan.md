---
title: "Chia lại service trong MVP: library-service, giải thể learning-support"
description: "Tạo library-service (catalog từ vựng/video từ content + thư viện học cá nhân từ learning-support), chuyển activity/streak sang user-service, gỡ learning-support. Route công khai giữ nguyên. Giao cho agent code theo 4 PR."
status: completed
priority: P2
branch: "feat/main-follow"
tags: [architecture, library-service, learning-support-service, user-service, content-service, game-service, api-gateway]
blockedBy: []
blocks: [260929-1640-lesson-learning-pipeline-mvp]
created: "2026-09-30T19:05:00.000Z"
createdBy: "claude"
source: conversation
---

# Chia lại service trong MVP

Kiến trúc đích: `.sdd/specs/SERVICE_ARCHITECTURE_V3.md` §1, §3, §4, §9. Đặc tả chi tiết cũ (routes, bảng, FK) ở plan tài liệu đã xóa,
đọc bằng `git show 6506d5e:plans/260928-2019-architecture-doc-service-split/plan.md` (mục "Đặc tả kiến trúc đích"); phần
`examiner_profiles` và notification trong đó **không làm**.

## Quyết định (chốt 2026-10-01, plan tổng `260930-2057` Validation Session 2)

| Chủ đề | Quyết định |
| --- | --- |
| Phạm vi | Chỉ chia service: `library-service` mới; giải thể `learning-support-service`; `learning_activities`, `streaks` sang `user-service`. Không làm `examiner_profiles`, bảng notification |
| Thứ tự merge | ~~Sau cùng, sau `260930-1006`~~ → **S1 merge trước 1640** (đổi 2026-10-01, plan tổng Validation Session 3): content V7 là migration xóa bảng, luồng học dùng V8–V13. S2–S4 không đụng migration content/ai-learning, merge lúc nào cũng được |
| Dữ liệu | Không chép dữ liệu. `library_db` và bảng mới ở `user_db` tạo bằng migration. Content V7 xóa 5 bảng từ vựng/video (phá hủy: người dùng duyệt trước khi chạy trên DB dùng chung). Bỏ `learning_support_db` |
| Route công khai | Giữ nguyên mọi path; Gateway trỏ lại theo nhóm path. Không đổi request/response |
| AI Learning | Không đổi |

## Kiến trúc đích

| Service | Cổng | DB | Nhận thêm | Route Gateway mới |
| --- | --- | --- | --- | --- |
| `library-service` (mới, Java, Maven module `services/library-service`, package `com.group01.library`) | 8081 | `library_db` (compose, cổng host 5437) | Catalog từ content: `vocabulary_items`, `vocabulary_senses`, `learning_videos`, `video_segments`, `video_segment_lexical_entries`. Thư viện cá nhân từ learning-support: `flashcard_decks`, `flashcards`, `flashcard_deck_items`, `notes`, `video_learning_progress`, `saved_video_segments` | `/api/content/videos/**`, `/api/content/vocabulary/**`, `/api/content/admin/vocabulary/**`, `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` |
| `user-service` | 8085 | `user_db` | `learning_activities`, `streaks` (migration V5; không FK tới `users`) | `/api/learning-support/{activities,streak}/**` |
| `content-service` | 8082 | `content_db` | Bỏ code và bảng từ vựng/video (V7); thêm `GET /api/content/topics/{id}` cho library kiểm topic khi ghi video | `/api/content/**` còn lại |
| `game-service` | 8087 | — | Snapshot `VOCABULARY` gọi library `/internal/game-content/snapshots` (`LIBRARY_SERVICE_URL`); `GRAMMAR` vẫn gọi content | — |
| ~~`learning-support-service`~~ | ~~8086~~ | ~~`learning_support_db`~~ | Gỡ module, config, compose DB, route | — |

Luật của library (V3 §9):
- Catalog chỉ `ADMIN`, `CONTENT_AUTHOR` ghi; chỉ soft-delete (`status` INACTIVE). `learning_videos.topic_id` là id logic → content.
- Thư viện cá nhân: chủ sở hữu ghi. FK thật tới catalog **`ON DELETE RESTRICT`**: `flashcards.vocabulary_sense_id` → `vocabulary_senses`,
  `video_learning_progress.video_id`, `saved_video_segments.video_id` → `learning_videos`, `saved_video_segments.segment_id` → `video_segments`.
- Không có `outbox_events` ở library (bảng của learning-support chưa từng được dùng).
- Giữ luật hiện có của learning-support (ví dụ `uq_flashcards_user_practice_question`, nguồn `PRACTICE_QUESTION` trỏ id câu luyện của AI Learning).

Id logic đổi đích (không đổi code, chỉ ghi tài liệu): `assessment.video_practice_attempts.video_id`, `game.game_answers.vocabulary_sense_id`,
`content.lesson_block_vocabulary.vocabulary_sense_id` → library.

## PR

| # | PR | Điểm dừng | Trạng thái |
| --- | --- | --- | --- |
| S1 | library-service + catalog từ content; content V7; game `VOCABULARY` → library; Gateway | **Dừng trước khi chạy V7 trên DB dùng chung** | Xong (`465f543`); chờ người dùng duyệt V7 trên DB local |
| S1b | Đổi content V13 → V7; `LIBRARY_SERVICE_URL` mặc định localhost | — | Xong (`0da2eb2`) |
| S2 | Thư viện cá nhân từ learning-support → library (library V2, 4 FK RESTRICT); Gateway | — | Xong (`d762660`) |
| S3 | activity/streak → user-service; gỡ learning-support | **Dừng trước khi xóa module** (báo danh sách file và route bị gỡ) | Xong (`fec5d14` chuyển, user V5; `ab613b1` gỡ module sau khi người dùng duyệt) |
| S4 | Tài liệu | — | Xong (`9fd7ada`) |

Nhánh: `feat/library-personal-library` chứa S1, S1b (cherry-pick) và S2; `feat/learning-support-removal` nối tiếp với S3, S4: **nhánh cần merge** (chứa đủ S1–S4). Chưa push.
Còn lại ngoài code: người dùng duyệt content V7 trên `content_db` local trước khi chạy content-service từ nhánh này. `feat/lesson-library-catalog-split` là bản cũ
của S1/S1b (`99d5347`, `081f22a`), không merge nhánh đó để tránh trùng commit.

Thứ tự `S1 → S1b → S2 → S3 → S4`. S1 + S1b merge vào `feat/main-follow` trước khi plan 1640 thêm migration content.

## Quy tắc chung cho agent code

Theo mục "Quy tắc chung" trong `plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md` (đọc trước, làm theo), **trừ** điều cấm
"tạo service mới hay chuyển bảng giữa service": plan này chính là việc đó. Thêm:
- Chuyển code nguyên hành vi: giữ layer (`api → application → domain`, `infrastructure`), đổi package sang `com.group01.library`; chuyển
  test cùng code. Không viết lại logic nếu không bắt buộc.
- Không đổi public path, method, request/response; không đổi JWT claim, role. Test controller cũ của các route chuyển đi phải pass ở
  service mới (chỉ đổi package/import).
- Migration: không sửa migration cũ. Chỉ chạy trên Testcontainers hoặc DB tạm cho tới khi người dùng duyệt.
- Cấu hình theo mẫu service Java có sẵn: `application.yml` (bootstrap), `infra/config-server/config-repo/library-service.yaml` (runtime,
  secret qua biến môi trường, **không** thêm giá trị fallback cho secret), import `.env` như các service khác, `Dockerfile.spring-service`.

## Prompt

### S1

```text
Làm PR S1 của plan plans/261001-0205-mvp-service-split-library/plan.md (đọc hết plan.md trước, rồi mục "Quy tắc chung" trong
plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md, theo ngoại lệ ghi trong plan.md).

Việc:
1. Tạo module services/library-service (Spring Boot, cổng 8081, DB library_db; thêm vào pom.xml reactor; compose thêm library-db
   cổng host 5437; config-repo library-service.yaml; đăng ký Eureka tên LIBRARY-SERVICE).
2. Chuyển catalog từ vựng/video từ content sang library: 5 bảng (library V1, schema như content V1 hiện tại), 56 file Java liên quan
   (VocabularyController, LearningVideoController và domain/application/infrastructure/test đi kèm), cùng public path.
   Việc ghi video kiểm topic bằng GET /api/content/topics/{id} mới ở content (forward bearer + X-Correlation-Id, timeout, lỗi → 503).
3. Content: xóa code đã chuyển; V13__drop_vocabulary_and_video_tables.sql xóa 5 bảng. Nhánh VOCABULARY của
   GetGameContentSnapshotUseCase chuyển sang library (/internal/game-content/snapshots, cùng request/response); content chỉ còn GRAMMAR.
4. Game: chọn đích snapshot theo learningDomain (VOCABULARY → library qua LIBRARY_SERVICE_URL, GRAMMAR → content).
5. Gateway: route library cho /api/content/videos/**, /api/content/vocabulary/**, /api/content/admin/vocabulary/** đặt TRƯỚC route
   /api/content/**; thêm path vào internal-jwt-paths nếu cần. Không đổi route khác.
6. Contract snapshot game ghi vào docs/contracts/ (một contract, hai nơi cài).

Test: test cũ của vocabulary/video chạy ở library; test game chọn đúng đích; Gateway route; content không còn bảng/Bean từ vựng.
Regression: mvn -q compile -DskipTests; mvn -q -pl services/library-service -am test; content, game, api-gateway test.
V13 xóa dữ liệu: chỉ chạy trên Testcontainers. DỪNG trước khi chạy trên DB dùng chung, báo Status: NEEDS_DECISION.
```

### S1b (đổi số migration, sau S1)

```text
Sửa nhỏ cho PR S1 của plan plans/261001-0205-mvp-service-split-library/plan.md (đọc mục "Quyết định" và "PR" trước; quy tắc chung
như S1). Lý do: plan chia service giờ merge TRƯỚC luồng học 1640, nên migration xóa bảng của content phải là V7 (1640 dùng V8, V9).

1. git mv services/content-service/src/main/resources/db/migration/V13__drop_vocabulary_and_video_tables.sql
   → V7__drop_vocabulary_and_video_tables.sql. Không đổi nội dung SQL. Grep "V13" trong services/ và docs/ để sửa chỗ còn nhắc
   (nếu có; test hiện không nhắc số version).
2. infra/config-server/config-repo/game-service.yaml: LIBRARY_SERVICE_URL mặc định http://localhost:8081 (service chạy trên host,
   compose không có container library-service). Không đổi CONTENT_SERVICE_URL (cạm bẫy đã ghi trong CLAUDE.md).
3. Chạy lại khi Docker có sẵn: mvn -q -pl services/content-service -am test; mvn -q -pl services/library-service -am test;
   mvn -q -pl services/game-service -am test. Báo riêng số test skip; test migration Testcontainers (CatalogRemovalMigrationTest,
   LibraryCatalogMigrationTest) phải CHẠY và pass, không được skip. Docker không có thì Status: BLOCKED, không báo DONE.
4. Không chạy content-service trên content_db dùng chung. V7 xóa vĩnh viễn 5 bảng: DỪNG, báo Status: NEEDS_DECISION kèm số dòng
   hiện có của 5 bảng (người dùng tự đếm nếu agent không có quyền DB) để người dùng quyết sao lưu và duyệt.
Commit: refactor: renumber catalog removal migration ahead of lesson migrations (không nhắc plan/phase).
```

### S2

```text
Làm PR S2 của plan plans/261001-0205-mvp-service-split-library/plan.md (S1 đã xong; đọc plan.md và quy tắc chung như S1).

Chuyển 6 bảng thư viện cá nhân từ learning-support sang library (library V2): flashcard_decks, flashcards, flashcard_deck_items,
notes, video_learning_progress, saved_video_segments, gồm các luật của learning-support V1–V3 (index, uq_flashcards_user_practice_question).
Thêm FK ON DELETE RESTRICT tới catalog như mục "Luật của library". Chuyển controller, use case, domain, persistence, test tương ứng
(Flashcard, FlashcardDeck, Note, VideoProgress, SavedVideoSegment), cùng path /api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}.
Nơi learning-support đang kiểm id video/segment/sense bằng HTTP (nếu có) chuyển thành kiểm trong cùng DB.
Gateway: route 5 nhóm path trên sang library, đặt trước route /api/learning-support/**.
Chưa gỡ learning-support ở PR này (S3 làm). Regression: library, api-gateway test; learning-support test vẫn chạy.
```

### S3

```text
Làm PR S3 của plan plans/261001-0205-mvp-service-split-library/plan.md (S1, S2 đã xong; đọc plan.md và quy tắc chung như S1).

1. user-service: V5 tạo learning_activities, streaks (schema như learning-support V1, không FK tới users); chuyển code activity và streak
   (LearningActivityController, StreakController và use case/domain/persistence/test) sang package của user-service theo layout của nó
   (domain/repository, không có port), cùng path /api/learning-support/{activities,streak}.
2. Gateway: route /api/learning-support/{activities,streak}/** sang user-service, trước route /api/learning-support/**.
3. Khi mọi path /api/learning-support/** đã có đích mới: DỪNG, báo Status: NEEDS_DECISION kèm danh sách sẽ gỡ. Sau khi người dùng
   đồng ý: gỡ services/learning-support-service, module trong pom.xml, config-repo learning-support-service.yaml, route và
   learning-support-db trong docker-compose.yml, route Gateway cũ. Không migration nào cho learning_support_db.
Regression: mvn -q compile -DskipTests; user-service, library-service, api-gateway test; docker compose config --quiet.
```

### S4

```text
Làm PR S4 của plan plans/261001-0205-mvp-service-split-library/plan.md (S1–S3 đã xong; đọc plan.md và quy tắc chung như S1).

Cập nhật tài liệu theo code thật (grep kiểm từng dữ kiện trước khi ghi): AGENTS.md và CLAUDE.md (bảng module, cổng, DB, route, lệnh
chạy local, cạm bẫy; bỏ learning-support, thêm library), README.md §6, README của library-service (mới), user-service, content-service,
game-service; docs/system-architecture.md (service và dữ liệu, giao tiếp, luồng); .sdd/specs/SERVICE_ARCHITECTURE_V3.md đổi trạng thái
phần chia service thành đã triển khai; .sdd/database/DATABASE_V5.md (bảng từ vựng/video và thư viện cá nhân thuộc library_db,
activity/streak thuộc user_db). Cập nhật dòng "Cập nhật lần cuối … commit" ở AGENTS.md, CLAUDE.md. Không sửa .sdd/global.
Chạy graphify update . --force nếu có graphify.
```
