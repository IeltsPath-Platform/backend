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

Chỉ dùng cho block **TEXT, ASSET, VOCABULARY**. EXERCISE không ghi vào đây (KP lấy từ `question_knowledge_points`).
Validation ở tầng application: KP gắn vào block phải thuộc `lesson_knowledge_points` của lesson đó.

Seed (theo `code` lesson + `sort_order` block, có `WHERE EXISTS`):

| Lesson | Quy tắc gắn |
| --- | --- |
| Lesson chỉ có 1 KP (đa số seed: L1, L2, TF1, LS1, LS2, PM1, PM2, PS1, PS2, W1, W2) | mọi TEXT/ASSET block → KP đó. |
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

Seed thêm (để mỗi lesson Reading/Listening có ≥ 1 practice và test được thang ôn tập): với lesson chưa có practice
(TF1, PM1, PM2, PS1, PS2) **không bắt buộc** seed thêm trong phase này; ghi danh sách vào Verification. KP5
(TFNG) cố ý không có practice set – giữ nguyên (đây là case "review bị SKIPPED vì hết set").

## 2. Internal lesson DTO

`GET /internal/learning-content/lessons/{id}`: mỗi block thêm `knowledgePointIds: UUID[]`:

- TEXT/ASSET/VOCABULARY: từ `lesson_block_knowledge_points` (có thể rỗng);
- EXERCISE: hợp các `knowledgePointIds` của câu hỏi trong block (đã có trên question).

Lesson thêm `skill` (của topic). Chỉ thêm field.

## 3. Practice theo lesson (internal)

- `GET /internal/learning-content/lessons/{id}/practice-sets` → danh sách
  `{packageId, code, title, packageVersionId, questionCount, knowledgePointIds, requiredFeatureKey}` cho PRACTICE_SET
  PUBLISHED có `lesson_id = id`, sắp theo `code`. Không trả đáp án.
- `POST /practice-sets/search` thêm tham số tuỳ chọn `preferredLessonId`: nếu có, package gắn lesson đó xếp trước;
  ngoài ra giữ nguyên hành vi (KP, exclude, minQuestions, loại package chứa câu đã dùng trong lesson/topic test).
- Package version (`/package-versions/{id}`) giữ nguyên; item phải có `hint` (đã có từ V13) để Learning hiển thị.

## 4. Admin authoring

API tạo/sửa package nhận `lessonId` (chỉ với PRACTICE_SET; lesson phải PUBLISHED hoặc DRAFT cùng skill với câu hỏi).
API gắn KP cho block TEXT/ASSET (nếu có controller authoring lesson block; nếu chưa có thì chỉ seed + ghi chú).

## 5. Test

- Migration: bảng/constraint tồn tại; backfill `lesson_id` đúng bảng mong đợi; CHECK chặn `lesson_id` trên MOCK_TEST.
- Reader: block TEXT có `knowledgePointIds`; EXERCISE có hợp KP câu hỏi; `lessons/{id}/practice-sets` trả đúng gói và
  không lộ `answerSpec`/`explanation`.
- `search` với `preferredLessonId` xếp gói của lesson trước.

## Acceptance

`mvn -q -pl services/content-service -am test` xanh; contract mục P6 cập nhật đủ field mới.

## Verification

(Codex điền: bảng block→KP của L3/L4, bảng practice→lesson.)
