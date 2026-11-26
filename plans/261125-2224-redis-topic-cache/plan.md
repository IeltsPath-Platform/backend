# Cache cây topic bằng Redis ở learning-service

Status: **implementation complete locally; 88 targeted tests and 2 Content migration tests pass; Learning suite: 2,024 tests, 0 failures, 1 ArchUnit frozen-store error, 2 skipped; post-change benchmark and PR remain; no commit created**.

| Repo | Nhánh | Tách từ |
| --- | --- | --- |
| backend | `feat/redis-content-cache` | `main` tại `221f2efa` |

Không đổi API công khai, event, database, Content hay frontend.

## 1. Mục tiêu

Learning gọi `GET /internal/learning-content/topic-sequence` của Content mỗi lần cần tên, kỹ năng, band của điểm kiến thức
(KP). Response là toàn bộ cây topic và giống nhau cho mọi người học. Plan này lưu response vào Redis 60 giây cho các luồng
**chỉ để hiển thị hoặc mô tả**. Redis là phần phụ: lỗi hoặc tắt thì Learning gọi thẳng Content như hiện nay.

### Ngoài phạm vi

- Mã lỗi, rubric: nếu làm thì dùng RAM, plan riêng.
- Nội dung đề (`package-versions/{id}`): xem lại khi có số đo.
- Không cache: quyền Premium, khóa miễn phí/trả phí, điểm, quota, mastery, tiến độ, URL ký.
- Các use case đang gọi HTTP khi giữ transaction hoặc khóa (`RefreshLearningTopicsUseCase`, `GetQuestsUseCase`,
  `GetLearnerTreeUseCase`...): lỗi có sẵn, sửa ở việc khác.

## 2. Điều kiện trước khi làm

1. **`main` phải khởi động được.** Tại `84b78fc7`, Content có hai migration V148 nên Flyway từ chối chạy. Bản sửa nằm ở nhánh
   local `fix/content-migration-version`, chưa vào `main` lúc viết plan. Cập nhật nhánh này từ `main` sau khi bản sửa được merge.
2. Docker chạy được (Testcontainers) và máy không có container khởi động lại liên tục, nếu không số đo sai.

## 3. Các phase

| Phase | Nội dung | Phụ thuộc |
| --- | --- | --- |
| [01](phase-01-measure.md) | Đo trước khi sửa. **Có điểm dừng, phải chờ duyệt** | Điều kiện 2 |
| [02](phase-02-infrastructure.md) | Dependency, cấu hình, Compose, production | 01 được duyệt, điều kiện 1 |
| [03](phase-03-code.md) | Port, cache Redis, chuyển nơi gọi | 02 |
| [04](phase-04-tests.md) | Test tự động và kiểm tra thật | 03 |
| [05](phase-05-docs-and-release.md) | Tài liệu, PR, deploy, lùi lại | 04 |

Phase 02 đến 05 nằm trong một PR.

## 4. Hiện trạng đã đọc từ code

- `GetTopicSequenceUseCase` (Content) chỉ nhận `includeErrorTraps`, không nhận user hay role.
- `getTopicSequence()` và `getTopicSequence(true)` là hai response khác nhau.
- `Topic` chứa cả dữ liệu quyền và giá: `requiredFeatureKey`, `active`, `KnowledgePoint.earliestLessonPosition`,
  `Course.skillUnlockPoints`, `Course.comboUnlockPoints`. Vì vậy không cache ngầm cho mọi nơi gọi.
- Learning chưa có Redis; api-gateway, user, game, notification và community đã có.
- Healthcheck Docker của Learning đọc `/actuator/health`; thêm starter Redis mà không cấu hình thì Redis chết làm Learning `DOWN`.
- Redis production: `mem_limit: 192m`, AOF bật, không có `maxmemory`. Redis này còn giữ rate limit, giới hạn đăng nhập, vé
  WebSocket, pub/sub, dấu thu hồi token `auth:revoked:*` và grant xác thực lại. **Không đặt chính sách evict toàn bộ khóa** để
  nhường chỗ cho cache, vì có thể làm mất dấu thu hồi token.

## 5. Thiết kế

`LearningContentClient.getTopicSequence(...)` giữ nguyên, luôn gọi Content. Nơi cần cache gọi port riêng `TopicCatalog`. Chỗ bị
bỏ sót vẫn đúng, chỉ không nhanh hơn.

```text
luồng hiển thị -> TopicCatalog (application/port)
                    `-- RedisTopicCatalog (infrastructure/cache)
                          |-- trúng         -> trả từ Redis
                          `-- trượt hoặc lỗi -> LearningContentClient -> ghi Redis (lỗi ghi thì bỏ qua)
```

| Mục | Quyết định |
| --- | --- |
| Khóa | `cache:learning:topic-sequence:v1:base` và `cache:learning:topic-sequence:v1:error-traps` |
| Giá trị | JSON của `List<Topic>`, qua `StringRedisTemplate` |
| TTL | 60 giây, đặt cùng lệnh `SET`; không xóa chủ động |
| Không lưu | Content lỗi, kết quả `null` hoặc rỗng |
| JSON hỏng, Redis lỗi | Coi là trượt, gọi Content |
| Hai request cùng trượt | Cùng gọi Content; chấp nhận như `PlacementCatalog`, không khóa phân tán |
| Đổi trường của `Topic`, `Course`, `KnowledgePoint` | Tăng `v1`; có test chốt danh sách trường |
| Timeout Redis | Kết nối và lệnh 200 ms; mất kết nối thì từ chối lệnh ngay. Không tự viết circuit breaker |
| Công tắc | `learning.topic-cache.enabled`: mặc định trong code `false`, config-repo `true` |

Mặc định trong code là `false` để hơn 40 test của Learning dùng Spring context hoặc kiểm health không vô tình đọc hay ghi Redis
đang chạy trên máy.

## 6. Nơi dùng cache và nơi đọc mới

| Nơi gọi | Cách đọc | Lý do |
| --- | --- | --- |
| `GetLearnerReportUseCase` (kèm bẫy lỗi) | Cache | Tên, kỹ năng, band trong báo cáo |
| `GetReviewBasketUseCase` | Cache | Tên KP; lúc bắt đầu ôn kiểm tra lại |
| `ListSubmissionHistoryUseCase` | Cache | Tên KP trong lịch sử |
| `GetQuestsUseCase` | Cache | Tên KP của nhiệm vụ |
| `GetDiagnosticUseCase` | Cache | Tên KP của phiên chẩn đoán |
| `GetSuggestionsUseCase` (`describe` và `currentBand`) | Cache | Gợi ý; quyền mở bài do `PaywallGate` kiểm tra riêng trên bài ứng viên |
| `QuestService.weaknessTarget` | Cache | Chọn KP cho nhiệm vụ; đã có phương án dự phòng khi lỗi |
| `EssayGrader.knowledgePointCatalog` | Cache | Tên, mô tả KP khi chấm |
| `SpeakingGradingContextCapture` | Cache | Mô tả KP trong ngữ cảnh chấm |
| `ApplyHumanReviewResultUseCase.knowledgePointDetails` | Cache | Chi tiết KP trong phản hồi |
| `RefreshLearningTopicsUseCase` | **Mới** | Ghi lộ trình và catalog KP vào DB |
| `StartFreePracticeUseCase` (`topicOf`, `skillOf`) | **Mới** | `PaywallGate`, khóa đang mở, kỹ năng ghi vào bài luyện |
| `StartBasketReviewUseCase` | **Mới** | `PaywallGate` |
| `DiagnosticPreparation` (4 chỗ) | **Mới** | `DiagnosticFlow` ghi kỹ năng KP vào bài ôn |
| `GetLearnerTreeUseCase` | **Mới** | Trả trạng thái khóa và chặn khóa học đã đóng |
| `StaffInsightUseCase` | **Mới** | Nhân viên ít dùng, cần thấy dữ liệu vừa sửa |

Nếu khi làm phát hiện nơi "Cache" dùng dữ liệu cho quyền, giá hoặc cổng mở bài thì chuyển sang "Mới" và sửa bảng này.

## 7. Rủi ro

| Rủi ro | Cách xử lý |
| --- | --- |
| Dữ liệu cũ dùng cho quyền hoặc giá | Cache tùy chọn, bảng mục 6, test các nơi đọc mới |
| Redis đầy, ảnh hưởng đăng nhập và thu hồi token | Chỉ 2 khóa; đo kích thước; không đổi `maxmemory` hay chính sách evict |
| AOF production ghi lại payload mỗi lần nạp | Khoảng 153 MB/ngày cho khóa `base` khi có người dùng liên tục; tính cả khóa thứ hai ở phase 01 |
| Hai phiên bản code chạy cùng lúc | Phiên bản trong khóa và test chốt trường |
| Redis chậm kéo dài request | Timeout 200 ms, từ chối lệnh khi mất kết nối |
| Thiếu `REDIS_PASSWORD` làm Learning không khởi động | Render env trước khi deploy, có test production |
| Đổi constructor làm Spring không chọn được | Chỉ một constructor có `@Autowired`; chạy test khởi động context |

## 8. Tiêu chí hoàn thành

- Trúng cache thì các luồng "Cache" không gọi `topic-sequence`.
- Tắt Redis không làm hỏng luồng nào, Learning vẫn `UP`.
- Các luồng "Mới" thấy thay đổi của Content ngay.
- Toàn bộ test Learning đạt, test Redis chạy thật chứ không bị bỏ qua.
- Có số đo trước và sau trong `reports/`; tài liệu ở phase 05 đã cập nhật.

## 9. Quyết định đã chốt (2026-11-25)

1. Cây kiến thức đọc mới ở bản đầu; xem lại khi có số đo.
2. TTL 60 giây.
3. Redis được dùng làm cache; sửa `AGENTS.md` cùng PR với code.
4. Phải có kết quả phase 01 và được duyệt trước khi làm phase 02.
