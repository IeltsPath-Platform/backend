---
phase: 4
title: "Dựng course từ cây tri thức"
status: pending
priority: P1
dependencies: [1, 2, "plan 261016-0131 phase 1, 2"]
---

# Phase 4: Dựng course từ cây tri thức

## Overview

Mentor: "Course bám theo cây tri thức để tạo ra", không phải "có đề rồi import vào". Phase này tạo **bản nháp course** là
một cấu trúc dữ liệu (chưa phải course thật), cùng bộ kiểm tra "course này phủ cây đến đâu", và bước duyệt biến nó thành
course, topic, lesson, gói thật ở trạng thái nháp. Người soạn tay và AI (phase 5) dùng chung cấu trúc này.

## Requirements

- **Cấu trúc bản nháp course** (lưu trong `ai_drafts.result` với `kind = 'COURSE'`; người soạn tay dùng cùng bảng với
  `requested_by` là chính họ và không qua LLM, nên `model` null):

  ```json
  { "course": { "code": "IELTS_6_5_R", "name": "IELTS 6.5 Reading", "bandLevel": 6.5 },
    "topics": [{ "code": "…", "name": "…", "skill": "READING", "sortOrder": 1,
      "lessons": [{ "code": "…", "title": "…", "summary": "…", "knowledgePointIds": ["…"],
        "blocks": [{ "type": "TEXT", "text": "…", "knowledgePointIds": ["…"] },
                   { "type": "EXERCISE", "questionRefs": ["importRef or questionVersionId"] }],
        "practiceSetPackageIds": ["…"] }],
      "topicTestPackageId": "…" }],
    "courseTestPackageIds": ["…"],
    "practiceTestMilestones": [{ "packageId": "…", "afterTopicSequence": 2 }],
    "gaps": [{ "knowledgePointId": "…", "reason": "NO_PRACTICE_SET | FEW_PRACTICE_SETS | NO_EXERCISE_QUESTION" }] }
  ```

  Khối `TEXT` do người soạn viết hoặc để trống chờ người nhập; AI không sinh bài lý thuyết dài (ngoài phạm vi).
- **API soạn tay** (`CONTENT_AUTHOR`, `ADMIN`):
  - `POST /api/content/admin/course-drafts`: tạo bản nháp trống hoặc từ một cấu trúc có sẵn.
  - `PUT /api/content/admin/course-drafts/{id}`: lưu cấu trúc (kiểm tra cú pháp, chưa kiểm phủ).
  - `GET /api/content/admin/course-drafts/{id}/analysis`: kết quả kiểm phủ (bên dưới).
  - `POST /api/content/admin/course-drafts/{id}/approve`.
- **Kiểm phủ cây** (domain service `CourseCoverageAnalyzer`, thuần):
  - **Phủ KP:** mọi KP `ACTIVE` thuộc band và kỹ năng của course phải xuất hiện trong ít nhất một lesson; thiếu → cảnh báo
    `KP_NOT_COVERED`.
  - **Thứ tự tiên quyết:** nếu B có tiên quyết A thì A phải nằm ở lesson trước B trong cùng course, hoặc thuộc course có
    band thấp hơn; vi phạm → lỗi `PREREQUISITE_ORDER` kèm cặp KP.
  - **Đủ kho đề:** mỗi KP cần tối thiểu 3 gói `PRACTICE_SET` `PUBLISHED` chưa lộ (ngưỡng của plan cây, phase 2) và tối thiểu
    1 câu `EXERCISE`; thiếu → `gaps`.
  - **Gói hợp lệ:** gói được chọn phải `PUBLISHED`, đúng loại, và **chưa gắn** vào lesson, topic hoặc course khác (một gói
    chỉ thuộc một chỗ); trùng → `PACKAGE_ALREADY_LINKED`.
  - Cảnh báo (`KP_NOT_COVERED`, `gaps`) không chặn duyệt; lỗi (`PREREQUISITE_ORDER`, `PACKAGE_ALREADY_LINKED`, mã trùng,
    tham chiếu không tồn tại) chặn duyệt.
- **Duyệt:** trong một transaction, bằng các use case của phase 1 và use case course hiện có:
  - tạo course `INACTIVE`, topic `INACTIVE` (course và topic để người dùng xuất bản sau), lesson `DRAFT`, block, liên kết
    KP; gắn các gói đã chọn vào lesson, topic, course đúng vai trò;
  - đặt mốc practice test (bảng của plan cây, phase 8) nếu bản nháp có;
  - lỗi giữa chừng thì không tạo gì (tất cả hoặc không);
  - course mang `status = INACTIVE` nên **học viên không thấy** cho tới khi R&D kích hoạt thủ công.
- Mã course, topic, lesson trùng với cái đã có → `409 CODE_CONFLICT` kèm danh sách mã trùng.

## Related Code Files

(`C` = content package)

- Tạo: `C/domain/service/CourseCoverageAnalyzer.java`, `C/domain/vo/CourseDraftStructure.java` (record kiểm cú pháp),
  `C/application/usecase/{CreateCourseDraftUseCase, SaveCourseDraftUseCase, AnalyzeCourseDraftUseCase,
  ApproveCourseDraftUseCase}.java`, `C/api/controller/CourseDraftController.java`
- Sửa: use case và repository gói để gắn gói vào lesson, topic, course khi duyệt; kiểm tra các ràng buộc hiện có của
  `content_packages` (một gói thuộc đúng một chỗ); thêm truy vấn "gói chưa gắn" nếu chưa có
- Test:
  - `CourseCoverageAnalyzerTest` (bảng tình huống: KP không được phủ, B đứng trước A, KP thiếu gói, gói đã gắn nơi khác,
    tiên quyết ở course band thấp hơn thì hợp lệ);
  - `ApproveCourseDraftIntegrationTest` (Testcontainers: duyệt tạo đúng cấu trúc; lỗi giữa chừng thì không còn bản ghi
    nào; course `INACTIVE` không xuất hiện trong `topic-sequence` của learning).

## Implementation Steps

1. **Đỏ trước:** `CourseCoverageAnalyzerTest`.
2. Cấu trúc và kiểm cú pháp.
3. Analyzer.
4. Use case lưu, phân tích, duyệt; controller.
5. Integration test duyệt và kiểm tra learning không thấy course `INACTIVE`.
6. `mvn -q -pl services/content-service,services/learning-service -am test`.

## Success Criteria

- [ ] Phân tích trả đủ ba nhóm: phủ KP, thứ tự tiên quyết, thiếu kho đề.
- [ ] Duyệt là nguyên tử; course tạo ra là `INACTIVE`.
- [ ] Học viên không thấy course khi chưa kích hoạt.

## Risk Assessment

- Ràng buộc "một gói chỉ thuộc một chỗ" có thể hạn chế dùng lại đề giữa các course. Nếu cần dùng chung, phải đổi mô hình
  gói; ngoài phạm vi và cần duyệt kiến trúc. Kiểm tra ràng buộc thực tế của `content_packages` trước khi code và ghi vào
  báo cáo phase.
- Kiểm phủ phụ thuộc cây phase 1-2 của plan cây đã có dữ liệu thật.

## Bàn giao FE

Màn dựng course: kéo KP vào lesson, chọn gói, xem phân tích (cảnh báo và lỗi), nút duyệt.
