# Duyệt gói A, B1–B4 (Codex) — 2026-11-01

Phạm vi: đọc báo cáo `plans/261031-2145-backend-reliability-patterns/reports/codex-package-{A,B1,B2,B3,B4}-report.md`, đối chiếu diff `f4685342..HEAD` với spec. Chỉ đọc, không sửa code, không commit. Không chạy lại Maven (Codex đang chạy); các kết luận "lỗi" dựa trên đọc code, số test đỏ lấy từ báo cáo Codex.

## Kết quả theo gói

| Gói | Codex báo | Claude duyệt | Ghi chú |
|---|---|---|---|
| A | DONE_WITH_CONCERNS | **Đạt** | Path auth đúng 5 đường dẫn; XFF duyệt phải sang trái, bỏ hop tin cậy, rơi về remote IP. Kiểm reset-password qua gateway còn để Claude làm tay |
| B1 | DONE_WITH_CONCERNS | **Đạt, có một điểm phạm vi** | `WorkItemSync` đã tách `loadFromContent()` (không khóa) và `apply()` (trong transaction với `pg_try_advisory_xact_lock`); job gọi đúng thứ tự. Báo cáo ghi "Deviations: none" nhưng B1 đã sửa 5 file plan (phase 1, 2, 5, 6, 8), spec chỉ nêu phase 1 và docs. Cần xem các sửa đó có phải chỉ là con trỏ không |
| B2 | DONE_WITH_CONCERNS | **Đạt, có một lỗ hổng xác minh** | Đủ 4 migration đúng số (access V8, community V9, assessment V21, learning V41); 9 test `/actuator/prometheus` có security thật; `DeadLetterMeterBinder` đúng hợp đồng `dead_letter_messages{queue}` và `NaN`. **Lỗ hổng:** `CorrelationListenerBrokerIntegrationTest` (notification, do chính commit `7b642c30` của B2 thêm) đang lỗi `SimpleMessageConverter does not accept CommunityEvent`. Báo cáo B2 không thấy vì Maven dừng ở learning, notification chưa chạy |
| B3 | **NEEDS_CONTEXT** | **Đúng là dừng, nguyên nhân là lỗi thiết kế thật** | Xem mục dưới |
| B4 | DONE_WITH_CONCERNS | **Đạt, chấp nhận lệch TDD** | 4 test mới xanh ngay trên code cũ (không có commit đỏ). Codex nêu rõ, không che. Notification chưa chạy test trong B4 |

## Lỗi thật: retry của notification không hoạt động (B3)

Chuỗi nhân quả, đã đối chiếu code:

1. `RetryTopology.of` (`shared/common-events/.../RetryTopology.java:29-33`): queue retry có `deadLetterExchange("")` và `deadLetterRoutingKey(<tên queue chính>)`.
2. Khi TTL hết, RabbitMQ đẩy message về queue chính qua default exchange, **routing key trở thành tên queue chính** (ví dụ `notification.subscription-granted.v1`), không còn là tên event.
3. Các listener notification kiểm `event.eventType().equals(routingKey)`: `RecordSubscriptionGrantedUseCase.java:33`, `RecordGradingRequestedUseCase.java:35`, `RecordGradingDueSoonUseCase.java:35`, `RecordGradingOverdueUseCase.java:34`, `RecordGradingCompletedUseCase.java:35`, `RecordCommunityEventUseCase.java:39`.
4. Lần retry đầu tiên bị coi là vi phạm hợp đồng, `AmqpRejectAndDontRequeueException` đẩy thẳng vào DLQ. Lỗi tạm thời (DB chập chờn) vì vậy **mất thông báo** thay vì được thử lại.

Ảnh hưởng: 5 listener retry của notification dùng `RetryTopology` (subscription granted + 4 grading). `CommunityEventConsumer` có retry riêng (`community.events.retry`, routing key `retry.<key>`), không thuộc lỗi này. Test `SubscriptionGrantedBrokerIntegrationTest.temporaryFailureIsRetriedThenSucceeds` đỏ đúng như Codex báo, và kết luận này khớp đọc code. Lỗi nằm trong thư viện dùng chung từ plan `261019-0025-common-events`, không do Codex viết.

Chưa kiểm: access và learning có dùng cùng `RetryTopology` cho listener điểm thưởng (`LearningPointsMessagingConfig.java:40,68`) và listener của chúng có kiểm routing key không. Cần kiểm trước khi kết luận phạm vi.

### Hướng sửa (cần bạn chọn)

- **A (khuyến nghị):** đổi retry queue để dead-letter về **exchange nguồn** với **routing key gốc** (`deadLetterExchange(sourceExchange.getName())`, `deadLetterRoutingKey(routingKey)`). Routing key giữ nguyên, kiểm hợp đồng vẫn đúng. Sửa một chỗ trong thư viện, phủ mọi listener retry. Cần test mới trong common-events và chạy lại test retry của notification.
- **B:** nới kiểm routing key, đọc event type từ header hoặc body. Làm yếu kiểm hợp đồng, không khuyến nghị.

Lý do chưa sửa: đổi `common-events` là thay đổi dùng chung, nằm ngoài phạm vi gói B3 mà Codex được giao, và Codex đang commit trên cùng nhánh.

## Lỗi xác minh còn lại (không phải do gói nào mới)

- **Cổng kiến trúc `BusinessControllerRoleConventionTest` đỏ trên HEAD.** `DeadLetterAdminController` (learning) dùng `hasRole('ADMIN')`, do commit `32d3886d` của phase 7 do Claude làm, và Claude chưa đưa đoạn vá cho Codex. Mọi lệnh `mvn verify` theo gói đều dừng ở learning, nên assessment, community, notification chưa được chạy đầy đủ trong bất kỳ gói nào. Vá đã kiểm (1/1 xanh) trong patch ở scratchpad. Cần đưa vào B4 hoặc cho Codex ghi chú này.
- **Test notification đang đỏ trên nhánh:** `CorrelationListenerBrokerIntegrationTest` (xem bảng B2) và `temporaryFailureIsRetriedThenSucceeds` (B3).

## Kiểm tra quy ước (stable artifact)

- Không tìm thấy mã plan, số phase, nhãn B1–B10 trong dòng code thêm mới (quét `f4685342..HEAD`, trừ thư mục plans).
- Commit message không chứa nhãn plan.

## Đề xuất thứ tự

1. Bạn chọn hướng sửa retry (A hoặc B). Tôi khuyến nghị A.
2. Nhắn Codex: đưa vá cổng kiến trúc vào B4, và báo lỗi `CorrelationListenerBrokerIntegrationTest`.
3. Codex làm tiếp gói C, D, E. Gói B3 mở lại sau khi có quyết định retry.

## Câu hỏi chưa giải quyết

- Bạn chọn hướng sửa retry nào (A hay B)?
- Các sửa file plan của B1 ngoài phase 1 có đúng phạm vi không?
