# common-storage

Thư viện dùng chung để ghi, xóa và cấp URL cho object trên Supabase Storage. Không deploy độc lập. Service nào cần
lưu file thì khai báo dependency và tự chọn bucket.

## Cấu trúc package

```
com.ieltspath.commonstorage
├── ObjectStorage              cổng duy nhất service gọi (put, delete, exists, publicUrl, signed URL)
├── StoredObject, SignedUpload kết quả ghi và URL upload có hạn
├── StorageException           lỗi hạ tầng; thông báo cố định, không chứa key/bucket/khóa
├── UploadRejectedException    file bị từ chối (IllegalArgumentException, đi đường 400)
├── UnavailableObjectStorage   bản cài khi chưa cấu hình: mọi lệnh ném UNAVAILABLE
├── key/                       ObjectKeys, InvalidObjectKeyException: sinh và kiểm object key
├── upload/                    ContentTypes, ContentSniffer, UploadPolicy: chuẩn hóa MIME, magic bytes, giới hạn
├── config/                    SupabaseStorageProperties, CommonStorageAutoConfiguration
└── supabase/                  SupabaseObjectStorage (client REST) và helper package-private
```

`key/`, `upload/` và phần còn lại thuần Java, không phụ thuộc Supabase. Chỉ `supabase/` gọi mạng.

## Bật trong service

Auto-configuration tắt mặc định. Service phải bật và cấp cấu hình:

```yaml
app:
  storage:
    enabled: true
    supabase:
      url: ${SUPABASE_URL:}          # https://<project-ref>.supabase.co, không có /storage/v1
      secret-key: ${SUPABASE_SECRET_KEY:}
      # connect-timeout 3s, read-timeout 10s, upload-timeout 120s (mặc định)
```

- `url` và `secret-key` phải cùng trống hoặc cùng có. Chỉ một trong hai là lỗi, service không khởi động.
- Cả hai trống: service vẫn khởi động, mọi lệnh storage ném `UNAVAILABLE`.
- `SUPABASE_*` là tên biến môi trường. Hiện chưa có service nào ánh xạ chúng vào config-repo; việc đó thuộc plan của từng service.

## Bucket

Module không biết bucket nào công khai; bảng dưới là quy ước chung của cả hệ thống. Cột "Trạng thái" ghi tình trạng
theo kiểm kê ngày 2026-10-31.

| Bucket | Công khai | Dùng để | Giới hạn | Loại file (MIME) | Chủ | Khóa thường gặp | Trạng thái |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `avatars` | có | Ảnh đại diện học viên | 2 MB | png, jpeg, webp | user-service | `images/{userId}/{uuid}.{ext}` | đã có |
| `flashcards` | có | Ảnh thẻ, bìa bộ thẻ, bìa catalog từ vựng | 2 MB | png, jpeg, webp | library-service | `images/{userId}/{uuid}.{ext}`; bìa catalog `catalog-cover/{userId}/{uuid}.{ext}` | đã có; bìa catalog là kế hoạch |
| `community` | có | Ảnh bài đăng cộng đồng | 5 MB | png, jpeg, webp, gif | community-service | `community/post/{userId}/{uuid}.{ext}` | đã có |
| `media` | có | Audio và ảnh nội dung bài học | 25 MB | mpeg, mp4, wav, ogg, png, jpeg, webp | content-service | seed: `listening/demo/ls1.mp3`; upload API: `content-audio/{userId}/{uuid}.{ext}` | đã có |
| `speaking-recordings` | không | Ghi âm Speaking của học viên | 5 MB | webm, mp4, ogg, mpeg, wav | assessment-service | `speaking/{userId}/{uuid}.{ext}` | đang dùng (upload ghi âm Speaking) |
| `media-premium` | không | Audio bài trả phí, chỉ phát bằng URL ký | 25 MB | mpeg, mp4, wav, ogg | content-service | như `media` | đã tạo, riêng tư |

Biến cấu hình tên bucket: `USER_AVATAR_BUCKET`, `LIBRARY_FLASHCARD_BUCKET`, `COMMUNITY_MEDIA_BUCKET`,
`CONTENT_MEDIA_BUCKET`, `CONTENT_PREMIUM_MEDIA_BUCKET`.
Giới hạn trong code là nguồn chuẩn; giới hạn bucket phải bằng hoặc lớn hơn giới hạn code.

### URL mẫu

`<project-ref>` là ref của project Supabase; `<key>` là khóa object.

Công khai, cho bucket có `public = true`. `publicUrl(bucket, key)` chỉ ghép chuỗi, không gọi mạng:

```
https://<project-ref>.supabase.co/storage/v1/object/public/<bucket>/<key>
https://<project-ref>.supabase.co/storage/v1/object/public/media/listening/demo/ls1.mp3
```

Ký có hạn, cho bucket riêng tư. `signedDownloadUrl(bucket, key, ttl)` gọi mạng, trả URL dùng được trong `ttl`:

```
https://<project-ref>.supabase.co/storage/v1/object/sign/<bucket>/<key>?token=<jwt>
```

- `ttl` phải từ 1 giây đến **15 phút** (`MAX_TTL`); ngoài khoảng này module từ chối.
- URL ký là bí mật: không lưu vào DB, không ghi log. Cấp lại mỗi lần cần.

Upload ký, cho file lớn qua `signedUploadUrl(bucket, key)`. Không có tham số hạn; Supabase quyết định thời gian hiệu lực:

```
https://<project-ref>.supabase.co/storage/v1/object/upload/sign/<bucket>/<key>?token=<jwt>
```

Biến `CONTENT_MEDIA_BASE_URL` của content-service là tiền tố công khai của bucket `media`, dạng
`https://<project-ref>.supabase.co/storage/v1/object/public/media/`, có `/` cuối. Khóa tương đối được ghép vào tiền tố này.

Content đánh dấu mỗi asset bằng `access_tier`: `PUBLIC` được resolve qua bucket `media`; `PREMIUM` được cấp URL ký
từ bucket riêng tư `media-premium` với TTL 900 giây. Learning chỉ yêu cầu URL ký sau khi lesson gate cho phép đọc.
URL ký không lưu vào DB hoặc log; cần URL mới thì service xin lại.

### Khóa object

- `ObjectKeys.forUser(prefix, userId, ext)` sinh `{prefix}/{userId}/{uuid}.{ext}`.
- `prefix`: chữ thường, số, dấu `-`, tối đa 32 ký tự. `ext`: 1 đến 5 ký tự chữ thường hoặc số.
- Khóa do người quản trị đặt tay (seed): mỗi đoạn gồm `A-Z a-z 0-9 . _ -`, bắt đầu bằng chữ hoặc số, tối đa 100 ký tự; không có `..`, không bắt đầu bằng `/`.
- Tên bucket: chữ thường, số, `_`, `-`; dài 3 đến 63 ký tự.

## Quy tắc khi dùng

- **Key do client gửi lại:** gọi `ObjectKeys.requireOwnedBy` và `ObjectStorage.exists` trước khi lưu. Thư viện không ép
  được việc gọi.
- **`put` chỉ cho file nhỏ** (nội dung giữ trong bộ nhớ). File lớn dùng `signedUploadUrl`. Đường này **không** qua kiểm tra
  nội dung của module; giới hạn MIME và dung lượng phải đặt ở bucket.
- **Sau `StorageException` có `outcomeUnknown=true`** (timeout giữa chừng): không retry cùng key. Sinh key mới và xóa key cũ,
  best-effort.
- **`exists`** trả `false` chỉ khi bucket truy cập được. Khóa sai cũng trả 404 khi kiểm object.
- **URL ký là bí mật**: không ghi log. `SignedUpload.toString()` đã che URL.
- **Bucket là tham số**: module không biết bucket nào công khai. Gọi `publicUrl` trên bucket private chỉ cho ra URL không dùng được.
- **Lỗi đầu vào** (`InvalidObjectKeyException`, `UploadRejectedException`) là `IllegalArgumentException`, thông báo cố định.
  Lỗi hạ tầng (`StorageException`) không phải. `GlobalExceptionHandler` hiện có map `IllegalArgumentException` thành 400.

## Test

```powershell
mvn -q -pl shared/common-storage test
```

Test mock dùng `MockRestServiceServer`, không cần mạng. Contract test
(`contract/SupabaseStorageContractTest`) chạy với Supabase thật, mặc định bị skip. Chạy khi có:

- `STORAGE_CONTRACT_URL`: URL project.
- `STORAGE_CONTRACT_KEY_FILE`: đường dẫn file chứa khóa, nằm ngoài repo. Không đặt khóa vào biến môi trường hay dòng lệnh.
- `STORAGE_CONTRACT_BUCKET`: tên bucket bắt đầu bằng `contract-test-`. Tên khác thì test skip.

Test chỉ thao tác dưới `contract-test/{uuid}/` trong bucket đó và tự dọn.

## Tài liệu liên quan

- Kết quả smoke và hành vi thật của API: `plans/261019-0813-common-storage-module/reports/`.
- Quy trình xoay khóa và ranh giới khóa server: `AGENTS.md`, mục 3.4.
