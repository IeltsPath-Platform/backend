# Phase 2 — Part 2: Scenarios và diagram

## Context

- SC-01..SC-08; nguồn diagram `diagram_script/part2-diagrams/IELTSPath_Scenarios.drawio` (mỗi scenario một page) và
  các PNG `IELTSPath_SC-0x_*.png`.

## Sửa

| SC | Sửa |
| --- | --- |
| SC-01 (đăng ký, goal, placement) | Sau placement, IELTSPath gợi ý course (ví dụ band 5.0 → course IELTS 5.5). Minh Anh vẫn có thể tự vào course khác. Sửa activity diagram. |
| SC-02 (học lesson) | Tiền điều kiện: "one topic in progress per course". Narrative: lesson có block Reading và Listening (có thể cả Writing). Hoàn thành khi block khách quan đạt, essay không chặn. Sửa activity diagram nếu có node "per skill". |
| SC-03 (practice + review) | Practice chấm theo từng skill. Review khóa các lesson có cùng skill, lesson khác vẫn học được. Sửa "Tuấn opens the Reading path" thành mở course. |
| SC-04 (Writing AI) | Thêm nhánh: essay trong Practice dùng cùng luồng chấm và trừ điểm. Sequence diagram giữ nguyên nếu luồng chính không đổi. |
| SC-05 (thi topic) | Đề trộn R/L/W. Essay được LLM trong assessment chấm, nhánh phụ chuyển EXAMINER. Đạt thì mở topic kế tiếp **trong course**. Thêm alternative flow: mọi topic PASSED thì có thể thi cuối course, kết quả không chặn gì. Sửa sequence diagram (thêm participant LLM ở assessment và nhánh HUMAN). |
| Scenario List | Thêm FT-55 vào SC-05; cập nhật link FT. |

## Steps

1. Sửa text các SC tại chỗ.
2. Sửa page SC-01, SC-02, SC-03, SC-05 trong `IELTSPath_Scenarios.drawio`, export PNG (theo cách render chốt ở phase 4),
   thay ảnh trong docx. Giữ quy tắc chỉ có tiêu đề, không legend.
3. Revision history.

## Câu hỏi chưa chốt

- Có cần SC-09 riêng cho course test không? Đề xuất là không, đưa vào alternative flow của SC-05 để khỏi phải vẽ thêm
  diagram và thêm dòng RTW.
