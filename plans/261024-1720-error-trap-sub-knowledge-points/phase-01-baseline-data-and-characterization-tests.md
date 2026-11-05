---
phase: 1
title: Baseline data and characterization tests
status: completed
priority: P1
effort: 0.5d
dependencies: []
---

# Phase 1: Baseline data and characterization tests

## Overview
Đo dữ liệu thật để biết thiết kế có đủ câu bẫy và bộ luyện không, và khóa hành vi hiện tại bằng test trước khi sửa.

## Requirements
- Functional: số liệu độ phủ; test đặc tả hành vi chọn bộ, giỏ ôn, tìm lesson, ghi bằng chứng hiện tại.
- Non-functional: không đổi code sản phẩm trong phase này.

## Architecture
Chạy truy vấn trên `content_db`/`learning_db` local (Compose) hoặc trên DB Testcontainers sau toàn bộ migration.

## Related Code Files
- Create: `plans/261024-1720-error-trap-sub-knowledge-points/reports/baseline-data.md`
- Create (nếu thiếu): test đặc tả trong `services/content-service/src/test/...` cho `JdbcLearningContentReader.searchPracticeSets` (thứ tự lesson → khớp KP → mã bộ) và `practiceSetAvailability`
- Read: `services/learning-service/src/test/...` các test sẵn có của `GetReviewBasketUseCase`, `StartBasketReviewUseCase`, `CompletedLessonLocator`, `WritingCriterionReviews`, `ErrorTagger` (+ `docs/contracts/answer-spec-error-tags-v1-vectors.json`)

## Implementation Steps
1. Đếm: câu `TRUE_FALSE_NOT_GIVEN` theo đáp án đúng (`NOT_GIVEN`/`FALSE`/`NO`/`TRUE`/`YES`) và theo bộ chứa chúng; câu
   `CHOICE` có `distractors` theo mã; mã lỗi ACTIVE có/không có `knowledge_point_id`.
2. Mô phỏng sau backfill T/F/NG: với mỗi mã có KP cha, số bộ đã xuất bản của KP cha có ≥ 2 câu chứa bẫy (đủ để xếp bộ và
   tạo `ERROR_PATTERN`).
3. Ghi `reports/baseline-data.md`: bảng số liệu, mã nào có KP con mà KP cha không có bộ ≥ 2 câu bẫy (bài ôn không nhắm
   được bẫy đó). Số liệu này chỉ ghi ở báo cáo, không bắt buộc qua test (test độ phủ bộ luyện loại KP con, phase 2). <!-- red-team PH2 -->
4. (Bỏ khi nghiệm thu: phase 4 tự so cùng hồ sơ có/không KP con trong một test, không cần mốc riêng.)
5. Chạy test hiện có của learning và content; ghi lại test đang đỏ từ trước (ví dụ
   `ReviewBasketIntegrationTest.aWrongAnswerInTheMainFlowUpgradesTheOptionalReviewOfThatPointToBlocking` theo plan 261021-2016) để
   không nhầm là lỗi mới.
6. Viết test đặc tả còn thiếu cho thứ tự `searchPracticeSets` (gồm hòa điểm) để phase 3 không đổi thứ tự khi không có bẫy.

## Success Criteria
- [ ] `reports/baseline-data.md` có số liệu ở bước 1–3.
- [ ] Danh sách test đỏ có sẵn được ghi rõ.
- [ ] Test đặc tả thứ tự chọn bộ hiện tại xanh.

## Risk Assessment
- Số liệu cho thấy quá ít câu bẫy → báo người dùng trước phase 2 (thiết kế vẫn đúng nhưng ít tác dụng ngoài T/F/NG).
