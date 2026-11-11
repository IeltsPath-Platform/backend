---
phase: 8
title: "Task 2 requirement checks"
status: pending
priority: P1
dependencies: [4]
---

# Phase 8: Kiểm yêu cầu đề Task 2 (learning, template dùng chung)

## Overview
Phần đối xứng với phase 5 cho Task 2. Lỗi làm tụt TR nặng nhất ở Task 2 là lỗi cả bài: trả lời thiếu phần đề, không có
hoặc đổi quan điểm, ý không phát triển, thiếu kết bài. Các lỗi này không nằm ở một câu để trích nên prompt chỉ sửa từng
đoạn sẽ bỏ sót. Phase này bắt model tách đề, đối chiếu từng yêu cầu và lập dàn ý bài (quyết định 20). Chạy song song với
phase 5, sau phase 4.

## Requirements
- **Prompt** (đoạn chỉ nối khi `TASK_2`, thuộc template v2 nếu kịp trước lần kích hoạt đầu; nếu không thì `v3` và đi lại
  quy trình eval), trả `taskChecks`:
  - `questionParts[]` (tối đa 4): `{part, addressed: "FULL"|"PARTIAL"|"NONE", excerpt}`. `part` là một yêu cầu của đề
    viết lại ngắn bằng tiếng Việt (ví dụ "Bàn quan điểm ủng hộ", "Nêu ý kiến của bạn"); `excerpt` bắt buộc khi `FULL`
    hoặc `PARTIAL`.
  - `position`: `{required, stated, excerpt, consistent, conflictExcerpt}`. `required` khi đề hỏi ý kiến; `conflictExcerpt`
    là đoạn mâu thuẫn với quan điểm đã nêu, chỉ có khi `consistent = false`.
  - `outline[]` (tối đa 8, theo thứ tự đoạn): `{role: "INTRODUCTION"|"BODY"|"CONCLUSION", mainIdea, developed, excerpt}`.
    `mainIdea` tiếng Việt ngắn; `developed` cho biết ý có lý do hoặc ví dụ hỗ trợ; `excerpt` là câu chủ đề.
  - Prompt nhắc: lỗi cả bài nêu ở đây, không cần ép thành correction; correction vẫn dành cho lỗi có đoạn trích.
- **Parser (Learning):**
  - `excerpt`, `conflictExcerpt` không có nguyên văn trong bài → bỏ trường đó; mục `FULL`/`PARTIAL` mất excerpt thì bỏ
    mục. Thiếu hoặc sai định dạng → rỗng, **không** `INVALID_GRADE`.
  - Giới hạn: 4 mục `questionParts`, 8 mục `outline`, mỗi chuỗi ≤ 200 ký tự.
  - Suy ra cờ cho phase 6 và giao diện: `partialAnswer` (có mục `PARTIAL` hoặc `NONE`), `noPosition`
    (`required && !stated`), `inconsistentPosition`, `noConclusion` (không có mục `CONCLUSION`).
  - `taskChecks` **không** ghi evidence trong đợt này: evidence Task 2 vẫn đến từ band TR và từ correction có mã lỗi.
    Cờ chỉ dùng để hiển thị, cho `priorities` dự phòng và cho luật trần của phase 6.
- **Mã lỗi:** khi phase 3 có mã Task 2 đã duyệt (`T2_PARTIAL_ANSWER`, `T2_NO_CONCLUSION`…), các cờ trên được gắn mã đó để
  nhóm lỗi và nút luyện dùng được; chưa duyệt thì dùng `TASK_OTHER`.
- **Response** (phase 7): trả `taskChecks` và các cờ cho bài `GRADED` Task 2; Task 1 không có trường này.
- **Assessment** (quyết định 5): template chung nên prompt có đoạn này; parser của Assessment bỏ qua `taskChecks`.

## Related Code Files
- Learning: `application/service/EssayGrader.java` (hoặc tách `Task2RequirementChecker` thuần trong `application/service/`
  theo mẫu `EssayKnowledgeJudge`, dùng chung cách kiểm trích dẫn với phần Task 1), `domain/vo/WritingGrade.java`,
  `api/dto/response/WritingSubmissionResponse.java`, template v2
- Docs: `docs/contracts/lesson-writing-v1.md`, template trong `docs/contracts/`
- Tests: parse `taskChecks` (hợp lệ, trích dẫn giả, thiếu mảng, quá giới hạn); suy cờ đúng ở các biên (đề không hỏi ý
  kiến thì không có `noPosition`); Task 1 không có `taskChecks`; `taskChecks` không sinh evidence

## Implementation Steps
1. Test đỏ parser và phép suy cờ.
2. Viết đoạn template; examiner duyệt câu chữ (cùng lượt duyệt với phase 4 và 5 nếu kịp).
3. Code, cập nhật contract trong cùng PR.
4. Chạy eval: band TR và CC trên các bài Task 2 của bộ gold không tụt so với v1.
5. **Kiểm tay (quyết định 22):** examiner xem 10 bài Task 2, gồm ít nhất 2 bài cố ý trả lời thiếu phần đề và 1 bài không
   nêu quan điểm; ghi trong `reports/` số bài mà `questionParts`, `position`, `outline` khớp nhận định của examiner.

## Success Criteria
- [ ] Trên 10 bài examiner kiểm: `questionParts` và `position` khớp ≥ 8 bài; mọi bài cố ý thiếu phần đề đều bị gắn cờ.
- [ ] Không bài Task 2 nào bị `INVALID_GRADE` vì `taskChecks`.
- [ ] p95 thời gian chấm Task 2 dưới timeout (đo ở phase 0 với đoạn này trong bản nháp).

## Risk Assessment
- Task 2 dài hơn Task 1 nên là ca chậm nhất; `outline` là phần bỏ được đầu tiên nếu phase 0 cho thấy quá giờ.
- Model tách đề sai (gộp hai yêu cầu, hoặc bịa yêu cầu): examiner kiểm tay ở bước 5; `part` hiển thị cho học viên nên
  phải đúng với đề.
- Cờ `partialAnswer` sai sẽ hạ band oan khi phase 6 bật luật tương ứng: luật đó chỉ bật sau khi bước 5 đạt.
