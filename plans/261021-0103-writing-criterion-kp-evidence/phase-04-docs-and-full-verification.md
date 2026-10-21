---
phase: 4
title: "Tài liệu và kiểm tra toàn bộ"
status: completed
priority: P1
effort: "0.25d"
dependencies: [1, 2, 3]
---

# Phase 4: Tài liệu và kiểm tra toàn bộ

## Bước 1: Build đầy đủ

```powershell
mvn -q -pl services/content-service -am package
mvn -q -pl services/learning-service -am package
mvn -q -pl services/assessment-service -am package   # assessment cũng đọc rubric nội bộ; xác nhận trường mới không làm vỡ
```

Ghi số test đã chạy và số test bị bỏ qua (Testcontainers khi không có Docker) vào báo cáo cuối.

## Bước 2: Tài liệu

1. `AGENTS.md` §3.8, sau gạch đầu dòng "Essay: sửa và nộp lại..." (dòng ~217-218), thêm:
   > - Bằng chứng essay (lesson và Practice): KP của đề nhận `passed`; mỗi tiêu chí rubric (TA/TR, CC, LR, GRA) là một bằng
   >   chứng riêng cho KP tiêu chí của nó (`DEMO_WRITING_CRITERION_*`, ánh xạ `writing_criterion_knowledge_points` ở
   >   Content), đúng khi band tiêu chí >= `passBand`. KP tiêu chí lưu trong JSON bài chấm lúc chấm; bài cũ không có thì ghi
   >   như trước. Event essay vẫn một dòng cho KP của đề, `error_code` là mã lỗi gặp nhiều nhất. KP tiêu chí từ Practice không
   >   được XP mốc; Writing vẫn không sinh bài ôn.
2. `docs/contracts/lesson-writing-v1.md` dòng 113-114: thay câu "Mastery evidence (...)" bằng:
   > - Mastery evidence (`source = lesson_writing`) is recorded on every graded submission until the block is first passed,
   >   and never after: one row per knowledge point of the question with `correct = passed`, and one row per rubric
   >   criterion for that criterion's knowledge point with `correct = criterion band >= passBand`. Criterion knowledge
   >   points are not part of the response. The question event carries the most frequent correction `errorCode`. No
   >   review is inserted.
3. `docs/contracts/learning-content-internal-v1.md`: kiểm lại đoạn đã thêm ở phase 1.
4. `docs/system-architecture.md`: tìm đoạn mô tả chấm essay/bằng chứng Writing. Nếu có thì sửa cho khớp; nếu không có
   thì không thêm.
5. Grep để không còn mô tả cũ:
   `rg -n "correct = passed, one row per knowledge point" docs AGENTS.md` không còn kết quả trái với mô tả mới.

## Bước 3: Ghi chú cho board Figma (việc thủ công, chuyển cho người dùng)

Board "IELTSPath – Cây tri thức – đánh giá":
- hộp "Bài viết (essay)" và "Chẩn đoán lỗi theo Knowledge ID" đổi thành: KP của đề nhận đạt/không đạt; mỗi tiêu chí
  TA/TR, CC, LR, GRA là một bằng chứng riêng cho KP tiêu chí trong topic Writing; trọng số essay lesson 0.5, essay
  Practice 0.6; mã lỗi phổ biến nhất vào event; Writing không sinh bài ôn chặn.

## Bước 4: Rà lại

- Tên trường thống nhất `knowledgePointId`; bảng `writing_criterion_knowledge_points`; migration V69 (hoặc số thực tế).
- Không còn comment/tên test chứa mã plan, phase hay D-số.
- `git diff --stat` chỉ gồm các file đã liệt kê trong phase 1-4.

## Tiêu chí hoàn thành

- [ ] Ba lệnh `package` xanh (hoặc ghi rõ test bị bỏ qua).
- [ ] `AGENTS.md`, `lesson-writing-v1.md`, `learning-content-internal-v1.md` mô tả đúng hành vi mới.
- [ ] Ghi chú Figma đã được gửi cho người dùng trong báo cáo cuối.
