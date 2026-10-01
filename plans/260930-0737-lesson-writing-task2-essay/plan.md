---
title: "Writing Task 2 trong bài học (khối essay, AI chấm, trừ point)"
description: "Khối essay Task 2 trong bài học, không chặn học tiếp; ai-learning chấm bằng LLM ngay trong request, chấm xong mới trừ 3 point qua access; ghi evidence mastery; content không đổi schema."
status: in-progress
priority: P2
branch: "feat/main-follow"
tags: [feature, backend, ai-learning, access, content, writing, tdd]
blockedBy: [261001-1228-learning-service-java, 260929-1640-lesson-learning-pipeline-mvp]
blocks: [260930-0812-lesson-writing-task1-academic]
created: "2026-09-30T00:56:09.778Z"
createdBy: "ck:plan"
source: skill
mode: "tdd"
---

# Writing Task 2 trong bài học (khối essay, AI chấm, trừ point)

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java, route `/api/learning/**` (plan [`261001-1228`](../261001-1228-learning-service-java/plan.md), làm trước plan này). Phần ai-learning của plan đọc theo [bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md).

## Overview

Học viên viết essay Task 2 ở khối essay trong bài học. ai-learning chấm bằng LLM theo band descriptor công khai và trả
band 4 tiêu chí kèm gợi ý sửa. **Chấm xong mới trừ 3 point** qua access-service. Kết quả ghi evidence mastery cho KP của
câu. Khối essay **không chặn** hoàn thành bài: học viên hết point vẫn học hết topic.

Nguồn: [brainstorm report](../reports/brainstorm-260930-0737-writing-speaking-practice-db-readiness-report.md).
Chế độ `--tdd`: mỗi phase theo trình tự test giữ hành vi cũ → sửa → test hành vi mới → cổng regression.

## Quyết định

| Chủ đề | Quyết định |
| --- | --- |
| Phạm vi | Chỉ Writing Task 2, khối essay trong bài học. Không làm Task 1, Writing trong đề cuối, Speaking, EXAMINER |
| Nơi chấm | learning-service (Java, gói `writing`), bảng mới `lesson_writing_submissions` (migration learning-service **V2**). Không dùng `learner_submissions`/`grading_jobs` của assessment |
| Luồng chấm | **Ngay trong request** (thay worker trong brainstorm). Lý do: internal JWT sống 60s, ai-learning không có credential riêng, nên worker nền không gọi được access. Không worker, không lease, không hoàn point |
| Thứ tự | Kiểm số dư → transaction path ngắn tạo dòng `GRADING` → LLM (ngoài transaction) → debit (idempotent) → transaction path ngắn ghi `GRADED` + evidence |
| Point | 3 point mỗi lần chấm thành công (`AI_LEARNING_WRITING_POINT_COST`). LLM lỗi thì không trừ. **Có hạn mức chấm theo ngày** (đổi 2026-10-01, mặc định 10 lần/ngày, hết → 429 `DAILY_LIMIT_REACHED`; chi tiết ở [bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md)) |
| Cổng | Dùng `authorize_lesson_access`. Khối essay không tính vào điều kiện hoàn thành bài |
| Mastery | Đúng khi `overall_band ≥ passBand` của câu; ghi `source = lesson_writing` ở **mọi lần nộp tới khi khối đạt**. Đây là ngoại lệ có lý do so với luật "chỉ lần nộp đầu" của bài tập tự chấm (1640): mỗi lần nộp là một bài viết mới, không phải đoán loại trừ. Không chèn bài ôn |
| Bài mẫu | `explanation` chỉ trả khi đã đạt, giống luật giấu đáp án |
| Access | Chỉ **debit** chuyển sang `/internal/access/points/debit`, bắt buộc `userId` = subject của token. **Refund, consume-human-grading, entitlement** giữ path cũ nhưng chỉ `ADMIN` (`@PreAuthorize`), không mở cho service (Validation Session 2). Debit song song cùng key trả lại giao dịch cũ; replay khác nội dung → 409 |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Contract va bao ve access](./phase-01-contract-va-bao-ve-access.md) | Completed (nhánh `feat/writing-access-content`; contract `lesson-writing-v1` duyệt 2026-10-01) |
| 2 | [Content seed khoi essay](./phase-02-content-seed-khoi-essay.md) | Completed (nhánh `feat/writing-access-content`) |
| 3 | [AI Learning grader va access client](./phase-03-ai-learning-grader-va-access-client.md) | Pending |
| 4 | [AI Learning API nop essay va evidence](./phase-04-ai-learning-api-nop-essay-va-evidence.md) | Pending |
| 5 | [Tai lieu](./phase-05-tai-lieu.md) | Pending |

Thứ tự: `1 → (2 ∥ 3) → 4 → 5`. Phase 2 và 3 chạy song song được sau phase 1.

**Thứ tự merge (2026-10-01):** content V10 thêm khối essay vào L4. Learning-service phải coi khối `blockKind = ESSAY` là không tính vào hoàn thành bài **trước hoặc cùng lúc** V10 lên `feat/main-follow`; nếu không, L4 không bao giờ xong. Cách nhẹ nhất: thêm luật này vào PR 3 của `261001-1228` (một dòng lọc khối), hoặc giữ nhánh này tới khi phase 4 của plan này xong.

## Dependencies

- **Quy ước chung của lộ trình MVP** (`260930-2057`, chốt 2026-10-01):
  - Nội dung seed: [`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md); KP mới `kind = STRATEGY`.
  - Kiểm media: một hàm ở content domain, một mã lỗi `INVALID_MEDIA_REFERENCE`. 0812 viết hàm (nhận `https://` và `data:image/(png|jpeg|svg+xml);base64,…` cho IMAGE); 0851 mở rộng cho AUDIO (key + `CONTENT_MEDIA_BASE_URL`). DB lưu `content_assets.media_reference` (giá trị gốc: URL, data URI hoặc key); content resolve thành `mediaUrl` (URL đầy đủ) ngay trong payload nội bộ; ai-learning chỉ chuyển `mediaUrl` cho học viên, cho cả ảnh và audio.
  - Test tập key của response học viên: kiểm "không chứa key cấm" (`answerSpec`, `chartFacts`, `explanation`/`transcript`/`solution` trước khi đạt) thay vì so bằng đúng một tập key, để plan sau thêm trường không làm vỡ test plan trước.
  - Số migration cố định: content V7 (chia service S1, merge trước), V8/V9 (1640), V10 (0737), V11 (0812),
    V12 (0851), V13 (1006); learning-service V2 (0737).
- **blockedBy `260929-1640-lesson-learning-pipeline-mvp`:** cần `V11__lesson_learning.sql`, `app/lessons/`,
  `practice_evidence.py`, `authorize_lesson_access`, endpoint `/internal/learning-content/*`, content V8/V9, Gateway
  chặn `/internal/**` (phase 2 của plan đó).
- Không đổi `common-security`, Gateway hay issuer/claim JWT.

## Tiêu chí nghiệm thu (tóm tắt, chi tiết ở phase 4)

- Nộp essay khi đủ point → 200 kèm band 4 tiêu chí; ví −3; gửi lại cùng `requestId` → cùng kết quả, ví không đổi.
- Không đủ point → 402 `INSUFFICIENT_POINTS`, không gọi LLM.
- Essay dưới 50 từ hoặc trên 1.000 từ → 422, không kiểm số dư, không gọi LLM.
- LLM lỗi hoặc trả sai schema → 503 `GRADING_UNAVAILABLE`, ví không đổi.
- Hết point giữa chừng → `PAYMENT_PENDING`, kết quả bị giữ; nạp point rồi gửi lại cùng `requestId` → trừ một lần, nhận kết quả.
- Học viên 0 point hoàn thành mọi bài; bài ôn chờ hoặc bài bị khóa vẫn chặn nộp essay.
- Evidence `lesson_writing` ghi một lần mỗi KP trước khi khối đạt; sau khi đạt thì không ghi.
- Qua Gateway không gọi được `/internal/access/**`; debit cho `userId` khác subject → 403.
- CUSTOMER gọi refund, consume-human-grading, entitlement → 403; ADMIN → như cũ.

## Câu hỏi mở

- Không còn (đã chốt ở Validation Session 1).

## Validation Log

### Session 1 — 2026-09-30
**Trigger:** `/ck:plan validate` sau khi tạo plan.
**Questions asked:** 6

#### Verification Results
- Tier: Full (5 phase; Fact Checker, Contract Verifier, Flow Tracer, Scope Auditor)
- Claims checked: 27 | Verified: 22 | Failed: 2 | Unverified: 3
- Failures:
  - Đường số dư: plan ghi `GET /api/access/points`, thật là `GET /api/access/me/points`
    (`LearnerAccessController.java:23,42`).
  - `services/access-service/README.md` không tồn tại.
- Unverified (phụ thuộc plan 1640, chưa có code): nơi kiểm luật "khối chỉ chứa câu tự chấm"; tên index evidence ở V11;
  bài cuối `DEMO_READING` ở seed V9.
- Ghi chú: consumer dùng `ConsumerSettings` (`assessment_consumer.py:28`), nên lý do "field optional để không vỡ consumer"
  là sai; đã sửa lý do. Bên gọi debit/refund ngoài test: chỉ `InternalAccessController.java:38,50`.

#### Questions & Answers
1. **[Fact]** Sửa đường kiểm số dư? — Options: dùng `/api/access/me/points` | thêm endpoint nội bộ mới.
   **Answer:** dùng `/api/access/me/points`.
2. **[Fact]** README access không tồn tại, ghi tài liệu ở đâu? — Options: contract + system-architecture | tạo README access.
   **Answer:** tạo README access.
3. **[Architecture]** Kiểm luật khối essay ở đâu? — Options: test seed + content tính `blockKind` | DB trigger/CHECK.
   **Answer:** test seed + content tính `blockKind`.
4. **[Assumption]** Giới hạn độ dài? — Options: 50–1.000 từ | chỉ giới hạn trên | chặn dưới `minWords/2`.
   **Answer:** 50–1.000 từ (và 10.000 ký tự).
5. **[Assumption]** Model LLM chấm? — Options: dùng chung `AI_LEARNING_LLM_*` | model riêng.
   **Answer:** dùng chung.
6. **[Risk]** Request đồng bộ 20–45s? — Options: chấp nhận | 202 + task nền trong API.
   **Answer:** chấp nhận đồng bộ.

#### Impact on Phases
- Phase 1: đường số dư; tạo `services/access-service/README.md`; mã `ESSAY_TOO_SHORT`; liệt kê test và bên gọi có sẵn.
- Phase 2: `LessonBlockKind.classify` ở domain, lỗi `INVALID_LESSON_BLOCK`, test quét mọi khối seed.
- Phase 3: đường số dư; `writing_min_words = 50`; dùng chung model qua `LlmSettings.model_copy`; sửa lý do field optional.
- Phase 4: kiểm `ESSAY_TOO_SHORT` trước access/LLM; test tương ứng.
- Phase 5: README access chỉ kiểm lại; kiểm mã lỗi trong contract.

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01…phase-05
- Decision deltas checked: 6 (đường số dư, README access, nơi kiểm luật khối, ngưỡng 50–1.000 từ, model dùng chung,
  request đồng bộ)
- Reconciled stale references: 17 (phase 1: 4, phase 2: 4, phase 3: 4, phase 4: 2, phase 5: 1, plan.md: 2)
- Unresolved contradictions: 0

### Session 2 — 2026-10-01
**Trigger:** roadmap `260930-2057` phase 2 (validate lại theo 1640 sau Validation Session 1 và 3).
**Questions asked:** 2 (hỏi trong phiên review sẵn sàng giao agent code, `plans/reports/review-261001-0116-mvp-plans-codex-readiness-report.md`)

#### Verification Results
- `InternalAccessController` (`@RequestMapping("/api/access")`): entitlement `:31`, debit `:36`, refund `:48`, consume `:60`; không `@PreAuthorize`. Entitlement và consume nhận `userId` trên path: ai đăng nhập cũng đọc hoặc trừ credit của người khác.
- `DebitPointsUseCase.execute` (`@Transactional` cấp class): replay chỉ tra `findByIdempotencyKey` (`:38`) rồi trả ledger, không so `userId`/`amount`/`referenceId`. Hai request song song cùng key → UNIQUE `point_ledger_entries.idempotency_key` (`V1:124`) → 500.
- `PointWalletJpaEntity` có `@Version` (`row_version`): debit song song khác key → lỗi optimistic lock (5xx); ai-learning coi là `AccessUnavailable` → `PAYMENT_PENDING`, gửi lại được. Không sửa.
- `GetUserEntitlementUseCase.execute(UUID)` không có command.
- `AuthenticatedUser` chỉ có `user_id`, `roles` (`internal_jwt.py:27-29`); router lấy bearer bằng `HTTPAuthorizationCredentials = Depends(bearer_scheme)` như `app/api/tutor.py:116`.
- Compose đặt `AI_LEARNING_LLM_REASONING_EFFORT=low` → `supports_temperature` false → grader không gửi `temperature`.
- 1640 V11 không giới hạn index evidence theo danh sách source (index V2 bao mọi source): bỏ bước điều kiện ở phase 4.

#### Questions & Answers
1. **[Security]** Refund của access? — Options: chỉ ADMIN | xóa route. **Answer:** chỉ ADMIN.
2. **[Assumption]** Temperature khi chấm? — Options: chấp nhận không gửi | grader tự đặt 0. **Answer:** chấp nhận.
- Tự chốt (mặc định rõ ràng, ghi lại để người dùng xem):
  - consume-human-grading và entitlement xử lý như refund (chỉ ADMIN), vì đang để lộ theo `userId` trên path và MVP không service nào gọi.
  - Debit song song cùng key: bắt vi phạm UNIQUE, đọc lại ledger theo key; cùng nội dung → trả ledger cũ, khác → 409.

#### Impact on Phases
- Phase 1: phạm vi access (chỉ debit sang `/internal`), luật replay, mã `ESSAY_BLOCK` vào bảng lỗi, test.
- Phase 2: seed theo `seed-content.md`, `kind = STRATEGY`, khối TEXT trước khối essay.
- Phase 3: ghi rõ không gửi temperature khi có reasoning effort.
- Phase 4: bearer lấy như `tutor.py:116`; bỏ bước index điều kiện; ghi rõ ngoại lệ bằng chứng Writing.

#### Whole-Plan Consistency Sweep
- Đã tìm "4 endpoint", "Đổi cả 4", "/api/access/points/refund", "chưa từng đạt", "Nếu V11", "temperature 0" trong plan.md và phase 1–5.
- Unresolved contradictions: 0.
