---
phase: 5
title: "AI Learning: path theo user"
status: in-progress
priority: P1
dependencies: [1]
effort: "2–3 ngày"
---

# Phase 5: AI Learning: path theo user

## Overview

- Mỗi user một path, tạo từ content theo `sort_order`: không cần goal, không lọc band, không gọi LLM.
- Tutor, `/paths`, `/status`, `/progress` và phần nhận kết quả thi dùng chung path này.
- Consumer nhận event không có goal (phải có trước phase 4).
- Placement không còn ghi mastery hay test-out.
- Chiếu evidence ghi theo lô.
- Xóa code sắp thứ tự bằng LLM và code lọc band, nhưng **giữ** những phần khác của `curriculum_scope.py` mà code còn dùng.

## Requirements

- **Tạo path:** `ensure_active_path(user_id, bearer)` không cần goal. Chưa có path thì tạo từ **một** lần gọi `ContentServiceClient.get_topic_sequence` (`/internal/learning-content/topic-sequence`, kèm KP; Validation Session 2), đổi sang dạng `topics` + `knowledgePoints` rồi qua `CurriculumAdapter.to_modules` như cũ; vẫn ghi snapshot chi tiết KP (skill, description, **`has_practice_set`** từ `hasPracticeSet`; Validation Session 3) từ cùng response. Path chỉ chứa topic học được (có bài và có đề). `get_curriculum` và hai API `/api/content/topics`, `/api/content/knowledge-points` không dùng trong luồng MVP; tutor ngoài phạm vi MVP, không sửa thêm cho tutor. **Không còn band của KP** (người dùng chốt 2026-09-29; content bỏ band ở phase 3).
- **Đồng thời:** advisory lock theo user; bắt `UniqueViolation` trên index mới.
- **Pending:**
  - áp khi tạo path, **và** khi nạp path đã có;
  - vì vậy kết quả đỗ lại dưới goal cũ không bị kẹt sau V10.
- **Consumer chịu được** (có trước phase 4):
  - `learning_goal_id` null hoặc thiếu: hợp lệ;
  - `package_version_id` được đọc nếu có, không bắt buộc ở phase này;
  - thiếu cả hai không phải `ContractError`.
- **PLACEMENT:** chỉ ghi version đã xử lý; không ghi evidence (chặn trước bước ghi ở `formal_result_applier.py:91-95`), không test-out.
- **Refresh:** gộp topic và KP mới (`merge_curriculum`) từ cùng response `topic-sequence`. Refresh đã ghi lại snapshot của mọi KP có trong response (`path_service.py:189-202`: `merged_details.update(details)`); chỉ cần `KnowledgePointDetails` có thêm `has_practice_set` để phép so `merged_details == current_details` thấy thay đổi khi content thêm gói. Phase 6 gọi trong `GET /topics`, một lần gọi Content cho cả path và thứ tự học.
- **Chiếu evidence** (`postgres_learning_store.py:231-268`): chèn theo lô (`execute_values`), không chèn từng dòng trong vòng lặp (AGENTS §3.7). Việc này cần trước phase 6, vì mỗi lần nộp bài sẽ commit path.
- **Không** đụng `app/mastery/*` (AGENTS §3.8).

## Architecture

**Migration `V10__one_mastery_path_per_user.sql`** (thay đổi dữ liệu có tính phá hủy: **cần người dùng duyệt**):
- Chọn path giữ lại cho mỗi user: `ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY <số evidence> DESC, updated_at DESC, path_id)`. Xóa các path còn lại; mọi FK đều `ON DELETE CASCADE` (V0_1, V2, V4, V5, V6). `RAISE NOTICE` số path và dòng bị xóa.
- **Phạm vi xóa (đã chấp nhận, Validation Session 1):** cascade xóa cả phiên tutor, turn, message, session material (V5, V8) và sổ luyện tập (V6) của path bị bỏ, không chỉ evidence. Không chuyển các dòng này sang path giữ lại: chưa có production, chỉ có dữ liệu dev; đã có `pg_dump` trước khi chạy.
- `DROP INDEX uq_mastery_paths_user_learning_goal`; `CREATE UNIQUE INDEX uq_mastery_paths_user ON mastery_paths(user_id)`. Cột `learning_goal_id` giữ lại, nullable, không dùng để tìm path.
- `pending_formal_assessment_results`:
  - `learning_goal_id` bỏ NOT NULL;
  - thêm index **không unique** (`user_id`, `attempt_id`, `result_version`), vì idx cũ vốn không unique (`V3:14-15`) và `ON CONFLICT (event_id)` giữ nguyên.
- `DROP TABLE mastery_path_knowledge_point_bands` (V4): không còn band của KP để ghi.
- `ALTER TABLE mastery_path_knowledge_point_details ADD COLUMN has_practice_set BOOLEAN NOT NULL DEFAULT FALSE` (Validation Session 3): cờ "content có gói luyện cho KP", luật chèn bài ôn của phase 6 và consumer phase 7 đọc cột này, không gọi HTTP.
- Trước khi chạy trên DB dev dùng chung: chạy query kiểm số path trùng và `pg_dump`.

**Code:**
- **Tách `curriculum_scope.py` trước khi xóa:**
  - chuyển `parse_band` (adapter event đọc `overall_band`), `KnowledgePointDetails` và phần dựng details sang `app/adapters/knowledge_point_details.py`; `KnowledgePointDetails` thêm trường `has_practice_set: bool = False`, đọc từ `hasPracticeSet`;
  - `replace_knowledge_point_details` (`postgres_learning_store.py:417-427`) ghi thêm cột mới và chuyển sang `execute_values` (hiện chèn từng dòng trong vòng lặp, AGENTS §3.7); `knowledge_point_details` đọc thêm cột;
  - **xóa** `KnowledgePointBand` và mọi đường band của KP:
    - `postgres_learning_store.py:392-415` (`replace_knowledge_point_bands`, `knowledge_point_bands`);
    - tham số `bands` trong `path_service.py:14,145,178-201,228-243`;
    - `band_min`/`band_max` trong tool `app/tutor/tools.py:347-357`;
  - sửa import tại `formal_evidence_adapter.py:17`, `postgres_learning_store.py:431`, `path_service.py:13`;
  - xóa `CurriculumScope.select`, `target_band_of`, `NoCurriculumInScope` và handler ở `main.py:15,67-72`.
- **`OrderingValidator`:** chuyển sang `app/learning/ordering_validator.py`, vì tool reorder của tutor còn dùng (`path_reorder.py:8,74`). Chuyển `OrderingValidatorTest` (`tests/test_path_ordering.py:76-141`) sang `tests/test_ordering_validator.py`.
- **Store:** `find_path_by_user`; lock theo user thay `_lock_goal`; `pending_formal_payloads(user_id)` và `park_formal_result` theo user.
- **`PathService`:**
  - bỏ `user_client`, `_active_goal`, `_scoped_curriculum`, `PathOrderer`;
  - tham số constructor **chỉ nhận theo tên**, vì chỗ gọi hiện truyền theo vị trí;
  - `ensure_path(user_id, …)`.
- **Consumer và adapter:** `formal_evidence_adapter.py:76-117` cho `learning_goal_id` optional (`:108`); đọc `package_version_id` optional.
- **Placement:** `formal_result_applier.py`: bỏ gọi `placement_test_out` và bỏ ghi evidence cho PLACEMENT.
- **Setting:** bỏ `user_service_base_url` khỏi `app/config.py:24` và khỏi hai validator (`:32`, `:46`) nếu grep xác nhận không còn nơi đọc (tutor, learner memory). Nơi đọc duy nhất hiện tại là `app/api/dependencies.py:24`. Bỏ biến env tương ứng trong `docker-compose.yml` của ai-learning cùng lúc.
- **Goal:** xóa `ActiveGoalRequired` (`path_service.py:25,65,68`) và handler của nó (`main.py:19,59`).
- **Xóa:** `app/application/path_orderer.py`, `app/learning/ordering_llm.py`, `app/learning/path_ordering.py` (sau khi tách validator), `app/adapters/curriculum_scope.py` (sau khi tách), `app/learning/placement_test_out.py`, `app/clients/user_service.py` (thành code chết khi bỏ goal).
<!-- Updated: Validation Session 1 - phạm vi xóa của V10; bổ sung chỗ gọi goal còn sót -->


**Toàn bộ chỗ gọi phải sửa** (không viết "sửa mọi chỗ gọi"):
- **App:**
  - `main.py:15,67-72,118-160`
  - `app/api/tutor.py:121`
  - `app/api/dependencies.py:20-27`
  - `app/messaging/assessment_consumer.py:116-122`
  - `app/application/formal_assessment_ingestion.py:39-53`
  - `app/application/formal_result_applier.py:19,93-95`
- **Test:**
  - `tests/test_assessment_consumer.py:52-54`
  - `tests/test_assessment_rabbitmq.py:67-69`
  - `tests/test_formal_assessment_ingestion.py:28-31,175-187,265-269`
  - `tests/test_formal_assessment_postgres.py:59-62,75,199,222,229,262`
  - `tests/test_path_refresh.py:64-69,106-148,264`
  - `tests/test_practice_postgres.py:96-97,117-121`
  - `tests/test_tutor_api_postgres.py:84`
  - `tests/test_tutor_engine_postgres.py:106-107` (và các chỗ dùng band của KP)
  - `tests/test_tutor_quota_api_postgres.py:86`
  - `tests/test_tutor_reading_postgres.py:82`
  - `tests/test_goal_scoped_path.py`, `tests/test_goal_scoped_path_postgres.py:52-57` (viết lại theo user, đổi tên)
  - `tests/test_mastery_path_goal_uniqueness.py`
  - `tests/test_formal_evidence_adapter.py:56` (đảo: thiếu goal là hợp lệ)
  - `tests/formal_assessment_support.py:80-236`
  - `tests/test_migrations_postgres.py`
- **Xóa test:** `tests/test_path_orderer.py`, `tests/test_ordering_llm.py`, `tests/test_path_ordering_postgres.py`, `tests/test_curriculum_scope.py`, `tests/test_placement_test_out.py`, phần không phải validator của `tests/test_path_ordering.py`.

## Implementation Steps

**Tests Before** (khóa hành vi phải giữ):
1. `/status`, `/progress`: shape response (key, kiểu).
2. Tutor mở session trên path có sẵn; tool `knowledge_point_details` trả `skill` và `description` khác rỗng. Sau khi sửa, output của tool không còn khóa `band_min`/`band_max`. Các test đang dùng band của KP phải sửa theo: `test_tutor_engine_postgres.py`, `test_practice_postgres.py`, `test_path_refresh.py`, `test_goal_scoped_path*.py`, `formal_assessment_support.py`.
3. `refresh_active_path` gộp topic mới, giữ thứ tự và evidence của topic cũ.
4. `OrderingValidatorTest` và test tool reorder của tutor pass sau khi chuyển file.

**Tests After** (viết trước code):
5. User không có goal: tạo path theo `sortOrder`; user client và LLM giả không được gọi; snapshot details được ghi, gồm `has_practice_set` (KP1–KP4 `true`, KP5 `false` theo seed). Refresh sau khi content đổi `hasPracticeSet` của KP5 thành `true` → cột được cập nhật.
6. Hai lần `ensure_active_path` song song (PostgreSQL) → một path.
7. Migration (schema tạm):
   - 2 path cùng user (một path có evidence, một path rỗng nhưng mới hơn) → giữ path có evidence;
   - hai path trùng `updated_at` → không lỗi;
   - index unique theo user tồn tại.
8. Pending: đỗ theo user khi chưa có path; tạo path hoặc nạp path có sẵn thì áp dụng và xóa pending.
9. Adapter: event `learning_goal_id: null` → command hợp lệ; có `package_version_id` thì đọc được.
10. PLACEMENT: `mastery_levels` không đổi.
11. Chiếu evidence với ≥ 1.000 dòng: một lần commit chạy xong trong giới hạn hợp lý; đúng số dòng.

**Duyệt:** trình người dùng migration V10 trước khi chạy trên DB dev dùng chung.

**Regression Gate** (trong `services/ai-learning-service`, venv Python 3.11):
```powershell
$env:PYTHONDONTWRITEBYTECODE = "1"
python -m pytest tests -p no:cacheprovider
```
Chạy kèm `AI_LEARNING_TEST_DATABASE_URL` và `AI_LEARNING_TEST_AMQP_URL`. `test_mastery_*` và `test_no_deeptutor_dependency.py` pass mà không bị sửa.

## Success Criteria

- [ ] Không đường nào đòi goal hay gọi LLM khi tạo path; app và consumer import được.
- [ ] Tutor vẫn có chi tiết KP; tool reorder vẫn chạy.
- [ ] Consumer nhận event không có goal.
- [ ] Test bước 1–11 pass; người dùng đã duyệt V10.

## Kết quả phần a (2026-10-01, PR 4, nhánh `feat/ai-learning-module-split`)

- Tách `app/adapters/knowledge_point_details.py` (`KnowledgePointDetails` thêm `has_practice_set`, `details_from_content`,
  `parse_band`) và `app/learning/ordering_validator.py`; test validator chuyển sang `tests/test_ordering_validator.py`.
- Adapter: `learning_goal_id` null/thiếu hợp lệ, `package_version_id` đọc nếu có; UUID sai định dạng vẫn là `ContractError`.
- PLACEMENT chỉ ghi version đã xử lý; xóa `placement_test_out.py` và test của nó.
- Chiếu evidence và snapshot details ghi bằng `execute_values` (test 1.200 dòng).
- **Tạm thời tới phần b:** path còn theo goal, nên ingestion ném `GoallessResultUnsupported` cho event không có goal
  (consumer thử lại rồi đưa vào DLQ, phát lại sau phần b). Chưa nơi nào phát event không goal (assessment đổi ở PR 8).
  Store chưa ghi `has_practice_set` (cột thêm ở V10). Phần b xóa `GoallessResultUnsupported`.
- Test: 427 pass, 0 skip (PostgreSQL và RabbitMQ thật).

## Risk Assessment

- **Mất dữ liệu dev** khi gộp path (evidence, phiên tutor, sổ luyện tập của path bị bỏ): đã chọn path có nhiều evidence nhất, có `pg_dump`, người dùng chấp nhận ở Validation Session 1.
- **Tutor dựa vào goal** (prompt, learner memory): grep `goal` trong `app/tutor`, `app/learning`. Nếu prompt dùng band hay ngày thi thì tách việc đọc goal chỉ để hiển thị, không dùng để tạo path.
- **Phase lớn:** tách thành 2 PR nếu cần: (5a) tách module, adapter chịu null, placement; (5b) path theo user, V10.
