---
phase: 8
title: "Lưu ghi chú từ buổi học vào notes của learning-support"
status: completed
priority: P3
dependencies: [4]
effort: "~1.5d"
---

# Phase 8: Lưu ghi chú từ buổi học vào notes của learning-support

## Overview
Học viên bảo tutor "lưu lại đoạn này". Tutor soạn một **bản nháp note** (tiêu đề và nội dung) rồi gửi qua SSE. **Frontend**
lưu bản nháp thành note của học viên bằng API notes sẵn có của `learning-support-service`, kèm nguồn (KP hoặc buổi học), để
lọc lại được. Cùng cách làm với pha 10 (phương án A): **AI Learning không gọi learning-support**.

Hai phần việc:
1. **learning-support-service (Java):** note có thêm nguồn (`sourceType`, `sourceReferenceId`); lọc được theo nguồn.
2. **ai-learning-service (Python):** tool `save_note` phát event `note.draft`; không lưu note, không gọi HTTP ra ngoài.

## Ràng buộc bắt buộc
- Không import hay chép code DeepTutor. Không thêm `third_party` vào `sys.path`, `PYTHONPATH`, requirements, Dockerfile,
  compose. Gate `tests/test_no_deeptutor_dependency.py` pass; `git grep -nE "deeptutor|third_party" --
  services/ai-learning-service/app services/ai-learning-service/tests ':!*.pyc'` không có dòng mới do pha này thêm.
- Không log title/body của note, cả ở Python lẫn Java.
- Giữ nguyên hành vi hiện có của API notes (tạo, sửa, xóa, list không lọc) và của các tool tutor khác.

## Sửa so với bản cũ của pha này (đã kiểm với code 2026-09-27)

| # | Bản cũ | Bằng chứng | Bản mới |
| --- | --- | --- | --- |
| S1 | AI Learning gọi thẳng `POST /api/learning-support/notes` bằng internal JWT của học viên. | Internal JWT do Gateway ký chỉ sống **60 giây** (`INTERNAL_TOKEN_MAX_AGE_SECONDS`, mặc định 60, tối đa 300: `infra/config-server/config-repo/api-gateway.yaml:102`, `AuthProperties.java:26`). Một lượt tutor có tới 6 vòng LLM, nên token dễ hết hạn trước khi tool chạy. `run_turn` hiện cũng không nhận bearer, AI Learning chưa có cấu hình URL learning-support, và docker-compose chỉ có `learning-support-db`. | Tool `save_note` chỉ phát event `note.draft`; **frontend** lưu bằng token còn hạn của chính học viên. Không có client, cấu hình hay lời gọi xuyên service mới. |
| S2 | `source_reference_id varchar(255)`; CHECK 4 giá trị `TUTOR_SESSION`, `KNOWLEDGE_POINT`, `PRACTICE`, `READING`. | `flashcards.source_reference_id` là `uuid` và không có CHECK trên `source_type` (Java enum validate). Mọi nguồn ở đây đều là uuid. `PRACTICE` đã được pha 10 lo bằng flashcard; `READING` thuộc pha 9 (đang blocked). | `source_reference_id uuid`, chỉ 2 giá trị enum `TUTOR_SESSION`, `KNOWLEDGE_POINT` (pha 9 thêm `READING` sau). Không CHECK giá trị trong DB, giống flashcards. Chỉ CHECK "hai cột cùng null hoặc cùng có". |
| S3 | "Nguồn là session hiện tại (và KP đang học)": hai nguồn cho một note. | Bảng chỉ có một cặp `source_type`/`source_reference_id`. | Một nguồn cho mỗi note: có `knowledge_point_id` → `KNOWLEDGE_POINT`; không có → `TUTOR_SESSION` (id session). |

## Quyết định đã chốt

| # | Quyết định |
| --- | --- |
| N1 | Tool chỉ dùng khi **học viên yêu cầu lưu**. Bản nháp là nội dung học viên muốn giữ (lời giải thích, tóm tắt, ví dụ), không phải tóm tắt tự phát. |
| N2 | Frontend nhận `note.draft` thì **tự lưu ngay** (học viên đã chủ động yêu cầu), rồi báo "Đã lưu" kèm link tới note. Không có bước xác nhận thêm. |
| N3 | Nguồn bất biến sau khi tạo: `PUT /notes/{id}` giữ nguyên nguồn (`UpdateNoteRequest` không đổi). |
| N4 | Không chống trùng: lưu hai lần thì có hai note (khác pha 10). Học viên xóa được như note thường. |
| N5 | Chặn lộ đáp án: nếu session đang có câu hỏi mastery **chưa chấm** và `expected_answer` (≥ 3 ký tự, không phân biệt hoa thường) nằm trong title hoặc body → tool trả lỗi, không phát event. |
| N6 | Không lưu bản nháp vào DB của AI Learning. Client rớt mạng thì mất bản nháp; học viên nhờ lưu lại. |

## Phần A: learning-support-service (Java)

### Đọc trước (trong `services/learning-support-service/src/main/java/com/group01/learningsupport`)
- `domain/aggregate/Note.java` (`create`, `update` trả `dropLinks`), `domain/vo/FlashcardSourceType.java` (mẫu enum nguồn).
- `domain/repository/NoteRepository.java`, `infrastructure/persistence/entity/NoteJpaEntity.java`,
  `infrastructure/persistence/mapper/NoteMapper.java` (MapStruct `copy`, `toDomain`),
  `infrastructure/persistence/repository/NoteJpaRepository.java` và adapter của `NoteRepository`.
- `application/usecase/CreateNoteUseCase.java`, `ListNotesUseCase.java`, `UpdateNoteUseCase.java`,
  `application/result/NoteResult.java`.
- `api/controller/NoteController.java`, `api/dto/request/CreateNoteRequest.java`, `api/dto/response/NoteResponse.java`.
- `src/main/resources/db/migration/V1__create_learning_support_tables.sql` (bảng `notes`).
- Test mẫu: `api/controller/NoteSecurityTest.java`, `application/usecase/PersonalLibraryUseCaseTest.java`,
  `infrastructure/persistence/LearningSupportSchemaTest.java`.

### Migration `V{n}__note_source.sql`
`n` = số phiên bản kế tiếp còn trống trong `src/main/resources/db/migration/` (pha 10 cũng thêm migration ở service này).

```sql
-- Where a note came from, so a learner can find notes saved from a tutor session or about a knowledge point.
-- Existing notes have no source. Values are validated by the NoteSourceType enum, as flashcard sources are.
ALTER TABLE notes ADD COLUMN source_type varchar(50);
ALTER TABLE notes ADD COLUMN source_reference_id uuid;
ALTER TABLE notes ADD CONSTRAINT chk_notes_source_pair
    CHECK ((source_type IS NULL) = (source_reference_id IS NULL));
CREATE INDEX idx_notes_user_source
    ON notes (user_id, source_type, source_reference_id, updated_at DESC)
    WHERE source_type IS NOT NULL;
```

### Code
1. `domain/vo/NoteSourceType.java`: `enum NoteSourceType { TUTOR_SESSION, KNOWLEDGE_POINT }`.
2. `Note`:
   - thêm `private final NoteSourceType sourceType; private final UUID sourceReferenceId;`
   - `create(userId, title, body, sourceType, sourceReferenceId)` validate cặp: cùng `null` hoặc cùng có; lệch →
     `InvalidDataException` (tức 400).
   - Giữ overload `create(userId, title, body)` gọi bản mới với `null, null`, để caller và test cũ không đổi.
   - `update` không đụng nguồn (N3).
3. `NoteJpaEntity` thêm 2 cột (`@Enumerated(EnumType.STRING)` cho `sourceType`). MapStruct map theo tên, nên kiểm lại
   `copy`/`toDomain` đã map đủ (có test persistence).
4. `NoteResult`, `NoteResponse` thêm `sourceType`, `sourceReferenceId` (null khi không có nguồn).
5. `CreateNoteRequest` thêm `NoteSourceType sourceType` và `UUID sourceReferenceId`, cả hai tùy chọn.
   `CreateNoteUseCase.execute(userId, title, body, sourceType, sourceReferenceId)`.
6. List:
   - `GET /api/learning-support/notes?status=&page=&size=&sourceType=&sourceReferenceId=`.
   - Có `sourceReferenceId` mà thiếu `sourceType` → 400 (`InvalidDataException`). `sourceType` sai giá trị → 400 như
     các enum param khác.
   - `NoteRepository` và JPA repository thêm hàm lọc theo `(userId, status, sourceType)` và
     `(userId, status, sourceType, sourceReferenceId)`, giữ sắp xếp như `findByUserIdAndStatus`.
   - Không truyền nguồn → hành vi cũ nguyên vẹn.

### Tests (Java)
1. Unit (`Note`): tạo có nguồn, không nguồn, lệch cặp → `InvalidDataException`; `update` giữ nguồn.
2. Web (`@WebMvcTest(NoteController)`, theo mẫu `NoteSecurityTest`): POST có nguồn → 201, response có `sourceType`,
   `sourceReferenceId`; `sourceType` sai → 400; chỉ có một trong hai trường → 400; GET lọc `sourceType` và cả cặp →
   use case nhận đúng tham số; `sourceReferenceId` thiếu `sourceType` → 400; test cũ pass nguyên.
3. Persistence (Testcontainers, theo mẫu `LearningSupportSchemaTest`): có 2 cột, `chk_notes_source_pair`,
   `idx_notes_user_source`; note cũ (chèn trước migration hoặc chèn không nguồn) vẫn đọc được với nguồn null; lọc theo
   nguồn chỉ trả note của đúng user.

## Phần B: ai-learning-service (Python)

### Đọc trước
- `app/tutor/tools.py` (`TOOL_DEFINITIONS`, `ToolOutcome`, `execute`, `find_knowledge_point`, `_practice_questions` làm
  mẫu validate và event), `app/tutor/engine.py` (event của tool không kết thúc lượt được yield ngay), `app/tutor/prompts.py`.
- `docs/contracts/tutor-sse-v1.md`.

### Tool `save_note`

```text
save_note(title: str, body: str, knowledge_point_id?: str)
```
- `title`: trim, bắt buộc, cắt 255 ký tự. `body`: trim, bắt buộc. Nếu dài hơn 19.900 ký tự thì cắt còn 19.900 và thêm
  `"\n\n[truncated]"` (chừa biên vì Java `@Size` đếm UTF-16 code unit, Python đếm code point).
- `knowledge_point_id` có → phải có trong path (`find_knowledge_point`), sai → error; nguồn = `KNOWLEDGE_POINT`, id KP.
  Không có → nguồn = `TUTOR_SESSION`, id session.
- N5: đọc `tx.active_interaction()` của path; nếu có, trạng thái chưa `graded`, `session_id` là session này, và
  `question.expected_answer` dài ≥ 3 ký tự nằm trong title hoặc body (casefold) → error
  `"The note would reveal the answer to the open question; save it after grading."`.
- Hợp lệ → `ToolOutcome({"status": "offered", "note": "The learner's app saves this note."},
  events=[("note.draft", {"title", "body", "source_type", "source_reference_id"})])`. **Không** `ends_turn`.
- Tool không đọc hay ghi DB nào ngoài lần đọc path/interaction ở trên.

### Prompt (thêm vào `SYSTEM_PROMPT`, trước mục "Style")

```text
Saving notes:
- When the learner asks you to save or remember something for later, call `save_note` with a short title and the
  content they want to keep, written clearly for re-reading later. Pass `knowledge_point_id` when the note is about
  a knowledge point. Only save when the learner asks.
- Never put the answer to a question the learner has not answered yet into a note.
- After calling it, tell the learner briefly that the note is being saved to their notes.
```

### Docs
- `docs/contracts/tutor-sse-v1.md`:
  - `tool.called` thêm `save_note`.
  - Thêm event `note.draft`: `{ "title": "...", "body": "...", "sourceType": "KNOWLEDGE_POINT|TUTOR_SESSION",
    "sourceReferenceId": "uuid" }`.
  - Ghi luồng frontend: nhận event → `POST /api/learning-support/notes` với đúng 4 trường → báo "Đã lưu"; lỗi thì báo
    học viên và cho thử lại.
- `services/ai-learning-service/README.md` mục Tutor: thêm `save_note` (N1, N2, N5, N6).
- `.sdd/database/DATABASE_V5.md` §8.5 `notes`: thêm 2 cột, CHECK cặp, index, giá trị enum; ghi rõ nguồn do frontend gửi
  theo `note.draft` của tutor.

### Tests (Python)
Thêm vào `tests/test_tutor_engine_postgres.py` (hoặc file mới `tests/test_tutor_save_note_postgres.py`, theo mẫu
`ScriptedChat`, `call(...)`):

4. Model gọi `save_note(title, body, knowledge_point_id=KP_BASIC)` rồi trả text → SSE có `tool.called`, `note.draft` với
   `sourceType=KNOWLEDGE_POINT`, `sourceReferenceId=KP_BASIC`, rồi `assistant.message`, `turn.completed`.
5. Không có `knowledge_point_id` → `sourceType=TUTOR_SESSION`, `sourceReferenceId` = id session.
6. KP không có trong path, title rỗng hoặc body rỗng → tool error, không có `note.draft`, lượt vẫn completed.
7. Body 25.000 ký tự → body trong event dài ≤ 19.900 + độ dài hậu tố và kết thúc bằng `[truncated]`.
8. Session có câu quiz đang mở (đặt bằng `mastery_quiz` ở lượt trước) với đáp án "has gone"; note chứa "Has gone" →
   error, không có event. Sau khi chấm xong, lưu lại được.
9. Không có HTTP call nào ra ngoài (không cần mock client vì tool không có client).

## Implementation Steps
1. Java: test trước (1–3), migration, enum, domain, entity/mapper, result/DTO, use case, repository, controller.
2. Python: test trước (4–9), tool `save_note`, prompt.
3. Docs (tutor-sse, README, DATABASE_V5).
4. Gate:
   - `mvn -pl services/learning-support-service -am test` (Testcontainers cần Docker, không được skip; không có Docker
     thì dừng và báo)
   - trong `services/ai-learning-service`: `python -m pytest tests -rs` (0 fail, 0 skip; cần PostgreSQL và RabbitMQ),
     `python -m compileall -q app`, lệnh `git grep` ở mục ràng buộc
   - `git diff --check`; ở root repo: `graphify update .`
5. **Một commit**: `feat(tutor): save tutor notes to the learner's notebook`.

## Success Criteria
- [x] Học viên nhờ tutor lưu, frontend nhận `note.draft` và tạo được note có nguồn trong learning-support.
- [x] Note lọc được theo `sourceType` (và `sourceReferenceId`); note cũ và API notes cũ không đổi hành vi.
- [x] Nguồn giữ nguyên khi sửa note; cặp nguồn lệch bị từ chối ở cả API lẫn DB.
- [x] Note không bao giờ chứa đáp án của câu hỏi đang mở (có test).
- [x] AI Learning không gọi learning-support; không phụ thuộc DeepTutor; tài liệu đã cập nhật.

## Risk Assessment
- **Frontend không lưu** (rớt mạng, lỗi): mất bản nháp (N6). Học viên nhờ lưu lại; sự cố hiện rõ trên UI.
- **Chặn lộ đáp án chỉ là so khớp chuỗi** (N5): diễn đạt khác đi vẫn lọt; prompt là lớp phòng thủ còn lại. Đáp án choice
  chỉ là nhãn (1 ký tự) nên không so được; phần giải thích lựa chọn vẫn dựa vào prompt.
- **Trùng số migration với pha 10**: lấy số kế tiếp còn trống lúc implement.
- Rollback: revert commit; migration chỉ thêm cột nullable, CHECK và index, nên xóa được bằng `ALTER TABLE ... DROP`.
