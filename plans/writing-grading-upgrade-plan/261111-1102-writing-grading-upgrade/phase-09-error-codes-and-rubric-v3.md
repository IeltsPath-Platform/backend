---
phase: 9
title: "Writing error codes v2 and rubric v3 (content migrations)"
status: planned
priority: P1
---

# Spec: migration mã lỗi Writing và rubric Writing v3 (content-service)

Tài liệu giao việc cho agent viết migration SQL. Nội dung (tên, định nghĩa, câu mô tả band) do product owner chốt;
agent chỉ chuyển thành SQL đúng quy ước repo, không tự đổi câu chữ. Câu nào cần đổi thì hỏi lại.

Căn cứ: sheet `Ma_loi`, `Rubric` của `docs/llm-grading-log/llm-writing-grading-prompt-iteration-log-261112.xlsx` và các
migration content V45, V50, V79, V118, V122, V123, V125, V126, V127 (nhánh `fix/writing-correction-text-and-template-v2`).

---

## 0. Quy tắc bắt buộc cho agent

1. **Số version:** lúc viết spec, migration lớn nhất của content-service là `V131`. Trước khi tạo file, fetch `main` và các
   nhánh đang mở, lấy số lớn nhất + 1 (repo từng bị trùng số, xem PR #169). Spec dùng `V132` và `V133` làm ví dụ.
2. **Không sửa migration cũ.** Không xóa dòng `error_types`, `rubrics`, `rubric_criteria`, `rubric_band_descriptors`.
   Mã lỗi thôi dùng thì đặt `status = 'INACTIVE'` hoặc bỏ khỏi `skills`. Rubric mới là version mới.
3. **Không đổi `code`** của mã lỗi đang có. Bài chấm cũ lưu mã trong JSON, bằng chứng và thống kê tham chiếu theo mã.
4. **Không đổi `knowledge_point_id` hoặc `trap_knowledge_point_id` của `TENSE`, `PLURAL_FORM`, `SENTENCE_FRAGMENT`,
   `LINKER_MISUSE`, `ARTICLE`, `SUBJECT_VERB_AGREEMENT`.** Các mã này có trap point và bộ luyện bẫy lỗi (V79, V80, V83,
   V118, V119). V118 kiểm tra trap point và KP cha cùng topic, `ErrorTrapCoverageMigrationTest` kiểm tra số bộ luyện.
   `TENSE` và `SENTENCE_FRAGMENT` còn được Speaking dùng (V115).
5. **Không đổi `category`, `knowledge_point_id`, `fix_hint` của `SPELLING` và `PLURAL_FORM`.** `ErrorTagger` của
   learning-service gán hai mã này cho câu trả lời điền từ của Listening và Reading, nên KP `LS_SPELL` và câu "theo đề bài"
   vẫn đúng cho Listening. Writing chuyển sang dùng mã mới (mục 2.4).
6. **Mỗi migration tự kiểm tra cuối file** bằng khối `DO $$ … RAISE EXCEPTION …` theo mẫu V125/V122. Thiếu KP hay sai số
   dòng thì migration phải thất bại, không được âm thầm bỏ qua.
7. **Comment đầu file** ghi lý do và ai quyết định (mẫu V125–V127). Không nhắc AI, agent, phase hay mã plan.
8. **Mã lỗi Writing dùng chung cho learning-service (luyện tập, lesson) và assessment-service (thi topic/course, qua
   `/internal/learning-content/error-types?skill=WRITING`).** Không đổi tên cột hay kiểu dữ liệu đang có.
9. **Test:** thêm hoặc cập nhật test Testcontainers kiểm tra kết quả migration (mẫu các `*MigrationTest` của
   content-service), rồi chạy `mvn -q -pl services/content-service -am test`. Thiếu Docker thì báo rõ test bị bỏ qua.
10. **Phạm vi:** chỉ migration và test của content-service. Code đọc cột mới (learning, assessment, content API) là
    việc khác, không làm trong task này (mục 4).

---

## 1. Migration 1: `V132__writing_error_codes_definitions_and_tasks.sql`

### 1.1 Thêm cột

```sql
ALTER TABLE error_types
    ADD COLUMN model_definition TEXT,
    ADD COLUMN writing_tasks VARCHAR(10)[] NOT NULL DEFAULT '{}',
    ADD COLUMN model_selectable BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE error_types ADD CONSTRAINT chk_error_types_writing_tasks
    CHECK (writing_tasks <@ ARRAY['TASK_1', 'TASK_2']::varchar[]);
```

Ý nghĩa:

| Cột | Nghĩa |
|---|---|
| `model_definition` | Định nghĩa tiếng Anh gửi cho model: dùng khi nào, không dùng khi nào, ví dụ. `fix_hint` vẫn là lời khuyên tiếng Việt cho học viên. NULL với mã không thuộc Writing. |
| `writing_tasks` | Task Writing mà mã áp dụng. Rỗng với mã không thuộc Writing. |
| `model_selectable` | `false`: mã vẫn ACTIVE để ghi bằng chứng, nhưng không gửi cho model chọn; backend tạo từ `taskChecks`. |

`content-service` dùng `ddl-auto: validate`; thêm cột không ánh xạ trong entity vẫn chạy. Agent kiểm tra lại bằng test
khởi động context.

### 1.2 Sửa mã đang có

| Mã | Thay đổi | Lý do |
|---|---|---|
| `NO_CLEAR_POSITION` | `category = 'TASK'`, `model_selectable = false` | Lập trường thuộc TR; KP `TR_W_OPINION` đã là TR. Backend tạo từ `taskChecks.position`. |
| `PREPOSITION` | `category = 'VOCABULARY'` | Giới từ đi kèm thuộc LR; KP `V55_ACADEMIC_COLLOCATIONS` là KP từ vựng. Mã này không có trap point. |
| `T1_NO_OVERVIEW` | `model_selectable = false` | Backend tạo từ `taskChecks.hasOverview`. |
| `T2_PARTIAL_ANSWER` | `model_selectable = false` | Backend tạo từ `taskChecks.questionParts`. |
| `VERB_FORM` | `fix_hint` = `Kiểm tra dạng động từ: sau trợ động từ và modal dùng nguyên mẫu (can go), không đặt am/is/are trước động từ thường (is have → has), dùng đúng dạng quá khứ của động từ bất quy tắc (grew, rose).` | fix_hint cũ mở đầu bằng can/could/should nên lệch với lỗi "is have". |
| `SPELLING`, `PLURAL_FORM` | `skills = array_remove(skills, 'WRITING')`; không đổi gì khác | Writing chuyển sang `WRITING_SPELLING`, `NOUN_PLURAL` (mục 1.4). Xem quy tắc 0.5. |
| `TENSE` | Không đổi KP, tên, fix_hint; chỉ thêm `model_definition` (thu hẹp phạm vi, quá khứ đơn tách sang `PAST_SIMPLE_TENSE`) | Quy tắc 0.4. |
| `LINKER_MISUSE` | Không đổi KP (vẫn `DEMO_WRITING_LINKING`) | Có trap point và 6 bộ luyện dưới KP này. Đổi sang KP thật là việc riêng của content. |
| `OFF_TOPIC` | `writing_tasks = '{TASK_2}'`; KP giữ NULL | Sheet `Ma_loi` đề xuất `M55_WRITING_T2_BASICS_KP` nhưng KP này không có trong migration nào. Task 1 dùng `T1_IRRELEVANT_CONTENT`. |

Mọi `UPDATE` đặt `updated_at = CURRENT_TIMESTAMP`.

### 1.3 `writing_tasks` cho mã Writing đang có

| `writing_tasks` | Mã |
|---|---|
| `{TASK_1}` | `T1_DATA_MISREPORTED`, `T1_NO_OVERVIEW` |
| `{TASK_2}` | `NO_CLEAR_POSITION`, `OFF_TOPIC`, `T2_IRRELEVANT_EXAMPLE`, `T2_PARTIAL_ANSWER`, `UNDERDEVELOPED_IDEA` |
| `{TASK_1,TASK_2}` | `ARTICLE`, `COLLOCATION`, `COMPARISON`, `LINKER_MISUSE`, `PASSIVE_VOICE`, `PREPOSITION`, `SENTENCE_FRAGMENT`, `SUBJECT_VERB_AGREEMENT`, `TENSE`, `VERB_FORM`, `WORD_CHOICE`, `WORD_FORM`, `GRAMMAR_OTHER`, `VOCABULARY_OTHER`, `COHERENCE_OTHER`, `TASK_OTHER` |

`T2_NO_CONCLUSION` (INACTIVE từ V127), `SPELLING`, `PLURAL_FORM` giữ `writing_tasks = '{}'`.

### 1.4 Thêm mã mới

Tất cả: `status = 'ACTIVE'`, `review_status = 'APPROVED'` (quyết định của product owner, như V125/V126),
`skills = ARRAY['WRITING']`, `fallback = false`, `model_selectable = true`, `trap_knowledge_point_id = NULL`
(chưa có bộ luyện bẫy lỗi, cùng lý do V125). Dùng `INSERT … SELECT … JOIN knowledge_points` và `ON CONFLICT (code) DO NOTHING`
như V125. Riêng `WRITING_SPELLING` không có KP nên dùng `LEFT JOIN` hoặc chèn riêng với `NULL`.

| code | name | category | severity | KP (code) | writing_tasks | fix_hint |
|---|---|---|---|---|---|---|
| `T1_IRRELEVANT_CONTENT` | Nêu ý kiến hoặc lý do không có trong biểu đồ | TASK | 2 | `W60_T1_KEY_FEATURES` | `{TASK_1}` | Task 1 chỉ mô tả những gì biểu đồ cho thấy; bỏ ý kiến, lời khuyên, nguyên nhân hay dự đoán không có trong đề. |
| `DATA_LANGUAGE` | Diễn đạt xu hướng hoặc số lượng chưa đúng | VOCABULARY | 1 | `V60_DATA_TREND_WORDS` | `{TASK_1}` | Dùng cụm chuẩn để mô tả số liệu: rose sharply, fell slightly, reached a peak of, made 9 visits. |
| `MISSING_VERB` | Câu thiếu động từ chính | GRAMMAR | 2 | `G55_BASIC_SENTENCE_PATTERNS` | `{TASK_1,TASK_2}` | Mỗi mệnh đề cần một động từ chia theo chủ ngữ; kiểm tra có thiếu be hay động từ chính không. |
| `PAST_SIMPLE_TENSE` | Sai thì quá khứ đơn | GRAMMAR | 1 | `G40_PAST_SIMPLE` | `{TASK_1,TASK_2}` | Mốc thời gian đã qua (in 2000, last year) dùng quá khứ đơn: was, became, rose. |
| `RUN_ON_SENTENCE` | Nối hai câu bằng dấu phẩy | GRAMMAR | 1 | `W50_COMPOUND_SENTENCES` | `{TASK_1,TASK_2}` | Hai mệnh đề độc lập phải tách bằng dấu chấm hoặc nối bằng từ nối (and, but, while, whereas). |
| `NOUN_PLURAL` | Sai số ít hoặc số nhiều của danh từ | GRAMMAR | 1 | `G55_ARTICLES` | `{TASK_1,TASK_2}` | Danh từ đếm được sau số lớn hơn một cần -s: five age groups, 9 visits; people đã là số nhiều. |
| `REFERENCE` | Từ thay thế không rõ chỉ cái gì | COHERENCE | 1 | `W60_PARAGRAPH_COHESION` | `{TASK_1,TASK_2}` | Dùng it, this, they chỉ khi người đọc biết rõ từ đó thay cho danh từ nào; nếu không rõ, nhắc lại danh từ. |
| `REPETITION` | Lặp từ nhiều lần | VOCABULARY | 1 | `V55_PARAPHRASE_WORDS` | `{TASK_1,TASK_2}` | Chuẩn bị từ thay thế cho từ hay lặp: visits / trips to the library, number / figure. |
| `INFORMAL_REGISTER` | Dùng từ thân mật trong bài học thuật | VOCABULARY | 1 | `W80_ACADEMIC_STYLE` | `{TASK_1,TASK_2}` | Bài IELTS Writing dùng văn phong trang trọng: không viết tắt (do not), tránh kids, a lot of, stuff. |
| `WRITING_SPELLING` | Sai chính tả | VOCABULARY | 1 | NULL (chưa có KP chính tả cho Writing) | `{TASK_1,TASK_2}` | Kiểm tra lại các từ hay viết sai: library, increase, government, environment. |

Nếu một KP trong bảng không tồn tại hoặc không ACTIVE dưới topic và course ACTIVE, **dừng và báo lại**, không tự chọn KP
khác. Đặc biệt `G40_PAST_SIMPLE` và `W80_ACADEMIC_STYLE` chỉ xuất hiện trong một migration, agent cần kiểm tra trạng thái.

### 1.5 `model_definition` (nguyên văn, tiếng Anh)

Đặt cho mọi mã có `'WRITING' = ANY(skills)` sau khi chạy 1.2 và 1.4.

| code | model_definition |
|---|---|
| `ARTICLE` | Use for a missing, extra or wrong article (a, an, the): "The number of the visits rose" -> "The number of visits rose". Not for a missing plural -s (NOUN_PLURAL). |
| `COLLOCATION` | Use when the meaning is right but the words do not naturally go together: "make a research" -> "do research". Not for a wrong meaning (WORD_CHOICE), a wrong word class (WORD_FORM), or a Task 1 trend or quantity phrase (DATA_LANGUAGE). |
| `COMPARISON` | Use for a wrongly built comparative or superlative: "more higher", "then" for "than", "the most high". Not for a comparison that contradicts the chart (T1_DATA_MISREPORTED). |
| `LINKER_MISUSE` | Use when a linking word shows the wrong relation between ideas ("However" for an addition, "because" for a result) or is used where none fits. Not for an unclear it/this/they (REFERENCE). |
| `NO_CLEAR_POSITION` | Task 2, only when the question asks for an opinion: the essay never states a clear position, or later contradicts it. |
| `OFF_TOPIC` | Task 2: a sentence or paragraph that does not answer the question that was asked. Not for a relevant idea with a poor example (T2_IRRELEVANT_EXAMPLE). |
| `PASSIVE_VOICE` | Use for a wrongly built passive ("is collect", "was locate", "are remove") or a passive where the active is needed. Not for other verb forms (VERB_FORM). |
| `PREPOSITION` | Use for a wrong, missing or extra preposition: "increase of 5%" -> "increase by 5%", "discuss about". Not for a whole unnatural phrase (COLLOCATION). |
| `SENTENCE_FRAGMENT` | Use for a dependent clause or phrase written as a full sentence: "Because the price rose." / "Which shows a rise.". Not for a clause with no verb (MISSING_VERB) or two sentences joined by a comma (RUN_ON_SENTENCE). |
| `SUBJECT_VERB_AGREEMENT` | Use when only the agreement is wrong: "it have", "they was", "the number of visits are". If the verb form is also wrong ("they was go"), give VERB_FORM as a second entry. |
| `T1_DATA_MISREPORTED` | Task 1: a figure, trend, comparison, process step or map change that contradicts <chart_facts>. The suggestion states what the chart shows. Grammar errors in the same words get their own entries. |
| `T1_NO_OVERVIEW` | Task 1: the essay has no overview of the main trends or features. |
| `T2_IRRELEVANT_EXAMPLE` | Task 2: an example that does not support the point of its paragraph. A relevant but too general example is UNDERDEVELOPED_IDEA. |
| `T2_PARTIAL_ANSWER` | Task 2: one part of the question is not answered, or is only mentioned without development. |
| `TENSE` | Use when the verb form is correct but the tense does not fit the time or meaning, such as present perfect with a finished time ("has increased in 2000") or present continuous for a general fact. A finished past time that needs the past simple uses PAST_SIMPLE_TENSE. |
| `UNDERDEVELOPED_IDEA` | Task 2: a main idea with no reason or example, or with an example too general to support it. Quote the topic sentence. |
| `VERB_FORM` | Use for a wrong verb form that no more specific code covers: be + base verb ("is have", "was go"), modal + -s or to ("can goes"), wrong -ing or to-infinitive ("enjoy to read"), wrong irregular past ("growed"). Not agreement only (SUBJECT_VERB_AGREEMENT), tense choice (TENSE, PAST_SIMPLE_TENSE), passive (PASSIVE_VOICE) or a missing verb (MISSING_VERB). |
| `WORD_CHOICE` | Use when a word has the wrong meaning in context: "The table talking about" -> "The table shows". Not for an unnatural pairing (COLLOCATION) or a wrong word class (WORD_FORM). |
| `WORD_FORM` | Use when the word class is wrong: "a convenience way" -> "a convenient way", "grow rapid" -> "grow rapidly". Not for a wrong meaning (WORD_CHOICE). |
| `GRAMMAR_OTHER` | Fallback: a grammar error that no listed grammar code covers. Never use it when a specific code fits. |
| `VOCABULARY_OTHER` | Fallback: a vocabulary error that no listed vocabulary code covers. Never use it when a specific code fits. |
| `COHERENCE_OTHER` | Fallback: an organisation or cohesion error that no listed coherence code covers. Never use it when a specific code fits. |
| `TASK_OTHER` | Fallback: a task problem that no listed task code covers. Never use it when a specific code fits. |
| `T1_IRRELEVANT_CONTENT` | Task 1: a personal opinion, recommendation, cause or prediction that the visual does not show ("because people like reading", "everyone should go to the library"). The suggestion is an empty string. |
| `DATA_LANGUAGE` | Task 1: a wrong or unnatural expression for a trend, change or quantity when the figure itself is right ("increase very big", "the number was go down"). If the figure is also wrong, add a T1_DATA_MISREPORTED entry. |
| `MISSING_VERB` | A clause has no finite verb, usually a missing be or main verb: "they only 6 visit" -> "they made only 6 visits". |
| `PAST_SIMPLE_TENSE` | A finished past time needs the past simple and another tense or the base form is used: "in 2020 it become 4" -> "became", "in 2000 the number is 9" -> "was". |
| `RUN_ON_SENTENCE` | Two independent clauses joined only by a comma or with no linking word: "The number rose, it reached 12 in 2020". Not for a linking word with the wrong meaning (LINKER_MISUSE). |
| `NOUN_PLURAL` | A countable noun that needs the plural ("five age group", "9 visit", "every years" -> "every year") or a wrong plural ("peoples"). Not for subject-verb agreement. |
| `REFERENCE` | A reference word (it, this, they, that, which) whose referent is unclear or wrong, or a noun repeated where a reference word is clearly needed. Not for a linking word (LINKER_MISUSE). |
| `REPETITION` | The same word or phrase repeated several times where an alternative is expected. Give one entry that quotes a repeated occurrence. Not for a single wrong word (WORD_CHOICE). |
| `INFORMAL_REGISTER` | Informal words or forms in academic writing: contractions ("don't"), "kids", "a lot of", "stuff". |
| `WRITING_SPELLING` | A misspelled word ("libary", "increace"). Not for a wrong word class (WORD_FORM) or a missing plural -s (NOUN_PLURAL). |

### 1.6 Kiểm tra cuối migration

Khối `DO $$` phải `RAISE EXCEPTION` khi:

1. Có mã ACTIVE với `'WRITING' = ANY(skills)` mà `model_definition IS NULL` hoặc `writing_tasks = '{}'`.
2. Thiếu một trong 10 mã mới, hoặc mã mới không có `'WRITING' = ANY(skills)`.
3. Một mã mới (trừ `WRITING_SPELLING`) không gắn KP ACTIVE dưới topic và course ACTIVE (mẫu V125).
4. `SPELLING` hoặc `PLURAL_FORM` vẫn có `'WRITING' = ANY(skills)`, hoặc `category`/`knowledge_point_id`/`fix_hint` của
   chúng khác giá trị trước migration. Lưu giá trị cũ vào biến hoặc bảng tạm đầu file để so.
5. `knowledge_point_id` hoặc `trap_knowledge_point_id` của `TENSE`, `PLURAL_FORM`, `SENTENCE_FRAGMENT`, `LINKER_MISUSE`,
   `ARTICLE`, `SUBJECT_VERB_AGREEMENT` thay đổi.

Kết quả mong đợi: 33 mã ACTIVE có `'WRITING' = ANY(skills)` (25 hiện có − `SPELLING` − `PLURAL_FORM` + 10 mới), trong đó
3 mã có `model_selectable = false`.

---

## 2. Migration 2: `V133__writing_rubric_v3_draft.sql`

### 2.1 Cách làm

- Theo mẫu V122, nhưng **không retire version đang ACTIVE** và **tạo version mới với `status = 'DRAFT'`**. Kích hoạt là
  migration riêng, chỉ làm sau khi chấm lại bộ test đạt (quy trình trong sheet `Huong_dan`). Lý do: grader dùng rubric
  ACTIVE khi không có activation; bật ngay sẽ đổi điểm của học viên thật trước khi đo.
- `uq_rubrics_one_active_per_task` chỉ chặn hai ACTIVE, nên DRAFT song song với ACTIVE hợp lệ.
- Id cố định: Task 1 `39a00000-0000-4000-8000-000000000005`, Task 2 `39a00000-0000-4000-8000-000000000006`. Agent kiểm
  tra hai id này chưa được dùng.
- `rubric_version` = `MAX(rubric_version) + 1` theo `code` (hiện là 3).
- `source_note`: `Version 3: criterion-specific descriptors for every whole band 0-9, scope and ceilings in each criterion description, half bands defined from the whole bands. Original paraphrase for calibration, based on the public IELTS Writing band descriptors (ielts.org): https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf. Requires review by an IELTS examiner before activation.`
- `system_prompt`: chép từ version ACTIVE hiện tại.
- `rubric_examples`: chép từ version ACTIVE hiện tại.
- `rubric_criteria`: giữ `code`, `name`, `sort_order` như version ACTIVE; **`description` thay bằng mục 2.3**.
- `rubric_band_descriptors`: 18 dòng mỗi tiêu chí (0, 1.0, 1.5 … 9.0), như V122:
  - Band nguyên 0–9 (10 dòng): câu ở mục 2.4–2.7.
  - Band 1.5–8.5 (8 dòng): `Meets every point of band X and some, but not all, points of band Y.` với X là band nguyên
    phía dưới, Y = X + 1. Ví dụ band 5.5: `Meets every point of band 5 and some, but not all, points of band 6.`
- **Không nối câu hướng dẫn vào cuối descriptor** như V122 đã làm. Câu đó nằm trong `description`.
- KP của tiêu chí (`writing_criterion_knowledge_points`, V69) tính theo task và mã tiêu chí, không theo version, nên
  không cần làm gì.

Khi render, learning-service ghép `CODE (name): description` rồi từng dòng `  Band X: descriptor`. Vì vậy `description`
không được lặp `TA (Task Achievement):` ở đầu.

### 2.2 Câu chung dùng cho mọi band 0 (8 tiêu chí)

```
No assessable response: not attempted, memorised or copied text, not in English, or entirely unrelated to the task.
```

### 2.3 `description` của từng tiêu chí (nguyên văn, giữ xuống dòng)

**TASK_1 / TA**

```
How well the response selects, reports and compares the key features of the visual. Key features are the main trends, the highest and lowest values and the main differences for data; the main stages for a process; the main changes for a map.
Judges: overview, choice of key features, accuracy against <chart_facts>, comparisons, irrelevant content.
Does not judge: grammar, vocabulary or organisation, unless they make the data impossible to understand.
Apply these ceilings before choosing a band:
- Fewer than 150 words: at most 5.
- No overview, or the overview states a wrong main trend: at most 5.
- Most reported figures are wrong: at most 4.
- A personal opinion, a reason or a prediction that the visual does not show: at most 6.
```

**TASK_2 / TR**

```
How fully the response answers every part of the question and develops relevant ideas.
Judges: coverage of each part of the question, position (only when the question asks for one), development and support of ideas, relevance.
Does not judge: grammar, vocabulary or organisation, unless they make the ideas impossible to understand.
Apply these ceilings before choosing a band:
- Fewer than 250 words: at most 5.
- The question asks for an opinion and no position is given: at most 5.
- One part of the question is not answered: at most 5.
- Most of the response is off topic: at most 4.
```

**CC (giống nhau ở TASK_1 và TASK_2)**

```
How clearly the ideas are ordered and connected.
Judges: logical order and progression, paragraphing, linking words, reference and substitution (it, this, they, the former).
Does not judge: grammar accuracy or word choice, except for linking words and reference.
Apply this ceiling before choosing a band:
- No paragraphing at all: at most 5.
```

**LR (giống nhau ở hai task)**

```
Range and accuracy of vocabulary.
Judges: range, word choice, collocation, word formation, spelling, register, repetition; in Task 1 also the words used to describe trends and quantities.
Does not judge: sentence structure, verb tense or agreement (these are GRA).
```

**GRA (giống nhau ở hai task)**

```
Range of sentence structures and grammatical accuracy.
Judges: simple and complex sentences, verb forms, tense, agreement, articles, plurals, sentence boundaries, punctuation.
Does not judge: whether a figure is right (that is TA) or word choice (that is LR).
Apply this ceiling before choosing a band:
- Almost only simple sentences: at most 5.
```

### 2.4 TA (TASK_1), band nguyên

| band | descriptor |
|---|---|
| 9 | Every key feature is selected and compared; the overview is clear and accurate; every figure, stage or change is reported accurately; nothing irrelevant is included. |
| 8 | Every key feature is covered and well compared; the overview is clear and accurate; at most a rare minor slip in a detail. |
| 7 | The key features are covered and clearly highlighted; there is a clear, accurate overview; data is selected and compared, though some points could be more fully covered or there is a little unnecessary detail. |
| 6 | The main requirements are met; an overview is attempted but is incomplete or only partly accurate; key features are covered but some details are missing, wrong or irrelevant. |
| 5 | Information is reported mechanically with little selection or comparison; there is no clear overview; several figures are wrong or the response focuses on detail instead of key features. |
| 4 | Several key features are missing or confused; there is no overview and much of the reported data is wrong, irrelevant or repeated. |
| 3 | The task is barely addressed: few key features appear and most of the data is wrong or misunderstood. |
| 2 | The content is barely related to the visual. |
| 1 | The response is unrelated to the visual apart from a few words. |
| 0 | (câu chung mục 2.2) |

### 2.5 TR (TASK_2), band nguyên

| band | descriptor |
|---|---|
| 9 | Every part of the question is fully answered; any required position is clear and consistent throughout; ideas are fully extended and well supported; nothing irrelevant. |
| 8 | Every part is answered well; any required position is clear; ideas are well developed and supported, with at most an occasional lapse. |
| 7 | Every part is answered; any required position is clear throughout; main ideas are extended and supported, though some are over-generalised or lack focus. |
| 6 | Every part is answered, some more fully than others; any required position is relevant but the conclusion may be unclear or repetitive; main ideas are relevant but some are not developed enough. |
| 5 | The question is answered only partly; a required position is unclear or changes; main ideas are few or not developed enough, and some content is irrelevant. |
| 4 | The question is answered minimally or only indirectly; a required position is missing or hard to find; ideas are few, repeated or unsupported. |
| 3 | No part of the question is answered adequately; ideas are few, undeveloped or irrelevant. |
| 2 | The content is barely related to the question. |
| 1 | The response is unrelated to the question apart from a few words. |
| 0 | (câu chung mục 2.2) |

### 2.6 CC (cả hai task), band nguyên

| band | descriptor |
|---|---|
| 9 | Ideas progress without effort for the reader; linking and reference are natural and never draw attention; paragraphing is skilful. |
| 8 | Ideas are ordered logically throughout; linking and reference are well managed with rare lapses; paragraphing is appropriate. |
| 7 | There is clear progression throughout; a range of linking words is used, with some over- or under-use; reference is generally clear; each paragraph has one clear central idea. |
| 6 | The response is coherent with clear progression in most places; linking works but is sometimes mechanical or faulty; reference is not always clear; paragraphs are used but not always logically. |
| 5 | There is some organisation but no clear overall progression; linking words are too few, wrong or overused; nouns are repeated because reference is not used; paragraphing is missing or weak. |
| 4 | Ideas are not arranged in a clear order; only basic linking words are used, often wrongly or repeatedly; paragraphing is missing or confusing. |
| 3 | Ideas have no logical order; linking words are few and often wrong; the reader must work hard to follow. |
| 2 | There is very little control of organisation. |
| 1 | No organised message can be identified. |
| 0 | (câu chung mục 2.2) |

### 2.7 LR (cả hai task), band nguyên

| band | descriptor |
|---|---|
| 9 | A wide range of vocabulary is used naturally and precisely; errors are very rare and minor. |
| 8 | A wide range is used fluently and flexibly, including less common words used skilfully; occasional errors in spelling or word formation. |
| 7 | The range allows some flexibility and precision; some less common words are used, with awareness of style and collocation; occasional errors in word choice, spelling or word formation. |
| 6 | The range is adequate for the task; less common words are attempted with some inaccuracy; some errors in spelling or word formation that do not stop communication. |
| 5 | The range is limited but just enough for the task; words are repeated; noticeable errors in spelling or word formation may cause some difficulty for the reader. |
| 4 | Only basic vocabulary is used, often repeatedly or inappropriately; frequent errors in word formation and spelling may strain the reader. |
| 3 | The range is very limited; errors in word choice, form and spelling seriously distort meaning. |
| 2 | Vocabulary is extremely limited, with almost no control of word formation or spelling. |
| 1 | Only a few isolated words can be used. |
| 0 | (câu chung mục 2.2) |

### 2.8 GRA (cả hai task), band nguyên

| band | descriptor |
|---|---|
| 9 | A wide range of structures is used flexibly and accurately; errors are very rare and minor. |
| 8 | A wide range of structures is used; most sentences are error-free; errors are very occasional. |
| 7 | A variety of complex structures is used; error-free sentences are frequent; grammar and punctuation are well controlled with a few errors. |
| 6 | Simple and complex sentences are mixed; there are some errors in grammar and punctuation, but they rarely make meaning unclear. |
| 5 | The range is limited; complex sentences are attempted but are less accurate than simple ones; grammar and punctuation errors are frequent and may cause difficulty for the reader. |
| 4 | The range is very limited and subordinate clauses are rare; some structures are correct but errors are the norm and punctuation is often wrong. |
| 3 | Sentences are attempted but errors in grammar and punctuation dominate and distort meaning. |
| 2 | There is almost no sentence structure apart from memorised phrases. |
| 1 | No rateable language apart from isolated words. |
| 0 | (câu chung mục 2.2) |

### 2.9 Kiểm tra cuối migration

Khối `DO $$` phải `RAISE EXCEPTION` khi:

1. Version ACTIVE của `IELTS_WRITING_TASK_1` hoặc `IELTS_WRITING_TASK_2` bị đổi trạng thái (phải vẫn là version trước đó).
2. Mỗi rubric mới không có đúng 4 tiêu chí và 4 × 18 descriptor, hoặc thiếu band nào trong 0, 1.0, 1.5 … 9.0.
3. `status` của rubric mới khác `DRAFT`.
4. Có descriptor của rubric mới chứa `For this visual report` hoặc `For this essay` (câu đuôi cũ của V122).
5. Số `rubric_examples` của rubric mới khác số của version ACTIVE.

---

## 3. Test cần có (content-service, Testcontainers)

Một lớp test cho mỗi migration, đặt cạnh các `*MigrationTest` hiện có:

- **Mã lỗi:**
  - 33 mã Writing ACTIVE, đủ `model_definition` và `writing_tasks`.
  - `SPELLING` và `PLURAL_FORM` không còn `WRITING`, nhưng `category`, KP, fix_hint giữ nguyên.
  - KP và trap point của 6 mã ở quy tắc 0.4 không đổi.
  - `ErrorTrapCoverageMigrationTest` vẫn xanh.
- **Rubric:**
  - Có 2 rubric `DRAFT` version 3, mỗi rubric 72 descriptor.
  - Version ACTIVE vẫn là version 2.
  - Band 5.5 của GRA Task 2 đúng là câu mẫu band nửa.
  - Không còn câu đuôi cũ.

Chạy `mvn -q -pl services/content-service -am test` và báo kết quả thật. CI chạy `mvn -B -ntp verify` toàn reactor;
test Testcontainers tự bỏ qua khi không có Docker.

---

## 4. Ngoài phạm vi (việc code làm sau, ghi lại để không quên)

1. **content-service:** entity, API nội bộ `/internal/learning-content/error-types` trả thêm `modelDefinition`,
   `writingTasks`, `modelSelectable`; trình sửa mã lỗi của admin cho sửa ba cột này.
2. **learning-service (`EssayGrader.systemPrompt`) và assessment-service (`RubricPromptBuilder`):**
   - Dòng gửi model dùng `code — model_definition` thay vì `fix_hint`.
   - Lọc theo `writing_tasks` và `model_selectable`.
   - Tạo bằng chứng `T1_NO_OVERVIEW`, `NO_CLEAR_POSITION`, `T2_PARTIAL_ANSWER` từ `taskChecks`.
3. **Grader:** đổi sang `WRITING_SPELLING`, `NOUN_PLURAL` khi chấm Writing; `ErrorTagger` của Listening/Reading giữ
   `SPELLING`, `PLURAL_FORM`.
4. **`withNextBands`:** bỏ qua band .5 khi gợi ý band tiếp theo.
5. **Kích hoạt rubric v3:** migration riêng (retire version 2, set version 3 ACTIVE) sau khi chấm lại bộ test với rubric
   v3 và prompt v2 đạt mục tiêu trong sheet `Tong_hop`. Kiểm tra `JdbcRubricRepository.validateForActivation` có chặn
   kích hoạt từ admin không (V122 ghi admin chỉ nhận band 4.0–9.0).
6. **`LINKER_MISUSE`:** chuyển khỏi KP demo cần chuyển cả trap point và 6 bộ luyện; việc riêng của content.
7. **KP chính tả cho Writing:** tạo KP rồi gắn cho `WRITING_SPELLING`.
