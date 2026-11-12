# Bàn giao frontend: dữ liệu mới của kết quả chấm Writing

Dành cho dev frontend làm màn kết quả `GET /api/learning/writing-submissions/{id}` (và các response nộp bài cùng dạng). Contract đầy đủ: `docs/contracts/lesson-writing-v1.md`. Mọi trường dưới đây **cộng thêm**: bài chấm cũ vắng trường, không có trường nào làm vỡ client hiện tại.

## 1. Khi nào có trường nào

| Trường | Có khi |
|---|---|
| `corrections[].start`, `end` | Mọi bài `GRADED` có `essayText`; vắng khi đoạn trích không có vị trí tự do |
| `corrections[].reason`, `certainty` | Chỉ bài chấm bằng **template v2** (chưa kích hoạt ở thời điểm này) |
| `criteria[].evidence`, `descriptor` | Template v2, vắng khi rỗng |
| `criteria[].nextBand` | Mọi bài chấm mới; vắng ở band cao nhất và ở bài cũ |
| `errorGroups` | Có ít nhất một correction loại `ERROR` có mã lỗi |
| `priorities` | Có `errorGroups`, hoặc template v2 trả danh sách ưu tiên |
| `dataChecks` | Task 1, template v2 |
| `taskChecks` | Task 2, template v2 |
| `revision` | Bài nộp lại, khi đọc được bản trước |

Template v2 chưa bật cho học viên: cho tới khi bật, `reason`, `certainty`, `evidence`, `descriptor`, `dataChecks`, `taskChecks` sẽ vắng. Màn hình phải chạy tốt khi chúng vắng; `priorities` luôn dùng được qua `source = RULE`.

## 2. Việc cần làm theo thứ tự ưu tiên

1. **Khối "3 vấn đề cần tập trung trước tiên"** từ `priorities`: `title`, `explanation`, ví dụ trích từ `excerpts`, `action`, nút "Luyện lỗi này" mở bộ luyện theo `practiceKnowledgePointId` (ẩn nút khi không có). Phía trên là `summary`.
2. **Highlight trong bài**: dùng `start`/`end` trên `essayText`. Hai correction chồng nhau: vẽ cái bắt đầu trước, cái còn lại chỉ hiện trong danh sách. `certainty = SUGGESTION` hiển thị khác `ERROR` (nhãn "Gợi ý", màu nhẹ). Thiếu `certainty` coi như `ERROR`. Thẻ chi tiết: nhóm lỗi, câu gốc, câu sửa, "Tại sao sai?" (`reason`), quy tắc (`fixHint`), ví dụ tương tự (`errorGroups[].example`).
3. **Thẻ tiêu chí**: band (ghi rõ là ước tính), `evidence`, "Để lên <`nextBand.band`>: `nextBand.descriptor`". Tiêu chí có `limited: true` thì hiển thị lý do từ `bandLimits`.
4. **Task 1**: bảng "Bạn viết / Bảng cho thấy" từ `dataChecks.wrong[]` (`excerpt`, `expected`), cộng `correctCount` ("X câu số liệu đúng") và danh sách kiểm: `hasOverview`, `overviewAccurate`, `hasPersonalOpinion`. Cờ vắng nghĩa là mô hình không nói: không hiển thị. **Task 2**: "Đề yêu cầu / Bài bạn đã làm" từ `taskChecks.questionParts[]`, quan điểm từ `position`, kết bài từ `noConclusion`, và "Dàn ý bài của bạn" từ `outline[]` (bấm để cuộn tới đoạn có `excerpt`).
5. **Bài sau khi sửa**: ghép `essayText` với `corrections` theo `start`/`end`, hiển thị song song với bài gốc. Backend không sinh bài viết lại.
6. **Bài nộp lại**: "Đã sửa X dạng lỗi, còn lặp lại: …" từ `revision`.

Màn đầu chỉ có band, nhận xét tổng quan, khối 1 và nút luyện; phần còn lại thu gọn.

## 3. Quy tắc phải giữ

- `chartFacts` không bao giờ có trong response. Phát biểu đúng cho câu số liệu viết sai (`dataChecks.wrong[].expected`) là ngoại lệ đã được ghi vào `AGENTS.md`; đừng ghép các `expected` lại thành bảng dữ kiện.
- `start`/`end` là chỉ số ký tự trên `essayText` đúng như server gửi. Dùng cùng cách đếm với JavaScript (UTF-16): `essayText.slice(start, end) === excerpt`.
- Đừng giả định `errorGroups` cùng thứ tự với `corrections`. Nó đã sắp theo `count × severity`, mã dự phòng (`*_OTHER`) xếp cuối.

## 4. JSON mẫu (rút gọn)

### Task 1 (template v2)

```json
{
  "submissionId": "…", "status": "GRADED", "task": "TASK_1", "overallBand": 6.0, "passed": true,
  "essayText": "The chart shows sales. Sales rose to 40 in 2020. Sales fell to 10 in 2021. I think this is sad.",
  "criteria": [{
    "code": "TA", "band": 6.0, "strengths": [], "improvements": [], "knowledgePointJudgments": [],
    "evidence": ["Sales rose to 40 in 2020."], "descriptor": "Band 6: …",
    "nextBand": {"band": 7.0, "descriptor": "Band 7: …"}
  }],
  "corrections": [{
    "excerpt": "Sales fell to 10 in 2021.", "suggestion": "Sales fell to 15 in 2021.", "category": "TASK",
    "errorCode": "T1_DATA_MISREPORTED", "errorName": "Số liệu sai", "fixHint": "Đối chiếu bảng", "severity": 3,
    "start": 49, "end": 74, "certainty": "ERROR"
  }],
  "errorGroups": [{"errorCode": "T1_DATA_MISREPORTED", "errorName": "Số liệu sai", "category": "TASK", "count": 1,
                   "severity": 3, "fixHint": "Đối chiếu bảng"}],
  "priorities": [{"title": "Số liệu sai", "explanation": "Đối chiếu bảng", "excerpts": ["Sales fell to 10 in 2021."],
                  "action": "Đối chiếu bảng", "errorCodes": ["T1_DATA_MISREPORTED"], "source": "RULE"}],
  "dataChecks": {
    "wrong": [{"excerpt": "Sales fell to 10 in 2021.", "expected": "Năm 2021 doanh số là 15."}],
    "correctCount": 1, "hasOverview": false, "hasPersonalOpinion": true
  }
}
```

### Task 2 (template v2)

```json
{
  "submissionId": "…", "status": "GRADED", "task": "TASK_2", "overallBand": 6.0,
  "taskChecks": {
    "questionParts": [
      {"part": "Nêu ý kiến", "addressed": "FULL", "excerpt": "In my view, schools matter a lot."},
      {"part": "Đưa giải pháp", "addressed": "NONE"}
    ],
    "position": {"required": true, "stated": true, "excerpt": "In my view, schools matter a lot.", "consistent": true},
    "outline": [
      {"role": "INTRODUCTION", "mainIdea": "Giới thiệu", "developed": false, "excerpt": "Some say schools matter."},
      {"role": "BODY", "mainIdea": "Kỹ năng", "developed": true, "excerpt": "First, they teach skills."}
    ],
    "partialAnswer": true, "noPosition": false, "inconsistentPosition": false, "noConclusion": true
  }
}
```

### Bài nộp lại

```json
{
  "submissionId": "…", "status": "GRADED",
  "revision": {"previousSubmissionId": "…", "fixedCodes": ["PLURAL_FORM"], "repeatedCodes": ["TENSE"], "newCodes": ["WORD_CHOICE"]}
}
```

## 5. Chưa có / lưu ý

- Chưa có dữ liệu thật từ template v2 để chụp màn hình: dùng dữ liệu mẫu ở trên và kiểm khi template được kích hoạt.
- `OFF_TOPIC` chưa có kiểm tra phía sau nên `bandLimits` sẽ không bao giờ có lý do `OFF_TOPIC` cho tới khi thêm.
- Các luật hạ band (`bandLimits`) mặc định tắt; khi bật, tiêu chí bị hạ có `limited: true`.
- Câu hỏi còn mở: để trống.
