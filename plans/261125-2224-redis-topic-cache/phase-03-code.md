# Phase 03. Port, cache Redis, chuyển nơi gọi

[Plan](plan.md). Đường dẫn dưới đây tính từ `services/learning-service/src/main/java/com/ieltspath/learning/`.

## File tạo mới

### `application/port/TopicCatalog.java`

```java
/** Topics for display only: may be up to the cache TTL old. Never decide access, price or a stored value from it. */
public interface TopicCatalog {
    List<LearningContentClient.Topic> topics(boolean includeErrorTraps);

    /** Reads Content every time. */
    static TopicCatalog direct(LearningContentClient content) { ... }
}
```

`direct` gọi `getTopicSequence()` khi `false` và `getTopicSequence(true)` khi `true`, đúng như code hiện nay. Test stub hai
method này riêng, nên không gộp thành `getTopicSequence(includeErrorTraps)`.

### `infrastructure/cache/RedisTopicCatalog.java`

`@Component`, triển khai `TopicCatalog`. Phụ thuộc: `LearningContentClient`, `StringRedisTemplate`, `ObjectMapper`,
`MeterRegistry`, `enabled` (`${learning.topic-cache.enabled:false}`), `ttl-seconds` (`${learning.topic-cache.ttl-seconds:60}`).

1. `enabled` là `false`: trả `TopicCatalog.direct(content).topics(...)`, không đụng Redis.
2. `GET` khóa. Lỗi Redis: metric `error`, sang bước 4.
3. Có giá trị: parse thành `List<Topic>`. Thành công: metric `hit`, trả về. Lỗi parse: metric `error`, sang bước 4.
   Không có giá trị: metric `miss`, sang bước 4.
4. Gọi Content. Lỗi của Content **ném nguyên vẹn**, để `CONTENT_UNAVAILABLE` và các nơi đang bắt lỗi không đổi.
5. Kết quả khác `null` và không rỗng: `SET` kèm TTL trong một lệnh. Lỗi ghi: log `warn`, vẫn trả kết quả.

- Chỉ bọc `try/catch` quanh lệnh Redis và parse JSON, không bọc lời gọi Content.
- Log chỉ ghi khóa và tên lớp lỗi; không ghi giá trị hay message có thể chứa thông tin kết nối.
- Metric: counter `learning.topic.cache`, tag `result` = `hit`, `miss`, `error`.
- Từ chối `ttl-seconds` nhỏ hơn 1 khi khởi tạo.
- Một constructor duy nhất. Nếu cần thêm constructor cho test thì đặt `@Autowired` ở constructor chính.

### `infrastructure/cache/TopicCacheRedisConfig.java`

Bean `LettuceClientConfigurationBuilderCustomizer` đặt `ClientOptions.disconnectedBehavior = REJECT_COMMANDS`. Khi dựng
`ClientOptions` mới phải giữ connect timeout 200 ms trong `SocketOptions`, vì có thể ghi đè giá trị Spring đã đặt.

## File sửa

| File | Thay đổi |
| --- | --- |
| `application/service/KnowledgePointDirectory.java` | Thêm `TopicCatalog`; `describe()` và `describe(boolean)` giữ nguyên (đọc mới); thêm `describeForDisplay()` và `describeForDisplay(boolean)` |
| `application/usecase/GetLearnerReportUseCase.java` (104-105) | `describeForDisplay(true)`, dự phòng `describeForDisplay()` |
| `application/usecase/GetReviewBasketUseCase.java` (93) | `describeForDisplay()` |
| `application/usecase/ListSubmissionHistoryUseCase.java` (57) | `describeForDisplay()` |
| `application/usecase/GetQuestsUseCase.java` (42) | `describeForDisplay()` |
| `application/usecase/GetDiagnosticUseCase.java` (31) | `describeForDisplay()` |
| `application/usecase/GetSuggestionsUseCase.java` (83, 120) | `describeForDisplay()` và `topicCatalog.topics(false)` |
| `application/service/QuestService.java` (151) | `topicCatalog.topics(false)` trong khối `try` đang có |
| `application/service/EssayGrader.java` (509) | `topicCatalog.topics(false)` trong khối `try` đang có |
| `application/service/SpeakingGradingContextCapture.java` (44) | `topicCatalog.topics(false)` trong khối `try` đang có |
| `application/usecase/ApplyHumanReviewResultUseCase.java` (297) | `topicCatalog.topics(false)` trong khối `try` đang có |

Số dòng theo `221f2efa`; tìm lại theo tên method khi làm.

### Constructor

- `EssayGrader` có ba constructor, được 13 chỗ trong 4 file test gọi. Thêm constructor 8 tham số có `TopicCatalog` và **chuyển
  `@Autowired` sang nó**; constructor 7 tham số gọi lại với `TopicCatalog.direct(content)`. Hai constructor ngắn giữ nguyên.
- `ApplyHumanReviewResultUseCase`: thêm tham số, sửa một chỗ trong `ApplyHumanReviewResultUseCaseTest`.
- `QuestService`, `SpeakingGradingContextCapture`, `GetSuggestionsUseCase`, `KnowledgePointDirectory`: test không tạo trực
  tiếp bằng `new`, chỉ thêm tham số.

## Không sửa

`RestLearningContentClient`, `LearningContentClient.getTopicSequence`, và sáu nơi "Mới" ở [plan mục 6](plan.md).
`StartFreePracticeUseCase.skillOf` và `DiagnosticPreparation` vẫn gọi `describe()`.

## Kiểm tra

```powershell
mvn -q -pl services/learning-service -am compile -DskipTests
mvn -q -pl services/learning-service -am -Dtest=ArchitectureTest,LearningServiceApplicationTests -Dsurefire.failIfNoSpecifiedTests=false test
```

Không cập nhật `archunit_store`.
