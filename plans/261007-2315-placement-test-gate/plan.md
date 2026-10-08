---
title: Placement test bắt buộc trước khi chọn course
status: backend implemented and verified 2026-10-07 (phases 1-4); frontend pending
created: 2026-10-07
services: content-service, assessment-service, learning-service
---

# Placement test bắt buộc trước khi chọn course

## Mục tiêu

Học viên mới phải làm placement test (4 kỹ năng, LLM chấm) trước. Có band thì mới xem được course/topic. Band chỉ dùng để
đánh dấu course gợi ý (`recommended`), vẫn không khóa course nào sau khi đã qua cổng.

## Quyết định đã chốt (từ người dùng, 2026-10-07)

| # | Quyết định |
| --- | --- |
| D1 | Đề placement gồm Listening, Reading, Writing, Speaking. |
| D2 | Câu hỏi placement có `purpose` riêng `PLACEMENT` để lọc, tách khỏi `EXAM` (mock) và `LEARNING`. |
| D3 | Chỉ làm được một lần. Có placement rồi thì endpoint trả `409 PLACEMENT_ALREADY_DONE`. |
| D4 | Cổng ở backend: `GET /api/learning/courses` và `GET /api/learning/topics` trả `403 PLACEMENT_REQUIRED` tới khi có placement. **Đảo ngược** quyết định "every course stays open" trong AGENTS.md, README, system-architecture, contract. |
| D5 | Ban đầu chọn Learning tự quy đổi; **đổi thành Assessment tính band** (đã duyệt 2026-10-07) vì event `AssessmentCompleted.v2` không mang kỹ năng từng item và không mang band Writing của LLM. Assessment ghi `overall_band`; Learning chỉ lưu. |
| D6 | Writing: LLM chấm ra band (cơ chế gate essay hiện có). Speaking: nhận bài thu âm như bình thường nhưng điểm cố định cho MVP. |

## API mới và đổi contract

| Thay đổi | Chi tiết |
| --- | --- |
| Mới `GET /api/learning/placement-test` (CUSTOMER) | `200 {packageId, packageVersionId}`; `409 PLACEMENT_ALREADY_DONE`; `404 NO_PLACEMENT_TEST` khi chưa có package published. |
| Mới (nội bộ) `GET /internal/learning-content/placement-packages` | Content trả package `PLACEMENT_TEST` published, giống `courses/{id}/test-packages`. |
| Đổi `GET /api/learning/courses`, `GET /api/learning/topics` | Thêm `403 PLACEMENT_REQUIRED`. |
| Đổi enum `QuestionPurpose` | Thêm `PLACEMENT`; CHECK của `questions.purpose` thêm giá trị. |
| Không đổi | `AssessmentCompleted.v2` (field `overall_band` giữ nghĩa cũ, gateway route, JWT claim). |

## Phases

| Phase | Nội dung | File |
| --- | --- | --- |
| 1 | Content: purpose `PLACEMENT`, seed package 4 kỹ năng, internal endpoint | [phase-01-content-placement-package.md](phase-01-content-placement-package.md) |
| 2 | Assessment: chốt kết quả PLACEMENT, LLM chấm Writing, Speaking điểm cố định | [phase-02-assessment-placement-grading.md](phase-02-assessment-placement-grading.md) |
| 3 | Learning: endpoint, quy đổi band, `409`, cổng `403` | [phase-03-learning-placement-gate.md](phase-03-learning-placement-gate.md) |
| 4 | Docs, contract, test liên thông, bàn giao FE | [phase-04-docs-and-verification.md](phase-04-docs-and-verification.md) |

Thứ tự: 1 → 2 → 3 → 4. Phase 2 và 3 phụ thuộc package ở phase 1.

## Acceptance criteria

- Tài khoản mới gọi `GET /courses` nhận `403 PLACEMENT_REQUIRED`.
- `GET /placement-test` trả `packageVersionId` có đủ 4 skill; tạo attempt từ đó có `attemptType = PLACEMENT`.
- Nộp bài xong, sau khi LLM chấm Writing, Learning lưu `learner_placements`, `GET /courses` trả 200 với đúng một course `recommended=true`.
- `GET /placement-test` lần hai trả `409 PLACEMENT_ALREADY_DONE`.
- Câu `PLACEMENT` không xuất hiện trong Practice, thi topic/course hay mock; publish package khác mà dùng câu `PLACEMENT` bị từ chối.
- Test hẹp từng service qua, docs đã sửa khớp hành vi.

## Quyết định bổ sung (đã duyệt 2026-10-07)

- D7: bảng quy đổi % → band và công thức gộp ở phase 3 được duyệt.
- D8: Speaking band cố định 5.5, cấu hình qua property.
- D9: không cấp miễn cho học viên cũ (MVP không có dữ liệu như vậy); mọi học viên chưa có placement đều bị cổng chặn, kể cả tài khoản demo.
- D10: cổng chặn **toàn bộ** `/api/learning/**` trừ `GET /placement-test` (courses, topics, lessons, practice, review, writing, mastery).
- D11: LLM lỗi, thiếu cấu hình hoặc hết quota khi chấm Writing placement thì dùng band mặc định (property, 5.5), không chuyển EXAMINER.
