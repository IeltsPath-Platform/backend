-- Demo lesson resources for the five Writing lessons (W1 to W5) of the chapter "Writing cơ bản": a Markdown summary, one
-- slide deck and two course documents each. Like the Reading demo, the slide link points at a shared demo deck and the
-- document links are placeholders until the real files exist; replace them through the lesson resource admin API.

UPDATE lessons SET summary_markdown = $md$## Learning Outcomes

Sau bài học, bạn có thể:

1. Viết câu tổng quan (overview) nêu đặc điểm nổi bật nhất của biểu đồ.
2. Chọn và so sánh số liệu để chứng minh cho overview.
3. Dùng ngôn ngữ so sánh và chỉ xu hướng chính xác.

## Task 1 Structure

- **Introduction:** diễn đạt lại đề bài bằng một câu.
- **Overview:** 2 câu nêu xu hướng chính hoặc điểm khác biệt lớn nhất, không đưa số liệu chi tiết.
- **Body 1 và Body 2:** nhóm số liệu theo logic (thời gian hoặc nhóm đối tượng) và so sánh.

## Useful Language

- **Compare:** higher than, twice as much as, the same as, whereas
- **Trends:** rose, fell, remained stable, peaked at

> **IELTS Note:** Thiếu overview là lỗi làm giảm điểm Task Achievement nhiều nhất ở Task 1; không đưa ý kiến cá nhân vào bài.$md$
WHERE id = '25000000-0000-4000-8000-030000000001';

UPDATE lessons SET summary_markdown = $md$## Learning Outcomes

Sau bài học, bạn có thể:

1. Nêu quan điểm rõ ràng ngay ở mở bài của bài luận.
2. Viết đoạn thân bài có câu chủ đề, lý do và ví dụ.
3. Viết kết luận nhắc lại quan điểm mà không lặp nguyên văn.

## Opinion Essay Structure

- **Introduction:** paraphrase đề + nêu quan điểm (thesis statement).
- **Body 1 / Body 2:** mỗi đoạn một lý do chính: topic sentence, giải thích, ví dụ.
- **Conclusion:** tóm tắt lý do và khẳng định lại quan điểm.

## Useful Language

- **Opinion:** In my opinion, I strongly believe that, From my point of view
- **Reasons:** because, since, due to
- **Examples:** for instance, such as

> **IELTS Note:** Giữ một quan điểm nhất quán từ mở bài đến kết luận để đạt Task Response.$md$
WHERE id = '25000000-0000-4000-8000-030000000002';

UPDATE lessons SET summary_markdown = $md$## Learning Outcomes

Sau bài học, bạn có thể:

1. Diễn đạt lại đề bài ở mở bài thay vì chép nguyên văn.
2. Viết câu chủ đề nêu ý chính của đoạn thân bài.
3. Phân biệt câu chủ đề với ví dụ và chi tiết.

## Core Ideas

- **Paraphrase:** đổi từ vựng và cấu trúc, giữ nguyên nghĩa. *"Many people believe..." -> "A large number of individuals consider..."*
- **Topic sentence:** câu khái quát đứng đầu đoạn, nêu ý chính và lý do cốt lõi.
- **Thesis statement:** câu nêu quan điểm của bạn trong mở bài.

## Common Mistakes

- Chép lại đề bài.
- Câu chủ đề quá chung chung ("Nhiều người làm việc ở nhà") hoặc chỉ là một ví dụ.

> **IELTS Note:** Người chấm đọc mở bài và câu chủ đề trước tiên để nắm lập luận của bạn.$md$
WHERE id = '34000000-0000-4000-8000-030000000001';

UPDATE lessons SET summary_markdown = $md$## Learning Outcomes

Sau bài học, bạn có thể:

1. Chọn từ nối theo chức năng: đối lập, nguyên nhân, kết quả, bổ sung.
2. Dùng từ nối đúng vị trí và dấu câu.
3. Tránh lạm dụng từ nối làm bài viết máy móc.

## Linking Words by Function

- **Contrast:** however, whereas, although, on the other hand
- **Reason:** because, since, due to
- **Result:** therefore, consequently, as a result
- **Addition:** moreover, in addition, furthermore
- **Example and conclusion:** for instance, in conclusion

## Punctuation

- *However* đứng đầu câu mới, theo sau là dấu phẩy: *"Cars are convenient. However, they cause pollution."*
- *Because* nối hai mệnh đề trong cùng một câu; *because of* đi với danh từ.

> **IELTS Note:** Coherence and Cohesion chấm sự mạch lạc của ý trước, số lượng từ nối không quyết định điểm.$md$
WHERE id = '34000000-0000-4000-8000-030000000002';

UPDATE lessons SET summary_markdown = $md$## Learning Outcomes

Sau bài học, bạn có thể:

1. Dùng động từ và danh từ chỉ xu hướng tăng, giảm, giữ nguyên.
2. Thêm trạng từ chỉ mức độ và mốc thời gian để mô tả đủ ý.
3. Tránh lặp lại một động từ trong cả bài.

## Trend Vocabulary

- **Increase:** rise, increase, grow, climb, soar
- **Decrease:** fall, decrease, decline, drop, plummet
- **No change:** remain stable, stay unchanged, level off
- **Highest point:** peak at, reach a peak of

## Adverbs and Prepositions

- **Degree:** sharply, dramatically, gradually, slightly
- **Prepositions:** rose *from* 40 *to* 65, rose *by* 25, peaked *at* 900

> **IELTS Note:** Dùng đúng thì quá khứ khi biểu đồ nói về các năm đã qua và nêu rõ mốc thời gian.$md$
WHERE id = '34000000-0000-4000-8000-030000000003';

INSERT INTO lesson_resources (lesson_id, resource_type, title, url, sort_order) VALUES
    ('25000000-0000-4000-8000-030000000001', 'SLIDE', 'Slide bài giảng: Mô tả biểu đồ (Task 1)', 'https://docs.google.com/presentation/d/1rky9iPmndXAGDhwcMSI8QM2jN1QaVyelycgAsloB7kM/edit', 0),
    ('25000000-0000-4000-8000-030000000001', 'DOCUMENT', 'Tài liệu 1: Giáo trình Writing Task 1', 'https://example.com/ieltspath/writing-w1-giao-trinh.pdf', 0),
    ('25000000-0000-4000-8000-030000000001', 'DOCUMENT', 'Tài liệu 2: Bài mẫu Task 1', 'https://example.com/ieltspath/writing-w1-bai-mau.pdf', 1),
    ('25000000-0000-4000-8000-030000000002', 'SLIDE', 'Slide bài giảng: Luận quan điểm (Task 2)', 'https://docs.google.com/presentation/d/1rky9iPmndXAGDhwcMSI8QM2jN1QaVyelycgAsloB7kM/edit', 0),
    ('25000000-0000-4000-8000-030000000002', 'DOCUMENT', 'Tài liệu 1: Giáo trình Writing Task 2', 'https://example.com/ieltspath/writing-w2-giao-trinh.pdf', 0),
    ('25000000-0000-4000-8000-030000000002', 'DOCUMENT', 'Tài liệu 2: Bài mẫu Task 2', 'https://example.com/ieltspath/writing-w2-bai-mau.pdf', 1),
    ('34000000-0000-4000-8000-030000000001', 'SLIDE', 'Slide bài giảng: Mở bài và câu chủ đề', 'https://docs.google.com/presentation/d/1rky9iPmndXAGDhwcMSI8QM2jN1QaVyelycgAsloB7kM/edit', 0),
    ('34000000-0000-4000-8000-030000000001', 'DOCUMENT', 'Tài liệu 1: Giáo trình mở bài và câu chủ đề', 'https://example.com/ieltspath/writing-w3-giao-trinh.pdf', 0),
    ('34000000-0000-4000-8000-030000000001', 'DOCUMENT', 'Tài liệu 2: Bài tập paraphrase', 'https://example.com/ieltspath/writing-w3-bai-tap.pdf', 1),
    ('34000000-0000-4000-8000-030000000002', 'SLIDE', 'Slide bài giảng: Từ nối và mạch lạc', 'https://docs.google.com/presentation/d/1rky9iPmndXAGDhwcMSI8QM2jN1QaVyelycgAsloB7kM/edit', 0),
    ('34000000-0000-4000-8000-030000000002', 'DOCUMENT', 'Tài liệu 1: Bảng từ nối theo chức năng', 'https://example.com/ieltspath/writing-w4-tu-noi.pdf', 0),
    ('34000000-0000-4000-8000-030000000002', 'DOCUMENT', 'Tài liệu 2: Bài tập từ nối', 'https://example.com/ieltspath/writing-w4-bai-tap.pdf', 1),
    ('34000000-0000-4000-8000-030000000003', 'SLIDE', 'Slide bài giảng: Mô tả xu hướng', 'https://docs.google.com/presentation/d/1rky9iPmndXAGDhwcMSI8QM2jN1QaVyelycgAsloB7kM/edit', 0),
    ('34000000-0000-4000-8000-030000000003', 'DOCUMENT', 'Tài liệu 1: Từ vựng chỉ xu hướng', 'https://example.com/ieltspath/writing-w5-tu-vung.pdf', 0),
    ('34000000-0000-4000-8000-030000000003', 'DOCUMENT', 'Tài liệu 2: Bài tập mô tả biểu đồ', 'https://example.com/ieltspath/writing-w5-bai-tap.pdf', 1);
