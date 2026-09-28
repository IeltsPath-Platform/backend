---
phase: 1
title: "Thêm mục kiến trúc đích"
status: pending
priority: P2
dependencies: []
---

# Phase 1: Thêm mục kiến trúc đích

## Overview

Thêm §12 vào `docs/system-architecture.md` mô tả cách chia service đích, đánh dấu rõ là chưa triển khai. Không sửa §1–§11.

## Requirements

- Functional: người đọc thấy ngay (a) kiến trúc hiện tại ở §1–§11, (b) kiến trúc đích ở §12, (c) plan nào đang thực hiện.
- Non-functional: tiếng Việt, cùng giọng văn và định dạng (bảng, khối `text`) với tài liệu; không ghi giá trị secret; tổng file ≤ 800 dòng.

## Architecture

Nội dung §12 "Kiến trúc đích (đang triển khai)":

1. **Trạng thái:** "Chưa triển khai. Khi code đã tách xong, mục này được gộp vào §1–§11."
2. **Sơ đồ đích** (khối `text` như §1):
   ```text
   Client --> API Gateway :8080 --(internal JWT)--> user :8085, content :8082, library :8081, assessment :8083,
                  |                                 access :8084, game :8087 (+ws), notification :8088, community :8089
                  +--(URI cố định)--> ai-learning :8000
   /api/content/videos|vocabulary/**, /api/learning-support/{flashcards,decks,notes,video-progress,saved-segments}/** -> library
   /api/learning-support/{activities,streak}/** -> user
   assessment --HTTP--> content, user     game --HTTP--> content (GRAMMAR), library (VOCABULARY)
   library --HTTP--> content (kiểm topic khi ghi video)     ai-learning --HTTP--> content, user
   ```
3. **Bảng service đích** (cùng cột với §2): library (bounded context: catalog video/từ vựng do admin soạn + dữ liệu học cá nhân flashcard/note/tiến độ video/đoạn đã lưu; `library_db` compose 5437; route như trên), content (curriculum, KP, câu hỏi, đề/gói, asset, **bài học**), user (+ nhật ký hoạt động, streak), learning-support (**xóa**).
4. **Bảng dữ liệu theo DB đích:** `content_db` 14 bảng + outbox; `library_db` 11 bảng; `user_db` 10 bảng (liệt kê tên theo mục "Bảng theo DB" trong `plan.md`).
5. **Giao tiếp mới** (bổ sung bảng §3): library → content `GET /api/content/topics/{id}` (`CONTENT_SERVICE_URL`); game → library `/internal/game-content/snapshots` (`LIBRARY_SERVICE_URL`, nhánh `VOCABULARY`).
6. **Hiện tại → đích** (bảng ngắn): bảng nào chuyển từ đâu sang đâu; FK mới trong `library_db` (flashcard→sense, tiến độ/đoạn video→video/segment); path giữ nguyên; quyền ghi catalog chỉ `ADMIN`/`CONTENT_AUTHOR`.
7. **Quyết định chính** (3–5 dòng, dạng bảng như §8): tách cụm không bám KP; gom dữ liệu người học có tham chiếu video/từ vựng để có FK; giải thể learning-support; giữ path bằng Gateway; kèm trade-off (library hai loại quyền; streak vẫn do client khai).

## Related Code Files

- Modify: `docs/system-architecture.md` (thêm §12; sửa dòng "Cập nhật lần cuối" → 2026-09-28, ghi "§12 mô tả kiến trúc đích chưa triển khai").
- Không sửa: §1–§11, `AGENTS.md`, `CLAUDE.md`, `.sdd/**` (cập nhật khi code đổi).

## Implementation Steps

1. Lấy số liệu từ mục "Đặc tả kiến trúc đích" trong `plan.md` (nguồn duy nhất).
2. Viết §12 theo 7 phần trên.
3. Kiểm: tên bảng, cổng, route, biến env khớp đặc tả; không có giá trị secret; `wc -l` ≤ 800.

## Success Criteria

- [ ] §12 tồn tại, có đủ 7 phần, ghi rõ "chưa triển khai" + link plan.
- [ ] `git diff docs/system-architecture.md` chỉ chạm dòng "Cập nhật lần cuối" và §12.
- [ ] Số liệu §12 khớp mục "Đặc tả kiến trúc đích" trong `plan.md`.

## Risk Assessment

| Rủi ro | Giảm thiểu |
| --- | --- |
| Người đọc tưởng §12 là hiện trạng | Tiêu đề và câu đầu ghi "chưa triển khai"; §1–§11 giữ nguyên |
| Cách chia đổi tiếp → §12 lệch | Mỗi lần đổi quyết định thì cập nhật đặc tả trong `plan.md` và §12 cùng lúc |
