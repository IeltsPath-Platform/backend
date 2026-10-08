---
title: "Cập nhật SRS theo luồng course, lesson nhiều skill và chấm essay trong đề thi"
description: "Đưa Report 3.0 SRS (v0.9.15, 2026-10-05) về khớp code sau hai plan course-band-levels và multi-skill-lessons."
status: completed
priority: P2
branch: "doc/srs-course-multi-skill"
tags: [docs, srs, capstone]
blockedBy: []
blocks: []
created: "2026-10-07T12:34:00.000Z"
createdBy: "ck:plan"
source: skill
---

# Cập nhật SRS theo luồng course và lesson nhiều skill

## Overview

SRS `capstone-docs/rp_3.0_system-requirements-system/Report 3.0_SRS_IELTSPath_IeltsPath_v1.docx` dừng ở v0.9.15
(2026-10-05). Sau đó hai plan đã merge và đổi luồng học:

- [261006-2228-course-band-levels](../261006-2228-course-band-levels/plan.md), D1–D12: course theo band, chuỗi topic
  theo course, mọi course mở, placement chỉ gợi ý course, thi cuối course không chặn.
- [261006-2309-multi-skill-lessons](../261006-2309-multi-skill-lessons/plan.md), M1–M15: một lesson có R+L+W, Practice
  đạt theo từng skill, review theo skill của KP, đề thi topic/course trộn skill, essay trong gate chấm LLM ở assessment,
  lỗi thì chuyển EXAMINER.

Nguồn đúng về dữ kiện: code + `docs/contracts/lesson-learning-v1.md`, `assessment-completed-v2.md`, `answer-spec-v1.md`,
`docs/system-architecture.md`. Khi plan và code lệch nhau thì theo code.

## Quy tắc khi sửa

1. Giữ đúng template SRS: mỗi mục chỉ có các phần tử template đã có (Summary, System Behaviour, BR, AC, NAC, BV…), không
   thêm tiểu mục, không thêm ghi chú "known gap". Các phát hiện ngoài template thì báo trong chat.
2. Ảnh diagram chỉ giữ tiêu đề, không có dòng legend.
3. Sửa docx bằng python-docx của skill venv (`~/.claude/skills/.venv/Scripts/python.exe`), viết script trong scratchpad.
   Giữ style và numbering sẵn có, sửa tại chỗ; không dựng lại toàn bộ file.
4. Mỗi phase thêm một dòng revision history (v0.9.16, v0.9.17, …), ngày 07/10/2026 hoặc ngày sửa thật.
5. Không đánh số lại FT-01..FT-54, vì RTW và FDS đang tham chiếu số này. Feature mới thêm vào cuối danh sách.
6. Mọi con số và mã lỗi lấy từ code hoặc contract. Không suy diễn.

## Phases

| # | Phase | Status |
| --- | --- | --- |
| 1 | [Part 0–1: lịch sử, mục tiêu, glossary, bridge, use case](phase-01-part0-1-overview-glossary.md) | done |
| 2 | [Part 2: scenario và diagram](phase-02-part2-scenarios.md) | done |
| 3 | [Part 3: feature description](phase-03-part3-features.md) | done |
| 4 | [Part 4: ERD, data constraint, retention, state machine](phase-04-part4-data.md) | done |
| 5 | [Part 5–6: business rule và NFR](phase-05-part5-6-rules-nfr.md) | done |
| 6 | [Rà nhất quán và bàn giao cho RTW/FDS](phase-06-consistency-handoff.md) | done |

Thứ tự: 3 → 5 → 4 → 1 → 2 → 6. Viết Part 3 trước vì nó chốt tên, mã lỗi và số BR; các phần khác chỉ trỏ tới đó.

## Acceptance criteria

- Không còn câu nào trong SRS nói lộ trình đi "per skill" / "skill-by-skill", "lesson inherits the topic's skill",
  "next topic of the skill", hay "placement [TBC]".
- Course, course test, course gợi ý, lesson nhiều skill, Practice đạt theo skill, essay trong gate (LLM, quota, fallback
  EXAMINER) đều có FT, BR, AC/NAC và entity hoặc state tương ứng.
- Mọi tham chiếu chéo FT↔BR↔SC↔DC khớp nhau; revision history có đủ dòng.
- Docx mở được, ảnh mới được chèn đúng chỗ placeholder cũ.

## Ngoài phạm vi

- Sửa RTW (Report 3.1) và FDS (Report 3.2): phase 6 chỉ lập danh sách việc cần làm.
- Speaking trong lesson (M1 để sau).
- TDS (Report 4).

## Quyết định khi thực hiện (2026-10-07)

- Course test là **FT-55** riêng; course test nằm trong alternative flow của SC-05 (không thêm SC-09).
- State machine và ERD sinh lại bằng script model của phiên trước (tái tạo đúng từng byte file đã commit rồi mới sửa),
  render bằng ImageMagick. Diagram scenario không render lại được (draw.io viewer headless không vẽ), nên sửa nhãn
  trong drawio và vá đúng vùng chữ trên PNG export (tỉ lệ 2×).
- Thêm SC-07 vào phạm vi (topic thuộc course).
- Kết quả: SRS v0.9.16–v0.9.19; bàn giao RTW/FDS ở
  [report](../reports/srs-to-rtw-fds-handoff-261007-1934-course-multi-skill-report.md).

## Câu hỏi chưa chốt

Không có.
