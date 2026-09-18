# GLOBAL CONSTRAINTS — IELTSPath

Version: 1.0.0
Status: ACTIVE
Last updated: 2026-09-18
Scope: Repository-wide technical constraints, with narrower scopes labelled explicitly.

## 1. MỤC ĐÍCH

File này ghi lại các technical constraint, engineering convention và technical choice đã được xác minh cho IELTSPath. Nó không chứa business rule, security policy chi tiết, safety procedure hoặc architecture narrative.

Khi cần chi tiết hoặc khi có xung đột, ưu tiên các tài liệu sau:

- [Constitution](../global/constitution.md) cho invariant dự án và safety của migration.
- [System Architecture](../global/system-architecture.md) cho service boundary, data ownership và communication.
- [Security Specification](../global/security.md) cho authentication, authorization, secret và sensitive logging.

`business.md`, `safety.md` và `api-standards.md` chưa tồn tại trong `.sdd/` tại thời điểm viết.

## 2. PHẠM VI ÁP DỤNG

- **Toàn repository:** Java/Maven toolchain, module organization, dependency policy, naming, configuration và delivery convention.
- **Backend business service:** structure API/application/domain/infrastructure, DTO validation và testing approach.
- **Gateway:** WebFlux/reactive stack là stack riêng; không áp dụng servlet security auto-configuration cho Gateway.
- **Persistence hiện hữu:** JPA, PostgreSQL và Flyway mới được xác minh ở `services/user-service`; không suy diễn rằng mọi service tương lai phải có cùng database hoặc persistence stack nếu chưa được phê duyệt.

## 3. TECH STACK ĐÃ ĐƯỢC XÁC MINH

| Khu vực | Công nghệ | Version / nguồn | Phạm vi |
| --- | --- | --- | --- |
| Language và compiler | Java | 21 — root `pom.xml` | Tất cả Maven modules hiện có |
| Build | Maven reactor multi-module | Root `pom.xml` | Toàn repository |
| Application platform | Spring Boot | 3.5.14 — root `pom.xml` | Tất cả Spring modules hiện có |
| Cloud platform | Spring Cloud | 2025.0.0 — root `pom.xml` | Infrastructure và service dùng Cloud starter |
| Gateway | Spring Cloud Gateway Server WebFlux, Reactor, reactive OAuth2 Resource Server | `infra/api-gateway/pom.xml` | Chỉ Gateway |
| Discovery và config | Eureka, Config Server | Module POM/configuration | Infrastructure và clients hiện có |
| Servlet security shared code | Spring Security, OAuth2 Resource Server, `common-security` | `shared/common-security/pom.xml` | Downstream servlet service |
| Persistence hiện hữu | Spring Data JPA, PostgreSQL JDBC, Flyway | `services/user-service/pom.xml` | `user-service` |
| Mapping/code generation | MapStruct 1.6.3, Lombok 1.18.46 | User-service/root POM | MapStruct: user-service; Lombok: modules đang khai báo |
| Testing | JUnit Jupiter, Spring Boot Test, Mockito, Spring Security Test, Testcontainers PostgreSQL | Module POM và test source | Theo module/test type |
| Local containers | Docker Compose, PostgreSQL image `postgres:15-alpine`, Eclipse Temurin 21 images | `docker-compose.yml`, Dockerfiles | Local stack |

Không có frontend, JavaScript/Node package manager, message broker, cache, object storage, OpenAPI/Swagger definition hoặc CI workflow được track.

## 4. TOOLCHAIN VÀ BUILD

### GLOB-01: Maven reactor và Java 21

**Phạm vi:** Toàn repository.

**Constraint:** Modules MUST tiếp tục là Maven modules của root reactor và MUST compile với Java 21. Không thêm build system thứ hai hoặc đổi Java major version nếu chưa có architectural approval.

**Bằng chứng:** Root `pom.xml` khai báo packaging `pom`, năm module, compiler source/target 21 và BOM dependency management.

### GLOB-02: Quản lý version dependency tập trung

**Phạm vi:** Toàn repository.

**Constraint:** Module SHOULD kế thừa root parent POM. Spring Boot và Spring Cloud version MUST tiếp tục lấy từ BOM/import ở root POM; không pin lại version của managed dependency trong module nếu chưa có lý do được review.

**Bằng chứng:** Tất cả module POM hiện có đều kế thừa `com.group01:code-base`; root POM import Spring Boot và Spring Cloud dependency BOM.

**Lưu ý:** Maven Compiler Plugin 3.14.0 và Surefire 3.5.3 chỉ được cấu hình tường minh ở `common-security`, `api-gateway` và `user-service`; chưa có bằng chứng rằng các plugin này được áp dụng đồng nhất cho mọi module.

## 5. DEPENDENCY CONSTRAINTS

### GLOB-03: Ưu tiên stack đã được chấp nhận

**Phạm vi:** Toàn repository.

**Constraint:** Thay đổi MUST ưu tiên dependency và framework đã được xác minh trong section 3. Không thêm framework lớn, database, messaging, cache, storage, frontend stack hoặc architectural pattern mới nếu chưa có approval rõ ràng.

**Bằng chứng:** `AGENTS.md` section 2 và Constitution `ENG-01`.

Không phát hiện danh sách package bị cấm hoặc dependency đã deprecate ở cấp repository. Không có lockfile, vulnerability scanner, SBOM hoặc supply-chain gate được track.

## 6. REPOSITORY VÀ MODULE ORGANIZATION

### GLOB-04: Phân vùng module

**Phạm vi:** Toàn repository.

**Constraint:** Đặt deployable infrastructure module trong `infra/`, business service trong `services/` và shared technical library trong `shared/`. Shared module MUST NOT trở thành nơi đặt business model hoặc business use case.

**Bằng chứng:** Root `pom.xml`, repository tree, `AGENTS.md`, Constitution `MS-03`.

Các module hiện có là `infra/api-gateway`, `infra/config-server`, `infra/eureka-server`, `services/user-service` và `shared/common-security`. Chỉ `user-service` hiện có persistence; đây là fact hiện tại, không phải quy định rằng mọi service phải dùng cùng persistence stack.

## 7. NAMING CONVENTIONS

| Đối tượng | Convention đã xác minh | Bằng chứng / phạm vi |
| --- | --- | --- |
| Maven module, deployable service, artifact | kebab-case | `api-gateway`, `config-server`, `eureka-server`, `user-service`, `common-security` |
| Java package | lowercase dưới `com.group01.<module>` | Source tree và `AGENTS.md` |
| Java public type | PascalCase, một public type mỗi file cùng tên | `AGENTS.md`; source tree |
| Use case | `<Verb><Noun>UseCase` | `application/usecase/*UseCase.java` |
| Command/result | `<Verb><Noun>Command`, `<Name>Result` | `application/command`, `application/result` |
| Domain repository / adapter | `<Name>Repository`, `<Name>RepositoryAdapter` | User-service domain/infrastructure source |
| Persistence types | `*JpaEntity`, `*JpaRepository`, `*Mapper`, `*Specifications` | User-service persistence convention |
| HTTP types | `*Controller`, `dto/request/*Request`, `dto/response/*Response` | User-service API source |
| Test | Mirror production package under `src/test/java`; class name `*Test` | Tests across common-security, Gateway, user-service |
| Flyway migration | `V{number}__{description}.sql` | `V1__create_user_tables.sql`, `AGENTS.md` |
| Bootstrap config | `src/main/resources/application.yml` or `application.yaml` | All deployable module resources |
| Central runtime config | `infra/config-server/config-repo/<application>.yaml` | Existing config repository |
| Environment variable | UPPER_SNAKE_CASE | Compose/config placeholders such as `SERVER_PORT` and config URI variables |

Không có naming convention toàn repository được xác minh cho API versioning, query parameter, JSON field casing ngoài behavior của module hiện hữu.

## 8. CODE ORGANIZATION CONSTRAINTS

### GLOB-05: Business-service source organization

**Phạm vi:** Business service hiện hữu và template business service trong `AGENTS.md`.

**Constraint:** Business service MUST giữ source theo responsibility hiện hữu: `api` xử lý HTTP/DTO, `application` điều phối use case, `domain` giữ model/contract và `infrastructure` chứa technical adapter. Dependency boundary chi tiết thuộc Constitution `ARCH-01` và System Architecture; không sao chép hoặc nới lỏng chúng trong module.

**Bằng chứng:** `AGENTS.md` sections 3–5; user-service controller, domain repository và infrastructure adapter source.

### GLOB-06: Mapper và generated output

**Phạm vi:** Modules dùng Lombok hoặc MapStruct.

**Constraint:** Thay đổi mapping MUST được thực hiện ở source mapper/model, không sửa output trong `target/`. Generated build output MUST NOT được commit.

**Bằng chứng:** User-service POM cấu hình MapStruct processor; module POM dùng Lombok processor; `AGENTS.md` delivery rules.

## 9. API CONVENTIONS

### GLOB-07: API boundary hiện hữu

**Phạm vi:** Backend HTTP API.

**Constraint:** Request DTO SHOULD dùng Bean Validation và controller SHOULD dùng `@Valid`; domain/use case vẫn phải bảo vệ invariant sau API validation. JPA entity MUST NOT được expose trực tiếp làm request hoặc response.

**Bằng chứng:** Controller source, request DTO usage và `AGENTS.md`.

Controller mapping là nguồn tham chiếu behavior hiện tại khi README mâu thuẫn source. Chưa có API version prefix, OpenAPI specification, cross-service response envelope hoặc API contract test được xác minh.

**Observed, module-specific:** `user-service` trả lỗi qua `GlobalExceptionHandler` và `ErrorResponse(timestamp, status, error, message, path, details)`. Đây chưa đủ bằng chứng để coi error schema này là contract toàn repository; thay đổi trong user-service nên mở rộng handler hiện có.

## 10. PERSISTENCE VÀ MIGRATION CONVENTIONS

### GLOB-08: Migration cho persistence hiện hữu

**Phạm vi:** `services/user-service`; áp dụng cho service khác chỉ khi service đó được phê duyệt dùng Flyway.

**Constraint:** Schema change của user-service MUST đi qua Flyway migration trong `src/main/resources/db/migration/` và dùng tên `V{number}__{description}.sql`. Migration history đã apply tuân Constitution `DATA-01`.

**Bằng chứng:** User-service POM, `config-repo/user-service.yaml`, migration directory, Constitution `DATA-01`.

Spring Data JPA/Hibernate chạy với `ddl-auto: validate` ở user-service; Flyway được enable ở configuration tập trung. JPA entity, Spring Data repository, MapStruct mapper và repository adapter là convention persistence được xác minh **chỉ** cho user-service.

Không có global database engine, schema naming standard hoặc persistence framework bắt buộc nào được xác minh cho service tương lai.

## 11. CONFIGURATION CONVENTIONS

### GLOB-09: Bootstrap và centralized runtime configuration

**Phạm vi:** Deployable Spring modules hiện có.

**Constraint:** Bootstrap setting thuộc `src/main/resources/application.yml` hoặc `application.yaml`. Shared/service runtime setting SHOULD thuộc native Config Server repository tại `infra/config-server/config-repo/`; không lặp lại global Eureka client setting trong từng service config khi shared configuration đã đáp ứng.

**Bằng chứng:** Module application files, config repository, `AGENTS.md`.

Configuration values MAY được ghi đè qua environment variable. Mọi cấu hình chứa secret phải tuân [security.md](../global/security.md).

## 12. ERROR HANDLING CONVENTIONS

Không có global exception-to-response schema được xác minh trên nhiều business service. Với `user-service`, `GlobalExceptionHandler` là extension point cho domain exception, validation, authorization và unexpected exception; không tạo ad-hoc error body trong module này.

Các error policy nhạy cảm và yêu cầu không lộ secret/credential thuộc [security.md](../global/security.md).

## 13. LOGGING VÀ OBSERVABILITY CONVENTIONS

### GLOB-10: Correlation context cho HTTP flow phân tán

**Phạm vi:** Gateway và downstream HTTP flow hiện có.

**Constraint:** Request đi qua Gateway SHOULD duy trì `X-Correlation-Id`. Gateway tạo hoặc forward header này, trả nó trên response và downstream hiện hữu dùng nó trong request log.

**Bằng chứng:** `CorrelationIdFilter`, `LoggingFilter`, `UserServiceLoggingFilter`, `AGENTS.md`.

Logging hiện có ghi method, path/URI, status và duration. Không có structured-log platform, trace backend, metric backend, alerting hoặc log-retention convention được xác minh. Sensitive logging thuộc [security.md](../global/security.md).

## 14. TESTING CONVENTIONS

### GLOB-11: Test theo layer và rủi ro

**Phạm vi:** Backend modules.

**Constraint:** Test class MUST nằm dưới `src/test/java`, mirror production package khi phù hợp và dùng hậu tố `*Test`. Thay đổi behavior SHOULD chọn test hẹp nhất có ý nghĩa: Mockito/JUnit cho application use case, `@WebMvcTest` cùng Spring Security Test cho controller/security, và Testcontainers PostgreSQL cho persistence/Flyway behavior khi Docker khả dụng.

**Bằng chứng:** Test source ở common-security, Gateway và user-service; `AGENTS.md`; Constitution `TEST-01`.

JUnit Jupiter và Spring Boot Test được dùng ở nhiều modules. Mockito, Spring Security Test và Testcontainers PostgreSQL chỉ được xác minh ở user-service. Không phát hiện coverage threshold, linter, formatter, test fixture convention toàn repository hoặc CI test gate.

## 15. BACKEND CONSTRAINTS

### GLOB-12: Servlet downstream và reactive Gateway tách biệt

**Phạm vi:** Gateway và downstream servlet service.

**Constraint:** Gateway MUST dùng reactive `SecurityWebFilterChain`/WebFlux path riêng. Downstream servlet service MAY dùng `common-security` auto-configuration; servlet auto-configuration MUST NOT được đưa vào Gateway.

**Bằng chứng:** Gateway/common-security POM and source; `AGENTS.md`; [security.md](../global/security.md).

Application use case hiện có Spring `@Service` và `@Transactional`. Đây là implementation hiện hữu, không phải bằng chứng cho phép domain layer phụ thuộc Spring.

## 16. INFRASTRUCTURE CONSTRAINTS

### GLOB-13: Runtime discovery, config và local container build

**Phạm vi:** Infrastructure modules và local Docker Compose stack.

**Constraint:** Runtime service discovery hiện dùng Eureka và Gateway routes dùng `lb://` thay vì hardcode target instance URL. Config Server cung cấp native centralized configuration. Startup local SHOULD theo Config Server → Eureka Server → API Gateway → business service.

**Bằng chứng:** Config repository, module application configuration, Docker Compose và `AGENTS.md`.

Regular Spring service container builds dùng root `Dockerfile.spring-service` với `MODULE_PATH`; Config Server có Dockerfile riêng để đóng gói native `config-repo`. Docker Compose mô tả local development stack; không có production deployment target hoặc orchestration standard được xác minh.

## 17. GENERATED CODE / ARTIFACTS

Lombok và MapStruct annotation processor tạo build artifacts cho modules sử dụng chúng. `target/`, local graph output và các artifact build/generated khác không phải source of truth và MUST NOT được commit. Không có command generate hoặc generated-source directory được track riêng.

## 18. COMMAND REFERENCE

Các command dưới đây được xác minh từ `AGENTS.md` và root README:

```powershell
mvn -pl shared/common-security test
mvn -pl infra/api-gateway test
mvn -pl services/user-service test
mvn clean compile -DskipTests
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Chạy Docker command chỉ khi Docker daemon khả dụng. Không có install, lint, format, type-check, migration CLI hay CI command toàn repository được xác minh ngoài các command trên.

## 19. CÁC CONVENTION CHƯA THỐNG NHẤT

- User-service README mô tả status update bằng `PATCH`, nhưng `UserController` hiện expose `PUT /api/users/{id}/status`; controller là behavior source of truth theo `AGENTS.md`.
- Maven compiler/Surefire plugin configuration chưa nằm đồng nhất ở mọi module.
- Gateway là reactive WebFlux, trong khi downstream security shared module là servlet-focused. Đây là scope-specific implementation hợp lệ, không phải một stack security chung để áp dụng hoán đổi.

## 20. CẦN TEAM LÀM RÕ

- Có đổi Maven coordinates/artifact `code-base` sang thương hiệu IELTSPath hay chỉ đổi tên hiển thị/tài liệu?
- API versioning, pagination/filter/sort contract, API response standard và OpenAPI/contract-test strategy.
- CI/CD, release process, formatter/linter và coverage policy.
- Production deployment target, secret manager, observability platform và operational policy.
- Persistence/migration/database standard cho business service mới; hiện chỉ user-service có bằng chứng.
- Messaging, cache, object storage và external integration strategy trước khi các capability này được thêm.
