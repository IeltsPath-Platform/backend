# Báo cáo rà soát bảo mật upload file

Cập nhật: 2026-11-05. Phạm vi: mọi đường upload file trên `main` (`d27bf38f`), cộng đường upload ghi âm Speaking của
learning-service đang ở PR #148.

Phương pháp: đọc code và cấu hình, đối chiếu với `docs/system-architecture.md` (bảng bucket). **Chưa** chạy thử tấn công,
chưa kiểm cấu hình thật trên Supabase, chưa kiểm frontend. Các mục "chưa kiểm" ở cuối là việc cần làm tiếp, không phải
kết luận an toàn.

Đường dẫn Java dưới đây tính từ `services/<service>/src/main/java/com/ieltspath/<package>/`; của module dùng chung tính từ
`shared/common-storage/src/main/java/com/ieltspath/commonstorage/`.

## 1. Tóm tắt

Phần nền làm chắc và nhất quán, vì mọi upload đi qua `common-storage`: danh sách loại file cho phép (không wildcard), so
byte đầu file với loại khai báo, tên file do server sinh, key được kiểm an toàn, không ghi đè, giới hạn kích thước hai tầng,
phân quyền và kiểm chủ sở hữu khi tham chiếu lại file.

Phần còn yếu là lớp phòng thủ thứ hai:

| Mã | Vấn đề | Mức |
| --- | --- | --- |
| F-01 | Ảnh không được giải mã và mã hóa lại: EXIF (kể cả GPS) nằm nguyên trong bucket công khai; ảnh đội lốt qua được | Trung bình |
| F-02 | Không có hạn mức dung lượng theo user; limiter tầng ứng dụng chỉ có ở avatar và cộng đồng | Trung bình |
| F-03 | File upload mà không được dùng không bao giờ bị xóa; retention ghi âm bỏ sót chúng | Trung bình |
| F-04 | Vài endpoint upload của author chưa có tier rate limit riêng | Thấp |
| F-05 | Kiểm định dạng audio lỏng (mp3, wav chỉ nhìn vài byte đầu) | Thấp |
| F-06 | Mọi controller đọc cả file vào heap (`getBytes()`), tối đa 25 MB mỗi request | Thấp |
| F-07 | Không quét mã độc | Thấp |

Không thấy lỗ hổng cho phép thực thi mã, path traversal, ghi đè file của người khác hay đọc ghi âm của người khác.

## 2. Các đường upload

| Endpoint | Ai gọi | Giới hạn | Loại file | Bucket | Limiter ứng dụng | Tier gateway |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/users/me/avatar` | đã đăng nhập | 2 MB | png, jpeg, webp | `avatars` (công khai) | Redis, theo user | `upload` |
| `POST /api/community/media/images` | đã đăng nhập | 5 MB | png, jpeg, webp, gif | `community` (công khai) | Redis, theo user | `upload` |
| `POST /api/learning-support/flashcards/images` | đã đăng nhập | 2 MB | png, jpeg, webp | `flashcards` (công khai) | không | `upload` |
| `POST /api/assessments/speaking-recordings` | `CUSTOMER` | 5 MB | webm, mp4, ogg, mpeg, wav | `speaking-recordings` (riêng tư) | không | `upload` |
| `PUT /api/learning/speaking-practice/submissions/{id}/recordings/{q}` (PR #148) | `CUSTOMER` | 10 MB | wav 16 kHz mono | `speaking-recordings` (riêng tư) | không | `upload` |
| `POST /api/content/admin/media/audio` | `CONTENT_AUTHOR` | 25 MB | mpeg, mp4, wav, ogg | `media` hoặc `media-premium` | không | `upload` |
| `POST /api/content/admin/media/image` | `CONTENT_AUTHOR` | 2 MB | png, jpeg, webp | `media` | không | `upload` |
| `POST /api/content/admin/media/document` | `CONTENT_AUTHOR` | 20 MB | pdf | `media-premium` (riêng tư) | không | `upload` |
| `POST /api/content/admin/vocabulary/{covers,sense-images,pronunciation}` | `CONTENT_AUTHOR`, `ADMIN` | 2 MB | ảnh; audio | `flashcards`, `media` | không | mặc định (xem F-04) |
| `POST /api/content/admin/course-import[/validate]` | `CONTENT_AUTHOR` | 5 MB | xlsx | không lưu file | không | mặc định (xem F-04) |

Controller của avatar, ảnh cộng đồng và ảnh flashcard không có `@PreAuthorize` theo vai trò: bất kỳ tài khoản đăng nhập nào
cũng upload được. Tạo tài khoản mới là điều kiện duy nhất để tấn công các đường này.

## 3. Đã làm tốt

Các điểm dưới đây đã được xác nhận trong code, không phải suy đoán.

1. **Danh sách loại file cho phép, không wildcard.** `upload/UploadPolicy.java` từ chối `*` và từng policy nêu rõ loại.
   Không có SVG hay HTML trong danh sách, nên không có đường XSS qua file.
2. **Nội dung phải khớp loại khai báo.** `UploadPolicy.check` (dòng 72) so kết quả `ContentSniffer.detect` với loại khai
   báo; lệch thì `CONTENT_MISMATCH`. Đổi đuôi hay `Content-Type` không qua được. Loại lưu xuống storage là loại đã chuẩn
   hóa, không phải chuỗi client gửi.
3. **Tên file do server sinh.** `key/ObjectKeys.forUser` dùng `UUID.randomUUID()`; tên file client không bao giờ vào key.
   Mọi thao tác storage gọi `ObjectKeys.requireSafe` (không `..`, tối đa 8 đoạn, 300 ký tự) và kiểm tên bucket.
4. **Không ghi đè.** `supabase/SupabaseObjectStorage.java:81` đặt `x-upsert: false`.
5. **Giới hạn kích thước hai tầng.** Policy theo từng use case cộng `spring.servlet.multipart.*` trong config-repo của từng
   service (2, 5, 10, 25 MB).
6. **Chỉ tham chiếu được file của chính mình.**
   - Ảnh bài đăng cộng đồng: `AllowedMediaPolicy.java:71` bắt buộc key thuộc tác giả và host nằm trong danh sách cho phép;
     áp dụng khi tạo và sửa bài.
   - Ghi âm assessment: `CreateLearnerSubmissionUseCase.java:43` gọi `requireOwnedBy` rồi kiểm file tồn tại.
   - Avatar chỉ đổi qua endpoint upload; trường `avatarReference` trong request cập nhật hồ sơ bị bỏ qua.
7. **File riêng tư phát bằng URL ký.** Ghi âm và PDF trả phí ở bucket riêng tư. URL ký tối đa 15 phút
   (`SupabaseObjectStorage.java:35`), chỉ cấp cho chủ sở hữu hoặc giám khảo được giao (`GetSpeakingRecordingUrlUseCase`).
8. **Import Excel có chặn zip bomb.** `PoiCourseWorkbookReader.java:42-44` đặt tỉ lệ giải nén, kích thước entry và kích
   thước text tối đa; từ chối macro; tối đa 2000 dòng và 20.000 ký tự mỗi ô; kích thước được kiểm trước khi đọc bytes
   (`CourseImportController`); `MAX_FILE_BYTES` là 5 MB.
9. **`signedUploadUrl` không được dùng.** Đường này cho client tự `PUT` và bỏ qua kiểm tra nội dung; không service nào gọi.
   Nên giữ như vậy.
10. **Ghi âm Speaking ở PR #148 kiểm sâu hơn cả mức chung.** Ngoài `UploadPolicy`, `WavFormat.isPcm16kMono` đọc chunk `fmt`
    và `data`. Bài viết này không tính lại các lỗi của PR đó; chúng đã được xử lý trên nhánh của PR.

## 4. Phát hiện

### F-01. Ảnh không được xử lý lại (Trung bình, riêng tư)

Bằng chứng: `ContentSniffer.detect` chỉ so vài byte đầu (PNG `89 50 4E 47...`, JPEG `FF D8 FF`, GIF `GIF8`, WebP `RIFF....WEBP`).
Không nơi nào giải mã ảnh rồi ghi lại.

Hệ quả:
- Metadata EXIF, gồm tọa độ GPS, hãng máy, thời gian chụp, đi nguyên vào `avatars`, `community`, `flashcards` là các
  bucket công khai. Học viên chụp ảnh bằng điện thoại và đăng sẽ lộ vị trí nhà.
- Ảnh có header hợp lệ nhưng chứa dữ liệu khác phía sau (polyglot) vẫn qua được. Rủi ro thực thi thấp vì file được phục vụ
  từ domain Supabase khác domain ứng dụng, nhưng đây là chỗ lưu trữ miễn phí cho dữ liệu tùy ý.
- Kích thước điểm ảnh không bị giới hạn: ảnh 5 MB có thể giải ra hàng trăm triệu điểm, gây tốn bộ nhớ ở phía người xem.

Đề xuất: thêm vào `common-storage` một bước `ImageSanitizer` giải mã ảnh bằng `javax.imageio` (có sẵn trong JDK, không
thêm thư viện), từ chối nếu số điểm ảnh vượt ngưỡng, rồi mã hóa lại PNG hoặc JPEG không kèm metadata. Lưu ý:
- WebP không có sẵn trong ImageIO của JDK; hỗ trợ WebP cần một plugin, tức là thêm dependency và phải được duyệt. Trước
  mắt có thể chỉ xử lý PNG, JPEG và để WebP cho bước sau, hoặc chuyển đầu ra sang PNG/JPEG.
- GIF động sẽ mất hoạt ảnh nếu mã hóa lại bằng ImageIO mặc định; cần quyết định giữ hay bỏ GIF ở cộng đồng.

### F-02. Không có hạn mức theo user, limiter không đồng đều (Trung bình, chi phí)

Bằng chứng:
- Chỉ `UploadAvatarUseCase.java:32` và `UploadCommunityImageUseCase.java:31` gọi limiter Redis theo user.
- Ảnh flashcard, ghi âm assessment, upload của author và ghi âm Speaking chỉ chịu tier `upload` của gateway
  (1 yêu cầu/giây, burst 5, `api-gateway.yaml:354`).
- Không có bộ đếm dung lượng theo user ở service nào.

Hệ quả: một tài khoản đăng nhập có thể tải liên tục ảnh 2 MB lên bucket công khai `flashcards` với tốc độ 1 ảnh/giây.
Về lý thuyết đó là khoảng 170 GB mỗi ngày (1 ảnh/giây × 2 MB × 86.400 giây), đủ làm hết hạn mức Supabase; thực tế thấp hơn
tùy băng thông, nhưng không có gì khác chặn. Kẻ tấn công chỉ cần tạo tài khoản, hoặc dùng một tài khoản thật bị chiếm.

Đề xuất: thống nhất một cơ chế giới hạn cho mọi upload của học viên (số file mỗi giờ và tổng dung lượng mỗi ngày theo user),
dùng Redis đã có. Có hai hướng: một port `UploadQuota` trong `common-storage` cài bằng Redis, hoặc mỗi service đếm theo
prefix `{prefix}/{userId}/` trong DB của mình. Hướng thứ nhất nhất quán hơn.

### F-03. File không được dùng không bao giờ bị xóa (Trung bình, chi phí và lưu giữ dữ liệu)

Bằng chứng: `ObjectStorage.java` chỉ có `put`, `delete(bucket, key)`, `exists`, `download`, URL; không có liệt kê hay xóa
theo prefix. Mọi luồng đều theo kiểu: upload trả về key, client dùng key ở bước sau.

Hệ quả:
- File upload mà người dùng bỏ ngang (đóng tab, đổi ý, chỉ thử upload) ở lại mãi. Dọn dẹp chỉ chạm tới file mà DB biết.
- Với ghi âm giọng nói điều này vượt thời hạn lưu giữ đã công bố (90 ngày trong `/terms` của Speaking). Cả assessment
  (ghi âm đã upload nhưng chưa nộp bài) và learning (PR #148) đều có điểm này. PR #148 đã giảm bớt: key có phần ngẫu nhiên
  nên file thừa không chặn lần ghi lại, và job retention chạy hết các bài hết hạn; nhưng file mồ côi vẫn tồn tại.
- Kết hợp với F-02, kẻ tấn công có thể tạo file mà không có gì thu dọn.

Đề xuất, chọn một:
- (a) Ghi key vào một bảng `pending_uploads(key, user_id, created_at)` của service trước khi `put`, đánh dấu đã xác nhận khi
  gắn vào nghiệp vụ, và một job (`FOR UPDATE SKIP LOCKED`) xóa các dòng chưa xác nhận sau N giờ. Không đụng module dùng chung.
- (b) Thêm `list(bucket, prefix)` và `deletePrefix` vào `ObjectStorage` (Supabase có API liệt kê) và cho job dọn theo
  prefix `{prefix}/{userId}/`. Gọn hơn nhưng sửa module dùng chung nên cần duyệt.

### F-04. Vài endpoint upload của author chưa có tier riêng (Thấp)

Bằng chứng: trong `api-gateway.yaml` các route avatar (dòng 89), flashcard (148), content media (172), ghi âm
assessment (189) và ảnh cộng đồng (248) có `tier: upload`. Không có route cho
`/api/content/admin/vocabulary/{covers,sense-images,pronunciation}` và `/api/content/admin/course-import`; chúng rơi vào tier
mặc định (20 yêu cầu/giây, burst 40).

Hệ quả thấp vì chỉ `CONTENT_AUTHOR` và `ADMIN` gọi được, nhưng một tài khoản author bị chiếm sẽ upload nhanh hơn tier
`upload` cho phép, và import Excel (tốn CPU khi phân tích) cũng nằm ngoài hạn mức nghiêm. Khai báo thêm route với `tier:
upload` (hoặc `ai` cho import nếu coi là nặng) theo `AGENTS.md` §3.9.

### F-05. Kiểm định dạng audio lỏng (Thấp)

Bằng chứng: `ContentSniffer.java:53-56` nhận bất kỳ file nào bắt đầu bằng `ID3`, hoặc bằng byte `FF` rồi một byte có ba bit
cao bật, là `audio/mpeg`. `ContentSniffer.java:44` nhận WAV chỉ với `RIFF` và `WAVE` ở byte 0 và 8.

Hệ quả: dữ liệu tùy ý có thể mang header audio. Mức thấp vì audio chỉ upload được bởi author (bucket `media`) hoặc đi vào
bucket riêng tư (ghi âm). Nếu sau này mở upload audio công khai cho học viên thì mức này tăng. Có thể siết bằng cách kiểm
cấu trúc frame mp3 và chunk WAV như `WavFormat` của PR #148 đã làm.

### F-06. Đọc cả file vào heap (Thấp)

Bằng chứng: `file.getBytes()` ở mọi controller upload (10 chỗ). Kích thước tối đa là 25 MB ở `ContentMediaController`, 10 MB
ở ghi âm Speaking, 5 MB ở assessment và cộng đồng. `UploadPolicy.readBounded` đã có nhưng không được dùng vì Spring đã
đệm body từ trước (chính javadoc của nó nói vậy). Gateway không đặt giới hạn kích thước body riêng.

Hệ quả: nhiều request đồng thời giữ tới 25 MB mỗi cái trong heap trước khi bị từ chối bởi quyền hay limiter. Đã giảm bớt vì
endpoint 25 MB chỉ dành cho author. Cân nhắc giới hạn tổng số upload đồng thời mỗi service (semaphore) nếu mở rộng quy mô.

### F-07. Không quét mã độc (Thấp)

Không có antivirus hay quét nội dung ở đâu. Rủi ro chủ yếu ở PDF của author (bucket riêng tư, phát bằng URL ký cho học
viên đã trả phí) và ảnh ở bucket công khai. Chỉ cần xem xét nếu mở upload PDF hoặc file lớn cho người dùng không tin cậy.

## 5. Lộ trình đề xuất

| Thứ tự | Việc | Mã | Cần duyệt `common-storage`? |
| --- | --- | --- | --- |
| 1 | Giải mã và mã hóa lại ảnh, giới hạn điểm ảnh | F-01 | Có |
| 2 | Cơ chế giới hạn số file và dung lượng theo user cho mọi upload học viên | F-02 | Có nếu làm trong `common-storage` |
| 3 | Dọn file không được dùng | F-03 | Hướng (a) không; hướng (b) có |
| 4 | Khai báo tier cho các route author còn thiếu | F-04 | Không (sửa config-repo) |
| 5 | Siết kiểm định dạng audio | F-05 | Có |
| 6 | Xem xét giới hạn upload đồng thời, quét mã độc | F-06, F-07 | Tùy thiết kế |

Theo `AGENTS.md` §3.4 và §5, sửa module dùng chung hoặc thêm dependency (plugin WebP, ClamAV) là quyết định thiết kế cần
được duyệt trước khi làm.

## 6. Chưa kiểm, cần xác nhận

- **Cấu hình bucket trên Supabase:** danh sách MIME và dung lượng tối đa của từng bucket có khớp bảng trong
  `docs/system-architecture.md` không (riêng `media-premium` còn ghi "cần thêm `application/pdf` vào loại file cho phép").
- **Header khi phục vụ file công khai:** Supabase có trả `X-Content-Type-Options: nosniff` và `Content-Type` đúng loại đã
  lưu không.
- **Frontend:** cách hiển thị URL ảnh, tên file và nội dung bài đăng.
- **Đăng ký tài khoản:** tạo tài khoản hàng loạt bị chặn đến đâu, vì đó là điều kiện tấn công F-02.
- **Thử nghiệm đối kháng:** chưa upload thử file polyglot, ảnh có điểm ảnh rất lớn, hay request đồng thời nhiều file lớn.

## 7. Nguồn

- `shared/common-storage` (`upload/`, `key/`, `supabase/`)
- Use case và controller upload liệt kê ở mục 2
- `infra/config-server/config-repo/*.yaml` (multipart, `api-gateway.yaml`)
- `docs/system-architecture.md` (bảng bucket, mục lưu trữ file)
