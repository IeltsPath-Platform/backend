---
title: "Triển khai pipeline học chính (bài học, bài ôn, đề cuối)"
description: "MVP luồng học topic → bài → bài ôn → đề cuối: path theo user, chấm theo answer_spec, giấu đáp án tới khi đạt, bài ôn dùng gói PRACTICE_SET mới, mã đề TOPIC_TEST dùng một lần, assessment tự chấm và tự quyết loại attempt, consumer ghi trong cùng transaction và không gọi HTTP."
status: pending
priority: P2
branch: "feat/main-follow"
tags: [ai-learning, content, assessment, lesson, mastery, tdd]
blockedBy: [260929-1830-sync-database-and-service-split-docs]
blocks: []
created: "2026-09-29T09:22:17.145Z"
createdBy: "ck:plan"
source: skill
mode: "tdd"
---

# Triển khai pipeline học chính (bài học, bài ôn, đề cuối)

## Overview

Làm luồng học chính đã chốt ngày 2026-09-29:
1. Học viên bắt đầu học: path mastery theo user, tạo theo `sort_order`, không LLM.
2. Thứ tự học gồm các topic có bài và có đề.
3. Học tuần tự từng bài, nộp bài tập theo khối, đạt ≥ 70% mới xong.
4. Bài xong hoặc có kết quả đề thì chèn bài ôn bắt buộc khi cần. Bài ôn dùng gói câu mới.
5. Đề cuối (nhiều mã, mỗi lần giao dùng một lần) do assessment tự chấm. Kết quả qua event sang ai-learning; đạt thì PASSED và mở topic kế.

Nguồn quyết định:
- [Scout report](../reports/scout-260929-1316-lesson-pipeline-change-map-report.md)
- [Brainstorm report](../reports/brainstorm-260929-1611-lesson-feedback-review-question-pool-report.md)
- [main-learning-pipeline.md](../260928-2019-architecture-doc-service-split/main-learning-pipeline.md): thiết kế gốc, một phần đã bị thay.
- Report red-team: `./reports/from-code-reviewer-to-planner-red-team-*-plan-review-report.md`
- Demo (chưa theo brainstorm): https://claude.ai/artifact/Ex15FBWAZvftnrubzJK5kd

Chế độ `--tdd`: mỗi phase code theo trình tự test giữ hành vi cũ → sửa → test hành vi mới → cổng regression.

## Quyết định áp dụng

| Chủ đề | Quyết định |
| --- | --- |
| Path mastery | Một path mỗi user, tạo theo `sort_order`; không LLM, không lọc band, không cần goal. Tutor, `/status`, `/progress` dùng cùng path. `GET /topics` gọi refresh để gộp topic và KP mới |
| Thứ tự học | Tách khỏi path: topic có ≥ 1 bài và ≥ 1 mã đề, theo `sort_order`, lưu ở `topic_progress.sequence_order`. Tool reorder của tutor không ảnh hưởng. Cổng bài học quyết định luồng học; `next_objective` chỉ là gợi ý cho tutor |
| Mastery | `compute_mastery` DeepTutor giữ nguyên. Bài học và bài ôn ghi qua `app/learning/practice_evidence.py` với `source` là `lesson_exercise`/`review_set`, reference tất định theo `request_id`. Khối đã đạt nộp lại thì không ghi |
| Transaction | Mọi thay đổi của học viên (nộp bài, bài ôn, giao mã đề, phần bài học của consumer) chạy trong một path transaction, một connection. Consumer làm phần bài học trước khi ghi bản ghi idempotency |
| Cổng | Một hàm `authorize_lesson_access` cho GET bài, nộp bài, complete. Đề cuối đòi topic IN_PROGRESS, mọi bài xong, không bài ôn chờ |
| Phản hồi | Câu hỏi trả cho học viên theo danh sách trường cho phép. Chưa đạt: chỉ đúng/sai. Đạt: thêm đáp án và giải thích. Đề cuối: đúng/sai từng câu; lời giải khi ≥ 70% |
| Làm lại khối | Nộp lại cả khối, `requestId` mới, cùng câu |
| Đánh giá lại path | Ba lúc: bài vừa xong; kết quả TOPIC_GATE; kết quả MOCK, OFFICIAL_PRACTICE hoặc QUIZ. Chỉ chèn bài ôn (xếp lại topic hoãn). Chèn khi: mastery < ngưỡng, có câu sai trong kết quả vừa xét, và bài dạy KP đã xong (**tính cả bài vừa xong**). **Luyện thêm theo mastery** (chốt 2026-09-29): học viên yếu ở KP của bài vừa học được giao ngay gói câu mới trước khi mở bài kế; học viên làm tốt thì đi thẳng |
| Ngưỡng bài ôn | Setting `AI_LEARNING_REVIEW_MASTERY_THRESHOLD`, mặc định 0.6 |
| Bài ôn | Lý thuyết bài dạy KP + một gói `PRACTICE_SET` (≥ 3 câu) chưa làm. Trượt thì giao gói khác; hết gói thì lấy gói lâu nhất. Chỉ chủ sở hữu truy cập; nộp chỉ vào set đang mở |
| Đề cuối | Nhiều gói `TOPIC_TEST` mỗi topic (`content_packages.topic_id`). `POST /topics/{id}/test-assignments` idempotent. Mỗi lần giao chỉ dùng một lần; làm lại thì sang mã khác theo `package_id`. PASSED chỉ đi một chiều |
| Assessment | Tự quyết `attemptType` từ loại gói; từ chối `PRACTICE_SET`; `expiresAt` lấy từ luật của gói; không migration mới (dùng `answer_snapshot`, `item_results.max_score`); không gọi user-service lấy goal; event có `package_version_id`, `learning_goal_id = null` |
| Consumer | Không gọi HTTP: topic lấy từ lần giao mã đề, bài dạy KP lấy từ `lesson_progress.knowledge_point_ids` |
| Placement | Không tạo path, không ghi mastery, bỏ test-out |
| Nội dung | Nạp bằng seed; không làm API soạn nội dung. Học viên không đọc được danh sách gói hay câu hỏi |
| Band | Bỏ band của KP: content V7 xóa cột; API KP bỏ 4 trường band; ai-learning V10 xóa `mastery_path_knowledge_point_bands`, tool tutor bỏ band. Band của topic giữ để hiển thị (chốt 2026-09-29, sau red-team) |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Contract và đặc tả](./phase-01-contract-va-dac-ta.md) | Pending |
| 2 | [Chặn lộ đáp án](./phase-02-chan-lo-dap-an.md) | Pending |
| 3 | [Content: bài học, gói, endpoint nội bộ, seed](./phase-03-content-bai-hoc-va-goi.md) | Pending |
| 4 | [Assessment: tự chấm, lấy đề từ content, event](./phase-04-assessment-tu-cham-va-ma-de.md) | Pending |
| 5 | [AI Learning: path theo user](./phase-05-ai-learning-path-theo-user.md) | Pending |
| 6 | [AI Learning: API bài học, bài ôn, giao mã đề](./phase-06-ai-learning-api-bai-hoc-va-bai-on.md) | Pending |
| 7 | [AI Learning: consumer kết quả đề](./phase-07-ai-learning-consumer-ket-qua-de.md) | Pending |
| 8 | [Tài liệu và dọn dẹp](./phase-08-tai-lieu-va-don-dep.md) | Pending |

Thứ tự:

```text
1 ──► 2
1 ──► 3 ──┬──► 4 ──┐
1 ──► 5 ──┤        ├──► 7 ──► 8
          └──► 6 ──┘
(4 cần 3 và 5; 6 cần 3 và 5; 7 cần 4, 5 và 6)
```

- Phase 2, 3, 5 chạy song song được sau phase 1.
- **Triển khai:** phase 5 (consumer nhận event không có goal) phải chạy trước phase 4. Event không có goal gửi tới consumer cũ sẽ vào DLQ, và muốn cứu phải replay theo runbook trong contract.

## Ngoài phạm vi

- Topic premium, client access, guard entitlement. Lỗ `debit`/`refund`/`consume-human-grading`/entitlement của access-service thuộc **plan bảo mật riêng**.
- **Báo người dùng, không sửa ở đây** (AGENTS: không sửa như tác dụng phụ):
  - config-repo có secret fallback công khai cho internal JWT (`content-service.yaml:16`, `assessment-service.yaml:41`, `api-gateway.yaml:101`);
  - Gateway mở actuator `gateway` (`api-gateway.yaml:133`).
- Xếp lại topic chưa học.
- API soạn nội dung.
- Sửa `JacksonGameAnswerEvaluator` để đọc khóa `correct`. Snapshot game đã bị chặn lấy câu của đề, gói ôn, bài học ở phase 3.
- Tách library-service; viết lại `docs/system-architecture.md` (plan kiến trúc làm).
- Vai trò tutor trong bài học; nhắc học.
- Cập nhật demo tương tác.

## Tiêu chí nghiệm thu

- Học viên mới, không có goal: `GET /api/ai-learning/topics` tạo path, không gọi LLM; thứ tự học = [DEMO_READING, TFNG_SKILLS]; topic đầu IN_PROGRESS.
- `GET /lessons/L2` và nộp bài L2 khi L1 chưa xong: đều 403 `LESSON_LOCKED`.
- Nộp khối 2/3: không có `correctAnswer`/`explanation`. 3/3: có cả hai, bài xong, bài kế mở.
- Trùng `requestId` → kết quả cũ. Hai lần nộp song song cùng khối → bằng chứng ghi một lần. Lỗi giữa chừng rồi thử lại → dữ liệu đầy đủ, không trùng.
- Nộp lại khối đã đạt: mastery không đổi.
- Kịch bản Lan (ngưỡng 0.6):
  - L1 xong (KP3 0.88) → không chèn.
  - L2 xong (KP1 0.51, từng sai Q5) → giao gói luyện KP1 cùng lý thuyết L2; L3 bị chặn tới khi đạt.
  - L3, L4 đúng hết → không chèn.
  - Đề 3/4 sai Q15 → PASSED và chèn bài ôn L3 cho KP2.
- Học viên làm đúng hết L1–L4 ngay lần đầu → không bị giao gói nào (đi thẳng).
- Còn bài ôn chờ: mọi đường mở hay nộp bài khác đều 403 `REVIEW_REQUIRED`.
- Bài ôn: gói có câu đo KP2, không trùng câu bài học hay đề. Trượt → gói khác. Review của user khác → 404.
- Mã đề: gọi giao hai lần → cùng lần giao. Sau khi dùng → mã khác. Làm lại mã cũ trực tiếp qua assessment → không mở topic.
- Assessment: gói `TOPIC_TEST` luôn là `TOPIC_GATE`; gói `PRACTICE_SET` → 422.
- Consumer: lỗi phần bài học rồi giao lại event → PASSED đúng. Không gọi HTTP. Event không có goal được xử lý.
- API học viên không bao giờ trả `answerSpec`, hay `explanation` trước khi đạt. CUSTOMER đọc câu hỏi hoặc gói qua `/api/content` → 403. Gateway chặn `/internal/**`.
- Cùng bộ vector `answer-spec-v1` pass ở assessment (Java) và ai-learning (Python).

## Dependencies

- Không chặn plan nào. Plan kiến trúc `260928-2019-architecture-doc-service-split` được cập nhật đặc tả ở phase 1; phase 2 của plan đó chờ library-service, không chờ plan này.
- Cần duyệt contract (cuối phase 1) và migration V10 (phase 5).

## Câu hỏi mở

- Ngưỡng bài ôn 0.6 hay 0.9: để setting, mặc định 0.6.
- `/internal/**` vẫn nhận mọi internal JWT (không có credential cho service). Gateway đã chặn; secret fallback cần xử lý ở plan khác.

## Red Team Review

### Session — 2026-09-29
**Findings:** 15 sau khi gộp từ 40 phát hiện của 4 reviewer (15 accepted, 0 rejected)
**Severity breakdown:** 4 Critical, 8 High, 3 Medium

| # | Finding | Severity | Disposition | Applied To |
|---|---------|----------|-------------|------------|
| 1 | Assessment là nơi dò đáp án: học viên tự chọn gói và loại attempt; danh sách gói mở | Critical | Accept | Phase 1, 2, 4 |
| 2 | Consumer ghi phần bài học sau bản ghi idempotency; kết quả đỗ lại bỏ qua luật bài học | Critical | Accept | Phase 7 |
| 3 | Nộp bài ghi qua hai connection; không có `source` riêng cho bằng chứng; không khóa khi nộp song song | Critical | Accept | Phase 5, 6 |
| 4 | Phase 5 xóa `curriculum_scope.py` khi còn chỗ import; thiếu chỗ gọi và test | Critical | Accept | Phase 5 |
| 5 | Cổng chỉ ở GET; nộp bài và complete không bị chặn; KP chỉ lưu khi GET | High | Accept | Phase 6 |
| 6 | Giao mã đề bằng GET có ghi dữ liệu; mã đề dùng lại được mãi | High | Accept (modified: người dùng chọn giữ đúng/sai từng câu + mã đề một lần) | Phase 1, 6, 7 |
| 7 | Bài ôn không kiểm chủ sở hữu; nộp được vào set đã đóng | High | Accept | Phase 1, 6 |
| 8 | `explanation` lộ vì chỉ loại `answerSpec` | High | Accept | Phase 1, 6 |
| 9 | Path có topic cha hoặc không có bài; không refresh; reorder của tutor vượt thứ tự | High | Accept | Phase 1, 3, 6, 7 |
| 10 | Thứ tự triển khai mâu thuẫn; event vào DLQ và mất | High | Accept | Phase 1, 4, 5, plan.md |
| 11 | V10 trùng `updated_at` làm hỏng Flyway; kết quả đỗ lại bị kẹt; có thể giữ path rỗng | High | Accept | Phase 5 |
| 12 | Assessment thêm cột trùng; DTO dùng chung với người chấm; vẫn tra goal; bug expire | High | Accept | Phase 4 |
| 13 | V11 lưu một sự thật nhiều nơi; seed quá tay; TFNG không có đề | Medium | Accept | Phase 3, 6 |
| 14 | Phase 2/3 sót: video controller, bỏ `LESSON` làm vỡ reading, gói V4, game, Gateway | Medium | Accept | Phase 2, 3 |
| 15 | Phase 8 trùng việc với plan kiến trúc; cạnh `blocks` sai | Medium | Accept | Phase 8, plan.md, plan kiến trúc |

Quyết định của người dùng trong phiên: đề cuối **giữ đúng/sai từng câu**, kết hợp mã đề một lần dùng (thay vì chỉ báo điểm dưới 70%).

### Whole-Plan Consistency Sweep

- **Decision delta:**
  - `GET /topics/{id}/test` → `POST /topics/{id}/test-assignments`;
  - assessment không migration V5;
  - giữ `LESSON`;
  - ghi bằng chứng qua `practice_evidence.py` thay `record_external_quiz_outcome`;
  - thứ tự học ở `topic_progress.sequence_order`;
  - adapter chịu null chuyển sang phase 5; placement chuyển sang phase 5;
  - bỏ cạnh `blocks`.
- **Đã quét:** `plan.md`, phase 1–8. Thuật ngữ cũ (`GET /topics/{id}/test`, `solution_snapshot`, `V5__auto_grading`, `bỏ LESSON`, `record_external_quiz_outcome` như đường ghi, `best_percent`, `by-knowledge-points`) chỉ còn ở ngữ cảnh "thay bởi", "không dùng", hoặc trong bảng này.
- **Mâu thuẫn còn lại:** 0.
