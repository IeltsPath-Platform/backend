---
phase: 1
title: "Khôi phục build contract"
status: pending
priority: P1
dependencies: []
---

# Phase 1: Khôi phục build contract

## Overview

Đưa `user-service` vào Maven reactor và khiến Dockerfile chỉ tham chiếu module
thực có. Không triển khai logic nghiệp vụ trong phase này.

## Requirements

- Root Maven phải nhận `services/user-service` là module hợp lệ.
- Parent coordinates của service phải khớp root POM (`com.group01:code-base`).
- Docker image build không được `COPY` các service/shared module không tồn tại.

## Related Code Files

- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\pom.xml`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\pom.xml`
- Modify: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\services\user-service\Dockerfile`
- Modify later only if needed: `C:\Users\pduy8\OneDrive\Desktop\microservices-starter-template\docker-compose.yml`

## Implementation Steps

1. Chạy Maven từ root để tái hiện parent/module failure; ghi nhận output trước
   khi sửa để phân biệt lỗi repo hiện hữu với lỗi phase sau.
2. Thay parent artifact của service từ `clinic-appointment-system` thành
   `code-base`; giữ version/groupId theo root, không đổi public artifactId trừ
   khi consumer thực tế yêu cầu.
3. Thêm `services/user-service` vào `<modules>` của root POM.
4. Thêm `spring-security-test` với scope `test` để Phase 2 có thể viết security
   regression tests trước khi sửa access policy. Testcontainers để Phase 5 thêm.
5. Rút Dockerfile về đúng reactor hiện hữu: root POM, `common-security` và
   user-service. Không giữ `COPY` tới patient/doctor/payment/notification,
   `common-events`, `common-web` khi thư mục không tồn tại.
6. Bổ sung build context/documentation Compose chỉ khi image đã build thành
   công; không tạo service giả cho profile provider chưa có trong repository.

## Todo List

- [ ] Maven parent/module contract hợp lệ.
- [ ] Dockerfile chỉ copy module tồn tại.
- [ ] Security test dependency sẵn sàng cho Phase 2.
- [ ] Build không dùng artifact cũ trong `target/` làm bằng chứng thành công.

## Success Criteria

- [ ] `mvn -pl services/user-service -am -DskipTests compile` thành công từ root.
- [ ] `mvn -pl services/user-service -am test` tìm và chạy test service.
- [ ] `docker build -f services/user-service/Dockerfile .` qua được Maven dependency layer.

## Risk Assessment

- User service hiện có thể là nhánh chưa được add vào root; trước khi commit,
  xác nhận team muốn module thuộc reactor này.
- Docker cache có thể che file `COPY` sai; build phải chạy clean CI context.
