---
title: "Module shared common-storage"
description: "Thư viện Maven dùng chung để ghi/xóa object và cấp URL trên Supabase Storage (3 bucket: media, avatars, speaking-recordings), kèm chính sách key và kiểm tra nội dung file. Chỉ là module thư viện, chưa có endpoint upload nào."
status: pending
priority: P2
branch: "feat/common-storage"
tags: [shared, storage, supabase, library]
blockedBy: []
blocks: [261016-1030-supabase-media-storage]
created: "2026-10-19T01:16:24.880Z"
createdBy: "ck:plan"
source: conversation
---

# Module shared `common-storage`

Trạng thái: **chờ duyệt.** Chưa có code. Phạm vi chỉ là thư viện `shared/common-storage`.

## Mục tiêu

Ba service sẽ cần ghi file lên Supabase Storage: user-service (avatar), assessment-service (ghi âm Speaking), content-service
(mp3 Listening, ảnh bài học). Nếu mỗi service tự gọi REST của Supabase thì sẽ có ba bản chép của: cách gắn khóa server, cách
ghép URL, cách đặt tên key, cách kiểm tra file. `common-storage` giữ **một** bản của phần kỹ thuật đó. Nghiệp vụ (ai được
upload, lưu key vào cột nào, quyền nghe lại) ở lại trong từng service.

## Bối cảnh và quyết định đã chốt (hội thoại 2026-10-19)

| Chủ đề | Quyết định |
| --- | --- |
| Hình thức | Thư viện `shared/common-storage`, **không** phải service riêng. Chỉ tách service khi cần quota theo user, dọn file mồ côi tập trung, hoặc xử lý nặng (chuyển mã audio, quét virus). Vì code đi qua interface `ObjectStorage` nên đổi sau này chỉ thay implementation |
| Nhà cung cấp | Supabase Storage, gọi REST bằng `RestClient` của Spring, **không** kéo SDK Supabase |
| Bucket | `media` (public, chỉ backend ghi), `avatars` (public, học viên upload), `speaking-recordings` (**private**). `media` đã có; `speaking-recordings` đang tạo; `avatars` chưa tạo (kiểm tra ở Dashboard). MIME và dung lượng cấu hình ở Dashboard, không nằm trong module |
| Cách upload | Avatar và Speaking: file đi **qua backend** (nhỏ: avatar ≤ 2 MB, ghi âm Speaking ≤ **5 MB**). mp3/ảnh nội dung do admin: **signed upload URL** (file lớn). Module phục vụ cả hai: `put` và `signedUploadUrl` |
| Không upload tay nữa | Mọi thứ phải có API. Bucket `media` vẫn public khi đọc nhưng chỉ backend (khóa server) được ghi |
| Phạm vi plan này | Chỉ module thư viện. **Không** gồm endpoint upload ở các service, **không** gồm resize/xóa EXIF ảnh (việc của user-service) |

## Quy tắc bắt buộc cho người làm

1. **Chỉ technical concern** (AGENTS.md §3.5): không entity, không use case, không repository, không tên bucket cố định, không logic
   "ai được làm gì". Bucket là **tham số** của mọi phương thức; mỗi service tự cấu hình bucket của nó.
2. **Không dependency ngoài danh sách ở phase 1.** Không Lombok (dùng `record`), không SDK Supabase, không Spring Web MVC/JPA,
   không `spring-boot-starter-web`. Module chỉ kéo `spring-web` để dùng `RestClient`.
3. **Khóa server không bao giờ xuất hiện ở**: log, thông báo exception, `toString()`, test, tài liệu, commit
   (AGENTS.md §3.4). Chỉ ghi **tên** biến môi trường. Giá trị chỉ ở `.env` (gitignored).
4. **Thông báo lỗi không chứa dữ liệu bên ngoài.** `StorageException` mang `operation`, `reason` (enum cố định), mã HTTP,
   `outcomeUnknown` và `ref` (8 ký tự đầu của SHA-256 của `bucket/key`, để truy vết mà không lộ key). **Không** chép `message` của
   Supabase, không kèm key, tên bucket, header hay body. Lỗi đầu vào dùng thông báo cố định không chứa giá trị, vì các
   `GlobalExceptionHandler` hiện có trả `getMessage()` của `IllegalArgumentException` cho client với HTTP 400.
5. Mỗi phase là các commit riêng, conventional commits, test xanh trước khi sang phase sau. **Không** đặt mã plan, số phase,
   nhắc AI/agent trong commit, comment hay tên test (AGENTS.md §6.8).
6. Không ghi "đã chạy" cho check chưa chạy (AGENTS.md §6.7). Contract test cần khóa thật nên mặc định **bị skip**; phải nói rõ là skip.
7. Không sửa Docker, config tập trung, hay `common-security` như side effect. `Dockerfile.spring-service` **không cần sửa**: nó
   `COPY . .` rồi `mvn -pl <module> -am`, nên module mới tự được kéo vào khi một service khai báo dependency. Việc nối `SUPABASE_*` vào `docker-compose.yml` và
   `config-repo/*.yaml` thuộc **plan của từng service dùng module** (mục "Việc của plan service").
8. Mọi giá trị về API Supabase ở dưới được đối chiếu với tài liệu và source `storage-js` ngày 2026-10-19 (mục "Hợp đồng REST
   đã xác minh"). Điều **chưa** xác minh nằm ở "Câu hỏi còn mở". Bước **smoke thật** cuối phase 1 chốt chúng **trước** khi viết client
   (phase 3); contract test ở phase 4 là **cổng bắt buộc** để các plan service dùng module.
9. **Nhánh nền:** `shared/common-events` hiện chỉ có trên `feat/common-events` (chưa có trên `origin/main`). `feat/common-storage`
   tách từ nhánh đó, hoặc từ `main` sau khi `common-events` merge. Các sửa của plan Supabase cũ (`261016-1030`) đang **chưa commit**
   trên nhánh hiện tại: commit chúng cùng plan này.

## Phase

| # | Phase | Nội dung | Commit chính |
| --- | --- | --- | --- |
| 1 | [Khung module và cấu hình](./phase-01-module-skeleton-and-config.md) | pom, reactor, interface + record, exception (enum lý do), properties, auto-config, **smoke thật (cổng trước phase 3)** | `feat(storage)` |
| 2 | [Chính sách key và nội dung file](./phase-02-key-and-content-policies.md) | `ObjectKeys`, `UploadPolicy`, `ContentSniffer`, chuẩn hóa MIME | `feat(storage)` |
| 3 | [Client Supabase Storage](./phase-03-supabase-storage-client.md) | `SupabaseObjectStorage` bằng `RestClient`, đăng ký bean, test bằng `MockRestServiceServer` | `feat(storage)` |
| 4 | [Contract test và tài liệu](./phase-04-contract-test-and-docs.md) | Contract test thật (opt-in), AGENTS/CLAUDE/README, cập nhật plan Supabase cũ | `test(storage)`, `docs(storage)` |

1 → 2 → 3 → 4. Phase 2 độc lập với phase 3 về code nhưng phase 3 dùng `ObjectKeys` để kiểm tra đầu vào, nên làm 2 trước.
Phase 3 chỉ bắt đầu khi smoke cuối phase 1 có kết quả. Dừng được sau phase 2 (đã có phần thuần Java, test xong) nhưng chưa dùng được cho service nào cho tới hết phase 3.

## Hợp đồng REST đã xác minh (Supabase Storage, 2026-10-19)

Nguồn: `supabase.com/docs/reference/self-hosting-storage`, `docs/guides/storage/uploads/standard-uploads`,
`docs/guides/api/api-keys`, source `supabase/storage-js` (`StorageFileApi.ts`, `lib/fetch.ts`). Gọi tới `{SUPABASE_URL}/storage/v1`.

| Thao tác | Request | Kết quả dùng |
| --- | --- | --- |
| Ghi object | `POST /object/{bucket}/{key}`, body là byte thô, header `Content-Type`, `x-upsert: false` | `200 {"Id","Key"}`. Object đã tồn tại mà không `x-upsert`: **400** |
| Xóa object | `DELETE /object/{bucket}` body JSON `{"prefixes":["{key}"]}` (cách SDK dùng). Có thêm dạng `DELETE /object/{bucket}/{key}` trong reference | `200`, idempotent (key không tồn tại không gây lỗi) |
| Kiểm tra tồn tại | **Chưa xác minh.** Ứng viên: `HEAD /object/{bucket}/{key}` hoặc `GET /object/info/{bucket}/{key}` | 200 có, 404 không. Smoke chốt endpoint và mã |
| URL công khai | Không gọi mạng: `{SUPABASE_URL}/storage/v1/object/public/{bucket}/{key}` | chuỗi URL |
| Signed URL tải xuống | `POST /object/sign/{bucket}/{key}` body `{"expiresIn": <giây>}` | `{"signedURL":"/object/sign/{bucket}/{key}?token=…"}`. URL đầy đủ = `{SUPABASE_URL}/storage/v1` + `signedURL` (SDK ghép như vậy) |
| Signed URL upload | `POST /object/upload/sign/{bucket}/{key}` body `{}`, tùy chọn `x-upsert: true` | `{"url":"/object/upload/sign/{bucket}/{key}?token=…"}`; client sau đó `PUT` file tới URL đầy đủ, kèm header `x-upsert`, `content-type`. **Không có tham số TTL** trong API (SDK không truyền) |
| Xác thực | Khóa mới `sb_secret_…`: gửi ở header **`apikey`**, **không** gửi ở `Authorization: Bearer` (không phải JWT). Khóa legacy `service_role` là JWT: dùng `Authorization: Bearer` (+ `apikey`) | Khóa server **bỏ qua RLS** |

Ghi chú: khóa secret bị chặn nếu gọi từ trình duyệt (so `User-Agent`, trả 401), nên chỉ dùng ở backend. Upload chuẩn khuyến nghị file
≤ 6 MB; lớn hơn nên dùng TUS resumable. Ghi đè object (`x-upsert`) có thể trả bản cũ do CDN; module **không** hỗ trợ ghi đè, luôn sinh key mới.

## Cấu hình

| Thuộc tính | Biến môi trường (do service ánh xạ trong config-repo) | Bí mật? | Ý nghĩa |
| --- | --- | --- | --- |
| `app.storage.enabled` | — | Không | `true` để bật. **Mặc định tắt**: service không dùng storage khởi động bình thường |
| `app.storage.supabase.url` | `SUPABASE_URL` | Không | `https://<project-ref>.supabase.co`, không có `/storage/v1` |
| `app.storage.supabase.secret-key` | `SUPABASE_SECRET_KEY` | **Có** | Khóa server. Chỉ ở `.env`. Config-repo chỉ được ánh xạ `${SUPABASE_SECRET_KEY:}` (mặc định **rỗng**), không bao giờ có giá trị thật |
| `app.storage.supabase.connect-timeout` | — | Không | Mặc định `3s` |
| `app.storage.supabase.read-timeout` | — | Không | Mặc định `10s` (thao tác nhỏ: xóa, ký URL, kiểm tra tồn tại) |
| `app.storage.supabase.upload-timeout` | — | Không | Mặc định `120s`, chỉ cho `put` |

Khác với ý ban đầu "bật khi có `SUPABASE_URL`": `@ConditionalOnProperty` coi giá trị rỗng (`${SUPABASE_URL:}`) là **có**, nên
dùng cờ `enabled` tường minh. Hành vi khi bật cờ: **cả url lẫn khóa trống** → service vẫn khởi động, `ObjectStorage` là bản "unavailable" mà mọi lệnh ném `StorageException(UNAVAILABLE)` (cùng kiểu các tích hợp ngoài khác của repo, dùng `${X:}` và hạ cấp nhẹ nhàng, tránh crash-loop do `restart: unless-stopped`); **chỉ một trong hai trống**, hoặc url sai định dạng → **fail nhanh lúc khởi động** (cấu hình dở dang là lỗi).
Tên bucket **không** nằm ở module; mỗi service khai báo riêng (ví dụ `SUPABASE_AVATAR_BUCKET` ở user-service, thuộc plan của service đó).

## Việc của plan service (checklist bắt buộc cho user, assessment, content)

Thư viện không ép được các điều sau; mỗi plan service phải có chúng, và reviewer plan đó kiểm từng dòng.

1. **Nối cấu hình:** `docker-compose.yml` (env của service) và `infra/config-server/config-repo/<service>.yaml`: `app.storage.enabled: true`, `app.storage.supabase.url: ${SUPABASE_URL:}`, `secret-key: ${SUPABASE_SECRET_KEY:}` (mặc định rỗng); tên bucket riêng của service. Hiện **chưa có** `SUPABASE_*` ở compose hay config-repo.
2. **Multipart:** đặt `spring.servlet.multipart.max-file-size` và `max-request-size` đúng trần của service (hiện không service nào cấu hình, mặc định 1 MB) và kiểm Gateway cho phép body đó đi qua.
3. **Giới hạn đồng thời** cho upload (semaphore) theo heap `-Xmx512m` của compose.
4. **Key từ client:** `ObjectKeys.requireOwnedBy` + `ObjectStorage.exists` trước khi lưu; thêm `@Size` khớp cột DB (`varchar(500)`).
5. **Upload rồi ghi DB:** nếu ghi DB lỗi thì `delete` best-effort; nếu bài nộp đã tồn tại (idempotent theo `submissionKey`, `CreateLearnerSubmissionUseCase` trả bản cũ) thì xóa file vừa upload; khi `StorageException.outcomeUnknown` thì thử lại bằng key mới.
6. **Avatar:** xóa EXIF **trước khi** ghi lên bucket public (điều kiện chặn); chỉ nhận URL thuộc bucket `avatars` (hiện `UpdateLearnerProfileRequest` nhận mọi chuỗi ≤ 500 ký tự, và hồ sơ công khai nhận mọi URL http/https).
7. **Assessment:** `audio_reference` có **hai** nơi: `learner_submissions` và `video_practice_attempts`; plan assessment phải nêu cả hai.
8. **Examiner/người chấm nghe ghi âm:** service tự thiết kế đường đọc và kiểm vai trò; thư viện chỉ cấp `signedDownloadUrl` (tối đa 15 phút) và không biết vai trò.
9. **Signed upload (content):** chỉ vai trò admin/`CONTENT_AUTHOR`; sau khi client báo xong, gọi `exists` trước khi lưu `media_reference`; sniff sau upload nếu cần.
10. **Giới hạn bucket:** xác nhận `allowed_mime_types` và `file_size_limit` đã đặt ở Dashboard cho `media`, `avatars`, `speaking-recordings` trước khi bật tính năng.

## Giá trị lưu ở DB

| Dữ liệu | Cột | Giá trị |
| --- | --- | --- |
| Avatar | `learner_profiles.avatar_reference` | **URL công khai đầy đủ** `{SUPABASE_URL}/storage/v1/object/public/avatars/{userId}/{uuid}.{ext}`; service khác chỉ đọc URL này |
| Ghi âm Speaking | `learner_submissions.audio_reference`, `video_practice_attempts.audio_reference` | **Object key** `speaking/{userId}/{uuid}.{ext}` trong bucket `speaking-recordings`, không phải URL |
| Nội dung (mp3, ảnh) | `content_assets.media_reference` | Key tương đối với `CONTENT_MEDIA_BASE_URL`, hoặc URL `https://`. Key cũ **có thể chứa chữ hoa** (`listening/demo/numM.mp3`), nên `ObjectKeys.requireSafe` phải chấp nhận |

## Tiêu chí hoàn thành

- `mvn -q -pl shared/common-storage test` xanh; số test được báo từng phase.
- `mvn -B -ntp verify` xanh toàn reactor (module mới được thêm vào `<modules>`).
- Grep `shared/common-storage/src` không có literal khóa, không có `System.out`, không có `printStackTrace`.
- Có test chứng minh: khóa không xuất hiện trong `toString()` của properties và trong message của mọi `StorageException`.
- Tài liệu (AGENTS.md, CLAUDE.md, README) có mô tả module; plan Supabase cũ được cập nhật phần TTL và bucket.
- Smoke thật có kết quả (báo cáo trong `reports/`) **trước** khi bắt đầu phase 3.
- Contract test chạy **xanh** trong bucket `contract-test-*` là điều kiện để plan service dùng module; nếu chưa chạy phải ghi rõ "chưa chạy" và `blocks` giữ nguyên.

## Rủi ro

| Rủi ro | Cách chặn |
| --- | --- |
| Gắn sai header khóa (Bearer vs `apikey`) làm mọi lệnh 401 | Test ghim header cho cả hai dạng khóa; contract test với khóa thật |
| Module phình thành "service trong thư viện" | Quy tắc số 1 và 2; review mọi PR theo hai quy tắc đó |
| Ghi đè object làm CDN trả bản cũ | Không có `upsert`; key luôn có UUID ngẫu nhiên |
| Khóa lộ qua log/exception | Quy tắc 3 và 4, test riêng, `toString()` che khóa |
| Một khóa toàn quyền (bỏ qua RLS) dùng chung, mọi service import `.env` gốc nên không thể ép bằng code "chỉ service bật cờ mới đọc" | Thử dùng mỗi service một khóa secret riêng nếu Supabase cho phép (câu hỏi mở); ghi quy trình xoay khóa; khóa legacy `service_role` đang được Supabase thay dần nên ưu tiên khóa mới |
| Service nhận key từ client rồi lưu dù file chưa tồn tại, hoặc để lại file mồ côi | `exists`, `requireOwnedBy`, quy trình trong "Việc của plan service" |
| Timeout sau khi object đã được ghi | `StorageException.outcomeUnknown`; caller thử lại bằng key mới và dọn key cũ best-effort |
| Signed upload qua mặt kiểm tra nội dung | Chỉ admin; giới hạn MIME/dung lượng của bucket được kiểm ở smoke và ở checklist rollout; sniff sau upload để plan content-service quyết |
| Client khai sai MIME khi upload | `ContentSniffer` (phase 2) cho đường đi qua backend; đường signed URL chỉ có giới hạn MIME/dung lượng của bucket, ghi rõ để service dùng biết |
| `put` nhận `byte[]`; compose chạy `-Xmx512m`, nên 20 upload 5 MB đồng thời (khoảng 100 MB file trong bộ nhớ, chưa tính bản sao của multipart) vẫn đáng kể so với heap | `UploadPolicy.readBounded` đọc tối đa `max+1` byte (không tin `Content-Length`); checklist buộc service đặt giới hạn multipart và giới hạn đồng thời; trần Speaking đã chốt **5 MB**; mp3 dài dùng signed upload |
| Nhánh nền thiếu `common-events`, plan Supabase cũ chưa commit | Quy tắc 9 |

## Câu hỏi còn mở

1. Khóa trong project của bạn là dạng mới (`sb_secret_…`) hay legacy (`service_role` JWT)? Module hỗ trợ cả hai; contract test xác nhận.
2. Thời hạn cố định của signed upload URL phía Supabase là bao lâu? Chưa tìm thấy con số chính thức. Hệ quả: biến
   `CONTENT_MEDIA_UPLOAD_TTL_SECONDS` trong plan Supabase cũ **không áp dụng được** cho signed upload (xem phase 4).
3. Xóa object: dùng dạng bulk `DELETE /object/{bucket}` (SDK) hay dạng `DELETE /object/{bucket}/{key}` (reference)? Plan chọn dạng bulk
   của SDK; contract test kiểm tra dạng bulk, và một bước thử tay ở phase 4 so với dạng còn lại để chốt.
4. ~~Project Supabase riêng để test?~~ **Chốt lại 2026-10-19 (Session 2):** chạy smoke và contract test trên **project hiện tại**, chỉ trong bucket private `contract-test-*`; người dùng xóa sau. Nên dùng một secret key tạm rồi thu hồi. Rủi ro: khóa toàn quyền cùng project với dữ liệu thật; giảm bằng allow-list bucket trong code.
5. Supabase có cho tạo nhiều secret key (mỗi service một khóa) không? **Đã chốt hướng:** mỗi service một khóa nếu được, ngược lại một khóa chung kèm quy trình xoay. Smoke #9 trả lời câu hỏi "có cho không".
6. Endpoint kiểm tra tồn tại: `HEAD /object/{bucket}/{key}` hay `GET /object/info/{bucket}/{key}`? Smoke chốt.
7. Ai sở hữu việc đặt giới hạn MIME/dung lượng cho từng bucket ở Dashboard và ghi lại ở đâu? Gợi ý: checklist rollout của từng plan service.

## Red Team Review

### Session — 2026-10-19
**Findings:** 15 sau khi gộp 28 phát hiện của 3 reviewer (15 accepted, trong đó 6 accepted một phần; 0 rejected hoàn toàn)
**Severity breakdown:** 0 Critical, 10 High, 5 Medium
**Báo cáo gốc** (thư mục `reports/`): `from-code-reviewer-to-planner-red-team-security-adversary-plan-review-report.md`,
`...-assumption-destroyer-plan-review-report.md`, `...-failure-mode-analyst-plan-review-report.md`.

Kiểm tra lại trực tiếp trong repo trước khi chấp nhận: `origin/main` chưa có `shared/common-events` (đúng); plan Supabase cũ **có** trên
`origin/main` (khác với khẳng định của một reviewer, người đó kiểm trên `main` local đã cũ) nhưng các sửa của ta vào nó chưa commit (đúng);
seed content có key chữ hoa như `listening/demo/numM.mp3` (đúng); `user-service` trả `getMessage()` của `IllegalArgumentException` cho client
với HTTP 400 (đúng); không có cấu hình multipart nào trong `services/` và `infra/` (đúng).

| # | Phát hiện | Mức | Quyết định | Áp dụng vào |
|---|-----------|-----|------------|-------------|
| 1 | Quyền sở hữu key chỉ là helper tùy chọn; không `exists`; file mồ côi | High | Accept | P1 (`exists`), P2 (`requireOwnedBy`), plan (checklist) |
| 2 | Kiểm tra dung lượng không giới hạn RAM; heap 512 MB | High | Accept (modified): thêm `readBounded` và checklist; **không** thêm `InputStream` cho `put` (YAGNI) | P2, plan |
| 3 | Signed upload qua mặt kiểm tra nội dung; giới hạn bucket chỉ ở Dashboard | High | Accept (modified): `exists` + checklist giới hạn bucket + chỉ admin; sniff sau upload để plan content | P3, plan |
| 4 | Một khóa toàn quyền dùng chung, `.env` gốc import bởi mọi service | High | Accept (modified): câu AGENTS.md trung thực, quy trình xoay khóa, thử mỗi service một khóa | P4, plan |
| 5 | Ánh xạ lỗi thiếu loại; chép message upstream; rò qua HTTP 400 | High | Accept: enum lý do cố định, `ref`, bắt mọi `RestClientException`/`RuntimeException`, message đầu vào cố định | P1, P2, P3 |
| 6 | Fail-fast gây crash-loop; không ai sở hữu việc nối compose/config-repo | High | Accept (modified): cả hai trống → bản unavailable; một trong hai trống → fail nhanh; checklist nêu chủ sở hữu | P1, plan |
| 7 | `ObjectKeys` chữ thường làm key seed cũ không dùng được | High | Accept: hai mức (`requireSafe` rộng, key sinh mới chặt), prefix định nghĩa rõ | P2, P3 |
| 8 | Nhánh nền sai; sửa plan cũ chưa commit; thiếu ghi nhận duyệt; số module cũ | High | Accept (kiểm lại: một phần claim sai, xem trên) | P1, P4, plan |
| 9 | Hợp đồng Supabase xác minh sau khi code; phase 4 cho "chưa chạy" mà vẫn mở khóa | High | Accept: smoke thật trước phase 3, phân loại lỗi theo `statusCode` trong body, contract xanh là cổng | P1, P3, P4, plan |
| 10 | `readTimeout` chung 10s; kết quả không rõ sau timeout; `HttpClient` không đóng | High | Accept: hai client, `outcomeUnknown`, đóng client | P1, P3 |
| 11 | Sniffer cho cảm giác an toàn giả; EXIF avatar | Medium | Accept: siết brand `ftyp` và DocType; EXIF là điều kiện chặn trong plan avatar | P2, plan |
| 12 | Signed download tới 24 giờ | Medium | Accept (modified): trần 15 phút; **không** chặn `publicUrl` trên bucket private (module không biết bucket) | P1, P3 |
| 13 | Kiểm tra URL chưa rõ, redirect | Medium | Accept (modified): parse bằng `URI`, từ chối userinfo, `Redirect.NEVER`; **không** thêm allow-list host (cấu hình do vận hành đặt) | P1, P3 |
| 14 | Contract test dùng deny-list, dùng chung tên biến, khóa trên dòng lệnh; grep tự khớp khóa giả | Medium | Accept: allow-list `contract-test-`, `STORAGE_CONTRACT_*`, khóa từ file, sửa regex | P4 |
| 15 | Lệnh tích hợp không build module mới; hợp đồng giá trị DB; consumer `video_practice_attempts` | Medium | Accept: test `.imports`, mục "Giá trị lưu ở DB", checklist assessment hai nơi | P1, plan |

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01-module-skeleton-and-config.md, phase-02-key-and-content-policies.md, phase-03-supabase-storage-client.md, phase-04-contract-test-and-docs.md
- Decision deltas checked: 14 (thêm `exists`; `StorageException` enum + `outcomeUnknown` + `ref`; `requireSafe`/`requireOwnedBy`/`InvalidObjectKeyException`; trần TTL 15 phút; bản unavailable và quy tắc cấu hình thiếu; `uploadTimeout` và hai client; `readBounded`; smoke cổng trước phase 3; `STORAGE_CONTRACT_*` và allow-list; nhánh nền; phân loại lỗi theo `statusCode`; sniffer brand/DocType; `redirect NEVER`; checklist và giá trị DB trong `plan.md`)
- Reconciled stale references: 7 (rule 4, 7, 8; hành vi fail nhanh ở mục cấu hình; hàng rủi ro `byte[]` và hàng xung đột `<module>`; hàng "Bộ ký tự key quá chặt" ở phase 2)
- Unresolved contradictions: 0. Còn mở và **không phải mâu thuẫn**: câu hỏi mở 1–7 ở trên; các điểm Supabase marked UNVERIFIED chỉ được chốt bởi smoke (phase 1) và contract test (phase 4).

## Validation Log

### Session 1 — 2026-10-19
**Trigger:** `/ck:plan validate` sau red-team; bỏ qua bước kiểm chứng lại vì mục Red Team Review đã có bằng chứng.
**Questions asked:** 6

#### Questions & Answers

1. **[Risks]** Smoke thật (cuối phase 1) và contract test (phase 4) cần một project Supabase KHÔNG phải production, vì `.env` gốc đang trỏ tới project thật. Bạn xử lý việc này thế nào?
   - Options: Tạo project Supabase riêng (Recommended) | Dùng project hiện tại, bucket riêng | Chưa có, hoãn phase 3
   - **Answer:** Tạo project Supabase riêng
   - **Rationale:** quyết định có chạy được phase 3 và contract hay không, và khóa dùng để test không chạm dữ liệu thật.
2. **[Architecture]** Nhánh nền cho `feat/common-storage`: tách từ đâu?
   - Options: Từ feat/common-events (Recommended) | Từ origin/main | Chờ common-events merge
   - **Answer:** Từ feat/common-events
   - **Rationale:** reactor có đủ hai module, không xung đột pom; PR storage phụ thuộc thứ tự merge với common-events.
3. **[Scope]** Cổng sau phase 4: các plan service được phép bắt đầu khi nào?
   - Options: Chặt: chờ contract xanh (Recommended) | Mềm: phải xanh mới được merge | Không cần cổng
   - **Answer:** Chặt: chờ contract xanh
   - **Rationale:** test mock chỉ phản ánh giả định của chính ta; không có contract xanh thì plan service chưa dùng module.
4. **[Assumptions]** Trần dung lượng mỗi bản ghi âm Speaking upload qua backend?
   - Options: 5 MB (Recommended) | 10 MB | 20 MB
   - **Answer:** 5 MB
   - **Rationale:** file được giữ trong bộ nhớ, compose chạy `-Xmx512m`; 5 MB đủ rộng cho bài Speaking vài phút ghi bằng opus.
5. **[Risks]** Khóa Supabase bỏ qua RLS và mọi service import `.env` gốc: xử lý số khóa thế nào?
   - Options: Mỗi service một khóa nếu được (Recommended) | Một khóa chung, có quy trình xoay | Tách khóa riêng cho bucket private
   - **Answer:** Mỗi service một khóa nếu được
   - **Rationale:** thu hồi từng khóa mà không ảnh hưởng service khác; phụ thuộc Supabase có cho nhiều secret key không.
6. **[Architecture]** Bật `app.storage.enabled` nhưng cả `SUPABASE_URL` lẫn `SUPABASE_SECRET_KEY` đều trống: service nên làm gì?
   - Options: Vẫn khởi động, lỗi khi dùng (Recommended) | Báo lỗi ngay khi khởi động
   - **Answer:** Vẫn khởi động, lỗi khi dùng
   - **Rationale:** khớp cách repo xử lý tích hợp ngoài; tránh crash-loop (`restart: unless-stopped`) làm sập đăng nhập.

#### Confirmed Decisions
- Project test: một project Supabase riêng, người dùng tạo — phase 3 chỉ bắt đầu sau khi có.
- Nhánh nền: `feat/common-events` — khớp quy tắc 9 và phase 1 bước 1, không đổi.
- Cổng: chặt — khớp phase 4 "Cổng cho plan service", không đổi.
- Trần Speaking: 5 MB (avatar 2 MB) — đã sửa hàng "Cách upload" và hàng rủi ro `byte[]`.
- Số khóa: mỗi service một khóa nếu Supabase cho; thêm smoke #9 để trả lời.
- Thiếu cả hai biến: bản unavailable — khớp phase 1, không đổi.

#### Action Items
- [ ] Người dùng tạo project Supabase test và bucket private `contract-test-smoke` trước khi chạy smoke.
- [ ] Smoke #9: xác nhận Supabase có cho nhiều secret key; ghi kết quả vào báo cáo.
- [ ] Plan avatar và Speaking dùng trần 2 MB và 5 MB trong `max-file-size` và `UploadPolicy`.

#### Impact on Phases
- Phase 1: thêm smoke #9 (nhiều secret key); nêu rõ project test là điều kiện tiên quyết; làm rõ hàng #5 đo upload 10 MB là đại diện cho mp3 của admin, không phải Speaking.
- Phase 3: sửa ví dụ timeout từ bản ghi 10 MB sang mp3 admin; rủi ro timeout giữ nguyên.
- Phase 2, 4: không đổi.

### Verification Results
- Bỏ qua bước kiểm chứng lại (Red Team Review đã có bằng chứng, tier Standard). Không có thẻ `[UNVERIFIED]` do planner gắn.

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01-module-skeleton-and-config.md, phase-02-key-and-content-policies.md, phase-03-supabase-storage-client.md, phase-04-contract-test-and-docs.md
- Decision deltas checked: 3 (trần Speaking 5 MB; smoke #9 và hướng mỗi service một khóa; project test riêng)
- Reconciled stale references: 4 (mục "Cách upload", hàng rủi ro `byte[]`, câu hỏi mở 4 và 5, ví dụ timeout ở phase 3)
- Unresolved contradictions: 0

### Session 2 — 2026-10-19
**Trigger:** người dùng đổi ý sau khi được giải thích "project test là gì": "cứ làm trên này đi rồi tôi xóa sau".
**Questions asked:** 0 (quyết định tự nguyện của người dùng, ghi đè câu trả lời 1 của Session 1)

#### Confirmed Decisions
- Smoke và contract test chạy trên **project Supabase hiện tại**, thay vì một project riêng; người dùng tự xóa bucket test (và thu hồi khóa tạm nếu có) sau khi xong.
- Hiểu "this/trên này" là project hiện có của ứng dụng; nếu ý khác (ví dụ chỉ một bucket), cần xác nhận lại trước phase 3.

#### Rủi ro đã nói với người dùng và biện pháp giữ lại
- Khóa secret bỏ qua RLS và cùng project với `media` (có dữ liệu thật): một thao tác nhầm có thể xóa file thật.
- Giữ nguyên: allow-list tiền tố bucket `contract-test-` trong code và smoke; không gọi thao tác nào tới `media`, `avatars`, `speaking-recordings`; khóa đọc từ file ngoài repo, không lấy từ `.env`.
- Khuyến nghị (không bắt buộc): tạo một secret key tạm chỉ để test rồi thu hồi.
- Bỏ guard "URL trùng `SUPABASE_URL` thì từ chối chạy" của contract test, vì giờ hai giá trị cố ý trùng.

#### Impact on Phases
- Phase 1: điều kiện tiên quyết của smoke và hai hàng rủi ro cập nhật.
- Phase 4: mô tả contract test, guard, bước chạy và bảng rủi ro cập nhật.
- Phase 2, 3: không đổi.

### Whole-Plan Consistency Sweep (Session 2)
- Files reread: plan.md, phase-01, phase-02, phase-03, phase-04
- Decision deltas checked: 2 (project dùng chung; bỏ guard so URL)
- Reconciled stale references: 16 (ràng buộc "không phải production"/"project phi production" ở plan.md, phase 1 và phase 4)
- Unresolved contradictions: 0. Ghi chú: câu trả lời 1 và mục "Confirmed Decisions" của Session 1 vẫn nói "project riêng"; chúng là lịch sử và đã được Session 2 thay thế.
