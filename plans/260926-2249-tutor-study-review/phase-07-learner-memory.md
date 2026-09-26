---
phase: 7
title: "Tutor nhớ học viên qua các buổi học"
status: pending
priority: P3
dependencies: [4]
effort: "~1.5d"
---

# Phase 7: Tutor nhớ học viên qua các buổi học

## Overview
Tutor nhớ những điều bền vững về học viên (điểm mạnh, điểm yếu, lỗi hay gặp, cách học hợp) giữa các buổi học. Ý tưởng
từ memory nhiều tầng của DeepTutor, tự viết ở dạng tối giản: một bản ghi nhớ ngắn theo học viên, lưu PostgreSQL.

## Đọc trước
- Tham khảo, chỉ đọc: `third_party/deeptutor/deeptutor/services/memory/{store,consolidator,recall}.py`.
- Pha 2: context của tutor; pha 5: hồ sơ học viên (khác memory: hồ sơ do học viên khai, memory do tutor rút ra).

## Requirements
- Migration `learner_memory(user_id PK, content text ≤ 2.000 ký tự, version, updated_at)`.
- Sau khi một buổi học có lượt mới, chạy tóm tắt: LLM nhận memory cũ và các message mới, trả memory mới (giới hạn độ dài).
  Chạy sau khi lượt kết thúc, không làm chậm SSE; lỗi LLM thì giữ memory cũ.
- Memory được đưa vào context tutor ở mọi buổi học.
- `GET /api/ai-learning/tutor/memory` xem, `DELETE` xóa (quyền được quên).
- Memory không chứa key, token, email hay tên thật; nội dung memory không vào log.

## Tests
1. Buổi 1 học viên nói điểm yếu → sau tóm tắt có memory; buổi 2 (session mới) → LLM giả thấy memory trong prompt.
2. Học viên B không thấy memory của A.
3. Xóa memory → buổi sau prompt không còn memory.
4. LLM lỗi khi tóm tắt → memory cũ giữ nguyên, lượt học không bị ảnh hưởng.

## Success Criteria
- [ ] Tutor nhớ học viên qua các buổi; memory nằm trong PostgreSQL, xem và xóa được.
