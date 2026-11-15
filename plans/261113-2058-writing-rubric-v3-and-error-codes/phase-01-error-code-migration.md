# Phase 1: Migration mã lỗi Writing

## Context

- Đề xuất Phần B (B1–B4). Quyết định D2, D4, D5 trong [plan.md](./plan.md).
- Schema: `error_types` (V45, V48 `review_status`, V123 `skills`/`fallback`, có `trap_knowledge_point_id`).
- Mẫu migration: V125 (insert theo bảng VALUES + JOIN KP + khối `DO $$` kiểm tra), V127 (ngừng dùng mã).
- `ErrorTagger` (learning, assessment) tự gắn `SPELLING`, `PLURAL_FORM` cho bài điền từ: hai mã này không được đổi.

## File

- Tạo `services/content-service/src/main/resources/db/migration/V132__writing_error_codes_model_definitions_and_tasks.sql`
- Tạo `services/content-service/src/test/java/com/ieltspath/content/infrastructure/persistence/WritingErrorCodeDefinitionsMigrationTest.java`

## Bước làm

1. **Schema**
   - `ALTER TABLE error_types ADD COLUMN model_definition TEXT` (null: grader dùng `fix_hint` như cũ).
   - `ADD COLUMN writing_tasks VARCHAR(10)[] NOT NULL DEFAULT '{}'` với
     `CHECK (writing_tasks <@ ARRAY['TASK_1','TASK_2']::varchar[])`. Mảng rỗng nghĩa là dùng cho cả hai task, nên
     mã Speaking/Reading/Listening không cần backfill.
2. **Sửa mã đang có (B3, theo quyết định đã chốt)**

   | Mã | Thay đổi |
   |---|---|
   | `NO_CLEAR_POSITION` | category → `TASK`; `writing_tasks = {TASK_2}` (KP `TR_W_OPINION` giữ) |
   | `PREPOSITION` | category → `VOCABULARY` (KP giữ) |
   | `TENSE` | KP → `G40_PAST_SIMPLE` |
   | `OFF_TOPIC` | KP → `TR_W_OPINION`; `writing_tasks = {TASK_2}` |
   | `VERB_FORM` | fix_hint mới theo B3 |
   | `LINKER_MISUSE` | KP → `G55_LINKING_WORDS` |
   | `SPELLING`, `PLURAL_FORM` | chỉ bỏ `WRITING` khỏi `skills`; KP, nhóm, fix_hint giữ nguyên |
   | `T1_*` | `writing_tasks = {TASK_1}` |
   | `T2_*`, `UNDERDEVELOPED_IDEA` | `writing_tasks = {TASK_2}` |

3. **Thêm mã mới**, `review_status = APPROVED`, `skills = {WRITING}`, chưa có trap KP (giống V125):
   - 8 mã của B4: `T1_IRRELEVANT_CONTENT`, `DATA_LANGUAGE` (TASK_1), `MISSING_VERB`, `PERFECT_TENSE`,
     `RUN_ON_SENTENCE`, `REFERENCE`, `REPETITION`, `INFORMAL_REGISTER` (cả hai task). Nhóm và KP đúng như bảng B4.
   - 2 mã thay cho mã dùng chung: `WRITING_SPELLING` (VOCABULARY, KP trống vì chưa có KP chính tả Writing, sev 1)
     và `WRITING_PLURAL_FORM` (GRAMMAR, KP `G55_ARTICLES`, fix_hint theo B3).
   - Tên và fix_hint tiếng Việt viết theo cột "Dùng khi" của B4.
4. **`model_definition` (tiếng Anh)** cho mọi mã có `WRITING` trong `skills`, kể cả 4 mã fallback. Mỗi mã 1–3 câu
   theo khung: `Use when … Do not use when … (use X). Example: "…" → "…".` Đưa luật ưu tiên B2 vào định nghĩa từng
   mã (không sửa template). Ví dụ lấy từ E01 khi B4 có.
5. **Khối `DO $$` kiểm tra** (giống V125/V126):
   - đủ 10 mã mới, ACTIVE, có `WRITING`;
   - mọi mã có KP thì KP, topic, course đều ACTIVE;
   - mọi mã Writing đang ACTIVE đều có `model_definition`;
   - `SPELLING`, `PLURAL_FORM` không còn `WRITING`.

## Test

Mẫu theo `WritingWordFormMigrationTest` (Testcontainers, Flyway migrate cả thư mục):
- 10 mã mới tồn tại, đúng nhóm, KP và `writing_tasks`.
- `SPELLING` còn KP `LS_SPELL` và nhóm `SPELLING_NUMBER`; `PLURAL_FORM` giữ fix_hint "theo đề bài".
- Không mã Writing ACTIVE nào thiếu `model_definition`.
- `ErrorTrapCoverageMigrationTest` vẫn xanh (mã mới không có trap KP).

## Rủi ro

- Định nghĩa cho model là nội dung mới, chưa qua examiner. Ghi vào comment đầu migration giống V125.
- KP `G40_PAST_SIMPLE`, `W50_COMPOUND_SENTENCES`, `W80_ACADEMIC_STYLE` nằm ở các course content migration
  (V105, V108, V110). Nếu topic/course chưa ACTIVE thì khối DO dừng migration; khi đó chọn KP khác.
- Rollback: migration mới, đặt các mã mới INACTIVE và trả KP cũ. Hai cột mới có thể để nguyên.
