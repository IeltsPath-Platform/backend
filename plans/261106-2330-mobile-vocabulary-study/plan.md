---
title: "BE: học từ vựng theo unit cho mobile, XP/streak/tim độc lập với web"
description: "library-service cung cấp unit do content author biên soạn, chấm bài theo attempt, và hệ thống XP/streak(freeze)/tim riêng; hồi tim và mua freeze bằng Points của access-service"
status: pending
priority: P1
branch: ""
tags: [backend, library-service, vocabulary, gamification, hearts, streak, points]
blockedBy: []
blocks: []
created: "2026-11-06T16:30:00.000Z"
createdBy: "ck:plan"
source: skill
---

# BE: học từ vựng theo unit, XP / streak / tim độc lập với web

**Người đọc: dev backend** (Spring Boot, library-service). Tài liệu này tự đủ để làm mà không cần xem hội thoại hay code mobile. Hình dạng JSON của mọi endpoint nằm ở hợp đồng API: [api-contract.md](./api-contract.md) (bản sao của `mobile/docs/vocabulary-study-backend-contract.md`). Plan này nói **làm gì, ở đâu, theo thứ tự nào, kiểm thử thế nào**, và ghi lại những chỗ code hiện tại buộc phải làm khác hợp đồng.

Các file: [phase 1](./phase-01-units-schema-authoring-read.md) · [phase 2](./phase-02-attempts-grading-progress.md) · [phase 3](./phase-03-gamification-and-points.md) · [phase 4](./phase-04-seed-and-verification.md).

## 1. Bối cảnh

App mobile chỉ dùng để **học từ vựng theo chủ đề**. Hôm nay mobile làm mọi thứ ở máy:

- tự cắt danh sách từ của chủ đề thành tối đa 9 "bài" theo trang, đặt tên bài bằng tay (tên không gắn với tập từ nên có thể sai nội dung khi catalog đổi);
- tự sinh và tự chấm trắc nghiệm, ghép từ, điền từ;
- lưu bài đã xong trong bộ nhớ máy (không sang máy khác, không có thưởng);
- hiển thị XP và streak lấy từ learning-service, tức là **số của bản web**. Mobile chưa có tim.

Yêu cầu mới: server là nguồn sự thật cho bài học và tiến độ; XP, cấp độ, streak (có freeze) và tim của mobile là **hệ thống riêng**, không cộng dồn và không ảnh hưởng gì tới bản web.

## 2. Quyết định đã chốt (06/11/2026)

| Chủ đề | Quyết định |
|---|---|
| Độc lập | XP, streak, tim của mobile tách hẳn web. Chỉ dùng chung: `userId` + token, và **ví Points** của access-service |
| Tim | Tối đa 5, **Premium tối đa 7**; hồi 1 tim mỗi 5 giờ; hết tim thì không bắt đầu được bài mới hoặc nộp đáp án chấm điểm; hồi đầy bằng 5 Points (cùng giá web) |
| XP | Lần hoàn thành đầu của một unit: 20 XP; thêm 10 XP nếu không sai bước nào; replay: 0 XP; 500 XP mỗi cấp |
| Streak | Một ngày được tính khi **hoàn thành một unit (kể cả replay) hoặc ôn một flashcard**. Có freeze mua bằng Points, cùng công thức giá như web |
| Unit | Do **content author** biên soạn (admin không), không tự cắt theo thứ tự từ |
| Khách (chưa đăng nhập) | Giữ nguyên như hiện tại: học thử trên máy từ catalog công khai, không có tiến độ/XP/streak/tim phía server. BE không làm API cho khách; chỉ cần hai endpoint đọc unit để công khai |
| Màn `learn`, `practice` cũ của mobile | Giữ nguyên, không đổi trong plan này |

## 3. Giả định đã chọn (dev có thể hỏi lại sản phẩm, đổi rẻ vì nằm trong cấu hình hoặc một lớp rule)

1. Ôn flashcard (bất kể đúng hay sai) tính một ngày streak nhưng **không** cộng XP và không liên quan tim.
2. Replay không kiểm tra và không mất tim. Bước WORD (xem từ) không mất tim.
3. Mỗi bước chấm điểm mất tối đa 1 tim, ở lần nộp sai đầu tiên của bước đó.
4. Giá hồi tim 5 Points. Giá freeze `10 × số ngày freeze liên tiếp tính cả ngày mua`, tối đa 5 ngày liên tiếp, giữ tối đa 1 freeze. Giống web (đã xác nhận).
5. Không trần XP mỗi ngày, không thưởng mốc streak, không league/huy hiệu/nhiệm vụ ngày.
6. Hồ sơ từ vựng có múi giờ riêng, mặc định `Asia/Ho_Chi_Minh`.
7. Premium có 7 tim, người thường 5, giống web. Gói được đọc từ access-service và làm mới tối đa mỗi 10 phút mỗi người (phase 3 mục 6b).

## 4. Phạm vi

**Trong phạm vi plan này (BE):**

- library-service: bảng, API biên soạn unit (content author), API đọc unit, attempt, chấm bài, tiến độ/mở khóa, XP, streak, tim, mua bằng Points.
- config-server + api-gateway + docker-compose: route, biến môi trường, cấu hình.
- Hai lời gọi mới từ library-service sang access-service: trừ Points (khi mua) và đọc gói Premium (để biết số tim tối đa).
- Một điểm nối vào code flashcard có sẵn: `ReviewFlashcardUseCase` ghi một ngày streak mobile khi có lượt ôn mới (phase 3 mục 5b).

**Ngoài phạm vi (đội khác), nhưng cần BE xong trước:**

| Việc | Đội | Cần gì từ BE |
|---|---|---|
| Màn biên soạn unit cho content author (tab mới cạnh phần biên soạn từ vựng hiện có) | Frontend | 7 endpoint biên soạn ở hợp đồng 4.1, xong sau **phase 1** |
| Tích hợp app mobile (bỏ tiến độ lưu máy, gọi API mới, hiển thị tim/XP/streak) | Mobile | 11 endpoint ở hợp đồng mục 4, xong sau **phase 2 và 3** |

**Không đụng:** learning-service (XP/streak/tim của web giữ nguyên), content-service, các màn `learn`/`practice` cũ của mobile.

## 5. Kiến trúc

```
Mobile ──HTTPS──> api-gateway ──> library-service ──> library_db (Postgres)
                                        │
                                        └─ HTTP, chỉ khi mua tim/freeze ─> access-service (/internal/access/points/debit)

learning-service, content-service: không tham gia.
```

Mọi bảng mới nằm trong `library_db`, tiền tố `vocabulary_study_`, không có khóa ngoại sang bảng của service khác. Quy tắc XP/streak/tim được **sao chép** từ learning-service sang code của library-service; không import module learning, không gọi API learning.

## 6. Thuật ngữ

| Từ | Nghĩa |
|---|---|
| Topic | Chủ đề từ vựng đã có (`vocabulary_topics`) |
| Sense | Một nghĩa của một từ (`vocabulary_senses`); đơn vị học là sense, không phải từ |
| Unit | Bài học nhỏ trong topic: tên, lời dẫn, danh sách sense có thứ tự |
| Revision | Phiên bản của unit. Sửa unit đã phát hành tạo revision mới |
| Curriculum version | Số tăng mỗi khi phát hành, lưu trữ hay sắp xếp lại unit của một topic; mobile dùng để biết cache cũ |
| Attempt | Một lượt học một unit: danh sách bước đã sinh sẵn và lưu lại (snapshot) |
| Step | Một bước trong attempt: `WORD`, `CHOICE`, `MATCH`, `FILL`. Ba loại sau là bước chấm điểm |
| `FIRST_COMPLETION` / `REPLAY` | Chế độ attempt: lần học đầu để hoàn thành unit / học lại unit đã xong |

## 7. Quy ước chung trong library-service (rút từ code hiện có)

Tất cả phase tuân theo mục này.

**Cấu trúc package** `com.ieltspath.library`: `api.controller`, `api.dto.request|response`, `application.usecase|port|result|service|exception`, `domain.study` (rule thuần, không Spring), `infrastructure.adapter|client|config`, `config` (ConfigurationProperties).

**Truy cập dữ liệu:** JDBC thuần với `NamedParameterJdbcTemplate` trong lớp `@Repository` (mẫu: `VocabularyProgressJdbcAdapter`). Cột JSONB ghi bằng `?::jsonb` hoặc `cast(:p as jsonb)` với chuỗi từ `ObjectMapper`. Thời gian lấy từ bean `Clock libraryClock` (`LibraryTimeConfig`), không gọi `Instant.now()` trực tiếp, để kiểm thử được.

**Người dùng hiện tại:** `CurrentUserProvider.requireUserId()` (module `common-security`). Không nhận `userId` từ body hay query.

**Lỗi.** `GlobalExceptionHandler` hiện chỉ có `ConflictException` (409, message cố định, không `code`) và `InvalidDataException` (400). `ErrorResponse` đã có trường `code` (constructor 7 tham số) nhưng chưa có exception nào điền. Cần thêm:

- `application/exception/StudyRequestException` mang `HttpStatus status`, `String code`, `String message` (tiếng Việt, hiển thị được cho người học), `Map<String,String> details`;
- một `@ExceptionHandler(StudyRequestException.class)` trong `GlobalExceptionHandler` trả `new ErrorResponse(now, status, reasonPhrase, message, path, details, code)`.

Định dạng lỗi thực tế sẽ là `{timestamp, status, error, message, path, details, code}`, không có `traceId` như bản nháp cũ của hợp đồng (hợp đồng mục 10 đã chỉnh theo). `details` là `Map<String,String>` nên mọi giá trị là chuỗi. Ví dụ:

```json
{
  "timestamp": "2026-11-07T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "Bạn đã hết tim. Chờ hồi tim hoặc dùng điểm để hồi đầy.",
  "path": "/api/learning-support/vocabulary-study/attempts/6f0c…/steps/1a2b…/answers",
  "details": { "nextHeartAt": "2026-11-07T15:02:11Z", "refillCost": "5" },
  "code": "VOCAB_NO_HEARTS"
}
```

**Bảo mật:**

- Mặc định mọi endpoint cần JWT do gateway ký. Chỉ các `GET` liệt kê trong `app.security.public-endpoints` của `library-service.yaml` là công khai. Hai endpoint nội dung mới (`/api/content/vocabulary/topics/{topicId}/units`, `/api/content/vocabulary/units/{unitId}`) phải được **thêm vào danh sách này** (dùng cùng regex UUID như các dòng sẵn có), nếu không chúng sẽ đòi đăng nhập.
- Controller biên soạn unit dùng `@PreAuthorize("hasRole('CONTENT_AUTHOR')")`: **chỉ content author, không cho `ADMIN`**. (`AdminVocabularyCatalogController` hiện cho cả hai nên test `BusinessControllerRoleConventionTest` có ngoại lệ ghi cứng cho nó; controller mới chỉ dùng `CONTENT_AUTHOR` nên qua test mà không cần thêm ngoại lệ.)
- Controller của học viên (attempt, me/*) không dùng `ADMIN` trong `@PreAuthorize`.

**Khóa theo người dùng.** Mọi ghi trạng thái của một người (attempt, XP, streak, tim, mua) chạy trong transaction ngắn, bắt đầu bằng khóa advisory:

```sql
SELECT pg_advisory_xact_lock(hashtext('vocab-study:' || :userId))
```

Mẫu: `JdbcLearnerLock` của learning-service (ném lỗi nếu không có transaction đang mở). Tạo `JdbcStudyLock` tương tự trong library-service.

**Không gọi HTTP khi đang giữ khóa hoặc transaction.** Lời gọi duy nhất ra ngoài (trừ Points) nằm giữa hai transaction (phase 3). Đây chính là lỗi vừa phải sửa ở consumer kết quả examiner của learning-service.

**Idempotency.** Mọi POST ghi nhận `requestId` (UUID do mobile sinh). Lưu kèm `payload_hash` = SHA-256 hex của JSON chuẩn hóa của body (khóa sắp xếp, mảng `pairs` sắp theo `leftOptionId`). Cùng `requestId` và cùng hash: trả lại kết quả đã lưu. Cùng `requestId` khác hash: `409 IDEMPOTENCY_CONFLICT`. Tra cứu `requestId` diễn ra **trước** các kiểm tra trạng thái hiện tại (tim, thứ tự bước), để retry sau khi trạng thái đã đổi vẫn trả đúng kết quả cũ.

**Flyway.** Migration của library-service hiện dừng ở `V38`. Phase này dùng `V39` đến `V42`. Trước khi tạo file, kiểm tra nhánh `main` và các PR đang mở xem số đó còn trống không.

**Phụ thuộc mới.** `pom.xml` của library-service chưa có `common-resilience` (hiện chỉ có `common-security`, `common-observability`, `common-storage`). Phase 3 thêm nó. Service chưa có HTTP client nào ra ngoài (không `RestClient`, `RestTemplate`, `WebClient`).

**Test.** Mẫu test tích hợp: `VocabularyNotebookJdbcIntegrationTest` (`@JdbcTest`, Testcontainers PostgreSQL, Flyway bật, `@Testcontainers(disabledWithoutDocker = true)`). Chạy bằng `mvn -pl services/library-service -o clean test` (dùng `clean`; build tăng dần đã từng báo "Nothing to compile" và chạy lại lớp cũ ở learning-service). `ServiceBoundaryTest` chỉ cấm library gọi ngược content-service, không cấm gọi access-service.

**springdoc.** Thêm `/api/learning-support/vocabulary-study/**` vào `springdoc.paths-to-match` trong `library-service.yaml` để Swagger của gateway thấy API mới.

## 8. Thứ tự làm và PR

| Phase | Nội dung | Phụ thuộc | PR gợi ý |
|---|---|---|---|
| 1 | Bảng unit, API biên soạn (content author), API đọc nội dung | - | 1 PR |
| 2 | Attempt: sinh bài, chấm, tiến độ, mở khóa, hoàn thành | 1 | 1 PR |
| 3 | XP, streak, tim, mua bằng Points, route gateway, cấu hình | 2 | 1–2 PR (tách phần Access client nếu muốn review riêng) |
| 4 | Dữ liệu mẫu, chạy thử đầu cuối, đối chiếu checklist | 1–3 | 1 PR |

Phase 2 và 3 phải nằm trên cùng nhánh phát hành: sau phase 2 API `complete` chưa có phần `reward` nên **chưa được bật cho mobile**. Frontend (màn biên soạn của content author) có thể bắt đầu ngay sau phase 1.

## 9. Định nghĩa hoàn thành

- Một học viên học xong một unit chỉ bằng API, tiếp tục được từ máy khác, và replay được.
- Hoàn thành lần đầu cộng đúng 20 XP (+10 nếu không sai bước nào) **một lần**, dù retry, hai máy cùng bấm, hay unit được sửa revision. Replay cộng 0 XP nhưng vẫn tính ngày streak.
- Làm bài trên mobile không đổi `learner_xp`, `learner_streaks`, tim của web; làm bài trên web không đổi bảng `vocabulary_study_*` (chứng minh bằng test kiến trúc và bằng truy vấn ở phase 4).
- Hết tim: không bắt đầu được attempt đầu tiên mới, không nộp được đáp án chấm điểm (`VOCAB_NO_HEARTS`); đọc nội dung và replay vẫn được; sau 5 giờ có 1 tim; hồi bằng Points khôi phục đầy.
- Hồi tim/mua freeze hai lần cùng `requestId` chỉ trừ Points một lần; Access lỗi không làm mất Points và không đổi trạng thái.
- Content author tạo, kiểm tra, phát hành, sắp xếp, lưu trữ unit qua API; attempt đang làm giữ nguyên snapshot sau khi unit sửa.
- Ôn một flashcard trong ngày giữ streak mobile (kể cả khi chưa hoàn thành unit nào), không cộng XP; gọi lại cùng `requestId` không tính thêm.
- Người dùng Premium có tối đa 7 tim, người thường 5; hạ gói thì tim bị cắt về 5; Access lỗi thì giữ mức tối đa đã lưu và vẫn học được.
- Toàn bộ test của library-service, test route của api-gateway và checklist hợp đồng mục 11 đạt.

## 10. Rủi ro

| Rủi ro | Cách xử lý |
|---|---|
| Library-service có HTTP client ra ngoài lần đầu (Access) | Sao chép mẫu `RestAccessClient` của learning-service (ResilientCall, timeout 2 s / 5 s, chuyển bearer token); test hợp đồng với server giả |
| Content author chưa kịp biên soạn nên mobile không có dữ liệu | Phase 4 tạo dữ liệu mẫu bằng migration, giống `V20__seed_vocabulary_catalog_demo.sql` |
| Points dùng chung với web, tiêu từ hai phía | Người dùng đã chấp nhận. Mô tả giao dịch ghi rõ "từ vựng" để sổ điểm đọc được |
| Đổi múi giờ gần nửa đêm có thể thêm hoặc mất một ngày streak | Ngày hoàn thành được ghi cố định lúc ghi; đổi múi giờ chỉ ảnh hưởng sự kiện sau |
| Nhầm "streak học từ vựng" với chuỗi ôn flashcard có sẵn trong `/notebook/achievements` | Đặt tên rõ trong API; không sửa achievements |
| Sửa nghĩa (sense) hoặc đặt INACTIVE sau khi unit đã phát hành | Attempt dùng snapshot, không bị ảnh hưởng; API đọc nội dung bỏ sense INACTIVE; `validation` báo ở lần sửa kế tiếp |
| Hai thiết bị cùng thao tác | Khóa advisory theo người dùng, ràng buộc unique, và test chạy đồng thời ở mỗi phase |
| Ghi streak nằm trong transaction của `ReviewFlashcardUseCase`: lỗi ghi streak làm hỏng cả lượt ôn | Ghi streak là một upsert đơn giản trên bảng riêng; có test lượt ôn vẫn thành công khi streak chưa có dòng. Khóa: thẻ trước, khóa người dùng sau; `complete` chỉ lấy khóa người dùng nên không có vòng chờ |
| Đọc gói Premium thêm tải lên Access | Làm mới tối đa mỗi 10 phút mỗi người, ngoài transaction, có mạch ngắt; lỗi thì giữ mức tối đa đã lưu |

## 11. Câu hỏi mở

Không còn câu hỏi chặn. Đã chốt ngày 07/11/2026: ôn flashcard giữ streak; Premium 7 tim; giá hồi tim và freeze giống web; màn `learn`/`practice` giữ nguyên; khách giữ cách học thử hiện tại (trên máy).

Việc ngoài BE cần lưu ý: các màn `learn`/`practice` cũ vẫn hiển thị XP của web, còn màn từ vựng hiển thị XP mobile. Đây là hệ quả của quyết định giữ nguyên, đội mobile cần đặt nhãn rõ để người dùng không nhầm hai con số.

