# P2 – Content: block→KP và Practice gắn Lesson

Service: `content-service`. Phụ thuộc: P1.

## 1. Migration `V16__lesson_block_kps_and_practice_lesson.sql`

### 1.1 Block → KP

```sql
CREATE TABLE lesson_block_knowledge_points (
    block_id UUID NOT NULL REFERENCES lesson_blocks(id) ON DELETE CASCADE,
    knowledge_point_id UUID NOT NULL REFERENCES knowledge_points(id),
    PRIMARY KEY (block_id, knowledge_point_id)
);
CREATE INDEX idx_lesson_block_kps_kp ON lesson_block_knowledge_points (knowledge_point_id);
```

Chỉ dùng cho block **TEXT** (D3). ASSET là passage/audio của bài tập; đưa vào lý thuyết ôn tập sẽ lộ transcript.
EXERCISE không ghi vào đây (KP lấy từ `question_knowledge_points`). Ràng buộc "KP gắn vào block phải thuộc
`lesson_knowledge_points` của lesson, block phải là TEXT" kiểm bằng test nhất quán (không có API authoring block).

Seed (theo `code` lesson + `sort_order` block, có `WHERE EXISTS`):

| Lesson | Quy tắc gắn |
| --- | --- |
| Lesson chỉ có 1 KP (đa số seed: L1, L2, TF1, LS1, LS2, PM1, PM2, PS1, PS2, W1, W2) | mọi TEXT block → KP đó. |
| Lesson có nhiều KP (kiểm tra L3, L4) | Codex đọc nội dung TEXT block trong V9 và gắn tay từng block với KP nó dạy; ghi bảng ánh xạ vào mục Verification. Không chắc ⇒ gắn block vào mọi KP của lesson. |

### 1.2 Practice → Lesson

```sql
ALTER TABLE content_packages ADD COLUMN lesson_id UUID REFERENCES lessons(id);
ALTER TABLE content_packages ADD CONSTRAINT chk_package_lesson_practice
    CHECK (lesson_id IS NULL OR package_type = 'PRACTICE_SET');
CREATE INDEX idx_content_packages_lesson ON content_packages (lesson_id) WHERE lesson_id IS NOT NULL;
```

Backfill cho PRACTICE_SET hiện có: lấy KP của các câu hỏi trong version published hiện tại của package; chọn lesson
PUBLISHED **sớm nhất** (topic.sort_order, lesson.sort_order) có `lesson_knowledge_points` chứa KP đó; topic của lesson
phải cùng skill với câu hỏi. Kết quả mong đợi với seed: PS-KP1-A/B, PS-KP2-A/B, PS-KP3-A, PS-KP4-A → lesson Reading
tương ứng; PS-NUM/SPELL/PARA/TRAP → LS1/LS2. Codex in bảng `code → lesson code` vào Verification.

### 1.3 Seed thêm practice set (bắt buộc, D12)

Practice và set ôn dùng chung kho, và package đã nộp thì không được giao lại làm set ôn (D11). Seed hiện có 1–2
package mỗi KP nên thang ôn không chạy đủ. Seed thêm **2 PRACTICE_SET mới** cho mỗi KP sau (mỗi set ≥ 3 câu, câu
**mới** – search loại package chứa câu đã dùng trong lesson/topic test – có `answerSpec`, `explanation`, `hint`, KP
mapping, cùng skill):

| KP | Hiện có | Thêm | Lesson |
| --- | --- | --- | --- |
| KP của `PS-KP1-A/B` (Reading) | 2 | `PS-KP1-C`, `PS-KP1-D` | lesson dạy KP đó (dự kiến L1) |

**Không** tạo package Listening mới, không thêm media key mới (chưa có mp3). PS-NUM/SPELL/PARA/TRAP chỉ được gắn
`lesson_id` qua backfill 1.2. README content thêm mục "Còn thiếu": Listening mỗi KP chỉ 1 đề Practice, cần thêm mp3
để thang ôn đủ bước. ID theo dải mới `26000000-0000-4000-8000-…`. Câu hỏi bằng tiếng Anh, giải thích và hint tiếng
Việt như seed hiện có. Toàn văn mọi câu mới (stem, options, đáp án, giải thích, hint) liệt kê trong Verification để
chủ dự án duyệt (D15).

Test sửa có chủ đích: `LessonPipelineSeedTest` (khẳng định KP5 không có practice set ≥ 3 câu – nay TF1 có set mới)
và snapshot `lesson-pipeline-demo-expected.json`. Chỉ sửa kỳ vọng đúng phần đổi, ghi lý do vào Verification.

Lesson chưa có practice (D13 bắt buộc mỗi lesson thuộc topic có TOPIC_TEST có ≥ 1): seed **1 PRACTICE_SET** cho
mỗi lesson TF1, PM1, PM2, PS1, PS2 (≥ 3 câu mới, cùng dạng câu hỏi của lesson, có `answerSpec`, `explanation`, `hint`,
KP mapping tới KP của lesson, `lesson_id` gắn thẳng). Package của PM*/PS* mang `required_feature_key` giống topic
premium. Sau backfill mục 1.2, nếu lesson nào khác của topic có TOPIC_TEST vẫn chưa có practice set (ví dụ L2 nếu
backfill dồn package về L1), seed thêm 1 set cho lesson đó và ghi vào Verification.

KP5 (TFNG): set mới của TF1 là set đầu tiên của KP này, nên KP5 không còn là case "không có practice set". Case
"hết package ⇒ không tạo review / SKIPPED" vẫn phủ bằng integration test với stub.

### 1.4 Kiểm tra cuối V16

`DO $$` raise exception nếu có lesson PUBLISHED thuộc topic có TOPIC_TEST PUBLISHED mà không có PRACTICE_SET
PUBLISHED nào có `lesson_id` = lesson.

## 2. Internal lesson DTO

`GET /internal/learning-content/lessons/{id}`: mỗi block thêm `knowledgePointIds: UUID[]`:

- TEXT: từ `lesson_block_knowledge_points` (có thể rỗng); ASSET/VOCABULARY: `[]`;
- EXERCISE: hợp các `knowledgePointIds` của câu hỏi trong block (đã có trên question).

Lesson thêm `skill` (của topic). Chỉ thêm field.

## 3. Practice theo lesson (internal)

- `GET /internal/learning-content/lessons/{id}/practice-sets` → danh sách
  `{packageId, code, title, packageVersionId, questionCount, knowledgePointIds, requiredFeatureKey}` cho PRACTICE_SET
  PUBLISHED có `lesson_id = id`, sắp theo `code`. Không trả đáp án.
- `POST /practice-sets/search` thêm tham số tuỳ chọn `preferredLessonId`: nếu có, package gắn lesson đó xếp trước;
  ngoài ra giữ nguyên hành vi (KP, exclude, minQuestions, loại package chứa câu đã dùng trong lesson/topic test).
  Learning truyền vào `exclude` **toàn bộ package đã lộ** với learner (D11), không chỉ package đã giao.
- Mới `GET /internal/learning-content/topics/{id}/practice-sets` ⇒ `{lessons: [{lessonId, practiceSets: [...]}]}`
  (cùng item như route theo lesson), **một query** cho cả topic; Learning dùng để xét "qua Practice" (D13) mà không
  gọi route theo lesson trong vòng lặp.
- Mới `POST /internal/learning-content/practice-sets/availability` body
  `{knowledgePointIds (≤ 50), excludePackageIds, minQuestions}` ⇒ `{counts: {knowledgePointId: số package khả dụng}}`
  – cùng điều kiện với `search`, **một query** cho cả tập KP. Learning dùng khi tạo review từ Practice ("KP còn package
  chưa lộ") để không gọi `search` trong vòng lặp.
- Package version (`/package-versions/{id}`) giữ nguyên; item phải có `hint` (đã có từ V13) để Learning hiển thị.

## 4. Admin authoring

Chỉ `CreateContentPackageUseCase` (+ request/command/response) nhận `lessonId` tuỳ chọn: chỉ với PRACTICE_SET,
lesson phải tồn tại ⇒ ngược lại 422 `INVALID_PACKAGE_LESSON`. Kiểm cùng skill khi publish package
(`PublishContentPackageUseCase`): mọi câu hỏi phải cùng skill với topic của lesson. Không có API sửa lesson_id, không có
API authoring block (ngoài phạm vi).

## 5. Test

- Migration: bảng/constraint tồn tại; backfill `lesson_id` đúng bảng mong đợi; CHECK chặn `lesson_id` trên MOCK_TEST.
- Reader: block TEXT có `knowledgePointIds`; EXERCISE có hợp KP câu hỏi; `lessons/{id}/practice-sets` trả đúng gói và
  không lộ `answerSpec`/`explanation`.
- `search` với `preferredLessonId` xếp gói của lesson trước.
- Seed mục 1.3: KP của PS-KP1 có ≥ 3 package qua được `search` (minQuestions 3, không trùng câu
  lesson/topic test); câu nào cũng có `explanation`, `hint`; mọi lesson của topic có TOPIC_TEST có ≥ 1 practice set.
- `availability`: đếm đúng khi có `exclude`; nhiều KP một query; > 50 KP ⇒ 400.
- Test nhất quán: `lesson_block_knowledge_points` chỉ trỏ block TEXT, KP thuộc `lesson_knowledge_points` của lesson.

## Acceptance

`mvn -q -pl services/content-service -am test` xanh; contract mục P6 cập nhật đủ field mới.

## Verification

Status: completed 2026-10-03 (nhánh `feat/skill-tracks-practice`).

- `mvn -pl services/content-service -am test` (build sạch): 166 test, 0 fail, 0 skip, Testcontainers chạy V1–V16.
- Lệch so với plan, đã chọn phương án an toàn:
  - "KP của PS-KP1" là `DEMO_READING_MAIN_IDEA`, dạy ở **L2** (không phải L1) ⇒ `PS-KP1-C/D` gắn L2.
  - Backfill chỉ gắn PRACTICE_SET có ≥ 3 câu, nên package một câu `DEMO_MAIN_FLOW_READING` (V4) không thành Practice.
  - Plan ghi item của `/package-versions/{id}` "đã có `hint` từ V13" là sai: phase này thêm `hint` vào item (cần cho P4, P5).
  - Test sửa có chủ đích: `LessonPipelineSeedTest` (KP5 nay có `PS-TF-A`; tìm KP1 trả 4 set; tổng câu được giữ cho
    learning 83 → 104). Snapshot `lesson-pipeline-demo-expected.json` không cần đổi (chỉ chứa câu `20000000-…`).
- Review (subagent) đã sửa: `hint` trên item package version; thứ tự `preferredLessonId` không đẩy set không có lesson
  xuống cuối; list/add-version trả đúng `lessonId`; bỏ constructor tương thích che lỗi; chặn id null và giới hạn 1000
  `excludePackageIds`; viết lại gợi ý lộ đáp án; bỏ block dẫn nhập khỏi bảng block→KP.

Block → KP: mỗi block TEXT của seed được gắn với mọi KP của lesson chứa nó (đã đọc từng đoạn), trừ block dẫn nhập
L1 `20000000-…-040000000004` ("Luyện thêm với đoạn C và D.") không dạy gì nên không gắn KP. Lesson nhiều KP: L4
(`DR_MATCHING_HEADINGS`, `DEMO_READING_MAIN_IDEA`: đoạn "Tiêu đề phải khớp ý chính của cả đoạn…"), LS1 (`LS_NUM`,
`LS_SPELL`: đoạn form completion nói cả số và đánh vần), LS2 (`LS_PARA`, `LS_TRAP`: đoạn paraphrase và bẫy "not…, but…").

Practice → lesson:

| Package | Lesson | Package | Lesson |
| --- | --- | --- | --- |
| PS-KP1-A, B, C, D | L2 | PS-NUM, PS-SPELL | LS1 |
| PS-KP2-A, B | L3 | PS-PARA, PS-TRAP | LS2 |
| PS-KP3-A | L1 | PS-TF-A | TF1 |
| PS-KP4-A | L4 | PS-PM1-A, PS-PM2-A (premium) | PM1, PM2 |
| | | PS-PS1-A, PS-PS2-A (premium) | PS1, PS2 |

Câu hỏi mới cần chủ dự án duyệt (D15), 7 package × 3 câu:

#### PS-KP1-C
Passage: A. Night trains are returning to Europe after years of decline. New routes now link cities such as Vienna, Paris and Amsterdam, and several more are planned. B. Travellers give two main reasons for choosing them. Many want to avoid the pollution caused by short flights, and others like arriving in a city centre after a night's sleep instead of spending hours at an airport. C. However, the comeback is fragile. Tickets often cost more than flights, and operators complain that old carriages and high track fees make the services hard to run at a profit.
1. What is the passage mainly about?
   - Lựa chọn: A. Night trains are cheaper than flights / B. The return of night trains and the problems they face / C. How to book a sleeper carriage
   - Đáp án: B
   - Giải thích: Đoạn A, B nói tàu đêm quay lại và vì sao khách chọn; đoạn C nói khó khăn. A sai vì vé thường đắt hơn máy bay; C không được nhắc.
   - Gợi ý: Ý chính của cả bài phải đúng với cả ba đoạn, kể cả đoạn C mở đầu bằng However.
2. What is the main idea of paragraph B?
   - Lựa chọn: A. Why travellers choose night trains / B. Which cities have new routes / C. How long flights take
   - Đáp án: A
   - Giải thích: Đoạn B nêu hai lý do khách chọn tàu đêm. Tên các thành phố nằm ở đoạn A.
   - Gợi ý: Câu đầu đoạn B báo trước nội dung cả đoạn: "two main reasons".
3. What is the main idea of paragraph C?
   - Lựa chọn: A. Night trains are popular with families / B. Old carriages are being replaced / C. Night trains struggle to make money
   - Đáp án: C
   - Giải thích: Đoạn C nói tàu đêm khó có lãi: vé đắt, toa cũ, phí đường ray cao. Bài không nói toa cũ đang được thay.
   - Gợi ý: "However, the comeback is fragile" là câu chủ đề của đoạn C.
#### PS-KP1-D
Passage: A. More people now read news and books on screens than on paper. Phones and tablets are cheap, light and always at hand. B. Yet studies suggest that screens change how well we understand. In several experiments, students who read long texts on paper remembered more of the details and the order of events than those who read the same texts on a tablet. C. Researchers believe the reason is how we read rather than the device itself. On screens, people tend to scan quickly and jump between links, while printed pages encourage slower, deeper reading.
1. What is the passage mainly about?
   - Lựa chọn: A. Screen reading is common but may lead to weaker understanding / B. Tablets are cheaper than books / C. How to read a printed page
   - Đáp án: A
   - Giải thích: Đọc trên màn hình phổ biến (đoạn A), hiểu kém hơn (đoạn B) và lý do (đoạn C). Bài không so sánh giá máy tính bảng với sách (B), và không hướng dẫn cách đọc (C).
   - Gợi ý: Ý chính phải bao được cả ba đoạn, không chỉ đoạn đầu.
2. What is the main idea of paragraph B?
   - Lựa chọn: A. Students prefer tablets / B. In studies, readers on paper remembered more / C. Experiments are expensive
   - Đáp án: B
   - Giải thích: Đoạn B mở bằng "Yet studies suggest…" rồi kể kết quả: đọc trên giấy nhớ nhiều hơn.
   - Gợi ý: Từ "Yet" ở đầu đoạn B báo hiệu ý chính đổi hướng so với đoạn A.
3. Which title best fits the whole passage?
   - Lựa chọn: A. The History of the Tablet / B. Paper or Screen: Does It Change How We Read? / C. Why Students Dislike Long Texts
   - Đáp án: B
   - Giải thích: B bao cả sự phổ biến, kết quả nghiên cứu và lý do. A và C không phải nội dung chính của bài.
   - Gợi ý: So từng tiêu đề với ý của cả ba đoạn, không chỉ với một đoạn.
#### PS-TF-A
1. Passage: "The swimming pool is closed every Monday for cleaning." Statement: "The pool opens on Mondays."
   - Lựa chọn: TRUE. TRUE / FALSE. FALSE / NOT_GIVEN. NOT GIVEN
   - Đáp án: FALSE
   - Giải thích: Đoạn văn nói hồ bơi đóng cửa mọi thứ Hai, mâu thuẫn trực tiếp với câu khẳng định: FALSE.
   - Gợi ý: So câu khẳng định với ngày đóng cửa được nêu trong đoạn văn.
2. Passage: "The bakery sells bread made with flour from local farms." Statement: "The bakery's bread is cheaper than supermarket bread."
   - Lựa chọn: TRUE. TRUE / FALSE. FALSE / NOT_GIVEN. NOT GIVEN
   - Đáp án: NOT_GIVEN
   - Giải thích: Đoạn văn không nói gì về giá, nên không thể kết luận đúng hay sai: NOT GIVEN, không phải FALSE.
   - Gợi ý: Với từng chi tiết của câu khẳng định, hỏi: đoạn văn xác nhận, phủ nhận, hay không nói tới?
3. Passage: "All visitors must sign in at the front desk before entering the laboratory." Statement: "Visitors have to register before they go into the laboratory."
   - Lựa chọn: TRUE. TRUE / FALSE. FALSE / NOT_GIVEN. NOT GIVEN
   - Đáp án: TRUE
   - Giải thích: "sign in at the front desk" cùng nghĩa với "register": TRUE.
   - Gợi ý: So từng phần của câu khẳng định (ai, làm gì, khi nào) với đoạn văn.
#### PS-PM1-A
1. Passage: "A. The town was founded beside a river in 1820. B. A railway arrived in 1870 and trade grew quickly. C. Today most residents work in tourism." Which paragraph mentions how people earn a living now?
   - Lựa chọn: A. Paragraph A / B. Paragraph B / C. Paragraph C
   - Đáp án: C
   - Giải thích: "Today most residents work in tourism" nói cách người dân kiếm sống hiện nay.
   - Gợi ý: Tìm đoạn nói về công việc ở thời điểm hiện tại.
2. Passage: "A. The festival began as a small music event. B. Tickets sold out within an hour last year. C. Organisers plan to add a second stage." Which paragraph mentions how popular the event has become?
   - Lựa chọn: A. Paragraph A / B. Paragraph B / C. Paragraph C
   - Đáp án: B
   - Giải thích: Vé bán hết trong một giờ cho thấy sự kiện rất được ưa chuộng.
   - Gợi ý: Chi tiết nào cho thấy rất nhiều người muốn tham dự?
3. Passage: "A. The bridge took four years to build. B. Engineers used steel shipped from abroad. C. Around 20,000 cars cross it every day." Which paragraph mentions where the building materials came from?
   - Lựa chọn: A. Paragraph A / B. Paragraph B / C. Paragraph C
   - Đáp án: B
   - Giải thích: "steel shipped from abroad" cho biết vật liệu đến từ nước ngoài.
   - Gợi ý: Vật liệu là gì, và đoạn nào nói nó đến từ đâu?
#### PS-PM2-A
1. Passage: "A. The school has a large library. B. Students can borrow laptops free of charge. C. The library is closed on Sundays." Which paragraph mentions something that costs nothing?
   - Lựa chọn: A. Paragraph A / B. Paragraph B / C. Paragraph C
   - Đáp án: B
   - Giải thích: "free of charge" nghĩa là không tốn tiền. Đoạn A và C lặp từ "library" nhưng không nói về chi phí.
   - Gợi ý: Câu hỏi không dùng từ "free"; tìm cách nói khác của "costs nothing".
2. Passage: "A. The company was set up by two brothers. B. Its first shop opened in a small village. C. It now employs over 500 people." Which paragraph mentions the size of the workforce?
   - Lựa chọn: A. Paragraph A / B. Paragraph B / C. Paragraph C
   - Đáp án: C
   - Giải thích: "employs over 500 people" cho biết quy mô lực lượng lao động.
   - Gợi ý: "workforce" là những người làm việc cho công ty.
3. Passage: "A. Rain fell for six days without stopping. B. Rivers rose and several roads were flooded. C. Farmers say the wet weather damaged their crops." Which paragraph mentions harm to food production?
   - Lựa chọn: A. Paragraph A / B. Paragraph B / C. Paragraph C
   - Đáp án: C
   - Giải thích: "damaged their crops" là thiệt hại cho sản xuất lương thực; đoạn B nói đường bị ngập, không nói về lương thực.
   - Gợi ý: Diễn đạt "food production" bằng từ khác, rồi tìm cách nói đó trong từng đoạn.
#### PS-PS1-A
1. Passage: "The castle was rebuilt in stone after a fire destroyed its walls, which were made of timber." Complete the sentence with ONE WORD from the passage: The first walls were built from ______.
   - Lựa chọn: (điền)
   - Đáp án: timber
   - Giải thích: "walls, which were made of timber": từ cần điền là "timber".
   - Gợi ý: Câu cần một danh từ chỉ vật liệu; tìm vật liệu của bức tường cũ.
2. Passage: "Most of the island's electricity now comes from wind turbines." Complete the sentence with ONE WORD from the passage: Wind provides most of the island's ______.
   - Lựa chọn: (điền)
   - Đáp án: electricity
   - Giải thích: Câu gốc: "Most of the island's electricity … comes from wind": từ cần điền là "electricity".
   - Gợi ý: Tìm thứ mà gió cung cấp cho hòn đảo.
3. Passage: "The museum asks visitors to leave large bags in the lockers near the entrance." Complete the sentence with ONE WORD from the passage: Large bags must be left in the ______ near the entrance.
   - Lựa chọn: (điền)
   - Đáp án: lockers
   - Giải thích: Chép đúng dạng số nhiều trong bài: "lockers".
   - Gợi ý: Cần một danh từ chỉ nơi để túi xách.
#### PS-PS2-A
1. Passage: "Researchers counted the birds every morning for three months." Complete the sentence with ONE WORD from the passage: The birds were counted daily for three ______.
   - Lựa chọn: (điền)
   - Đáp án: months
   - Giải thích: Sau "three" cần danh từ số nhiều: "months". Viết "month" là sai.
   - Gợi ý: Sau một con số lớn hơn một, danh từ phải ở dạng nào?
2. Passage: "The new library will open to the public in early September." Complete the sentence with NO MORE THAN TWO WORDS from the passage: The library will open in ______.
   - Lựa chọn: (điền)
   - Đáp án: early September | September
   - Giải thích: "early September" (hai từ) hoặc "September" đều nằm trong giới hạn hai từ.
   - Gợi ý: Đề cho tối đa hai từ; chọn cụm chỉ thời gian trong bài.
3. Passage: "Visitors are advised to book tickets online to avoid long queues." Complete the sentence with ONE WORD from the passage: Booking online helps visitors avoid long ______.
   - Lựa chọn: (điền)
   - Đáp án: queues
   - Giải thích: Cần danh từ số nhiều "queues", giữ nguyên dạng trong bài.
   - Gợi ý: Từ cần điền đứng sau tính từ "long".
