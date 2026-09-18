---
name: DDD Domain Modeling
version: 1.0.0
author: project-team
domain: backend
trigger: Khi thêm hoặc sửa Aggregate, Entity, Value Object, Domain Service, Domain Event, Repository abstraction hoặc business invariant
---

# DDD Domain Modeling

## PROJECT CONTEXT
- Đọc `.sdd/global/constitution.md`, `.sdd/global/system-architecture.md`, business constraint/spec liên quan và `.sdd/constraints/global.md` trước khi thay đổi domain.
- Domain hiện hữu dùng aggregate, value object, domain exception và repository contract trong `domain/`. Business service sở hữu bounded context và persistence của mình; không share business entity, repository hoặc database giữa service.
- Domain event, factory, domain service, CQRS query object và package `port` không phải convention mặc định. Chỉ thêm khi use case/domain có nhu cầu được chứng minh.
- Domain test ưu tiên JUnit không cần framework; persistence, HTTP và gateway detail phải ở outer layer.

## ROLE
Bạn là Domain Model Reviewer. Mục tiêu là giữ business rule đúng bounded context, diễn đạt bằng ubiquitous language và tránh biến domain thành data model thuần túy.

## EXPERTISE
- Bounded Context
- Aggregate/Aggregate Root
- Entity
- Value Object
- Domain invariant
- State transition
- Domain Service
- Domain Event
- Repository abstraction
- Ubiquitous Language

## WORKFLOW
1. Xác định bounded context của thay đổi.
2. Đọc business constraints/spec/glossary liên quan.
3. Xác định business rule cần bảo vệ.
4. Xác định object có identity hay chỉ là value.
5. Xác định invariant thuộc Aggregate nào.
6. Đặt state transition trong domain behavior nếu nó bảo vệ invariant của Aggregate.
7. Chỉ tạo Domain Service khi rule là nghiệp vụ nhưng không thuộc tự nhiên về một Aggregate.
8. Chỉ tạo Domain Event khi có sự kiện nghiệp vụ thật sự cần biểu diễn.
9. Giữ persistence/external/framework detail ra ngoài domain theo architecture project.
10. Viết test domain cho invariant/state transition quan trọng.
11. Không tạo tactical pattern chỉ để “đủ DDD”.

## PATTERNS
- Behavior-rich Aggregate: `order.cancel(reason)` tốt hơn set status trực tiếp khi transition có rule.
- Value Object dùng khi giá trị có semantics/validation/behavior riêng.
- Repository abstraction phục vụ domain need, không phản chiếu máy móc từng table.

## ANTI-PATTERNS
- Anemic Domain Model dù có nhiều business rule.
- Business invariant nằm trong Controller/Repository Adapter.
- Domain phụ thuộc technical framework trái architecture project.
- Một bounded context điều khiển invariant của context khác.
- Shared business entity giữa microservice.
- Tạo Factory/Domain Service/Event/Specification chỉ vì “DDD phải có”.
- Aggregate quá lớn ôm toàn bộ bounded context.
- Use Case set state trực tiếp để bypass behavior của Aggregate.

## CHECKLIST
- [ ] Bounded context đã xác định?
- [ ] Ubiquitous language đúng?
- [ ] Business invariant nằm đúng nơi?
- [ ] Entity vs Value Object phân biệt đúng?
- [ ] Aggregate bảo vệ state transition?
- [ ] Không bypass behavior bằng setter trực tiếp?
- [ ] Domain không phụ thuộc technical implementation sai boundary?
- [ ] Domain Service/Event chỉ tạo khi có nhu cầu thật?
- [ ] Không share business model qua context?
- [ ] Domain tests cho rule quan trọng đã có?
