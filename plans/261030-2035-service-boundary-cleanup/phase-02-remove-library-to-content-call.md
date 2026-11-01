---
phase: 2
title: "Gỡ lời gọi library → content"
status: pending
priority: P1
effort: "1-1.5 ngày"
dependencies: [1]
---

# Phase 02: Gỡ lời gọi library → content

## Overview

Giữ một chiều content → library (content cần dữ liệu từ vựng khi soạn bài), bỏ chiều library → content. Library hiện kiểm topic của video bằng `GET /api/content/topics/{id}`. Quyết định D1: bỏ kiểm tra, library lưu `topicId` như tham chiếu.

## Requirements

- `CreateLearningVideoUseCase` không còn phụ thuộc `TopicLookup`; vẫn nhận và lưu `topicId` như trước.
- Xóa `ContentTopicClient`, port `TopicLookup`, khối cấu hình `library.content` và biến `CONTENT_SERVICE_URL` dành cho library.
- Hành vi API cũ: yêu cầu tạo video với `topicId` không tồn tại trước đây bị từ chối; từ nay được chấp nhận miễn đúng định dạng UUID. Ghi sự thay đổi này trong contract/README. Frontend soạn video chọn topic từ danh sách do content trả nên id sai chỉ xảy ra khi gọi API trực tiếp.
- Game vẫn gọi library và content: không đổi.

## Tests first (red)

- `CreateLearningVideoUseCaseTest`: bỏ mock `TopicLookup`; thêm test tạo video với `topicId` hợp lệ về định dạng thì lưu được; `topicId` null (nếu hiện cho phép) giữ nguyên hành vi; định dạng không phải UUID bị từ chối ở lớp request như trước.
- Xóa `ContentTopicClientTest` cùng lớp.
- Test cấu trúc: không còn bean implement `TopicLookup` và không còn lớp client trỏ tới content trong library-service (một test đơn giản quét classpath hoặc đọc cây file là đủ).
- Test cấu hình: `library-service.yaml` không còn khóa `library.content.base-url` (đỏ trước, xanh sau).

## Related Code Files

- Xóa: `library-service/.../infrastructure/client/ContentTopicClient.java`, `application/port/TopicLookup.java`, `ContentTopicClientTest.java`.
- Sửa: `application/usecase/CreateLearningVideoUseCase.java`, `CreateLearningVideoUseCaseTest.java`.
- Sửa (cấu hình tập trung, commit riêng): `infra/config-server/config-repo/library-service.yaml` (bỏ khối `content:` dòng 48-49), `docker-compose.yml` (bỏ `CONTENT_SERVICE_URL` của library-service, dòng ~176, và `content-service` khỏi `depends_on` của library nếu chỉ vì lý do này, dòng ~181).
- Sửa tài liệu: `library-service/README.md` (dòng 57 mô tả kiểm topic, dòng 74 bảng biến `CONTENT_SERVICE_URL`), `docs/system-architecture.md` (bỏ hàng library → content), `AGENTS.md` dòng 125 (ví dụ "library → content" trong mục `X-Correlation-Id`).

## Implementation Steps

1. Viết test đỏ, sửa use case, xóa client và port.
2. Bỏ cấu hình, Compose và cập nhật tài liệu.
3. `mvn -B -ntp -pl services/library-service -am test`, `docker compose config --quiet`.

## Success Criteria

- [ ] Tìm trong `library-service` không còn lớp gọi content (`grep ContentTopicClient|TopicLookup|/api/content/topics` rỗng trong `src/main`).
- [ ] Test library xanh; Compose hợp lệ.
- [ ] Tài liệu không còn nhắc library → content.

## Risk Assessment

- Mất kiểm tra tồn tại topic ở backend: chấp nhận theo D1. Nếu sau này cần, kiểm ở frontend hoặc bước xuất bản.
- Rollback: revert commit (không có migration).
