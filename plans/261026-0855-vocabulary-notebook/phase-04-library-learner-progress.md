# Phase 04 — Library: tiến độ học từ của học viên

Phụ thuộc: phase 01 (sổ từ), phase 03 (CEFR, chủ đề, bộ từ). Migration: **V21**. Mọi endpoint ở đây cần đăng nhập,
dưới `/api/learning-support/flashcards/notebook/...`; danh tính từ `CurrentUserProvider`.

## Dữ liệu (V21, chỉ thêm)

- `flashcard_review_events(id, user_id, flashcard_id, vocabulary_sense_id NULL, session_id UUID NULL, correct BOOLEAN,
  reviewed_at TIMESTAMPTZ)`, index `(user_id, reviewed_at)`, `(user_id, session_id)`. Ghi trong **cùng transaction** của
  `ReviewFlashcardUseCase` (chỉ khi review thật sự áp dụng — request lặp theo `requestId` không ghi thêm).
  `ReviewFlashcardRequest` thêm trường tùy chọn `sessionId` (không bắt buộc → client cũ vẫn chạy).
- `vocabulary_weekly_goals(user_id PK, target_words INT CHECK 10..1000, updated_at)`; mặc định
  `library.vocabulary.weekly-goal-default` = 120 trong `config-repo/library-service.yaml` khi user chưa đặt.
- "Thành thạo/Đang học/Cần ôn" vẫn suy ra từ `leitner_box`/`next_review_at` (phase 01), không thêm cột.

## Định nghĩa (đặt trong domain, có unit test)

- **Đã học** = mục sổ từ có `review_count > 0` hoặc level ≥ `FAMILIAR`. **Tổng** = số sense `ACTIVE` của catalog.
- **Từ đang học** = `FAMILIAR + LEARNING`; **Cần ôn** = `next_review_at <= now`; **Đã thành thạo** = `MASTERED`.
- **Tuần** = Thứ Hai–Chủ Nhật theo `timezone` IANA (query param, mặc định `Asia/Ho_Chi_Minh`).
- **Tiến độ mục tiêu tuần** = số sense khác nhau được thêm vào sổ hoặc ôn trong tuần; **ngày đạt** = ngày có ≥ 1 lần ôn.

## Endpoint

| Method / Path | Dùng cho Figma | Trả về |
| --- | --- | --- |
| `GET /notebook/summary` (mở rộng phase 01, chỉ thêm trường) | Hero "Bạn đã học 1.246 / 4.000 từ", 3 bộ đếm | thêm `learnedCount`, `catalogTotal`, `learningCount`, `masteredCount` |
| `GET /notebook/weekly-goal?timezone=` | "Mục tiêu tuần 86/120", T2–CN | `{target, progress, remaining, days:[{date, weekday, achieved}]}` |
| `PUT /notebook/weekly-goal` `{targetWords}` | Đặt mục tiêu | như trên |
| `GET /notebook/vocabulary-map` | "Bản đồ từ vựng của bạn" | 6 cột band: `{band, cefr, total, learned, learning, needsReview, notLearned}`, `currentBand` (cột có nhiều từ đang học nhất — hoặc band ước tính FE gửi lên qua `?currentCefr=`), `suggestedBand` (band thấp nhất còn `needsReview`) — một query `GROUP BY cefr_level` join catalog |
| `GET /notebook/topics-progress?setId=` | "Chủ đề đang học", thẻ chủ đề có % và nhãn Hoàn thành/Đang học/Chưa học | mỗi chủ đề: `{topicId, name, total, learned, percent, status}`; `current` = chủ đề có lần ôn gần nhất + `nextWords` (≤ 5 từ chưa học) |
| `GET /notebook/sets/{setId}/progress` | Hero bộ từ "Đã học 180/1.000", 386/224/42 | đếm theo bộ |
| `GET /notebook/recent-result` | "Kết quả gần nhất 88%, 27/30 câu đúng" | phiên (`session_id`) gần nhất: `{sessionId, topic, correct, total, percent, finishedAt}` hoặc 204 |
| `GET /notebook/achievements` | "Thành tích bộ từ" | tính khi đọc, không lưu: chuỗi ngày ôn liên tiếp (Học 7 ngày), số chủ đề hoàn thành, số từ thành thạo (300 từ) — `{code, achieved, progress, target}` |
| `GET /notebook/entries?topicId=&setId=` | Bảng "Từ vựng gần đây" | thêm hai filter vào phase 01, trả thêm `cefrLevel`, `topicName` |

Mọi đếm dùng SQL aggregate (`count(*) FILTER`, `GROUP BY`); không N+1, không lặp gọi repository.

## Không làm (giữ đúng ranh giới)

- "Trình độ hiện tại B1 / IELTS 3.0–4.0": FE đọc `estimatedBand` từ `GET /api/learning/report` (learning-service sở hữu),
  library không gọi learning. "Test lại" → `/placement` nếu chưa làm; placement chỉ một lần (`409 PLACEMENT_ALREADY_DONE`)
  nên khi đã làm thì nút dẫn tới báo cáo học tập — không mở lại placement.
- Không cộng XP/điểm (thuộc learning qua `reward_grants`).

## Kiểm thử

- Unit: định nghĩa đã học/tuần/ngày đạt (múi giờ, ranh giới Chủ Nhật→Thứ Hai), achievements.
- Use case review: request lặp không ghi event thứ hai; `sessionId` null vẫn chạy.
- Testcontainers: V21, vocabulary-map/topics-progress/weekly-goal với dữ liệu nhiều user (không lộ dữ liệu user khác).
- `@WebMvcTest`: 401 không token, 400 timezone sai/`targetWords` ngoài khoảng.
