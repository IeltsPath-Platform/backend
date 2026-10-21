---
title: "Writing: KP con theo tiêu chí và đánh giá từng KP có dẫn chứng"
description: "Tiêu chí CC có KP con; lần gọi LLM thứ hai đánh giá PASS/FAIL/NOT_ASSESSED từng KP con kèm đoạn trích có thật; evidence, study point và UI theo KP con."
status: in_progress
priority: P2
branch: "fix/learn-writing-skill"
tags: [writing, mastery, llm, tdd, codex-ready]
blockedBy: [261021-0103-writing-criterion-kp-evidence, 261021-1415-essay-study-points]
blocks: []
created: "2026-10-21T13:25:31.334Z"
createdBy: "ck:plan"
source: skill
---

# Writing: KP con theo tiêu chí và đánh giá từng KP có dẫn chứng

> **Cập nhật 2026-10-22 — triển khai đã đi theo thiết kế khác.** Code thực tế đánh giá KP con trong **cùng một request LLM**
> với band (`judgmentVersion = kpj2`, template `essay-kp-judgment-v2.txt` nối vào template band `v1`), không còn lần gọi thứ
> hai, không có `judgment-timeout-seconds` / `judgment-skip-after-seconds`, và client LLM **không còn** retry sửa JSON hay retry
> 429/503 (JSON hoặc band sai là `INVALID_GRADE`, không thu points). Các đoạn dưới đây mô tả lần gọi thứ hai, `kpj1`, ngân sách
> 12 s / 25 s và `completeJsonQuick` là **thiết kế ban đầu, không còn đúng**; nguồn đúng là code và `AGENTS.md` mục 3.8.
> Phần dữ kiện đã kiểm chứng, mapping KP, hợp đồng JSON và luật kiểm tra judgment vẫn đúng.

## Overview

Band CC thấp hiện chỉ sinh 1 evidence sai cho KP tiêu chí và 1 study point chung. Plan này thêm KP con cho tiêu chí **CC**,
một lần gọi LLM thứ hai đánh giá từng KP con (`PASS` / `FAIL` / `NOT_ASSESSED`, bắt buộc kèm đoạn trích có thật trong bài),
kiểm tra chéo với correction, rồi ghi evidence và study point theo KP con. KP tiêu chí (`DEMO_WRITING_CRITERION_*`) giữ
nguyên cách ghi theo band. Thiết kế tổng quát cho mọi tiêu chí (bảng + code không riêng CC); đợt này chỉ seed CC vì chỉ CC
có KP con thật dùng được (xem "Dữ kiện đã kiểm chứng").

Brainstorm: [report](../../../plans/reports/brainstorm-261021-2016-writing-criterion-sub-knowledge-points-report.md). Khi lệch với
report thì **plan này đúng** (mapping KP trong report đã sai, xem dưới).

Chế độ `--tdd`: mỗi phase viết test trước, chạy thấy đỏ đúng lý do, rồi mới code.

## Phases

| Phase | Name | Repo | Status |
|-------|------|------|--------|
| 1 | [Content: bảng KP con, API rubric/error type, hợp đồng](./phase-01-content-sub-kp-mapping-and-api.md) | backend | Complete |
| 2 | [Learning: lần gọi đánh giá KP, luật kiểm tra, evidence, response, tài liệu](./phase-02-learning-kp-judgment-and-evidence.md) | backend | Complete |
| 3 | [Frontend: hiển thị đánh giá KP con và nút Luyện tập](./phase-03-frontend-sub-kp-display.md) | frontend | Complete |

Thứ tự bắt buộc 1 → 2 → 3. Phase 3 có thể bắt đầu khi phase 2 đã chốt hợp đồng JSON (mục dưới, không đổi nữa).

## Dữ kiện đã kiểm chứng (2026-10-21)

Áp toàn bộ 70 migration Content (V1–V70, mỗi file một transaction như Flyway) lên Postgres 15 tạm rồi truy vấn. Mapping KP
phải dựa trên bảng này, không dựa trên tên.

| KP | status | topic / course | Bộ luyện tự do đủ điều kiện | Dùng làm KP con? |
|----|--------|----------------|-----------------------------|------------------|
| `DEMO_WRITING_LINKING` "Từ nối và mạch lạc" | ACTIVE | `DEMO_WRITING` / IELTS_5_5 ACTIVE | 5 | **Có** (CC, cả hai task) |
| `DEMO_WRITING_TOPIC_SENTENCE` "Mở bài và câu chủ đề (Task 2)" | ACTIVE | `DEMO_WRITING` / IELTS_5_5 | 5 | **Có** (CC, Task 2) |
| `DEMO_WRITING_TREND_LANGUAGE`, `DEMO_READING_W1_CHART`, `DEMO_READING_W2_OPINION` | ACTIVE | `DEMO_WRITING` / IELTS_5_5 | 5 / 2 / 2 | Chưa (xem Follow-ups) |
| `TR_W_OPINION`, `W65_DISCUSS_BOTH_VIEWS` | ACTIVE | ACTIVE course | 0 / 0 | Chưa (không có bộ luyện) |
| `W60_*`, `W70_*` (kể cả `W60_T1_KEY_FEATURES`, `W70_LEXICAL_PRECISION`) | ACTIVE | topic `IELTS_6_0_WRITING` / `IELTS_7_0_WRITING` **INACTIVE**, course 6.0/7.0 **INACTIVE** | — | **Không**: không vào `knowledge_point_catalog` nên không có mastery |
| `G55_*`, `V55_*`, `G60_*` | **INACTIVE**, skill `ALL` | topic/course INACTIVE | — | **Không** |

Lý do: Learning chỉ tạo mastery cho KP ACTIVE của topic ACTIVE trong course ACTIVE (`JdbcLearningContentReader.topicSequence`
→ `knowledge_point_catalog`). Error type: tất cả đang `review_status = 'DRAFT'` (V48); endpoint nội bộ
`/internal/learning-content/error-types` chỉ trả `ACTIVE` + `APPROVED` (`JdbcErrorTypeRepository.findApprovedActive`), nên
trên DB mới Learning nhận **0 mã lỗi** và mọi correction của LLM bị lọc. Hệ quả cho thiết kế: luật kiểm tra chéo với
correction vẫn phải viết và test (bằng fixture), nhưng chỉ chạy thật khi chuyên gia duyệt error type. **Không tự đặt `APPROVED`.**

Truy vấn kiểm lại (chạy trên DB đã migrate): trạng thái KP/topic/course của các code trên; số gói đủ điều kiện = gói
`PRACTICE_SET` PUBLISHED, ≥ 3 câu, có câu map KP, không chứa câu dùng trong lesson/test (định nghĩa `ELIGIBLE_PRACTICE_SET`
trong `JdbcLearningContentReader`).

## Quyết định đã chốt (không đảo; muốn đổi phải hỏi người dùng)

1. PASS và FAIL đều phải có `excerpt` nguyên văn trong essay mới thành evidence; thiếu → `NOT_ASSESSED`; `NOT_ASSESSED` không ghi evidence.
2. KP tiêu chí `DEMO_WRITING_CRITERION_*` giữ song song, vẫn ghi theo band ≥ passBand.
3. KP con đợt này (3 dòng mapping):
   - TASK_1 / CC → `DEMO_WRITING_LINKING` (sort 1)
   - TASK_2 / CC → `DEMO_WRITING_TOPIC_SENTENCE` (sort 1), `DEMO_WRITING_LINKING` (sort 2)
   Không tạo KP mới, không viết bộ luyện mới, không dùng KP của course INACTIVE.
4. Phạm vi: Learning backend + UI study points. Grader Assessment ngoài phạm vi.
5. Lần gọi LLM thứ hai riêng, lỗi thì bỏ qua; prompt chấm band (`tplv1`), `promptVersion`, `ai_prompt_activations` không đổi.
6. Ngân sách thời gian: internal JWT sống 60 s và được dùng cả cho bước trừ điểm (xem `WritingSettings`). Lần 2 timeout
   `judgment-timeout-seconds = 12`, và **không gọi** nếu đã quá `judgment-skip-after-seconds = 25` kể từ lúc bắt đầu
   `EssayGrader.grade` (trễ nhất kết thúc sau 37 s). Cập nhật so với 35 s đã hỏi ở validation: 35 + 12 = 47 s cộng thời gian
   trước/sau chấm và trừ điểm sát giới hạn 60 s; 25 s giữ nguyên ý định "luôn dưới 60 s".
7. `LINKER_MISUSE.knowledge_point_id` đổi sang `DEMO_WRITING_LINKING` (đang trỏ `W60_PARAGRAPH_COHESION` thuộc topic INACTIVE).

## Hợp đồng JSON (chốt; phase 2 tạo, phase 3 dùng)

Trong `criteria[]` của response essay `GRADED` (luôn có mảng, rỗng với bài cũ hoặc khi không có KP con):

```json
{"code":"CC","band":5.0,"strengths":[],"improvements":["Liên kết đoạn còn máy móc."],
 "knowledgePointJudgments":[
   {"knowledgePointId":"…","code":"DEMO_WRITING_LINKING","name":"Từ nối và mạch lạc","judgment":"FAIL",
    "excerpt":"Moreover, it is bad.","reason":"Moreover dùng sai chức năng tương phản."},
   {"knowledgePointId":"…","code":"DEMO_WRITING_TOPIC_SENTENCE","name":"Mở bài và câu chủ đề (Task 2)","judgment":"NOT_ASSESSED"}]}
```

`judgment` ∈ `PASS` | `FAIL` | `NOT_ASSESSED`. `excerpt`/`reason` bị bỏ khỏi JSON khi null (chỉ `PASS`/`FAIL` hợp lệ mới có `excerpt`).

Trong `studyPoints[]`, kind mới `KNOWLEDGE_POINT` (chỉ cho judgment `FAIL`):

```json
{"kind":"KNOWLEDGE_POINT","criterionCode":"CC","band":5.0,"knowledgePointId":"…","code":"DEMO_WRITING_LINKING",
 "name":"Từ nối và mạch lạc","title":"Từ nối và mạch lạc","reason":"Moreover dùng sai chức năng tương phản.",
 "excerpt":"Moreover, it is bad.","practiceKnowledgePointId":"…"}
```

`practiceKnowledgePointId` = KP con nếu Content báo nó có bộ luyện, nếu không là KP của tiêu chí cha, nếu không có thì bỏ.
Trường mới của `CRITERION`/`AI` study point: `excerpt` và `practiceKnowledgePointId` luôn null (bị bỏ khỏi JSON).
Thứ tự `studyPoints`: mỗi `CRITERION` theo thứ tự hiện có, ngay sau nó là các `KNOWLEDGE_POINT` cùng `criterionCode`;
rồi `KNOWLEDGE_POINT` của tiêu chí không có `CRITERION` point (theo thứ tự tiêu chí của bài chấm); cuối cùng các `AI`.

## Hướng dẫn cho Codex (đọc trước khi làm)

- Đọc `backend/AGENTS.md` (quy tắc bắt buộc: layer, không log essay/prompt/output LLM, không sửa migration cũ, test không gọi
  LLM thật, không N+1) và `backend/CLAUDE.md` mục 3-4 (lệnh, Docker).
- Hai plan `blockedBy` còn `in-progress` chỉ vì các gate kiểm chứng (xem "Baseline đã biết"); code và migration V70 của chúng
  **đã commit** (backend `ae44b87d`, frontend `e273d55`). Trước khi bắt đầu: `git status`, `git log -5`, xác nhận
  `V70__writing_criterion_practice_sets.sql` tồn tại và `V71` còn trống. Nếu `V71` đã tồn tại với nội dung khác, dùng số kế
  tiếp và sửa mọi chỗ nhắc "V71" trong plan này cho khớp. Nếu các file được nhắc dưới đây không tồn tại hoặc khác hẳn mô tả
  → **dừng và báo**, đừng đoán.
- Mỗi phase: (1) chạy baseline của module, ghi lại test đang đỏ trước khi sửa; (2) viết test mới, chạy, thấy đỏ đúng lý do;
  (3) code; (4) chạy lại toàn module. Lỗi nền có sẵn: ghi vào báo cáo, **không** sửa/vô hiệu test không liên quan.
- Testcontainers tự bỏ qua khi không có Docker (`disabledWithoutDocker = true`): ghi rõ "chưa chạy" thay vì coi là xanh.
  Kiểm `docker ps` trước.
- Không commit, không push, không sửa file ngoài danh sách phase, trừ khi người dùng yêu cầu. Không đưa mã plan/phase/số
  phase vào comment, tên test, tên migration hay commit message.
- Dừng và hỏi người dùng (không tự quyết) nếu: cần đổi quyết định ở trên; cần đổi `tplv1`/activation/Assessment; thấy cần
  KP hoặc bộ luyện mới; test nền đỏ khiến không thể phân biệt lỗi mới; thời hạn token nội bộ/gateway ngắn hơn 60 s.
- Báo cáo cuối mỗi phase theo mẫu: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED`, danh sách file đã sửa, lệnh test đã chạy +
  kết quả thật, test chưa chạy và lý do, câu hỏi còn mở.

## Prompt giao việc cho Codex (dán nguyên văn, mỗi phase một lần)

Dùng đường dẫn tuyệt đối của máy bạn cho `<PLAN_DIR>` (thư mục chứa file này). Chạy phase sau khi phase trước báo `DONE`.

```text
Làm phase 1 của plan <PLAN_DIR>\plan.md, chi tiết ở <PLAN_DIR>\phase-01-content-sub-kp-mapping-and-api.md.
Đọc plan.md (mục "Hướng dẫn cho Codex", "Dữ kiện đã kiểm chứng", "Quyết định đã chốt") và backend/AGENTS.md trước.
Làm đúng thứ tự TDD trong phase: baseline test, viết test đỏ, code, chạy lại. Chỉ sửa các file liệt kê trong phase.
Không commit, không sửa migration cũ, không đổi quyết định đã chốt. Gặp điều kiện "dừng và hỏi" thì dừng và báo.
Kết thúc bằng báo cáo: Status, file đã sửa, lệnh test đã chạy và kết quả thật, test chưa chạy và lý do, câu hỏi còn mở.
```

```text
Làm phase 2 của plan <PLAN_DIR>\plan.md, chi tiết ở <PLAN_DIR>\phase-02-learning-kp-judgment-and-evidence.md.
(Điều kiện: phase 1 đã DONE và migration V71 tồn tại.) Quy tắc và báo cáo như phase 1.
Phần tài liệu ở cuối phase chỉ ghi điều đã có trong code; chạy thêm `mvn -q -pl services/assessment-service -am test` nếu phase 1 chưa chạy.
```

```text
Làm phase 3 của plan <PLAN_DIR>\plan.md, chi tiết ở <PLAN_DIR>\phase-03-frontend-sub-kp-display.md. Repo là thư mục frontend/.
(Điều kiện: phase 2 đã DONE; dùng đúng "Hợp đồng JSON" trong plan.md.) Ghi test nền đang đỏ trước khi sửa và không sửa test không liên quan.
Quy tắc và báo cáo như phase 1.
```

## Baseline đã biết (kiểm 2026-10-21 trên HEAD; không sửa, không che)

Ba test sau **đã lỗi trước khi** có công việc Writing (chạy lại trên commit trước đó: backend `6eafc387`, frontend `bf37041`, cùng lỗi, cùng dòng). Chúng không thuộc plan này; ghi vào báo cáo là "lỗi nền", đừng sửa hay vô hiệu hóa.

| Module | Test | Lỗi |
|--------|------|-----|
| learning-service | `ReviewBasketIntegrationTest.aWrongAnswerInTheMainFlowUpgradesTheOptionalReviewOfThatPointToBlocking` | `Expected LearningGateException to be thrown, but nothing was thrown` (dòng 273) |
| learning-service | `StudyContextTest.lessonUsesExistingGateAndTrustedCourse` | `NullPointerException`: `LessonAccess.authorizeToRead` trả null (dòng 30) |
| frontend | `src/features/auth/pages/AuthPage.test.tsx` "marks a successful login redirect…" | `useToast must be used inside ToastProvider` |

Mọi test khác ở các module Content, Learning, Assessment và frontend phải xanh trước khi bắt đầu; nếu thấy thêm test đỏ ngoài ba test này, **dừng và báo** trước khi sửa gì.

## Kiểm chứng cuối (checklist; làm sau phase 3)

- [ ] `mvn -q -pl services/content-service -am test` và `mvn -q -pl services/learning-service -am test` xanh (hoặc lỗi nền đã ghi).
- [ ] `mvn -q -pl services/assessment-service -am test` xanh: Assessment đọc cùng endpoint rubric nội bộ, thêm trường không được làm vỡ nó.
- [ ] `mvn -q compile -DskipTests` toàn reactor.
- [ ] Frontend: `npx vitest run` các file đã sửa, `npm run typecheck`, `npm run lint`.
- [ ] Mỗi mục "Tiêu chí nghiệm thu" dưới đây có test hoặc ghi rõ chưa kiểm.
- [ ] Đo thời gian: với `LlmClient` giả trễ 20 s cho lần 1 thì lần 2 vẫn được gọi; trễ 26 s thì không gọi (test bằng Clock giả, không sleep thật).
- [ ] Kiểm thủ công nếu có LLM cấu hình (`LEARNING_LLM_*`): 1 essay Task 2 có từ nối sai → có `KNOWLEDGE_POINT` cho `DEMO_WRITING_LINKING`. Nếu không chạy được, ghi "chưa kiểm thủ công".
- [ ] `docker compose config --quiet` không bị ảnh hưởng (không đổi compose).

## Tiêu chí nghiệm thu

1. Essay Task 2, LLM trả `FAIL` cho `DEMO_WRITING_LINKING` kèm excerpt có thật → có đúng 1 dòng `kp_evidence` correct=false cho KP đó
   (nguồn `lesson_writing` hoặc `practice_set`), `studyPoints` có `KNOWLEDGE_POINT` kèm excerpt, `practiceKnowledgePointId` = id KP đó.
2. `PASS` không có excerpt, hoặc excerpt không có nguyên văn trong essay → `NOT_ASSESSED`, không có dòng evidence.
3. KP con không được LLM nhắc → `NOT_ASSESSED`, không evidence, mastery không đổi.
4. Có correction hợp lệ mà error type của nó trỏ tới KP con: LLM nói `PASS` → hạ `NOT_ASSESSED`; LLM nói `NOT_ASSESSED` hoặc im lặng → `FAIL` kèm excerpt của correction (chỉ kiểm bằng fixture error type APPROVED).
5. Lần gọi 2 lỗi / hết hạn / JSON hỏng / `completeJsonQuick` trả null / đã quá 25 s → bài vẫn `GRADED`, band và `promptVersion` y như không có tính năng, chỉ còn judgment suy từ correction.
6. KP tiêu chí vẫn nhận evidence theo band; mọi test hiện có của `WritingEvidenceRuleTest`, `EssayGraderTest`, `LessonWritingIntegrationTest`, `PracticeEssayIntegrationTest` vẫn xanh.
7. Grade cũ trong DB đọc lại không lỗi: `knowledgePointJudgments = []`, không study point `KNOWLEDGE_POINT`.
8. KP con không có evidence `NOT_ASSESSED` không bị đánh dấu yếu ở UI: nhãn trung tính "Chưa đủ bằng chứng".
9. Không đổi `ai_prompt_activations`, rubric `tplv1`, grader Assessment.

## Follow-ups (ngoài phạm vi, không làm trong plan này)

- KP con cho TA/TR/LR/GRA: cần KP ACTIVE trong course ACTIVE **và** có bộ luyện. Hiện có sẵn `DEMO_READING_W1_CHART` (TA), `DEMO_READING_W2_OPINION` (TR), `DEMO_WRITING_TREND_LANGUAGE` (LR Task 1); chưa có cho GRA. Thêm chỉ cần migration dữ liệu vào bảng V71.
- KP "Từ tham chiếu" + 3 bộ luyện (cần giám khảo IELTS duyệt nội dung).
- Chuyên gia duyệt error type (`review_status = APPROVED`) để kiểm tra chéo chạy thật.
- Gold set do giám khảo chấm để đo độ chính xác của `kpj1`.
- Trang kết quả standalone chưa render study points.

## Dependencies

- **blockedBy** `261021-0103-writing-criterion-kp-evidence` và `261021-1415-essay-study-points` (cùng sửa `EssayGrader`,
  `WritingEvidenceRule`, `WritingGrade`, `EssayStudyPoints.tsx`). Code của hai plan đã commit; phần còn mở chỉ là gate kiểm
  chứng và kiểm thủ công, không chặn việc bắt đầu plan này.

## Validation Log

### Session 1 — 2026-10-21

Verification (Full tier, ~25 claims): 21 verified, 2 failed, 2 unverified.
- Failed: 5 KP V55 `INACTIVE`/skill `ALL`; error type seed đều `DRAFT`.
- Quyết định: chỉ dùng KP ACTIVE; giữ chính sách DRAFT; lần 2 timeout 12 s; mã KP mới `WRITING_CC_*` (sau đó bỏ vì không còn tạo KP mới).

### Session 2 — 2026-10-21 (làm plan sẵn cho Codex)

Kiểm lại bằng cách áp thật migration V1–V70 lên Postgres tạm. Hai điều Session 1 chưa bắt được:
- `W60_*`, `W70_*`, `TR_W_OPINION` mapping của Session 1 vẫn sai: `W60_*`/`W70_*` thuộc topic + course **INACTIVE** nên cũng không có mastery (chỉ `TR_W_OPINION`, `W65_DISCUSS_BOTH_VIEWS` nằm trong catalog nhưng không có bộ luyện).
- Course 5.5 đã có sẵn `DEMO_WRITING_LINKING` và `DEMO_WRITING_TOPIC_SENTENCE` với 5 bộ luyện mỗi KP, nên **không cần tạo KP mới hay viết bộ luyện**.
Thay đổi: gộp 7 phase còn 3; bỏ KP mới `WRITING_CC_*` và phase bộ luyện; mapping đợt này chỉ CC (3 dòng); `judgment-skip-after-seconds` hạ từ 35 xuống 25 (JWT 60 s dùng chung với bước trừ điểm).
Whole-plan consistency sweep: đã đọc lại plan.md và 3 phase; không còn tham chiếu `G55_*`, `V55_*`, `W70_*`, `WRITING_CC_*`, "phase 4 bộ luyện" ngoài các câu giải thích lý do loại bỏ. Unresolved contradictions: 0.
