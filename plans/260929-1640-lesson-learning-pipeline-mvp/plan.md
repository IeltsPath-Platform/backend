---
title: "Triển khai pipeline học chính (bài học, bài ôn, đề cuối)"
description: "MVP luồng học topic → bài → bài ôn → đề cuối: path theo user, chấm theo answer_spec, giấu đáp án tới khi đạt, bài ôn dùng gói PRACTICE_SET mới, mã đề TOPIC_TEST dùng một lần, assessment tự chấm và tự quyết loại attempt, consumer ghi trong cùng transaction và không gọi HTTP."
status: completed
priority: P2
branch: "feat/main-follow"
tags: [ai-learning, content, assessment, lesson, mastery, tdd]
blockedBy: []
blocks: [260930-0851-listening-topic-audio-lessons, 260930-1006-reading-question-hints]
created: "2026-09-29T09:22:17.145Z"
createdBy: "ck:plan"
source: skill
mode: "tdd"
---

# Triển khai pipeline học chính (bài học, bài ôn, đề cuối)

> **Đổi 2026-10-01:** người dùng bỏ ai-learning Python. Phase 5–7 (path, API bài học/bài ôn/mã đề, consumer) do plan `261001-1228` làm bằng Java (`learning-service`, route `/api/learning/**`, mô hình `kp_evidence` thay path DeepTutor). Luật nghiệp vụ trong plan này vẫn là nguồn. **2026-10-02:** phase 4 và 8 đã hoàn tất; nền pipeline có [report live E2E](./reports/e2e-261002-foundation-learning-pipeline.md). Các quyết định và Validation Log nhắc runtime Python bên dưới được đọc theo ánh xạ Java, không phải mô tả runtime hiện tại.

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
- `main-learning-pipeline.md` (plan kiến trúc cũ, đã xóa khỏi working tree; git history, commit `6506d5e`): thiết kế gốc, một phần đã bị thay bởi plan này.
- Report red-team: `./reports/from-code-reviewer-to-planner-red-team-*-plan-review-report.md`
- Demo (chưa theo brainstorm): https://claude.ai/artifact/Ex15FBWAZvftnrubzJK5kd

Chế độ `--tdd`: mỗi phase code theo trình tự test giữ hành vi cũ → sửa → test hành vi mới → cổng regression.

**Giao cho agent code:** [`codex-handoff.md`](./codex-handoff.md) (quy tắc chung, thứ tự 10 PR, prompt từng PR, điểm dừng).

## Quyết định áp dụng

| Chủ đề | Quyết định |
| --- | --- |
| Path mastery | Một path mỗi user, tạo theo `sort_order` từ **một** lần gọi `topic-sequence` (kèm KP; chỉ topic có bài và có đề); không LLM, không lọc band, không cần goal. Tutor, `/status`, `/progress` dùng cùng path. `GET /topics` gọi refresh để gộp topic và KP mới |
| Thứ tự học | Tách khỏi path: topic có ≥ 1 bài và ≥ 1 mã đề, theo `sort_order`, lưu ở `topic_progress.sequence_order`. **Chỉ lưu `passed_at`**; trạng thái suy ra khi đọc: topic đầu chưa PASSED là IN_PROGRESS, còn lại LOCKED (chốt ở Validation Session 1). Tool reorder của tutor không ảnh hưởng. Cổng bài học quyết định luồng học; `next_objective` chỉ là gợi ý cho tutor |
| Mastery | `compute_mastery` DeepTutor giữ nguyên. Bài học và bài ôn ghi qua `app/learning/practice_evidence.py` với `source` là `lesson_exercise`/`review_set`, reference tất định theo `request_id`. **Chỉ lần nộp đầu của mỗi khối ghi bằng chứng**; làm lại chỉ để qua khối (chốt ở Validation Session 1) |
| Hiển thị MVP | App chỉ hiển thị trạng thái topic và bài. `/status`, `/progress` (ngưỡng "đã nắm" 0.9), `/practice/*` là của tutor, app MVP không dùng |
| Transaction | Mọi thay đổi của học viên (nộp bài, bài ôn, giao mã đề, phần bài học của consumer) chạy trong một path transaction, một connection. Consumer làm phần bài học trước khi ghi bản ghi idempotency |
| Cổng | Một hàm `authorize_lesson_access` cho GET bài, nộp bài, complete. Đề cuối đòi topic IN_PROGRESS, mọi bài xong, không bài ôn chờ |
| Phản hồi | Câu hỏi trả cho học viên theo danh sách trường cho phép. Chưa đạt: chỉ đúng/sai. Đạt: thêm đáp án và giải thích. Đề cuối: đúng/sai từng câu; lời giải khi ≥ 70% |
| Làm lại khối | Nộp lại cả khối, `requestId` mới, cùng câu |
| Đánh giá lại path | Ba lúc: bài vừa xong; kết quả TOPIC_GATE; kết quả MOCK, OFFICIAL_PRACTICE hoặc QUIZ. Chỉ chèn bài ôn (xếp lại topic hoãn). Chèn khi: mastery < ngưỡng, có câu sai trong kết quả vừa xét, bài dạy KP đã xong (**tính cả bài vừa xong**), và content có gói luyện cho KP (`hasPracticeSet`, lưu ở snapshot KP; chốt ở Validation Session 3). **Luyện thêm theo mastery** (chốt 2026-09-29): học viên yếu ở KP của bài vừa học được giao ngay gói câu mới trước khi mở bài kế; học viên làm tốt thì đi thẳng |
| Ngưỡng bài ôn | Setting `AI_LEARNING_REVIEW_MASTERY_THRESHOLD`, mặc định 0.6 |
| Bài ôn | Lý thuyết bài dạy KP + một gói `PRACTICE_SET` (≥ 3 câu) chưa làm. Trượt thì giao gói khác; hết gói thì lấy gói lâu nhất. **Trượt set thứ 3 → `SKIPPED`, cho học tiếp** (chốt ở Validation Session 1). Chỉ chủ sở hữu truy cập; nộp chỉ vào set đang mở |
| Đề cuối | Nhiều gói `TOPIC_TEST` mỗi topic (`content_packages.topic_id`). `POST /topics/{id}/test-assignments` idempotent. Mỗi lần giao chỉ dùng một lần; làm lại thì sang mã khác theo `package_id`. PASSED chỉ đi một chiều |
| Assessment | Tự quyết `attemptType` từ loại gói; từ chối `PRACTICE_SET`; `expiresAt` lấy từ luật của gói; không migration mới (dùng `answer_snapshot`, `item_results.max_score`); không gọi user-service lấy goal; event có `package_version_id`, `learning_goal_id = null` |
| Consumer | Không gọi HTTP: topic lấy từ lần giao mã đề, bài dạy KP lấy từ `lesson_progress.knowledge_point_ids` |
| Placement | Không tạo path, không ghi mastery, bỏ test-out |
| Nội dung | Nạp bằng seed; không làm API soạn nội dung. Học viên không đọc được danh sách gói hay câu hỏi. Nội dung seed đầy đủ ở [`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md); KP mới `kind = STRATEGY` |
| Band | Bỏ band của KP: content V8 xóa cột; API KP bỏ 4 trường band; ai-learning V10 xóa `mastery_path_knowledge_point_bands`, tool tutor bỏ band. Band của topic giữ để hiển thị (chốt 2026-09-29, sau red-team) |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Contract và đặc tả](./phase-01-contract-va-dac-ta.md) | Completed (duyệt 2026-10-01) |
| 2 | [Chặn lộ đáp án](./phase-02-chan-lo-dap-an.md) | Completed (`34335aa`, merge PR #23) |
| 3 | [Content: bài học, gói, endpoint nội bộ, seed](./phase-03-content-bai-hoc-va-goi.md) | Completed (merge PR #24, `d7ea05f`) |
| 4 | [Assessment: tự chấm, lấy đề từ content, event](./phase-04-assessment-tu-cham-va-ma-de.md) | Completed (nhánh `feat/assessment-auto-grading`; assessment 102 test, content 138 test pass) |
| 5 | [AI Learning: path theo user](./phase-05-ai-learning-path-theo-user.md) | Superseded (phần a merge PR #25; còn lại thay bởi `261001-1228`) |
| 6 | [AI Learning: API bài học, bài ôn, giao mã đề](./phase-06-ai-learning-api-bai-hoc-va-bai-on.md) | Superseded (`261001-1228` phase 2–3) |
| 7 | [AI Learning: consumer kết quả đề](./phase-07-ai-learning-consumer-ket-qua-de.md) | Superseded (`261001-1228` phase 4) |
| 8 | [Tài liệu và dọn dẹp](./phase-08-tai-lieu-va-don-dep.md) | Completed (2026-10-02; 693 test, 0 fail/error/skip; live E2E 7/7) |

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
- **Chia lại service** (tách `library-service`, giải thể learning-support; `.sdd/specs/SERVICE_ARCHITECTURE_V3.md`): thuộc MVP
  nhưng làm ở plan riêng (xem plan tổng `260930-2057`), không làm trong plan này.
- Vai trò tutor trong bài học; nhắc học.
- Cập nhật demo tương tác.

## Tiêu chí nghiệm thu

- Học viên mới, không có goal: `GET /api/ai-learning/topics` tạo path, không gọi LLM; thứ tự học = [DEMO_READING, TFNG_SKILLS]; topic đầu IN_PROGRESS.
- `GET /lessons/L2` và nộp bài L2 khi L1 chưa xong: đều 403 `LESSON_LOCKED`.
- Nộp khối 2/3: không có `correctAnswer`/`explanation`. 3/3: có cả hai, bài xong, bài kế mở.
- Trùng `requestId` → kết quả cũ. Hai lần nộp song song cùng khối → bằng chứng ghi một lần. Lỗi giữa chừng rồi thử lại → dữ liệu đầy đủ, không trùng.
- Mọi lần nộp sau lần đầu của một khối (sửa câu sai, hay nộp lại khối đã đạt): mastery không đổi.
- Content chèn topic mới trước topic đang học → không topic nào kẹt LOCKED.
- Kịch bản Lan (ngưỡng 0.6):
  - L1 xong (KP3 ≥ ngưỡng) → không chèn.
  - L2 xong (KP1 < ngưỡng, lần đầu sai Q5) → giao gói luyện KP1 cùng lý thuyết L2; L3 bị chặn tới khi đạt, hoặc tới khi trượt 3 set (`SKIPPED`).
  - L3, L4 đúng hết → không chèn.
  - Đề 3/4 sai Q15 → PASSED và chèn bài ôn L3 cho KP2.
- Học viên làm đúng hết L1–L4 ngay lần đầu → không bị giao gói nào (đi thẳng).
- Còn bài ôn chờ: mọi đường mở hay nộp bài khác đều 403 `REVIEW_REQUIRED`.
- Bài ôn: gói có câu đo KP2, không trùng câu bài học hay đề. Trượt → gói khác; trượt set thứ 3 → `SKIPPED`, học tiếp. Review của user khác → 404.
- Mã đề: gọi giao hai lần → cùng lần giao. Sau khi dùng → mã khác. Làm lại mã cũ trực tiếp qua assessment → không mở topic.
- Assessment: gói `TOPIC_TEST` luôn là `TOPIC_GATE`; gói `PRACTICE_SET` → 422.
- Consumer: lỗi phần bài học rồi giao lại event → PASSED đúng. Không gọi HTTP. Event không có goal được xử lý.
- API học viên không bao giờ trả `answerSpec`, hay `explanation` trước khi đạt. CUSTOMER đọc câu hỏi hoặc gói qua `/api/content` → 403. Gateway chặn `/internal/**`.
- Cùng bộ vector `answer-spec-v1` pass ở assessment (Java) và ai-learning (Python).

## Dependencies

- Không chặn plan nào. Plan kiến trúc chia service (library-service) và plan đồng bộ tài liệu cũ đã xóa (2026-09-30); việc chia service thuộc MVP, làm ở plan riêng (chốt 2026-10-01).
- **Chờ PR S1 của `261001-0205` merge trước** (đổi 2026-10-01): S1 chiếm content V7
  (xóa 5 bảng từ vựng/video), nên migration content của plan này là V8 (bảng bài học) và V9 (seed). Ai-learning giữ V10, V11.
  Validation log bên dưới ghi số cũ (V7/V8) theo thời điểm viết.
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

## Validation Log

### Session 1 — 2026-09-30
**Trigger:** `/ck:plan validate` sau scout đối chiếu plan với code (`plans/reports/scout-260930-1642-lesson-plans-vs-codebase-report.md`) và tư vấn kiến trúc về path, mastery, chấm điểm. Người dùng muốn MVP đơn giản.
**Questions asked:** 7

#### Verification Results
- Tier: Full (8 phase). Plan đã có Red Team Review; lượt này dùng scout đối chiếu 8 phase với code thay cho bước kiểm lại.
- Claims checked: ~60 | Verified: ~52 | Failed: 8 | Unverified: 0 (file do plan tạo mới không tính)
- Failures (đã sửa trong phase, không đổi quyết định cũ):
  - Phase 2 và 4 mâu thuẫn về `CreateAssessmentResultUseCaseTest` (`phase-02:67`, `phase-04:67`) → câu hỏi 5.
  - `StartAssessmentAttemptUseCase.execute` là `@Transactional` (`:32`): gọi content "ngoài transaction" không làm được trong cùng bean → tách `AttemptCreator`.
  - `GlobalExceptionHandler.java:24` map `InvalidAssessmentStateException` → 400, plan cần 409/422/503 → thêm vào Modify.
  - Phase 4 định xóa "DTO nội bộ liên quan", nhưng `QuestionKnowledgePointResponse/Result` còn được `GetQuestionDetailUseCase` dùng → chỉ xóa `KnowledgePointMappingResponse`.
  - Phase 5 sót `ActiveGoalRequired` (`main.py:19,59`), `app/clients/user_service.py`, validator `config.py:32,46`.
  - Phase 5: V10 cascade xóa cả phiên tutor và sổ luyện tập, không chỉ evidence → câu hỏi 6.
  - Phase 6: đổi index evidence là thừa (index V2 đã có `source`, partial); evidence phải đi qua aggregate (`quiz_attempts`), không INSERT thẳng vì `_sync_evidence_projection` ghi lại bảng mỗi commit.
  - Phase 7: `FormalAssessmentIngestionService` tự tạo applier riêng (`formal_assessment_ingestion.py:39`); số dòng `record_applied_result` là `:287`, không phải `:270-274`.

#### Questions & Answers
1. **[Architecture]** Bài tập trong bài học ghi bằng chứng mastery thế nào?
   - Options: Chỉ lần đầu | Giữ như plan (mọi lần nộp tới khi đạt)
   - **Answer:** Chỉ lần đầu.
   - **Rationale:** `compute_mastery` lấy 5 lần gần nhất, trọng số lần mới cao nhất; tính lần làm lại (loại trừ sau khi biết sai) làm mastery bị thổi phồng và kết quả phụ thuộc số câu của KP.
2. **[Risk]** Học viên trượt gói ôn liên tục thì sao?
   - Options: Cho qua sau 3 lần | Chặn tới khi đạt
   - **Answer:** Cho qua sau 3 lần.
   - **Rationale:** Tránh kẹt vĩnh viễn; mastery vẫn thấp nên đề cuối bắt lại.
3. **[Architecture]** Trạng thái topic lưu thế nào?
   - Options: Chỉ lưu PASSED, suy ra đang học | Giữ 3 trạng thái lưu
   - **Answer:** Chỉ lưu PASSED.
   - **Rationale:** Ít code (consumer bỏ bước mở topic kế), không kẹt khi content thêm topic.
4. **[Scope]** MVP hiển thị tiến độ gì cho học viên?
   - Options: Chỉ topic và bài | Hiện cả mastery
   - **Answer:** Chỉ topic và bài.
   - **Rationale:** Tránh ba ngưỡng khác nhau (70%, 0.6, 0.9) trên cùng màn hình.
5. **[Assumption]** Phase 2 bỏ `execute` của học viên nhưng tiêu chí ghi "không test cũ nào bị xóa". Sửa thế nào?
   - Options: Xóa test hành vi bị bỏ | Giữ execute, chỉ chặn quyền
   - **Answer:** Xóa test hành vi bị bỏ.
6. **[Risk]** V10 cascade xóa phiên tutor và sổ luyện tập của path bị bỏ. Xử lý thế nào?
   - Options: Chấp nhận xóa | Chuyển sang path giữ lại
   - **Answer:** Chấp nhận xóa (chỉ có dữ liệu dev, có `pg_dump`).
7. **[Scope]** Listening (`260930-0851`) có tách khỏi plan chọn gói theo độ khó (`260930-0908`) không?
   - Options: Tách, hoãn 0908 | Giữ thứ tự hiện tại
   - **Answer:** Tách, hoãn 0908.
   - **Rationale:** Seed 1–2 gói mỗi KP nên luật độ khó hầu như không có tác dụng; 0908 còn hai lỗ dữ liệu.

#### Confirmed Decisions
- Bằng chứng bài học: chỉ lần nộp đầu của mỗi khối.
- Bài ôn: `SKIPPED` sau 3 set trượt (hằng số, không setting).
- `topic_progress`: không có cột `status`; `derive_topic_statuses` dùng chung.
- App MVP: chỉ trạng thái topic/bài.
- Xóa test `execute` học viên; V10 chấp nhận cascade; Listening không chờ 0908.

#### Impact on Phases
- Phase 1: contract ghi luật trạng thái topic, bằng chứng lần đầu, `reviewStatus`, hiển thị MVP.
- Phase 2: tiêu chí test.
- Phase 4: giữ test phần người chấm; tách `AttemptCreator`; mapping lỗi 409/422/503; danh sách xóa DTO.
- Phase 5: phạm vi xóa V10; `ActiveGoalRequired`, `user_service.py`, validator config.
- Phase 6: bằng chứng lần đầu; trạng thái suy ra; `SKIPPED`; bỏ đổi index; evidence qua aggregate; test tương ứng.
- Phase 7: chỉ ghi `passed_at`; một applier; sửa số dòng.
- Phase 8: `DATABASE_V5` §7.21, §7.23, §7.24; `docs/ai-learning-database.md`, `mvp-database.md`.
- Ngoài plan: `260930-0851` bỏ `blockedBy` 0908; `260930-0908` bỏ `blocks` 0851, ghi hoãn; `260930-1006` ghi tác động lên `hints_used`.

### Whole-Plan Consistency Sweep (Session 1)
- Files reread: plan.md, phase-01 … phase-08.
- Decision deltas checked: 7 (bằng chứng lần đầu, `SKIPPED`, trạng thái topic suy ra, hiển thị MVP, xóa test `execute` học viên, V10 cascade, Listening tách 0908) + 8 sửa theo bằng chứng code.
- Stale terms searched: `status LOCKED|IN_PROGRESS|PASSED` lưu cứng, "chuyển từ LOCKED", "khối chưa từng đạt", "Khối đã đạt nộp lại thì không ghi", "đổi partial unique index", `0.88`/`0.51`, `PENDING|DONE` thiếu `SKIPPED`, "mở topic kế" như bước ghi, `:270-274`, "mọi lần nộp của bài", "không test cũ nào bị xóa".
- Reconciled: 14 (plan.md 5; phase-01 1; phase-02 1; phase-04 3; phase-05 3; phase-06 10; phase-07 5; phase-08 1 — có dòng sửa nhiều chỗ).
- Còn lại có chủ đích: "đạt thì PASSED và mở topic kế" ở Overview (mô tả hành vi, đúng với trạng thái suy ra); tên thuật ngữ cũ trong `reports/` red-team (lịch sử, không sửa).
- Plan ngoài: 0851, 0908, 1006 đã ghi tác động và cần validate lại trước khi làm; chưa sửa phase của các plan đó.
- Unresolved contradictions trong 1640: 0.

### Session 2 — 2026-09-30
**Trigger:** người dùng hỏi vì sao `GET /topics` gọi Content ba lần.
**Questions asked:** 1

1. **[Architecture]** Gộp ba lần gọi (`/api/content/topics`, `/api/content/knowledge-points`, `/internal/learning-content/topic-sequence`) thành một?
   - Options: Gộp: `topic-sequence` trả kèm KP | Giữ ba lần gọi
   - **Answer:** Gộp.
   - **Rationale:** MVP chỉ ghi bằng chứng cho KP của topic học được (bài, bài ôn, Writing, đề cuối đều thuộc các topic này); hai API lấy toàn bộ topic và KP kéo dữ liệu không dùng. Content lọc được bằng một query topic + một query KP `IN`.
   - Hệ quả: path chỉ chứa topic có bài và có đề; KP ngoài lộ trình không nhận bằng chứng (MVP không có mock; placement không ghi bằng chứng). Tutor ngoài phạm vi MVP.

#### Impact on Phases
- Phase 1: contract `topic-sequence` kèm `knowledgePoints`.
- Phase 3: query plan và test seed của `topic-sequence`.
- Phase 5: dựng path từ `topic-sequence` thay `get_curriculum`.
- Phase 6: `GET /topics` gọi Content một lần.

#### Whole-Plan Consistency Sweep
- Đã tìm `get_curriculum`, `/api/content/topics`, `/api/content/knowledge-points`, `topic-sequence` trong plan.md và phase 1–8; các chỗ còn lại chỉ ở ngữ cảnh "không dùng trong MVP".
- Unresolved contradictions: 0.

### Session 3 — 2026-10-01
**Trigger:** review "plan đã đủ để giao Codex code chưa" (`plans/reports/review-261001-0116-mvp-plans-codex-readiness-report.md`).
**Questions asked:** 4 (phần của 1640)

#### Verification Results
- Seed V8 chỉ có trong trang demo claude.ai (agent code không mở được); repo chỉ có đoạn văn V6, 0 câu hỏi.
- Seed có gói luyện cho KP1–KP4, không có cho KP5: sai câu KP5 ở đề TFNG có thể chèn bài ôn không có gói nào để giao.
- `knowledge_points.kind` NOT NULL + CHECK (`V1:23`); seed KP mới chưa ghi `kind`.
- Refresh đã ghi lại snapshot chi tiết của mọi KP (`path_service.py:189-202`); `replace_knowledge_point_details` chèn từng dòng trong vòng lặp (`postgres_learning_store.py:417-427`).

#### Questions & Answers
1. **[Architecture]** KP không có gói luyện thì sao? — Options: không chèn bài ôn | bài ôn chỉ lý thuyết | bắt buộc mỗi KP có gói.
   **Answer:** không chèn bài ôn. Cách làm: `topic-sequence` trả `hasPracticeSet`, ai-learning lưu ở `mastery_path_knowledge_point_details.has_practice_set` (V10), luật đọc cột này (consumer không gọi HTTP).
2. **[Scope]** Nguồn nội dung seed? — Options: chép từ demo vào plan | agent tự soạn.
   **Answer:** chép từ demo. Đã trích bằng script ra `260930-2057-…/seed-content.md` (dùng chung cho 1640, 0737, 0812, 0851, 1006); đề TFNG (3 câu) soạn thêm.
3. **[Assumption]** `kind` của KP mới? — **Answer:** tất cả `STRATEGY`.
4. **[Process]** Giao việc cho agent code? — **Answer:** mỗi PR một prompt (phạm vi file, điểm dừng duyệt, lệnh test, biến môi trường).

#### Impact on Phases
- Phase 1: `topic-sequence` có `hasPracticeSet`; luật KP không có gói; `options = null` là câu điền.
- Phase 3: seed trỏ `seed-content.md`; `kind`; mã đề TFNG; `hasPracticeSet` dùng chung predicate với search; test seed.
- Phase 5: V10 thêm cột `has_practice_set`; `KnowledgePointDetails` thêm trường; ghi details theo lô.
- Phase 6: điều kiện thứ tư của luật chèn; review không còn gói → `SKIPPED`; test KP5 và số mastery theo bảng seed.
- Phase 7: test KP không có gói.

#### Whole-Plan Consistency Sweep
- Đã tìm "đủ cả ba", "demo tương tác", "Nội dung lấy từ", `knowledgePoints: [{id, code, name, learningType, skill, description}]` trong plan.md và phase 1–8; đã sửa hết chỗ mô tả luật hay contract cũ.
- Unresolved contradictions: 0.

### Session 4 — 2026-10-01 (duyệt contract PR 1)
**Trigger:** agent code viết xong bản nháp contract (nhánh `feat/lesson-contracts`) và nêu 3 điểm mở.
**Questions asked:** 3

#### Verification Results
- `topics` (content V1) không có cột `skill`; `DEMO_READING` có KP Reading lẫn Writing nên topic không có một skill duy nhất.
- Phase 7: consumer tiêu lần giao bằng lần nộp hoàn thành **đầu tiên** của (`user_id`, `package_version_id`) sau `assigned_at`;
  attempt sau không mở topic. Dò đáp án bằng attempt song song không qua được cổng; rủi ro còn lại là giao lại mã cũ khi hết mã
  (đã chấp nhận).
- Seed đề cuối dùng `rules = {}`; phase 4 đã ghi `expiresAt` null khi không có luật.

#### Questions & Answers
1. **[Contract]** `skill` cấp topic trong `topic-sequence`? **Answer:** bỏ; skill chỉ ở KP.
2. **[Security]** Attempt đề cuối gắn `assignmentId`? **Answer:** không; contract ghi rõ luật "lần nộp hoàn thành đầu tiên của
   version đã giao mới tính".
3. **[Scope]** Giới hạn thời gian đề cuối? **Answer:** không trong MVP; `expiresAt` = null, luật tính giờ để sau.

#### Impact
- Phase 1, 3: bỏ `skill` khỏi topic trong `topic-sequence`.
- Contract: bỏ 3 mục "Open decision"; `POST /lessons/{id}/complete` trên bài có bài tập dùng mã riêng `LESSON_HAS_EXERCISES` (409)
  thay vì dùng lại `REQUEST_CONFLICT`.
