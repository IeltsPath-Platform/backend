# Phase 6 — Rà nhất quán và bàn giao cho RTW/FDS

## Rà SRS

1. Xuất text docx và grep cụm cũ: `per skill`, `skill-by-skill`, `of the skill`, `of the same skill`,
   `inherits the topic's skill`, `each skill has its own`, `[TBC: uses it`. Mỗi kết quả còn sót phải có lý do giữ lại,
   ví dụ review chặn "same skill" là đúng.
2. Kiểm tham chiếu chéo: FT ↔ BR (Part 5), FT ↔ SC (Scenario List), DC ↔ FT/state, Feature List ↔ Part 1.5 bridge.
3. Mở docx (Word hoặc LibreOffice), kiểm ảnh, mục lục, numbering.
4. Revision history đủ dòng, đúng ngày.

## Bàn giao (không sửa trong plan này)

Ghi report `plans/reports/srs-to-rtw-fds-handoff-<date>-course-multi-skill-report.md`, liệt kê:
- RTW (Report 3.1): dòng FT-55, đổi tên FT-20, UC mới, BR-35..38, DC mới, các mapping SC.
- FDS (Report 3.2, `fds_content.py` / `fds_screens_*.py`): màn hình learning path theo course, danh sách course có
  gợi ý, course test, Practice lọc theo skill, essay trong Practice.

## Commit

Một nhánh `doc/srs-course-multi-skill`, commit `docs(srs): ...`, không nhắc mã plan.
