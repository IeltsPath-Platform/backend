---
phase: 8
title: "Sinh câu hỏi, Question Notebook và practice review"
status: pending
priority: P2
dependencies: [6]
effort: "~2d"
---

# Phase 8: Sinh câu hỏi, Question Notebook và practice review

## Overview
Bật sinh câu hỏi của DeepTutor (`deep_question` / `ask_questions`) để học viên luyện thêm theo KP, **không RAG**:
nguồn câu hỏi là metadata KP của Content (tên, mô tả, loại, band) cộng câu hỏi mẫu của Content nếu có. Mở Question Notebook
và practice review của DeepTutor để học viên ôn lại câu đã làm hoặc đã sai theo lịch ôn riêng từng câu.

## Đọc trước khi code
- `agents/question/{capability,pipeline,coordinator,request_config,mimic_source,history}.py`: các chế độ sinh; chế độ nào
  không cần knowledge base.
- `capabilities/ask_questions`.
- `services/practice/{scheduler,storage,answers,analytics}.py`, `api/routers/{practice,question_notebook,quiz_judge}.py`.
- Content: `GET /api/content/questions?knowledgePointId=…` (nếu có), `question_knowledge_points`.

## Requirements
- Functional:
  - Sinh câu hỏi cho một KP của path: request mang `knowledge_point_id`. AI Learning cấp **context** là metadata KP và tối
    đa N câu hỏi mẫu từ Content. DeepTutor sinh, chấm (`quiz_judge`), và ghi `notebook_entries`
    (`source = deep_question`, `knowledge_point_id`).
  - Câu hỏi do Tutor sinh **không** vào ngân hàng câu hỏi của Content (V5 §7.0).
  - Kết quả luyện ngoài mastery loop có ghi evidence vào path không → **mặc định KHÔNG** (chỉ notebook và practice
    review), để mastery chỉ đến từ mastery loop và bài thi chính thức. Ghi rõ; đổi thì cần quyết định mới.
  - Practice review: API của DeepTutor (`practice`, `question_notebook`) mở dưới `/api/ai-learning/practice/**`, lọc theo
    học viên. Chấm `again`/`hard`/`good`/`easy` cập nhật `practice_review_state` và event.
- Non-functional: câu hỏi sinh ra không chứa thông tin định danh; giới hạn số câu mỗi lần sinh.

## Implementation Steps
### Tests Before
1. Sinh câu hỏi không có knowledge base → chạy được, LLM giả nhận context là metadata KP; không gọi embedding hay kho tài liệu.
2. Trả lời, chấm → `notebook_entries`; câu sai → `practice_review_state` có `due_at`.
3. Review đúng hạn → state đổi, event idempotent theo `request_id`.
4. Mastery của path **không** đổi vì luyện ngoài loop.
### Refactor
5. Nối capability, context provider từ Content, router practice.
### Tests After
6. Gate đầy đủ.

## Success Criteria
- [ ] Học viên luyện thêm câu hỏi theo KP và ôn lại câu sai theo lịch, hoàn toàn bằng code của DeepTutor.
