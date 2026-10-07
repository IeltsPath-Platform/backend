# Phase 4 — Part 4: ERD, Data Constraints, Retention, State Machines

## Context

- 4.1: 5 ERD view (`diagram_script/part4-diagrams/IELTSPath_ERD.drawio`, `IELTSPath_ERD_1..5.png`) + bảng entity.
- 4.2: DC-01..DC-40. 4.3: chính sách cho 14 loại dữ liệu. 4.4.1–4.4.6: state table + `IELTSPath_StateMachines.drawio`.
- Nguồn schema: migration content V19–V23, learning V6–V10, assessment V5–V6 (kiểm số thật), `.sdd/database/DATABASE_V5.md`.

## Sửa

**4.1 Entities**
- Content: entity **Course** (code, name, band_level unique, status). Topic có quan hệ n–1 tới Course (tùy chọn).
  Package có type `COURSE_TEST` gắn Course. Lesson không còn có một skill duy nhất (skill suy ra từ câu hỏi).
- Learning: **Learner placement band** (band mới nhất), **Course test assignment**, **Course progress/pass**. Tên lấy
  theo bảng trong migration. Practice attempt có điểm theo từng skill.
- Assessment: grading job cho essay gate, kèm trạng thái chuyển HUMAN.
- Vẽ lại view ERD liên quan: curriculum và learning progress. Chỉ giữ tiêu đề trong ảnh, không thêm legend.

**4.2 Data constraints**: thêm DC-41.. theo migration thật, ví dụ:
- `band_level` của course là duy nhất, trong 0–9, bước 0.5.
- Mỗi learner có tối đa một course test assignment chưa dùng cho mỗi course.
- Essay trong PRACTICE_SET/TOPIC_TEST/COURSE_TEST phải có `passBand`.
- Quota chấm LLM của assessment theo ngày.
- Sửa các DC đang nói "topic một skill" hoặc "lesson cùng skill".

**4.3 Retention**: thêm dòng cho placement band và course test assignment, theo nhóm "learning records" đã có.

**4.4 State machines**
- 4.4.3 Topic Progress: luật chuyển trạng thái theo course; transition "pass → next topic of the course".
- Thêm **4.4.7 Course Progress**: course test `NONE / LOCKED / AVAILABLE / PASSED` theo `testStatus` của contract.
  Có bảng valid/invalid và hình.
- 4.4.4 Review Item: skill lấy từ KP; danh sách thứ bị chặn đổi theo lesson có skill đó.
- 4.4.6 Assessment Attempt: thêm loại COURSE_GATE; essay gate đi theo nhánh AI hoặc HUMAN; result chỉ final sau khi
  essay được chấm.

## Steps

1. Đọc migration để chốt tên bảng và cột.
2. Sửa drawio ERD và state machine, render PNG. Máy chưa có CLI draw.io, nên cần một cách render, xem Risks.
3. Script python-docx: sửa bảng entity, DC, retention, state table; thay ảnh.
4. Revision history v0.9.18.

## Risks

- Không có draw.io CLI trên máy. Cách xử lý: hỏi user đã render các ảnh trước bằng cách nào (draw.io desktop export
  hoặc skill excalidraw/playwright), rồi dùng lại đúng cách đó.
