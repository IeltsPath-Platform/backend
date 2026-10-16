---
title: "Luyện 4 kỹ năng: Reading (thư viện đề Reading từ catalog đến kết quả)"
description: "Lát cắt Reading của thư viện luyện đề: catalog đề/part, làm bài có giờ, tiến độ, lịch sử, điểm và lời giải. Seed demo V30 đã có sẵn trong DB local."
status: pending
priority: P1
branch: "feat/practice_skills"
tags: [content, assessment, reading, tdd]
blockedBy: []
blocks: []
created: "2026-10-15T04:42:06.750Z"
createdBy: "ck:plan"
source: skill
---

# Luyện 4 kỹ năng: Reading

## Overview

Mục "Luyện 4 kỹ năng" có bốn tab: Reading, Listening, Writing, Speaking. Plan này chỉ làm **Reading**: học viên thấy thư viện đề Reading
(lọc nguồn, Full đề hoặc Bài lẻ Passage 1/2/3, tìm tên, tab chưa làm/đã làm), làm bài có đồng hồ, nộp, xem điểm thô và lời giải ngay, xem lịch sử.

**Đây là lát cắt thực thi của hai plan thiết kế đã có**, không thiết kế lại:

- [`261014-1002-practice-test-library`](../261014-1002-practice-test-library/plan.md): bảng quyết định D1–D10, câu hỏi mở Q1–Q8 và đặc tả chi tiết từng phase.
- [`261014-1021-practice-test-data-model`](../261014-1021-practice-test-data-model/plan.md): schema (S1–S10). Content `V28` đã làm; assessment `V8` chưa.

Mỗi phase của plan này trỏ tới phase thiết kế tương ứng và chỉ ghi **phần lệch / cắt / sửa** (đã đối chiếu với repo ngày 2026-10-15).
Phase thiết kế cũ vẫn là nguồn cho tên field, test cần viết, rủi ro. Mâu thuẫn giữa phase cũ và "Lệch so với plan cũ" bên dưới → plan này thắng.

## Hiện trạng đã kiểm (2026-10-15, nhánh `feat/practice_skills` @ `c914e6a`)

| Hạng mục | Trạng thái |
| --- | --- |
| Content schema `V28` (`source_code`, `part_no`, `cover_asset_id`, `section_question_groups`, `section_questions.group_id`, loại gói `PRACTICE_TEST`) | **Đã áp dụng** (file + DB local). |
| Content `V29__question_version_guide` (cột `guide` cho Writing) | Đã có, không liên quan Reading. |
| Learning `V11` (bài Writing tự do) | Đã có, không liên quan Reading. |
| Assessment schema | Mới tới `V7`. **`V8` chưa có**. |
| Code Java cho thư viện đề (enum, entity, API, use case) | **Chưa có dòng nào.** `PackageType` chưa có `PRACTICE_TEST`. |
| Seed Reading demo | **Đã tạo và đã chèn vào `content_db` local**: [`V30__seed_reading_practice_demo.sql`](../../services/content-service/src/main/resources/db/migration/V30__seed_reading_practice_demo.sql). Xem mục "Dữ liệu demo". |
| `ReadingController` / `GetReadingPassageUseCase` hiện có | Phục vụ gia sư (đoạn văn demo), **không liên quan**; không sửa. |

## Lệch so với plan cũ (plan này thắng)

| # | Plan cũ nói | Thực tế / quyết định mới |
| --- | --- | --- |
| L1 | Nhánh `practice_test`, base `ba02556` | Làm trên `feat/practice_skills`. |
| L2 | Seed là `V29__seed_practice_test_demo.sql` | `V29` đã bị `question_version_guide` dùng. Seed là **`V30__seed_reading_practice_demo.sql`, đã viết xong**; phase 6 chỉ kiểm, không viết lại. |
| L3 | Phase 1 của plan data-model (content `V28`) còn pending | Xong. Chỉ còn assessment `V8` (làm trong phase 3 ở đây, gộp DDL của plan data-model phase 2). |
| L4 | Bao gồm Listening, `POST /stats`, index `idx_assessment_attempts_package_status` (O1) | **Cắt** (YAGNI): màn hình đích không hiện lượt làm. Không `stats`, không index đó. Code vẫn không cứng skill, nên Listening thêm sau chỉ cần seed + audio. |
| L5 | Phase 6 (khóa PRO) và phase 7 (chọn nhiều đáp án) | **Ngoài phạm vi.** Q1, Q2 giữ mặc định "chưa duyệt". Catalog vẫn trả `requiredFeatureKey` để frontend hiện ribbon PRO. |
| L6 | Nội dung seed chép vào Verification để duyệt | Đã là file SQL trong repo; Verification chỉ cần dẫn tới file. |

## Giao cho người implement (Codex)

1. Đọc theo thứ tự: `AGENTS.md`, `CLAUDE.md`, `plan.md` này, rồi **từng phase của plan này kèm phase thiết kế nó trỏ tới** trước khi làm phase đó.
2. Bảng quyết định D1–D10 (plan 1002) là chốt của chủ dự án: không tự đổi. Mục L1–L6 ở trên là chốt mới nhất.
3. Một phase = một commit conventional (`feat(content): ...`, `feat(assessment): ...`). **Không** ghi mã plan, số phase, mã quyết định (D1, L2, Q3…) vào code, comment, tên test, migration hay commit.
4. TDD: viết test đỏ trước, thấy đỏ, rồi code cho xanh. Lệnh hẹp nhất trước: `mvn -q -pl services/<service> -am test`.
   Testcontainers cần Docker; thiếu Docker thì ghi rõ test nào bị skip, **không tuyên bố đã chạy**, và không commit phase có migration.
5. Gốc package Java là `com.ieltspath.<service>`. Theo layer `AGENTS.md` §3.1: không JPA/Spring trong `domain`; không gọi repository/HTTP trong vòng lặp;
   endpoint danh sách phân trang và lọc ở DB; không expose JPA entity; không `answerSpec` cho học viên.
6. Không sửa migration đã áp dụng (V28, V29, V30 đã chạy trên DB local; sửa nội dung seed = migration mới). Không sửa `third_party/`, `.env`, Docker,
   `common-security`, Gateway, config tập trung ngoài `springdoc.paths-to-match` của chính hai service. Không push, không tạo PR, không commit `graphify-out/`.
7. Test cũ đỏ: chỉ được sửa kỳ vọng khi **cả ba** đúng: (a) đỏ chỉ vì số đếm/dữ liệu do chính plan này thêm (V30, V8); (b) chỉ đổi con số/danh sách, không xóa, không nới assertion;
   (c) ghi test, kỳ vọng cũ → mới, lý do vào Verification. Khác thế: dừng, ghi `BLOCKED`.
8. Dừng và ghi `BLOCKED` khi: quyết định mâu thuẫn code thật, phải đổi contract/route ngoài plan, đụng `common-security`/Gateway/JWT, hoặc Docker không chạy.

## Phases

| Phase | Name | Thiết kế gốc | Status |
|-------|------|--------------|--------|
| 1 | [Content domain và authoring](./phase-01-content-domain-v-authoring.md) | 1002/phase-01 | Pending |
| 2 | [Content catalog API](./phase-02-content-catalog-api.md) | 1002/phase-02 | Pending |
| 3 | [Assessment V8 và attempt theo part](./phase-03-assessment-v8-v-attempt-theo-part.md) | 1021/phase-02 + 1002/phase-03 | Pending |
| 4 | [Assessment tiến độ và lịch sử](./phase-04-assessment-ti-n-v-l-ch-s.md) | 1002/phase-04 (bỏ `stats`) | Pending |
| 5 | [Assessment điểm và lời giải](./phase-05-assessment-i-m-v-l-i-gi-i.md) | 1002/phase-05 (không band) | Pending |
| 6 | [Seed verification, docs và e2e](./phase-06-seed-verification-docs-v-e2e.md) | 1002/phase-08 | Pending |

Thứ tự bắt buộc: 1 → 2 → 3 → 4 → 5 → 6. Phase 3 đọc payload nội bộ do phase 2 mở rộng. Phase 1 và 3 độc lập về code nhưng giữ thứ tự để mỗi commit xanh.

## Dữ liệu demo (đã có trong `content_db` local)

Ba gói `PRACTICE_TEST` `PUBLISHED`, version 1, mọi câu `purpose = EXAM`, `answer_spec` chỉ `CHOICE`/`FILL`, mỗi part 20 phút (`time_limit_seconds = 1200`),
ảnh bìa là SVG data URI. Nội dung tự viết, không chép từ nguồn ngoài.

| Gói (`code`) | Nguồn | PRO | Part | Câu | Nhóm câu hỏi (`format_code`) |
| --- | --- | --- | --- | --- | --- |
| `PT-READING-ISP-01` "Reading Practice Test 1" | `IELTS_SPACE_PRO` | Không | 3 | 15 | P1: `TRUE_FALSE_NOT_GIVEN` 3 + `GAP_FILLING` 2 · P2: `MULTIPLE_CHOICE_ONE` 3 + `MATCHING_INFORMATION` 2 · P3: `YES_NO_NOT_GIVEN` 3 + `SENTENCE_COMPLETION` 2 |
| `PT-READING-C10-01` "Reading Practice Test 2" | `C10_C20` | Không | 1 | 6 | `TRUE_FALSE_NOT_GIVEN` 3 + `MULTIPLE_CHOICE_ONE` 2 + `MATCHING_INFORMATION` 1 |
| `PT-READING-ACT-01` "Reading Practice Test 3" | `ACTUAL_TESTS` | `PREMIUM_CONTENT` | 2 | 10 | P1: `TRUE_FALSE_NOT_GIVEN` 3 + `GAP_FILLING` 2 · P2: `MULTIPLE_CHOICE_ONE` 3 + `MATCHING_INFORMATION` 2 |

Tổng 3 gói, 6 section, 13 nhóm, 31 câu, 6 passage, 6 ảnh bìa. UUID cố định tiền tố `2d000000-0000-4000-8000-`.
Dữ liệu người học (attempt, kết quả) **không** seed: sinh ra khi học viên làm bài.

> **Cảnh báo tạm thời:** cho tới khi phase 1 thêm `PRACTICE_TEST` vào enum `PackageType`, endpoint admin `GET /api/content/packages` sẽ lỗi 500 khi đọc
> các gói mới (Hibernate không map được giá trị lạ). Luồng học viên dự kiến không bị ảnh hưởng (Learning/Assessment/Game đọc content qua JDBC/HTTP theo loại gói cụ thể); phase 1 có test khẳng định.
> Cách tạm gỡ nếu cần ngay: `DELETE` theo tiền tố UUID `2d000000-...` (xem phase 6 mục Rollback).

## Acceptance criteria

Dùng đúng bộ dữ liệu demo ở trên.

- `POST /api/content/practice-tests/search` `{skill: READING, scope: PART}` trả 6 thẻ (mỗi section một thẻ), mỗi thẻ có `partNo`, `coverUrl`, `source {code, name}`,
  `formats` đúng thứ tự nhóm, `questionCount`, `timeLimitSeconds = 1200`, `requiredFeatureKey` (chỉ 2 thẻ của `PT-READING-ACT-01` là `PREMIUM_CONTENT`).
- `scope: FULL` trả 3 thẻ; thẻ `PT-READING-ISP-01` có `questionCount = 15`, `timeLimitSeconds = 3600`, `parts` 3 phần tử, ảnh là ảnh part 1.
- Lọc `sources: ["C10_C20"]` chỉ trả đề C10; `query` "walking" trả đúng part walking-buses; `partNo = 2` + `PART` chỉ trả part 2.
- `POST /api/assessments/attempts` với `sectionIds` của Passage 2 của `PT-READING-ISP-01`: attempt chỉ có section đó, `scope = PART`, `package_id` có giá trị, `skill = READING`,
  `attemptType = OFFICIAL_PRACTICE`. `sectionIds` không thuộc version → 4xx. `sectionIds` cho gói loại khác → 4xx.
- Nộp đúng toàn bộ 5 câu Passage 2 → điểm thô 5/5, có `skill_scores` (`raw_score = 5`, `band = null`, `grading_source = AUTO`), lời giải có ngay dù điểm thấp.
- `GET /api/assessments/practice/progress?skill=READING&scope=PART` sau đó có khóa `(PT-READING-ISP-01 package, 2)`; Passage 1 vẫn "chưa làm".
- `GET /api/assessments/practice/history?skill=READING` có lượt vừa làm.
- Học viên không bao giờ nhận `answerSpec` từ catalog hay từ payload attempt đang làm dở.
- Không test cũ nào đổi kỳ vọng ngoài luật số 7; `mvn -q -pl services/content-service,services/assessment-service,services/learning-service -am test` xanh.

## Ngoài phạm vi

Listening/Writing/Speaking/Dictation theo đề · khóa PRO thật · chọn nhiều đáp án · bảng quy đổi band · `stats` lượt làm · cache · giao diện ·
API soạn câu hỏi/nhóm · nhập đề hàng loạt · sửa `ReadingController` của gia sư.

## Dependencies

- Dựa trên `V28` (đã có). Không plan mở nào khác chồng lấn: `261009-1417-practice-gated-lesson-unlock` chỉ đụng Learning.
- Hai plan 1002/1021 vẫn là tài liệu thiết kế sống; chúng được ghi chú trỏ về plan này (xem đầu `plan.md` của hai plan đó).

## Câu hỏi chưa giải quyết

- Q4 (bảng quy đổi điểm thô → band): chưa có bảng được xác nhận → plan này không tính band. Cần chủ dự án cung cấp nếu muốn hiện band.
- Có cần hiện nhãn "PRO" thật (chặn bắt đầu attempt) không? Mặc định: chưa (Q2).
- Frontend lấy danh sách nguồn từ `GET /api/content/practice-tests/sources` (Q8, mặc định có) hay tự khai báo?

## Verification

(Implementer điền khi làm: lệnh đã chạy, test bị skip vì thiếu Docker, test cũ đã đổi kỳ vọng, kết quả e2e qua Gateway.)
