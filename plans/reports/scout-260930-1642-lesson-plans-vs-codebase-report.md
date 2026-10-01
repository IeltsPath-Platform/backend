# Scout Report: codebase và các plan học theo bài (2026-09-30)

Phạm vi: code hiện tại + 6 plan (`260929-1640` nền, 5 plan mới `260930-*`) + thay đổi chưa commit trên `feat/main-follow`.
Cách làm: graphify (`graphify-out/graph.json`, build ở `0e3c44a4`; sau đó chỉ có commit docs nên graph còn đúng với code) +
3 Explore agent đọc 30 file phase và đối chiếu code. Các điểm đánh dấu ✔ đã được controller tự kiểm lại trên file.

## 1. Hiện trạng code

| Khu vực | Hiện có | Plan cần thêm |
| --- | --- | --- |
| content-service | Migration V1–V6. Có topic, KP (có band ở V5), question/version (`difficulty` nullable), package, asset (`AUDIO` có trong enum), `content_asset_links` XOR section/question_version | V7 bảng bài học + `TOPIC_TEST`, V8 seed, endpoint `/internal/learning-content/*` |
| assessment-service | Migration V1–V4. Snapshot section do **client gửi**; `AttemptStructureResponse` trả `answerSnapshot`; tạo result bởi học viên | Tự chấm, lấy đề từ content, bỏ `answerSnapshot` |
| access-service | V1–V2. `InternalAccessController` ở `/api/access` (debit/refund/consume/entitlement), chỉ test gọi | Chuyển sang `/internal/access/**`, kiểm subject |
| ai-learning | Migration V0_1–V9. Path theo **goal** (`POST /paths`), tutor SSE, practice notebook (`/api/ai-learning/practice/*`), consumer `AssessmentCompleted.v2` | V10 path theo user, V11 6 bảng bài học, `app/lessons/`, `practice_evidence.py` |
| Gateway | Chỉ route public; không route `/internal/**` (đã 404); chưa có test security WebFlux | Deny `/internal/**` |
| Contract | `assessment-completed-v2`, `practice-v1`, `tutor-sse-v1` | `lesson-learning-v1`, `learning-content-internal-v1`, `answer-spec-v1` (+ vectors), Writing contract |

**Chưa có dòng code nào của plan 1640.** Mọi plan `260930-*` đều bị 1640 chặn.

## 2. Thay đổi chưa commit

- `.sdd/database/DATABASE_V5.md`: thêm V5.3 (Listening + chọn gói theo dạng/độ khó), §5.18, `wrong_question_types`, index `lesson_exercise_submissions`.
- `.sdd/specs/SERVICE_ARCHITECTURE_V3.md`: bổ sung Listening, audio key, `difficulty`.
- `plans/260929-1640.../plan.md` (`blocks` 4 plan), `phase-06` (index V11 do plan hint yêu cầu).
- `services/ai-learning-service/README.md`: link tới `docs/ai-learning-database.md`.
- Untracked: `docs/ai-learning-database.md` (17 bảng hiện tại, V0_1–V9), `services/ai-learning-service/mvp-database.md` (thiết kế đích 11 bảng, MVP không tutor chat — file .md nằm ngoài `docs/`/`plans/`), 5 thư mục plan, 2 brainstorm report.

## 3. Plan và phụ thuộc

```text
1640 (nền, 8 phase) ─┬─► 0737 Writing T2 (5) ─► 0812 Writing T1 (3)
                     ├─► 0908 chọn gói theo dạng/độ khó (3) ─► 0851 Listening (5)
                     └─► 1006 gợi ý Reading (3)
```

Tất cả `pending`, chế độ `--tdd`. 0737 và 1006 đã có Validation Log; 1640 đã red-team.

## 4. File liên quan (theo service)

**content-service** (`services/content-service/src/main/java/com/group01/content/`)
- `api/controller/ContentAssetController.java` — GET/POST/links chưa có `@PreAuthorize`; GET trả `textContent` (lộ transcript) ✔
- `api/controller/QuestionController.java`, `ContentPackageController`, `KnowledgePointController`, `LearningVideoController` — 1640 P2 khóa quyền
- `domain/entity/QuestionVersion.java` (13 tham số), `domain/vo/QuestionDifficulty.java`, `domain/vo/PackageType.java` (chưa `TOPIC_TEST`)
- `application/usecase/GetGameContentSnapshotUseCase.java` — nhận **question id** (không phải version id), trả `answerSpec` + `explanation`
- `application/usecase/GetReadingPassageUseCase.java`, `AddQuestionVersionUseCase.java`, `CreateContentPackageUseCase.java`
- `infrastructure/persistence/` — `ContentAssetLinkJpaRepository` (chỉ tìm theo từng version), `QuestionVersionJpaEntity`, `QuestionPersistenceMapper`
- `src/main/resources/db/migration/V1..V6` — CHECK `knowledge_points.kind` ∈ GRAMMAR/VOCABULARY/STRATEGY/PRONUNCIATION ✔

**assessment-service** (`.../assessment/`)
- `application/usecase/StartAssessmentAttemptUseCase.java` — `execute` là `@Transactional` ✔
- `api/dto/response/AttemptStructureResponse.java` (trả `answerSnapshot`), `AssessmentResultController`, `CreateAssessmentResultUseCase` (+ test)
- `domain/.../AssessmentAttempt.java` (`expire()` rồi throw), finalize + event factory (chặn event khi không có goal)
- `api/.../GlobalExceptionHandler.java` — `InvalidAssessmentStateException` → 400; chưa có 422/503

**access-service**: `api/controller/InternalAccessController.java:22`, `LearnerAccessController.java` (`/api/access/me/points`), `DebitPointsUseCase`, `RefundPointsUseCase`, `GetUserEntitlementUseCase`

**ai-learning** (`services/ai-learning-service/`)
- `app/adapters/curriculum_scope.py` + 7 nơi import (1640 P5 xóa)
- `app/application/path_service.py`, `formal_assessment_ingestion.py` (tự tạo applier riêng ✔), `formal_result_applier.py`, `path_reorder.py`
- `app/persistence/postgres_learning_store.py` — `_sync_evidence_projection` DELETE + insert lại từ `state_json`
- `app/messaging/assessment_consumer.py`, `app/clients/content_service.py` (chỉ `get_reading_passage`, `get_curriculum`), `app/clients/user_service.py`
- `app/llm/client.py` (`supports_temperature`), `app/config.py` (`LlmSettings`)
- `app/mastery/models.py:168` `hints_used`; `main.py` (handler `ActiveGoalRequired`)
- `migrations/V5`, `V6` — `ON DELETE CASCADE` theo `mastery_paths` ✔

**Gateway/config**: `infra/api-gateway/.../security/SecurityConfig.java`, `infra/config-server/config-repo/{api-gateway,content-service,access-service}.yaml`

## 5. Phát hiện chính

### Plan 1640 (nền)
1. ✔ **Test mâu thuẫn:** P2 xóa `CreateAssessmentResultUseCase.execute` của học viên nhưng tiêu chí "không test cũ nào bị xóa" (`phase-02:67`); P4 ghi "giữ nguyên `CreateAssessmentResultUseCaseTest`" (`phase-04:67`).
2. ✔ **HTTP trong transaction:** P4 muốn gọi content ngoài transaction, nhưng `StartAssessmentAttemptUseCase.execute` là `@Transactional`; gọi nội bộ cùng bean thì proxy không tác dụng → cần tách bean.
3. ✔ **V10 cascade:** gộp path mỗi user sẽ xóa cả tutor sessions (V5) và practice notebook (V6) của path bị bỏ, không chỉ evidence.
4. ✔ **Hai applier (P7):** `FormalAssessmentIngestionService` tạo `FormalResultApplier` riêng nếu không truyền → consumer phải truyền `applier=`; file chưa có trong danh sách Modify.
5. P6: evidence bài học phải ghi vào aggregate `learning_evidence` (store sync lại bảng từ `state_json`); index `uq_mastery_evidence_source_reference` đã đủ, chỉ sửa `formal_source_reference`.
6. P5 bỏ sót: handler `ActiveGoalRequired`, `user_service.py` thành code chết, validator config đọc `user_service_base_url`.
7. P3: chặn game snapshot phải map question id → version trước khi đối chiếu; content chưa có test game snapshot.
8. P4: expire trả 400 (không 409); `GetAssessmentResultUseCase` cần query bản COMPLETED mới nhất; `QuestionKnowledgePointResponse/Result` còn được dùng, không xóa.

### Writing (0737, 0812)
1. **Refund vẫn là đường tạo point:** chỉ kiểm subject, `amount` tùy ý; internal JWT secret fallback công khai + access 8084 gọi thẳng được. Writing không dùng refund → nên tắt/hạn chế.
2. Debit song song cùng key → vi phạm UNIQUE → 500 (không replay); replay khác `amount` không bị chặn.
3. `reasoning_effort=low` mặc định (compose) → `supports_temperature` False → "temperature 0" không bao giờ gửi.
4. `AuthenticatedUser` không giữ token → router phải tự lấy bearer để forward cho access.
5. `GetUserEntitlementUseCase.execute(UUID)` không có command (plan nói có).
6. SVG data URI: frontend chỉ render qua `<img>`.

### Listening, độ khó, gợi ý (0851, 0908, 1006)
1. ✔ **Lỗ transcript có thật:** `GET /api/content/assets/{id}` mở cho mọi user đăng nhập.
2. ✔ **Seed KP Listening thiếu `kind`** (NOT NULL + CHECK) → phải chọn (đề xuất `STRATEGY`).
3. ✔ **0908 thiếu nguồn `questionType`:** payload bài học của 1640 P3 không có `questionType`; `answerSpec.type` chỉ phân biệt CHOICE/FILL.
4. **0908 nhánh `ALL_USED`** cần dạng/mức của gói đã giao nhưng `path_review_sets` không lưu và search loại gói đã giao.
5. ✔ 0908 ghi `limit` "từ 1 lên 20"; 1640 là mặc định 1, tối đa 10 (sửa câu chữ).
6. Listening P3 giả định `section_snapshot` có `passage`; 1640 P4 chưa định nghĩa dạng snapshot.
7. Listening P4: `extra="forbid"` không lọc `transcript` mà gây 500 → cần model riêng cho học viên.
8. Hints: neo code khớp hết; `QuestionController` chưa có `@PreAuthorize` (1640 P2 thêm).

### Xung đột chéo
| Điểm | Chi tiết |
| --- | --- |
| Migration content | 1640 V7/V8; Writing T2 **V9**, T1 **V10** (ghi cứng); Listening, Hints ghi `V<n>` |
| Migration ai-learning | 1640 V10/V11; Writing T2 **V12** (ghi cứng); 0908 thành **V13**, test "chạy sau V11" phải sửa |
| Version tài liệu ✔ | `DATABASE_V5.md` đã dùng V5.3 cho Listening + 0908; Writing T2 phase-05 cũng định V5.3; Hints V5.4 |
| Kiểm media | Listening `MediaUrlResolver` (key + base, `INVALID_MEDIA_REFERENCE`) vs Writing T1 (chỉ https/data URI, `INVALID_LESSON_BLOCK`); field `mediaUrl` vs `mediaReference` |
| File sửa chung | 2 contract bài học (cả 5 plan); `app/lessons/service.py`, `app/api/dto/lessons.py`, `app/clients/content_service.py`; use case/DTO nội bộ lấy bài; `GET /package-versions/{id}`; `tests/test_lesson_api.py` (mỗi plan assert tập key riêng) |
| `review_rule.py` | 0908 đổi chữ ký `reevaluate_reviews`; Listening P4 ghi "không đổi" |
| Tên "review" | Đã có `/api/ai-learning/practice/reviews` (notebook); 1640 thêm `/api/ai-learning/reviews/{id}` (bài ôn) — không đụng path nhưng dễ nhầm |

## 6. Đề xuất

- Chốt thứ tự merge sau 1640: **0908 → 1006 → 0851 → 0737 → 0812** (hoặc thứ tự khác) rồi ghi cứng số migration + version tài liệu vào từng plan.
- Gộp kiểm media thành một hàm/mã lỗi dùng chung trước khi làm Listening hoặc Writing T1 (plan vào trước viết, plan sau mở rộng).
- Sửa plan 1640 trước khi code: mục 1–6 phần "Plan 1640".
- Assert tập key trong `test_lesson_api.py` nên theo "chứa/không chứa key cấm" thay vì bằng đúng tập key, để các plan không vỡ nhau.

## Unresolved Questions

1. P2/P4 của 1640: xóa hay viết lại 4 test của `CreateAssessmentResultUseCaseTest`?
2. V10: giữ hay bỏ tutor sessions/notebook của path bị gộp?
3. Refund ở `/internal/access`: tắt, hay giới hạn theo role service? Replay có kiểm `amount`/`referenceId`?
4. 0908: `questionType` của câu bài học lấy từ đâu (thêm vào payload nội bộ bài học?); dạng/mức gói đã giao ở `ALL_USED` (thêm cột hay gọi content)?
5. Thứ tự merge và số migration/version tài liệu cuối cùng?
6. Field media chung (`mediaReference` hay `mediaUrl`) và mã lỗi chung?
7. `kind` của 4 KP Listening?
8. Dạng `section_snapshot` của 1640 P4 (có `passage`)?
9. Grader Writing: ép `reasoning_effort` để gửi temperature 0, hay chấp nhận?
10. `mvp-database.md` để trong `services/ai-learning-service/` hay chuyển sang `docs/`?
