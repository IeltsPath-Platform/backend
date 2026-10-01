---
title: "Writing Task 1 Academic trong bài học (biểu đồ + chartFacts)"
description: "Mở rộng khối essay của đợt 1 sang Writing Task 1 Academic: ảnh biểu đồ gắn vào câu, grader chấm TA/CC/LR/GRA dựa trên chartFacts; không migration, không đổi luồng trừ point."
status: pending
priority: P2
branch: "feat/main-follow"
tags: [feature, backend, ai-learning, content, writing, tdd]
blockedBy: [261001-1228-learning-service-java, 260930-0737-lesson-writing-task2-essay]
blocks: [260930-0851-listening-topic-audio-lessons]
created: "2026-09-30T01:18:27.049Z"
createdBy: "ck:plan"
source: skill
mode: "tdd"
---

# Writing Task 1 Academic trong bài học (biểu đồ + chartFacts)

> **Đổi 2026-10-01:** ai-learning Python đã được thay bằng `learning-service` Java, route `/api/learning/**` (plan [`261001-1228`](../261001-1228-learning-service-java/plan.md), làm trước plan này). Phần ai-learning của plan đọc theo [bảng ánh xạ](../261001-1228-learning-service-java/python-to-java-mapping.md).

## Overview

Đợt 2 của Writing. Khối essay trong bài học nhận thêm đề Task 1 Academic:
- học viên xem ảnh biểu đồ và viết mô tả ≥ 150 từ;
- ai-learning chấm theo 4 tiêu chí Task 1 (Task Achievement, CC, LR, GRA);
- grader **không đọc ảnh**, mà đọc `chartFacts`: số liệu chính do người soạn ghi trong `answerSpec`.

Toàn bộ luồng nộp bài, trừ 3 point, evidence và cổng bài học dùng lại nguyên của đợt 1.

Nguồn:
- plan đợt 1 [260930-0737-lesson-writing-task2-essay](../260930-0737-lesson-writing-task2-essay/plan.md);
- quyết định ngày 2026-09-30 (bên dưới).

Chế độ `--tdd`.

## Quyết định

| Chủ đề | Quyết định |
| --- | --- |
| Phạm vi | Chỉ Task 1 Academic trong bài học. **Không** đưa Writing vào đề cuối (tránh tường phí ở cổng topic; chờ chốt chính sách phí). Không Speaking, không EXAMINER |
| Biểu đồ | Asset `IMAGE` gắn vào câu qua `content_asset_links.question_version_id` (đã có ở V1). Backend lưu `media_reference`, content trả `mediaUrl` (với IMAGE thì giữ nguyên giá trị); chưa có upload hay object storage |
| Seed ảnh | `media_reference` là data URI SVG (`data:image/svg+xml;base64,…`) tự vẽ, nên demo chạy được mà không cần chỗ lưu file. Nội dung thật về sau dùng URL |
| Grader | Đọc `answerSpec.chartFacts` (text ≤ 2.000 ký tự), đặt trong thẻ riêng. Không gửi ảnh cho LLM |
| Bí mật | `chartFacts` nằm trong `answerSpec` nên không bao giờ trả cho học viên (có thể chép làm bài) |
| Schema | Không migration ở content hay ai-learning. `prompt_snapshot` và `result` của `lesson_writing_submissions` là jsonb, chứa được task và tiêu chí mới |
| Point, cổng, evidence | Giữ nguyên đợt 1: 3 point, `passBand` theo câu, không chặn hoàn thành bài |

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Contract va content Task 1](./phase-01-contract-va-content-task-1.md) | Pending |
| 2 | [AI Learning grader Task 1](./phase-02-ai-learning-grader-task-1.md) | Pending |
| 3 | [Tai lieu](./phase-03-tai-lieu.md) | Pending |

Thứ tự: `1 → 2 → 3`. Phase 2 có thể viết grader song song với phase 1, sau khi contract được duyệt.

## Dependencies

- **Quy ước chung của lộ trình MVP** (`260930-2057`, chốt 2026-10-01):
  - Nội dung seed: [`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md); KP mới `kind = STRATEGY`.
  - Kiểm media: một hàm ở content domain, một mã lỗi `INVALID_MEDIA_REFERENCE`. 0812 viết hàm (nhận `https://` và `data:image/(png|jpeg|svg+xml);base64,…` cho IMAGE); 0851 mở rộng cho AUDIO (key + `CONTENT_MEDIA_BASE_URL`). DB lưu `content_assets.media_reference` (giá trị gốc: URL, data URI hoặc key); content resolve thành `mediaUrl` (URL đầy đủ) ngay trong payload nội bộ; ai-learning chỉ chuyển `mediaUrl` cho học viên, cho cả ảnh và audio.
  - Test tập key của response học viên: kiểm "không chứa key cấm" (`answerSpec`, `chartFacts`, `explanation`/`transcript`/`solution` trước khi đạt) thay vì so bằng đúng một tập key, để plan sau thêm trường không làm vỡ test plan trước.
  - Số migration cố định: content V7 (chia service S1, merge trước), V8/V9 (1640), V10 (0737), V11 (0812),
    V12 (0851), V13 (1006); ai-learning V12 (0737).
- **blockedBy `260930-0737-lesson-writing-task2-essay`:** cần `app/writing/`, bảng `lesson_writing_submissions`,
  `LessonBlockKind`, seed V10, contract `lesson-writing-v1`. Plan đó lại bị chặn bởi `260929-1640`.

## Tiêu chí nghiệm thu

- `GET /lessons/{id}` của bài có khối Task 1: khối essay có `task = TASK_1`, `minWords = 150` và `images[{mediaUrl,
  altText}]`; không có `chartFacts`, `answerSpec` hay `explanation` (chưa đạt).
- Nộp Task 1 đủ point → 200 với 4 tiêu chí `TA`, `CC`, `LR`, `GRA`; ví −3; evidence như đợt 1.
- Nộp Task 2 vẫn trả `TR`, `CC`, `LR`, `GRA` (không hồi quy).
- Khối Task 1 thiếu `chartFacts` hoặc thiếu ảnh → content báo `INVALID_LESSON_BLOCK`; `media_reference` sai scheme →
  `INVALID_MEDIA_REFERENCE`; test quét seed bắt được.
- Không test nào gọi LLM thật.

## Câu hỏi mở

- Không có.

## Validation Log

### Session 1 — 2026-10-01
**Trigger:** roadmap `260930-2057` phase 2 (validate lại theo 1640 và 0737 đã sửa).
**Questions asked:** 1 (hỏi trong phiên review sẵn sàng giao agent code, `plans/reports/review-261001-0116-mvp-plans-codex-readiness-report.md`)

#### Verification Results
- `ContentAssetRepositoryAdapter.findByQuestionVersionId` (`:61-66`) và `findBySectionId` (`:51-56`) gọi `findById` trong vòng
  lặp (N+1); `ContentAssetLinkJpaRepository` chỉ tìm theo một `sectionId`/`questionVersionId`. Cần hàm load theo tập.
- `ContentAssetController` (`/api/content/assets`) `GET /{id}` (`:28`) không `@PreAuthorize`: 1640 phase 2 chỉ khóa ghi; GET mở tới
  khi 0851 khóa. `chartFacts` nằm trong `answer_spec`, không nằm trong asset, nên khoảng hở này không lộ dữ kiện chấm.
- Nội dung seed (đề, `chartFacts`, bảng số của biểu đồ, bài mẫu) có trong `seed-content.md`, mục "Khối essay `L3-W1`".

#### Questions & Answers
1. **[Contract]** Tên trường link ảnh trả học viên? — Options: `mediaUrl` | giữ `mediaReference`. **Answer:** `mediaUrl`.
- Tự chốt: content resolve `media_reference` thành `mediaUrl` trong payload nội bộ (content sở hữu `CONTENT_MEDIA_BASE_URL` ở
  0851); ai-learning không ghép URL. Lỗi media dùng mã riêng `INVALID_MEDIA_REFERENCE` (quy ước lộ trình), không dùng
  `INVALID_LESSON_BLOCK`.

#### Impact on Phases
- Phase 1: contract `mediaUrl`, frontend chỉ render ảnh qua `<img>`; `MediaReferencePolicy` + `INVALID_MEDIA_REFERENCE`; load asset
  theo tập; seed theo `seed-content.md`, `kind = STRATEGY`, khối TEXT trước khối essay.
- Phase 2: DTO và content client dùng `mediaUrl`.
- Phase 3: tài liệu ghi mã lỗi media.

#### Whole-Plan Consistency Sweep
- Đã tìm `mediaReference`, `INVALID_LESSON_BLOCK` cho lỗi media, "id cố định với prefix mới", "tự bịa" trong plan.md và phase 1–3.
- Unresolved contradictions: 0.
