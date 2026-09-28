---
title: "Brainstorm — mô hình học bài học kiểu web luyện thi"
date: 2026-09-28 22:15
status: approved
related_plan: plans/260928-2019-architecture-doc-service-split/plan.md
sources: .sdd/specs/FEATURE_TREE_V2.md, docs/contracts/assessment-completed-v2.md, code tại commit 2079964
---

# Brainstorm — mô hình học bài học kiểu web luyện thi

> **Cập nhật 2026-09-28 23:47:** luồng học hiện hành nằm ở [main-learning-pipeline.md](../260928-2019-architecture-doc-service-split/main-learning-pipeline.md). Thay đổi so với báo cáo này: goal không bắt buộc, path gắn học viên; path ban đầu theo `sort_order`, không LLM, không lọc band; sau mỗi kết quả xếp lại chủ đề chưa học (cùng cha) và chèn bài ôn bắt buộc khi KP dưới ngưỡng (bảng `path_review_items`). Các mục "Luồng học", "Luật" dưới đây là bản trước đó.

## Vấn đề

- `FEATURE_TREE_V2.md` (2026-09-22) lấy tutor DeepTutor làm nơi học chính (`next_objective` dẫn từng bước); cây tính năng không có bài học.
- Người dùng chốt hướng mới: học theo **chủ đề → bài học** như các web luyện thi; ai-learning chạy song song (xếp thứ tự chủ đề, gợi ý ôn).
- Plan tách service mới có 3 bảng bài học, chưa có luồng học, tiến độ, bài tập.

## Quyết định đã chốt

| # | Câu hỏi | Chọn | Đã loại |
| --- | --- | --- | --- |
| 1 | Vai trò ai-learning | Song song: path xếp thứ tự chủ đề, gợi ý ôn | AI chỉ là công cụ phụ; bỏ tutor + path |
| 2 | Cấu trúc nội dung | Chủ đề → bài (topic có sẵn, dạng cây) | Khóa học → chương → bài; chỉ kho đề |
| 3 | Bài tập | Kết hợp: bài tập nhúng chấm ngay; đề cuối chủ đề qua assessment | Tất cả qua assessment; chỉ bài tập nhúng |
| 4 | Thứ tự học | Tuần tự, khóa bài sau | Tự do + % tiến độ |
| 5 | Path + khóa | Path xếp thứ tự chủ đề theo learning goal; mọi học viên học từ chủ đề đầu; trong chủ đề học tuần tự; đạt đề cuối mở chủ đề tiếp | Thứ tự cố định; chủ đề tự do |
| 6 | Hoàn thành bài | Bài tập đạt ≥ 70%; bài không có bài tập thì bấm hoàn thành | Làm hết bài tập; chỉ bấm hoàn thành |
| 7 | Loại nội dung | Lý thuyết + ảnh/audio, danh sách từ vựng, bài tập nhúng | Video bài giảng |
| 8 | Ai chấm bài tập nhúng, giữ tiến độ | ai-learning (khớp spec V2 §7: micro question không cần attempt formal) | assessment chấm tất cả |
| 9 | Premium | Theo chủ đề | Theo bài; bài học miễn phí |
| 10 | Đề cuối chủ đề | Package soạn sẵn, ngưỡng 70% | Bốc ngẫu nhiên từ ngân hàng |
| 11 | Writing/Speaking trong bài | Không, chỉ dạng tự chấm | Bài viết chấm AI |
| 12 | Bài tập nhúng tính vào mastery | Có (câu hỏi soạn sẵn đã publish); câu hỏi AI sinh vẫn không tính | Không tính |
| 13 | Cổng mở bài | Học viên mở bài qua ai-learning (kiểm khóa + premium ở server) | App gọi thẳng content (khóa chỉ ở giao diện) |
| 14 | Khi nào có path | Khi học viên bắt đầu học (cần learning goal đang hoạt động); placement không tạo path | Placement tạo path |
| 15 | Placement có bỏ qua chủ đề | Không: placement chỉ để xem trình độ, không có trạng thái `TESTED_OUT` | Chủ đề đã vững theo placement được bỏ qua |

## Luồng học (ví dụ seed thật)

Chủ đề "Demo IELTS Reading" có L1 "Tìm ý chính", L2 "Skimming", L3 "Paraphrase" và đề cuối 10 câu.

```text
Học viên có learning goal bấm bắt đầu học → tạo path: xếp thứ tự chủ đề theo mục tiêu
(placement không tạo path, không bỏ qua chủ đề; chủ đề đầu tiên mở, các chủ đề sau khóa)
   ↓
Mở chủ đề đầu: L1 mở; L2, L3, đề cuối khóa
   ↓
L1: lý thuyết → đoạn văn mẫu (asset PASSAGE) → 5 từ vựng ("thêm vào flashcard" → library)
    → bài tập 3 câu → ai-learning chấm ngay, cập nhật mastery KP
    2/3 = 67% → chưa xong, làm lại → 3/3 → L1 COMPLETED → mở L2
   ↓
L2, L3 tương tự → mở đề cuối
   ↓
Đề cuối (package TOPIC_TEST) → assessment tự chấm 8/10 = 80%
    → AssessmentCompleted.v2 (TOPIC_GATE) → ai-learning: chủ đề PASSED → mở chủ đề kế trong path
    (< 70%: gợi ý ôn các bài dạy KP làm sai, làm lại đề)
```

## Phân vai service

| Việc | Service |
| --- | --- |
| Soạn chủ đề, bài, block, câu hỏi, đề cuối; đặt `required_feature_key`, đề cuối cho chủ đề | content (`ADMIN`, `CONTENT_AUTHOR`) |
| Từ vựng, thêm vào flashcard | library (không đổi) |
| API học viên: danh sách chủ đề/bài + trạng thái, mở bài, chấm bài tập nhúng, lưu tiến độ, mở khóa, gợi ý ôn, cập nhật mastery | ai-learning |
| Làm + tự chấm đề cuối chủ đề, phát event | assessment |
| Kiểm quyền premium | access (ai-learning gọi) |

## Dữ liệu

**`content_db`** (15 → 17)
- `topics` + `required_feature_key` (NULL = miễn phí), + `test_package_id` (FK `content_packages`).
- `content_packages.package_type` + `TOPIC_TEST`, − `LESSON`.
- `lessons`: `id`, `topic_id` FK, `code` UNIQUE, `title`, `summary`, `sort_order` (UQ theo `topic_id`), `status` `DRAFT`/`PUBLISHED`/`ARCHIVED`, `created_at`, `updated_at`.
- `lesson_blocks`: `id`, `lesson_id` FK, `sort_order`, `block_type` `TEXT`/`ASSET`/`VOCABULARY`/`EXERCISE`, `text_content` (markdown, TEXT), `asset_id` FK `content_assets` NULL (ASSET: IMAGE/AUDIO/PASSAGE); CHECK theo `block_type`.
- `lesson_block_vocabulary` (mới): `block_id` FK, `vocabulary_sense_id` (id logic → library), `sort_order`.
- `lesson_block_questions` (mới): `block_id` FK, `question_version_id` FK (ghim phiên bản đã publish), `sort_order`.
- `lesson_knowledge_points`: `lesson_id`, `knowledge_point_id` FK (tìm bài dạy KP yếu).

**`ai_learning_db`** (17 → 20); tiến độ theo học viên, không theo goal
- `topic_progress`: PK(`user_id`, `topic_id`); `status` `IN_PROGRESS`/`PASSED`; `best_test_score_percent`; `passed_at`; `updated_at`.
- `lesson_progress`: PK(`user_id`, `lesson_id`); `topic_id`; `best_score_percent` NULL; `completed_at` NULL; `updated_at`.
- `lesson_exercise_submissions`: `id`; `user_id`; `lesson_id`; `block_id`; `request_id` UNIQUE; `answers` JSONB; `correct_count`; `total_count`; `score_percent`; `submitted_at`.

## API đề xuất (tên mang tính chỉ dẫn)

Học viên, qua Gateway `/api/ai-learning/**`:
- `GET /topics`: chủ đề theo thứ tự path + trạng thái (`LOCKED`/`IN_PROGRESS`/`PASSED`), cờ premium, % bài xong. Lần gọi đầu tạo path nếu chưa có (như `/status` hiện nay); chưa có learning goal thì 409.
- `GET /topics/{topicId}/lessons`: bài + trạng thái (`LOCKED`/`AVAILABLE`/`COMPLETED`) + điểm cao nhất; trạng thái đề cuối.
- `GET /lessons/{lessonId}`: nội dung bài, **không có đáp án**; 403 `LESSON_LOCKED` hoặc `PREMIUM_REQUIRED`.
- `POST /lessons/{lessonId}/exercises/{blockId}/submissions` `{requestId, answers}` → đúng/sai từng câu, giải thích (`question_versions.explanation`), điểm, bài đã hoàn thành chưa.
- `POST /lessons/{lessonId}/complete`: chỉ cho bài không có bài tập.
- `GET /review-suggestions`: bài nên ôn (KP yếu hoặc đến hạn ôn).

Nội bộ content (Gateway không route `/internal/**`):
- `GET /internal/learning-content/topics/{topicId}/lessons`, `GET /internal/learning-content/lessons/{lessonId}` (kèm `answer_spec`, KP của câu hỏi), tra `package_version_id` → chủ đề.

Khác:
- assessment: tự chấm câu khách quan theo `answer_spec` khi nộp đề `TOPIC_TEST`, tự tạo result `COMPLETED` (không cần examiner).
- `AssessmentCompleted.v2`: thêm `package_version_id` tùy chọn (bổ sung, không phá tương thích).
- access: `GET /api/access/users/{userId}/entitlement` (phải thêm guard trước khi dùng).

## Luật

- Path tạo khi học viên bắt đầu học, cần learning goal đang hoạt động (code hiện tại đã tạo path lười như vậy qua `POST /paths`, `/status`, `/progress`, mở phiên tutor). Placement không tạo path.
- Chủ đề mở: chủ đề đầu tiên trong path, hoặc chủ đề trước đó `PASSED`. Placement không mở hay bỏ qua chủ đề nào; logic test-out placement có sẵn trong ai-learning không dùng cho luồng bài học.
- Bài n mở khi bài n−1 `COMPLETED`; bài 1 mở khi chủ đề mở.
- Bài hoàn thành: một lần nộp bài tập đạt ≥ 70% (mỗi block bài tập), hoặc bấm hoàn thành nếu bài không có bài tập. Hoàn thành rồi thì giữ.
- Đề cuối mở khi mọi bài `PUBLISHED` của chủ đề `COMPLETED`; đạt khi tổng `score / max_score` của `item_results` ≥ 70%.
- Làm lại không giới hạn; `best_score_percent` giữ điểm cao nhất; `request_id` trùng trả lại kết quả cũ.
- Premium: danh sách vẫn hiện chủ đề trả phí (có dấu premium), nội dung bị chặn.
- Mastery: mỗi câu trả lời bài tập nhúng thành một interaction cho các KP gắn câu hỏi (`question_knowledge_points`, có trọng số); câu hỏi AI sinh trong tutor vẫn không tính.

## Rủi ro

| Rủi ro | Giảm thiểu |
| --- | --- |
| `grade_answer` (port DeepTutor) chấm gần đúng (câu ngắn giống ≥ 85% là đúng), không hợp IELTS và không được sửa (AGENTS §3.8) | Viết bộ chấm mới trong ai-learning theo `answer_spec` của content |
| Ba nơi chấm theo `answer_spec`: game (Java, đã có), assessment (Java, mới), ai-learning (Python, mới); constitution cấm dùng chung code nghiệp vụ | Contract `answer_spec` trong `docs/contracts/` + bộ test vector JSON cả ba bộ test cùng chạy |
| Endpoint entitlement của access đang không guard (P0 scout) | Sửa trước khi ai-learning dựa vào nó |
| Mastery trộn kết quả luyện tập với kết quả chính thức | Lưu nguồn evidence (bài tập nhúng vs formal) để có thể tách khi hiển thị |
| `FEATURE_TREE_V2.md` còn ghi tutor là nơi học chính, đã bỏ `topic_progress` | Cập nhật spec theo hướng mới |
| Lộ đáp án | API học viên bỏ `answer_spec`; chỉ endpoint nội bộ trả |
| Đổi contract event | Chỉ thêm trường tùy chọn; cần duyệt theo AGENTS §5 |

## Ngoài phạm vi vòng này

Video bài giảng trong bài học; Writing/Speaking trong bài học; đề sinh ngẫu nhiên; versioning bài học; bình luận dưới bài học; thay đổi hành vi tutor.

## Tiêu chí nghiệm thu (cho plan triển khai sau)

- Học viên chưa có gói gọi `GET /lessons/{id}` của chủ đề premium → 403 `PREMIUM_REQUIRED`.
- Gọi L2 khi L1 chưa xong → 403 `LESSON_LOCKED`.
- Nộp bài tập 2/3 → 67%, bài chưa hoàn thành; nộp 3/3 → hoàn thành, L2 thành `AVAILABLE`.
- Nộp lại cùng `requestId` → trả kết quả cũ, không thêm bản ghi.
- Đề cuối 8/10 → event → chủ đề `PASSED`, chủ đề kế tiếp mở.
- Response bài học cho học viên không có `answer_spec`.
- Cùng bộ test vector `answer_spec` pass ở game, assessment, ai-learning.

## Bước tiếp

1. Cập nhật đặc tả bảng trong plan tách service (đã làm cùng phiên).
2. Cập nhật `FEATURE_TREE` theo hướng học mới.
3. Lập plan triển khai bài học.

## Câu hỏi mở

- Thêm bài mới vào chủ đề học viên đã `PASSED`: đề xuất không khóa lại chủ đề, bài mới hiện `AVAILABLE`.
- Kết quả placement còn ghi vào mastery (dùng cho gợi ý ôn) không, hay chỉ để hiển thị trình độ? Code hiện tại có ghi.
- Path xếp thứ tự chủ đề theo mục tiêu bằng gì khi không còn placement: phạm vi band của goal + LLM ordering có sẵn, hay thứ tự `sort_order` của content?
- Tutor còn vai trò gì trong bài học (hỏi đáp về bài đang học)? Vòng này không đổi tutor.
- Dạng câu hỏi nào cần hỗ trợ trong `answer_spec`: một đáp án, nhiều đáp án, điền từ, nối, True/False/Not Given?
- Chấm điền từ: không phân biệt hoa thường, bỏ khoảng trắng thừa; có chấp nhận nhiều đáp án đúng không?
