---
title: Tái cấu trúc tài liệu ngữ cảnh cho agent
description: >-
  CLAUDE.md và AGENTS.md lỗi thời 79 commit và không nhắc AI Learning; tách
  thành CLAUDE.md gọn + CLAUDE.md riêng cho ai-learning-service + AGENTS.md làm
  nguồn quy tắc + docs/system-architecture.md, và loại third_party khỏi
  graphify.
status: pending
priority: P2
branch: feat/ai-learning-service
tags:
  - docs
  - agent-context
  - graphify
blockedBy: []
blocks: []
created: '2026-09-27T15:10:41.531Z'
createdBy: 'ck:plan'
source: skill
---

# Tái cấu trúc tài liệu ngữ cảnh cho agent

## Overview
Đầu vào: [báo cáo brainstorm](../reports/brainstorm-260927-2201-agent-context-docs-restructure-report.md) (người dùng
duyệt hướng B + loại `third_party/` khỏi graphify).

`CLAUDE.md` (277 dòng, sửa lần cuối `115c36d`, 79 commit trước HEAD `79f9fd6`) ghi "chỉ có User", "5 module", "không có
messaging"; `AGENTS.md` (298 dòng) không được Claude Code nạp và ra lệnh "tech stack bắt buộc" không có Python, "không
có message broker". Cả hai không nhắc `ai-learning`, Python, RabbitMQ, DeepTutor. Plan này **chỉ đổi tài liệu và phạm vi
graph**, không đổi code.

## Kiến trúc tài liệu đích

| File | Nạp khi | Nội dung | Giới hạn |
| --- | --- | --- | --- |
| `CLAUDE.md` (root) | Mọi phiên Claude Code | Map module, chạy local, lệnh, bất biến, cạm bẫy, con trỏ tài liệu, graphify; `@AGENTS.md` | ≤ 150 dòng |
| `services/ai-learning-service/CLAUDE.md` (mới) | Khi Claude làm trong service | Stack Python, package map, bất biến, lệnh test | ≤ 100 dòng |
| `AGENTS.md` | Qua `@AGENTS.md`; Codex/tool khác đọc trực tiếp | Quy tắc bắt buộc, tự đứng được | ~200–230 dòng |
| `docs/system-architecture.md` (mới) | Khi cần | Kiến trúc, flow, security, data ownership, quyết định | ≤ 800 dòng |
| `.graphifyignore` | graphify | Thêm `third_party/` | — |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Fact sheet and graphify scope](./phase-01-fact-sheet-and-graphify-scope.md) | Completed |
| 2 | [System architecture doc](./phase-02-system-architecture-doc.md) | Completed |
| 3 | [AGENTS rules source](./phase-03-agents-rules-source.md) | Pending |
| 4 | [Slim CLAUDE files](./phase-04-slim-claude-files.md) | Pending |

Tuần tự: 1 → 2 → 3 → 4. Phase 1 tạo fact sheet đã kiểm chứng; mọi phase sau chỉ ghi dữ kiện có trong fact sheet.
Phase 3 cần phase 2 (nội dung kiến trúc chuyển sang doc trước khi cắt khỏi AGENTS.md). Phase 4 cần phase 1 và 3.

## Dependencies
- Không chồng phạm vi với plan đang mở: `260926-2249-tutor-study-review`, `260927-1453-ai-learning-operational-hardening`
  (đã quét: không plan nào sửa `CLAUDE.md`, `AGENTS.md`, `.graphifyignore`, `docs/system-architecture.md`).
- Phase 2 của `260927-1453` (E2E) chạy độc lập; nếu nó thêm lệnh/biến mới, cập nhật lại CLAUDE.md của ai-learning sau.

## Quy tắc cho người implement
1. Chỉ ghi dữ kiện có trong fact sheet (phase 1) hoặc đã kiểm bằng lệnh; ghi nguồn khi dữ kiện dễ đổi.
2. **Không ghi giá trị secret** (mật khẩu, JWT secret, API key, URL có credential); chỉ ghi tên biến.
3. Tiếng Việt, identifier/lệnh/biến giữ tiếng Anh (như tài liệu hiện có).
4. Tránh dữ kiện dễ lỗi thời trong CLAUDE.md (đếm số class, liệt kê class); ưu tiên con trỏ tới file nguồn.
5. Di chuyển nội dung, không xóa mất: mục nào rời CLAUDE.md/AGENTS.md phải có chỗ mới.
6. Mỗi phase một commit `docs:`/`chore:` conventional, không nhắc AI, số phase hay mã plan.
7. Gặp dữ kiện mâu thuẫn giữa code và tài liệu: code là đúng; ghi vào mục "điểm chưa nhất quán", không tự sửa code.

## Acceptance criteria
- Mọi dữ kiện trong 4 file khớp HEAD (module, cổng, route Gateway, DB, tên biến env) — đối chiếu fact sheet.
- 0 giá trị secret (scan ở phase 4).
- Root CLAUDE.md ≤ 150 dòng; CLAUDE.md của ai-learning ≤ 100 dòng; AGENTS.md không còn khẳng định Java-only/không broker.
- Mọi lệnh ghi trong tài liệu đã chạy thử thành công (hoặc ghi rõ điều kiện, ví dụ cần Docker).
- Graph sau rebuild: 0 node có `source_file` bắt đầu bằng `third_party/`; `graphify explain "run_turn"` ra node first-party.
- Smoke test: một agent chỉ đọc CLAUDE.md (kèm import) trả lời đúng 6 câu hỏi kiểm (phase 4).

## Ngoài phạm vi
- Viết lại `README.md` root và README từng service (có thể dedupe ở đợt sau).
- Sửa code, CI, frontend; sửa các "điểm chưa nhất quán" phát hiện được (chỉ ghi lại).

## Risk
- **Lỗi thời lại**: CLAUDE.md chỉ giữ dữ kiện ổn định + con trỏ; đầu mỗi file ghi "cập nhật lần cuối: <ngày>, commit <sha>".
- **Context mỗi phiên tăng** (CLAUDE.md + AGENTS.md ≈ 320–380 dòng): chấp nhận để giữ DRY; cắt narrative khỏi AGENTS.md.
- **Tooling graphify tự chèn lại mục `## graphify`** vào CLAUDE.md: giữ nguyên tiêu đề `## graphify` trong CLAUDE.md mới.
- **Graph mất DeepTutor**: khi port code đọc trực tiếp `third_party/deeptutor`; ai-learning không import nó.
- Rollback: `git revert` từng commit; `.graphifyignore` bỏ dòng `third_party/` rồi `graphify update . --force`.
