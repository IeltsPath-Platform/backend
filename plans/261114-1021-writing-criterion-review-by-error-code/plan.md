---
title: Bài ôn tiêu chí Writing ôn KP của lỗi gặp nhiều nhất
status: implemented (tests pending full run)
branch: fix/writing-correction-text-and-template-v2
created: 2026-11-14
---

# Bài ôn tiêu chí Writing chọn KP theo mã lỗi

Quyết định của product owner (2026-11-14): khi một tiêu chí rubric của essay rớt (band < `passBand`), bài ôn tùy chọn
`WRITING_CRITERION` ôn KP của mã lỗi gặp nhiều nhất thuộc tiêu chí đó, thay vì luôn ôn KP tiêu chí. Chỉ essay trong course
chấm bằng AI (lesson, Practice của course). Luyện tự do, Mentor và Speaking không đổi.

## Quy tắc

1. Tiêu chí quyết định có bài ôn hay không (không đổi): band < `passBand`, tối đa `criterion-review-limit` (2), yếu nhất trước.
2. Lỗi thuộc tiêu chí: correction không phải `SUGGESTION`, có `practiceKnowledgePointId` (KP của mã có bộ luyện).
   LR: nhóm VOCABULARY và `WORD_FORM`, `NOUN_PLURAL`, `WRITING_SPELLING`; GRA: GRAMMAR trừ ba mã đó; CC: COHERENCE; TA/TR: TASK.
3. Xếp mã: nhiều lần nhất, rồi `severity` cao hơn, rồi mã A→Z. Thử lần lượt KP của từng mã; bỏ KP đang có bài ôn chờ hoặc
   hết bộ luyện chưa xem (≥ 3 câu); cuối cùng là KP tiêu chí như cũ.
4. Mastery không đổi: band vẫn là bằng chứng cho KP tiêu chí.
5. Mỗi tiêu chí chỉ có một bài ôn chờ: bài ôn lưu thêm KP tiêu chí (`review_items.criterion_knowledge_point_id`), để Mentor
   vẫn nâng thành bắt buộc hoặc đóng đúng bài ôn của tiêu chí.

## Thay đổi

- learning V53: cột `criterion_knowledge_point_id` (nullable) ở `review_items`.
- `FailedWritingCriterion` mang danh sách KP ôn ưu tiên; `WritingEvidenceRule` điền từ corrections.
- `WritingCriterionReviewRule.select` chọn KP ôn; `WritingCriterionReviews` lấy availability cho cả KP ôn.
- Repository: lưu cột mới; promote/close và kiểm tra bài ôn chờ khớp theo KP tiêu chí.
- Contract `lesson-writing-v1.md`, `AGENTS.md` §3.8.

## Kiểm chứng

Test rule (chọn mã, phá hòa, dự phòng, bỏ KP chờ), test Testcontainers cho repository và luồng lesson essay;
`mvn -q -pl services/learning-service -am test` xanh.

## Rủi ro

- Bài ôn mang skill `WRITING` nhưng bộ luyện của KP ngữ pháp/từ vựng là trắc nghiệm.
- Model gắn sai mã thì ôn sai KP; bài ôn tùy chọn nên hậu quả nhẹ.
