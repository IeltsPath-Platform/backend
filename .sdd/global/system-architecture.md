# SYSTEM ARCHITECTURE - code-base

Version: 1.0.0
Status: ACTIVE
Last updated: 2026-09-18

## 1. MỤC ĐÍCH

Tài liệu này mô tả topology, communication pattern, data ownership và architectural boundary đang tồn tại của hệ thống. Nó phải tuân [Constitution](constitution.md) và tham chiếu [Security Specification](security.md) cho security policy chi tiết.

Đây không phải API/schema/class documentation. Feature và module spec phải dùng tài liệu này để đặt thay đổi vào đúng bounded context và dependency boundary.

## 2. KIẾN TRÚC TỔNG QUAN

`code-base` là Maven multi-module backend theo architectural direction DDD, Clean Architecture và Microservices. Hiện có một business bounded context cùng ba infrastructure service, một shared library và một database do business service sở hữu.

```text
Configuration and discovery

Native configuration repository
             |
             v
       Config Server ---- configuration ----> Eureka, Gateway, user-service
                                                     ^                 |
                                                     |---- register ----|

Request path

Client ---> API Gateway ---- route via Eureka + internal identity ----> user-service ---> user-db
```

Client request đi qua API Gateway. Gateway dùng Service Discovery để route tới business service, đồng thời chuyển external identity đã verify thành internal identity theo security boundary hiện có.

## 3. SYSTEM COMPONENT MAP

| Component | Loại | Trách nhiệm | Deployable |
| --- | --- | --- | --- |
| `api-gateway` | Infrastructure Service / Edge | Điểm vào HTTP, routing, external token validation, internal token issuance, CORS và correlation/logging edge concern | Có |
| `config-server` | Infrastructure Service | Phân phối runtime configuration từ native configuration repository | Có |
| `eureka-server` | Infrastructure Service | Đăng ký và discovery instance cho Gateway và service | Có |
| `user-service` | Business Service | Identity, user lifecycle, role, login và refresh credential lifecycle | Có |
| `common-security` | Shared Library | Security cross-cutting cho downstream servlet service và security constants/utility dùng chung | Không |
| `user-db` | Database | Persistence do user-service sở hữu | Có, trong local Compose |
| Native configuration repository | Configuration Asset | Nguồn cấu hình runtime cho Config Server | Không phải service |

Không có frontend, cache, message broker, object storage, external provider, observability platform hoặc CI/CD component được xác minh trong repository hiện tại.

## 4. BOUNDED CONTEXT MAP

| Bounded Context | Service | Trách nhiệm | Dependency chính |
| --- | --- | --- | --- |
| Identity and Access | `user-service` | Quản lý user, role, authentication và refresh credential lifecycle | PostgreSQL, Config Server, Service Discovery, shared security |

Gateway, Config Server và Eureka Server là infrastructure component, không phải business bounded context. Không có business bounded context thứ hai được xác minh.

## 5. SERVICE RESPONSIBILITIES

### api-gateway

**Responsibility:** Nhận client request, áp dụng edge security, định tuyến qua discovery và phát correlation context.

**Owns:** Không sở hữu business domain hoặc business database.

**Does NOT own:** User lifecycle, role decision, credential persistence hoặc business invariant.

**Dependencies:** Config Server, Eureka Server và shared security types.

**Communication:** Nhận HTTP từ client, synchronous route tới business service; đăng ký và tra cứu instance qua Eureka.

### config-server

**Responsibility:** Cung cấp cấu hình theo service/profile từ native configuration repository.

**Owns:** Configuration distribution mechanism và native configuration source trong deployment hiện tại.

**Does NOT own:** Business data, authentication decision hoặc Service Discovery registry.

**Dependencies:** Native configuration repository.

**Communication:** Synchronous configuration delivery tới Eureka, Gateway và business service khi khởi động/refresh theo cấu hình.

### eureka-server

**Responsibility:** Duy trì service registry để client-side discovery và load-balanced routing hoạt động.

**Owns:** Runtime registry metadata.

**Does NOT own:** Business data, routing policy hoặc security decision.

**Dependencies:** Config Server trong startup configuration flow.

**Communication:** Gateway và business service đăng ký/tìm instance qua Eureka client protocol.

### user-service

**Responsibility:** Thực thi Identity and Access bounded context, gồm user lifecycle, role, login, external access token và refresh credential lifecycle.

**Owns:** Identity/access domain state và `user-db` persistence.

**Does NOT own:** Gateway routing, centralized configuration, service registry hoặc domain của service tương lai.

**Dependencies:** Config Server, Eureka Server, `user-db` và `common-security` cho downstream security.

**Communication:** Nhận HTTP đã được Gateway route; nhận authenticated identity từ internal JWT; không có outbound business HTTP/event integration được xác minh.

### common-security

**Responsibility:** Cung cấp technical security cross-cutting concern cho downstream servlet service, gồm internal token validation, authority mapping và current-user access.

**Owns:** Không sở hữu bounded context hoặc persistence.

**Does NOT own:** Business authorization rule của user-service hay service khác.

**Dependencies:** Spring Security APIs.

**Communication:** Compile-time dependency; không deploy độc lập và không mở network endpoint.

## 6. COMMUNICATION ARCHITECTURE

### Synchronous communication

- Client giao tiếp HTTP với API Gateway.
- Gateway route HTTP đồng bộ tới business service bằng `lb://` và Eureka discovery.
- Config Server phân phối configuration đồng bộ cho các service.
- Gateway và business service đăng ký/tra cứu instance qua Eureka.
- Gateway không gọi trực tiếp identity service để verify mỗi request; verification dùng signing material và token claim đã cấu hình.

### Asynchronous communication

Hiện tại chưa có asynchronous messaging architecture. Không có broker, topic, queue, event publisher/consumer hoặc outbox được xác minh.

### External integration

Hiện không có third-party API, storage, payment, AI, email hoặc external identity provider được xác minh.

## 7. DATA OWNERSHIP

| Data / Domain | Owner | Storage | Access from other services |
| --- | --- | --- | --- |
| Identity, role và authentication credential lifecycle | `user-service` | PostgreSQL `user-db` | Không có service business khác hiện tại; service mới phải dùng explicit contract, không direct database access. |
| Runtime configuration | `config-server` | Native configuration repository | Các service lấy qua Config Server, không sở hữu business data này. |
| Service registry metadata | `eureka-server` | Eureka runtime registry | Gateway và service dùng Eureka client. |

Không có shared business database. `MS-01` cấm direct cross-service database access.

## 8. REQUEST FLOW

```text
Client
  | external bearer token + request
  v
API Gateway
  | validate external identity, create internal identity, resolve instance via Eureka
  v
user-service API
  | application use case
  v
domain model and repository contract
  | persistence adapter
  v
user-db
```

Gateway là edge và security boundary; `user-service` chịu trách nhiệm business authorization, use case và persistence thuộc Identity and Access context. Flow authentication chi tiết thuộc [security.md](security.md).

## 9. SECURITY ARCHITECTURE

```text
Client -- external JWT --> API Gateway -- internal JWT --> Downstream servlet service
```

Gateway xác thực external identity rồi tạo internal identity có thời hạn ngắn. Downstream servlet service tự validate internal token trước khi tạo security context; raw identity header không phải trust source. `common-security` dùng cho downstream servlet security, còn Gateway giữ reactive security chain riêng.

Policy về token, authorization, secret, CORS, logging và review trigger nằm tại [security.md](security.md).

## 10. DDD ARCHITECTURE

- Identity and Access là bounded context duy nhất được triển khai ở thời điểm hiện tại.
- User-service sở hữu domain model, business invariant, repository contract và persistence của context này.
- Context không chia sẻ aggregate, repository hoặc database với service khác.
- Communication liên context trong tương lai phải qua explicit contract theo `MS-02`; không có cross-context business communication đang tồn tại.

## 11. CLEAN ARCHITECTURE

Business service hiện theo hướng dependency vào core:

```text
API entry point -> Application use case -> Domain model / contracts
Infrastructure adapter ------------------> Domain contracts
```

API chịu HTTP mapping/validation, application điều phối use case, domain giữ business invariant, và infrastructure thực thi persistence detail. Đây là intended architecture; tên package không phải contract bắt buộc cho mọi service mới.

Current implementation có một giới hạn đã biết: application layer của user-service vẫn dùng một số Spring annotation và dependency framework. Điều này không thay đổi intended dependency direction nhưng chưa phải Clean Architecture thuần.

## 12. INFRASTRUCTURE ARCHITECTURE

- **Config Server:** Nguồn configuration tập trung, chạy native repository trong local deployment.
- **Eureka Server:** Service registry để Gateway/service không hardcode target instance.
- **API Gateway:** Edge HTTP/routing/security component chạy reactive stack.
- **PostgreSQL:** Database hiện do user-service sở hữu và Flyway quản lý schema migration.
- **Docker Compose:** Mô tả local multi-container stack gồm database, Config Server, Eureka, Gateway và user-service.

Không có cache, message broker, object storage hoặc monitoring platform được xác minh. Thứ tự startup thực tế là Config Server, Eureka, Gateway, rồi business service.

## 13. EXTERNAL SYSTEMS

| External System | Được sử dụng bởi | Mục đích | Protocol/Adapter |
| --- | --- | --- | --- |
| Không có external system được xác minh | N/A | N/A | N/A |

## 14. ARCHITECTURAL PATTERNS

- Maven multi-module microservices baseline.
- DDD bounded context cho business domain hiện có.
- Clean Architecture direction với API, application, domain và infrastructure responsibility tách biệt.
- Repository contract/adapter cho persistence.
- API Gateway edge pattern.
- Service Discovery qua Eureka.
- Centralized Configuration qua Config Server.
- Shared technical security module cho downstream service.

Event-driven architecture, outbox, CQRS, event sourcing, cache-aside và external integration adapter chưa được xác minh là pattern hiện tại.

## 15. ARCHITECTURAL DECISIONS

| Decision | Bằng chứng hiện tại | Lý do |
| --- | --- | --- |
| Tách business service khỏi infrastructure service | Maven modules, Compose và source tree | DDD/Clean Architecture/Microservices là architectural direction đã xác nhận. |
| Centralized configuration | Config Server và native configuration repository | Tránh phân tán runtime configuration giữa các service. |
| Discovery-based routing | Eureka client/server và Gateway `lb://` route | Không hardcode business service instance tại Gateway. |
| Gateway-signed internal identity | Gateway security và common-security source | Tách external client credential khỏi downstream service trust boundary. |
| Service-owned database | User service migration/database và Constitution `MS-01` | Giữ data ownership theo bounded context. |
| Shared security chỉ cho cross-cutting concern | `common-security` module và Constitution `MS-03` | Tránh share business domain giữa service. |

Không có ADR/RFC được xác minh trong repository. Với quyết định không có lý do ghi trên, lý do: Chưa xác định từ repository.

## 16. KNOWN ARCHITECTURAL GAPS

- Chỉ có một business bounded context và một business database đang được triển khai; service mới chưa tồn tại trong source tree.
- Application layer của user-service có Spring framework dependency, nên implementation không hoàn toàn Clean Architecture thuần.
- Chưa có asynchronous messaging, external integration, cache, object storage, observability platform hoặc CI/CD workflow được track.
- `graphify-out` có tham chiếu stale đến security source đã bị xóa; source tree hiện tại là nguồn chính xác khi graph mâu thuẫn source.

Các điểm trên là trạng thái/gap có bằng chứng, không phải yêu cầu kiến trúc tương lai.

## 17. CÁC NỘI DUNG CẦN LÀM RÕ

- Production deployment topology, cloud/on-prem target, scaling model và disaster recovery.
- Messaging/event strategy, external integration direction, cache, object storage và observability stack.
- API versioning, contract compatibility policy và service-to-service communication ngoài Gateway route hiện tại.
- CI/CD, architecture test, contract test và release process.
- Khi có thêm business service: bounded context, data ownership, route contract và dependency cần được xác định bằng module/feature spec.

## 18. RELATED GLOBAL SPECS

- [constitution.md](constitution.md)
- [security.md](security.md)
- `data-governance.md`: chưa tồn tại.
- `api-standards.md`: chưa tồn tại.
