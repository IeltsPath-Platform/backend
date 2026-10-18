-- Demo lesson resources: a Markdown summary, one slide deck and course documents for Reading lesson L1 (the first
-- lesson a new learner can open) and lesson T1 of the "Cây cối" topic. Document links are placeholders until real
-- files exist.

UPDATE lessons SET summary_markdown = $md$## Learning Outcomes

Sau bài học, bạn có thể:

1. Tìm câu chủ đề (topic sentence) của một đoạn văn IELTS.
2. Phân biệt câu chủ đề với câu dẫn và câu triển khai.
3. Dùng câu chủ đề để đoán nhanh ý của cả đoạn.

## Key Points

- **Vị trí thường gặp:** câu đầu đoạn; đôi khi là câu thứ hai, sau một câu dẫn.
- **Dấu hiệu:** câu khái quát, không chứa số liệu hay ví dụ cụ thể.
- **Mẹo kiểm tra:** thử bỏ câu đó đi. Nếu đoạn văn mất ý chung thì đó là câu chủ đề.

## Vocabulary Core

| Từ | Nghĩa |
| --- | --- |
| topic sentence | câu chủ đề |
| supporting detail | chi tiết bổ trợ |
| generalisation | sự khái quát |

## Collocations

- state the main idea
- support an argument
- skim a paragraph

> **IELTS Note:** Đọc câu chủ đề của từng đoạn trước khi đọc kỹ giúp bạn làm nhanh dạng Matching Headings.$md$
WHERE id = '20000000-0000-4000-8000-030000000001';

UPDATE lessons SET summary_markdown = $md$## Learning Outcomes

By the end of the lesson, students should be able to:

1. Describe a natural environment.
2. Express likes and dislikes about nature.
3. Give a simple reason and example.

## Grammar Core

- **Present Simple**: facts, habits and preferences. *"I often visit the countryside."*
- **There is / There are**: describing a place. *"There are many trees in this park."*
- **Because / So**: reason and result. *"It is peaceful, so I feel relaxed."*

## Vocabulary Core

- **Environment:** nature, forest, river, park, wildlife
- **Describing:** beautiful, peaceful, fresh, green, quiet
- **Problems:** pollution, dirty, crowded, plastic waste

## Collocations

- fresh air
- green landscape
- protect the environment
- spend time outdoors

## Idioms

- **a breath of fresh air**: something refreshing. *"The park is a breath of fresh air."*
- **at one with nature**: closely connected to nature.

> **IELTS Note:** Ở band 3.5–5.0, hãy ưu tiên từ vựng cơ bản chính xác và collocation tự nhiên thay vì học thuộc nhiều idiom.$md$
WHERE id = '2a000000-0000-4000-8000-030000000001';

INSERT INTO lesson_resources (id, lesson_id, resource_type, title, url, sort_order) VALUES
    ('31000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-030000000001', 'SLIDE',
     'Slide bài giảng: Câu chủ đề', 'https://docs.google.com/presentation/d/1rky9iPmndXAGDhwcMSI8QM2jN1QaVyelycgAsloB7kM/edit', 0),
    ('31000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-030000000001', 'DOCUMENT',
     'Tài liệu 1: Giáo trình Reading', 'https://example.com/ieltspath/reading-l1-giao-trinh.pdf', 0),
    ('31000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-030000000001', 'DOCUMENT',
     'Tài liệu 2: Bài tập luyện thêm', 'https://example.com/ieltspath/reading-l1-bai-tap.pdf', 1),
    ('31000000-0000-4000-8000-000000000004', '2a000000-0000-4000-8000-030000000001', 'SLIDE',
     'Slide bài giảng: Cây trong thành phố', 'https://docs.google.com/presentation/d/1rky9iPmndXAGDhwcMSI8QM2jN1QaVyelycgAsloB7kM/edit', 0),
    ('31000000-0000-4000-8000-000000000005', '2a000000-0000-4000-8000-030000000001', 'DOCUMENT',
     'Tài liệu 1: Giáo trình', 'https://example.com/ieltspath/trees-t1-giao-trinh.pdf', 0),
    ('31000000-0000-4000-8000-000000000006', '2a000000-0000-4000-8000-030000000001', 'DOCUMENT',
     'Tài liệu 2: Bài tập', 'https://example.com/ieltspath/trees-t1-bai-tap.pdf', 1),
    ('31000000-0000-4000-8000-000000000007', '2a000000-0000-4000-8000-030000000001', 'DOCUMENT',
     'Tài liệu 3: File luyện nghe', 'https://example.com/ieltspath/trees-t1-luyen-nghe.mp3', 2);
