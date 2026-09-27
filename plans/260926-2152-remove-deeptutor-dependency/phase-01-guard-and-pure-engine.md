---
phase: 1
title: "Test chặn và engine thuần (models, policy, scheduler, grading)"
status: completed
priority: P1
dependencies: []
effort: "~1.5d"
---

# Phase 1: Test chặn và engine thuần

## Overview
Hai việc, chưa nối vào app:
1. Test chặn phụ thuộc DeepTutor, bắt đầu ở trạng thái hiện tại và chỉ được thu nhỏ.
2. Port các module **thuần** (không I/O) của lõi mastery vào `app/mastery/`, kèm test chuyển thể từ test của DeepTutor.

App vẫn dùng DeepTutor sau pha này; pha 2 mới chuyển.

## Đọc trước (chỉ đọc, không import)
Trong `third_party/deeptutor/deeptutor/`:
- `learning/models.py`, `learning/pending.py`, `utils/text_display.py` (`decode_escaped_unicode_for_display`).
- `learning/policy.py`, `learning/scheduler.py`, `learning/mastery.py`, `learning/grading.py`.
- Test nguồn: `learning/tests/test_models.py` (31), `test_policy.py` (23), `test_scheduler.py` (27), `test_grading.py` (24).

Các module này chỉ dùng thư viện chuẩn và `pydantic` (đã có trong `requirements.txt`).

## Requirements
- **Test chặn** `tests/test_no_deeptutor_dependency.py`:
  - Quét AST mọi file `.py` trong `app/`, `main.py`, `tests/`: tập file có `import deeptutor…`/`from deeptutor…` phải **bằng đúng** hằng `DEEPTUTOR_IMPORT_ALLOWLIST` trong test. Hiện là 14 file `app/` và 11 file `tests/` (liệt kê bằng lệnh ở cuối file này). File mới import DeepTutor → fail; file đã sạch mà vẫn trong allowlist → fail (buộc thu nhỏ).
  - `app/mastery/` không bao giờ được có trong allowlist.
  - Hằng `BUILD_REFERENCES_THIRD_PARTY = True` (pha 3 đổi thành `False`): khi `False`, `Dockerfile`, `requirements*.txt`, `README.md` của service và service `ai-learning-*` trong `docker-compose.yml` không được chứa `third_party` hay `deeptutor`.
- **Port** vào `app/mastery/`:
  - `__init__.py` (rỗng), `models.py`, `pending.py` (kèm hàm `decode_escaped_unicode_for_display`), `mastery.py`, `grading.py`, `scheduler.py`, `policy.py`.
  - Chép logic và hằng số nguyên văn; đổi import `deeptutor.learning.x` → `app.mastery.x`. Header nguồn theo quy tắc 3 của `plan.md`.
  - Bỏ phần không dùng: code chỉ phục vụ topic/RAG, book, i18n. Nếu một hàm bị bỏ, không để lại stub.
- **Test chuyển thể** `tests/mastery/test_models.py`, `test_policy.py`, `test_scheduler.py`, `test_grading.py`:
  - Giữ tên test, ca kiểm tra, giá trị kỳ vọng; chỉ đổi import và fixture.
  - Docstring đầu module ghi file nguồn và **liệt kê ca bị bỏ kèm lý do** (ví dụ: dùng SQLite `LearningStore`, topic). Không dùng `skip`.
  - Thêm `tests/mastery/__init__.py` nếu cần cho cách import của suite.
- **Giấy phép**: `licenses/DeepTutor-LICENSE.txt` (bản sao nguyên văn `third_party/deeptutor/LICENSE`) và `NOTICE` ở gốc service:
  `This product includes software derived from DeepTutor v1.6.9 (Apache License 2.0), modified for IELTSPath. See licenses/DeepTutor-LICENSE.txt.`

## Implementation Steps
1. Viết test chặn với allowlist hiện tại; chạy pass.
2. Chuyển thể test của DeepTutor sang `tests/mastery/` (fail vì chưa có module).
3. Port từng module theo thứ tự phụ thuộc: `models` → `pending` → `mastery`, `grading` → `scheduler` → `policy`.
4. Gate (vẫn có `PYTHONPATH` tới `third_party` cho code chưa chuyển).

## Success Criteria
- [x] Test chuyển thể pass trên `app.mastery` với kỳ vọng nguyên văn.
- [x] `app/mastery/` không import `deeptutor`; test chặn pass.
- [x] Suite hiện có không đổi.

## Lệnh liệt kê allowlist ban đầu
```bash
grep -rlE "^\s*(from|import) deeptutor" app main.py tests --include=*.py | sort
```
