---
title: "Lesson vocabulary data"
description: "Attach vocabulary senses to lessons (VOCABULARY block) and expose card content; learning mechanism and gate deferred to the flashcard module."
status: in-progress
priority: P2
branch: "feat/lesson-vocabulary (backend repo, from origin/main)"
tags: [library-service, content-service, vocabulary, tdd]
blockedBy: []
blocks: []
created: "2026-10-21T13:23:52.379Z"
createdBy: "ck:plan"
source: skill
---

# Lesson vocabulary data

## Overview

Mỗi lesson có một bộ từ vựng riêng, lưu bằng block `VOCABULARY` (content V8, đã có nhưng chưa dùng). Block này trỏ tới
sense của library. Vòng này **chỉ lo data**:
- seed từ,
- gắn từ vào lesson,
- API lấy nội dung thẻ,
- contract để module flashcard nối vào sau.

Cách học (flashcard, Leitner) do module flashcard của dev khác lo (library, `flashcard-srs-v1.md`, thẻ
`sourceType: VOCABULARY_SENSE`).

**Không đổi** điều kiện hoàn thành lesson, gate, `kp_evidence` hay mastery.

Nguồn quyết định:
- [Brainstorm report](../reports/brainstorm-261021-2016-lesson-vocabulary-progress-gate-report.md). Phạm vi đã thu hẹp,
  xem Validation Session 2.
- Tài liệu kiến trúc `01-architecture-overview`, `02-dev-integration-spec`, `03-ba-spec`.

Mode: `--tdd`, viết test trước trong mỗi phase.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Library sense batch API and vocabulary seed](./phase-01-library-sense-batch-api-and-vocabulary-seed.md) | In progress — validation pending |
| 4 | [Attach vocabulary blocks to lessons](./phase-04-attach-vocabulary-blocks-to-lessons.md) | In progress — validation pending |
| 5 | [Docs and flashcard API contract](./phase-05-docs-and-flashcard-api-contract.md) | Complete |

Phase 2–3 (bảng tiến độ, endpoint review, gate) đã gỡ khỏi vòng này. Giữ số phase cũ để link ổn định.
Thứ tự: 1 → 4 → 5. Phase 1 và 4 đã có implementation, nhưng test/Gateway acceptance chưa xác minh; phase 5 đã hoàn tất. Plan vẫn in-progress đến khi phase 1 và 4 đạt tiêu chí validation.

## Acceptance criteria

- `GET /api/content/vocabulary/senses?ids=…` trả nội dung thẻ theo đúng thứ tự id, chỉ sense ACTIVE.
- L1–L4 của topic `DEMO_READING` mỗi bài có một block VOCABULARY ở đầu bài, 8–15 sense.
- `GET /api/learning/lessons/{id}` trả block đó kèm `vocabularySenseIds` (đã chạy sẵn qua code hiện có, chỉ cần test xác nhận).
- Lesson có block VOCABULARY vẫn hoàn thành y như trước: block này không chặn completion, không khóa bài sau.
- Không có dòng `kp_evidence`, XP hay hearts mới nào phát sinh từ từ vựng.
- Contract mô tả cách module flashcard lấy `vocabularySenseIds` của lesson để tạo thẻ `VOCABULARY_SENSE`.

## Deferred (vòng sau, khi nối module flashcard)

- **Định nghĩa "học xong từ vựng của lesson"** và gate chặn bài sau. Thiết kế đã cân nhắc (bảng tiến độ ở learning,
  completion tính cả block VOCABULARY, pass khi mỗi từ "Đã biết" ≥ 1 lần) nằm trong brainstorm report, mục 5.
- **Mastery:** theo tài liệu, flashcard/SRS không sinh evidence (SRS là lớp riêng của Practice Service, chỉ dùng cho gợi ý
  và chỉ số Vocabulary coverage). Muốn từ vựng ảnh hưởng mastery thì phải thêm bài tập chấm điểm (mcq/fib/match) gắn KP
  VOCABULARY. Pipeline EXERCISE → `kp_evidence` hiện có đã hỗ trợ.

## Branch & migration numbering

- Repo `backend`, nhánh `feat/lesson-vocabulary` tạo từ `origin/main` (`ef769165`) ngày 2026-10-21.
- Tại thời điểm tách nhánh, `main` có library V14 và content V58; `fix/learn-writing-skill` đã có content V59–V70.
- Vocabulary dùng **library V15** và **content V72**.
- Khắc phục startup 2026-10-21: database đang dùng đã áp dụng V59–V70, trong khi source nhánh vocabulary thiếu 12 file này. Đã khôi phục nguyên bản V59–V70 từ `fix/learn-writing-skill` (commit `ae44b87d`); mọi file được kiểm tra trùng byte với bản gốc.
- Content hiện có đầy đủ V1–V58 và V72 (V59–V71 thuộc nhánh Writing, đã tách khỏi PR này); giữ nguyên V59–V70 khi tích hợp nhánh Writing để bảo toàn checksum và lịch sử Flyway. Kiểm lại số migration mới nhất trên `origin/main` trước PR.

## Dependencies

- Không chồng plan đang mở: `plans/261021-*` (writing KP, essay) và plan workspace `capstone-fall26/plans/261020-course-writing-guides`.
- Không phụ thuộc UI flashcard nữa: FE bỏ qua block VOCABULARY
  (frontend repo `src/features/learning-path/api/learningAdapters.ts:441-460`) và block này không chặn completion, nên seed sớm
  vô hại.

## Open questions

- None.

## Validation Log

### Session 1 — 2026-10-21

#### Verification Results
- Claims checked: 22
- Verified: 19 | Failed: 3 | Unverified: 0
- Tier: Full (5 phases)
- Failures:
  1. Mã gate là 403, không phải 409.
  2. Contract lesson đã có ở `lesson-learning-v1.md`, nên không tạo file mới.
  3. springdoc `/api/learning/**` đã có.
- Verified: library `public-endpoints` có `/api/content/vocabulary/**`. Số migration đo trên nhánh Writing (learning V34,
  content V70, library V4). Đã đo lại trên `main`: xem mục "Branch & migration numbering".
- Re-verify trên `main` (2026-10-21): `lesson_block_vocabulary` (content V8), `VocabularyController` (`/search`, `/{id}`),
  public path, `LessonResponse.vocabularySenseIds`, `isExercise()` loại VOCABULARY, `flashcard-srs-v1.md`,
  `lesson-learning-v1.md`, `DEMO_READING` đều có.

#### Decisions
| # | Câu hỏi | Quyết định |
|---|---|---|
| 1 | Sửa mã lỗi theo code thật? | Có (áp dụng cho phase 2–3, nay đã gỡ) |
| 2 | Contract đặt ở đâu? | Mở rộng `lesson-learning-v1.md` |
| 3 | Seed cho lesson nào? | L1–L4 topic `DEMO_READING`, 8–15 từ/lesson, người dùng duyệt danh sách |
| 4 | XP khi pass block từ vựng? | Không |

### Session 2 — 2026-10-21 (thu hẹp phạm vi)

#### Bối cảnh
Người dùng làm rõ: flashcard là **module riêng** của dev khác (library Leitner, `flashcard-srs-v1.md`), không phải UI cần
backend riêng. Vòng này chỉ làm phần từ vựng gắn với lesson. Việc học sẽ nối sang module flashcard sau.

#### Đọc tài liệu: từ vựng có ảnh hưởng mastery không?
- **Có, ở mức KP.** VOC là một miền trong cây (01 §3.1). Mastery tính bằng MP-1.0 từ evidence của bài tập chấm điểm
  (02 §A.3: context practice/review/test/ai_feedback; dạng mcq/fib/match).
- **Không, ở mức từng từ.** Từ được theo dõi bằng SRS Leitner 5 hộp, bảng `lexical_progress` (02 §A.7, 03 US-050–053).
  Lớp này tách khỏi Mastery Engine, chỉ dùng cho gợi ý (03 §E.6, priority 20) và chỉ số Vocabulary coverage (01 §6.2).
- **VOC/GRA không vào band** (03 §C.3). Khớp với `BandEstimator` trong code.

#### Decision
| # | Câu hỏi | Quyết định |
|---|---|---|
| 5 | Gate "học xong từ vựng mới qua bài" vòng này? | Không. Chỉ data; gate quyết khi nối module flashcard |

#### Propagation
- Xóa phase-02, phase-03.
- phase-04: bỏ phụ thuộc phase 3 và UI flashcard; thêm test "block VOCABULARY không chặn completion".
- phase-05: contract nối module flashcard, bỏ phần endpoint review/gate.
- brainstorm report: thêm addendum về phạm vi.

#### Whole-Plan Consistency Sweep
- Grep `phase-02|phase-03|lesson_vocabulary_progress|reviews|VOCABULARY_REQUIRED|UI flashcard|LessonCompletionRule|isGradable`
  trên mọi file plan: chỉ còn tham chiếu lịch sử trong Validation Log và mục Deferred. Unresolved contradictions: 0.

### Session 3 — 2026-10-21 (phase 5 closeout)

- Phase 5 hoàn tất: contract lesson + Library lookup + flashcard link và kiến trúc/mastery đã đồng bộ với DTO, V15/V72.
- Rà soát phase 1 phát hiện hai `@Override` trên cùng một phương thức; đã bỏ annotation thừa. Test và Gateway acceptance của phase 1/4 chưa chạy/xác minh, nên chưa thể đánh dấu toàn plan completed.
