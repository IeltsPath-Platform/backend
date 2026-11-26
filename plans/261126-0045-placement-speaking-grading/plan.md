---
status: pending
created: 2026-11-26
---

# Chấm Speaking trong bài test đầu vào

## Hiện trạng (kiểm trên `main` 84b78fc7)

- Web upload bản ghi lên assessment-service (`speaking/{userId}/…`, bucket `speaking-recordings`) rồi nộp learner
  submission `SPEAKING`. Không có job chấm.
- `PlacementGradingService` chỉ cho Speaking một band cố định nếu cấu hình `assessment.placement.speaking-band`
  (mặc định không có).
- learning-service đã có `AzureSpeechClient`, `SpeakingGrader` (LLM), validator, rubric, nhưng gắn với bài luyện
  (trừ point, quota, mastery). Chỉ nhận WAV 16 kHz mono 16-bit; recorder placement gửi webm/mp4.
- Plan `261103-1156-ai-speaking-assessment` đã loại placement Speaking khỏi phạm vi.

## Thiết kế

Learning đã gọi HTTP sang assessment, nên assessment không được gọi ngược (AGENTS §3.5). Dùng event qua outbox:

1. Assessment, khi nộp bài placement có bản ghi và cờ bật: tạo job Speaking `PENDING`, ghi outbox
   `SpeakingGradingRequested.v1` (eventId, attemptId, userId, các item: attemptItemId, part, câu hỏi, object key).
2. Learning consume (idempotent theo eventId/attemptId, retry + DLQ), lưu job riêng cho placement; scheduler claim
   bằng `FOR UPDATE SKIP LOCKED`, đọc bản ghi qua `common-storage`, gọi Azure rồi `SpeakingGrader` ngoài transaction.
3. Learning ghi outbox `SpeakingGradingCompleted.v1` (eventId, attemptId, userId, status `GRADED|FAILED`, band,
   band từng tiêu chí, nhận xét ngắn, rubric code/version).
4. Assessment consume, lưu kết quả, hoàn tất bài khi Writing và Speaking đều xong hoặc quá hạn chờ.

Tên event, payload ở `common-events`; contract mới trong `docs/contracts/`.

## Quy tắc nghiệp vụ (đề xuất, chờ chủ dự án xác nhận)

| # | Quy tắc |
| --- | --- |
| R1 | Chấm miễn phí: không trừ point, không tính quota luyện Speaking, không ghi mastery. Chi phí chặn bởi giới hạn 3 lượt/24 giờ |
| R2 | Bỏ qua phần Speaking: không có band Speaking, như hiện nay |
| R3 | Đã ghi âm nhưng chấm lỗi, chưa cấu hình, hoặc quá 10 phút: bỏ band Speaking, `partial = true`, không chuyển giám khảo |
| R4 | Cờ `assessment.placement.speaking-grading.enabled` mặc định `false`; tắt thì giữ hành vi hiện tại |
| R5 | Rubric Speaking đang ACTIVE của Content; bảng quy đổi phát âm hiện tại là tạm, chưa hiệu chỉnh với giám khảo |

## Phase

1. `common-events` + contract hai event. 2. Assessment: job, outbox, consumer, hoàn tất bài, timeout (migration mới).
3. Learning: consumer, job, chấm, outbox (migration mới). 4. Web: đổi bản ghi sang WAV 16 kHz mono (dùng
`toWav16kMono` có sẵn), chờ kết quả, hiện band và nhận xét Speaking. 5. Test và tài liệu.

## Tiêu chí xong

- Nộp bài có ghi âm và cờ bật: kết quả có band Speaking, band tổng gồm 4 kỹ năng, vẫn bị trần theo kho.
- Event gửi lặp không chấm hai lần; lỗi theo R3 không làm kẹt bài.
- Cờ tắt: mọi test placement hiện có giữ nguyên.

## Rủi ro và điểm phải kiểm khi làm

- `SpeakingGrader` giả định cấu trúc part của bài luyện; đề placement phải khớp hoặc cần chế độ riêng.
- Hai service dùng chung bucket: xác nhận cấu hình `ASSESSMENT_SPEAKING_BUCKET` = `LEARNING_SPEAKING_BUCKET`.
- Mobile đang dùng route placement cũ và không đổi trong plan này.
