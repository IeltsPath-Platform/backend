---
phase: 7
title: "Câu chọn nhiều đáp án"
status: blocked
priority: P3
dependencies: [1, 3]
blockedBy: ["Q1 trong plan.md"]
---

# Phase 7: Câu chọn nhiều đáp án (Multiple Choice, Many Answers)

**Không làm phase này khi Q1 chưa được chủ dự án duyệt.** Nó đổi `answer-spec-v1`, một contract dùng chung, mà `AGENTS.md` cấm đổi khi chưa duyệt.

## Overview
Đề Reading thật có câu "Choose TWO letters": một câu hiển thị, hai điểm, thứ tự chọn không quan trọng. `answer_spec` hiện chỉ có `CHOICE` (một khóa), `FILL`, `ESSAY`, `SPEAKING`,
và auto-grade cho điểm tuyệt đối hoặc 0 theo từng item. Không thể biểu diễn câu này bằng `CHOICE` mà chấm đúng.

## Requirements (đề xuất, để chủ dự án duyệt)
- Spec mới: `{"type":"MULTI","correct":["A","C"]}`; số đáp án đúng `n = correct.length` (≥ 2). Câu trả lời học viên: `{"answer":["C","A"]}` (mảng khóa option).
- Chấm: `maxScore = n`; điểm = số khóa **khác nhau** học viên chọn mà nằm trong `correct`; chọn nhiều hơn `n` khóa hoặc khóa không có trong option → dữ liệu không hợp lệ, điểm 0
  (cùng tinh thần "dữ liệu sai không được trùng ngầm" của spec hiện tại). `is_correct = (điểm == maxScore)`. `item_results.score <= max_score` đã có CHECK nên điểm từng phần hợp lệ.
- Tổng điểm đề tính theo tổng `maxScore` (một câu MULTI hai điểm đóng góp 2 vào mốc 40), không theo số item.
- Mọi grader dùng chung bộ vector: `AnswerSpecGrader` ở assessment **và** ở learning (hai bản riêng), cùng `docs/contracts/answer-spec-v1.md` và `answer-spec-v1-vectors.json`.
- Content: `PublishContentPackageUseCase.autoGradable` nhận `MULTI` hợp lệ; luật của phase 1 chặn nhóm `MULTIPLE_CHOICE_MANY` được gỡ.
- Assessment: `AutoGradeAttemptService.isCorrect` hiện coi payload `answer` không phải chuỗi là dữ liệu sai; mở cho mảng chuỗi khi spec là `MULTI`;
  kiểm payload khi lưu đáp án.

## Related Code Files
- Modify: `docs/contracts/answer-spec-v1.md`, `docs/contracts/answer-spec-v1-vectors.json`,
  assessment `domain/service/AnswerSpecGrader.java`, `application/usecase/{AutoGradeAttemptService,AnswerSnapshot}.java`,
  learning `domain/service/AnswerSpecGrader.java`, content `application/usecase/PublishContentPackageUseCase.java`
- Tests: hai `AnswerSpecGraderTest` chạy cùng vector mới (mọi ca: đủ, thiếu một, thừa một, lẫn khóa sai, trùng khóa, thứ tự khác, mảng rỗng, kiểu sai)

## Implementation Steps
1. Chủ dự án duyệt Q1 và chốt công thức chấm trên (ghi vào bảng quyết định của `plan.md`).
2. **Test đỏ** trên vector chung ở cả hai service, rồi cài hai grader theo cùng vector.
3. Content publish và assessment auto-grade; tài liệu contract.
4. `mvn -q -pl services/content-service,services/assessment-service,services/learning-service -am test`.

## Success Criteria
- [ ] Hai grader cho cùng kết quả với mọi vector (bao gồm các vector cũ giữ nguyên).
- [ ] Câu "choose two" đúng một đáp án được 1/2 điểm, đủ hai được 2/2.
- [ ] Đề không dùng `MULTI` không đổi hành vi.

## Risk Assessment
- Contract dùng chung: consumer nào không biết `MULTI` sẽ coi là không chấm được. Phải deploy grader mới ở assessment và learning **trước** khi publish đề có `MULTI`.
- Cách chia điểm là quyết định sản phẩm (có người muốn trừ điểm khi chọn thừa); chủ dự án chốt ở bước 1, implementer không tự chọn.
- Nếu không duyệt: đề luyện không có dạng này (lệch đề thật) hoặc phải tách mỗi đáp án thành một câu `CHOICE` riêng, không chấm đúng quy tắc "thứ tự bất kỳ".
