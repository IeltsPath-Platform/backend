---
phase: 1
title: "Contract và khóa lộ transcript"
status: in-progress
priority: P1
dependencies: []
effort: "1 ngày"
---

# Phase 1: Contract và khóa lộ transcript

## Tiến độ (2026-10-01)

- Xong: `GET /api/content/assets/{id}` chỉ `ADMIN`/`CONTENT_AUTHOR` (CUSTOMER 403, không token 401; chỉ test gọi path này); contract `learning-content-internal-v1` có audio của bài học và section.
- Còn: phần học viên trong `lesson-learning-v1.md` (audio, transcript sau khi đạt, cấu trúc attempt của assessment). Làm sau khi PR của Codex sửa file này đã merge, để không xung đột.

## Context Links

- Contract của plan 1640: `docs/contracts/learning-content-internal-v1.md`, `lesson-learning-v1.md`
- `services/content-service/src/main/java/com/group01/content/api/controller/ContentAssetController.java:28`
  (`GET /{id}` không có `@PreAuthorize`, trả `textContent`)
- Plan 1640 phase 2 chỉ khóa endpoint **ghi** của asset với CUSTOMER (`phase-02-chan-lo-dap-an.md:21,51`)

## Overview

Ghi contract cho audio và transcript, rồi khóa đường đọc asset trước khi seed transcript thật. Hiện học viên nào đã đăng
nhập cũng đọc được `text_content` của mọi asset theo id; khi có Listening, id asset lại nằm sẵn trong payload bài học.

## Requirements

**Contract:**
- `learning-content-internal-v1.md`:
  - asset AUDIO trả `{assetId, assetType: "AUDIO", mediaUrl, durationSeconds, transcript}`, trong đó `mediaUrl` là URL đầy
    đủ do content ghép;
  - áp dụng cho khối `ASSET` của bài học và cho asset gắn vào section của package version (gói luyện, đề cuối);
  - `transcript` chỉ có ở endpoint nội bộ.
- `lesson-learning-v1.md`:
  - khối `ASSET` loại AUDIO của học viên: `{assetType, mediaUrl, durationSeconds}`, thêm `transcript` **chỉ khi bài đã xong**;
  - gói ôn (`GET /reviews/{id}`) trả audio của section, không có transcript;
  - response nộp gói đạt ≥ 70% có `solutions.transcript`;
  - danh sách trường cho phép liệt kê rõ `transcript` là trường bị giấu.
- Assessment (ghi trong `lesson-learning-v1.md`, mục assessment):
  - cấu trúc attempt trả section theo allowlist `{title, skill, instructions, passage?, audio?: {url, durationSeconds}}`
    (thay chuỗi `snapshot` hiện tại: đổi response schema, ghi rõ trong contract);
  - kết quả học viên có `solutions.sections[].transcript` khi ≥ 70%.

**Khóa lộ:**
- `GET /api/content/assets/{id}`: chỉ `ADMIN`, `CONTENT_AUTHOR` (đúng kiểu `@PreAuthorize` mà 1640 phase 2 dùng cho câu hỏi và
  package). Học viên nhận audio qua ai-learning và assessment, không gọi thẳng endpoint này.
- Kiểm các bên gọi hiện có: grep repo (service và test) cho `/api/content/assets`, liệt kê trong PR.

## Related Code Files

- Modify: `docs/contracts/learning-content-internal-v1.md`, `docs/contracts/lesson-learning-v1.md`
- Modify (content): `api/controller/ContentAssetController.java`
- Tests (content): test controller asset (`@WebMvcTest`, tạo nếu chưa có)

## Implementation Steps

**Tests Before:**
1. `mvn -q -pl services/content-service -am test` và lưu baseline. Thêm test ghi lại hành vi hiện tại: ADMIN đọc asset → 200.

**Tests After** (viết trước code):
2. CUSTOMER `GET /api/content/assets/{id}` → 403; CONTENT_AUTHOR → 200; không token → 401.

**Implement:** `@PreAuthorize` cho `GET /{id}` → contract.

**Regression Gate:**
```powershell
mvn -q -pl services/content-service -am test
```

## Success Criteria

- [ ] CUSTOMER không đọc được asset trực tiếp.
- [ ] Contract nêu rõ trường nào bị giấu và khi nào hiện.

## Risk Assessment

- **Có client dùng `GET /api/content/assets/{id}` cho học viên:** repo chưa có frontend; grep bên gọi trong service.
  Nếu có, chuyển sang payload bài học.

## Security Considerations

- Transcript coi như đáp án: chỉ đi qua endpoint nội bộ, học viên chỉ thấy khi đã đạt.
- URL audio công khai (bucket public-read) không lộ đáp án. Tránh nghe trước đề cuối bằng signed URL để sau.
