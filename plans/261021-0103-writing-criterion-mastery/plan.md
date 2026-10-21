---
title: "Mastery Writing theo tiêu chí: mỗi tiêu chí một bằng chứng KP"
description: "Sau khi chấm essay, ghi một bằng chứng mastery cho mỗi tiêu chí (TA/TR, CC, LR, GRA) vào KP tương ứng, và ghi mã lỗi phổ biến nhất vào event, để mastery biết học viên yếu ở đâu."
status: pending
priority: P2
branch: "fix/learn-writing-skill"
tags: [content-service, learning-service, mastery, writing]
blockedBy: []
blocks: []
created: "2026-10-20T18:07:41.859Z"
createdBy: "ck:plan"
source: skill
---

# Mastery Writing theo tiêu chí: mỗi tiêu chí một bằng chứng KP

## Overview

Hiện tại một bài essay chỉ ghi MỘT kết quả đạt/chưa đạt (`passed` = band tổng >= `passBand` 5.5) cho các KP gắn với đề (`question_knowledge_points`, V63). Mastery vì thế không biết học viên yếu tiêu chí nào, và `corrections[].errorCode` chỉ nằm trong bài nộp, không vào `question_attempt_events.error_code`.

Nâng cấp, làm một lần (không chia mức): sau khi chấm xong, ghi một bằng chứng cho MỖI TIÊU CHÍ vào KP tương ứng (đúng nếu band tiêu chí >= `passBand` của đề), và ghi mã lỗi phổ biến nhất vào event. Giữ nguyên cách LLM chấm, band tổng, `passed`, phí 3 điểm, và việc Writing không sinh bài ôn chặn.

Hiện trạng đã xác minh trong code:
- Essay lesson: `SubmitLessonEssayUseCase.finish()` ghi 1 event (errorCode `null`) + bằng chứng `LESSON_WRITING` (trọng số `essay` 0.5) cho KP của đề, chỉ ở các bài nộp trước khi block đạt.
- Essay Practice: ghi khi "Hoàn thành bộ luyện tập" (`SubmitPracticeAttemptUseCase`), mỗi essay là 1 item: event + bằng chứng `PRACTICE_SET` (trọng số `practice` 0.6) cho KP của item, kết quả = `passed` của bài chấm mới nhất; chỉ lần nộp đầu của gói (`counted`).
- `ItemGrading` chỉ gán mã lỗi cho câu khách quan; `reviewNeeds` bỏ qua WRITING.
- Mastery MP-1.0 tính theo từng KP từ `kp_evidence`; dedupe 7 ngày theo `questionVersionId` trong từng KP; cửa sổ 8 bằng chứng.

## Quyết định (đề xuất, có thể đổi trước khi làm)

| # | Quyết định | Lý do |
|---|---|---|
| D1 | Ánh xạ `(task, tiêu chí)` -> KP, lưu trong bảng content-service, không gắn vào phiên bản rubric: TA -> `W60_T1_KEY_FEATURES`; TR -> `W70_IDEA_DEVELOPMENT`; CC -> `W60_PARAGRAPH_COHESION`; LR -> `W70_LEXICAL_PRECISION`; GRA -> `W70_COMPLEX_STRUCTURES` | Cả 5 KP có sẵn trong V43. Rubric đã publish là bất biến; ánh xạ tách riêng thì phiên bản rubric mới vẫn dùng được. Các KP ngữ pháp `G55_*` còn `INACTIVE`, không dùng |
| D2 | Tiêu chí đúng khi `band >= prompt.passBand` (dùng chung 5.5) | Khớp ngưỡng đạt hiện nay, không thêm cấu hình |
| D3 | Trọng số không đổi (essay lesson 0.5, essay Practice 0.6) | Mỗi KP chỉ nhận 1 điểm mỗi essay vì mỗi tiêu chí ứng với KP khác nhau, nên cửa sổ 8 bằng chứng không bị chiếm thêm |
| D4 | Với cùng một KP, kết quả tiêu chí thay kết quả `passed`; KP của đề không thuộc ánh xạ giữ `passed` như cũ | Không mất hành vi cũ của KP bài học, không tính hai lần |
| D5 | Mỗi essay vẫn đúng 1 event (khóa unique `user, source, ref, question`): `correct = passed`; `error_code` = mã lỗi gặp nhiều nhất trong `corrections` (hòa: theo chữ cái); `knowledge_point_ids` = KP của đề + KP của các tiêu chí chưa đạt | Cột `error_code` chỉ một giá trị; thống kê lỗi theo KP đọc từ mảng KP của event |
| D6 | Nếu hai tiêu chí cùng ánh xạ một KP thì KP đó đúng khi tất cả tiêu chí liên quan đúng | Không phát sinh trong ánh xạ mặc định, nhưng quy tắc phải xác định |
| D7 | `WritingGrade.Criterion` thêm `knowledgePointId` (nullable) và được lưu trong JSON bài chấm | Ghi bằng chứng không cần gọi Content trong transaction; bài cũ không có trường này thì đọc ra `null` |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Content: ánh xạ tiêu chí sang KP và contract](./phase-01-content-nh-x-ti-u-ch-sang-kp-v-contract.md) | Pending |
| 2 | [Learning: grader gắn KP vào điểm tiêu chí](./phase-02-learning-grader-g-n-kp-v-o-i-m-ti-u-ch.md) | Pending |
| 3 | [Learning: ghi bằng chứng KP và mã lỗi cho essay lesson và Practice](./phase-03-learning-ghi-b-ng-ch-ng-kp-v-m-l-i-cho-essay-lesson-v-practi.md) | Pending |
| 4 | [Learning: backfill và rebuild mastery cho essay cũ](./phase-04-learning-backfill-v-rebuild-mastery-cho-essay-c.md) | Pending |
| 5 | [Test và tài liệu](./phase-05-test-v-t-i-li-u.md) | Pending |

Thứ tự bắt buộc: 1 -> 2 -> 3 -> 4 -> 5 (phase sau cần contract và dữ liệu của phase trước). Triển khai cùng một lượt: content-service phải chạy migration trước khi learning-service nhận trường mới; learning-service chịu được thiếu trường (null) nên thứ tự khởi động không gây lỗi.

## Ngoài phạm vi

- Bài ôn riêng cho KP Writing (cần soạn câu hỏi khách quan); Writing vẫn không sinh bài ôn chặn.
- Đổi prompt LLM; lọc mã lỗi Reading/Listening khỏi prompt Writing.
- Dùng `severity` của mã lỗi (hiện chưa có nơi nào đọc).
- Sửa bài mẫu, PS-W1-WR-3, hay file của Codex (V66/V67, `LessonPipelineSeedTest`). Migration mới đánh số V69.

## Rủi ro chính

- Mastery của học viên đã nộp essay bị tính lại sau backfill; mức Ưu tiên/Củng cố/Duy trì có thể đổi.
- KP tiêu chí không có trong danh mục KP của học viên thì bằng chứng bị bỏ (đúng như essay lesson hiện nay: `knownKnowledgePoints`); cần test với dữ liệu thật.
- `error_code` đơn lẻ gán cho mọi KP trong mảng của event, có thể gây nhiễu thống kê lỗi theo KP.
- Backfill cần giữ thời điểm bằng chứng gốc, nếu không bằng chứng cũ trông như mới và lệch hệ số giảm theo tuổi.
- File `docs/contracts/lesson-writing-v1.md` do Codex phụ trách; phối hợp trước khi sửa.

## Tiêu chí nghiệm thu

- Essay Task 1 chấm xong ghi đúng 4 bằng chứng (TA/CC/LR/GRA) vào 4 KP; Task 2 dùng TR; tiêu chí dưới `passBand` ghi sai.
- Event của essay có `error_code` khi bài có `corrections`; `null` khi không có.
- Band tổng, `passed`, phí, trạng thái bài nộp và phản hồi API không đổi.
- Bài cũ (không có `knowledgePointId`) vẫn ghi theo hành vi cũ cho tới khi backfill chạy; backfill chạy lại không nhân đôi bằng chứng.
- `package` của content-service và learning-service xanh, kèm test Testcontainers mới.

## Câu hỏi còn mở

1. GRA -> `W70_COMPLEX_STRUCTURES` đo độ đa dạng cấu trúc hơn là độ chính xác ngữ pháp. Chấp nhận, hay tạo KP "độ chính xác ngữ pháp" riêng (cần đưa vào topic để vào danh mục học viên)?
2. Nhiễu thống kê lỗi theo KP (D5) có chấp nhận được, hay ánh xạ mã lỗi theo `category` (GRAMMAR -> GRA, VOCABULARY -> LR, COHERENCE -> CC, TASK -> TA/TR) để gán lỗi đúng KP?
3. Backfill có chạy tự động khi khởi động (cờ mặc định bật) hay chỉ chạy bằng lệnh Admin?
4. Ai cập nhật hộp "Rubric Writing / Chẩn đoán lỗi theo Knowledge ID" trên board Figma (việc thủ công, xem phase 5)?
