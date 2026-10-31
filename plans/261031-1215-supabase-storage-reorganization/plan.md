---
title: "Supabase Storage reorganization: buckets, covers, premium audio, Speaking upload"
description: "Mọi ảnh bìa, biểu đồ Writing và media nằm trên Supabase Storage (DB chỉ giữ khóa); tách media-premium cho audio sau paywall; upload ghi âm Speaking thật; đổi khóa Writing; chỉnh cấu hình bucket và nạp 20 file mp3 còn thiếu."
status: pending
priority: P2
branch: "feat/band-skill-paywall"
tags: [storage, supabase, covers, media, paywall, speaking]
blockedBy: []
blocks: []
created: "2026-10-31T05:16:55.717Z"
createdBy: "ck:plan"
source: skill
---

# Supabase Storage reorganization

Căn cứ: báo cáo bucket Supabase ngày 2026-10-31 (đối chiếu `storage.buckets` / `storage.objects` thật của project `kndrsdqzbzsaiuuruqno`) và rà code `backend/`, `frontend/` cùng ngày. `backend-boundary/`, `frontend-boundary/` là bản song song, không đụng.

## Overview

Hiện trạng cần sửa:

- 20 file mp3 Listening chưa upload (`listening/demo` 8, `listening/practice` 12) nên bài seed và 3 đề luyện Listening không phát được.
- Cấu hình bucket lệch code: `community` không giới hạn loại file; `media` 20 MB và chỉ nhận `audio/mpeg` trong khi code cho 25 MB và 4 loại audio.
- Ảnh bìa chưa thống nhất: bìa khóa học là URL dán tay; bìa đề luyện tập là 58 data URI SVG trong `content_assets.media_reference` (`V34`); bìa chủ đề/bộ từ vựng (`cover_key`) chưa có endpoint upload. Biểu đồ Writing (~28 chỗ trong `V11`, `V37`, `V38`, `V44`) cũng là data URI SVG trong DB.
- Ghi âm Speaking không được upload: `SpeakingSectionRunner.tsx:82` gửi chuỗi giả `local-recording:{attemptId}:{itemId}`.
- Audio bài trả phí nằm trong bucket công khai, có thể tải thẳng bỏ qua paywall.
- Khóa Writing sai chính tả `writtings/leassons/`.
- `docker-compose.yml` đặt mặc định `CONTENT_MEDIA_BASE_URL` là `https://media.example.invalid/`: thiếu biến thì khóa vẫn resolve ra URL hỏng, không báo lỗi.

## Quyết định đã chốt (người dùng, 2026-10-31)

| # | Quyết định |
|---|---|
| 1 | Tách `media-premium` (riêng tư, URL ký hạn ngắn) cho audio/tài liệu bài trả phí sau paywall; `media` giữ nội dung công khai |
| 2 | Giữ `speaking-recordings` (riêng tư, 5 MB) và làm upload ghi âm thật |
| 3 | Đổi `writtings/leassons` thành `writing/lessons` kèm migration; copy 4 ảnh |
| 4 | Bỏ `image/svg+xml` khỏi bucket `media` |
| 5 | **Mọi ảnh bìa lưu file trên Supabase, DB chỉ giữ khóa/tham chiếu**: bìa khóa học, bìa đề luyện tập, bìa chủ đề/bộ từ vựng. Bìa bộ thẻ flashcards (`coverImageKey`) đã đi qua upload sẵn |

Quyết định 5 đảo quyết định của plan `261030-1200-course-cover-description` ("ảnh là URL dán vào, không upload"). Người dùng đã chọn rõ; plan đó được ghi chú superseded.

Chốt thêm ở phiên validate 1 (chi tiết trong Validation Log):

| # | Quyết định |
|---|---|
| 6 | Bìa khóa học: DB lưu **khóa** (`content-image/{userId}/{uuid}.{ext}`), content-service resolve ra URL lúc đọc |
| 7 | Bìa đề luyện tập xuất **WebP 1280×800**; không giữ `image/svg+xml` |
| 8 | Audio PREMIUM = audio chỉ dùng ở bài sau 3 bài miễn phí của mỗi (band, kỹ năng). Placement, 3 bài đầu và đề luyện tập (PT-LISTENING-01/02/03) giữ PUBLIC |
| 9 | Ghi âm Speaking lưu **90 ngày**; chỉ chủ bài và examiner được giao nghe lại (URL ký 15 phút) |
| 10 | **Biểu đồ Writing** (data URI SVG) cũng chuyển lên Supabase, làm trong phase 4 |
| 11 | Object demo trong `avatars`/`flashcards`/`community`: kiểm tham chiếu rồi chỉ xóa phần không dùng |
| 12 | Đổi mặc định `CONTENT_MEDIA_BASE_URL` trong `docker-compose.yml` thành URL Supabase thật (`https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/`) |
| 13 | Thay hoặc xóa ảnh bìa: object cũ **để mồ côi**, không viết code xóa; dọn sau |

## Bố cục bucket đích

| Bucket | Chủ | Công khai | Giới hạn | Loại file | Ghi chú |
|---|---|---|---|---|---|
| `avatars` | user | Có | 2 MB | png, jpeg, webp | Giữ |
| `flashcards` | library | Có | 2 MB | png, jpeg, webp | Giữ; thêm tiền tố `catalog-cover/` cho bìa catalog (phase 5) |
| `community` | community | Có | 5 MB | png, jpeg, webp, gif | Thêm danh sách loại file |
| `media` | content | Có | 25 MB | mpeg, mp4, wav, ogg; png, jpeg, webp | Nâng 25 MB, thêm audio, bỏ svg; chứa bìa khóa học, bìa đề luyện tập, biểu đồ Writing |
| `media-premium` | content | **Không** | 25 MB | mpeg, mp4, wav, ogg | Mới (phase 7) |
| `speaking-recordings` | assessment | **Không** | 5 MB | webm, mp4, ogg, mpeg, wav | Giữ; có upload thật (phase 8) |

Quy tắc: cùng chủ và cùng chính sách thì dùng chung bucket, phân biệt bằng tiền tố khóa; khác chính sách thì bucket mới; khác service thì không chung bucket.

## Phases

| Phase | Name | Status | Phụ thuộc |
|-------|------|--------|-----------|
| 1 | [Bucket config cleanup and missing audio](./phase-01-bucket-config-cleanup-and-missing-audio.md) | Pending | - |
| 2 | [Docs and limit alignment](./phase-02-docs-and-limit-alignment.md) | Pending | 1 |
| 3 | [Content image upload and course cover](./phase-03-content-image-upload-and-course-cover.md) | Pending | 1 |
| 4 | [Practice test covers to storage](./phase-04-practice-test-covers-to-storage.md) | Pending | 1, 3 |
| 5 | [Vocabulary catalog covers](./phase-05-vocabulary-catalog-covers.md) | Pending | 1 |
| 6 | [Writing key rename](./phase-06-writing-key-rename.md) | Pending | 1, 4 |
| 7 | [Premium audio bucket](./phase-07-premium-audio-bucket.md) | Pending | 1, 3, 6; paywall audit |
| 8 | [Speaking recording upload](./phase-08-speaking-recording-upload.md) | Pending | 1 |

Tên phase 4 giữ như CLI đã tạo; phạm vi của nó gồm cả bìa đề luyện tập lẫn biểu đồ Writing (quyết định 10).

Giai đoạn A (cấu hình, không đổi hành vi): phase 1, 2. Giai đoạn B (đổi code/dữ liệu): phase 3 đến 8.

Chạy song song được: phase 5 (library-service) và phase 8 (assessment-service) độc lập với nhau và với chuỗi content-service. Chuỗi content-service **phải tuần tự** 3 → 4 → 6 → 7 vì cùng sửa `ContentMediaController`, `MediaReferencePolicy` và đánh số migration.

Số migration (kiểm tra lại thư mục `db/migration` ngay trước khi tạo file; plan khác có thể đã lấy số): content mới nhất `V89` (dự kiến phase 4 `V90`, phase 6 `V91`, phase 7 `V92`); library `V22` (phase 5 không cần); assessment `V19` (phase 8 dùng `V20`).

## Phân công và phối hợp (Claude và Codex làm song song)

Brief chi tiết cho Codex nằm trong `briefs/`: phase 2, 3, 4, 5, 6, 7, 8. Phase 4, 6, 7 có điều kiện chờ (xem mục quyết định bên dưới). Phase 4, 6, 7 có phần "Phân công" riêng trong file phase.

### Ai làm gì

| Việc | Người làm | Ghi chú |
| --- | --- | --- |
| Ghi Supabase (bucket, upload, sao chép, xóa) | Claude, sau xác nhận từng bước | Codex không có quyền này |
| Thu âm 20 mp3, upload bằng Dashboard | Bạn | Kịch bản: `recording-script-listening-audio.md` |
| Điền `SUPABASE_URL`, `SUPABASE_SECRET_KEY` trong `.env` | Bạn | Claude và Codex không đọc giá trị |
| Code và tài liệu: phase 2, 3, 5, 8 | Codex theo brief | Claude review từng PR |
| Migration và test: phase 4, 6 | Codex, sau khi Claude xuất danh sách id và sao chép file | |
| Xuất ảnh và kiểm kê DB: phase 4, 6 | Claude | Dùng `scratchpad` cho file tạm, không đưa vào repo |
| Thiết kế phase 7 (kiểm kê endpoint phát audio, TTL, phân loại PREMIUM) | Claude và bạn quyết định | Codex chỉ code sau khi có brief |
| Merge vào nhánh tích hợp và vào `main` | Bạn quyết định | |

### Nhánh và PR

- Backend tích hợp: `feat/supabase-storage-reorganization` (đã có). Frontend tích hợp: cùng tên, tạo từ `main` của repo `frontend/`.
- Mỗi brief Codex một nhánh con (`feat/storage-p<N>-<slug>`), PR vào nhánh tích hợp.
- Claude không sửa nhánh của Codex. Claude làm việc trên Supabase và file tạm; mọi thay đổi repo do Claude làm đi qua PR.

### Quyền sửa file (tránh xung đột)

| File | Ai được sửa | Thứ tự |
| --- | --- | --- |
| `docker-compose.yml` | Phase 1 (đã xong), phase 8 (khối `assessment-service` only) | 8 sau 1 |
| `ContentMediaController.java`, `ContentAudioStorage*`, `UploadContentAudioUseCase.java` | Phase 3 (đổi tên, thêm `/image`) | 3 trước 7 |
| `Course.java`, `CreateCourseRequest.java`, `UpdateCourseRequest.java` | Phase 3 | |
| `MediaReferencePolicy.java` | Phase 7 | sau 3 |
| `CourseForm.tsx`, `types.ts` | Phase 3 | |
| `frontend/src/features/admin/media/api/mediaUploadApi.ts` | Phase 3 tạo, phase 5 chỉ dùng | 3 trước 5 |
| `AdminVocabularyPage.tsx`, `vocabularySchemas.ts` | Phase 5 | |
| `SpeakingSectionRunner.tsx`, `useMicRecorder.ts` | Phase 8 | |
| Migration content | Số đã giữ: V90 phase 4, V91 phase 6, V92 phase 7 | |
| Migration assessment | V20, phase 8 | |
| Migration library | Không có | |

### Thứ tự

1. **Ngay khi bạn chốt:** Codex làm phase 2 và phase 3. Claude kiểm kê `library_db` (cho phase 5), kiểm kê endpoint phát audio (cho phase 7), chuẩn bị danh sách id bìa đề luyện tập (cho phase 4).
2. **Sau khi phase 3 merge:** Codex làm phase 5 và phase 8 (hai phase này độc lập với nhau). Claude xuất ảnh phase 4, upload file lên `media` và sao chép file phase 6.
3. **Sau khi Claude hoàn tất sao chép file:** Codex merge migration phase 4 và phase 6. Không merge migration trước khi file đã có trên bucket, vì khóa không có file sẽ làm ảnh hỏng.
4. **Phase 7:** Claude thiết kế, bạn quyết định, rồi Codex code.

### Quy tắc cho Codex

- Không ghi Supabase; không đọc hoặc in giá trị `.env`; không commit `.env`, khóa, URL ký.
- Không đặt số phase, mã plan trong code, comment, tên test, tên migration, commit message.
- Làm test trước, rồi code. Ghi lệnh test đã chạy và kết quả thật vào PR.
- Kết quả báo theo định dạng: `Status: DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT`, `Summary`, `Concerns`.
- Gặp mâu thuẫn với plan thì `NEEDS_CONTEXT`, không tự đổi quyết định đã chốt.

### Điều kiện nhận PR

- Test của module chạm vào đều xanh. Output thật được dán vào PR.
- Diff chỉ nằm trong danh sách file của brief.
- Claude review, kiểm tra không có secret và không có lệnh ghi Supabase.

### Trạng thái hiện tại

- Đã xong: bucket `community` và `media` (2026-10-31); RLS đã kiểm (không có policy trên `storage.objects`, RLS bật); mặc định `CONTENT_MEDIA_BASE_URL` trong compose (`f708ef9d`); README content (`de1bec41`); README common-storage (`1aca55e1`). Nhánh `feat/supabase-storage-reorganization` đã có các commit này.
- Chưa làm: 20 mp3, bucket `media-premium` (chờ quyết định TTL và tên), toàn bộ code phase 2 đến 8.

## Working notes cho người làm

- Đọc `backend/AGENTS.md` trước (phân lớp, quy tắc test, điều 6.7 và 6.8).
- Không đặt mã plan, số phase, nhãn audit trong comment, tên migration, tên test, commit. Mô tả hành vi trực tiếp. Commit theo conventional commits, không nhắc AI.
- Không chép khóa Supabase vào log, test, tài liệu, commit; chỉ tên biến môi trường (`SUPABASE_URL`, `SUPABASE_SECRET_KEY`). URL công khai của bucket và project ref không phải bí mật.
- Mọi thao tác ghi lên project Supabase thật (đổi `storage.buckets`, upload, xóa object) **phải hỏi lại người dùng ngay trước khi chạy**; plan này không cấp quyền sẵn.
- Test chạy một service: `mvn -B -ntp -pl services/<service> -am test` (Docker bật cho Testcontainers).
- Không sửa migration đã áp dụng (đổi checksum Flyway): luôn thêm migration mới.
- Báo cáo test thật, không ghi "đã chạy" cho kiểm tra chưa chạy.

## Phụ thuộc giữa các plan

- `261030-1200-course-cover-description` (status ghi pending nhưng `V86` và `CourseForm` đã có): phase 3 **thay quyết định "chỉ dán URL"** bằng upload; sửa cùng `CourseForm.tsx`, `Course.normalizeCoverImageUrl`, `CreateCourseRequest`. Đã thêm ghi chú superseded vào plan đó.
- `261030-1204-band-skill-lesson-paywall` (status `complete` từ 2026-10-31, 7/7 phase Done; còn lại: giá Points chưa được product owner duyệt, chưa kiểm thử trên trình duyệt thật): phase 7 dựa vào `skill`/`skillPosition` do content-service tính (phase 2 của plan đó) và dùng kết quả audit đã có (báo cáo `plans/reports/implementation-261030-band-skill-paywall-acceptance-report.md`, mục Phase 6; migration `V89` đã tách bài cây cối theo kỹ năng) để chọn audio PREMIUM.
- `261016-1030-supabase-media-storage`, `261019-0813-common-storage-module` (status pending nhưng `shared/common-storage` đã có code và test): nền tảng; plan này dùng `ObjectStorage`, `ObjectKeys`, `UploadPolicy`, `signedDownloadUrl`. Đã thêm ghi chú tiếp nối vào `261016-1030`.
- `261023-0050-content-author-workspace-completion` (in-progress): đã tạo `ContentMediaController` upload audio; phase 3 mở rộng nó.

## Acceptance criteria

- Cả 24 mp3 theo README trả 200 `audio/mpeg`; bài Listening seed và PT-LISTENING-01/02/03 phát được.
- Cấu hình `community` và `media` khớp bảng trên; truy vấn kiểm tra trong phase 2 trả đúng.
- R&D tạo/sửa khóa học bằng **upload** ảnh bìa; learner thấy ảnh từ URL Supabase; DB không chứa data URI cho ảnh bìa (khóa học, đề luyện tập) và biểu đồ Writing.
- Admin tạo/sửa chủ đề và bộ từ vựng upload được ảnh bìa; `cover_key` trỏ tới object tồn tại.
- Audio PREMIUM chỉ lấy được qua URL ký có hạn; URL công khai của khóa đó trả lỗi; learner chưa mở khóa không nhận được URL.
- Học viên ghi âm Speaking: file lên `speaking-recordings`, `audio_reference` là khóa thật thuộc đúng user, người chấm được giao nghe được qua URL ký, quá 90 ngày thì bị xóa.
- Không còn khóa `writtings/leassons`; 4 ảnh Writing hiển thị.
- `mvn verify` các service chạm vào xanh; test FE liên quan xanh.

### Quyết định đang chờ bạn

1. **Phase 7, D1:** `listening/demo/ls1.mp3` vừa là audio bài 1 (miễn phí) vừa là audio bài 4 (trả phí) của Listening 5.5. Chọn một:
   - (A) Khuyến nghị: chuyển `ls1.mp3` sang `media-premium`, đánh dấu PREMIUM. Bài 1 cũng phát bằng URL ký. Không cần thu âm mới.
   - (B) Bạn thu một bản riêng cho bài 4, lưu trong `media-premium`. Bài 1 giữ công khai.
2. **Phase 7, D2:** tên bucket `media-premium`. Giữ hay đổi?
3. **Phase 6:** `PS-W1-WR-3` không có file trên bucket (biểu đồ đang hỏng). Chọn: (a) bạn cung cấp file; (b) giữ hỏng; (c) xóa tham chiếu biểu đồ.
4. **Ghi Supabase:** hỏi từng bước (hiện tại), hay cho phép một lần cho các lệnh đã định nghĩa trong brief (86 upload phase 4, 5 khóa phase 6, tạo `media-premium` phase 7)?
5. **Bạn:** thu 20 mp3 và điền `SUPABASE_URL`, `SUPABASE_SECRET_KEY` trong `.env`.
6. **Git:** push các nhánh và mở PR draft (backend và frontend)?

Đã chốt bằng ràng buộc kỹ thuật, không cần bạn: TTL URL ký = 900 giây (giới hạn `MAX_TTL` của `common-storage`).

## Câu hỏi còn mở

1. Việc bắt đầu đề luyện tập ở assessment-service (`StartAssessmentAttemptUseCase`) có đi qua `PaywallGate` của learning-service không? Chưa xác minh. Không chặn quyết định 8 (đề luyện tập giữ PUBLIC) nhưng phase 7 bước 1 phải truy vết để chắc không có đường trả audio PREMIUM mà không qua cổng.
2. Khi xuất biểu đồ Writing (phase 4), có luồng chấm AI/LLM nào đang nhận ảnh biểu đồ dưới dạng data URI không? Nếu có, đổi sang URL có thể đổi cách chấm; phase 4 bước 2 kiểm trước.

## Validation Log

### Session 1 — 2026-10-31
**Trigger:** Handoff sau khi lập plan; chạy `/ck:plan validate`.
**Questions asked:** 8

#### Questions & Answers

1. **[Architecture]** Phase 3: cột courses.cover_image_url của ảnh bìa khóa học nên lưu gì sau khi upload lên bucket media?
   - Options: Lưu khóa, resolve lúc đọc (Recommended) | Lưu URL đầy đủ
   - **Answer:** Lưu khóa, resolve lúc đọc
   - **Rationale:** đổi bucket/host chỉ đổi base URL; kéo theo sửa `Course`, request, response; tên cột giữ nguyên.
2. **[Tradeoffs]** Phase 4: 58 ảnh bìa đề luyện tập đang là SVG nhúng trong DB; xuất ra file định dạng nào?
   - Options: WebP 1280x800 (Recommended) | Giữ SVG, cho phép image/svg+xml trong media | PNG 1280x800
   - **Answer:** WebP 1280x800
   - **Rationale:** khớp quyết định bỏ SVG khỏi bucket công khai; mất tính vector.
3. **[Scope]** Phase 7: audio nào đi vào bucket riêng tư media-premium (chưa xác minh đường bắt đầu đề luyện tập có qua paywall không)?
   - Options: Chỉ audio bài sau 3 bài miễn phí (Recommended) | Thêm cả đề luyện tập Listening | Chưa chốt, tách phase 7 làm sau
   - **Answer:** Chỉ audio bài sau 3 bài miễn phí
   - **Rationale:** đề luyện tập và placement không bị ảnh hưởng; giảm rủi ro làm hỏng phát audio.
4. **[Risks]** Phase 8: ghi âm Speaking lưu bao lâu và ai được nghe lại?
   - Options: 90 ngày; chủ bài và examiner được giao (Recommended) | 30 ngày; chủ bài và examiner được giao | 180 ngày; thêm quyền admin
   - **Answer:** 90 ngày; chủ bài và examiner được giao
   - **Rationale:** giọng nói là dữ liệu cá nhân; hạn lưu bắt buộc, quyền nghe tối thiểu.
5. **[Scope]** Ngoài ảnh bìa, còn khoảng 28 ảnh biểu đồ Writing dạng data URI SVG (V11, V37, V38, V44). Có chuyển lên Supabase trong plan này không?
   - Options: Ngoài phạm vi, để plan sau (Recommended) | Chuyển luôn, thêm vào phase 4
   - **Answer:** Chuyển luôn, thêm vào phase 4 *(khác mặc định đề xuất)*
   - **Rationale:** phase 4 mở rộng: khóa `writing/charts/{assetId}.webp`, kiểm luồng chấm dùng ảnh.
6. **[Scope]** Phase 1: 4 object thử của tài khoản demo xử lý thế nào?
   - Options: Kiểm tham chiếu rồi xóa phần không dùng (Recommended) | Giữ tất cả
   - **Answer:** Kiểm tham chiếu rồi xóa phần không dùng
   - **Rationale:** tránh làm hỏng ảnh của tài khoản demo.
7. **[Risks]** docker-compose.yml đặt mặc định CONTENT_MEDIA_BASE_URL là https://media.example.invalid/. Thiếu biến thì audio và bìa resolve ra URL hỏng mà không báo lỗi. Xử lý thế nào?
   - Options: Giữ mặc định, thêm cảnh báo khởi động (Recommended) | Đổi mặc định thành URL Supabase thật | Chỉ dặn trong phase 1, không sửa code
   - **Answer:** Đổi mặc định thành URL Supabase thật *(khác mặc định đề xuất)*
   - **Rationale:** dev mới chạy là thấy media ngay; đổi lại là cố định project ref vào file commit. URL công khai của bucket không phải bí mật.
8. **[Tradeoffs]** Khi R&D thay hoặc xóa ảnh bìa (khóa học, chủ đề/bộ từ vựng), object cũ xử lý thế nào?
   - Options: Để mồ côi, dọn sau (Recommended) | Xóa object cũ khi không còn dòng nào tham chiếu
   - **Answer:** Để mồ côi, dọn sau
   - **Rationale:** không thêm code xóa, không rủi ro xóa nhầm khi hai nơi dùng chung khóa.

#### Confirmed Decisions
- Bìa khóa học: lưu khóa, resolve lúc đọc.
- Bìa đề luyện tập và biểu đồ Writing: WebP, khóa `practice/covers/{id}.webp` và `writing/charts/{id}.webp` trong `media`.
- Audio PREMIUM: chỉ audio bài sau 3 bài miễn phí; placement, 3 bài đầu, đề luyện tập giữ PUBLIC.
- Speaking: 90 ngày, chủ bài và examiner được giao.
- `docker-compose.yml`: mặc định `CONTENT_MEDIA_BASE_URL` thành URL Supabase thật.
- Ảnh bìa cũ để mồ côi.

#### Action Items
- [ ] Phase 4: kiểm luồng chấm AI/LLM có dùng data URI của biểu đồ không (câu hỏi mở 2).
- [ ] Phase 7 bước 1: truy vết đường bắt đầu đề luyện tập ở assessment-service (câu hỏi mở 1).
- [ ] Sửa `docker-compose.yml` mặc định `CONTENT_MEDIA_BASE_URL` (phase 1).

#### Impact on Phases
- Phase 1: bước 5 đổi mặc định compose; kiểm tham chiếu demo giữ nguyên.
- Phase 3: liệt kê 8 file đổi tên `ContentAudioStorage*`; xác nhận lưu khóa; ảnh cũ để mồ côi.
- Phase 4: mở rộng sang biểu đồ Writing; viết lại.
- Phase 5: bỏ giả định "job dọn mồ côi" không tồn tại; ảnh cũ để mồ côi.
- Phase 6: thêm `scripts/generate-course-writing-charts.py` vào danh sách sửa.
- Phase 7: xác nhận phân loại PREMIUM; liệt kê 7 nơi dùng `MediaReferencePolicy` và bean `ContentMediaConfig`.
- Phase 8: xác nhận hạn lưu 90 ngày và quyền nghe lại.

### Verification Results
- **Tier:** Full (8 phase)
- **Claims checked:** 43
- **Verified:** 38 | **Failed:** 4 | **Unverified:** 1

#### Failures
1. [Fact Checker] Phase 5 nói có "job dọn ảnh mồ côi" của flashcards: không có `@Scheduled` nào trong library-service; `FlashcardImageUsageService` chỉ trả `isStillReferenced`, và các use case xóa object cũ khi không còn tham chiếu. Học viên chỉ đặt được khóa `images/{userId}/` (`ObjectKeys.requireOwnedBy`, `UpdateFlashcardUseCase.java:55`).
2. [Fact Checker] Phase 1, 3, 4 nói base URL trống thì khóa thành lỗi hoặc `null`; `docker-compose.yml:156` đặt mặc định `https://media.example.invalid/` nên khóa resolve ra URL hỏng, không báo lỗi.
3. [Contract Verifier] Phase 6 thiếu `backend/scripts/generate-course-writing-charts.py` (chứa `writtings`).
4. [Contract Verifier] Phase 3 và 7 không liệt kê người dùng: `ContentAudioStorage*` có 8 file; `MediaReferencePolicy` có 7 nơi dùng cộng `ContentMediaConfig`.

#### Unverified
1. [Flow Tracer] `StartAssessmentAttemptUseCase` có qua `PaywallGate` không (`AttemptStructureResponse` trả `audio.mediaUrl` cho học viên).

### Whole-Plan Consistency Sweep
- Files: plan.md và 8 file phase quét bằng từ khóa; đọc lại đầy đủ các phần đã sửa (plan.md, phase 4, phase 5 viết lại; đoạn sửa ở phase 1, 3, 6, 7, 8). Chưa đọc lại toàn văn phase 2.
- Decision deltas checked: 8 (lưu khóa, WebP, tập audio PREMIUM, hạn lưu và quyền nghe Speaking, biểu đồ Writing vào phase 4, file demo, mặc định compose, bìa cũ để mồ côi)
- Reconciled stale references: 5 (phase 3: câu "quyết định mặc định", rủi ro base URL, ghi chú job dọn; phase 8: hai chỗ nhắc "câu hỏi mở 4"/"admin?")
- Unresolved contradictions: 0
- Còn mở (không phải mâu thuẫn): 2 câu ở mục "Câu hỏi còn mở" của plan.md, đều có bước xác minh trong phase 4 (bước 2) và phase 7 (bước 1).
