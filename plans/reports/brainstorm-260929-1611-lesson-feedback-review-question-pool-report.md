---
title: "Brainstorm: giấu đáp án, bài ôn dùng câu mới, nhiều mã đề cuối"
date: 2026-09-29 16:11
status: approved
related:
  - plans/reports/scout-260929-1316-lesson-pipeline-change-map-report.md
  - plans/260928-2019-architecture-doc-service-split/main-learning-pipeline.md
demo: https://claude.ai/artifact/Ex15FBWAZvftnrubzJK5kd (chưa cập nhật theo report này)
---

# Brainstorm: giấu đáp án, bài ôn dùng câu mới, nhiều mã đề cuối

## Vấn đề

- Người dùng yêu cầu:
  1. Sau khi nộp, chỉ báo đúng hay sai, không đưa đáp án.
  2. Khi ôn lại, không gặp lại câu cũ mà phải là câu khác, nội dung khác, cùng KP với lỗi sai.
- Vấn đề gốc: **mastery phải đo kỹ năng, không đo trí nhớ đáp án.** Thiết kế trước (bài ôn = làm lại bài tập cũ, response luôn kèm giải thích) để mastery tăng nhờ nhớ đáp án.
- Giới hạn còn lại: câu trắc nghiệm vẫn đoán loại trừ được (3 lựa chọn thì tối đa 3 lần nộp). Giấu đáp án làm chậm việc đoán, không loại bỏ được. Chỉ việc đổi câu mới đo lại được kỹ năng thật.

## Hiện trạng (scout)

- Ngân hàng câu gần như trống: seed chỉ có 1 câu (`content V4`). Chưa có API publish câu hỏi hay gắn câu vào section (chỗ chặn B4).
- Câu reading cần đoạn văn. `content_asset_links` gắn được PASSAGE vào section hoặc question version.
- Loại gói `PRACTICE_SET` đã có (`V1:74`). Seed V4 có một gói loại này.
- Tiền lệ giấu đáp án: `reveal_answer` trong `app/practice/store.py:47-69`.
- DeepTutor làm lại đúng câu cũ: `ErrorRecord` theo `question_id` (`app/mastery/models.py:138-150`).
- Đã chốt từ trước: câu do AI sinh không tính vào mastery.

## Quyết định đã chốt

| # | Câu hỏi | Chọn | Đã loại |
| --- | --- | --- | --- |
| 1 | Phản hồi sau khi nộp khối | Chỉ đúng/sai; đáp án và giải thích chỉ hiện khi khối đạt ≥ 70% | Không bao giờ hiện; gợi ý không lộ đáp án |
| 2 | Nguồn câu cho bài ôn | Ngân hàng soạn sẵn theo KP | LLM sinh; ngân hàng rồi LLM dự phòng |
| 3 | Làm lại khối trong bài đang học | Giữ câu cũ, chỉ giấu đáp án | Đổi câu khác cùng KP |
| 4 | Làm lại đề cuối | Nhiều mã đề; làm lại thì lấy đề khác | Ngoài phạm vi vòng này |
| 5 | Hình dạng ngân hàng câu ôn | Gói `PRACTICE_SET`: 1 đoạn văn + 3–5 câu | Câu rời theo KP |
| 6 | Bài ôn chưa đạt | Lấy gói khác chưa làm; hết thì gói làm lâu nhất | Làm lại đúng gói đó |
| 7 | Nộp lại khối đã từng đạt (lời giải đã hiện) | Chấm, báo đúng/sai, **không ghi mastery** | Vẫn ghi mastery |

## Thiết kế

### 1. Phản hồi và mastery

- Response `POST /api/ai-learning/lessons/{lessonId}/exercises/{blockId}/submissions`:
  - **Chưa đạt:** `{ blockPassed: false, scorePercent, results: [{ questionVersionId, correct }] }`. Không có `correctAnswer`, không có `explanation`.
  - **Đạt:** mỗi phần tử `results` thêm `correctAnswer` và `explanation`.
- `GET /api/ai-learning/lessons/{id}` trả lời giải cho những khối học viên đã đạt; khối chưa đạt thì không.
- Mastery:
  - Nộp khối **chưa từng đạt**: ghi bằng chứng cho mọi câu, như quyết định "mọi lần nộp đều ghi".
  - Nộp khối **đã từng đạt**: chỉ để luyện, không ghi bằng chứng.
- Đề cuối: `GET /api/assessments/attempts/{id}/result` trả điểm và đúng/sai từng câu. Lời giải chỉ có khi `percent ≥ 70`. Đây là thay đổi contract response, cần duyệt.

### 2. Bài ôn dùng gói câu mới

- **Content:** không thêm bảng.
  - Gói `PRACTICE_SET` gồm section (asset PASSAGE) và `section_questions` (3–5 câu, mỗi câu có `question_knowledge_points`). KP của gói suy ra từ KP của các câu.
  - Luật soạn nội dung: câu trong gói luyện tập không trùng câu trong bài học hay đề cuối của cùng topic. Kiểm lúc publish nếu có API soạn nội dung; MVP kiểm bằng test seed.
  - Endpoint nội bộ mới: `POST /internal/learning-content/practice-sets/search { knowledgePointId, excludePackageIds[] }` trả danh sách ứng viên `{ packageId, packageVersionId, questionCountForKp }`.
  - Đọc đề: dùng chung `GET /internal/.../package-versions/{id}` với assessment (section, câu, `answerSpec`, KP). Endpoint phải nằm dưới `/internal`.
- **ai-learning:**
  - Migration: bảng `path_review_sets` (`id`, `review_item_id` FK `path_review_items`, `user_id`, `package_id`, `package_version_id`, `assigned_at`, `submitted_at`, `score_percent`, `passed`).
  - Luồng mở bài ôn:
    1. Tìm set đang mở của bài ôn; nếu chưa có thì chọn gói.
    2. Chọn gói: gói chưa từng giao cho học viên (theo `path_review_sets`) có câu đo KP yếu. Hết thì lấy gói giao lâu nhất.
    3. Ghi `path_review_sets`.
    4. Trả lý thuyết của bài dạy KP (ví dụ L3), đoạn văn và câu hỏi của gói, không kèm `answerSpec`.
  - Nộp bài ôn:
    - Chấm theo `answerSpec` và ghi mastery cho mọi câu (câu mới nên bằng chứng có giá trị).
    - ≥ 70%: `path_review_items` chuyển DONE.
    - < 70%: set đóng lại (`passed=false`); lần mở sau giao gói khác.
    - Idempotent theo `requestId` như nộp khối.
  - Cổng 403 `REVIEW_REQUIRED` giữ nguyên. Hoàn thành bài ôn không kích hoạt đánh giá lại path.
  - API học viên: `path_review_items` vẫn trỏ tới bài dạy KP (để lấy lý thuyết). Response mở bài ôn có thêm `reviewSet`. Tên endpoint cụ thể chốt trong plan (ví dụ `GET /api/ai-learning/reviews/{reviewId}`, `POST .../reviews/{reviewId}/submissions`).

### 3. Nhiều mã đề cuối

- **Content:** thêm `content_packages.topic_id` (nullable, dùng cho `TOPIC_TEST`), thay cho `topics.test_package_id`. Endpoint nội bộ `GET /internal/learning-content/topics/{topicId}/test-packages` trả các mã đề đã publish.
- **ai-learning:**
  - Endpoint `GET /api/ai-learning/topics/{topicId}/test`:
    - Cổng: mọi bài đã xong, không còn bài ôn chờ.
    - Chọn mã đề chưa làm; hết thì lấy mã làm lâu nhất.
    - Ghi bảng `topic_test_assignments` (`user_id`, `topic_id`, `package_id`, `package_version_id`, `assigned_at`, `best_percent`, `completed_at`).
    - Trả `packageVersionId`.
  - App gọi `POST /api/assessments/attempts { packageVersionId }` như thiết kế trước.
  - **Giải chỗ chặn B1:** consumer nhận `AssessmentCompleted.v2` kèm `package_version_id`, rồi tra `topic_test_assignments` ngay trong DB. Không cần gọi HTTP sang content, nên không cần token cho consumer. Event vẫn phải thêm `package_version_id`.

## Đổi so với các quyết định trước

- Bài ôn **không còn** là "làm lại bài tập của bài đã học". Nó là lý thuyết của bài đó cộng một gói câu mới đo KP yếu.
- Response nộp khối không còn luôn kèm giải thích.
- Một topic có nhiều mã đề cuối; bỏ `topics.test_package_id`.
- Thêm luật: nộp lại khối đã đạt thì không ghi mastery.
- Consumer không gọi content để tra topic (thay cho phương án (a)/(b) của B1 trong scout report).

## Chi phí và rủi ro

| Rủi ro | Giảm thiểu |
| --- | --- |
| Công soạn nội dung: mỗi KP cần ≥ 2 gói luyện tập, mỗi topic ≥ 2 mã đề. DEMO_READING (4 KP) cần khoảng 8 gói và 2 đề | MVP nạp bằng seed migration. API soạn nội dung (B4) là việc riêng |
| Hết gói chưa làm thì dùng lại gói cũ, có thể đã hiện lời giải | Chọn gói làm lâu nhất; theo dõi số gói mỗi KP. Có thể thêm LLM dự phòng sau MVP |
| Câu trong gói luyện tập trùng câu bài học hoặc đề cuối | Test seed kiểm không trùng trong cùng topic |
| Endpoint đọc đề trả `answerSpec` | Chỉ nằm dưới `/internal`; API học viên luôn bỏ `answerSpec` |
| Đổi contract: response nộp khối, response kết quả đề, event thêm `package_version_id` | Ghi vào `docs/contracts/`, cần duyệt theo AGENTS §5 |
| Đoán loại trừ vẫn làm được khi làm lại khối trong bài | Chấp nhận: bài ôn sau đó đo lại bằng câu mới |

## Tiêu chí nghiệm thu

- Nộp khối 2/3: response không có `correctAnswer` hay `explanation`. Nộp đạt: có cả hai.
- Nộp lại một khối đã đạt: có kết quả đúng/sai, mastery không đổi.
- KP2 bị chèn bài ôn: mở bài ôn nhận lý thuyết L3 và một gói `PRACTICE_SET` có câu đo KP2, không có câu nào đã gặp trong L3 hay đề cuối.
- Bài ôn 1/4 → lần mở sau nhận gói khác. Đạt 3/4 → DONE, mở được bài khác.
- Làm lại đề cuối: nhận mã đề khác với lần trước (khi còn mã chưa làm).
- Consumer xử lý kết quả đề cuối mà không gọi HTTP.

## Bước tiếp

1. Cập nhật demo theo thiết kế này (giấu đáp án tới khi đạt, bài ôn dùng gói mới, 2 mã đề).
2. Gộp vào plan triển khai pipeline học (`/ck:plan`), cùng scout report.
3. Viết contract: `answer-spec-v1`, API bài học và bài ôn, thay đổi `assessment-completed-v2`.

## Câu hỏi mở

- Số câu mỗi gói luyện tập cố định (ví dụ 4, để 3/4 = 75% là đạt) hay để 3–5 tùy gói? Với 3 câu thì phải 3/3 mới ≥ 70%.
- Gói luyện tập đo nhiều KP: chỉ cần có câu đo KP yếu, hay KP yếu phải chiếm đa số?
- Có dùng gói `PRACTICE_SET` làm tính năng "luyện thêm" tự chọn ngoài bài ôn không? Nếu có thì dùng chung lịch sử "đã làm".
- Lời giải đề cuối khi đạt: có lộ đề cho người khác làm cùng mã không? Chấp nhận trong MVP.
