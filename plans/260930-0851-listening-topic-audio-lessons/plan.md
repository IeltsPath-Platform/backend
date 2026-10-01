---
title: "Listening trong luồng học (topic riêng, audio mp3 qua URL)"
description: "Topic Listening đi trọn pipeline: bài học có khối audio + bài tập tự chấm, gói luyện thêm/ôn có audio, đề cuối có audio; mp3 trên cloud, DB lưu key, transcript ẩn tới khi đạt; không migration schema."
status: in-progress
priority: P2
branch: "feat/main-follow"
tags: [feature, backend, content, assessment, ai-learning, listening, tdd]
blockedBy: [260929-1640-lesson-learning-pipeline-mvp, 260930-0812-lesson-writing-task1-academic]
blocks: [260930-1006-reading-question-hints]
created: "2026-09-30T01:59:09.555Z"
createdBy: "ck:plan"
source: skill
mode: "tdd"
---

# Listening trong luồng học (topic riêng, audio mp3 qua URL)

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java, route `/api/learning/**` (plan `261001-1228`, làm trước plan này). Phần ai-learning của plan đọc theo [bảng ánh xạ](../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md).

## Overview

Thêm topic `DEMO_LISTENING` đi trọn pipeline học như Reading:
- bài học có khối audio (`ASSET` loại `AUDIO`) và khối bài tập tự chấm;
- luyện thêm và ôn bằng gói `PRACTICE_SET` có audio;
- đề cuối `TOPIC_TEST` có section Listening.

Câu Listening dùng dạng có sẵn (`MULTIPLE_CHOICE`; `FILL_IN_BLANK` cho form và note completion), chấm theo `answer-spec-v1`.
Mastery, luật luyện thêm, giao mã đề, consumer giữ nguyên. **Không migration schema** ở cả ba service.

Scope đã chốt với người dùng ngày 2026-09-30 (HOLD). Chế độ `--tdd`.

## Quyết định

| Chủ đề | Quyết định |
| --- | --- |
| Cấu trúc | Topic Listening riêng: có bài, có gói luyện, có đề; vào thứ tự học theo `sort_order` như topic khác |
| Nơi lưu audio | mp3 trên object storage cloud (S3, R2, GCS, Supabase…), bucket public-read cho MVP. Team tự upload; mp3 không commit vào git |
| Giá trị trong DB | `content_assets.media_reference` lưu **key** (ví dụ `listening/demo/ls1.mp3`); content ghép `CONTENT_MEDIA_BASE_URL` khi trả ra. Chấp nhận thêm URL `https://` đầy đủ. Lý do: seed Flyway không sửa được, đổi bucket chỉ cần đổi env |
| Transcript | `content_assets.text_content` của asset AUDIO. **Ẩn tới khi đạt:** bài học hiện khi bài xong; gói ôn và đề cuối hiện khi ≥ 70%, cùng lời giải |
| Lộ transcript | Khóa `GET /api/content/assets/{id}` với CUSTOMER (hiện ai đăng nhập cũng đọc được `text_content`). Assessment trả section cho học viên theo allowlist; transcript nằm ở khóa `solution` |
| Nghe lại | Không giới hạn trong bài học và gói ôn. Giới hạn số lần nghe (cần đếm ở server) để sau |
| KP và ADP | 4 KP kỹ năng con, đều `PROCEDURE`, `kind = STRATEGY`: `LS_NUM`, `LS_SPELL`, `LS_PARA`, `LS_TRAP`; mỗi câu 1 KP chính. Gói luyện **không gắn độ khó**, **1 gói mỗi KP** (chốt 2026-10-01), chọn theo luật của 1640 (trượt thì giao lại gói giao lâu nhất); chọn gói theo độ khó hoãn ngoài MVP (plan 0908 đã xóa 2026-09-30). [brainstorm ADP](../reports/brainstorm-260930-0908-listening-adaptive-path-report.md) |
| Không làm | Upload API, signed URL, giới hạn lượt nghe, dictation/shadowing, YouTube, TTS |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Contract va khoa lo transcript](./phase-01-contract-va-khoa-lo-transcript.md) | Completed (khóa asset + contract nội bộ ở #30; phần học viên của `lesson-learning-v1` ở `feat/learning-reviews-tests`) |
| 2 | [Content media URL va seed Listening](./phase-02-content-media-url-va-seed-listening.md) | Completed (nhánh `feat/writing-access-content`) |
| 3 | [Assessment de cuoi co audio](./phase-03-assessment-de-cuoi-co-audio.md) | Completed (nhánh `feat/lesson-listening-final-test`; regression 125 pass, 0 skip) |
| 4 | [AI Learning bai hoc va goi on co audio](./phase-04-ai-learning-bai-hoc-va-goi-on-co-audio.md) | Completed (learning-service, nhánh `feat/learning-reviews-tests`) |
| 5 | [Tai lieu](./phase-05-tai-lieu.md) | Pending |

Thứ tự: `1 → 2 → (3 ∥ 4) → 5`.

## Dependencies

- **Quy ước chung của lộ trình MVP** (`260930-2057`, chốt 2026-10-01):
  - Nội dung seed: [`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md); KP mới `kind = STRATEGY`.
  - Kiểm media: một hàm ở content domain, một mã lỗi `INVALID_MEDIA_REFERENCE`. 0812 viết hàm (nhận `https://` và `data:image/(png|jpeg|svg+xml);base64,…` cho IMAGE); 0851 mở rộng cho AUDIO (key + `CONTENT_MEDIA_BASE_URL`). DB lưu `content_assets.media_reference` (giá trị gốc: URL, data URI hoặc key); content resolve thành `mediaUrl` (URL đầy đủ) ngay trong payload nội bộ; ai-learning chỉ chuyển `mediaUrl` cho học viên, cho cả ảnh và audio.
  - Test tập key của response học viên: kiểm "không chứa key cấm" (`answerSpec`, `chartFacts`, `explanation`/`transcript`/`solution` trước khi đạt) thay vì so bằng đúng một tập key, để plan sau thêm trường không làm vỡ test plan trước.
  - Số migration cố định: content V7 (chia service S1, merge trước), V8/V9 (1640), V10 (0737), V11 (0812),
    V12 (0851), V13 (1006); ai-learning V12 (0737).
- **blockedBy `260929-1640-lesson-learning-pipeline-mvp`:** cần bảng bài học (content V8), endpoint `/internal/learning-content/*`,
  assessment tự chấm và lấy đề từ content, API bài học và bài ôn của ai-learning, allowlist chặn lộ đáp án.
- **blockedBy `260930-0812-lesson-writing-task1-academic`** (thứ tự merge của lộ trình): plan này **mở rộng**
  `MediaReferencePolicy` của 0812 cho AUDIO và dùng lại cách load asset theo tập. Không còn chờ plan chọn gói theo độ khó
  (0908 đã xóa). Đã validate lại (Validation Session 1 dưới đây).
- Seed content: `V12__seed_listening_demo.sql`.
- Hạ tầng: một bucket cloud có URL công khai. `CONTENT_MEDIA_BASE_URL` đặt trong `.env` và config-repo content.

## Tiêu chí nghiệm thu

- Thứ tự học có `DEMO_LISTENING` đứng cuối (`sort_order` 920, sau `TFNG_SKILLS`); mở khi topic trước PASSED.
- `GET /lessons/{LS1}`: khối audio có `mediaUrl` là URL đầy đủ (base + key) và `durationSeconds`, **không** có
  `transcript`. Xong bài thì có `transcript`.
- Nộp bài tập form completion: chấm theo `answer-spec-v1` (sai chính tả là sai); KP yếu kèm câu sai → luyện thêm bằng gói có audio.
- Gói ôn: `GET /reviews/{id}` trả audio không có transcript; nộp đạt ≥ 70% thì có transcript trong lời giải.
- Đề cuối Listening: cấu trúc attempt trả `audio.url`, không có `transcript` hay `solution`; kết quả ≥ 70% có transcript.
- CUSTOMER gọi `GET /api/content/assets/{id}` → 403.
- Đổi `CONTENT_MEDIA_BASE_URL` → URL trả ra đổi theo, không cần migration.

## Câu hỏi mở

- Chọn dịch vụ cloud cụ thể (không ảnh hưởng code; chỉ cần URL công khai, `Content-Type: audio/mpeg`, hỗ trợ range request).
- Ai thu âm hoặc tạo file mp3 cho seed demo (8 file ngắn: 2 bài, 4 gói, 2 mã đề; transcript ở `seed-content.md`).

## Validation Log

### Session 1 — 2026-10-01
**Trigger:** roadmap `260930-2057` phase 2 (plan tự ghi cần validate lại sau khi 0908 bị hoãn).
**Questions asked:** 1 (hỏi trong phiên review sẵn sàng giao agent code, `plans/reports/review-261001-0116-mvp-plans-codex-readiness-report.md`)

#### Verification Results
- `knowledge_points.kind` NOT NULL + CHECK (`V1:23`): 4 KP Listening cần `kind` → `STRATEGY` (quyết định chung).
- `AttemptStructureResponse.Section.snapshot` là `String` (`AttemptStructureResponse.java:3`); hiện snapshot do client gửi. 1640 phase 4
  (đã sửa) dựng `section_snapshot = {title, skill, instructions, passage}` và giữ kiểu chuỗi; plan này thêm `audio`, `solution` và
  đổi sang DTO allowlist.
- `content_sections` có `title`, `skill`, `instructions` (`V1:96-106`); `content_asset_links` gắn asset theo `section_id` hoặc
  `question_version_id` (`V1:189-199`). `ContentAssetRepositoryAdapter.findBySectionId` gọi `findById` trong vòng lặp (N+1).
- DTO học viên dùng `extra="forbid"`: `model_validate` thẳng payload content có `transcript` sẽ lỗi 500; phải map sang model riêng.
- Nội dung seed (transcript 8 audio, câu, đáp án) có trong `seed-content.md`; gói `PS-PARA` có 3 câu, các gói Listening khác 3 câu.

#### Questions & Answers
1. **[Scope]** Số gói luyện mỗi KP khi bỏ độ khó? — Options: 1 gói mỗi KP | 2 gói mỗi KP. **Answer:** 1 gói mỗi KP (8 mp3).

#### Impact on Phases
- Phase 2: mở rộng `MediaReferencePolicy` (thay `MediaUrlResolver`); seed theo `seed-content.md`, 1 gói mỗi KP, không `difficulty`;
  load asset theo tập section; test seed.
- Phase 3: bỏ nhánh điều kiện, đổi `snapshot` sang DTO allowlist là việc của plan này.
- Phase 4: DTO riêng không có `transcript`; test gói ôn theo 1 gói mỗi KP (3 câu); bằng chứng lần nộp đầu.
- Phase 5: ghi độ khó đã hoãn.

#### Whole-Plan Consistency Sweep
- Đã tìm "theo mức", `difficulty`, "EASY", "HARD", "12 file", "8 gói", `MediaUrlResolver`, `mediaReference` (trường học viên),
  "1/4", "4/4", "Nếu phase 2 của plan 1640" trong plan.md và phase 1–5.
- Unresolved contradictions: 0.
