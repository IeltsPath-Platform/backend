# Plan closeout — 2026-10-21

Plan: [Lesson vocabulary data](../plan.md)

| Phase | Status | Evidence / remaining work |
| --- | --- | --- |
| 1. Library API and seed | In progress | Implementation and tests are present. Removed a duplicate @Override found during closeout review. Tests and Gateway request remain unverified. |
| 4. Lesson vocabulary blocks | In progress | V72 seed and content/Learning regression tests are present. Maven tests and Gateway checks remain unverified. |
| 5. Docs and contract | Complete | Lesson contract and architecture docs updated; JSON examples parse and claims were matched against DTOs, controller, use case and V72. |

## Closure decision

Keep the plan in-progress. Phase 1 and 4 acceptance criteria explicitly require passing tests and Gateway checks. Maven was previously unavailable in the environment and the Gateway check was not performed, so the plan cannot be marked complete without claiming unverified results.

## Updated files

- backend/docs/contracts/lesson-learning-v1.md
- backend/docs/system-architecture.md
- backend/services/library-service/src/main/java/com/ieltspath/library/infrastructure/persistence/adapter/VocabularyRepositoryAdapter.java
- Plan and phase status/progress files.

The workspace did not expose a usable Git repository for git diff --check; docs JSON parsing succeeded. No tests were run during this closeout.

## Follow-up verification — 2026-10-21

Maven và Docker hiện truy cập được. Đã khôi phục Content V59–V70 gốc để khớp lịch sử database, chạy thành công V1–V58 và V72 (V59–V71 thuộc nhánh Writing, đã tách khỏi PR này) trên database tạm và đồng bộ các assertion seed trong `LessonPipelineSeedTest`.

- Lượt đầy đủ Content: 520 tests; 6 assertion cũ thất bại trong một class.
- Sau cập nhật, hai class seed chạy lại: 34 tests, 0 failures, 0 errors, 0 skipped; 486 test còn lại đã pass ở lượt đầy đủ.
- `git diff --check` chạy thành công trong repo backend (dùng safe.directory cho chính repo này).
- Library/Learning tests và Gateway acceptance vẫn chờ xác minh; plan chưa đóng.
