---
title: "Gợi ý câu Reading sau lần sai đầu"
description: "Câu điền từ và câu chọn có từ 3 phương án (kể cả True/False/Not Given) trong bài tập bài học có gợi ý (hint) do người soạn viết; học viên làm sai một lần thì thấy gợi ý tới khi khối đạt. Content thêm cột question_versions.hint; ai-learning trả gợi ý, không ghi hints_used; không đổi công thức mastery; không áp dụng cho gói luyện thêm và đề cuối."
status: in-progress
priority: P2
branch: "feat/main-follow"
tags: [feature, backend, ai-learning, content, reading, tdd]
blockedBy: [261001-1228-learning-service-java, 260929-1640-lesson-learning-pipeline-mvp, 260930-0851-listening-topic-audio-lessons]
blocks: []
created: "2026-09-30T03:12:28.903Z"
createdBy: "ck:plan"
source: skill
mode: "tdd"
---

# Gợi ý câu Reading sau lần sai đầu

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java, route `/api/learning/**` (plan [`261001-1228`](../261001-1228-learning-service-java/plan.md), làm trước plan này). Phần ai-learning của plan đọc theo [bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md).

## Overview

Trong khối bài tập của bài học, học viên nộp sai thì nộp lại **cả khối, cùng câu** (luật của 1640). Plan này thêm một gợi ý ngắn
cho câu đã sai. Gợi ý chỉ chỗ cần đọc lại hoặc cách nghĩ, không nói đáp án. Ví dụ: "Đọc câu thứ hai của đoạn D. Từ cần tìm là
danh từ số nhiều chỉ người, đứng đầu câu."

Nguồn: trao đổi với người dùng ngày 2026-09-30 (chọn Reading trước, Listening sau, Writing không dùng cơ chế này). Chế độ `--tdd`.

## Quyết định

| Chủ đề | Quyết định |
| --- | --- |
| Nơi lưu | `content_db`: `question_versions.hint TEXT NULL`. Gắn với version câu, nên sửa câu thì ra version mới kèm gợi ý mới. Không dùng `explanation` (lời giải đầy đủ, lộ đáp án) |
| Dạng câu được gợi ý | Câu `FILL` (điền từ, trả lời ngắn) và câu `CHOICE` có **từ 3 phương án** trở lên, kể cả True/False/Not Given (3 lựa chọn cố định). Chỉ câu **2 phương án** không có gợi ý: sai một lần thì chỉ còn đúng một lựa chọn (người dùng đổi lại ngày 2026-09-30, sau validate) |
| Khi nào hiện | Câu đủ điều kiện, đã bị làm sai ít nhất một lần trong khối **chưa đạt**, thì hiện gợi ý ở cả response nộp khối và `GET /lessons/{id}`, kể cả khi lần sau đã làm đúng. Khối đạt thì trả lời giải như 1640, không trả gợi ý |
| Ai quyết định | ai-learning, từ các lần nộp đã lưu (`lesson_exercise_submissions.response`). Client không có cách lấy gợi ý trước |
| Nơi áp dụng | Chỉ bài tập trong bài học. Gói luyện thêm đóng sau một lần nộp và lần sau là câu khác; đề cuối là bài thi |
| Mastery | **Không ghi `hints_used`** (chốt 2026-09-30 ở lộ trình, Validation Session 3): 1640 chỉ ghi bằng chứng ở lần nộp đầu của khối, mà gợi ý chỉ mở sau lần sai đầu, nên không lần nộp nào vừa có bằng chứng vừa có gợi ý. Mastery không đổi; plan này không chạm `practice_evidence.py` |
| Kỹ năng | Code không lọc theo kỹ năng. Plan chỉ seed gợi ý cho câu Reading; câu Listening để `NULL` tới khi có quyết định (gợi ý Listening không được trích transcript) |
| Contract | `lesson-learning-v1`, `learning-content-internal-v1` thêm trường `hint` và luật dạng câu (chỉ thêm). `GET /package-versions/{id}` và game snapshot **không** trả `hint` |
| Schema | Content: một migration thêm cột và điền gợi ý cho câu bài học Reading đủ điều kiện. ai-learning: không migration. Index `(user_id, lesson_id, block_id)` của `lesson_exercise_submissions` được thêm vào spec V11 của 1640 |
| Chặn gợi ý lộ đáp án ở API admin | Đợt sau. Đợt này chỉ test seed kiểm |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Contract va content hint](./phase-01-contract-va-content-hint.md) | Completed (nhánh `feat/writing-access-content`; phần học viên của `lesson-learning-v1` chờ Codex) |
| 2 | [AI Learning tra hint](./phase-02-ai-learning-tra-hint.md) | Pending |
| 3 | [Tai lieu](./phase-03-tai-lieu.md) | Pending |

Thứ tự: `1 → 2 → 3`.

## Dependencies

- **Quy ước chung của lộ trình MVP** (`260930-2057`, chốt 2026-10-01):
  - Nội dung seed: [`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md); KP mới `kind = STRATEGY`.
  - Kiểm media: một hàm ở content domain, một mã lỗi `INVALID_MEDIA_REFERENCE`. 0812 viết hàm (nhận `https://` và `data:image/(png|jpeg|svg+xml);base64,…` cho IMAGE); 0851 mở rộng cho AUDIO (key + `CONTENT_MEDIA_BASE_URL`). DB lưu `content_assets.media_reference` (giá trị gốc: URL, data URI hoặc key); content resolve thành `mediaUrl` (URL đầy đủ) ngay trong payload nội bộ; ai-learning chỉ chuyển `mediaUrl` cho học viên, cho cả ảnh và audio.
  - Test tập key của response học viên: kiểm "không chứa key cấm" (`answerSpec`, `chartFacts`, `explanation`/`transcript`/`solution` trước khi đạt) thay vì so bằng đúng một tập key, để plan sau thêm trường không làm vỡ test plan trước.
  - Số migration cố định: content V7 (chia service S1, merge trước), V8/V9 (1640), V10 (0737), V11 (0812),
    V12 (0851), V13 (1006); ai-learning V12 (0737).
- **blockedBy `260929-1640-lesson-learning-pipeline-mvp`:** sửa endpoint nội bộ `GET /internal/learning-content/lessons/{id}` và
  seed `V9__seed_lesson_pipeline_demo.sql` (phase 3), luồng nộp khối và `GET /lessons/{id}` (phase 6), hai contract (phase 1).
  Cần index `(user_id, lesson_id, block_id)` của V11, đã ghi vào `phase-06` của 1640 lúc validate. Ngoài dòng index này, không mở lại 1640.
- **blockedBy `260930-0851-listening-topic-audio-lessons`** (thứ tự merge của lộ trình; plan cuối). Câu Listening để `hint = NULL`.
- Gợi ý seed lấy từ mục "Gợi ý câu Reading" của `seed-content.md` (trùng bảng ở phase 1).

## Tiêu chí nghiệm thu

- Nộp khối lần đầu, sai Q12 (điền từ, có gợi ý) → `results` của Q12 có `hint`; câu chưa từng sai có `hint = null`.
- Câu sai mà content không có gợi ý → `hint = null`.
- Câu trắc nghiệm 3 phương án (Q11) hoặc True/False/Not Given (QT1) sai → có `hint`.
- Câu 2 phương án (Q3) sai → `hint = null`, kể cả khi content có gợi ý.
- Khối đạt → `hint = null` ở mọi câu, có `correctAnswer` và `explanation` như 1640.
- Q12 sai lần 1, đúng lần 2, khối chưa đạt → response lần 2 và `GET /lessons/{id}` vẫn có `hint` của Q12.
- Tải lại bài trước lần nộp đầu → mọi câu `hint = null`.
- Nộp lại cùng `requestId` → response y hệt, kể cả `hint`.
- Lần sai của học viên B không mở gợi ý cho học viên A.
- Lần nộp thứ hai sau khi gợi ý Q12 đã mở → không ghi bằng chứng (luật lần nộp đầu của 1640); mastery không đổi.
- Bài ôn và đề cuối không bao giờ trả `hint`.
- Seed: mọi câu đủ điều kiện trong khối bài tập của bài Reading có `hint`; câu không đủ điều kiện có `hint = NULL`; không gợi ý nào
  chứa đáp án được chấp nhận.

## Câu hỏi mở

- Gợi ý nhiều bậc (sai 1 lần gợi ý nhẹ, sai 2 lần gợi ý rõ hơn): chưa làm; khi cần thì đổi cột sang `hints TEXT[]` bằng migration mới.

## Validation Log

### Session 1 — 2026-09-30

**Verification Results**
- Tier: Standard (3 phase; Fact Checker + Contract Verifier)
- Claims checked: 22
- Verified: 15 | Failed: 0 | Unverified: 7
- Verified: `V1__create_content_tables.sql:127-141` (`question_versions`, chưa có `hint`); constructor `QuestionVersion` 13 tham số;
  `AddQuestionVersionUseCase.java:33`, `QuestionVersionJpaEntity.java:50`, `QuestionPersistenceMapper.java:91,125`,
  `QuestionController.java:71`; `AddQuestionVersionUseCaseTest`, `QuestionControllerTest` có sẵn; `GameContentSnapshotResponse` trả
  `explanation`; `mastery_learning_evidence.hints_used INTEGER NOT NULL DEFAULT 0` (`V2:13`); `LearningEvidence.hints_used`
  (`models.py:168`); store ghi cột (`postgres_learning_store.py:246-260`); `app/mastery` không đọc `hints_used`; dòng 30-42, 66-69,
  108-114, 175 của `phase-06` 1640; answer-spec-v1 `CHOICE`/`FILL` (`phase-01` 1640); `DATABASE_V5` §5.5, §7.5, §7.23 tồn tại.
- Unverified (file do 1640 tạo, chưa có): `GetLessonContentUseCase`, `app/lessons/*`, `app/learning/practice_evidence.py`,
  `app/api/dto/lessons.py`, `test_lesson_api.py`, `test_practice_evidence.py`, hai contract mới. Kiểm lại ở bước "Tests Before".

**Câu hỏi và quyết định**
1. Luật mở gợi ý: response nộp khối (câu vừa sai) lệch với `GET` và `hints_used` (câu từng sai). → **Đã mở thì giữ tới khi khối đạt**, cả ba chỗ.
2. V11 của 1640 thiếu index tra theo `(user_id, lesson_id)`. → **Thêm vào V11 của 1640** (sửa `phase-06` và `DATABASE_V5` §7.23); bỏ migration có điều kiện.
3. Trắc nghiệm ít phương án. → **Không gợi ý cho câu ≤ 3 phương án** và True/False/Not Given. Người dùng chấp nhận seed hiện tại chỉ còn Q12. *(Đổi ở Session 2: từ 3 phương án vẫn có gợi ý.)*
4. API admin chặn gợi ý lộ đáp án. → **Đợt sau.**
- Tự chốt (mặc định thông thường): `hint` gửi lên chỉ có khoảng trắng thì lưu `NULL`.

**Lan truyền:** `plan.md` (quyết định, phụ thuộc, tiêu chí, câu hỏi mở); `phase-01` (luật dạng câu, bảng gợi ý còn Q12, test seed,
blank → NULL); `phase-02` (luật mở, lọc dạng câu, bỏ migration index); `phase-03` (bỏ ghi chú index, thêm luật dạng câu).
Ngoài plan: `plans/260929-1640-lesson-learning-pipeline-mvp/phase-06-ai-learning-api-bai-hoc-va-bai-on.md` (index V11),
`.sdd/database/DATABASE_V5.md` §7.23 (index).

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01-contract-va-content-hint.md, phase-02-ai-learning-tra-hint-va-hints-used.md, phase-03-tai-lieu.md
- Decision deltas checked: 5 (luật mở, index sang V11, dạng câu đủ điều kiện, chặn lộ ở API đợt sau, hint rỗng → NULL)
- Reconciled stale references: 7 (response chỉ câu vừa sai; migration index có điều kiện ở phase 2 và 3; seed 8 câu; test "mọi câu có hint"; tiêu chí "câu đúng có hint = null"; bài ôn "luôn null"; câu hỏi mở về API admin)
- Unresolved contradictions: 0

### Session 2 — 2026-09-30 (người dùng đổi quyết định 3)

- Quyết định mới: câu chọn **3 phương án vẫn có gợi ý**. Ngưỡng hạ từ ≥ 4 xuống ≥ 3 phương án; True/False/Not Given tính là 3
  (`options` rỗng) nên cũng có gợi ý. Chỉ câu 2 phương án không có.
- Hệ quả: 7/8 câu bài học Reading của seed có gợi ý (Q1, Q4, Q5, Q11, Q12, Q13, QT1); chỉ Q3 (2 phương án) không có. Bỏ câu hỏi mở
  "seed chỉ có Q12".
- Lan truyền: `plan.md` (mô tả, quyết định, tiêu chí, câu hỏi mở); `phase-01` (luật đủ điều kiện, migration điền 7 câu, bảng gợi ý,
  test seed); `phase-02` (`hint_eligible`, test); `phase-03` (câu chữ tài liệu).
- Whole-Plan Consistency Sweep: đã tìm "4 phương án", "≤ 3", "chỉ Q12" trong mọi file plan và sửa hết. Unresolved contradictions: 0.

### Session 3 — 2026-10-01
**Trigger:** roadmap `260930-2057` phase 2 (validate lại theo 1640 sau Validation Session 1 và 3).
**Questions asked:** 0 (quyết định bỏ `hints_used` đã chốt ở Validation Session 1 của lộ trình)

#### Verification Results
- 1640 chỉ ghi bằng chứng ở lần nộp đầu của khối; gợi ý chỉ mở sau lần sai đầu → `hints_used = 1` không bao giờ được ghi.
- 1640 nộp lại cả khối, cùng câu (`plan.md` 1640, "Làm lại khối"); câu "làm lại đúng các câu đó" sai.
- `wrong_kps` của 1640 chỉ đọc lần nộp đầu; tập câu đã mở gợi ý đọc mọi lần nộp → query riêng, không dùng lại.
- Seed 1640 ghi TFNG với đủ 3 `options` (`seed-content.md`), nên nhánh "`options` rỗng" của `hint_eligible` chỉ còn là phòng thủ.
- Bảng gợi ý ở phase 1 trùng mục gợi ý của `seed-content.md`.

#### Impact on Phases
- Phase 2: đổi tên file thành `phase-02-ai-learning-tra-hint.md`; bỏ bước ghi `hints_used`, mục Evidence, test `test_practice_evidence.py`;
  query tập câu đã mở là query riêng.
- Phase 3: bỏ ghi chú `hints_used` ở §0 và §7.5.

#### Whole-Plan Consistency Sweep
- Đã tìm `hints_used`, "đúng các câu đó", `phase-02-ai-learning-tra-hint-va-hints-used`, "Nếu 1640 đã có hàm" trong plan.md và phase 1–3.
- Unresolved contradictions: 0.
