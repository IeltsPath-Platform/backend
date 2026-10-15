---
title: "Thư viện luyện đề IELTS: full đề, bài lẻ theo part, lịch sử và lượt làm"
description: "Cho học viên luyện từng đề, full đề hoặc từng part của Reading/Listening: catalog ở content, attempt theo phạm vi section ở assessment, lịch sử và lượt làm bài."
status: pending
priority: P2
branch: "practice_test"
tags: [content, assessment, tdd]
blockedBy: [261014-1021-practice-test-data-model]
blocks: []
created: "2026-10-14T03:02:59.000Z"
createdBy: "ck:plan"
source: skill
---

# Thư viện luyện đề IELTS: full đề, bài lẻ theo part, lịch sử và lượt làm

## Overview

Học viên cần một thư viện đề tách khỏi lộ trình học: lọc theo kỹ năng, theo nguồn tài liệu, chọn **Full đề** hoặc **Bài lẻ**
(Passage 1/2/3 của Reading, Section 1-4 của Listening), tìm theo tên, xem tab chưa làm/đã làm, lịch sử và số lượt làm.
Mỗi thẻ đề hiện ảnh bìa, badge part, nguồn và các dạng câu hỏi (Gap Filling, Matching Information…).

Hiện trạng đã kiểm trên code và DB thật (2026-10-14, schema content tới V26, assessment tới V7):

- Không có đề nào để hiện: 0 package `MOCK_TEST`, 0 câu hỏi `purpose = EXAM`. 38 `PRACTICE_SET` là bài ôn gắn lesson (Learning
  chấm, Assessment từ chối làm attempt); 13 `TOPIC_TEST` và 4 `COURSE_TEST` là cổng của lộ trình.
- `GET /api/content/packages` chỉ cho ADMIN/CONTENT_AUTHOR; mọi endpoint "đề" cho học viên hiện là nội bộ
  (`/internal/learning-content/*`). Chưa có catalog cho học viên.
- `content_sections` có `skill`, `sort_order`, `time_limit_seconds` nhưng không có số part, ảnh bìa, nhóm câu hỏi; package không có nguồn.
- `answer_spec` chỉ có `CHOICE`, `FILL`, `ESSAY`, `SPEAKING`; chưa chấm được "chọn nhiều đáp án".
- Assessment: `assessment_attempts` chỉ lưu `package_version_id` (đề ra version mới thì "đã làm" thành "chưa làm");
  attempt luôn lấy mọi section; `expiresAt` luôn null nên `TIMED` chưa có hiệu lực; auto-grade không ghi `skill_scores`;
  học viên chỉ thấy lời giải khi đạt ≥ 70%; không có endpoint liệt kê attempt.
- Không service nào kiểm `requiredFeatureKey` khi cho làm bài: nhãn "PRO" hiện chỉ là hiển thị (Learning chỉ đổi nhãn
  `accessLevel`). Endpoint entitlement của Access là ADMIN-only và ghi chú "chưa service nào gọi".
- Learning đã nhận `OFFICIAL_PRACTICE` (whitelist parser, `REVIEWED_TYPES` kích hoạt đánh giá lại bài ôn). Enum, check constraint
  DB và contract `assessment-completed-v2` đã có giá trị này nên không đổi contract event.

Nguồn: trao đổi thiết kế với chủ dự án 2026-10-13/14 (ảnh giao diện thư viện đề đối chiếu).

## Giao cho người implement

Base: nhánh `practice_test` @ `ba02556`; lúc lập plan working tree chỉ có thư mục `plans/` chưa theo dõi.

1. Đọc theo thứ tự: `AGENTS.md`, `CLAUDE.md`, `plan.md` này (bảng quyết định, câu hỏi mở), rồi từng phase trước khi làm phase đó.
   Bảng quyết định là chốt của chủ dự án: **không tự đổi**.
2. Làm trên nhánh `practice_test`. Mỗi phase một commit conventional (`feat(content): ...`, `feat(assessment): ...`).
   Không ghi mã plan, số phase hay mã quyết định (D1, Q2…) vào code, comment, tên test, migration hay commit.
3. TDD: viết test đỏ của phase, chạy để thấy đỏ, rồi code cho xanh. Lệnh hẹp nhất trước:
   `mvn -q -pl services/<service> -am test`. Testcontainers cần Docker; thiếu Docker thì ghi rõ test nào bị skip, không tuyên bố đã chạy.
4. Gốc package Java là `com.ieltspath.<service>` (các plan cũ ghi `com.group01` là cũ). Theo layer trong `AGENTS.md` §3.1:
   không đưa JPA/Spring vào `domain`; không gọi repository trong vòng lặp; endpoint danh sách phải phân trang và lọc ở DB.
5. **Schema (migration content `V28`, assessment `V8`) do plan [`261014-1021-practice-test-data-model`](../261014-1021-practice-test-data-model/plan.md) làm trước**
   và là nguồn duy nhất cho bảng, cột, ràng buộc, quan hệ; plan này chỉ viết code dùng chúng và thêm migration seed `V29` ở phase 8. `V28` hoặc `V8` chưa có khi bắt tay vào làm → dừng và ghi `BLOCKED`.
6. Chỉ được sửa kỳ vọng của test cũ khi thỏa **cả ba**: (a) test đỏ chỉ vì dữ liệu/số đếm do chính plan này thêm (migration, seed,
   cột mới); (b) chỉ sửa con số hoặc danh sách cho khớp, không xóa test, không nới assertion; (c) ghi vào Verification tên test,
   kỳ vọng cũ → mới, lý do. Test cũ đỏ vì lý do khác: dừng, ghi `BLOCKED`.
7. Dừng và ghi `BLOCKED` khi: quyết định mâu thuẫn với code thật, phải đổi contract hoặc route ngoài plan, đụng
   `common-security`/Gateway/JWT, hoặc Docker không chạy được (khi đó không commit phase có migration).
8. Nội dung seed (passage, câu hỏi, đáp án) tự viết mới theo mẫu seed hiện có, **không chép đề từ nguồn bên ngoài**; chép
   toàn văn vào Verification để chủ dự án duyệt trong PR.
9. Không sửa `third_party/`, `.env`, Docker, config tập trung ngoài `springdoc.paths-to-match` của chính hai service; không commit `graphify-out/`.
   Không push, không tạo PR.

## Quyết định đã chốt (implementer không được tự đổi)

| # | Quyết định |
| --- | --- |
| D1 | Loại gói mới `PRACTICE_TEST` (content) → `AttemptType.OFFICIAL_PRACTICE` (assessment). Không dùng lại `MOCK_TEST` (mock = thi thử nhiều kỹ năng, Writing do EXAMINER chấm). Không đổi event contract. |
| D2 | **Một đề, ba cách làm**: Full đề = mọi section; Bài lẻ = tập con section qua `sectionIds` khi bắt đầu attempt. Không nhân bản package cho từng part. |
| D3 | Danh tính một part qua các version là `(package_id, part_no)`, không phải `section.id` (đổi mỗi version). |
| D4 | Catalog ở **content** (dữ liệu đề), trạng thái/lượt làm ở **assessment** (dữ liệu người học). Hai DB không đọc nhau; frontend ghép hai phản hồi. Gateway không chứa logic. |
| D5 | Skill của đề suy ra từ `content_sections.skill`, không thêm cột `skill` vào `content_packages`. Thẻ Full đề dùng ảnh của part 1, không có cột ảnh ở package. |
| D6 | Không bảng riêng cho số lượt làm; đếm từ `assessment_attempts` có index. Chưa cache. |
| D7 | Phạm vi: **Reading và Listening** chấm tự động. Writing, Speaking, Dictation theo đề, "Review đề thi thật" nằm ngoài. |
| D8 | Mỗi đề `PRACTICE_TEST` là **một skill**; publish kiểm mọi section cùng skill, có `part_no` duy nhất trong version, và gói có `source_code` hợp lệ (thuộc `TestSource`). |
| D9 | Nguồn tài liệu là **một cột** `content_packages.source_code`; mã hợp lệ, tên hiển thị và thứ tự do enum `TestSource` (domain) quyết định, không có bảng nguồn. Thêm nguồn mới là sửa enum; chưa có API admin cho nguồn. |
| D10 | Đổi quyền/giá điểm/cache: không thuộc plan này. |

## Câu hỏi mở (cần chủ dự án quyết; mặc định ghi sẵn)

| # | Câu hỏi | Mặc định nếu không trả lời | Ảnh hưởng |
| --- | --- | --- | --- |
| Q1 | Duyệt đổi `answer-spec-v1` để chấm "chọn nhiều đáp án" (Multiple Choice Many Answers)? | **Chưa duyệt**: phase 7 không làm; đề không dùng dạng này. | Phase 7 (contract dùng chung, `AGENTS.md` cấm đổi khi chưa duyệt). |
| Q2 | Chặn thật khóa PRO khi bắt đầu attempt? Cần endpoint mới ở access-service. | **Chưa duyệt**: catalog trả `requiredFeatureKey` và frontend khóa giao diện, giống topic premium hiện tại (hiển thị, không chặn API). | Phase 6. |
| Q3 | Hiện lời giải ngay sau khi nộp cho attempt `OFFICIAL_PRACTICE` (hiện chỉ khi ≥ 70%)? | **Có**, chỉ cho `OFFICIAL_PRACTICE`; loại attempt khác giữ nguyên. | Phase 5. |
| Q4 | Bảng quy đổi điểm thô → band Listening/Reading: lấy nguồn chính thức nào? | Dùng bảng chủ dự án cung cấp; **chưa có thì không tính band** (chỉ điểm thô). Chỉ tính khi tổng điểm tối đa là 40. | Phase 5. |
| Q5 | Chế độ `TIMED` mà một section được chọn thiếu `time_limit_seconds`? | Từ chối (`422`), không âm thầm chạy không giờ. | Phase 3. |
| Q6 | Full đề có cần ảnh riêng thay vì ảnh part 1? | Không (D5); thêm cột sau cũng được. | Phase 1-2. |
| Q7 | Học viên nộp đề có giờ sau khi hết giờ: hiện attempt thành `EXPIRED` và **mất bài** (và lưu đáp án không kiểm hạn). | Với `OFFICIAL_PRACTICE`: nộp sau hạn vẫn chấm trên đáp án lưu trước hạn, lưu đáp án sau hạn bị từ chối; loại attempt khác giữ nguyên. | Phase 3 (bắt buộc khi bật `TIMED`). |
| Q8 | Frontend lấy danh sách nguồn (mã, tên, thứ tự) cho bộ lọc "Nguồn tài liệu" từ đâu? Nguồn nằm trong enum `TestSource`, không còn bảng để truy vấn. | Thêm `GET /api/content/practice-tests/sources` đọc từ enum (không đụng schema) để tên và thứ tự chỉ có một chỗ định nghĩa. Frontend tự khai báo cũng được nhưng sẽ lệch khi thêm nguồn. | Phase 2. |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Content model và authoring](./phase-01-content-schema-and-authoring.md) | Pending |
| 2 | [Content catalog API và payload nội bộ](./phase-02-content-catalog-api.md) | Pending |
| 3 | [Assessment attempt theo phạm vi section](./phase-03-assessment-attempt-scope.md) | Pending |
| 4 | [Assessment lịch sử, tiến độ, lượt làm](./phase-04-assessment-history-and-stats.md) | Pending |
| 5 | [Assessment điểm và lời giải cho đề luyện](./phase-05-assessment-practice-results.md) | Pending |
| 6 | [Chặn khóa PRO (chờ Q2)](./phase-06-access-feature-gate.md) | Blocked on Q2 |
| 7 | [Chọn nhiều đáp án (chờ Q1)](./phase-07-multi-answer-question.md) | Blocked on Q1 |
| 8 | [Seed demo, docs và verification](./phase-08-seed-docs-and-verification.md) | Pending |

Thứ tự commit: 1 → 2 → 3 → 4 → 5 → 8 (6 và 7 chỉ khi được duyệt). Phase 3 cần phase 2 vì Assessment đọc payload nội bộ mới của content.

## Acceptance criteria

Dữ liệu ví dụ: nguồn `IELTS_SPACE_PRO` và `C10_C20`; đề Reading `R-A` (nguồn `IELTS_SPACE_PRO`, 3 part, nhiều câu mỗi part, mỗi part có nhóm câu hỏi) và `R-B`
(nguồn `C10_C20`, 1 part).

- `POST /api/content/practice-tests/search` `{skill: READING, scope: PART}`: mỗi thẻ là một section, có `partNo`, `coverUrl`,
  `source {code, name}`, `formats` đúng thứ tự nhóm, `questionCount`, `requiredFeatureKey`. Lọc `sources`, `partNo`, `query` chạy ở DB, có phân trang.
- `scope: FULL`: mỗi thẻ là một đề (`R-A`, `R-B`); ảnh là ảnh part 1.
- `POST /api/assessments/attempts` với `sectionIds` của part 2 của `R-A`: attempt chỉ có section đó, `scope = PART`, `package_id` có giá trị.
  `sectionIds` không thuộc version → 4xx; gửi `sectionIds` cho package khác `PRACTICE_TEST` → 4xx.
- Học viên làm part 1 của `R-A` rồi publish version 2 của `R-A`: part 1 vẫn "đã làm", part 2 vẫn "chưa làm".
- `POST /api/assessments/practice-stats` trả lượt làm theo package và theo part; `GET /api/assessments/practice-progress` trả danh sách part đã làm.
- Nộp full đề Reading 40 câu: có `skill_scores` (`raw_score`, `band` khi có bảng quy đổi, `grading_source = AUTO`); nộp part lẻ: chỉ `raw_score`.
- Lời giải có ngay sau khi nộp attempt `OFFICIAL_PRACTICE` dù dưới 70% (Q3); attempt loại khác không đổi.
- Event `AssessmentCompleted.v2` của attempt `OFFICIAL_PRACTICE` được Learning parse không vào DLQ.
- Mọi test content, assessment, learning hiện có vẫn xanh;
  `mvn -q -pl services/content-service,services/assessment-service,services/learning-service -am test` xanh.

## Ngoài phạm vi

Writing/Speaking/Dictation theo đề · "Review đề thi thật" · API/bảng quản lý nguồn tài liệu · cache số lượt làm · bảng tổng hợp
lượt làm · chặn PRO (cho tới khi Q2 được duyệt) · chọn nhiều đáp án (cho tới khi Q1 được duyệt) · giao diện · nhập đề hàng loạt.

## Dependencies

- Chờ plan [`261014-1021-practice-test-data-model`](../261014-1021-practice-test-data-model/plan.md) (schema V28/V8). Index `(package_id, status)` ở assessment
  chỉ có ích nếu giữ endpoint `stats` của phase 4 (câu hỏi mở O1 của plan đó).
- Không có plan nào khác đang mở chồng lấn. Dựa trên mô hình `content_packages`/`content_package_versions`, `AttemptCreator` snapshot của assessment và
  `PublishContentPackageUseCase` hiện có.

## Verification

(Implementer điền khi làm: lệnh đã chạy, test bị skip vì thiếu Docker, test cũ đã sửa kỳ vọng, nội dung seed mới.)
