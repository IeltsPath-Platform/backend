# PROJECT CONSTITUTION - code-base

Version: 2.0.0
Status: LOCKED
Applies to: All developers, AI agents, specifications, services and pull requests

## Mục đích

Constitution xác định những invariant dài hạn bảo vệ security, data integrity và kiến trúc toàn dự án. Đây là luật cấp dự án, không phải mô tả implementation, hướng dẫn package hoặc danh sách feature hiện tại.

## Thứ tự ưu tiên

Khi có xung đột, thứ tự áp dụng là:

1. Constitution.
2. Constitution-approved Exception hoặc Amendment.
3. Approved Architecture Decision (ADR/RFC).
4. Feature Specification.
5. Implementation Preference.

ADR/RFC thông thường MUST NOT vi phạm Constitution. Feature specification mâu thuẫn Constitution phải được sửa hoặc có Constitution Exception hợp lệ trước khi implementation bắt đầu.

## LAYER 1 - HARD RULES

### SEC-01: Bảo vệ secret và trust material

**Mức:** Hard Rule

**Quy tắc:** Secret, credential, private key, reusable token và trust material MUST NOT được hardcode, commit, đưa vào artifact, log hoặc trả về API. Trust material cho các boundary độc lập MUST NOT được tái sử dụng; chúng MUST được cung cấp qua cơ chế cấu hình phù hợp theo môi trường.

**Lý do:** Rò rỉ hoặc tái sử dụng trust material làm mất boundary và mở rộng blast radius của sự cố.

**Phạm vi áp dụng:** Toàn hệ thống, mọi service, shared module, configuration, test và Agent.

**Enforcement:** Partially automated: runtime kiểm tra trust material được cấu hình. Manual review: bảo vệ source, environment và artifact. Candidate for automation: secret scanning trước khi merge.

**Ngoại lệ:** Không cho phép.

### SEC-02: Xử lý dữ liệu và identity không tin cậy

**Mức:** Hard Rule

**Quy tắc:** Input không tin cậy MUST được validate tại trust boundary phù hợp; domain invariant MUST NOT chỉ dựa vào validation ở entry point. Protected action MUST có authentication và authorization được kiểm tra phía server. Header hoặc claim chưa được verify MUST NOT được dùng làm identity hoặc quyền hợp lệ.

**Lý do:** Validation, authentication và authorization phía server ngăn dữ liệu không hợp lệ, giả mạo identity và leo thang quyền.

**Phạm vi áp dụng:** Mọi API, message consumer, background entry point và Agent.

**Enforcement:** Partially automated: security configuration và security tests hiện kiểm tra một phần protected behavior. Manual review: coverage của mọi entry point mới.

**Ngoại lệ:** Không cho phép.

### SEC-03: Bảo vệ dữ liệu nhạy cảm và credential đã lưu

**Mức:** Hard Rule

**Quy tắc:** Password MUST chỉ được lưu bằng approved adaptive one-way hashing. Reusable credential MUST chỉ được lưu dưới dạng giá trị một chiều phù hợp. Log, error response và telemetry MUST NOT tiết lộ password, token, credential hoặc dữ liệu nhạy cảm không cần thiết.

**Lý do:** Dữ liệu bị lộ không được trở thành credential có thể dùng lại hoặc gây lộ thông tin nhạy cảm.

**Phạm vi áp dụng:** Mọi thành phần xử lý identity, credential hoặc dữ liệu nhạy cảm.

**Enforcement:** Manual review / Agent validation. Candidate for automation: sensitive-data log scanning.

**Ngoại lệ:** Không cho phép.

### DATA-01: Tính bất biến của migration history

**Mức:** Hard Rule

**Quy tắc:** Migration đã được áp dụng MUST NOT bị sửa, xóa hoặc rewrite để xử lý validation hoặc checksum issue. Mọi thay đổi schema MUST đi qua migration mới theo convention của project.

**Lý do:** Migration history là bản ghi tái lập schema và bảo vệ tính toàn vẹn dữ liệu giữa các môi trường.

**Phạm vi áp dụng:** Mọi database và migration của service.

**Enforcement:** Partially automated: migration validation khi service hoặc integration test liên quan chạy. Manual review: quyết định thay đổi schema.

**Ngoại lệ:** Không cho phép.

### DATA-02: An toàn dữ liệu phá hủy

**Mức:** Hard Rule

**Quy tắc:** Destructive schema operation, destructive data operation hoặc việc sử dụng production-like personal data MUST NOT được thực hiện khi chưa có phê duyệt rõ ràng, phạm vi tác động và phương án khôi phục được document.

**Lý do:** Thay đổi không thể đảo ngược có thể làm mất dữ liệu hoặc gây lộ dữ liệu nhạy cảm.

**Phạm vi áp dụng:** Mọi migration, script vận hành, môi trường và Agent.

**Enforcement:** Manual review / Agent validation.

**Ngoại lệ:** Không cho phép.

## LAYER 2 - ARCHITECTURAL CONSTRAINTS

### ARCH-01: Dependency hướng vào core

**Mức:** Architectural Constraint

**Quy tắc:** Outer layer MUST depend inward. Core business model MUST NOT phụ thuộc HTTP, persistence, messaging, external SDK hoặc framework implementation. Khi core cần technical capability, core/application MUST phụ thuộc contract hướng vào trong; outer layer cung cấp implementation.

**Lý do:** Dependency direction giữ core độc lập với chi tiết kỹ thuật, có thể kiểm thử và thay thế adapter.

**Phạm vi áp dụng:** Mọi bounded context, service và integration.

**Enforcement:** Manual review / Agent validation. Candidate for automation: architecture dependency tests.

**Ngoại lệ:** Chỉ qua Constitution Exception được approve.

### DDD-01: Ownership của bounded context

**Mức:** Architectural Constraint

**Quy tắc:** Business rule, invariant và state transition MUST thuộc bounded context chịu trách nhiệm cho chúng. Một bounded context MUST NOT sở hữu hoặc điều khiển invariant của context khác chỉ vì tiện tái sử dụng. Tactical pattern DDD chỉ SHOULD được dùng khi domain cần.

**Lý do:** Ownership rõ ràng tránh coupling nghiệp vụ và model bị pha trộn.

**Phạm vi áp dụng:** Mọi domain model, use case, specification và service.

**Enforcement:** Manual review / Agent validation.

**Ngoại lệ:** Chỉ qua Constitution Exception được approve.

### CLEAN-01: Trách nhiệm của use case và adapter

**Mức:** Architectural Constraint

**Quy tắc:** Application layer MUST điều phối use case và transaction boundary khi cần. Controller, persistence adapter, external client, message publisher và gateway MUST NOT sở hữu core business invariant.

**Lý do:** Business behavior cần ở nơi có thể kiểm thử và tiến hóa mà không phụ thuộc delivery hoặc technical adapter.

**Phạm vi áp dụng:** Mọi entry point, use case, adapter và gateway.

**Enforcement:** Manual review / Agent validation.

**Ngoại lệ:** Chỉ qua Constitution Exception được approve.

### MS-01: Ownership dữ liệu theo service

**Mức:** Architectural Constraint

**Quy tắc:** Mỗi service MUST sở hữu persistence và schema thuộc bounded context của mình. Một service MUST NOT đọc, ghi hoặc join trực tiếp database thuộc service khác.

**Lý do:** Data ownership bảo vệ autonomy, deploy độc lập và boundary nghiệp vụ.

**Phạm vi áp dụng:** Mọi microservice, database, migration và integration.

**Enforcement:** Manual review / Agent validation.

**Ngoại lệ:** Chỉ qua Constitution Exception được approve.

### MS-02: Contract rõ ràng, model độc lập

**Mức:** Architectural Constraint

**Quy tắc:** Cross-service communication MUST đi qua contract rõ ràng. Aggregate, business entity và repository của một bounded context MUST NOT được share như model dùng chung giữa các service.

**Lý do:** Contract rõ ràng bảo vệ giao tiếp, còn model độc lập giúp mỗi bounded context tiến hóa theo nhu cầu riêng.

**Phạm vi áp dụng:** Mọi API, event, client, shared library và integration.

**Enforcement:** Manual review / Agent validation. Candidate for automation: contract compatibility tests.

**Ngoại lệ:** Chỉ qua Constitution Exception được approve.

### MS-03: Shared code và gateway chỉ phục vụ cross-cutting concern

**Mức:** Architectural Constraint

**Quy tắc:** Shared library MAY chỉ chứa cross-cutting concern thực sự dùng chung; nó MUST NOT chứa business logic của bounded context. Gateway MUST giới hạn ở edge hoặc cross-cutting responsibility và MUST NOT chứa domain business logic.

**Lý do:** Quy tắc này tránh distributed monolith và giữ ownership nghiệp vụ tại service phù hợp.

**Phạm vi áp dụng:** Shared module, gateway và mọi service.

**Enforcement:** Manual review / Agent validation.

**Ngoại lệ:** Chỉ qua Constitution Exception được approve.

### SPEC-01: Specification tuân thủ Constitution

**Mức:** Architectural Constraint

**Quy tắc:** Feature specification, plan, implementation và review MUST được kiểm tra với Constitution trước khi tiếp tục. Conflict MUST được giải quyết bằng sửa specification hoặc Constitution Exception hợp lệ trước khi implementation bắt đầu.

**Lý do:** Spec-Driven Development chỉ đáng tin cậy khi mọi thay đổi tôn trọng invariant toàn dự án.

**Phạm vi áp dụng:** Mọi developer, AI Agent, feature specification, plan, pull request và architectural decision.

**Enforcement:** Manual review / Agent self-check.

**Ngoại lệ:** Chỉ qua Constitution Exception được approve.

## LAYER 3 - ENGINEERING STANDARDS

### TEST-01: Kiểm chứng behavior theo rủi ro

**Mức:** Engineering Standard

**Quy tắc:** Business behavior mới hoặc thay đổi SHOULD có automated test phù hợp. Security boundary, persistence semantics, migration và concurrency-sensitive behavior SHOULD được kiểm chứng ở mức integration khi unit test không đủ bằng chứng.

**Lý do:** Test phải chứng minh behavior có rủi ro, không phải chạy theo coverage số lượng.

**Phạm vi áp dụng:** Mọi feature, bug fix, migration và security change.

**Enforcement:** Partially automated: unit, MVC security và PostgreSQL integration tests có thể chạy cục bộ. Manual review: phạm vi test. Chưa phát hiện CI workflow được track.

**Ngoại lệ:** Có thể override với lý do được document và reviewer phê duyệt, miễn không vi phạm Layer 1 hoặc Layer 2.

### API-01: Compatibility của contract công khai

**Mức:** Engineering Standard

**Quy tắc:** Public API và inter-service contract SHOULD duy trì compatibility. Khi breaking change là cần thiết, owner MUST document consumer impact, migration path và validation trước khi phát hành. Error response SHOULD nhất quán theo contract của service.

**Lý do:** Consumer cần thông tin và thời gian để chuyển đổi an toàn.

**Phạm vi áp dụng:** Mọi public API, internal API, event và client contract.

**Enforcement:** Manual review / Agent validation. Candidate for automation: API contract tests.

**Ngoại lệ:** Có thể override với lý do được document và reviewer phê duyệt, miễn không vi phạm Layer 1 hoặc Layer 2.

### OBS-01: Observability an toàn

**Mức:** Engineering Standard

**Quy tắc:** Request đi qua nhiều thành phần SHOULD giữ correlation context. Log, metrics và error SHOULD đủ để trace lỗi mà không vi phạm Hard Rule về dữ liệu nhạy cảm.

**Lý do:** Điều tra lỗi phân tán cần traceability nhưng không được đánh đổi bằng dữ liệu nhạy cảm.

**Phạm vi áp dụng:** Gateway, service, client, job và message flow.

**Enforcement:** Partially automated: correlation handling tồn tại ở một số request path. Manual review: coverage toàn hệ thống.

**Ngoại lệ:** Có thể override với lý do được document và reviewer phê duyệt, miễn không vi phạm Layer 1 hoặc Layer 2.

### PERF-01: Chi phí dữ liệu và xử lý phù hợp quy mô

**Mức:** Engineering Standard

**Quy tắc:** Data access và processing SHOULD được thiết kế phù hợp với quy mô dữ liệu dự kiến. Thay đổi có nguy cơ tạo chi phí tăng nhanh theo kích thước dữ liệu SHOULD được đo lường hoặc kiểm chứng trước khi phát hành.

**Lý do:** Hiệu năng và độ ổn định không được phụ thuộc vào kích thước dữ liệu thử nghiệm.

**Phạm vi áp dụng:** Mọi API, use case, repository và integration xử lý dữ liệu biến thiên.

**Enforcement:** Manual review / Agent validation. Candidate for automation: performance hoặc query-behavior integration tests.

**Ngoại lệ:** Có thể override với lý do được document và reviewer phê duyệt, miễn không vi phạm Layer 1 hoặc Layer 2.

### ENG-01: Delivery có bằng chứng

**Mức:** Engineering Standard

**Quy tắc:** Thay đổi SHOULD ưu tiên dependency và pattern đã được project chấp nhận khi đáp ứng nhu cầu. Mỗi thay đổi MUST được kiểm chứng bằng build, test hoặc validation hẹp nhất có ý nghĩa; documentation liên quan MUST được đồng bộ khi workflow, architecture, security posture hoặc public contract thay đổi.

**Lý do:** Thay đổi có bằng chứng giảm regression, dependency drift và kiến thức lỗi thời.

**Phạm vi áp dụng:** Mọi source code, dependency, configuration, documentation và pull request.

**Enforcement:** Partially automated: Maven validation có thể chạy cục bộ. Manual review: phạm vi validation và đồng bộ documentation. Chưa phát hiện CI workflow được track.

**Ngoại lệ:** Có thể override với lý do được document và reviewer phê duyệt, miễn không vi phạm Layer 1 hoặc Layer 2.

## QUY TRÌNH NGOẠI LỆ

### Layer 1

Không cho phép ngoại lệ. Thay đổi vi phạm Hard Rule phải bị từ chối.

### Layer 2

Chỉ được ngoại lệ khi có Constitution Exception được approve trước khi implementation, bao gồm:

1. Lý do rõ ràng.
2. Phân tích tác động.
3. ADR/RFC được đánh dấu rõ là `Constitution Exception`.
4. Architectural review.
5. Approval rõ ràng. Approver: Cần team làm rõ.
6. Phạm vi ngoại lệ cụ thể.
7. Thời hạn hoặc điều kiện kết thúc nếu phù hợp.

ADR/RFC thông thường không phải Constitution Exception và không thể override Layer 2.

### Layer 3

Có thể override với lý do được document và reviewer phê duyệt, miễn không vi phạm Layer 1 hoặc Layer 2.

## QUY TRÌNH THAY ĐỔI CONSTITUTION

Constitution có trạng thái LOCKED. Không được sửa trực tiếp chỉ để feature hiện tại dễ implement hơn.

Mọi amendment phải:

1. Nêu vấn đề cần giải quyết.
2. Nêu rule hiện tại và giải thích vì sao nó không còn phù hợp.
3. Phân tích impact đến security, data, architecture, service và specification.
4. Được review và approve. Approver: Cần team làm rõ.
5. Tăng version theo semantic-style versioning.
6. Cập nhật các artifact bị ảnh hưởng.
7. Ghi version history.

Versioning:

- MAJOR: thay đổi ý nghĩa hoặc loại bỏ nguyên tắc quan trọng.
- MINOR: thêm rule mới mà không phá semantics hiện tại.
- PATCH: sửa wording, clarification hoặc typo mà không đổi ý nghĩa.

## VERSION HISTORY

- v2.0.0 - Gộp rule trùng, đưa specification governance lên Layer 2, tổng quát hóa performance và chuẩn hóa hierarchy/exception.
- v1.0.0 - Constitution ban đầu.
