---
phase: 9
title: "Memory trên PostgreSQL"
status: pending
priority: P2
dependencies: [6]
effort: "~1.5d"
---

# Phase 9: Memory trên PostgreSQL

## Overview
Bật memory 3 tầng (L1/L2/L3) của DeepTutor để Tutor nhớ học viên qua các buổi học: điểm mạnh, điểm yếu, cách học ưa
thích. DeepTutor lưu memory thành document dạng file dưới `PathService` của từng người dùng. V5 §7.14 yêu cầu adapter lưu
trữ production, không dùng file theo workspace. Adapter lưu document vào `ai_learning_db`.

## Đọc trước khi code
- `services/memory/{store,paths,document,ops,recall,consolidator,snapshot,trace}.py`: `MemoryStore` là facade; đọc và ghi
  document qua `paths` và `atomic_write_text`.
- `api/routers/memory.py`: API xem, sửa memory.
- `.sdd/database/DATABASE_V5.md` §7.14 "Learner Memory".

## Requirements
- Functional:
  - Migration `V7__learner_memory.sql`: `learner_memory_documents(user_id, layer, slot, surface, content, version,
    updated_at)`. Unique theo khóa mà `paths` của DeepTutor dùng để định danh file. Thêm bảng trace nếu `trace.py` cần.
  - Adapter giữ nguyên **API `MemoryStore`** và định dạng document của DeepTutor (`document.parse` / `serialize`). Chỉ thay
    chỗ đọc và ghi file. Nếu `MemoryStore` gọi file ở nhiều nơi, thì thay cả class qua bootstrap (theo danh sách của pha 1).
  - Phạm vi theo current user. Học viên xem được memory của mình qua `GET /api/ai-learning/tutor/memory`, và xóa được
    (quyền được quên).
  - Consolidation (L1 → L2 → L3) chạy như DeepTutor, sau turn, dùng LLM qua catalog.
- Non-functional: memory không chứa key hay token. Nội dung memory không vào log.

## Implementation Steps
### Tests Before
1. Turn 1 học viên nói điểm yếu → sau consolidation có document L2/L3 trong DB; turn 2 ở session mới → recall đưa memory
   vào context (LLM giả thấy nó trong prompt).
2. Học viên B không thấy memory của A.
3. Xóa memory → recall rỗng.
4. Không file memory nào được ghi vào `deeptutor-data/`.
### Refactor
5. Migration, adapter, router.
### Tests After
6. Gate đầy đủ.

## Success Criteria
- [ ] Tutor nhớ học viên qua các session; memory nằm trong PostgreSQL, xem và xóa được.
