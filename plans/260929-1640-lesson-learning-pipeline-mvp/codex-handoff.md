# Giao việc cho agent code: plan 1640 (nền MVP)

Mỗi PR chạy trong **một phiên agent mới**. Dán nguyên khối "Prompt" của PR đó. Prompt trỏ tới mục "Quy tắc chung" ngay dưới,
nên agent phải đọc file này trước.

## Quy tắc chung (mọi PR)

1. **Đọc trước khi code:** `AGENTS.md`, `CLAUDE.md`; thêm `services/ai-learning-service/CLAUDE.md` nếu chạm ai-learning;
   `plans/260929-1640-lesson-learning-pipeline-mvp/plan.md` (bảng "Quyết định áp dụng" và các Validation Log); file phase của PR;
   `plans/260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md` nếu PR có seed hoặc test theo kịch bản.
2. **Plan là nguồn quyết định.** Không tự đổi quyết định đã chốt. Plan và code mâu thuẫn, hoặc plan thiếu thông tin để làm tiếp
   → dừng, báo `NEEDS_DECISION` kèm lựa chọn, không đoán. Số dòng trong plan (`file.py:80-93`) chỉ là gợi ý và có thể lệch
   sau PR trước: tìm theo tên class/hàm.
3. **TDD theo phase:** chạy "Tests Before" (khóa hành vi cũ) → viết "Tests After" và thấy chúng fail → code → "Regression Gate".
   Không nới lỏng hay xóa test để pass, trừ test mà phase ghi rõ là xóa.
4. **Cấm:** tạo service mới hay chuyển bảng/entity giữa service trong PR của plan này (việc chia service có plan riêng);
   sửa migration đã có; sửa `services/ai-learning-service/app/mastery/*`; import `deeptutor`; test gọi LLM thật; log
   nội dung câu trả lời, prompt, token; ghi mã plan, số phase hay mã review vào code comment, tên test, tên migration, commit message;
   commit `.env`, `__pycache__`, `*.pyc`, `graphify-out/`.
5. **Môi trường test:**
   - Java: Docker phải chạy (Testcontainers). Lệnh `mvn -q -pl services/<service> -am test`.
   - ai-learning: venv Python 3.11 riêng, `python -m pip install pytest -r requirements-test.txt`, đặt
     `PYTHONDONTWRITEBYTECODE=1`, `AI_LEARNING_TEST_DATABASE_URL`, `AI_LEARNING_TEST_AMQP_URL`, chạy
     `python -m pytest tests -p no:cacheprovider` trong `services/ai-learning-service`.
   - Thiếu Docker hay biến môi trường thì test bị **skip**: báo rõ test nào chưa chạy, **không** báo là pass.
6. **Git:** nhánh mới từ `feat/main-follow` (hoặc từ nhánh của PR trước nếu chưa merge), tên `feat/lesson-<việc>`. Conventional
   commit, không nhắc AI hay agent. Không push khi chưa được yêu cầu.
7. **Contract:** đổi tên hay kiểu trường so với `docs/contracts/*` thì sửa contract trong cùng commit.
8. **Sau khi code:** nếu có `graphify`, chạy `graphify update .`.
9. **Báo cáo cuối** (bắt buộc, theo mẫu):
   ```text
   Status: DONE | BLOCKED | NEEDS_DECISION
   Đã làm: <tóm tắt theo mục Requirements của phase>
   File đổi: <danh sách>
   Test đã chạy: <lệnh> → <kết quả thật, số pass/fail/skip>
   Lệch plan: <chỗ làm khác plan và lý do, hoặc "không">
   Cần người duyệt: <nếu có>
   ```

## Thứ tự PR

| # | PR | Phase | Cần có trước | Điểm dừng | Trạng thái |
| --- | --- | --- | --- | --- | --- |
| 1 | Contract | 1 | — | **Dừng: người dùng duyệt contract** | Xong (duyệt 2026-10-01) |
| 2 | Chặn lộ đáp án | 2 | PR 1 duyệt | — | Xong (merge #23) |
| 3 | Content bài học, gói, seed | 3 | PR 1 duyệt | — | Xong (merge #24) |
| 4 | AI Learning: tách module, adapter, placement | 5 (phần a) | PR 1 duyệt | — | Xong (merge #25) |
| 5 | AI Learning: path theo user, V10 | 5 (phần b) | PR 3, PR 4 | **Dừng: người dùng duyệt V10 trước khi chạy trên DB dùng chung** | **Bỏ** (thay bởi `261001-1228`) |
| 6 | AI Learning: bài học, nộp bài, cổng | 6 (phần a) | PR 3, PR 5 | — | **Bỏ** (thay bởi `261001-1228`) |
| 7 | AI Learning: bài ôn, giao mã đề | 6 (phần b) | PR 6 | — | **Bỏ** (thay bởi `261001-1228`) |
| 8 | Assessment tự chấm, event | 4 | PR 3 và phase 4 của `261001-1228` (consumer Java nhận event không goal) | — | Chưa (làm sau `261001-1228` phase 4) |
| 9 | Consumer kết quả đề | 7 | PR 7, PR 8 | — | **Bỏ** (thay bởi `261001-1228`) |
| 10 | Tài liệu, kiểm toàn repo | 8 | PR 8, `261001-1228` | E2E thủ công do người dùng chạy | Chưa |

**Đổi 2026-10-01:** ai-learning Python bị bỏ; prompt PR 5–7, 9 bên dưới không dùng nữa. PR 8, PR 10 đọc phần ai-learning theo
[bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md); prompt learning-service ở
[`261001-1228/codex-handoff.md`](../261001-1228-learning-service-java/codex-handoff.md).

PR 2, 3, 4 làm song song được sau khi PR 1 được duyệt, nếu mỗi PR một nhánh và không sửa cùng file.

---

## PR 1 — Contract

```text
Làm phase 1 của plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-01-contract-va-dac-ta.md.
Trước tiên đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.

Phạm vi: chỉ tài liệu trong docs/contracts/ (4 file mới, sửa assessment-completed-v2.md). Không sửa code.
- Tên trường giữ khớp code hiện có (đọc các controller/DTO phase 1 liệt kê).
- Ví dụ JSON dùng dữ liệu thật của seed-content.md (L1, Q12, mã đề X1, gói PS-KP1-A) thay vì ví dụ bịa.
- answer-spec-v1-vectors.json: ≥ 15 ca, gồm các câu của seed-content.md và câu V4 dạng {"correct":"A"}.
- topic-sequence có hasPracticeSet; options = null là câu điền; luật KP không có gói luyện (Validation Session 3).

Xong thì DỪNG, báo Status: NEEDS_DECISION và liệt kê các thay đổi contract cần người dùng duyệt (event, request/response
assessment, bỏ endpoint học viên mở result, API học viên mới). Không làm phase khác.
```

## PR 2 — Chặn lộ đáp án

```text
Làm phase 2 của plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-02-chan-lo-dap-an.md.
Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.
Contract ở docs/contracts/ đã được duyệt.

Phạm vi file: content-service (5 controller), assessment-service (structure bỏ answerSnapshot, xóa route học viên mở result
và đường execute của học viên), infra/api-gateway (deny /internal/** trong SecurityWebFilterChain, không đổi route).
Test: ContentAuthorizationWebMvcTest dùng security thật (không standaloneSetup); test Gateway cho /internal/**.
Chỉ xóa các test execute của học viên trong CreateAssessmentResultUseCaseTest; test người chấm giữ nguyên.
Regression Gate: 3 lệnh mvn trong phase.
```

## PR 3 — Content: bài học, gói, endpoint nội bộ, seed

```text
Làm phase 3 của plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-03-content-bai-hoc-va-goi.md.
Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.

Phạm vi: chỉ services/content-service.
- V8 (schema, bỏ band KP, CHECK package_type giữ LESSON thêm TOPIC_TEST) và V9 (seed). V7 là migration xóa bảng từ vựng/video
  của plan chia service (đã merge trước); không sửa V7.
- Seed V9: lấy ĐÚNG nội dung các mục ghi "V9 (1640)" trong
  plans/260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md (đoạn văn, câu, options, answer_spec, explanation,
  khối bài học, 6 gói luyện, 3 mã đề X1/X2/X5). Không seed mục V10–V13. UUID prefix 20000000-0000-4000-8000-…. KP mới
  kind = STRATEGY. Không sửa V4, V6.
- 6 endpoint /internal/learning-content/*; topic-sequence có hasPracticeSet dùng chung predicate SQL với practice-sets/search.
- Không N+1: mỗi endpoint số query cố định, lấy theo lô bằng IN. Không gọi repository trong vòng lặp.
- Test seed: đáp án từng câu khớp seed-content.md (chấm bằng bộ vector answer-spec-v1), hasPracticeSet KP1–KP4 true, KP5 false.
Trước khi chặn câu ở game snapshot, grep game-service xem đang dùng câu nào (Risk Assessment của phase).
Regression Gate: mvn -q -pl services/content-service -am test.
```

## PR 4 — AI Learning: tách module, adapter chịu null, placement

```text
Làm PHẦN A của phase 5 plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-05-ai-learning-path-theo-user.md.
Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.

Phần A gồm (theo mục Risk Assessment "tách thành 2 PR" của phase):
- tách curriculum_scope.py: KnowledgePointDetails (thêm has_practice_set: bool = False) và parse_band sang
  app/adapters/knowledge_point_details.py; OrderingValidator sang app/learning/ordering_validator.py (chuyển test);
- adapter event chịu learning_goal_id null/thiếu, đọc package_version_id nếu có;
- PLACEMENT không ghi evidence, không test-out;
- chiếu evidence và replace_knowledge_point_details ghi theo lô (execute_values).
KHÔNG làm ở PR này: path theo user, V10, xóa PathOrderer/LLM ordering, bỏ goal (để PR 5).
Mọi test hiện có (kể cả tutor) phải pass. Không sửa app/mastery.
Regression Gate: pytest ai-learning đủ biến DB và AMQP.
```

## PR 5 — AI Learning: path theo user, V10

```text
Làm PHẦN B của phase 5 plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-05-ai-learning-path-theo-user.md
(phần A đã merge). Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.

Phần A ĐÃ XONG (xem mục "Kết quả phần a" của phase), không làm lại: knowledge_point_details.py, ordering_validator.py,
adapter chịu goal null + package_version_id, PLACEMENT không ghi evidence, placement_test_out đã xóa, execute_values.

Phần B gồm:
- ContentServiceClient.get_topic_sequence (GET /internal/learning-content/topic-sequence, contract
  docs/contracts/learning-content-internal-v1.md); đổi response sang topics + knowledgePoints rồi qua CurriculumAdapter.to_modules
  như cũ; snapshot details lấy từ cùng response (details_from_content đã đọc hasPracticeSet).
- PathService: ensure_path/ensure_active_path theo user, không goal, không LLM, không band; refresh gộp từ cùng topic-sequence.
  Constructor chỉ nhận keyword. Bỏ user_client, _active_goal, _scoped_curriculum, PathOrderer, ActiveGoalRequired.
- Store: find_path_by_user; advisory lock theo user; bắt UniqueViolation trên index mới; pending theo user (áp khi tạo path và
  khi nạp path có sẵn); replace_knowledge_point_details ghi thêm has_practice_set, knowledge_point_details đọc thêm cột; xóa
  replace_knowledge_point_bands / knowledge_point_bands và mọi tham số bands, band_min/band_max trong tool tutor.
- Ingestion: xóa GoallessResultUnsupported; event không goal đi đường bình thường theo user.
- migrations/V10__one_mastery_path_per_user.sql đúng mục Architecture của phase (giữ path nhiều evidence nhất, RAISE NOTICE,
  unique index theo user, pending.learning_goal_id nullable + index không unique, DROP bảng band, thêm has_practice_set).
- Xóa: path_orderer.py, ordering_llm.py, path_ordering.py (import nào còn trỏ tới nó thì đổi sang ordering_validator), curriculum_scope.py,
  clients/user_service.py, handler NoCurriculumInScope/ActiveGoalRequired trong main.py, setting user_service_base_url +
  validator + biến env trong docker-compose.yml (grep xác nhận không còn nơi đọc). Xóa test tương ứng: test_path_orderer.py,
  test_ordering_llm.py, test_path_ordering_postgres.py, test_curriculum_scope.py, test_path_ordering.py (phần còn lại),
  test_mastery_path_goal_uniqueness.py. Chỗ gọi phải sửa: mục "Toàn bộ chỗ gọi phải sửa" của phase (số dòng có thể lệch).
- Cập nhật bảng migration trong README service, .sdd/database/DATABASE_V5.md mục AI Learning, CLAUDE.md của service
  (bảng package: bỏ "LLM sắp thứ tự", path_orderer).

Test: KHÔNG cần TDD đầy đủ, không cần test mới cho từng nhánh. Bắt buộc:
- sửa test cũ cho chạy được với API mới (goal → user, bỏ band); test goal-scoped viết lại thành theo user, đổi tên file;
- thêm tối thiểu 4 test: (1) user không goal → path theo sortOrder, details có has_practice_set; (2) hai ensure_active_path
  song song trên PostgreSQL → một path; (3) migration: 2 path cùng user, giữ path có evidence, index unique tồn tại;
  (4) pending đỗ theo user rồi được áp khi tạo path.

V10 xóa dữ liệu (cascade phiên tutor, sổ luyện tập của path bị bỏ): chỉ chạy trên schema test tạm. KHÔNG chạy trên DB dev
dùng chung (ai-learning-db compose). Xong thì báo Status: NEEDS_DECISION kèm query đếm path trùng và lệnh pg_dump để người dùng
tự chạy và duyệt V10.
Regression Gate: pytest ai-learning đủ biến DB và AMQP (0 skip); test_mastery_* và test_no_deeptutor_dependency.py pass không sửa.
```

## PR 6 — AI Learning: bài học, nộp bài, cổng (phase 6a)

```text
Làm PHẦN 6a của phase 6 plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-06-ai-learning-api-bai-hoc-va-bai-on.md.
Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.

6a gồm: V11; app/learning/practice_evidence.py (không gọi scheduler lịch ôn); app/lessons/{grading,gates,review_rule,store,
service}.py; content client (get_topic_lessons, get_lesson, search_practice_sets, get_package_version, get_topic_test_packages);
router GET /topics, GET /topics/{id}/lessons, GET /lessons/{id}, POST .../exercises/{blockId}/submissions,
POST /lessons/{id}/complete; LearningGateError và handler; setting review_mastery_threshold.
KHÔNG làm: GET/POST /reviews, POST /topics/{id}/test-assignments (PR 7).

Bắt buộc: mọi ghi trong một path transaction trên connection đang mở; bằng chứng chỉ ở lần nộp đầu của khối; review_rule đủ
4 điều kiện (đọc has_practice_set bằng một query). Test review_rule dùng kịch bản trong seed-content.md và in số mastery;
số lệch bảng "Kịch bản kiểm thử" → dừng, báo, không sửa số trong test cho khớp.
Router lấy bearer bằng HTTPAuthorizationCredentials = Depends(bearer_scheme) như app/api/tutor.py.
Regression Gate: pytest ai-learning đủ biến DB.
```

## PR 7 — AI Learning: bài ôn, giao mã đề (phase 6b)

```text
Làm PHẦN 6b của phase 6 plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-06-ai-learning-api-bai-hoc-va-bai-on.md
(6a đã merge). Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.

6b gồm: GET /reviews/{reviewId}, POST /reviews/{reviewId}/submissions (SKIPPED sau set thứ 3 trượt; không còn gói nào → SKIPPED),
POST /topics/{id}/test-assignments (idempotent, mã một lần dùng, TEST_UNAVAILABLE). Test test_review_sets.py,
test_topic_test_assignment.py theo phase.
Regression Gate: pytest ai-learning đủ biến DB.
```

## PR 8 — Assessment tự chấm, lấy đề từ content, event

```text
Làm phase 4 của plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-04-assessment-tu-cham-va-ma-de.md.
Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.
Điều kiện: consumer ai-learning nhận event không goal (PR 4) đã merge.

Phạm vi: services/assessment-service; ở content chỉ xóa InternalAssessmentContentController,
GetQuestionKnowledgePointMappingsUseCase + test, KnowledgePointMappingResponse (giữ QuestionKnowledgePointResponse/Result).
Gọi content ngoài transaction: bỏ @Transactional khỏi execute, phần ghi sang bean AttemptCreator. section_snapshot theo dạng
{title, skill, instructions, passage}, snapshot trả ra vẫn là chuỗi. AnswerSpecGraderTest đọc docs/contracts/answer-spec-v1-vectors.json.
Không migration mới. DTO của người chấm không đổi.
Regression Gate: mvn assessment-service và content-service.
```

## PR 9 — Consumer kết quả đề

```text
Làm phase 7 của plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-07-ai-learning-consumer-ket-qua-de.md.
Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.

LessonResultApplier chạy trong path transaction, trước record_applied_result; một instance applier dùng chung cho PathService
và FormalAssessmentIngestionService (truyền applier=). Consumer không gọi HTTP. TOPIC_GATE chỉ ghi passed_at (một chiều).
Test lỗi giữa chừng rồi giao lại event: bằng chứng không nhân đôi. Không log nội dung câu trả lời.
Regression Gate: pytest ai-learning đủ biến DB và AMQP. Phần "E2E thủ công" của phase để người dùng chạy; chỉ ghi lại các bước.
```

## PR 10 — Tài liệu và kiểm toàn repo

```text
Làm phase 8 của plan 1640: plans/260929-1640-lesson-learning-pipeline-mvp/phase-08-tai-lieu-va-don-dep.md.
Đọc mục "Quy tắc chung" trong plans/260929-1640-lesson-learning-pipeline-mvp/codex-handoff.md và làm theo.

Grep kiểm từng dữ kiện trước khi ghi (route, bảng, setting, migration content V8–V9, ai-learning V10–V11). DATABASE_V5.md và
.sdd/database/mvp-database.md đã có thiết kế đích: chỉ sửa chỗ lệch code và đánh dấu mục đã có migration. Không sửa
.sdd/global. Chạy toàn bộ lệnh kiểm ở bước 4 của phase và báo kết quả thật.
E2E thủ công (bước 5) cần stack chạy: ghi checklist cho người dùng, không tự tuyên bố đã chạy.
```
