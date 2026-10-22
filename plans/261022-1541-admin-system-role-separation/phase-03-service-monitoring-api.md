# Phase 03: API trạng thái microservice (backend, api-gateway)

## Hiện trạng

- `infra/api-gateway`: Spring Cloud Gateway (WebFlux), có `spring-cloud-starter-netflix-eureka-client`,
  `spring-boot-starter-actuator`, `common-security`. Cấu hình route lấy từ config-server.
- Mọi service trong `services/` đều có actuator; mặc định lộ `/actuator/health`.
- `SecurityConfig` của gateway: `/actuator/**` GET mở; quyền theo claim `roles` của JWT đã xác thực.

## Endpoint mới

`GET /api/admin/system/services` (chỉ `ADMIN`, kiểm tra ở `SecurityConfig` của gateway bằng
`pathMatchers("/api/admin/system/**").hasRole("ADMIN")` theo đúng cách gateway đang chuyển `roles` thành authority).

Response:

```json
{
  "checkedAt": "2026-10-22T08:40:00Z",
  "services": [
    {
      "serviceId": "CONTENT-SERVICE",
      "status": "UP",
      "instances": [
        { "instanceId": "content-service:8082", "uri": "http://host:8082", "status": "UP", "responseMs": 34 }
      ]
    },
    { "serviceId": "COMMUNITY-SERVICE", "status": "DOWN", "instances": [] }
  ]
}
```

- Danh sách dịch vụ: hợp của (a) các service đang đăng ký trong Eureka (`ReactiveDiscoveryClient`) và (b) danh sách
  mong đợi cấu hình bằng property `app.monitoring.expected-services` (mặc định: user, access, assessment, content,
  learning, library, community, game, notification). Service mong đợi mà không có instance nào thì `DOWN`,
  `instances: []`.
- Với mỗi instance: gọi `GET {uri}/actuator/health` bằng `WebClient`, timeout 2 giây, gọi song song. Trạng thái lấy từ
  trường `status` (`UP`, `DOWN`, `OUT_OF_SERVICE`, `UNKNOWN`); lỗi kết nối hoặc hết giờ là `DOWN`. `responseMs` là
  thời gian phản hồi.
- `status` của service: `UP` nếu ít nhất một instance `UP`, ngược lại `DOWN`.
- Không lộ chi tiết health (DB, disk); chỉ trạng thái. Không log URI nội bộ ở mức INFO mỗi lần gọi.
- Gateway và config-server, eureka-server tự đưa vào danh sách với trạng thái lấy được (gateway luôn `UP` vì đang trả lời).

## Cấu trúc code

- `com.ieltspath.apigateway.monitoring`: `ServiceHealthController` (WebFlux `@RestController`), `ServiceHealthChecker`
  (logic gọi health, nhận `ReactiveDiscoveryClient` và `WebClient.Builder`), record response.
- Chỉ đọc; không có endpoint khởi động/dừng service.

## Contract

`backend/docs/contracts/system-monitoring-v1.md`: endpoint, response, quy tắc trạng thái, quyền.

## Test

- Unit `ServiceHealthChecker` với discovery giả và `MockWebServer` hoặc `ExchangeFunction` giả: instance UP, DOWN,
  timeout, service mong đợi không có instance, nhiều instance một UP một DOWN.
- `WebTestClient` cho security: không token `401`, CUSTOMER/CONTENT_AUTHOR `403`, ADMIN `200`.

## Tiêu chí hoàn thành

`mvn -q -pl infra/api-gateway -am test` xanh. Chạy thật: khi tắt một service, lần gọi kế tiếp báo service đó `DOWN`.
