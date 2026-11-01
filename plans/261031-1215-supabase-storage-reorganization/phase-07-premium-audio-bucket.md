---
phase: 7
title: "Premium audio bucket"
status: pending
priority: P2
effort: "2d"
dependencies: [1, 3, 6]
---

# Phase 7: Premium audio bucket

## Phân công

- **Chưa giao Codex.** Phase này chưa đủ chi tiết để code. Trước khi có brief, Claude làm và bạn quyết:
  1. **Kiểm kê endpoint** phát `mediaUrl` của audio: lập bảng (endpoint, gọi bởi ai, qua gateway hay nội bộ, có cổng paywall không). Đã biết: `GetLessonContentUseCase`, `GetPackageVersionContentUseCase`, `GetAuthorPackageUseCase`, `GetPackageVersionPreviewUseCase`, `SectionMediaUseCases`; cần thêm phía assessment (`StartAssessmentAttemptUseCase`, `AttemptStructureResponse`) và learning.
  2. **TTL URL ký:** plan ghi 3600 giây, nhưng `common-storage` giới hạn tối đa 900 giây. Chọn một: giữ 900 và FE tải lại khi hết hạn (khuyến nghị), hoặc đổi giới hạn module.
  3. **Tên bucket** `media-premium` (hoặc tên khác) và **danh sách audio PREMIUM** (quyết định sản phẩm).
- Brief đã viết: `briefs/codex-phase-07-premium-audio.md`. Bị chặn bởi quyết định D1 (ls1 dùng chung bài miễn phí và bài trả phí) và D2 (tên bucket). TTL đã chốt 900 giây.
- Kiểm kê đã làm: đường phát audio cho học viên là `GET /api/learning/lessons/{id}`; đề thử giữ công khai. Trước phase 7: phase 3 phải merge (vì cùng sửa `ContentMediaController`).

**Trạng thái (2026-10-31):** bucket `media-premium` đã tạo (riêng tư, 25 MB, audio). `listening/demo/ls1.mp3` đã copy sang đó; URL ký (900 giây) phát được `audio/mpeg`; URL công khai của bucket riêng tư bị chặn. Bản công khai `media/listening/demo/ls1.mp3` vẫn còn, chờ code phase 7 (Codex) rồi mới xóa.

## Overview

Tạo bucket riêng tư `media-premium` cho audio của bài trả phí sau paywall (plan `261030-1204-band-skill-lesson-paywall`). Audio PREMIUM chỉ phát qua URL ký có hạn do content-service cấp; URL công khai của chúng ngừng hoạt động. Hiện paywall khóa bài ở tầng API (`PAYWALL`) nhưng audio nằm bucket công khai với khóa đoán được (`listening/demo/ls1.mp3`), nên biết URL là nghe được.

## Requirements

- Functional: mỗi `content_assets` audio có `access_tier` (`PUBLIC` mặc định hoặc `PREMIUM`). Với PREMIUM, mọi nơi content-service trả `mediaUrl` đều trả URL ký (TTL cấu hình, mặc định 1 giờ) từ bucket `media-premium`. Tác giả chọn tầng khi upload audio. Learner chưa mở khóa không nhận được URL của bài khóa.
- Non-functional: không thay đổi hành vi cho audio PUBLIC; khóa trong DB giữ nguyên (bucket chọn theo tầng, không đổi `media_reference`); không phát URL ký qua đường không có cổng paywall.

## Architecture

- `MediaReferencePolicy` (domain, thuần) giữ nguyên cho PUBLIC. Thêm bộ resolve ở tầng application, ví dụ `MediaUrlResolver`: PUBLIC → `policy.resolve`; PREMIUM + khóa tương đối → `ObjectStorage.signedDownloadUrl(premiumBucket, key, ttl)` (đã có trong `shared/common-storage`); URL `https://` đầy đủ không đổi. Mọi nơi gọi `mediaPolicy.resolve(AssetType.AUDIO, …)` chuyển sang bộ resolve mới kèm `access_tier`.
- Cổng an toàn: ký URL ở content-service chỉ an toàn nếu các endpoint trả `mediaUrl` hoặc là nội bộ (learning-service gọi sau khi `PAYWALL` đã kiểm: `LessonContentResponse`, `PackageVersionContentResponse` nằm ở `api/dto/internal`), hoặc chỉ dành cho tác giả. Gateway chỉ định tuyến `/api/content/**` (`api-gateway.yaml`), không định tuyến `/internal/**`. Việc đầu tiên của phase là liệt kê **mọi** endpoint `/api/content/**` có thể trả `mediaUrl` và xác nhận từng cái hoặc không trả audio PREMIUM hoặc đã nằm sau cổng.
- Phân loại PREMIUM (đã chốt ở phiên validate 1): audio chỉ dùng trong bài ở vị trí sau 3 bài miễn phí của mỗi (band, kỹ năng) là PREMIUM, dựa `skill`/`skillPosition` mà content-service đã tính cho paywall, đối chiếu với kết quả audit nội dung của plan paywall (phase 6, đã xong; xem báo cáo nghiệm thu của plan đó). Placement, 3 bài đầu và đề luyện tập (PT-LISTENING-01/02/03) giữ PUBLIC.
  <!-- Updated: Validation Session 1 - phân loại PREMIUM đã chốt; liệt kê nơi dùng MediaReferencePolicy -->
- Nơi dùng `MediaReferencePolicy` trong content-service (đã grep, 7 nơi cộng bean): `SectionMediaUseCases`, `GetAuthorPackageUseCase`, `GetPackageVersionContentUseCase`, `GetPackageVersionPreviewUseCase`, `GetLessonContentUseCase` (2 chỗ gọi `resolve`), `GetWritingTaskUseCase`, `SearchPracticeTestsUseCase` (ảnh bìa, không phải audio), và bean `ContentMediaConfig`. Chỉ các nơi resolve `AssetType.AUDIO` cần đi qua bộ resolve mới.
- Chưa xác minh: việc bắt đầu đề luyện tập ở assessment-service (`StartAssessmentAttemptUseCase`, trả `audio.mediaUrl` qua `AttemptStructureResponse`) có đi qua `PaywallGate` của learning-service không. Vì đề luyện tập giữ PUBLIC nên không chặn, nhưng bước 1 vẫn phải truy vết để chắc không có đường nào phát URL ký của audio PREMIUM mà không qua cổng.

## Related Code Files

- Create: migration `V92__content_asset_access_tier.sql` (số kiểm lại): `access_tier VARCHAR(10) NOT NULL DEFAULT 'PUBLIC' CHECK (access_tier IN ('PUBLIC','PREMIUM'))` trên `content_assets`; `MediaUrlResolver` + port ký URL; test.
- Modify (content-service): mọi nơi gọi `MediaReferencePolicy.resolve` cho AUDIO (tìm bằng `grep -rn "mediaPolicy\|MediaReferencePolicy" src/main`), `JdbcLearningContentReader`, các mapper của `LessonContentResult` / `PackageVersionContentResult` / `AuthorPackageResponse` / `PackageVersionPreviewResponse`, `ContentMediaController` + `UploadContentAudioUseCase` (tham số `tier`, mặc định PUBLIC; PREMIUM lưu vào bucket premium), `ContentAudioStorage*` (đã đổi tên ở phase 3, thêm bucket theo tầng), `config-repo/content-service.yaml`, `docker-compose.yml` (`CONTENT_PREMIUM_MEDIA_BUCKET` mặc định `media-premium`, `CONTENT_PREMIUM_URL_TTL_SECONDS` mặc định 3600).
- Modify (frontend): trình phát audio xử lý URL hết hạn (tải lại payload khi lỗi 403/hết hạn); form tác giả chọn tầng khi upload audio (`SectionContentEditor.tsx`).
- Tham khảo: plan `261030-1204` (phase 2, 4, 6), `PracticeTestController` (audio của đề luyện tập), `ContentPackageClient` của assessment-service (`Audio.mediaUrl`).

## Implementation Steps

1. **Liệt kê điểm phát `mediaUrl`** (bảng: endpoint, gọi bởi ai, qua gateway hay nội bộ, có cổng paywall chưa). Mở rộng cho assessment-service (luyện tập/đề) và learning-service. Kết quả là phụ lục của phase; nếu có đường công khai không có cổng, nó là điều kiện chặn.
2. **Chốt tập audio PREMIUM** bằng truy vấn trên `content_db` kết hợp `skillPosition` của paywall; trình người dùng danh sách (khóa, bài, kỹ năng) trước khi dán nhãn. Placement, 3 bài miễn phí đầu mỗi (band, kỹ năng) và đề luyện tập mặc định PUBLIC cho tới khi sản phẩm xác nhận khác.
3. **Test trước:** `MediaUrlResolver` (PUBLIC → URL `/object/public/`, PREMIUM → URL có `/object/sign/` và token, URL `https://` ngoài không đổi, tham chiếu hỏng → lỗi cũ); test API: learner chưa mở khóa không nhận URL của bài khóa; tác giả upload audio với `tier=PREMIUM` lưu vào bucket premium; không-tác-giả bị 403.
4. Migration thêm `access_tier`; mặc định PUBLIC nên **deploy code trước, hành vi chưa đổi**.
5. Thêm resolver, nối vào mọi điểm phát đã liệt kê; cấu hình bucket premium và TTL. Thêm `tier` vào upload audio.
6. **Tạo bucket** `media-premium` (hỏi người dùng rồi chạy): `public = false`, 25 MB, `audio/mpeg, audio/mp4, audio/wav, audio/ogg`.
7. **Chuyển file theo thứ tự an toàn:** (a) sao chép từng khóa PREMIUM sang `media-premium` giữ nguyên khóa; (b) kiểm URL ký phát được; (c) đặt `access_tier = 'PREMIUM'` cho các tài sản đó (migration dữ liệu hoặc công cụ tác giả); (d) xác nhận bài phát được; (e) xóa bản trong `media`. Kiểm `curl` URL công khai cũ của khóa đó trả lỗi.
8. Frontend: trình phát tải lại khi URL ký hết hạn; chọn tầng ở form upload audio.
9. Tài liệu phase 2: thêm dòng `media-premium`; mô tả quy tắc tầng và TTL.
10. Chạy test content-service, learning-service, assessment-service và FE liên quan.

## Success Criteria

- [ ] Phụ lục liệt kê điểm phát `mediaUrl`; không có đường công khai nào phát URL của audio PREMIUM mà không qua cổng paywall.
- [ ] Audio PREMIUM phát được bằng URL ký; URL công khai cũ trả lỗi; hết hạn thì FE tải lại và phát tiếp.
- [ ] Audio PUBLIC (placement, 3 bài đầu) hành vi không đổi.
- [ ] Học viên chưa mở khóa gọi API bài khóa nhận `PAYWALL`, không nhận URL.
- [ ] Tác giả upload được audio PREMIUM; test các service xanh.

## Risk Assessment

- **Rò URL ký:** URL ký có thể chia sẻ trong thời hạn TTL; chấp nhận, TTL ngắn; không cache URL ký phía server lâu hơn TTL.
- **Đường không có cổng:** rủi ro chính, xử lý bằng bước 1 là điều kiện chặn.
- **TTL ngắn hơn phiên học:** bài nghe dài hơn TTL bị ngắt giữa chừng; mặc định 1 giờ, chỉnh theo độ dài bài dài nhất; FE tải lại khi 403.
- **Cache payload ở FE** (staleTime của react-query) lâu hơn TTL: đặt staleTime của payload có audio nhỏ hơn TTL hoặc tải lại khi lỗi.
- **Phân loại sai:** gán nhầm audio miễn phí vào PREMIUM làm 3 bài đầu mất tiếng; kiểm từng bài miễn phí phát được ở bước 7(d).
- **Phụ thuộc:** audit nội dung của plan paywall đã xong; vẫn phải tự lập danh sách audio PREMIUM ở bước 2 từ `content_db` hiện tại, vì số bài mỗi (band, kỹ năng) có thể đã đổi sau `V89`.
- Rollback: đặt lại `access_tier = 'PUBLIC'` và sao chép file về `media` (giữ bản trong premium tới khi xác nhận ổn định, chỉ xóa bản công khai ở bước 7(e) sau khi kiểm).
