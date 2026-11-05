# Phase 03 — Library: làm giàu catalog từ vựng theo Figma

Phụ thuộc: phase 01 (endpoint public, V18). Migration của phase này: **V19** (schema), **V20** (seed demo).
Nếu khi làm số migration đã bị dùng, lấy số kế tiếp và ghi trong báo cáo.

## Mục tiêu

Có đủ dữ liệu cho màn Figma `6038:19017` (Từ điển), `3967:17116` (chi tiết từ), `3042:16743` (bộ từ):
CEFR, chủ đề, band, IPA UK/US, tần suất, Academic Word List, Collocations, Thesaurus, Common Mistakes, Examples
(có bản dịch), Word family, ghi chú dùng, bộ từ theo band và theo chủ đề, mindmap.

## Quyết định

1. **Band suy ra từ CEFR** của sense (luật ở domain, enum `CefrLevel` + `IeltsBandRange`), không lưu cột band riêng:
   A1 → 1.0–2.5 · A2 → 3.0–3.5 · B1 → 4.0–5.0 · B2 → 5.5–6.5 · C1 → 7.0–8.0 · C2 → 8.5–9.0 (6 cột của "Bản đồ từ vựng").
2. **Chủ đề từ vựng do library sở hữu** (`vocabulary_topics`: Education, Environment, Technology, Health, Work &
   Business…), tách khỏi topic curriculum của content-service để không phải gọi HTTP/đồng bộ khi duyệt từ điển.
   Ghi rõ khác biệt này trong contract và `docs/system-architecture.md`.
3. **Bộ từ** (`vocabulary_sets`): `kind = BAND | TOPIC`, tiêu đề, mô tả, ảnh bìa (`cover_key`, qua `common-storage` như ảnh
   flashcard), `cefr_level` (cho BAND), `topic_id` (cho TOPIC), `sort_order`, `status`. Thành viên qua
   `vocabulary_set_items(set_id, sense_id, sort_order)`. Các nhóm Figma "English Grammar / Vocabulary / Pronunciation /
   Practice Skills / Exam Strategy / Real-life Topics" là bộ `BAND`/`TOPIC` có tiêu đề tương ứng.
4. **Không bịa dữ kiện đo được:** `frequency_rank` (COCA) chỉ ghi khi có nguồn thật, nếu không để null và UI ẩn.
   `academic_word_list` chỉ đặt `true` cho từ có trong danh sách AWL của Coxhead (2000). CEFR, chủ đề, collocation,
   ví dụ, lỗi thường gặp seed cho từ đã có ở V4/V15 là **nội dung demo do AI soạn, cần người duyệt** — ghi trong comment
   migration và README (tiền lệ: bộ `PS-WCR-*` V70 của learning).
5. Nội dung mới chỉ do `CONTENT_AUTHOR`/`ADMIN` sửa qua API admin; không có đường AI → học viên.

## Schema (V19, chỉ thêm)

- `vocabulary_items` thêm: `ipa_uk`, `ipa_us`, `audio_uk_reference`, `audio_us_reference`, `frequency_rank INT NULL`,
  `academic_word_list BOOLEAN NOT NULL DEFAULT false`, `word_family TEXT NULL`. Giữ `ipa`/`pronunciation_audio_reference`
  cũ (fallback khi UK/US null) — không xóa cột.
- `vocabulary_senses` thêm: `cefr_level VARCHAR(2) NULL CHECK (A1..C2)`, `topic_id UUID NULL REFERENCES vocabulary_topics`,
  `register VARCHAR(30) NULL` (formal/informal/academic), `grammar_note TEXT`, `usage_note TEXT`,
  `example_translation TEXT` (dịch `example_sentence`).
- Bảng mới: `vocabulary_topics(id, slug UNIQUE, name, name_vi, cover_key, sort_order, status)`;
  `vocabulary_collocations(id, sense_id, phrase, meaning_vi, example, sort_order)`;
  `vocabulary_relations(id, sense_id, related_lemma, related_item_id NULL, kind CHECK SYNONYM|ANTONYM|RELATED, sort_order)`;
  `vocabulary_common_mistakes(id, sense_id, wrong_usage, correct_usage, explanation, sort_order)`;
  `vocabulary_examples(id, sense_id, sentence, translation_vi, sort_order)`;
  `vocabulary_sets`, `vocabulary_set_items` (như trên).
- Index: `vocabulary_senses(cefr_level)`, `(topic_id)`, FK index cho mọi bảng con theo `sense_id`, `vocabulary_set_items(set_id, sort_order)`.

## API public (GET, không cần đăng nhập — thêm vào cả Gateway và library như phase 01)

| Path | Trả về |
| --- | --- |
| `GET /api/content/vocabulary/{id}` | **Mở rộng** response (chỉ thêm trường, không đổi/bỏ trường cũ): IPA/audio UK/US, frequencyRank, academicWordList, wordFamily; mỗi sense thêm cefrLevel, bandRange, topic{id,name}, register, grammarNote, usageNote, exampleTranslation, collocations[], synonyms[], antonyms[], commonMistakes[], examples[] và `counts{definitions,collocations,thesaurus,commonMistakes,examples}` cho số trên tab. Bảng con nạp theo tập sense id (mỗi bảng một query). |
| `GET /api/content/vocabulary?q=&lang=EN\|VI&cefr=&topicId=&page=&size=` | Mở rộng phase 01: `lang=VI` tìm theo `vietnamese_meaning`; lọc CEFR/chủ đề. |
| `GET /api/content/vocabulary/suggestions?q=` | ≤ 8 lemma bắt đầu bằng `q` (dòng "Gợi ý" dưới ô tìm). `q` 1–50 ký tự. |
| `GET /api/content/vocabulary/topics` | Danh sách chủ đề + số từ mỗi chủ đề (một query `GROUP BY`). |
| `GET /api/content/vocabulary/sets?kind=BAND\|TOPIC&page=&size=` | Thẻ bộ từ: tiêu đề, band, coverUrl, số chủ đề, số từ. |
| `GET /api/content/vocabulary/sets/{id}` | Chi tiết bộ: thông tin + các chủ đề trong bộ (đếm từ theo chủ đề). |
| `GET /api/content/vocabulary/sets/{id}/words?topicId=&page=&size=` | Từ trong bộ, phân trang. |
| `GET /api/content/vocabulary/mindmap?topicId=` hoặc `?itemId=` | `{nodes[], edges[]}` giới hạn ≤ 80 node: chủ đề → từ → collocation/synonym. Dùng cho tab Mindmap. |

Pattern public trên Gateway: thêm các path GET trên (đặt `suggestions`, `topics`, `sets`, `mindmap` trước pattern
`{id}` UUID — chúng không khớp regex UUID nên không xung đột). `/search`, `/senses` vẫn yêu cầu đăng nhập.

## API admin (`CONTENT_AUTHOR`, `ADMIN`; dưới `/api/content/admin/vocabulary`)

- `PUT /items/{id}` sửa trường cấp từ; `PUT /senses/{id}` sửa trường cấp nghĩa (CEFR, chủ đề, ghi chú, dịch ví dụ).
- `PUT /senses/{id}/collocations|relations|common-mistakes|examples` thay cả danh sách (validate độ dài, tối đa 30 mục).
- CRUD `topics`, `sets`, `PUT /sets/{id}/items` (danh sách sense id có thứ tự, tối đa 500).
- Bean Validation + `GlobalExceptionHandler`; không log nội dung.

## Seed demo (V20)

- 6 chủ đề Figma; CEFR + chủ đề + 2–4 collocation + 1–2 synonym + 1 common mistake + 2 ví dụ có dịch cho các từ
  hiện có ở V4/V15 (chỉ những từ đó, không thêm từ mới giả); 6 bộ BAND (A1..C2) và bộ TOPIC theo 6 chủ đề.
- `frequency_rank` để null; AWL chỉ đặt cho từ nằm trong danh sách Coxhead.
- Comment đầu file: "Demo content authored by AI; requires review by an IELTS examiner before production."

## Kiểm thử

- Unit: `CefrLevel → IeltsBandRange`, validation use case admin.
- `@WebMvcTest`: public GET không token 200; admin PUT không token 401, `CUSTOMER` 403.
- Testcontainers: V19/V20 chạy, detail nạp bảng con không N+1 (đếm query hoặc kiểm SQL log), lọc `lang=VI`, `cefr`,
  `topicId`, mindmap giới hạn node, response cũ của `/{id}` vẫn có đủ trường cũ.
- Gateway test cho các path public mới và `/search` vẫn 401.
