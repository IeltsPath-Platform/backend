# Cache cây topic bằng Redis ở learning-service

Trạng thái: **đã duyệt 2026-11-25** (mục 9). Đang ở bước 1, chưa có code.

| Repo | Nhánh | Tách từ |
| --- | --- | --- |
| backend | `feat/redis-content-cache` | `main` tại `221f2efa` |

Không đổi API công khai, event, database hay frontend.

## 1. Mục tiêu

Learning gọi `GET /internal/learning-content/topic-sequence` của Content mỗi lần cần tên, kỹ năng, band của điểm kiến thức
(KP). Response là toàn bộ cây topic, giống nhau cho mọi người học, và Content chạy nhiều truy vấn để dựng.
Plan này lưu response vào Redis trong 60 giây cho các luồng **chỉ để hiển thị hoặc mô tả**.

Redis là phần phụ: Redis lỗi hoặc tắt thì Learning gọi thẳng Content như hiện nay.

### Ngoài phạm vi

- Mã lỗi và rubric: nếu làm thì dùng RAM, plan riêng. Lợi ích thấp vì chủ yếu đọc lúc chấm bài.
- Nội dung đề (`package-versions/{id}`): chỉ xem lại khi có số đo.
- Không cache: quyền Premium, khóa miễn phí/trả phí, điểm, quota, mastery, tiến độ, URL ký.
- Lỗi có sẵn: một số use case gọi HTTP khi giữ transaction hoặc khóa (`RefreshLearningTopicsUseCase`,
  `GetQuestsUseCase`, `GetLearnerTreeUseCase`...). Đây là việc riêng; không lấy cache làm cách sửa.

## 2. Hiện trạng (đã đọc code trên `main`)

- `GetTopicSequenceUseCase` (Content) chỉ nhận `includeErrorTraps`, không nhận user hay role.
- `RestLearningContentClient.getTopicSequence()` và `getTopicSequence(true)` là hai response khác nhau.
- `Topic` chứa cả dữ liệu dùng cho quyền và giá: `requiredFeatureKey`, `earliestLessonPosition`,
  `skillUnlockPoints`, `comboUnlockPoints`, `active`. Vì vậy không được cache ngầm cho mọi nơi gọi.
- Learning chưa có Redis. Năm module khác đã dùng `spring-boot-starter-data-redis`.
- Docker healthcheck đọc `/actuator/health`. Thêm starter Redis mà không cấu hình thì Redis chết sẽ làm Learning `DOWN`.
- Redis production (`docker-compose.prod.yml`, `infra/redis/start-production.sh`): `mem_limit: 192m`, bật AOF, không đặt
  `maxmemory`, dùng chung với rate limit, giới hạn đăng nhập, vé WebSocket và pub/sub.
- `scripts/production/render-env.py` sinh file env production và chọn service nào nhận biến Redis (dòng 101).

## 3. Thiết kế

### Cache là tùy chọn, mặc định đọc mới

`LearningContentClient.getTopicSequence(...)` giữ nguyên và luôn gọi Content. Thêm port riêng để nơi nào cần cache phải gọi
rõ ràng; chỗ bị bỏ sót vẫn đúng, chỉ là không nhanh hơn.

```text
use case hiển thị -> TopicCatalog (application/port)
                       `-- RedisTopicCatalog (infrastructure/cache)
                             |-- trúng  -> trả từ Redis
                             `-- trượt/lỗi -> LearningContentClient -> ghi Redis (bỏ qua nếu lỗi)
```

- `TopicCatalog.topics(boolean includeErrorTraps)`.
- `KnowledgePointDirectory`: giữ `describe(...)` đọc mới; thêm `describeForDisplay(...)` đi qua `TopicCatalog`.
- Khóa: `cache:learning:topic-sequence:v1:{base|error-traps}`. Giá trị: JSON qua `StringRedisTemplate` và `ObjectMapper`.
- TTL 60 giây, chỉ hết hạn theo TTL. Người soạn sửa nội dung thì phần hiển thị trễ tối đa 60 giây.
- Không lưu khi Content lỗi hoặc danh sách rỗng.
- JSON hỏng hoặc Redis lỗi: coi là trượt, log một dòng `warn` không kèm nội dung, gọi Content.
- Hai request cùng trượt có thể cùng gọi Content. Chấp nhận, giống `PlacementCatalog`; không thêm khóa phân tán.
- Đổi hình dạng `Topic`, `Course` hoặc `KnowledgePoint` thì phải tăng `v1`. Một test chốt danh sách trường để không quên.

### Redis lỗi không làm chậm request

- Connect timeout và command timeout 200 ms.
- Lettuce `disconnectedBehavior = REJECT_COMMANDS`, để mất kết nối thì lỗi ngay thay vì xếp hàng chờ timeout.
- `management.health.redis.enabled: false` cho Learning.
- `spring.data.redis.repositories.enabled: false`, vì Learning chỉ dùng JPA repository.
- Không tự viết circuit breaker. `ResilientCall` vẫn bọc lời gọi HTTP sang Content.

### Cấu hình

```yaml
learning:
  topic-cache:
    enabled: ${LEARNING_TOPIC_CACHE_ENABLED:true}
    ttl-seconds: 60
```

`enabled: false` thì `TopicCatalog` gọi thẳng Content. Đây là đường lùi không cần deploy lại code.

## 4. Nơi dùng cache và nơi đọc mới

| Nơi gọi | Cách đọc | Lý do |
| --- | --- | --- |
| `GetLearnerReportUseCase` | Cache | Tên, kỹ năng, band trong báo cáo |
| `GetReviewBasketUseCase` | Cache | Tên KP; lúc bắt đầu ôn kiểm tra lại |
| `ListSubmissionHistoryUseCase` | Cache | Tên KP trong lịch sử |
| `GetQuestsUseCase` | Cache | Tên KP của nhiệm vụ |
| `GetDiagnosticUseCase` | Cache | Tên KP của phiên chẩn đoán |
| `GetSuggestionsUseCase` (2 lần gọi) | Cache | Gợi ý; phần quyền do `PaywallGate` kiểm tra ứng viên riêng |
| `QuestService.weaknessTarget` | Cache | Chọn KP cho nhiệm vụ; lỗi Content vẫn có phương án dự phòng |
| `EssayGrader.knowledgePointCatalog` | Cache | Tên, mô tả KP khi chấm |
| `SpeakingGradingContextCapture` | Cache | Mô tả KP đưa vào ngữ cảnh chấm |
| `ApplyHumanReviewResultUseCase.knowledgePointDetails` | Cache | Chi tiết KP đưa vào phản hồi |
| `RefreshLearningTopicsUseCase` | **Mới** | Ghi lộ trình và catalog KP vào DB |
| `StartFreePracticeUseCase` | **Mới** | `PaywallGate`, khóa đang mở, kỹ năng ghi vào bài luyện |
| `StartBasketReviewUseCase` | **Mới** | `PaywallGate` |
| `DiagnosticPreparation` | **Mới** | Kỹ năng KP được ghi vào bài ôn chẩn đoán |
| `GetLearnerTreeUseCase` | **Mới** | Trả trạng thái khóa và chặn khóa học đã đóng |
| `StaffInsightUseCase` | **Mới** | Nhân viên ít dùng, cần thấy dữ liệu vừa sửa |

Trước khi đổi từng dòng "Cache", kiểm tra lại dữ liệu lấy ra không được dùng để quyết định quyền, giá hoặc cổng mở bài.
Nếu có thì chuyển sang đọc mới và sửa bảng này.

## 5. Các bước

### Bước 1. Đo trước khi sửa

1. Đo kích thước JSON của hai response với dữ liệu seed và thời gian Content trả về (p50, p95, tối thiểu 50 lần).
2. Ghi số lần gọi Content của: báo cáo, giỏ ôn, gợi ý, nhiệm vụ, lịch sử.
3. Lưu kết quả vào `plans/261125-2224-redis-topic-cache/reports/`.

**Điểm dừng:** báo lại trước khi làm tiếp nếu một response lớn hơn 1 MB (ảnh hưởng RAM và AOF của Redis), hoặc p95 của
Content dưới 20 ms (cache gần như không đáng làm).

### Bước 2. Hạ tầng

| File | Thay đổi |
| --- | --- |
| `services/learning-service/pom.xml` | Thêm `spring-boot-starter-data-redis` |
| `infra/config-server/config-repo/learning-service.yaml` | `spring.data.redis` (host, port, `password: ${REDIS_PASSWORD}`, timeout), tắt health và Redis repository, `learning.topic-cache` |
| `docker-compose.yml` | `REDIS_HOST`, `REDIS_PASSWORD` cho `learning-service`; không thêm `depends_on` redis vì Redis là phần phụ |
| `scripts/production/render-env.py` | Thêm `learning-service` vào nhóm nhận biến Redis |
| `scripts/production/test-production.py` | Kiểm tra env của Learning có biến Redis |

`REDIS_PASSWORD` không có giá trị dự phòng, đúng quy tắc secret. Hệ quả: **thiếu biến này thì Learning không khởi động**,
dù Redis chỉ là phần phụ. Cấu hình production phải được render lại trước khi deploy bản mới.

### Bước 3. Code

1. `application/port/TopicCatalog.java`.
2. `infrastructure/cache/RedisTopicCatalog.java` và cấu hình Lettuce.
3. `KnowledgePointDirectory.describeForDisplay(...)`.
4. Chuyển các nơi gọi ghi "Cache" ở mục 4; các nơi "Mới" không sửa.
5. Metric Micrometer `learning.topic.cache` với tag `result=hit|miss|error`. Không đưa id hay nội dung vào tag.

### Bước 4. Kiểm thử

| Trường hợp | Loại |
| --- | --- |
| Trượt rồi trúng: Content chỉ bị gọi một lần; hai khóa độc lập | Testcontainers Redis |
| Khóa có TTL không quá 60 giây | Testcontainers Redis |
| Ghi rồi đọc lại ra object bằng nhau, gồm KP bẫy lỗi và course có giá | Testcontainers Redis |
| Không có Redis: vẫn trả dữ liệu, không ném lỗi, không chờ quá timeout | Unit |
| Mất kết nối giữa chừng | Testcontainers Redis |
| JSON hỏng: gọi Content và ghi đè | Testcontainers Redis |
| Content lỗi hoặc trả rỗng: không lưu; lỗi giữ nguyên contract | Unit |
| `enabled: false`: không đụng Redis | Unit |
| Danh sách trường của `Topic`, `Course`, `KnowledgePoint` khớp phiên bản khóa | Unit |
| Các use case "Mới" không gọi `TopicCatalog` | Unit/ArchUnit |
| `/actuator/health` vẫn `UP` khi Redis tắt | Integration |

Cập nhật mock trong test của các use case được chuyển.

```powershell
mvn -q -pl services/learning-service -am test
mvn -q -pl infra/config-server test
python scripts/production/test-production.py
docker compose config --quiet
```

Chạy thật: mở báo cáo hai lần, xem metric `hit` tăng; `docker compose stop redis`, báo cáo vẫn mở được và Learning vẫn `UP`.

### Bước 5. Tài liệu

- `AGENTS.md`: dòng Redis ở §2 thêm cache; §3.9 thêm dòng "cache dữ liệu dùng chung" (tùy chọn, có TTL, Redis lỗi thì gọi
  nguồn, không cache quyền/tiền/quota).
- `docs/system-architecture.md` §9: khóa, TTL, nơi dùng.
- `docs/local-development.md`: `REDIS_PASSWORD` nay cần cho Learning; chạy IDE dùng
  `docker compose up -d postgres rabbitmq redis`.
- `docs/deployment.md`: render lại env trước khi deploy Learning.
- README của learning-service: khóa `learning.topic-cache`.

## 6. Rủi ro

| Rủi ro | Cách xử lý |
| --- | --- |
| Dữ liệu cũ dùng cho quyền hoặc giá | Cache tùy chọn, bảng mục 4, test các nơi đọc mới |
| Redis đầy, ảnh hưởng rate limit và đăng nhập | Chỉ 2 khóa; đo kích thước ở bước 1; không đổi `maxmemory` dùng chung trong plan này |
| AOF production ghi cả cache mỗi lần nạp | Tính dung lượng ghi mỗi ngày ở bước 1; nếu lớn thì tăng TTL hoặc nén, cần duyệt |
| Hai phiên bản code chạy cùng lúc đọc JSON khác nhau | Phiên bản trong khóa và test chốt trường |
| Redis chậm kéo dài request | Timeout 200 ms, từ chối lệnh khi mất kết nối |
| Thiếu `REDIS_PASSWORD` khi deploy | Render env và kiểm tra bằng test production trước |

## 7. Lùi lại

1. `LEARNING_TOPIC_CACHE_ENABLED=false` rồi restart Learning.
2. Nếu cần, revert PR. Không có migration; khóa Redis tự hết hạn sau 60 giây.

## 8. Tiêu chí hoàn thành

- Khi trúng cache, các luồng "Cache" không gọi `topic-sequence` của Content.
- Tắt Redis không làm hỏng luồng nào, và Learning vẫn `UP`.
- Các luồng "Mới" vẫn thấy thay đổi của Content ngay.
- Toàn bộ test Learning đạt; test Redis chạy thật với Docker, không bị bỏ qua.
- Có số đo trước và sau trong `reports/`.
- Tài liệu ở bước 5 đã cập nhật.

## 9. Quyết định đã chốt (2026-11-25)

1. **Cây kiến thức (`GetLearnerTreeUseCase`)**: đọc mới ở bản đầu; xem lại khi có số đo.
2. **TTL**: 60 giây.
3. **Redis làm cache**: đã duyệt; sửa `AGENTS.md` cùng PR với code.
4. **Thứ tự**: commit plan, chạy bước 1, báo kết quả đo trước khi code.
