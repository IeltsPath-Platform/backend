---
phase: 6
title: Learning feedback and report
status: completed
priority: P2
effort: 1d
dependencies:
  - 5
---

# Phase 6: Learning feedback and report

## Overview
Trả tên lỗi + `fixHint` khi chọn đáp án sai có mã; báo cáo và cây kiến thức hiện KP con (đã có bằng chứng) lồng dưới KP cha;
giỏ ôn ghi lỗi chính cho mục `ERROR_PATTERN`.

## Requirements
1. **Phản hồi khi sai:** kết quả từng câu ở bài tập lesson, Practice, set bài ôn, luyện tự do, câu chẩn đoán có thêm `error`
   khi câu sai và `ErrorTagger` trả mã (cả mã luật câu điền như `SPELLING`). Tên/`fixHint` qua `ErrorTypeTexts` (phase 5);
   mã không có trong danh mục hoặc gọi lỗi → không có `error`, lần nộp vẫn thành công. Không lộ `answerSpec`. <!-- red-team PH3 -->
   Tìm các record kết quả: `PracticeSubmission.Answer` (`SubmitPracticeAttemptUseCase.java:~170`) và record tương ứng của
   lesson/review/free practice/diagnostic (`grep -rn "explanation()" application/usecase application/service`).
2. **Báo cáo học viên** (`GetLearnerReportUseCase`) và **cây kiến thức học viên**: mỗi KP cha có `errorTraps` gồm KP con có
   ít nhất một bằng chứng (backend lọc, frontend không phải ẩn). "Lỗi hay gặp" của KP cha = lỗi chính (phase 5).
   Không xóa cột `top_error_codes`; grep `topErrorCodes`, ngừng dùng ở báo cáo học viên nếu có; thống kê từng câu cho staff giữ.
3. **Giỏ ôn:** mục có `reason = ERROR_PATTERN` có thêm `errorTrap` (lỗi chính hiện tại của KP cha).
4. **Chi tiết bài ôn** (`GET /reviews/{reviewId}` và các response trả cùng dạng) thêm `blocking` (boolean, lấy từ
   `ReviewItem.blocking()`), để màn bài ôn ghi đúng "bắt buộc" hay "tự chọn" (frontend đã dùng; thiếu trường thì coi là bắt buộc).

## Hợp đồng JSON (trường mới, đều tùy chọn)

```json
// kết quả một câu
{ "questionVersionId": "…", "correct": false, "correctAnswer": "NOT_GIVEN", "explanation": "…",
  "error": { "code": "TFNG_NG_AS_FALSE", "name": "Chọn False khi thông tin không được nêu",
             "fixHint": "Phân biệt \"bài nói sai\" (False) với \"bài không nói tới\" (Not Given) trước khi chọn." } }

// bài ôn KP cha (GET bài ôn) và mục giỏ ôn reason = ERROR_PATTERN
{ "blocking": false, "errorTrap": { "code": "TFNG_NG_AS_FALSE", "name": "…", "fixHint": "…" } }   // blocking chỉ ở GET bài ôn

// KP trong báo cáo / cây
{ "knowledgePointId": "…", "code": "TFNG_FALSE_VS_NOT_GIVEN", "tier": "MAINTAIN", "mastery": 0.85,
  "errorTraps": [ { "knowledgePointId": "…", "code": "TFNG_NG_AS_FALSE", "name": "…",
                    "tier": "PRIORITY", "mastery": 0.45, "evidenceCount": 5 } ] }
```

## Related Code Files
- Modify: result/DTO kết quả câu của 5 đường chấm; `GetLearnerReportUseCase` + DTO; use case cây kiến thức học viên + DTO
  (theo `docs/contracts/knowledge-tree-v1.md`); `GetReviewBasketUseCase` + DTO
- Modify: `docs/contracts/lesson-learning-v1.md`, `free-practice-v1.md`, `diagnostics-v1.md`, `learner-report-v1.md`, `knowledge-tree-v1.md`

## Implementation Steps
1. **Đỏ trước** (`@WebMvcTest`/use case/Testcontainers): chọn bẫy có mã → có `error` đúng; chọn đúng → không; câu điền sai
   chính tả → `SPELLING`; mã không có trong danh mục → không `error`; content lỗi → lần nộp thành công, không `error`;
   báo cáo/cây có `errorTraps` chỉ gồm KP con có bằng chứng; mục giỏ ôn `ERROR_PATTERN` có `errorTrap`.
2. Cài đặt; cập nhật contract đúng hợp đồng JSON trên.
3. `mvn -q -pl services/learning-service -am test`.

## Success Criteria
- [ ] Test mới xanh, test cũ xanh.
- [ ] Contract khớp hợp đồng JSON trên.

## Risk Assessment
- `fixHint` mã `DRAFT` hiện cho học viên: chấp nhận theo quyết định người dùng; docs ghi cần duyệt.
