# SECURITY SPECIFICATION - code-base

Version: 1.0.0
Status: ACTIVE
Last updated: 2026-09-18
Applies to: All services, shared modules, configuration, feature specifications, developers, AI agents and pull requests

## 1. MỤC ĐÍCH VÀ PHẠM VI

Đây là Global Security Spec của dự án. Nó cụ thể hóa các security invariant trong `SEC-01`, `SEC-02`, `SEC-03`, `DATA-01` và `DATA-02` của [Constitution](constitution.md), nhưng không thay thế Constitution.

Mọi module spec và feature spec có thay đổi liên quan đến security MUST tuân file này. Khi có xung đột, Constitution thắng; conflict phải được giải quyết trước khi implementation bắt đầu.

## 2. SECURITY PRINCIPLES

- Protected behavior được deny-by-default: endpoint chỉ public khi được khai báo rõ trong cấu hình.
- Identity và authority chỉ được tin sau khi credential/token đã được server verify; client-controlled header hoặc claim chưa verify không phải identity hợp lệ.
- Input được validate tại trust boundary, còn domain invariant không chỉ dựa vào API validation.
- Secret, credential, reusable token và trust material không được xuất hiện trong source, artifact, API response hoặc log.
- Credential cho các trust boundary độc lập phải tách biệt.

## 3. TRUST BOUNDARIES

```text
External client
    | HTTP request + external bearer token
    v
API Gateway
    | verified identity + short-lived internal bearer token
    v
Business service
    | service-owned persistence access
    v
Service database

Configuration and delivery environment
    | configuration values and secret references
    v
Runtime components
```

| Boundary | Dữ liệu đi qua | Identity được tin từ đâu | Trách nhiệm validation |
| --- | --- | --- | --- |
| Client -> Gateway | Request, bearer token, path/query/body | Không tin dữ liệu client trước khi verify | Gateway xác thực token ngoài, áp dụng allowlist/public route và CORS policy. |
| Gateway -> Business service | Internal bearer token, correlation context, request | Token nội bộ đã được downstream verify | Downstream validate signing material, issuer, expiry, subject format và role canonical trước khi tạo security context. |
| Business service -> Database | Domain state, PII, credential hash | Service-local persistence contract | Service chịu trách nhiệm authorization, domain invariant và query binding trước persistence. |
| Configuration -> Runtime | URL, issuer, credential và secret reference | Cấu hình theo môi trường, không phải input client | Runtime property validation fail fast khi thiếu hoặc không hợp lệ ở các security property đã kiểm tra. |

Không có external identity provider, webhook, message broker hoặc trust boundary integration khác được xác minh trong repository hiện tại.

## 4. AUTHENTICATION

- Hệ thống hiện dùng stateless bearer JWT. HTTP Basic và form login bị tắt ở Gateway và downstream servlet security; CSRF bị tắt trong context bearer-token stateless.
- Identity service hiện phát external access token có issuer, subject, expiry và roles. Gateway validate external token trước khi route protected request.
- Gateway chỉ chấp nhận external token có issuer đúng, subject là UUID và role thuộc canonical set hiện hành; token hết hạn bị từ chối bởi validator JWT.
- Gateway phát internal JWT ngắn hạn cho downstream sau khi external identity đã được verify. Downstream protected endpoint chỉ xây dựng security context từ internal token đã verify, không dùng trực tiếp external token của client.
- Public endpoint là allowlist theo configuration. Các endpoint còn lại yêu cầu authentication.
- Refresh credential được dùng để cấp lại token theo flow hiện có; nó phải còn usable và user phải ở trạng thái cho phép authentication.
- Không có SSO, OAuth provider bên thứ ba, session server-side hoặc audience policy được xác minh. Cần làm rõ nếu các capability này được thêm vào.

## 5. AUTHORIZATION

- Authenticated không đồng nghĩa authorized. Authorization MUST được kiểm tra phía server cho protected action.
- Authority được tạo từ role claim đã validate; role ngoài canonical set bị từ chối. Role mới là thay đổi security và contract, cần security review.
- API có ownership policy MUST kiểm tra ownership bằng security context đã verify hoặc authority phía server. Test hiện có chứng minh một số action identity dùng policy administrator/owner.
- Client MUST NOT gửi role hoặc user identity qua raw header để yêu cầu quyền. Downstream lấy current user từ security context đã được verify.
- Cross-service authorization dựa trên internal identity đã verify; mỗi service vẫn chịu trách nhiệm authorization đối với behavior và dữ liệu mình sở hữu.

## 6. INPUT VALIDATION VÀ OUTPUT SAFETY

- API request hiện được validate bằng request validation; configuration security properties cũng được validate khi binding.
- Domain value object, aggregate và use case phải tiếp tục bảo vệ business invariant sau API validation.
- Persistence access hiện đi qua JPA/repository abstraction. Native query hoặc integration mới MUST bind input thay vì ghép input không tin cậy vào query.
- Error response phải có cấu trúc nhất quán và MUST NOT trả stack trace, secret, credential hoặc security detail không cần thiết cho client. Current user-service handler trả lỗi generic cho unexpected exception.
- Repository hiện không có file upload, message payload hoặc external callback handler. Các input type này cần được bổ sung threat model và validation rule trong feature/module spec trước khi dùng.

## 7. SECRET VÀ CREDENTIAL MANAGEMENT

Secret bao gồm signing material, database credential, password, refresh credential, access token và bất kỳ token/key có thể tái sử dụng nào.

- Secret được cung cấp bằng environment/configuration theo môi trường. Các biến security hiện hữu gồm `EXTERNAL_JWT_SECRET`, `GATEWAY_INTERNAL_JWT_SECRET` và database password variable; giá trị thực không được đặt trong spec này.
- Signing material của external token và internal token là hai trust material tách biệt.
- Runtime hiện fail fast với security property thiếu, không Base64 hoặc không đủ độ dài theo validator được dùng cho signing key.
- Secret không được hardcode, commit, log, đưa vào image/artifact hoặc response, kể cả khi phục vụ local test. Test chỉ được dùng fixture không có giá trị production.
- Rotation, revocation procedure, production secret manager và quyền truy cập secret chưa được xác minh. Cần làm rõ.

## 8. PASSWORD VÀ AUTHENTICATION CREDENTIAL

- Plaintext password không được persist. Implementation hiện tại hash/verify password bằng Spring `PasswordEncoder`; implementation baseline đang dùng BCrypt. Tên thuật toán không được coi là constitutional invariant nếu chưa có amendment.
- Login trả cùng loại lỗi cho email/password không hợp lệ hoặc account không active, giảm thông tin hỗ trợ enumeration.
- Refresh credential hiện được hash trước persistence, có trạng thái revoke/expiry và bị consume khi refresh để phát hành credential mới. Reusable credential không được log hoặc trả lại ngoài flow response được thiết kế.
- Độ dài credential, rotation policy production và account lockout/rate limit chưa được xác minh. Cần làm rõ.

## 9. DATA PROTECTION VÀ PRIVACY

- Identity data hiện lưu PII gồm email, tên và số điện thoại; password và refresh credential được lưu dưới dạng hash.
- Response, log và telemetry chỉ được chứa dữ liệu cần cho behavior hoặc observability. `SEC-03` cấm lộ sensitive data không cần thiết.
- Mỗi service sở hữu persistence của mình theo `MS-01`; service khác không được truy cập trực tiếp database đó.
- Encryption at rest, transport TLS production, PII masking, retention, deletion policy và access policy cho log chưa được xác minh. Cần làm rõ.
- Chưa có `.sdd/global/data-governance.md` trong repository tại thời điểm viết. Khi file này xuất hiện, data lifecycle policy phải được đặt ở đó và security.md chỉ tham chiếu phần security liên quan.

## 10. API SECURITY

- API Gateway là edge security boundary hiện tại. Gateway chịu trách nhiệm external JWT validation, CORS policy, structured authentication/authorization error và route protection.
- CORS được cấu hình tại Gateway theo frontend origin cấu hình và local development origin pattern hiện có. Origin mới phải được review như thay đổi security configuration.
- Gateway và downstream dùng explicit public allowlist; protected behavior ngoài allowlist phải yêu cầu token hợp lệ.
- Health/info exposure được cấu hình riêng. Bất kỳ actuator hoặc management endpoint mới nào phải được review về public exposure.
- Không có rate limiting, WAF, trusted-proxy policy hoặc API version security policy được xác minh. Cần làm rõ trước khi phụ thuộc vào chúng.

## 11. MICROSERVICE VÀ INTERNAL SECURITY

- Gateway không được coi internal network là trust proof; downstream phải tự validate internal token trước khi tin identity.
- Gateway thay external authorization bằng internal bearer token cho downstream path được cấu hình. Internal token tối giản chỉ mang identity/authority đã được verify và có expiry ngắn hạn theo cấu hình.
- Shared security module tập trung validator, canonical role, authority mapping và current-user access cho downstream servlet service. Gateway giữ reactive security chain riêng.
- Shared library không được trở thành nơi chứa business authorization của bounded context; service sở hữu action vẫn phải ra authorization decision của mình.
- Không có messaging hoặc service-to-service credential model khác được xác minh. Cần làm rõ khi thêm communication mechanism mới.

## 12. LOGGING, ERROR VÀ OBSERVABILITY

- Gateway tạo hoặc forward `X-Correlation-Id`; user service ghi nhận correlation context cho request. Correlation ID có thể dùng để trace flow nhưng không thay thế authentication.
- Các filter hiện log method, URI/path, status, duration và error information; chúng không log request body hoặc authorization header trong source đã kiểm tra.
- Password, token, credential, secret và PII không cần thiết MUST NOT xuất hiện trong log, error response hoặc telemetry. Query parameter có thể bị log tại Gateway, vì vậy feature không được đặt sensitive value trên URL nếu chưa có redaction được review.
- Error response phải tránh stack trace và credential. Current user-service handler trả generic message cho unexpected error; thay đổi gateway error mapping phải được security review để không phát lộ detail từ upstream.
- Log access control, retention, central aggregation và alerting/SIEM chưa được xác minh. Cần làm rõ.

## 13. DATABASE VÀ PERSISTENCE SECURITY

- Database credential được cấu hình ngoài source; secret value không được ghi vào migration hoặc data fixture.
- Database thuộc service boundary. Direct cross-service database access bị cấm bởi `MS-01`.
- Schema change sử dụng migration; migration đã applied không được sửa, xóa hoặc rewrite theo `DATA-01`.
- Credential và dữ liệu nhạy cảm phải được persist theo policy ở phần 8 và 9. Không có encryption-at-rest policy hoặc database access-role policy được xác minh. Cần làm rõ.

## 14. EXTERNAL INTEGRATION SECURITY

Không có external provider, webhook, API key integration hoặc file storage integration được xác minh trong source/documentation hiện tại.

Khi thêm external integration, feature/module spec phải xác định trust boundary, credential source, input/signature validation, authorization impact, failure behavior và security tests trước khi implementation.

## 15. DEPENDENCY VÀ SUPPLY-CHAIN SECURITY

- Dependency mới phải tuân approved stack và dependency-management pattern hiện có; security-sensitive dependency cần được review.
- Không có dependency vulnerability scanner, SBOM, lockfile policy hoặc CI supply-chain gate được xác minh.
- Khi dependency mới xử lý authentication, cryptography, serialization, file, network hoặc data access, security review là bắt buộc.

## 16. SECURITY TESTING VÀ ENFORCEMENT

| Policy | Enforcement hiện tại | Mức tự động |
| --- | --- | --- |
| Signing property hợp lệ | Runtime key/property validation và property tests | Partially automated |
| JWT issuer, subject, role và expiry validation | Gateway/downstream validator và validator tests | Partially automated |
| Protected route, authority, ownership và header spoofing | Spring Security configuration và controller security tests | Partially automated |
| Password/login/refresh credential behavior | Password encoder, use-case tests và refresh-token integration behavior | Partially automated |
| Migration integrity | Migration tool khi chạy và PostgreSQL integration test | Partially automated |
| Secret trong source, artifact và log | Code review / Agent validation | Manual; candidate for secret/log scanning |
| API error leakage và CORS change | Code review / Agent validation | Manual |
| Dependency vulnerability review | Dependency review | Manual; candidate for vulnerability scanning |
| Security gate trong CI | Không có CI workflow được track | Candidate for automation |

## 17. SECURITY REVIEW TRIGGERS

Security review là bắt buộc khi thay đổi:

- authentication, authorization, role/claim hoặc current-user access;
- token issuance, validation, refresh/revocation hoặc credential persistence;
- public endpoint, CORS, gateway/security configuration hoặc actuator exposure;
- password, secret, PII, log/error content hoặc database credential;
- external integration, webhook, file upload, message consumer hoặc service-to-service mechanism;
- dependency xử lý security-sensitive behavior;
- database schema có dữ liệu nhạy cảm hoặc migration có destructive impact.

## 18. SECURITY CHECKLIST CHO FEATURE

- [ ] Trust boundary và dữ liệu đi qua đã được xác định.
- [ ] Input được validate tại đúng boundary; domain invariant vẫn được bảo vệ.
- [ ] Authentication và authorization phía server phù hợp với action.
- [ ] Client-controlled identity, header hoặc claim chưa verify không được tin.
- [ ] Public endpoint, CORS và management exposure đã được review khi có thay đổi.
- [ ] Secret, credential và PII không bị hardcode, log, trả về error hoặc commit.
- [ ] Password/reusable credential được xử lý theo phần 8.
- [ ] Error response không lộ security/internal detail không cần thiết.
- [ ] Security test phù hợp đã chạy hoặc lý do giới hạn đã được document.
- [ ] Feature spec tuân Constitution và security.md; conflict đã được giải quyết trước implementation.

## 19. COMPLIANCE REQUIREMENTS

Hiện chưa xác định compliance framework bắt buộc.

GDPR, PCI-DSS, HIPAA, ISO 27001, SOC 2 hoặc requirement tương tự không được giả định chỉ từ loại dữ liệu hay best practice. Cần team xác nhận trước khi biến chúng thành project requirement.

## 20. CÁC ĐIỂM CẦN LÀM RÕ

- Production secret manager, key rotation, revocation procedure và secret access ownership.
- Transport TLS production, encryption at rest và database/log access control.
- Rate limiting, WAF, trusted proxy/header policy và account lockout policy.
- PII retention, deletion, masking, data governance và log retention.
- Security review owner, penetration testing process và incident response process.
- Dependency vulnerability scanning, SBOM và CI security gate.
- External identity provider, messaging, webhook, file upload hoặc external integration security model nếu các capability này được thêm.
