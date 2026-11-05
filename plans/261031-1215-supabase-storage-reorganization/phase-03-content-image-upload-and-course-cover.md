---
phase: 3
title: "Content image upload and course cover"
status: pending
priority: P1
effort: "1.5d"
dependencies: [1]
---

# Phase 3: Content image upload and course cover

## Phân công

- **Codex**, theo `briefs/codex-phase-03-course-cover.md` (backend và frontend). Claude review.
- Đây là phase đầu tiên trong chuỗi `ContentMediaController`: các phase 4 và 7 phải chờ phase này merge.

## Overview

Thêm upload ảnh cho tác giả nội dung (content-service) và dùng nó cho **ảnh bìa khóa học**: R&D chọn file trong form khóa học, file lên bucket `media`, DB chỉ giữ khóa. Thay quyết định "chỉ dán URL" của plan `261030-1200-course-cover-description`.

## Requirements

- Functional: `POST /api/content/admin/media/image` (multipart, quyền `CONTENT_AUTHOR`, như upload audio) nhận jpeg/png/webp tối đa 2 MB, kiểm theo **byte** không tin `Content-Type`, lưu `content-image/{userId}/{uuid}.{ext}`, trả `{key, url, contentType, sizeBytes}`. Form khóa học (admin) chọn/đổi/xóa ảnh bìa bằng upload, có xem trước. Learner vẫn nhận `coverImageUrl` là URL đầy đủ.
- Non-functional: không thêm bucket; không kiểm tra loại file bằng đuôi; SVG bị từ chối. Khóa không chứa `..`; URL ngoài hệ thống cũ vẫn đọc được (không backfill).

## Architecture

Quyết định đã chốt (phiên validate 1, lưu khóa thay vì URL đầy đủ): cột `courses.cover_image_url` (`VARCHAR(500)`, V86) lưu **khóa** (`content-image/…`) hoặc `https://` URL cũ. Content-service resolve ra URL đầy đủ khi trả `CourseResponse` và `TopicSequenceResponse` bằng `MediaReferencePolicy.resolve(AssetType.IMAGE, …)` (đã hỗ trợ khóa khi có base URL). Learning-service nhận URL đã resolve và chuyển tiếp; phải xác minh nó không tự ghép/kiểm tra lại (`ListCoursesUseCase`, `learningAdapters.ts`). Tên cột giữ nguyên để tránh đổi tên liên-service.

Port lưu file: hiện `ContentAudioStorage` + `ContentAudioStorageAdapter` + `StoredAudioResult` mang tên audio nhưng thân hàm không có gì riêng audio. Đổi tên thành `ContentMediaStorage` / `StoredMediaResult` (cơ học, không sao chép lớp); giữ nguyên hợp đồng HTTP của `/audio` (`StoredAudioResponse`).

## Related Code Files

- Create: `application/usecase/UploadContentImageUseCase.java` (policy jpeg/png/webp, 2 MB, `ObjectKeys.forUser("content-image", …)`), `api/dto/response/StoredImageResponse.java`, test tương ứng.
- Modify (content-service): `ContentMediaController` (thêm `POST /image`), `ContentAudioStorage*` → `ContentMediaStorage*` và `UploadContentAudioUseCase`, `Course.normalizeCoverImageUrl` (nhận khóa hợp lệ ngoài `http(s)`), `CreateCourseRequest` / `UpdateCourseRequest` (`@Pattern` hiện chỉ cho `https?://`), `CourseResponse`, `TopicSequenceResponse`, các mapper/adapter dựng 2 response này, `config-repo/content-service.yaml` nếu cần.
- Modify (frontend): `src/features/admin/courses/components/CourseForm.tsx`, `src/features/admin/courses/types.ts`, thêm hàm `uploadContentImage` cạnh `uploadAudio` trong `src/features/admin/packages/api/packageApi.ts` (hoặc tách `adminMediaApi.ts` nếu cả hai feature dùng), `learningAdapters.ts` chỉ kiểm tra.
- Cập nhật plan cũ: ghi chú superseded trong `plans/261030-1200-course-cover-description/plan.md`.
- Không tạo migration: cột đủ chỗ cho khóa.

## Implementation Steps

1. **Đọc trước:** `ContentMediaController`, `UploadContentAudioUseCase`, `ContentAudioStorageAdapter`, `Course.java` (quanh `normalizeCoverImageUrl`), `CreateCourseRequest`, `CourseResponse`, `SectionContentEditor.tsx` (mẫu upload audio) và `packageApi.ts:82`.
2. **Test trước (TDD):** `UploadContentImageUseCaseTest` (từ chối SVG, >2 MB, rỗng, byte không phải ảnh dù khai `image/png`; chấp nhận jpeg/png/webp; khóa đúng tiền tố và đuôi theo nội dung thật); `ContentMediaControllerTest` cho `/image` (201, 400 với file rỗng); test phân quyền (không phải `CONTENT_AUTHOR` bị 403, như `ContentMediaAuthorizationTest`); test `Course` (nhận khóa hợp lệ, `https://` cũ, từ chối `javascript:`, `..`, đường dẫn tuyệt đối, null/blank thành null); test response (khóa → URL đầy đủ; tham chiếu hỏng → `null`, không làm hỏng cả danh sách).
3. Đổi tên port/adapter/result sang `ContentMediaStorage` / `StoredMediaResult` và sửa `UploadContentAudioUseCase` + test hiện có; chạy test content-service để chắc không đổi hành vi audio.
   <!-- Updated: Validation Session 1 - liệt kê đủ file bị đổi tên (đã grep) -->
   Tám file bị đụng (đã grep): `ContentMediaController`, `StoredAudioResponse`, `ContentAudioStorage`, `StoredAudioResult`, `UploadContentAudioUseCase`, `ContentAudioStorageAdapter`, `ContentMediaControllerTest`, `UploadContentAudioUseCaseTest`. `StoredAudioResponse` và hợp đồng HTTP của `/audio` giữ nguyên.
4. Thêm `UploadContentImageUseCase`, endpoint `/image`, DTO. Giới hạn 2 MB đặt hằng số trong use case (nguồn chuẩn, xem phase 2).
5. Mở `Course.normalizeCoverImageUrl` và `@Pattern` của request: chấp nhận `https://…` hoặc khóa khớp mẫu khóa của `MediaReferencePolicy` (không scheme, không `/` đầu, không `..`). Giữ giới hạn 500 ký tự.
6. Resolve khi đọc: trong tầng dựng `CourseResponse` và `TopicSequenceResponse`. Khi base URL trống mà tham chiếu là khóa, ghi log cảnh báo một lần và trả `null` (không ném lỗi làm hỏng danh sách khóa học).
7. Kiểm learning-service chuyển tiếp nguyên URL (không kiểm `http(s)` lại, không ghép base URL lần hai); nếu có, sửa.
8. Frontend: trong `CourseForm`, thay ô URL bằng điều khiển chọn file (`accept=image/png,image/jpeg,image/webp`), xem trước, nút xóa; gọi `uploadContentImage` rồi đặt `coverImageUrl` thành **khóa** trả về, gửi lên khi lưu. Hiển thị lỗi 400/413 bằng thông báo hiện có của form. Theo mẫu overlay admin của dự án (create/edit trong drawer). Test FE: chọn file hợp lệ gọi API và gửi khóa; xóa ảnh gửi `null`; hiển thị lỗi.
9. **Rà dữ liệu cũ** (chạy trên `content_db`, chỉ đọc): `select code, cover_image_url from courses where cover_image_url is not null;`. URL ngoài vẫn hoạt động; liệt kê cho R&D để tự upload lại. **Không** tự tải ảnh của bên thứ ba về bucket (bản quyền, hotlink).
10. Cập nhật tài liệu: dòng `content-image/` và endpoint mới vào bảng bucket (phase 2); sửa dòng "Không làm: upload file" và quyết định "URL dán vào" trong plan `261030-1200` thành superseded.
11. Chạy: `mvn -B -ntp -pl services/content-service -am test`, `mvn -B -ntp -pl services/learning-service -am test`, test FE của `admin/courses`.

## Success Criteria

- [ ] Upload ảnh hợp lệ trả 201 và object xuất hiện trong `media` dưới `content-image/{userId}/`.
- [ ] SVG, file >2 MB, file giả đuôi ảnh bị từ chối; người không phải `CONTENT_AUTHOR` bị 403.
- [ ] Tạo/sửa khóa học bằng upload; `GET` admin và `GET /api/learning/courses` trả URL đầy đủ từ Supabase; để trống thì `null` và FE dùng lại `CourseArt`.
- [ ] Khóa học còn URL cũ vẫn hiển thị.
- [ ] Test audio hiện có vẫn xanh sau khi đổi tên port.
- [ ] Plan `261030-1200` có ghi chú superseded.

## Risk Assessment

- Base URL trống khi chạy ngoài compose (`config-repo/content-service.yaml` mặc định rỗng): bìa biến mất lặng lẽ. Trong compose, mặc định nay là URL Supabase thật (phase 1). Giảm bằng log cảnh báo ở bước 6 và bước kiểm base URL ở phase 1.
- Hai plan cùng sửa `CourseForm`/`Course`: plan `261030-1200` thực tế đã đổ vào repo; làm phase này trên nhánh có các thay đổi đó, kiểm tra xung đột trước.
- Ảnh bìa quá lớn làm chậm trang: giới hạn 2 MB; nếu cần, tối ưu kích thước ở FE trước khi gửi (không làm trong phase này, YAGNI).
- Object mồ côi khi tác giả upload rồi không lưu: chấp nhận theo quyết định "để mồ côi, dọn sau" (chưa có job dọn); đo sau khi dùng thực tế.
- Rollback: endpoint và field mới thêm vào, không xóa gì; revert commit là đủ, dữ liệu khóa cũ vẫn là chuỗi hợp lệ nhưng sẽ bị trình kiểm tra cũ từ chối khi sửa khóa học, nên revert phải kèm kiểm tra các khóa học đã lưu khóa.
