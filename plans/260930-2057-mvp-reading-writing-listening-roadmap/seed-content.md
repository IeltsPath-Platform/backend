# Nội dung seed MVP (Reading, Writing, Listening)

- Nguồn: trang demo tương tác của lộ trình MVP (claude.ai, trích ngày 2026-10-01 bằng script, không chép tay). Agent code không cần mở trang demo: mọi nội dung seed nằm trong file này.
- Riêng đề cuối `TFNG_SKILLS` (mã `X5`, đoạn `garden`, câu TT1–TT3) **không có trong demo**, được soạn thêm vì plan 1640 yêu cầu topic TFNG có đề.
- File này là **dữ liệu**. Luật (bảng, cột, migration, test) nằm ở phase của từng plan; cột "Migration" chỉ nơi mỗi phần được seed.

## Quy ước chung

- **UUID cố định**, tự đặt theo prefix của plan: 1640 `20000000-0000-4000-8000-…`, 0737 `21000000-…`, 0812 `22000000-…`, 0851 `23000000-…`. Test tra theo `code`, không theo UUID.
- **KP mới:** `kind = STRATEGY`, `learning_type = PROCEDURE`, `status = ACTIVE` (quyết định 2026-10-01). KP1 dùng lại `DEMO_READING_MAIN_IDEA` có sẵn ở V4.
- **Câu hỏi:** `questions.status` và `question_versions.status` = `PUBLISHED`, `version_number = 1`, `schema_version = 1`, `difficulty = NULL` (gói luyện không gắn độ khó), `access_level = FREE`; cập nhật `questions.current_published_version_id`. `question_knowledge_points`: một KP mỗi câu, `weight = 1.00`.
- **`options`** theo dạng của V4: `[{"optionKey":"A","content":"…","sortOrder":1}, …]` theo thứ tự trong bảng. Câu `FILL_IN_BLANK`: `options = NULL`. Câu True/False/Not Given: `question_type = TRUE_FALSE_NOT_GIVEN`, `options` ghi đủ 3 lựa chọn như bảng (client biết loại ô trả lời nhờ `options`: `NULL` là ô điền).
- **`answer_spec`** theo `answer-spec-v1`: `CHOICE` so `optionKey`; `FILL` so sau chuẩn hóa (trim, gộp khoảng trắng, không phân biệt hoa thường).
- **Khối bài học:** `TEXT` lưu chữ thường (các đoạn cách một dòng trống; dòng "Mẹo:" giữ nguyên); `ASSET` trỏ asset `PASSAGE`/`AUDIO`; `EXERCISE` gồm câu theo thứ tự trong bảng (`sort_order` 1, 2, …). Mã khối (`L1-B2`…) chỉ để tham chiếu trong plan và test.
- **Gói luyện (`PRACTICE_SET`) và mã đề (`TOPIC_TEST`):** một package, version 1 `PUBLISHED`, một section chứa asset đoạn văn/audio và các câu (`max_score = 1`), `rules` không đặt giới hạn thời gian. `TOPIC_TEST` có `content_packages.topic_id`.
- **Bằng chứng mastery trong một khối** ghi theo `sort_order` của câu; số mastery dưới đây tính theo thứ tự đó.

## Topic và KP

| Topic | `code` | `sort_order` | Migration |
| --- | --- | --- | --- |
| Demo IELTS Reading | `DEMO_READING` | 900 | có sẵn (V4) |
| True / False / Not Given | `TFNG_SKILLS` | 910 | V9 (1640) |
| Demo IELTS Listening | `DEMO_LISTENING` | 920 | V12 (0851) |

| Mã | `code` | Tên (`name`) | Topic | `skill` | Migration |
| --- | --- | --- | --- | --- | --- |
| KP1 | `DEMO_READING_MAIN_IDEA` | Ý chính cả bài | `DEMO_READING` | READING | có sẵn (V4) |
| KP2 | `DR_IDEA_OR_DETAIL` | Ý chính hay chi tiết | `DEMO_READING` | READING | V9 |
| KP3 | `DR_TOPIC_SENTENCE` | Câu chủ đề | `DEMO_READING` | READING | V9 |
| KP4 | `DR_MATCHING_HEADINGS` | Chọn tiêu đề đoạn | `DEMO_READING` | READING | V9 |
| KP5 | `TFNG_FALSE_VS_NOT_GIVEN` | False hay Not Given | `TFNG_SKILLS` | READING | V9 |
| KP6 | `DEMO_READING_W1_CHART` | Mô tả biểu đồ (Task 1) | `DEMO_READING` | WRITING | V11 |
| KP7 | `DEMO_READING_W2_OPINION` | Luận quan điểm (Task 2) | `DEMO_READING` | WRITING | V10 |
| KP8 | `LS_NUM` | Nghe số, ngày, giờ, giá | `DEMO_LISTENING` | LISTENING | V12 |
| KP9 | `LS_SPELL` | Nghe đánh vần tên, mã | `DEMO_LISTENING` | LISTENING | V12 |
| KP10 | `LS_PARA` | Bắt ý qua paraphrase | `DEMO_LISTENING` | LISTENING | V12 |
| KP11 | `LS_TRAP` | Tránh bẫy đổi ý | `DEMO_LISTENING` | LISTENING | V12 |

`description` của KP: một câu ngắn theo tên (agent tự viết, không ảnh hưởng luật).

## Bài học

### L1 — Câu chủ đề nằm ở đâu

- Topic `DEMO_READING`, `sort_order` 1, `code` `L1`, migration V9 (1640). `lesson_knowledge_points`: KP3.
- Khối theo thứ tự:
  1. `TEXT`: Câu chủ đề (topic sentence) nêu ý mà cả đoạn triển khai. Trong bài IELTS nó thường là câu đầu đoạn, đôi khi là câu thứ hai sau một câu dẫn. Mẹo: thử bỏ câu đó đi. Nếu đoạn văn mất ý chung thì đó là câu chủ đề.
  2. `ASSET` đoạn văn `roofs`
  3. `EXERCISE` `L1-B2`: Q13
  4. `TEXT`: Luyện thêm với đoạn C và D.
  5. `EXERCISE` `L1-B5`: Q1, Q11, Q12

### L2 — Ý chính của cả bài

- Topic `DEMO_READING`, `sort_order` 2, `code` `L2`, migration V9 (1640). `lesson_knowledge_points`: KP1.
- Khối theo thứ tự:
  1. `TEXT`: Ý chính của cả bài là điều mọi đoạn cùng góp vào. Đọc câu chủ đề của từng đoạn rồi tìm điểm chung. Mẹo: Đáp án đúng thường khái quát; đáp án bẫy chỉ đúng với một đoạn.
  2. `ASSET` đoạn văn `roofs`
  3. `EXERCISE` `L2-B3`: Q5

### L3 — Ý chính hay chi tiết?

- Topic `DEMO_READING`, `sort_order` 3, `code` `L3`, migration V9 (1640). `lesson_knowledge_points`: KP2, KP6 (thêm ở V11).
- Khối theo thứ tự:
  1. `TEXT`: Ý chính là nhận định chung. Chi tiết là số liệu, ví dụ, tên riêng, kết quả nghiên cứu dùng để chứng minh. Mẹo: Hỏi: câu này trả lời "điều gì" hay "bằng chứng nào"?
  2. `ASSET` đoạn văn `roofs`
  3. `EXERCISE` `L3-B2`: Q3
  4. `TEXT` (thêm cùng khối essay, V11): Viết cũng vậy: Writing Task 1 mở bằng câu tổng quan (ý chính của biểu đồ), rồi mới đưa số liệu (chi tiết) để chứng minh.
  5. `EXERCISE` khối essay `L3-W1` (V11 (0812)), xem mục Writing

### L4 — Dạng Matching Headings

- Topic `DEMO_READING`, `sort_order` 4, `code` `L4`, migration V9 (1640). `lesson_knowledge_points`: KP4, KP1, KP7 (thêm ở V10).
- Khối theo thứ tự:
  1. `TEXT`: Tiêu đề phải khớp ý chính của cả đoạn, không phải một chi tiết trong đoạn. Tìm câu chủ đề trước, rồi chọn tiêu đề diễn đạt lại nó.
  2. `ASSET` đoạn văn `roofs`
  3. `EXERCISE` `L4-B3`: Q4
  4. `TEXT` (thêm cùng khối essay, V10): Dùng chính bài đọc về green roofs để viết một bài luận nêu quan điểm. Mỗi đoạn thân bài mở bằng một câu chủ đề, như bạn vừa luyện khi đọc.
  5. `EXERCISE` khối essay `L4-W2` (V10 (0737)), xem mục Writing

### TF1 — False hay Not Given?

- Topic `TFNG_SKILLS`, `sort_order` 1, `code` `TF1`, migration V9 (1640). `lesson_knowledge_points`: KP5.
- Khối theo thứ tự:
  1. `TEXT`: TRUE: đoạn văn nói giống. FALSE: nói ngược lại. NOT GIVEN: không nói tới. Mẹo: Bẫy hay gặp: chọn FALSE khi thông tin chỉ đơn giản là không có.
  2. `EXERCISE` `TF1-B2`: QT1

### LS1 — Điền form khi nghe gọi điện

- Topic `DEMO_LISTENING`, `sort_order` 1, `code` `LS1`, migration V12 (0851). `lesson_knowledge_points`: KP8, KP9.
- Khối theo thứ tự:
  1. `TEXT`: Dạng form completion: đọc trước các chỗ trống và đoán loại thông tin cần nghe (tên, ngày, số tiền, số điện thoại). Mẹo: Tên riêng thường được đánh vần từng chữ. Viết đúng chính tả: sai một chữ là sai cả câu.
  2. `ASSET` audio `ls1`
  3. `EXERCISE` `LS1-B3`: LQ1, LQ2, LQ3, LQ4

### LS2 — Bắt ý chính khi nghe nói

- Topic `DEMO_LISTENING`, `sort_order` 2, `code` `LS2`, migration V12 (0851). `lesson_knowledge_points`: KP10, KP11.
- Khối theo thứ tự:
  1. `TEXT`: Đáp án đúng hiếm khi lặp lại đúng từ trong bài nghe mà diễn đạt lại (paraphrase). Lựa chọn có từ giống hệt bài nghe thường là bẫy. Mẹo: Để ý câu phủ định kiểu "not to make money, but to…": ý chính nằm sau "but".
  2. `ASSET` audio `ls2`
  3. `EXERCISE` `LS2-B3`: LQ5, LQ6, LQ7

## Câu hỏi bài học

Reading (V9):

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| Q13 | KP3 | MULTIPLE_CHOICE | Which sentence is the topic sentence of paragraph B? | A: The most important benefit of green roofs is that they keep buildings cool.<br>B: On a summer afternoon, a conventional black roof can reach 80°C…<br>C: As a result, the floors below need far less air conditioning. | `{"type":"CHOICE","correct":"A"}` | Câu A nêu ý của cả đoạn (làm mát). Hai câu sau là số liệu và hệ quả để chứng minh. |
| Q1 | KP3 | MULTIPLE_CHOICE | Which sentence is the topic sentence of paragraph C? | A: Green roofs also manage rainwater.<br>B: The soil soaks up much of a heavy shower and releases it slowly…<br>C: In Copenhagen, new flat roofs must now be planted for this reason. | `{"type":"CHOICE","correct":"A"}` | "Green roofs also manage rainwater" nêu chủ đề. Câu B giải thích cách làm, câu C là ví dụ. |
| Q11 | KP3 | MULTIPLE_CHOICE | Which sentence tells you what paragraph D is about? | A: Not everyone is convinced.<br>B: Critics point out that green roofs are expensive to install and need regular care…<br>C: …many older buildings are not strong enough to carry the extra weight. | `{"type":"CHOICE","correct":"A"}` | "Not everyone is convinced" báo trước cả đoạn nói về ý kiến phản đối. Câu B chỉ là một lý do cụ thể. |
| Q12 | KP3 | FILL_IN_BLANK | Complete with ONE WORD from paragraph D: people who doubt green roofs are called ______. | NULL | `{"type":"FILL","accepted":["critics"]}` | Đoạn D: "Critics point out that…". Không phân biệt hoa thường. |
| Q5 | KP1 | MULTIPLE_CHOICE | What is the main idea of the whole passage? | A: Black roofs get much hotter than planted roofs.<br>B: Green roofs bring cities real benefits but also have drawbacks.<br>C: Copenhagen requires new flat roofs to be planted. | `{"type":"CHOICE","correct":"B"}` | Bài nói lợi ích (làm mát, thoát nước) và nhược điểm (đoạn D). A và C chỉ đúng với một chi tiết. |
| Q3 | KP2 | MULTIPLE_CHOICE | In paragraph B, "a conventional black roof can reach 80°C" is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"DETAIL"}` | Con số 80°C là bằng chứng cho ý chính "green roofs keep buildings cool". |
| Q4 | KP4 | MULTIPLE_CHOICE | Choose the best heading for paragraph C. | i: Cooling from above<br>ii: Where the rain goes<br>iii: Too heavy for old buildings | `{"type":"CHOICE","correct":"ii"}` | Đoạn C nói về nước mưa. Heading i hợp đoạn B, iii hợp đoạn D. |
| QT1 | KP5 | TRUE_FALSE_NOT_GIVEN | Passage: "The museum opens at 9 a.m. on weekdays." Statement: "The museum opens at 9 a.m. on Saturdays." | TRUE: TRUE<br>FALSE: FALSE<br>NOT_GIVEN: NOT GIVEN | `{"type":"CHOICE","correct":"NOT_GIVEN"}` | Đoạn văn chỉ nói ngày thường, không nói gì về thứ Bảy: NOT GIVEN. |

Listening (V12):

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| LQ1 | KP9 | FILL_IN_BLANK | Surname: ______ | NULL | `{"type":"FILL","accepted":["thompson"]}` | Người gọi đánh vần T-H-O-M-P-S-O-N. |
| LQ2 | KP8 | FILL_IN_BLANK | Date: Friday ______ May | NULL | `{"type":"FILL","accepted":["15","15th","fifteenth","the 15th","the fifteenth"]}` | "Friday the fifteenth of May". |
| LQ3 | KP8 | FILL_IN_BLANK | Cost for three hours: £______ | NULL | `{"type":"FILL","accepted":["12","twelve"]}` | "costs twelve pounds for three hours". |
| LQ4 | KP8 | FILL_IN_BLANK | Contact number: ______ | NULL | `{"type":"FILL","accepted":["07700900314","07700 900314","07700 900 314"]}` | "0 7 7 0 0, 9 0 0, 3 1 4". |
| LQ5 | KP10 | MULTIPLE_CHOICE | What is the main purpose of the scheme? | A: To raise money for the council<br>B: To reduce short car trips in the centre<br>C: To attract more tourists | `{"type":"CHOICE","correct":"B"}` | "not trying to make money… cut the number of short car journeys" được diễn đạt lại thành B. |
| LQ6 | KP11 | MULTIPLE_CHOICE | What will the council do because of safety concerns? | A: Limit each ride to thirty minutes<br>B: Build protected lanes by the river<br>C: Close some streets in the centre | `{"type":"CHOICE","correct":"B"}` | "protected cycle lanes along the river". Ba mươi phút là bẫy: đó là thời gian miễn phí, không liên quan an toàn. |
| LQ7 | KP10 | MULTIPLE_CHOICE | What may happen after the first year? | A: The scheme will end<br>B: Riders will start paying<br>C: The scheme will cover outer areas of the city | `{"type":"CHOICE","correct":"C"}` | "extended to the suburbs" = cover outer areas. |

## Đoạn văn Reading

**Đoạn văn `roofs` — Green roofs** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. Across Europe and North America, city planners are turning to green roofs to cope with hotter summers. A green roof is a layer of soil and plants laid over a waterproof membrane on top of a building.
>
> B. The most important benefit of green roofs is that they keep buildings cool. On a summer afternoon, a conventional black roof can reach 80°C, while a planted roof nearby rarely rises above 30°C. As a result, the floors below need far less air conditioning.
>
> C. Green roofs also manage rainwater. The soil soaks up much of a heavy shower and releases it slowly, which takes pressure off city drains. In Copenhagen, new flat roofs must now be planted for this reason.
>
> D. Not everyone is convinced. Critics point out that green roofs are expensive to install and need regular care, and that many older buildings are not strong enough to carry the extra weight.
>

**Đoạn văn `trees` — Street trees** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. City trees do more than make streets look pleasant. They are one of the cheapest ways to improve life in a crowded city.
>
> B. Trees filter the air. Their leaves trap fine dust from traffic, and a single mature oak can remove several kilograms of pollutants a year.
>
> C. Trees also calm people. In a 2019 study in Toronto, residents of tree-lined streets reported lower stress than people living just two blocks away.
>

**Đoạn văn `plastic` — Recycling plastic** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. Recycling plastic is harder than most people think. Only about 9% of all plastic ever made has been recycled.
>
> B. One reason is that there are many types of plastic, and most recycling plants can process only a few. Yoghurt pots and drink bottles, for example, often need different machines.
>
> C. New chemical methods may help. They break plastic down into its basic building blocks, which can then be made into new plastic of high quality.
>

**Đoạn văn `garden` — Hillside community garden** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. The Hillside community garden opened in 2015 on land that had been an empty car park.
>
> B. Members pay a small yearly fee and can borrow the tools that are kept in the shed.
>
> C. The garden is open from 7 a.m. to 8 p.m. between April and September.
>

**Đoạn văn `bees` — Bees in the city** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. Beekeeping is no longer only a country pursuit. In London, Paris and New York, thousands of hives now sit on rooftops and in backyards, kept by office workers, schools and even hotels.
>
> B. Supporters say city bees do well because parks and gardens offer a wide variety of flowers throughout the year. Honey from urban hives often wins prizes for its complex flavour.
>
> C. However, scientists warn that too many hives can harm wild bees. When honeybees are crowded into a small area, they compete with native species for the same limited flowers.
>

**Đoạn văn `week` — The four-day week** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. Several companies have tested a four-day working week, paying staff the same salary for fewer hours.
>
> B. In most trials, managers reported that productivity stayed the same or even rose, while employees said they felt less stressed and took fewer sick days.
>
> C. Critics, however, point out that the model suits office work better than hospitals or factories, where someone must be present every day.
>

**Đoạn văn `sleep` — Sleep and memory** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. Sleep plays a key role in learning. During deep sleep, the brain replays what happened during the day and strengthens important memories.
>
> B. In one experiment, students who slept after studying a list of words remembered 20% more of them the next morning than students who stayed awake.
>
> C. Short naps can help too. A nap of just 30 minutes improved performance on a drawing task in a 2018 study.
>

**Đoạn văn `bikes` — Cycling in Copenhagen** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. Copenhagen has become one of the easiest cities in the world to cycle in. Over 60% of residents ride to work or school every day.
>
> B. The city invested heavily in safety. Its cycle lanes are separated from traffic by a raised kerb, and some bridges are built only for bicycles.
>
> C. Cycling also saves money. Officials estimate that every kilometre cycled saves the health system about one euro.
>

**Đoạn văn `desert` — Desert plants** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. Desert plants have developed clever ways to survive with little water. Many store water in thick stems. Others have tiny leaves that lose less moisture.
>
> B. Some plants avoid the dry season altogether. Their seeds lie in the sand for years and sprout only after heavy rain. The plants then flower and produce new seeds within a few weeks.
>
> C. Roots matter as much as leaves. The mesquite tree sends roots over 50 metres deep to reach underground water. Cacti, in contrast, spread shallow roots wide to catch light rain.
>
> D. Animals benefit from these plants. Birds nest in tall cacti, and lizards shelter under desert shrubs during the hottest hours.
>

**Đoạn văn `coffee` — The story of coffee** (asset `PASSAGE`, `text_content` là các đoạn cách nhau một dòng trống, theo thứ tự A, B, C…):

> A. Coffee was first drunk in Yemen in the 15th century, and from there it spread to Turkey, Europe and the Americas.
>
> B. Today coffee is one of the world's most traded products. Brazil alone produces about a third of the global supply.
>
> C. Climate change threatens coffee farms. Rising temperatures and new plant diseases could halve the land suitable for growing coffee by 2050.
>
> D. Researchers are testing wild coffee species that survive heat better, hoping to protect future harvests.
>

`roofs` là asset mới của V9 (khác đoạn văn V6 mà tutor đang dùng; không sửa V6).

## Mã đề cuối (`TOPIC_TEST`)

### Mã A (`X1`) — topic `DEMO_READING`, đoạn văn `trees`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| Q2 | KP1 | MULTIPLE_CHOICE | What is the passage mainly about? | A: How oak trees grow in cities<br>B: The ways street trees improve city life<br>C: A 2019 study of stress in Toronto | `{"type":"CHOICE","correct":"B"}` | Cả ba đoạn cùng nói lợi ích của cây đường phố. |
| Q14 | KP3 | MULTIPLE_CHOICE | Which sentence is the topic sentence of paragraph B? | A: Trees filter the air.<br>B: Their leaves trap fine dust from traffic…<br>C: …a single mature oak can remove several kilograms of pollutants a year. | `{"type":"CHOICE","correct":"A"}` | "Trees filter the air." là ý khái quát; hai câu sau là cách lọc và số liệu. |
| Q15 | KP2 | MULTIPLE_CHOICE | In paragraph C, "residents of tree-lined streets reported lower stress" is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"DETAIL"}` | Kết quả nghiên cứu ở Toronto là bằng chứng cho ý "Trees also calm people". |
| Q16 | KP4 | MULTIPLE_CHOICE | Choose the best heading for paragraph C. | i: Cleaner air<br>ii: A calmer mind<br>iii: The cost of planting | `{"type":"CHOICE","correct":"ii"}` | Đoạn C nói cây giúp giảm căng thẳng. |

### Mã B (`X2`) — topic `DEMO_READING`, đoạn văn `plastic`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| PL1 | KP1 | MULTIPLE_CHOICE | What is the passage mainly about? | A: Why plastic recycling is difficult and how it may improve<br>B: How yoghurt pots are made<br>C: The history of plastic | `{"type":"CHOICE","correct":"A"}` | Đoạn A–B nói vì sao khó, đoạn C nói cách cải thiện. |
| PL2 | KP3 | MULTIPLE_CHOICE | Which sentence is the topic sentence of paragraph B? | A: One reason is that there are many types of plastic, and most recycling plants can process only a few.<br>B: Yoghurt pots and drink bottles, for example, often need different machines. | `{"type":"CHOICE","correct":"A"}` | Câu B mở đầu bằng "for example", tức là ví dụ cho câu A. |
| PL3 | KP2 | MULTIPLE_CHOICE | "Only about 9% of all plastic ever made has been recycled" is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"DETAIL"}` | Con số 9% chứng minh ý "Recycling plastic is harder than most people think". |
| PL4 | KP4 | MULTIPLE_CHOICE | Choose the best heading for paragraph C. | i: Too many kinds<br>ii: A chemical solution<br>iii: Plastic in the ocean | `{"type":"CHOICE","correct":"ii"}` | Đoạn C nói phương pháp hóa học mới. |

### Mã TFNG (`X5`) — topic `TFNG_SKILLS`, đoạn văn `garden`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| TT1 | KP5 | TRUE_FALSE_NOT_GIVEN | Statement: "The garden was built on land that used to be a car park." | TRUE: TRUE<br>FALSE: FALSE<br>NOT_GIVEN: NOT GIVEN | `{"type":"CHOICE","correct":"TRUE"}` | Đoạn A: "on land that had been an empty car park". |
| TT2 | KP5 | TRUE_FALSE_NOT_GIVEN | Statement: "Members have to bring their own tools." | TRUE: TRUE<br>FALSE: FALSE<br>NOT_GIVEN: NOT GIVEN | `{"type":"CHOICE","correct":"FALSE"}` | Đoạn B nói ngược lại: thành viên mượn dụng cụ để trong kho. |
| TT3 | KP5 | TRUE_FALSE_NOT_GIVEN | Statement: "Most members live within walking distance of the garden." | TRUE: TRUE<br>FALSE: FALSE<br>NOT_GIVEN: NOT GIVEN | `{"type":"CHOICE","correct":"NOT_GIVEN"}` | Đoạn văn không nói thành viên sống ở đâu. |

### Mã C (`X3`) — topic `DEMO_LISTENING`, audio `hotel`, V12 (0851)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| HT1 | KP9 | FILL_IN_BLANK | Surname: ______ | NULL | `{"type":"FILL","accepted":["delaney"]}` | D-E-L-A-N-E-Y. |
| HT2 | KP8 | FILL_IN_BLANK | Number of nights: ______ | NULL | `{"type":"FILL","accepted":["4","four"]}` | "Four nights". |
| HT3 | KP10 | MULTIPLE_CHOICE | Why did the guest choose this hotel? | A: It is the cheapest near the station<br>B: Someone she works with recommended it<br>C: She saw an online advert | `{"type":"CHOICE","correct":"B"}` | "A colleague stayed here" = someone she works with. |
| HT4 | KP11 | MULTIPLE_CHOICE | What must guests pay extra for? | A: Breakfast<br>B: Parking<br>C: Wi-Fi | `{"type":"CHOICE","correct":"B"}` | Bẫy: bữa sáng được nhắc trước nhưng đã gồm trong giá; "parking is charged separately". |

### Mã D (`X4`) — topic `DEMO_LISTENING`, audio `tour`, V12 (0851)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| TR1 | KP8 | FILL_IN_BLANK | Start time: ______ | NULL | `{"type":"FILL","accepted":["10.30","10:30","ten thirty","half past ten"]}` | Bẫy: trước đây là mười giờ, nay "ten thirty". |
| TR2 | KP9 | FILL_IN_BLANK | Guide's name: Ms ______ | NULL | `{"type":"FILL","accepted":["fenn"]}` | F-E-N-N. |
| TR3 | KP10 | MULTIPLE_CHOICE | What is the tour mainly about? | A: Famous buildings in the old town<br>B: The town's history of making and selling wool<br>C: Shopping in the market | `{"type":"CHOICE","correct":"B"}` | "history of the old wool trade rather than famous buildings". |
| TR4 | KP11 | MULTIPLE_CHOICE | What can people bring on the tour? | A: Large bags<br>B: A small backpack<br>C: Nothing at all | `{"type":"CHOICE","correct":"B"}` | "do not bring large bags. A small backpack is fine." Large bags là bẫy vì được nhắc tới đầu tiên. |

## Gói luyện (`PRACTICE_SET`)

Listening chỉ seed **1 gói mỗi KP** (quyết định 2026-10-01), lấy các gói mức vừa của demo; gói không gắn `difficulty`.

### `PS-KP1-A` — KP1, đoạn văn `bees`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| BE1 | KP1 | MULTIPLE_CHOICE | What is the passage mainly about? | A: City honey tastes better than country honey<br>B: Urban beekeeping is growing, with benefits and risks<br>C: Wild bees are disappearing from London | `{"type":"CHOICE","correct":"B"}` | Đoạn A: đang phát triển; B: lợi ích; C: rủi ro. |
| BE2 | KP1 | MULTIPLE_CHOICE | What is the main idea of paragraph B? | A: Why city bees can do well<br>B: How to win a honey prize<br>C: Where parks are located | `{"type":"CHOICE","correct":"A"}` | Cả đoạn giải thích vì sao ong thành phố phát triển tốt. |
| BE3 | KP1 | MULTIPLE_CHOICE | What is the main idea of paragraph C? | A: A possible downside of city hives<br>B: How scientists count bees<br>C: Why flowers are limited | `{"type":"CHOICE","correct":"A"}` | "However… can harm wild bees" là mặt trái. |
| BE4 | KP1 | MULTIPLE_CHOICE | Which title best fits the whole passage? | A: Buzz in the City: Promise and Problems<br>B: A Guide to Building Hives<br>C: The History of Honey | `{"type":"CHOICE","correct":"A"}` | Tiêu đề phải bao cả lợi ích lẫn rủi ro. |

### `PS-KP1-B` — KP1, đoạn văn `week`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| WW1 | KP1 | MULTIPLE_CHOICE | What is the passage mainly about? | A: Four-day weeks save companies money<br>B: Four-day week trials show gains but may not suit every job<br>C: Hospitals need more staff | `{"type":"CHOICE","correct":"B"}` | Đoạn B: kết quả tốt; đoạn C: giới hạn. |
| WW2 | KP1 | MULTIPLE_CHOICE | What is the main idea of paragraph B? | A: Positive results of the trials<br>B: How sick days are counted<br>C: Why managers dislike change | `{"type":"CHOICE","correct":"A"}` | Năng suất giữ nguyên hoặc tăng, nhân viên bớt căng thẳng. |
| WW3 | KP1 | MULTIPLE_CHOICE | What is the main idea of paragraph C? | A: Limits of the four-day model<br>B: How factories are designed<br>C: Salaries in hospitals | `{"type":"CHOICE","correct":"A"}` | Mô hình hợp văn phòng hơn bệnh viện, nhà máy. |
| WW4 | KP1 | MULTIPLE_CHOICE | Which title best fits the whole passage? | A: Less Time, Same Work?<br>B: Factory Life Today<br>C: A History of the Weekend | `{"type":"CHOICE","correct":"A"}` | Bao được cả thử nghiệm lẫn câu hỏi còn mở. |

### `PS-KP2-A` — KP2, đoạn văn `sleep`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| SL1 | KP2 | MULTIPLE_CHOICE | "Sleep plays a key role in learning." is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"MAIN_IDEA"}` | Nhận định chung, cả đoạn A triển khai nó. |
| SL2 | KP2 | MULTIPLE_CHOICE | "students who slept… remembered 20% more" is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"DETAIL"}` | Kết quả thí nghiệm, dùng làm bằng chứng. |
| SL3 | KP2 | MULTIPLE_CHOICE | "Short naps can help too." is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"MAIN_IDEA"}` | Nhận định chung của đoạn C. |
| SL4 | KP2 | MULTIPLE_CHOICE | "A nap of just 30 minutes improved performance on a drawing task" is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"DETAIL"}` | Kết quả nghiên cứu cụ thể năm 2018. |

### `PS-KP2-B` — KP2, đoạn văn `bikes`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| CB1 | KP2 | MULTIPLE_CHOICE | "Copenhagen has become one of the easiest cities in the world to cycle in." is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"MAIN_IDEA"}` | Nhận định chung của đoạn A. |
| CB2 | KP2 | MULTIPLE_CHOICE | "Over 60% of residents ride to work or school every day." is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"DETAIL"}` | Số liệu minh họa. |
| CB3 | KP2 | MULTIPLE_CHOICE | "some bridges are built only for bicycles" is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"DETAIL"}` | Ví dụ cho việc đầu tư an toàn. |
| CB4 | KP2 | MULTIPLE_CHOICE | "Cycling also saves money." is… | MAIN_IDEA: a main idea<br>DETAIL: a supporting detail | `{"type":"CHOICE","correct":"MAIN_IDEA"}` | Nhận định chung của đoạn C. |

### `PS-KP3-A` — KP3, đoạn văn `desert`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| DS1 | KP3 | MULTIPLE_CHOICE | Which sentence is the topic sentence of paragraph A? | A: Desert plants have developed clever ways to survive with little water.<br>B: Many store water in thick stems.<br>C: Others have tiny leaves that lose less moisture. | `{"type":"CHOICE","correct":"A"}` | Hai câu sau là hai "cách" cụ thể. |
| DS2 | KP3 | MULTIPLE_CHOICE | Which sentence is the topic sentence of paragraph B? | A: Some plants avoid the dry season altogether.<br>B: Their seeds lie in the sand for years…<br>C: The plants then flower and produce new seeds… | `{"type":"CHOICE","correct":"A"}` | Các câu sau mô tả cách "tránh mùa khô". |
| DS3 | KP3 | MULTIPLE_CHOICE | Which sentence is the topic sentence of paragraph C? | A: Roots matter as much as leaves.<br>B: The mesquite tree sends roots over 50 metres deep…<br>C: Cacti, in contrast, spread shallow roots wide… | `{"type":"CHOICE","correct":"A"}` | Hai câu sau là hai ví dụ về rễ. |
| DS4 | KP3 | MULTIPLE_CHOICE | Which sentence is the topic sentence of paragraph D? | A: Animals benefit from these plants.<br>B: Birds nest in tall cacti…<br>C: …lizards shelter under desert shrubs… | `{"type":"CHOICE","correct":"A"}` | Chim và thằn lằn là ví dụ. |

### `PS-KP4-A` — KP4, đoạn văn `coffee`, V9 (1640)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| CF1 | KP4 | MULTIPLE_CHOICE | Choose the heading for paragraph A. | i: A drink that travelled the world<br>ii: A giant global trade<br>iii: A warming threat<br>iv: Searching for tougher plants<br>v: How to brew the perfect cup | `{"type":"CHOICE","correct":"i"}` | Đoạn A kể cà phê lan từ Yemen ra thế giới. |
| CF2 | KP4 | MULTIPLE_CHOICE | Choose the heading for paragraph B. | i: A drink that travelled the world<br>ii: A giant global trade<br>iii: A warming threat<br>iv: Searching for tougher plants<br>v: How to brew the perfect cup | `{"type":"CHOICE","correct":"ii"}` | Đoạn B nói cà phê là mặt hàng giao dịch lớn. |
| CF3 | KP4 | MULTIPLE_CHOICE | Choose the heading for paragraph C. | i: A drink that travelled the world<br>ii: A giant global trade<br>iii: A warming threat<br>iv: Searching for tougher plants<br>v: How to brew the perfect cup | `{"type":"CHOICE","correct":"iii"}` | Đoạn C nói biến đổi khí hậu đe dọa. |
| CF4 | KP4 | MULTIPLE_CHOICE | Choose the heading for paragraph D. | i: A drink that travelled the world<br>ii: A giant global trade<br>iii: A warming threat<br>iv: Searching for tougher plants<br>v: How to brew the perfect cup | `{"type":"CHOICE","correct":"iv"}` | Đoạn D nói thử giống chịu nóng. |

### `PS-NUM` — KP8, audio `numM`, V12 (0851)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| NM1 | KP8 | FILL_IN_BLANK | Showing time: ______ | NULL | `{"type":"FILL","accepted":["9.30","9:30","nine thirty","half past nine"]}` | Bẫy: suất bảy giờ đã hết vé, nên là "nine thirty". |
| NM2 | KP8 | FILL_IN_BLANK | Total price: £______ | NULL | `{"type":"FILL","accepted":["18.50","18.5"]}` | "eighteen pounds fifty altogether". |
| NM3 | KP8 | FILL_IN_BLANK | First seat number: ______ | NULL | `{"type":"FILL","accepted":["12","twelve"]}` | "seats twelve and thirteen". |

### `PS-SPELL` — KP9, audio `spellM`, V12 (0851)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| SM1 | KP9 | FILL_IN_BLANK | Surname: ______ | NULL | `{"type":"FILL","accepted":["okafor"]}` | O-K-A-F-O-R. |
| SM2 | KP9 | FILL_IN_BLANK | Company: ______ | NULL | `{"type":"FILL","accepted":["brightwell"]}` | B-R-I-G-H-T-W-E-L-L, hai chữ L ở cuối. |
| SM3 | KP9 | FILL_IN_BLANK | Email username: ______ | NULL | `{"type":"FILL","accepted":["jaye88"]}` | Bẫy: người gọi sửa J-A-Y thành J-A-Y-E, rồi số 88. |

### `PS-PARA` — KP10, audio `museum`, V12 (0851)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| MU1 | KP10 | MULTIPLE_CHOICE | Why did the museum close? | A: To build a new cafe<br>B: Damp was harming the building and objects<br>C: Too few people were visiting | `{"type":"CHOICE","correct":"B"}` | "serious problems with damp… the main reason for closing". |
| MU2 | KP10 | MULTIPLE_CHOICE | What can visitors see for the first time? | A: Ship models that were stored away before<br>B: A new harbour exhibition<br>C: The museum roof garden | `{"type":"CHOICE","correct":"A"}` | "used to be kept in storage" = stored away before. |
| MU4 | KP10 | MULTIPLE_CHOICE | What does the museum plan for next year? | A: A cafe on the roof<br>B: More repairs to the roof<br>C: A second building | `{"type":"CHOICE","correct":"A"}` | "a small cafe on the roof next spring". |

### `PS-TRAP` — KP11, audio `trapM`, V12 (0851)

| Mã câu | KP | Loại | Đề (`stem`) | `options` (key: nội dung) | `answer_spec` | `explanation` |
| --- | --- | --- | --- | --- | --- | --- |
| TM1 | KP11 | MULTIPLE_CHOICE | When will they meet? | A: Thursday<br>B: Friday<br>C: Saturday | `{"type":"CHOICE","correct":"B"}` | Thursday được nhắc trước rồi bị đổi: "Could we make it Friday instead?" |
| TM2 | KP11 | MULTIPLE_CHOICE | Where will they meet? | A: At the station<br>B: In the library<br>C: At a cafe | `{"type":"CHOICE","correct":"C"}` | Station bị đổi thành "the cafe next to the library". Library là bẫy. |
| TM3 | KP11 | MULTIPLE_CHOICE | What will Anna bring? | A: Sandwiches<br>B: The map<br>C: Lunch for both | `{"type":"CHOICE","correct":"B"}` | Sandwiches bị gạt đi: "No need… Just bring the map." |

## Audio Listening (V12, 8 file mp3)

File mp3 thu theo đúng transcript; không commit mp3 vào git. Key trong DB là `listening/demo/<ref>.mp3`.

**Audio `ls1` — Đặt phòng học ở thư viện** (asset `AUDIO`, `media_reference = listening/demo/ls1.mp3`, `durationSeconds = 45`; `text_content` là transcript dưới đây, mỗi lượt một dòng `Người nói: câu`):

> Librarian: Good morning, Riverside Library. How can I help you?
>
> Caller: Hi. I would like to book a study room for my group.
>
> Librarian: Of course. Can I have your surname, please?
>
> Caller: Yes, it is Thompson. T, H, O, M, P, S, O, N.
>
> Librarian: Thank you. Which day would you like the room?
>
> Caller: Friday the fifteenth of May, in the afternoon, please.
>
> Librarian: Fine. A room for up to six people costs twelve pounds for three hours.
>
> Caller: Twelve pounds is fine.
>
> Librarian: And could I have a contact number?
>
> Caller: Sure. It is 0 7 7 0 0, 9 0 0, 3 1 4.
>

**Audio `ls2` — Chương trình xe đạp công cộng** (asset `AUDIO`, `media_reference = listening/demo/ls2.mp3`, `durationSeconds = 50`; `text_content` là transcript dưới đây, mỗi lượt một dòng `Người nói: câu`):

> Officer: Good evening, everyone. Tonight I want to explain the new bike-sharing scheme, which starts next month.
>
> Officer: The council is not trying to make money from it. The main aim is to cut the number of short car journeys in the city centre, because almost half of all car trips here are under three kilometres.
>
> Officer: Bikes will be available at forty stations, and the first thirty minutes of every ride will be free.
>
> Officer: Some residents are worried about safety, so we are also building protected cycle lanes along the river.
>
> Officer: Finally, the scheme will be reviewed after one year. If it works well, it will be extended to the suburbs.
>

**Audio `numM` — Mua vé xem phim** (asset `AUDIO`, `media_reference = listening/demo/numM.mp3`, `durationSeconds = 30`; `text_content` là transcript dưới đây, mỗi lượt một dòng `Người nói: câu`):

> Customer: Hi, two tickets for the seven o'clock film on Saturday, please.
>
> Clerk: Sorry, seven o'clock is sold out. There is another showing at nine thirty.
>
> Customer: OK, nine thirty then. How much is that?
>
> Clerk: Two adults, that is eighteen pounds fifty altogether.
>
> Clerk: You are in row F, seats twelve and thirteen.
>

**Audio `spellM` — Đăng ký phòng tập** (asset `AUDIO`, `media_reference = listening/demo/spellM.mp3`, `durationSeconds = 35`; `text_content` là transcript dưới đây, mỗi lượt một dòng `Người nói: câu`):

> Receptionist: City Fitness, how can I help?
>
> Caller: I want to join. My surname is Okafor, O, K, A, F, O, R.
>
> Receptionist: And the company you work for?
>
> Caller: Brightwell. B, R, I, G, H, T, W, E, L, L. Two Ls at the end.
>
> Receptionist: Great. And your email username?
>
> Caller: It is J, A, Y... sorry, J, A, Y, E, then the number eighty-eight.
>

**Audio `museum` — Bảo tàng mở cửa lại** (asset `AUDIO`, `media_reference = listening/demo/museum.mp3`, `durationSeconds = 50`; `text_content` là transcript dưới đây, mỗi lượt một dòng `Người nói: câu`):

> Guide: Welcome back to the Harbour Museum, which reopened last week after two years of repairs.
>
> Guide: The old building had serious problems with damp, and many objects could not be shown safely. That was the main reason for closing.
>
> Guide: Now, for the first time, visitors can also see the collection of ship models, which used to be kept in storage.
>
> Guide: Entry is still free, but we ask visitors to book a time online at weekends, when it gets very busy.
>
> Guide: We also hope to open a small cafe on the roof next spring.
>

**Audio `trapM` — Hẹn gặp lên kế hoạch** (asset `AUDIO`, `media_reference = listening/demo/trapM.mp3`, `durationSeconds = 30`; `text_content` là transcript dưới đây, mỗi lượt một dòng `Người nói: câu`):

> Anna: Shall we meet on Thursday to plan the trip?
>
> Ben: Thursday is difficult for me. Could we make it Friday instead?
>
> Anna: Friday is fine. Let us meet at the station.
>
> Ben: Actually, the cafe next to the library is quieter. Let us go there.
>
> Anna: Good idea. I will bring some sandwiches.
>
> Ben: No need, the cafe does lunch. Just bring the map.
>

**Audio `hotel` — Đặt phòng khách sạn** (asset `AUDIO`, `media_reference = listening/demo/hotel.mp3`, `durationSeconds = 45`; `text_content` là transcript dưới đây, mỗi lượt một dòng `Người nói: câu`):

> Receptionist: Good evening, Harbour View Hotel.
>
> Guest: Hello. I would like to book a room. My name is Delaney. D, E, L, A, N, E, Y.
>
> Receptionist: Thank you, Ms Delaney. How many nights?
>
> Guest: Four nights, from the twelfth of July.
>
> Receptionist: Certainly. Can I ask how you heard about us?
>
> Guest: A colleague stayed here last year. She said the rooms were quiet even though the hotel is near the station.
>
> Receptionist: That is good to hear. Breakfast is included, but parking is charged separately.
>

**Audio `tour` — Tour đi bộ quanh phố cổ** (asset `AUDIO`, `media_reference = listening/demo/tour.mp3`, `durationSeconds = 40`; `text_content` là transcript dưới đây, mỗi lượt một dòng `Người nói: câu`):

> Guide: Hello and welcome. The walking tour used to start at ten, but it now leaves at ten thirty from the fountain in Market Square.
>
> Guide: My name is Ms Fenn, that is F, E, N, N, and I will be your guide for about two hours.
>
> Guide: We focus on the history of the old wool trade rather than on famous buildings, so you will visit several workshops that most tourists never see.
>
> Guide: Please do not bring large bags. A small backpack is fine.
>

## Writing

### Khối essay `L3-W1` — TASK_1, KP6 (`DEMO_READING_W1_CHART`), V11 (0812)

- Câu `ESSAY`, `skill = WRITING`, `options = NULL`.
- `stem`: The bar chart below shows the average afternoon surface temperature of three types of roof in three cities in July. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 150 words.
- `answer_spec`: `{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":6.0,"chartFacts":"Average afternoon roof surface temperature in July (°C). Madrid: black roof 82, white-painted roof 45, green (planted) roof 33. Chicago: black roof 74, white-painted roof 41, green (planted) roof 30. Copenhagen: black roof 61, white-painted roof 35, green (planted) roof 25. Gap between black and green roofs: Madrid 49, Chicago 44, Copenhagen 36. Key features: black roofs hottest and green roofs coolest in every city; Madrid hottest and Copenhagen coolest for every roof type; white roofs about 45% cooler than black roofs in each city."}`
- Ảnh: asset `IMAGE`, `media_reference` là data URI SVG biểu đồ cột (≤ 8 KB) vẽ từ bảng số dưới; `text_content` (alt text): "Bar chart of July afternoon roof temperatures for black, white and green roofs in Madrid, Chicago and Copenhagen." Gắn qua `content_asset_links.question_version_id`.

  | Thành phố | Black roof | White-painted roof | Green (planted) roof |
  | --- | --- | --- | --- |
  | Madrid | 82 | 45 | 33 |
  | Chicago | 74 | 41 | 30 |
  | Copenhagen | 61 | 35 | 25 |

- `explanation` (bài mẫu, chỉ trả khi khối đạt):

  > The chart compares how hot three kinds of roof became on July afternoons in Madrid, Chicago and Copenhagen.
  >
  > It is clear that the type of roof mattered far more than the location. In every city, black roofs were the hottest and planted roofs the coolest, with white roofs in between. Madrid was the hottest city for every roof type, and Copenhagen the mildest.
  >
  > Black roofs peaked at 82°C in Madrid and reached 74°C in Chicago and 61°C in Copenhagen. Painting a roof white cut these figures to 45°C, 41°C and 35°C respectively, a fall of roughly 45 per cent in each case.
  >
  > Green roofs performed best, staying at 33°C in Madrid, 30°C in Chicago and just 25°C in Copenhagen. The advantage of planting over black roofing was therefore greatest in Madrid, at 49°C, compared with 44°C in Chicago and 36°C in Copenhagen.
  >

- Bài dùng cho test và E2E (không seed vào DB): bài yếu nên ra band dưới `passBand`, bài tốt nên đạt. Test tự động dùng LLM giả nên chỉ cần số từ.

  Bài yếu:

  > The chart show temperature of roofs in Madrid, Chicago and Copenhagen. In Madrid the black roof is 82 degrees and it is the most hot. The white roof is 45 and the green roof is 33. In Chicago the black roof is 74 and the green one is 30. In Copenhagen the black roof is 61. Also the white roof in Copenhagen is 40 degrees, which is lower than Chicago. i think green roofs is more better because they are cool and peoples can save money for air conditioner. Alot of cities should use them.
  >

  Bài tốt:

  > The bar chart compares the average afternoon surface temperature of black, white and green roofs in Madrid, Chicago and Copenhagen in July.
  >
  > Overall, black roofs were by far the hottest in all three cities, whereas planted roofs remained the coolest. Madrid recorded the highest temperatures for every type of roof, while Copenhagen recorded the lowest.
  >
  > Black roofs reached 82°C in Madrid, which was considerably higher than the figures for Chicago (74°C) and Copenhagen (61°C). White roofs were substantially cooler, at 45°C, 41°C and 35°C respectively, so painting a roof white reduced its temperature by roughly half in each city.
  >
  > Green roofs stayed between 25°C and 33°C, making them the coolest option everywhere. As a result, the gap between black and green roofs was largest in Madrid, at 49°C, and smallest in Copenhagen, at 36°C. In contrast, the difference between white and green roofs was relatively small in each city, although it was slightly wider in Madrid than in the other two cities.
  >

### Khối essay `L4-W2` — TASK_2, KP7 (`DEMO_READING_W2_OPINION`), V10 (0737)

- Câu `ESSAY`, `skill = WRITING`, `options = NULL`.
- `stem`: Some people believe that every new building in a city should be required to have a green roof. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.
- `answer_spec`: `{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":6}`
- `explanation` (bài mẫu, chỉ trả khi khối đạt):

  > Many cities now face hotter summers and more frequent flooding, so it is tempting to make green roofs compulsory on all new buildings. While I accept that such roofs have real advantages, I disagree that the rule should apply to every building.
  >
  > The case for green roofs is strong. Plants and soil keep the rooms below cooler, which lowers demand for air conditioning during heatwaves. The soil also holds rainwater and releases it gradually, easing pressure on drains. Where these roofs are widespread, as in parts of Copenhagen, flooding after heavy storms has become less of a problem.
  >
  > However, a single rule for all buildings ignores important differences between them. Installing a green roof adds considerably to construction costs, and the plants need watering, weeding and inspection for decades. A developer building a large office block can spread these costs across many tenants, but the owner of a small house cannot. In addition, some roofs are too steep or too light to carry wet soil safely, and forcing a green roof onto them would create structural risks rather than solve environmental ones.
  >
  > A better approach would be to target the requirement. Large residential and commercial buildings, where the benefits are greatest, could be obliged to include green roofs, while smaller projects could receive tax reductions or grants if their owners choose to install one.
  >
  > In conclusion, green roofs deserve support, but making them compulsory for every new building would be unfair and, in some cases, unsafe. A targeted rule combined with incentives would achieve most of the benefits at a fraction of the cost.
  >

- Bài dùng cho test và E2E (không seed vào DB): bài yếu nên ra band dưới `passBand`, bài tốt nên đạt. Test tự động dùng LLM giả nên chỉ cần số từ.

  Bài yếu:

  > Nowadays green roof is very popular in the world. Green roofs have plants on the top of the building and it make the building cool in summer. Also they can keep rain water so the city have less flood. Peoples like to see plants and it is good for bees.
  >
  > But green roof is expensive and the old building is not strong. The owner need to pay alot of money for build it and for care it every year. In the other hand the goverment can help them with money. So there are good things and bad things about green roof and every city is different.
  >

  Bài tốt:

  > In many crowded cities, summers are getting hotter and heavy rain is causing more floods. For this reason, some people argue that every new building should be required to have a green roof. I partly agree with this view: the rule makes sense for large buildings, but it should not apply to every project.
  >
  > On the one hand, green roofs bring clear benefits to urban areas. A planted roof keeps the floors below cooler, which reduces the energy needed for air conditioning. In addition, the soil absorbs rainwater and releases it slowly, so drains are less likely to overflow during storms. Copenhagen, for example, already requires new flat roofs to be planted, and the city has reported fewer problems with flooding. Cooler roofs also make summer streets more pleasant for residents.
  >
  > On the other hand, a strict rule for every building would be unfair and sometimes unsafe. Green roofs are expensive to install, and their maintenance continues for as long as the building stands. Although a large company can absorb these costs, a family building a small house could find them a serious burden. Moreover, many steep roofs cannot hold soil safely, and if owners were forced to add one anyway, the extra weight could create new risks. Older buildings would also need expensive structural checks first.
  >
  > In conclusion, I believe that cities should require green roofs on large new buildings, such as offices and apartment blocks, where the benefits are greatest and the costs can be shared. For smaller buildings, however, governments should offer incentives rather than impose a legal requirement.
  >

## Gợi ý câu Reading (V13, plan 1006)

Câu từ 3 phương án (kể cả True/False/Not Given) và câu điền có gợi ý; Q3 (2 phương án) không có.

| Mã câu | `hint` |
| --- | --- |
| Q13 | Câu chủ đề nêu ý chung của cả đoạn B. Câu có số liệu hoặc bắt đầu bằng "As a result" thường là bằng chứng hay hệ quả. |
| Q1 | Tìm câu mà các câu còn lại của đoạn C đều giải thích hoặc minh họa cho nó. Ví dụ về một thành phố cụ thể hiếm khi là câu chủ đề. |
| Q11 | Câu báo trước nội dung cả đoạn D thường ngắn và khái quát. Câu nêu một lý do cụ thể chỉ là chi tiết. |
| Q12 | Đọc câu thứ hai của đoạn D. Từ cần tìm là danh từ số nhiều chỉ người, đứng đầu câu. |
| Q5 | Ý chính của cả bài phải đúng với mọi đoạn, kể cả đoạn D. Loại phương án chỉ khớp với một chi tiết. |
| Q4 | Đọc câu đầu đoạn C, rồi chọn heading tóm được cả đoạn, không chỉ một ví dụ. |
| QT1 | So từng chi tiết của câu khẳng định với đoạn văn, nhất là ngày trong tuần. |

## Kịch bản kiểm thử

Mastery theo `compute_mastery` (5 lần gần nhất, trọng số 0.5, 0.7, 0.85, 0.95, 1.0; trần 0.5 khi 1 lần, 0.8 khi 2 lần). Số dưới đây để đối chiếu; test lấy số thật từ engine.

**Lan** (ngưỡng 0.6):

| Bước | Lần nộp đầu | Làm lại | Bằng chứng KP (theo thứ tự) | Kết quả |
| --- | --- | --- | --- | --- |
| L1 khối `L1-B2` | Q13 = A (đúng) | — | KP3: đúng | Khối đạt |
| L1 khối `L1-B5` | Q1 = A (đúng), Q11 = B (**sai**), Q12 = "critics" (đúng) | Q1 = A, Q11 = A, Q12 = "Critics": đạt, không ghi bằng chứng | KP3: đúng, đúng, sai, đúng → 0.729 | L1 xong; KP3 ≥ 0.6 → không chèn. Ngưỡng 0.9 → chèn L1 |
| L2 khối `L2-B3` | Q5 = C (**sai**) | Q5 = B: đạt | KP1: sai → 0.0 | L2 xong; KP1 < 0.6 và sai → chèn bài ôn KP1 (bài dạy L2); L3 bị chặn `REVIEW_REQUIRED` |
| Bài ôn KP1 | gói `PS-KP1-A` đúng 4/4 | — | KP1: sai, đúng ×4 → 0.875 | Review `DONE`, mở L3 |
| L3 khối `L3-B2` | Q3 = DETAIL (đúng) | — | KP2: đúng → 0.5 | L3 xong; KP2 < 0.6 nhưng không sai → không chèn |
| L4 khối `L4-B3` | Q4 = ii (đúng) | — | KP4: đúng → 0.5 | L4 xong; không chèn |
| Đề mã A (`X1`) | Q2 đúng, Q14 đúng, Q15 = MAIN_IDEA (**sai**), Q16 đúng | — | KP2: đúng, sai → 0.487 | 75% → `DEMO_READING` PASSED, `TFNG_SKILLS` IN_PROGRESS; KP2 < 0.6 và sai → chèn bài ôn KP2 (bài dạy L3) |

**Học viên giỏi:** đúng hết mọi khối ngay lần đầu (Q13, Q1, Q11, Q12, Q5, Q3, Q4) → không bài ôn nào; đề mã A 4/4 → PASSED.

**KP không có gói luyện** (quyết định 2026-10-01): KP5 không có `PRACTICE_SET`. Làm đề TFNG sai câu KP5 khi mastery KP5 < 0.6 → **không** chèn bài ôn (content không có gói cho KP5). Tương tự KP6, KP7 (Writing).

**Trượt bài ôn:** bài ôn KP2 → set 1 `PS-KP2-A` sai 2/4 → set 2 `PS-KP2-B` sai 2/4 → set 3 hết gói mới, lấy lại gói giao lâu nhất (`PS-KP2-A`) sai → review `SKIPPED`.
