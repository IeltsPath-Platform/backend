---
phase: 3
title: "Architecture tests in CI"
status: pending
priority: P2
dependencies: []
---

# Phase 3: Test kiến trúc chạy ở CI (ArchUnit)

## Overview
Các quy tắc layer và pattern trong `AGENTS.md` §3.1, §3.9, §5 hiện chỉ là chữ, chờ người review bắt. Phase này biến
những quy tắc kiểm tĩnh được thành test, chạy trong `mvn verify` của CI. Vi phạm có sẵn được chốt thành danh sách, chỉ
**vi phạm mới** làm CI đỏ.

## Requirements
- Dependency `com.tngtech.archunit:archunit-junit5`, **scope `test`**, phiên bản khai báo một lần ở `dependencyManagement`
  của root `pom.xml` (cần người dùng duyệt, `AGENTS.md` §2).
- Mỗi service Java nghiệp vụ có `src/test/java/com/ieltspath/<svc>/ArchitectureTest.java` dùng chung bộ luật. Bộ luật đặt
  trong module test dùng chung nếu gọn (ví dụ thư mục test-jar của `shared/common-security`); không được thì lặp ngắn ở
  từng service, không tạo module shared mới chỉ cho việc này.
- Luật (kiểm tĩnh được):
  1. `domain` không phụ thuộc `org.springframework..`, `jakarta.persistence..`, `org.hibernate..`, `..api..`,
     `..application..`, `..infrastructure..`.
  2. `application` không phụ thuộc `..api..`, `..infrastructure..`.
  3. `api` không phụ thuộc `..infrastructure..persistence..` hay Spring Data repository.
  4. `RabbitTemplate`/`AmqpTemplate` chỉ được dùng trong `..infrastructure..`.
  5. `RestClient`, `RestTemplate`, `WebClient` chỉ được dùng trong `..infrastructure..`; lớp ở `..infrastructure..client..`
     dùng chúng phải phụ thuộc `ResilientCall` (hoặc `common-llm` sau plan nâng cấp chấm Writing).
  6. Không gọi Supabase Storage ngoài `common-storage` (lớp nào dùng client HTTP với host Supabase: kiểm theo phụ thuộc vào
     lớp của `common-storage`, không theo chuỗi).
  - Theo convention từng service (`AGENTS.md` §3.1): user-service dùng `domain/repository`, không có `port`; luật viết theo
    package có thật, không ép cấu trúc mới.
- **Không** kiểm được tĩnh, giữ cho review: "không gọi HTTP/LLM trong transaction hoặc khi giữ khóa". Ghi rõ trong test
  (comment) và `AGENTS.md` để không ai tưởng đã có máy kiểm.
- Vi phạm có sẵn: `FreezingArchRule`, store commit ở `src/test/resources/archunit_store/` của từng service; tắt tự ghi
  thêm vi phạm mới ở CI (`freeze.store.default.allowStoreUpdate=false` trong `archunit.properties` khi chạy CI).
- `AGENTS.md` §6 và `CLAUDE.md` §4: lệnh chạy, cách cập nhật store khi **sửa** vi phạm cũ (store chỉ được nhỏ đi).

## Related Code Files
- Modify: root `pom.xml`, `pom.xml` của 9 service nghiệp vụ, `AGENTS.md`, `CLAUDE.md`
- Create: `ArchitectureTest` mỗi service, `archunit.properties`, store vi phạm

## Implementation Steps
1. Thêm dependency, viết luật cho một service (learning) trước, chạy, xem vi phạm.
2. Phân loại vi phạm: thật (đưa vào store), hay luật viết sai (sửa luật).
3. Nhân ra 8 service còn lại; tạo store.
4. `mvn -B -ntp verify` cả reactor; cố ý thêm một vi phạm mới ở nhánh nháp để xác nhận CI đỏ, rồi bỏ.
5. Báo cáo: số vi phạm cũ theo service và theo luật, để làm việc dọn sau.

## Success Criteria
- [ ] CI xanh với code hiện tại; một vi phạm mới làm CI đỏ.
- [ ] Báo cáo có số vi phạm cũ theo service và luật.

## Risk Assessment
- Luật quá chặt gây nhiều vi phạm giả: bắt đầu với 6 luật trên, không thêm luật phong cách.
- Store bị cập nhật âm thầm làm mất tác dụng: CI không cho ghi store; review PR nhìn diff store.
