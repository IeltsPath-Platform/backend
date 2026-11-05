# Báo cáo phase 7: frontend (Claude làm song song với Codex)

Status: DONE_WITH_CONCERNS (chưa nghiệm thu độc lập; chưa chạy với backend thật)

- Repo: `frontend`, worktree `frontend-error-trap`, nhánh `feat/error-trap-feedback` từ `origin/main` (`77c4949`). Chưa commit.
- Hợp đồng dùng: "Hợp đồng JSON" ở `phase-06-learning-feedback-and-report.md`.

## Đã làm
- Kiểu: `ErrorHint`, `QuestionResult.error?`, `ReviewDetail.errorTrap?`, `BasketOptionalItem.errorTrap?` + `reason: 'ERROR_PATTERN'`,
  `TreePoint.errorTraps?` (`ErrorTrapPoint`). Tất cả tùy chọn.
- Adapter: `api/errorHint.ts` (`mapErrorHint`: bỏ nếu thiếu `code` hoặc `name`, `fixHint` thiếu → rỗng); nối vào
  `mapQuestionResult` (một chỗ cho bài tập lesson, Practice, kiểm tra lý thuyết, set bài ôn), `mapReviewDetail`, `mapOptional`
  của giỏ ôn. Không có trường → **bỏ hẳn thuộc tính** (không gán `null`) để test cũ dùng `toEqual` còn đúng.
- UI: `ErrorHintNote` (`role="note"`, tên "Lỗi cần sửa", biến thể `answer`/`review`); gắn ở `QuestionField` (chỉ khi sai),
  `PracticeAttemptReview`, `ReviewPage` (chỉ khi bài ôn còn `PENDING`), `ReviewBasketPage` (dòng "Lỗi hay gặp"),
  `KnowledgeTreePage` (danh sách `aria-label="Lỗi hay gặp"` lồng dưới KP: tên, mức, % và số lượt làm). CSS dùng token `--lp-accent-*`.
- Mock: `tfng()` gắn bẫy theo đáp án đúng; `gradeAnswers` trả `error` khi sai (kể cả bộ chưa đạt); cây mock có một điểm với 2 lỗi;
  giỏ ôn mock đưa điểm đó vào nhóm "Cần ưu tiên" với `ERROR_PATTERN`.

## File
Sửa: `types/learningPath.ts`, `api/{learningAdapters,masteryGuideAdapters}.ts`, `api/mock/{grading,mockGuide,mockTree}.ts`,
`mocks/learning-path/contentTypes.ts`, `components/blocks/QuestionField.tsx`, `components/PracticeAttemptReview.tsx`,
`pages/{ReviewPage,ReviewBasketPage,KnowledgeTreePage}.tsx`, `learning-path.css`.
Mới: `api/errorHint.ts`, `components/blocks/ErrorHintNote.tsx`.
Test mới: `api/errorTrapAdapters.test.ts`, `components/blocks/ErrorHintNote.test.tsx`, `pages/ReviewPage.errorTrap.test.tsx`,
`api/mock/grading.errorTrap.test.ts`; thêm ca vào `PracticeAttemptReview.test.tsx`, `guide/guide.test.tsx`, `KnowledgeTreePage.test.tsx`.

## Lệnh đã chạy (kết quả thật)
- Baseline (trước khi sửa): `npm test` → 1 lỗi / 1066 (`flashcards/lib/owl-reactions.test.ts`, ca "returns confused pose for mixed-up errors");
  `npm run lint` exit 0; `npm run typecheck` exit 0.
- Test đỏ trước khi code: 9 ca đỏ đúng lý do (thiếu tính năng). Hai ca đỏ do lỗi của chính test (tên trùng với mục có sẵn trong
  fixture giỏ ôn; thiếu `ToastProvider`/`ResizeObserver`) đã sửa trước khi code.
- Sau khi code: `npm test` → **135 file, 1092 test, 0 lỗi**; `npm run lint` exit 0; `npm run typecheck` exit 0; `npm run build` exit 0.
  1092 − 1066 = 26 test mới.

## Lưu ý và câu hỏi còn mở
1. **Chưa chạy với backend thật** (phase 5–6 backend chưa làm). Chỉ kiểm bằng test đơn vị, test component và mock. Cần chạy lại
   khi phase 6 backend xong để đối chiếu hợp đồng JSON thực tế.
2. **Test đỏ có sẵn `owl-reactions.test.ts` không còn đỏ ở lần chạy cuối** dù không sửa gì liên quan. Nhiều khả năng test phụ
   thuộc ngẫu nhiên (chưa điều tra, ngoài phạm vi).
3. **Frontend chưa có màn báo cáo học viên** nên "báo cáo" của phase 7 chỉ áp dụng cho cây kiến thức.
4. **Đã xử lý (2026-10-24 21:30):** `ReviewPage` từng luôn ghi "Bài ôn bắt buộc", sai với bài ôn tùy chọn như `ERROR_PATTERN`.
   Frontend nay đọc `ReviewDetail.blocking` (tùy chọn; `false` → "Bài ôn tự chọn", thiếu → "bắt buộc" như cũ). Backend phải trả
   `blocking` ở chi tiết bài ôn: đã thêm vào phase 6 (yêu cầu 4). Test: `ReviewPage.errorTrap.test.tsx`. Sau sửa: 28 file / 176
   test learning-path xanh, typecheck và lint exit 0. `owl-reactions.test.ts` (ca "tired pose") đỏ ngẫu nhiên, không liên quan,
   có cả ở baseline.
5. Không có `graphify-out` trong worktree nên dùng grep/đọc file trực tiếp (AGENTS.md của frontend yêu cầu graphify khi graph tồn tại).
6. `AGENTS.md` frontend ghi "chưa có `npm test`" nhưng `package.json` có `vitest run`; đã dùng.
7. Mock `tfng()` lặp lại luật bẫy T/F/NG của backend, chỉ để demo offline.
