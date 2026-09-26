---
phase: 2
title: "Port service và hợp đồng store, chuyển toàn bộ app sang app.mastery"
status: completed
priority: P1
dependencies: [1]
effort: "~2d"
---

# Phase 2: Port service và store, chuyển app sang `app.mastery`

## Overview
Port phần `LearningService` và `LearningTransaction` mà app dùng, rồi đổi mọi import `deeptutor.learning.*` trong `app/`
và `tests/` sang `app.mastery.*`. **Refactor không đổi hành vi**: suite hiện có pass mà không sửa kỳ vọng. Sau pha này
chỉ còn lớp LLM (và test của nó) import DeepTutor.

## Đọc trước (chỉ đọc)
- `third_party/deeptutor/deeptutor/learning/service.py`: `__init__`, `get_or_create`, `replace_modules_for_path`,
  `set_learner_mastery_override`, `record_learner_profile`, `record_quiz_attempt` (dòng 157), `calculate_mastery`,
  `update_mastery`, `record_qualitative_in_memory` (dòng 1120), `_record_quiz_evidence` và các helper private chúng gọi.
- `third_party/deeptutor/deeptutor/learning/storage.py` dòng 1–130 (lỗi), 104–318 (`LearningTransaction`).
- Test nguồn: `learning/tests/test_service_replace_merge.py` (31).
- Code của mình: `app/persistence/postgres_learning_store.py` (`_Connection`, `transaction`, `mutate`),
  `app/learning/external_assessment.py` (subclass `LearningService`), `app/application/path_service.py`.

## Requirements
- `app/mastery/store.py`:
  - `LearningStoreError`, `LearningConflictError` (cùng tên, cùng thông điệp).
  - `LearningTransaction`: `__init__`, `progress`, `base_revision`, `changed`, `created`, `touch`, `emit`, `events`.
    **Không** port `put_topic` (T4) và chưa port nhóm interaction (`get_interaction`, `active_interaction`,
    `put_interaction`, `abandon_active_interactions`), để pha 5 thêm cùng test của tutor.
  - Một `Protocol` `LearningStore` mô tả đúng các method mà `LearningService` port gọi (`transaction`, `mutate`, `load`…),
    để service không phụ thuộc lớp cụ thể.
- `app/mastery/service.py`: `LearningService(store)` với **store bắt buộc** (bản gốc tự tạo SQLite `LearningStore()` khi
  thiếu; bỏ hành vi đó). Chỉ các method ở mục "Đọc trước", cộng helper private chúng cần. Tên event (`path.*`) và
  payload giữ nguyên.
- `ExternalAssessmentLearningService` kế thừa `app.mastery.service.LearningService`.
- `PostgresLearningStore` dựng `app.mastery.store.LearningTransaction`.
- Đổi import ở 14 file `app/` và 9 file test (trừ `tests/deeptutor_llm_support.py`, `tests/test_deeptutor_llm.py`, thuộc
  pha 3). Symbol test đang dùng: `KnowledgePoint`, `KnowledgeType`, `LearningModule`, `LearningProgress`,
  `LearnerMasteryOverride`, `ErrorType`, `compute_mastery`, `gate_threshold`, `is_assessed_mastered`, `next_objective`,
  `map_summary`, `SpacedRepetitionScheduler`, `LearningStoreError`, `LearningTransaction`.
- Thu nhỏ `DEEPTUTOR_IMPORT_ALLOWLIST` còn đúng: `app/learning/deeptutor_llm.py`, `tests/deeptutor_llm_support.py`,
  `tests/test_deeptutor_llm.py`.
- **Tương thích dữ liệu đã lưu**: `mastery_paths.state_json` chứa `LearningProgress` do model của DeepTutor serialize.
  Model port phải đọc được và ghi lại **giống hệt** (cùng field, default, alias, thứ tự không quan trọng).

## Implementation Steps
### Tests Before
1. **Trước khi đổi import**, chạy suite hiện tại để sinh một path đầy đủ (có KP nhiều loại, evidence chính thức,
   override placement, profile, sự kiện), lấy `state_json` từ DB, lưu thành `tests/fixtures/mastery_state_v1.json`. Đây là
   dữ liệu của chính app, không phải script chạy DeepTutor, và không commit script sinh ra nó.
2. Test round-trip: `LearningProgress.model_validate(fixture).model_dump(mode="json") == fixture`.
3. Chuyển thể `test_service_replace_merge.py` sang `tests/mastery/test_service_replace_merge.py` (quy tắc như pha 1).
### Refactor
4. Port `store.py`, `service.py`; đổi import; thu nhỏ allowlist.
### Tests After
5. Gate đầy đủ, **không sửa kỳ vọng** nào của test hiện có. Nếu một test chỉ pass khi sửa kỳ vọng thì đó là lệch
   hành vi: dừng lại và báo.

## Success Criteria
- [x] Chỉ 3 file trong allowlist; suite hiện có pass nguyên văn.
- [x] `state_json` cũ đọc được và ghi lại giống hệt (5/5 path của DB dev).
