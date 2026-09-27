---
phase: 1
title: "Hạn mức lượt tutor theo ngày"
status: completed
priority: P1
dependencies: []
effort: "~1.5d"
---

# Phase 1: Hạn mức lượt tutor theo ngày

<!-- Updated: Validation Session 1 - mặc định 50/10, hoàn lượt khi lỗi do hệ thống, test hạn mức ở file riêng -->

## Overview
Mỗi học viên có tối đa `N` lượt tutor và `M` lần tóm tắt memory mỗi ngày (theo giờ Việt Nam, cấu hình được). Vượt hạn mức
thì lượt tutor trả `429` **trước khi mở SSE và trước khi gọi LLM**; tóm tắt memory thì bị bỏ qua lặng lẽ. Frontend xem
được lượng đã dùng qua `GET /api/ai-learning/tutor/usage`.

## Hiện trạng (đã kiểm 2026-09-27)
- `app/api/tutor.py::run_turn`: `engine.open_turn` (404 session lạ, 409 lượt đang chạy), rồi `StreamingResponse`. Không có
  giới hạn nào.
- Một lượt gọi LLM tối đa `max_rounds=6` lần (`TutorEngine`). `LearnerMemoryService.update` gọi `complete` thêm một lần mỗi
  đợt tóm tắt (≥ 8 message mới).
- Gọi LLM lúc tạo path (`PathOrderer`) chỉ một lần mỗi goal, nên không tính vào hạn mức.
- `Settings` (`app/config.py`) dùng prefix `AI_LEARNING_`.

## Quyết định

| # | Quyết định |
| --- | --- |
| Q1 | Đếm **lượt tutor đã mở** (không đếm số lần gọi LLM trong lượt). **Hoàn lượt** (trừ lại 1) khi lượt kết thúc bằng `turn.failed` với `failureCode` là `llm_not_configured` hoặc `llm_error`: lỗi do hệ thống hoặc nhà cung cấp, không phải do học viên. Các lỗi khác (`too_many_rounds`, `internal_error`, `cancelled`) vẫn tính. |
| Q2 | Kiểm **sau** `open_turn` (để 404/409 không tốn hạn mức): tiêu một đơn vị; nếu hết thì đóng lượt vừa mở bằng `finish_turn(turn, "failed", "quota_exceeded")` và trả `429`. |
| Q3 | Đếm nguyên tử trong PostgreSQL (`INSERT ... ON CONFLICT DO UPDATE ... WHERE used < limit RETURNING used`); không có dòng trả về nghĩa là đã hết. Đúng khi request đồng thời. |
| Q4 | Ngày tính theo `now() AT TIME ZONE <timezone>` trong PostgreSQL (dùng tz database của Postgres, không phụ thuộc `tzdata` của Python trên Windows). Mặc định `Asia/Ho_Chi_Minh`. |
| Q5 | Mức `0` nghĩa là **không giới hạn** (vẫn đếm để hiện usage). Số âm → cấu hình không hợp lệ (fail khi khởi động). |
| Q6 | Tóm tắt memory vượt hạn mức → `update` trả `"skipped"`, không tiến mốc (lượt sau hoặc ngày sau làm tiếp). |
| Q7 | Không động tới LLM sắp thứ tự lộ trình, hay tính năng khác không gọi LLM (practice REST, notes, flashcard). |

## Cấu hình (thêm vào `Settings`)

| Biến | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `AI_LEARNING_TUTOR_TURNS_PER_DAY` | `50` | Lượt tutor tối đa mỗi học viên mỗi ngày; `0` = không giới hạn |
| `AI_LEARNING_MEMORY_SUMMARIES_PER_DAY` | `10` | Lần tóm tắt memory tối đa mỗi ngày; `0` = không giới hạn |
| `AI_LEARNING_QUOTA_TIMEZONE` | `Asia/Ho_Chi_Minh` | Múi giờ tính ranh giới ngày (tên IANA, Postgres hiểu được) |

## Migration `V9__llm_daily_usage.sql`

```sql
-- How many LLM-backed actions each learner used per local day, so a learner cannot run up provider cost without limit.
CREATE TABLE llm_daily_usage (
    user_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('tutor_turn', 'memory_summary')),
    used INTEGER NOT NULL CHECK (used >= 0),
    PRIMARY KEY (user_id, usage_date, kind)
);
```

## Kiến trúc

```python
# app/usage/quota.py  (package mới app/usage)
@dataclass(frozen=True)
class QuotaResult:
    allowed: bool
    used: int
    limit: int            # 0 = unlimited
    usage_date: date      # the local day the unit was counted on (refund targets this day)
    resets_at: datetime   # next local midnight, timezone-aware (UTC)

class DailyQuotaStore:
    def __init__(self, database_url: str, timezone: str): ...
    def consume(self, user_id, kind: str, limit: int) -> QuotaResult:
        # one statement; limit 0 skips the WHERE guard
        # INSERT INTO llm_daily_usage (user_id, usage_date, kind, used)
        # VALUES (%s, (now() AT TIME ZONE %s)::date, %s, 1)
        # ON CONFLICT (user_id, usage_date, kind) DO UPDATE SET used = llm_daily_usage.used + 1
        #   WHERE %s = 0 OR llm_daily_usage.used < %s
        # RETURNING used
        # no row → allowed False, used = current value (read back)
    def refund(self, user_id, kind: str, usage_date: date) -> None:
        # UPDATE llm_daily_usage SET used = used - 1
        # WHERE user_id = %s AND kind = %s AND usage_date = %s AND used > 0
        # (targets the day consumed, so a turn that crosses midnight refunds the right day)
    def usage(self, user_id, kind: str, limit: int) -> QuotaResult: ...   # read-only, used 0 when no row
```
- `resets_at` tính trong SQL: `(date_trunc('day', now() AT TIME ZONE tz) + interval '1 day') AT TIME ZONE tz`.
- `kind` hợp lệ: `tutor_turn`, `memory_summary` (hằng số trong module).

**API (`app/api/tutor.py`):**
- `run_turn`: sau `open_turn` gọi `quota.consume(user, "tutor_turn", settings.tutor_turns_per_day)` (qua
  `asyncio.to_thread`). Hết hạn mức thì:
  - `finish_turn(turn_id, "failed", "quota_exceeded")`;
  - trả `JSONResponse(429, {"detail": "Daily tutor turn limit reached", "limit": N, "resetsAt": iso})`, header
    `Retry-After` = số giây tới `resets_at`.
- Hoàn lượt (Q1): `run_turn` bọc generator của `engine.run` bằng một async generator nhỏ; gặp event `turn.failed` có
  `failureCode` thuộc `{"llm_not_configured", "llm_error"}` thì gọi `quota.refund(user, "tutor_turn", result.usage_date)`
  (qua `asyncio.to_thread`, lỗi refund chỉ log `error_type`) rồi mới chuyển event đi. `TutorEngine` **không đổi**.
- `GET /api/ai-learning/tutor/usage` → `200 {"timezone": "...", "resetsAt": "...", "tutorTurns": {"used", "limit"},
  "memorySummaries": {"used", "limit"}}` (limit `0` = không giới hạn).
- Dependency `get_quota_store()` trong `app/api/dependencies.py`.

**Memory (`app/tutor/memory.py`):** `LearnerMemoryService(store, quota=None, summaries_per_day=0)`. Trong `update`, **sau**
khi đã đủ điều kiện (≥ 8 message) và **trước** `complete`, gọi `consume(user, "memory_summary", limit)`; hết thì trả
`"skipped"` và log `Learner memory summary skipped reason=quota`. `quota=None` giữ hành vi cũ (các test hiện có không đổi).
`get_tutor_engine` truyền quota store và mức cấu hình.

## Related Code Files
- Create: `services/ai-learning-service/app/usage/__init__.py`, `app/usage/quota.py`, `migrations/V9__llm_daily_usage.sql`
- Modify: `app/config.py`, `app/api/dependencies.py`, `app/api/tutor.py`, `app/api/dto/tutor.py` (DTO usage), `app/tutor/memory.py`
- Tests: create `tests/test_daily_quota_postgres.py`, `tests/test_tutor_quota_api_postgres.py` (class riêng với mức 2
  lượt/ngày, để `tests/test_tutor_api_postgres.py` giữ mức mặc định và không đổi); modify
  `tests/test_learner_memory_postgres.py`, `tests/test_migrations_postgres.py` (bảng mới)
- Docs: `docs/contracts/tutor-sse-v1.md` (`429`, `/usage`), `services/ai-learning-service/README.md` (biến cấu hình, bảng
  migration V9), `.sdd/database/DATABASE_V5.md` (mục `llm_daily_usage`)

## Implementation Steps (TDD)
1. **Khóa hành vi hiện tại** (chạy phải pass trước khi đổi code):
   - lượt tutor thường vẫn `200 text/event-stream`, session 404 và lượt đang chạy 409 vẫn như cũ;
   - memory tóm tắt đúng khi ≥ 8 message. Test hiện có đã phủ phần lớn; bổ sung test 404/409 nếu thiếu.
2. **Viết test cho hành vi mới** (chạy phải fail), xem mục Tests.
3. Migration V9 và `Settings` (validator: số nguyên ≥ 0; timezone không rỗng).
4. `DailyQuotaStore`.
5. `run_turn`, endpoint `/usage`, dependency.
6. Memory quota.
7. Docs.
8. Gate: `python -m pytest tests -rs` (0 fail, 0 skip), `python -m compileall -q app`, `git diff --check`, lệnh `git grep`
   DeepTutor, `graphify update .`. **Một commit**: `feat(tutor): cap daily tutor turns and memory summaries per learner`.

## Tests
`tests/test_daily_quota_postgres.py`:
1. `consume` cho phép đúng `limit` lần rồi từ chối; lần bị từ chối không tăng `used`.
2. Hai `kind` và hai học viên đếm độc lập.
3. `limit = 0` không bao giờ từ chối, vẫn đếm.
4. **Đồng thời:** 20 thread cùng `consume` với `limit = 10` → đúng 10 lần `allowed`.
5. Ngày mới: sửa `usage_date` của dòng hiện có về hôm qua → `consume` cho phép lại từ 1.
6. `resets_at` là nửa đêm kế tiếp theo múi giờ cấu hình (so với giá trị Postgres tính cho cùng `now()`).

`tests/test_tutor_quota_api_postgres.py` (file riêng; `patch.dict` env `AI_LEARNING_TUTOR_TURNS_PER_DAY=2` ở `setUpClass`,
xóa cache settings như `test_tutor_api_postgres.py`; mỗi test dùng học viên mới):

7. Hai lượt đầu `200`; lượt thứ ba `429`, có `limit`, `resetsAt`, header `Retry-After`; **`ScriptedChat` không nhận thêm
   request nào**; turn thứ ba có `status = failed`, `failure_code = quota_exceeded`; session vẫn mở được lượt mới sau khi
   đổi `usage_date` về hôm qua.
8. Session lạ vẫn `404` và lượt đang chạy vẫn `409`, không tốn hạn mức (`used` không đổi).
9. `GET /tutor/usage` đúng `used` và `limit`; không token → `401`; học viên B không thấy số của A.
9b. Hoàn lượt: `ScriptedChat` ném `LlmApiError` → `turn.failed` `llm_error`, `used` **không đổi**; lượt không có chat
    (`llm_not_configured`) cũng không tốn; `ScriptedChat` dùng hết 6 vòng (`too_many_rounds`) → **có** tính.

`tests/test_learner_memory_postgres.py`:

10. `summaries_per_day = 1`: đợt đầu `"updated"`; đợt sau (đủ 8 message mới) `"skipped"`, `complete` không được gọi, mốc giữ nguyên.

## Success Criteria
- [x] Vượt hạn mức lượt tutor bị chặn trước khi mở SSE và trước khi gọi LLM, trả `429` kèm thời điểm reset.
- [x] Đếm đúng khi đồng thời; 404/409 không tốn hạn mức; lượt lỗi do hệ thống (`llm_error`, `llm_not_configured`) được hoàn.
- [x] Tóm tắt memory bị giới hạn riêng; vượt thì bỏ qua, không lỗi.
- [x] Frontend đọc được usage qua `/tutor/usage`.
- [x] Toàn bộ test Python pass (0 fail, 0 skip); docs cập nhật.

## Kết quả triển khai (2026-09-27)
- Test: 432 passed, 0 fail, 0 skip (kể cả RabbitMQ), Python 3.11, PostgreSQL 17 local.
- Thay đổi so với plan, đã được người dùng chốt sau code review:
  - **Q1 thu hẹp**: chỉ hoàn lượt khi `llm_not_configured` hoặc `llm_error` **trước** event `tool.called`/`assistant.message`
    đầu tiên (lỗi ở lần gọi model đầu). Model đã trả lời ít nhất một vòng thì lượt vẫn tính, vì các vòng đó đã bị tính phí.
    Mỗi lần hoàn ghi log `Tutor turn refunded session turn code`.
  - **Compose**: `ai-learning-api` nhận 3 biến hạn mức từ `.env` root (mặc định 50 / 10 / `Asia/Ho_Chi_Minh`).
- Bổ sung từ mục Risk: khởi động gọi `DailyQuotaStore.check_ready()`; chỉ nhận tên IANA trong `pg_timezone_names`
  (từ chối `UTC+7` vì Postgres đọc ngược dấu) và kiểm bảng `llm_daily_usage` tồn tại.
- Lỗi khi `consume` → đóng lượt `failed/internal_error` rồi trả lỗi, session không bị kẹt. Lượt bị từ chối có log `reason=quota`.
- Review: `plans/reports/code-reviewer-260927-2117-daily-tutor-quota-review-report.md`. Các việc còn mở (Low) được
  ghi ở `plan.md`, mục "Làm sau", từ mục 4 đến 6.

## Risk Assessment
- **Múi giờ sai tên** → Postgres báo lỗi ở lần gọi đầu. Giảm: test khởi động gọi `usage()` một lần; README ghi rõ tên IANA.
- **Hoàn lượt nhầm hoặc hoàn trùng**: chỉ hoàn đúng một lần, khi gặp event `turn.failed` (mỗi lượt phát tối đa một lần);
  `refund` không đưa `used` xuống dưới 0.
- **Hạn mức mặc định chưa có số liệu thật**: 50 lượt và 10 lần tóm tắt mỗi ngày (người dùng chốt); chỉnh bằng env, không
  cần deploy code.
- Rollback: revert commit, `DROP TABLE llm_daily_usage`.
