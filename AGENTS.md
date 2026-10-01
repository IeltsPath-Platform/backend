# AGENTS.md - Quy tắc bắt buộc cho AI Agent

- Phiên bản: 2.1
- Cập nhật lần cuối: 2026-10-02; dữ kiện pipeline đã kiểm với code Java trên nhánh `feat/lesson-pipeline-docs`
- Dự án: `IELTSPath` (Maven coordinates: `com.group01:code-base:1.0-SNAPSHOT`)
- Kiến trúc chi tiết (sơ đồ, flow, data ownership, quyết định): [`docs/system-architecture.md`](docs/system-architecture.md)

**Thứ tự ưu tiên:** `.sdd/global/constitution.md` (invariant cấp dự án, LOCKED) → file này (quy tắc vận hành cho agent;
các `CLAUDE.md` chỉ bổ sung ngữ cảnh chạy, không đặt quy tắc riêng) → tài liệu mô tả. `.sdd/global/system-architecture.md` và `.sdd/constraints/global.md` là baseline 2026-09-18, lỗi thời về
messaging, dịch vụ học (AI Learning Python cũ đã bị xóa) và danh sách service; khi lệch về dữ kiện thì code, file này và `docs/system-architecture.md` đúng.

## 1. Tổng quan dự án

`IELTSPath` là backend microservices cho nền tảng học IELTS. Repository không phải đặc tả sản phẩm hoàn chỉnh; không
suy diễn khả năng nghiệp vụ ngoài các module dưới đây.

| Module | Trách nhiệm |
| --- | --- |
| `infra/config-server`, `infra/eureka-server` | Cấu hình tập trung (`config-repo/`), service registry. |
| `infra/api-gateway` | Ingress WebFlux: xác thực external JWT, ký internal JWT, routing, CORS. |
| `shared/common-security` | Thư viện security servlet dùng chung (internal JWT, `CanonicalRoles`, `CurrentUserProvider`); không deploy độc lập. |
| `services/user-service` | Tài khoản, role, auth token, hồ sơ học viên, learning goal, activity và streak tại `/api/learning-support/{activities,streak}`. |
| `services/content-service` | Curriculum: topic, knowledge point, câu hỏi, gói nội dung, asset; kiểm topic cho library. |
| `services/library-service` | Cổng 8081, `library_db` (Compose host 5437): catalog từ vựng/video và thư viện cá nhân; Gateway chuyển `/api/content/{videos,vocabulary}`, `/api/content/admin/vocabulary` và năm nhóm `/api/learning-support` tương ứng tới đây. |
| `services/assessment-service` | Làm bài, chấm, kết quả; phát `AssessmentCompleted.v2`. |
| `services/access-service` | Gói, subscription, activation key, điểm. |
| `services/game-service` | Phòng game, trận, phiên chơi, WebSocket. |
| `services/community-service` | Bài viết, bình luận, reaction, kiểm duyệt. |
| `services/notification-service` | Chưa triển khai (khung package). |
| `services/learning-service` | Cổng 8086, `learning_db` (Compose host 5436): thứ tự topic, bài học, nộp bài, cổng mở bài, mastery theo KP, bài ôn, giao mã đề cuối, consumer `AssessmentCompleted.v2`. Route `/api/learning/**`. |
| `third_party/deeptutor` | Bản clone chỉ để đọc (nguồn của công thức mastery đã port). **Không phải dependency.** |

Mỗi business service sở hữu một database PostgreSQL riêng. Gateway chuyển hai nhóm activity/streak tới user-service;
không còn route tổng quát `/api/learning-support/**`.

## 2. Tech Stack - Bắt buộc tuân thủ

Chỉ dùng công nghệ đã có trong repository. Không thêm framework, thư viện lớn, database, công nghệ messaging, thành
phần hạ tầng hoặc architectural pattern mới nếu chưa được phê duyệt rõ ràng.

| Khu vực | Công nghệ đã được xác minh |
| --- | --- |
| Java build | Java 21; Maven multi-module (13 module trong reactor: 3 infra, 1 shared, 9 business); Maven Compiler Plugin 3.14.0; Surefire 3.5.3 ở module đã cấu hình. |
| Java framework | Spring Boot 3.5.14; Spring Cloud 2025.0.0 (Config, Netflix Eureka, Gateway Server WebFlux + Reactor). |
| Persistence (Java) | Spring Data JPA, Hibernate, PostgreSQL JDBC, Flyway (chạy khi service khởi động). |
| Bảo mật | Spring Security, OAuth2 Resource Server, Nimbus JWT, HMAC-SHA256 JWT, BCrypt. |
| Messaging | RabbitMQ 3.13; Spring AMQP (`spring-boot-starter-amqp`): assessment phát, learning-service nhận. |
| Realtime | Spring WebSocket (game-service). |
| Shared code | `common-security` auto-configuration; MapStruct 1.6.3; Lombok 1.18.46. |
| Kiểm thử | JUnit Jupiter, Spring Boot Test, Mockito, Spring Security Test, Testcontainers PostgreSQL. |
| Container | Docker Compose, `postgres:15-alpine`, `rabbitmq:3.13-management-alpine`, Eclipse Temurin 21. |

Repository **chưa có**: frontend, cache, object storage, file OpenAPI được track, CI
workflow, formatter, linter, coverage gate. Bổ sung các thành phần này là quyết định thiết kế, không phải mở rộng mặc định.

## 3. Nguyên tắc kiến trúc

Dự án dùng DDD, Clean Architecture và Microservices. Đây là ràng buộc bắt buộc, kể cả khi code hiện tại có điểm chưa
nhất quán từ trước (xem §7).

### 3.1 Layer và hướng phụ thuộc (service Java)

```text
api -> application -> domain
infrastructure -> domain (và application/port nếu có)
config -> framework wiring
```

- `domain`: aggregate, entity, value object, domain exception, repository contract.
- `application`: command/result (query khi cần), `*UseCase`; phụ thuộc repository contract hoặc `application/port`, không
  phụ thuộc JPA repository hay adapter.
- `infrastructure`: JPA entity, Spring Data repository, MapStruct mapper, repository adapter, HTTP client, messaging.
- `api`: controller, DTO, exception handler, filter; controller gọi use case và map sang response.
- Code mới trong `domain` KHÔNG ĐƯỢC import `api`, `application`, `infrastructure`, Spring, JPA, HTTP hoặc class của gateway.
- Code mới trong `application` KHÔNG ĐƯỢC import `api` hoặc `infrastructure`.
- Infrastructure triển khai contract hướng vào trong; KHÔNG chứa business rule.
- user-service dùng `domain/repository` làm persistence contract, không có `port`; assessment và game có `application/port`
  cho client/outbox/ticket. Giữ convention của service đang sửa.

### 3.1.1 Template bắt buộc cho business service Java mới

```text
src/main/java/com/group01/<service>
|-- domain         aggregate, entity, vo, event, exception, repository
|-- application    command, query, result, usecase, port, exception
|-- api            controller, dto
`-- infrastructure persistence, client, messaging, scheduler, config
```

- Khi cần service ngoài, broker, storage hoặc clock: định nghĩa `application/port`, adapter ở infrastructure.
- Tách domain aggregate khỏi JPA entity. Mỗi service mới sở hữu database và domain model riêng.
- Không tạo package `port`, `client`, `messaging`, `scheduler`, CQRS query hoặc domain event rỗng; chỉ thêm khi use case cần.

### 3.2 DDD building block

- Business validation và state transition nằm trong aggregate/value object hoặc use case phù hợp.
- Không tự tạo domain event, factory, CQRS query object hay domain service chỉ để cấu trúc trông đầy đủ/đối xứng.

### 3.3 Presentation, validation, error handling và logging

- Mapping trong controller (hoặc FastAPI router) là nguồn tham chiếu chính thức của API; contract nằm ở `docs/contracts/`.
- Request DTO dùng Bean Validation + `@Valid`; domain vẫn tự bảo vệ invariant.
- Mở rộng `GlobalExceptionHandler` → `ErrorResponse` của service thay vì trả error body tự phát.
- Giữ và forward `X-Correlation-Id`. Không log credential, token, password, nội dung hội thoại, prompt hay API key.
- Lấy danh tính bằng `CurrentUserProvider` (`common-security`); không tin header `X-User-*` do client gửi.

### 3.4 Ranh giới bảo mật

- `user-service` phát hành external HMAC JWT; refresh token chỉ lưu dạng hash.
- `api-gateway` xác thực external JWT, route mọi service qua Eureka (`lb://`) và thay Authorization bằng internal HMAC JWT sống ngắn cho các path trong `internal-jwt-paths`. Gateway dùng `SecurityWebFilterChain` riêng; không áp servlet auto-configuration cho Gateway.
- Service servlet dùng `CommonSecurityAutoConfiguration` để xác thực internal token (issuer đúng, subject UUID, có expiry,
  role thuộc `CanonicalRoles.ALL`: `ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF`). Claim và role
  constant quản lý tập trung trong `common-security`.
- `EXTERNAL_JWT_SECRET` và `GATEWAY_INTERNAL_JWT_SECRET` tách biệt, Base64, ≥ 32 byte. Không bao giờ chép giá trị thật vào
  code, tài liệu, test, log hoặc commit.

### 3.5 Ranh giới microservice

- Mỗi service sở hữu bounded context và database; không đọc/ghi database hay dùng chung JPA entity của service khác.
- Gọi đồng bộ service-to-service bằng HTTP tới endpoint nội bộ hoặc public của service đích, kèm bearer của request và
  `X-Correlation-Id` (như assessment → content, library → content, game → library/content). learning-service → content cũng gửi cả hai.
- Bất đồng bộ qua transactional outbox + RabbitMQ; event có version trong tên và contract ở `docs/contracts/`. Consumer
  phải idempotent và có retry/dead-letter (xem consumer của learning-service).
- `shared/` chỉ chứa technical concern dùng chung; không chuyển entity hay use case nghiệp vụ vào đó.
- Chưa có API version prefix. Giữ path tương thích; đánh giá mọi consumer trước khi đổi API, event hoặc JWT claim contract.
- Gateway chỉ chứa routing, security, CORS, filter, observability; KHÔNG chứa business logic.

### 3.6 Configuration và runtime

- Bootstrap setting ở `src/main/resources/application.y(a)ml` của module; runtime setting ở
  `infra/config-server/config-repo/<service>.yaml` (+ `application.yaml` dùng chung). Không lặp setting Eureka client toàn cục.
- Gateway và mọi service Java nghiệp vụ import `.env` ở root (`optional:file:../../.env[.properties]`; config-server và
  eureka-server thì không); compose cũng nội suy file này. `.env` không commit. Compose yêu cầu `LIBRARY_DB_PASSWORD` kể cả khi chỉ bật một phần stack;
  community-service mặc định DB local 5432, dùng DB compose (5434) thì đặt `COMMUNITY_DB_URL`.
- Chạy local: service Java trên host (Config Server → Eureka → Gateway → business service); compose chạy DB của
  library/community/game/learning và RabbitMQ. Library dùng `library_db` qua host 5437.
  Game chọn snapshot `VOCABULARY` từ library và `GRAMMAR` từ content; `LIBRARY_SERVICE_URL` mặc định
  `http://localhost:8081` trong config-repo. Chi tiết: `README.md` §6, `docs/system-architecture.md` §7.
- `Dockerfile.spring-service` build module Maven theo `MODULE_PATH`; Config Server có Dockerfile riêng.

### 3.7 Hiệu năng persistence, N+1 và độ phức tạp

- Giữ fetch plan tường minh theo nhu cầu response (ví dụ `@EntityGraph(attributePaths = "roles")` ở `UserJpaRepository`);
  không đổi toàn cục sang `EAGER` để che lazy loading hay N+1.
- Trước khi thêm endpoint đọc collection/relation, xác định dữ liệu response cần và tạo query có fetch plan/projection tương ứng.
- TUYỆT ĐỐI KHÔNG gọi repository, external client hoặc lazy relation trong vòng lặp; gom key, query theo tập, ghép bằng `Map`/`Set`.
- `findAll()` không phân trang là baseline cũ; endpoint danh sách mới phải phân trang/giới hạn, filter/sort ở database.
- Request path với dữ liệu biến thiên: ưu tiên O(n)/O(n log n); O(n^2) chỉ khi cận nhỏ được chứng minh và giải thích gần code.
- Query phức tạp: thêm/cập nhật integration test Testcontainers và review SQL. Chưa có query-count gate tự động.

### 3.8 Learning Service

- Không còn path DeepTutor: bằng chứng học lưu theo user ở `kp_evidence`; mastery của KP tính khi đọc bằng
  `MasteryCalculator` (port `compute_mastery` của DeepTutor v1.6.9, Apache-2.0; giữ comment ghi nguồn và giá trị test gốc).
- Thứ tự topic theo user từ Content `topic-sequence`, không LLM, không goal/band. `LessonAccessGate` và use case trong
  `services/learning-service/src/main/java/com/group01/learning/` quyết định cổng bài; trạng thái topic suy ra khi đọc.
- Mọi lượt ghi của một học viên chạy trong một `@Transactional` mở đầu bằng `pg_advisory_xact_lock` theo user.
- Bằng chứng bài học chỉ ghi ở lần nộp đầu của mỗi khối; bài ôn ghi mỗi set (nộp một lần); kết quả thi ghi theo
  `(attempt_id, result_version)`, chấm lại thì thay bằng chứng của version cũ.
- Học viên không bao giờ nhận `answerSpec`, `explanation` trước khi đạt, `chartFacts`, hay transcript trước khi đạt.
- Consumer không gọi HTTP; ACK sau khi transaction commit; vi phạm contract hoặc hết lượt thử → DLQ.
- Tác vụ LLM mới cho học viên (chấm Writing) phải có hạn mức theo ngày (plan 0737).
## 4. Quy tắc đặt tên file và cấu trúc dự án

```text
shared/common-security/                 thư viện security dùng chung
infra/{api-gateway,config-server,eureka-server}/
infra/config-server/config-repo/        YAML runtime tập trung
services/<name>-service/                service Java: src/main/java/com/group01/<package>/{api,application,domain,infrastructure}
                                        (package bỏ gạch nối, ví dụ library; user-service có thêm config/)
                                        + src/main/resources/db/migration/
docs/contracts/                         contract HTTP/SSE/event
docker-compose.yml                      stack local (một phần)
```

Khi không có convention cụ thể, theo code lân cận trong cùng module.

| Thành phần | Convention |
| --- | --- |
| Java package / type | `com.group01.<module>` viết thường; type PascalCase, một public type mỗi file. |
| Aggregate, VO | `domain/aggregate/<Name>.java`, `domain/vo/<Name>.java` (record hoặc enum). |
| Use case | `application/usecase/<Verb><Noun>UseCase.java`, `application/command/...Command.java`, `application/result/...Result.java`. |
| Repository | `domain/repository/<Name>Repository.java`; adapter `<Name>RepositoryAdapter` ở `infrastructure/adapter` hoặc `infrastructure/persistence` theo service. |
| JPA | `*JpaEntity`, `*JpaRepository`, `*Mapper`, `*Specifications` trong `infrastructure/persistence`. |
| HTTP API | `*Controller`, `*Request`, `*Response`, `GlobalExceptionHandler`. |
| Spring config | `*Config`, `*Properties`, `*AutoConfiguration`. |
| Test Java | Mirror package dưới `src/test/java`, lớp `*Test`. |
| Migration | `V{number}__{description}.sql` (Flyway), version tăng dần. |
| Service/artifact | kebab-case, ví dụ `user-service`. |

## 5. Các pattern bị cấm

### Kiến trúc

- TUYỆT ĐỐI KHÔNG đặt core business rule, persistence access hoặc cross-service call trong controller hoặc gateway filter.
- TUYỆT ĐỐI KHÔNG để domain phụ thuộc HTTP, Spring, JPA, adapter, Gateway hoặc service khác.
- TUYỆT ĐỐI KHÔNG bỏ qua use case để gọi thẳng JPA repository từ `api`.
- TUYỆT ĐỐI KHÔNG tạo entity, table hoặc repository dùng chung giữa bounded context; không truy cập database service khác.
- TUYỆT ĐỐI KHÔNG đổi architecture boundary, public route, event contract, JWT claim hoặc canonical role khi chưa được duyệt.
- TUYỆT ĐỐI KHÔNG đưa `third_party/` vào build, Dockerfile, compose hay classpath của service. Comment ghi nguồn của code
  port (`MasteryCalculator`) là được phép và phải giữ.

### Bảo mật

- TUYỆT ĐỐI KHÔNG commit, hardcode, log, expose hoặc ghi giá trị secret vào tài liệu.
- TUYỆT ĐỐI KHÔNG coi raw identity header là danh tính đã xác thực.
- TUYỆT ĐỐI KHÔNG dùng external JWT secret làm internal JWT secret.
- TUYỆT ĐỐI KHÔNG tắt JWT validation, method authorization hoặc request validation để feature chạy hay test pass.
- TUYỆT ĐỐI KHÔNG thêm role ngoài `CanonicalRoles.ALL` khi chưa review.
- TUYỆT ĐỐI KHÔNG log nội dung hội thoại, câu trả lời, prompt hay API key của LLM; log chỉ mang id, loại event, mã lỗi.

### Database và API

- TUYỆT ĐỐI KHÔNG sửa, xóa hoặc rewrite migration đã áp dụng; thay đổi schema bằng migration mới.
- TUYỆT ĐỐI KHÔNG destructive schema/data change khi chưa được phê duyệt rõ ràng.
- TUYỆT ĐỐI KHÔNG expose JPA entity làm request/response DTO.
- TUYỆT ĐỐI KHÔNG âm thầm đổi HTTP status, response schema, HTTP method hoặc inter-service/event contract.
- TUYỆT ĐỐI KHÔNG filter, sort, phân trang tập không giới hạn trong bộ nhớ sau `findAll()`; không tạo N+1.

### Độ phức tạp thuật toán

- TUYỆT ĐỐI KHÔNG đưa O(n^2), sort lặp hoặc lookup tuyến tính lặp vào request path chỉ vì dữ liệu test nhỏ.
- TUYỆT ĐỐI KHÔNG tối ưu bằng cách đổi fetch type toàn cục sang `EAGER`.

### Dependency, test và delivery

- TUYỆT ĐỐI KHÔNG thêm dependency chỉ để tránh tự viết một hành vi nhỏ.
- TUYỆT ĐỐI KHÔNG sửa Docker, config tập trung hoặc shared security như side effect của task chỉ liên quan một service.
- TUYỆT ĐỐI KHÔNG để test gọi LLM thật; dùng model giả lập (`ScriptedChat`) hoặc stub OpenAI-compatible cục bộ.
- TUYỆT ĐỐI KHÔNG commit `.env`, credential, build output, bytecode (`__pycache__/`, `*.pyc`) hoặc `graphify-out/`.

## 6. Quy trình phát triển và kiểm chứng

1. Đọc file này, `README.md` và README/config của module bị ảnh hưởng trước khi sửa code.
2. Giữ thay đổi trong đúng bounded context. Chỉ sửa `common-security`, Gateway, Config Server hoặc Docker khi contract thật sự cần.
3. Khi `graphify-out/graph.json` tồn tại, định hướng bằng Graphify trước khi đọc nhiều source (graph đã loại `third_party/`
   và `*.md`). Tên trùng nhau thì dùng dạng `path::Symbol`.

```powershell
graphify query "<câu hỏi về codebase>"
graphify explain "<symbol>"
graphify path "<symbol A>" "<symbol B>"
graphify affected "<symbol thay đổi>"   # node bị ảnh hưởng khi đổi symbol
graphify update .                       # sau khi sửa code; thêm --force khi số node giảm có chủ đích
```

   Chỉ đọc `graphify-out/GRAPH_REPORT.md` khi query/explain/path chưa đủ. Chỉ chạy full build `graphify .` khi graph chưa
   có, bị lỗi hoặc được yêu cầu. Không tự cài Graphify; không commit `.graphify-tools/` hay `graphify-out/`.

4. Thêm test tập trung vào layer bị ảnh hưởng: Mockito cho use case, `@WebMvcTest` cho controller/security, Testcontainers
   cho persistence (cần Docker).
5. Chạy lệnh hẹp nhất trước, mở rộng khi shared contract thay đổi:

```powershell
mvn -q -pl shared/common-security test
mvn -q -pl infra/api-gateway test
mvn -q -pl services/<name>-service -am test      # -am build kèm common-security
mvn -q compile -DskipTests                        # cả reactor; không dùng clean khi service đang chạy từ IDE
docker compose config --quiet
```

   Learning Service dùng `mvn -q -pl services/learning-service -am test`. Test context và Flyway chạy với
   Testcontainers PostgreSQL; thiếu Docker thì báo rõ test bị skip.

6. Chỉ build/chạy Docker khi daemon khả dụng: `docker compose up -d --build <service...>`, `docker compose ps`.
7. Không tuyên bố check nào đã chạy (CI, lint, coverage, contract test) khi chúng chưa tồn tại hoặc chưa thực sự chạy.
8. Commit theo conventional commit; không nhắc việc dùng AI/agent để viết code, số phase hay mã plan trong commit, code
   comment, tên test hay migration (comment ghi nguồn port DeepTutor là chuyện khác và bắt buộc).

## 7. Điểm chưa nhất quán và nội dung cần làm rõ

Danh sách đầy đủ: [`docs/system-architecture.md` §11](docs/system-architecture.md#11-điểm-chưa-nhất-quán-đã-biết).
Những điểm ảnh hưởng trực tiếp tới quy tắc:

- Use case Java mang `@Service`/`@Transactional` của Spring. Code mới vẫn giữ `application` không phụ thuộc `api`/
  `infrastructure` và không đưa dependency dạng này vào `domain`.
- `CONTENT_SERVICE_URL` của game mặc định `http://content-service:8082` trong config-repo; khi chạy game trên host,
  đặt `http://localhost:8082`. `LIBRARY_SERVICE_URL` đã mặc định `http://localhost:8081`.
- Outbox của access/content/game đã ghi event nhưng chưa có relay; đừng giả định các event đó tới được consumer.
- Config-repo có giá trị fallback cho secret; không dựa vào chúng và không tự sửa như side effect, báo người dùng.
- Chưa xác định: production deployment, secret management, CI policy, frontend contract, API versioning, cache, object storage.
