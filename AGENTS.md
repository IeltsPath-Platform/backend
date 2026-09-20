# AGENTS.md - Ngữ cảnh dự án dành cho AI Agent

- Phiên bản: 1.3
- Cập nhật lần cuối: 2026-09-18
- Dự án: `IELTSPath` (Maven coordinates hiện tại: `com.group01:code-base:1.0-SNAPSHOT`)

## 1. Tổng quan dự án

`IELTSPath` là backend theo kiến trúc microservices. Dự án cung cấp hạ tầng Spring Cloud, cơ chế bảo mật JWT dùng chung cho các downstream service và một bounded context nghiệp vụ đã được triển khai: quản lý danh tính người dùng và quyền truy cập.

Repository này không phải là đặc tả sản phẩm hoàn chỉnh; không được suy diễn thêm các khả năng nghiệp vụ ngoài các module được liệt kê bên dưới.

| Module | Trách nhiệm |
| --- | --- |
| `infra/config-server` | Cung cấp cấu hình runtime tập trung từ thư mục native `config-repo/`. |
| `infra/eureka-server` | Service Registry dùng cho Service Discovery. |
| `infra/api-gateway` | Điểm vào WebFlux, xác thực JWT bên ngoài, routing và phát hành JWT nội bộ có thời gian sống ngắn. |
| `services/user-service` | Quản lý người dùng, vai trò, đăng nhập, phát hành access token, vòng đời refresh token và quản trị người dùng. |
| `shared/common-security` | Auto-configuration bảo mật servlet dùng chung cho downstream service, xác thực JWT nội bộ, định nghĩa role chuẩn và truy cập thông tin người dùng hiện tại. Đây là thư viện dùng chung, không phải một service có thể deploy độc lập. |

Hiện tại chỉ `user-service` sở hữu database. PostgreSQL database/schema của service này được đặt tên là `user_db` trong Docker Compose và chứa người dùng, vai trò, quan hệ user-role và refresh token đã được hash.

## 2. Tech Stack - Bắt buộc tuân thủ

Chỉ sử dụng các công nghệ đã được xác minh đang tồn tại trong repository này. Không được thêm framework, thư viện lớn, database, công nghệ messaging, thành phần hạ tầng hoặc architectural pattern mới nếu chưa có sự phê duyệt rõ ràng.

| Khu vực | Công nghệ đã được xác minh |
| --- | --- |
| Ngôn ngữ và build | Java 21; Maven multi-module project; Maven Compiler Plugin 3.14.0; Maven Surefire 3.5.3 tại những module đã cấu hình. |
| Application Framework | Spring Boot 3.5.14; Spring Cloud 2025.0.0. |
| Gateway | Spring Cloud Gateway Server WebFlux, Project Reactor, Spring Security Reactive Resource Server. |
| Nền tảng service | Spring Cloud Config Server; Spring Cloud Netflix Eureka Server và Eureka Client. |
| Persistence của User Service | Spring Data JPA, Hibernate thông qua Spring Boot, PostgreSQL JDBC, Flyway. |
| Bảo mật | Spring Security, OAuth2 Resource Server, Nimbus JWT, HMAC-SHA256 JWT, BCrypt password hashing. |
| Shared code | `common-security` auto-configuration; MapStruct 1.6.3; Lombok 1.18.46. |
| Kiểm thử | JUnit Jupiter/Spring Boot Test, Mockito, Spring Security Test, Testcontainers PostgreSQL. |
| Container | Docker, Docker Compose, `postgres:15-alpine`, Eclipse Temurin JDK/JRE 21 images. |

Repository hiện tại không có frontend, message broker, cache, object storage, OpenAPI/Swagger definition hoặc CI workflow được track. Mọi nhu cầu bổ sung các thành phần này phải được coi là một quyết định thiết kế rõ ràng, không phải phần mở rộng mặc định của codebase hiện tại.

## 3. Nguyên tắc kiến trúc

Dự án sử dụng Domain-Driven Design (DDD), Clean Architecture và Microservices Architecture. Đây là các ràng buộc kiến trúc bắt buộc. Phải duy trì các nguyên tắc này kể cả khi code hiện tại có một số điểm chưa nhất quán từ trước.

### 3.1 Các layer và hướng phụ thuộc của User Service

Bounded context đã triển khai được tổ chức như sau:

```text
api -> application -> domain

infrastructure -> domain

config -> framework wiring
```

- `domain` là model lõi bên trong. Nó chứa aggregate, value object, domain exception và repository contract.
- `application` chứa command/result record và logic điều phối `*UseCase`. Layer này phụ thuộc vào repository interface trong domain, không phụ thuộc vào JPA repository hoặc adapter.
- `infrastructure` triển khai repository contract của domain bằng JPA entity, Spring Data repository, MapStruct mapper và repository adapter.
- `api` chứa REST controller, request/response DTO, exception handling và servlet filter. Controller gọi các use case và ánh xạ domain object thành API response.
- `config` chứa cấu hình bean và property đặc thù của Spring.

Code mới trong `domain` KHÔNG ĐƯỢC import `api`, `application`, `infrastructure`, Spring, JPA, HTTP hoặc các class của gateway.

Code mới trong `application` KHÔNG ĐƯỢC import `api` hoặc `infrastructure`.

Infrastructure triển khai các contract hướng vào bên trong; Infrastructure KHÔNG ĐƯỢC trở thành nơi chứa business rule.

`user-service` hiện không có package `ports` riêng: các interface trong `domain/repository` đóng vai trò persistence contract. Đây là convention của service hiện hữu, không phải lý do để đưa HTTP/SDK vào use case của service mới.

### 3.1.1 Template bắt buộc cho business service mới

```text
src/main/java/com/group01/<service>
|
|-- domain
|   |-- aggregate
|   |-- entity
|   |-- vo
|   |-- event
|   |-- exception
|   `-- repository
|
|-- application
|   |-- command
|   |-- query
|   |-- result
|   |-- usecase
|   |-- port
|   `-- exception
|
|-- api
|   |-- controller
|   `-- dto
|
`-- infrastructure
    |-- persistence
    |-- client
    |-- messaging
    |-- scheduler
    `-- config
```

- `api`: controller và DTO; chỉ map request/query thành command/query, gọi use case rồi map response.
- `application`: use case, command và result. Khi cần gọi service ngoài, broker, storage hoặc clock, định nghĩa `application/port`; adapter cụ thể nằm ở infrastructure.
- `domain`: aggregate, value object, domain exception và repository contract; bảo vệ business invariant và state transition.
- `infrastructure`: JPA entity/repository/mapper/adapter và các client kỹ thuật. Tách domain aggregate khỏi JPA entity.
- Mỗi service mới sở hữu database và domain model riêng. Không tạo package `port`, `client`, `messaging`, `scheduler`, CQRS query hoặc domain event rỗng; chỉ thêm khi use case cần.

### 3.2 Các building block DDD đang được sử dụng

- Aggregate: `User`, `Role` và `RefreshToken` trong `domain/aggregate`.
- Value Object: `Email`, `PhoneNumber`, `RoleName` và `UserStatus` trong `domain/vo`.
- Domain Exception: `domain/exception`.
- Repository Abstraction: `domain/repository`.
- Application Command/Result: `application/command` và `application/result`.

Đặt business validation và state transition trong aggregate/value object hoặc use case phù hợp.

Code hiện tại không sử dụng domain event, factory, CQRS query object hoặc package domain service riêng; không được tự tạo thêm các khái niệm này chỉ để làm cấu trúc có vẻ đầy đủ hoặc đối xứng.

### 3.3 Presentation, Validation, Error Handling và Logging

- Các REST entry point hiện tại là `AuthController` (`/auth`) và `UserController` (`/api/users`). Mapping trong controller là nguồn tham chiếu chính thức cho API hiện tại.
- Request DTO sử dụng Bean Validation và controller sử dụng `@Valid`. Domain value object và aggregate cũng phải bảo vệ các domain invariant.
- `GlobalExceptionHandler` ánh xạ domain exception, validation error, authorization error và unexpected exception sang cấu trúc `ErrorResponse` hiện có. Hãy mở rộng cơ chế này thay vì trả về các error body tự phát, không thống nhất.
- Log của Gateway và User Service có `X-Correlation-Id`. Phải duy trì và forward header này; không được log credential, token hoặc thông tin password.
- Sử dụng `CurrentUserProvider` từ `common-security` để lấy danh tính đã được xác thực ở downstream service. Không được tin tưởng trực tiếp các header `X-User-*` do client gửi lên.

### 3.4 Ranh giới bảo mật

- `user-service` phát hành external HMAC JWT access token và chỉ lưu refresh token dưới dạng hash.
- `api-gateway` xác thực external JWT, route thông qua Eureka, thay thế authorization value đầu vào cho các downstream path đã cấu hình và ký một internal HMAC JWT có thời gian sống ngắn.
- Các downstream service dạng servlet sử dụng `CommonSecurityAutoConfiguration` của `common-security` để xác thực internal token và tạo Spring authorities. Auto-configuration này được thiết kế riêng cho servlet.
- Gateway sử dụng WebFlux và có `SecurityWebFilterChain` riêng; không được áp dụng servlet auto-configuration cho Gateway.
- Việc xác thực internal token yêu cầu issuer đúng theo cấu hình, subject là UUID, có expiry và role thuộc `CanonicalRoles.ALL` (`ADMIN`, `CUSTOMER`, `CONTENT_AUTHOR`, `EXAMINER`, `SALES_STAFF`). Token claim và role constant phải được quản lý tập trung trong `common-security`.
- External signing secret và internal signing secret phải tách biệt. Cấu hình sử dụng `EXTERNAL_JWT_SECRET` và `GATEWAY_INTERNAL_JWT_SECRET`; HMAC secret phải được encode bằng Base64 và có độ dài tối thiểu 32 byte. Không bao giờ sao chép giá trị thực của chúng vào code, tài liệu, test, log hoặc commit.

### 3.5 Ranh giới Microservice

- Mỗi business service sở hữu bounded context và persistence riêng. Không service nào được đọc hoặc ghi database của service khác hoặc dùng chung JPA entity với service khác.
- Routing service-to-service hiện tại là đồng bộ thông qua Gateway, sử dụng Eureka và route `lb://`. Hiện chưa triển khai messaging mechanism.
- Shared module chỉ dành cho các technical concern dùng chung. Không được chuyển entity thuộc user domain hoặc business use case vào `shared/`.
- Các public route hiện tại ánh xạ `/auth/**` và `/api/users/**` tới `USER-SERVICE`. Hiện chưa có API version prefix. Phải duy trì các path tương thích và đánh giá tất cả consumer trước khi thay đổi API contract hoặc JWT claim contract.
- Gateway chỉ chứa routing, security, CORS, filtering và observability. TUYỆT ĐỐI KHÔNG đặt business logic của user domain hoặc domain khác trong Gateway.

### 3.6 Configuration và Runtime

- Bootstrap setting nằm trong `src/main/resources/application.yml` hoặc `src/main/resources/application.yaml` của từng module.
- Shared runtime setting và service-specific runtime setting nằm trong `infra/config-server/config-repo/{application,api-gateway,eureka-server,user-service}.yaml`. Tránh lặp lại global Eureka Client setting trong các file cấu hình riêng của từng service.
- Thứ tự khởi động là Config Server, Eureka Server, Gateway, sau đó đến các business service.
- Docker Compose khởi động `user-db` cùng toàn bộ infrastructure service và `user-service`.
- `Dockerfile.spring-service` build các Maven module thông thường được chọn bằng `MODULE_PATH`; Config Server có Dockerfile riêng vì cần đóng gói thêm `config-repo`.

### 3.7 Hiệu năng Persistence, N+1 và độ phức tạp

- `UserJpaEntity.roles` hiện là quan hệ `LAZY`. Các query đọc user cần role đã dùng `@EntityGraph(attributePaths = "roles")` trong `UserJpaRepository`. Giữ fetch plan tường minh theo nhu cầu response; không đổi toàn cục sang `EAGER` chỉ để che lỗi lazy loading hoặc N+1.
- Trước khi thêm endpoint đọc collection hoặc relation, xác định dữ liệu nào response thực sự cần rồi tạo query/repository method có fetch plan tương ứng. Không tải toàn bộ entity graph khi chỉ cần một projection nhỏ.
- TUYỆT ĐỐI KHÔNG gọi repository, external client hoặc lazy relation trong vòng lặp trên danh sách request/entity. Thu thập key trước, query theo tập key hoặc fetch relation một lần, sau đó ghép dữ liệu bằng `Map`/`Set`.
- `findAll()` không phân trang là baseline hiện hữu. Không được nhân rộng nó cho dữ liệu có thể tăng không giới hạn; endpoint danh sách mới phải có pagination hoặc giới hạn rõ ràng và việc filter/sort phải diễn ra ở database.
- Trên request path có dữ liệu biến thiên, ưu tiên độ phức tạp O(n) hoặc O(n log n). O(n^2) chỉ được chấp nhận khi tập dữ liệu có cận nhỏ được chứng minh, và phải giải thích lý do ngay gần code.
- Dùng `HashSet`/`HashMap` cho membership check, deduplication hoặc join in-memory; không dùng nested loop hoặc `List.contains()` lặp lại khi kích thước dữ liệu có thể tăng.
- Với query phức tạp hoặc có relation, bổ sung hoặc cập nhật integration test Testcontainers khi phù hợp và review SQL/fetch behavior. Repository hiện chưa có query-count gate tự động, nên không được tuyên bố N+1 đã được kiểm soát nếu chưa kiểm tra query thực tế.

## 4. Quy tắc đặt tên file và cấu trúc dự án

```text
shared/common-security/                thư viện security kỹ thuật dùng chung

infra/{api-gateway,config-server,eureka-server}/

services/user-service/
  src/main/java/com/group01/user/
    api/ application/ config/ domain/ infrastructure/
  src/main/resources/db/migration/

infra/config-server/config-repo/       YAML cấu hình runtime tập trung

docker-compose.yml                     môi trường multi-container local
```

Khi không có convention cụ thể được liệt kê, hãy tuân theo code lân cận trong cùng module.

| Thành phần | Convention hiện tại |
| --- | --- |
| Java package | Tên package viết thường dưới `com.group01.<module>`. |
| Java type | PascalCase; mỗi public type nằm trong một file có tên tương ứng. |
| Aggregate và Value Object | `domain/aggregate/<Name>.java`, `domain/vo/<Name>.java`; Value Object có thể là record hoặc enum. |
| Use Case và Command | `application/usecase/<Verb>NounUseCase.java`, `application/command/<Verb>NounCommand.java`, `application/result/<Name>Result.java`. |
| Repository Boundary | `domain/repository/<Name>Repository.java`; implementation là `infrastructure/adapter/<Name>RepositoryAdapter.java`. |
| JPA Persistence | `*JpaEntity`, `*JpaRepository`, `*Mapper` và `*Specifications` trong `infrastructure/persistence`. |
| HTTP API | `*Controller`, `dto/request/*Request`, `dto/response/*Response` và `GlobalExceptionHandler`. |
| Spring Configuration | `*Config`, `*Properties` và `*AutoConfiguration`. |
| Test | Mirror package production dưới `src/test/java`; test class đặt tên `*Test`. |
| Migration | Flyway SQL tại `src/main/resources/db/migration/V{number}__{description}.sql`; baseline hiện tại là `V1__create_user_tables.sql`. |
| Tên service và artifact | Dùng kebab-case cho directory và Maven Artifact ID, ví dụ `user-service`. |

## 5. Các pattern bị cấm

### Kiến trúc

- TUYỆT ĐỐI KHÔNG đặt core business rule, persistence access hoặc cross-service call trong controller hoặc gateway filter.
- TUYỆT ĐỐI KHÔNG để code domain phụ thuộc HTTP, Spring, JPA, adapter, Gateway hoặc service khác.
- TUYỆT ĐỐI KHÔNG bỏ qua Application Use Case để gọi trực tiếp JPA Repository từ `api`.
- TUYỆT ĐỐI KHÔNG tạo shared domain entity, table hoặc repository dùng chung giữa các bounded context.
- TUYỆT ĐỐI KHÔNG truy cập trực tiếp database của microservice khác.
- TUYỆT ĐỐI KHÔNG thay đổi architecture boundary, public route contract, JWT claim hoặc canonical role nếu chưa được review và phê duyệt có chủ đích.

### Bảo mật

- TUYỆT ĐỐI KHÔNG commit, hardcode, log, expose hoặc đưa giá trị secret vào file này.
- TUYỆT ĐỐI KHÔNG coi raw identity header là danh tính đã được xác thực.
- TUYỆT ĐỐI KHÔNG tái sử dụng external JWT signing secret làm Gateway internal JWT signing secret.
- TUYỆT ĐỐI KHÔNG tắt JWT validation, method authorization hoặc request validation chỉ để feature chạy hoặc test pass.
- TUYỆT ĐỐI KHÔNG thêm role chưa được review ngoài `CanonicalRoles.ALL` và role model của user domain.

### Database và API

- TUYỆT ĐỐI KHÔNG chỉnh sửa, xóa hoặc rewrite một Flyway migration đã được apply một cách tùy tiện. Khi thay đổi schema, hãy thêm migration mới.
- TUYỆT ĐỐI KHÔNG thực hiện destructive schema change hoặc destructive data change nếu chưa được phê duyệt rõ ràng.
- TUYỆT ĐỐI KHÔNG expose JPA Entity trực tiếp làm request hoặc response DTO.
- TUYỆT ĐỐI KHÔNG âm thầm thay đổi HTTP status code, response schema, HTTP method của endpoint hoặc inter-service contract.
- TUYỆT ĐỐI KHÔNG lọc, sort hoặc phân trang một tập database không giới hạn trong Java sau khi đã `findAll()`.
- TUYỆT ĐỐI KHÔNG tạo N+1 query bằng cách dereference relation `LAZY` hoặc gọi repository trong vòng lặp.

### Độ phức tạp thuật toán

- TUYỆT ĐỐI KHÔNG đưa nested loop O(n^2), repeated sort, hoặc repeated linear lookup vào request path chỉ vì dữ liệu test còn nhỏ.
- TUYỆT ĐỐI KHÔNG tối ưu bằng cách đổi fetch type toàn cục sang `EAGER`; lựa chọn fetch strategy phải nằm ở query/repository method đang phục vụ use case cụ thể.

### Dependency và Delivery

- TUYỆT ĐỐI KHÔNG thêm dependency chỉ để tránh tự triển khai một hành vi local nhỏ.
- TUYỆT ĐỐI KHÔNG sửa Docker, centralized configuration hoặc shared security như một side effect của task chỉ liên quan đến một service.
- TUYỆT ĐỐI KHÔNG commit `.env`, credential, generated build output hoặc local graph artifact.

## 6. Quy trình phát triển và kiểm chứng

1. Đọc file này, `README.md` và README/configuration của module bị ảnh hưởng trước khi thay đổi code.

2. Giữ thay đổi trong đúng bounded context. Chỉ cập nhật `common-security`, Gateway, Config Server hoặc Docker khi contract được yêu cầu thực sự cần đến thay đổi đó.

3. Khi cần hiểu quan hệ codebase và `graphify-out/graph.json` tồn tại, dùng Graphify trước khi đọc một lượng lớn source:

```powershell
graphify query "<câu hỏi về codebase>"

graphify explain "<khái niệm hoặc symbol>"

graphify path "<symbol A>" "<symbol B>"

graphify affected "<symbol thay đổi>"
```

Chỉ đọc `graphify-out/GRAPH_REPORT.md` cho review kiến trúc rộng khi query/path/explain chưa đủ. Không cần chạy Graphify cho thay đổi Markdown vì `.graphifyignore` đang bỏ qua `*.md`.

4. Sau khi thay đổi source code, chạy lệnh cập nhật incremental sau khi test:

```powershell
graphify update .
```

Chỉ chạy full build `graphify .` khi graph chưa tồn tại, graph bị lỗi, hoặc cần tái trích xuất semantic toàn diện theo yêu cầu. Sau refactor xóa nhiều file, dùng `graphify update . --force` khi đã kiểm tra việc graph có ít node hơn là đúng. Không tự cài Graphify hoặc không commit `.graphify-tools/` và `graphify-out/` nếu môi trường chưa có công cụ.

5. Thêm test tập trung vào layer bị ảnh hưởng. Sử dụng Mockito cho Application Use Case, `@WebMvcTest` cho hành vi Controller/Security và Testcontainers PostgreSQL cho Persistence behavior khi Docker khả dụng.

6. Chạy Maven command có phạm vi nhỏ nhất liên quan trước, sau đó mới mở rộng phạm vi khi shared contract thay đổi:

```powershell
mvn -pl shared/common-security test

mvn -pl infra/api-gateway test

mvn -pl services/user-service test

mvn clean compile -DskipTests

docker compose config --quiet
```

7. Chỉ build hoặc chạy Docker stack khi Docker daemon khả dụng:

```powershell
docker compose up -d --build

docker compose ps
```

Hiện chưa có CI workflow được track, formatter, linter, code-coverage gate hoặc OpenAPI contract test. Không được tuyên bố các check này đã chạy cho đến khi chúng thực sự được bổ sung.

## 7. Các điểm chưa nhất quán đã quan sát và nội dung cần làm rõ

- Các Application Use Case hiện đang mang annotation Spring `@Service` và `@Transactional`. Đây là framework dependency đang tồn tại trong Application Layer; code mới vẫn phải giữ cho Application không phụ thuộc `api` và `infrastructure`, đồng thời không được thêm dependency dạng này vào Domain Layer.

- `services/user-service/README.md` mô tả endpoint cập nhật trạng thái người dùng bằng `PATCH`, trong khi `UserController` hiện expose `PUT /api/users/{id}/status`. Tạm thời coi Controller là nguồn tham chiếu chính xác cho behavior thực tế cho đến khi documentation được đồng bộ một cách có chủ đích.

- Production deployment target, CI policy, frontend contract, API versioning, messaging, cache và object storage hiện chưa được thiết lập trong repository này. Cần làm rõ trước khi bổ sung các thành phần đó.
