---
phase: 6
title: "Luyện câu hỏi theo KP, sổ câu hỏi và ôn câu sai"
status: pending
priority: P2
dependencies: [4]
effort: "~2.5d"
---

# Phase 6: Luyện câu hỏi theo KP, sổ câu hỏi và ôn câu sai

## Overview
Ngoài mastery loop, học viên xin thêm câu hỏi luyện cho một KP. Tutor sinh câu hỏi từ metadata KP của Content, chấm, và
lưu vào **sổ câu hỏi** của học viên. Câu làm sai được lên lịch ôn lại theo từng câu. Ý tưởng từ `deep_question`,
Question Notebook và practice review của DeepTutor, tự viết.

## Đọc trước
- `.sdd/database/DATABASE_V5.md` §7.11 `notebook_entries`, §7.12 `practice_review_state`, §7.13 `practice_review_events`.
- `app/clients/content_service.py` (metadata KP: tên, mô tả, loại, band).
- `app/mastery/grading.py`, `app/mastery/scheduler.py` (chấm và lịch ôn đã port).
- Tham khảo, chỉ đọc: `third_party/deeptutor/deeptutor/services/practice/{scheduler,storage}.py`, `agents/question/`.

## Requirements
- Migration: `notebook_entries` (câu hỏi đã làm: câu hỏi, đáp án của học viên, đúng/sai, KP, session, turn, thời điểm),
  `practice_review_state` (lịch ôn theo từng câu: `due_at`, trạng thái), `practice_review_events` (idempotent theo
  `request_id`). Theo V5, giản lược phần không dùng; ghi khác biệt vào README.
- Tool `practice_questions(knowledge_point_id, count ≤ 5)`: LLM sinh câu hỏi chỉ từ metadata KP (không RAG, không câu hỏi
  mẫu của Content); lưu đáp án ở server như `mastery_quiz`.
- Chấm bằng `grading.py`; mọi câu vào `notebook_entries`; câu sai tạo `practice_review_state` với `due_at`.
- **Luyện thêm không ghi evidence vào path**: mastery chỉ đến từ mastery loop (pha 2) và bài thi chính thức.
- API: `GET /api/ai-learning/practice/notebook` (lọc theo KP, đúng/sai), `GET /api/ai-learning/practice/due`,
  `POST /api/ai-learning/practice/reviews` (`again` | `hard` | `good` | `easy`, có `requestId`).
- Câu hỏi sinh ra không vào ngân hàng câu hỏi của Content.

## Tests
1. Sinh câu hỏi cho KP: LLM giả nhận đúng metadata KP; không gọi embedding hay kho tài liệu.
2. Trả lời, chấm → `notebook_entries`; câu sai → `practice_review_state` có `due_at`.
3. Review đúng hạn → state đổi, event idempotent theo `requestId`.
4. Mastery của path **không** đổi vì luyện thêm.
5. Học viên B không đọc hay review được sổ của A.

## Success Criteria
- [ ] Học viên luyện thêm câu hỏi theo KP và ôn lại câu sai theo lịch.
