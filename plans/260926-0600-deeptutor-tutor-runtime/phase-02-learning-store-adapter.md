---
phase: 2
title: "LearningStore adapter đầy đủ"
status: pending
priority: P1
dependencies: [1]
effort: "~2d"
---

# Phase 2: LearningStore adapter đầy đủ

## Overview
Mở rộng `PostgresLearningStore` để thỏa **toàn bộ** API mà `LearningStore` của DeepTutor dùng trong các capability được
bật: interactions, session binding, path lease, events, evidence projection, danh sách path. Adapter cũng là nơi kiểm
tra invariant T5: tập KP của path chỉ được đổi qua refresh từ Content.

## Đọc trước khi code
- Report của pha 1 (method coverage).
- `third_party/deeptutor/deeptutor/learning/storage.py` (`LearningStore`, `LearningTransaction`, `MasteryPathLease`,
  `_converge_single_membership`, `bind_session`, `acquire_path_lease`).
- `app/persistence/postgres_learning_store.py`, `migrations/V0_1__create_v5_mastery_tables.sql`.
- `.sdd/database/DATABASE_V5.md` §7.2 `mastery_path_sessions`, §7.3 `mastery_interactions`, §7.6 `mastery_path_leases`.

## Requirements
- Functional:
  - Migration `V5__mastery_path_sessions_and_leases.sql`:
    - `mastery_path_sessions`: PK `(path_id, session_id)`, unique `session_id`;
    - `mastery_path_leases`: PK `path_id`, unique `turn_id`.
    - Tạo theo V5. Khóa ngoại tới `sessions` và `turns` thêm ở pha 3.
  - Adapter: các method trong coverage của pha 1. Nếu thiếu thì dựa theo DeepTutor:
    - `bind_session`, `list_session_ids`, `path_id_for_session`, `detach_session`;
    - `get_path_lease`, `acquire_path_lease`, `release_path_lease`, `release_leases_for_turn`;
    - `get_interaction`, `get_active_interaction`, `list_interactions`, `list_events`;
    - `load`, `exists`, `list_all`, `list_learning_evidence`, `count_learning_evidence`.
    - Chỉ trả path của **current user**: một học viên không bao giờ thấy path của người khác, kể cả khi DeepTutor gọi `list_all`.
    - Các method về topic material (`put_topic`, `get_topic`…) gắn với RAG (T4): ném `NotImplementedError("topics are disabled: T4")`.
  - **Invariant T5** trong `transaction`: khi commit, nếu tập `(module_id, kp_id)` khác tập trước đó, thì phải có cờ
    `curriculum_change` do chính AI Learning đặt (tạo path, refresh). Nếu không có cờ, rollback và ném `LearningStoreError`
    với thông báo mà tool trả được cho LLM. Nhờ vậy `mastery_build` và `mastery_revise` đổi KP đều bị chặn, còn chỉ đổi
    thứ tự thì được phép.
  - Bootstrap gán `deeptutor.learning.storage.LearningStore` = adapter; constructor không tham số dùng current user.
- Non-functional: giữ semantics revision và CAS hiện có; lock hàng như `transaction` đang làm.

## Implementation Steps
### Tests Before
1. Test hợp đồng: chạy lại các test store liên quan trong `third_party/deeptutor/tests` (interaction lifecycle, lease,
   session binding) với adapter; nếu không import được test gốc thì viết lại cùng kịch bản.
2. Test invariant: `replace_modules_for_path` đổi KP mà không có cờ → lỗi; chỉ đổi thứ tự → OK; refresh và tạo path → OK.
3. Test phạm vi người dùng: `list_all` và `path_id_for_session` không lộ path của người khác.
### Refactor
4. Migration, adapter, bootstrap.
### Tests After
5. Gate đầy đủ; toàn bộ test hiện có (plan trước) vẫn pass.

## Success Criteria
- [ ] Mọi method trong coverage của pha 1 có bản PostgreSQL và test.
- [ ] Không đường nào (tool, turn, router) đổi được tập KP của path, trừ tạo path và refresh.
