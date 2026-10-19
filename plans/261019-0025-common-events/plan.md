# Module shared `common-events`

Trạng thái: **hoàn tất (2026-10-19).** Giai đoạn 1–4 đã commit trên `feat/common-events`; kiểm tra cuối đạt, xem mục "Kết quả".
Người duyệt đã chọn làm cả 3 giai đoạn, có cả community và notification, và cho phép `shared/` chứa record payload và
`MessageRetry` (phụ thuộc `spring-amqp`). Nền: PR 79 đã merge vào `main` (commit `15057e74`), làm từ `main` mới nhất.

## Mục tiêu

Hiện mỗi event qua RabbitMQ có hai đầu, và mỗi đầu **tự gõ lại** tên exchange, routing key, tên header và hình dạng payload.
Hai bản lệch nhau thì broker không báo lỗi (tin rơi mất, hoặc consumer đọc thiếu field). Đã có ca thật: access và learning
phải thêm hai `PointsGrantTopologyContractTest` chỉ để canh hai bản chép khớp nhau, và hai `MessageRetry` giống nhau từng dòng.

`shared/common-events` là một thư viện Maven nhỏ, một nơi duy nhất giữ **phần dùng chung của hợp đồng event**: hằng số
messaging, record payload của event, và một `MessageRetry` + dựng topology retry. Producer và consumer cùng import nó.
Nó **không** thay `docs/contracts/*.md`: tài liệu mô tả ý nghĩa, module là bản mã hóa của tên và hình dạng.

## Quy tắc bắt buộc cho người làm (Codex)

1. **Giá trị chuỗi giữ nguyên từng ký tự.** Queue/exchange đã tồn tại trên broker dev và prod; đổi một chữ là
   `PRECONDITION_FAILED` hoặc tin đi sai chỗ. Đây là việc *chuyển chỗ khai báo*, không đổi hành vi, JSON hay tên queue.
2. **Chỉ đưa vào module thứ mà CẢ producer lẫn consumer cùng dùng.** Queue/DLQ/retry queue chỉ một bên sở hữu thì ở lại service đó.
3. Không đưa vào module: entity, use case, repository, logic xử lý hay kiểm tra nghiệp vụ (AGENTS.md §3.5: `shared/` chỉ chứa
   technical concern). Record payload là dữ liệu thuần, **không có hành vi** (ngoại lệ duy nhất: factory tĩnh của
   `PointsGrantSettled`, vì đang tồn tại ở access và là phần của hợp đồng).
4. **Không thêm dependency vào module ngoài danh sách trong từng giai đoạn.** Module không phụ thuộc service nào, Spring Web, JPA.
5. Property override đang có (`assessment.messaging.exchange`, `community.messaging.exchange`...) **giữ lại**; chỉ đổi giá trị
   mặc định từ literal sang hằng số của `common-events`.
6. Mỗi giai đoạn là các commit riêng, test xanh trước khi sang giai đoạn sau. Không đặt mã plan, số phase hay nhắc AI/agent
   trong commit, comment hay tên test (AGENTS.md §6.8). Commit theo conventional commits, thể hiện hành vi.
7. Không sửa Docker, config tập trung hay shared security như side effect. `Dockerfile.spring-service` **không cần sửa**: nó
   `COPY . .` rồi `mvn ... -pl <module> -am`, nên module mới tự được kéo vào.
8. Không ghi "đã chạy" cho check chưa chạy (AGENTS.md §6.7). Test Testcontainers cần Docker; nếu bị skip phải nói rõ.

## Thứ tự và phụ thuộc

| # | Giai đoạn | Nội dung | Commit |
| --- | --- | --- | --- |
| 1 | [Khung module và hằng số](./phase-01-module-and-constants.md) | Tạo module, chuyển hằng số của 3 cặp service (points, assessment, community) | `feat(events)` + `refactor` theo service |
| 2 | [Record payload](./phase-02-event-payloads.md) | `PointsGrantRequested/Settled`, `AssessmentCompletedV2`, `CommunityEvent` | theo từng event |
| 3 | [`MessageRetry` và topology retry](./phase-03-messaging-helper.md) | Một `MessageRetry`, một chỗ dựng topology cho hai queue grant | 1 commit |
| 4 | [Tài liệu và dọn dẹp](./phase-04-docs-and-cleanup.md) | AGENTS.md, system-architecture, contracts, grep kiểm tra | 1 commit |

1 → 2 → 3 → 4. Mỗi giai đoạn có giá trị riêng, dừng được. Giai đoạn 3 cần giai đoạn 1 (dùng `MessageHeaders`).

## Cái nào ở lại trong service (KHÔNG chuyển)

Ghi ra để người làm không "tiện tay" gộp thêm:

- Tên queue, queue retry, DLQ, DLX, retry exchange (`access.points-grant*`, `learning.points-grant-settled*`,
  `learning.assessment-completed.*`, `community.notifications.*`...): chỉ một service khai báo và sở hữu.
- Topology của `learning.assessment-completed.v2` (retry exchange + DLX riêng, header `x-learning-failure`) và của
  `community.notifications.v1` (header `community-retry-count`, retry theo từng routing key): hình dạng khác với retry của points,
  **không** dùng `RetryTopology`/`MessageRetry` ở giai đoạn 3. Giữ nguyên cơ chế của chúng.
- Parser `AssessmentCompletedParser` của learning (quy tắc hợp đồng, tolerant với field thừa) và validate payload grant của access
  (`LearningPointsGrantListener.parse`): ở lại, vì là logic kiểm tra phía consumer.
- `AssessmentCompletedEventFactory` của assessment (dựng event từ domain): ở lại, chỉ đổi import record.
- Migration SQL của notification (`V3__add_notification_preferences.sql`) chứa literal tên event: **không đụng** (migration đã chạy).

## Kiểm tra toàn bộ (cuối mỗi giai đoạn, và lần cuối trước khi báo xong)

```
mvn -q -pl shared/common-events test
mvn -q -pl services/access-service,services/learning-service,services/assessment-service,services/community-service,services/notification-service -am test
mvn -B -ntp verify          # cả reactor, trước khi mở PR
docker compose config --quiet
```

- Test Testcontainers với RabbitMQ thật **phải chạy, không skip**: `PointsGrantDeadLetterBrokerTest` (access),
  `PointsGrantSettledDeadLetterBrokerTest`, `PointsGrantUnroutedBrokerTest`, `PointsGrantBrokerRoundTripTest` (learning),
  `CommunityOutboxRelayIntegrationTest`, `CommunityEventConsumerIntegrationTest`.
- Nếu IDE đang chạy, build trong một bản copy ngoài workspace hoặc dừng IDE (IDE ghi class vào `target/` gây
  `ClassFormatError`/"cannot be converted").
- Đếm test trước/sau cho từng module và báo số liệu.

## Tiêu chí hoàn thành

- Grep trên `services/*/src/main` (lệnh ở giai đoạn 4) không còn literal `learning.points`, `points.grant.requested`,
  `points.grant.settled`, `assessment.events`, `assessment.completed.v2`, `community.events`, `community.<sự-kiện>.v1`,
  `x-retry-count` ngoài `common-events` (và ngoài các ngoại lệ liệt kê ở trên).
- Chỉ còn **một** `MessageRetry`. Hai `PointsGrantTopologyContractTest` đã bị thay bằng test ghim giá trị trong `common-events`.
- Mọi test cũ xanh; số test không giảm trừ các test được thay thế có chủ đích (ghi rõ ở từng giai đoạn).
- `mvn -B -ntp verify` xanh toàn reactor; `docker compose config --quiet` qua.

## Rủi ro

| Rủi ro | Cách chặn |
| --- | --- |
| Sửa nhầm một ký tự chuỗi, hỏng broker | Test ghim từng giá trị bằng literal trong `common-events`; test broker thật vẫn chạy |
| Đổi hình dạng JSON khi chuyển record | Test serialize đối chiếu JSON mẫu trong `docs/contracts`; consumer-side test parse record đã serialize |
| Module phình to | Quy tắc số 2 và 3 ở trên; review từng PR theo quy tắc này |
| Ghép chặt phiên bản (mọi service cùng reactor) | Chấp nhận, repo đã làm vậy với `common-security`; hợp đồng `*-vN` vẫn là lan can tương thích |
| `spring-amqp` kéo vào `shared/` | Chỉ `spring-amqp` (scope `compile` trong `common-events`), không `spring-boot-starter-amqp` |

## Kết quả

- Giai đoạn 1–3: commit `8d5f91bb` … `8a7121e8` (module, payload, `MessageRetry`/`RetryTopology` cho access và learning). Giai đoạn 3 còn commit `0eb2dc07` (learning) và `6e090d71`.
- Giai đoạn 4: tài liệu cập nhật (`AGENTS.md`, `CLAUDE.md`, `README.md`, `docs/system-architecture.md`, ba file `docs/contracts/`), grep 4.2 sạch.
- Quy tắc 2 được giữ đúng: tên retry exchange, DLX và DLQ của Notification **ở lại service** (không đưa vào `CommunityMessaging`). Một lần Codex đưa chúng vào module đã được revert.
- Kiểm tra cuối: `mvn -B -ntp verify` cả reactor trên bản sao sạch của HEAD (`git worktree` ngoài thư mục IDE), tất cả module xanh, không có test fail hoặc skip. Các test broker thật (`PointsGrantDeadLetterBrokerTest`, `PointsGrantSettledDeadLetterBrokerTest`, `PointsGrantUnroutedBrokerTest`, `PointsGrantBrokerRoundTripTest`, `CommunityOutboxRelayIntegrationTest`, `CommunityEventConsumerIntegrationTest`) chạy với Docker thật.
- `docker compose config --quiet` đạt.
- Lưu ý: chạy trực tiếp trong thư mục đang mở bằng IDE làm lỗi `ClassFormatError` và `Flexible Constructor Bodies` (ECJ ghi class vào `target/`). Kiểm tra cuối phải chạy ở bản sao sạch.

## Chưa làm

- Chưa push `feat/common-events` lên remote.

