---
phase: 6
title: "Band caps"
status: pending
priority: P2
dependencies: [4]
---

# Phase 6: Luật cứng cho band (learning, domain)

## Overview
Những gì là luật của trung tâm thì code quyết, không để LLM quyết. Band của LLM đi qua một bước áp trần thuần, cấu hình
được. Bảng ngưỡng **do examiner duyệt** (quyết định 4); tính năng tắt mặc định cho tới khi có bảng. Cách cấu hình và hình
dạng response **theo mẫu Speaking** (quyết định 13), để frontend dùng một kiểu hiển thị cho cả hai kỹ năng.

## Mẫu Speaking đang có
- `SpeakingPracticeSettings`: mỗi luật một trường `*BandCap` (`tooShortBandCap`, `offTopicBandCap`, `notEnglishBandCap`).
- `SpeakingResultAssembler`: `Limit(reason, cap, message)`; band hiển thị = `min(band, cap thấp nhất)`.
- Response: `bandLimits[] {reason, cap, message}`; tiêu chí bị hạ có `limited: true`; signals giữ `bandBeforeLimit`.
- Khác biệt cần thêm cho Writing: trần áp **theo tiêu chí** (TA/TR, GRA), Speaking áp chung. Mỗi mục `bandLimits` thêm
  `criterion` (null = mọi tiêu chí). Đây là trường cộng thêm, Speaking không đổi.

## Requirements
- `domain/service/WritingBandCaps` (thuần: không Spring, không clock, không I/O). Input: task, band từng tiêu chí, số từ,
  `minWords` của đề, các cờ từ LLM (lạc đề, thiếu overview, có ý kiến cá nhân; Task 2: `partialAnswer`, `noPosition` từ
  phase 8). **Không** nhận số lỗi: band không tính
  từ số lỗi đếm được (quyết định 17).
  Output: danh sách `Limit(reason, criterion, cap, message)`; band sau trần tính bằng `min` như Speaking.
- Luật ứng viên (examiner chọn và đặt số):
  | `reason` | Tiêu chí | Điều kiện |
  | --- | --- | --- |
  | `TOO_SHORT` | TA/TR | số từ < `minWords` của đề (có thể hai bậc: thiếu ít, thiếu nhiều) |
  | `OFF_TOPIC` | TA/TR | LLM gắn cờ lạc đề **kèm trích dẫn hợp lệ** |
  | `T1_NO_OVERVIEW` | TA | Task 1 không có overview |
  | `PARTIAL_ANSWER` | TR | Task 2 có yêu cầu đề `PARTIAL` hoặc `NONE` kèm trích dẫn hợp lệ (quyết định 21) |
  | `NO_POSITION` | TR | Task 2, đề hỏi ý kiến mà bài không nêu quan điểm (quyết định 21) |
  `TOO_SHORT` và `OFF_TOPIC` dùng cùng tên `reason` với Speaking.
- Trần chỉ **hạ** band, không bao giờ nâng. `overallBand` tính lại bằng `WritingScore.overallBand` sau trần.
- Cấu hình trong `WritingSettings` theo kiểu `SpeakingPracticeSettings`: `tooShortBandCap`, `offTopicBandCap`,
  `noOverviewBandCap`, `partialAnswerBandCap`, `noPositionBandCap` (`@DefaultValue`, `@Min`, `@Max`), cộng
  `bandCapsEnabled` (mặc định `false`). Giá trị đặt trong config-repo, đổi không cần deploy.
- JSON bài chấm lưu `bandBeforeLimit` trên từng tiêu chí bị hạ (như signals của Speaking) và `bandLimits`.
- Response: `bandLimits[] {reason, criterion, cap, message}`; tiêu chí bị hạ có `limited: true`. `message` tiếng Việt,
  ví dụ "Bài chưa đủ 150 từ nên TA bị giới hạn ở 5.0".
- Evidence, `passed` và bài ôn dùng band **sau** trần.
- Cờ lạc đề cần thêm vào prompt (một trường `offTopic` kèm `excerpt`); gộp vào lần sửa template gần nhất để không tốn một
  vòng eval riêng.

## Related Code Files
- Create: `domain/service/WritingBandCaps.java`, VO `Limit` trong `domain/vo/`
- Modify: `application/service/EssayGrader.java` (gọi sau parse), `application/service/WritingSettings.java`,
  `domain/vo/WritingGrade.java`, `api/dto/response/WritingSubmissionResponse.java`, `learning-service.yaml`
- Đọc để theo mẫu: `application/service/SpeakingPracticeSettings.java`, `application/service/SpeakingResultAssembler.java`
- Docs: `docs/contracts/lesson-writing-v1.md` (ghi rõ `bandLimits` cùng hình dạng với Speaking, thêm `criterion`);
  `AGENTS.md` §3.6 (nhóm cấu hình mới trong `learning.writing.*`)
- Tests: test domain cho từng luật và biên (đúng bằng ngưỡng, nhiều luật cùng tiêu chí lấy trần thấp nhất, tắt cờ thì
  không đổi gì); test response `bandLimits` và `limited` giống hình dạng của Speaking

## Implementation Steps
1. Gửi examiner bảng luật ứng viên để chọn và điền ngưỡng. **Chặn bước 4**, không chặn bước 2 và 3.
2. Test đỏ domain với ngưỡng giả → code `WritingBandCaps`.
3. Nối vào grader sau cờ `bandCapsEnabled = false`; deploy được ngay vì không đổi hành vi.
4. Có bảng ngưỡng → chạy eval với caps bật: within-half-band và QWK phải không tụt. Bật ở cấu hình. Hai luật Task 2 chỉ
   bật khi phase 8 đã qua bước kiểm tay của examiner.

## Success Criteria
- [ ] `bandCapsEnabled = false`: kết quả chấm giống hệt trước phase này.
- [ ] Mỗi band bị hạ đều có mục trong `bandLimits` và `limited: true`.
- [ ] Frontend hiển thị trần band của Writing và Speaking bằng cùng một component.
- [ ] Eval với caps bật vẫn đạt ngưỡng.

## Risk Assessment
- Trần sai làm điểm lệch có hệ thống so với giám khảo: chỉ bật sau eval, và giữ `bandBeforeLimit` để tính lại được.
- Cờ lạc đề của LLM có thể sai: yêu cầu trích dẫn hợp lệ, và cân nhắc để luật `OFF_TOPIC` chỉ gắn cờ cho Mentor thay vì hạ band (examiner quyết).
- Essay đã chấm trước đó không được tính lại.
