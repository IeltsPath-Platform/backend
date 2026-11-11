---
phase: 7
title: "Result view response"
status: pending
priority: P2
dependencies: [2, 4, 5, 8]
---

# Phase 7: Response cho giao diện kết quả (learning, frontend)

## Overview
Biến bài chấm thành thứ học viên hành động được: sai dạng gì nhiều nhất, vì sao, cần gì để lên band kế, luyện ở đâu.
Backend trả dữ liệu; frontend ở repo khác, bàn giao bằng contract. Phần lớn mục không cần LLM.

## Requirements
Backend (`WritingSubmissionResponse`, mọi trường là cộng thêm, bài cũ vắng trường):

| Trường | Nội dung | Nguồn |
| --- | --- | --- |
| `corrections[].start`, `end` | Vị trí ký tự của `excerpt` trong `essayText` | Tính lúc chấm: lần xuất hiện đầu tiên chưa bị correction trước đó chiếm; lưu trong JSON |
| `corrections[].reason`, `certainty` | Vì sao sai; lỗi chắc chắn hay gợi ý diễn đạt | Phase 4 |
| `corrections[].errorCode`, `errorName`, `fixHint`, `severity`, `practiceKnowledgePointId` | Dạng lỗi, quy tắc, mức nghiêm trọng, chỗ luyện | Phase 2 |
| `errorGroups[]` | `{errorCode, errorName, category, count, severity, fixHint, example: {wrong, right}, practiceKnowledgePointId}`, sắp theo `count × severity` giảm; chỉ đếm `ERROR` | Tính khi đọc từ `corrections` và `errorExamples`; mã dự phòng xếp cuối |
| `priorities[]` | Top 3 vấn đề: `{title, explanation, excerpts, action, errorCodes, practiceKnowledgePointId, source}` | Phase 4 (`source = AI`). Thiếu thì lấy 3 `errorGroups` đầu, `title = errorName`, `action = fixHint`, `excerpts` từ correction đầu tiên của nhóm (`source = RULE`); tính lúc chấm, lưu trong JSON |
| `criteria[].evidence`, `descriptor` | Dẫn chứng cho band | Phase 4 |
| `criteria[].nextBand` | `{band, descriptor}` của mức kế tiếp có trong rubric | Rubric đã tải lúc chấm; lưu trong JSON |
| `dataChecks`, cờ Task 1 | Bảng đối chiếu số liệu, danh sách kiểm | Phase 5 |
| `taskChecks`, cờ Task 2 | Yêu cầu đề đã/chưa trả lời, quan điểm, dàn ý từng đoạn | Phase 8 |
| `bandLimits`, `criteria[].limited` | Lý do band bị giới hạn, cùng hình dạng với Speaking | Phase 6 (có thì trả) |
| `revision` | `{previousSubmissionId, fixedCodes[], repeatedCodes[], newCodes[]}` | So `errorCode` với bản trước trong chuỗi revision (`essay-revision-v1`); chỉ cho bài nộp lại |

- "Điểm làm tốt": dùng `knowledgePointJudgments` có `PASS` đã có sẵn; không thêm trường.
- "Bài của bạn sau khi sửa": frontend ghép từ `essayText` và `corrections` theo `start`/`end`. Hai correction chồng nhau:
  áp cái bắt đầu trước, cái còn lại chỉ hiện trong danh sách. Backend không sinh bài viết lại (không thêm lượt LLM).
- Không gọi Content hay LLM khi đọc bài chấm; mọi thứ lấy từ JSON đã lưu và chuỗi revision trong DB của Learning.
- `revision` đọc bản trước bằng một query theo id, không lặp.

Frontend (bàn giao, không code ở repo này), đặt dưới 4 thẻ chấm điểm, theo thứ tự ưu tiên:
1. Khối "3 vấn đề cần tập trung trước tiên" từ `priorities`: tiêu đề, giải thích, ví dụ trích từ bài, "Cần làm" và nút
   "Luyện lỗi này" (mở bộ luyện đã duyệt theo `practiceKnowledgePointId`; không có câu luyện do AI tạo). Phía trên là
   nhận xét tổng quan (`summary` đã có).
2. Bấm vào đoạn highlight hoặc mục trong danh sách: mở thẻ gồm nhóm lỗi, câu gốc, câu được sửa, "Tại sao sai?"
   (`reason`), quy tắc (`fixHint`) và ví dụ tương tự (`example` của nhóm); hai chiều cuộn tới nhau. `SUGGESTION` hiển
   thị khác `ERROR` (nhãn "Gợi ý", màu nhẹ hơn).
3. Mỗi thẻ tiêu chí: band (ghi rõ là ước tính), dẫn chứng, "Để lên <band kế>: …".
4. Task 1: bảng "Bạn viết / Bảng cho thấy" và danh sách kiểm (overview, ý kiến cá nhân). Task 2: danh sách kiểm "Đề
   yêu cầu / Bài bạn đã làm" (từng `questionParts`, quan điểm, kết bài) và "Dàn ý bài của bạn" (mỗi đoạn: vai trò, ý
   chính, đã phát triển chưa; bấm để cuộn tới đoạn đó).
5. "Bài của bạn sau khi sửa": hiển thị **song song** bài gốc và bài đã sửa, lọc theo nhóm (`category`: chính xác, bố
   cục, từ vựng); đặt cạnh "Xem bài mẫu".
6. Bài nộp lại: "Đã sửa X dạng lỗi, còn lặp lại: …".
Màn hình đầu chỉ có band, nhận xét tổng quan, khối 1 và nút luyện; phần còn lại thu gọn. Bộ lọc highlight theo nhóm lỗi.

## Related Code Files
- Learning: `api/dto/response/WritingSubmissionResponse.java`, `application/result/WritingSubmissionResult.java`,
  `application/service/WritingSubmissionViewAssembler.java`, `application/service/EssayGrader.java` (tính `start`/`end`,
  `nextBand`), `domain/vo/WritingGrade.java`, `application/usecase/GetWritingSubmissionUseCase.java`
- Docs: `docs/contracts/lesson-writing-v1.md`, `essay-revision-v1.md`; ghi chú bàn giao frontend trong `reports/` của plan
- Tests: vị trí `start`/`end` khi cụm lặp nhiều lần và khi chồng nhau; `errorGroups` sắp đúng và bỏ `SUGGESTION`;
  `priorities` dùng bản AI khi có, dự phòng `RULE` khi thiếu; `nextBand` ở band cao nhất
  là null; `revision` với bài không có bản trước; JSON bài chấm cũ

## Implementation Steps
1. Chốt JSON mẫu với frontend trước khi code (một ví dụ Task 1, một ví dụ Task 2, một bài nộp lại).
2. Test đỏ → code → cập nhật contract.
3. Bàn giao frontend; frontend làm khối 1 và 2 trước (chỉ cần phase 2).

## Success Criteria
- [ ] Frontend dựng được cả sáu khối chỉ từ một response, không gọi thêm API.
- [ ] Response bài cũ không đổi.
- [ ] Không có lượt gọi LLM hay HTTP nào thêm khi đọc bài chấm.

## Risk Assessment
- Response lớn hơn (30 correction kèm lý do): vẫn chỉ vài chục KB; không phân trang.
- `practiceKnowledgePointId` trỏ tới KP đã hết gói luyện tự do: frontend dựa vào phản hồi của endpoint luyện tự do, không giả định luôn luyện được.
- Giao diện quá tải với học viên band thấp: giữ nguyên tắc màn hình đầu tối giản; đo bằng tỷ lệ bấm "Luyện lỗi này".
