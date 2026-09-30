---
phase: 1
title: "Contract và content Task 1"
status: pending
priority: P1
dependencies: []
effort: "1 ngày"
---

# Phase 1: Contract và content Task 1

## Context Links

- Đợt 1: `plans/260930-0737-lesson-writing-task2-essay/phase-01-*.md` (contract), `phase-02-*.md` (`LessonBlockKind`, seed V10)
- `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql:189-200` (`content_asset_links`
  có `question_version_id`, CHECK một owner)
- `docs/contracts/answer-spec-v1.md`, `learning-content-internal-v1.md`, `lesson-learning-v1.md`, `lesson-writing-v1.md`

## Overview

Mở rộng dạng `ESSAY` sang Task 1, gắn ảnh biểu đồ vào câu, seed một đề demo. Không migration schema.

## Requirements

**Contract:**
- `answer-spec-v1.md`, dạng `ESSAY`:
  - `task` nhận `TASK_1 | TASK_2`;
  - với `TASK_1`: `chartFacts` bắt buộc, chuỗi 1–2.000 ký tự; `minWords` mặc định 150;
  - thêm vector: TASK_1 thiếu `chartFacts` → spec không hợp lệ.
- `learning-content-internal-v1.md`: câu essay trả thêm `assets[]` gồm
  `{assetId, assetType: "IMAGE", mediaUrl, altText, sortOrder}`, lấy từ `content_asset_links` theo `question_version_id`.
  `mediaUrl` do content resolve từ `media_reference` (IMAGE: giữ nguyên giá trị đã kiểm).
  `altText` lấy từ `content_assets.text_content` của asset IMAGE.
- `lesson-learning-v1.md`: khối essay của học viên có thêm `images[{mediaUrl, altText}]`, theo `sortOrder`. Ghi rõ:
  frontend chỉ render ảnh qua `<img src>` (không nhúng SVG inline), vì `mediaUrl` có thể là data URI SVG.
  **Không** có `chartFacts`.
- `lesson-writing-v1.md`: `criteria[].code` là `TA, CC, LR, GRA` khi `task = TASK_1`, và `TR, CC, LR, GRA` khi
  `task = TASK_2`. Response nộp bài có thêm `task`.

**Content:**
- `LessonBlockKind.classify` (của đợt 1) thêm luật cho `TASK_1`:
  - có `chartFacts` hợp lệ;
  - có ≥ 1 asset `IMAGE` gắn vào version câu;
  - thiếu một trong hai → `InvalidLessonBlockException`.
  - Hàm nhận thêm danh sách loại asset của câu, đã load cùng lúc với câu.
- Use case đọc bài nội bộ load asset của **mọi** câu trong bài bằng **một** query
  (`content_asset_links JOIN content_assets WHERE question_version_id IN (…)`), ghép bằng `Map`. Không N+1.
  - Thêm hàm theo tập: `ContentAssetRepository.findByQuestionVersionIds(Collection<UUID>)` trả map version → danh sách asset
    theo `sort_order`, cài bằng **một** query JPQL/native ở `ContentAssetLinkJpaRepository`. Không gọi `findById` trong vòng
    lặp như `ContentAssetRepositoryAdapter.findByQuestionVersionId` hiện tại (`:61-66`); không sửa hàm cũ.
- **Kiểm media dùng chung** (quy ước lộ trình): lớp domain thuần `MediaReferencePolicy` (không Spring), dựng với base URL
  của media (plan này truyền chuỗi rỗng; plan Listening nối vào `CONTENT_MEDIA_BASE_URL`), method
  `resolve(assetType, mediaReference)` trả `mediaUrl` hoặc ném `InvalidMediaReferenceException`, map `INVALID_MEDIA_REFERENCE` (500, lỗi dữ liệu seed) qua
  `GlobalExceptionHandler`. Plan này chỉ cài IMAGE: nhận `https://…` hoặc `data:image/(png|jpeg|svg+xml);base64,…`, trả
  nguyên giá trị (chặn `javascript:`, `file:` hay đường dẫn nội bộ). Loại khác ném lỗi; 0851 thêm AUDIO.
- Seed `V11__seed_writing_task1_demo.sql` (không sửa V1–V10), UUID prefix `22000000-…`. **Nội dung (đề, `chartFacts`, bảng số
  của biểu đồ, alt text, bài mẫu, khối TEXT) lấy đúng mục "Khối essay `L3-W1`" và bài L3 trong
  [`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md).** Tóm tắt:
  - KP `DEMO_READING_W1_CHART` (KP6) thuộc `DEMO_READING`: `kind = STRATEGY`, `PROCEDURE`, `WRITING`, `ACTIVE`;
  - câu `ESSAY`, `WRITING`, `PUBLISHED`:
    - `stem` là đề mô tả biểu đồ cột nhiệt độ mái nhà ở 3 thành phố (theo `seed-content.md`);
    - `answer_spec = {"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":6.0,"chartFacts":"…"}`, trong đó
      `chartFacts` liệt kê số liệu chính, xu hướng và điểm so sánh;
    - `explanation` là bài mẫu khoảng 170 từ tự viết;
  - `content_assets` IMAGE:
    - `media_reference` là data URI SVG biểu đồ cột tự vẽ (≤ 8 KB), số liệu khớp `chartFacts`;
    - `text_content` là alt text ngắn, không liệt kê số liệu;
  - `content_asset_links (question_version_id)` → asset;
  - **bài L3** của `DEMO_READING` (seed V9) thêm ở cuối một khối `TEXT` (nội dung trong `seed-content.md`) rồi một khối
    `EXERCISE` chứa câu essay. Kèm `lesson_block_questions` và `lesson_knowledge_points` (L3 → KP6).

## Architecture

- Không thêm cột, enum, bảng.
- Dữ liệu ảnh do content sở hữu; ai-learning chỉ chuyển `mediaUrl` và `altText` cho học viên.

## Related Code Files

- Modify: `docs/contracts/answer-spec-v1.md`, `answer-spec-v1-vectors.json`, `learning-content-internal-v1.md`,
  `lesson-learning-v1.md`, `lesson-writing-v1.md`
- Modify (content): `domain/vo/LessonBlockKind.java`, use case đọc bài nội bộ và `api/dto/internal/*`,
  `domain/repository/ContentAssetRepository.java` + adapter + `ContentAssetLinkJpaRepository` (hàm theo tập),
  `api/exception/GlobalExceptionHandler.java` (`INVALID_MEDIA_REFERENCE`)
- Create (content): `domain/vo/MediaReferencePolicy.java` (hoặc domain service tương đương),
  `domain/exception/InvalidMediaReferenceException.java`, test `MediaReferencePolicyTest`
- Create (content): `src/main/resources/db/migration/V11__seed_writing_task1_demo.sql`
- Tests (content): `LessonBlockKindTest`, test use case đọc bài, test seed quét mọi khối, test controller nội bộ

## Implementation Steps

**Tests Before:**
1. `mvn -q -pl services/content-service -am test` và lưu baseline. Snapshot payload bài L4 (Task 2 của đợt 1) và một
   bài không có essay: phải giữ nguyên, chỉ được thêm `assets: []` vào câu essay.

**Tests After** (viết trước code):
2. `LessonBlockKindTest`:
   - TASK_1 đủ `chartFacts` và ảnh → `ESSAY`;
   - thiếu `chartFacts`, `chartFacts` > 2.000 ký tự, hoặc không có ảnh → exception;
   - TASK_2 không cần ảnh (không hồi quy).
3. Use case đọc bài:
   - L3 có khối essay với 1 asset IMAGE, đúng `sortOrder`;
   - asset của mọi câu load bằng một query (đếm query hoặc review SQL);
   - `media_reference` dạng `javascript:` hay `file:` → `INVALID_MEDIA_REFERENCE`;
   - `MediaReferencePolicyTest`: `https://…` và data URI png/jpeg/svg → trả nguyên; scheme khác, data URI không phải ảnh → lỗi.
4. Seed: V11 chạy sạch sau V10; quét mọi khối `EXERCISE` không lỗi; SVG decode được và ≤ 8 KB.
5. API content công khai: CUSTOMER đọc câu Task 1 → 403 (luật 1640 giữ).

**Implement:** contract → `classify` → query asset theo tập + DTO → V11.

**Regression Gate:**
```powershell
mvn -q -pl services/content-service -am test
```

## Success Criteria

- [ ] Payload nội bộ có `assets[]` cho câu essay; payload cũ không đổi gì khác.
- [ ] Không N+1 khi load asset.
- [ ] V11 chạy sạch; khối Task 1 ở L3 hợp lệ.

## Risk Assessment

- **Data URI lớn làm nặng payload bài học:** giới hạn 8 KB trong seed. Nội dung thật dùng URL khi có chỗ lưu file.
- **Ảnh và `chartFacts` lệch nhau:** cả hai cùng sinh từ một bảng số trong seed; test kiểm các số trong `chartFacts` có
  mặt trong SVG.
- **Seed làm vỡ kịch bản Lan:** khối essay không tính vào hoàn thành bài (luật đợt 1); chạy lại test kịch bản.

## Security Considerations

- Chặn scheme `media_reference` lạ để client không render nội dung nguy hiểm.
- `chartFacts` chỉ đi qua endpoint nội bộ.
