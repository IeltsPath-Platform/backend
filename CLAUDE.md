# CLAUDE.md - Bộ nhớ ngữ cảnh dự án

- Phiên bản: 1.0
- Cập nhật lần cuối: 2026-09-18
- Dự án: `IELTSPath` (Maven coordinates hiện tại: `com.group01:code-base:1.0-SNAPSHOT`)

## 1. TL;DR - Đọc trước trong 60 giây

`IELTSPath` là backend Java 21/Spring Cloud theo kiến trúc microservices. Hiện chỉ
có một bounded context nghiệp vụ đã triển khai: User. Hạ tầng gồm Config Server,
Eureka và API Gateway; PostgreSQL `user_db` thuộc riêng `user-service`.

Gateway WebFlux xác thực external JWT rồi ký internal JWT cho downstream servlet
service. `common-security` là thư viện kỹ thuật dùng chung để downstream xác
thực internal JWT và lấy `CurrentUser`. Chưa có frontend, messaging, cache,
object storage hoặc CI workflow được track.

## 2. Bức Tranh Toàn Hệ Thống

Đây là baseline để phát triển các business service độc lập trên hạ tầng Spring
Cloud chung. Khả năng nghiệp vụ xác minh được hiện tại là đăng ký, đăng nhập,
quản lý user/role, cấp access token và quản lý refresh token. Actor cụ thể ngoài
người dùng có role `ADMIN` và `LEARNER` chưa được đặc tả thêm từ repository.

Kiến trúc chủ đích: Domain-Driven Design, Clean Architecture và Microservices.
Implementation hiện tại tập trung chứng minh các boundary này trong
`user-service`; các service nghiệp vụ khác chưa tồn tại.

## 3. Bản Đồ Service / Module

| Service / module | Loại | Bounded context / trách nhiệm | Data ownership | Giao tiếp chính |
| --- | --- | --- | --- | --- |
| `infra/config-server` | Infrastructure service | Phục vụ cấu hình runtime từ native `config-repo`. | Không có business data. | Các service lấy config khi khởi động. |
| `infra/eureka-server` | Infrastructure service | Service registry. | Registry runtime. | Gateway và business service đăng ký/tìm instance. |
| `infra/api-gateway` | Infrastructure service | Ingress WebFlux, CORS, JWT validation, routing và internal JWT. | Không có business data. | Client, Eureka, `USER-SERVICE`. |
| `services/user-service` | Business service | User, role, authentication và refresh token. | PostgreSQL `user_db`. | Gateway, Config Server, Eureka. |
| `shared/common-security` | Shared library | Servlet security auto-configuration, internal JWT, canonical roles, current-user access. | Không có data. | Dependency của downstream service. |

## 4. Kiến Trúc Hệ Thống

```text
                    config-repo
                        |
                        v
                  Config Server
                    |         \
                    v          v
                Eureka       Gateway <--- Client
                  ^             |
                  |             | external JWT verified,
                  +--- User ----+ internal JWT forwarded
                       |
                       v
                    user_db
```

Gateway route hiện có chuyển `/auth/**` và `/api/users/**` đến `USER-SERVICE`
qua `lb://` và Eureka. Config Server chạy native mode; Docker Compose khởi động
theo thứ tự Config Server, Eureka, Gateway, User Service và `user-db`.

## 5. DDD Trong Dự Án

Bounded context hiện có là User. Các class trong `domain/aggregate` là `User`,
`Role` và `RefreshToken`; code không đánh dấu aggregate root riêng. Value object
gồm `Email`, `PhoneNumber`, `RoleName` và `UserStatus`. Repository abstraction
gồm `UserRepository`, `RoleRepository` và `RefreshTokenRepository`.

Business behavior hiện có nằm trong aggregate/value object (`User` cập nhật hồ
sơ, role, trạng thái; `RefreshToken` kiểm tra/revoke) và các use case điều phối
đăng ký, đăng nhập, refresh token, quản trị user. Không có domain event, domain
service, factory, CQRS query object hay application port trong `user-service`
hiện tại.

## 6. Clean Architecture Trong Dự Án

`user-service` được chia thành `api`, `application`, `domain`, `infrastructure`
và `config`:

```text
api ------------> application ------------> domain
                                      ^         ^
                                      |         |
                         infrastructure (JPA adapters)
```

- `api`: controllers, DTO, validation boundary, servlet logging và exception handler.
- `application`: `*UseCase`, command/result và transaction workflow.
- `domain`: model, validation, exception và repository contract.
- `infrastructure`: JPA entity/repository, MapStruct mapper và repository adapter.
- `config`: Spring bean/property wiring.

`UserRepositoryAdapter` maps `UserJpaEntity` through `UserMapper` rather than
exposing JPA types to the domain. Application use cases call domain repository
interfaces. Current implementation is pragmatic rather than framework-pure:
application classes use Spring `@Service`/`@Transactional`, and authentication
use cases depend on Spring security/configuration types.

## 7. Các Flow Quan Trọng

### Đăng nhập và cấp token

```text
Client -> Gateway public /auth/login -> User Service
       -> LoginUseCase -> UserRepository
       -> AuthTokenIssuer -> external JWT + hashed refresh token in user_db
       -> Client
```

`RegisterUseCase` chỉ cho đăng ký public với role `LEARNER`. `RefreshTokenUseCase`
hash token nhận vào, chỉ consume token còn hiệu lực một lần, kiểm tra user active,
rồi phát hành cặp token mới.

### Request protected qua Gateway

```text
Client external JWT -> Gateway validates issuer/UUID subject/roles
                    -> Gateway signs short-lived internal JWT
                    -> User Service validates internal JWT
                    -> CurrentUserProvider -> controller -> use case -> repository
```

Downstream không dùng raw `X-User-*` header làm danh tính. Test MVC xác minh
external token và các header giả mạo bị từ chối bởi `user-service`.

## 8. Security Model

`user-service` ký external HMAC JWT. Gateway là reactive resource server xác
thực token này, sau đó `InternalJwtGatewayFilter` thay authorization header cho
các downstream path đã cấu hình bằng internal HMAC JWT có thời gian sống ngắn.

`CommonSecurityAutoConfiguration` là auto-configuration cho servlet downstream:
nó tạo stateless `SecurityFilterChain`, decoder và converter sang authority.
Internal JWT phải có issuer đúng, subject UUID, expiry và role hợp lệ.
`CanonicalRoles` hiện có `ADMIN` và `LEARNER`. `CurrentUserProvider` đọc
identity đã xác thực từ `SecurityContext`.

Các tên cấu hình quan trọng là `EXTERNAL_JWT_SECRET` và
`GATEWAY_INTERNAL_JWT_SECRET`; giá trị không được lưu trong tài liệu này.

## 9. Data Và Persistence

Chỉ `user-service` có database business: PostgreSQL `user_db`. Flyway dùng
`src/main/resources/db/migration/V1__create_user_tables.sql` để tạo `users`,
`roles`, `user_roles` và `refresh_tokens`, cùng seed role `ADMIN`/`LEARNER`.

Persistence dùng Spring Data JPA. Domain model tách khỏi `*JpaEntity` và mapper
MapStruct. `UserJpaRepository` dùng `@EntityGraph(attributePaths = "roles")`
cho các read path hiện tại để nạp role chủ động. Transaction boundary hiện nằm
trên application use case bằng `@Transactional`. Integration test Testcontainers
kiểm tra Flyway và việc chỉ một request refresh concurrent có thể consume token.

## 10. Giao Tiếp Giữa Các Service

Giao tiếp runtime hiện có là HTTP qua Gateway; Gateway dùng Eureka để tìm instance
`USER-SERVICE`. Config Server phân phối YAML theo application/profile khi service
khởi động. Không có outbound HTTP client, gRPC, message broker, event, scheduler,
cache, object storage hay service-to-service database access trong source hiện tại.

## 11. Quyết Định Kiến Trúc Quan Trọng

Không có ADR chính thức trong repository. Các mục sau là observed architectural
decision, được suy ra từ code và tài liệu hiện tại.

### Gateway-signed internal JWT

- Quyết định: Gateway xác thực external JWT và downstream chỉ nhận internal JWT.
- Lý do: README và test ghi rõ không tin raw identity header; `common-security`
  tập trung validation và authority mapping.
- Trade-off: Gateway và downstream phải cùng tuân theo contract claim/issuer/role.
- Tham chiếu: `infra/api-gateway`, `shared/common-security`, test security của User Service.

### Config Server + Eureka

- Quyết định: dùng native Config Server và Eureka thay vì hard-code instance URL.
- Lý do: module README mô tả config tập trung và Gateway route `lb://USER-SERVICE`.
- Trade-off: startup và local runtime phụ thuộc thứ tự infrastructure service.
- Tham chiếu: `infra/config-server/config-repo`, `infra/eureka-server`, Gateway config.

### Domain model tách JPA model

- Quyết định: `User`/`Role` domain type tách khỏi `*JpaEntity`, nối qua mapper/adapter.
- Lý do: quan sát từ package và dependency hiện tại; không có ADR ghi lý do chi tiết.
- Trade-off: tăng mapper và model duplication để giữ JPA ngoài domain.
- Tham chiếu: `services/user-service/src/main/java/com/group01/user/infrastructure`.

### Docker build dùng hai kiểu Dockerfile

- Quyết định: service Spring thông thường dùng root `Dockerfile.spring-service` với
  `MODULE_PATH`; Config Server có Dockerfile riêng.
- Lý do: Config Server cần đóng gói thêm `config-repo`.
- Tham chiếu: `Dockerfile.spring-service`, `infra/config-server/Dockerfile`, commit `dac8b03`.

## 12. Pattern Đang Được Sử Dụng

| Pattern | Nơi dùng | Mục đích |
| --- | --- | --- |
| API Gateway | `infra/api-gateway` | Ingress, security và discovery-based routing. |
| Configuration Server | `infra/config-server` | Cấu hình runtime tập trung. |
| Service Discovery | `infra/eureka-server` và Eureka client | Tìm service bằng tên thay vì host/port cố định. |
| Repository Adapter | User infrastructure adapter | Che Spring Data JPA sau domain repository contract. |
| Aggregate / Value Object | User domain | Mô hình hóa state và validation nghiệp vụ. |
| Mapper | MapStruct mapper | Chuyển domain model và JPA entity. |
| Shared auto-configuration | `common-security` | Tránh lặp servlet downstream security. |
| Global exception handler | User API | Chuẩn hóa error response HTTP. |

## 13. Các Điểm Chưa Nhất Quán Đã Biết

- Application layer hiện import Spring qua `@Service`, `@Transactional`, security
  encoder và auth properties; điều này khác với Clean Architecture thuần.
- `services/user-service/README.md` ghi `PATCH` cho user status, nhưng
  `UserController` expose `PUT /api/users/{id}/status`; controller là behavior hiện tại.
- `GetAllUsersUseCase` dùng `findAll()` chưa phân trang; đây là giới hạn hiện tại
  cho dữ liệu có thể tăng.
- `graphify-out/graph.json` vẫn tham chiếu `services/user-service/.../SecurityConfig.java`
  đã bị xóa trong commit `a18a44e`; source tree là nguồn đúng cho security hiện tại.
- Template kiến trúc cho service mới có thể dùng `application/port` và
  `infrastructure/config`, nhưng `user-service` hiện dùng `domain/repository` và
  package `config` ở root. Đây là khác biệt giữa template tương lai và service hiện hữu.

## 14. Bài Học Và Lịch Sử Kỹ Thuật

### Gom downstream security vào `common-security`

- Bối cảnh: `a18a44e` xóa `SecurityConfig` cũ của User Service.
- Cách xử lý: thêm `CommonSecurityAutoConfiguration`, `CurrentUserProvider`,
  validator/authority JWT và canonical role dùng chung.
- Trạng thái: đang được User Service dùng; Gateway vẫn có security WebFlux riêng.
- Tham chiếu: commit `a18a44e`, `shared/common-security`.

### Đóng gói Docker theo Maven module

- Bối cảnh: Docker setup được refactor ở `dac8b03`.
- Cách xử lý: root Dockerfile nhận `MODULE_PATH`; Config Server giữ Dockerfile
  riêng để mang `config-repo` vào image.
- Trạng thái: Docker Compose dùng mô hình này cho các container hiện có.
- Tham chiếu: commit `dac8b03`, `docker-compose.yml`.

## 15. Trạng Thái Hiện Tại Của Dự Án

- Maven reactor có 5 module: 3 infrastructure service, `user-service` và
  `common-security`.
- User là business service duy nhất; database baseline chỉ có migration `V1`.
- Docker Compose mô tả full local stack và health check cho infrastructure/user DB.
- Security shared refactor và Docker refactor đều đã có trong lịch sử Git gần nhất.
- Graphify output tồn tại nhưng có ít nhất một đường dẫn security stale như mục 13.

## 16. Nội Dung Chưa Xác Định

- Production deployment target và secret-management strategy: Cần làm rõ.
- Frontend/client application và actor ngoài role hiện tại: Cần làm rõ.
- API versioning, pagination contract và compatibility policy: Cần làm rõ.
- Messaging/event strategy, cache, object storage và external integration: Cần làm rõ.
- CI policy, formatter, lint, coverage gate và API contract testing: Cần làm rõ.

## 17. Tài Liệu Và File Quan Trọng

| Path | Khi cần đọc |
| --- | --- |
| `AGENTS.md` | Quy tắc bắt buộc cho agent và template service mới. |
| `README.md` | Khởi động local/Docker, flow tổng quan và cách thêm service. |
| `infra/README.md` cùng README từng infra module | Vai trò và thứ tự khởi động hạ tầng. |
| `services/user-service/README.md` | Capability User, route chính và security boundary. |
| `infra/config-server/config-repo/` | Cấu hình runtime tập trung theo service. |
| `services/user-service/src/main/resources/db/migration/` | Flyway schema của User Service. |
| `shared/common-security/` | Internal JWT, role và servlet downstream security. |
| `docker-compose.yml` | Thành phần local stack, dependency và health check. |
| `graphify-out/graph.json` | Graph hỗ trợ khám phá source; kiểm tra freshness trước khi tin hoàn toàn. |

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
