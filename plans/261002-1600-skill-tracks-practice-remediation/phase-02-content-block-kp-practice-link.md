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

(Codex điền: bảng block→KP của L3/L4, bảng practice→lesson.)
