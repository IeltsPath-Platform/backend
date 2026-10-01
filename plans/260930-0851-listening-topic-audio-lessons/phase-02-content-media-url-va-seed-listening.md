---
phase: 2
title: "Content: media URL và seed Listening"
status: completed
priority: P1
dependencies: [1]
effort: "2 ngày"
---

# Phase 2: Content: media URL và seed Listening

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java (plan `261001-1228`). Mọi tên file, lệnh và API Python dưới đây đọc theo [bảng ánh xạ](../260930-2057-mvp-reading-writing-listening-roadmap/python-to-java-mapping.md); luật nghiệp vụ, mã lỗi và test case giữ nguyên.

## Kết quả (2026-10-01, nhánh `feat/writing-access-content`)

- `MediaReferencePolicy` thành bean nhận `content.media.base-url` (env `CONTENT_MEDIA_BASE_URL`, phải là https), thêm nhánh AUDIO (URL https hoặc key ghép base; chặn `..`, `/`, scheme lạ; base rỗng + key → lỗi). V12 seed đúng `seed-content.md`: 8 audio, LS1/LS2, 4 gói × 3 câu, X3/X4 × 4 câu (27 câu). README content có bảng 8 key mp3. Content 138 test pass, 0 skip.
- Lệch plan (đều là thêm field, không đổi field cũ, để không làm vỡ client của Codex):
  - khối `ASSET` giữ `{id, assetType, textContent, mediaReference, durationSeconds}` và thêm `mediaUrl`; với AUDIO thì `textContent` là transcript (không thêm khóa `transcript`).
  - section của package version thêm `audio {assetId, mediaUrl, durationSeconds, transcript}` thay vì `assets[]`; audio lấy bằng `LEFT JOIN LATERAL` trong cùng query section, không thêm hàm repository.
  - bean dùng `@Value`, không tạo lớp `ContentMediaProperties`.

## Context Links

- Plan 1640 phase 3 (V8 bảng bài học, V9 seed Reading, `InternalLearningContentController`, luật gói `PRACTICE_SET`)
- `services/content-service/src/main/resources/db/migration/V1__create_content_tables.sql` (`content_assets` có `AUDIO`,
  `media_reference`, `duration_seconds`; `content_asset_links` theo section)
- `infra/config-server/config-repo/content-service.yaml`

## Overview

Content ghép URL audio từ key, trả audio và transcript trong payload nội bộ, rồi seed trọn một topic Listening. Không đổi schema.

## Requirements

**Ghép URL** (mở rộng `MediaReferencePolicy` của 0812, không tạo resolver thứ hai):
- Setting `content.media.base-url` (env `CONTENT_MEDIA_BASE_URL`, ví dụ `https://<bucket-host>/ieltspath/`) ở config-repo
  content, kèm giá trị mặc định rỗng; `ContentMediaProperties` đưa giá trị này vào `MediaReferencePolicy` (0812 đang dựng
  với chuỗi rỗng).
- Thêm nhánh AUDIO cho `MediaReferencePolicy.resolve(assetType, mediaReference)`:
  - `https://…` → giữ nguyên;
  - key (không scheme, không `..`, không bắt đầu bằng `/`) → `baseUrl + key` (đúng một dấu `/`);
  - còn lại (scheme lạ, `javascript:`, `file:`, `data:`, đường dẫn tuyệt đối) → `INVALID_MEDIA_REFERENCE`;
  - base rỗng mà gặp key → cũng lỗi, không trả URL hỏng.
  - Nhánh IMAGE của 0812 giữ nguyên.

**Payload nội bộ** (endpoint của 1640):
- `GET /lessons/{id}`: khối `ASSET` loại AUDIO trả `{assetId, assetType, mediaUrl, durationSeconds, transcript}`.
- `GET /package-versions/{id}`: mỗi section trả `assets[]` (PASSAGE như cũ, thêm AUDIO) và `skill`. Load asset theo tập
  section bằng một query: thêm `ContentAssetRepository.findBySectionIds(Collection<UUID>)` (cùng kiểu hàm theo tập của 0812);
  không dùng `findBySectionId` hiện tại vì nó gọi `findById` trong vòng lặp.
- `GET /api/content/reading/sections/{id}` giữ nguyên (chỉ section READING).

**Seed** `V12__seed_listening_demo.sql` (không sửa migration cũ; UUID prefix `23000000-…`). **Nội dung (transcript, câu,
`answer_spec` với các biến thể được chấp nhận, khối bài học, gói, mã đề) lấy đúng các mục Listening trong
[`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md).** Tóm tắt:
- Topic `DEMO_LISTENING`: `sort_order` 920 (sau `DEMO_READING` 900 và `TFNG_SKILLS` 910, nên không đổi thứ tự mà test của
  1640 đang kiểm), `ACTIVE`.
- KP (cùng topic, `skill = LISTENING`, `ACTIVE`, `kind = STRATEGY`, **tất cả `PROCEDURE`**, vì `CONCEPT`/`DESIGN` chỉ được coi
  là đã nắm khi tutor chấm giải thích, `app/mastery/policy.py:42-47`): `LS_NUM` (KP8), `LS_SPELL` (KP9), `LS_PARA` (KP10),
  `LS_TRAP` (KP11).
- Bài `LS1` (LQ1–LQ4, dạy KP8 + KP9) và `LS2` (LQ5–LQ7, dạy KP10 + KP11): khối TEXT → khối ASSET AUDIO → khối EXERCISE.
  Mỗi câu gắn **1 KP chính**.
- **1 gói `PRACTICE_SET` mỗi KP** (chốt 2026-10-01): `PS-NUM`, `PS-SPELL`, `PS-PARA`, `PS-TRAP`; mỗi gói 1 section LISTENING +
  1 AUDIO + 3 câu cùng KP, `difficulty = NULL`, không trùng câu bài học hay đề.
- `TOPIC_TEST` × 2 mã (`X3` audio `hotel`, `X4` audio `tour`; `topic_id = DEMO_LISTENING`): mỗi mã 1 section LISTENING +
  1 AUDIO + 4 câu, mỗi KP một câu.
- Asset AUDIO: `media_reference` = key `listening/demo/<ref>.mp3`; `duration_seconds` theo `seed-content.md` (sửa theo độ
  dài file thật khi có); `text_content` = transcript đủ lời, có tên người nói. Tổng **8 file**: 2 bài + 4 gói + 2 mã đề.

**File mp3:** tạo ngoài repo (thu âm hoặc TTS một lần), upload lên bucket đúng key. `README` content ghi danh sách key và
thời lượng. Test không cần file thật.

## Architecture

- `MediaReferencePolicy` là lớp domain thuần (0812); base URL đi vào qua constructor từ `ContentMediaProperties`.
- Payload nội bộ là nơi **duy nhất** ghép URL. ai-learning và assessment nhận URL đầy đủ, không biết base.

## Related Code Files

- Create (content): `config/ContentMediaProperties.java`, `src/main/resources/db/migration/V12__seed_listening_demo.sql`
- Modify (content): `MediaReferencePolicy` (nhánh AUDIO) và nơi dựng nó (truyền base URL), repository asset (hàm theo tập section)
- Modify (content): use case và DTO nội bộ lấy bài và package version (của 1640), query asset theo tập section
- Modify: `infra/config-server/config-repo/content-service.yaml` (một key mới), `services/content-service/README.md`
- Tests: `MediaReferencePolicyTest` (thêm ca AUDIO), test use case lấy bài và package version, test seed (Testcontainers)

## Implementation Steps

**Tests Before:**
1. `mvn -q -pl services/content-service -am test` và lưu baseline. Snapshot payload một bài Reading và một gói Reading:
   chỉ được thêm field, không đổi field cũ.

**Tests After** (viết trước code):
2. `MediaReferencePolicyTest` (thêm ca AUDIO, ca IMAGE của 0812 giữ nguyên):
   - `https` giữ nguyên; key ghép đúng một dấu `/`;
   - `../x.mp3`, `/x.mp3`, `javascript:…`, `file:…`, `data:audio/…` → lỗi;
   - base rỗng + key → lỗi.
3. Use case:
   - bài `LS1` có khối AUDIO với `mediaUrl` đầy đủ, `durationSeconds`, `transcript`;
   - package version Listening trả `assets` của section; asset load bằng một query.
4. Seed:
   - migration chạy sạch;
   - topic có ≥ 1 bài và ≥ 1 mã đề nên vào thứ tự học;
   - mỗi KP Listening có đúng 1 gói, mỗi gói 3 câu cùng KP, `difficulty` NULL, không trùng câu bài học hay đề;
   - mọi KP Listening là `PROCEDURE`, `kind = STRATEGY`; `topic-sequence` trả `hasPracticeSet = true` cho cả 4 KP;
   - câu, đáp án, KP khớp `seed-content.md`;
   - mọi câu có spec chấm được;
   - mọi asset AUDIO có key hợp lệ và `text_content` khác rỗng.

**Implement:** properties + resolver → payload nội bộ → seed.

**Regression Gate:**
```powershell
mvn -q -pl services/content-service -am test
```

## Success Criteria

- [ ] Không migration schema; một migration seed mới.
- [ ] URL audio chỉ ghép ở content; đổi base URL không cần sửa dữ liệu.
- [ ] Seed đủ để chạy: học 2 bài, sai một câu lần đầu để nhận luyện thêm, làm đề 2 mã.

## Risk Assessment

- **File mp3 chưa upload khi chạy demo:** API vẫn chạy nhưng không phát được; README ghi rõ danh sách key cần upload.
- **Tên riêng dễ bị chấm sai vì chính tả:** transcript đánh vần rõ ("T-H-O-M-P-S-O-N"); đề nêu giới hạn từ.
- **Test thứ tự học của 1640 kiểm cả danh sách:** Listening đứng cuối nên chỉ nối thêm phần tử; nếu test so khớp nguyên danh
  sách thì cập nhật test đó trong PR này.

## Security Considerations

- Key bị chặn path traversal và scheme lạ trước khi ghép.
- Transcript chỉ nằm trong payload nội bộ.
