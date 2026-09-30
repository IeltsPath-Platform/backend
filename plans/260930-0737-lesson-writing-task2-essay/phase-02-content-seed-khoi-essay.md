---
phase: 2
title: "Content: seed khối essay"
status: pending
priority: P2
dependencies: [1]
effort: "1 ngày"
---

# Phase 2: Content: seed khối essay

## Context Links

- Plan 1640 phase 3: `plans/260929-1640-lesson-learning-pipeline-mvp/phase-03-content-bai-hoc-va-goi.md` (V8 bảng bài học,
  V9 seed, `InternalLearningContentController`)
- Contract: `docs/contracts/learning-content-internal-v1.md`, `answer-spec-v1.md` (bản đã sửa ở phase 1)
- `services/content-service/src/main/java/com/group01/content/domain/vo/QuestionType.java` (đã có `ESSAY`)

## Overview

Content **không đổi schema**. Phase này gồm:
- seed một câu `ESSAY` Task 2 và KP Writing;
- gắn một khối essay vào bài cuối của topic demo;
- endpoint nội bộ trả `blockKind` để ai-learning phân biệt khối essay với khối tự chấm.

## Requirements

- Seed `V10__seed_writing_task2_demo.sql` (không sửa V1–V9), UUID prefix `21000000-…`. **Nội dung (đề, `answer_spec`, bài
  mẫu, khối TEXT) lấy đúng mục "Khối essay `L4-W2`" và bài L4 trong
  [`seed-content.md`](../260930-2057-mvp-reading-writing-listening-roadmap/seed-content.md).** Tóm tắt:
  - KP `DEMO_READING_W2_OPINION` (KP7) thuộc topic `DEMO_READING`: `kind = STRATEGY`, `learning_type = PROCEDURE`,
    `skill = WRITING`, `ACTIVE`.
  - Câu hỏi:
    - `questions`: `question_type = ESSAY`, `skill = WRITING`, `PUBLISHED`.
    - `question_versions`: `stem` là đề green roofs trong `seed-content.md`; `answer_spec` =
      `{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":6.0}`; `explanation` là bài mẫu trong `seed-content.md`
      (tự viết, không bản quyền); `options` NULL.
    - `question_knowledge_points`: câu → KP mới, `weight = 1.00`.
  - Khối essay:
    - `lesson_blocks`: bài L4 (bài cuối của `DEMO_READING`) thêm ở cuối một khối `TEXT` (nội dung trong `seed-content.md`)
      rồi một khối `EXERCISE` chứa câu essay.
    - `lesson_block_questions`: khối → version câu essay.
    - `lesson_knowledge_points`: bài đó → KP mới.
- Luật khối essay: khối `EXERCISE` hoặc chỉ gồm câu tự chấm, hoặc gồm **đúng 1** câu `ESSAY`. Trộn hai loại, 2 câu ESSAY,
  hay ESSAY thiếu `answerSpec.type = ESSAY` hoặc `passBand` hợp lệ (4.0–9.0, bước 0.5) đều sai luật.
  <!-- Updated: Validation Session 1 - chốt nơi kiểm luật: test seed + content tính blockKind, không DB trigger -->
  - Nơi kiểm: hàm thuần ở domain `LessonBlockKind.classify(questions)`. Hàm trả `EXERCISE` hoặc `ESSAY`; sai luật thì
    ném `InvalidLessonBlockException`.
  - Use case đọc bài gọi hàm này. Khối sai luật → 500 với mã `INVALID_LESSON_BLOCK` qua `GlobalExceptionHandler`
    (lỗi dữ liệu seed, không phải lỗi người dùng).
  - Test Testcontainers quét **mọi** khối `EXERCISE` trong seed: không khối nào sai luật. Content không có API soạn bài
    nên đây là chốt chặn chính.
  - ai-learning chỉ tin `blockKind`, không tự phân loại lại.
- Endpoint nội bộ lấy bài (`/internal/learning-content/...` của 1640): mỗi khối `EXERCISE` có
  `blockKind = "ESSAY" | "EXERCISE"`. Khối essay trả câu với `questionType`, `stem`, `answerSpec`, `explanation`,
  `knowledgePointIds`. Endpoint nội bộ đã có `answerSpec` nên vẫn chỉ gọi được từ nội bộ.
- Snapshot game, API content công khai, assessment: không đổi. Câu essay nằm trong khối bài học nên đã bị chặn khỏi snapshot
  game (luật của 1640).

## Architecture

- Tính `blockKind` ở use case đọc bài (một lần duyệt các câu đã load). Không query thêm, không N+1.
- Không thêm cột, không thêm enum giá trị.

## Related Code Files

- Create: `services/content-service/src/main/resources/db/migration/V10__seed_writing_task2_demo.sql`
- Create: `domain/vo/LessonBlockKind.java` (enum + `classify`), `domain/exception/InvalidLessonBlockException.java`,
  test `domain/vo/LessonBlockKindTest.java`
- Modify: use case và DTO nội bộ lấy bài của 1640 (`api/dto/internal/*`, use case get lesson), `GlobalExceptionHandler`
- Tests: test seed/migration Testcontainers của content (của 1640, thêm phần quét mọi khối), test controller nội bộ, test use
  case lấy bài

## Implementation Steps

**Tests Before:**
1. Chạy `mvn -q -pl services/content-service -am test` và lưu baseline: payload lấy bài của các khối tự chấm giữ nguyên
   (so snapshot JSON một bài không có essay).

**Tests After** (viết trước code):
2. Migration: V10 chạy sạch sau V9. Câu essay `PUBLISHED` có spec hợp lệ và KP cùng topic với bài.
3. `LessonBlockKindTest` (unit):
   - chỉ câu tự chấm → `EXERCISE`; 1 ESSAY hợp lệ → `ESSAY`;
   - ESSAY + MULTIPLE_CHOICE, 2 ESSAY, ESSAY thiếu `passBand`, `passBand` 6.3 hoặc 9.5 → `InvalidLessonBlockException`.
   Test seed (Testcontainers): quét mọi khối `EXERCISE` sau V10, không khối nào ném lỗi.
4. Endpoint nội bộ:
   - bài cuối `DEMO_READING` có khối `blockKind = ESSAY` với `answerSpec.task = TASK_2`;
   - khối cũ có `blockKind = EXERCISE`;
   - bài không có essay giữ nguyên payload, chỉ thêm `blockKind`.
5. API content công khai: CUSTOMER đọc câu essay → 403 (luật 1640 vẫn giữ).

**Implement:** `LessonBlockKind.classify` → DTO + use case (`blockKind`) + handler lỗi → V10.

**Regression Gate:**
```powershell
mvn -q -pl services/content-service -am test
```

## Success Criteria

- [ ] V10 chạy sạch; seed có 1 khối essay ở bài cuối `DEMO_READING`.
- [ ] Payload nội bộ có `blockKind`; các khối cũ không đổi gì khác.
- [ ] Không migration schema mới ở content.

## Risk Assessment

- **Seed làm vỡ test kịch bản Lan của 1640:** khối essay không tính vào hoàn thành bài (phase 4), và bài cuối vẫn xong
  bằng các khối tự chấm. Chạy lại test kịch bản sau V10.
- **Bài mẫu dính bản quyền:** tự viết, không lấy từ sách hay đề thật.

## Security Considerations

- `explanation` của câu essay chỉ đi qua endpoint nội bộ; ai-learning quyết định khi nào trả cho học viên.

## Next Steps

- Phase 4 dùng `blockKind`, `answerSpec`, `knowledgePointIds` từ payload này.
