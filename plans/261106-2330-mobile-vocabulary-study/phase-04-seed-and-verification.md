# Phase 4: Dữ liệu mẫu và nghiệm thu đầu cuối

Status: complete for backend delivery; final focused 38 and full Library 407 tests passed, and the final fresh isolated Gateway matrix passed 404 checks with exit 0. [Live acceptance](./reports/vocabulary-study-e2e-verification.md) preserves earlier harness failures and the final passing rerun. Non-null remote media and physical-device/client fallback verification remain explicitly unchecked client acceptance work.

Implementation clarification (2026-11-07): the fresh catalog receives all eight curated units. On upgrade, V42 preserves author edits and skips an entire demo topic if a chosen primary sense is inactive, has an unsuitable meaning/example, or duplicates a meaning within a unit. Valid demo units append after existing author units and bump the existing curriculum version once per topic. This prevents demo data from blocking service startup or hiding a curriculum change from cached clients.

Phụ thuộc: phase 1–3. Mục đích: chứng minh cả chuỗi chạy thật (gateway, xác thực, access, library, DB) và đối chiếu từng mục của checklist hợp đồng mục 11. Phase này chỉ làm việc backend; đội mobile và frontend dùng kết quả để tích hợp.

## 1. Dữ liệu mẫu `V42__seed_vocabulary_study_units_demo.sql`

Mục đích: để mobile và màn biên soạn có dữ liệu chạy được trước khi content author biên soạn thật. Cùng cách làm với `V20__seed_vocabulary_catalog_demo.sql` và `V22__seed_vocabulary_catalog_more.sql`.

- Chọn **hai topic** đã có trong catalog có ít nhất 24 sense `ACTIVE` với `vietnamese_meaning` và `example_sentence`. Chọn bằng truy vấn trên dữ liệu đã seed, không bịa từ:

```sql
SELECT t.id, t.slug, count(*) AS active_senses
FROM vocabulary_topics t
JOIN vocabulary_senses s ON s.topic_id = t.id AND s.status = 'ACTIVE'
JOIN vocabulary_items i  ON i.id = s.vocabulary_item_id AND i.status = 'ACTIVE'
WHERE t.status = 'ACTIVE'
GROUP BY t.id, t.slug
HAVING count(*) >= 24
ORDER BY active_senses DESC;
```

- Mỗi topic tạo 3 unit `LEARN` mỗi unit 8 sense (đã phát hành, revision 1) và 1 unit `REVIEW` ôn tập gồm các sense lấy từ 3 unit trước (để kiểm thử `uniqueWordCount` không đếm trùng). Tên unit phải mô tả đúng nhóm từ trong đó (đọc danh sách từ rồi đặt tên), không dùng tên chung chung.
- Đặt `created_by` bằng UUID cố định của tài khoản hệ thống/seed theo quy ước các seed hiện có (xem V20/V22).
- Đặt `curriculum_version` của hai topic lên 1 và gán `exercise_types`, `graded_step_count` giống phát hành thật (bộ sinh bài đã có từ phase 2; nếu migration không gọi được Java, tính tay và ghi sẵn giá trị hợp lý, kèm một test xác nhận seed khớp với kết quả của bộ sinh).
- Test `StudyUnitSeedMigrationTest` (mẫu `LessonVocabularySeedMigrationTest`): sau khi chạy mọi migration, mỗi unit seed có đủ 8 sense hợp lệ, `validation` không báo lỗi chặn, và `GET units` trả đúng thứ tự.

## 2. Chạy thử đầu cuối qua gateway

Dựng stack bằng docker compose (gateway, auth, access, library, learning, config-server, Eureka, Postgres, RabbitMQ). Dùng một tài khoản học viên thật có token và có sẵn vài Points (cấp bằng cơ chế admin/điểm hiện có của access-service).

Kịch bản gọi tuần tự (curl hoặc file `.http`, lưu vào `backend/scripts` hoặc thư mục tương đương nếu repo có quy ước):

1. `GET /api/content/vocabulary/topics/{topicId}/units` (không token): trả đúng 4 unit.
2. `GET /api/learning-support/vocabulary-study/progress?topicIds={topicId}`: unit 1 `AVAILABLE`, các unit sau `LOCKED`.
3. `GET .../me/summary` trên tài khoản mới: 0 XP, cấp 1, 5/5 tim, streak 0.
4. `POST .../units/{unit1}/attempts` `RESUME_OR_START`: nhận attempt có WORD, CHOICE, MATCH, FILL.
5. Nộp một đáp án sai (CHOICE): `outcome INCORRECT`, `heartLost true`, `hearts.hearts 4`; nộp lại đúng: `CORRECT`.
6. Nộp hết các bước còn lại, `POST .../complete`: `firstCompletion true`, `xpEarned 20` (vì có sai), streak 1, unit 2 mở khóa.
7. `POST .../units/{unit1}/attempts` với `REPLAY`, làm lại đúng hết, `complete`: `xpEarned 0`, streak vẫn 1 (cùng ngày).
8. Làm unit 2 không sai bước nào: `xpEarned 30`.

## 3. Kiểm tra độc lập với web (hai chiều)

Trước và sau mỗi hành động, ghi lại ba nhóm số:

- Của web: `GET /api/learning/me/progress`, `GET /api/learning/me/streak`, và số tim của web (endpoint `HeartsController` của learning-service).
- Của mobile: `GET .../vocabulary-study/me/summary`.
- Dữ liệu thô bằng SQL: `SELECT * FROM learner_xp, learner_streaks` (learning_db) và các bảng `vocabulary_study_profiles`, `vocabulary_study_xp_ledger`, `vocabulary_study_streaks` (library_db) cho cùng `user_id`.

Pass khi: hoàn thành unit mobile **không** làm đổi bất kỳ số nào của web; làm một bài trên web (ví dụ nộp một bài tập của lesson) **không** làm đổi bất kỳ số nào của mobile; hai sổ XP không dùng chung dòng nào.

## 4. Chạy thử các tình huống lỗi

| # | Tình huống | Kỳ vọng |
|---|---|---|
| 1 | Dừng access-service, gọi hồi tim | 503 `ACCESS_UNAVAILABLE`; Points và tim không đổi; dòng purchase `PENDING` |
| 2 | Bật lại access-service, gọi lại cùng `requestId` | 200; Points trừ đúng 1 lần (kiểm tra sổ điểm của Access); purchase `DONE` |
| 3 | Gọi hồi tim khi tim đầy | 409 `HEARTS_FULL`, không trừ điểm |
| 4 | Gọi hồi tim khi 0 Points | 402 `INSUFFICIENT_POINTS`, không còn dòng purchase |
| 5 | Mua freeze khi chưa có chuỗi | 409 `NO_STREAK_TO_PROTECT` |
| 6 | Hai thiết bị cùng `complete` một attempt | Cùng một `Completion`; chỉ một dòng XP |
| 7 | Gửi lại một đáp án sau khi đã đúng, cùng `requestId` | Kết quả cũ, không đổi tim |
| 8 | Sai tới 0 tim | Nộp bước chấm điểm tiếp theo bị 409 `VOCAB_NO_HEARTS`; `GET attempt` và `REPLAY` unit đã xong vẫn chạy |
| 9 | Chờ qua 5 giờ (đặt `Clock` trong test tích hợp, hoặc đổi `hearts_regen_at` ở DB thử nghiệm) | Có 1 tim; nộp lại được |
| 10 | Đặt múi giờ `Pacific/Auckland` rồi hoàn thành lúc gần nửa đêm UTC | `lastQualifiedDate` đúng ngày theo múi giờ đó |
| 11 | Tác giả sửa và phát hành revision mới khi học viên đang làm dở | Attempt giữ snapshot; sau khi hoàn thành, `GET progress` hiện revision mới cho lần sau |
| 12 | Lưu trữ unit đang có học viên làm dở | Attempt đó tiếp tục hoàn thành được; không ai bắt đầu mới được |
| 13 | Thêm unit mới vào giữa danh sách | Unit đã xong vẫn `COMPLETED`; unit mới chưa mở theo tiền đề |
| 14 | Thiếu audio/ảnh của một từ | Trường `null`, bài vẫn chạy |
| 15 | Token hết hạn giữa phiên | 401 từ gateway; sau khi làm mới token, gọi lại cùng `requestId` an toàn |
| 16 | Tài khoản mới, ôn một flashcard (`POST /api/learning-support/flashcards/{id}/reviews`) rồi đọc `me/summary` | `streak.currentDays = 1`, `qualifiedToday = true`, XP vẫn 0, tim không đổi |
| 17 | Cùng ngày, ôn flashcard rồi hoàn thành một unit | `currentDays` vẫn 1; `streakIncreased = false` ở `Completion` |
| 18 | Gửi lại lượt ôn flashcard cùng `requestId` | Không đổi gì ở streak |
| 19 | Tài khoản Premium (cấp bằng cơ chế admin của Access), đọc `me/summary` lần đầu | `maxHearts = 7`, `hearts = 7` |
| 20 | Hủy/hết hạn Premium của tài khoản đó, chờ quá 10 phút (hoặc dùng `Clock` thử nghiệm), đọc lại | `maxHearts = 5`, tim bị cắt về tối đa 5 |
| 21 | Dừng access-service rồi đọc `me/summary` và học tiếp | Vẫn chạy; `maxHearts` giữ mức đã lưu; không lỗi 5xx cho người học |
| 22 | Khách (không token) gọi `GET .../topics/{id}/units` và `GET .../units/{id}` | 200, nội dung đầy đủ; mọi API `vocabulary-study/*` còn lại trả 401 |

## 5. Hiệu năng sơ bộ

- `GET progress` với 100 topic trên dữ liệu seed: đúng 3 truy vấn, thời gian phản hồi ổn (ghi số đo vào báo cáo; ngưỡng đề xuất < 300 ms ở môi trường local).
- Tạo attempt cho unit 8 từ: dưới 200 ms (một lần đọc từ, một lần đọc bể đáp án nhiễu, một lần ghi hàng loạt).
- Nhật ký (log) mỗi lần mua ghi `requestId`, `kind`, `status`, không ghi token.

## 6. Quan sát và vận hành

- Thêm bộ đếm Micrometer (đã có `common-observability`): `vocabulary_study_attempts_started`, `_completed`, `vocabulary_study_purchases{kind,status}`, `vocabulary_study_access_failures`. Dòng `PENDING` quá 15 phút là tín hiệu cần xem (ghi chú trong tài liệu vận hành; v1 chưa có job tự xử lý).
- Ghi các khóa cấu hình mới (`library.vocabulary-study.*`, `ACCESS_SERVICE_URL`) vào tài liệu vận hành/README của library-service nếu có.

## 7. Bàn giao cho đội khác

- **Frontend (màn biên soạn của content author):** danh sách 7 endpoint ở hợp đồng 4.1 (chỉ vai trò `CONTENT_AUTHOR`), ví dụ JSON ở phase 1, danh sách mã lỗi và cảnh báo của `validation`.
- **Mobile:** 11 endpoint ở hợp đồng mục 4, ví dụ JSON ở phase 2 và 3, bảng lỗi mục 10, quy tắc `requestId`, và cách xử lý `VOCAB_NO_HEARTS` (hiển thị `nextHeartAt`, `refillCost`).
- Cập nhật hợp đồng: đánh dấu endpoint đã triển khai, ghi tên cuối cùng nếu có đổi.
- OpenAPI từ springdoc (`/api-docs/library-service`) phải có đủ các endpoint mới (đã thêm `paths-to-match` ở phase 1).

## 8. Lệnh kiểm thử

- library-service: `mvn -pl services/library-service -o clean test`
- api-gateway: `mvn -pl infra/api-gateway -o clean test` (hoặc đường dẫn module thực tế trong `pom.xml` gốc)
- Dùng `clean` để tránh lớp biên dịch cũ.

## 9. Xong khi

- Mọi ô trong checklist hợp đồng mục 11 được tích kèm bằng chứng (tên test, kết quả lệnh, hoặc ghi chép lần chạy tay).
- Không còn lỗi mở từ mục 4.
- Biên bản bàn giao ghi rõ phần ngoài phạm vi: thưởng mốc streak, job tự hoàn tất purchase `PENDING`, trường hợp hai máy hồi tim cùng lúc (xem phase 3 mục 10). Tim Premium nằm trong phạm vi và đã triển khai.
