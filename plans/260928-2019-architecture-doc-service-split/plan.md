---
title: "Cập nhật system-architecture theo cách chia service mới"
description: "Ghi kiến trúc đích (library-service mới, giải thể learning-support, activity/streak sang user-service, model bài học) vào docs/system-architecture.md: mục đích ngay bây giờ, gộp vào các mục chính khi code đã tách xong."
status: pending
priority: P2
branch: "main"
tags: [docs, architecture, library-service, learning-support-service]
blockedBy: []
blocks: []
created: "2026-09-28T13:19:00.000Z"
createdBy: "ck:plan"
source: skill
---

# Cập nhật system-architecture theo cách chia service mới

## Overview

`docs/system-architecture.md` (§1–§11) mô tả code tại commit `79f9fd6` và tự khẳng định "code là nguồn đúng khi tài liệu này lệch". Cách chia service đích (mục dưới) **chưa triển khai**, nên:

- **Pha 1 (làm ngay):** thêm **§12 "Kiến trúc đích (chưa triển khai)"** theo đặc tả dưới. §1–§11 giữ nguyên.
- **Pha 2 (khi code đã tách xong):** gộp §12 vào §1–§11 theo code thật, xóa §12.

Plan này là **nguồn duy nhất** ghi cách chia đích (plan triển khai trước đó đã được xóa theo chủ ý người dùng). Khi lập plan triển khai mới, lấy đặc tả từ đây.

## Đặc tả kiến trúc đích

### Service, DB, route

| Service | Cổng | DB | Route Gateway | Thay đổi |
| --- | --- | --- | --- | --- |
| user-service | 8085 | `user_db` (local 5432) | `/auth/**`, `/api/users/**`, **`/api/learning-support/{activities,streak}/**`** | + `learning_activities`, `streaks` |
| access-service | 8084 | `access_db` (local 5432) | `/api/access/**` | — |
| content-service | 8082 | `content_db` (local 5432) | `/api/content/**` (trừ video, từ vựng) | − video, từ vựng; + bài học; bỏ `LESSON` khỏi `package_type` |
| **library-service** (mới) | 8081 | `library_db` (compose 5437) | `/api/content/videos/**`, `/api/content/vocabulary/**`, `/api/content/admin/vocabulary/**`, `/api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/**` | Service mới |
| assessment-service | 8083 | `assessment_db` (local 5432) | `/api/assessments/**` | — |
| game-service | 8087 | `game_db` (compose 5435) | `/api/games/**`, `/ws/games/**` | Snapshot `VOCABULARY` lấy từ library |
| community-service | 8089 | `community_db` (local 5432 / compose 5434) | `/api/community/**` | — |
| notification-service | 8088 | `notification_db` (local 5432) | `/api/notifications/**` | — |
| ai-learning-service | 8000 | `ai_learning_db` (compose 5436) | `/api/ai-learning/**` | — |
| ~~learning-support-service~~ | ~~8086~~ | ~~`learning_support_db` (compose 5433)~~ | — | **Xóa** |

Mọi public path giữ nguyên; Gateway (`order: -1`) trỏ theo nhóm path. Reactor vẫn 12 module (thêm library, bỏ learning-support).

### Bảng theo DB (chỉ DB đổi)

- **`content_db`** (14 + outbox): `topics`, `knowledge_points`, `questions`, `question_versions`, `question_knowledge_points`, `content_packages`, `content_package_versions`, `content_sections`, `section_questions`, `content_assets`, `content_asset_links`, **`lessons`** (FK topic), **`lesson_blocks`** (TEXT | ASSET, FK `content_assets`), **`lesson_knowledge_points`** (FK KP), `outbox_events`.
- **`library_db`** (11):
  - Catalog (chỉ `ADMIN`, `CONTENT_AUTHOR` ghi): `vocabulary_items`, `vocabulary_senses`, `learning_videos` (`topic_id` id logic → content), `video_segments`, `video_segment_lexical_entries`.
  - Dữ liệu người học (chủ sở hữu tự ghi): `flashcard_decks`, `flashcards` (FK → `vocabulary_senses`), `flashcard_deck_items`, `notes`, `video_learning_progress` (FK → `learning_videos`), `saved_video_segments` (FK → `learning_videos`, `video_segments`).
- **`user_db`** (10): 8 bảng hiện có + `learning_activities`, `streaks` (không FK tới `users`).

### Giao tiếp mới

| Caller | Callee | Endpoint | Base URL |
| --- | --- | --- | --- |
| library | content | `GET /api/content/topics/{id}` (mới; kiểm topic khi ghi video) | `CONTENT_SERVICE_URL` |
| game | library | `/internal/game-content/snapshots` (nhánh `VOCABULARY`) | `LIBRARY_SERVICE_URL` |
| game | content | `/internal/game-content/snapshots` (chỉ `GRAMMAR`) | `CONTENT_SERVICE_URL` |

Id logic đổi đích: `video_id` (assessment `video_practice_attempts`), `vocabulary_sense_id` (game `game_answers`) → library.

### Quyết định chính

| Quyết định | Lý do | Trade-off |
| --- | --- | --- |
| Tách video + từ vựng khỏi content | Cụm duy nhất không bám KP; chỉ mất FK `learning_videos.topic_id` | Thêm một service Java |
| Gom dữ liệu người học có tham chiếu video/từ vựng vào library | FK thật tới sense/video/segment; join từ vựng khi đọc thẻ | Library có hai loại quyền (catalog vs chủ sở hữu) |
| Activity, streak sang user-service; giải thể learning-support | Không còn bảng nào ở learning-support | Streak vẫn do client tự khai (`PUT /streak`) |
| Bài học có model riêng gắn KP | Bài học không còn bị ép vào package/section | Không versioning bài học |
| Giữ mọi public path qua Gateway | AGENTS §3.5 giữ path tương thích | Tên path `/api/learning-support/**` không còn khớp service |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Thêm mục kiến trúc đích](./phase-01-them-muc-kien-truc-dich.md) | Pending |
| 2 | [Gộp kiến trúc đích vào tài liệu sau khi tách xong](./phase-02-gop-kien-truc-dich-sau-khi-tach-xong.md) | Pending |

## Dependencies

- Pha 1: không phụ thuộc.
- Pha 2: chờ code đã tách thật (library-service tồn tại, learning-support đã xóa). Hiện **chưa có plan triển khai** — cần lập lại từ đặc tả trên.
- Không sửa `.sdd/global/system-architecture.md` (baseline 2026-09-18, AGENTS đánh dấu lỗi thời).

## Acceptance criteria

- [ ] Sau pha 1: §12 có đủ nội dung đặc tả, ghi "chưa triển khai"; §1–§11 không đổi.
- [ ] Sau pha 2: §1–§11 khớp code sau khi tách; không còn §12; không còn `learning-support-service`/`learning_support_db` ngoài ghi chú lịch sử có chủ đích.
