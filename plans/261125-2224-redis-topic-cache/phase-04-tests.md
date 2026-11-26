# Phase 04. Test tự động và kiểm tra thật

[Plan](plan.md). Test nằm ở `services/learning-service/src/test/java/com/ieltspath/learning/`.

## Test mới

### `infrastructure/cache/RedisTopicCatalogTest` (Testcontainers `redis:7-alpine`)

Theo mẫu `RedisMediaRateLimiterTest` của community-service; `LearningContentClient` là mock.

| Trường hợp | Kỳ vọng |
| --- | --- |
| Gọi hai lần | Content bị gọi một lần; kết quả bằng nhau |
| `base` và `error-traps` | Hai khóa, hai lần gọi, không lẫn dữ liệu |
| TTL | Lớn hơn 0, không quá 60 giây |
| Hết hạn (TTL 1 giây) | Gọi lại Content |
| Ghi rồi đọc | `Topic` có course, giá mở khóa, KP bẫy lỗi, `earliestLessonPosition`, trường null: bằng bản gốc |
| JSON hỏng trong Redis | Gọi Content, ghi đè bản đúng, metric `error` |
| Content ném lỗi | Lỗi giữ nguyên kiểu; không có khóa mới |
| Content trả rỗng hoặc `null` | Không có khóa mới |
| Dừng Redis sau khi đã kết nối | Trả dữ liệu từ Content, dưới 1 giây |

### `infrastructure/cache/RedisTopicCatalogUnitTest` (không cần Docker)

| Trường hợp | Kỳ vọng |
| --- | --- |
| `enabled = false` | Không tương tác với `StringRedisTemplate` |
| `GET` ném lỗi | Gọi Content, trả kết quả, metric `error` |
| `SET` ném lỗi | Vẫn trả kết quả |
| `ttl-seconds = 0` | Từ chối khởi tạo |

### `infrastructure/cache/TopicCacheShapeTest`

So tên và kiểu các record component của `Topic`, `Course`, `KnowledgePoint` với danh sách ghi trong test. Thông báo lỗi: tăng
phiên bản khóa cache rồi cập nhật danh sách.

### `application/service/KnowledgePointDirectoryTest`

`describe()` chỉ gọi `LearningContentClient`; `describeForDisplay()` chỉ gọi `TopicCatalog`; hai method cho cùng kết quả với
cùng dữ liệu.

### Nơi đọc mới

`RefreshLearningTopicsUseCase`, `StartFreePracticeUseCase`, `StartBasketReviewUseCase`, `DiagnosticPreparation`,
`GetLearnerTreeUseCase`, `StaffInsightUseCase` không phụ thuộc `TopicCatalog` và không gọi `describeForDisplay`. Kiểm bằng
ArchUnit trên bytecode, trong test riêng, để phủ cả hai điều kiện.

## Test phải sửa

| File | Thay đổi |
| --- | --- |
| `GetLearnerReportUseCaseTest` (65) | Stub `describeForDisplay()` thay cho `describe()` |
| `ListSubmissionHistoryUseCaseTest` (97, 110) | Như trên |
| `ApplyHumanReviewResultUseCaseTest` (95) | Thêm `TopicCatalog.direct(content)` |

Test context không có config-repo nên cache tắt và đi qua `direct`; stub `getTopicSequence` đang có vẫn dùng được.

## Lệnh

```powershell
mvn -q -pl services/learning-service -am test
mvn -q -pl infra/config-server test
mvn -q -pl infra/api-gateway test
python scripts/production/test-production.py
docker compose config --quiet
```

Báo rõ nếu test Testcontainers bị bỏ qua; như vậy chưa tính là đạt.

## Kiểm tra thật

`docker compose up -d --build`, dùng tài khoản demo.

| Bước | Kỳ vọng |
| --- | --- |
| Mở báo cáo hai lần | Metric `learning_topic_cache_total{result="hit"}` tăng |
| Đổi tên KP ở khu author, mở lại báo cáo | Tên mới hiện trong 60 giây |
| Đóng khóa học, mở cây kiến thức và bắt đầu luyện tự do | Áp dụng ngay |
| `docker compose stop redis`, mở báo cáo, giỏ ôn, gợi ý | Vẫn chạy; `/actuator/health` của Learning `UP` |
| `docker compose start redis` | Metric `hit` tăng trở lại, không cần restart Learning |
| `LEARNING_TOPIC_CACHE_ENABLED=false`, tạo lại container Learning | Không có khóa `cache:learning:*` mới |

Redis còn phục vụ đăng nhập, thu hồi token và rate limit, nên dừng Redis ảnh hưởng cả các phần đó. Đăng nhập trước khi dừng.

Đo lại như phase 01 và ghi `reports/measure-261125-2224-topic-sequence-after-report.md`.
