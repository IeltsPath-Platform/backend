---
phase: 1
title: "Content band metadata"
status: completed
priority: P1
dependencies: []
effort: "~3h"
---

# Phase 1: Content band metadata

## Overview
Topic và KP có khoảng band IELTS để AI Learning lọc phạm vi path (D1). Content là nơi duy nhất giữ luật kế
thừa: KP không ghi đè thì dùng band của topic. API trả sẵn band hiệu lực, nên AI Learning không phải tự suy ra.

## Context
- `topics`: `code`, `name`, `parent_topic_id`, `sort_order`, `status`. `knowledge_points`: `kind`, `learning_type`,
  `skill`, `description`, `status`. Chưa có trường band hay level nào.
- Migration Content hiện đến `V4__seed_main_flow_content.sql`. Migration mới là `V5`.
- `TopicTreeResponse` và `KnowledgePointResponse` là DTO AI Learning đang đọc qua `ContentServiceClient`.
- Seed `V4` (topic `DEMO_READING`) để band trống, nghĩa là "mọi band".

## Requirements
- Functional:
  - `V5__add_band_ranges.sql`:
    - `topics.band_min`, `topics.band_max`, `knowledge_points.band_min`, `knowledge_points.band_max`.
    - Kiểu `NUMERIC(2,1)`, nullable.
    - CHECK: giá trị nằm trong 0.0–9.0, bội số của 0.5, và `band_min ≤ band_max` khi cả hai có mặt.
  - Band hiệu lực của KP: KP có khoảng riêng (ít nhất một đầu khác null) thì dùng **trọn** khoảng đó, ngược lại
    dùng khoảng của topic. *(Đổi khi cook: kế thừa từng đầu độc lập có thể ghép ra khoảng ngược, ví dụ KP
    `min=8.0` dưới topic `max=7.0`.)* Topic con **không** kế thừa band của topic cha (ghi rõ trong README).
  - `CreateTopicRequest`, `UpdateTopicRequest` và `CreateKnowledgePointRequest` nhận `bandMin`/`bandMax`
    tùy chọn, validate cùng quy tắc với CHECK.
  - `TopicTreeResponse` trả `bandMin`, `bandMax`. `KnowledgePointResponse` trả `bandMin`, `bandMax` (giá trị
    riêng của KP) và `effectiveBandMin`, `effectiveBandMax`.
- Non-functional:
  - Chỉ thêm trường; không đổi hay bỏ trường cũ. Client cũ không bị ảnh hưởng.
  - Tính band hiệu lực không gây N+1 query khi liệt kê KP.

## Related Code Files
- Create: `services/content-service/src/main/resources/db/migration/V5__add_band_ranges.sql`
- Create: `.../domain/vo/BandRange.java` (value object: validate và `inheritFrom(BandRange topic)`)
- Modify: `domain/aggregate/Topic`, `domain/aggregate/KnowledgePoint`, các JPA entity, mapper
- Modify: `api/dto/request/{CreateTopicRequest,UpdateTopicRequest,CreateKnowledgePointRequest}.java`
- Modify: `api/dto/response/{TopicTreeResponse,TopicResponse,KnowledgePointResponse}.java`, các result record
- Modify: `application/usecase/{CreateTopicUseCase,UpdateTopicUseCase,CreateKnowledgePointUseCase,GetKnowledgePointsUseCase}.java`
- Modify: `services/content-service/README.md`
- Tests: `BandRangeTest`, `GetKnowledgePointsUseCaseTest` (kế thừa), `KnowledgePointControllerTest`,
  `TopicControllerTest` (hoặc tạo mới), migration test Testcontainers `BandRangeMigrationTest`

## Implementation Steps
### Tests Before
1. Chạy gate Content, ghi mốc (hiện 38 test).
2. Viết test **fail trước**:
   - `BandRangeTest`: nhận 4.0–6.5; từ chối 9.5, 4.3 và min > max; KP có khoảng riêng thay trọn khoảng của topic.
   - Migration test (Testcontainers): Flyway đến V5, CHECK từ chối `band_min = 7.0, band_max = 5.0`; dữ liệu
     V4 vẫn hợp lệ với band NULL.
   - `GetKnowledgePointsUseCase`: KP không ghi đè thì `effective*` bằng band topic; KP ghi đè một đầu thì chỉ
     đầu đó khác; topic không có band thì `effective*` là null.
   - Controller: tạo topic với band sai → 400; response có `bandMin`/`effectiveBandMin`.
### Refactor
3. Migration V5, value object, domain, entity và mapper.
4. Use case và DTO. Tính band hiệu lực trong một lần đọc: nạp topic của các KP theo lô, hoặc JOIN.
### Tests After
5. Toàn bộ test Content pass, gồm test controller và migration.
### Regression Gate
6. Gate chung; `git diff --check`; `graphify update .`.

## Success Criteria
- [ ] Flyway áp V5 trên DB có dữ liệu V4 mà không lỗi.
- [ ] API topic và KP trả band thô và band hiệu lực đúng luật kế thừa.
- [ ] Band sai bị từ chối ở cả API (400) và DB (CHECK).
- [ ] README Content mô tả ý nghĩa band và luật kế thừa.

## Risk Assessment
- **Bước 0.5:** IELTS band dùng bước 0.5. Nếu nhóm muốn dùng CEFR thì đổi value object, không đổi luồng.
- **Topic con không kế thừa topic cha:** đơn giản hơn nhưng nhập liệu nhiều hơn. Có thể bổ sung sau mà không đổi API.
