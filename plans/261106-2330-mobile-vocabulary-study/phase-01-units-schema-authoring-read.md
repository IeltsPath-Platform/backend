# Phase 1: Unit — bảng dữ liệu, API biên soạn (content author), API đọc nội dung

Service: library-service. Phụ thuộc: không. Đọc trước: [plan.md](./plan.md) mục 7 (quy ước chung).

## 1. Mục tiêu

Content author tạo unit cho một topic (tên, lời dẫn, danh sách sense có thứ tự), kiểm tra, phát hành, sắp xếp, lưu trữ. Học viên đọc danh sách unit của topic và nội dung một unit. Sửa unit đã phát hành không làm đổi thứ học viên đang thấy cho đến khi phát hành revision mới.

## 2. Dữ liệu có sẵn để dùng lại

- `vocabulary_topics` (V19): `id, slug, name, name_vi, cover_key, sort_order, status ACTIVE|INACTIVE`.
- `vocabulary_senses` (V1 + V19): `id, vocabulary_item_id, part_of_speech` (chuỗi hoa: `NOUN`, `VERB`, ...), `english_definition, vietnamese_meaning` (NOT NULL), `example_sentence` (NOT NULL), `example_translation, image_url, cefr_level, topic_id, status`.
- `vocabulary_items` (V1 + V19 + V33/34): `id, lemma, normalized_lemma, ipa, ipa_uk, ipa_us, audio_uk_reference, audio_us_reference, pronunciation_audio_reference, status`. Các cột audio chứa **khóa đối tượng** trong bucket, không phải URL. Đổi thành URL phát được bằng `VocabularyAudioUrlResolver.resolve(reference)` (trả `null` nếu thiếu hoặc storage chưa cấu hình). Chọn audio ưu tiên bằng `VocabularyItem.preferredAudioReference(us, uk, generic)`, y như `VocabularyCatalogJdbcAdapter` (dòng 635).
- Phạm vi "từ của chủ đề": `vocabulary_senses.topic_id`. Tác giả được phép chọn sense của topic khác (unit ôn tập có thể trộn), nhưng sense phải `ACTIVE` và thuộc item `ACTIVE`.

## 3. Migration `V39__vocabulary_study_units.sql`

```sql
CREATE TABLE vocabulary_study_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES vocabulary_topics(id),
    sort_order INT NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('LEARN', 'REVIEW')),
    published_revision INT,
    draft_revision INT,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (published_revision IS NOT NULL OR draft_revision IS NOT NULL)
);
CREATE INDEX idx_vocab_study_units_topic
    ON vocabulary_study_units (topic_id, sort_order) WHERE archived_at IS NULL;

CREATE TABLE vocabulary_study_unit_revisions (
    unit_id UUID NOT NULL REFERENCES vocabulary_study_units(id) ON DELETE CASCADE,
    revision INT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    introduction_vi TEXT NOT NULL,
    exercise_types TEXT[],            -- tính lúc phát hành, ví dụ {CHOICE,MATCH,FILL}
    graded_step_count INT,            -- tính lúc phát hành
    published_at TIMESTAMPTZ,         -- null = bản nháp
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (unit_id, revision)
);

CREATE TABLE vocabulary_study_unit_words (
    unit_id UUID NOT NULL,
    revision INT NOT NULL,
    sense_id UUID NOT NULL REFERENCES vocabulary_senses(id) ON DELETE RESTRICT,
    sort_order INT NOT NULL,
    PRIMARY KEY (unit_id, revision, sense_id),
    FOREIGN KEY (unit_id, revision)
        REFERENCES vocabulary_study_unit_revisions(unit_id, revision) ON DELETE CASCADE
);
CREATE INDEX idx_vocab_study_unit_words_order ON vocabulary_study_unit_words (unit_id, revision, sort_order);

CREATE TABLE vocabulary_study_topic_versions (
    topic_id UUID PRIMARY KEY REFERENCES vocabulary_topics(id),
    curriculum_version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Ý nghĩa trạng thái (suy ra, không lưu cột `status`):

| `published_revision` | `draft_revision` | `archived_at` | Trạng thái trả về cho content author |
|---|---|---|---|
| null | số | null | `DRAFT` |
| số | null | null | `PUBLISHED` |
| số | số | null | `PUBLISHED_WITH_DRAFT` |
| bất kỳ | bất kỳ | có giá trị | `ARCHIVED` |

Học viên chỉ thấy unit có `published_revision` khác null và `archived_at` null, và chỉ thấy **revision đã phát hành**.

## 4. Quy tắc nghiệp vụ

**Tạo.** Insert `vocabulary_study_units` với `sort_order = (max sort_order của topic chưa lưu trữ) + 1` (1 nếu chưa có), `draft_revision = 1`, `published_revision = null`; insert revision 1 và các dòng `unit_words` theo thứ tự `senseIds` (sort_order 1..n). `senseIds` không được trùng. Mọi sense phải tồn tại.

**Sửa.**
- Unit chưa từng phát hành, hoặc đã có `draft_revision`: cập nhật tại chỗ revision nháp (xóa `unit_words` cũ của revision đó rồi ghi lại).
- Unit đã phát hành và chưa có nháp: tạo `revision = published_revision + 1` (sao chép nội dung rồi áp thay đổi), đặt `draft_revision`.
- Unit đã lưu trữ: `409 VOCAB_UNIT_ARCHIVED`.

**Kiểm tra (`validation`).** Chạy trên `draft_revision` nếu có, nếu không thì trên `published_revision`. Lớp thuần `domain/study/StudyUnitRules`.

Lỗi chặn phát hành:

| Mã | Điều kiện |
|---|---|
| `TOO_FEW_WORDS` | Ít hơn 4 sense hợp lệ |
| `INACTIVE_SENSE` | Có sense hoặc item `INACTIVE` (kèm `senseId`) |
| `MISSING_MEANING` | `vietnamese_meaning` rỗng sau khi trim |
| `DUPLICATE_MEANING` | Hai sense trong unit có `vietnamese_meaning` giống nhau sau khi chuẩn hóa (trim, NFC, chữ thường). Cần để câu ghép và trắc nghiệm không mơ hồ |
| `NO_GRADED_STEP` | Bộ sinh bài (phase 2) không tạo được bước chấm điểm nào |

Cảnh báo (không chặn): `FEW_DISTRACTORS` (nghĩa khác trong unit + topic ít hơn 3, trắc nghiệm chỉ có 2 phương án), `MISSING_IPA`, `MISSING_AUDIO`, `MISSING_IMAGE`, `MISSING_EXAMPLE_TRANSLATION`, `EXAMPLE_WITHOUT_LEMMA` (câu ví dụ không chứa đúng từ nên sense này không có bước điền từ), `SAME_WORD_TWICE` (hai sense của cùng một item).

Ví dụ phản hồi:

```json
{
  "unitId": "3d9f…",
  "revision": 2,
  "canPublish": false,
  "errors": [
    { "code": "TOO_FEW_WORDS", "message": "Cần ít nhất 4 từ hợp lệ.", "senseId": null }
  ],
  "warnings": [
    { "code": "MISSING_AUDIO", "message": "Từ chưa có phát âm.", "senseId": "a1b2…" }
  ],
  "preview": { "wordCount": 3, "gradedStepCount": 6, "fillStepCount": 1, "exerciseTypes": ["CHOICE", "MATCH", "FILL"] }
}
```

`preview` lấy từ chạy khô `ExerciseGenerator` (phase 2) với một seed cố định. Phase 1 chưa có bộ sinh nên triển khai `NO_GRADED_STEP` và `preview` bằng một interface `ExercisePreview` có hai cài đặt: bản tạm trả `null`/không lỗi ở phase 1, thay bằng bộ sinh thật ở phase 2. Test của phase 2 phải phủ phần này.

**Phát hành** (một transaction): khóa dòng `vocabulary_study_units` bằng `SELECT ... FOR UPDATE`; chạy `validation`, nếu có lỗi chặn trả `422 VOCAB_UNIT_INVALID` kèm danh sách; điền `published_at = now`, `exercise_types`, `graded_step_count` cho revision nháp; `published_revision = draft_revision`, `draft_revision = null`; tăng `curriculum_version` của topic (insert dòng nếu chưa có: `INSERT ... ON CONFLICT (topic_id) DO UPDATE SET curriculum_version = vocabulary_study_topic_versions.curriculum_version + 1`). Không có nháp để phát hành: `409 VOCAB_UNIT_NO_DRAFT`.

**Lưu trữ:** đặt `archived_at = now`, tăng `curriculum_version`. Attempt đang làm và tiến độ cũ giữ nguyên. Không có thao tác bỏ lưu trữ trong v1.

**Sắp xếp:** body `{ "unitIds": [...] }` phải đúng bằng tập unit chưa lưu trữ của topic (thiếu hoặc thừa: `400`). Gán `sort_order = 1..n`, tăng `curriculum_version`. Hai request sắp xếp đồng thời phải cho kết quả nhất quán: khóa dòng topic trong `vocabulary_study_topic_versions` bằng `FOR UPDATE` (tạo dòng trước nếu chưa có).

## 5. API biên soạn (chỉ content author)

Controller `StudyUnitAuthoringController`, `@RequestMapping("/api/content/admin/vocabulary")`, `@PreAuthorize("hasRole('CONTENT_AUTHOR')")`. **Chỉ content author**, không cho `ADMIN` (khác `AdminVocabularyCatalogController` đang cho cả hai). Đường dẫn giữ tiền tố `/api/content/admin/vocabulary` là tiền tố biên soạn từ vựng sẵn có; gateway đã chuyển nó sang library-service và không chặn theo vai trò nên không cần đổi route.

| Phương thức, đường dẫn | Thành công | Ghi chú |
|---|---|---|
| `GET /topics/{topicId}/study-units` | 200 danh sách | Gồm nháp và đã lưu trữ, sắp theo `sort_order` (lưu trữ ở cuối) |
| `POST /topics/{topicId}/study-units` | 201 | Tạo nháp |
| `PUT /study-units/{unitId}` | 200 | Sửa theo quy tắc ở mục 4 |
| `PUT /topics/{topicId}/study-units/order` | 200 | Sắp xếp |
| `GET /study-units/{unitId}/validation` | 200 | Báo cáo kiểm tra |
| `POST /study-units/{unitId}/publish` | 200 | Phát hành nháp |
| `POST /study-units/{unitId}/archive` | 200 | Lưu trữ |

Tạo/sửa body:

```json
{
  "title": "Trong lớp học",
  "description": "Từ vựng thường gặp trong lớp học",
  "kind": "LEARN",
  "introductionVi": "Bài này giúp bạn gọi tên đồ vật và hoạt động trong lớp.",
  "senseIds": ["a1b2…", "c3d4…", "e5f6…", "0718…"]
}
```

Ràng buộc: `title` 1–120 ký tự, `description` ≤ 500, `introductionVi` không rỗng, `kind` thuộc `LEARN|REVIEW`, `senseIds` 4–60 phần tử. Dùng Bean Validation như các request biên soạn từ vựng sẵn có.

Phản hồi một unit (dành cho content author):

```json
{
  "unitId": "3d9f…",
  "topicId": "7a10…",
  "sortOrder": 3,
  "kind": "LEARN",
  "status": "PUBLISHED_WITH_DRAFT",
  "publishedRevision": 1,
  "draftRevision": 2,
  "revision": {
    "revision": 2,
    "title": "Trong lớp học",
    "description": null,
    "introductionVi": "…",
    "words": [
      { "senseId": "a1b2…", "lemma": "homework", "vietnameseMeaning": "bài tập về nhà", "sortOrder": 1 }
    ]
  },
  "updatedAt": "2026-11-07T09:30:00Z"
}
```

`revision` là bản đang sửa (nháp nếu có, nếu không là bản đã phát hành).

## 6. API đọc cho học viên

Controller `VocabularyStudyContentController`. Cả hai endpoint là `GET`, công khai (thêm vào `app.security.public-endpoints`), chỉ trả nội dung, không dữ liệu cá nhân.

`GET /api/content/vocabulary/topics/{topicId}/units`

```json
{
  "topicId": "7a10…",
  "curriculumVersion": 7,
  "totalUnits": 9,
  "uniqueWordCount": 72,
  "items": [
    {
      "unitId": "3d9f…", "topicId": "7a10…", "revision": 2,
      "title": "Trong lớp học", "description": null, "sortOrder": 2, "kind": "LEARN",
      "wordCount": 8, "estimatedMinutes": 6, "exerciseTypes": ["CHOICE", "MATCH", "FILL"]
    }
  ]
}
```

- Chỉ unit đã phát hành và chưa lưu trữ, theo `sort_order`. Topic không tồn tại hoặc `INACTIVE`: `404 VOCAB_TOPIC_NOT_FOUND`.
- `wordCount`: số sense `ACTIVE` (của item `ACTIVE`) trong revision đã phát hành.
- `uniqueWordCount`: số **item** (từ) khác nhau trong mọi unit đã phát hành của topic, không đếm trùng giữa unit học và unit ôn tập.
- `estimatedMinutes = max(1, ceil(wordCount * 0.75))`.
- `exerciseTypes`, lấy nguyên từ cột `exercise_types` đã tính lúc phát hành.
- `curriculumVersion`: từ `vocabulary_study_topic_versions` (0 nếu topic chưa có dòng).
- Tùy chọn: trả `ETag: "<curriculumVersion>"` và hỗ trợ `If-None-Match` trả 304.

`GET /api/content/vocabulary/units/{unitId}`

```json
{
  "unit": { "…": "như một phần tử items ở trên" },
  "introductionVi": "Bài này giúp bạn gọi tên đồ vật và hoạt động trong lớp.",
  "words": [
    {
      "vocabularyItemId": "9c1e…", "vocabularySenseId": "a1b2…", "sortOrder": 1,
      "lemma": "homework", "partOfSpeech": "NOUN",
      "vietnameseMeaning": "bài tập về nhà", "englishDefinition": "work given to students to do at home",
      "ipaUk": "/ˈhəʊmwɜːk/", "ipaUs": "/ˈhoʊmwɝːk/",
      "audioUkUrl": "https://…/vocabulary/pronunciation/uk/homework.mp3", "audioUsUrl": null,
      "imageUrl": null,
      "exampleSentence": "I finish my homework before dinner.",
      "exampleTranslationVi": "Tôi làm xong bài tập trước bữa tối."
    }
  ]
}
```

Truy vấn nội dung (lược):

```sql
SELECT w.sort_order, s.id AS sense_id, i.id AS item_id, i.lemma, s.part_of_speech,
       s.vietnamese_meaning, s.english_definition, s.example_sentence, s.example_translation, s.image_url,
       i.ipa, i.ipa_uk, i.ipa_us, i.audio_uk_reference, i.audio_us_reference, i.pronunciation_audio_reference
FROM vocabulary_study_unit_words w
JOIN vocabulary_senses s ON s.id = w.sense_id AND s.status = 'ACTIVE'
JOIN vocabulary_items  i ON i.id = s.vocabulary_item_id AND i.status = 'ACTIVE'
WHERE w.unit_id = :unit AND w.revision = :published_revision
ORDER BY w.sort_order
```

Quy tắc ánh xạ: `ipaUk = ipa_uk`, `ipaUs = ipa_us`; nếu cả hai null thì dùng `ipa` cho cả hai. `audioUkUrl = resolver.resolve(audio_uk_reference)`, `audioUsUrl = resolver.resolve(audio_us_reference)` (không dùng audio chung ở đây; hai trường là của từng giọng). Giá trị thiếu trả `null`, không bỏ khóa. Unit chưa phát hành, đã lưu trữ hoặc không tồn tại: `404 VOCAB_UNIT_NOT_FOUND`.

## 7. Các lớp cần tạo

`domain/study/StudyUnitRules`, `StudyUnitRevision` (value object); `application/port/StudyUnitRepository` (ghi) và `StudyUnitReader` (đọc); `infrastructure/adapter/StudyUnitJdbcAdapter`; use case `CreateStudyUnitUseCase`, `UpdateStudyUnitUseCase`, `ReorderStudyUnitsUseCase`, `ValidateStudyUnitUseCase`, `PublishStudyUnitUseCase`, `ArchiveStudyUnitUseCase`, `ListAuthoringStudyUnitsUseCase`, `ListTopicStudyUnitsUseCase`, `GetStudyUnitContentUseCase`; result record cho từng response; request/response DTO; `StudyRequestException` và handler (mục 7 của plan.md).

## 8. Cấu hình

- `library-service.yaml`: thêm hai đường dẫn công khai (kèm regex UUID, theo dòng 37–43 hiện có) và `/api/learning-support/vocabulary-study/**` vào `springdoc.paths-to-match`.
- `BusinessControllerRoleConventionTest`: **không cần sửa**. Test chỉ cấm `ADMIN` trong `@PreAuthorize`; controller mới chỉ dùng `CONTENT_AUTHOR` nên qua mà không cần ngoại lệ.

## 9. Kiểm thử (viết trước khi code)

1. `StudyUnitRulesTest`: từng lỗi chặn và từng cảnh báo; chuẩn hóa nghĩa trùng (khoảng trắng, hoa/thường, Unicode).
2. `StudyUnitMigrationTest` (mẫu `LibraryCatalogMigrationTest`): bảng, CHECK `published_revision IS NOT NULL OR draft_revision IS NOT NULL`, xóa sense đang dùng bị từ chối, xóa unit kéo theo revision và words.
3. `StudyUnitJdbcIntegrationTest` (mẫu `VocabularyNotebookJdbcIntegrationTest`):
   - tạo hai unit, `sort_order` 1 và 2;
   - phát hành, `curriculum_version` từ 0 lên 1;
   - sửa unit đã phát hành tạo revision 2 nháp, học viên vẫn thấy revision 1; phát hành revision 2 thì học viên thấy revision 2;
   - lưu trữ làm unit biến khỏi API học viên; sắp xếp lại ổn định; sắp xếp thiếu một unit bị từ chối;
   - hai thread cùng phát hành một unit chỉ phát hành một lần (cái còn lại `409 VOCAB_UNIT_NO_DRAFT`).
4. Test controller: học viên không gọi được API biên soạn (403); `ADMIN` không có vai trò `CONTENT_AUTHOR` cũng bị 403; `CONTENT_AUTHOR` gọi được; hai endpoint học viên truy cập không cần token; unit nháp, lưu trữ trả 404; nội dung đúng thứ tự tác giả đặt, `uniqueWordCount` không đếm trùng item giữa unit LEARN và REVIEW; sense INACTIVE bị bỏ khỏi `words` và `wordCount`.
5. `BusinessControllerRoleConventionTest` xanh (không có ngoại lệ mới).

## 10. Xong khi

- Toàn bộ API ở mục 5 và 6 chạy qua gateway với token thật.
- Học viên không bao giờ thấy nội dung nháp hoặc đã lưu trữ.
- `mvn -pl services/library-service -o clean test` xanh.
